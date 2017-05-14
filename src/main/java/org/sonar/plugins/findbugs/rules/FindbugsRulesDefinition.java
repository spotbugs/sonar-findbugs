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
import org.sonar.squidbridge.rules.ExternalDescriptionLoader;
import org.sonar.squidbridge.rules.SqaleXmlLoader;

public final class FindbugsRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "findbugs";
  public static final String REPOSITORY_NAME = "FindBugs";
  public static final int RULE_COUNT = 452;
  public static final int DEACTIVED_RULE_COUNT = 6;

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, Java.KEY)
      .setName(REPOSITORY_NAME);


    RulesDefinitionXmlLoader ruleLoaderJsp = new RulesDefinitionXmlLoader();
    ruleLoaderJsp.load(repository, FindSecurityBugsRulesDefinition.class.getResourceAsStream("/org/sonar/plugins/findbugs/rules-findbugs.xml"), "UTF-8");
    SqaleXmlLoader.load(repository, "/com/sonar/sqale/findbugs-model.xml");
    repository.done();

//    RulesDefinitionXmlLoader ruleLoader = new RulesDefinitionXmlLoader();
//    ruleLoader.load(repository, FindbugsRulesDefinition.class.getResourceAsStream("/org/sonar/plugins/findbugs/rules.xml"), "UTF-8");
//    ExternalDescriptionLoader.loadHtmlDescriptions(repository, "/org/sonar/l10n/findbugs/rules/findbugs");
//    SqaleXmlLoader.load(repository, "/com/sonar/sqale/findbugs-model.xml");
//    repository.done();
  }

}
