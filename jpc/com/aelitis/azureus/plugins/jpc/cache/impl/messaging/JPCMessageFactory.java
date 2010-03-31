/*
 * Created on Feb 16, 2005
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

package com.aelitis.azureus.plugins.jpc.cache.impl.messaging;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.gudy.azureus2.plugins.messaging.*;
import org.gudy.azureus2.plugins.network.RawMessage;

/**
 *
 */
public class JPCMessageFactory {
  private static final byte ID_REQUEST_DOWN     = 30;
  private static final byte ID_CANCEL           = 31;
  private static final byte ID_HELLO_DOWN       = 32;
  private static final byte ID_INVALIDATE       = 33;
  private static final byte ID_PIECE_DOWN       = 34;
  private static final byte ID_LICENSE_REQUEST  = 30;  //a license request is simply a normal piece request id with no payload
  private static final byte ID_LICENSE_REPLY    = 35;
  private static final byte ID_REPLY_DOWN       = 36;
  private static final byte ID_BYE_DOWN         = 37;
  
  private static final byte ID_REQUEST_UP       = 40;
  private static final byte ID_DOWNLOADED       = 41;
  private static final byte ID_REPLY_UP         = 42;
  private static final byte ID_BYE_UP           = 43;
  private static final byte ID_PIECE_UP         = 44;
  private static final byte ID_HELLO_UP         = 45;
  private static final byte ID_ACTIVE           = 46;
  private static final byte ID_ACTIVE_ACK       = 47;
  
  private static final byte ID_ERROR            = 50;
  

  
  private static final HashMap message_map = new HashMap();
  static {
    //these are the only messages we receive in
    message_map.put( new Byte( ID_PIECE_DOWN ),    new JPCPiece( -1, new byte[20], -1, -1, ByteBuffer.allocate(0) ) );
    message_map.put( new Byte( ID_LICENSE_REPLY ), new JPCLicenseReply( new byte[20] ) );
    message_map.put( new Byte( ID_REPLY_DOWN ),    new JPCReply( -1, -1, -1 ) );
    message_map.put( new Byte( ID_BYE_DOWN ),      new JPCBye( "" ) );
    message_map.put( new Byte( ID_REQUEST_UP ),    new JPCRequest( -1, new byte[20], -1, -1, -1 ) );
    message_map.put( new Byte( ID_DOWNLOADED ),    new JPCDownloaded( -1, new byte[20], -1, -1, -1 ) );
    message_map.put( new Byte( ID_REPLY_UP ),      new JPCReply( -1, -1, -1 ) );
    message_map.put( new Byte( ID_BYE_UP ),        new JPCBye( "" ) );
    message_map.put( new Byte( ID_ERROR ),         new JPCError( -1, (short)-1 ) );
    message_map.put( new Byte( ID_ACTIVE_ACK ),    new JPCActiveAck( -1, -1 ) );
  }  
  
  
  private static final HashMap id_map = new HashMap();
  static {
    //these are the only messages we send out
    id_map.put( JPCCacheMessage.ID_JPC_LICENSE_REQUEST,  new Byte( ID_LICENSE_REQUEST ) );
    id_map.put( JPCCacheMessage.ID_JPC_REQUEST,          new Byte( ID_REQUEST_DOWN ) );
    id_map.put( JPCCacheMessage.ID_JPC_CANCEL,           new Byte( ID_CANCEL ) );
    id_map.put( JPCCacheMessage.ID_JPC_HELLO_DOWN,       new Byte( ID_HELLO_DOWN ) );
    id_map.put( JPCCacheMessage.ID_JPC_INVALIDATE,       new Byte( ID_INVALIDATE ) );
    id_map.put( JPCCacheMessage.ID_JPC_PIECE,            new Byte( ID_PIECE_UP ) );
    id_map.put( JPCCacheMessage.ID_JPC_HELLO_UP,         new Byte( ID_HELLO_UP ) );
    id_map.put( JPCCacheMessage.ID_JPC_ACTIVE,           new Byte( ID_ACTIVE ) );
  }
  
  
  
  
  
  
  

  /**
   * Create the proper JPC-cache message from the given parsed raw stream payload.
   * @param stream_payload raw message
   * @return JPC cache message
   * @throws MessageException on creation error
   */
  public static Message createJPCMessage( ByteBuffer stream_payload ) throws MessageException {
    byte id = stream_payload.get();
    
    Message msg = (Message)message_map.get( new Byte( id ) );
    
    if( msg == null ) {
      throw new MessageException( "unknown JPC message id [" +id+ "]" );
    }
    
    return msg.create( stream_payload );
  }
  
  
  
  /**
   * Create the proper JPC-cache raw message from the given base message.
   * @param base_message to create from
   * @return JPC raw message
   */
  public static RawMessage createJPCRawMessage( Message base_message ) {
    Byte id = (Byte)id_map.get( base_message.getID() );
    
    if( id == null ) {
      System.out.println( "ERROR: could not find id for base message [" +base_message.getID()+ "]" );
      return null;
    }
    
    return new JPCRawMessageImpl( base_message, id.byteValue() );
    
  }
  
}
