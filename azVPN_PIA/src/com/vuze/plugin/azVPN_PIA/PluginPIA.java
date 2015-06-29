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

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

public class PluginPIA
	implements UnloadablePlugin
{
	private static final String CONFIG_SECTION_ID = "vpn_pia";

	public static final String CONFIG_PIA_MANAGER_DIR = "pia_manager.dir";

	public static final String CONFIG_P = "p.privx";

	public static final String CONFIG_USER = "user";

	public static final String CONFIG_CHECK_MINUTES = "check.minutes";

	private static final boolean LOG_TO_STDOUT = false;

	private PluginInterface pi;

	private static LoggerChannel logger;

	protected UIInstance uiInstance;

	public static PluginPIA instance;

	public CheckerPIA checkerPIA;

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize(PluginInterface plugin_interface)
			throws PluginException {
		instance = this;

		this.pi = plugin_interface;

		checkerPIA = new CheckerPIA(pi);

		plugin_interface.getUIManager().addUIListener(new UIManagerListener() {

			public void UIDetached(UIInstance instance) {
				uiInstance = null;
			}

			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					UISWTInstance swtInstance = (UISWTInstance) instance;
					new UI(pi, swtInstance);
				}
				uiInstance = instance;
			}

		});

		UIManager uiManager = pi.getUIManager();

		logger = pi.getLogger().getTimeStampedChannel(CONFIG_SECTION_ID);

		final BasicPluginViewModel model = uiManager.createLoggingViewModel(logger,
				true);
		model.setConfigSectionID(CONFIG_SECTION_ID);

		setupConfigModel(uiManager);

		MenuItem menuItem = uiManager.getMenuManager().addMenuItem(
				MenuManager.MENU_MENUBAR, "ConfigView.section.vpn_pia");
		menuItem.addListener(new MenuItemListener() {

			public void selected(MenuItem menu, Object target) {
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				mdi.showEntryByID(UI.VIEW_ID);
			}
		});

		pi.addListener(new PluginListener() {

			public void initializationComplete() {
				try {
					checkerPIA.portBindingCheck();
					checkerPIA.calcProtocolAddresses();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				checkerPIA.buildTimer();
			}

			public void closedownInitiated() {
			}

			public void closedownComplete() {
			}
		});
	}

	private void setupConfigModel(UIManager uiManager) {
		BasicPluginConfigModel configModel = uiManager.createBasicPluginConfigModel(
				CONFIG_SECTION_ID);

		configModel.addDirectoryParameter2(CONFIG_PIA_MANAGER_DIR,
				CONFIG_PIA_MANAGER_DIR, checkerPIA.getPIAManagerPath().toString());

		IntParameter checkMinsParameter = configModel.addIntParameter2(
				CONFIG_CHECK_MINUTES, "check.port.every.mins", 1);
		checkMinsParameter.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				checkerPIA.buildTimer();
			}
		});

		List<Parameter> parameters = new ArrayList<Parameter>();

		parameters.add(configModel.addLabelParameter2("login.group.explain"));
		StringParameter paramUser = configModel.addStringParameter2(CONFIG_USER,
				"config.user", checkerPIA.getDefaultUsername());
		parameters.add(paramUser);
		PasswordParameter paramPass = configModel.addPasswordParameter2(CONFIG_P,
				"config.pass", PasswordParameter.ET_PLAIN, new byte[] {});
		parameters.add(paramPass);

		configModel.createGroup("login.group",
				parameters.toArray(new Parameter[0]));
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	 */
	public void unload()
			throws PluginException {
	}

	public static void log(String s) {
		if (LOG_TO_STDOUT || logger == null) {
			long offsetTime = SystemTime.getCurrentTime()
					- AzureusCoreFactory.getSingleton().getCreateTime();
			System.out.println(offsetTime + "] LOGGER: " + s);
		}
		if (logger == null) {
			return;
		}
		logger.log(s);
	}

}
