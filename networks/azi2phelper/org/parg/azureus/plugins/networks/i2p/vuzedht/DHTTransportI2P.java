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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.parg.azureus.plugins.networks.i2p.dht.NID;
import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;

import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.I2PSessionMuxedListener;
import net.i2p.client.SendMessageOptions;
import net.i2p.client.datagram.I2PDatagramDissector;
import net.i2p.client.datagram.I2PDatagramMaker;
import net.i2p.data.Base32;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportRequestHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportStatsImpl;
import com.aelitis.azureus.util.MapUtils;

public class 
DHTTransportI2P
	implements DHTTransport, I2PSessionMuxedListener
{
	private static final byte PROTOCOL_VERSION		= 1;
	private static final byte PROTOCOL_VERSION_MIN	= 1;
	
	private I2PSession					session;
	private NodeInfo					my_node;
	private int							query_port;
	private int							reply_port;
	private NID							my_nid;
	
	private DHTTransportStatsImpl		stats;
	
	private DHTTransportContactI2P		local_contact;
	
	private DHTTransportRequestHandler	request_handler;
	
	private TimerEventPeriodic 			timer_event;
	
	private long						request_timeout;
	
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
		
		return( new DHTTransportContactI2P( this, node ));
	}
	
	protected void
	removeContact(
		DHTTransportContactI2P	contact )
	{
		request_handler.contactRemoved( contact );
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
	
		// RPCs
	
	final int DEST_LOOKUP_TIMEOUT = 10*1000;

	private HashMap<HashWrapper,Request>		requests = new HashMap<HashWrapper, Request>();
	
	public void
	sendPing(
		final DHTTransportReplyHandler		handler,
		final DHTTransportContactI2P		contact )
	{
		try{
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
	        			System.out.println( "receivePing" );
	        			
	        			handler.pingReply( contact, -1 );
	        		}
	        		
	        		@Override
	        		public void 
	        		handleError(
	        			DHTTransportException error) 
	        		{
	        			handler.failed( contact, error );
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
    sendQuery(
    	ReplyHandler				handler,
    	DHTTransportContactI2P		contact,
    	Map<String, Object> 		map, 
    	boolean 					repliable ) 
    	
    	throws Exception
    {
    	if ( session.isClosed()){
    		
           	throw( new DHTTransportException( "Session closed" ));
    	}
    	
    	NodeInfo node = contact.getNode();
    	
    	Destination	dest = node.getDestination();
    	
    	if ( dest == null ) {

            dest = session.lookupDest( node.getHash(), DEST_LOOKUP_TIMEOUT );
            
            if (dest != null ){
            
                node.setDestination(dest);
                
            }else{
            	
            	throw( new DHTTransportException( "Destination lookup failed" ));
            }
    	}

	    map.put( "y", "q" );
	
	    	// i2p uses 8 byte random message ids and supports receiving up to 16 byte ids
	
	    byte [] msg_id = new byte[8];
	
	    RandomUtils.nextBytes( msg_id );
	
	    map.put("t", msg_id );
	
	    Map<String, Object> args = (Map<String, Object>) map.get("a");
	
	    args.put("id", my_nid.getData());
	    
	    int port = node.getPort();
	    
	    if ( !repliable ){
	    	
	    	port++;
	    }
	    
	    if ( repliable ){
	    	
	    	synchronized( requests ){
	    		
	    		requests.put( new HashWrapper( msg_id ), new Request( handler ));
	    	}
	    }
	    
	    boolean	ok = false;
	    
	    try{
	    	sendMessage( dest, port, map, repliable );
	    	
	    	ok	= true;
	    	
	    }finally{
	    	
	    	if ( repliable && !ok ){
	    	
	    		synchronized( requests ){
	    		
	    			requests.remove( new HashWrapper( msg_id ));
	    		}
	    	}
	    }
	}
    
    private static final int SEND_CRYPTO_TAGS 	= 8;
    private static final int LOW_CRYPTO_TAGS 	= 4;

    private void 
    sendMessage(
    	Destination 			dest, 
    	int 					toPort, 
    	Map<String, Object> 	map, 
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
    	System.out.println( "Received incoming message!" );

    	try{
	    	byte[] payload = session.receiveMessage(msg_id);
	        
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
    	try{
	    	Map		map = BDecoder.decode( payload );
	
	        byte[] msg_id = (byte[])map.get( "t" );
	      
	        String type = MapUtils.getMapString( map, "y", "" );
	        
	        if ( type.equals("q")){
	        	
	            	// queries must be repliable
	        	
	            String method = MapUtils.getMapString( map, "q", "" );
	            
	            Map args = (Map)map.get( "a" );
	            
	            //receiveQuery( msg_id, from_dest, from_port, method, args );
	            
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
		destroyed	= true;
		
		timer_event.cancel();
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
