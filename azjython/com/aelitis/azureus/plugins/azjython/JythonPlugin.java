/**
 * 
 */
package com.aelitis.azureus.plugins.azjython;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;

import org.gudy.azureus2.plugins.config.PluginConfigSource;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;

import com.aelitis.azureus.plugins.azjython.interactive.JythonCoreConsole;

/**
 * @author Allan Crooks
 *
 */
public class JythonPlugin implements Plugin {
	
	private JythonPluginCore core = null;
	public JythonSWTUIManager ui_manager = null;

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
     */
	public void initialize(final PluginInterface plugin_interface)
			throws PluginException {
		
		final JythonPluginCore core = new JythonPluginCore(this, plugin_interface);
		this.core = core;
		
		PluginConfigSource pcs = plugin_interface.getPluginconfig().enableExternalConfigSource();
		pcs.forceSettingsMigration();
		pcs.initialize();
		core.addInternalRefs();
		core.addExternalRefs();
		
		/**
		 * Set up the log view as soon as we can.
		 * 
		 * To do at a later date - the status should indicate if the plugin is
		 * Jython enabled or not.
		 */
		final BasicPluginViewModel model = JythonPluginViewModel.createLogViewModel(core);
		JythonInstaller.createSingleton(core, model);
		
		/**
		 * Try to initialise Jython - we'll delay doing this though.
		 * 
		 * We don't force this initialisation immediately even if users want to execute
		 * the script before the UI is attached. The flag is there to indicate if the
		 * SWT UI is needed, and *not* when you want the script to be executed. It is
		 * appropriate for us to delay its execution. Perhaps we might change this behaviour
		 * if users specifically request the ability to run scripts at startup (if they
		 * really need it).
		 */
		org.gudy.azureus2.plugins.utils.DelayedTask dt = plugin_interface.getUtilities().createDelayedTask(new Runnable() {
			public void run() {
				JythonPluginInitialiser jpi = new JythonPluginInitialiser(core);
				boolean init_ok = jpi.initialiseJython(true);
		
				// Setup config panel.
				JythonPluginConfig.initialise(JythonPlugin.this, core, jpi, init_ok);
				
				// If we didn't initialise Jython, we don't do any more initialisations.
				JythonPluginViewModel.prepareModel(model, init_ok);
						
				ui_manager = new JythonSWTUIManager(core, init_ok);
				if (!init_ok) {return;}
				
				// At this point, we may need to launch a startup script.
				String script_name = core.getScriptValue("startup_script.azureus");
				if (script_name != null && !core.getFlag("startup_script.azureus.wait_for_ui")) {
		
					// This means we will run the startup script now.
					core.runStartupScript(script_name, true);
				}
			}
		});
		dt.queue();
		
	}
	
	// IPC methods.
	public void ipcRunScriptFromFile(String filepath, boolean show_window) throws java.io.IOException {
		JythonCoreConsole con = core.createConsoleForScript(filepath);
		if (!show_window) {
			ui_manager.populateConsoleVariables(con);
			con.startConsoleRunning("");
			return;
		}
		ui_manager.createOutputWindow(con, true);
	}
	
	public void ipcRunScriptFromFileSafe(String filepath, boolean show_window) {
		try {ipcRunScriptFromFile(filepath, show_window);}
		catch (Exception e) {
			core.logger.logAlertRepeatable(
				core.locale_utils.getLocalisedMessageText(
					"azjython.one_off_script_exec.failure",
					new String[]{filepath}
				),
			e);
		}
	}
	
}
