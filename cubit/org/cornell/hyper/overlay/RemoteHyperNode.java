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

public class RemoteHyperNode {
	final private String 	IP;
	final private int		port;
	final private String	nodeID;
	final private String	key;
		
	public RemoteHyperNode(String IP, int port, String nodeID, String key) {
		this.IP 	= IP;
		this.port 	= port;
		this.nodeID	= nodeID;
		this.key	= key;	
	}	

	public String getIP() 		{ return IP; }
	public int getPort() 		{ return port; }
	public String getNodeID() 	{ return nodeID; }
	public String getKey() 		{ return key; }
	
	public boolean equals(Object o){
		if (!(o instanceof RemoteHyperNode)) {
			return false;		
		}
		RemoteHyperNode otherNode = (RemoteHyperNode)o;		
		if (getIP().equals(otherNode.getIP()) 			&&
			getPort() == otherNode.getPort() 			&&
			getNodeID().equals(otherNode.getNodeID())	&&
			getKey().equals(otherNode.getKey())) {
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		return getIP().hashCode() ^ 
			getPort() ^ getNodeID().hashCode() ^ getKey().hashCode();
	}
	
	/*
	public boolean valid(){
		if (getNodeID() == null || getKey() == null) {
			return false;		
		}
		return true;		
	}
	*/
	
	public Map<String, Object> toMap() {
		HashMap<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("IP", getIP());
		retMap.put("port", getPort());
		retMap.put("nodeID", getNodeID());
		retMap.put("key", getKey());
		return retMap;
	}
	
	public AddressPort toAddressPort() {
		return new AddressPort(getIP(), getPort());
	}

	@SuppressWarnings("unchecked")
	public static RemoteHyperNode convert(Object obj) {
		// Check that it is indeed a map.
		if (!(obj instanceof Map)) {
			return null;			
		}		
		// Cast it to a Map object		
		Map<String, Object> mapObj = (Map<String, Object>) obj;		
		if (!mapObj.containsKey("IP") 		||
			!mapObj.containsKey("port") 	||
			!mapObj.containsKey("nodeID") 	||
			!mapObj.containsKey("key")) {
			return null;
		}
		String IP 		= (String) mapObj.get("IP");
		int port 		= ((Integer) mapObj.get("port")).intValue();
		String nodeID	= (String) mapObj.get("nodeID");
		String key		= (String) mapObj.get("key");
				
		return new RemoteHyperNode(IP, port, nodeID, key);
	}
}
