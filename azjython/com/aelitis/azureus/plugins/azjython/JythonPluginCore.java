/**
 * File: JythonPluginCore.java
 * Library: Jython Plugin for Azureus
 * Date: 29th November 2006
 *
 * Author: Allan Crooks
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package com.aelitis.azureus.plugins.azjython;

import java.io.File;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

// Maybe interpreter related parts need to be moved somewhere else.
import com.aelitis.azureus.plugins.azjython.interactive.JythonCoreConsole;
import com.aelitis.azureus.plugins.azjython.interactive.JythonOutputWindow;

/**
 * @author Allan Crooks
 *
 */
public class JythonPluginCore {
	
	public final boolean DEBUG_MODE = false;
	public JythonPlugin plugin;
	public PluginInterface plugin_interface;
	public LoggerChannel logger;
	public LocaleUtilities locale_utils;
	public String jython_version = null;
	public JythonUINamespace ui_namespace = null;
	public JythonCoreConsole startup_console = null;
	
	public JythonPluginCore(JythonPlugin jp, PluginInterface pi) {
		this.plugin = jp;
		this.plugin_interface = pi;
	}
	
	public void addInternalRefs() {
		this.ui_namespace = new JythonUINamespace();
	}
	
	public void addExternalRefs() {
		this.logger = this.plugin_interface.getLogger().getChannel("azjython");
		this.locale_utils = this.plugin_interface.getUtilities().getLocaleUtilities();
	}
	
	public String getJythonPath() {
		String result = plugin_interface.getPluginconfig().getPluginStringParameter("jython.path");
		if (result != null) {result.trim();}
		return result;
	}

	public void informUser(String message) {
		logger.logAlert(LoggerChannel.LT_INFORMATION, p_localise(message));
	}

	public void informUser(String message, String data) {
		logger.logAlert(LoggerChannel.LT_INFORMATION, p_localise(message, data));
	}
	
	public void warnUser(String message) {
		logger.logAlert(LoggerChannel.LT_WARNING, p_localise(message));
	}
	
	public void warnUser(String message, String data) {
		logger.logAlert(LoggerChannel.LT_WARNING, p_localise(message, data));
	}
	
	public void showError(String message) {
		logger.logAlert(LoggerChannel.LT_ERROR, p_localise(message));
	}

	public void showError(String message, String data) {
		logger.logAlert(LoggerChannel.LT_ERROR, p_localise(message, data));
	}
	
	public void showError(String message, Exception e) {
		logger.logAlert(p_localise(message), e);
	}
	
	private String p_localise(String key) {
		return this.locale_utils.getLocalisedMessageText("azjython." + key);
	}
	
	private String p_localise(String key, String message) {
		return this.locale_utils.getLocalisedMessageText("azjython." + key, new String[]{message});
	}
	
	public String getTimePrefix() {
		return "[" +
		    this.plugin_interface.getUtilities().getFormatters().formatTimeOnly(
				this.plugin_interface.getUtilities().getCurrentSystemTime(), true
		    ) + "]";
	}
	
	public String localise(String template) {
		return locale_utils.getLocalisedMessageText(template);
	}
	
	public String localise(String template, File file_object) {
		return locale_utils.getLocalisedMessageText(template, new String[] {
			file_object.getAbsolutePath(), file_object.getName()
		});
	}
	
	public boolean initSuccessful(boolean successful) {
		boolean result = this.getFlag("last.init.ok");
		this.setFlag("last.init.ok", successful);
		return result;
	}
	
	public boolean isFirstRun() {
		return !this.hasFlag("last.init.ok");
	}
	
	public void setFlag(String name, boolean value) {
		this.plugin_interface.getPluginconfig().setPluginParameter(name, value);
	}

	public boolean getFlag(String name) {
		return this.plugin_interface.getPluginconfig().getPluginBooleanParameter(name);
	}
	
	public boolean getFlag(String name, boolean default_) {
		if (!this.hasFlag(name)) {return default_;}
		return this.getFlag(name);
	}

	public boolean hasFlag(String name) {
		return this.plugin_interface.getPluginconfig().hasPluginParameter(name);
	}
	
	public boolean delFlag(String name) {
		return this.plugin_interface.getPluginconfig().removePluginParameter(name);
	}
	
	public String getScriptValue(String parameter_prefix) {
		boolean enabled = getFlag(parameter_prefix + ".enabled", false);
		if (!enabled) {return null;}
		String script = plugin_interface.getPluginconfig().getPluginStringParameter(parameter_prefix + ".script");
		if (script == null || script.length() == 0) {return null;}
		return script;
	}
	
	public void runStartupScript(String script_name, boolean run) {
		JythonCoreConsole con = createConsoleForScriptNoRegister(script_name);
		boolean init_ok = con.registerScriptFileAtStartup(new File(script_name), "azjython.script_window.script_pre_init");
		if (!init_ok) {return;}
		if (run) {con.startConsoleRunning("");} // XXX; Shouldn't have to remember to pass empty string...
		startup_console = con;
	}
	
	public JythonCoreConsole createConsoleForScript(String script_name) throws java.io.IOException {
		JythonCoreConsole con = createConsoleForScriptNoRegister(script_name);
		con.runAndOpenScriptFileAtStartup(new File(script_name), "azjython.script_window.script_pre_init");
		return con;
	}
	
	private JythonCoreConsole createConsoleForScriptNoRegister(String script_name) {
		File script_file = new File(script_name);
		String console_name = JythonOutputWindow.getConsoleName(this, script_file);
		return JythonCoreConsole.create(this, "script", console_name);
	}
	
}
