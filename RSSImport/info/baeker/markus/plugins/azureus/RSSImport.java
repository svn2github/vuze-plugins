/*
 * Created on 24.04.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package info.baeker.markus.plugins.azureus;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentDownloader;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;

import de.mbaeker.utilities.REUtilities;

/** 
 * @author BaekerAdmin
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RSSImport implements Plugin, Runnable, PluginListener {
	private PluginInterface azureus = null;
	private boolean initialized = false;
	private boolean stop = false;
	private LoggerChannel log = null;
	private static RE itemMatcher = null;
	private static RE linkMatcher = null;
	private static RE enclosureMatcher = null;

	private static Thread runner = null;
	private java.util.Date lastCycle = null;
	private BasicPluginViewModel model = null;
	private int numberOfChannels = 0;
	private int currentChannel = 0;
	private int numberOfEntries = 0;
	private int currentEntry = 0;
	private String filter = null;
	/**
	 * @return
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * @param filter
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}

	public static void main(String[] args) throws MalformedURLException {
		RSSImport rss = new RSSImport();
		rss.checkChannel(new URL("http://66.199.180.26/~tvt/tvt.xml"));
		//torrents without ending .torrent
		rss.checkChannel(new URL("http://anime.mircx.com/rss"));
		rss.checkChannel(new URL("http://www.torrents.co.uk/backend.php"));
		//torrents in enclosure 
		rss.checkChannel(new URL("http://www.legaltorrents.com/musicrss.xml"));

	}

	public void setStatus(String text) {
		if (model != null) {
			model.getStatus().setText(text);
		} else {
			//System.out.println("Status: " + text);
		}

	}
	private void updateProgressBar() {
		if (model != null) {
			int percent = 0;
			if (numberOfChannels > 0) {
				if (numberOfEntries > 0) {
					percent =
						(int) (((currentChannel - 1d)
							+ (double) currentEntry / (double) numberOfEntries)
							/ numberOfChannels
							* 100d);
				} else {
					percent =
						(int) ((currentChannel - 1d) / numberOfChannels * 100d);
				}
			}

			model.getProgress().setPercentageComplete(percent);
		}
	}

	public void log(int type, String text, boolean activity) {
		if (log != null) {
			log.log(type, text);
			if (model != null) {
				model.getLogArea().appendText("\n" + text);
				if (activity) {
					model.getActivity().setText(text);
				}
			}
		} else {
			//System.out.println(text);
		}
	}

	public void log(String data, Throwable error) {
		if (log != null) {
			log.log(data, error);
			if (model != null) {
				model.getLogArea().appendText("\n" + error.getMessage());
			}
			setStatus("Error.");
		} else {
			//System.out.println(data);
			//error.printStackTrace(System.out);
		}
	}

	/**
	 * checks if the torrent t should be downloaded
	 * by compairing it to the filter setting
	 * if filter is null or empty return is false
	 * @param t
	 * @return
	 */
	protected boolean checkFilter(String item) {
		boolean rt = false;
		String filter = getFilter();

		//if setting is empty or null then everything will be downloaded
		if (filter == null || filter.trim().length() == 0) {
			return false;
		}

		log(
			LoggerChannel.LT_INFORMATION,
			"    Compairing filter \"" + filter + "\" to \"" + item + "\"",
			false);
		StringTokenizer st = new StringTokenizer(filter, ";");
		while (st.hasMoreElements()) {
			String token = st.nextToken();
			if (!token.startsWith(".*") & !token.endsWith(".*")) {
				token = ".*" + token + ".*";
			}
			//System.out.println(token);
			RE re;
			try {
				re = REUtilities.getInstance().getRE(token,RE.REG_ICASE);
				if (re.isMatch(item)) {
					return true;
				}
			} catch (REException e) {
				log("Wrong regEx: " + token, e);
			}
		}

		return rt;
	}

	/**
	 * adds a torrent to azureus but first checks if the url already exists
	 * @param url java.net.URL the Tracker URL
	 */
	private void addTorrent(String url, String item) {
		if (azureus != null) {

			//check if link is a torrent
			if (RSSImportUtilties.getInstance().isTorrent(url)) {
				try {
					log(
						LoggerChannel.LT_INFORMATION,
						"  Torrent " + url + " found.",
						false);
					if (checkFilter(item) & !RSSImportConfig.getInstance().isInHistory(url)) {
						TorrentDownloader td =
							azureus.getTorrentManager().getURLDownloader(
								new URL(url));

						Torrent t = td.download();
						//azureus.getDownloadManager().addDownload(t,new File("xyz"),new File("xyz"));
						azureus.getDownloadManager().addDownload(t);
						if (RSSImportConfig.getInstance().isHistory()) {
							RSSImportConfig.getInstance().addHistory(url);
						}
						log(
							LoggerChannel.LT_INFORMATION,
							"    Torrent " + url + " added.",
							false);
					} else {
						if (!RSSImportConfig.getInstance().isInHistory(url)) {
						log(
							LoggerChannel.LT_INFORMATION,
							"    Filter prevented torrent "
								+ url
								+ " from being downloaded.",
							false);
						}
						else
						{
							log(
								LoggerChannel.LT_INFORMATION,
								"    Torrent  "
									+ url
									+ " is in history.",
								false);
							
						}
					}
				} catch (TorrentException e) {
					log("URL:" + url, e);
				} catch (DownloadException e) {
					log("URL:" + url, e);
				} catch (MalformedURLException e) {
					log("URL:" + url, e);
				}
			}
		} else
			log(
				LoggerChannel.LT_INFORMATION,
				"No azureus found (debug mode?)"
					+ url
					+ " isTorrent"
					+ RSSImportUtilties.getInstance().isTorrent(url),
				false);

	}

	/**
	 * Retrieves a channel with Informa and
	 * adds all found torrents
	 * @param url java.net.URL
	 */
	private void checkChannel(URL url) {
		log(
			LoggerChannel.LT_INFORMATION,
			RSSImportConfig.getInstance().getMessage(
				RSSImportConfig.RSSIMPORT_CHECK_CHANNEL)
				+ " "
				+ url.toExternalForm(),
			true);
		setFilter(RSSImportConfig.getInstance().getFilter());
		BufferedReader in = null;
		try {
			HttpClient client = new HttpClient();
			client.getParams().setConnectionManagerTimeout(
				RSSImportConfig.getInstance().getTimeout());

			GetMethod get = new GetMethod(url.toExternalForm());

			try{
				client.executeMethod(get);
				
			}catch( SSLException e ){
				
				SSLSocketFactory fact = azureus.getUtilities().getSecurityManager().installServerCertificate( url );
				
				int	port = url.getPort();
				
				if ( port == -1 ){
					
					port = url.getDefaultPort();
				}
				
				Protocol myhttps = new Protocol("https", new mySSLSocketFactory( fact ), url.getDefaultPort());

				client = new HttpClient();
				
				client.getParams().setConnectionManagerTimeout(
					RSSImportConfig.getInstance().getTimeout());

				client.getHostConfiguration().setHost( url.getHost(), port, myhttps);

				get = new GetMethod( url.getFile());
				
				client.executeMethod(get);
			}

			in =
				new BufferedReader(
					new InputStreamReader(get.getResponseBodyAsStream()));
			String line = null;
			StringBuffer body = new StringBuffer();
			while ((line = in.readLine()) != null) {
				if (line.indexOf("<item>") > -1) {
					while (line.indexOf("<item>") > -1) {
						body.append(line.substring(0, line.indexOf("<item>")));
						body.append("\n<item>");
						line = line.substring(line.indexOf("<item>") + 6);
					}
					body.append(line);
				} else {
					body.append(line);
				}
			}
			get.releaseConnection();
			if (itemMatcher == null) {
				itemMatcher = new RE("<item>(.*)</item>");
				linkMatcher = new RE("<link>(.*)</link>");
				enclosureMatcher = new RE("<enclosure(.*)/");
			}
			REMatch[] items = itemMatcher.getAllMatches(body);
			numberOfEntries = items.length;
			if (numberOfEntries == 0) {
				log(LoggerChannel.LT_INFORMATION, "  no entries found.", false);
			}
			for (int currentEntry = 1;
				(currentEntry <= items.length) && !stop;
				currentEntry++) {
				//updating progress bar
				updateProgressBar();

				//extract links
				String item = items[currentEntry - 1].toString(1).trim();
				//first check for enclosure
				if (item.indexOf("<enclosure") > -1) {
					Enumeration e = enclosureMatcher.getMatchEnumeration(item);
					while (e.hasMoreElements()) {
						String enclosure =
							((REMatch) e.nextElement()).toString(1);
						if (enclosure.indexOf("url") > -1) {
							String link =
								enclosure.substring(
									enclosure.indexOf("url") + 3);
							link = link.substring(link.indexOf("\"") + 1);
							link = link.substring(0, link.indexOf("\""));
							addTorrent(link, item);
						}
					}
				} else //then the link section
					{
					Enumeration e = linkMatcher.getMatchEnumeration(item);
					while (e.hasMoreElements()) {
						String link = ((REMatch) e.nextElement()).toString(1);
						addTorrent(link, item);
					}
				}
				//System.out.println(item);
			}
		} catch (MalformedURLException e) {
			log("Channel: " + url.toExternalForm(), e);
		} catch (IOException e) {
			log("Channel: " + url.toExternalForm(), e);
		} catch (REException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/** (non-Javadoc)
								 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
								 **/
	public void initialize(PluginInterface pluginInterface)
		throws PluginException {
		this.azureus = pluginInterface;
		azureus.addListener(this);
		RSSImportConfig.getInstance().setRSSImport(this);
		log = azureus.getLogger().getChannel(RSSImportConfig.RSSIMPORT);
		model = azureus.getUIManager().createBasicPluginViewModel(RSSImportConfig.RSSIMPORT);

		if (runner == null) {
			//runner = pluginInterface.getUtilities().createThread("RSSImport", this);
			runner = new Thread(this);
			runner.setDaemon(true);
			runner.start();
		} else {
			if (!runner.isAlive()) {
				runner.start();
			}
		}

	}

	public boolean checkSettings(boolean silent) {
		boolean rt = true;
		//if no default directory is set or the default directory is disabled then
		//deactivate this plugin and give a warning message

		if (!azureus
			.getPluginconfig()
			.getUnsafeBooleanParameter("Use default data dir")
			|| azureus.getPluginconfig().getUnsafeStringParameter("Default save path")
				== null) {
			rt = false;
			if (!silent) {
				log(
					LoggerChannel.LT_ERROR,
					RSSImportConfig.getInstance().getMessage(
						RSSImportConfig.RSSIMPORT_DEFAULT_DIR_WARNING),
					true);
			}
		}

		String feed = RSSImportConfig.getInstance().getFeed();
		if (feed == null || feed.trim().length() == 0) {
			rt = false;
			if (!silent) {
				log(
					LoggerChannel.LT_ERROR,
					RSSImportConfig.getInstance().getMessage(
						RSSImportConfig.RSSIMPORT_FEED_WARNING),
					true);
			}

		}
		return rt;
	}
	/** the main method, which will get the rss channels setting and
	 * starts the channel check
		 * @see java.lang.Runnable#run()
		 **/
	public void run() {
		//wait for the initialize event... (but it is only the event for the plugins?)
		while (!initialized) {
			try {
				Thread.sleep(5000); //5 sec
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		//just give the interface some time...
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		//cycle through check...
		while (!stop) {
			//check settings
			boolean lastActiveSetting =
				RSSImportConfig.getInstance().isActive();
			if (!checkSettings(false)) {
				RSSImportConfig.getInstance().setActive(false);
			}
			//if plugin is deactivated it will wait until status is changed...
			if (!RSSImportConfig.getInstance().isActive()) {
				log(
					LoggerChannel.LT_INFORMATION,
					RSSImportConfig.getInstance().getMessage(
						RSSImportConfig.RSSIMPORT_STATUS_DEACTIVATED),
					true);
				setStatus(
					RSSImportConfig.getInstance().getMessage(
						RSSImportConfig.RSSIMPORT_STATUS_DEACTIVATED));
			}

			while (!RSSImportConfig.getInstance().isActive()) {
				try {
					Thread.sleep(5000); //5 sec

				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				//recheck settings and disable again if user changed setting
				//to activated without changing nessesary settings
				//(just to annoy the user ;-) )
				if (!checkSettings(true)) {
					RSSImportConfig.getInstance().setActive(false);
				} else if (
					//if this plugin has deactivated itself because of missing
				//settings and now the settings are correct, then restore
				//the old active setting...
				!RSSImportConfig
					.getInstance()
					.isActive()
						& lastActiveSetting) {
					RSSImportConfig.getInstance().setActive(true);
				}
			}
			setStatus(
				RSSImportConfig.getInstance().getMessage(
					RSSImportConfig.RSSIMPORT_STATUS_ACTIVE));

			lastCycle = new Date();
			setStatus(
				RSSImportConfig.getInstance().getMessage(
					RSSImportConfig.RSSIMPORT_STATUS_RUNNING));
			log(
				LoggerChannel.LT_INFORMATION,
				"Starting check of RSS channels.",
				true);
			model.getProgress().setPercentageComplete(0);

			//get the feed url setting
			String feed = RSSImportConfig.getInstance().getFeed();

			//does it exists?
			if (feed != null && feed.length() > 0) {
				//more than one feed? (separated by ;)
				StringTokenizer st = new StringTokenizer(feed, ";");
				numberOfChannels = st.countTokens();
				currentChannel = 0;

				while (st.hasMoreElements() && !stop) {
					currentChannel++;
					String url = st.nextToken();
					try {
						checkChannel(new URL(url));
					} catch (MalformedURLException e) {

						log("No URL: " + url, e);
					}
				}
			}
			model.getProgress().setPercentageComplete(100);

			model.getActivity().setText(
				RSSImportConfig.getInstance().getMessage(
					RSSImportConfig.RSSIMPORT_LAST_CHECK)
					+ ": "
					+ DateFormat.getInstance().format(lastCycle));

			//waiting for recheck
			long milliWaits =
				RSSImportConfig.getInstance().getRecheck() * 60 * 1000;
			if (milliWaits > 0) {
				log(
					LoggerChannel.LT_INFORMATION,
					"Waiting "
						+ milliWaits / 60 / 1000
						+ " min for recheck. ("
						+ DateFormat.getDateTimeInstance().format(new Date())
						+ ")",
					true);
				setStatus(
					RSSImportConfig.getInstance().getMessage(
						RSSImportConfig.RSSIMPORT_STATUS_ACTIVE));
				try {
					Thread.sleep(milliWaits);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				//recheck<=0 -> no cylce
				log(
					LoggerChannel.LT_INFORMATION,
					"Recheck set to zero -> exiting cycle.",
					false);
				stop = true;
				setStatus(
					RSSImportConfig.getInstance().getMessage(
						RSSImportConfig.RSSIMPORT_STATUS_DEACTIVATED));
			}

		}

	}

	/* (non-Javadoc)
					 * @see org.gudy.azureus2.plugins.PluginListener#initializationComplete()
					 */
	public void initializationComplete() {
		initialized = true;
	} /* (non-Javadoc)
																																 * @see org.gudy.azureus2.plugins.PluginListener#closedownInitiated()
																																 */
	public void closedownInitiated() {
		stop = true;

	} /* (non-Javadoc)
																															 * @see org.gudy.azureus2.plugins.PluginListener#closedownComplete()
																															 */
	public void closedownComplete() {

	}
	/**
	 * @return
	 */
	public PluginInterface getAzureus() {
		return azureus;
	}

	/**
	 * @return
	 */
	public BasicPluginViewModel getModel() {
		return model;
	}

	/**
	 * @return
	 */
	public boolean isStop() {
		return stop;
	}

	class
	mySSLSocketFactory
		implements ProtocolSocketFactory
	{
		private SSLSocketFactory	factory;
		
		protected
		mySSLSocketFactory(
			SSLSocketFactory	_factory )
		{
			factory	= _factory;
		}
		
	    public Socket 
	    createSocket(
	        String host,
	        int port,
	        InetAddress clientHost,
	        int clientPort )
	        throws IOException, UnknownHostException 
	    {
	    	return factory.createSocket(
	                host,
	                port,
	                clientHost,
	                clientPort
	            );
	    }

	
        public Socket createSocket(
            final String host,
            final int port,
            final InetAddress localAddress,
            final int localPort,
            final HttpConnectionParams params ) 
        
        	throws IOException, UnknownHostException, ConnectTimeoutException 
        {
            if (params == null) {
                throw new IllegalArgumentException("Parameters may not be null");
            }
           
            int timeout = params.getConnectionTimeout();
            
            if (timeout == 0) {
            	
                return createSocket(host, port, localAddress, localPort);
                
            }else{
               
                return ControllerThreadSocketFactory.createSocket(
                        this, host, port, localAddress, localPort, timeout);
            }
        }


        public Socket 
        createSocket(
        	String host, int port )
            	
        	throws IOException, UnknownHostException 
        {	
            return factory.createSocket( host, port );
        }

   
        public Socket 
        createSocket(
            Socket socket,
            String host,
            int port,
            boolean autoClose)
            throws IOException, UnknownHostException 
        {
            return factory.createSocket( socket, host, port, autoClose );
        }
    }
}
