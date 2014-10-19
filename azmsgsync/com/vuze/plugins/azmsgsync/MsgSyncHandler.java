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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.util.average.AverageFactory;
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
	private static final int VERSION	= 1;
	
	private static final boolean TRACE = System.getProperty( "az.msgsync.trace.enable", "0" ).equals( "1" );
	
	private static final String	HANDLER_BASE_KEY = "com.vuze.plugins.azmsgsync.MsgSyncHandler";
	private static final byte[]	HANDLER_BASE_KEY_BYTES;
	
	public static final int ST_INITIALISING		= 0;
	public static final int ST_RUNNING			= 1;
	public static final int ST_DESTROYED		= 2;
	
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

	private PrivateKey			private_key;
	private PublicKey			public_key;
	
	private byte[]				my_uid;
	private MsgSyncNode			my_node;
	
	private ByteArrayHashMap<List<MsgSyncNode>>		node_uid_map 		= new ByteArrayHashMap<List<MsgSyncNode>>();
	private ByteArrayHashMap<MsgSyncNode>			node_uid_loopbacks	= new ByteArrayHashMap<MsgSyncNode>();

	private static final int				MIN_BLOOM_BITS	= 8*8;
	
	private static final int				MAX_MESSAGES			= 128;
	private static final int				MAX_DELETED_MESSAGES	= 64;
	
	private static final int				MAX_NODES				= 128;

	protected static final int				MAX_MESSAGE_SIZE		= 350;
	
	private static final int				MAX_MESSSAGE_REPLY_SIZE	= 4*1024;
	
		
	private Object							message_lock	= new Object();
	
	private LinkedList<MsgSyncMessage>		messages 	= new LinkedList<MsgSyncMessage>();
	
	private LinkedList<byte[]>				deleted_messages_sigs 	= new LinkedList<byte[]>();

	
	private ByteArrayHashMap<String>		message_sigs	= new ByteArrayHashMap<String>();
	
		
	private Map<HashWrapper,String>	request_id_history = 
		new LinkedHashMap<HashWrapper,String>(512,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry<HashWrapper,String> eldest) 
			{
				return size() > 512;
			}
		};
			
	private CopyOnWriteList<MsgSyncListener>		listeners = new CopyOnWriteList<MsgSyncListener>();
	
	private volatile boolean		destroyed;
	
	private volatile int 		status			= ST_INITIALISING;
	private volatile int		last_dht_count	= -1;
	
	private volatile int		in_req;
	private volatile int		out_req_ok;
	private volatile int		out_req_fail;
	
	private int	last_in_req;
	private int last_out_req;
	
	private Average		in_req_average 	= AverageFactory.MovingImmediateAverage( 30*1000/MsgSyncPlugin.TIMER_PERIOD );
	private Average		out_req_average = AverageFactory.MovingImmediateAverage( 30*1000/MsgSyncPlugin.TIMER_PERIOD );
	
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
			
		String	config_key = CryptoManager.CRYPTO_CONFIG_PREFIX + "msgsync." + dht.getNetwork() + "." + ByteFormatter.encodeString( user_key );
		
		boolean	config_updated = false;
		
		Map map = COConfigurationManager.getMapParameter( config_key, new HashMap());
		
		my_uid = (byte[])map.get( "uid" );
		
		if ( my_uid == null || my_uid.length != 8 ){
		
			my_uid = new byte[8];
		
			RandomUtils.nextSecureBytes( my_uid );
			
			map.put( "uid", my_uid );
			
			config_updated = true;
		}
		
		byte[]	public_key_bytes 	= (byte[])map.get( "pub" );
		byte[]	private_key_bytes 	= (byte[])map.get( "pri" );
		 
		if ( public_key_bytes != null && private_key_bytes != null ){
		
			try{
				public_key	= CryptoECCUtils.rawdataToPubkey( public_key_bytes );
				private_key	= CryptoECCUtils.rawdataToPrivkey( private_key_bytes );
				
			}catch( Throwable e ){
				
				public_key	= null;
				private_key	= null;
			}
		}
		
		if ( public_key == null || private_key == null ){
			
			KeyPair ecc_keys = CryptoECCUtils.createKeys();

			public_key	= ecc_keys.getPublic();
			private_key	= ecc_keys.getPrivate();
			
			map.put( "pub", CryptoECCUtils.keyToRawdata( public_key ));
			map.put( "pri", CryptoECCUtils.keyToRawdata( private_key ));
			
			config_updated = true;
		}
		
		if ( config_updated ){
			
			COConfigurationManager.setParameter( config_key, map );
			
			COConfigurationManager.setDirty();
		}
		
		my_node	= new MsgSyncNode( dht.getLocalAddress(), my_uid, CryptoECCUtils.keyToRawdata( public_key ));
		
		dht_key = new SHA1Simple().calculateHash( user_key );
		
		for ( int i=0;i<dht_key.length;i++){
			
			dht_key[i] ^= HANDLER_BASE_KEY_BYTES[i];
		}
		
		dht.registerHandler( dht_key, this );
		
		checkDHT( true );
	}
	
	protected MsgSyncPlugin
	getPlugin()
	{
		return( plugin );
	}
	
	public String
	getName()
	{
		return( "Message Sync: " + getString());
	}

	public int
	getStatus()
	{
		return( status );
	}
	
	public int
	getDHTCount()
	{
		return( last_dht_count );
	}
	
	public int[]
	getNodeCounts()
	{
		int	total	= 0;
		int	live	= 0;
		int	dying	= 0;
		
		synchronized( node_uid_map ){
		
			for ( List<MsgSyncNode> nodes: node_uid_map.values()){
				
				for ( MsgSyncNode node: nodes ){
					
					total++;
					
					if ( node.getFailCount() == 0 ){
						
						if ( node.getLastAlive() > 0 ){
							
							live++;
						}
					}else{
						
						dying++;
					}
				}
			}
		}
		
		return( new int[]{ total, live, dying });
	}
	
	public double[]
	getRequestCounts()
	{
		return( 
			new double[]{ 
				in_req, 
				in_req_average.getAverage()*1000/MsgSyncPlugin.TIMER_PERIOD, 
				out_req_ok, 
				out_req_fail, 
				out_req_average.getAverage()*1000/MsgSyncPlugin.TIMER_PERIOD });
	}
	
	public List<MsgSyncMessage>
	getMessages()
	{
		synchronized( message_lock ){

			List<MsgSyncMessage> result = new ArrayList<MsgSyncMessage>( messages.size());

			for ( MsgSyncMessage msg: messages ){
				
				if ( msg.getStatus() == MsgSyncMessage.ST_OK ){
					
					result.add( msg );
				}
			}
			
			return( result );
		}
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
			
			if ( destroyed ){
				
				return;
			}
			
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
					
				private int		dht_count = 0;
				
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
					
						addDHTContact( originator, m );
						
						dht_count++;
						
					}catch( Throwable e ){
						
					}
				}
				
				@Override
				public void 
				complete(
					byte[] 		key, 
					boolean 	timeout_occurred) 
				{	
					last_dht_count = dht_count;
					
					try{
						if ( first_time ){
							
							if ( diversified ){
								
								log( "Not registering as sufficient nodes located" );
								
								status = ST_RUNNING;
								
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
													
													status = ST_RUNNING;
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
		if ( destroyed ){
			
			return;
		}
		
		int	in_req_diff 	= in_req - last_in_req;
		int	out_req			= out_req_fail + out_req_ok;
		int out_req_diff	= out_req	- last_out_req;
		
		in_req_average.update( in_req_diff );
		out_req_average.update( out_req_diff );
		
		last_out_req	= out_req;
		last_in_req		= in_req;
		
		//System.out.println( in_req_average.getAverage()*1000/MsgSyncPlugin.TIMER_PERIOD + "/" + out_req_average.getAverage()*1000/MsgSyncPlugin.TIMER_PERIOD);
		
		if ( count % NODE_STATUS_CHECK_TICKS == 0 ){
			
			int	failed	= 0;
			int	live	= 0;
			int	total	= 0;
			
			List<MsgSyncNode>	to_remove = new ArrayList<MsgSyncNode>();
			
			synchronized( node_uid_map ){
				
				List<MsgSyncNode>	living		 	= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
				List<MsgSyncNode>	not_failing	 	= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
				List<MsgSyncNode>	failing 		= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
								
				//if ( TRACE )System.out.println( "Current nodes: ");
				
				for ( List<MsgSyncNode> nodes: node_uid_map.values()){
					
					for ( MsgSyncNode node: nodes ){
						
						//if ( TRACE )System.out.println( "    " + node.getContact().getAddress() + "/" + ByteFormatter.encodeString( node.getUID()));
						
						total++;
						
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
			
			log( "Node status: live=" + live + ", failed=" + failed + ", total=" + total + ", to_remove=" + to_remove.size() + "; messages=" + messages.size());
			
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

	private boolean
	addDHTContact(
		DHTPluginContact		contact,
		Map<String,Object>		map )
	{				
		byte[] uid = (byte[])map.get( "u" );
				
		if ( uid != null ){
			
				// we have no verification as to validity of the contact/uid at this point - it'll get checked later
				// if/when we obtain its public key
			
			if ( addNode( contact, uid, null ) != my_node ){
			
				return( true );
			}
		}
		
		return( false );
	}
	
	private MsgSyncNode
	addNode(
		DHTPluginContact		contact,
		byte[]					uid,
		byte[]					public_key )
	{
		MsgSyncNode node = addNodeSupport( contact, uid, public_key );
		
		if ( public_key != null ){
			
			if ( !node.setDetails( contact, public_key)){
								
				node = new MsgSyncNode( contact, uid, public_key );
			}
		}
		
		return( node );
	}
	
	private MsgSyncNode
	addNodeSupport(
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
				
			nodes.add( node );		

			if ( TRACE )System.out.println( "Add node: " + contact.getName() + ByteFormatter.encodeString( uid ) + "/" + (public_key==null?"no PK":"with PK" ) + ", total uids=" + node_uid_map.size());
			
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
					
					//if ( TRACE )System.out.println( "Remove node: " + node.getContact().getName() + ByteFormatter.encodeString( node_id ) + ", loop=" + is_loopback );
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
	
	protected static String
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
		byte[]			signature,
		int				age_secs,
		boolean			is_incoming )
	{
		MsgSyncMessage msg = new MsgSyncMessage( node, message_id, content, signature, age_secs );
			
		if ( msg.getStatus() == MsgSyncMessage.ST_OK || is_incoming ){
			
				// remember message if is it valid or it is incoming - latter is to 
				// prevent an invalid incoming message from being replayed over and over
			
			synchronized( message_lock ){
			
				if ( message_sigs.containsKey( signature )){
					
					return;
				}
				
				message_sigs.put( signature, "" );
										
				ListIterator<MsgSyncMessage> lit = messages.listIterator(messages.size());
				
				boolean added = false;
				
				while( lit.hasPrevious()){
					
					MsgSyncMessage prev  = lit.previous();
					
					if ( prev.getAgeSecs() > age_secs ){
						
						lit.next();
						
						lit.add( msg );
						
						added = true;
						
						break;
					}
				}
				
				if ( !added ){
				
						// no older messages found, stick it at the front
					
					messages.addFirst( msg );
				}
				
				if ( messages.size() > MAX_MESSAGES ){
											
					MsgSyncMessage removed = messages.removeFirst();
						
					byte[]	sig = removed.getSignature();
					
					message_sigs.remove( sig );
					
					deleted_messages_sigs.addLast( sig );
					
					if ( deleted_messages_sigs.size() > MAX_DELETED_MESSAGES ){
						
						deleted_messages_sigs.removeFirst();
					}
				}
			}
		}
		
		if ( msg.getStatus() == MsgSyncMessage.ST_OK || !is_incoming ){
			
				// we want to deliver any local error responses back to the caller but not
				// incoming messages that are errors as these are maintained for house
				// keeping purposes only
			
			for ( MsgSyncListener l: listeners ){
				
				try{
					l.messageReceived( msg );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
	}
	
	public void
	sendMessage(
		byte[]		content )
	{
		if ( content == null ){
			
			content = new byte[0];
		}
		
		try{
			Signature sig = CryptoECCUtils.getSignature( private_key );
			
			byte[]	message_id = new byte[8];
			
			RandomUtils.nextSecureBytes( message_id );
			
			sig.update( my_uid );
			sig.update( message_id );
			sig.update( content );
			
			byte[]	sig_bytes = sig.sign();
			
			addMessage( my_node, message_id, content, sig_bytes, 0, false );
			
			sync( true );
			
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
			
			if ( TRACE )System.out.println( "Sync: active=" + active_syncs );
			
			if ( active_syncs.size() > MAX_CONC_SYNC ){
				
				return;
			}
			
			Set<String>	active_addresses = new HashSet<String>();
			
			for ( MsgSyncNode n: active_syncs ){
				
				active_addresses.add( n.getContactAddress());
			}
							
			List<MsgSyncNode>	not_failed 	= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
			List<MsgSyncNode>	failed 		= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
			List<MsgSyncNode>	live 		= new ArrayList<MsgSyncNode>( MAX_NODES*2 );
			
			for ( List<MsgSyncNode> nodes: node_uid_map.values()){
				
				for ( MsgSyncNode node: nodes ){
					
					if ( active_syncs.size() > 0 ){
					
						if ( active_syncs.contains( node ) || active_addresses.contains( node.getContactAddress())){
						
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
				
				sync_node = getRandomSyncNode( live );
				
			}else{
				
				int	active_fails = 0;
				
				for ( MsgSyncNode node: active_syncs ){
					
					if ( node.getFailCount() > 0 ){
						
						active_fails++;
					}
				}
				
				if ( active_fails >= MAX_FAIL_SYNC && not_failed.size() > 0 ){
					
					sync_node = getRandomSyncNode( not_failed );
				}
				
				if ( sync_node == null ){
					
					sync_node = getRandomSyncNode( failed, not_failed );
				}
			}
			
			if ( TRACE )System.out.println( "    selected " + (sync_node==null?"none":sync_node.getName()));
			
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
	
	private MsgSyncNode
	getRandomSyncNode(
		List<MsgSyncNode>		nodes1,
		List<MsgSyncNode>		nodes2 )
	{
		List<MsgSyncNode>	nodes = new ArrayList<MsgSyncNode>( nodes1.size() + nodes2.size());
		
		nodes.addAll( nodes1 );
		nodes.addAll( nodes2 );
		
		return( getRandomSyncNode( nodes ));
	}
	
	private MsgSyncNode
	getRandomSyncNode(
		List<MsgSyncNode>		nodes )
	{
		int	num = nodes.size();
		
		if ( num == 0 ){
			
			return( null );
			
		}else if ( num == 1 ){
			
			return( nodes.get(0));
			
		}else{
			
			Map<String,Object>	map = new HashMap<String, Object>(num*2);
			
			for ( MsgSyncNode node: nodes ){
				
				String str = node.getContactAddress();
				
				Object x = map.get( str );
				
				if ( x == null ){
					
					map.put( str, node );
					
				}else if ( x instanceof MsgSyncNode ){
					
					List<MsgSyncNode> list = new ArrayList<MsgSyncNode>(10);
					
					list.add((MsgSyncNode)x);
					list.add( node );
					
					map.put( str, list );
					
				}else{
					
					((List<MsgSyncNode>)x).add( node );
				}
			}
			
			int	index = RandomUtils.nextInt( map.size());
			
			Iterator<Object>	it = map.values().iterator();
			
			for ( int i=0;i<index-1;i++){
				
				it.next();
			}
			
			Object result = it.next();
			
			if ( result instanceof MsgSyncNode ){
				
				return((MsgSyncNode)result);
				
			}else{
				
				List<MsgSyncNode>	list = (List<MsgSyncNode>)result;
				
				return( list.get( RandomUtils.nextInt( list.size())));
			}
		}
	}
	
	private void
	sync(
		MsgSyncNode		sync_node )
	{
		byte[]		rand 	= new byte[8];
		BloomFilter	bloom	= null;
		
		ByteArrayHashMap<List<MsgSyncNode>>	msg_node_map = null;
		
		synchronized( message_lock ){

			for ( int i=0;i<64;i++){
				
				RandomUtils.nextSecureBytes( rand );
				
					// slight chance of duplicate keys (maybe two nodes share a public key due to a restart)
					// so just use distinct ones
				
				ByteArrayHashMap<String>	bloom_keys = new ByteArrayHashMap<String>();
				
				Set<MsgSyncNode>	done_nodes = new HashSet<MsgSyncNode>();
				
				msg_node_map = new ByteArrayHashMap<List<MsgSyncNode>>();
					
				for ( MsgSyncMessage msg: messages ){
					
					MsgSyncNode n = msg.getNode();
					
					if ( !done_nodes.contains( n )){
						
						byte[] nid = n.getUID();
						
						List<MsgSyncNode> list = msg_node_map.get( nid );
						
						if ( list == null ){
							
							list = new ArrayList<MsgSyncNode>();
							
							msg_node_map.put( nid, list );
						}
						
						list.add( n );
						
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
				
				for ( byte[] sig: deleted_messages_sigs ){
				
					sig = sig.clone();
					
					for ( int j=0;j<rand.length;j++){
						
						sig[j] ^= rand[j];
					}
					
					bloom_keys.put( sig, ""  );
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
				
				if ( bloom != null ){
					
					break;
				}
			}
		}
		
		if ( bloom == null ){
			
				// clashed too many times, whatever, we'll try again soon
			
			return;
		}
		
		Map<String,Object> request_map = new HashMap<String,Object>();
		
		request_map.put( "v", VERSION );
		
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
		
			out_req_ok++;
			
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
				
				if ( TRACE )System.out.println( "reply: " + reply_map + " from " + sync_node.getName());
				
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
								
								int	age = ((Number)m.get( "a" )).intValue();
										
									// these won't be present if remote believes we already have it (subject to occasional bloom false positives)
								
								byte[] 	public_key		= (byte[])m.get( "p" );
								
								Map<String,Object>		contact_map		= (Map<String,Object>)m.get( "k" );
								
								log( "Message: " + ByteFormatter.encodeString( message_id ) + ": " + new String( content ) + ", age=" + age );
																
								boolean handled = false;
								
									// see if we already have a node with the correct public key
								
								List<MsgSyncNode> nodes = msg_node_map.get( node_uid );
								
								if ( nodes != null ){
									
									for ( MsgSyncNode node: nodes ){
										
										byte[] pk = node.getPublicKey();
										
										if ( pk != null ){
											
											Signature sig = CryptoECCUtils.getSignature( CryptoECCUtils.rawdataToPubkey( pk ));
											
											sig.update( node_uid );
											sig.update( message_id );
											sig.update( content );
											
											if ( sig.verify( signature )){
												
												addMessage( node, message_id, content, signature, age, true );
												
												handled = true;
												
												break;
											}
										}
									}
								}
									
								if ( !handled ){
									
									if ( public_key != null ){
										
											// no existing pk - we HAVE to record this message against
											// ths supplied pk otherwise we can't replicate it later

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
													
													if ( node.setDetails( contact, public_key )){
														
														msg_node = node;
														
														break;
													}
												}
											}
											
											if ( msg_node == null ){
											
												msg_node = addNode( contact, node_uid, public_key );
												
													// save so local list so pk available to other messages
													// in this loop
												
												List<MsgSyncNode> x = msg_node_map.get( node_uid );
												
												if ( x == null ){
													
													x = new ArrayList<MsgSyncNode>();
													
													msg_node_map.put( node_uid, x );
												}
												
												x.add( msg_node );
											}
																						
											addMessage( msg_node, message_id, content, signature, age, true );
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
			
			out_req_fail++;
			
			sync_node.failed();
		}
	}	

	public byte[]
	handleRead(
		DHTPluginContact	originator,
		byte[]				key )
	{
		if ( destroyed ){
			
			return( null );
		}
		
		try{
			Map<String,Object> request_map = BDecoder.decode( key );

			int	type = request_map.containsKey( "t" )?((Number)request_map.get( "t" )).intValue():-1; 

			if ( type != 0 ){
				
				return( null );
			}

			byte[]	rand = (byte[])request_map.get( "r" );

			HashWrapper rand_wrapper = new HashWrapper( rand );
			
			synchronized( request_id_history ){
				
				if ( request_id_history.get( rand_wrapper ) != null ){
				
					if ( TRACE )System.out.println( "duplicate request: " + request_map + " from " + getString( originator ));
					
					return( null );
				}
				
				request_id_history.put( rand_wrapper, "" );
			}
			
			if ( TRACE )System.out.println( "request: " + request_map + " from " + getString( originator ));

			in_req++;
			
			Map<String,Object> reply_map = new HashMap<String,Object>();

			int		status;

			byte[]	uid = (byte[])request_map.get( "u" );

			if ( Arrays.equals( my_uid, uid )){
				
				status = STATUS_LOOPBACK;
				
			}else{
				
				/*
				if ( originator.getAddress().isUnresolved()){
					
					System.out.println( "unresolved" );
				}
				*/
				
				status = STATUS_OK;
				
				List<MsgSyncNode> caller_nodes = getNodes( uid );
				
				boolean found_originator = false;
				
				if ( caller_nodes != null ){
					
					for ( MsgSyncNode n: caller_nodes ){
						
						if ( sameContact( originator, n.getContact())){
							
							found_originator = true;
						}
					}
				}
				
				if ( !found_originator ){
					
					addNode( originator, uid, null );
				}
				
				BloomFilter bloom = BloomFilterFactory.deserialiseFromMap((Map<String,Object>)request_map.get("b"));
								
				List<MsgSyncMessage>	missing = new ArrayList<MsgSyncMessage>();
				
				int	num_they_have_i_dont = bloom.getEntryCount();
				
				synchronized( message_lock ){
					
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
					
					int	content_bytes = 0;
					
					for ( MsgSyncMessage message: missing ){
						
						if ( content_bytes > MAX_MESSSAGE_REPLY_SIZE ){
							
							break;
						}
												
						if ( message.getStatus() != MsgSyncMessage.ST_OK ){
							
								// invalid message, don't propagate
							
							continue;
						}
						
						if ( TRACE )System.out.println( "    returning " + ByteFormatter.encodeString( message.getID()));
						
						Map<String,Object> m = new HashMap<String,Object>();
						
						l.add( m );
						
						MsgSyncNode	n = message.getNode();
						
						byte[]	content = message.getContent();
						
						content_bytes += content.length;

						m.put( "u", n.getUID());
						m.put( "i", message.getID());
						m.put( "c", content );
						m.put( "s", message.getSignature());
						m.put( "a", message.getAgeSecs());
												
						if ( !done_nodes.contains( n )){
							
							done_nodes.add( n );
							
							byte[]	pub = n.getPublicKey();	
							
							if ( pub != null ){
							
								pub = pub.clone();
								
								for ( int i=0;i<rand.length;i++){
									
									pub[i] ^= rand[i];
								}
								
								if ( !bloom.contains( pub )){
									
									if ( TRACE )System.out.println( "    and pk" );
									
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
			
			//e.printStackTrace();
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
	
	public void
	addListener(
		MsgSyncListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		MsgSyncListener		listener )
	{
		listeners.remove( listener );
	}
	
	protected void
	destroy()
	{
		destroyed	= true;
		
		status = ST_DESTROYED;
		
		dht.unregisterHandler( dht_key, this );
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
