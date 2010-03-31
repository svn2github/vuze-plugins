/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.utils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Allan Crooks
 */
public class MultisourceDataSink implements DataSink {
	
	private ArrayList data_sinks;
	public MultisourceDataSink() {
		data_sinks = new ArrayList();
	}

	public synchronized void put(Object o) {
		Iterator itr = data_sinks.iterator();
		while (itr.hasNext()) {
			((DataSink)itr.next()).put(o);
		}
	}
	
	public synchronized void addSink(DataSink ds) {
		data_sinks.add(ds);
	}
	
	public synchronized void delSink(DataSink ds) {
		data_sinks.remove(ds);
	}

}
