package com.aelitis.azbuddy.buddy.torrents;

import org.gudy.azureus2.plugins.download.Download;

import com.aelitis.azbuddy.utils.ByteArray;

class LocalTorrent implements BuddyTorrent {
	
	private final Download dl;
	private int flags;

	LocalTorrent(Download dl, int initialState)
	{
		this.dl = dl;
		flags = initialState;
	}
	
	public long getFilesSize()
	{
		return dl.getTorrent().getSize();
	}
	
	public ByteArray getHash()
	{
		return new ByteArray(dl.getTorrent().getHash());
	}
	
	public String getName()
	{
		return dl.getName();
	}
	
	public int getNbFiles()
	{
		return dl.getTorrent().getFiles().length;
	}
	
	public int getRating()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	public boolean isPrivate()
	{
		return dl.getTorrent().isPrivate();
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
