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

import java.util.HashMap;
import java.util.Map;

public class RemoteHyperImpl implements RemoteHyperInterface {
	private HyperNode hyperNode = null;

	public void init(HyperNode hyperNode) {
		this.hyperNode = hyperNode;
	}
	
	public String echo(String inString) {
		return inString;
	}
	
	public Object[] getAllPeers() {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getAllPeers();
		}
		return new Object[0];
	}
	
	public Object[] getRandomNodes() {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getRandomNodes();
		}
		return new Object[0];
	}
	
	public Object[] getRandomWords() {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getRandomWords();
		}
		return new Object[0];
	}
	
	public Object[] getKClosestNodes(int val, String keyword) {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getKClosestNodes(val, keyword);
		}
		return new Object[0];
	}
	
	public Object[] getKClosestEntries(int val, Object[] keywords) {
		if (hyperNode.nodeStarted()) { 
			return hyperNode.getKClosestEntries(val, keywords);
		}
		return new Object[0];
	}
		
	public Boolean insertKeyEntry(String key, Object[] entry) {
		if (hyperNode.nodeStarted()) {
			return hyperNode.insertKeyEntry(key, entry);
		}
		return false;
	}

	public Boolean addNodes(Object[] inNodes) {
		if (hyperNode.nodeStarted()) {
			return hyperNode.addNodes(inNodes);
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public Map getNode() {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getNode();
		}
		return new HashMap();
	}
	
	public Object[] getDistBoundNodes(String keyword, int distBound) {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getDistBoundNodes(keyword, distBound);
		}
		return new Object[0];
	}
	
	public Object[] getDistKNodes(String keyword, int distBound, int kVal) {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getDistKNodes(keyword, distBound, kVal);
		}
		return new Object[0];
	}
	
	public Object[] getCloserKeys(String keyword) {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getCloserKeys(keyword);
		}
		return new Object[0];
	}
	
	@SuppressWarnings("unchecked")
	public Map getEntries(String keyword) {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getEntries(keyword);
		}
		return new HashMap();
	}
	
	public Object[] getRingInfo() {
		if (hyperNode.nodeStarted()) {
			return hyperNode.getRingInfo();
		}
		return new Object[0];
	}
}
