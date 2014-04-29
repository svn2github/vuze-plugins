/*
 * Created on Apr 24, 2014
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.i2p.I2PAppContext;
import net.i2p.client.naming.NamingService;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.data.Destination;

import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.ThreadPool;

import com.aelitis.azureus.core.proxy.AEProxyConnection;
import com.aelitis.azureus.core.proxy.AEProxyException;
import com.aelitis.azureus.core.proxy.AEProxyFactory;
import com.aelitis.azureus.core.proxy.AEProxyState;
import com.aelitis.azureus.core.proxy.socks.AESocksProxy;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyAddress;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyConnection;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyFactory;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyPlugableConnection;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyPlugableConnectionFactory;

public class 
I2PHelperSocksProxy 
	implements AESocksProxyPlugableConnectionFactory
{
	private InetAddress local_address;
	
	private Set<SOCKSProxyConnection>		connections = new HashSet<SOCKSProxyConnection>();
	
	private ThreadPool	connect_pool = new ThreadPool( "I2PHelperSocksProxyConnect", 10 );

	{
		try{
			local_address = InetAddress.getLocalHost();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			local_address = null;
		}
	}
	
	private I2PHelperRouter		router;
	private I2PHelperAdapter	adapter;
	
	private NamingService		name_service;
	
	private AESocksProxy 		proxy;
	
	private Map<String,String>	intermediate_host_map	= new HashMap<String, String>();

	private boolean				destroyed;
	
	protected
	I2PHelperSocksProxy(
		I2PHelperRouter			_router,
		int						_port,
		I2PHelperAdapter		_adapter )
	
		throws AEProxyException
	{
		router	= _router;
		adapter = _adapter;
				
		name_service = I2PAppContext.getGlobalContext().namingService();
				
		proxy = AESocksProxyFactory.create( _port, 120*1000, 120*1000, this );
		
		adapter.log( "Intermediate SOCKS proxy started on port " + proxy.getPort());
	}
	
	protected int
	getPort()
	{
		return( proxy.getPort());
	}
	
	protected String
	getIntermediateHost(
		String		host )
		
		throws AEProxyException
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				throw( new AEProxyException( "Proxy destroyed" ));
			}
	
			while( true ){
				
				int	address = 0x0a000000 + RandomUtils.nextInt( 0x00ffffff );
								
				String intermediate_host = PRHelpers.intToAddress( address );
				
				if ( !intermediate_host_map.containsKey( intermediate_host )){
					
					intermediate_host_map.put( intermediate_host, host );
					
					return( intermediate_host );
				}
			}
		}
	}
	
	protected void
	removeIntermediateHost(
		String		intermediate )
	{
		synchronized( this ){
			
			intermediate_host_map.remove( intermediate );
		}
	}
	
	public AESocksProxyPlugableConnection
	create(
		AESocksProxyConnection	connection )
	
		throws AEProxyException
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				throw( new AEProxyException( "Proxy destroyed" ));
			}
			
			if ( connections.size() > 32 ){
				
				try{
					connection.close();
					
				}catch( Throwable e ){
				}
				
				throw( new AEProxyException( "Too many connections" ));
			}
		
			SOCKSProxyConnection con = new SOCKSProxyConnection( connection );
			
			connections.add( con );
			
			System.out.println( "total connections=" + connections.size() + ", ih=" + intermediate_host_map.size());
			
			return( con );
		}
	}
	
	private I2PSocketManager
	getSocketManager()
	
		throws Exception
	{
		long	start = SystemTime.getMonotonousTime();
		
		while( SystemTime.getMonotonousTime() - start < 60*1000 ){
			
			if ( destroyed ){
				
				throw( new Exception( "SOCKS proxy destroyed" ));
			}
			
			I2PSocketManager sm = router.getSocketManager();
			
			if ( sm != null ){
				
				return( sm );
			}
			
			try{
				Thread.sleep(1000);
				
			}catch( Throwable e ){
				
			}
		}
		
		throw( new Exception( "Timeout waiting for socket manager" ));
	}
	
	private I2PSocket
	connectToAddress(
		String		address )
		
		throws Exception
	{
		if ( address.length() < 400 ){
			
			if ( !address.endsWith( ".i2p" )){
			
				address += ".i2p";
			}
		}
		
		try{
			Destination remote_dest;
			
			if ( name_service != null ){
			
				remote_dest = name_service.lookup( address );
				
			}else{
				
				remote_dest = new Destination();
	       
				remote_dest.fromBase64( address );
			}
			
			if ( remote_dest == null ){
				
				throw( new Exception( "Failed to resolve address '" + address + "'" ));
			}
			
			I2PSocket socket = getSocketManager().connect( remote_dest );
						
			return( socket );
			
		}catch( Throwable e ){
			
			String msg = Debug.getNestedExceptionMessage(e).toLowerCase();
			
			boolean	logit = true;
			
			if ( msg.contains( "timeout" ) || msg.contains( "timed out" ) || msg.contains( "reset" ) || msg.contains( "resolve" )){
				
				logit = false;
			}
			
			if ( logit ){
				
				e.printStackTrace();
			}
			
			throw( new IOException( Debug.getNestedExceptionMessage(e)));
		}
	}
	
	private void
	removeConnection(
		SOCKSProxyConnection	connection )
	{
		synchronized( this ){
			
			connections.remove( connection );
			
			System.out.println( "total connections=" + connections.size() + ", ih=" + intermediate_host_map.size());
		}
	}
	
	private void
	trace(
		String		str )
	{
		adapter.log( str );
	}
	
	protected void
	destroy()
	{
		List<SOCKSProxyConnection>	to_close = new ArrayList<SOCKSProxyConnection>();
		
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			synchronized( this ){ 
				
				destroyed = true;
			}
			
			to_close.addAll( connections );
			
			try{
				proxy.destroy();
			
			}catch( Throwable e ){
			}
		}
		
		for ( SOCKSProxyConnection c: to_close ){
			
			try{
				c.close();
				
			}catch( Throwable e ){
			}
		}
	}
	
	private class
	SOCKSProxyConnection
		implements AESocksProxyPlugableConnection
	{
		
			// try to buffer at least a whole block
		
		public static final int RELAY_BUFFER_SIZE	= 64*1024 + 256;
		
		protected I2PSocket						socket;
		protected boolean						socket_closed;
		
		protected AESocksProxyConnection		proxy_connection;
		
		protected proxyStateRelayData			relay_state;
		
		protected
		SOCKSProxyConnection(
			AESocksProxyConnection			_proxy_connection )
		{
			proxy_connection	= _proxy_connection;
			
			proxy_connection.disableDNSLookups();
		}
		
		public String
		getName()
		{
			return( "I2PPluginConnection" );
		}
		
		public InetAddress
		getLocalAddress()
		{
			return( local_address );
		}
		
		public int
		getLocalPort()
		{
			return( -1 );
		}

		public void
		connect(
			AESocksProxyAddress		_address )
			
			throws IOException
		{
			InetAddress resolved 	= _address.getAddress();
			String		unresolved	= _address.getUnresolvedAddress();
			
			if ( resolved != null ){
				
				synchronized( this ){
					
					String	intermediate = intermediate_host_map.remove( resolved.getHostAddress());
					
					if ( intermediate != null ){
						
						resolved	= null;
						unresolved	= intermediate;
					}
				}
			}
			
			trace( "connect request to " + unresolved + "/" + resolved + "/" + _address.getPort());
					
			boolean		handling_connection = false;
			
			try{
				if ( resolved != null ){
						
					trace( "    delegating resolved" );
						
					AESocksProxyPlugableConnection	delegate = proxy_connection.getProxy().getDefaultPlugableConnection( proxy_connection );
						
					proxy_connection.setDelegate( delegate );
						
					delegate.connect( _address );

				}else{ 
					
					final String	externalised_address = AEProxyFactory.getAddressMapper().externalise( unresolved );
				
					if ( !externalised_address.toLowerCase().endsWith(".i2p")){
																
						trace( "    delegating unresolved" );
	
						AESocksProxyPlugableConnection	delegate = proxy_connection.getProxy().getDefaultPlugableConnection( proxy_connection );
						
						proxy_connection.enableDNSLookups();
						
						proxy_connection.setDelegate( delegate );
						
						delegate.connect( _address );
	
					}else{
	
						connect_pool.run(
							new AERunnable()
							{
								public void
								runSupport()
								{									
									trace( "    delegating to I2P" );
									
									try{
										
											// remove the .i2p
										
										String new_externalised_address = externalised_address.substring( 0, externalised_address.length() - 4 );
										
								        socket = connectToAddress( new_externalised_address );
								       	
								        proxy_connection.connected();
								        
								        
									}catch( Throwable e ){
										
										try{
											proxy_connection.close();
											
										}catch( Throwable f ){
											
											f.printStackTrace();
										}
										
											//e.printStackTrace();
										
										trace( "I2PSocket creation fails: " + Debug.getNestedExceptionMessage(e) );
									}
								}
							});
						
						handling_connection = true;
					}
				}
			}finally{
				
				if ( !handling_connection ){
					
						// we've handed over control for this connection and won't hear about it again
					
					removeConnection( this );
				}
			}
		}
		
		public void
		relayData()
		
			throws IOException
		{
			synchronized( this ){
			
				if ( socket_closed ){
				
					throw( new IOException( "I2PPluginConnection::relayData: socket already closed"));
				}
			
				relay_state = new proxyStateRelayData( proxy_connection.getConnection());
			}
		}
		
		public void
		close()
		
			throws IOException
		{
			synchronized( this ){
			
				if ( socket != null && !socket_closed ){
					
					socket_closed	= true;
				
					if ( relay_state != null ){
						
						relay_state.close();
					}
					
					final I2PSocket	f_socket	= socket;
					
					socket	= null;
					
					AEThread2 t = 
						new AEThread2( "I2P SocketCloser" )
						{
							public void
							run()
							{
								try{
									f_socket.close();
									
								}catch( Throwable e ){
									
								}
							}
						};
					
					t.start();
				}
			}
			
			removeConnection( this );
		}
		
		protected class
		proxyStateRelayData
			implements AEProxyState
		{
			protected AEProxyConnection		connection;
			protected ByteBuffer			source_buffer;
			protected ByteBuffer			target_buffer;
			
			protected SocketChannel			source_channel;
			
			protected InputStream			input_stream;
			protected OutputStream			output_stream;
			
			protected long					outward_bytes	= 0;
			protected long					inward_bytes	= 0;
			
			protected AESemaphore			write_sem = new AESemaphore( "I2PSocket write sem" );
			
			protected ThreadPool			async_pool = new ThreadPool( "I2PSocket async", 2 );
			
			protected
			proxyStateRelayData(
				AEProxyConnection	_connection )
			
				throws IOException
			{		
				connection	= _connection;
				
				source_channel	= connection.getSourceChannel();
				
				source_buffer	= ByteBuffer.allocate( RELAY_BUFFER_SIZE );
				
				input_stream 	= socket.getInputStream();
				output_stream 	= socket.getOutputStream();

				connection.setReadState( this );
				
				connection.setWriteState( this );
				
				connection.requestReadSelect( source_channel );
							
				connection.setConnected();
				
				async_pool.run(
					new AERunnable()
					{
						public void
						runSupport()
						{
							byte[]	buffer = new byte[RELAY_BUFFER_SIZE];
							
							
							while( !connection.isClosed()){
							
								try{
									trace( "I2PCon: " + getStateName() + " : read Starts <- I2P " );

									long	start = System.currentTimeMillis();
									
									int	len = input_stream.read( buffer );
									
									if ( len <= 0 ){
										
										break;
									}
																
									trace( "I2PCon: " + getStateName() + " : read Done <- I2P - " + len + ", elapsed = " + ( System.currentTimeMillis() - start ));
									
									if ( target_buffer != null ){
										
										Debug.out("I2PluginConnection: target buffer should be null" );
									}
									
									target_buffer = ByteBuffer.wrap( buffer, 0, len );
									
									read();
									
								}catch( Throwable e ){
									
									boolean ignore = false;
									
									if ( e instanceof ClosedChannelException  ){
										
										ignore = true;
										
									}else if ( e instanceof IOException ){
										
										String message = e.getMessage();
										
										if ( message != null ){
											
											message = message.toLowerCase( Locale.US );
										
											if (	message.contains( "closed" ) ||
													message.contains( "aborted" ) ||
													message.contains( "disconnected" ) ||
													message.contains( "reset" )){
									
												ignore = true;
											}
										}
									}
									
									if ( !ignore ){
										
										Debug.out( e );
									}
									
									break;
								}
							}
							
							if ( !proxy_connection.isClosed()){
								
								try{
									proxy_connection.close();
									
								}catch( IOException e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					});
			}
			
			protected void
			close()
			{						
				trace( "I2PCon: " + getStateName() + " close" );
				
				write_sem.releaseForever();
			}
			
			protected void
			read()
			
				throws IOException
			{
					// data from I2P
				
				connection.setTimeStamp();
			
				int written = source_channel.write( target_buffer );
					
				trace( "I2PCon: " + getStateName() + " : write -> AZ - " + written );
				
				inward_bytes += written;
				
				if ( target_buffer.hasRemaining()){
				
					connection.requestWriteSelect( source_channel );
					
					write_sem.reserve();
				}
				
				target_buffer	= null;
			}
			
			public boolean
			read(
				SocketChannel 		sc )
			
				throws IOException
			{
				if ( source_buffer.position() != 0 ){
					
					Debug.out( "I2PluginConnection: source buffer position invalid" );
				}
				
					// data read from source
				
				connection.setTimeStamp();
																
				final int	len = sc.read( source_buffer );
		
				if ( len == 0 ){
					
					return( false );
				}
				
				if ( len == -1 ){
					
					throw( new EOFException( "read channel shutdown" ));
					
				}else{
					
					if ( source_buffer.position() > 0 ){
						
						connection.cancelReadSelect( source_channel );
						
						trace( "I2PCon: " + getStateName() + " : read <- AZ - " + len );
						
							// offload the write to separate thread as can't afford to block the
							// proxy
					
						async_pool.run(
							new AERunnable()
							{
								public void
								runSupport()
								{
									try{					
										source_buffer.flip();
										
										long	start = System.currentTimeMillis();
										
										trace( "I2PCon: " + getStateName() + " : write Starts -> I2P - " + len );
										
										output_stream.write( source_buffer.array(), 0, len );
						
										source_buffer.position( 0 );
										
										source_buffer.limit( source_buffer.capacity());
										
										output_stream.flush();
										
										trace( "I2PCon: " + getStateName() + " : write done -> I2P - " + len + ", elapsed = " + ( System.currentTimeMillis() - start ));
										
										outward_bytes += len;
										
										connection.requestReadSelect( source_channel );								

									}catch( Throwable e ){
										
										connection.failed( e );
									}
								}
							});			
					}
				}
				
				return( true );
			}
			
			public boolean
			write(
				SocketChannel 		sc )
			
				throws IOException
			{
				
				try{
					int written = source_channel.write( target_buffer );
						
					inward_bytes += written;
						
					trace( "I2PCon: " + getStateName() + " write -> AZ: " + written );
					
					if ( target_buffer.hasRemaining()){
										
						connection.requestWriteSelect( source_channel );
						
					}else{
						
						write_sem.release();
					}
					
					return( written > 0 );
					
				}catch( Throwable e ){
					
					write_sem.release();
					
					if (e instanceof IOException ){
						
						throw((IOException)e);
					}
					
					throw( new IOException( "write fails: " + Debug.getNestedExceptionMessage(e)));
				}
			}
			
			public boolean
			connect(
				SocketChannel	sc )
			
				throws IOException
			{
				throw( new IOException( "unexpected connect" ));
			}
			
			public String
			getStateName()
			{
				String	state = this.getClass().getName();
				
				int	pos = state.indexOf( "$");
				
				state = state.substring(pos+1);
				
				return( state  +" [out=" + outward_bytes +",in=" + inward_bytes +"] " + (source_buffer==null?"":source_buffer.toString()) + " / " + target_buffer );
			}
		}
	}
}
