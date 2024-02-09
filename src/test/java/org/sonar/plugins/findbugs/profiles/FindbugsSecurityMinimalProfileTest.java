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
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.java.Java;

class FindbugsSecurityMinimalProfileTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldCreateProfile() {
    RuleFinder ruleFinder = FakeRuleFinder.createWithAllRules();
    FindbugsProfile findbugsProfile = new FindbugsProfile(ruleFinder);
    Context context = new Context();
    findbugsProfile.define(context);

    BuiltInQualityProfile profile = context.profile(Java.KEY, FindbugsProfile.FINDBUGS_SECURITY_MINIMAL_PROFILE_NAME);
    assertThat(logTester.getLogs(Level.ERROR)).isEmpty();
    // FSB rules must be added to FsbClassifier.groovy otherwise new rules metadata are not added in rules-findsecbugs.xml
    assertThat(logTester.getLogs(Level.WARN)).isEmpty();
    // The standard FindBugs include only 9. Fb-Contrib and FindSecurityBugs include other rules
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(8);
    // 94 rules total - 8 fb = 86
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindSecurityBugsRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(96);

    FindbugsProfileTest.assertHasOnlyRulesForLanguage(profile.rules(), Java.KEY);
  }
}
