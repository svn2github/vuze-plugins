/**
 * Created on Mar 11, 2008
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

package com.azureus.plugins.aznetmon.pcap.stats;

import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.azureus.plugins.aznetmon.pcap.IpPortPair;
import com.azureus.plugins.aznetmon.pcap.util.NetMon16Util;

public class ConnectStatsMonitor {
	private static ConnectStatsMonitor ourInstance = new ConnectStatsMonitor();

	Map<IpPortPair,ConnectStats> activeConnStats = new ConcurrentHashMap<IpPortPair,ConnectStats>();
	List<ConnectStats> archiveConnStats = new ArrayList<ConnectStats>();


	public static ConnectStatsMonitor getInstance() {
		return ourInstance;
	}

	private ConnectStatsMonitor() {
	}



	public synchronized boolean hasConnection(IpPortPair key){

		return activeConnStats.get(key)!=null;

	}

	/**
	 * Add a new acitve connection stat.
	 * @param key -
	 * @param cStat -
	 */
	public synchronized void addConnection(IpPortPair key, ConnectStats cStat){

		activeConnStats.put(key,cStat);

	}

	/**
	 * Call to update an active connection.
	 * @param key -
	 * @param packet -
	 */
	public synchronized void updateConnectStats(IpPortPair key, Packet packet){

		ConnectStats cs = activeConnStats.get(key);
		cs.addByteTransferred( packet.len );
		activeConnStats.put(key,cs);

	}


	/**
	 * Call when a close connection packet is detected.
	 * @param key -
	 * @param packet -
	 */
	public synchronized void setCloseStats(IpPortPair key, TCPPacket packet){

		ConnectStats cs = activeConnStats.get(key);

		ConnectStats.ConnectEndType endType;
		if(packet.rst){
			endType = ConnectStats.ConnectEndType.RST;
		}else{
			endType = ConnectStats.ConnectEndType.FIN;
		}

		cs.finPacket(endType,packet);
	}

	/**
	 * Move items from the activeConnStats area to the archiveConnStats
	 * area if they haven't been updated in a while.
	 */
	public synchronized void moveToArchive(){

		Set<IpPortPair> allKeys = activeConnStats.keySet();

		for( IpPortPair key : allKeys ){

			ConnectStats cs = activeConnStats.get(key);
			activeConnStats.remove(key);
			archiveConnStats.add(cs);

			NetMon16Util.debug("archiving: "+key);

		}

	}//moveToArchive


	public synchronized ConnectStatsSummary createArchiveSummary(){

		ConnectStatsSummary summary = new ConnectStatsSummary();

		for( ConnectStats cs: archiveConnStats ){

			ConnectStats.ConnectEndType endType = cs.getEndType();

			if( endType == ConnectStats.ConnectEndType.RST ){
				summary.numRstCloses++;
				summary.resetIpAddr.add( cs );
			}else if( endType == ConnectStats.ConnectEndType.FIN ){
				summary.numFinCloses++;
			}

			IpPortPair key = cs.getKey();
			summary.uniqueIps.add( key.lowAddr() );
			summary.uniqueIps.add( key.highAddr() );

			summary.uniquePorts.add( key.lowPort() );
			summary.uniquePorts.add( key.highPort() );


			//ToDo: want the same thing on this side.
			summary.addConnectionList( key ); //Need the same thing on the Peer side!!!

		}//for

		archiveConnStats = new ArrayList<ConnectStats>();

		return summary;
	}//createArchiveSummary

}
