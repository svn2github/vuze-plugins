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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import net.i2p.CoreVersion;
import net.i2p.I2PAppContext;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.client.streaming.I2PSocketOptions;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.data.PrivateKeyFile;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelManagerFacade;
import net.i2p.router.transport.FIFOBandwidthLimiter;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.parg.azureus.plugins.networks.i2p.dht.*;


public class 
I2PHelperRouter 
{
	private static final String 	i2p_host 	= "127.0.0.1";

	private boolean					is_bootstrap_node;
	private I2pHelperAdapter		logger;
	
	private static final boolean	FULL_STATS = false;
	
	private Router 		router;
	private I2PSession 	session;
	private DHT			dht;
	
	private boolean		destroyed;
	
	protected
	I2PHelperRouter(
		boolean				bootstrap_node,
		I2pHelperAdapter		_logger )
	{
		is_bootstrap_node	= bootstrap_node;
		logger				= _logger;
	}
	
	private Properties
	readProperties(
		File		file )
	{
		Properties props = new Properties();
		
		try{
		
			if ( file.exists()){
				
				InputStream is = new FileInputStream( file );
			
				try{
					props.load( new InputStreamReader( is, "UTF-8" ));
					
				}finally{
					
					is.close();
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		return( props );
	}
	
	private void
	normalizeProperties(
		Properties	props )
	{
		Set<Object> keys = props.keySet();
		
		for ( Object key: keys ){
			
			Object value = props.get( key );
			
			if ( !(value instanceof String )){
				
				props.put( key, String.valueOf( value ));
			}
		}
	}
	

	private void
	writeProperties(
		File		file,
		Properties	_props )
	{
		try{
			Properties props = 
				new Properties()
				{
			    	public Enumeration<Object> keys() {
			    	
			    		List<String> keys = new ArrayList<String>((Set<String>)(Object)keySet());
			    		
			    		Collections.sort( keys );
			    		
			    		return( new Vector<Object>( keys ).elements());
			        }
				};
			
			props.putAll( _props );
			
			FileOutputStream os = new FileOutputStream( file );
			
			try{
				props.store( new OutputStreamWriter(os, "UTF-8" ), "" );
				
			}finally{
				
				os.close();
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
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
	initialise(
		File		config_dir,
		int			i2p_internal_port,
		int			i2p_external_port )
	
		throws Exception
	{	
		try{
			init( config_dir );
			
			new File( config_dir, "router.ping" ).delete();
			
			File router_config = new File( config_dir, "router.config" );
			
			Properties router_props = readProperties( router_config );
			
				// router config
			
			router_props.put( "i2cp.port", i2p_internal_port );
			router_props.put( "i2np.upnp.enable", false );
			router_props.put( "i2p.streaming.answerPings", true );		// testing
			
				// Note that TCP defaults to the same as UDP port
						
			router_props.put( "i2np.udp.port", i2p_external_port );
			router_props.put( "i2np.udp.internalPort", i2p_external_port );
	
				// router bandwidth
				// TODO: review!
			
			int	base 		= is_bootstrap_node?500:50;
			int burst_ks 	= base+(base/10);
			int burst_k		= burst_ks*20;
			
			int share_pct	= is_bootstrap_node?75:25;
			
			router_props.put( "i2np.bandwidth.inboundBurstKBytes", burst_k );
			router_props.put( "i2np.bandwidth.inboundBurstKBytesPerSecond", burst_ks );
			router_props.put( "i2np.bandwidth.inboundKBytesPerSecond", base );
			router_props.put( "i2np.bandwidth.outboundBurstKBytes", burst_k );
			router_props.put( "i2np.bandwidth.outboundBurstKBytesPerSecond", burst_ks );
			router_props.put( "i2np.bandwidth.outboundKBytesPerSecond", base );
			
				// router pools
					
			router_props.put( "router.inboundPool.backupQuantity", 0 );
			router_props.put( "router.inboundPool.length", 2 );
			router_props.put( "router.inboundPool.lengthVariance", 0 );
			router_props.put( "router.inboundPool.quantity", 2 );
			router_props.put( "router.outboundPool.backupQuantity", 0 );
			router_props.put( "router.outboundPool.length", 2 );
			router_props.put( "router.outboundPool.lengthVariance", 0 );
			router_props.put( "router.outboundPool.quantity", 2 );
			
			router_props.put( "router.sharePercentage", share_pct );
						
			normalizeProperties( router_props );
			
			writeProperties( router_config, router_props );
			
			router_props.setProperty( "router.configLocation", router_config.getAbsolutePath());
			router_props.setProperty( "i2p.dir.base" , config_dir.getAbsolutePath());
			router_props.setProperty( "i2p.dir.config" , config_dir.getAbsolutePath());

			if ( FULL_STATS ){
				
				Debug.out( "Turn off stats sometime!!!!" );
				
				router_props.put( "stat.full", "true" );
			}
			
			router = new Router( router_props );
				
			router.setKillVMOnEnd( false );
			
			router.runRouter();
				
			router.setKillVMOnEnd( false );	// has to be done again after run phase as set true in that code :(

			RouterContext router_ctx = router.getContext();
			
			logger.log( "Waiting for router startup" );;
						
			while( true ){
				
				try{
					Socket s = new Socket( i2p_host, i2p_internal_port );
				
					s.close();
				
					break;
					
				}catch( Throwable e ){
					
					try{
						Thread.sleep(250);
						
					}catch( Throwable f ){
						
					}
				}
			}
			
			logger.log( "Router startup complete: version=" + CoreVersion.VERSION );
			
            Properties opts = new Properties();
            
            opts.putAll( router_ctx.getProperties());
            
            initialiseDHT( config_dir, i2p_host, i2p_internal_port, opts );
            
		}catch( Throwable e ){
			
			destroy();
			
			throw( new Exception( "Initialisation failed", e ));
			
		}finally{
			
			synchronized( this ){
				
				if ( destroyed ){
					
					destroy();
				}
			}
		}
	}
	
	protected void
	initialise(
		File		config_dir,
		int			i2p_separate_port )
	
		throws Exception
	{	
		try{
			init( config_dir );
		
			logger.log( "Waiting for router startup" );;
						
			while( true ){
				
				try{
					Socket s = new Socket( i2p_host, i2p_separate_port );
				
					s.close();
				
					break;
					
				}catch( Throwable e ){
					
					try{
						Thread.sleep(250);
						
					}catch( Throwable f ){
						
					}
				}
			}
			
			logger.log( "Router startup complete" );
			
            Properties opts = new Properties();
                        
            initialiseDHT( config_dir, i2p_host, i2p_separate_port, opts );
            
		}catch( Throwable e ){
			
			destroy();
			
			throw( new Exception( "Initialisation failed", e ));
			
		}finally{
			
			synchronized( this ){
				
				if ( destroyed ){
					
					destroy();
				}
			}
		}
	}
	
	private void 
	initialiseDHT(
		File			config_dir,
		String			i2p_host,
		int				i2p_internal_port,
		Properties		opts )
		
		throws Exception
	{
		logger.log( "Initializing DHT..." );
		
		File dht_config 	= new File( config_dir,  "dht.config" );
		File dest_key_file 	= new File( config_dir,  "dest_key.dat" );


		I2PAppContext ctx = I2PAppContext.getGlobalContext();

		ctx.logManager().setDefaultLimit( "ERROR" ); // "WARN" );
		
		I2PSocketManager manager;
		
		
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
        
        boolean	use_existing_key = dest_key_file.exists();
        		
		if ( use_existing_key ){
         	
    		InputStream is = new FileInputStream( dest_key_file );
    	
    		try{
    			manager = I2PSocketManagerFactory.createManager( is, i2p_host, i2p_internal_port, opts );
    	
    		}finally{
    		
    			is.close();
    		}
        }else{
        	
        	manager = I2PSocketManagerFactory.createManager( i2p_host, i2p_internal_port, opts );
        }
		
		if ( manager == null ){
			
			throw( new Exception ( "Failed to create socket manager" ));
		}
		
		logger.log( "Waiting for socket manager startup" );
		
		while( true ){
			
			session = manager.getSession();
			
			if ( session != null ){
				
				break;
			}
			
			Thread.sleep(250);
		}
		
		DHTNodes.setBootstrap( is_bootstrap_node ); 

		logger.log( "Socket manager startup complete" );
		
		Properties dht_props;
		
		if ( !use_existing_key ){
			
			new PrivateKeyFile( dest_key_file , session ).write();
			
			dht_props = new Properties();
			
		}else{
		
			dht_props = readProperties( dht_config );
		}
		
		String dht_port_str = dht_props.getProperty( "port" );
		String dht_NID_str 	= dht_props.getProperty( "nid" );
			
		boolean	use_existing_nid = dht_port_str != null && dht_NID_str != null;
				
		if ( use_existing_nid ){
			
			int	dht_port = Integer.parseInt( dht_port_str );
			NID	dht_nid	= new NID( Base32.decode( dht_NID_str ));
	
			dht = new KRPC( ctx, "i2pvuze", session, dht_port, dht_nid, logger );
			
		}else{	
			
    		dht = new KRPC( ctx, "i2pvuze", session, logger );
    	}
					
		if ( !use_existing_nid){
			
			dht_props.setProperty( "dest", session.getMyDestination().toBase64());
			dht_props.setProperty( "port", String.valueOf( dht.getPort()));
			dht_props.setProperty( "nid", Base32.encode( dht.getNID().getData()));
			
			writeProperties( dht_config, dht_props );
		}
		
		if ( !is_bootstrap_node ){
			
			String 	boot_dest 	= "N0e4jfsxy~NYzyr-0bY1nwpnhTza8fn1wWr6IHHOmaIEnbEvgltJvyJn8LWvwlu589mUPhQXQb9BtMrkEan8RZSL4Vo2iFgMCxjTOnfA2dW1~JpL0ddGM28OQITya-1YDgNZFmyX0Me-~RjJjTg31YNozDoosIQ-Uvz2s5aUrzI0gt0r3M4PFUThb0eefd51Yb-eEQMpBb-Hd~EU07yw46ljy2uP4tiEPlWt0l0YR8nbeH0Eg6i3fCoSVgWpSeRjJ9vJeHvwGymO2rPHCSCPgIVwwyqNYpgkqGWnn9Qg97Wc-zrTBiRJp0Dn4lcYvkbbeBrblZDOy6PnPFp33-WZ7lcaVeR6uNGqphQxCYv8pbti5Q9QYcc6IzYpvzsgDCbIVhuzQ9Px2-l6qVg6S-i-cYwQfxBYnVSyVmryuGSkIha2AezYJk2~0k7-byeJ0q57Re~aZy6boIDa2qtaOyi-RDbCWAoIIfOycwkAvqf5nG8KOVwGzvFEjYuExyP3f9ZlAAAA";
			int		boot_port 	= 52896;
			String	boot_nid	= "6d3dh2bwrafjdx4ba46zb6jvbnnt2g3r";
				
			NodeInfo ninf = new NodeInfo( new NID( Base32.decode( boot_nid )), new Destination( boot_dest ), boot_port );
				
			dht.setBootstrapNode( ninf );
		}
		
		logger.log( "MyDest: " + session.getMyDestination().toBase64());
		logger.log( "        " + Base32.encode( session.getMyDestination().calculateHash().getData()).toUpperCase() + ".b32.i2p"  + ", existing=" + use_existing_key );
		logger.log( "MyNID:  " + Base32.encode( dht.getNID().getData()) + ", existing=" + use_existing_nid );
	}
	
	public Destination
	lookupDestination(
		byte[]		hash )
	
		throws Exception
	{
		return( session.lookupDest( new Hash( hash ), 30*1000 ));
	}
	
	protected DHT
	getDHT()
	{
		return( dht );
	}
	
	protected void
	destroy()
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed	= true;
			
			try{
				if ( router != null ){
		
					router.shutdown( Router.EXIT_HARD );
					
					router = null;
				}
				
				if ( dht != null ){
					
					dht.stop();
					
					dht = null;
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	protected void
	logInfo()
	{
		DHT		dht		= this.dht;
		Router router 	= this.router;
		
		if ( dht == null ){
			
			logger.log( "DHT is inactive" );
			
		}else{
			
			logger.log( dht.renderStatusHTML());
			logger.log( dht.getStats());
		}
		
		if ( router == null ){
			
			logger.log( "Router is inactive" );
			
		}else{
			
			RouterContext router_ctx = router.getContext();
			
			logger.log( "Known routers=" + router_ctx.netDb().getKnownRouters() + ", lease-sets=" + router_ctx.netDb().getKnownLeaseSets());
			
			TunnelManagerFacade tunnel_manager = router_ctx.tunnelManager();
			
			int	exploratory_tunnels		= tunnel_manager.getFreeTunnelCount() + tunnel_manager.getOutboundTunnelCount();
			int	client_tunnels			= tunnel_manager.getInboundClientTunnelCount()  + tunnel_manager.getOutboundClientTunnelCount();
			int participating_tunnels	= tunnel_manager.getParticipatingCount();
			
			logger.log( "Tunnels: exploratory=" + exploratory_tunnels + ", client=" + client_tunnels + ", participating=" + participating_tunnels ); 
	
			logger.log( "Throttle: msg_delay=" + router_ctx.throttle().getMessageDelay() + ", tunnel_lag=" + router_ctx.throttle().getTunnelLag() + ", tunnel_stat=" +  router_ctx.throttle().getTunnelStatus());
			
			FIFOBandwidthLimiter bwl = router_ctx.bandwidthLimiter();
			
			long recv_rate = (long)bwl.getReceiveBps();
			long send_rate = (long)bwl.getSendBps();
			
			//RateStat sendRate = router_ctx.statManager().getRate("bw.sendRate");
		    //RateStat recvRate = router_ctx.statManager().getRate("bw.recvRate"); 
			//System.out.println( "Rates: send=" + sendRate.getRate(60*1000).getAverageValue() + ", recv=" + recvRate.getRate(60*1000).getAverageValue());
		    
			if ( FULL_STATS ){
			
				logger.log( "Lease repubs=" + router_ctx.statManager().getRate("netDb.republishLeaseSetCount" ).getLifetimeEventCount());
			}
			
			logger.log( 
				"Rates: send=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(send_rate) +
				", recv=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(recv_rate) +
				"; Limits: send=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(bwl.getOutboundKBytesPerSecond()*1024) + 
				", recv=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(bwl.getInboundKBytesPerSecond()*1024));
		}
	}
}
