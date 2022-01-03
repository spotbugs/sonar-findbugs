package org.sonar.plugins.findbugs.it;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;

import java.io.File;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;

/**
 * @author gtoison
 *
 */
public class FindSecBugsIT {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:findbugs";
  public static Orchestrator orchestrator = FindbugsTestSuite.ORCHESTRATOR;
  
  @BeforeClass
  public static void startOrchestrator() {
    orchestrator.start();
  }
  
  @AfterClass
  public static void stopOrchestrator() {
    orchestrator.stop();
  }

  @Before
  public void setupProfile() {
    FindbugsTestSuite.setupProjectAndProfile(PROJECT_KEY, "Find Sec Bugs Integration Tests", "IT", "java");
  }
  
  @After
  public void deleteProject() {
    FindbugsTestSuite.deleteProject(PROJECT_KEY);
  }

  @Test
  public void noAnalysisWhenProfileDoesNotHaveSpotBugsRules() throws Exception {
    // Set an empty Java quality profile for the project
    AddProjectRequest addProjectRequest = new AddProjectRequest();
    addProjectRequest.setLanguage("java");
    addProjectRequest.setProject(PROJECT_KEY);
    addProjectRequest.setQualityProfile("empty");
    FindbugsTestSuite.qualityProfileClient().addProject(addProjectRequest);
    
    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("findbugs"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    File projectDir = FindbugsTestSuite.projectPom("findbugs").getParentFile();
    File resultFile = new File(projectDir, "target/sonar/findbugs-result.xml");
    
    String message = "The analysis must not run because the findbugs sample project does not contain JSP files and there are no spotbugs rules in the profile";
    assertFalse(message, resultFile.exists());
  }
}
