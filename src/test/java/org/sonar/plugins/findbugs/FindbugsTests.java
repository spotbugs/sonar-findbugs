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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.CharUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.Assertions;
import org.sonar.api.rules.Rule;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.findbugs.util.TestUtils;
import org.xml.sax.SAXException;

public abstract class FindbugsTests {

  protected void assertXmlAreSimilar(String actualContent, String expectedFileName) throws SAXException, IOException {
    File expectedContent = TestUtils.getResource("/org/sonar/plugins/findbugs/" + expectedFileName);
    assertSimilarXml(expectedContent, actualContent);
  }

  protected List<Rule> buildRulesFixture() {
    List<Rule> rules = new ArrayList<Rule>();

    Rule rule1 = Rule.create(FindbugsRulesDefinition.REPOSITORY_KEY, "DLS_DEAD_LOCAL_STORE", "DLS: Dead store to local variable");
    Rule rule2 = Rule.create(FindbugsRulesDefinition.REPOSITORY_KEY, "URF_UNREAD_FIELD", "UrF: Unread field");

    rules.add(rule1);
    rules.add(rule2);

    return rules;
  }

  private void assertSimilarXml(File expectedFile, String xml) throws SAXException, IOException {
    XMLUnit.setIgnoreWhitespace(true);
    Reader reader = new FileReader(expectedFile);
    Diff diff = XMLUnit.compareXML(reader, xml);
    String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
    Assertions.assertTrue(diff.similar(), message);
  }
}
