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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface Producer<T> {
	public boolean issueProducer();
	public void attachCB(Consumer<T> inConsume);
	//public T getProduceResult();
	//public boolean done();
}

abstract class AbstractProducer<T> implements Producer<T> {
	private List<Consumer<T>> consumerList;
	
	public AbstractProducer() {
		consumerList = new ArrayList<Consumer<T>>();
	}
	
	protected synchronized void issueResult(T result) {
		Iterator<Consumer<T>> it = consumerList.iterator();
		while (it.hasNext()) {
			boolean contProd = it.next().newProducerResult(this, result);
			if (!contProd) {
				it.remove(); // This consumer requests to be removed
			}
		}	
	}
	
	protected synchronized void issueComplete(boolean success) {
		Iterator<Consumer<T>> it = consumerList.iterator();
		while (it.hasNext()) {
			it.next().producerComplete(this, success);
		}
	}
	
	public synchronized void attachCB(Consumer<T> inConsume) {
		consumerList.add(inConsume);	
	}
	
	public synchronized boolean issueCheck() {
		Iterator<Consumer<T>> it = consumerList.iterator();
		while (it.hasNext()) {
			if (it.next().checkConsumer(this)) {
				return true;
			}
		}
		return false;
	}
	
	public synchronized boolean consumersRemaining() {
		if (consumerList.size() > 0) {
			return true;		
		}
		return false;
	}
}

/*
abstract class AbstractProducerRPC<T> implements Producer<T>, AsyncCallback{
	
	protected Consumer<?> consume;
	
	AbstractProducerRPC(Consumer<?> inConsume) {
		consume = inConsume;
	}
	
	protected void producerComplete(boolean success) {
		consume.finiProducer(this, success);
	}

	//public void handleError(XmlRpcRequest pRequest, Throwable pError);

	//public void handleResult(XmlRpcRequest pRequest, Object pResult);		
}
*/