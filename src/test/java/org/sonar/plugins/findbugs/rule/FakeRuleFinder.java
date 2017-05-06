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
package org.sonar.plugins.findbugs.rule;

import com.google.common.collect.Lists;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mock of the interface RuleFinder which include rules from the Std rules repo only.
 */
public class FakeRuleFinder {

  private FakeRuleFinder() {
  }

  private static RuleFinder create(boolean findbugs, boolean fbContrib, boolean findSecBug, boolean findSecBugJsp) {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    RulesDefinition.Context context = new RulesDefinition.Context();

    if (findbugs) {
      RulesDefinition rulesDefinition = new FindbugsRulesDefinition();
      rulesDefinition.define(context);
      configRuleFinderForRepo(ruleFinder, context, FindbugsRulesDefinition.REPOSITORY_KEY);
    }

    if (fbContrib) {
      RulesDefinition rulesDefinition = new FbContribRulesDefinition();
      rulesDefinition.define(context);
      configRuleFinderForRepo(ruleFinder, context, FbContribRulesDefinition.REPOSITORY_KEY);
    }

    if (findSecBug) {
      RulesDefinition rulesDefinition = new FindSecurityBugsRulesDefinition();
      rulesDefinition.define(context);
      configRuleFinderForRepo(ruleFinder, context, FindSecurityBugsRulesDefinition.REPOSITORY_KEY);
    }

    if (findSecBugJsp) {
      RulesDefinition rulesDefinition = new FindSecurityBugsJspRulesDefinition();
      rulesDefinition.define(context);
      configRuleFinderForRepo(ruleFinder, context, FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY);
    }

    return ruleFinder;
  }

  public static RuleFinder createWithAllRules() {
    return create(true, true, true, true);
  }

  public static RuleFinder createWithOnlyFindbugsRules() {
    return create(true, false, false,false);
  }

  public static RuleFinder createWithOnlyFbContribRules() {
    return create(false, true, false,false);
  }

  public static RuleFinder createWithOnlyFindSecBugsRules() {
    return create(false, false, true,false);
  }

  private static void configRuleFinderForRepo(RuleFinder ruleFinder, final Context context, final String repositoryKey) {
    final RulesDefinition.Repository repository = context.repository(repositoryKey);
    final List<Rule> rules = convert(repository.rules());

    when(ruleFinder.findAll(argThat(new ArgumentMatcher<RuleQuery>() {
      @Override
      public boolean matches(RuleQuery ruleQuery) {
        if (ruleQuery == null) {
          return false;
        }
        return repositoryKey.equals(ruleQuery.getRepositoryKey());
      }
    }))).thenReturn(rules);
    when(ruleFinder.findByKey(eq(repositoryKey), any(String.class))).thenAnswer(new Answer<Rule>() {
      @Override
      public Rule answer(InvocationOnMock invocation) throws Throwable {
        String key = (String) invocation.getArguments()[1];
        for (Rule rule : rules) {
          if (rule.getKey().equals(key)) {
            return rule;
          }
        }
        return null;
      }
    });
  }

  private static List<Rule> convert(List<RulesDefinition.Rule> rules) {
    List<Rule> results = Lists.newArrayListWithCapacity(rules.size());
    for (RulesDefinition.Rule rule : rules) {
      Rule newRule = Rule.create(rule.repository().key(), rule.key(), rule.name()).setDescription(rule.htmlDescription()).setRepositoryKey(rule.repository().key());
      results.add(newRule);
    }
    return results;
  }
}
