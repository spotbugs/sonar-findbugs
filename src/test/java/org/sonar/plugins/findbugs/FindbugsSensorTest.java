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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.findbugs.classpath.ClasspathLocator;
import org.sonar.plugins.findbugs.resource.ByteCodeResourceLocator;
import org.sonar.plugins.findbugs.resource.SmapParser.FileInfo;
import org.sonar.plugins.findbugs.resource.SmapParser.SmapLocation;
import org.sonar.plugins.findbugs.rule.FakeActiveRules;

import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;

class FindbugsSensorTest extends FindbugsTests {
  @TempDir
  public File temp;

  private FileSystem fs;
  private ByteCodeResourceLocator byteCodeResourceLocator;
  private MutablePicoContainer pico;
  private SensorContext sensorContext;
  private FindbugsExecutor executor;
  private ClasspathLocator classpathLocator;

  @BeforeEach
  public void setUp() throws IOException {
    sensorContext = mock(SensorContext.class);
    byteCodeResourceLocator = mock(ByteCodeResourceLocator.class);
    executor = mock(FindbugsExecutor.class);
    classpathLocator = mock(ClasspathLocator.class);
    
    File baseDir = new File(temp, "findbugs");

    FilePredicate relativePathFilePredicate = mock(FilePredicate.class);
    when(relativePathFilePredicate.apply(any(InputFile.class))).thenReturn(true);
    
    FilePredicates filePredicates = mock(FilePredicates.class);
    when(filePredicates.hasRelativePath(any(String.class))).thenReturn(relativePathFilePredicate);
    
    fs = mock(FileSystem.class);
    when(fs.baseDir()).thenReturn(baseDir);
    when(fs.workDir()).thenReturn(new File(temp, "workdir"));
    when(fs.predicates()).thenReturn(filePredicates);

    InputFile dummyFile = mock(InputFile.class);
    when(dummyFile.relativePath()).thenReturn("src/main/java/com/helloworld/DummyFile.java");
    //Will make sure that the lookup on the filesystem will always find a file.
    when(fs.inputFiles(any(FilePredicate.class))).thenReturn(Arrays.asList(dummyFile));

    pico = new DefaultPicoContainer();

    //Common components are defined in the setup. This way they don't have to be defined in every test.
    pico.addComponent(fs);
    pico.addComponent(byteCodeResourceLocator);
    pico.addComponent(FindbugsSensor.class);
    pico.addComponent(sensorContext);


    //Stub NewIssue builder when a new issue is raised
    NewIssue newIssue = mock(NewIssue.class);
    when(newIssue.forRule(any(RuleKey.class))).thenReturn(newIssue);

    NewIssueLocation newIssueLocation = mock(NewIssueLocation.class);
    when(newIssue.newLocation()).thenReturn(newIssueLocation);
    when(newIssueLocation.at(any(TextRange.class))).thenReturn(newIssueLocation);
    when(newIssueLocation.on(any(InputComponent.class))).thenReturn(newIssueLocation);
    //--

    when(sensorContext.newIssue()).thenReturn(newIssue);

    NewAnalysisError newAnalysisError = mock(NewAnalysisError.class);
    when(sensorContext.newAnalysisError()).thenReturn(newAnalysisError);

    pico.addComponent(executor);
    pico.addComponent(classpathLocator);
  }

  @Test
  void should_execute_findbugs() throws Exception {

    BugInstance bugInstance = getBugInstance("AM_CREATES_EMPTY_ZIP_FILE_ENTRY", 6, true);
    when(executor.execute(false, false)).thenReturn(new AnalysisResult(bugInstance));

    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, times(1)).newIssue();
  }

  @Test
  void should_not_add_issue_if_resource_not_found() throws Exception {

    BugInstance bugInstance = getBugInstance("AM_CREATES_EMPTY_ZIP_FILE_ENTRY", 13, false);
    when(executor.execute(false, false)).thenReturn(new AnalysisResult(bugInstance));

    when(fs.inputFiles(any(FilePredicate.class))).thenReturn(new ArrayList<InputFile>());

    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());
    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }


  @Test
  void should_execute_findbugs_even_if_only_fbcontrib() throws Exception {

    BugInstance bugInstance = getBugInstance("ISB_INEFFICIENT_STRING_BUFFERING", 49, true);
    when(executor.execute(true, false)).thenReturn(new AnalysisResult(bugInstance));

    pico.addComponent(FakeActiveRules.createWithOnlyFbContribRules());

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(true, false);
    verify(sensorContext, times(1)).newIssue();
  }

  @Test
  void should_execute_findbugs_even_if_only_findsecbug() throws Exception {

    BugInstance bugInstance = getBugInstance("PREDICTABLE_RANDOM", 0, true);
    when(executor.execute(false, true)).thenReturn(new AnalysisResult(bugInstance));

    pico.addComponent(FakeActiveRules.createWithOnlyFindSecBugsRules());

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(false, true);
    verify(sensorContext, times(1)).newIssue();
  }

  @Test
  void should_execute_findbugs_but_not_find_violation() throws Exception {

    BugInstance bugInstance = getBugInstance("THIS_RULE_DOES_NOT_EXIST", 107, true);
    when(executor.execute(false, false)).thenReturn(new AnalysisResult(bugInstance));

    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }

  @Test
  void should_not_execute_findbugs_if_no_active() throws Exception {

    pico.addComponent(FakeActiveRules.createWithNoRules());

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor, never()).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }

  /**
   * When the users want to disable SpotBugs on a project they usually disable the java rules but do not realize that
   * their default JSP profile has some Find Sec Bugs JSP rules. SonarQube's ActiveRule will return these JSP rules
   * because it selects a profile for every language installed on the server (typically the default profile)
   */
  @Test
  void should_not_execute_findbugs_if_only_jsp_rules_and_no_jsp_file() throws Exception {
    TreeSet<String> languages = new TreeSet<>(Arrays.asList("java", "xml"));
    when(fs.languages()).thenReturn(languages);

    pico.addComponent(FakeActiveRules.createWithOnlyFindSecBugsJspRules());

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor, never()).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }
  
  /**
   * Check that the Find Sec Bugs analysis still runs when there are some JSP rules and files
   */
  @Test
  void should_execute_findbugs_if_only_jsp_rules_and_some_jsp_files() throws Exception {
    TreeSet<String> languages = new TreeSet<>(Arrays.asList("java", "xml", "jsp"));
    when(fs.languages()).thenReturn(languages);
    
    when(executor.execute(false, true)).thenReturn(new AnalysisResult());

    pico.addComponent(FakeActiveRules.createWithOnlyFindSecBugsJspRules());

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(false, true);
    verify(sensorContext, never()).newIssue();
  }
  
  @Test
  void should_execute_findbugs_with_missing_smap_and_source() throws Exception {
    BugInstance bugInstance = getBugInstance("AM_CREATES_EMPTY_ZIP_FILE_ENTRY", 6, true);
    when(executor.execute(false, false)).thenReturn(new AnalysisResult(bugInstance));
    
    // return a class file that does not have SMAP (doesn't exist actually)
    when(byteCodeResourceLocator.findClassFileByClassName("org.sonar.commons.ZipUtils", this.classpathLocator)).thenReturn("");
    // don't return a source file: the input file is not in the file system even if SpotBugs found an issue in a class file
    when(byteCodeResourceLocator.findSourceFile("org/sonar/commons/org/sonar/commons/ZipUtils.java", fs)).thenReturn(null);
    
    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }
  
  @Test
  void should_execute_findbugs_with_smap() throws Exception {
    BugInstance bugInstance = getBugInstance("AM_CREATES_EMPTY_ZIP_FILE_ENTRY", 6, true);
    when(executor.execute(false, false)).thenReturn(new AnalysisResult(bugInstance));
    
    String classFileName = "org/sonar/commons/ZipUtils.class";
    
    when(byteCodeResourceLocator.findClassFileByClassName("org.sonar.commons.ZipUtils", this.classpathLocator)).thenReturn(classFileName);
    
    // Return a valid SMAP location
    FileInfo fileInfo = new FileInfo("ZipUtils", "org/sonar/commons/org/sonar/commons/ZipUtils.java");
    SmapLocation smapLocation = new SmapLocation(fileInfo, 6, true);
    when(byteCodeResourceLocator.extractSmapLocation("org.sonar.commons.ZipUtils", 6, new File(classFileName))).thenReturn(smapLocation);
    
    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, times(1)).newIssue();
  }

  private BugInstance getBugInstance(String name, int line, boolean mockFindSourceFile) {
    BugInstance bugInstance = new BugInstance(name, 2);
    String className = "org.sonar.commons.ZipUtils";
    String sourceFile = "org/sonar/commons/ZipUtils.java";
    ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFile);
    bugInstance.add(classAnnotation);
    MethodAnnotation methodAnnotation = new MethodAnnotation(className, "_zip", "(Ljava/lang/String;Ljava/io/File;Ljava/util/zip/ZipOutputStream;)V", true);
    methodAnnotation.setSourceLines(new SourceLineAnnotation(className, sourceFile, line, 0, 0, 0));
    bugInstance.add(methodAnnotation);
    
    if (mockFindSourceFile) {
      InputFile resource = mock(InputFile.class);
      TextRange textRange = mock(TextRange.class);

      ReportedBug reportedBug = new ReportedBug(bugInstance);
      when(byteCodeResourceLocator.findSourceFile(reportedBug.getSourceFile(), fs)).thenReturn(resource);
      when(resource.selectLine(line > 0 ? line : 1)).thenReturn(textRange);
    }
    
    return bugInstance;
  }

  @Test
  void shouldIgnoreNotActiveViolations() throws Exception {
    BugInstance bugInstance = new BugInstance("UNKNOWN", 2);
    String className = "org.sonar.commons.ZipUtils";
    String sourceFile = "org/sonar/commons/ZipUtils.java";
    ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFile);
    bugInstance.add(classAnnotation);

    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());
    when(executor.execute(false, false)).thenReturn(new AnalysisResult(bugInstance));
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(sensorContext, never()).newIssue();
  }

  @Test
  void describe() {
    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    
    sensor.describe(descriptor);
    
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(descriptor).name(argument.capture());
    assertEquals("FindBugs Sensor", argument.getValue());
  }
  
  @Test
  void shouldReportAnalysisError() throws Exception {
    Collection<AnalysisError> analysisErrors = Arrays.asList(
        new AnalysisError("Only a message"),
        new AnalysisError("A message and an exception", new UnsupportedOperationException("Unsupported"))
        );
    
    AnalysisResult analysisResult = new AnalysisResult(Collections.emptyList(), analysisErrors);

    pico.addComponent(FakeActiveRules.createWithOnlyFindbugsRules());
    when(executor.execute(false, false)).thenReturn(analysisResult);
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(sensorContext, never()).newIssue();
    verify(sensorContext, times(2)).newAnalysisError();
  }
}
