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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.AsyncCallback;
import org.apache.xmlrpc.client.TimingOutCallback;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.TimingOutCallback.TimeoutException;

public class XmlRPCClient {
	private static Logger log = Logger.getLogger(XmlRPCClient.class.getName());
	
	protected static class LogCallback implements AsyncCallback {
		private final Logger log = Logger.getLogger(LogCallback.class.getName());
		
		public LogCallback(Level cbLevel) {
			log.setLevel(cbLevel);
		}
		
		public void handleError(XmlRpcRequest pRequest, Throwable pError) {
			log.warn("AsyncCallback failed for:" + pRequest.toString());
		}

		public void handleResult(XmlRpcRequest pRequest, Object pResult) {
			log.info("AsyncCallback succeeded for: " + pRequest.toString());
		}
	}	
	
	private final static int NATCallbackTimeoutMS 		= 5000;
	private final static int QueryCallbackTimeoutMS 	= 3000;
	private final static int GetKClosestNodesTimeoutMS	= 5000;
	private final static int InsertCallbackTimeoutMS 	= 5000;
	private final static int GetAllPeersTimeoutMS 		= 5000;
	public final static int MaxThreadsPerCall			= 8;
	public final static int minGetK						= 8;
	//private final static int maxSearchDist				= 4;
	
	private static URL createURL(String nodeAddr, int nodePort, String path) 
			throws MalformedURLException {
		StringBuffer buf = new StringBuffer();
		buf.append("http://");
		buf.append(nodeAddr);
		buf.append(":");
		buf.append((new Integer(nodePort)).toString());
		buf.append(path);
		return new URL(buf.toString());
	}
	
	private static XmlRpcClient configClient(RemoteHyperNode remoteNode) 
			throws MalformedURLException {
		URL serverURL = createURL(remoteNode.getIP(), remoteNode.getPort(), "");
		// Create the config and the XmlRPCClient for this query
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();					
		config.setServerURL(serverURL);			
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		return client;
	}	
	
	// TODO: Make this follow a producer/consumer model when there is time
	private static TimingOutCallback getNonNATAddrHelper(URL serverURL, int localPort) {
		try {
			// Create the config and the XmlRPCClient for this query
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();			
			config.setServerURL(serverURL);			
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// Set the parameters for the RPC
			Object[] params = new Object[1];
			params[0] = new Integer(localPort);
			
			// Perform the RPC
			TimingOutCallback callback = 
				new TimingOutCallback(NATCallbackTimeoutMS);
			client.executeAsync("self_IP", params, callback);
			
			// Return the callback
			return callback;			
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}
		return null;
	}
		
	public static String getNonNatAddr(URL serverURL, int localPort) {
		String selfIP = null;
		if (serverURL != null) {
			try {
				TimingOutCallback callback = 
					getNonNATAddrHelper(serverURL, localPort);			
				Object newObj = callback.waitForResponse();
				if (newObj instanceof Object[] && ((Object[])newObj).length == 2) {
					Object[] objArray 	= (Object[]) newObj;
					String IP 			= (String) objArray[0];
					Boolean notNATed	= (Boolean) objArray[1];
												
					if (notNATed.booleanValue() == true) {
						selfIP = IP;					
					}
				} else {				
					System.out.println("Unexpected return value.");
				}
			} catch (TimeoutException e) {
				System.out.println("Called to self_IP timed out.");
			} catch (Throwable e) {
				System.out.println("Exception received in getNonNatAddrBlock: " + e);
			}
		}
		return selfIP;
	}
	
	public static RemoteHyperNode closestInSet(String keyword,
			Set<RemoteHyperNode> nodeSet, Set<RemoteHyperNode> excludeSet) {
		RemoteHyperNode closestNode	= null;
		int closestDist 			= -1;
		Iterator<RemoteHyperNode> nodeIt = nodeSet.iterator();
		while (nodeIt.hasNext()) {
			RemoteHyperNode curNode = nodeIt.next();
			if (excludeSet.contains(curNode)) {
				continue;	// 
			}
			int curDist =  EditDistance.computeEditDistance(
					keyword, curNode.getKey());
			if (closestNode == null || curDist < closestDist) {
				closestNode = curNode;
				closestDist = curDist;
			}
		}
		return closestNode;
	}
	
	public static List<RemoteHyperNode> closestKInSet(int kVal, String keyword,
			Set<RemoteHyperNode> nodeSet, Set<RemoteHyperNode> excludeSet) {
		Set<RemoteHyperNode> localExcludeSet = new HashSet<RemoteHyperNode>(excludeSet);
		List<RemoteHyperNode> retList = new ArrayList<RemoteHyperNode>();
		while (kVal-- > 0) {
			RemoteHyperNode curNode = closestInSet(keyword, nodeSet, localExcludeSet);
			if (curNode == null) {
				break;	// Done
			}
			retList.add(curNode);
			localExcludeSet.add(curNode);
		}
		return retList;
	}
			
	public static List<RemoteHyperNode> getKClosestNodesHelper(
			int kVal, String keyword, 
			Set<RemoteHyperNode> availNodes,
			Set<RemoteHyperNode> checkedSet,			
			Set<RemoteHyperNode> badSet) {
		log.info("getKClosestNodesHelper: " + keyword + ", " + kVal);
		GetKClosestNodes consume = new GetKClosestNodes(
				availNodes, checkedSet, badSet, keyword, 
				Integer.MAX_VALUE, kVal, MaxThreadsPerCall, 
				GetKClosestNodesTimeoutMS, true);
		if (!consume.issueProducerBlock()) {
			log.error("Error issueing GetKClosestNodes producer");
			return null;
		}
		List<RemoteHyperNode> retList = consume.getConsumeResult();
		log.info("Closest nodes are:");
		Iterator<RemoteHyperNode> listIt = retList.iterator();
		while (listIt.hasNext()) {
			RemoteHyperNode curNode = listIt.next();
			log.info("--> " + curNode.getKey() + ", " + 
					curNode.getIP() + ":" + curNode.getPort());
		}
		return retList;
	}
	
	public static TimingOutCallback getNode(AddressPort curAddrPort) {		
		try {
			URL serverURL = createURL(curAddrPort.getAddr(), curAddrPort.getPort(), "");
			// Create the config and the XmlRPCClient for this query
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();					
			config.setServerURL(serverURL);			
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// Perform the RPC
			TimingOutCallback callback = 
				new TimingOutCallback(QueryCallbackTimeoutMS);
			client.executeAsync(
				"RemoteHyperInterface.getNode", new Object[0], callback);			
			// Return the callback
			return callback;
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}
		return null;
	}
	
	public static TimingOutCallback getRingInfo(AddressPort curAddrPort) {		
		try {
			URL serverURL = createURL(curAddrPort.getAddr(), curAddrPort.getPort(), "");
			// Create the config and the XmlRPCClient for this query
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();					
			config.setServerURL(serverURL);			
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// Perform the RPC
			TimingOutCallback callback = 
				new TimingOutCallback(QueryCallbackTimeoutMS);
			client.executeAsync(
				"RemoteHyperInterface.getRingInfo", new Object[0], callback);			
			// Return the callback
			return callback;
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}
		return null;
	}	
	
	public static void getKClosestMoviesAsync(RemoteHyperNode curNode,
			int kVal, List<String> keywords, AsyncCallback callback) { 
		try {
			XmlRpcClient client = configClient(curNode);
			Object[] params = new Object[2];
			params[0] = kVal;
			params[1] = keywords.toArray();
			client.executeAsync(
					"RemoteHyperInterface.getKClosestEntries", params, callback);
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}
	}	

	public static void getRandomWordsAsync(
			RemoteHyperNode seedNode, AsyncCallback callback) {
		try {			
			XmlRpcClient client = configClient(seedNode);
			client.executeAsync(
					"RemoteHyperInterface.getRandomWords", new Object[0], callback);
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}
	}		

	public static void getRandomNodesAsync(
			RemoteHyperNode seedNode, AsyncCallback callback) {
		try {
			XmlRpcClient client = configClient(seedNode);			
			client.executeAsync("RemoteHyperInterface.getRandomNodes", new Object[0], callback);
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}		
	}	

	public static TimingOutCallback getKClosestNodes(
			RemoteHyperNode curNode, int val, String keyword) {
		try {
			XmlRpcClient client = configClient(curNode);			
			// Set the parameters for the RPC
			Object[] params = new Object[2];
			params[0] = new Integer(val);
			params[1] = keyword;
			TimingOutCallback callback = 
				new TimingOutCallback(QueryCallbackTimeoutMS);			
			client.executeAsync("RemoteHyperInterface.getKClosestNodes", params, callback);
			return callback;
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}	
		return null;
	}
		
	public static boolean getKClosestNodesAsync(
			RemoteHyperNode curNode, int val, String keyword, AsyncCallback cb) {
		try {
			XmlRpcClient client = configClient(curNode);			
			// Set the parameters for the RPC
			Object[] params = new Object[2];
			params[0] = new Integer(val);
			params[1] = keyword;		
			client.executeAsync(
					"RemoteHyperInterface.getKClosestNodes", params, cb);
			return true;
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}	
		return false;
	}

	public static TimingOutCallback getAllPeers(RemoteHyperNode curNode) {
		try {
			XmlRpcClient client = configClient(curNode);
			TimingOutCallback callback = 
				new TimingOutCallback(GetAllPeersTimeoutMS);			
			client.executeAsync("RemoteHyperInterface.getAllPeers", new Object[0], callback);
			return callback;
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		}	
		return null;
	}	
	
	// Finds the K Closest nodes to the keyword, given a list of 
	// starting remote nodes.
	public static List<RemoteHyperNode> getKClosestNodesBlock(
			Collection<RemoteHyperNode> remoteNodes, int kVal, String keyword) {		
		return getKClosestNodesHelper(kVal, keyword,
				new HashSet<RemoteHyperNode>(remoteNodes),
				new HashSet<RemoteHyperNode>(), 
				new HashSet<RemoteHyperNode>()); 		
	}

	// Performs a remote insert, should be asynchronous. The async callback
	// should just print out into the log.
	public static void insertKeyEntry(
			RemoteHyperNode curNode, String curKey, MovieEntry movieEntry) {
		try {
			XmlRpcClient client = configClient(curNode);						
			Object[] params = new Object[2];
			params[0] = curKey;
			params[1] = movieEntry.xmlMarshal();		
			LogCallback callback = new XmlRPCClient.LogCallback(Level.WARN);			
			client.executeAsync(
					"RemoteHyperInterface.insertKeyEntry", params, callback);
		} catch (MalformedURLException e) {
			log.warn("Malformed URL");
		} catch (XmlRpcException e) {
			log.warn("XmlRPCException received.");
		}
	}

	/*
	public static void getCloserEntries(
			RemoteHyperNode remoteNode, String keyword, AsyncCallback callback) {
		try {
			XmlRpcClient client = configClient(remoteNode);			
			Object[] params = new Object[1];
			params[0] = keyword;				
			client.executeAsync("RemoteHyperInterface.getCloserEntries", params, callback);
		} catch (MalformedURLException e) {
			log.warn("Malformed URL");
		} catch (XmlRpcException e) {
			log.warn("XmlRPCException received.");
		}			
	}
	*/
	
	public static void getEntries(
			RemoteHyperNode remoteNode, String keyword, AsyncCallback callback) {
		try {
			XmlRpcClient client = configClient(remoteNode);			
			Object[] params = new Object[1];
			params[0] = keyword;				
			client.executeAsync("RemoteHyperInterface.getEntries", params, callback);
		} catch (MalformedURLException e) {
			log.warn("Malformed URL");
		} catch (XmlRpcException e) {
			log.warn("XmlRPCException received.");
		}			
	}
	
	public static void getCloserKeys(
			RemoteHyperNode remoteNode, String keyword, AsyncCallback callback) {
		try {
			XmlRpcClient client = configClient(remoteNode);			
			Object[] params = new Object[1];
			params[0] = keyword;				
			client.executeAsync("RemoteHyperInterface.getCloserKeys", params, callback);
		} catch (MalformedURLException e) {
			log.warn("Malformed URL");
		} catch (XmlRpcException e) {
			log.warn("XmlRPCException received.");
		}			
	}	

	public static boolean insertNodesBlock(
			RemoteHyperNode curNode, Set<RemoteHyperNode> sendSet) {
		// Create the send object array
		Object[] sendObjects = new Object[sendSet.size()];
		int counter = 0;
		Iterator<RemoteHyperNode> setIt = sendSet.iterator();
		while (setIt.hasNext()) {
			sendObjects[counter++] = setIt.next().toMap();
		}
		// Perform the actual RPC		
		try {
			XmlRpcClient client = configClient(curNode);			
			Object[] params = new Object[1];
			params[0] = sendObjects;
			
			TimingOutCallback callback = 
				new TimingOutCallback(XmlRPCClient.InsertCallbackTimeoutMS);				
			client.executeAsync("RemoteHyperInterface.addNodes", params, callback);
			Object newObj = callback.waitForResponse();
			if (newObj instanceof Boolean) {
				Boolean retBool = (Boolean) newObj;
				return retBool.booleanValue();
			} else {				
				System.out.println("Unexpected return value.");
			}			
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
		} catch (TimeoutException e) {
			System.out.println("Called to addNodes timed out.");
		} catch (XmlRpcException e) {
			System.out.println("XmlRPCException received.");
		} catch (Throwable e) {
			System.out.println("Exception received in insertNodes.");
		}						
		return false;
	}

	public static List<RemoteHyperNode> getRandomNodesBlock(RemoteHyperNode remoteNode) {
		ArrayList<RemoteHyperNode> retList = new ArrayList<RemoteHyperNode>();
		try {
			XmlRpcClient client = configClient(remoteNode);			
			TimingOutCallback callback = 
				new TimingOutCallback(XmlRPCClient.QueryCallbackTimeoutMS);				
			client.executeAsync("RemoteHyperInterface.getRandomNodes", new Object[0], callback);
			Object newObj = callback.waitForResponse();
			if (newObj instanceof Object[]) {
				Object[] objArray = (Object[]) newObj;
				for (int i = 0; i < objArray.length; i++) {
					RemoteHyperNode curInnerNode = 
						RemoteHyperNode.convert(objArray[i]);
					if (curInnerNode != null) {					
						retList.add(curInnerNode);
					} else {
						System.out.println("Unexpected return value");
						return null;
					}
				}
				return retList;
			} else {				
				System.out.println("Unexpected return value.");
			}			
		} catch (MalformedURLException e) {
			log.warn("Malformed URL");
		} catch (TimeoutException e) {
			log.warn("Called to addNodes timed out.");
		} catch (XmlRpcException e) {
			log.warn("XmlRPCException received.");
		} catch (Throwable e) {
			log.warn("Exception received in insertNodes.");
		}						
		return null;
	}

	public static void getDistBoundNodesAsync(RemoteHyperNode node,
			String keyword, int distBound, AsyncCallback callback) {
		try {
			XmlRpcClient client = configClient(node);			
			Object[] params = new Object[2];
			params[0] = keyword;
			params[1] = distBound;
			client.executeAsync(
					"RemoteHyperInterface.getDistBoundNodes", params, callback);
		} catch (MalformedURLException e) {
			log.warn("Malformed URL");
		} catch (XmlRpcException e) {
			log.warn("XmlRPCException received.");
		}
	}	
	
	public static void getDistKNodesAsync(RemoteHyperNode node,
			String keyword, int distBound, int kVal, AsyncCallback callback) {
		try {
			XmlRpcClient client = configClient(node);			
			Object[] params = new Object[3];
			params[0] = keyword;
			params[1] = distBound;
			params[2] = kVal;
			client.executeAsync(
					"RemoteHyperInterface.getDistKNodes", params, callback);
		} catch (MalformedURLException e) {
			log.warn("Malformed URL");
		} catch (XmlRpcException e) {
			log.warn("XmlRPCException received.");
		}
	}	
	
	public static List<RemoteHyperNode> getRingMembersBlock(
			RemoteHyperNode closestNode) {
		TimingOutCallback timeoutCB = XmlRPCClient.getAllPeers(closestNode);
		try {
			Object retObj = timeoutCB.waitForResponse();
			if (!(retObj instanceof Object[])) {
				return null;		// Unexpected result, skipping.
			}
			Object[] nodeArray = (Object[]) retObj;
			List<RemoteHyperNode> nodeList = new ArrayList<RemoteHyperNode>();			
			for (int i = 0; i < nodeArray.length; i++) {
				RemoteHyperNode curNode = RemoteHyperNode.convert(nodeArray[i]);
				if (curNode == null) {
					return null;	// Not marshalled correctly, skipping.
				}
				nodeList.add(curNode);
			}
			return nodeList;		// Return node list results
		} catch (TimeoutException e) {			
		} catch (Throwable e) {}
		return null;
	}
	
	
	public static List<String> findTopKMoviesConsumer(
			EntryContainer entryContainer, 
			Set<RemoteHyperNode> candidateNodes, 
			int inK, String searchStr, 
			int charPerPerturb, int inTimeout,
			Consumer<MovieEntry> inConsumer) {
		String[] keysArray = entryContainer.removeSymbols(searchStr);
		List<String> keysList = new ArrayList<String>();
		for (int i = 0; i < keysArray.length; i++) {
			keysList.add(keysArray[i]);
		}
		List<String> normalizeKeys = 
			entryContainer.normalizeKeywords(keysList);
		
		GetKClosestObjectsThread getMovie = 
			new GetKClosestObjectsThread(
				candidateNodes, new HashSet<RemoteHyperNode>(),
				inK, normalizeKeys, charPerPerturb, 
				MaxThreadsPerCall, inTimeout);
		getMovie.attachCB(inConsumer);
		if (getMovie.issueProducer()) {
			return normalizeKeys;
		}
		return null;
	}	
		
	public static List<MovieEntry> findTopKMovies(
			EntryContainer entryContainer, 
			Set<RemoteHyperNode> candidateNodes, 
			int inK, String searchStr, 
			int charPerPerturb, int inTimeout) {
		String[] keysArray = entryContainer.removeSymbols(searchStr);
		List<String> keysList = new ArrayList<String>();
		for (int i = 0; i < keysArray.length; i++) {
			keysList.add(keysArray[i]);
		}
		List<String> normalizeKeys = 
			entryContainer.normalizeKeywords(keysList);
		
		GetKClosestObjects getMovie = 
			new GetKClosestObjects(
					entryContainer, candidateNodes,
					new HashSet<RemoteHyperNode>(),
					inK, normalizeKeys, charPerPerturb, 
					MaxThreadsPerCall, inTimeout);
		getMovie.startGetBoundNodes();
		return getMovie.getConsumeResult();
	}	
}
