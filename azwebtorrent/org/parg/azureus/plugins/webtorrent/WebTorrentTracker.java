/*
 * Created on Feb 5, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
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

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.pluginsimpl.local.ipc.IPCInterfaceImpl;
import org.json.simple.JSONObject;
import org.parg.azureus.plugins.webtorrent.GenericWSServer.ServerWrapper;

import com.aelitis.azureus.util.JSONUtils;

public class 
WebTorrentTracker 
{
	private final WebTorrentPlugin		plugin;
	
	private final GenericWSServer		gs_server;
	
	private final boolean				is_ssl;
	private final String				bind_ip;
	private final int					port;
	
	private final String				host;
	
	private ServerWrapper		server;
	
	private Map<Object,SessionWrapper>		session_map = new HashMap<>();
	
	private boolean		destroyed;
	
	protected
	WebTorrentTracker(
		WebTorrentPlugin	_plugin,
		GenericWSServer		_server,
		boolean				_is_ssl,
		String				_bind_ip,
		int					_port,
		String				_host )
	{
		plugin		= _plugin;
		gs_server	= _server;
		is_ssl		= _is_ssl;
		bind_ip		= _bind_ip;
		port		= _port;
		host		= _host;
	}
	
	public boolean
	isSSL()
	{
		return( is_ssl );
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public String
	getBindIP()
	{
		return( bind_ip );
	}

	public String
	getHost()
	{
		return( host );
	}
	
	public void
	start()
	
		throws Exception
	{
		server = gs_server.startServer( is_ssl, bind_ip, port, "/wstracker", new IPCInterfaceImpl( this ));
	}
	
	public void
	stop()
	{
		server.destroy();
	}
	
	public String
	getURL()
	{
		return( ( is_ssl?"wss":"ws" ) + "://" + host + ":" + port + "/wstracker/vuze" );
	}
	
	public void
	sessionAdded(
		Object		server,
		URI			uri,
		Object		session )
	{
		boolean	close_it = false;
		
		synchronized( session_map ){
			
			if ( destroyed ){
				
				close_it = true;
				
			}else{
			
				session_map.put( session, new SessionWrapper( server,  session, uri ));
			}
		}
		
		if ( close_it ){
			
			try{
				plugin.closeSession( server, session );
				
			}catch( Throwable f ){
				
			}
		}
	}
	
	public void
	sessionRemoved(
		Object		server,
		Object		session )
	{
		SessionWrapper	wrapper;
		
		synchronized( session_map ){
			
			wrapper = session_map.remove( session );
		}
		
		if ( wrapper != null ){
			
			wrapper.destroy();
		}
	}
	
	public void
	messageReceived(
		Object		server,
		Object		session,
		String		message )
	{
		System.out.println( "messageReceived: " + message );
		
		boolean	close_it	= false;
		
		String	reply 		= null;
		
		synchronized( session_map ){
			
			SessionWrapper wrapper = session_map.get( session );
		
			if ( wrapper == null ){
				
				close_it = true;
				
			}else{
			
				wrapper.setAlive();
				
				try{			
					Map<String, Object> map_in	= JSONUtils.decodeJSON( message );
					
					String hash_str = (String)map_in.get( "info_hash" );
								
					byte[] info_hash = hash_str.getBytes( "ISO-8859-1" );
					
					System.out.println( "hash=" + ByteFormatter.encodeString( info_hash ));
					
					String peer_id_str = (String)map_in.get( "peer_id" );
					
					byte[] peer_id = peer_id_str.getBytes( "ISO-8859-1" );

					if ( map_in.containsKey( "answer" )){
						
						for ( SessionWrapper w2: session_map.values()){
							
							if ( w2 != wrapper ){
								
								plugin.sendMessage( w2.server, w2.session, message );
							}
						}
					}else{
						int		numwant 	= ((Number)map_in.get("numwant")).intValue();
						long	uploaded 	= ((Number)map_in.get("uploaded")).longValue();
						long	downloaded 	= ((Number)map_in.get("downloaded")).longValue();
						
							// meh, not present for webtorrent
						
						Number	l_left		= (Number)map_in.get("left");
						long	left 		= l_left==null?Long.MAX_VALUE:l_left.longValue();
												
						if ( left == 0 ){
														
							wrapper.setComplete();
						}
						
						String event = (String)map_in.get( "event" );
						
						if ( event != null && event.equals( "completed" )){
														
							wrapper.setComplete();
						}
						
						List<Map>	offers = (List<Map>)map_in.get( "offers" );
						
						if ( offers != null ){
							
							for ( Map m: offers ){
								
								Map	 	offer 			= (Map)m.get( "offer" );
								String 	offer_id_str	= (String)m.get( "offer_id" );
								
								String offer_sdp	= (String)offer.get( "sdp" );
								
								byte[] offer_id = offer_id_str.getBytes( "ISO-8859-1" );
								
								for ( SessionWrapper w2: session_map.values()){
									
									if ( w2 != wrapper ){
										
										String offer_str = "{\"offer\":{\"type\":\"offer\",\"sdp\":\"" + 
												WebTorrentPlugin.encodeForJSON( offer_sdp  ) + "\"}," + 
												"\"offer_id\":\"" + WebTorrentPlugin.encodeForJSON( offer_id ) + "\"," + 
												"\"peer_id\":\"" + WebTorrentPlugin.encodeForJSON( peer_id ) + "\"," + 
												"\"info_hash\":\"" + WebTorrentPlugin.encodeForJSON( info_hash ) + "\"" + 
												"}";
	
										plugin.sendMessage( w2.server, w2.session, offer_str );
									}
								}
							}
						}
					}
					
					Map<String, Object> map_out = new JSONObject();
					
					map_out.put( "info_hash", WebTorrentPlugin.encodeForJSON( info_hash ));
					
					map_out.put( "complete", 23 );
					map_out.put( "incomplete", 42 );
					
					map_out.put( "action", 1 );
					map_out.put( "interval", 120 );
					
					reply = JSONUtils.encodeToJSON( map_out );
					
					reply = reply.replaceAll( "\\\\u00", "\\u00" );
					
				}catch( Throwable e ){
					
					close_it = true;
					
				}finally{
			
					if ( close_it ){
						
						wrapper.destroy();
						
						session_map.remove( session );
					}
				}
			}
		}
		
		if ( reply != null ){
			
			try{
				
				plugin.sendMessage( server, session, reply );
				
			}catch( Throwable f ){
				
				close_it	= true;
			}	
		}
			
		if ( close_it ){
			
			try{
				plugin.closeSession( server, session );
				
			}catch( Throwable f ){
				
			}
		}
	}
	
	public void
	destroy()
	{
		if ( server != null ){
			
			server.destroy();
		}
		
		Collection<SessionWrapper>	to_destroy;
		
		synchronized( session_map ){
			
			destroyed	= true;
			
			to_destroy = session_map.values();
			
			session_map.clear();
		
		}
	}
	
	private class
	SessionWrapper
	{
		private final Object		server;
		private final Object		session;
		private final URI			uri;
		
		private long	last_heard_from;
		
		private boolean	complete;
		
		private
		SessionWrapper(
			Object		_server,
			Object		_session,
			URI			_uri )
		{
			server		= _server;
			session		= _session;
			uri			= _uri;
			
			setAlive();
		}
		
		private void
		setAlive()
		{
			last_heard_from = SystemTime.getMonotonousTime();
		}
		
		private void
		setComplete()
		{
			complete	= true;
		}
		
		private void
		destroy()
		{
			
		}
	}
}
