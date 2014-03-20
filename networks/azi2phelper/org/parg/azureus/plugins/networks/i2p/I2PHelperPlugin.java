/*
 * Created on Mar 17, 2014
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

import java.io.File;



import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;


public class 
I2PHelperPlugin 
	implements UnloadablePlugin
{	
	/*
	 * Router: commented out System.setProperties in static initialiser
	 * KRPC: Added code to persist DHT periodically in Cleaner
	 *  	System.out.println( "Persisting DHT" );
            boolean saveAll = _context.clock().now() - _started < 20*60*1000;
            PersistDHT.saveDHT(_knownNodes, saveAll, _dhtFile);
       DHT/KRPC: Added constructor/accessor to support persistent NID
	*/
	
	private PluginInterface		plugin_interface;
	
	private I2PHelperRouter		router;
	
	private boolean	unloaded;
	
	public void
	initialize(
		PluginInterface		pi )
	{
		try{
			plugin_interface	= pi;
			
			setUnloadable( true );
			
			plugin_interface.addListener(
				new PluginAdapter()
				{
					public void
					initializationComplete()
					{
						
					}
					
					public void
					closedownInitiated()
					{
						if ( router != null ){
							
							router.destroy();
						}
					}
				});
				
			new AEThread2("derp")
			{
				public void
				run()
				{
					router = new I2PHelperRouter( false );
					
					try{
						router.initialise( new File( "C:\\test\\i2phelper" ));
						
						while( true ){
							
							router.logInfo();
							
							Thread.sleep(1000);
						}
					}catch( Throwable e ){
						
						e.printStackTrace();
						
						router = null;
					}
				}
			}.start();
			
		}catch( Throwable e ){
			
			synchronized( this ){
			
				unloaded = true;
			}
			
			Debug.out( e );
		}
	}
	
	private void
	setUnloadable(
		boolean	b )
	{
		PluginInterface pi = plugin_interface;
		
		if ( pi != null ){
			
			pi.getPluginProperties().put( "plugin.unload.disabled", String.valueOf( !b ));
		}
	}
	
	public void
	unload()
	{
		synchronized( this ){
			
			unloaded = true;
		}
		
		if ( router != null ){
			
			router.destroy();
		}
	}
	

	
	
	public static void
	main(
		String[]	args )
	{
		if ( args.length == 0 ){
			
			System.out.println( "Usage: config_dir");
			
			return;
		}
		
		File config_dir = new File( args[0] );
		
		if ( !config_dir.isDirectory()){
			
			if ( !config_dir.mkdirs()){
				
				System.out.println( "Failed to make directory '" + config_dir + "'" );
				
				return;
			}
		}
		
		boolean bootstrap = args.length > 1 && args[1].equals( "true" );
		
		if ( bootstrap ){
			
			System.out.println( "Boostrap Node" );
		}
		
		try{
			I2PHelperRouter router = new I2PHelperRouter( bootstrap );
			
			router.initialise( config_dir );
			
			I2PHelperTracker tracker = new I2PHelperTracker( router.getDHT());
			
			I2PHelperConsole console = new I2PHelperConsole();
			
			while( true ){
				
				String line = console.readLine( ">" );
				
				if ( line == null ){
					
					break;
				}
				
				try{
					line = line.trim();
					
					String[]	bits = line.split(" ");
					
					String cmd = bits[0].toLowerCase();
					
					if ( cmd.equals( "quit" )){
						
						break;
						
					}else if ( cmd.equals( "announce" )){
												
						byte[] hash = bits.length==1?new byte[20]: ByteFormatter.decodeString( bits[1] );
					
						tracker.announce( hash );
						
					}else if ( cmd.equals( "crawl" )){
						
						router.getDHT().crawl();
						
					}else if ( cmd.equals( "print" )){
						
						router.getDHT().print();
						
					}else{
						
						router.logInfo();
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
			
			router.destroy();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
