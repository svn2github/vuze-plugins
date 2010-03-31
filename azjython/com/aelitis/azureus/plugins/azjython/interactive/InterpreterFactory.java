/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import com.aelitis.azureus.plugins.azjython.JythonPluginCore;

import org.eclipse.swt.widgets.Composite;
import java.util.*;
import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * @author Allan Crooks
 *
 */
public class InterpreterFactory implements UISWTViewEventListener {
	
	private Map view_to_console_map;
	private JythonPluginCore core;
	private UISWTInstance swt_ui;
	private int interpreters_created = 0;
	
	public InterpreterFactory(JythonPluginCore core, UISWTInstance swt_ui) {
		this.core = core;
		this.view_to_console_map = new HashMap();
		this.swt_ui = swt_ui;
	}
	
	public boolean eventOccurred(UISWTViewEvent event) {

		// Get the console object involved too.
		UISWTView view = event.getView();
		JythonInteractiveConsole console = (JythonInteractiveConsole)view_to_console_map.get(view);
		
		switch(event.getType()) {
		
			// We do nothing for these ones.
			case UISWTViewEvent.TYPE_CREATE:
			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
			case UISWTViewEvent.TYPE_FOCUSLOST:
			case UISWTViewEvent.TYPE_REFRESH:
			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				return true;
				
			case UISWTViewEvent.TYPE_INITIALIZE:
				String console_name = JythonInteractiveConsole.getConsoleName(core, ++this.interpreters_created);
				JythonCoreConsole core_console = JythonCoreConsole.create(core, "interpreter", console_name);
				
				// Then setup the JythonInteractiveConsole, and then start it going.
				console = new JythonInteractiveConsole(core_console, swt_ui, (Composite)event.getData());
				console.startConsoleRunning();
				view.setTitle(console_name);
				view_to_console_map.put(view, console);
				return true; 

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				if (console != null) {
					console.setFocus();
				}
				return true;
				
			case UISWTViewEvent.TYPE_CLOSE:
				// Add a message box in the future.
				return true;
				
			case UISWTViewEvent.TYPE_DESTROY:
				if (console != null) {
					view_to_console_map.put(view, null);
					console.destroy();
				}
				return true;
				
			default:
				return true;
		}
	}

}
