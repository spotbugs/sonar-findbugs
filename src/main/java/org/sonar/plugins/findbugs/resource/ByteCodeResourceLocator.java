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
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility method related to mapped class name to various resources and extracting addition information.
 */
public class ByteCodeResourceLocator implements BatchExtension {


    private static final Logger LOG = LoggerFactory.getLogger(ByteCodeResourceLocator.class);

    /**
     * Find the file system location of a given class name.<br/>
     * (ie : <code>test.SomeClass</code> ->  <code>src/main/java/test/SomeClass.java</code>)
     *
     * @param className Class name to look for
     * @param fs File system
     * @return Java source file that conrespond to the class name specified.
     */
    public InputFile findJavaClassFile(String className, FileSystem fs) {
        int indexDollarSign = className.indexOf("$");
        if(indexDollarSign != -1) {
            className = className.substring(0, indexDollarSign); //Remove innerClass from the class name
        }

        Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasRelativePath("src/main/java/"+className.replaceAll("\\.","/")+".java"));

        for(InputFile f: files) {
            return f;
        }
        return null;

//        for(File sourceDir : proj.) {
//            File potentialFile = new File(sourceDir, );
//            if(potentialFile.exists()) {
//                org.sonar.api.resources.File file = org.sonar.api.resources.File.fromIOFile(potentialFile, project);
//                return file;
//            }
//        }
//        return null;
    }

    /**
     * JSP files are compile to class with pseudo packages and class name that vary based on the compiler used.
     * Multiples patterns are test against the available sources files.<br/>
     * (ie : <code>test.index_jsp</code> ->  <code>src/main/webapp/test/index.jsp</code>)
     * <br/>
     * Their is a certain level of guessing since their could always be a class following the same pattern of colliding
     * precompiled jsp. (same pseudo package, same class format, etc.)
     *
     * @param className Class name of the precompiled jsp
     * @param fs File system
     * @return The
     */
    public InputFile findTemplateFile(String className, FileSystem fs) {
        List<String> potentialJspFilenames = new ArrayList<>();


        //Weblogic APPC precompiled form
        //Expected class name: "jsp_servlet._folder1._folder2.__helloworld"
        if(className.startsWith("jsp_servlet")) {
            String jspFile = className.substring(11).replaceFirst("\\.__([^\\.]+)$", "/$1\\.jsp").replace("._", "/");
            potentialJspFilenames.add(jspFile);
        }
        //Jetty and Tomcat JSP precompiled form
        //Expected class name: "jsp.folder1.folder2.hello_005fworld_jsp"
        if (className.endsWith("_jsp")) {
            String jspFileFromClass = JasperUtils.decodeJspClassName(className);

            potentialJspFilenames.add(jspFileFromClass);

            for(String packageName : Arrays.asList("jsp/", "org/apache/jsp/")) {
                if(jspFileFromClass.startsWith(packageName))
                    potentialJspFilenames.add(jspFileFromClass.substring(packageName.length()));
            }
        }

        //Source directories will include typically : /src/main/java and /src/main/webapp

        for(String sourceDir : Arrays.asList("src/main/webapp/","src/main/resources","src/main/java")) {
            for (String jspFilename : potentialJspFilenames) {
                Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasRelativePath(sourceDir+jspFilename));
                for (InputFile f : files) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Map Java line number to JSP line number based on SMAP
     *
     * The smap can be either embedded in the class file or alternatively place in separate file.
     *
     * @param className Class name
     * @param originalLine Line of code of the auto-generated Java line (.jsp -> .java -> .class)
     * @param classFile (Optional)
     * @return JSP line number
     */
    public Integer findJspLine(String className, int originalLine, File classFile) {
        //Extract the SMAP (JSR45) from the class file (SourceDebugExtension section)
        try (InputStream in = new FileInputStream(classFile)) {
            DebugExtensionExtractor debug = new DebugExtensionExtractor();
            return getJspLineNumberFromSmap(debug.getDebugExtFromClass(in), originalLine);
        }
        catch (IOException e) {
            LOG.warn("An error occurs while opening classfile : " + classFile.getPath());
        }
        LOG.debug("No smap file found for the class: " + className);

        //Extract the SMAP (JSR45) from the separated smap file
        File smapFile = new File(classFile.getPath()+".smap");
        if(smapFile.exists()) {
            try (InputStream smapInputStream = new FileInputStream(smapFile)) {
                return getJspLineNumberFromSmap(IOUtils.toString(smapInputStream), originalLine);
            }
            catch (IOException e) {
                LOG.debug("Unable to open smap file : " + smapFile.getAbsolutePath());
                throw new RuntimeException(e);
            }
        }

        LOG.debug("No smap mapping found.");
        return null; //No smap file is present.
    }

    /**
     *
     * @param smap SMAP content (See smap.txt sample in test directories)
     * @param originalLine Java source code line number
     * @return JSP line number
     * @throws IOException
     */
    private int getJspLineNumberFromSmap(String smap, Integer originalLine) throws IOException {
        SmapParser parser = new SmapParser(smap);
        int[] mapping = parser.getScriptLineNumber(originalLine);
        return mapping[1];
    }
}
