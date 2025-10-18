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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FindbugsCategory {
  private static final Map<String, String> FINDBUGS_TO_SONAR;
  
  static {
    Map<String, String> map = new HashMap<>();

    map.put("BAD_PRACTICE", "Bad practice");
    map.put("CORRECTNESS", "Correctness");
    map.put("MT_CORRECTNESS", "Multithreaded correctness");
    map.put("I18N", "Internationalization");
    map.put("EXPERIMENTAL", "Experimental");
    map.put("MALICIOUS_CODE", "Malicious code");
    map.put("PERFORMANCE", "Performance");
    map.put("SECURITY", "Security");
    map.put("STYLE", "Style");
    
    FINDBUGS_TO_SONAR = Collections.unmodifiableMap(map);
  }

  public static String findbugsToSonar(String findbugsCategKey) {
    return FINDBUGS_TO_SONAR.get(findbugsCategKey);
  }

  private FindbugsCategory() {
  }

}
