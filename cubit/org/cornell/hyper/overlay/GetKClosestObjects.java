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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

// Create a GetKClosestNodes for each keyword. Then issue a 
// DistKObjectsProducer for each node that it returns.
@SuppressWarnings("unchecked")
public class GetKClosestObjects implements Consumer {
	private Set<GetKClosestNodes> 	consumeSet;
	private List<String>			keywords;
	private Set<RemoteHyperNode>	newNodesSet;
	private Set<RemoteHyperNode>	pendingNodesSet;	
	private Set<RemoteHyperNode>	doneNodesSet;
	private Set<MovieEntry>			allMovieEntries;	
	private final EntryContainer	entryContainer;	
	private final int				kMovies;
	private final int				timeoutMS;
	private final int				charPerPerturb;
	private boolean					done			= false;
	private long					startTime 		= -1;
	private HyperNode				localNode 		= null;
	private final int				minKNodes		= 8;
	private final double			nodeTimeoutFrac	= 3.0;
	
	private static Logger log = Logger.getLogger(
			GetKClosestObjects.class.getName());
	
	private void initStructs(List<String> inKeywords) {
		allMovieEntries	= new HashSet<MovieEntry>();
		newNodesSet		= new HashSet<RemoteHyperNode>();
		pendingNodesSet	= new HashSet<RemoteHyperNode>();
		doneNodesSet	= new HashSet<RemoteHyperNode>();
		keywords 		= new ArrayList<String>(inKeywords);		
		consumeSet		= new HashSet<GetKClosestNodes>();		
	}
	
	// The nodes timeout should only be a fraction of the total query time,
	// as we need time to fetch the movie entries.
	private int getNodesTimeoutMS() {
		return (int)(timeoutMS /  nodeTimeoutFrac);  // Round down
	}
	
	public GetKClosestObjects(
			EntryContainer inEntryContainer,
			Set<RemoteHyperNode> candidateNodes, 
			Set<RemoteHyperNode> badNodes,
			int inKMovies, List<String> inKeywords, 
			int inCharPerPerturb, int maxThreadsPerKW, 
			int inTimeoutMS) {
		timeoutMS		= inTimeoutMS;
		kMovies			= inKMovies;
		charPerPerturb	= inCharPerPerturb;
		entryContainer	= inEntryContainer;
		initStructs(inKeywords);
		// Iterate through the keywords and get the K closest nodes for each.
		Iterator<String> keyIt = keywords.iterator();
		while (keyIt.hasNext()) {
			GetKClosestNodes curConsume = new GetKClosestNodes(
				candidateNodes, new HashSet<RemoteHyperNode>(), badNodes, 
				keyIt.next(), charPerPerturb, minKNodes, maxThreadsPerKW, 
				getNodesTimeoutMS(), false);			
			consumeSet.add(curConsume);
			curConsume.attachCB(this);
		}			
	}
	
	public GetKClosestObjects(
			HyperNode inLocalNode, int inKMovies, 
			List<String> inKeywords, int inCharPerPerturb, 
			int maxThreadsPerKW, int inTimeoutMS) {
		timeoutMS		= inTimeoutMS;
		kMovies			= inKMovies;
		charPerPerturb	= inCharPerPerturb;		
		localNode 		= inLocalNode;
		entryContainer	= localNode.getEntryContainer();
		initStructs(inKeywords);
		// Iterate through the keywords and get the K closest nodes for each.		
		Iterator<String> keyIt = keywords.iterator();
		while (keyIt.hasNext()) {
			GetKClosestNodes curConsume = new GetKClosestNodes(
				localNode, keyIt.next(), charPerPerturb, minKNodes, 
				maxThreadsPerKW, getNodesTimeoutMS(), false);					
			consumeSet.add(curConsume);
			curConsume.attachCB(this);
		}
	}
		
	private synchronized void blockOnAsync() {
		issueFetches();	// See if any nodes are ALREADY available
		long curTime = (new Date()).getTime();
		long remTime = timeoutMS - (curTime - startTime);			
		// Keep waiting if there is time remaining and there is 
		// still work to be done.
		while (remTime > 0 && 
				(consumeSet.size() > 0 || pendingNodesSet.size() > 0)) {			
			try {
				log.info("Blocking on GetKClosestMoviesConsumer");
				wait(remTime);	// Block for the remaining time
				log.info("GetKClosestMoviesConsumer unblocked");
			} catch (InterruptedException e) {
				break;
			}
			issueFetches();	// See if any nodes are available for fetches
			curTime = (new Date()).getTime();
			remTime = timeoutMS - (curTime - startTime);	
		}
		done = true;
	}	
								
	@SuppressWarnings("unchecked")
	public synchronized void producerComplete(Producer inProduce, boolean success) {
		if (inProduce instanceof GetKClosestNodes) {
			handleDoneNodesProducer((GetKClosestNodes)inProduce);
		} else if (inProduce instanceof DistKObjectsProducer) {
			handleDoneObjectsProducer((DistKObjectsProducer)inProduce);			
		} else {
			log.warn("Received unexpected producer in producerComplete");
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized boolean newProducerResult(Producer inProduce, Object result) {
		if (inProduce instanceof GetKClosestNodes) {
			return handleNewNode(
					(GetKClosestNodes)inProduce, (RemoteHyperNode) result);
		} else if (inProduce instanceof DistKObjectsProducer) {
			return handleNewObjects(
					(DistKObjectsProducer)inProduce, (Collection<MovieEntry>) result);			
		}
		log.warn("Received unexpected producer in newProducerResult");
		return false;
	}
	
	private synchronized void issueFetches() {
		log.info("In issueFetches");
		Iterator<RemoteHyperNode> nodeIt = newNodesSet.iterator();
		while (nodeIt.hasNext()) {
			RemoteHyperNode curNode = nodeIt.next();
			nodeIt.remove(); // Remove the node from newNodesSet
			if (localNode == null || !(localNode.getLocal().equals(curNode))) {			
				DistKObjectsProducer curProd = new 
					DistKObjectsProducer(curNode, keywords, kMovies, this);			
				if (curProd.issueProducer()) {
					pendingNodesSet.add(curNode);
				} else {				
					log.warn("Error issuing DistKObjectsProducer");
					doneNodesSet.add(curNode);
				}
			} else {
				// Local node, just fetch locally
				allMovieEntries.addAll(
					localNode.getLocalKClosestEntries(kMovies, keywords));
				doneNodesSet.add(curNode);
			}						
		}
	}		
		
	private synchronized void handleDoneObjectsProducer(DistKObjectsProducer inProduce) {
		if (!pendingNodesSet.contains(inProduce.getNode())) {
			log.warn("Received unexpected producer");			
		} else {
			pendingNodesSet.remove(inProduce.getNode());
			doneNodesSet.add(inProduce.getNode());
		}
		notify();
	}

	private synchronized void handleDoneNodesProducer(GetKClosestNodes inProduce) {
		consumeSet.remove(inProduce);
		notify();
	}	

	private synchronized boolean handleNewObjects(
			DistKObjectsProducer inProduce, Collection<MovieEntry> movies) {
		log.info("In handleNewObjects");
		if (done) { return false; }
		Iterator<MovieEntry> movieIt = movies.iterator();
		while (movieIt.hasNext()) {
			MovieEntry newEntry = movieIt.next();
			log.info("Returned movie result: " + newEntry.getMovName());
			allMovieEntries.add(newEntry);
		}
		//notify();
		return true;
	}

	private synchronized boolean handleNewNode(
			GetKClosestNodes inProduce, RemoteHyperNode curNode) {
		log.info("In handleNewNode");
		if (done) { return false; }
		if (!newNodesSet.contains(curNode) 		&&
			!pendingNodesSet.contains(curNode) 	&&
			!doneNodesSet.contains(curNode)) {
			newNodesSet.add(curNode);
		}
		notify(); // See if we can issue new fetches
		return true;
	}
	
	public synchronized boolean checkConsumer(Producer inProduce) {
		return !done;
	}	
	
	private synchronized MovieEntry getMinEntry(
			Set<MovieEntry> availEntries, Set<MovieEntry> excludeEntries) {
		int minDist = -1;
		MovieEntry minEntry = null;
		Iterator<MovieEntry> entryIt = availEntries.iterator();
		while (entryIt.hasNext()) {
			MovieEntry curEntry = entryIt.next();
			if (excludeEntries.contains(curEntry)) {
				continue;
			}
			int dist = entryContainer.computeDistSum(
					curEntry.getMovName(), keywords);
			if (minDist == -1 || dist < minDist) {
				minDist = dist;
				minEntry = curEntry;
			}
		}
		return minEntry;
	}
					
	public synchronized List<MovieEntry> getConsumeResult() {				
		log.info("Find the top K movies");
		List<MovieEntry> retList = new ArrayList<MovieEntry>();
		Set<MovieEntry>	exclude = new HashSet<MovieEntry>();
		//for (int i = 0; i < kMovies; i++) {
		while (true) {
			MovieEntry curEntry = getMinEntry(allMovieEntries, exclude);
			if (curEntry == null) {
				break;
			}
			retList.add(curEntry);
			exclude.add(curEntry);
		}
		return retList;			
	}
	
	// Start getting the K closest nodes for each keyword. Keep
	// track of the start time to know when to timeout.
	public synchronized void startGetBoundNodes() {
		startTime = (new Date()).getTime();
		System.out.println("consumeSet size: " + consumeSet.size());
		Iterator<GetKClosestNodes> curIt = 
			new HashSet<GetKClosestNodes>(consumeSet).iterator();
		while (curIt.hasNext()) {
			curIt.next().issueProducer();
		}
		blockOnAsync();
	}	
}
