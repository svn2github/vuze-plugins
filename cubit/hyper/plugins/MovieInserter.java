package hyper.plugins;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.cornell.hyper.overlay.*;
import hyper.keygen.*;

import org.apache.log4j.Logger;

public class MovieInserter extends Thread {
	public class InserterThread extends Thread {
		private MovieEntry curMovie;
		
		public InserterThread(MovieEntry inMovie) {
			curMovie = inMovie;
		}			
		
		public void run() {
			System.out.println("Inserting movie " + curMovie.getTorName());
			HyperNode.insertEntry(getEC(), getSeedNodes(), 
					MovieAdder.DefaultNumReplica, curMovie);
			inserterThreadComplete();
		}
	}	
	
	private boolean done 			= false;
	private int numThreads 			= 0;
	private final int maxThreads	= 4;
	
	private EntryContainer curContainer;	
	private Set<RemoteHyperNode> seedNodes;
	private LinkedList<MovieEntry> movieList;
	
	private static Logger log = Logger.getLogger(MovieInserter.class.getName());
	
	private synchronized EntryContainer getEC() 			 { return curContainer; }
	private synchronized Set<RemoteHyperNode> getSeedNodes() { return seedNodes; 	}	
	
	public MovieInserter(EntryContainer inContainer, Set<RemoteHyperNode> inSeedNodes) {
		movieList = new LinkedList<MovieEntry>();
		curContainer = inContainer;
		seedNodes = new HashSet<RemoteHyperNode>(inSeedNodes);
	}
		
	private synchronized void createInserterThread(MovieEntry curMovie) {
		numThreads++;
		new InserterThread(curMovie).start();		
	}
	
	private synchronized void inserterThreadComplete() {
		numThreads--;
		notify();
	}
	
	//public synchronized boolean checkConsumer(Producer<MovieEntry> inProduce) {
	//	return !done;
	//}
	
	public synchronized void stopMovieInserter() {
		done = true;
		notify();
	}
	
	public synchronized void updateSeeder(Set<RemoteHyperNode> inSeedNodes) {
		seedNodes = new HashSet<RemoteHyperNode>(inSeedNodes);		
	}
	
	//public synchronized boolean newProducerResult(MovieEntry result) {
	public synchronized boolean newMovie(MovieEntry result) {
		// TODO Auto-generated method stub
		// We don't actually touch the producer. It is mainly there to 
		// distinguish one from another in case a consumer has multiple 
		// producers. Calling methods in it runs the risk of introducing 
		// circular dependencies if newProducerResult is called in an 
		// unexpected manner.
		if (done) return false; 	// Just exit right away
		movieList.addLast(result);	// Add to the new movies list
		notify(); 					// Wake-up the master thread
		return true;
	}	
	
	public void run() { blockOnAsync(); }	
	
	// Block and wait for results to come back, or the producer to timeout.
	private synchronized boolean blockOnAsync() {
		while (!done) {
			try {
				log.info("Blocking in MovieInserter");
				wait();
				log.info("Unblocking in MovieInserter");
			} catch (InterruptedException e) {
				log.warn("Wait interrupted");
				break;
			}
			while (numThreads < maxThreads && !movieList.isEmpty()) {
				System.out.println("Inserting movie");
				createInserterThread(movieList.pop());
			}	
		}
		done = true;
		return true;		
	}
}
