/*
 * Copyright (C) 2014-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.java.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsRulingTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin("java")
    .addPlugin("findbugs")
    .setMainPluginKey("findbugs")
    .addPlugin(MavenLocation.create("com.sonarsource.lits", "sonar-lits-plugin", "0.4"))
    .restoreProfileAtStartup(FileLocation.of("src/test/resources/profile-findbugs.xml"))
    .build();

  @Test
  public void test() throws Exception {
    MavenBuild build = MavenBuild.create(FileLocation.ofShared("it-sonar-performancing/struts-1.3.9/core/pom.xml").getFile())
      .setProfile("rules_findbugs")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("dump.old", FileLocation.of("src/test/resources/expected-findbugs").getFile().getAbsolutePath())
      .setProperty("dump.new", FileLocation.of("target/actual-findbugs").getFile().getAbsolutePath())
      .setCleanPackageSonarGoals()
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1000m");
    orchestrator.executeBuild(build);

    assertThatNoDifferences();
  }

  private void assertThatNoDifferences() {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().componentRoots("org.apache.struts:struts-core").severities("BLOCKER", "INFO")).list();
    assertThat(issues.size()).as("differences").isEqualTo(0);
  }

}
