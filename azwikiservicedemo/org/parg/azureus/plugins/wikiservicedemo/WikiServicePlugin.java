/*
 * Created on Apr 24, 2015
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


package org.parg.azureus.plugins.wikiservicedemo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.webplugin.WebPlugin;

public class 
WikiServicePlugin 
	extends WebPlugin
{
	public static final int DEFAULT_PORT    = 23144;

	protected static Properties defaults = new Properties();

	static{

		defaults.put( WebPlugin.CONFIG_PORT, new Integer( DEFAULT_PORT ));
		defaults.put( WebPlugin.PR_HIDE_RESOURCE_CONFIG, true );
		defaults.put( WebPlugin.PR_ENABLE_KEEP_ALIVE, true );
	}

	public
	WikiServicePlugin()
	{
		super( defaults );
	}

	public void 
	initialize(
		PluginInterface _plugin_interface) 

		throws PluginException 
	{
		super.initialize( _plugin_interface );
		
		LocaleUtilities loc_util = _plugin_interface.getUtilities().getLocaleUtilities();
		
		getConfigModel().setLocalizedName( loc_util.getLocalisedMessageText( "azwikiservicedemo.name" ));
	}

	public boolean
	generateSupport(
		TrackerWebPageRequest       request,
		TrackerWebPageResponse      response )

		throws IOException
	{
		String current_page = null;
		
		String url = request.getURL();

		int pos = url.indexOf( '?' );
		
		if ( pos != -1 ){
			
			String	modified_args = "";
			
			String[] args = url.substring( pos + 1 ).split( "&" );
			
			for ( String arg: args ){
				
				String[] bits = arg.split( "=" );
				
				boolean skip = false;
				
				if ( bits.length == 2 ){
					
					if ( bits[0].equals( "formcurrentpage" )){
						
						current_page = URLDecoder.decode( bits[1], "UTF-8" );
						
						skip = true;
					}
				}
				
				if ( !skip ){
					
					modified_args += (modified_args.length()==0?"":"&") + arg;
				}
			}
			
			url = url.substring( 0, pos+1 ) + modified_args;
		}
		
		if ( current_page == null ){
			
			String cookies = (String)request.getHeaders().get("cookie");
			
			if ( cookies != null ){
				
				String[] cookie_list = cookies.split( ";" );
				
				for ( String cookie: cookie_list ){
					
					String[] bits = cookie.split( "=" );
					
					if ( bits.length == 2 ){
						
						if ( bits[0].trim().equals( "X-Wiki-Page" )){
							
							current_page = URLDecoder.decode( bits[1].trim(), "UTF-8" );
						}
					}
				}
			}
		}
		
		//System.out.println( "GET: " + url + " - " + request.getHeaders());
		
		if ( url.contains( ".php?" )){
				
			String target = "http://wiki.vuze.com" + url;

			HttpURLConnection connection = (HttpURLConnection)new URL( target ).openConnection();
			
			InputStream	 is = connection.getInputStream();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream( 32*1024 );
			
			byte[] buffer = new byte[2048];
			
			while( true ){
				
				int len = is.read( buffer );
				
				if ( len == -1 ){
					
					break;
				}
				
				baos.write( buffer, 0, len );
			}
			
			
			String content = new String( baos.toByteArray(), "UTF-8" );
			
			String redirect_to;
			
			if ( current_page == null ){
				
				redirect_to = "http://wiki.vuze.com/";
				
			}else{
				
				URL u = new URL( current_page );
				
				redirect_to = "http://" + u.getHost() + ":" + u.getPort() + "/wiki.vuze.com/";
			}
						
			Pattern p = Pattern.compile("(?i)(src|href)=\"/([^\"]+)\"" );

			Matcher m = p.matcher( content );

			boolean result = m.find();

			if ( result ){

				StringBuffer sb = new StringBuffer();

		    	while( result ){
		    		
		    		String	type = m.group(1);
		    		String 	str = m.group(2);
		    		 	    		
		    		if ( str.contains( ".php?" )){
		    			 
		    			m.appendReplacement(sb, type + "=\"" + "/" + str + "\"" );
		    			 
		    		}else{

		    			if ( str.contains( "/images/thumb/" )){
		    				
		    				m.appendReplacement(sb, type + "=\"" + "http://wiki.vuze.com/" + str + "\"" );
		    				
		    			}else{
		    				
			    			int x = str.lastIndexOf( "/" );
	
			    			if ( x != -1 ){
	
			    				String tail = str.substring( x+1 );
	
			    				str = str.replaceAll( ":", "_" );
	
			    				if ( !tail.contains( "." )){
	
			    					str += ".html";
			    				}
			    			}
	
			    			m.appendReplacement(sb, type + "=\"" + redirect_to + str + "\"" );
		    			}
		    		}

		    		result = m.find(); 
		    	}

				m.appendTail(sb);

				content = sb.toString();
			}
			
			if ( current_page != null ){
			
				response.setHeader( "Set-Cookie", "X-Wiki-Page=" + URLEncoder.encode( current_page, "UTF-8" )+ "; path=/; HttpOnly" );
			}
			
			response.getOutputStream().write( content.getBytes( "UTF-8" ));
			
		}else{
			
			String target;
			
			if ( current_page == null ){
				
				target = "http://wiki.vuze.com" + url;
				
			}else{
				
				URL u = new URL( current_page );
				
				target = "http://" + u.getHost() + ":" + u.getPort() + url;
			}

			//System.out.println( "redirect to " + target );
			
			response.getRawOutputStream().write((
				"HTTP/1.1 302 Found" + NL +
				"Location: " + target + NL +
				NL ).getBytes( "UTF-8" ));
			
		}
		
		return( true );
	}
}
