/*
 * Created on Jan 30, 2008
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


package org.parg.azureus.plugins.networks.i2p.swt;



import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.stats.DHTOpsView;
import org.gudy.azureus2.ui.swt.views.stats.DHTView;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;
import org.parg.azureus.plugins.networks.i2p.I2PHelperDHT;
import org.parg.azureus.plugins.networks.i2p.I2PHelperPlugin;
import org.parg.azureus.plugins.networks.i2p.I2PHelperRouter;
import org.parg.azureus.plugins.networks.i2p.vuzedht.DHTI2P;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

public class 
I2PHelperView 
	implements UISWTViewEventListener
{
	private static final String	resource_path = "org/parg/azureus/plugins/networks/i2p/swt/";

	private static final boolean	SHOW_AZ_DHTS = true;
	
	private I2PHelperPlugin		plugin;
	private UISWTInstance		ui;
	private String				view_id;
	
	private UISWTView			current_view;
	

	private Composite	composite;

	private DHTView[] 			dht_views;
	private DHTOpsView[]		ops_views;
	
	private TimerEventPeriodic	view_timer;
	private TimerEventPeriodic	status_timer;
	
	private UISWTStatusEntry	status_icon;

	private Image				img_sb_enabled;
	private Image				img_sb_disabled;
	
	
	private boolean		unloaded;
	
	public
	I2PHelperView(
		I2PHelperPlugin	_plugin,
		UIInstance		_ui,
		String			_view_id )
	{
		plugin	= _plugin;
		ui 		= (UISWTInstance)_ui;
		view_id	= _view_id;
		
		int	view_count = plugin.getDHTCount();
		
		if ( SHOW_AZ_DHTS ){
			
			view_count *= 2;
		}
		
		dht_views	= new DHTView[ view_count ];
		ops_views	= new DHTOpsView[ view_count ];
		
		ui.addView( UISWTInstance.VIEW_MAIN, view_id, this );
		
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					synchronized( I2PHelperView.this ){
						
						if ( unloaded ){
							
							return;
						}
						
						status_icon	= ui.createStatusEntry();
						
						status_icon.setImageEnabled( true );
						
						status_icon.setVisible( true );
																	
						UISWTStatusEntryListener status_listener = 
							new UISWTStatusEntryListener()
							{
								public void 
								entryClicked(
									UISWTStatusEntry entry )
								{
									ui.openView( UISWTInstance.VIEW_MAIN, view_id, null );
								}
							};
							
						status_icon.setListener( status_listener );
							
						img_sb_disabled	 	= loadImage( ui, "sb_i2p_disabled.png" );
						img_sb_enabled	 	= loadImage( ui, "sb_i2p_running.png" );
						
						boolean	enabled = plugin.isEnabled();
						
						status_icon.setImage( enabled?img_sb_enabled:img_sb_disabled);
						
						MenuItem mi_options =
								plugin.getPluginInterface().getUIManager().getMenuManager().addMenuItem(
										status_icon.getMenuContext(),
										"MainWindow.menu.view.configuration" );

						mi_options.addListener(
							new MenuItemListener()
							{
								public void
								selected(
									MenuItem			menu,
									Object 				target )
								{
									UIFunctions uif = UIFunctionsManager.getUIFunctions();

									if ( uif != null ){

										uif.openView( UIFunctions.VIEW_CONFIG, "azi2phelper.name" );
									}
								}
							});

						status_icon.setTooltipText( plugin.getStatusText());

						if ( enabled ){
							
							status_timer = SimpleTimer.addPeriodicEvent(
									"I2PView:status",
									10*1000,
									new TimerEventPerformer() 
									{
										public void 
										perform(
											TimerEvent event ) 
										{
											if ( unloaded ){
												
												TimerEventPeriodic timer = status_timer;
												
												if ( timer != null ){
													
													timer.cancel();
												}
												
												return;
											}
											
											status_icon.setTooltipText( plugin.getStatusText());
										}
									});
						}
					}
				}
			});
	}
	
	protected Image
	loadImage(
		UISWTInstance	swt,
		String			name )
	{
		Image	image = swt.loadImage( resource_path + name );

		return( image );
	}
	
	protected Graphic
	loadGraphic(
		UISWTInstance	swt,
		String			name )
	{
		Image	image = swt.loadImage( resource_path + name );

		Graphic graphic = swt.createGraphic(image );
				
		return( graphic );
	}
	
	protected void
	initialise(
		Composite	_composite )
	{
		composite	= _composite;
		
		Composite main = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		main.setLayoutData(grid_data);
		
		CTabFolder  tab_folder = new CTabFolder(main, SWT.LEFT);
		tab_folder.setBorderVisible(true);
		tab_folder.setTabHeight(20);
		
		Label lblClose = new Label(tab_folder, SWT.WRAP);
		lblClose.setText("x");
		lblClose.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				
			}
		});
		tab_folder.setTopRight(lblClose);
		
		grid_data = new GridData(GridData.FILL_BOTH);
		tab_folder.setLayoutData(grid_data);
		
		CTabItem first_stats_item = null;
		
		int dht_count = plugin.getDHTCount();

		for ( int i=0;i<dht_views.length;i++){
				
				// DHT stats view
			
			CTabItem stats_item = new CTabItem(tab_folder, SWT.NULL);
	
			if ( i == 0 ){
				
				first_stats_item = stats_item;
			}
			
			String stats_text = plugin.getMessageText("azi2phelper.ui.dht_stats" + (i%dht_count));
			String graph_text = plugin.getMessageText("azi2phelper.ui.dht_graph" + (i%dht_count));
			
			if ( i >= dht_count ){
				
				stats_text = stats_text + " (AZ)";
				graph_text = graph_text + " (AZ)";
			}
			
			stats_item.setText( stats_text );
			
			DHTView dht_view = dht_views[i] = new DHTView( false );
			Composite stats_comp = new Composite( tab_folder, SWT.NULL );
			stats_item.setControl( stats_comp );
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			stats_comp.setLayout(layout);
			grid_data = new GridData(GridData.FILL_BOTH);
			stats_comp.setLayoutData(grid_data);
			dht_view.initialize( stats_comp );
				
			fixupView( stats_comp );
			
				// DHT Graph view
			
			CTabItem ops_item = new CTabItem(tab_folder, SWT.NULL);
	
			ops_item.setText( graph_text );
			
			DHTOpsView ops_view = ops_views[i] = new DHTOpsView( false, false );
			Composite ops_comp = new Composite( tab_folder, SWT.NULL );
			ops_item.setControl( ops_comp );
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			ops_comp.setLayout(layout);
			grid_data = new GridData(GridData.FILL_BOTH);
			ops_comp.setLayoutData(grid_data);
			ops_view.initialize( ops_comp );
				
			fixupView( ops_comp );
		}
		
		tab_folder.setSelection( first_stats_item );
	}

	private void
	fixupView(
		Composite	comp )
	{
			// hack to do the same as normal view construction code :(
			
		Control[] children = comp.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			Object layoutData = control.getLayoutData();
			if (layoutData == null || !(layoutData instanceof GridData)) {
				GridData grid_data;
				if (children.length == 1)
					grid_data = new GridData(SWT.FILL, SWT.FILL, true, true);
				else
					grid_data = new GridData();
	
				control.setLayoutData(grid_data);
			}
		}	
	}
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
		switch( event.getType() ){

			case UISWTViewEvent.TYPE_CREATE:{
				
				if ( current_view != null ){
					
					return( false );
				}
				
				current_view = event.getView();
				
				view_timer = SimpleTimer.addPeriodicEvent(
					"I2PView:stats",
					1000,
					new TimerEventPerformer() 
					{
						public void 
						perform(
							TimerEvent event ) 
						{
							if ( dht_views[0] != null ){
								
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											for ( DHTView dht_view: dht_views ){
												
												dht_view.eventOccurred(
													new UISWTViewEvent() {										
														public UISWTView getView() {
															return null;
														}
														
														public int getType() {
															return( StatsView.EVENT_PERIODIC_UPDATE );
														}										
														public Object getData() {
														
															return null;
														}
													});
											}
										}
									});
							}
						}
					});
				
				break;
			}
			case UISWTViewEvent.TYPE_INITIALIZE:{
				
				initialise((Composite)event.getData());
				
				break;
			}
			case UISWTViewEvent.TYPE_REFRESH:{
				
				for ( int i=0;i<dht_views.length;i++){
					
					DHTView 	dht_view = dht_views[i];
					DHTOpsView 	ops_view = ops_views[i];
					
					if ( dht_view != null && ops_view != null ){
						
						I2PHelperRouter router = plugin.getRouter();
						
						if ( router != null ){
							
							int	view_count = plugin.getDHTCount();

							I2PHelperDHT dht_helper = router.getAllDHTs()[i%view_count].getDHT();
							
							if ( dht_helper instanceof DHTI2P ){
								
								DHT dht;
								
								if ( i < view_count ){
									
									dht = ((DHTI2P)dht_helper).getDHT();
								}else{
									
									dht = ((DHTI2P)dht_helper).getAZDHT().getDHT();
								}
								
								dht_view.setDHT( dht );
								ops_view.setDHT( dht );
							}
						}
						
						dht_view.eventOccurred(event);
						ops_view.eventOccurred(event);
					}
				}
				
				break;
			}
			case UISWTViewEvent.TYPE_CLOSE:
			case UISWTViewEvent.TYPE_DESTROY:{
				
				try{
					composite = null;
					
					if ( view_timer != null ){
						
						view_timer.cancel();
						
						view_timer = null;
					}
					
				}finally{
					
					UISWTView view = current_view;
					
					if ( view != null ){
						
						current_view = null;
						
						view.closeView();
					}
				}
				
				break;
			}
		}
		
		return true;
	}
	
	public void
	unload()
	{
		synchronized( this ){
			
			unloaded = true;
		}
		
		ui.removeViews( UISWTInstance.VIEW_MAIN, view_id );
		
		if ( status_timer != null ){
			
			status_timer.cancel();
			
			status_timer = null;
		}
		
		if ( status_icon != null ){
			
			status_icon.destroy();
			
			status_icon = null;
		}
		
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					if ( img_sb_disabled != null ){
						
						img_sb_disabled.dispose();
						
						img_sb_disabled = null;
					}
					
					if ( img_sb_enabled != null ){
						
						img_sb_enabled.dispose();
						
						img_sb_enabled = null;
					}
				}
			});
		
	}
}
