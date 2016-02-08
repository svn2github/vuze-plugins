/*
 * Created on Feb 4, 2016
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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.server.Server;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.plugins.ipc.IPCInterface;


@ServerEndpoint("/vuze")
public class 
GenericWSServer 
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
	
	private static Map<String,ServerWrapper>		server_map	= new HashMap<>();
	private static boolean							unloaded;

	
	private Session				session;
	private ServerWrapper		server_wrapper;
	private URI					uri;
	
	private boolean				session_destroyed;
	
	
    private ServerWrapper 
    startServerInternal(
    	String 			host,
    	int				port,
    	String			context,
    	IPCInterface	ipc )
    
    	throws Exception
    {
    	if ( unloaded ){
    		
    		throw( new Exception( "Destroyed" ));
    	}
    	
    	ClassLoader old_loader = Thread.currentThread().getContextClassLoader();
    
    	try{
    			// need to do this as the tyrus/grizzly stuff explicitly uses the context class
    			// loader to instantiate things and this fails if it isn't the plugin one
    		
    		Thread.currentThread().setContextClassLoader( GenericWSServer.class.getClassLoader());
    		    	
	    	int[] potential_ports;
	    	
	    	if ( port == 0 ){
	    		
	    		potential_ports = new int[32];
	    		    	
		    	for ( int i=0;i<potential_ports.length;i++){
		    		
		    		potential_ports[i] = 2000 + (RandomUtils.nextAbsoluteInt()%60000);
		    	}
	    	}else{
	    		
	    		potential_ports = new int[]{ port };
	    	}
	    	
	    	Exception last_error = null;
	    	
	    	for ( int p: potential_ports ){
	    	
	    		try{
	    			Map<String,Object>	properties = new HashMap<>();
	    			
	    			Server server = new Server( host, p, context, properties, GenericWSServer.class);
	        
	    			server.start();
	    	
	    			return( new ServerWrapper( host, p, context, server, ipc ));
	    			
	    		}catch( DeploymentException e ){
	    			
	    			last_error = e;
	    		}
	    	}
	    	
	    	throw( last_error );
	    	
    	}finally{
    		
    		try{
    			Thread.currentThread().setContextClassLoader( old_loader );
    			
    		}catch( Throwable e ){
    			
    			Debug.out( e );
    		}
    	}
    }
    
	public ServerWrapper
	startServer(
		String				host,
		int					port,
		String				context,
		IPCInterface		ipc )
			
		throws Exception
	{
		synchronized( server_map ){
			
			ServerWrapper existing = server_map.get( context );
			
			if ( existing != null ){
				
				if ( port == 0 || port == existing.getPort()){
					
					return( existing );
				}
				
				throw( new Exception( "context already in use" ));
			}
		
			ServerWrapper server = startServerInternal( host, port, context, ipc );
			
			server_map.put( context, server );
			
			return( server );
		}
	}
	
	public void
	unload()
	{
		synchronized( server_map ){
			
			unloaded	= true;
			
			List<ServerWrapper> to_destroy = new ArrayList<>( server_map.values());
			
			for ( ServerWrapper sw: to_destroy ){
				
				try{
					sw.destroy();
					
				}catch( Throwable e ){

					Debug.out( e );
				}
			}
		}
	}
	
	public URI
	getURI()
	{
		return( uri );
	}
	
	@OnOpen
    public void 
    onOpen(
    	Session _session )
    			
    	throws IOException 
    {
		session	= _session;
		
		uri	= session.getRequestURI();
		
		String path = uri.getPath();
		
		String context = path.substring( 0, path.lastIndexOf( '/' ));
		
		synchronized( server_map ){
			
			server_wrapper = server_map.get( context );
			
			if ( server_wrapper == null ){
				
				destroySession();
				
				throw( new IOException( "Server not found for '" + context + "'" ));
			}
			
			server_wrapper.addSession( this );
		}
    }
	
    @OnMessage
    public void 
    onMessage(
    	ByteBuffer 	message ) 
    {
    	server_wrapper.receive( this, message );
    }
	
    @OnMessage
    public void 
    onMessage(
    	String 	message ) 
    {
    	server_wrapper.receive( this, message );
    }
    
    @OnError
    public void 
    onError(
    	Throwable e ) 
    {
    	Debug.out( e );
    	
    	destroySession();
    }

    @OnClose
    public void
    onClose(
    	Session session) 
    {
    	session_destroyed	= true;
    	
    	server_wrapper.removeSession( this );
    }
    
    public void
    stopServer(
    	ServerWrapper		server )
    {
    	server.destroy();
    }
    
    public void
    sendMessage(
    	ServerWrapper		server,
    	GenericWSServer		g_session,
    	ByteBuffer			buffer )
    	
    	throws Exception
    {
    	g_session.session.getBasicRemote().sendBinary( buffer );
    }
    
    public void
    sendMessage(
    	ServerWrapper		server,
    	GenericWSServer		g_session,
    	String				buffer )
    	
    	throws Exception
    {
    	g_session.session.getBasicRemote().sendText( buffer );
    }
    
    public void
    closeSession(
    	ServerWrapper		server,
    	GenericWSServer		session )
    {
    	server.removeSession( session );
    }
    
    protected void
    destroySession()
    {
    	session_destroyed	= true;
    	
    	try{
    		session.close();
    		
    	}catch( Throwable e ){
    		
    		Debug.out( e );
    	}
    	
    	server_wrapper.removeSession( this );
    }
    
	protected static class
	ServerWrapper
	{
		private final Server			server;
		private final String			host;
		private final int				port;
		private final String			context;
		
		private final IPCInterface		ipc;
		
		private final URL				url;
		
		private List<GenericWSServer>		sessions = new ArrayList<>();
		
		private boolean	server_destroyed;
		
		private
		ServerWrapper(
			String			_host,
			int				_port,
			String			_context,
			Server			_server,
			IPCInterface	_ipc )
		{
			host		= _host;
			port		= _port;
			context		= _context;
			server		= _server;
			
			URL		_url;
			
			try{
				
				_url = new URL( "ws://127.0.0.1:" + port + context + "/vuze" );
				
			}catch( Throwable e ){
				
				_url = null;
			}
			
			url			= _url;
			
			ipc			= _ipc;
		}

		public int
		getPort()
		{
			return( port );
		}

		public URL
		getTrackerURL()
		{
			return( url );
		}
		
		private void
		addSession(
			GenericWSServer		session )
		{
			synchronized( sessions ){
				
				if ( server_destroyed ){
					
					session.destroySession();
					
					return;
				}
				
				sessions.add( session );
			}
			
			try{
				ipc.invoke( "sessionAdded", new Object[]{ this, session.getURI(), session });
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		private void
		removeSession(
			GenericWSServer		session )
		{
			boolean	found;
			
			synchronized( sessions ){
				
				found = sessions.remove( session );
			}
			
			if ( found ){
				
				try{
					ipc.invoke( "sessionRemoved", new Object[]{ this, session });
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		private void
		receive(
			GenericWSServer	session,
			ByteBuffer		buffer )
		{
			try{
				ipc.invoke( "messageReceived", new Object[]{ this, session, buffer });
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		private void
		receive(
			GenericWSServer	session,
			String			buffer )
		{
			try{
				ipc.invoke( "messageReceived", new Object[]{ this, session, buffer });
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		public void
		destroy()
		{
			List<GenericWSServer>		to_close = new ArrayList<>();
			
			synchronized( sessions ){
				
				server_destroyed	= true;
				
				to_close.addAll( sessions );
				
				// don't clear sessions, this will be tidied as the sessions are closed below
			}
			
			for ( GenericWSServer s: to_close ){
				
				try{
					s.destroySession();
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			synchronized( server_map ){
				
				try{
					server.stop();
	
				}catch( Throwable e ){
	
					Debug.out( e );
					
				}finally{
			
					server_map.remove( context );
				}
			}
		}
	} 
}
