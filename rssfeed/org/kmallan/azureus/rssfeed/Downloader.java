/*
 *
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

import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.proxy.AEProxySelector;
import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class Downloader extends InputStream {

  final static int DOWNLOADER_NON_INIT = 0, DOWNLOADER_INIT = 1, DOWNLOADER_START = 2;
  final static int DOWNLOADER_DOWNLOADING = 3, DOWNLOADER_FINISHED = 4, DOWNLOADER_CANCELED = 5;
  final static int DOWNLOADER_NOTMODIFIED = 6, DOWNLOADER_CHECKING = 7, DOWNLOADER_ERROR = -1;

  private URL url;
  private URLConnection con;

  private int state = DOWNLOADER_NON_INIT;
  private int percentDone = 0;
  private int readTotal = 0;
  private int size = 0;
  private int refCount = 0;

  protected String fileName, contentType, etag;
  protected long lastModified;
  protected InputStream in;
  protected List listeners = new ArrayList();

  public int getState() {
    return state;
  }

  private synchronized void error(String err) {
    synchronized(listeners) {
      state = DOWNLOADER_ERROR;
      fireDownloaderUpdate(state, 0, 0, err);
    }
  }

/*
  public static void testDownload(String url) throws Exception {
    Downloader downloader = new Downloader();
    downloader.addListener(new DownloaderListener() {
      public void downloaderUpdate(int state, int percent, int amount, String err) {
        System.out.println(state + " " + percent + " " + amount + " " + err);
      }
    });
    downloader.init(url, "application/x-bittorrent, application/x-httpd-php", null, null, 0, null);

    if(downloader.getState() == Downloader.DOWNLOADER_CANCELED || downloader.getState() == Downloader.DOWNLOADER_ERROR) {
      System.out.println("null");
      return;
    }

    String filename = downloader.fileName;
    if(!downloader.fileName.toLowerCase().endsWith(".torrent"))
      filename = "temp-" + Long.toString((new Date()).getTime()) + "-" + Long.toString((new Random()).nextLong()) + ".torrent";

    File torrentLocation = new File(filename);
    torrentLocation.createNewFile();
    FileOutputStream fileout = new FileOutputStream(torrentLocation, false);

    byte[] buf = new byte[4096];
    int read;
    while((read = downloader.read(buf)) != -1) {
      fileout.write(buf, 0, read);
      if(downloader.getState() == Downloader.DOWNLOADER_CANCELED) break;
    }
    fileout.flush();
    fileout.close();

    if(downloader.getState() == Downloader.DOWNLOADER_CANCELED || downloader.getState() == Downloader.DOWNLOADER_ERROR) {
      downloader.done();
      torrentLocation.delete();
      return;
    }

    if(!downloader.fileName.toLowerCase().endsWith(".torrent")) {
      Plugin.debugOut("contentType: " + downloader.contentType);

      if(downloader.contentType != null && downloader.contentType.toLowerCase().startsWith("text/html")) {

        // html file encountered, look for link to torrent
        String href = TorrentDownloader.findTorrentHref(torrentLocation, url, null);
        if(href != null) {
          Plugin.debugOut("href: " + href);
          torrentLocation.delete();
          downloader.done();
          testDownload(href);
        } else throw new Exception("Html content returned, but no links to torrent files found.");

      } else if(downloader.contentType != null && !downloader.contentType.toLowerCase().startsWith("application/x-bittorrent")) {

        // something else encountered, just move it to outputdir
        System.out.println("other " + downloader.fileName);

      }
    }
    System.out.println("torrent?");
  }

  public static void main(String[] args) throws Exception {
    String url = "";
    testDownload(url);
  }
*/
  public void init(String urlStr, boolean forceNoProxy, String accept, String referer, String cookie, long lastModSince, String oldEtag) {

    Pattern exprHost = Pattern.compile(".*(https?://.*?)", Pattern.CASE_INSENSITIVE);
    Matcher m = exprHost.matcher(urlStr);
    if(m.matches() && !m.group(1).equalsIgnoreCase(urlStr)) urlStr = m.group(1);
    urlStr = urlStr.replaceAll(" ", "%20");

    try {
    	if ( forceNoProxy ){
    		
    		AEProxySelectorFactory.getSelector().startNoProxy();
    	}
      synchronized(listeners) {
        url = new URL(urlStr);

        for (int i=0;i<2;i++){
          try{
        
	        if(url.getProtocol().equalsIgnoreCase("https")) {
	          HttpsURLConnection sslCon = (HttpsURLConnection)url.openConnection();
	          
	          // allow for certs that contain IP addresses rather than dns names
	          sslCon.setHostnameVerifier(new HostnameVerifier() {
	            public boolean verify(String host, SSLSession session) {return true;}
	          });
	          con = sslCon;
	        } else {
	        
	          con = url.openConnection();
	        }
	
	        con.setDoInput(true);
	        con.setUseCaches(false);
	
	        if(con instanceof HttpURLConnection) {
	          exprHost = Pattern.compile("https?://([^/]+@)?([^/@:]+)(:[0-9]+)?/.*");
	          m = exprHost.matcher(urlStr.toLowerCase());
	          if(m.matches()) con.setRequestProperty("Host", m.group(2)); // isn't this handled automatically? /bow
	          con.setRequestProperty("User-Agent", Plugin.PLUGIN_VERSION);
	          if(referer != null && referer.length() > 0) con.setRequestProperty("Referer", referer);
	          if(accept != null && accept.length() > 0) con.setRequestProperty("Accept", accept);
	          if(cookie != null && cookie.length() > 0) con.setRequestProperty("Cookie", cookie);
	          if(lastModSince > 0) con.setIfModifiedSince(lastModSince);
	          if(oldEtag != null) con.setRequestProperty("If-None-Match", oldEtag);
	        }
	
	        state = DOWNLOADER_INIT;
	        fireDownloaderUpdate(state, 0, 0, "");
	
	        con.connect();        
	
	        if(con instanceof HttpURLConnection) {
	          int response = ((HttpURLConnection)con).getResponseCode();
	          Plugin.debugOut("response code: " + response);
	
	          if(response == -1) { // HttpURLConnection in undefined state? weird stuff... occurs sporadically
	            Thread.sleep(10000); // waiting and trying again seems to do the trick
	            if(refCount++ < 5) {
	              init(urlStr, forceNoProxy,accept, referer, cookie, lastModSince, oldEtag);
	              return;
	            }
	          }
	
	          String refresh = con.getHeaderField("Refresh");
	          if(refresh != null) {
	            Plugin.debugOut("refresh: " + refresh);
	            int idx = refresh.indexOf("url=");
	            if(idx > -1) {
	              refresh = refresh.substring(idx + 4);
	              if(refresh.indexOf(' ') > -1) refresh = refresh.substring(0, refresh.lastIndexOf(' '));
	              ((HttpURLConnection)con).disconnect();
	              if(refresh.indexOf("://") == -1) refresh = HtmlAnalyzer.resolveRelativeURL(urlStr, refresh);
	              Plugin.debugOut("new url: " + refresh);
	              if(refCount++ < 3) init(refresh, forceNoProxy,accept, referer, cookie, lastModSince, oldEtag);
	            }
	          }
	
	          if(response == HttpURLConnection.HTTP_NOT_MODIFIED) {
	            state = DOWNLOADER_NOTMODIFIED;
	            return;
	          } else if((response != HttpURLConnection.HTTP_ACCEPTED) && (response != HttpURLConnection.HTTP_OK)) {
	            error("Bad response for '" + url.toString() + "': " + Integer.toString(response) + " " + ((HttpURLConnection)con).getResponseMessage());
	            return;
	          }
	          contentType = con.getContentType();
	          lastModified = con.getLastModified();
	          etag = con.getHeaderField("ETag");
	          url = con.getURL();
	
	          // some code to handle b0rked servers.
	          fileName = con.getHeaderField("Content-Disposition");
	          if((fileName != null) && fileName.toLowerCase().matches(".*attachment.*"))
	            while(fileName.toLowerCase().charAt(0) != 'a') fileName = fileName.substring(1);
	          if((fileName == null) || !fileName.toLowerCase().startsWith("attachment") || (fileName.indexOf('=') == -1)) {
	            String tmp = url.getFile();
	            if(tmp.lastIndexOf('/') != -1) tmp = tmp.substring(tmp.lastIndexOf('/') + 1);
	            // remove any params in the url
	            int paramPos = tmp.indexOf('?');
	            if(paramPos != -1) tmp = tmp.substring(0, paramPos);
	
	            fileName = URLDecoder.decode(tmp, Constants.DEFAULT_ENCODING);
	          } else {
	            fileName = fileName.substring(fileName.indexOf('=') + 1);
	            if(fileName.startsWith("\"") && fileName.endsWith("\"")) fileName = fileName.substring(1, fileName.lastIndexOf('\"'));
	            File temp = new File(fileName);
	            fileName = temp.getName();
	          }
	        }
	        
	        break;
          }catch( SSLException e ){
        	  
        	  if ( i == 0 ){
        		  
        		  Plugin.getPluginInterface().getUtilities().getSecurityManager().installServerCertificate( url );
        		  
        	  }else{
        		  
        		  throw( e );
        	  }
          }
        }

      }
    } catch(java.net.MalformedURLException e) {
      e.printStackTrace();
      error("Bad URL '" + url + "':" + e.getMessage());
    } catch(java.net.UnknownHostException e) {
      e.printStackTrace();
      error("Unknown Host '" + e.getMessage() + "'");
    } catch(IOException ioe) {
      ioe.printStackTrace();
      error("Failed: " + ioe.getMessage());
    } catch(Throwable e) {
      e.printStackTrace();
      error("Failed: " + e.toString());
    }finally{
    	
    	if ( forceNoProxy ){
    		
    		AEProxySelectorFactory.getSelector().endNoProxy();
    	}
    }

    if(state != DOWNLOADER_ERROR) {
      synchronized(listeners) {
        state = DOWNLOADER_START;
        fireDownloaderUpdate(state, 0, 0, "");
        state = DOWNLOADER_DOWNLOADING;
        fireDownloaderUpdate(state, 0, 0, "");
      }
      try {
        in = con.getInputStream();
        
        String encoding = con.getHeaderField( "content-encoding");

        boolean compressed = false;
        
        if ( encoding != null ){

        	if ( encoding.equalsIgnoreCase( "gzip"  )){

        		compressed = true;

        		in = new GZIPInputStream( in );

        	}else if ( encoding.equalsIgnoreCase( "deflate" )){

        		compressed = true;

        		in = new InflaterInputStream( in );
        	}
        }
			
        size = compressed?-1:con.getContentLength();
        percentDone = readTotal = 0;
      } catch(Exception e) {
        error("Exception while downloading '" + url.toString() + "':" + e.getMessage());
      }
    }
  }

  public InputStream getStream() {
    return in;
  }

  public void cancel() {
    if(state == DOWNLOADER_ERROR) return;
    synchronized(listeners) {
      state = DOWNLOADER_CANCELED;
      fireDownloaderUpdate(state, 0, 0, "");
    }
  }

  public void done() {
    if(state == DOWNLOADER_ERROR || state == DOWNLOADER_CANCELED || state == DOWNLOADER_FINISHED) return;
    synchronized(listeners) {
      if(state != DOWNLOADER_NOTMODIFIED && readTotal == 0) {
        error("No data contained in '" + url.toString() + "'");
        return;
      }
      state = DOWNLOADER_FINISHED;
      fireDownloaderUpdate(state, 0, 0, "");
    }
  }

  public void notModified() {
    if(state == DOWNLOADER_NOTMODIFIED) return;
    synchronized(listeners) {
      state = DOWNLOADER_NOTMODIFIED;
      fireDownloaderUpdate(state, 0, 0, "");
    }
  }

  protected void finalize() {
    try {
      in.close();
    } catch(Throwable e) {}
    
    try{
    	if ( con instanceof HttpURLConnection ){
    		((HttpURLConnection)con).disconnect();
    		
    	}
    }catch( Throwable e ){}
    done();
  }

  // listeners
  public void addListener(DownloaderListener l) {
    synchronized(listeners) {
      listeners.add(l);
    }
  }

  public void removeListener(DownloaderListener l) {
    synchronized(listeners) {
      listeners.remove(l);
    }
  }

  private void fireDownloaderUpdate(int state, int percentDone, int readTotal, String str) {
    for(int i = 0; i < listeners.size(); i++) {
      ((DownloaderListener)listeners.get(i)).downloaderUpdate(state, percentDone, readTotal, str);
    }
  }

  // InputStream Filtered Stuff
  public int read() throws IOException{
    try {
      synchronized(listeners) {
        int read = in.read();
        if ( read == -1 ){
        	return( -1 );
        }
        readTotal += read;
        if(this.size > 0) {
          this.percentDone = (100 * this.readTotal) / this.size;
          fireDownloaderUpdate(state, percentDone, 0, "");
        } else fireDownloaderUpdate(state, 0, readTotal, "");
        return read;
      }
    } catch(IOException e) {error( e.getMessage());}
    return -1;
  }

  public int read(byte b[]) throws IOException{
    return read(b, 0, b.length);
  }

  public int read(byte b[], int off, int len) throws IOException {
    try {
      synchronized(listeners) {
        int read = in.read(b, off, len);
        if ( read == -1 ){
        	return( -1 );
        }
        this.readTotal += read;
        if(this.size > 0) {
          this.percentDone = (100 * this.readTotal) / this.size;
          fireDownloaderUpdate(state, percentDone, 0, "");
        } else fireDownloaderUpdate(state, 0, readTotal, "");
        return read;
      }
    } catch(IOException e) {
    	error( e.getMessage());
    }
    return -1;
  }

  // InputStream PassThru Stuff
  public long skip(long n) {
    try { return in.skip(n); } catch(IOException e) { }
    return 0;
  }

  public int available() {
    try { return in.available(); } catch(IOException e) { }
    return 0;
  }

  public void close() { try { in.close(); } catch(IOException e) { } }
  public synchronized void mark(int readlimit) { in.mark(readlimit); }
  public synchronized void reset() { try { in.reset(); } catch(IOException e) { } }
  public boolean markSupported() { return in.markSupported(); }
}
