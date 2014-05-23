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

import java.io.ByteArrayOutputStream;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import net.i2p.CoreVersion;
import net.i2p.I2PAppContext;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
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

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SystemTime;
import org.parg.azureus.plugins.networks.i2p.dht.*;
import org.parg.azureus.plugins.networks.i2p.vuzedht.DHTI2P;


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

	private final File						config_dir;

	private final Map<String,Object>		router_properties;
	
	
	private final boolean					is_bootstrap_node;
	private final boolean					is_vuze_dht;
	private final I2PHelperAdapter			adapter;
	
	private boolean		is_external_router;
	private String		i2p_host;
	private int			i2p_port;
	
	
	private volatile Router 			router;
	
	private Properties						sm_properties = new Properties();

		// DHT stuff
	
	private volatile I2PSession 			dht_session;
	private volatile I2PSocketManager 		dht_socket_manager;
	private volatile I2PServerSocket		dht_server_socket;
	
	private I2PHelperDHT		dht;
	
	private Map<String,ServerInstance>		servers = new HashMap<String, ServerInstance>();
	
	private volatile boolean	destroyed;
	
	protected
	I2PHelperRouter(
		File					_config_dir,
		Map<String,Object>		_properties,
		boolean					_is_bootstrap_node,
		boolean					_is_vuze_dht,
		I2PHelperAdapter		_adapter )
	{
		config_dir			= _config_dir;
		router_properties	= _properties;
		is_bootstrap_node	= _is_bootstrap_node;		// could be props one day
		is_vuze_dht			= _is_vuze_dht;
		adapter				= _adapter;
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
		
		normalizeProperties( props );
		
		router.saveConfig( props, null );
		
		router.getContext().bandwidthLimiter().reinitialize();
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
			
			Properties router_props = readProperties( router_config );
			
				// router config
			
			router_props.put( "i2cp.port", i2p_internal_port );
			router_props.put( "i2np.upnp.enable", false );
			router_props.put( "i2p.streaming.answerPings", true );		// testing
			
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
									
			normalizeProperties( router_props );
			
			writeProperties( router_config, router_props );
			
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
				
			router.setKillVMOnEnd( false );	// has to be done again after run phase as set true in that code :(

			RouterContext router_ctx = router.getContext();
			
			adapter.log( "Waiting for router startup" );;
						
			while( true ){
				
				if ( destroyed ){
					
					throw( new Exception( "Router has been shutdown" ));
				}
				
				try{
					Socket s = new Socket( i2p_internal_host, i2p_internal_port );
				
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
            
			adapter.log( "Router startup complete: version=" + CoreVersion.VERSION );

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
		
			adapter.log( "Waiting for router startup" );;
						
			while( true ){
				
				if ( destroyed ){
					
					throw( new Exception( "Router has been shutdown" ));
				}
				
				try{
					Socket s = new Socket( i2p_separate_host, i2p_separate_port );
				
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
			
			adapter.log( "Router startup complete" );
			            
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
	initialiseDHT()
		
		throws Exception
	{
		try{
			adapter.log( "Initializing DHT..." );
			
			File dht_config 	= new File( config_dir,  "dht.config" );
			File dest_key_file 	= new File( config_dir,  "dest_key.dat" );
	

	        
	        boolean	use_existing_key = dest_key_file.exists();
	        	
	        I2PSocketManager	sm = null;
	        
			if ( use_existing_key ){
	         	
	    		InputStream is = new FileInputStream( dest_key_file );
	    	
	    		try{
	    			sm = I2PSocketManagerFactory.createManager( is, i2p_host, i2p_port, sm_properties );
	    	
	    		}finally{
	    		
	    			is.close();
	    		}
	        }else{
	        	
	        	sm = I2PSocketManagerFactory.createManager( i2p_host, i2p_port, sm_properties );
	        }
			
			if ( sm == null ){
				
				throw( new Exception ( "Failed to create socket manager" ));
			}
			
			adapter.log( "Waiting for socket manager startup" );
			
			while( true ){
				
				if ( destroyed ){
					
					sm.destroySocketManager();
					
					throw( new Exception( "Router destroyed" ));
				}
				
				dht_session = sm.getSession();
				
				if ( dht_session != null ){
					
					break;
				}
				
				Thread.sleep(250);
			}
			
			adapter.log( "Socket manager startup complete" );
			
			Destination my_dest = dht_session.getMyDestination();
			
			String	full_dest 	= my_dest.toBase64() + ".i2p";
			String	b32_dest	= Base32.encode( my_dest.calculateHash().getData()) + ".b32.i2p";
			
				// some older trackers require ip to be explicitly set to the full destination name :(
			
			/*
			 * don't do this anymore, the socks proxy adds this in when required
			 * 
		    String explicit_ips = COConfigurationManager.getStringParameter( "Override Ip", "" ).trim();
	
		    if ( !explicit_ips.contains( full_dest )){
		    	
		    	if ( explicit_ips.length() == 0 ){
		    		
		    		explicit_ips = full_dest;
		    		
		    	}else{
		    
		    		String[]	bits = explicit_ips.split( ";" );
		    		
		    		explicit_ips = "";
		    		
		    		for ( String bit: bits ){
		    			
		    			bit = bit.trim();
		    			
		    			if ( bit.length() > 0 ){
		    				
		    				if ( !bit.endsWith( ".i2p" )){
		    						    			
		    					explicit_ips += ( explicit_ips.length()==0?"":"; ") + bit;
		    				}
		    			}
		    		}
		    		
		    		explicit_ips += ( explicit_ips.length()==0?"":"; ") + full_dest;
		    	}
		    	
		    	COConfigurationManager.setParameter( "Override Ip", explicit_ips );
		    }
		    */
			
			dht_socket_manager	= sm;
					
			dht_server_socket = dht_socket_manager.getServerSocket();
			
			new AEThread2( "I2P:accepter" )
			{
				public void
				run()
				{
					while( !destroyed ){
						
						try{
							I2PSocket socket = dht_server_socket.accept();
							
							if ( socket == null ){
								
								if ( destroyed ){
									
									break;
									
								}else{
									
									Thread.sleep(500);
								}
							}else{
								try{
								
									adapter.incomingConnection( socket );
								
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
			
			Properties dht_props;
			
			if ( !use_existing_key ){
				
				new PrivateKeyFile( dest_key_file , dht_session ).write();
				
				dht_props = new Properties();
				
			}else{
			
				dht_props = readProperties( dht_config );
			}
			
			String dht_port_str = dht_props.getProperty( "port" );
			String dht_NID_str 	= dht_props.getProperty( "nid" );
				
			boolean	use_existing_nid = dht_port_str != null && dht_NID_str != null;
				
			
			String 	boot_dest 	= "N0e4jfsxy~NYzyr-0bY1nwpnhTza8fn1wWr6IHHOmaIEnbEvgltJvyJn8LWvwlu589mUPhQXQb9BtMrkEan8RZSL4Vo2iFgMCxjTOnfA2dW1~JpL0ddGM28OQITya-1YDgNZFmyX0Me-~RjJjTg31YNozDoosIQ-Uvz2s5aUrzI0gt0r3M4PFUThb0eefd51Yb-eEQMpBb-Hd~EU07yw46ljy2uP4tiEPlWt0l0YR8nbeH0Eg6i3fCoSVgWpSeRjJ9vJeHvwGymO2rPHCSCPgIVwwyqNYpgkqGWnn9Qg97Wc-zrTBiRJp0Dn4lcYvkbbeBrblZDOy6PnPFp33-WZ7lcaVeR6uNGqphQxCYv8pbti5Q9QYcc6IzYpvzsgDCbIVhuzQ9Px2-l6qVg6S-i-cYwQfxBYnVSyVmryuGSkIha2AezYJk2~0k7-byeJ0q57Re~aZy6boIDa2qtaOyi-RDbCWAoIIfOycwkAvqf5nG8KOVwGzvFEjYuExyP3f9ZlAAAA";
			int		boot_port 	= 52896;
			String	boot_nid	= "6d3dh2bwrafjdx4ba46zb6jvbnnt2g3r";
				
			NodeInfo boot_ninf = new NodeInfo( new NID( Base32.decode( boot_nid )), new Destination( boot_dest ), boot_port );
	
			if ( destroyed ){
				
				throw( new Exception( "Router destroyed" ));
			}
			
			I2PAppContext ctx = I2PAppContext.getGlobalContext();
	
			if ( !is_vuze_dht ){
				
				DHTNodes.setBootstrap( is_bootstrap_node ); 
	
				KRPC	snark_dht;
				
				if ( use_existing_nid ){
					
					int	dht_port = Integer.parseInt( dht_port_str );
					NID	dht_nid	= new NID( Base32.decode( dht_NID_str ));
			
					dht = snark_dht = new KRPC( ctx, "i2pvuze", dht_session, dht_port, dht_nid, adapter );
					
				}else{	
					
		    		dht = snark_dht = new KRPC( ctx, "i2pvuze", dht_session, adapter );
		    	}
							
				if ( !use_existing_nid ){
					
					dht_props.setProperty( "dest", my_dest.toBase64());
					dht_props.setProperty( "port", String.valueOf( snark_dht.getPort()));
					dht_props.setProperty( "nid", Base32.encode( snark_dht.getNID().getData()));
					
					writeProperties( dht_config, dht_props );
				}
				
				if ( !is_bootstrap_node ){
										
					snark_dht.setBootstrapNode( boot_ninf );
				}
				
				adapter.log( "MyDest: " + full_dest);
				adapter.log( "        " + b32_dest  + ", existing=" + use_existing_key );
				adapter.log( "MyNID:  " + Base32.encode( snark_dht.getNID().getData()) + ", existing=" + use_existing_nid );
				
			}else{
				
				int		dht_port;
				NID		dht_nid;
				
				if ( use_existing_nid ){
					
					dht_port = Integer.parseInt( dht_port_str );
					dht_nid	= new NID( Base32.decode( dht_NID_str ));
	
				}else{
					
					dht_port = 10000 + RandomUtils.nextInt( 65535 - 10000 );
					dht_nid = NodeInfo.generateNID(my_dest.calculateHash(), dht_port, ctx.random());
					
					dht_props.setProperty( "dest", my_dest.toBase64());
					dht_props.setProperty( "port", String.valueOf( dht_port ));
					dht_props.setProperty( "nid", Base32.encode( dht_nid.getData()));
					
					writeProperties( dht_config, dht_props );
				}
				
				NodeInfo my_node_info = new NodeInfo( dht_nid, my_dest, dht_port );
	
				adapter.log( "MyDest: " + full_dest );
				adapter.log( "        " + b32_dest  + ", existing=" + use_existing_key );
				adapter.log( "MyNID:  " + Base32.encode( dht_nid.getData()) + ", existing=" + use_existing_nid );
	
				dht = new DHTI2P( config_dir, dht_session, my_node_info, is_bootstrap_node?null:boot_ninf, adapter );						
			}
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
	
	
	public Destination
	lookupDestination(
		byte[]		hash )
	
		throws Exception
	{
			// just used for testing, leave blocking
		
		return( dht_session.lookupDest( new Hash( hash ), 30*1000 ));
	}
	
	public I2PSocketManager
	getDHTSocketManager()
	{
		return( dht_socket_manager );
	}
	
	public I2PHelperDHT
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
				
				if ( dht_server_socket != null ){
					
					dht_server_socket.close();
					
					dht_server_socket = null;
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
		I2PHelperDHT	dht		= this.dht;
		Router 			router 	= this.router;
		
		if ( dht == null ){
			
			adapter.log( "DHT is inactive" );
			
		}else{
			
			String html = dht.renderStatusHTML();
			
			if ( html.length() > 0 ){
				adapter.log( html );
			}
			
			adapter.log( dht.getStats());
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
		I2PHelperDHT	dht		= this.dht;
		Router 			router 	= this.router;
		
		if ( router == null || dht == null ){
			
			if ( is_external_router ){
				
				if ( dht != null ){
					
					return( dht.getStatusString() + "\nExternal Router" );
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

		return( dht.getStatusString() + "\n" + router_str );
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
						
						if ( SystemTime.getMonotonousTime() - start > 3*60*1000 ){
							
							throw( new Exception( "Timeout creating socket manager" ));
						}
						
						Thread.sleep( 250 );
									
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
