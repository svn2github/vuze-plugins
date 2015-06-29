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
import java.net.*;
import java.util.*;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.message.BasicNameValuePair;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.admin.*;
import com.aelitis.azureus.core.proxy.AEProxySelector;
import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPReleasablePacketHandler;

public class CheckerPIA
{
	private static final char CHAR_GOOD = '\u2714';

	private static final char CHAR_BAD = '\u2716';

	private static final char CHAR_WARN = '\u2318';

	private static final int STATUS_ID_OK = 0;

	private static final int STATUS_ID_BAD = 1;

	private static final int STATUS_ID_WARN = 2;

	// Is it always 70000? who knows
	private static final int STATUS_FILE_PORT_INDEX = 70000;

	private static final String PIA_DOMAIN = "www.privateinternetaccess.com";

	private static final String PIA_RPC_URL = "https://" + PIA_DOMAIN
			+ "/vpninfo/port_forward_assignment";

	private static final boolean rebindNetworkInterface = true;

	private UTTimer timer;

	private PluginConfig config;

	private PluginInterface pi;

	private List<CheckerPIAListener> listeners = new ArrayList<CheckerPIAListener>(
			1);

	private String lastProtocolAddresses = "";

	private String lastPortCheckStatus = "";

	private boolean checkingPortBinding;

	private LocaleUtilities texts;

	private InetAddress testSocketAddress;

	private InetAddress vpnIP;

	private int currentStatusID = -1;

	public CheckerPIA(PluginInterface pi) {
		this.pi = pi;
		this.config = pi.getPluginconfig();
		this.texts = pi.getUtilities().getLocaleUtilities();

		try {
			testSocketAddress = InetAddress.getByAddress(new byte[] {
				1,
				1,
				1,
				1
			});
		} catch (UnknownHostException e) {
		}

	}

	protected void buildTimer() {
		if (timer != null) {
			timer.destroy();
			timer = null;
		}
		int mins = config.getPluginIntParameter(PluginPIA.CONFIG_CHECK_MINUTES);
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

	protected String getDefaultUsername() {
		try {
			String pathPIAManager = config.getPluginStringParameter(
					PluginPIA.CONFIG_PIA_MANAGER_DIR);

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

	public String portBindingCheck() {
		synchronized (this) {
			if (checkingPortBinding) {
				return lastPortCheckStatus;
			}
			checkingPortBinding = true;
		}

		CheckerPIAListener[] triggers = listeners.toArray(
				new CheckerPIAListener[0]);
		for (CheckerPIAListener l : triggers) {
			try {
				l.portCheckStart();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		StringBuilder sReply = new StringBuilder();

		String pathPIAManager = config.getPluginStringParameter(
				PluginPIA.CONFIG_PIA_MANAGER_DIR);

		File pathPIAManagerData = new File(pathPIAManager, "data");

		try {
			int newStatusID = findBindingAddress(pathPIAManagerData, sReply);

			boolean rpcCalled = false;
			if (newStatusID != STATUS_ID_BAD && vpnIP != null) {
				rpcCalled = callRPCforPort(pathPIAManagerData, vpnIP, sReply);
			}

			if (!rpcCalled) {
				boolean gotPort = checkStatusFileForPort(pathPIAManagerData, sReply);
				if (!gotPort && newStatusID != STATUS_ID_BAD) {
					newStatusID = STATUS_ID_WARN;
				}
			}

			if (newStatusID != -1) {
				currentStatusID = newStatusID;
			}
			if (newStatusID == STATUS_ID_BAD) {
				sReply.insert(0, "VPN NOT SET UP CORRECTLY\n");
			} else if (newStatusID == STATUS_ID_OK) {
				sReply.insert(0, "VPN OK!\n");
			} else if (newStatusID == STATUS_ID_WARN) {
				sReply.insert(0, "VPN HAS WARNINGS\n");
			}

		} catch (Throwable t) {
			t.printStackTrace();
			PluginPIA.log(t.toString());
		}

		lastPortCheckStatus = sReply.toString();

		triggers = listeners.toArray(new CheckerPIAListener[0]);
		for (CheckerPIAListener l : triggers) {
			try {
				l.portCheckStatusChanged(lastPortCheckStatus);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		synchronized (this) {
			checkingPortBinding = false;
		}
		return lastPortCheckStatus;
	}

	private boolean checkStatusFileForPort(File pathPIAManagerData,
			StringBuilder sReply) {
		// Read the status_file for forwarding port

		boolean gotValidPort = false;

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

						String user = config.getPluginStringParameter(
								PluginPIA.CONFIG_USER);
						byte[] pass = config.getPluginByteParameter(PluginPIA.CONFIG_P);

						if (user == null || user.length() == 0 || pass == null
								|| pass.length == 0) {

							boolean portForwardEnabled = false;
							File fileSettings = new File(pathPIAManagerData, "settings.json");
							String settingsString = FileUtil.readFileAsString(fileSettings,
									-1);
							Map<?, ?> mapSettings = JSONUtils.decodeJSON(settingsString);
							if (mapSettings != null
									&& mapSettings.containsKey("portforward")) {
								portForwardEnabled = (Boolean) mapSettings.get("portforward");
							}

							String s = texts.getLocalisedMessageText(portForwardEnabled
									? "pia.no.forwarding.port" : "pia.no.port.config");
							sReply.append(s).append("\n");
							PluginPIA.log(s);
						}

					}
					if (oPort instanceof Number) {
						gotPort = true;
						gotValidPort = true;

						Number nPort = (Number) oPort;
						int port = nPort.intValue();

						String s = CHAR_GOOD + " Port stored in PIA Manager config is "
								+ port;
						sReply.append(s).append("\n");
						PluginPIA.log(s);

						changePort(port, sReply);
					}
				}

				if (!gotPort) {
					String s = CHAR_BAD + " Read invalid port JSON from status_file: "
							+ jsonPort;
					sReply.append(s).append("\n");
					PluginPIA.log(s);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return gotValidPort;
	}

	public String calcProtocolAddresses() {
		long now = pi.getUtilities().getCurrentSystemTime();
		StringBuffer sReply = new StringBuffer("Last Checked ").append(
				pi.getUtilities().getFormatters().formatDate(now)).append("\n");
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

					String country = null;
					if (ext_addr != null) {

						List<LocationProvider> locationProviders = pi.getUtilities().getLocationProviders();
						for (LocationProvider locationProvider : locationProviders) {
							country = locationProvider.getCountryNameForIP(ext_addr,
									Locale.getDefault());
							if (country != null) {
								break;
							}
						}
					}

					String s = protocol.getName() + " - " + ext_addr
							+ (country == null ? "" : " - " + country);
					sReply.append(s).append("\n");
					PluginPIA.log(s);

				} catch (NetworkAdminException e) {

					String s = protocol.getName() + " - "
							+ Debug.getNestedExceptionMessage(e);
					sReply.append(s).append("\n");
					PluginPIA.log(s);
				}
			}

		} catch (Exception e) {
			String s = "Nat Devices: Can't get -> " + e.toString();
			sReply.append(s).append("\n");
			PluginPIA.log(s);
		}

		lastProtocolAddresses = sReply.toString();

		CheckerPIAListener[] triggers = listeners.toArray(
				new CheckerPIAListener[0]);
		for (CheckerPIAListener l : triggers) {
			try {
				l.protocolAddressesStatusChanged(lastProtocolAddresses);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return lastProtocolAddresses;
	}

	private int findBindingAddress(File pathPIAManagerData,
			StringBuilder sReply) {
		int newStatusID = -1;

		// Find our VPN binding (interface) address.  Checking UDP is the best bet,
		// since TCP and http might be proxied
		List<PRUDPPacketHandler> handlers = PRUDPPacketHandlerFactory.getHandlers();
		if (handlers.size() == 0) {
			PRUDPReleasablePacketHandler releasableHandler = PRUDPPacketHandlerFactory.getReleasableHandler(
					0);
			PluginPIA.log("derp");
			handlers = PRUDPPacketHandlerFactory.getHandlers();
			releasableHandler.release();
		}
		if (handlers.size() == 0) {
			String s = CHAR_BAD + " No UDP Handlers";
			sReply.append(s).append("\n");
			PluginPIA.log(s);

			newStatusID = STATUS_ID_BAD;
		} else {

			InetAddress bindIP = handlers.get(0).getBindIP();

			// The "Any" field is equivalent to 0.0.0.0 in dotted-quad notation, which is unbound.
			// "Loopback" is 127.0.0.1, which is bound when Vuze can't bind to
			// user specified interface (ie. kill switched)
			if (bindIP.isAnyLocalAddress() || bindIP.isLoopbackAddress()) {
				newStatusID = handleUnboundOrLoopback(bindIP, sReply);
				if (newStatusID == STATUS_ID_BAD) {
					return newStatusID;
				}
			} else {
				newStatusID = handleBound(bindIP, sReply);
			}
		}
		return newStatusID;
	}

	private boolean callRPCforPort(File pathPIAManagerData, InetAddress bindIP,
			StringBuilder sReply) {
		InetAddress[] resolve = null;
		try {
			// Let's assume the client_id.txt file is the one for port forwarding.
			File fileClientID = new File(pathPIAManagerData, "client_id.txt");
			String clientID = FileUtil.readFileAsString(fileClientID, -1);

			HttpPost post = new HttpPost(PIA_RPC_URL);

			String user = config.getPluginStringParameter(PluginPIA.CONFIG_USER);
			String pass = new String(
					config.getPluginByteParameter(PluginPIA.CONFIG_P, new byte[0]),
					"utf-8");

			if (user == null || user.length() == 0 || pass == null
					|| pass.length() == 0) {
				return false;
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

					String s = CHAR_GOOD + " Port returned from RPC is " + port;
					sReply.append(s).append("\n");
					PluginPIA.log(s);
					changePort(port, sReply);
				}
			}

			if (!gotPort) {
				String s = CHAR_WARN + " RPC result: " + result;
				sReply.append(s).append("\n");
				PluginPIA.log(s);

				// mapResult.containsKey("error")
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			String s = CHAR_BAD + " Can't connect to PIA RPC via " + bindIP + ": "
					+ e.getMessage();
			sReply.append(s).append("\n");
			PluginPIA.log(s);

			return false;
		} finally {
			AEProxySelector selector = AEProxySelectorFactory.getSelector();
			if (selector != null && resolve != null) {
				for (InetAddress address : resolve) {
					AEProxySelectorFactory.getSelector().removeProxy(
							new InetSocketAddress(address, 443));
				}
			}
		}
		return true;
	}

	private int handleBound(InetAddress bindIP, StringBuilder sReply) {
		int newStatusID = STATUS_ID_OK;

		String s;
		boolean isGoodExistingBind = bindIP.getHostAddress().startsWith("10.");
		if (isGoodExistingBind) {
			String niName = "Unknown Interface";
			try {
				NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
						bindIP);
				niName = networkInterface.getName() + " ("
						+ networkInterface.getDisplayName() + ")";
			} catch (Throwable e) {
			}
			s = CHAR_GOOD + " Vuze UDP is bound to " + bindIP + " aka " + niName;
			vpnIP = bindIP;
		} else {
			s = CHAR_BAD + " Vuze UDP is bound to non PIA address of " + bindIP;
			newStatusID = STATUS_ID_BAD;
		}
		sReply.append(s).append("\n");
		PluginPIA.log(s);

		try {
			// Check if default routing goes through 10.*, by connecting to address
			// via socket.  Address doesn't need to be reachable, just routable.
			DatagramSocket socket = new DatagramSocket();
			socket.connect(testSocketAddress, 0);
			InetAddress localAddress = socket.getLocalAddress();
			NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
					localAddress);

			s = "Non-Vuze probably routing through " + localAddress + " aka "
					+ networkInterface.getName() + " ("
					+ networkInterface.getDisplayName() + ").";
			if ((localAddress instanceof Inet4Address)
					&& localAddress.getHostAddress().startsWith("10.")) {

				if (localAddress.equals(bindIP)) {
					s = (isGoodExistingBind ? CHAR_GOOD : CHAR_WARN) + " " + s
							+ " Same as Vuze";
				} else {
					// Vuze is bound, default routing goes somewhere else
					// This is ok, since Vuze will not accept incoming from "somewhere else"
					// We'll warn, but not update the status id

					s = CHAR_WARN + " " + s + " WARNING, not the same as Vuze!";

					if (isGoodExistingBind) {
						s += " " + texts.getLocalisedMessageText(
								"default.routing.not.vpn.network.splitting");
					}
				}

				sReply.append(s).append("\n");
				PluginPIA.log(s);

				if (!isGoodExistingBind && rebindNetworkInterface) {
					rebindNetworkInterface(networkInterface, localAddress, sReply);
					// Should we redo test?
				}

			} else {
				// Vuze is bound, default routing goes to somewhere else.
				// Probably network splitting
				s = (isGoodExistingBind ? CHAR_WARN : CHAR_BAD) + " " + s;
				if (isGoodExistingBind) {
					s += " " + texts.getLocalisedMessageText(
							"default.routing.not.vpn.network.splitting");
				}
				sReply.append(s).append("\n");
				PluginPIA.log(s);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return newStatusID;
	}

	private int handleUnboundOrLoopback(InetAddress bindIP,
			StringBuilder sReply) {

		int newStatusID = STATUS_ID_OK;

		InetAddress newBindIP = null;
		NetworkInterface newBindNetworkInterface = null;

		String s;

		if (bindIP.isAnyLocalAddress()) {
			s = CHAR_WARN + " Warning: Vuze UDP is unbound (We'll try to fix this)";
		} else {
			s = CHAR_BAD
					+ " Vuze UDP is loopback (Kill Switched or incorrect bind address/interface)";
		}
		sReply.append(s).append("\n");
		PluginPIA.log(s);

		try {
			NetworkAdmin networkAdmin = NetworkAdmin.getSingleton();

			// Find a bindable address that starts with 10.
			InetAddress[] bindableAddresses = networkAdmin.getBindableAddresses();

			for (InetAddress bindableAddress : bindableAddresses) {
				if (bindableAddress.getHostAddress().startsWith("10.")) {
					newBindIP = bindableAddress;
					newBindNetworkInterface = NetworkInterface.getByInetAddress(
							newBindIP);

					s = CHAR_GOOD + " Found bindable address " + newBindIP
							+ " which is likely VPN's local IP";
					sReply.append(s).append("\n");
					PluginPIA.log(s);

					break;
				}
			}

			// Find a Network Interface that has an address that starts with 10.
			NetworkAdminNetworkInterface[] interfaces = networkAdmin.getInterfaces();

			boolean foundNIF = false;
			for (NetworkAdminNetworkInterface networkAdminInterface : interfaces) {
				NetworkAdminNetworkInterfaceAddress[] addresses = networkAdminInterface.getAddresses();
				for (NetworkAdminNetworkInterfaceAddress a : addresses) {
					InetAddress address = a.getAddress();
					if (address instanceof Inet4Address) {
						if (address.getHostAddress().startsWith("10.")) {
							s = "Possible PIA VPN at " + address + " on "
									+ networkAdminInterface.getDisplayName() + "/"
									+ networkAdminInterface.getName();

							if (newBindIP == null) {
								foundNIF = true;
								newBindIP = address;

								// Either one should work
								//newBindNetworkInterface = NetworkInterface.getByInetAddress(newBindIP);
								newBindNetworkInterface = NetworkInterface.getByName(
										networkAdminInterface.getName());

								s += ".  Assuming VPN";
							} else if (address.equals(newBindIP)) {
								s = CHAR_GOOD + " " + s + ".  Same as address above :)";
								foundNIF = true;
							} else {
								if (newStatusID != STATUS_ID_BAD) {
									newStatusID = STATUS_ID_WARN;
								}
								s = CHAR_WARN + " " + s + ".  WARNING, not the same as above!";
							}

							sReply.append(s).append("\n");
							PluginPIA.log(s);

							if (rebindNetworkInterface) {
								// stops message below from being added, we'll rebind later
								foundNIF = true;
							}

						}
					}
				}
			}

			if (!foundNIF) {
				s = CHAR_BAD
						+ " Could not find PIA's network interface.  Perhaps your VPN is disconnected?";
				sReply.append(s).append("\n");
				PluginPIA.log(s);
			}

			// Check if default routing goes through 10.*, by connecting to address
			// via socket.  Address doesn't need to be reachable, just routable.
			DatagramSocket socket = new DatagramSocket();
			socket.connect(testSocketAddress, 0);
			InetAddress localAddress = socket.getLocalAddress();
			NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
					localAddress);

			s = "Non-Vuze probably routing through " + localAddress + " aka "
					+ networkInterface.getName() + " ("
					+ networkInterface.getDisplayName() + ").";

			if ((localAddress instanceof Inet4Address)
					&& localAddress.getHostAddress().startsWith("10.")) {

				if (newBindIP == null) {
					newBindIP = localAddress;
					newBindNetworkInterface = networkInterface;

					s = CHAR_GOOD + " " + s + "  Assuming VPN";
				} else if (localAddress.equals(newBindIP)) {
					s = CHAR_GOOD + " " + s + "  Same as address above :)";
				} else {
					// Vuze not bound. We already found a boundable address, but it's not this one
					/* Possibly good case:
					 * - Vuze: unbound
					 * - Found Bindable: 10.100.1.6
					 * - Default Routing: 10.255.1.1
					 * -> Split network
					 */
					if (newStatusID != STATUS_ID_BAD) {
						newStatusID = STATUS_ID_WARN;
					}
					s = CHAR_WARN + " " + s
							+ "  WARNING, not the same as the one we will try to bind to! "
							+ texts.getLocalisedMessageText(
									"default.routing.not.vpn.network.splitting")
							+ " " + texts.getLocalisedMessageText(
									"default.routing.not.vpn.network.splitting.unbound");
				}

				sReply.append(s).append("\n");
				PluginPIA.log(s);

			} else {
				s = CHAR_WARN + " " + s;
				if (!bindIP.isLoopbackAddress()) {
					s += " " + texts.getLocalisedMessageText(
							"default.routing.not.vpn.network.splitting");
				}

				if (newBindIP == null && foundNIF) {
					if (newStatusID != STATUS_ID_BAD) {
						newStatusID = STATUS_ID_WARN;
					}
					s += " " + texts.getLocalisedMessageText(
							"default.routing.not.vpn.network.splitting.unbound");
				}

				sReply.append(s).append("\n");
				PluginPIA.log(s);
			}

		} catch (Exception e) {
			s = CHAR_BAD + " Nat Devices: Can't get -> " + e.toString();
			sReply.append(s).append("\n");
			PluginPIA.log(s);
		}

		if (newBindIP == null) {
			s = CHAR_BAD + " Can't determine VPN's local IP";
			sReply.append(s).append("\n");
			PluginPIA.log(s);
			return STATUS_ID_BAD;
		}

		rebindNetworkInterface(newBindNetworkInterface, newBindIP, sReply);
		return newStatusID;
	}

	/**
	 * @return rebind sucessful, or rebinding to already bound address
	 */
	private boolean rebindNetworkInterface(NetworkInterface networkInterface,
			InetAddress onlyToAddress, final StringBuilder sReply) {
		vpnIP = onlyToAddress;

		/**
		if (true) {
			sReply.append("Would rebind to " + networkInterface.getDisplayName()
					+ onlyToAddress + "\n");
			return false;
		}
		/**/
		String ifName = networkInterface.getName();

		String configBindIP = config.getCoreStringParameter(
				PluginConfig.CORE_PARAM_STRING_LOCAL_BIND_IP);

		int bindNetworkInterfaceIndex = -1;
		if (onlyToAddress != null) {
			Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
			for (int j = 0; inetAddresses.hasMoreElements(); j++) {
				InetAddress element = inetAddresses.nextElement();
				if (element.equals(onlyToAddress)) {
					bindNetworkInterfaceIndex = j;
					break;
				}
			}
		}

		if (configBindIP.equals(ifName)
				|| (bindNetworkInterfaceIndex >= 0 && configBindIP.equals(
						ifName + "[" + bindNetworkInterfaceIndex + "]"))) {

			String s = CHAR_GOOD + " Excellent, Vuze already bound to " + ifName;
			sReply.append(s).append("\n");
			PluginPIA.log(s);
		} else {
			String newConfigBindIP = ifName;
			if (bindNetworkInterfaceIndex >= 0) {
				newConfigBindIP += "[" + bindNetworkInterfaceIndex + "]";
			}

			final AESemaphore sem = new AESemaphore("PIA BindWait");

			NetworkAdmin.getSingleton().addPropertyChangeListener(
					new NetworkAdminPropertyChangeListener() {
						public void propertyChanged(String property) {
							if (property.equals(NetworkAdmin.PR_DEFAULT_BIND_ADDRESS)) {
								sem.releaseForever();
								NetworkAdmin.getSingleton().removePropertyChangeListener(this);
								String s = CHAR_GOOD + " Received binding applied event";

								sReply.append(s).append("\n");
								PluginPIA.log(s);
							}
						}
					});

			config.setCoreStringParameter(
					PluginConfig.CORE_PARAM_STRING_LOCAL_BIND_IP, newConfigBindIP);
			// XXX What about Enforce Bind IP
			/*
			 * Vuze Core does this when generic VPN detected:
			 		COConfigurationManager.setParameter( "User Mode", 2 );
					COConfigurationManager.setParameter( "Bind IP", intf.getName());
					COConfigurationManager.setParameter( "Enforce Bind IP", true );
					COConfigurationManager.setParameter( "Check Bind IP On Start", true );
			 */

			String s = CHAR_GOOD + " Change Vuze binding to interface "
					+ newConfigBindIP + " (" + networkInterface.getDisplayName() + ")";
			sReply.append(s).append("\n");
			PluginPIA.log(s);

			sem.reserve(11000);
			return sem.isReleasedForever();
		}
		return true;
	}

	protected File getPIAManagerPath() {
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

	private void changePort(int port, StringBuilder sReply) {

		PluginConfig pluginConfig = pi.getPluginconfig();
		int coreTCPPort = pluginConfig.getCoreIntParameter(
				PluginConfig.CORE_PARAM_INT_INCOMING_TCP_PORT);
		int coreUDPPort = pluginConfig.getCoreIntParameter(
				PluginConfig.CORE_PARAM_INT_INCOMING_UDP_PORT);
		if (coreTCPPort != port) {
			pluginConfig.setCoreIntParameter(
					PluginConfig.CORE_PARAM_INT_INCOMING_TCP_PORT, port);
			String s = CHAR_GOOD + " Changed core TCP port from " + coreTCPPort
					+ " to " + port;
			sReply.append(s).append("\n");
			PluginPIA.log(s);
		}
		if (coreUDPPort != port) {
			pluginConfig.setCoreIntParameter(
					PluginConfig.CORE_PARAM_INT_INCOMING_UDP_PORT, port);
			String s = CHAR_GOOD + " Changed core UDP port from " + coreUDPPort
					+ " to " + port;
			sReply.append(s).append("\n");
			PluginPIA.log(s);
		}
	}

	public void addListener(CheckerPIAListener l) {
		listeners.add(l);
		try {
			l.portCheckStatusChanged(lastPortCheckStatus);
			l.protocolAddressesStatusChanged(lastProtocolAddresses);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeListener(CheckerPIAListener l) {
		listeners.remove(l);
	}

	public int getCurrentStatusID() {
		return currentStatusID;
	}
}
