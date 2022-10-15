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
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.IssuesService;

import java.util.List;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;

@IntegrationTest
class FBContribIT {

  private static final String PROJECT_KEY = "org.sonar.tests:fb-contrib";
  public static Orchestrator orchestrator = FindbugsTestSuite.ORCHESTRATOR;
  
  @BeforeEach
  public void setupProfile() {
    FindbugsTestSuite.setupProjectAndProfile(PROJECT_KEY, "Findbugs Contrib Integration Tests", "IT", "java");
  }
  
  @AfterEach
  public void deleteProject() {
    FindbugsTestSuite.deleteProject(PROJECT_KEY);
  }

  @Test
  void test() throws Exception {
    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("simple"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().projects(PROJECT_KEY)).getIssuesList();
    assertThat(issues).hasSize(3);
    
    issues = issueClient.search(IssueQuery.create().rules("fb-contrib:IPU_IMPROPER_PROPERTIES_USE_SETPROPERTY")).getIssuesList();
    // SONARJAVA-216:
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getMessage()).isEqualTo("Method Simple.method() uses Properties.put instead of Properties.setProperty");

    issues = issueClient.search(IssueQuery.create().rules("findsecbugs:PREDICTABLE_RANDOM")).getIssuesList();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getMessage()).isEqualTo("This random generator (java.util.Random) is predictable");
  }
}
