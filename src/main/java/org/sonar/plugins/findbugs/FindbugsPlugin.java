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
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.plugins.findbugs.language.Jsp;
import org.sonar.plugins.findbugs.language.JspSyntaxSensor;
import org.sonar.plugins.findbugs.profiles.*;
import org.sonar.plugins.findbugs.resource.ByteCodeResourceLocator;
import org.sonar.plugins.findbugs.rules.*;
import org.sonar.plugins.java.Java;

import java.util.Arrays;
import java.util.stream.Collectors;

public class FindbugsPlugin implements Plugin {

    public static final String SUPPORTED_JVM_LANGUAGES[] = {
            Java.KEY, Jsp.KEY, "scala", "clojure"
    };

    public static final String SUPPORTED_JVM_LANGUAGES_EXTENSIONS[] = {
            "java", "jsp", "scala", "clj"
    };

    public static FilePredicate[] getSupportedLanguagesFilePredicate(FilePredicates pred) {
        return Arrays.stream(SUPPORTED_JVM_LANGUAGES)
                .map(s -> pred.hasLanguage(s))
                .collect(Collectors.toList())
                .toArray(new FilePredicate[SUPPORTED_JVM_LANGUAGES.length]);
    }

    @Override
  public void define(Context context) {
    context.addExtensions(FindbugsConfiguration.getPropertyDefinitions());
    context.addExtensions(Arrays.asList(
            Jsp.class,
            JspSyntaxSensor.class,

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
