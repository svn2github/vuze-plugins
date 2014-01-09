/*
 * Created on Jan 6, 2014
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


package org.parg.azureus.plugins.networks.torbrowser;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import com.aelitis.azureus.core.util.GeneralUtils;

public class 
TorBrowserPlugin
	implements UnloadablePlugin
{
	public static final String HOME_PAGE = "https://check.torproject.org/";

	private PluginInterface				plugin_interface;
	private BasicPluginConfigModel 		config_model;
	private BasicPluginViewModel		view_model;
	private LoggerChannel				log;
	
	private IPCInterface		tor_ipc;
	
	private String				init_error;
	private File				browser_dir;
	
	private AESemaphore					init_complete_sem = new AESemaphore( "tbp_init" );
	
	private Set<BrowserInstance>		browser_instances = new HashSet<BrowserInstance>();
	
	private TimerEventPeriodic			browser_timer;
	
	private AsyncDispatcher		launch_dispatcher = new AsyncDispatcher( "Tor:launcher" );
	
	private static final int LAUNCH_TIMEOUT_INIT 	= 30*1000;
	private static final int LAUNCH_TIMEOUT_NEXT	= 1000;
	
	private int	launch_timeout	= LAUNCH_TIMEOUT_INIT;
	
	private String	last_check_log = "";
	
	public void
	initialize(
		PluginInterface		pi )
	
		throws PluginException 
	{
		plugin_interface = pi;

		setUnloadable( true );
		
		final LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();

		log	= plugin_interface.getLogger().getTimeStampedChannel( "TorBrowser");
		
		final UIManager	ui_manager = plugin_interface.getUIManager();

		view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "aztorbrowserplugin.name" ));

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

		config_model = ui_manager.createBasicPluginConfigModel( "plugins", "aztorbrowserplugin.name" );

		config_model.addLabelParameter2( "aztorbrowserplugin.info1" );
		config_model.addLabelParameter2( "aztorbrowserplugin.info2" );

		config_model.addLabelParameter2( "aztorbrowserplugin.blank" );

		config_model.addHyperlinkParameter2( "aztorbrowserplugin.link", loc_utils.getLocalisedMessageText( "aztorbrowserplugin.link.url" ));
		
		config_model.addLabelParameter2( "aztorbrowserplugin.blank" );

		final LabelParameter status_label = config_model.addLabelParameter2( "aztorbrowserplugin.status");
		
		config_model.addLabelParameter2( "aztorbrowserplugin.blank" );
		
		view_model.setConfigSectionID( "aztorbrowserplugin.name" );

		final ActionParameter prompt_reset_param = config_model.addActionParameter2( "aztorbrowserplugin.launch", "aztorbrowserplugin.launch.button" );
		
		prompt_reset_param.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					prompt_reset_param.setEnabled( false );
					
					try{
						launchBrowser( 
							HOME_PAGE,
							true,
							new Runnable()
							{
								public void
								run()
								{
									prompt_reset_param.setEnabled( true );
								}
							});
						
					}catch( Throwable e ){
						
						prompt_reset_param.setEnabled( true );
						
						ui_manager.showTextMessage(
								"aztorbrowserplugin.launch.fail.msg",
								null,
								"Browser launch failed: " + Debug.getNestedExceptionMessage(e));
					}
				}
			});
		
		try{
			File plugin_install_dir = new File( pi.getPluginDirectoryName());
			
			File plugin_data_dir	= pi.getPluginconfig().getPluginUserFile( "test" ).getParentFile();
			
			if ( !plugin_data_dir.exists()){
				
				plugin_data_dir.mkdirs();
			}
			
			File[]	install_files = plugin_install_dir.listFiles();
			
			List<File>	old_zip_files = new ArrayList<File>();
			
			String 	highest_version_zip			= "0";
			File	highest_version_zip_file	= null;
			
			for ( File file: install_files ){
				
				String name = file.getName();
				
				if ( file.isFile() && name.startsWith( "browser-" ) && name.endsWith( ".zip" )){
					
					String version = name.substring( name.lastIndexOf( "-" ) + 1, name.length() - 4 );
					
					if ( Constants.compareVersions( version, highest_version_zip ) > 0 ){
						
						highest_version_zip = version;
						
						if ( highest_version_zip_file != null ){
							
							old_zip_files.add( highest_version_zip_file );
						}
						
						highest_version_zip_file = file;
					}
				}
			}
			
			File[]	data_files = plugin_data_dir.listFiles();
			
			String 	highest_version_data		= "0";
			File	highest_version_data_file 	= null;
			
			for ( File file: data_files ){
				
				String name = file.getName();
				
				if ( file.isDirectory() && name.startsWith( "browser_" )){
					
					String version = name.substring( 8 );
					
					if ( Constants.compareVersions( version, highest_version_data ) > 0 ){
						
						highest_version_data = version;
						
						highest_version_data_file = file;
					}
				}
			}
						
			if ( Constants.compareVersions( highest_version_zip, highest_version_data ) > 0 ){
				
				File temp_data = new File( plugin_data_dir, "tmp_" + highest_version_zip );
				
				if ( temp_data.exists()){
					
					if ( !FileUtil.recursiveDeleteNoCheck( temp_data )){
						
						throw( new Exception( "Failed to remove tmp directory: " + temp_data ));
					}
				}
				
				ZipInputStream zis = null;
				
				try{
					zis = new ZipInputStream( new BufferedInputStream( new FileInputStream( highest_version_zip_file ) ));
							
					byte[] buffer = new byte[64*1024];
					
					while( true ){
						
						ZipEntry	entry = zis.getNextEntry();
							
						if ( entry == null ){
							
							break;
						}
					
						String	name = entry.getName();
					
						if ( name.endsWith( "/" )){
							
							continue;
						}
						
						if ( File.separatorChar != '/' ){
							
							name = name.replace( '/', File.separatorChar );
						}
						
						File target_out = new File( temp_data, name );
						
						File parent_folder = target_out.getParentFile();
						
						if ( !parent_folder.exists()){
							
							parent_folder.mkdirs();
						}
						
						OutputStream	entry_os = null;

						try{
							entry_os = new FileOutputStream( target_out );
							
							while( true ){
								
								int	len = zis.read( buffer );
								
								if ( len <= 0 ){
									
									break;
								}
																											
								entry_os.write( buffer, 0, len );
							}
						}finally{
							
							if ( entry_os != null ){
								
								try{
									entry_os.close();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					}
				}finally{
					
					if ( zis != null ){
						
						try{
							zis.close();
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
				
					// migrate any existing profile data
				
				if ( highest_version_data_file != null ){
					
					File	old_profile = new File( highest_version_data_file, "Data" );
					File	new_profile = new File( temp_data, "Data" );
					
					copyProfile( old_profile, new_profile );
					
				}
				
				File target_data = new File( plugin_data_dir, "browser_" + highest_version_zip );
				
				if ( target_data.exists()){
					
					throw( new Exception( "Target already exists: " + target_data ));
				}
				
				if ( !temp_data.renameTo( target_data )){
					
					throw( new Exception( "Failed to rename " + temp_data + " to " + target_data ));
				}
				
				for ( File old: old_zip_files ){
					
					old.delete();
				}
								
				if ( Constants.isOSX ){
					
					String chmod = findCommand( "chmod" );
					
					if ( chmod == null ){
						
						throw( new Exception( "Failed to find 'chmod' command" ));
					}
					
					Runtime.getRuntime().exec(
						new String[]{
							chmod,
							"-R",
							"+x",
							target_data.getAbsolutePath()
						});
				}
				
				browser_dir = target_data;

			}else{
				
				File existing_data = new File( plugin_data_dir, "browser_" + highest_version_data );

				if ( highest_version_data.equals( "0" ) || !existing_data.exists()){
					
					throw( new Exception( "No browser version installed" ));
				}
								
				browser_dir = existing_data;
			}
			
			plugin_interface.addListener(
				new PluginAdapter()
				{
					public void
					initializationComplete()
					{
						try{
							checkConfig();
							
							status_label.setLabelText( loc_utils.getLocalisedMessageText( "aztorbrowserplugin.status.ok" ));
							
							log( "Initialization complete" );
							
						}catch( Throwable e ){
							
							init_error = Debug.getNestedExceptionMessage( e );
							
							status_label.setLabelText( loc_utils.getLocalisedMessageText( "aztorbrowserplugin.status.fail", new String[]{ init_error }) );

							Debug.out( e );
							
							log( "Initialization failed: " + init_error );
							
						}finally{
							
							init_complete_sem.releaseForever();
						}
					}
					
					public void 
					closedownInitiated() 
					{
						killBrowsers();
					}
				});
				
		}catch( Throwable e ){
			
			init_error = Debug.getNestedExceptionMessage( e );
			
			status_label.setLabelText( loc_utils.getLocalisedMessageText( "aztorbrowserplugin.status.fail", new String[]{ init_error }) );
			
			log( "Initialization failed: " + init_error );
			
			throw( new PluginException( "Initialisation failed: " + Debug.getNestedExceptionMessage( e )));
		}
	}
	
	private void
	copyProfile(
		File	from_dir,
		File	to_dir )
	
		throws Exception
	{
		if ( !from_dir.isDirectory()){
			
			return;
		}
		
		File[] from_files = from_dir.listFiles();
		
		for ( File from_file: from_files ){
			
			File to_file = new File( to_dir, from_file.getName());
			
			if ( from_file.isDirectory()){
				
				if ( !to_file.exists()){
					
					if ( !to_file.mkdir()){
						
						throw( new Exception( "Failed to create dir: " + to_file ));
					}
				}
				
				copyProfile( from_file, to_file );
				
			}else{
				
				if ( to_file.exists()){
					
					if ( !to_file.delete()){
						
						throw( new Exception( "Failed to delete file: " + to_file ));
					}
				}
				
				if ( !FileUtil.copyFile( from_file, to_file )){
					
					throw( new Exception( "Failed to copy file: " + from_file + " -> " + to_file ));
				}
			}
		}
	}
	
	private IPCInterface
	getTorIPC()
		
		throws Exception
	{
		IPCInterface result = tor_ipc;
		
		if ( result == null ){
			
			PluginInterface tor_pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "aznettor", true );
			
			if ( tor_pi != null ){
				
				result = tor_ipc = tor_pi.getIPC();
				
			}else{
				
				throw( new Exception( "Tor Helper Plugin not installed" ));
			}
		}
		
		return( result );
	}
	
	private void
	checkConfig()
	
		throws Exception
	{
		IPCInterface ipc = getTorIPC();
		
		if ( !ipc.canInvoke( "getConfig", new Object[0] )){
			
			throw( new Exception( "Tor Helper Plugin needs updating" ));
		}
		
		int	socks_port;

		try{
			Map<String,Object>	config = (Map<String,Object>)ipc.invoke( "getConfig", new Object[0] );
		
			socks_port = (Integer)config.get( "socks_port" );
			
		}catch( Throwable e ){
			
			throw( new Exception( "Tor Helper Plugin communication failure", e ));

		}
		
		log( "Tor socks port is " + socks_port );
		
		Map<String,Object> user_pref = new HashMap<String, Object>();
		 
		user_pref.put("browser.startup.homepage", HOME_PAGE );
		user_pref.put("network.proxy.no_proxies_on", "127.0.0.1");
		user_pref.put("network.proxy.socks_port", socks_port );
		
		Set<String>	user_pref_opt = new HashSet<String>();
		
		user_pref_opt.add( "browser.startup.homepage" );
		user_pref_opt.add( "network.proxy.no_proxies_on" );
		
		Map<String,Object> ext_pref = new HashMap<String, Object>();

		ext_pref.put("extensions.torbutton.fresh_install", true );
		ext_pref.put("extensions.torbutton.tor_enabled", true);
		ext_pref.put("extensions.torbutton.proxies_applied", false );
		ext_pref.put("extensions.torbutton.settings_applied", false );
		ext_pref.put("extensions.torbutton.socks_host", "127.0.0.1");
		ext_pref.put("extensions.torbutton.socks_port", socks_port );
		ext_pref.put("extensions.torbutton.custom.socks_host", "127.0.0.1");
		ext_pref.put("extensions.torbutton.custom.socks_port", socks_port );
		ext_pref.put("extensions.torbutton.settings_method", "custom");

		
		File	root = browser_dir;
		
		if ( root == null ){
			
			throw( new Exception( "Browser not installed" ));
		}
		
		char slash = File.separatorChar;
		
		File	profile_dir = new File( root, "Data" + slash + "Browser" + slash + "profile.default" );
		
		File	user_prefs_file = new File( profile_dir, "prefs.js" );
		
		fixPrefs( user_prefs_file, "user_pref", user_pref, user_pref_opt );
		
		File	ext_prefs_file = new File( profile_dir, "preferences" + slash + "extension-overrides.js" );
		
		fixPrefs( ext_prefs_file, "pref", ext_pref, new HashSet<String>() );
	}
	
	private void
	fixPrefs(
		File				file,
		String				pref_key,
		Map<String,Object>	prefs,
		Set<String>			optional_keys )
	
		throws Exception
	{
		List<String>	lines = new ArrayList<String>();
		
		boolean updated = false;
		
		Map<String,Object>	prefs_to_add = new HashMap<String, Object>( prefs );
		
		if ( file.exists()){
			
			LineNumberReader	lnr = null;
			
			try{
				lnr = new LineNumberReader( new InputStreamReader( new FileInputStream( file ), "UTF-8" ));
				
				while( true ){
					
					String 	line = lnr.readLine();
					
					if ( line == null ){
						
						break;
					}
					
					line = line.trim();
					
					boolean	handled = false;
					
					if ( line.startsWith( pref_key )){
						
						int	pos1 = line.indexOf( "\"" );
						int	pos2 = line.indexOf( "\"", pos1+1 );
						
						if ( pos2 > pos1 ){
							
							String key = line.substring( pos1+1, pos2 );
							
							Object	required_value = prefs_to_add.remove( key );
							
							if ( required_value != null ){
								
								pos1 = line.indexOf( ",", pos2 + 1 );
								pos2 = line.indexOf( ")", pos1+1 );
								
								if ( pos2 > pos1 ){
									
									String	current_str = line.substring( pos1+1, pos2 ).trim();
									
									String 	required_str;
									
									if ( required_value instanceof String ){
										
										required_str = "\"" + required_value + "\"";
									}else{
										
										required_str = String.valueOf( required_value );
									}
									
									if ( !current_str.equals( required_str )){
										
										lines.add( pref_key + "(\"" + key + "\", " + required_str + ");");
										
										updated	= true;
										handled = true;
									}
								}else{
									
									throw( new Exception( "Couldn't parse line: " + line ));
								}
							}
						}
					}
					
					if ( !handled ){
						
						lines.add( line );
					}
				}
			}finally{
				
				if ( lnr != null ){
					
					lnr.close();
				}
			}
		}
		
		for ( Map.Entry<String, Object> entry: prefs_to_add.entrySet()){
			
			String key = entry.getKey();
			
				// if all we are missing is optional keys then don't flag as updated (might be flagged
				// so due to other things of course)
				// we're dealing with the case that some settings are used-and-removed from the pref
				// config when firefox runs and we don't want to keep re-writing them
			
			if ( !optional_keys.contains( key )){
				
				updated = true;
			}
			
			Object val = entry.getValue();
			
			if ( val instanceof String ){
				
				val = "\"" + val + "\"";
			}
			
			lines.add( pref_key + "(\"" + key + "\", " + val + ");");
		}
		
		if ( updated ){
			
			File temp_file 	= new File( file.getAbsolutePath() + ".tmp" );
			File bak_file 	= new File( file.getAbsolutePath() + ".bak" );
			
			temp_file.delete();
			
			try{
				PrintWriter writer = new PrintWriter( new OutputStreamWriter( new FileOutputStream( temp_file ), "UTF-8" ));
				
				try{
					for ( String line: lines ){
					
						writer.println( line );
					}
				}finally{
					
					writer.close();
				}
				
				bak_file.delete();
				
				if ( file.exists()){
					
					if ( !file.renameTo( bak_file )){
						
						throw( new Exception( "Rename of " + file + " to " + bak_file + " failed" ));
					}
				}
					
				if ( !temp_file.renameTo( file )){
						
					if ( bak_file.exists()){
						
						bak_file.renameTo( file );
					}
					
					throw( new Exception( "Rename of " + temp_file + " to " + file + " failed" ));
				}
			}catch( Throwable e ){
				
				temp_file.delete();
				
				throw( new Exception( "Failed to udpate " + file, e ));
			}	
		}
	}
	
	
	private void
	launchBrowser(
		final String		url,
		final boolean		new_window,
		final Runnable		run_when_done )
	
		throws Exception
	{
		log( "Launch request for " + (url==null?"<default>":url) + ", new window=" + new_window );
		
		if ( init_error != null ){
			
			throw( new Exception( "Browser initialisation failed: " + init_error ));
		}

		final File	root = browser_dir;

		if ( root == null ){
						
			throw( new Exception( "Browser not installed" ));
		}
		
		launch_dispatcher.dispatch(
			new AERunnable()
			{
				public void 
				runSupport() 
				{
					try{
						launchBrowserSupport( root, url, new_window, run_when_done );
						
					}catch( Throwable e ){
						
						log( "Launch failed: " + Debug.getNestedExceptionMessage( e ));
					}
				}
			});
	}
	
	private void
	launchBrowserSupport(
		File			root,
		String			url,
		boolean			new_window,
		Runnable		run_when_done ) 
	
		throws Exception
	{
		try{
			long	now = SystemTime.getMonotonousTime();
			
			while( true ){
				
				if ( checkTor()){
					
					launch_timeout	= LAUNCH_TIMEOUT_INIT;
					
					break;
				}
			
				if ( SystemTime.getMonotonousTime() - now > launch_timeout ){
				
					log( "Timeout waiting for Tor to start" );
					
					launch_timeout	= LAUNCH_TIMEOUT_NEXT;
					
					break;
				}
				
				try{
					Thread.sleep( 1000 );
					
				}catch( Throwable e ){
					
				}
			}
			
			List<String>	cmd_list = new ArrayList<String>();
		
			String	browser_root = root.getAbsolutePath();
			
			String slash = File.separator;
			
			if ( Constants.isWindows ){
		
				cmd_list.add( browser_root + slash + "Browser" + slash + "firefox.exe" );
				
				cmd_list.add( "-profile" );
				
				cmd_list.add( browser_root + slash + "Data" + slash + "Browser" + slash + "profile.default" + slash );
				
			}else if ( Constants.isOSX ){
				
				cmd_list.add( browser_root + slash + "TorBrowser.app" + slash + "Contents" + slash + "MacOS" + slash + "firefox" );
				
				cmd_list.add( "-profile" );
				
				cmd_list.add( browser_root + slash + "Data" + slash + "Browser" + slash + "profile.default" );
	
			}else{
				
				throw( new Exception( "Unsupported OS" ));
			}
			
			if ( url == null ){
			
			}else{
				
				if ( new_window ){
				
					cmd_list.add( "-new-window"  );
				}
		
				cmd_list.add( url );
			}
			
			ProcessBuilder pb = GeneralUtils.createProcessBuilder( root, cmd_list.toArray(new String[cmd_list.size()]), null );
			
			if ( Constants.isOSX ){
				
				pb.environment().put(
					"DYLD_LIBRARY_PATH",
					browser_root + slash + "TorBrowser.app" + slash + "Contents" + slash + "MacOS" );
			}
					
			new BrowserInstance( pb );	
			
		}finally{
			
			if ( run_when_done != null ){
				
				run_when_done.run();
			}
		}
	}
	
	private String
	findCommand(
		String	name )
	{
		final String[]  locations = { "/bin", "/usr/bin" };

		for ( String s: locations ){

			File f = new File( s, name );

			if ( f.exists() && f.canRead()){

				return( f.getAbsolutePath());
			}
		}

		return( name );
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
			
		throws PluginException 
	{
		synchronized( browser_instances ){
			
			if ( browser_instances.size() > 0 ){
				
				throw( new PluginException( "Unload prevented as browsers are active" ));
			}
		}
		
		browser_dir 		= null;
		init_error			= null;
		plugin_interface	= null;
		
		if ( config_model != null ){
			
			config_model.destroy();
			
			config_model = null;
		}
		
		if ( view_model != null ){
			
			view_model.destroy();
			
			view_model = null;
		}
		
			// should be null op due to above test, but leave here for completeness
		
		killBrowsers();
	}
	
	private void
	killBrowsers()
	{
		final AESemaphore sem = new AESemaphore( "waiter" );
		
			// just in case something blocks here...
		
		new AEThread2( "killer")
		{
			public void
			run()
			{
				try{
					synchronized( browser_instances ){
						
						for ( BrowserInstance b: browser_instances ){
							
							b.destroy();
						}
						
						browser_instances.clear();
						
						setUnloadable( true );
						
						if ( browser_timer != null ){
							
							browser_timer.cancel();
							
							browser_timer = null;
						}
					}
				}finally{
					
					sem.release();
				}
			}
		}.start();
		
		sem.reserve( 2500 );
	}
	
	private boolean
	checkTor()
	{
		try{
			IPCInterface ipc = getTorIPC();
			
			if ( !ipc.canInvoke( "requestActivation", new Object[0] )){
				
				return( false );
			}
			
			return( (Boolean)ipc.invoke( "requestActivation", new Object[0] ));
			
		}catch( Throwable e ){
			
			return( false );
		}
	}
	
	private void
	checkBrowsers()
	{
		int	num_active;
		
		synchronized( browser_instances ){
		
			num_active = browser_instances.size();
			
			if ( num_active == 0 ){
			
				if ( browser_timer != null ){
				
					browser_timer.cancel();
				
					browser_timer = null;
				}
				
				
				return;
			}
		}
		
		if ( num_active > 0 ){
		
			checkTor();
		}
		
		String str = "Actve browsers: " + num_active;
		
		if ( !last_check_log.equals( str )){
			
			log( str );
			
			last_check_log = str;
		}
	}
	
	private Set<Integer>
	getProcesses(
		String	exe )
	{
		Set<Integer>	result = new HashSet<Integer>();
		
		try{
			
			Process p = Runtime.getRuntime().exec( new String[]{ "cmd", "/c", "tasklist" });
			
			try{
				LineNumberReader lnr = new LineNumberReader( new InputStreamReader( p.getInputStream(), "UTF-8" ));
				
				while( true ){
					
					String line = lnr.readLine();
					
					if ( line == null ){
						
						break;
					}
					
					if ( line.startsWith( exe )){
						
						String[] bits = line.split( "\\s+" );
						
						if ( bits.length >= 2 ){
							
							String	exe_name 	= bits[0].trim();
							int		pid 		= Integer.parseInt( bits[1].trim());
							
							if ( exe_name.equals( exe )){
								
								result.add( pid );
							}
						}
					}
				}
					
			}catch( Throwable e ){
				
			}finally{
				
				p.destroy();
			}
		}catch( Throwable e ){
			
		}
		
		return( result );
	}
	
		// IPC methods
	
	public void
	launchURL(
		URL			url )
	
		throws IPCException
	{
		launchURL( url, false );
	}
	
	public void
	launchURL(
		URL			url,
		boolean		new_window )
	
		throws IPCException
	{
		try{
			launchBrowser( url==null?null:url.toExternalForm(), new_window, null );
			
		}catch( Throwable e ){
			
			throw( new IPCException( "Launch url failed", e ));
		}
	}
	
	private void
	log(
		String		str )
	{
		log.log( str );
	}
	
	private class
	BrowserInstance
	{
		private Process		process;
		private int			process_id	= -1;
		
		private List<AEThread2>	threads = new ArrayList<AEThread2>();
		
		private volatile boolean	destroyed;
		
		private
		BrowserInstance(
			ProcessBuilder		pb )
		
			throws IOException
		{		
			if ( Constants.isWindows ){
				
					// process.destroy doesn't work on Windows :( - rumour is it sends a SIG_TERM which is ignored
				
				Set<Integer>	pre_procs = getProcesses( "firefox.exe" );
			
				process = pb.start();
				
				Set<Integer>	post_procs = getProcesses( "firefox.exe" );
				
				for ( Integer s: pre_procs ){
					
					post_procs.remove( s );
				}
				
				if ( post_procs.size() == 1 ){
					
					process_id = post_procs.iterator().next();
				}
			}else{
				
				process = pb.start();
			}
					
						
			try{
				synchronized( browser_instances ){
					
					browser_instances.add( this );
					
					setUnloadable( false );
					
					if ( browser_timer == null ){
						
						browser_timer = 
							SimpleTimer.addPeriodicEvent(
								"TBChecker",
								30*1000,
								new TimerEventPerformer()
								{	
									public void 
									perform(
										TimerEvent event) 
									{
										checkBrowsers();
									}
								});
					}
				}

				if ( browser_dir == null ){
					
					throw( new Exception( "Unloaded" ));
				}
				
				AEThread2 thread =
					new AEThread2( "TorBrowser:proc_read_out" )
					{
						public void
						run()
						{
							try{
								LineNumberReader lnr = new LineNumberReader( new InputStreamReader( process.getInputStream()));
								
								while( true ){
								
									String line = lnr.readLine();
									
									if ( line == null ){
										
										break;
									}
								
									System.out.println( "> " + line );
								}
							}catch( Throwable e ){
								
							}
						}
					};
					
				threads.add( thread );
				
				thread.start();
				
				thread =
					new AEThread2( "TorBrowser:proc_read_err" )
					{
						public void
						run()
						{
							try{
								LineNumberReader lnr = new LineNumberReader( new InputStreamReader( process.getErrorStream()));
								
								while( true ){
								
									String line = lnr.readLine();
									
									if ( line == null ){
										
										break;
									}
								
									System.out.println( "*" + line );
								}
							}catch( Throwable e ){
								
							}
						}
					};
					
				threads.add( thread );
					
				thread.start();

				thread =
					new AEThread2( "TorBrowser:proc_wait" )
					{
						public void
						run()
						{
							try{
								process.waitFor();
								
							}catch( Throwable e ){
								
							}finally{
								
								synchronized( browser_instances ){
									
									browser_instances.remove( BrowserInstance.this );
									
									setUnloadable( browser_instances.size() == 0 );
								}
							}
						}
					};
					
				threads.add( thread );
					
				thread.start();	
				
			}catch( Throwable e ){
				
				synchronized( browser_instances ){
					
					browser_instances.remove( this );
					
					setUnloadable( browser_instances.size() == 0 );
				}
				
				destroy();
			}
		}
		
		private void
		destroy()
		{
			destroyed = true;
			
			try{	
				for ( AEThread2 thread: threads ){
					
					thread.interrupt();
				}
				
				process.getOutputStream().close();
				
				process.destroy();
				
				if ( Constants.isWindows && process_id >= 0 ){
					
					Process p = Runtime.getRuntime().exec( new String[]{ "cmd", "/c", "taskkill", "/f", "/pid", String.valueOf( process_id ) });

					p.waitFor();
				}
			}catch( Throwable e ){
				
			}
		}
	}
}
