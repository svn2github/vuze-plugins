/*
 * Created on Feb 6, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import java.io.File;


import omschaub.azcvsupdater.utilities.ShellUtils;



import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIException;

/**
 * StatusBox Graphics Initialization
 */
public class StatusBox {
    
    static Table mainStatus;
    private static int sashMemory;
    public static Group status_group; 
    private static String defaultPath;
    private static int escPressed;
    
    static void open(final PluginInterface pluginInterface){
       
   
        //Composite for Status bar
		status_group = new Group(View.sash,SWT.NULL);
		GridData gridData=new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan=1;
		//gridData.verticalSpan=1;
		status_group.setLayoutData(gridData);
		
		GridLayout status_layout = new GridLayout();
		status_layout.numColumns = 3;
		//status_layout.makeColumnsEqualWidth = false;
		status_group.setLayout(status_layout);
		status_group.setText("Status (Double Click to Hide)");


		
//	mainStatus	element		
		
		mainStatus = new Table(status_group,SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 3;
		//gridData.heightHint = 55;
		//gridData.verticalSpan = 3;
		mainStatus.setLayoutData(gridData);
		
		StatusBoxUtils.mainStatusAdd(" AZCVSUpdater Running...",0);
		
		//element to tell about config options
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gridData.horizontalSpan = 1;
	
		


		
		status_group.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event e) {
				if(status_group == null || status_group.isDisposed() )
					return;
				if(View.sash == null || View.sash.isDisposed())
				    return;
				int[] sash_weight_array = View.sash.getWeights();
				
				
				if(sash_weight_array[0] < 950)
				{
				    sashMemory = sash_weight_array[0];
				    int [] sash_new_weight = {970,30};
				    View.sash.setWeights(sash_new_weight);
				    status_group.setText("Status (Double Click to Reveal)");
				}
				else
				{
				    int [] sash_new_weight = {sashMemory,1000-sashMemory};
				    View.sash.setWeights(sash_new_weight);
				    status_group.setText("Status (Double Click to Hide)");
				}
			}
		});

		
		
		mainStatus.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event e) {
				if(status_group == null || status_group.isDisposed() )
					return;
				if(View.sash == null || View.sash.isDisposed())
				    return;
				int[] sash_weight_array = View.sash.getWeights();
				
				if(sash_weight_array[0] < 950)
				{
				    sashMemory = sash_weight_array[0];
				    int [] sash_new_weight = {970,30};
				    View.sash.setWeights(sash_new_weight);
				    status_group.setText("Status (Double Click to Reveal)");
				}
				else
				{
				    int [] sash_new_weight = {sashMemory,1000-sashMemory};
				    View.sash.setWeights(sash_new_weight);
				    status_group.setText("Status (Double Click to Hide)");
				}
			}
		});

		
		
		 Menu popupmenu_status = new Menu(status_group);
		 
		 
		 final MenuItem saveStatus = new MenuItem(popupmenu_status, SWT.PUSH);
		 saveStatus.setText("Save status window to log file");
		 saveStatus.addListener(SWT.Selection, new Listener() {
		    	public void handleEvent (Event e){
                    try{
                        TableItem[] items = mainStatus.getItems();
                        if(items.length == 0){
                            MessageBox messageBox = new MessageBox(mainStatus.getShell(), SWT.ICON_ERROR | SWT.OK);
                            messageBox.setText("Table is Empty");
                            messageBox.setMessage("The table is empty, therefore nothing can be written to a file.");
                            messageBox.open();
                            return;
                        }
                        final String[] item_text = new String[items.length];
                        for(int i = 0; i < items.length; i++){
                            item_text[i] = items[i].getText();
                        }
                        
                        FileDialog fileDialog = new FileDialog(status_group.getShell(), SWT.SAVE);
                        fileDialog.setText("Please choose a file to save the information to");
                        String[] filterExtensions = {"*.txt","*.log","*.*"};
                        fileDialog.setFilterExtensions(filterExtensions);
                        if(defaultPath == null){
                            defaultPath = View.getPluginInterface().getPluginDirectoryName();    
                        }                    
                        fileDialog.setFilterPath(defaultPath);
                        String selectedFile = fileDialog.open();
                        if(selectedFile != null){
                            final File fileToSave = new File(selectedFile);
                            
                            defaultPath = fileToSave.getParent();
                            if(fileToSave.exists()){
                                if(!fileToSave.canWrite()){
                                    MessageBox messageBox = new MessageBox(status_group.getShell(), SWT.ICON_ERROR | SWT.OK);
                                    messageBox.setText("Error writing to file");
                                    messageBox.setMessage("Your computer is reporting that the selected file cannot be written to, please retry this operation and select a different file");
                                    messageBox.open();
                                    return;
                                }
                                
                                final Shell shell = new Shell(SWT.DIALOG_TRIM);
                                shell.setLayout(new GridLayout(3,false));
                                shell.setText("File Exists");
                                Label message = new Label(shell,SWT.NULL);
                                message.setText("Your selected file already exists. \n" +
                                                            "Choose 'Overwrite' to overwrite it, deleting the original contents \n" +
                                                            "Choose 'Append' to append the information to the existing file \n" +
                                                            "Choose 'Cancel' to abort this action all together\n\n");
                                GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                                gridData.horizontalSpan = 3;
                                message.setLayoutData(gridData);
                                
                                Button overwrite = new Button(shell,SWT.PUSH);
                                overwrite.setText("Overwrite");
                                overwrite.addListener(SWT.Selection, new Listener(){
                                    public void handleEvent(Event e) 
                                    {
                                        shell.close();
                                        shell.dispose();
                                        StatusBoxUtils.save_log(item_text,fileToSave,1);
                                    }
                                });
                                
                                gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                overwrite.setLayoutData(gridData);
                                
                                
                                Button append = new Button(shell,SWT.PUSH);
                                append.setText("Append");
                                append.addListener(SWT.Selection, new Listener(){
                                    public void handleEvent(Event e) 
                                    {   
                                        shell.close();
                                        shell.dispose();
                                        StatusBoxUtils.save_log(item_text,fileToSave,0);
                                    }
                                });
                                
                                Button cancel = new Button(shell,SWT.PUSH);
                                cancel.setText("Cancel");
                                cancel.addListener(SWT.Selection, new Listener(){
                                    public void handleEvent(Event e) 
                                    {   
                                        shell.close();
                                        shell.dispose();
                                    }
                                });
                                
                                gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                cancel.setLayoutData(gridData);
                                overwrite.addKeyListener(new KeyListener() {
                                    public void keyPressed(KeyEvent e) {
                                        switch (e.character){
                                            case SWT.ESC: escPressed=1;break;
                                            
                                        }
                                        
                                    }
                                    public void keyReleased (KeyEvent e) {
                                        if (escPressed == 1){
                                        
                                            escPressed = 0;
                                            shell.close();
                                            shell.dispose();
                                        }
                                        
                                    }
                                });

                                
                                ShellUtils.centerShellandOpen(shell);
                            }else{
                                fileToSave.createNewFile();
                                StatusBoxUtils.save_log(item_text,fileToSave,2);
                            }
                            
                            
                        }
                        
                        
                    }catch (Exception f){
                        f.printStackTrace();
                        MessageBox messageBox = new MessageBox(status_group.getShell(), SWT.ICON_ERROR | SWT.OK);
                        messageBox.setText("Error writing to file");
                        messageBox.setMessage("Your computer is reporting that the selected file cannot be written to, please retry this operation and select a different file");
                        messageBox.open();
                    }
                    
                    
                    
                    
                    
                    
                    
		    	
		    	}
		    });

		    
		 
		 
		 
		    final MenuItem clearStatus = new MenuItem(popupmenu_status, SWT.PUSH);
		    clearStatus.setText("Clear Status List");
		    clearStatus.addListener(SWT.Selection, new Listener() {
		    	public void handleEvent (Event e){
		    	    if(mainStatus != null && !mainStatus.isDisposed())
		    	    {
		    	        mainStatus.removeAll();
		    	    }
		    	
		    	}
		    });
		
		    
			 final MenuItem copyClip = new MenuItem(popupmenu_status, SWT.PUSH);
			 copyClip.setText("Copy selected text to clipboard");
			 copyClip.addListener(SWT.Selection, new Listener() {
			    	public void handleEvent (Event e){
			    	    try {
                            TableItem[] item = mainStatus.getSelection();                        
                               if(item.length == 0){
                                   return;
                               }
                               View.getPluginInterface().getUIManager().copyToClipBoard(item[0].getText());
                            
                           // pluginInterface.getUIManager().copyToClipBoard(mainStatus.getItem(mainStatus.getSelectionIndex()).getText());
                        } catch (UIException e1) {
                            e1.printStackTrace();
                        }
			    	
			    	}
			    });
		    
		mainStatus.setMenu(popupmenu_status);    
        
        
        
        popupmenu_status.addMenuListener(new MenuListener(){
            public void menuHidden(MenuEvent arg0) {
                
                
            }

            public void menuShown(MenuEvent arg0) {
                saveStatus.setEnabled(false);
                clearStatus.setEnabled(false);
                copyClip.setEnabled(false);
                
                if(mainStatus.getItemCount() > 0){
                    saveStatus.setEnabled(true);
                    clearStatus.setEnabled(true);
                    //copyClip.setEnabled(true);
                }
                
                TableItem[] item = mainStatus.getSelection();                        
                if(item.length > 0){
                    saveStatus.setEnabled(true);
                    clearStatus.setEnabled(true);
                    copyClip.setEnabled(true);
                    
                }
                
            }
        });
        
        
        
           if(status_group != null && !status_group.isDisposed()){
               status_group.layout();
            }
    }
    

}
