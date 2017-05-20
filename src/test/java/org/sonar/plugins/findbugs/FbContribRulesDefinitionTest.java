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

import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Rule;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.java.Java;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FbContribRulesDefinitionTest {

  @Test
  public void test() {
    FbContribRulesDefinition definition = new FbContribRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(FbContribRulesDefinition.REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo(FbContribRulesDefinition.REPOSITORY_NAME);
    assertThat(repository.language()).isEqualTo(Java.KEY);

    List<Rule> rules = repository.rules();
    assertThat(rules).hasSize(FbContribRulesDefinition.RULE_COUNT + FbContribRulesDefinition.DEACTIVED_RULE_COUNT);

    for (Rule rule : rules) {
      assertThat(rule.key()).isNotNull();
      assertThat(rule.internalKey()).isEqualTo(rule.key());
      assertThat(rule.name()).isNotNull();
      assertThat(rule.htmlDescription()).isNotNull();
    }
  }

}
