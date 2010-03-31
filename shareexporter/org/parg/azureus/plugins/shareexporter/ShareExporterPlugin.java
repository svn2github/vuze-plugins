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

package org.parg.azureus.plugins.shareexporter;

import java.io.*;
import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.sharing.ShareResource;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.sharing.*;


/**
 * @author parg
 *
 */

public class 
ShareExporterPlugin
	implements Plugin
{
	public static final String 	CONFIG_ENABLE		= "enable";
	public static final boolean CONFIG_ENABLE_DEFAULT	= true;
	
	public static final String 	CONFIG_TORRENT_DIR		= "torrent_dir";
	
	public static final String 	CONFIG_REMOVE_TORRENTS			= "remove_torrents";
	public static final boolean CONFIG_REMOVE_TORRENTS_DEFAULT	= false;
	
	public static final String 	CONFIG_STRUCTURE_CONTENTS			= "structure_contents";
	public static final boolean CONFIG_STRUCTURE_CONTENTS_DEFAULT	= false;

	
	public static final String	CONFIG_CATEGORY_LIST			= "include_categories";	
	public static final String	CONFIG_CATEGORY_LIST_DEFAULT	= "";
	
	public static final String	CONFIG_CATEGORY_EXCLUDE_LIST			= "exclude_categories";	
	public static final String	CONFIG_CATEGORY_EXCLUDE_LIST_DEFAULT	= "";

	public static final String	CONFIG_CATEGORY_SUBFOLDER_LIST			= "subfolder_categories";	
	public static final String	CONFIG_CATEGORY_SUBFOLDER_LIST_DEFAULT	= "";

	public static final String	CATEGORY_UNKNOWN	= "Uncategorized";

	private PluginInterface		plugin_interface;
	
	private LoggerChannel		log;
	
	private BooleanParameter 	enable;
	private DirectoryParameter 	torrent_dir;
	private BooleanParameter 	remove_torrents;
	private BooleanParameter 	structure_contents;
	
	protected TorrentAttribute		torrent_categories;

	private String				last_category_list = "";
	private Map					enabled_category_map	= new HashMap();
	
	private String				last_category_exclude_list = "";
	private Map					disabled_category_map	= new HashMap();

	private String				last_category_subfolder_list 	= "";
	private Map					subfolder_category_map			= new HashMap();

	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		torrent_categories = plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_CATEGORY );
		
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		log	= plugin_interface.getLogger().getChannel( "ShareExporter");
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "shareexporter.name");
						
		enable 		= config_model.addBooleanParameter2( CONFIG_ENABLE, "shareexporter.enable", CONFIG_ENABLE_DEFAULT );
		
		torrent_dir = config_model.addDirectoryParameter2( CONFIG_TORRENT_DIR, "shareexporter.torrentdir", "" );
		
		remove_torrents		= config_model.addBooleanParameter2( CONFIG_REMOVE_TORRENTS, "shareexporter.remove_torrents", CONFIG_REMOVE_TORRENTS_DEFAULT );
		
		structure_contents		= config_model.addBooleanParameter2( CONFIG_STRUCTURE_CONTENTS, "shareexporter.structurecontents", CONFIG_STRUCTURE_CONTENTS_DEFAULT );

				
		final StringParameter	category_list		= config_model.addStringParameter2( CONFIG_CATEGORY_LIST, "shareexporter.categorylist", CONFIG_CATEGORY_LIST_DEFAULT );
		
		final StringParameter	category_exclude_list		= config_model.addStringParameter2( CONFIG_CATEGORY_EXCLUDE_LIST, "shareexporter.categoryexcludelist", CONFIG_CATEGORY_EXCLUDE_LIST_DEFAULT );
		
		final StringParameter	category_subfolder_list		= config_model.addStringParameter2( CONFIG_CATEGORY_SUBFOLDER_LIST, "shareexporter.categorysubfolderlist", CONFIG_CATEGORY_SUBFOLDER_LIST_DEFAULT );
		
		config_model.createGroup( "shareexporter.category_group",
				new Parameter[]{ 
						category_list,
						category_exclude_list,
						category_subfolder_list });	

		log.log( "Torrent directory = '" + torrent_dir.getValue() + "'" );
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "shareexporter.name" ));

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
		
		enable.addEnabledOnSelection( torrent_dir );
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
						// hack alert - we need to ensure that we register as a listener
						// *after* the share-hoster plugin, as this sets the categories of
						// newly added shares
					
					plugin_interface.getUtilities().createThread(
						"ListenerAdder",
						new Runnable()
						{
							public void
							run()
							{
								try{
									Thread.sleep(10000);
									
									plugin_interface.getShareManager().addListener(
										new ShareManagerListener()
										{
											public void
											resourceAdded(
												ShareResource		resource )
											{
												process( resource, 0 );
											}
											
											public void
											resourceModified(
												ShareResource		resource )
											{
												process( resource, 1 );
											}
											
											public void
											resourceDeleted(
												ShareResource		resource )
											{
												process( resource, 2 );
											}
											
											public void
											reportProgress(
												int		percent_complete )
											{
												
											}
											
											public void
											reportCurrentTask(
												String	task_description )
											{
												
											}
										});
									
									ShareResource[]	shares = plugin_interface.getShareManager().getShares();
									
									for (int i=0;i<shares.length;i++){
										
										process( shares[i], 0 );
									}
									
								}catch( Throwable e ){
									
									log.log(e);
								}
							}
						});
				}
				
				public void
				closedownInitiated()
				{
				}
				
				public void
				closedownComplete()
				{
				}
			});

	}
		
	protected synchronized void
	process(
		ShareResource	share,
		int				action )		// 0 = add, 1 = mod, 2 = remove
	{
		boolean	enabled = enable.getValue();
		
		if ( !enabled){
			
			return;
		}
	
		log.log( "Procesing share '" + share.getName()  +"'");
		
		if ( !new File(torrent_dir.getValue()).exists()){
			
			log.logAlert( LoggerChannel.LT_ERROR, "ShareExporter: Torrent directory '" + torrent_dir.getValue() + "' doesn't exist" );
			
			return;
		}
		
		int	type = share.getType();

		Torrent	torrent = null;
		
		try{
			if ( type == ShareResource.ST_DIR ){
				
				torrent	= ((ShareResourceDir)share).getItem().getTorrent();
				
			}else if ( type == ShareResource.ST_FILE ){
				
				torrent	= ((ShareResourceFile)share).getItem().getTorrent();	
			}
					
			if ( torrent != null ){
			
				File	dir 	= new File( torrent_dir.getValue());

				String	sub_folder_str = getSubFolder( share );
				
				if ( sub_folder_str != null ){
					
					dir = new File( dir, sub_folder_str );
				}
				
				if ( structure_contents.getValue()){
					
					ShareResourceDirContents	current = share.getParent();
					
					if ( current != null ){
						
						String	root = current.getRoot().getParent().toString();
						
						String	leaf = new File( share.getName()).getParent();
						
						String	diff = leaf.substring( root.length());
						
						dir = new File( dir.toString() + diff );
					}
				}
				
				if ( !dir.exists()){
						
					dir.mkdirs();
				}
				
				File	file = new File(dir, torrent.getName() + ".torrent");

				if ( 	action == 1 || 
						( action == 2 && remove_torrents.getValue())){
					
					if ( file.delete()){
						
						log.log( "    deleted " + file.toString());

					}
				}
				
				if ( action == 0 || action == 1 ){
				
					if ( isCategoryEnabled( share )){
						
						torrent.writeToFile( file );
				
						log.log( "    wrote " + file.toString());
						
					}else{
						
						if ( file.exists()){
							
							log.log( "    deleting " + file.toString() + ", category not enabled" );

							file.delete();
							
						}else{
						
							log.log( "    not writing " + file.toString() + ", category not enabled" );
						}
					}
				}
			}
		}catch( Throwable e ){
			
			log.log( e );
		}
	}
	
	public boolean
	isCategoryEnabled(
		ShareResource		resource )
	{
		String	category_list = plugin_interface.getPluginconfig().getPluginStringParameter( CONFIG_CATEGORY_LIST, CONFIG_CATEGORY_LIST_DEFAULT ).trim();
		
		if ( !category_list.equals( last_category_list )){
		
			Map	new_map = new HashMap();
			
			if ( category_list.length() > 0 ){
				
				StringTokenizer tok = new StringTokenizer( category_list, ";" );
				
				while( tok.hasMoreTokens()){
					
					String	cat = tok.nextToken().trim();
					
					if ( cat.length() > 0 ){
						
						new_map.put( cat, cat );
					}
				}		
			}
			
			enabled_category_map	= new_map;
			
			last_category_list	= category_list;
		}
		
		String	category_exclude_list = plugin_interface.getPluginconfig().getPluginStringParameter( CONFIG_CATEGORY_EXCLUDE_LIST, CONFIG_CATEGORY_EXCLUDE_LIST_DEFAULT ).trim();
		
		if ( !category_exclude_list.equals( last_category_exclude_list )){
		
			Map	new_map = new HashMap();
			
			if ( category_exclude_list.length() > 0 ){
				
				StringTokenizer tok = new StringTokenizer( category_exclude_list, ";" );
				
				while( tok.hasMoreTokens()){
					
					String	cat = tok.nextToken().trim();
					
					if ( cat.length() > 0 ){
						
						new_map.put( cat, cat );
					}
				}		
			}
			
			disabled_category_map	= new_map;
			
			last_category_exclude_list	= category_exclude_list;
		}
		
		String	cat_name = getCategoryName( resource );

		if ( enabled_category_map.size() == 0 ){
			
				// none explicitly enabled -> only disable explicitly disabled ones 
			
			return( disabled_category_map.get(cat_name) == null );
		}
		
		return( 	enabled_category_map.get( cat_name ) != null &&
				 	disabled_category_map.get( cat_name ) == null );
	}
	
	public String
	getSubFolder(
		ShareResource		resource )
	{
		String	subfolder_list = plugin_interface.getPluginconfig().getPluginStringParameter( CONFIG_CATEGORY_SUBFOLDER_LIST, CONFIG_CATEGORY_SUBFOLDER_LIST_DEFAULT ).trim();
		
		if ( !subfolder_list.equals( last_category_subfolder_list )){
		
			Map	new_map = new HashMap();
			
			if ( subfolder_list.length() > 0 ){
				
				StringTokenizer tok = new StringTokenizer( subfolder_list, ";" );
				
				while( tok.hasMoreTokens()){
					
					String	cat = tok.nextToken().trim();
					
					if ( cat.length() > 0 ){
						
						new_map.put( cat, cat );
					}
				}		
			}
			
			subfolder_category_map	= new_map;
			
			last_category_subfolder_list	= subfolder_list;
		}
		
		String	category = getCategoryName( resource );
		
		if ( category == CATEGORY_UNKNOWN ){
			
			return( null );
		}
		
		if ( 	subfolder_category_map.get( "all" ) != null ||
				subfolder_category_map.get(category) != null ){
			
			return( category );
		}
		
		return( null );
	}
	
	protected String
	getCategoryName(
		ShareResource		resource )
	{		
		String	c = resource.getAttribute( torrent_categories);
		
		if ( c == null || c.equals( "Categories.uncategorized" )){
			
			c = CATEGORY_UNKNOWN;
		}
	
		return( c );
	}
}
