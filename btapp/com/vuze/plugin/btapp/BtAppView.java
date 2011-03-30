package com.vuze.plugin.btapp;

import java.io.*;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentDownloader;
import org.gudy.azureus2.plugins.torrent.TorrentFile;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.json.simple.JSONValue;

import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.util.JSONUtils;

public class BtAppView
	implements UISWTViewEventListener, BtApp
{

	// Rate limiter will count the # of api calls within RATE_LIMITER_SPAN ms,
	// and only after RATE_LIMITER_SPAN ms of API activity.
	// If the api call rate is over RATE_LIMITER_MAX each new API call will
	// sleep for least RATE_LIMITER_SLEEPFOR, until the count goes down to near 0

	// Essentially it means the plugin can suck your CPU for up to 
	// RATE_LIMITER_SPAN before it gets rate limited
	private static final int RATE_LIMITER_MAX = 1000;

	private static final int RATE_LIMITER_SPAN = 10000;

	private static final int RATE_LIMITER_SLEEPFOR = 10;

	private boolean rateLimiterFullSpan = false;

	private LinkedList<Long> rateLimiterList = new LinkedList<Long>();

	private boolean isRateLimiting = false;

	private long accessMode;

	private File basePath;

	private Browser browser;

	private BrowserFunction browserFunction;

	private String btappid = "testBTAPPID";

	private BtAppWebServ btAppWebServ;

	private String jsAjax;

	private String jsBtApp;

	/**
	 * Map of event ids ('torrent') to javascript function that wants to be called
	 */
	private Map<String, String> mapEventToJsFunc = new HashMap<String, String>(0);

	private Map<String, String> mapStash = new HashMap<String, String>();

	private List<String> listOwnedTorrents = new ArrayList<String>();

	private Composite parent;

	private PluginInterface pi;

	private UISWTView swtView;

	private String title;

	private Composite cTopArea;

	private Composite cOptionsArea;

	private Label lblTitle;

	private Label lblRateLimiter;

	private boolean disposeOnFocusOut = true;

	private static String encodeToJavascript(List<?> list) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		sb.append("[ ");
		for (Object val : list) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}

			sb.append(encodeToJavascript(val));
		}
		sb.append(" ]");

		return sb.toString();
	}

	private static String encodeToJavascript(Map<?, ?> map) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		sb.append("{ ");
		for (Object key : map.keySet()) {
			Object val = map.get(key);
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}

			sb.append('\"');
			sb.append(Plugin.jsTextify((String) key));
			sb.append("\": ");
			sb.append(encodeToJavascript(val));
		}
		sb.append(" }");

		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	private static String encodeToJavascript(Object val) {
		if (val instanceof String) {
			return encodeToJavascript((String) val);
		} else if (val instanceof Map) {
			return encodeToJavascript((Map) val);
		} else if (val instanceof Set) {
			return encodeToJavascript((Set) val);
		} else if (val instanceof List) {
			return encodeToJavascript((List) val);
		} else if (val == null) {
			return "null";
		} else {
			return val.toString();
		}
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes"
	})
	private static String encodeToJavascript(Set set) {
		return encodeToJavascript(new ArrayList(set));
	}

	private static String encodeToJavascript(String s) {
		StringBuffer sb = new StringBuffer();
		sb.append('\"');
		sb.append(Plugin.jsTextify(s));
		sb.append('\"');

		return sb.toString();
	}

	protected static Map<String, Object> getAllSettings() {
		Map<String, Object> map = new HashMap<String, Object>();
		String[] keys = {
			"max_downloads",
			"max downloads"
		};
		for (int i = 0; i < keys.length; i += 2) {
			String btKey = keys[i];
			String azKey = keys[i + 1];
			map.put(btKey, COConfigurationManager.getParameter(azKey));
		}
		return map;
	}

	public BtAppView() {
	}

	protected Object api_add_torrent(Object[] args) {
		if (args.length > 1) {
			String arg = (String) args[1];
			File file = new File(arg);
			if (file.exists()) {
				try {
					Torrent torrent = pi.getTorrentManager().createFromBEncodedFile(file);
					if (torrent != null) {
						listOwnedTorrents.add(ByteFormatter.encodeString(torrent.getHash()));

						pi.getDownloadManager().addDownload(torrent);

						Map<String, Object> map = new HashMap<String, Object>();
						map.put("status", 200);
						map.put("message", "success");
						map.put("hash", ByteFormatter.encodeString(torrent.getHash()));
						triggerEvent("torrent", JSONUtils.encodeToJSON(map));
					}
					return true;
				} catch (Exception e) {
					// todo: triggerEvent
				}
			}
			String urlString = UrlUtils.parseTextForURL(arg, true, true);
			if (urlString != null) {
				try {
					URL url = new URL(urlString);
					asyncDownloadURL(url);
				} catch (Exception e) {
					// todo: triggerEvent
					return false;
				}
				return false;
			}
		}
		return false;
	}

	protected String api_events_set(Object[] args) {
		if (args.length > 2) {
			String event = (String) args[1];
			String jsFunc = (String) args[2];
			return setEventFunction(event, jsFunc);
		}
		return "undefined";
	}

	private StringDirect api_not_implemented() {
		// probably doesn't actually throw
		return new StringDirect("throw 'not implemented'");
	}

	private Object api_peer_properties_all(Object[] args) {
		if (args.length < 3) {
			return Collections.EMPTY_MAP;
		}
		String hash40 = (String) args[1];
		String peerid = (String) args[2];

		Download download = getDownload(hash40);
		if (download != null) {
			PeerManager pm = download.getPeerManager();
			if (pm != null) {
				Peer[] peers = pm.getPeers();
				for (Peer peer : peers) {
					if (peer.getState() == Peer.TRANSFERING) {
						if (peerid.equals(ByteFormatter.encodeString(peer.getId()))) {
							return getAllPeerProperties(peer);
						}
					}
				}
			}
		}

		return Collections.EMPTY_MAP;
	}

	private Object api_peer_properties_get(Object[] args) {
		if (args.length < 4) {
			return null;
		}
		String hash40 = (String) args[1];
		String peerid = (String) args[2];
		String key = (String) args[3];

		Download download = getDownload(hash40);
		if (download != null) {
			PeerManager pm = download.getPeerManager();
			if (pm != null) {
				Peer[] peers = pm.getPeers();
				for (Peer peer : peers) {
					if (peer.getState() == Peer.TRANSFERING) {
						if (peerid.equals(ByteFormatter.encodeString(peer.getId()))) {
							return getAllPeerProperties(peer).get(key);
						}
					}
				}
			}
		}

		return null;
	}

	private Object[] api_peer_properties_keys(Object[] args) {
		if (args.length < 3) {
			return new Object[0];
		}
		String hash40 = (String) args[1];
		String peerid = (String) args[2];

		Download download = getDownload(hash40);
		if (download != null) {
			PeerManager pm = download.getPeerManager();
			if (pm != null) {
				Peer[] peers = pm.getPeers();
				for (Peer peer : peers) {
					if (peer.getState() == Peer.TRANSFERING) {
						if (peerid.equals(ByteFormatter.encodeString(peer.getId()))) {
							return getAllPeerProperties(peer).keySet().toArray();
						}
					}
				}
			}
		}

		return new Object[0];
	}

	protected String api_resource(Object[] args) {
		if (args.length > 1) {
			String resource = (String) args[1];
			File file = new File(basePath, resource);
			if (file.exists()) {
				try {
					String fileText = Plugin.jsTextify(FileUtil.readFileAsString(file, -1));
					// IE HACK.. stylesheet link placed into the body via javascript
					// won't run unless there's a script or img tag before it..
					if (fileText.contains("text/css") && fileText.contains("stylesheet")) {
						fileText = Plugin.jsTextify("<script src=\"\"></script>") + fileText;
					}
					return "\"" + fileText + "\"";
				} catch (IOException e) {
				}
			}
		}
		return "\"\"";
	}

	protected Object api_settings_all(Object[] args) {
		StringBuffer sb = new StringBuffer();
		Map<String, Object> allSettings = getAllSettings();
		return allSettings;
	}

	protected Object api_settings_get(Object[] args) {
		Map<String, Object> allSettings = getAllSettings();
		return allSettings.get(args[1]);
	}

	protected Object[] api_settings_keys(Object[] args) {
		Map<String, Object> allSettings = getAllSettings();
		return allSettings.keySet().toArray();
	}

	protected String api_settings_set(Object[] args) {
		// TODO
		return null;
	}

	protected String api_stash_get(Object[] args) {
		if (args.length > 1) {
			synchronized (mapStash) {
				String string = mapStash.get(args[1]);

				return string;
			}
		}
		return null;
	}

	protected Object[] api_stash_keys(Object[] args) {
		synchronized (mapStash) {
			return mapStash.keySet().toArray();
		}
	}

	protected String api_stash_set(Object[] args) {
		if (args.length > 2) {
			synchronized (mapStash) {
				String val = (String) args[2];
				mapStash.put((String) args[1], val);
			}
		}
		return null;
	}

	protected String api_stash_unset(Object[] args) {
		if (args.length > 1) {
			synchronized (mapStash) {
				String key = (String) args[1];
				mapStash.remove((String) args[1]);
			}
		}
		return null;
	}

	protected String api_torrent_all(Object[] args) {
		Map<String, Object> mapMain = new HashMap<String, Object>();
		if ((accessMode & PRIV_READALL) > 0) {
			Download[] downloads = pi.getDownloadManager().getDownloads();

			for (Download download : downloads) {
				Torrent torrent = download.getTorrent();
				if (torrent != null) {
					byte[] hash = torrent.getHash();
					String hash40 = ByteFormatter.encodeString(hash);

					mapMain.put(hash40, buildTorrentJS(download, hash40));
				}
			}
		} else {
			DownloadManager manager = pi.getDownloadManager();
			for (String hash40 : listOwnedTorrents) {
				try {
					Download download = manager.getDownload(ByteFormatter.decodeString(hash40));
					if (download != null) {
						mapMain.put(hash40, buildTorrentJS(download, hash40));
					}
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return encodeToJavascript(mapMain);
	}

	protected String api_torrent_file_all(Object[] args) {
		if (args.length < 2) {
			return null;
		}
		String hash40 = (String) args[1];
		Map<String, Object> map = new HashMap<String, Object>();

		Download download = getDownload(hash40);
		if (download != null) {
			Torrent torrent = download.getTorrent();
			if (torrent != null) {
				DiskManagerFileInfo[] fileInfos = download.getDiskManagerFileInfo();
				TorrentFile[] files = torrent.getFiles();

				for (DiskManagerFileInfo info : fileInfos) {
					int index = info.getIndex();
					map.put("" + index,
							buildTorrentFileJS(hash40, info, files[info.getIndex()]));
				}
			}
		}

		return encodeToJavascript(map);
	}

	protected Object[] api_torrent_file_keys(Object[] args) {
		if (args.length < 2) {
			return new Object[0];
		}
		List<String> list = new ArrayList<String>();
		String hash40 = (String) args[1];

		Download download = getDownload(hash40);
		if (download != null) {
			DiskManagerFileInfo[] fileInfos = download.getDiskManagerFileInfo();

			for (DiskManagerFileInfo info : fileInfos) {
				list.add("" + info.getIndex());
			}
		}

		return list.toArray();
	}

	protected Object api_torrent_file_open(Object[] args) {
		if (args.length < 3) {
			return null;
		}
		String hash40 = (String) args[1];
		int index;
		if (args[2] instanceof Number) {
			index = ((Number) args[2]).intValue();
		} else if (args[2] instanceof String) {
			index = Integer.valueOf((String) args[2]);
		} else {
			return null;
		}

		Download download = getDownload(hash40);
		if (download != null) {
			DiskManagerFileInfo[] fileInfos = download.getDiskManagerFileInfo();

			if (index < fileInfos.length) {
				TorrentUtil.runDataSources(new Object[] {
					fileInfos[index]
				});
			}
		}

		return null;
	}

	private String api_torrent_file_properties_all(Object[] args) {
		if (args.length < 2) {
			return "{ }";
		}
		String hash40 = (String) args[1];
		int index;
		if (args[2] instanceof Number) {
			index = ((Number) args[2]).intValue();
		} else if (args[2] instanceof String) {
			index = Integer.valueOf((String) args[2]);
		} else {
			return "{ }";
		}

		Download download = getDownload(hash40);
		if (download != null) {
			Torrent torrent = download.getTorrent();
			if (torrent != null) {
				DiskManagerFileInfo[] fileInfos = download.getDiskManagerFileInfo();
				TorrentFile[] files = torrent.getFiles();

				if (index < fileInfos.length && index < files.length) {
					return encodeToJavascript(getAllFileProperties(fileInfos[index],
							files[index]));
				}
			}
		}

		return "{ }";
	}

	private Object api_torrent_file_properties_get(Object[] args) {
		if (args.length < 4) {
			return null;
		}
		String hash40 = (String) args[1];
		int index;
		if (args[2] instanceof Number) {
			index = ((Number) args[2]).intValue();
		} else if (args[2] instanceof String) {
			index = Integer.valueOf((String) args[2]);
		} else {
			return null;
		}

		String key = (String) args[3];

		Download download = getDownload(hash40);
		if (download != null) {
			DiskManagerFileInfo fileInfo = download.getDiskManagerFileInfo(index);
			//Incredibly Slow
			//TorrentFile[] files = torrent.getFiles();
			//if (index < fileInfos.length && index < files.length) {

			if (fileInfo != null) {
				return getFileProperty(key, fileInfo);
			}
		}

		return null;
	}

	private Object[] api_torrent_file_properties_keys(Object[] args) {
		if (args.length < 2) {
			return new Object[0];
		}
		String hash40 = (String) args[1];
		int index;
		if (args[2] instanceof Number) {
			index = ((Number) args[2]).intValue();
		} else if (args[2] instanceof String) {
			index = Integer.valueOf((String) args[2]);
		} else {
			return new Object[0];
		}

		Download download = getDownload(hash40);
		if (download != null) {
			Torrent torrent = download.getTorrent();
			if (torrent != null) {
				DiskManagerFileInfo[] fileInfos = download.getDiskManagerFileInfo();
				TorrentFile[] files = torrent.getFiles();

				if (index < fileInfos.length && index < files.length) {
					return getAllFileProperties(fileInfos[index], files[index]).keySet().toArray();
				}
			}
		}

		return new Object[0];
	}

	protected Object[] api_torrent_keys(Object[] args) {
		List<String> list = new ArrayList<String>();
		if ((accessMode & PRIV_READALL) > 0) {
			Download[] downloads = pi.getDownloadManager().getDownloads();

			for (Download download : downloads) {
				Torrent torrent = download.getTorrent();
				if (torrent != null) {
					byte[] hash = torrent.getHash();
					String hashID = ByteFormatter.encodeString(hash);
					list.add(hashID);
				}
			}
		} else {
			DownloadManager manager = pi.getDownloadManager();
			for (String hash40 : listOwnedTorrents) {
				try {
					Download download = manager.getDownload(ByteFormatter.decodeString(hash40));
					if (download != null) {
						list.add(hash40);
					}
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return list.toArray();
	}

	private String api_torrent_peer_all(Object[] args) {
		if (args.length < 2) {
			return "{ }";
		}
		Map<String, Object> map = new HashMap<String, Object>();
		String hash40 = (String) args[1];
		Download download = getDownload(hash40);
		if (download != null) {
			PeerManager pm = download.getPeerManager();
			if (pm != null) {
				Peer[] peers = pm.getPeers();
				for (Peer peer : peers) {
					if (peer.getState() == Peer.TRANSFERING) {
						String key = ByteFormatter.encodeString(peer.getId());
						map.put(key, buildPeerJS(hash40, peer));
					}
				}
			}
		}

		return encodeToJavascript(map);
	}

	private Object[] api_torrent_peer_keys(Object[] args) {
		if (args.length < 2) {
			return new Object[0];
		}
		List<String> list = new ArrayList<String>();
		String hash40 = (String) args[1];
		Download download = getDownload(hash40);
		if (download != null) {
			PeerManager pm = download.getPeerManager();
			if (pm != null) {
				Peer[] peers = pm.getPeers();
				for (Peer peer : peers) {
					if (peer.getState() == Peer.TRANSFERING) {
						list.add(ByteFormatter.encodeString(peer.getId()));
					}
				}
			}
		}

		return list.toArray();
	}

	protected String api_torrent_properties_all(Object[] args) {
		if (args.length < 2) {
			return null;
		}
		String hash40 = (String) args[1];

		Map<String, Object> properties = getAllDownloadProperties(hash40);

		return JSONUtils.encodeToJSON(properties);
	}

	protected Object api_torrent_properties_get(Object[] args) {
		if (args.length < 3) {
			return null;
		}
		String hash40 = (String) args[1];
		String key = (String) args[2];
		Map<String, Object> properties = getAllDownloadProperties(hash40);
		Object val = properties.get(key);
		return val;
	}

	protected Object[] api_torrent_properties_keys(Object[] args) {
		if (args.length < 2) {
			return new Object[0];
		}
		String hash40 = (String) args[1];

		Map<String, Object> properties = getAllDownloadProperties(hash40);

		return properties.keySet().toArray();
	}

	protected void asyncDownloadURL(final URL url) {
		pi.getUtilities().createThread("asyncDownload", new Runnable() {
			public void run() {
				syncDownloadURL(url);
			}
		});
	}

	private StringDirect buildPeerJS(String hash40, Peer peer) {
		return new StringDirect("(new vzPeer(this, '" + hash40 + "', '"
				+ Plugin.jsTextify(ByteFormatter.encodeString(peer.getId())) + "'))");
	}

	private StringDirect buildTorrentFileJS(String hash40,
			DiskManagerFileInfo info, TorrentFile file) {
		return new StringDirect("(new vzTorrentFile(this, '" + hash40 + "', "
				+ info.getIndex() + ", \"" + Plugin.jsTextify(file.getName()) + "\"))");
	}

	private StringDirect buildTorrentJS(Download download, String hash40) {
		return new StringDirect("(new vzTorrent('" + hash40 + "'))");
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		int type = event.getType();
		switch (type) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				pi = swtView.getPluginInterface();
				// not sure if adding a 'torrent' event to an app with PRIV_READALL
				// should send events for all new torrents.
				//pi.getDownloadManager().addListener(new DownloadManagerListener() {
				//	public void downloadAdded(Download download) {
				//	}
				//
				//	public void downloadRemoved(Download download) {
				//	}
				//});
				if (basePath != null) {
					loadBTAPPfile(basePath);
				}
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				parent = (Composite) event.getData();
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				loadApp(parent);
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				if (event.getData() instanceof File) {
					basePath = (File) event.getData();
					log("Got " + basePath);
				}
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				if (disposeOnFocusOut) {
					unloadApp();
				}
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				unloadApp();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				reduceRateLimiterList();

				refreshRateLimiterDisplay();
				break;

		}
		return true;
	}

	private void refreshRateLimiterDisplay() {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				if (lblRateLimiter == null || lblRateLimiter.isDisposed()) {
					return;
				}
				String s = "";
				synchronized (rateLimiterList) {

					if (rateLimiterList.size() > 1) {
						long span = SystemTime.getCurrentTime()
								- rateLimiterList.getFirst();

						if (span > 0) {
							s = "api: " + (rateLimiterList.size() * 1000 / span) + "/s";
						}
					}
				}

				lblRateLimiter.setText(s);
			}
		});
	}

	public void executeJS(final String js) {
		if (browser == null) {
			return;
		}
		Utils.execSWTThread(new Runnable() {
			public void run() {
				if (browser == null || browser.isDisposed()) {
					return;
				}
				//System.out.println("Execute: " + js);
				browser.execute(js);
			}
		});
	}

	private Map<String, Object> getAllDownloadProperties(String hash40) {
		Map<String, Object> map = new HashMap<String, Object>();

		try {
			Download download = getDownload(hash40);
			if (download != null) {

				map.put("name", Plugin.jsTextify(download.getName()));

				map.put("trackers", Collections.EMPTY_LIST);

				String status;
				int state = download.getState();
				if (state == Download.ST_DOWNLOADING) {
					status = "started";
				} else if (state == Download.ST_SEEDING) {
					status = "seeding";
				} else {
					status = "paused";
				}
				map.put("status", status);

				DownloadStats stats = download.getStats();
				PeerManager pm = download.getPeerManager();
				map.put("remaining", stats.getRemaining());

				Torrent torrent = download.getTorrent();
				if (torrent != null) {
					map.put("size", torrent.getSize());
				}

				map.put("downloaded", stats.getDownloaded());
				map.put("uploaded", stats.getUploaded());
				// One spec example shows progress from 0 - 1000
				// Another spec example shows progress as a percentage float (0.5)
				// Seems most assume 0 - 1000
				map.put("progress", stats.getDownloadCompleted(true));
				long etaSecs = stats.getETASecs();
				map.put("eta", etaSecs > 0 ? etaSecs : 0);
				map.put("ratio", stats.getShareRatio() / 1000.0);

				map.put("peers_connected", pm == null ? 0
						: pm.getStats().getConnectedLeechers());
				map.put("seeds_connected", pm == null ? 0
						: pm.getStats().getConnectedSeeds());

				map.put("availability", stats.getAvailability());
				map.put("queue_order", download.getIndex());

				map.put("download_url", "http://unknown.url/");

				map.put("seed_time", stats.getSecondsOnlySeeding());
				map.put("upload_speed", stats.getUploadAverage());
				map.put("download_speed", stats.getDownloadAverage());

				DownloadScrapeResult scrapeResult = download.getLastScrapeResult();
				map.put("peers_in_swarm", scrapeResult.getNonSeedCount());
				map.put("seeds_in_swarm", scrapeResult.getSeedCount());

				map.put("upload_limit", download.getUploadRateLimitBytesPerSecond());
				map.put("download_limit", download.getDownloadRateLimitBytesPerSecond());
				map.put("seed_ratio", 1); // ???

				map.put("label", download.getCategoryName());

				/*
				trackers: ['tracker1', 'tracker2'] // list
				superseed: enabled // not_allowed/disabled/enabled
				dht // not_allowed/disabled/enabled
				pex // not_allowed/disabled/enabled
				seed_override // not_allowed/disabled/enabled
				seed_ratio: 0.1 // percentage
				ulslots // maximum upload slots
				rss_feed_url: 'rss://rss.utorrent.com'
				 */
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return map;
	}

	private Map<String, Object> getAllFileProperties(DiskManagerFileInfo info,
			TorrentFile file) {
		Map<String, Object> mapPropertyVals = new HashMap<String, Object>();
		mapPropertyVals.put("name", file.getName());
		mapPropertyVals.put("size", file.getSize());
		mapPropertyVals.put("downloaded", info.getDownloaded());
		mapPropertyVals.put("priority", info.getNumericPriorty());

		return mapPropertyVals;
	}

	private Object getFileProperty(String key, DiskManagerFileInfo info) {
		info.getLength();
		if (key.equals("name")) {
			return info.getFile().getName();
		}
		if (key.equals("size")) {
			return info.getLength();
		}
		if (key.equals("downloaded")) {
			return info.getDownloaded();
		}
		if (key.equals("priority")) {
			return info.getNumericPriorty();
		}

		return null;
	}

	private Map<String, Object> getAllPeerProperties(Peer peer) {
		Map<String, Object> mapPropertyVals = new HashMap<String, Object>();
		// NO SPEC
		mapPropertyVals.put("location", "what's this");
		mapPropertyVals.put("ip", peer.getIp());
		mapPropertyVals.put("client", peer.getClient());
		// NO SPEC, assuming it's "UDP","TCP" or something, but our vuze Peer
		// object doesn't have access to that anyway.. :(
		//mapPropertyVals.put("connection_type", peer.getSupportedMessages());

		// rest aren't in the spec, just guessing at this point..
		mapPropertyVals.put("port", peer.getPort());
		mapPropertyVals.put("progress", peer.getPercentDoneInThousandNotation());

		return mapPropertyVals;
	}

	public String getAppId() {
		return btappid;
	}

	public void insertAjaxProxy() {
		log("Inserting AJAX proxy");
		executeJS(jsAjax);
	}

	public void insertBtAppJS() {
		log("Inserting btapp js");
		executeJS(jsBtApp);
	}

	private void loadApp(Composite parent) {
		if (btAppWebServ != null) {
			return;
		}
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = layout.verticalSpacing = layout.marginHeight = layout.marginWidth = 0;
		parent.setLayout(layout);

		loadBTAPPfile(basePath);

		try {
			btAppWebServ = new BtAppWebServ(pi, basePath, this);
		} catch (Exception e1) {
			log("WebServ init failed: " + e1.toString());
			return;
		}

		FormData fd;
		cTopArea = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		cTopArea.setLayoutData(gd);
		FormLayout formLayout = new FormLayout();
		cTopArea.setLayout(formLayout);

		lblTitle = new Label(cTopArea, SWT.WRAP);
		lblTitle.setText(title);

		Button btnOptions = new Button(cTopArea, SWT.TOGGLE);
		btnOptions.setText("Options >>");

		cOptionsArea = new Composite(cTopArea, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(btnOptions);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		fd.height = 1;
		cOptionsArea.setLayoutData(fd);
		cOptionsArea.setLayout(new FormLayout());
		cOptionsArea.setVisible(false);

		fd = new FormData();
		fd.bottom = new FormAttachment(btnOptions, 0, SWT.CENTER);
		lblTitle.setLayoutData(fd);

		fd = new FormData();
		fd.left = null;
		fd.right = new FormAttachment(100, -1);
		btnOptions.setLayoutData(fd);

		btnOptions.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					showOptionsArea();
				} else {
					hideOptionsArea();
				}
			}
		});

		Button btnDisposeOnLostSelection = new Button(cOptionsArea, SWT.CHECK);
		btnDisposeOnLostSelection.setText("Dispose of browser when switching away from view");

		lblRateLimiter = new Label(cOptionsArea, SWT.NONE);
		btnDisposeOnLostSelection.setSelection(disposeOnFocusOut);
		btnDisposeOnLostSelection.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event event) {
				disposeOnFocusOut = ((Button) event.widget).getSelection();
			}
		});

		fd = new FormData();
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		lblRateLimiter.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(lblRateLimiter);
		btnDisposeOnLostSelection.setLayoutData(fd);

		browser = new Browser(parent, SWT.NONE);
		if (browser.evaluate("return 0") == null) {
			// Some browsers (IE) won't do javascript until about:blank is loaded
			// Other browsers (Safari) mess up on BrowserFunction when set to about:blank
			browser.setUrl("about:blank");
		}
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));

		// load first from class dir, 2nd from plugin app dir
		File dirJS = new File(pi.getPluginDirectoryName(), "js");
		try {
			InputStream is = BtAppView.class.getResourceAsStream("/js/btapp.js");
			jsBtApp = FileUtil.readInputStreamAsString(is, -1);
			is.close();
		} catch (Exception e) {
		}
		if (jsBtApp == null) {
			try {
				jsBtApp = FileUtil.readFileAsString(new File(dirJS, "btapp.js"), -1);
			} catch (IOException e) {
			}
		}
		try {
			InputStream is = BtAppView.class.getResourceAsStream("/js/jquery.ajaxproxy.js");
			jsAjax = FileUtil.readInputStreamAsString(is, -1);
			is.close();
		} catch (Exception e) {
		}
		if (jsAjax == null) {
			try {
				jsAjax = FileUtil.readFileAsString(new File(dirJS,
						"jquery.ajaxproxy.js"), -1);
			} catch (IOException e) {
			}
		}

		browserFunction = new BrowserFunction(browser, "bt2vuze") {

			public Object function(Object[] arguments) {
				try {
					if (arguments.length == 0) {
						log("Func null");
						return null;
					}
					if (!(arguments[0] instanceof String)) {
						log("Func " + arguments[0]);
						return null;
					}
					String func = (String) arguments[0];
					Object result = processFunction(func, arguments);
					String arguementsString = Arrays.toString(arguments);
					String s;
					if (result == null) {
						s = null;
					} else if (result instanceof Object[]) {
						s = Arrays.toString((Object[]) result);
					} else {
						s = result.toString();
					}
					log("Func "
							+ (arguementsString.length() > 200 ? (arguementsString.length()
									+ ": " + arguementsString.substring(0, 200))
									: arguementsString)
							+ ": "
							+ (s == null ? "null"
									: (s.length() > 2024 ? (s.length() + ": " + s.substring(0,
											2024).replaceAll("[\r\n]", "\n\t")) : s.replaceAll(
											"[\r\n]", "\n------\t"))));
					return result;
				} catch (Exception e) {
					log("BrowserFunction error: " + e.toString());
				}
				return super.function(arguments);
			}

		};

		browser.setUrl("http://127.0.0.1:" + btAppWebServ.getPort() + "/index.html");
		browser.addLocationListener(new LocationListener() {
			public void changed(LocationEvent event) {
				synchronized (rateLimiterList) {
					setRateLimiting(false);
					rateLimiterList.clear();
				}
				insertBtAppJS();
			}

			public void changing(LocationEvent event) {
				if (!event.location.startsWith("http://127.0.0.1:")) {
					log("LAUNCHING " + event.location);
					Utils.launch(event.location);
					event.doit = false;
				} else {
					log("URL CHANGING TO " + event.location);
				}
			}
		});

		browser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				event.required = true;
				final Shell shell = ShellFactory.createMainShell(SWT.SHELL_TRIM);
				shell.setLayout(new FillLayout());
				final Browser subBrowser = new Browser(shell,
						Utils.getInitialBrowserStyle(SWT.NONE));
				shell.open();
				event.browser = subBrowser;
				subBrowser.addTitleListener(new TitleListener() {
					public void changed(TitleEvent event) {
						shell.setText(event.title);
					}
				});
				subBrowser.addLocationListener(new LocationListener() {
					public void changed(LocationEvent event) {
					}

					public void changing(LocationEvent event) {
						log("Popup: URL: " + event.location);
					}
				});
				shell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						subBrowser.dispose();
					}
				});

			}
		});

		browser.getParent().layout(true);
	}

	protected void showOptionsArea() {
		//FormData fd = new FormData();
		FormData fd = (FormData) cOptionsArea.getLayoutData();
		fd.height = -1;
		cOptionsArea.setLayoutData(fd);
		cTopArea.getParent().layout(true, true);
		cOptionsArea.setVisible(true);
	}

	protected void hideOptionsArea() {
		FormData fd = (FormData) cOptionsArea.getLayoutData();
		fd.height = 1;
		cOptionsArea.setLayoutData(fd);
		cOptionsArea.setVisible(false);
		cTopArea.getParent().layout(true, true);
	}

	private void loadBTAPPfile(final File basePath) {
		File file = new File(basePath, "btapp");
		btappid = file.getParentFile().getName();
		log("Our BtAppId is " + btappid);
		if (!file.exists()) {
			return;
		}
		try {
			String s = FileUtil.readFileAsString(file, 0xffff);
			String[] lines = s.split("[\r\n]+");
			for (String line : lines) {
				String[] sections = line.split(":", 2);
				if (sections.length == 2) {
					String id = sections[0].toLowerCase();
					String val = sections[1].trim();
					if (id.equals("access")) {
						log("accessMode value " + val);
						if (val.equalsIgnoreCase("list_restricted")) {
							accessMode = PRIV_READALL;
						}
						if (val.equalsIgnoreCase("restricted")) {
							accessMode = PRIV_READALL | PRIV_WRITEALL;
						}
					} else if (id.equals("name")) {
						title = val;
						if (swtView != null) {
							swtView.setTitle(val);
						}
						// lame way to give the plugin a name in the menu
						pi.getPluginconfig().setPluginParameter(
								"Views.plugins.btapp." + btappid + ".title", title);
					}
				}
			}

		} catch (IOException e) {
		}

		try {
			File stashFile = pi.getPluginconfig().getPluginUserFile(
					getAppId() + ".stash");
			Reader reader = new InputStreamReader(new FileInputStream(stashFile),
					"utf-8");
			Object object = JSONValue.parse(reader);
			if (object instanceof Map) {
				Map savedStash = (Map) object;
				if (savedStash.containsKey("torrents")
						&& savedStash.containsKey("stash")) {
					listOwnedTorrents.addAll((Collection) savedStash.get("torrents"));
					mapStash.putAll((Map) savedStash.get("stash"));
				} else {
					mapStash.putAll(savedStash);
				}
			}
		} catch (Exception e) {
			log("StashLoad: " + e.getMessage());
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					File fileIcon = new File(basePath, "icon.png");
					if (!fileIcon.exists()) {
						fileIcon = new File(basePath, "icon.bmp");
						if (!fileIcon.exists()) {
							return;
						}
					}

					Display display = Display.getCurrent();

					InputStream is = new FileInputStream(fileIcon);
					Image image = new Image(display, is);
					is.close();
					String id = "btapp." + btappid + ".icon";
					ImageLoader.getInstance().addImage(id, image);

					UIFunctionsSWT uiFunctionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
					MultipleDocumentInterfaceSWT mdi = uiFunctionsSWT.getMDISWT();
					MdiEntry entry = mdi.getEntry(swtView.getViewID());
					entry.setImageLeftID(id);
				} catch (Exception e) {
				}
			}
		});
	}

	public void log(String string) {
		StringBuffer sb = new StringBuffer();
		sb.append(title);
		sb.append(":");
		sb.append(rateLimiterList.size());
		if (isRateLimitingAndFull()) {
			sb.append("*");
		}
		sb.append("] ");
		sb.append(string);
		Plugin.log(sb.toString());
	}

	protected Object processFunction(String func, Object[] args) {
		try {
			synchronized (rateLimiterList) {
				reduceRateLimiterList();
				int count = rateLimiterList.size();
				long now = pi.getUtilities().getCurrentSystemTime();
				if (count <= 1) {
					if (isRateLimitingAndFull()) {
						setRateLimiting(false);
						log("Rate Limiting OFF");
					}
				} else if (count >= RATE_LIMITER_MAX
						&& (now - rateLimiterList.getFirst()) > (RATE_LIMITER_SPAN * 0.95)) {
					if (!isRateLimitingAndFull()) {
						setRateLimiting(true);
						log("Rate Limiting ON");
					}
				}
				rateLimiterList.add(now);

				if (isRateLimitingAndFull()) {
					Display display = Display.getDefault();
					while (!display.isDisposed() && display.readAndDispatch()) {
					}
					long now2 = pi.getUtilities().getCurrentSystemTime();
					long sleepFor = RATE_LIMITER_SLEEPFOR - (now2 - now);
					if (sleepFor > 0) {
						Thread.sleep(sleepFor);
					}
				}
			}
		} catch (Exception e) {
		}

		String lfunc = func.toLowerCase();
		Object result = null;
		if (lfunc.equals("peer_id")) {
			// We generate peer_id per torrent.. so just fudge this
			// Maybe the specs mean this should be an unique user/app id?
			result = "VuzeConstantPeerID";

		} else if (lfunc.equals("settings.all")) {
			result = api_settings_all(args);

		} else if (lfunc.equals("settings.keys")) {
			result = api_settings_keys(args);

		} else if (lfunc.equals("settings.get")) {
			result = api_settings_get(args);

		} else if (lfunc.equals("settings.set")) {
			result = api_settings_set(args);

		} else if (lfunc.equals("events.set")) {
			result = api_events_set(args);

		} else if (lfunc.equals("add.torrent")) {
			result = api_add_torrent(args);

		} else if (lfunc.equals("add.rss_feed")) {
			result = api_not_implemented();

		} else if (lfunc.equals("add.rss_filter")) {
			result = api_not_implemented();

		} else if (lfunc.equals("stash.set")) {
			result = api_stash_set(args);

		} else if (lfunc.equals("stash.unset")) {
			result = api_stash_unset(args);

		} else if (lfunc.equals("stash.get")) {
			result = api_stash_get(args);

		} else if (lfunc.equals("stash.keys")) {
			result = api_stash_keys(args);

		} else if (lfunc.equals("torrent.all")) {
			result = api_torrent_all(args);

		} else if (lfunc.equals("torrent.keys")) {
			result = api_torrent_keys(args);

		} else if (lfunc.equals("torrent.recheck")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.remove")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.pause")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.stop")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.unpause")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.start")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.properties.all")) {
			result = api_torrent_properties_all(args);

		} else if (lfunc.equals("torrent.properties.get")) {
			result = api_torrent_properties_get(args);

		} else if (lfunc.equals("torrent.properties.set")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.properties.keys")) {
			result = api_torrent_properties_keys(args);

		} else if (lfunc.equals("torrent.file.all")) {
			result = api_torrent_file_all(args);

		} else if (lfunc.equals("torrent.file.keys")) {
			result = api_torrent_file_keys(args);

		} else if (lfunc.equals("torrent.file.open")) {
			result = api_torrent_file_open(args);

		} else if (lfunc.equals("torrent.file.get_data")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.file.properties.all")) {
			result = api_torrent_file_properties_all(args);

		} else if (lfunc.equals("torrent.file.properties.keys")) {
			result = api_torrent_file_properties_keys(args);

		} else if (lfunc.equals("torrent.file.properties.get")) {
			result = api_torrent_file_properties_get(args);

		} else if (lfunc.equals("torrent.file.properties.set")) {
			result = api_not_implemented();

		} else if (lfunc.equals("torrent.peer.all")) {
			result = api_torrent_peer_all(args);

		} else if (lfunc.equals("torrent.peer.keys")) {
			result = api_torrent_peer_keys(args);

		} else if (lfunc.equals("peer.properties.all")) {
			result = api_peer_properties_all(args);

		} else if (lfunc.equals("peer.properties.keys")) {
			result = api_peer_properties_keys(args);

		} else if (lfunc.equals("peer.properties.get")) {
			result = api_peer_properties_get(args);

		} else if (lfunc.equals("peer.properties.set")) {
			result = api_not_implemented();

		} else if (lfunc.equals("resource")) {
			result = api_resource(args);
			//browser.execute(jsAjax);
		}

		return result;
	}

	private void reduceRateLimiterList() {
		synchronized (rateLimiterList) {
			if (pi == null) {
				return;
			}
			long gracePeriod = pi.getUtilities().getCurrentSystemTime()
					- RATE_LIMITER_SPAN;
			if (rateLimiterList.size() > 0) {
				Long first = rateLimiterList.getFirst();
				if (first != null && first < gracePeriod) {
					setFullSpan(true);

					while (first != null && first < gracePeriod) {
						rateLimiterList.removeFirst();
						first = rateLimiterList.size() == 0 ? null
								: rateLimiterList.getFirst();
					}
				}
			} else {
				setFullSpan(false);
			}
		}
	}

	private String setEventFunction(String event, String jsFunc) {
		synchronized (mapEventToJsFunc) {
			return mapEventToJsFunc.put(event, jsFunc);
			//log("add " + jsFunc);
		}
	}

	protected void syncDownloadURL(final URL url) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			TorrentDownloader urlDownloader = pi.getTorrentManager().getURLDownloader(
					url);
			Torrent torrent = urlDownloader.download();
			pi.getDownloadManager().addDownload(torrent);

			listOwnedTorrents.add(ByteFormatter.encodeString(torrent.getHash()));

			map.put("status", 200);
			map.put("message", "success");
			map.put("url", url.toExternalForm());
			map.put("hash", ByteFormatter.encodeString(torrent.getHash()));
		} catch (Exception e) {
			map.put("status", 0); // make up number
			map.put("message", Plugin.jsTextify(e.toString()));
			if (url != null) {
				map.put("url", url.toExternalForm());
			}
		}
		triggerEvent("torrent", JSONUtils.encodeToJSON(map));
	}

	public void triggerEvent(final String event, final String result) {
		synchronized (mapEventToJsFunc) {
			log("Trigger event " + event + " with " + result);
			String jsFunc = mapEventToJsFunc.get(event);
			if (jsFunc != null) {
				executeJS("(" + jsFunc + ").call(this, " + result + ")");
			} else {
				log("  failed.. no Function to call");
			}
		}
	}

	private void unloadApp() {
		synchronized (rateLimiterList) {
			setRateLimiting(false);
			rateLimiterList.clear();
		}

		if (btAppWebServ != null) {
			btAppWebServ.delete();
			btAppWebServ = null;

			// We don't use pi.getUtilities().writeResilientBEncodedFile(..) because
			// it has a history of not working with certain non-ASCII map keys
			File stashFile = pi.getPluginconfig().getPluginUserFile(
					getAppId() + ".stash");
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("stash", mapStash);
			map.put("torrents", listOwnedTorrents);
			String json = JSONUtils.encodeToJSON(map);
			FileUtil.writeStringAsFile(stashFile, json);
		}

		listOwnedTorrents.clear();
		mapStash.clear();
		mapEventToJsFunc.clear();

		Utils.execSWTThread(new Runnable() {
			public void run() {
				if (cTopArea != null && !cTopArea.isDisposed()) {
					cTopArea.dispose();
				}
				if (browser != null && !browser.isDisposed()) {
					browser.dispose();
				}
				if (browserFunction != null && !browserFunction.isDisposed()) {
					browserFunction.dispose();
				}
			}
		});
	}

	public void setRateLimiting(boolean isRateLimiting) {
		if (this.isRateLimiting == isRateLimiting) {
			return;
		}
		if (!isRateLimiting) {
			setFullSpan(false);
		}
		this.isRateLimiting = isRateLimiting;
		updateTitle();
	}

	public void updateTitle() {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				if (lblTitle == null || lblTitle.isDisposed()) {
					return;
				}
				String s = title;
				if (isRateLimitingAndFull()) {
					s += " (Rate Limiting to prevent CPU suckage)";
				}
				lblTitle.setText(s);
				lblTitle.getParent().layout();
			}
		});
	}

	public boolean isRateLimitingAndFull() {
		return isRateLimiting && isFullSpan();
	}

	public void setFullSpan(boolean fullSpan) {
		if (this.rateLimiterFullSpan == fullSpan) {
			return;
		}
		this.rateLimiterFullSpan = fullSpan;
		updateTitle();
	}

	public boolean isFullSpan() {
		return rateLimiterFullSpan;
	}

	private Download getDownload(String hash40) {
		try {
			if ((accessMode & PRIV_READALL) > 0) {
				byte[] hashBytes = ByteFormatter.decodeString(hash40);
				return pi.getDownloadManager().getDownload(hashBytes);
			}
			if (listOwnedTorrents.contains(hash40)) {
				byte[] hashBytes = ByteFormatter.decodeString(hash40);
				return pi.getDownloadManager().getDownload(hashBytes);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public class StringDirect
	{
		public String s;

		public StringDirect(String s) {
			this.s = s;
		}

		public String toString() {
			return s;
		}
	}
}
