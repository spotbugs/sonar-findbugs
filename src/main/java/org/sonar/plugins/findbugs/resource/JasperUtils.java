package org.sonar.plugins.findbugs.resource;

import java.awt.event.KeyEvent;

public class JasperUtils {

  public static String decodeJspClassName(String value) {
    //This function is replacing this operation and is expected to support all special characters instead only 2.
    //Prior implementation : .replaceAll("\\.", "/").replaceAll("_005f", "_").replaceAll("_002d","-").replaceAll("_jsp", ".jsp")

    value = value.replaceAll("\\.", "/");

    for(char ch = Character.MIN_VALUE; ch < 128 ; ch++) {
      if(isPrintableChar(ch) && !Character.isJavaIdentifierPart(ch) || ch == '_') {
        //System.out.println("Code "+ ((int)ch)+":");
        //System.out.println(ch);

        //The replaceAll operation are highly ineffective for large string
        //In was implemented this way because is
        value = value.replace(mangleChar(ch),""+ch);
      }
    }
    return value.replaceAll("_jsp", ".jsp");
  }

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

  public static boolean isPrintableChar( char c ) {
    Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
    return (!Character.isISOControl(c)) &&
            c != KeyEvent.CHAR_UNDEFINED &&
            block != null &&
            block != Character.UnicodeBlock.SPECIALS;
  }
}
