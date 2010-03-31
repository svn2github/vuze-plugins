/*
 * Created on Feb 24, 2005
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

package com.aelitis.azureus.plugins.jpc.peer.impl;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.messaging.*;
import org.gudy.azureus2.plugins.messaging.bittorrent.*;
import org.gudy.azureus2.plugins.network.*;
import org.gudy.azureus2.plugins.peers.*;


import com.aelitis.azureus.plugins.jpc.*;
import com.aelitis.azureus.plugins.jpc.cache.*;
import com.aelitis.azureus.plugins.jpc.license.*;
import com.aelitis.azureus.plugins.jpc.peer.PeerController;
import com.aelitis.azureus.plugins.jpc.peer.impl.messaging.*;
import com.aelitis.azureus.plugins.jpc.validation.*;


/**
 *
 */
public class PeerControllerImpl implements PeerController {
  private final JPCPlugin jpc_plugin;
  private final JPCCacheManager cache_manager;
  private final HashMap peers_uploading = new HashMap();
  private final HashMap peers_downloading = new HashMap();
  
  private JPCCacheDownloader download_cache;
  
 
  private final MessageManagerListener message_manager_listener = new MessageManagerListener() {
    
    public void compatiblePeerFound( final Download download, final Peer peer, Message message ) {
      //register for incoming JPC message handling
      peer.getConnection().getIncomingMessageQueue().registerListener( new IncomingMessageQueueListener() {  //TODO remove reg
        
        public boolean messageReceived( Message message ) {
          ///////////////////////////////////////////////////////////////////
          if( message.getID().equals( JPCPeerMessage.ID_JPC_CACHE_HELLO ) ) {
            jpc_plugin.log( "Received [" +message.getDescription()+ "] message from peer [" +peer.getClient()+ " @" +peer.getIp()+ ":" +peer.getPort()+ "]", JPCPlugin.LOG_DEBUG );
            
            if( !jpc_plugin.isCacheUploadEnabled() ) {
              jpc_plugin.log( "Cache upload usage disabled....not responding to downloader hello", JPCPlugin.LOG_DEBUG );
              return true;  //local upload cache server usage disabled in config
            }
            
            final JPCCacheHello hello = (JPCCacheHello)message;

            //initiate upload cache server connection
            int id = hello.getSessionID();
            peers_downloading.put( peer, new Integer( id ) );
            cache_manager.registerWithRemoteUploadCache( new InetSocketAddress( hello.getAddress(), hello.getPort() ), download, peer, id );

            return true;
          }
          
          ///////////////////////////////////////////////////////////////////
          if( message.getID().equals( JPCPeerMessage.ID_JPC_CACHE_REPLY ) ) {
            jpc_plugin.log( "Received [" +message.getDescription()+ "] message from peer [" +peer.getClient()+ " @" +peer.getIp()+ ":" +peer.getPort()+ "]", JPCPlugin.LOG_DEBUG );
            
            final JPCCacheReply reply = (JPCCacheReply)message;
            
            peers_uploading.put( new Integer( reply.getSessionID() ), peer );
            
            //register request+cancel rerouting
            peer.getConnection().getOutgoingMessageQueue().registerListener( new OutgoingMessageQueueListener() {  //TODO: remove reg
              public boolean messageAdded( Message message ) {
                if( download_cache == null )  return true;
                
                if( message.getID().equals( BTMessageManager.ID_BTMESSAGE_REQUEST ) ) {
                  BTMessageRequest req = BTMessageManager.createCoreBTRequestAdaptation( message );
                  
                  int id = reply.getSessionID();
                  byte[] hash = download.getTorrent().getHash();
                  int piece_num = req.getPieceNumber();
                  int offset = req.getPieceOffset();
                  int length = req.getLength();

                  download_cache.requestBlock( id, hash, piece_num, offset, length );
                  
                  //pretend the request was actually sent
                  Message fake = BTMessageManager.createCoreBTRequest( piece_num, offset, length );
                  peer.getConnection().getOutgoingMessageQueue().notifyOfExternalSend( fake );
                  return false; 
                }
                
                if( message.getID().equals( BTMessageManager.ID_BTMESSAGE_CANCEL ) ) {
                  BTMessageCancel can = BTMessageManager.createCoreBTCancelAdaptation( message );
                  
                  int id = reply.getSessionID();
                  byte[] hash = download.getTorrent().getHash();
                  int piece_num = can.getPieceNumber();
                  int offset = can.getPieceOffset();
                  int length = can.getLength();

                  download_cache.sendCancel( id, hash, piece_num, offset, length );
                  
                  //pretend the cancel was actually sent
                  Message fake = BTMessageManager.createCoreBTCancel( piece_num, offset, length );
                  peer.getConnection().getOutgoingMessageQueue().notifyOfExternalSend( fake );
                  return false; 
                }
                
                //TODO invalidate message
                
                return true;
              }

              public void messageSent( Message message ) {  /*nothing*/  }
              public void bytesSent( int byte_count ) {  /*nothing*/  }
            });
            
            return true;
          }                                

          return false;
        }

        public void bytesReceived( int byte_count ) { /*nothing*/ }
      });
      
      
      //send our own JPC hello if we have a local download cache server to use
      //TODO dont send download hello if we're just seeding!
      //TODO drop download caching when seeding
      if( download_cache != null ) {
        JPCCacheHello hello = new JPCCacheHello( download_cache.getAddress().getAddress().getHostAddress(), download_cache.getAddress().getPort(), download_cache.getSessionID() );
        peer.getConnection().getOutgoingMessageQueue().sendMessage( hello );
      }
    }
    
    
    
    
    public void peerRemoved( Download download, Peer peer ) {
      Integer downloader_id = (Integer)peers_downloading.remove( peer );
      if( downloader_id != null ) {
        cache_manager.cancelRemoteUploadCacheRegistration( downloader_id.intValue() );  //peer downloading from us dropped
        return;
      }
      
      for( Iterator i = peers_uploading.values().iterator(); i.hasNext(); ) {  //peer uploading to us dropped
        Peer p = (Peer)i.next();
        if( p == peer ) {
          jpc_plugin.log( "Core reported cache upload peer as removed: " +peer.getIp()+ ":" +peer.getPort()+ ", dropping.", JPCPlugin.LOG_DEBUG );
          i.remove();
          break;
        }
      }

      //TODO drop download cache server connection if no jpc cache-aware peers are avail
    }
  };
  
  
  
 
  public PeerControllerImpl( JPCPlugin plugin, JPCCacheManager manager ) {
    this.jpc_plugin = plugin;
    this.cache_manager = manager;
  }
  


  public void startPeerProcessing() {    
    PluginInterface pi = jpc_plugin.getPluginInterface();
    
    //Register the JPC inter-client extended message types.
    try {
      pi.getMessageManager().registerMessageType( new JPCCacheHello( "", -1, -1 ) );
      pi.getMessageManager().registerMessageType( new JPCCacheReply( -1 ) );
    }
    catch( MessageException e ) {   e.printStackTrace();  }
    
    
    pi.getMessageManager().locateCompatiblePeers( pi, new JPCCacheHello( "", -1, -1 ), message_manager_listener );
  }
  
  
  
  public void stopPeerProcessing() {
    jpc_plugin.getPluginInterface().getMessageManager().deregisterMessageType( new JPCCacheHello( "", -1, -1 ) );
    jpc_plugin.getPluginInterface().getMessageManager().deregisterMessageType( new JPCCacheReply( -1 ) );
    
    jpc_plugin.getPluginInterface().getMessageManager().cancelCompatiblePeersLocation( message_manager_listener );
  }
    
  
  
  public void establishDownloadCacheSession() {
    if( download_cache != null )  return;  //no need to connect again, as we're already established
    
    //try and connect to local download cache server
    final JPCCacheDownloader[] temp = new JPCCacheDownloader[ 1 ];
    
    cache_manager.connectLocalDownloadCache( new JPCCacheDownloaderAdapter() {
      
      public void connectSuccess( JPCCache cache, boolean is_new_discovery ) {
        if( is_new_discovery ) {
          temp[0] = (JPCCacheDownloader)cache;
          temp[0].sendHello( jpc_plugin.getPluginInterface().getAzureusName() +
                             "/" +
                             jpc_plugin.getPluginInterface().getAzureusVersion() +
                             "/" +
                             JPCPlugin.PLUGIN_VERSION );
        }
        else {  //just re-validate
          download_cache.requestLicense();
        }
      }
      
      public void receivedReply( int peer_id, int idle_time, int drop_timeout ) {
        temp[0].requestLicense();
      }
      
      
      public void receivedActiveAck( int idle_time, int drop_timeout ){
        //TODO
      }
      
      
      public void receivedLicense( byte[] content ) {
        JPCLicenseVerifier  verifier = JPCLicenseVerifierFactory.create();
        
        try{
          JPCLicense license = verifier.verify( content );
          JPCValidator  validator = JPCValidatorFactory.create( jpc_plugin);  
          try{
            validator.validate( temp[0].getAddress(), jpc_plugin.getPluginInterface().getUtilities().getPublicAddress(), license );
          }
          catch( JPCException jex ) {
            jpc_plugin.log( "Download cache validation failed: " +jex.getMessage(), JPCPlugin.LOG_PUBLIC );
            
            if( JPCPlugin.USE_TEST_CACHE ) {
              jpc_plugin.log( "....BUT, ignoring cache validation during testing.", JPCPlugin.LOG_PUBLIC );
            }
            else {
              throw jex;
            }
          }

          //dont set real cache until after we've verified the license
          if( download_cache == null )  download_cache = temp[0];
          jpc_plugin.log( "Download cache validation succeeded, using cache.", JPCPlugin.LOG_PUBLIC );          
          
          jpc_plugin.updateVersionCheckString( "$" + download_cache.getAddress().toString() );          
          
        }
        catch( JPCException e ){
          jpc_plugin.log( "Download cache validation failed", e, JPCPlugin.LOG_PUBLIC );

          if ( temp[0] != null ){
            temp[0].closeConnection();     
            temp[0] = null;
          }
          
          dropDownloadCacheSession();
        }
      }

      public void receivedBlock( int peer_id, byte[] hash, int piece_index, int start_offset, ByteBuffer data ) {
        Peer peer = (Peer)peers_uploading.get( new Integer( peer_id ) );
        
        if( peer == null ) {
          jpc_plugin.log( "D: receivedBlock():: peer_id[" +peer_id+ "] not found", JPCPlugin.LOG_DEBUG );
          return;
        }

        Message piece = BTMessageManager.createCoreBTPiece( piece_index, start_offset, data );
        peer.getConnection().getIncomingMessageQueue().notifyOfExternalReceive( piece );
      }

      public void receivedBye( String reason ) {
        connectionError( new JPCException( "download cache server said goodbye: " + reason ) );
      }
      
      public void receivedError( int peer_id, String reason ) {
        Peer peer = (Peer)peers_uploading.remove( new Integer( peer_id ) );
        
        if( peer != null ) {
          jpc_plugin.log( "Download cache server reported [" +reason+ "] error for uploading peer #" +peer_id+ ", dropping " +peer.getIp()+ ":" +peer.getPort(), JPCPlugin.LOG_DEBUG );
        }
      }
      
      public void connectionError( JPCException error ) {
        jpc_plugin.log( "[D] connectionError thrown: " +error.getMessage(), JPCPlugin.LOG_DEBUG );

        if ( temp[0] != null ){
          temp[0].closeConnection();     
          temp[0] = null;
        }
        
        dropDownloadCacheSession();
      }

    });
  }
  
  
  
  public void dropDownloadCacheSession() {
    if ( download_cache != null ){
      download_cache.closeConnection();     
      download_cache = null;
    }
  }
    
}
