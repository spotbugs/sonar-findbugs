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
import org.sonar.api.rule.Severity;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;
import org.sonar.plugins.java.Java;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsProfileImporterTest {   
	
	private static final String TEST_PROFILE = "TEST_PROFILE";

	@org.junit.Rule
  public LogTester logTester = new LogTester();

	private	Context context = new Context();
  private final FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithOnlyFindbugsRules());

  @Test
  public void shouldImportPatterns() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream findbugsConf = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportPatterns.xml");
    importer.importProfile(new InputStreamReader(findbugsConf), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);

    assertThat(profile.rules()).hasSize(2);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "NP_CLOSING_NULL")).isNotNull();
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE")).isNotNull();
  }

  @Test
  public void shouldImportPatternsWithMultiplePriority() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream findbugsConf = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportPatternsWithMultiplePriorities.xml");
    importer.importProfile(new InputStreamReader(findbugsConf), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);

    assertThat(profile.rules()).hasSize(3);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "NP_CLOSING_NULL")).isNotNull();
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "NP_CLOSING_NULL").overriddenSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE")).isNotNull();
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE").overriddenSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "DLS_DEAD_LOCAL_STORE")).isNotNull();
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "DLS_DEAD_LOCAL_STORE").overriddenSeverity()).isEqualTo(Severity.MAJOR);
  }

  @Test
  public void shouldNotImportIfInvalidPriority() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream findbugsConf = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/invalidPriority.xml");
    importer.importProfile(new InputStreamReader(findbugsConf), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);

    assertThat(profile.rules()).hasSize(0);
  }

  @Test
  public void shouldImportCodes() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportCodes.xml");
    importer.importProfile(new InputStreamReader(input), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(20);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "EC_INCOMPATIBLE_ARRAY_COMPARE")).isNotNull();
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY")).isNotNull();
  }

  @Test
  public void shouldImportCategories() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportCategories.xml");
    importer.importProfile(new InputStreamReader(input), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(151);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "BC_IMPOSSIBLE_DOWNCAST")).isNotNull();
  }

  @Test
  public void shouldImportConfigurationBugInclude() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugs-include.xml");
    importer.importProfile(new InputStreamReader(input), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(12);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE")).isNotNull();
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
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/uncorrectFindbugsXml.xml");
    importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(0);
    assertThat(logTester.getLogs(LoggerLevel.ERROR)).hasSize(1);
  }

  @Test
  public void testImportingXmlFileWithUnknownRule() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownRule.xml");
    importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(1);
    assertThat(logTester.getLogs(LoggerLevel.ERROR)).isNull();
    assertThat(logTester.getLogs(LoggerLevel.WARN)).hasSize(1);
  }

  @Test
  public void testImportingXmlFileWithUnknownCategory() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCategory.xml");
    importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(151);
    assertThat(logTester.getLogs(LoggerLevel.ERROR)).isNull();
    assertThat(logTester.getLogs(LoggerLevel.WARN)).hasSize(1);
  }

  @Test
  public void testImportingXmlFileWithUnknownCode() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCode.xml");
    importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(12);
    assertThat(logTester.getLogs(LoggerLevel.ERROR)).isNull();
    assertThat(logTester.getLogs(LoggerLevel.WARN)).hasSize(1);
  }
  
  private BuiltInActiveRule findActiveRule(BuiltInQualityProfile profile, String repositoryKey, String ruleKey) {
  	return profile
  			.rules()
  			.stream()
  			.filter(r -> r.repoKey().equals(repositoryKey) && r.ruleKey().equals(ruleKey))
  			.findAny()
  			.orElse(null);
  }
}
