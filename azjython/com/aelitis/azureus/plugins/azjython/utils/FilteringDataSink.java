/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.utils;

/**
 * @author allan
 *
 */
public abstract class FilteringDataSink implements DataSink {
	
	protected DataSink sink;
	
	public FilteringDataSink(DataSink sink) {
		this.sink = sink;
	}

	public void put(Object o) {
		Object obj = filter(o);
		if (obj != null) {sink.put(obj);}
	}
	
	public abstract Object filter(Object o);

}
