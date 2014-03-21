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

import net.i2p.I2PAppContext;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.client.streaming.I2PSocketOptions;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKeyFile;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelManagerFacade;
import net.i2p.router.transport.FIFOBandwidthLimiter;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.parg.azureus.plugins.networks.i2p.dht.*;


public class 
I2PHelperRouter 
{
	private boolean		is_bootstrap_node;
	
	private Router 		router;
	private DHT			dht;
	
	private boolean		destroyed;
	
	protected
	I2PHelperRouter(
		boolean	bootstrap_node )
	{
		is_bootstrap_node	= bootstrap_node;
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
	
	protected void
	initialise(
		File		config_dir )
	
		throws Exception
	{	
		try{
			File dht_config 	= new File( config_dir,  "dht.config" );
			File dest_key_file 	= new File( config_dir,  "dest_key.dat" );
	
			String 	i2p_host 	= "127.0.0.1";
			int		i2p_port	= 17654;

			if ( !config_dir.isDirectory()){
				
				config_dir.mkdirs();
				
				if ( !config_dir.isDirectory()){
					
					throw( new Exception( "Failed to create config dir '" + config_dir +"'" ));
				}
			}
					
				// setting this prevents stdout/stderr from being hijacked
			
			System.setProperty( "wrapper.version", "dummy" );
						
			new File( config_dir, "router.ping" ).delete();
			
			File router_config = new File( config_dir, "router.config" );
			
			Properties router_props = readProperties( router_config );
			
				// router config
			
			router_props.put( "i2cp.port", i2p_port );
			router_props.put( "i2np.upnp.enable", false );
			router_props.put( "i2p.streaming.answerPings", true );		// testing
			
				// TODO: manage the external port + upnp where required
				// Note that TCP defaults to the same as UDP port
			
			if ( is_bootstrap_node ){
			
				router_props.put( "i2np.udp.port", 19817 );
				router_props.put( "i2np.udp.internalPort", 19817 );
			}
	
				// router bandwidth
				// TODO: review!
			
			router_props.put( "i2np.bandwidth.inboundBurstKBytes", 11000 );
			router_props.put( "i2np.bandwidth.inboundBurstKBytesPerSecond", 550 );
			router_props.put( "i2np.bandwidth.inboundKBytesPerSecond", 500 );
			router_props.put( "i2np.bandwidth.outboundBurstKBytes", 11000 );
			router_props.put( "i2np.bandwidth.outboundBurstKBytesPerSecond", 550 );
			router_props.put( "i2np.bandwidth.outboundKBytesPerSecond", 500 );
			
				// router pools
					
			router_props.put( "router.inboundPool.backupQuantity", 0 );
			router_props.put( "router.inboundPool.length", 2 );
			router_props.put( "router.inboundPool.lengthVariance", 0 );
			router_props.put( "router.inboundPool.quantity", 2 );
			router_props.put( "router.outboundPool.backupQuantity", 0 );
			router_props.put( "router.outboundPool.length", 2 );
			router_props.put( "router.outboundPool.lengthVariance", 0 );
			router_props.put( "router.outboundPool.quantity", 2 );
			router_props.put( "router.sharePercentage", 30 );
						
			normalizeProperties( router_props );
			
			writeProperties( router_config, router_props );
			
			router_props.setProperty( "router.configLocation", router_config.getAbsolutePath());
			router_props.setProperty( "i2p.dir.base" , config_dir.getAbsolutePath());
			router_props.setProperty( "i2p.dir.config" , config_dir.getAbsolutePath());

			router = new Router( router_props );
				
			router.setKillVMOnEnd( false );
			
			router.runRouter();
				
			router.setKillVMOnEnd( false );	// has to be done again after run phase as set true in that code :(

			RouterContext router_ctx = router.getContext();
			
			System.out.println( "Waiting for router startup" );;
						
			while( true ){
				
				try{
					Socket s = new Socket( i2p_host, 17654 );
				
					s.close();
				
					break;
					
				}catch( Throwable e ){
					
					try{
						Thread.sleep(250);
						
					}catch( Throwable f ){
						
					}
				}
			}
			
			System.out.println( "Router startup complete" );
			
			I2PAppContext ctx = I2PAppContext.getGlobalContext();

			ctx.logManager().setDefaultLimit( "WARN" );
			
			I2PSocketManager manager;
			
            Properties opts = new Properties();        
            opts.putAll( router_ctx.getProperties());
                        
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
            if (opts.getProperty("i2p.streaming.maxConnsPerMinute") == null)
                opts.setProperty("i2p.streaming.maxConnsPerMinute", "2");
            if (opts.getProperty("i2p.streaming.maxTotalConnsPerMinute") == null)
                opts.setProperty("i2p.streaming.maxTotalConnsPerMinute", "8");
            if (opts.getProperty("i2p.streaming.maxConnsPerHour") == null)
                opts.setProperty("i2p.streaming.maxConnsPerHour", "20");
            if (opts.getProperty("i2p.streaming.enforceProtocol") == null)
                opts.setProperty("i2p.streaming.enforceProtocol", "true");
            if (opts.getProperty("i2p.streaming.disableRejectLogging") == null)
                opts.setProperty("i2p.streaming.disableRejectLogging", "true");
            if (opts.getProperty("i2p.streaming.answerPings") == null)
                opts.setProperty("i2p.streaming.answerPings", "false");
            
            opts.setProperty( "inbound.length", "2" );
            opts.setProperty( "inbound.lengthVariance", "0" );
            opts.setProperty( "outbound.length", "2" ); 
            opts.setProperty( "outbound.lengthVariance", "0" ); 
            opts.setProperty( "inbound.quantity", "3" ); 
            opts.setProperty( "outbound.quantity", "3" );
            
			if ( !dest_key_file.exists() ){
               
            	manager = I2PSocketManagerFactory.createManager( i2p_host, i2p_port, opts );
            	
            }else{
            	
        		InputStream is = new FileInputStream( dest_key_file );
        	
        		try{
        			manager = I2PSocketManagerFactory.createManager( is, i2p_host, i2p_port, opts );
        	
        		}finally{
        		
        			is.close();
        		}
            }
			
			System.out.println( "Waiting for socket manager startup" );
			
			I2PSession session;
			
			while( true ){
				
				session = manager.getSession();
				
				if ( session != null ){
					
					break;
				}
				
				Thread.sleep(250);
			}
			
			System.out.println( "Socket manager startup complete" );
			
			if ( !dest_key_file.exists()){
				
				new PrivateKeyFile( dest_key_file , session ).write();
			}
			
			Properties dht_props = readProperties( dht_config );

			String dht_port_str = dht_props.getProperty( "port" );
			String dht_NID_str 	= dht_props.getProperty( "nid" );
						
			if ( dht_port_str != null && dht_NID_str != null ){
				
				int	dht_port = Integer.parseInt( dht_port_str );
				NID	dht_nid	= new NID( Base32.decode( dht_NID_str ));
		
				dht = new KRPC( ctx, "i2pvuze", session, dht_port, dht_nid );
				
			}else{	
				
        		dht = new KRPC( ctx, "i2pvuze", session );
        	}
						
			if ( dht_props.isEmpty()){
				
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
					
				dht.heardAbout( ninf );
			}
			
			System.out.println( "MyDest: " + session.getMyDestination().toBase64());
			System.out.println( "MyDest: " + Base32.encode( session.getMyDestination().calculateHash().getData()).toUpperCase() + ".b32.i2p" );
			System.out.println( "MyNID:  " + Base32.encode( dht.getNID().getData()));
						
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
			
			System.out.println( "DHT is inactive" );
			
		}else{
			
			System.out.println( dht.renderStatusHTML());
		}
		
		if ( router == null ){
			
			System.out.println( "Router is inactive" );
			
		}else{
			
			RouterContext router_ctx = router.getContext();
			
			System.out.println( "Known routers=" + router_ctx.netDb().getKnownRouters() + ", lease-sets=" + router_ctx.netDb().getKnownLeaseSets());
			
			TunnelManagerFacade tunnel_manager = router_ctx.tunnelManager();
			
			int	exploratory_tunnels		= tunnel_manager.getFreeTunnelCount() + tunnel_manager.getOutboundTunnelCount();
			int	client_tunnels			= tunnel_manager.getInboundClientTunnelCount()  + tunnel_manager.getOutboundClientTunnelCount();
			int participating_tunnels	= tunnel_manager.getParticipatingCount();
			
			System.out.println( "Tunnels: exploratory=" + exploratory_tunnels + ", client=" + client_tunnels + ", participating=" + participating_tunnels ); 
	
			System.out.println( "Throttle: msg_delay=" + router_ctx.throttle().getMessageDelay() + ", tunnel_lag=" + router_ctx.throttle().getTunnelLag() + ", tunnel_stat=" +  router_ctx.throttle().getTunnelStatus());
			
			FIFOBandwidthLimiter bwl = router_ctx.bandwidthLimiter();
			
			long recv_rate = (long)bwl.getReceiveBps();
			long send_rate = (long)bwl.getSendBps();
			
			//RateStat sendRate = router_ctx.statManager().getRate("bw.sendRate");
		    //RateStat recvRate = router_ctx.statManager().getRate("bw.recvRate"); 
			//System.out.println( "Rates: send=" + sendRate.getRate(60*1000).getAverageValue() + ", recv=" + recvRate.getRate(60*1000).getAverageValue());
		    
			System.out.println( 
				"Rates: send=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(send_rate) +
				", recv=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(recv_rate) +
				"; Limits: send=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(bwl.getOutboundKBytesPerSecond()*1024) + 
				", recv=" + DisplayFormatters.formatByteCountToKiBEtcPerSec(bwl.getInboundKBytesPerSecond()*1024));
		}
	}
}