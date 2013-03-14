/*
 * Created on Mar 13, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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


package com.aelitis.azureus.plugins.xmwebui.client.proxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking.TRNonBlockingServer;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking.TRNonBlockingServerProcessor;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking.TRNonBlockingServerProcessorFactory;
import org.gudy.azureus2.core3.util.AsyncController;
import org.gudy.azureus2.core3.util.FileUtil;
import org.json.simple.JSONObject;

import com.aelitis.azureus.plugins.xmwebui.client.rpc.XMRPCClient;
import com.aelitis.azureus.plugins.xmwebui.client.rpc.XMRPCClientFactory;
import com.aelitis.azureus.util.JSONUtils;

public class 
XMClientProxy 
{
	private File	resource_dir = new File( "C:\\Projects\\Development\\xmwebui\\transmission\\web" );
		
	private XMRPCClient	rpc;
	
	private
	XMClientProxy(
		XMRPCClient		_rpc )
	{
		rpc 	= _rpc;
		
		try{
			new TRNonBlockingServer( 
					"VersionServer", 
					5666, 
					InetAddress.getByName( "127.0.0.1" ), 
					false,
					new TRNonBlockingServerProcessorFactory()
					{
						public TRNonBlockingServerProcessor
						create(
							TRTrackerServerTCP		_server,
							SocketChannel			_socket )
						{
							return( new NonBlockingProcessor( _server, _socket ));
						}
					});
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private class
	NonBlockingProcessor
		extends TRNonBlockingServerProcessor
	{
		protected
		NonBlockingProcessor(
			TRTrackerServerTCP		_server,
			SocketChannel			_socket )
		{
			super( _server, _socket );
		}
		
		protected ByteArrayOutputStream 
		process(
			String 						input_header, 
			String 						lowercase_input_header, 
			String 						url_path, 
			final InetSocketAddress 	client_address, 
			boolean 					announce_and_scrape_only, 
			InputStream 				is,
			AsyncController				async )
		
			throws IOException 
		{
			ByteArrayOutputStream result	= new ByteArrayOutputStream( 1024 );

			System.out.println( url_path );
			
			System.out.println( input_header );
			
			String[] lines = input_header.split( "\n" );
			
			Map<String,String>		headers = new LinkedHashMap<String,String>();
			
			for ( String line: lines ){
				
				line = line.trim();
				
				int	pos = line.indexOf( ':' );
				
				if ( pos != -1 ){
					
					headers.put( line.substring( 0, pos ).trim().toLowerCase(), line.substring( pos+1).trim());
				}
			}

			try{
				
				ByteArrayOutputStream data	= new ByteArrayOutputStream( 1024 );
	
				
				if ( url_path.startsWith( "/transmission/rpc" )){
					
					JSONObject rcp_request = new JSONObject(); 
						
					rcp_request.putAll( JSONUtils.decodeJSON( new String( getPostData(), "UTF-8" )));
					
					JSONObject rpc_reply = rpc.call( rcp_request );
					
					data.write( JSONUtils.encodeToJSON( rpc_reply ).getBytes( "UTF-8" ));
					
				}else if ( url_path.startsWith( "/psearch/" )){
					
					XMRPCClient.HTTPResponse resp = rpc.call( "GET", url_path, headers, null );
					
					data.write( resp.getData());
					
				}else{
					
					if ( url_path.equals( "/" )){
						
						url_path = "/index.html";
					}
					
					File file = new File( resource_dir, url_path.substring(1).replace( "/", File.separator ));
					
					file = file.getCanonicalFile();
					
					System.out.println( file + " -> " + file.exists());
					
					data.write( FileUtil.readFileAsByteArray( file ));
				}
				
				byte[] bytes = data.toByteArray();
				
	
	
				result.write((
					"HTTP/1.1 200 OK" + NL + 
					"Content-length: " + bytes.length + NL + 
					NL ).getBytes());
				
				result.write( bytes );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				result.write((
						"HTTP/1.1 404 Not Found" + NL + "Content-length: 0" + NL + NL ).getBytes()); 
			}
			
			String ka = headers.get( "keep-alive" );
			
			setKeepAlive( ka == null || !ka.equalsIgnoreCase( "close" ));
			
			return( result );
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			//XMRPCClient	rpc = XMRPCClientFactory.createDirect( false, "127.0.0.1", 9091, args[0], args[1] );
			XMRPCClient	rpc = XMRPCClientFactory.createIndirect( args[0] );

			new XMClientProxy( rpc );
			
			while( true ){
				
				Thread.sleep(1000);
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
