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

import java.util.Arrays;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.plugins.dht.DHTPluginContact;

public class 
MsgSyncNode 
{
	private DHTPluginContact		contact;
	private byte[]					uid;
	private byte[]					public_key;
	
	private String			contact_str;
	
	private volatile long	last_alive;
	private volatile int	fail_count;
	
	private long			last_message_timestamp;
	
	protected
	MsgSyncNode(
		DHTPluginContact		_contact,
		byte[]					_uid,
		byte[]					_public_key )
	{
		contact		= _contact;
		uid			= _uid;
		public_key	= _public_key;
		
		contact_str = MsgSyncHandler.getString( contact );
	}
	
	protected boolean
	setDetails(
		DHTPluginContact	_contact,
		byte[]				_public_key )
	{
		synchronized( this ){
			
			if ( public_key != null ){
				
				return( Arrays.equals( public_key, _public_key ));
			}
	
			contact			= _contact;
			public_key		= _public_key;
			
			contact_str = MsgSyncHandler.getString( contact );
			
			return( true );
		}
	}
	
	protected void
	setDetails(
		DHTPluginContact	_contact,
		long				_time )
	{
		synchronized( this ){
			
			contact			= _contact;
			
			contact_str 	= MsgSyncHandler.getString( contact );
			
			last_message_timestamp = _time;
		}
	}
	
	protected long
	getLatestMessageTimestamp()
	{
		synchronized( this ){
			
			return( last_message_timestamp );
		}
	}
	
	protected void
	ok()
	{
		last_alive 	= SystemTime.getMonotonousTime();
		fail_count	= 0;
	}
	
	protected long
	getLastAlive()
	{
		return( last_alive );
	}

	protected void
	failed()
	{
		fail_count++;
	}

	protected int
	getFailCount()
	{
		return( fail_count );
	}
	
	public byte[]
	getUID()
	{
		return( uid );
	}
	
	public byte[]
	getPublicKey()
	{
		return( public_key );
	}
	
	public DHTPluginContact
	getContact()
	{
		return( contact );
	}
	
	public String
	getContactAddress()
	{
		return( contact_str );
	}
	
	public String
	getName()
	{
		return( contact_str );
	}
}