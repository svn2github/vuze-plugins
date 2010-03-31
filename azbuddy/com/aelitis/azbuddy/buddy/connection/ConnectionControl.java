package com.aelitis.azbuddy.buddy.connection;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageRegistration;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;

import com.aelitis.azbuddy.BuddyPlugin;

public class ConnectionControl implements GenericMessageHandler  {
	
	
	private List<BuddyConnection> connections = new ArrayList<BuddyConnection>();
	private GenericMessageRegistration messageReg;
	private boolean isRunning;
	
	
	final private PluginInterface pI;
	final private SESecurityManager secMan;
	
	final ConnectionControlHandler handler;
	
	public ConnectionControl(ConnectionControlHandler handler)
	{
		this.handler = handler;
		
		pI = BuddyPlugin.getPI();
		secMan = BuddyPlugin.getSecurityManager();
	}
	
	
	public void start()
	{
		isRunning = true;
		
		try
		{
			messageReg = pI.getMessageManager().registerGenericMessageType(BuddyMsg.BUDDY_CONNECTION_TOKEN, "v"+BuddyMsg.VERSION+" Buddy Connection", MessageManager.STREAM_ENCRYPTION_NONE, this);
		}catch (MessageException e)
		{
			e.printStackTrace();
		}
	}
	
	public void stop()
	{
		isRunning = false;
		for (BuddyConnection i : connections)
		{
			i.closeConnection("plugin closedown");
		}
		messageReg.cancel();
	}
	
	
	public void createBuddyConnection(InetSocketAddress tcpDest, InetSocketAddress udpDest, boolean useCrypto)
	{
		if(!isRunning)
			throw new IllegalStateException("Connection Control currently not running");
		
		GenericMessageEndpoint endpoint = messageReg.createEndpoint(tcpDest);
		endpoint.addTCP(tcpDest);
		endpoint.addUDP(udpDest);
		
		GenericMessageConnection connection;
		
		try
		{
			connection = messageReg.createConnection(endpoint);
		} catch (MessageException e)
		{
			Debug.printStackTrace(e);
			return;
		}
		
		
		RemoteKeyAcceptor acceptor = new RemoteKeyAcceptor();
		
		
		try
		{
			connection = secMan.getSTSConnection(
					connection,
					BuddyPlugin.getIdentity(),
					acceptor,
					"Outgoing Buddy Connection",
					SESecurityManager.BLOCK_ENCRYPTION_AES);
			
		} catch (Exception e)
		{
			Debug.printStackTrace(e);
			return;
		}
		
		
		BuddyConnection buddyConn = new BuddyConnection(this,connection,false);
		acceptor.setHandler(buddyConn);
		
		try
		{
			connection.connect(); // connect after setting the handler to avoid a deadlock
		} catch (MessageException e)
		{
			Debug.printStackTrace(e);
		}
		
		connectionAdded(buddyConn);
	}
	
	public void performConnectionChecks()
	{
		for(int i=0;i<connections.size();i++)
		{
			// this might close the connection and thus modify the array concurrent to the loop => no iterator here
			connections.get(i).performConnectionCheck();
		}
	}
	
	public void removeBuddyConnection(BuddyConnection conn, String reason)
	{
		conn.closeConnection(reason);
	}
	
	void connectionRemoved(BuddyConnection conn)
	{
		synchronized(connections)
		{
			connections.remove(conn);
		}
	}
	
	private void connectionAdded(BuddyConnection conn)
	{
		synchronized(connections)
		{
			connections.add(conn);
		}
	}
	
	
	public boolean accept(GenericMessageConnection connection) throws MessageException
	{
		if(!isRunning)
			throw new IllegalStateException("Connection Control currently not running");
		
		
		RemoteKeyAcceptor acceptor = new RemoteKeyAcceptor();

		
		try
		{
			connection = secMan.getSTSConnection(
					connection,
					BuddyPlugin.getIdentity(),
					acceptor,
					"Incoming Buddy Connection",
					SESecurityManager.BLOCK_ENCRYPTION_AES);
		} catch (Exception e)
		{
			Debug.printStackTrace(e);
			return false;
		}
		
		
		BuddyConnection buddyConn = new BuddyConnection(this,connection,true);
		acceptor.setHandler(buddyConn);
		
		// connection.connect();
		
		connectionAdded(buddyConn);
		
		return true;
	}
	
	private final class RemoteKeyAcceptor implements SEPublicKeyLocator
	{
		private AESemaphore initSem = new AESemaphore("KeyAcceptSem");
		private BuddyConnection handler;
		//private RemoteKeyAcceptor()	{ initSem.reserve(); }
		
		public boolean accept(Object context, SEPublicKey other_key)
		{
			initSem.reserve();
			return handler.remoteAuth(other_key);
		}
		
		private void setHandler(BuddyConnection handler)
		{
			this.handler = handler;
			initSem.releaseForever();
		}
	}
}
