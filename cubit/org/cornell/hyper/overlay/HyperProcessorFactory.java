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

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory;

public class HyperProcessorFactory extends
		RequestSpecificProcessorFactoryFactory {
	// the object we are going to be passing to our exposed methods
	private HyperNode hyperNode = null;

	public HyperProcessorFactory(HyperNode hyperNode){
		this.hyperNode = hyperNode;
	}

	@SuppressWarnings("unchecked")
	protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest)
			throws XmlRpcException {	
		RemoteHyperInterface proc = 
			(RemoteHyperInterface) super.getRequestProcessor(pClass, pRequest);
		// pass our object to the init method of our 
		// exposed methods interface.
		proc.init(hyperNode);
		return proc;
	}
}
