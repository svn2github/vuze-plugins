/*
 * Created on Dec 15, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.networks.tor;

import java.io.*;
import java.util.*;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.ServerSocketChannel;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import com.aelitis.azureus.core.util.GeneralUtils;

public class 
TorPlugin
	implements UnloadablePlugin
{
	private PluginInterface			plugin_interface;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	
	private AESemaphore		init_sem = new AESemaphore( "TP:init" );
	
	private static final int	SOCKS_PORT_DEFAULT		= 29101;
	private static final int	CONTROL_PORT_DEFAULT	= 29151;

	private File	plugin_dir;
	private File	config_file;
	private File	data_dir;
	
	private boolean	plugin_enabled;
	private boolean	external_tor;
	private boolean	start_on_demand;
	private boolean	stop_on_idle;
	private boolean	prompt_on_use;
	private boolean	prompt_skip_vuze;
	
	private int		internal_control_port;
	private int		internal_socks_port;
	
	private int		active_socks_port;
	
	private long	MIN_RECONNECT_TIME		= 60*1000;
	private long	MAX_CONNECT_WAIT_TIME	= 2*60*1000;
	private long	STOP_ON_IDLE_TIME		= 10*60*1000;

	
	private ControlConnection		current_connection;
	private AESemaphore 			connection_sem;
	private long					last_connect_time;
	
	private long					last_use_time;
	
	private volatile boolean		unloaded;
	
	
	public void
	initialize(
		PluginInterface		pi )
	{
		try{
			plugin_interface	= pi;
			
			LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
			
			log	= plugin_interface.getLogger().getChannel( "TorHelper");
			
			final UIManager	ui_manager = plugin_interface.getUIManager();
			
			config_model = ui_manager.createBasicPluginConfigModel( "plugins", "aztorplugin.name" );

			config_model.addLabelParameter2( "aztorplugin.info1" );
			config_model.addLabelParameter2( "aztorplugin.info2" );
			
			final BooleanParameter enable_param = config_model.addBooleanParameter2( "enable", "aztorplugin.enable", true );

			final BooleanParameter start_on_demand_param 	= config_model.addBooleanParameter2( "start_on_demand", "aztorplugin.start_on_demand", true );
			final BooleanParameter stop_on_idle_param	 	= config_model.addBooleanParameter2( "stop_on_idle", "aztorplugin.stop_on_idle", true );
			final BooleanParameter prompt_on_use_param 		= config_model.addBooleanParameter2( "prompt_on_use", "aztorplugin.prompt_on_use", true );
			final BooleanParameter prompt_skip_vuze_param 	= config_model.addBooleanParameter2( "prompt_skip_vuze", "aztorplugin.prompt_skip_vuze", true );

			config_model.createGroup( "aztorplugin.prompt_options", new Parameter[]{ prompt_skip_vuze_param });
			
			final IntParameter control_port_param = config_model.addIntParameter2( "control_port", "aztorplugin.control_port", 0 ); 
			
			if ( control_port_param.getValue() == 0 ){
				
				control_port_param.setValue( allocatePort( CONTROL_PORT_DEFAULT ));
			}
			
			internal_control_port = control_port_param.getValue();
			
			final IntParameter socks_port_param = config_model.addIntParameter2( "socks_port", "aztorplugin.socks_port", 0 ); 
			
			if ( socks_port_param.getValue() == 0 ){
				
				socks_port_param.setValue( allocatePort( SOCKS_PORT_DEFAULT ));
			}
			
			internal_socks_port = socks_port_param.getValue();
			
			final BooleanParameter ext_tor_param = config_model.addBooleanParameter2( "ext_tor", "aztorplugin.use_external", false );
			
			final IntParameter ext_socks_port_param = config_model.addIntParameter2( "ext_socks_port", "aztorplugin.ext_socks_port", 9050 ); 

			ext_socks_port_param.addListener(
				new ParameterListener()
				{	
					public void 
					parameterChanged(
						Parameter param) 
					{
						active_socks_port = ext_socks_port_param.getValue();
					}
				});
			
			final StringParameter test_url_param	= config_model.addStringParameter2( "test_url", "aztorplugin.test_url", "http://www.vuze.com/" );
			
			final ActionParameter test_param = config_model.addActionParameter2( "aztorplugin.test_text", "aztorplugin.test_button" );
			
			test_param.addListener(
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter param ) 
					{
						test_param.setEnabled( false );
						
						new AEThread2( "tester" )
						{
							public void
							run()
							{
								List<String>	lines = new ArrayList<String>();
								
								lines.add( "Testing connection via SOCKS proxy on port " + active_socks_port );
								
								try{
									Proxy proxy = new Proxy( Proxy.Type.SOCKS, new InetSocketAddress( "127.0.0.1", active_socks_port ));
									
									HttpURLConnection con = (HttpURLConnection)new URL( test_url_param.getValue()).openConnection( proxy );
									
									con.setConnectTimeout( 30*1000 );
									con.setReadTimeout( 30*1000 );
								
									con.getResponseCode();
									
									InputStream is = con.getInputStream();
									
									try{
										lines.add( "Connection succeeded, response=" + con.getResponseCode() + "/" + con.getResponseMessage());
										
										try{
											String text = FileUtil.readInputStreamAsString( is, 1024 );
											
											lines.add( "Start of response: " );
											lines.add( text );
											
										}catch( Throwable e ){
											
										}
									}finally{
										
										try{										
											is.close();
											
										}catch( Throwable e ){
										}
									}
									
								}catch( Throwable e ){
									
									lines.add( "Test failed: " + Debug.getNestedExceptionMessage( e ));
	
								}finally{
									
									test_param.setEnabled( true );
								}
								
								String text = "";
								
								for ( String str: lines ){
									text += str + "\r\n";
								}
								
								ui_manager.showTextMessage(
										"aztorplugin.test.msg.title",
										null,
										text );
																	

							}
						}.start();
					}
				});
			
			ParameterListener enabler_listener =
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter param )
					{
						plugin_enabled 	= enable_param.getValue();
						external_tor	= ext_tor_param.getValue();
						start_on_demand	= start_on_demand_param.getValue();
						stop_on_idle	= stop_on_idle_param.getValue();
						prompt_on_use	= prompt_on_use_param.getValue();
						
						if ( plugin_enabled ){
							
							if ( external_tor ){
								
								active_socks_port = ext_socks_port_param.getValue();
								
							}else{
								
								active_socks_port = internal_socks_port;
							}
						}else{
							
							active_socks_port = 0;
						}
						
						start_on_demand_param.setEnabled( plugin_enabled && !external_tor );
						stop_on_idle_param.setEnabled( plugin_enabled && !external_tor && start_on_demand );
						
						prompt_on_use_param.setEnabled( plugin_enabled );
						prompt_skip_vuze_param.setEnabled( plugin_enabled && prompt_on_use );
						
						control_port_param.setEnabled( plugin_enabled && !external_tor );
						socks_port_param.setEnabled( plugin_enabled && !external_tor );
						ext_tor_param.setEnabled( plugin_enabled );
						ext_socks_port_param.setEnabled( plugin_enabled && external_tor );
						
						test_url_param.setEnabled( plugin_enabled );
						test_param.setEnabled( plugin_enabled );
					}
				};
				
			enable_param.addListener( enabler_listener );
			start_on_demand_param.addListener( enabler_listener );
			stop_on_idle_param.addListener( enabler_listener );
			prompt_on_use_param.addListener( enabler_listener );

			ext_tor_param.addListener( enabler_listener );
			
			enabler_listener.parameterChanged( null );
			
			view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "aztorplugin.name" ));

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
			
			config_file = pi.getPluginconfig().getPluginUserFile( "config.txt" );
			
			plugin_dir 	= config_file.getParentFile();
			
			data_dir 	= new File( plugin_dir, "data" );
			
				// see if server already running, unlikely due to the way we arrange for it to die if we do but you never know
			
			ControlConnection control = new ControlConnection( data_dir, internal_control_port, internal_socks_port );
		
			if ( control.connect()){
			
				log( "Found an existing server instance - closing it" );
			
				control.shutdown( true );
			}
		
			boolean	write_config = false;
			
			List<String>	required_config_lines = new ArrayList<String>();
			
			required_config_lines.add( "SocksPort 127.0.0.1:" + internal_socks_port );
			required_config_lines.add( "ControlPort " + internal_control_port );
			required_config_lines.add( "CookieAuthentication 1" );
			required_config_lines.add( "DataDirectory ." + File.separator + data_dir.getName());
			
			if ( config_file.exists()){
				
				LineNumberReader lnr = null;
				
				try{
					lnr = new LineNumberReader( new InputStreamReader( new FileInputStream( config_file )));
					
					Set<String>	keys = new HashSet<String>();
					
					for ( String str: required_config_lines ){
						
						str = str.substring( 0, str.indexOf(' ' ));
						
						keys.add( str );
					}
					
					Set<String>	missing_lines = new LinkedHashSet<String>( required_config_lines );
					
					List<String> config_lines = new ArrayList<String>();
					
					while( true ){
						
						String line = lnr.readLine();
						
						if ( line == null ){
							
							break;
						}
						
						line = line.trim();
						
						boolean	ok = true;
						
						if ( !missing_lines.remove( line )){
							
							int	pos = line.indexOf( ' ' );
							
							if ( pos > 0 ){
								
								String l_key = line.substring( 0, pos );
								
								if ( keys.contains( l_key )){
									
									ok = false;
								}
							}
						}
						
						if ( ok ){
						
							config_lines.add( line );
						}
					}
					
					if ( missing_lines.size() > 0 ){
						
						config_lines.addAll( missing_lines );
						
						required_config_lines = config_lines;
						
						write_config = true;
					}
				}catch( Throwable e ){
					
					write_config = true;
					
				}finally{
					
					try{
						lnr.close();
						
					}catch( Throwable e ){
					}
				}
			}else{
				
				write_config = true;
			}
			
			if ( write_config ){
				
				try{
						// appears that the local file system encoding needs to be used
					
					PrintWriter pw = new PrintWriter( new OutputStreamWriter( new FileOutputStream( config_file )));
					
					for ( String line: required_config_lines ){
						
						pw.println( line );
					}
					
					pw.close();
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}		
			
			pi.addListener(
				new PluginAdapter()
				{
					public void
					initializationComplete()
					{
						init_sem.releaseForever();
						
						if ( plugin_enabled ){
							
							init();
						}
					}
					
					public void
					closedownInitiated()
					{
						init_sem.releaseForever();
						
						synchronized( TorPlugin.this ){
							
							unloaded = true;
							
							if ( current_connection != null ){
								
								current_connection.shutdown( false );
								
								current_connection = null;
							}
						}	
					}
				});
		}catch( Throwable e ){
			
			synchronized( TorPlugin.this ){
				
				unloaded = true;
				
				init_sem.releaseForever();
			}
			
			Debug.out( e );
		}
	}
	
	private int
	allocatePort(
		int		def )
	{
		for ( int i=0;i<32;i++){
			
			int port = 20000 + RandomUtils.nextInt( 20000 );
			
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
	
	private void
	init()
	{
			// see if we should connect at start of day
		
		if ( plugin_enabled && !( unloaded || external_tor || start_on_demand )){
			
			asyncGetConnection();
		}
		
		SimpleTimer.addPeriodicEvent(
			"TP:checker",
			30*1000,
			new TimerEventPerformer()
			{	
				public void 
				perform(
					TimerEvent event ) 
				{
					boolean	should_be_disconnected 	= true;
					boolean	should_be_connected 	= false;
					
					synchronized( TorPlugin.this ){
						
						if ( plugin_enabled && !( unloaded || external_tor )){
							
							if ( start_on_demand ){
								
								if ( stop_on_idle ){
									
									should_be_disconnected = SystemTime.getMonotonousTime() - last_use_time > STOP_ON_IDLE_TIME;
										
								}else{
										// leave it in whatever state it is in
									
									should_be_disconnected = false; 
								}
							}else{
									// should always be running
							
								should_be_disconnected = false;
								
								if ( !isConnected()){
								
									should_be_connected = true;
								}
							}
						}
					}
					
					if ( should_be_disconnected ){
						
						closeConnection();
						
					}else if ( should_be_connected ){
						
						asyncGetConnection();
					}
				}
			});
	}
	
	
	private boolean
	startServer()
	{
		log( "Starting server" );
		
		File exe_file = new File( plugin_dir, Constants.isWindows?"AzureusTor.exe":"AzureusTor" );
		
		int	pid = getPID();
		
		try{
			List<String>	cmd_list = new ArrayList<String>();
			
			cmd_list.add( exe_file.getAbsolutePath());
			cmd_list.add( "-f" );
			cmd_list.add( config_file.getName());
			
			if ( pid >= 0 ){
				
				cmd_list.add( "__OwningControllerProcess" );
				cmd_list.add( String.valueOf( pid ));
			}
			
			ProcessBuilder pb = GeneralUtils.createProcessBuilder( plugin_dir, cmd_list.toArray(new String[cmd_list.size()]), null );
		
			pb.start();
			
			log( "Server started" );
			
			return( true );
			
		}catch( Throwable e ){
		
			log( "Server start failed: " + Debug.getNestedExceptionMessage( e ));
			
			Debug.out( e );
			
			return( false );
		}
	}
	
	private void
	asyncGetConnection()
	{
		new AEThread2( "init" )
		{
			public void
			run()
			{
				getConnection();
			}
		}.start();
	}
	
	private boolean
	isConnected()
	{
		synchronized( this ){
			
			return( current_connection != null && current_connection.isConnected());
		}
	}
	
	private void
	closeConnection()
	{
		synchronized( this ){
			
			if ( current_connection != null ){
						
				if ( current_connection.isConnected()){
					
					current_connection.close( "Close requested" );
				}
				
				current_connection = null;
			}
			
			last_connect_time = 0;		// explicit close so reset connect rate limiter
		}
	}
		
	private ControlConnection
	getConnection()
	{
		init_sem.reserve();
		
		final AESemaphore sem;
		
		synchronized( this ){
		
			if ( current_connection != null ){
				
				if ( current_connection.isConnected()){
					
					return( current_connection );
					
				}else{
					
					current_connection = null;
				}
			}
			
			if ( unloaded ){
					
				return( null );
			}
				
			if ( connection_sem == null ){
				
				final long now = SystemTime.getMonotonousTime();
				
				if ( last_connect_time != 0 && now - last_connect_time < MIN_RECONNECT_TIME ){
					
					return( null );
				}
				
				sem = connection_sem = new AESemaphore( "ConWait" );
				
				last_connect_time  = now;
				
				// kick off async con
				
				new AEThread2( "ControlPortCon")
				{
					public void
					run()
					{		
						try{
							if ( startServer()){
								
								log( "Waiting for server to initialise" );

								while( !unloaded ){
																	
									ControlConnection control = new ControlConnection( data_dir, internal_control_port, internal_socks_port );
								
									if ( control.connect()){
										
										log( "Server initialised" );
										
										current_connection = control;
										
										last_use_time	= SystemTime.getMonotonousTime();
										
										break;
										
									}else{
										
										control.close( null );
									}
									
									if ( SystemTime.getMonotonousTime() - now > MAX_CONNECT_WAIT_TIME ){
										
										log( "Server failed to initialise, abandoning" );
										
										break;
										
									}else{
										
										try{
											Thread.sleep( 1000 );
											
										}catch( Throwable f ){
											
										}
									}
								}
							}
						}finally{
							
							synchronized( TorPlugin.this ){
								
								connection_sem = null;
								
								sem.releaseForever();
							}
						}
					}
				}.start();
				
			}else{
				
				sem = connection_sem;
			}
		}
		
		sem.reserve();
		
		synchronized( this ){

			return( current_connection );
		}
	}
	
	private int
	getPID()
	{
		try{
			RuntimeMXBean runtime_bean =	java.lang.management.ManagementFactory.getRuntimeMXBean();
			
			Field jvm_field = runtime_bean.getClass().getDeclaredField( "jvm" );
			
			jvm_field.setAccessible( true );
			
			Object jvm = jvm_field.get( runtime_bean );
			
			Method pid_method = jvm.getClass().getDeclaredMethod( "getProcessId" );
			
			pid_method.setAccessible( true );

			int pid = (Integer)pid_method.invoke( jvm );
			
			return( pid );
			
		}catch( Throwable e ){
			
			return( -1 );
		}
	}
	
	private void
	log(
		String		str )
	{
		if ( log != null ){
			
			log.log( str );
		}
	}
	
	public void
	unload()
	{
		synchronized( this ){
			
			unloaded = true;
			
			if ( current_connection != null ){
				
				current_connection.shutdown( false );
				
				current_connection = null;
			}
		}
		
		if ( config_model != null ){
			
			config_model.destroy();
			
			config_model = null;
		}
		
		if ( view_model != null ){
			
			view_model.destroy();
		}
	}
	
	private boolean
	hostAccepted(
		String	host )
	{
		if ( host.equals( "127.0.0.1" )){

			return( false );
		}
		
		String	lc_host = host.toLowerCase( Locale.US );
		
		if ( lc_host.endsWith( ".onion" )){

			return( true );
			
		}else if ( lc_host.endsWith( ".i2p" )){
			
			return( false );
		}
		
		if ( prompt_on_use ){

			if ( prompt_skip_vuze && Constants.isAzureusDomain( host )){
			
				return( true );
			}
		}
		
		return( true );
	}
	
	private String
	rewriteHost(
		String		host )
	{
		if ( host.equals( "version.vuze.com" )){
			
			return( "up33pjmm5bxgy7nb.onion" );
		}
		
		return( null );
	}
	
	private WeakHashMap<Proxy,String>	proxy_map = new WeakHashMap<Proxy, String>();
	
	private Proxy
	getActiveProxy(
		String		host )
	{
		if ( !hostAccepted( host )){
			
			return( null );
		}
				
		int	socks_port;
		
		if ( external_tor ){

			socks_port = active_socks_port;
			
		}else{
			
			ControlConnection con = getConnection();
	
			if ( con == null ){
		
				return( null );
			}
			
			socks_port = con.getSOCKSPort();
		}
		
		Proxy proxy = new Proxy( Proxy.Type.SOCKS, new InetSocketAddress( "127.0.0.1", socks_port ));
		
		synchronized( proxy_map ){
		
			proxy_map.put( proxy, host );
		}
		
		return( proxy );
	}
	
		// IPC stuff
	
	public void
	setProxyStatus(
		Proxy		proxy,
		boolean		good )
	{
		String host;
		
		synchronized( proxy_map ){
			
			host = proxy_map.remove( proxy );
		}
		
		if ( host != null ){
				
			System.out.println( host + " -> " + good );
		}
	}
	
	public Object[]
	getProxy(
		URL		url )
	
		throws IPCException
	{
		String 	host = url.getHost();
		
		Proxy proxy = getActiveProxy( host );
		
		if ( proxy != null ){
		
			String updated_host	= rewriteHost( host );
			
			if ( host != null ){
				
				url = UrlUtils.setHost( url, updated_host );
			}
			
			return( new Object[]{ proxy, url });
		}
		
		return( null );
	}
	
	public Object[]
	getProxy(
		String		host,
		int			port )
	
		throws IPCException
	{
		Proxy proxy = getActiveProxy( host );
		
		if ( proxy != null ){
		
			String updated_host	= rewriteHost( host );
			
			if ( updated_host != null ){
				
				host = updated_host;
			}
			
			return( new Object[]{ proxy, host, port });
		}
		
		return( null );	}
	
	private class
	ControlConnection
	{
		private int			control_port;
		private int			socks_port;
		private File		data_dir;
	
		private Socket				socket;
		private LineNumberReader 	lnr;
		private OutputStream 		os;
		
		private boolean		did_connect;
	
		private TimerEventPeriodic	timer;
		
		private 
		ControlConnection(
			File		_data_dir,
			int			_control_port,
			int			_socks_port )
		{
			data_dir		= _data_dir;
			control_port	= _control_port;
			socks_port		= _socks_port;
		}
		
		private int
		getSOCKSPort()
		{
			return( socks_port );
		}
		
		private boolean
		connect()
		{
			try{
				socket = new Socket( "127.0.0.1", control_port );

				did_connect = true;
				
				socket.setSoTimeout( 30*1000 );

				InputStream is = socket.getInputStream();
										
				lnr = new LineNumberReader( new InputStreamReader( is ));

				os = socket.getOutputStream();
							
				byte[] client_nonce = new byte[32];
			
				RandomUtils.nextSecureBytes( client_nonce );
				
				String reply = sendAndReceive( "AUTHCHALLENGE SAFECOOKIE " + ByteFormatter.encodeString( client_nonce ).toUpperCase());
								
				if ( !reply.startsWith( "250 AUTHCHALLENGE " )){
					
					throw( new Exception( "AUTHCHALLENGE response invalid: " + reply ));
				}
				
				File cookie_file = new File( data_dir, "control_auth_cookie" );
				
				byte[] cookie = FileUtil.readFileAsByteArray( cookie_file );
				
				reply = reply.substring( 18 ).trim();
				
				String[] bits = reply.split( " " );
				
				byte[] server_hash 	= ByteFormatter.decodeString( bits[0].substring( 11 ).trim());
				byte[] server_nonce = ByteFormatter.decodeString( bits[1].substring( 12 ).trim());
				
				Mac mac = Mac.getInstance("HmacSHA256");
				
				SecretKeySpec secret_key = new SecretKeySpec( "Tor safe cookie authentication server-to-controller hash".getBytes(Constants.BYTE_ENCODING), "HmacSHA256");
				
				mac.init( secret_key );
				
				mac.update( cookie );
				mac.update( client_nonce );
				mac.update( server_nonce );
				
				byte[] server_digest = mac.doFinal();
				
				if ( !Arrays.equals( server_hash, server_digest )){
					
					throw( new Exception( "AUTHCHALLENGE response server hash incorrect" ));
				}
											
				secret_key = new SecretKeySpec( "Tor safe cookie authentication controller-to-server hash".getBytes(Constants.BYTE_ENCODING), "HmacSHA256");

				mac.init( secret_key );
				
				mac.update( cookie );
				mac.update( client_nonce );
				mac.update( server_nonce );
				
				reply = sendAndReceive( "AUTHENTICATE " + ByteFormatter.encodeString( mac.doFinal()).toUpperCase());
				
				if ( !reply.startsWith( "250 OK" )){
					
					throw( new Exception( "AUTHENTICATE response invalid: " + reply ));
				}
				
				reply = sendAndReceive( "TAKEOWNERSHIP" );
										
				if ( !reply.startsWith( "250 OK" )){
					
					throw( new Exception( "TAKEOWNERSHIP response invalid: " + reply ));
				}
				
				reply = sendAndReceive( "RESETCONF __OwningControllerProcess" );
									
				if ( !reply.startsWith( "250 OK" )){
					
					throw( new Exception( "TAKEOWNERSHIP response invalid: " + reply ));
				}
				
				String info = getInfo();
				
				log( "Connection to control port established - " + info );
				
				timer = SimpleTimer.addPeriodicEvent(
							"keepalive",
							30*1000,
							new TimerEventPerformer() {
								
								private boolean	running = false;
								
								public void 
								perform(
									TimerEvent event )
								{
									if ( unloaded || !isConnected()){
										
										timer.cancel();
										
										return;
									}
									
									synchronized( ControlConnection.this ){
										
										if ( running ){
											
											return;
										}
										
										running = true;
									}
									
									new AEThread2( "getinfo" )
									{
										public void
										run()
										{
											try{
												String info = getInfo();
												
												//System.out.println( info );
												
											}catch( Throwable e ){
												
											}finally{
												
												synchronized( ControlConnection.this ){
													
													running = false;
												}
											}
										}
									}.start();
								}
							});
					
				return( true );
				
			}catch( Throwable e ){
				
				if ( did_connect ){
				
					String msg = "Connection error: " + Debug.getNestedExceptionMessage( e );
					
					Debug.out( msg );
					
					close( msg );
					
				}else{
					
					close( null );
				}
				
				
				return( false );
			}	
		}
		
		private String
		getInfo()
		
			throws IOException
		{
			synchronized( ControlConnection.this ){

				writeLine( "GETINFO version" );
				
				String	result = "";
				
				while( true ){
					
					String reply = readLine();
					
					if ( reply.startsWith( "250" )){
						
						if ( reply.equals( "250 OK" )){
							
							return( result );
							
						}else{
							
							result = reply.substring( 4 );
						}
					}else{
						
						throw( new IOException( "Unexpected reply: " + reply ));
					}	
				}
			}
		}
		
		private String
		sendAndReceive(
			String	str )
		
			throws IOException
		{
			synchronized( ControlConnection.this ){
				
				writeLine( str );
				
				return( readLine());
			}
		}
		
		private void
		writeLine(
			String		str )
		
			throws IOException 
		{
			try{
				os.write( ( str + "\r\n" ).getBytes(Constants.BYTE_ENCODING));
			
				os.flush();
				
			}catch( IOException e ){
			
				close( Debug.getNestedExceptionMessage( e ));
				
				throw( e );
			}
		}
		
		private String
		readLine()
		
			throws IOException
		{
			String line = lnr.readLine();
			
			if ( line == null ){
				
				close( "Unexpected end of file" );
				
				throw( new IOException( "Unexpected end of file" ));
			}
			
			return( line.trim());
		}
		
		private boolean
		isConnected()
		{
			return( socket != null );
		}
		
		private void
		shutdown(
			boolean	force )
		{
			try{
				if ( socket != null ){
					
					if ( force ){
						
						sendAndReceive( "SIGNAL HALT" );
						
					}else{
						
						sendAndReceive( "SIGNAL SHUTDOWN" );
					}
				}
			}catch( Throwable e ){
				
			}finally{
				
				close( "Shutdown" );
			}
		}
		
		private void
		close(
			String	reason )
		{
			if ( reason != null ){
				
				log( "Control connection closed: " + reason );
			}
			
			if ( timer != null ){
				
				timer.cancel();
				
				timer = null;
			}
			
			if ( lnr != null ){
				
				try{
					lnr.close();
					
				}catch( Throwable e ){
				}
				
				lnr = null;
			}
			
			if ( os != null ){
				
				try{
					os.close();
					
				}catch( Throwable e ){
				}
				
				os = null;
			}
			
			if ( socket != null ){
				
				try{
					socket.close();
					
				}catch( Throwable e ){
				}
				
				socket = null;
			}
		}
	}
}
