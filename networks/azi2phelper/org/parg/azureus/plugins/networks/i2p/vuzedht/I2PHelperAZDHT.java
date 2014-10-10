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


package org.parg.azureus.plugins.networks.i2p.vuzedht;

import java.net.InetSocketAddress;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.*;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginProgressListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;

public abstract class 
I2PHelperAZDHT 
{
	public static final short		FLAG_NONE			= DHT.FLAG_NONE;			
	public static final short		FLAG_NON_ANON		= DHT.FLAG_SINGLE_VALUE;	// getters will get putter's address
	public static final short		FLAG_ANON			= DHT.FLAG_ANON;			// getters don't get putters address
	public static final short		FLAG_HIGH_PRIORITY	= DHT.FLAG_HIGH_PRIORITY;		

	public abstract DHT
	getDHT()
	
		throws Exception;
	
	public void
	put(
		final byte[]						key,
		String								description,
		byte[]								value,
		short								flags,
		boolean								high_priority,
		final DHTPluginOperationListener	listener)
	{
		if ( high_priority ){
			
			flags |= FLAG_HIGH_PRIORITY;
		}
		
		try{
			getDHT().put(
				key, 
				description, 
				value, 
				flags, 
				high_priority,
				new ListenerWrapper( key, listener ));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			listener.complete( key, false );
		}
	}
	
	public void
	get(
		final byte[]						key,
		String								description,
		short								flags,
		int									max_values,
		long								timeout,
		boolean								exhaustive,
		boolean								high_priority,
		final DHTPluginOperationListener	listener)
	{		
		if ( high_priority ){
			
			flags |= FLAG_HIGH_PRIORITY;
		}
		
		try{
			getDHT().get(
				key, 
				description, 
				flags, 
				max_values,
				timeout,
				exhaustive,
				high_priority,
				new ListenerWrapper( key, listener ));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			listener.complete( key, false );
		}
	}
	
	public void
	remove(
		final byte[]						key,
		String								description,
		final DHTPluginOperationListener	listener)
	{
		try{
			getDHT().remove(
				key, 
				description, 
				new ListenerWrapper( key, listener ));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			listener.complete( key, false );
		}
	}
	
	public void
	registerHandler(
		byte[]							handler_key,
		final DHTPluginTransferHandler	handler )
	{		
		try{
			getDHT().getTransport().registerTransferHandler( 
				handler_key,
				new DHTTransportTransferHandler()
				{
					public String
					getName()
					{
						return( handler.getName());
					}
					
					public byte[]
					handleRead(
						DHTTransportContact	originator,
						byte[]				key )
					{
						return( handler.handleRead( new DHTContactImpl( originator ), key ));
					}
					
					public byte[]
					handleWrite(
							DHTTransportContact	originator,
						byte[]				key,
						byte[]				value )
					{
						handler.handleWrite( new DHTContactImpl( originator ), key, value );
						
						return( null );
					}
				});
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address )
	{
		try{
			return( new DHTContactImpl(((DHTTransportAZ)getDHT().getTransport()).importContact(address)));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( null );
		}
	}
	
	public DHTPluginContact
	importContact(
		Map<String,Object>				map )
	{
		try{
			return( new DHTContactImpl(((DHTTransportAZ)getDHT().getTransport()).importContact(map)));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( null );
		}
	}
	
	private class
	ListenerWrapper
		implements DHTOperationListener
	{
		private final byte[]							key;
		private final DHTPluginOperationListener		listener;
		
		private boolean	started;
		
		private 
		ListenerWrapper(
			byte[]							_key,
			DHTPluginOperationListener		_listener )
		{
			key			= _key;
			listener 	= _listener;
		}
		
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
			return( listener.diversified());
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
			listener.valueRead( new DHTContactImpl( value.getOriginator()), new DHTValueImpl( value ));
		}
		
		public void
		wrote(
			DHTTransportContact		contact,
			DHTTransportValue		value )
		{
			listener.valueWritten( new DHTContactImpl( contact ), new DHTValueImpl( value ));
		}
		
		public void
		complete(
			boolean					timeout )
		{
			listener.complete( key, timeout );
		}
	};
	
	
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
		
		@Override
		public int 
		getNetwork() 
		{
			return( contact.getTransport().getNetwork());
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
		
		public Map<String, Object> 
		exportToMap()
		{
			return( contact.exportContactToMap());
		}
		
		public boolean
		isAlive(
			long		timeout )
		{
			return( false );
		}
		
		@Override
		public void isAlive(long timeout, DHTPluginOperationListener listener) {
			Debug.out( "not imp" );
		}
		
		@Override
		public boolean isOrHasBeenLocal() {
			Debug.out( "not imp" );
			return false;
		}
		
		@Override
		public Map openTunnel() {
			Debug.out( "not imp" );
			return null;
		}
		
		@Override
		public byte[] 
		read(
			final DHTPluginProgressListener 	listener,
			byte[] 								handler_key, 
			byte[] 								key, 
			long 								timeout )
		{
			try{
				return( getDHT().getTransport().readTransfer(
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
		
		@Override
		public void
		write(
			final DHTPluginProgressListener 	listener,
			byte[] 								handler_key, 
			byte[] 								key, 
			byte[]								data,
			long 								timeout )
		{
			try{
				 getDHT().getTransport().writeTransfer(
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
		
		@Override
		public byte[] 
		call(
			final DHTPluginProgressListener 	listener,
			byte[] 								handler_key, 
			byte[] 								data, 
			long 								timeout )
		{
			try{
				return( getDHT().getTransport().writeReadTransfer(
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
		
		@Override
		public DHTContact getOriginator() {
			return( new DHTContactImpl( value.getOriginator()));
		}
	}
	
	public interface
	DHTContact
		extends DHTPluginContact
	{
	}
	
	public interface 
	DHTValue 
		extends DHTPluginValue
	{
		public DHTContact
		getOriginator();
	}
	
	public interface
	OperationListener
		extends DHTPluginOperationListener
	{
		public void
		valueRead(
			DHTContact			originator,
			DHTValue			value );
		
		public void
		valueWritten(
			DHTContact			target,
			DHTValue			value );
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
		diversified()
		{
			return( true );
		}
		
		@Override
		public void
		valueRead(
			DHTPluginContact	originator,
			DHTPluginValue		value )
		{
			valueRead((DHTContact)originator, (DHTValue)value );
		}
		
		@Override
		public void
		valueRead(
			DHTContact			originator,
			DHTValue			value )
		{
		}
		
		@Override
		public void
		valueWritten(
			DHTPluginContact	target,
			DHTPluginValue		value )
		{
			valueWritten((DHTContact)target, (DHTValue)value );
		}
		
		@Override
		public void
		valueWritten(
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
