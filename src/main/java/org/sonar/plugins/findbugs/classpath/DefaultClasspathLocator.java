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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger LOG = LoggerFactory.getLogger(DefaultClasspathLocator.class);

  private ClasspathForMain classpathForMain;
  private ClasspathForTest classpathForTest;

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

  @Override
  public Collection<File> classFilesToAnalyze() {
    ClassFileVisitor visitor = new ClassFileVisitor();
    for (File binaryDir : binaryDirs()) {
      try {
        Files.walkFileTree(binaryDir.toPath(), visitor);
      } catch (IOException e) {
        LOG.error("Error listing class files to analyze", e);
      }
    }

    return visitor.matchedFiles;
  }

  private static class ClassFileVisitor implements FileVisitor<Path> {
    private PathMatcher classFileMatcher = p -> p.toString().endsWith(".class");
    private Collection<File> matchedFiles = new ArrayList<>();
    
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (classFileMatcher.matches(file)) {
        matchedFiles.add(file.toFile().getCanonicalFile());
      }
      
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      return FileVisitResult.CONTINUE;
    }
  }
}
