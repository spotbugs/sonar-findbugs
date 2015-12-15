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
package org.sonar.plugins.findbugs.resource;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.fest.assertions.Assertions.assertThat;

public class DebugExtensionExtractorTest {

    @Test
    public void loadDebugInfoFromWeblogicClass() throws IOException {
        InputStream in = getClass().getResourceAsStream("/jsp_classes/weblogic/__test.clazz");
        String debugInfo = new DebugExtensionExtractor().getDebugExtFromClass(in);
        //System.out.println(debugInfo);

        SmapParser smap = new SmapParser(debugInfo);
        int[] jspLines = smap.getScriptLineNumber(282);
        assertThat(jspLines[1]).isEqualTo(20);
    }

    @Test
    public void loadDebugInfoFromJettyClass() throws IOException {
        InputStream in = getClass().getResourceAsStream("/jsp_classes/jetty936/test_jsp.clazz");
        String debugInfo = new DebugExtensionExtractor().getDebugExtFromClass(in);
        //System.out.println(debugInfo);

        SmapParser smap = new SmapParser(debugInfo);
        int[] jspLines = smap.getScriptLineNumber(260);
        assertThat(jspLines[1]).isEqualTo(20);
    }
}
