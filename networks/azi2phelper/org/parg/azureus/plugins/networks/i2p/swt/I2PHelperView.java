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
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
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

public class 
I2PHelperView 
	implements UISWTViewEventListener
{
	private I2PHelperPlugin		plugin;
	private UISWTInstance		ui;
	private String				view_id;
	
	private UISWTView			current_view;
	

	private Composite	composite;

	private DHTView 		dht_view;
	private DHTOpsView		ops_view;
	
	private TimerEventPeriodic	timer;
	
	public
	I2PHelperView(
		I2PHelperPlugin	_plugin,
		UIInstance		_ui,
		String			_view_id )
	{
		plugin	= _plugin;
		ui 		= (UISWTInstance)_ui;
		view_id	= _view_id;
		
		ui.addView( UISWTInstance.VIEW_MAIN, view_id, this );
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
		
			// DHT stats view
		
		CTabItem stats_item = new CTabItem(tab_folder, SWT.NULL);

		stats_item.setText( plugin.getMessageText("azi2phelper.ui.dht_stats") );
		
		dht_view = new DHTView( false );
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

		ops_item.setText( plugin.getMessageText("azi2phelper.ui.dht_graph") );
		
		ops_view = new DHTOpsView( false, false );
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
		
		tab_folder.setSelection( stats_item );
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
				
				timer = SimpleTimer.addPeriodicEvent(
					"I2PView:stats",
					1000,
					new TimerEventPerformer() 
					{
						public void 
						perform(
							TimerEvent event ) 
						{
							if ( dht_view != null ){
								
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
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
				
				if ( dht_view != null && ops_view != null ){
					
					I2PHelperRouter router = plugin.getRouter();
					
					if ( router != null ){
						
						I2PHelperDHT dht_helper = router.getDHT();
						
						if ( dht_helper instanceof DHTI2P ){
							
							DHT dht = ((DHTI2P)dht_helper).getDHT();
							
							dht_view.setDHT( dht );
							ops_view.setDHT( dht );
						}
					}
					
					dht_view.eventOccurred(event);
					ops_view.eventOccurred(event);
				}
				
				break;
			}
			case UISWTViewEvent.TYPE_CLOSE:
			case UISWTViewEvent.TYPE_DESTROY:{
				
				try{
					composite = null;
					
					if ( timer != null ){
						
						timer.cancel();
						
						timer = null;
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
		ui.removeViews( UISWTInstance.VIEW_MAIN, view_id );
	}
}
