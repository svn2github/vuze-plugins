/*
 * Created on Dec 18, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.core3.util.ThreadPoolTask;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.ui.swt.Utils;
import org.parg.azureus.plugins.webtorrent.WebSocketServer.SessionWrapper;

import com.aelitis.azureus.util.JSONUtils;

public class 
WebTorrentPlugin 
	implements Plugin
{
	private static final long rand = RandomUtils.nextSecureAbsoluteLong();
	
	private int filter_port;
	
	private WebSocketServer	ws_server;
	
	private Map<String,Session>	client_sessions = new HashMap<String, Session>();
	
	@Override
	public void 
	initialize(
		PluginInterface plugin_interface )
		
		throws PluginException 
	{
		setupServer();
		
		setupURLHandler();
		
		getControlConnection();
		
		SimpleTimer.addPeriodicEvent(
			"WSTimer",
			5*1000,
			new TimerEventPerformer() {
				
				@Override
				public void perform(TimerEvent event) {
				
					if ( ws_server != null ){
						
						SessionWrapper session = ws_server.getSession( rand, 10*1000 );
						
						if ( session != null ){
							
							try{
								Map ping = new HashMap();
								
								ping.put( "type", "ping" );
								
								session.send( JSONUtils.encodeToJSON( ping ));
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}
					
				}
			});
	}
	
	private void
	setupServer()
	{
		try{
			ws_server = new WebSocketServer();
			
			ws_server.runServer();
			
		}catch( Throwable e ){
			
			ws_server = null;
			
			Debug.out( e );
		}
	}
	
	private void
	getControlConnection()
	{
		if ( filter_port != 0 ){
			
			new AEThread2( "")
			{
				public void
				run()
				{
					try{
						Utils.launch( new URL( "http://127.0.0.1:" + filter_port + "/index.html?id=" + rand ));
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}.start();
		}
	}
	
	private void
	setupURLHandler()
	{
		try{
			final ThreadPool thread_pool = new ThreadPool( "WebSocketPlugin", 32 );
			
		  	String	connect_timeout = System.getProperty("sun.net.client.defaultConnectTimeout"); 
		  	String	read_timeout 	= System.getProperty("sun.net.client.defaultReadTimeout"); 
		  			
		  	int	timeout = Integer.parseInt( connect_timeout ) + Integer.parseInt( read_timeout );
			
			thread_pool.setExecutionLimit( timeout );
		
			final ServerSocket ss = new ServerSocket( 0, 1024, InetAddress.getByName("127.0.0.1"));
			
			filter_port	= ss.getLocalPort();
			
			ss.setReuseAddress(true);
							
			new AEThread2("WebSocketPlugin::filterloop")
			{
				public void
				run()
				{
					long	failed_accepts		= 0;
	
					while(true){
						
						try{				
							Socket socket = ss.accept();
									
							failed_accepts = 0;
							
							thread_pool.run( new HttpFilter( socket ));
							
						}catch( Throwable e ){
							
							failed_accepts++;
													
							if ( failed_accepts > 10  ){
	
									// looks like its not going to work...
									// some kind of socket problem
												
								Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
									LogAlert.AT_ERROR, "Network.alert.acceptfail"),
									new String[] { "" + filter_port, "TCP" });
													
								break;
							}
						}
					}
				}
			}.start();
			
		}catch( Throwable e){
		
			Debug.out( e );
		}	
	}
	
	public URL
	getProxyURL(
		URL		url )
		
		throws IPCException
	{
		if ( filter_port == 0 ){
			
			throw( new IPCException( "Proxy unavailable" ));
		}
		
		try{
			return( new URL( "http://127.0.0.1:" + filter_port + "/?target=" + UrlUtils.encode( url.toExternalForm())));
			
		}catch( Throwable e ){
			
			throw( new IPCException( e ));
		}
	}
	
	private class
	HttpFilter
		extends ThreadPoolTask
	{
		private final String NL = "\r\n";
		
		private Socket		socket;
		
		protected
		HttpFilter(
			Socket		_socket )
		{
			socket	= _socket;
		}
		
		public void
		runSupport()
		{
			try{						
				setTaskState( "reading header" );
										
				InputStream	is = socket.getInputStream();
				
				byte[]	buffer = new byte[1024];
				
				String	header = "";
				
				while(true ){
						
					int	len = is.read(buffer);
						
					if ( len == -1 ){
					
						break;
					}
									
					header += new String( buffer, 0, len, Constants.BYTE_ENCODING );
									
					if ( 	header.endsWith( NL+NL ) ||
							header.indexOf( NL+NL ) != -1 ){
						
						break;
					}
				}
				
				List<String>	lines = new ArrayList<String>();
				
				int	pos = 0;
				
				while( true){
					
					int	p1 = header.indexOf( NL, pos );
					
					String	line;
					
					if ( p1 == -1 ){
						
						line = header.substring(pos);
						
					}else{
											
						line = header.substring( pos, p1 );
					}
					
					line = line.trim();
					
					if ( line.length() > 0 ){
					
						lines.add( line );
					}
				
					if ( p1 == -1 ){
						
						break;
					}
					
					pos = p1+2;
				}
				
				if ( lines.size() == 0 ){
					
					throw( new IOException( "No request" ));
				}
				
				String[]	lines_in = new String[ lines.size()];
				
				lines.toArray( lines_in );
				
				String	get = lines_in[0];
				
				System.out.println( "Got request: " + get );
				
				String resource 		= null;
				String content_type 	= null;
				
				if ( get.contains( "/index.html?id=" + rand )){
					
					resource 		= "index.html";
					content_type	= "text/html";
					
				}else if ( get.contains( "/script.js" )){
					
					resource 		= "script.js";
					content_type	= "application/javascript;charset=UTF-8";
					
				}else if ( get.contains( "/favicon.ico" )){

					resource 		= "favicon.ico";
					content_type	= "image/x-icon";
				}
				
				OutputStream	os = socket.getOutputStream();

				if ( resource != null ){
					
					InputStream res = getClass().getResourceAsStream( "/org/parg/azureus/plugins/webtorrent/resources/" + resource );
					
					byte[] bytes = FileUtil.readInputStreamAsByteArray( res );
					
					try{
						
						os.write( 
							( 	"HTTP/1.1 200 OK" + NL +
								"Content-Type: " + content_type + NL + 
								"Set-Cookie: vuze-ws-id=" + rand + "; path=/" + NL +
								"Connection: close" + NL +
								"Content-Length: " + bytes.length + NL + NL ).getBytes());
						
						os.write( bytes );
						
						os.flush();						
						
					}finally{
						
						res.close();
					}
				}else{
					
					pos = get.indexOf( ' ' );
					
					String url = get.substring( pos+1 ).trim();
					
					pos = url.lastIndexOf( ' ' );
					
					url = url.substring( 0,  pos ).trim();
					
					if ( url.startsWith( "/?target" )){
						
						String original_url = UrlUtils.decode( url.substring( url.indexOf('=') + 1 ));
						
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
						
						SessionWrapper wrapper = ws_server.getSession( rand, 60*1000 );
						
						if ( wrapper != null ){
							
							String offer_sdp = wrapper.getOfferSDP( 60*1000 );
							
							if ( offer_sdp != null ){
															
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
								
								String offer_str = "{\"offer\":{\"type\":\"offer\",\"sdp\":\"" + encodeCrap( offer_sdp ) + "\"},\"offer_id\":\"" + rand + "\"}";
								
					        	final String announce = 
					        			"{\"numwant\":" + numwant + 
					        			",\"uploaded\":" + uploaded + 
					        			",\"downloaded\":" + downloaded + 
					        			(event==null?"": ( ",\"event\":\"" + event + "\"" )) + 
					        			",\"info_hash\":\"" + encodeCrap( info_hash ) + "\"" +
					        			",\"peer_id\":\"" + encodeCrap( peer_id ) + "\"" +
					        			",\"offers\":[" + offer_str + "]}";
								
								Session	session = null;
								
								synchronized( client_sessions ){
									
									session = client_sessions.get( ws_url );
									
									if ( session != null && !session.isOpen()){
										
										client_sessions.remove( ws_url );
										
										session = null;
									}
								}
							
								if ( session != null ){
									
									session.getBasicRemote().sendText( announce );
									
								}else{
									
						            ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
						            
						            ClientManager client = ClientManager.createClient();
						            						            
						            Session client_session = 
						            	client.connectToServer(
						            		new Endpoint() 
						            		{
								                @Override
								                public void 
								                onOpen(
								                	Session 			session, 
								                	EndpointConfig 		config) 
								                {
								                    try{
								                    	
								                        session.addMessageHandler(new MessageHandler.Whole<String>() {
			
								                            @Override
								                            public void 
								                            onMessage(
								                            	String message ) 
								                            {
								                            	Map map = JSONUtils.decodeJSON( message );
								                            	
								                            	Map answer = (Map)map.get( "answer" );
								                            	
								                            	if ( answer != null ){
								                            	
								                            		String offer_id = (String)map.get( "offer_id" );
								                            		
								                            		try{
								                            			String sdp = (String)answer.get( "sdp" );
								                            		
								                            			Map to_send = new HashMap();
								                            			
								                            			to_send.put( "type", "answer" );
								                            			to_send.put( "sdp", sdp );
								                            			
								                						SessionWrapper wrapper = ws_server.getSession( rand, 60*1000 );

								                						if ( wrapper != null ){
								                						
								                							wrapper.send( JSONUtils.encodeToJSON( to_send ));
								                						}else{
								                							
								                							throw( new IOException( "wrapper is null" ));
								                						}
								                            			
								                            		}catch( Throwable e ){
								                            			
								                            			Debug.out( e );
								                            		}
								                            	}
								                            	
								                                System.out.println("Received message: "+message);
								                              
								                            }
								                        });
								                      
								                    	System.out.println( announce );
								                    	
								                        session.getBasicRemote().sendText( announce );
								                        
								                    }catch( IOException e ){
								                    	
								                        e.printStackTrace();
								                    }
								                }
								                
								                @Override
								                public void 
								                onError(
								                	Session 	session, 
								                	Throwable 	e )
								                {
								                	 synchronized( client_sessions ){
								                		 
								                		 Session s = client_sessions.get( ws_url );
								                		 
								                		 if ( s == session ){
								                			 
								                			 client_sessions.remove( ws_url );
								                		 }
								                	 }
								                }
								            }, cec, new URI( ws_url ));
					            
						            synchronized( client_sessions ){
						            	
						            	Session existing = client_sessions.put( ws_url, client_session );
						            	
						            	if ( existing != null ){
						            		
						            		try{
						            			existing.close();
						            			
						            		}catch( Throwable e ){
						            			
						            		}
						            	}
						            }
								}
					            
							}else{
								
								throw( new Exception( "WebSocket proxy offer not available" ));
							}
						}else{
							
							throw( new Exception( "WebSocket proxy not running" ));
						}
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
				
			}finally{
				
				try{
					socket.close();
					
				}catch( Throwable f ){
					
				}
			}
		}
		
		public void
		interruptTask()
		{
			try{	
				socket.close();
				
			}catch( Throwable e ){
				
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
}
