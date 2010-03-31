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

package com.aelitis.azureus.plugins.jpc.cache;

import java.net.InetSocketAddress;

import com.aelitis.azureus.plugins.jpc.JPCException;

/**
 * @author parg
 *
 */

public interface 
JPCCache 
{
  
  /**
   * Attempt to establish the cache network connection synchronously.
   * @param adapter for cache handling
   * @throws JPCException on error
   */
  public void connect( JPCCacheAdapter adapter ) throws JPCException;

  
	public InetSocketAddress getAddress();
  
  
  /**
   * Get the session (peer) id given to us by the cache server.
   * @return id
   */
  public int getSessionID();
  
		
  public void sendHello( String	client_and_version );
  
  
  public void sendActive();
  
  
  public void closeConnection();
  
  public boolean isConnected();
}
