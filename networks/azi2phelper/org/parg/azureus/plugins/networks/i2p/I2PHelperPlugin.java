/*
 * Created on Mar 17, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.parg.azureus.plugins.networks.i2p;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;





import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ServerSocketChannel;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.i2p.I2PAppContext;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketOptions;
import net.i2p.data.Base32;
import net.i2p.data.Base64;
import net.i2p.data.Destination;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.InfoParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;
import org.parg.azureus.plugins.networks.i2p.swt.I2PHelperView;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.proxy.AEProxyAddressMapper;
import com.aelitis.azureus.core.proxy.AEProxyFactory;
import com.aelitis.azureus.core.proxy.AEProxyFactory.PluginProxy;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.plugins.upnp.UPnPMapping;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.net.magneturi.MagnetURIHandler;
import com.aelitis.net.magneturi.MagnetURIHandlerException;
import com.aelitis.net.magneturi.MagnetURIHandlerListener;
import com.aelitis.net.magneturi.MagnetURIHandlerProgressListener;


public class 
I2PHelperPlugin 
	implements UnloadablePlugin, I2PHelperAdapter
{	
	/*
	 *	Router: commented out System.setProperties for timezone, http agent etc in static initialiser
	 *	RoutingKeyGenerator: Fixed up SimpleDateFormat as it assumes GMT (TimeZone default used within SimpleDateFormat)
	 *    	private final static SimpleDateFormat _fmt = new SimpleDateFormat(FORMAT, Locale.UK);
    		static{
    			_fmt.setCalendar( _cal );	 // PARG
    		}
    		
    		
    	NativeBigInteger: Added load attempt from classes's loader (i.e. plugin class loader)
    	    if (resource == null) {
        		// PARG added search via plugin class loader as well
        		resource = NativeBigInteger.class.getClassLoader().getResource(resourceName);
        	}
	*/
	
	private static final String	BOOTSTRAP_SERVER = "http://i2pboot.vuze.com:60000/?getNodes=true";
	
	private PluginInterface			plugin_interface;
	private PluginConfig			plugin_config;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	private LocaleUtilities			loc_utils;

	private IntParameter 			int_port_param;
	private IntParameter 			ext_port_param;
	private IntParameter 			socks_port_param;
	private InfoParameter 			port_info_param;
	
	private int						active_int_port;
	private int						active_ext_port;
	
	private I2PHelperView			ui_view;
	
	private boolean					plugin_enabled;
	
	private I2PHelperRouter			router;
	private I2PHelperTracker		tracker;
	
	private File			lock_file;
	private InputStream		lock_stream;
	
	private I2PHelperSocksProxy					socks_proxy;
	private Map<Proxy,ProxyMapEntry>			proxy_map 				= new IdentityHashMap<Proxy, ProxyMapEntry>();

	private I2PHelperMessageHandler		message_handler;
	
	private MagnetURIHandlerListener	magnet_handler =
		new MagnetURIHandlerListener()
		{
			public byte[]
			badge()
			{
				return( null );
			}
			
			public byte[]
			download(
				MagnetURIHandlerProgressListener	progress,
				byte[]								hash,
				String								args,
				InetSocketAddress[]					sources,
				long								timeout )
			
				throws MagnetURIHandlerException
			{
				if ( args.contains( "maggot_sha1" )){
					
					return( handleMaggotRequest( progress, hash, args, timeout ));
					
				}else{
					
					return( null );
				}
			}
			
			public boolean
			download(
				URL			magnet_url )
			
				throws MagnetURIHandlerException
			{
				return( false );
			}
			
			public boolean
			set(
				String	name,
				Map values )
			{
				return( false );
			}
			
			public int
			get(
				String	name,
				Map 	values )
			{
				return( Integer.MIN_VALUE );
			}
		};
		
	private int DEST_HISTORY_MAX	= 100;
	
	private Map<String,Destination>		dest_map = 
		new LinkedHashMap<String,Destination>(DEST_HISTORY_MAX,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry<String,Destination> eldest) 
			{
				return size() > DEST_HISTORY_MAX;
			}
		};
		
	private static final int EXTERNAL_BOOTSTRAP_PERIOD = 30*60*1000;
		
	private long		last_external_bootstrap;
	private long		last_external_bootstrap_import;
	private List<Map>	last_external_bootstrap_nodes;
		
	private static final int NODES_FROM_PEERS_MAX	= 10;
	private LinkedList<NodeInfo>	bootstrap_nodes_from_peers = new LinkedList<NodeInfo>();
	
		
	private volatile boolean	unloaded;
	
	public void
	initialize(
		PluginInterface		pi )
		
		throws PluginException
	{
		try{
			plugin_interface	= pi;
			
			setUnloadable( true );
			
			final File plugin_dir = pi.getPluginconfig().getPluginUserFile( "tmp.tmp" ).getParentFile();

			lock_file = new File( plugin_dir, ".azlock" );
			
			lock_file.delete();
			
			if ( lock_file.createNewFile()){

				lock_stream = new FileInputStream( lock_file );
				
			}else{
				
				throw( new PluginException( "Another instance of Vuze is running, can't initialize plugin" ));
			}
			
			loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
			
			log	= plugin_interface.getLogger().getTimeStampedChannel( "I2PHelper");
			
			final UIManager	ui_manager = plugin_interface.getUIManager();

			view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azi2phelper.name" ));

			view_model.getActivity().setVisible( false );
			view_model.getProgress().setVisible( false );
			
			log.addListener(
					new LoggerChannelListener()
					{
						public void
						messageLogged(
							int		type,
							String	content )
						{
							view_model.getLogArea().appendText( content + "\n" );
						}
						
						public void
						messageLogged(
							String		str,
							Throwable	error )
						{
							view_model.getLogArea().appendText( str + "\n" );
							view_model.getLogArea().appendText( error.toString() + "\n" );
						}
					});
					
			plugin_config = plugin_interface.getPluginconfig();
						
			config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azi2phelper.name" );

			view_model.setConfigSectionID( "azi2phelper.name" );

			config_model.addLabelParameter2( "azi2phelper.info1" );
			config_model.addLabelParameter2( "azi2phelper.info2" );
			
			config_model.addHyperlinkParameter2( "azi2phelper.i2p.link", loc_utils.getLocalisedMessageText( "azi2phelper.i2p.link.url" ));
			config_model.addHyperlinkParameter2( "azi2phelper.plugin.link", loc_utils.getLocalisedMessageText( "azi2phelper.plugin.link.url" ));

			final BooleanParameter enable_param = config_model.addBooleanParameter2( "enable", "azi2phelper.enable", true );

			final BooleanParameter link_rates_param = config_model.addBooleanParameter2( "azi2phelper.link.rates", "azi2phelper.link.rates", false );
			
			final IntParameter up_limit_param 		= config_model.addIntParameter2( I2PHelperRouter.PARAM_SEND_KBS, I2PHelperRouter.PARAM_SEND_KBS, I2PHelperRouter.PARAM_SEND_KBS_DEFAULT, 0, Integer.MAX_VALUE );
			final IntParameter down_limit_param 	= config_model.addIntParameter2( I2PHelperRouter.PARAM_RECV_KBS, I2PHelperRouter.PARAM_RECV_KBS, I2PHelperRouter.PARAM_RECV_KBS_DEFAULT, 0, Integer.MAX_VALUE );
			final IntParameter share_percent_param 	= config_model.addIntParameter2( I2PHelperRouter.PARAM_SHARE_PERCENT, I2PHelperRouter.PARAM_SHARE_PERCENT, I2PHelperRouter.PARAM_SHARE_PERCENT_DEFAULT, 10, 100 );

			final Map<String,Object>	router_properties = new HashMap<String, Object>();
			
			ParameterListener rate_change_listener = 
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter param) 
					{
						router_properties.put( I2PHelperRouter.PARAM_SEND_KBS, up_limit_param.getValue());
						router_properties.put( I2PHelperRouter.PARAM_RECV_KBS, down_limit_param.getValue());
						router_properties.put( I2PHelperRouter.PARAM_SHARE_PERCENT, share_percent_param.getValue());

						I2PHelperRouter current_router = router;
						
						if ( current_router != null ){
							
							current_router.updateProperties();
						}
					}
				};
			
			rate_change_listener.parameterChanged( null );
			
			up_limit_param.addListener( rate_change_listener );
			down_limit_param.addListener( rate_change_listener );
			share_percent_param.addListener( rate_change_listener );
			
			ParameterListener link_listener = 
				new ParameterListener()
				{
					private TimerEventPeriodic event;
					
					public void 
					parameterChanged(
						Parameter 	param ) 
					{
						boolean	is_linked = link_rates_param.getValue();
						
						if ( is_linked ){
							
							if ( event == null ){
								
								syncRates();
								
								event = SimpleTimer.addPeriodicEvent(
									"I2PRateLinker",
									15*1000,
									new TimerEventPerformer()
									{
										public void 
										perform(
											TimerEvent event) 
										{
											if ( !link_rates_param.getValue()){
												
												return;
											}
											
											syncRates();
										}
									});
							}
						}else{
							
							if ( event != null ){
								
								event.cancel();
								
								event = null;
							}
						}
					}
					
					private void
					syncRates()
					{
						int dl_limit = NetworkManager.getMaxDownloadRateBPS() / 1024;
						
						int ul_limit;
						
						if (NetworkManager.isSeedingOnlyUploadRate()){
							
							ul_limit= NetworkManager.getMaxUploadRateBPSSeedingOnly() / 1024;
							
						}else{
							
							ul_limit = NetworkManager.getMaxUploadRateBPSNormal() / 1024;
						}
						
						if ( up_limit_param.getValue() != ul_limit ){
							
							up_limit_param.setValue( ul_limit );
						}
						
						if ( down_limit_param.getValue() != dl_limit ){
							
							down_limit_param.setValue( dl_limit );
						}
					}
				};
			
			link_listener.parameterChanged( null );
				
			link_rates_param.addListener( link_listener );
			
			int_port_param 		= config_model.addIntParameter2( "azi2phelper.internal.port", "azi2phelper.internal.port", 0 );
			ext_port_param	 	= config_model.addIntParameter2( "azi2phelper.external.port", "azi2phelper.external.port", 0 );
			socks_port_param 	= config_model.addIntParameter2( "azi2phelper.socks.port", "azi2phelper.socks.port", 0 );
			
			int	int_port = int_port_param.getValue();
			
			boolean port_changed = false;
			
			if ( int_port == 0 ){
				
				int_port = plugin_config.getPluginIntParameter( "azi2phelper.internal.port.auto", 0 );
				
				if ( int_port == 0 || !testPort( int_port )){
					
					int_port = allocatePort( 17654, 0 );
					
					plugin_config.setPluginParameter( "azi2phelper.internal.port.auto", int_port );
					
					port_changed = true;
				}
			}else{
				if  ( !testPort( int_port )){
					
					log( "Testing of explicitly configured internal port " + int_port + " failed - this isn't good" );
				}
			}
			
			active_int_port	= int_port;

			
			int	ext_port = ext_port_param.getValue();
			
			if ( ext_port == 0 ){
				
				ext_port = plugin_config.getPluginIntParameter( "azi2phelper.external.port.auto", 0 );
				
				if ( ext_port == 0 || !testPort( ext_port )){
					
					ext_port = allocatePort( 23154, int_port );
					
					plugin_config.setPluginParameter( "azi2phelper.external.port.auto", ext_port );
					
					port_changed = true;
				}
			}else{
				
				if  ( !testPort( ext_port )){
					
					log( "Testing of explicitly configured external port " + ext_port + " failed - this isn't good" );
				}
			}

			active_ext_port	= ext_port;
			
			int	sock_port = socks_port_param.getValue();
			
			if ( sock_port != 0 ){
				
				if  ( !testPort( sock_port )){
					
					log( "Testing of explicitly configured SOCKS port " + sock_port + " failed - this isn't good" );
				}
			}
			
			port_info_param = config_model.addInfoParameter2( "azi2phelper.port.info", "" );

			updatePortInfo();
			
			final BooleanParameter use_upnp = config_model.addBooleanParameter2( "azi2phelper.upnp.enable", "azi2phelper.upnp.enable", true );
			
			final BooleanParameter always_socks = config_model.addBooleanParameter2( "azi2phelper.socks.always", "azi2phelper.socks.always", false );
		
			final BooleanParameter ext_i2p_param 		= config_model.addBooleanParameter2( "azi2phelper.use.ext", "azi2phelper.use.ext", false );
			
			final IntParameter ext_i2p_port_param 		= config_model.addIntParameter2( "azi2phelper.use.ext.port", "azi2phelper.use.ext.port", 7654 ); 
			
			config_model.createGroup( 
				"azi2phelper.internals.group",
				new Parameter[]{ 
						int_port_param, ext_port_param, socks_port_param,
						port_info_param, use_upnp, always_socks, ext_i2p_param,ext_i2p_port_param });
			
			
			final StringParameter 	command_text_param = config_model.addStringParameter2( "azi2phelper.cmd.text", "azi2phelper.cmd.text", "" );
			final ActionParameter	command_exec_param = config_model.addActionParameter2( "azi2phelper.cmd.act1", "azi2phelper.cmd.act2" );
			
			command_exec_param.addListener(
				new ParameterListener() 
				{
					public void 
					parameterChanged(
						Parameter param ) 
					{
						new AEThread2( "cmdrunner" )
						{
							public void
							run()
							{
								try{
									command_exec_param.setEnabled( false );

									executeCommand( 
										command_text_param.getValue(),
										router,
										tracker,
										I2PHelperPlugin.this );
									
								}catch( Throwable e){
									
									log( "Command failed: " + Debug.getNestedExceptionMessage( e ));
									
								}finally{
									
									command_exec_param.setEnabled( true );
								}
							}
						}.start();
					}
				});
			
			if ( port_changed ){
				
				plugin_config.save();
			}
			
			final int f_int_port = int_port;
			final int f_ext_port = ext_port;
			
			if ( plugin_enabled ){
			
				log( "Internal port=" + int_port +", external=" + ext_port + ", socks=" + sock_port );
				
			}else{
				
				log( "Plugin is disabled" );
			}
			ParameterListener enabler_listener =
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param )
						{
							plugin_enabled 			= enable_param.getValue();

							boolean use_ext_i2p  	= ext_i2p_param.getValue();
							
							boolean	enabled_not_ext = plugin_enabled && !use_ext_i2p;
							
							boolean	is_linked = link_rates_param.getValue();

							link_rates_param.setEnabled( enabled_not_ext );
							up_limit_param.setEnabled( enabled_not_ext && !is_linked );
							down_limit_param.setEnabled( enabled_not_ext  && !is_linked );
							share_percent_param.setEnabled( enabled_not_ext );
							
							int_port_param.setEnabled( enabled_not_ext );
							ext_port_param.setEnabled( enabled_not_ext);
							socks_port_param.setEnabled( plugin_enabled );
							port_info_param.setEnabled( plugin_enabled );
							use_upnp.setEnabled( enabled_not_ext );
							always_socks.setEnabled( plugin_enabled);
							ext_i2p_param.setEnabled( plugin_enabled );
							ext_i2p_port_param.setEnabled( enabled_not_ext );
							
							command_text_param.setEnabled( plugin_enabled );
							command_exec_param.setEnabled( plugin_enabled );
						}
					};
			
			enable_param.addListener( enabler_listener );
			ext_i2p_param.addListener( enabler_listener );
			link_rates_param.addListener( enabler_listener );
			
			enabler_listener.parameterChanged( null );
					
			if ( plugin_enabled ){
				
				message_handler = new I2PHelperMessageHandler( I2PHelperPlugin.this );
				
				plugin_interface.addListener(
					new PluginAdapter()
					{
						public void
						initializationComplete()
						{
							if ( use_upnp.getValue()){
								
								PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
								
								if ( pi_upnp == null ){
									
									log( "No UPnP plugin available, not attempting port mapping");
									
								}else{
									
									for ( int i=0;i<2;i++){
										UPnPMapping	mapping = 
											((UPnPPlugin)pi_upnp.getPlugin()).addMapping( 
												plugin_interface.getPluginName(), 
												i==0, 
												f_ext_port, 
												true );
									}
								}
							}else{
									
								log( "UPnP disabled for the plugin, not attempting port mapping");
							}
						}
						
						public void
						closedownInitiated()
						{
							unload( true );
						}
					});
					
				new AEThread2("I2P: RouterInit")
				{
					public void
					run()
					{
						final boolean	is_bootstrap_node	= false;
						final boolean	is_vuze_dht			= true;
										
						while( !unloaded ){
														
							try{
								I2PHelperRouter my_router	= null;

								try{																
									my_router = router = 
											new I2PHelperRouter( router_properties, is_bootstrap_node, is_vuze_dht, I2PHelperPlugin.this );
									
									if ( ext_i2p_param.getValue()){
											
										my_router.initialise( plugin_dir, ext_i2p_port_param.getValue());
													
									}else{
										
										my_router.initialise( plugin_dir, f_int_port, f_ext_port );
									}
										
									if ( !unloaded ){
										
										boolean	first_run = true;

										tracker = new I2PHelperTracker( I2PHelperPlugin.this, my_router.getDHT());
										
										while( !unloaded ){
											
											if ( first_run ){
												
												if ( always_socks.getValue()){
													
													try{
														getSocksProxy();
														
													}catch( Throwable e ){
														
													}
												}
											}
											
											try{
												my_router.logInfo();
											
											}catch( Throwable e ){
												
											}
											
											Thread.sleep(60*1000);
											
											first_run = false;
										}
									}
									
								}finally{
									
									if ( my_router != null ){
										
										my_router.destroy();
			
										if ( router == my_router ){
																		
											router = null;
										}
									}
								}
							}catch( Throwable e ){
								
								log( "Router initialisation fail: " + Debug.getNestedExceptionMessage( e ));
								
								if ( !unloaded ){
										
									try{
										Thread.sleep(15*1000);
										
									}catch( Throwable f ){
									}
								}
							}
						}
					}
				}.start();
				
				plugin_interface.getUIManager().addUIListener(
						new UIManagerListener()
						{
							public void
							UIAttached(
								UIInstance		instance )
							{
								if ( instance.getUIType() == UIInstance.UIT_SWT ){
									
									ui_view = new I2PHelperView( I2PHelperPlugin.this, instance, "azi2phelper.name" );
								}
							}

							public void
							UIDetached(
								UIInstance		instance )
							{
							}
						});
				
				MagnetURIHandler uri_handler = MagnetURIHandler.getSingleton();

				uri_handler.addListener( magnet_handler );
			}
		}catch( Throwable e ){
			
			synchronized( this ){
			
				unloaded = true;
			}
			
			Debug.out( e );
			
			if ( e instanceof PluginException ){
				
				throw((PluginException)e);
			}
		}
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	public boolean
	isEnabled()
	{
		return( plugin_enabled );
	}
	
	public String
	getStatusText()
	{
		if ( plugin_enabled ){
			
			I2PHelperRouter	r = router;
			
			if ( r == null ){
				
				return(  getMessageText( "azi2phelper.status.initialising" ));
				
			}else{
				
				return( r.getStatusText());
			}
			
		}else{
			
			return( getMessageText( "azi2phelper.status.disabled" ));
		}
	}
	
	public String
	getMessageText(
		String		key )
	{
		return( loc_utils.getLocalisedMessageText(key));
	}
	
	public String
	getMessageText(
		String		key,
		String...	args )
	{
		return( loc_utils.getLocalisedMessageText(key, args));
	}
	
	private void
	updatePortInfo()
	{
		String	socks_port;
		
		synchronized( this ){
			
			if ( socks_proxy != null ){
				
				socks_port = "" + socks_proxy.getPort();
				
			}else{
				
				socks_port = "inactive";
			}
		}
		
		port_info_param.setValue( active_int_port + "/" + active_ext_port + "/" + socks_port );
	}
	
	public I2PHelperRouter
	getRouter()
	{
		return( router );
	}
	
	private I2PHelperSocksProxy
	getSocksProxy()
	
		throws IPCException
	{
		if ( !plugin_enabled ){
			
			throw( new IPCException( "Plugin disabled" ));
		}
		
		synchronized( I2PHelperPlugin.this ){
			
			if ( socks_proxy == null ){
			
				if ( router == null ){
					
					throw( new IPCException( "Router unavailable" ));
				}
				
				try{
					socks_proxy = new I2PHelperSocksProxy( router, socks_port_param.getValue(), I2PHelperPlugin.this );
				
					updatePortInfo();
					
				}catch( Throwable e ){
				
					throw( new IPCException( e ));
				}
			}
			
			return( socks_proxy );
		}
	}
	
	private static void
	executeCommand(
		String				cmd_str,
		I2PHelperRouter		router,
		I2PHelperTracker	tracker,
		I2PHelperAdapter	log )
		
		throws Exception
	{
		cmd_str = cmd_str.trim();
		
		String[] bits = cmd_str.split( " " );

		if ( bits.length == 0 ){
			
			log.log( "No command" );
			
		}else if ( router == null ){
			
			log.log( "Router is not initialised" );
			
		}else{
		
			I2PHelperDHT dht = router.getDHT();
			
			if ( dht == null ){
				
				log.log( "DHT is not initialised" );
				
			}else{
				
				String cmd = bits[0].toLowerCase();
				
				if ( cmd.equals( "print" )){
				
					dht.print();
				
				}else if ( cmd.equals( "info" )){
						
					router.logInfo();
					
				}else if ( cmd.equals( "lookup" )){
					
					if ( bits.length != 2 ){
						
						throw( new Exception( "usage: lookup <hash>"));
					}
					
					byte[] hash = decodeHash( bits[1] );

					Destination dest = router.lookupDestination( hash );
					
					if ( dest == null ){
						
						log.log( "lookup failed" );
						
					}else{
					
						log.log( "lookup -> " + dest.toBase64());
					}		
				}else if ( cmd.equals( "get" )){
					
					if ( bits.length != 2 ){
						
						throw( new Exception( "usage: get <base16_infohash>"));
					}
					
					byte[] hash = decodeHash( bits[1] );
				
					tracker.get( hash );
				
				}else if ( cmd.equals( "put" )){
					
					if ( bits.length != 2 ){
						
						throw( new Exception( "usage: put <base16_infohash>"));
					}
					
					byte[] hash = decodeHash( bits[1] );
				
					tracker.put( hash );

				}else if ( cmd.equals( "ping_dest" )){
					
					if ( bits.length != 3 ){
					
						throw( new Exception( "usage: ping_dest <base64_dest> <dht_port>"));
					}
					
					String dest_64 = bits[1];
					
					int		port 	= Integer.parseInt( bits[2] );
					
					Destination dest = new Destination();
					
					dest.fromBase64( dest_64 );
					
					dht.ping( dest, port );
					
				}else if ( cmd.equals( "ping_node" )){

					if ( bits.length != 2 ){
						
						throw( new Exception( "usage: ping_node <nid_hash>"));
					}
					
					byte[] hash = decodeHash( bits[1] );
					
					NodeInfo ni = dht.getNodeInfo( hash );
					
					if ( ni == null ){
						
						log.log( "Node not found in routing table" );
						
					}else{
						
						dht.ping( ni );
					}
					
				}else if ( cmd.equals( "bootstrap" )){
					
					dht.requestBootstrap();
					
				}else if ( cmd.equals( "boottest" )){
					
					List<NodeInfo> nodes = dht.getNodesForBootstrap(8);
					
					for ( NodeInfo node: nodes ){
						
						log.log( "    " + node.toString());
					}
				}else{
			
					log.log( "Usage: print|info..." );
				}
			}
		}	
	}
		
	public void
	tryExternalBootstrap(
		boolean		force )
	{
		long	now = SystemTime.getMonotonousTime();
		
		if ( 	force ||
				last_external_bootstrap == 0 || 
				now - last_external_bootstrap >= EXTERNAL_BOOTSTRAP_PERIOD ){
			
			last_external_bootstrap = now;
		
			log( "External bootstrap requested" );
			
			try{
				PluginProxy proxy = AEProxyFactory.getPluginProxy( "I2P bootstrap", new URL( BOOTSTRAP_SERVER ));
			
				if ( proxy != null ){
					
					boolean	worked = false;
					
					try{
						HttpURLConnection url_connection = (HttpURLConnection)proxy.getURL().openConnection( proxy.getProxy());
						
						url_connection.setConnectTimeout( 3*60*1000 );
						url_connection.setReadTimeout( 30*1000 );
				
						url_connection.connect();
				
						try{
							InputStream	is = url_connection.getInputStream();
				
							Map map = BDecoder.decode( new BufferedInputStream( is ));
							
							List<Map> nodes = (List<Map>)map.get( "nodes" );
							
							log( "I2P Bootstrap server returned " + nodes.size() + " nodes" );
							
							last_external_bootstrap_nodes 	= nodes;
							last_external_bootstrap_import	= now;
							
							for ( Map m: nodes ){
								
								NodeInfo ni = router.getDHT().heardAbout( m );
								
								if ( ni != null ){
									
									log( "    imported " + ni );
								}
							}
				
						}finally{
				
							url_connection.disconnect();
						}
					}finally{
						
						proxy.setOK( worked );
					}	
				}else{
					
					throw( new Exception( "No plugin proxy available" ));
				}
			}catch( Throwable e ){
			
				log( "External bootstrap failed: " + Debug.getNestedExceptionMessage(e));
				
					// retry if we got a timeout or malformed socks error reply
				
				String msg = Debug.getNestedExceptionMessage(e).toLowerCase();
				
				if ( msg.contains( "timeout" ) || msg.contains( "malformed" )){
					
					// reschedule with a 2 min delay
					
					last_external_bootstrap = now - (EXTERNAL_BOOTSTRAP_PERIOD - 2*60*1000 );
				}
			}
		}else{
			List<NodeInfo>	temp = null;
			
			synchronized( bootstrap_nodes_from_peers ){
				
				if ( bootstrap_nodes_from_peers.size() > 0 ){
					
					temp = new ArrayList<NodeInfo>( bootstrap_nodes_from_peers );
					
					bootstrap_nodes_from_peers.clear();
				}
			}
			
			if ( temp != null ){
			
				log( "Injecting bootstrap nodes from peers" );
				
				for ( NodeInfo ni: temp ){
					
						// these have no NID yet
					
					router.getDHT().ping( ni.getDestination(), ni.getPort());
				}
				
				if ( temp.size() > 5 ){
					
					return;
				}
			}
			
			if ( 	last_external_bootstrap_nodes != null &&
					now - last_external_bootstrap_import >= 3*60*1000 ){
				
				log( "Injecting cached bootstrap nodes" );
				
				last_external_bootstrap_import = now;
				
				for ( Map m: last_external_bootstrap_nodes ){
					
					router.getDHT().heardAbout( m );
				}
			}
		}
	}
	
	public void
	handleDHTPort(
		String		host,
		int			port )
	{
		Destination dest;
		
		synchronized( dest_map ){
			
			dest = dest_map.get( host );
		}
		
		if ( dest != null ){
			
			NodeInfo ni = new NodeInfo( dest, port );
			
			synchronized( bootstrap_nodes_from_peers ){
				
				if ( !bootstrap_nodes_from_peers.contains( ni )){
					
					bootstrap_nodes_from_peers.addFirst( ni );
					
					if ( bootstrap_nodes_from_peers.size() > NODES_FROM_PEERS_MAX ){
						
						bootstrap_nodes_from_peers.removeLast();
					}
				}
			}
		}
	}
	
	public void 
	outgoingConnection(
		I2PSocket i2p_socket )
		
		throws Exception
	{
		Destination dest = i2p_socket.getPeerDestination();
		
		if ( dest != null ){
			
			byte[]	peer_hash = dest.calculateHash().getData();
			
			String peer_ip = Base32.encode( peer_hash ) + ".b32.i2p";

			synchronized( dest_map ){
				
				dest_map.put( peer_ip, dest );
			}
		}
	}
	
	public void 
	incomingConnection(
		I2PSocket i2p_socket )
		
		throws Exception 
	{
		Socket vuze_socket = new Socket( Proxy.NO_PROXY );
		
		try{
			Destination dest = i2p_socket.getPeerDestination();
			
			if ( dest == null ){
				
				i2p_socket.close();
				
				return;
			}
			
			byte[]	peer_hash = dest.calculateHash().getData();
			
			String peer_ip = Base32.encode( peer_hash ) + ".b32.i2p";
			
			// System.out.println( "Incoming from " + peer_ip + ", port=" + i2p_socket.getLocalPort());
			
			if ( i2p_socket.getLocalPort() == 80 ){
				
				handleMaggotRequest( i2p_socket, peer_hash );
				
			}else{
				
				synchronized( dest_map ){
					
					dest_map.put( peer_ip, dest );
				}
				
				vuze_socket.bind( null );
				
				int local_port = vuze_socket.getLocalPort();
				
					// we need to pass the peer_ip to the core so that it doesn't just see '127.0.0.1'
				
				final AEProxyAddressMapper.PortMapping mapping = AEProxyFactory.getAddressMapper().registerPortMapping( local_port, peer_ip );
				
				// System.out.println( "local port=" + local_port );
				
				boolean	ok = false;
				
				try{
					vuze_socket.connect( new InetSocketAddress( "127.0.0.1", COConfigurationManager.getIntParameter( "TCP.Listen.Port" )));
				
					Runnable	on_complete = 
						new Runnable()
						{
							private int done_count;
							
							public void
							run()
							{
								synchronized( this ){
									
									done_count++;
									
									if ( done_count < 2 ){
										
										return;
									}
								}
								
								mapping.unregister();
							}
						};
						
					runPipe( i2p_socket.getInputStream(), vuze_socket.getOutputStream(), on_complete );
				
					runPipe( vuze_socket.getInputStream(), i2p_socket.getOutputStream(), on_complete );
				
					ok = true;
					
				}finally{
					
					if ( !ok ){
						
						mapping.unregister();
					}
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			try{
				i2p_socket.close();
				
			}catch( Throwable f ){		
			}
			
			if ( vuze_socket != null ){
				
				try{
					vuze_socket.close();
					
				}catch( Throwable f ){
				}
			}
		}
	}
	
	private byte[] 
	handleMaggotRequest(
		final MagnetURIHandlerProgressListener	progress,
		final byte[]							hash,
		final String							args,
		final long								timeout )
		
		throws MagnetURIHandlerException
	{
		String[]	bits = args.split( "&" );
		
		String sha1 = null;
		
		for ( String bit: bits ){
			
			String[] temp = bit.trim().split( "=" );
			
			if ( temp[0].equals( "maggot_sha1" )){
				
				sha1 = temp[1];
				
				break;
			}
		}
		
		final String	sha1_hash 	= sha1;
		final String	info_hash	= ByteFormatter.encodeString( hash );
		
		final byte[][]	result = { null };
		
		final AESemaphore	wait_sem = new AESemaphore( "maggot:wait" );
		
		new AEThread2( "maggotLookup" )
		{
			private int		active_gets;
			private boolean	complete;
			
			private List<String>	pending_gets = new ArrayList<String>();
			
			public void
			run()
			{
				try{
					tracker.get( 
						hash,
						"Maggot lookup",
						16,
						timeout,
						new I2PHelperDHTListener()
						{
							public void 
							searching(
								String host ) 
							{
								progress.reportActivity( "I2P: Searching " + host );
							}
							
							public void 
							valueRead(
								String 	host ) 
							{
								if ( progress.cancelled()){
									
									return;
								}
								
								progress.reportActivity( "I2P: Found " + host );
								
								synchronized( result ){
									
									if ( result[0] != null ){
										
										return;
									}
									
									pending_gets.add( host );

									if ( active_gets > 5 ){										
										
										return;
										
									}else{
										
										new AEThread2( "maggotLookup:get" )
										{
											public void
											run()
											{
												while( !progress.cancelled()){
													
													String host;
													
													synchronized( result ){
														
														if ( result[0] != null || pending_gets.isEmpty()){
															
															active_gets--;
															
															if ( active_gets == 0 && complete ){
																
																wait_sem.release();
															}
															
															break;
														}
														
														host = pending_gets.remove(0);
													}
													
													try{
								        				Destination dest = I2PAppContext.getGlobalContext().namingService().lookup( host );

								        				I2PSocketManager socket_manager = router.getSocketManager();
								        						
								        				I2PSocketOptions opts = socket_manager.buildOptions();
								        				
								        				opts.setPort( 80 );
								        				
								        				opts.setConnectTimeout( 30*1000 );
								        				
								        				opts.setReadTimeout( 30*1000 );
								        				
								        				I2PSocket socket = socket_manager.connect( dest, opts );
								        				
								        				try{
									        				OutputStream os = socket.getOutputStream();
									        											
									        				os.write(( "GET /" + info_hash + ":" + sha1_hash + " HTTP/1.1\r\n\r\n" ).getBytes( "ISO8859-1" ));
									        				
									        				os.flush();
									        				
									        				InputStream is = socket.getInputStream();
									        				
									        				ByteArrayOutputStream baos = new ByteArrayOutputStream( 64*1024 );
									        				
									        				while( true ){
									        					
									        					if ( progress.cancelled()){
									        						
									        						throw( new Exception( "Cancelled" ));
									        					}
									        					
									        					byte[]	buffer = new byte[65536];
									        					
									        					int	len = is.read( buffer );
									        					
									        					if ( len <= 0 ){
									        						
									        						break;
									        					}
									        					
									        					if ( baos.size() > 5*1024*1024 ){
									        						
									        						throw( new Exception( "Data too large" ));
									        					}
									        					
									        					baos.write( buffer, 0, len );
									        				}
									        				
									        				byte[] response_bytes = baos.toByteArray();
									        													        					
									        				for ( int i=4;i<response_bytes.length;i++){
									        					
									        					if ( 	response_bytes[i-4] == '\r' && 
									        							response_bytes[i-3] == '\n' &&
									       								response_bytes[i-2] == '\r' &&
									       								response_bytes[i-1] == '\n' ){
									        							
									        						byte[] torrent_bytes = new byte[response_bytes.length-i];
									        						
									        						System.arraycopy( response_bytes, i, torrent_bytes, 0, torrent_bytes.length );
									        						
											        				TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray( torrent_bytes );
											        				
											        				if ( torrent != null && Arrays.equals( torrent.getHash(), hash )){
											        					
											        					synchronized( result ){
											        					
											        						if ( result[0] == null ){
											        							
											        							result[0] = torrent_bytes;
											        							
											        							wait_sem.release();
											        						}
											        					}
											        				}
											        				
											        				break;
									        					}
									        				}
								        				}finally{
								        				
								        					try{
								        						socket.getOutputStream().close();
								        						
								        					}catch( Throwable e ){
								        					}
								        					
								        					try{
								        						socket.getInputStream().close();
								        						
								        					}catch( Throwable e ){
								        					}
								        					
								        					socket.close();
								        				}
													}catch( Throwable e ){
														
													}
												}
											}
										}.start();
										
										active_gets++;
									}
								}
							}
							
							public void 
							complete(
								boolean timeout ) 
							{
								synchronized( result ){
									
									complete = true;
									
									if ( active_gets == 0 ){
									
										wait_sem.release();
									}
								}
							}
						});
					
				}catch( Throwable e ){
					
				}
			}
		}.start();
		
		while( true ){
			
			if ( wait_sem.reserve(1000)){
			
				break;
				
			}else{
				
				if ( progress.cancelled()){
					
					break;
				}
			}
		}
		
		boolean	worked = false;
		
		try{
			synchronized( result ){
			
				worked = result[0] != null;
				
				return( result[0] );
			}
		}finally{
			
			progress.reportActivity( "Maggot lookup " + (worked?"succeeded":"failed" ));
		}
	}
	
	private AtomicInteger	active_maggot_requests 	= new AtomicInteger();
	private BloomFilter		maggot_bloom			= null;
	private long			maggot_bloom_create_time;
	
	private void 
	handleMaggotRequest(
		final I2PSocket 	i2p_socket,
		byte[]				peer_hash )
		
		throws Exception 
	{
		BloomFilter	bloom = maggot_bloom;
		
		if ( bloom == null ){
			
			maggot_bloom = bloom = BloomFilterFactory.createAddRemove4Bit( 128 );
			
			maggot_bloom_create_time = SystemTime.getMonotonousTime();
			
		}else{
			
			if ( SystemTime.getMonotonousTime() - maggot_bloom_create_time > 3*60*1000 ){
				
				maggot_bloom = null;
			}
		}
		
		if ( bloom.add( peer_hash ) > 3 ){
			
			throw( new Exception( "Too many maggot requests from " + ByteFormatter.encodeString( peer_hash )));
		}
		
		if ( active_maggot_requests.incrementAndGet() > 3 ){
			
			active_maggot_requests.decrementAndGet();
			
			throw( new Exception( "Too many active maggot requests" ));
		}
		
		new AEThread2( "I2P.maggot" )
		{
			public void
			run()
			{
				try{
					InputStream is = i2p_socket.getInputStream();
					
					byte[]	buffer = new byte[1024];
					
					String	header = "";
					
					while( true ){
						
						int len = is.read( buffer );
						
						if ( len <= 0 ){
							
							break;
						}
						
						header += new String( buffer, 0, len, "ISO8859-1" );
						
						if ( header.contains( "\r\n\r\n" )){
							
							String[]	lines = header.split( "\r\n" );
							
							String 	line = lines[0].trim();
							
							if ( line.startsWith( "GET " )){
								
								line = line.substring( 4 ).trim();
								
								int pos = line.lastIndexOf( ' ' );
								
								String url = line.substring( 0, pos ).trim();
								
								pos = url.lastIndexOf( '/' );
									
								if ( pos >= 0 ){
									
									url = url.substring(pos+1);
								}
								
								String[] bits = url.split( ":" );
								
								byte[]	info_hash 		= ByteFormatter.decodeString( bits[0].trim());
								byte[]	torrent_sha1  	= ByteFormatter.decodeString( bits[1].trim());
								
								System.out.println( "Maggot request: " + url );
								
								Download download = plugin_interface.getDownloadManager().getDownload( info_hash );
								
								if ( download != null ){
									
									Torrent torrent = download.getTorrent();
									
									if ( torrent != null && !torrent.isPrivate()){
										
										String torrent_name = torrent.getName() + ".torrent";
										
										byte[] torrent_data = torrent.writeToBEncodedData();
										
											// could check the sha1 sometime...
										
										OutputStream os = i2p_socket.getOutputStream();
										
										os.write(( 
											"HTTP/1.0 200 OK\r\n" + 
											"Content-Length: " + torrent_data.length + "\r\n" +
											"Content-Disposition: attachment; filename=\"" + torrent_name + "\"\r\n" +
											"Content-Description: " + torrent_name + "\r\n" +
											"Content-Type: application/x-bittorrent\r\n" +
											"\r\n").getBytes( "UTF-8" ));
										
										os.write( torrent_data );
										
										os.flush();
									}
								}
							}
							
							break;
						}
						
						if ( header.length() > 8192 ){
							
							throw( new Exception( "Error reading header" ));
						}
					}
					
				}catch( Throwable e ){
					
				}finally{
					
					try{
						try{
							i2p_socket.getOutputStream().close();
							
						}catch( Throwable e ){
						}
						
						i2p_socket.close();
						
					}catch( Throwable e ){
						
					}
					
					active_maggot_requests.decrementAndGet();
				}
			}
		}.start();
	}
	
	private void
	runPipe(
		final InputStream		from,
		final OutputStream		to,
		final Runnable			on_complete )
	{
		new AEThread2( "I2P.in.pipe" )
		{
			public void
			run()
			{
				try{
					byte[]	buffer = new byte[16*1024];
					
					while( !unloaded ){
					
						int	len = from.read( buffer );
						
						if ( len <= 0 ){
							
							break;
						}
						
						to.write( buffer, 0, len );
					}
				}catch( Throwable e ){
					
				}finally{
					
					try{
						from.close();
						
					}catch( Throwable e ){
					}
					
					try{
						to.close();
						
					}catch( Throwable e ){
					}
					
					on_complete.run();
				}
			}
		}.start();
	}
	
	public void
	log(
		String	str )
	{
		if ( log != null ){
			
			log.log( str );
			
		}else{
			
			System.out.println( str );
		}
	}
	
	private static byte[]
	decodeHash(
		String	hash_str )
	{		
		byte[] hash;
		
		int	pos = hash_str.indexOf( ".b32" );
		
		if ( pos != -1 ){
			
			hash = Base32.decode( hash_str.substring(0,pos));
			
		}else{
			
			if ( hash_str.length() == 40 ){
				
				hash = ByteFormatter.decodeString( hash_str );
				
			}else if ( hash_str.length() == 32 ){
				
				hash = Base32.decode( hash_str );
			}else{
				
				hash =  Base64.decode( hash_str );
			}
		}
		
		return( hash );
	}
	
	private void
	setUnloadable(
		boolean	b )
	{
		PluginInterface pi = plugin_interface;
		
		if ( pi != null ){
			
			pi.getPluginProperties().put( "plugin.unload.disabled", String.valueOf( !b ));
		}
	}

	public Object[]
	getProxy(
		String		reason,
		String		host,
		int			port )
	
		throws IPCException
	{
		if ( !plugin_enabled ){
			
			return( null );
		}
		
		if ( !host.toLowerCase().endsWith( ".i2p" )){
			
			return( null );
		}
		
		synchronized( this ){
			
			if ( unloaded ){
				
				return( null );
			}

			I2PHelperSocksProxy socks_proxy = getSocksProxy();
				
			try{
				int intermediate_port = socks_proxy.getPort();
				
				String intermediate_host = socks_proxy.getIntermediateHost( host );
		
				Proxy proxy = new Proxy( Proxy.Type.SOCKS, new InetSocketAddress( "127.0.0.1", intermediate_port ));	
								
				proxy_map.put( proxy, new ProxyMapEntry( host, intermediate_host ));
		
				//System.out.println( "proxy_map=" + proxy_map.size());
				
				//last_use_time	= SystemTime.getMonotonousTime();
	
				//proxy_request_count.incrementAndGet();
					
				return( new Object[]{ proxy, intermediate_host, port });
				
			}catch( Throwable e ){
				
				throw( new IPCException( e ));
			}
		}
	}
	
	public Object[]
	getProxy(
		String		reason,
		URL			url )
		
		throws IPCException
	{
		if ( !plugin_enabled ){
			
			return( null );
		}
		
		String 	host = url.getHost();
				
		if ( !host.toLowerCase().endsWith( ".i2p" )){
			
			return( null );
		}
		
		synchronized( this ){
			
			if ( unloaded ){
				
				return( null );
			}

			I2PHelperSocksProxy socks_proxy = getSocksProxy();
			
			try{
				int intermediate_port = socks_proxy.getPort();
				
				String intermediate_host = socks_proxy.getIntermediateHost( host );
		
				Proxy proxy = new Proxy( Proxy.Type.SOCKS, new InetSocketAddress( "127.0.0.1", intermediate_port ));	
								
				proxy_map.put( proxy, new ProxyMapEntry( host, intermediate_host ));
		
				//System.out.println( "proxy_map=" + proxy_map.size());
	
				//last_use_time	= SystemTime.getMonotonousTime();
	
				//proxy_request_count.incrementAndGet();
					
				url = UrlUtils.setHost( url, intermediate_host );
	
				return( new Object[]{ proxy, url, host });
				
			}catch( Throwable e ){
				
				throw( new IPCException( e ));
			}
		}
	}
	
	public void
	setProxyStatus(
		Proxy		proxy,
		boolean		good )
	{
		ProxyMapEntry	entry;
		
		synchronized( this ){
			
			entry = proxy_map.remove( proxy );
		
			if ( entry != null ){
					
				// System.out.println( "proxy_map=" + proxy_map.size());

				String 	host 				= entry.getHost();
				
				/*
				if ( good ){
					
					proxy_request_ok.incrementAndGet();
					
				}else{
					
					proxy_request_failed.incrementAndGet();
				}
				
				updateProxyHistory( host, good );
				*/
				
				String	intermediate	= entry.getIntermediateHost();
	
				if ( intermediate != null ){
					
					if ( socks_proxy != null ){
						
						socks_proxy.removeIntermediateHost( intermediate );
					}
				}
			}else{
			
				Debug.out( "Proxy entry missing!" );
			}
		}
	}
	
	public void
	unload()
	{
		unload( false );
	}
	
	public void
	unload(
		boolean	for_closedown )
	{
		synchronized( this ){
			
			unloaded = true;
		}
		
		try{
			if ( router != null ){
				
				router.destroy();
				
				router = null;
			}
			
			if ( socks_proxy != null ){
				
				socks_proxy.destroy();
				
				socks_proxy = null;
			}
			
			if ( !for_closedown ){
				
				if ( config_model != null ){
					
					config_model.destroy();
					
					config_model = null;
				}
				
				if ( view_model != null ){
					
					view_model.destroy();
					
					view_model = null;
				}
				
				if ( ui_view != null ){
					
					ui_view.unload();
				}
				
				if ( tracker != null ){
					
					tracker.destroy();
				}
				
				if ( magnet_handler != null ){
				
					MagnetURIHandler uri_handler = MagnetURIHandler.getSingleton();
				
					uri_handler.removeListener( magnet_handler );
				}
				
				if ( message_handler != null ){
					
					message_handler.destroy();
					
					message_handler = null;
				}
			}
		}finally{
			
			if ( lock_stream != null ){
				
				try{
					lock_stream.close();
					
				}catch( Throwable e ){
				}
				
				lock_stream = null;
			}
			
			if ( lock_file != null ){
				
				lock_file.delete();
				
				lock_file = null;
			}
		}
	}
	

	private boolean
	testPort(
		int		port )
	{
		ServerSocketChannel ssc = null;

		try{	
			ssc = ServerSocketChannel.open();
			
			ssc.socket().bind( new InetSocketAddress( "127.0.0.1", port ));
			
			return( true );
			
		}catch( Throwable e ){
			
		}finally{
			
			if ( ssc != null ){
				
				try{
					ssc.close();
					
				}catch( Throwable e ){
					
				}
			}
		}
		
		return( false );
	}
	
	private int
	allocatePort(
		int				def,
		int				exclude_port )
	{
		for ( int i=0;i<32;i++){
			
			int port = 20000 + RandomUtils.nextInt( 20000 );
			
			if ( port == exclude_port ){
				
				continue;
			}
			
			ServerSocketChannel ssc = null;

			try{	
				ssc = ServerSocketChannel.open();
				
				ssc.socket().bind( new InetSocketAddress( "127.0.0.1", port ));
				
				return( port );
				
			}catch( Throwable e ){
				
			}finally{
				
				if ( ssc != null ){
					
					try{
						ssc.close();
						
					}catch( Throwable e ){
						
					}
				}
			}
		}
		
		return( def );
	}
	
	private class
	ProxyMapEntry
	{
		private long	created = SystemTime.getMonotonousTime();
		
		private	String	host;
		private String	intermediate_host;
		
		private
		ProxyMapEntry(
			String		_host,
			String		_intermediate_host )
		{
			host				= _host;
			intermediate_host	= _intermediate_host;
		}
		
		private long
		getCreateTime()
		{
			return( created );
		}
		
		private String
		getHost()
		{
			return( host );
		}
		
		private String
		getIntermediateHost()
		{
			return( intermediate_host );
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		if ( args.length == 0 ){
			
			System.out.println( "Usage: config_dir");
			
			return;
		}
		
		File config_dir = new File( args[0] );
		
		if ( !config_dir.isDirectory()){
			
			if ( !config_dir.mkdirs()){
				
				System.out.println( "Failed to make directory '" + config_dir + "'" );
				
				return;
			}
		}
		
		boolean bootstrap 	= args.length > 1 && args[1].equals( "true" );
				
		if ( bootstrap ){
			
			System.out.println( "Bootstrap Node" );
		}
		
		boolean vuze_dht 	= args.length > 2 && args[2].equals( "true" );
		
		if ( vuze_dht ){
			
			System.out.println( "Vuze DHT" );
		}
		
		final I2PHelperRouter[] f_router = { null };
		
		I2PHelperAdapter adapter = 
			new I2PHelperAdapter() 
			{
				public void 
				log(
					String str ) 
				{
					System.out.println( str );
				}
				
				public String
				getMessageText(
					String		key )
				{
					return( "!" + key + "!" );
				}
				
				public String
				getMessageText(
					String		key,
					String...	args )
				{
					return( "!" + key + "!: " + args );
				}
				
				public void
				tryExternalBootstrap(
					boolean	force )
				{
					log( "External bootstrap test" );
					
					try{
						URL url = new URL( BOOTSTRAP_SERVER );
						
						URLConnection connection = url.openConnection();
						
						connection.connect();
						
						InputStream is = connection.getInputStream();
						
						Map map = BDecoder.decode( new BufferedInputStream( is ));
									
						List<Map> nodes = (List<Map>)map.get( "nodes" );
						
						log( "I2P Bootstrap server returned " + nodes.size() + " nodes" );
						
						for ( Map m: nodes ){
							
							NodeInfo ni = f_router[0].getDHT().heardAbout( m );
							
							if ( ni != null ){
								
								log( "    imported " + ni );
							}
						}
				
					}catch( Throwable e ){
						
						log( "External bootstrap failed: " + Debug.getNestedExceptionMessage(e));
					}
				}
				
				public void 
				incomingConnection(
					I2PSocket socket )
					
					throws Exception 
				{
					log( "incoming connections not supported" );
					
					socket.close();
				}
				public void 
				outgoingConnection(
					I2PSocket socket )
				{	
				}
				
				public PluginInterface
				getPluginInterface()
				{
					return( null );
				}
			};
			
		try{
			I2PHelperRouter router = f_router[0] = new I2PHelperRouter( new HashMap<String, Object>(), bootstrap, vuze_dht, adapter );
			
				// 19817 must be used for bootstrap node
			
			router.initialise( config_dir, 17654, bootstrap?19817:28513 );
			//router.initialise( config_dir, 29903 ); // 7654 );
			//router.initialise( config_dir, 7654 );

			I2PHelperTracker tracker = new I2PHelperTracker( adapter, router.getDHT());
			
			I2PHelperConsole console = new I2PHelperConsole();
			
			I2PHelperSocksProxy	socks_proxy = new I2PHelperSocksProxy( router, 8964, adapter );
			
			I2PHelperBootstrapServer bootstrap_server = null;
			
			if ( bootstrap ){
				
				bootstrap_server = new I2PHelperBootstrapServer( 60000, router );
			}
			
			System.out.println( "Accepting commands" );
			
			while( true ){
				
				String line = console.readLine( ">" );
				
				if ( line == null ){
					
					break;
				}
				
				try{
					line = line.trim();
					
					if ( line.equals( "quit" )){
						
						break;
						
					}else if ( line.equals( "extboot" )){
						
						adapter.tryExternalBootstrap( true );
						
					}else{
						
						executeCommand(line, router, tracker, adapter);
					}
					
					if ( bootstrap_server != null ){
						
						System.out.println( "Bootstrap http: " + bootstrap_server.getString());
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
			
			router.destroy();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
