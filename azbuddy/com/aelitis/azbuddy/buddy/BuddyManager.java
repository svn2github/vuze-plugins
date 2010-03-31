package com.aelitis.azbuddy.buddy;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.buddy.connection.BuddyConnection;
import com.aelitis.azbuddy.buddy.connection.BuddyConnectionHandler;
import com.aelitis.azbuddy.buddy.connection.ConnectionControl;
import com.aelitis.azbuddy.buddy.connection.ConnectionControlHandler;
import com.aelitis.azbuddy.buddy.torrents.TorrentDownloadManager;
import com.aelitis.azbuddy.config.InheritableConfigTree;
import com.aelitis.azbuddy.config.InheritableString;
import com.aelitis.azbuddy.config.UninheritableMap;
import com.aelitis.azbuddy.dht.DDBKeyValuePair;
import com.aelitis.azbuddy.dht.DHTManager;
import com.aelitis.azbuddy.dht.DHTManagerListener;
import com.aelitis.azbuddy.ui.UILogger;
import com.aelitis.azbuddy.utils.ByteArray;


public class BuddyManager {
	
	public final static int MANAGER_NOT_INITIALIZED = 0;
	public final static int MANAGER_INITIALIZED = 1;
	public final static int MANAGER_IS_RUNNING = 2;
	public final static int MANAGER_IS_REGISTERING = 4;
	public final static int MANAGER_IS_REGISTERED = 8;
	
	private final List<Buddy> buddies = new ArrayList<Buddy>();
	private final List<Buddy> buddiesRO = Collections.unmodifiableList(buddies);
	private final List<BuddyManagerListener> listeners = new ArrayList<BuddyManagerListener>();
	
	private final PluginInterface pI;
	private final DHTManager dhtMan;
	private int state;
	private int loopcount;
	
	private int tcpPort;
	private int udpPort;
	private boolean rqCrypt;
	private SEPublicKey identity;
	
	private DDBKeyValuePair selfRegistration; 

	private final ListenerBundle myListener = new ListenerBundle();
	private final class ListenerBundle implements DHTManagerListener, ConfigParameterListener, DownloadManagerListener, ConnectionControlHandler {
		
		// DHT manager listener
		public void dhtUsabilityChanged(boolean isUsable)
		{
			UILogger.log("DHT usability changed to: "+(isUsable?"usable":"not usable"));
		}
		
		public void externalAddressChanged(InetAddress newAddress)
		{
			reRegisterSelf();
		}
		
		public void operationFinished(DDBKeyValuePair handler, int flags)
		{
			if(handler == selfRegistration && (flags & DHTManagerListener.WRITE_QUERY) != 0 )
			{
				state &= ~MANAGER_IS_REGISTERING;
				if((flags & DHTManagerListener.QUERY_DONE) != 0)
					state |= MANAGER_IS_REGISTERED;
			}
		}
		
		// config parameter listener
		public void configParameterChanged(ConfigParameter param)
		{
			rqCrypt = pI.getPluginconfig().getBooleanParameter("network.transport.encrypted.require",false);
			tcpPort = pI.getPluginconfig().getIntParameter("TCP.Listen.Port",-1);
			udpPort = pI.getPluginconfig().getIntParameter("UDP.Listen.Port",-1);
			state &= ~ MANAGER_IS_REGISTERED;
			reRegisterSelf();
		}
		
		// download manager listener		
		public void downloadAdded(Download download)
		{
			for(Buddy i : buddies)
			{
				i.addLocalDownload(download);
			}
		}
		
		public void downloadRemoved(Download download)
		{
			final ByteArray hash = new ByteArray(download.getTorrent().getHash());
			for(Buddy i : buddies)
			{
				i.removeLocalDownload(hash);
			}
		}
		
		
		// connection control handler
		
		public BuddyConnectionHandler matchNewConnection(BuddyConnection conn, SEPublicKey remoteKey)
		{
			UILogger.log("searching buddy");
			for(Buddy i : buddies)
			{
				if(remoteKey.equals(i.getPubKey()))
					return i.newBuddyConnectionAuthenticated(conn);
			}
			UILogger.log("couldn't find matching buddy");
			return null;
		}
	}
	

	
	private TorrentDownloadManager torrentDownloader;
	
	final ConnectionControl connControl;
	
	final InheritableConfigTree managerConfig;
	final UninheritableMap buddyMap;
	final InheritableString myNick;
	
	public BuddyManager()
	{
		pI = BuddyPlugin.getPI();

		state = MANAGER_NOT_INITIALIZED;
		connControl = new ConnectionControl(myListener);
		torrentDownloader = new TorrentDownloadManager();
		
		dhtMan = BuddyPlugin.getDHTMan();
		
		managerConfig = InheritableConfigTree.getInstance(BuddyPlugin.getConfigMan().getRoot(), "BuddyManagerConfig");
		buddyMap = UninheritableMap.getInstance(managerConfig, "BuddyMap");
		myNick = InheritableString.getInstance(managerConfig, "self.nick", true);
	}
	
	public void start()
	{
		if((state & MANAGER_IS_RUNNING) != 0)
			return;
		state |= (MANAGER_INITIALIZED | MANAGER_IS_RUNNING);
		
		identity = BuddyPlugin.getIdentity();
		
		// load necessary objects
		String serializedKey = BuddyUtils.serializePubkey(identity);
		selfRegistration = dhtMan.getManagedDDBKeyValuePair("buddy:"+serializedKey, "self registration ("+serializedKey+")");
		
		Set<String> buddyKeys = buddyMap.getKeySet();
		for(String i : buddyKeys)
		{
			InheritableString buddyKey = buddyMap.get(InheritableString.class, i);
			try
			{
				if(buddyKey != null)
					addBuddy(new Buddy(this,BuddyPlugin.getSecurityManager().decodePublicKey(buddyKey.getBytes())));			
			} catch (Exception e)
			{
				Debug.printStackTrace(e);
			}
		}
		
		
		
		
		connControl.start();

		// listeners come last since they might trigger events
		pI.getPluginconfig().getParameter("TCP.Listen.Port").addConfigParameterListener(myListener);
		pI.getPluginconfig().getParameter("UDP.Listen.Port").addConfigParameterListener(myListener);
		pI.getPluginconfig().getParameter("network.transport.encrypted.require").addConfigParameterListener(myListener);
		pI.getDownloadManager().addListener(myListener);
		myListener.configParameterChanged(null);
		dhtMan.registerListener(myListener);
		
		loopcount = 0;
		new AEThread("BuddyManager",true) {
			public void runSupport()
			{
				mainLoop();
			}
		}.start();
		
		UILogger.log(BuddyUtils.createMagnet(myNick.getString(), identity));
	}
	
	private void mainLoop()
	{
		while((state & MANAGER_IS_RUNNING) != 0)
		{
			updateConfig(false);
			
			// buddy specific tasks
			for(Buddy i : buddies)
			{
				if(loopcount % 60 == 0)
				{
					i.lookupAddress();
					i.connect();
				}
				
				if(loopcount % 5 == 0)
					i.sendPendingTorrents();
			}
			
			if(loopcount % 60 == 0)
			{
				connControl.performConnectionChecks();
			}
				
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			loopcount++;
		}
	}
	
	public void stop()
	{
		state &= ~MANAGER_IS_RUNNING;
		updateConfig(true);
		loopcount = 0;
		pI.getPluginconfig().getParameter("TCP.Listen.Port").removeConfigParameterListener(myListener);
		pI.getPluginconfig().getParameter("UDP.Listen.Port").removeConfigParameterListener(myListener);
		pI.getPluginconfig().getParameter("network.transport.encrypted.require").removeConfigParameterListener(myListener);
		pI.getDownloadManager().removeListener(myListener);
		dhtMan.unregisterListener(myListener);
	}
	
	private void updateConfig(boolean forced)
	{
		if(!forced && (loopcount % 3*60) != 0)
			return;
		
		buddyMap.clear();
		
		for(Buddy i : buddies)
		{
			// using constructor since the map is cleared an thus no collision can occur
			new InheritableString(buddyMap,i.getSerializedPublicKey(),false,i.getPubKey().encodePublicKey());			
		}

		
		BuddyPlugin.getConfigMan().save();
	}
	
	
	/**
	 * call when something that requires republishing to the DDB has changed
	 */
	private void reRegisterSelf()
	{
		// no other registration in progress and required data available?
		if((state & MANAGER_IS_REGISTERING) == 0 && dhtMan.getAddress() != null)
		{
			state &= ~MANAGER_IS_REGISTERED;
			Map toPublish = new HashMap();
			toPublish.put("ip",dhtMan.getAddress().getAddress());
			toPublish.put("tcp",new Integer(tcpPort));
			toPublish.put("udp",new Integer(udpPort));
			toPublish.put("c",new Integer(rqCrypt ? 1 : 0));
			
			try
			{
				byte[] encoded = BEncoder.encode(toPublish);
				state |= MANAGER_IS_REGISTERING;
				selfRegistration.putValue(encoded);
			} catch (Exception e)
			{
				Debug.printStackTrace(e);
			}			
		}
	}
	
	
	private void addBuddy(Buddy toAdd)
	{
		if(toAdd == null || buddies.contains(toAdd) || identity.equals(toAdd.getPubKey()))
			return;
		UILogger.log("adding new buddy: "+toAdd.getPermaNick()+" pubkey:"+BuddyUtils.serializePubkey(toAdd.getPubKey()));

		// add current set of downloads
		final Download[] downloads = pI.getDownloadManager().getDownloads();
		for(Download i : downloads)
			toAdd.addLocalDownload(i);
		
		toAdd.bless();
		
		// TODO synchronization needed?
		buddies.add(toAdd);
				
		// notify listeners
		for(BuddyManagerListener i : listeners)
			i.buddyAdded(toAdd);
	}
	
	public void removeBuddy(Buddy toRemove)
	{
		if(toRemove == null || !buddies.contains(toRemove))
			return;
		buddies.remove(toRemove);
		toRemove.destroy();
		
		// notify listeners
		for(BuddyManagerListener i : listeners)
			i.buddyRemoved(toRemove);
	}
	
	
	public void addBuddyByMagnet(String magnet)
	{
		Buddy newBuddy = createBuddyFromMagnet(magnet);
		if(newBuddy == null)
			return;
		addBuddy(newBuddy);
	}
	
	public void registerListener(BuddyManagerListener listener)
	{ // TODO sync?
		listeners.add(listener);
	}
	
	public void unregisterListener(BuddyManagerListener listener)
	{
		listeners.remove(listener);
	}
	
	public String getPubkey()
	{
		return BuddyUtils.serializePubkey(identity);
	}

	public String getNickName()
	{
		return myNick.getString();
	}
	
	public void setLocalNick(String newNick)
	{
		if(!newNick.equals(myNick.getString()))
		{
			myNick.setMonitoringState(false);
			myNick.setString(newNick);
			for(Buddy i : buddies)
			{
				i.sendNickChange(newNick);
			}
		}
	}
	
	public List<Buddy> getBuddies() { return buddiesRO;	}
	
	
	InheritableConfigTree getConfig()
	{
		return managerConfig;
	}
	
	public TorrentDownloadManager getTorrentDownloader()
	{
		return torrentDownloader;
	}

	private Buddy createBuddyFromMagnet(String magnet)
	{
		Matcher regex = Pattern.compile("^magnet:\\?xt=urn:azbd:([A-Z0-9-]+)&dn=(.*)$").matcher(magnet);
		if(!regex.matches())
			return null;
	
		SEPublicKey buddyKey = BuddyUtils.deserializePubKey(regex.group(1));
	
		String nickname = null;
		try
		{
			nickname = URLDecoder.decode(regex.group(2), "UTF-8");
		} catch (UnsupportedEncodingException e)
		{
			Debug.printStackTrace(e);
		}
	
		if(nickname != null && buddyKey != null)
		{
			Buddy newBud = new Buddy(this, buddyKey);
			newBud.setPermaNick(nickname);
			return newBud;
		} else
			return null;
	}

}
