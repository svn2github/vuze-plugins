/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import java.io.File;

import com.aelitis.azureus.plugins.azjython.JythonPluginCore;
import com.aelitis.azureus.plugins.azjython.utils.DataSink;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * @author Allan Crooks
 *
 */
public class JythonInteractiveConsole extends AbstractSWTJythonEnvironment {
	
	private static SWTJythonConsole makeSWTJythonConsole(JythonCoreConsole j_console, UISWTInstance swt_ui) {
		return new SWTJythonConsole(j_console.getCore(), swt_ui, j_console.getName());
	}
	
	public static String getConsoleName(JythonPluginCore core, int interpreter_id) {
		return core.locale_utils.getLocalisedMessageText(
				"azjython.interpreter.title",
				new String[] {String.valueOf(interpreter_id)});
	}
	
	private SWTJythonConsole swt_con;
	
	public JythonInteractiveConsole(JythonCoreConsole j_console, UISWTInstance swt_ui, Composite composite) {
		super(j_console, makeSWTJythonConsole(j_console, swt_ui), swt_ui, composite);
		swt_con = (SWTJythonConsole)this.swt_output;

		swt_con.setDataSink(new DataSink() {
	    	public void put(Object o) {
	    		JythonInteractiveConsole.this.console.putInput((String)o);
	    	}
	    });
		
	    swt_con.setMultiLineDataSink(new DataSink() {
	    	public void put(Object o) {
	    		JythonInteractiveConsole.this.console.putDelayedInput((String[])o);
	    	}
	    });
	    
	    String script_filename = j_console.getCore().getScriptValue("startup_script.interpreter");
	    if (script_filename != null) {
	    	j_console.registerScriptFileAtStartup(new File(script_filename), "azjython.interpreter.script_pre_init");
	    }
	    		
	}
	
	public String getBanner() {
		JythonPluginCore core = console.getCore();
		return BasicThreadedConsole.getDefaultBanner(core) + "\n\n" +
    	core.locale_utils.getLocalisedMessageText("azjython.interpreter.banner_instability_warning") +
    	"\n\n" + core.locale_utils.getLocalisedMessageText("azjython.interpreter.banner");
	}
		
}
