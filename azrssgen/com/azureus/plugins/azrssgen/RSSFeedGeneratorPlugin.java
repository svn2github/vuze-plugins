/*
 * Created on Oct 31, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */



package com.azureus.plugins.azrssgen;

import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.plugins.upnp.UPnPPlugin;


public class 
RSSFeedGeneratorPlugin 
	implements Plugin, TrackerWebPageGenerator
{
	private static final int DEFAULT_PORT	= 6889;

	private PluginInterface		plugin_interface;
	private LoggerChannel		log;

	private BooleanParameter	gen_port_local;
	private BooleanParameter 	gen_cats;
	
	private TorrentAttribute	ta_category;

	
	public void 
	initialize(
		PluginInterface _plugin_interface )
		
		throws PluginException 
	{	
		plugin_interface = _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel( "RSSFeedGen" );
		
		ta_category		= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_CATEGORY );

		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "com.aelitis.azureus.plugins.azrssgen.internat.Messages");
		
		final String plugin_name = plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "azrssgen.name" );
				
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azrssgen.name");
		
		config_model.addLabelParameter2( "azrssgen.info" );
		
		IntParameter		gen_port 			= config_model.addIntParameter2( "gen.port", "azrssgen.port", DEFAULT_PORT );
		
		gen_port_local		= config_model.addBooleanParameter2( "gen.port.local", "azrssgen.port.local", false );

		final int	port = gen_port.getValue();
				
		BooleanParameter gen_port_upnp		= config_model.addBooleanParameter2( "gen.port.upnp", "azrssgen.port.upnp", true );

		gen_cats			= config_model.addBooleanParameter2( "azrssgen.categories.enable", "azrssgen.categories.enable", true );

		
		if ( gen_port_upnp.getValue() ){
			
			plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						try{
							PluginManager manager = plugin_interface.getPluginManager();
							
							PluginInterface pi_upnp = manager.getPluginInterfaceByClass(UPnPPlugin.class);
							
							if ( pi_upnp != null ){
								
								((UPnPPlugin)pi_upnp.getPlugin()).addMapping( plugin_name, true, port, true);
							}
						}catch( Throwable e ){
							
			
							log.log("Error adding UPnP mapping for: " + port);
						}
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
		
		try{
			TrackerWebContext ctx = plugin_interface.getTracker().createWebContext( port, Tracker.PR_HTTP );
		
			ctx.addPageGenerator( this );
	
			log.log( "generator running on port " + port );
			
		}catch( Throwable e ){
			
			log.log( "generator initialisation failed", e );
		}
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url	= request.getURL();
				
		log.log( request.getClientAddress() + " -> " + url );
	
		if ( gen_port_local.getValue()){
			
			InetAddress addr = InetAddress.getByName( request.getClientAddress());
					
			if ( 	NetworkInterface.getByInetAddress( addr ) == null &&
					!addr.isLoopbackAddress()){
				
				log.log( "   access denied" );
				
				response.setReplyStatus( 404 );
				
				return( true );	
			}
		}
		
		int	pos = url.indexOf( '?' );
		
		if ( pos != -1 ){
		
			String[]	args = url.substring( pos+1 ).split( "&" );
			
			for (int i=0;i<args.length;i++){
				
				String[]	x = args[i].split( "=" );
				
				String	lhs = x[0];
				String	rhs	= UrlUtils.decode(x[1]);
				
				List<Download>	selected_dls = new ArrayList<Download>();
				
				if ( lhs.equals( "hash" )){
					
					try{
						Download dl = plugin_interface.getDownloadManager().getDownload( Base32.decode( rhs ));
					
						Torrent torrent = dl.getTorrent();
						
						response.getOutputStream().write( torrent.writeToBEncodedData());
						
						response.setContentType( "application/x-bittorrent" );
						
						return( true );
						
					}catch( Throwable e ){
					}
					
					log.log( "   torrent download failed" );
					
					response.setReplyStatus( 404 );
					
					return( true );	
					
				}else if ( lhs.equals( "cat" ) && gen_cats.getValue()){
					
					Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
					
					for (int j=0;j<downloads.length;j++){
						
						Download download = downloads[j];
						
						Torrent torrent = download.getTorrent();
						
						if ( torrent == null ){
							
							continue;
						}
						
						String dl_cat = download.getAttribute( ta_category );
						
						if ( 	rhs.equals( "All" ) ||
								( dl_cat != null && dl_cat.equals( rhs )) ||
								( dl_cat == null && rhs.equals( "Uncategorized" ))){
							
							if ( !TorrentUtils.isReallyPrivate( PluginCoreUtils.unwrap( torrent ))){
								
								selected_dls.add( download );
							}
						}
					}
					
					response.setContentType( "text/xml" );
					
					OutputStream os = response.getOutputStream();
					
					PrintWriter pw = new PrintWriter(new OutputStreamWriter( os, "UTF-8" ));
					
					pw.println( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
					
					pw.println( "<rss version=\"2.0\" xmlns:vuze=\"http://www.vuze.com\">" );
					
					pw.println( "<channel>" );
					
					pw.println( "<title>" + escape( rhs ) + "</title>" );
					
					Collections.sort(
						selected_dls,
						new Comparator<Download>()
						{
							public int  
							compare(
								Download d1, 
								Download d2) 
							{
								long	added1 = getAddedTime( d1 )/1000;
								long	added2 = getAddedTime( d2 )/1000;

								return((int)(added2 - added1 ));
							}
						});
										
					String	feed_date_key = "feed_date.category." + rhs;
					
					PluginConfig pc = plugin_interface.getPluginconfig();
					
					long	feed_date = pc.getPluginLongParameter( feed_date_key, 0 );

					if ( selected_dls.size() > 0 ){
						
						long newest = getAddedTime( selected_dls.get(0));
						
						if ( newest > feed_date ){
							
							feed_date = newest;
							
							pc.setPluginParameter( feed_date_key, feed_date );
						}
					}
									
					pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( feed_date ) + "</pubDate>" );

					for (int j=0;j<selected_dls.size();j++){
						
						Download download = (Download)selected_dls.get( j );
						
						DownloadManager	core_download = PluginCoreUtils.unwrap( download );
						
						Torrent torrent = download.getTorrent();
												
						pw.println( "<item>" );
						
						pw.println( "<title>" + escape( download.getName()) + "</title>" );
						
						String magnet_uri = UrlUtils.getMagnetURI( download );
						
						String obtained_from = TorrentUtils.getObtainedFrom( core_download.getTorrent());
						
						boolean	added_fl = false;
						
						if ( obtained_from != null ){
							
							try{
								URL ou = new URL( obtained_from );
								
								if ( ou.getProtocol().toLowerCase( Locale.US ).startsWith( "http" )){
									
									magnet_uri += "&fl=" + UrlUtils.encode( ou.toExternalForm());
									
									added_fl = true;
								}
							}catch( Throwable e ){
								
							}
						}
						
						if ( !added_fl ){
						
							String host = (String)request.getHeaders().get( "host" );
							
							String local_fl = "http://" + host + "/?hash=" + Base32.encode( torrent.getHash());
							
							magnet_uri += "&fl=" + UrlUtils.encode( local_fl );
						}
						
						pw.println( "<link>" + escape( magnet_uri ) + "</link>" );
						
						long added = core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
						
						pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( added ) + "</pubDate>" );
						
						pw.println(	"<vuze:size>" + torrent.getSize()+ "</vuze:size>" );
						pw.println(	"<vuze:assethash>" + Base32.encode( torrent.getHash())+ "</vuze:assethash>" );
						pw.println( "<vuze:downloadurl>" + escape( magnet_uri ) + "</vuze:downloadurl>" );

						DownloadScrapeResult scrape = download.getLastScrapeResult();
						
						if ( scrape != null && scrape.getResponseType() == DownloadScrapeResult.RT_SUCCESS ){
							
							pw.println(	"<vuze:seeds>" + scrape.getSeedCount() + "</vuze:seeds>" );
							pw.println(	"<vuze:peers>" + scrape.getNonSeedCount() + "</vuze:peers>" );
						}
						
						pw.println( "</item>" );
					}
					
					pw.println( "</channel>" );
					
					pw.println( "</rss>" );

					pw.flush();
					
					return( true );
				}
			}
		}
		
		return( false );
	}
	
	protected long
	getAddedTime(
		Download	download )
	{
		DownloadManager	core_download = PluginCoreUtils.unwrap( download );
		
		return( core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME));
	}
	
	protected String
	escape(
		String	str )
	{
		return( XUXmlWriter.escapeXML(str));
	}
}
