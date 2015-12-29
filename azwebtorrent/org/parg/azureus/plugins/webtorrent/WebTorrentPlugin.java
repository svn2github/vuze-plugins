/*
 * Created on Dec 18, 2015
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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.core3.util.ThreadPoolTask;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ipc.IPCException;

public class 
WebTorrentPlugin 
	implements Plugin
{
	private int filter_port;
	
	@Override
	public void 
	initialize(
		PluginInterface plugin_interface )
		
		throws PluginException 
	{
		setupServer();
		
		setupURLHandler();
	}
	
	private void
	setupServer()
	{
		try{
			WebSocketServer server = new WebSocketServer();
			
			server.runServer();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	private void
	setupURLHandler()
	{
		try{
			final ThreadPool thread_pool = new ThreadPool( "WebSocketPlugin", 32 );
			
		  	String	connect_timeout = System.getProperty("sun.net.client.defaultConnectTimeout"); 
		  	String	read_timeout 	= System.getProperty("sun.net.client.defaultReadTimeout"); 
		  			
		  	int	timeout = Integer.parseInt( connect_timeout ) + Integer.parseInt( read_timeout );
			
			thread_pool.setExecutionLimit( timeout );
		
			final ServerSocket ss = new ServerSocket( 0, 1024, InetAddress.getByName("127.0.0.1"));
			
			filter_port	= ss.getLocalPort();
			
			ss.setReuseAddress(true);
							
			new AEThread2("WebSocketPlugin::filterloop")
			{
				public void
				run()
				{
					long	failed_accepts		= 0;
	
					while(true){
						
						try{				
							Socket socket = ss.accept();
									
							failed_accepts = 0;
							
							thread_pool.run( new HttpFilter( socket ));
							
						}catch( Throwable e ){
							
							failed_accepts++;
													
							if ( failed_accepts > 10  ){
	
									// looks like its not going to work...
									// some kind of socket problem
												
								Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
									LogAlert.AT_ERROR, "Network.alert.acceptfail"),
									new String[] { "" + filter_port, "TCP" });
													
								break;
							}
						}
					}
				}
			}.start();
			
		}catch( Throwable e){
		
			Debug.out( e );
		}	
	}
	
	public URL
	getProxyURL(
		URL		url )
		
		throws IPCException
	{
		if ( filter_port == 0 ){
			
			throw( new IPCException( "Proxy unavailable" ));
		}
		
		try{
			return( new URL( "http://127.0.0.1:" + filter_port + "/?target=" + UrlUtils.encode( url.toExternalForm())));
			
		}catch( Throwable e ){
			
			throw( new IPCException( e ));
		}
	}
	
	private class
	HttpFilter
		extends ThreadPoolTask
	{
		private final String NL = "\r\n";
		
		private Socket		socket;
		
		protected
		HttpFilter(
			Socket		_socket )
		{
			socket	= _socket;
		}
		
		public void
		runSupport()
		{
			try{						
				setTaskState( "reading header" );
										
				InputStream	is = socket.getInputStream();
				
				byte[]	buffer = new byte[1024];
				
				String	header = "";
				
				while(true ){
						
					int	len = is.read(buffer);
						
					if ( len == -1 ){
					
						break;
					}
									
					header += new String( buffer, 0, len, Constants.BYTE_ENCODING );
									
					if ( 	header.endsWith( NL+NL ) ||
							header.indexOf( NL+NL ) != -1 ){
						
						break;
					}
				}
				
				List<String>	lines = new ArrayList<String>();
				
				int	pos = 0;
				
				while( true){
					
					int	p1 = header.indexOf( NL, pos );
					
					String	line;
					
					if ( p1 == -1 ){
						
						line = header.substring(pos);
						
					}else{
											
						line = header.substring( pos, p1 );
					}
					
					line = line.trim();
					
					if ( line.length() > 0 ){
					
						lines.add( line );
					}
				
					if ( p1 == -1 ){
						
						break;
					}
					
					pos = p1+2;
				}
				
				
				String[]	lines_in = new String[ lines.size()];
				
				lines.toArray( lines_in );
				
				String	get = lines_in[0];
				
				System.out.println( "Got request: " + get );
				
			}catch( Throwable e ){
				
			}finally{
				
				try{
					socket.close();
					
				}catch( Throwable f ){
					
				}
			}
		}
		
		public void
		interruptTask()
		{
			try{	
				socket.close();
				
			}catch( Throwable e ){
				
			}
		}
	}
}
