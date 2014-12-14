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

import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.plugins.utils.xml.rss.*;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

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
	private PluginInterface		plugin_interface;
	
	private TimerEventPeriodic	timer;
	
	public void 
	initialize(
		PluginInterface pi )
			
		throws PluginException 
	{
		plugin_interface = pi;
		
		final String	source 		= "http://vhdn.vuze.com/vhdn/feed.php?id=126";
		final String	target_net	= AENetworkClassifier.AT_PUBLIC;
		final String	target_key	= "Test Me[pk=AT4PLXYNI255M3XJBWKBTSMRFVYNG73JVB7GG6747NDNCKM2ALBIJM2MUCQZHDANBADMOVZ77PDVMGA&ro=1]";
		
		
		timer = 
			SimpleTimer.addPeriodicEvent(
				"RSSToChat",
				60*1000,
				new TimerEventPerformer()
				{	
					private ChatInstance inst;
				
					public void 
					perform(
						TimerEvent event ) 
					{
						BuddyPluginBeta bp = BuddyPluginUtils.getBetaPlugin();
						
						if ( inst == null || inst.isDestroyed()){
							
							try{
								inst = bp.getChat( target_net, target_key );
								
								inst.setFavourite( true );
								
								inst.setSaveMessages( true );
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
						
						Subscription[] subscriptions = SubscriptionManagerFactory.getSingleton().getSubscriptions();
						
						for ( Subscription sub: subscriptions ){
							
							if ( !sub.isSubscribed()){
								
								continue;
							}
							
							String sub_name = sub.getName();
							
							if ( sub_name.equals( "Test Subscription" )){
								
								SubscriptionResult[] results = sub.getResults( false );
								
								int	done = 0;
								
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
										
									int	length_rem = 450;
									
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
									
									if ( done++ > 5 ){
										
										break;
									}
								}
							}
						}
						
						if ( inst != null ){
							
							try{
								RSSFeed feed = plugin_interface.getUtilities().getRSSFeed( new URL( source ));
								
								RSSChannel channel = feed.getChannels()[0];
								
								RSSItem[] items = channel.getItems();
								
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
										
									int	length_rem = 450;
									
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
								}
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
						
					}
				});
	}
	
	public void
	unload() 
			
		throws PluginException 
	{
		if ( timer != null ){
			
			timer.cancel();
			
			timer = null;
		}
	}
}
