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

import static java.lang.String.format;

import org.apache.commons.lang.ArrayUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.Project;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@BatchSide
public class FindbugsConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsConfiguration.class);

  private final FileSystem fileSystem;
  private final Settings settings;
  private final RulesProfile profile;
  private final FindbugsProfileExporter exporter;
  private final JavaResourceLocator javaResourceLocator;

  public FindbugsConfiguration(FileSystem fileSystem, Settings settings, RulesProfile profile, FindbugsProfileExporter exporter,
    JavaResourceLocator javaResourceLocator) {
    this.fileSystem = fileSystem;
    this.settings = settings;
    this.profile = profile;
    this.exporter = exporter;
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

      if (hasSourceFiles()) { //This excludes test source files
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

  /**
   * Return the complete list of Java files from the project (excluding test files)
   * @return The list of Java files
   */
  private Iterable<File> getSourceFiles() {
    FilePredicates pred = fileSystem.predicates();
    return fileSystem.files(pred.and(
            pred.hasType(Type.MAIN),
            pred.or(FindbugsPlugin.getSupportedLanguagesFilePredicate(pred)),
            pred.not(pred.matchesPathPattern("**/package-info.java")),
            pred.not(pred.matchesPathPattern("**/module-info.java"))
    ));
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
                    pred.not(pred.matchesPathPattern("**/module-info.java"))
            )
    );
  }

  @VisibleForTesting
  File saveIncludeConfigXml() throws IOException {
    StringWriter conf = new StringWriter();
    exporter.exportProfile(profile, conf);
    File file = new File(fileSystem.workDir(), "findbugs-include.xml");
    FileUtils.write(file, conf.toString(), CharEncoding.UTF_8);
    return file;
  }

  /**
   * Scan the given folder for classes. It will catch classes from Java, JSP and more.
   *
   * @param folder Folder to scan
   * @return
   * @throws IOException
   */
  public static List<File> scanForAdditionalClasses(File folder) throws IOException {
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
    String[] filters = settings.getStringArray(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY);
    for (String excludesFilterPath : filters) {
      excludesFilterPath = StringUtils.trim(excludesFilterPath);
      if (StringUtils.isNotBlank(excludesFilterPath)) {
        result.add(pathResolver.relativeFile(fileSystem.baseDir(), excludesFilterPath));
      }
    }
    return result;
  }

  public String getEffort() {
    return StringUtils.lowerCase(settings.getString(FindbugsConstants.EFFORT_PROPERTY));
  }

  public String getConfidenceLevel() {
    return StringUtils.lowerCase(settings.getString(FindbugsConstants.CONFIDENCE_LEVEL_PROPERTY));
  }

  public long getTimeout() {
    return settings.getLong(FindbugsConstants.TIMEOUT_PROPERTY);
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
      fbContrib = copyLib("/fb-contrib.jar");
    }
    if (findSecBugs == null) {
      findSecBugs = copyLib("/findsecbugs-plugin.jar");
    }
  }

  /**
   * Invoked by PicoContainer to remove temporary files.
   */
  @SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
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
        .category(CoreProperties.CATEGORY_JAVA)
        .subCategory(subCategory)
        .name("Effort")
        .description("Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases " +
          "memory consumption.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.TIMEOUT_PROPERTY)
        .defaultValue(Long.toString(FindbugsConstants.TIMEOUT_DEFAULT_VALUE))
        .category(CoreProperties.CATEGORY_JAVA)
        .subCategory(subCategory)
        .name("Timeout")
        .description("Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. " +
          "The default is 600,000 milliseconds, which is ten minutes.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .type(PropertyType.INTEGER)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)
        .category(CoreProperties.CATEGORY_JAVA)
        .subCategory(subCategory)
        .name("Excludes Filters")
        .description("Paths to findbugs filter-files with exclusions.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .multiValues(true)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.CONFIDENCE_LEVEL_PROPERTY)
        .defaultValue(FindbugsConstants.CONFIDENCE_LEVEL_DEFAULT_VALUE)
        .category(CoreProperties.CATEGORY_JAVA)
        .subCategory(subCategory)
        .name("Confidence Level")
        .description("Specifies the confidence threshold (previously called \"priority\") for reporting issues. If set to \"low\", confidence is not used to filter bugs. " +
          "If set to \"medium\" (the default), low confidence issues are supressed. If set to \"high\", only high confidence bugs are reported. ")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build()
      );
  }

}
