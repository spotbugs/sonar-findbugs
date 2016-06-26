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

import org.sonar.api.Plugin;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.language.JspCodeColorizerFormat;
import org.sonar.plugins.findbugs.profiles.FindbugsContribProfile;
import org.sonar.plugins.findbugs.profiles.FindbugsProfile;
import org.sonar.plugins.findbugs.profiles.FindbugsSecurityAuditProfile;
import org.sonar.plugins.findbugs.profiles.FindbugsSecurityJspProfile;
import org.sonar.plugins.findbugs.profiles.FindbugsSecurityMinimalProfile;
import org.sonar.plugins.findbugs.resource.ByteCodeResourceLocator;
import org.sonar.plugins.findbugs.rules.FbContribRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsJspRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindSecurityBugsRulesDefinition;
import org.sonar.plugins.findbugs.rules.FindbugsRulesDefinition;

import java.util.Arrays;

public class FindbugsPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(FindbugsConfiguration.getPropertyDefinitions());
    context.addExtensions(Arrays.asList(Jsp.class,
            JspCodeColorizerFormat.class,
            FindbugsSensor.class,
            FindbugsConfiguration.class,
            FindbugsExecutor.class,
            FindbugsProfileExporter.class,
            FindbugsProfileImporter.class,

            FindbugsProfile.class,
            FindbugsContribProfile.class,
            FindbugsSecurityAuditProfile.class,
            FindbugsSecurityMinimalProfile.class,
            FindbugsSecurityJspProfile.class,

            FindbugsRulesDefinition.class,
            FbContribRulesDefinition.class,
            FindSecurityBugsRulesDefinition.class,
            FindSecurityBugsJspRulesDefinition.class,
            ByteCodeResourceLocator.class));
  }
}
