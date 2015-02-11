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
import org.sonar.api.rules.XMLRuleParser;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Mock of the interface RuleFinder which include rules from the Std rules repo only.
 *
 * Use by the test FindbugsSecurityOnlyProfileTest which test a profile on security rules.
 */
public class FakeRuleFinderAllRepo {

  private FakeRuleFinderAllRepo() {
  }

  public static RuleFinder create() {
    RuleFinder ruleFinder = mock(RuleFinder.class);

    //Std rules
    ServerFileSystem sfs = mock(ServerFileSystem.class);
    FindbugsRuleRepository repo = new FindbugsRuleRepository(sfs, new XMLRuleParser());
    final List<Rule> stdRules = repo.createRules();
    for (Rule rule : stdRules) {
      rule.setRepositoryKey(FindbugsRuleRepository.REPOSITORY_KEY);
    }

    when(ruleFinder.findAll(argThat(new CustomTypeSafeMatcher<RuleQuery>("RuleQuery") {
      @Override
      public boolean matchesSafely(RuleQuery ruleQuery) {
        return FindbugsRuleRepository.REPOSITORY_KEY.equals(ruleQuery.getRepositoryKey());
      }
    }))).thenReturn(stdRules);

    //Adding FindSecBugs rules
    FindSecurityBugsRuleRepository fsbRepo = new FindSecurityBugsRuleRepository(new XMLRuleParser());
    final List<Rule> fsbRules = fsbRepo.createRules();
    for (Rule rule : fsbRules) {
      rule.setRepositoryKey(FindSecurityBugsRuleRepository.REPOSITORY_KEY);
    }

    when(ruleFinder.findAll(argThat(new CustomTypeSafeMatcher<RuleQuery>("RuleQuery") {
      @Override
      public boolean matchesSafely(RuleQuery ruleQuery) {
        return FindSecurityBugsRuleRepository.REPOSITORY_KEY.equals(ruleQuery.getRepositoryKey());
      }
    }))).thenReturn(fsbRules);


    when(ruleFinder.findByKey(any(String.class), any(String.class))).thenAnswer(new Answer<Rule>() {
      @Override
      public Rule answer(InvocationOnMock invocation) throws Throwable {
        String key = (String) invocation.getArguments()[1];
        for (Rule rule : stdRules) {
          if (rule.getKey().equals(key)) {
            return rule;
          }
        }
        for (Rule rule : fsbRules) {
          if (rule.getKey().equals(key)) {
            return rule;
          }
        }
        return null;
      }
    });

    return ruleFinder;
  }

}
