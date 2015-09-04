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

package com.vuze.plugin.azVPN_Air;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.message.BasicNameValuePair;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.xml.simpleparser.SimpleXMLParserDocumentFactory;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.admin.*;
import com.aelitis.azureus.core.proxy.AEProxySelector;
import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;
import com.aelitis.azureus.util.MapUtils;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPReleasablePacketHandler;

public class Checker
{
	private static final char CHAR_GOOD = '\u2714';

	private static final char CHAR_BAD = '\u2716';

	private static final char CHAR_WARN = '\u2318';

	public static final int STATUS_ID_OK = 0;

	public static final int STATUS_ID_BAD = 1;

	public static final int STATUS_ID_WARN = 2;

	private static final String VPN_DOMAIN = "airvpn.org";

	private static final boolean rebindNetworkInterface = true;

	private static final String VPN_LOGIN_URL = "https://" + VPN_DOMAIN
			+ "/login";

	private static final String VPN_PORTS_URL = "https://" + VPN_DOMAIN
			+ "/ports/";

	private static final String REGEX_ActionURL = "action=\"([^\"]+)\"";

	private static final String REGEX_AuthKey = "name=['\"]auth_key['\"]\\s*value=['\"]([^\"']+)['\"]";

	private static final String REGEX_Port = "class=\"ports_port\">([0-9]+)<";

	private static final String REGEX_Token = "name=['\"]csrf_token['\"]\\s*value=['\"]([^\"']+)['\"]";

	private static final String REGEX_PortForwardedTo = "<td>Forwarded to:</td><td>([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)</td>";

	private UTTimer timer;

	private PluginConfig config;

	private PluginInterface pi;

	private List<CheckerListener> listeners = new ArrayList<CheckerListener>(1);

	private String lastProtocolAddresses = "";

	private String lastPortCheckStatus = "";

	private boolean checkingPortBinding;

	private LocaleUtilities texts;

	private InetAddress testSocketAddress;

	private InetAddress vpnIP;

	private int currentStatusID = -1;

	private HttpClientContext httpClientContext;

	public Checker() {
	}

	public Checker(PluginInterface pi) {
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

	public void destroy() {
		if (timer != null) {
			timer.destroy();
			timer = null;
		}
		listeners.clear();
	}

	protected void buildTimer() {
		if (timer != null) {
			timer.destroy();
			timer = null;
		}
		int mins = config.getPluginIntParameter(PluginAir.CONFIG_CHECK_MINUTES);
		if (mins == 0) {
			return;
		}
		timer = pi.getUtilities().createTimer("AirVPN");
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
			File vpnConfigPath = getVPNConfigPath();
			if (vpnConfigPath == null) {
				return "";
			}

			File fileSettings = new File(vpnConfigPath, "AirVPN.xml");
			if (!fileSettings.isFile() || !fileSettings.canRead()) {
				return "";
			}
			SimpleXMLParserDocument xml = SimpleXMLParserDocumentFactory.create(
					fileSettings);
			SimpleXMLParserDocumentNode options = xml.getChild("options");
			SimpleXMLParserDocumentNode[] children = options.getChildren();

			Map<String, String> mapOptions = new HashMap<String, String>();

			for (SimpleXMLParserDocumentNode child : children) {
				SimpleXMLParserDocumentAttribute name = child.getAttribute("name");
				if (name != null) {
					SimpleXMLParserDocumentAttribute value = child.getAttribute("value");
					if (value != null) {
						mapOptions.put(name.getValue(), value.getValue());
					}
				}
			}
			String user = MapUtils.getMapString(mapOptions, "login", "");

			return user;
		} catch (Exception e) {
			PluginAir.log("Get login name: " + e.toString());
			return "";
		}
	}

	protected String getPassword() {
		try {
			File vpnConfigPath = getVPNConfigPath();
			if (vpnConfigPath == null) {
				return "";
			}

			File fileSettings = new File(vpnConfigPath, "AirVPN.xml");
			if (!fileSettings.isFile() || !fileSettings.canRead()) {
				return "";
			}
			SimpleXMLParserDocument xml = SimpleXMLParserDocumentFactory.create(
					fileSettings);
			SimpleXMLParserDocumentNode options = xml.getChild("options");
			SimpleXMLParserDocumentNode[] children = options.getChildren();

			Map<String, String> mapOptions = new HashMap<String, String>();

			for (SimpleXMLParserDocumentNode child : children) {
				SimpleXMLParserDocumentAttribute name = child.getAttribute("name");
				if (name != null) {
					SimpleXMLParserDocumentAttribute value = child.getAttribute("value");
					if (value != null) {
						mapOptions.put(name.getValue(), value.getValue());
					}
				}
			}
			String pw = MapUtils.getMapString(mapOptions, "password", "");

			return pw;
		} catch (Exception e) {
			PluginAir.log("Get login creds: " + e.toString());
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

		CheckerListener[] triggers = listeners.toArray(new CheckerListener[0]);
		for (CheckerListener l : triggers) {
			try {
				l.portCheckStart();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		StringBuilder sReply = new StringBuilder();

		try {
			int newStatusID = findBindingAddress(sReply);

			boolean doPortForwarding = config.getPluginBooleanParameter(
					PluginAir.CONFIG_DO_PORT_FORWARDING);

			if (doPortForwarding) {
				boolean rpcCalled = false;
				if (newStatusID != STATUS_ID_BAD && vpnIP != null) {
					rpcCalled = callRPCforPort(vpnIP, sReply);
				}

				if (!rpcCalled) {
					if (newStatusID != STATUS_ID_BAD) {
						newStatusID = STATUS_ID_WARN;

						addReply(sReply, CHAR_WARN, "airvpn.port.forwarding.get.failed");
					}
				}
			}

			if (newStatusID != -1) {
				currentStatusID = newStatusID;
			}
			String msgID = null;
			if (newStatusID == STATUS_ID_BAD) {
				msgID = "airvpn.topline.bad";
			} else if (newStatusID == STATUS_ID_OK) {
				msgID = "airvpn.topline.ok";
			} else if (newStatusID == STATUS_ID_WARN) {
				msgID = "airvpn.topline.warn";
			}
			if (msgID != null) {
				sReply.insert(0, texts.getLocalisedMessageText(msgID) + "\n");
			}

		} catch (Throwable t) {
			t.printStackTrace();
			PluginAir.log(t.toString());
		}

		lastPortCheckStatus = sReply.toString();

		triggers = listeners.toArray(new CheckerListener[0]);
		for (CheckerListener l : triggers) {
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

	public String calcProtocolAddresses() {
		long now = pi.getUtilities().getCurrentSystemTime();
		StringBuilder sReply = new StringBuilder("Last Checked ").append(
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

					addLiteralReply(sReply, protocol.getName() + " - " + ext_addr
							+ (country == null ? "" : " - " + country));

				} catch (NetworkAdminException e) {

					addLiteralReply(sReply,
							protocol.getName() + " - " + Debug.getNestedExceptionMessage(e));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			addReply(sReply, CHAR_BAD, "airvpn.nat.error", new String[] {
				e.toString()
			});
		}

		lastProtocolAddresses = sReply.toString();

		CheckerListener[] triggers = listeners.toArray(new CheckerListener[0]);
		for (CheckerListener l : triggers) {
			try {
				l.protocolAddressesStatusChanged(lastProtocolAddresses);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return lastProtocolAddresses;
	}

	private void addReply(StringBuilder sReply, char prefix, String id) {
		String s = (prefix == 0 ? "" : prefix + " ")
				+ texts.getLocalisedMessageText(id);
		addLiteralReply(sReply, s);
	}

	private void addReply(StringBuilder sReply, char prefix, String id,
			String[] params) {
		String s = (prefix == 0 ? "" : prefix + " ")
				+ (texts == null ? "!" + id + "!" + Arrays.toString(params)
						: texts.getLocalisedMessageText(id, params));
		addLiteralReply(sReply, s);
	}

	private void addLiteralReply(StringBuilder sReply, String s) {
		sReply.append(s).append("\n");
		PluginAir.log(s);
	}

	private int findBindingAddress(StringBuilder sReply) {
		int newStatusID = -1;

		// Find our VPN binding (interface) address.  Checking UDP is the best bet,
		// since TCP and http might be proxied
		List<PRUDPPacketHandler> handlers = PRUDPPacketHandlerFactory.getHandlers();
		if (handlers.size() == 0) {
			PRUDPReleasablePacketHandler releasableHandler = PRUDPPacketHandlerFactory.getReleasableHandler(
					0);
			handlers = PRUDPPacketHandlerFactory.getHandlers();
			releasableHandler.release();
		}
		if (handlers.size() == 0) {
			addLiteralReply(sReply, CHAR_BAD + " No UDP Handlers");

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

	private boolean callRPCforPort(InetAddress bindIP, StringBuilder sReply) {
		InetAddress[] resolve = null;
		try {

			String user = getDefaultUsername();
			String pass = null;
			if (user == null || user.length() == 0) {
				user = config.getPluginStringParameter(PluginAir.CONFIG_USER);
				pass = new String(
						config.getPluginByteParameter(PluginAir.CONFIG_P, new byte[0]),
						"utf-8");
			} else {
				pass = getPassword();
			}

			if (user == null || user.length() == 0 || pass == null
					|| pass.length() == 0) {
				addReply(sReply, CHAR_WARN, "airvpn.rpc.nocreds");
				return false;
			}

			// If Vuze has a proxy set up (Tools->Options->Connection->Proxy), then
			// we'll need to disable it for the URL
			AEProxySelector selector = AEProxySelectorFactory.getSelector();
			if (selector != null) {
				resolve = SystemDefaultDnsResolver.INSTANCE.resolve(VPN_DOMAIN);

				for (InetAddress address : resolve) {
					selector.setProxy(new InetSocketAddress(address, 443),
							Proxy.NO_PROXY);
				}
			}

			RequestConfig requestConfig;
			StringBuffer token = new StringBuffer();

			boolean skipLoginPage = false;
			boolean alreadyLoggedIn = false;
			PortInfo[] ports = null;

			if (httpClientContext == null) {
				httpClientContext = HttpClientContext.create();
			} else {
				PluginAir.log(
						"Have existing context.  Trying to grab port list without logging in.");
				ports = scrapePorts(bindIP, token);
				// assume no token means we aren't logged in
				if (token.length() > 0) {
					PluginAir.log("Valid ports page. Skipping Login");
					skipLoginPage = true;
					alreadyLoggedIn = true;
				} else {
					ports = null;
				}
			}

			if (!skipLoginPage) {
				String loginURL = null;
				String authKey = null;

				PluginAir.log("Getting Login post URL and auth_key");

				HttpGet getLoginPage = new HttpGet(VPN_LOGIN_URL);
				requestConfig = RequestConfig.custom().setLocalAddress(
						bindIP).setConnectTimeout(10000).build();
				getLoginPage.setConfig(requestConfig);

				CloseableHttpClient httpClientLoginPage = HttpClients.createDefault();
				CloseableHttpResponse loginPageResponse = httpClientLoginPage.execute(
						getLoginPage, httpClientContext);

				BufferedReader rd = new BufferedReader(
						new InputStreamReader(loginPageResponse.getEntity().getContent()));

				Pattern patAuthKey = Pattern.compile(REGEX_AuthKey);

				String line = "";
				while ((line = rd.readLine()) != null) {
					if (line.contains("<form")
							&& line.matches(".*id=['\"]login['\"].*")) {
						Matcher matcher = Pattern.compile(REGEX_ActionURL).matcher(line);
						if (matcher.find()) {
							loginURL = matcher.group(1);
							if (authKey != null) {
								break;
							}
						}
					}
					Matcher matcherAuthKey = patAuthKey.matcher(line);
					if (matcherAuthKey.find()) {
						authKey = matcherAuthKey.group(1);
						if (loginURL != null) {
							break;
						}
					}

					if (line.contains("['member_id']")
							&& line.matches(".*parseInt\\s*\\(\\s*[1-9][0-9]*\\s*.*")) {
						alreadyLoggedIn = true;
					}
				}
				rd.close();

				if (loginURL == null) {
					PluginAir.log("Could not scrape Login URL.  Using default");
					loginURL = "https://airvpn.org/index.php?app=core&module=global&section=login&do=process";
				}
				if (authKey == null) {
					addReply(sReply, CHAR_WARN, "airvpn.rpc.noauthkey");
					return false;
				}

				loginURL = UrlUtils.unescapeXML(loginURL);

				///////////////////////////////

				if (alreadyLoggedIn) {
					PluginAir.log("Already Logged In");
				} else {
					PluginAir.log("Login URL:" + loginURL);
					//https://airvpn.org/index.php?app=core&module=global&section=login&do=process
					//https://airvpn.org/index.php?app=core&module=global&section=login&do=process

					HttpPost httpPostLogin = new HttpPost(loginURL);

					requestConfig = RequestConfig.custom().setLocalAddress(
							bindIP).setConnectTimeout(10000).build();

					httpPostLogin.setConfig(requestConfig);

					CloseableHttpClient httpClient = HttpClients.createDefault();

					List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
					urlParameters.add(new BasicNameValuePair("ips_username", user));
					urlParameters.add(new BasicNameValuePair("ips_password", pass));
					urlParameters.add(new BasicNameValuePair("auth_key", authKey));
					urlParameters.add(new BasicNameValuePair("invisible", "1"));
					urlParameters.add(new BasicNameValuePair("inline_invisible", "1"));

					httpPostLogin.setEntity(new UrlEncodedFormEntity(urlParameters));

					CloseableHttpResponse response = httpClient.execute(httpPostLogin,
							httpClientContext);

					rd = new BufferedReader(
							new InputStreamReader(response.getEntity().getContent()));

					line = "";
					while ((line = rd.readLine()) != null) {
					}

					PluginAir.log("Login Result: " + response.getStatusLine().toString());
				}
			}

			////////////////////////////

			if (ports == null) {
				ports = scrapePorts(bindIP, token);
			}

			PluginAir.log("Found Ports: " + Arrays.toString(ports));

			int existingIndex = ourPortInList(ports);
			if (existingIndex >= 0) {
				addReply(sReply, CHAR_GOOD, "airvpn.port.from.rpc.match", new String[] {
					ports[existingIndex].port
				});
				return true;
			}

			boolean gotPort = false;

			// There's a limit of 20 ports.  If [0] isn't ours and 20 of them are
			// created, then assume our detection of "ours" is broke and just use
			// the first one
			if ((ports.length > 0 && ports[0].ourBinding) || ports.length == 20) {
				int port = Integer.parseInt(ports[0].port);
				gotPort = true;

				addReply(sReply, CHAR_GOOD, "airvpn.port.from.rpc", new String[] {
					Integer.toString(port)
				});

				changePort(port, sReply);
			} else {
				// create port
				ports = createPort(bindIP, token);
				if (ports.length == 0) {
					// form post should have got the new port, but if it didn't, try
					// reloading the ports page again.
					token.setLength(0);
					ports = scrapePorts(bindIP, token);
				}

				PluginAir.log("Added a port. Ports: " + Arrays.toString(ports));

				existingIndex = ourPortInList(ports);
				if (existingIndex >= 0) {
					addReply(sReply, CHAR_GOOD, "airvpn.port.from.rpc.match",
							new String[] {
								ports[existingIndex].port
					});
					return true;
				}

				if ((ports.length > 0 && ports[0].ourBinding) || ports.length == 20) {
					int port = Integer.parseInt(ports[0].port);
					gotPort = true;

					addReply(sReply, CHAR_GOOD, "airvpn.port.from.rpc", new String[] {
						Integer.toString(port)
					});

					changePort(port, sReply);
				}

			}

			if (!gotPort) {
				addReply(sReply, CHAR_WARN, "airvpn.rpc.no.connect", new String[] {
					bindIP.toString()
				});

				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			addReply(sReply, CHAR_BAD, "airvpn.rpc.no.connect", new String[] {
				bindIP + ": " + e.getMessage()
			});

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

	private PortInfo[] createPort(InetAddress bindIP, StringBuffer token)
			throws ClientProtocolException, IOException {
		HttpPost httpPostCreatePort = new HttpPost(VPN_PORTS_URL);

		RequestConfig requestConfig = RequestConfig.custom().setLocalAddress(
				bindIP).setConnectTimeout(10000).build();

		httpPostCreatePort.setConfig(requestConfig);

		CloseableHttpClient httpClientCreatPort = HttpClients.createDefault();

		List<NameValuePair> urlParamsAddPort = new ArrayList<NameValuePair>();
		urlParamsAddPort.add(
				new BasicNameValuePair("csrf_token", token.toString()));
		urlParamsAddPort.add(new BasicNameValuePair("action", "ports_ins"));
		urlParamsAddPort.add(new BasicNameValuePair("ports_ins_port", ""));
		urlParamsAddPort.add(new BasicNameValuePair("ports_ins_protocol", "both"));
		urlParamsAddPort.add(new BasicNameValuePair("ports_ins_local", ""));
		urlParamsAddPort.add(new BasicNameValuePair("ports_ins_dns_name", ""));
		urlParamsAddPort.add(new BasicNameValuePair("ports_ins", "ports_ins"));

		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(urlParamsAddPort);
		httpPostCreatePort.setEntity(entity);

		CloseableHttpResponse responseCreatePort = httpClientCreatPort.execute(
				httpPostCreatePort, httpClientContext);
		BufferedReader rd = new BufferedReader(
				new InputStreamReader(responseCreatePort.getEntity().getContent()));

		String bindIPString = bindIP == null ? null : bindIP.getHostAddress();

		PortInfo[] ports = parsePorts(rd, bindIPString, token);
		rd.close();

		return ports;
	}

	private PortInfo[] scrapePorts(InetAddress bindIP, StringBuffer token)
			throws ClientProtocolException, IOException {
		String bindIPString = bindIP == null ? null : bindIP.getHostAddress();
		HttpGet getPortsPage = new HttpGet(VPN_PORTS_URL);
		RequestConfig requestConfig = RequestConfig.custom().setLocalAddress(
				bindIP).setConnectTimeout(10000).build();
		getPortsPage.setConfig(requestConfig);

		CloseableHttpClient httpClientPortsPage = HttpClients.createDefault();
		CloseableHttpResponse portsPageResponse = httpClientPortsPage.execute(
				getPortsPage, httpClientContext);

		BufferedReader rd = new BufferedReader(
				new InputStreamReader(portsPageResponse.getEntity().getContent()));

		PortInfo[] ports = parsePorts(rd, bindIPString, token);

		rd.close();

		return ports;
	}

	private PortInfo[] parsePorts(BufferedReader rd, String bindIPString,
			StringBuffer token)
					throws IOException {
		Pattern patPort = Pattern.compile(REGEX_Port);
		Pattern patToken = Pattern.compile(REGEX_Token);
		Pattern patFwdToIP = Pattern.compile(REGEX_PortForwardedTo);

		Map<String, PortInfo> mapPorts = new HashMap<String, PortInfo>();

		String line = "";
		String lastPortFound = null;
		while ((line = rd.readLine()) != null) {
			Matcher matcher = patPort.matcher(line);
			boolean found = matcher.find();
			if (found) {
				while (found) {
					lastPortFound = matcher.group(1);
					mapPorts.put(lastPortFound, new PortInfo(lastPortFound, false));

					Matcher matcherFwdToIP = patFwdToIP.matcher(line);
					if (matcherFwdToIP.find()) {
						String ip = matcherFwdToIP.group(1);
						if (ip.equals(bindIPString)) {
							mapPorts.put(lastPortFound, new PortInfo(lastPortFound, true));
						}
					}

					found = matcher.find();
				}
			} else {
				if (lastPortFound != null) {
					Matcher matcherFwdToIP = patFwdToIP.matcher(line);
					if (matcherFwdToIP.find()) {
						String ip = matcherFwdToIP.group(1);
						if (ip.equals(bindIPString)) {
							mapPorts.put(lastPortFound, new PortInfo(lastPortFound, true));
						}
					}
				}
			}

			if (token != null && token.length() == 0) {
				Matcher matcherToken = patToken.matcher(line);
				if (matcherToken.find()) {
					token.append(matcherToken.group(1));
				}
			}
		}

		PortInfo[] array = mapPorts.values().toArray(new PortInfo[0]);
		Arrays.sort(array, new Comparator<PortInfo>() {
			public int compare(PortInfo arg0, PortInfo arg1) {
				return Boolean.compare(arg1.ourBinding, arg0.ourBinding);
			}
		});
		return array;
	}

	private boolean matchesVPNIP(InetAddress address) {
		if (address == null) {
			return false;
		}
		String regex = config.getPluginStringParameter(
				PluginAir.CONFIG_VPN_IP_MATCHING);
		return Pattern.matches(regex, address.getHostAddress());
	}

	private int handleBound(InetAddress bindIP, StringBuilder sReply) {
		int newStatusID = STATUS_ID_OK;

		String s;
		boolean isGoodExistingBind = matchesVPNIP(bindIP);
		if (isGoodExistingBind) {
			String niName = "Unknown Interface";
			try {
				NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
						bindIP);
				niName = networkInterface.getName() + " ("
						+ networkInterface.getDisplayName() + ")";
			} catch (Throwable e) {
			}
			addReply(sReply, CHAR_GOOD, "airvpn.bound.good", new String[] {
				"" + bindIP,
				niName
			});
			vpnIP = bindIP;
		} else {
			addReply(sReply, CHAR_BAD, "airvpn.bound.bad", new String[] {
				"" + bindIP
			});
			newStatusID = STATUS_ID_BAD;
		}

		try {
			// Check if default routing goes through 10.*, by connecting to address
			// via socket.  Address doesn't need to be reachable, just routable.
			// This works on Windows (in some cases), but on Mac returns a wildcard 
			// address
			DatagramSocket socket = new DatagramSocket();
			socket.connect(testSocketAddress, 0);
			InetAddress localAddress = socket.getLocalAddress();
			socket.close();

			if (!localAddress.isAnyLocalAddress()) {
				NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
						localAddress);

				s = texts.getLocalisedMessageText("airvpn.nonvuze.probable.route",
						new String[] {
							"" + localAddress,
							networkInterface.getName() + " ("
									+ networkInterface.getDisplayName() + ")"
				});
				char replyChar = ' ';

				if ((localAddress instanceof Inet4Address)
						&& matchesVPNIP(localAddress)) {

					if (localAddress.equals(bindIP)) {
						replyChar = isGoodExistingBind ? CHAR_GOOD : CHAR_WARN;
						s += " " + texts.getLocalisedMessageText("airvpn.same.as.vuze");
					} else {
						// Vuze is bound, default routing goes somewhere else
						// This is ok, since Vuze will not accept incoming from "somewhere else"
						// We'll warn, but not update the status id

						replyChar = CHAR_WARN;
						s += " " + texts.getLocalisedMessageText("airvpn.not.same");

						if (isGoodExistingBind) {
							s += " " + texts.getLocalisedMessageText(
									"default.routing.not.vpn.network.splitting");
						}
					}

					addLiteralReply(sReply, replyChar + " " + s);

					if (!isGoodExistingBind && rebindNetworkInterface) {
						rebindNetworkInterface(networkInterface, localAddress, sReply);
						// Should we redo test?
					}

				} else {
					// Vuze is bound, default routing goes to somewhere else.
					// Probably network splitting
					replyChar = isGoodExistingBind ? CHAR_WARN : CHAR_BAD;
					if (isGoodExistingBind) {
						s += " " + texts.getLocalisedMessageText(
								"default.routing.not.vpn.network.splitting");
					}
					addLiteralReply(sReply, replyChar + " " + s);
				}
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
			addReply(sReply, CHAR_WARN, "airvpn.vuze.unbound");
		} else {
			addReply(sReply, CHAR_BAD, "airvpn.vuze.loopback");
		}

		try {
			NetworkAdmin networkAdmin = NetworkAdmin.getSingleton();

			// Find a bindable address that starts with 10.
			InetAddress[] bindableAddresses = networkAdmin.getBindableAddresses();

			for (InetAddress bindableAddress : bindableAddresses) {
				if (matchesVPNIP(bindableAddress)) {
					newBindIP = bindableAddress;
					newBindNetworkInterface = NetworkInterface.getByInetAddress(
							newBindIP);

					addReply(sReply, CHAR_GOOD, "airvpn.found.bindable.vpn",
							new String[] {
								"" + newBindIP
					});

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
						if (matchesVPNIP(address)) {
							s = texts.getLocalisedMessageText("airvpn.possible.vpn",
									new String[] {
										"" + address,
										networkAdminInterface.getName() + " ("
												+ networkAdminInterface.getDisplayName() + ")"
							});

							if (newBindIP == null) {
								foundNIF = true;
								newBindIP = address;

								// Either one should work
								//newBindNetworkInterface = NetworkInterface.getByInetAddress(newBindIP);
								newBindNetworkInterface = NetworkInterface.getByName(
										networkAdminInterface.getName());

								s = CHAR_GOOD + " " + s + ". "
										+ texts.getLocalisedMessageText("airvpn.assuming.vpn");
							} else if (address.equals(newBindIP)) {
								s = CHAR_GOOD + " " + s + ". "
										+ texts.getLocalisedMessageText("airvpn.same.address");
								foundNIF = true;
							} else {
								if (newStatusID != STATUS_ID_BAD) {
									newStatusID = STATUS_ID_WARN;
								}
								s = CHAR_WARN + " " + s + ". "
										+ texts.getLocalisedMessageText("airvpn.not.same.address");
							}

							addLiteralReply(sReply, s);

							if (rebindNetworkInterface) {
								// stops message below from being added, we'll rebind later
								foundNIF = true;
							}

						}
					}
				}
			}

			if (!foundNIF) {
				addReply(sReply, CHAR_BAD, "airvpn.interface.not.found");
			}

			// Check if default routing goes through 10.*, by connecting to address
			// via socket.  Address doesn't need to be reachable, just routable.
			// This works on Windows, but on Mac returns a wildcard address
			DatagramSocket socket = new DatagramSocket();
			socket.connect(testSocketAddress, 0);
			InetAddress localAddress = socket.getLocalAddress();
			socket.close();

			if (!localAddress.isAnyLocalAddress()) {
				NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
						localAddress);

				s = texts.getLocalisedMessageText("airvpn.nonvuze.probable.route",
						new String[] {
							"" + localAddress,
							networkInterface == null ? "null" : networkInterface.getName()
									+ " (" + networkInterface.getDisplayName() + ")"
				});

				if ((localAddress instanceof Inet4Address)
						&& matchesVPNIP(localAddress)) {

					if (newBindIP == null) {
						newBindIP = localAddress;
						newBindNetworkInterface = networkInterface;

						s = CHAR_GOOD + " " + s + " "
								+ texts.getLocalisedMessageText("airvpn.assuming.vpn");
					} else if (localAddress.equals(newBindIP)) {
						s = CHAR_GOOD + " " + s + " "
								+ texts.getLocalisedMessageText("airvpn.same.address");
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
						s = CHAR_WARN + " " + s + " "
								+ texts.getLocalisedMessageText(
										"airvpn.not.same.future.address")
								+ " "
								+ texts.getLocalisedMessageText(
										"default.routing.not.vpn.network.splitting")
								+ " " + texts.getLocalisedMessageText(
										"default.routing.not.vpn.network.splitting.unbound");
					}

					addLiteralReply(sReply, s);

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

					addLiteralReply(sReply, s);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			addReply(sReply, CHAR_BAD, "airvpn.nat.error", new String[] {
				e.toString()
			});
		}

		if (newBindIP == null) {
			addReply(sReply, CHAR_BAD, "airvpn.vpn.ip.detect.fail");
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

			addReply(sReply, CHAR_GOOD, "airvpn.already.bound.good", new String[] {
				ifName
			});
		} else {
			String newConfigBindIP = ifName;
			if (bindNetworkInterfaceIndex >= 0) {
				newConfigBindIP += "[" + bindNetworkInterfaceIndex + "]";
			}

			final AESemaphore sem = new AESemaphore("AirVPN BindWait");

			NetworkAdmin.getSingleton().addPropertyChangeListener(
					new NetworkAdminPropertyChangeListener() {
						public void propertyChanged(String property) {
							if (property.equals(NetworkAdmin.PR_DEFAULT_BIND_ADDRESS)) {
								sem.releaseForever();
								NetworkAdmin.getSingleton().removePropertyChangeListener(this);

								addReply(sReply, CHAR_GOOD, "airvpn.bind.complete.triggered");
							}
						}
					});

			// I think setting CORE_PARAM_STRING_LOCAL_BIND_IP is actually synchronous
			// We set up a PropertyChangeListener in case it ever becomes asynchronous
			config.setCoreStringParameter(
					PluginConfig.CORE_PARAM_STRING_LOCAL_BIND_IP, newConfigBindIP);
			config.setUnsafeBooleanParameter("Enforce Bind IP", true);
			config.setUnsafeBooleanParameter("Check Bind IP On Start", true);

			config.setUnsafeBooleanParameter("upnp.enable", false);
			config.setUnsafeBooleanParameter("natpmp.enable", false);

			addReply(sReply, CHAR_GOOD, "airvpn.change.binding", new String[] {
				"" + newConfigBindIP,
				networkInterface.getName() + " (" + networkInterface.getDisplayName()
						+ ")"
			});

			sem.reserve(11000);
			return sem.isReleasedForever();
		}
		return true;
	}

	protected File getVPNConfigPath() {
		PlatformManager platformManager = PlatformManagerFactory.getPlatformManager();

		try {
			File fDocPath = platformManager.getLocation(
					PlatformManager.LOC_DOCUMENTS);
			if (fDocPath != null) {
				File f = new File(fDocPath.getParentFile(),
						Constants.isLinux ? ".airvpn" : "AirVPN");
				if (f.isDirectory()) {
					return f;
				}
			}
		} catch (PlatformManagerException e) {
		}

		String appData;
		String userhome = System.getProperty("user.home");

		if (Constants.isWindows) {
			appData = SystemProperties.getEnvironmentalVariable("LOCALAPPDATA");

			if (appData != null && appData.length() > 0) {
			} else {
				appData = userhome + SystemProperties.SEP + "Application Data";
			}

		} else if (Constants.isOSX) {
			appData = userhome + SystemProperties.SEP + "Library"
					+ SystemProperties.SEP + "Application Support";

		} else {
			// unix type
			appData = userhome;
		}

		File f = new File(appData, Constants.isLinux ? ".airvpn" : "AirVPN");
		if (f.isDirectory()) {
			return f;
		}
		return null;
	}

	private int ourPortInList(PortInfo[] ports) {
		PluginConfig pluginConfig = pi.getPluginconfig();
		int coreTCPPort = pluginConfig.getCoreIntParameter(
				PluginConfig.CORE_PARAM_INT_INCOMING_TCP_PORT);
		int coreUDPPort = pluginConfig.getCoreIntParameter(
				PluginConfig.CORE_PARAM_INT_INCOMING_UDP_PORT);
		if (coreTCPPort != coreUDPPort) {
			return -1;
		}

		boolean skipIfNotOurBinding = ports.length != 20;

		String sPort = Integer.toString(coreUDPPort);
		for (int i = 0; i < ports.length; i++) {
			PortInfo portInfo = ports[i];
			if (!portInfo.ourBinding && skipIfNotOurBinding) {
				return -1;
			}
			if (portInfo.port.equals(sPort)) {
				return i;
			}
		}
		return -1;
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
			addReply(sReply, CHAR_GOOD, "airvpn.changed.port", new String[] {
				"TCP",
				Integer.toString(coreTCPPort),
				Integer.toString(port)
			});
		}
		if (coreUDPPort != port) {
			pluginConfig.setCoreIntParameter(
					PluginConfig.CORE_PARAM_INT_INCOMING_UDP_PORT, port);
			addReply(sReply, CHAR_GOOD, "airvpn.changed.port", new String[] {
				"UDP",
				Integer.toString(coreUDPPort),
				Integer.toString(port)
			});
		}
	}

	public void addListener(CheckerListener l) {
		listeners.add(l);
		try {
			l.portCheckStatusChanged(lastPortCheckStatus);
			l.protocolAddressesStatusChanged(lastProtocolAddresses);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeListener(CheckerListener l) {
		listeners.remove(l);
	}

	public int getCurrentStatusID() {
		return currentStatusID;
	}

	public static void main(String[] args) {
		Checker checker = new Checker();
		File vpnConfigPath = checker.getVPNConfigPath();
		System.out.println(vpnConfigPath);
		String defaultUsername = checker.getDefaultUsername();
		System.out.println("user=" + defaultUsername + "/" + checker.getPassword());

		checker.callRPCforPort(null, new StringBuilder());
	}

	private static class PortInfo
	{
		String port;

		boolean ourBinding;

		public PortInfo(String port, boolean ourBinding) {
			super();
			this.port = port;
			this.ourBinding = ourBinding;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return port + ";" + ourBinding;
		}
	}
}
