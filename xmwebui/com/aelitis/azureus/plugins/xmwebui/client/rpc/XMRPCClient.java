/*
 * Created on Feb 28, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.xmwebui.client.rpc;


import org.json.simple.JSONObject;

public interface 
XMRPCClient 
{
	//public static final String PAIRING_URL 	= "https://pair.vuze.com/";
	public static final String PAIRING_URL 	= "http://127.0.0.1:4080/";
	
	public static final String SID			= "xmwebui";
	
	public JSONObject
	call(
		JSONObject		request )
	
		throws XMRPCClientException;
	
	public void
	destroy();
}
