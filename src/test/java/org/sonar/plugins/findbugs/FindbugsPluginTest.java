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

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.utils.Version;

import static junit.framework.TestCase.assertEquals;

public class FindbugsPluginTest {

  @Test
  public void testGetExtensions() {

    Plugin.Context ctx = new FindbugsPlugin.Context(Version.parse("1.33.7"));

    FindbugsPlugin plugin = new FindbugsPlugin();
    plugin.define(ctx);

    assertEquals("extension count", 21, ctx.getExtensions().size());
  }

}
