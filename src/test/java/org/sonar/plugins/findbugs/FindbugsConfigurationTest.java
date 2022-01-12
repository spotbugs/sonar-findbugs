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

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.Project;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.plugins.findbugs.configuration.SimpleConfiguration;
import org.sonar.plugins.findbugs.rule.FakeActiveRules;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindbugsConfigurationTest {

  @TempDir
  public File temp;

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
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(ImmutableList.of(file));
    try (Project findbugsProject = new Project()) {
      conf.initializeFindbugsProject(findbugsProject);
      
      assertThat(findbugsProject.getFileList()).containsOnly(file.getCanonicalPath());
      conf.stop();
    }
  }

  @Test
  void should_set_class_path() throws IOException {
    File classpath = new File(temp, "classpath");
    when(javaResourceLocator.classpath()).thenReturn(ImmutableList.of(classpath));
    try (Project findbugsProject = new Project()) {
      conf.initializeFindbugsProject(findbugsProject);

      assertThat(findbugsProject.getAuxClasspathEntryList()).contains(classpath.getCanonicalPath());
      conf.stop();
    }
  }

  @Test
  void should_copy_lib_in_working_dir() throws IOException {
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
  void should_get_fbcontrib() throws IOException {
    conf.copyLibs();
    assertThat(conf.getFbContribJar()).isFile();
  }

  @Test
  void should_get_findSecBugs() throws IOException {
    conf.copyLibs();
    assertThat(conf.getFindSecBugsJar()).isFile();
  }

  @Test
  void scanEmptyFolderForAdditionalClasses() {
    List<File> classes = FindbugsConfiguration.scanForAdditionalClasses(temp);
    
    assertThat(classes).isEmpty();
  }
}
