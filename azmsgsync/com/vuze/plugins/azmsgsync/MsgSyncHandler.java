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
	
	private final MsgSyncPlugin						plugin;
	private final DHTPluginInterface				dht;
	private final byte[]							user_key;
	private final byte[]							dht_key;
	
	private KeyPair				ecc_keys;
	
	private byte[]				my_uid;
	private MsgSyncNode			my_node;
	
	private Map<String,MsgSyncNode>		nodes = new HashMap<String, MsgSyncNode>();
	
		// this is linked to the bloom filter size which in turn is limited by the max transfer
		// key size of 255 bytes....
	
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
		final DHTPluginContact		contact,
		Map						map )
	{
		System.out.println( "Add contact: " + contact.getName() + "/" + map );
				
		byte[] uid = (byte[])map.get( "u" );
		
		if ( uid == null || Arrays.equals( uid, my_uid )){
			
			return;
		}
		
		synchronized( nodes ){
			
			InetSocketAddress address = contact.getAddress();
			
			String str = address.toString();
			
			if ( nodes.containsKey( str )){
				
				return;
			}
			
			nodes.put( str, new MsgSyncNode( contact, uid, null ));
			
			System.out.println( "address=" + str );
		}
		
		sync();	
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
			
			MsgSyncMessage msg = new MsgSyncMessage( my_node, message_id, content, sig_bytes );
			
			synchronized( messages ){
			
				messages.add( msg );
				
				if ( messages.size() > MAX_MESSAGES ){
					
					messages.removeFirst();
				}
			}
			
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
		
		synchronized( nodes ){
			
			if ( nodes.size() > 0 ){
				
				Iterator<MsgSyncNode>	it = nodes.values().iterator();
				
				node = it.next();
				
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
		MsgSyncNode		node )
	{
		byte[]		rand 	= new byte[8];
		BloomFilter	bloom	= null;
		
		synchronized( messages ){

			for ( int i=0;i<32;i++){
				
				RandomUtils.nextSecureBytes( rand );
				
				List<byte[]>	bloom_keys = new ArrayList<byte[]>();
				
				Set<MsgSyncNode>	done_nodes = new HashSet<MsgSyncNode>();
				
				for ( MsgSyncMessage msg: messages ){
					
					MsgSyncNode n = msg.getNode();
					
					if ( !done_nodes.contains( n )){
						
						done_nodes.add( n );
						
						byte[] pub = node.getPublicKey().clone();
						
						if ( pub != null ){
							
							for ( int j=0;j<rand.length;j++){
								
								pub[j] ^= rand[j];
							}
							
							bloom_keys.add( pub );
						}
					}
					
					byte[]	sig = msg.getSignature().clone();
		
					for ( int j=0;j<rand.length;j++){
						
						sig[j] ^= rand[j];
					}
					
					bloom_keys.add( sig );
				}
				
				bloom = BloomFilterFactory.createAddOnly( bloom_keys.size() * 10 );
				
				for ( byte[] k: bloom_keys ){
					
					if ( bloom.contains( k )){
						
						bloom = null;
						
						break;
					}
				}
			}
		}
		
		if ( bloom == null ){
			
			Debug.out( "Too many clashes, bailing" );
			
			return;
		}
		
		Map sync_map = new HashMap();
		
		sync_map.put( "b", bloom.serialiseToMap());
		sync_map.put( "r", rand );
		
		try{
			byte[]	sync_key = BEncoder.encode( sync_map);
			
			byte[] reply_bytes = 
				node.getContact().read(
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
				
				System.out.println( "reply: " + reply_map );
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
		System.out.println( "handle read" );
		
		try{
			Map<String,Object> map = BDecoder.decode( key );
						
			BloomFilter bloom = BloomFilterFactory.deserialiseFromMap((Map<String,Object>)map.get("b"));
			
			byte[]	rand = (byte[])map.get( "r" );
			
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
			
			Map<String,Object> reply_map = new HashMap<String,Object>();
			
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
						
						byte[]	pub = n.getPublicKey().clone();	
						
						if ( pub != null ){
							
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
