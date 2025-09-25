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
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.api.scanner.ScannerSide;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;

@ScannerSide
public class FindbugsExecutor {

  private static final String FINDBUGS_CORE_PLUGIN_ID = "edu.umd.cs.findbugs.plugins.core";

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsExecutor.class);
  public static final List<String> EXISTING_FINDBUGS_REPORT_PATHS = Arrays.asList("/target/findbugsXml.xml","/target/spotbugsXml.xml");

  private FileSystem fs;
  private Configuration config;

  /**
   * Map of priority level names to their numeric values.
   */
  private static Map<String, Integer> priorityNameToValueMap = new HashMap<String, Integer>();

  static {
    priorityNameToValueMap.put("high", Priorities.HIGH_PRIORITY);
    priorityNameToValueMap.put("medium", Priorities.NORMAL_PRIORITY);
    priorityNameToValueMap.put("low", Priorities.LOW_PRIORITY);
    priorityNameToValueMap.put("experimental", Priorities.EXP_PRIORITY);
  }

  private static final Integer DEFAULT_PRIORITY = Priorities.NORMAL_PRIORITY;

  private final FindbugsConfiguration configuration;

  public FindbugsExecutor(FindbugsConfiguration configuration, FileSystem fs, Configuration config) {
    this.configuration = configuration;
    this.fs = fs;
    this.config = config;
  }

  @VisibleForTesting
  AnalysisResult execute() {
    return execute(true);
  }

  public AnalysisResult execute(boolean useAllPlugin) {
    return execute(useAllPlugin,useAllPlugin);
  }

  public AnalysisResult execute(boolean useFbContrib, boolean useFindSecBugs) {
    ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(FindBugs2.class.getClassLoader());

    // This is a dirty workaround, but unfortunately there is no other way to make Findbugs generate english messages only - see SONARJAVA-380
    Locale initialLocale = Locale.getDefault();
    Locale.setDefault(Locale.ENGLISH);

    OutputStream xmlOutput = null;
    Collection<Plugin> customPlugins = null;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try (FindBugs2 engine = new FindBugs2(); Project project = new Project()) {
      configuration.initializeFindbugsProject(project);

      if(project.getFileCount() == 0) {
        LOG.info("Findbugs analysis skipped for this project.");
        return new AnalysisResult();
      }

      customPlugins = loadFindbugsPlugins(useFbContrib,useFindSecBugs);

      disableUpdateChecksOnEveryPlugin();

      engine.setProject(project);

      XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
      xmlBugReporter.setPriorityThreshold(determinePriorityThreshold());
      xmlBugReporter.setAddMessages(true);

      File xmlReport = configuration.getTargetXMLReport();
      LOG.info("Findbugs output report: " + xmlReport.getAbsolutePath());
      xmlOutput = FileUtils.openOutputStream(xmlReport);
      xmlBugReporter.setOutputStream(new PrintStream(xmlOutput));

      engine.setBugReporter(xmlBugReporter);

      UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
      userPreferences.setEffort(configuration.getEffort());
      engine.setUserPreferences(userPreferences);

      engine.addFilter(configuration.saveIncludeConfigXml().getAbsolutePath(), true);

      for (File filterFile : configuration.getExcludesFilters()) {
        if (filterFile.isFile()) {
          LOG.info("Use filter-file: {}", filterFile);
          engine.addFilter(filterFile.getAbsolutePath(), false);
        } else {
          LOG.warn("FindBugs filter-file not found: {}", filterFile);
        }
      }

      engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
      engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

      engine.finishSettings();

      //Load findbugs report location
      List<String> potentialReportPaths = new ArrayList<>();
      potentialReportPaths.addAll(EXISTING_FINDBUGS_REPORT_PATHS);
      String[] paths = config.getStringArray(FindbugsConstants.REPORT_PATHS);
      if(paths != null) potentialReportPaths.addAll(Arrays.asList(paths));
      boolean foundExistingReport = false;

      //Look for existing reports relative to subproject directory
      reportPaths : for(String potentialPath : potentialReportPaths) {
        File findbugsReport = new File(fs.baseDir(), potentialPath);
        
        // File.length() is unspecified for directories
        if(findbugsReport.exists() && !findbugsReport.isDirectory() && findbugsReport.length() > 0) {
          LOG.info("FindBugs report is already generated {}. Reusing the report.",findbugsReport.getAbsolutePath());
          xmlBugReporter.getBugCollection().readXML(new FileReader(findbugsReport));
          foundExistingReport = true;
          break reportPaths;
        }
      }

      if(!foundExistingReport) { //Avoid rescanning the project if FindBugs was run already
        executorService.submit(new FindbugsTask(engine)).get(configuration.getTimeout(), TimeUnit.MILLISECONDS);
      }
      Collection<ReportedBug> reportedBugs = toReportedBugs(xmlBugReporter.getBugCollection());
      Collection<? extends AnalysisError> analysisErrors = ((SortedBugCollection) xmlBugReporter.getBugCollection()).getErrors();
      
      return new AnalysisResult(reportedBugs, analysisErrors);
    } catch (TimeoutException e) {
      throw new IllegalStateException("Can not execute Findbugs with a timeout threshold value of " + configuration.getTimeout() + " milliseconds", e);
    } catch (Exception e) {
      throw new IllegalStateException("Can not execute Findbugs", e);
    } finally {
      resetCustomPluginList(customPlugins);
      executorService.shutdown();
      IOUtils.closeQuietly(xmlOutput);
      Thread.currentThread().setContextClassLoader(initialClassLoader);
      Locale.setDefault(initialLocale);
    }
  }

  private static Collection<ReportedBug> toReportedBugs(BugCollection bugCollection) {
    // We need to retrieve information such as the message before we shut everything down as we will lose any custom
    // bug messages
    final Collection<ReportedBug> bugs = new ArrayList<ReportedBug>();

    for (final BugInstance bugInstance : bugCollection) {
      if (bugInstance.getPrimarySourceLineAnnotation() == null) {
        LOG.warn("No source line for " + bugInstance.getType());
        continue;
      }

      bugs.add(new ReportedBug(bugInstance));
    }
    return bugs;
  }

  private Integer determinePriorityThreshold() {
    Integer integer = priorityNameToValueMap.get(configuration.getConfidenceLevel());
    if (integer == null) {
      integer = DEFAULT_PRIORITY;
    }
    return integer;
  }

  private static class FindbugsTask implements Callable<Object> {

    private final FindBugs2 engine;

    public FindbugsTask(FindBugs2 engine) {
      this.engine = engine;
    }

    @Override
    public Object call() {
      try {
        engine.execute();
        return null;
      } catch (InterruptedException | IOException e) {
        throw Throwables.propagate(e);
      } finally {
        engine.dispose();
      }
    }
  }

  private Collection<Plugin> loadFindbugsPlugins(boolean useFbContrib,boolean useFindSecBugs) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    List<String> pluginJarPathList = Lists.newArrayList();
    try {
      Enumeration<URL> urls = contextClassLoader.getResources("findbugs.xml");
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        pluginJarPathList.add(normalizeUrl(url));
      }
      //Add fb-contrib plugin.
      if (useFbContrib && configuration.getFbContribJar() != null) {
        // fb-contrib plugin is packaged by Maven. It is not available during execution of unit tests.
        pluginJarPathList.add(configuration.getFbContribJar().getAbsolutePath());
      }
      //Add find-sec-bugs plugin. (same as fb-contrib)
      if (useFindSecBugs && configuration.getFindSecBugsJar() != null) {
        pluginJarPathList.add(configuration.getFindSecBugsJar().getAbsolutePath());
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
    List<Plugin> customPluginList = Lists.newArrayList();

    for (String path : pluginJarPathList) {
      try {
        Plugin plugin = Plugin.addCustomPlugin(new File(path).toURI(), contextClassLoader);
        if (plugin != null) {
          customPluginList.add(plugin);
          LOG.info("Loading findbugs plugin: " + path);
        }
      } catch (PluginException e) {
        LOG.warn("Failed to load plugin for custom detector: " + path);
        LOG.debug("Cause of failure", e);
      } catch (DuplicatePluginIdException e) {
        // FB Core plugin is always loaded, so we'll get an exception for it always
        if (!FINDBUGS_CORE_PLUGIN_ID.equals(e.getPluginId())) {
          // log only if it's not the FV Core plugin
          LOG.debug("Plugin already loaded: exception ignored: " + e.getMessage(), e);
        }
      }
    }

    return customPluginList;
  }

  private static String normalizeUrl(URL url) throws URISyntaxException {
    return StringUtils.removeStart(StringUtils.substringBefore(url.toURI().getSchemeSpecificPart(), "!"), "file:");
  }

  /**
   * Disable the update check for every plugin. See http://findbugs.sourceforge.net/updateChecking.html
   */
  private static void disableUpdateChecksOnEveryPlugin() {
    for (Plugin plugin : Plugin.getAllPlugins()) {
      plugin.setMyGlobalOption("noUpdateChecks", "true");
    }
  }

  private static void resetCustomPluginList(Collection<Plugin> customPlugins) {
    if (customPlugins != null) {
      for (Plugin plugin : customPlugins) {
        Plugin.removeCustomPlugin(plugin);

        try {
          // We have copied the plugin jar in the project's build directory
          // Now we need to close the classloaders pointing to that jar
          // so we do not prevent the deletion of the build folder
          plugin.close();
        } catch (IOException e) {
          LOG.error("Error closing plugin", e);
        }
      }
    }
  }

}
