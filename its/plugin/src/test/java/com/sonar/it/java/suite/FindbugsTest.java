/*
 * Copyright (C) 2014-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.java.suite;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsTest {

  @ClassRule
  public static Orchestrator orchestrator = FindbugsTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void analysis() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("findbugs"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.profile", "findbugs-it")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    // Check that issue with confidence level lower than high was reported:
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().components(FindbugsTestSuite.keyFor("com.sonarsource.it.samples:findbugs", "", "Findbugs1.java"))).list();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("findbugs:NP_DEREFERENCE_OF_READLINE_VALUE");
    assertThat(issues.get(0).line()).isEqualTo(13);

    issues = issueClient.find(IssueQuery.create().components(FindbugsTestSuite.keyFor("com.sonarsource.it.samples:findbugs", "", "Findbugs2.java"))).list();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("findbugs:ICAST_INT_2_LONG_AS_INSTANT");
    assertThat(issues.get(0).line()).isEqualTo(8);

    issues = issueClient.find(IssueQuery.create().components(FindbugsTestSuite.keyFor("com.sonarsource.it.samples:findbugs", "", "Findbugs4.java"))).list();
    if (FindbugsTestSuite.isJavaAtLeast_2_9()) {
      assertThat(issues).isEmpty();
    } else {
      assertThat(issues).hasSize(1);
      assertThat(issues.get(0).ruleKey()).isEqualTo("findbugs:ICAST_INT_2_LONG_AS_INSTANT");
      assertThat(issues.get(0).line()).isEqualTo(9);
    }
  }

  /**
   * SONAR-2325
   */
  @Test
  public void confidence_level() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("findbugs"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.profile", "findbugs-it")
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "high");
    orchestrator.executeBuild(build);

    // Check that issue with confidence level lower than high was NOT reported:
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().components(FindbugsTestSuite.keyFor("com.sonarsource.it.samples:findbugs", "", "Findbugs1.java"))).list();
    assertThat(issues).isEmpty();

    // Check that other files were analysed by Findbugs:
    issues = issueClient.find(IssueQuery.create().components(FindbugsTestSuite.keyFor("com.sonarsource.it.samples:findbugs", "", "Findbugs2.java"))).list();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("findbugs:ICAST_INT_2_LONG_AS_INSTANT");
    assertThat(issues.get(0).line()).isEqualTo(8);
  }

  /**
   * SONARJAVA-380
   */
  @Test
  public void should_always_use_english_locale() throws Exception {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("findbugs"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.profile", "findbugs-it")
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      .setProperty("sonar.violationLocale", "fr")
      .setEnvironmentVariable("MAVEN_OPTS", "-Duser.language=fr");
    orchestrator.executeBuild(build);
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().components(FindbugsTestSuite.keyFor("com.sonarsource.it.samples:findbugs", "", "Findbugs3.java"))).list();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).message()).isEqualTo("Private method Findbugs3.method() is never called");
  }

  /**
   * SONARJAVA-385
   */
  @Test
  public void inclusions_exclusions() throws Exception {
    File projectDir = TestUtils.projectPom("findbugs").getParentFile();
    // Compile
    MavenBuild build = MavenBuild.create(new File(projectDir, "pom.xml"))
      .setGoals("clean package");
    orchestrator.executeBuild(build);
    // Analyze
    SonarRunner sonarRunner = SonarRunner.create()
      .setProjectDir(projectDir)
      .setProperty("sonar.projectKey", "com.sonarsource.it.samples:findbugs")
      .setProperty("sonar.projectName", "findbugs")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.sources", ".")
      .setProperty("sonar.binaries", "target/classes")
      .setProperty("sonar.profile", "findbugs-it")
      .setProperty("sonar.exclusions", "src/main/java/Findbugs2.java");
    orchestrator.executeBuild(sonarRunner);

    // Check that class was really excluded from Findbugs analysis:
    String findbugsXml = Files.toString(new File(projectDir, ".sonar/findbugs-result.xml"), Charsets.UTF_8);
    assertThat(findbugsXml).doesNotContain("Findbugs2.class");

    // Check that other files were analysed by Findbugs:
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().components(FindbugsTestSuite.keyFor("com.sonarsource.it.samples:findbugs", "", "Findbugs1.java"))).list();
    assertThat(issues).hasSize(1);
  }

  @Test
  public void multiple_directories_with_classes() throws Exception {
    File projectDir = TestUtils.projectPom("multiple-directories-with-classes").getParentFile();
    // Compile
    MavenBuild build = MavenBuild.create(new File(projectDir, "pom.xml"))
      .setGoals("clean package");
    orchestrator.executeBuild(build);
    // Analyze
    SonarRunner sonarRunner = SonarRunner.create()
      .setProjectDir(projectDir)
      .setProperty("sonar.projectKey", "example")
      .setProperty("sonar.projectName", "example")
      .setProperty("sonar.projectVersion", "1")
      .setProperty("sonar.sources", ".")
      .setProperty("sonar.binaries", "dir1/target/classes,dir2/target/classes")
      .setProperty("sonar.profile", "findbugs-it");
    orchestrator.executeBuild(sonarRunner);

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().componentRoots("example")).list();
    assertThat(issues).hasSize(2);
  }

}
