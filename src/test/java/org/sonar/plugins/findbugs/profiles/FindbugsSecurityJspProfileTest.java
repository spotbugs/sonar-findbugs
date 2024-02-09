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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;

class FindbugsSecurityJspProfileTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldCreateProfile() {
    RuleFinder ruleFinder = FakeRuleFinder.createWithAllRules();
    FindbugsProfile findbugsProfile = new FindbugsProfile(ruleFinder);
    Context context = new Context();
    findbugsProfile.define(context);

    //There are 6 rules that are JSP specific (the other findbugs rules can also be found in JSP files)
    BuiltInQualityProfile profile = context.profile(Jsp.KEY, FindbugsProfile.FINDBUGS_SECURITY_JSP_PROFILE_NAME);
    assertThat(logTester.getLogs(Level.ERROR)).isEmpty();
    assertThat(logTester.getLogs(Level.WARN)).isEmpty();
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(6);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).isZero();

    FindbugsProfileTest.assertHasOnlyRulesForLanguage(profile.rules(), Jsp.KEY);
  }

  @Test
  void disabledRuleMustNotBeActivated() {
    RuleFinder ruleFinder = FakeRuleFinder.createWithAllRules();
    
    // Mark a rule as removed
    org.sonar.api.rules.Rule rule = ruleFinder.findByKey(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY, "XSS_JSP_PRINT");
    rule.setStatus(org.sonar.api.rules.Rule.STATUS_REMOVED);

    FindbugsProfile findbugsProfile = new FindbugsProfile(ruleFinder);
    Context context = new Context();
    findbugsProfile.define(context);

    //There should be 5 rules left since we removed one
    BuiltInQualityProfile profile = context.profile(Jsp.KEY, FindbugsProfile.FINDBUGS_SECURITY_JSP_PROFILE_NAME);
    assertThat(logTester.getLogs(Level.ERROR)).isEmpty();
    assertThat(logTester.getLogs(Level.WARN)).isEmpty();
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(5);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).isZero();
  }
}
