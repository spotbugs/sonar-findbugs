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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsConfigurationTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultFileSystem fs;
  private Settings settings;
  private File baseDir;
  private FindbugsConfiguration conf;
  private JavaResourceLocator javaResourceLocator;

  @Before
  public void setUp() throws Exception {
    baseDir = temp.newFolder("findbugs");

    fs = new DefaultFileSystem(baseDir);
    fs.setWorkDir(temp.newFolder());

    settings = new Settings(new PropertyDefinitions().addComponents(FindbugsConfiguration.getPropertyDefinitions()));
    javaResourceLocator = mock(JavaResourceLocator.class);
    conf = new FindbugsConfiguration(fs, settings, RulesProfile.create(), new FindbugsProfileExporter(), javaResourceLocator);
  }

  @Test
  public void should_return_report_file() throws Exception {
    assertThat(conf.getTargetXMLReport().getCanonicalPath()).isEqualTo(new File(fs.workDir(), "findbugs-result.xml").getCanonicalPath());
  }

  @Test
  public void should_save_include_config() throws Exception {
    conf.saveIncludeConfigXml();
    File findbugsIncludeFile = new File(fs.workDir(), "findbugs-include.xml");
    assertThat(findbugsIncludeFile.exists()).isTrue();
  }

  @Test
  public void should_return_effort() {
    assertThat(conf.getEffort()).as("default effort").isEqualTo("default");
    settings.setProperty(FindbugsConstants.EFFORT_PROPERTY, "Max");
    assertThat(conf.getEffort()).isEqualTo("max");
  }

  @Test
  public void should_return_timeout() {
    assertThat(conf.getTimeout()).as("default timeout").isEqualTo(600000);
    settings.setProperty(FindbugsConstants.TIMEOUT_PROPERTY, 1);
    assertThat(conf.getTimeout()).isEqualTo(1);
  }

  @Test
  public void should_return_excludes_filters() {
    assertThat(conf.getExcludesFilters()).isEmpty();
    settings.setProperty(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY, " foo.xml , bar.xml,");
    assertThat(conf.getExcludesFilters()).hasSize(2);
  }

  @Test
  public void should_return_confidence_level() {
    assertThat(conf.getConfidenceLevel()).as("default confidence level").isEqualTo("medium");
    settings.setProperty(FindbugsConstants.EFFORT_PROPERTY, "HIGH");
    assertThat(conf.getEffort()).isEqualTo("high");
  }

  @Test
  public void should_set_class_files() throws IOException {
    File file = temp.newFile("MyClass.class");
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(ImmutableList.of(file));
    Project findbugsProject = conf.getFindbugsProject();

    assertThat(findbugsProject.getFileList()).containsOnly(file.getCanonicalPath());
    conf.stop();
  }

  @Test
  public void should_set_class_path() throws IOException {
    File classpath = temp.newFolder();
    when(javaResourceLocator.classpath()).thenReturn(ImmutableList.of(classpath));
    Project findbugsProject = conf.getFindbugsProject();

    assertThat(findbugsProject.getAuxClasspathEntryList()).contains(classpath.getCanonicalPath());
    conf.stop();
  }

  @Test
  public void should_copy_lib_in_working_dir() throws IOException {
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
  public void should_get_fbcontrib() throws IOException {
    conf.copyLibs();
    assertThat(conf.getFbContribJar()).isFile();
  }

  @Test
  public void should_get_findSecBugs() throws IOException {
    conf.copyLibs();
    assertThat(conf.getFindSecBugsJar()).isFile();
  }

}
