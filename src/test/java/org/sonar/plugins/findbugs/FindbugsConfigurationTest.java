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

import edu.umd.cs.findbugs.ClassScreener;
import edu.umd.cs.findbugs.Project;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.findbugs.configuration.SimpleConfiguration;
import org.sonar.plugins.findbugs.rule.FakeActiveRules;
import org.sonar.plugins.findbugs.util.JupiterLogTester;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindbugsConfigurationTest {

  @TempDir
  public File temp;
  
  @RegisterExtension
  public LogTester logTester = new JupiterLogTester();

  private FilePredicates filePredicates;
  private FileSystem fs;
  private SimpleConfiguration configuration;
  private File baseDir;
  private File workDir;
  private ActiveRules activeRules;
  private FindbugsConfiguration conf;
  private JavaResourceLocator javaResourceLocator;

  @BeforeEach
  public void setUp() throws Exception {
    baseDir = new File(temp, "findbugs");
    workDir = new File(temp, "findbugs");

    filePredicates = mock(FilePredicates.class);
    
    fs = mock(FileSystem.class);
    when(fs.baseDir()).thenReturn(baseDir);
    when(fs.workDir()).thenReturn(workDir);
    when(fs.predicates()).thenReturn(filePredicates);
    
    activeRules = FakeActiveRules.createWithOnlyFindbugsRules();

    configuration = new SimpleConfiguration();
    javaResourceLocator = mock(JavaResourceLocator.class);
    conf = new FindbugsConfiguration(fs, configuration, activeRules, javaResourceLocator);
  }

  @Test
  void should_return_report_file() throws Exception {
    assertThat(conf.getTargetXMLReport().getCanonicalPath()).isEqualTo(new File(fs.workDir(), "findbugs-result.xml").getCanonicalPath());
  }

  @Test
  void should_save_include_config() throws Exception {
    conf.saveIncludeConfigXml();
    File findbugsIncludeFile = new File(fs.workDir(), "findbugs-include.xml");
    assertThat(findbugsIncludeFile).exists();
  }

  @Test
  void should_return_effort() {
    assertThat(conf.getEffort()).as("default effort").isEqualTo("default");
    configuration.setProperty(FindbugsConstants.EFFORT_PROPERTY, "Max");
    assertThat(conf.getEffort()).isEqualTo("max");
  }

  @Test
  void should_return_timeout() {
    assertThat(conf.getTimeout()).as("default timeout").isEqualTo(600000);
    configuration.setProperty(FindbugsConstants.TIMEOUT_PROPERTY, 1);
    assertThat(conf.getTimeout()).isEqualTo(1);
  }

  @Test
  void should_return_excludes_filters() {
    assertThat(conf.getExcludesFilters()).isEmpty();
    configuration.setProperty(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY, " foo.xml , bar.xml,");
    assertThat(conf.getExcludesFilters()).hasSize(2);
  }

  @Test
  void should_return_confidence_level() {
    assertThat(conf.getConfidenceLevel()).as("default confidence level").isEqualTo("medium");
    configuration.setProperty(FindbugsConstants.EFFORT_PROPERTY, "HIGH");
    assertThat(conf.getEffort()).isEqualTo("high");
  }

  @Test
  void should_set_class_files() throws IOException {
    File file = new File(temp, "MyClass.class");
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Collections.singletonList(file));
    try (Project findbugsProject = new Project()) {
      conf.initializeFindbugsProject(findbugsProject);
      
      assertThat(findbugsProject.getFileList()).containsOnly(file.getCanonicalPath());
    }
  }

  @Test
  void should_set_class_path() throws IOException {
    File classpath = new File(temp, "classpath");
    when(javaResourceLocator.classpath()).thenReturn(Collections.singletonList(classpath));
    try (Project findbugsProject = new Project()) {
      conf.initializeFindbugsProject(findbugsProject);

      assertThat(findbugsProject.getAuxClasspathEntryList()).contains(classpath.getCanonicalPath());
    }
  }
  
  @Test
  public void should_get_only_analyze_filter() {
	 // No onlyAnalyze option present 
	 assertNull(conf.getOnlyAnalyzeFilter());
	 // Empty Property
	 configuration.setProperty(FindbugsConstants.ONLY_ANALYZE_PROPERTY, "");
	 assertNull(conf.getOnlyAnalyzeFilter());
	 
	 // Screener made correctly for class files
	 configuration.setProperty(FindbugsConstants.ONLY_ANALYZE_PROPERTY, "com.example.Test");
	 ClassScreener expected = conf.getOnlyAnalyzeFilter();
	 assertNotNull(expected);	
	 assertTrue(expected.matches("any/random/src/main/java/com/exam"
	 		+ "ple/Test.class"));
	 
	 // Screener made correctly for package
	 configuration.setProperty(FindbugsConstants.ONLY_ANALYZE_PROPERTY, "com.example.*");
	 expected = conf.getOnlyAnalyzeFilter();
	 assertNotNull(expected);
	 assertTrue(expected.matches("any/random/src/main/java/com/exam"
	 		+ "ple/Test.class"));
	 assertTrue(expected.matches("any/random/src/main/java/com/exam"
		 		+ "ple/Test2.class"));
	 
	 // Screener made correctly for deep match
	 configuration.setProperty(FindbugsConstants.ONLY_ANALYZE_PROPERTY, "com.example.-");
	 expected = conf.getOnlyAnalyzeFilter();
	 assertNotNull(expected);	
	 assertTrue(expected.matches("any/random/src/main/java/com/exam"
		 		+ "ple/Test1.class"));
	 assertTrue(expected.matches("any/random/src/main/java/com/exam"
		 		+ "ple/innerPackage/Test2.class"));
	 // To prevent other test to fail
	 configuration.setProperty(FindbugsConstants.ONLY_ANALYZE_PROPERTY, "");
	 
  }

  @Test
  void scanEmptyFolderForAdditionalClasses() throws IOException {
    List<File> classes = FindbugsConfiguration.scanForAdditionalClasses(temp, f -> true);
    
    assertThat(classes).isEmpty();
  }
  
  @Test
  void should_warn_of_missing_precompiled_jsp() throws IOException {
    setupSampleProject(false, true, false);
    
    try (Project project = new Project()) {
      conf.initializeFindbugsProject(project);
    }
    
    // There should be two warnings:
    //  - There are JSP but they are not precompiled
    //  - Findbugs needs sources to be compiled
    assertThat(logTester.getLogs(LoggerLevel.WARN)).hasSize(2);
  }
  
  @Test
  void should_analyze_precompiled_jsp() throws IOException {
    setupSampleProject(true, true, false);
    
    try (Project project = new Project()) {
      conf.initializeFindbugsProject(project);
      
      assertThat(project.getFileCount()).isEqualTo(3);
    }
    
    assertThat(logTester.getLogs(LoggerLevel.WARN)).isNull();
  }
  
  @Test
  void scala_project() throws IOException {
    setupSampleProject(false, false, true);
    
    try (Project project = new Project()) {
      conf.initializeFindbugsProject(project);
      
      assertThat(project.getFileCount()).isEqualTo(1);
      assertThat(project.getFile(0)).endsWith("Test.class");
    }
    
    assertThat(logTester.getLogs(LoggerLevel.WARN)).isNull();
  }

  private void setupSampleProject(boolean withPrecompiledJsp, boolean withJspFiles, boolean withScalaFiles) throws IOException {
    mockHasLanguagePredicate(withJspFiles, "jsp");
    mockHasLanguagesPredicate(withScalaFiles, "scala");
    
    // classpath
    // |_ some.jar
    // |_ target
    //   |_ jsp_servlet
    //   |_ classes
    //     |_ module-info.class
    //     |_ package
    //       |_ Test.class
    //       |_ message.txt
    File classpath = new File(temp, "classpath");
    File jarFile = new File(classpath, "some.jar");
    File targetFolder = new File(classpath, "target");
    File classesFolder = new File(targetFolder, "classes");
    File jspServletFolder = new File(targetFolder, "jsp_servlet");
    File packageFolder = new File(classesFolder, "package");
    File classFile = new File(packageFolder, "Test.class");
    File txtFile = new File(packageFolder, "message.txt");
    File moduleInfoFile = new File(classesFolder, "module-info.class");
    
    Files.createDirectories(jspServletFolder.toPath());
    Files.createDirectories(packageFolder.toPath());
    Files.createFile(jarFile.toPath());
    Files.createFile(classFile.toPath());
    Files.createFile(txtFile.toPath());
    Files.createFile(moduleInfoFile.toPath());
    
    if (withPrecompiledJsp) {
      File jspClassFile = new File(packageFolder, "page1_jsp.class");
      File jspInnerClassFile = new File(packageFolder, "page1_jsp$1.class");
      File weblogicJspClassFile = new File(jspServletFolder, "weblogic.class");
      
      Files.createFile(jspClassFile.toPath());
      Files.createFile(jspInnerClassFile.toPath());
      Files.createFile(weblogicJspClassFile.toPath());
    }
    
    List<File> classpathFiles = Arrays.asList(jarFile, classesFolder, jspServletFolder, moduleInfoFile);
    
    when(javaResourceLocator.classpath()).thenReturn(classpathFiles);
  }

  private void mockHasLanguagePredicate(boolean predicateReturn, String language) {
    FilePredicate languagePredicate = mock(FilePredicate.class);
    when(filePredicates.hasLanguage(language)).thenReturn(languagePredicate);
    when(fs.hasFiles(languagePredicate)).thenReturn(predicateReturn);
  }

  private void mockHasLanguagesPredicate(boolean predicateReturn, String ... languages) {
    // First mock the calls to hasLanguage() for individual languages
    for (String language : languages) {
      mockHasLanguagePredicate(predicateReturn, language);
    }
    
    // Then mock the call to the vararg method hasLanguages()
    when(filePredicates.hasLanguages(Mockito.<String>any())).thenAnswer(i -> {
      Object[] arguments = i.getArguments();
      for (int j = 0; j < arguments.length; j++) {
        String language = (String) arguments[j];
        
        if (fs.hasFiles(filePredicates.hasLanguage(language))) {
          return filePredicates.hasLanguage(language);
        }
      }
      
      return (FilePredicate) file -> false;
    });
  }
  
  @Test
  void buildMissingCompiledCodeException() {
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Collections.emptyList());
    when(javaResourceLocator.classpath()).thenReturn(Collections.emptyList());
    
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).contains("Property sonar.java.binaries was not set");
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).contains("Sonar JavaResourceLocator.classpath was empty");
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).contains("Sonar JavaResourceLocator.classFilesToAnalyze was empty");
    
    configuration.setProperty(FindbugsConfiguration.SONAR_JAVA_BINARIES, "foo/bar");
    
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).contains("sonar.java.binaries was set to");
    
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Collections.singletonList(baseDir));
    when(javaResourceLocator.classpath()).thenReturn(Collections.singletonList(baseDir));
    
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).doesNotContain("Sonar JavaResourceLocator.classpath was empty");
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).doesNotContain("Sonar JavaResourceLocator.classFilesToAnalyze was empty");
  }
}
