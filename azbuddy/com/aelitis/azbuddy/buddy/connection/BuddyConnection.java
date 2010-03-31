package com.aelitis.azbuddy.buddy.connection;


import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.network.IncomingMessageQueueListener;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;

import com.aelitis.azbuddy.ui.UILogger;
import com.aelitis.azbuddy.utils.ByteArray;

public class BuddyConnection implements IncomingMessageQueueListener, GenericMessageConnectionListener  {


	private int stateFlags;
	
	public static final int CONN_INIT = 1;
	public static final int CONN_AUTHED = 2;
	public static final int CONN_CONNECTED = 4;
	public static final int DESTROYED = 128;
	
	public static final int CONN_ESTABLISHED = CONN_CONNECTED | CONN_AUTHED | CONN_INIT;
	
	private BuddyConnectionHandler handler;
	private final ConnectionControl connControl;
	private GenericMessageConnection connection;
	private GenericMessageMuxer muxer;
	
	private long initTime = SystemTime.getCurrentTime();
	private static final long INIT_TIMEOUT = 60*1000; // 1 minute
	
	private final boolean isRemote;
	
	BuddyConnection(ConnectionControl control, GenericMessageConnection conn, boolean isRemote)
	{
		stateFlags = 0;
		
		connControl = control;

		this.isRemote = isRemote;		
		connection = conn;
		connection.addListener(this);
		muxer = new GenericMessageMuxer(connection,this,BuddyMsg.REGISTERED_MESSAGES);
		
		changeStateFlag(CONN_INIT,true);
		
		if(isRemote) // fake connected event in case of remote connections
			connected(connection);
	}
	
	boolean remoteAuth(SEPublicKey remoteKey)
	{
		handler = connControl.handler.matchNewConnection(this, remoteKey);
		if(handler == null)
		{
			closeConnection("buddy not authorized");
			return false;
		} else 
		{
			changeStateFlag(CONN_AUTHED,true);
			return true;
		}
	}

	public void sendTorrentList(Map torrentBatch)
	{
		if(!isFlagSet(CONN_ESTABLISHED))
			throw new IllegalStateException("Connection not established");
		
		muxer.send(new BuddyTorListMsg(ConnectionUtils.bencode(torrentBatch)));
	}
	
	public void sendChatMsg(BuddyChatMsg msg)
	{
		if(!isFlagSet(CONN_ESTABLISHED))
			throw new IllegalStateException("Connection not established");
		
		UILogger.log("sending chat msg:"+msg.getType()+" "+msg.getMessageContent());
		muxer.send(msg);
	}
	
	public void requestTorrent(ByteArray toRequest, LocalTransferListener toNotify)
	{
		new LocalTransferRequest(this,toRequest,toNotify);
	}
	
	void sendTorrentTransferMsg(BuddyTorrentTransferMsg msg)
	{
		if(!isFlagSet(CONN_ESTABLISHED))
			throw new IllegalStateException("Connection not established");
		muxer.send(msg);
	}
	
	
	/**
	 * should be called from the connection controller or internally
	 */
	void closeConnection(String reason)
	{
		if(isFlagSet(DESTROYED))
			return;
		changeStateFlag(CONN_ESTABLISHED, false);
		changeStateFlag(DESTROYED, true);
		
		
		UILogger.log("Closing "+(isRemote?"incoming":"outgoing")+" connection to "+connection.getEndpoint().getNotionalAddress()+" Cause: "+reason);
		try
		{
			connection.close();
		} catch (MessageException e)
		{
			Debug.printStackTrace(e); // dispose anyway
		}
		
		connection.removeListener(this);
		connection = null;
		muxer = null;
		connControl.connectionRemoved(this);
	}

	private synchronized void changeStateFlag(int mask, boolean setTo)
	{
		if(isFlagSet(DESTROYED))
			return;
		
		if(setTo)
			stateFlags |= mask;
		else
			stateFlags &= ~mask;
		
		
		UILogger.log("new stateFlags for "+(isRemote?"incoming":"outgoing")+" connection ("+connection.getEndpoint().getNotionalAddress()+")  :"+stateFlags);

		if(handler != null)
			handler.stateChanged();

	}
	
	public boolean isFlagSet(int mask)
	{
		return (stateFlags & mask) == mask;
	}
	
	void performConnectionCheck()
	{
		if(isFlagSet(DESTROYED))
			return;
		
		if(!isFlagSet(CONN_ESTABLISHED) && SystemTime.getCurrentTime()-initTime > INIT_TIMEOUT)
			closeConnection("connection init timed out");
	}
	
	// Listeners
	
	public void bytesReceived(int byte_count) { /* do nothing */ }
	public boolean messageReceived(Message message)
	{
		if(message.getID() == BuddyMsg.BUDDY_TORRENT_LIST_MSG)
		{
			if(handler != null)
				handler.torrentListMsgReceived(ConnectionUtils.bdecode((((BuddyTorListMsg)message).serializedDownloads)));
			return true;
		}else if(message.getID() == BuddyMsg.BUDDY_CHAT_MSG)
		{
			final BuddyChatMsg msg = (BuddyChatMsg)message; 
			UILogger.log("received chat msg:"+msg.getType()+" "+msg.getMessageContent());
			if(handler != null)
				handler.chatMessageReceived(msg);
			return true;
		}else if(message.getID() == BuddyMsg.BUDDY_TORRENT_TRANSFER)
		{
			final BuddyTorrentTransferMsg msg = (BuddyTorrentTransferMsg)message;
			
			if(msg.msgType == BuddyTorrentTransferMsg.MSG_TYPE_TRANSFER_REQ && handler != null)
			{
				handler.torrentReqReceived(new RemoteTransferRequest(msg,this));
			}else if(msg.msgType == BuddyTorrentTransferMsg.MSG_TYPE_TRANSFER_CHUNK)
			{
				LocalTransferRequest.update(msg);
			}
			
			return true;
		}
		
		
		return false;
	}
	
	
	public void failed(GenericMessageConnection connection, Throwable error) throws MessageException
	{ // if an error occurs assume the connection is closed by remote peer
		closeConnection("Connection closed by remote peer, cause: "+error.toString());		
	}
	
	public void receive(GenericMessageConnection connection, PooledByteBuffer message) throws MessageException { /* the muxer handles that */ }
	
	public void connected(GenericMessageConnection connection)
	{
		UILogger.log("connection started");
		// connection established, enter handshake phase
		changeStateFlag(CONN_CONNECTED, true);
		initTime = SystemTime.getCurrentTime();
	}
}
