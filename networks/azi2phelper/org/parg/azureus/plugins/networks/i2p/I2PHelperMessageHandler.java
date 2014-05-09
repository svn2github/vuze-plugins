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
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageFactory;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessageEncoder;


public class 
I2PHelperMessageHandler
	implements NetworkConnectionFactory.NetworkConnectionFactoryListener
{
	private I2PHelperPlugin		plugin;
		
	private LTI2PDHT	i2p_dht	= new LTI2PDHT();

	private LTMessage[]	lt_messages = { i2p_dht };
	
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
								
								((LTHandshake)message).addOptionalExtensionMapping( LTI2PDHT.MSG_ID, LTI2PDHT.SUBID_I2P_DHT );
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
									
								LTI2PDHT ip_dht = (LTI2PDHT)message;
								
								plugin.handleDHTPort( peer_host, ip_dht.getQueryPort());
								
								return( true );
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
}
