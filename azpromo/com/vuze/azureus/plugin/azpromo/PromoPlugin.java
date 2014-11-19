/**
 * Created on Sep 29, 2014
 *
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

package com.vuze.azureus.plugin.azpromo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.util.FeatureUtils;

/**
 * @created Sep 29, 2014
 */
public class PromoPlugin
	implements UnloadablePlugin
{

	private static final String VIEWID = "SidebarPromo";

	private UISWTInstance swtInstance = null;

	public static PluginInterface pluginInterface;

	public static String readStringFromUrl(String url) {
		StringBuffer sb = new StringBuffer();
		try {
			URL _url = new URL(url);
			HttpURLConnection con = (HttpURLConnection) _url.openConnection();

			InputStream is = con.getInputStream();

			byte[] buffer = new byte[256];

			int read = 0;

			while ((read = is.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, read));
			}
			con.disconnect();

		} catch (Throwable e) {

		}
		return sb.toString();
	}

	// @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	public void initialize(final PluginInterface pi) throws PluginException {
		this.pluginInterface = pi;

// We are usually initialized before FeatMan, so this check isn't very usefull
//		if (pi.getUtilities().getFeatureManager() != null) {
//  		boolean hasFullLicence = FeatureUtils.hasFullLicence();
//  		if (hasFullLicence) {
//  			return;
//  		}
//		}

		UIManager uiManager = pluginInterface.getUIManager();
		BasicPluginConfigModel configModel = uiManager.createBasicPluginConfigModel("ConfigView.Section."
				+ VIEWID);
		BooleanParameter paramEnabled = configModel.addBooleanParameter2("enabled",
				VIEWID + ".enabled", true);
		paramEnabled.addConfigParameterListener(new ConfigParameterListener() {
			public void configParameterChanged(ConfigParameter param) {
				UIInstance[] uiInstances = pluginInterface.getUIManager().getUIInstances();
				for (UIInstance uiInstance : uiInstances) {
					if (uiInstance instanceof UISWTInstance) {
						swtInstance = (UISWTInstance) uiInstance;
						break;
					}
				}
				if (swtInstance != null) {
					boolean enabled = pluginInterface.getPluginconfig().getPluginBooleanParameter(
							"enabled");
					if (enabled) {
						swtInstance.addView(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID,
								PromoView.class, null);
					} else {
						swtInstance.removeViews(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID);

						PromoPlugin.logEvent("goaway");
					}
				}
			}
		});

		boolean enabled = pluginInterface.getPluginconfig().getPluginBooleanParameter(
				"enabled");
		if (enabled) {

			// Get notified when the UI is attached
			pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
				public void UIAttached(UIInstance instance) {
					if (instance instanceof UISWTInstance) {
						swtInstance = ((UISWTInstance) instance);
						swtInstance.addView(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID,
								PromoView.class, null);
					}
				}

				public void UIDetached(UIInstance instance) {
					swtInstance = null;
				}
			});

			pluginInterface.addListener(new PluginListener() {
				public void initializationComplete() {
					if (pluginInterface == null) {
						return;
					}

					FeatureManager fm = pluginInterface.getUtilities().getFeatureManager();

					FeatureManager.FeatureManagerListener fml = new FeatureManager.FeatureManagerListener() {

						public void licenceRemoved(Licence licence) {
							checkLicence();
						}

						public void licenceChanged(Licence licence) {
							checkLicence();
						}

						public void licenceAdded(Licence licence) {
							checkLicence();
						}
					};

					fm.addListener(fml);

					checkLicence();
				}

				public void closedownInitiated() {
				}

				public void closedownComplete() {
				}
			});

		}

	}

	protected void checkLicence() {
		boolean hasFullLicence = FeatureUtils.hasFullLicence();

		if (hasFullLicence) {
			pluginInterface.getPluginconfig().setPluginParameter("enabled", false);
		}
	}

	// @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	public void unload() throws PluginException {
		if (swtInstance != null)
			swtInstance.removeViews(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID);
	}

	public static void logEvent(String event) {
		PlatformManager pm = PlatformManagerFactory.getPlatformManager();
		if (pm == null) {
			return;
		}
		Object[] params = new Object[] {
			"locale",
			MessageText.getCurrentLocale().toString(),
			"event-type",
			event
		};
		PlatformMessage message = new PlatformMessage("AZMSG", "PromoPlugin",
				"log", params, 1000);
		PlatformMessenger.pushMessageNow(message, null);
	}

}
