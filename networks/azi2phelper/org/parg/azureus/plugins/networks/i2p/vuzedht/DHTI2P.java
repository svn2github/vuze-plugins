/*
 * Created on Apr 16, 2014
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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.i2p.client.I2PSession;
import net.i2p.data.Destination;
import net.i2p.data.Hash;

import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.parg.azureus.plugins.networks.i2p.I2PHelperDHT;
import org.parg.azureus.plugins.networks.i2p.dht.NID;
import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

public class 
DHTI2P
	implements DHTLogger, I2PHelperDHT
{
	public static final int		DHT_NETWORK		= 10;
	
	private DHT 						dht;
	private DHTTransportI2P				transport;
	private DHTPluginStorageManager 	storage_manager;
	
	public 
	DHTI2P(
		File			dir,
		I2PSession		session,
		int				port,
		NID				nid )
	{
		File storage_dir = new File( dir, "dhtdata");
		
		if ( !storage_dir.isDirectory()){
		
			storage_dir.mkdirs();
		}
		
		storage_manager = new DHTPluginStorageManager( DHT_NETWORK, this, storage_dir );

		transport = new DHTTransportI2P( session, port, nid );
				
		Properties	props = new Properties();
		
			// need to check out the republish / cache forward logic required
		
		props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer( 1*60*60*1000 ));
		
		dht = DHTFactory.create( 
				transport, 
				props,
				storage_manager,
				null,
				this );
		
		storage_manager.importContacts( dht );
		
		dht.integrate( false );
		
		final int timer_period 	= 15*1000;
		final int save_period	= 2*60*1000;
		final int save_ticks	= save_period / timer_period;
		
		SimpleTimer.addPeriodicEvent(
			"DHTI2P:checker",
			timer_period,
			new TimerEventPerformer() {
				
				private int	tick_count = 0;
				
				@Override
				public void
				perform(
					TimerEvent event) 
				{
					tick_count++;
					
					if ( tick_count % save_ticks == 0 ){
						
						storage_manager.exportContacts( dht );
					}
				}
			});
	}
	
	public void
	log(
		String	str )
	{
		System.out.println( str );
	}
		
	public void
	log(
		Throwable	e )
	{
		e.printStackTrace();
	}
	
	public void
	log(
		int		log_type,
		String	str )
	{
		System.out.println( str );
	}
	
	public boolean
	isEnabled(
		int	log_type )
	{
		return( true );
	}
			
	public PluginInterface
	getPluginInterface()
	{
		return( null );
	}
	
	public NodeInfo
	getNodeInfo(
		byte[]		hash )
	{
		return( null );
	}
	
	public NodeInfo
	heardAbout(
		Map			map )
	{
		return( null );
	}
	
	public void
	ping(
		Destination		destination,
		int				port )
	{
		
	}
	
	public void
	ping(
		NodeInfo		node )
	{
		
	}
	
	public Collection<Hash> 
	getPeersAndNoAnnounce(
		byte[] 			ih, 
		int 			max, 
		long			maxWait, 
		int 			annMax, 
		long 			annMaxWait )
	{
		return( null );
	}
	
	public void
	setBootstrapNode(
		NodeInfo		node )
	{
	
	}
	
	public void
	requestBootstrap()
	{
		
	}
	
	public List<NodeInfo>
	getNodesForBootstrap(
		int			number )
	{
		return( null );
	}
	
	public void
	stop()
	{
		storage_manager.exportContacts( dht );
		
		dht.destroy();
	}
	
	public void
	print()
	{
		dht.print( true );
		
		System.out.println( transport.getStats().getString());
	}
	
	public String
	renderStatusHTML()
	{
		return( "derp" );
	}
	
	public String
	getStats()
	{
		return( "derp" );
	}
}
