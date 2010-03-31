/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import java.io.File;

import com.aelitis.azureus.plugins.azjython.JythonPluginCore;

/**
 * @author Allan Crooks
 *
 */
public class JythonOutputWindow extends AbstractSWTJythonEnvironment {
	
	private static SWTJythonOutputWindow makeSWTJythonOutputWindow(JythonCoreConsole j_console, UISWTInstance swt_ui) {
		return new SWTJythonOutputWindow(j_console.getCore(), swt_ui, j_console.getName());
	}
	
	public static String getConsoleName(JythonPluginCore core, File script_file) {
		return core.localise("azjython.script_window.title", script_file);
	}
	
	public JythonOutputWindow(JythonCoreConsole console, UISWTInstance swt_ui, Composite composite) {
		super(console, makeSWTJythonOutputWindow(console, swt_ui), swt_ui, composite);
	}
	
	public boolean runScript(File script_file) {
		return console.registerScriptFileAtStartup(script_file, "azjython.script_window.script_pre_init");
	}
	
	public void startConsoleRunning() {
		super.startConsoleRunning();
		this.console.putInput(null); // We won't be providing any input, so just close stdin.
	}
	
}
