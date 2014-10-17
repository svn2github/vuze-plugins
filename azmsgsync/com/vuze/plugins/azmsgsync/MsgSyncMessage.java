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

import org.gudy.azureus2.core3.util.SystemTime;

public class 
MsgSyncMessage 
{
	public static final int ST_OK			= 1;
	public static final int ST_ERROR		= 2;
	
	private MsgSyncNode		node;
	private byte[]			message_id;
	private byte[]			content;
	
	private byte[]			signature;
	
	private int				age_when_received_secs;
	private long			time_received;
	
	private String			error;
	
	protected
	MsgSyncMessage(
		MsgSyncNode		_node,
		byte[]			_message_id,
		byte[]			_content,
		byte[]			_signature,
		int				_age_secs )
	{
		node		= _node;
		message_id	= _message_id;
		content		= _content;
		signature	= _signature;
		
		age_when_received_secs	= _age_secs;
		time_received			= SystemTime.getMonotonousTime();
		
		if ( content == null ){
			content = new byte[0];
		}
		
		if ( content.length > MsgSyncHandler.MAX_MESSAGE_SIZE ){
			
			content = new byte[0];
			
			setError( "Message rejected - too large (max bytes=" + MsgSyncHandler.MAX_MESSAGE_SIZE + ")" );
		}
	}
	
	public int
	getStatus()
	{
		return( error==null?ST_OK:ST_ERROR );
	}
	
	public String
	getError()
	{
		return( error );
	}
	
	protected void
	setError(
		String		_error )
	{ 
		error	= _error;
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
	
	public int
	getAgeSecs()
	{
		return((int)( age_when_received_secs + (( SystemTime.getMonotonousTime() - time_received )/1000)));
	}
	
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
}
