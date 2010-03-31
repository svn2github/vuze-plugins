package com.aelitis.azbuddy.dht.search;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.dht.DDBKeyValuePair;
import com.aelitis.azbuddy.dht.DHTManager;
import com.aelitis.azbuddy.dht.DHTManagerListener;

public abstract class KeywordSearch {
	
	static final int SEARCH_FAILED = -1;
	static final int SEARCH_UPDATE_AND_ESTIMATE_KEYWORD_WEIGHTS = 1;
	static final int SEARCH_CREATE_TUPLES = 2;
	static final int SEARCH_DO_DDB_QUERIES = 3;
	static final int SEARCH_DONE = 4;
	
	
	static final int TWO_KEYWORD_TUPLE_WEIGHT_THRESHOLD = 5; // 5 reads per minute
	
	
	
	final static DHTManager dhtMan = BuddyPlugin.getDHTMan();
	
	int state;
	final MetaDataItem[] terms;
	final ArrayList<MetaDataNTuple> tuples = new ArrayList<MetaDataNTuple>();
	final ArrayList<DDBKeyValuePair> results = new ArrayList<DDBKeyValuePair>();
	
	final KeywordSearchListener listener;
	
	final DHTManagerListener dhtListener = new DHTManagerListener()
	{
		public void dhtUsabilityChanged(boolean isUsable) {	}
		public void externalAddressChanged(InetAddress newAddress) { }
		public void operationFinished(final DDBKeyValuePair handler, int flags)
		{
			if(state == SEARCH_UPDATE_AND_ESTIMATE_KEYWORD_WEIGHTS)
			{
				boolean updatesDone = true;
				for(MetaDataItem i : terms)
				{
					if(handler == i.kvp)
					{
						// hit the counter
						if((flags & (DHTManagerListener.READ_QUERY | DHTManagerListener.QUERY_DONE)) != 0)
							handler.refreshStats(60);
						// fetch the counter
						if((flags & (DHTManagerListener.STATS_QUERY | DHTManagerListener.QUERY_DONE)) != 0)
						{
							i.updateWeight();
						}
						if((flags & DHTManagerListener.QUERY_DONE) == 0)
							searchFailed();
					}
					updatesDone &= i.isUpdated;
				}
				if(updatesDone)
				{
					createTuples();
					doTupleQueries();
				}
			}
			if(state == SEARCH_DO_DDB_QUERIES)
			{
				for(MetaDataNTuple i : tuples)
				{
					//if((flags & (DHTManagerListener.READ_QUERY | DHTManagerListener.QUERY_DONE)) != 0))
				}
			}
		}
	};
	
	KeywordSearch(final List<MetaDataItem> terms)
	{
		listener = null; // TODO fix that
		this.terms = (MetaDataItem[])terms.toArray();
		Collections.sort(terms, new MetaDataItem.LexicalSorter());
	}
	
	
	abstract void createTuples();
	abstract void doTupleQueries();
	abstract void parseResults();
	
	private void searchFailed()
	{
		state = SEARCH_FAILED;
	}
	
	

	
}
