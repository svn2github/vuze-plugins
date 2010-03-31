package com.aelitis.azbuddy.dht;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKeyStats;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;

import com.aelitis.azbuddy.ui.UILogger;
import com.aelitis.azbuddy.utils.ByteArray;
import com.aelitis.azbuddy.dht.DHTManager.DHTTask;

public class DDBKeyValuePair {
	
	public static final int IS_FREQUENCY_DIVERSIFIED = 1;
	public static final int IS_SIZE_DIVERSIFIED = 2;
	
	private int diversificationStatus;
	private double avgReadsPerMinute;
	private double avgNbPuts;
	

	private static final Comparator<DistributedDatabaseValue> DDBValueSorter = new Comparator<DistributedDatabaseValue>() {
		public int compare(DistributedDatabaseValue arg0, DistributedDatabaseValue arg1)
		{
			return (int)(arg0.getVersion()-arg1.getVersion());
		}
	};

	private final DHTManager manager; 

	private final String rawKey;
	private String description;
	private DistributedDatabaseValue[] getValues;


	DDBKeyValuePair(DHTManager man, String key, final String descr)
	{
		manager = man;
		rawKey = key;
		description = descr;
	}

	public void putValue(ByteBuffer toSend) { putValue(toSend.array()); }
	public void putValue(ByteArray toSend) { putValue(toSend.getArray()); }
	public void putValue(final byte[] toSend)
	{
		manager.enqueueDHTAction(new DHTTask() {
			public void run() throws DistributedDatabaseException
			{
					final DistributedDatabaseValue toPut = manager.ddbPi.createValue(toSend);
					manager.ddbPi.write(new WriteLookupListener(false), manager.ddbPi.createKey(rawKey,"BuddyPlugin single put: "+description), toPut);
			}
		});

	}

	public void putValues(final byte[][] toSend)
	{
		manager.enqueueDHTAction(new DHTTask() {
			public void run() throws DistributedDatabaseException
			{
					final DistributedDatabaseValue[] toPut = new DistributedDatabaseValue[toSend.length];
					for(int i=0;i<toPut.length;i++)
					toPut[i] = manager.ddbPi.createValue(toSend[i]);
					manager.ddbPi.write(new WriteLookupListener(false), manager.ddbPi.createKey(rawKey,"BuddyPlugin multi put: "+description), toPut);
			}
		});
	}

	public void deleteValue()
	{
		manager.enqueueDHTAction(new DHTTask() {
			public void run() throws DistributedDatabaseException
			{
				manager.ddbPi.delete(new WriteLookupListener(true), manager.ddbPi.createKey(rawKey,"BuddyPlugin delete: "+description));
			}
		});
	}

	public void refresh(final long timeout, final boolean exhaustiveGet)
	{
		manager.enqueueDHTAction(new DHTTask() {
			public void run() throws DistributedDatabaseException
			{
				manager.ddbPi.read(new ReadLookupListener(), manager.ddbPi.createKey(rawKey,"BuddyPlugin get: "+description), timeout, exhaustiveGet ? DistributedDatabase.OP_EXHAUSTIVE_READ : DistributedDatabase.OP_NONE);
			}
		});
	}
	
	public void refreshStats(final long timeout)
	{
		manager.enqueueDHTAction(new DHTTask() {
			public void run() throws DistributedDatabaseException
			{
				manager.ddbPi.readKeyStats(new StatsLookupListener(), manager.ddbPi.createKey(rawKey,"BuddyPlugin stats get: "+description), timeout);
			}
			
		});
	}
	
	private void updateStats(ArrayList<DistributedDatabaseKeyStats> statList)
	{
		DistributedDatabaseKeyStats stat;
		int sumReadsPerMin = 0;
		int sumNbPuts = 0;
		
		diversificationStatus = 0;
		for(int i=0;i<statList.size();i++)
		{
			stat = statList.get(i);
			if((stat.getDiversification() & DistributedDatabase.DT_SIZE) != 0)
				diversificationStatus |= IS_SIZE_DIVERSIFIED;
			if((stat.getDiversification() & DistributedDatabase.DT_FREQUENCY) != 0)
				diversificationStatus |= IS_FREQUENCY_DIVERSIFIED;
			sumReadsPerMin += stat.getReadsPerMinute();
			sumNbPuts += stat.getEntryCount();
		}
		
		avgReadsPerMinute = (double)sumReadsPerMin/statList.size();
		avgNbPuts = (double)sumNbPuts/statList.size();
	}

	public DistributedDatabaseValue getLatestValue()
	{
		if(getValues.length > 0)
			return getValues[getValues.length-1];
		else
			return null;
	}

	public ByteArray getLatestValueAsArray()
	{
		if(getValues.length > 0)
		{
			try
			{
				return new ByteArray((byte[])getValues[getValues.length-1].getValue(byte[].class));
			} catch (DistributedDatabaseException e)
			{
				Debug.printStackTrace(e);
			}
			
		}
		return null;
	}

	public ByteArray[] getValuesAsArray()
	{
		if(getValues.length > 0)
		{
			ByteArray[] rawdata = new ByteArray[getValues.length];
			try
			{
				for(int i=0;i<rawdata.length;i++)
				{
					rawdata[i] = new ByteArray((byte[])getValues[i].getValue(byte[].class));
				}
			} catch (DistributedDatabaseException e)
			{
				Debug.printStackTrace(e);
				return null;
			}
			return rawdata;
		}
		return null;
	}

	public DistributedDatabaseValue[] getValues()
	{
		if(getValues.length > 0)
			return getValues.clone();
		return null;
	}

	public int getNbRemoteValues()
	{
		return getValues != null ? getValues.length : 0;
	}
	
	public void destroy()
	{
		manager.unregisterKeyPair(rawKey);
	}
	
	public String getRawKey()
	{
		return rawKey;
	}
	
	public double getAvgNbPuts()
	{
		return avgNbPuts;
	}

	public double getAvgReadsPerMinute()
	{
		return avgReadsPerMinute;
	}

	public int getDiversificationStatus()
	{
		return diversificationStatus;
	}
	
	public void setDescription(String newDesc)
	{
		description = newDesc;
	}
	
	private final class ReadLookupListener implements DistributedDatabaseListener
	{
		private final ArrayList<DistributedDatabaseValue> buffer = new ArrayList<DistributedDatabaseValue>();
		public final void event(DistributedDatabaseEvent event)
		{
			UILogger.log("DDB read event:"+event.getType());
			if(event.getType() == DistributedDatabaseEvent.ET_VALUE_READ)
				buffer.add(event.getValue());
			if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE)
			{
				// replace current value list with sorted new one
				Collections.sort(buffer, DDBValueSorter);
				DistributedDatabaseValue[] newArr = new DistributedDatabaseValue[buffer.size()];
				getValues = buffer.toArray(newArr);

				//notify listeners of finished read operation
				for(DHTManagerListener i : manager.listeners)
					i.operationFinished(DDBKeyValuePair.this, DHTManagerListener.READ_QUERY | DHTManagerListener.QUERY_DONE);
			}
			if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT)
				for(DHTManagerListener i : manager.listeners)
					i.operationFinished(DDBKeyValuePair.this, DHTManagerListener.READ_QUERY);
		}
	}

	private final class WriteLookupListener implements DistributedDatabaseListener
	{
		private final int type;
		private WriteLookupListener(boolean isDelete)
		{
			type = isDelete ? DHTManagerListener.DELETE_QUERY : DHTManagerListener.READ_QUERY; 
		}
		public final void event(DistributedDatabaseEvent event)
		{
			//UILogger.log("DDB write event:"+event.getType());
			if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE)
				for(DHTManagerListener i : manager.listeners)
					i.operationFinished(DDBKeyValuePair.this, type | DHTManagerListener.QUERY_DONE);
			if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT)
				for(DHTManagerListener i : manager.listeners)
					i.operationFinished(DDBKeyValuePair.this, type);
		}
	}
	
	private final class StatsLookupListener implements DistributedDatabaseListener
	{
		private final ArrayList<DistributedDatabaseKeyStats> stats = new ArrayList<DistributedDatabaseKeyStats>();
		public final void event(DistributedDatabaseEvent event)
		{
			UILogger.log("DDB stats event:"+event.getType());
			if(event.getType() == DistributedDatabaseEvent.ET_KEY_STATS_READ)
				stats.add(event.getKeyStats());
			if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE)
			{
				updateStats(stats);
				for(DHTManagerListener i : manager.listeners)
					i.operationFinished(DDBKeyValuePair.this, (DHTManagerListener.STATS_QUERY | DHTManagerListener.QUERY_DONE));
			}
			if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT)
				for(DHTManagerListener i : manager.listeners)
					i.operationFinished(DDBKeyValuePair.this, (DHTManagerListener.STATS_QUERY));
		}
	}
}
