/*
 *    This file is part of mlDHT. 
 * 
 *    mlDHT is free software: you can redistribute it and/or modify 
 *    it under the terms of the GNU General Public License as published by 
 *    the Free Software Foundation, either version 2 of the License, or 
 *    (at your option) any later version. 
 * 
 *    mlDHT is distributed in the hope that it will be useful, 
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *    GNU General Public License for more details. 
 * 
 *    You should have received a copy of the GNU General Public License 
 *    along with mlDHT.  If not, see <http://www.gnu.org/licenses/>. 
 */
package lbms.plugins.mldht.azureus;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

import lbms.plugins.mldht.DHTConfiguration;
import lbms.plugins.mldht.kad.DHT;
import lbms.plugins.mldht.kad.DHTConstants;
import lbms.plugins.mldht.kad.DHTLogger;
import lbms.plugins.mldht.kad.DHT.DHTtype;
import lbms.plugins.mldht.kad.RPCServer;
import lbms.plugins.mldht.kad.RPCServerListener;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminPropertyChangeListener;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

/**
 * @author Damokles
 * 
 */
public class MlDHTPlugin implements UnloadablePlugin, PluginListener, NetworkAdminPropertyChangeListener {

	private PluginInterface			pluginInterface;
	private Map<DHTtype, DHT>		dhts;
	private Tracker					tracker;
	private BasicPluginConfigModel	config_model;

	private BasicPluginViewModel	view_model;
	private Logger					logger;
	private LoggerChannel			logChannel;
	private LoggerChannelListener	logListener;
	private UIManagerListener		uiListener;

	private LocaleUtilities			locale_utils;
	
	private AlternativeContactHandler	alt_contact_handler;
	
	//private Display					display;

	private volatile boolean		unloaded;
	
	private UIHelper				uiHelper;

	private Object					mlDHTProvider;

	private static MlDHTPlugin		singleton;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins
	 * .PluginInterface)
	 */
	public void initialize (final PluginInterface pluginInterface)
			throws PluginException {
		if (singleton != null) {
			throw new IllegalStateException("Plugin already initialized");
		}
		singleton = this;
		
		this.pluginInterface = pluginInterface;
		UIManager ui_manager = pluginInterface.getUIManager();
		
		locale_utils = pluginInterface.getUtilities().getLocaleUtilities();
		
		config_model = ui_manager.createBasicPluginConfigModel("plugins",
				"plugin.mldht");

		config_model.addBooleanParameter2("enable", "mldht.enable", true);
		config_model.addIntParameter2("port", "mldht.port", 49001);

		for (DHTtype type : DHTtype.values()) {
			config_model.addBooleanParameter2("autoopen." + type.shortName,
					("mldht.autoopen." + type.shortName).toLowerCase(), false);
		}
		config_model.addBooleanParameter2("backupOnly", "mldht.backupOnly",
				false);
		config_model.addBooleanParameter2("onlyPeerBootstrap",
				"mldht.onlyPeerBootstrap", false);
		config_model.addBooleanParameter2("alwaysRestoreID", "mldht.restoreID",
				true);
		config_model.addBooleanParameter2("showStatusEntry",
				"mldht.showStatusEntry", true);
		config_model.addBooleanParameter2("multihoming", "mldht.multihoming", false);

		view_model = ui_manager.createBasicPluginViewModel("Mainline DHT Log");

		view_model.getActivity().setVisible(false);
		view_model.getProgress().setVisible(false);

		view_model.getStatus().setText("Stopped");

		logger = pluginInterface.getLogger();
		logChannel = logger.getTimeStampedChannel("Mainline DHT");

		logListener = new LoggerChannelListener() {
			public void messageLogged (int type, String content) {
				view_model.getLogArea().appendText(content + "\n");
			}

			public void messageLogged (String str, Throwable error) {
				if (str.length() > 0) {
					view_model.getLogArea().appendText(str + "\n");
				}

				StringWriter sw = new StringWriter();

				PrintWriter pw = new PrintWriter(sw);

				error.printStackTrace(pw);

				pw.flush();

				view_model.getLogArea().appendText(sw.toString() + "\n");
			}
		};

		logChannel.addListener(logListener);

		String version = pluginInterface.getPluginVersion();
		int parsedVersion = -1;
		if (version != null) {
			version = version.replaceAll("[^0-9]", "");
			if (version.length() > 9) {
				version = version.substring(0, 8);
			}
			parsedVersion = Integer.parseInt(version);
		}

		DHTConstants.setVersion(parsedVersion);
		dhts = DHT.createDHTs();

		DHT.setLogger(new DHTLogger() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.mldht.kad.DHTLogger#log(java.lang.String)
			 */
			public void log (String message) {
				logChannel.log(message);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see lbms.plugins.mldht.kad.DHTLogger#log(java.lang.Exception)
			 */
			public void log (Throwable e) {
				logChannel.log(e);
			}
		});

		try {
			mlDHTProvider = new org.gudy.azureus2.plugins.dht.mainline.MainlineDHTProvider() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * org.gudy.azureus2.plugins.dht.mainline.MainlineDHTProvider
				 * #getDHTPort()
				 */
				public int getDHTPort () {
					return pluginInterface.getPluginconfig()
							.getPluginIntParameter("port");
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * org.gudy.azureus2.plugins.dht.mainline.MainlineDHTProvider
				 * #notifyOfIncomingPort(java.lang.String, int)
				 */
				public void notifyOfIncomingPort (String ip_addr, int port) {
					for (DHT dht : dhts.values()) {
						dht.addDHTNode(ip_addr, port);
					}
				}
			};
			pluginInterface
					.getMainlineDHTManager()
					.setProvider(
							(org.gudy.azureus2.plugins.dht.mainline.MainlineDHTProvider) mlDHTProvider);
		} catch (Throwable e) {

			e.printStackTrace();
		}

		tracker = new Tracker(this);

		uiListener = new UIManagerListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.gudy.azureus2.plugins.ui.UIManagerListener#UIAttached(org
			 * .gudy.azureus2.plugins.ui.UIInstance)
			 */
			public void UIAttached (UIInstance instance) {
				if (uiHelper == null) {
					if ( instance.getUIType() == UIInstance.UIT_SWT ){
						try{
							Class cla = getClass().getClassLoader().loadClass( "lbms.plugins.mldht.azureus.gui.SWTHelper");
							
							uiHelper = (UIHelper)cla.getConstructor( MlDHTPlugin.class ).newInstance( MlDHTPlugin.this );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
				if ( uiHelper != null ){
					uiHelper.UIAttached(instance);
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.gudy.azureus2.plugins.ui.UIManagerListener#UIDetached(org
			 * .gudy.azureus2.plugins.ui.UIInstance)
			 */
			public void UIDetached (UIInstance instance) {
				if (uiHelper != null) {
					uiHelper.UIDetached(instance);
				}

			}
		};

		ui_manager.addUIListener(uiListener);

		TableContextMenuItem incompleteMenuItem = ui_manager.getTableManager()
				.addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE,
						"tablemenu.main.item");
		TableContextMenuItem completeMenuItem = ui_manager.getTableManager()
				.addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE,
						"tablemenu.main.item");

		incompleteMenuItem.setStyle(MenuItem.STYLE_MENU);
		completeMenuItem.setStyle(MenuItem.STYLE_MENU);

		TableContextMenuItem incAnnounceItem = ui_manager.getTableManager()
				.addContextMenuItem(incompleteMenuItem,
						"tablemenu.announce.item");

		TableContextMenuItem comAnnounceItem = ui_manager
				.getTableManager()
				.addContextMenuItem(completeMenuItem, "tablemenu.announce.item");

		MenuItemListener announceItemListener = new MenuItemListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.gudy.azureus2.plugins.ui.menus.MenuItemListener#selected(
			 * org.gudy.azureus2.plugins.ui.menus.MenuItem, java.lang.Object)
			 */
			public void selected (MenuItem menu, Object target) {
				TableRow row = (TableRow) target;
				Download dl = (Download) row.getDataSource();
				tracker.announceDownload(dl);
			}
		};

		incAnnounceItem.addListener(announceItemListener);
		comAnnounceItem.addListener(announceItemListener);
		
		NetworkAdmin.getSingleton().addPropertyChangeListener(this);

		//must be at the end because on update you get a synchronous callback
		pluginInterface.addListener(this);
	}

	//-------------------------------------------------------------------

	public static MlDHTPlugin getSingleton () {
		return singleton;
	}

	/**
	 * @return the pluginInterface
	 */
	public PluginInterface getPluginInterface () {
		return pluginInterface;
	}

	/**
	 * @return the dht
	 */
	public DHT getDHT (DHT.DHTtype type) {
		return dhts.get(type);
	}

	public Tracker getTracker () {
		return tracker;
	}

	/**
	 * @return the logger
	 */
	public Logger getLogger () {
		return logger;
	}

	public String
	getMessageText(
		String	key )
	{
		return( locale_utils.getLocalisedMessageText( key ));
	}
	
	public void
	showConfig()
	{
		pluginInterface.getUIManager().showConfigSection( "plugin.mldht" );
	}
	
	/**
	 * Returns the user set status of whether or not the plugin should autoOpen
	 * 
	 * @return boolean autoOpen
	 */
	public boolean isPluginAutoOpen (String dhtType) {
		return pluginInterface.getPluginconfig().getPluginBooleanParameter(
				"autoopen." + dhtType, false);
	}

	private void registerUPnPMapping (int port) {
		try {
			PluginInterface pi_upnp = pluginInterface.getPluginManager()
					.getPluginInterfaceByClass(UPnPPlugin.class);
			if (pi_upnp != null) {
				((UPnPPlugin) pi_upnp.getPlugin()).addMapping(pluginInterface
						.getPluginName(), false, port, true);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	//-------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	 */
	public void unload () throws PluginException {
		
		unloaded = true;
		
		NetworkAdmin.getSingleton().removePropertyChangeListener(this);

		if (uiHelper != null) {
			uiHelper.onPluginUnload();
		}

		stopDHT();

		if ( pluginInterface != null ){
			try {
				pluginInterface.getMainlineDHTManager().setProvider(null);
			} catch (Throwable e) {
			}
	
			pluginInterface.getUIManager().removeUIListener(uiListener);
			
			pluginInterface.removeListener(this);
		}
		
		if ( view_model != null ){
		
			view_model.destroy();
		}
		
		if ( config_model != null ){
		
			config_model.destroy();
		}
		
		if ( logChannel != null ){
			
			logChannel.removeListener(logListener);
		}
			
		DHT.initStatics();	// reset in case plugin class isn't unloaded (happend when bundled)
		
		dhts 		= null;
		
		singleton 	= null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.PluginListener#closedownComplete()
	 */
	public void closedownComplete () {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.PluginListener#closedownInitiated()
	 */
	public void closedownInitiated () {
		stopDHT();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.PluginListener#initializationComplete()
	 */
	public void initializationComplete () {
		registerUPnPMapping(pluginInterface.getPluginconfig()
				.getPluginIntParameter("port"));
		if (pluginInterface.getPluginconfig().getPluginBooleanParameter(
				"enable")) {
			//start in a separate Thread, since it might block
			//when getPublicAddress is called and the version server is offline
			Thread t = new Thread(new Runnable() {
				public void run () {
					try{
						startDHT();
				
					}catch( Throwable e ){
						e.printStackTrace();
					}
				}
			});
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		}

	}

	//-------------------------------------------------------------------

	// this is all a mess - serialise the start/stop operations in an attempt to get on top of it
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( "MLDHT:disp", 2500 );
	
	public void startDHT () {
		
		dispatcher.dispatch(
			new AERunnable() {

				@Override
				public void 
				runSupport() 
				{
					if ( unloaded ){
						return;
					}

					DHTConfiguration config = new DHTConfiguration() {
						public boolean noRouterBootstrap() {
							return pluginInterface.getPluginconfig()
									.getPluginBooleanParameter(
											"onlyPeerBootstrap");
						}

						public boolean isPersistingID() {
							return pluginInterface.getPluginconfig().getPluginBooleanParameter("alwaysRestoreID");
						}

						public File getNodeCachePath() {
							return pluginInterface.getPluginconfig().getPluginUserFile("dht.cache");
						}

						public int getListeningPort() {
							return pluginInterface.getPluginconfig().getPluginIntParameter("port");
						}

						public boolean allowMultiHoming() {
							return pluginInterface.getPluginconfig().getPluginBooleanParameter("multihoming");
						}
					}; 

					try{
						alt_contact_handler = new AlternativeContactHandler();
						
					}catch( Throwable e ){
					}
					
					RPCServerListener serverListener = 
							new RPCServerListener() {
								@Override
								public void 
								replyReceived(
									InetSocketAddress from_node) 
								{
									if ( alt_contact_handler != null ){
									
										try{
											alt_contact_handler.nodeAlive( from_node );
											
										}catch( Throwable e ){
											
											Debug.out( e );
										}
									}
								}
							};
							
					view_model.getStatus().setText("Initializing");
					try {
						for (Map.Entry<DHTtype, DHT> e : dhts.entrySet()) {
							e.getValue().start(config, serverListener);

							e.getValue().bootstrap();
						}

						tracker.start();
						view_model.getStatus().setText("Running");
					} catch (SocketException e) {
						e.printStackTrace();
					}
				}
			});
	}

	public void stopDHT () {
		
		final AESemaphore sem = new AESemaphore( "MLDHT:Stopper" );
				
		dispatcher.dispatch(
			new AERunnable() {
				
				@Override
				public void 
				runSupport() 
				{
					try{
						if ( tracker != null ){
							tracker.stop();
						}
						if ( dhts != null ){
							for (DHT dht : dhts.values()) {
								dht.stop();
							}
						}
						
						if ( alt_contact_handler != null ){
							
							alt_contact_handler.destroy();
						}
						
						if ( view_model != null ){
							view_model.getStatus().setText("Stopped");
						}
					}finally{
						
						sem.release();
					}
				}
			});
		
		if ( !sem.reserve(30*1000)){
			
			Debug.out( "Timeout waiting for DHT to stop" );
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.networkmanager.admin.NetworkAdminPropertyChangeListener#propertyChanged(java.lang.String)
	 */
	@Override
	public void propertyChanged(String property) {
		for (DHT dht : dhts.values()) {
			List<RPCServer> servers = dht.getServers();
			for (RPCServer server: servers) {
				server.closeSocket();
			}
		}
	}
}
