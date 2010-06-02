/*
 * Created on Sep 16, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.xmwebui;

import java.util.*;
import java.net.*;
import java.io.*;

import org.json.simple.JSONObject;

import com.aelitis.azureus.util.JSONUtils;

public class 
XMRPCClient 
{
	public static void
	main(
		String[]		args )
	{
		try{
			HttpURLConnection connection = (HttpURLConnection)new URL( "http://192.168.0.125:9091/transmission/rpc" ).openConnection();
			
			connection.setRequestMethod( "POST" );
			
			connection.setDoOutput( true );
			
			PrintWriter pw = new PrintWriter( new OutputStreamWriter( connection.getOutputStream(), "UTF-8" ));
			
			Map request = new JSONObject();
			
			/*
			request.put( "method", "torrent-get" );
			
			Map	arg_map = new HashMap();
			
			request.put( "arguments", arg_map );
			
			List fields = new ArrayList();
			
			arg_map.put( "fields", fields );
			
			fields.add( "addedDate" );
			fields.add( "announceURL" );
			fields.add( "comment" );
			fields.add( "creator" );
			fields.add( "dateCreated" );
			fields.add( "downloadedEver" );
			fields.add( "error" );
			fields.add( "errorString" );
			fields.add( "eta" );
			fields.add( "hashString" );
			fields.add( "haveUnchecked" );
			fields.add( "haveValid" );
			fields.add( "id" );
			fields.add( "isPrivate" );
			fields.add( "leechers" );
			fields.add( "leftUntilDone" );
			fields.add( "name" );
			fields.add( "peersConnected" );
			fields.add( "peersGettingFromUs" );
			fields.add( "peersSendingToUs" );
			fields.add( "rateDownload" );
			fields.add( "rateUpload" );
			fields.add( "seeders" );
			fields.add( "sizeWhenDone" );
			fields.add( "status" );
			fields.add( "swarmSpeed" );
			fields.add( "totalSize" );
			fields.add( "uploadedEver" );

			
			request.put( "tag", "1234" );
			*/
			
			// {"method":"torrent-add","arguments":{"paused":"true","filename":"http://www.mininova.org/get/2963304"}}
			
			request.put( "method", "torrent-add" );
			
			Map	arg_map = new HashMap();
			
			request.put( "arguments", arg_map );

			arg_map.put( "paused", "true");
			arg_map.put( "filename", "http://www.mininova.org/get/2963304" );
			
			pw.println( JSONUtils.encodeToJSON( request ));
			
			pw.flush();
			
			LineNumberReader lnr = new LineNumberReader( new InputStreamReader( connection.getInputStream(), "UTF-8" ));
			
			StringBuffer	request_json_str = new StringBuffer(2048);
			
			while( true ){
				
				String	line = lnr.readLine();
				
				if ( line == null ){
					
					break;
				}
				
				request_json_str.append( line );
			}
			
			System.out.println( request_json_str.toString());
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
