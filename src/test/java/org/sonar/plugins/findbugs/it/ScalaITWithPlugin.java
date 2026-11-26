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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.List;

import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.api.utils.Version;
import org.sonar.plugins.findbugs.profiles.FindbugsSecurityScalaProfile;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.issues.IssuesService;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.ProjectsService;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofiles.QualityprofilesService;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.locator.FileLocation;

@IntegrationTest
class ScalaITWithPlugin {

  private static final String PROJECT_KEY = "org.sonar.tests:scala";
  private static OrchestratorExtension orchestrator;

  @BeforeAll
  static void startServer() throws Exception {
    Version sonarVersion = Version.parse(System.getProperty("sonar.server.version", "8.9"));
    assumeTrue(sonarVersion.isGreaterThanOrEqual(Version.create(26, 11)),
      "The Scala plugin can only be installed on SonarQube 26.11 or later");

    orchestrator = OrchestratorExtension.builderEnv()
      .keepBundledPlugins()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion("LATEST_RELEASE[" + sonarVersion + "]")
      .setOrchestratorProperty("orchestrator.artifactory.url", "https://repo1.maven.org/maven2")
      .addPlugin(FileLocation.of(new File("target", "sonar-findbugs-plugin.jar")))
      .addPlugin(MavenLocation.of("org.sonarsource.slang", "sonar-scala-plugin", "1.25.0.2697"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/it/profiles/empty-backup.xml"))
      .build();
    orchestrator.start();
  }

  @AfterAll
  static void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  void analysis() {
    HttpConnector connector = HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .credentials("admin", "admin")
      .build();
    CreateRequest createRequest = new CreateRequest();
    createRequest.setName("Scala Plugin Integration Tests");
    createRequest.setProject(PROJECT_KEY);
    new ProjectsService(connector).create(createRequest);

    QualityprofilesService profiles = new QualityprofilesService(connector);
    AddProjectRequest javaProfile = new AddProjectRequest();
    javaProfile.setProject(PROJECT_KEY);
    javaProfile.setLanguage("java");
    javaProfile.setQualityProfile("IT");
    profiles.addProject(javaProfile);

    AddProjectRequest scalaProfile = new AddProjectRequest();
    scalaProfile.setProject(PROJECT_KEY);
    scalaProfile.setLanguage("scala");
    scalaProfile.setQualityProfile(FindbugsSecurityScalaProfile.FINDBUGS_SECURITY_SCALA_PROFILE_NAME);
    profiles.addProject(scalaProfile);

    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("scala"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      .setGoals("clean package org.sonarsource.scanner.maven:sonar-maven-plugin:sonar");
    orchestrator.executeBuild(build);

    List<Issue> issues = new IssuesService(connector).search(IssueQuery.create().projects(PROJECT_KEY)).getIssuesList();
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getMessage()).isEqualTo("Hello$.main(String[]) invokes toString() method on a String");
    assertThat(issues.get(1).getMessage()).isEqualTo("This Scala random generator (scala.util.Random.nextInt()) is predictable");
  }
}
