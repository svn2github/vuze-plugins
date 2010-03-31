package com.aelitis.azbuddy.buddy.connection;

import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;

import com.aelitis.azbuddy.buddy.connection.BuddyConnection;
import com.aelitis.azbuddy.utils.ByteArray;

public class RemoteTransferRequest {
	private final int ID;
	private final BuddyConnection connection;
	private final ByteArray requestedHash;
	
	RemoteTransferRequest(BuddyTorrentTransferMsg msg, BuddyConnection conn)
	{
		ID = msg.transferID;
		connection = conn;
		requestedHash = new ByteArray(msg.chunk); 
	}
	
	public void denyRequest()
	{
		connection.sendTorrentTransferMsg(new BuddyTorrentTransferMsg(ID));
	}
	
	public void grantRequest(Torrent toSend)
	{
		ByteBuffer rawTorrent;
		try
		{
			rawTorrent = ByteBuffer.wrap(toSend.removeAdditionalProperties().writeToBEncodedData());
		} catch (TorrentException e)
		{
			throw new RuntimeException("torrent serialization failed",e);
		}

		while(rawTorrent.position() < rawTorrent.limit())
		{
			connection.sendTorrentTransferMsg(new BuddyTorrentTransferMsg(ID, rawTorrent));
		}
	}
	
	public ByteArray getRequestedTorrentHash()
	{
		return requestedHash;
	}
}
