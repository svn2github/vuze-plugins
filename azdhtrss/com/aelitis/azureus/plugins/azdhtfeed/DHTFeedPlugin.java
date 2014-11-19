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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashMap;






import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadWillBeAddedListener;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.components.UITextArea;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.proxy.AEProxyFactory;
import com.aelitis.azureus.core.proxy.AEProxyFactory.PluginProxy;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;



public class 
DHTFeedPlugin 
	implements Plugin
{	
	private final static int		DEFAULT_PORT = 6887;
	
	private final static int		AUTO_REPUBLISH_DEFAULT	= 60;
	private final static int		AUTO_REPUBLISH_MIN		= 15;
	
	private final static int		AUTO_SUBSCRIBE_DEFAULT	= 60;
	private final static int		AUTO_SUBSCRIBE_MIN		= 15;

	protected final static String		TAG_FEED_DESC		= "DDB Feed Description";
	protected final static String		TAG_FEED_CONTENT	= "DDB Feed Content";
	
	protected final static String		TORRENT_CONTENT_TYPE_PROPERTY = "Content-Type";

	private PluginInterface		plugin_interface;
	private String				plugin_name; 

	//private TorrentAttribute	ta_category;
	
	private DHTFeedPluginSubscriber 	subscriber;
	private DHTFeedPluginPublisher		publisher;

	private boolean				checked_i2p_network;
	
	private LoggerChannel		log;
			
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel( "DDB Feed" );
				
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "com.aelitis.azureus.plugins.azdhtfeed.internat.Messages");
		
		plugin_name = plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "azdhtfeed.name" );
		
		//ta_category		= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_CATEGORY );
		
		final UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azdhtfeed.name");

		config_model.addHyperlinkParameter2( "azdhtfeed.wiki", "http://wiki.vuze.com/w/Distributed_Database_Trusted_Feed" );
		
		final BooleanParameter 	subscribe_enable 		= config_model.addBooleanParameter2( "subscribe.enable", "azdhtfeed.subscribe.enable", true );
		
		LabelParameter	subscribe_info = config_model.addLabelParameter2( "azdhtfeed.subscribe.info" );
		
		final StringParameter	subscribe_name			= config_model.addStringParameter2( "subscribe.name", "azdhtfeed.subscribe.name", "" );
		
		final IntParameter			subscribe_port 			= config_model.addIntParameter2( "subscribe.port", "azdhtfeed.subscribe.port", DEFAULT_PORT );
		final BooleanParameter		subscribe_port_local	= config_model.addBooleanParameter2( "subscribe.port.local", "azdhtfeed.subscribe.port.local", true );
		final HyperlinkParameter	subscribe_port_browse	= config_model.addHyperlinkParameter2( "azdhtfeed.subscribe.port.test", "http://127.0.0.1:" + subscribe_port.getValue() + "/" );
		
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
						subscribe_port_browse,
						subscribe_button,
						unsubscribe_button,
						subscribe_refresh_button,
						subscribe_refresh});
		
		subscribe_enable.addEnabledOnSelection( subscribe_info );
		subscribe_enable.addEnabledOnSelection( subscribe_name );
		subscribe_enable.addEnabledOnSelection( subscribe_port );
		subscribe_enable.addEnabledOnSelection( subscribe_port_local );
		subscribe_enable.addEnabledOnSelection( subscribe_port_browse );
		subscribe_enable.addEnabledOnSelection( subscribe_button );
		subscribe_enable.addEnabledOnSelection( unsubscribe_button );
		subscribe_enable.addEnabledOnSelection( subscribe_refresh_button );
		subscribe_enable.addEnabledOnSelection( subscribe_refresh );

		
		final BooleanParameter 	publish_enable 		= config_model.addBooleanParameter2( "publish.enable", "azdhtfeed.publish.enable", false );
		
		LabelParameter	publish_info = config_model.addLabelParameter2( "azdhtfeed.publish.info" );

		final StringListParameter	publish_network	 = config_model.addStringListParameter2( "publish.network", "azdhtfeed.publish.network", new String[]{ AENetworkClassifier.AT_PUBLIC, AENetworkClassifier.AT_I2P }, AENetworkClassifier.AT_PUBLIC );

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
						publish_network,
						publish_name, 
						publish_location, 
						publish_button,
						unpublish_button,
						republish_button,
						auto_republish,
						keyregen_enable,
						keyregen_button });

		publish_enable.addEnabledOnSelection( publish_info );
		publish_enable.addEnabledOnSelection( publish_network );
		publish_enable.addEnabledOnSelection( publish_name );
		publish_enable.addEnabledOnSelection( publish_location );
		publish_enable.addEnabledOnSelection( publish_button );
		publish_enable.addEnabledOnSelection( unpublish_button );
		publish_enable.addEnabledOnSelection( republish_button );
		publish_enable.addEnabledOnSelection( auto_republish );
		publish_enable.addEnabledOnSelection( keyregen_enable );
		
		final BasicPluginViewModel model = 
			plugin_interface.getUIManager().createBasicPluginViewModel( "DDB Feed" );
		
		model.setConfigSectionID( "azdhtfeed.name" );

		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );

		final UITextArea text_area = config_model.addTextArea( "azdhtfeed.statuslog");

		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
						
						text_area.appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						String 	text = "";
						
						if  ( str != null && str.length() > 0 ){
							
							text = str;
							
							model.getLogArea().appendText( str +"\n");
						}
						
						text += ": " + Debug.getNestedExceptionMessage( error );
							
						text_area.appendText( text+"\n");
						
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
												subscribe_button.setEnabled( false );
												
												plugin_interface.getUtilities().createThread(
														"Subscribe",
														new Runnable()
														{
															public void
															run()
															{
																final int[] ok = { 0 };
																
																new AEThread2( "" ){
																	public void
																	run()
																	{
																		try{
																			Thread.sleep(2500);
																		
																			synchronized( ok ){
																				
																				if ( ok[0] != 0 ){
																					
																					return;
																				}
																			}
																			
																			ui_manager.showMessageBox(
																					"azdhtfeed.msg.async.title",
																					"azdhtfeed.msg.async.msg",
																					UIManagerEvent.MT_OK );
																			
																		}catch( Throwable e ){
																			
																			Debug.out( e);
																			
																		}finally{
																			
																			subscribe_button.setEnabled( true );
																		}
																	}
																}.start();
																
																boolean res = subscriber.subscribe( subscribe_name.getValue().trim());
																
																synchronized( ok ){
																
																	ok[0] = res?1:2;
																}
																
																if ( !res ){
																	
																	ui_manager.showMessageBox(
																			"azdhtfeed.msg.failed.title",
																			"azdhtfeed.msg.failed.msg",
																			UIManagerEvent.MT_OK );	
																}
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
																publisher.publish( 
																	publish_name.getValue().trim(), 
																	publish_location.getValue().trim(), 
																	AENetworkClassifier.internalise( publish_network.getValue()));
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
	
	protected void
	checkNetworkAvailable(
		String		network )
	{
		if ( network == AENetworkClassifier.AT_I2P ){
			
			synchronized( this ){
				
				if ( checked_i2p_network ){
				
					return;
				}
				
				checked_i2p_network = true;
			}
			
			if ( plugin_interface.getPluginManager().getPluginInterfaceByID( "azneti2phelper" ) == null ){
				
				plugin_interface.getUIManager().showMessageBox(
						"azdhtfeed.msg.needi2p.title",
						"azdhtfeed.msg.needi2p.msg",
						UIManagerEvent.MT_OK );

			}
		}
	}
	
	protected DistributedDatabase
	getDDB(
		String		network )
	{
		List<DistributedDatabase> ddbs = plugin_interface.getUtilities().getDistributedDatabases( new String[]{ network });
		
		if ( ddbs.size() == 1 ){
			
			DistributedDatabase result = ddbs.get(0);
			
			if ( result.isAvailable()){
				
				return( result );
				
			}else{
				
				log.log( "DDB for network '" + network + "' is not yet available" );
				
				return( null );
			}
		}else{
			
			log.log( "DDB for network '" + network + "' is not available" );

			return( null );
		}
	}
	
	protected Download
	addDownload(
		final Torrent			torrent,
		File					torrent_file,
		File					data_dir,
		final String			network )
		
		throws DownloadException
	{
		DownloadManager download_manager = plugin_interface.getDownloadManager();
		
		DownloadWillBeAddedListener dwbal = null; 

		if ( network != AENetworkClassifier.AT_PUBLIC ){
			
			dwbal = 
				new DownloadWillBeAddedListener()
				{		
					public void 
					initialised(
						Download download )
					{
						if ( Arrays.equals( download.getTorrentHash(), torrent.getHash())){ 
						
							PluginCoreUtils.unwrap( download ).getDownloadState().setNetworks( new String[]{ network });
						}
					}
				};
		
			download_manager.addDownloadWillBeAddedListener( dwbal );
		}
		
		try{

			return( download_manager.addDownload( torrent, torrent_file, data_dir ));
		
		}finally{
			
			if ( dwbal != null ){
				
				download_manager.removeDownloadWillBeAddedListener( dwbal );
			}
		}
	}
	
	protected downloadDetails
	downloadResource(
		String	resource )
	
		throws Exception
	{
		log.log( "Download of " + resource  + " starts" );
		
		ResourceDownloader	rd;
		
		String	lc = resource.toLowerCase();
		
		String network;
		
		PluginProxy plugin_proxy	= null;
		boolean		ok				= false;
		
		try{
			if ( lc.startsWith( "http:" ) || lc.startsWith( "https:" ) || lc.startsWith( "magnet:" )){
				
				rd = plugin_interface.getUtilities().getResourceDownloaderFactory().create( new URL( resource ));
				
				network = lc.contains( "&net=i2p" )?AENetworkClassifier.AT_I2P:AENetworkClassifier.AT_PUBLIC;
				
			}else if ( lc.startsWith( "tor:" )){
				
				String target_resource = resource.substring( 4 );
	
				Map<String,Object>	options = new HashMap<String,Object>();
				
				options.put( AEProxyFactory.PO_PEER_NETWORKS, new String[]{ AENetworkClassifier.AT_TOR });
				
				plugin_proxy = 
					AEProxyFactory.getPluginProxy( 
						"DDB Feed download of '" + target_resource + "'",
						new URL( target_resource ),
						options,
						true );
	
				if ( plugin_proxy == null ){
					
					throw( new Exception( "No Tor plugin proxy available" ));
				}
				
				rd = plugin_interface.getUtilities().getResourceDownloaderFactory().create( plugin_proxy.getURL(), plugin_proxy.getProxy());		
				
				network = lc.contains( "&net=i2p" )?AENetworkClassifier.AT_I2P:AENetworkClassifier.AT_PUBLIC;

			}else{
				
				rd = plugin_interface.getUtilities().getResourceDownloaderFactory().create( new File( resource ));
			
				network = null;
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
					
			downloadDetails result = new downloadDetails(is, (String)rd.getProperty( ResourceDownloader.PR_STRING_CONTENT_TYPE ), network );
			
			log.log( "Download of " + resource  + " completed" );
			
			ok = true;
			
			return( result );
			
		}finally{
			
			if ( plugin_proxy != null ){
				
				plugin_proxy.setOK( ok );
			}
		}
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
	
	/*
	protected TorrentAttribute
	getCategoryAttribute()
	{
		return( ta_category );
	}
	*/
	
	protected void
	assignTag(
		Download		download,
		String			tag_name )
	{
		try{
			TagManager tag_manager = TagManagerFactory.getTagManager();
			
			TagType tag_type = tag_manager.getTagType( TagType.TT_DOWNLOAD_MANUAL );
			
			Tag tag = tag_type.getTag( tag_name, true );
			
			if ( tag == null ){
				
				tag = tag_type.createTag( tag_name, true );
			}
			
			tag.addTaggable( PluginCoreUtils.unwrap( download ));
			
		}catch( Throwable e ){
			
			log.log( "Failed to assign tag", e );
		}
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
	
	protected class
	downloadDetails
	{
		private InputStream			is;
		private String				content_type;
		private String				network;
		
		protected
		downloadDetails(
			InputStream	_is,
			String		_content_type,
			String		_network )
		{
			is				= _is;
			content_type	= _content_type;
			network			= _network;
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
		
		protected String 
		getNetwork()
		{
			return( network );
		}
	}
}
