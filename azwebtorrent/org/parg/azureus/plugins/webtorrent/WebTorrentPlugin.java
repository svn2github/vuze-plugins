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



import java.net.URL;



import org.gudy.azureus2.core3.util.Debug;
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
		
	private BrowserManager	browser_manager = new BrowserManager();
	
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
		
		plugin_interface.addListener(
			new PluginAdapter()
			{
				public void 
				closedownInitiated() 
				{
					browser_manager.killBrowsers();
				}
			});
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
	launchBrowser(
		Runnable		callback )
	{
		String	url = "http://127.0.0.1:" + web_server.getPort() + "/index.html?id=" + instance_id;
	
		browser_manager.launchBrowser( this, url, callback );
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
		
		browser_manager.killBrowsers();
	}
	
	protected void
	setStatusError(
		Throwable	e )
	{
		String init_error = Debug.getNestedExceptionMessage( e );
		
		status_label.setLabelText( loc_utils.getLocalisedMessageText( "azwebtorrent.status.fail", new String[]{ init_error }) );
		
		log( init_error );	
	}
	
	protected PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected void
	log(
		String		str )
	{
		log.log( str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		log.log( str, e );
	}
}
