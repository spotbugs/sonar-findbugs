/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.plugins.findbugs;

import org.hamcrest.CustomTypeSafeMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

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

  public static RuleFinder create(boolean onlyFindbugs) {
    RuleFinder ruleFinder = mock(RuleFinder.class);

    RuleRepository repo = new FindbugsRuleRepository(mock(ServerFileSystem.class), new XMLRuleParser());
    final List<Rule> findbugsRules = getRulesFromRepo(repo, FindbugsRuleRepository.REPOSITORY_KEY);
    configRuleFinderForRepo(ruleFinder, FindbugsRuleRepository.REPOSITORY_KEY, findbugsRules);

    if (!onlyFindbugs) {
      repo = new FindSecurityBugsRuleRepository(new XMLRuleParser());
      final List<Rule> findsecuritybugsRules = getRulesFromRepo(repo, FindSecurityBugsRuleRepository.REPOSITORY_KEY);
      configRuleFinderForRepo(ruleFinder, FindSecurityBugsRuleRepository.REPOSITORY_KEY, findsecuritybugsRules);

      repo = new FbContribRuleRepository(new XMLRuleParser());
      final List<Rule> fbContribRules = getRulesFromRepo(repo, FbContribRuleRepository.REPOSITORY_KEY);
      configRuleFinderForRepo(ruleFinder, FbContribRuleRepository.REPOSITORY_KEY, fbContribRules);
    }
    return ruleFinder;
  }

  public static RuleFinder create() {
    return create(false);
  }

  private static void configRuleFinderForRepo(RuleFinder ruleFinder, final String repositoryKey, final List<Rule> rules) {
    when(ruleFinder.findAll(argThat(new CustomTypeSafeMatcher<RuleQuery>("RuleQuery") {
      @Override
      public boolean matchesSafely(RuleQuery ruleQuery) {
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

  private static List<Rule> getRulesFromRepo(RuleRepository repo, String repositoryKey) {
    List<Rule> rules = repo.createRules();
    for (Rule rule : rules) {
      rule.setRepositoryKey(repositoryKey);
    }
    return rules;
  }
}
