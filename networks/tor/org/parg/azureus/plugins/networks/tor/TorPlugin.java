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
import java.util.concurrent.atomic.AtomicLong;
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
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.parg.azureus.plugins.networks.tor.TorPluginUI.PromptResponse;

import com.aelitis.azureus.core.util.GeneralUtils;

public class 
TorPlugin
	implements UnloadablePlugin
{
	private PluginInterface			plugin_interface;
	private PluginConfig			plugin_config;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	
	private volatile TorPluginUI		plugin_ui;
	
	private AESemaphore		init_sem 		= new AESemaphore( "TP:init" );
	private AESemaphore		ui_attach_sem 	= new AESemaphore( "TP:UI" );
	
	private volatile long	init_time;
	
	private static final int	SOCKS_PORT_DEFAULT		= 29101;
	private static final int	CONTROL_PORT_DEFAULT	= 29151;

	private File	plugin_dir;
	private File	config_file;
	private File	data_dir;
	
	private BooleanParameter prompt_on_use_param;
	
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
	
	private Set<String>				prompt_decisions 	= new HashSet<String>();
	private String					last_decision_log	= "";
	
	private long					last_use_time;
	
	private AtomicLong	proxy_request_count		= new AtomicLong();
	private AtomicLong	proxy_request_ok		= new AtomicLong();
	private AtomicLong	proxy_request_failed	= new AtomicLong();
	
	private static final int MAX_HISTORY_RECORDS	= 4096;
	
	private Map<String,ProxyHistory>		proxy_history = 
			new LinkedHashMap<String,ProxyHistory>(MAX_HISTORY_RECORDS,0.75f,true)
			{
				protected boolean 
				removeEldestEntry(
			   		Map.Entry<String,ProxyHistory> eldest) 
				{
					return size() > MAX_HISTORY_RECORDS;
				}
			};
			
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
						
			plugin_config = plugin_interface.getPluginconfig();
						
			config_model = ui_manager.createBasicPluginConfigModel( "plugins", "aztorplugin.name" );

			config_model.addLabelParameter2( "aztorplugin.info1" );
			config_model.addLabelParameter2( "aztorplugin.info2" );
			
			final BooleanParameter enable_param = config_model.addBooleanParameter2( "enable", "aztorplugin.enable", true );

			final BooleanParameter start_on_demand_param 	= config_model.addBooleanParameter2( "start_on_demand", "aztorplugin.start_on_demand", true );
			final BooleanParameter stop_on_idle_param	 	= config_model.addBooleanParameter2( "stop_on_idle", "aztorplugin.stop_on_idle", true );
			prompt_on_use_param 							= config_model.addBooleanParameter2( "prompt_on_use", "aztorplugin.prompt_on_use", true );
			final BooleanParameter prompt_skip_vuze_param 	= config_model.addBooleanParameter2( "prompt_skip_vuze", "aztorplugin.prompt_skip_vuze", true );

			final ActionParameter prompt_reset_param = config_model.addActionParameter2( "aztorplugin.ask.clear", "aztorplugin.ask.clear.button" );
			
			prompt_reset_param.addListener(
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter param ) 
					{
						resetPromptDecisions();
					}
				});
			
			config_model.createGroup( "aztorplugin.prompt_options", new Parameter[]{ prompt_skip_vuze_param, prompt_reset_param });
			
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
									if ( !external_tor ){
										
										if ( !isConnected()){
											
											lines.add( "Server not running, starting it" );
										}
										
										getConnection( 10*1000, false );
									}
									
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
						plugin_enabled 		= enable_param.getValue();
						external_tor		= ext_tor_param.getValue();
						start_on_demand		= start_on_demand_param.getValue();
						stop_on_idle		= stop_on_idle_param.getValue();
						prompt_on_use		= prompt_on_use_param.getValue();
						prompt_skip_vuze	= prompt_skip_vuze_param.getValue();
						
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
						prompt_reset_param.setEnabled( plugin_enabled && prompt_on_use );
						
						control_port_param.setEnabled( plugin_enabled && !external_tor );
						socks_port_param.setEnabled( plugin_enabled && !external_tor );
						ext_tor_param.setEnabled( plugin_enabled );
						ext_socks_port_param.setEnabled( plugin_enabled && external_tor );
						
						test_url_param.setEnabled( plugin_enabled );
						test_param.setEnabled( plugin_enabled );
						
						if ( param != null ){
						
							logPromptDecisions();
						}
					}
				};
				
			enable_param.addListener( enabler_listener );
			start_on_demand_param.addListener( enabler_listener );
			stop_on_idle_param.addListener( enabler_listener );
			prompt_on_use_param.addListener( enabler_listener );
			prompt_skip_vuze_param.addListener( enabler_listener );
			ext_tor_param.addListener( enabler_listener );
			
			enabler_listener.parameterChanged( null );
			
			readPromptDecisions();

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
						init_time = SystemTime.getMonotonousTime();
						
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
			
			pi.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						final UIInstance		instance )
					{
						if ( instance.getUIType() == UIInstance.UIT_SWT ){
								
							try{
								synchronized( TorPlugin.this ){
									
									if ( unloaded ){
										
										return;
									}
								
									try{
										plugin_ui = 
											(TorPluginUI)Class.forName( "org.parg.azureus.plugins.networks.tor.swt.TorPluginUISWT").getConstructor(
													new Class[]{ TorPlugin.class } ).newInstance( new Object[]{ TorPlugin.this } );
										
									}catch( Throwable e ){
									
										Debug.out( e );
									}
								}
							}finally{
								
								ui_attach_sem.releaseForever();
							}
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						
					}
				});
			

		}catch( Throwable e ){
				
			init_time = SystemTime.getMonotonousTime();

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
			
			prepareConnection( "Startup" );
		}
		
		SimpleTimer.addPeriodicEvent(
			"TP:checker",
			30*1000,
			new TimerEventPerformer()
			{	
				private String	last_stats = "";
				
				public void 
				perform(
					TimerEvent event ) 
				{
					String stats = "Requests=" + proxy_request_count.get() + ", ok=" + proxy_request_ok.get() + ", failed=" + proxy_request_failed.get();
					
					if ( !stats.equals( last_stats )){
						
						last_stats = stats;
						
						log( stats );
					}
					
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
						
						closeConnection( "Close on idle" );
						
					}else if ( should_be_connected ){
						
						prepareConnection( "Start on demand disabled" );
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
	
	private boolean
	isConnected()
	{
		synchronized( this ){
			
			return( current_connection != null && current_connection.isConnected());
		}
	}
	
	private boolean
	isConnectedOrConnecting()
	{	
		synchronized( this ){
		
			return( isConnected() || connection_sem != null );
		}
	}
	
	private void
	closeConnection(
		String	reason )
	{
		synchronized( this ){
			
			if ( current_connection != null ){
						
				if ( current_connection.isConnected()){
					
					current_connection.close( "Close requested: " + reason );
				}
				
				current_connection = null;
			}
			
			last_connect_time = 0;		// explicit close so reset connect rate limiter
		}
	}
		
	private void
	prepareConnection(
		final String	reason )
	{
		if ( isConnectedOrConnecting()){
			
			return;
		}
		
		new AEThread2( "init" )
		{
			public void
			run()
			{
				
				if ( !isConnectedOrConnecting()){
				
					log( "Preparing connection: " + reason );
					
					getConnection( 0, true );
				}
			}
		}.start();
	}
	
	private ControlConnection
	getConnection(
		int			max_wait_millis,
		boolean		async )
	{
		if ( !init_sem.reserve( max_wait_millis )){
			
			return( null );
		}
		
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
		
		if ( async ){
			
			return( null );
			
		}else{
			sem.reserve( max_wait_millis );
			
			synchronized( this ){
	
				return( current_connection );
			}
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
			
			if ( plugin_ui != null ){
				
				plugin_ui.destroy();
				
				plugin_ui = null;
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
	
	private void
	logPromptDecisions()
	{
		String	msg;
		
		if ( prompt_on_use ){
		
			msg = (prompt_skip_vuze?"Allow Vuze; ":"") + prompt_decisions;
			
		}else{
			
			msg = "Disabled";
		}
		
		if ( !last_decision_log.equals( msg )){
			
			last_decision_log = msg;
			
			log( "Prompt decisions: " + msg );
		}
	}
	
	private void
	resetPromptDecisions()
	{
		synchronized( this ){
			
			if ( prompt_decisions.size() > 0 ){
			
				prompt_decisions.clear();
			
				writePromptDecisions();
			}
		}
	}
	
	private void
	readPromptDecisions()
	{
		synchronized( this ){
			
			String	str = plugin_config.getPluginStringParameter( "prompt.decisions", "" );
			
			String[] bits = str.split( "," );
			
			prompt_decisions.clear();
			
			for ( String bit: bits ){
				
				bit = bit.trim();
				
				if ( bit.length() > 0 ){
				
					prompt_decisions.add( bit );
				}
			}
			
			logPromptDecisions();
		}
	}
	
	private void
	writePromptDecisions()
	{
		synchronized( this ){
			
			String str = "";
			
			for ( String s: prompt_decisions ){
				
				str += (str.length()==0?"":",") + s;
			}
			
			plugin_config.setPluginParameter( "prompt.decisions", str );
			
			logPromptDecisions();
		}
	}
	
		/**
		 * @param host
		 * @return 0 = no prompt, do it; 1 = prompt; 2 = don't do it
		 */

	private int
	getPromptDecision(
		String		host )
	{
		synchronized( this ){
			
			if ( prompt_on_use ){
	
				if ( prompt_skip_vuze && Constants.isAzureusDomain( host )){
				
					return( 0 );
					
				}else{
				
					if ( prompt_decisions.contains( "^*" )){
					
						return( 2 );
						
					}else if ( prompt_decisions.contains( host )){
						
						return( 0 );
					
					}else if ( prompt_decisions.contains( "^" + host )){
						
						return( 2 );
					}
					
					
					String[] bits = host.split( "\\." );
					
					int	bits_num = bits.length;
					
					if ( bits_num > 2 ){
						
						String wild =  "*." + bits[bits_num-2] + "." + bits[bits_num-1];
						
						if ( prompt_decisions.contains( wild )){
							
							return( 0 );
						
						}else if ( prompt_decisions.contains( "^" + wild )){
							
							return( 2 );
						}
					}
				}
				
				return( 1 );
				
			}else{
				
				return( 0 );
			}
		}
	}
	
	private void
	setPromptDecision(
		String		host,
		boolean		accepted )
	{
		boolean	all_domains = host.equals( "*" );
		
		synchronized( this ){
			
			if ( all_domains ){
				
				if ( accepted ){
					
					prompt_on_use_param.setValue( false );
					
					resetPromptDecisions();
					
				}else{
					
					prompt_decisions.clear();
					
					prompt_decisions.add( "^*" );
					
					writePromptDecisions();
				}
			}else{
				
				if ( host.startsWith( "*" )){
					
					String term = host.substring( 1 );
					
					Iterator<String> it = prompt_decisions.iterator();
					
					while( it.hasNext()){
						
						String entry = it.next();
						
						if ( entry.endsWith( term )){
							
							it.remove();
						}
					}
				}
				
				prompt_decisions.add( accepted?host:("^"+host));	
				
				writePromptDecisions();
			}
		}
	}
	
	private AsyncDispatcher prompt_dispatcher = new AsyncDispatcher();
	
	private boolean
	promptUser(
		final String		reason,
		final String		host )
	{	
			// maintain a queue of prompt requests so things don't get out of control
			// timeout callers to prevent hanging the core if user isn't present
		
		final AESemaphore sem = new AESemaphore( "promptAsync" );
		
		final boolean[] result = { false };
		
		prompt_dispatcher.dispatch(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					try{
						boolean	wait_for_ui = false;
						
						synchronized( this ){
							
							if ( unloaded ){
								
								return;
							}
							
							if ( !ui_attach_sem.isReleasedForever()){
								
								if ( init_time != 0 && SystemTime.getMonotonousTime() - init_time > 60*1000 ){
									
									return;
								}
							}
							
							wait_for_ui = plugin_ui == null;
						}
						
						if ( wait_for_ui ){
							
							ui_attach_sem.reserve( 30*1000 );
							
							if ( plugin_ui == null ){
								
								return;
							}
						}
						
						int recheck_decision = getPromptDecision( host );
						
						if ( recheck_decision == 0 ){
							
							result[0] = true;
							
						}else if ( recheck_decision == 1 ){
							
								// we're prompting the user, let's assume they're going to go ahead so
								// we should warm up the server if not yet up
							
							if ( !external_tor ){
								
								prepareConnection( "About to prompt" );
							}
							
							PromptResponse response = plugin_ui.promptForHost( reason, host );
							
							boolean	accepted = response.getAccepted();
		
							String remembered = response.getRemembered();
							
							if ( remembered != null ){
								
								setPromptDecision( remembered, accepted );
							}				
							
							result[0] = accepted;
						}
					}finally{
						
						sem.release();
					}
				}
			});
		
		sem.reserve( 60*1000 );
						
		return( result[0] );
	}
	
	private boolean
	isHostAccepted(
		String		reason,
		String		host )
	{
		if ( host.equals( "127.0.0.1" )){

			return( false );
		}
		
		String	lc_host = host.toLowerCase( Locale.US );
		
		if ( lc_host.endsWith( ".i2p" )){
			
			return( false );
		}
		
		if ( !checkProxyHistoryOK( host )){
			
			return( false );
		}
		
		if ( lc_host.endsWith( ".onion" )){

			return( true );	
		}
		
		int decision = getPromptDecision( host );
		
		if ( decision == 0 ){
			
			return( true );
			
		}else if ( decision == 1 ){
			
			return( promptUser( reason, host ));
			
		}else{
				
			return( false );
		}
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
		String		reason,
		String		host )
	{
		if ( !plugin_enabled || unloaded ){
			
			return( null );
		}
		
		if ( !isHostAccepted( reason, host )){
			
			return( null );
		}
				
		int	socks_port;
		
		if ( external_tor ){

			socks_port = active_socks_port;
			
		}else{
			
			ControlConnection con = getConnection( 30*1000, false );
	
			if ( con == null ){
		
				return( null );
			}
			
			socks_port = con.getSOCKSPort();
		}
		
		Proxy proxy = new Proxy( Proxy.Type.SOCKS, new InetSocketAddress( "127.0.0.1", socks_port ));
		
		synchronized( proxy_map ){
		
			proxy_map.put( proxy, host );
		}
		
		last_use_time	= SystemTime.getMonotonousTime();

		proxy_request_count.incrementAndGet();
		
		return( proxy );
	}
	
	private boolean
	checkProxyHistoryOK(
		String		host )
	{
		synchronized( this ){

			ProxyHistory history = proxy_history.get( host );

			if ( history == null ){
				
				history = new ProxyHistory( host );
				
				proxy_history.put( host, history );
			}
			
			return( history.canConnect());
		}
	}
	
	private void
	updateProxyHistory(
		String		host,
		boolean		ok )
	{
		synchronized( this ){
			
			ProxyHistory history = proxy_history.get( host );
			
			if ( history == null ){
				
				history = new ProxyHistory( host );
				
				proxy_history.put( host, history );
			}
			
			history.setOutcome( ok );
		}
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
				
			if ( good ){
				
				proxy_request_ok.incrementAndGet();
				
			}else{
				
				proxy_request_failed.incrementAndGet();
			}
			
			updateProxyHistory( host, good );
			
		}else{
			
			Debug.out( "Proxy entry missing!" );
		}
	}
	
	public Object[]
	getProxy(
		String		reason,
		URL			url )
	
		throws IPCException
	{
		String 	host = url.getHost();
		
		Proxy proxy = getActiveProxy( reason, host );
		
		if ( proxy != null ){
		
			String updated_host	= rewriteHost( host );
			
			if ( updated_host != null ){
				
				url = UrlUtils.setHost( url, updated_host );
			}
			
			return( new Object[]{ proxy, url });
		}
		
		return( null );
	}
	
	public Object[]
	getProxy(
		String		reason,
		String		host,
		int			port )
	
		throws IPCException
	{
		Proxy proxy = getActiveProxy( reason, host );
		
		if ( proxy != null ){
		
			String updated_host	= rewriteHost( host );
			
			if ( updated_host != null ){
				
				host = updated_host;
			}
			
			return( new Object[]{ proxy, host, port });
		}
		
		return( null );
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
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
	
	private class
	ProxyHistory
	{
		private String	host;
		
		private long	last_connect_time;
		private int		total_fails;
		private int		total_ok;
		
		private int		consec_fails;
		
		private
		ProxyHistory(
			String		_host )
		{
			host		= _host;
		}
		
		private boolean
		canConnect()
		{
			long now = SystemTime.getMonotonousTime();
			
			boolean ok = consec_fails < 3;
			
			if ( !ok ){
				
				int delay = 30*60*1000;
				
				for ( int i=3;i<consec_fails;i++){
					
					delay <<= 1;
					
					if ( delay > 24*60*60*1000 ){
						
						delay = 24*60*60*1000;
						
						break;
					}
				}
				
				if ( now - last_connect_time >= delay ){
					
					ok = true;
				}
			}
			
			if ( ok ){
				
				last_connect_time = now;
			}
			
			return( ok );
		}
		
		private void
		setOutcome(
			boolean		ok )
		{
			if ( ok ){
				
				total_ok++;
				
				consec_fails = 0;
				
			}else{
				
				total_fails++;
				
				consec_fails++;
				
				if ( consec_fails > 2 ){
					
					log( "Failed to connect to '" + host + "' " + consec_fails + " in a row (ok=" + total_ok + ", fails=" + total_fails +")" );
				}
			}
		}
	}
}
