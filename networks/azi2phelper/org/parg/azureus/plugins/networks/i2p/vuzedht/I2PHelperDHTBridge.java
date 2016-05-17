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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.parg.azureus.plugins.networks.i2p.I2PHelperPlugin;
import org.parg.azureus.plugins.networks.i2p.router.I2PHelperRouter;

public class 
I2PHelperDHTBridge 
	implements DistributedDatabaseTransferType, DistributedDatabaseTransferHandler
{
	private I2PHelperPlugin			plugin;
	
	private DistributedDatabase			ddb_read;
	private DistributedDatabase[]		ddb_write = new DistributedDatabase[2];
	
	public
	I2PHelperDHTBridge(
		I2PHelperPlugin		_plugin )
	{
		plugin	= _plugin;
	}
	
	public void
	setDDB(
		DistributedDatabase _read_ddb )
		
	{
		ddb_read = _read_ddb;
		
		try{
			ddb_read.addTransferHandler( this, this );
	
				// TODO: only need to create write side of things when a write actually occurs
			
			Map<String,Object>	options = new HashMap<String, Object>();
			
			options.put( "server_id", "DHT Bridge" );
			options.put( "server_id_transient", true );
			options.put( "server_sm_type", I2PHelperRouter.SM_TYPE_PURE );
					
			for ( int i=0;i<2;i++){
				
				String[]	nets = i==0?new String[]{ AENetworkClassifier.AT_PUBLIC, AENetworkClassifier.AT_I2P }:new String[]{ AENetworkClassifier.AT_I2P };;
				
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
		
			int	num = 0;
			
			while( true ){
				
				for ( DistributedDatabase ddb: ddb_write ){
					
					if ( ddb == null ){
						
						continue;
					}
					
					byte[] key 		= new byte[10];
					byte[] value 	= new byte[10];
					
					RandomUtils.nextBytes( key );
					
					int	write_num = num++;
					
					DistributedDatabaseKey 		k = ddb.createKey( key, "TEST WRITE " + write_num );
					DistributedDatabaseValue 	v = ddb.createValue( value );
					
					final int f_n = write_num;
					
					ddb.write(
							new DistributedDatabaseListener()
							{
								public void
								event(
									DistributedDatabaseEvent		event )
								{
									if ( event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
										
										System.out.println( "Test write " + f_n + " complete" );
									}
								}
							},
							k, v );
						
				}
				
				Thread.sleep( 60*1000 );
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
		DistributedDatabaseKey 				key )
		
		throws DistributedDatabaseException 
	{
		return( ddb_read.createValue( new byte[40000] ));
	}
}
