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
package org.sonar.plugins.findbugs.profiles;

import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.findbugs.FindbugsProfileImporter;
import org.sonar.plugins.findbugs.profiles.FindbugsSecurityMinimalProfile;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsSecurityMinimalProfileTest {

  @Test
  public void shouldCreateProfile() {
    FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithAllRules());
    FindbugsSecurityMinimalProfile secOnlyProfile = new FindbugsSecurityMinimalProfile(importer);
    ValidationMessages validation = ValidationMessages.create();
    RulesProfile profile = secOnlyProfile.createProfile(validation);


    assertThat(validation.getErrors()).isEmpty();
    assertThat(validation.getWarnings()).isEmpty();
    // The standard FindBugs include only 9. Fb-Contrib and FindSecurityBugs include other rules
    assertThat(profile.getActiveRulesByRepository(FindbugsRulesDefinition.REPOSITORY_KEY)).hasSize(8);
    // 62 rules total - 20 informational = 42 major or critical
    assertThat(profile.getActiveRulesByRepository(FindSecurityBugsRulesDefinition.REPOSITORY_KEY)).hasSize(75);
  }
}
