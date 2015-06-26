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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.message.BasicNameValuePair;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.*;
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
import org.gudy.azureus2.plugins.utils.LocationProvider;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminException;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminProtocol;
import com.aelitis.azureus.core.proxy.AEProxySelector;
import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;

public class Plugin
	implements UnloadablePlugin
{
	// Is it always 70000? who knows
	private final static int STATUS_FILE_PORT_INDEX = 70000;

	private final static String PIA_DOMAIN = "www.privateinternetaccess.com";

	private final static String PIA_RPC_URL = "https://" + PIA_DOMAIN
			+ "/vpninfo/port_forward_assignment";

	private PluginInterface pi;

	private static LoggerChannel logger;

	private UTTimer timer;

	private IntParameter checkMinsParameter;

	private DirectoryParameter paramPIAManagerDir;

	private PasswordParameter paramPass;

	private StringParameter paramUser;

	protected UIInstance uiInstance;

	private ActionParameter buttonProtocolAddresses;

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize(PluginInterface plugin_interface)
			throws PluginException {
		this.pi = plugin_interface;

		plugin_interface.getUIManager().addUIListener(new UIManagerListener() {

			public void UIDetached(UIInstance instance) {
				uiInstance = null;
			}

			public void UIAttached(UIInstance instance) {
				uiInstance = instance;
			}

		});

		UIManager uiManager = pi.getUIManager();

		logger = pi.getLogger().getTimeStampedChannel("vpn_pia");

		final BasicPluginViewModel model = uiManager.createLoggingViewModel(logger,
				true);
		model.setConfigSectionID("vpn_pia");

		MenuItem menuItem = uiManager.getMenuManager().addMenuItem(
				MenuManager.MENU_MENUBAR, "ConfigView.section.vpn_pia");
		menuItem.addListener(new MenuItemListener() {

			public void selected(MenuItem menu, Object target) {
				if (uiInstance != null) {
					uiInstance.openView(model);
				}
			}
		});

		setupConfigModel(uiManager);

		pi.addListener(new PluginListener() {

			public void initializationComplete() {
				try {
					portBindingCheck();
					calcProtocolAddresses();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				buildTimer();
			}

			public void closedownInitiated() {
			}

			public void closedownComplete() {
			}
		});
	}

	private void setupConfigModel(UIManager uiManager) {
		BasicPluginConfigModel configModel = uiManager.createBasicPluginConfigModel(
				"vpn_pia");

		paramPIAManagerDir = configModel.addDirectoryParameter2("pia_manager.dir",
				"pia_manager.dir", getPIAManagerPath().toString());

		checkMinsParameter = configModel.addIntParameter2("check.minutes",
				"check.port.every.mins", 1);
		checkMinsParameter.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				buildTimer();
			}
		});

		ActionParameter buttonCheck = configModel.addActionParameter2(null,
				"button.check.binding");
		buttonCheck.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				portBindingCheck();
			}
		});

		buttonProtocolAddresses = configModel.addActionParameter2(null,
				"button.verify.addresses");
		buttonProtocolAddresses.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				calcProtocolAddresses();
			}
		});

		List<Parameter> parameters = new ArrayList<Parameter>();

		parameters.add(configModel.addLabelParameter2("login.group.explain"));
		paramUser = configModel.addStringParameter2("user", "config.user",
				getDefaultUsername());
		parameters.add(paramUser);
		paramPass = configModel.addPasswordParameter2("p.privx", "config.pass", 0,
				new byte[] {});
		parameters.add(paramPass);

		configModel.createGroup("login.group",
				parameters.toArray(new Parameter[0]));
	}

	protected void buildTimer() {
		if (timer != null) {
			timer.destroy();
			timer = null;
		}
		int mins = checkMinsParameter.getValue();
		if (mins == 0) {
			return;
		}
		timer = pi.getUtilities().createTimer("PIA");
		timer.addPeriodicEvent(mins * 60 * 1000l, new UTTimerEventPerformer() {
			public void perform(UTTimerEvent event) {
				try {
					portBindingCheck();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	 */
	public void unload()
			throws PluginException {
	}

	public static void log(String s) {
		System.out.println("LOGGER: " + s);
		if (logger == null) {
			return;
		}
		logger.log(s);
	}

	private String getDefaultUsername() {
		try {
			String pathPIAManager = paramPIAManagerDir.getValue();

			File pathPIAManagerData = new File(pathPIAManager, "data");

			// settings.json has the user name
			File fileSettings = new File(pathPIAManagerData, "settings.json");
			String settingsText = FileUtil.readFileAsString(fileSettings, -1);
			Map<?, ?> mapSettings = JSONUtils.decodeJSON(settingsText);
			String user = (String) mapSettings.get("user");

			return user;
		} catch (Exception e) {
			return "";
		}
	}

	private void portBindingCheck() {
		if (true) {
			//return;
		}
		String pathPIAManager = paramPIAManagerDir.getValue();

		File pathPIAManagerData = new File(pathPIAManager, "data");

		// Read the status_file for forwarding port
		File fileStatus = new File(pathPIAManagerData, "status_file.txt");
		try {
			byte[] statusFileBytes = FileUtil.readFileAsByteArray(fileStatus);

			if (statusFileBytes.length > STATUS_FILE_PORT_INDEX
					&& statusFileBytes[STATUS_FILE_PORT_INDEX] == '{') {
				int endPos = STATUS_FILE_PORT_INDEX;
				while (endPos < statusFileBytes.length && statusFileBytes[endPos] > 1) {
					endPos++;
				}
				boolean gotPort = false;
				String jsonPort = new String(statusFileBytes, STATUS_FILE_PORT_INDEX,
						endPos - STATUS_FILE_PORT_INDEX);
				Map<?, ?> decodeJSON = JSONUtils.decodeJSON(jsonPort);
				if (decodeJSON.containsKey("single")) {
					Object oPort = decodeJSON.get("single");
					if (oPort == null) {
						gotPort = true;

						String user = paramUser.getValue();
						String pass = new String(paramPass.getValue(), "utf-8");

						if (user == null || user.length() == 0 || pass == null
								|| pass.length() == 0) {
							log(pi.getUtilities().getLocaleUtilities().getLocalisedMessageText(
									"pia.no.port.config"));
						}

					}
					if (oPort instanceof Number) {
						gotPort = true;

						Number nPort = (Number) oPort;
						int port = nPort.intValue();
						log("port is " + port);

						changePort(port);
					}
				}

				if (!gotPort) {
					log("read invalid port JSON from status_file: " + jsonPort);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		findBindingAddress(pathPIAManagerData);
	}

	private String calcProtocolAddresses() {
		long now = pi.getUtilities().getCurrentSystemTime();
		String sReply = "Last Checked "
				+ pi.getUtilities().getFormatters().formatDate(now) + "\n";
		// Stolen from NetworkAdminImpl.generateDiagnostics
		// This takes some time (1s-ish), so it's better to do it on demand
		try {
			NetworkAdmin networkAdmin = NetworkAdmin.getSingleton();
			AzureusCore azureus_core = AzureusCoreFactory.getSingleton();

			NetworkAdminProtocol[] protocols = networkAdmin.getOutboundProtocols(
					azureus_core);

			for (int i = 0; i < protocols.length; i++) {

				NetworkAdminProtocol protocol = protocols[i];

				try {

					InetAddress ext_addr = networkAdmin.testProtocol(protocol);

					if (ext_addr != null) {

						//public_addresses.add( ext_addr );
					}

					String country = null;

					List<LocationProvider> locationProviders = pi.getUtilities().getLocationProviders();
					for (LocationProvider locationProvider : locationProviders) {
						country = locationProvider.getCountryNameForIP(ext_addr,
								Locale.getDefault());
						if (country != null) {
							break;
						}
					}

					String s = protocol.getName() + " - " + ext_addr
							+ (country == null ? "" : " - " + country);
					sReply += s + "\n";
					log(s);

				} catch (NetworkAdminException e) {

					String s = protocol.getName() + " - "
							+ Debug.getNestedExceptionMessage(e);
					sReply += s + "\n";
					log(s);
				}
			}

		} catch (Exception e) {
			String s = "Nat Devices: Can't get -> " + e.toString();
			sReply += s + "\n";
			log(s);
		}

		buttonProtocolAddresses.setLabelText(sReply);
		return sReply;
	}

	private void findBindingAddress(File pathPIAManagerData) {
		// Find our VPN binding (interface) address.  Checking UDP is the best bet,
		// since TCP and http might be proxied
		List<PRUDPPacketHandler> handlers = PRUDPPacketHandlerFactory.getHandlers();
		if (handlers.size() > 0) {
			InetAddress bindIP = handlers.get(0).getBindIP();
			log("UDP is bound to " + bindIP);

			InetAddress[] resolve = null;
			try {
				// Let's assume the client_id.txt file is the one for port forwarding.
				File fileClientID = new File(pathPIAManagerData, "client_id.txt");
				String clientID = FileUtil.readFileAsString(fileClientID, -1);

				HttpPost post = new HttpPost(PIA_RPC_URL);

				String user = paramUser.getValue();
				String pass = new String(paramPass.getValue(), "utf-8");

				if (user == null || user.length() == 0 || pass == null
						|| pass.length() == 0) {
					return;
				}

				List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
				urlParameters.add(new BasicNameValuePair("user", user));
				urlParameters.add(new BasicNameValuePair("pass", pass));
				urlParameters.add(new BasicNameValuePair("client_id", clientID));
				urlParameters.add(
						new BasicNameValuePair("local_ip", bindIP.getHostAddress()));

				// Call needs to be from the VPN interface (the bindIP)
				RequestConfig requestConfig = RequestConfig.custom().setLocalAddress(
						bindIP).setConnectTimeout(10000).build();

				post.setConfig(requestConfig);

				post.setEntity(new UrlEncodedFormEntity(urlParameters));

				CloseableHttpClient httpClient = HttpClients.createDefault();

				// If Vuze has a proxy set up (Tools->Options->Connection->Proxy), then
				// we'll need to disable it for the URL
				AEProxySelector selector = AEProxySelectorFactory.getSelector();
				if (selector != null) {
					resolve = SystemDefaultDnsResolver.INSTANCE.resolve(PIA_DOMAIN);

					for (InetAddress address : resolve) {
						selector.setProxy(new InetSocketAddress(address, 443),
								Proxy.NO_PROXY);
					}
				}

				CloseableHttpResponse response = httpClient.execute(post);
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}

				boolean gotPort = false;
				// should be {"port":xyz}

				Map<?, ?> mapResult = JSONUtils.decodeJSON(result.toString());
				if (mapResult.containsKey("port")) {
					Object oPort = mapResult.get("port");
					if (oPort instanceof Number) {
						gotPort = true;
						Number nPort = (Number) oPort;
						int port = nPort.intValue();
						log("port returned from RPC is " + port);
						changePort(port);
					}
				}

				if (!gotPort) {
					log("RPC result: " + result);
				}
			} catch (Exception e) {
				e.printStackTrace();
				log(e.toString());
			} finally {
				AEProxySelector selector = AEProxySelectorFactory.getSelector();
				if (selector != null && resolve != null) {
					for (InetAddress address : resolve) {
						AEProxySelectorFactory.getSelector().removeProxy(
								new InetSocketAddress(address, 443));
					}
				}
			}

		} else {
			log("No UDP Handlers");
		}
	}

	private File getPIAManagerPath() {
		File pathPIAManager = null;
		String pathProgFiles = System.getenv("ProgramFiles");
		if (pathProgFiles != null) {
			pathPIAManager = new File(pathProgFiles, "pia_manager");
		}
		if (pathPIAManager == null || !pathPIAManager.exists()) {
			String pathProgFiles86 = System.getenv("ProgramFiles");
			if (pathProgFiles == null && pathProgFiles86 != null) {
				pathProgFiles86 = pathProgFiles + "(x86)";
			}
			if (pathProgFiles86 != null) {
				pathPIAManager = new File(pathProgFiles86, "pia_manager");
			}
		}
		if (pathPIAManager == null || !pathPIAManager.exists()) {
			pathPIAManager = new File("C:\\Program Files\\pia_manager");
		}
		return pathPIAManager;
	}

	private void changePort(int port) {
		PluginConfig pluginConfig = pi.getPluginconfig();
		int coreTCPPort = pluginConfig.getCoreIntParameter(
				PluginConfig.CORE_PARAM_INT_INCOMING_TCP_PORT);
		int coreUDPPort = pluginConfig.getCoreIntParameter(
				PluginConfig.CORE_PARAM_INT_INCOMING_UDP_PORT);
		if (coreTCPPort != port) {
			pluginConfig.setCoreIntParameter(
					PluginConfig.CORE_PARAM_INT_INCOMING_TCP_PORT, port);
			log("Changed core TCP port from " + coreTCPPort + " to " + port);
		}
		if (coreUDPPort != port) {
			pluginConfig.setCoreIntParameter(
					PluginConfig.CORE_PARAM_INT_INCOMING_UDP_PORT, port);
			log("Changed core UDP port from " + coreUDPPort + " to " + port);
		}
	}
}
