/*
 * Created on Sep 21, 2004
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

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class ContentTypeChecker {
  
  private  Map cache;

  private static final String TORRRENT_TYPE = "application/x-bittorrent";
  
  private static final String knownTypes[] = {
      ".net",   //Domains
      ".com",
      ".org",
      ".html",  //static HMTL Pages
      ".htm",      
      ".php",   //Dynamic web pages with no parameters
      ".css",   //Style sheets
      ".jpg",   //Images
      ".JPG",
      ".jpeg",
      ".gif",
      ".png",
      ".ico",
      ".js",    //javascript
      ".swf",   //Flash
      "/"       //Usually files are not after an ending /
  };
  
  private static final String excludeDomains[] = {      
      ".bluestreak.com",
      ".doubleclick.net",
      ".cjt1.net"
  };
  
  public ContentTypeChecker() {
    cache = new HashMap();
  }
  
  public boolean isTorrent(String url) {            
    // 1. Check for "known" content-types from name :
    for(int i = 0 ; i < knownTypes.length ; i++) {
      if(url.endsWith(knownTypes[i])) return false;
    }
    
    // 2. If file name ends with .torrent, then obviously, we should have a .torrent
    if(url.endsWith(".torrent")) return true;
    
    // 3. Constructs the URL
    URL _url = null;
    try {
      _url = new URL(url);
    } catch(Exception e) {
      //Do nothing
    }
    
    // 4. If not a valid URL, return false
    if(_url == null) return false;
    
    // 5. Filter some domains (ads, etc ...)
    String host = _url.getHost();
    if(host != null) {
      for(int i = 0 ; i < excludeDomains.length ; i++) {
        if(host.endsWith(excludeDomains[i]))
          return false;
      }
    }
    
    //OK, so far we do really have to check for the real content-type :
    
    // 6. check the cache
    Boolean bool = (Boolean) cache.get(url);
    if(bool != null) {
      return bool.booleanValue();
    }
    
    boolean result = false;
    
    //7. Not in cache, create a connection, and get content-type
    //DEBUG:
    System.out.println("Checking Content-Type for : " + url);
    try {
      URLConnection con = _url.openConnection();
      String contentType = con.getContentType();     
      if(contentType != null) {
        //DEBUG
        System.out.println("Found Content-Type : " + contentType);
        if(contentType.equals(ContentTypeChecker.TORRRENT_TYPE)) {
          result = true;
        }
      }      
    } catch(Exception e) {
      result = false;
    }
    
    //Add to cache for further calls
    cache.put(url,new Boolean(result));
        
    return result;
  }
}
