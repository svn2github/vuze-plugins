package com.aelitis.azbuddy.buddy.torrents;

import java.util.HashSet;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.buddy.BuddyManager;
import com.aelitis.azbuddy.buddy.connection.LocalTransferRequest;

public class TorrentDownloadManager {
	
	private final HashSet<TorrentDownloadRequest> requests = new HashSet<TorrentDownloadRequest>();
	private final HashSet<LocalTransferRequest> transfers = new HashSet<LocalTransferRequest>();
	
	BuddyManager manager = BuddyPlugin.getSingleton().getBuddyMan();
	
	public TorrentDownloadManager()
	{
		
	}
	
	
	public void requestTorrent(TorrentDownloadRequest req)
	{
		requests.add(req);
	}
	
	public void processRequests()
	{
		for(TorrentDownloadRequest i : requests)
		{
			
		}
	}
	
	

}
