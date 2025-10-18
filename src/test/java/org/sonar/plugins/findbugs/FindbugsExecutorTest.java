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

import edu.umd.cs.findbugs.Project;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.plugins.findbugs.configuration.SimpleConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindbugsExecutorTest {
  
  @TempDir
  public File temporaryFolder;

  FileSystem fsEmpty;
  FilePredicates predicatesEmpty;

  Configuration configEmpty;

  @BeforeEach
  public void setUp() {
    fsEmpty = mock(FileSystem.class);
    predicatesEmpty = mock(FilePredicates.class);
    when(fsEmpty.baseDir()).thenReturn(new File("./"));
    when(fsEmpty.predicates()).thenReturn(predicatesEmpty);
    when(fsEmpty.inputFiles(any(FilePredicate.class))).thenReturn(new ArrayList<InputFile>());

    configEmpty = mock(Configuration.class);
    when(configEmpty.getStringArray(any())).thenReturn(new String[0]);
    when(configEmpty.get(any())).thenReturn(Optional.of(""));
  }

  @Test
  void canGenerateXMLReport() throws Exception {
    FindbugsConfiguration conf = mockConf();

    File reportFile = new File(temporaryFolder, "findbugs-report.xml");
    when(conf.getTargetXMLReport()).thenReturn(reportFile);

    AnalysisResult analysisResult = new FindbugsExecutor(conf, fsEmpty, configEmpty).execute();

    assertThat(reportFile).exists();
    String report = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
    
    assertThat(report)
    .as("Report should contain bug instance").contains("<BugInstance")
    .as("Report should be generated with messages").contains("<Message>")
    .contains("priority=\"1\"")
    .doesNotContain("priority=\"3\"");

    checkAnalysisResult(analysisResult);
  }

  @Test
  void canGenerateXMLReportWithCustomConfidence() throws Exception {
    FindbugsConfiguration conf = mockConf();
    File reportFile = new File(temporaryFolder, "customized-findbugs-report.xml");
    when(conf.getTargetXMLReport()).thenReturn(reportFile);
    when(conf.getConfidenceLevel()).thenReturn("low");

    AnalysisResult analysisResult = new FindbugsExecutor(conf, fsEmpty, configEmpty).execute();

    assertThat(reportFile).exists();
    String report = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
    
    assertThat(report)
    .as("Report should contain bug instance").contains("<BugInstance")
    .as("Report should be generated with messages").contains("<Message>")
    .contains("priority=\"1\"")
    .contains("priority=\"3\"");
    
    checkAnalysisResult(analysisResult);
  }

  public void shouldTerminateAfterTimeout() throws Exception {
    FindbugsConfiguration conf = mockConf();
    when(conf.getTimeout()).thenReturn(1L);

    FindbugsExecutor executor = new FindbugsExecutor(conf, fsEmpty, configEmpty);
    assertThrows(IllegalStateException.class, () -> {
      executor.execute();
    });
  }

  public void shoulFailIfNoCompiledClasses() throws Exception {
    FileSystem fs = mock(FileSystem.class);
    when(fs.baseDir()).thenReturn(new File("."));
    SimpleConfiguration configuration = new SimpleConfiguration();
    //settings.setProperty(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY, Locale.getDefault().getDisplayName());
    FindbugsConfiguration conf = new FindbugsConfiguration(fs, configuration, null, null);
    
    FindbugsExecutor executor = new FindbugsExecutor(conf, fsEmpty, configEmpty);
    assertThrows(IllegalStateException.class, () -> {
      executor.execute();
    });
  }

  private FindbugsConfiguration mockConf() throws Exception {
    FindbugsConfiguration conf = mock(FindbugsConfiguration.class);
    doAnswer(invocation -> {
      Project project = invocation.getArgument(0);
      project.addFile(new File("test-resources/classes").getCanonicalPath());
      project.addSourceDirs(Collections.singletonList(new File("test-resources/src").getCanonicalPath()));
      project.setCurrentWorkingDirectory(new File("test-resources"));
      
      return null;
    }).when(conf).initializeFindbugsProject(any());
    when(conf.saveIncludeConfigXml()).thenReturn(new File("test-resources/findbugs-include.xml"));
    when(conf.getExcludesFilters()).thenReturn(Arrays.asList(new File("test-resources/findbugs-exclude.xml"), new File("test-resources/fake-file.xml")));
    when(conf.getEffort()).thenReturn("default");
    when(conf.getTimeout()).thenReturn(FindbugsConstants.TIMEOUT_DEFAULT_VALUE);
    return conf;
  }

  /**
   * Smoke test checking that no errors where reported.
   * This might happen when upgrading to a new version of SpotBugs or plugins and would most likely mean that there's a regression in the new version
   * 
   * @param analysisResult
   */
  public void checkAnalysisResult(AnalysisResult analysisResult) {
    assertThat(analysisResult.getAnalysisErrors()).isEmpty();
  }
}
