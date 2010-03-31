package com.azureus.plugins.aznetmon.pcap.stats;

import com.azureus.plugins.aznetmon.pcap.IpPortPair;
import com.azureus.plugins.aznetmon.pcap.LocalIpList;
import com.azureus.plugins.aznetmon.pcap.IpPort;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.net.InetAddress;

import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

/**
 * Created on Mar 12, 2008
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

public class ConnectStatsSummary
{
	
	public long intervalStartTime;
	public long intervalLength;

	public int numFinCloses=0;
	public int numRstCloses=0;

	public List<ConnectStats> resetIpAddr = new ArrayList<ConnectStats>();

	public List<IpPortPair> ipPortPair = new ArrayList<IpPortPair>();//maybe this should be a RST.

	public Set<String> uniqueIps = new HashSet<String>();
	public Set<Integer> uniquePorts = new HashSet<Integer>();

	
	public ConnectStatsSummary(){

	}

	public float percentResetCloses(){
		
		if( numFinCloses+numRstCloses==0 ){
			return 0.0f;
		}

		return ( (float)numRstCloses/(numFinCloses+numRstCloses) );

	}


	public Set<IpPortPair> getResetConnections(){

		return getIpPortPair(resetIpAddr);

	}

	private static Set<IpPortPair> getIpPortPair( List<ConnectStats> resetIpAddrList ){

		Set<IpPortPair> retVal = new HashSet<IpPortPair>();
		for(ConnectStats cs : resetIpAddrList){
			//Does the ConnectStats have the IpPortPair?
			retVal.add( cs.getIpPortPair() );
		}
		return retVal;
	}


	/**
	 * The set of all connections that have (a) non-zero sequence numbers (b) more than 300 bytes transferred.
	 * @return -
	 */
	public Set<IpPortPair> getMultiResetConnections(){

		Set<IpPortPair> retVal = new HashSet<IpPortPair>();
		for(ConnectStats cs : resetIpAddr){

			if( cs.bytesTransferred<300 ){
				continue;
			}

			LocalIpList local = LocalIpList.getInstance();

			for( Packet p : cs.finPackets ){

				//asdf- find how to get the sequence number.
				if( p instanceof TCPPacket){

					TCPPacket tcpPkt = (TCPPacket) p;
					if( tcpPkt.rst ){
						if( tcpPkt.sequence>0 ){

							//adsfsd - add the IpPortPair to retVal
							InetAddress destIp = tcpPkt.dst_ip;
							int destPort = tcpPkt.dst_port;
							InetAddress srcIp = tcpPkt.src_ip;
							int srcPort = tcpPkt.src_port;

							IpPortPair ipp;
							if( local.isLocalIp(destIp) ){
								ipp = new IpPortPair( new IpPort(srcIp.getHostAddress(),srcPort),new IpPort() );
							}else if( local.isLocalIp(srcIp) ){
								ipp = new IpPortPair( new IpPort(destIp.getHostAddress(),destPort),new IpPort() );
							}else{
								ipp = new IpPortPair( new IpPort(srcIp.getHostAddress(),srcPort),
													  new IpPort(destIp.getHostAddress(),destPort));
							}

							retVal.add(ipp);
							break;
						}
					}
				}
			}//for

		}//for

		return retVal;
	}//getMultiResetConnections


	public void addConnectionList( IpPortPair ipp ){
		ipPortPair.add(ipp);
	}

	public String getLog(){

		StringBuffer sb = new StringBuffer();

		float percReset = percentResetCloses();

		sb.append(" %RST closes:       ").append(percReset);
		sb.append(" #RST               ").append(this.numRstCloses);
		sb.append(" #FIN               ").append(this.numFinCloses);

		sb.append(" #unique ips =      ").append(this.uniqueIps.size());
		sb.append(" #unique src ports= ").append(this.uniquePorts.size()).append("\n\n");

		int size = this.resetIpAddr.size();
		for( int i=0;i<size;i++ ){

			ConnectStats cs = this.resetIpAddr.get(i);
			sb.append( cs.printConnectionResultWithFinPackets() );
		}

		return sb.toString();
	}//getLog
	
}
