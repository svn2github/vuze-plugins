/*
 * Created on Dec 17, 2015
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

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.server.Server;
import org.gudy.azureus2.core3.util.AESemaphore;

@ServerEndpoint("/vuze")
public class 
WebSocketServer
{
	private static List<Logger>	loggers = new ArrayList<Logger>();
	
	static{
		String[] logger_classes = {
				"java.lang.Class",		// someone messed up coding org.glassfish.tyrus.server.Server logger...
				"org.glassfish.grizzly.http.server.NetworkListener",
				"org.glassfish.grizzly.http.server.HttpServer",
		};
		
		for ( String name: logger_classes ){
			
			Logger logger = Logger.getLogger( name );
		
			logger.setLevel( Level.OFF );
	
			loggers.add( logger );
		}
	}
	
	private static Map<Session,SessionWrapper>	session_map = new IdentityHashMap<Session,SessionWrapper>();
	
	private static Map<Long,Object[]>		id_map	= new HashMap<Long,Object[]>();
	
	
	public SessionWrapper
	getSession(
		long		id,
		long		timeout )
	{
		Object[]	entry;
		
		synchronized( session_map ){
			
			entry = id_map.get( id );
			
			if ( entry == null ){
				
				entry = new Object[]{ new AESemaphore( "" ), null };
				
				id_map.put( id, entry );
			}
		}
		
		if ( ((AESemaphore)entry[0]).reserve( timeout )){
			
			synchronized( session_map ){
				
				SessionWrapper wrapper = (SessionWrapper)entry[1];
				
				if ( !wrapper.isDestroyed()){
					
					return( wrapper );
				}
			}
		}
			
		return( null );
	}
	
	@OnOpen
    public void 
    onOpen(
    	Session session )
    			
    	throws IOException 
    {
		System.out.println( "onOpen: " + session );
		
		String query = session.getRequestURI().getQuery();
		
		String[] args = query.split( "&" );
		
		String	type 	= null;
		long	id		= -1;
		
		for ( String arg: args ){
			
			String[]	bits = arg.split( "=" );
			
			String lhs 	= bits[0].trim();
			String rhs	= bits[1].trim();
			
			if ( lhs.equals( "type" )){
				
				type = rhs;
				
			}else if ( lhs.equals( "id" )){
				
				id = Long.parseLong( rhs );
			}
		}
		
		if ( type.equals( "control" ) && id >= 0 ){
			
			synchronized( session_map ){
				
				SessionWrapper wrapper = new SessionWrapper( session, id );
				
				session_map.put( session, wrapper );
				
				Object[] entry = id_map.get( id );
				
				if ( entry == null ){
					
					entry = new Object[]{ new AESemaphore( "" ), wrapper };
					
					id_map.put( id, entry );
					
				}else{
					
					entry[1] = wrapper;
				}
				
				((AESemaphore)entry[0]).releaseForever();
			}
		}
    }

    @OnMessage
    public String 
    onMessage(
    	String message) 
    {
        return message + " (from your server)";
    }

    @OnError
    public void 
    onError(
    	Throwable t ) 
    {
        t.printStackTrace();
    }

    @OnClose
    public void
    onClose(
    	Session session) 
    {
    	synchronized( session_map ){
    		
    		SessionWrapper wrapper = session_map.remove( session );
    		
    		wrapper.destroy();
    		
    		id_map.remove( wrapper.getID());
    	}
    }
    
    public void 
    runServer() 
    
    	throws Exception
    {
    	Server server = new Server("localhost", 8025, "/websockets", null, WebSocketServer.class);
        
    	server.start();
    }
    
    public class
    SessionWrapper
    {
    	private final Session		session;
    	private final long			id;
    	
    	private boolean	destroyed;
    	
    	private
    	SessionWrapper(
    		Session		_session,
    		long		_id )
    	{
    		session	= _session;
    		id		= _id;
    	}
    	
    	public void
    	send(
    		String		str )
    		
    		throws Exception
    	{
    		session.getBasicRemote().sendText( str );
    	}
    	
    	private long
    	getID()
    	{
    		return( id );
    	}
    	
    	private void
    	destroy()
    	{
    		destroyed	= true;
    	}
    	
    	private boolean
    	isDestroyed()
    	{
    		return( destroyed );
    	}
    }
}
