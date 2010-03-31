/*
 * Created on May 18, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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


import java.util.*;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.utils.Monitor;

import com.aelitis.azureus.plugins.jpc.*;
import com.aelitis.azureus.plugins.jpc.cache.*;




public class JPCCacheMultiplexer {
  
  private final JPCPlugin jpc_plugin;
  private final JPCCacheManagerImpl manager;
  private final JPCCacheUploader cache;
  
  private final HashMap sessions = new HashMap();
  private final Monitor sessions_mon;
  
  private boolean is_established = false;
  private boolean is_establishing = false;
  private int uploader_id;
  private int session_idle_time;
  private int session_drop_time;  //for keep-alive
  private long last_message_transfered_time;
  
  private boolean is_authenticated = false;
  
  private boolean keep_alive_connect = false;

  private TimerTask idle_timer_task = new TimerTask() {
    public void run(){
      if( !is_established )  return;  //not connected, so no need to check for drop      
      if( session_idle_time < 1 )  return;   //0 means we can idle forever, no need to drop
      if( System.currentTimeMillis() - last_message_transfered_time > (session_idle_time*1000) ) {  //we've idled too long, so drop connection
        jpc_plugin.log( "Upload cache connection idle expired after ["+session_idle_time+"sec], temporarily closing connection.  Keep-alive scheduled in ["+session_drop_time+"sec]", JPCPlugin.LOG_DEBUG );
        closeCacheConnection();
      }
    }
  };
  
  
  private final JPCCacheMultiplexerHelper.Listener helper_listener = new JPCCacheMultiplexerHelper.Listener() {
    public void messageSent(){
      last_message_transfered_time = System.currentTimeMillis();
    }
    
    public void establishmentNeeded() {
      establishCacheConnection();
    }
  };
  
  
  
  
  private final JPCCacheUploaderAdapter upload_adapter = new JPCCacheUploaderAdapter() {
    
    public void connectSuccess( JPCCache up_cache, boolean is_new_discovery ) { //upload cache connection connected
      is_established = true;
      is_establishing = false;
      
      if( is_authenticated ) {
        cache.sendActive();
        last_message_transfered_time = System.currentTimeMillis();
      }
      else {      
        cache.sendHello( jpc_plugin.getPluginInterface().getAzureusName() + "/" + jpc_plugin.getPluginInterface().getAzureusVersion() );
        last_message_transfered_time = System.currentTimeMillis();
      }
    }
    
    
    public void receivedReply( int peer_id, int idle_time, int drop_timeout ) {  //upload cache connection accepted
      jpc_plugin.log( "Upload cache connection succeeded, using cache.", JPCPlugin.LOG_PUBLIC );
      last_message_transfered_time = System.currentTimeMillis();
      
      //notify existing peers and hook into core message re-routing
      try{  sessions_mon.enter();
        is_authenticated = true;
        
        uploader_id = peer_id;
        session_idle_time = idle_time;
        session_drop_time = drop_timeout;

        jpc_plugin.getPluginTimer().schedule( idle_timer_task, 10*1000, 10*1000 );
        
        for( Iterator it = sessions.values().iterator(); it.hasNext(); ) {
          JPCCacheMultiplexerHelper helper = (JPCCacheMultiplexerHelper)it.next();
          
          helper.sessionEstablished( cache, uploader_id );
        }
      }
      finally{ sessions_mon.exit();  }
    }

    
    public void receivedActiveAck( int idle_time, int drop_timeout ){
      last_message_transfered_time = System.currentTimeMillis();
      session_idle_time = idle_time;
      session_drop_time = drop_timeout;

      if( keep_alive_connect ) {  //drop connectiin again since this was just a keep-alive
        jpc_plugin.log( "Was keep-alive connect, dropping connection again", JPCPlugin.LOG_DEBUG );
        last_message_transfered_time = 0;
        keep_alive_connect = false;
      }
    }
    
    
    public void receivedRequest( int peer_id, byte[] hash, int piece_index, int start_offset, int length ) {
      last_message_transfered_time = System.currentTimeMillis();
      
      try{  sessions_mon.enter();
        JPCCacheMultiplexerHelper helper = (JPCCacheMultiplexerHelper)sessions.get( new Integer( peer_id ) );
        
        if( helper == null ) {
          jpc_plugin.log( "receivedRequest() helper == null: request received for unknown downloader id", JPCPlugin.LOG_DEBUG );
          return;
        }
        
        helper.receivedRequest( hash, piece_index, start_offset, length );
      }
      finally{ sessions_mon.exit();  }
    }
    
    
    public void receivedDownloaded( int peer_id, byte[] hash, int piece_index, int start_offset, int length ) {
      last_message_transfered_time = System.currentTimeMillis();
      
      try{  sessions_mon.enter();
        JPCCacheMultiplexerHelper helper = (JPCCacheMultiplexerHelper)sessions.get( new Integer( peer_id ) );
      
        if( helper == null ) {
          jpc_plugin.log( "receivedDownloaded() helper == null: downloaded received for unknown downloader id", JPCPlugin.LOG_DEBUG );
          return;
        }
      
        helper.receivedDownloaded( hash, piece_index, start_offset, length );
      }
      finally{ sessions_mon.exit();  }
    }
    
    
    public void receivedError( int peer_id, String reason ) {
      last_message_transfered_time = System.currentTimeMillis();
      
      if( peer_id == uploader_id ) {
        jpc_plugin.log( "RECEIVED ERROR TYPE 2 ON ACTIVE MESSAGE REPLY, CACHE CONNECTION TIMED OUT", JPCPlugin.LOG_PUBLIC );
        destroy();  //try to reestablish?
      }
      
      jpc_plugin.log( "Upload cache server reported error on download side for [#" +peer_id+ "]: " + reason, JPCPlugin.LOG_DEBUG );
      deregisterSession( peer_id );
    } 
    
    
    public void receivedBye( String reason ) {
      connectionError( new JPCException( "upload cache server said goodbye: " + reason ) );
    }

    
    public void connectionError( JPCException error ) {
      jpc_plugin.log( "Upload cache connection error: " +error.getMessage(), JPCPlugin.LOG_DEBUG );
      destroy();
    }
  };
  
  
  
  

  protected JPCCacheMultiplexer( JPCPlugin plugin, JPCCacheManagerImpl cache_manager, JPCCacheUploader up_cache ) {
    this.jpc_plugin = plugin;
    this.manager = cache_manager;
    this.cache = up_cache;
    this.sessions_mon = jpc_plugin.getPluginInterface().getUtilities().getMonitor();
    
    establishCacheConnection();
  }
  
  
  
  private void establishCacheConnection() {
    if( is_established || is_establishing )  return;

    is_establishing = true;
    
    jpc_plugin.getPluginInterface().getUtilities().createThread( "JPCRemoteUploadCacheConnect:" +cache.getAddress(), new Runnable() {
      public void run() {  //make cache.connect() non-blocking?
        jpc_plugin.log( "Remote upload cache server [" +cache.getAddress()+ "] connection attempt started.", JPCPlugin.LOG_DEBUG );
        
        try {
          cache.connect( upload_adapter );
        }
        catch( JPCException e ) {
          e.printStackTrace();
          upload_adapter.connectionError( e );
        }
      }
    });
  }
  
  
  private void closeCacheConnection() {
    is_established = false;
    is_establishing = false;
    cache.closeConnection();
    
    if( session_drop_time < 1 )  return;  //no need to send keep-alives

    jpc_plugin.getPluginTimer().schedule( new TimerTask() {  //set keep-alive timer
      public void run() {
        if( !is_authenticated )  return;  //already destroyed
        keep_alive_connect = true;
        jpc_plugin.log( "Session keep-alive started", JPCPlugin.LOG_DEBUG );
        establishCacheConnection();
      }},
      session_drop_time * 900 );  //90%
  }
  

  
  protected void registerSession( Download download, Peer download_peer, int downloader_id ) {
    try{  sessions_mon.enter();
      JPCCacheMultiplexerHelper helper = new JPCCacheMultiplexerHelper( jpc_plugin, download, download_peer, downloader_id, helper_listener );
      Integer id = new Integer( downloader_id );
      
      if( !sessions.containsKey( id ) ) {
        sessions.put( id, helper );
      }
      else {
        jpc_plugin.log( "REGISTER ERROR: session already registered: " +downloader_id, JPCPlugin.LOG_DEBUG );
        return;
      }
      
      if( is_authenticated ) {  //the cache connection is already established, so establish peer session immediately
        helper.sessionEstablished( cache, uploader_id );
      }
    }
    finally{  sessions_mon.exit();  }
  }
  
  
  
  protected boolean deregisterSession( int downloader_id ) {
    try{  sessions_mon.enter();
      JPCCacheMultiplexerHelper helper = (JPCCacheMultiplexerHelper)sessions.remove( new Integer( downloader_id ) );

      if( helper == null ) {
        return false;
      }
      
      helper.destroy();
      
      if( sessions.isEmpty() ) {  //no more sessions for this cache
        jpc_plugin.log( "No more sessions running for upload cache server [" +cache.getAddress()+ "], terminating upload cache session", JPCPlugin.LOG_DEBUG );
        destroy();
      }
      
      return true;
    }
    finally{  sessions_mon.exit();  }
  }
  
  
  
  private void destroy() {
    is_authenticated = false;
    is_established = false;
    is_establishing = false;
    
    try{  sessions_mon.enter();
      idle_timer_task.cancel();
      
      for( Iterator it = sessions.values().iterator(); it.hasNext(); ) {
        JPCCacheMultiplexerHelper helper = (JPCCacheMultiplexerHelper)it.next();
      
        helper.destroy();
      }
      
      sessions.clear();
    }
    finally{ sessions_mon.exit();  }
    
    if( cache != null ) {
      cache.closeConnection();
      manager.removeRemoteUploadCache( cache.getAddress() );  //deregister from main cache manager
    }
  }
  
}
