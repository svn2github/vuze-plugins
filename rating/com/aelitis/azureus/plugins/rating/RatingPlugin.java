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

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.plugins.rating.ui.RatingColumn;
import com.aelitis.azureus.plugins.rating.ui.RatingImageUtil;
import com.aelitis.azureus.plugins.rating.ui.RatingWindow;
import com.aelitis.azureus.plugins.rating.updater.RatingsUpdater;

public class RatingPlugin implements Plugin, ConfigParameterListener, PluginListener {
  
  private static final String COLUMN_ID_RATING = "RatingColumn";  
  
  private PluginInterface pluginInterface;
  
  private UISWTInstance		swt_ui;
  private LoggerChannel     log;
  
  private String nick;
  private RatingsUpdater updater;
  
  
  public void initialize(PluginInterface pluginInterface) {
    this.pluginInterface = pluginInterface;
    
    log = pluginInterface.getLogger().getChannel("Rating Plugin");
    /*log.addListener(new LoggerChannelListener() {
      public void messageLogged(int type,String content) {
        System.out.println("Rating Plugin::" + content); 
      }
      public void messageLogged(String str, Throwable error) {
        System.err.println("Rating Plugin::" + str + " : " + error.toString());       
      }      
    });*/
    
    
    pluginInterface.getUIManager().addUIListener(
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
			});
  }
  
  protected void
  initialise(
	  UISWTInstance	_swt )
  {  
	swt_ui	= _swt;
	  
	RatingImageUtil.init(_swt.getDisplay());
    
    nick = pluginInterface.getPluginconfig().getPluginStringParameter("nick","Anonymous");
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
    PluginConfigUIFactory factory = pluginInterface.getPluginConfigUIFactory();
    Parameter parameters[] = new Parameter[1];   
    parameters[0] = factory.createStringParameter("nick","rating.config.nick","");
    parameters[0].addConfigParameterListener(this);
    pluginInterface.addConfigUIParameters(parameters,"rating.config.title");  
  }
  
  private void addMyTorrentsColumn() {
    RatingColumn ratingColumn = new RatingColumn(this);
    
    addRatingColumnToTable(TableManager.TABLE_MYTORRENTS_INCOMPLETE,ratingColumn);
    addRatingColumnToTable(TableManager.TABLE_MYTORRENTS_COMPLETE,ratingColumn);
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
    
    activityColumn.addListeners(ratingColumn);
    
    tableManager.addColumn(activityColumn);
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
    
    TableContextMenuItem menu1 = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "RatingPlugin.contextmenu.manageRating" );
    TableContextMenuItem menu2 = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE,   "RatingPlugin.contextmenu.manageRating" );
      
    menu1.addListener( listener );
    menu2.addListener( listener );      
  }
  
  public void configParameterChanged(ConfigParameter param) {
    nick = pluginInterface.getPluginconfig().getPluginStringParameter("nick","Anonymous");
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
