package org.cornell.hyper.overlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// TODO: Get rid of all apache logging and replace with Azureus logging.
// TODO: Add JavaDocs to this class.

import org.apache.log4j.Logger;

@SuppressWarnings("unchecked")
public class GetKClosestObjectsThread extends Thread 
		implements Producer<MovieEntry>, Consumer{
	
	private List<Consumer<MovieEntry>> 	consumersList;	
	private Set<GetKClosestNodes> 		getClosestNodesSet;
	private List<String>				keywords;
	private Set<RemoteHyperNode>		newNodesSet;
	private Set<RemoteHyperNode>		pendingNodesSet;	
	private Set<RemoteHyperNode>		doneNodesSet;
	private final int					kMovies;
	private final int					timeoutMS;
	private final int					charPerPerturb;	
	private boolean						done			= false;	
	private HyperNode					localNode 		= null;
	private final int					minKNodes		= 8;
	private final double				nodeTimeoutFrac	= (1.0 / 3.0);	
	
	private static Logger log = Logger.getLogger(
			GetKClosestObjectsThread.class.getName());
	
	/**
	 * The nodes timeout should only be a fraction of the total query time,
	 * as we need time to fetch the movie entries.
	 * 
	 * @return The timeout in MS for fetching nodes.
	 */
	private int getNodesTimeoutMS() {
		return (int)(timeoutMS * nodeTimeoutFrac);  // Round down
	}
		
	public int getTimeoutMS() { return timeoutMS; }
		
	/**
	 * Constructor for a GetKClosestObjectsThread when a local node
	 * is not available.
	 * 
	 * @param candidateNodes	Nodes to check for objects
	 * @param badNodes			Nodes to ignore
	 * @param inKMovies			Number of movies to fetch from each node		
	 * @param inKeywords		Search keywords	
	 * @param inCharPerPerturb	Upper bound on the number of characters per perturbation
	 * @param maxThreadsPerKW	Max number of threads more keyword
	 * @param inTimeoutMS		Timeout for entire search
	 */
	public GetKClosestObjectsThread(
			Set<RemoteHyperNode> candidateNodes, 
			Set<RemoteHyperNode> badNodes,
			int inKMovies, final List<String> inKeywords, 
			int inCharPerPerturb, int maxThreadsPerKW, 
			int inTimeoutMS) {		
		this(inKMovies, inKeywords, inCharPerPerturb, maxThreadsPerKW, inTimeoutMS);
		// Iterate through the keywords and get the K closest nodes for each.
		Iterator<String> keyIt = keywords.iterator();
		while (keyIt.hasNext()) {
			GetKClosestNodes curConsume = new GetKClosestNodes(
				candidateNodes, new HashSet<RemoteHyperNode>(), badNodes, 
				keyIt.next(), charPerPerturb, minKNodes, maxThreadsPerKW, 
				getNodesTimeoutMS(), false);			
			getClosestNodesSet.add(curConsume);
			curConsume.attachCB(this);
		}
	}
	
	/**
	 * Constructor for a GetKClosestObjectsThread when there is a local node.
	 * 
	 * @param inLocalNode		Local node object
	 * @param inKMovies			Number of movies to fetch from each node		
	 * @param inKeywords		Search keywords	
	 * @param inCharPerPerturb	Upper bound on the number of characters per perturbation
	 * @param maxThreadsPerKW	Max number of threads more keyword
	 * @param inTimeoutMS		Timeout for entire search
	 */
	public GetKClosestObjectsThread(
			HyperNode inLocalNode, int inKMovies, 
			final List<String> inKeywords, int inCharPerPerturb, 
			int maxThreadsPerKW, int inTimeoutMS) {
		this(inKMovies, inKeywords, inCharPerPerturb, maxThreadsPerKW, inTimeoutMS);		
		// Iterate through the keywords and get the K closest nodes for each.
		Iterator<String> keyIt = keywords.iterator();
		while (keyIt.hasNext()) {
			GetKClosestNodes curConsume = new GetKClosestNodes(
				localNode, keyIt.next(), charPerPerturb, minKNodes, 
				maxThreadsPerKW, getNodesTimeoutMS(), false);					
			getClosestNodesSet.add(curConsume);
			curConsume.attachCB(this);
		}
	}
	
	/**
	 * Factored out the common constructor
	 */
	private GetKClosestObjectsThread(
			int inKMovies, List<String> inKeywords, 
			int inCharPerPerturb, int maxThreadsPerKW, 
			int inTimeoutMS) {
		timeoutMS			= inTimeoutMS;
		kMovies				= inKMovies;
		charPerPerturb		= inCharPerPerturb;
		newNodesSet			= new HashSet<RemoteHyperNode>();
		pendingNodesSet		= new HashSet<RemoteHyperNode>();
		doneNodesSet		= new HashSet<RemoteHyperNode>();
		keywords 			= new ArrayList<String>(inKeywords);		
		getClosestNodesSet	= new HashSet<GetKClosestNodes>();
		consumersList 		= new ArrayList<Consumer<MovieEntry>>();
	}
	
	/**
	 * Calls the main blocking method in a separate thread.
	 */
	public void run() { blockOnAsync(); }	
	
	/**
	 * Main blocking method.
	 */
	private synchronized void blockOnAsync() {
		// Get the start time
		long startTime = (new Date()).getTime();
		log.info("Issueing " + getClosestNodesSet.size() + "GetKClosestNodes");
		
		// Iterate through the GetKClosestNodes producers
		// and start each one.
		Iterator<GetKClosestNodes> curIt = getClosestNodesSet.iterator();
		while (curIt.hasNext()) {
			curIt.next().issueProducer();
		}
		
		// Re-fetch the current time just in case, and calculate 
		// the amount of time remaining.
		long curTime = (new Date()).getTime();
		long remTime = timeoutMS - (curTime - startTime);
		
		// Keep waiting if there is time remaining and there is 
		// still work to be done.
		while (!done && remTime > 0 && (getClosestNodesSet.size() > 0 
				|| pendingNodesSet.size() > 0)) {			
			try {
				log.info("Blocking on GetKClosestMoviesThread");
				wait(remTime);	// Block for the remaining time
				log.info("GetKClosestMoviesThread unblocked");
			} catch (InterruptedException e) {
				break; // If interrupted, just break out of the loop
			}
			issueFetches(); // See if any nodes are available for fetches
			curTime = (new Date()).getTime();
			remTime = timeoutMS - (curTime - startTime);
			//System.err.println("Time remaining is " + remTime);
		}		
		// Done with all the fetches. All methods are short circuited
		// after the done is set to true.
		done = true; 
		
		// Tell all the consumers that it has completed.
		Iterator<Consumer<MovieEntry>> conIt = consumersList.iterator();
		while (conIt.hasNext()) {
			conIt.next().producerComplete(this, true);
		}
		consumersList.clear();		
		//System.err.println("**************** Block Async complete ***********************");
	}
	
	/**
	 * Issue object fetches from the collected nodes. If the closest node is
	 * the local node, just short circuit it and get the movie entries.
	 * 
	 * TODO: Should the fetches be staggered to reduce burst load? That would
	 * mean a close but unresponsive node can clog up the search. Perhaps a 
	 * better solution is to provide a slider/option that allows the user to
	 * choose a longer wait time, given that partial results are shown.
	 */
	private synchronized void issueFetches() {		
		log.info("Fetching objects");
		if (done) { return; }
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
				Iterator<MovieEntry> movieIt = localNode.
					getLocalKClosestEntries(kMovies, keywords).iterator();
				while (movieIt.hasNext()) {
					sendToConsumers(movieIt.next());
				}
				doneNodesSet.add(curNode);
			}
		}
	}

	/**
	 * Attaching a callback to this producer. 
	 */
	public synchronized void attachCB(Consumer<MovieEntry> inConsume) {
		consumersList.add(inConsume);
	}

	/**
	 * Start this producer.
	 */
	public synchronized boolean issueProducer() {
		// Check that there is at least one consumer.
		if (consumersList.size() == 0) {
			return false;
		}
		start();  // Start the thread up and return success.
		return true;
	}

	/** 
	 * Returns whether this consumer is done or not.
	 */
	public synchronized boolean checkConsumer(Producer inProduce) {
		return !done;
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
	
	/** 
	 * Send movie results to the consumers
	 * 
	 * @param movieEntry
	 */ 
	private synchronized void sendToConsumers(MovieEntry movieEntry) {
		if (done) { return; }
		Iterator<Consumer<MovieEntry>> conIt = consumersList.iterator();
		while (conIt.hasNext()) {
			// Remove the consumer if it is done
			Consumer<MovieEntry> curConsume = conIt.next(); 
			if (!curConsume.newProducerResult(this, movieEntry)) {
				// Calling producerComplete should not be necessary
				// if the consumer is telling to producer to stop.
				//curConsume.producerComplete(this, true);
				conIt.remove();
			}
		}
		if (consumersList.size() == 0 ) {
			done = true; // No more consumers, can safely stop 
		}
	}
	
	/** 
	 * Handle new movie entries that come in.
	 * 
	 * @param inProduce
	 * @param movies
	 * @return True unless the thread has completed.
	 */
	private synchronized boolean handleNewObjects(
			DistKObjectsProducer inProduce, Collection<MovieEntry> movies) {
		log.info("In handleNewObjects");
		if (done) { return false; }
		Iterator<MovieEntry> movieIt = movies.iterator();
		while (movieIt.hasNext()) {
			sendToConsumers(movieIt.next());
		}
		notify();	// This notify is probably not necessary atm
		return true;
	}

	/** 
	 * Handle new nodes that come in.
	 * 
	 * @param inProduce
	 * @param curNode
	 * @return True unless the thread has completed.
	 */
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
		getClosestNodesSet.remove(inProduce);
		notify();
	}
}