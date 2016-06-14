package org.sonar.plugins.findbugs.rule;

import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link ActiveRules} is the future API for doing lookup to find rules. It has similar operations as {@link RuleFinder}.
 * As long as {@link RuleFinder}, we can wrap the same mock {@link FakeRuleFinder} to create the behavior of a basic
 * repository.
 */
public class FakeActiveRules {

  public static ActiveRules createWithAllRules() {
    return new ActiveRulesMock(FakeRuleFinder.createWithAllRules());
  }

  public static ActiveRules createWithOnlyFindbugsRules() {
    return new ActiveRulesMock(FakeRuleFinder.createWithOnlyFindbugsRules());
  }

  public static ActiveRules createWithOnlyFbContribRules() {
    return new ActiveRulesMock(FakeRuleFinder.createWithOnlyFbContribRules());
  }

  public static ActiveRules createWithOnlyFindSecBugsRules() {
    return new ActiveRulesMock(FakeRuleFinder.createWithOnlyFindSecBugsRules());
  }

  public static class ActiveRulesMock implements ActiveRules {
    private RuleFinder ruleFinder;

    public ActiveRulesMock(RuleFinder ruleFinder) {
      this.ruleFinder = ruleFinder;
    }

    @Override
    public ActiveRule find(RuleKey ruleKey) {
      Rule r = ruleFinder.findByKey(ruleKey);
      if (r == null) return null;
      return new SimpleActiveRule(r);
    }

    @Override
    public Collection<ActiveRule> findAll() {
      return null;
    }

    @Override
    public Collection<ActiveRule> findByRepository(String repository) {
      return transformRules(ruleFinder.findAll(RuleQuery.create().withRepositoryKey(repository)));
    }

    @Override
    public Collection<ActiveRule> findByLanguage(String language) {
      return transformRules(ruleFinder.findAll(RuleQuery.create()));
    }

    @Override
    public ActiveRule findByInternalKey(String repository, String internalKey) {
      Rule r = ruleFinder.findByKey(repository, internalKey);
      if (r == null) return null;
      return new SimpleActiveRule(r);
    }

    public Collection<ActiveRule> transformRules(Collection<Rule> all) {
      List<ActiveRule> newList = new ArrayList<>();
      for (Rule r : all) {
        newList.add(new SimpleActiveRule(r));
      }
      return newList;
    }
  }
}
