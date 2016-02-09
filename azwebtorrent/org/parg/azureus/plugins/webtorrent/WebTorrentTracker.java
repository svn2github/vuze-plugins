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
import java.util.Map;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
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
	
	
	private ServerWrapper		server;
	
	protected
	WebTorrentTracker(
		WebTorrentPlugin	_plugin,
		GenericWSServer		_server,
		boolean				_is_ssl,
		String				_bind_ip,
		int					_port )
	{
		plugin		= _plugin;
		gs_server	= _server;
		is_ssl		= _is_ssl;
		bind_ip		= _bind_ip;
		port		= _port;
	}
	
	public void
	start()
	
		throws Exception
	{
		server = gs_server.startServer( is_ssl, bind_ip, port, "/wstracker", new IPCInterfaceImpl( this ));
		
		System.out.println( "tracker url=" + server.getTrackerURL());
	}
	
	public void
	stop()
	{
		server.destroy();
	}
	
	public void
	sessionAdded(
		Object		server,
		URI			uri,
		Object		session )
	{
		//System.out.println( "sessionAdded: " + uri );
	}
	
	public void
	sessionRemoved(
		Object		server,
		Object		session )
	{
		//System.out.println( "sessionRemoved" );
	}
	
	public void
	messageReceived(
		Object		server,
		Object		session,
		String		message )
	{
		System.out.println( "messageReceived: " + message );
		
		try{			
			Map<String, Object> map_in	= JSONUtils.decodeJSON( message );
			
			String hash_str = (String)map_in.get( "info_hash" );
						
			byte[] info_hash = hash_str.getBytes( "ISO-8859-1" );
			
			System.out.println( "hash=" + ByteFormatter.encodeString( info_hash ));
			
			Map<String, Object> map_out = new JSONObject();
			
			map_out.put( "info_hash", WebTorrentPlugin.encodeForJSON( info_hash ));
			
			map_out.put( "complete", 23 );
			map_out.put( "incomplete", 42 );
			
			map_out.put( "action", 1 );
			map_out.put( "interval", 120 );
			
			String reply = JSONUtils.encodeToJSON( map_out );
			
			reply = reply.replaceAll( "\\\\u00", "\\u00" );
			
			plugin.sendMessage( server, session, reply );
			
		}catch( Throwable e ){
			
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
	}
}
