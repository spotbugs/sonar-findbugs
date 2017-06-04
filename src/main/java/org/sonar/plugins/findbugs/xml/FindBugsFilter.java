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
package org.sonar.plugins.findbugs.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.findbugs.FindbugsLevelUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XStreamAlias("FindBugsFilter")
public class FindBugsFilter {

  private static final String PATTERN_SEPARATOR = ",";
  private static final String CODE_SEPARATOR = ",";
  private static final String CATEGORY_SEPARATOR = ",";
  private static final Class[] ALL_XSTREAM_TYPES = {Bug.class, ClassFilter.class, FieldFilter.class, FindBugsFilter.class, LocalFilter.class, Match.class, MethodFilter.class, OrFilter.class, PackageFilter.class, Priority.class};

  @XStreamImplicit
  private List<Match> matchs;

  public FindBugsFilter() {
    matchs = new ArrayList<>();
  }

  public String toXml() {
    XStream xstream = createXStream();
    return xstream.toXML(this);
  }

  public List<Match> getMatchs() {
    return matchs;
  }

  public List<Match> getChildren() {
    return matchs;
  }

  public void addMatch(Match child) {
    matchs.add(child);
  }

  public Map<String, String> getPatternLevels(FindbugsLevelUtils priorityMapper) {
    return processMatches(priorityMapper, new PatternSplitter());
  }

  public Map<String, String> getCodeLevels(FindbugsLevelUtils priorityMapper) {
    return processMatches(priorityMapper, new CodeSplitter());
  }

  public Map<String, String> getCategoryLevels(FindbugsLevelUtils priorityMapper) {
    return processMatches(priorityMapper, new CategorySplitter());
  }

  private static String getRuleSeverity(Priority priority, FindbugsLevelUtils priorityMapper) {
    return priority != null ? priorityMapper.from(priority.getValue()) : null;
  }

  private Map<String, String> processMatches(FindbugsLevelUtils priorityMapper, BugInfoSplitter splitter) {
    Map<String, String> result = new HashMap<>();
    for (Match child : getChildren()) {
      if (child.getOrs() != null) {
        for (OrFilter orFilter : child.getOrs()) {
          completeLevels(result, orFilter.getBugs(), child.getPriority(), priorityMapper, splitter);
        }
      }
      if (child.getBug() != null) {
        completeLevels(result, Arrays.asList(child.getBug()), child.getPriority(), priorityMapper, splitter);
      }
    }
    return result;
  }

  private static void completeLevels(Map<String, String> result, List<Bug> bugs, Priority priority, FindbugsLevelUtils priorityMapper, BugInfoSplitter splitter) {
    if (bugs == null) {
      return;
    }
    String severity = getRuleSeverity(priority, priorityMapper);
    for (Bug bug : bugs) {
      String varToSplit = splitter.getVar(bug);
      if (!StringUtils.isBlank(varToSplit)) {
        String[] splitted = StringUtils.split(varToSplit, splitter.getSeparator());
        for (String code : splitted) {
          mapRuleSeverity(result, severity, code);
        }
      }
    }
  }

  private static void mapRuleSeverity(Map<String, String> severityByRule, String severity, String key) {
    if (severityByRule.containsKey(key) && severityByRule.get(key) != null) {
      severityByRule.put(key, getHighestSeverity(severityByRule.get(key), severity));
    } else {
      severityByRule.put(key, severity);
    }
  }

  private static String getHighestSeverity(String s1, String s2) {
    if (s1.equals(s2) || (Severity.MAJOR.equals(s1) && Severity.INFO.equals(s2)) || Severity.BLOCKER.equals(s1)) {
      return s1;
    }
    return s2;
  }

  public static XStream createXStream() {
    XStream xstream = new XStream(new StaxDriver());
    XStream.setupDefaultSecurity(xstream); //Setup the default hardening of types disallowed.
    xstream.setClassLoader(FindBugsFilter.class.getClassLoader());

    for (Class modelClass : ALL_XSTREAM_TYPES) {
      xstream.processAnnotations(modelClass);
      xstream.allowTypeHierarchy(modelClass); //Build a whitelist of the class allowed
    }
    return xstream;
  }

  private interface BugInfoSplitter {
    String getVar(Bug bug);

    String getSeparator();
  }

  private static class PatternSplitter implements BugInfoSplitter {
    @Override
    public String getSeparator() {
      return PATTERN_SEPARATOR;
    }

    @Override
    public String getVar(Bug bug) {
      return bug.getPattern();
    }
  }

  private static class CodeSplitter implements BugInfoSplitter {
    @Override
    public String getSeparator() {
      return CODE_SEPARATOR;
    }

    @Override
    public String getVar(Bug bug) {
      return bug.getCode();
    }
  }

  private static class CategorySplitter implements BugInfoSplitter {
    @Override
    public String getSeparator() {
      return CATEGORY_SEPARATOR;
    }

    @Override
    public String getVar(Bug bug) {
      return bug.getCategory();
    }
  }
}
