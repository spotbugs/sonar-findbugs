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
package org.sonar.plugins.findbugs.configuration;

import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Splitter;

public class SimpleConfiguration implements Configuration {
  private Map<String, String> values = new HashMap<>();
  
  @Override
  public Optional<String> get(String key) {
    return Optional.ofNullable(values.get(key));
  }

  @Override
  public boolean hasKey(String key) {
    return values.containsKey(key);
  }

  @Override
  public String[] getStringArray(String key) {
    String value = values.get(key);
    if (value == null) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    List<String> values = new ArrayList<>();
    for (String v : Splitter.on(",").trimResults().split(value)) {
      values.add(v.replace("%2C", ","));
    }
    return values.toArray(new String[values.size()]);
  }

  public void setProperty(String key, int i) {
    values.put(key, Integer.toString(i));
  }

  public void setProperty(String key, String ... v) {
    values.put(key, String.join(",", v));
  }
}
