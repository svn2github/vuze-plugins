/*
 * Created on Jun 26, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */



package com.azureus.plugins.trackerurladder;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAnnounceURLList;
import org.gudy.azureus2.plugins.torrent.TorrentAnnounceURLListSet;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;


public class 
TrackerURLAdderPlugin 
	implements UnloadablePlugin, DownloadManagerListener
{
	private PluginInterface plugin_interface;
	
	public void
	initialize(
		PluginInterface		_pi )
	{
		plugin_interface = _pi;
		
		plugin_interface.getDownloadManager().addListener( this );
		
		UIManager	ui_manager	= plugin_interface.getUIManager();
							
		TableManager	table_manager = ui_manager.getTableManager();
						
		String[] tables = {
			TableManager.TABLE_MYTORRENTS_INCOMPLETE,
			TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
			TableManager.TABLE_MYTORRENTS_COMPLETE,
			TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
			TableManager.TABLE_MYTORRENTS_ALL_BIG,
			TableManager.TABLE_MYTORRENTS_UNOPENED,
			TableManager.TABLE_MYTORRENTS_UNOPENED_BIG,
		};
		
		for ( String table: tables ){

		
			TableContextMenuItem scan_menu = 
				table_manager.addContextMenuItem(  
					table,
					"aztrackerurladder.addem" );		
			
			scan_menu.addFillListener(
				new MenuItemFillListener()
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		target )
					{
						TableRow[]	rows = (TableRow[])target;
						
						boolean	ok = false;
													
						for (int i=0;i<rows.length;i++){
	
							TableRow	row = rows[i];
							
							Object ds = row.getDataSource();
							
							if ( ds instanceof Download ){
							
								if ( isEditable((Download)ds)){
									
									ok	= true;
									
									break;
								}
							}
						}
						
						menu.setEnabled( ok );
					}
				});
			
			scan_menu.addMultiListener(
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 			menu, 
						final Object 		target )
					{
						final PluginConfig config = plugin_interface.getPluginconfig();
						
						String[] urls = config.getPluginStringListParameter( "url_list" );
						String url_str = "";
						
						for ( String u: urls ){
							
							url_str += u + "\r\n";
						}
						
						SimpleTextEntryWindow text_entry = new SimpleTextEntryWindow();
						
						text_entry.setTitle( "aztrackerurladder.urls.title");
						
						text_entry.setMessage( "aztrackerurladder.urls.msg");
						
						text_entry.setPreenteredText( url_str, false );
						
						text_entry.setLineHeight( 16 );
						
						text_entry.setMultiLine(true);
						
						text_entry.prompt(
							new UIInputReceiverListener() 
							{
								public void 
								UIInputReceiverClosed(
									UIInputReceiver text_entry) 
								{
									if ( text_entry.hasSubmittedInput()){
										
										String value = text_entry.getSubmittedInput();
	
										String[]	_lines = value.split( "\n" );
										
										List<String>	c_lines 	= new ArrayList<String>();
										List<String>	urls 		= new ArrayList<String>();
										
										String bad = "";
										
										for ( String l: _lines ){
											
											l = l.trim();
											
											if ( l.length() > 0 ){
												
												c_lines.add( l );
												
												try{
													new URL( l );
													
													urls.add( l );
													
												}catch( Throwable e ){
													
													Debug.out( "Bad URL: " + l );
													
													bad += "\r\n" + l;
												}
											}
										}
										
										config.setPluginStringListParameter( "url_list", c_lines.toArray( new String[ c_lines.size()] ));
										
										if ( bad.length() > 0 ){
																					
					    					MessageBoxShell mb = new MessageBoxShell(
					    							SWT.ICON_ERROR | SWT.OK,
					    							MessageText.getString("aztrackerurladder.error.title"),
					    							MessageText.getString("aztrackerurladder.error.urls",
					    									new String[] { bad }));
					    					
				    					
					    					mb.open(null);
					    				
										}else{
											
											TableRow[]	rows = (TableRow[])target;
													
											int	downloads_processed = 0;
											int urls_added			= 0;
											
											for ( int i=0;i<rows.length;i++ ){
																							
												TableRow	row = rows[i];
												
												Object ds = row.getDataSource();
												
												if ( ds instanceof Download ){
												
													Download download = (Download)ds;
													
													if ( isEditable(download )){
														
														downloads_processed++;
														
														urls_added += addURLs( download, urls );
														
													}
												}
											}
											
					    					MessageBoxShell mb = new MessageBoxShell(
					    							SWT.ICON_INFORMATION | SWT.OK,
					    							MessageText.getString("aztrackerurladder.added.title"),
					    							MessageText.getString("aztrackerurladder.added.urls",
					    									new String[] { String.valueOf( downloads_processed ), String.valueOf( urls_added ) }));
					    					
				    					
					    					mb.open(null);
										}
									}
								}
							});
					}
				});
		}
	}
	
	private int
	addURLs(
		Download			download,
		List<String>		_urls_to_add )
	{
		List<String>	urls_to_add = new ArrayList<String>( _urls_to_add );
		
		Torrent torrent = download.getTorrent();
		
		int	added = 0;
		
		if ( torrent != null ){

			TorrentAnnounceURLList urls_list = torrent.getAnnounceURLList();
			
			TorrentAnnounceURLListSet[] sets = urls_list.getSets();
			
			for ( TorrentAnnounceURLListSet set: sets ){
				
				URL[] urls = set.getURLs();
				
				for ( URL u: urls ){
					
					urls_to_add.remove( u.toExternalForm());
				}
			}
			
			for ( String u: urls_to_add ){
				
				try{
					urls_list.addSet( new URL[]{ new URL( u )});
					
					added ++;
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		return( added );
	}
	
	private boolean
	isEditable(
		Download	download )
	{
		if ( download.getFlag( Download.FLAG_LOW_NOISE )){
			
			return( false );
		}
		
		Torrent torrent = download.getTorrent();
		
		if ( torrent != null ){
			
			TOTorrent to_torrent = PluginCoreUtils.unwrap( torrent );
			
			if ( TorrentUtils.isReallyPrivate( to_torrent )){
				
				return ( false );
			}
		}
		
		return( true );
	}
	
	public void
	downloadAdded(
		Download	download )
	{
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
	}
	
	public void
	unload()
	{
		if ( plugin_interface != null ){
			
			plugin_interface.getDownloadManager().removeListener( this );
			
			plugin_interface = null;
		}
	}
}
