/*
 * Created on Mar 19, 2014
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.*;

import net.i2p.CoreVersion;
import net.i2p.I2PAppContext;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.client.streaming.I2PSocketOptions;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelManagerFacade;
import net.i2p.router.transport.FIFOBandwidthLimiter;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;


public class 
I2PHelperRouter 
{
	private static final String 	i2p_internal_host 	= "127.0.0.1";

	public static final String	PARAM_SEND_KBS				= "azi2phelper.rate.send.max";
	public static final int		PARAM_SEND_KBS_DEFAULT		= 50;
	
	public static final String	PARAM_RECV_KBS				= "azi2phelper.rate.recv.max";
	public static final int		PARAM_RECV_KBS_DEFAULT		= 50;
	
	public static final String	PARAM_SHARE_PERCENT			= "azi2phelper.share.percent";
	public static final int		PARAM_SHARE_PERCENT_DEFAULT	= 25;
	
	private static final boolean	FULL_STATS = false;

	private final I2PHelperPlugin			plugin;
	private final File						config_dir;

	private final Map<String,Object>		router_properties;
	
	
	private final boolean					is_bootstrap_node;
	private final boolean					is_vuze_dht;
	private final boolean					force_new_address;
	private final I2PHelperAdapter			adapter;
	
	private boolean		is_external_router;
	private String		i2p_host;
	private int			i2p_port;
	
	
	private volatile Router 			router;
	
	private Properties						sm_properties = new Properties();	
	
	private I2PHelperRouterDHT[]			dhts;
	
	private Map<String,ServerInstance>		servers = new HashMap<String, ServerInstance>();
	
	private AESemaphore			init_sem	= new AESemaphore( "I2PRouterInit" );
	
	private volatile boolean	destroyed;
	
	protected
	I2PHelperRouter(
		I2PHelperPlugin			_plugin,
		File					_config_dir,
		Map<String,Object>		_properties,
		boolean					_is_bootstrap_node,
		boolean					_is_vuze_dht,
		boolean					_force_new_address,
		int						_dht_count,
		I2PHelperAdapter		_adapter )
	{
		plugin				= _plugin;
		config_dir			= _config_dir;
		router_properties	= _properties;
		is_bootstrap_node	= _is_bootstrap_node;		// could be props one day
		is_vuze_dht			= _is_vuze_dht;
		force_new_address	= _force_new_address;
		adapter				= _adapter;
		
		dhts = new I2PHelperRouterDHT[_dht_count ];

		for ( int i=0;i<dhts.length;i++){
		
			I2PHelperRouterDHT dht = new I2PHelperRouterDHT( this, config_dir, i, is_bootstrap_node, is_vuze_dht, force_new_address, adapter );
			
			dhts[i] = dht;
		}
	}
	
	private int
	getIntegerParameter(
		String		name )
	{
		int	def;
		
		if ( name == PARAM_SEND_KBS ){
			
			def = PARAM_SEND_KBS_DEFAULT;
			
		}else if ( name == PARAM_RECV_KBS ){
			
			def = PARAM_RECV_KBS_DEFAULT;
			
		}else if ( name == PARAM_SHARE_PERCENT ){
			
			def = PARAM_SHARE_PERCENT_DEFAULT;
			
		}else{
			
			Debug.out( "Unknown parameter: " + name );
			
			return( 0 );
		}
		
		Object val = router_properties.get( name );
		
		if ( val instanceof Number ){
			
			return(((Number)val).intValue());
		}else{
			
			return( def );
		}
	}
	
	private void
	addRateLimitProperties(
		Properties		props )
	{
			// router bandwidth
			
		int	base_in 		= is_bootstrap_node?500:getIntegerParameter(PARAM_RECV_KBS);
		
		if ( base_in <= 0 ){
			base_in = 100*1024;	// unlimited - 100MB/sec
		}
		
			// got to keep some bytes flowing here
		
		if ( base_in < 10 ){
			base_in = 10;
		}
		
		int burst_in_ks 	= base_in+(base_in/10);
		int burst_in_k		= burst_in_ks*20;
		
		int	base_out 		= is_bootstrap_node?500:getIntegerParameter(PARAM_SEND_KBS);
		
		if ( base_out <= 0 ){
			base_out = 100*1024;	// unlimited - 100MB/sec
		}
		
			// got to keep some bytes flowing here
		
		if ( base_out < 10 ){
			base_out = 10;
		}
		
		int burst_out_ks 	= base_out+(base_out/10);
		int burst_out_k		= burst_out_ks*20;
	
		
		int share_pct	= is_bootstrap_node?75:getIntegerParameter(PARAM_SHARE_PERCENT);
		
		props.put( "i2np.bandwidth.inboundBurstKBytes", burst_in_k );
		props.put( "i2np.bandwidth.inboundBurstKBytesPerSecond", burst_in_ks );
		props.put( "i2np.bandwidth.inboundKBytesPerSecond", base_in );
		
		props.put( "i2np.bandwidth.outboundBurstKBytes", burst_out_k );
		props.put( "i2np.bandwidth.outboundBurstKBytesPerSecond", burst_out_ks );
		props.put( "i2np.bandwidth.outboundKBytesPerSecond", base_out );	
		
		props.put( "router.sharePercentage", share_pct );
	}
	
	public Map<String,Object>
	getProperties()
	{
		return( router_properties );
	}
	
	public void
	updateProperties()
	{
		Properties props = new Properties();
		
		addRateLimitProperties( props );
		
		I2PHelperUtils.normalizeProperties( props );
		
		router.saveConfig( props, null );
		
		router.getContext().bandwidthLimiter().reinitialize();
	}
	
	private void
	setupSMOpts(
		RouterContext		router_ctx )
	{
		Properties opts = sm_properties;
				
		if ( router_ctx != null ){
		
			opts.putAll( router_ctx.getProperties());
		}
				
        // outbound speed limit
        // "i2cp.outboundBytesPerSecond"
        // tell router -> 
        //Properties newProps = new Properties();
        //newProps.putAll(_opts);
        // sess.updateOptions(newProps);
        
        
        // Dont do this for now, it is set in I2PSocketEepGet for announces,
        // we don't need fast handshake for peer connections.
        //if (opts.getProperty("i2p.streaming.connectDelay") == null)
        //    opts.setProperty("i2p.streaming.connectDelay", "500");
        if (opts.getProperty(I2PSocketOptions.PROP_CONNECT_TIMEOUT) == null)
            opts.setProperty(I2PSocketOptions.PROP_CONNECT_TIMEOUT, "75000");
        if (opts.getProperty("i2p.streaming.inactivityTimeout") == null)
            opts.setProperty("i2p.streaming.inactivityTimeout", "240000");
        if (opts.getProperty("i2p.streaming.inactivityAction") == null)
            opts.setProperty("i2p.streaming.inactivityAction", "1"); // 1 == disconnect, 2 == ping
        if (opts.getProperty("i2p.streaming.initialWindowSize") == null)
            opts.setProperty("i2p.streaming.initialWindowSize", "1");
        if (opts.getProperty("i2p.streaming.slowStartGrowthRateFactor") == null)
            opts.setProperty("i2p.streaming.slowStartGrowthRateFactor", "1");
        //if (opts.getProperty("i2p.streaming.writeTimeout") == null)
        //    opts.setProperty("i2p.streaming.writeTimeout", "90000");
        //if (opts.getProperty("i2p.streaming.readTimeout") == null)
        //    opts.setProperty("i2p.streaming.readTimeout", "120000");
        //if (opts.getProperty("i2p.streaming.maxConnsPerMinute") == null)
        //    opts.setProperty("i2p.streaming.maxConnsPerMinute", "2");
        //if (opts.getProperty("i2p.streaming.maxTotalConnsPerMinute") == null)
        //    opts.setProperty("i2p.streaming.maxTotalConnsPerMinute", "8");
        //if (opts.getProperty("i2p.streaming.maxConnsPerHour") == null)
        //    opts.setProperty("i2p.streaming.maxConnsPerHour", "20");
        if (opts.getProperty("i2p.streaming.enforceProtocol") == null)
            opts.setProperty("i2p.streaming.enforceProtocol", "true");
        //if (opts.getProperty("i2p.streaming.disableRejectLogging") == null)
        //    opts.setProperty("i2p.streaming.disableRejectLogging", "true");
        if (opts.getProperty("i2p.streaming.answerPings") == null)
            opts.setProperty("i2p.streaming.answerPings", "false");
        
        opts.setProperty( "i2p.streaming.disableRejectLogging", "false");
        opts.setProperty( "i2cp.dontPublishLeaseSet", "false" );
        opts.setProperty( "inbound.length", "2" );
        opts.setProperty( "inbound.lengthVariance", "0" );
        opts.setProperty( "outbound.length", "2" ); 
        opts.setProperty( "outbound.lengthVariance", "0" ); 
        opts.setProperty( "inbound.quantity", "4" ); 
        opts.setProperty( "outbound.quantity", "4" );
	}
	
	private void
	init(
		File		config_dir )
		
		throws Exception
	{
		if ( !config_dir.isDirectory()){
			
			config_dir.mkdirs();
			
			if ( !config_dir.isDirectory()){
				
				throw( new Exception( "Failed to create config dir '" + config_dir +"'" ));
			}
		}
				
			// setting this prevents stdout/stderr from being hijacked
		
		System.setProperty( "wrapper.version", "dummy" );	
	}
	
	protected void
	initialiseRouter(
		int			i2p_internal_port,
		int			i2p_external_port )
	
		throws Exception
	{	
		try{
			is_external_router 	= false;
			i2p_host			= i2p_internal_host;
			i2p_port			= i2p_internal_port;

			init( config_dir );
			
			new File( config_dir, "router.ping" ).delete();
			
			File router_config = new File( config_dir, "router.config" );
			
			Properties router_props = I2PHelperUtils.readProperties( router_config );
			
				// router config
			
			router_props.put( "i2cp.port", i2p_internal_port );
			router_props.put( "i2np.upnp.enable", false );
			router_props.put( "i2p.streaming.answerPings", false );		// reverted this to false, 29/5/14 as things seem to be working
			
				// Note that TCP defaults to the same as UDP port
						
			router_props.put( "i2np.udp.port", i2p_external_port );
			router_props.put( "i2np.udp.internalPort", i2p_external_port );
	
			addRateLimitProperties( router_props );
			
				// router pools
					
			router_props.put( "router.inboundPool.backupQuantity", 0 );
			router_props.put( "router.inboundPool.length", 2 );
			router_props.put( "router.inboundPool.lengthVariance", 0 );
			router_props.put( "router.inboundPool.quantity", 2 );
			router_props.put( "router.outboundPool.backupQuantity", 0 );
			router_props.put( "router.outboundPool.length", 2 );
			router_props.put( "router.outboundPool.lengthVariance", 0 );
			router_props.put( "router.outboundPool.quantity", 2 );
									
			I2PHelperUtils.normalizeProperties( router_props );
			
			I2PHelperUtils.writeProperties( router_config, router_props );
			
			router_props.setProperty( "router.configLocation", router_config.getAbsolutePath());
			router_props.setProperty( "i2p.dir.base" , config_dir.getAbsolutePath());
			router_props.setProperty( "i2p.dir.config" , config_dir.getAbsolutePath());

			if ( FULL_STATS ){
				
				Debug.out( "Turn off stats sometime!!!!" );
				
				router_props.put( "stat.full", "true" );
			}
			
			I2PAppContext ctx = I2PAppContext.getGlobalContext();

			ctx.logManager().setDefaultLimit( "ERROR" ); // "WARN" );

			router = new Router( router_props );
				
			router.setKillVMOnEnd( false );
			
			router.runRouter();
				
			// not needed since 0.9.13 as setting moved to Router constructor
			// router.setKillVMOnEnd( false );	// has to be done again after run phase as set true in that code :(

			RouterContext router_ctx = router.getContext();
			
			long start = SystemTime.getMonotonousTime();
			
			adapter.log( "Waiting for internal router startup on " + i2p_host + ":" + i2p_port );
						
			while( true ){
				
				if ( destroyed ){
					
					throw( new Exception( "Router has been shutdown" ));
				}
				
				try{
					Socket s = new Socket( Proxy.NO_PROXY );
					
					s.connect( new InetSocketAddress( i2p_internal_host, i2p_internal_port ), 1000 );
				
					s.close();
				
					break;
					
				}catch( Throwable e ){
					
					try{
						Thread.sleep(250);
						
					}catch( Throwable f ){
						
					}
				}
			}
				
			setupSMOpts( router_ctx );
            
			adapter.log( "Router startup complete: version=" + CoreVersion.VERSION + ", elapsed=" + (SystemTime.getMonotonousTime() - start ));

			init_sem.releaseForever();
			
		}catch( Throwable e ){
			
			destroy();
			
			throw( new Exception( "Initialisation failed", e ));
		}
	}
	
	protected void
	initialiseRouter(
		String		i2p_separate_host,
		int			i2p_separate_port )
	
		throws Exception
	{	
		try{
			is_external_router = true;
			
			i2p_host	= i2p_separate_host;
			i2p_port	= i2p_separate_port;
					
			
				// we need this so that the NameService picks up the hosts file in the plugin dir when using
				// an external router (dunno how/if to delegate lookups to the external router's hosts...)
						
			System.setProperty( "i2p.dir.config", config_dir.getAbsolutePath());
			
			init( config_dir );
		
			long start = SystemTime.getMonotonousTime();
			
			adapter.log( "Waiting for external router startup on " + i2p_host + ":" + i2p_port );
						
			while( true ){
				
				if ( destroyed ){
					
					throw( new Exception( "Router has been shutdown" ));
				}
				
				try{
					Socket s = new Socket( Proxy.NO_PROXY );
					
					s.connect( new InetSocketAddress( i2p_separate_host, i2p_separate_port ), 1000 );
				
					s.close();
				
					break;
					
				}catch( Throwable e ){
					
					try{
						Thread.sleep(1000);
						
					}catch( Throwable f ){
						
					}
				}
			}
			
			setupSMOpts( null );
			
			adapter.log( "Router startup complete, elapsed=" + (SystemTime.getMonotonousTime() - start ));
			       
			init_sem.releaseForever();
			
		}catch( Throwable e ){
			
			destroy();
			
			throw( new Exception( "Initialisation failed", e ));	
		}
	}
	
	protected void
	waitForInitialisation()
	
		throws Exception
	{
		init_sem.reserve();
		
		if ( destroyed ){
			
			throw( new Exception( "Router destroyed" ));
		}
	}
	
	protected void
	initialiseDHTs()
	
		throws Exception
	{
			// second DHT is lazy initialised if/when selected
		
		dhts[0].initialiseDHT( i2p_host, i2p_port, sm_properties );
	}
	
	public I2PHelperRouterDHT
	selectDHT()
	{
		return(selectDHT((String[])null ));
	}
	
	public I2PHelperRouterDHT
	selectDHT(
		byte[]	torrent_hash )
	{
		if ( plugin != null ){
					
			try{
				Download download = plugin.getPluginInterface().getDownloadManager().getDownload( torrent_hash );
				
				return( selectDHT( download ));
			
			}catch( Exception e ){
			}
		}
		
		return( selectDHT());
	}
	
	public I2PHelperRouterDHT
	selectDHT(
		Download	download )
			
	{ 
		if ( download == null ){
		
			return( selectDHT());
			
		}else{
			
			return( selectDHT( PluginCoreUtils.unwrap( download ).getDownloadState().getNetworks()));
		}
	}
	
	public I2PHelperRouterDHT
	selectDHT(
		Map<String,Object>		options )
	{
		String[] peer_networks = options==null?null:(String[])options.get( "peer_networks" );
		
		return( selectDHT( peer_networks ));
	}
	
	public I2PHelperRouterDHT
	selectDHT(
		String[]		peer_networks )
	{
		String str = "";
		
		if ( peer_networks != null ){
			
			for ( String net: peer_networks ){
			
				str += (str.length()==0?"":", ") + net;
			}
		}
			
		if ( dhts.length < 2 ){
			
			return( dhts[0] );
		}
		
		if ( peer_networks == null || peer_networks.length == 0 ){
			
			return( dhts[0] );
		}
		
		if ( peer_networks.length == 1 && peer_networks[0] == AENetworkClassifier.AT_I2P ){
			
			I2PHelperRouterDHT dht = dhts[1];
			
			if ( !dht.isDHTInitialised()){
				
				try{
					dht.initialiseDHT( i2p_host, i2p_port, sm_properties );
					
				}catch( Throwable e ){
				}
			}
			
			return( dht );
		}
		
		return( dhts[0] );
	}
	
	public I2PHelperRouterDHT[]
	getAllDHTs()
	{
		return( dhts );
	}
	
	public Destination
	lookupDestination(
		byte[]		hash )
	
		throws Exception
	{
		return( dhts[0].lookupDestination( hash ));
	}
	
	protected ServerInstance 
	createServer(
		final String			server_id,
		ServerAdapter			server_adapter )
		
		throws Exception
	{
		final ServerInstance server;
		
		synchronized( this ){
			
			if ( destroyed ){
				
				throw( new Exception( "Router destroyed" ));
			}
					
			ServerInstance existing_server = servers.get( server_id );
				
			if ( existing_server != null ){
				
				return( existing_server );
			}
			
			server = new ServerInstance( server_id, server_adapter );
			
			servers.put( server_id, server );
		}
		
		new AEThread2( "I2PServer:asyncinit" )
		{
			public void
			run()
			{
				try{
					server.initialise();
					
				}catch( Throwable e ){
					
					Debug.out( e );
					
					synchronized( I2PHelperRouter.this ){
						
						servers.remove( server_id );
					}
				}
			}
		}.start();
		
		return( server );
	}
	
	protected void
	destroy()
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed	= true;
			
			init_sem.releaseForever();
			
			try{
				if ( router != null ){
		
					router.shutdown( Router.EXIT_HARD );
					
					router = null;
				}
				
				for ( I2PHelperRouterDHT dht: dhts ){
				
					dht.destroy();
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
			
			for ( ServerInstance server: servers.values()){
				
				try{
					server.destroy();
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
			
			servers.clear();
		}
	}
	
	protected void
	logInfo()
	{
		if ( destroyed ){
			
			return;
		}
		
		Router 			router 	= this.router;
		
		for ( int i=0;i<dhts.length;i++){
			
			I2PHelperDHT	dht		= dhts[i].getDHT();

			if( dhts.length > 1 ){
				
				adapter.log( "DHT " + i );
			}
			
			if ( dht == null ){
				
				adapter.log( "DHT is inactive" );
				
			}else{
				
				String html = dht.renderStatusHTML();
				
				if ( html.length() > 0 ){
					adapter.log( html );
				}
				
				adapter.log( dht.getStats());
			}
		}
		
		if ( router == null ){
			
			if ( !is_external_router ){
			
				adapter.log( "Router is inactive" );
			}
		}else{
			
			RouterContext router_ctx = router.getContext();
			
			adapter.log( "Known routers=" + router_ctx.netDb().getKnownRouters() + ", lease-sets=" + router_ctx.netDb().getKnownLeaseSets());
			
			TunnelManagerFacade tunnel_manager = router_ctx.tunnelManager();
			
			int	exploratory_tunnels		= tunnel_manager.getFreeTunnelCount() + tunnel_manager.getOutboundTunnelCount();
			int	client_tunnels			= tunnel_manager.getInboundClientTunnelCount()  + tunnel_manager.getOutboundClientTunnelCount();
			int participating_tunnels	= tunnel_manager.getParticipatingCount();
			
			adapter.log( "Tunnels: exploratory=" + exploratory_tunnels + ", client=" + client_tunnels + ", participating=" + participating_tunnels ); 
	
			adapter.log( "Throttle: msg_delay=" + router_ctx.throttle().getMessageDelay() + ", tunnel_lag=" + router_ctx.throttle().getTunnelLag() + ", tunnel_stat=" +  router_ctx.throttle().getTunnelStatus());
			
			FIFOBandwidthLimiter bwl = router_ctx.bandwidthLimiter();
			
			long recv_rate = (long)bwl.getReceiveBps();
			long send_rate = (long)bwl.getSendBps();
			
			//RateStat sendRate = router_ctx.statManager().getRate("bw.sendRate");
		    //RateStat recvRate = router_ctx.statManager().getRate("bw.recvRate"); 
			//System.out.println( "Rates: send=" + sendRate.getRate(60*1000).getAverageValue() + ", recv=" + recvRate.getRate(60*1000).getAverageValue());
		    
			if ( FULL_STATS ){
			
				adapter.log( "Lease repubs=" + router_ctx.statManager().getRate("netDb.republishLeaseSetCount" ).getLifetimeEventCount());
			}
			
			adapter.log( 
				"Rates: send=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(send_rate) +
				", recv=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(recv_rate) +
				"; Limits: send=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(bwl.getOutboundKBytesPerSecond()*1024) + 
				", recv=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(bwl.getInboundKBytesPerSecond()*1024));
		}
	}
	
	public String
	getStatusText()
	{	
		if ( destroyed ){
			
			return( "Destroyed" );
		}
		
		Router 			router 	= this.router;
		
		String dht_status = "";
		
		for ( int i=0;i<dhts.length;i++){
			
			I2PHelperDHT	dht		= dhts[i].getDHT();
			
			if ( dht != null ){
				
				dht_status += (dht_status.length()==0?"":"\n") + dht.getStatusString();
			}
		}
		
		if ( router == null  ){
			
			if ( is_external_router ){
				
				if ( dht_status.length() > 0 ){
					
					return( dht_status + "\nExternal Router" );
				}
			}
			
			return( adapter.getMessageText( "azi2phelper.status.initialising" ));
		}

		RouterContext router_ctx = router.getContext();
		
		FIFOBandwidthLimiter bwl = router_ctx.bandwidthLimiter();
		
		long recv_rate = (long)bwl.getReceiveBps();
		long send_rate = (long)bwl.getSendBps();
		
		long send_limit = bwl.getOutboundKBytesPerSecond()*1024;
		long recv_limit = bwl.getInboundKBytesPerSecond()*1024;
		
		String recv_limit_str = recv_limit > 50*1024*1024?"": (	"[" + DisplayFormatters.formatByteCountToKiBEtcPerSec(recv_limit) + "] " );
		String send_limit_str = send_limit > 50*1024*1024?"": (	"[" + DisplayFormatters.formatByteCountToKiBEtcPerSec(send_limit) + "] " );
		
		String router_str = 
			adapter.getMessageText( "azi2phelper.status.router",
					router_ctx.throttle().getTunnelStatus(),
					recv_limit_str + DisplayFormatters.formatByteCountToKiBEtcPerSec(recv_rate),
					send_limit_str + DisplayFormatters.formatByteCountToKiBEtcPerSec(send_rate));

		if ( dht_status.length() > 0 ){
			
			dht_status += "\n";
		}
		
		return( dht_status + router_str );
	}
	
	protected interface
	ServerAdapter
	{
		public void
		incomingConnection(
			ServerInstance	server,
			I2PSocket		socket )
			
			throws Exception;
	}
	
	protected class
	ServerInstance
	{
		private final String			server_id;
		private final ServerAdapter		server_adapter;
		
		private I2PSession 			session;
		private I2PSocketManager 	socket_manager;
		private I2PServerSocket		server_socket;

		private String				b32_dest;
		
		private volatile boolean	server_destroyed;
		
		private Map<String,Object>		user_properties = new HashMap<String, Object>();
		
		private
		ServerInstance(
			String				_server_id,
			ServerAdapter		_adapter )
			
			throws Exception
		{
			server_id			= _server_id;
			server_adapter		= _adapter;
			
			File dest_key_file 	= new File( config_dir,  server_id + "_dest_key.dat" );
	        
			Destination	dest = null;
			
			try{
				if ( dest_key_file.exists()){
		    	   
		    		InputStream is = new FileInputStream( dest_key_file );
		    		
		    		try{
		    			dest = new Destination();
		    		
		    			dest.readBytes( is );
		    			
		    		}finally{
		    			
		    			is.close();
		    		}
				}
				
				if ( dest == null ){
					
					FileOutputStream	os = new FileOutputStream( dest_key_file );
					
					try{
						dest = I2PClientFactory.createClient().createDestination( os );
						
					}finally{
						
						os.close();
					}
				}
				
				b32_dest	= Base32.encode( dest.calculateHash().getData()) + ".b32.i2p";

				log( "address=" + b32_dest );
				
			}catch( Throwable e ){
				
				log( "Create failed: " + Debug.getNestedExceptionMessage( e ));
				
				throw( new Exception( "Failed to create server destination", e ));
			}
		}
		
		private void
		initialise()
		
			throws Exception
		{
			try{
				log( "Initializing..." );
					        
				File dest_key_file 	= new File( config_dir,  server_id + "_dest_key.dat" );
         	
		        I2PSocketManager	sm = null;

		        long start = SystemTime.getMonotonousTime();
		        
		        while( true ){
			
		        	InputStream is = new FileInputStream( dest_key_file );
		        	
			    	try{
			    		sm = I2PSocketManagerFactory.createManager( is, i2p_host, i2p_port, sm_properties );
			    	
			    	}finally{
			    		
			    		is.close();
			    	}
			    	
					if ( sm != null ){
						
						break;
					
					}else{
						
							// I've seen timeouts with 3 mins, crank it up
						
						if ( SystemTime.getMonotonousTime() - start > 15*60*1000 ){
							
							throw( new Exception( "Timeout creating socket manager" ));
						}
						
						Thread.sleep( 5000 );
									
						if ( server_destroyed ){
							
							throw( new Exception( "Server destroyed" ));
						}
					}
		        }
		        		
				log( "Waiting for socket manager startup" );
				
		        start = SystemTime.getMonotonousTime();

				while( true ){
					
					if ( server_destroyed ){
						
						sm.destroySocketManager();
						
						throw( new Exception( "Server destroyed" ));
					}
					
					session = sm.getSession();
					
					if ( session != null ){
						
						break;
					}
					
					if ( SystemTime.getMonotonousTime() - start > 3*60*1000 ){
						
						throw( new Exception( "Timeout waiting for socket manager startup" ));
					}

					
					Thread.sleep(250);
				}
								
				Destination my_dest = session.getMyDestination();
				
				b32_dest	= Base32.encode( my_dest.calculateHash().getData()) + ".b32.i2p";
				
				log( "Socket manager startup complete" );

				socket_manager	= sm;
						
				server_socket = socket_manager.getServerSocket();
				
				new AEThread2( "I2P:accepter" )
				{
					public void
					run()
					{
						while( !server_destroyed ){
							
							try{
								I2PSocket socket = server_socket.accept();
								
								if ( socket == null ){
									
									if ( server_destroyed ){
										
										break;
										
									}else{
										
										Thread.sleep(500);
									}
								}else{
									try{
									
										server_adapter.incomingConnection( ServerInstance.this, socket );
									
									}catch( Throwable e ){
										
										Debug.out( e );
										
										try{
											socket.close();
											
										}catch( Throwable f ){
										}
									}
								}
							}catch( Throwable e ){
								
								Debug.out( e );
								
								break;
							}
						}
					}
				}.start();
							

			}catch( Throwable e ){
				
				log( "Initialisation failed: " + Debug.getNestedExceptionMessage( e ));
				
				destroy();
				
				throw( new Exception( "Initialisation failed", e ));
				
			}finally{
				
				synchronized( this ){
					
					if ( server_destroyed ){
						
						destroy();
					}
				}
			}			
		}
		
		protected String
		getB32Dest()
		{
			return( b32_dest );
		}
		
		protected void
		setUserProperty(
			String	key,
			Object	value )
		{	
			synchronized( user_properties ){
				
				user_properties.put( key, value );
			}
		}
			
		protected Object
		getUserProperty(
			String	key )
		{	
			synchronized( user_properties ){
				
				return( user_properties.get( key ));
			}
		}
		
		private void
		destroy()
		{
			synchronized( this ){
				
				if ( server_destroyed ){
					
					return;
				}
				
				server_destroyed	= true;
				
				try{

					if ( socket_manager != null ){
						
						socket_manager.destroySocketManager();
						
						socket_manager = null;
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
				
				try{
					if ( server_socket != null ){
						
						server_socket.close();
						
						server_socket = null;
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
		
		private void
		log(
			String	str )
		{
			adapter.log( server_id + ": " + str );
		}
	}
}
