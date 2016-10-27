package org.sonar.plugins.findbugs.profiles;

import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.findbugs.FindbugsProfileImporter;
import org.sonar.plugins.findbugs.rule.FakeRuleFinder;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsContribProfileTest {
  @Test
  public void shouldCreateProfile() {
    FindbugsProfileImporter importer = new FindbugsProfileImporter(FakeRuleFinder.createWithAllRules());
    FindbugsContribProfile findbugsProfile = new FindbugsContribProfile(importer);
    ValidationMessages validation = ValidationMessages.create();
    RulesProfile profile = findbugsProfile.createProfile(validation);
    assertThat(profile.getActiveRulesByRepository(FindbugsRulesDefinition.REPOSITORY_KEY))
            .hasSize(FindbugsRulesDefinition.RULE_COUNT);
    assertThat(profile.getActiveRulesByRepository(FbContribRulesDefinition.REPOSITORY_KEY))
            .hasSize(FbContribRulesDefinition.RULE_COUNT);
    assertThat(validation.hasErrors()).isFalse();
  }
}
