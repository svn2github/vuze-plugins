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

package com.aelitis.azureus.plugins.jpc.peer.impl.messaging;

import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.messaging.*;



/**
 *
 */
public class JPCCacheHello implements JPCPeerMessage {
  private final String description;
  private final ByteBuffer buffer;
  
  private final String address;
  private final int port;
  private final int session_id;

  
  
  
  public JPCCacheHello( String address, int port, int session_id ) {
    this.address = address;
    this.port = port;
    this.session_id = session_id;

    description = getID()+ " for server at " +address+ ":" +port+ " using session id# " +session_id;

    byte[] raw_address = address.getBytes();
    
    buffer = ByteBuffer.allocate( 12 + raw_address.length );
    buffer.putInt( raw_address.length );
    buffer.put( raw_address );
    buffer.putInt( port );
    buffer.putInt( session_id );
    buffer.flip();
  }
  
  
  public String getAddress() {  return address;  }
  
  public int getPort() {  return port;  }
  
  public int getSessionID() {  return session_id;  }

  
  
  
  public String getID() {  return JPCPeerMessage.ID_JPC_CACHE_HELLO;  }

  public byte getVersion() {  return JPCPeerMessage.JPC_DEFAULT_VERSION;  }

  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public ByteBuffer[] getPayload() {  return new ByteBuffer[] { buffer };  }
    
  public void destroy() { /*nothing*/ }
    
  
  

  public Message create( ByteBuffer data ) throws MessageException {
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining() < 13 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining()+ "] < 13" );
    }
    
    int size = data.getInt();

    byte[] raw_address = new byte[ size ];
    data.get( raw_address );
    
    int prt = data.getInt();
    
    int id = data.getInt();
    
    return new JPCCacheHello( new String( raw_address ), prt, id );
  }
    
  
  
}
