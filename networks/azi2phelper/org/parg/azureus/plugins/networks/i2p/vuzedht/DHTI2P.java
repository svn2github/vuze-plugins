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
import java.util.*;

import net.i2p.client.I2PSession;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.data.Hash;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginInterface;
import org.parg.azureus.plugins.networks.i2p.I2PHelperDHT;
import org.parg.azureus.plugins.networks.i2p.I2PHelperAdapter;
import org.parg.azureus.plugins.networks.i2p.I2PHelperDHTListener;
import org.parg.azureus.plugins.networks.i2p.router.I2PHelperRouter;
import org.parg.azureus.plugins.networks.i2p.snarkdht.NID;
import org.parg.azureus.plugins.networks.i2p.snarkdht.NodeInfo;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.router.DHTRouterContact;
import com.aelitis.azureus.core.dht.router.DHTRouterStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

public class 
DHTI2P
	implements DHTLogger, I2PHelperDHT, DHTI2PAdapter
{
		
		// increased as snark 0.9.13-2 increased expiry to 3 hours along with num-stores to 4. given that
		// we store at 20 it seems reasonable to bump this up. Remember though that snark re-announces fairly
		// frequently whereas we only re-announce if things change
	
	public static final int		REPUBLISH_PERIOD	= (int)( 1.5*60*60*1000 );		// 40*60*1000;
		
	private final File					dir;
	private final int					dht_index;
	private DHT 						dht;
	private DHTTransportI2P				transport;
	private DHTPluginStorageManager 	storage_manager;
	
	private I2PHelperAdapter			adapter;
	
	private NodeInfo					bootstrap_node;
	
	private I2PHelperAZDHT			az_dht_helper;
	private DHTAZ					az_dht;
	
	private TimerEventPeriodic		timer;
	
	final int timer_period 			= 15*1000;
	final int save_period			= 2*60*1000;
	final int save_ticks			= save_period / timer_period;
	final int cache_check_period	= 1*60*1000;
	final int cache_check_ticks		= cache_check_period / timer_period;

	final int get_cache_expiry		= 1*60*1000;
	
	private int		bootstrap_check_tick_count	= 1;
	private boolean	force_bootstrap;
	private long 	next_bootstrap;
	private int		consec_bootstraps;

	private int refresh_ping_ok;
	private int refresh_ping_fail;
	private int refresh_find_node_ok;
	private int refresh_find_node_fail;
	
	private Map<String,GetCacheEntry>	get_cache = new HashMap<String, GetCacheEntry>();
	
	private NodeInfo	my_node;
	private String		my_address;
	
	private boolean					destroyed;
	
	public 
	DHTI2P(
		File				_dir,
		int					_dht_index,
		I2PSession			session,
		NodeInfo			_my_node,
		NodeInfo			boot_node,
		I2PHelperAdapter	_adapter )
	{
		dir			= _dir;
		dht_index	= _dht_index;
		my_node		= _my_node;
		adapter		= _adapter;
		
		File storage_dir = new File( dir, "dhtdata" + (dht_index==0?"":String.valueOf(dht_index)));
		
		if ( !storage_dir.isDirectory()){
		
			storage_dir.mkdirs();
		}
	
		my_address = Base32.encode( my_node.getHash().getData()) + ".b32.i2p";
		
		bootstrap_node	= boot_node;
		
		storage_manager = new DHTPluginStorageManager( DHTUtilsI2P.DHT_NETWORK, this, storage_dir );

		transport = new DHTTransportI2P( this, session, my_node, DHTUtilsI2P.REQUEST_TIMEOUT );
				
		Properties	props = new Properties();
		
			// need to check out the republish / cache forward logic required
		
		props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, 	new Integer( 0 ));	// disabled :(
		props.put( DHT.PR_ORIGINAL_REPUBLISH_INTERVAL, 	new Integer( REPUBLISH_PERIOD ));
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
		
		az_dht = new DHTAZ( this, adapter );
		
		az_dht_helper = new DHTAZFull( az_dht );
		
		storage_manager.importContacts( dht );
		
		DHTTransportContactI2P boot_contact = boot_node==null?null:transport.importContact( boot_node, true );
		
		dht.integrate( false );
		
		if ( boot_contact != null ){
		
			boot_contact.remove();
		}
					
		timer = SimpleTimer.addPeriodicEvent(
			"DHTI2P:checker",
			timer_period,
			new TimerEventPerformer() {
				
				private int	tick_count = 0;
				
				private volatile boolean bootstrapping;
				
				private boolean	seeded;
				
				@Override
				public void
				perform(
					TimerEvent event) 
				{
					if ( destroyed ){
						
						timer.cancel();
						
						return;
					}
					
					tick_count++;
					
					if ( !seeded ){
					
						if ( dht.getControl().isSeeded()){
					
							seeded = true;
							
							az_dht.setSeeded();
						}
					}
					
					if ( tick_count % cache_check_ticks == 0 ){
						
						checkCache();
					}

					if ( tick_count % save_ticks == 0 ){
						
						storage_manager.exportContacts( dht );
					}
					
					if ( tick_count % bootstrap_check_tick_count == 0 ){
					
						if ( !bootstrapping ){
							
							bootstrapping = true;
							
							new AEThread2( "I2P:bootcheck" )
							{
								public void
								run()
								{
									try{
										checkForBootstrap();
										
									}catch( Throwable e ){
										
									}finally{
									
										bootstrapping = false;
									}
								}
							}.start();
						}
					}
				}
			});
	}
	
	public void
	log(
		String	str )
	{
		adapter.log( str );
	}
		
	public void
	log(
		Throwable	e )
	{
		adapter.log( Debug.getNestedExceptionMessage(e));
	}
	
	public void
	log(
		int		log_type,
		String	str )
	{
		adapter.log( str );
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
		// this currently prevents the speed-tester from being created (which is what we want) so
		// if this gets changed that will need to be prevented by another means
		return( null );
	}
	
	public String
	getLocalAddress()
	{
		return( my_address );
	}
	
	protected File
	getDir()
	{
		return( dir );
	}
	
	public I2PHelperAZDHT
	getHelperAZDHT()
	{
		return( az_dht_helper );
	}
	
	public DHTAZ
	getAZDHT()
	{
		return( az_dht );
	}
	
	public int
	getQueryPort()
	{
		return( transport.getPort());
	}
	
	public int
	getReplyPort()
	{
		return( transport.getReplyPort());
	}
	
	public DHT
	getDHT()
	{
		return( dht );
	}
	
	public int
	getDHTIndex()
	{
		return( dht_index );
	}
	
	public DHTTransportI2P
	getTransport()
	{
		return( transport );
	}
	
	public void
	contactAlive(
		DHTTransportContactI2P		contact )
	{
		adapter.contactAlive( this, contact );
	}
	
	/*
		many of the methods below are test functions from when snark + vuze DHTs were both supported
	 */
	
	public NodeInfo
	getNodeInfo(
		byte[]		hash )
	{
		log( "getNodeInfo not supported" );
		
		return( null );
	}
	
	public NodeInfo
	heardAbout(
		Map			map )
	{
    	try{
	    	byte[]	nid_bytes 	= (byte[])map.get( "n" );
	    	int		port 		= ((Number)map.get( "p" )).intValue();
	    	byte[]	dest_bytes	= (byte[])map.get( "d" );
	    	
	    	NID nid = new NID( nid_bytes );
	    	
	    	Destination destination = new Destination();
	    
	    	destination.fromByteArray( dest_bytes );
	    	
	    	NodeInfo ni = new NodeInfo( nid, destination, port );
	    	
	    	transport.importContact( ni, false );
	    	
	    	return( ni );
	    	
    	}catch( Throwable e ){
    		
    		return( null );
    	}
	}
	
	public void
	heardAbout(
		NodeInfo			ni )
	{
		transport.importContact( ni, false );
	}
	
	public void
	ping(
		Destination		destination,
		int				port,
		boolean			az )
	{
		if ( az ){
			
			DHTTransportContactI2P contact = 
				new DHTTransportContactI2P( 
						transport, 
						new NodeInfo( destination, port ), 
						DHTUtilsI2P.PROTOCOL_VERSION,
						0,
						0,
						(byte)0 );
			
			az_dht.ping( contact );
			
		}else{
		
			transport.sendPing( destination, port );
		}
	}
	
	public void
	findNode(
		Destination		destination,
		int				port,
		byte[]			id )
	{
		DHTTransportContactI2P contact = 
				new DHTTransportContactI2P( 
						transport, 
						new NodeInfo( destination, port ), 
						DHTUtilsI2P.PROTOCOL_VERSION,
						0,
						0,
						(byte)0 );
			
		az_dht.findNode( contact, id );
	}
	
	public void
	findValue(
		Destination		destination,
		int				port,
		byte[]			id )
	{
		DHTTransportContactI2P contact = 
				new DHTTransportContactI2P( 
						transport, 
						new NodeInfo( destination, port ), 
						DHTUtilsI2P.PROTOCOL_VERSION,
						0,
						0,
						(byte)0 );
			
		az_dht.findValue( contact, id );
	}
	
	public void
	store(
		Destination		destination,
		int				port,
		byte[]			key,
		byte[]			value )
	{
		DHTTransportContactI2P contact = 
				new DHTTransportContactI2P( 
						transport, 
						new NodeInfo( destination, port ), 
						DHTUtilsI2P.PROTOCOL_VERSION,
						0,
						0,
						(byte)0 );
			
		az_dht.store( contact, key, value );	
	}
	
	public void
	ping(
		NodeInfo		node )
	{
		log( "ping not supported" );
	}
	
	private void
	checkCache()
	{
		synchronized( get_cache ){
			
			if ( get_cache.size() > 0 ){
			
				long now = SystemTime.getMonotonousTime();
				
				Iterator<GetCacheEntry>	it = get_cache.values().iterator();
				
				while( it.hasNext()){
					
					GetCacheEntry entry = it.next();
					
					if ( entry.isComplete() && now - entry.getCreateTime() > get_cache_expiry ){
						
						it.remove();
					}
				}
			}
		}
	}
	
	public boolean 
	hasLocalKey(
		byte[] hash) 
	{
		return( dht.getLocalValue( hash ) != null );
	}
	
	public void
	get(
		byte[] 					ih,
		String					reason,
		short					flags,
		int 					max, 
		long					maxWait,
		I2PHelperDHTListener	listener )
	{		
		flags |= I2PHelperAZDHT.FLAG_HIGH_PRIORITY;
		
		String key = ByteFormatter.encodeString( ih ) + "/" + max + "/" + maxWait;
		
		GetCacheEntry 	cache_entry;
		
		synchronized( get_cache ){
			
			cache_entry = get_cache.get( key );
			
			if ( cache_entry != null && cache_entry.isComplete()){
				
				if ( SystemTime.getMonotonousTime() - cache_entry.getCreateTime() > get_cache_expiry ){
					
					cache_entry = null;
				}
			}
			
			if ( cache_entry == null ){
				
				cache_entry = new GetCacheEntry( listener );
				
				get_cache.put( key, cache_entry );
				
			}else{
				
				cache_entry.addListener( listener );
				
				return;
			}
		}
		
		dht.get(	ih,
					reason + " for " + ByteFormatter.encodeString( ih ),
					flags,
					max,
					maxWait,
					false,
					true,		// high priority
					cache_entry );
	}
	
	public void
	put(
		byte[] 						ih,
		String						reason,
		short						flags,
		final I2PHelperDHTListener	listener )
	{
		flags |= I2PHelperAZDHT.FLAG_HIGH_PRIORITY;

		dht.put(	ih,
					reason + " for " + ByteFormatter.encodeString( ih ),
					new byte[1],
					flags,
					new DHTOperationAdapter() 
					{	
						public void 
						searching(
							DHTTransportContact 	contact, 
							int 					level,
							int 					active_searches )
						{
							
							listener.searching( contact.getName());
						}
						
						public void 
						complete(
							boolean timeout ) 
						{
							listener.complete( timeout );
						}
					});
	}
	
	public void
	remove(
		byte[] 						ih,
		String						reason,
		final I2PHelperDHTListener	listener )
	{
		dht.remove(	ih,
					reason + " for " + ByteFormatter.encodeString( ih ),
					new DHTOperationAdapter() 
					{	
						public void 
						searching(
							DHTTransportContact 	contact, 
							int 					level,
							int 					active_searches )
						{
							
							listener.searching( contact.getName());
						}
						
						public void 
						complete(
							boolean timeout ) 
						{
							listener.complete( timeout );
						}
					});
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
					I2PHelperAZDHT.FLAG_HIGH_PRIORITY,
					max,
					maxWait,
					false,
					true,		// high priority
					new DHTOperationAdapter() 
					{	
						public void 
						searching(
							DHTTransportContact 	contact, 
							int 					level,
							int 					active_searches) {
							
							log( "get - searching " + contact.getName() + ", level=" + level );
						}
						
						public void 
						read(
							DHTTransportContact 	contact, 
							DHTTransportValue 		value) {
							
							result.add( new Hash( value.getValue()));
						}
						
						public void 
						complete(
							boolean timeout ) 
						{
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
					I2PHelperAZDHT.FLAG_HIGH_PRIORITY,
					new DHTOperationAdapter() {
						
						public void 
						wrote(
							DHTTransportContact 	contact, 
							DHTTransportValue 		value ) 
						{
							log( "put - wrote to " + contact.getName());

							result.add(((DHTTransportContactI2P)contact).getNode().getHash());
						}
						
						public void 
						searching(
							DHTTransportContact contact, 
							int 				level,
							int 				active_searches ) 
						{
							log( "put - searching " + contact.getName() + ", level=" + level );
						}
						
			
						public void 
						found(
							DHTTransportContact contact, 
							boolean 			is_closest )
						{
							
							log( "put - found " + contact.getName());
						}
						
						public void 
						complete(
							boolean timeout ) 
						{
							sem.release();
						}
					});
			
		sem.reserve();
		
		return( result );
	}
	
	public void
	requestBootstrap()
	{
		force_bootstrap = true;

		if ( bootstrap_node == null ){
			
			log( "No bootstrap node" );
			
		}else{
			
			log( "Importing bootstrap node and integrating" );
			
			DHTTransportContactI2P boot_contact = transport.importContact( bootstrap_node, true );
		
			dht.integrate( false );
				
			boot_contact.remove();
		}
	}
	
	private void
	checkForBootstrap()
	{		
        int	live_node_count = 0;           
        
        try{  
        	DHTRouter router = dht.getRouter();
        	
    		List<DHTRouterContact> r_contacts = router.getAllContacts();

            int	all_nodes_count = r_contacts.size();

           	boolean 	all_failing 	= true;

           	List<NodeInfo>	live_nodes 		= new ArrayList<NodeInfo>( all_nodes_count );
           	List<NodeInfo>	unknown_nodes 	= new ArrayList<NodeInfo>( all_nodes_count );

           	Iterator<DHTRouterContact>	rc_it = r_contacts.iterator();
           	
    		while( rc_it.hasNext()){
    			
    			DHTRouterContact r_contact = rc_it.next();

       			if ( router.isID( r_contact.getID())){
       				
       				rc_it.remove();
       				
       				all_nodes_count--;
       				
       				continue;
       			}

    			DHTTransportContactI2P t_cn = (DHTTransportContactI2P)((DHTControlContact)r_contact.getAttachment()).getTransportContact();
    			
     			NodeInfo node = t_cn.getNode();

           		NID nid = node.getNID();

    			if ( 	bootstrap_node != null && 
    					( node == bootstrap_node || nid.equals( bootstrap_node.getNID()))){
    				
    				router.contactDead( nid.getData(), true );
    				
    				all_nodes_count--;
    				
    				rc_it.remove();
    				
    				continue;
    			} 	
        		        		
        		if ( !r_contact.isFailing()){
        			
        			all_failing = false;
        			
        			if ( r_contact.getTimeAlive() > 0 ){
        				
        				live_nodes.add( node );
        				
        			}else{
        				
        				unknown_nodes.add( node );
        			}
        		}
        	}
        	
        	live_node_count = live_nodes.size();
        	
        	long now = SystemTime.getMonotonousTime();
        	
        	if ( all_nodes_count < 20 || all_failing || force_bootstrap ){
        		
        			// not enough nodes, see if we can grab some more from most recently heard from node
        			// track to ensure we don't keep hitting the same most recent one...
        			// boostrap if screwed
        		
        		if ( 	force_bootstrap || 
        					(( all_failing || all_nodes_count < 5 ) && 
        					bootstrap_node != null &&
        					( next_bootstrap == 0 || ( now > next_bootstrap )))){
        			
        			if ( force_bootstrap ){
        				        				
        				consec_bootstraps 		= 0;
        			}
        			
        			log( "Bootstrapping..." );
        			    
        			boolean	try_external = false;
        			
        			if ( transport.lookupDest( bootstrap_node )){

        				if ( destroyed ){
        					
        					return;
        				}
        				
            			int delay = 1*60*1000;
            			
            			for (int i=0;i<consec_bootstraps;i++){
            				
            				delay *= 2;
            				
            				if ( delay > 10*60*1000 ){
            				
            					delay = 10*60*1000;
            					
            					break;
            				}
            			}
            			
            			next_bootstrap = now + delay;
            			
            			consec_bootstraps++;
             			
            			if ( transport.sendFindNode( bootstrap_node, RandomUtils.nextSecureHash())){
    						
    						log( "Bootstrap worked" );
    							
    					}else{
    						try_external = true;
    						
    						log( "Bootstrap failed" );
						}
        			}else{
        				
        				try_external = true;
        				
        				log( "Bootstrap not resolved" );
        			}
        			
        			if ( try_external ){
        				    				
        				adapter.tryExternalBootstrap( this, force_bootstrap );
        			}
        			
        			force_bootstrap = false;
        			
        		}else{
        			
        				// try and grab some more nodes from existing non-dead ones
        			            			
        			List<NodeInfo> nodes_to_use = live_node_count>0?live_nodes:unknown_nodes;
        			
        			if ( nodes_to_use.size() > 0 ){
        				
        				if ( destroyed ){
        					
        					return;
        				}
        				
        				NodeInfo node = nodes_to_use.get( RandomUtils.nextInt( nodes_to_use.size()));
        				
        				if ( transport.sendFindNode( node, RandomUtils.nextSecureHash())){
									
							refresh_find_node_ok++;
										
						}else{
										
							refresh_find_node_fail++;
						}
        			}
        		}
        	}else{
        		
        		consec_bootstraps = 0;
        		
            	Collections.shuffle( r_contacts );
            	
            	Collections.sort(
            		r_contacts,
            		new Comparator<DHTRouterContact>()
            		{
            			public int 
            			compare(
            				DHTRouterContact n1, 
            				DHTRouterContact n2) 
            			{
            					// failing nodes get lower attention
            				
            				            				
               				boolean n1_fail = n1.isFailing();
               				boolean n2_fail = n2.isFailing();
            				
               				if ( n1_fail && n2_fail ){
               					return( 0 );
               				}else if ( n1_fail ){
               					return( 1 );
               				}else if ( n2_fail ){
               					return( -1 );
               				}
               				
              				long n1_alive = n1.getTimeAlive();
              				long n2_alive = n2.getTimeAlive();
              				
              				if ( n1_alive == 0 && n2_alive == 0 ){
              					
              					return( 0 );
              					
              				}else if ( n1_alive == 0 ){
              					
              					return( -1 );
              					
              				}else if ( n2_alive == 0 ){
              					
              					return( 1 );
              				}
              				
              				if ( n1_alive > n2_alive ){
              					
              					return( -1 );
              					
              				}else if ( n1_alive < n2_alive ){
              					
              					return( 1 );
              				}
              				
              				return( 0 );
            			}
            		});
            	
            	int	done = 0;
            	
            	boolean	try_fn = live_node_count < 5;
            	
            	for ( DHTRouterContact rc: r_contacts ){
            	
    				if ( destroyed ){
    					
    					return;
    				}
    				
    				if ( force_bootstrap ){
    					
    					break;
    				}
    				
            		if ( done > (live_node_count>10?5:10 )){
            			
            			break;
            		}
            		
        			DHTTransportContactI2P t_cn = (DHTTransportContactI2P)((DHTControlContact)rc.getAttachment()).getTransportContact();

            		done++;
            			 
            		if ( try_fn ){
            			
            			try_fn = false;
            			
               			if ( transport.sendFindNode( t_cn.getNode(), RandomUtils.nextSecureHash())){
			
							refresh_find_node_ok++;
							
						}else{
							
							refresh_find_node_fail++;
						}
            		}else{
            			
            			if ( transport.sendPing( t_cn.getNode())){
							
            				router.contactAlive( rc.getID(), rc.getAttachment());
            				
							refresh_ping_ok++;
							
						}else{
							
            				router.contactDead( rc.getID(), false);

							refresh_ping_fail++;
						}
            		}
            	}
            	
            	if ( done < all_nodes_count ){
            		
            		int	bad_sent = 0;
            		
            			// ping a couple of the failing ones to precipitate their demise if possible
            		
            		for ( int i=all_nodes_count-1;i>=done&&bad_sent<2;i--){
            			
        				if ( destroyed ){
        					
        					return;
        				}
        				
          				if ( force_bootstrap ){
        					
        					break;
        				}
          				
          				DHTRouterContact	rc = r_contacts.get( i );
          				
               			DHTTransportContactI2P t_cn = (DHTTransportContactI2P)((DHTControlContact)rc.getAttachment()).getTransportContact();

            			if ( transport.sendPing( t_cn.getNode())){
							
            				router.contactAlive( rc.getID(), rc.getAttachment());
            											
						}else{
							
            				router.contactDead( rc.getID(), false);
						}
            			
            			bad_sent++;
            		}
            	}
        	}
        }finally{
        					
           	if ( bootstrap_node == null ){
            		
           			// we're bootstrap node, keep things fresh
           		            		
            }else{
            	
            	if ( force_bootstrap ){
            			
            		bootstrap_check_tick_count = 1;
            			
            	}else{
            			
            		if ( live_node_count>10 ){
            			
            			bootstrap_check_tick_count = 4;
            			
            		}else{
            			
            			bootstrap_check_tick_count = 1;
            		}
            	}
			}
        }
	}
	
	public List<NodeInfo>
	getNodesForBootstrap(
		int			number )
	{
		List<DHTRouterContact> contacts = dht.getRouter().getAllContacts();

		Collections.shuffle( contacts );

		List<NodeInfo> result = new ArrayList<NodeInfo>(number);
		List<NodeInfo> backup = new ArrayList<NodeInfo>(number);

		for ( DHTRouterContact contact: contacts ){

			DHTTransportContactI2P t_cn = (DHTTransportContactI2P)((DHTControlContact)contact.getAttachment()).getTransportContact();

			NodeInfo node = t_cn.getNode();

			if ( node.getDestination() == null ){

				continue;
			}

			if ( contact.isAlive()){

				result.add( node );

				if ( result.size() >= number ){

					break;
				}

			}else if ( !contact.isFailing()){

				if ( backup.size() < number ){

					backup.add( node );
				}
			}
		}
		
		int	num_live = result.size();
		
		if ( result.size() < number ){

			for ( int i=0;i<backup.size()&&result.size()<number;i++){

				result.add( backup.get(i));
			}
		}
		
		int num_backup = result.size() - num_live;

		log( "Getting nodes for bootstrap-> " + num_live + "/" + num_backup );

		return( result );
	}
	
	public void
	stop()
	{
		destroyed	= true;
		
		timer.cancel();
		
		storage_manager.exportContacts( dht );
		
		dht.destroy();
		
		transport.destroy();
	}
	
	public void
	print()
	{
		dht.print( true );
		
		log( transport.getStats().getString());
	}
	
	public String
	renderStatusHTML()
	{
		return( "" );
	}
	
	public String
	getStats()
	{
		return( transport.getStats().getString() + "/" + az_dht.getDHT().getTransport().getStats().getString() + ":" + az_dht.getDHT().getRouter().getStats().getStats()[DHTRouterStats.ST_CONTACTS]);
	}
	
	public String 
	getStatusString() 
	{
		long size = dht.getControl().getStats().getEstimatedDHTSize();
		
		if ( size < 50 ){
			
			return( adapter.getMessageText( "azi2phelper.status.bootstrapping" ));
					
		}else{
		
			return( adapter.getMessageText( "azi2phelper.status.node.est", String.valueOf( size ) + "/" + az_dht.getDHT().getControl().getStats().getEstimatedDHTSize()));
		}
	}
	
	private static class
	GetCacheEntry
		extends DHTOperationAdapter
	{
		private long						create_time = SystemTime.getMonotonousTime();
		
		private Map<String,Object[]>		contacts 			= new HashMap<String, Object[]>();
		
		private CopyOnWriteList<I2PHelperDHTListener>	listeners 	= new CopyOnWriteList<I2PHelperDHTListener>();

		private boolean		complete;
		private boolean		timeout;
		
		private
		GetCacheEntry(
			I2PHelperDHTListener		listener )
		{
			listeners.add( listener );
		}
		
		private long
		getCreateTime()
		{
			return( create_time );
		}
		
		private boolean
		isComplete()
		{
			synchronized( this ){
			
				return( complete );
			}
		}
		
		private void
		addListener(
			I2PHelperDHTListener		listener )
		{
			boolean			was_complete;
			boolean			was_timeout;
			
			synchronized( this ){
				
				was_complete	= complete;
				was_timeout		= timeout;

				if ( !was_complete ){
					
					listeners.add( listener );
				}
					
					// prefer not to do this while locked but if we want to avoid this we'll have to
					// deal with the fact that a 'complete' event might sneak in before we've informed
					// the listener of the pending 'valueRead' events...
				
				for ( Object[] entry: contacts.values()){
					
					DHTTransportContactI2P 	contact = (DHTTransportContactI2P)entry[0];
					DHTTransportValue 		value	= (DHTTransportValue)entry[1];
					
					String 	host 	=  Base32.encode( value.getValue()) + ".b32.i2p";
					boolean	is_seed = ( value.getFlags() & DHT.FLAG_SEEDING ) != 0;

					int	state = is_seed?I2PHelperDHTListener.CS_SEED:(contact.getProtocolVersion()==DHTUtilsI2P.PROTOCOL_VERSION_NON_VUZE?I2PHelperDHTListener.CS_UNKNOWN:I2PHelperDHTListener.CS_LEECH );
					
					try{
						listener.valueRead( contact, host, state );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
			
			if ( was_complete ){
				
				listener.complete( was_timeout );
			}
		}
		
		public void 
		searching(
			DHTTransportContact 	contact, 
			int 					level,
			int 					active_searches )
		{
				// not important to get this exactly right
			
			for ( I2PHelperDHTListener listener: listeners ){
			
				listener.searching( contact.getName());
			}
		}
		
		public void 
		read(
			DHTTransportContact 	_contact, 
			DHTTransportValue 		value)
		{
			String host =  Base32.encode( value.getValue()) + ".b32.i2p";
			
			synchronized( this ){
			
				if ( contacts.containsKey( host )){
					
					return;
				}
				
				DHTTransportContactI2P contact = (DHTTransportContactI2P)_contact;
				
				contacts.put( host, new Object[]{ contact, value });
				
				boolean	is_seed = ( value.getFlags() & DHT.FLAG_SEEDING ) != 0;
					
				int	state = is_seed?I2PHelperDHTListener.CS_SEED:(contact.getProtocolVersion()==DHTUtilsI2P.PROTOCOL_VERSION_NON_VUZE?I2PHelperDHTListener.CS_UNKNOWN:I2PHelperDHTListener.CS_LEECH );
				
				for ( I2PHelperDHTListener listener: listeners ){
			
					try{
						listener.valueRead((DHTTransportContactI2P)contact, host, state );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
		
		public void 
		complete(
			boolean _timeout ) 
		{
			List<I2PHelperDHTListener> to_inform;
			
			synchronized( this ){
			
				complete = true;
				timeout	= _timeout;
				
				to_inform = listeners.getList();
				
				listeners.clear();
			}
			
			for ( I2PHelperDHTListener listener: to_inform ){
				
				try{
					listener.complete( _timeout );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
	}
}
