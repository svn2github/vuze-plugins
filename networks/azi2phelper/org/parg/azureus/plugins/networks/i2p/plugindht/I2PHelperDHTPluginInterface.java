/*
 * Created on Sep 6, 2014
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


package org.parg.azureus.plugins.networks.i2p.plugindht;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.parg.azureus.plugins.networks.i2p.*;
import org.parg.azureus.plugins.networks.i2p.router.I2PHelperRouter;
import org.parg.azureus.plugins.networks.i2p.router.I2PHelperRouterDHT;
import org.parg.azureus.plugins.networks.i2p.vuzedht.I2PHelperAZDHT;















import com.aelitis.azureus.core.dht.DHTStorageKeyStats;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface;
import com.aelitis.azureus.plugins.dht.DHTPluginKeyStats;
import com.aelitis.azureus.plugins.dht.DHTPluginListener;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginProgressListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;


public class 
I2PHelperDHTPluginInterface
	implements DHTPluginInterface
{
	final private I2PHelperPlugin		plugin;
	final private String				name;
	
	private final int					dht_index;
	
	private volatile I2PHelperAZDHT		dht;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher( "I2PHelperDHTPluginInterface" );
	
	private AESemaphore			init_sem = new AESemaphore( "I2PHelperDHTPluginInterface" );
	
	private volatile TimerEventPeriodic	init_event;
	
	private DHTPluginContact	local_contact = new LocalContact();
	
	public 
	I2PHelperDHTPluginInterface(
		I2PHelperPlugin		_plugin,
		int					_dht_index,
		I2PHelperAZDHT		_dht,
		String				_name )
	{
		plugin		= _plugin;
		dht_index	= _dht_index;
		dht			= _dht;
		name		= _name;
		
		init_event = SimpleTimer.addPeriodicEvent(
				"I2PHelperDHTPluginInterface",
				1000,
				new TimerEventPerformer() {
					
					@Override
					public void 
					perform(
						TimerEvent event )
					{
						if ( init_event == null ){
							
							return;
						}
		
						if ( dht.isInitialised()){
							
							init_event.cancel();
							
							init_event = null;
							
							init_sem.releaseForever();
						}
					}
				});	
	}
	
	public 
	I2PHelperDHTPluginInterface(
		I2PHelperPlugin		_plugin,
		int					_dht_index )
	{
		plugin			= _plugin;
		dht_index		= _dht_index;
		
		name			= "<internal>";
		
		final int dht_index = _dht_index;
		
		init_event = SimpleTimer.addPeriodicEvent(
			"I2PHelperDHTPluginInterface",
			1000,
			new TimerEventPerformer() {
				
				@Override
				public void 
				perform(
					TimerEvent event )
				{
					if ( init_event == null ){
						
						return;
					}
					
					final I2PHelperRouter router = plugin.getRouter();
					
					if ( router != null ){
						
						init_event.cancel();
						
						new AEThread2( "I2PHelperDHTPluginInterface" )
						{
							public void
							run()
							{
								try{
									I2PHelperRouterDHT router_dht = router.selectDHT( dht_index );
									
									if ( router_dht != null ){
										
										I2PHelperDHT helper_dht = router_dht.getDHTBlocking();
										
										if ( helper_dht != null ){
											
											dht = helper_dht.getHelperAZDHT();
											
										}else{
											
											Debug.out( "Helper DHT not available" );;
										}
									}else{
										
										Debug.out( "Router DHT not available" );
									}
								}finally{
									
									init_sem.releaseForever();
								}
							}
						}.start();
					}
				}
			});
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public boolean
	isEnabled()
	{
		return( true );
	}
	
	public boolean
	isExtendedUseAllowed()
	{
		return( true );
	}
	
	@Override
	public boolean 
	isInitialising() 
	{
		if ( init_sem.isReleasedForever()){
			
			I2PHelperAZDHT azdht = dht;
			
			if ( azdht != null ){
				
				return( !azdht.waitForInitialisation(1));
				
			}else{
				
				return( false );
			}
		}else{
		
			return( true );
		}
	}
	
	public int
	getDHTIndex()
	{
		return( dht_index );
	}
	
	public I2PHelperAZDHT
	getDHT(
		int		max_wait )
	{
		init_sem.reserve( max_wait );
		
		return( dht );
	}
	
	@Override
	public boolean 
	isSleeping() 
	{
		return( false );
	}
	
	public String
	getNetwork()
	{
		return( AENetworkClassifier.AT_I2P );
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		return( local_contact );
	}
	
	public DHTPluginKeyStats
	decodeStats(
		DHTPluginValue		value )
	{
		if (( value.getFlags() & DHTPlugin.FLAG_STATS) == 0 ){
			
			return( null );
		}
		
		try{
			DataInputStream	dis = new DataInputStream( new ByteArrayInputStream( value.getValue()));
			
			final DHTStorageKeyStats stats = DHTPluginStorageManager.decodeStats( dis );
			
			return( 
				new DHTPluginKeyStats()
				{
					public int
					getEntryCount()
					{
						return( stats.getEntryCount());
					}
					
					public int
					getSize()
					{
						return( stats.getSize());
					}
					
					public int
					getReadsPerMinute()
					{
						return( stats.getReadsPerMinute());
					}
					
					public byte
					getDiversification()
					{
						return( stats.getDiversification());
					}
				});
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}

	public void
	registerHandler(
		final byte[]					handler_key,
		final DHTPluginTransferHandler	handler,
		final Map<String,Object>		options )
	{
		if ( dht != null && dispatcher.getQueueSize() == 0 ){
			
			dht.registerHandler( handler_key, handler, options );
		
		}else{
			
			if ( dispatcher.getQueueSize() > 100 ){
				
				Debug.out( "Dispatch queue too large" );
			}
			
			dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						I2PHelperAZDHT	dht_to_use = dht;
						
						if ( dht_to_use == null ){
							
							init_sem.reserve();
							
							dht_to_use = dht;
						}
						
						if ( dht_to_use != null ){
						
							dht_to_use.registerHandler( handler_key, handler, options );
							
						}else{
							
							Debug.out( "Failed to initialise DHT" );
						}
					}
				});
		}	
	}	
	
	public void
	unregisterHandler(
		final byte[]					handler_key,
		final DHTPluginTransferHandler	handler )
	{
		if ( dht != null && dispatcher.getQueueSize() == 0 ){
			
			dht.unregisterHandler( handler_key, handler );
		
		}else{
						
			dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						I2PHelperAZDHT	dht_to_use = dht;
						
						if ( dht_to_use == null ){
							
							init_sem.reserve();
							
							dht_to_use = dht;
						}
						
						if ( dht_to_use != null ){
						
							dht_to_use.unregisterHandler( handler_key, handler );
							
						}else{
							
							Debug.out( "Failed to initialise DHT" );
						}
					}
				});
		}	
	}	

	public DHTPluginContact
	importContact(
		InetSocketAddress				address )
	{
		if ( dht == null ){
			
			Debug.out( "DHT not yet available" );
		
			return( null );
		}
		
		return( dht.importContact( address ));
	}
		
	public DHTPluginContact
	importContact(
		Map<String,Object>		map )
	{
		if ( dht == null ){
			
			Debug.out( "DHT not yet available" );
		
			return( null );
		}
		
		return( dht.importContact( map ));
	}
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address,
		byte							version )
	{
		Debug.out( "not imp" );
		
		return( null );
	}
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address,
		byte							version,
		boolean							is_cvs )
	{
		if ( dht == null ){
			
			Debug.out( "DHT not yet available" );
		
			return( null );
		}
		
		return( dht.importContact( address ));
	}
	
	public void
	get(
		final byte[]							key,
		final String							description,
		final byte								flags,
		final int								max_values,
		final long								timeout,
		final boolean							exhaustive,
		final boolean							high_priority,
		final DHTPluginOperationListener		listener )
	{	
		if ( dht != null && dispatcher.getQueueSize() == 0 ){
			
			dht.get( key, description, (short)(flags&0x00ff), max_values, timeout, exhaustive, high_priority, listener );
		
		}else{
			
			if ( dispatcher.getQueueSize() > 100 ){
				
				Debug.out( "Dispatch queue too large" );
				
				listener.complete( key, false );
			}
			
			dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						I2PHelperAZDHT	dht_to_use = dht;
						
						if ( dht_to_use == null ){
							
							init_sem.reserve();
							
							dht_to_use = dht;
						}
						
						if ( dht_to_use == null ){
							
							listener.complete( key, false );
							
						}else{
							
							dht_to_use.get( key, description, (short)(flags&0x00ff), max_values, timeout, exhaustive, high_priority, listener );
						}
					}
				});
		}
	}
	
	public void
	put(
		final byte[]						key,
		final String						description,
		final byte[]						value,
		final byte							flags,
		final DHTPluginOperationListener	listener)
	{	
		if ((flags & DHTPluginInterface.FLAG_BRIDGED )!= 0 ){
			
			plugin.handleBridgePut( this, key, description, value, flags, listener );
			
		}else{
			
			if ( dht != null && dispatcher.getQueueSize() == 0 ){
				
				dht.put( key, description, value, (short)(flags&0x00ff), true, listener );
			
			}else{
				
				if ( dispatcher.getQueueSize() > 100 ){
					
					Debug.out( "Dispatch queue too large" );
					
					listener.complete( key, false );
				}
				
				dispatcher.dispatch(
					new AERunnable() {
						
						@Override
						public void 
						runSupport() 
						{
							I2PHelperAZDHT	dht_to_use = dht;
							
							if ( dht_to_use == null ){
								
								init_sem.reserve();
								
								dht_to_use = dht;
							}
							
							if ( dht_to_use == null ){
								
								listener.complete( key, false );
								
							}else{
								
								dht.put( key, description, value, (short)(flags&0x00ff), true, listener );
							}
						}
					});
			};
		}
	}
	
	public void
	remove(
		final byte[]						key,
		final String						description,
		final DHTPluginOperationListener	listener )
	{
		if ( dht != null && dispatcher.getQueueSize() == 0 ){
			
			dht.remove( key, description, listener );
		
		}else{
			
			if ( dispatcher.getQueueSize() > 100 ){
				
				Debug.out( "Dispatch queue too large" );
				
				listener.complete( key, false );
			}
			
			dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						I2PHelperAZDHT	dht_to_use = dht;
						
						if ( dht_to_use == null ){
							
							init_sem.reserve();
							
							dht_to_use = dht;
						}
						
						if ( dht_to_use == null ){
							
							listener.complete( key, false );
							
						}else{
							
							dht.remove( key, description, listener );
						}
					}
				});
		};	
	}
	
	public void
	remove(
		DHTPluginContact[]			targets,
		byte[]						key,
		String						description,
		DHTPluginOperationListener	listener )
	{
		Debug.out( "not imp" );
	}
	
	@Override
	public DHTInterface[] 
	getDHTInterfaces() 
	{
		I2PHelperAZDHT dht = getDHT( 5*1000 );
	
		if ( dht == null ){
			
			return( new DHTInterface[0] );
		}
		
		return( new DHTInterface[]{ dht } );
	}
	
	@Override
	public List<DHTPluginValue> 
	getValues() 
	{
		List<DHTPluginValue>	vals = new ArrayList<DHTPluginValue>();

		if ( dht != null ){
							
			try{
				DHTDB	db = dht.getDHT().getDataBase();
				
				Iterator<HashWrapper>	keys = db.getKeys();
				
				
				while( keys.hasNext()){
					
					DHTDBValue val = db.getAnyValue( keys.next());
					
					if ( val != null ){
						
						vals.add( mapValue( val ));
					}
				}
				
				return( vals );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( vals );
	}
	
	@Override
	public List<DHTPluginValue> 
	getValues(byte[] key)
	{
		List<DHTPluginValue>	vals = new ArrayList<DHTPluginValue>();

		if ( dht != null ){
							
			try{
				List<DHTTransportValue> values = dht.getDHT().getStoredValues( key );
							
				for ( DHTTransportValue v: values ){
					
					vals.add( mapValue( v ));
				}
				
				return( vals );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( vals );
	}

	protected DHTPluginValue
	mapValue(
		final DHTTransportValue	value )
	{
		if ( value == null ){
			
			return( null );
		}
		
		return( new DHTPluginValueImpl(value));
	}
	
	public void
	addListener(
		DHTPluginListener	l )
	{	
	}
	
	public void
	removeListener(
		DHTPluginListener	l )
	{
	}
	
	public void
	log(
		String	str )
	{
		plugin.log( str );
	}
	
	private class
	DHTPluginValueImpl
		implements DHTPluginValue
	{
		private DHTTransportValue		value;
		
		private 
		DHTPluginValueImpl(
			DHTTransportValue		_v )
		{
			value	= _v;
		}
		
		public byte[]
		getValue()
		{
			return( value.getValue());
		}
		
		@Override
		public long getCreationTime() {
			return( value.getCreationTime());
		}
		
		@Override
		public int getFlags() {
			return( value.getFlags());
		}
		
		@Override
		public long getVersion() {
			return( value.getVersion());
		}
		@Override
		public boolean isLocal() {
			return( value.isLocal());
		}
	}
	
	private class
	LocalContact
		implements DHTPluginContact
	{
		private volatile DHTTransportContact	delegate;
		
		private DHTTransportContact
		fixup()
		{
			if ( delegate != null ){
				
				return( delegate );
			}
			
			if ( !init_sem.reserve( 2*60*1000 )){
				
				Debug.out( "hmm" );
				
				return( null );
			}

			try{
				
				delegate = dht.getDHT().getTransport().getLocalContact();
			
				return( delegate );
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				return( null );
			}
		}
		
		public byte[]
		getID()
		{
			DHTTransportContact contact = fixup();
			
			return( contact.getID());
		}
		
		public String
		getName()
		{
			DHTTransportContact contact = fixup();
			
			return( contact.getName());
		}
		
		public InetSocketAddress
		getAddress()
		{
			DHTTransportContact contact = fixup();
			
			return( contact.getAddress());	
		}
		
		public byte
		getProtocolVersion()
		{
			DHTTransportContact contact = fixup();
			
			return( contact.getProtocolVersion());			
		}
		
		public int
		getNetwork()
		{
			DHTTransportContact contact = fixup();
			
			return( contact.getTransport().getNetwork());	
		}
		
		@Override
		public Map<String, Object> 
		exportToMap()
		{
			DHTTransportContact contact = fixup();
			
			return( contact.exportContactToMap());	
		}
		
		public boolean
		isAlive(
			long		timeout )
		{
			return( true );
		}
		
		public void
		isAlive(
			long						timeout,
			DHTPluginOperationListener	listener )
		{
			listener.complete( null, false );
		}
		
		public boolean
		isOrHasBeenLocal()
		{
			return( true );
		}
		
		public Map
		openTunnel()
		{
			Debug.out( "not imp" );
			
			return( null );		
		}
		
		public byte[]
		read(
			final DHTPluginProgressListener		listener,
			byte[]								handler_key,
			byte[]								key,
			long								timeout )
		{
			DHTTransportContact contact = fixup();
			
			try{
				return( contact.getTransport().readTransfer(
						listener == null ? null :
						new DHTTransportProgressListener()
						{
							public void
							reportSize(
								long	size )
							{
								listener.reportSize( size );
							}
							
							public void
							reportActivity(
								String	str )
							{
								listener.reportActivity( str );
							}
							
							public void
							reportCompleteness(
								int		percent )
							{
								listener.reportCompleteness( percent );
							}
						},
						contact, 
						handler_key, 
						key, 
						timeout ));
				
			}catch( Throwable e ){
				
				throw( new RuntimeException( e ));
			}
		}
		
		public void
		write(
			final DHTPluginProgressListener		listener,
			byte[]								handler_key,
			byte[]								key,
			byte[]								data,
			long								timeout )
		{
			DHTTransportContact contact = fixup();
			
			try{
				contact.getTransport().writeTransfer(
						listener == null ? null :
						new DHTTransportProgressListener()
						{
							public void
							reportSize(
								long	size )
							{
								listener.reportSize( size );
							}
							
							public void
							reportActivity(
								String	str )
							{
								listener.reportActivity( str );
							}
							
							public void
							reportCompleteness(
								int		percent )
							{
								listener.reportCompleteness( percent );
							}
						},
						contact, 
						handler_key, 
						key, 
						data,
						timeout );
				
			}catch( Throwable e ){
				
				throw( new RuntimeException( e ));
			}
		}
		
		public byte[]
		call(
			final DHTPluginProgressListener		listener,
			byte[]								handler_key,
			byte[]								data,
			long								timeout )
		{
			DHTTransportContact contact = fixup();
			
			try{
				return( contact.getTransport().writeReadTransfer(
						listener == null ? null :
						new DHTTransportProgressListener()
						{
							public void
							reportSize(
								long	size )
							{
								listener.reportSize( size );
							}
							
							public void
							reportActivity(
								String	str )
							{
								listener.reportActivity( str );
							}
							
							public void
							reportCompleteness(
								int		percent )
							{
								listener.reportCompleteness( percent );
							}
						},
						contact, 
						handler_key, 
						data, 
						timeout ));
				
			}catch( Throwable e ){
				
				throw( new RuntimeException( e ));
			}
		}
		
		public String
		getString()
		{
			DHTTransportContact contact = fixup();
			
			return( contact.getString());	
		}
	}
}
