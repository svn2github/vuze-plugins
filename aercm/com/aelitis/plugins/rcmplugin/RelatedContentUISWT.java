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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.search.SearchInstance;
import org.gudy.azureus2.plugins.utils.search.SearchObserver;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.content.*;
import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

public class 
RelatedContentUISWT 
	implements RelatedContentUI
{		
	public static final String SIDEBAR_SECTION_RELATED_CONTENT = "RelatedContent";
	
	private static final String SPINNER_IMAGE_ID 	= "image.sidebar.vitality.dl";

	private static RelatedContentUISWT		singleton;
	
	protected synchronized static RelatedContentUISWT
	getSingleton()
	{
		return( getSingleton( null, null, null ));
	}
	
	public synchronized static RelatedContentUISWT
	getSingleton(
		PluginInterface		pi,
		UIInstance			ui,
		RCMPlugin			plugin )
	{
		if ( singleton == null || singleton.isDestroyed()){
			
			if ( pi == null || ui == null || plugin == null ){
				
				return( null );
			}
			
			singleton = new RelatedContentUISWT( pi, (UISWTInstance)ui, plugin );
		}
		
		return( singleton );
	}
	
	private PluginInterface		plugin_interface;
	private UISWTInstance		swt_ui;
	private RCMPlugin			plugin;
	
	private BasicPluginConfigModel 	config_model;
	private BooleanParameter 		enable_ui;
	private BooleanParameter 		enable_search;
	
	private RelatedContentManager			manager;
	private RelatedContentManagerListener	rcm_listener;
	
	private boolean			ui_setup;
	
	private boolean			root_menus_added;
	private MenuItem		root_menu;
	
	private Image			swarm_image;
	private Image[]			vitality_images;
	
	private List<MenuItem>	torrent_menus = new ArrayList<MenuItem>();
		
	private ByteArrayHashMap<RCMItem>	rcm_item_map = new ByteArrayHashMap<RCMItem>();
	
	private AsyncDispatcher	async_dispatcher = new AsyncDispatcher();
	
	private HashMap<UISWTView,SubViewHolder> rcm_subviews = new HashMap<UISWTView,SubViewHolder>();

	
	private volatile boolean	destroyed = false;

	private UISWTGraphic menu_icon;

	private 
	RelatedContentUISWT(
		PluginInterface	_plugin_interface, 
		UISWTInstance	_ui,
		RCMPlugin		_plugin )
	{
		plugin_interface	= _plugin_interface;
		swt_ui				= _ui;
		plugin				= _plugin;
		
		String path = "com/aelitis/plugins/rcmplugin/skins/";

		String sFile = path + "skin3_rcm";

		ClassLoader loader = RCMPlugin.class.getClassLoader();

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();

		try {
			ResourceBundle subBundle = ResourceBundle.getBundle(sFile, Locale.getDefault(), loader);
			
			skinProperties.addResourceBundle(subBundle, path, loader);
			
		} catch (MissingResourceException mre) {
			
			Debug.out(mre);
		}	
		
		CoreWaiterSWT.waitForCore(TriggerInThread.SWT_THREAD,
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
	
	protected PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected Image
	getSwarmImage()
	{
		return( swarm_image );
	}
	
	public void
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
						for ( MenuItem menu: torrent_menus ){
							
							menu.remove();
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
					
					if ( root_menu != null ){
						
						root_menu.remove();
						
						root_menu = null;
					}
					
					hookSubViews( false );
					
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
		
		swarm_image = swt_ui.loadImage( "org/gudy/azureus2/ui/icons/rcm.png" );
	
		vitality_images = ImageLoader.getInstance().getImages( SPINNER_IMAGE_ID );

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
						showFTUX(null,null);
					}		
				});


			ActionParameter sourcesAction = config_model.addActionParameter2(null, "rcm.button.sources");
			sourcesAction.setMinimumRequiredUserMode(ActionParameter.MODE_INTERMEDIATE);
			sourcesAction.addListener(new ParameterListener() {
				public void parameterChanged(Parameter param) {
					showSourcesList();
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

			enable_search = 
				config_model.addBooleanParameter2( 
					"rcm.search.enable", "rcm.search.enable",
					false );

			enable_search.addListener(new ParameterListener() {
				public void parameterChanged(Parameter param) {
					plugin.hookSearch();
				}
			});

			IntParameter sr_min_rank = 
				config_model.addIntParameter2( 
					"rcm.search.min_rank", "rcm.search.min_rank", RCMPlugin.MIN_SEARCH_RANK_DEFAULT );

			enable_search.addEnabledOnSelection( sr_min_rank );
			
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
				config_model.addBooleanParameter2(RCMPlugin.PARAM_FTUX_SHOWN, "!Debug:Was Welcome Shown?!", false);
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

	protected void showSourcesList() {

		List<String> list = RelatedContentUISWT.getSingleton().getPlugin().getSourcesList();
		
		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
				"rcm.sources.title", "rcm.sources.text" );
		
		String 	text = "";
		
		for ( String s: list ){
			
			text += s + "\r\n";
		}
									
		entryWindow.setPreenteredText( text, false );
		
		entryWindow.selectPreenteredText( false );
		
		entryWindow.setMultiLine( true );
		
		entryWindow.setLineHeight( list.size() + 3 );
		
		entryWindow.prompt(new UIInputReceiverListener() {
			public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
				if (!entryWindow.hasSubmittedInput()) {
					return;
				}
				
				String input = entryWindow.getSubmittedInput();
				
				if ( input == null ){
					
					input = "";
					
				}else{
					
					input = input.trim();
				}
				
				String[] lines = input.split( "\n" );
				
				List<String> list = new ArrayList<String>();
				
				for ( String line: lines ){
					
					line = line.trim();
					
					if ( line.length() > 0  ){
						
						list.add( line );
					}
				}
				
				COConfigurationManager.setParameter( RCMPlugin.PARAM_SOURCES_LIST, list );
			}
		});

	}

	private void
	hookUI()
	{
		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (plugin.isRCMEnabled() && enable_ui.getValue()) {

			if ( swarm_image != null && !swarm_image.isDisposed() && menu_icon == null){
				menu_icon = swt_ui.createGraphic( swarm_image );
			}


			mdi.loadEntryByID(SIDEBAR_SECTION_RELATED_CONTENT, false, true, null);
			hookMenus(true);
			hookSubViews(true);
		} else {
			mdi.closeEntry(SIDEBAR_SECTION_RELATED_CONTENT);
			hookMenus(false);
			hookSubViews(false);
		}

		plugin.hookSearch();
	}
	
	private void
	hookSubViews(
		boolean	enable )
	{
		String[] views = {
			TableManager.TABLE_MYTORRENTS_ALL_BIG,
			TableManager.TABLE_MYTORRENTS_INCOMPLETE,
			TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
			TableManager.TABLE_MYTORRENTS_COMPLETE,
		};
		
		if ( enable ){
				
			UISWTViewEventListener listener = 
				new UISWTViewEventListener()
				{	
					public boolean 
					eventOccurred(
						UISWTViewEvent event ) 
					{
						UISWTView 	currentView = event.getView();
						
						switch (event.getType()) {
							case UISWTViewEvent.TYPE_CREATE:{
								
								SWTSkin skin = SWTSkinFactory.getNonPersistentInstance(
										getClass().getClassLoader(),
										"com/aelitis/plugins/rcmplugin/skins",
										"skin3_rcm.properties" );
								
								rcm_subviews.put(currentView, new SubViewHolder( skin ));
								
								break;
							}
							case UISWTViewEvent.TYPE_INITIALIZE:{
							
								SubViewHolder subview = rcm_subviews.get(currentView);
								
								if ( subview != null ){
									
									subview.initialise((Composite)event.getData());
								}
		
								break;
							}
							case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:{
								
								SubViewHolder subview = rcm_subviews.get(currentView);
								
								if ( subview != null ){
									
									subview.setDataSource( event.getData());
								}
								
								break;
							}
							
							case UISWTViewEvent.TYPE_DESTROY:{
								
								SubViewHolder subview = rcm_subviews.remove(currentView);
							
								if ( subview != null ){
									
									subview.destroy();
								}
								
								break;
							}
						}
						return true;
					}
				};
				
			for ( String table_id: views ){
				
				swt_ui.addView(table_id, "rcm.subview.torrentdetails.name",	listener );
			}
		}else{
			
			for ( String table_id: views ){
				
				swt_ui.removeViews( table_id, "rcm.subview.torrentdetails.name" );
			}
			
			for ( UISWTView entry: new ArrayList<UISWTView>(rcm_subviews.keySet())){
				
				entry.closeView();
			}
			
			rcm_subviews.clear();
		}
	}
	
	private class
	SubViewHolder
	{
		private SWTSkin				skin;
		private RCMItemSubView		current_data_source;
		
		private Download			dl 		= null;
		private DiskManagerFileInfo	dl_file = null;

		private boolean				related_mode = true;

		private ImageLabel			status_img_label;
		private Button[]			buttons;
		
		private boolean 			current_rm;
		private Download			current_dl;
		private DiskManagerFileInfo	current_file;
		
		
		private
		SubViewHolder(
			SWTSkin	_skin  )
		{
			skin	= _skin;
		}
		
		private void
		initialise(
			Composite		parent )
		{		
			Composite header = new Composite( parent, SWT.NULL );
			
			RowLayout header_layout = new RowLayout();
			
			header_layout.center = true;
			header_layout.spacing = 10;
			
			header.setLayout( header_layout );
						
			header.setLayoutData( new GridData( GridData.FILL_HORIZONTAL));
			
			status_img_label = new ImageLabel( header, swarm_image );
									
			final Button button_related = new Button( header, SWT.RADIO );
			
			button_related.setText( MessageText.getString( "rcm.subview.relto" ));
			
			button_related.setSelection( true );
						
			final Button button_size = new Button( header, SWT.RADIO );
			
			button_size.setText( MessageText.getString( "rcm.subview.filesize" ));
			
			Listener but_list  = 
				new Listener()
				{
					public void 
					handleEvent(
						Event arg0) 
					{
						related_mode = button_related.getSelection();
						
						doSearch();
					}
				};
				
			button_related.addListener( SWT.Selection, but_list );
			button_size.addListener( SWT.Selection, but_list );
			
			buttons = new Button[]{ button_related, button_size };
			
			buttons[0].setEnabled( dl != null );
			buttons[1].setEnabled( dl_file != null );
			
			Composite skin_area = new Composite( parent, SWT.NULL );
			
			skin_area.setLayout( new FormLayout());
			
			skin_area.setLayoutData( new GridData( GridData.FILL_BOTH ));
			
			skin.initialize( skin_area, "subskin" );
			
			SWTSkinObject so = skin.getSkinObjectByID( "rcmsubskinview" );
			
			RCMItemSubView	ds = current_data_source;
			
			if ( ds == null ){
				
				ds = new RCMItemSubViewEmpty();
			}
			
			so.triggerListeners( SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, ds );
					
			so.setVisible( true );

			skin.layout();
		}
		
		private void
		setDataSource(
			Object		obj )
		{									
			dl 		= null;
			dl_file = null;
			
			if ( obj instanceof Object[]){
				
				Object[] ds = (Object[])obj;
				
				if ( ds.length > 0 ){
					
					if ( ds[0] instanceof Download ){
		
						dl = (Download)ds[0];
						
					}else if ( ds[0] instanceof DiskManagerFileInfo ){
						
						dl_file = (DiskManagerFileInfo)ds[0];
					}
				}
			}else{
				
				if ( obj instanceof Download ){
					
					dl = (Download)obj;
					
				}else if ( obj instanceof DiskManagerFileInfo ){
					
					dl_file = (DiskManagerFileInfo)obj;
				}
			}
			
			if ( dl_file != null ){
				
				try{
					dl = dl_file.getDownload();
					
				}catch( Throwable e ){	
				}
				
				if ( dl_file.getLength() < RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
					
					dl_file = null;
				}
			}
			
			if ( dl_file == null ){
				
				related_mode = true;
				
				if ( dl != null ){
					
					if ( dl.getDiskManagerFileCount() == 1 ){
						
						dl_file = dl.getDiskManagerFileInfo( 0 );
						
						if ( dl_file.getLength() < RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
							
							dl_file = null;
						}
					}
				}
			}else{
				
				related_mode = false;
			}
			
			Utils.execSWTThread(
				new Runnable()
				{
					public void 
					run() 
					{
						if ( buttons != null ){
							
							if ( related_mode ){
								
								buttons[0].setSelection( true );
								buttons[1].setSelection( false );
								
							}else{
								
								buttons[1].setSelection( true );
								buttons[0].setSelection( false );
							}
							
							buttons[0].setEnabled( dl != null );
							buttons[1].setEnabled( dl_file != null );
						}
					}
				});
			
			doSearch();
		}
		
		private void
		doSearch()
		{	
			if ( current_rm == related_mode && current_dl == dl ){
				
				if ( current_file == null && dl_file == null ){
					
					return;
					
				}else if ( current_file != null && dl_file != null ){
				
						// can't test on object equality for the files as the plugin interface
						// generates wrappers on demand...
					
					if ( current_file.getIndex() == dl_file.getIndex()){
						
						return;
					}
				}
			}
			
			current_rm 		= related_mode;
			current_dl		= dl;
			current_file	= dl_file;
			
			if ( current_data_source != null ){
				
				current_data_source.destroy( false );
			}
			
			final SWTSkinObject so = skin.getSkinObjectByID( "rcmsubskinview" );

			final RCMItemSubView	new_subview;
			
			if ( related_mode && dl != null ){
				
				String[] networks = PluginCoreUtils.unwrap( dl ).getDownloadState().getNetworks();
				
				new_subview = new RCMItemSubView(dl.getTorrent().getHash(), networks );
								
			}else if ( !related_mode && dl_file != null && dl != null ){
						
				String[] networks = PluginCoreUtils.unwrap( dl ).getDownloadState().getNetworks();

				long file_size = dl_file.getLength();
											
				new_subview = new RCMItemSubView(dl.getTorrent().getHash(), networks, file_size );
												
			}else{
				
				new_subview = new RCMItemSubViewEmpty();
			}
			
			if ( !( new_subview instanceof RCMItemSubViewEmpty )){
				
				new_subview.setListener(
					new RCMItemSubViewListener()
					{
						private int vi_index;
						
						public boolean
						searching()
						{
							if ( 	current_data_source != new_subview ||
									( status_img_label != null && status_img_label.isDisposed())){
								
								return( false );
							}
							
							Utils.execSWTThread(
									new Runnable()
									{
										public void 
										run() 
										{
											if ( status_img_label != null && !status_img_label.isDisposed() && vitality_images.length > 0){
												
												status_img_label.setImage( vitality_images[vi_index++%vitality_images.length]);
											}
										}
									});
							
							return( true );
						}
						
						public void
						complete()
						{
							if ( 	current_data_source != new_subview ||
									( status_img_label != null && status_img_label.isDisposed())){
																
								return;
							}
							
							Utils.execSWTThread(
									new Runnable()
									{
										public void 
										run() 
										{
											if ( status_img_label != null && !status_img_label.isDisposed()){
												
												status_img_label.setImage( swarm_image );	
											}
										}
									});
						}
					});
				
				new_subview.setMdiEntry( null );	// trigger search start
			}
			
			if ( so != null ){
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void 
							run() 
							{
									// not great but need this to pick up teh new datasource
									// properly...
								
								so.setVisible( false );
								
								so.triggerListeners( SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, new_subview );
								
								so.setVisible( true );
							}
						});
			}
			
			current_data_source = new_subview;
		}
		
		private void
		destroy()
		{			
			SWTSkinObject so = skin.getSkinObjectByID( "rcmsubskinview" );
			
			if ( so != null ){
			
				skin.removeSkinObject( so );
			}
			
			if ( current_data_source != null ){
				
				current_data_source.destroy( false );
			}
		}
	}
	
	protected void
	hookMenus(
		boolean enable )
	{
		if ( enable && torrent_menus.size() > 0 ) {
			return;
		}
		
		if ( !enable ){
			
			if ( torrent_menus.size() > 0 ){
				
				for (MenuItem menuitem : torrent_menus) {
					menuitem.remove();
				}
				
				torrent_menus.clear();
			}
			return;
		}

		MenuManager mm = plugin_interface.getUIManager().getMenuManager();
		
		MenuItem mi_searchtag = mm.addMenuItem(MenuManager.MENU_TAG_CONTEXT, "rcm.contextmenu.searchtag");
		torrent_menus.add( mi_searchtag );

		mi_searchtag.setGraphic(menu_icon);
		mi_searchtag.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object target) {
				boolean enable = false;
				if (target instanceof Tag[]) {
					Tag[] tags = (Tag[]) target;

					for (Tag tag : tags) {
						if (tag.getTagType().getTagType() == TagType.TT_DOWNLOAD_MANUAL) {
							enable = true;
							break;
						}
					}
				}
				menu.setVisible(enable);
			}
		});
		mi_searchtag.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (!(target instanceof Tag[])) {
					return;
				}
				Tag[] tags = (Tag[]) target;

				String[] networks = AENetworkClassifier.getDefaultNetworks();

				for (Tag tag : tags) {
					addSearch("tag:" + tag.getTagName(true), networks);
				}
			}
		});
		

				
			MenuItem mi_rel = mm.addMenuItem(MenuManager.MENU_DOWNLOAD_CONTEXT, "rcm.contextmenu.lookupassoc");
		
			torrent_menus.add( mi_rel );
			
			mi_rel.setStyle( TableContextMenuItem.STYLE_PUSH );

			mi_rel.setGraphic( menu_icon );
			
			MenuItemListener listener = 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						if (!(target instanceof Download[])) {
							return;
						}
						Download[]	rows = (Download[])target;
						
						for ( Download download: rows ){
							
							explicitSearch( download );
						}
					}
				};
				
			mi_rel.addMultiListener( listener );
						
			MenuItem mi_size = mm.addMenuItem(MenuManager.MENU_DOWNLOAD_CONTEXT, "rcm.contextmenu.lookupsize");
		
			
			torrent_menus.add( mi_size );
			
			mi_size.setStyle( TableContextMenuItem.STYLE_PUSH );

			mi_size.setGraphic( menu_icon );
				
			mi_size.addFillListener(
				new MenuItemFillListener()
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		data )
					{
						Download[]	rows = (Download[])data;
						
						int	num_ok = 0;
						
						for ( Download dl: rows ){
							
							if ( dl.getDiskManagerFileCount() == 1 ){
							
								if ( dl.getDiskManagerFileInfo(0).getLength() >= RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
								
									num_ok++;
								}
							}
						}
						
						menu.setVisible( num_ok > 0 );
					}
				});
			
			mi_size.addMultiListener( 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						Download[]	rows = (Download[])target;
						
						for ( Download dl: rows ){
							
							String[] networks = PluginCoreUtils.unwrap( dl ).getDownloadState().getNetworks();
							
							if ( networks == null || networks.length == 0 ){
								
								networks = new String[]{ AENetworkClassifier.AT_PUBLIC };
							}
							
							if ( dl.getDiskManagerFileCount() == 1 ){
								
								long len = dl.getDiskManagerFileInfo(0).getLength();
								
								if ( len >= RelatedContentManager.FILE_ASSOC_MIN_SIZE ){

									explicitSearch( len, networks );
								}
							}
						}
					}
				});
		
		TableManager	table_manager = plugin_interface.getUIManager().getTableManager();

		String[]	file_table_ids = {
				TableManager.TABLE_TORRENT_FILES,
		};
				
		for (int i = 0; i < file_table_ids.length; i++){ 
			
			String table_id = file_table_ids[i];
			
			TableContextMenuItem mi = table_manager.addContextMenuItem( table_id, "rcm.contextmenu.lookupsize");
		
			torrent_menus.add( mi );
			
			mi.setStyle( TableContextMenuItem.STYLE_PUSH );

			mi.setGraphic( menu_icon );
			
			mi.addFillListener(
				new MenuItemFillListener()
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		data )
					{
						TableRow[]	rows = (TableRow[])data;
						
						int	num_ok = 0;
						
						for ( TableRow row: rows ){
							
							DiskManagerFileInfo file = (DiskManagerFileInfo)row.getDataSource();
							
							if ( file.getLength() >= RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
								
								num_ok++;
							}
						}
						
						menu.setEnabled( num_ok > 0 );
					}
				});
			
			mi.addMultiListener( 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						TableRow[]	rows = (TableRow[])target;
						
						for ( TableRow row: rows ){
							
							DiskManagerFileInfo file = (DiskManagerFileInfo)row.getDataSource();
							
							if ( file.getLength() >= RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
							
								String[] networks = null;
								
								try{
									networks = PluginCoreUtils.unwrap( file.getDownload()).getDownloadState().getNetworks();
									
								}catch( Throwable e ){
								}
								
								if ( networks == null || networks.length == 0 ){
									
									networks = new String[]{ AENetworkClassifier.AT_PUBLIC };
								}
								
								explicitSearch( file.getLength(), networks );
							}
						}
					}
				});
		}
	}
	
	protected void
	explicitSearch(
		Download		download )
	{
		addSearch( download );
	}
	
	protected void
	explicitSearch(
		long			file_size,
		String[]		networks )
	{
		addSearch( file_size, networks );
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
				
				// place before the Subscriptions entry as there may be a lot of subs and we'd prefer
				// not to be pushed right down
				
				MdiEntry mdiEntry = mdi.createEntryFromSkinRef(
						MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY,
						SIDEBAR_SECTION_RELATED_CONTENT, "rcmview",
						main_view_info.getTitle(),
						main_view_info, null, true, 
						"~" + MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS );

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
					
					synchronized( RelatedContentUISWT.this ){
						
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
					
					synchronized( RelatedContentUISWT.this ){
						
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
			
			root_menu = menu_item;

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

			menu_item = menu_manager.addMenuItem( parent_id, "rcm.menu.findbyhash" );
			
			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
							SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
									"rcm.menu.findbyhash.title", "rcm.menu.findbyhash.msg" );
							
							entryWindow.prompt(new UIInputReceiverListener() {
								public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
									if (!entryWindow.hasSubmittedInput()) {
										return;
									}
									
									String value = entryWindow.getSubmittedInput();
									
									boolean	ok = false;
									
									if ( value != null && value.length() > 0 ){
										
										value = value.trim();
										
										String[] networks = new String[]{ AENetworkClassifier.AT_PUBLIC };
										
										String[] bits = value.split( ":", 2 );
										
										if ( bits.length == 2 ){
											
											String net = AENetworkClassifier.internalise( bits[0].trim() );
											
											if ( net != null ){
												
												networks[0] = net;
												
												value = bits[1].trim();
											}
										}
										
										byte[] hash = UrlUtils.decodeSHA1Hash( value.trim());
										
										if ( hash == null ){
											
											try{
												String url = UrlUtils.parseTextForURL( value, true );
												
												if ( url != null && url.startsWith( "magnet" )){
													
													int	pos = url.indexOf( "btih:" );
													
													if ( pos > 0 ){
														
														url = url.substring( pos+5 );
														
														pos = url.indexOf( '&' );
														
														if ( pos != -1 ){
															
															url = url.substring( 0, pos );
														}
														
														hash = UrlUtils.decodeSHA1Hash( url );
													}
												}
											}catch( Throwable e ){
												
											}
										}
										if ( hash != null ){
										
											addSearch( hash, networks, ByteFormatter.encodeString( hash ));
											
											ok = true;
										}
									}
									
									if ( !ok ){
										
										MessageBox mb = new MessageBox( Utils.findAnyShell(), SWT.ICON_ERROR | SWT.OK);
										
										mb.setText( MessageText.getString( "rcm.menu.findbyhash.invalid.title" ));
										
										mb.setMessage(
											MessageText.getString(
												"rcm.menu.findbyhash.invalid.msg",
												new String[]{ value }));

										mb.open();
									}
								}
							}); 	
						}
					});
			
			menu_item = menu_manager.addMenuItem( parent_id, "rcm.menu.findbysize" );
			
			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
							SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
									"rcm.menu.findbysize.title", "rcm.menu.findbysize.msg" );
							
							entryWindow.prompt(new UIInputReceiverListener() {
								public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
									if (!entryWindow.hasSubmittedInput()) {
										return;
									}
									
									String value = entryWindow.getSubmittedInput();
									
									boolean	ok = false;
									
									if ( value != null && value.length() > 0 ){
										
										value = value.replaceAll( ",", "" ).trim();
										
										String[] networks = new String[]{ AENetworkClassifier.AT_PUBLIC };
										
										String[] bits = value.split( ":" );
										
										if ( bits.length == 2 ){
											
											String net = AENetworkClassifier.internalise( bits[0].trim() );
											
											if ( net != null ){
												
												networks[0] = net;
												
												value = bits[1].trim();
											}
										}
										
										try{
											long	file_size = Long.parseLong( value );
										
											if ( file_size >= RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
												
												addSearch( file_size, networks );
											
												ok = true;
											}
										}catch( Throwable e ){
										}
									}
									
									if ( !ok ){
										
										MessageBox mb = new MessageBox( Utils.findAnyShell(), SWT.ICON_ERROR | SWT.OK);
										
										mb.setText( MessageText.getString( "rcm.menu.findbysize.invalid.title" ));
										
										mb.setMessage(
											MessageText.getString(
												"rcm.menu.findbysize.invalid.msg",
												new String[]{ value, DisplayFormatters.formatByteCountToKiBEtc( RelatedContentManager.FILE_ASSOC_MIN_SIZE ) }));

										mb.open();
									}
								}
							}); 	
						}
					});
			
			menu_item = menu_manager.addMenuItem( parent_id, "rcm.menu.findbyexpr" );
			
			menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
							SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
									"rcm.menu.findbyexpr.title", "rcm.menu.findbyexpr.msg" );
							
							entryWindow.prompt(new UIInputReceiverListener() {
								public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
									if (!entryWindow.hasSubmittedInput()) {
										return;
									}
									
									String value = entryWindow.getSubmittedInput();
									
									boolean	ok = false;
									
									if ( value != null && value.length() > 0 ){
										
										value = value.replaceAll( ",", "" ).trim();
										
										String[] networks = new String[]{ AENetworkClassifier.AT_PUBLIC };
										
										String[] bits = value.split( ":" );
										
										if ( bits.length == 2 ){
											
											String net = AENetworkClassifier.internalise( bits[0].trim() );
											
											if ( net != null ){
												
												networks[0] = net;
												
												value = bits[1].trim();
											}
										}
																						
										addSearch( value, networks );
									}
								}
							}); 	
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

	public void
	addSearch(
		final Download		download )
	{
		Torrent	torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return;
		}
		
		byte[] hash = torrent.getHash();
		
		String[] networks = PluginCoreUtils.unwrap( download ).getDownloadState().getNetworks();

		addSearch( hash, networks, download.getName());
	}
	
	public void
	addSearch(
		final byte[]		hash,
		final String[]		networks,
		final String		name )
	{
		synchronized( this ){
			
			final RCMItem existing_si = rcm_item_map.get( hash );
			
			if (  existing_si == null ){
	
				final RCMItem new_si = new RCMItemContent( hash, networks );
				
				rcm_item_map.put( hash, new_si );
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( RelatedContentUISWT.this ){

								if ( new_si.isDestroyed()){
									
									return;
								}
								
								RCMView view = new RCMView( SIDEBAR_SECTION_RELATED_CONTENT, name + getNetworkString( networks ));
								
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
					
				existing_si.search();
				
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
	
	public void
	addSearch(
		final long 			file_size,
		final String[]		networks )
	{
		final String name = MessageText.getString( "rcm.label.filesize" ) + ": " + file_size;
		
		try{
			synchronized( this ){
				
				final String net_str = getNetworkString( networks );
				
				final byte[]	dummy_hash = (String.valueOf( file_size ) + net_str ).getBytes( "UTF-8" );
				
				final RCMItem existing_si = rcm_item_map.get( dummy_hash );
				
				if (  existing_si == null ){
		
					final RCMItem new_si = new RCMItemContent( dummy_hash, networks, file_size );
					
					rcm_item_map.put( dummy_hash, new_si );
					
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( RelatedContentUISWT.this ){
	
									if ( new_si.isDestroyed()){
										
										return;
									}
									
									RCMView view = new RCMView( SIDEBAR_SECTION_RELATED_CONTENT, name + getNetworkString( networks ) );
									
									new_si.setView( view );
									
									String key = "RCM_" + ByteFormatter.encodeString( dummy_hash );
									
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
									
									if ( net_str.length() > 0 ){
										
										UIManager			ui_manager = plugin_interface.getUIManager();
	
										MenuManager menu_manager = ui_manager.getMenuManager();
	
										MenuItem menu_item = menu_manager.addMenuItem( "sidebar." + key, "label.public" );
	
										menu_item.addListener(
											new MenuItemListener() 
											{
												public void
												selected(
													MenuItem			menu,
													Object 				target )
												{
													addSearch( file_size, new String[]{ AENetworkClassifier.AT_PUBLIC });
												}
											});
										
										menu_item = menu_manager.addMenuItem( "sidebar." + key, "sep" );

										menu_item.setStyle(MenuItem.STYLE_SEPARATOR );
									}
									
									UIManager			ui_manager = plugin_interface.getUIManager();
									
									MenuManager menu_manager = ui_manager.getMenuManager();

									MenuItem menu_item = menu_manager.addMenuItem( "sidebar." + key, "rcm.menu.searchmore" );

									menu_item.addListener(
										new MenuItemListener() 
										{
											public void
											selected(
												MenuItem			menu,
												Object 				target )
											{
												addSearch( file_size, networks );
											}
										});
									
									new_si.activate();
								}
							}
						});
				}else{
					
					existing_si.search();

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
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public void
	addSearch(
		final String 		expression,
		final String[]		networks )
	{
		final String name = "'" + expression + "'";
		
		try{
			synchronized( this ){
				
				final String net_str = getNetworkString( networks );
				
				final byte[]	dummy_hash = (name + net_str ).getBytes( "UTF-8" );
				
				final RCMItem existing_si = rcm_item_map.get( dummy_hash );
				
				if (  existing_si == null ){
		
					final RCMItem new_si = new RCMItemContent( dummy_hash, networks, expression );
					
					rcm_item_map.put( dummy_hash, new_si );
					
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( RelatedContentUISWT.this ){
	
									if ( new_si.isDestroyed()){
										
										return;
									}
									
									RCMView view = new RCMView( SIDEBAR_SECTION_RELATED_CONTENT, name + getNetworkString( networks ) );
									
									new_si.setView( view );
									
									String key = "RCM_" + ByteFormatter.encodeString( dummy_hash );
									
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
									
									UIManager			ui_manager = plugin_interface.getUIManager();
									
									MenuManager menu_manager = ui_manager.getMenuManager();

									MenuItem menu_item = menu_manager.addMenuItem( "sidebar." + key, "rcm.menu.searchmore" );

									menu_item.addListener(
										new MenuItemListener() 
										{
											public void
											selected(
												MenuItem			menu,
												Object 				target )
											{
												addSearch( expression, networks );
											}
										});
									
									new_si.activate();
								}
							}
						});
				}else{
					
					existing_si.search();

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
		}catch( Throwable e ){
			
			Debug.out( e );
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
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT && plugin.isAllSources() ){
				
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
		private String			name;
		
		private int				num_unread;
		
		protected
		RCMView(
			String			_parent_key,
			String			_name )
		{
			name		= _name;
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{		
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT && plugin.isAllSources() ){
				
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
		
	private String
	getNetworkString(
		String[]		networks )
	{
		if ( networks == null || networks.length == 0 ){
			
			return( "" );
			
		}else if ( networks.length == 1 ){
			
			if ( networks[0] != AENetworkClassifier.AT_PUBLIC ){
				
				return( " [" + networks[0] + "]" );
				
			}else{
				
				return( "" );
			}
		}else{
	
			String str = "";
			
			for ( String net: networks ){
				
				str += (str.length()==0?"":",") + net;
			}
			
			return( " [" + str + "]" );
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
							synchronized( RelatedContentUISWT.this ){

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
		
		public void
		search();
		
		public boolean
		isDestroyed();
	}
	
	public class
	RCMItemContent
		implements RCMItem
	{	
		private byte[]				hash;
		private long				file_size;
		private String				expression;
		
		private String[]			networks;
		
		private RCMView				view;
		private MdiEntry			sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private MdiEntryVitalityImage	spinner;
		
		private List<RelatedContent>	content_list = new ArrayList<RelatedContent>();
		
		private int	num_unread;
		
		private CopyOnWriteList<RelatedContentEnumeratorListener>	listeners = new CopyOnWriteList<RelatedContentEnumeratorListener>();
		
		private ByteArrayHashMap<String>	uniques = new ByteArrayHashMap<String>();
		
		private int		lookup_starts;
		
		protected
		RCMItemContent(
			byte[]		_hash,
			String[]	_networks )
		{
			hash		= _hash;
			networks	= _networks;
		}
		
		protected
		RCMItemContent(
			byte[]		_hash,
			String[]	_networks,
			long		_file_size )
		{
			hash		= _hash;
			networks	= _networks;
			file_size	= _file_size;
		}
		
		protected
		RCMItemContent(
			byte[]		_hash,
			String[]	_networks,
			String		_expression )
		{
			hash		= _hash;
			networks	= _networks;
			expression	= _expression;
		}
		
		public void
		setMdiEntry(
			MdiEntry _sb_entry )
		{
			sb_entry	= _sb_entry;
			
			if ( sb_entry != null ){
				
				sb_entry.setDatasource( this );
				
				sb_entry.addListener( this );
				
				spinner = sb_entry.addVitalityImage( SPINNER_IMAGE_ID );
			}
			
			search();
		}
		
		public void
		search()
		{
			try{
				lookupStarts();
				
				final RelatedContentLookupListener listener =
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
							List<RelatedContent>	content_new = new ArrayList<RelatedContent>( content.length );
							
							synchronized( RCMItemContent.this ){
							
								if ( destroyed ){
								
									return;
								}
								
								for ( RelatedContent c: content ){
									
									if ( !content_list.contains( c )){
																			
										byte[] hash = c.getHash();
										
										if ( hash == null ){
											
											hash = c.getTitle().getBytes();
										}
																					
										if ( uniques.put( hash, "" ) == null ){

											content_new.add( c );
											
											content_list.add( c );
										}
									}
								}
							}
							
							int	num_new = content_new.size();
								
							if ( num_new == 0 ){
									
								return;
							}
								
							if ( num_new != content.length ){
									
								content = content_new.toArray( new RelatedContent[content_new.size()]); 
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
							lookupEnds();
						}
						
						public void
						lookupFailed(
							ContentException e )
						{	
							lookupComplete();
						}
					};
					
				
					
				if ( expression != null ){
					
					Map<String,Object>	parameters = new HashMap<String, Object>();
					
					parameters.put( SearchProvider.SP_SEARCH_TERM, expression );
					
					manager.searchRCM(
						parameters, 
						new SearchObserver() {
							
							public void 
							resultReceived(
								SearchInstance 		search, 
								SearchResult 		search_result ) 
							{
								SearchRelatedContent result = new SearchRelatedContent( search_result );
								
								listener.contentFound( new RelatedContent[]{ result });
							}
							
							public Object 
							getProperty(
								int property ) 
							{
								return null;
							}
							
							public void 
							complete() 
							{
								listener.lookupComplete();
							}
							
							public void 
							cancelled() 
							{
								listener.lookupComplete();
							}
						});
					
				}else if ( file_size != 0 ){
				
					manager.lookupContent( file_size, networks, listener );

				}else{
					
					manager.lookupContent( hash, networks, listener );
					
					SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
					
					subs_man.lookupAssociations(
						hash,
						new SubscriptionLookupListener()
						{
							private Map<String,SubsRelatedContent>	s_map = new HashMap<String,SubsRelatedContent>();

							public void
							found(
								byte[]					hash,
								Subscription			subscription )
							{
								try{
									RelatedContent[] content;
									
									if ( subscription.isSearchTemplate()){
										
										if ( !subscription.isSearchTemplateImportable()){
											
											return;
										}
										
										String sub_name = subscription.getName();
										
										int	pos = sub_name.indexOf( ":" ) + 1;
										
										String t_prefix = sub_name.substring( 0, pos ) + " ";
										String t_name 	= sub_name.substring( pos );
										
										pos	= t_name.indexOf( "(v" );
										
										int t_ver;
										
										if ( pos == -1 ){
											
											t_ver = 1;
											
										}else{
											
											String s = t_name.substring( pos+2, t_name.length()-1);
											
											t_name = t_name.substring( 0, pos );
											
											try{
										
												t_ver = Integer.parseInt(s);
												
											}catch( Throwable e ){
												
												t_ver = 1;
											}
										}
										
										t_name = t_name.trim();
										
										synchronized( RCMItemContent.this ){
													
											if ( destroyed ){
												
												return;
											}
											
											SubsRelatedContent existing = s_map.get( t_name );
											
											if ( existing != null ){
												
												int e = existing.getRank();
												
												if ( e >= t_ver ){
													
													return;
												}
												
												existing.setRank( t_ver );
												
												existing.setSubscription( subscription );
												
												return;
												
											}else{
												
												existing = new SubsRelatedContent( subscription, t_prefix + t_name );
												
												s_map.put( t_name, existing );
												
												existing.setRank( t_ver );
												
												content_list.add( existing );
												
												content = new RelatedContent[]{ existing };
											}
										}
											
									}else{
									
										synchronized( RCMItemContent.this ){
												
											if ( destroyed ){
												
												return;
											}
	
											String id = subscription.getID();
											
											SubsRelatedContent existing = s_map.get( id );
											
											if ( existing == null ){
												
												existing = new SubsRelatedContent( subscription, subscription.getName());
												
												s_map.put( id, existing );
												
												content_list.add( existing );
												
												content = new RelatedContent[]{ existing };
												
											}else{
												
												existing.setRank( existing.getRank() + 1 );
												
												return;
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
								}catch( Throwable e ){
									
								}
							}
							
							public void
							complete(
								byte[]					hash,
								Subscription[]			subscriptions )
							{
								
							}
							
							public void
							failed(
								byte[]					hash,
								SubscriptionException	error )
							{
								
							}
						});					
				}
			}catch( Throwable e ){
								
				Debug.out( e );
				
				lookupEnds();
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
					
					if ( view != null ){
						
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
		}
		
		public void
		enumerate(
			final RelatedContentEnumeratorListener	listener )
		{
			RelatedContent[]	already_found;
			 
			synchronized( this ){
				
				//if ( lookup_starts > 0 ){
					
					listeners.add( listener );
				//}
				
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
		
		protected void
		lookupStarts()
		{
			synchronized( this ){
				
				lookup_starts++;
				
				if ( lookup_starts == 1 ){
			
					showIcon( spinner, null );
				}
			}
		}
		
		protected void
		lookupEnds()
		{
			synchronized( this ){
				
				lookup_starts--;
				
				if ( lookup_starts <= 0 ){
			
					hideIcon( spinner );
				}
			}
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
			destroy( true );
		}
		
		protected void
		destroy(
			boolean	remove_from_item_map )
		{
			synchronized( this ){
			
				content_list.clear();
				
				destroyed = true;
			}
			
			if ( remove_from_item_map ){
			
				synchronized( RelatedContentUISWT.this ){
					
					rcm_item_map.remove( hash );
				}
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
	RCMItemSubView
		extends RCMItemContent
	{
		private RCMItemSubViewListener		listener;
		private TimerEventPeriodic			update_event;
		private boolean						complete;
		
		protected
		RCMItemSubView(
			byte[]		_hash,
			String[]	_networks )
		{
			super( _hash, _networks );
		}
		
		protected
		RCMItemSubView(
			byte[]		_hash,
			String[]	_networks,
			long		_file_size )
		{
			super( _hash, _networks, _file_size );
		}
		
		private void
		setListener(
			RCMItemSubViewListener		l )
		{
			synchronized( this ){
				
				listener	 = l;
				
				if ( complete ){
					
					l.complete();
				}
			}
		}
		
		@Override
		protected void
		lookupStarts()
		{
			super.lookupStarts();
			
			synchronized( this ){
				
				if ( !complete ){
					
					update_event = 
						SimpleTimer.addPeriodicEvent( 
							"rcm:subview:updater",
							250,
							new TimerEventPerformer()
							{
								public void 
								perform(
									TimerEvent event) 
								{
									synchronized( RCMItemSubView.this ){
										
										if ( listener != null ){
											
											if ( !listener.searching()){
												
												if ( update_event != null ){
												
													update_event.cancel();
												
													update_event = null;
												}
											}
										}
									}
									
								}
							});
				}
			}
		}
		
		@Override
		protected void
		lookupEnds()
		{
			super.lookupEnds();
			
			synchronized( this ){
				
				complete = true;
				
				if ( update_event != null ){
					
					update_event.cancel();
					
					update_event = null;
				}
				
				if ( listener != null ){
					
					listener.complete();
				}
			}
		}
	}

	public interface
	RCMItemSubViewListener
	{
		public boolean
		searching();
		
		public void
		complete();
	}
	
	public class
	RCMItemSubViewEmpty
		extends RCMItemSubView
	{
		private
		RCMItemSubViewEmpty()
		{
			super( new byte[0], new String[]{ AENetworkClassifier.AT_PUBLIC });
		}
		
		public void
		setMdiEntry(
			MdiEntry _sb_entry )
		{
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

			search();
		}
		
		public void
		search()
		{
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
														
														existing = new SubsRelatedContent( subscription, subscription.getName());
														
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
				
				//if ( !lookup_complete ){
					
					listeners.add( listener );
				//}
				
				
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
			
			synchronized( RelatedContentUISWT.this ){
					
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
			Subscription	subs,
			String			name )
		{
			super( name, new byte[0], subs.getNameEx(), -1, -1, (int)(subs.getCachedPopularity()<<16), (byte)ContentNetwork.CONTENT_NETWORK_UNKNOWN );
			
			subscription = subs;
		}
		
		public Subscription
		getSubscription()
		{
			return( subscription );
		}
		
		private void
		setSubscription(
			Subscription		_subs )
		{
			subscription = _subs;
		}
		
		private void
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
	
		// duplicated from RelatedContentManager - remove sometime!
	
	protected static final byte		NET_NONE	= 0x00;
	protected static final byte		NET_PUBLIC	= 0x01;
	protected static final byte		NET_I2P		= 0x02;
	protected static final byte		NET_TOR		= 0x04;
	
	protected static byte
	convertNetworks(
		String[]		networks )
	{
		byte	nets = NET_NONE;

		for ( int i=0;i<networks.length;i++ ){
			
			String n = networks[i];
			
			if (n.equalsIgnoreCase( AENetworkClassifier.AT_PUBLIC )){
				
				nets |= NET_PUBLIC;
				
			}else if ( n.equalsIgnoreCase( AENetworkClassifier.AT_I2P )){
				
				nets |= NET_I2P;
				
			}else if ( n.equalsIgnoreCase( AENetworkClassifier.AT_TOR )){
				
				nets |= NET_TOR;
			}
		}
		
		return( nets );
	}
	
	public static class
	SearchRelatedContent
		extends RelatedContent
	{
		private int		rank;
		private boolean	unread	= true;
		
		private
		SearchRelatedContent(
			SearchResult	sr )
		{
			super( 
				(String)sr.getProperty( SearchResult.PR_NAME ),
				(byte[])sr.getProperty( SearchResult.PR_HASH ),
				null,	// tracker
				(byte[])sr.getProperty( RelatedContentManager.RCM_SEARCH_PROPERTY_TRACKER_KEYS ),
				(byte[])sr.getProperty( RelatedContentManager.RCM_SEARCH_PROPERTY_WEB_SEED_KEYS ),
				(String[])sr.getProperty( RelatedContentManager.RCM_SEARCH_PROPERTY_TAGS ),
				convertNetworks((String[])sr.getProperty( RelatedContentManager.RCM_SEARCH_PROPERTY_NETWORKS )),
				(Long)sr.getProperty( SearchResult.PR_SIZE ),
				getDate( sr ),
				getSeedsLeechers( sr ),
				(byte)ContentNetwork.CONTENT_NETWORK_UNKNOWN );
			
			Long l_rank = (Long)sr.getProperty( SearchResult.PR_RANK );
			
			if ( l_rank != null ){
				
				rank = l_rank.intValue();
			}
		}
		
		private static int
		getDate(
			SearchResult		sr )
		{
			Date date = (Date)sr.getProperty( SearchResult.PR_PUB_DATE );
			
			if ( date == null ){
				
				return(0);
			}
			
			return((int)(date.getTime()/(60*60*1000)));	
		}
		
		private static int
		getSeedsLeechers(
			SearchResult		sr )
		{
			int seeds 		= ((Long)sr.getProperty( SearchResult.PR_SEED_COUNT )).intValue();
			int leechers 	= ((Long)sr.getProperty( SearchResult.PR_LEECHER_COUNT )).intValue();
			
			return( seeds << 16 | leechers );
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
			return( unread );
		}
		
		public void 
		setUnread(
			boolean _unread )
		{
			unread	= _unread;
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
	
	private class 
	ImageLabel 
		extends Canvas implements PaintListener
	{
		private Image		image;
		
		public 
		ImageLabel(
			Composite 	parent,
			Image		_image )
		{
			super( parent, SWT.DOUBLE_BUFFERED );
		
			image	= _image;
			
			addPaintListener(this);
		}
		
		public void 
		paintControl(
			PaintEvent e) 
		{
			if ( !image.isDisposed()){
			
				e.gc.drawImage( image, 0, 0 );
			}
		}


		public Point 
		computeSize(
			int 	wHint, 
			int 	hHint, 
			boolean changed ) 
		{
			if ( image.isDisposed()){
				return( new Point(0,0));
			}
			
			Rectangle rect = image.getBounds();
			
			return( new Point( rect.width, rect.height ));
		}

		private void
		setImage(
			Image	_image )
		{
			image	= _image;
						
			redraw();
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

	protected void showFTUX( SWTSkinObject so_list) {
		showFTUX( so_list, null );
	}
	
	public void showFTUX( UserPrompterResultListener listener ) {
		showFTUX( null, listener );
	}
	
	private void showFTUX(final SWTSkinObject so_list, final  UserPrompterResultListener listener ) {
		final VuzeMessageBox box = new VuzeMessageBox(MessageText.getString("rcm.ftux.title"), null, new String[] {
			MessageText.getString("rcm.ftux.accept"),
			MessageText.getString("rcm.ftux.decline"),
		}, 0);
		
		final int[] radioResult = { -1 };
		box.setSubTitle(MessageText.getString("rcm.ftux.heading"));
		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				addResourceBundle(skin, "com/aelitis/plugins/rcmplugin/skins/",
						"skin3_rcm_ftux");

				String id = "rcm.ftux.shell";
				skin.createSkinObject(id, id, soExtra);

				box.setButtonEnabled(0, false);

				// dummy button so Windows doesn't automatically select the first one
				new Button(soExtra.getComposite(), SWT.RADIO);
				
				final  Button [] buttons = { null, null };
				final SWTSkinObjectContainer soOption1 = (SWTSkinObjectContainer) skin.getSkinObject("option-preselect");
				if (soOption1 != null) {
					
					buttons[0] = new Button(soOption1.getComposite(), SWT.RADIO);
					buttons[0].addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							box.setButtonEnabled(0, true);	
							radioResult[0] = 0;
							if (buttons[1] != null) {
								buttons[1].setSelection(false);
							}
						}
					});
				}
				
				SWTSkinObjectContainer soOption2 = (SWTSkinObjectContainer) skin.getSkinObject("option-all");
				if (soOption2 != null) {
					buttons[1] = new Button(soOption2.getComposite(), SWT.RADIO);
					buttons[1].addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							box.setButtonEnabled(0, true);				
							radioResult[0] = 1;
							if (buttons[0] != null) {
								buttons[0].setSelection(false);
							}
						}
					});
				}
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result < 0) {
					if (so_list != null) {
						UIFunctionsManager.getUIFunctions().openView(UIFunctions.VIEW_MYTORRENTS, null);
					}
					
					if ( listener != null ){
						listener.prompterClosed(result);
					}
					return;
				}
				boolean enabled = result == 0;
				
				if (enabled && radioResult[0] == 1) {
					showFTUX2(new UserPrompterResultListener() {
						public void prompterClosed(int result) {
							if (result == 0) {
								enableRCM(true, true, so_list);
							} else if (so_list != null) {
								UIFunctionsManager.getUIFunctions().openView(UIFunctions.VIEW_MYTORRENTS, null);
							}
							
							if ( listener != null ){
								listener.prompterClosed(result);
							}
						}
					});
					return;
				}
				enableRCM(enabled, false, so_list);
				
				if ( listener != null ){
					listener.prompterClosed(result);
				}
			}
		});
		
		box.waitUntilClosed();
	}

	private static void enableRCM(boolean enabled, boolean all, SWTSkinObject so_list) {
		if ( so_list != null ){
			so_list.setVisible(enabled);
		}

		RelatedContentUISWT ui = RelatedContentUISWT.getSingleton();

		if (ui != null) {
			if (enabled) {
				ui.plugin.setRCMEnabled(enabled);
			}
			ui.setSearchEnabled(enabled);
			ui.setUIEnabled(enabled);
			ui.plugin.setFTUXBeenShown(true);
			
			if (all) {
				ui.plugin.setToAllSources();
			} else {
				ui.plugin.setToDefaultSourcesList();
			}
		}

	}

	protected static void showFTUX2(UserPrompterResultListener l) {
		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("rcm.ftux2.title"), null, new String[] {
					MessageText.getString("Button.ok"),
					MessageText.getString("Button.cancel"),
				}, 0);
		box.setSubTitle(MessageText.getString("rcm.ftux2.heading"));
		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				addResourceBundle(skin, "com/aelitis/plugins/rcmplugin/skins/",
						"skin3_rcm_ftux2");

				String id = "rcm.ftux2.shell";
				skin.createSkinObject(id, id, soExtra);

				box.setButtonEnabled(0, false);

				final SWTSkinObjectCheckbox cb = (SWTSkinObjectCheckbox) skin.getSkinObject("agree-checkbox");
				cb.addSelectionListener(new SWTSkinCheckboxListener() {
					public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
						box.setButtonEnabled(0, checked);
					}
				});
			}
		});
		
		box.open(l);
	}
}
