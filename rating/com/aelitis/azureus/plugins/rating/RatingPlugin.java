/*
 * Created on 11 mars 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.plugins.rating;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.plugins.rating.ui.RatingColumn;
import com.aelitis.azureus.plugins.rating.ui.RatingImageUtil;
import com.aelitis.azureus.plugins.rating.ui.RatingWindow;
import com.aelitis.azureus.plugins.rating.updater.RatingsUpdater;

public class RatingPlugin implements UnloadablePlugin, PluginListener {
  
  private static final String COLUMN_ID_RATING = "RatingColumn";  
  
  private PluginInterface pluginInterface;
  
  private UISWTInstance		swt_ui;
  private LoggerChannel     log;
  
  private UIManagerListener			ui_listener;
  
  private BasicPluginConfigModel		config_model;
  private List<TableColumn>				table_columns = new ArrayList<TableColumn>();
  private List<TableContextMenuItem>	table_menus = new ArrayList<TableContextMenuItem>();
  
  private String nick;
  private RatingsUpdater updater;
  
  private static String[] table_names = {
	  TableManager.TABLE_MYTORRENTS_INCOMPLETE,
	  TableManager.TABLE_MYTORRENTS_COMPLETE,
	  TableManager.TABLE_MYTORRENTS_ALL_BIG,
	  TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
	  TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
	  TableManager.TABLE_MYTORRENTS_UNOPENED,
	  TableManager.TABLE_MYTORRENTS_UNOPENED_BIG,
  };
  
  public void 
  initialize(
		PluginInterface _pluginInterface) 
  {
    this.pluginInterface = _pluginInterface;
    
    log = pluginInterface.getLogger().getChannel("Rating Plugin");
    
    ui_listener = 
    	new UIManagerListener()
		{
			public void
			UIAttached(
				UIInstance		instance )
			{
				if ( instance instanceof UISWTInstance ){
					
					UISWTInstance	swt = (UISWTInstance)instance;
					
					initialise( swt );
				}
			}
			
			public void
			UIDetached(
				UIInstance		instance )
			{
				
			}
		};
	
    pluginInterface.getUIManager().addUIListener( ui_listener );
	
  }
  
  public void 
  unload() 
  
  	throws PluginException 
  {
		if ( updater != null ){
			
			updater.destroy();
		}
		
		if ( config_model != null ){
			
			config_model.destroy();
			
			config_model = null;
		}
		
		for ( TableColumn c: table_columns ){
			
			c.remove();
		}
		
		table_columns.clear();
		
		for ( TableContextMenuItem m: table_menus ){
			
			m.remove();
		}
		
		table_menus.clear();
		
		if ( pluginInterface != null ){
			
			pluginInterface.getUIManager().removeUIListener( ui_listener );
			
			pluginInterface.removeListener( this );
		}
  }
  
  protected void
  initialise(
	  UISWTInstance	_swt )
  {  
	swt_ui	= _swt;
	  
	RatingImageUtil.init(_swt.getDisplay());
        
    addPluginConfig();
    
    updater = new RatingsUpdater(this);
    
    pluginInterface.addListener(this);       
    
    addMyTorrentsColumn();
    
    addMyTorrentsMenu();
  }
  
  public void closedownComplete() {
  }
  
  public void closedownInitiated() {
  }
  
  public void initializationComplete() {
    updater.initialize();        
  }
  
  
  private void addPluginConfig()  {
	  
	config_model = pluginInterface.getUIManager().createBasicPluginConfigModel( "rating.config.title" );
	
	final StringParameter nick_param = config_model.addStringParameter2( "nick","rating.config.nick","");
	
	nick = nick_param.getValue().trim();
	
	if ( nick.length() == 0 ){
		
		nick = "Anonymous";
	}
	
	nick_param.addListener(
		new ParameterListener()
		{
			public void 
			parameterChanged(
				Parameter param )
			{
				String val = nick_param.getValue().trim();
				
				if ( val.length() == 0 ){
					
					val = "Anonymous";
				}
				
				nick = val;
			}
		});
  }
  
  private void addMyTorrentsColumn() {
    RatingColumn ratingColumn = new RatingColumn(this);
    for ( String table_name: table_names ){
    	addRatingColumnToTable(table_name,ratingColumn);
    }
  }
  
  private void addRatingColumnToTable(String tableID,RatingColumn ratingColumn) {
    UIManager uiManager = pluginInterface.getUIManager();
    TableManager tableManager = uiManager.getTableManager();
    TableColumn activityColumn = tableManager.createColumn(tableID, COLUMN_ID_RATING);
   
    activityColumn.setAlignment(TableColumn.ALIGN_LEAD);
    activityColumn.setPosition(5);
    activityColumn.setWidth(95);
    activityColumn.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
    activityColumn.setType(TableColumn.TYPE_GRAPHIC);
    activityColumn.setVisible( true );
    activityColumn.addListeners(ratingColumn);
    
    tableManager.addColumn(activityColumn);
    
    table_columns.add( activityColumn );
  }
  
  private void addMyTorrentsMenu()  {
    MenuItemListener  listener = 
      new MenuItemListener()
      {
        public void
        selected(
          MenuItem    _menu,
          Object      _target )
        {
          Download download = (Download)((TableRow)_target).getDataSource();
          
          if ( download == null || download.getTorrent() == null ){
            
            return;
          }
          
          if (swt_ui != null)
          	new RatingWindow(RatingPlugin.this,download);
        }
      };
    
      for ( String table_name: table_names ){
    	  TableContextMenuItem menu1 = pluginInterface.getUIManager().getTableManager().addContextMenuItem(table_name, "RatingPlugin.contextmenu.manageRating" );
      
    	  menu1.addListener( listener );
    	  
    	  table_menus.add( menu1 );
      }
   
  }
  
  public UISWTInstance
  getUI()
  {
	  return( swt_ui );
  }
  
  public PluginInterface getPluginInterface() {
    return this.pluginInterface;
  }
  
  public String getNick() {
    return nick;
  }
  
  public void logInfo(String text) {
    log.log(LoggerChannel.LT_INFORMATION,text);
  }
  
  public void logError(String text) {
    log.log(LoggerChannel.LT_ERROR,text);
  }
  
  public void logError(String text, Throwable t) {
    log.log(text, t);
  }

  public RatingsUpdater getUpdater() {
    return updater;
  }
  
}
