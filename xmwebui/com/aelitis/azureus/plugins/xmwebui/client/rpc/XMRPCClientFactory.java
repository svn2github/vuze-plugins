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

import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class 
XMRPCClientFactory 
{
    static{
    	try{
    			//general hacks to support self-signed SSL certs
    		
			TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}
						public void checkClientTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}
						public void checkServerTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}
					}
				};
		
			SSLContext sc = SSLContext.getInstance("SSL");
			
			sc.init( null, trustAllCerts, new SecureRandom());
			
			SSLSocketFactory factory = sc.getSocketFactory();
			
			HttpsURLConnection.setDefaultSSLSocketFactory( factory );
			
			HttpsURLConnection.setDefaultHostnameVerifier(
				new HostnameVerifier()
				{
					public boolean
					verify(
							String		host,
							SSLSession	session )
					{
						return( true );
					}
				});
			
    	}catch( Throwable e ){
    		
    		e.printStackTrace();
    	}
    }
    
	public static XMRPCClient
	createDirect(
		boolean		http,
		String		host,
		int			port,
		String		username,
		String		password )
	{
		return( new XMRPCClientDirect( http, host, port, username, password ));
	}
	
	public static XMRPCClient
	createIndirect(
		String	access_code )
	{
		return( new XMRPCClientIndirect( access_code ));
	}
	
	public static XMRPCClient
	createTunnel(
		String	access_code,
		String	tunnel_user,
		String	tunnel_password )
	{
		return( new XMRPCClientTunnel( access_code, tunnel_user, tunnel_password ));
	}
}