package com.aelitis.azbuddy.dht.search;

import java.util.Arrays;

import com.aelitis.azbuddy.dht.DDBKeyValuePair;
import com.aelitis.azbuddy.dht.DHTManager;

public class MetaDataNTuple {
	final MetaDataItem[] items;
	final DDBKeyValuePair keyMapping;
	
	public MetaDataNTuple(MetaDataItem[] items, DHTManager man)
	{
		Arrays.sort(items,new MetaDataItem.LexicalSorter());
		this.items = items;
		StringBuilder buf = new StringBuilder("azbuddy:searchtuple:");
		for(MetaDataItem i : items)
		{
			buf.append(i.metakey);
			buf.append(i.value);
		}
		keyMapping = man.getManagedDDBKeyValuePair(buf.toString(), buf.toString());
	}
}
