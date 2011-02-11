/*
 * Created on Apr 8, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.Tab1;
import omschaub.azcvsupdater.main.Tab1Utils;
import omschaub.azcvsupdater.main.Tab1_Subtab_3;
import omschaub.azcvsupdater.main.View;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;

public class DirectoryChanger {
	private static String selectedDir;
	private static String oldDir;
	private static Button submit;
	private static int FILE_COUNT;
    private static boolean no_revert, cancelled;
    private static int escPressed;
    private static ProgressBar pb;
    private static Label progress_label;
    private static Button move_files, copy_files, noMove, cancel;
    private static Composite fp_left_composite;
    private static String[][] fileTimes; 
    private static int COUNTER;
    private static Tree table;
    private static Shell fileShell;
    
    public static void openChanger(){
        no_revert=false;
        cancelled = true;
        COUNTER = 0;
        fileTimes = new String[1000][3];
        
        oldDir=DirectoryUtils.getBackupDirectory();
        final Shell dirShell = new Shell(View.getDisplay());
        
        //Grid Layout 
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        dirShell.setLayout(layout);
        
        //composite for shell
        final Composite dirchanger_composite = new Composite(dirShell,SWT.NULL);
        
        //Grid Layout   
         layout = new GridLayout();
        layout.numColumns = 3;
        dirchanger_composite.setLayout(layout);
        
        //shell title
        dirShell.setText("Change Default Backup Directory");
        
        //Text Line 1
        Label currentLabel = new Label(dirchanger_composite, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        currentLabel.setLayoutData( gridData );
        currentLabel.setText("Current Default Directory:");

        //Text Line 1
        Text currentText = new Text(dirchanger_composite, SWT.READ_ONLY | SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        currentText.setLayoutData( gridData );
        currentText.setText(oldDir);
        
        
        //Text Line 2
        Label newdirLabel = new Label(dirchanger_composite, SWT.NONE);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        newdirLabel.setLayoutData(gridData);
        newdirLabel.setText("New Default Directory: ");
        
        //Text Line 2
        final Text newdirText = new Text(dirchanger_composite, SWT.READ_ONLY |  SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.widthHint=400;
        newdirText.setLayoutData(gridData);
        newdirText.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
        newdirText.setText("None, please choose directory");
        
        //Key Listener for Tim ;)
        currentText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                
                
                switch (e.character){
                    case SWT.ESC: escPressed=1;break;
                    
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
                    //StatusBoxUtils.mainStatusAdd(" Default Directory Change Cancelled",0);
                    dirShell.dispose();
                    return;
                }
            }
        });
        
        newdirText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                
                
                switch (e.character){
                    case SWT.ESC: escPressed=1;break;
                    
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
                    //StatusBoxUtils.mainStatusAdd(" Default Directory Change Cancelled",0);
                    dirShell.dispose();
                    return;
                }
            }
        });
        
        //Button for choosing directory
        Button chooseDir = new Button(dirchanger_composite,SWT.PUSH);
        chooseDir.setText("Choose Directory");
        chooseDir.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                submit.setVisible(false);
                DirectoryDialog dirDialog = new DirectoryDialog(dirShell);
                dirDialog.setText("Please Choose a New Default Directory");
                dirDialog.setFilterPath(DirectoryUtils.getBackupDirectory());
                selectedDir = dirDialog.open();
               
                //need to check if selected dir has files and if so, does it have a comments dir
                if(selectedDir == null){
                    return;
                }
                
                File checkFile = new File(selectedDir);
                String[] checkFile_string = checkFile.list();
                boolean check = false;
                if(checkFile_string != null && checkFile_string.length != 0){
                    //System.out.println("checkFile_string: " + checkFile_string.length);
                    for(int i = 0; i < checkFile_string.length ; i++){
                        //System.out.println("checkFile_string: " + checkFile_string[i]);
                        if(checkFile_string[i].equalsIgnoreCase("config"))
                            check=true;
                    }
                }else check=true;
                
                //Make sure we are in the right place and the user does not select a stupid directory
                //C:/windows, etc.
                if(selectedDir.equalsIgnoreCase(oldDir)){
                    newdirText.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
                    newdirText.setText("You chose the same directory as your old directory, please choose again");
                }else if(selectedDir.startsWith(oldDir + System.getProperty("file.separator"))){
                    newdirText.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
                    newdirText.setText("You cannot choose a directory within the current backup directory");
                }else if(!check){
                    newdirText.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
                    newdirText.setText("Selected directory must either be empty or an old backup directory");
                }else{
                    newdirText.setText(selectedDir);
                    newdirText.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
                  
                    dirchanger_composite.layout(true);
                    submit.setVisible(true);
                    dirShell.layout(true);
                    dirShell.pack();
                }
            }
        });
        
        
        chooseDir.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                
                
                switch (e.character){
                    case SWT.ESC: escPressed=1;break;
                    
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
                   // StatusBoxUtils.mainStatusAdd(" Default Directory Change Cancelled",0);
                    dirShell.dispose();
                    //return;
                }
            }
        });
//      Button for Cancel
        Button cancel = new Button(dirchanger_composite, SWT.PUSH);
        gridData = new GridData(GridData.CENTER);
        gridData.horizontalSpan = 1;
        cancel.setLayoutData( gridData);
        cancel.setText( "Cancel");
        cancel.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                
                //StatusBoxUtils.mainStatusAdd(" Default Directory Change Cancelled",0);
                dirShell.dispose();
                //return;
                }
        });
        
        
//      Button for Submit
        submit = new Button(dirchanger_composite, SWT.PUSH);
        gridData = new GridData(GridData.CENTER);
        gridData.horizontalSpan = 1;
        submit.setLayoutData( gridData);
        submit.setText( "Submit");
        submit.setVisible(false);
        
        submit.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                //pull old directory
                File dir = new File(oldDir);
                //set new directory
                
                
                
                
                
                
                if(dir.isDirectory()){
                    File[] files = dir.listFiles();
                    if(files.length > 0){
                        filesPresent(files);
                     
                                
                    }else{
                        View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",selectedDir);
                        // refresh backup and title
                        //Tab4Utils.backup_loadDirectory();
                    }
                }
                cancelled = false;
                dirShell.dispose();
                //return;
                }
        });
        
        
        //listener for shell disposal
        dirShell.addListener(SWT.Dispose, new Listener() {
            public void handleEvent(Event e) {
                if(!no_revert){
                    /*View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",oldDir);
                    Tab4Utils.backup_loadDirectory();
                    Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
                    */
                    if(cancelled){
                        StatusBoxUtils.mainStatusAdd(" Default Directory Change Cancelled",0);    
                    }
                    
                    
                }
                
            }
        });
        
        
//      open shell
        dirShell.pack();
        
        //Center Shell
        if(View.getDisplay()==null && View.getDisplay().isDisposed())
            return;
        Monitor primary = View.getDisplay().getPrimaryMonitor ();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = dirShell.getBounds ();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y +(bounds.height - rect.height) / 2;
        dirShell.setLocation (x, y);
        
        dirShell.open();
            
    }

private static void filesPresent(final File[] files){
    if(fileShell != null) fileShell = null;
    
    
    fileShell = new Shell(View.getDisplay());
    
    //Grid Layout 
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    fileShell.setLayout(layout);
    
    //composite for shell
    final Composite fp_composite = new Composite(fileShell,SWT.NULL);
    
    //Grid Layout   
     layout = new GridLayout();
    layout.numColumns = 3;
    fp_composite.setLayout(layout);
    
    //shell title
    fileShell.setText("Files are present in old default directory");
    
    //left composite
    fp_left_composite = new Composite(fp_composite,SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 1;
    fp_left_composite.setLayout(layout);
    
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 1;
    fp_left_composite.setLayoutData(gridData);
    
    
    
    //Text1
    Label label_1 = new Label(fp_left_composite, SWT.NULL);
    label_1.setText("The following files/directories are present in the old default directory.");
    
    Label label_2 = new Label(fp_left_composite, SWT.NULL);
    label_2.setText("Please choose which option you would like to perform on these files");
    
    Label label_3 = new Label(fp_left_composite, SWT.NULL);
    label_3.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
    label_3.setText("(Note: Moving/Copying files will stop these files from seeding and remove their torrent file)");
    
    //Buttons

    //Button for Move
    
    
    move_files = new Button(fp_left_composite, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.horizontalSpan = 1;
    move_files.setLayoutData( gridData);
    move_files.setText( "Move the files to the new directory, deleting them from the old one");
    move_files.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
            View.asyncExec(new Runnable ()
                    {
            	public void run () 
                        {                   
                            move_files.setEnabled(false);
                            move_files.setVisible(false);
                            copy_files.setEnabled(false);
                            copy_files.setVisible(false);
                            noMove.setEnabled(false);
                            noMove.setVisible(false);
                            cancel.setEnabled(false);
                            cancel.setVisible(false);
                            //set new directory before we start the move
                            View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",selectedDir);
                            Thread move_thread = new Thread() 
                            {
                                public void run() 
                                {
                                    
                                    copyFiles(oldDir,selectedDir);
                                    File olddir_to_die = new File(oldDir);
                                    redateFiles();
                                    
                                    //delete the directory
                                    deleteDir(olddir_to_die);
                                    
                                    // Make this a recheck to see if that helps
                                    try{
                                        File still_there = new File(oldDir);
                                        //System.out.println(still_there.getPath() + " : " + still_there.getName() + " : " + still_there.exists() + " : " + still_there.isFile() + " : " + still_there.isDirectory());
                                        if(still_there.isDirectory()){
                                            View.getDisplay().asyncExec(new Runnable (){
                                            	public void run () {
                                                    StatusBoxUtils.mainStatusAdd(" Successfully moved all files to new backup directory",0);
                                                    StatusBoxUtils.mainStatusAdd(" Please check that no files are being used by the system in the old directory, then manually delete it",2);
                                                    StatusBoxUtils.mainStatusAdd(" The files were copied to the new directory successfully, but deleting the old directory failed",2);
                                                    Shell messageshell = new Shell(View.getDisplay());
                                                    MessageBox messageBox = new MessageBox(messageshell, SWT.ICON_ERROR | SWT.OK);
                                                    messageBox.setText("Problems deleting old directory");
                                                    messageBox.setMessage("The files were copied to the new directory successfully, but deleting the old directory failed." +
                                                            "  Please check that no files are being used by the system in the old directory, then manually delete it.");
                                                    messageBox.open();       
                                                }
                                            });
                                        }else{
                                            StatusBoxUtils.mainStatusAdd(" Successfully moved all files to new backup directory",0);    
                                        }
                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                    
                                                                        
                                    View.asyncExec(new Runnable (){
                                        public void run () {
                                            Tab1_Subtab_3.backupDirectory.setText(DirectoryUtils.getBackupDirectory());
                                            Tab1_Subtab_3.backupDirectory.getParent().layout();
                                            Tab1.mini_comp2.layout();
                                            Tab1.changer.layout();
                                            //Tab1.changeBackupDir.setToolTipText("Change the current default backup directory, currently\n" + DirectoryUtils.getBackupDirectory());
                                            //Tab4Utils.backup_loadDirectory();
                                            Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));                
                                        }
                                    });
                                }
                            };
                            move_thread.setDaemon(true);
                            move_thread.start();
                            
                            
                            
                            
                            no_revert=true;
                            
                        }
                    });
            
            
            
            }
    });
    
    //  Button for copy
    copy_files = new Button(fp_left_composite, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.horizontalSpan = 1;
    copy_files.setLayoutData( gridData);
    copy_files.setText( "Make a copy of the files in the newly selected directory");
    copy_files.addListener(SWT.Selection, new Listener() {
    	public void handleEvent(Event e) {
            move_files.setEnabled(false);
            move_files.setVisible(false);
            copy_files.setEnabled(false);
            copy_files.setVisible(false);
            noMove.setEnabled(false);
            noMove.setVisible(false);
            cancel.setEnabled(false);
            cancel.setVisible(false);
            
            //set directory before we copy files
            View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",selectedDir);
           
            Thread copy_thread = new Thread() 
            {
                public void run() 
                {
                    copyFiles(oldDir,selectedDir);
                    redateFiles();
                    StatusBoxUtils.mainStatusAdd(" Successfully copied all files to new backup directory",0);
                    
                    View.asyncExec(new Runnable (){
                    	public void run () {
                            Tab1_Subtab_3.backupDirectory.setText(DirectoryUtils.getBackupDirectory());
                            Tab1_Subtab_3.backupDirectory.getParent().layout();
                            Tab1.mini_comp2.layout();
                            Tab1.changer.layout();
                            //Tab1.changeBackupDir.setToolTipText("Change the current default backup directory, currently\n" + DirectoryUtils.getBackupDirectory());
                            //Tab4Utils.backup_loadDirectory();
                            Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));                
                        }
                    });
                }
            };
            copy_thread.setDaemon(true);
            copy_thread.start();
            no_revert=true;
            //fileShell.dispose();
            //return;
            }
    });
    //  Button for noMove
    noMove = new Button(fp_left_composite, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.horizontalSpan = 1;
    noMove.setLayoutData( gridData);
    noMove.setText( "Do not copy/move files");
    noMove.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
            
            
            try{
                File oldDir_file = new File(oldDir);
                String[] files_present = oldDir_file.list();
                if(files != null){
                    for(int i = 0; i < files_present.length; i++){
                        remove_any_downloads(files_present[i]);
                    }    
                }
            }catch(Exception e1){
                
            }
                
            
            if (files != null) {
	            for(int i = 0; i < files.length; i++){
	                remove_any_downloads(files[i].getName());
	            }
            }
            StatusBoxUtils.mainStatusAdd(" Successfully changed backup directory",0);
            no_revert=true;
            View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",selectedDir);
            Tab1_Subtab_3.backupDirectory.setText(DirectoryUtils.getBackupDirectory());
            Tab1_Subtab_3.backupDirectory.getParent().layout();
            Tab1.mini_comp2.layout();
            Tab1.changer.layout();
            //Tab1.changeBackupDir.setToolTipText("Change the current default backup directory, currently\n" + DirectoryUtils.getBackupDirectory());
            //Tab4Utils.backup_loadDirectory();
            Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
            fileShell.dispose();
            return;
            }
    });
    
    //  Button for cancel
    cancel = new Button(fp_left_composite, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.horizontalSpan = 1;
    cancel.setLayoutData( gridData);
    cancel.setText( "Cancel - Discarding all changes");
    cancel.addListener(SWT.Selection, new Listener() {
    	public void handleEvent(Event e) {
            //View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",oldDir);
            //Tab4Utils.backup_loadDirectory();
            //Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
            fileShell.dispose();
            
            //return;
            }
    });
    

    
    
    progress_label = new Label(fp_left_composite, SWT.NULL);
    gridData=new GridData(GridData.FILL_HORIZONTAL);
    progress_label.setLayoutData(gridData);
    
    progress_label.setText("Progress: ");
    progress_label.setVisible(false);
    
    
    pb = new ProgressBar(fp_left_composite, SWT.NULL);
    gridData=new GridData(GridData.FILL_HORIZONTAL);
    pb.setLayoutData(gridData);
    pb.setMinimum(0);
    File pb_count_file = new File(oldDir);
    FILE_COUNT=0;
    countFiles(pb_count_file);
    //System.out.println(FILE_COUNT);
    pb.setMaximum(FILE_COUNT);
    pb.setSelection(0);
    pb.setVisible(false);
    
//  right composite
    Composite fp_right_composite = new Composite(fp_composite,SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 1;
    fp_right_composite.setLayout(layout);
    
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    fp_right_composite.setLayoutData(gridData);
    
    //TableTree
    table = new Tree(fp_right_composite,SWT.SINGLE |SWT.BORDER |SWT.V_SCROLL);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 1;
    table.setLayoutData(gridData);
    
    //Fill List
    for(int i = 0; i < files.length ; i++){
        TreeItem item = new TreeItem(table,SWT.NULL);
        item.setText(files[i].getName());
        if(files[i].isDirectory()){
          item.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
          File[] new_files = files[i].listFiles();
          fillList(new_files,item);
        }
    }
    
    //key listener for Tim :)
    move_files.addKeyListener(new KeyListener() {
    	public void keyPressed(KeyEvent e) {
            
            
            switch (e.character){
                case SWT.ESC: escPressed=1;break;
                
            }
            if (escPressed == 1){
              
            }
        }
        public void keyReleased (KeyEvent e) {
            if (escPressed == 1){
            //shell.close();
                escPressed = 0;
                
                no_revert=false;
                fileShell.dispose();
                return;
            }
        }
    });
    
    copy_files.addKeyListener(new KeyListener() {
        public void keyPressed(KeyEvent e) {
            
            
            switch (e.character){
                case SWT.ESC: escPressed=1;break;
                
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
                //View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",oldDir);
                //Tab4Utils.backup_loadDirectory();
                //Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
                no_revert=false;
                fileShell.dispose();
                return;
            }
        }
    });
    
    noMove.addKeyListener(new KeyListener() {
    	public void keyPressed(KeyEvent e) {
            
            
            switch (e.character){
                case SWT.ESC: escPressed=1;break;
                
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
                
                no_revert=false;
                fileShell.dispose();
                return;
            }
        }
    });
    
    cancel.addKeyListener(new KeyListener() {
    	public void keyPressed(KeyEvent e) {
            
            
            switch (e.character){
                case SWT.ESC: escPressed=1;break;
                
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
                
                no_revert=false;
                fileShell.dispose();
                return;
            }
        }
    });
    
    
    table.addKeyListener(new KeyListener() {
    	public void keyPressed(KeyEvent e) {
            
            
            switch (e.character){
                case SWT.ESC: escPressed=1;break;
                
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
                
                no_revert=false;
                fileShell.dispose();
                return;
            }
        }
    });
    

    //listener for shell disposal
    fileShell.addListener(SWT.Dispose, new Listener() {
        public void handleEvent(Event e) {
            if(!no_revert){
                View.getPluginInterface().getPluginconfig().setPluginParameter("backup_directory",oldDir);
                //Tab4Utils.backup_loadDirectory();
                //Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
                StatusBoxUtils.mainStatusAdd(" Default Directory Change Cancelled",0);
            }
            
        }
    });
    
    
    
    
    
    //  open shell
    

    fileShell.pack();
    
    //Center Shell
    if(View.getDisplay()==null && View.getDisplay().isDisposed())
        return;
    Monitor primary = View.getDisplay().getPrimaryMonitor ();
    Rectangle bounds = primary.getBounds ();
    Rectangle rect = fileShell.getBounds ();
    int x = bounds.x + (bounds.width - rect.width) / 2;
    int y = bounds.y +(bounds.height - rect.height) / 2;
    fileShell.setLocation (x, y);
    

    fileShell.open();                
     
    
}



private static void copyFiles(final String oldDir_copy, final String newDir_copy){


			if (newDir_copy.startsWith(oldDir_copy))
			{
				return;
			}

            File oldDir_file = new File(oldDir_copy);
            String[] files = oldDir_file.list();
            try{
                if(files == null){
                    return;
                }
                
                for(int i = 0; i < files.length; i++){
                    remove_any_downloads(files[i]);
                }
                
                for (int i = 0; i < files.length; i++) {
                    
                        String destinationFile = newDir_copy + System.getProperty("file.separator") + files[i];
                        String sourceFile = oldDir_copy + System.getProperty("file.separator") + files[i];
                        File f = new File(sourceFile);
                        long f_date = f.lastModified();
                        if(f.isDirectory()){
                            
                                File newdir_to_make = new File(newDir_copy + System.getProperty("file.separator") + files[i]);
                                newdir_to_make.mkdir();
                                newdir_to_make.setLastModified(f_date);
                                //System.out.println(f.getPath() + "  :  "  + newdir_to_make.getPath());
                                copyFiles(f.getPath(),newdir_to_make.getPath());
                                newdir_to_make.setLastModified(f_date);
                        }
                        else{
                        
                        FileInputStream infile = new FileInputStream(sourceFile);
                        BufferedInputStream in = new BufferedInputStream(infile);
                        FileOutputStream outfile = new FileOutputStream(destinationFile);
                        BufferedOutputStream out = new BufferedOutputStream(outfile);       
                        int c;
                        while ((c = in.read()) != -1)
                        {
                            out.write(c);
                        }
                        
                        in.close();
                        out.flush();
                        out.close();
                        
                        infile.close();
                        outfile.close();    
                        File dest = new File(destinationFile);
                        dest.setLastModified(f_date);
                        //remove_any_downloads(files[i]);
                        
                        //progress_label.setText(files[i] + " to " + newDir_copy);
                        View.asyncExec(new Runnable ()
                                {
                        	public void run () 
                                    {  
                                        progress_label.setVisible(true);
                                        pb.setVisible(true);
                                        pb.setSelection((pb.getSelection()+1));
                                        //System.out.println(pb.getSelection() + "  :  " + FILE_COUNT);
                                        if(pb.getSelection() == FILE_COUNT){
                                            if(fileShell != null && !fileShell.isDisposed())
                                                fileShell.dispose();
                                        }
                                    }
                                });
                        
                       
                        // StatusBoxUtils.mainStatusAdd(" Copied file " + files[i] + " to new backup directory: " + newDir_copy,0);   
                     }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }   

            
}







private static boolean deleteDir(File dir) {
    // to see if this directory is actually a symbolic link to a directory,
    // we want to get its canonical path - that is, we follow the link to
    // the file it's actually linked to
    File candir;
    try {
        candir = dir.getCanonicalFile();
    } catch (IOException e) {
        return false;
    }

    // a symbolic link has a different canonical path than its actual path,
    // unless it's a link to itself
    if (!candir.equals(dir.getAbsoluteFile())) {
        // this file is a symbolic link, and there's no reason for us to
        // follow it, because then we might be deleting something outside of
        // the directory we were told to delete
        return false;
    }

    // now we go through all of the files and subdirectories in the
    // directory and delete them one by one
    File[] files = candir.listFiles();
    if (files != null) {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            // in case this directory is actually a symbolic link, or it's
            // empty, we want to try to delete the link before we try
            // anything
            boolean deleted = file.delete();
            if (!deleted) {
                // deleting the file failed, so maybe it's a non-empty
                // directory
                if (file.isDirectory()) deleteDir(file);

                // otherwise, there's nothing else we can do
            }
        }
    }

    // now that we tried to clear the directory out, we can try to delete it
    // again
    return dir.delete();  
}


private static void countFiles(File dir) {
    
    
    File candir;
    try {
        candir = dir.getCanonicalFile();
    } catch (IOException e) {
        return;
    }
    if (!candir.equals(dir.getAbsoluteFile())) {
        return;
    }

    // now we go through all of the files and subdirectories in the
    // directory and count them one by one
    File[] files = candir.listFiles();
    if (files != null) {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            //System.out.println(file.getPath() + "  :   "  + Long.toString(file.lastModified()));
            fileTimes[COUNTER][0] = file.getPath();
            fileTimes[COUNTER][1] = Long.toString(file.lastModified());
            COUNTER++;
            if(file.isFile())
                FILE_COUNT++;
            
            
            if (file.isDirectory()) {
                //is a dir so we need to count the files in the dir
                if (file.isDirectory()) countFiles(file);


            }
        }
    }

  
}

private static void redateFiles(){
    try{
        for(int i = 0 ; i < COUNTER ; i++){
            String dir = fileTimes[i][0];
            dir = selectedDir + dir.substring(oldDir.length(), dir.length());
            //System.out.println(dir);
            File file = new File(dir);
            if(file.isFile() || file.isDirectory()){
                file.setLastModified(Long.parseLong(fileTimes[i][1]));
              //  System.out.println(file.getName() + " set to " + Long.parseLong(fileTimes[i][1]));
            }
        }
    }catch(Exception e){
        e.printStackTrace();
    }
}

private static void fillList(File[] files, TreeItem parent){
    
    try{
        for(int i = 0; i < files.length ; i++){
            TreeItem item = new TreeItem(parent,SWT.NULL);
            item.setText(files[i].getName());
            if(files[i].isDirectory()){
              item.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
              File[] new_files = files[i].listFiles();
              fillList(new_files,item);
            }
        }
    }catch(Exception e){
        e.printStackTrace();
    }
    
    

    

}



private static void remove_any_downloads(final String name){
    final DownloadManager dm;
    dm = View.getPluginInterface().getDownloadManager();
    
        DownloadManagerListener dml = new DownloadManagerListener()
        {
            public void
            downloadAdded(
            final Download  download )
            {
                                
                if(download.getName().equals(name))
                {
    
                    try 
                    {
                        if(download.getState() != Download.ST_STOPPED){
                            download.stop();
                        }
                            
                        download.remove(true,false);
                        dm.removeListener(this);
                        
                    } catch (DownloadException e) {
                        e.printStackTrace();
                    } catch (DownloadRemovalVetoException e1) {
                        e1.printStackTrace();
                    }
                }
                dm.removeListener(this);
            }
                public void
                downloadRemoved(
                    Download    download )
                {
                    
            }
        };
    

        dm.addListener(dml);
    
}
}//EOF
