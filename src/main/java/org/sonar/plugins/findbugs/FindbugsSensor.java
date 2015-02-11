/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.resources.Java;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.util.Collection;

public class FindbugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private FindbugsExecutor executor;
  private final JavaResourceLocator javaResourceLocator;
  private final FileSystem fs;

  public FindbugsSensor(RulesProfile profile, RuleFinder ruleFinder, FindbugsExecutor executor, JavaResourceLocator javaResourceLocator, FileSystem fs) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.executor = executor;
    this.javaResourceLocator = javaResourceLocator;
    this.fs = fs;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().hasLanguage(Java.KEY))
        && (hasActiveFindbugsRules() || hasActiveFbContribRules() || hasActiveFindSecBugsRules());
  }

  private boolean hasActiveFindbugsRules() {
    return !profile.getActiveRulesByRepository(FindbugsRuleRepository.REPOSITORY_KEY).isEmpty();
  }

  private boolean hasActiveFbContribRules() {
    return !profile.getActiveRulesByRepository(FbContribRuleRepository.REPOSITORY_KEY).isEmpty();
  }

  private boolean hasActiveFindSecBugsRules() {
    return !profile.getActiveRulesByRepository(FindSecurityBugsRuleRepository.REPOSITORY_KEY).isEmpty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    if (javaResourceLocator.classFilesToAnalyze().isEmpty()) {
      LOG.warn("Findbugs needs sources to be compiled."
          + " Please build project before executing sonar or check the location of compiled classes to"
          + " make it possible for Findbugs to analyse your project.");
      return;
    }
    Collection<ReportedBug> collection = executor.execute(hasActiveFbContribRules(), hasActiveFindSecBugsRules());

    for (ReportedBug bugInstance : collection) {
      Rule rule = ruleFinder.findByKey(FindbugsRuleRepository.REPOSITORY_KEY, bugInstance.getType());
      if (rule == null) {
        rule = ruleFinder.findByKey(FbContribRuleRepository.REPOSITORY_KEY, bugInstance.getType());
        if (rule == null) {
          rule = ruleFinder.findByKey(FindSecurityBugsRuleRepository.REPOSITORY_KEY, bugInstance.getType());

          if (rule == null) {
            // ignore violations from report, if rule not activated in Sonar
            LOG.warn("Findbugs rule '{}' is not active in Sonar.", bugInstance.getType());
            continue;
          }
        }
      }

      String longMessage = bugInstance.getMessage();
      String className = bugInstance.getClassName();
      int start = bugInstance.getStartLine();

      Resource resource = javaResourceLocator.findResourceByClassName(className);
      if (context.getResource(resource) != null) {
        Violation violation = Violation.create(rule, resource)
            .setMessage(longMessage);
        if (start > 0) {
          violation.setLineId(start);
        }
        context.saveViolation(violation);
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
