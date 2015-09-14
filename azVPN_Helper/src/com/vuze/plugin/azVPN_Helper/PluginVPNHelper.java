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

package com.vuze.plugin.azVPN_Helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.gudy.azureus2.core3.internat.IntegratedResourceBundle;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

public class PluginVPNHelper
	implements UnloadablePlugin, UIManagerListener, PluginListener
{
	private static final boolean LOG_TO_STDOUT = false;

	private static final int DEFAULT_CHECK_EVERY_MINS = 2;

	private static final String DEFAULT_VPN_IP_REGEX = "10\\.[0-9]+\\.[0-9]+\\.[0-9]+";

	private static String[] vpnIDs = new String[] {
		"AirVPN",
		"PIA",
		"Mullvad",
		""
	};

	private PluginInterface pi;

	private static LoggerChannel logger;

	protected UIInstance uiInstance;

	public static PluginVPNHelper instance;

	public CheckerCommon checker;

	public String checkerID;

	private BasicPluginConfigModel configModel;

	private BasicPluginViewModel model;

	private UI ui;

	private static long initializedOn;

	private StringListParameter currentVPN;

	protected List<CheckerListener> listeners = new ArrayList<CheckerListener>(1);

	private HashMap<String, List<Parameter>> mapVPNConfigParams;

	private ParameterTabFolder tabFolder;

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize(PluginInterface plugin_interface)
			throws PluginException {
		instance = this;

		initializedOn = System.currentTimeMillis();

		this.pi = plugin_interface;

		UIManager uiManager = pi.getUIManager();

		logger = pi.getLogger().getTimeStampedChannel(
				PluginConstants.CONFIG_SECTION_ID);

		model = uiManager.createLoggingViewModel(logger, true);
		model.setConfigSectionID(PluginConstants.CONFIG_SECTION_ID);

		setupConfigModel(uiManager);

		LocaleUtilities i18n = pi.getUtilities().getLocaleUtilities();

		for (String vpnID : vpnIDs) {
			if (vpnID.length() == 0) {
				continue;
			}
			try {
				i18n.integrateLocalisedMessageBundle(
						"com.vuze.plugin.azVPN_Helper.internat.Messages_" + vpnID);

				Class<?> checkerCla = Class.forName(
						"com.vuze.plugin.azVPN_Helper.Checker_" + vpnID);

				Method method = checkerCla.getMethod("setupConfigModel",
						PluginInterface.class, BasicPluginConfigModel.class);

				@SuppressWarnings("unchecked")
				List<Parameter> listParams = (List<Parameter>) method.invoke(null, pi,
						configModel);

				boolean visible = vpnID.equals(checkerID);
				if (listParams != null && listParams.size() > 0) {

					ParameterGroup group = configModel.createGroup("!" + vpnID + "!",
							listParams.toArray(new Parameter[0]));
					tabFolder.addTab(group);

					for (Parameter configParameter : listParams) {
						//configParameter.setVisible(visible);
						configParameter.setEnabled(visible);
					}
				}

				mapVPNConfigParams.put(vpnID, listParams);

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		i18n.integrateLocalisedMessageBundle(
				"com.vuze.plugin.azVPN_Helper.internat.Messages");

		String vpnID = currentVPN.getValue();
		if (vpnID.length() > 0) {
			i18n.integrateLocalisedMessageBundle(
					"com.vuze.plugin.azVPN_Helper.internat.Messages_" + vpnID);

			try {
				Class<?> checkerCla = Class.forName(
						"com.vuze.plugin.azVPN_Helper.Checker_" + vpnID);

				checker = (CheckerCommon) checkerCla.getConstructor(
						PluginInterface.class).newInstance(pi);
				checkerID = vpnID;

				CheckerListener[] triggers = getCheckerListeners();
				for (CheckerListener l : triggers) {
					try {
						l.checkerChanged(checker);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			} catch (Throwable e) {
				e.printStackTrace();
			}

			List<Parameter> list = mapVPNConfigParams.get(vpnID);
			if (list != null && list.size() > 0) {
				for (Parameter configParameter : list) {
					//configParameter.setVisible(true);
					configParameter.setEnabled(true);
				}
			}
		}

		pi.getUIManager().addUIListener(this);

		pi.addListener(this);
	}

	private void setupConfigModel(UIManager uiManager) {
		configModel = uiManager.createBasicPluginConfigModel(
				PluginConstants.CONFIG_SECTION_ID);

		currentVPN = configModel.addStringListParameter2(
				PluginConstants.CONFIG_CURRENT_VPN, "vpnhelper.currentvpn", vpnIDs,
				vpnIDs, "");
		currentVPN.addConfigParameterListener(new ConfigParameterListener() {
			public void configParameterChanged(ConfigParameter param) {
				LocaleUtilities i18n = pi.getUtilities().getLocaleUtilities();
				i18n.integrateLocalisedMessageBundle(
						"com.vuze.plugin.azVPN_Helper.internat.Messages");

				if (checker != null) {
					checker.destroy();
					checker = null;

					List<Parameter> list = mapVPNConfigParams.get(checkerID);
					if (list != null && list.size() > 0) {
						for (Parameter configParameter : list) {
							//configParameter.setVisible(false);
							configParameter.setEnabled(false);
						}
					}

					checkerID = null;
				}

				String vpnID = currentVPN.getValue();
				if (vpnID.length() > 0) {
					i18n.integrateLocalisedMessageBundle(
							"com.vuze.plugin.azVPN_Helper.internat.Messages_" + vpnID);

					try {
						Class<?> checkerCla = Class.forName(
								"com.vuze.plugin.azVPN_Helper.Checker_" + vpnID);

						if (pi.getUtilities().compareVersions(pi.getAzureusVersion(),
								"5.6.2.1") < 0) {
							// replacing keys did not clear the UsedMessageMap until 5621
							Class<?> claMT = Class.forName(
									"org.gudy.azureus2.core3.internat.MessageText");
							Field declaredField = claMT.getDeclaredField("RESOURCE_BUNDLE");
							declaredField.setAccessible(true);
							IntegratedResourceBundle RESOURCE_BUNDLE = (IntegratedResourceBundle) declaredField.get(
									null);
							RESOURCE_BUNDLE.clearUsedMessagesMap(1);
						}

						checker = (CheckerCommon) checkerCla.getConstructor(
								PluginInterface.class).newInstance(pi);
						checkerID = vpnID;

						List<Parameter> list = mapVPNConfigParams.get(vpnID);
						if (list != null && list.size() > 0) {
							for (Parameter configParameter : list) {
								//configParameter.setVisible(true);
								configParameter.setEnabled(true);
							}
						}

					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				CheckerListener[] triggers = getCheckerListeners();
				for (CheckerListener l : triggers) {
					try {
						l.checkerChanged(checker);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (checker != null) {
					checker.buildTimer();
					checker.portBindingCheck();
				}
			}
		});

		IntParameter checkMinsParameter = configModel.addIntParameter2(
				PluginConstants.CONFIG_CHECK_MINUTES, "vpnhelper.check.port.every.mins",
				DEFAULT_CHECK_EVERY_MINS, 0, 60 * 24);
		checkMinsParameter.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				if (checker != null) {
					checker.buildTimer();
				}
			}
		});

		configModel.addBooleanParameter2(PluginConstants.CONFIG_DO_PORT_FORWARDING,
				PluginConstants.CONFIG_DO_PORT_FORWARDING, true);

		StringParameter paramRegex = configModel.addStringParameter2(
				PluginConstants.CONFIG_VPN_IP_MATCHING,
				PluginConstants.CONFIG_VPN_IP_MATCHING, DEFAULT_VPN_IP_REGEX);
		paramRegex.setMinimumRequiredUserMode(StringParameter.MODE_ADVANCED);

		mapVPNConfigParams = new HashMap<String, List<Parameter>>(1);

		tabFolder = configModel.createTabFolder();
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

		listeners.clear();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginListener#initializationComplete()
	 */
	public void initializationComplete() {
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

	public final void addListener(CheckerListener l) {
		listeners.add(l);
		try {
			if (checker != null) {
				l.portCheckStatusChanged(checker.lastPortCheckStatus);
				l.protocolAddressesStatusChanged(checker.lastProtocolAddresses);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public final void removeListener(CheckerListener l) {
		listeners.remove(l);
	}

	public CheckerListener[] getCheckerListeners() {
		return listeners.toArray(new CheckerListener[0]);
	}
}
