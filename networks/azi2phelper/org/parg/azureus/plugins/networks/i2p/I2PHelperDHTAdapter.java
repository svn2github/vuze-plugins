/*
 * Created on May 6, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.parg.azureus.plugins.networks.i2p;

import org.parg.azureus.plugins.networks.i2p.vuzedht.DHTTransportContactI2P;

public abstract class 
I2PHelperDHTAdapter 
	implements I2PHelperDHTListener
{
	public void
	searching(
		String		host )
	{
	}

	public void
	valueRead(
		DHTTransportContactI2P		contact,
		String						host,
		int							contact_state )
	{
	}

	public void
	complete(
		boolean		timeout )
	{
	}
}
