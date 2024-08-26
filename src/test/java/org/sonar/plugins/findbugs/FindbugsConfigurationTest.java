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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.findbugs.classpath.ClasspathLocator;
import org.sonar.plugins.findbugs.configuration.SimpleConfiguration;
import org.sonar.plugins.findbugs.rule.FakeActiveRules;

import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.ClassScreener;
import edu.umd.cs.findbugs.Project;

class FindbugsConfigurationTest {

  @TempDir
  public File temp;
  
  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private FilePredicates filePredicates;
  private FileSystem fs;
  private SimpleConfiguration configuration;
  private File baseDir;
  private File workDir;
  private ActiveRules activeRules;
  private FindbugsConfiguration conf;
  private ClasspathLocator classpathLocator;

  @BeforeEach
  public void setUp() {
    baseDir = new File(temp, "findbugs");
    workDir = new File(temp, "findbugs");

    filePredicates = mock(FilePredicates.class);
    
    fs = mock(FileSystem.class);
    when(fs.baseDir()).thenReturn(baseDir);
    when(fs.workDir()).thenReturn(workDir);
    when(fs.predicates()).thenReturn(filePredicates);
    
    activeRules = FakeActiveRules.createWithOnlyFindbugsRules();

    configuration = new SimpleConfiguration();
    classpathLocator = mock(ClasspathLocator.class);
    conf = new FindbugsConfiguration(fs, configuration, activeRules, classpathLocator);
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
    File classesDir = new File(temp, "xyz");
    File file = new File(classesDir, "MyClass.class");

    Files.createDirectories(classesDir.toPath());
    Files.createFile(file.toPath());
    
    when(classpathLocator.binaryDirs()).thenReturn(ImmutableList.of(classesDir));
    try (Project findbugsProject = new Project()) {
      conf.initializeFindbugsProject(findbugsProject);
      
      assertThat(findbugsProject.getFileList()).containsOnly(file.getCanonicalPath());
      conf.stop();
    }
  }

  @Test
  void should_set_class_path() throws IOException {
    File classpath = new File(temp, "classpath");
    when(classpathLocator.classpath()).thenReturn(ImmutableList.of(classpath));
    try (Project findbugsProject = new Project()) {
      conf.initializeFindbugsProject(findbugsProject);

      assertThat(findbugsProject.getAuxClasspathEntryList()).contains(classpath.getCanonicalPath());
      conf.stop();
    }
  }

  @Test
  void should_copy_lib_in_working_dir() {
    String jsr305 = "findbugs/jsr305.jar";
    String annotations = "findbugs/annotations.jar";

    // stop at start
    conf.stop();
    assertThat(new File(fs.workDir(), jsr305)).doesNotExist();
    assertThat(new File(fs.workDir(), annotations)).doesNotExist();

    conf.copyLibs();
    assertThat(new File(fs.workDir(), jsr305)).isFile();
    assertThat(new File(fs.workDir(), annotations)).isFile();

    // copy again
    conf.copyLibs();
    assertThat(new File(fs.workDir(), jsr305)).isFile();
    assertThat(new File(fs.workDir(), annotations)).isFile();

    conf.stop();
    assertThat(new File(fs.workDir(), jsr305)).doesNotExist();
    assertThat(new File(fs.workDir(), annotations)).doesNotExist();

  }

  @Test
  void should_get_fbcontrib() {
    conf.copyLibs();
    assertThat(conf.getFbContribJar()).isFile();
  }

  @Test
  void should_get_findSecBugs() {
    conf.copyLibs();
    assertThat(conf.getFindSecBugsJar()).isFile();
  }
  
  @Test
  void should_get_only_analyze_filter() {
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
  void scanEmptyFolderForAdditionalClasses() {
    List<File> classes = FindbugsConfiguration.scanForAdditionalClasses(temp, f -> true);
    
    assertThat(classes).isEmpty();
  }
  
  @ParameterizedTest
  @CsvSource({
    "true",
    "false",
  })
  void should_warn_of_missing_precompiled_jsp(boolean analyzeTests) throws IOException {
    setupSampleProject(false, true, false, analyzeTests);
    
    try (Project project = new Project()) {
      conf.initializeFindbugsProject(project, classpathLocator);
    }
    
    assertThat(logTester.getLogs(Level.WARN)).hasSize(1);
  }

  @ParameterizedTest
  @CsvSource({
    "true",
    "false",
  })
  void should_analyze_precompiled_jsp(boolean analyzeTests) throws IOException {
    setupSampleProject(true, true, false, analyzeTests);
    
    try (Project project = new Project()) {
      conf.initializeFindbugsProject(project, classpathLocator);
      
      if (analyzeTests) {
        // we should also capture the .class that are not from JSP sources and also the unit tests
        assertThat(project.getFileCount()).isEqualTo(5);
        
        verify(classpathLocator, times(1)).testClasspath();
      } else {
        // we should also capture the .class that are not from JSP sources but not the unit tests
        assertThat(project.getFileCount()).isEqualTo(4);
        
        verify(classpathLocator, never()).testClasspath();
      }
    }
    
    assertThat(logTester.getLogs(Level.WARN)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "true",
    "false",
  })
  void scala_project(boolean analyzeTests) throws IOException {
    setupSampleProject(false, false, true, analyzeTests);
    
    try (Project project = new Project()) {
      conf.initializeFindbugsProject(project, classpathLocator);
      
      if (analyzeTests) {
        assertThat(project.getFileCount()).isEqualTo(2);
        assertThat(project.getFile(0)).endsWith("Test.class");
        assertThat(project.getFile(1)).endsWith("UnitTest.class");
        
        verify(classpathLocator, times(1)).testClasspath();
      } else {
        assertThat(project.getFileCount()).isEqualTo(1);
        // Even though it is named "Test" it is in the "main" folder so it should be analyzed
        assertThat(project.getFile(0)).endsWith("Test.class");
        
        verify(classpathLocator, never()).testClasspath();
      }
    }
    
    assertThat(logTester.getLogs(Level.WARN)).isEmpty();
  }

  private void setupSampleProject(boolean withPrecompiledJsp,
      boolean withJspFiles,
      boolean withScalaFiles,
      boolean analyzeTests) throws IOException {
    configuration.setProperty(FindbugsConstants.ANALYZE_TESTS, Boolean.toString(analyzeTests));
    
    mockHasLanguagePredicate(withJspFiles, "jsp");
    mockHasLanguagesPredicate(withScalaFiles, "scala");
    
    // classpath
    // |_ some.jar
    // |_ some-test.jar
    // |_ some-other-test.jar
    // |_ target
    //   |_ jsp_servlet
    //   |_ classes
    //     |_ module-info.class
    //     |_ package
    //       |_ Test.class
    //       |_ message.txt
    //   |_ test-classes
    //     |_ package
    //       |_ UnitTest.class
    File classpath = new File(temp, "classpath");
    File jarFile = new File(classpath, "some.jar");
    File testJarFile = new File(classpath, "some-test.jar");
    File testOtherJarFile = new File(classpath, "some-other-test.jar");
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
    
    // test binaries
    File testClassesFolder = new File(targetFolder, "test-classes");
    File testPackageFolder = new File(testClassesFolder, "package");
    File unitTestClassFile = new File(testPackageFolder, "UnitTest.class");
    
    Files.createDirectories(testPackageFolder.toPath());
    Files.createFile(unitTestClassFile.toPath());
    
    if (withPrecompiledJsp) {
      File jspClassFile = new File(packageFolder, "page1_jsp.class");
      File jspInnerClassFile = new File(packageFolder, "page1_jsp$1.class");
      File weblogicJspClassFile = new File(jspServletFolder, "weblogic.class");
      
      Files.createFile(jspClassFile.toPath());
      Files.createFile(jspInnerClassFile.toPath());
      Files.createFile(weblogicJspClassFile.toPath());
    }
    
    List<File> classpathFiles = Arrays.asList(jarFile, classesFolder, jspServletFolder, moduleInfoFile);
    List<File> testClasspathFiles = Arrays.asList(testJarFile, testOtherJarFile);
    
    when(classpathLocator.classpath()).thenReturn(classpathFiles);
    when(classpathLocator.testClasspath()).thenReturn(testClasspathFiles);
    
    List<File> binaryDirs = Arrays.asList(classesFolder, jspServletFolder);
    List<File> testBinaryDirs = Collections.singletonList(testClassesFolder);

    when(classpathLocator.binaryDirs()).thenReturn(binaryDirs);
    when(classpathLocator.testBinaryDirs()).thenReturn(testBinaryDirs);
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
    when(classpathLocator.classpath()).thenReturn(Collections.emptyList());
    
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).contains("Property sonar.java.binaries was not set");
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).contains("Sonar JavaResourceLocator.classpath was empty");
    
    configuration.setProperty(FindbugsConfiguration.SONAR_JAVA_BINARIES, "foo/bar");
    
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).contains("sonar.java.binaries was set to");
    
    when(classpathLocator.classpath()).thenReturn(Collections.singletonList(baseDir));
    
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).doesNotContain("Sonar JavaResourceLocator.classpath was empty");
    assertThat(conf.buildMissingCompiledCodeException().getMessage()).doesNotContain("Sonar JavaResourceLocator.classFilesToAnalyze was empty");
  }
}
