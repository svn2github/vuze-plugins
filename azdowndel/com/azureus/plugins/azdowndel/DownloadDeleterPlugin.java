/*
 * Created on Nov 7, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.azureus.plugins.azdowndel;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

public class 
DownloadDeleterPlugin 
	implements Plugin
{
	private PluginInterface		plugin_interface;
	private LoggerChannel		log;

	private TorrentAttribute 	ta_category;
	
	private TorrentAttribute 	ta_delete_enable;
	private TorrentAttribute 	ta_start_enable;
	
	private int					target_sr;
	private IntParameter		del_idle;
	private BooleanParameter	del_idle_if_complete;

	private StringParameter		del_cats;
	private StringParameter		start_cats;
	
	
	private BooleanParameter	enable_del_all;

	private BooleanParameter	delete_torrent;
	private BooleanParameter	delete_data;
	private BooleanParameter	start_if_space;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
		
		throws PluginException 
	{	
		plugin_interface = _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel( "DownloadDeleter" );
		
		ta_category			= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_CATEGORY );
		ta_delete_enable	= plugin_interface.getTorrentManager().getPluginAttribute( "delete_enable" );
		ta_start_enable		= plugin_interface.getTorrentManager().getPluginAttribute( "start_enable" );

		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		loc_utils.integrateLocalisedMessageBundle( "com.azureus.plugins.azdowndel.internat.Messages");
		
		final String plugin_name = plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "azdowndel.name" );
				
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azdowndel.name" ));

		view_model.setConfigSectionID( "azdowndel.name" );
		
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
						if ( str.length() > 0 ){
							view_model.getLogArea().appendText( str + "\n" );
						}
						
						StringWriter sw = new StringWriter();
						
						PrintWriter	pw = new PrintWriter( sw );
						
						error.printStackTrace( pw );
						
						pw.flush();
						
						view_model.getLogArea().appendText( sw.toString() + "\n" );
					}
				});
		
		log.setDiagnostic();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azdowndel.name");
	
		config_model.addLabelParameter2( "azdowndel.info" );
		
			// del
		
		enable_del_all		= config_model.addBooleanParameter2( "azdowndel.del_enable_all", "azdowndel.del_enable_all", false );
	
		final StringParameter sr_param = config_model.addStringParameter2( "azdowndel.sr", "azdowndel.sr", "1.0" );
		
		sr_param.setGenerateIntermediateEvents( false );
		
		setTargetSR( sr_param );
		
		sr_param.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					setTargetSR( sr_param );
				}
			});
		
		del_idle 		= config_model.addIntParameter2( "azdowndel.del_idle", "azdowndel.del_idle", 0 );

		del_idle_if_complete	= config_model.addBooleanParameter2( "azdowndel.del_idle.if_comp", "azdowndel.del_idle.if_comp", true );

		if ( del_idle.getValue() == 0 ){
			
			del_idle_if_complete.setEnabled( false );
		}
		
		del_idle.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					del_idle_if_complete.setEnabled( del_idle.getValue() > 0 );
				}
			});
		
		del_cats 		= config_model.addStringParameter2( "azdowndel.del_cats", "azdowndel.del_cats", "" );

		delete_torrent 	= config_model.addBooleanParameter2( "azdowndel.del_torrent", "azdowndel.del_torrent", true );
		delete_data		= config_model.addBooleanParameter2( "azdowndel.del_data", "azdowndel.del_data", true );
			
		config_model.createGroup(
				"azdowndel.group.del",
				new Parameter[]{ 
						enable_del_all, sr_param, del_idle, del_idle_if_complete, 
						del_cats, delete_torrent, delete_data });

		
			// start
		
		start_cats 		= config_model.addStringParameter2( "azdowndel.start_cats", "azdowndel.start_cats", "" );

		start_if_space		= config_model.addBooleanParameter2( "azdowndel.start_if_space", "azdowndel.start_if_space", true );

		config_model.createGroup(
			"azdowndel.group.start",
			new Parameter[]{ start_cats, start_if_space });
		
		
		enable_del_all.addDisabledOnSelection( del_cats );
		
		TableManager	table_manager = ui_manager.getTableManager();

		TableContextMenuItem	root_menu1 = 
			table_manager.addContextMenuItem(  
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				"azdowndel.contextmenu" );
		
		TableContextMenuItem	root_menu2 = 
			table_manager.addContextMenuItem(  
				TableManager.TABLE_MYTORRENTS_COMPLETE,
				"azdowndel.contextmenu" );
		
		TableContextMenuItem[]	menus = { root_menu1, root_menu2 };
		
		for (int i=0;i<menus.length;i++){

			TableContextMenuItem root_menu = menus[i];
						
			root_menu.setStyle( MenuItem.STYLE_MENU );	
			
			{
				final TableContextMenuItem del_menu = table_manager.addContextMenuItem( root_menu, "azdowndel.contextmenu.del_enable" );
				
				del_menu.setStyle( MenuItem.STYLE_CHECK );	
				
				del_menu.setData( new Boolean( false ));
				
				del_menu.addFillListener(
						new MenuItemFillListener()
						{
							public void
							menuWillBeShown(
								MenuItem	menu,
								Object 		target )
							{
								TableRow[]	rows = (TableRow[])target;
								
								boolean	all_enabled	 		= true;
								boolean	all_disabled		= true;
								boolean	all_cant_be_enabled	= true;
								
								for (int i=0;i<rows.length;i++){
									
									Download	download = (Download)rows[i].getDataSource();
		
									boolean	enabled = isDeleteEnabled( download, false );
									
									if ( enabled ){
										
										all_disabled = false;
										
									}else{
										
										all_enabled = false;
									}
									
									if ( canDeleteBeEnabled( download )){
										
										all_cant_be_enabled	= false;	
									}
								}
								
								if ( all_enabled ){
									
									del_menu.setEnabled( true );
									
									del_menu.setData( new Boolean( true ));
									
								}else if ( all_disabled ){
									
									del_menu.setData( new Boolean( false ));
		
									del_menu.setEnabled( !all_cant_be_enabled );
		
								}else{
									
									del_menu.setEnabled( false );
								}
								
								if ( enable_del_all.getValue()){
									
									del_menu.setEnabled( false );
								}
							}
						});
				
				del_menu.addListener(
					new MenuItemListener()
					{
						public void
						selected(
							MenuItem	menu,
							Object 		target )
						{
							TableRow	row = (TableRow)target;
							
							Download	download = (Download)row.getDataSource();
							
							setDeleteEnabled( 
								download, ((Boolean)del_menu.getData()).booleanValue());
						}
					});
			}
			
			{
				final TableContextMenuItem start_menu = table_manager.addContextMenuItem( root_menu, "azdowndel.contextmenu.start_enable" );
				
				start_menu.setStyle( MenuItem.STYLE_CHECK );	
				
				start_menu.setData( new Boolean( false ));
				
				start_menu.addFillListener(
						new MenuItemFillListener()
						{
							public void
							menuWillBeShown(
								MenuItem	menu,
								Object 		target )
							{
								TableRow[]	rows = (TableRow[])target;
								
								boolean	all_enabled	 		= true;
								boolean	all_disabled		= true;
								boolean	all_cant_be_enabled	= true;
								
								for (int i=0;i<rows.length;i++){
									
									Download	download = (Download)rows[i].getDataSource();
		
									boolean	enabled = isStartEnabled( download, false );
									
									if ( enabled ){
										
										all_disabled = false;
										
									}else{
										
										all_enabled = false;
									}
									
									if ( canStartBeEnabled( download )){
										
										all_cant_be_enabled	= false;	
									}
								}
								
								if ( all_enabled ){
									
									start_menu.setEnabled( true );
									
									start_menu.setData( new Boolean( true ));
									
								}else if ( all_disabled ){
									
									start_menu.setData( new Boolean( false ));
		
									start_menu.setEnabled( !all_cant_be_enabled );
		
								}else{
									
									start_menu.setEnabled( false );
								}
							}
						});
				
				start_menu.addListener(
					new MenuItemListener()
					{
						public void
						selected(
							MenuItem	menu,
							Object 		target )
						{
							TableRow	row = (TableRow)target;
							
							Download	download = (Download)row.getDataSource();
							
							setStartEnabled( 
								download, ((Boolean)start_menu.getData()).booleanValue());
						}
					});
			}
		}
		
		plugin_interface.getUtilities().createTimer( "Download Deleter" ).addPeriodicEvent(
			30*1000,
			new UTTimerEventPerformer()
			{
				public void 
				perform(
					UTTimerEvent event )
				{
					int	idle_days = del_idle.getValue();

					if ( target_sr <= 0 && idle_days <= 0 ){
						
						return;
					}
					
					Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
					
					for ( Download download: downloads ){
						
						int	state = download.getState();
	
						if ( state != Download.ST_ERROR ){
																							
							boolean	meets_share_ratio = false;
							
							if ( target_sr > 0 && download.isComplete()){

								int sr = download.getStats().getShareRatio();

								meets_share_ratio = sr >= target_sr;
							}
							
							long idle_up_secs = download.getStats().getSecondsSinceLastUpload();	
		
							boolean meets_idle_up = false;
								
							if ( 	idle_days > 0 && 
									idle_up_secs > 0 &&
									idle_up_secs >= idle_days*24*60*60 ){
																										
								if ( del_idle_if_complete.getValue()){
									
									meets_idle_up = download.isComplete();
									
								}else{
									
									meets_idle_up = true;
								}
							}
							
							if ( meets_share_ratio || meets_idle_up ){
								
								if ( isDeleteEnabled( download, true )){
							
									if ( meets_share_ratio ){
										
										log.log( "'" + download.getName() + "' meets share ratio for deletion" );
									}
									
									if ( meets_idle_up ){
										
										log.log( "'" + download.getName() + "' meets no-upload criteria for deletion" );
									}
									
									if ( state == Download.ST_QUEUED || state == Download.ST_SEEDING ){
										
										try{
											log.log( "Stopping '" + download.getName() + "' for deletion" );
											
											download.stop();
											
										}catch( Throwable e ){
										
											log.log( "Failed to stop '" + download.getName() + "'", e );
										}
									}else if ( state == Download.ST_STOPPED ){
										
										try{
											log.log( "Deleting '" + download.getName() + "'" );
											
											download.remove( delete_torrent.getValue(), delete_data.getValue());
											
											tryStart();
											
										}catch( Throwable e ){
											
											log.log( "Failed to delete '" + download.getName() + "'", e );
										}
									}
								}
							}
						}
					}
				}
				
				protected void
				tryStart()
				{
					Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
											
					for ( Download download: downloads ){
						
						int	state = download.getState();
	
						if ( state == Download.ST_STOPPED && isStartEnabled( download, true )){
							
							if ( download.getTorrent() == null ){
								
								continue;
							}

							boolean start;
							
							if ( start_if_space.getValue()){
								
								try{
									long space = new File(download.getSavePath()).getUsableSpace();
									
									long allocated	= 0;
									long size		= download.getTorrent().getSize();
									
									DiskManagerFileInfo[] files = download.getDiskManagerFileInfo();
									
									for ( DiskManagerFileInfo file: files ){
										
										File f = file.getFile();
										
										if ( f.exists()){
											
											allocated += f.length();
										}
									}
									
									start = space > (size - allocated );
									
									if ( start ){
										
										log.log( "Starting '" + download.getName() + "': available=" + space + ", required=" + size + " (allocated=" + allocated + ")" );

									}else{
										log.log( "Insufficient space to start '" + download.getName() + "': available=" + space + ", required=" + size + " (allocated=" + allocated + ")" );
									}
								}catch( Throwable e ){
									
									log.log( "Failed to get free space for '" + download.getName() + "'", e );
									
									start = false;
								}
							}else{
								
								start = true;
							}
							
							if ( start ){
								
								try{
									download.restart();
									
									setStartEnabled( download, false );
									
									return;
									
								}catch( Throwable e ){
									
									log.log( "Failed to start '" + download.getName() + "'", e );
								}
							}
						}
					}
					
					log.log( "Can't start a download, none found or insufficient space" );
				}
			});
	}

	protected void
	setTargetSR(
		StringParameter		param )
	{
		try{
			target_sr = new BigDecimal( param.getValue() ).multiply( new BigDecimal( 1000 )).intValue();
			
			log.log( "Set target share ratio to " + param.getValue());
			
		}catch( Throwable e ){
			
			log.log( "Invalid share ratio '" + param.getValue());
		}
	}
	
	protected boolean
	isInCategory(
		Download		download,
		String			cats_str )
	{
		if ( cats_str == null ){
			
			return( false );
		}
		
		String[]	cats = cats_str.split( "," );
		
		if ( cats.length > 0 ){
			
			String cat = download.getAttribute( ta_category );
			
			if ( cat != null ){
				
				for (String c: cats ){
					
					if ( c.equals( cat )){
						
						return( true );
					}
				}
			}
		}
		
		return( false );
	}
	
	protected boolean
	isDeleteEnabled(
		Download		download,
		boolean			check_category )
	{
		if ( enable_del_all.getValue() || download.getBooleanAttribute( ta_delete_enable )){
			
			return( true );
		}
		
		if ( check_category ){
		
			return( isInCategory( download, del_cats.getValue()));
		}
		
		return( false );
	}
	
	protected boolean
	canDeleteBeEnabled(
		Download		download )
	{
		return( !enable_del_all.getValue() && download.getState() != Download.ST_ERROR && download.getTorrent() != null );	
	}
	
	protected void
	setDeleteEnabled(
		Download		download,
		boolean			enabled )
	{
		download.setBooleanAttribute( ta_delete_enable, enabled );
	}
	
	protected boolean
	isStartEnabled(
		Download		download,
		boolean			check_category )
	{
		if ( download.getBooleanAttribute( ta_start_enable )){
			
			return( true );
		}
		
		if ( check_category ){
			
			return( isInCategory( download, start_cats.getValue()));
		}
		
		return( false );
	}
	
	protected boolean
	canStartBeEnabled(
		Download		download )
	{
		return( download.getState() == Download.ST_STOPPED && download.getTorrent() != null );	
	}
	
	protected void
	setStartEnabled(
		Download		download,
		boolean			enabled )
	{
		download.setBooleanAttribute( ta_start_enable, enabled );
	}
}
