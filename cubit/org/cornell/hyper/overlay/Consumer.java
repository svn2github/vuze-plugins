/******************************************************************************
Cubit distribution
Copyright (C) 2008 Bernard Wong

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

The copyright owner can be contacted by e-mail at bwong@cs.cornell.edu
*******************************************************************************/

package org.cornell.hyper.overlay;


public interface Consumer<T> {
	// If returns false, tells producer to stop
	public boolean newProducerResult(Producer<T> inProduce, T result);
	// If returns false, tells producer to stop
	public boolean checkConsumer(Producer<T> inProduce);
	public void producerComplete(Producer<T> inProduce, boolean success);
	//public boolean blockOnAsync();
}

/*
public interface Consumer<T> {
	public void finiProducer(Producer<?> inProduce, boolean success);
	public boolean blockOnAsync();
	public T getConsumeResult();
}
abstract class AbstractProducerConsumer<T> extends Thread 
		implements Producer<T>, Consumer<T> {
	
	protected Consumer<T> consume;
	
	AbstractProducerConsumer(Consumer<T> inConsume) {
		consume = inConsume;
	}	
	
	protected void producerComplete(boolean success) {
		consume.producerComplete(this, success);
	}
	
	public void run() {		
		producerComplete(blockOnAsync());
	}
}
*/
