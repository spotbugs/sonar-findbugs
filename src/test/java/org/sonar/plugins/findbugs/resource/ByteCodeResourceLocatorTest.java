package org.sonar.plugins.findbugs.resource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ByteCodeResourceLocatorTest {
  FileSystem fs;
  FilePredicates predicates;

  @Before
  public void setUp() {

    fs = mock(FileSystem.class);
    predicates = mock(FilePredicates.class);
    when(fs.predicates()).thenReturn(predicates);
  }


  @Test
  public void findJavaClassFile_normalClassName() {
    //No Input file need to be return for the test
    when(fs.inputFiles(Matchers.<FilePredicate>any())).thenReturn(new ArrayList<InputFile>());

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    locator.findJavaClassFile("com.helloworld.ThisIsATest",fs);

    verify(predicates,times(1)).hasRelativePath("src/main/java/com/helloworld/ThisIsATest.java");
  }

  @Test
  public void findJavaClassFile_withInnerClass() {
    //No Input file need to be return for the test
    when(fs.inputFiles(Matchers.<FilePredicate>any())).thenReturn(new ArrayList<InputFile>());

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    locator.findJavaClassFile("com.helloworld.ThisIsATest$InnerClass",fs);

    verify(predicates,times(1)).hasRelativePath("src/main/java/com/helloworld/ThisIsATest.java");
  }

  @Test
  public void findTemplateFile_weblogicFileName() {

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();

    //No Input file need to be return for the test
    when(fs.inputFiles(Matchers.<FilePredicate>any())).thenReturn(new ArrayList<InputFile>());

    locator.findTemplateFile("jsp_servlet._folder1._folder2.__helloworld", fs);

    verify(predicates,times(1)).hasRelativePath("src/main/webapp//folder1/folder2/helloworld.jsp");
  }

  @Test
  public void findTemplateFile_jasperFileName() {

    String prefixSource = "src/main/webapp/org/apache/jsp/";

    String[] pages = {"WEB-INF/pages/widgets/cookies_and_params.jsp", "lessons/DBCrossSiteScripting/DBCrossSiteScripting.jsp"};

    for(String jspPage : pages) {
      String name = "org.apache.jsp." + JspUtils.makeJavaPackage(jspPage);
      System.out.println(name);

      ByteCodeResourceLocator locator = new ByteCodeResourceLocator();

      //No Input file need to be return for the test
      when(fs.inputFiles(Matchers.<FilePredicate>any())).thenReturn(new ArrayList<InputFile>());

      locator.findTemplateFile(name, fs);

      System.out.println("Expecting: "+prefixSource + jspPage);
      verify(predicates,times(1)).hasRelativePath(prefixSource + jspPage);
    }

  }


}
