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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.i2p.client.I2PSession;
import net.i2p.data.Destination;
import net.i2p.data.Hash;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.ByteFormatter;
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
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

public class 
DHTI2P
	implements DHTLogger, I2PHelperDHT
{
	public static final int		DHT_NETWORK		= 10;
	
	private static final int	REQUEST_TIMEOUT	= 45*1000;
			
	private DHT 						dht;
	private DHTTransportI2P				transport;
	private DHTPluginStorageManager 	storage_manager;
	
	public 
	DHTI2P(
		File			dir,
		I2PSession		session,
		NodeInfo		my_node,
		NodeInfo		boot_node )
	{
		File storage_dir = new File( dir, "dhtdata");
		
		if ( !storage_dir.isDirectory()){
		
			storage_dir.mkdirs();
		}
		
		storage_manager = new DHTPluginStorageManager( DHT_NETWORK, this, storage_dir );

		transport = new DHTTransportI2P( session, my_node,REQUEST_TIMEOUT );
				
		Properties	props = new Properties();
		
			// need to check out the republish / cache forward logic required
		
		props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, 	new Integer( 0 ));	// disabled :(
		props.put( DHT.PR_ORIGINAL_REPUBLISH_INTERVAL, 	new Integer( 40*60*1000 ));
		props.put( DHT.PR_ENCODE_KEYS, 					0 );		// raw keys, no sha1'ing them

		/*
		int		K 		= getProp( PR_CONTACTS_PER_NODE, 			DHTControl.K_DEFAULT );
		int		B 		= getProp( PR_NODE_SPLIT_FACTOR, 			DHTControl.B_DEFAULT );
		int		max_r	= getProp( PR_MAX_REPLACEMENTS_PER_NODE, 	DHTControl.MAX_REP_PER_NODE_DEFAULT );
		int		s_conc 	= getProp( PR_SEARCH_CONCURRENCY, 			DHTControl.SEARCH_CONCURRENCY_DEFAULT );
		int		l_conc 	= getProp( PR_LOOKUP_CONCURRENCY, 			DHTControl.LOOKUP_CONCURRENCY_DEFAULT );
		int		o_rep 	= getProp( PR_ORIGINAL_REPUBLISH_INTERVAL, 	DHTControl.ORIGINAL_REPUBLISH_INTERVAL_DEFAULT );
		int		c_rep 	= getProp( PR_CACHE_REPUBLISH_INTERVAL, 	DHTControl.CACHE_REPUBLISH_INTERVAL_DEFAULT );
		*/
		
		dht = DHTFactory.create( 
				transport, 
				props,
				storage_manager,
				null,
				this );
		
		storage_manager.importContacts( dht );
		
		DHTTransportContactI2P boot_contact = boot_node==null?null:transport.importContact( boot_node, true );
		
		dht.integrate( false );
		
		if ( boot_contact != null ){
		
			boot_contact.remove();
		}
			
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
		final Collection<Hash> result = new ArrayList<Hash>();
		
		final AESemaphore sem = new AESemaphore( "" );
		
		dht.get(	ih,
					"Get for " + ByteFormatter.encodeString( ih ),
					DHT.FLAG_NONE,
					max,
					maxWait,
					false,
					false,
					new DHTOperationListener() {
						
						@Override
						public void wrote(DHTTransportContact contact, DHTTransportValue value) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void searching(DHTTransportContact contact, int level,
								int active_searches) {
							
							System.out.println( "get - searching " + contact.getName() + ", level=" + level );

						}
						
						@Override
						public void read(DHTTransportContact contact, DHTTransportValue value) {
							// TODO Auto-generated method stub
							
							result.add( new Hash( value.getValue()));
						}
						
						@Override
						public void found(DHTTransportContact contact, boolean is_closest) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public boolean diversified(String desc) {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public void complete(boolean timeout) {
							// TODO Auto-generated method stub
							sem.release();
						}
					});
			
		sem.reserve();
		
		return( result );
	}
	
	public Collection<Hash> 
	getPeersAndAnnounce(
		byte[] 			ih, 
		int 			max, 
		long			maxWait, 
		int 			annMax, 
		long 			annMaxWait )
	{
		final Collection<Hash> result = new ArrayList<Hash>();
		
		final AESemaphore sem = new AESemaphore( "" );
		
		dht.put(	ih,
					"Put for " + ByteFormatter.encodeString( ih ),
					new byte[1],
					DHT.FLAG_NONE,
					new DHTOperationListener() {
						
						@Override
						public void wrote(DHTTransportContact contact, DHTTransportValue value) {
							System.out.println( "put - wrote to " + contact.getName());

							result.add(((DHTTransportContactI2P)contact).getNode().getHash());
						}
						
						@Override
						public void searching(DHTTransportContact contact, int level,
								int active_searches) {
						
							System.out.println( "put - searching " + contact.getName() + ", level=" + level );
						}
						
						@Override
						public void read(DHTTransportContact contact, DHTTransportValue value) {
							
							
							
						}
						
						@Override
						public void found(DHTTransportContact contact, boolean is_closest) {
							
							System.out.println( "put - found " + contact.getName());
						}
						
						@Override
						public boolean diversified(String desc) {
						
							return false;
						}
						
						@Override
						public void complete(boolean timeout) {
						
							sem.release();
						}
					});
			
		sem.reserve();
		
		return( result );
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
		
		transport.destroy();
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
