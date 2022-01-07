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
package org.sonar.plugins.findbugs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Rule;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.java.Java;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FindbugsRulesDefinitionTest {
  /**
   * The SpotBugs rules repository
   */
  private RulesDefinition.Repository repository;
  
  @BeforeEach
  public void setupRepository() {
    FindbugsRulesDefinition definition = new FindbugsRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    
    repository = context.repository(FindbugsRulesDefinition.REPOSITORY_KEY);
  }
  
  @Test
  public void testRepositoryRulesCount() {
    assertThat(repository.name()).isEqualTo(FindbugsRulesDefinition.REPOSITORY_NAME);
    assertThat(repository.language()).isEqualTo(Java.KEY);

    List<Rule> rules = repository.rules();
    assertThat(rules).hasSize(FindbugsRulesDefinition.RULE_COUNT + FindbugsRulesDefinition.DEACTIVED_RULE_COUNT);

    for (Rule rule : rules) {
      assertThat(rule.key()).isNotNull();
      assertThat(rule.internalKey()).isEqualTo(rule.key());
      assertThat(rule.name()).isNotNull();
      assertThat(rule.htmlDescription()).isNotNull();
    }
  }
  
  /**
   * Rule TLW_TWO_LOCK_NOTIFY was marked deprecated in Spotbugs so we're testing that the SonarQube rule status is updated accordingly
   * 
   * In case that rule is removed in a future release, this unit test will need to be updated to test against another deprecated rule
   */
  @Test
  public void testDeprecatedRules() {
    Rule rule = repository.rule("TLW_TWO_LOCK_NOTIFY");
    
    assertThat(rule).isNotNull();
    assertThat(rule.status()).isEqualTo(RuleStatus.DEPRECATED);
  }
}
