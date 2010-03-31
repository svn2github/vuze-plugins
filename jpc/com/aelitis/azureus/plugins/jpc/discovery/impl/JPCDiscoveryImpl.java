/*
 * Created on 08-Feb-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.jpc.discovery.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.plugins.jpc.JPCException;
import com.aelitis.azureus.plugins.jpc.JPCPlugin;
import com.aelitis.azureus.plugins.jpc.discovery.*;

/**
 * @author parg
 *
 */

public class 
JPCDiscoveryImpl
	implements JPCDiscovery
{
	private JPCDiscoveryAdapter		adapter;
	
	private InetAddress				public_address;
	
	private InetSocketAddress		cache_address;	
	private InetSocketAddress		informed_cache_address;	
	
	private JPCPlugin 				jpcPlugin;
	
	private UTTimer					timer;
	
    private static final String TEST_ADDRESS = "dev2.skype.net";  //195.50.208.66
	private static final int CACHE_PORT = 1215;
	private static final int DISCOVERY_REFRESH_TIME = 2 * 24 * 60 * 60; //2 days
	
	public
	JPCDiscoveryImpl(
		JPCPlugin 				_jpcPlugin,
		JPCDiscoveryAdapter		_adapter )
	
		throws JPCException
	{
		adapter			= _adapter;
		jpcPlugin 		= _jpcPlugin;

			// we always have to get access to the public address of the machine as it is required
			// during 
		
		public_address = jpcPlugin.getPluginInterface().getUtilities().getPublicAddress();
		
		jpcPlugin.log("Public Address : " + public_address ,JPCPlugin.LOG_DEBUG);
		
		
		if ( public_address == null ){
			
			throw( new JPCException( "Can't determine the public address of the computer" ));
		}

		discoverCache();
	}
	
	protected void
	discoverCache()
	{
		InetAddress discovery_address 	= null;
		
		boolean		newly_discovered	= true;
		
		try{
			
			PluginConfig config = jpcPlugin.getPluginInterface().getPluginconfig();
			
			int lastDiscoveryTime = config.getPluginIntParameter("lastJPCDiscoveryTime",0);
      
            if( JPCPlugin.USE_TEST_CACHE )  lastDiscoveryTime = 0;
			
			String lastDiscoveryAddress = config.getPluginStringParameter("lastJPCDiscoveryAddress","");
			
			int currentTime = (int) (System.currentTimeMillis() / 1000);	
			
			jpcPlugin.log("Loaded from config : last update : " + lastDiscoveryTime + " , lastAddress : " + lastDiscoveryAddress + " (current time : " + currentTime + " )",JPCPlugin.LOG_DEBUG);
			
			if (	lastDiscoveryAddress != null && 
					! lastDiscoveryAddress.equals("") 
					&& (currentTime - lastDiscoveryTime) < DISCOVERY_REFRESH_TIME) {
			  
				try{
			  	
					jpcPlugin.log("Using saved address for the cache",JPCPlugin.LOG_DEBUG);
			    
					discovery_address =  InetAddress.getByName(lastDiscoveryAddress);
			    			    
					newly_discovered	= false;
					
				}catch( UnknownHostException e ){
			  	
					jpcPlugin.log("Unknown Host :" + discovery_address, JPCPlugin.LOG_DEBUG);
				}
			}
			  
			if ( discovery_address == null ){
				
				try {
				  discovery_address = InetAddress.getByName("btcache.p2p");
				} catch (UnknownHostException e) {		
				  jpcPlugin.log("Unknown Host : btcache.p2p",JPCPlugin.LOG_DEBUG);
				}
			}
			
			
			if ( discovery_address == null ){
				
					//Cache not found yet, we need to use the public IP.
				
				String public_name	= jpcPlugin.getPluginInterface().getUtilities().reverseDNSLookup( public_address );
				
				if ( public_name == null ){
					
					jpcPlugin.log( "Can't perform reverse-dns lookup of '" + public_address + "'",JPCPlugin.LOG_PUBLIC );
					
				}else{
				
					jpcPlugin.log("Public Name : " + public_name,JPCPlugin.LOG_DEBUG);
					
					StringTokenizer st = new StringTokenizer(public_name,".");
					
					int nbSubDomains = st.countTokens();
					
					String subDomains[] = new String[nbSubDomains];
					
					int i = 0;
					
					while(st.hasMoreElements()) {
						
					  subDomains[i++] = st.nextToken();
					}
					
					i = 0 ;
					
					while(i < (nbSubDomains - 1) && discovery_address == null) {
					  String host = constructDiscoveryAddress(i,subDomains);
					  try {		    
					    jpcPlugin.log("Looking for : " + constructDiscoveryAddress(i,subDomains),JPCPlugin.LOG_DEBUG);
					    discovery_address = InetAddress.getByName(host);
					  } catch(UnknownHostException e) {		 
					    jpcPlugin.log("Unknown Host : " + host ,JPCPlugin.LOG_DEBUG);
					  }
					  i++;
					}
				}
			}
			
			
			/*
			 * For debug purposes only
			 */
			
			if ( discovery_address == null && JPCPlugin.USE_TEST_CACHE ){
				
			  try{
				  discovery_address = InetAddress.getByName( TEST_ADDRESS );
				  
			  }catch(UnknownHostException e) {			  
			  }
			}
		}finally{
			
			setCacheAddress( discovery_address, newly_discovered );
		}
	}
	
	private String 
	constructDiscoveryAddress(
			int start,
			String[] subDomains) 
	{
	  StringBuffer buffer = new StringBuffer();
	  buffer.append("btcache.p2p");
	  for(int i = start ; i < subDomains.length ; i++) {
	    buffer.append(".");
	    buffer.append(subDomains[i]);
	  }
	  
	  return buffer.toString();
	}
	
	private void 
	setCacheAddress(
		InetAddress address,
		boolean 	persist) 
	{
		try{
			if( address != null ) {
				
				jpcPlugin.log("Cache Found at address : " + address.getHostName(),JPCPlugin.LOG_PUBLIC);
			  
			  	cache_address = new InetSocketAddress(address,CACHE_PORT);
				
				if(persist){ 
				  	jpcPlugin.log("Persisting cache information.",JPCPlugin.LOG_DEBUG);
					int currentTime = (int) (System.currentTimeMillis() / 1000);
					PluginConfig config = jpcPlugin.getPluginInterface().getPluginconfig();
					config.setPluginParameter("lastJPCDiscoveryTime",currentTime);
					config.setPluginParameter("lastJPCDiscoveryAddress",address.getHostName());
					try { 
					  config.save();
					} catch(Exception e) {
					  jpcPlugin.log("Error while saving cache information : " + e.getMessage(),JPCPlugin.LOG_DEBUG);
					}
				}
		  	}else{
		  		
		  	  jpcPlugin.log("Cache Not Found",JPCPlugin.LOG_PUBLIC);
		  	  
		  	  cache_address = null;
		  	}
		  	  
			if ( timer == null) {
				
			  timer = jpcPlugin.getPluginInterface().getUtilities().createTimer("JPC Discovery Refresh");
			  
			  PluginConfig config = jpcPlugin.getPluginInterface().getPluginconfig();
			  
			  int currentTime = (int) (System.currentTimeMillis() / 1000);
			  
			  int lastCheck = config.getPluginIntParameter("lastJPCDiscoveryTime",currentTime);
			  
			  long waitTime = (lastCheck - currentTime + DISCOVERY_REFRESH_TIME) * 1000; // s -> ms
			  
			  if(waitTime < 0) waitTime = DISCOVERY_REFRESH_TIME * 1000;
			  
			  jpcPlugin.log("Adding a refresh timer on " + jpcPlugin.getPluginInterface().getUtilities().getFormatters().formatDate(waitTime +((long)currentTime*1000l)) + " (" + waitTime/1000 + "s).",JPCPlugin.LOG_DEBUG);
			  
			  timer.addPeriodicEvent(waitTime,new UTTimerEventPerformer() {
		        public void perform(UTTimerEvent event) {
		          timer.destroy();
		          timer = null;
		      
		          discoverCache();
		        }
			  });
			}
		}finally{
		
				// inform the adapter if the cache address has changed
			
			if ( informed_cache_address == null ||
					( 	cache_address != null &&
						!informed_cache_address.equals( cache_address ))){
				
				jpcPlugin.log( "Informing adapter of cache address", JPCPlugin.LOG_DEBUG );
				
				informed_cache_address	= cache_address;
				
				adapter.cacheDiscovered(this,false);
			}else{
				
				adapter.cacheDiscovered(this,true);

				jpcPlugin.log( "Informing adapter that cache address hasn't changed", JPCPlugin.LOG_DEBUG );

			}
		}
	}
	
	public InetAddress
	getPublicAddress()
	{
		return( public_address );
	}
	
	public InetSocketAddress
	getCacheAddress()
	{
		return( cache_address );
	}
}
