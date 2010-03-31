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

/**
 *
 */
public class JPCReply implements JPCCacheMessage {
  private final String description;
  private final ByteBuffer buffer;
  private final int session_id;
  private final int idle_time;
  private final int drop_time;
  
  public JPCReply( int session_id, int session_idle_time, int session_drop_time ) {    
    this.session_id = session_id;
    this.idle_time = session_idle_time;
    this.drop_time = session_drop_time;
    description = getID()+ " for session id# " +session_id+ " with idle time=" +idle_time+ ", drop time=" +drop_time;
    buffer = ByteBuffer.allocate( 12 );
    buffer.putInt( session_id );
    buffer.putInt( idle_time );
    buffer.putInt( drop_time );
    buffer.flip();
  }
  
  
  public int getSessionID() {  return session_id;  }
  //public int getSessionIdleTime() {  return 60;  }
  //public int getSessionDropTime() {  return 120;  }
  public int getSessionIdleTime() {  return idle_time;  }
  public int getSessionDropTime() {  return drop_time;  }
  
  public String getID() {  return JPCCacheMessage.ID_JPC_REPLY;  }

  public byte getVersion() {  return JPCCacheMessage.JPC_DEFAULT_VERSION;  }

  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public ByteBuffer[] getPayload() {  return new ByteBuffer[] { buffer };  }
    
  public void destroy() { /*nothing*/ }
    
  
  
  public Message create( ByteBuffer data ) throws MessageException {    
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining() < 4 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining()+ "] < 4" );
    }
    

    int id = data.getInt();
    
    int idle = -1;
    int drop = -1;
    
    if( data.hasRemaining() ) {  //upload session
      idle = data.getInt();
      drop = data.getInt();
    }
    
    return new JPCReply( id, idle, drop );
  }
    
  
  
}
