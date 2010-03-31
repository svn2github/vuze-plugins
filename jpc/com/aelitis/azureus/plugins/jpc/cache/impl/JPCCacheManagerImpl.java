/*
 * Created on Feb 25, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.jpc.cache.impl;


import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.utils.Monitor;

import com.aelitis.azureus.plugins.jpc.*;
import com.aelitis.azureus.plugins.jpc.cache.*;
import com.aelitis.azureus.plugins.jpc.discovery.*;



/**
 *
 */
public class JPCCacheManagerImpl implements JPCCacheManager {
  private final JPCPlugin jpc_plugin;

  private final HashMap upload_caches = new HashMap();
  private final Monitor upload_caches_mon;
  
  private JPCCacheDownloader download_cache = null;

  
  public JPCCacheManagerImpl( JPCPlugin plugin ) {
    this.jpc_plugin = plugin;
    upload_caches_mon = jpc_plugin.getPluginInterface().getUtilities().getMonitor();
  }
  


  public void connectLocalDownloadCache( final JPCCacheDownloaderAdapter adapter ) {
    //try and locate a local download cache server
    jpc_plugin.getPluginInterface().getUtilities().createThread( "JPCLocalCacheConnect", new Runnable() {
      public void run() {
        try {
          JPCDiscoveryFactory.create( jpc_plugin, new JPCDiscoveryAdapter() {
            
            public void cacheDiscovered( final JPCDiscovery discovery, boolean same_as_before ) {
              
              InetSocketAddress cache_address = discovery.getCacheAddress();

              if ( cache_address == null ){
                adapter.connectionError( new JPCException( "download cache_address == null" ) );
              }
              else{
                if( same_as_before && download_cache.isConnected() ) {  //if "same_as_before" then we just need to revalidate the cache
                  jpc_plugin.log( "Local download cache server [" +cache_address+ "] re-discovered at same address; re-verifying...", JPCPlugin.LOG_DEBUG );
                  adapter.connectSuccess( download_cache, false );
                  return;  
                }
       
                //either new address or no longer connected to old cache
                download_cache = JPCCacheFactory.createDownloader( jpc_plugin, cache_address );

                jpc_plugin.log( "Local download cache server [" +cache_address+ "] connection attempt started.", JPCPlugin.LOG_DEBUG );
                  
                try {
                  download_cache.connect( adapter );
                }
                catch( JPCException e ) {
                  e.printStackTrace();
                  adapter.connectionError( e );
                }   
              }
            }
          });
          
        }
        catch( JPCException e ){	
          jpc_plugin.log( "Failed to connect to local download cache", e, JPCPlugin.LOG_PUBLIC );
        }
      }
    });
  }
  
  
  
  public void registerWithRemoteUploadCache( InetSocketAddress address, Download download, Peer download_peer, int downloader_id ) {
    try{  upload_caches_mon.enter();
      //see if we're already connected to this upload cache
      JPCCacheMultiplexer cache_mux = (JPCCacheMultiplexer)upload_caches.get( address );
    
      if( cache_mux == null ) {
        jpc_plugin.log( "No existing upload cache connection to [" +address+ "], establishing new.", JPCPlugin.LOG_DEBUG );
        cache_mux = new JPCCacheMultiplexer( jpc_plugin, this, JPCCacheFactory.createUploader( jpc_plugin, address ) );
        upload_caches.put( address, cache_mux );
      }
      else {
        jpc_plugin.log( "upload cache connection to [" +address+ "] already established, mux'ing in new session", JPCPlugin.LOG_DEBUG );
      }
      
      cache_mux.registerSession( download, download_peer, downloader_id );
    }
    finally{ upload_caches_mon.exit();  }
  }
  
  
  
  public void cancelRemoteUploadCacheRegistration( int downloader_id ) {
    try{  upload_caches_mon.enter();
      for( Iterator it = upload_caches.values().iterator(); it.hasNext(); ) {
        JPCCacheMultiplexer cache_mux = (JPCCacheMultiplexer)it.next();
        
        if( cache_mux.deregisterSession( downloader_id ) ) {
          break;
        }
      }
    }
    finally{ upload_caches_mon.exit();  }
  }
  
  
  
  protected void removeRemoteUploadCache( InetSocketAddress address ) {
    try{  upload_caches_mon.enter();
    
      upload_caches.remove( address );
    }
    finally{ upload_caches_mon.exit();  }
  }


}
