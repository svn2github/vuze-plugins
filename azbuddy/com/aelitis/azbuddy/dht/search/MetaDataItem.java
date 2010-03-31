package com.aelitis.azbuddy.dht.search;

import java.util.Comparator;

import com.aelitis.azbuddy.dht.DDBKeyValuePair;
import com.aelitis.azbuddy.dht.DHTManager;


public final class MetaDataItem {
	final String metakey;
	final String value;

	/**
	 * uniquenessWeight is expressed in reads per minute that hit a key
	 */
	int uniquenessWeight;
	DDBKeyValuePair kvp;
	boolean isUpdated = false;
	
	/**
	 * 
	 * @param metakey the property described by the value (e.h. first name, file format, language or similar categories)
	 * @param value 
	 */		
	public MetaDataItem(final String metakey, final String value)
	{
		this.metakey = metakey;
		this.value = value.toLowerCase();
	}
	
	void queryWeights(DHTManager dhtMan)
	{
		if(kvp == null)
			kvp = dhtMan.getManagedDDBKeyValuePair("azbuddy:keywordHitcounter:"+metakey+":"+value, "Hitcounter for keyword: "+metakey+":"+value);
		kvp.refresh(60, false);
	}
	
	void updateWeight()
	{
		
	}
	
	
	static final class WeightSorter implements Comparator<MetaDataItem> {
		public int compare(MetaDataItem arg0, MetaDataItem arg1)
		{
			return arg0.uniquenessWeight-arg1.uniquenessWeight;
		}
	};
	
	static final class LexicalSorter implements Comparator<MetaDataItem> {
		public int compare(MetaDataItem arg0, MetaDataItem arg1)
		{
			return (arg0.metakey+arg0.value).compareTo(arg1.metakey+arg1.value);
		}
	}
}
