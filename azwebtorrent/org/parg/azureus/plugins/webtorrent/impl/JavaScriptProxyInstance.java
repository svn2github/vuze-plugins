/*
 * Created on Jan 6, 2016
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


package org.parg.azureus.plugins.webtorrent.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.server.Server;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.util.JSONUtils;

@ServerEndpoint("/vuze")
public class 
JavaScriptProxyInstance 
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
	
	private static Listener		listener;
	
    protected static void 
    startServer(
    	Listener		_listener ) 
    
    	throws Exception
    {
    	listener	= _listener;
    	
    	Server server = new Server("localhost", 8025, "/websockets", null, JavaScriptProxyInstance.class);
        
    	server.start();
    }
    

    private long		instance_id;
    private Session		session;
    private boolean		destroyed;
    
	protected long
	getInstanceID()
	{
		return( instance_id );
	}
	
	protected boolean
	isDestroyed()
	{
		return( destroyed );
	}
	
	@OnOpen
    public void 
    onOpen(
    	Session _session )
    			
    	throws IOException 
    {
		session = _session;
		
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
			
			instance_id	= id;
			
			listener.instanceCreated( this );
		}
    }

    @OnMessage
    public String 
    onMessage(
    	String message) 
    {
    	Map<String,Object> map = JSONUtils.decodeJSON( message );
    	
    	Map<String,Object> result = listener.receiveMessage( this, map );
    	
    	return( JSONUtils.encodeToJSON( result ));
    }

    protected void
    sendMessage(
    	Map<String,Object>			message )
    	
    	throws Throwable
    {
    	if ( isDestroyed()){
    		
    		throw( new Exception( "Destroyed" ));
    	}
    	
    	try{
    		session.getBasicRemote().sendText( JSONUtils.encodeToJSON( message ));
    		
    	}catch( Throwable e ){
    		
    		onError( e );
    		
    		throw( e );
    	}
    }
    
    @OnError
    public void 
    onError(
    	Throwable e ) 
    {
    	Debug.out( e );
    	
    	destroy();
    	
    	listener.instanceDestroyed( this );
    }

    @OnClose
    public void
    onClose(
    	Session session) 
    {
    	destroyed	= true;
    	
    	listener.instanceDestroyed( this );
    }
    
    protected void
    destroy()
    {
    	destroyed	= true;
    	
    	try{
    		session.close();
    		
    	}catch( Throwable e ){
    		
    		Debug.out( e );
    	}
    }
    
    protected interface
    Listener
    {
    	public void
    	instanceCreated(
    		JavaScriptProxyInstance		inst );
    	
    	public Map<String,Object>
    	receiveMessage(
    		JavaScriptProxyInstance		inst,
    		Map<String,Object>			message );
    	
    	public void
    	instanceDestroyed(
    		JavaScriptProxyInstance		inst );
    }
}
