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
import org.sonar.plugins.findbugs.language.Jsp;

/**
 * This RulesDefinition build a separate repository from the FindSecurityBugsRulesDefinition to allow a separate ruleset
 * for JSP language.
 * @see FindSecurityBugsRulesDefinition
 */
public class FindSecurityBugsJspRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY_KEY = "findsecbugs-jsp";
    public static final String REPOSITORY_JSP_NAME = "Find Security Bugs (JSP)";

    @Override
    public void define(Context context) {
        NewRepository repositoryJsp = context
                .createRepository(REPOSITORY_KEY, Jsp.KEY)
                .setName(REPOSITORY_JSP_NAME);

        RulesDefinitionXmlLoader ruleLoaderJsp = new RulesDefinitionXmlLoader();
        ruleLoaderJsp.load(repositoryJsp, FindSecurityBugsRulesDefinition.class.getResourceAsStream("/org/sonar/plugins/findbugs/rules-jsp.xml"), "UTF-8");
        repositoryJsp.done();
    }
}
