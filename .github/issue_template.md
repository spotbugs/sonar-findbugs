
## Issue Description

<!-- Describe the symptom, the error message and the expected behavior. -->

<!-- Include partial log or console output that are relevant to this issue. -->


## Environment

<!-- The versions used: SonarQube 5.6/6.7/7.0, Sonar-FindBugs 3.6/3.7/..., Gradle 4.5/4.6, Maven 3.5.X, Java 7/8/9 -->

| Component          | Version |
| ------------------ | ------- |
| SonarQube          | ?????   |
| Sonar-FindBugs     | ?????   |
| Maven              | ?????   |
| Gradle             | ?????   |
| Java               | ?????   |

## Code (If needed)

<!-- Include the Java code samples or ZIP files of a sample project that reproduce the given bug. -->

```java
public class BugSample1 {
  public static void hello(String message) {
       
    //Something
    Runnable r = () -> System.out.println(message);
   
    r.run();
  }
}
```