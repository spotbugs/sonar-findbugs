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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.plugins.findbugs.resource.ByteCodeResourceLocator;
import org.sonar.plugins.findbugs.resource.ClassMetadataLoadingException;
import org.sonar.plugins.findbugs.resource.SmapParser;
import org.sonar.plugins.findbugs.rules.*;
import org.sonar.plugins.java.api.JavaResourceLocator;

public class FindbugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  public static final String[] REPOS = {FindbugsRulesDefinition.REPOSITORY_KEY, FbContribRulesDefinition.REPOSITORY_KEY,
          FindSecurityBugsRulesDefinition.REPOSITORY_KEY, FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY
  };

  private List<String> repositories = new ArrayList<String>();

  private RulesProfile profile;
  private ActiveRules ruleFinder;
  private FindbugsExecutor executor;
  private final JavaResourceLocator javaResourceLocator;
  private final ByteCodeResourceLocator byteCodeResourceLocator;
  private final FileSystem fs;
  private final SensorContext sensorContext;
  protected final File classMappingFile;
  protected PrintWriter classMappingWriter;

  public FindbugsSensor(RulesProfile profile, ActiveRules ruleFinder, SensorContext sensorContext,
                        FindbugsExecutor executor, JavaResourceLocator javaResourceLocator, FileSystem fs, ByteCodeResourceLocator byteCodeResourceLocator) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.sensorContext = sensorContext;
    this.executor = executor;
    this.javaResourceLocator = javaResourceLocator;
    this.byteCodeResourceLocator = byteCodeResourceLocator;
    this.fs = fs;
    registerRepositories(REPOS);
    this.classMappingFile = new File(fs.workDir(), "class-mapping.csv");
    try {
      this.classMappingWriter = new PrintWriter(new FileOutputStream(classMappingFile));
    } catch (FileNotFoundException e) {
    }
  }

  public void registerRepositories(String... repos) {
    Collections.addAll(repositories, repos);
  }

  private boolean hasActiveRules(String repoSubstring) {
    return profile.getActiveRules().stream().anyMatch(activeRule ->
      activeRule.getRepositoryKey().contains(repoSubstring)
    );
  }

  public List<String> getRepositories() {
    return repositories;
  }

  private boolean hasActiveFindbugsRules() {
    return hasActiveRules("findbugs");
  }

  private boolean hasActiveFbContribRules() {
    return hasActiveRules("fb-contrib");
  }

  private boolean hasActiveFindSecBugsRules() {
    return hasActiveRules("findsecbugs");
  }

  @Override
  public void execute(SensorContext context) {

    if (!hasActiveFindbugsRules() && !hasActiveFbContribRules() && !hasActiveFindSecBugsRules()) {
      return;
    }

    Collection<ReportedBug> collection = executor.execute(hasActiveFbContribRules(), hasActiveFindSecBugsRules());

    try {

      for (ReportedBug bugInstance : collection) {

        try {
          ActiveRule rule = null;
          for (String repoKey : getRepositories()) {
            rule = ruleFinder.findByInternalKey(repoKey, bugInstance.getType());
            if (rule != null) {
              break;
            }
          }
          if (rule == null) {
            // ignore violations from report, if rule not activated in Sonar
            LOG.warn("Findbugs rule '{}' is not active in Sonar.", bugInstance.getType());
            continue;
          }

          String className = bugInstance.getClassName();
          String sourceFile = bugInstance.getSourceFile();
          String longMessage = bugInstance.getMessage();
          int line = bugInstance.getStartLine();


          //Regular Java class mapped to their original .java
          InputFile resource = byteCodeResourceLocator.findSourceFile(sourceFile, this.fs);
          if (resource != null) {
            insertIssue(rule, resource, line, longMessage, bugInstance);
            continue;
          }

          //Locate the original class file
          File classFile = findOriginalClassForBug(bugInstance.getClassFile());
          if (classFile == null) {
            LOG.warn("Unable to find the class " + bugInstance.getClassName());
            continue;
          }

  //        //If the class was an outer class, the source file will not be analog to the class name.
  //        //The original source file is available in the class file metadata.
  //        resource = byteCodeResourceLocator.findJavaOuterClassFile(className, classFile, this.fs);
  //        if (resource != null) {
  //          insertIssue(rule, resource, line, longMessage);
  //          continue;
  //        }

          //More advanced mapping if the original source is not Java files
          if (classFile != null) {
            //Attempt to load SMAP debug metadata
            try {
              SmapParser.SmapLocation location = byteCodeResourceLocator.extractSmapLocation(className, line, classFile);
              if (location != null) {
                if (!location.isPrimaryFile) { //Avoid reporting issue in double when a source file was include inline
                  continue;
                }

                //SMAP was found
                resource = byteCodeResourceLocator.findSourceFile(location.fileInfo.path, fs);
                if (resource != null) {
                  insertIssue(rule, resource, location.line, longMessage, bugInstance);
                  continue;
                }
              } else {
                //SMAP was not found or unparsable.. The orgininal source file will be guess based on the class name
                resource = byteCodeResourceLocator.findTemplateFile(className, this.fs);
                if (resource != null) {
                  insertIssue(rule, resource, line, longMessage, bugInstance);
                  continue;
                }
              }
            } catch (ClassMetadataLoadingException e) {
              LOG.warn("Failed to load the class file metadata", e);
            }
          }

          LOG.warn("The class '" + className + "' could not be matched to its original source file. It might be a dynamically generated class.");
        } catch (Exception e) {
          String bugInstanceDebug = String.format("[BugInstance type=%s, class=%s, line=%s]", bugInstance.getType(), bugInstance.getClassName(), bugInstance.getStartLine());
          LOG.warn("An error occurs while processing the bug instance " + bugInstanceDebug, e);
          //Continue to the bug without aborting the report
        }
      }

    }
      finally {
      classMappingWriter.flush();
      classMappingWriter.close();
    }
  }


  protected void insertIssue(ActiveRule rule, InputFile resource, int line, String message, ReportedBug bugInstance) {
    NewIssue newIssue = sensorContext.newIssue().forRule(rule.ruleKey());

    NewIssueLocation location = newIssue.newLocation()
            .on(resource)
            .at(resource.selectLine(line > 0 ? line : 1))
            .message(message);

    newIssue.at(location); //Primary location
    newIssue.save();

    writeDebugMappingToFile(bugInstance.getClassName(),bugInstance.getStartLine(), resource.relativePath(), line);
  }

  protected void writeDebugMappingToFile(String classFile, int classFileLine, String sourceFile, int sourceFileLine) {
    if(classMappingWriter != null) {
      classMappingWriter.println(classFile + ":" + classFileLine + "," + sourceFile + ":" + sourceFileLine);
    }
  }

  /**
   *
   * @param className Class name
   * @return File handle of the original class file analyzed
   */
  private File findOriginalClassForBug(String className) {
    String sourceFile = byteCodeResourceLocator.findSourceFileKeyByClassName(className,javaResourceLocator);
    if (sourceFile == null) {
      return null;
    }

    return new File(sourceFile);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguages(FindbugsPlugin.SUPPORTED_JVM_LANGUAGES);
    descriptor.name("FindBugs Sensor");
  }
}
