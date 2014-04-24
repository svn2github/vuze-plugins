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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.i2p.client.I2PSession;
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
import org.parg.azureus.plugins.networks.i2p.dht.NID;
import org.parg.azureus.plugins.networks.i2p.dht.NodeInfo;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.router.DHTRouterContact;
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
	
	private I2PHelperAdapter			adapter;
	
	private NodeInfo					bootstrap_node;
	
	private TimerEventPeriodic		timer;
	
	final int timer_period 		= 15*1000;
	final int save_period		= 2*60*1000;
	final int save_ticks		= save_period / timer_period;
	
	private int		bootstrap_check_tick_count	= 1;
	private boolean	force_bootstrap;
	private long 	next_bootstrap;
	private int		consec_bootstraps;
	private long	last_external_bootstrap;

	private int refresh_ping_ok;
	private int refresh_ping_fail;
	private int refresh_find_node_ok;
	private int refresh_find_node_fail;
	
	private boolean					destroyed;
	
	public 
	DHTI2P(
		File				dir,
		I2PSession			session,
		NodeInfo			my_node,
		NodeInfo			boot_node,
		I2PHelperAdapter	_adapter )
	{
		adapter	= _adapter;
		
		File storage_dir = new File( dir, "dhtdata");
		
		if ( !storage_dir.isDirectory()){
		
			storage_dir.mkdirs();
		}
	
		bootstrap_node	= boot_node;
		
		storage_manager = new DHTPluginStorageManager( DHT_NETWORK, this, storage_dir );

		transport = new DHTTransportI2P( session, my_node, REQUEST_TIMEOUT );
				
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
					
		timer = SimpleTimer.addPeriodicEvent(
			"DHTI2P:checker",
			timer_period,
			new TimerEventPerformer() {
				
				private int	tick_count = 0;
				
				private volatile boolean bootstrapping;
				
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
	
	public DHT
	getDHT()
	{
		return( dht );
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
	ping(
		Destination		destination,
		int				port )
	{
		log( "ping not supported" );
	}
	
	public void
	ping(
		NodeInfo		node )
	{
		log( "ping not supported" );
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
					DHT.FLAG_NONE,
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
        				
        				force_bootstrap = false;
        				
        				consec_bootstraps 		= 0;
        				last_external_bootstrap	= 0;
        			}
        			
        			log( "Bootstrapping..." );
        			       
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
    							
    						log( "Bootstrap failed" );
						}
        			}else{
        				
        				log( "Bootstrap not resolved" );
        				
        				if ( 	last_external_bootstrap == 0 || 
        						now - last_external_bootstrap >= 30*60*1000 ){
        					
        					last_external_bootstrap = now;
        					
        					if ( adapter.tryExternalBootstrap()){
        						
        							// reschedule with a 2 min delay
        						
        						last_external_bootstrap = now - 28*60*1000;
        					}
        				}
        			}
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
		return( transport.getStats().getString());
	}
}
