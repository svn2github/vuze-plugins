package com.aelitis.azbuddy.buddy.connection;

import org.gudy.azureus2.plugins.messaging.Message;

public interface BuddyMsg extends Message {
	// TODO version handling... if necessary
	public final static short VERSION = 1;
	
	public final static String BUDDY_CONNECTION_TOKEN = "BuddyConnection v"+VERSION; 
	
	public final static String BUDDY_CHAT_MSG = "BuddyChat";
	public final static String BUDDY_TORRENT_LIST_MSG = "BuddyTorLst";
	public final static String BUDDY_TORRENT_TRANSFER = "BuddyTorTra";
	
	public final static int MAX_CHUNK_SIZE = 1024*16;
	
	public final static Message[] REGISTERED_MESSAGES = new Message[] {
		new BuddyChatMsg((short)1,""),
		new BuddyTorListMsg(null),
		new BuddyTorrentTransferMsg(0)
	};
}
