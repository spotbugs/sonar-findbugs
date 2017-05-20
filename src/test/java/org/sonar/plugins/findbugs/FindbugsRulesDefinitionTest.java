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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Rule;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.java.Java;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsRulesDefinitionTest {
  @Test
  public void test() {
    FindbugsRulesDefinition definition = new FindbugsRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(FindbugsRulesDefinition.REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo(FindbugsRulesDefinition.REPOSITORY_NAME);
    assertThat(repository.language()).isEqualTo(Java.KEY);

    List<Rule> rules = repository.rules();
    assertThat(rules).hasSize(FindbugsRulesDefinition.RULE_COUNT + FindbugsRulesDefinition.DEACTIVED_RULE_COUNT);

    List<String> rulesWithMissingSQALE = Lists.newLinkedList();
    for (Rule rule : rules) {
      assertThat(rule.key()).isNotNull();
      assertThat(rule.internalKey()).isEqualTo(rule.key());
      assertThat(rule.name()).isNotNull();
      assertThat(rule.htmlDescription()).isNotNull();
      if (rule.debtSubCharacteristic() == null) {
        rulesWithMissingSQALE.add(rule.key());
      }
    }
    // These rules are "rejected" Findbugs rules
    //FIXME:
    //assertThat(rulesWithMissingSQALE).containsOnly("CNT_ROUGH_CONSTANT_VALUE", "TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED");
  }
}
