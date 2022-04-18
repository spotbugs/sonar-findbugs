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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;

import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.issues.IssuesService;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.DeleteRequest;
import org.sonarqube.ws.client.projects.ProjectsService;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofiles.QualityprofilesService;

import java.io.File;

public class FindbugsTestSuite {

  public static final Orchestrator ORCHESTRATOR;

  static {
    // build, start and stop the orchestrator here, making sure that it happens exactly once whether we run one or multiple tests
    String sonarVersion = System.getProperty("sonar.version", "8.9");
    
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .addPlugin(FileLocation.of("./target/sonar-findbugs-plugin.jar"))
      .keepBundledPlugins()
      .setSonarVersion("LATEST_RELEASE[" + sonarVersion + "]")
      .restoreProfileAtStartup(FileLocation.ofClasspath("/it/profiles/empty-backup.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/it/profiles/findbugs-backup.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/it/profiles/fbcontrib-backup.xml"));
    ORCHESTRATOR = orchestratorBuilder.build();
    ORCHESTRATOR.start();

    Thread stopOrchestratorThread = new Thread(() -> ORCHESTRATOR.stop(), sonarVersion);
    Runtime.getRuntime().addShutdownHook(stopOrchestratorThread);
  }
  
  public static void setupProjectAndProfile(String projectKey, String projectName, String qualityProfile, String language) {
    CreateRequest createRequest = new CreateRequest();
    createRequest.setName(projectName);
    createRequest.setProject(projectKey);
    
    projectClient().create(createRequest);
    
    setupProfile(projectKey, qualityProfile, language);
  }

  public static void setupProfile(String projectKey, String qualityProfile, String language) {
    AddProjectRequest addProjectRequest = new AddProjectRequest();
    addProjectRequest.setLanguage(language);
    addProjectRequest.setProject(projectKey);
    addProjectRequest.setQualityProfile(qualityProfile);
    qualityProfileClient().addProject(addProjectRequest);
  }
  
  /**
   * Tests are checking the number of issues so we need to delete previous issues after running a another test
   */
  public static void deleteProject(String projectKey) {
    DeleteRequest deleteRequest = new DeleteRequest();
    deleteRequest.setProject(projectKey);
    
    projectClient().delete(deleteRequest);
  }

  public static String keyFor(String projectKey, String pkgDir, String cls) {
    return projectKey + ":src/main/java/" + pkgDir + cls;
  }

  public static String keyForFile(String projectKey, String fileName) {
    return projectKey + ":" + fileName;
  }

  public static File projectPom(String projectName) {
    return new File("src/test/resources/projects/" + projectName + "/pom.xml").getAbsoluteFile();
  }

  public static Location projectDirectoryLocation(String projectName) {
    return FileLocation.of(new File("src/test/resources/projects/" + projectName).getAbsoluteFile());
  }

  public static HttpConnector connector() {
    String baseUrl = ORCHESTRATOR.getServer().getUrl();
    
    return HttpConnector.newBuilder().url(baseUrl).credentials("admin", "admin").build();
  }

  public static IssuesService issueClient() {
    return new IssuesService(connector());
  }

  public static ProjectsService projectClient() {
    return new ProjectsService(connector());
  }

  public static QualityprofilesService qualityProfileClient() {
    return new QualityprofilesService(connector());
  }
}
