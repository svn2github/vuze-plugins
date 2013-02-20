/*
 * Util.java
 *
 * Created on February 24, 2004, 12:00 PM
 */

package i18nPlugin;

/** Misc Usefull functions.
 *
 * @author  TuxPaper
 */
public class Util {
  private static final String zeros = "000";
  
  public static final boolean doUTF8 = false;
  
  public static String toEscape(String str) {
    
    StringBuffer sb = new StringBuffer();
    char[] charArr = str.toCharArray();
    for (int i = 0; i < charArr.length; i++) {
      if (charArr[i] == '\n')
        sb.append( "\\n" );
      else if (charArr[i] == '\t')
        sb.append( "\\t" );
      else if (charArr[i] >= 0 && charArr[i] < 128 && charArr[i] != '\\' ) 
        sb.append( charArr[i] );
      else {
      	if (doUTF8) {
      		sb.append( charArr[i]);
      	} else {
      		sb.append( toEscape( charArr[i] ));
      	}
      }
    }
    return sb.toString();
  }
 
  public static String toEscape(char c) {
    int n = (int)c;
    String body = Integer.toHexString(n);
    return ("\\u" + zeros.substring(0, 4 - body.length()) + body);
  }
}
