/*
 * Created on 13.06.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package info.baeker.markus.plugins.azureus;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;

import de.mbaeker.utilities.html.MimeType;

/**
 * @author BaekerAdmin
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RSSImportUtilties {
	private static final RSSImportUtilties riu = new RSSImportUtilties();
	private RSSImport rssImport = null;
	private HttpClient client = null;
	private RSSImportUtilties() {

	}

	private void log(String data, Throwable error) {
		if (rssImport != null) {
			rssImport.log(data, error);
		} else {
			System.out.println(data);
			error.printStackTrace(System.out);
		}
	}

	public static RSSImportUtilties getInstance() {
		return riu;
	}

	public boolean isTorrent(String url) {
		if (MimeType
			.guessMimeType(url)
			.equalsIgnoreCase("application/x-bittorrent")) {
			return true;
		}
		if (!RSSImportConfig.getInstance().isFastCheck()) {
			try {
				//not directly by link identified as a torrent.
				//open a stream to the url
				if (client == null) {
					client = new HttpClient();
				}
				client.getParams().setConnectionManagerTimeout(
					RSSImportConfig.getInstance().getTimeout() * 1000l);
				HeadMethod h = new HeadMethod(url);
				client.executeMethod(h);
				Header ctype = h.getResponseHeader("Content-Type");
				if (ctype != null
					&& ctype.getValue().equalsIgnoreCase(
						"application/x-bittorrent")) {
					return true;
				}
				h.releaseConnection();
			} catch (IOException e) {
				log("Error identifying torrent " + url, e);
			}
		}
		return false;
	}
	public static void main(String[] args) {
	}

	/**
	 * @return
	 */
	public RSSImport getRssImport() {
		return rssImport;
	}

	/**
	 * @param rssImport
	 */
	public void setRssImport(RSSImport rssImport) {
		this.rssImport = rssImport;
	}

}
