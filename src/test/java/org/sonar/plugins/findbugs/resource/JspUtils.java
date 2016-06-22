package org.sonar.plugins.findbugs.resource;

import java.util.Arrays;
import java.util.List;

public class JspUtils {
  private static final List<String> javaKeywords = Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while");

  public static final String makeJavaPackage(String path)
  {
    String[] classNameComponents = path.split("/");
    StringBuilder legalClassNames = new StringBuilder();
    for (int i = 0; i < classNameComponents.length; i++)
    {
      legalClassNames.append(makeJavaIdentifier(classNameComponents[i]));
      if (i < classNameComponents.length - 1) {
        legalClassNames.append('.');
      }
    }
    return legalClassNames.toString();
  }

  private static final String makeJavaIdentifier(String identifier)
  {
    boolean periodToUnderscore = true;
    StringBuilder modifiedIdentifier = new StringBuilder(identifier.length());
    if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
      modifiedIdentifier.append('_');
    }
    for (int i = 0; i < identifier.length(); i++)
    {
      char ch = identifier.charAt(i);
      if ((Character.isJavaIdentifierPart(ch)) && ((ch != '_') || (!periodToUnderscore))) {
        modifiedIdentifier.append(ch);
      } else if ((ch == '.') && (periodToUnderscore)) {
        modifiedIdentifier.append('_');
      } else {
        modifiedIdentifier.append(JasperUtils.mangleChar(ch));
      }
    }
    if (isJavaKeyword(modifiedIdentifier.toString())) {
      modifiedIdentifier.append('_');
    }
    return modifiedIdentifier.toString();
  }


  public static boolean isJavaKeyword(String key)
  {
    return javaKeywords.contains(key);
  }
}
