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

import org.eclipse.swt.graphics.Color;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import javax.swing.text.html.parser.ParserDelegator;

import java.io.*;
import java.util.*;
import java.net.*;

public class Scheduler extends TimerTask {

  private View view = null;

  public void setView(View view) {
    this.view = view;
  }

  private int getDelay() {
    int delay;
    delay = Plugin.getIntParameter("Delay");
    if(delay < Plugin.MIN_REFRESH) {
      delay = Plugin.MIN_REFRESH;
      Plugin.setParameter("Delay", delay);
    }
    return delay;
  }

  private boolean isEnabled() {
    return Plugin.getBooleanParameter("Enabled");
  }

  public void run() {
		for (int iLoop = 0; iLoop < view.rssfeedConfig.getUrlCount(); iLoop++) {
			final UrlBean urlBean = view.rssfeedConfig.getUrl(iLoop);
			final ListGroup listGroup = urlBean.getGroup(view.treeViewManager,
					getDelay());

			final int delay = listGroup.getDelay();
			final int elapsed = urlBean.isHitting() ? 0 : listGroup.getElapsed();

			if (elapsed >= delay && (isEnabled() && urlBean.isEnabled())
					|| urlBean.getRefreshNow()) {
				urlBean.resetGroup(getDelay());

				Thread t = new Thread("Fetcher-" + urlBean.getName()) {
					public void run() {
						Plugin.debugOut("hitting " + urlBean.getName() + " - "
								+ urlBean.getLocation());
						urlBean.setHitting(true);
						runFeed(urlBean);
						addBacklogElements(urlBean);
						urlBean.setHitting(false);
					}
				};

				t.start();
			}

			if (view.isOpen() && view.display != null && !view.display.isDisposed())
				view.display.asyncExec(new Runnable() {
					public void run() {
						if (view.listTable == null || view.listTable.isDisposed())
							return;

						ListTreeItem listGroup = view.treeViewManager.getItem(urlBean);
						if (urlBean.isHitting()) {
							String s = urlBean.getStatus() + " ";
							if (urlBean.getError().length() > 0
									&& urlBean.getStatus().equals("Error")) {
								s += "- " + urlBean.getError();
							} else if (urlBean.getStatus().equals("Downloading")) {
								if (urlBean.getPercent() > 0) {
									s += Integer.toString(urlBean.getPercent()) + "%";
								} else if (urlBean.getAmount() > 0) {
									s += Double.toString(Math.floor(new Integer(
											urlBean.getAmount()).doubleValue()
											/ (double) 1024 * (double) 100)
											/ (double) 100)
											+ "KB";
								}
							}
							listGroup.setText(1, s);
						} else if (urlBean.isEnabled()) {
							if (isEnabled()) {
								int time = delay - elapsed;
								int minutes = new Double(
										Math.floor(new Integer(time).doubleValue() / (double) 60)).intValue();
								int seconds = time - (minutes * 60);
								String newTime = Integer.toString(minutes)
										+ ":"
										+ (seconds < 10 ? "0" + Integer.toString(seconds)
												: Integer.toString(seconds));
								listGroup.setText(1, newTime
										+ " until reload"
										+ (!urlBean.getError().equalsIgnoreCase("") ? " - "
												+ urlBean.getError() : ""));
							} else {
								listGroup.setText(1, "Automatic reload disabled"
										+ (!urlBean.getError().equalsIgnoreCase("") ? " - "
												+ urlBean.getError() : ""));
							}
						} else {
							listGroup.setText(1, "Feed disabled"
									+ (!urlBean.getError().equalsIgnoreCase("") ? " - "
											+ urlBean.getError() : ""));
						}
						if (!urlBean.getError().equalsIgnoreCase(""))
							listGroup.setForeground(new Color(view.display, 255, 0, 0));
						else
							listGroup.resetForeground();
					}
				});

		}
	}

  public synchronized void runFeed(final UrlBean urlBean) {
    String url = urlBean.getLocation();
    String title, link, description;

    ListGroup listBeans = urlBean.getGroup();

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setIgnoringComments(true);
    docFactory.setIgnoringElementContentWhitespace(true);
    DocumentBuilder docBuild;
    Document feed;

    File xmlTmp = null;
    try {
      docBuild = docFactory.newDocumentBuilder();
      Downloader downloader = new Downloader();
      downloader.addListener(new DownloaderListener() {
        public boolean completed = false, error = false;

        public void downloaderUpdate(int state, int percent, int amount, String err) {
          if(completed || error) return;
          String status = new String("Pending");
          switch(state) {
            case Downloader.DOWNLOADER_NON_INIT:
              status = "Pending";
              break;
            case Downloader.DOWNLOADER_INIT:
              status = "Connecting";
              break;
            case Downloader.DOWNLOADER_START:
              status = "Download Starting";
              break;
            case Downloader.DOWNLOADER_DOWNLOADING:
              status = "Downloading";
              break;
            case Downloader.DOWNLOADER_FINISHED:
              status = "Download Finished";
              completed = true;
              break;
            case Downloader.DOWNLOADER_NOTMODIFIED:
              status = "Not modified";
              completed = true;
              break;
            case Downloader.DOWNLOADER_ERROR:
              status = "Error";
              error = true;
              break;
          }
          urlBean.setStatus(status);
          if(percent > 0) urlBean.setPercent(percent);
          if(amount > 0) urlBean.setAmount(amount);
          if(!err.equalsIgnoreCase("")) urlBean.setError(err);

          if(view.isOpen() && view.display != null && !view.display.isDisposed())
            view.display.asyncExec(new Runnable() {
              public void run() {
                if(view.listTable == null || view.listTable.isDisposed()) return;
                ListTreeItem listGroup = view.treeViewManager.getItem(urlBean);
                listGroup.setText(1, urlBean.getStatus() + " "
                    + (!urlBean.getError().equalsIgnoreCase("") && urlBean.getStatus() == "Error"?"- "
                    + urlBean.getError():(urlBean.getStatus() == "Downloading"?(urlBean.getPercent() > 0?Integer.toString(urlBean.getPercent())
                    + "%":(urlBean.getAmount() > 0?Double.toString(Math.floor(new Integer(urlBean.getAmount()).doubleValue() / (double) 1024 * (double) 100) / (double) 100) + "KB":"")):"")));
                if(!urlBean.getError().equalsIgnoreCase("")) listGroup.setForeground(new Color(view.display, 255, 0, 0));
                else listGroup.resetForeground();
              }
            });
        }
      });
      downloader.init(url, "text/xml, text/html, text/plain, application/x-httpd-php", null,
          (urlBean.getUseCookie()?urlBean.getCookie():null), urlBean.getLastModifed(), urlBean.getLastEtag());

      if(downloader.getState() == Downloader.DOWNLOADER_ERROR) return;

      if(downloader.getState() == Downloader.DOWNLOADER_NOTMODIFIED) {
        // no change, add the old items again
        for(Iterator iter = listBeans.getPreviousItems().iterator(); iter.hasNext(); ) {
          addTableElement(urlBean, listBeans, (ListBean)iter.next());
        }
        addBacklogElements(urlBean);
        downloader.notModified();
        // use the last seen TTL value if available
        if(urlBean.getObeyTTL() && listBeans.getPreviousDelay() > 0) listBeans.setDelay(listBeans.getPreviousDelay());
        return;
      }

      Plugin.debugOut(urlBean.getName() + " Last-Modified: " + downloader.lastModified + " ETag: " + downloader.etag);

      urlBean.setLastModifed(downloader.lastModified);
      urlBean.setLastEtag(downloader.etag);

      xmlTmp = new File(Plugin.getPluginDirectoryName(), "tmp-" + urlBean.getID() + ".xml");
      xmlTmp.createNewFile();
      FileOutputStream fileout = new FileOutputStream(xmlTmp, false);

      byte[] buf = new byte[2048];
      int read = 0;
      do {
        if(downloader.getState() == Downloader.DOWNLOADER_CANCELED) break;
        read = downloader.read(buf);
        if(read > 0) {
          System.err.print(".");
          fileout.write(buf, 0, read);
        } else if(read == 0) {
          System.err.print("?");
          try {
            long numMillisecondsToSleep = 100;
            Thread.sleep(numMillisecondsToSleep);
          } catch(InterruptedException e) {
          }
        }
      } while(read >= 0);

      fileout.flush();
      fileout.close();
      
      docBuild.setEntityResolver(
			new EntityResolver()
			{
				public InputSource 
				resolveEntity(
					String publicId, String systemId )
				{
					// System.out.println( publicId + ", " + systemId );
					
					// handle bad DTD external refs
					
					if ( Plugin.getProxyOption() == Plugin.PROXY_TRY_PLUGIN ){
				
						return new InputSource(	new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
					}
			
					try{
						URL url  = new URL( systemId );
						
						String host = url.getHost();
						
						InetAddress.getByName( host );
						
							// try connecting too as connection-refused will also bork XML parsing
						
						InputStream is = null;
						
						try{
							URLConnection con = url.openConnection();
							
							con.setConnectTimeout( 15*1000 );
							con.setReadTimeout( 15*1000 );
							
							is = con.getInputStream();
								
							byte[]	buffer = new byte[32];
							
							int	pos = 0;
							
							while( pos < buffer.length ){
								
								int len = is.read( buffer, pos, buffer.length - pos );
								
								if ( len <= 0 ){
									
									break;
								}
								
								pos += len;
							}
															
							String str = new String( buffer, "UTF-8" ).trim().toLowerCase( Locale.US );
							
							if ( !str.contains( "<?xml" )){
								
									// not straightforward to check for naked DTDs, could be lots of <!-- commentry preamble which of course can occur
									// in HTML too
								
								buffer = new byte[32000];
								
								pos = 0;
								
								while( pos < buffer.length ){
									
									int len = is.read( buffer, pos, buffer.length - pos );
									
									if ( len <= 0 ){
										
										break;
									}
									
									pos += len;
								}
								
								str += new String( buffer, "UTF-8" ).trim().toLowerCase( Locale.US );
								
								if ( str.contains( "<html") && str.contains( "<head" )){
								
									throw( new Exception( "Bad DTD" ));
								}
							}
						}catch( Throwable e ){
							
							return new InputSource(	new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
							
						}finally{
							
							if ( is != null ){
								
								try{
									is.close();
									
								}catch( Throwable e){
									
								}
							}
						}
						return( null );
						
					}catch( UnknownHostException e ){
						
						return new InputSource(	new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
						
					}catch( Throwable e ){
						
						return( null );
					}
				}
			});
      
      feed = docBuild.parse(xmlTmp);
      
      xmlTmp.delete(); 
      downloader.done();

      if(downloader.getState() == Downloader.DOWNLOADER_ERROR) return;
    } catch(ParserConfigurationException e) {
      if(xmlTmp != null) xmlTmp.delete();
      urlBean.setError("Malformed RSS XML: " + e.getMessage());
      return;
    } catch(SAXException e) {
      if(xmlTmp != null) xmlTmp.delete();
      urlBean.setError("Malformed RSS XML: " + e.getMessage());
      return;
    } catch(IOException e) {
      if(xmlTmp != null) xmlTmp.delete();
      urlBean.setError("IO Exception: " + e.getMessage());
      return;
    }

    if(urlBean.getObeyTTL()) {
      NodeList feedTTL = feed.getElementsByTagName("ttl");
      if(feedTTL.getLength() == 1) {
        int newDelay = Integer.parseInt(getText(feedTTL.item(0))) * 60;
        if(newDelay > 0) urlBean.getGroup().setDelay(newDelay, true);
      }
    }

    // Parse the channel's "item"s
    NodeList feedItems = feed.getElementsByTagName("item");
    int feedItemLen = feedItems.getLength();
    for(int iLoop = 0; iLoop < feedItemLen; iLoop++) {
      Node item = feedItems.item(iLoop);
      NodeList params = item.getChildNodes();
      int paramsLen = params.getLength();

      title = link = description = "";

      for(int i = 0; i < paramsLen; i++) {
        Node param = params.item(i);
        if(param.getNodeType() == Node.ELEMENT_NODE) {
          if(param.getNodeName().equalsIgnoreCase("title")) {
            title = getText(param);
          } else if(param.getNodeName().equalsIgnoreCase("enclosure") && param.hasAttributes()) {
            if((((param.getAttributes()).getNamedItem("type")).getNodeValue()).equalsIgnoreCase("application/x-bittorrent")) {
              link = ((param.getAttributes()).getNamedItem("url")).getNodeValue();
            }
          } else if(param.getNodeName().equalsIgnoreCase("link") && link.length() == 0) {
            link = getText(param);
          } else if(param.getNodeName().equalsIgnoreCase("description")) {
            description = getText(param);
            if(description != null && description.trim().startsWith("<")) {
              // strip html tags and entity references from description
              HtmlAnalyzer parser = new HtmlAnalyzer();
              try {
                new ParserDelegator().parse(new StringReader(description), parser, true);
                description = parser.getPlainText();
              } catch(IOException e) {
              }
            }
            description += "\n";
          }
        }
      }

      if (link.length() == 0)
				continue;
      if(link.indexOf("://") < 0 && !link.toLowerCase().startsWith( "magnet" )) {
        try {
          link = HtmlAnalyzer.resolveRelativeURL(urlBean.getLocation(), link);
        } catch(MalformedURLException e) {
          Plugin.debugOut("Bad link URL: " + link + " -> " + e.getMessage());
          continue;
        }
      }

      int state = ListBean.NO_DOWNLOAD;

      String titleTest = title.toLowerCase();
      String linkTest = link.toLowerCase();

      FilterBean curFilter = null;
      for(int i = 0; i < view.rssfeedConfig.getFilterCount(); i++) {
        curFilter = view.rssfeedConfig.getFilter(i);
        if(curFilter == null) continue;
        if(curFilter.matches(urlBean.getID(), titleTest, linkTest)) {
          if(curFilter.getMode().equalsIgnoreCase("Pass")) {
            state = ListBean.DOWNLOAD_INCL;
          } else {
            state = ListBean.DOWNLOAD_EXCL;
          }
          break;
        }
      }
      Episode e = null;
      final FilterBean filterBean = curFilter;
      if(filterBean != null && "TVShow".equalsIgnoreCase(filterBean.getType())) {
        try {
          e = FilterBean.getSeason(titleTest);
        } catch (Exception ee) {}
        try {
          if(e == null) e = FilterBean.getSeason(linkTest);
        } catch (Exception ee) {}
      }

      if(state == ListBean.DOWNLOAD_INCL) {
        Plugin.debugOut("testing for download: " + linkTest);
        if(filterBean.getUseSmartHistory()) {
          for(int i = 0; i < view.rssfeedConfig.getHistoryCount(); i++) {
            HistoryBean histBean = view.rssfeedConfig.getHistory(i);
            if(linkTest.equalsIgnoreCase(histBean.getLocation())) {
              Plugin.debugOut("found location match: " + histBean);
              state = ListBean.DOWNLOAD_HIST;
              break;
            }
            if(e != null && histBean.getFiltID() == filterBean.getID() && histBean.getSeasonStart() >= 0 && filterBean.getUseSmartHistory()) {
              int seasonStart = histBean.getSeasonStart();
              int episodeStart = histBean.getEpisodeStart();
              int seasonEnd = histBean.getSeasonEnd();
              int episodeEnd = histBean.getEpisodeEnd();
              Plugin.debugOut(e + " vs s" + seasonStart + "e" + episodeStart + " - s" + seasonEnd + "e" + episodeEnd);
              if(e.inRange(seasonStart, episodeStart, seasonEnd, episodeEnd)) {
                Plugin.debugOut("found filter and episode match: " + e);
                state = ListBean.DOWNLOAD_HIST;
                break;
              }
            }
          }
        } else Plugin.debugOut("Filter doesn't use smart history: " + filterBean);
      }

      final ListBean listBean = addTableElement(urlBean, listBeans, title, link, description, state);

      if(state == ListBean.DOWNLOAD_INCL) {
        // Add the feed
        final String curLink = link;
        boolean success = view.torrentDownloader.addTorrent(curLink, urlBean, filterBean, listBean);
        if(success && filterBean.getType().equalsIgnoreCase("Other") && filterBean.getDisableAfter())
          filterBean.setEnabled(false);

        if(view.isOpen() && view.display != null && !view.display.isDisposed())
          view.display.asyncExec(new Runnable() {
            public void run() {
              ListTreeItem listItem = view.treeViewManager.getItem(listBean);
              if(listItem != null) listItem.update();
            }
          });
      }
    }
  }

  private static String getText(Node node) {
    StringBuffer sb = new StringBuffer();
    node.normalize();
    NodeList children = node.getChildNodes();
    int childrenLen = children.getLength(), type;

    for(int iLoop = 0; iLoop < childrenLen; iLoop++) {
      Node child = children.item(iLoop);
      type = child.getNodeType();
      if(type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
        sb.append(child.getNodeValue());
        sb.append(" ");
      }
    }
    return sb.toString().trim();
  }

  private void addBacklogElements(UrlBean urlBean) {
    List backLog = urlBean.getBackLog();
    for(Iterator iter = backLog.iterator(); iter.hasNext(); )
      view.treeViewManager.addListBean((ListBean)iter.next(), urlBean, true);
    if(backLog.size() != urlBean.getPrevBackLogSize()) view.rssfeedConfig.storeOptions();
  }

  private ListBean addTableElement(UrlBean urlBean, ListGroup listBeans, String title, String link, String description, int state) {
    ListBean listBean = new ListBean();
    listBean.setName(title);
    listBean.setLocation(link);
    listBean.setState(state);
    listBean.setDescription(description);

    return addTableElement(urlBean, listBeans, listBean);
  }

  private ListBean addTableElement(UrlBean urlBean, ListGroup listBeans, ListBean listBean) {
    listBean.setFeed(urlBean);
    view.treeViewManager.addListBean(listBean, urlBean, false);
    listBeans.add(listBean);

    return listBean;
  }
}

