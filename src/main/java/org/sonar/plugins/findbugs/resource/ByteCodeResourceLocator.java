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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteCodeResourceLocator {

    private static final Logger LOG = LoggerFactory.getLogger(ByteCodeResourceLocator.class);

    /**
     * JSP compiler choose an arbitrary package name when converting JSP files to classes files
     */
    private static final String[] KNOWN_JSP_PACKAGES = {"jsp/", "org/apache/jsp/", "jsp_servlet/"};

    public Resource findJavaClassFile(String className, Project project) {
        for(File sourceDir : project.getFileSystem().getSourceDirs()) {
            File potentialFile = new File(sourceDir, className.replaceAll("\\.","/")+".java");
            if(potentialFile.exists()) {
                org.sonar.api.resources.File file = org.sonar.api.resources.File.fromIOFile(potentialFile, project);
                return file;
            }
        }
        return null;
    }

    public Resource findTemplateFile(String className, Project project) {
        if(className.endsWith("_jsp")) {
            String jspFileFromClass = className.replaceAll("\\.", "/").replaceAll("_005f", "_").replaceAll("_jsp", ".jsp");
            List<String> potentialJspFilenames = new ArrayList<>();
            potentialJspFilenames.add(jspFileFromClass);

            for(String packageName : KNOWN_JSP_PACKAGES) {
                if(jspFileFromClass.startsWith(packageName))
                    potentialJspFilenames.add(jspFileFromClass.substring(packageName.length()));
            }

            //Source directories will include typically : /src/main/java and /src/main/webapp
            for(File sourceDir : project.getFileSystem().getSourceDirs()) {
                for(String jspFilename : potentialJspFilenames) {

                    File jspFile = new File(sourceDir, jspFilename);
                    if(jspFile.exists()) {
                        org.sonar.api.resources.File file = org.sonar.api.resources.File.fromIOFile(jspFile, project);
                        return file;
                    }
                }
            }
            LOG.warn("The source file for " + jspFileFromClass + " (" + className + ") was not found.");
        }
        return null;
    }

    public Integer findJspLine(String className, int originalLine, JavaResourceLocator javaResourceLocator) {
        for(File path : javaResourceLocator.classpath()) { //Include classes directories and jars
            if(path.isDirectory()) { //Skip the jars
                String relativeSmapFile = className.replaceAll("\\.","/")+".class.smap";
                File smapFile = new File(path, relativeSmapFile);
                if(smapFile.exists()) {
                    try {
                        InputStream smapInputStream = new FileInputStream(smapFile);
                        SMAPSourceDebugExtension debugExtension = new SMAPSourceDebugExtension(IOUtils.toString(smapInputStream));
                        List<Integer> jspLines= debugExtension.getJspLineNumber(originalLine);
                        if(jspLines != null) {
                            for(Integer l : jspLines) {
                                if(l != 0) {
                                    return l;
                                }
                            }
                        }
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return null; //No smap file is present.
    }
}
