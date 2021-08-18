package org.sonar.plugins.findbugs.profiles;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.findbugs.FindbugsProfileImporter;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.java.Java;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsContribProfileTest { 

  @Rule
  public LogTester logTester = new LogTester();
  
  @Test
  public void shouldCreateProfile() {
    FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithAllRules());
    FindbugsContribProfile findbugsProfile = new FindbugsContribProfile(importer);
    Context context = new Context();
    findbugsProfile.define(context);
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, FindbugsContribProfile.FB_CONTRIB_PROFILE_NAME);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(FindbugsRulesDefinition.RULE_COUNT);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FbContribRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(FbContribRulesDefinition.RULE_COUNT);
    assertThat(logTester.getLogs(LoggerLevel.ERROR)).isNull();
  }
}
