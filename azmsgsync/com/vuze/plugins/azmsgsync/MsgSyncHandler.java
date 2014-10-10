/*
 * Created on Oct 6, 2014
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


package com.vuze.plugins.azmsgsync;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.Signature;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationAdapter;
import com.aelitis.azureus.plugins.dht.DHTPluginProgressListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;

public class 
MsgSyncHandler 
	implements DHTPluginTransferHandler
{
	private static final String	HANDLER_BASE_KEY = "com.vuze.plugins.azmsgsync.MsgSyncHandler";
	private static final byte[]	HANDLER_BASE_KEY_BYTES;
	
	static{
		byte[]	 bytes = null;
		
		try{
			bytes = new SHA1Simple().calculateHash( HANDLER_BASE_KEY.getBytes( "UTF-8" ));
			
		}catch( Throwable e ){
			
		}
		
		HANDLER_BASE_KEY_BYTES = bytes;
	}
	
	private static final int STATUS_OK			= 1;
	private static final int STATUS_LOOPBACK	= 2;
	
	
	private final MsgSyncPlugin						plugin;
	private final DHTPluginInterface				dht;
	private final byte[]							user_key;
	private final byte[]							dht_key;
	
	private KeyPair				ecc_keys;
	
	private byte[]				my_uid;
	private MsgSyncNode			my_node;
	
	private ByteArrayHashMap<List<MsgSyncNode>>		node_uid_map 	= new ByteArrayHashMap<List<MsgSyncNode>>();
	
		// this is linked to the bloom filter size which in turn is limited by the max transfer
		// key size of 255 bytes....
	
	private static final int				MAX_BLOOM_BITS	= 200*8;
	private static final int				MIN_BLOOM_BITS	= 64*8;
	
	private static final int				MAX_MESSAGES	= 64;
	
	private LinkedList<MsgSyncMessage>		messages = new LinkedList<MsgSyncMessage>();
	
	protected
	MsgSyncHandler(
		MsgSyncPlugin			_plugin,
		DHTPluginInterface		_dht,
		byte[]					_key )
		
		throws Exception
	{
		plugin			= _plugin;
		dht				= _dht;
		user_key		= _key;
			
		my_uid = new byte[8];
		
		RandomUtils.nextSecureBytes( my_uid );
		
		ecc_keys = CryptoECCUtils.createKeys();

		my_node	= new MsgSyncNode( dht.getLocalAddress(), my_uid, CryptoECCUtils.keyToRawdata( ecc_keys.getPublic()));
		

		dht_key = new SHA1Simple().calculateHash( user_key );
		
		for ( int i=0;i<dht_key.length;i++){
			
			dht_key[i] ^= HANDLER_BASE_KEY_BYTES[i];
		}
		
		dht.registerHandler( dht_key, this );

		log( "Checking for existing nodes" );
		
		// TODO periodic bootstrap
		
		dht.get(
			dht_key,
			"Message Sync lookup: " + getString(),
			DHTPluginInterface.FLAG_SINGLE_VALUE,
			32,
			60*1000,
			false,
			true,
			new DHTPluginOperationAdapter() 
			{
				private boolean diversified;
				
				public boolean 
				diversified() 
				{
					diversified = true;
					
					return( true );
				}
				
				@Override
				public void 
				valueRead(
					DHTPluginContact 	originator, 
					DHTPluginValue 		value ) 
				{
					try{
					
						Map m = BDecoder.decode( value.getValue());
					
						addContact( originator, m );
						
					}catch( Throwable e ){
						
					}
				}
				
				@Override
				public void 
				complete(
					byte[] 		key, 
					boolean 	timeout_occurred) 
				{			
					if ( diversified ){
						
						log( "Not registering as sufficient nodes located" );
						
					}else{
						
						log( "Registering node" );
						
						Map	map = new HashMap();
						
						map.put( "u", my_uid );
						
						try{
							byte[] blah_bytes = BEncoder.encode( map );
							
							dht.put(
									dht_key,
									"Message Sync write: " + getString(),
									blah_bytes,
									DHTPluginInterface.FLAG_SINGLE_VALUE,
									new DHTPluginOperationAdapter() {
																			
										@Override
										public boolean 
										diversified() 
										{
											return( false );
										}
										
										@Override
										public void 
										complete(
											byte[] 		key, 
											boolean 	timeout_occurred ) 
										{
											log( "Node registered" );
										}
									});
							
						}catch( Throwable e ){
							
							Debug.out( e);
						}
					}
				}
			});	
	}
	
	public String
	getName()
	{
		return( "Message Sync: " + getString());
	}

	protected DHTPluginInterface
	getDHT()
	{
		return( dht );
	}
	
	protected byte[]
	getUserKey()
	{
		return( user_key );
	}

	
	private void
	addContact(
		DHTPluginContact		contact,
		Map						map )
	{				
		byte[] uid = (byte[])map.get( "u" );
				
			// we have no verification as to validity of the contact/uid at this point - it'll get checked later
			// if/when we obtain its public key
		
		if ( addNode( contact, uid, null ) != null ){
		
			sync();
		}
	}
	
	private MsgSyncNode
	addNode(
		DHTPluginContact		contact,
		byte[]					uid,
		byte[]					public_key )
	{
		if ( uid == null || uid == my_uid ){
			
			return( null );
		}
		
		synchronized( node_uid_map ){
			
			List<MsgSyncNode> nodes = node_uid_map.get( uid );
						
			if ( nodes != null ){
				
				for ( MsgSyncNode n: nodes ){
					
					if ( sameContact( n.getContact(), contact )){
						
						return( n );
					}
				}
			}
							
			if ( nodes == null ){
				
				nodes = new ArrayList<MsgSyncNode>();
				
				node_uid_map.put( uid, nodes );
			}
			
			MsgSyncNode node = new MsgSyncNode( contact, uid, public_key );
				
			System.out.println( "Add node: " + contact.getName() + ByteFormatter.encodeString( uid ) + "/" + (public_key==null?"no PK":"with PK" ));

			nodes.add( node );		
			
			return( node );
		}	
	}
	
	private void
	removeNode(
		MsgSyncNode		node )
	{
		synchronized( node_uid_map ){
			
			byte[]	node_id = node.getUID();
			
			List<MsgSyncNode> nodes = node_uid_map.get( node_id );
			
			if ( nodes != null ){
				
				if ( nodes.remove( node )){
					
					if ( nodes.size() == 0 ){
						
						node_uid_map.remove( node_id );
					}
					
					System.out.println( "Remove node: " + node.getContact().getName() + ByteFormatter.encodeString( node_id ));
				}
			}
		}
	}
	
	private List<MsgSyncNode>
	getNodes(
		byte[]		node_id )
	{
		synchronized( node_uid_map ){

			List<MsgSyncNode> nodes = node_uid_map.get( node_id );
			
			if ( nodes != null ){
				
				nodes = new ArrayList<MsgSyncNode>( nodes );
			}
			
			return( nodes );
		}
	}
	
	private boolean
	sameContact(
		DHTPluginContact		c1,
		DHTPluginContact		c2 )
	{
		InetSocketAddress a1 = c1.getAddress();
		InetSocketAddress a2 = c2.getAddress();
		
		if ( a1.getPort() == a2.getPort()){
			
			if ( a1.isUnresolved() && a2.isUnresolved()){
				
				return( a1.getHostName().equals( a2.getHostName()));
				
			}else if ( a1.isUnresolved() || a2.isUnresolved()){
				
				return( false );
				
			}else{
				
				return( a1.getAddress().equals( a2.getAddress()));
			}
		}else{
			
			return( false );
		}
	}
	
	private void
	addMessage(
		MsgSyncNode		node,
		byte[]			message_id,
		byte[]			content,
		byte[]			signature )
	{
		MsgSyncMessage msg = new MsgSyncMessage( node, message_id, content, signature );
		
		synchronized( messages ){
		
			messages.add( msg );
			
			if ( messages.size() > MAX_MESSAGES ){
				
				messages.removeFirst();
			}
		}
	}
	
	public void
	sendMessage(
		byte[]		content )
	{
		try{
			Signature sig = CryptoECCUtils.getSignature( ecc_keys.getPrivate());
			
			byte[]	message_id = new byte[8];
			
			RandomUtils.nextSecureBytes( message_id );
			
			sig.update( my_uid );
			sig.update( message_id );
			sig.update( content );
			
			byte[]	sig_bytes = sig.sign();
			
			addMessage( my_node, message_id, content, sig_bytes );
			
			sync();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	sync()
	{
		sync( false );
	}
	
	protected void
	sync(
		boolean		prefer_live )
	{
		MsgSyncNode node;
		
		synchronized( node_uid_map ){
			
			if ( node_uid_map.size() > 0 ){
				
				Iterator<List<MsgSyncNode>>	it = node_uid_map.values().iterator();
				
				node = it.next().get(0);
				
			}else{
				
				node = null;
			}
		}
		
		if ( node != null ){
			
			sync(node );
		}
	}
	
	private void
	sync(
		MsgSyncNode		sync_node )
	{
		byte[]		rand 	= new byte[8];
		BloomFilter	bloom	= null;
		
		synchronized( messages ){

			for ( int i=0;i<64;i++){
				
				RandomUtils.nextSecureBytes( rand );
				
					// slight chance of duplicate keys (maybe two nodes share a public key due to a restart)
					// so just use distinct ones
				
				ByteArrayHashMap<String>	bloom_keys = new ByteArrayHashMap<String>();
				
				Set<MsgSyncNode>	done_nodes = new HashSet<MsgSyncNode>();
				
				for ( MsgSyncMessage msg: messages ){
					
					MsgSyncNode n = msg.getNode();
					
					if ( !done_nodes.contains( n )){
						
						done_nodes.add( n );
						
						byte[] pub = n.getPublicKey();
						
						if ( pub != null ){
						
							pub = pub.clone();
							
							for ( int j=0;j<rand.length;j++){
								
								pub[j] ^= rand[j];
							}
							
							bloom_keys.put( pub, "" );
						}
					}
					
					byte[]	sig = msg.getSignature().clone();
		
					for ( int j=0;j<rand.length;j++){
						
						sig[j] ^= rand[j];
					}
					
					bloom_keys.put( sig, ""  );
				}
				
					// in theory we could have 64 sigs + 64 pks -> 128 -> 1280 bits -> 160 bytes + overhead
				
				int	bloom_bits = bloom_keys.size() * 10;
					
				if ( bloom_bits < MIN_BLOOM_BITS ){
				
					bloom_bits = MIN_BLOOM_BITS;
				}
				
				bloom = BloomFilterFactory.createAddOnly( bloom_bits );
				
				for ( byte[] k: bloom_keys.keys()){
					
					if ( bloom.contains( k )){
						
						bloom = null;
						
						break;
						
					}else{
						
						bloom.add( k );
					}
				}
			}
		}
		
		if ( bloom == null ){
			
				// clashed too many times, whatever, we'll try again soon
			
			return;
		}
		
		Map sync_map = new HashMap();
		
		sync_map.put( "u", my_uid );
		sync_map.put( "b", bloom.serialiseToMap());
		sync_map.put( "r", rand );
		
		try{
			byte[]	sync_key = BEncoder.encode( sync_map);
			
			byte[] reply_bytes = 
				sync_node.getContact().call(
					new DHTPluginProgressListener() {
						
						@Override
						public void reportSize(long size) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void reportCompleteness(int percent) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void reportActivity(String str) {
							// TODO Auto-generated method stub
							
						}
					},
					dht_key,
					sync_key, 
					30*1000 );
		
			if ( reply_bytes != null ){

				Map reply_map = BDecoder.decode( reply_bytes );

				System.out.println( "reply: " + reply_map + " from " + sync_node.getName());
				
				int status = ((Number)reply_map.get( "s" )).intValue();
				
				if ( status == STATUS_LOOPBACK ){
					
					removeNode( sync_node );
					
				}else{
					List<Map<String,Object>>	list = (List<Map<String,Object>>)reply_map.get( "m" );
					
					if ( list != null ){
						
						for ( Map<String,Object> m: list ){
							
							try{
								byte[] node_uid		= (byte[])m.get( "u" );
								byte[] message_id 	= (byte[])m.get( "i" );
								byte[] content		= (byte[])m.get( "c" );
								byte[] signature	= (byte[])m.get( "s" );
								
									// these won't be present if remote believes we already have it (subject to occasional bloom false positives)
								
								byte[] 	public_key		= (byte[])m.get( "p" );
								
								Map<String,Object>		contact_map		= (Map<String,Object>)m.get( "k" );
								
								System.out.println( "Got " + ByteFormatter.encodeString( message_id ) + ": " + new String( content ));
																
								boolean handled = false;
								
									// see if we already have a node with the correct public key
								
								List<MsgSyncNode> nodes = getNodes( node_uid );
								
								if ( nodes != null ){
									
									for ( MsgSyncNode node: nodes ){
										
										byte[] pk = node.getPublicKey();
										
										if ( pk != null ){
											
											Signature sig = CryptoECCUtils.getSignature( CryptoECCUtils.rawdataToPubkey( pk ));
											
											sig.update( node_uid );
											sig.update( message_id );
											sig.update( content );
											
											if ( sig.verify( signature )){
												
												addMessage( node, message_id, content, signature );
												
												handled = true;
											}
										}
									}
								}
									
								if ( !handled ){
									
									if ( public_key != null ){
										
										Signature sig = CryptoECCUtils.getSignature( CryptoECCUtils.rawdataToPubkey( public_key ));
										
										sig.update( node_uid );
										sig.update( message_id );
										sig.update( content );
										
										if ( sig.verify( signature )){
											
											DHTPluginContact contact = dht.importContact( contact_map );
											
											MsgSyncNode msg_node = null;
											
												// look for existing node without public key that we can use
											
											if ( nodes != null ){
												
												for ( MsgSyncNode node: nodes ){
													
													if ( node.getPublicKey() == null ){
														
														node.setDetails( contact, public_key );
														
														msg_node = node;
													}
												}
											}
											if ( msg_node == null ){
											
												msg_node = addNode( contact, node_uid, public_key );
											}
											
											if ( msg_node != null ){
												
												addMessage( msg_node, message_id, content, signature );
											}
										}
									}
								}
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}	

	public byte[]
	handleRead(
		DHTPluginContact	originator,
		byte[]				key )
	{
		try{
			Map<String,Object> request_map = BDecoder.decode( key );

			System.out.println( "handle read: " + request_map );

			Map<String,Object> reply_map = new HashMap<String,Object>();

			int		status;

			byte[]	uid = (byte[])request_map.get( "u" );

			if ( Arrays.equals( my_uid, uid )){
				
				status = STATUS_LOOPBACK;
				
			}else{
				
				status = STATUS_OK;
				
				BloomFilter bloom = BloomFilterFactory.deserialiseFromMap((Map<String,Object>)request_map.get("b"));
				
				byte[]	rand = (byte[])request_map.get( "r" );
				
				List<MsgSyncMessage>	missing = new ArrayList<MsgSyncMessage>();
				
				int	num_they_have_i_dont = bloom.getEntryCount();
				
				synchronized( messages ){
					
					for ( MsgSyncMessage msg: messages ){
						
						byte[]	sig = msg.getSignature().clone();	// clone as we mod it
			
						for ( int i=0;i<rand.length;i++){
							
							sig[i] ^= rand[i];
						}
						
						if ( !bloom.contains( sig )){
						
								// I have it, they don't
							
							missing.add( msg );
							
						}else{
							
							num_they_have_i_dont--;
						}
					}
				}
								
				if ( missing.size() > 0 ){
					
					Set<MsgSyncNode>	done_nodes = new HashSet<MsgSyncNode>();
					
					List<Map<String,Object>> l = new ArrayList<Map<String,Object>>();
					
					reply_map.put( "m", l );
					
					for ( MsgSyncMessage message: missing ){
						
						Map<String,Object> m = new HashMap<String,Object>();
						
						l.add( m );
						
						MsgSyncNode	n = message.getNode();
						
						m.put( "u", n.getUID());
						m.put( "i", message.getID());
						m.put( "c", message.getContent());
						m.put( "s", message.getSignature());
						
						if ( !done_nodes.contains( n )){
							
							done_nodes.add( n );
							
							byte[]	pub = n.getPublicKey();	
							
							if ( pub != null ){
							
								pub = pub.clone();
								
								for ( int i=0;i<rand.length;i++){
									
									pub[i] ^= rand[i];
								}
								
								if ( !bloom.contains( pub )){
									
									m.put( "p", n.getPublicKey());
									
									DHTPluginContact contact = n.getContact();
									
									m.put( "k", contact.exportToMap());
								}
							}
						}
					}
				}
				
				if ( num_they_have_i_dont > 0 ){
					
					// TODO: prioritise us hitting them to get this based on num missing prolly
					
				}
			}
			
			reply_map.put( "s", status );
			
			return( BEncoder.encode( reply_map ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		return( null );
	}
	
	public void
	handleWrite(
		DHTPluginContact	originator,
		byte[]				key,
		byte[]				value )
	{
		Debug.out( "eh?" );
	}
	
	private void
	log(
		String	str )
	{
		plugin.log( str );
	}
	

	
	protected String
	getString()
	{
		return( dht + "/" + ByteFormatter.encodeString( dht_key ) + "/" + new String( user_key ));
	}
}
