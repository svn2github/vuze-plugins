/*
 * Created on Dec 15, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.networks.tor;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.plugins.*;

import com.aelitis.azureus.core.util.GeneralUtils;

public class 
TorPlugin
	implements UnloadablePlugin
{
	private int	socks_port		= 9100;
	private int control_port	= 9151;

	public void
	initialize(
		PluginInterface		pi )
	{
		
		File config_file = pi.getPluginconfig().getPluginUserFile( "config.txt" );
		
		File plugin_dir = config_file.getParentFile();
		
		final File data_dir = new File( plugin_dir, "data" );
		
		if ( !config_file.exists()){
			
			try{
					// appears that the local file system encoding needs to be used
				
				PrintWriter pw = new PrintWriter( new OutputStreamWriter( new FileOutputStream( config_file )));
				
				pw.println( "SocksPort 127.0.0.1:" + socks_port );
				pw.println( "ControlPort " + control_port );
				pw.println( "CookieAuthentication 1" );
				pw.println( "DataDirectory ." + File.separator + data_dir.getName());
				
				pw.close();
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		File exe_file = new File( plugin_dir, Constants.isWindows?"AzureusTor.exe":"AzureusTor" );
		
		int	pid = getPID();
			
		/*
		if ( Constants.isWindows ){
				
			String command = exe_file.getAbsolutePath() + " -f \"" + config_file + "\"";
			
			if ( pid >= 0 ){
					
				command += " __OwningControllerProcess " + pid;
			}
			
			try{
				PlatformManagerFactory.getPlatformManager().createProcess( command, false );
							
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		*/
		
		try{
			List<String>	cmd_list = new ArrayList<String>();
			
			cmd_list.add( exe_file.getAbsolutePath());
			cmd_list.add( "-f" );
			cmd_list.add( config_file.getName());
			
			if ( pid >= 0 ){
				
				cmd_list.add( "__OwningControllerProcess" );
				cmd_list.add( String.valueOf( pid ));
			}
			
			ProcessBuilder pb = GeneralUtils.createProcessBuilder( plugin_dir, cmd_list.toArray(new String[cmd_list.size()]), null );
		
			pb.start();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		new AEThread2( "ControlPortCon")
		{
			public void
			run()
			{				
				while( true ){
				
					ControlConnection control = new ControlConnection( data_dir, control_port );
				
					if ( control.connect()){
						
						break;
					}
					
					try{
						Thread.sleep(5000);
						
					}catch( Throwable f ){
						
					}
				}
			}
		}.start();
	}
	
	private int
	getPID()
	{
		try{
			RuntimeMXBean runtime_bean =	java.lang.management.ManagementFactory.getRuntimeMXBean();
			
			Field jvm_field = runtime_bean.getClass().getDeclaredField( "jvm" );
			
			jvm_field.setAccessible( true );
			
			Object jvm = jvm_field.get( runtime_bean );
			
			Method pid_method = jvm.getClass().getDeclaredMethod( "getProcessId" );
			
			pid_method.setAccessible( true );

			int pid = (Integer)pid_method.invoke( jvm );
			
			return( pid );
			
		}catch( Throwable e ){
			
			return( -1 );
		}
	}
	
	public void
	unload()
	{
		
	}
	
	private class
	ControlConnection
	{
		private int			port;
		private File		data_dir;
	
		private Socket				socket;
		private LineNumberReader 	lnr;
		private OutputStream 		os;
		
		private boolean		did_connect;
	
		private 
		ControlConnection(
			File		_data_dir,
			int			_port )
		{
			data_dir	= _data_dir;
			port		= _port;
		}
		
		private boolean
		connect()
		{
			try{
				socket = new Socket( "127.0.0.1", port );

				did_connect = true;
				
				InputStream is = socket.getInputStream();
							
				lnr = new LineNumberReader( new InputStreamReader( is ));

				os = socket.getOutputStream();
							
				byte[] client_nonce = new byte[32];
			
				RandomUtils.nextSecureBytes( client_nonce );
				
				writeLine( "AUTHCHALLENGE SAFECOOKIE " + ByteFormatter.encodeString( client_nonce ).toUpperCase());
				
				String reply = readLine();
				
				if ( !reply.startsWith( "250 AUTHCHALLENGE " )){
					
					throw( new Exception( "AUTHCHALLENGE response invalid: " + reply ));
				}
				
				File cookie_file = new File( data_dir, "control_auth_cookie" );
				
				byte[] cookie = FileUtil.readFileAsByteArray( cookie_file );
				
				reply = reply.substring( 18 ).trim();
				
				String[] bits = reply.split( " " );
				
				byte[] server_hash 	= ByteFormatter.decodeString( bits[0].substring( 11 ).trim());
				byte[] server_nonce = ByteFormatter.decodeString( bits[1].substring( 12 ).trim());
				
				Mac mac = Mac.getInstance("HmacSHA256");
				
				SecretKeySpec secret_key = new SecretKeySpec( "Tor safe cookie authentication server-to-controller hash".getBytes(Constants.BYTE_ENCODING), "HmacSHA256");
				
				mac.init( secret_key );
				
				mac.update( cookie );
				mac.update( client_nonce );
				mac.update( server_nonce );
				
				byte[] server_digest = mac.doFinal();
				
				if ( !Arrays.equals( server_hash, server_digest )){
					
					throw( new Exception( "AUTHCHALLENGE response server hash incorrect" ));
				}
											
				secret_key = new SecretKeySpec( "Tor safe cookie authentication controller-to-server hash".getBytes(Constants.BYTE_ENCODING), "HmacSHA256");

				mac.init( secret_key );
				
				mac.update( cookie );
				mac.update( client_nonce );
				mac.update( server_nonce );
				
				writeLine( "AUTHENTICATE " + ByteFormatter.encodeString( mac.doFinal()).toUpperCase());

				reply = readLine();
				
				if ( !reply.startsWith( "250 OK" )){
					
					throw( new Exception( "AUTHENTICATE response invalid: " + reply ));
				}
				
				writeLine( "TAKEOWNERSHIP" );
					
				reply = readLine();
					
				if ( !reply.startsWith( "250 OK" )){
					
					throw( new Exception( "TAKEOWNERSHIP response invalid: " + reply ));
				}
				
				writeLine( "RESETCONF __OwningControllerProcess" );
				
				reply = readLine();
					
				if ( !reply.startsWith( "250 OK" )){
					
					throw( new Exception( "TAKEOWNERSHIP response invalid: " + reply ));
				}
				
				System.out.println( "Connection to control port established" );
				
				return( true );
				
			}catch( Throwable e ){
				
				Debug.out( "Failed to connect to control socket: " + Debug.getNestedExceptionMessage( e ));
				
				close();
				
				return( false );
			}	
		}
		
		private void
		writeLine(
			String		str )
		
			throws IOException 
		{
			try{
				os.write( ( str + "\r\n" ).getBytes(Constants.BYTE_ENCODING));
			
				os.flush();
				
			}catch( IOException e ){
			
				close();
			}
		}
		
		private String
		readLine()
		
			throws IOException
		{
			String line = lnr.readLine();
			
			if ( line == null ){
				
				close();
				
				throw( new IOException( "Unexpected end of file" ));
			}
			
			return( line.trim());
		}
		
		private void
		close()
		{
			if ( lnr != null ){
				
				try{
					lnr.close();
					
				}catch( Throwable e ){
				}
				
				lnr = null;
			}
			
			if ( os != null ){
				
				try{
					os.close();
					
				}catch( Throwable e ){
				}
				
				os = null;
			}
			
			if ( socket != null ){
				
				try{
					socket.close();
					
				}catch( Throwable e ){
				}
				
				socket = null;
			}
		}
	}
}
