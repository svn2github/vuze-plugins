/*
 * Created on 2 mars 2005
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
package com.aelitis.azureus.plugins.chat.ui;

import java.util.HashMap;
import java.util.Map;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.plugins.chat.ChatPlugin;
import com.aelitis.azureus.plugins.chat.messaging.MessageListener;
import com.aelitis.azureus.plugins.chat.peer.PeerController;

public class MyTorrentsActivityIndicator  implements TableCellRefreshListener, MessageListener {
  
  PeerController controller;
  UISWTInstance swt_ui;
  Map downloadsToActivity;
  
  private static final Integer STATUS_INACTIVE  = new Integer(0);
  private static final Integer STATUS_ACTIVE    = new Integer(1);
  private static final Integer STATUS_ACTIVITY  = new Integer(2);
  
  private Graphic gInactive;
  private Graphic gActive;
  private Graphic gActivity;
  
  public MyTorrentsActivityIndicator(ChatPlugin plugin,PeerController controller, UISWTInstance _swtui) {
	  this.swt_ui = _swtui;
    this.controller = controller;
    controller.addMessageListener(this);
    downloadsToActivity = new HashMap();
     
    gInactive = swt_ui.createGraphic(plugin.imgInactive);
    gActive   = swt_ui.createGraphic(plugin.imgActive);
    gActivity = swt_ui.createGraphic(plugin.imgActivity);
    
  }

  public void refresh(TableCell cell) {
    Object dataSource = cell.getDataSource();
    if (dataSource == null || ! (dataSource instanceof Download)) {
        return; //opps something went wrong
    }        
    Download download = (Download) dataSource;
    
    Integer status = (Integer) downloadsToActivity.get(download);
    if(status == null) status = STATUS_INACTIVE;
    
    //int nStatus = status.intValue();
    if (!cell.setSortValue(status) && cell.isValid())
      return;
    
    if(status == STATUS_ACTIVE) {
      cell.setGraphic(gActive);
      cell.setToolTip("You are connected to at least one other peer using the Chat plugin");
    }
      
    
    if(status == STATUS_ACTIVITY) {
      cell.setGraphic(gActivity);
      cell.setToolTip("A message just arrived");
    }
      
    
    if(status == STATUS_INACTIVE) {
      cell.setGraphic(gInactive);
      cell.setToolTip("This torrent chat is not active");
    }
      
    
    if(status == STATUS_ACTIVITY) {
      downloadsToActivity.put(download,STATUS_ACTIVE);
    }
      
  }
  
  public void downloadAdded(Download download) {
   downloadsToActivity.put(download,STATUS_INACTIVE); 
  }
  
  public void downloadRemoved(Download download) {
    downloadsToActivity.remove(download);
  }
  
  public void downloadActive(Download download) {
    downloadsToActivity.put(download,STATUS_ACTIVE);
  }
  
  public void downloadInactive(Download download) {
    downloadsToActivity.put(download,STATUS_INACTIVE);     
  }
  
  public void messageReceived(Download download,byte[] sender,String nick,String text) {
    Integer status = (Integer) downloadsToActivity.get(download);
    if(status == STATUS_ACTIVE)
      downloadsToActivity.put(download,STATUS_ACTIVITY);
  }
  
  
}
