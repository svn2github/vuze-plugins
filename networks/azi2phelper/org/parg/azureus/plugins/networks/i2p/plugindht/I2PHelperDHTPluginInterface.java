/*
 * Created on Sep 6, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package org.parg.azureus.plugins.networks.i2p.plugindht;

import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface;
import com.aelitis.azureus.plugins.dht.DHTPluginKeyStats;
import com.aelitis.azureus.plugins.dht.DHTPluginListener;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;

public class 
I2PHelperDHTPluginInterface
	implements DHTPluginInterface
{
	public boolean
	isEnabled()
	{
		return( true );
	}
	
	public boolean
	isExtendedUseAllowed()
	{
		return( true );
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		return( null );
	}
	
	public DHTPluginKeyStats
	decodeStats(
		DHTPluginValue		value )
	{
		return( null );
	}

	public void
	registerHandler(
		byte[]							handler_key,
		final DHTPluginTransferHandler	handler )
	{
		Debug.out( "not imp" );
	}	
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address )
	{
		return( null );
	}
		
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address,
		byte							version )
	{
		return( null );
	}
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address,
		byte							version,
		boolean							is_cvs )
	{
		return( null );
	}
	
	public void
	get(
		byte[]								original_key,
		String								description,
		byte								flags,
		int									max_values,
		long								timeout,
		boolean								exhaustive,
		boolean								high_priority,
		DHTPluginOperationListener			original_listener )
				{
		
		}
	
	public void
	put(
		byte[]						key,
		String						description,
		byte[]						value,
		byte						flags,
		DHTPluginOperationListener	listener)
				{
		
		}
	
	public void
	remove(
		byte[]						key,
		String						description,
		DHTPluginOperationListener	listener )
				{
		
		}
	
	public void
	remove(
		DHTPluginContact[]			targets,
		byte[]						key,
		String						description,
		DHTPluginOperationListener	listener )
		{
		
		}
	
	public void
	addListener(
		DHTPluginListener	l )
	{	
	}

	
	public void
	removeListener(
		DHTPluginListener	l )
	{	
	}

	
	public void
	log(
		String	str )
				{
		
		}
}
