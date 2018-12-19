package org.sonar.plugins.findbugs.rules;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.plugins.findbugs.language.scala.Scala;

public class FindSecurityBugsScalaRulesDefinition implements RulesDefinition {
    public static final String REPOSITORY_KEY = "findsecbugs-scala";
    public static final String REPOSITORY_SCALA_NAME = "Find Security Bugs (Scala)";

    @Override
    public void define(Context context) {
        NewRepository repositoryJsp = context
                .createRepository(REPOSITORY_KEY, Scala.KEY)
                .setName(REPOSITORY_SCALA_NAME);

        RulesDefinitionXmlLoader ruleLoaderJsp = new RulesDefinitionXmlLoader();
        ruleLoaderJsp.load(repositoryJsp, FindSecurityBugsRulesDefinition.class.getResourceAsStream("/org/sonar/plugins/findbugs/rules-scala.xml"), "UTF-8");
        repositoryJsp.done();
    }
}
