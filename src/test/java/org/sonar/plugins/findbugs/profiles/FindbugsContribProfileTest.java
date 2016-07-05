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
    assertThat(profile.getActiveRulesByRepository(FindbugsRulesDefinition.REPOSITORY_KEY)).hasSize(452);
    assertThat(profile.getActiveRulesByRepository(FbContribRulesDefinition.REPOSITORY_KEY)).hasSize(257);
    assertThat(validation.hasErrors()).isFalse();
  }
}
