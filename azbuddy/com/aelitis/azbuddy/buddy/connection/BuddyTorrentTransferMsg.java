package com.aelitis.azbuddy.buddy.connection;

import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;

import com.aelitis.azbuddy.utils.ByteArray;

class BuddyTorrentTransferMsg implements BuddyMsg {
	
	static final short MSG_TYPE_TRANSFER_REQ		= 0;
	static final short MSG_TYPE_TRANSFER_NOT_AVL	= 1;
	static final short MSG_TYPE_TRANSFER_CHUNK		= 2;
	
	private ByteBuffer buffer;
	
	final short msgType;
	final int transferID;
		
	final int bundledSize;
	final ByteBuffer chunk;
	final int chunkOffset;
	
	
	/**
	 * Use this constructor to deny a torrent transfer request 
	 * @param ID the ID that identifies the denied transfer
	 */
	BuddyTorrentTransferMsg(int ID)
	{
		transferID = ID;
		msgType = MSG_TYPE_TRANSFER_NOT_AVL;
		chunk = ByteBuffer.allocate(0);
		bundledSize = chunkOffset = 0;
	}
	
	
	/**
	 * Use this constructor to create a torrent transfer request
	 * @param ID the transaction ID that'll be used by the buddy to respond to our request
	 * @param toRequest the torrent hash we want from our buddy
	 */
	BuddyTorrentTransferMsg(int ID, ByteArray toRequest)
	{
		transferID = ID;
		msgType = MSG_TYPE_TRANSFER_REQ;
		chunk = ByteBuffer.wrap(toRequest.getArray());
		bundledSize = chunkOffset = 0;
	}
	
	/**
	 * 
	 * @param ID the transaction ID that identifies multiple messages belonging to the same torrent
	 * @param payload the entire data that has to be sent across multiple messages, slicing is done based on the current position
	 */
	BuddyTorrentTransferMsg(int ID, ByteBuffer payload)
	{
		transferID = ID;
		msgType = MSG_TYPE_TRANSFER_CHUNK;
	
		bundledSize = payload.limit();

		// create copy with identical position
		chunk = payload.duplicate();
		chunkOffset = chunk.position();

		// adjust limit if the packet would be too big
		if(chunk.remaining() > MAX_CHUNK_SIZE)
			chunk.limit(chunk.position()+MAX_CHUNK_SIZE);
		
		// forward position for next slicing
		payload.position(chunkOffset+chunk.remaining());
	}
	
	private BuddyTorrentTransferMsg(int ID, short type, int offset, int size, ByteBuffer data)
	{
		transferID = ID;
		msgType = type;
		chunkOffset = offset;
		bundledSize = size;
		chunk = data;
	}
	
	
	
	public String getID() {	return BUDDY_TORRENT_TRANSFER; }
	public int getType() { return TYPE_PROTOCOL_PAYLOAD; }
	public String getDescription() { return BUDDY_TORRENT_TRANSFER+" Type:"+msgType+" offset"+chunkOffset+" chunk length:"+chunk.remaining()+" bundle size:"+bundledSize; }

	
	private void createBuffer()
	{
		chunk.mark();
		buffer = ByteBuffer.allocate(2+2+4+4+4+2+chunk.remaining());
		buffer.putShort(VERSION);
		buffer.putShort(msgType);
		buffer.putInt(transferID);
		buffer.putInt(bundledSize);
		buffer.putInt(chunkOffset);
		buffer.putShort((short)(chunk.remaining()));
		buffer.put(chunk);
		buffer.flip();
		chunk.reset();
	}
	
	public ByteBuffer[] getPayload()
	{
		// delay buffer creation until it's necessary
		if(buffer == null)
			createBuffer();
		
		return new ByteBuffer[] {buffer};
	}

	public Message create(ByteBuffer data) throws MessageException
	{
		short version = data.getShort();
		short type = data.getShort();
		int ID = data.getInt();
		int size = data.getInt();
		int offset = data.getInt();
		short length = data.getShort();
		if(data.remaining() < length)
			throw new MessageException(BUDDY_TORRENT_TRANSFER+" Decode failed, message too short");
		byte[] content = new byte[length];
		data.get(content);

		return new BuddyTorrentTransferMsg(ID,type, offset, size, ByteBuffer.wrap(content));
	}

	public void destroy() { buffer = null; }
}
