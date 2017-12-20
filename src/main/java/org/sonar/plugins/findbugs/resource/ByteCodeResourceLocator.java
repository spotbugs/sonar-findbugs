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
import org.sonar.plugins.java.api.JavaResourceLocator;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Utility method related to mapped class name to various resources and extracting addition information.
 */
public class ByteCodeResourceLocator implements BatchExtension {


    private static final Logger LOG = LoggerFactory.getLogger(ByteCodeResourceLocator.class);

    private static final String[] SOURCE_DIRECTORIES = {"src/main/java","src/main/webapp","src/main/resources", "src", "src/java", "app", "src/main/scala"};

    /**
     * findSourceFileKeyByClassName() is broken in SonarQube 6.3.1.. This method is fixing it.
     * @param className
     * @param javaResourceLocator
     * @return
     */
    public String findSourceFileKeyByClassName(String className, JavaResourceLocator javaResourceLocator) {
        String classFile = javaResourceLocator.findSourceFileKeyByClassName(className);
        if(classFile != null) return classFile;

        String fileName = className.replaceAll("\\.","/")+".class";

        Collection<File> classPathEntries = javaResourceLocator.classpath();
        for(File classPathEntry : classPathEntries) {
            if(classPathEntry.isDirectory()) { //Skip jars in the classpath

                File potentialLocation = new File(classPathEntry, fileName);
                if(potentialLocation.exists()) {
                    return potentialLocation.getPath();
                }
            }
        }
        return null;
    }

    /**
     * Find a Java source file based on the _exact_ filename passed.
     * @param sourceFile Path to the source file (/package/MyClass.java)
     * @param fs File system
     * @return The InputFile instance to the source file
     */
    public InputFile findSourceFile(String sourceFile, FileSystem fs) {
        return buildInputFile(sourceFile, fs);
    }

    /**
     * JSP files are compile to class with pseudo packages and class name that vary based on the compiler used.
     * Multiples patterns are test against the available sources files.<br>
     * (ie : <code>test.index_jsp</code> -&gt; <code>src/main/webapp/test/index.jsp</code>)
     * <br>
     * Their is a certain level of guessing since their could always be a class following the same pattern of colliding
     * precompiled jsp. (same pseudo package, same class format, etc.)
     *
     * @param className Class name of the precompiled jsp
     * @param fs File system
     * @return The InputFile instance to the template file
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
        for (String jspFilename : potentialJspFilenames) {
            InputFile file = buildInputFile(jspFilename, fs);
            if(file != null) return file;
        }
        return null;
    }

    /**
     * Look in the potential source directories to find the given source file
     * @param fileName Path to the source file (/package/MyClass.java)
     * @param fs File system
     * @return The InputFile instance to the source file
     */
    private InputFile buildInputFile(String fileName,FileSystem fs) {
        for(String sourceDir : SOURCE_DIRECTORIES) { //Quick lookup to skip manual iteration (see next loop)
            //System.out.println("Source file tested : "+sourceDir+"/"+fileName);
            Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasRelativePath(sourceDir+"/"+fileName));
            for (InputFile f : files) {
                return f;
            }
        }
        //Search for path _ending_ with the filename (https://github.com/SonarQubeCommunity/sonar-findbugs/issues/51)
        for (InputFile f : fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN))) {
            if (f.relativePath().endsWith(fileName)) {
                return f;
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
     * @param originalLine Line of code of the auto-generated Java line (.jsp -&gt; .java -&gt; .class)
     * @param classFile (Optional)
     * @return JSP line number
     */
    @Nullable
    public SmapParser.SmapLocation extractSmapLocation(String className, int originalLine, File classFile) {
        //Extract the SMAP (JSR45) from the class file (SourceDebugExtension section)
        try (InputStream in = new FileInputStream(classFile)) {
            DebugExtensionExtractor debug = new DebugExtensionExtractor();
            String smap = debug.getDebugExtFromClass(in);
            if(smap != null)
                return getJspLineNumberFromSmap(smap, originalLine);
        }
        catch (IOException e) {
            LOG.warn("An error occurs while opening classfile : {}", classFile.getPath());
        }
        LOG.debug("No smap file found for the class: " + className);

        //Extract the SMAP (JSR45) from the separated smap file
        File smapFile = new File(classFile.getPath()+".smap");
        if(smapFile.exists()) {
            try (InputStream smapInputStream = new FileInputStream(smapFile)) {
                return getJspLineNumberFromSmap(IOUtils.toString(smapInputStream), originalLine);
            }
            catch (IOException e) {
                LOG.debug("Unable to open smap file : {} ({})", smapFile.getAbsolutePath(), e.getMessage());
            }
        }
        else {
            LOG.debug("No smap mapping found.");
        }
        return null; //No smap file is present.
    }

    /**
     *
     * @param smap SMAP content (See smap.txt sample in test directories)
     * @param originalLine Java source code line number
     * @return JSP line number
     * @throws IOException
     */
    private SmapParser.SmapLocation getJspLineNumberFromSmap(String smap, Integer originalLine) throws IOException {
        SmapParser parser = new SmapParser(smap);
        return parser.getSmapLocation(originalLine);
    }

}
