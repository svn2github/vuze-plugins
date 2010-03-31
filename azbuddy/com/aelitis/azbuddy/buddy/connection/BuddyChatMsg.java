package com.aelitis.azbuddy.buddy.connection;

import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;

public class BuddyChatMsg implements BuddyMsg {
	
	public static final short MSG_TYPE_NICKCHANGE	= 0;
	public static final short MSG_TYPE_CHATLINE		= 1;
	public static final short MSG_TYPE_ACTIONLINE	= 2;
	
	short msgType;
	String payload;
	ByteBuffer buffer;
	
	public BuddyChatMsg(short type, String content)
	{
		msgType = type;
		payload = content; 
	}
	
	public String getID() {	return BUDDY_CHAT_MSG; }
	public int getType() { return TYPE_PROTOCOL_PAYLOAD; }
	public String getDescription() { return "["+BUDDY_CHAT_MSG+"]"; }

	public short getMessageType() { return msgType; }
	public String getMessageContent() { return payload; }
	
	private void createBuffer()
	{
		buffer = ByteBuffer.allocate(2+2+2+payload.length());
		buffer.putShort(VERSION);
		buffer.putShort(msgType);
		buffer.putShort((short)payload.length());
		buffer.put(payload.getBytes());
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
		short type = data.getShort();
		short length = data.getShort();
		if(data.remaining() < length)
			throw new MessageException("["+BUDDY_CHAT_MSG+"] Decode failed, message too short");
		byte[] content = new byte[length];
		data.get(content,0,length);
		
		return new BuddyChatMsg(type, new String(content));
	}

	public void destroy() { buffer = null; }
}
