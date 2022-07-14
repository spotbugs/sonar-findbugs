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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.LoggerFactory;

public enum FindbugsVersion {
  INSTANCE;

  private static final String PROPERTIES_PATH = "/org/sonar/plugins/findbugs/findbugs-plugin.properties";
  private String version;

  public static String getVersion() {
    return INSTANCE.version;
  }

  private FindbugsVersion() {
    try (InputStream input = getClass().getResourceAsStream(PROPERTIES_PATH)) {
      Properties properties = new Properties();
      properties.load(input);
      this.version = properties.getProperty("findbugs.plugin.version");
    } catch (IOException e) {
      LoggerFactory.getLogger(getClass()).warn("Can not load the Findbugs version from the file " + PROPERTIES_PATH);
      LoggerFactory.getLogger(getClass()).debug("Error while loading propoerties", e);
      this.version = "";
    }
  }
}
