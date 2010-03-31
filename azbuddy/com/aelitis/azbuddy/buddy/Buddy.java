package com.aelitis.azbuddy.buddy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.buddy.connection.BuddyChatMsg;
import com.aelitis.azbuddy.buddy.connection.BuddyConnection;
import com.aelitis.azbuddy.buddy.connection.BuddyConnectionHandler;
import com.aelitis.azbuddy.buddy.connection.LocalTransferListener;
import com.aelitis.azbuddy.buddy.connection.LocalTransferRequest;
import com.aelitis.azbuddy.buddy.connection.RemoteTransferRequest;
import com.aelitis.azbuddy.buddy.torrents.BuddyTorrent;
import com.aelitis.azbuddy.buddy.torrents.TorrentUtils;
import com.aelitis.azbuddy.config.InheritableConfigTree;
import com.aelitis.azbuddy.config.InheritableString;
import com.aelitis.azbuddy.dht.DDBKeyValuePair;
import com.aelitis.azbuddy.dht.DHTManagerListener;
import com.aelitis.azbuddy.ui.UILogger;
import com.aelitis.azbuddy.utils.ByteArray;
import com.aelitis.azbuddy.utils.Utils;

public class Buddy {

	public static final int BUDDY_STATE_NOT_MANAGED = 0;
	public static final int BUDDY_STATE_LOOKUP_PENDING = 1;
	public static final int BUDDY_STATE_ADDRESS_KNOWN = 2;
	public static final int BUDDY_STATE_AUTHENTICATED = 3;
	public static final int BUDDY_STATE_CONNECTED = 4;
	

	/**
	 * if set the buddy won't be saved to disk
	 */
	public static final int BUDDY_FLAGS_EPHEMERAL_BUDDY = 1;

	/**
	 * i trust this buddy permanently
	 */
	public static final int BUDDY_FLAGS_IS_AUTHORIZED = 2;

	/**
	 * buddy trusts me permanently
	 */
	public static final int BUDDY_FLAGS_IS_AUTHORIZING = 4;

	public static final int BUDDY_FLAGS_SEND_TORRENTS_ALLOW	= 8;
	public static final int BUDDY_FLAGS_SEND_TORRENTS_DENY	= 16;
	
	
	//private final static long ADDRESS_LOOKUP_INTERVAL = 10*60*1000L;
	private final static long ADDRESS_LOOKUP_INTERVAL = 2*60*1000L;

	private String remoteNick;

	private int state;
	private int flags;
	private BuddyManager manager;
	private BuddyConnection connection = null;
	private final class ListenerBundle implements BuddyConnectionHandler, DHTManagerListener {
		
		// buddy connection handler
		public void stateChanged()
		{
			if(connection.isFlagSet(BuddyConnection.DESTROYED))
				buddyDisconnected();
			else if(connection.isFlagSet(BuddyConnection.CONN_ESTABLISHED))
				connectionEstablished();
		}
		public void chatMessageReceived(BuddyChatMsg msg) {	Buddy.this.chatMessageReceived(msg); }
		public void torrentListMsgReceived(Map downloads) { processRemoteTorrentList(downloads); }
		public void torrentReqReceived(RemoteTransferRequest ttReq) { processTorrentReq(ttReq); };
		
		// dht manager stuff
		
		public void dhtUsabilityChanged(boolean isUsable) {	}
		public void externalAddressChanged(InetAddress newAddress) { }
		public void operationFinished(DDBKeyValuePair handler, int flags)
		{
			if(handler == buddyRegistration)
				processLookupResult(flags);
		}
	}
	private final ListenerBundle myListeners = new ListenerBundle();

	private final List<BuddyListener> listeners = new ArrayList<BuddyListener>();
	private final HashMap<ByteArray,BuddyTorrent> torrentsFromBuddy = new HashMap<ByteArray,BuddyTorrent>();
	// TODO manage local torrents on a per-buddy basis
	private final HashMap<ByteArray,BuddyTorrent> torrentsLocal = new HashMap<ByteArray,BuddyTorrent>();

	
	private long stateChangeTime;
	private long lastConnectTime;
	
	private DDBKeyValuePair buddyRegistration;
	private InetSocketAddress tcpAddress = null;
	private InetSocketAddress udpAddress = null;
	private boolean useCrypto = false;
	
	private final InheritableConfigTree buddyConfig;
	private InheritableString permaNick;
	
	
	private final SEPublicKey publicKey;
	
	

	Buddy(BuddyManager man, SEPublicKey key)
	{
		manager = man;
		publicKey = key;
		
		
		buddyConfig = InheritableConfigTree.getInstance(manager.getConfig(), Utils.bytesToString(publicKey.encodePublicKey()));
		buddyConfig.setMonitoringState(true); // prevent serialisation
		
		permaNick = InheritableString.getInstance(buddyConfig, "Buddy.permaNick", true);
		
		
		buddyRegistration = BuddyPlugin.getDHTMan().getManagedDDBKeyValuePair("buddy:"+getSerializedPublicKey(), permaNick.getString()+"'s registration ("+getSerializedPublicKey()+")");
		BuddyPlugin.getDHTMan().registerListener(myListeners);
		
		setState(BUDDY_STATE_NOT_MANAGED);
	}
	
	void bless()
	{

		setState(BUDDY_STATE_LOOKUP_PENDING);
		// fake time to trigger an initial lookup when the buddy is added
		stateChangeTime = SystemTime.getCurrentTime()-ADDRESS_LOOKUP_INTERVAL;
	}

	/**
	 * add and initialise connection if buddy is currently not connected
	 * @return handler if connection is accepted, null otherwise
	 */
	BuddyConnectionHandler newBuddyConnectionAuthenticated(BuddyConnection conn)
	{
		if(state >= BUDDY_STATE_AUTHENTICATED || conn == null)
			return null;
		lastConnectTime = SystemTime.getCurrentTime();
		connection = conn;
		setState(BUDDY_STATE_AUTHENTICATED);

		// set handler after the state change (handler might cause authed state)		
		return myListeners;
	}

	/**
	 * send initial data after buddy has been fully established
	 */
	private void connectionEstablished()
	{
		setState(BUDDY_STATE_CONNECTED);
		sendNickChange(manager.getNickName());
		sendPendingTorrents();
	}

	/*
	 * cleanup state when connection is lost
	 */
	private void buddyDisconnected()
	{
		connection = null;
		if(tcpAddress != null)
			setState(BUDDY_STATE_ADDRESS_KNOWN);
		else
			setState(BUDDY_STATE_LOOKUP_PENDING);
		for(BuddyTorrent i : torrentsLocal.values())
		{
			i.setFlag(BuddyTorrent.BUDDY_TORRENT_ADD | BuddyTorrent.BUDDY_TORRENT_SEND_PENDING, true);
		}
	}


	public void setPermaNick(String newNick)
	{
		permaNick.setMonitoringState(false);
		permaNick.setString(newNick);
	}

	public String getPermaNick()
	{
		return permaNick.getString();
	}

	public String getCurrentNick()
	{
		return remoteNick;
	}

	public String getSerializedPublicKey() {
		return BuddyUtils.serializePubkey(publicKey);
	}


	private void processRemoteTorrentList(Map downloads)
	{
		if(state < BUDDY_STATE_CONNECTED)
			return;

		List result;
		result = (List)downloads.get("torrents");
		if(result != null)
		{
			for(int i=0;i<result.size();i++)
			{
				if(result.get(i) instanceof Map)
				{
					BuddyTorrent newTor = TorrentUtils.createFromRemote((Map)result.get(i), BuddyTorrent.BUDDY_TORRENT_ADD);
					if(newTor == null)
						continue;

					if(newTor.isFlagSet(BuddyTorrent.BUDDY_TORRENT_REMOVE))
					{
						removeRemoteDownload(newTor.getHash());
						continue;
					}

					if(newTor.isFlagSet(BuddyTorrent.BUDDY_TORRENT_ADD))
						addRemoteTorrent(newTor);
					if(newTor.isFlagSet(BuddyTorrent.BUDDY_TORRENT_RECOMMENDED))
						recommendationRecieved(newTor.getHash());

				}
			}
		}
	}

	private void processTorrentReq(RemoteTransferRequest req)
	{
		if(state < BUDDY_STATE_CONNECTED)
			return;

		if(torrentsLocal.containsKey(req.getRequestedTorrentHash()))
		{
			try
			{
				req.grantRequest(BuddyPlugin.getPI().getDownloadManager().getDownload(req.getRequestedTorrentHash().getArray()).getTorrent());
			} catch (DownloadException e)
			{
				Debug.printStackTrace(e);
			}

			// TODO implement policy checking

		}

	}

	void sendPendingTorrents()
	{ // TODO loop this via BuddyManager
		if(state < BUDDY_STATE_CONNECTED)
			return;

		Collection<BuddyTorrent> locals = torrentsLocal.values();
		Map toSend = new HashMap(1);
		List torrents = new ArrayList();
		toSend.put("torrents", torrents);

		int c = 0;

		for(BuddyTorrent i : locals)
		{
			if( c == 50 )
				break;
			if(i.isFlagSet(BuddyTorrent.BUDDY_TORRENT_SEND_PENDING))
			{
				torrents.add(TorrentUtils.serializeBuddyTorrent(i,true));
				c++;
			}


			i.setFlag(BuddyTorrent.BUDDY_TORRENT_SEND_PENDING, false);
		}

		if(c == 0)
			return;

		connection.sendTorrentList(toSend);
	}



	private void addRemoteTorrent(BuddyTorrent torrent)
	{
		if(torrentsFromBuddy.containsKey(torrent.getHash()))
			return;

		torrentsFromBuddy.put(torrent.getHash(), torrent);
		UILogger.log("adding torrent: "+torrent.getName());
		// notify listeners
		for(BuddyListener i : listeners)
			i.remoteDownloadAdded(torrent.getHash(), torrent);

		
		/**
		 * testing code
		 */
		connection.requestTorrent(torrent.getHash(), new LocalTransferListener() {
			public void transferEnded(LocalTransferRequest request)
			{
				UILogger.log("transfer finished with state: "+request.getStage());
			}
		});

	}

	private void removeRemoteDownload(ByteArray hash)
	{
		if(!torrentsFromBuddy.containsKey(hash))
			return;
		torrentsFromBuddy.remove(hash);
		for(BuddyListener i : listeners)
			i.remoteDownloadRemoved(hash);

	}

	private void recommendationRecieved(ByteArray hash)
	{
		if(torrentsFromBuddy.containsKey(hash))
			UILogger.log("recommendation received for torrent"+torrentsFromBuddy.get(hash).getName());
		else
			UILogger.log("recommendation received for unlisted torrent "+new String(hash.getArray()));
		// TODO recommendation stuff
	}

	void addLocalDownload(Download download)
	{
		final BuddyTorrent newTor = TorrentUtils.createFromLocal(download, BuddyTorrent.BUDDY_TORRENT_ADD | BuddyTorrent.BUDDY_TORRENT_SEND_PENDING);
		if(!torrentsLocal.containsKey(newTor.getHash()))
		{
			torrentsLocal.put(newTor.getHash(), newTor);
		}
	}

	void removeLocalDownload(ByteArray hash)
	{
		final BuddyTorrent delTor = torrentsLocal.get(hash);
		if(delTor != null)
		{
			delTor.setFlag(BuddyTorrent.BUDDY_TORRENT_ADD, false);
			delTor.setFlag(BuddyTorrent.BUDDY_TORRENT_REMOVE | BuddyTorrent.BUDDY_TORRENT_SEND_PENDING, true);
		}


	}

	void destroy()
	{
		if(connection != null)
			manager.connControl.removeBuddyConnection(connection, "Buddy has been removed");
	}

	public Map<ByteArray,BuddyTorrent> getRemoteTorrents()
	{
		return Collections.unmodifiableMap(torrentsFromBuddy);
	}

	public Map<ByteArray,BuddyTorrent> getLocalTorrents()
	{
		return Collections.unmodifiableMap(torrentsLocal);
	}

	public int hashCode() {	return BuddyUtils.serializePubkey(publicKey).hashCode()+1; }
	
	public boolean equals(Object arg0)
	{
		if(!(arg0 instanceof Buddy))
			return false;

		final Buddy toCheck = (Buddy)arg0;

		return this.publicKey.equals(toCheck.publicKey);
	}



	private void chatMessageReceived(BuddyChatMsg msg)
	{
		// TODO listener stuff, maybe keep a message log?
		if(msg.getMessageType() == BuddyChatMsg.MSG_TYPE_NICKCHANGE)
		{
			remoteNick = msg.getMessageContent();
		}
	}

	public void sendChatMessage(String message, boolean isAction)
	{
		if(connection == null) return;
		connection.sendChatMsg(new BuddyChatMsg(isAction ? BuddyChatMsg.MSG_TYPE_ACTIONLINE : BuddyChatMsg.MSG_TYPE_CHATLINE, message));
	}

	void sendNickChange(String newNick)
	{
		if(connection == null) return;
		connection.sendChatMsg(new BuddyChatMsg(BuddyChatMsg.MSG_TYPE_NICKCHANGE, newNick));
	}

	public void registerListener(BuddyListener listener)
	{
		listeners.add(listener);
	}

	public void unregisterListener(BuddyListener listener)
	{
		listeners.remove(listener);
	}

	long timeSinceStateChanged()
	{
		return SystemTime.getCurrentTime()-stateChangeTime;
	}


	public long timeSinceLastConnection()
	{
		if(lastConnectTime == -1)
			return -1;
		else
			return SystemTime.getCurrentTime()-lastConnectTime; 
	}

	void setState(int newState)
	{
		if(state == newState)
			return;
		stateChangeTime  = SystemTime.getCurrentTime();
		state = newState;
		for(BuddyListener i : listeners)
		{
			i.stateChanged(state);
		}
	}

	void setFlag(int flag, boolean set)
	{ 
		if(set)
			flags |= flag;
		else
			flags &= ~flag;
	}
	
	void connect()
	{
		if(state == BUDDY_STATE_ADDRESS_KNOWN)
		{
			manager.connControl.createBuddyConnection(tcpAddress, udpAddress, useCrypto);
			// TODO remove testing hack
			manager.connControl.createBuddyConnection(new InetSocketAddress("127.0.0.1",tcpAddress.getPort()) , new InetSocketAddress("127.0.0.1",udpAddress.getPort()) , useCrypto);
		}
	}
	
	void lookupAddress()
	{
		// lookup needed and didn't happen within the last 10 minutes?
		if(state == BUDDY_STATE_LOOKUP_PENDING && SystemTime.getCurrentTime()-stateChangeTime > ADDRESS_LOOKUP_INTERVAL)
		{
			buddyRegistration.refresh(60*1000, true);
			// fake state change to delay next lookup
			stateChangeTime = SystemTime.getCurrentTime();
		}
	}
	
	private void processLookupResult(int flags)
	{
		if(flags == (DHTManagerListener.READ_QUERY | DHTManagerListener.QUERY_DONE))
		{
			ByteArray result = buddyRegistration.getLatestValueAsArray();
			if(result != null)
			{
				InetAddress addr;
				int tcpPort;
				int udpPort;
				boolean useCrypto;
				
				try
				{
					byte[] value = result.getArray();
					Map decoded = BDecoder.decode(value);
					addr = InetAddress.getByAddress((byte[])decoded.get("ip"));
					tcpPort = ((Long)decoded.get("tcp")).intValue();
					udpPort = ((Long)decoded.get("udp")).intValue();
					useCrypto = ((Long)decoded.get("c")).intValue() > 0;
				} catch (IOException e)
				{
					Debug.printStackTrace(e);
					return;
				}
				
				tcpAddress = new InetSocketAddress(addr,tcpPort);
				udpAddress = new InetSocketAddress(addr,udpPort);
				this.useCrypto = useCrypto;
				
				if(state < BUDDY_STATE_ADDRESS_KNOWN)
				{
					setState(BUDDY_STATE_ADDRESS_KNOWN);
					connect(); // attempt to connect immediately
				}
				
				UILogger.log("Found Buddy: "+getPermaNick()+" at "+addr.toString()+" tcp:"+tcpPort+" udp:"+udpPort);
			}	
		}
	}

	InetSocketAddress getAddress() { return tcpAddress; }
	boolean isCryptoRequired() { return useCrypto; }
	SEPublicKey getPubKey() { return publicKey; }

	public int getFlags() {	return flags; }
	public boolean isFlagSet(int flag) { return (flags & flag) == flags; }
	public int getState() {	return state; }

	BuddyConnection getConnection() { return connection; }
}
