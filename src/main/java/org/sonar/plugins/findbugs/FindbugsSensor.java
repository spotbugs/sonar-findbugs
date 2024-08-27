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
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.plugins.findbugs.classpath.ClasspathLocator;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.language.scala.Scala;
import org.sonar.plugins.findbugs.resource.ByteCodeResourceLocator;
import org.sonar.plugins.findbugs.resource.ClassMetadataLoadingException;
import org.sonar.plugins.findbugs.resource.SmapParser;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsScalaRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;

import edu.umd.cs.findbugs.AnalysisError;

public class FindbugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  public static final String[] REPOS = {FindbugsRulesDefinition.REPOSITORY_KEY, FbContribRulesDefinition.REPOSITORY_KEY,
          FindSecurityBugsRulesDefinition.REPOSITORY_KEY, FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY,
          FindSecurityBugsScalaRulesDefinition.REPOSITORY_KEY
  };

  private List<String> repositories = new ArrayList<String>();

  private ActiveRules activeRules;
  private FindbugsExecutor executor;
  private final ClasspathLocator classpathLocator;
  private final ByteCodeResourceLocator byteCodeResourceLocator;
  private final FileSystem fs;
  private final SensorContext sensorContext;
  protected final File classMappingFile;
  protected PrintWriter classMappingWriter;

  public FindbugsSensor(ActiveRules activeRules, SensorContext sensorContext,
                        FindbugsExecutor executor, ClasspathLocator classpathLocator, FileSystem fs, ByteCodeResourceLocator byteCodeResourceLocator) {
    this.activeRules = activeRules;
    this.sensorContext = sensorContext;
    this.executor = executor;
    this.classpathLocator = classpathLocator;
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

  private boolean hasActiveRules(String repository) {
    return !activeRules.findByRepository(repository).isEmpty();
  }

  public List<String> getRepositories() {
    return repositories;
  }

  private boolean hasActiveFindbugsRules() {
    return hasActiveRules(FindbugsRulesDefinition.REPOSITORY_KEY);
  }

  private boolean hasActiveFbContribRules() {
    return hasActiveRules(FbContribRulesDefinition.REPOSITORY_KEY);
  }

  private boolean hasActiveFindSecBugsRules() {
    boolean hasActiveFindSecBugsRules = hasActiveRules(FindSecurityBugsRulesDefinition.REPOSITORY_KEY);
    boolean hasActiveFindSecBugsJspRules = hasActiveRules(FindSecurityBugsJspRulesDefinition.REPOSITORY_KEY);
    boolean hasActiveFindSecBugsScalaRules = hasActiveRules(FindSecurityBugsScalaRulesDefinition.REPOSITORY_KEY);
    
    SortedSet<String> languages = fs.languages();
    boolean hasActiveFindSecBugsJspRulesAndJspFiles = hasActiveFindSecBugsJspRules && languages.contains(Jsp.KEY);
    boolean hasActiveFindSecBugsScalaRulesAndScalaFiles = hasActiveFindSecBugsScalaRules && languages.contains(Scala.KEY);
    
    return hasActiveFindSecBugsRules || hasActiveFindSecBugsJspRulesAndJspFiles || hasActiveFindSecBugsScalaRulesAndScalaFiles;
  }

  @Override
  public void execute(SensorContext context) {
    LOG.info("Findbugs plugin version: {}", FindbugsVersion.getVersion());
    if (!hasActiveFindbugsRules() && !hasActiveFbContribRules() && !hasActiveFindSecBugsRules()) {
      return;
    }

    AnalysisResult analysisResult = executor.execute(context.activeRules());

    try {

      for (ReportedBug bugInstance : analysisResult.getReportedBugs()) {

        try {
          ActiveRule rule = null;
          for (String repoKey : getRepositories()) {
            rule = activeRules.findByInternalKey(repoKey, bugInstance.getType());
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
          
          // Example values for an inner class
          // className:                   multimodule.core.InnerClassSample$InnerClass
          // bugInstance.getClassFile():  multimodule.core.InnerClassSample
          // sourceFile:                  multimodule/core/InnerClassSample.java

          // Example values for a Kotlin extension class (classFile and sourceFile are missing Kt at the end
          // className:                   org.jitsi.rtp.extensions.bytearray.ByteArrayExtensionsKt
          // bugInstance.getClassFile():  org.jitsi.rtp.extensions.bytearray.ByteArrayExtensions
          // sourceFile:                  org/jitsi/rtp/extensions/bytearray/ByteArrayExtensions.kt
          
          //Locate the original class file
          File classFile = findOriginalClassForBug(bugInstance);

  //        //If the class was an outer class, the source file will not be analog to the class name.
  //        //The original source file is available in the class file metadata.
  //        resource = byteCodeResourceLocator.findJavaOuterClassFile(className, classFile, this.fs);
  //        if (resource != null) {
  //          insertIssue(rule, resource, line, longMessage);
  //          continue;
  //        }

          //More advanced mapping if the original source is not Java files
          // Even though we might be able to find the source file right away there might be an SMAP we need to look for:
          // For Kotlin classes part of the .class file might be from other sources files
          if (classFile != null) {
            //Attempt to load SMAP debug metadata
            try {
              SmapParser.SmapLocation location = byteCodeResourceLocator.extractSmapLocation(className, line, classFile);
              if (location != null) {
                if (!location.isPrimaryFile) { //Avoid reporting issue in double when a source file was include inline
                  continue;
                }

                //SMAP was found
                InputFile resource = byteCodeResourceLocator.findSourceFile(location.fileInfo.path, fs);
                if (resource != null) {
                  insertIssue(rule, resource, location.line, longMessage, bugInstance);
                  continue;
                }
              } else {
                //SMAP was not found or unparsable.. The orgininal source file will be guess based on the class name
                InputFile resource = byteCodeResourceLocator.findTemplateFile(className, this.fs);
                if (resource != null) {
                  insertIssue(rule, resource, line, longMessage, bugInstance);
                  continue;
                }
              }
            } catch (ClassMetadataLoadingException e) {
              LOG.warn("Failed to load the class file metadata", e);
            }
          }
          
          // In case there was no .class file or we could not use the SMAP
          //Regular Java class mapped to their original .java
          InputFile resource = byteCodeResourceLocator.findSourceFile(sourceFile, this.fs);
          if (resource != null) {
            insertIssue(rule, resource, line, longMessage, bugInstance);
            continue;
          }

          // We have found an issue in a class file but the corresponding source file was not found, this might be because:
          // - it's a Kotlin extension from another project/module
          // - the source file was excluded and is not visible in the FileSystem interface
          // - we're analyzing all the class files on the classpath and some are from another Gradle module, but the source file is not in the FileSystem
          LOG.debug("The class '{}' could not be matched to its original source file. It might be a dynamically generated class. Class file: {}", className, classFile);
        } catch (Exception e) {
          String bugInstanceDebug = String.format("[BugInstance type=%s, class=%s, line=%s]", bugInstance.getType(), bugInstance.getClassName(), bugInstance.getStartLine());
          LOG.warn("An error occurs while processing the bug instance " + bugInstanceDebug, e);
          //Continue to the bug without aborting the report
        }
      }

      for (AnalysisError analysisError : analysisResult.getAnalysisErrors()) {
        insertAnalysisError(context, analysisError);
      }
    }
    finally {
      if(classMappingWriter != null) {
        classMappingWriter.flush();
        classMappingWriter.close();
      }
    }
  }

  public void insertAnalysisError(SensorContext context, AnalysisError analysisError) {
    NewAnalysisError error = context.newAnalysisError();

    StringBuilder message = buildAnalysisErrorMessage(analysisError);
    error.message(message.toString());

    error.save();
  }

  public StringBuilder buildAnalysisErrorMessage(AnalysisError analysisError) {
    StringBuilder message = new StringBuilder(analysisError.getMessage());
    message.append("Findbugs plugin version: " + FindbugsVersion.getVersion());
    
    if (analysisError.getStackTrace() != null) {
      for (String trace : analysisError.getStackTrace()) {
        message.append('\n');
        message.append(trace);
      }
    }
    if (analysisError.getNestedStackTrace() != null) {
      for (String trace : analysisError.getNestedStackTrace()) {
        message.append('\n');
        message.append(trace);
      }
    }
    return message;
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
   * @param bugInstance The bug instance detected by SpotBugs
   * @return File handle of the original class file analyzed
   */
  private File findOriginalClassForBug(ReportedBug bugInstance) {
    String sourceFile = byteCodeResourceLocator.findClassFileByClassName(bugInstance.getClassName(), classpathLocator);
    if (sourceFile == null) {
      sourceFile = byteCodeResourceLocator.findClassFileByClassName(bugInstance.getClassFile(), classpathLocator);
    }
    
    if (sourceFile == null) {
      return null;
    }

    return new File(sourceFile);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.createIssuesForRuleRepositories(REPOS);
    descriptor.onlyOnLanguages(FindbugsPlugin.SUPPORTED_JVM_LANGUAGES);
    descriptor.name("FindBugs Sensor");
  }
}
