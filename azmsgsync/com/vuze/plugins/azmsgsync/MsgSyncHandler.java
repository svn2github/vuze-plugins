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

import java.util.*;

import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationAdapter;
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
	
	private final DHTPluginInterface				dht;
	private final byte[]							user_key;
	private final byte[]							dht_key;
	
	protected
	MsgSyncHandler(
		DHTPluginInterface		_dht,
		byte[]					_key )
	{
		dht				= _dht;
		user_key		= _key;
						
		dht_key = new SHA1Simple().calculateHash( user_key );
		
		for ( int i=0;i<dht_key.length;i++){
			
			dht_key[i] ^= HANDLER_BASE_KEY_BYTES[i];
		}
		
		dht.registerHandler( dht_key, this );

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
				}
				
				@Override
				public void 
				complete(
					byte[] 		key, 
					boolean 	timeout_occurred) 
				{
				
					if ( !diversified ){
						
						Map	blah = new HashMap();
						
						try{
							byte[] blah_bytes = BEncoder.encode( blah );
							
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
										}
									});
							
						}catch( Throwable e ){
							
							Debug.out( e);
						}
					}
				}
			});
		

			
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
	
	public void
	sendMessage(
		byte[]		content )
	{
		
	}
	
	public String
	getName()
	{
		return( "Message Sync: " + getString());
	}
	
	public byte[]
	handleRead(
		DHTPluginContact	originator,
		byte[]				key )
	{
		return( null );
	}
	
	public void
	handleWrite(
		DHTPluginContact	originator,
		byte[]				key,
		byte[]				value )
	{
		
	}
	
	protected String
	getString()
	{
		return( dht + "/" + ByteFormatter.encodeString( dht_key ) + "/" + new String( user_key ));
	}
}
