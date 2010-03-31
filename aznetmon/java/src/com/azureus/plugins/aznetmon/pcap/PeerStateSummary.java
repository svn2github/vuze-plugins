package com.azureus.plugins.aznetmon.pcap;

import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsSummary;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

/**
 * Created on Apr 4, 2008
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class PeerStateSummary
{

	final List<PeerInfo> archive;

	public PeerStateSummary(List<PeerInfo> _archive){
		archive = _archive;
	}


	public Set<IpPortPair> getAllCommonPeers(){

		Set<IpPortPair> retVal = new HashSet<IpPortPair>();
		retVal.addAll( getIpPortPair(archive) );

		return retVal;
	}


	public Set<IpPortPair> getOnlyResetPeers(ConnectStatsSummary css){

		Set<IpPortPair> rstConnections = css.getResetConnections();
		Set<IpPortPair> retVal = getAllCommonPeers();
		retVal.retainAll(rstConnections);

		return retVal;
	}


	/**
	 * Get the peers that have (a) non-zero values sequence numbers (b) more than 300 bytes transferred.
	 * @param css -
	 * @return -
	 */
	public Set<IpPortPair> getMultiResetPeers(ConnectStatsSummary css){

		Set<IpPortPair> multiRstConnections = css.getMultiResetConnections();
		Set<IpPortPair> retVal = getAllCommonPeers();
		retVal.retainAll(multiRstConnections);

		return retVal;
	}


	private Set<IpPortPair> getIpPortPair(List<PeerInfo> peerInfoList){

		Set<IpPortPair> retVal = new HashSet<IpPortPair>();
		for(PeerInfo pi : peerInfoList){
			IpPortPair key = pi.getIpPortPair();
			retVal.add(key);
		}
		return retVal;
	}//getIpPortPair

}
