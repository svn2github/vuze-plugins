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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.content.ContentException;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentLookupListener;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.core.content.RelatedContentManagerListener;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionLookupListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

public class 
RelatedContentUI 
{		
	public static final String SIDEBAR_SECTION_RELATED_CONTENT = "RelatedContent";

	private static RelatedContentUI	singleton;
	
	public static synchronized RelatedContentUI
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new RelatedContentUI();
		}
		
		return( singleton );
	}
	
	private PluginInterface		plugin_interface;
	private UIManager			ui_manager;
	
	private RelatedContentManager	manager;
	
	private boolean			ui_setup;
	private boolean			root_menus_added;
	private MainViewInfo 	main_view_info;
	
	
	private ByteArrayHashMap<RCMItem>	rcm_item_map = new ByteArrayHashMap<RCMItem>();
	
	private AsyncDispatcher	async_dispatcher = new AsyncDispatcher();
	
	public 
	RelatedContentUI()
	{
		plugin_interface = PluginInitializer.getDefaultInterface();
		
		ui_manager = plugin_interface.getUIManager();

		ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
							UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){

							AzureusCoreFactory.addCoreRunningListener(
								new AzureusCoreRunningListener() 
								{
									public void 
									azureusCoreRunning(
										AzureusCore core ) 
									{
										uiAttachedAndCoreRunning(core);
									}
								});
						}
					}

					public void
					UIDetached(
							UIInstance		instance )
					{
					}
				});
	}
	
	private void 
	uiAttachedAndCoreRunning(
		AzureusCore core ) 
	{
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
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
									if (skinview instanceof SideBar) {
									
										setupUI((SideBar) skinview);
										
										SkinViewManager.RemoveListener(this);
									}
								}
							});
					}
				}
			});
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

			if ( RelatedContentManager.DISABLE_ALL_UI ){
				
				return;
			}
			
			BasicPluginConfigModel config_model = 
				ui_manager.createBasicPluginConfigModel( "Associations" );
			
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
						
			main_view_info = new MainViewInfo();

			hookMenus();
						
			buildSideBar();
			
			manager.addListener(
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
						int	unread = manager.getNumUnread();
						
						synchronized( this ){
							
							if ( unread == last_unread ){
								
								return;
							}
							
							last_unread = unread;
						}
						
						ViewTitleInfoManager.refreshTitleInfo( main_view_info );
					}
				});
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected void
	hookMenus()
	{
		TableManager	table_manager = plugin_interface.getUIManager().getTableManager();

		String[]	table_ids = {
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				TableManager.TABLE_MYTORRENTS_COMPLETE,
				TableManager.TABLE_MYTORRENTS_ALL_BIG,
				TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
		};
		
		for ( String table_id: table_ids ){
			
			TableContextMenuItem menu_item = table_manager.addContextMenuItem( table_id, "rcm.contextmenu.lookupassoc");
		
			menu_item.setStyle( TableContextMenuItem.STYLE_PUSH );

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
				
				menu_item.addMultiListener( listener );
		}
	}
	
	protected void
	explicitSearch(
		Download		download )
	{
		addSearch( download );
	}
	
	protected void
	buildSideBar()
	{		
		final String parent_id = "sidebar." + SIDEBAR_SECTION_RELATED_CONTENT;

		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		mdi.registerEntry(SIDEBAR_SECTION_RELATED_CONTENT, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				
				MdiEntry mdiEntry = mdi.createEntryFromSkinRef(
						null,
						SIDEBAR_SECTION_RELATED_CONTENT, "rcmview",
						main_view_info.getTitle(),
						main_view_info, null, false, -1  );

				mdiEntry.setImageLeftID( "image.sidebar.rcm" );
				
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
											current_listener.contentFound( content );
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
									
								manager.addListener( base_listener );
							}
							
							RelatedContent[] current_content = manager.getRelatedContent();
							
							listener.contentFound( current_content );
						}
					});

				return mdiEntry;
			}
		});
		
		mdi.loadEntryByID( SIDEBAR_SECTION_RELATED_CONTENT, false );
		
		if ( !root_menus_added ){
			
			root_menus_added = true;
			
			MenuManager menu_manager = ui_manager.getMenuManager();

			MenuItem menu_item = menu_manager.addMenuItem( parent_id, "rcm.menu.findsubs" );
			
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
										view, null, true, -1 );
								
								new_si.setMdiEntry(entry);
								if (entry instanceof SideBarEntrySWT) {
									new_si.setTreeItem( ((SideBarEntrySWT)entry).getTreeItem() );
								}
								
								/*
								TreeItem  tree_item = 
									side_bar.createTreeItemFromIView(
										SideBar.SIDEBAR_SECTION_RELATED_CONTENT, 
										view,
										key, 
										null, 
										true, 
										true,
										false );
										
									SideBarEntrySWT	entry = SideBar.getEntry( key );
																
									new_si.setTreeItem( tree_item, entry );
								*/
																
									
								/*
								PluginInterface pi = PluginInitializer.getDefaultInterface();
								UIManager uim = pi.getUIManager();
								MenuManager menuManager = uim.getMenuManager();
								
								MenuItem menuItem;
								*/
								
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
				
				int	 unread = manager.getNumUnread();
				
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
										view, null, true, -1 );
								
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
}
