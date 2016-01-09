/*
 * Created on Jan 7, 2016
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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.ByteBuffer;


import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;
import com.aelitis.azureus.core.proxy.AEProxyAddressMapper;
import com.aelitis.azureus.core.proxy.AEProxyFactory;

public class 
JavaScriptProxyPeerBridge 
{
	private static byte[] fake_header_and_reserved = new byte[1+19+8];

	static{
		fake_header_and_reserved[0] = (byte)19;
		
		System.arraycopy( BTHandshake.PROTOCOL.getBytes(),0,fake_header_and_reserved,1,19);
		
		byte[] reserved = new byte[]{0, 0, 0, 0, 0, (byte)16, 0, 0 }; 
		
		System.arraycopy( reserved,0,fake_header_and_reserved, 20, 8 );
	}
	
	private Map<JavaScriptProxyInstance,Connection>		peer_map = new HashMap<>();
	
	public void
	addPeer(
		JavaScriptProxyInstance		peer )
	{		
		Connection connection = new Connection( peer );
		
		synchronized( peer_map ){
		
			peer_map.put( peer, connection );
			
			System.out.println( "addPeer: " + peer.getOfferID() + ", peers=" + peer_map.size());

		}
		
		try{
			connection.start();
			
			
		}catch( Throwable e ){
			
			connection.destroy();
		}
	}
	
	public void
	removePeer(
		JavaScriptProxyInstance		peer )
	{		
		Connection connection;
		
		synchronized( peer_map ){
		
			connection = peer_map.remove( peer );
			
			if ( connection != null ){
			
				System.out.println( "removePeer: " + peer.getOfferID() + ", peers=" + peer_map.size());
			}
		}
		
		if ( connection != null ){
			
			connection.destroy();
		}
	}
	
	public void
	receive(
		JavaScriptProxyInstance		peer,
		ByteBuffer					data )
	{
		//System.out.println( "receive: " + peer.getOfferID());
		
		Connection connection;
		
		synchronized( peer_map ){
		
			connection = peer_map.get( peer );
		}
		
		if ( connection != null ){
			
			connection.receive( data );
			
		}else{
			
			peer.destroy();
		}
	}
	
	
	private class
	Connection
	{
		private	JavaScriptProxyInstance				proxy;
		private Socket								vuze_socket;
		
		private AEProxyAddressMapper.PortMapping	mapping;
		
		AESemaphore		wait_sem = new AESemaphore( "" );
		
		long	total_sent	= 0;
		
		int	to_skip = 0;
		
		private
		Connection(
			JavaScriptProxyInstance		_proxy )
		{
			proxy	= _proxy;
		}
		
		private void
		start()
		
			throws Exception
		{
			boolean	ok = false;
			
			try{
				vuze_socket = new Socket( Proxy.NO_PROXY );

				vuze_socket.bind( null );
				
				final int local_port = vuze_socket.getLocalPort();
				
					// we need to pass the peer_ip to the core so that it doesn't just see '127.0.0.1'
				
				String remote_ip = proxy.getRemoteIP();
				
				Debug.outNoStack( "remote-ip disabled for testing" );
				//if ( remote_ip == null ){
					
					remote_ip = "websocket." + ( proxy.isIncoming()?1:0) + local_port;
				//}
				
				Map<String,Object>	props = new HashMap<String, Object>();
				
				props.put( AEProxyAddressMapper.MAP_PROPERTY_DISABLE_AZ_MESSAGING, true );
				props.put( AEProxyAddressMapper.MAP_PROPERTY_PROTOCOL_QUALIFIER, "WebSocket" );
					
				mapping = AEProxyFactory.getAddressMapper().registerPortMapping( local_port, remote_ip, props );
				
				InetAddress bind = NetworkAdmin.getSingleton().getSingleHomedServiceBindAddress();
				
				if ( bind == null || bind.isAnyLocalAddress()){
					
					bind = InetAddress.getByName( "127.0.0.1" );
				}
				
				vuze_socket.connect( new InetSocketAddress( bind, COConfigurationManager.getIntParameter( "TCP.Listen.Port" )));
			
				vuze_socket.setTcpNoDelay( true );
	
				if ( proxy.isIncoming()){
					
					OutputStream	os = vuze_socket.getOutputStream();
					
					os.write( fake_header_and_reserved );
					
					os.write( proxy.getInfoHash());
					/*
					byte[] peer_id = new byte[20];
					
					RandomUtils.nextBytes( peer_id );
					
					os.write( peer_id );
					*/
					
					os.flush();
					
					to_skip = fake_header_and_reserved.length + 20;
					
				}
				
				final InputStream is = vuze_socket.getInputStream();
				
				new AEThread2( "WebSocket:pipe" )
				{
					public void
					run()
					{
						try{						
							while( true ){
								
								byte[]	buffer = new byte[16*1024];
	
								int	len = is.read( buffer );
								
								if ( len <= 0 ){
									
									break;
								}
								
								if ( total_sent == 11111 && len >= 68 ){
									
									proxy.sendPeerMessage( ByteBuffer.wrap( buffer, 0, 68 ));
									
									proxy.sendPeerMessage( ByteBuffer.wrap( buffer, 68, len-68 ));
									
								}else{
									
									proxy.sendPeerMessage( ByteBuffer.wrap( buffer, 0, len ));
								}
								
								total_sent += len;
							}
						}catch( Throwable e ){
							
						}finally{
							
							destroy();
						}
					}
				}.start();
				
				ok = true;
				
			}finally{
				
				wait_sem.releaseForever();
				
				if ( !ok ){
					
					destroy();
				}
			}
		}
		
		private void
		receive(
			ByteBuffer	data )
		{
			if ( !wait_sem.reserve(60*1000)){
				
				Debug.out( "eh?" );
				
				destroy();
				
				return;
			}
			
			if ( to_skip > 0 ){
				
				int	remaining = data.remaining();
				
				if ( to_skip <  remaining ){
					
					data.position( data.position() + to_skip );
					
					to_skip = 0;
					
				}else{
					
					to_skip -= remaining;
					
					return;
				}
			}
			
			try{
				OutputStream os = vuze_socket.getOutputStream();
				
				os.write( data.array(), data.arrayOffset() + data.position(), data.remaining() );
				
				os.flush();
				
			}catch( Throwable e ){
				
				destroy();
			}
		}
		
		private void
		destroy()
		{
			try{
				if ( vuze_socket != null ){
				
					try{
						vuze_socket.close();
					
						vuze_socket = null;
						
					}catch( Throwable e ){
					}
				}
				
				if ( proxy != null ){
					
					try{
						proxy.destroy();
						
					}catch( Throwable e ){
						
					}
				}
				
				if ( mapping != null ){
					
					mapping.unregister();
					
					mapping = null;
				}
			}finally{
				
				synchronized( peer_map ){
					
					peer_map.remove( proxy );
				}
			}
		}
	}
}
