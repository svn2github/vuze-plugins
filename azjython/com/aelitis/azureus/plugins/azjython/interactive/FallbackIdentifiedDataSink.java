/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import com.aelitis.azureus.plugins.azjython.utils.ProxyDataSink;

/**
 *
 */
public class FallbackIdentifiedDataSink extends ProxyDataSink {
	public void put(Object o) {
		if (o instanceof IdentifiedIO) {throw new RuntimeException("shouldnt be wrapped already!");}
		super.put(new IdentifiedIO(o, Thread.currentThread()));
	}
}
