/*
 * Created on Jan 29, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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


package com.vuze.plugins.mlab.tools.shaperprobe;

import java.io.*;

/*
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
*/

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;


public class 
ShaperProbe 
{
	private long	up_bps;
	private long	down_bps;
	
	private long	shape_up_bps;
	private long	shape_down_bps;
	
	
	public static ShaperProbe
	run(
		PluginInterface				plugin_interface,
		final ShaperProbeListener	listener )
	{
		ShaperProbe sp = new ShaperProbe( plugin_interface, listener );
		
		return( sp );
	}
		
	private
	ShaperProbe(
		PluginInterface				plugin_interface,
		final ShaperProbeListener	listener )
	{
		try{	
			String[] command;
			
			if ( Constants.isOSX ){
				
				File app = new File( plugin_interface.getPluginDirectoryName(),	"ShaperProbe" );
				
				runCommand(
					new String[]{
						"killall",
						"-9",
						"ShaperProbe"
					});

				runCommand(
					new String[]{
						"chmod",
						"+x",
						app.getAbsolutePath().replaceAll(" ", "\\ ")
					});
					
				command = new String[]{ app.getAbsolutePath() };

			}else{
				
				File app = new File( plugin_interface.getPluginDirectoryName(),	"ShaperProbeC.exe");
				
				command = new String[]{ app.getAbsolutePath() };
			}
			
			Process process = Runtime.getRuntime().exec( command );
			
			final InputStream 	is 	= process.getInputStream();
			final OutputStream	os 	= process.getOutputStream();
			final InputStream 	es 	= process.getErrorStream();
			
			final AESemaphore processor_sem = new AESemaphore( "waiter" );
			
			new AEThread2( "ProcessReader:is" )
			{
				private int	mode = 0;
				
				public void
				run()
				{					
					String	line = "";
					
					try{						
						while( true ){
							
							byte[] buffer = new byte[32*1024];
							
							int	len = is.read( buffer );
							
							if ( len <= 0 ){
								
								break;
							}									
							
							line += new String( buffer, 0, len );
															
							while( true ){
								
								int	pos = line.indexOf( '\n' );
								
								if ( pos == -1 ){
										
									break;
								}
								
								String	x = line.substring(0,pos).trim();
								
								if ( x.length() > 0 ){
									
									logLine( x );
								}
								
								line = line.substring(pos+1);
								
							}
						}
					}catch( Throwable e ){
						
						Debug.out( e );
						
					}finally{
						
						try{
							if ( line.length() > 0 ){
								
								logLine( line );
							}
						}finally{
							
							processor_sem.release();
						}
					}
				}
				
				protected void
				logLine(
					String	str )
				{
					if ( str.contains( "traffic shapers" )){
						
						mode	= 1;
					}
					
					if ( mode == 0 ){
					
						if ( up_bps == 0 && str.startsWith( "Upstream:" )){
							
							up_bps = getLong( str.substring( 9 ));
							
						}else if ( down_bps == 0 && str.startsWith( "Downstream:" )){
							
							down_bps = getLong( str.substring( 11 ));
	
						}
					}else{
						
						if ( str.contains( "Downstream:" )){
							
							mode = 2;
							
						}else{
							
							if ( str.contains( "Shaping rate:" )){
								
								long rate = getLong( str.substring( 13 ));
								
								if ( mode == 1 ){
								
									shape_up_bps = rate;
									
								}else{
									
									shape_down_bps = rate;
								}
								
							}
						}
					}
					listener.reportSummary( str );
				}
				
				protected long
				getLong(
					String	str )
				{
					str = str.trim();
					
					int	pos = str.indexOf( ' ' );
					
					if ( pos != -1 ){
						
						str = str.substring( 0, pos ).trim();
					}
					
						// kbps
					
					return( Long.parseLong( str ) * 1024 );
				}
			}.start();
			
			
			new AEThread2( "ProcessReader:es" )
			{
				public void
				run()
				{	
					String	line = "";
					
					try{						
						while( true ){
							
							byte[] buffer = new byte[32*1024];
							
							int	len = es.read( buffer );
							
							if ( len <= 0 ){
								
								break;
							}									
							
							line += new String( buffer, 0, len );
															
							while( true ){
								
								int	pos = line.indexOf( '\n' );
								
								if ( pos == -1 ){
									
									break;
								}
								
								String	x = line.substring(0,pos).trim();
								
								if ( x.length() > 0 ){
									
									logLine( x );
								}
								
								line = line.substring(pos+1);
								
							}
						}
					}catch( Throwable e ){
						
						Debug.out( e );
						
					}finally{
						
						try{
							if ( line.length() > 0 ){
								
								logLine( line );
							}
						}finally{
						
							processor_sem.release();
						}
					}
				}
				
				protected void
				logLine(
					String	str )
				{
					listener.reportSummary( str );
				}
			}.start();
			
			try{
				process.waitFor();
				
			}finally{
				
				processor_sem.reserve();
				processor_sem.reserve();
				
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	private static void
	runCommand(
		String[]	command )
	{
		try{
			command[0] = findCommand( command[0] );
		
			Runtime.getRuntime().exec( command ).waitFor();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	private static String
	findCommand(
		String	name )
	{
		final String[]  locations = { "/bin", "/usr/bin" };

		for ( String s: locations ){

			File f = new File( s, name );

			if ( f.exists() && f.canRead()){

				return( f.getAbsolutePath());
			}
		}

		return( name );
	}
	
	public long
	getUpBitsPerSec()
	{
		return( up_bps );
	}
	
	public long
	getDownBitsPerSec()
	{
		return( down_bps );
	}
	
	public long
	getShapeUpBitsPerSec()
	{
		return( shape_up_bps );
	}
	
	public long
	getShapeDownBitsPerSec()
	{
		return( shape_down_bps );
	}
}
