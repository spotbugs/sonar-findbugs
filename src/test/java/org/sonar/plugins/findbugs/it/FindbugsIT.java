/*
 * Findbugs :: IT :: Plugin
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
package org.sonar.plugins.findbugs.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.findbugs.FindbugsConstants;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.IssuesService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.io.Files;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.junit5.OrchestratorExtension;

@IntegrationTest
class FindbugsIT {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:findbugs";
  private static OrchestratorExtension orchestrator = FindbugsTestSuite.ORCHESTRATOR;
  
  @BeforeEach
  public void setupProfile() {
    FindbugsTestSuite.setupProjectAndProfile(PROJECT_KEY, "Findbugs Integration Tests", "findbugs-it", "java");
  }
  
  @AfterEach
  public void deleteProject() {
    FindbugsTestSuite.deleteProject(PROJECT_KEY);
  }

  @Test
  void analysis() {
    MavenBuild build = MavenBuild.create(FindbugsTestSuite.projectPom("findbugs"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    // Check that issue with confidence level lower than high was reported:
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    
    List<Issue> issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs1.java"))).getIssuesList();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getRule()).isEqualTo("findbugs:NP_DEREFERENCE_OF_READLINE_VALUE");
    assertThat(issues.get(0).getLine()).isEqualTo(13);

    issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs2.java"))).getIssuesList();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getRule()).isEqualTo("findbugs:ICAST_INT_2_LONG_AS_INSTANT");
    assertThat(issues.get(0).getLine()).isEqualTo(8);

    issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs4.java"))).getIssuesList();
    assertThat(issues).isEmpty();
  }

  /**
   * SONAR-2325
   */
  @Test
  void confidence_level() {
    MavenBuild build = MavenBuild.create(FindbugsTestSuite.projectPom("findbugs"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "high");
    orchestrator.executeBuild(build);

    // Check that issue with confidence level lower than high was NOT reported:
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs1.java"))).getIssuesList();
    assertThat(issues).isEmpty();

    // Check that other files were analysed by Findbugs:
    issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs2.java"))).getIssuesList();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getRule()).isEqualTo("findbugs:ICAST_INT_2_LONG_AS_INSTANT");
    assertThat(issues.get(0).getLine()).isEqualTo(8);
  }

  /**
   * SONARJAVA-380
   */
  @Test
  void should_always_use_english_locale() throws Exception {
    MavenBuild build = MavenBuild.create(FindbugsTestSuite.projectPom("findbugs"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      .setProperty("sonar.violationLocale", "fr")
      .setEnvironmentVariable("MAVEN_OPTS", "-Duser.language=fr");
    orchestrator.executeBuild(build);
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs3.java"))).getIssuesList();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getMessage()).isEqualTo("Private method Findbugs3.method() is never called");
  }

  /**
   * SONARJAVA-385
   */
  @Test
  void inclusions_exclusions() throws Exception {
    File projectDir = FindbugsTestSuite.projectPom("findbugs").getParentFile();
    // Compile
    MavenBuild build = MavenBuild.create(new File(projectDir, "pom.xml"))
        .setGoals("clean package");
    orchestrator.executeBuild(build);
    // Analyze
    SonarScanner sonarScanner = SonarScanner.create()
      .setProjectDir(projectDir)
      .setProperty("sonar.projectKey", PROJECT_KEY)
      .setProperty("sonar.projectName", "findbugs")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.sources", ".")
      .setProperty("sonar.java.binaries", "target/classes")
      .setProperty("sonar.exclusions", "src/main/java/Findbugs2.java");
    orchestrator.executeBuild(sonarScanner);

    // Check that class was really excluded from Findbugs analysis:
    String findbugsXml = Files.toString(new File(projectDir, ".scannerwork/findbugs-result.xml"), StandardCharsets.UTF_8);
    
    // FIXME Even though a source file is excluded, the corresponding .class file is currently analyzed by the plugin
    // assertThat(findbugsXml).doesNotContain("Findbugs2.class");

    // Check that other files were analysed by Findbugs:
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs1.java"))).getIssuesList();
    assertThat(issues).hasSize(1);
  }

  @Test
  void multiple_directories_with_classes() throws Exception {
    File projectDir = FindbugsTestSuite.projectPom("multiple-directories-with-classes").getParentFile();
    // Compile
    MavenBuild build = MavenBuild.create(new File(projectDir, "pom.xml"))
        .setGoals("clean package");
    orchestrator.executeBuild(build);
    // Analyze
    SonarScanner sonarScanner = SonarScanner.create()
      .setProjectDir(projectDir)
      .setProperty("sonar.projectKey", PROJECT_KEY)
      .setProperty("sonar.projectName", "example")
      .setProperty("sonar.projectVersion", "1")
      .setProperty("sonar.sources", ".")
      .setProperty("sonar.java.binaries", "dir1/target/classes,dir2/target/classes");
    orchestrator.executeBuild(sonarScanner);

    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().projects(PROJECT_KEY)).getIssuesList();
    assertThat(issues).hasSize(2);
  }

  @Test
  void only_analyze() throws Exception {
    File projectDir = FindbugsTestSuite.projectPom("findbugs").getParentFile();
    
    MavenBuild build = MavenBuild.create(new File(projectDir, "pom.xml"))
        .setProperty(FindbugsConstants.ONLY_ANALYZE_PROPERTY, "Findbugs1")
        .setGoals("clean package sonar:sonar");
    orchestrator.executeBuild(build);

    // Check that class was really excluded from Findbugs analysis:
    // Not sure why but depending on the build the output is either is scannerwork or in target/sonar
    // For this build the output seems to be in target/sonar
    String findbugsXml = Files.toString(new File(projectDir, "target/sonar/findbugs-result.xml"), StandardCharsets.UTF_8);
    
    assertThat(findbugsXml).doesNotContain("Findbugs2.class");

    // Check that other files were analysed by Findbugs:
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().components(FindbugsTestSuite.keyFor(PROJECT_KEY, "", "Findbugs1.java"))).getIssuesList();
    assertThat(issues).hasSize(1);
  }
}
