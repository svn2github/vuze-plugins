/*
 * Created on Apr 17, 2014
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.i2p.data.Destination;
import net.i2p.data.Hash;

import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;

public interface 
I2PHelperDHT 
{
	public NodeInfo
	getNodeInfo(
		byte[]		hash );
	
	public NodeInfo
	heardAbout(
		Map			map );
	
	public void
	ping(
		Destination		destination,
		int				port );
	
	public void
	ping(
		NodeInfo		node );
	
	public Collection<Hash> 
	getPeersAndNoAnnounce(
		byte[] 			ih, 
		int 			max, 
		long			maxWait, 
		int 			annMax, 
		long 			annMaxWait );
		
	public Collection<Hash> 
	getPeersAndAnnounce(
		byte[] 			ih, 
		int 			max, 
		long			maxWait, 
		int 			annMax, 
		long 			annMaxWait );
	
	public void
	requestBootstrap();
	
	/**
	 * Used by the bootstrap server
	 * @param number
	 * @return
	 */
	
	public List<NodeInfo>
	getNodesForBootstrap(
		int			number );
	
	public void
	stop();
	
	public void
	print();
	
	public String
	renderStatusHTML();
	
	public String
	getStats();
}