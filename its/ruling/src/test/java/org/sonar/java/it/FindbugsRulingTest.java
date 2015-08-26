/*
 * Copyright (C) 2014-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.java.it;

import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsRulingTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin("java")
    .addPlugin("findbugs")
    .setMainPluginKey("findbugs")
    .addPlugin(MavenLocation.create("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.5-SNAPSHOT"))
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
