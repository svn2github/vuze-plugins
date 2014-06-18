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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.cornell.hyper.overlay.DistKNodesProducer;

/**
 * Collects the K closest nodes, or optionally, all nodes within some
 * distance bounds, to a given target string.
 * 
 * @author Bernard Wong
 */
public class GetKClosestNodes 
		extends AbstractProducer<RemoteHyperNode>
		implements Consumer<Collection<RemoteHyperNode>>, Runnable {
	
	private String					keyword;
	private	int						distBound;
	private int						kVal;
	private int						maxThreads;	
	private int						timeoutMS;
		
	private Set<RemoteHyperNode>	candidateNodes;
	private Set<RemoteHyperNode>	pendingNodes;
	private Set<RemoteHyperNode>	checkedNodes;
	private Set<RemoteHyperNode>	badNodes;
	private Set<RemoteHyperNode>	sentNodes;
		
	protected boolean				done 		= false;	
	protected RemoteHyperNode		localNode 	= null;
	private long					startTime	= -1;	
	private boolean					noConsumer	= false;
	
	// If the node distance is less than maxSearchDist and meets 
	// the characters per perturbation requirement, it is sent
	// to the consumer immediately.
	private final int				maxSearchDist = 4;
	
	private static Logger log = Logger.getLogger(
			GetKClosestNodes.class.getName());
		
	/**
	 * Constructor for GetKClosestNodes given candidate nodes
	 */
	public GetKClosestNodes(
			Set<RemoteHyperNode> inCandidateNodes,
			Set<RemoteHyperNode> inCheckedNodes,
			Set<RemoteHyperNode> inBadNodes,
			String keyword, int charPerPerturb, 
			int kVal, int maxThreads, int timeoutMS,
			boolean noConsumer) {
		super();
		this.keyword	= keyword;	
		this.maxThreads	= maxThreads;
		this.timeoutMS	= timeoutMS;
		this.kVal		= kVal;
		this.noConsumer	= noConsumer;
		
		// Compute the distance bound based on the expected
		// characters per perturbation.
		distBound = computeDistBound(charPerPerturb);
				
		candidateNodes	= new HashSet<RemoteHyperNode>(inCandidateNodes);
		badNodes		= new HashSet<RemoteHyperNode>(inBadNodes);				
		pendingNodes 	= new HashSet<RemoteHyperNode>();
		checkedNodes	= new HashSet<RemoteHyperNode>(inCheckedNodes);
		sentNodes		= new HashSet<RemoteHyperNode>();
		
		log.info("Search distance bound and K for keyword " 
				+ this.keyword + " is: " + distBound + ", " + kVal);
	}	
		
	/**
	 * Constructor for GetKClosestNodes given the full node info.
	 */
	public GetKClosestNodes(
			HyperNode inLocalNode, String keyword, 
			int charPerPerturb, int kVal, 
			int maxThreads, int timeoutMS, 
			boolean noConsumer) {
		this(new HashSet<RemoteHyperNode>(),
			 new HashSet<RemoteHyperNode>(),
			 new HashSet<RemoteHyperNode>(),
			 keyword, charPerPerturb, kVal, 
			 maxThreads, timeoutMS, noConsumer);
		candidateNodes.addAll(
				inLocalNode.getLocalDistBoundNodes(keyword, distBound));
		candidateNodes.addAll(
				inLocalNode.getLocalKClosestNodes(keyword, kVal));					
		localNode = inLocalNode.getLocal();
	}	
	
	/**
	 * Convenience method for getting the current time
	 * 
	 * @return	The number of milliseconds since the Unix epoch.
	 */
	private static long getTime() { return (new Date()).getTime(); }	
	
	/**
	 * Convenience method that calculates the search bounds given
	 * an estimated number of characters per perturbation to the
	 * search keyword.
	 * 
	 * @param charPerPerturb	Characters per perturbation
	 * @return					Search bounds
	 */
	private int computeDistBound(int charPerPerturb) {
		return Math.min(maxSearchDist, (int)(Math.ceil(
			this.keyword.length() / (double)charPerPerturb)));
	}
	
	/** 
	 * Add a node to the checked node set
	 * 
	 * @param curNode	Node to be added to the checked list
	 */
	private synchronized void addCheckedNode(RemoteHyperNode curNode) {
		log.info("In addCheckedNode");						
		if (!checkedNodes.contains(curNode)) {
			checkedNodes.add(curNode);
		}
	}
	
	/** 
	 * Check if any of the checked nodes are close enough to be
	 * sent up to the consumer immediately rather than waiting
	 * for the producer to fully complete.
	 */
	private synchronized void checkNodesToSend() {
		Iterator<RemoteHyperNode> it = checkedNodes.iterator();
		while (it.hasNext()) {
			RemoteHyperNode curNode = it.next();
			if (sentNodes.contains(curNode)) {
				continue;	// Skip sent nodes
			}
			int curNodeDist = 
				EditDistance.computeEditDistance(keyword, curNode.getKey());
			if (curNodeDist <= distBound) {
				issueResult(curNode);
				sentNodes.add(curNode);
			}
		}
	}
	
	/** 
	 * Add the collection of nodes to the candidate set
	 */
	private synchronized void addCandidateNodes(
			Collection<RemoteHyperNode> nodeList) {
		log.info("In GetDistAndKConsumer.addCandidateNodes");	
		Iterator<RemoteHyperNode> nodeIt = nodeList.iterator();
		while (nodeIt.hasNext()) {
			RemoteHyperNode curNode = nodeIt.next();
			log.info("New node is: " + curNode.getIP() + ":" + curNode.getPort());
			if (badNodes.contains(curNode) 		|| 
				checkedNodes.contains(curNode)	||
				pendingNodes.contains(curNode)) {
				log.info("Skipping node");
				continue;	// Already processed or processing node
			}
			log.info("Adding to candidate set");
			candidateNodes.add(curNode);
		}
	}	
	
	/**
	 * Check if a node should be added to the pendingQueue
	 *  
	 * @param curNode	Remote node to check if it should be queried
	 * @return			whether not not the node should be queried.
	 */
	private synchronized boolean shouldAddToPending(RemoteHyperNode curNode) {
		int curDist = EditDistance.computeEditDistance(keyword, curNode.getKey());			
		int numCloserEqual = 0;			
		Iterator<RemoteHyperNode> checkedIt = checkedNodes.iterator();
		while (checkedIt.hasNext() && numCloserEqual < kVal) {
			RemoteHyperNode curChecked = checkedIt.next();
			// Check if this checked node is already closer/equal than 
			// this pending node. If it is, add it to the number of  
			// checked nodes is already known to be closer.
			// 
			// NOTE: This has been changed to *just* closer, not counting
			// distances that are equal. This makes it allow potentially 
			// more than the requested nodes.
			//
			// TODO: May want to experiment with whether it should be strictly
			// closer or closer and equal for the check.
			int curCheckedDist = EditDistance.computeEditDistance(
					keyword, curChecked.getKey());
			if (curCheckedDist < curDist) {
				numCloserEqual++;
			}
		}
		// If there are less than the desired amount of already checked
		// nodes that are closer/equal to the keyword than this one, then 
		// add this node to pending.
		return (numCloserEqual < kVal);
	}
	
	// Issue producers up to maxThreads
	private synchronized void startProducers() {
		log.info("In GetDistAndKConsumer.startProducers");
		Iterator<RemoteHyperNode> nodeIt = candidateNodes.iterator();
		while (nodeIt.hasNext() && pendingNodes.size() < maxThreads) {
			RemoteHyperNode curNode = nodeIt.next();
			nodeIt.remove();	// Remove it from candidateNodes
			// Skip if this node has been checked or is bad. This
			// can happen if one of the closest nodes is listed as a 
			// bad node, or if the local node is in the candidate set. 
			// These events should not happen normally.
			if (checkedNodes.contains(curNode) 	|| 
				badNodes.contains(curNode)		||
				pendingNodes.contains(curNode)) {
				continue;
			}
			// If we already have found enough nodes, don't need to add
			// a node that is outside of the distance bound.
			int curNodeDist = 
				EditDistance.computeEditDistance(keyword, curNode.getKey());
			if (curNodeDist > distBound && !shouldAddToPending(curNode)) {
				continue;
			}				
			pendingNodes.add(curNode);
			log.info("Creating producer to " + curNode.getIP() + ":"
					+ curNode.getPort() + " for: " + keyword);
			DistKNodesProducer curDistProduce = new DistKNodesProducer(
					curNode, keyword, distBound, kVal, this);
			if (!curDistProduce.issueProducer()) {
				pendingNodes.remove(curNode);
			}
		}
	}	

	// Send back the closest K nodes (including ties) back to the consumer.
	// This is called when the producer is complete.
	private synchronized void sendTopKChecked() {
		// First insert all the hyper nodes into a tree, ordered by
		// its distance to the keyword
		TreeMap<Integer, List<RemoteHyperNode>> sortedMap = 
			new TreeMap<Integer, List<RemoteHyperNode>>();		
		Iterator<RemoteHyperNode> checkedIt = checkedNodes.iterator();
		while (checkedIt.hasNext()) {
			RemoteHyperNode curNode = checkedIt.next();
			int curNodeDist = 
				EditDistance.computeEditDistance(keyword, curNode.getKey());
			if (!sortedMap.containsKey(curNodeDist)) {
				sortedMap.put(curNodeDist, new ArrayList<RemoteHyperNode>());
			}
			sortedMap.get(curNodeDist).add(curNode);
		}
		// Find the top k items and send them to the consumer. For now, 
		// let's send ties back as well (so the K limit is not strict).
		int totalSent = 0;
		Iterator<Integer> treeIt = sortedMap.keySet().iterator();
		while (treeIt.hasNext() && totalSent < kVal) {
			List<RemoteHyperNode> curList = sortedMap.get(treeIt.next());
			Iterator<RemoteHyperNode> listIt = curList.iterator();
			while (listIt.hasNext()) {
				totalSent++; // Increment even if the node has been sent
				RemoteHyperNode curNode = listIt.next();
				if (!sentNodes.contains(curNode)) {
					issueResult(curNode);
					sentNodes.add(curNode);
				}
			}
		}
	}
		
	// Block and wait for results to come back, or the producer to timeout.
	private synchronized boolean blockOnAsync() {
		if (startTime == -1) 	{ startTime = getTime(); 	 }			
		if (localNode != null) 	{ addCheckedNode(localNode); }
		checkNodesToSend();		// Check if this node is close enough
		startProducers();		// Issue producers for closest candidates
		long remainTime	= timeoutMS - (getTime() - startTime);
		while (consumersRemaining() && remainTime > 0 && pendingNodes.size() > 0) {
			try {
				log.info("Blocking in GetKClosestNodes for " + remainTime);
				wait(remainTime);
				log.info("Unblocking in GetKClosestNodes");
			} catch (InterruptedException e) {
				log.warn("Wait interrupted");
				break;
			}
			// Quick shortcut to stop issuing producers
			if (!consumersRemaining()) break;
			checkNodesToSend();	// Check if any nodes can be sent back immediately						 
			startProducers();	// See if we need to start additional producers
			remainTime = timeoutMS - (getTime() - startTime);				
		}
		// If there are still consumers, send results back
		if (consumersRemaining()) {
			sendTopKChecked();		// Send the top K nodes to the consumer
			issueComplete(true);	// Producer finished correctly
		}
		done = true;
		return true;
	}			
		
	public void run() {
		blockOnAsync();		
	}
	
	public synchronized boolean issueProducer() {
		new Thread(this).start();
		return true;
	}	
	
	public synchronized boolean issueProducerBlock() {
		return blockOnAsync();
	}
	
	public synchronized List<RemoteHyperNode> getConsumeResult() {
		TreeMap<Integer, List<RemoteHyperNode>> sortedMap = 
			new TreeMap<Integer, List<RemoteHyperNode>>();		
		Iterator<RemoteHyperNode> sentIt = sentNodes.iterator();
		while (sentIt.hasNext()) {
			RemoteHyperNode curNode = sentIt.next();
			int curNodeDist = 
				EditDistance.computeEditDistance(keyword, curNode.getKey());
			if (!sortedMap.containsKey(curNodeDist)) {
				sortedMap.put(curNodeDist, new ArrayList<RemoteHyperNode>());
			}
			sortedMap.get(curNodeDist).add(curNode);
		}
		
		List<RemoteHyperNode> closestList = new ArrayList<RemoteHyperNode>();
		Iterator<Integer> sortedIt = sortedMap.keySet().iterator();
		while (sortedIt.hasNext()) {
			closestList.addAll(sortedMap.get(sortedIt.next()));
		}
		return closestList;
	}

	public synchronized void producerComplete(
			Producer<Collection<RemoteHyperNode>> inProduce, boolean success) {
		if (done) return;	// Just exit right away
		log.info("In producerComplete");
		if (!(inProduce instanceof DistKNodesProducer)) {
			log.warn("Received unexpected producer");
			return;
		}
		DistKNodesProducer distKProducer = (DistKNodesProducer) inProduce;
		// NOTE: getNode is not synchronized, so it cannot cause a circular
		// dependency (and therefore not deadlock).
		RemoteHyperNode remoteNode = distKProducer.getNode();
		pendingNodes.remove(remoteNode);
		if (success) {
			log.info("Adding checked node");
			addCheckedNode(remoteNode);
		} else {
			log.info("Adding bad node");
			badNodes.add(remoteNode);
		}
		notify();	// Wake-up the master thread
	}

	public synchronized boolean newProducerResult(
			Producer<Collection<RemoteHyperNode>> inProduce,
			Collection<RemoteHyperNode> result) {
		// We don't actually touch the producer. It is mainly there to 
		// distinguish one from another in case a consumer has multiple 
		// producers. Calling methods in it runs the risk of introducing 
		// circular dependencies if newProducerResult is called in an 
		// unexpected manner.
		if (done) return false; 	// Just exit right away
		addCandidateNodes(result);	// Add these nodes to the candidate set
		notify(); 					// Wake-up the master thread
		return consumersRemaining();
	}
	
	public synchronized boolean checkConsumer(
			Producer<Collection<RemoteHyperNode>> inProduce) {
		if (done) return false; 	// Just exit right away
		return consumersRemaining();
	}
	
	public synchronized boolean consumersRemaining() {
		if (noConsumer) {
			return true;
		}
		return super.consumersRemaining();	
	}
}
