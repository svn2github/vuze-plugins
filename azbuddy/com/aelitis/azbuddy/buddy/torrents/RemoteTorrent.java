package com.aelitis.azbuddy.buddy.torrents;

import java.util.Map;

import com.aelitis.azbuddy.utils.ByteArray;

class RemoteTorrent implements BuddyTorrent {
	
	private final Map data;
	private int flags;
	
	RemoteTorrent(Map serialized, int defaultFlags)
	{
		data = serialized;
		if(data.containsKey("flags") && data.get("flags") instanceof Long)
			flags = ((Long)data.get("flags")).intValue() & BUDDY_TORRENT_REMOTE_MASK;
		else
			flags = defaultFlags & BUDDY_TORRENT_REMOTE_MASK;
	}
	
	public long getFilesSize()
	{
		return ((Long)data.get("size")).longValue();
	}
	
	public ByteArray getHash()
	{
		return (ByteArray)data.get("hash");
	}
	
	public String getName()
	{
		return new String((byte[])data.get("name"));
	}
	
	public int getNbFiles()
	{
		return ((Long)data.get("files")).intValue();
	}
	
	public int getRating()
	{
		return 0;
	}
	
	public boolean isPrivate()
	{
		return data.containsKey("priv");
	}
	
	public boolean isFlagSet(int flag)
	{
		return (flag & flags) == flag;
	}
	
	public void setFlag(int flag, boolean setTo)
	{
		if(setTo)
			flags |= flag;
		else
			flags &= ~flag;
	}
	
	public int getFlags()
	{
		return flags;
	}
}
