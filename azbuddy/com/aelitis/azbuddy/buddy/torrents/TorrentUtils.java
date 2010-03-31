package com.aelitis.azbuddy.buddy.torrents;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.plugins.download.Download;

import com.aelitis.azbuddy.utils.ByteArray;

public final class TorrentUtils {
	
	public static Map serializeBuddyTorrent(BuddyTorrent toSerialize, boolean forLocalUse)
	{
		final Map serialized = new HashMap();
		serialized.put("hash", toSerialize.getHash().getArray());
		int flags = toSerialize.getFlags();
		if(!forLocalUse)
			flags &= BuddyTorrent.BUDDY_TORRENT_REMOTE_MASK;
		serialized.put("flags", flags);
		if(toSerialize.isFlagSet(BuddyTorrent.BUDDY_TORRENT_ADD))
		{
			serialized.put("name", toSerialize.getName());
			serialized.put("size", toSerialize.getFilesSize());
			serialized.put("files", toSerialize.getNbFiles());
			if(toSerialize.isPrivate())
				serialized.put("priv", null);
		}

		return serialized;
	}

	/**
	 * create a serializable download wrapper from a Map
	 * with limited data received from a Buddy. 
	 * @param defaultFlags sets the flags if none are specified by the map
	 * Please note: All flags will be masked to hide any data that's only intended for local use. 
	 */
	public static BuddyTorrent createFromRemote(Map serialized, int defaultFlags)
	{
		// check for most basic properties
		if(serialized.get("hash") instanceof byte[] && serialized.get("name") instanceof byte[])
		{
			final Map internalMap = new HashMap(serialized);
			if(internalMap.get("hash") instanceof byte[])
				internalMap.put("hash", new ByteArray((byte[])internalMap.get("hash")));
			return new RemoteTorrent(internalMap, defaultFlags);
		} else
			return null;
	}
	
	/**
	 * create a serializable download wrapper from a local download object.
	 * Private torrents will be set to to 
	 * @param initialState set the downloads status flags, those decide how it'll be distributed to the buddies
	 */
	public static BuddyTorrent createFromLocal(Download dl, int initialState)
	{
		if(dl == null)
			return null;
		else
		{
			final BuddyTorrent tor = new LocalTorrent(dl, initialState);
			if(tor.isPrivate())
				tor.setFlag(BuddyTorrent.BUDDY_TORRENT_SEND_DENY, true);
			return tor;
		}
			
	}

}
