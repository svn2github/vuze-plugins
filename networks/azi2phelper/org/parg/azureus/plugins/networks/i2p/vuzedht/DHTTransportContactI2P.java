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

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.parg.azureus.plugins.networks.i2p.snarkdht.NodeInfo;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;

public class 
DHTTransportContactI2P 
	implements DHTTransportContact
{
	protected static final byte[]	DEFAULT_TOKEN = {};
	
	private DHTTransportI2P		transport;
	private NodeInfo			node;
	private byte				version;
	private int					instance_id;
	private long				skew;
	private byte				generic_flags;
	
	private InetSocketAddress	address;
	
	private byte[]				id;
	
	
	private byte[]				random_id	= DEFAULT_TOKEN;
	private long				random_id_set_time;				
	
	protected
	DHTTransportContactI2P(
		DHTTransportI2P		_transport,
		NodeInfo			_node,
		byte				_version,
		int					_instance_id,
		long				_skew,
		byte				_generic_flags )
	{
		transport 		= _transport;
		node			= _node;
		version			= _version;
		instance_id		= _instance_id;
		skew			= _skew;
		generic_flags	= _generic_flags;
		
		String 	host = Base32.encode( node.getHash().getData())  + ".b32.i2p";
		
		address = InetSocketAddress.createUnresolved( host, node.getPort());

		id		= node.getNID().getData();
	}
	
	protected void
	setDetails(
		int			_instance_id,
		byte		_flags )
	{
		instance_id		= _instance_id;
		generic_flags	= _flags;
	}
	
	protected void
	setDetails(
		int			_instance_id,
		long		_skew,
		byte		_flags )
	{
		instance_id		= _instance_id;
		skew			= _skew;
		generic_flags	= _flags;
	}
	
	protected NodeInfo
	getNode()
	{
		return( node );
	}
	
	protected void
	setProtocolVersion(
		byte		_version )
	{
		version	= _version;
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
		return( instance_id );
	}
	
	public byte[]
	getID()
	{
		return( id );
	}
	
	public byte
	getProtocolVersion()
	{
		return( version );
	}
	
	public long
	getClockSkew()
	{
		return( skew );
	}
	
	public int
	getRandomIDType()
	{
		return( RANDOM_ID_TYPE2 );
	}
	
	public void
	setRandomID(
		int	id )
	{
		System.out.println( "nuhuh" );
	}
	
	public int
	getRandomID()
	{
		System.out.println( "nuhuh" );
		
		return(0);
	}
	
	public void
	setRandomID2(
		byte[]		id )
	{
		random_id = id;
		
		random_id_set_time = SystemTime.getMonotonousTime();
	}
	
	protected long
	getRandomID2Age()
	{
		if ( random_id_set_time == 0  ){
			
			return( -1 );
		}
		
		return( SystemTime.getMonotonousTime() - random_id_set_time );
	}
	
	public byte[]
	getRandomID2()
	{
		return( random_id );
	}
	
	public String
	getName()
	{
		return( address.toString());
	}
	
	public byte[]
	getBloomKey()
	{
		return( node.getHash().getData());
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
		return(( generic_flags & DHTTransportUDP.GF_DHT_SLEEPING ) != 0 );
	}
	
	public void
	sendPing(
		DHTTransportReplyHandler	handler )
	{
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
		transport.sendStore( handler, this, keys, value_sets );
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
		byte[]						id,
		short						flags )
	{		
		transport.sendFindNode( handler, this, id, flags );
	}
		
	public void
	sendFindValue(
		DHTTransportReplyHandler	handler,
		byte[]						key,
		int							max_values,
		short						flags )
	{
		boolean priority = (flags&I2PHelperAZDHT.FLAG_HIGH_PRIORITY) != 0;

		transport.sendFindValue( handler, this, key, flags, priority );
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
		return( transport.getFullStats( this ));
	}
	
	public void
	exportContact(
		DataOutputStream	os )
	
		throws IOException, DHTTransportException
	{
		transport.exportContact( os, this );
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
		return( getName() + ",nid=" + ByteFormatter.encodeString( node.getNID().getData()) + ",v=" + version);
	}
}
