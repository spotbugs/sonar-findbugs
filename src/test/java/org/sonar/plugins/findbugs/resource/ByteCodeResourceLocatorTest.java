package org.sonar.plugins.findbugs.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

class ByteCodeResourceLocatorTest {

  @TempDir
  public File temp;

  //File system that return no Input files
  FileSystem fsEmpty;
  FilePredicates predicatesEmpty;

  @BeforeEach
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
  void findJavaClassFile_normalClassName() {

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    locator.findSourceFile("com/helloworld/ThisIsATest.java", fsEmpty);

    verify(predicatesEmpty,times(1)).hasRelativePath("src/main/java/com/helloworld/ThisIsATest.java");
  }

  @Test
  void findScalaClassFileNormalClassName() {

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    locator.findSourceFile("com/helloworld/ThisIsATest.scala", fsEmpty);

    verify(predicatesEmpty,times(1)).hasRelativePath("src/main/scala/com/helloworld/ThisIsATest.scala");
  }

//  @Test
//  public void findJavaClassFile_withInnerClass() {
//
//    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
//    locator.findJavaClassFile("com.helloworld.ThisIsATest$InnerClass",fsEmpty);
//
//    verify(predicatesEmpty,times(1)).hasRelativePath("src/main/java/com/helloworld/ThisIsATest.java");
//  }

  @Test
  void findTemplateFile_weblogicFileName() {

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();

    locator.findTemplateFile("jsp_servlet._folder1._folder2.__helloworld", fsEmpty);

    verify(predicatesEmpty,times(1)).hasRelativePath("src/main/webapp//folder1/folder2/helloworld.jsp");
  }

  @Test
  void findTemplateFile_jasperFileName() {

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

  @Test
  void findRegularSourceFile() throws Exception {
    InputFile givenJavaFile = mock(InputFile.class);
    when(fsEmpty.inputFiles(any())).thenReturn(Collections.singletonList(givenJavaFile));

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    assertEquals(givenJavaFile, locator.findSourceFile("com/helloworld/TestJavaClass.java", fsEmpty));
  }

  @Test
  void findSourceFileFromScalaClassName() throws Exception {
    InputFile givenJavaFile = mock(InputFile.class);
    when(fsEmpty.inputFiles(any())).thenReturn(Collections.singletonList(givenJavaFile));

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    assertEquals(givenJavaFile, locator.findSourceFile("TestOperationalProfileIccidModel$TestOperationalProfileIccid$.class", fsEmpty));
  }
  
  @Test
  void findClassFileByClassName() throws IOException {
    JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
    
    Path folderPath = Files.createDirectories(temp.toPath().resolve("foo").resolve("bar"));
    Path testFolderPath = Files.createDirectories(temp.toPath().resolve("test").resolve("123"));
    Path filePath = Files.createFile(folderPath.resolve("Test.class"));
    
    when(javaResourceLocator.classpath()).thenReturn(Arrays.asList(testFolderPath.toFile(), filePath.toFile(), temp));

    ByteCodeResourceLocator locator = new ByteCodeResourceLocator();
    assertThat(locator.findClassFileByClassName("foo.bar.Test", javaResourceLocator)).isNotNull();
  }
}
