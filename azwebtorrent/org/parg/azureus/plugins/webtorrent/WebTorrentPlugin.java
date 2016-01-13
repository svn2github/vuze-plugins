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


import java.io.File;
import java.net.URL;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;


import com.aelitis.azureus.core.util.GeneralUtils;


public class 
WebTorrentPlugin 
	implements UnloadablePlugin
{
	private static final long instance_id = RandomUtils.nextSecureAbsoluteLong();
		
	private PluginInterface	plugin_interface;
	
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	

	private LocalWebServer	web_server;
	private TrackerProxy	tracker_proxy;
	private JavaScriptProxy	js_proxy;
		
	private Object			browser_lock = new Object();
	
	private Process			browser_process;
	
	private boolean			unloaded;
	
	@Override
	public void 
	initialize(
		PluginInterface _plugin_interface )
		
		throws PluginException 
	{
		plugin_interface = _plugin_interface;
		
		final LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
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

		final ActionParameter browser_launch_param = config_model.addActionParameter2( "azwebtorrent.browser.launch", "azwebtorrent.browser.launch.button" );
		
		browser_launch_param.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					browser_launch_param.setEnabled( false );
					
					launchBrowser(
						new Runnable()
						{
							@Override
							public void run() {
								browser_launch_param.setEnabled( true );
							}
						});
				}
			});
					
		try{
			js_proxy = JavaScriptProxyManager.getProxy( instance_id );

			tracker_proxy = 
				new TrackerProxy(
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
										
			launchBrowser( null );
		
		}catch( Throwable e ){
			
			Debug.out( e );
		}
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
					
					try{
						File plugin_install_dir = new File( plugin_interface.getPluginDirectoryName());
	
						// Ideally we'd do what we used to:
						//     config_file = pi.getPluginconfig().getPluginUserFile( "config.txt" );
						// so we always get a writable location even if plugin installed into shared space. However,
						// the way things currently work we have to have the executable in the same location so for 
						// the moment I'm fixing by assuming we can write to wherever the plugin is installed.
						// This issue came to light on Linux where the bundled plugins are installed into the
						// shared plugin location...
						
						File config_file = new File( plugin_install_dir, "config.txt" );
						
						File plugin_dir 	= config_file.getParentFile();
	
						File data_dir 		= new File( plugin_dir, "data" );
						File browser_dir 	= new File( plugin_dir, "browser" );
	
						data_dir.mkdirs();
						browser_dir.mkdirs();
						
						String	url = "http://127.0.0.1:" + web_server.getPort() + "/index.html?id=" + instance_id;
						
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
						
						Debug.out( e );
						
					}finally{
						
						if ( callback != null ){
							
							callback.run();
						}
					}
				}
			}
		}.start();
	}
	public URL
	getProxyURL(
		URL		url )
		
		throws IPCException
	{
		if ( web_server == null || js_proxy == null || unloaded ){
			
			throw( new IPCException( "Proxy unavailable" ));
		}
		
		try{
			return( new URL( "http://127.0.0.1:" + web_server.getPort() + "/?target=" + UrlUtils.encode( url.toExternalForm())));
			
		}catch( Throwable e ){
			
			throw( new IPCException( e ));
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
		
		if ( js_proxy != null ){
			
			js_proxy.destroy();
			
			js_proxy = null;
		}
	
		if ( tracker_proxy != null ){
			
			tracker_proxy.destroy();
			
			tracker_proxy = null;
		}
		
		if ( web_server != null ){
			
			web_server.destroy();
			
			web_server = null;
		}
	}
}
