/*
 * Created on Feb 28, 2005
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

package com.aelitis.azureus.plugins.jpc.cache.impl;

import java.util.Arrays;


/**
 *
 */
public class PieceRequest {
  private final byte[] hash;
  private final int piece;
  private final int offset;
  private final int length;
  
  private final int hashcode;
  
  
  public PieceRequest( byte[] infohash, int piece_number, int piece_offset, int length ) {
    this.hash = infohash;
    this.piece = piece_number;
    this.offset = piece_offset;
    this.length = length;
    
    hashcode = (new String(infohash) + piece_number + piece_offset + length).hashCode();
  }
  
  
  
  public byte[] getHash(){  return hash;  }
  public int getPiece(){  return piece;  }
  public int getOffset(){  return offset;  }
  public int getLength(){  return length;  }
  
  
  
  public boolean equals( Object o ) {
    if( !(o instanceof PieceRequest) )  return false;
    
    PieceRequest other = (PieceRequest)o;
    
    if( !Arrays.equals( this.hash, other.hash ) )  return false;
    if( this.piece != other.piece )  return false;
    if( this.offset != other.offset )  return false;
    if( this.length != other.length )  return false;

    return true;
  }
  

  public int hashCode() {  return hashcode;  }
  


}
