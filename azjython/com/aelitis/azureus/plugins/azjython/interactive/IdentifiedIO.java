/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

/**
 * @author allan
 */
public class IdentifiedIO {
	public Object object;
	public Thread thread;
	
	public IdentifiedIO(Object object, Thread thread) {
		this.object = object; this.thread = thread;
	}
}
