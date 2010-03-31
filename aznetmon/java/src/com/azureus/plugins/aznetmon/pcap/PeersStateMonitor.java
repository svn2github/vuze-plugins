/**
 * Created on Mar 14, 2008
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.azureus.plugins.aznetmon.pcap;

import org.gudy.azureus2.plugins.peers.Peer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.azureus.plugins.aznetmon.pcap.util.NetMon16Util;


/**
 * Get the state of all the known peers.
 */
public class PeersStateMonitor {
	
	private static PeersStateMonitor ourInstance = new PeersStateMonitor();

	private Map<IpPortPair,PeerInfo> connections = new ConcurrentHashMap<IpPortPair,PeerInfo>();

	private List<PeerInfo> archive = new ArrayList<PeerInfo>();


	public static PeersStateMonitor getInstance() {
		return ourInstance;
	}

	private PeersStateMonitor()
	{

	}


	public void peerAddEvent(Peer peer){
		IpPortPair key = makeKey(peer);
		PeerInfo newConnect = new PeerInfo(peer);
		connections.put(key,newConnect);

		NetMon16Util.debug("add   : "+key);

	}//peerAddEvent


	public void peerCloseEvent(Peer peer){
		IpPortPair key = makeKey(peer);
		PeerInfo c = connections.remove(key);

		if( c!=null){
			c.close();
			archive.add(c);
			NetMon16Util.debug("remove: "+key);
		}else{
			NetMon16Util.debug("remove: "+key+" (not found)");
		}

	}//peerCloseEvent


	private IpPortPair makeKey(Peer peer){

		IpPort local = new IpPort();
		IpPort peerIp = new IpPort(peer.getIp(),peer.getPort());

		return new IpPortPair(local,peerIp);
	}

	public PeerStateSummary createArchiveSummary()
	{
		PeerStateSummary retVal = new PeerStateSummary(archive);
		archive = new ArrayList<PeerInfo>();

		return retVal;
	}

}//class
