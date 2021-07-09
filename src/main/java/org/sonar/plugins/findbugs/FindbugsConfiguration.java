/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import edu.umd.cs.findbugs.Project;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.PropertyType;
import org.sonar.api.Startable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.findbugs.xml.Bug;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;
import org.sonar.plugins.java.Java;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static java.lang.String.format;

@ScannerSide
public class FindbugsConfiguration implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsConfiguration.class);

  private final FileSystem fileSystem;
  private final Configuration config;
  private final ActiveRules activeRules;
  private final JavaResourceLocator javaResourceLocator;

  public FindbugsConfiguration(FileSystem fileSystem, Configuration config, ActiveRules activeRules,
                               JavaResourceLocator javaResourceLocator) {
    this.fileSystem = fileSystem;
    this.config = config;
    this.activeRules = activeRules;
    this.javaResourceLocator = javaResourceLocator;
  }

  public File getTargetXMLReport() {
    return new File(fileSystem.workDir(), "findbugs-result.xml");
  }

  public Project getFindbugsProject() throws IOException {
    Project findbugsProject = new Project();

    List<File> classFilesToAnalyze = new ArrayList<>(javaResourceLocator.classFilesToAnalyze());

    for (File file : javaResourceLocator.classpath()) {
      //Will capture additional classes including precompiled JSP
      if(file.isDirectory()) { // will include "/target/classes" and other non-standard folders
        classFilesToAnalyze.addAll(scanForAdditionalClasses(file));
      }

      //Auxiliary dependencies
      findbugsProject.addAuxClasspathEntry(file.getCanonicalPath());
    }

    boolean hasJspFiles = fileSystem.hasFiles(fileSystem.predicates().hasLanguage("jsp"));
    boolean hasPrecompiledJsp = false;
    for (File classToAnalyze : classFilesToAnalyze) {
      String absolutePath = classToAnalyze.getCanonicalPath();
      if(hasJspFiles && !hasPrecompiledJsp
              && (absolutePath.endsWith("_jsp.class") || //Jasper
                  absolutePath.contains("/jsp_servlet/")) //WebLogic
              ) {
        hasPrecompiledJsp = true;
      }
      if(!"module-info.class".equals(classToAnalyze.getName())) {
        findbugsProject.addFile(absolutePath);
      }
    }

    if (classFilesToAnalyze.isEmpty()) {
      LOG.warn("Findbugs needs sources to be compiled."
              + " Please build project before executing sonar or check the location of compiled classes to"
              + " make it possible for Findbugs to analyse your (sub)project ({}).", fileSystem.baseDir().getPath());
      
      if (!isAllowUncompiledCode() && hasSourceFiles()) { //This excludes test source files
        throw new IllegalStateException(format("One (sub)project contains Java source files that are not compiled (%s).",
                fileSystem.baseDir().getPath()));
      }
    }

    if (hasJspFiles && !hasPrecompiledJsp) {
      LOG.warn("JSP files were found in the current (sub)project ({}) but FindBugs requires their precompiled form. " +
              "For more information on how to configure JSP precompilation : https://github.com/find-sec-bugs/find-sec-bugs/wiki/JSP-precompilation",
              fileSystem.baseDir().getPath());
    }

    copyLibs();
    if (annotationsLib != null) {
      // Findbugs dependencies are packaged by Maven. They are not available during execution of unit tests.
      findbugsProject.addAuxClasspathEntry(annotationsLib.getCanonicalPath());
      findbugsProject.addAuxClasspathEntry(jsr305Lib.getCanonicalPath());
    }
    findbugsProject.setCurrentWorkingDirectory(fileSystem.workDir());
    return findbugsProject;
  }

  private void exportProfile(ActiveRules activeRules, Writer writer) {
    try {
      FindBugsFilter filter = buildFindbugsFilter(
        activeRules.findAll().stream().filter(activeRule ->
        {
          String repKey = activeRule.ruleKey().repository();
          return repKey.contains(FindbugsRulesDefinition.REPOSITORY_KEY) ||
            repKey.contains(FindSecurityBugsRulesDefinition.REPOSITORY_KEY) ||
            repKey.contains(FbContribRulesDefinition.REPOSITORY_KEY);
        })
          .collect(Collectors.toList())
      );
      XStream xstream = FindBugsFilter.createXStream();
      writer.append(xstream.toXML(filter));
    } catch (IOException e) {
      throw new SonarException("Fail to generate the Findbugs profile configuration", e);
    }
  }

  private static FindBugsFilter buildFindbugsFilter(Iterable<ActiveRule> activeRules) {
    FindBugsFilter root = new FindBugsFilter();
    for (ActiveRule activeRule : activeRules) {
      String repoKey = activeRule.ruleKey().repository();

      if (repoKey.contains(FindSecurityBugsRulesDefinition.REPOSITORY_KEY) || repoKey.contains(FindbugsRulesDefinition.REPOSITORY_KEY) || repoKey.contains(FbContribRulesDefinition.REPOSITORY_KEY)) {
        Match child = new Match();
        child.setBug(new Bug(activeRule.internalKey()));
        root.addMatch(child);
      }
    }
    return root;
  }

  /**
   * Determine if the project has Java source files. This is used to determine if the project has no compiled classes on
   * purpose or because the compilation was omit from the process.
   * @return If at least one Java file is present
   */
  private boolean hasSourceFiles() {
    FilePredicates pred = fileSystem.predicates();
    return fileSystem.hasFiles(
            pred.and(
                    pred.hasType(Type.MAIN),
                    pred.or(FindbugsPlugin.getSupportedLanguagesFilePredicate(pred)),
                    //package-info.java will not generate any class files.
                    //See: https://github.com/SonarQubeCommunity/sonar-findbugs/issues/36
                    pred.not(pred.matchesPathPattern("**/package-info.java")),
                    pred.not(pred.matchesPathPattern("**/module-info.java")),
                    pred.not(pred.matchesPathPattern("**/*.jsp"))
            )
    );
  }

  @VisibleForTesting
  File saveIncludeConfigXml() throws IOException {
    StringWriter conf = new StringWriter();
    exportProfile(activeRules, conf);
    File file = new File(fileSystem.workDir(), "findbugs-include.xml");
    FileUtils.write(file, conf.toString(), CharEncoding.UTF_8);
    return file;
  }

  /**
   * Scan the given folder for classes. It will catch classes from Java, JSP and more.
   *
   * @param folder Folder to scan
   * @return {@code List<File>} of class files
   */
  public static List<File> scanForAdditionalClasses(File folder) {
    List<File> allFiles = new ArrayList<File>();
    Queue<File> dirs = new LinkedList<File>();
    dirs.add(folder);
    while (!dirs.isEmpty()) {
      File dirPoll = dirs.poll();
      if(dirPoll == null) break; //poll() result could be null if the queue is empty.
      for (File f : dirPoll.listFiles()) {
        if (f.isDirectory()) {
          dirs.add(f);
        } else if (f.isFile()&& f.getName().endsWith(".class")) {
          allFiles.add(f);
        }
      }
    }
    return allFiles;
  }

  @VisibleForTesting
  List<File> getExcludesFilters() {
    List<File> result = Lists.newArrayList();
    PathResolver pathResolver = new PathResolver();
    String[] filters = config.getStringArray(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY);
    for (String excludesFilterPath : filters) {
      excludesFilterPath = StringUtils.trim(excludesFilterPath);
      if (StringUtils.isNotBlank(excludesFilterPath)) {
        result.add(pathResolver.relativeFile(fileSystem.baseDir(), excludesFilterPath));
      }
    }
    return result;
  }

  public String getEffort() {
    return StringUtils.lowerCase(
            config.get(FindbugsConstants.EFFORT_PROPERTY)
            .orElse(FindbugsConstants.EFFORT_DEFAULT_VALUE));
  }

  public String getConfidenceLevel() {
    return StringUtils.lowerCase(
            config.get(FindbugsConstants.CONFIDENCE_LEVEL_PROPERTY)
            .orElse(FindbugsConstants.CONFIDENCE_LEVEL_DEFAULT_VALUE));
  }

  public long getTimeout() {
    return config.getLong(FindbugsConstants.TIMEOUT_PROPERTY).orElse(FindbugsConstants.TIMEOUT_DEFAULT_VALUE);
  }

  public boolean isAllowUncompiledCode() {
    return config.getBoolean(FindbugsConstants.ALLOW_UNCOMPILED_CODE).orElse(FindbugsConstants.ALLOW_UNCOMPILED_CODE_VALUE);
  }

  private File jsr305Lib;
  private File annotationsLib;
  private File fbContrib;
  private File findSecBugs;

  public void copyLibs() {
    if (jsr305Lib == null) {
      jsr305Lib = copyLib("/jsr305.jar");
    }
    if (annotationsLib == null) {
      annotationsLib = copyLib("/annotations.jar");
    }
    if (fbContrib == null) {
      fbContrib = copyLib("/sb-contrib.jar");
    }
    if (findSecBugs == null) {
      findSecBugs = copyLib("/findsecbugs-plugin.jar");
    }
  }

  @Override
  public void start() {
    // do nothing
  }

  /**
   * Invoked by PicoContainer to remove temporary files.
   */
  @SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  @Override
  public void stop() {
    if (jsr305Lib != null) {
      jsr305Lib.delete();
    }
    if (annotationsLib != null) {
      annotationsLib.delete();
    }
    if (fbContrib != null) {
      fbContrib.delete();
    }

    if (findSecBugs != null) {
      findSecBugs.delete();
    }
  }

  private File copyLib(String name) {
    InputStream input = null;
    try {
      input = getClass().getResourceAsStream(name);
      File dir = new File(fileSystem.workDir(), "findbugs");
      FileUtils.forceMkdir(dir);
      File target = new File(dir, name);
      FileUtils.copyInputStreamToFile(input, target);
      return target;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to extract Findbugs dependency", e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public File getFbContribJar() {
    return fbContrib;
  }

  public File getFindSecBugsJar() {
    return findSecBugs;
  }

  public static List<PropertyDefinition> getPropertyDefinitions() {
    String subCategory = "FindBugs";
    return ImmutableList.of(
      PropertyDefinition.builder(FindbugsConstants.EFFORT_PROPERTY)
        .defaultValue(FindbugsConstants.EFFORT_DEFAULT_VALUE)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Effort")
        .description("Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases " +
          "memory consumption.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.TIMEOUT_PROPERTY)
        .defaultValue(Long.toString(FindbugsConstants.TIMEOUT_DEFAULT_VALUE))
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Timeout")
        .description("Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. " +
          "The default is 600,000 milliseconds, which is ten minutes.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .type(PropertyType.INTEGER)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Excludes Filters")
        .description("Paths to findbugs filter-files with exclusions.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .multiValues(true)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.CONFIDENCE_LEVEL_PROPERTY)
        .defaultValue(FindbugsConstants.CONFIDENCE_LEVEL_DEFAULT_VALUE)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Confidence Level")
        .description("Specifies the confidence threshold (previously called \"priority\") for reporting issues. If set to \"low\", confidence is not used to filter bugs. " +
          "If set to \"medium\" (the default), low confidence issues are suppressed. If set to \"high\", only high confidence bugs are reported. ")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.ALLOW_UNCOMPILED_CODE)
        .defaultValue(Boolean.toString(FindbugsConstants.ALLOW_UNCOMPILED_CODE_VALUE))
        .type(PropertyType.BOOLEAN)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Allow Uncompiled Code")
        .description("Remove the compiled code requirement for all projects. "+
          "It can lead to a false sense of security if the build process skips certain projects.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.REPORT_PATHS)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Report Paths")
        .description("Relative path to SpotBugs report files intended to be reused. (<code>/target/findbugsXml.xml</code> and <code>/target/spotbugsXml.xml</code> are included by default)")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .multiValues(true)
        .build()
      );
  }

}
