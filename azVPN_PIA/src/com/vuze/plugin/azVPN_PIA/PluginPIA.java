/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.vuze.plugin.azVPN_PIA;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.PasswordParameter;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

public class PluginPIA
	implements UnloadablePlugin, UIManagerListener, PluginListener
{
	public static final String CONFIG_SECTION_ID = "vpn_pia";

	public static final String CONFIG_PIA_MANAGER_DIR = "pia_manager.dir";

	public static final String CONFIG_P = "p.privx";

	public static final String CONFIG_USER = "user";

	public static final String CONFIG_CHECK_MINUTES = "check.minutes";

	public static final String CONFIG_VPN_IP_MATCHING = "vpn.ip.regex";

	private static final boolean LOG_TO_STDOUT = false;

	private static final int DEFAULT_CHECK_EVERY_MINS = 2;

	private static final String DEFAULT_VPN_IP_REGEX = "10\\.[0-9]+\\.[0-9]+\\.[0-9]+";

	private PluginInterface pi;

	private static LoggerChannel logger;

	protected UIInstance uiInstance;

	public static PluginPIA instance;

	public Checker checker;

	private BasicPluginConfigModel configModel;

	private BasicPluginViewModel model;

	private UI ui;

	private static long initializedOn;

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize(PluginInterface plugin_interface)
			throws PluginException {
		instance = this;

		initializedOn = System.currentTimeMillis();

		this.pi = plugin_interface;
		
		checker = new Checker(pi);

		plugin_interface.getUIManager().addUIListener(this);

		UIManager uiManager = pi.getUIManager();

		logger = pi.getLogger().getTimeStampedChannel(CONFIG_SECTION_ID);

		model = uiManager.createLoggingViewModel(logger, true);
		model.setConfigSectionID(CONFIG_SECTION_ID);

		setupConfigModel(uiManager);

		pi.addListener(this);
	}

	private void setupConfigModel(UIManager uiManager) {
		configModel = uiManager.createBasicPluginConfigModel(CONFIG_SECTION_ID);

		if (pi.getUtilities().isWindows() || pi.getUtilities().isOSX()) {
			configModel.addDirectoryParameter2(CONFIG_PIA_MANAGER_DIR,
					CONFIG_PIA_MANAGER_DIR, checker.getPIAManagerPath().toString());
		}

		IntParameter checkMinsParameter = configModel.addIntParameter2(
				CONFIG_CHECK_MINUTES, "check.port.every.mins", DEFAULT_CHECK_EVERY_MINS,
				0, 60 * 24);
		checkMinsParameter.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				checker.buildTimer();
			}
		});

		List<Parameter> parameters = new ArrayList<Parameter>();

		parameters.add(configModel.addLabelParameter2("login.group.explain"));
		StringParameter paramUser = configModel.addStringParameter2(CONFIG_USER,
				"config.user", checker.getDefaultUsername());
		parameters.add(paramUser);
		PasswordParameter paramPass = configModel.addPasswordParameter2(CONFIG_P,
				"config.pass", PasswordParameter.ET_PLAIN, new byte[] {});
		parameters.add(paramPass);

		configModel.createGroup("login.group",
				parameters.toArray(new Parameter[0]));

		StringParameter paramRegex = configModel.addStringParameter2(
				CONFIG_VPN_IP_MATCHING, CONFIG_VPN_IP_MATCHING, DEFAULT_VPN_IP_REGEX);
		paramRegex.setMinimumRequiredUserMode(StringParameter.MODE_ADVANCED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	 */
	public void unload()
			throws PluginException {

		if (pi != null) {
			UIManager uiManager = pi.getUIManager();
			if (uiManager != null) {
				uiManager.removeUIListener(this);
			}
			pi.removeListener(this);
		}

		if (ui != null) {
			ui.destroy();
			ui = null;
		}

		if (configModel != null) {
			configModel.destroy();
		}
		if (model != null) {
			model.destroy();
		}

		if (checker != null) {
			checker.destroy();
			checker = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginListener#initializationComplete()
	 */
	public void initializationComplete() {
		PluginInterface betterPlugin = pi.getPluginManager().getPluginInterfaceByID("vpn_helper");
		if (betterPlugin == null) {
			try {
				StandardPlugin standardPlugin = pi.getPluginManager().getPluginInstaller().getStandardPlugin("vpn_helper");
				if (standardPlugin != null) {
					standardPlugin.install(true, true, false);
					pi.getPluginState().unload();
					return;
				}
			} catch (PluginException e1) {
				e1.printStackTrace();
			}
		} else if (betterPlugin.getPluginState().isOperational()) {
			checker = null;
			try {
				pi.getPluginState().unload();
			} catch (PluginException e) {
				e.printStackTrace();
			}
 			return;
 		}


		if (checker == null) {
			return;
		}
		try {
			checker.portBindingCheck();
			checker.calcProtocolAddresses();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		checker.buildTimer();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginListener#closedownInitiated()
	 */
	public void closedownInitiated() {
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginListener#closedownComplete()
	 */
	public void closedownComplete() {
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIManagerListener#UIDetached(org.gudy.azureus2.plugins.ui.UIInstance)
	 */
	public void UIDetached(UIInstance instance) {
		uiInstance = null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIManagerListener#UIAttached(org.gudy.azureus2.plugins.ui.UIInstance)
	 */
	public void UIAttached(UIInstance instance) {
		if (instance instanceof UISWTInstance) {
			UISWTInstance swtInstance = (UISWTInstance) instance;
			ui = new UI(pi, swtInstance);
		}
		uiInstance = instance;
	}

	public static void log(String s) {
		if (s == null) {
			return;
		}
		if (s.endsWith("\n")) {
			s = s.substring(0, s.length() - 1);
		}
		if (LOG_TO_STDOUT || logger == null) {
			long offsetTime = System.currentTimeMillis() - initializedOn;
			System.out.println(offsetTime + "] LOGGER: " + s);
		}
		if (logger == null) {
			return;
		}
		logger.log(s);
	}

}
