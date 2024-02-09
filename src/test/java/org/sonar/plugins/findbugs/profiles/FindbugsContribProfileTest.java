package org.sonar.plugins.findbugs.profiles;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.java.Java;

import java.util.List;
import java.util.stream.Collectors;

class FindbugsContribProfileTest { 

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();
  
  @Test
  void shouldCreateProfile() {
    RuleFinder ruleFinder = FakeRuleFinder.createWithAllRules();
    FindbugsProfile findbugsProfile = new FindbugsProfile(ruleFinder);
    Context context = new Context();
    findbugsProfile.define(context);
    
    BuiltInQualityProfile profile = context.profile(Java.KEY, FindbugsProfile.FB_CONTRIB_PROFILE_NAME);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(FindbugsRulesDefinition.RULE_COUNT);
    assertThat(profile.rules().stream().filter(r -> r.repoKey().equals(FbContribRulesDefinition.REPOSITORY_KEY)).count()).isEqualTo(FbContribRulesDefinition.RULE_COUNT);
    assertThat(logTester.getLogs(Level.ERROR)).isEmpty();

    FindbugsProfileTest.assertHasOnlyRulesForLanguage(profile.rules(), Java.KEY);
  }
  
  @Test
  void coreRulesAreFindBugsProfile() {
    RuleFinder ruleFinder = FakeRuleFinder.createWithAllRules();
    FindbugsProfile findbugsProfile = new FindbugsProfile(ruleFinder);
    Context context = new Context();
    findbugsProfile.define(context);
    
    BuiltInQualityProfile fbContribQualityProfile = context.profile(Java.KEY, FindbugsProfile.FB_CONTRIB_PROFILE_NAME);
    BuiltInQualityProfile findbugsQualityProfile = context.profile(Java.KEY, FindbugsProfile.FINDBUGS_PROFILE_NAME);
    
    List<String> findbugsRulesInFbContribProfile = fbContribQualityProfile
    		.rules()
    		.stream()
    		.filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY))
    		.map(BuiltInActiveRule::ruleKey)
    		.sorted()
    		.collect(Collectors.toList());
    
    List<String> findbugsRules = findbugsQualityProfile
    		.rules()
    		.stream()
    		.filter(r -> r.repoKey().equals(FindbugsRulesDefinition.REPOSITORY_KEY))
    		.map(BuiltInActiveRule::ruleKey)
    		.sorted()
    		.collect(Collectors.toList());
    
    assertThat(findbugsRulesInFbContribProfile)
    .containsExactlyElementsOf(findbugsRules)
    .hasSize(FindbugsRulesDefinition.RULE_COUNT);
  }
}
