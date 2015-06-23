/*
 * Copyright (C) 2014-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.java.suite;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class TestUtils {
  private static final File home;

  static {
    File testResources = FileUtils.toFile(FindbugsTest.class.getResource("/TestUtils.txt"));
    home = testResources // home/src/tests/resources
        .getParentFile() // home/src/tests
        .getParentFile() // home/src
        .getParentFile(); // home
  }

  public static File projectPom(String projectName) {
    return new File(home, "projects/" + projectName + "/pom.xml");
  }
}
