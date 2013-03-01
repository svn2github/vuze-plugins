/*
 * Created on Feb 28, 2013
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


package com.aelitis.azureus.plugins.xmwebui.client.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class 
XMRPCClientUtils 
{
	private static String session_id;
	
	protected static String
	getFromURL(
		String		url )
	
		throws XMRPCClientException
	{
		try{
			HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
			
			connection.setRequestProperty( "Connection", "Keep-Alive" );
			
			InputStream is = connection.getInputStream();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream( 10000 );
			
			byte[]	buffer = new byte[8*1024];
			
			while( true ){
			
				int len = is.read( buffer );
				
				if ( len <= 0  ){
					
					break;
				}
				
				baos.write( buffer, 0, len );
			}
			
			String	result = new String( baos.toByteArray(), "UTF-8" );
			
			System.out.println( url + " -> " + result );
			
			return( result );
			
		}catch( IOException e ){
			
			throw( new XMRPCClientException( "HTTP GET for " + url + " failed", e ));
		}
	}
	
	protected static byte[]
	postToURL(
		String		url,
		byte[]		payload )
	
		throws XMRPCClientException
	{
		try{
			for ( int i=0; i<2; i++ ){
				
				HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
				
				connection.setRequestMethod( "POST" );
				
				connection.setRequestProperty( "Connection", "Keep-Alive" );
				
				if ( session_id != null ){
					
					connection.setRequestProperty( "X-Transmission-Session-Id", session_id );
				}
				
				connection.setReadTimeout( 5*60*1000 );
				
				connection.setDoOutput( true );
				
				connection.getOutputStream().write( payload );
				
				try{
					InputStream is = connection.getInputStream();
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream( 10000 );
					
					byte[]	buffer = new byte[8*1024];
					
					while( true ){
					
						int len = is.read( buffer );
						
						if ( len <= 0  ){
							
							break;
						}
						
						baos.write( buffer, 0, len );
					}
					
					return( baos.toByteArray());
					
				}catch( IOException e ){
					
					if ( connection.getResponseCode() == 409 ){
						
						String str = connection.getHeaderField( "x-transmission-session-id" );
						
						if ( str != null && ( session_id == null || !session_id.equals( str ))){
							
							session_id = str;
							
							continue;
						}
					}
					
					throw( e );
				}
			}
			
			return( null );
			
		}catch( IOException e ){
			
			throw( new XMRPCClientException( "HTTP POST for " + url + " failed", e ));
		}
	}
}
