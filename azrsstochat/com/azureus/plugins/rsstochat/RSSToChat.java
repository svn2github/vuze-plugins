/*
 * Created on Dec 11, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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


package com.azureus.plugins.rsstochat;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.components.UITextArea;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.plugins.utils.xml.rss.*;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionResult;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginUtils;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatInstance;

public class 
RSSToChat
	implements UnloadablePlugin
{
	public static final int MAX_MESSAGE_SIZE	= 450;
	
	private PluginInterface			plugin_interface;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	private LocaleUtilities			loc_utils;

	private File		config_file;
	
	private TimerEventPeriodic	timer;
	
	private List<Mapping>		mappings = new ArrayList<Mapping>();
	
	private boolean	unloaded;
	
	public void 
	initialize(
		PluginInterface pi )
			
		throws PluginException 
	{
		plugin_interface = pi;
				
		File data_dir = plugin_interface.getPluginconfig().getPluginUserFile( "test" ).getParentFile();
		
		config_file = new File( data_dir, "config.xml" );
		
		loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		log	= plugin_interface.getLogger().getTimeStampedChannel( "Message Sync");
		
		final UIManager	ui_manager = plugin_interface.getUIManager();
		
		view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azrsstochat.name" ));

		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
									
		config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azrsstochat.name" );

		view_model.setConfigSectionID( "azrsstochat.name" );

		config_model.addLabelParameter2( "azrsstochat.info" );
		
		config_model.addHyperlinkParameter2( "azrsstochat.plugin.link", loc_utils.getLocalisedMessageText( "azrsstochat.plugin.link.url" ));

		ActionParameter	open_dir = config_model.addActionParameter2( "azrsstochat.config.dir.open", "azrsstochat.open" );

		open_dir.addListener(
				new ParameterListener() 
				{
					public void 
					parameterChanged(
						Parameter param ) 
					{
						Utils.launch( config_file.getParentFile().getAbsolutePath());
					}
				});
		ActionParameter	reload_param = config_model.addActionParameter2( "azrsstochat.config.reload", "azrsstochat.reload" );
		
		reload_param.addListener(
			new ParameterListener() 
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					synchronized( mappings ){
					
						loadConfig();
					}
				}
			});
		
		final UITextArea text_area = config_model.addTextArea( "azrsstochat.statuslog");
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	content )
					{
						view_model.getLogArea().appendText( content + "\n" );
						
						text_area.appendText( content + "\n" );
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						view_model.getLogArea().appendText( str + "\n" );
						view_model.getLogArea().appendText( error.toString() + "\n" );
						
						String text = str + ": " + Debug.getNestedExceptionMessage( error );
						
						text_area.appendText( text + "\n" );

					}
				});
				
		synchronized( mappings ){
			
			loadConfig();

			if ( unloaded ){
				
				return;
			}
			
			timer = 
				SimpleTimer.addPeriodicEvent(
					"RSSToChat",
					60*1000,
					new TimerEventPerformer()
					{	
						private int		minute_count;
					
						public void 
						perform(
							TimerEvent event ) 
						{
							minute_count++;
							
							BuddyPluginBeta bp = BuddyPluginUtils.getBetaPlugin();
							
							if ( bp == null ){
								
								log( "Decentralized chat not available (yet)" );
								
								return;
							}
							
							List<Mapping>	maps;
							
							synchronized( mappings ){
								
								maps = new ArrayList<Mapping>( mappings );
							}
							
							for ( Mapping map: maps ){
								
								map.update( bp, minute_count );
							}
						}
					});
		}
	}
	
	private void
	log(
		String		str )
	{
		log.log( str);
	}
	
	private void
	log(
		String		str,
		Throwable 	e )
	{
		log.log( str, e );
	}
	
	private void
	loadConfig()
	{
		log( "Loading configuration" );
		
		if ( !config_file.exists()){
			
			try{
				PrintWriter pw = new PrintWriter( new OutputStreamWriter( new FileOutputStream( config_file ), "UTF-8" ));
			
				String NL = "\r\n";
				
				pw.println(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
					"<!-- See http://wiki.vuze.com/w/RSS_To_Chat -->" + NL +
					"<config>" + NL +
					"</config>");

				pw.close();
				
			}catch( Throwable e ){
			
				log( "Failed to create default configuration", e );
			}
		}
		
		List<Mapping>	loaded_mappings = new ArrayList<Mapping>();
		
		try{
			SimpleXMLParserDocument doc = plugin_interface.getUtilities().getSimpleXMLParserDocumentFactory().create( config_file );
			
			SimpleXMLParserDocumentNode[] kids = doc.getChildren();
			
			int	num_mappings = 0;
			
			for ( SimpleXMLParserDocumentNode kid: kids ){
				
				num_mappings++;
				
				log( "    Processing mapping " + num_mappings );
				
				String kid_name = kid.getName();
				
				if ( !kid_name.equalsIgnoreCase("mapping")){
					
					throw( new Exception( "<mapping> element expected, got " + kid_name  ));
				}
				
				SimpleXMLParserDocumentNode rss_node 		= kid.getChild( "rss" );
				SimpleXMLParserDocumentNode subs_node 		= kid.getChild( "subscription" );
				SimpleXMLParserDocumentNode chat_node 		= kid.getChild( "chat" );
				SimpleXMLParserDocumentNode refresh_node 	= kid.getChild( "refresh" );
				
				String 	source;
				boolean	is_rss;
				
				if ( rss_node != null && subs_node == null ){
					
					SimpleXMLParserDocumentNode url_node = rss_node.getChild( "url" );
					
					if ( url_node == null ){
						
						throw( new Exception( "<rss> must contain a <url> entry" ));
					}

					String url_str = url_node.getValue().trim();
					
					try{
						URL url = new URL( url_str );
						
					}catch( Throwable e ){
						
						throw( new Exception( "<url> value '" + url_str + "' is invalid" ));
					}
					
					source 	= url_str;
					is_rss	= true;
					
				}else if ( subs_node != null && rss_node == null ){
					
					SimpleXMLParserDocumentNode name_node = subs_node.getChild( "name" );
					
					if ( name_node == null ){
						
						throw( new Exception( "<subscription> must contain a <name> entry" ));
					}

					String name = name_node.getValue().trim();

					source 	= name;
					is_rss	= false;
				}else{
					
					throw( new Exception( "<mapping> must contain either an <rss> or a <subscription> entry" ));

				}
					
				if ( chat_node == null ){
					
					throw( new Exception( "<mapping> must contain a <chat> entry" ));
				}
				
				SimpleXMLParserDocumentNode net_node = chat_node.getChild( "network" );
				SimpleXMLParserDocumentNode key_node = chat_node.getChild( "key" );

				if ( net_node == null || key_node == null ){
					
					throw( new Exception( "<chat> must contain a <network> and a <key> entry" ));
				}
				
				String network_str = net_node.getValue().trim();
				
				String network;
				
				if ( network_str.equalsIgnoreCase( "public" )){
					
					network = AENetworkClassifier.AT_PUBLIC;
					
				}else if ( network_str.equalsIgnoreCase( "anonymous" )){
					
					network = AENetworkClassifier.AT_I2P;
					
				}else{
					
					throw( new Exception( "<network> must be either 'public' or 'anonymous'" ));
				}
				
				String key = key_node.getValue().trim();
				
				if ( 	key.startsWith( "Tag:" ) || 
						key.startsWith( "Download:" ) || 
						key.startsWith( "General:" ) || 
						key.startsWith( "Announce:" )){
					
					throw( new Exception( "Invalid key name '" + key + "', select something else" ));
				}
				
				if ( refresh_node == null ){
					
					throw( new Exception( "<mapping> must contain a <refresh> entry" ));
				}

				String refresh_str = refresh_node.getValue().trim();
				
				int	refresh_mins;
				
				try{
					refresh_mins = Integer.parseInt( refresh_str );
							
				}catch( Throwable e ){
					
					throw( new Exception( "<refresh> value of '" + refresh_str + "' is invalid" ));

				}
				
				Mapping mapping = new Mapping( source, is_rss, network, key, refresh_mins );
				
				log( "    Mapping: " + mapping.getOverallName());
				
				loaded_mappings.add( mapping );
			}
		
			
		}catch( Throwable e ){
			
			log( "Failed to parse configuration file: " + Debug.getNestedExceptionMessage( e ));
		}
		
		log( "Configuration loaded" );

		synchronized( mappings ){
			
			for ( Mapping mapping: mappings ){
				
				mapping.destroy();
			}
			
			mappings.clear();
			
			mappings.addAll ( loaded_mappings );
		}
	}
	
	private void
	updateRSS(
		String			rss_source,
		ChatInstance	inst )
	{
		try{
			RSSFeed feed = plugin_interface.getUtilities().getRSSFeed( new URL( rss_source ));
			
			RSSChannel channel = feed.getChannels()[0];
			
			RSSItem[] items = channel.getItems();
			
			log( "    RSS '" + rss_source + "' returned " + items.length + " total items" );
			
			Arrays.sort( 
				items,
				new Comparator<RSSItem>()
				{
					public int 
					compare(
						RSSItem i1,
						RSSItem i2) 
					{
						long res = i1.getPublicationDate().getTime() - i2.getPublicationDate().getTime();
						
						if ( res < 0 ){
							return( -1 );
						}else if ( res > 0 ){
							return( 1 );
						}else{
							return( 0 );
						}
					}
				});
			
			int	posted = 0;
			
			for ( RSSItem item: items ){
				
				String title = item.getTitle();
				
				if ( title.length() > 80 ){
					
					title = title.substring( 0, 80 ) + "...";
				}
				
				String 	hash 		= "";
				String	dl_link 	= null;
				String	cdp_link 	= null;
				
				long	size 		= -1;
				long	seeds		= -1;
				long	leechers	= -1;
				
				SimpleXMLParserDocumentNode node = item.getNode();
				
				SimpleXMLParserDocumentNode[] kids = node.getChildren();
				
				for ( SimpleXMLParserDocumentNode child: kids ){
					
					String	lc_child_name 		= child.getName().toLowerCase();
					String	lc_full_child_name 	= child.getFullName().toLowerCase();
					
					String	value = child.getValue();
					
					if (lc_child_name.equals( "enclosure" )){
						
						SimpleXMLParserDocumentAttribute typeAtt = child.getAttribute("type");
						
						if( typeAtt != null && typeAtt.getValue().equalsIgnoreCase( "application/x-bittorrent")) {
							
							SimpleXMLParserDocumentAttribute urlAtt = child.getAttribute("url");
							
							if( urlAtt != null ){
								
								dl_link = urlAtt.getValue();
							}
							
							SimpleXMLParserDocumentAttribute lengthAtt = child.getAttribute("length");
							
							if (lengthAtt != null){
								
								try{
									size = Long.parseLong(lengthAtt.getValue().trim());
									
								}catch( Throwable e ){
								}	
							}
						}
			
					}else if ( lc_child_name.equals( "link" ) || lc_child_name.equals( "guid" )) {
						
						String lc_value = value.toLowerCase();
														
						try{
							URL url = new URL(value);

							if ( 	lc_value.endsWith( ".torrent" ) ||
									lc_value.startsWith( "magnet:" ) ||
									lc_value.startsWith( "bc:" ) ||
									lc_value.startsWith( "bctp:" ) ||
									lc_value.startsWith( "dht:" )){
								
								
								dl_link = value;
								
							}else{
								
								cdp_link = value;
							}
						}catch( Throwable e ){
							
								// see if this is an atom feed 
								//  <link rel="alternate" type="application/x-bittorrent" href="http://asdasd/ 
							
							SimpleXMLParserDocumentAttribute typeAtt = child.getAttribute( "type" );
							
							if ( typeAtt != null && typeAtt.getValue().equalsIgnoreCase("application/x-bittorrent")) {
							
								SimpleXMLParserDocumentAttribute hrefAtt = child.getAttribute( "href" );
								
								if ( hrefAtt != null ){
									
									String	href = hrefAtt.getValue().trim();
									
									try{
										
										dl_link = new URL( href ).toExternalForm();
										
									}catch( Throwable f ){
										
									}
								}
							}
						}
					}else if ( lc_child_name.equals( "content" ) && feed.isAtomFeed()){
						
						SimpleXMLParserDocumentAttribute srcAtt = child.getAttribute( "src" );
						
						String	src = srcAtt==null?null:srcAtt.getValue();
									
						if ( src != null ){
							
							boolean	is_dl_link = false;
							
							SimpleXMLParserDocumentAttribute typeAtt = child.getAttribute( "type" );
							
							if ( typeAtt != null && typeAtt.getValue().equalsIgnoreCase("application/x-bittorrent")) {

								is_dl_link = true;
							}
							
							if ( !is_dl_link ){
							
								is_dl_link = src.toLowerCase().indexOf( ".torrent" ) != -1;
							}
								
							if ( is_dl_link ){
								
								try{
									new URL( src );
								
									dl_link = src;
									
								}catch( Throwable e ){
								}
							}
						}
					}else if ( lc_full_child_name.equals( "vuze:size" )){
						
						try{
							size = Long.parseLong( value );
							
						}catch( Throwable e ){
						}
					}else if ( lc_full_child_name.equals( "vuze:seeds" )){
						
						try{
							seeds = Long.parseLong( value );
							
						}catch( Throwable e ){
						}
					
					}else if ( lc_full_child_name.equals( "vuze:peers" )){
						
						try{
							leechers = Long.parseLong( value );
							
						}catch( Throwable e ){
						}
						
					
						
					}else if ( lc_full_child_name.equals( "vuze:downloadurl" )){

						dl_link = value;
						
					}else if ( lc_full_child_name.equals( "vuze:assethash" )){

						hash = value;
					}
				}
				
				if ( hash == "" && dl_link == null ){
					
					continue;
				}
					
				int	length_rem = MAX_MESSAGE_SIZE;
				
				String magnet;
				
				if ( dl_link != null && dl_link.toLowerCase(Locale.US).startsWith( "magnet:" )){
					
					magnet = dl_link;
					
				}else{
				
					magnet = 
						"magnet:?xt=urn:btih:" + hash + 
						"&dn=" + UrlUtils.encode( title ) + 
						(dl_link==null?"":("&fl=" + UrlUtils.encode( dl_link )));
				}
				
				String lc_magnet = magnet.toLowerCase( Locale.US );
				
				if ( !lc_magnet.contains( "&dn=" )){	
					magnet += "&dn=" + UrlUtils.encode( title );
				}
				
				if ( size != -1 ){
					if ( !lc_magnet.contains( "&xl=" )){
						magnet += "&xl="  + size;
					}
				}
				
				if ( seeds != -1 ){
					magnet += "&_s="  + seeds;
				}
				
				if ( leechers != -1 ){
					magnet += "&_l="  + leechers;
				}
				
				length_rem -= magnet.length();
				
				String tail = "[[$dn]]";
				
				String info = "";
				
				if ( size != -1 ){
					
					info = DisplayFormatters.formatByteCountToKiBEtc( size );
				}
				
				if ( cdp_link != null ){
					
					info += (info==""?"":", ")+ cdp_link + "[[details]]";
				}
				
				if ( info != "" ){
					
					tail += " (" + info + ")";
				}
				
				if ( tail.length() < length_rem ){
					
					magnet += tail;
				}
				
				inst.sendMessage( magnet, new HashMap<String, Object>());
				
				posted++;
			}
			
			log( "    Posted " + posted + " new results" );
			
		}catch( Throwable e ){
			
			log( "RSS update for " + rss_source + " failed", e );
		}
	}	
	
	private void
	updateSubscription(
		String			subscription_name,
		ChatInstance	chat )
	{
		Subscription[] subscriptions = SubscriptionManagerFactory.getSingleton().getSubscriptions();
		
		boolean	subs_found = false;
		
		for ( Subscription sub: subscriptions ){
			
			if ( !sub.isSubscribed()){
				
				continue;
			}
			
			String sub_name = sub.getName();
			
			if ( sub_name.equals( subscription_name )){
				
				subs_found = true;
				
				SubscriptionResult[] results = sub.getResults( false );
				
				log( "    Subscription '" + subscription_name + "' returned " + results.length + " total results" );

				int	posted = 0;
				
				for ( SubscriptionResult result: results ){
					
					String id = result.getID();
					
					Map<Integer,Object>	props = result.toPropertyMap();
					
					System.out.println( id + " -> " + result.toPropertyMap());
					
					String title = (String)props.get( SearchResult.PR_NAME );
					
					if ( title.length() > 80 ){
						
						title = title.substring( 0, 80 ) + "...";
					}
					
					byte[] 	b_hash 		= (byte[])props.get( SearchResult.PR_HASH );
					
					String hash = b_hash==null?"":Base32.encode(b_hash);
					
					String	dl_link 	= (String)props.get( SearchResult.PR_DOWNLOAD_LINK );
					String	cdp_link 	= (String)props.get( SearchResult.PR_DETAILS_LINK );
					
					long	size 		= (Long)props.get( SearchResult.PR_SIZE );
					long	seeds		= (Long)props.get( SearchResult.PR_SEED_COUNT );
					long	leechers	= (Long)props.get( SearchResult.PR_LEECHER_COUNT );
													
					if ( hash == "" && dl_link == null ){
						
						continue;
					}
						
					int	length_rem = MAX_MESSAGE_SIZE;
					
					String magnet;
					
					if ( dl_link != null && dl_link.toLowerCase(Locale.US).startsWith( "magnet:" )){
						
						magnet = dl_link;
						
					}else{
					
						magnet = 
							"magnet:?xt=urn:btih:" + hash + 
							"&dn=" + UrlUtils.encode( title ) + 
							(dl_link==null?"":("&fl=" + UrlUtils.encode( dl_link )));
					}
					
					String lc_magnet = magnet.toLowerCase( Locale.US );
					
					if ( !lc_magnet.contains( "&dn=" )){	
						magnet += "&dn=" + UrlUtils.encode( title );
					}
					
					if ( size != -1 ){
						if ( !lc_magnet.contains( "&xl=" )){
							magnet += "&xl="  + size;
						}
					}
					
					if ( seeds != -1 ){
						magnet += "&_s="  + seeds;
					}
					
					if ( leechers != -1 ){
						magnet += "&_l="  + leechers;
					}
					
					length_rem -= magnet.length();
					
					String tail = "[[$dn]]";
					
					String info = "";
					
					if ( size != -1 ){
						
						info = DisplayFormatters.formatByteCountToKiBEtc( size );
					}
					
					if ( cdp_link != null ){
						
						info += (info==""?"":", ")+ cdp_link + "[[details]]";
					}
					
					if ( info != "" ){
						
						tail += " (" + info + ")";
					}
					
					if ( tail.length() < length_rem ){
						
						magnet += tail;
					}
					
					chat.sendMessage( magnet, new HashMap<String, Object>());

					posted ++;

					if ( posted++ > 5 ){
						
						break;
					}
				}
				
				log( "    Posted " + posted + " new results to " + chat.getName());
			}
		}
		
		if ( !subs_found ){
			
			log( "Subscription '" + subscription_name + "' not found" );
		}
	}
	
	
	public void
	unload() 
			
		throws PluginException 
	{
		synchronized( mappings ){
			
			unloaded	= true;
			
			if ( timer != null ){
				
				timer.cancel();
				
				timer = null;
			}
			
			for ( Mapping mapping: mappings ){
				
				mapping.destroy();
			}
			
			mappings.clear();
		}
	}
	
	private class
	Mapping
	{
		private final String		source;
		private final boolean		is_rss;
		private final String		network;
		private final String		key;
		private final int			refresh;
		
		private ChatInstance	chat;
		private boolean			updating;
		private int				last_udpate_ticks;
		
		private boolean	destroyed;
		
		private
		Mapping(
			String			_source,
			boolean			_is_rss,
			String			_network,
			String			_key,
			int				_refresh )
		{
			source		= _source;
			is_rss		= _is_rss;
			network		= _network;
			key			= _key;
			refresh		= _refresh;
		}
		
		private void
		update(
			BuddyPluginBeta	bp,
			int				minute_count )
		{
			if ( minute_count % refresh != 0 ){
				
				return;
			}
						
			log( "Refreshing " + getSourceName());
			
			ChatInstance chat_instance;
			
			synchronized( this ){
				
				if ( destroyed ){
					
					log( "Mapping destroyed" );
					
					return;
				}
				
				if ( chat == null || chat.isDestroyed()){
					
					
					try{
						chat = bp.getChat( network, key );
						
						chat.setFavourite( true );
						
						chat.setSaveMessages( true );
						
					}catch( Throwable e ){
						
						log( "Failed to create chat '" + getChatName() + "': " + Debug.getNestedExceptionMessage( e ));
						
						return;
					}
				}
				
				chat_instance = chat;
			}
			
			if ( is_rss ){
				
				updateRSS( source, chat_instance );
				
			}else{
				
				updateSubscription( source, chat_instance);
			}
			
			last_udpate_ticks = minute_count;
		}
		
		private void
		destroy()
		{
			synchronized( this ){

				destroyed = true;
				
				if ( chat != null ){
				
					chat.destroy();
				
					chat = null;
				}
			}	
		}
		
		private String
		getOverallName()
		{
			return( getSourceName() + ", " + getChatName() + ", refresh=" + refresh + " min" );
		}
		
		private String
		getSourceName()
		{
			return( ( is_rss?"RSS":"Subscription") + ": " + source );
		}
		
		private String
		getChatName()
		{
			return((network==AENetworkClassifier.AT_PUBLIC?"Public":"Anonymous") + ": " + key );
		}
	}
}
