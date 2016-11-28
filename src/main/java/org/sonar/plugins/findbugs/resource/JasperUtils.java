package org.sonar.plugins.findbugs.resource;

import java.awt.event.KeyEvent;

public class JasperUtils {

  /**
   * Transform a class name from a precompiled JSP and return the file name of the source file (*.jsp).
   * @param className Class name of the JSP class
   * @return JSP file name
   */
  public static String decodeJspClassName(String className) {
    className = className.replaceAll("\\.", "/");

    for(char ch = Character.MIN_VALUE; ch < 128 ; ch++) {
      //Condition minimize the number of replace operations
      if((isPrintableChar(ch) && !Character.isJavaIdentifierPart(ch)) || ch == '_') {

        //The replaceAll operation is highly ineffective for large string
        //In was implemented this way because it is simpler to maintain.
        className = className.replace(mangleChar(ch), Character.toString(ch));
      }
    }
    return className.replaceAll("_jsp", ".jsp");
  }

  /**
   * Encode special char to make sure it will be compliant to the class name restriction.
   * @param ch Special character
   * @return Encoded format (_XXXX)
   */
  public static final String mangleChar(char ch)
  {
    char[] result = new char[5];
    result[0] = '_';
    result[1] = Character.forDigit(ch >> '\f' & 0xF, 16);
    result[2] = Character.forDigit(ch >> '\b' & 0xF, 16);
    result[3] = Character.forDigit(ch >> '\004' & 0xF, 16);
    result[4] = Character.forDigit(ch & 0xF, 16);
    return new String(result);
  }

  /**
   * Detect if the character is printable
   * @param c Character to test
   * @return
   */
  public static boolean isPrintableChar( char c ) {
    Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
    return (!Character.isISOControl(c)) &&
            c != KeyEvent.CHAR_UNDEFINED &&
            block != null &&
            block != Character.UnicodeBlock.SPECIALS;
  }
}
