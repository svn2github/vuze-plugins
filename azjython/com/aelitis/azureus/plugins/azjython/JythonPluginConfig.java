/**
 * File: JythonPluginConfig.java
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

import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import java.util.*;

/**
 * @author Allan Crooks
 *
 */
public class JythonPluginConfig {

	public static void initialise(final JythonPlugin plugin, final JythonPluginCore core, final JythonPluginInitialiser jpi, boolean init_ok) {
		BasicPluginConfigModel model = createMainConfigGroup(plugin, core, jpi, init_ok);
		createInstallationConfigGroup(plugin, core, jpi, model, init_ok);
		createScriptsConfigGroup(plugin, core, jpi, model);
	}

	private static void addJythonStatusParameter(boolean init_ok, JythonPluginCore core, BasicPluginConfigModel model, boolean refer_to_panel) {
		LocaleUtilities lu = core.plugin_interface.getUtilities().getLocaleUtilities();
		if (init_ok) {
			Parameter p = model.addLabelParameter2("azjython.config.init_not_ok" + ((refer_to_panel) ? ".2" : ""));
			if (core.jython_version != null) {
				p.setLabelText(lu.getLocalisedMessageText("azjython.config.init_ok_with_version", new String[] {core.jython_version}));
			}
		}
		else {
			model.addLabelParameter2("azjython.config.init_not_ok" + ((refer_to_panel) ? ".2" : ""));
		}
	}
	
	private static BasicPluginConfigModel createMainConfigGroup(final JythonPlugin plugin, final JythonPluginCore core, final JythonPluginInitialiser jpi, boolean init_ok) {
		BasicPluginConfigModel model = core.plugin_interface.getUIManager().createBasicPluginConfigModel("azjython");
		addJythonStatusParameter(init_ok, core, model, true);
		model.addLabelParameter2("azjython.config.require_restart");

		// We reuse this multiple times to store parameters in a group.
		ArrayList parameter_group = new ArrayList();

		LocaleUtilities lu = core.plugin_interface.getUtilities().getLocaleUtilities();
		
		// Start interpreter on plugin startup.
		parameter_group.add(model.addBooleanParameter2("auto_open_console", "azjython.config.auto_open_console", false));
		
		// Have "Mark as selected" item menu option.
		parameter_group.add(model.addBooleanParameter2("mark_as_selected", "azjython.config.mark_as_selected", true));
		
		createGroup(model, "preferences", parameter_group);

		// Development only settings.core.initSuccessful(true);
		if (core.DEBUG_MODE) {
			String k = "azjython.config.debug.init_flag";
			ActionParameter reset_am = model.addActionParameter2(k, k + ".action.reset");
			reset_am.addListener(new ParameterListener() {
				public void parameterChanged(Parameter p) {
					core.delFlag("last.init.ok");
				}
			});
			ActionParameter init_ok_am = model.addActionParameter2(k, k + ".action.false");
			init_ok_am.addListener(new ParameterListener() {
				public void parameterChanged(Parameter p) {
					core.initSuccessful(false);
				}
			});
			ActionParameter init_fail_am = model.addActionParameter2(k, k + ".action.true");
			init_fail_am.addListener(new ParameterListener() {
				public void parameterChanged(Parameter p) {
					core.initSuccessful(true);
				}
			});
		}
		
		String[] websites = new String[] {"azjython", "jython", "bugreport", "forum"};
		for (int i=0; i<websites.length; i++) {
			parameter_group.add(model.addHyperlinkParameter2("azjython.website." + websites[i] + ".title", lu.getLocalisedMessageText("azjython.website." + websites[i] + ".url")));
		}
		createGroup(model, "websites", parameter_group);

		
		return model;
	}
	
	private static BasicPluginConfigModel createInstallationConfigGroup(final JythonPlugin plugin, final JythonPluginCore core, final JythonPluginInitialiser jpi, final BasicPluginConfigModel parent, final boolean init_ok) {
		BasicPluginConfigModel model = core.plugin_interface.getUIManager().createBasicPluginConfigModel(parent.getSection(), "azjython.install");
		addJythonStatusParameter(init_ok, core, model, false);
		
		// Things to disable upon installing Jython.
		final List disable_on_install = new ArrayList();

		// We reuse this multiple times to store parameters in a group.
		ArrayList parameter_group = new ArrayList();
		
		LabelParameter lp = model.addLabelParameter2("azjython.config.auto_config.info");
		final ActionParameter start_param = model.addActionParameter2("azjython.blank", "azjython.config.auto_config.start");
		final ActionParameter stop_param = model.addActionParameter2("azjython.blank", "azjython.config.auto_config.stop");
		start_param.setEnabled(!init_ok);
		stop_param.setEnabled(false);
				
		parameter_group.add(lp);
		parameter_group.add(start_param);
		parameter_group.add(stop_param);
		disable_on_install.add(start_param);
		disable_on_install.add(stop_param);
		createGroup(model, "auto_config", parameter_group);
				
		final DirectoryParameter dm = model.addDirectoryParameter2("jython.path", "azjython.config.jythonpath", "");
		disable_on_install.add(dm);
		parameter_group.add(dm);
					
		ActionParameter am = model.addActionParameter2("azjython.config.install", "azjython.config.install.action");
		disable_on_install.add(am);
		parameter_group.add(am);
		
		final ParameterListener auto_install_listener = new ParameterListener() {
			public void parameterChanged(Parameter p) {
				boolean installed = jpi.installJython(true);
				if (installed) {
					for (int i=0; i < disable_on_install.size(); i++) {
						((Parameter)disable_on_install.get(i)).setEnabled(false);
					}
				}
			}
		};
		
		am.addListener(auto_install_listener);
		am.setEnabled(!init_ok);

		start_param.addListener(new ParameterListener() {
			public void parameterChanged(Parameter p) {
				JythonInstaller.getSingleton().startInstall(start_param, stop_param, auto_install_listener);
			}
		});

		stop_param.addListener(new ParameterListener() {
			public void parameterChanged(Parameter p) {
				JythonInstaller.getSingleton().stopInstall();
			}
		});
		
		createGroup(model, "jython_config", parameter_group);
		return model;
	}

	private static BasicPluginConfigModel createScriptsConfigGroup(final JythonPlugin plugin, final JythonPluginCore core, final JythonPluginInitialiser jpi, final BasicPluginConfigModel parent) {
		BasicPluginConfigModel model = core.plugin_interface.getUIManager().createBasicPluginConfigModel(parent.getSection(), "azjython.scripts");
		
		// We reuse this multiple times to store parameters in a group.
		ArrayList parameter_group = new ArrayList();

		// Startup script support for interpreters.
		parameter_group.add(model.addBooleanParameter2("startup_script.interpreter.enabled", "azjython.config.startup_script.interpreter.enabled", false));
		parameter_group.add(model.addFileParameter2("startup_script.interpreter.script", "azjython.config.startup_script.interpreter.script", "", new String[]{"*.py"}));
		
		addEnableDisableListener(parameter_group);
		createGroup(model, "startup_script.interpreter", parameter_group);
		
		// Startup script support for Azureus.
		parameter_group.add(model.addBooleanParameter2("startup_script.azureus.enabled", "azjython.config.startup_script.azureus.enabled", false));
		parameter_group.add(model.addFileParameter2("startup_script.azureus.script", "azjython.config.startup_script.azureus.script", "", new String[]{"*.py"}));
		parameter_group.add(model.addBooleanParameter2("startup_script.azureus.wait_for_ui", "azjython.config.startup_script.azureus.wait_for_ui", true));
		parameter_group.add(model.addBooleanParameter2("startup_script.azureus.output_in_window", "azjython.config.startup_script.azureus.output_in_window", false));
		
		addEnableDisableListener(parameter_group);
		createGroup(model, "startup_script.azureus", parameter_group);

		// Running a script now (not really a config thing, but never mind).
		final FileParameter run_script_name_param = model.addFileParameter2("one_off_script_exec.script", "azjython.config.one_off_script_exec.script", "", new String[]{"*.py"});
		parameter_group.add(run_script_name_param);
		
		final BooleanParameter run_script_in_window = model.addBooleanParameter2("one_off_script_exec.output_in_window", "azjython.config.one_off_script_exec.output_in_window", true);
		parameter_group.add(run_script_in_window);

		ActionParameter run_script_param_invoke = model.addActionParameter2("azjython.config.one_off_script_exec.invoke", "azjython.config.one_off_script_exec.invoke.action");
		parameter_group.add(run_script_param_invoke);

		createGroup(model, "one_off_script_exec", parameter_group);
		
		// This will execute the script.
		run_script_param_invoke.addListener(new ParameterListener() {
			public void parameterChanged(Parameter p) {
				String script_name = run_script_name_param.getValue();
				plugin.ipcRunScriptFromFileSafe(script_name, run_script_in_window.getValue());
			}
		});

		return model;
	
	}
	
	private static void createGroup(BasicPluginConfigModel model, String group_name, List parameters) {
		model.createGroup("azjython.config.group." + group_name, (Parameter[])parameters.toArray(new Parameter[parameters.size()]));
		parameters.clear();
	}
	
	private static void addEnableDisableListener(List param_list) {
		Iterator itr = param_list.iterator();
		BooleanParameter bp = (BooleanParameter)itr.next();
		
		while (itr.hasNext()) {
			bp.addEnabledOnSelection((Parameter)itr.next());
		}
	}
	
}
