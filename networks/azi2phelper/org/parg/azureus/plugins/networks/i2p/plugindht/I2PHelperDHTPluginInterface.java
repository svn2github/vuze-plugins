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

import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.parg.azureus.plugins.networks.i2p.*;
import org.parg.azureus.plugins.networks.i2p.router.I2PHelperRouter;
import org.parg.azureus.plugins.networks.i2p.router.I2PHelperRouterDHT;
import org.parg.azureus.plugins.networks.i2p.vuzedht.I2PHelperAZDHT;

import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface;
import com.aelitis.azureus.plugins.dht.DHTPluginKeyStats;
import com.aelitis.azureus.plugins.dht.DHTPluginListener;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;

public class 
I2PHelperDHTPluginInterface
	implements DHTPluginInterface
{
	private I2PHelperPlugin		plugin;
	private int					dht_index;
	
	private volatile I2PHelperAZDHT		dht;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher( "I2PHelperDHTPluginInterface" );
	
	private AESemaphore			init_sem = new AESemaphore( "I2PHelperDHTPluginInterface" );
	
	private volatile TimerEventPeriodic	init_event;
	
	public 
	I2PHelperDHTPluginInterface(
		I2PHelperPlugin		_plugin,
		int					_dht_index )
	{
		plugin			= _plugin;
		dht_index		= _dht_index;
		
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
	
	public DHTPluginContact
	getLocalAddress()
	{
		Debug.out( "not imp" );
		
		return( null );
	}
	
	public DHTPluginKeyStats
	decodeStats(
		DHTPluginValue		value )
	{
		Debug.out( "not imp" );
		
		return( null );
	}

	public void
	registerHandler(
		byte[]							handler_key,
		final DHTPluginTransferHandler	handler )
	{
		Debug.out( "not imp" );
	}	
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address )
	{
		Debug.out( "not imp" );
		
		return( null );
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
		Debug.out( "not imp" );
		
		return( null );
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
			
			dht.get( key, description, max_values, timeout, exhaustive, high_priority, listener );
		
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
							
							dht_to_use.get( key, description, max_values, timeout, exhaustive, high_priority, listener );
						}
					}
				});
		}
	}
	
	public void
	put(
		byte[]						key,
		String						description,
		byte[]						value,
		byte						flags,
		DHTPluginOperationListener	listener)
	{
		Debug.out( "not imp" );
	}
	
	public void
	remove(
		byte[]						key,
		String						description,
		DHTPluginOperationListener	listener )
	{
		Debug.out( "not imp" );
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
}
