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

import com.aelitis.azureus.plugins.dht.DHTPluginContact;

public class 
MsgSyncNode 
{
	private DHTPluginContact		contact;
	private byte[]					uid;
	private byte[]					public_key;
	
	protected
	MsgSyncNode(
		DHTPluginContact		_contact,
		byte[]					_uid,
		byte[]					_public_key )
	{
		contact		= _contact;
		uid			= _uid;
		public_key	= _public_key;
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
	getName()
	{
		return( contact.toString());
	}
}
