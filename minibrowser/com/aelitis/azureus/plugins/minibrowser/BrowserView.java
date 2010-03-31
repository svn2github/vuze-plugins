/*
 * Created on Sep 20, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.plugins.minibrowser;


import java.io.InputStream;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.plugins.ui.SWT.SWTManager;

/**
 * 
 */
public class BrowserView extends PluginView implements TitleListener,ProgressListener,LocationListener,OpenWindowListener {
  
  BrowserConfig config;
  SWTManager swtManager;
  
  ContentTypeChecker checker;
  
  String title;
  
  Display display;
  
  Composite panel;
  Browser browser;
  
  Text address;
  ProgressBar progress; 
  
  Table favorites; 
  
  Image imgStop,imgForward,imgBack,imgReload,imgAdd;
  
  /**
   * 
   */
  public BrowserView(BrowserConfig config,SWTManager swtManager) {
    this.config = config;
    this.swtManager = swtManager;
    this.checker = new ContentTypeChecker();
  }
  
  public String getPluginViewName() {
    return "Mini-Browser";
  }

  public String getFullTitle() {
    if(title != null)
      return title;
    return "Mini-Browser";
  }
  
  public void initialize(Composite composite) {
    
    display = composite.getDisplay();
    
    InputStream is;
    ClassLoader loader = BrowserView.class.getClassLoader();
    
    String resPath = "com/aelitis/azureus/plugins/minibrowser/resources/";
    
    is = loader.getResourceAsStream(resPath + "stop.png");    
    imgStop = new Image(display,is);
        
    is = loader.getResourceAsStream(resPath + "back.png");    
    imgBack = new Image(display,is);
   
    is = loader.getResourceAsStream(resPath + "forward.png");    
    imgForward = new Image(display,is);
      
    is = loader.getResourceAsStream(resPath + "reload.png");    
    imgReload = new Image(display,is);
   
    is = loader.getResourceAsStream(resPath + "add_to_bookmark.png");    
    imgAdd = new Image(display,is);
    
    panel = new Composite(composite,SWT.NULL);
    
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    panel.setLayout(gridLayout);
    
    ToolBar toolbar = new ToolBar(panel, SWT.HORIZONTAL);
    
    ToolItem itemBack = new ToolItem(toolbar, SWT.PUSH);
    itemBack.setText("Back");
    itemBack.setData("action","back");
    itemBack.setImage(imgBack);
    
    ToolItem itemForward = new ToolItem(toolbar, SWT.PUSH);
    itemForward.setText("Forward");
    itemForward.setData("action","forward");
    itemForward.setImage(imgForward);
    
    ToolItem itemStop = new ToolItem(toolbar, SWT.PUSH);
    itemStop.setText("Stop");
    itemStop.setData("action","stop");
    itemStop.setImage(imgStop);
    
    ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
    itemRefresh.setText("Refresh");
    itemRefresh.setData("action","refresh");
    itemRefresh.setImage(imgReload);
    
    ToolItem itemAdd = new ToolItem(toolbar, SWT.PUSH);
    itemAdd.setText("Add");
    itemAdd.setData("action","add");
    itemAdd.setImage(imgAdd);
    
    GridData data = new GridData();
    data.horizontalSpan = 1;
    toolbar.setLayoutData(data);

    //Label labelAddress = new Label(panel, SWT.NONE);
    //labelAddress.setText("Address");
    
    address = new Text(panel, SWT.BORDER);
    data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = 2;
    data.grabExcessHorizontalSpace = true;
    address.setLayoutData(data);

    SashForm sash = new SashForm(panel,SWT.HORIZONTAL);
    data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.verticalAlignment = GridData.FILL;
    data.horizontalSpan = 3;
    data.grabExcessHorizontalSpace = true;
    data.grabExcessVerticalSpace = true;
    sash.setLayoutData(data);
    sash.SASH_WIDTH = 5;
    
    favorites = new Table(sash,SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    final TableColumn tc = new TableColumn(favorites,SWT.LEFT);
    
    favorites.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
          Rectangle area = favorites.getClientArea();
          int width = area.width - 2*favorites.getBorderWidth();
  
          Point vBarSize = favorites.getVerticalBar().getSize();
          width -= vBarSize.x + 5;
          
          tc.setWidth(width);
        }
      });
    
    java.util.List bookmarks = config.getBookmarks();
    Iterator iter = bookmarks.iterator();
    while(iter.hasNext()) {
      Bookmark bookmark = (Bookmark) iter.next();
      TableItem ti = new TableItem(favorites,SWT.NULL);
      ti.setText(bookmark.getName());
      ti.setData(bookmark);      
    }
    
    favorites.addListener(SWT.DefaultSelection,new Listener() {
      public void handleEvent(Event e) {
        openSelected();
      }    
    });
    
    Menu menuFav = new Menu(favorites);
    MenuItem itemOpen = new MenuItem(menuFav,SWT.NULL);
    itemOpen.setText("Open");
    menuFav.setDefaultItem(itemOpen);
    itemOpen.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        openSelected();
      }
    });
    
    
    
    MenuItem itemEdit = new MenuItem(menuFav,SWT.NULL);
    itemEdit.setText("Edit...");
    itemEdit.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        int selected = favorites.getSelectionIndex();
        final TableItem item = favorites.getItem(selected);
        final Bookmark bookmark = (Bookmark) item.getData();
        new BookmarkEditor(display,new BookmarkEditorListener() {
          
          public void canceled() {            
          }

          public void changed(String newName, String newURL) {
            bookmark.setName(newName);
            bookmark.setUrl(newURL);
            config.saveBookmarks();
            item.setText(newName);
          }
        },bookmark);        
      }
    });
    
    
    new MenuItem(menuFav,SWT.SEPARATOR);
    
    MenuItem itemRemove = new MenuItem(menuFav,SWT.NULL);
    itemRemove.setText("Remove");
    itemRemove.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        int selected = favorites.getSelectionIndex();
        Bookmark bookmark = (Bookmark) favorites.getItem(selected).getData();
        config.removeBookmark(bookmark);
        favorites.remove(selected);
      }
    });
    
    favorites.setMenu(menuFav);
    
    browser = new Browser(sash, SWT.NONE);
    browser.setUrl(config.getHomePage());
    address.setText(config.getHomePage());
    
    sash.setWeights(new int[] {1,4});

    final Label status = new Label(panel, SWT.NONE);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    status.setLayoutData(data);

    progress = new ProgressBar(panel, SWT.NONE);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    progress.setLayoutData(data);

    /* event handling */
    Listener listener = new Listener() {
      public void handleEvent(Event event) {
        ToolItem item = (ToolItem)event.widget;
        String string = (String) item.getData("action");
        if (string.equals("back")) browser.back(); 
        else if (string.equals("forward")) browser.forward();
        else if (string.equals("stop")) browser.stop();
        else if (string.equals("refresh")) browser.refresh();
        else if (string.equals("add")) {
          String url = browser.getUrl();
          new BookmarkEditor(display,new BookmarkEditorListener() {
            public void changed(String name,String address) {
              TableItem ti =  new TableItem(favorites,SWT.NULL);
              ti.setText(name);
              Bookmark bookmark = new Bookmark(name,address);
              ti.setData(bookmark);
              config.addBookmark(bookmark);
            }
            
            public void canceled() {
              
            }
          },new Bookmark(title,url));          
        }
       }
    };

    itemBack.addListener(SWT.Selection, listener);
    itemForward.addListener(SWT.Selection, listener);
    itemStop.addListener(SWT.Selection, listener);
    itemRefresh.addListener(SWT.Selection, listener);
    itemAdd.addListener(SWT.Selection, listener);
    address.addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event e) {
        browser.setUrl(address.getText());
      }
    });
    
    
    
    browser.addProgressListener(this);
    
    browser.addTitleListener(this);
    
    browser.addLocationListener(this);
    
    browser.addOpenWindowListener(this);
    
  }
  
  private void openSelected() {
    int selected = favorites.getSelectionIndex();
    String url = ((Bookmark) favorites.getItem(selected).getData()).getUrl();
    browser.setUrl(url);
    address.setText(url);
  }

  
  public Composite getComposite() {
   return panel;
  }
  
 
  public void changed(final ProgressEvent evt) {
    display.asyncExec(new Runnable() {
      public void run() {
        if(evt.total > 0) {
          progress.setSelection((100 *evt.current) / evt.total);
        }              
      }
    });
  }
  

  public void changed(TitleEvent evt) {
   title = evt.title;
  }
  

  public void completed(ProgressEvent evt) {    
    display.asyncExec(new Runnable() {
      public void run() {
        progress.setSelection(0);        
      }
    });
  }
  

  public void changed(final LocationEvent evt) {
        //System.out.println("    Opened : " + evt.location);
        if (evt.top) address.setText(evt.location);
  }
  
  public void changing(final LocationEvent evt) {    
     //System.out.println("Opening : " + evt.location + ", " + evt.doit + ", " + evt.top + ", " + ((Browser)evt.widget).getUrl());   
     if(checker.isTorrent(evt.location)) {     	
       evt.doit =  false;
       System.out.println("Found a torrent at : " + evt.location );       
     }
  }

  
  static void initialize(final Display display, Browser browser) {
	browser.addOpenWindowListener(new OpenWindowListener() {
		public void open(WindowEvent event) {
			Shell shell = new Shell(display);
			shell.setText("New Window");
			shell.setLayout(new FillLayout());
			Browser browser = new Browser(shell, SWT.NONE);
			initialize(display, browser);
			event.browser = browser;
		}
	});
	
	browser.addCloseWindowListener(new CloseWindowListener() {
		public void close(WindowEvent event) {
			Browser browser = (Browser)event.widget;
			Shell shell = browser.getShell();
			shell.close();
		}
	});
}
  
  public void open(WindowEvent evt) {
  	System.out.println("Open Window Event, data : " + evt.data);  
  	final Shell shell = new Shell(display);
  	shell.setText("New Window");
  	shell.setLayout(new FillLayout());
  	Browser browser = new Browser(shell,SWT.NONE);
  	browser.addLocationListener(this);
  	browser.addVisibilityWindowListener(new VisibilityWindowListener() {
		public void hide(WindowEvent event) {		
			System.out.println("Sub-Browser : hide");
		}
		public void show(WindowEvent event) {
			System.out.println("Sub-Browser : hide");
		}
	});
  	browser.addOpenWindowListener(this);
  	evt.browser = browser;
  	
  }
}
