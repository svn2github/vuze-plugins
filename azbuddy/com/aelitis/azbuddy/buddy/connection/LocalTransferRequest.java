package com.aelitis.azbuddy.buddy.connection;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.buddy.connection.BuddyConnection;
import com.aelitis.azbuddy.buddy.connection.BuddyTorrentTransferMsg;
import com.aelitis.azbuddy.ui.UILogger;
import com.aelitis.azbuddy.utils.ByteArray;
import com.aelitis.azbuddy.utils.TaskScheduler;

public class LocalTransferRequest {

	static final ArrayList<LocalTransferRequest> requests = new ArrayList<LocalTransferRequest>();


	static final Random rand = new Random();
	final int ID;

	private static final int TTR_REQUESTED = 0;
	private static final int TTR_RECEIVING = 1;
	public static final int TTR_DONE = 2;
	public static final int TTR_FAILED = 3;
	public static final int TTR_DENIED = 4;

	private int stage;
	private int recieved;
	private BuddyConnection connection;

	ByteBuffer rawTorrent;
	Torrent decodedTorrent;
	final ByteArray torHash;

	TaskScheduler.TimedEvent timeout;

	LocalTransferListener listener;


	public static final int TRANSFER_TIMEOUT = 60*1000;

	LocalTransferRequest(BuddyConnection conn, ByteArray toReq, LocalTransferListener l)
	{
		synchronized (rand)
		{
			ID = rand.nextInt();
		}
		connection = conn;
		torHash = toReq;
		conn.sendTorrentTransferMsg(new BuddyTorrentTransferMsg(ID,toReq));
		stage = TTR_REQUESTED;
		listener = l;
		timeout = TaskScheduler.timedAdd(new Runnable(){
			public void run()
			{
				finalCheck();
			}
		}, TRANSFER_TIMEOUT);
		requests.add(this);
	}

	/**
	 * 
	 * @return true if a matching request was found
	 */
	static boolean update(BuddyTorrentTransferMsg updateMsg)
	{
		for(LocalTransferRequest i : requests)
		{
			if(i.ID == updateMsg.transferID)
			{
				i.updateSupport(updateMsg);
				return true;
			}
		}
		return false;
	}

	void updateSupport(BuddyTorrentTransferMsg updateMsg)
	{
		if(rawTorrent == null && stage == TTR_REQUESTED)
		{
			rawTorrent = ByteBuffer.allocate(updateMsg.bundledSize);
			stage = TTR_RECEIVING;
		}

		if(stage != TTR_RECEIVING)
			return;

		timeout.reset();

		rawTorrent.position(updateMsg.chunkOffset);
		rawTorrent.put(updateMsg.chunk);
		recieved += updateMsg.chunk.limit();
		if(recieved >= rawTorrent.capacity())
		{
			UILogger.log("torrent handler revieved "+recieved+"/"+rawTorrent.capacity());
			finalCheck();
		}
	}



	private void finalCheck()
	{
		timeout.stop();
		requests.remove(this);
		if(stage == TTR_DONE || stage == TTR_FAILED || stage == TTR_DENIED)
			return;

		boolean decoded = false;

		if(rawTorrent != null)
		{
			try
			{
				decodedTorrent = BuddyPlugin.getPI().getTorrentManager().createFromBEncodedData(rawTorrent.array());
				if(torHash.equals(decodedTorrent.getHash()))
					decoded = true;
			} catch (TorrentException e)
			{
				Debug.printStackTrace(e);
			}
		}

		rawTorrent = null;
		if(!decoded)
		{
			decodedTorrent = null;
			stage = TTR_FAILED;
		} else
			stage = TTR_DONE;
			



		listener.transferEnded(this);		
	}

	public int getStage()
	{
		return stage;
	}

	public Torrent getTorrent()
	{
		return decodedTorrent;
	}
}