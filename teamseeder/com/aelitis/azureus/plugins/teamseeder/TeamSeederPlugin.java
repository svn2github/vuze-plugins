/*
 * Created on 13-Dec-2005
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.teamseeder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.TrackerException;
import org.gudy.azureus2.plugins.tracker.TrackerListener;
import org.gudy.azureus2.plugins.tracker.TrackerPeer;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;
import org.gudy.azureus2.plugins.tracker.TrackerTorrentListener;
import org.gudy.azureus2.plugins.tracker.TrackerTorrentRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Monitor;
import org.gudy.azureus2.plugins.utils.Utilities;

public class 
TeamSeederPlugin 
	implements Plugin, TrackerTorrentListener
{
	private PluginInterface		plugin_interface;
	private Utilities 			utilities;
	private TorrentAttribute	tracker_client_extensions_attribute;
	
	private LoggerChannel		log;
	
	private String				tracker_ip;
	private Map 				peer_map		= new HashMap();
	private Monitor				peer_map_mon;
	
	private StringParameter		key_param;
	private IntParameter		port_param;
	
	public void
	initialize(
		PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		utilities			= plugin_interface.getUtilities();

		peer_map_mon	= utilities.getMonitor();
		
		LocaleUtilities loc_utils = utilities.getLocaleUtilities();

		loc_utils.integrateLocalisedMessageBundle( "com.aelitis.azureus.plugins.teamseeder.internat.Messages" );
		
		String	plugin_name	= loc_utils.getLocalisedMessageText( "aeteamseeder.name" );
		
		log					= plugin_interface.getLogger().getTimeStampedChannel( plugin_name );

		tracker_client_extensions_attribute	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS );

		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( "aeteamseeder.name" );

		
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
						if ( str.length() > 0 ){
							view_model.getLogArea().appendText( str + "\n" );
						}
						
						StringWriter sw = new StringWriter();
						
						PrintWriter	pw = new PrintWriter( sw );
						
						error.printStackTrace( pw );
						
						pw.flush();
						
						view_model.getLogArea().appendText( sw.toString() + "\n" );
					}
				});		
	
		BasicPluginConfigModel config_model = 
			ui_manager.createBasicPluginConfigModel( "aeteamseeder.name" );
		
		BooleanParameter plugin_enabled = config_model.addBooleanParameter2( "aeteamseeder.enabled", "aeteamseeder.enabled", true );
		

		key_param = config_model.addStringParameter2( "aeteamseeder.key", "aeteamseeder.key", "" );
		
		port_param = config_model.addIntParameter2( "aeteamseeder.port", "aeteamseeder.port", 9354 );
		
		if ( !plugin_enabled.getValue()){
			
			view_model.getStatus().setText( "Disabled" );
			
			log( "Plugin disabled" );
			
			plugin_interface.getDownloadManager().addListener(
					new DownloadManagerListener()
					{
						public void
						downloadAdded(
							Download	download )
						{
							String	old = download.getAttribute( tracker_client_extensions_attribute );
							
							if ( old != null && old.length() > 0 ){
								
								download.setAttribute( tracker_client_extensions_attribute, "" );
							}
						}
							
						public void
						downloadRemoved(
							Download	download )
						{
						}
					});
			return;
		}
		
		Tracker tracker = plugin_interface.getTracker();
		
		URL[]	urls = tracker.getURLs();
		
		if ( urls.length > 0 ){
			
			tracker_ip = urls[0].getHost();
			
			log( "Tracker is configured on '" + tracker_ip + "' - hooking into tracker communications" );
			
			tracker.addListener(
				new TrackerListener()
				{
					public void
					torrentAdded(
						TrackerTorrent	torrent )
					{
						torrent.addListener( TeamSeederPlugin.this );
					}
					
					public void
					torrentChanged(
						TrackerTorrent	torrent )
					{
					}
					
					public void
					torrentRemoved(
						TrackerTorrent	torrent )
					{
						torrent.removeListener(	TeamSeederPlugin.this );
					}
				});
		}
		
		try{
			TrackerWebContext context = tracker.createWebContext( port_param.getValue(), Tracker.PR_HTTP );
			
			context.addPageGenerator(
				new TrackerWebPageGenerator()
				{
					public boolean
					generate(
						TrackerWebPageRequest		request,
						TrackerWebPageResponse		response )
					
						throws IOException
					{
						log( "Received wakeup: " + request.getURL());
						
						try{
							StringTokenizer	tok = new StringTokenizer( request.getURL(), "&" );
							
							String	key 		= null;
							byte[]	hash		= null;
							
							while( tok.hasMoreTokens()){
								
								String	p = tok.nextToken();
								
								int	pos = p.indexOf('=');
								
								if ( pos != -1 ){
									
									String	lhs = p.substring( 0, pos ).toLowerCase();
									
									String	rhs = p.substring(pos+1).trim();
									
									if ( lhs.equals( "team_key")){
										
										key	= rhs;
										
									}else if ( lhs.equals( "/?info_hash" )){
										
										hash = utilities.getFormatters().decodeBytesFromString( rhs );
									}
								}
							}
							
							
							if ( hash == null ){
								
								log( "Invalid hash - null" );
								
								return( false );
							}
							
							if ( key == null || !key.equals( key_param.getValue())){
								
								log( "Invalid key" );
								
								return( false );
							}
							
							Download dl = plugin_interface.getShortCuts().getDownload( hash );
							
							if ( dl == null ){
								
								log( "Invalid hash - download not found" );
								
								return( false );
							}
							
							String name = dl.getName();
							
							if ( dl.getState() == Download.ST_STOPPED ){
								
								log( "    download '" + name + "' is stopped, starting it" );
								
								dl.restart();
								
							}else if ( dl.getState() == Download.ST_QUEUED ){
								
								log( "    download '" + name + "' is queued, updating scrape" );

								dl.requestTrackerScrape( true );
								
							}else if ( dl.getState() == Download.ST_SEEDING ){
								
									// we might not be accepting incoming connections, kick it to establish
									// outbound
								
								log( "    download '" + name + "' is seeding, updating announce" );

								dl.requestTrackerAnnounce( true );
							}
						}catch( Throwable e ){
							
							log( "wakeup processing failed", e );
						}
						
						return( true );
					}
				});
			
			log( "Established wakeup listen on port " + port_param.getValue());
			
		}catch( Throwable e ){
			
			log( "Failed to establish wakeup listen", e );
		}
		
		plugin_interface.getDownloadManager().addListener(
			new DownloadManagerListener()
			{
				public void
				downloadAdded(
					Download	download )
				{
					download.addAttributeListener(
							new DownloadAttributeListener() {
								public void attributeEventOccurred(Download download, TorrentAttribute attribute, int event_type) {
									download.setAttribute( tracker_client_extensions_attribute, "&team_key=" + key_param.getValue());
								}
							}, tracker_client_extensions_attribute, DownloadAttributeListener.WILL_BE_READ);
					
				}
					
				public void
				downloadRemoved(
					Download	download )
				{
					
				}
			});
	}
	
	public void
	preProcess(
		TrackerTorrentRequest request )
	
		throws TrackerException
	{
		TrackerTorrent torrent = request.getTorrent();
		
		String	torrent_name = torrent.getTorrent().getName();
		
		String	param_str = request.getRequest();
							
		StringTokenizer	tok = new StringTokenizer( param_str, "&" );
		
		String	key 		= null;
		long	amount_left	= -1;
		String	event		= null;
		
		while( tok.hasMoreTokens()){
			
			String	p = tok.nextToken();
			
			int	pos = p.indexOf('=');
			
			if ( pos != -1 ){
				
				String	lhs = p.substring( 0, pos ).toLowerCase();
				
				String	rhs = p.substring(pos+1).trim();
				
				if ( lhs.equals( "team_key")){
					
					key = rhs;
					
				}else if( lhs.equals( "left" )){
					
					amount_left	= Long.parseLong( rhs );
					
				}else if ( lhs.equals("event")){
					
					event	= rhs;
				}
			}
		}
		
		if ( event != null && event.equalsIgnoreCase( "stopped" )){
			
			return;
		}
		
		final String	hash = utilities.getFormatters().encodeBytesToString(torrent.getTorrent().getHash());
		
			// handle peer registration for those with keys
		
		if ( key != null ){
			
			String	peer_ip = request.getPeer().getIPRaw();
			
			if ( !key.equals( key_param.getValue())){
				
				log( "Key parameter mismatch for peer '" + peer_ip + "' - supplied = " + key );
				
				return;
			}
			
				// map external address onto loopback
			
			if ( peer_ip.equals( tracker_ip )){
				
				try{
					peer_ip	= InetAddress.getLocalHost().getHostAddress();
					
				}catch( Throwable e ){
					
					log( "Failed to map to local host", e );
				}
			}
			
			try{
				peer_map_mon.enter();
				
				List	peers = (List)peer_map.get( hash );
				
				if ( peers == null ){
					
					peers = new ArrayList();
					
					peer_map.put( hash, peers );
				}
				
				if ( !peers.contains( peer_ip )){
					
					log( "Peer " + peer_ip + " added to team for '" + torrent_name + "'" );
					
					peers.add( peer_ip );
				}
			}finally{
				
				peer_map_mon.exit();
			}
		}else{
			
				// normal peer
			
			if ( request.getRequestType() != TrackerTorrentRequest.RT_ANNOUNCE ){
				
				return;
			}
			
			if ( amount_left < 0 ){
				
				log( "Invalid amount left '" + amount_left + "'" );
				
				return;
			}
			
			if ( amount_left == 0 ){
				
					// seeding, nothing to do
				
				return;
			}
			
			int	seeds = torrent.getSeedCount();
			
			List	peers_to_kick = new ArrayList();
			
			try{
				peer_map_mon.enter();
				
				List	registered_peers = (List)peer_map.get( hash );
				
				if ( registered_peers != null && registered_peers.size() > seeds ){
					
					peers_to_kick = new ArrayList( registered_peers );
					
					TrackerPeer[]	torrent_peers = torrent.getPeers();
					
					for (int i=0;i<torrent_peers.length;i++){
						
						String	ip = torrent_peers[i].getIPRaw();
						
						if ( peers_to_kick.contains( ip )){
							
							// peers_to_kick.remove( ip );
						}
					}

				}
			}finally{
					
				peer_map_mon.exit();
			}
			
			for (int i=0;i<peers_to_kick.size();i++){
				
				final String	target_ip	= (String)peers_to_kick.get(i);
				int		target_port	= port_param.getValue();
				
				log( "Torrent '" + torrent_name + "': waking up peer " + target_ip + " on port " + target_port );
				
				try{
					final URL	url = new URL( "http://" + target_ip + ":" + target_port + "/?info_hash=" + hash + "&team_key=" + key_param.getValue());
				
					utilities.createThread(
						"Kicker",
						new Runnable()
						{
							public void
							run()
							{
								try{
										// make sure this runs after this new peer has been registered on the
										// local tracker
									
									Thread.sleep(1000);
									
									URLConnection	connection = url.openConnection();
									
									connection.connect();
									
									connection.getContentLength();
									
									log( "Triggered wakeup via '" + url + "'" );
									
								}catch( Throwable e ){
									
									log( "Failed to trigger wakeup '" + url + "', removing from team set", e );
									
									try{
										peer_map_mon.enter();
										
										List	registered_peers = (List)peer_map.get( hash );
										
										registered_peers.remove( target_ip );
										
									}finally{
										
										peer_map_mon.exit();
									}
								}
							}
						});
				}catch( Throwable e ){
					
					log( "Kick failed", e );
				}
			}
		}
	}
	
	public void
	postProcess(
		TrackerTorrentRequest request )
	
		throws TrackerException
	{
	}
	
	public void
	log(
		String	str )
	{
		log.log( str );
	}
	
	public void
	log(
		String		str,
		Throwable	e )
	{
		log.log( str, e );
	}
}
