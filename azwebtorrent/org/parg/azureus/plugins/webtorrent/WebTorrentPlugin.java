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


import java.io.File;
import java.net.URL;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.util.GeneralUtils;


public class 
WebTorrentPlugin 
	implements Plugin
{
	private static final long instance_id = RandomUtils.nextSecureAbsoluteLong();
		
	private LocalWebServer	web_server;
	private JavaScriptProxy	js_proxy;
		
	@Override
	public void 
	initialize(
		PluginInterface plugin_interface )
		
		throws PluginException 
	{
		try{
			js_proxy = JavaScriptProxyManager.getProxy( instance_id );

			TrackerProxy	tracker_proxy = 
				new TrackerProxy(
					new TrackerProxy.Listener()
					{
				    	public JavaScriptProxy.Offer
				    	getOffer(
				    		byte[]		hash,
				    		long		timeout )
				    	{
				    		return( js_proxy.getOffer( hash, timeout ));
				    	}
				    	
				    	public void
				    	gotAnswer(
				    		byte[]		hash,
				    		String		offer_id,
				    		String		sdp )
				    		
				    		throws Exception
				    	{
				    		js_proxy.gotAnswer( offer_id, sdp );
				    	}
				    	
				    	public void
				    	gotOffer(
				    		byte[]							hash,
				    		String							offer_id,
				    		String							sdp,
				    		JavaScriptProxy.AnswerListener 	listener )
				    		
				    		throws Exception
				    	{
				    		js_proxy.gotOffer( hash, offer_id, sdp, listener );
				    	}
					});
			
			web_server = new LocalWebServer( instance_id, js_proxy.getPort(), tracker_proxy );
										
			new AEThread2( "")
			{
				public void
				run()
				{
					try{
						String	url = "http://127.0.0.1:" + web_server.getPort() + "/index.html?id=" + instance_id;
						
						ProcessBuilder pb = GeneralUtils.createProcessBuilder( new File( "C:\\temp\\chrome-win32" ), new String[]{ "chrome.exe", url }, null );

						pb.start();
						
						// PlatformManagerFactory.getPlatformManager().createProcess( "C:\\temp\\chrome-win32\\chrome.exe " + url, false );
						
						//Utils.launch( new URL( url ));
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}.start();
		
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	


	
	public URL
	getProxyURL(
		URL		url )
		
		throws IPCException
	{
		if ( web_server == null || js_proxy == null ){
			
			throw( new IPCException( "Proxy unavailable" ));
		}
		
		try{
			return( new URL( "http://127.0.0.1:" + web_server.getPort() + "/?target=" + UrlUtils.encode( url.toExternalForm())));
			
		}catch( Throwable e ){
			
			throw( new IPCException( e ));
		}
	}
}
