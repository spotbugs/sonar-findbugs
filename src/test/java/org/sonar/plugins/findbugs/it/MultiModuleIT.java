package org.sonar.plugins.findbugs.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.IssuesService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.GradleBuild;
import com.sonar.orchestrator.build.MavenBuild;

/**
 * @author gtoison
 *
 */
class MultiModuleIT {

  private static final String MAVEN_PROJECT_KEY = "spotbugs:multi-module";
  private static final String GRADLE_PROJECT_KEY = "org.sonarqube:gradle-multimodule";
  
  public static Orchestrator orchestrator = FindbugsTestSuite.ORCHESTRATOR;

  @Test
  void multiModuleMavenAnalysis() throws Exception {
    FindbugsTestSuite.setupProjectAndProfile("spotbugs:multi-module", "Multi Module Maven Project", "FindBugs + FB-Contrib", "java");

    File projectDir = FindbugsTestSuite.projectPom("multi-module").getParentFile();
    
    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("multi-module"))
      .setGoals("clean package sonar:sonar");
    orchestrator.executeBuild(build);
    
    Path appModuleReportPath = projectDir.toPath().resolve("multi-module-app/target/sonar/findbugs-result.xml");
    
    checkMultiModuleAnalysis(appModuleReportPath);
    checkIssues(MAVEN_PROJECT_KEY);
       
    FindbugsTestSuite.deleteProject(MAVEN_PROJECT_KEY);
  }

  @Test
  void multiModuleGradleAnalysis() throws IOException {
    FindbugsTestSuite.setupProjectAndProfile(GRADLE_PROJECT_KEY, "Multimodule Gradle Project", "FindBugs + FB-Contrib", "java");

    File projectDir = FindbugsTestSuite.projectPom("multi-module").getParentFile();
    
    GradleBuild build = GradleBuild.create()
      .setProjectDirectory(FindbugsTestSuite.projectDirectoryLocation("multi-module"))
      .setTasks("clean", "build")
      .addArgument("--stacktrace")
      .addSonarTask();
    orchestrator.executeBuild(build);
    
    Path appModuleReportPath = projectDir.toPath().resolve("build/sonar/org.sonarqube_gradle-multimodule_multi-module-app/findbugs-result.xml");

    checkMultiModuleAnalysis(appModuleReportPath);
    checkIssues(GRADLE_PROJECT_KEY);
    
    FindbugsTestSuite.deleteProject(GRADLE_PROJECT_KEY);
  }

  public void checkMultiModuleAnalysis(Path appModuleReportPath) throws IOException {
    List<String> appModuleFindbugsXml = Files.readAllLines(appModuleReportPath, StandardCharsets.UTF_8);
    
    // SampleApp should be analyzed
    assertThat(appModuleFindbugsXml).filteredOn(line -> line.contains("SampleApp.class")).singleElement();
    
    // But not SampleCore or SampleFx
    assertThat(appModuleFindbugsXml)
    .noneMatch(line -> line.contains("SampleCore.class"))
    .noneMatch(line -> line.contains("SampleFx.class"));
  }

  private void checkIssues(String projectKey) {
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    List<Issue> issues = issueClient.search(IssueQuery.create().projects(projectKey)).getIssuesList();
    
    assertThat(issues.stream().filter(component(projectKey, "multi-module-app/src/main/java/multimodule/app/SampleApp.java"))).hasSize(5);
    assertThat(issues.stream().filter(component(projectKey, "multi-module-core/src/main/java/multimodule/core/SampleCore.java"))).hasSize(4);
    assertThat(issues.stream().filter(component(projectKey, "multi-module-fx/src/main/java/multimodule/fx/SampleFx.java"))).hasSize(3);
    // There's one native SQ issue for the Hello.scala sample
    assertThat(issues.stream().filter(component(projectKey, "multi-module-scala/src/main/scala/Hello.scala"))).hasSize(2);
    assertThat(issues.stream().filter(component(projectKey, "multi-module-kotlin/src/main/kotlin/com/bugs/KotlinSample.kt"))).hasSize(2);
  }
  
  private Predicate<Issue> component(String projectKey, String fileName) {
    return i -> i.getComponent().equals(projectKey + ":" + fileName);
  }
}
