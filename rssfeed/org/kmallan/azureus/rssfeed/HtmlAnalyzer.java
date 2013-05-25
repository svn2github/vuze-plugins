/*
 * RSSFeed - Azureus2 Plugin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package org.kmallan.azureus.rssfeed;

import javax.swing.text.html.*;
import javax.swing.text.MutableAttributeSet;

import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;

import java.util.*;
import java.net.*;
import java.io.IOException;

/**
 * Used to parse the HTML on a URL recieved from the rss feed.
 * 
 * First, it looks for a .torrent link.  If one is not found, it checks
 * each URL to see if it's HEAD is of torrent type
 * 
 * Created by IntelliJ IDEA.
 * User: Johan Frank
 * Date: Jan 7, 2005
 * Time: 12:48:57 AM
 */
public class HtmlAnalyzer extends HTMLEditorKit.ParserCallback implements Runnable {

  private static final int HREF_CHECK_TIMEOUT = 60000;

  private List hrefs = new ArrayList();
  private String torrentUrl = null, baseUrl = null;
  private StringBuffer text = new StringBuffer();
  private ListBean listBean;
  
  private String lastURL = null;
  private boolean lastWasURL = false;
	private String lastURLtext;

  public HtmlAnalyzer() {
    this("", null);
  }

  public HtmlAnalyzer(String baseUrl, ListBean listBean) {
    this.baseUrl = baseUrl;
    this.listBean = listBean;
  }

  public void handleStartTag(HTML.Tag tag, MutableAttributeSet mas, int pos) {
    if(tag == HTML.Tag.A) {
      lastURL = (String)mas.getAttribute(HTML.Attribute.HREF);
      if(lastURL != null) {
        if(lastURL.indexOf("://") < 0) {
          try {
            lastURL = resolveRelativeURL(baseUrl, lastURL);
          } catch(MalformedURLException e) {}
        }
        if(lastURL.toLowerCase().endsWith(".torrent")) {
        	torrentUrl = lastURL;
        } else {
        	lastWasURL = true;
        }
      }
    } else {
    	lastWasURL = false;
    }
  }

  public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet mutableAttributeSet, int i) {
    if(tag == HTML.Tag.BR) text.append("\n");
  }

  public void handleEndTag(HTML.Tag tag, int i) {
    if (tag == HTML.Tag.BODY || tag == HTML.Tag.HTML || tag == HTML.Tag.HEAD)
			return;
		else if (tag == HTML.Tag.A && lastWasURL && torrentUrl == null) {
			// put links with .torrent name in them at the top of parsing list
			if (lastURLtext != null && lastURLtext.contains(".torrent")) {
				hrefs.add(0, lastURL);
			}
			hrefs.add(lastURL);
		} else {
			text.append("\n");
		}
  }

  public void handleText(char[] chars, int i) {
    String s = new String(chars);
    if (lastWasURL) {
    	lastURLtext = s;
    }
    text.append(s.replace('<', ' ').replace('>', ' ').trim()); // remove remnants of broken tags :-P
  }

  public synchronized String getTorrentUrl() {
    if(torrentUrl == null &! hrefs.isEmpty()) {
    	Plugin.debugOut("No url ending in .torrent, checking " + hrefs.size()
					+ " URL(s) to see if any are application/x-bittorrent");
			Plugin.debugOut("After " + (HREF_CHECK_TIMEOUT / 1000)
					+ " seconds, check will abort.");

      Thread hrefChecker = new Thread(this, "HrefContentCheckerThread");
      hrefChecker.start();
      try {
        wait(HREF_CHECK_TIMEOUT);
      } catch(InterruptedException e) {}
      hrefChecker.interrupt();
    }
    Plugin.debugOut("returning torrentUrl: " + torrentUrl);
    return torrentUrl;
  }

  public String getPlainText() {
    return text.toString();
  }

  protected static String resolveRelativeURL(String url, String href) throws MalformedURLException {
    URL u = new URL(url);
    String newUrl = u.getProtocol() + "://" + u.getHost();
    if(u.getPort() > 0) newUrl += ":" + u.getPort();
    if(!href.startsWith("/")) { // path relative to current
      String path = u.getPath(); // e.g /dir/file.php
      if(path.indexOf("/") > -1) path = path.substring(0, path.lastIndexOf("/") + 1); // strip file part
      newUrl += path; // append /dir
      if(!newUrl.endsWith("/")) newUrl += "/";
    }
    return newUrl + href;
  }

  /**
   * Check all the URLs that don't end in 'torrent' to see if they are actually
   * torrents
   */
  public void run() {
    synchronized(this) {
      String href = null;
      int count = 1;
      for(Iterator iter = hrefs.iterator(); iter.hasNext(); ) {
        href = (String)iter.next();
        if(isHrefTorrent(href, Plugin.getBooleanParameter( "ForceNoProxy"))) {
          torrentUrl = href;
          Plugin.debugOut("found torrent: " + href);
          break;
        }
        updateView(count++);
      }
      notifyAll();
    }
  }

  private void updateView(int count) {
    if(listBean == null) return;
    listBean.setState(Downloader.DOWNLOADER_CHECKING);
    listBean.setAmount(count);
    listBean.setPercent(hrefs.size());
    Plugin.updateView(listBean);
  }

  /**
   * Check one URL to see if it's a torrent by grabbing the HEAD and seeing
   * if the connection type is of torrent type.
   * 
   * @param href
   * @return
   */
  private boolean isHrefTorrent(String href, boolean forceNoProxy) {
    try {
      if ( forceNoProxy ){
    		
    	AEProxySelectorFactory.getSelector().startNoProxy();
      }
      
      URLConnection conn = new URL(href).openConnection();
      
      if(conn instanceof HttpURLConnection) {
        ((HttpURLConnection)conn).setRequestMethod("HEAD");
        String cookie = listBean.getFeed().getCookie();
        if(cookie != null && cookie.length() > 0) conn.setRequestProperty("Cookie", cookie);
        conn.connect();
        String ct = conn.getContentType();
        ((HttpURLConnection)conn).disconnect();
        if(ct != null) {
          Plugin.debugOut("href: " + href + " -> " + ct);
          return ct.toLowerCase().startsWith("application/x-bittorrent");
        }
      }
    } catch(IOException e) {
      e.printStackTrace();
    }finally{
    	
      if ( forceNoProxy ){
    		
    	AEProxySelectorFactory.getSelector().endNoProxy();
      }
    }
    return false;
  }

}
