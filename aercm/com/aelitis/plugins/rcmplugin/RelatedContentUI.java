/*
 * Created on Jul 14, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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

package com.aelitis.plugins.rcmplugin;

import java.util.*;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.content.*;
import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

public class 
RelatedContentUI 
{		
	public static final String SIDEBAR_SECTION_RELATED_CONTENT = "RelatedContent";
	
	private static RelatedContentUI		singleton;
	
	public synchronized static RelatedContentUI
	getSingleton(
		PluginInterface		pi,
		RCMPlugin			plugin )
	{
		if ( singleton == null || singleton.isDestroyed()){
			
			if ( pi == null || plugin == null ){
				
				return( null );
			}
			
			singleton = new RelatedContentUI( pi, plugin );
		}
		
		return( singleton );
	}
	
	private PluginInterface		plugin_interface;
	private RCMPlugin			plugin;
	
	private BasicPluginConfigModel 	config_model;
	private BooleanParameter 		enable_ui;
	private BooleanParameter 		enable_search;
	
	private RelatedContentManager			manager;
	private RelatedContentManagerListener	rcm_listener;
	
	private boolean			ui_setup;
	private boolean			root_menus_added;
	
	private List<TableContextMenuItem>	menus = new ArrayList<TableContextMenuItem>();
		
	private ByteArrayHashMap<RCMItem>	rcm_item_map = new ByteArrayHashMap<RCMItem>();
	
	private AsyncDispatcher	async_dispatcher = new AsyncDispatcher();
	
	
	private volatile boolean	destroyed = false;

	private TableContextMenuItem[] menu_items;

	private 
	RelatedContentUI(
		PluginInterface	_plugin_interface, 
		RCMPlugin		_plugin )
	{
		plugin_interface	= _plugin_interface;
		plugin				= _plugin;
		
		CoreWaiterSWT.waitForCoreRunning(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core ) 
				{
					uiAttachedAndCoreRunning( core );
				}
			});
	}
	
	protected RCMPlugin
	getPlugin()
	{
		return( plugin );
	}
	
	protected void
	destroy()
	{
		destroyed	= true;
		
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{					
					if ( config_model != null ){
						
						config_model.destroy();
					}
					
					try{
						for ( TableContextMenuItem menu: menus ){
							
							menu.remove();
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
					
					try{
						MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

						if ( mdi != null ){
							
							MdiEntry mdi_entry = mdi.getEntry( SIDEBAR_SECTION_RELATED_CONTENT );
							
							if ( mdi_entry != null ){
								
								mdi_entry.close( true );
							}
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
					
					if ( manager != null ){
						
						try{
							if ( rcm_listener != null ){
								
								manager.removeListener( rcm_listener );								
							}
						}catch( Throwable e ){
							
							Debug.out( e );
						}
						
						manager 		= null;
						rcm_listener 	= null;

					}
				
					singleton	= null;
				}
			});
	}
	
	private boolean
	isDestroyed()
	{
		return( destroyed );
	}
	
	private void 
	uiAttachedAndCoreRunning(
		AzureusCore core ) 
	{
		if ( destroyed ){
			
			return;
		}
		
		
		
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		
		if ( mdi != null ){
			
			setupUI(mdi);
			
		} else {
			
			SkinViewManager.addListener(
				new SkinViewManagerListener() 
				{
					public void 
					skinViewAdded(
						SkinView skinview ) 
					{
						if ( destroyed ){
							
							SkinViewManager.RemoveListener(this);
							
							return;
						}
						
						if (skinview instanceof SideBar) {
						
							setupUI((SideBar) skinview);
							
							SkinViewManager.RemoveListener(this);
						}
					}
				});
		}
	}
	

	
	protected void
	setupUI(
		MultipleDocumentInterface			mdi )	
	{
		synchronized( this ){
			
			if ( ui_setup ){
				
				return;
			}
			
			ui_setup = true;
		}
		
		try{	
			manager 	= RelatedContentManager.getSingleton();

			UIManager			ui_manager = plugin_interface.getUIManager();

			config_model = 
				ui_manager.createBasicPluginConfigModel( "Associations" );
			
			config_model.addHyperlinkParameter2( "rcm.plugin.wiki", MessageText.getString( "rcm.plugin.wiki.url" ));

			ActionParameter action = config_model.addActionParameter2( null, "rcm.show.ftux" );
			
			action.addListener(
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter param ) 
					{
						showFTUX(null);
					}		
				});

			enable_ui = 
				config_model.addBooleanParameter2( 
					"rcm.ui.enable", "rcm.ui.enable",
					true );
			
			enable_ui.addListener(new ParameterListener() {
				public void parameterChanged(Parameter param) {
					hookUI();
				}
			});
			
			enable_search = 
				config_model.addBooleanParameter2( 
					"rcm.search.enable", "rcm.search.enable",
					false );

			enable_search.addListener(new ParameterListener() {
				public void parameterChanged(Parameter param) {
					plugin.hookSearch();
					plugin.updatePluginInfo();
				}
			});

				// max results
			
			final IntParameter max_results = 
				config_model.addIntParameter2( 
					"rcm.config.max_results", "rcm.config.max_results",
					manager.getMaxResults());
			
			max_results.addListener(
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param) 
						{
							manager.setMaxResults( max_results.getValue());
						}
					});
			
				// max level
			
			final IntParameter max_level = 
				config_model.addIntParameter2( 
					"rcm.config.max_level", "rcm.config.max_level",
					manager.getMaxSearchLevel());
			
			max_level.addListener(
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param) 
						{
							manager.setMaxSearchLevel( max_level.getValue());
						}
					});

				// overall enable
			
			final BooleanParameter overall_disable = 
				config_model.addBooleanParameter2( 
					"rcm.overall.disable", "rcm.overall.disable",
					!plugin.isRCMEnabled());
			
			overall_disable.setMinimumRequiredUserMode( 
					overall_disable.getValue()?Parameter.MODE_BEGINNER:Parameter.MODE_INTERMEDIATE );
			
			overall_disable.addListener(
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param) 
						{
							if (plugin.setRCMEnabled( !overall_disable.getValue())) {
  							MessageBoxShell mb = new MessageBoxShell(
  									MessageText.getString("rcm.restart.title"),
  									MessageText.getString("rcm.restart.text"),
  									new String[] {
  										MessageText.getString("UpdateWindow.restart"),
  										MessageText.getString("UpdateWindow.restartLater"),
  									}, 0);
  							mb.open(new UserPrompterResultListener() {
  								public void prompterClosed(int result) {
  									if (result != 0) {
  										return;
  									}
  									UIFunctions uif = UIFunctionsManager.getUIFunctions();
  									if (uif != null) {
  										uif.dispose(true, false);
  									}
  								}
  							});
							}

						}
					});

			
			overall_disable.addDisabledOnSelection( enable_ui  );
			overall_disable.addDisabledOnSelection( enable_search  );
			overall_disable.addDisabledOnSelection( max_results  );
			overall_disable.addDisabledOnSelection( max_level  );

			
			if (Constants.IS_CVS_VERSION) {
				config_model.addBooleanParameter2("rcm.ftux.shown", "!Debug:Was Welcome Shown!", false);
			}

			buildSideBar( new MainViewInfo() );

			COConfigurationManager.addAndFireParameterListener("rcm.overall.enabled",
					new org.gudy.azureus2.core3.config.ParameterListener() {
						public void parameterChanged(String parameterName) {
							overall_disable.setValue(!plugin.isRCMEnabled());
							hookUI();
							plugin.updatePluginInfo();
						}
					});

		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}

	private void
	hookUI()
	{
		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (plugin.isRCMEnabled() && enable_ui.getValue()) {
			mdi.loadEntryByID(SIDEBAR_SECTION_RELATED_CONTENT, false);
			hookMyTorrentMenus(true);
		} else {
			mdi.closeEntry(SIDEBAR_SECTION_RELATED_CONTENT);
			hookMyTorrentMenus(false);
		}

		plugin.hookSearch();
	}
	
	protected void
	hookMyTorrentMenus(boolean enable)
	{
		if (enable && menu_items != null) {
			return;
		}
		
		if (!enable) {
			if (menu_items != null){ 
				for (TableContextMenuItem menuitem : menu_items) {
					menuitem.remove();
				}
				menu_items = null;
			}
			return;
		}

		TableManager	table_manager = plugin_interface.getUIManager().getTableManager();

		String[]	table_ids = {
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				TableManager.TABLE_MYTORRENTS_COMPLETE,
				TableManager.TABLE_MYTORRENTS_ALL_BIG,
				TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
		};
		
		menu_items = new TableContextMenuItem[table_ids.length];
		
		for (int i = 0; i < table_ids.length; i++) {
			String table_id = table_ids[i];
			
			menu_items[i] = table_manager.addContextMenuItem( table_id, "rcm.contextmenu.lookupassoc");
		
			menus.add( menu_items[i] );
			
			menu_items[i].setStyle( TableContextMenuItem.STYLE_PUSH );

			MenuItemListener listener = 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						TableRow[]	rows = (TableRow[])target;
						
						if ( rows.length > 0 ){
							
							Download download = (Download)rows[0].getDataSource();
							
							explicitSearch( download );
						}
					}
				};
				
				menu_items[i].addMultiListener( listener );
		}
		
	}
	
	protected void
	explicitSearch(
		Download		download )
	{
		addSearch( download );
	}
	
	protected void
	buildSideBar(
		final MainViewInfo main_view_info )
	{		
		final String parent_id = "sidebar." + SIDEBAR_SECTION_RELATED_CONTENT;

		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		
		final RelatedContentManager f_manager = manager;

		mdi.registerEntry(SIDEBAR_SECTION_RELATED_CONTENT, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				
				// might be called by auto-open
				if (!plugin.isRCMEnabled() || !enable_ui.getValue()) {
					return null;
				}
				
				MdiEntry mdiEntry = mdi.createEntryFromSkinRef(
						null,
						SIDEBAR_SECTION_RELATED_CONTENT, "rcmview",
						main_view_info.getTitle(),
						main_view_info, null, true, null  );

				mdiEntry.setImageLeftID( "image.sidebar.rcm" );
				
				PluginConfig plugin_config = plugin_interface.getPluginconfig();
				
				if ( plugin_config.getPluginBooleanParameter( "rcm.sidebar.initial.show", true )){
					
					String parent_id = mdiEntry.getParentID();
				
					if ( parent_id != null ){
					
						MdiEntry parent = mdi.getEntry( parent_id );
						
						if ( parent != null ){
							
							parent.setExpanded( true );
							
							plugin_config.setPluginParameter( "rcm.sidebar.initial.show", false );
						}
					}
				}
				
				mdiEntry.setDatasource(
					new RelatedContentEnumerator()
					{
						private RelatedContentManagerListener base_listener;
						
						private RelatedContentEnumeratorListener current_listener;
						
						public void
						enumerate(
							RelatedContentEnumeratorListener	listener )
						{
							current_listener = listener;
							
							if ( base_listener == null ){
																
								base_listener = 
									new RelatedContentManagerListener()
									{
										public void
										contentFound(
											RelatedContent[]	content )
										{
											if ( destroyed ){
												
													// use final ref here as manager will be nulled
												
												f_manager.removeListener( base_listener );
												
											}else{
											
												current_listener.contentFound( content );
											}
										}
										
										public void
										contentChanged(
											RelatedContent[]	content )
										{
										}
										
										public void 
										contentRemoved(
											RelatedContent[] 	content ) 
										{
										}
										
										public void 
										contentChanged() 
										{
										}
										
										public void
										contentReset()
										{
										}
									};
									
									f_manager.addListener( base_listener );
							}
							
							RelatedContent[] current_content = f_manager.getRelatedContent();
							
							listener.contentFound( current_content );
						}
					});

				return mdiEntry;
			}
		});
		
		addMdiMenus(parent_id);
		
		rcm_listener = 
			new RelatedContentManagerListener()
			{
				private int last_unread;
				
				public void
				contentFound(
					RelatedContent[]	content )
				{
					check();
				}

				public void
				contentChanged(
					RelatedContent[]	content )
				{
					contentChanged();
				}
				
				public void 
				contentChanged() 
				{
					check();
					
					List<RCMItem>	items;
					
					synchronized( RelatedContentUI.this ){
						
						items = new ArrayList<RCMItem>( rcm_item_map.values());
					}
					
					for ( RCMItem item: items ){
						
						item.updateNumUnread();
					}
				}
				
				public void 
				contentRemoved(
					RelatedContent[] content ) 
				{
					check();
					
					List<RCMItem>	items;
					
					synchronized( RelatedContentUI.this ){
						
						items = new ArrayList<RCMItem>( rcm_item_map.values());
					}
					
					for ( RCMItem item: items ){
						
						item.contentRemoved( content );
					}
				}
				
				public void
				contentReset()
				{
					check();
				}
				
				protected void
				check()
				{
					int	unread = f_manager.getNumUnread();
					
					synchronized( this ){
						
						if ( unread == last_unread ){
							
							return;
						}
						
						last_unread = unread;
					}
					
					ViewTitleInfoManager.refreshTitleInfo( main_view_info );
				}
			};
			
			f_manager.addListener( rcm_listener );
	}
	
	private void addMdiMenus(String parent_id) {
		if ( !root_menus_added ){
			
			root_menus_added = true;
			
			UIManager			ui_manager = plugin_interface.getUIManager();

			MenuManager menu_manager = ui_manager.getMenuManager();

			MenuItem menu_item;
			
			menu_item = menu_manager.addMenuItem( MenuManager.MENU_MENUBAR, "rcm.view.heading" );

			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
							MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
							mdi.showEntryByID(SIDEBAR_SECTION_RELATED_CONTENT);
						}
					});


			menu_item = menu_manager.addMenuItem( parent_id, "rcm.menu.findsubs" );
			
			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
					      	lookupSubscriptions();
						}
					});

			menu_item = menu_manager.addMenuItem( parent_id, "sep1" );

			menu_item.setStyle( MenuItem.STYLE_SEPARATOR );
		
			menu_item = menu_manager.addMenuItem( parent_id, "v3.activity.button.readall" );
			
			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
					      	manager.setAllRead();
						}
					});
			
			menu_item = menu_manager.addMenuItem( parent_id, "Subscription.menu.deleteall");
			
			menu_item.addListener(
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
					      	manager.deleteAll();
						}
					});
			
			menu_item = menu_manager.addMenuItem( parent_id, "Subscription.menu.reset" );
			
			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
							for ( RCMItem item: rcm_item_map.values()){
								
								if (item.getTreeItem() != null) {
									item.getTreeItem().dispose();
								}
							}
							
					      	manager.reset();
						}
					});
			
			
			menu_item = menu_manager.addMenuItem( parent_id, "sep2" );

			menu_item.setStyle( MenuItem.STYLE_SEPARATOR );
			
			menu_item = menu_manager.addMenuItem( parent_id, "MainWindow.menu.view.configuration" );
			
			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
					      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
					      	 
					      	 if ( uif != null ){
					      		 
					      		 uif.openView( UIFunctions.VIEW_CONFIG, "Associations" );
					      	 }
						}
					});
		}
	}

	protected void
	addSearch(
		final Download		download )
	{
		Torrent	torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return;
		}
		
		final byte[] hash = torrent.getHash();
		
		addSearch( hash, download.getName());
	}
	
	protected void
	addSearch(
		final byte[]		hash,
		final String		name )
	{
		synchronized( this ){
			
			final RCMItem existing_si = rcm_item_map.get( hash );
			
			if (  existing_si == null ){
	
				final RCMItem new_si = new RCMItemContent( hash );
				
				rcm_item_map.put( hash, new_si );
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( RelatedContentUI.this ){

								if ( new_si.isDestroyed()){
									
									return;
								}
								
								RCMView view = new RCMView( SIDEBAR_SECTION_RELATED_CONTENT, name );
								
								new_si.setView( view );
								
								String key = "RCM_" + ByteFormatter.encodeString( hash );
								
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								MdiEntry	entry = mdi.createEntryFromSkinRef(
										SIDEBAR_SECTION_RELATED_CONTENT,
										key, "rcmview",
										view.getTitle(),
										view, null, true, null );
								
								new_si.setMdiEntry(entry);
								if (entry instanceof SideBarEntrySWT) {
									new_si.setTreeItem( ((SideBarEntrySWT)entry).getTreeItem() );
								}
								
								new_si.activate();
							}
						}
					});
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								ViewTitleInfoManager.refreshTitleInfo( existing_si.getView());
								
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								MdiEntry mainEntry = mdi.getEntry(SIDEBAR_SECTION_RELATED_CONTENT );
								
								if ( mainEntry != null ){
									
									ViewTitleInfoManager.refreshTitleInfo( mainEntry.getViewTitleInfo());
								}
								
								existing_si.activate();
							}
						});
			}
		}
	}
	
	protected class
	MainViewInfo
		implements 	ViewTitleInfo
	{
		protected
		MainViewInfo()
		{
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{		
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT ){
				
				int	 unread = manager==null?0:manager.getNumUnread();
				
				if ( unread > 0 ){
				
					return( String.valueOf( unread ));
				}
				
			}else if ( propertyID == TITLE_INDICATOR_COLOR ){
	
			}
			
			return null;
		}
		
		public String
		getTitle()
		{
			return( MessageText.getString("rcm.view.title"));
		}
	}
	
	protected class
	RCMView
		implements 	ViewTitleInfo
	{
		private String			parent_key;
		private String			name;
		
		private int				num_unread;
		
		protected
		RCMView(
			String			_parent_key,
			String			_name )
		{
			parent_key	= _parent_key;
			name		= _name;
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{		
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT ){
				
				if ( num_unread > 0 ){
				
					return( String.valueOf( num_unread ));
				}
				
			}else if ( propertyID == TITLE_INDICATOR_COLOR ){
	
			}
			
			return null;
		}
		
		public String
		getTitle()
		{
			return( name );
		}
		
		protected void
		setNumUnread(
			int	n )
		{
			num_unread = n;
						
			ViewTitleInfoManager.refreshTitleInfo( this );
		}
	}
		
	private void
	lookupSubscriptions()
	{
		final byte[] subs_hash = { 0 };
		
		synchronized( this ){
			
			final RCMItem existing_si = rcm_item_map.get( subs_hash );
			
			if (  existing_si == null ){
	
				final RCMItem new_si = new RCMItemSubscriptions( subs_hash );
				
				rcm_item_map.put( subs_hash, new_si );
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( RelatedContentUI.this ){

								if ( new_si.isDestroyed()){
									
									return;
								}
								
								RCMView view = new RCMView( SIDEBAR_SECTION_RELATED_CONTENT, "Swarm Subscriptions" );
								
								new_si.setView( view );
								
								String key = "RCM_" + ByteFormatter.encodeString( subs_hash );
								
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								
								MdiEntry	entry = mdi.createEntryFromSkinRef(
										SIDEBAR_SECTION_RELATED_CONTENT,
										key, "rcmview",
										view.getTitle(),
										view, null, true, null );
								
								new_si.setMdiEntry(entry);
								
								if (entry instanceof SideBarEntrySWT){
									
									new_si.setTreeItem( ((SideBarEntrySWT)entry).getTreeItem() );
								}
								
								new_si.activate();
							}
						}
					});
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								ViewTitleInfoManager.refreshTitleInfo( existing_si.getView());
								
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								MdiEntry mainEntry = mdi.getEntry(SIDEBAR_SECTION_RELATED_CONTENT );
								
								if ( mainEntry != null ){
									
									ViewTitleInfoManager.refreshTitleInfo( mainEntry.getViewTitleInfo());
								}
								
								existing_si.activate();
							}
						});
			}
		}
	}
	
	private static final String SPINNER_IMAGE_ID 	= "image.sidebar.vitality.dl";

	protected static void
	hideIcon(
		MdiEntryVitalityImage	x )
	{
		if ( x == null ){
			return;
		}
		
		x.setVisible( false );
		x.setToolTip( "" );
	}
	
	protected static void
	showIcon(
		MdiEntryVitalityImage	x ,
		String					t )
	{
		if ( x == null ){
			return;
		}
		
		x.setToolTip( t );
		x.setVisible( true );
	}
	
	public interface
	RCMItem
		extends RelatedContentEnumerator, MdiCloseListener
	{	
		public void
		contentRemoved(
			RelatedContent[]	rc );
		
		public void
		updateNumUnread();
		
		public void
		setTreeItem(
			TreeItem		ti );
		
		public TreeItem
		getTreeItem();
		
		public void
		setView(
			RCMView		v );
		
		public RCMView
		getView();
		
		public void
		setMdiEntry(
			MdiEntry _sb_entry );
		
		public void
		activate();
		
		public boolean
		isDestroyed();
	}
	
	public class
	RCMItemContent
		implements RCMItem
	{	
		private byte[]				hash;
		
		private RCMView				view;
		private MdiEntry			sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private MdiEntryVitalityImage	spinner;
		
		private List<RelatedContent>	content_list = new ArrayList<RelatedContent>();
		
		private int	num_unread;
		
		private CopyOnWriteList<RelatedContentEnumeratorListener>	listeners = new CopyOnWriteList<RelatedContentEnumeratorListener>();
		
		private boolean	lookup_complete;
		
		protected
		RCMItemContent(
			byte[]		_hash )
		{
			hash		= _hash;
		}
		
		public void
		setMdiEntry(
			MdiEntry _sb_entry )
		{
			sb_entry	= _sb_entry;
			
			sb_entry.setDatasource( this );
			
			sb_entry.addListener( this );
			
			spinner = sb_entry.addVitalityImage( SPINNER_IMAGE_ID );

			try{
				showIcon( spinner, null );
				
				manager.lookupContent(
					hash,
					new RelatedContentLookupListener()
					{
						public void
						lookupStart()
						{
						}
						
						public void
						contentFound(
							RelatedContent[]	content )
						{
							synchronized( RCMItemContent.this ){
							
								if ( !destroyed ){
								
									for ( RelatedContent c: content ){
									
										if ( !content_list.contains( c )){
										
											content_list.add( c );
										}
									}
								}
							}
							
							updateNumUnread();
							
							for ( RelatedContentEnumeratorListener listener: listeners ){
								
								try{
									listener.contentFound( content );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
						
						public void
						lookupComplete()
						{	
							synchronized( RCMItemContent.this ){
								
								lookup_complete = true;
							}
							
							hideIcon( spinner );
						}
						
						public void
						lookupFailed(
							ContentException e )
						{	
							lookupComplete();
						}
					});
			}catch( Throwable e ){
				
				lookup_complete = true;
				
				Debug.out( e );
				
				hideIcon( spinner );
			}
		}
		
		public void
		setTreeItem(
			TreeItem		_tree_item )
		{
			tree_item	= _tree_item;

		}
		
		public void 
		contentRemoved(
			RelatedContent[] content ) 
		{
			boolean deleted = false;
			
			synchronized( RCMItemContent.this ){
									
				for ( RelatedContent c: content ){
						
					if ( content_list.remove( c )){
														
						deleted = true;
					}
				}
			}
			
			if ( deleted ){
			
				updateNumUnread();
			}
		}
		
		public void
		updateNumUnread()
		{
			synchronized( RCMItemContent.this ){
				
				int	num = 0;
				
				for ( RelatedContent c: content_list ){
					
					if ( c.isUnread()){
						
						num++;
					}
				}
				
				if ( num != num_unread ){
					
					num_unread = num;
					
					final int f_num = num;
										
					async_dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								if ( async_dispatcher.getQueueSize() > 0 ){
									
									return;
								}
								
								view.setNumUnread( f_num );
							}
						});
				}
			}
		}
		
		public void
		enumerate(
			final RelatedContentEnumeratorListener	listener )
		{
			RelatedContent[]	already_found;
			 
			synchronized( this ){
				
				if ( !lookup_complete ){
					
					listeners.add( listener );
				}
				
				already_found = content_list.toArray( new RelatedContent[ content_list.size()]);
			}
			
			if ( already_found.length > 0 ){
				
				listener.contentFound( already_found );
			}
		}
		
		public TreeItem
		getTreeItem()
		{
			return( tree_item );
		}
		
		protected MdiEntry
		getSideBarEntry()
		{
			return( sb_entry );
		}
		
		public void
		setView(
			RCMView		_view )
		{
			view	= _view;
		}
		
		public RCMView
		getView()
		{
			return( view );
		}
		
		public boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		public void 
		mdiEntryClosed(
			MdiEntry entry,
			boolean userClosed )
		{
			destroy();
		}
		
		protected void
		destroy()
		{
			synchronized( this ){
			
				content_list.clear();
				
				destroyed = true;
			}
			
			synchronized( RelatedContentUI.this ){
					
				rcm_item_map.remove( hash );
			}
		}
		
		public void 
		activate() 
		{
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			
			if ( mdi != null && sb_entry != null ){
				
				mdi.showEntryByID(sb_entry.getId());
			}
		}
	}
	
	
	public class
	RCMItemSubscriptions
		implements RCMItem
	{	
		private byte[]				hash;
		
		private RCMView				view;
		private MdiEntry			sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private MdiEntryVitalityImage	spinner;
		
		private List<RelatedContent>	content_list = new ArrayList<RelatedContent>();
		
		private int	num_unread;
		
		private CopyOnWriteList<RelatedContentEnumeratorListener>	listeners = new CopyOnWriteList<RelatedContentEnumeratorListener>();
		
		private boolean	lookup_complete;
		
		protected
		RCMItemSubscriptions(
			byte[]		_hash )
		{
			hash		= _hash;
		}
		
		public void
		setMdiEntry(
			MdiEntry _sb_entry )
		{
			sb_entry	= _sb_entry;
			
			sb_entry.setDatasource( this );
			
			sb_entry.addListener( this );
			
			spinner = sb_entry.addVitalityImage( SPINNER_IMAGE_ID );

			try{
				showIcon( spinner, null );
						
				new AEThread2( "async" )
				{
					private Map<String,SubsRelatedContent>	s_map = new HashMap<String,SubsRelatedContent>();
					
					public void 
					run()
					{
						try{
							SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
							
							RelatedContent[] content = manager.getRelatedContent();
							
							final AESemaphore sem = new AESemaphore( "rcm", 16 );
							
							for ( RelatedContent c: content ){
								
								byte[] hash = c.getHash();
								
								if ( hash == null ){
									
									continue;
								}
								
								try{
									sem.reserve();
									
									subs_man.lookupAssociations(
										hash,
										new SubscriptionLookupListener()
										{
											public void
											found(
												byte[]					hash,
												Subscription			subscription )
											{
												RelatedContent[] content;
												
												synchronized( RCMItemSubscriptions.this ){
														
													String id = subscription.getID();
													
													SubsRelatedContent existing = s_map.get( id );
													
													if ( existing == null ){
														
														existing = new SubsRelatedContent( subscription );
														
														s_map.put( id, existing );
														
														content = new RelatedContent[]{ existing };
														
													}else{
														
														existing.setRank( existing.getRank() + 1 );
														
														return;
													}
													
													if ( !destroyed ){
													
														for ( RelatedContent c: content ){
														
															if ( !content_list.contains( c )){
															
																content_list.add( c );
															}
														}
													}
												}
												
												updateNumUnread();
												
												for ( RelatedContentEnumeratorListener listener: listeners ){
													
													try{
														listener.contentFound( content );
														
													}catch( Throwable e ){
														
														Debug.out( e );
													}
												}
											}
											
											public void
											complete(
												byte[]					hash,
												Subscription[]			subscriptions )
											{
												sem.release();
											}
											
											public void
											failed(
												byte[]					hash,
												SubscriptionException	error )
											{
												sem.release();
											}
										});
									
								}catch( Throwable e ){
									
									sem.release();
								}
							}
						}finally{
							
							synchronized( RCMItemSubscriptions.this ){
								
								lookup_complete = true;
							}
							
							hideIcon( spinner );
						}
					}
				}.start();

			}catch( Throwable e ){
				
				lookup_complete = true;
				
				Debug.out( e );
				
				hideIcon( spinner );
			}
		}
		
		public void
		setTreeItem(
			TreeItem		_tree_item )
		{
			tree_item	= _tree_item;

		}
		
		public void 
		contentRemoved(
			RelatedContent[] content ) 
		{
			boolean deleted = false;
			
			synchronized( RCMItemSubscriptions.this ){
									
				for ( RelatedContent c: content ){
						
					if ( content_list.remove( c )){
														
						deleted = true;
					}
				}
			}
			
			if ( deleted ){
			
				updateNumUnread();
			}
		}
		
		public void
		updateNumUnread()
		{
			synchronized( RCMItemSubscriptions.this ){
				
				int	num = 0;
				
				for ( RelatedContent c: content_list ){
					
					if ( c.isUnread()){
						
						num++;
					}
				}
				
				if ( num != num_unread ){
					
					num_unread = num;
					
					final int f_num = num;
										
					async_dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								if ( async_dispatcher.getQueueSize() > 0 ){
									
									return;
								}
								
								view.setNumUnread( f_num );
							}
						});
				}
			}
		}
		
		public void
		enumerate(
			final RelatedContentEnumeratorListener	listener )
		{
			RelatedContent[]	already_found;
			 
			synchronized( this ){
				
				if ( !lookup_complete ){
					
					listeners.add( listener );
				}
				
				already_found = content_list.toArray( new RelatedContent[ content_list.size()]);
			}
			
			if ( already_found.length > 0 ){
				
				listener.contentFound( already_found );
			}
		}
		
		public TreeItem
		getTreeItem()
		{
			return( tree_item );
		}
		
		protected MdiEntry
		getSideBarEntry()
		{
			return( sb_entry );
		}
		
		public void
		setView(
			RCMView		_view )
		{
			view	= _view;
		}
		
		public RCMView
		getView()
		{
			return( view );
		}
		
		public boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		public void 
		mdiEntryClosed(
			MdiEntry entry,
			boolean userClosed )
		{
			destroy();
		}
		
		protected void
		destroy()
		{
			synchronized( this ){
			
				content_list.clear();
				
				destroyed = true;
			}
			
			synchronized( RelatedContentUI.this ){
					
				rcm_item_map.remove( hash );
			}
		}
		
		public void 
		activate() 
		{
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			
			if ( mdi != null && sb_entry != null ){
				
				mdi.showEntryByID(sb_entry.getId());
			}
		}
	}
	
	public class
	SubsRelatedContent
		extends RelatedContent
	{
		private Subscription	subscription;
		
		private int rank;
		
		private
		SubsRelatedContent(
			Subscription	subs )
		{
			super( subs.getName(), new byte[0], subs.getNameEx(), -1, -1, (int)(subs.getCachedPopularity()<<16), (byte)ContentNetwork.CONTENT_NETWORK_UNKNOWN );
			
			subscription = subs;
		}
		
		public void
		setRank(
			int		r )
		{
			rank = r;
		}
		
		public int
		getRank()
		{
			return( rank );
		}

		public int 
		getLevel() 
		{
			return( 0 );
		}
		
		public boolean 
		isUnread() 
		{
			return( !subscription.isSubscribed() );
		}
		
		public void 
		setUnread(
			boolean unread )
		{
			subscription.setSubscribed( !unread );
		}
		
		public Download
		getRelatedToDownload()
		{
			return( null );
		}

		public int 
		getLastSeenSecs() 
		{
			return 0;
		}
		
		public void 
		delete() 
		{
		}
	}

	public void setSearchEnabled(boolean b) {
		enable_search.setValue(b);
	}

	public void setUIEnabled(boolean b) {
		enable_ui.setValue(b);
	}


	private static void addResourceBundle(SWTSkin skin, String path, String name) {
		String sFile = path + name;
		ClassLoader loader = RCMPlugin.class.getClassLoader();
		SWTSkinProperties skinProperties = skin.getSkinProperties();
		try {
			ResourceBundle subBundle = ResourceBundle.getBundle(sFile,
					Locale.getDefault(), loader);
			skinProperties.addResourceBundle(subBundle, path, loader);
		} catch (MissingResourceException mre) {
			Debug.out(mre);
		}
	}

	public static void showFTUX(final SWTSkinObject so_list) {
		VuzeMessageBox box = new VuzeMessageBox(MessageText.getString("rcm.ftux.title"), null, new String[] {
			MessageText.getString("rcm.ftux.enable"),
			MessageText.getString("rcm.ftux.disable"),
		}, 0);
		box.setSubTitle(MessageText.getString("rcm.ftux.heading"));
		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				addResourceBundle(skin, "com/aelitis/plugins/rcmplugin/skins/",
						"skin3_rcm_ftux");

				String id = "rcm.ftux.shell";
				skin.createSkinObject(id, id, soExtra);
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result < 0) {
					UIFunctionsManager.getUIFunctions().openView(UIFunctions.VIEW_MYTORRENTS, null);
					return;
				}
				boolean enabled = result == 0;				
				
				if ( so_list != null ){
					so_list.setVisible(enabled);
				}


				RelatedContentUI ui = RelatedContentUI.getSingleton( null, null );

				if (ui != null) {
					if (enabled) {
						ui.plugin.setRCMEnabled(enabled);
					}
					ui.setSearchEnabled(enabled);
					ui.setUIEnabled(enabled);
					ui.plugin.setFTUXBeenShown(true);
				}

			}
		});
		
		box.waitUntilClosed();
	}
}
