/*
 * Created on 28 févr. 2005
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;

import com.aelitis.azureus.plugins.chat.ChatPlugin;
import com.aelitis.azureus.plugins.chat.messaging.MessageListener;

public class ChatPanel implements MessageListener {

	private ChatPlugin plugin;
  
	private Composite composite;
	private ChannelPanel panel; 
	private Display display;
  
	private Label lHeader;
	private Font headerFont;
  
	private final UISWTView view;

	private Download download;

	private static final int STATE_ACTIVE = 0;
	private static final int STATE_INACTIVE = 1;
	private static final int STATE_ACTIVITY = 2;
	
	private int state = STATE_INACTIVE;
	
	private long lastActivityOn = -1;

	private boolean bMainWindow;

  public ChatPanel(ChatPlugin plugin, UISWTView view, Download download) {
    this.plugin = plugin;
		this.view = view;
		bMainWindow = download != null;
		if (bMainWindow) {
			this.download = download;
		}
  }
  
  public void initialize(Composite composite, Download forDownload) {
  	if (forDownload != null)
  		this.download = forDownload;
  	
		FontData[] fontData;
    
    display = composite.getDisplay();
    
    composite = new Composite(composite,SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;   
    composite.setLayout(gridLayout);
    
    GridData data ;
    data = new GridData(GridData.FILL_BOTH);
    composite.setLayoutData(data);

    // Header
    Composite cHeader = new Composite(composite, SWT.BORDER);
    gridLayout = new GridLayout();
    gridLayout.marginHeight = 3;
    gridLayout.marginWidth = 0;
    cHeader.setLayout(gridLayout);
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    cHeader.setLayoutData(data);

    cHeader.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
    cHeader.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

    lHeader = new Label(cHeader, SWT.NULL);
    lHeader.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
    lHeader.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
    fontData = lHeader.getFont().getFontData();
    fontData[0].setStyle(SWT.BOLD);
    int fontHeight = (int)(fontData[0].getHeight() * 1.2);
    fontData[0].setHeight(fontHeight);
    headerFont = new Font(display, fontData);
    lHeader.setFont(headerFont);
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    lHeader.setLayoutData(data);
    lHeader.setText(" Channel " + download.getTorrent().getName());

    // Channel Section
    panel = new ChannelPanel(plugin,ChatPanel.this,download,composite);

    plugin.addMessageListener(this, download);
    updateTitle();
  }

  public Composite getComposite() {
    return composite;
  }

  public String getPluginViewName() {    
    return "Chat";
  }

  public String getFullTitle() {
    return "Chat";
  }
  
  public void downloadAdded(final Download download) {
  }
  
  public void downloadRemoved(final Download download) {
  }
  
  public void downloadActive(final Download download) {
		if (state == STATE_ACTIVE)
			return;
  	
    //When a download becomes active, we send a "hello" message
    plugin.sendMessage(download,"/me has joined the channel");
    
    state = STATE_ACTIVE;
    updateTitle();
  }
  
	public void downloadInactive(final Download download) {
		if (state == STATE_INACTIVE)
			return;

    //When a download becomes inactive, display a "left" message
    plugin.sendMessage(download,"/me has left the channel");

    state = STATE_INACTIVE;
    updateTitle();
  }
  
  public void messageReceived(final Download download,final byte[] sender,final String nick,final String text) {
    if(display != null && ! display.isDisposed()) {
      display.asyncExec(new Runnable() {
        public void run() {
              panel.messageReceived(nick,text);
        }
      });
    }    
    state = STATE_ACTIVITY;
    lastActivityOn = System.currentTimeMillis();
    updateTitle();
  }
  
  public void delete() {   
    plugin.sendMessage(download,"/me has left");
    
    plugin.removeMessageListener(this);
    
    
    if(headerFont != null && ! headerFont.isDisposed()) {
      headerFont.dispose();
    }
  }
  
  public void updateLanguage() {
  	updateTitle();
  }

  /**
	 * 
	 */
	private void updateTitle() {
		String sPrefix;
		String sSuffix;
		
		switch (state) {
			case STATE_ACTIVE:
				sPrefix = "";
				sSuffix = "!";
				break;

			case STATE_ACTIVITY:
				sPrefix = "* ";
				sSuffix = "";
				break;

			default:
				sPrefix = "";
				sSuffix = "";
				break;
		}
		
		String sTitle;
		if (bMainWindow) {
			sTitle = sPrefix + plugin.getTitle() + sSuffix + ": " + download.getName();
		} else {
			sTitle = sPrefix + plugin.getTitle() + sSuffix;
		}
    view.setTitle(sTitle);
	}

	/**
	 * 
	 */
	public void refresh() {
		long now = System.currentTimeMillis();
		if (state == STATE_ACTIVITY && lastActivityOn < now - 5000) {
			state = STATE_ACTIVE;
			updateTitle();
		}
	}

}
