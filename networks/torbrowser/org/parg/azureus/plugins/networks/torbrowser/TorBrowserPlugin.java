/*
 * Created on Jan 6, 2014
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


package org.parg.azureus.plugins.networks.torbrowser;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;

public class 
TorBrowserPlugin
	implements UnloadablePlugin
{
	private PluginInterface		plugin_interface;
	private File				browser_dir;
	
	public void
	initialize(
		PluginInterface		pi )
	
		throws PluginException 
	{
		plugin_interface = pi;
		
		try{
			File plugin_install_dir = new File( pi.getPluginDirectoryName());
			
			File plugin_data_dir	= pi.getPluginconfig().getPluginUserFile( "test" ).getParentFile();
			
			if ( !plugin_data_dir.exists()){
				
				plugin_data_dir.mkdirs();
			}
			
			File[]	install_files = plugin_install_dir.listFiles();
			
			List<File>	old_zip_files = new ArrayList<File>();
			
			String 	highest_version_zip			= "0";
			File	highest_version_zip_file	= null;
			
			for ( File file: install_files ){
				
				String name = file.getName();
				
				if ( file.isFile() && name.startsWith( "browser-" ) && name.endsWith( ".zip" )){
					
					String version = name.substring( name.lastIndexOf( "_" ) + 1, name.length() - 4 );
					
					if ( Constants.compareVersions( version, highest_version_zip ) > 0 ){
						
						highest_version_zip = version;
						
						if ( highest_version_zip_file != null ){
							
							old_zip_files.add( highest_version_zip_file );
						}
						
						highest_version_zip_file = file;
					}
				}
			}
			
			File[]	data_files = plugin_data_dir.listFiles();
			
			String 	highest_version_data		= "0";
			File	highest_version_data_file 	= null;
			
			for ( File file: data_files ){
				
				String name = file.getName();
				
				if ( file.isDirectory() && name.startsWith( "browser_" )){
					
					String version = name.substring( 8 );
					
					if ( Constants.compareVersions( version, highest_version_data ) > 0 ){
						
						highest_version_data = version;
						
						highest_version_data_file = file;
					}
				}
			}
						
			if ( Constants.compareVersions( highest_version_zip, highest_version_data ) > 0 ){
				
				File temp_data = new File( plugin_data_dir, "tmp_" + highest_version_zip );
				
				if ( temp_data.exists()){
					
					if ( !FileUtil.recursiveDeleteNoCheck( temp_data )){
						
						throw( new Exception( "Failed to remove tmp directory: " + temp_data ));
					}
				}
				
				ZipInputStream zis = null;
				
				try{
					zis = new ZipInputStream( new BufferedInputStream( new FileInputStream( highest_version_zip_file ) ));
							
					byte[] buffer = new byte[64*1024];
					
					while( true ){
						
						ZipEntry	entry = zis.getNextEntry();
							
						if ( entry == null ){
							
							break;
						}
					
						String	name = entry.getName();
					
						if ( name.endsWith( "/" )){
							
							continue;
						}
						
						if ( File.separatorChar != '/' ){
							
							name = name.replace( '/', File.separatorChar );
						}
						
						File target_out = new File( temp_data, name );
						
						File parent_folder = target_out.getParentFile();
						
						if ( !parent_folder.exists()){
							
							parent_folder.mkdirs();
						}
						
						OutputStream	entry_os = null;

						try{
							entry_os = new FileOutputStream( target_out );
							
							while( true ){
								
								int	len = zis.read( buffer );
								
								if ( len <= 0 ){
									
									break;
								}
																											
								entry_os.write( buffer, 0, len );
							}
						}finally{
							
							if ( entry_os != null ){
								
								try{
									entry_os.close();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					}
				}finally{
					
					if ( zis != null ){
						
						try{
							zis.close();
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
				
					// migrate any existing profile data
				
				if ( highest_version_data_file != null ){
					
					File	old_profile = new File( highest_version_data_file, "Data" );
					File	new_profile = new File( temp_data, "Data" );
					
					copyProfile( old_profile, new_profile );
					
				}
				
				File target_data = new File( plugin_data_dir, "browser_" + highest_version_zip );
				
				if ( target_data.exists()){
					
					throw( new Exception( "Target already exists: " + target_data ));
				}
				
				if ( !temp_data.renameTo( target_data )){
					
					throw( new Exception( "Failed to rename " + temp_data + " to " + target_data ));
				}
				
				for ( File old: old_zip_files ){
					
					old.delete();
				}
				
				browser_dir = target_data;
				
			}else{
				
				File existing_data = new File( plugin_data_dir, "browser_" + highest_version_data );

				if ( highest_version_data.equals( "0" ) || !existing_data.exists()){
					
					throw( new Exception( "No browser version installed" ));
				}
								
				browser_dir = existing_data;
			}
			
			plugin_interface.addListener(
				new PluginAdapter()
				{
					public void
					initializationComplete()
					{
						try{
							checkConfig();
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				});
				
		}catch( Throwable e ){
			
			throw( new PluginException( "Initialisation failed: " + Debug.getNestedExceptionMessage( e )));
		}
	}
	
	private void
	copyProfile(
		File	from_dir,
		File	to_dir )
	
		throws Exception
	{
		if ( !from_dir.isDirectory()){
			
			return;
		}
		
		File[] from_files = from_dir.listFiles();
		
		for ( File from_file: from_files ){
			
			File to_file = new File( to_dir, from_file.getName());
			
			if ( from_file.isDirectory()){
				
				if ( !to_file.exists()){
					
					if ( !to_file.mkdir()){
						
						throw( new Exception( "Failed to create dir: " + to_file ));
					}
				}
				
				copyProfile( from_file, to_file );
				
			}else{
				
				if ( to_file.exists()){
					
					if ( !to_file.delete()){
						
						throw( new Exception( "Failed to delete file: " + to_file ));
					}
				}
				
				if ( !FileUtil.copyFile( from_file, to_file )){
					
					throw( new Exception( "Failed to copy file: " + from_file + " -> " + to_file ));
				}
			}
		}
	}
	
	private void
	checkConfig()
	
		throws Exception
	{
		int	socks_port = 26699;
		
		Map<String,Object> user_pref = new HashMap<String, Object>();
		 
		user_pref.put("browser.startup.homepage", "check.torproject.org");
		user_pref.put("network.proxy.no_proxies_on", "127.0.0.1");
		user_pref.put("network.proxy.socks_port", socks_port );
		
		Map<String,Object> ext_pref = new HashMap<String, Object>();

		ext_pref.put("extensions.torbutton.fresh_install", true );
		ext_pref.put("extensions.torbutton.tor_enabled", true);
		ext_pref.put("extensions.torbutton.proxies_applied", false );
		ext_pref.put("extensions.torbutton.settings_applied", false );
		ext_pref.put("extensions.torbutton.socks_host", "127.0.0.1");
		ext_pref.put("extensions.torbutton.socks_port", socks_port );
		ext_pref.put("extensions.torbutton.custom.socks_host", "127.0.0.1");
		ext_pref.put("extensions.torbutton.custom.socks_port", socks_port );
		ext_pref.put("extensions.torbutton.settings_method", "custom");

		
		File	root = browser_dir;
		
		if ( root == null ){
			
			throw( new Exception( "Browser not installed" ));
		}
		
		char slash = File.separatorChar;
		
		File	profile_dir = new File( root, "Data" + slash + "Browser" + slash + "profile.default" );
		
		File	user_prefs_file = new File( profile_dir, "prefs.js" );
		
		fixPrefs( user_prefs_file, "user_pref", user_pref );
		
		File	ext_prefs_file = new File( profile_dir, "preferences" + slash + "extension-overrides.js" );
		
		fixPrefs( ext_prefs_file, "pref", ext_pref );
	}
	
	private void
	fixPrefs(
		File				file,
		String				pref_key,
		Map<String,Object>	prefs )
	
		throws Exception
	{
		List<String>	lines = new ArrayList<String>();
		
		boolean updated = false;
		
		Map<String,Object>	prefs_to_add = new HashMap<String, Object>( prefs );
		
		if ( file.exists()){
			
			LineNumberReader	lnr = null;
			
			try{
				lnr = new LineNumberReader( new InputStreamReader( new FileInputStream( file ), "UTF-8" ));
				
				while( true ){
					
					String 	line = lnr.readLine();
					
					if ( line == null ){
						
						break;
					}
					
					line = line.trim();
					
					boolean	handled = false;
					
					if ( line.startsWith( pref_key )){
						
						int	pos1 = line.indexOf( "\"" );
						int	pos2 = line.indexOf( "\"", pos1+1 );
						
						if ( pos2 > pos1 ){
							
							String key = line.substring( pos1+1, pos2 );
							
							Object	required_value = prefs_to_add.remove( key );
							
							if ( required_value != null ){
								
								pos1 = line.indexOf( ",", pos2 + 1 );
								pos2 = line.indexOf( ")", pos1+1 );
								
								if ( pos2 > pos1 ){
									
									String	current_str = line.substring( pos1+1, pos2 ).trim();
									
									String 	required_str;
									
									if ( required_value instanceof String ){
										
										required_str = "\"" + required_value + "\"";
									}else{
										
										required_str = String.valueOf( required_value );
									}
									
									if ( !current_str.equals( required_str )){
										
										lines.add( pref_key + "(\"" + key + "\", " + required_str + ");");
										
										updated	= true;
										handled = true;
									}
								}else{
									
									throw( new Exception( "Couldn't parse line: " + line ));
								}
							}
						}
					}
					
					if ( !handled ){
						
						lines.add( line );
					}
				}
			}finally{
				
				if ( lnr != null ){
					
					lnr.close();
				}
			}
		}
		
		if ( prefs_to_add.size() > 0 ){
		
			for ( Map.Entry<String, Object> entry: prefs_to_add.entrySet()){
				
				Object val = entry.getValue();
				
				if ( val instanceof String ){
					
					val = "\"" + val + "\"";
				}
				
				lines.add( pref_key + "(\"" + entry.getKey() + "\", " + val + ");");
			}
			
			updated = true;
		}
		
		if ( updated ){
			
			File temp_file 	= new File( file.getAbsolutePath() + ".tmp" );
			File bak_file 	= new File( file.getAbsolutePath() + ".bak" );
			
			temp_file.delete();
			
			try{
				PrintWriter writer = new PrintWriter( new OutputStreamWriter( new FileOutputStream( temp_file ), "UTF-8" ));
				
				try{
					for ( String line: lines ){
					
						writer.println( line );
					}
				}finally{
					
					writer.close();
				}
				
				bak_file.delete();
				
				if ( file.exists()){
					
					if ( !file.renameTo( bak_file )){
						
						throw( new Exception( "Rename of " + file + " to " + bak_file + " failed" ));
					}
				}
					
				if ( !temp_file.renameTo( file )){
						
					if ( bak_file.exists()){
						
						bak_file.renameTo( file );
					}
					
					throw( new Exception( "Rename of " + temp_file + " to " + file + " failed" ));
				}
			}catch( Throwable e ){
				
				temp_file.delete();
				
				throw( new Exception( "Failed to udpate " + file, e ));
			}	
		}
	}
	
	public void 
	unload() 
			
		throws PluginException 
	{
		browser_dir 		= null;
		plugin_interface	= null;
	}
}
