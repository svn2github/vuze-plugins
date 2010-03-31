/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.plugins.azjython.JythonPluginCore;
import com.aelitis.azureus.plugins.azjython.utils.DataSink;

/**
 * @author Allan Crooks
 *
 */
public final class JythonCoreConsole implements Plugin {
	
	public static JythonCoreConsole create(JythonPluginCore core, String namespace_key, String console_name) {

		PluginInterface con_pi;
		
		// Create the core console like a plugin.
		try {con_pi = core.plugin_interface.getLocalPluginInterface(JythonCoreConsole.class, namespace_key);}
		catch (PluginException pe) {return null;}
		
		// Get the core console and initialise it (not run it).
		JythonCoreConsole core_console = (JythonCoreConsole)con_pi.getPlugin();
		core_console.initialize(core, console_name);
		return core_console;
	}
	
	private BasicThreadedConsole console = null;
	private PluginInterface plugin_interface = null;
	private JythonPluginCore core = null;
	private String console_name = null; // InterpreterFactory uses this.

	public final void initialize(PluginInterface plugin_interface) throws PluginException {
		this.plugin_interface = plugin_interface;
		setScriptName("main");
	}
	
	public final void setScriptName(String script_name) {
		plugin_interface.getPluginconfig().setPluginConfigKeyPrefix("jython_script:" + script_name);
	}
	
	public final void setVariable(String name, Object value) {
		this.console.set(name, value);
	}
	
	public void initialize(JythonPluginCore core, String console_name) {
		
		this.console_name = console_name;
		
		// Create the console (both the underlying thing and the SWT version), and
		// link them together.
		console = new BasicThreadedConsole(core, console_name);
		this.core = core;

		// You have to modify console_init.py to copy across the value to
		// the interpreter globals.
		setVariable("plugin_interface", plugin_interface);    
		setVariable("_ui_namespace", core.ui_namespace);
		setVariable("swt_ui", null); // This is overridden by other objects.
		setVariable("jstdout", console.pw_stdout);
		setVariable("jstderr", console.pw_stderr);
		
		ClassLoader cl = core.plugin_interface.getPluginClassLoader();
		console.runScriptOnStartup(cl.getResourceAsStream("azjython/py/console_init.py"), "azjython_init", null, null);
	}
	
	public boolean registerScriptFileAtStartup(File script_file, String announce_tmpl) {
		try {
			runAndOpenScriptFileAtStartup(script_file, announce_tmpl);
			return true;
		}
		catch (IOException ioe) {
			// Complain about it if anything happens.
			// XXX: Maybe we should output this information in the console window...
			core.logger.logAlert("Error opening up script: " + script_file.getAbsolutePath(), ioe);
			return false;
		}
	}
	
	public void runAndOpenScriptFileAtStartup(File script_file, String announce_tmpl) throws IOException {
		BufferedInputStream script_stream;
		script_stream = new BufferedInputStream(new FileInputStream(script_file));
		
		String pre_init = null;
		if (announce_tmpl != null) {
			pre_init = core.getTimePrefix() + " " + core.localise(announce_tmpl, script_file);
		}
		
		// Derive a script name to use (for display on tracebacks).
		String script_name = script_file.getName();
		/**
		 * I've changed my mind about this - tracebacks look nicer when the file extension is present.
		 * 
		int file_ext_pos = script_name.lastIndexOf('.');
		if (file_ext_pos != -1) {script_name = script_name.substring(0, file_ext_pos);}
		*/
		
		this.console.runScriptOnStartup(script_stream, script_name, pre_init, null);
	}
		
	
	public void startConsoleRunning() {
		console.startInteracting();
	}

	public void startConsoleRunning(String banner) {
		console.startInteracting(banner);
	}
	
	public void destroy() {
		console.setTerminateHook(null);
		console.exit();
	}

	// These methods are mainly proxy methods on the console itself.
	public boolean hasStarted() {return console.hasStarted();}
	public boolean hasFinished() {return console.hasFinished();}
	public void setDataSink(DataSink sink) {console.setDataSink(sink);}
	
	public void setTerminateHook(Runnable hook) {
		// Some slight problems here with race conditions here, but ignore for now.
		if (console.hasFinished()) {hook.run();}
		else {console.setTerminateHook(hook);}
	}
	
	public String getName() {return this.console_name;}
	public PluginInterface getPluginInterface() {return this.plugin_interface;}
	public JythonPluginCore getCore() {return this.core;}
	
	public void putInput(String s) {this.console.putInput(s);}
	public void putDelayedInput(String[] s) {this.console.putDelayedInput(s);}
	
}
