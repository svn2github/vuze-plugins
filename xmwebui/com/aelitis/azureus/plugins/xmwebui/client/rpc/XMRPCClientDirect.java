/*
 * Created on Mar 1, 2013
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.aelitis.azureus.util.JSONUtils;

public class 
XMRPCClientDirect
	implements XMRPCClient
{
	private boolean	http;
	private String 	host;
	private int		port;
	
	private String	username;
	private String	password;
	
	public 
	XMRPCClientDirect(
		boolean	_http,
		String	_host,
		int		_port,
		String	_username,
		String	_password )
	{
		http		= _http;
		host		= _host;
		port		= _port;
		username	= _username;
		password	= _password;
	}
	
	public JSONObject
	call(
		JSONObject		request )
	
		throws XMRPCClientException
	{
		try{
			String url = (http?"http":"https") + "://" + host + ":" + port + "/transmission/rpc";
			
			String json = JSONUtils.encodeToJSON( request );
			
			byte[] reply = XMRPCClientUtils.postToURL( url , json.getBytes( "UTF-8" ), username, password );
			
			Map m = JSONUtils.decodeJSON( new String( reply, "UTF-8" ));
			
			JSONObject result = new JSONObject();
			
			result.putAll( m );
			
			return( result );
			
		}catch( IOException e ){
			
			throw( new XMRPCClientException( "unexpected" ));
		}	
	}
	
	public HTTPResponse 
	call(
		String 					method, 
		String 					url,
		Map<String, String> 	headers_in, 
		byte[] 					input_data)
	
		throws XMRPCClientException 
	{
		if ( method.equals( "GET" )){
			
			url = (http?"http":"https") + "://" + host + ":" + port + url;
	
			Map<String,String>	headers_out = new HashMap<String, String>();

			byte[] output_data = XMRPCClientUtils.getFromURL( url, headers_in, headers_out, username, password );
			
			return( XMRPCClientUtils.createHTTPResponse( headers_out, output_data, 0 ));
			
		}else{
			
			throw( new XMRPCClientException( "Not supported" ));
		}
	}
	
	public void
	destroy()
	{
		
	}
}
