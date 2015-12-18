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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.resource.ByteCodeResourceLocator;
import org.sonar.plugins.java.Java;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.util.Collection;

public class FindbugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private FindbugsExecutor executor;
  private final JavaResourceLocator javaResourceLocator;
  private final ByteCodeResourceLocator byteCodeResourceLocator;
  private final FileSystem fs;
  private final ResourcePerspectives perspectives;

  public FindbugsSensor(RulesProfile profile, RuleFinder ruleFinder, ResourcePerspectives perspectives,
    FindbugsExecutor executor, JavaResourceLocator javaResourceLocator, FileSystem fs, ByteCodeResourceLocator byteCodeResourceLocator) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.perspectives = perspectives;
    this.executor = executor;
    this.javaResourceLocator = javaResourceLocator;
    this.byteCodeResourceLocator = byteCodeResourceLocator;
    this.fs = fs;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return (fs.hasFiles(fs.predicates().hasLanguage(Java.KEY)) || fs.hasFiles(fs.predicates().hasLanguage(Jsp.KEY)))
      && (hasActiveFindbugsRules() || hasActiveFbContribRules() || hasActiveFindSecBugsRules());
  }

  private boolean hasActiveFindbugsRules() {
    return !profile.getActiveRulesByRepository(FindbugsRulesDefinition.REPOSITORY_KEY).isEmpty();
  }

  private boolean hasActiveFbContribRules() {
    return !profile.getActiveRulesByRepository(FbContribRulesDefinition.REPOSITORY_KEY).isEmpty();
  }

  private boolean hasActiveFindSecBugsRules() {
    return !profile.getActiveRulesByRepository(FindSecurityBugsRulesDefinition.REPOSITORY_KEY).isEmpty();
  }

  private boolean hasActiveFindSecBugsJspRules() {
    return !profile.getActiveRulesByRepository(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY).isEmpty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {

    Collection<ReportedBug> collection = executor.execute(hasActiveFbContribRules(), hasActiveFindSecBugsRules());

    for (ReportedBug bugInstance : collection) {
      Rule rule = ruleFinder.findByKey(FindbugsRulesDefinition.REPOSITORY_KEY, bugInstance.getType());
      if (rule == null) {
        rule = ruleFinder.findByKey(FbContribRulesDefinition.REPOSITORY_KEY, bugInstance.getType());
        if (rule == null) {
          rule = ruleFinder.findByKey(FindSecurityBugsRulesDefinition.REPOSITORY_KEY, bugInstance.getType());
          if (rule == null) {
            rule = ruleFinder.findByKey(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY, bugInstance.getType());
            if (rule == null) {
              // ignore violations from report, if rule not activated in Sonar
              LOG.warn("Findbugs rule '{}' is not active in Sonar.", bugInstance.getType());
              continue;
            }
          }
        }
      }

      String className = bugInstance.getClassName();
      String longMessage = bugInstance.getMessage();
      int line = bugInstance.getStartLine();

      //Legacy
      Resource resource = javaResourceLocator.findResourceByClassName(className);
      if (resource != null) {
        insertIssue(rule, resource, line, longMessage);
        continue;
      }

      //Regular Java class mapped to their original .java
      resource = byteCodeResourceLocator.findJavaClassFile(className, project);
      if (resource != null) {
        insertIssue(rule, resource, line, longMessage);
        continue;
      }

      //Precompiled JSP mapped to their original .jsp with the correct line of code if SMAP file is present.
      resource = byteCodeResourceLocator.findTemplateFile(className, project);
      if (resource != null) {
        if(resource.getPath().endsWith(".jsp")) {
          File classFile = findOriginalClassForBug(bugInstance.getClassName());
          Integer jspLine = byteCodeResourceLocator.findJspLine(className, line, javaResourceLocator, classFile);
          line = jspLine == null ?  1 : jspLine;
        }
        else {
          line = 1;
        }
        insertIssue(rule, resource, line, longMessage);
        continue;
      }

      LOG.warn("The class '"+className+"' could not be match to its original source file. It might be a dynamically generated class.");
    }
  }


  private void insertIssue(Rule rule, Resource resource, int line, String message) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null) {
      IssueBuilder builder = issuable.newIssueBuilder().ruleKey(rule.ruleKey()).message(message);
      if (line > 0) {
        builder.line(line);
      }
      issuable.addIssue(builder.build());
    }
  }

  /**
   *
   * @param className Class name
   * @return File handle of the original class file analyzed
   */
  private File findOriginalClassForBug(String className) {
    String classFile = className.replaceAll("\\.","/").concat(".class");

    for(File classPath : javaResourceLocator.classpath()) {
      if(!classPath.isDirectory()) {
        continue;
      }

      File testClassFile = new File(classPath, classFile);
      if(testClassFile.exists()) {
        return testClassFile;
      }
    }

    return null;
  }
}
