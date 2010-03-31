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
public class JPCPiece implements JPCCacheMessage {
  private final String description;
  private final ByteBuffer[] buffer;
  
  private final int session_id;
  private final byte[] infohash;
  private final int piece_number;
  private final int piece_offset;
  
  
  
  public JPCPiece( int session_id, byte[] infohash, int piece_number, int piece_offset, ByteBuffer data ) {
    this.session_id = session_id;
    this.infohash = infohash;
    this.piece_number = piece_number;
    this.piece_offset = piece_offset;
    
    description = getID()+ " for session id# " +session_id+ " infohash " + StaticUtilities.getFormatters().formatByteArray( infohash, true )+ " piece #" + piece_number + ": " + piece_offset + "->" + (piece_offset + data.remaining() -1);

    ByteBuffer header = ByteBuffer.allocate( 32 );
    
    header.putInt( session_id );
    header.put( infohash );
    header.putInt( piece_number );
    header.putInt( piece_offset );
    header.flip();
    
    buffer = new ByteBuffer[] { header, data };
  }
  
  
  
  public int getSessionID() {  return session_id;  }
  
  public byte[] getInfohash() {  return infohash;  }
  
  public int getPieceNumber() {  return piece_number;  }
  
  public int getPieceOffset() {  return piece_offset;  }
  
  public ByteBuffer getPieceData() {  return buffer[1];  }
  
  
  
  
  
  public String getID() {  return JPCCacheMessage.ID_JPC_PIECE;  }

  public byte getVersion() {  return JPCCacheMessage.JPC_DEFAULT_VERSION;  }

  public int getType() {  return Message.TYPE_DATA_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public ByteBuffer[] getPayload() {  return buffer;  }
    
  public void destroy() { /*nothing*/ }
    
  
  

  public Message create( ByteBuffer data ) throws MessageException {
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining() < 32 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining()+ "] < 32" );
    }
    
    int id = data.getInt();

    byte[] hash = new byte[ 20 ];
    data.get( hash );
    
    int number = data.getInt();
    if( number < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: number < 0" );
    }
    
    int offset = data.getInt();
    if( offset < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: offset < 0" );
    }

    
    return new JPCPiece( id, hash, number, offset, data );
  }
    
  
  
}
