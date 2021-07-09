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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;

import java.io.Reader;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.XStream;

@ScannerSide
@ServerSide
@ExtensionPoint
public class FindbugsProfileImporter {

  private final RuleFinder ruleFinder;
  private static final Logger LOGGER = Loggers.get(FindbugsProfileImporter.class);

  public FindbugsProfileImporter(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  public void importProfile(Reader findbugsConf, NewBuiltInQualityProfile qualityProfile) {
    try {
      XStream xStream = FindBugsFilter.createXStream();
      FindBugsFilter filter = (FindBugsFilter) xStream.fromXML(findbugsConf);

      activateRulesByCategory(qualityProfile, filter);
      activateRulesByCode(qualityProfile, filter);
      activateRulesByPattern(qualityProfile, filter);
    } catch (Exception e) {
      String errorMessage = "The Findbugs configuration file is not valid";
      LOGGER.error(errorMessage, e);
    }
  }

  private void activateRulesByPattern(NewBuiltInQualityProfile profile, FindBugsFilter filter) {
    for (Map.Entry<String, String> patternLevel : filter.getPatternLevels(new FindbugsLevelUtils()).entrySet()) {
      Rule rule = ruleFinder.findByKey(FindbugsRulesDefinition.REPOSITORY_KEY, patternLevel.getKey());
      if (rule == null) {
        rule = ruleFinder.findByKey(FbContribRulesDefinition.REPOSITORY_KEY, patternLevel.getKey());
        if (rule == null) {
          rule = ruleFinder.findByKey(FindSecurityBugsRulesDefinition.REPOSITORY_KEY, patternLevel.getKey());
          if (rule == null) {
            rule = ruleFinder.findByKey(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY, patternLevel.getKey());
          }
        }
      }
      if (rule != null) {
        activateRule(profile, rule, patternLevel.getValue());
      } else {
        LOGGER.warn("Unable to activate unknown rule : '" + patternLevel.getKey() + "'");
      }
    }
  }

  private void activateRulesByCode(NewBuiltInQualityProfile profile, FindBugsFilter filter) {
    for (Map.Entry<String, String> codeLevel : filter.getCodeLevels(new FindbugsLevelUtils()).entrySet()) {
      boolean someRulesHaveBeenActivated = false;
      for (Rule rule : rules()) {
        if (rule.getKey().equals(codeLevel.getKey()) || StringUtils.startsWith(rule.getKey(), codeLevel.getKey() + "_")) {
          someRulesHaveBeenActivated = true;
          activateRule(profile, rule, codeLevel.getValue());
        }
      }
      if (!someRulesHaveBeenActivated) {
        LOGGER.warn("Unable to find any rules associated to code  : '" + codeLevel.getKey() + "'");
      }
    }
  }

  private void activateRulesByCategory(NewBuiltInQualityProfile profile, FindBugsFilter filter) {
    for (Map.Entry<String, String> categoryLevel : filter.getCategoryLevels(new FindbugsLevelUtils()).entrySet()) {
      boolean someRulesHaveBeenActivated = false;
      String sonarCateg = FindbugsCategory.findbugsToSonar(categoryLevel.getKey());
      for (Rule rule : rules()) {
        if (sonarCateg != null && rule.getName().startsWith(sonarCateg)) {
          someRulesHaveBeenActivated = true;
          activateRule(profile, rule, categoryLevel.getValue());
        }
      }
      if (!someRulesHaveBeenActivated) {
        LOGGER.warn("Unable to find any rules associated to category  : '" + categoryLevel.getKey() + "'");
      }
    }
  }
  
  private void activateRule(NewBuiltInQualityProfile profile, Rule rule, @Nullable String severity) {
    NewBuiltInActiveRule r = profile.activateRule(rule.getRepositoryKey(), rule.getKey());
    if (severity != null) {
      r.overrideSeverity(severity);
    } else {
      r.overrideSeverity(getSeverityFromPriority(rule.getSeverity()));
    }
  }

  private static String getSeverityFromPriority(RulePriority priority) {
    switch (priority) {
    case INFO:
      return Severity.INFO;
    case MINOR:
      return Severity.MINOR;
    case MAJOR:
    return Severity.MAJOR;
    case CRITICAL:
    return Severity.CRITICAL;
    case BLOCKER:
      return Severity.BLOCKER;
    default:
      return Severity.defaultSeverity();
    }
  }

  private Iterable<Rule> rules() {
    return Iterables.concat(
      ruleFinder.findAll(RuleQuery.create().withRepositoryKey(FindbugsRulesDefinition.REPOSITORY_KEY)),
      ruleFinder.findAll(RuleQuery.create().withRepositoryKey(FbContribRulesDefinition.REPOSITORY_KEY)),
      ruleFinder.findAll(RuleQuery.create().withRepositoryKey(FindSecurityBugsRulesDefinition.REPOSITORY_KEY)),
      ruleFinder.findAll(RuleQuery.create().withRepositoryKey(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY)));
  }

}
