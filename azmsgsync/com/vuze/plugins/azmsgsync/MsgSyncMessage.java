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

public class 
MsgSyncMessage 
{
	public final int ST_PENDING		= 5;
	
	private MsgSyncNode		node;
	private byte[]			message_id;
	private byte[]			content;
	
	private byte[]			signature;
	
	protected
	MsgSyncMessage(
		MsgSyncNode		_node,
		byte[]			_message_id,
		byte[]			_content,
		byte[]			_signature )
	{
		node		= _node;
		message_id	= _message_id;
		content		= _content;
		signature	= _signature;
	}
	
	public MsgSyncNode
	getNode()
	{
		return( node );
	}
	
	public byte[]
	getID()
	{
		return( message_id );
	}
	
	/*
	public int
	getStatus()
	{
		
	}
	*/
	
	public byte[]
	getContent()
	{
		return( content );
	}
	
	public byte[]
	getSignature()
	{
		return( signature );
	}
	
	/*
	public long
	getOriginatorTimestamp()
	{
		
	}
	
	public MsgSyncMessage
	getPreviousMessage(){
		
		
	}
	
	public MsgSyncMessage
	getNextMessage()
	{
		
	}
	*/
}
