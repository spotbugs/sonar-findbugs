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

import com.thoughtworks.xstream.XStream;

import edu.umd.cs.findbugs.ClassScreener;
import edu.umd.cs.findbugs.Project;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
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
public class FindbugsConfiguration {

  private static final Logger LOG = Loggers.get(FindbugsConfiguration.class);
  private static final Pattern JSP_FILE_NAME_PATTERN = Pattern.compile(".*_jsp[\\$0-9]*\\.class");

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

  public void initializeFindbugsProject(Project findbugsProject) throws IOException {
    List<File> classFilesToAnalyze = buildClassFilesToAnalyze();

    for (File file : javaResourceLocator.classpath()) {
      //Auxiliary dependencies
      findbugsProject.addAuxClasspathEntry(file.getCanonicalPath());
    }
    
    ClassScreener classScreener = getOnlyAnalyzeFilter();
    
    for (File classToAnalyze : classFilesToAnalyze) {   
      String absolutePath = classToAnalyze.getCanonicalPath(); 	     
      
      boolean matchesClassScreener = classScreener!=null && classScreener.matches(absolutePath);
      boolean noClassScreenerAndMatches = classScreener == null && !"module-info.class".equals(classToAnalyze.getName());
      
      if(matchesClassScreener || noClassScreenerAndMatches) {
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

    findbugsProject.setCurrentWorkingDirectory(fileSystem.workDir());
  }

  /**
   * Creates a class screener to filter the files for analysis by findbugs.
   * The filter is based on  sonar.findbugs.onlyAnalyze {@link FindbugsConstants} property
   * 
   * @return ClassScreener object if property is present and not empty, null otherwise.
   */
  protected @Nullable ClassScreener getOnlyAnalyzeFilter() {
	  ClassScreener classScreener = new ClassScreener();
	  Optional<String> onlyAnalyzeProp = config.get(FindbugsConstants.ONLY_ANALYZE_PROPERTY);
	  if(!onlyAnalyzeProp.isPresent() || StringUtils.isEmpty(onlyAnalyzeProp.get())) {
		  return null;
	  }
	  String onlyAnayzeOptions = onlyAnalyzeProp.get();
	  StringTokenizer tok = new StringTokenizer(onlyAnayzeOptions, ",");
	  while (tok.hasMoreTokens()) {
		  String item = tok.nextToken();
		  if (item.endsWith(".-")) {
			  classScreener.addAllowedPrefix(item.substring(0, item.length() - 1));
		  } else if (item.endsWith(".*")) {
			  classScreener.addAllowedPackage(item.substring(0, item.length() - 1));
		  } else {
			  classScreener.addAllowedClass(item);
		  }
	  }
	  return classScreener;
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
      throw new IllegalArgumentException("Fail to generate the Findbugs profile configuration", e);
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
                    pred.not(pred.matchesPathPattern("**/module-info.java"))
            )
    );
  }

  File saveIncludeConfigXml() throws IOException {
    StringWriter conf = new StringWriter();
    exportProfile(activeRules, conf);
    File file = new File(fileSystem.workDir(), "findbugs-include.xml");
    FileUtils.write(file, conf.toString(), StandardCharsets.UTF_8);
    return file;
  }
  
  private List<File> buildClassFilesToAnalyze() throws IOException {
    List<File> classFilesToAnalyze = new ArrayList<>(javaResourceLocator.classFilesToAnalyze());
    
    boolean hasScalaFiles = fileSystem.hasFiles(fileSystem.predicates().hasLanguage("scala"));
    boolean hasJspFiles = fileSystem.hasFiles(fileSystem.predicates().hasLanguage("jsp"));    

    // javaResourceLocator.classFilesToAnalyze() only contains .class files from Java sources
    if (hasScalaFiles) {
      // Add all the .class files from the classpath
      // For Gradle multi-module projects this will unfortunately include compiled .class files from dependency modules
      addClassFilesFromClasspath(classFilesToAnalyze);
    } else if (hasJspFiles) {
      // Add the precompiled JSP .class files
      addPrecompiledJspClasses(classFilesToAnalyze);
    }

    return classFilesToAnalyze;
  }

  /**
   * Updates the class files list by adding precompiled JSP classes
   * 
   * @param classFilesToAnalyze The current list of class files to analyze, by default SonarQube does not include precompiled JSP classes
   * 
   * @throws IOException In case an exception was thrown when building a file canonical path
   */
  public void addPrecompiledJspClasses(List<File> classFilesToAnalyze) throws IOException {
    addClassFilesFromClasspath(classFilesToAnalyze, FindbugsConfiguration::isPrecompiledJspClassFile);
    
    boolean hasPrecompiledJsp = hasPrecompiledJsp(classFilesToAnalyze);

    if (!hasPrecompiledJsp) {
      LOG.warn("JSP files were found in the current (sub)project ({}) but FindBugs requires their precompiled form. " +
              "For more information on how to configure JSP precompilation : https://github.com/find-sec-bugs/find-sec-bugs/wiki/JSP-precompilation",
              fileSystem.baseDir().getPath());
    }
  }
  
  /**
   * Updates the class files list by adding all .class files from the classpath
   * 
   * @param classFilesToAnalyze The current list of class files to analyze
   */
  private void addClassFilesFromClasspath(List<File> classFilesToAnalyze) {
    addClassFilesFromClasspath(classFilesToAnalyze, f -> f.getName().endsWith(".class"));
  }

  private void addClassFilesFromClasspath(List<File> classFilesToAnalyze, Predicate<File> filePredicate) {
    for (File file : javaResourceLocator.classpath()) {
      //Will capture additional classes including precompiled JSP
      if(file.isDirectory()) { // will include "/target/classes" and other non-standard folders
        classFilesToAnalyze.addAll(scanForAdditionalClasses(file, filePredicate));
      }
    }
  }

  public boolean hasPrecompiledJsp(List<File> classFilesToAnalyze) {
    for (File classToAnalyze : classFilesToAnalyze) {
      if(isPrecompiledJspClassFile(classToAnalyze)) {
        return true;
      }
    }
    
    return false;
  }

  public static boolean isPrecompiledJspClassFile(File file) {
    String fileName = file.getName();
    
    //Jasper
    // Replacement for previous implementation absolutePath.endsWith("_jsp.class") to account for inner classes
    if (JSP_FILE_NAME_PATTERN.matcher(fileName).matches()) {
      return true; 
    }
    
    if (fileName.endsWith(".class")) {
      //WebLogic
      File parent = file.getParentFile();
      while (parent != null) {
        // Replacement for previous implementation absolutePath.contains("/jsp_servlet/") to account for windows paths
        if (parent.getName().equals("jsp_servlet")) {
          return true;
        }
        
        parent = parent.getParentFile();
      }
    }
    
    return false;
  }

  /**
   * Scan the given folder for classes. It will catch classes compiled JSP classes.
   *
   * @param folder Folder to scan
   * @param filePredicate The {@link Predicate} applied to filter files (folders are not tested against the predicate)
   * @return {@code List<File>} of class files
   */
  public static List<File> scanForAdditionalClasses(File folder, Predicate<File> filePredicate) {
    List<File> allFiles = new ArrayList<>();
    Queue<File> dirs = new LinkedList<>();
    dirs.add(folder);
    while (!dirs.isEmpty()) {
      File dirPoll = dirs.poll();
      if(dirPoll == null) break; //poll() result could be null if the queue is empty.
      for (File f : dirPoll.listFiles()) {
        if (f.isDirectory()) {
          dirs.add(f);
        } else if (filePredicate.test(f)) {
          allFiles.add(f);
        }
      }
    }
    return allFiles;
  }

  List<File> getExcludesFilters() {
    List<File> result = new ArrayList<>();
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

  public static List<PropertyDefinition> getPropertyDefinitions() {
    String subCategory = "FindBugs";
    return Collections.unmodifiableList(Arrays.asList(
      PropertyDefinition.builder(FindbugsConstants.EFFORT_PROPERTY)
        .defaultValue(FindbugsConstants.EFFORT_DEFAULT_VALUE)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Effort")
        .description("Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases " +
          "memory consumption.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.TIMEOUT_PROPERTY)
        .defaultValue(Long.toString(FindbugsConstants.TIMEOUT_DEFAULT_VALUE))
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Timeout")
        .description("Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. " +
          "The default is 600,000 milliseconds, which is ten minutes.")
        .onQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.INTEGER)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Excludes Filters")
        .description("Paths to findbugs filter-files with exclusions.")
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.CONFIDENCE_LEVEL_PROPERTY)
        .defaultValue(FindbugsConstants.CONFIDENCE_LEVEL_DEFAULT_VALUE)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Confidence Level")
        .description("Specifies the confidence threshold (previously called \"priority\") for reporting issues. If set to \"low\", confidence is not used to filter bugs. " +
          "If set to \"medium\" (the default), low confidence issues are suppressed. If set to \"high\", only high confidence bugs are reported. ")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.ALLOW_UNCOMPILED_CODE)
        .defaultValue(Boolean.toString(FindbugsConstants.ALLOW_UNCOMPILED_CODE_VALUE))
        .type(PropertyType.BOOLEAN)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Allow Uncompiled Code")
        .description("Remove the compiled code requirement for all projects. "+
          "It can lead to a false sense of security if the build process skips certain projects.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition.builder(FindbugsConstants.REPORT_PATHS)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Report Paths")
        .description("Relative path to SpotBugs report files intended to be reused. (<code>/target/findbugsXml.xml</code> and <code>/target/spotbugsXml.xml</code> are included by default)")
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build(),
        PropertyDefinition.builder(FindbugsConstants.ONLY_ANALYZE_PROPERTY)
        .category(Java.KEY)
        .subCategory(subCategory)
        .name("Only Analyze")
        .description("To analyze only the given files (in FQCN, comma separted) / package patterns")
        .type(PropertyType.STRING)
        .build()      
      ));
  }

}
