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

import net.i2p.data.Base32;
import net.i2p.data.Destination;

import org.gudy.azureus2.core3.util.Debug;
import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;

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
	
	private NodeInfo			node;
	private InetSocketAddress	address;
	
	private byte[]				id;
	
	
	private int	random_id;
	
	protected
	DHTTransportContactI2P(
		DHTTransportI2P		_transport,
		NodeInfo			_node )
	{
		transport 	= _transport;
		node		= _node;
		
		String 	host = Base32.encode( node.getHash().getData())  + ".b32.i2p";
		
		address = InetSocketAddress.createUnresolved( host, node.getPort());

		id		= node.getNID().getData();
	}
	
	protected NodeInfo
	getNode()
	{
		return( node );
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
		System.out.println( "isAlive" );
		
		return( true );	// derp
	}

	public void
	isAlive(
		DHTTransportReplyHandler	handler,
		long						timeout )
	{
		System.out.println( "isAlive2" );
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
		System.out.println( "sendPing" );
		
		transport.sendPing( handler, this );
	}
	
	public void
	sendImmediatePing(
		DHTTransportReplyHandler	handler,
		long						timeout )
	{
		Debug.out( "Not Supported" );
		
		handler.failed( this, new Exception( "Not Supported" ));
	}

	public void
	sendStats(
		DHTTransportReplyHandler	handler )
	{
		Debug.out( "Not Supported" );
		
		handler.failed( this, new Exception( "Not Supported" ));
	}
	
	public void
	sendStore(
		DHTTransportReplyHandler	handler,
		byte[][]					keys,
		DHTTransportValue[][]		value_sets,
		boolean						immediate )
	{
		System.out.println( "sendStore" );
	}
	
	public void
	sendQueryStore(
		DHTTransportReplyHandler	handler,
		int							header_length,
		List<Object[]>				key_details )
	{
		Debug.out( "Not Supported" );
		
		handler.failed( this, new Exception( "Not Supported" ));
	}
	
	public void
	sendFindNode(
		DHTTransportReplyHandler	handler,
		byte[]						id )
	{
		System.out.println( "sendFindNode" );
	}
		
	public void
	sendFindValue(
		DHTTransportReplyHandler	handler,
		byte[]						key,
		int							max_values,
		byte						flags )
	{
		System.out.println( "sendFindValue" );
	}
		
	public void
	sendKeyBlock(
		DHTTransportReplyHandler	handler,
		byte[]						key_block_request,
		byte[]						key_block_signature )
	{
		Debug.out( "Not Supported" );
		
		handler.failed( this, new Exception( "Not Supported" ));	
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
		transport.exportContact( os, node );
	}
	
	public void
	remove()
	{
		transport.removeContact( this );
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
