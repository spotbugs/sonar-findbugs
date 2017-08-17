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

import com.thoughtworks.xstream.XStream;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;
import org.sonar.plugins.findbugs.xml.Bug;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;

import java.io.IOException;
import java.io.Writer;
import java.util.stream.Collectors;

public class FindbugsProfileExporter extends ProfileExporter {

  public FindbugsProfileExporter() {
    super(/* (Godin): actually exporter key: */FindbugsRulesDefinition.REPOSITORY_KEY, FindbugsConstants.PLUGIN_NAME);
    setSupportedLanguages(FindbugsPlugin.SUPPORTED_JVM_LANGUAGES);
    setMimeType("application/xml");
  }

  @Override
  public void exportProfile(RulesProfile profile, Writer writer) {
    try {
      FindBugsFilter filter = buildFindbugsFilter(
              profile.getActiveRules().stream().filter(activeRule ->
                      activeRule.getRepositoryKey().contains("findbugs") ||
                              activeRule.getRepositoryKey().contains("findsecbugs") ||
                              activeRule.getRepositoryKey().contains("fb-contrib"))
                      .collect(Collectors.toList())
      );
      XStream xstream = FindBugsFilter.createXStream();
      writer.append(xstream.toXML(filter));
    } catch (IOException e) {
      throw new SonarException("Fail to export the Findbugs profile : " + profile, e);
    }
  }

  public static FindBugsFilter buildFindbugsFilter(Iterable<ActiveRule> activeRules) {
    FindBugsFilter root = new FindBugsFilter();
    for (ActiveRule activeRule : activeRules) {
      String repoKey = activeRule.getRepositoryKey();

      if (repoKey.contains("findsecbugs") || repoKey.contains("findbugs") || repoKey.contains("fb-contrib")) {
        Match child = new Match();
        child.setBug(new Bug(activeRule.getConfigKey()));
        root.addMatch(child);
      }
    }
    return root;
  }

}
