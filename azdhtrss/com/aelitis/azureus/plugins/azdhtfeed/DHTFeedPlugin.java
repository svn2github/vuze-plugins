/*
 * Created on 03-May-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.azdhtfeed;

import java.io.*;

import java.net.URL;
import java.net.URLEncoder;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;


import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;



public class 
DHTFeedPlugin 
	implements Plugin
{	
	private final static int		DEFAULT_PORT = 6887;
	
	private final static int		AUTO_REPUBLISH_DEFAULT	= 60;
	private final static int		AUTO_REPUBLISH_MIN		= 15;
	
	private final static int		AUTO_SUBSCRIBE_DEFAULT	= 60;
	private final static int		AUTO_SUBSCRIBE_MIN		= 15;

	protected final static String		CATEGORY_FEED_DESC		= "Feed Desc";
	protected final static String		CATEGORY_FEED_CONTENT	= "Feed Content";
	
	protected final static String		TORRENT_CONTENT_TYPE_PROPERTY = "Content-Type";

	private PluginInterface		plugin_interface;
	private String				plugin_name; 

	private TorrentAttribute	ta_category;
	
	private DHTFeedPluginSubscriber 	subscriber;
	private DHTFeedPluginPublisher		publisher;

	private LoggerChannel		log;
			
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel( "DDB Feed" );
				
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "com.aelitis.azureus.plugins.azdhtfeed.internat.Messages");
		
		plugin_name = plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "azdhtfeed.name" );
		
		ta_category		= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_CATEGORY );
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azdhtfeed.name");

		final BooleanParameter 	subscribe_enable 		= config_model.addBooleanParameter2( "subscribe.enable", "azdhtfeed.subscribe.enable", true );
		
		LabelParameter	subscribe_info = config_model.addLabelParameter2( "azdhtfeed.subscribe.info" );
		
		final StringParameter	subscribe_name			= config_model.addStringParameter2( "subscribe.name", "azdhtfeed.subscribe.name", "" );
		
		final IntParameter		subscribe_port 			= config_model.addIntParameter2( "subscribe.port", "azdhtfeed.subscribe.port", DEFAULT_PORT );
		final BooleanParameter	subscribe_port_local	= config_model.addBooleanParameter2( "subscribe.port.local", "azdhtfeed.subscribe.port.local", true );
		
		final ActionParameter	subscribe_button			= config_model.addActionParameter2( "azdhtfeed.subscribe.button.info", "azdhtfeed.subscribe.button" );

		final ActionParameter	unsubscribe_button			= config_model.addActionParameter2( "azdhtfeed.unsubscribe.button.info", "azdhtfeed.unsubscribe.button" );
		final ActionParameter	subscribe_refresh_button	= config_model.addActionParameter2( "azdhtfeed.subscriberefresh.button.info", "azdhtfeed.subscriberefresh.button" );
		final IntParameter		subscribe_refresh			= config_model.addIntParameter2( "subscribe.refresh", "azdhtfeed.subscriberefresh", AUTO_SUBSCRIBE_DEFAULT );

		subscribe_refresh.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						if ( subscribe_refresh.getValue() < AUTO_SUBSCRIBE_MIN ){
							
							subscribe_refresh.setValue( AUTO_SUBSCRIBE_MIN );
						}
					}
				});
			
		if ( subscribe_refresh.getValue() < AUTO_SUBSCRIBE_MIN ){
				
			subscribe_refresh.setValue( AUTO_SUBSCRIBE_MIN );
		}
			
		config_model.createGroup( 
				"azdhtfeed.subscribe.group", 
				new Parameter[]{ 
						subscribe_info,
						subscribe_name, 
						subscribe_port, 
						subscribe_port_local,
						subscribe_button,
						unsubscribe_button,
						subscribe_refresh_button,
						subscribe_refresh});
		
		subscribe_enable.addEnabledOnSelection( subscribe_info );
		subscribe_enable.addEnabledOnSelection( subscribe_name );
		subscribe_enable.addEnabledOnSelection( subscribe_port );
		subscribe_enable.addEnabledOnSelection( subscribe_port_local );
		subscribe_enable.addEnabledOnSelection( subscribe_button );
		subscribe_enable.addEnabledOnSelection( unsubscribe_button );
		subscribe_enable.addEnabledOnSelection( subscribe_refresh_button );
		subscribe_enable.addEnabledOnSelection( subscribe_refresh );

		
		final BooleanParameter 	publish_enable 		= config_model.addBooleanParameter2( "publish.enable", "azdhtfeed.publish.enable", false );
		
		LabelParameter	publish_info = config_model.addLabelParameter2( "azdhtfeed.publish.info" );

		final StringParameter	publish_name		= config_model.addStringParameter2( "publish.name", "azdhtfeed.publish.name", "" );
		
		final StringParameter	publish_location	= config_model.addStringParameter2( "publish.location", "azdhtfeed.publish.location", "" );

		final ActionParameter	publish_button		= config_model.addActionParameter2( "azdhtfeed.publish.button.info", "azdhtfeed.publish.button" );
		final ActionParameter	unpublish_button	= config_model.addActionParameter2( "azdhtfeed.unpublish.button.info", "azdhtfeed.unpublish.button" );
		final ActionParameter	republish_button	= config_model.addActionParameter2( "azdhtfeed.republish.button.info", "azdhtfeed.republish.button" );
		final IntParameter		auto_republish		= config_model.addIntParameter2( "publish.auto", "azdhtfeed.autorepublish", AUTO_REPUBLISH_DEFAULT );
		
		final BooleanParameter 	keyregen_enable 	= config_model.addBooleanParameter2( "keyregen.enable", "azdhtfeed.keyregen.enable", false );
		final ActionParameter	keyregen_button		= config_model.addActionParameter2( "azdhtfeed.keyregen.button.info", "azdhtfeed.keyregen.button" );

		keyregen_enable.setValue( false );
		keyregen_button.setEnabled( false );
		
		ParameterListener	keyregen_listener =
			new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						keyregen_button.setEnabled( 
							publish_enable.getValue() &&
							keyregen_enable.getValue());
					}
				};
		
		keyregen_enable.addListener( keyregen_listener );
		publish_enable.addListener( keyregen_listener );
		
		auto_republish.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					if ( auto_republish.getValue() < AUTO_REPUBLISH_MIN ){
						
						auto_republish.setValue( AUTO_REPUBLISH_MIN );
					}
				}
			});
		
		if ( auto_republish.getValue() < AUTO_REPUBLISH_MIN ){
			
			auto_republish.setValue( AUTO_REPUBLISH_MIN );
		}
		
		config_model.createGroup( 
				"azdhtfeed.publish.group", 
				new Parameter[]{ 
						publish_info,
						publish_name, 
						publish_location, 
						publish_button,
						unpublish_button,
						republish_button,
						auto_republish,
						keyregen_enable,
						keyregen_button });

		publish_enable.addEnabledOnSelection( publish_info );
		publish_enable.addEnabledOnSelection( publish_name );
		publish_enable.addEnabledOnSelection( publish_location );
		publish_enable.addEnabledOnSelection( publish_button );
		publish_enable.addEnabledOnSelection( unpublish_button );
		publish_enable.addEnabledOnSelection( republish_button );
		publish_enable.addEnabledOnSelection( auto_republish );
		publish_enable.addEnabledOnSelection( keyregen_enable );
		
		final BasicPluginViewModel model = 
			plugin_interface.getUIManager().createBasicPluginViewModel( "DDB Feed" );
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );

		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						if  ( str != null && str.length() > 0 ){
							
							model.getLogArea().appendText( str +"\n");
						}
						
						StringWriter	sw = new StringWriter();
						PrintWriter		pw = new PrintWriter(sw);
						
						error.printStackTrace( pw );
						
						pw.flush();
						
						model.getLogArea().appendText( sw.toString());
					}
				});
		
		subscriber 	= new DHTFeedPluginSubscriber( this, plugin_interface );
		publisher	= new DHTFeedPluginPublisher( this, plugin_interface );

		MenuItemFillListener	menu_fill_listener = 
			new MenuItemFillListener()
			{
				public void
				menuWillBeShown(
					MenuItem	menu,
					Object		_target )
				{
					Object	obj = null;
					
					if ( _target instanceof TableRow ){
						
						obj = ((TableRow)_target).getDataSource();
	
					}else{
						
						TableRow[] rows = (TableRow[])_target;
					     
						if ( rows.length > 0 ){
						
							obj = rows[0].getDataSource();
						}
					}
					
					if ( obj == null ){
						
						menu.setEnabled( false );

						return;
					}
					
					Download				download;
					
					if ( obj instanceof Download ){
					
						download = (Download)obj;

					}else{
						
						DiskManagerFileInfo file = (DiskManagerFileInfo)obj;
						
						try{
							download	= file.getDownload();
							
						}catch( DownloadException e ){	
							
							Debug.printStackTrace(e);
							
							return;
						}
					}
					
					if ( download.getTorrent() == null ){
						
						menu.setEnabled( false );
						
						return;
					}
					
					menu.setEnabled( publisher.isPublishDesc(download) || subscriber.isSubscriptionDesc(download));
				}
			};
		
		
		MenuItemListener	menu_listener = 
				new MenuItemListener()
				{
					public void
					selected(
						MenuItem		_menu,
						Object			_target )
					{
						Object	obj = ((TableRow)_target).getDataSource();
						
						if ( obj == null ){
							
							return;
						}
						
						Download				download;
						
						if ( obj instanceof Download ){
						
							download = (Download)obj;
								
						}else{
							
							DiskManagerFileInfo file = (DiskManagerFileInfo)obj;
							
							try{
								download	= file.getDownload();
								
							}catch( DownloadException e ){	
								
								Debug.printStackTrace(e);
								
								return;
							}
						}
						
						String url = "azplug:?id=azdhtfeed&name=DDB%20Feed&arg=";
						
						String magnet = "magnet:?xt=urn:btih:" + Base32.encode( download.getTorrent().getHash());
						
						try{
							url += URLEncoder.encode( magnet, "UTF-8" );
							
							Subscription   subs = SubscriptionManagerFactory.getSingleton().createRSS(
								"DDB Feed: " + magnet, new URL( url ), 120, new HashMap());
							
							// subs.setPublic( false );
							
						}catch( Throwable e ){
							
							log.log( "Failed to create subscription", e );
						}
					}
				};
				

					
		TableContextMenuItem menu_item_ctorrents 	= 
			plugin_interface.getUIManager().getTableManager().addContextMenuItem(
					TableManager.TABLE_MYTORRENTS_COMPLETE, 
					"azdhtfeed.menu.createsubs");

		menu_item_ctorrents.addFillListener( menu_fill_listener );
		
		menu_item_ctorrents.addListener( menu_listener );
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{	
					plugin_interface.getUtilities().createThread(
						"Subscriber Initialsation",
						new Runnable()
						{
							public void
							run()
							{		
								if ( !checkDHTAvailable()){
									
									return;
								}
								
								if ( subscribe_enable.getValue()){
												
									subscriber.initialise( 
											subscribe_port.getValue(), 
											subscribe_refresh.getValue(),
											subscribe_port_local );
													
									subscribe_button.addListener(
										new ParameterListener()
										{
											public void
											parameterChanged(
												Parameter	param )
											{
												plugin_interface.getUtilities().createThread(
														"Subscribe",
														new Runnable()
														{
															public void
															run()
															{
																subscriber.subscribe( subscribe_name.getValue().trim());
															}
														});
											}							
										});
									
									unsubscribe_button.addListener(
											new ParameterListener()
											{
												public void
												parameterChanged(
													Parameter	param )
												{
													plugin_interface.getUtilities().createThread(
														"Unsubscribe",
														new Runnable()
														{
															public void
															run()
															{
																subscriber.unsubscribe( subscribe_name.getValue().trim());
															}
														});
												}
											});
									
									subscribe_refresh_button.addListener(
											new ParameterListener()
											{
												public void
												parameterChanged(
													Parameter	param )
												{
													plugin_interface.getUtilities().createThread(
														"Subscribe refresh",
														new Runnable()
														{
															public void
															run()
															{
																subscriber.refresh();
															}
														});
												}
											});
								}		
							}
						});
					
					plugin_interface.getUtilities().createThread(
							"Publisher Initialsation",
							new Runnable()
							{
								public void
								run()
								{						
									if ( !checkDHTAvailable()){
										
										return;
									}
									
									if ( publish_enable.getValue()){
													
										publisher.initialise( auto_republish.getValue());
										
									}else{
										
										publish_enable.addListener(
											new ParameterListener()
											{
												public void
												parameterChanged(
													Parameter	param )
												{
													publisher.initialise(auto_republish.getValue());
												}					
											});
									}
									
									publish_button.addListener(
											new ParameterListener()
											{
												public void
												parameterChanged(
													Parameter	param )
												{
													plugin_interface.getUtilities().createThread(
														"Publish",
														new Runnable()
														{
															public void
															run()
															{
																publisher.publish( publish_name.getValue().trim(), publish_location.getValue().trim());
															}
														});
												}
											});
																		
									unpublish_button.addListener(
											new ParameterListener()
											{
												public void
												parameterChanged(
													Parameter	param )
												{
													plugin_interface.getUtilities().createThread(
														"Unpublish",
														new Runnable()
														{
															public void
															run()
															{
																publisher.unpublish( publish_name.getValue().trim());
															}
														});
												}
											});
									
									republish_button.addListener(
											new ParameterListener()
											{
												public void
												parameterChanged(
													Parameter	param )
												{
													plugin_interface.getUtilities().createThread(
														"Republish",
														new Runnable()
														{
															public void
															run()
															{
																publisher.republish();
															}
														});
												}
											});
									
									keyregen_button.addListener(
											new ParameterListener()
											{
												public void
												parameterChanged(
													Parameter	param )
												{
													keyregen_enable.setValue( false );
													
													plugin_interface.getUtilities().createThread(
															"KeyGen",
															new Runnable()
															{
																public void
																run()
																{
																	publisher.generateKeys();
																}
															});												}
											});
									}
						});
				}
			
				public void
				closedownInitiated()
				{				
				}
				
				public void
				closedownComplete()
				{	
				}
			});
	}
	
	public InputStream
	handleURLProtocol(
		String		arg )
	
		throws IPCException
	{
			// arg is magnet uri of publish resource (feed_location)
				
		try{
			return( subscriber.getSubscriptionContent( arg ));
			
		}catch( Throwable e ){
			
			log.log( "handleURLProtocol failed: " + Debug.getNestedExceptionMessage(e));
			
			throw( new IPCException( "Failed to read content for '" + arg + "'", e ));
		}
	}
	
	protected String
	getContentKey(
		String			feed_name,
		RSAPublicKey	public_key )
	{
		String	content_dht_key = 
			feed_name + "," +
			public_key.getPublicExponent().toString(32) + "," +
			public_key.getModulus().toString(32);

		return( content_dht_key );
	}
	
	protected byte[]
	getContentKeyBytes(
		String			feed_name,
		RSAPublicKey	public_key )
	
		throws UnsupportedEncodingException
	{
		return( getContentKey( feed_name, public_key )).getBytes( "ISO-8859-1" );
	}
	
	protected downloadDetails
	downloadResource(
		String	resource )
	
		throws Exception
	{
		ResourceDownloader	rd;
		
		String	lc = resource.toLowerCase();
		
		if ( lc.startsWith( "http:" ) || lc.startsWith( "https:" ) || lc.startsWith( "magnet:" )){
			
			rd = plugin_interface.getUtilities().getResourceDownloaderFactory().create( new URL( resource ));
			
		}else{
			
			rd = plugin_interface.getUtilities().getResourceDownloaderFactory().create( new File( resource ));
			
		}
		
		rd.addListener(
			new ResourceDownloaderAdapter()
			{
				public void
				reportActivity(
					ResourceDownloader	downloader,
					String				activity )
				{
					log.log( activity );
				}
			});
		
	
		InputStream	is = rd.download();
				
		return( new downloadDetails(is, (String)rd.getProperty( ResourceDownloader.PR_STRING_CONTENT_TYPE )));
	}
	
	protected byte[]
	inputStreamToByteArray(
		InputStream	is )
	
		throws IOException
	{
		try{
				// normally the result will be a torrent file, however for testing it can 
				// be the content 
			
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			byte[]	buffer = new byte[65536];
			
			while( true ){
			
				int	len = is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				baos.write( buffer, 0, len );
			}
			
			return( baos.toByteArray());
			
		}finally{
			
			is.close();
		}
	}
	
	protected TorrentAttribute
	getCategoryAttribute()
	{
		return( ta_category );
	}
	
	protected LoggerChannel
	getLog()
	{
		return( log );
	}
	
	protected void
	removeDownload(
		Download	download,
		File		torrent_dir,
		String		log_str )
	{
		try{
			try{
				download.stop();
				
			}catch( Throwable e ){
			}
			
			File	original_torrent = new File( torrent_dir, download.getTorrent().getName() + ".torrent" );
			
			download.remove( true, true );
			
			original_torrent.delete();
			
			log.log( "Removed torrent '" + download.getName() + "' for " + log_str );
			
		}catch( Throwable e ){
			
			log.log( "Failed to remove existing torrent '" + download.getName() + "' for " + log_str, e );
		}
	}
	
	protected Download
	getPublishContent(
		String		feed_name )
	{
		return( publisher.getPublishContent( feed_name ));
	}
	
	protected boolean
	checkDHTAvailable()
	{
		if ( !plugin_interface.getDistributedDatabase().isAvailable()){
			
			log.logAlert( 
					LoggerChannel.LT_ERROR,
					plugin_name + " initialisation failed, Distributed Database unavailable" );
			
			return( false );
		}
		
		return( true );
	}
	
	protected class
	downloadDetails
	{
		private InputStream			is;
		private String				content_type;
		
		protected
		downloadDetails(
			InputStream	_is,
			String		_content_type )
		{
			is				= _is;
			content_type	= _content_type;
		}
		
		protected String
		getContentType()
		{
			return( content_type );
		}
		
		protected InputStream
		getInputStream()
		{
			return( is );
		}
	}
}
