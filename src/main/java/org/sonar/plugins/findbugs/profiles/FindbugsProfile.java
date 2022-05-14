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
package org.sonar.plugins.findbugs.profiles;

import static org.sonar.plugins.findbugs.rules.FindbugsRules.EXCLUDED_BUGS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.FINDBUGS_JSP_RULES;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.FINDBUGS_PATTERNS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.INFORMATIONAL_PATTERNS;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.SECURITY_JSP_RULES;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.SECURITY_SCALA_RULES;
import static org.sonar.plugins.findbugs.rules.FindbugsRules.union;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.plugins.findbugs.FindbugsExecutor;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.language.scala.Scala;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsScalaRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRules;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.java.Java;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginLoader;

public class FindbugsProfile implements BuiltInQualityProfilesDefinition {

  public static final String FINDBUGS_PROFILE_NAME = "FindBugs";
  public static final String FB_CONTRIB_PROFILE_NAME = "FindBugs + FB-Contrib";

  /**
   * Security rules including informational rules. This profile is intend for in
   * depth security code review.
   */
  public static final String FINDBUGS_SECURITY_AUDIT_PROFILE_NAME = "FindBugs Security Audit";

  /**
   * Security rules with only the issue that require immediate analysis. It is
   * intend for periodic scan that will trigger a moderate number of false
   * positive.
   */
  public static final String FINDBUGS_SECURITY_MINIMAL_PROFILE_NAME = "FindBugs Security Minimal";
  public static final String FINDBUGS_SECURITY_JSP_PROFILE_NAME = "FindBugs Security JSP";
  public static final String FINDBUGS_SECURITY_SCALA_PROFILE_NAME = "FindBugs Security Scala";

  private RuleFinder ruleFinder;

  public FindbugsProfile(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  @Override
  public void define(Context context) {
    try {
      NewBuiltInQualityProfile findbugsProfile = context.createBuiltInQualityProfile(FINDBUGS_PROFILE_NAME, Java.KEY);
      NewBuiltInQualityProfile findbugsContribProfile = context.createBuiltInQualityProfile(FB_CONTRIB_PROFILE_NAME,
          Java.KEY);
      NewBuiltInQualityProfile findbugsSecurityAuditProfile = context
          .createBuiltInQualityProfile(FINDBUGS_SECURITY_AUDIT_PROFILE_NAME, Java.KEY);
      NewBuiltInQualityProfile findbugsSecurityMinimalProfile = context
          .createBuiltInQualityProfile(FINDBUGS_SECURITY_MINIMAL_PROFILE_NAME, Java.KEY);
      NewBuiltInQualityProfile findbugsSecurityJspProfile = context
          .createBuiltInQualityProfile(FINDBUGS_SECURITY_JSP_PROFILE_NAME, Jsp.KEY);
      NewBuiltInQualityProfile findbugsSecurityScalaProfile = context
          .createBuiltInQualityProfile(FINDBUGS_SECURITY_SCALA_PROFILE_NAME, Scala.KEY);

      Map<String, Plugin> plugins = FindbugsExecutor.loadFindbugsPlugins();
      Plugin corePlugin = PluginLoader.getCorePluginLoader().getPlugin();
      Plugin fbContribPlugin = plugins.get(FindbugsRules.PLUGIN_ID_FINDBUGS_CONTRIB);
      Plugin findsecbugsPlugin = plugins.get(FindbugsRules.PLUGIN_ID_FINDSECBUGS);

      // FindBugs profile
      activateRules(findbugsProfile, FindbugsRulesDefinition.REPOSITORY_KEY, getAllPatternsFromPlugin(corePlugin),
          Collections.emptyList(), union(SECURITY_JSP_RULES, SECURITY_SCALA_RULES));

      // FindBugs + FB Contrib profile
      activateRules(findbugsContribProfile, FindbugsRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(corePlugin), Collections.emptyList(),
          union(SECURITY_JSP_RULES, SECURITY_SCALA_RULES));
      activateRules(findbugsContribProfile, FbContribRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(fbContribPlugin), Collections.emptyList(),
          union(SECURITY_JSP_RULES, SECURITY_SCALA_RULES));

      // FindSecBugs Audit profile
      activateRules(findbugsSecurityAuditProfile, FindSecurityBugsRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(findsecbugsPlugin), Collections.emptyList(),
          union(SECURITY_JSP_RULES, SECURITY_SCALA_RULES, EXCLUDED_BUGS));
      activateRules(findbugsSecurityAuditProfile, FindbugsRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(corePlugin), FINDBUGS_PATTERNS, Collections.emptyList());

      // FindSecBugs Minimal profile
      activateRules(findbugsSecurityMinimalProfile, FindSecurityBugsRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(findsecbugsPlugin), Collections.emptyList(),
          union(SECURITY_JSP_RULES, SECURITY_SCALA_RULES, EXCLUDED_BUGS, INFORMATIONAL_PATTERNS));
      activateRules(findbugsSecurityMinimalProfile, FindbugsRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(corePlugin), FINDBUGS_PATTERNS, Collections.emptyList());

      // FindSecBugs JPS profile
      activateRules(findbugsSecurityJspProfile, FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(findsecbugsPlugin), SECURITY_JSP_RULES, Collections.emptyList());
      activateRules(findbugsSecurityJspProfile, FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(corePlugin), FINDBUGS_JSP_RULES, Collections.emptyList());

      // FindSecBugs Scala profile
      activateRules(findbugsSecurityScalaProfile, FindSecurityBugsScalaRulesDefinition.REPOSITORY_KEY,
          getAllPatternsFromPlugin(findsecbugsPlugin), SECURITY_SCALA_RULES, Collections.emptyList());

      findbugsProfile.done();
      findbugsContribProfile.done();
      findbugsSecurityAuditProfile.done();
      findbugsSecurityMinimalProfile.done();
      findbugsSecurityJspProfile.done();
      findbugsSecurityScalaProfile.done();
    } catch (Exception e) {
      throw new RuntimeException("Error defining quality profiles", e);
    }
  }

  public void activateRules(NewBuiltInQualityProfile profile, String repoKey, Collection<BugPattern> bugPatterns,
      Collection<String> includedBugs, Collection<String> excludedBugs) {
    for (BugPattern bugPattern : bugPatterns) {
      String type = bugPattern.getType();

      if ((includedBugs.isEmpty() || includedBugs.contains(type)) && !excludedBugs.contains(type)) {
        Rule rule = ruleFinder.findByKey(repoKey, type);

        if (rule != null && !Rule.STATUS_REMOVED.equals(rule.getStatus())) {
          profile.activateRule(repoKey, type);
        }
      }
    }
  }

  private Collection<BugPattern> getAllPatternsFromPlugin(Plugin plugin) {
    Collection<BugPattern> bugPatterns = new HashSet<>();

    for (BugPattern bugPattern : plugin.getBugPatterns()) {
      String category = bugPattern.getCategory();

      if (!"EXPERIMENTAL".equals(category) && !"NOISE".equals(category)) {
        bugPatterns.add(bugPattern);
      }
    }

    return bugPatterns;
  }
}
