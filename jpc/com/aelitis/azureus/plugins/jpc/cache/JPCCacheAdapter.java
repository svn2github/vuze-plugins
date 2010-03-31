/*
 * Created on 09-Feb-2005
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

import com.aelitis.azureus.plugins.jpc.JPCException;

/**
 * @author parg
 *
 */

public interface 
JPCCacheAdapter 
{

	public void	receivedReply( int peer_id, int idle_time, int drop_timeout );
	
	public void receivedActiveAck( int session_idle_time, int session_drop_time );  
  
	public void
	receivedBye(
		String	reason );
	

	public void
	receivedError(
		int		peer_id,
		String reason );
  
  
  public void connectSuccess( JPCCache cache, boolean is_new_discovery );

  public void connectionError( JPCException error );
}
