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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.gudy.azureus2.plugins.PluginConfig;

/**
 * 
 */
public class BrowserConfig {

  PluginConfig config;
  
  List bookmarks;
  
  public static String DEFAULT_HOME_PAGE = "http://azureus.sf.net/";
  public static String DEFAULT_BOOKMARKS = "Azureus Web Site\thttp://azureus.sf.net/\nAzureus Wiki\thttp://azureus.sf.net/wiki/\n";
  /**
   * 
   */
  public BrowserConfig(PluginConfig config) {
   this.config = config;
   bookmarks = new ArrayList();
   String strBookmarks = config.getPluginStringParameter("bookmarks",DEFAULT_BOOKMARKS);
   if(strBookmarks != null) {
     StringTokenizer st = new StringTokenizer(strBookmarks,"\n");
     while(st.hasMoreTokens()) {
       try {
         String strBookmark = st.nextToken();
         StringTokenizer st2 = new StringTokenizer(strBookmark,"\t");
         String name = st2.nextToken(); 
         String url = st2.nextToken();
         bookmarks.add(new Bookmark(name,url));
       } catch(Exception e) {
         e.printStackTrace();
       }
     }
   }
  }
  
  public String getHomePage() {
    return config.getPluginStringParameter("homepage",DEFAULT_HOME_PAGE);
  }
  
  public List getBookmarks() {
   return bookmarks; 
  }
  
  public void addBookmark(Bookmark bookmark) {
    bookmarks.add(bookmark);
    saveBookmarks();
  }
  
  public void removeBookmark(Bookmark bookmark) {
    bookmarks.remove(bookmark);
    saveBookmarks();
  }  
  
  public void saveBookmarks() {
    StringBuffer str = new StringBuffer();
    Iterator iter = bookmarks.iterator();
    while(iter.hasNext()) {
      Bookmark bookmark = (Bookmark) iter.next();
      str.append(bookmark.getName());
      str.append("\t");
      str.append(bookmark.getUrl());
      str.append("\n");
    }    
    
    config.setPluginParameter("bookmarks",str.toString());
    try {
      config.save();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
}
