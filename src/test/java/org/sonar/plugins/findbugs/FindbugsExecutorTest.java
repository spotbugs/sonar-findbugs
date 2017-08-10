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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsExecutorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  FileSystem fsEmpty;
  FilePredicates predicatesEmpty;

  @Before
  public void setUp() {
    fsEmpty = mock(FileSystem.class);
    predicatesEmpty = mock(FilePredicates.class);
    when(fsEmpty.baseDir()).thenReturn(new File("./"));
    when(fsEmpty.predicates()).thenReturn(predicatesEmpty);
    when(fsEmpty.inputFiles(any(FilePredicate.class))).thenReturn(new ArrayList<InputFile>());
  }

  @Test
  public void canGenerateXMLReport() throws Exception {
    FindbugsConfiguration conf = mockConf();

    File reportFile = temporaryFolder.newFile("findbugs-report.xml");
    when(conf.getTargetXMLReport()).thenReturn(reportFile);

    new FindbugsExecutor(conf, fsEmpty).execute();

    assertThat(reportFile.exists()).isTrue();
    String report = FileUtils.readFileToString(reportFile);
    assertThat(report).as("Report should contain bug instance").contains("<BugInstance");
    assertThat(report).as("Report should be generated with messages").contains("<Message>");
    assertThat(report).contains("priority=\"1\"");
    assertThat(report).doesNotContain("priority=\"3\"");
  }

  @Test
  public void canGenerateXMLReportWithCustomConfidence() throws Exception {
    FindbugsConfiguration conf = mockConf();
    File reportFile = temporaryFolder.newFile("customized-findbugs-report.xml");
    when(conf.getTargetXMLReport()).thenReturn(reportFile);
    when(conf.getConfidenceLevel()).thenReturn("low");

    new FindbugsExecutor(conf, fsEmpty).execute();

    assertThat(reportFile.exists()).isTrue();
    String report = FileUtils.readFileToString(reportFile);
    assertThat(report).as("Report should contain bug instance").contains("<BugInstance");
    assertThat(report).as("Report should be generated with messages").contains("<Message>");
    assertThat(report).contains("priority=\"1\"");
    assertThat(report).contains("priority=\"3\"");
  }

  @Test(expected = IllegalStateException.class)
  public void shouldTerminateAfterTimeout() throws Exception {
    FindbugsConfiguration conf = mockConf();
    when(conf.getTimeout()).thenReturn(1L);

    new FindbugsExecutor(conf, fsEmpty).execute();
  }

  @Test(expected = IllegalStateException.class)
  public void shoulFailIfNoCompiledClasses() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem(new File("."));
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY, Locale.getDefault().getDisplayName());
    FindbugsConfiguration conf = new FindbugsConfiguration(fs, settings, null, null, null);

    new FindbugsExecutor(conf, fsEmpty).execute();
  }

  private FindbugsConfiguration mockConf() throws Exception {
    FindbugsConfiguration conf = mock(FindbugsConfiguration.class);
    edu.umd.cs.findbugs.Project project = new edu.umd.cs.findbugs.Project();
    project.addFile(new File("test-resources/classes").getCanonicalPath());
    project.addSourceDir(new File("test-resources/src").getCanonicalPath());
    project.setCurrentWorkingDirectory(new File("test-resources"));
    when(conf.getFindbugsProject()).thenReturn(project);
    when(conf.saveIncludeConfigXml()).thenReturn(new File("test-resources/findbugs-include.xml"));
    when(conf.getExcludesFilters()).thenReturn(Lists.newArrayList(new File("test-resources/findbugs-exclude.xml"), new File("test-resources/fake-file.xml")));
    when(conf.getEffort()).thenReturn("default");
    when(conf.getTimeout()).thenReturn(FindbugsConstants.TIMEOUT_DEFAULT_VALUE);
    return conf;
  }

}
