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

import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.bittorrent.BTMessageManager;
import org.gudy.azureus2.plugins.messaging.bittorrent.BTMessagePiece;
import org.gudy.azureus2.plugins.network.OutgoingMessageQueueListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.utils.StaticUtilities;

import com.aelitis.azureus.plugins.jpc.JPCPlugin;
import com.aelitis.azureus.plugins.jpc.cache.JPCCacheUploader;
import com.aelitis.azureus.plugins.jpc.peer.impl.messaging.JPCCacheReply;



public class JPCCacheMultiplexerHelper {
  private final int MAX_REQUEST_HISTORY = 150;  
  
  private final JPCPlugin jpc_plugin;
  private final Peer peer;
  private final Download download;
  private final int downloader_id;
  private final Listener listener;
  
  private JPCCacheUploader cache = null;
  private final LinkedList requests = new LinkedList();

  
  private final OutgoingMessageQueueListener queue_listener = new OutgoingMessageQueueListener() {
    public boolean messageAdded( Message mesg ) {
      if( cache == null )  return true;
      
      if( mesg.getID().equals( BTMessageManager.ID_BTMESSAGE_UNCHOKE ) ) {
        listener.establishmentNeeded();
        return true;
      }
      
      if( mesg.getID().equals( BTMessageManager.ID_BTMESSAGE_PIECE ) ) {
        BTMessagePiece piece = BTMessageManager.createCoreBTPieceAdaptation( mesg );
        
        byte[] hash = download.getTorrent().getHash();
        int number = piece.getPieceNumber();
        int offset = piece.getPieceOffset();
        ByteBuffer data = piece.getPieceData();
        
        
        PieceRequest request = new PieceRequest( hash, number, offset, data.remaining() );
        
        boolean found = requests.remove( request );
        
        if( !found ) {  //this piece data was not requested via the cache server
          /*
          System.out.println( "Piece details: " +StaticUtilities.getFormatters().formatByteArray( request.getHash(), true )+ ", " +request.getPiece()+ ", " +request.getOffset()+ ", " +request.getLength() );
          System.out.println( "Registered Requests:" );
          for( int i=0; i < requests.size(); i++ ) {
            PieceRequest req = (PieceRequest)requests.get( i );
            System.out.println( StaticUtilities.getFormatters().formatByteArray( req.getHash(), true )+ ", " +req.getPiece()+ ", " +req.getOffset()+ ", " +req.getLength() );
          }
          System.out.println();
          */
          jpc_plugin.log( "Piece data [" +number+":"+offset+"+"+data.remaining()+ "] not requested via cache, sending direct as normal", JPCPlugin.LOG_DEBUG );
          return true;
        }

        cache.sendBlock( downloader_id, hash, number, offset, data );
        listener.messageSent();
        return false; 
      }
      
      return true;
    }

    public void messageSent( Message message ) {  /*nothing*/  }
    public void bytesSent( int byte_count ) {  /*nothing*/  }
  };
  
  
  
  
  
  public JPCCacheMultiplexerHelper( JPCPlugin plugin, Download download, Peer download_peer, int downloader_id, Listener listener ) {
    this.jpc_plugin = plugin;
    this.peer = download_peer;
    this.download = download;
    this.downloader_id = downloader_id;
    this.listener = listener;
  }
  

  public void sessionEstablished( JPCCacheUploader up_cache, int upload_id ) {
    this.cache = up_cache;
    peer.getConnection().getOutgoingMessageQueue().sendMessage( new JPCCacheReply( upload_id ) );  //send cache acceptance reply back to peer
    peer.getConnection().getOutgoingMessageQueue().registerListener( queue_listener );  //start core message re-routing
  }  
  
  
  public void receivedRequest( byte[] hash, int piece_index, int start_offset, int length ) {
    requests.add( new PieceRequest( hash, piece_index, start_offset, length ) );
    Message request = BTMessageManager.createCoreBTRequest( piece_index, start_offset, length );
    peer.getConnection().getIncomingMessageQueue().notifyOfExternalReceive( request );
  }
  
  
  public void receivedDownloaded( byte[] hash, int piece_index, int start_offset, int length ) {
    //pretend the piece was actually sent directly
    Message fake = BTMessageManager.createCoreBTPiece( piece_index, start_offset, ByteBuffer.allocate( length ) );
    peer.getConnection().getOutgoingMessageQueue().notifyOfExternalSend( fake );
  }
  

  public void destroy() {
    peer.getConnection().getOutgoingMessageQueue().deregisterListener( queue_listener );  //cancel core message re-routing
  }

  
  
  
  public interface Listener {
    public void messageSent();
    public void establishmentNeeded();
  }
    
}
