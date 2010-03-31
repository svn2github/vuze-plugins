package com.aelitis.azbuddy.buddy.connection;

import java.util.Map;


public interface BuddyConnectionHandler {
	public void stateChanged();
	public void torrentListMsgReceived(Map downloads);
	public void chatMessageReceived(BuddyChatMsg msg);
	public void torrentReqReceived(RemoteTransferRequest handler);
}
