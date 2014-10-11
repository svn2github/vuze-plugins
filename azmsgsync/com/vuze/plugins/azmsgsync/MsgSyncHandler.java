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
import org.gudy.azureus2.core3.util.SystemTime;

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
	
	private int NODE_STATUS_CHECK_PERIOD			= 60*1000;
	private int	NODE_STATUS_CHECK_TICKS				= NODE_STATUS_CHECK_PERIOD / MsgSyncPlugin.TIMER_PERIOD;
	
	
	private static final int STATUS_OK			= 1;
	private static final int STATUS_LOOPBACK	= 2;
	
	
	private final MsgSyncPlugin						plugin;
	private final DHTPluginInterface				dht;
	private final byte[]							user_key;
	private final byte[]							dht_key;
	private boolean									checking_dht;
	private long									last_dht_check;

	private KeyPair				ecc_keys;
	
	private byte[]				my_uid;
	private MsgSyncNode			my_node;
	
	private ByteArrayHashMap<List<MsgSyncNode>>		node_uid_map 		= new ByteArrayHashMap<List<MsgSyncNode>>();
	private ByteArrayHashMap<MsgSyncNode>			node_uid_loopbacks	= new ByteArrayHashMap<MsgSyncNode>();

	private static final int				MIN_BLOOM_BITS	= 64*8;
	
	private static final int				MAX_OTHER_MESSAGES	= 64;
	private static final int				MAX_MY_MESSAGES		= 16;
	
	private static final int				MAX_NODES			= 128;

	
		// TODO: figure this out - once messages start being removed we need to limit the
		// time that we keep our own messages around so that recipients don't get replays
		// remove messages only need to be remembered by their sigs
	
	private static final int				MY_MESSAGE_RETENTION		= 2*60*1000;
	private static final int				REMOVED_MESSAGE_RETENTION	= 4*60*1000;
	
	private Object							message_lock	= new Object();
	
	private LinkedList<MsgSyncMessage>		my_messages 	= new LinkedList<MsgSyncMessage>();
	private LinkedList<MsgSyncMessage>		other_messages 	= new LinkedList<MsgSyncMessage>();
	
	private ByteArrayHashMap<String>		message_sigs	= new ByteArrayHashMap<String>();
	
	private List<List<MsgSyncMessage>>		all_messages = new ArrayList<List<MsgSyncMessage>>();
	
	{
		all_messages.add( my_messages );
		all_messages.add( other_messages );
	}
			
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
		
		checkDHT( true );
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
	checkDHT(
		final boolean	first_time )
	{
		synchronized( this ){
			
			if ( checking_dht ){
				
				return;
			}
			
			checking_dht = true;
			
			last_dht_check	= SystemTime.getMonotonousTime();
		}
		
		log( "Checking DHT for nodes" );
		
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
					
						Map<String,Object> m = BDecoder.decode( value.getValue());
					
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
					try{
						if ( first_time ){
							
							if ( diversified ){
								
								log( "Not registering as sufficient nodes located" );
								
							}else{
								
								log( "Registering node" );
								
								Map<String,Object>	map = new HashMap<String,Object>();
								
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
					}finally{
						
						synchronized( MsgSyncHandler.this ){
							
							checking_dht = false;
						}
					}
				}
			});		
	}
	
	protected void
	timerTick(
		int		count )
	{
		if ( count % NODE_STATUS_CHECK_TICKS == 0 ){
			
			int	failed	= 0;
			int	live	= 0;
			int	total	= 0;
			
			List<MsgSyncNode>	to_remove = new ArrayList<MsgSyncNode>();
			
			synchronized( node_uid_map ){
				
				List<MsgSyncNode>	living		 	= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
				List<MsgSyncNode>	not_failing	 	= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
				List<MsgSyncNode>	failing 		= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
				
				Map<String,List<MsgSyncNode>>	address_map = new HashMap<String, List<MsgSyncNode>>();
				
				System.out.println( "Current nodes: ");
				
				for ( List<MsgSyncNode> nodes: node_uid_map.values()){
					
					for ( MsgSyncNode node: nodes ){
						
						System.out.println( "    " + node.getContact().getAddress() + "/" + ByteFormatter.encodeString( node.getUID()));
						
						total++;
						
						String c_key = getString( node.getContact());
						
						List<MsgSyncNode> x = address_map.get( c_key );
						
						if ( x == null ){
							
							x = new ArrayList<MsgSyncNode>();
							
							address_map.put( c_key, x );
						}
						
						if ( node.getFailCount() > 0 ){
							
							failed++;
							
							if ( node.getFailCount() > 1 ){
								
								to_remove.add( node );
								
							}else{
								
								failing.add( node );
							}
						}else{
							
							if ( node.getLastAlive() > 0 ){
												
								live++;
								
								living.add( node );
								
							}else{
								
								not_failing.add( node );
							}
						}
					}
				}
				
				int	excess = total - to_remove.size() - MAX_NODES;
				
				if ( excess > 0 ){
					
					List<List<MsgSyncNode>>	lists = new ArrayList<List<MsgSyncNode>>();
					
					Collections.shuffle( living );
					
					lists.add( failing );
					lists.add( not_failing );
					lists.add( living );
					
					for ( List<MsgSyncNode> list: lists ){
						
						if ( excess == 0 ){
							
							break;
						}
						
						for ( MsgSyncNode node: list ){
							
							to_remove.add( node );
							
							excess--;
							
							if ( excess == 0 ){
								
								break;
							}
						}
					}
				}
			}
			
			log( "Node status: live=" + live + ", failed=" + failed + ", total=" + total + ", to_remove=" + to_remove.size());
			
			for ( MsgSyncNode node: to_remove ){
				
				removeNode( node, false );
			}
			
			long	now = SystemTime.getMonotonousTime();
			
			if ( 	live == 0 ||
					now - last_dht_check > 30*60*1000 ||
					now - last_dht_check > live*60*1000 ){
				
				checkDHT( false );
			}
		}
		
		sync();
	}

	private void
	addContact(
		DHTPluginContact		contact,
		Map<String,Object>		map )
	{				
		byte[] uid = (byte[])map.get( "u" );
				
		if ( uid != null ){
			
				// we have no verification as to validity of the contact/uid at this point - it'll get checked later
				// if/when we obtain its public key
			
			if ( addNode( contact, uid, null ) != my_node ){
			
				sync();
			}
		}
	}
	
	private MsgSyncNode
	addNode(
		DHTPluginContact		contact,
		byte[]					uid,
		byte[]					public_key )
	{
			// we need to always return a node as it is required to create associated messages and we have to create each message otherwise
			// we'll keep on getting it from other nodes
		
		if ( uid == my_uid ){
			
			return( my_node );
		}
		
		synchronized( node_uid_map ){
			
			MsgSyncNode loop = node_uid_loopbacks.get( uid );
				
			if ( loop != null ){
				
				return( loop );
			}
			
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
		MsgSyncNode		node,
		boolean			is_loopback )
	{
		synchronized( node_uid_map ){
			
			byte[]	node_id = node.getUID();
			
			if ( is_loopback ){
				
				node_uid_loopbacks.put( node_id, node );
			}
			
			List<MsgSyncNode> nodes = node_uid_map.get( node_id );
			
			if ( nodes != null ){
				
				if ( nodes.remove( node )){
					
					if ( nodes.size() == 0 ){
						
						node_uid_map.remove( node_id );
					}
					
					System.out.println( "Remove node: " + node.getContact().getName() + ByteFormatter.encodeString( node_id ) + ", loop=" + is_loopback );
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
	
	private String
	getString(
		DHTPluginContact		c )
	{
		InetSocketAddress a = c.getAddress();
		
		if ( a.isUnresolved()){
			
			return( a.getHostName() + ":" + a.getPort());
			
		}else{
			
			return( a.getAddress().getHostAddress() + ":" + a.getPort());
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
		
			// TODO: keep my messages separate so that we retain last X regardless (or at least with a long timeout)
			// this prevents high message throughput from losing them
		
		synchronized( message_lock ){
		
			if ( message_sigs.containsKey( signature )){
				
				return;
			}
			
			message_sigs.put( signature, "" );
			
			if ( node == my_node ){
				
				my_messages.add( msg );
			
				if ( my_messages.size() > MAX_MY_MESSAGES ){
					
					MsgSyncMessage removed = my_messages.removeFirst();
					
					message_sigs.remove( removed.getSignature());
				}
			}else{
				
				other_messages.add( msg );
				
				if ( other_messages.size() > MAX_OTHER_MESSAGES ){
					
					MsgSyncMessage removed = other_messages.removeFirst();
					
					message_sigs.remove( removed.getSignature());
				}
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
	
	private static final int MAX_CONC_SYNC	= 5;
	private static final int MAX_FAIL_SYNC	= 2;
	
	private Set<MsgSyncNode> active_syncs	 = new HashSet<MsgSyncNode>();
	
	private boolean	prefer_live_sync_outstanding;
	
	protected void
	sync(
		final boolean		prefer_live )
	{
		MsgSyncNode	sync_node = null;
		
		synchronized( node_uid_map ){
			
			if ( prefer_live ){
				
				prefer_live_sync_outstanding = true;
			}
			
			if ( active_syncs.size() > MAX_CONC_SYNC ){
				
				return;
			}
			
			Set<String>	active_addresses = new HashSet<String>();
			
			for ( MsgSyncNode n: active_syncs ){
				
				active_addresses.add( getString( n.getContact()));
			}
							
			List<MsgSyncNode>	not_failed 	= new ArrayList<MsgSyncNode>();
			List<MsgSyncNode>	failed 		= new ArrayList<MsgSyncNode>();
			List<MsgSyncNode>	live 		= new ArrayList<MsgSyncNode>();
			
			for ( List<MsgSyncNode> nodes: node_uid_map.values()){
				
				for ( MsgSyncNode node: nodes ){
					
					if ( active_syncs.size() > 0 ){
					
						if ( active_syncs.contains( node ) || active_addresses.contains( getString( node.getContact()))){
						
							continue;
						}
					}
										
					if ( node.getFailCount() == 0 ){
						
						not_failed.add( node );
						
						if ( node.getLastAlive() > 0 ){
							
							live.add( node );
						}
					}else{
						
						failed.add( node );
					}
				}
			}
			
			if ( prefer_live_sync_outstanding && live.size() > 0 ){
				
				prefer_live_sync_outstanding = false;
				
				sync_node = live.get( RandomUtils.nextInt( live.size()));
				
			}else{
				
				int	active_fails = 0;
				
				for ( MsgSyncNode node: active_syncs ){
					
					if ( node.getFailCount() > 0 ){
						
						active_fails++;
					}
				}
				
				if ( active_fails >= MAX_FAIL_SYNC && not_failed.size() > 0 ){
					
					sync_node = not_failed.get( RandomUtils.nextInt( not_failed.size()));
				}
				
				if ( sync_node == null ){
					
					int	num_not_failed = not_failed.size();
					
					int rem_size = num_not_failed + failed.size();
						
					if ( rem_size > 0 ){
						
						int	pos =  RandomUtils.nextInt( rem_size );
						
						if ( pos < num_not_failed ){
							
							sync_node = not_failed.get( pos );
							
						}else{
							
							sync_node = failed.get( pos - num_not_failed );
						}
					}
					
				}
			}
			
			if ( sync_node == null ){
				
				return;
			}
			
			active_syncs.add( sync_node );
		}
						
		final MsgSyncNode	f_sync_node = sync_node;
		
		new AEThread2( "MsgSyncHandler:sync"){
			
			@Override
			public void run() {
				try{
					
					sync( f_sync_node );
					
				}finally{
						
					synchronized( node_uid_map ){
						
						active_syncs.remove( f_sync_node );
					}
				}
			}
		}.start();
	}
	
	private void
	sync(
		MsgSyncNode		sync_node )
	{
		byte[]		rand 	= new byte[8];
		BloomFilter	bloom	= null;
		
		synchronized( message_lock ){

			for ( int i=0;i<64;i++){
				
				RandomUtils.nextSecureBytes( rand );
				
					// slight chance of duplicate keys (maybe two nodes share a public key due to a restart)
					// so just use distinct ones
				
				ByteArrayHashMap<String>	bloom_keys = new ByteArrayHashMap<String>();
				
				Set<MsgSyncNode>	done_nodes = new HashSet<MsgSyncNode>();
				
				for ( List<MsgSyncMessage> msgs: all_messages ){
					
					for ( MsgSyncMessage msg: msgs ){
						
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
				}
				
					// in theory we could have 64 sigs + 64 pks -> 128 -> 1280 bits -> 160 bytes + overhead
				
				int	bloom_bits = bloom_keys.size() * 20;
					
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
		
		Map<String,Object> request_map = new HashMap<String,Object>();
		
		request_map.put( "t", 0 );		// type
		
		request_map.put( "u", my_uid );
		request_map.put( "b", bloom.serialiseToMap());
		request_map.put( "r", rand );
				
		try{
			byte[]	sync_data = BEncoder.encode( request_map );
			
			byte[] reply_bytes = 
				sync_node.getContact().call(
					new DHTPluginProgressListener() {
						
						@Override
						public void reportSize(long size) {
						}
						
						@Override
						public void reportCompleteness(int percent) {
						}
						
						@Override
						public void reportActivity(String str) {
						}
					},
					dht_key,
					sync_data, 
					30*1000 );
		
			if ( reply_bytes == null ){

				throw( new Exception( "No reply" ));
			}
			
			Map<String,Object> reply_map = BDecoder.decode( reply_bytes );

			int	type = reply_map.containsKey( "t" )?((Number)reply_map.get( "t" )).intValue():-1; 

			if ( type != 1 ){
				
					// meh, issue with 'call' implementation when made to self - you end up getting the
					// original request data back as the result :( Can't currently see how to easily fix
					// this so handle it for the moment
				
				removeNode( sync_node, true );
				
			}else{
				
				System.out.println( "reply: " + reply_map + " from " + sync_node.getName());
				
				int status = ((Number)reply_map.get( "s" )).intValue();
				
				if ( status == STATUS_LOOPBACK ){
					
					removeNode( sync_node, true );
					
				}else{
					
					sync_node.ok();
					
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
																						
											addMessage( msg_node, message_id, content, signature );
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
			
			sync_node.failed();
		}
	}	

	public byte[]
	handleRead(
		DHTPluginContact	originator,
		byte[]				key )
	{
		try{
			Map<String,Object> request_map = BDecoder.decode( key );

			int	type = request_map.containsKey( "t" )?((Number)request_map.get( "t" )).intValue():-1; 

			if ( type != 0 ){
				
				return( null );
			}
			
			System.out.println( "request: " + request_map );

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
				
				synchronized( message_lock ){
					
					for ( List<MsgSyncMessage> msgs: all_messages ){

						for ( MsgSyncMessage msg: msgs ){
							
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
			
			reply_map.put( "t", 1 );		// type

			return( BEncoder.encode( reply_map ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		return( null );
	}
	
	public byte[]
	handleWrite(
		DHTPluginContact	originator,
		byte[]				call_key,
		byte[]				value )
	{
			// switched to using 'call' to allow larger bloom sizes - in this case we come in
			// here with a unique rcp key for the 'key and the value is the payload
		
		return( handleRead( originator, value ));
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
		return( dht.getNetwork() + "/" + ByteFormatter.encodeString( dht_key ) + "/" + new String( user_key ));
	}
}
