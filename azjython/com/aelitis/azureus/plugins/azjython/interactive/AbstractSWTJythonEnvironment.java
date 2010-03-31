/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import com.aelitis.azureus.plugins.azjython.utils.DataSink;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * @author Allan Crooks
 *
 */
public abstract class AbstractSWTJythonEnvironment {
	
	protected SWTJythonOutputWindow swt_output;
	protected JythonCoreConsole console;
	protected Composite composite;
	//private UISWTInstance swt_ui;
	
	protected AbstractSWTJythonEnvironment(JythonCoreConsole console, SWTJythonOutputWindow swt_output, UISWTInstance swt_ui, Composite composite) {
		this.console = console;
		//this.swt_ui = swt_ui;
		this.composite = composite;
		this.swt_output = swt_output;
	
		if (!console.hasStarted()) {
			console.setVariable("swt_ui", swt_ui);
		}
	    
	}
	
	public void startConsoleRunning() {
		swt_output.initialise(composite);
		
		// These things have to be done *after* the output view has been initialised.
		console.setDataSink(new TransferConsoleDataSink());

		// Disable the console UI when the main interpreter dies.
		console.setTerminateHook(new Runnable() {
			public void run() {
				AbstractSWTJythonEnvironment.this.swt_output.disableAsync();
			}
		});
		if (!console.hasStarted()) {
			console.startConsoleRunning(getBanner());
		}
	}
	
	protected String getBanner() {return "";}
		
	private class TransferConsoleDataSink implements DataSink {
		
		public void put(Object o) {
			if (o == null) {return;}
			if (o instanceof IOControlCommand) {
				AbstractSWTJythonEnvironment.this.swt_output.sendTextControlCommandAsync((IOControlCommand)o);
				return;
			}
			OutputContextString ocs = (OutputContextString)o;
			AbstractSWTJythonEnvironment.this.swt_output.putTextAsync(ocs);
		}
	}
	
	public void setFocus() {
		swt_output.setFocus();
	}
	
	public void destroy() {
		console.destroy();
		swt_output.dispose();
	}
		
}
