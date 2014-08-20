/*
 * Created on Jul 25, 2014
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

import org.parg.azureus.plugins.networks.i2p.vuzedht.DHTAZ;

import com.aelitis.azureus.core.dht.*;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class 
I2PHelperAZDHT 
{
	public static final short		FLAG_NONE			= DHT.FLAG_NONE;			
	public static final short		FLAG_NON_ANON		= DHT.FLAG_SINGLE_VALUE;	// getters will get putter's address
	public static final short		FLAG_ANON			= DHT.FLAG_ANON;			// getters don't get putters address
	public static final short		FLAG_HIGH_PRIORITY	= 0x0200; // update to DHT.FLAG_HIGH_PRIORITY;	sometime		

	private DHTAZ		dht;
	
	public
	I2PHelperAZDHT(
		DHTAZ			_d )
	{
		dht		= _d;
	}
	
	public void
	put(
		final byte[]				key,
		String						description,
		byte[]						value,
		short						flags,
		boolean						high_priority,
		final OperationListener		listener)
	{
		if ( high_priority ){
			
			flags |= FLAG_HIGH_PRIORITY;
		}
		
		dht.getDHT().put(
			key, 
			description, 
			value, 
			flags, 
			high_priority,
			new DHTOperationListener()
			{
				private boolean	started;
				
				public void
				searching(
					DHTTransportContact	contact,
					int					level,
					int					active_searches )
				{
					if ( listener != null ){
						
						synchronized( this ){
							
							if ( started ){
								
								return;
							}
							
							started = true;
						}
						
						listener.starts( key );
					}
				}
				
				public boolean
				diversified(
					String				desc )
				{
					return( listener.diversified( key ));
				}
				
				public void
				found(
					DHTTransportContact		contact,
					boolean					is_closest )
				{
					// nada
				}
				
				public void
				read(
					DHTTransportContact		contact,
					DHTTransportValue		value )
				{
					listener.valueRead(key, new DHTContactImpl( contact ), new DHTValueImpl( value ));
				}
				
				public void
				wrote(
					DHTTransportContact		contact,
					DHTTransportValue		value )
				{
					listener.valueWritten(key, new DHTContactImpl( contact ), new DHTValueImpl( value ));
				}
				
				public void
				complete(
					boolean					timeout )
				{
					listener.complete( key, timeout );
				}
			});
	}
	
	public void
	get(
		final byte[]				key,
		String						description,
		int							max_values,
		long						timeout,
		boolean						high_priority,
		final OperationListener		listener)
	{
		short flags = FLAG_NONE;
		
		if ( high_priority ){
			
			flags |= FLAG_HIGH_PRIORITY;
		}
		
		dht.getDHT().get(
			key, 
			description, 
			flags, 
			max_values,
			timeout,
			false,	// exhaustive
			high_priority,
			new DHTOperationListener()
			{
				private boolean	started;
				
				public void
				searching(
					DHTTransportContact	contact,
					int					level,
					int					active_searches )
				{
					if ( listener != null ){
						
						synchronized( this ){
							
							if ( started ){
								
								return;
							}
							
							started = true;
						}
						
						listener.starts( key );
					}
				}
				
				public boolean
				diversified(
					String				desc )
				{
					return( listener.diversified( key ));
				}
				
				public void
				found(
					DHTTransportContact		contact,
					boolean					is_closest )
				{
					// nada
				}
				
				public void
				read(
					DHTTransportContact		contact,
					DHTTransportValue		value )
				{
					listener.valueRead(key, new DHTContactImpl( contact ), new DHTValueImpl( value ));
				}
				
				public void
				wrote(
					DHTTransportContact		contact,
					DHTTransportValue		value )
				{
					listener.valueWritten(key, new DHTContactImpl( contact ), new DHTValueImpl( value ));
				}
				
				public void
				complete(
					boolean					timeout )
				{
					listener.complete( key, timeout );
				}
			});
	}
	
	private class
	DHTContactImpl
		implements DHTContact
	{
		private DHTTransportContact		contact;
		
		private
		DHTContactImpl(
			DHTTransportContact		_c )
		{
			contact = _c;
		}
		
		public byte[]
		getID()
		{
			return( contact.getID());
		}
		
		public String
		getName()
		{
			return( contact.getName());
		}
		
		public InetSocketAddress
		getAddress()
		{
			return( contact.getAddress());
		}
		
		public byte
		getProtocolVersion()
		{
			return( contact.getProtocolVersion());
		}
		
		public boolean
		isAlive(
			long		timeout )
		{
			return( false );
		}
		
		public void
		isAlive(
			long					timeout,
			OperationListener		listener )
		{
			listener.complete( null, true );
		}
	}
	
	private class
	DHTValueImpl
		implements DHTValue
	{
		private DHTTransportValue		value;
		
		private 
		DHTValueImpl(
			DHTTransportValue		_v )
		{
			value	= _v;
		}
		
		public byte[]
		getValue()
		{
			return( value.getValue());
		}
	}
	
	public interface
	DHTContact
	{
		public byte[]
		getID();
		
		public String
		getName();
		
		public InetSocketAddress
		getAddress();
		
		public byte
		getProtocolVersion();
				
		public boolean
		isAlive(
			long		timeout );
		
		public void
		isAlive(
			long					timeout,
			OperationListener		listener );
	}
	
	public interface 
	DHTValue 
	{
		public byte[]
		getValue();
	}
	
	public interface 
	OperationListener 
	{
		public void
		starts(
			byte[]				key );
		
		public boolean
		diversified(
			byte[]				key );
		
		public void
		valueRead(
			byte[]				key,
			DHTContact			originator,
			DHTValue			value );
		
		public void
		valueWritten(
			byte[]				key,
			DHTContact			target,
			DHTValue			value );
		
		public void
		complete(
			byte[]				key,
			boolean				timeout_occurred );
	}
	
	public static class
	OperationAdapter
		implements OperationListener
	{
		@Override
		public void
		starts(
			byte[]				key )
		{
		}
		
		@Override
		public boolean
		diversified(
			byte[]				key )
		{
			return( true );
		}
		
		@Override
		public void
		valueRead(
			byte[]				key,
			DHTContact			originator,
			DHTValue			value )
		{
		}
		
		@Override
		public void
		valueWritten(
			byte[]				key,
			DHTContact			target,
			DHTValue			value )
		{
		}
		
		@Override
		public void
		complete(
			byte[]				key,
			boolean				timeout_occurred )
		{
		}
	}
}
