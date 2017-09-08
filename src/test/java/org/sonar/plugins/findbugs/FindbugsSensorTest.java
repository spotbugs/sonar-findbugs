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

import com.google.common.io.Files;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.findbugs.resource.ByteCodeResourceLocator;
import org.sonar.plugins.findbugs.rule.FakeActiveRules;
import org.sonar.plugins.java.api.JavaResourceLocator;

import com.google.common.collect.Lists;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class FindbugsSensorTest extends FindbugsTests {

  DefaultFileSystem fs;
  private ByteCodeResourceLocator byteCodeResourceLocator;
  private MutablePicoContainer pico;
  private SensorContext sensorContext;
  private FindbugsExecutor executor;
  private JavaResourceLocator javaResourceLocator;

  @Before
  public void setUp() {
    sensorContext = mock(SensorContext.class);
    byteCodeResourceLocator = new ByteCodeResourceLocator();
    executor = mock(FindbugsExecutor.class);
    javaResourceLocator = mockJavaResourceLocator();

    DefaultFileSystem dfs = new DefaultFileSystem(new File("."));
    dfs.setWorkDir(Files.createTempDir());
    fs = spy(dfs);

    InputFile dummyFile = mock(InputFile.class);
    when(dummyFile.relativePath()).thenReturn("src/main/java/com/helloworld/DummyFile.java");
    //Will make sure that the lookup on the filesystem will always find a file.
    when(fs.inputFiles(any(FilePredicate.class))).thenReturn(Arrays.asList(dummyFile));
    File tempDir = Files.createTempDir();
    when(fs.workDir()).then(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        return tempDir;
      }
    });
    //when(fs.workDir()).thenReturn(tempDir);

    pico = new DefaultPicoContainer();

    //Common components are defined in the setup. This way they don't have to be defined in every test.
    pico.addComponent(fs);
    pico.addComponent(byteCodeResourceLocator);
    pico.addComponent(FakeActiveRules.createWithAllRules());
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

    pico.addComponent(executor);
    pico.addComponent(javaResourceLocator);
  }

  private static JavaResourceLocator mockJavaResourceLocator() {
    JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
    InputFile resource = mock(InputFile.class);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    return javaResourceLocator;
  }

  @Test
  public void should_execute_findbugs() throws Exception {

    BugInstance bugInstance = getBugInstance("AM_CREATES_EMPTY_ZIP_FILE_ENTRY", 6);
    Collection<ReportedBug> collection = Arrays.asList(new ReportedBug(bugInstance));
    when(executor.execute(false, false)).thenReturn(collection);
    JavaResourceLocator javaResourceLocator = mockJavaResourceLocator();
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Lists.newArrayList(new File("file")));

    pico.addComponent(createRulesProfileWithActiveRules());
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, times(1)).newIssue();
  }

  @Test
  public void should_not_add_issue_if_resource_not_found() throws Exception {

    BugInstance bugInstance = getBugInstance("AM_CREATES_EMPTY_ZIP_FILE_ENTRY", 13);
    Collection<ReportedBug> collection = Arrays.asList(new ReportedBug(bugInstance));
    when(executor.execute(false, false)).thenReturn(collection);

    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(null);
    when(fs.inputFiles(any(FilePredicate.class))).thenReturn(new ArrayList<InputFile>());
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Lists.newArrayList(new File("file")));

    pico.addComponent(createRulesProfileWithActiveRules());
    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }


  @Test
  public void should_execute_findbugs_even_if_only_fbcontrib() throws Exception {

    BugInstance bugInstance = getBugInstance("ISB_INEFFICIENT_STRING_BUFFERING", 49);
    Collection<ReportedBug> collection = Arrays.asList(new ReportedBug(bugInstance));
    when(executor.execute(true, false)).thenReturn(collection);
    JavaResourceLocator javaResourceLocator = mockJavaResourceLocator();
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Lists.newArrayList(new File("file")));

    pico.addComponent(createRulesProfileWithActiveRules(false, true, false, false));

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(true, false);
    verify(sensorContext, times(1)).newIssue();
  }

  @Test
  public void should_execute_findbugs_even_if_only_findsecbug() throws Exception {

    BugInstance bugInstance = getBugInstance("PREDICTABLE_RANDOM", 0);
    Collection<ReportedBug> collection = Arrays.asList(new ReportedBug(bugInstance));
    when(executor.execute(false, true)).thenReturn(collection);

    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Lists.newArrayList(new File("file")));

    pico.addComponent(createRulesProfileWithActiveRules(false, false, true, false));

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(false, true);
    verify(sensorContext, times(1)).newIssue();
  }

  @Test
  public void should_execute_findbugs_but_not_find_violation() throws Exception {

    BugInstance bugInstance = getBugInstance("THIS_RULE_DOES_NOT_EXIST", 107);
    Collection<ReportedBug> collection = Arrays.asList(new ReportedBug(bugInstance));
    when(executor.execute(false, false)).thenReturn(collection);

    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Lists.newArrayList(new File("file")));

    pico.addComponent(createRulesProfileWithActiveRules(true, false, false, false));

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }

  @Test
  public void should_not_execute_findbugs_if_no_active() throws Exception {

    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Lists.newArrayList(new File("file")));

    pico.addComponent(createRulesProfileWithActiveRules(false, false, false, false));

    FindbugsSensor analyser = pico.getComponent(FindbugsSensor.class);
    analyser.execute(sensorContext);

    verify(executor, never()).execute(false, false);
    verify(sensorContext, never()).newIssue();
  }

  private BugInstance getBugInstance(String name, int line) {
    BugInstance bugInstance = new BugInstance(name, 2);
    String className = "org.sonar.commons.ZipUtils";
    String sourceFile = "org/sonar/commons/ZipUtils.java";
    ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFile);
    bugInstance.add(classAnnotation);
    MethodAnnotation methodAnnotation = new MethodAnnotation(className, "_zip", "(Ljava/lang/String;Ljava/io/File;Ljava/util/zip/ZipOutputStream;)V", true);
    methodAnnotation.setSourceLines(new SourceLineAnnotation(className, sourceFile, line, 0, 0, 0));
    bugInstance.add(methodAnnotation);
    return bugInstance;
  }

  @Test
  public void should_not_execute_if_no_compiled_class_available() throws Exception {
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(Collections.<File>emptyList());

    pico.addComponent(createRulesProfileWithActiveRules());

    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(executor, never()).execute();
  }

  @Test
  public void shouldIgnoreNotActiveViolations() throws Exception {
    BugInstance bugInstance = new BugInstance("UNKNOWN", 2);
    String className = "org.sonar.commons.ZipUtils";
    String sourceFile = "org/sonar/commons/ZipUtils.java";
    ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFile);
    bugInstance.add(classAnnotation);
    Collection<ReportedBug> collection = Arrays.asList(new ReportedBug(bugInstance));
    when(executor.execute()).thenReturn(collection);

    pico.addComponent(createRulesProfileWithActiveRules());
    FindbugsSensor sensor = pico.getComponent(FindbugsSensor.class);
    sensor.execute(sensorContext);

    verify(sensorContext, never()).newIssue();
  }


}
