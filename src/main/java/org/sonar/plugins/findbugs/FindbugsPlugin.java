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

import com.google.common.collect.ImmutableList;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.language.JspCodeColorizerFormat;

import java.util.List;

public class FindbugsPlugin extends SonarPlugin {

  @Override
  public List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();
    extensions.addAll(FindbugsConfiguration.getPropertyDefinitions());
    extensions.add(
      Jsp.class,
      JspCodeColorizerFormat.class,
      FindbugsSensor.class,
      FindbugsConfiguration.class,
      FindbugsExecutor.class,
      FindbugsProfileExporter.class,
      FindbugsProfileImporter.class,
      FindbugsProfile.class,
      FindbugsSecurityAuditProfile.class,
      FindbugsSecurityMinimalProfile.class,
      FindbugsSecurityJspProfile.class,
      FindbugsRulesDefinition.class,
      FbContribRulesDefinition.class,
      FindSecurityBugsRulesDefinition.class,
      FindSecurityBugsJspRulesDefinition.class);
    return extensions.build();
  }

}
