package org.sonar.plugins.findbugs.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.IssuesService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit5.OrchestratorExtension;

/**
 * @author gtoison
 *
 */
@IntegrationTest
class FindSecBugsJspIT {

  private static final String SLING_PROJECT_KEY = "org.sonar.tests:jspc-sling";
  private static final String JETTY_PROJECT_KEY = "org.sonar.tests:jspc-jetty";
  
  public static OrchestratorExtension orchestrator = FindbugsTestSuite.ORCHESTRATOR;

  @Test
  void jspSlingAnalysis() throws Exception {
    FindbugsTestSuite.setupProjectAndProfile(SLING_PROJECT_KEY, "Find Sec Bugs JSP Sling Integration Tests", "IT", "java");
    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("jspc-sling"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      .setProperty("sonar.plugins.downloadOnlyRequired", "false")
      .setGoals("clean package sonar:sonar");
    orchestrator.executeBuild(build);

    File projectDir = FindbugsTestSuite.projectPom("jspc-sling").getParentFile();
    File resultFile = new File(projectDir, "target/sonar/findbugs-result.xml");
    
    assertThat(resultFile).withFailMessage("JSP Sling project must be analyzed").exists();
    
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    
    String indexKey = FindbugsTestSuite.keyForFile(SLING_PROJECT_KEY, "src/main/scripts/index.jsp");
    
    List<Issue> issues = issueClient.search(IssueQuery.create().components(indexKey)).getIssuesList();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getRule()).isEqualTo("findsecbugs-jsp:XSS_REQUEST_PARAMETER_TO_JSP_WRITER");
    assertThat(issues.get(0).getLine()).isEqualTo(18);
    
    FindbugsTestSuite.deleteProject(SLING_PROJECT_KEY);
  }

  @Test
  void jspJettyAnalysis() throws Exception {
    FindbugsTestSuite.setupProjectAndProfile(JETTY_PROJECT_KEY, "Find Sec Bugs JSP Jetty Integration Tests", "IT", "java");
    
    MavenBuild build = MavenBuild.create()
      .setPom(FindbugsTestSuite.projectPom("jspc-jetty"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.findbugs.confidenceLevel", "low")
      .setProperty("sonar.plugins.downloadOnlyRequired", "false")
      .setGoals("clean package sonar:sonar");
    orchestrator.executeBuild(build);

    File projectDir = FindbugsTestSuite.projectPom("jspc-jetty").getParentFile();
    File resultFile = new File(projectDir, "target/sonar/findbugs-result.xml");
    
    assertThat(resultFile).withFailMessage("JSP Jetty project must be analyzed").exists();
    
    IssuesService issueClient = FindbugsTestSuite.issueClient();
    
    String indexKey = FindbugsTestSuite.keyForFile(JETTY_PROJECT_KEY, "src/main/webapp/WEB-INF/jsp/pages/page1/page1.jsp");
    
    // Check that there's an issue on page1.jsp
    List<Issue> issues = new ArrayList<>(issueClient.search(IssueQuery.create().components(indexKey)).getIssuesList());
    Collections.sort(issues, Comparator.comparing(Issue::getRule));
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getRule()).isEqualTo("findsecbugs-jsp:XSS_JSP_PRINT");
    assertThat(issues.get(0).getLine()).isEqualTo(11);
    assertThat(issues.get(1).getRule()).isEqualTo("findsecbugs-jsp:XSS_REQUEST_PARAMETER_TO_JSP_WRITER");
    assertThat(issues.get(1).getLine()).isEqualTo(11);
    

    // Check that there's an issue on page2.jsp
    indexKey = FindbugsTestSuite.keyForFile(JETTY_PROJECT_KEY, "src/main/webapp/WEB-INF/jsp/pages/page2/page2.jsp");
    
    issues = new ArrayList<>(issueClient.search(IssueQuery.create().components(indexKey)).getIssuesList());
    Collections.sort(issues, Comparator.comparing(Issue::getRule));
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getRule()).isEqualTo("findsecbugs-jsp:XSS_JSP_PRINT");
    assertThat(issues.get(0).getLine()).isEqualTo(11);
    assertThat(issues.get(1).getRule()).isEqualTo("findsecbugs-jsp:XSS_REQUEST_PARAMETER_TO_JSP_WRITER");
    assertThat(issues.get(1).getLine()).isEqualTo(11);
    
    FindbugsTestSuite.deleteProject(JETTY_PROJECT_KEY);
  }
}
