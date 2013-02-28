/*
 * Created on Feb 25, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import java.io.File;

import org.gudy.azureus2.core3.util.SystemProperties;

import omschaub.azcvsupdater.main.View;

public class DirectoryUtils {
	
	public static String getInstallDirectory(){ 
		String installDirectory = View.getPluginInterface().getUtilities().getAzureusProgramDir();
		installDirectory = doubleSlash_At_End_Check(installDirectory);
		return installDirectory;
	}

	public static String getBackupDirectory(){
		String installDirectory = View.getPluginInterface().getUtilities().getAzureusProgramDir();
		installDirectory = doubleSlash_At_End_Check(installDirectory);
		String backupInstallDirectory = View.getPluginInterface().getPluginconfig().getPluginStringParameter("backup_directory", installDirectory + System.getProperty("file.separator") + "backup");


		File checkdir = new File(backupInstallDirectory);
		if (!checkdir.isDirectory()) {
			checkdir.mkdir();
		}
		
			// may not be able to write to this dir, fall back to user-dir if so - this is a fix
			// so we need to handle existing users where things are ok...
		
		if ( !checkdir.exists() || !checkdir.canWrite()){
		
			checkdir = new File( SystemProperties.getUserPath(), "backup" );
			
			checkdir.mkdirs();
			
			View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory", checkdir.getAbsolutePath());
		}
		
		return checkdir.getAbsolutePath();
	}


	private static String doubleSlash_At_End_Check(String directoryString){
		String doubleSlash = System.getProperty("file.separator");
		if (directoryString.endsWith(doubleSlash)){
			directoryString = directoryString.substring(0,(directoryString.length() - 1));
		}
		return directoryString;
	}
}
