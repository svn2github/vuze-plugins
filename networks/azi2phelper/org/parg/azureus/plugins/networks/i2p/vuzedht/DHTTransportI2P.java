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
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.parg.azureus.plugins.networks.i2p.dht.NID;
import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;

import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionMuxedListener;
import net.i2p.client.SendMessageOptions;
import net.i2p.client.datagram.I2PDatagramDissector;
import net.i2p.client.datagram.I2PDatagramMaker;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.Hash;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFindValueReply;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportRequestHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportRequestCounter;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportStatsImpl;
import com.aelitis.azureus.util.MapUtils;

public class 
DHTTransportI2P
	implements DHTTransport, I2PSessionMuxedListener
{
	private static final boolean TRACE = false;
	
	private static final byte PROTOCOL_VERSION		= 1;
	private static final byte PROTOCOL_VERSION_MIN	= 1;
	
	private static final int NUM_WANT	= 16;
	
	private I2PSession					session;
	private NodeInfo					my_node;
	private int							query_port;
	private int							reply_port;
	private NID							my_nid;
	
	private DHTTransportStatsI2P		stats;
	
	private DHTTransportContactI2P		local_contact;
	
	private DHTTransportRequestHandler	request_handler;
	
	private TimerEventPeriodic 			timer_event;
	
	private long						request_timeout;
	
	private static final int TOKEN_MAP_MAX = 512;
	
	private Map<HashWrapper,NodeInfo>	token_map =
			new LinkedHashMap<HashWrapper,NodeInfo>(TOKEN_MAP_MAX,0.75f,true)
			{
				protected boolean 
				removeEldestEntry(
			   		Map.Entry<HashWrapper,NodeInfo> eldest) 
				{
					return size() > TOKEN_MAP_MAX;
				}
			};
	
	private ThreadPool	destination_lookup_pool 	= new ThreadPool("DHTTransportI2P::destlookup", 5, true );

	private volatile boolean			destroyed;
	
	protected
	DHTTransportI2P(
		I2PSession 		_session,
		NodeInfo		_my_node,
		int				_request_timeout )
	{
		session 		= _session;
		my_node			= _my_node;
		request_timeout	= _request_timeout;
		
		query_port	= my_node.getPort();
		reply_port	= query_port+1;
		my_nid		= my_node.getNID();
		
		stats = new DHTTransportStatsI2P();
				
		local_contact = new DHTTransportContactI2P( this, my_node );
		
        session.addMuxedSessionListener( this, I2PSession.PROTO_DATAGRAM_RAW, reply_port );
        session.addMuxedSessionListener( this, I2PSession.PROTO_DATAGRAM, query_port );
        
        timer_event = 
				SimpleTimer.addPeriodicEvent(
					"DHTTransportI2P:timeouts",
					5000,
					new TimerEventPerformer()
					{
						public void
						perform(
							TimerEvent	event )
						{
							if ( destroyed ){
								
								timer_event.cancel();
								
							}else{
							
								checkTimeouts();
							}
						}
					});
			
	}
	
	protected DHTTransportContactI2P
	importContact(
		NodeInfo		node,
		boolean			is_bootstrap )
	{
		DHTTransportContactI2P	contact = new DHTTransportContactI2P( this, node );
		
		request_handler.contactImported( contact, is_bootstrap );
		
		return( contact );
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
	
	public int
	getReplyPort()
	{
		return( reply_port );
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
		return( request_timeout );
	}
	
	public void
	setTimeout(
		long		millis )
	{
		request_timeout	= millis;
	}
	
	protected DHTTransportFullStats
	getFullStats(
		DHTTransportContactI2P	contact )
	{
		if ( contact == local_contact ){
			
			return( request_handler.statsRequest( contact ));
		}
		
		Debug.out( "Status not supported for remote contacts" );
		
		return( null );
	}
	
	protected void
	exportContact(
		DataOutputStream		os,
		NodeInfo				node )
		
		throws IOException, DHTTransportException
	{
		os.writeByte( 0 );	// version
		
		DHTUtilsI2P.serialiseByteArray( os, node.getNID().getData(), 255 );
		
		os.writeInt( node.getPort());
		
		DHTUtilsI2P.serialiseByteArray( os, node.getHash().getData(), 255 );
			
		Destination dest = node.getDestination();
		
		byte[] b_dest;
		
		if ( dest == null ){
			
			b_dest = new byte[0];
			
		}else{
			
			b_dest = dest.toByteArray();
		}
		
		DHTUtilsI2P.serialiseByteArray( os, b_dest, 512 );
	}
	
	public DHTTransportContact
	importContact(
		DataInputStream		is,
		boolean				is_bootstrap )
	
		throws IOException, DHTTransportException
	{
		is.readByte();
		
		byte[]	b_nid = DHTUtilsI2P.deserialiseByteArray( is, 255 );
		
		int port = is.readInt();
		
		byte[]	b_hash = DHTUtilsI2P.deserialiseByteArray( is, 255 );

		byte[]	b_dest = DHTUtilsI2P.deserialiseByteArray( is, 512 );

		NodeInfo node;
		
		if ( b_dest.length == 0 ){
			
			node = new NodeInfo( new NID(b_nid), new Hash( b_hash ), port );
			
		}else{
			
			Destination dest = new Destination();
			
			try{
				dest.fromByteArray( b_dest );
			
			}catch( DataFormatException e ){
				
				throw( new IOException( e ));
			}
			
			node = new NodeInfo( new NID(b_nid), dest, port );
		}
		
		DHTTransportContact contact = new DHTTransportContactI2P( this, node );
		
		request_handler.contactImported( contact, is_bootstrap );

		return( contact );
	}
	
	protected void
	removeContact(
		DHTTransportContactI2P	contact )
	{
		request_handler.contactRemoved( contact );
	}
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	_request_handler )
	{
		request_handler	= new DHTTransportRequestCounter( _request_handler, stats );
	}
	
	public DHTTransportStats
	getStats()
	{
		return( stats );
	}
	
		// RPCs
	
	final int DEST_LOOKUP_TIMEOUT = 20*1000;

	private HashMap<HashWrapper,Request>		requests = new HashMap<HashWrapper, Request>();
	
	public void
	sendPing(
		Destination		dest,
		int				port )
	{
			// used to help bootstrap 
		
		sendPing( new NodeInfo( dest, port ), false );
	}
	
	protected boolean
	sendPing(
		NodeInfo		node )
	{
		return( sendPing( node, true ));
	}
	
	private boolean
	sendPing(
		NodeInfo		node,
		boolean			wait_for_reply )
	{
		final boolean[] result = { false };
		
		try{
	        stats.pingSent( null );
	
	        Map<String, Object> map = new HashMap<String, Object>();
	        	        
	        map.put( "q", "ping" );
	        
	        Map<String, Object> args = new HashMap<String, Object>();
	        
	        map.put( "a", args );
	        
	        final AESemaphore sem = new AESemaphore( "i2p:wait" );
	        
	        sendQuery( 
	        	new ReplyHandler()
	        	{
	        		@Override
	        		public void 
	        		handleReply(
	        			Map reply ) 
	        		{	        			
	        			stats.pingOK();
	        			
	        			result[0] = true;
	        			
	        			sem.release();
	        		}
	        		
	        		@Override
	        		public void 
	        		handleError(
	        			DHTTransportException error) 
	        		{	        			
	        			stats.pingFailed();
	        			
	        			sem.release();
	        		}
	        	}, 
	        	node, map, true );
	        
	        if ( wait_for_reply ){
	        
	        	sem.reserve();
	        }
	        
		}catch( Throwable e ){
		}
		
		return( result[0] );
	}
	
	public void
	sendPing(
		final DHTTransportReplyHandler		handler,
		final DHTTransportContactI2P		contact )
	{
		if ( TRACE ) trace( "sendPing" );
		
		try{
	        stats.pingSent( null );

	        Map<String, Object> map = new HashMap<String, Object>();
	        
	        map.put( "q", "ping" );
	        
	        Map<String, Object> args = new HashMap<String, Object>();
	        
	        map.put( "a", args );
	        
	        sendQuery( 
	        	new ReplyHandler()
	        	{
	        		@Override
	        		public void 
	        		handleReply(
	        			Map reply ) 
	        		{
	        			if ( TRACE ) trace( "good pingReply" );
	        			
	        			handler.pingReply( contact, -1 );
	        			
	        			stats.pingOK();
	        		}
	        		
	        		@Override
	        		public void 
	        		handleError(
	        			DHTTransportException error) 
	        		{
	        			if ( TRACE ) trace( "error pingReply: " + Debug.getNestedExceptionMessage( error ));

	        			handler.failed( contact, error );
	        			
	        			stats.pingFailed();
	        		}
	        	}, 
	        	contact, map, true );
	        	        
		}catch( Throwable e ){
			
			if ( e instanceof DHTTransportException ){
				
				handler.failed( contact, (DHTTransportException)e) ;
				
			}else{
				
				handler.failed( contact, new DHTTransportException( "ping failed", e )) ;
			}
		}
    }
    
	private void
	receivePing(
		DHTTransportContactI2P		originator,
		byte[]						message_id )
		
		throws Exception
	{
		if ( TRACE ) trace( "receivePing" );

		request_handler.pingRequest( originator );

		Map<String, Object> map = new HashMap<String, Object>();
		
		Map<String, Object> resps = new HashMap<String, Object>();
		
		map.put("r", resps);
		
		sendResponse( originator, message_id, map );
	}
	   
	protected boolean
	sendFindNode(
		NodeInfo		node,
		byte[]			target )
	{
		final boolean[] result = { false };
		
		try{
	        stats.findNodeSent( null );
	
	        Map<String, Object> map = new HashMap<String, Object>();
	        
	        map.put( "q", "find_node" );
	        
	        Map<String, Object> args = new HashMap<String, Object>();
	        
	        map.put( "a", args );
	        
	        args.put( "target", target );
	        
	        final AESemaphore sem = new AESemaphore( "i2p:wait" );
	        
	        sendQuery( 
	        	new ReplyHandler()
	        	{
	        		@Override
	        		public void 
	        		handleReply(
	        			Map reply ) 
	        		{
	        			byte[]	nodes = (byte[])reply.get( "nodes" );
	        				        				        			
	        			for ( int off = 0; off < nodes.length; off += NodeInfo.LENGTH ){
	        				
	        				NodeInfo node = new NodeInfo( nodes, off );
	        				
	        				request_handler.contactImported( new DHTTransportContactI2P( DHTTransportI2P.this, node ), false );
	        			}
	        			
	        			stats.findNodeOK();
	        			
	        			result[0] = true;
	        			
	        			sem.release();
	        		}
	        		
	        		@Override
	        		public void 
	        		handleError(
	        			DHTTransportException error) 
	        		{
	        			
	        			stats.findNodeFailed();
	        			
	        			sem.release();
	        		}
	        	}, 
	        	node, map, true );
	        
	        sem.reserve();
	        
		}catch( Throwable e ){
		}
		
		return( result[0] );
	}
	
	public void
	sendFindNode(
		final DHTTransportReplyHandler		handler,
		final DHTTransportContactI2P		contact,
		byte[]								target,
		short								flags )
	{
		if ( TRACE ) trace( "sendFindNode, flags=" + flags );
		
		if (( flags & DHT.FLAG_LOOKUP_FOR_STORE ) != 0 ){
			
				// only way tokens get allocated is via findValue (get_peers)
				// so we frig this by creating an obfuscated derived key that really shouldn't
				// happen to return anything other than peers...
			
			byte[] new_target = new byte[target.length];
						
			RandomUtils.nextBytes( new_target );
			
			System.arraycopy( target, 0, new_target, 0, target.length - 8 );
			
			sendFindValue( 
				new DHTTransportReplyHandlerAdapter()
				{
					public void
					findValueReply(
						DHTTransportContact 	contact,
						DHTTransportValue[]		values,
						byte					diversification_type,
						boolean					more_to_come )
					{
						handler.findNodeReply( contact, new DHTTransportContact[0] );
					}
					
					public void
					findValueReply(
						DHTTransportContact 	contact,
						DHTTransportContact[]	contacts )
					{
						handler.findNodeReply( contact, contacts );
					}
					
					public void 
					failed(
						DHTTransportContact 	contact,
						Throwable 				error) 
					{
						handler.failed( contact, error );
					}
				},
				contact,
				new_target );
			
		}else{
			try{
		        stats.findNodeSent( null );
	
		        Map<String, Object> map = new HashMap<String, Object>();
		        
		        map.put( "q", "find_node" );
		        
		        Map<String, Object> args = new HashMap<String, Object>();
		        
		        map.put( "a", args );
		        
		        args.put( "target", target );
		        
		        sendQuery( 
		        	new ReplyHandler()
		        	{
		        		@Override
		        		public void 
		        		handleReply(
		        			Map reply ) 
		        		{
		        			if ( TRACE ) trace( "good findNodeReply: " + reply );
		        				        			
		        			/* no token on findNode
		        			byte[]	token = (byte[])reply.get( "token" );
		        			if ( token != null ){
		        			}
		        			*/
		        			
		        			byte[]	nodes = (byte[])reply.get( "nodes" );
		        			
		        			DHTTransportContactI2P[]	contacts  = new DHTTransportContactI2P[nodes.length/NodeInfo.LENGTH];
		        			
		        			int	pos = 0;
		        			
		        			for ( int off = 0; off < nodes.length; off += NodeInfo.LENGTH ){
		        				
		        				NodeInfo node = new NodeInfo( nodes, off );
		        				
		        				contacts[pos++] = new DHTTransportContactI2P( DHTTransportI2P.this, node );
		        			}
		        			
		        			handler.findNodeReply( contact, contacts );
		        			
		        			stats.findNodeOK();
		        		}
		        		
		        		@Override
		        		public void 
		        		handleError(
		        			DHTTransportException error) 
		        		{
		        			if ( TRACE ) trace( "error findNodeReply: " + Debug.getNestedExceptionMessage( error ));
	
		        			handler.failed( contact, error );
		        			
		        			stats.findNodeFailed();
		        		}
		        	}, 
		        	contact, map, true );
		        	        
			}catch( Throwable e ){
				
				if ( e instanceof DHTTransportException ){
					
					handler.failed( contact, (DHTTransportException)e) ;
					
				}else{
					
					handler.failed( contact, new DHTTransportException( "findNode failed", e )) ;
				}
			}
		}
    }
    
	private void
	receiveFindNode(
		DHTTransportContactI2P		originator,
		byte[]						message_id,
		byte[]						target )
		
		throws Exception
	{
		if ( TRACE ) trace( "receiveFindNode" );

		DHTTransportContact[] contacts = request_handler.findNodeRequest( originator, target );

		Map<String, Object> map = new HashMap<String, Object>();
		
		Map<String, Object> resps = new HashMap<String, Object>();
		
		map.put( "r", resps);

		// no token returned for find-node, just find-value
		// byte[] token = originator.getRandomID2();				
		// resps.put( "token", token );
		
        byte[] nodes = new byte[contacts.length * NodeInfo.LENGTH];
        
        for ( int i=0; i<contacts.length; i++ ){
        	
            System.arraycopy(((DHTTransportContactI2P)contacts[i]).getNode().getData(), 0, nodes, i * NodeInfo.LENGTH, NodeInfo.LENGTH);
        }
        
		resps.put( "nodes", nodes );
		
		sendResponse( originator, message_id, map );
	}
	
	
	public void
	sendFindValue(
		final DHTTransportReplyHandler		handler,
		final DHTTransportContactI2P		contact,
		byte[]								target )
	{
		if ( TRACE ) trace( "sendFindValue: contact=" + contact.getString() + ", target=" + ByteFormatter.encodeString( target ));
		
		try{
	        stats.findValueSent( null );

	        Map<String, Object> map = new HashMap<String, Object>();
	        
	        map.put( "q", "get_peers" );
	        
	        Map<String, Object> args = new HashMap<String, Object>();
	        
	        map.put( "a", args );
	        
	        args.put( "info_hash", target );
	        
	        sendQuery( 
	        	new ReplyHandler()
	        	{
	        		@Override
	        		public void 
	        		handleReply(
	        			Map reply ) 
	        		{
	        			if ( TRACE ) trace( "good sendFindValue: " + reply );
	        			
	        			byte[]	token = (byte[])reply.get( "token" );
	        			
	        			if ( token != null ){
	        			
	        				contact.setRandomID2( token );
	        			}
	        			
	        			byte[]	nodes = (byte[])reply.get( "nodes" );
	        			
	        			if ( nodes != null ){
	        				
		        			DHTTransportContactI2P[]	contacts  = new DHTTransportContactI2P[nodes.length/NodeInfo.LENGTH];
		        			
		        			int	pos = 0;
		        			
		        			for ( int off = 0; off < nodes.length; off += NodeInfo.LENGTH ){
		        				
		        				NodeInfo node = new NodeInfo( nodes, off );
		        				
		        				contacts[pos++] = new DHTTransportContactI2P( DHTTransportI2P.this, node );
		        			}
		        			
		        			handler.findValueReply( contact, contacts );
		        			
	        			}else{
	        			
	        				List<byte[]> peers = (List<byte[]>)reply.get( "values");
	        				
	        				if ( peers == null ){
	        					
	        					peers = new ArrayList<byte[]>(0);
	        				}
	        				
	        				DHTTransportValue[] values = new DHTTransportValue[peers.size()];
	        				
	        				for ( int i=0;i<values.length;i++){
	        					
	        					values[i] = new DHTTransportValueImpl( contact, peers.get(i));
	        				}
	        				
	        				handler.findValueReply( contact, values, DHT.DT_NONE, false );
	        			}
	        			
	        			stats.findValueOK();
	        		}
	        		
	        		@Override
	        		public void 
	        		handleError(
	        			DHTTransportException error) 
	        		{
	        			if ( TRACE ) trace( "error sendFindValue: " + Debug.getNestedExceptionMessage( error ));

	        			handler.failed( contact, error );
	        			
	        			stats.findValueFailed();
	        		}
	        	}, 
	        	contact, map, true );
	        	        
		}catch( Throwable e ){
			
			if ( e instanceof DHTTransportException ){
				
				handler.failed( contact, (DHTTransportException)e) ;
				
			}else{
				
				handler.failed( contact, new DHTTransportException( "findValue failed", e )) ;
			}
		}
    }
	
	
	private void
	receiveFindValue(
		DHTTransportContactI2P		originator,
		byte[]						message_id,
		byte[]						hash )
		
		throws Exception
	{
		if ( TRACE ) trace( "receiveFindValue" );

		DHTTransportFindValueReply reply = request_handler.findValueRequest( originator, hash, NUM_WANT, (byte)0 );

		Map<String, Object> map = new HashMap<String, Object>();
		
		Map<String, Object> resps = new HashMap<String, Object>();
		
		map.put( "r", resps);

		byte[] token = originator.getRandomID2();
						
		resps.put( "token", token );
		
		NodeInfo node = originator.getNode();
		
		synchronized( token_map ){
			
			token_map.put( new HashWrapper( token ), node );
		}
		
		if ( reply.hit()){
			
			DHTTransportValue[] values = reply.getValues();
			
			List<byte[]>	peers = new ArrayList<byte[]>( values.length );
			
				// Snark removes the peer itself from the list returned so a peer can't read its own
				// values stored at a node. This in itself isn't so bad, but what is worse is that
				// Snark will end up returning "nodes" if there was only the one value stored which
				// results in an exhaustive search by the caller in the case where there is only
				// one announcer.... I'm not going to do this
			
			byte[]	caller_hash = originator.getNode().getHash().getData();
			
			for ( DHTTransportValue value: values ){
				
				byte[]	peer_hash = value.getValue();
				
				if ( !Arrays.equals( caller_hash, peer_hash )){
					
					peers.add( value.getValue());
				}
			}
			
			resps.put( "values", peers );
			
		}else{
			
			DHTTransportContact[] contacts = reply.getContacts();
			
	        byte[] nodes = new byte[contacts.length * NodeInfo.LENGTH];
	        
	        for ( int i=0; i<contacts.length; i++ ){
	        	
	            System.arraycopy(((DHTTransportContactI2P)contacts[i]).getNode().getData(), 0, nodes, i * NodeInfo.LENGTH, NodeInfo.LENGTH);
	        }
	        
			resps.put( "nodes", nodes );
		}
		
		if ( TRACE ) trace( "    findValue->" + map);

		sendResponse( originator, message_id, map );
	}
	
	protected void
	sendStore(
		final DHTTransportReplyHandler	handler,
		final DHTTransportContactI2P	contact,
		byte[][]						keys,
		DHTTransportValue[][]			value_sets )
	{
		byte[]	token = contact.getRandomID2();
		
		if ( TRACE ) trace( "sendStore: keys=" + keys.length + ", token=" + token + ", contact=" + contact.getString());
		
		try{
			if ( token == null || token == DHTTransportContactI2P.DEFAULT_TOKEN ){

				throw( new DHTTransportException( "No token available for store operation" ));
			}

			stats.storeSent( null );

			for ( int i=0;i<keys.length;i++){

				final boolean	report_result = (i==keys.length-1);

				try{
					Map<String, Object> map = new HashMap<String, Object>();

					map.put( "q", "announce_peer" );

					Map<String, Object> args = new HashMap<String, Object>();

					map.put( "a", args );

					if ( TRACE ) trace( "   storeKey: " + ByteFormatter.encodeString( keys[i] ));
					
					args.put( "info_hash", keys[i] );

					args.put( "port", 6881 );		// not used but for completeness

					args.put( "token", token );

					sendQuery( 
							new ReplyHandler()
							{
								@Override
								public void 
								handleReply(
									Map 	reply ) 
								{
									if ( TRACE ) trace( "good sendStoreReply" );

									if ( report_result ){

										handler.storeReply( contact, new byte[]{ DHT.DT_NONE });

										stats.storeOK();
									}
								}

								@Override
								public void 
								handleError(
										DHTTransportException error) 
								{
									if ( TRACE ) trace( "error sendStoreReply: " + Debug.getNestedExceptionMessage( error ));

									if ( report_result ){

										handler.failed( contact, error );

										stats.storeFailed();
									}
								}
							}, 
							contact, map, false );		// NOT repliable. Note however that we still get a reply as the target has (or should have) our dest cached against the token...

				}catch( Throwable e ){

					if ( report_result ){

						throw( e );
					}
				}
			}     
		}catch( Throwable e ){

			if ( e instanceof DHTTransportException ){

				handler.failed( contact, (DHTTransportException)e) ;

			}else{

				handler.failed( contact, new DHTTransportException( "sendStore failed", e )) ;
			}
		}	
	}
	
	private void
	receiveStore(
		DHTTransportContactI2P		originator,
		byte[]						message_id,
		byte[]						hash )
		
		throws Exception
	{
		if ( TRACE ) trace( "receiveStore" );
		
		byte[][]				keys 	= new byte[][]{ hash };
		
		DHTTransportValue value = new DHTTransportValueImpl( originator, originator.getNode().getHash().getData());
		
		DHTTransportValue[][]	values 	= new DHTTransportValue[][]{{ value }};
		
		request_handler.storeRequest( originator, keys, values );
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		Map<String, Object> resps = new HashMap<String, Object>();
		
		map.put( "r", resps);
		
		sendResponse( originator, message_id, map );
	}
	
		// -------------
	
	protected boolean
	lookupDest(
		NodeInfo		node )
	{
		try{
				// blocking ok here as this method only used for bootstrap test logic which is
				// already async. Also we want to force the lookup regardless of whether or not
				// we have a dest
			
			Destination dest = session.lookupDest( node.getHash(), DEST_LOOKUP_TIMEOUT );
        
            if ( dest != null ){
            
                node.setDestination(dest);
                
                return( true );
            }
		}catch( Throwable e ){
		}
		
		return( false );
	}
	
	private void 
    sendQuery(
    	ReplyHandler				handler,
    	DHTTransportContactI2P		contact,
    	Map					 		map, 
    	boolean 					repliable ) 
    	
    	throws Exception
    {
	   	NodeInfo node = contact.getNode();
	   
		sendQuery( handler, node, map, repliable );
    }
   
    private void 
    sendQuery(
    	final ReplyHandler				handler,
    	final NodeInfo					node,
    	final Map				 		map, 
    	final boolean 					repliable ) 
    	
    	throws Exception
    {
    	if ( session.isClosed()){
    		
           	throw( new DHTTransportException( "Session closed" ));
    	}
    	   	
    	Destination	dest = node.getDestination();
    	
    	if ( dest == null ) {

    		if ( TRACE ) trace( "Scheduling dest lookup: active=" + destination_lookup_pool.getRunningCount() + ", queued=" + destination_lookup_pool.getQueueSize());
    		
    		destination_lookup_pool.run(
    			new AERunnable() 
    			{
					public void 
					runSupport() 
					{
						try{
							long	start = SystemTime.getMonotonousTime();
							
							Destination dest = session.lookupDest( node.getHash(), DEST_LOOKUP_TIMEOUT );
            
							if ( dest != null ){
            
								if ( TRACE ) trace( "Destination lookup ok - elapsed=" + (SystemTime.getMonotonousTime()-start));
								
								node.setDestination( dest );
								
					    		sendQuery( handler, dest, node.getPort(), map, repliable );
					    		
							}else{
								
								throw( new DHTTransportException( "Destination lookup failed" ));
							}
						}catch( Throwable e ){
							
							if ( e instanceof DHTTransportException ){
								
								handler.handleError((DHTTransportException)e);
								
							}else{
								
								handler.handleError( new DHTTransportException( "Destination lookup failed", e ));
							}
						}
					}
    			});
    	}else{
    		
    		sendQuery( handler, dest, node.getPort(), map, repliable );
    	}
    }
    
    private void 
    sendQuery(
    	ReplyHandler				handler,
    	Destination					dest,
    	int							port,
    	Map				 			map, 
    	boolean 					repliable ) 
    	
    	throws Exception
    {
    	
	    map.put( "y", "q" );
	
	    	// i2p uses 8 byte random message ids and supports receiving up to 16 byte ids
	
	    byte [] msg_id = new byte[8];
	
	    RandomUtils.nextBytes( msg_id );
	
	    map.put("t", msg_id );
	
	    Map<String, Object> args = (Map<String, Object>) map.get("a");
	
	    args.put("id", my_nid.getData());
	    	    
	    if ( !repliable ){
	    	
	    	port++;
	    }
	    
    	synchronized( requests ){
	    		
    		if ( destroyed ){
    			
    			throw( new DHTTransportException( "Transport destroyed" ));
    		}
    		
    		requests.put( new HashWrapper( msg_id ), new Request( handler ));
	    }
	    
	    boolean	ok = false;
	    
	    try{
	    	sendMessage( dest, port, map, repliable );
	    	
	    	ok	= true;
	    	
	    }finally{
	    	
	    	if ( !ok ){
	    	
	    		synchronized( requests ){
	    		
	    			requests.remove( new HashWrapper( msg_id ));
	    		}
	    	}
	    }
	}
    
    private void
    sendResponse(
    	DHTTransportContactI2P 		originator, 
    	byte[]						message_id, 
    	Map							map )
    	
    	throws Exception
    {
    	NodeInfo node = originator.getNode();
    	
        Destination dest = node.getDestination();
        
        if ( dest == null ){
        	
        	Debug.out( "Hmm, destination is null" );
        	
        	return;
        }
        
        map.put( "y", "r" );
        
        map.put( "t", message_id );
        
        Map<String, Object> resps = (Map<String, Object>) map.get("r");
        
        resps.put( "id", my_nid.getData());
        
        sendMessage( dest, node.getPort() + 1, map, false );
    }
    
    
    private static final int SEND_CRYPTO_TAGS 	= 8;
    private static final int LOW_CRYPTO_TAGS 	= 4;

    private void 
    sendMessage(
    	Destination 			dest, 
    	int 					toPort, 
    	Map					 	map, 
    	boolean 				repliable ) 
    	
    	throws Exception
    {

        byte[] payload = BEncoder.encode( map );

        	// Always send query port, peer will increment for unsigned replies
        
        int fromPort = query_port;
        
        if ( repliable ){
        	
            I2PDatagramMaker dgMaker = new I2PDatagramMaker( session );
            
            payload = dgMaker.makeI2PDatagram( payload );
            
            if ( payload == null ){
               
            	throw( new DHTTransportException( "Datagram construction failed" ));
            }
        }

        SendMessageOptions opts = new SendMessageOptions();
        
        opts.setDate( SystemTime.getCurrentTime() + 60*1000);
        
        opts.setTagsToSend(SEND_CRYPTO_TAGS);
        
        opts.setTagThreshold(LOW_CRYPTO_TAGS);
        
        if ( !repliable ){
        	
            opts.setSendLeaseSet( false );
        }
        
        stats.total_packets_sent++;
        stats.total_bytes_sent += payload.length;
        
        if ( session.sendMessage(
           		dest, 
           		payload, 
           		0, 
           		payload.length,
                repliable ? I2PSession.PROTO_DATAGRAM : I2PSession.PROTO_DATAGRAM_RAW,
                fromPort, 
                toPort, 
                opts )){
        	
        }else{
        	
        	throw( new DHTTransportException( "sendMessage failed" ));
        }
    }
    
    public void 
    messageAvailable(
    	I2PSession 		session, 
    	int 			msg_id, 
    	long 			size, 
    	int 			proto, 
    	int 			from_port, 
    	int 			to_port )
    {
    	try{
	    	byte[] payload = session.receiveMessage(msg_id);
	        
	    	if ( payload == null ){
	    	
	    			// seen a few of these, not much we can do!
	    		
	    		return;
	    	}
	    	
	        if ( to_port == query_port ){
	
	        		// repliable
	
	        	I2PDatagramDissector dgDiss = new I2PDatagramDissector();
	
	        	dgDiss.loadI2PDatagram(payload);
	
	        	payload = dgDiss.getPayload();
	
	        	Destination from = dgDiss.getSender();
	
	        	receiveMessage( from, from_port, payload);
	
	        }else if ( to_port == reply_port) {
	
	        	receiveMessage( null, from_port, payload );
	        }
    	}catch( Throwable e ){
    		
    		Debug.out( e );
    	}
    }

    private void 
    receiveMessage(
    	Destination 	from_dest, 
    	int 			from_port, 
    	byte[]			payload ) 
    {
    	stats.total_packets_received++;
    	
    	stats.total_bytes_received += payload.length;
    	
    	try{
	    	Map		map = BDecoder.decode( payload );
	
	        byte[] msg_id = (byte[])map.get( "t" );
	      
	        String type = MapUtils.getMapString( map, "y", "" );
	        
	        if ( type.equals("q")){
	        	
	            	// queries must be repliable
	        	
	            String method = MapUtils.getMapString( map, "q", "" );
	            
	            Map args = (Map)map.get( "a" );
	            
	            receiveQuery( msg_id, from_dest, from_port, method, args );
	            
	        }else if ( type.equals("r") || type.equals("e")){
	        	
	        	Request request;
	        	
	        	synchronized( requests ){
	        		
	        		request = requests.remove( new HashWrapper( msg_id ));
	        	}
	        	
	        	if ( request != null ){
	        		
	        		ReplyHandler reply_handler = request.getHandler();
	        		
	        		try{
		                if ( type.equals("r")){
		                	
		                    Map reply = (Map)map.get( "r" );
		                    
		                    reply_handler.handleReply( reply );
		                    
		                }else{
		                	
	                		List error = (List)map.get("e");
		                    
	                		reply_handler.handleError( new DHTTransportException( "Received error: " + error ));
		                }
	        		}catch( Throwable e ){
	        			
	        			reply_handler.handleError( new DHTTransportException( "Reply processing failed", e ));
	        		}
	        	}
	        }
    	}catch( Throwable e ){
    		
    		Debug.out( e );
    	}
    }
    
    private void 
    receiveQuery(
    	byte[] 			msg_id, 
    	Destination 	dest, 
    	int 			from_port, 
    	String 			method, 
    	Map				args )
    	
    	throws Exception
    {
    	stats.incomingRequestReceived( null, false );
    	
        if ( dest == null && !method.equals( "announce_peer")){
    
        		// only announce is valid without a replyable dest
        	
            return;
        }
        
        byte[] nid = (byte[])args.get("id");
        
        byte[] token	= null;
        
        NodeInfo node;
        
        if ( dest != null ){
        	
        	node = new NodeInfo(new NID(nid), dest, from_port);
           
        }else{
        	
            token = (byte[])args.get("token");

        	if ( token == null ){
        		
        		if ( TRACE ) trace( "Token missing, store deined" );
        		
        		return;
        	}
        	
        	synchronized( token_map ){
        		
        		node = token_map.get( new HashWrapper( token ));
        	}
        	
        	if ( node == null ){
        		
        		if ( TRACE ) trace( "Token invalid/expired, store deined" );
        		
        		return;
        	}
        }
       
        DHTTransportContactI2P originator = new DHTTransportContactI2P( this, node );
        
        if ( method.equals("ping")){
        	
            receivePing( originator, msg_id );
      
        }else if ( method.equals("find_node")){
        	
            byte[] target = (byte[])args.get("target");
             
            receiveFindNode( originator, msg_id, target );
            
        }else if ( method.equals("get_peers")) {
        	
            byte[] hash = (byte[])args.get("info_hash");
           
            receiveFindValue( originator, msg_id, hash );
            
        }else if ( method.equals("announce_peer")) {
        	
            byte[] hash = (byte[])args.get("info_hash");
            
            originator.setRandomID2( token );
            
            // this is the "TCP" port, we don't care
            //int port = args.get("port").getInt();
             
            receiveStore( originator, msg_id, hash );   
        }
    }
    
    private void
    checkTimeouts()
    {
    	List<Request>	timed_out = null;
    	
    	synchronized( requests ){
    		
    		if ( requests.size() > 0 ){
    		
    			long	now = SystemTime.getMonotonousTime();
    			
    			Iterator<Request>	it = requests.values().iterator();
    			
    			while( it.hasNext()){
    				
    				Request	req = it.next();
    				
    				if ( now - req.getStartTime() > request_timeout ){
    					
    					if ( timed_out == null ){
    						
    						timed_out = new ArrayList<Request>();
    					}
    					
    					timed_out.add( req );
    					
    					it.remove();
    				}
    			}
    		}
    	}
    	
    	if ( timed_out != null ){
    		
    		stats.total_request_timeouts += timed_out.size();
    		
    		for ( Request r: timed_out ){
    			
    			try{
    				r.getHandler().handleError( new DHTTransportException( "Timeout" ));
    				
    			}catch( Throwable e ){
    				
    				Debug.out( e );
    			}
    		}
    	}
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
	
	protected void
	destroy()
	{
		synchronized( requests ){
			
			destroyed	= true;			

			for ( Request request: requests.values() ){
				
				try{
					request.handler.handleError( new DHTTransportException( "Transport destroyed" ));
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		timer_event.cancel();
	}
	
	private void
	trace(
		String	str )
	{
		System.out.println( str );
	}
	
	private interface
	ReplyHandler
	{
		public void
		handleReply(
			Map		reply );
		
		public void
		handleError(
			DHTTransportException	error );
	}
	
	private class
	Request
	{
		private ReplyHandler				handler;
    	
    	private long	start_time = SystemTime.getMonotonousTime();
    	
    	private
    	Request(
    		ReplyHandler				_handler )
    	{
    		handler		= _handler;
    	}
    	
    	private ReplyHandler
    	getHandler()
    	{
    		return( handler );
    	}
    	
    	private long
    	getStartTime()
    	{
    		return( start_time );
    	}
	}
	
	private class
	DHTTransportStatsI2P
		extends DHTTransportStatsImpl
	{
		private long	total_request_timeouts;
		private long	total_packets_sent;
		private long	total_packets_received;
		private long	total_bytes_sent;
		private long	total_bytes_received;

		private 
		DHTTransportStatsI2P()
		{
			super( PROTOCOL_VERSION );
		}
		
		public DHTTransportStats snapshot() {
			
			DHTTransportStatsI2P res = new DHTTransportStatsI2P();
			
			snapshotSupport( res );
			
			res.total_request_timeouts		= total_request_timeouts;
			res.total_packets_sent			= total_packets_sent;
			res.total_packets_received		= total_packets_received;
			res.total_bytes_sent			= total_bytes_sent;
			res.total_bytes_received		= total_bytes_received;
			
			return( res );
		}
		
		@Override
		public int getRouteablePercentage() {
			return( 100 );
		}
		
		@Override
		public long getRequestsTimedOut() {
			return( total_request_timeouts );
		}
		
		@Override
		public long getPacketsSent() {
			return( total_packets_sent );
		}
		
		@Override
		public long getPacketsReceived() {
			return( total_packets_received );
		}
		
		@Override
		public long getBytesSent() {
			return( total_bytes_sent );
		}
		
		@Override
		public long getBytesReceived() {
			return( total_bytes_received );
		}
	}
	
	private static class
	DHTTransportValueImpl
		implements DHTTransportValue
	{
		private DHTTransportContact		originator;
		private byte[]					value_bytes;
		
		private
		DHTTransportValueImpl(
			DHTTransportContact		_originator,
			byte[]					_value_bytes )
		{
			originator		= _originator;
			value_bytes		= _value_bytes;
		}
		
		public boolean
		isLocal()
		{
			return( false );
		}
		
		public long
		getCreationTime()
		{
			return( SystemTime.getCurrentTime());
		}
		
		public byte[]
		getValue()
		{
			return( value_bytes );
		}
		
		public int
		getVersion()
		{
			return( 1 );
		}
		
		public DHTTransportContact
		getOriginator()
		{
			return( originator );
		}
		
		public int
		getFlags()
		{
			return( 0 );
		}
		
		public int
		getLifeTimeHours()
		{
			return( 0 );	// default is repub interval
		}
		
		public byte
		getReplicationControl()
		{
			return( 0 );
		}
		
		public byte 
		getReplicationFactor() 
		{
			return( 0);
		}
		
		public byte 
		getReplicationFrequencyHours() 
		{
			return((byte)255 );
		}
		
		public String
		getString()
		{			
			return( DHTLog.getString( value_bytes ));
		}
	};
}
