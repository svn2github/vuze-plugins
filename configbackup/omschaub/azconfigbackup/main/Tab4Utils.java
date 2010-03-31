/*
 * Created on Feb 6, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.main;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import omschaub.azconfigbackup.utilities.DirectoryUtils;
import omschaub.azconfigbackup.utilities.ShellUtils;
import omschaub.azconfigbackup.utilities.imagerepository.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * Tab4 Utilites 
 */
public class Tab4Utils {

    static int escPressed, crPressed;
    static String destinationDirEnding;
    static private boolean backupButtonStatus;
    
    static boolean makeBackup(File dir, String sourcedir, String destinationdir, String destinationDirEnding, boolean autoInitialized) {
        boolean success = false;
        String[] files;
    	files = dir.list();
    	File checkdir = new File(destinationdir + System.getProperty("file.separator") + destinationDirEnding);
    	if (!checkdir.isDirectory()) {
    			checkdir.mkdir();
    		};
    	Date date = new Date();
    	long msec = date.getTime();
    	checkdir.setLastModified(msec);

    	try{
             for (int i = 0; i < files.length; i++) {
                    File f = new File (dir, files[i]);
                    File g = new File (files[i]);
                        
                        if (f.isDirectory()){
                        
                        }else if(f.getName().endsWith("saving")){
                            //StatusBoxUtils.mainStatusAdd(" Skipped " + f.getName(),0);
                            //Skipping any .saving files here
                        }
                        else {
                                                        
                            if(f.canRead()){
                                String destinationFile = checkdir + System.getProperty("file.separator") + g;
                                String sourceFile = sourcedir + System.getProperty("file.separator") + g;
                                FileInputStream infile = new FileInputStream(sourceFile);
                                FileOutputStream outfile = new FileOutputStream(destinationFile);
                                        
                                byte[] buf = new byte[65536];
                                int buf_size = 0;
                                while (true) {
                                    buf_size = infile.read(buf);
                                    if (buf_size == -1) {break;}
                                    outfile.write(buf, 0, buf_size);
                                }
                                
                                infile.close();
                                outfile.close();    
                             }else{
                                 System.out.println(f.getName() + " is LOCKED!");  
                                 while(!f.canRead()){
                                   //spin wildly here    
                                   }
                                   String destinationFile = checkdir + System.getProperty("file.separator") + g;
                                   String sourceFile = sourcedir + System.getProperty("file.separator") + g;
                                   FileInputStream infile = new FileInputStream(sourceFile);
                                   FileOutputStream outfile = new FileOutputStream(destinationFile);
                                           
                                   byte[] buf = new byte[65536];
                                   int buf_size = 0;
                                   while (true) {
                                       buf_size = infile.read(buf);
                                       if (buf_size == -1) {break;}
                                       outfile.write(buf, 0, buf_size);
                                   }
                                   
                                   infile.close();
                                   outfile.close();    
                                   }
                               }
                            
                            }
             success = true;
        } catch(Exception e) {
    		success = false;
            e.printStackTrace();
    	}	
    	if(autoInitialized){
    	    Display display = View.getDisplay();
            if(display != null && !display.isDisposed()){
                View.getDisplay().syncExec( new Runnable (){
                    public void run() {
                        //backup_loadDirectory();
                        Tab4.redrawBackupTable();
                    }
                });
            }
            
            return success;
        }else{
            View.getDisplay().syncExec( new Runnable (){
                public void run() {
                    
                        StatusBoxUtils.mainStatusAdd(" Backup Complete",1);
                        View.getPluginInterface().getPluginconfig().setPluginParameter
                        (
                                "Azconfigbackup_last_backup", 
                                getCurrentTime()
                        );
                    
                        Tab4.lastBackupTime = View.getPluginInterface().getPluginconfig().getPluginStringParameter("Azconfigbackup_last_backup");
                        
                        if(Tab4.lastbackupValue != null || !Tab4.lastbackupValue.isDisposed()){
                            Tab4.lastbackupValue.setText("Last backup: " + Tab4.lastBackupTime);
                        }
                        //backup_loadDirectory();
                        Tab4.redrawBackupTable();
                        
                        
                        Tab6Utils.refreshLists();
                        
                }
            
            });
            return success;   
        }
    	
    	
    }
    
    static void deleteMultiDirs(String[] dirs_to_die){
        String file_names = "";
        File[] dirFiles = new File[dirs_to_die.length];
        for(int i = 0; i < dirs_to_die.length ; i++){
            File isDir = new File(dirs_to_die[i]);
            if(!isDir.isDirectory())
                return;
            
            file_names = file_names + "\n" + isDir.getName();
        }
        
        Shell shell = new Shell();
    	MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
    	messageBox.setText("Delete Confirmation");
    	messageBox.setMessage("Are you sure you want to delete the following directories?" + file_names + "\nand all of their configuration files?");
    	int response = messageBox.open();
    	switch (response){
    		case SWT.YES:
    	try{
    		for(int j = 0 ; j < dirs_to_die.length ; j++){	
    		    dirFiles[j] = new File(dirs_to_die[j]);
    		    File[] dirListing = dirFiles[j].listFiles();
    		    for(int i = 0; i < dirListing.length ; i ++) {
    				if(dirListing[i].isFile())
    				    dirListing[i].delete();
    				//StatusBoxUtils.mainStatusAdd(" File " + dirListing[i] + " deleted",0);
    			}
    			dirFiles[j].delete();
    			StatusBoxUtils.mainStatusAdd(" Directory " + dirFiles[j].getPath() + " deleted",0);
    		}	
    		//backup_loadDirectory();
            Tab4.redrawBackupTable();
    	}catch (Exception e){
    		e.printStackTrace() ;
    	}
        if(View.getDisplay() == null || View.getDisplay().isDisposed())
            return;
    	View.getDisplay().asyncExec(new Runnable (){
    	    public void run (){
    	        if(Tab4.backupTable != null && !Tab4.backupTable.isDisposed()){
                    Tab4.backupTable.deselectAll();
    		    }  
    	    }
    	});	
    			
    			shell.dispose();
    			break;
    		case SWT.NO:
    			//loadDirectory(type);
    		    if(View.getDisplay() == null || View.getDisplay().isDisposed())
    	            return;
    		    View.getDisplay().asyncExec(new Runnable (){
    				public void run () {
    				    StatusBoxUtils.mainStatusAdd(" Directory Delete Cancelled",0);
    				    if(Tab4.backupTable != null && !Tab4.backupTable.isDisposed()){
                            Tab4.backupTable.deselectAll();
    					}
    				    if(Tab4.deleteBackup != null && !Tab4.deleteBackup.isDisposed()){
                            Tab4.deleteBackup.setEnabled(false);
    				    }
    				}
    					
    			});
    		    
    			shell.dispose();
    			break;
    	}
    }


    static void deleteSelectedDir(String dirtodie) {
    	File timetodie = new File(dirtodie);
    	
    	if (!timetodie.isDirectory())
    		return;
    	File[] dirListing = timetodie.listFiles();
    	
    		
    	
    	
    	
    	Shell shell = new Shell();
    	MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
    	messageBox.setText("Delete Confirmation");
    	messageBox.setMessage("Are you sure you want to delete \n" + timetodie.getName() + "\nand all of its files?");
    	int response = messageBox.open();
    	switch (response){
    		case SWT.YES:
    	try{
    			for(int i = 0; i < dirListing.length ; i ++) {
    				if(dirListing[i].isFile())
    				    dirListing[i].delete();
    				//StatusBoxUtils.mainStatusAdd(" File " + dirListing[i] + " deleted",0);
    			}
    			if(timetodie.isDirectory())
    			    timetodie.delete();
    			StatusBoxUtils.mainStatusAdd(" Directory " + timetodie + " deleted",0);
    			
    			//backup_loadDirectory();
                Tab4.redrawBackupTable();
                
                Tab4.deleteBackup.setEnabled(false);
    	}catch (Exception e){
    		e.printStackTrace() ;
    	}
        if(View.getDisplay() == null || View.getDisplay().isDisposed())
            return;
    	View.getDisplay().asyncExec(new Runnable (){
    	    public void run (){
    	        //StatusBoxUtils.mainStatusAdd(" Directory Deleted",0);
    		    if(Tab4.backupTable != null && !Tab4.backupTable.isDisposed()){
                    Tab4.backupTable.deselectAll();
    		    }  
    	    }
    	});	
    			
    			shell.dispose();
    			break;
    		case SWT.NO:
    			//loadDirectory(type);
    		    if(View.getDisplay() == null || View.getDisplay().isDisposed())
    	            return;
    		    View.getDisplay().asyncExec(new Runnable (){
    				public void run () {
    				    StatusBoxUtils.mainStatusAdd(" Directory Delete Cancelled",0);
    				    if(Tab4.backupTable != null && !Tab4.backupTable.isDisposed()){
                            Tab4.backupTable.deselectAll();
    					}
    				    if(Tab4.deleteBackup != null && !Tab4.deleteBackup.isDisposed()){
                            Tab4.deleteBackup.setEnabled(false);
    				    }
    				}
    					
    			});
    		    
    			shell.dispose();
    			break;
    	}
    	
    	
    }

    public static boolean backupButtonRun(int autoInitialized)
    {
    	boolean success = false;
        final String pluginDir2 =  View.getPluginInterface().getUtilities().getAzureusUserDir();	
    	  	
    	File directory = new File(pluginDir2);
    	if (!directory.isDirectory()) {
    		StatusBoxUtils.mainStatusAdd("ERROR! File is a directory!?!",2);
    	return false;
    	}
    	else {
    		
    		String backupinstalldirectory = DirectoryUtils.getBackupDirectory()+ System.getProperty("file.separator") + "config";
    	try {
    		if (autoInitialized == 0){
    			directoryQuestion(directory,pluginDir2);
    			
    		} else {
                 if(makeBackup(directory,pluginDir2,backupinstalldirectory,("ConfigAutoBackup" + View.getPluginInterface().getAzureusVersion() + "-" + getCurrentDate()),true)){
                     success = true;
                 }    
            }
    		
            View.getPluginInterface().getPluginconfig().setPluginParameter("Azconfigbackup_last_backup", getCurrentTime());
    	} catch (Exception e1) {
    	    success = false;
            e1.printStackTrace();
    	}
    }
    	
    	return success;
    }

    
    
    
    static boolean directoryQuestion(final File directory, final String pluginDir2){
    	backupButtonStatus = false;
    	destinationDirEnding =  ("ConfigBackup" + View.getPluginInterface().getAzureusVersion());
    	
    	// Shell Initialize
    	if(View.getDisplay()==null && View.getDisplay().isDisposed())
    	    return false;
    	final Shell shell = new Shell(View.getDisplay(),SWT.DIALOG_TRIM);
    	
    	shell.setImage(ImageRepository.getImage("folder"));
    	//Grid Layout	
    	GridLayout layout = new GridLayout();
    	layout.numColumns = 1;
    	shell.setLayout(layout);
    	
    	//composite for shell
    	Composite backup_composite = new Composite(shell,SWT.NULL);
    	
    	//Grid Layout	
    	 layout = new GridLayout();
    	layout.numColumns = 3;
    	backup_composite.setLayout(layout);
    	
    	//shell title
    	shell.setText("Directory Name");
    	

    	
    	//Text Line 1
    	Label nameLabel = new Label(backup_composite, SWT.NONE);
    	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    	gridData.horizontalSpan = 3;
    	nameLabel.setLayoutData( gridData );
    	nameLabel.setText("Input Backup Configuration Directory Name");
    	
    	

    	//Input field
    	final Combo  line1 = new Combo(backup_composite,SWT.BORDER);
    	gridData = new GridData(GridData.FILL_HORIZONTAL);
    	gridData.horizontalSpan = 3;
    	line1.setLayoutData( gridData);
    	//line1.setEditable( true );
    	

    	
    	//bring in suggested name and directory listing
    	line1.add(destinationDirEnding);
    	String configInstallDirectory = DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator") + "config";
    	try {
        	File f = new File(configInstallDirectory);
        	File[] files = f.listFiles();
        	for(int i = 0 ; i < files.length ; i++) {
        		String fileName = files[i].getName();
        		if(files[i].isDirectory() )
        		{
        			if(!fileName.equalsIgnoreCase(destinationDirEnding)){
        				line1.add(fileName);
        			}
        		
        		
        		}
        	}
        	
        	
        } catch(Exception e) {
        	//Stop process and trace the exception
        	e.printStackTrace();
        }
        line1.select(0);
    	

    	//Button for Commit Changes
    	Button commit = new Button(backup_composite, SWT.PUSH);
    	gridData = new GridData(GridData.CENTER);
    	gridData.horizontalSpan = 1;
    	commit.setLayoutData( gridData);
    	commit.setText( "Accept Directory Name");
    	commit.addListener(SWT.Selection, new Listener() {
    		public void handleEvent(Event e) {
    			destinationDirEnding = line1.getText();
    			
    			if (line1.getText() == null){
    				line1.setText("Cannot Be Empty");
    				
    			}
    			else {
    			
    			//shell.close() ;
    			shell.dispose();
    			String backupinstalldirectory = DirectoryUtils.getBackupDirectory()+ System.getProperty("file.separator") + "config";
                backupButtonStatus =  Tab4Utils.makeBackup(directory,pluginDir2,backupinstalldirectory,destinationDirEnding,false);
                               
                
                String[] files = directory.list();
                String[] file_to_zip = new String[files.length];
                
                try{
                    for (int i = 0; i < files.length; i++){
                        File tempFile = new File(directory.getAbsolutePath() + System.getProperty("file.separator") + files[i]);
                        if(!tempFile.isDirectory() && !tempFile.getName().endsWith(".bak") &&
                                !tempFile.getName().endsWith(".saving")){
                            file_to_zip[i] = directory.getAbsolutePath() + System.getProperty("file.separator")+ files[i];
                        }
                    }    
                }catch(Exception f){
                    f.printStackTrace();
                }
                
                
                
                /*ZipUtils zipU = new ZipUtils();
                zipU.makeBackupToZip(file_to_zip ,backupinstalldirectory,"test.zip");*/
                //System.out.println("Manual Run of Backup using directoryQuestion is " + backupButtonStatus);
                View.getPluginInterface().getPluginconfig().setPluginParameter("Azconfigbackup_last_backup",
                        getCurrentTime());
    			}
    			
    			
    		}
    	});
    	
//    	Button for Cancel
    	Button cancel = new Button(backup_composite, SWT.PUSH);
    	gridData = new GridData(GridData.CENTER);
    	gridData.horizontalSpan = 2;
    	cancel.setLayoutData( gridData);
    	cancel.setText( "Cancel");
    	cancel.addListener(SWT.Selection, new Listener() {
    		public void handleEvent(Event e) {
    			destinationDirEnding = null;
    			//shell.close() ;
    			if(View.getDisplay()==null && View.getDisplay().isDisposed())
    	    	    return;
    			View.getDisplay().asyncExec(new Runnable (){
    				public void run () {
    					
    					StatusBoxUtils.mainStatusAdd(" Backup Cancelled",0);
    					
    				}
    			
    			});
    			
    			shell.dispose();
    			return;
    			}
    	});
    	
    	ShellUtils.centerShellandOpen(shell);
    	
    	    	
    	//Listener for shell

    	line1.addKeyListener(new KeyListener() {
    		public void keyPressed(KeyEvent e) {
    		    
    			
    			switch (e.character){
    				case SWT.ESC: escPressed=1;break;
    				case SWT.CR: crPressed=1;break;
    			}
    			if (escPressed == 1){
    				
    				//escPressed = 0;
    				//shell.dispose() ;
    			}
    		}
    		public void keyReleased (KeyEvent e) {
    			if (escPressed == 1){
    			//shell.close();
    				escPressed = 0;
    				//System.out.println("Key Pressed");
    				shell.dispose();
    			}
    			if (crPressed == 1)
    			{
    			    crPressed = 0;
    			    destinationDirEnding = line1.getText();
    				
    				if (line1.getText() == null)
    				{
    				    line1.setText("Cannot Be Empty");
    				}
    				else 
    				{
    				    shell.dispose();
    				    String backupinstalldirectory = DirectoryUtils.getBackupDirectory()+ System.getProperty("file.separator") + "config";
                        backupButtonStatus = makeBackup(directory,pluginDir2,backupinstalldirectory,destinationDirEnding,false);
                        System.out.println("backupButtonStatus Run " + backupButtonStatus);
                        View.getPluginInterface().getPluginconfig().setPluginParameter("Azconfigbackup_last_backup",
                                getCurrentTime());
    				}
    			}
    		}
    	});

    	return backupButtonStatus;
    }

    public static String getCurrentTime(){
    	return View.formatDate(View.getPluginInterface().getUtilities().getCurrentSystemTime());
    }

    public static String getCurrentDate() {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
    	return sdf.format(new Date());
    }


}
