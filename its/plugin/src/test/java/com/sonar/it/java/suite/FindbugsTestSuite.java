/*
 * Copyright (C) 2014-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.java.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  FindbugsTest.class,
  FBContribTest.class
})
public class FindbugsTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .addPlugin("findbugs")
      .addPlugin("java")
      .setMainPluginKey("findbugs")
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/java/FindbugsTest/findbugs-backup.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/java/FindbugsTest/fbcontrib-backup.xml"));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  public static String keyFor(String projectKey, String pkgDir, String cls) {
    return projectKey + ":src/main/java/" + pkgDir + cls;
  }

  public static boolean isJavaAtLeast_2_9() {
    return ORCHESTRATOR.getConfiguration().getPluginVersion("java").isGreaterThanOrEquals("2.9");
  }

}
