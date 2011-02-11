/*
 * Created on Feb 5, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.Restart;
import omschaub.azcvsupdater.utilities.StackX;

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
 *tab6 graphics for AZCVSUpdater
 *
 */
public class Tab6 {
    
    
	static Combo comboJar;
		
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
    	
    	comboJar = generateSection(shell, "Would you like to insert a backup file as Azureus2.jar?", null, false);
    	Tab6Utils.getCVSFiles();
    	comboJar.setVisibleItemCount(10);
    	
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
                Tab6Utils.getCVSFiles();    		    
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
    		    
    			if (isComboEnabled(comboJar))
    			{
    				String directoryName=DirectoryUtils.getBackupDirectory();
    				String fullFileName = directoryName + System.getProperty("file.separator") + comboJar.getItem(comboJar.getSelectionIndex());
    				File az_file = new File(fullFileName);
    				if(!az_file.isFile())
    				{
    				    StatusBoxUtils.mainStatusAdd(" Major Error -- Chosen jar is not a real file",2);
    				}
    				
    				complete_file_from.push(az_file.getPath());
    				complete_file_to.push(View.getJARFileDestination());
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
    	Listener nostop_listener = new Listener() {
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
    			if(!isComboEnabled(comboJar)){
    				    if(View.getDisplay() == null && View.getDisplay().isDisposed())
    				        return;
    				    alert.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_RED));
    					    alert.setText("Error:  All options are set to NO. " +
    						"\nPlease make sure that all settings are filled out correctly and Submit again");
    						alert.setVisible(true);
    						return;	    
    					
    				    
    				}
    				
    			if(comboJar.isEnabled()){
    				if (comboJar.getSelectionIndex() == 0){
    				    if(View.getDisplay() == null && View.getDisplay().isDisposed())
    				        return;
    				    alert.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_RED));
    				    alert.setText("ERROR:  You have not selected an Azureus2-BXX file to insert. " +
    					"\nPlease make sure that all settings are filled out correctly and Submit again");
    					alert.setVisible(true);
    					return;
    				}
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
