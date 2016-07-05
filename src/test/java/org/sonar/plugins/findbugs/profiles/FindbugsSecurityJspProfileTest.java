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
import org.sonar.plugins.findbugs.profiles.FindbugsSecurityJspProfile;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsSecurityJspProfileTest {

  @Test
  public void shouldCreateProfile() {
    FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithAllRules());
    FindbugsSecurityJspProfile secJspProfile = new FindbugsSecurityJspProfile(importer);
    ValidationMessages validation = ValidationMessages.create();
    RulesProfile profile = secJspProfile.createProfile(validation);

    //There are 6 rules that are JSP specific (the other findbugs rules can also be found in JSP files)
    assertThat(validation.getErrors()).isEmpty();
    assertThat(validation.getWarnings()).isEmpty();
    assertThat(profile.getActiveRulesByRepository(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY)).hasSize(6);
    assertThat(profile.getActiveRulesByRepository(FindbugsRulesDefinition.REPOSITORY_KEY)).hasSize(0);
  }
}
