/*
 * Created on Jan 6, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.webtorrent.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.ui.swt.Utils;
import org.parg.azureus.plugins.webtorrent.JavaScriptProxy;

import com.aelitis.azureus.util.JSONUtils;

public class 
JavaScriptProxyImpl 
	implements JavaScriptProxy
{
	private static Object	lock 			= new Object();
	private static long		next_offer_id	= RandomUtils.nextAbsoluteLong();
	
	private static boolean				server_started = false;
	
	private static volatile JavaScriptProxyImpl	current_proxy;
	
	
	
	private final long	instance_id;	
	
	private JavaScriptProxyInstance		current_instance;
	private AESemaphore					current_instance_sem = new AESemaphore("");
	
	private Map<String,OfferImpl>		offer_map = new HashMap<>();
	
	public
	JavaScriptProxyImpl(
		long		_instance_id )
		
		throws Exception
	{
		instance_id		= _instance_id;
		
		current_proxy	= this;
		
		synchronized( lock ){
			
			if ( !server_started ){
								
				JavaScriptProxyInstance.startServer(
					new JavaScriptProxyInstance.Listener()
					{
						@Override
						public void 
						instanceCreated(
							JavaScriptProxyInstance inst ) 
						{
							current_proxy.instanceCreated( inst );
						}
						
						@Override
						public void 
						instanceDestroyed(
							JavaScriptProxyInstance inst) 
						{
							current_proxy.instanceDestroyed( inst );
						}
						
						@Override
						public Map<String,Object>
				    	receiveMessage(
				    		JavaScriptProxyInstance		inst,
				    		Map<String,Object>			message )
				    	{
							return( current_proxy.receiveMessage( inst, message ));
						}
					});
				
				server_started = true;

				SimpleTimer.addPeriodicEvent(
						"WSTimer",
						5*1000,
						new TimerEventPerformer() {
							
							@Override
							public void perform(TimerEvent event) {
							
								if ( current_instance != null ){
									
										
									try{
										Map ping = new HashMap();
										
										ping.put( "type", "ping" );
										
										current_instance.sendMessage( ping );
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
								
							}
						});
			}
		}
	}
	
	public void
	instanceCreated(
		JavaScriptProxyInstance		inst )
	{
		synchronized( lock ){
			
			if ( inst.getInstanceID() != instance_id ){
				
				inst.destroy();
				
				return;
			}
					
			current_instance	= inst;
			current_instance_sem.releaseForever();
		}	
	}
	
	public void
	instanceDestroyed(
		JavaScriptProxyInstance		inst )
	{
    	synchronized( lock ){
    		
    		if ( inst.getInstanceID() != instance_id ){
								
				return;
			}

    		if ( inst == current_instance ){
    			
    			current_instance 		= null;
    			current_instance_sem	= new AESemaphore( "" );
    		}
    	}
	}
	
	public Map<String,Object>
	receiveMessage(
		JavaScriptProxyInstance		inst,
		Map<String,Object>			message )
	{
		Map<String,Object>	result = new HashMap();
		
    	synchronized( lock ){
    		
    		if ( inst.getInstanceID() != instance_id ){
					
    			inst.destroy();
    			
				return( result );
			}
    		
    		String	offer_id = (String)message.get( "offer_id" );
    		
    		OfferImpl offer = offer_map.get( offer_id );
    		
    		if ( offer != null ){
    			
    			offer.addMessage( message );
    		}
    	}
    	
    	return( result );
	}
	
	@Override
	public Offer 
	getOffer(
		long		timeout )
	{
		if ( current_instance_sem.reserve( timeout )){
			
			JavaScriptProxyInstance	inst;
			
			String	offer_id;
			
			OfferImpl	offer;
			
			synchronized( lock ){
			
				inst = current_instance;
				
				if ( inst == null ){
					
					return( null );
				}
				
				offer_id = String.valueOf( next_offer_id++ );
				
				offer = new OfferImpl( inst, offer_id );
				
				offer_map.put( offer_id, offer );
			}
				
			try{
				Map<String,Object>	message = new HashMap<>();
					
				message.put( "type", "create_offer" );
				message.put( "offer_id", offer_id );
					
				inst.sendMessage( message );
				
				if ( offer.waitFor( timeout )){
					
					synchronized( lock ){
						
						offer_map.remove( offer_id );
					}
					
					return( offer );
					
				}else{
					
					// TODO: pending offer, maybe it'll complete so use or delete?
					
				}
			}catch( Throwable e ){
				
				synchronized( lock ){
					
					offer_map.remove( offer_id );
				}
			}
		}
		
		return( null );
	}
	
	
	@Override
	public void 
	gotAnswer(
		String 			offer_id, 
		String 			sdp) 
	{
		JavaScriptProxyInstance	inst;

		synchronized( lock ){

			inst	= current_instance;
		}
		
		Map to_send = new HashMap();
		
		to_send.put( "type", "answer" );
		to_send.put( "offer_id", offer_id );
		to_send.put( "sdp", sdp );
	
		if ( inst != null ){
		
			try{
				inst.sendMessage( to_send );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
			
		}
	}
	
	@Override
	public void 
	gotOffer(
		String 			offer_id, 
		String 			sdp) 
	{
		System.out.println( "TODO: gotOffer: " + offer_id );
	}
	
    public class
    OfferImpl
    	implements Offer
    {
    	private final JavaScriptProxyInstance		inst;
    	private final String						offer_id;
    	
    	private String			sdp 			= null;
    	private List<String>	candidates 		= new ArrayList<>();
    	
    	private AESemaphore		offer_sem = new AESemaphore( "" );
    	
    	private boolean	destroyed;
    	
    	private
    	OfferImpl(
    		JavaScriptProxyInstance		_inst,
    		String						_offer_id )
    	{
    		inst			= _inst;
    		offer_id		= _offer_id;
    	}
    	
    	protected boolean
    	waitFor(
    		long		timeout )
    	{
    		boolean done = offer_sem.reserve( timeout );
    		
    		if ( !done ){
    		
    			Debug.out( "Timeout waiting for offer" );
    		}
    		
    		return( done );
    	}
    	
    	public void
    	addMessage(
    		Map		msg )
    	{
    		String	type = (String)msg.get( "type" );
    		
    		if ( type.equals( "sdp" )){
    			
    			sdp	= (String)msg.get( "sdp" );
    			
    		}else if ( type.equals( "ice_candidate" )){
    			
    			String candidate = (String)msg.get( "candidate" );
    			    			
    			if ( candidate.length() > 0 ){
    				
    				candidates.add( candidate );
    				
    			}else{
    				
    				offer_sem.releaseForever();
    			}
    		}
    	}
    	
    	@Override
    	public String
    	getSDP()
    	{	
    		String offer = "";
    		
    		String[] bits = sdp.split( "\n" );
    		
    		boolean	 done_candidates = false;
    		
    		for ( String bit: bits ){
    			
    			bit = bit.trim();
    			
    			if ( bit.length() == 0 ){
    				
    				continue;
    			}
    			
    			if ( bit.startsWith( "a=" ) && !bit.startsWith( "a=msid-" ) && !done_candidates){
    				
    				done_candidates = true;
    				
    				for ( String candidate: candidates ){
    					
    					offer += "a=" + candidate + "\r\n";
    				}
    			}
    			
    			offer += bit + "\r\n";
    		}
    		
    		if ( !done_candidates ){
    			
    			for ( String candidate: candidates ){
					
					offer += "a=" + candidate + "\r\n";
				}
    		}
    		    		    		
    		return( offer );
    	}
    	
    	public String
    	getOfferID()
    	{
    		return( offer_id );
    	}
    }	
}
