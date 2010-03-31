package com.aelitis.azbuddy.buddy.connection;

import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;

class BuddyTorListMsg implements BuddyMsg {
	
	final byte[] serializedDownloads;
	ByteBuffer buffer;
	
	BuddyTorListMsg(byte[] downloads)
	{
		serializedDownloads = downloads;
	}

	public String getID() {	return BUDDY_TORRENT_LIST_MSG; }
	public int getType() { return TYPE_PROTOCOL_PAYLOAD; }
	public String getDescription() { return "["+BUDDY_TORRENT_LIST_MSG+"] raw text ("+serializedDownloads.length+"): "+new String(serializedDownloads); }
	
	private void createBuffer()
	{
		buffer = ByteBuffer.allocate(2+2+serializedDownloads.length);
		buffer.putShort(VERSION);
		buffer.putShort((short)serializedDownloads.length);
		buffer.put(serializedDownloads);
		buffer.flip();
	}

	public ByteBuffer[] getPayload()
	{
		// delay buffer creation until it's neccessary
		if(buffer == null)
			createBuffer();
		
		return new ByteBuffer[] {buffer};
	}

	public Message create(ByteBuffer data) throws MessageException
	{
		short version = data.getShort();
		short length = data.getShort();
		if(data.remaining() < length)
			throw new MessageException("["+BUDDY_TORRENT_LIST_MSG+"] Decode failed, message too short");
		byte[] downloads = new byte[length];
		data.get(downloads,0,length);
		
		return new BuddyTorListMsg(downloads);
	}

	public void destroy() { buffer = null; }

}
