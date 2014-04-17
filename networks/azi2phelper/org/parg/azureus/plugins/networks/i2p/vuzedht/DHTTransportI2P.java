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

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.parg.azureus.plugins.networks.i2p.dht.NID;

import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionMuxedListener;
import net.i2p.data.Base32;

import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportRequestHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportStatsImpl;

public class 
DHTTransportI2P
	implements DHTTransport, I2PSessionMuxedListener
{
	private static final byte PROTOCOL_VERSION		= 1;
	private static final byte PROTOCOL_VERSION_MIN	= 1;
	
	private I2PSession					session;
	private int							query_port;
	private int							reply_port;
	private NID							my_nid;
	
	private DHTTransportStatsImpl		stats;
	
	private DHTTransportContactI2P		local_contact;
	
	private DHTTransportRequestHandler	request_handler;
	
	private long		timeout;
	
	protected
	DHTTransportI2P(
		I2PSession 		_session,
		int				_query_port,
		NID				_my_nid )
	{
		session 	= _session;
		query_port	= _query_port;
		reply_port	= query_port+1;
		my_nid		= _my_nid;
		
		stats = new DHTTransportStatsI2P();
		
		String my_host = Base32.encode(session.getMyDestination().calculateHash().getData()) + ".b32.i2p";
		
		InetSocketAddress address = InetSocketAddress.createUnresolved( my_host, query_port );
		
		local_contact = new DHTTransportContactI2P( this, address, my_nid.getData());
		
        session.addMuxedSessionListener( this, I2PSession.PROTO_DATAGRAM_RAW, reply_port );
        session.addMuxedSessionListener( this, I2PSession.PROTO_DATAGRAM, query_port );
	}
		
    public void 
    messageAvailable(
    	I2PSession 		session, 
    	int 			msgId, 
    	long 			size, 
    	int 			proto, 
    	int 			fromport, 
    	int 			toport )
    {
    	System.out.println( "Received incoming message!" );
    }

    	// these not relevant for muxed listener
    
    public void 
    messageAvailable(
    	I2PSession 	session, 
    	int 		msgId, 
    	long 		size )
    {
    }

    public void 
    reportAbuse(
    	I2PSession 	session, 
    	int 		severity )
    {
    }

    public void 
    disconnected(
    	I2PSession 	session )
    {
    }

    public void 
    errorOccurred(
    	I2PSession 	session, 
    	String 		message, 
    	Throwable 	error ) 
    {
    }
    
	public byte
	getProtocolVersion()
	{
		return( PROTOCOL_VERSION );
	}
	
	public byte
	getMinimumProtocolVersion()
	{
		return( PROTOCOL_VERSION_MIN );
	}
	
	public int
	getNetwork()
	{
		return( DHTI2P.DHT_NETWORK );
	}

	public boolean
	isIPV6()
	{
		return( false );
	}
	
	public byte
	getGenericFlags()
	{
		return( 0 );
	}
	
	public void
	setGenericFlag(
		byte		flag,
		boolean		value )
	{
		
	}
	
	public void
	setSuspended(
		boolean			susp )
	{
		
	}

	
	public DHTTransportContact
	getLocalContact()
	{
		return( local_contact );
	}
	
	public int
	getPort()
	{
		return( query_port );
	}
	
	public void
	setPort(
		int	port )
	
		throws DHTTransportException
	{
		throw( new RuntimeException( "Not Supported" ));
	}
	
	public long
	getTimeout()
	{
		return( timeout );
	}
	
	public void
	setTimeout(
		long		millis )
	{
		timeout	= millis;
	}
	
	public DHTTransportContact
	importContact(
		DataInputStream		is,
		boolean				is_bootstrap )
	
		throws IOException, DHTTransportException
	{
		return( null );
	}
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	receiver )
	{
		request_handler	= receiver;
	}
	
	public DHTTransportStats
	getStats()
	{
		return( stats );
	}
	
		// direct contact-contact communication
	
	public void
	registerTransferHandler(
		byte[]						handler_key,
		DHTTransportTransferHandler	handler )
	{
		throw( new RuntimeException( "Not Supported" ));
	}
	
	public byte[]
	readTransfer(
		DHTTransportProgressListener	listener,
		DHTTransportContact				target,
		byte[]							handler_key,
		byte[]							key,
		long							timeout )
	
		throws DHTTransportException
	{
		throw( new RuntimeException( "Not Supported" ));
	}
	
	public void
	writeTransfer(
		DHTTransportProgressListener	listener,
		DHTTransportContact				target,
		byte[]							handler_key,
		byte[]							key,
		byte[]							data,
		long							timeout )
	
		throws DHTTransportException
	{
		throw( new RuntimeException( "Not Supported" ));
	}
	
	public byte[]
	writeReadTransfer(
		DHTTransportProgressListener	listener,
		DHTTransportContact				target,
		byte[]							handler_key,
		byte[]							data,
		long							timeout )	
	
		throws DHTTransportException
	{
		throw( new RuntimeException( "Not Supported" ));
	}

	public boolean
	supportsStorage()
	{
		return( true );
	}
	
	public boolean
	isReachable()
	{
		return( true );
	}
	
	public DHTTransportContact[]
	getReachableContacts()
	{
		return( new DHTTransportContact[0]);
	}
	
	public DHTTransportContact[]
	getRecentContacts()
	{
		return( new DHTTransportContact[0]);		
	}
	
	public void
	addListener(
		DHTTransportListener	l )
	{
		
	}
	
	public void
	removeListener(
		DHTTransportListener	l )
	{
		
	}
	
	private class
	DHTTransportStatsI2P
		extends DHTTransportStatsImpl
	{
		private 
		DHTTransportStatsI2P()
		{
			super( PROTOCOL_VERSION );
		}
		
		public DHTTransportStats snapshot() {
			
			DHTTransportStatsImpl res = new DHTTransportStatsI2P();
			
			snapshotSupport( res );
			
			return( res );
		}
		
		@Override
		public int getRouteablePercentage() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public long getRequestsTimedOut() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public long getPacketsSent() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public long getPacketsReceived() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public long getBytesSent() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public long getBytesReceived() {
			// TODO Auto-generated method stub
			return 0;
		}
	}
}
