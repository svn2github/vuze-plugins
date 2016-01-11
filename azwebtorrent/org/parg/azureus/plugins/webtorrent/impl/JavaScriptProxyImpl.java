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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.parg.azureus.plugins.webtorrent.JavaScriptProxy;


public class 
JavaScriptProxyImpl 
	implements JavaScriptProxy
{
	private static Object	lock 			= new Object();
	private static long		next_offer_id	= RandomUtils.nextAbsoluteLong();
	
	private static boolean				server_started = false;
	
	private static volatile JavaScriptProxyImpl	current_proxy;
	
	private static JavaScriptProxyPeerBridge	peer_bridge = new JavaScriptProxyPeerBridge();
	
	private final long	instance_id;	
	
	private int	current_port;
	
	private JavaScriptProxyInstance		current_instance;
	private AESemaphore					current_instance_sem = new AESemaphore("");
	
	private Map<String,OfferAnswerImpl>		offer_answer_map = new HashMap<>();
	
	public
	JavaScriptProxyImpl(
		long		_instance_id )
		
		throws Exception
	{
		instance_id		= _instance_id;
		
		current_proxy	= this;
		
		synchronized( lock ){
			
			if ( !server_started ){
								
				current_port = 
					JavaScriptProxyInstance.startServer(
						new JavaScriptProxyInstance.Listener()
						{
							@Override
							public void 
							controlCreated(
								JavaScriptProxyInstance inst ) 
							{
								peer_bridge.reset();
								
								current_proxy.instanceCreated( inst );
							}
							
							@Override
							public void 
							controlDestroyed(
								JavaScriptProxyInstance inst) 
							{
								current_proxy.instanceDestroyed( inst );
							}
							
							@Override
							public Map<String,Object>
					    	receiveControlMessage(
					    		JavaScriptProxyInstance		inst,
					    		Map<String,Object>			message )
					    	{
								return( current_proxy.receiveMessage( inst, message ));
							}
							
							@Override
					       	public void
					    	peerCreated(
					    		JavaScriptProxyInstance		inst )
					    	{
								peer_bridge.addPeer( inst );
					    	}
					    	
							@Override
					    	public void
					    	receivePeerMessage(
					    		JavaScriptProxyInstance		inst,
					    		ByteBuffer					message )
					    	{
								peer_bridge.receive( inst, message );
					    	}
					    	
							@Override
					    	public void
					    	peerDestroyed(
					    		JavaScriptProxyInstance		inst )
					    	{
								peer_bridge.removePeer( inst );
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

										current_instance.sendControlMessage( ping );

									}catch( Throwable e ){

										Debug.out( e );
									}
								}
							}
						});
			}
		}
	}
	
	public int
	getPort()
	{
		return( current_port );
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
    		
    		OfferAnswerImpl offer = offer_answer_map.get( offer_id );
    		
    		if ( offer != null ){
    			
    			offer.addMessage( message );
    		}
    	}
    	
    	return( result );
	}
	
	@Override
	public Offer 
	getOffer(
		byte[]		info_hash,
		long		timeout )
	{
		if ( current_instance_sem.reserve( timeout )){
			
			JavaScriptProxyInstance	inst;
			
			String	offer_id;
			
			OfferAnswerImpl	offer;
			
			synchronized( lock ){
			
				inst = current_instance;
				
				if ( inst == null ){
					
					return( null );
				}
				
				long oid = next_offer_id++;
				
				if ( oid == 0 ){
					
					oid = next_offer_id++;
				}
				
				offer_id = String.valueOf( oid );
				
				offer = new OfferAnswerImpl( inst, offer_id );
				
				offer_answer_map.put( offer_id, offer );
			}
				
			try{
				Map<String,Object>	message = new HashMap<>();
					
				message.put( "type", "create_offer" );
				message.put( "info_hash", Base32.encode( info_hash ));
				message.put( "offer_id", offer_id );
					
				inst.sendControlMessage( message );
				
				if ( offer.waitFor( timeout )){
					
					synchronized( lock ){
						
						offer_answer_map.remove( offer_id );
					}
					
					return( offer );
					
				}else{
					
					// TODO: pending offer, maybe it'll complete so use or delete?
					
				}
			}catch( Throwable e ){
				
				synchronized( lock ){
					
					offer_answer_map.remove( offer_id );
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
				inst.sendControlMessage( to_send );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	@Override
	public void 
	gotOffer(
		byte[]							info_hash,
		String 							external_offer_id, 
		String 							sdp,
		JavaScriptProxy.AnswerListener	listener )
	{
		if ( current_instance_sem.reserve( 10*1000 )){
			
			JavaScriptProxyInstance	inst;
			
			String	internal_offer_id;
			
			OfferAnswerImpl	offer;
			
			synchronized( lock ){
			
				inst = current_instance;
				
				if ( inst == null ){
					
					return;
				}
				
				long oid = next_offer_id++;
				
				if ( oid == 0 ){
					
					oid = next_offer_id++;
				}
				
				internal_offer_id = String.valueOf( oid );
				
				offer = new OfferAnswerImpl( inst, external_offer_id, listener );
				
				offer_answer_map.put( internal_offer_id, offer );
			}
		
			try{
				Map<String,Object> to_send = new HashMap<>();
				
				to_send.put( "type", "offer" );
				to_send.put( "offer_id", internal_offer_id );
				to_send.put( "info_hash", Base32.encode( info_hash ));
				to_send.put( "sdp", sdp );
			
				inst.sendControlMessage( to_send );
						
			}catch( Throwable e ){
				
				Debug.out( e );
				
				synchronized( lock ){
					
					offer_answer_map.remove( internal_offer_id );
				}
			}
		}
	}
	
    public class
    OfferAnswerImpl
    	implements Offer, Answer
    {
    	private final JavaScriptProxyInstance		inst;
    	private final String						external_offer_id;
    	private final AnswerListener				listener;
    	
    	private String			sdp 			= null;
    	private List<String>	candidates 		= new ArrayList<>();
    	
    	private AESemaphore		offer_sem = new AESemaphore( "" );
    	
    	private boolean	destroyed;
    	
       	private
    	OfferAnswerImpl(
    		JavaScriptProxyInstance		inst,
    		String						external_offer_id )
    	{
       		this( inst, external_offer_id, null );
    	}
       	
    	private
    	OfferAnswerImpl(
    		JavaScriptProxyInstance		_inst,
    		String						_external_offer_id,
    		AnswerListener				_listener )
    	{
    		inst					= _inst;
    		external_offer_id		= _external_offer_id;
    		listener				= _listener;
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
    				
    				if ( listener != null ){
    					
    					listener.gotAnswer( this );
    				}
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
    		return( external_offer_id );
    	}
    }	
}
