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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.AsyncCallback;
import org.apache.xmlrpc.client.TimingOutCallback;
import org.apache.xmlrpc.client.TimingOutCallback.TimeoutException;
import org.apache.xmlrpc.webserver.WebServer;

public class HyperNode {
	public class HyperNodeException extends Exception {
		private static final long serialVersionUID = -1057493401742538818L;
	}
	
	// Used a launch to copy all movies that are closer to this
	// node than its original node.
	protected class CopyEntriesThread extends Thread {
		// AsyncCallback that write new keys that are closer
		protected class CloserAsync implements AsyncCallback {
			private CopyEntriesThread 	parentThread;
			private RemoteHyperNode		targetNode;
			
			public CloserAsync(CopyEntriesThread parentThread,
					RemoteHyperNode	targetNode) {
				this.parentThread 	= parentThread;
				this.targetNode		= targetNode;
			}
			
			public void handleError(XmlRpcRequest pRequest, Throwable pError) {
				parentThread.addCloserKeys(targetNode, null);
			}			

			public void handleResult(XmlRpcRequest pRequest, Object pResult) {
				if (!(pResult instanceof Object[])) {
					log.info("Incorrect parameters");
					parentThread.addCloserKeys(targetNode, null);
					return;
				}				
				Object[] keysArray = (Object[]) pResult;
				Set<String> keysSet = new HashSet<String>();
				for (int i = 0; i < keysArray.length; i++) {
					if (!(keysArray[i] instanceof String)) {
						parentThread.addCloserKeys(targetNode, null);
						return;
					}
					keysSet.add((String)keysArray[i]);					
				}
				parentThread.addCloserKeys(targetNode, keysSet);
			}		
		}		
		
		// AsyncCallback that write back movie entries from keywords
		// fetched earlier by CloserAsync.
		protected class CloserAsyncEntries implements AsyncCallback {
			private CopyEntriesThread 	parentThread;
			private RemoteHyperNode		targetNode;
			
			public CloserAsyncEntries(CopyEntriesThread parentThread,
					RemoteHyperNode	targetNode) {
				this.parentThread 	= parentThread;
				this.targetNode		= targetNode;
			}
			
			public void handleError(XmlRpcRequest pRequest, Throwable pError) {
				parentThread.addCloserEntries(targetNode, null);
			}			
			
			private boolean addResults(Map<String, Set<MovieEntry>> entries , 
					Object curKey, Object curVal) {
				// Make sure it is a (String : Object[]) map;
				if (!(curKey instanceof String)) {
					log.info("Incorrect parameters (key)");
					parentThread.addCloserEntries(targetNode, null);
					return false;
				}
				if (!(curVal instanceof Object[])) {
					log.info("Incorrect parameters (key)");
					parentThread.addCloserEntries(targetNode, null);
					return false;							
				}
				// Fetch the Key -> Movies mapping. There really should
				// only be one key in the map, given the current 
				// implementation.
				String curStr 		= (String) curKey;
				Object[] objArray 	= (Object[]) curVal;
				
				// Try to cast each object to a MovieEntry					
				for (int i = 0; i < objArray.length; i++) {
					MovieEntry curMovie = 
						MovieEntry.xmlUnMarshal(objArray[i]);
					if (curMovie == null) {
						//log.info("Incorrect parameters (marshal)");								
						//parentThread.addCloserEntries(targetNode, null);
						//return false;
						continue;
					}
					log.info("Adding entry " + curStr + ":" + curMovie);						
					if (!entries.containsKey(curStr)) {
						entries.put(curStr, new HashSet<MovieEntry>());
					}
					entries.get(curStr).add(curMovie);
				}
				return true;
			}

			@SuppressWarnings("unchecked")
			public void handleResult(XmlRpcRequest pRequest, Object pResult) {
				//log.info("pResult is " + pResult);
				if (!(pResult instanceof Map)) {
					log.info("Incorrect parameters");
					parentThread.addCloserEntries(targetNode, null);
					return;
				}
				// Collect all the movie entries 
				Map<String, Set<MovieEntry>> entries = 
						new HashMap<String, Set<MovieEntry>>();
				
				// Iterate the returned Map
				Map keyMap = (Map) pResult;
				Iterator strIt = keyMap.keySet().iterator();
				while (strIt.hasNext()) {
					// Make sure it is a (String : Object[]) map
					Object curKey = strIt.next();
					Object curVal = keyMap.get(curKey);
					if (!addResults(entries, curKey, curVal)) {
						return;	// Just exit if addResults is false
					}						
				}
				parentThread.addCloserEntries(targetNode, entries);
			}
		}			
				
		private HyperNode 	node;				// Parent node
		private final int 	initVal 	 = 8;	// Number closest nodes to ask
		private int			asyncCounter = 0;	// Outstanding AsyncCallbacks
		
		// Keeps track of the number of keys that have been fetched
		// but its associated movie hasn't.
		private HashMap<RemoteHyperNode, Set<String>> pendingKeys;
		
		protected CopyEntriesThread(HyperNode node) {
			log.info("Starting CopyEntriesThread");
			pendingKeys = new HashMap<RemoteHyperNode, Set<String>>();
			this.node = node;
			start();
		}
		
		// Fetch one entry from the pending keys for the given
		// node and issue one getEntries fetch.
		private synchronized void issueOneCloserEntriesQuery(
				RemoteHyperNode curNode) {
			if (pendingKeys.containsKey(curNode)) {
				Set<String> keywords = pendingKeys.get(curNode);
				if (keywords.size() > 0) {
					asyncCounter++;
					Iterator<String> curIt = keywords.iterator();
					String curKey = curIt.next();
					curIt.remove();	
					log.info("Issueing getCloserEntries for: " + curKey);
					XmlRPCClient.getEntries(curNode, curKey, 
							new CloserAsyncEntries(this, curNode));
				}
			}
		}
		
		// Called by CloserAsync to add additional keywords for this
		// given node.
		public synchronized void addCloserKeys(
				RemoteHyperNode curNode, Set<String> keywords) {
			asyncCounter--;
			if (keywords != null) {
				log.info("Addingin to pendingKeys");
				pendingKeys.put(curNode, new HashSet<String>(keywords));
				issueOneCloserEntriesQuery(curNode);
			}
			notify();
		}
		
		// Called by CloserAsyncEntries to add additional movie entries
		// and to issue additional entry fetches if there are still more
		// pending.
		public synchronized void addCloserEntries(RemoteHyperNode curNode, 
				Map<String, Set<MovieEntry>> inEntries) {
			log.info("Received Closer Entries");
			asyncCounter--;
			if (inEntries != null) {
				// Iterate through the Map and add it to the node				
				Iterator<String> strIt = inEntries.keySet().iterator();
				while (strIt.hasNext()) {
					String curStr = strIt.next();
					Set<MovieEntry> curEntry = inEntries.get(curStr);
					Iterator<MovieEntry> entryIt = curEntry.iterator();
					while (entryIt.hasNext()) {
						log.info("Inserting movie for key: " + curStr);
						node.insertMovie(curStr, entryIt.next());
					}					
				}
			}
			issueOneCloserEntriesQuery(curNode);
			notify();
		}
		
		public synchronized void stopThread() {
			interrupt();
		}
		
		private synchronized void blockOnAsync() {
			// While there are still AsyncCallbacks outstanding
			while (asyncCounter > 0 && !interrupted()) {
				try {
					wait();
				} catch (InterruptedException e) {
					break;	// Just exit the loop
				}
			}
		}
		
		// Use as a thread safe way to increment the counter from
		// the run method
		private synchronized void incAsyncCounter() {
			asyncCounter++;
		}
		
		// Fetches the K closest nodes and then ask each whether they have
		// any movies that are closer than it than themselves.
		public void run() {
			Set<RemoteHyperNode> exceptNodes = new HashSet<RemoteHyperNode>();
			exceptNodes.add(node.getLocal());
			List<RemoteHyperNode> closestNodes =
				node.getKClosestNodesExcept(initVal, node.getKey(), exceptNodes);
			if (closestNodes == null) {
				log.info("CopyEntriesThread complete (no other nodes)");
				return; // Done, error getting closest nodes
			}
			Iterator<RemoteHyperNode> listIt = closestNodes.iterator();
			while (listIt.hasNext()) {				
				incAsyncCounter();
				log.info("Calling getCloserKey");
				RemoteHyperNode curNode = listIt.next(); 
				XmlRPCClient.getCloserKeys(
					curNode, node.getKey(), new CloserAsync(this, curNode));				
			}
			blockOnAsync();	// Blocks until the process is complete.
			log.info("CopyEntriesThread complete");
		}
	}
	
	// Gossip thread that periodically contacts ring members to request
	// additional nodes.
	protected class GossipThread extends Thread {
		private int 		timerMS;	// Period of gossip
		private HyperNode 	node;		// Parent node
		
		protected GossipThread(HyperNode hNode, int gossipTimerMS) {
			node 	= hNode;
			timerMS = gossipTimerMS;
			start();
		}			
		
		// Check that the node still exists with the same key
		private RemoteHyperNode checkNode(RemoteHyperNode curNode) {
			// If node, for whatever reason, refers to the same
			// IP/port as the local node, remove it and skip.
			// (This should not be possible, but check just in case)
			if (curNode.getIP().equals(node.getLocal().getIP()) &&
					curNode.getPort() == node.getLocal().getPort()) {
				node.localRemoveNode(curNode);
				return null;
			}
			// Try to contact the node with a given timeout
			TimingOutCallback callback = 
				XmlRPCClient.getNode(curNode.toAddressPort());			
			log.info("Calling getNode: " + curNode.getIP() 
					+ ":" + curNode.getPort());
			Object res = null;
			try {
				res = callback.waitForResponse();
			} catch (TimeoutException e) {
			} catch (Throwable e) 		 {}
			if (res == null) {
				// Timed out, remove the node from the ring
				node.localRemoveNode(curNode);
				return null;
			}
			// See if the node has changed its key
			RemoteHyperNode resNode = RemoteHyperNode.convert(res);
			if (resNode == null) {
				node.localRemoveNode(curNode);
				return null;					
			}
			log.info("getNode call succeeded: " + curNode.getIP() 
					+ ":" + curNode.getPort());
			
			// Remove the old node if it has changed									
			if (!curNode.equals(resNode)) {
				log.info("Removing stale node entry");
				node.localRemoveNode(curNode);
			}
			// Make sure the new node is not this local node
			// (This should not be possible, but check just in case)
			if (resNode.equals(node.getLocal())) {
				log.error("Removing self refernece (unexpected)");
				return null;
			}
			node.localAddNode(resNode); // Add the new node	
			return resNode;
		}
		
		private void performGossip() {
			log.info("Performing Gossiping");
			// Get random ring members
			Collection<RemoteHyperNode> randNodes = node.getLocalRandomNodes();
			
			// Create a set that are node set to be sent during gossip
			Set<RemoteHyperNode> sendSet = new HashSet<RemoteHyperNode>();
			
			// For each node verify that he is still who he is, i.e.
			// it is still using the same representative key.
			Iterator<RemoteHyperNode> nodeIt = randNodes.iterator();
			while (nodeIt.hasNext()) {		
				RemoteHyperNode cNode = checkNode(nodeIt.next());
				if (cNode != null) {
					sendSet.add(cNode);	// Node responded, add to the send set.
				}
			}
			
			// Reinitialize the random node set with the known good nodes
			randNodes = new HashSet<RemoteHyperNode>(sendSet);
			
			// Add the local node the send set, as we always want these
			// nodes that we are running.
			sendSet.add(node.getLocal());
			
			// Iterate through the random nodes and perform gossip with 
			// each of these nodes.
			nodeIt = randNodes.iterator();
			while (nodeIt.hasNext()) {
				RemoteHyperNode curNode = nodeIt.next();
				
				// Remove the current node from the send set since 
				// we don't want to send itself to the remote node.
				sendSet.remove(curNode);
				
				log.info("Calling insertNodesBlock: " 
						+ curNode.getIP() + ":" + curNode.getPort());
				
				// Push some members to him
				if (!XmlRPCClient.insertNodesBlock(curNode, sendSet)) {
					// Failed on the push, let's keep the node for now
					log.warn("Failed on node push to: " 
							+ curNode.getIP() + ":" + curNode.getPort());
				}
				
				log.info("insertNodesBlock succeeded: " 
						+ curNode.getIP() + ":" + curNode.getPort());
				
				// Add current node back to the send set.
				sendSet.add(curNode);
				
				log.info("Calling getRandomNodesBlock: " 
						+ curNode.getIP() + ":" + curNode.getPort());
				
				// Pull some nodes back	from the remote node
				List<RemoteHyperNode> nodeList = 
					XmlRPCClient.getRandomNodesBlock(curNode);				
				
				if (nodeList == null) {
					log.warn("Failed on node pull: " 
							+ curNode.getIP() + ":" + curNode.getPort());
				} else {
					log.info("getRandomNodesBlock succeeded: " + curNode.getIP() 
							+ ":" + curNode.getPort());
					// Check that these nodes are valid and insert them into
					// the ring set.
					Iterator<RemoteHyperNode> innerNodeIt = nodeList.iterator();
					while (innerNodeIt.hasNext()) {
						checkNode(innerNodeIt.next());
					}
				}
			}			
		}
		
		private synchronized void blockOnTimeout() {
			try {
				wait(timerMS);
			} catch (InterruptedException e) {
				log.warn("Interrupted on thread block");
			}
		}
		
		public synchronized void stopThread() {
			interrupt();
		}
		
		public void run() {
			while (!interrupted()) {
				performGossip();
				blockOnTimeout();
			}
		}
	}
	
	// Periodically perform ring management on a given ring
	protected class RingManageThread extends Thread {
		private int 		timerMS;
		private int			nRings;
		private HyperNode 	node;
		private	int			ringCounter = 0; 
		protected RingManageThread(HyperNode hNode, int ringManageMS) {
			node 	= hNode;
			timerMS = ringManageMS;
			nRings	= hNode.getNumRings(); 
			start();
		}			
		private synchronized void performRingManagement() {			
			log.info("Performing Ring Management");			
			node.performRingManagement(ringCounter);
			ringCounter = (ringCounter + 1) % nRings;	
		}
		
		private synchronized void blockOnTimeout() {
			try {
				wait(timerMS);
			} catch (InterruptedException e) {
				log.warn("Interrupted on thread block");
			}
		}
		
		public synchronized void stopThread() {
			interrupt();
		}
		
		public void run() {
			while (!interrupted()) {
				performRingManagement();
				blockOnTimeout();
			}
		}
	}		
	
	// Used to find a pseudo-unique keyword as well as a set of
	// remote nodes that are close in the Hyperspace to the keyword.
	protected static class HyperBootStrap {
		private List<AddressPort> 			seedAddrs;
		private int 						minSeeds;
		private int							maxThreads;
		private HashSet<ProducerThread> 	threadsSet;
		// TODO: May want to do something more clever to make 
		// keyword inserts quicker. Right now, it is a linear
		// search through a list.
		private ArrayList<String>			keywords;
		private String						selectedKeyword = null;
		private ArrayList<RemoteHyperNode>	seedNodes;
		// Container for all nodes that we are in the progress
		// of checking for liveness.
		private HashSet<AddressPort>		checkingNodes;
		// These nodes are waiting on available threads 
		private HashSet<AddressPort>		pendingNodes;
		// These are known bad nodes
		private HashSet<AddressPort>		badNodes;
		private BootStrapGCNThread			closestNodeThread = null;
		
		protected HyperBootStrap(List<AddressPort> seedAddrs, 
				Set<String> defaultKeys, int minSeeds, int maxThreads) {
			this.seedAddrs 	= seedAddrs;
			this.minSeeds 	= minSeeds;
			this.maxThreads	= maxThreads;
			threadsSet 		= new HashSet<ProducerThread>();
			keywords 		= new ArrayList<String>();
			seedNodes 		= new ArrayList<RemoteHyperNode>();
			checkingNodes 	= new HashSet<AddressPort>();
			pendingNodes	= new HashSet<AddressPort>();
			badNodes		= new HashSet<AddressPort>();

			log.info("Seed addr size is " + this.seedAddrs.size());
			Random curRand = new Random();
			TreeMap<Double, String> randMap = new TreeMap<Double, String>();			
			// Just manually add these default keywords
			Iterator<String> keys = defaultKeys.iterator();
			while (keys.hasNext()) {
				Double randDB = curRand.nextDouble();
				// Skip if in the rare case that we get a repeated value
				if (!randMap.containsKey(randDB)) {
					randMap.put(randDB, keys.next().toLowerCase());
				}
			}
			keywords.addAll(randMap.values());
		}
		
		private synchronized void createOneProducer(AddressPort curSeedAddr) {
			// Skip these nodes if we are already checking them 
			// or they are known to be bad nodes.
			if (checkingNodes.contains(curSeedAddr)	||
				pendingNodes.contains(curSeedAddr)	||
				badNodes.contains(curSeedAddr)) {
				return;
			}
			// Limit the number of threads to maxThreads 
			if (threadsSet.size() >= maxThreads) {
				pendingNodes.add(curSeedAddr);
			} else {
				log.info("Creating producer thread for: " + 
						curSeedAddr.getAddr() + ":" + curSeedAddr.getPort());
				checkingNodes.add(curSeedAddr);
				threadsSet.add(new ProducerThread(curSeedAddr, this));
			}						
		}
		
		// Create a producer for each seed node
		private synchronized void createProducers() {	
			for (int i = 0; i < seedAddrs.size(); i++) {
				AddressPort curSeedAddr = seedAddrs.get(i);
				createOneProducer(curSeedAddr);
			}
		}
		
		// NOTE: Assumes seedNodes.size() > 0
		private synchronized void issueCNSearch(String curKeyword) {
			log.info("In issueCNSearch");
			Random randVal = new Random();
			HashSet<RemoteHyperNode> nSet = new HashSet<RemoteHyperNode>();
			int numSeedsToUse = Math.min(minSeeds, seedNodes.size());
			for (int i = 0; i < numSeedsToUse; i++) {
				int curIndex = randVal.nextInt(seedNodes.size());						
				while (nSet.contains(seedNodes.get(curIndex))) {
					curIndex = (curIndex + 1) % seedNodes.size();
				}
				nSet.add(seedNodes.get(curIndex));				
			}
			closestNodeThread = new BootStrapGCNThread(curKeyword, nSet, this);
		}
		
		public synchronized void init() {
			// Create the producer threads
			createProducers();
			
			// Keep looping if the number of seeds are bigger than 0 or there 
			// are pending new seeds, and there are still keywords to check or 
			// we are checking one right now, and we haven't selected a word yet.
			int kwIndex = 0;
			while ((seedNodes.size() > 0 || threadsSet.size() > 0)	 &&
					(kwIndex < keywords.size() || closestNodeThread != null) &&					 
					selectedKeyword == null) {
				// See if the criteria are met to issue a search
				if (closestNodeThread == null) {
					// Issue a search if the number of seed nodes are bigger
					// than the minSeed nodes, or there are no more seed nodes
					// pending (threadsSet == 0)
					if (seedNodes.size() >= minSeeds || threadsSet.size() == 0) {
						assert(kwIndex < keywords.size());
						issueCNSearch(keywords.get(kwIndex++));						
					}
				}
				log.info("Blocking inside init");
				// Block until next event
				try { wait();} catch (InterruptedException e) {}
				log.info("Done blocking");
			}
			
			// Just select a random word if there are no seed nodes. keywords.size() 
			// needs to be bigger than 0, which should always be the case from
			// the default keywords we installed.
			if (selectedKeyword == null && 
				seedNodes.size() == 0 	&& 
				keywords.size() > 0) {
				selectedKeyword = keywords.get(
					(new Random()).nextInt(keywords.size()));
			}
		}
		
		public synchronized String getKeyWord() {
			return selectedKeyword;
		}
		
		public synchronized List<RemoteHyperNode> getSeedNodes() {
			return new ArrayList<RemoteHyperNode>(seedNodes);
		}
						
		public synchronized void addKeyWords(List<String> newKeywords) {
			Iterator<String> curIt = newKeywords.iterator();
			while (curIt.hasNext()) {
				String curKeyword = curIt.next();
				if (!keywords.contains(curKeyword)) {
					keywords.add(curKeyword);
				}
			}
			notify();
		}
		
		public synchronized void addClosestResult(BootStrapGCNThread curThread) {				
			assert(curThread == closestNodeThread);
			String keyword = curThread.getSearchWord();
			RemoteHyperNode closestNode = curThread.getNode();
			
			/* For testing */
			if (closestNode != null) {				
				log.info("Search word is " + keyword);
				log.info("Closest node is " + closestNode.getKey());
			}				
			
			if (closestNode != null && !keyword.equals(closestNode.getKey())) {				
				selectedKeyword = keyword;
				log.info("Selected key is " + selectedKeyword);
			}
			closestNodeThread = null;	// Always set it back to NULL
			notify();
		}

		public synchronized void addNewNodes(List<RemoteHyperNode> nodes) {
			Iterator<RemoteHyperNode> curIt = nodes.iterator();
			while (curIt.hasNext()) {
				RemoteHyperNode curNode	= curIt.next();
				AddressPort	curNodeAddr	= curNode.toAddressPort();
				if (seedNodes.contains(curNode)) {
					continue;
				}
				// Keep creating producers if we don't have a selected word.
				if (selectedKeyword == null) { 
					createOneProducer(curNodeAddr);
				}
			}
			notify();
		}
		
		public synchronized void addSeedNode(RemoteHyperNode curNode) {
			AddressPort curNodeAddr = curNode.toAddressPort();
			assert(!badNodes.contains(curNodeAddr));
			if (checkingNodes.contains(curNodeAddr)) {
				checkingNodes.remove(curNodeAddr);				
			}
			if (!seedNodes.contains(curNode)) {
				seedNodes.add(curNode);
			}
			notify();
		}
		
		public synchronized void finProducer(ProducerThread producerThread) {
			threadsSet.remove(producerThread);			
			// Remove it from the checkingNodes set 
			AddressPort produceAddrPort = producerThread.getAddressPort();
			if (checkingNodes.contains(produceAddrPort)) {
				// Node never responded to a probe
				checkingNodes.remove(produceAddrPort);
				badNodes.add(produceAddrPort);
			}			
			if (pendingNodes.size() > 0 && selectedKeyword == null) {
				AddressPort pendingAddrPort = pendingNodes.iterator().next();
				pendingNodes.remove(pendingAddrPort);
				threadsSet.add(new ProducerThread(pendingAddrPort, this));
			}
			notify();
		}
	}	
	
	// Simple thread used at bootstrap to find the closest 
	// node to the given search word given a set of seed nodes.
	protected static class BootStrapGCNThread extends Thread {
		private ArrayList<RemoteHyperNode> 	seedNodes;
		private HyperBootStrap 				consumer;
		private String 						searchWord;
		private RemoteHyperNode				closestNode = null;
		
		protected BootStrapGCNThread(String searchWord, 
				Set<RemoteHyperNode> remoteNodes, 
				HyperBootStrap consumer) {
			log.info("In BootStrapGCNThread constructor");
			this.searchWord = searchWord;
			this.consumer = consumer;
			seedNodes = new ArrayList<RemoteHyperNode>();
			seedNodes.addAll(remoteNodes);
			start();
		}
		
		public void run() {
			log.info("Calling getKClosestNodes");
			List<RemoteHyperNode> closestList = 
				XmlRPCClient.getKClosestNodesBlock(seedNodes, 1, searchWord);
			if (closestList.size() > 0) {				
				closestNode = closestList.get(0);
				log.info("Closest node is: " 
					+ closestNode.getIP() + ":" + closestNode.getPort());
				log.info("With key: " + closestNode.getKey());
			}
			consumer.addClosestResult(this);
		}

		protected synchronized String getSearchWord() { 
			return searchWord;  
		}
		
		protected synchronized RemoteHyperNode getNode() { 
			return closestNode;
		}
	}	
	
	// For a given raw-node (just ip/port), it fetches the RemoteHyperNode
	// representation, fetches a few random nodes as well as a bunch
	// of random keywords.
	protected static class ProducerThread extends Thread{
		protected class AddKeyWords implements AsyncCallback {
			private ProducerThread worker;
			
			protected AddKeyWords(ProducerThread worker) {
				this.worker = worker;				
			}
			
			public void handleError(XmlRpcRequest pRequest, Throwable pError) {
				worker.addKeyWords(this, null);
			}

			public void handleResult(XmlRpcRequest pRequest, Object pResult) {
				ArrayList<String> keywords = new ArrayList<String>();
				if (pResult instanceof Object[]) {
					Object[] objArray = (Object[]) pResult;
					for (int i = 0; i < objArray.length; i++) {
						if (objArray[i] instanceof String) {
							keywords.add((String)objArray[i]);							
						}				
					}					
				}
				worker.addKeyWords(this, keywords);
			}
		}
		
		protected class AddNewNodes implements AsyncCallback {
			private ProducerThread worker;
			
			protected AddNewNodes(ProducerThread worker) {
				this.worker = worker;				
			}
			
			public void handleError(XmlRpcRequest pRequest, Throwable pError) {
				worker.addNewNodes(this, null);
			}

			public void handleResult(XmlRpcRequest pRequest, Object pResult) {
				ArrayList<RemoteHyperNode> nodes = new ArrayList<RemoteHyperNode>();				
				if (pResult instanceof Object[]) {
					Object[] objArray = (Object[]) pResult;
					// Convert each of these objects into a RemoteHyperNode.								
					for (int j = 0; j < objArray.length; j++) {
						RemoteHyperNode curNode = RemoteHyperNode.convert(objArray[j]);
						if (curNode != null) {
							nodes.add(curNode);
						}
					}
				}
				worker.addNewNodes(this, nodes);
			}
		}
		
		private AddressPort 			seedAddrPort;
		private HyperBootStrap 			consume;
		private HashSet<AsyncCallback> 	asyncHandlers;
		private long					waitTimeout = 5000; 
		
		protected ProducerThread(
				AddressPort seedAddrPort, HyperBootStrap consume) {
			log.info("Starting producer");
			this.seedAddrPort = seedAddrPort;
			this.consume = consume;
			asyncHandlers = new HashSet<AsyncCallback>();
			start();
		}
		
		public synchronized AddressPort getAddressPort() {
			return seedAddrPort;
		}
		
		public synchronized void addAsyncHandler(AsyncCallback callback) {
			asyncHandlers.add(callback);
		}
		
		public synchronized void addKeyWords(
				AddKeyWords addKeyWords, List<String> keywords) {
			if (keywords != null && consume != null) {
				consume.addKeyWords(keywords);
			}
			if (asyncHandlers.contains(addKeyWords)) {
				asyncHandlers.remove(addKeyWords);
			}
			notify();
		}

		public synchronized void addNewNodes(
				AddNewNodes addNewNodes, List<RemoteHyperNode> nodes) {
			if (nodes != null && consume != null) {
				consume.addNewNodes(nodes);
			}
			if (asyncHandlers.contains(addNewNodes)) {
				asyncHandlers.remove(addNewNodes);
			}
			notify();
		}
		
		public synchronized void waitOnAsync(long timeout) {
			Date date = new Date();
			long startTime  = date.getTime();
			long timeRemain = timeout; 
			while (asyncHandlers.size() > 0) {
				try {
					wait(timeRemain);
				} catch (InterruptedException e) {
					// Do nothing, let it loop again
				}				
				// Calculate remaining time to wait
				long curTime = date.getTime();
				long timeWaited = curTime - startTime;
				if (timeWaited > timeout) {
					break;	// Done, stop waiting on these thread
				}
				timeRemain = timeout - timeWaited;
			}
			consume.finProducer(this);
			consume = null;
		}

		private RemoteHyperNode getSeedNode() {
			log.info("getSeedNode");
			TimingOutCallback callback = XmlRPCClient.getNode(seedAddrPort);
			if (callback == null) { 
				return null;	// xmlRPC failure
			}
			// Wait for the response
			Object retObj = null;
			try {
				retObj = callback.waitForResponse();
			} catch (TimeoutException e) {				
				return null;	// Timed out
			} catch (Throwable e) {
				return null;	// Server returned an error
			}			
			// Make sure it is indeed a node
			if (!(retObj instanceof Map)) {				
				return null;	// Returned object is unexpected
			}					
			RemoteHyperNode seedNode = RemoteHyperNode.convert(retObj);
			if (seedNode == null) {
				log.warn("Cannot create seed node from: " 
						+ seedAddrPort.getAddr() + ":" + seedAddrPort.getPort());
			} else {
				log.info("Retrieve node info: " 
						+ seedNode.getIP() + ":" + seedNode.getPort());
			}
			return seedNode;
		}

		public void run() {
			// First retrieve the seedNode info
			RemoteHyperNode seedNode = getSeedNode();
			if (seedNode == null) {
				consume.finProducer(this);
				consume = null;	// Null it just in case
				return; // Done with this thread;
			}
			consume.addSeedNode(seedNode);
			
			// Now get some additional keywords
			AddKeyWords addKWAsync = new AddKeyWords(this);
			addAsyncHandler(addKWAsync);	
			XmlRPCClient.getRandomWordsAsync(seedNode, addKWAsync);
			
			// And some additional nodes
			AddNewNodes addNodesAsync = new AddNewNodes(this);
			addAsyncHandler(addNodesAsync);			
			XmlRPCClient.getRandomNodesAsync(seedNode, addNodesAsync);
			
			waitOnAsync(waitTimeout); // Block until the handlers are finished
		}		
	}
	
	private int 				nodesPerRing;
	private double 				ringLogBase;
	private int 				maxDist;
	private int 				gossipMS;
	private int					ringManageMS;
	private String 				nodeID;
	private String				localIP;
	private int 				portNum;
	private List<URL>			checkNATServer;
	private EntryContainer		entryContainer;
	private HyperRings			hyperRings 			= null;
	private RemoteHyperNode		localNode 			= null;
	private WebServer 			rpcServer 			= null;
	private boolean				nodeStarted			= false;
		
	private final static int 	minSeeds 			= 4;
	private final static int 	maxProducerThreads 	= 32;
	private final static int 	maxRetries			= 4;
	private final static int	numWordsReturned	= 32;	
	private CopyEntriesThread 	copyEntriesThread 	= null;
	private GossipThread		gossipThread		= null;
	private RingManageThread	ringManageThread 	= null;
	
	private final static int 	defaultNodesPerRing	= 8;
	private final static double	defaultRingLogBase	= 1.2;
	private final static int 	defaultMaxDist		= 16;
	private final static int 	defaultGossipMS		= 15000;
	private final static int 	defaultRingMangeMS	= 60000;	
	
	private static Logger 		log = Logger.getLogger(HyperNode.class.getName());
				
		
	// NOTE: The common key words are used to bootstrap the system.
	public HyperNode(int nodesPerRing, double ringLogBase, 
				int maxDist, int gossipMS, int ringManageMS, 
				String nodeID, List<URL> checkNATServer, 
				EntryContainer entryContainer, String keyword, 
				Set<RemoteHyperNode> seedNodes, int portNum) 
			throws HyperNodeException {
		this.nodesPerRing 		= nodesPerRing;
		this.ringLogBase		= ringLogBase;
		this.maxDist			= maxDist;
		this.gossipMS			= gossipMS;
		this.ringManageMS		= ringManageMS;
		this.nodeID				= nodeID;	
		this.portNum			= portNum;
		this.checkNATServer		= checkNATServer;		
		this.entryContainer 	= entryContainer;
		
		// Log the actual selected keyword
		log.info("Selected keyword is " + keyword);		
				
		// Generate the Multi-resolution ring and generate a RemoteHyperNode
		// representation of this local node.
		hyperRings = new HyperRings(
				this.nodesPerRing, this.ringLogBase, this.maxDist, keyword);

		// Add all the known nodes into the ring set
		Iterator<RemoteHyperNode> seedIt = seedNodes.iterator();
		while (seedIt.hasNext()) {
			localAddNode(seedIt.next());
		}
		
		// Find the local closest node to this node from the seed nodes and then
		// fetch and add all of its neighbors.The real closest node should already 
		// be in the hyper-ring.
		initFillRings();
		
		// Start XmlRPCServer
		try {
			rpcServer = XmlRPCServer.startXmlRPCServer(this, this.portNum);
		} catch (BindException e) {
			log.error("Cannot bind to the local port");
			throw new HyperNodeException();
		} catch (IOException e) {
			log.error("IOException error creating RPC server: " + e);
			throw new HyperNodeException();
		} catch (XmlRpcException e) {
			log.error("XmlRPCException error creating RPC server: " + e);
			throw new HyperNodeException();		
		}

		// Check if this node is behind a NAT
		Iterator<URL> urlIt = this.checkNATServer.iterator();
		while (urlIt.hasNext()) {
			URL curURL = urlIt.next();
			localIP = XmlRPCClient.getNonNatAddr(curURL, this.portNum);
			if (localIP != null) {
				break;	// Done, non NAT'ed node
			}
		}
		
		// If all the NAT checking servers agree that this node is behind
		// a NAT that UPnP can't punch through, then just exit.
		if (localIP == null) {
			if (rpcServer != null) {
				log.info("Shutting down the rpc server");
				rpcServer.shutdown();				
				rpcServer = null;
			}
			throw new HyperNodeException();
		}
		
		log.info("Local IP:Port is " + localIP + ":" + this.portNum);
				
		// Create a RemoteHyperNode representation of this node
		localNode = new RemoteHyperNode(
				localIP, this.portNum, this.nodeID, hyperRings.getKey());
		
		setStarted();
		
		// Start CopyEntries thread to copy all movies that should be
		// migrated/copied to this node.
		copyEntriesThread = new CopyEntriesThread(this);
		
		// Start Gossip thread
		gossipThread = new GossipThread(this, this.gossipMS);
		
		// Start a ring-management thread
		ringManageThread = new RingManageThread(this, this.ringManageMS);
	}	
	
	public static KeyVal<String, Set<RemoteHyperNode>> findKeyAndSeeds(
			List<AddressPort> seedAddrs, Set<String> defaultKeywords) {
		// Start producer/consumer threads that tries to find
		// an unique keyword for use by this node.
		Set<RemoteHyperNode> seedNodes = new HashSet<RemoteHyperNode>();
		
		// Try to find a unique keyword for this node.
		String keyword = null;
		log.info("Size of seedAddrs is " + seedAddrs.size());
		for (int i = 0; i < maxRetries; i++) {
			log.info("Unique keyword search iteration " + i);
			
			// Create a consumer object that spawns multiple
			// producer threads. Each producer tries to contact 
			// a seed node, fetch its info, get additional
			// nodes and keywords. Once minSeeds are retrieved,
			// keys are checked from these seeds one-by-one to
			// see if it is unique.
			HyperBootStrap startupBS = new HyperBootStrap(seedAddrs, 
					defaultKeywords, minSeeds, maxProducerThreads);
			log.info("Starting HyperBootStrap");
			startupBS.init();
			keyword = startupBS.getKeyWord();
			seedNodes.addAll(startupBS.getSeedNodes());			
			if (keyword != null) {
				log.info("Unique key found: " + keyword);
				break;	// Done, found an unique keyword
			}
		}		
		// No unique word is available, just pick a random word instead
		if (keyword == null) {
			log.info("Unique key not available, selecting random key");
			if (defaultKeywords.size() == 0) {
				return null;
			}
			log.warn("Cannot find an unique word, using random word.");
			// Get the number of times to iterate through the set.
			int rCount = (new Random()).nextInt(defaultKeywords.size()) + 1;
			Iterator<String> strIt = defaultKeywords.iterator();
			while (strIt.hasNext() && rCount-- > 0) {
				keyword = strIt.next();	
			}
			assert(keyword != null);
		}
		return new KeyVal<String, Set<RemoteHyperNode>>(keyword, seedNodes);
	}
	
	private synchronized void setStarted() {
		nodeStarted = true;
	}
	
	public synchronized boolean nodeStarted() {
		return nodeStarted;
	}
	
	public void performRingManagement(int ringNum) {
		hyperRings.performRingManagement(ringNum);		
	}

	public int getNumRings() {
		return hyperRings.getNumRings();
	}

	// Stop all outstanding threads and shutdown the RPC server
	public void stopNode() {
		if (gossipThread != null) { 
			log.info("Shutting down gossip thread");
			gossipThread.stopThread();
			gossipThread = null;
		}
		if (ringManageThread != null) {
			log.info("Shutting down ring management thread");
			ringManageThread.stopThread();
			ringManageThread = null;
		}
		if (copyEntriesThread != null) {
			log.info("Shutting down copy entries thread");
			copyEntriesThread.stopThread();
			copyEntriesThread = null;
		}
		if (rpcServer != null) {
			log.info("Shutting down the rpc server");
			rpcServer.shutdown();
			rpcServer = null;
		}
	}
	
	public Set<MovieEntry> getLocalKClosestEntries(
			int kVal, List<String> keywords) {		
		return entryContainer.getKClosestKeys(kVal, keywords);
	}
	
	public EntryContainer getEntryContainer() {
		return entryContainer;
	}
	
	// Used only during bootstrap. Fills rings by asking the closest
	// known node for all of its ring members.
	private void initFillRings() {
		RemoteHyperNode closestNode = null;
		Set<RemoteHyperNode> exceptNodeSet = new HashSet<RemoteHyperNode>();
		while (closestNode == null) {
			// First try to find the local closest node
			closestNode = hyperRings.getClosestNodeExcept(
					hyperRings.getKey(), exceptNodeSet);
			if (closestNode == null) {
				break; // Done, no more nodes to try
			}
			log.info("Attempting to fetch ring members of " 
					+ closestNode.getIP() + ":" + closestNode.getPort());
			List<RemoteHyperNode> nodeList = 
				XmlRPCClient.getRingMembersBlock(closestNode);
			if (nodeList == null) {
				// Node did not respond, add to except list, and try the 
				// next closest node
				exceptNodeSet.add(closestNode);
				closestNode = null;	
			} else{
				// Add all its peers into its own ring set
				Iterator<RemoteHyperNode> listIt = nodeList.iterator();
				while (listIt.hasNext()) {					
					localAddNode(listIt.next());
				}
			}
		}
	}

	public Set<RemoteHyperNode> getLocalDistBoundNodes(
			String keyword, int distBound) {
		return hyperRings.getDistNodes(keyword, distBound);
	}
	
	public List<RemoteHyperNode> getLocalKClosestNodes(
			String keyword, int inKVal) {
		return hyperRings.getKClosestNodes(inKVal, keyword);
	}
	
	public static void insertEntry(
			EntryContainer entryContainer, Set<RemoteHyperNode> seedNodes, 
			int numReplicas, MovieEntry movieEntry) {
		// Get the individual words from this movie entry
		String[] wordsArray = 
			entryContainer.removeSymbols(movieEntry.getMovName());
		
		Map<String, List<RemoteHyperNode>> wordToClosestNodes = 
			new HashMap<String, List<RemoteHyperNode>>();
		
		for (int i = 0; i < wordsArray.length; i++) {
			String curWord = wordsArray[i].toLowerCase();
			System.out.println("Inserting word: " + curWord);
			if (!wordToClosestNodes.containsKey(curWord)) {
				// This is a blocking call. Maybe make this 
				// a non-blocking call in the future? Performance
				// for insertEntry is not that important at the
				// moment though, as it is performed by our crawler.
				//log.info("Calling getKClosestNodesExcept");
				System.out.println("Calling getKClosestNodesExcept");
				List<RemoteHyperNode> curList = 
					XmlRPCClient.getKClosestNodesBlock(
						seedNodes, numReplicas, curWord);
				if (curList != null) {
					wordToClosestNodes.put(curWord, curList);
				}
			}
		}
		
		Iterator<String> mapIt = wordToClosestNodes.keySet().iterator();
		while (mapIt.hasNext()) {
			String curKey = mapIt.next();
			List<RemoteHyperNode> curList = wordToClosestNodes.get(curKey);
			Iterator<RemoteHyperNode> listIt = curList.iterator();
			while (listIt.hasNext()) {
				RemoteHyperNode curNode = listIt.next();
				System.out.println("Inserting key: " + curKey);
				XmlRPCClient.insertKeyEntry(curNode, curKey, movieEntry);
			}
		}		
	}
		
	public void insertMovie(String key, MovieEntry movieEntry) {
		log.info("insertMovie: " + key + ":" 
				+ movieEntry.getMovName() + ":" + movieEntry.getMagLink());
		entryContainer.addKeyEntry(key, movieEntry);		
	}
	
	// INVARIANT: AvailSet is a superset of CheckedSet, and 
	// AvailSet intersect BadSet => null set
	public List<RemoteHyperNode> getKClosestNodesExcept(
			int kVal, String keyword, Set<RemoteHyperNode> exceptNodes) {		
		// Generate the badSet
		Set<RemoteHyperNode> badSet = new HashSet<RemoteHyperNode>(exceptNodes);
		
		// Generate the checked nodes set (just add the local node if it is
		// not already in the badSet)
		Set<RemoteHyperNode> checkedSet = new HashSet<RemoteHyperNode>();
		if (!badSet.contains(localNode)) {
			checkedSet.add(localNode);
		}

		// Generate the available nodes set from the hyper-ring
		int numToRetrieve = Math.max(XmlRPCClient.minGetK, kVal * 2);
		Set<RemoteHyperNode> availSet = new HashSet<RemoteHyperNode>(
				hyperRings.getKClosestNodes(numToRetrieve, keyword));
		log.info("Number of nodes returned by hyperRings.getKClosestNodes: " +
				availSet.size());
		
		// Remove any nodes from availSet that are also in the bad set 
		Iterator<RemoteHyperNode> availIt = availSet.iterator();
		while (availIt.hasNext()) {
			RemoteHyperNode curHyperNode = availIt.next();
			if (badSet.contains(curHyperNode)) {
				availIt.remove();
			}
		}
		
		// Add the localNode to the availSet 
		if (!badSet.contains(localNode)) {
			availSet.add(localNode);
		}
		
		// Call the XmlRPCClient helper method with these parameters.
		return XmlRPCClient.getKClosestNodesHelper(
				kVal, keyword, availSet, checkedSet, badSet);
	}	
		
	public RemoteHyperNode getLocal() {
		log.info("getLocal");
		return localNode;
	}
	
	public String getKey() {
		log.info("getKey");
		return hyperRings.getKey();
	}
	
	public List<RemoteHyperNode> getLocalRandomNodes() {
		log.info("getLocalRandomNodes");
		return hyperRings.getRandomNodes();
	}
	
	private void localAddNode(RemoteHyperNode curNode) {
		//if (localNode == null || !curNode.equals(localNode)) { 
		if (localNode == null 							|| 
			!curNode.getIP().equals(localNode.getIP()) 	||
			curNode.getPort() != localNode.getPort()) {
			log.info("Adding node: " 
				+ curNode.getIP() + ":" + curNode.getPort());
			hyperRings.addMember(curNode);
		}
	}
	
	private void localRemoveNode(RemoteHyperNode curNode) {
		log.info("localRemoveNode: " + curNode.getIP() + ":" + curNode.getPort());
		hyperRings.removeMember(curNode);		
	}
	
	/* RPC methods start here
	 * -----------------------------------------------------
	 */
	@SuppressWarnings("unchecked")
	public Map getNode() {
		return getLocal().toMap();
	}
	
	public Object[] getDistBoundNodes(String keyword, int distBound) {
		Set<RemoteHyperNode> curSet = 
			getLocalDistBoundNodes(keyword, distBound);
		Object[] retArray = new Object[curSet.size()];
		int counter = 0;
		Iterator<RemoteHyperNode> nodeIt = curSet.iterator();
		while (nodeIt.hasNext()) {
			retArray[counter++] = nodeIt.next().toMap();
		}
		return retArray;
	}
	
	public Object[] getDistKNodes(String keyword, int distBound, int kVal) {
		log.info("Calling getLocalDistBoundNodes: " 
				+ keyword + ", " + distBound);
		Set<RemoteHyperNode> curSet = 
			getLocalDistBoundNodes(keyword, distBound);
		log.info("Calling getLocalKClosestNodes: " + keyword + ", " + kVal);
		curSet.addAll(getLocalKClosestNodes(keyword, kVal));
		log.info("Num total results: " + curSet.size());
		Object[] retArray = new Object[curSet.size()];
		int counter = 0;
		Iterator<RemoteHyperNode> nodeIt = curSet.iterator();
		while (nodeIt.hasNext()) {
			retArray[counter++] = nodeIt.next().toMap();
		}
		return retArray;
	}	
	
	public Boolean addNodes(Object[] inNodes) {
		log.info("In addNodes");
		ArrayList<RemoteHyperNode> nodeList = new ArrayList<RemoteHyperNode>();
		for (int i = 0; i < inNodes.length; i++) {	
			RemoteHyperNode remoteNode = RemoteHyperNode.convert(inNodes[i]);
			if (remoteNode != null) {
				nodeList.add(remoteNode);
			} else {				
				log.warn("Parse error, ending addNodes");
				return false;
			}
		}
		for (int i = 0; i < nodeList.size(); i++) {
			RemoteHyperNode curNode = nodeList.get(i);	
			localAddNode(curNode);
		}		
		return true;
	}
	
	/*
	public void removeNode(Object inNode) {
		RemoteHyperNode remoteNode = RemoteHyperNode.convert(inNode);
		if (remoteNode != null) {
			localRemoveNode(remoteNode);
		}		
	}
	*/
	
	private Object[] getNodesHelper(List<RemoteHyperNode> nodes) {
		Object[] retObjects = new Object[nodes.size()];
		Iterator<RemoteHyperNode> nodesIt = nodes.iterator();
		int counter = 0;
		while (nodesIt.hasNext()) {
			retObjects[counter++] = nodesIt.next().toMap();
		}		
		return retObjects;	
	}
	
	public Object[] getAllPeers() {
		return getNodesHelper(hyperRings.getAllPrimaryPeers());
	}
	
	public Object[] getRandomNodes() {
		return getNodesHelper(hyperRings.getRandomNodes());
	}	
	
	public Object[] getRingInfo() {
		Object[] objArray = new Object[hyperRings.getNumRings()];
		for (int i = 0; i < objArray.length; i++) {
			objArray[i] = getNodesHelper(hyperRings.getRingPeers(i));
		}
		return objArray;
	}	
	
	public Object[] getCloserKeys(String keyword) {
		Set<String> origSet = entryContainer.getStrictCloserKeys(
				hyperRings.getKey(), keyword);
		return origSet.toArray();		
	}
	
	@SuppressWarnings("unchecked")
	public Map getEntries(String keyword) {
		Set<MovieEntry> retEntries = entryContainer.getEntries(keyword);
		if (retEntries == null) {
			return new HashMap();
		}
		// The XML-RPC friendly map representing the real map
		Map<String, Object[]> retMap = new HashMap<String, Object[]>();
		Object[] curObjArray = new Object[retEntries.size()];
		// Create an object from each of the MovieEntry and 
		// store it into the curObjectArray 
		int arrayCounter = 0;
		Iterator<MovieEntry> setIt = retEntries.iterator();
		while (setIt.hasNext()) {
			curObjArray[arrayCounter++] = setIt.next().xmlMarshal();
		}				
		retMap.put(keyword, curObjArray);
		return retMap;
	}
	
	
	/*
	// TODO: Change this to return the CloserKeywords, and then
	// have the other side fetch then Object[] from each keyword
	// one at a time.
	//
	// Return all objects that are closer to the keyword than 
	// this node's representative keyword. Must include the keyword
	@SuppressWarnings("unchecked")
	public Map getCloserEntries(String keyword) {
		Map<String, Set<MovieEntry>> origMap = 
			entryContainer.getCloserKeys(hyperRings.getKey(), keyword);
		
		// The XML-RPC friendly map representing the real map
		Map<String, Object[]> retMap = new HashMap<String, Object[]>();
		
		Iterator<String> origIt = origMap.keySet().iterator();
		while (origIt.hasNext()) {			
			String curKeyStr = origIt.next();
			Set<MovieEntry> curSet = origMap.get(curKeyStr);
			// The actual object array to store
			Object[] curObjArray = new Object[curSet.size()];
			// Create an object from each of the MovieEntry and 
			// store it into the curObjectArray 
			int arrayCounter = 0;
			Iterator<MovieEntry> setIt = curSet.iterator();
			while (setIt.hasNext()) {
				curObjArray[arrayCounter++] = setIt.next().xmlMarshal();
			}
			retMap.put(curKeyStr, curObjArray);
		}
		return retMap;
	}	
	*/	
	
	public Object[] getKClosestEntries(int val, Object[] keywords) {
		List<String> searchTerms = new ArrayList<String>();
		for (int i = 0; i < keywords.length; i++) {
			if (!(keywords[i] instanceof String)) {
				log.warn("Received unexpected parameters");
				return new Object[0];
			}
			searchTerms.add((String) keywords[i]);
		}
		System.out.println("%%%%%%%%%%%%%%%% Get " + val + " number of movies %%%%%%%%");
		System.out.println("---> For Keys: " + searchTerms);
		Set<MovieEntry> origSet = entryContainer.getKClosestKeys(val, searchTerms);
		Object[] objArray = new Object[origSet.size()];
		int arrayCounter = 0;
		Iterator<MovieEntry> movieIt = origSet.iterator();
		while (movieIt.hasNext()) {
			objArray[arrayCounter++] = movieIt.next().xmlMarshal();
		}
		return objArray;
	}	
	
	public Object[] getKClosestNodes(int val, String keyword) {
		// NOTE: Does not include itself.
		return getNodesHelper(hyperRings.getKClosestNodes(val, keyword));
	}	

	public Object[] getRandomWords() {
		return entryContainer.getRandomKeys(numWordsReturned).toArray();
	}
	
	public Boolean insertKeyEntry(String key, Object[] entry) {
		MovieEntry movieEntry = MovieEntry.xmlUnMarshal((Object)entry);			
		if (movieEntry != null) {
			entryContainer.addKeyEntry(key, movieEntry);	
		}
		return true;
	}	
	
	public List<String> findTopKMoviesConsumer(
			int inK, String searchStr, int charPerPerturb, int inTimeout,
			Consumer<MovieEntry> inConsumer) {
		
		String[] keysArray = entryContainer.removeSymbols(searchStr);
		List<String> keysList = new ArrayList<String>();
		for (int i = 0; i < keysArray.length; i++) {
			keysList.add(keysArray[i]);
		}		
		List<String> normalizeKeys = 
			entryContainer.normalizeKeywords(keysList);
		
		GetKClosestObjectsThread getMovie = 
			new GetKClosestObjectsThread(this, inK, normalizeKeys, 
					charPerPerturb, XmlRPCClient.MaxThreadsPerCall, inTimeout);
		getMovie.attachCB(inConsumer);
		if (getMovie.issueProducer()) {
			return normalizeKeys;
		}
		return null;
	}
	
	public List<MovieEntry> findTopKMovies(
			int inK, String searchStr, int charPerPerturb, int inTimeout) {
		
		log.info("****************** In findTopKMovies ***************");
		log.info("****************** In findTopKMovies ***************");
		log.info("****************** In findTopKMovies ***************");
		log.info("****************** In findTopKMovies ***************");
		log.info("****************** In findTopKMovies ***************");
		log.info("****************** In findTopKMovies ***************");
		log.info("****************** In findTopKMovies ***************");
		String[] keysArray = entryContainer.removeSymbols(searchStr);
		List<String> keysList = new ArrayList<String>();
		for (int i = 0; i < keysArray.length; i++) {
			keysList.add(keysArray[i]);
		}		
		List<String> normalizeKeys = 
			entryContainer.normalizeKeywords(keysList);		
		
		GetKClosestObjects getMovie = 
			new GetKClosestObjects(this, inK, normalizeKeys, charPerPerturb, 
					XmlRPCClient.MaxThreadsPerCall, inTimeout);
		getMovie.startGetBoundNodes();
		return getMovie.getConsumeResult();
	}
	
	public static Map<String, String> parseFile(String fileName) {		
		Map<String, String> keyValMap = new HashMap<String, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			while (true) {
				String curLine = in.readLine();
				if (curLine == null) {
					break;	// End of file
				}
				//System.out.println(curLine);
				String[] strArray = curLine.split(",", 3);
				if (strArray.length == 3) {
					String curStr = strArray[2];
					//MessageDigest md = MessageDigest.getInstance("SHA");
					//String sha1 = new String(md.digest(curStr.getBytes()));					
					keyValMap.put(curStr, curStr);
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return keyValMap;
	}
	
	// Taken from: http://www.javapractices.com/topic/TopicAction.do?Id=96
	public static String escapeForRegex(String aRegexFragment){
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = 
				new StringCharacterIterator(aRegexFragment);
		char character = iterator.current();
		while (character != CharacterIterator.DONE ){
			/*
			 * All literals need to have backslashes doubled.
			 */
			if (character == '.') {
				result.append("\\.");
			}
			else if (character == '\\') {
				result.append("\\\\");
			}
			else if (character == '?') {
				result.append("\\?");
			}
			else if (character == '*') {
				result.append("\\*");
			}
			else if (character == '+') {
				result.append("\\+");
			}
			else if (character == '&') {
				result.append("\\&");
			}
			else if (character == ':') {
				result.append("\\:");
			}
			else if (character == '{') {
				result.append("\\{");
			}
			else if (character == '}') {
				result.append("\\}");
			}
			else if (character == '[') {
				result.append("\\[");
			}
			else if (character == ']') {
				result.append("\\]");
			}
			else if (character == '(') {
				result.append("\\(");
			}
			else if (character == ')') {
				result.append("\\)");
			}
			else if (character == '^') {
				result.append("\\^");
			}
			else if (character == '$') {
				result.append("\\$");
			}
			else {
				//the char is not a special one
				//add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}
	
	public static Set<String> getCommonWords() {
		Set<String> commonWords = new HashSet<String>();		
		Properties commonProperties = new Properties();
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle(
				"org.cornell.hyper.overlay.CommonWords");
		} catch (MissingResourceException e) {
			log.error("Cannot find CommonWords.properties");
			return commonWords;
		}
		Enumeration<String> keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			final String key = (String) keys.nextElement();
			commonProperties.put(key, rb.getString(key));
		}
    	String rawCommonWords = 
			commonProperties.getProperty("CommonWords");
    	if (rawCommonWords != null) {
    		String[] wordsArray = rawCommonWords.split(" ");
    		for (int i = 0; i < wordsArray.length; i++) {
    			log.info("Adding common word: " + wordsArray[i]);
    			commonWords.add(wordsArray[i]);
    		}
    	}
	    return commonWords;
	}
	
	public static Set<String> getPunct() {
		Set<String> punctSet = new HashSet<String>();		
		Properties punctProperties = new Properties();
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle(
				"org.cornell.hyper.overlay.Punctuation");
		} catch (MissingResourceException e) {
			log.error("Cannot find Punctuation.properties");
			return punctSet;
		}
		Enumeration<String> keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			final String key = (String) keys.nextElement();
			punctProperties.put(key, rb.getString(key));
		}
		String rawPunctuations = 
				punctProperties.getProperty("Punctuation");
		rawPunctuations = escapeForRegex(rawPunctuations);
		if (rawPunctuations != null) {
			String[] wordsArray = rawPunctuations.split(" ");
			for (int i = 0; i < wordsArray.length; i++) {
				log.info("Adding punctuation: " + wordsArray[i]);
				punctSet.add(wordsArray[i]);
			}
		}
	    return punctSet;		
	}
	
	public static Set<String> getDefaultMovies() {
		Set<String> moviesSet = new HashSet<String>();
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle(
				"org.cornell.hyper.overlay.DefaultMovies");
		} catch (MissingResourceException e) {
			log.error("Cannot find Punctuation.properties");
			return moviesSet;
		}
		Enumeration<String> keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			final String key = (String) keys.nextElement();
			moviesSet.add(rb.getString(key));
		}
		return moviesSet;
	}
		
	public static HyperNode defaultServer(
			EntryContainer 			entryContainer,
			String					keyword,
			Set<RemoteHyperNode>	seedNodes, 
			List<URL> 				NATList,
			int						portNum) {
		
		String nodeID = new Integer(
				new Random().nextInt(1 << 20)).toString();
				
		HyperNode hyperNode = null;
		try {
			hyperNode = new HyperNode(
					defaultNodesPerRing, defaultRingLogBase, 
					defaultMaxDist, defaultGossipMS, 
					defaultRingMangeMS, nodeID, NATList, 
					entryContainer, keyword, seedNodes, portNum);
		} catch (HyperNodeException e) {
			
			e.printStackTrace();
			return null;
		}
		return hyperNode;
	}
	
	// Following private methods only used for test server 
	private static List<AddressPort> parseSeeds(
			String[] args, int argCounter) {
		// Parse seed nodes
		List<AddressPort> seedAddrs = new ArrayList<AddressPort>();
		for (; argCounter < args.length; argCounter++) {
			String curStr = args[argCounter];
			String[] addrPort = curStr.split(":");
			if (addrPort.length == 2) {
				try {
					InetAddress remoteAddr = InetAddress.getByName(addrPort[0]);
					String addStr = remoteAddr.getHostAddress();
					int port = Integer.parseInt(addrPort[1]);
					seedAddrs.add(new AddressPort(addStr, port));
					log.info("Remote node: " + addStr + ":" + port);
				} catch (UnknownHostException e) {
					log.warn("Cannot use seednode: " + curStr);					
				} catch (NumberFormatException e) {
					log.warn("Cannot use seednode: " + curStr);				
				}
			}
		}
		return seedAddrs;
	}
	
	private static void startClient(			
			EntryContainer 			entryContainer,
			Set<String> 			defaultKW,
			String[] 				args,
			int 					argCounter) {
				
		// Parse seed node
		List<AddressPort> seedAddrs = new ArrayList<AddressPort>();
		if (argCounter >= args.length) {
			log.error("Seed node must be specified");
			return;			
		}
		String curStr = args[argCounter++];
		String[] addrPort = curStr.split(":");
		if (addrPort.length == 2) {
			try {
				InetAddress remoteAddr = InetAddress.getByName(addrPort[0]);
				String addStr = remoteAddr.getHostAddress();
				int port = Integer.parseInt(addrPort[1]);
				seedAddrs.add(new AddressPort(addStr, port));
				log.info("Remote node: " + addStr + ":" + port);
			} catch (UnknownHostException e) {
				log.warn("Cannot use seednode: " + curStr);
				return;
			} catch (NumberFormatException e) {
				log.warn("Cannot use seednode: " + curStr);			
				return;
			}
		}
		
		if (argCounter >= args.length) {
			log.error("Search strings must be specified");
			return;			
		}		
		// Parse search keys
		StringBuffer searchStrBuf = new StringBuffer();
		for (int i = argCounter; i < args.length; i++) {
			searchStrBuf.append(args[i]);
			searchStrBuf.append(" ");
		}
		
		// Issue search for key and seeds
		KeyVal<String, Set<RemoteHyperNode>> curKeyVal = 
			findKeyAndSeeds(seedAddrs, defaultKW);		
		if (curKeyVal == null) {
			log.error("Cannot access any seed nodes, exiting");
			return;
		}
		
		// Get the seed node for use with the client
		Set<RemoteHyperNode> seedRemoteNodes = curKeyVal.getVal();
		
		//final int numResults 				= 32;
		//final int charPerPerturb 			= 4;
		//final int defaultSearchTimeout	= 30000;
		final int numResults 			= 8;
		final int charPerPerturb 		= 4;		
		final int defaultSearchTimeout	= 15000;
		String searchStr = searchStrBuf.toString().trim();
		
		List<MovieEntry> topMovies = XmlRPCClient.findTopKMovies(
				entryContainer, seedRemoteNodes, numResults, 
				searchStr, charPerPerturb, defaultSearchTimeout);
		
		String[] keysArray = entryContainer.removeSymbols(searchStr);
		List<String> keysList = new ArrayList<String>();
		for (int i = 0; i < keysArray.length; i++) {
			keysList.add(keysArray[i]);
		}
		List<String> normalizeKeys = 
			entryContainer.normalizeKeywords(keysList);		
		
		Iterator<MovieEntry> topIt = topMovies.iterator();
		while (topIt.hasNext()) {
			MovieEntry curMovieEntry = topIt.next();
			int curMovieDist = entryContainer.computeDistSum(
					curMovieEntry.getMovName(), normalizeKeys);
			System.out.println("Search Result: " + curMovieDist + 
					" " + curMovieEntry.getMovName());
		}
	}
	
	private static Map<String,String> parseMovieFile(String fileName) {		
		//Set<MovieEntry> movieSet = new HashSet<MovieEntry>();
		Map<String,String> movieMap = new HashMap<String,String>();
		final String torrentKey = "torrent_name";
		final String commentKey = "comment_str";
		final String urlKey		= "url"; 
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			Map<String, String> curTuple = new HashMap<String, String>();
			while (true) {
				String curLine = in.readLine();
				if (curLine == null) {
					break;	// End of file
				}
				String[] strArray = curLine.split("=", 2);							
				if (strArray.length == 2) {
					curTuple.put(strArray[0], strArray[1]);
				}
				if (curTuple.containsKey(torrentKey) &&
					curTuple.containsKey(commentKey) &&
					curTuple.containsKey(urlKey)) {
					movieMap.put(curTuple.get(torrentKey), curTuple.get(commentKey));
					curTuple.clear();
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return movieMap;
	}	
	
	private static String genSearchStr(List<String> strList, int charPerPerturb) {
		StringBuffer strBuf = new StringBuffer();
		int numTerms = (int)Math.ceil((2.0 * strList.size()) / 3);
		
		char[] charArray = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 
				'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 
				'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', 
				'8', '9', '0'};
		
		System.out.println("Char array is: " + String.valueOf(charArray));
		
		Random rand = new Random();
		for (int i = 0; i < numTerms; i++) {
			String curStr = strList.get(i);
			int numPerturb = (int)Math.round(
					((double)curStr.length()) / charPerPerturb);  
			char[] curArray = curStr.toCharArray();
			for (int j = 0; j < numPerturb; j++) {
				//curArray[j] = Byte.(byte)(63 + (rand.nextInt() % 64));
				curArray[j] = charArray[rand.nextInt(charArray.length)];
			}
			strBuf.append(curArray);
			strBuf.append(" ");
		}
		return strBuf.toString().trim();
	}
	
	private static void startAutomated(
			EntryContainer 			entryContainer,
			Set<String> 			defaultKW,
			String[] 				args,
			int 					argCounter) {
		//if (argCounter >= args.length) {
		//	log.error("Characters per perturbation must be specified");
		//	return;			
		//}
		//int charPerPerturb = Integer.parseInt(args[argCounter++]);		
		
		if (argCounter >= args.length) {
			log.error("Movie file must be specified");
			return;			
		}		
		Map<String,String> movieMap = HyperNode.parseMovieFile(args[argCounter++]);
		
		if (argCounter >= args.length) {
			log.error("Seed node must be specified");
			return;			
		}
		String curStr = args[argCounter++];
		// Parse seed node
		List<AddressPort> seedAddrs = new ArrayList<AddressPort>();
		String[] addrPort = curStr.split(":");
		if (addrPort.length == 2) {
			try {
				InetAddress remoteAddr = InetAddress.getByName(addrPort[0]);
				String addStr = remoteAddr.getHostAddress();
				int port = Integer.parseInt(addrPort[1]);
				seedAddrs.add(new AddressPort(addStr, port));
				log.info("Remote node: " + addStr + ":" + port);
			} catch (UnknownHostException e) {
				log.warn("Cannot use seednode: " + curStr);
				return;
			} catch (NumberFormatException e) {
				log.warn("Cannot use seednode: " + curStr);			
				return;
			}
		}		
		
		// Issue search for key and seeds
		KeyVal<String, Set<RemoteHyperNode>> curKeyVal = 
			findKeyAndSeeds(seedAddrs, defaultKW);		
		if (curKeyVal == null) {
			log.error("Cannot access any seed nodes, exiting");
			return;
		}
		
		// Get the seed node for use with the client
		Set<RemoteHyperNode> seedRemoteNodes = curKeyVal.getVal();
		
		Iterator<String> strIt = movieMap.keySet().iterator();
		while (strIt.hasNext()) {
			String urlStr = strIt.next();
			//final int numResults 			= 32;
			final int numResults 			= 10;
			//final int charPerPerturb 		= 4;
			final int defaultSearchTimeout	= 20000;
							
			String commentStr = movieMap.get(urlStr);
			String[] strArray = entryContainer.removeSymbols(commentStr);
			List<String> strList = new ArrayList<String>();
			for (int i = 0; i < strArray.length; i++) {
				strList.add(strArray[i]);
			}
			strList = entryContainer.normalizeKeywords(strList);
			if (strList.size() == 0) {
				System.out.println("Skipping movie entry");
				continue;
			}
			StringBuffer strBuf = new StringBuffer();
			for (int i = 0; i < strList.size(); i++) {
				strBuf.append(strList.get(i));
				strBuf.append(" ");				
			}
			String exactStr = strBuf.toString().trim();
			System.out.println("Exact search string is: " + exactStr);
			strBuf = null;	// Make sure we don't re-use it
			
			// Perform exact string search
			List<MovieEntry> topMovies = XmlRPCClient.findTopKMovies(
					entryContainer, seedRemoteNodes, numResults, 
					exactStr, 4, defaultSearchTimeout);		
			
			Iterator<MovieEntry> topIt = topMovies.iterator();
			boolean movieFound = false;
			while (topIt.hasNext()) {
				MovieEntry curMovieEntry = topIt.next();
				/*
				if (curMovieEntry.getMagLink().equals(urlStr)) {
					// We can find the movie using its exact strings
					movieFound = true;
					break;
				}
				*/
				
				if (curMovieEntry.getMovName().equals(commentStr)) {
					// We can find the movie using its exact strings
					movieFound = true;
					break;
				}				
				
			}
			if (!movieFound) {				
				System.out.println("Skipping movie: " + urlStr);
				continue;
			}			
			
			for (int charPerPerturb = 1; charPerPerturb <= 6; charPerPerturb++) {
	
				
				String searchStr = HyperNode.genSearchStr(
						strList, charPerPerturb).trim();			
				//String searchStr = searchStrBuf.toString().trim();
				
				System.out.println("Search string for " + charPerPerturb + ": " + searchStr);
				
				topMovies = XmlRPCClient.findTopKMovies(
						entryContainer, seedRemoteNodes, numResults, 
						searchStr, charPerPerturb, defaultSearchTimeout);		
				
				topIt = topMovies.iterator();
				movieFound = false;
				while (topIt.hasNext()) {
					MovieEntry curMovieEntry = topIt.next();
					//if (curMovieEntry.getMagLink().equals(urlStr)) {
					if (curMovieEntry.getMovName().equals(commentStr)) {						
						System.out.println("SUCCESS: " + charPerPerturb + " " + urlStr);
						movieFound = true;
						break;
					}
				}
				if (!movieFound) {
					System.out.println("FAILURE: " + charPerPerturb + " " + urlStr);
				}
			}
		}
	}
	
	private static void startServer(
			EntryContainer 			entryContainer,
			Set<String> 			defaultKW,
			String[] 				args,
			int 					argCounter) {
		
		int localPort = 3184;	// Default port
		if (args.length > argCounter) {
			try {
				localPort = Integer.parseInt(args[argCounter++]);
			} catch (NumberFormatException e) {
				log.warn("Cannot parse the port value");
				return;
			}		
		}
		
		// Parse seed nodes
		List<AddressPort> seedAddrs = parseSeeds(args, argCounter);
				
		// Issue search for key and seeds
		KeyVal<String, Set<RemoteHyperNode>> curKeyVal = 
			findKeyAndSeeds(seedAddrs, defaultKW);		
		if (curKeyVal == null) {
			log.error("Cannot access any seed nodes, exiting");
			return;
		}
		
		// Get the keyword and seed node for this server
		String keyword = curKeyVal.getKey();
		Set<RemoteHyperNode> seedNodes = curKeyVal.getVal();
		
		log.info("Number of available seed nodes: " + seedNodes.size());
		
		// Load default URLs
		List<URL> urlList = new ArrayList<URL>();
		try {
			URL serverURL1 = new URL(
					"http://localhost:3000/cgi-bin/query_ip");
			URL serverURL2 = new URL(
					"http://gattaca.cs.cornell.edu/cgi-bin/query_ip");
			urlList.add(serverURL1);
			urlList.add(serverURL2);
		} catch (MalformedURLException e) {
			log.error("Cannot load local NAT URL");
		}
		
		// Start the server
		HyperNode hyperNode = defaultServer(entryContainer, 
				keyword, seedNodes, urlList, localPort);
		
		if (hyperNode != null) {
			log.info("Node constructed successfully");
		} else {
			log.error("Cannot start HyperNode, exiting");
			return;
		}		
		while (true) {		
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private static void startTest(String[] args, int argCounter) {
		AddressPort seedAddr = null;
		if (argCounter >= args.length) {
			log.error("FAILURE: Seed node must be specified");
			return;			
		}
		String curStr = args[argCounter++];
		String[] addrPort = curStr.split(":");
		if (addrPort.length == 2) {
			try {
				InetAddress remoteAddr = InetAddress.getByName(addrPort[0]);
				String addStr = remoteAddr.getHostAddress();
				int port = Integer.parseInt(addrPort[1]);
				seedAddr = new AddressPort(addStr, port);			
			} catch (UnknownHostException e) {
				log.error("FAILURE: Cannot use seednode: " + curStr);
				return;
			} catch (NumberFormatException e) {
				log.error("FAILURE: Cannot use seednode: " + curStr);			
				return;
			}
		}
		if (seedAddr == null) {
			log.error("FAILURE: Cannot contact node");
			return;
		}		
		TimingOutCallback callback = XmlRPCClient.getNode(seedAddr);
		Object res = null;
		try {
			res = callback.waitForResponse();
		} catch (TimeoutException e) {
		} catch (Throwable e) 		 {}
		if (res == null) {
			log.error("FAILURE: Timed out waiting for node response");
			return;
		}
		// See if the node has changed its key
		RemoteHyperNode resNode = RemoteHyperNode.convert(res);
		if (resNode == null) {
			log.error("FAILURE: Received unexpected response");
			return;					
		}
		log.info("SUCCESS: Node test complete");		
	}
	
	private static void startInfo(String[] args, int argCounter) {
		AddressPort seedAddr = null;
		if (argCounter >= args.length) {
			log.error("FAILURE: Seed node must be specified");
			return;			
		}
		String curStr = args[argCounter++];
		String[] addrPort = curStr.split(":");
		if (addrPort.length == 2) {
			try {
				InetAddress remoteAddr = InetAddress.getByName(addrPort[0]);
				String addStr = remoteAddr.getHostAddress();
				int port = Integer.parseInt(addrPort[1]);
				seedAddr = new AddressPort(addStr, port);			
			} catch (UnknownHostException e) {
				log.error("FAILURE: Cannot use seednode: " + curStr);
				return;
			} catch (NumberFormatException e) {
				log.error("FAILURE: Cannot use seednode: " + curStr);			
				return;
			}
		}
		if (seedAddr == null) {
			log.error("FAILURE: Cannot contact node");
			return;
		}		
		// Try to contact the node with a given timeout
		TimingOutCallback callback = XmlRPCClient.getNode(seedAddr);			
		Object res = null;
		try {
			res = callback.waitForResponse();
		} catch (TimeoutException e) {
		} catch (Throwable e) 		 {}
		if (res == null) {
			log.error("FAILURE: Timed out waiting for node response");
			return;
		}
		// See if the node has changed its key
		RemoteHyperNode resNode = RemoteHyperNode.convert(res);
		if (resNode == null) {
			log.error("FAILURE: Received unexpected response");
			return;			
		}
		log.info("NODE_KEY: " + resNode.getKey());
				
		callback = XmlRPCClient.getRingInfo(seedAddr);
		res = null;
		try {
			res = callback.waitForResponse();
		} catch (TimeoutException e) {
		} catch (Throwable e) 		 {}
		if (res == null) {
			log.error("FAILURE: Timed out waiting for node response");
			return;
		}
		if (!(res instanceof Object[])) {
			log.error("FAILURE: Received unexpected response");
			return;
		}
		Object[] objArray = (Object[]) res;		
		for (int i = 0; i < objArray.length; i++) {
			log.info("Ring: " + i);
			if (!(objArray[i] instanceof Object[])) {
				log.error("FAILURE: Received unexpected response");
				return;
			}
			Object[] innerObjArray = (Object[]) objArray[i];
			for (int j = 0; j < innerObjArray.length; j++) {
				// See if the node has changed its key
				resNode = RemoteHyperNode.convert(innerObjArray[j]);
				if (resNode == null) {
					log.error("FAILURE: Received unexpected response");
					return;					
				}
				log.info("--> " + resNode.getIP() + ":" + resNode.getPort() 
						+ " with key " + resNode.getKey() + " and ID " 
						+ resNode.getNodeID());
			}			
		}
		log.info("SUCCESS: Node test complete");	
	}	
	
	/* Main test method
	 * ---------------------------------------------------------------
	 */	
	public static void main(String[] args) 
			throws HyperNodeException, IOException, XmlRpcException {
		// Configure the log system
		BasicConfigurator.configure();		
		log.setLevel(Level.INFO);
		
		// Set the XML-RPC logger to log only warnings and up.
		Logger xmlServerLog = Logger.getLogger(
				"org.apache.xmlrpc.server.XmlRpcStreamServer");
		xmlServerLog.setLevel(Level.INFO);
		
		// Set the XML-RPC logger to log only warnings and up.
		Logger xmlClientLog = Logger.getLogger(
				"org.cornell.hyper.overlay.XmlRPCClient");
		xmlClientLog.setLevel(Level.INFO);
				
		EntryContainer entryContainer = 
			new EntryContainer(getPunct(), getCommonWords());
		
		Set<String> defaultKW = new HashSet<String>();
		Iterator<String> movieIt = HyperNode.getDefaultMovies().iterator();		
		while (movieIt.hasNext()) {
			String[] curKey = entryContainer.removeSymbols(movieIt.next());
			ArrayList<String> curList = new ArrayList<String>();
			for (int i = 0; i < curKey.length; i++) {
				curList.add(curKey[i]);
			}
			defaultKW.addAll(entryContainer.normalizeKeywords(curList));
		}
		
		if (args.length == 0) {
			log.error("Must specify client/server");
			return;
		}
		
		int argCounter 	= 0;
		String nodeType	= args[argCounter++];		
		if (nodeType.toLowerCase().equals("server")) {
			startServer(entryContainer, defaultKW, args, argCounter);
		} else if (nodeType.toLowerCase().equals("client")) {
			startClient(entryContainer, defaultKW, args, argCounter);
		} else if (nodeType.toLowerCase().equals("automated")) {
			startAutomated(entryContainer, defaultKW, args, argCounter);			
		} else if (nodeType.toLowerCase().equals("test")) {
			startTest(args, argCounter);
		} else if (nodeType.toLowerCase().equals("info")) {
			startInfo(args, argCounter);
		} else {
			log.error("Must be either server/client/automated/test/info");
			return;
		}
	}
}
