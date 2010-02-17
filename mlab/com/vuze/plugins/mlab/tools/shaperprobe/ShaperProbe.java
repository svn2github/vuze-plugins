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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;


public class 
ShaperProbe 
{
	public static void
	run(
		PluginInterface		plugin_interface )
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
			
			final boolean[] done = {false};
			
			new AEThread2( "ProcessReader" )
			{
				private JFrame 		frame;
				private JTextArea	area;
				
				public void
				run()
				{
					frame = new JFrame( "ShaperProbe" );
					
					area = new JTextArea();
					
					JScrollPane	scroller = new JScrollPane( area );
					
					frame.getContentPane().add( scroller, BorderLayout.CENTER );
										
					frame.addWindowListener(
						new WindowAdapter() 
						{
							public void 
							windowClosing(
								WindowEvent e )
							{
								frame.dispose();
							}
						});
					
					frame.setSize( new Dimension( 400, 600 ));
						
					Toolkit kit = Toolkit.getDefaultToolkit();
					
					Dimension screenSize = kit.getScreenSize();

					int screenWidth = (int)screenSize.getWidth(); 
					
					int screenHeight = (int)screenSize.getHeight();

					frame.setLocation(
							((screenWidth / 2) - (frame.getWidth()/2)),
							((screenHeight / 2) - (frame.getHeight()/2)));
					
					frame.setVisible( true );
					
					String	line = "";
					
					try{						
						while( true ){
							
							byte[] buffer = new byte[32*1024];
							
							int	len = is.read( buffer );
							
							if ( len <= 0 ){
								
								break;
							}									
							
							line += new String( buffer, 0, len );
								
							System.out.println( line );
							
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
						
						if ( line.length() > 0 ){
							
							logLine( line );
						}
						
						Debug.out( e );
					}
				}
				
				protected void
				logLine(
					String	str )
				{
					if ( area != null ){
						
						area.setText( area.getText() + str + "\n" );
					}
				}
			}.start();
			
			try{
				process.waitFor();
				
			}finally{
				
				synchronized( done ){
				
					done[0] = true;
				}
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
}
