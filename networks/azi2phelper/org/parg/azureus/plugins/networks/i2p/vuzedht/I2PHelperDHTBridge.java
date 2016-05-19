/*
 * Created on May 16, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.networks.i2p.vuzedht;

import java.util.*;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.parg.azureus.plugins.networks.i2p.I2PHelperPlugin;
import org.parg.azureus.plugins.networks.i2p.router.I2PHelperRouter;

import com.aelitis.azureus.core.dht.control.DHTControl;

public class 
I2PHelperDHTBridge 
	implements DistributedDatabaseTransferType, DistributedDatabaseTransferHandler
{
	private static boolean TEST_LOOPBACK	= true;
	
	static{
		if ( TEST_LOOPBACK ){
			System.err.println( "**** I2P DHT Bridge loopback enabled ****" );
		}
	}
	
	private I2PHelperPlugin			plugin;
	
	private final AESemaphore		init_sem = new AESemaphore( "I2PHelperDHTBridge" );
			
	private boolean					ddb_write_initialised;
	
	private DistributedDatabase			ddb_read;
	private DistributedDatabase[]		ddb_write = new DistributedDatabase[2];
	
	private AsyncDispatcher		bridge_dispatcher = new AsyncDispatcher();
	
	public
	I2PHelperDHTBridge(
		I2PHelperPlugin		_plugin )
	{
		plugin	= _plugin;
		
		if ( TEST_LOOPBACK ){
			
			byte[] key 		= new byte[10];
			byte[] value 	= new byte[10];
			
			value[0] = 45;
			
			RandomUtils.nextBytes( key );

			writeToBridge( "BLORP", key, value );
		}
	}
	
	public void
	setDDB(
		DistributedDatabase _read_ddb )
		
	{
		try{

			ddb_read = _read_ddb;
		
			ddb_read.addTransferHandler( this, this );
	
		}catch( Throwable e ){
			
			Debug.out( e );
			
		}finally{
			
			init_sem.releaseForever();
		}
	}
	
	public void
	writeToBridge(
		final String		desc,
		final byte[]		key,
		final byte[]		value )
	{
			// TODO: periodic republish every
			// DHTControl.ORIGINAL_REPUBLISH_INTERVAL_DEFAULT
		
		if ( bridge_dispatcher.getQueueSize() > 256 ){
			
			return;
		}
		
		bridge_dispatcher.dispatch(
			new AERunnable(){
				public void 
				runSupport()
				{
					writeToBridgeAsync( desc, key, value );
				}
			});
	}
	
	private void
	writeToBridgeAsync(
		String		desc,
		byte[]		key,
		byte[]		value )
	{
		init_sem.reserve();
		
		try{		
			synchronized( this ){
				
				if ( !ddb_write_initialised ){
					
					ddb_write_initialised = true;
			
					Map<String,Object>	options = new HashMap<String, Object>();
					
					options.put( "server_id", "DHT Bridge" );
					options.put( "server_id_transient", true );
					options.put( "server_sm_type", I2PHelperRouter.SM_TYPE_PURE );
							
					for ( int i=0;i<2;i++){
						
						String[]	nets = i==0?new String[]{ AENetworkClassifier.AT_PUBLIC, AENetworkClassifier.AT_I2P }:new String[]{ AENetworkClassifier.AT_I2P };
						
						options.put( "server_name", i==0?"Mix Bridge":"I2P Bridge" );
		
						List<DistributedDatabase> ddbs = plugin.getPluginInterface().getUtilities().getDistributedDatabases( nets, options );
						
						for ( DistributedDatabase ddb: ddbs ){
							
							if ( ddb.getNetwork() == AENetworkClassifier.AT_I2P ){
								
								while( !ddb.isInitialized()){
									
									Thread.sleep(1000);
								}
																
								ddb_write[i] = ddb;
							}
						}
					}
				}
			}
			
			writeToBridgeSupport( desc, key, value );
			
		}catch( Throwable e ){
		
			Debug.out( e );
		}

	}
	
	private void
	writeToBridgeSupport(
		final String		desc,
		final byte[]		key,
		final byte[]		value )
	{
		if ( TEST_LOOPBACK ){
			
				// have to store it in ddb_read as the actual write will go elsewhere in the mix DHT
			
			try{
				System.out.println( "Writing to mix, key=" + ByteFormatter.encodeString( key ));
				
				DistributedDatabaseKey 		k = ddb_read.createKey( key, desc );
				DistributedDatabaseValue 	v = ddb_read.createValue( value );
		
				ddb_read.write(
						new DistributedDatabaseListener()
						{
							public void
							event(
								DistributedDatabaseEvent		event )
							{
							}
						},
						k, v );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
			
		}
		
		DistributedDatabase			pure_dht 	= ddb_write[1];

		if ( pure_dht != null ){
			
			try{		
				DistributedDatabaseKey 		pure_k = pure_dht.createKey( key, desc );
				DistributedDatabaseValue 	pure_v = pure_dht.createValue( value );
		
				pure_dht.write(
						new DistributedDatabaseListener()
						{
							public void
							event(
								DistributedDatabaseEvent		event )
							{
							}
						},
						pure_k, pure_v );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		final DistributedDatabase	mix_dht 	= ddb_write[0];

		if ( mix_dht != null ){
			
			try{
	
				DistributedDatabaseKey 		mix_k = mix_dht.createKey( key, desc );
				DistributedDatabaseValue 	mix_v = mix_dht.createValue( value );
		
				mix_dht.write(
						new DistributedDatabaseListener()
						{
							private List<DistributedDatabaseContact>	write_contacts = new ArrayList<DistributedDatabaseContact>();
							public void
							event(
								DistributedDatabaseEvent		event )
							{
								int type = event.getType();
							
								if ( type == DistributedDatabaseEvent.ET_VALUE_WRITTEN ){
									
									synchronized( write_contacts ){
									
										write_contacts.add(event.getContact());
									}
								}else if ( type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
									
									bridge_dispatcher.dispatch(
										new AERunnable(){
											public void 
											runSupport()
											{
												sendBridgeRequest( mix_dht, key, value,  write_contacts );
											}
										});
								}
							}
						},
						mix_k, mix_v );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	private void
	sendBridgeRequest(
		DistributedDatabase					ddb,
		byte[]								key,
		byte[]								value,
		List<DistributedDatabaseContact>	contacts )
	{
		Map<String,Object>	request = new HashMap<String,Object>();
		
		request.put( "k", key );
		request.put( "v", value );
	
		if ( TEST_LOOPBACK ){
		
			contacts.add( 0, ddb_read.getLocalContact());
		}
		
		try{
			DistributedDatabaseKey read_key = ddb.createKey( BEncoder.encode( request ));
	
			
			for ( DistributedDatabaseContact contact: contacts ){
				
				if ( contact.getVersion() < DHTUtilsI2P.PROTOCOL_VERSION_BRIDGE ){
					
					continue;
				}
								
				try{
					DistributedDatabaseValue result = 
						contact.read(
							new DistributedDatabaseProgressListener() {								
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
							this,
							read_key,
							30*1000 );
										
					if ( result != null ){
						
						Map<String,Object> reply = (Map<String,Object>)BDecoder.decode((byte[])result.getValue( byte[].class ));

						Long r = (Long)reply.get( "a" );
						
						if ( r != null && r == 1 ){
						
								// job done
							
							break;
						}
					}
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
		
	public DistributedDatabaseValue 
	write(
		DistributedDatabaseContact 			contact,
		DistributedDatabaseTransferType 	type, 
		DistributedDatabaseKey 				key,
		DistributedDatabaseValue 			value ) 
					
		throws DistributedDatabaseException 
	{
		return( null );
	}
	
	@Override
	public DistributedDatabaseValue 
	read(
		DistributedDatabaseContact 			contact,
		DistributedDatabaseTransferType 	type, 
		DistributedDatabaseKey 				ddb_key )
		
		throws DistributedDatabaseException 
	{
		Object	o_key = ddb_key.getKey();
		
		try{
			byte[]	key_bytes = (byte[])o_key;
						
			Map<String,Object>	request = BDecoder.decode( key_bytes );
			
			byte[]	request_key		= (byte[])request.get( "k" );
			byte[]	request_value	= (byte[])request.get( "v" );
			
			List<DistributedDatabaseValue> values = ddb_read.getValues( ddb_read.createKey( request_key));
						
			boolean	ok = false;
			
			for ( DistributedDatabaseValue v: values ){
				
				byte[] b_value = (byte[])v.getValue( byte[].class );
				
				if ( Arrays.equals( b_value, request_value )){
										
					ok = true;
							
					break;
				}
			}
			
			if ( ok ){
				
				DistributedDatabase public_ddb = plugin.getPluginInterface().getDistributedDatabase();
				
				if ( public_ddb.isAvailable()){
					
					DistributedDatabaseKey dd_key = public_ddb.createKey( request_key, "Bridge mapping" );
					
					dd_key.setFlags( DistributedDatabaseKey.FL_ANON );
					
					DistributedDatabaseValue dd_value = public_ddb.createValue( request_value );
					
					public_ddb.write(
						new DistributedDatabaseListener() {							
							@Override
							public void event(DistributedDatabaseEvent event) {
							}
						}, dd_key, dd_value );
				}else{
					
					ok = false;
				}
			}
			
			Map<String,Object>	result = new HashMap<String, Object>();
			
			result.put( "a", new Long(ok?1:0));
			
			return( ddb_read.createValue( BEncoder.encode( result )));
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
}
