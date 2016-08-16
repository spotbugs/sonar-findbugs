package org.sonar.plugins.findbugs.resource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ByteCodeResourceLocatorTest {

  //File system that return mock input files
//  FileSystem fs;
//  FilePredicates predicates;

  //File system that return no Input files
  FileSystem fsEmpty;
  FilePredicates predicatesEmpty;

  @Before
  public void setUp() {

    //Not used for the moment
//    fs = mock(FileSystem.class);
//    predicates = mock(FilePredicates.class);
//    when(fs.predicates()).thenReturn(predicates);

    fsEmpty = mock(FileSystem.class);
    predicatesEmpty = mock(FilePredicates.class);
    when(fsEmpty.predicates()).thenReturn(predicatesEmpty);
    when(fsEmpty.inputFiles(any(FilePredicate.class))).thenReturn(new ArrayList<InputFile>());
  }


  @Test
  public void findJavaClassFile_normalClassName() {

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    locator.findJavaClassFile("com.helloworld.ThisIsATest", fsEmpty);

    verify(predicatesEmpty,times(1)).hasRelativePath("src/main/java/com/helloworld/ThisIsATest.java");
  }

  @Test
  public void findJavaClassFile_withInnerClass() {

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    locator.findJavaClassFile("com.helloworld.ThisIsATest$InnerClass",fsEmpty);

    verify(predicatesEmpty,times(1)).hasRelativePath("src/main/java/com/helloworld/ThisIsATest.java");
  }

  @Test
  public void findTemplateFile_weblogicFileName() {

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();

    locator.findTemplateFile("jsp_servlet._folder1._folder2.__helloworld", fsEmpty);

    verify(predicatesEmpty,times(1)).hasRelativePath("src/main/webapp//folder1/folder2/helloworld.jsp");
  }

  @Test
  public void findTemplateFile_jasperFileName() {

    String prefixSource = "src/main/webapp/org/apache/jsp/";

    String[] pages = {"WEB-INF/pages/widgets/cookies_and_params.jsp", "lessons/DBCrossSiteScripting/DBCrossSiteScripting.jsp"};

    for(String jspPage : pages) {
      String name = "org.apache.jsp." + JspUtils.makeJavaPackage(jspPage);
      System.out.println("Compiled class name: "+name);

      ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
      locator.findTemplateFile(name, fsEmpty);

      System.out.println("Expecting: "+ prefixSource + jspPage);
      verify(predicatesEmpty,times(1)).hasRelativePath(prefixSource + jspPage);
    }

  }


}
