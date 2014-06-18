/******************************************************************************
Cubit distribution
Copyright (C) 2008 Bernard Wong

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

The copyright owner can be contacted by e-mail at bwong@cs.cornell.edu
 *******************************************************************************/

package hyper.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.cornell.hyper.overlay.AddressPort;
import org.cornell.hyper.overlay.Consumer;
import org.cornell.hyper.overlay.EntryContainer;
import org.cornell.hyper.overlay.HyperNode;
import org.cornell.hyper.overlay.KeyVal;
import org.cornell.hyper.overlay.MovieEntry;
import org.cornell.hyper.overlay.RemoteHyperNode;
import org.cornell.hyper.overlay.XmlRPCClient;
import org.cornell.hyper.overlay.MovieEntry.MovieEntryException;
import org.cornell.hyper.overlay.MovieEntry.TimedPublicKey;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.TrackerException;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.search.SearchException;

import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

import hyper.keygen.*;

// TODO: Add an appender class that bridges all apache style logs
// to the Azureus logging system.

public class HyperPlugin implements UnloadablePlugin, PluginListener {
	private static final String VIEWID = "hyperspace";
	private static HyperPlugin singleton;
	private PluginInterface pluginInterface;
	private BasicPluginConfigModel configModel;
	private BasicPluginViewModel viewModel;
	private ViewListener viewListener = null;
	private UISWTInstance swtInstance = null;
	private Logger logger;
	private LoggerChannel logChannel = null;
	private LoggerChannelListener logListener;
	private HyperNode hyperNode = null;
	private UIManagerListener uiListener;
	private final int charPerPerturb = 4;
	public final int defaultSearchTimeout = 15000;
	private Set<RemoteHyperNode> seedRemoteNodes;
	private EntryContainer entryContainer;
	private DownloadManager dlManager;
	private DownloadManagerListener dlListener;
	private MovieInserter movieInserter = null;
	private final int keyTimeoutDays = 30;	
		
	private byte[] prodSig 			= null;
	private TimedPublicKey pubKey 	= null;
	private PrivateKey privKey 		= null;		

	protected Display getDisplay() {
		if (swtInstance == null) {
			return null;
		}
		return swtInstance.getDisplay();
	}

	public LoggerChannel getLogger() {
		return logChannel;
	}

	public void unload() throws PluginException {
		if (swtInstance != null) {
			swtInstance.removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
		}
		configModel.destroy();
		if (hyperNode != null) {
			hyperNode.stopNode();
			hyperNode = null;
		}
		if (movieInserter != null) {
			movieInserter.stopMovieInserter();
			movieInserter = null;
		}
		logChannel.removeListener(logListener);
		pluginInterface.getUIManager().removeUIListener(uiListener);
		viewModel.destroy();
		configModel.destroy();
		pluginInterface.removeListener(this);
	}

	public PluginConfig getConfig() {
		return pluginInterface.getPluginconfig();
	}

	protected PluginInterface getPluginInterface() {
		return pluginInterface;
	}

	public String getNATCheckURL() {
		StringBuffer strBuf = new StringBuffer();
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle("hyper.plugins.NATCheck");
			logChannel.log("Loading default NAT checker URLs");
		} catch (MissingResourceException e) {
			e.printStackTrace();
			if (logChannel != null) {
				logChannel.log("Cannot find the NAT checker property file");
			}
			return strBuf.toString();
		}
		Set<String> sortedSet = new TreeSet<String>();
		Enumeration<String> keysEnum = rb.getKeys();
		while (keysEnum.hasMoreElements()) {
			sortedSet.add(rb.getString(keysEnum.nextElement()));
		}
		Iterator<String> sortedIt = sortedSet.iterator();
		while (sortedIt.hasNext()) {
			strBuf.append(sortedIt.next());
			strBuf.append(" ");
		}
		return strBuf.toString();
	}

	public String getSeedNodes() {
		StringBuffer strBuf = new StringBuffer();
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle("hyper.plugins.SeedNodes");
			logChannel.log("Loading default seed nodes");
		} catch (MissingResourceException e) {
			if (logChannel != null) {
				logChannel.log("Cannot find the SeedNodes properties file");
			}
			return strBuf.toString();
		}
		Set<String> sortedSet = new TreeSet<String>();
		Enumeration<String> keysEnum = rb.getKeys();
		while (keysEnum.hasMoreElements()) {
			sortedSet.add(rb.getString(keysEnum.nextElement()));
		}
		Iterator<String> sortedIt = sortedSet.iterator();
		while (sortedIt.hasNext()) {
			strBuf.append(sortedIt.next());
			strBuf.append(" ");
		}
		return strBuf.toString();
	}

	private void initLogger() {
		logger = pluginInterface.getLogger();
		logChannel = logger.getTimeStampedChannel("Cubit Search");
		logListener = new LoggerChannelListener() {
			public void messageLogged(int type, String content) {
				viewModel.getLogArea().appendText(content + "\n");
			}

			public void messageLogged(String str, Throwable error) {
				if (str.length() > 0) {
					viewModel.getLogArea().appendText(str + "\n");
				}
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				error.printStackTrace(pw);
				pw.flush();
				viewModel.getLogArea().appendText(sw.toString() + "\n");
			}
		};
		logChannel.addListener(logListener);
	}
	
	public int getEditDist(String movString, List<String> keywords) {
		return entryContainer.computeDistSum(movString, keywords);		
	}

	public List<String> issueSearch(String searchStr, 
			int numResults, Consumer<MovieEntry> inConsumer) {
		//List<MovieEntry> movieList = null;
		if (hyperNode != null) {
			return hyperNode.findTopKMoviesConsumer(numResults, searchStr, 
				charPerPerturb, defaultSearchTimeout, inConsumer);
			//movieList = hyperNode.findTopKMovies(numResults, searchStr,
			//		charPerPerturb, defaultSearchTimeout);
		}
		return XmlRPCClient.findTopKMoviesConsumer(entryContainer,
			seedRemoteNodes, numResults, searchStr, charPerPerturb,
			defaultSearchTimeout, inConsumer);
		/*
		} else {
			movieList = XmlRPCClient.findTopKMovies(entryContainer,
					seedRemoteNodes, numResults, searchStr, charPerPerturb,
					defaultSearchTimeout);
		}		
		if (movieList != null) {
			Set<String> strictUnique = new HashSet<String>();
			Iterator<MovieEntry> listIt = movieList.iterator();
			while (listIt.hasNext()) {
				MovieEntry curMovie = listIt.next();
				StringBuffer strBuf = new StringBuffer();
				strBuf.append(curMovie.getTorName());
				strBuf.append(curMovie.getMovName());
				strBuf.append(curMovie.getMagLink());
				String fullString = strBuf.toString();
				if (strictUnique.contains(fullString)) {
					// Duplicate entry (with different key/sig) already
					// exists, remove it from being displayed.
					listIt.remove();
					continue;
				}
				strictUnique.add(fullString);
			}
		}
		return movieList;
		*/
	}

	public void initialize(PluginInterface inPluginInter) 
			throws PluginException {
		if (singleton != null) {
			throw new IllegalStateException("Plugin already initialized"); 
		}
		singleton 		= this;
		pluginInterface	= inPluginInter;
		UIManager ui	= pluginInterface.getUIManager();
		
		// Used for providing icon for search API
		try {
			TrackerWebContext ctx = pluginInterface.getTracker().createWebContext(
					"Cubit", 0, Tracker.PR_HTTP, InetAddress.getByName( "127.0.0.1" ));
			ctx.addPageGenerator(new TrackerWebPageGenerator() {
				public boolean generate(TrackerWebPageRequest request,
						TrackerWebPageResponse response) throws IOException {
					// TODO Auto-generated method stub					
					String path = request.getURL();
					System.out.println("************ Path is " + path);
					if (path.equals("...")) {						
						return response.useFile("/hyper/plugins/", "search.png");
					}
					return false;
				}							
			});			
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TrackerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
					
		viewModel = ui.createBasicPluginViewModel("Cubit Log");
		viewModel.getActivity().setVisible(false);
		viewModel.getProgress().setVisible(false);
		viewModel.getStatus().setText("Running");
		
		// Setup the logger
		initLogger();
		
		// Add user configurable options
		configModel = ui.createBasicPluginConfigModel("plugins", "plugin.hyper");
		configModel.addIntParameter2("port", "hyper.port", 3184);		
		configModel.addStringParameter2(
				"urls", "hyper.url", getNATCheckURL());
		configModel.addStringParameter2(
				"seedNodes", "hyper.seedNodes", getSeedNodes());
		// configModel.addBooleanParameter2("enable", "hyper.enable", true);
		// configModel.addBooleanParameter2("autoOpen", "hyper.autoOpen", true);
		
        // Get notified when the UI is attached
		final HyperPlugin thisPlugin = this;
		uiListener = new UIManagerListener() {
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
                    swtInstance = ((UISWTInstance) instance);
                    viewListener = new ViewListener(thisPlugin);
                    if (viewListener != null) {
                        // Add it to the menu
                        swtInstance.addView(
                        	UISWTInstance.VIEW_MAIN, VIEWID, viewListener);
                        // Open it immediately
                        swtInstance.openMainView(VIEWID, viewListener, null);
                        
                		try {
                			pluginInterface.getUtilities().registerSearchProvider(viewListener);
                		} catch (SearchException e) {
                			System.err.println("Cannot register Cubit as a search provider");
                			// TODO Auto-generated catch block
                			e.printStackTrace();
                		}
                    }
                }
            }
            public void UIDetached(UIInstance instance) {
            	if (instance instanceof UISWTInstance) {
            		swtInstance = null;
            	}
            }
        };
		ui.addUIListener(uiListener);
				
		long timeoutMS = this.keyTimeoutDays * 24L * 60L * 60L * 1000L;				
		List<Object> keyList = Generator.GeneratePublisherKey(timeoutMS);		
		if (keyList != null) {
			try {
				prodSig = (byte[])keyList.get(0);
				pubKey 	= (TimedPublicKey) keyList.get(1);
				privKey = (PrivateKey) keyList.get(2);
			} catch (ClassCastException e) {
				e.getStackTrace();
			}
		}		
		
		// Logging related
		BasicConfigurator.configure();

		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.HyperNode").setLevel(Level.ERROR);		
		
		// Set the XML-RPC logger to log only warnings and up.
		org.apache.log4j.Logger.getLogger(
			"org.apache.xmlrpc.server.XmlRpcStreamServer").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.XmlRPCClient").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.GetKClosestObjects").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.GetKClosestNodes").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.DistKNodesProducer").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.DistKObjectsProducer").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.GetKClosestObjects").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"hyper.plugins.MovieInserter").setLevel(Level.ERROR);
		
		org.apache.log4j.Logger.getLogger(
			"org.cornell.hyper.overlay.GetKClosestObjectsThread").setLevel(Level.ERROR);
		
		pluginInterface.addListener(this);
	}

	public void closedownComplete() {
		logChannel.log("Calling closedown complete");
	}

	public void closedownInitiated() {
		logChannel.log("Calling closedown initiated");
		if (hyperNode != null) {
			hyperNode.stopNode();
			hyperNode = null;
		}
		if (movieInserter != null) {
			movieInserter.stopMovieInserter();
			movieInserter = null;
		}
	}

	@SuppressWarnings("unchecked")
	private void startHyper(int localPort) {
		// Create Entry Container
		entryContainer = new EntryContainer(HyperNode.getPunct(), HyperNode
				.getCommonWords());

		// Parse the default seed nodes
		String seedNodesStr = getConfig().getPluginStringParameter("seedNodes");
		List<AddressPort> seedAddrs = new ArrayList<AddressPort>();
		String[] seedArray = seedNodesStr.split(" ");
		for (int i = 0; i < seedArray.length; i++) {
			String[] seedPort = seedArray[i].split(":");
			if (seedPort.length != 2) {
				logChannel.log("Invalid seed node: " + seedArray[i]);
				continue;
			}
			try {
				int curPort = Integer.valueOf(seedPort[1].trim()).intValue();
				String curNode = seedPort[0].trim();
				logChannel.log("Adding seed node: " + curNode + ":" + curPort);
				seedAddrs.add(new AddressPort(curNode, curPort));
			} catch (NumberFormatException e) {
				logChannel.log("Invalid port number: " + seedPort[1]);
				continue;
			}
		}

		// Parse the default URLs for NAT checking
		String RawNATString = getConfig().getPluginStringParameter("urls");
		String[] NATStrings = RawNATString.split(" ");
		List<URL> urlList = new ArrayList<URL>();
		for (int i = 0; i < NATStrings.length; i++) {
			String curStr = NATStrings[i].trim();
			if (curStr.equals("")) {
				continue; // Skip on empty NAT string
			}
			try {
				URL serverURL = new URL(curStr);
				logChannel.log("Adding URL: " + serverURL);
				urlList.add(serverURL);
			} catch (MalformedURLException e) {
				logChannel.log("Invalid URL: " + curStr);
			}
		}

		// Grab the default keywords used for bootstrapping
		Set<String> defaultKW = new HashSet<String>();
		Iterator<String> movieIt = HyperNode.getDefaultMovies().iterator();
		while (movieIt.hasNext()) {
			String[] curKey = entryContainer.removeSymbols(movieIt.next());
			ArrayList<String> curList = new ArrayList<String>();
			for (int i = 0; i < curKey.length; i++) {
				curList.add(curKey[i]);
			}
			defaultKW.addAll(entryContainer.normalizeKeywords(curList));
		}

		// Contact the seed nodes to find a unique keyword and
		// additional nodes.
		KeyVal<String, Set<RemoteHyperNode>> curKeyVal = HyperNode
				.findKeyAndSeeds(seedAddrs, defaultKW);
		if (curKeyVal == null) {
			logChannel.log("Cannot access any seed nodes, exiting");
		}

		String keyword = curKeyVal.getKey();
		seedRemoteNodes = curKeyVal.getVal();

		// Start the HyperNode
		hyperNode = HyperNode.defaultServer(entryContainer, keyword,
				seedRemoteNodes, urlList, localPort);
					
		if (hyperNode != null) {
			Set<RemoteHyperNode> tmpSet = new HashSet<RemoteHyperNode>();
			tmpSet.add(hyperNode.getLocal());
			movieInserter = new MovieInserter(entryContainer, tmpSet);
		} else {
			movieInserter = new MovieInserter(entryContainer, seedRemoteNodes);	
		}
		movieInserter.start();
		
		dlManager = pluginInterface.getDownloadManager();
		// Use addListener with the flag notify_of_current_downloads = true
		dlListener = new DownloadManagerListener () {
			public void downloadAdded(Download download) {
				if (prodSig == null || pubKey == null || privKey == null) {					
					return; // The timed key is not created, skip
				}				
				if (download == null) {
					return; // Problem with the download
				}
				Torrent curTorrent = download.getTorrent();
				if (curTorrent == null || curTorrent.isPrivate()) {
					return; // Problem with the torrent or is private
				}	
				if (!curTorrent.isDecentralised() && 
					!curTorrent.isDecentralisedBackupRequested()) {
					// Currently, only these two types of torrents are supported.
					// Adding torrents where backup is enabled but not requested
					// is problematic, because the tracker is probably still up.
					return;
				}				
				try {
					String torrentName = curTorrent.getName();
					String torrentComment = curTorrent.getComment();
					URL magnetURL = curTorrent.getMagnetURI();					
					//curTorrent.setDecentralisedBackupRequested(true);
					//System.out.println("Setting Decentralized Backup Requested - NEW");								
					try {
						// Create the comment string that includes the torrent name
						String commentStr = null;
						if (torrentComment.isEmpty()) {
							commentStr = torrentName;
						} else {
							commentStr = torrentComment + " " + torrentName;
						}
						// Create MovieEntry
						MovieEntry curMovie = new MovieEntry(
								prodSig, pubKey, privKey.getEncoded(),
								commentStr, magnetURL.toString(),
								torrentName);
						// Insert into inserter thread
						movieInserter.newMovie(curMovie);
					} catch (MovieEntryException e) {
						e.printStackTrace(); // Cannot create MovieEntry, skip
					}				
					System.out.println(torrentName + " | " +  torrentComment + " | " 
							+ magnetURL + " | " + curTorrent.getAnnounceURL() + " | " 
							+ curTorrent.getAnnounceURLList() + " | " 
							+ curTorrent.isDecentralisedBackupRequested());
				} catch (TorrentException e) {
					e.getStackTrace();
					// Torrent doesn't have a magnetURL
				}
			}			
			public void downloadRemoved(Download download) {
				// No action required.
			}			
		};		
		dlManager.addListener(dlListener, true);		

		if (hyperNode != null) {
			logChannel.log("Node constructed successfully");
		} else {
			logChannel.log("Cannot start HyperNode, using client mode");
			return;
		}
	}

	public void initializationComplete() {
		logChannel.log("Calling initialization complete");
		int localPort = getConfig().getPluginIntParameter("port");
		logChannel.log("Using local port: " + localPort);
		// Try to talk over UPnP to request the local port
		try {
			PluginManager piManage = pluginInterface.getPluginManager();
			PluginInterface piUPnP = piManage
					.getPluginInterfaceByClass(UPnPPlugin.class);
			if (piUPnP != null) {
				((UPnPPlugin) piUPnP.getPlugin()).addMapping(pluginInterface
						.getPluginName(), false, localPort, true);
			}
		} catch (Throwable e) {
			logChannel.log("Error adding UPnP mapping for: " + localPort);
		}
		startHyper(localPort);

	}
}
