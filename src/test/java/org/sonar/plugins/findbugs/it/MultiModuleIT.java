package org.sonar.plugins.findbugs.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    FindbugsTestSuite.setupProjectAndProfile("spotbugs:multi-module", "Multi Module Maven Project", "IT", "java");

    File projectDir = FindbugsTestSuite.projectPom("multi-module").getParentFile();
    
    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("multi-module"))
      .setGoals("clean package sonar:sonar");
    orchestrator.executeBuild(build);
    
    Path appModuleReportPath = projectDir.toPath().resolve("multi-module-app/target/sonar/findbugs-result.xml");
    
    checkMultiModuleAnalysis(appModuleReportPath);
       
    FindbugsTestSuite.deleteProject(MAVEN_PROJECT_KEY);
  }

  @Test
  void multiModuleGradleAnalysis() throws IOException {
    FindbugsTestSuite.setupProjectAndProfile(GRADLE_PROJECT_KEY, "Multimodule Gradle Project", "IT", "java");

    File projectDir = FindbugsTestSuite.projectPom("multi-module").getParentFile();
    
    GradleBuild build = GradleBuild.create()
      .setProjectDirectory(FindbugsTestSuite.projectDirectoryLocation("multi-module"))
      .setTasks("clean", "build")
      .addArgument("--stacktrace")
      .addSonarTask();
    orchestrator.executeBuild(build);
    
    Path appModuleReportPath = projectDir.toPath().resolve("build/sonar/org.sonarqube_gradle-multimodule_multi-module-app/findbugs-result.xml");

    checkMultiModuleAnalysis(appModuleReportPath);
    
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
}
