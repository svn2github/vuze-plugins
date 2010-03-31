package com.aelitis.azbuddy.buddy;

import com.aelitis.azbuddy.buddy.torrents.BuddyTorrent;
import com.aelitis.azbuddy.utils.ByteArray;

public interface BuddyListener {
	public void stateChanged(int state);
	public void remoteDownloadAdded(ByteArray hash, BuddyTorrent torrent);
	public void remoteDownloadRemoved(ByteArray hash);
	public void nickChanged(String newNick);
	public void receivedChatMsg(String msg, boolean isAction);
}
