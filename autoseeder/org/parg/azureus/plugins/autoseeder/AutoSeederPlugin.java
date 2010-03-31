/*
 * Created on 08-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.parg.azureus.plugins.autoseeder;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.download.*;


/**
 * @author parg
 *
 */

public class 
AutoSeederPlugin
	implements Plugin, UTTimerEventPerformer
{
	public static final String 	CONFIG_ENABLE		= "enable";
	public static final boolean CONFIG_ENABLE_DEFAULT	= true;
	
	public static final String 	CONFIG_TORRENT_DIR		= "torrent_dir";
	public static final String 	CONFIG_DATA_DIR			= "data_dir";
	
	public static final String	CONFIG_REFRESH			= "refresh";
	public static final int		CONFIG_REFRESH_DEFAULT	= 60;
	
	protected PluginInterface		plugin_interface;
	
	protected LoggerChannel			log;
	protected UTTimer				timer;
	protected UTTimerEvent			timer_event;
	
	protected BooleanParameter 		enable;
	protected DirectoryParameter 	torrent_dir;
	protected DirectoryParameter 	data_dir;
	
	protected List					downloads	= new ArrayList(); 
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		log	= plugin_interface.getLogger().getChannel( "AutoSeeder");
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "autoseeder.name");
						
		enable 		= config_model.addBooleanParameter2( CONFIG_ENABLE, "autoseeder.enable", CONFIG_ENABLE_DEFAULT );
		
		torrent_dir = config_model.addDirectoryParameter2( CONFIG_TORRENT_DIR, "autoseeder.torrentdir", "" );
		
		log.log( "Torrent directory = '" + torrent_dir.getValue() + "'" );
		
		data_dir = config_model.addDirectoryParameter2( CONFIG_DATA_DIR, "autoseeder.datadir", "" );
		
		log.log( "Data directory = '" + data_dir.getValue() + "'" );
		
		final IntParameter refresh 		= config_model.addIntParameter2( CONFIG_REFRESH, "autoseeder.refresh", CONFIG_REFRESH_DEFAULT );

		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "autoseeder.name" ));

		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	content )
					{
						view_model.getLogArea().appendText( content + "\n" );
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						view_model.getLogArea().appendText( str + "\n" );
						view_model.getLogArea().appendText( error.toString() + "\n" );
					}
				});
		
		view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
		
		enable.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter		p )
				{					
					view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
				}
			});	
		
		timer = plugin_interface.getUtilities().createTimer( "refresher");
		
		timer_event = timer.addPeriodicEvent( refresh.getValue() * 1000, this );
		
		log.log( "Refresh period = " + refresh.getValue() + " seconds" );
		
		refresh.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter		p )
				{
					timer_event.cancel();
					
					log.log( "Refresh period = " + refresh.getValue() + " seconds" );

					timer_event = timer.addPeriodicEvent( refresh.getValue()*1000, AutoSeederPlugin.this );
				}
			});
		
		enable.addEnabledOnSelection( torrent_dir );
		enable.addEnabledOnSelection( data_dir );
		enable.addEnabledOnSelection( refresh );
		
		process();
	}
	
	public void
	perform(
		UTTimerEvent		event )
	{
		process();
	}
	
	protected synchronized void
	process()
	{
		boolean	enabled = enable.getValue();
		
		if ( downloads.size() == 0 && !enabled){
			
			return;
		}
	
		TorrentManager	torrent_manager 	= plugin_interface.getTorrentManager();
		DownloadManager	download_manager	= plugin_interface.getDownloadManager();

		String str= new SimpleDateFormat("HH:mm:ss").format(new Date());
		
		log.log( "Directory scan starts at " + str );
		
		try{
			File	td = new File( torrent_dir.getValue());
			
			if ( !td.exists()){
				
				log.log( "Torrent directory '" + td.toString() + "' does not exist" );
				
				return;
			}
			
			if ( !td.isDirectory()){
				
				log.log( "Torrent directory '" + td.toString() + "' is not a directory" );
				
				return;
			}
			
			File	dd = new File( data_dir.getValue());
			
			if ( !dd.exists()){
				
				log.log( "Data directory '" + dd.toString() + "' does not exist" );
				
				return;
			}
			
			if ( !dd.isDirectory()){
				
				log.log( "Data directory '" + dd.toString() + "' is not a directory" );
				
				return;
			}	
		
			List	downloads_to_remove = new ArrayList(downloads);
			
			if ( enabled ){
				
				File[]	td_contents = td.listFiles();
				
				for (int i=0;i<td_contents.length;i++){
					
					File	file = td_contents[i];
					
					if ( file.isDirectory()){
						
						continue;
					}
					
					if ( file.getName().toLowerCase().endsWith(".torrent")){
						
						try{
							
							Torrent	torrent = torrent_manager.createFromBEncodedFile( file );
							
							Download dl = download_manager.getDownload( torrent );
							
							if ( dl != null ){
								
								downloads_to_remove.remove( dl );
								
									// already added
								
								continue;
							}
							
							torrent.setComplete( dd );
							
							File	torrent_file = File.createTempFile( "AZU", "torrent" );
							
							torrent.writeToFile( torrent_file );
							
							torrent_file.deleteOnExit();
							
							Download new_dl = download_manager.addNonPersistentDownload( torrent, torrent_file, dd );
							
							log.log( "adding download '" + torrent.getName() + "'" );
							
							downloads.add( new_dl );
							
							new_dl.addDownloadWillBeRemovedListener(
									new DownloadWillBeRemovedListener()
									{
										public void
										downloadWillBeRemoved(
											Download	download )
										
											throws DownloadRemovalVetoException
										{
											if ( downloads.contains(download)){
												
												throw( new DownloadRemovalVetoException( "Download can't be removed as it has been created by auto-seeder plugin"));
											}
										}
									});
							
						}catch( TorrentException e ){
							
							log.log( "processing of torrent '" + file.toString() + "' fails", e );
						}
					}
				}
			}
			
			for (int i=0;i<downloads_to_remove.size();i++){
					
				Download	dl = (Download)downloads_to_remove.get(i);
					
				downloads.remove(dl);
					
				log.log( "removing dowload '" + dl.getTorrent().getName() + "'" );
				
				try{
					
					dl.addListener(
						new DownloadListener()
						{
							public void
							stateChanged(
								Download		download,
								int				old_state,
								int				new_state )
							{
								if ( new_state == Download.ST_STOPPED ){
									
									try{
										download.remove();
										
									}catch( Throwable e ){
										
										log.log( "removal fails", e );
									}
								}
							}

							public void
							positionChanged(
								Download	download, 
								int 		oldPosition,
								int 		newPosition )
							{
								
							}
						});
					
					if ( dl.getState() == Download.ST_STOPPED ){
						
						dl.remove();
						
					}else{
						
						dl.stop();
					}
				}catch( Throwable e ){
					
					log.log( "removal fails", e );
				}
			}
		}catch( Throwable e ){
			
			log.log( "procesing fails", e );
			
		}finally{
			
			// log.log( "Directory scan complete" );
		}
	}
}
