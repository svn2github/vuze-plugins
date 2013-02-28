/*
 * Created on Feb 5, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.main;

import java.io.File;
import java.text.Collator;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import omschaub.azconfigbackup.utilities.DirectoryUtils;
import omschaub.azconfigbackup.utilities.StackX;

public class Tab6Utils {
	
    private static void sortBackupArray(File[] fileArray){
        Collator collator = Collator.getInstance(Locale.getDefault());
        
        File tmp0;
        if (fileArray.length == 1) return;
        for (int i = 0; i < fileArray.length; i++) {
          for (int j = i + 1; j < fileArray.length; j++) {
            if( collator.compare(fileArray[i].getName(), fileArray[j].getName()) < 0 ) {
              tmp0 = fileArray[i];
              fileArray[i] = fileArray[j];
              fileArray[j] = tmp0;
              
              }
            }
          }
    }
    
    
    /**
     * Sets up the combo for the BackupFiles in Tab6
     * @param destinationDirEnd
     */
    static void getBackupFiles(String destinationDirEnd){
//      Getting Backup Directory Listing
        if(Tab6.backup_combo == null && Tab6.backup_combo.isDisposed())
            return;
            
        Tab6.backup_combo.removeAll();
        //Tab6.directory_get.add(destinationDirEnd);
        String configInstallDirectory = DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator") + "config";
//        System.out.println(configInstallDirectory);
        final String configDirectory = configInstallDirectory;
        try {
        	File f = new File(configDirectory);
        	if(!f.isDirectory()){
                f.mkdir();
            }
            File[] files = f.listFiles();    
            
            sortBackupArray(files);
            
            
        	for(int i = 0 ; i < files.length ; i++) {
        		String fileName = files[i].getName();
        		if(files[i].isDirectory() )
        		{
                    Tab6.backup_combo.add(fileName);
        			//if(!fileName.equalsIgnoreCase(destinationDirEnd)){
        			    
        			//}
        		
        		
        		}
        	}
        	
        	Tab6.backup_combo.select(0);
        } catch(Exception e) {
        	//Stop process and trace the exception
        	e.printStackTrace();
        }
    }

    static void getConfigFiles(){
        if(Tab6.restore_combo==null && Tab6.restore_combo.isDisposed())
            return;
            
            // Getting Config Directory Listing
        String configinstalldirectory = DirectoryUtils.getBackupDirectory();
        String newDirectory = configinstalldirectory + System.getProperty("file.separator") + "config";
        Tab6.restore_combo.removeAll();
        Tab6.restore_combo.add("Please Choose a Config Directory to Restore");
        try {
        	File f = new File(newDirectory);
        	if(f.isFile()){
        		f.delete();
        	}
        	if(!f.isDirectory()){
        		f.mkdir();
        	}
        	
        	File[] files = f.listFiles();
        	
            sortBackupArray(files);
        	
        	for(int i = 0 ; i < files.length ; i++) {
        		String fileName = files[i].getName();
        		if(!files[i].isDirectory() )
        			continue;
        		
        		String dateCurrentTime = View.formatDate(files[i].lastModified());
        		Tab6.restore_combo.add(fileName + " | " + dateCurrentTime);
        		
        	}
        	
        	Tab6.restore_combo.select(0);
        } catch(Exception e) {
        	//Stop process and trace the exception
        	e.printStackTrace();
        }


    }
    
    public static void refreshLists(){
        View.asyncExec(new Runnable (){
        	public void run () 
    		{
    		    getBackupFiles(("ConfigBackup" + View.getPluginInterface().getAzureusVersion()));
    		    getConfigFiles();  
    		}
        });
        

    }
    
    static void clearLists(){
       	Event e = new Event();
    	e.type = SWT.None;
       	Tab6.restore_combo.notifyListeners(SWT.None, e);
       	Tab6.backup_combo.notifyListeners(SWT.None, e);
		
		Tab6.submit_button.setVisible(true);
		Tab6.final_restart_button.setVisible(false);
		Tab6.final_stop_button.setVisible(false);
		Tab6.final_nostop_button.setVisible(false);
		Tab6.alert.setVisible(false);
		
    }

    static void insertSelect(String selectedJar){
        View.tab.setSelection(5);
        Tab6Utils.clearLists();
        
        Event evt = new Event();
        evt.type = SWT.Selection;
        evt.widget = Tab6.restore_combo;

    }
    
    
    
    
//EOF
}
