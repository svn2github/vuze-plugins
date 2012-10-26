/*
 * Created on 30 juil. 2005
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
package com.aelitis.azureus.plugins.view3d;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;



public class Panel3D  {

  PluginInterface plugin_interface;
  DownloadManager download_manager;
  Download[] downloads;
  
  Comparator comparator;
  
  Composite panel;
  Display display;
  
  Table channelTable;
  TableColumn name;
  Label lHeader;
  Font headerFont; 
  GridData data;
  TableItem[] items;
  TableItem item;
  
  Composite c3DViewSection;
  
  Peers3DGraphicView peer3DView;
  
  HashMap params = new HashMap();
  
  HashMap activeDownloads = new HashMap();

  public Panel3D(PluginInterface _plugin_interface, HashMap _params) {
	  plugin_interface = _plugin_interface;
	  params = _params;
  }
  
  public void initialize(Composite composite) {
    
    initUI(composite);
    
    download_manager = plugin_interface.getDownloadManager();
    download_manager.addListener(new Swarm3DDMListener());
    comparator = plugin_interface.getUtilities().getFormatters().getAlphanumericComparator(true);
    
  }

  private void initUI(Composite composite) {
    display = composite.getDisplay();
    
    panel = new Composite(composite,SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;   
    panel.setLayout(gridLayout);
    
    data = new GridData(GridData.FILL_BOTH);
    panel.setLayoutData(data);
    
    SashForm form = new SashForm(panel,SWT.HORIZONTAL);
    data = new GridData(GridData.FILL_BOTH);
    form.setLayoutData(data);

    Composite cLeftSide = new Composite(form, SWT.NONE);    
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    cLeftSide.setLayout(layout);
    
    
    channelTable = new Table(cLeftSide, SWT.BORDER | SWT.SINGLE);
    data = new GridData(GridData.FILL_BOTH);
    data.horizontalSpan = 2;
    channelTable.setLayoutData(data);
    channelTable.setHeaderVisible(true);
    name = new TableColumn(channelTable,SWT.LEFT);
    name.setText("Running Torrents");
    
    Composite cRightSide = new Composite(form, SWT.NONE);
    gridLayout = new GridLayout();
    gridLayout.marginHeight = 3;
    gridLayout.marginWidth = 0;
    cRightSide.setLayout(gridLayout);

    // Header
    Composite cHeader = new Composite(cRightSide, SWT.BORDER);
    gridLayout = new GridLayout();
    gridLayout.marginHeight = 3;
    gridLayout.marginWidth = 0;
    cHeader.setLayout(gridLayout);
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    cHeader.setLayoutData(data);

    Display d = cRightSide.getDisplay();
    cHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
    cHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

    lHeader = new Label(cHeader, SWT.NULL);
    lHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
    lHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
    FontData[] fontData = lHeader.getFont().getFontData();
    fontData[0].setStyle(SWT.BOLD);
    int fontHeight = (int)(fontData[0].getHeight() * 1.2);
    fontData[0].setHeight(fontHeight);
    headerFont = new Font(d, fontData);
    lHeader.setFont(headerFont);
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    lHeader.setLayoutData(data);
    
    // Channel Section
    c3DViewSection = new Composite(cRightSide, SWT.NULL);
    c3DViewSection.setLayout(new GridLayout());
    data = new GridData(GridData.FILL_BOTH);
    c3DViewSection.setLayoutData(data);

    peer3DView = new Peers3DGraphicView(plugin_interface, c3DViewSection, params);
        
    form.setWeights(new int[] {20,80});

    channelTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Table table = (Table)e.getSource();
        //Check that at least an item is selected
        //OSX lets you select nothing in the tree for example when a child is selected
        //and you close its parent.
        if(table.getSelection().length > 0) {
          table.getSelection()[0].setFont(null);
          showSection(table.getSelection()[0]);
        }          
      }
    });
    
    
  }
  


  public Composite getComposite() {
    return panel;
  }

  public String getPluginViewName() {    
    return "3D View";
  }

  public String getFullTitle() {
    return "3D View";
  }
  
  private class Swarm3DDownloadListener implements DownloadListener
  {
    public void stateChanged(Download download, int old_state, int new_state) {
    	if(new_state == Download.ST_DOWNLOADING || new_state == Download.ST_SEEDING ) {
    		if(!activeDownloads.containsKey(download)) {
	    		activeDownloads.put(download, download);
	    	    displayDownload(download);
    	    }
    	} else {
    		if(activeDownloads.containsKey(download)) {
    			activeDownloads.remove(download);
    	        disposeDownload(download);   
    		}
    	}
    }

    public void positionChanged(Download download, 
                                int oldPosition, int newPosition) {}
  }
  
  private class Swarm3DDMListener implements DownloadManagerListener
  {
    private DownloadListener        download_listener;

    
    public Swarm3DDMListener() {
      download_listener = new Swarm3DDownloadListener();
      downloads = download_manager.getDownloads();
      for(int i=0;i<downloads.length;i++) {
    	  Download download = downloads[i];
    	  if(download.getState() == Download.ST_DOWNLOADING || download.getState() == Download.ST_SEEDING) {
    		  activeDownloads.put(download, download);
    		  displayDownload(download);
    	  }
      }
    }

    public void downloadAdded(final Download download) {
    	download.addListener( download_listener );
    }

    public void downloadRemoved(final Download download) {
    	download.removeListener( download_listener ); 
      }
  }

  private void showSection(TableItem section) {
    
    Download download = (Download)section.getData("Download");

    if (download != null) {
      peer3DView.setDownload(download);
      updateHeader(section);
    }
  }
  
  private void updateHeader(TableItem section) {
    Download download = (Download)section.getData("Download");
    lHeader.setText("  3D View : " + download.getTorrent().getName());
  }
  
  public void delete() {   
//    TreeItem[] items = channelTree.getItems();
	  
	  peer3DView.delete();

    if(headerFont != null && ! headerFont.isDisposed()) {
      headerFont.dispose();
    }
  }
  
  public void refresh() {
  }
  
	/**
	 * @param download
	 */
	private void disposeDownload(final Download download) {
		if(display != null && ! display.isDisposed() && ! channelTable.isDisposed()) {
		    display.asyncExec(new Runnable() {
		      public void run() {
		    	items = channelTable.getItems();
		        for(int i = 0 ; i < items.length ; i++) {
		          if(items[i].getData("Download") == download) {
		            items[i].dispose();
		          }
		        }	
		      }
		    });    
		  }
	}

	/**
	 * @param download
	 */
	private void displayDownload(final Download download) {
		if(display != null && ! display.isDisposed() && ! channelTable.isDisposed()) {
		      display.asyncExec(new Runnable() {
		        public void run() {
		          item = new TableItem(channelTable,SWT.NONE);
		          item.setData("Download",download);
		          item.setText(download.getTorrent().getName());
		          fillTable();
		        }
		      });    
		    }
	}
	
	private void fillTable() {
	    // Turn off drawing to avoid flicker
	    channelTable.setRedraw(false);
	    Download[] dls = new Download[activeDownloads.size()];
	    Set set = activeDownloads.keySet();
	    Iterator iterator = set.iterator();
	    int k = 0;
	    while( iterator.hasNext() ) {
	    	dls[k] = (Download)iterator.next();
	    	k++;
	    }
	    // We remove all the table entries, sort our
	    // rows, then add the entries
	    channelTable.removeAll();
		Arrays.sort(
				dls,
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{
						Download	d1 = (Download)o1;
						Download	d2 = (Download)o2;

						int	res = comparator.compare( "" + d1.getTorrent().getName(),  "" + d2.getTorrent().getName() );
						
						return(  res );
					}
				});
	    for (int i = 0; i<dls.length; i++) {
	    	item = new TableItem(channelTable,SWT.NONE);
	        item.setData("Download",dls[i]);
	        item.setText(dls[i].getTorrent().getName());          
	    }

	    // Turn drawing back on
	    channelTable.setRedraw(true);
	    name.pack();
	  }
 
}
  
