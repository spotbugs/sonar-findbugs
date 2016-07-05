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

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsProfileImporterTest {

  private final FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithOnlyFindbugsRules());

  @Test
  public void shouldImportPatterns() {
    InputStream findbugsConf = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportPatterns.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(findbugsConf), ValidationMessages.create());

    assertThat(profile.getActiveRules()).hasSize(2);
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "NP_CLOSING_NULL")).isNotNull();
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE")).isNotNull();
  }

  @Test
  public void shouldImportPatternsWithMultiplePriority() {
    InputStream findbugsConf = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportPatternsWithMultiplePriorities.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(findbugsConf), ValidationMessages.create());

    assertThat(profile.getActiveRules()).hasSize(3);
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "NP_CLOSING_NULL")).isNotNull();
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "NP_CLOSING_NULL").getSeverity()).isEqualTo(RulePriority.BLOCKER);
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE")).isNotNull();
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE").getSeverity()).isEqualTo(RulePriority.BLOCKER);
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "DLS_DEAD_LOCAL_STORE")).isNotNull();
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "DLS_DEAD_LOCAL_STORE").getSeverity()).isEqualTo(RulePriority.MAJOR);
  }

  @Test
  public void shouldNotImportIfInvalidPriority() {
    InputStream findbugsConf = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/invalidPriority.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(findbugsConf), ValidationMessages.create());

    assertThat(profile.getActiveRules()).hasSize(0);
  }

  @Test
  public void shouldImportCodes() {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportCodes.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(input), ValidationMessages.create());
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results).hasSize(20);
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "EC_INCOMPATIBLE_ARRAY_COMPARE")).isNotNull();
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY")).isNotNull();
  }

  @Test
  public void shouldImportCategories() {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportCategories.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(input), ValidationMessages.create());
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results).hasSize(159);
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "BC_IMPOSSIBLE_DOWNCAST")).isNotNull();
  }

  @Test
  public void shouldImportConfigurationBugInclude() {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugs-include.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(input), ValidationMessages.create());
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results).hasSize(12);
    assertThat(profile.getActiveRule(FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE")).isNotNull();
  }

  @Test
  public void shouldBuildModuleTreeFromXml() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/test_module_tree.xml");

    XStream xStream = FindBugsFilter.createXStream();
    FindBugsFilter filter = (FindBugsFilter) xStream.fromXML(IOUtils.toString(input));

    List<Match> matches = filter.getMatchs();
    assertThat(matches).hasSize(2);
    assertThat(matches.get(0).getBug().getPattern()).isEqualTo("DLS_DEAD_LOCAL_STORE");
    assertThat(matches.get(1).getBug().getPattern()).isEqualTo("URF_UNREAD_FIELD");
  }

  @Test
  public void testImportingUncorrectXmlFile() {
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/uncorrectFindbugsXml.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results).hasSize(0);
    assertThat(messages.getErrors()).hasSize(1);
  }

  @Test
  public void testImportingXmlFileWithUnknownRule() {
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownRule.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results).hasSize(1);
    assertThat(messages.getErrors()).isEmpty();
    assertThat(messages.getWarnings()).hasSize(1);
  }

  @Test
  public void testImportingXmlFileWithUnknownCategory() {
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCategory.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results).hasSize(159);
    assertThat(messages.getErrors()).isEmpty();
    assertThat(messages.getWarnings()).hasSize(1);
  }

  @Test
  public void testImportingXmlFileWithUnknownCode() {
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCode.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results).hasSize(12);
    assertThat(messages.getErrors()).isEmpty();
    assertThat(messages.getWarnings()).hasSize(1);
  }
}
