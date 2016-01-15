/*
 * Created on Dec 18, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package org.parg.azureus.plugins.webtorrent;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ipc.IPCException;
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








import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.util.GeneralUtils;


public class 
WebTorrentPlugin 
	implements UnloadablePlugin
{
	private static final long instance_id = RandomUtils.nextSecureAbsoluteLong();
		
	private PluginInterface	plugin_interface;
	
	private LoggerChannel 			log;
	private LocaleUtilities 		loc_utils;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	
	private LabelParameter 			status_label;
	

	private LocalWebServer	web_server;
	private TrackerProxy	tracker_proxy;
	private JavaScriptProxy	js_proxy;
		
	private Object			browser_lock = new Object();
	private Process			browser_process;
	
	private Object			active_lock	= new Object();
	private boolean			active;
	private long			last_activation_attempt;
	
	private boolean			unloaded;
	
	@Override
	public void 
	initialize(
		PluginInterface _plugin_interface )
		
		throws PluginException 
	{
		plugin_interface = _plugin_interface;
		
		loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		log	= plugin_interface.getLogger().getTimeStampedChannel( "WebTorrent");
		
		final UIManager	ui_manager = plugin_interface.getUIManager();

		view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azwebtorrent.name" ));

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
		
		config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azwebtorrent.name" );

		view_model.setConfigSectionID( "azwebtorrent.name" );

		
		
		config_model.addLabelParameter2( "azwebtorrent.info" );

		config_model.addLabelParameter2( "azwebtorrent.blank" );

		config_model.addHyperlinkParameter2( "azwebtorrent.link", loc_utils.getLocalisedMessageText( "azwebtorrent.link.url" ));
		
		config_model.addLabelParameter2( "azwebtorrent.blank" );

		
	
		status_label = config_model.addLabelParameter2( "azwebtorrent.status");

		config_model.addLabelParameter2( "azwebtorrent.browser.info" );

		final ActionParameter browser_launch_param = config_model.addActionParameter2( "azwebtorrent.browser.launch", "azwebtorrent.browser.launch.button" );
		
		browser_launch_param.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					browser_launch_param.setEnabled( false );
					
					Runnable cb = new Runnable()
					{
						@Override
						public void run() 
						{
							browser_launch_param.setEnabled( true );
						}
					};
					
					if ( active ){
						
						launchBrowser( cb );
								
					}else{
						
						activate( cb );
					}
				}
			});
	}
	
	private File
	checkBrowserInstall()
	
		throws PluginException
	{
		try{
			File plugin_install_dir = new File( plugin_interface.getPluginDirectoryName());
				
			File plugin_data_dir	= plugin_interface.getPluginconfig().getPluginUserFile( "test" ).getParentFile();
			
			if ( !plugin_data_dir.exists()){
				
				plugin_data_dir.mkdirs();
			}
			
			deleteOldStuff( plugin_install_dir );
			deleteOldStuff( plugin_data_dir );
			
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
								
				if ( Constants.isOSX || Constants.isLinux ){
					
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
				
				return( target_data );

			}else{
				
				File existing_data = new File( plugin_data_dir, "browser_" + highest_version_data );

				if ( highest_version_data.equals( "0" ) || !existing_data.exists()){
					
					throw( new Exception( "No browser version installed" ));
				}
								
				return( existing_data );
			}
					
		}catch( Throwable e ){
			
			String init_error = Debug.getNestedExceptionMessage( e );
			
			status_label.setLabelText( loc_utils.getLocalisedMessageText( "azwebtorrent.status.fail", new String[]{ init_error }) );
			
			log( "Browser setup failed: " + init_error );
			
			throw( new PluginException( "Browser setup failed: " + Debug.getNestedExceptionMessage( e )));
		}
	}
		
	private void
	deleteOldStuff(
		File		dir )
	{
		File[] files = dir.listFiles();
		
		if ( files == null || files.length == 0 ){
			
			return;
		}
		
		Map<String,List<Object[]>>	map = new HashMap<String,List<Object[]>>();
		
		for ( File f: files ){
			
			String name = f.getName();
			
			int	pos = name.lastIndexOf( '_' );
			
			if ( pos == -1 ){
				
				continue;
			}
			
			String root		= name.substring( 0, pos );
			String ver_str 	= name.substring( pos+1 );
			
			if ( ver_str.endsWith( ".jar" ) || ver_str.endsWith( ".zip" )){
				
				root += ver_str.substring(  ver_str.length() - 4 );
				
				ver_str = ver_str.substring( 0, ver_str.length() - 4 );
			}
			
			for ( char c: ver_str.toCharArray()){
				
				if ( c != '.' && !Character.isDigit( c )){
					
					ver_str = null;
					
					break;
				}
			}
			
			if ( ver_str != null && ver_str.length() > 0 ){
				
				List<Object[]> entry = map.get( root );
				
				if ( entry == null ){
					
					entry = new ArrayList<Object[]>();
					
					map.put( root, entry );
				}
				
				entry.add( new Object[]{ ver_str, f });
			}
		}
		
		for ( Map.Entry<String,List<Object[]>> entry: map.entrySet()){
			
			String 			root 	= entry.getKey();
			List<Object[]>	list	= entry.getValue();
			
			Collections.sort(
				list,
				new Comparator<Object[]>()
				{
					public int 
					compare(
						Object[] e1, 
						Object[] e2) 
					{
						String ver1 = (String)e1[0];
						String ver2 = (String)e2[0];
						
						return( Constants.compareVersions( ver1, ver2 ));
					}
				});
			
			/*
			System.out.println( root );
			
			for ( Object[] o: list ){
				
				System.out.println( "    " + o[0] + " - " + o[1] );
			}
			*/
			
			int	ver_to_delete = list.size() - 3;
							
			for ( int i=0;i<ver_to_delete;i++ ){
				
				File f = (File)list.get(i)[1];
				
				delete( f );
			}
		}
	}
	
	private void
	delete(
		File		f )
	{
		if ( f.isDirectory()){
						
			File[] files = f.listFiles();
			
			if ( files != null ){
				
				for ( File x: files ){
					
					delete( x );
				}
			}
		}
		
		f.delete();
	}
		
	private void
	launchBrowser(
		final Runnable		callback )
	{
		new AEThread2( "")
		{
			public void
			run()
			{
				synchronized( browser_lock ){
										
					if ( browser_process != null ){
						
						try{
							browser_process.destroy();
							
						}catch( Throwable e ){
						}
					}
					
					String	url = "http://127.0.0.1:" + web_server.getPort() + "/index.html?id=" + instance_id;

					try{
						if ( !Constants.isWindows ){
							
							Utils.launch( new URL( url ));
							
							return;
						}
						
						File browser_dir = checkBrowserInstall();
						
						File plugin_dir = plugin_interface.getPluginconfig().getPluginUserFile( "test" ).getParentFile();
						
						File data_dir 		= new File( plugin_dir, "data" );
		
						data_dir.mkdirs();
						browser_dir.mkdirs();
											
						String[] args = {
							"chrome.exe",
							"--window-size=600,600",
							"--incognito",
							"--no-default-browser-check",
							"--no-first-run",
							"--user-data-dir=\"" + data_dir.getAbsolutePath() + "\"",
							url };
						
						ProcessBuilder pb = GeneralUtils.createProcessBuilder( browser_dir, args, null );
	
						browser_process = pb.start();
												
					}catch( Throwable e ){
						
						log( "Failed to launch browser", e );
						
					}finally{
						
						if ( callback != null ){
							
							callback.run();
						}
					}
				}
			}
		}.start();
	}
	
	private boolean
	activate(
		Runnable	callback )
	{
		boolean	async = false;
		
		try{
			synchronized( active_lock ){
				
				if ( active ){
					
					return( true );
				}
				
				long	now = SystemTime.getMonotonousTime();
				
				if ( last_activation_attempt != 0 && now - last_activation_attempt < 30*1000 ){
					
					return( false );
				}
				
				last_activation_attempt = now;
				
				log( "Activating" );
				
				try{
					js_proxy = JavaScriptProxyManager.getProxy( instance_id );
		
					tracker_proxy = 
						new TrackerProxy(
							this,
							new TrackerProxy.Listener()
							{
						    	public JavaScriptProxy.Offer
						    	getOffer(
						    		byte[]		hash,
						    		long		timeout )
						    	{
						    		return( js_proxy.getOffer( hash, timeout ));
						    	}
						    	
						    	public void
						    	gotAnswer(
						    		byte[]		hash,
						    		String		offer_id,
						    		String		sdp )
						    		
						    		throws Exception
						    	{
						    		js_proxy.gotAnswer( offer_id, sdp );
						    	}
						    	
						    	public void
						    	gotOffer(
						    		byte[]							hash,
						    		String							offer_id,
						    		String							sdp,
						    		JavaScriptProxy.AnswerListener 	listener )
						    		
						    		throws Exception
						    	{
						    		js_proxy.gotOffer( hash, offer_id, sdp, listener );
						    	}
							});
					
					web_server = new LocalWebServer( instance_id, js_proxy.getPort(), tracker_proxy );
						
					status_label.setLabelText( loc_utils.getLocalisedMessageText( "azwebtorrent.status.ok" ));
	
					launchBrowser( callback );
				
					async = true;
					
					active = true;
					
					return( true );
					
				}catch( Throwable e ){
					
					status_label.setLabelText( loc_utils.getLocalisedMessageText( "azwebtorrent.status.fail", new String[]{ Debug.getNestedExceptionMessage(e) }));
	
					active = false;
					
					log( "Activation failed", e );
					
					deactivate();
					
					return( false );
				}
			}
		}finally{
			
			if ( !async && callback != null ){
				
				callback.run();
			}
		}
	}
	
	private void
	deactivate()
	{
		synchronized( active_lock ){
			
			active = false;
			
			if ( web_server != null ){
				
				try{
					
					web_server.destroy();
					
				}catch( Throwable f ){
					
				}
				
				web_server = null;
			}
			
			if ( tracker_proxy != null ){
				
				try{
					
					tracker_proxy.destroy();
					
				}catch( Throwable f ){
					
				}
				
				tracker_proxy = null;
			}
			
			if ( js_proxy != null ){
				
				try{
					
					js_proxy.destroy();
					
				}catch( Throwable f ){
					
				}
				
				js_proxy = null;
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
	
	public URL
	getProxyURL(
		URL		url )
		
		throws IPCException
	{
		if ( unloaded ){
			
			throw( new IPCException( "Proxy unavailable: plugin unloaded" ));
		}
		
		if ( activate( null )){
				
			try{
				return( new URL( "http://127.0.0.1:" + web_server.getPort() + "/?target=" + UrlUtils.encode( url.toExternalForm())));
				
			}catch( Throwable e ){
				
				throw( new IPCException( e ));
			}
		}else{
			
			throw( new IPCException( "Proxy unavailable" ));
		}
	}
	
	public void
	unload()
	{
		unloaded	= true;
		
		if ( config_model != null ){
			
			config_model.destroy();
			
			config_model = null;
		}
		
		if ( view_model != null ){
			
			view_model.destroy();
			
			view_model = null;
		}
		
		deactivate();
	}
	
	private void
	log(
		String		str )
	{
		log.log( str );
	}
	
	private void
	log(
		String		str,
		Throwable	e )
	{
		log.log( str, e );
	}
}
