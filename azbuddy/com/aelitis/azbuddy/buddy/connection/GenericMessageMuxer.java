package com.aelitis.azbuddy.buddy.connection;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.network.IncomingMessageQueueListener;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.utils.AsyncTaskRunner;

import java.nio.ByteBuffer;

public class GenericMessageMuxer implements GenericMessageConnectionListener  {
	
	final GenericMessageConnection connection;
	final IncomingMessageQueueListener consumer;
	final Message[] decoders;
	
	public GenericMessageMuxer(GenericMessageConnection conn, IncomingMessageQueueListener consumer, Message[] reggedMessages)
	{
		this.consumer = consumer;
		connection = conn;
		decoders = reggedMessages;
		
		connection.addListener(this);
	}
	
	public void send(Message toSend)
	{
		short IDsize = (short)toSend.getID().getBytes().length;
		
		
		int payloadSize = 0;
		final ByteBuffer[] payload = toSend.getPayload();
		for(ByteBuffer i : payload)
			payloadSize+=i.limit();
		
		final PooledByteBuffer outputBuf = BuddyPlugin.getPI().getUtilities().allocatePooledByteBuffer(2+IDsize+4+payloadSize);
		
		outputBuf.toByteBuffer().putShort(IDsize);
		outputBuf.toByteBuffer().put(toSend.getID().getBytes());
		outputBuf.toByteBuffer().putInt(payloadSize);
		for(ByteBuffer i : payload)
			outputBuf.toByteBuffer().put(i);
		

		// use a seperate thread to avoid any deadlocks
		AsyncTaskRunner.addTask(new Runnable() { 
			public void run()
			{
				try
				{
					connection.send(outputBuf);
				} catch (MessageException e)
				{
					Debug.printStackTrace(e);
				}
			}
		});
			
	}
	
	public void receive(GenericMessageConnection connection, PooledByteBuffer message) throws MessageException
	{
		ByteBuffer outerMsg = message.toByteBuffer();
		
		byte[] IDbytes = new byte[outerMsg.getShort()];
		outerMsg.get(IDbytes);
		String incomingID = new String(IDbytes);
		
		int innerMsgSize = outerMsg.getInt();
		ByteBuffer innerMsg = outerMsg.slice();
		innerMsg.limit(innerMsgSize);
		
		for(Message i : decoders)
		{
			if(i.getID().equals(incomingID))
			{
				final Message decodedMsg = i.create(innerMsg);
				// use a seperate thread to avoid any deadlocks
				AsyncTaskRunner.addTask(new Runnable() {
					public void run()
					{
						consumer.messageReceived(decodedMsg);
					}
				});
				break;
			}
		}
	}
	
	
	// the consumer should take care of those events	
	public void connected(GenericMessageConnection connection) {}
	public void failed(GenericMessageConnection connection, Throwable error) throws MessageException {}
	
}
