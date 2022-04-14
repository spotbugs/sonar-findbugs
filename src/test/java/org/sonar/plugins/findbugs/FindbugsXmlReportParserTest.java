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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindbugsXmlReportParserTest {

  private List<FindbugsXmlReportParser.XmlBugInstance> violations;

  @BeforeEach
  public void init() {
    File findbugsXmlReport = getFile("/org/sonar/plugins/findbugs/findbugsReport.xml");
    FindbugsXmlReportParser xmlParser = new FindbugsXmlReportParser(findbugsXmlReport);
    violations = xmlParser.getBugInstances();
  }

  @Test
  void createFindbugsXmlReportParserWithUnexistedReportFile() {
    File xmlReport = new File("doesntExist.xml");
    
    Throwable thrown = assertThrows(IllegalStateException.class, () -> {
      new FindbugsXmlReportParser(xmlReport);
    });
    
    assertEquals("The findbugs XML report can't be found at '" + xmlReport.getAbsolutePath() + "'", thrown.getMessage());
  }

  @Test
  void testGetViolations() {
    assertThat(violations).hasSize(2);

    FindbugsXmlReportParser.XmlBugInstance fbViolation = violations.get(0);
    assertThat(fbViolation.getType()).isEqualTo("AM_CREATES_EMPTY_ZIP_FILE_ENTRY");
    assertThat(fbViolation.getLongMessage()).isEqualTo("Empty zip file entry created in org.sonar.commons.ZipUtils._zip(String, File, ZipOutputStream)");

    FindbugsXmlReportParser.XmlSourceLineAnnotation sourceLine = fbViolation.getPrimarySourceLine();
    assertThat(sourceLine.getStart()).isEqualTo(107);
    assertThat(sourceLine.getEnd()).isEqualTo(107);
    assertThat(sourceLine.getClassName()).isEqualTo("org.sonar.commons.ZipUtils");
  }

  @Test
  void testGetSonarJavaFileKey() {
    FindbugsXmlReportParser.XmlSourceLineAnnotation sourceLine = new FindbugsXmlReportParser.XmlSourceLineAnnotation();
    sourceLine.className = "org.sonar.batch.Sensor";
    assertThat(sourceLine.getSonarJavaFileKey()).isEqualTo("org.sonar.batch.Sensor");
    sourceLine.className = "Sensor";
    assertThat(sourceLine.getSonarJavaFileKey()).isEqualTo("Sensor");
    sourceLine.className = "org.sonar.batch.Sensor$1";
    assertThat(sourceLine.getSonarJavaFileKey()).isEqualTo("org.sonar.batch.Sensor");
  }

  private final File getFile(String filename) {
    try {
      return new File(getClass().getResource(filename).toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Unable to open file " + filename, e);
    }
  }
}
