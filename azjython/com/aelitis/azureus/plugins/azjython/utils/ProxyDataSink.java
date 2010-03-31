/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.utils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Allan Crooks
 *
 */
public class ProxyDataSink implements DataSink {

	private DataSink delegate = null;
	private boolean setup = false;
	private ArrayList queue = new ArrayList();
	
	public void put(Object o) {
		if (setup) {delegate.put(o);}
		else {queue.add(o);}
	}
	
	public void setDelegate(DataSink sink) {
		if (setup) {throw new RuntimeException("sink already set");}
		this.delegate = sink;
		this.setup = true;
		Iterator itr = queue.iterator();
		while (itr.hasNext()) {
			this.put(itr.next());
		}
		queue = null;
	}

}
