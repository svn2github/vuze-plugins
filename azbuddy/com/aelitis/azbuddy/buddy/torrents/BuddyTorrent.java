package com.aelitis.azbuddy.buddy.torrents;

import com.aelitis.azbuddy.utils.ByteArray;

public interface BuddyTorrent {
	
	public final static int BUDDY_TORRENT_ADD = 1;
	public final static int BUDDY_TORRENT_REMOVE = 2;
	public final static int BUDDY_TORRENT_RECOMMENDED = 4;
	
	public final static int BUDDY_TORRENT_SEND_PENDING = 32;
	
	
	
	
	/**
	 * set this value to explicitly deny the sending of that torrent to a specific peer
	 * defaults apply otherwise
	 */
	public final static int BUDDY_TORRENT_SEND_DENY = 8;
	/**
	 * set this value to explicitly allow the sending of that torrent to a specific peer
	 * defaults apply otherwise
	 */	
	public final static int BUDDY_TORRENT_SEND_ALLOW = 16;


	/**
	 * mask flags with this value to censor any data that shouldn't be sent to a buddy
	 */
	public final static int BUDDY_TORRENT_REMOTE_MASK = BUDDY_TORRENT_ADD | BUDDY_TORRENT_REMOVE | BUDDY_TORRENT_RECOMMENDED;
	
	
	public ByteArray getHash();
	public String getName();
	public int getRating();
	public boolean isPrivate();
	public int getNbFiles();
	public long getFilesSize();
	public void setFlag(int flag, boolean setTo);
	public boolean isFlagSet(int flag);
	public int getFlags();
}
