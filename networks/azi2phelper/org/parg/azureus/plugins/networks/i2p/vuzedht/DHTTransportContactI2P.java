/*
 * Created on Apr 16, 2014
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


package org.parg.azureus.plugins.networks.i2p.vuzedht;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class 
DHTTransportContactI2P 
	implements DHTTransportContact
{
	private DHTTransportI2P		transport;
	private InetSocketAddress	address;
	private byte[]				id;
	
	
	private int	random_id;
	
	protected
	DHTTransportContactI2P(
		DHTTransportI2P		_transport,
		InetSocketAddress	_address,
		byte[]				_id )
	{
		transport 	= _transport;
		address		= _address;
		id			= _id;
	}
	
	public int
	getMaxFailForLiveCount()
	{
		return( 3 );
	}
	
	public int
	getMaxFailForUnknownCount()
	{
		return( 2 );
	}
	
	public int
	getInstanceID()
	{
		return( 0 );
	}
	
	public byte[]
	getID()
	{
		return( id );
	}
	
	public byte
	getProtocolVersion()
	{
		return( 1 );
	}
	
	public long
	getClockSkew()
	{
		return( 0 );
	}
	
	public void
	setRandomID(
		int	id )
	{
		random_id	= id;
	}
	
	public int
	getRandomID()
	{
		return( random_id );
	}
	
	public String
	getName()
	{
		return( address.toString());
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( address );
	}
	
	public InetSocketAddress
	getTransportAddress()
	{
		return( address );
	}
	
	public InetSocketAddress
	getExternalAddress()
	{
		return( address );
	}
	
	public boolean
	isAlive(
		long		timeout )
	{
		return( true );	// derp
	}

	public void
	isAlive(
		DHTTransportReplyHandler	handler,
		long						timeout )
	{
		
	}
	
	public boolean
	isValid()
	{
		return( true );
	}
	
	public boolean
	isSleeping()
	{
		return( false );
	}
	
	public void
	sendPing(
		DHTTransportReplyHandler	handler )
	{
		
	}
	
	public void
	sendImmediatePing(
		DHTTransportReplyHandler	handler,
		long						timeout )
	{
		
	}

	public void
	sendStats(
		DHTTransportReplyHandler	handler )
	{
		
	}
	
	public void
	sendStore(
		DHTTransportReplyHandler	handler,
		byte[][]					keys,
		DHTTransportValue[][]		value_sets,
		boolean						immediate )
	{
		
	}
	
	public void
	sendQueryStore(
		DHTTransportReplyHandler	handler,
		int							header_length,
		List<Object[]>				key_details )
	{
		
	}
	
	public void
	sendFindNode(
		DHTTransportReplyHandler	handler,
		byte[]						id )
	{
		
	}
		
	public void
	sendFindValue(
		DHTTransportReplyHandler	handler,
		byte[]						key,
		int							max_values,
		byte						flags )
	{
		
	}
		
	public void
	sendKeyBlock(
		DHTTransportReplyHandler	handler,
		byte[]						key_block_request,
		byte[]						key_block_signature )
	{
		
	}

	public DHTTransportFullStats
	getStats()
	{
		return( null );
	}
	
	public void
	exportContact(
		DataOutputStream	os )
	
		throws IOException, DHTTransportException
	{
			
	}
	
	public void
	remove()
	{
		
	}
	
	public void
	createNetworkPositions(
		boolean		is_local )
	{
	}
			
	public DHTNetworkPosition[]
	getNetworkPositions()
	{
		return( new DHTNetworkPosition[0]);
	}
	
	public DHTNetworkPosition
	getNetworkPosition(
		byte	position_type )
	{
		return( null );
	}

	public DHTTransport
	getTransport()
	{
		return( transport );
	}
	
	public String
	getString()
	{
		return( getName());
	}
}
