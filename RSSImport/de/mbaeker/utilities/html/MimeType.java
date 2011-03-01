/*
 * MimeType.java
 *
 * Created on 24. September 2000, 19:49
 */

package de.mbaeker.utilities.html;

/**
 * Class to guess the MimeType/ContentType of a filename/url
 * @author  Baeker
 * @version
 * 2000-09-24
 * - created.
 * 2000-10-05
 * - added arj
 * - some changes
 */
public class MimeType extends Object {
    
    public static int TEXT=0;
    public static int IMAGE=1;
    public static int AUDIO=2;
    public static int VIDEO=3;
    public static int APPLICATION=4;
    
    public static final String[] topLevel= {
        "text","image","audio","video","application"
    };
    private static final String[][][] topLevelEndings= {
        {
            {"cgi","html"},
            {"asp","html"},
            {"htm","html"},
            {"html"},
            {"txt","plain"},
            {"xml"},
            {"js","script"},
        }, {
            {"jpg","jpeg"},
            {"jpeg"},
            {"gif"},
            {"bmp"},
            {"tiff"},
            {"tif","tiff"},
            {"png" },
        }, {
            {"mp3"},
            {"mp2"},
            {"wav"},
            {"au"}
        }, {
            {"avi"},
            {"mpeg"},
            {"mpg","mpeg"},
            {"mpe"},
            {"mpv"},
            {"mlv"},
            {"asf"},
            {"asx"},
            {"wm"},
            {"wmx"},
            {"wmp"},
            {"wmv"},
            {"wvx"},
            {"mov","quicktime"}
        }, {
            {"exe","executeable"},
            {"com","executeable"},
            {"dat"},
            {"jar","packed"},
            {"j","packed"},
            {"zip","packed"},
            {"rar","packed"},
            {"arj","packed"},
            {"ps","postscript"},
            {"pdf"},
            {"torrent","x-bittorrent"},
        },
    };
    /** Creates new MimeType */
    public MimeType() {
    }
    
    public static boolean hasExtension(String filename) {
        boolean hasEnding=false;
        if (filename.indexOf(".")>-1) {
            if (filename.lastIndexOf(".")<filename.lastIndexOf("/")) //like test.Dir/
            {
                hasEnding=false;
            }
            else {
                hasEnding=true;
            }
        }
        return hasEnding;
    }
    
    public static String getExtension(String filename) {
        if (hasExtension(filename)) {
            String extension=filename.substring(filename.lastIndexOf(".")+1).toLowerCase();
            if (extension.indexOf('?')>-1) // eventuelle query abschneiden
            {
                extension=extension.substring(0,extension.indexOf('?'));
            }
            return extension;
        }
        return null;
    }
    
    public static String guessTopLevel(String filename) {
        String mime=guessMimeType(filename);
        return mime.substring(0,mime.indexOf('/'));
    }
    
    public static String guessMimeType(String filename) {
        //check image
        String ending=null;
        if (filename.lastIndexOf("/")>-1 && filename.substring(filename.lastIndexOf("/")).indexOf(".")==-1 && filename.indexOf('?')==-1 && !filename.endsWith("/")) {
            filename+="/";
        }
        //? is query and not a valid ending!!
        if (filename.indexOf('?')>-1) {
            filename=filename.substring(0,filename.indexOf('?'));
        }
        ending=getExtension(filename);
        if (ending!=null) {
            for (int level=0;level<topLevelEndings.length;level++) {
                for (int endings=0;endings<topLevelEndings[level].length;endings++) {
                    if (topLevelEndings[level][endings][0].equalsIgnoreCase(ending)) {
                        if (topLevelEndings[level][endings].length>1) {
                            return topLevel[level]+"/"+topLevelEndings[level][endings][1];
                        }
                        else {
                            return topLevel[level]+"/"+topLevelEndings[level][endings][0];
                        }
                    }
                }
            }
            return "application/octet-stream";
        }
        //if filename has no extension and ends with / return html
        if (filename.trim().endsWith("/")) {
            return "text/html";
        }
        return  "application/octet-stream";
    }
    
    public static int getTopLevelNumber(String toplevel) {
        if (toplevel!=null) {
            if (toplevel.indexOf("/")>-1) {
                toplevel=toplevel.substring(0,toplevel.indexOf('/'));
            }
            for (int i=0;i<topLevel.length;i++) {
                if (toplevel.equalsIgnoreCase(topLevel[i])) {
                    return i;
                }
            }
        }
        return 4;
    }
    
    public static void main(String args[]) {
        System.out.println(guessMimeType("http://server.com/?A=B"));
		System.out.println(guessMimeType("test.torrent"));
    }
    
}