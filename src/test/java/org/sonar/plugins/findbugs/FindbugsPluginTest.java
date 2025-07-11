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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

class FindbugsPluginTest {

  @ParameterizedTest
  @CsvSource({
    "11.3,26",
    // We disable the profile exporter when the plugin API version is >= 11.4 (since 2025.3 commercial editions)
    "11.4,25"
  })
  void testGetExtensions(String version, int expectedExtensionsCount) {
    SonarRuntime runtime = mock(SonarRuntime.class);
    when(runtime.getApiVersion()).thenReturn(Version.parse(version));
    Plugin.Context ctx = new Plugin.Context(runtime);

    FindbugsPlugin plugin = new FindbugsPlugin();
    plugin.define(ctx);

    assertEquals(expectedExtensionsCount, ctx.getExtensions().size(), "extensions count");
  }
}
