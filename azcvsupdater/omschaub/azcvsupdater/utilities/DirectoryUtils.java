/*
 * Created on Feb 25, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import java.io.File;

import omschaub.azcvsupdater.main.View;

public class DirectoryUtils {
	
    public static String getInstallDirectory(){ 
    String installDirectory = new String (System.getProperty("user.dir"));
    installDirectory = doubleSlash_At_End_Check(installDirectory);
    return installDirectory;
    }
    
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
