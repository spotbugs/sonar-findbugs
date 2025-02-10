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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.event.Level;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;
import org.sonar.plugins.java.Java;

import com.thoughtworks.xstream.XStream;

class FindbugsProfileImporterTest {   
	
	private static final String TEST_PROFILE = "TEST_PROFILE";

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

	private	Context context = new Context();
  private final FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithOnlyFindbugsRules());

  @Test
  void shouldImportPatterns() {
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
  void shouldImportPatternsWithMultiplePriority() {
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
  void shouldNotImportIfInvalidPriority() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream findbugsConf = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/invalidPriority.xml");
    importer.importProfile(new InputStreamReader(findbugsConf), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);

    assertThat(profile.rules()).isEmpty();
  }

  @Test
  void shouldImportCodes() {
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
  void shouldImportCategories() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportCategories.xml");
    importer.importProfile(new InputStreamReader(input), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(160);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "BC_IMPOSSIBLE_DOWNCAST")).isNotNull();
  }

  @Test
  void shouldImportConfigurationBugInclude() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugs-include.xml");
    importer.importProfile(new InputStreamReader(input), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(23);
    assertThat(findActiveRule(profile, FindbugsRulesDefinition.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE")).isNotNull();
  }

  @Test
  void shouldBuildModuleTreeFromXml() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/test_module_tree.xml");

    XStream xStream = FindBugsFilter.createXStream();
    FindBugsFilter filter = (FindBugsFilter) xStream.fromXML(IOUtils.toString(input, StandardCharsets.UTF_8));

    List<Match> matches = filter.getMatchs();
    assertThat(matches).hasSize(2);
    assertThat(matches.get(0).getBug().getPattern()).isEqualTo("DLS_DEAD_LOCAL_STORE");
    assertThat(matches.get(1).getBug().getPattern()).isEqualTo("URF_UNREAD_FIELD");
  }

  @Test
  void testImportingUncorrectXmlFile() {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/uncorrectFindbugsXml.xml");
    importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).isEmpty();
    assertThat(logTester.getLogs(Level.ERROR)).hasSize(1);
  }

  @ParameterizedTest
  @CsvSource({
      "/org/sonar/plugins/findbugs/findbugsXmlWithUnknownRule.xml,1",
      "/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCategory.xml,160",
      "/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCode.xml,12"})
  void profileImport(String profilePath, int expectedSize) {
  	NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(TEST_PROFILE, Java.KEY);
    InputStream uncorrectFindbugsXml = getClass().getResourceAsStream(profilePath);
    importer.importProfile(new InputStreamReader(uncorrectFindbugsXml), newProfile);
    
    newProfile.done();
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, TEST_PROFILE);
    Collection<BuiltInActiveRule> results = profile.rules();

    assertThat(results).hasSize(expectedSize);
    assertThat(logTester.getLogs(Level.ERROR)).isEmpty();
    assertThat(logTester.getLogs(Level.WARN)).hasSize(1);
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
