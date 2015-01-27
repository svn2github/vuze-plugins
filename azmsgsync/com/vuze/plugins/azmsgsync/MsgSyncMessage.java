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
	public static final byte[]	BLANK_HISTORY = {};
	
	public static final int ST_NORMAL_MESSAGE	= 1;
	public static final int ST_LOCAL_MESSAGE	= 2;
	
	private MsgSyncNode		node;
	private byte[]			message_id;
	private byte[]			content;
	
	private byte[]			signature;
	
	private int				age_when_received_secs;
	private long			time_received;
	
	private int				delivery_count;
	private int				probably_seen_count;
	private int				seen_count;
	
	private byte[]			history;
	
	private String			local_msg;
	
	protected
	MsgSyncMessage(
		MsgSyncNode		_node,
		byte[]			_message_id,
		byte[]			_content,
		byte[]			_signature,
		int				_age_secs,
		byte[]			_history )
	{
		node		= _node;
		message_id	= _message_id;
		content		= _content;
		signature	= _signature;
		history		= _history==null?BLANK_HISTORY:_history;
		
		age_when_received_secs	= _age_secs < 0 ?0:_age_secs;
		time_received			= SystemTime.getCurrentTime();		// can't use monotime here as computer sleep suspends it :(
		
		if ( content == null ){
			content = new byte[0];
		}
		
		if ( content.length > MsgSyncHandler.MAX_MESSAGE_SIZE ){
			
			content = new byte[0];
			
			setLocalMessage( "Message rejected - too large (max bytes=" + MsgSyncHandler.MAX_MESSAGE_SIZE + ")" );
		}
	}
	
	protected
	MsgSyncMessage(
		MsgSyncNode		_node,
		byte[]			_message_id,
		byte[]			_signature,
		String			_local_msg )
	{
		node		= _node;
		message_id	= _message_id;
		content		= new byte[0];
		signature	= _signature;
		history		= BLANK_HISTORY;
		
		age_when_received_secs	= 0;
		time_received			= SystemTime.getCurrentTime();
		
		local_msg	= _local_msg;
	}
	
	public int
	getMessageType()
	{
		return( local_msg==null?ST_NORMAL_MESSAGE:ST_LOCAL_MESSAGE );
	}
	
	public String
	getLocalMessage()
	{
		return( local_msg );
	}
	
	protected void
	setLocalMessage(
		String		_local_msg )
	{ 
		local_msg	= _local_msg;
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
	
	public long
	getTimestamp()
	{
		return( time_received - age_when_received_secs*1000L );
	}
	
	public int
	getAgeSecsWhenReceived()
	{
		return( age_when_received_secs );
	}
	
	public int
	getAgeSecs()
	{
		long now	=  SystemTime.getCurrentTime();
		
		long elapsed = now - time_received;
		
		if ( elapsed < 0 ){
			
				// not much we can do apart from reset things
			
			time_received = now;
			
			return( age_when_received_secs );
		}
		
		return((int)( age_when_received_secs + ( elapsed/1000 )));
	}
	
	protected byte[]
	getHistory()
	{
		return( history );
	}
	
	protected int
	getDeliveryCount()
	{
		synchronized( this ){
		
			return( delivery_count );
		}
	}
	
	protected void
	delivered()
	{
		synchronized( this ){
		
			delivery_count++;
		}
	}
	
	protected int
	getProbablySeenCount()
	{
		synchronized( this ){
		
			return( probably_seen_count );
		}
	}
	
	protected void
	probablySeen()
	{
		synchronized( this ){
		
			probably_seen_count++;
		}
	}
	
	protected int
	getSeenCount()
	{
		synchronized( this ){
		
			return( seen_count );
		}
	}
	
	protected void
	seen()
	{
		synchronized( this ){
		
			seen_count++;
		}
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
