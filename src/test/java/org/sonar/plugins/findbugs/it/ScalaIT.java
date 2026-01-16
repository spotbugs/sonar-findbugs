/*
 * SonarQube SpotBugs Plugin
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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.findbugs.profiles.FindbugsSecurityScalaProfile;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.IssuesService;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit5.OrchestratorExtension;

@IntegrationTest
class ScalaIT {

  private static final String PROJECT_KEY = "org.sonar.tests:scala";
  public static OrchestratorExtension orchestrator = FindbugsTestSuite.ORCHESTRATOR;
  
  @BeforeEach
  public void setupProfile() {
    FindbugsTestSuite.setupProjectAndProfile(PROJECT_KEY, "Scala Integration Tests", "IT", "java");
    FindbugsTestSuite.setupProfile(PROJECT_KEY, FindbugsSecurityScalaProfile.FINDBUGS_SECURITY_SCALA_PROFILE_NAME, "scala");
  }
  
  @AfterEach
  public void deleteProject() {
    FindbugsTestSuite.deleteProject(PROJECT_KEY);
  }

  @Test
  void test() throws Exception {
    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("scala"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      //.setProperty("sonar.sources", "src/main/scala")
      //.setProperty("sonar.java.binaries", "target/classes")
      .setGoals("clean package org.sonarsource.scanner.maven:sonar-maven-plugin:sonar");
    orchestrator.executeBuild(build);

    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().projects(PROJECT_KEY)).getIssuesList();
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getMessage()).isEqualTo("Hello$.main(String[]) invokes toString() method on a String");
    assertThat(issues.get(1).getMessage()).isEqualTo("This Scala random generator (scala.util.Random.nextInt()) is predictable");
  }
}
