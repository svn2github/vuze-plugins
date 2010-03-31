/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.utils;

import java.util.LinkedList;

/**
 * Taken and modified from:
 *    http://www.exampledepot.com/egs/java.lang/WorkQueue.html
  */
public class WorkQueue implements DataSink {
    LinkedList queue = new LinkedList();

    // Add work to the work queue
    public synchronized void put(Object o) {
        push(o);
        notifyAll();
    }

    // Retrieve work from the work queue; block if the queue is empty
    public synchronized Object get() throws InterruptedException {
        while (isEmpty()) {
            wait();
        }
        return pop();
    }
    
    public synchronized void destroy() {
    	this.queue = null;
    	notifyAll();
    }
    
    private boolean isEmpty() {
    	return (queue == null) ? false : queue.isEmpty();
    }
    
    private Object pop() {
    	if (queue == null) {return null;}
    	return queue.removeFirst();
    }
    
    private void push(Object o) {
    	if (queue != null) {queue.addLast(o);}
    }
    
}