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

import net.i2p.data.Base32;
import net.i2p.data.Hash;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;


public class 
I2PHelperTracker 
{
	private I2PHelperRouter				router;
	private I2PHelperAdapter			adapter;
	
	private I2PDHTTrackerPlugin			tracker;
	
	protected
	I2PHelperTracker(
		I2PHelperAdapter	_adapter,
		I2PHelperRouter		_router )
	{
		adapter	= _adapter;
		router	= _router;
		
		PluginInterface	pi = adapter.getPluginInterface();
		
		if ( pi != null ){
			
			tracker = new I2PDHTTrackerPlugin( adapter, router );
		}
	}
	
	protected I2PDHTTrackerPlugin
	getTrackerPlugin()
	{
		return( tracker );
	}
	
	protected void
	get(
		byte[]						hash,
		String						reason,
		byte						flags,
		int							num_want,
		long						timeout,
		I2PHelperDHTListener		listener )
	{
		try{
			I2PHelperDHT dht = router.selectDHT().getDHT(true);
			
			dht.get( hash, reason, flags, num_want, timeout, listener );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	put(
		byte[]						hash,
		String						reason,
		byte						flags,
		I2PHelperDHTListener		listener )
	{
		try{
			I2PHelperDHT dht = router.selectDHT().getDHT(true);
	
			dht.put( hash, reason, flags, listener );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	
	
	protected void
	get(
		byte[]		torrent_hash )
	{	
		try{
			int	num_want 		= 30;
			int	get_timeout		= 5*60*1000;
			int	num_put			= 1;
			int	put_timeout		= 3*60*1000;
			
			long	start = SystemTime.getMonotonousTime();
			
			I2PHelperDHT dht = router.selectDHT().getDHT(true);
	
			Collection<Hash> peer_hashes = dht.getPeersAndNoAnnounce( torrent_hash, num_want, get_timeout, num_put, put_timeout );
			
				// Note that we can get duplicates here as use the target node as the originator node (don't actually know the originator in I2P DHT...)
			
			adapter.log( "get -> " + peer_hashes.size() + ", elapsed=" + (SystemTime.getMonotonousTime() - start ));
			
			for ( Hash hash: peer_hashes ){
				
				adapter.log( "    " + Base32.encode( hash.getData()) + ".b32.i2p" );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}	
	}
	
	protected void
	put(
		byte[]		torrent_hash )
	{	
		try{
			int	num_want 		= 30;
			int	get_timeout		= 5*60*1000;
			int	num_put			= 1;
			int	put_timeout		= 3*60*1000;
			
			long	start = SystemTime.getMonotonousTime();
			
			I2PHelperDHT dht = router.selectDHT().getDHT(true);
	
			Collection<Hash> peer_hashes = dht.getPeersAndAnnounce( torrent_hash, num_want, get_timeout, num_put, put_timeout );
			
			adapter.log( "put -> " + peer_hashes.size() + ", elapsed=" + (SystemTime.getMonotonousTime() - start ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public void
	destroy()
	{
		if ( tracker != null ){
			
			tracker.unload();
		}
	}
}
