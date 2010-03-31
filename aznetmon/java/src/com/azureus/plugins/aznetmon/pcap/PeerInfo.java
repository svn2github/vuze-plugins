package com.azureus.plugins.aznetmon.pcap;

import org.gudy.azureus2.plugins.peers.Peer;

/**
 * Created on Apr 23, 2008
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

class PeerInfo implements Comparable
{
	String ip;
	int port;
	IpPortPair ipp;

	long startTime;
	long closeTime;

	public PeerInfo(Peer peer){

		ip = peer.getIp();
		port = peer.getPort();
		ipp = new IpPortPair( new IpPort(peer.getIp(),peer.getPort()), new IpPort() );
		startTime = System.currentTimeMillis();
	}

	public void close(){
		closeTime = System.currentTimeMillis();
	}

	public IpPortPair getIpPortPair(){
		return ipp;
	}

	public boolean equals(Object o){
		if( !(o instanceof PeerInfo) ){
			return false;
		}

		PeerInfo that = (PeerInfo) o;

		return ( this.ip.equals(that) && this.port==that.port && this.startTime==that.startTime );
	}

	public int hashCode(){
		return 31*ip.hashCode()+(101*port)+ (int)startTime;
	}


	public int compareTo(Object o){

		if( !(o instanceof PeerInfo) ){
			throw new ClassCastException("compareTo needs an IpPortPair class");
		}

		PeerInfo that = (PeerInfo) o;

		if( this.ipp.compareTo(that.ipp) !=0 ){
			return this.compareTo(that);
		}

		if( this.startTime!=that.startTime ){
			return (int) (this.startTime-that.startTime);
		}

		return 0;
	}//compareTo

}//class
