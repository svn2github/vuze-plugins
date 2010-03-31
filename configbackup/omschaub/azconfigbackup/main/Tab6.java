/*
 * Created on Feb 5, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import omschaub.azconfigbackup.utilities.DirectoryUtils;
import omschaub.azconfigbackup.utilities.Restart;
import omschaub.azconfigbackup.utilities.StackX;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
//import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 *tab6 graphics for AzConfigBackup
 *
 */
public class Tab6 {
    
	static Combo backup_combo;
	static Combo restore_combo;
	
	static Label alert;
	static Button final_restart_button;
	static Button final_stop_button;
	static Button final_nostop_button;
	static Button submit_button;
	static ArrayList all_components;
	
    static void open(){
    	
    	all_components = new ArrayList();
       
        Composite shell = new Composite(View.composite_for_tab6, SWT.NULL);

    	GridLayout layout = new GridLayout();
    	layout.numColumns = 1;
    	shell.setLayout(layout);
    	//final Display display = shell.getDisplay();
    	GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING);
    	gridData.horizontalSpan = 1;
    	shell.setLayoutData(gridData);
    	
    	backup_combo = generateSection(shell, "Would you like to make a backup of your current config files?", "What would you like to call the backup config directory?", true);
        backup_combo.setVisibleItemCount(10);
        Tab6Utils.getBackupFiles(("ConfigBackup" + View.getPluginInterface().getAzureusVersion()));
       	
       	restore_combo = generateSection(shell, "Would you like to restore a previously saved backup config directory?", null, false);
    	Tab6Utils.getConfigFiles();
    	restore_combo.setVisibleItemCount(10);
    	
    	// Composite for the alert row
    	Composite composite_alert_row = new Composite(shell,SWT.NULL);
    	layout = new GridLayout();
    	layout.numColumns = 3;
    	composite_alert_row.setLayout(layout);
    	gridData = new GridData(GridData.FILL_HORIZONTAL);
    	gridData.horizontalSpan=2;
    	composite_alert_row.setLayoutData(gridData);
    	//Label for alert row
    	alert = new Label(composite_alert_row,SWT.NULL);
    	gridData = new GridData(GridData.FILL_HORIZONTAL);
    	alert.setLayoutData(gridData);
    	alert.setText("You have not completed the form correctly. " +
    			"\nPlease make sure that all settings are filled out \ncorrectly and Submit again");
    	alert.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_RED));
    	alert.setVisible(false);
    	
    	//Composite for the final buttons
    	Group composite_final = new Group(shell,SWT.NULL);
    	layout = new GridLayout();
    	layout.numColumns = 3;
    	composite_final.setLayout(layout);
    	gridData = new GridData(GridData.FILL_HORIZONTAL);
    	composite_final.setLayoutData(gridData);
    	    	
    	//Cancel Button
    	final Button cancel_button = new Button(composite_final,SWT.PUSH);
    	cancel_button.setText("&Clear Choices / Refresh Lists");
    	gridData= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    	gridData.horizontalSpan = 2;
    	cancel_button.setLayoutData(gridData);
    	cancel_button.addListener(SWT.Selection, new Listener() {
    		public void handleEvent(Event e) {
                cancel_button.setText("&Clear Choices / Refresh Lists");
    		    Tab6Utils.getBackupFiles(("ConfigBackup" + View.getPluginInterface().getAzureusVersion()));
    	    	Tab6Utils.getConfigFiles();
    		    Tab6Utils.clearLists();
    		}
    	});
    	
    	//	Submit Button
    	submit_button = new Button(composite_final,SWT.PUSH);
    	submit_button.setText(" &Submit ");
    	gridData= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    	submit_button.setLayoutData(gridData);    	
    	
    	//Final Yes_restart Button
    	final_restart_button = new Button(composite_final,SWT.PUSH);
    	final_restart_button.setText("Accept and Restart Now");
    	gridData= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    	final_restart_button.setLayoutData(gridData);
    	final_restart_button.setVisible(false);
    	
    	//	Final Yes_Exit Button
    	final_stop_button = new Button(composite_final,SWT.PUSH);
    	final_stop_button.setText("Accept and Exit Now");
    	gridData= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    	final_stop_button.setLayoutData(gridData);
    	final_stop_button.setVisible(false);

    	//	Final Yes_NOExit Button
    	final_nostop_button = new Button(composite_final,SWT.PUSH);
    	final_nostop_button.setText("Accept and Restart Later");
    	gridData= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    	final_nostop_button.setLayoutData(gridData);
    	final_nostop_button.setVisible(false);
    	
    	final ProcessJob button_processor = new ProcessJob() { 
    		public StackX[] process() {
    		    final StackX complete_file_from = new StackX(50);  //make from stack
    		    final StackX complete_file_to = new StackX(50);  //make to stack
    		    
    			if(isComboEnabled(backup_combo)){
    				String filename = View.getPluginInterface().getUtilities().getAzureusUserDir();
    				File dir = new File(filename);
    				String destinationDir = DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator") + "config";
                    Tab4Utils.makeBackup(dir,filename,destinationDir,backup_combo.getText(),false);
    			}
    			
    			if(isComboEnabled(restore_combo)){
    				String config_directoryToRestore = restore_combo.getItem(restore_combo.getSelectionIndex());
                    config_directoryToRestore = config_directoryToRestore.substring(0,(config_directoryToRestore.indexOf("|")-1));
    				String destinationDir = DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator") + "config";
    				String azureusUserDir = View.getPluginInterface().getUtilities().getAzureusUserDir();
    				try 
    				{
    					File config_directory = new File(destinationDir + System.getProperty("file.separator") + config_directoryToRestore);
    					if(!config_directory.isDirectory())
    					{
    					    StatusBoxUtils.mainStatusAdd(" Major Error - Config Directory not a directory",2);
    					    return null;
    					}
    					
    					File[] files_array = config_directory.listFiles();
    					if(files_array == null)
    					{
    					    StatusBoxUtils.mainStatusAdd(" Major Error - Config Directory Empty",2);
    					    return null;
    					}
    					
    					for (int i = 0 ; i < files_array.length ; i++)
    					{
    					    complete_file_from.push(destinationDir + System.getProperty("file.separator") + config_directoryToRestore +System.getProperty("file.separator")  + files_array[i].getName());
    					    complete_file_to.push(azureusUserDir + System.getProperty("file.separator") + files_array[i].getName());
    					}
    				
    				} 
    				catch (Exception configException) 
    				{
    				    configException.printStackTrace();
    				} 
    			}
    			
    			return new StackX[] {complete_file_from, complete_file_to};
    		}
    	};
    	
    	//restart listener
    	Listener restart_listener = new Listener() {
    		public void handleEvent(Event e) {
    			StackX[] files = button_processor.process();
    			if (files == null) {return;}
                Restart.updateRestart(View.getPluginInterface(),files[0],files[1],true);
    		}
    	};
    	final_restart_button.addListener(SWT.Selection, restart_listener);
    	    	        
    	Listener  stop_listener = new Listener() {
    		public void handleEvent(Event e) {
    			StackX[] files = button_processor.process();
    			if (files == null) {return;}
                Restart.updateRestart(View.getPluginInterface(),files[0],files[1],false);
    		}
    	};
    	
    	final_stop_button.addListener(SWT.Selection,stop_listener);
    	
    	//nostop button listener
    	final Listener nostop_listener = new Listener() {
 		public void handleEvent(Event e) {
			StackX[] files = button_processor.process();
			if (files == null) {return;}
            Restart.updateNORestart(View.getPluginInterface(),files[0],files[1]);
            Tab6Utils.clearLists();
            cancel_button.setText("&Clear Choices / Refresh Lists");
 		}
 	};
 	
 	final_nostop_button.addListener(SWT.Selection,nostop_listener);
 	
    	// Listener for Submit Button
    	submit_button.addListener(SWT.Selection, new Listener() {
    		public void handleEvent(Event e) {
    			if(!isComboEnabled(restore_combo) && !isComboEnabled(backup_combo)){
    				    if(View.getDisplay() == null && View.getDisplay().isDisposed())
    				        return;
    				    alert.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_RED));
    					    alert.setText("Error:  All options are set to NO. " +
    						"\nPlease make sure that all settings are filled out correctly and Submit again");
    						alert.setVisible(true);
    						return;	    
    					
    				    
    				}
    				
    			if(restore_combo.isEnabled()){
    				if (restore_combo.getSelectionIndex() == 0){
    				    if(View.getDisplay() == null && View.getDisplay().isDisposed())
    				        return;
    				    alert.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_RED));
    				    alert.setText("ERROR:  You have not selected a config directory to restore. " +
    					"\nPlease make sure that all settings are filled out correctly and Submit again");
    					alert.setVisible(true);
    					return;
    				}
    			}
    			String config_directoryToRestore = restore_combo.getItem(restore_combo.getSelectionIndex());
    			config_directoryToRestore = config_directoryToRestore.substring(0,(config_directoryToRestore.length()-24));
    			if (backup_combo.getEnabled()){
    				//System.out.println("here it is: " + directory_get.getText());
    			if (config_directoryToRestore.equalsIgnoreCase(backup_combo.getText())){
    			    if(View.getDisplay() == null && View.getDisplay().isDisposed())
				        return;
				    alert.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_RED));
    			    alert.setText("ERROR:  Your backup and restore config directories are the same. " +
    				"\nPlease make sure that all settings are filled out correctly and Submit again");
    				alert.setVisible(true);
    				return;
    			}
    			}
    			
    			// If the "backup" section is enabled (but not for "restore"), then just perform
    			// the action - no need to prompt the user.
    			if (isComboEnabled(backup_combo) && !isComboEnabled(restore_combo)) {
    				nostop_listener.handleEvent(null);
    				return;
    			}
    			    			    		
    			alert.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
    			alert.setText("Settings will be submitted as shown above.  Do you accept these settings?");
    			alert.setVisible(true);
    			//green.dispose();
    			
    			//Disable everything
    			Object component = null;
    			Iterator itr = all_components.iterator();
    			while (itr.hasNext()) {
    				component = itr.next();
    				if (component instanceof Control) {
    					((Control)component).setEnabled(false);
    				}
    			}
    			
    			submit_button.setVisible(false);
    			final_restart_button.setVisible(true);
    			final_stop_button.setVisible(true);
    			final_nostop_button.setVisible(true);
    			cancel_button.setText("Do Not Accept");
    		}
    	});

    }
    
    /**
     * Generates a "section" for this tab - there's a lot of shared behaviour...
     */
    private static Combo generateSection(Composite shell, String main_text, String sub_text, boolean editable) {
    	Group this_row = new Group(shell, SWT.NULL);
    	
    	GridLayout layout = new GridLayout();
    	layout.numColumns = 3;
    	this_row.setLayout(layout);
    	
    	GridData grid_data = new GridData(GridData.FILL_HORIZONTAL);
    	this_row.setLayoutData(grid_data);
    	
    	final Label main_text_label = new Label(this_row, SWT.NULL);
    	main_text_label.setText(main_text);
    	main_text_label.setEnabled(false);
    	all_components.add(main_text_label);

    	final Button yes_button = new Button(this_row, SWT.RADIO);
    	yes_button.setText("Yes");
    	yes_button.setSelection(false);
    	grid_data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL);
    	yes_button.setLayoutData(grid_data);
    	all_components.add(yes_button);
    	
    	final Button no_button = new Button(this_row, SWT.RADIO);
    	no_button.setText("No");
    	no_button.setSelection(true);
    	grid_data = new GridData(GridData.HORIZONTAL_ALIGN_END );
    	no_button.setLayoutData(grid_data);
    	all_components.add(no_button);

    	final Label[] sub_text_label = new Label[1];
    	if (sub_text != null) {
    		sub_text_label[0] = new Label(this_row, SWT.NULL);
    		sub_text_label[0].setText(sub_text);
    		sub_text_label[0].setEnabled(false);
    		all_components.add(sub_text_label[0]);
    	}
    	
    	final Combo combo = new Combo(this_row, (editable) ? SWT.BORDER : (SWT.DROP_DOWN | SWT.READ_ONLY));
    	combo.select(0);
    	combo.setEnabled(false);
    	all_components.add(combo);
    	
    	// Allows us to see whether "yes" is selected even if the control is disabled.
    	combo.setData("yes button", yes_button);
    	
    	grid_data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    	grid_data.horizontalSpan = 3;
    	grid_data.widthHint = 300;
    	combo.setLayoutData(grid_data);

    	
    	Listener yes_listener = new Listener() {
    		public void handleEvent(Event e) {
    			if(!yes_button.getSelection()){
    				yes_button.setSelection(true);
    			}
    			no_button.setSelection(false);
    			combo.setEnabled(true);
    			main_text_label.setEnabled(true);
    			if (sub_text_label[0] != null) {
    				sub_text_label[0].setEnabled(true);
    			}
    		}
    	};
    	
    	final Listener no_listener = new Listener() {
    		public void handleEvent(Event e) {
    			if(!no_button.getSelection()){
    				no_button.setSelection(true);
    			}
    			yes_button.setSelection(false);
    			combo.setEnabled(false);
    			main_text_label.setEnabled(false);
    			if (sub_text_label[0] != null) {
    				sub_text_label[0].setEnabled(false);
    			}
    		}
    	};
    	
    	yes_button.addListener(SWT.Selection, yes_listener);
    	combo.addListener(SWT.Selection, yes_listener);
    	no_button.addListener(SWT.Selection, no_listener);
    	
    	// Send a "none" event to reset things back to their default settings.
    	combo.addListener(SWT.None, new Listener() {
    		public void handleEvent(Event e) {
    			no_listener.handleEvent(e);
    			combo.select(0);
    		}
    	});
    	
    	return combo;
    }
    
    private interface ProcessJob {
    	public StackX[] process();
    }
    
    private static boolean isComboEnabled(Combo combo) {
    	return ((Button)combo.getData("yes button")).getSelection();
    }

}
