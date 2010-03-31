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

import org.gudy.azureus2.plugins.messaging.Message;

/**
 *
 */
public interface JPCCacheMessage extends Message {
  public static final String ID_JPC_REQUEST         = "JPC_REQUEST";
  public static final String ID_JPC_CANCEL          = "JPC_CANCEL";
  public static final String ID_JPC_INVALIDATE      = "JPC_INVALIDATE";
  public static final String ID_JPC_DOWNLOADED      = "JPC_DOWNLOADED";
  public static final String ID_JPC_HELLO_DOWN      = "JPC_HELLO_DOWN";
  public static final String ID_JPC_HELLO_UP        = "JPC_HELLO_UP";
  public static final String ID_JPC_BYE             = "JPC_BYE";
  public static final String ID_JPC_REPLY           = "JPC_REPLY";
  public static final String ID_JPC_LICENSE_REQUEST = "JPC_LICENSE_REQUEST";
  public static final String ID_JPC_LICENSE_REPLY   = "JPC_LICENSE_REPLY";
  public static final String ID_JPC_ERROR           = "JPC_ERROR";
  public static final String ID_JPC_PIECE           = "JPC_PIECE";
  public static final String ID_JPC_ACTIVE          = "JPC_ACTIVE";
  public static final String ID_JPC_ACTIVE_ACK      = "JPC_ACTIVE_ACK";
  
  public static final byte JPC_DEFAULT_VERSION = (byte)1;
  
}
