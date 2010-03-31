/*
 * Created on Feb 6, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.main;


import java.io.File;


import omschaub.azconfigbackup.utilities.ButtonStatus;
import omschaub.azconfigbackup.utilities.ColorUtilities;
import omschaub.azconfigbackup.utilities.DirectoryUtils;
import omschaub.azconfigbackup.utilities.imagerepository.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;


/**
 * Tab4 Graphics Initialization
 */
public class Tab4 {
      
    static ToolBar backup_tb;
    public static ToolItem refreshBackup, backup;
    static ToolItem deleteBackup;   
    
    public static Table backupTable;
    
    static Label configdirValue;
    static Label lastbackupValue;
    
    static File[] backupFiles;
    
    static boolean directoryNamer_sort = true;
    static boolean directoryTime_sort = true;
    
    static String lastBackupTime;
    
    static void open(){
        
        
        //Last Backup Time
		lastBackupTime = View.getPluginInterface().getPluginconfig().getPluginStringParameter("Azconfigbackup_last_backup","Never! Please backup now");

        
        //      new backupGroup for Current system information -- on Tab4
		
		Group backupGroup = new Group(View.composite_for_tab4, SWT.NULL);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan  = 3;
		backupGroup.setLayoutData(gridData);
		GridLayout backupGroup_layout = new GridLayout();
		backupGroup_layout.numColumns = 3;
        //backupGroup_layout.marginHeight = 0;
        //backupGroup_layout.marginWidth = 0;
		backupGroup.setLayout(backupGroup_layout);

		backupGroup.setText("Backup Configuration Files");

	    
        Composite comp1_backup = new Composite(backupGroup,SWT.NULL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 1;
        //gridData.widthHint=220;
        comp1_backup.setLayoutData(gridData);
        backupGroup_layout = new GridLayout();
        backupGroup_layout.marginHeight = 0;
        //backupGroup_layout.marginWidth = 0;
        backupGroup_layout.numColumns = 2;
        comp1_backup.setLayout(backupGroup_layout);
        
        Label backupSeparator = new Label(backupGroup,SWT.SEPARATOR);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING  );
        gridData.heightHint =50;
        gridData.verticalSpan=1;
        backupSeparator.setLayoutData(gridData);
        
        Composite comp2_backup = new Composite(backupGroup,SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        comp2_backup.setLayoutData(gridData);
        backupGroup_layout = new GridLayout();
        backupGroup_layout.marginHeight = 0;
       // backupGroup_layout.marginWidth = 0;
        backupGroup_layout.verticalSpacing = 8;
        backupGroup_layout.numColumns = 1;
        comp2_backup.setLayout(backupGroup_layout);
	    

	   	
//		Label to show last backup time  -- in backupGroup    
	    
        lastBackupTime = View.getPluginInterface().getPluginconfig().getPluginStringParameter("Azconfigbackup_last_backup","Never! Please backup now");
	    
  
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING );
		gridData.horizontalSpan=2;
		lastbackupValue = new Label(comp1_backup,SWT.NULL);
		lastbackupValue.setLayoutData(gridData);
		lastbackupValue.setText("Last backup: " + lastBackupTime);


        
		//AutoBackup composite for label and check button
		Composite autoBackupComp = new Composite(comp1_backup, SWT.NONE);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.horizontalSpan =1;
		gridData.horizontalAlignment = GridData.FILL;
		autoBackupComp.setLayoutData(gridData);
		GridLayout autoBackupComp_layout = new GridLayout();
		autoBackupComp_layout.marginHeight = 0;
		autoBackupComp_layout.marginWidth = 0;
		autoBackupComp_layout.numColumns = 2;
		autoBackupComp_layout.makeColumnsEqualWidth = false;
		autoBackupComp.setLayout(autoBackupComp_layout);
		
        comp1_backup.pack();
		
    	//		 Label to show AutoBackup status
		Label backupStatus = new Label(autoBackupComp, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING );
		gridData.horizontalSpan = 1;
		backupStatus.setLayoutData(gridData);
		backupStatus.setText("Auto Backup Config Files:");
		backupStatus.setToolTipText("On automatic restart, also automatically back up config files");
        
        
		//Check button to change status
		final Button autoBackup_Check = new Button(autoBackupComp,SWT.CHECK);
		autoBackup_Check.setSelection(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoBackupConfig"));
		//autoBackup_Check.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoBackupConfig"))?"On  ":"Off");
		autoBackup_Check.pack();
		autoBackup_Check.setToolTipText("On automatic restart, also automatically back up config files");
		
		
		autoBackup_Check.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
			    autoBackup_Check.setSelection((autoBackup_Check.getSelection())?true:false);
			    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoBackupConfig",autoBackup_Check.getSelection());
			    //autoBackup_Check.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoBackupConfig"))?"On  ":"Off");
			    
			}
		});
		

     //  place user directory here -- In backupGroup
        configdirValue = new Label(comp2_backup,SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        configdirValue.setLayoutData(gridData);
        final String pluginDir =  View.getPluginInterface().getUtilities().getAzureusUserDir();
        configdirValue.setText("User Configuration Directory : " + pluginDir );
        

        
        
        
        
        //-----------------------------------------------------\\
        //---------------------Table Toolbar-------------------\\
        //-----------------------------------------------------\\
 
	
//				 New Config Table Section
			  	Group backupTableGroup = new Group(View.composite_for_tab4, SWT.NULL);
			  	gridData = new GridData(GridData.FILL_BOTH);
			  	gridData.horizontalSpan  = 3;
			  	gridData.verticalSpan = 5;
			  	backupTableGroup.setLayoutData(gridData);
			  	GridLayout backupTableGroup_layout = new GridLayout();
			  	backupTableGroup_layout.numColumns = 3;
			  	backupTableGroup.setLayout(backupTableGroup_layout);
			 	backupTableGroup.setText("Current Configuration Backups");
				
		
                
                
                
                backup_tb = new ToolBar(backupTableGroup,SWT.FLAT | SWT.HORIZONTAL);
                
                
                
                
                //      Refresh Button on Tab4 for BackupTable
            refreshBackup = new ToolItem(backup_tb,SWT.PUSH);
            refreshBackup.setImage(ImageRepository.getImage("refresh"));
            refreshBackup.setToolTipText("Refresh Backup Table");
            //refreshBackup.pack();
            refreshBackup.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event e) {
                    //loadDirectory(type);
                    //Tab4Utils.backup_loadDirectory();
                    redrawBackupTable();
                    StatusBoxUtils.mainStatusAdd(" Backup Table Refreshed",0);
                }
            });
            //   Backup Button -- On Tab4 
            
            
            backup = new ToolItem(backup_tb,SWT.PUSH);
            backup.setImage(ImageRepository.getImage("create_backup"));
            backup.setToolTipText("Create Backup");
            backup.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event e) {
                    Tab4Utils.backupButtonRun(0);
                }
            });
            
            
            //      Delete Button on Tab4
            deleteBackup = new ToolItem(backup_tb,SWT.PUSH);
            deleteBackup.setToolTipText("Delete Selected Directory(s)");
            deleteBackup.setImage(ImageRepository.getImage("delete"));
            deleteBackup.setEnabled(false);
            deleteBackup.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event e) {
                    try {
                        //deleteselectedfile(fullFileName);
                        //fullFileName=null;
                        TableItem[] backup_item = backupTable.getSelection();
                        //System.out.println(backup_item.length);
                                    
                        
                        if(backup_item.length > 0)
                        {
                        
                            // Check if the table is not disposed
                             if(backupTable == null || backupTable.isDisposed())
                              return;
                            //Get the selection
                            TableItem[] items = backupTable.getSelection();
                            
                            //If an item is selected (single click selects at least one)
                            if(items.length == 1) 
                            {
                              //Grab selected item
                              TableItem item = items[0];
                              String fileName = item.getText(1);
                              //Delete selected directory                         
                              
                              String backupDir = DirectoryUtils.getBackupDirectory();
                              String fullDirName = backupDir  + System.getProperty("file.separator") + "config" + System.getProperty("file.separator") + fileName;
                              Tab4Utils.deleteSelectedDir(fullDirName);
                            }else if (items.length > 1){
                                String[] dirs_to_die = new String[items.length];
                                for(int i = 0 ; i < items.length ; i++){
                                    dirs_to_die[i] = DirectoryUtils.getBackupDirectory()+System.getProperty("file.separator") + "config" + System.getProperty("file.separator") +items[i].getText(1); 
                                }
                                Tab4Utils.deleteMultiDirs(dirs_to_die);
                                
                                
                            }
                        }               
                        
                    }catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    Tab6Utils.refreshLists();
                
                    }
                });
                        
                
            ToolItem sep = new ToolItem(backup_tb,SWT.SEPARATOR);
            sep.setWidth(10);
            
            ToolItem help = new ToolItem(backup_tb,SWT.PUSH);
            help.setImage(ImageRepository.getImage("help"));
            help.setToolTipText("View helpful tips about this list");
            help.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event e) {
                    Shell shell = new Shell();
                    MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                    mb.setText("Helpful Hints");
                    mb.setMessage("1) Double click on Directory Name to restore backup\n\n" +
                    "2) Right click on a row to view more options");
                    mb.open();
                    
                }
            });
               
            
            
            
            
            //-----------------------------------------------------\\
            //-------------------Table initialization--------------\\
            //-----------------------------------------------------\\
                //Beginning of the backupTable!
                backupTable = new Table(backupTableGroup, SWT.VIRTUAL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
				backupTable.setHeaderVisible(true);
		
				//Initialise this table
				TableColumn directoryIcon = new TableColumn(backupTable,SWT.NULL);
				directoryIcon.setWidth(25);
				
				TableColumn directoryNamer = new TableColumn(backupTable,SWT.NULL);
				directoryNamer.setText("Config Backup Directory Name");
				directoryNamer.setWidth(300);
				
				directoryNamer.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
					    if(backupTable == null || backupTable.isDisposed())
					        return;
				
					    directoryNamer_sort = (directoryNamer_sort)?false:true;
					    
					}
				});
				
				TableColumn directoryTime = new TableColumn(backupTable,SWT.NULL);
				directoryTime.setText("Directory Time Stamp");
				directoryTime.setWidth(300);
				directoryTime.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
					    if(backupTable == null || backupTable.isDisposed())
					        return;
					    
                        directoryTime_sort=(directoryTime_sort)?false:true;
					    
					}
				});
				
				backupTable.addMouseListener(new MouseAdapter() {
			    	public void mouseDoubleClick(MouseEvent event) {
			         // Check is the table is not disposed
			    		 if(backupTable == null || backupTable.isDisposed())
			           return;
			         //Get the selection
			    		 
			         TableItem[] items = backupTable.getSelection();
			         if(items.length == 1){
			             View.tab.setSelection(View.tab6);
			             Tab6Utils.clearLists();
			             Tab6.restore_combo.setEnabled(true);
			             String combo_items[] = Tab6.restore_combo.getItems();
			             for (int i = 0; i < combo_items.length ; i++){
			                 if(combo_items[i].startsWith(items[0].getText(1))){
			                     Tab6.restore_combo.select(i);
			                 }
			             }
			             
			         }
			         
			    	}
			    });
				
                
                //Virtual data listener for backup table

                backupTable.addListener(SWT.SetData, new Listener() {
                    public void handleEvent(Event e) {
                        //pull the item
                        TableItem item = (TableItem)e.item;
                        
                        //get the index of the item
                        int index = backupTable.indexOf(item);
                        
                        
                        
                        
                                                
                        //return if empty directory
                        if(backupFiles == null || backupFiles.length == 0) return;
                        
                        //return if file is not a directory
                        if(backupFiles[index] == null){
                            
                            return;
                        }
                        
                        BackupUserTableItemAdapter butia;
                        
                        butia = new BackupUserTableItemAdapter(backupFiles[index].getName(),backupFiles[index].lastModified());
                        item = butia.getTableItem(item);
                        
                        //gray if needed
                        if(index%2!=0){
                            item.setBackground(ColorUtilities.getBackgroundColor());
                        }
                        
                    }
                });
                
                
                
                
				
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 3;
				backupTable.setLayoutData(gridData);

				Menu popupmenu_ConfigTable = new Menu(View.composite_for_tab4);
				
				MenuItem deleteItem_config = new MenuItem(popupmenu_ConfigTable, SWT.PUSH);
				deleteItem_config.setText("Delete Config Backup Directory(s)");
				deleteItem_config.addListener(SWT.Selection, new Listener() {
			    	public void handleEvent (Event e){
						try {
//			   			 Check if the table is not disposed
				       		 if(backupTable == null || backupTable.isDisposed())
				              return;
				            //Get the selection
				            TableItem[] items = backupTable.getSelection();
				            
				            //If an item is selected (single click selects at least one)
				            if(items.length == 1) {
				              //Grab selected item
				              TableItem item = items[0];
				              String fileName = item.getText(1);
				              
				              
				              String backupDir = DirectoryUtils.getBackupDirectory();
				      		
				              
				              String fullDirName = backupDir  + System.getProperty("file.separator") + "config" + System.getProperty("file.separator") + fileName;
				              //System.out.println(fullDirName);
				              Tab4Utils.deleteSelectedDir(fullDirName);
				            } else if (items.length > 1){
				                String[] dirs_to_die = new String[items.length];
				                for(int i = 0 ; i < items.length ; i++){
				                    dirs_to_die[i] = DirectoryUtils.getBackupDirectory()+System.getProperty("file.separator") + "config" + System.getProperty("file.separator") +items[i].getText(1); 
				                }
				                Tab4Utils.deleteMultiDirs(dirs_to_die);
				                
				                
				            }
							
							
							
							
						}catch (Exception e1) {
							e1.printStackTrace();
						}
						Tab6Utils.refreshLists();
			    	}
			    });
				
			    final MenuItem insertDirectory = new MenuItem(popupmenu_ConfigTable, SWT.PUSH);
			    insertDirectory.setText("Restore directory of saved settings");
			    insertDirectory.addListener(SWT.Selection, new Listener() {
			    	public void handleEvent (Event e){
			    	    // Check is the table is not disposed
			   		 if(backupTable == null || backupTable.isDisposed())
			          return;
			        //Get the selection
			   		 
			        TableItem[] items = backupTable.getSelection();
			        if(items.length == 1){
			            View.tab.setSelection(5);
			            Tab6Utils.clearLists();
			            
			            String combo_items[] = Tab6.restore_combo.getItems();
			            for (int i = 0; i < combo_items.length ; i++){
			                if(combo_items[i].startsWith(items[0].getText(1))){
			                    Tab6.restore_combo.select(i);
			                }
			            }
			        }
			        
			   	}
			    });
				
			    
			    final MenuItem exploreDirectory = new MenuItem(popupmenu_ConfigTable, SWT.PUSH);
			    
                //Appease the Tim-meister
                if(View.getPluginInterface().getUtilities().isWindows())
			        exploreDirectory.setText("Show in Explorer");
                else
                    exploreDirectory.setText("Explore directory");
                
			    exploreDirectory.addListener(SWT.Selection, new Listener() {
			    	public void handleEvent (Event e){
			    	    // Check is the table is not disposed
			   		 if(backupTable == null || backupTable.isDisposed())
			          return;
			   		 	//			   	Get the selection
			   		 
			        TableItem[] items = backupTable.getSelection();
			        if(items.length == 1){
			            TableItem item = items[0];
			            String dir_go = DirectoryUtils.getBackupDirectory()
			            	+ System.getProperty("file.separator") 
			            	+ "config" 
			            	+ System.getProperty("file.separator") 
			            	+ item.getText(1);
			                                  
			            View.getPluginInterface().getUIManager().openFile(new File(dir_go));
			        }
			   		 		   		 
			   		 
			    	}
			    });
			   		 
			   		 
			   		 
			    backupTable.setMenu(popupmenu_ConfigTable);
			    
			    
			    
    //And finally add a listener to the table
    backupTable.addMouseListener(new MouseAdapter() {
    	public void mouseDown(MouseEvent event) {
         // Check is the table is not disposed
    		 if(backupTable == null && backupTable.isDisposed())
    	           return;
         //Get the selection
         TableItem[] items = backupTable.getSelection();
         //System.out.println(items.length);
         ButtonStatus.set(true,true,false,true,true);
         if(items.length == 0){
         	backupTable.deselectAll();
        	if(View.getDisplay() == null && View.getDisplay().isDisposed())
        	    return;
            View.getDisplay().asyncExec(new Runnable (){
				public void run () {
				    if(deleteBackup !=null && !deleteBackup.isDisposed()){
				        if (deleteBackup.getEnabled())
                            deleteBackup.setEnabled(false);
				    }
				    
				     					
				}
			
			});
         }
         //If an item is selected (single click selects at least one)
         if(items.length == 1) {
           //Grab selected item
             insertDirectory.setEnabled(true);
             exploreDirectory.setEnabled(true);
             //TableItem item = items[0];
           //String fileName = item.getText(1);
           if(deleteBackup !=null && !deleteBackup.isDisposed())
               deleteBackup.setEnabled(true);
            
         }else if(items.length > 1){
             insertDirectory.setEnabled(false);
             exploreDirectory.setEnabled(false);
         }
      }
    	
    });
    }
    
    /**
     * Redraws the backupTable.. since it is virtual, we need to repopulate it
     * each time the user array is modified
     * 
     */
    public static void redrawBackupTable(){
        ButtonStatus.set(true, true, false, true, true);
        int j = 0;
        try{
            final String backupinstalldirectory = DirectoryUtils.getBackupDirectory();
            final String backupDirectory = backupinstalldirectory + System.getProperty("file.separator") + "config";
            
            
            File f = new File(backupDirectory);
            if(f.isFile()){
                f.delete();
            }
            if(!f.isDirectory()){
                f.mkdir();
            }
            File[] files = f.listFiles();
            backupFiles = new File[files.length];
            
            for(int i = 0; i < files.length; i++){
                if(files[i].isDirectory()){
                    backupFiles[j] = files[i];
                    j++;
                }
            }
            
            
            //backupFiles = f.listFiles();
        }catch(Exception e){
            e.printStackTrace();
        }
        
        
        
        final int tableLength = j;
        // Reset the data so that the SWT.Virtual picks up the array
        View.getDisplay().syncExec(new Runnable() {
            public void run() {
                if (backupTable.isDisposed()) 
                    return;
                
                try{
                    backupTable.setItemCount(tableLength);    
                }catch (Exception e){
                    backupTable.setItemCount(0);
                }
                
                backupTable.clearAll();
                
                
                
            }
        });
    }

   
    
    

//EOF
}
