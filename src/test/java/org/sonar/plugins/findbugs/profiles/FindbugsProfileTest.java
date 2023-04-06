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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.findbugs.FindbugsProfileImporter;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.language.scala.Scala;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsScalaRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.java.Java;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

class FindbugsProfileTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldCreateProfile() {
    FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithAllRules());
    FindbugsProfile findbugsProfile = new FindbugsProfile(importer);
    Context context = new Context();
    findbugsProfile.define(context);
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, FindbugsProfile.FINDBUGS_PROFILE_NAME);
    assertThat(profile.rules()).hasSize(FindbugsRulesDefinition.RULE_COUNT);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(FindbugsRulesDefinition.RULE_COUNT);
    assertThat(logTester.getLogs(Level.ERROR)).isEmpty();

    FindbugsProfileTest.assertHasOnlyRulesForLanguage(profile.rules(), Java.KEY);
  }
  
  public static void assertHasOnlyRulesForLanguage(List<BuiltInActiveRule> rules, String language) {
    switch (language) {
    case Java.KEY:
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("Java profiles must only contain Java rules").isZero();
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindSecurityBugsScalaRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("Java profiles must only contain Java rules").isZero();
      break;
    case Jsp.KEY:
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("JSP profiles must only contain JSP rules").isZero();
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FbContribRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("JSP profiles must only contain JSP rules").isZero();
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindSecurityBugsRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("JSP profiles must only contain JSP rules").isZero();
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindSecurityBugsScalaRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("JSP profiles must only contain JSP rules").isZero();
      break;
    case Scala.KEY:
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("Scala profiles must only contain Scala rules").isZero();
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FbContribRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("Scala profiles must only contain Scala rules").isZero();
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindSecurityBugsRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("Scala profiles must only contain Scala rules").isZero();
      assertThat(rules.stream().filter(r -> r.repoKey().equals(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY)).count()).withFailMessage("Scala profiles must only contain Scala rules").isZero();
      break;
    default:
      throw new IllegalArgumentException("Unexpected language: " + language);
    }
  }
}
