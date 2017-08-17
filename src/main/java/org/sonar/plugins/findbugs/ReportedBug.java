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
package org.sonar.plugins.findbugs;

import edu.umd.cs.findbugs.BugInstance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportedBug {

  private final String type;
  private final String message;
  private final String className;
  private final int startLine;
  private final String sourceFile;
  private final String classFile;

  private static final Pattern SOURCE_FILE_PATTERN = createSourceFilePattern();

  public ReportedBug(BugInstance bugInstance) {
    this.type = bugInstance.getType();
    this.message = bugInstance.getMessageWithoutPrefix();
    this.className = bugInstance.getPrimarySourceLineAnnotation().getClassName();
    this.startLine = bugInstance.getPrimarySourceLineAnnotation().getStartLine();
    this.sourceFile = bugInstance.getPrimarySourceLineAnnotation().getSourcePath();
    Matcher m = SOURCE_FILE_PATTERN.matcher(sourceFile);
    if (m.find()) {
      this.classFile = m.group(1).replaceAll("/",".");
    }
    else {
      this.classFile = className;
    }
  }

  public String getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public String getClassName() {
    return className;
  }

  public int getStartLine() {
    return startLine;
  }

  public String getSourceFile() { return sourceFile; }

  public String getClassFile() { return classFile; }

  private static Pattern createSourceFilePattern() {
    StringBuffer extensions = new StringBuffer();

    for (int i = 0; i < FindbugsPlugin.SUPPORTED_JVM_LANGUAGES_EXTENSIONS.length; i++) {
      String extension = FindbugsPlugin.SUPPORTED_JVM_LANGUAGES_EXTENSIONS[i];
      extensions.append(extension);
      if(i< FindbugsPlugin.SUPPORTED_JVM_LANGUAGES_EXTENSIONS.length - 1 ) {
        extensions.append("|");
      }
    }

    String pattern = "^(.*)\\.(" + extensions + ")$";
    return Pattern.compile(pattern);
  }
}
