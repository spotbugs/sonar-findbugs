/*
 * SonarQube SpotBugs Plugin
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

import static org.sonar.plugins.findbugs.rules.FindbugsRules.CRITICAL_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.CRITICAL_JSP_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.CRITICAL_SCALA_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.CRYPTO_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.EXCLUDED_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.INFORMATIONAL_PATTERNS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.MAJOR_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.MAJOR_JSP_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.SECURITY_JSP_RULES;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.SECURITY_SCALA_RULES;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.union;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.findbugs.FindbugsExecutor;
import org.sonar.plugins.findbugs.FindbugsPluginException;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.language.scala.Scala;
import org.sonar.plugins.java.Java;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginLoader;

/**
 * Builds the rules definitions based on the plugins bug patterns
 * 
 * @author gtoison
 */
public final class FindbugsRulesPluginsDefinition implements RulesDefinition {
  private static final Logger LOG = LoggerFactory.getLogger(FindbugsRulesPluginsDefinition.class);

  @Override
  public void define(Context context) {
  	try {
  		NewRepository repository = context
  				.createRepository(FindbugsRulesDefinition.REPOSITORY_KEY, Java.KEY)
  				.setName(FindbugsRulesDefinition.REPOSITORY_NAME);
  		NewRepository fbContribRepository = context
  				.createRepository(FbContribRulesDefinition.REPOSITORY_KEY, Java.KEY)
  				.setName(FbContribRulesDefinition.REPOSITORY_NAME);
  		NewRepository findsecbugsRepository = context
  				.createRepository(FindSecurityBugsRulesDefinition.REPOSITORY_KEY, Java.KEY)
  				.setName(FindSecurityBugsRulesDefinition.REPOSITORY_NAME);
  		NewRepository findsecbugsJspRepository = context
  				.createRepository(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY, Jsp.KEY)
  				.setName(FindSecurityBugsJspRulesDefinition.REPOSITORY_JSP_NAME);
  		NewRepository findsecbugsScalaRepository = context
  				.createRepository(FindSecurityBugsScalaRulesDefinition.REPOSITORY_KEY, Scala.KEY)
  				.setName(FindSecurityBugsScalaRulesDefinition.REPOSITORY_SCALA_NAME);

  		// Rules marked as deprecated (and their equivalent) because there are redundant with native SonarQube rules
  		Map<String, String> deprecatedFbContribRules = new HashMap<>();
  		deprecatedFbContribRules.put("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN", "java:S1143");
  		deprecatedFbContribRules.put("AIOB_ARRAY_STORE_TO_NULL_REFERENCE", "java:S2259");
  		deprecatedFbContribRules.put("FII_USE_METHOD_REFERENCE", "java:S1612");
  		deprecatedFbContribRules.put("SPP_TOSTRING_ON_STRING", "java:S1858");
  		deprecatedFbContribRules.put("SPP_USE_BIGDECIMAL_STRING_CTOR", "java:S2111");
  		deprecatedFbContribRules.put("SPP_USELESS_TERNARY", "java:S1125");
  		deprecatedFbContribRules.put("USBR_UNNECESSARY_STORE_BEFORE_RETURN", "java:S1488");

  		Map<String, Plugin> plugins = FindbugsExecutor.loadFindbugsPlugins();
  		Plugin corePlugin = PluginLoader.getCorePluginLoader().getPlugin();
  		Plugin fbContribPlugin = plugins.get(FindbugsRules.PLUGIN_ID_FINDBUGS_CONTRIB);
  		Plugin findsecbugsPlugin = plugins.get(FindbugsRules.PLUGIN_ID_FINDSECBUGS);

  		initializeRulesRepository(repository, corePlugin, Collections.emptyList(), union(SECURITY_JSP_RULES, SECURITY_SCALA_RULES), Collections.emptyMap());
  		initializeRulesRepository(fbContribRepository, fbContribPlugin, Collections.emptyList(), Collections.emptyList(), deprecatedFbContribRules);
  		initializeRulesRepository(findsecbugsRepository, findsecbugsPlugin, Collections.emptyList(), union(SECURITY_JSP_RULES, SECURITY_SCALA_RULES, EXCLUDED_BUGS), Collections.emptyMap());
  		initializeRulesRepository(findsecbugsJspRepository, corePlugin, SECURITY_JSP_RULES, Collections.emptyList(), Collections.emptyMap());
  		initializeRulesRepository(findsecbugsJspRepository, findsecbugsPlugin, SECURITY_JSP_RULES, Collections.emptyList(), Collections.emptyMap());
  		initializeRulesRepository(findsecbugsScalaRepository, findsecbugsPlugin, SECURITY_SCALA_RULES, Collections.emptyList(), Collections.emptyMap());

  		FindSecurityBugsJspRulesDefinition.addDeprecatedRuleKeys(findsecbugsJspRepository);

  		repository.done();
  		fbContribRepository.done();
  		findsecbugsRepository.done();
  		findsecbugsJspRepository.done();
  		findsecbugsScalaRepository.done();
    } catch (Exception e) {
    	throw new FindbugsPluginException("Error building rules", e);
    }
  }
	
  /**
   * @param repository The {@link NewRepository} where we will create the rules
   * @param plugin The {@link Plugin} we want to load the {@link BugPattern} from
   * @param includedBugs The ids of the {@link BugPattern} we want to include or an empty collection to include all of them
   * @param excludedBugs The ids of the excluded {@link BugPattern}
   * @param deprecatedRules The ids of the deprecated {@link BugPattern} and their replacement
   */
  public void initializeRulesRepository(NewRepository repository, Plugin plugin, Collection<String> includedBugs,
          Collection<String> excludedBugs, Map<String, String> deprecatedRules) {
  	for (BugPattern bugPattern : plugin.getBugPatterns()) {
  		String type = bugPattern.getType();

  		String category = bugPattern.getCategory();

  		if(category.equals("NOISE") || Arrays.asList("TESTING", "TESTING1", "TESTING2", "TESTING3", "UNKNOWN").contains(type)) {
  			continue;
  		}

  		if(category.equals("MT_CORRECTNESS")) {
  			category = "MULTI-THREADING";
  		}

  		String htmlDescription = bugPattern.getDetailText();
  		String severity = getSonarSeverity(type, category, htmlDescription);
  		String name = capitalize(category.toLowerCase()).replace("_"," ") + " - " + bugPattern.getShortDescription();
  		boolean deprecated = bugPattern.isDeprecated();
  		String deprecationReplacement = deprecatedRules.get(type);

  		RuleStatus ruleStatus = RuleStatus.READY;

  		if (deprecationReplacement != null) {
  			htmlDescription = htmlDescription.trim() + "\n<h2>Deprecated</h2>\n<p>This rule is deprecated; use {rule:" + deprecationReplacement + "} instead.</p>";
  			ruleStatus = RuleStatus.DEPRECATED;
  		} else if (deprecated) {
  			htmlDescription = htmlDescription.trim() + "\n<h2>Deprecated</h2>\n<p>This rule is deprecated</p>";
  			ruleStatus = RuleStatus.DEPRECATED;
  		}

  		List<String> tags = new ArrayList<>();

  		//OWASP TOP 10 2013
  		if (htmlDescription.toLowerCase().contains("injection") || htmlDescription.contains("A1-Injection")) {
  			tags.add("owasp-a1");
  			tags.add("injection");
  		}
  		if (htmlDescription.contains("A2-Broken_Authentication_and_Session_Management")) {
  			tags.add("owasp-a2");
  		}
  		if (type.contains("XSS") || htmlDescription.contains("A3-Cross-Site_Scripting")) {
  			tags.add("owasp-a3");
  		}
  		if (htmlDescription.contains("A4-Insecure_Direct_Object_References") || htmlDescription.contains("Path_Traversal")) {
  			tags.add("owasp-a4");
  		}
  		if (htmlDescription.contains("A5-Security_Misconfiguration")) {
  			tags.add("owasp-a5");
  		}
  		if (type.equals("HARD_CODE_PASSWORD") ||
  				CRYPTO_BUGS.contains(type) ||
  				htmlDescription.contains("A6-Sensitive_Data_Exposure")) {
  			tags.add("owasp-a6");
  			tags.add("cryptography");
  		}
  		if (htmlDescription.contains("A7-Missing_Function_Level_Access_Control")) {
  			tags.add("owasp-a7");
  		}
  		if (htmlDescription.toLowerCase().contains("A8-Cross-Site_Request_Forgery")) {
  			tags.add("owasp-a8");
  		}
  		if (htmlDescription.toLowerCase().contains("A9-Using_Components_with_Known_Vulnerabilities")) {
  			tags.add("owasp-a9");
  		}
  		if (htmlDescription.toLowerCase().contains("A10-Unvalidated_Redirects_and_Forwards")) {
  			tags.add("owasp-a10");
  		}

  		//Misc tags

  		if (htmlDescription.toLowerCase().contains("wasc")) {
  			tags.add("wasc");
  		}
  		if (htmlDescription.toLowerCase().contains("cwe")) {
  			tags.add("cwe");
  		}
  		if (bugPattern.getShortDescription().toLowerCase().contains("android")) {
  			tags.add("android");
  		}
  		if (type.contains("JSP")) {
  			tags.add("jsp");
  		}

  		//Category related
  		tags.add(category.toLowerCase().replace("_","-"));

  		if(Arrays.asList("PERFORMANCE","CORRECTNESS","MULTI-THREADING").contains(category)) {
  			tags.add("bug");
  		}

  		if((includedBugs.isEmpty() || includedBugs.contains(type)) && !excludedBugs.contains(type)) {
  			repository
  			.createRule(type)
  			.setInternalKey(type)
  			.setSeverity(severity)
  			.setName(name)
  			.setHtmlDescription(htmlDescription)
  			.setStatus(ruleStatus)
  			.setTags(tags.toArray(String[]::new));
  		}
  	}
  }
  
  public static String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }
  
  public String getSonarSeverity(String type, String category, String description) {
    String priority = getFsbSeverityFromType(type,category);
    if(priority != null) {
    	return priority;
    }

    //Findbugs critical base on the type or message
    if(type.contains("IMPOSSIBLE")) {
        return Severity.CRITICAL;
    }
    
    Pattern willResultInExceptionAtRuntimePattern = Pattern.compile("[\\S\\s]*will result in [\\w]+Exception at runtime[\\S\\s]*");
    Pattern willAlwaysThrowExceptionPattern = Pattern.compile("[\\S\\s]*will always throw a [\\w]+Exception[\\S\\s]*");
    
    if(willResultInExceptionAtRuntimePattern.matcher(description).matches() || willAlwaysThrowExceptionPattern.matcher(description).matches()) {
        return Severity.CRITICAL;
    }

    //Findbugs general
    if(Arrays.asList("CORRECTNESS", "PERFORMANCE", "SECURITY","MULTI-THREADING","BAD_PRACTICE").contains(category)) {
      return Severity.MAJOR;
    }
    if(Arrays.asList("STYLE", "MALICIOUS_CODE", "I18N","EXPERIMENTAL").contains(category)) {
      return Severity.INFO;
    }

    LOG.warn("Unknown priority for {} ({})", type, category);
    return Severity.INFO;
  }
  
  private String getFsbSeverityFromType(String type, String category) {
  	if (CRITICAL_BUGS.contains(type) || CRITICAL_JSP_BUGS.contains(type) || CRITICAL_SCALA_BUGS.contains(type)) {
  	  return Severity.CRITICAL;
  	}
  	
    if (MAJOR_BUGS.contains(type) || CRYPTO_BUGS.contains(type) || MAJOR_JSP_BUGS.contains(type)) {
      return Severity.MAJOR;
    }
    
    if (INFORMATIONAL_PATTERNS.contains(type)) {
      return Severity.INFO;
    }

    if(category.equals("SECURITY")) {
      return Severity.MAJOR;
    }

    return null;
	}
}
