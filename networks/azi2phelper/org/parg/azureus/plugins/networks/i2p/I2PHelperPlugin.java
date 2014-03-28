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

import java.io.File;





import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.text.SimpleDateFormat;
import java.util.Locale;

import net.i2p.data.Base32;
import net.i2p.data.Base64;
import net.i2p.data.Destination;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
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
import org.parg.azureus.plugins.networks.i2p.dht.DHT;
import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;

import com.aelitis.azureus.plugins.upnp.UPnPMapping;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;


public class 
I2PHelperPlugin 
	implements UnloadablePlugin, I2pHelperLogger
{	
	/*
	 * Router: commented out System.setProperties for timezone, http agent etc in static initialiser
	 * RoutingKeyGenerator: Fixed up SimpleDateFormat
	 *    	private final static SimpleDateFormat _fmt = new SimpleDateFormat(FORMAT, Locale.UK);
    		static{
    			_fmt.setCalendar( _cal );	 // PARG
    		}
	 * KRPC: Added code to persist DHT periodically in Cleaner
	 *  	System.out.println( "Persisting DHT" );
            boolean saveAll = _context.clock().now() - _started < 20*60*1000;
            PersistDHT.saveDHT(_knownNodes, saveAll, _dhtFile);
       DHT/KRPC: Added constructor/accessor to support persistent NID
	*/
	
	private PluginInterface			plugin_interface;
	private PluginConfig			plugin_config;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;

	private boolean					plugin_enabled;
	
	private I2PHelperRouter			router;
	private I2PHelperTracker		tracker;
	
	private File			lock_file;
	private InputStream		lock_stream;
	
	private boolean	unloaded;
	
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
			
			final LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
			
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
			
			final BooleanParameter enable_param = config_model.addBooleanParameter2( "enable", "azi2phelper.enable", true );

			final IntParameter internal_port = config_model.addIntParameter2( "azi2phelper.internal.port", "azi2phelper.internal.port", 0 );
			final IntParameter external_port = config_model.addIntParameter2( "azi2phelper.external.port", "azi2phelper.external.port", 0 );
						
			int	int_port = internal_port.getValue();
			
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
			
			int	ext_port = external_port.getValue();
			
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

			final InfoParameter info_param = config_model.addInfoParameter2( "azi2phelper.port.info", int_port + "/" + ext_port );

			final BooleanParameter use_upnp = config_model.addBooleanParameter2( "azi2phelper.upnp.enable", "azi2phelper.upnp.enable", true );

			final BooleanParameter ext_i2p_param 		= config_model.addBooleanParameter2( "azi2phelper.use.ext", "azi2phelper.use.ext", false );
			
			final IntParameter ext_i2p_port_param 		= config_model.addIntParameter2( "azi2phelper.use.ext.port", "azi2phelper.use.ext.port", 7654 ); 

			
			
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
			
			log( "Internal port=" + int_port +", external=" + ext_port );
						
			ParameterListener enabler_listener =
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param )
						{
							plugin_enabled 			= enable_param.getValue();

							boolean use_ext_i2p  	= ext_i2p_param.getValue();
							
							internal_port.setEnabled( plugin_enabled && !use_ext_i2p );
							external_port.setEnabled( plugin_enabled && !use_ext_i2p);
							info_param.setEnabled( plugin_enabled  && !use_ext_i2p);
							use_upnp.setEnabled( plugin_enabled  && !use_ext_i2p );
							
							ext_i2p_port_param.setEnabled( plugin_enabled && use_ext_i2p );
							
							command_text_param.setEnabled( plugin_enabled );
							command_exec_param.setEnabled( plugin_enabled );
						}
					};
			
			enable_param.addListener( enabler_listener );
			ext_i2p_param.addListener( enabler_listener );
			
			enabler_listener.parameterChanged( null );
					
			if ( plugin_enabled ){
				
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
							unload();
						}
					});
					
				new AEThread2("I2P: RouterInit")
				{
					public void
					run()
					{
						router = new I2PHelperRouter( false, I2PHelperPlugin.this );
						
						try{
							if ( ext_i2p_param.getValue()){
								
								router.initialise( plugin_dir, ext_i2p_port_param.getValue());
										
							}else{
							
								router.initialise( plugin_dir, f_int_port, f_ext_port );
							}
							
							tracker = new I2PHelperTracker( router.getDHT());
							
							while( true ){
								
								router.logInfo();
								
								Thread.sleep(60*1000);
							}
						}catch( Throwable e ){
							
							e.printStackTrace();
							
							router = null;
						}
					}
				}.start();
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
	
	private static void
	executeCommand(
		String				cmd_str,
		I2PHelperRouter		router,
		I2PHelperTracker	tracker,
		I2pHelperLogger		log )
		
		throws Exception
	{
		cmd_str = cmd_str.trim();
		
		String[] bits = cmd_str.split( " " );

		if ( bits.length == 0 ){
			
			log.log( "No command" );
			
		}else if ( router == null ){
			
			log.log( "Router is not initialised" );
			
		}else{
		
			DHT dht = router.getDHT();
			
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
				}else if ( cmd.equals( "announce" )){
					
					if ( bits.length != 2 ){
						
						throw( new Exception( "usage: announce <base16_infohash>"));
					}
					
					byte[] hash = decodeHash( bits[1] );
				
					tracker.announce( hash );
					
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
					
				}else{
			
					log.log( "Usage: print|info..." );
				}
			}
		}	
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
	
	public void
	unload()
	{
		synchronized( this ){
			
			unloaded = true;
		}
		
		if ( router != null ){
			
			router.destroy();
			
			router = null;
		}
		
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
		
		boolean bootstrap = args.length > 1 && args[1].equals( "true" );
		
		if ( bootstrap ){
			
			System.out.println( "Boostrap Node" );
		}
		
		I2pHelperLogger logger = 
			new I2pHelperLogger() 
			{
				public void 
				log(
					String str ) 
				{
					System.out.println( str );
				}
			};
			
		try{
			I2PHelperRouter router = new I2PHelperRouter( bootstrap, logger );
			
				// 19817 must be used for bootstrap node
			
			router.initialise( config_dir, 17654, bootstrap?19817:23014 );
			//router.initialise( config_dir, 29903 ); // 7654 );
			//router.initialise( config_dir, 7654 );

			I2PHelperTracker tracker = new I2PHelperTracker( router.getDHT());
			
			I2PHelperConsole console = new I2PHelperConsole();
			
			while( true ){
				
				String line = console.readLine( ">" );
				
				if ( line == null ){
					
					break;
				}
				
				try{
					line = line.trim();
					
					if ( line.equals( "quit" )){
						
						break;
						
					}else{
						
						executeCommand(line, router, tracker, logger);
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
