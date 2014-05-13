/*
 * Created on May 8, 2014
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

import java.net.InetSocketAddress;
import java.util.*;

import net.i2p.data.Base32;

import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageManager;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZStylePeerExchange;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageFactory;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessageEncoder;
import com.aelitis.azureus.core.peermanager.peerdb.PeerExchangerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;


public class 
I2PHelperMessageHandler
	implements NetworkConnectionFactory.NetworkConnectionFactoryListener
{
	private I2PHelperPlugin		plugin;
		
	private LTI2PDHT	i2p_dht	= new LTI2PDHT();
	private LTI2PPEX	i2p_pex	= new LTI2PPEX();

	private LTMessage[]	lt_messages = { i2p_dht, i2p_pex };
	
	private Set<String>	lt_message_ids = new HashSet<String>();
	
	{
		for ( LTMessage message: lt_messages ){
			
			lt_message_ids.add( message.getID());
		}
	}

	private int		q_port;
	private	int		r_port;
	
	protected
	I2PHelperMessageHandler(
		I2PHelperPlugin		_plugin )
	{
		plugin	= _plugin;
		
		for ( LTMessage message: lt_messages ){
		
			registerMessage( message );
		}
		
		NetworkConnectionFactory.addListener( this );			
	}
	
	private void
	registerMessage(
		Message	message )
	{
		try{
			unregisterMessage( message );
			
			LTMessageDecoder.addDefaultExtensionHandler( message.getFeatureSubID(), message.getIDBytes());
			
			MessageManager.getSingleton().registerMessageType( message );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	private void
	unregisterMessage(
		Message	message )
	{
		Message existing = MessageManager.getSingleton().lookupMessage( message.getIDBytes());
		
		if ( existing != null ){
			
			MessageManager.getSingleton().deregisterMessageType( existing );
		}
		
		LTMessageDecoder.removeDefaultExtensionHandler( message.getFeatureSubID() );
	}
	
	public void
	connectionCreated(
		final NetworkConnection		connection )
	{
		InetSocketAddress address = connection.getEndpoint().getNotionalAddress();
		
		if ( address.isUnresolved()){
			
			final String peer_host = address.getHostName();
			
			if ( peer_host.endsWith( ".i2p" )){
				
				final OutgoingMessageQueue out_queue = connection.getOutgoingMessageQueue();

				connection.getOutgoingMessageQueue().registerQueueListener(
					new OutgoingMessageQueue.MessageQueueListener() 
					{						
						@Override
						public void protocolBytesSent(int byte_count) {
						}
						
						@Override
						public void messageSent(Message message) {
						}
						
						@Override
						public void messageRemoved(Message message) {
						}
						
						@Override
						public void messageQueued(Message message) {
						}
						
						@Override
						public boolean messageAdded(Message message) {
							
							if ( message instanceof LTHandshake ){
								
								LTHandshake lt_handshake = (LTHandshake)message;
								
								lt_handshake.addOptionalExtensionMapping( LTI2PDHT.MSG_ID, LTI2PDHT.SUBID_I2P_DHT );
								
								lt_handshake.addOptionalExtensionMapping( LTI2PPEX.MSG_ID, LTI2PPEX.SUBID_I2P_PEX );
							}
							
							return true;
						}
						
						@Override
						public void flush() {
						}
						
						@Override
						public void dataBytesSent(int byte_count) {
						}
					});
				
				connection.getIncomingMessageQueue().registerQueueListener(
					new IncomingMessageQueue.MessageQueueListener() {
						
						@Override
						public void protocolBytesReceived(int byte_count) {
						}
						
						@Override
						public boolean 
						messageReceived(
							Message message ) 
						{
							//System.out.println( "Received: " + connection + " - " + message );
							
							if ( message instanceof LTHandshake ){
								
								LTMessageEncoder encoder = (LTMessageEncoder)connection.getOutgoingMessageQueue().getEncoder();
								
								Map extensions = ((LTHandshake)message).getExtensionMapping();
								
								Iterator<Object> it = extensions.keySet().iterator();
								
								Map my_extensions = new HashMap();
								
								while( it.hasNext()){
									
									Object o = it.next();
									
									String key;
									
									if ( o instanceof String ){
										
										key = (String)o;
										
									}else if ( o instanceof byte[] ){
										
										try{
											key = new String((byte[])o, Constants.DEFAULT_ENCODING );
											
										}catch( Throwable e ){
											
											continue;
										}
										
									}else{
										
										continue;
									}
									
									if ( lt_message_ids.contains( key )){
										
										Object value = extensions.get( key );
										
										it.remove();
										
										my_extensions.put( key, value );
									}
								}
								
								if ( my_extensions.size() > 0 ){
								
									encoder.updateSupportedExtensions( my_extensions );
								}
								
								encoder.addCustomExtensionHandler( 
									LTMessageEncoder.CET_PEX,
									new LTMessageEncoder.CustomExtensionHandler() {
										
										public Object 
										handleExtension(
											Object[] args ) 
										{
											PeerExchangerItem	pxi = (PeerExchangerItem)args[0];
											
											PeerItem[] adds 	= pxi.getNewlyAddedPeerConnections();
											PeerItem[] drops 	= pxi.getNewlyDroppedPeerConnections();  
											
											if ( adds != null && adds.length > 0 ){
												
												List<PeerItem>	my_adds = new ArrayList<PeerItem>( adds.length );
												
												for ( PeerItem pi: adds ){
													
													if ( pi.getNetwork() == AENetworkClassifier.AT_I2P ){
														
														my_adds.add( pi );
													}
												}
												
												if ( my_adds.size() > 0 ){
												
													connection.getOutgoingMessageQueue().addMessage( 
															new LTI2PPEX( my_adds ),
															false );
												}
											}

											return( null );
										}
									});	
								
								if ( q_port == 0 ){
									
										// can take a bit of time for the DHT to become available - could queue
										// missed connections for later message send I guess?
									
									I2PHelperRouter router = plugin.getRouter();
									
									if ( router != null ){
										
										I2PHelperDHT dht = router.getDHT();
										
										if ( dht != null ){
											
											q_port 	= dht.getQueryPort();
											r_port	= dht.getReplyPort();
										}
									}
								}
								
								if ( q_port != 0 ){
								
									out_queue.addMessage( new LTI2PDHT( q_port, r_port ), false );
								}
							}else if ( message instanceof LTI2PDHT ){
									
								LTI2PDHT i2p_dht = (LTI2PDHT)message;
								
								plugin.handleDHTPort( peer_host, i2p_dht.getQueryPort());
								
								i2p_dht.destroy();
								
								return( true );
								
							}else if ( message instanceof LTI2PPEX ){
								
								return( false );	// handled by core as instanceof AZStylePeerExchange
							}
							
							return false;
						}
						
						@Override
						public boolean isPriority() {
							return false;
						}
						
						@Override
						public void dataBytesReceived(int byte_count) {							
						}
					});
			}			
		}
	}
	
	protected void
	destroy()
	{
		NetworkConnectionFactory.removeListener( this );		
		
		for ( LTMessage message: lt_messages ){
			
			unregisterMessage( message );
		}
	}
	
	protected static class
	LTI2PDHT
		implements LTMessage
	{
		public static String 	MSG_ID			= "i2p_dht";
		public static byte[] 	MSG_ID_BYTES 	= MSG_ID.getBytes();
		public static int 		SUBID_I2P_DHT	= 10;
		
		private DirectByteBuffer buffer = null;
		  
		private int		q_port;
		private int		r_port;
		
		public
		LTI2PDHT()
		{
		}
		
		public
		LTI2PDHT(
			int		_q_port,
			int		_r_port )
		{
			q_port		= _q_port;
			r_port		= _r_port;
		}
		
		public 
		LTI2PDHT(
			Map					map,
			DirectByteBuffer	data )
		{
			q_port = ((Long)map.get("port")).intValue();
			r_port = ((Long)map.get("rport")).intValue();
		}
		
		public String 
		getID()
		{
			return( MSG_ID );
		}

		public byte[] 
		getIDBytes()
		{
			return( MSG_ID_BYTES );
		}

		public String getFeatureID() {  return LTMessage.LT_FEATURE_ID;  }  
		public int getFeatureSubID() { return SUBID_I2P_DHT;  }
		public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
		public byte getVersion() { return BTMessageFactory.MESSAGE_VERSION_INITIAL; };


		public String 
		getDescription()
		{
			return( MSG_ID );
		}

		public int
		getQueryPort()
		{
			return( q_port );
		}
		
		public DirectByteBuffer[] 
		getData()
		{
			if ( buffer == null ){
				
				Map payload_map = new HashMap();

				payload_map.put( "port", new Long(q_port));
				payload_map.put( "rport", new Long(r_port));
								
				buffer = MessagingUtil.convertPayloadToBencodedByteStream(payload_map, DirectByteBuffer.AL_MSG );
			}

			return new DirectByteBuffer[]{ buffer };  
		}

		public Message 
		deserialize( 
			DirectByteBuffer 	data, 
			byte 				version ) 

			throws MessageException
		{
			int	pos = data.position( DirectByteBuffer.SS_MSG );
			
			byte[] dict_bytes = new byte[ Math.min( 128, data.remaining( DirectByteBuffer.SS_MSG )) ];
						
			data.get( DirectByteBuffer.SS_MSG, dict_bytes );
			
			try{
				Map root = BDecoder.decode( dict_bytes );

				data.position( DirectByteBuffer.SS_MSG, pos + BEncoder.encode( root ).length );			
									
				return( new LTI2PDHT( root, data ));
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				throw( new MessageException( "decode failed", e ));
			}
		}


		public void 
		destroy()
		{
			if ( buffer != null ){
				
				buffer.returnToPool();
			}
		}
	}
	
	protected static class
	LTI2PPEX
		implements AZStylePeerExchange, LTMessage
	{
		public static String 	MSG_ID			= "i2p_pex";
		public static byte[] 	MSG_ID_BYTES 	= MSG_ID.getBytes();
		public static int 		SUBID_I2P_PEX	= 11;
		
		private DirectByteBuffer buffer = null;
		  
		private	PeerItem[]		added		= new PeerItem[0];
		private	PeerItem[]		dropped		= new PeerItem[0];
		
		public
		LTI2PPEX()
		{
		}
		
		public
		LTI2PPEX(
			List<PeerItem>		_adds )
		{
			added = _adds.toArray( new PeerItem[ _adds.size()]);
		}
		
		public 
		LTI2PPEX(
			Map					map,
			DirectByteBuffer	data )
		{
			byte[] added_hashes = (byte[])map.get( "added" );
					
			if ( added_hashes != null ){
				
				added	= decodePeers( added_hashes );
			}
			
			byte[] dropped_hashes = (byte[])map.get( "dropped" );
			
			if ( dropped_hashes != null ){
				
				dropped	= decodePeers( dropped_hashes );
			}
		}
		
		private PeerItem[]
		decodePeers(
			byte[]	hashes )
		{
			int	hashes_len = hashes.length;
			
			if ( hashes_len % 32 != 0 ){
				
				return( new PeerItem[0]);
			}
			
			PeerItem[] result = new PeerItem[hashes_len/32];
			
			int	pos = 0;
			
			for ( int i=0;i<hashes_len;i+=32 ){
				
				byte[]	bytes = new byte[32];
				
				System.arraycopy( hashes, i, bytes, 0, 32 );
				
				String host = Base32.encode( bytes ) + ".b32.i2p";
								
				PeerItem peer = 
					PeerItemFactory.createPeerItem( 
						host, 
						6881, 
						PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE, 
						PeerItemFactory.HANDSHAKE_TYPE_PLAIN,
						0, 
						PeerItemFactory.CRYPTO_LEVEL_1,
						0 );
				
				result[pos++] = peer;
			}
			
			return( result );
		}
		
		public String 
		getID()
		{
			return( MSG_ID );
		}

		public byte[] 
		getIDBytes()
		{
			return( MSG_ID_BYTES );
		}

		public String getFeatureID() {  return LTMessage.LT_FEATURE_ID;  }  
		public int getFeatureSubID() { return SUBID_I2P_PEX;  }
		public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
		public byte getVersion() { return BTMessageFactory.MESSAGE_VERSION_INITIAL; };


		public String 
		getDescription()
		{
			return( MSG_ID );
		}
		
		public DirectByteBuffer[] 
		getData()
		{
			if ( buffer == null ){
				
				Map payload_map = new HashMap();

				if ( added.length > 0 ){
					
					byte[]	hashes = new byte[added.length*32];
					
					int	pos = 0;
					
					for ( int i=0;i<added.length;i++){
						
						String host = added[i].getAddressString();
						
						if ( host.endsWith( ".b32.i2p" )){
							
							byte[] h = Base32.decode( host.substring( 0, host.length() - 8 ));
							
							if ( h.length == 32 ){
								
								System.arraycopy( h, 0, hashes, pos, 32 );
								
								pos += 32;
							}
						}
					}
					
					if ( pos < hashes.length ){
						
						if ( pos > 0 ){
							
							byte[] temp = new byte[pos];
							
							System.arraycopy( hashes, 0, temp, 0, pos );
							
							hashes = temp;
						}
					}
					
					if ( pos > 0 ){
					
						payload_map.put( "added", hashes );
					}
				}
					
				//System.out.println( "Sending " + payload_map );
				
				buffer = MessagingUtil.convertPayloadToBencodedByteStream(payload_map, DirectByteBuffer.AL_MSG );
			}

			return new DirectByteBuffer[]{ buffer };  
		}

		public PeerItem[] 
		getAddedPeers()
		{
			return( added );
		}
		
		public PeerItem[] 
		getDroppedPeers()
		{
			return( dropped );
		}
		
		public int 
		getMaxAllowedPeersPerVolley(
			boolean initial, 
			boolean added )
		{
			return (initial && added) ? 500 : 250;
		}
		  
		public Message 
		deserialize( 
			DirectByteBuffer 	data, 
			byte 				version ) 

			throws MessageException
		{
			int	pos = data.position( DirectByteBuffer.SS_MSG );
			
			byte[] dict_bytes = new byte[ Math.min( 128, data.remaining( DirectByteBuffer.SS_MSG )) ];
						
			data.get( DirectByteBuffer.SS_MSG, dict_bytes );
			
			try{
				Map root = BDecoder.decode( dict_bytes );

				data.position( DirectByteBuffer.SS_MSG, pos + BEncoder.encode( root ).length );			
									
				return( new LTI2PPEX( root, data ));
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				throw( new MessageException( "decode failed", e ));
			}
		}


		public void 
		destroy()
		{
			if ( buffer != null ){
				
				buffer.returnToPool();
			}
		}
	}
}
