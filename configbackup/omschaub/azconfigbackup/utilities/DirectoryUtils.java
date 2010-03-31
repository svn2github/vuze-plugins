/*
 * Created on Feb 25, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.utilities;

import java.io.File;
import java.util.Comparator;

import omschaub.azconfigbackup.main.View;

public class DirectoryUtils {
	
	private static final Comparator FILE_DATE_ASCENDING = new Comparator() {
		public int compare(Object o1, Object o2) {
			return (int)(((File)o1).lastModified() - ((File)o2).lastModified());
		}
	};
	
	private static final Comparator FILE_NAME_ASCENDING = new Comparator() {
		public int compare(Object o1, Object o2) {
			return ((File)o1).compareTo((File)o2);
		}
	};
	
	private static final Comparator FILE_SIZE_ASCENDING = new Comparator() {
		public int compare(Object o1, Object o2) {
			return (int)(((File)o1).length() - ((File)o2).length());
		}
	};

    public static String getBackupDirectory(){
    String installDirectory = new String (System.getProperty("user.dir"));
    installDirectory = doubleSlash_At_End_Check(installDirectory);
    String backupInstallDirectory = View.getPluginInterface().getPluginconfig().getPluginStringParameter("backup_directory", installDirectory + System.getProperty("file.separator") + "backup");
    
    
    File checkdir = new File(backupInstallDirectory);
    if (!checkdir.isDirectory()) {
        checkdir.mkdir();
        }else;
    return backupInstallDirectory;
    }

    
    private static String doubleSlash_At_End_Check(String directoryString){
        String doubleSlash = System.getProperty("file.separator");
        if (directoryString.endsWith(doubleSlash)){
            directoryString = directoryString.substring(0,(directoryString.length() - 1));
        }
        return directoryString;
    }
}
