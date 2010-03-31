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

package com.aelitis.azureus.plugins.jpc.cache.impl;

import java.net.*;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.utils.Semaphore;

import org.gudy.azureus2.plugins.messaging.*;
import org.gudy.azureus2.plugins.network.*;

import com.aelitis.azureus.plugins.jpc.*;
import com.aelitis.azureus.plugins.jpc.cache.*;
import com.aelitis.azureus.plugins.jpc.cache.impl.messaging.*;


//TODO better download/upload speed reporting


/**
 *
 *
 */
public class JPCCacheImpl implements JPCCacheDownloader, JPCCacheUploader {
  
  private final JPCPlugin jpc_plugin;
  private final InetSocketAddress address;
  private final boolean uploader;
  private Connection connection = null;
 
  private int session_id;
  

  private int bytes_sent = 1;
  private long last_time = 0;
  
  private boolean is_connected = false;
  
  
  
  public JPCCacheImpl( JPCPlugin plugin, InetSocketAddress address, boolean uploader ) {
    this.jpc_plugin = plugin;
    this.address = address;
    this.uploader = uploader;
  }
  

  public void connect( final JPCCacheAdapter adapter ) throws JPCException {    
    final Semaphore connect_sem = jpc_plugin.getPluginInterface().getUtilities().getSemaphore();
    
    final Throwable[] connect_error = new Throwable[ 1 ];    

    if( connection != null ) {
      throw new JPCException( "connection != null :: connect called on already-connected connection", connect_error[0] );
    }
    
    connection = jpc_plugin.getPluginInterface().getConnectionManager().createConnection( address, new JPCMessageEncoder(), new JPCMessageDecoder() );
    
    //connect to the cache
    connection.connect( new ConnectionListener() {
      public void connectStarted() { /*nothing*/ }

      public void connectSuccess() {
        connect_sem.release();
      }

      public void connectFailure( Throwable failure_msg ) {
        jpc_plugin.log( "connect failure" +failure_msg.getMessage(), JPCPlugin.LOG_DEBUG );
        connect_error[0] = failure_msg;
        connect_sem.release();
      }
      

      public void exceptionThrown( Throwable error ) {
        jpc_plugin.log( "cache connection error: " +error.getMessage(), JPCPlugin.LOG_DEBUG );
        adapter.connectionError( new JPCException( "cache connection exception: " +error.getMessage(), error ) );
        connect_sem.release();
      }
      
    });
    
    connect_sem.reserve();  //block until connected
    
    if( connect_error[0] != null ) {
      throw new JPCException( "cache connection connect failure", connect_error[0] );
    }
    
    if( connection == null )  return;  //the connection was closed during connect operation
    
    is_connected = true;
    
    connection.getOutgoingMessageQueue().registerListener( new OutgoingMessageQueueListener() {
      public boolean messageAdded( Message message ) {  return true;  }
      
      public void messageSent( Message message ){
        jpc_plugin.log( "Sent [" +message.getDescription()+ "] message to " +(uploader?"upload":"download")+ " cache server [" +address+ "]", JPCPlugin.LOG_DEBUG );
        
        if( message.getID().equals( JPCCacheMessage.ID_JPC_PIECE ) ) {
          //the cache server only sends us a DOWNLOADED message when a piece has been downloaded directly from the cache,
          //so we still need to notify ourselves of piece data we've actually uploaded ourselves
          JPCPiece piece = (JPCPiece)message;
          JPCCacheUploaderAdapter up_adapt = (JPCCacheUploaderAdapter)adapter;
          up_adapt.receivedDownloaded( piece.getSessionID(), piece.getInfohash(), piece.getPieceNumber(), piece.getPieceOffset(), piece.getPieceData().limit() );  //fake it
        }
      }
      
      public void bytesSent( int byte_count ) {
        bytes_sent += byte_count;
        
        long diff = SystemTime.getCurrentTime() - last_time;
        
        long rate = (bytes_sent*1000) / (diff+1);
        
        if( diff > 2000 ) {
          if( rate > 0 )  jpc_plugin.log( (uploader?"upload":"download")+ " cache server send-to rate: " +rate+ " bps", JPCPlugin.LOG_DEBUG );
          bytes_sent = 1;
          last_time = SystemTime.getCurrentTime();
        }
      }
    });
    
    
    connection.getIncomingMessageQueue().registerListener( new IncomingMessageQueueListener() {
      public boolean messageReceived( Message message ) {
        jpc_plugin.log( "Received [" +message.getDescription()+ "] message from " +(uploader?"upload":"download")+ " cache server [" +address+ "]", JPCPlugin.LOG_DEBUG );

        String id = message.getID();
        
        if( id.equals( JPCCacheMessage.ID_JPC_PIECE ) ) {
          JPCPiece piece = (JPCPiece)message;
          JPCCacheDownloaderAdapter down_adapt = (JPCCacheDownloaderAdapter)adapter;
          down_adapt.receivedBlock( piece.getSessionID(), piece.getInfohash(), piece.getPieceNumber(), piece.getPieceOffset(), piece.getPieceData() );
          return true;
        }
        
        if( id.equals( JPCCacheMessage.ID_JPC_LICENSE_REPLY ) ) {
          JPCLicenseReply reply = (JPCLicenseReply)message;
          JPCCacheDownloaderAdapter down_adapt = (JPCCacheDownloaderAdapter)adapter;
          down_adapt.receivedLicense( reply.getLicense() );
          return true;
        }
        
        if( id.equals( JPCCacheMessage.ID_JPC_REPLY ) ) {    
          JPCReply reply = (JPCReply)message;
          
          session_id = reply.getSessionID();  //record our id
          
          adapter.receivedReply( session_id, reply.getSessionIdleTime(), reply.getSessionDropTime() );
          return true;
        }
        
        if( id.equals( JPCCacheMessage.ID_JPC_ACTIVE_ACK ) ) {    
          JPCActiveAck ack = (JPCActiveAck)message;
          adapter.receivedActiveAck( ack.getSessionIdleTime(), ack.getSessionDropTime() );
          return true;
        }
        
        
        if( id.equals( JPCCacheMessage.ID_JPC_BYE ) ) {
          JPCBye bye = (JPCBye)message;
          adapter.receivedBye( bye.getReason() );
          return true;
        }
        
        if( id.equals( JPCCacheMessage.ID_JPC_ERROR ) ) {
          JPCError error = (JPCError)message;
          
          String reason;
          
          if( error.getErrorCode() == 0 )  reason = "connection dropped";
          else if( error.getErrorCode() == 1 )  reason = "invalid response";
          else if( error.getErrorCode() == 2 )  reason = "unknown peer id";
          else reason = "unknown error code: " +error.getErrorCode();
          
          adapter.receivedError( error.getSessionID(), reason );
          return true;
        }
        
        if( id.equals( JPCCacheMessage.ID_JPC_REQUEST ) ) {
          JPCRequest req = (JPCRequest)message;
          JPCCacheUploaderAdapter up_adapt = (JPCCacheUploaderAdapter)adapter;
          up_adapt.receivedRequest( req.getSessionID(), req.getInfohash(), req.getPieceNumber(), req.getPieceOffset(), req.getPieceLength() );
          return true;
        }
        
        if( id.equals( JPCCacheMessage.ID_JPC_DOWNLOADED ) ) {
          JPCDownloaded down = (JPCDownloaded)message;
          JPCCacheUploaderAdapter up_adapt = (JPCCacheUploaderAdapter)adapter;
          up_adapt.receivedDownloaded( down.getSessionID(), down.getInfohash(), down.getPieceNumber(), down.getPieceOffset(), down.getPieceLength() );
          return true;
        }

        jpc_plugin.log( "ERROR: received unknown message id from cache server: " +id, JPCPlugin.LOG_DEBUG );
        return false;
      }

      public void bytesReceived( int byte_count ) {
        //TODO cache stats?
      }
    });
    
    
    connection.startMessageProcessing();
    adapter.connectSuccess( this, true );
  }
  
  
  
  
  
  
  
	public InetSocketAddress getAddress()	{  return address; 	}
	
  public int getSessionID() {  return session_id;  }
  
  
  
	public void
	sendHello(
		String	client_and_version )
	{
	  Message hello;
	  if( uploader ) {
	    hello = new JPCHelloUp( client_and_version );
	  }
	  else {
	    hello = new JPCHelloDown( client_and_version );
	  }
    
      if( !is_connected )  jpc_plugin.log( "ERROR: cache connection not connected!", JPCPlugin.LOG_DEBUG );
	  connection.getOutgoingMessageQueue().sendMessage( hello );
	}
  
  
  public void sendActive() {
    if( !is_connected )  jpc_plugin.log( "ERROR: cache connection not connected!", JPCPlugin.LOG_DEBUG );
    connection.getOutgoingMessageQueue().sendMessage( new JPCActive( getSessionID() ) );
  }
	
  
	public void
	requestLicense()
	{
	  if( !is_connected )  jpc_plugin.log( "ERROR: cache connection not connected!", JPCPlugin.LOG_DEBUG );
      connection.getOutgoingMessageQueue().sendMessage( new JPCLicenseRequest() );
	}
  
	
	public void
	requestBlock(
		int		peer_id,
		byte[]	hash,
		int		piece_index,
		int		start_offset,
		int		length )
	{
	  if( !is_connected )  jpc_plugin.log( "ERROR: cache connection not connected!", JPCPlugin.LOG_DEBUG );
	  connection.getOutgoingMessageQueue().sendMessage( new JPCRequest( peer_id, hash, piece_index, start_offset, length ) );
	}
	
  
	public void
	sendBlock(
		int		peer_id,
		byte[]	hash,
		int		piece_index,
		int		start_offset,
		ByteBuffer	data )
	{
	  if( !is_connected )  jpc_plugin.log( "ERROR: cache connection not connected!", JPCPlugin.LOG_DEBUG );
	  connection.getOutgoingMessageQueue().sendMessage( new JPCPiece( peer_id, hash, piece_index, start_offset, data ) );
	}
	
	public void
	sendCancel(
		int		peer_id,
		byte[]	hash,
		int		piece_index,
		int		start_offset,
		int		length )
	{
	  if( !is_connected )  jpc_plugin.log( "ERROR: cache connection not connected!", JPCPlugin.LOG_DEBUG );
	  connection.getOutgoingMessageQueue().sendMessage( new JPCCancel( peer_id, hash, piece_index, start_offset, length ) );
	}
	
  
	public void
	sendInvalidate(
		int		peer_id,
		byte[]	hash,
		int		piece_index,
		int		start_offset,
		int		length )
	{
	  if( !is_connected )  jpc_plugin.log( "ERROR: cache connection not connected!", JPCPlugin.LOG_DEBUG );
	  connection.getOutgoingMessageQueue().sendMessage( new JPCInvalidate( peer_id, hash, piece_index, start_offset, length ) );
	}
	
  
	public void
	closeConnection()
	{
	  is_connected = false;
      if( connection != null ) {
        connection.close();
        connection = null;
      }
	}
  
  
  public boolean isConnected() {  return is_connected;  }
}
