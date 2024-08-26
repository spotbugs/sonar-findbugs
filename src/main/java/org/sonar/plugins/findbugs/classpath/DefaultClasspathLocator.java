/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
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
package org.sonar.plugins.findbugs.classpath;

import java.io.File;
import java.util.Collection;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;

/**
 * @author gtoison
 *
 */
@ScannerSide
public class DefaultClasspathLocator implements ClasspathLocator {
  private final ClasspathForMain classpathForMain;
  private final ClasspathForTest classpathForTest;

  public DefaultClasspathLocator(Configuration configuration, FileSystem fileSystem) {
    classpathForMain = new ClasspathForMain(configuration, fileSystem);
    classpathForTest = new ClasspathForTest(configuration, fileSystem);
  }

  @Override
  public Collection<File> binaryDirs() {
    return classpathForMain.getBinaryDirs();
  }

  @Override
  public Collection<File> classpath() {
    return classpathForMain.getElements();
  }

  @Override
  public Collection<File> testBinaryDirs() {
    return classpathForTest.getBinaryDirs();
  }

  @Override
  public Collection<File> testClasspath() {
    return classpathForTest.getElements();
  }
}
