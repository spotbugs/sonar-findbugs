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
package org.sonar.plugins.findbugs.rules;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.plugins.java.Java;

public class FbContribRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "fb-contrib";
  public static final String REPOSITORY_NAME = "FindBugs Contrib";
  public static final int RULE_COUNT = 283;
  public static final int DEACTIVED_RULE_COUNT = 0;

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, Java.KEY)
      .setName(REPOSITORY_NAME);

    RulesDefinitionXmlLoader ruleLoader = new RulesDefinitionXmlLoader();
    ruleLoader.load(repository, FbContribRulesDefinition.class.getResourceAsStream("/org/sonar/plugins/findbugs/rules-fbcontrib.xml"), "UTF-8");
    repository.done();
  }

}
