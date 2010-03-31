package com.aelitis.azbuddy.buddy.torrents;

import java.util.LinkedList;
import java.util.List;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.buddy.Buddy;

public abstract class TorrentDownloadRequest {
	
	final BuddyTorrent toRequest;
	final long creationTime = SystemTime.getCurrentTime();
	LinkedList<Buddy> sources;
	
	public TorrentDownloadRequest(BuddyTorrent tor)
	{
		if(tor == null)
			throw new NullPointerException();
		toRequest = tor;
		List<Buddy> buddies = BuddyPlugin.getSingleton().getBuddyMan().getBuddies();
		for(Buddy i : buddies)
		{
			if(i.getRemoteTorrents().containsKey(toRequest.getHash()) && i.getState() == Buddy.BUDDY_STATE_AUTHENTICATED)
				sources.addLast(i);
		}
		
	}
	
	abstract public void downloadFinished(BuddyTorrent tor, Torrent result);
	abstract public void downloadFailed(BuddyTorrent tor, String reason);
}
