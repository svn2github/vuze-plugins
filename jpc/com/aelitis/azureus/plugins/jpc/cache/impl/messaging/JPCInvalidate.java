/*
 * Created on Feb 15, 2005
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

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.utils.StaticUtilities;

/**
 *
 */
public class JPCInvalidate implements JPCCacheMessage {
  private final String description;
  private final ByteBuffer buffer;
  
  
  public JPCInvalidate( int session_id, byte[] infohash, int piece_number, int piece_offset, int length ) {    
    description = getID()+ " session id# " +session_id+ " infohash " + StaticUtilities.getFormatters().formatByteArray( infohash, true )+ " piece #" + piece_number + ": " + piece_offset + "->" + (piece_offset + length -1);

    buffer = ByteBuffer.allocate( 36 );
    
    buffer.putInt( session_id );
    buffer.put( infohash );
    buffer.putInt( piece_number );
    buffer.putInt( piece_offset );
    buffer.putInt( length );
    buffer.flip();
  }
  
  
  public String getID() {  return JPCCacheMessage.ID_JPC_INVALIDATE;  }

  public byte getVersion() {  return JPCCacheMessage.JPC_DEFAULT_VERSION;  }

  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public ByteBuffer[] getPayload() {  return new ByteBuffer[] { buffer };  }
    
  public void destroy() { /*nothing*/ }
    
  
  

  public Message create( ByteBuffer data ) throws MessageException {
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining() != 36 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining()+ "] != 36" );
    }
    
    int session_id = data.getInt();

    byte[] infohash = new byte[ 20 ];
    data.get( infohash );
    
    int piece_number = data.getInt();
    if( piece_number < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: piece_number < 0" );
    }
    
    int piece_offset = data.getInt();
    if( piece_offset < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: piece_offset < 0" );
    }
    
    int length = data.getInt();
    if( length < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: length < 0" );
    }
    
    
    return new JPCInvalidate( session_id, infohash, piece_number, piece_offset, length );
  }
    
  
  
}