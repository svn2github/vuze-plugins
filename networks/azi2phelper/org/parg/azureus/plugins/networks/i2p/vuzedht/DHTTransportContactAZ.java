/*
 * Created on Jul 16, 2014
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



import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;

public class 
DHTTransportContactAZ
	implements DHTTransportContact
{
	private DHTTransportAZ			transport;
	private DHTTransportContactI2P	basis;	// DON'T use this for any transport operations as DHTAZClient relies on this NOT BEING DONE
	
	protected
	DHTTransportContactAZ(
		DHTTransportAZ			_transport,
		DHTTransportContactI2P	_basis )
	{
		transport		= _transport;
		basis			= _basis;
	}
	
	protected DHTTransportContactI2P
	getBasis()
	{
		return( basis );
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
		return( basis.getInstanceID());
	}
	
	public byte[]
	getID()
	{
		return( basis.getID());
	}
	
	public byte
	getProtocolVersion()
	{
			// TODO: There is some interaction with the protocol version and the DHTControlImpl etc
		
		return( basis.getProtocolVersion());
	}
	
	public long
	getClockSkew()
	{
		return( basis.getClockSkew());
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
		basis.setRandomID2( id );
	}
	
	protected long
	getRandomID2Age()
	{
		return( basis.getRandomID2Age());
	}
	
	public byte[]
	getRandomID2()
	{
		return( basis.getRandomID2());
	}
	
	public String
	getName()
	{
		return( basis.getName());
	}
	
	public byte[]
	getBloomKey()
	{
		return( basis.getBloomKey());
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( basis.getAddress());
	}
	
	public InetSocketAddress
	getTransportAddress()
	{
		return( getAddress());
	}
	
	public InetSocketAddress
	getExternalAddress()
	{
		return( getAddress());
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
		return(( transport.getGenericFlags() & DHTTransportUDP.GF_DHT_SLEEPING ) != 0 );
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
		transport.sendFindValue( handler, this, key, max_values, flags );
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
		basis.exportContact( os );
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
		return( "AZ:" + basis.getString());
	}
}
