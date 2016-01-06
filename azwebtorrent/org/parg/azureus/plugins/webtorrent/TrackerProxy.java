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


package org.parg.azureus.plugins.webtorrent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.bouncycastle.jce.provider.JDKAlgorithmParameterGenerator.AES;
import org.glassfish.tyrus.client.ClientManager;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.parg.azureus.plugins.webtorrent.JavaScriptProxy.Offer;

import com.aelitis.azureus.util.JSONUtils;

public class 
TrackerProxy 
	implements LocalWebServer.Listener
{
	private final Listener		listener;
	
	private Map<String,ClientSession>	client_sessions = new HashMap<String, ClientSession>();

	protected
	TrackerProxy(
		Listener		_listener )
	{
		listener	= _listener;
	}
	
	@Override
	public void 
	handleRequest(
		String 			original_url, 
		OutputStream 	os )
			
		throws Exception 
	{
		try{
			int	q_pos = original_url.indexOf( '?' );
			
			final String	ws_url = original_url.substring( 0,  q_pos );
			
			String[] bits = original_url.substring( q_pos+1 ).split( "&" );
			
			int		numwant		= 0;
			long	uploaded	= 0;
			long	downloaded	= 0;
			String	event		= null;
			byte[]	info_hash	= null;
			byte[]	peer_id		= null;
			
			for ( String bit: bits ){
				
				String[]	temp = bit.split( "=" );
				
				String	lhs = temp[0];
				String	rhs	= temp[1];
				
				if ( lhs.equals( "numwant" )){
					
					numwant = Integer.parseInt( rhs );
					
				}else if ( lhs.equals( "uploaded" )){
					
					uploaded = Long.parseLong( rhs );
					
				}else if ( lhs.equals( "downloaded" )){
					
					downloaded = Long.parseLong( rhs );
					
				}else if ( lhs.equals( "event" )){
					
					event	= rhs;
					
				}else if ( lhs.equals( "info_hash" )){
	
					info_hash = URLDecoder.decode( rhs, "ISO-8859-1" ).getBytes( "ISO-8859-1" );
					
				}else if ( lhs.equals( "peer_id" )){
	
					peer_id = URLDecoder.decode( rhs, "ISO-8859-1" ).getBytes( "ISO-8859-1" );
				}		
			}
			
			Offer	offer = listener.getOffer( 60*1000 );
			
			if ( offer != null ){
				
												
				/*
				Map<String,Object> announce_map = new HashMap<String,Object>();
				
				announce_map.put( "numwant", numwant );
				announce_map.put( "uploaded", uploaded );
				announce_map.put( "downloaded", downloaded );
				if ( event != null ){
					announce_map.put( "event", event );
				}
				announce_map.put( "info_hash", encodeCrap( info_hash ));
				announce_map.put( "peer_id", "-WW0063-8a3c574bbf70" );// encodeCrap( peer_id ));
				
				List<Map> offers = new ArrayList<Map>();
				
				Map<String,Object> offer_outer = new HashMap<>();
				Map<String,Object> offer_inner = new HashMap<>();
				
				offer_inner.put( "type", "offer" );
				
				String working_offer = encodeCrap("v=0\r\no=- 1861226001015807639 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=msid-semantic: WMS\r\nm=application 12897 DTLS/SCTP 5000\r\nc=IN IP4 52.27.228.126\r\na=candidate:211156821 1 udp 2122260223 192.168.1.5 60711 typ host generation 0\r\na=candidate:964701137 1 udp 2122194687 169.254.111.217 60712 typ host generation 0\r\na=candidate:1839720110 1 udp 2122129151 192.168.201.1 60713 typ host generation 0\r\na=candidate:2999745851 1 udp 2122063615 192.168.56.1 60714 typ host generation 0\r\na=candidate:2527468734 1 udp 2121998079 169.254.138.101 60715 typ host generation 0\r\na=candidate:1108738981 1 tcp 1518280447 192.168.1.5 0 typ host tcptype active generation 0\r\na=candidate:1996740385 1 tcp 1518214911 169.254.111.217 0 typ host tcptype active generation 0\r\na=candidate:589568606 1 tcp 1518149375 192.168.201.1 0 typ host tcptype active generation 0\r\na=candidate:4233069003 1 tcp 1518083839 192.168.56.1 0 typ host tcptype active generation 0\r\na=candidate:3626360910 1 tcp 1518018303 169.254.138.101 0 typ host tcptype active generation 0\r\na=candidate:2781507712 1 udp 1686052607 207.140.28.98 60711 typ srflx raddr 192.168.1.5 rport 60711 generation 0\r\na=candidate:1392442185 1 udp 41885439 52.27.228.126 12897 typ relay raddr 207.140.28.98 rport 60711 generation 0\r\na=ice-ufrag:qC4tVJ3Z5FjY8jJm\r\na=ice-pwd:bVpH0lVsz15UbHlrMWuf4mfv\r\na=fingerprint:sha-256 0A:02:71:70:54:0E:3B:DB:EA:C3:3A:3A:F3:A1:ED:EA:C4:A5:57:DF:51:EE:F2:1C:A0:0B:55:9F:8D:13:63:00\r\na=setup:actpass\r\na=mid:data\r\na=sctpmap:5000 webrtc-datachannel 1024\r\n" ); 
	
				offer_inner.put( "sdp", working_offer  ); // encodeCrap( offer_sdp ));
				
				offer_outer.put( "offer", offer_inner );
				offer_outer.put( "offer_id", String.valueOf( rand ) );
				
				offers.add( offer_outer );
				
				announce_map.put( "offers", offers );
				
				final String	announce = JSONUtils.encodeToJSON( announce_map );
				
				*/
				
					// roll it by hand, something in the above fucks up
				
				String offer_str = "{\"offer\":{\"type\":\"offer\",\"sdp\":\"" + encodeCrap( offer.getSDP()) + "\"},\"offer_id\":\"" + offer.getOfferID() + "\"}";
				
	        	final String announce = 
	        			"{\"numwant\":" + numwant + 
	        			",\"uploaded\":" + uploaded + 
	        			",\"downloaded\":" + downloaded + 
	        			(event==null?"": ( ",\"event\":\"" + event + "\"" )) + 
	        			",\"info_hash\":\"" + encodeCrap( info_hash ) + "\"" +
	        			",\"peer_id\":\"" + encodeCrap( peer_id ) + "\"" +
	        			",\"offers\":[" + offer_str + "]}";
				
	        	ClientSession	client_session = null;
				
				synchronized( client_sessions ){
					
					client_session = client_sessions.get( ws_url );
					
					if ( client_session != null && !client_session.isOpen()){
						
						client_sessions.remove( ws_url );
						
						client_session = null;
					}
				}
			
				if ( client_session != null ){
					
					client_session.send( announce );
					
				}else{
					
		            ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		            
		            ClientManager client = ClientManager.createClient();
		            						            
		            Session session = 
		            	client.connectToServer(
		            		new Endpoint() 
		            		{
				                @Override
				                public void 
				                onOpen(
				                	final Session 		session, 
				                	EndpointConfig 		config) 
				                {
				                    try{
				                    	
				                        session.addMessageHandler(new MessageHandler.Whole<String>() {
	
				                            @Override
				                            public void 
				                            onMessage(
				                            	String message ) 
				                            {
				                                System.out.println("Received message: " + message);
	
				                            	Map map = JSONUtils.decodeJSON( message );
				                            			                            	
				                            	if ( map.containsKey( "answer" )){
				                            	
				                            		Map answer = (Map)map.get( "answer" );
				                            		
				                            		String offer_id = (String)map.get( "offer_id" );
				                            		
				                            		try{
				                            			String sdp = (String)answer.get( "sdp" );
				                            		
				                            			listener.gotAnswer( null, offer_id, sdp );
				                            			
				                            		}catch( Throwable e ){
				                            			
				                            			Debug.out( e );
				                            		}
				                            	}else if ( map.containsKey( "offer" )){
				                            		
				                            		Map answer = (Map)map.get( "offer" );
				                            		
				                            		String offer_id = (String)map.get( "offer_id" );
				                            		
				                            		try{
				                            			String sdp = (String)answer.get( "sdp" );
				                            		
				                            			listener.gotOffer( null, offer_id, sdp );
				                            			
				                            		}catch( Throwable e ){
				                            			
				                            			Debug.out( e );
				                            		}
				                            		
				                            	}else if ( map.containsKey( "complete" )){
				                            		
				                            		synchronized( client_sessions ){
							                    		 
							                    		 ClientSession s = client_sessions.get( ws_url );
								                		 
								                		 if ( s.getSession() == session ){
								                			 
								                			 s.announceReceived( map );
								                		 }
								                	 }
				                            	}                              
				                            }
				                        });
				                      
				                    	System.out.println( announce );
				                    	
				                        session.getBasicRemote().sendText( announce );
				                        
				                    }catch( IOException e ){
				                    	
				                    	 synchronized( client_sessions ){
				                    		 
				                    		 ClientSession s = client_sessions.get( ws_url );
					                		 
					                		 if ( s.getSession() == session ){
					                			 
					                			 s.close();
					                			 
					                			 client_sessions.remove( ws_url );
					                		 }
				                    	 }
				                    }
				                }
				                
				                @Override
				                public void 
				                onError(
				                	Session 	session, 
				                	Throwable 	e )
				                {
				                	 synchronized( client_sessions ){
				                		 
				                		 ClientSession s = client_sessions.get( ws_url );
				                		 
				                		 if ( s.getSession() == session ){
				                			 
				                			 s.close();
				                			 
				                			 client_sessions.remove( ws_url );
				                		 }
				                	 }
				                }
				            }, cec, new URI( ws_url ));
	            
		            client_session = new ClientSession( session );
		            
		            synchronized( client_sessions ){
		            	
		            	ClientSession existing = client_sessions.put( ws_url, client_session );
		            	
		            	if ( existing != null ){
		            		
		            		try{
		            			existing.close();
		            			
		            		}catch( Throwable e ){
		            			
		            		}
		            	}
		            }
				}   
				
				Map reply = client_session.waitForAnnounce( 30*1000 );
				
				if ( reply == null ){
					
					throw( new Exception( "Timeout" ));
				}
				
				byte[] x = BEncoder.encode( reply );
				
				String[] reply_lines = {
						
						"HTTP/1.1 200 OK",
						"Content-Length: " + x.length,
						"Connection: close"
				};
				
				for ( String str: reply_lines ){
					
					os.write((str + "\r\n" ).getBytes( "ISO-8859-1" ));
				}
				
				os.write( "\r\n" .getBytes( "ISO-8859-1" ));
				
				os.write( x );
			}else{
				
				throw( new Exception( "WebSocket proxy offer not available" ));
			}
		}catch( Throwable e ){
			
			Map	failure = new HashMap();
			
			failure.put( "failure reason", Debug.getNestedExceptionMessage( e ));
			
			try{
				byte[] x = BEncoder.encode( failure );
							
				String[] reply_lines = {
				
						"HTTP/1.1 200 OK",
						"Content-Length: " + x.length,
						"Connection: close"
				};
				
				for ( String str: reply_lines ){
					
					os.write((str + "\r\n" ).getBytes( "ISO-8859-1" ));
				}
				
				os.write( "\r\n" .getBytes( "ISO-8859-1" ));
				
				os.write( x );
				
			}catch( Throwable f ){
				
				//Debug.printStackTrace(f);
			}
		}
	}
	
	private static String
	encodeCrap(
		byte[]	bytes )
	{
		String str = "";

		for ( byte b: bytes ){

			if ( Character.isLetterOrDigit((char)b)){

				str += (char)b;
			}else{
				int	code = b&0xff;

				String s = Integer.toHexString( code );

				while( s.length() < 4 ){

					s = "0" + s;
				}

				str += "\\u" + s;
			}
		}

		return( str );
	}

    private static String
    encodeCrap(
    	String str_in )
    {
       	String str = "";
    	
    	for (char c: str_in.toCharArray()){
    		
    		if ( Character.isLetterOrDigit(c)){
    		
    			str += c;
    			
    		}else{
    			
    			int	code = c&0xff;
    			
    			String s = Integer.toHexString( code );
    			
    			while( s.length() < 4 ){
    				
    				s = "0" + s;
    			}
    			
    			str += "\\u" + s;
    		}
    	}
    	
    	return( str );
    }
    
    public interface
    Listener
    {
    	public JavaScriptProxy.Offer
    	getOffer(
    		long		timeout );
    	
    	public void
    	gotAnswer(
    		byte[]		hash,
    		String		offer_id,
    		String		sdp )
    		
    		throws Exception;
    	
    	public void
    	gotOffer(
    		byte[]		hash,
    		String		offer_id,
    		String		sdp )
    		
    		throws Exception;
    }
    
    private class
    ClientSession
    {
    	private Session		session;
    	
    	private Map			last_announce;
    	private long		last_announce_time;
    	private AESemaphore	announce_sem;
    	
    	protected
    	ClientSession(
    		Session		_session )
    	{
    		session	= _session;
    	}
    	
    	private void
    	send(
    		String		str )
    		
    		throws Exception
    	{
    		session.getBasicRemote().sendText( str );
    	}
    	
    	private Session
    	getSession()
    	{
    		return( session );
    	}
    	
    	private boolean
    	isOpen()
    	{
    		return( session.isOpen());
    	}
    	
    	private void
    	announceReceived(
    		Map		map )
    	{
    		synchronized( this ){
    			last_announce 		= map;
    			last_announce_time	= SystemTime.getMonotonousTime();
    			
    			if ( announce_sem != null ){
    				
    				announce_sem.releaseForever();
    				
    				announce_sem = null;
    			}
    		}
    	}
    	
    	private Map
    	waitForAnnounce(
    		long		timeout )
    	{
    		AESemaphore sem;
    		
    		synchronized( this ){
    			
    			if ( last_announce != null && SystemTime.getMonotonousTime() - last_announce_time < 10000 ){
    				
    				return( last_announce );
    			}
    			
    			if ( announce_sem == null ){
    				
    				announce_sem = new AESemaphore( "" );
    			}
    			
    			sem = announce_sem;
    		}
    		
    		sem.reserve( timeout );
    		
    		return( last_announce );
    	}
    	
    	private void
    	close()
    	{
    		try{
    			session.close();
    			
    		}catch( Throwable e ){
    			
    		}
    	}
    }
}
