/*
 * SonarQube SpotBugs Plugin
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
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.findbugs.FindbugsProfileImporter;
import org.sonar.plugins.findbugs.language.scala.Scala;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsScalaRulesDefinition;
import org.sonar.plugins.findbugs.util.JupiterLogTester;

class FindbugsScalaProfileTest {

  @RegisterExtension
  public LogTester logTester = new JupiterLogTester();

  @Test
  void shouldCreateProfile() {
    FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithAllRules());
    FindbugsSecurityScalaProfile findbugsProfile = new FindbugsSecurityScalaProfile(importer);
    Context context = new Context();
    findbugsProfile.define(context);
    
    BuiltInQualityProfile profile = context.profile(Scala.KEY, FindbugsSecurityScalaProfile.FINDBUGS_SECURITY_SCALA_PROFILE_NAME);
    assertThat(profile.rules()).hasSize(FindSecurityBugsScalaRulesDefinition.RULE_COUNT);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindSecurityBugsScalaRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(FindSecurityBugsScalaRulesDefinition.RULE_COUNT);
    assertThat(logTester.getLogs(LoggerLevel.ERROR)).isNull();

    FindbugsProfileTest.assertHasOnlyRulesForLanguage(profile.rules(), Scala.KEY);
  }
}
