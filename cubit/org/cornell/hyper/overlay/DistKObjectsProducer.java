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

public class DistKObjectsProducer 
		extends AbstractProducer<Collection<MovieEntry>> 
		implements AsyncCallback {

	private RemoteHyperNode	remoteNode;
	private int				kMovies;
	private List<String>	keywords;
	
	private static Logger log = Logger.getLogger(
			DistKObjectsProducer.class.getName());	
	
	protected DistKObjectsProducer(
			RemoteHyperNode remoteNode,		// Node to ask 
			List<String> keywords, 			// Search keyword
			int kMovies, 					// K movies
			Consumer<Collection<MovieEntry>> consume) {
		super();
		this.remoteNode	= remoteNode;
		this.kMovies	= kMovies;
		this.keywords	= new ArrayList<String>(keywords);	
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
			log.warn("Incorrect result in DistKObjectsProducer");
			issueComplete(false);				
			return;	// Parse error
		}
		if (!issueCheck()) {
			return;	// Consumer no longer wants to know
		}
		// Unmarshal and collect all entries into the newMovies array
		ArrayList<MovieEntry> newMovies = new ArrayList<MovieEntry>();
		Object[] objArray = (Object[]) pResult;
		for (int i = 0; i < objArray.length; i++) {
			MovieEntry curEntry = MovieEntry.xmlUnMarshal(objArray[i]);					
			if (curEntry == null) {
				//log.warn("Incorrect return result in DistKObjectsProducer");
				//issueComplete(false);
				//return;	// Parse error
				continue;
			}
			newMovies.add(curEntry);
		}
		issueResult(newMovies);
		issueComplete(true);
	}

	public synchronized boolean issueProducer() {
		log.info("Issueing getKClosestMoviesAsync to " +
				remoteNode.getIP() + ":" + remoteNode.getPort() +
				" for keyword: " + keywords);
		XmlRPCClient.getKClosestMoviesAsync(
				remoteNode, kMovies, keywords, this);
		return true;
	}
}
