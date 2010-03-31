/*
 * Created on Feb 17, 2005
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
import org.gudy.azureus2.plugins.network.RawMessage;

/**
 *
 */
public class JPCRawMessageImpl implements RawMessage {
  private final Message base_message;
  private final ByteBuffer[] raw_payload;
  
  public JPCRawMessageImpl( Message base_msg, byte jpc_id ) {
    this.base_message = base_msg;
    
    ByteBuffer[] base_payload = base_msg.getPayload();

    int base_payload_size = 0;
    for( int i=0; i < base_payload.length; i++ ) {
      base_payload_size += base_payload[i].remaining();
    }
    
    ByteBuffer header = ByteBuffer.allocate( 5 );
    header.putInt( base_payload_size + 1 );
    header.put( jpc_id );
    header.flip();    
    
    raw_payload = new ByteBuffer[ base_payload.length + 1 ];
    
    raw_payload[0] = header;
    
    for( int i=0; i < base_payload.length; i++ ) {
      raw_payload[i+1] = base_payload[ i ];
    }
  }
  

  public String getID(){  return base_message.getID();  }
  public int getType(){  return base_message.getType();  }
  public String getDescription(){  return base_message.getDescription();  }
  public ByteBuffer[] getPayload(){  return base_message.getPayload();  }
  public Message create( ByteBuffer data ) throws MessageException{  return base_message.create( data );  }
  public void destroy(){  base_message.destroy();  }

  
  public ByteBuffer[] getRawPayload() {  return raw_payload;  }
  public Message getOriginalMessage() {  return base_message;  }
}
