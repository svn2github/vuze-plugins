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
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.AsyncCallback;

public class DistKNodesProducer 
		extends AbstractProducer<Collection<RemoteHyperNode>> 
		implements AsyncCallback {	
	final private int 				distBound;
	final private int				kVal;
	final private RemoteHyperNode	remoteNode;
	final private String			keyword;
	private List<RemoteHyperNode> 	nodeArray;
	
	private static Logger log = Logger.getLogger(
			DistKNodesProducer.class.getName());

	public DistKNodesProducer(
			RemoteHyperNode remoteNode,		// Node to ask 
			String keyword, 				// Search keyword
			int distBound, 					// Edit-distance bound
			int kVal, 						// K closest
			Consumer<Collection<RemoteHyperNode>> consume) {
		super();
		this.distBound 	= distBound;
		this.kVal		= kVal;
		this.remoteNode	= remoteNode;
		this.keyword	= keyword;	
		nodeArray 		= new ArrayList<RemoteHyperNode>();
		attachCB(consume);		
	}

	// Not synchronized as remoteNode is final and RemoteHyperNode is immutable.
	public RemoteHyperNode getNode() {
		return remoteNode;
	}
	
	public synchronized void handleError(
			XmlRpcRequest pRequest, Throwable pError) {
		issueComplete(false);
	}

	public synchronized void handleResult(
			XmlRpcRequest pRequest, Object pResult) {
		if (!(pResult instanceof Object[])) {
			log.warn("Incorrect return result in DistKNodesProducer");
			issueComplete(false);
			return;
		}
		if (!issueCheck()) {
			return;	// Consumer no longer wants to know
		}		
		// Convert each object into a RemoteHyperNode
		Object[] objArray = (Object[]) pResult;
		for (int i = 0; i < objArray.length; i++) {
			RemoteHyperNode curNode = 
				RemoteHyperNode.convert(objArray[i]);
			if (curNode == null) {
				log.warn("Incorrect return result in DistKNodesProducer");
				issueComplete(false);
				return;		
			}
			nodeArray.add(curNode);
		}
		issueResult(nodeArray);
		issueComplete(true);
	}	
	
	public synchronized boolean issueProducer() {
		log.info("Issueing getDistKNodesAsync to " +
				remoteNode.getIP() + ":" + remoteNode.getPort() +
				" for keyword: " + keyword);
		XmlRPCClient.getDistKNodesAsync(
				remoteNode, keyword, distBound, kVal, this);
		return true;
	}
}
