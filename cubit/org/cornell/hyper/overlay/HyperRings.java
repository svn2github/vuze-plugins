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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

// NOTE: All public methods need to be synchronized
public class HyperRings {
	protected class InnerRing {
		private int 						nodesPerRing;
		private ArrayList<RemoteHyperNode>	ringMembers;
		
		protected InnerRing(int nodesPerRing) {
			this.nodesPerRing 	= nodesPerRing;
			ringMembers			= new ArrayList<RemoteHyperNode>();
		}
		
		protected boolean addMember(RemoteHyperNode hyperNode) {
			if (ringMembers.size() >= nodesPerRing) {
				return false;	// Ring is full
			}
			ringMembers.add(hyperNode);
			return true;
		}
		
		protected boolean removeMember(RemoteHyperNode hyperNode) {
			for (int i = 0; i < ringMembers.size(); i++) {
				if (hyperNode.equals(ringMembers.get(i))) {
					ringMembers.remove(i);
					return true;
				}				
			}
			return false;
		}
		
		protected RemoteHyperNode removeOldest() {
			if (ringMembers.size() == 0) {
				return null;				
			}
			RemoteHyperNode hyperNode = ringMembers.get(0);
			ringMembers.remove(0);
			return hyperNode;
		}
		
		protected boolean isMember(RemoteHyperNode hyperNode) {
			for (int i = 0; i < ringMembers.size(); i++) {
				if (hyperNode.equals(ringMembers.get(i))) {			
					return true;
				}				
			}			
			return false;			
		}
		
		protected boolean isFull() {
			if (numMembers() >= nodesPerRing) {
				return true;			
			}
			return false;		
		}
		
		protected int numMembers() {
			return ringMembers.size();
		}
		
		@SuppressWarnings("unchecked")
		protected List<RemoteHyperNode> getRingMembers() {
			return (List<RemoteHyperNode>) ringMembers.clone();		
		}

		protected RemoteHyperNode getRingMember(int index) {
			if (index >= ringMembers.size()) {
				return null;
			}
			return ringMembers.get(index);			
		}
	}
	
	private int 						nodesPerRing;
	private double 						ringLogBase;
	private double 						maxDist;
	private String 						ringKey;
	private int 						numRings;
	private HashSet<RemoteHyperNode>	allNodesSet;
	private ArrayList<InnerRing>		priRings;
	private ArrayList<InnerRing>		secRings;
	private Random						randVal;
	
	public HyperRings(int nodesPerRing, double ringLogBase, 
			double maxDist, String ringKey) {
		this.nodesPerRing 	= nodesPerRing;
		this.ringLogBase	= ringLogBase;
		this.maxDist		= maxDist;
		this.ringKey		= ringKey;
		numRings			= calcRingNum(this.maxDist) + 1;
		allNodesSet			= new HashSet<RemoteHyperNode>();
		priRings			= new ArrayList<InnerRing>();
		secRings			= new ArrayList<InnerRing>();
		randVal				= new Random();
		
		if (this.nodesPerRing <= 0) {
			System.out.println("Nodes per ring cannot be <= 0, setting to 1");
			this.nodesPerRing = 1;					
		}
		
		for (int i = 0; i < numRings; i++) {
			priRings.add(new InnerRing(nodesPerRing));
			secRings.add(new InnerRing(nodesPerRing));			
		}		
	}
	
	public synchronized int getNumRings() {
		return numRings;
	}
	
	private int calcRingNum(double dist) {
		if (dist <= 0.0) return 0;	// 0 distance will below to ring 0		
		double rawRingNum = 
			Math.log(Math.min(dist, maxDist)) / Math.log(ringLogBase);
		return (int)Math.ceil(rawRingNum);
	}
	
	public synchronized String getKey() {
		return ringKey;
	}
	
	public synchronized List<RemoteHyperNode> getRandomNodes() {
		ArrayList<RemoteHyperNode> retList = new ArrayList<RemoteHyperNode>();
		for (int i = 0; i < numRings; i++) {
			InnerRing curRing = priRings.get(i);
			if (curRing.numMembers() > 0) {
				int randIndex = randVal.nextInt(curRing.numMembers());
				RemoteHyperNode randNode = curRing.getRingMember(randIndex);
				assert(randNode != null);
				retList.add(randNode);
			}
		}
		return retList;
	}
	
	public RemoteHyperNode getClosestNodeExcept(
			String reqKey, Set<RemoteHyperNode> exceptNodeSet) {
		int lowestDist 				= -1;
		RemoteHyperNode closestNode	= null;
		
		Iterator<RemoteHyperNode> setIt = allNodesSet.iterator();
		while (setIt.hasNext()) {
			RemoteHyperNode curNode = setIt.next();
			if (exceptNodeSet.contains(curNode)) {
				continue;	// Skipping nodes in the except set		
			}
			int curDist = EditDistance.computeEditDistance(
					ringKey, curNode.getKey());
			
			if (lowestDist == -1 || curDist < lowestDist) {
				lowestDist = curDist;
				closestNode = curNode;
			}			
		}
		return closestNode;
	}
	
	public synchronized RemoteHyperNode getClosestNode(String reqKey) {
		return getClosestNodeExcept(reqKey, null);
	}
	
	public synchronized List<RemoteHyperNode> getKClosestNodes(
			int k, String reqKey) {
		System.out.println("HyperRing.getKClosestNodes: " + k + ", " + reqKey);
		ArrayList<RemoteHyperNode> retList = new ArrayList<RemoteHyperNode>();
		HashSet<RemoteHyperNode> exceptSet = new HashSet<RemoteHyperNode>();
		for (int i = 0; i < k; i++) {
			RemoteHyperNode closestNode = 
				getClosestNodeExcept(reqKey, exceptSet);
			if (closestNode == null) {
				break;	// Done, no more unique nodes available
			}
			retList.add(closestNode);
			exceptSet.add(closestNode);	// Add to checked set1
		}
		return retList;
	}
	
	public synchronized void addMember(RemoteHyperNode hyperNode) {
		// Check if it is already a a member.
		if (allNodesSet.contains(hyperNode)) {
			return;	// Done, already a member
		}
		
		// Add it to the all node set		
		allNodesSet.add(hyperNode);
		
		// Compute the edit distance and see which ring this node belongs in. 
		int dist = EditDistance.computeEditDistance(ringKey, hyperNode.getKey());
		int ringNum = calcRingNum(dist);
		
		System.out.println("Dist is " + dist + " ring num is " + ringNum);
		
		System.out.println("Adding to primary");
		// Try to add it to the primary ring first
		InnerRing curRing = priRings.get(ringNum);		
		if (curRing.addMember(hyperNode)) {
			return;	// Done, added to the primary ring
		}
		
		System.out.println("Adding to secondary");
		
		// If full, try the secondary ring.
		curRing = secRings.get(ringNum);
		if (curRing.addMember(hyperNode)) {
			return;	// Done, added to the secondary ring
		}
		
		// Secondary ring is full as well, remove the oldest member.
		RemoteHyperNode oldestNode = curRing.removeOldest();
		if (oldestNode != null) {
			allNodesSet.remove(oldestNode);
			curRing.addMember(hyperNode);
		} else {
			// Couldn't add it for unknown reasons, remove it from the set.
			// This might be because the secondary ring size is set to 0.
			allNodesSet.remove(hyperNode);
			System.out.println("Consistency error in addMember.");
		}
	}
	
	public synchronized void removeMember(RemoteHyperNode hyperNode) {
		// Check if the node is in there
		if (!allNodesSet.contains(hyperNode)) {
			return;	// Done, not a member
		}
		
		// Remove it from the nodes set
		allNodesSet.remove(hyperNode);
		
		// Compute the edit distance and see which ring this node belongs in. 
		int dist = EditDistance.computeEditDistance(ringKey, hyperNode.getKey());
		int ringNum = calcRingNum(dist);
		
		// See if it is in the primary ring.
		InnerRing curRing = priRings.get(ringNum);		
		if (curRing.removeMember(hyperNode)) {			
			return; // Done
		}

		// Now check the secondary ring.
		curRing = secRings.get(ringNum);
		if (curRing.removeMember(hyperNode)) {
			return; // Done
		}
		
		System.out.println("Consistency error in removeMember.");
	}
	
	// NOTE: Returns -1 if the nodeList is empty.
	private int findSmallestIndex(List<RemoteHyperNode> nodeList) {
		int minDist  = -1;
		int minIndex = -1;
		for (int i = 0; i < nodeList.size(); i++) {			
			int curTotalDist = 0;
			String outerKey = nodeList.get(i).getKey();
			
			// Compute the sum of the distance from node i to every other node
			for (int j = 0; j < nodeList.size(); j++) {
				if (i == j) continue;	// Skip itself
				curTotalDist = curTotalDist + EditDistance.computeEditDistance(
						outerKey, nodeList.get(j).getKey());
			}
			
			// See if it is the minimum.
			if (minDist == -1 || curTotalDist < minDist) {
				minDist = curTotalDist;
				minIndex = i;
			}			
		}
		return minIndex;
	}
	
	public synchronized void performRingManagement(int ringNum) {
		if (ringNum < 0 || ringNum >= getNumRings()) {
			System.out.println("Cannot perform ring management on ring "
					+ ringNum);
			return;
		}
				
		// Add all ring members from the primary and secondary rings
		// into a single node list
		ArrayList<RemoteHyperNode> combineList = new ArrayList<RemoteHyperNode>();
		InnerRing curPriRing = priRings.get(ringNum);
		InnerRing curSecRing = secRings.get(ringNum);
		combineList.addAll(curPriRing.getRingMembers());
		combineList.addAll(curSecRing.getRingMembers());
		
		// If the combineList is 0, or the nodesPerRing is <= 0, just skip.
		if (combineList.size() == 0 || nodesPerRing <= 0) {
			return; // Nothing to do for this ring
		}
		
		// Remove all primary nodes from the allNodesSet
		// BUG: Should not remove it from allNodesSet
		while (curPriRing.numMembers() > 0) {
			//allNodesSet.remove(curPriRing.removeOldest());			
			curPriRing.removeOldest();
		}
		
		// Remove all secondary nodes from the allNodesSet
		// BUG: Should not remove it from allNodesSet
		while (curSecRing.numMembers() > 0) {
			//allNodesSet.remove(curSecRing.removeOldest());				
			curSecRing.removeOldest();
		}
		
		// At each iteration, remove the least useful node from the 
		// combined list and put that into the secondary ring. When 
		// the combine list is equal to the maximum number of nodes 
		// per ring, set the primary list as that of the remaining 
		// members in the combined list.
		//
		// NOTE: The ordering of the secondary nodes are not kept.
		while (combineList.size() > nodesPerRing) {
			int curIndex = findSmallestIndex(combineList);
			curSecRing.addMember(combineList.get(curIndex));
			combineList.remove(curIndex);
		}
		
		// Add the rest as the primary ring members
		for (int j = 0; j < combineList.size(); j++) {
			curPriRing.addMember(combineList.get(j));			
		}		
	}
	
	public synchronized List<RemoteHyperNode> getAllPrimaryPeers() {
		List<RemoteHyperNode> allNodes = new ArrayList<RemoteHyperNode>();
		for (int i = 0; i < numRings; i++) {
			allNodes.addAll(priRings.get(i).getRingMembers());		
		}
		return allNodes;
	}
	
	public synchronized List<RemoteHyperNode> getRingPeers(int index) {
		if (index < numRings) {
			return priRings.get(index).getRingMembers();
		}
		return new ArrayList<RemoteHyperNode>(); 
	}
	
	public synchronized boolean isMember(RemoteHyperNode hyperNode) {
		return allNodesSet.contains(hyperNode);
	}
	
	public synchronized boolean isPrimary(RemoteHyperNode hyperNode) {
		// Compute the edit distance and see which ring this node belongs in. 
		int dist = EditDistance.computeEditDistance(ringKey, hyperNode.getKey());
		int ringNum = calcRingNum(dist);
		
		return priRings.get(ringNum).isMember(hyperNode);		
	}

	public synchronized Set<RemoteHyperNode> getDistNodes(
			String keyword, int distBound) {
		Set<RemoteHyperNode> retSet = new HashSet<RemoteHyperNode>();
		Iterator<RemoteHyperNode> nodeIt = allNodesSet.iterator();
		while (nodeIt.hasNext()) {
			RemoteHyperNode curNode = nodeIt.next();
			int dist = EditDistance.computeEditDistance(keyword, curNode.getKey());
			if (dist <= distBound) {
				retSet.add(curNode);
			}
		}
		return retSet;
	}
}
