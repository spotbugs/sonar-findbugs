/*
 * SonarSource :: Findbugs :: ITs :: Ruling
 * Copyright (C) 2014 SonarSource
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
package org.sonar.java.it;

import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsRulingTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin("java")
    .addPlugin(FileLocation.of("../../target/sonar-findbugs-plugin.jar"))
    .setOrchestratorProperty("litsVersion", "0.5")
    .addPlugin("lits")
    .restoreProfileAtStartup(FileLocation.of("src/test/resources/profile-findbugs.xml"))
    .build();

  @Test
  public void test() throws Exception {
    File litsDifferencesFile = FileLocation.of("target/differences").getFile();
    MavenBuild build = MavenBuild.create(FileLocation.of("../sources/struts-1.3.9/core/pom.xml").getFile())
      .setProfile("rules_findbugs")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.analysis.mode", "preview")
      .setProperty("sonar.issuesReport.html.enable", "true")
      .setProperty("dump.old", FileLocation.of("src/test/resources/expected-findbugs").getFile().getAbsolutePath())
      .setProperty("dump.new", FileLocation.of("target/actual-findbugs").getFile().getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesFile.getAbsolutePath())
      .setCleanPackageSonarGoals()
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1000m");
    orchestrator.executeBuild(build);

    assertThat(Files.toString(litsDifferencesFile, StandardCharsets.UTF_8)).isEmpty();
  }
}
