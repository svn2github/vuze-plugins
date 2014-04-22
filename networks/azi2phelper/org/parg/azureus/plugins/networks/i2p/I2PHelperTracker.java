/*
 * Created on Mar 17, 2014
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

import java.util.*;

import net.i2p.data.Hash;

import org.gudy.azureus2.core3.util.SystemTime;


public class 
I2PHelperTracker 
{
	private I2PHelperDHT			dht;
	
	protected
	I2PHelperTracker(
		I2PHelperDHT		_dht )
	{
		dht		= _dht;
	}
	
	
	protected void
	get(
		byte[]		torrent_hash )
	{		
		int	num_want 		= 30;
		int	get_timeout		= 5*60*1000;
		int	num_put			= 1;
		int	put_timeout		= 3*60*1000;
		
		long	start = SystemTime.getMonotonousTime();
		
		Collection<Hash> peer_hashes = dht.getPeersAndNoAnnounce( torrent_hash, num_want, get_timeout, num_put, put_timeout );
		
		System.out.println( "get -> " + peer_hashes.size() + ", elapsed=" + (SystemTime.getMonotonousTime() - start ));
	}
	
	protected void
	put(
		byte[]		torrent_hash )
	{		
		int	num_want 		= 30;
		int	get_timeout		= 5*60*1000;
		int	num_put			= 1;
		int	put_timeout		= 3*60*1000;
		
		long	start = SystemTime.getMonotonousTime();
		
		Collection<Hash> peer_hashes = dht.getPeersAndAnnounce( torrent_hash, num_want, get_timeout, num_put, put_timeout );
		
		System.out.println( "put -> " + peer_hashes.size() + ", elapsed=" + (SystemTime.getMonotonousTime() - start ));
	}
}
