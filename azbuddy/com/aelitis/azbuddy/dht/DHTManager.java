package com.aelitis.azbuddy.dht;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;

public class DHTManager {
	
	final DistributedDatabase ddbPi;
	private final PluginInterface pI;
	
	final ArrayList<DHTManagerListener> listeners = new ArrayList<DHTManagerListener>();
	final HashMap<String, DDBKeyValuePair> pairs = new HashMap<String, DDBKeyValuePair>();
	final LinkedList<DHTTask> tasks = new LinkedList<DHTTask>();
	
	private boolean isRunning = false;
	// start out as unconnected
	private boolean isConnected = false;
	private InetAddress addr;
	private int loopcount;
	
	public DHTManager(PluginInterface pI)
	{
		this.pI = pI;
		this.ddbPi = pI.getDistributedDatabase();
	}
	
	public void start()
	{
		if(isRunning)
			return;
		
		loopcount = 0;
		isRunning = true;
		new AEThread("BuddyDHTManager",true) {
			public void runSupport()
			{
				mainLoop();
			}
		}.start();
	}
	
	private void mainLoop()
	{
		while(isRunning)
		{
			checkState();
			if(isConnected)
			{
				runTasks();
			}
			try
			{
				Thread.sleep(1000);
			} catch (InterruptedException e)
			{
				Debug.printStackTrace(e);
			}
			loopcount++;
		}
	}
	
	public void stop()
	{
		isRunning = false;
	}
	
	private void checkState()
	{
		if(isConnected != (ddbPi.isAvailable() && ddbPi.isExtendedUseAllowed()))
		{ // dht connectivity change detected
			isConnected = !isConnected; // toggle
			for(DHTManagerListener i : listeners)
			{
				i.dhtUsabilityChanged(isConnected);
			}
		}
		
		final InetAddress newAddr = pI.getUtilities().getPublicAddress();
		
		if(newAddr != null && !newAddr.equals(addr))
		{ // addresschange detected
			addr = newAddr;
			for(DHTManagerListener i : listeners)
			{
				i.externalAddressChanged(newAddr);
			}
		}
	}
	
	private void runTasks()
	{
		synchronized (tasks)
		{
			try
			{
				while(tasks.size() > 0)
				{
					tasks.poll().run();
				}
			} catch (DistributedDatabaseException e)
			{
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void registerListener(DHTManagerListener listener)
	{
		if(!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public void unregisterListener(DHTManagerListener listener)
	{
		listeners.remove(listener);		
	}
	
	public DDBKeyValuePair getManagedDDBKeyValuePair(String key, String description)
	{
		if(!pairs.containsKey(key))
		{
			return new DDBKeyValuePair(this, key, description);
		} else
		{
			return pairs.get(key);
		}
	}

	interface DHTTask {
		void run() throws DistributedDatabaseException;
	}
	
	/**
	 * This method enqueues a DHT query until the DHT is available. In order execution is guaranteed
	 */
	void enqueueDHTAction(DHTTask toRun)
	{
		synchronized (tasks)
		{
			tasks.addLast(toRun);
		}
	}
	
	void unregisterKeyPair(String key)
	{
		pairs.remove(key);
	}

	public InetAddress getAddress()
	{
		return addr;
	}
}
