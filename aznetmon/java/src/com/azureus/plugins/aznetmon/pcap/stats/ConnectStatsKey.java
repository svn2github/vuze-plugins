package com.azureus.plugins.aznetmon.pcap.stats;

import java.net.InetAddress;

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

public class ConnectStatsKey
{
	final InetAddress srcAddr;
	final int srcPort;
	final InetAddress destAddr;
	final int destPort;

	public ConnectStatsKey(InetAddress _srcAddr, int _srcPort, InetAddress _destAddr, int _destPort){
		srcAddr = _srcAddr;
		srcPort = _srcPort;
		destAddr = _destAddr;
		destPort = _destPort;
	}

	public String getSourceIP(){
		return srcAddr.getHostAddress();
	}

	public String getKey(){
		return srcAddr.getHostAddress()+":"+srcPort+" -> "+destAddr.getHostAddress()+":"+destPort;
	}

	public String toString(){
		return getKey();
	}

	/**
	 * Overrides equals method.
	 * @param thatObj -
	 * @return -
	 */
	public boolean equals(Object thatObj){

		if( this == thatObj){
			return true;
		}

		if( !(thatObj instanceof ConnectStatsKey) ){
			return false;
		}

		ConnectStatsKey that = (ConnectStatsKey) thatObj;

		//check the ports first.
		if( this.srcPort  != that.srcPort ||
			this.destPort != that.destPort )
		{
			return false;
		}

		//check the IP address second.
		String thisSrcAddr = this.srcAddr.getHostAddress();
		String thatSrcAddr = that.srcAddr.getHostAddress();
		String thisDestAddr = this.destAddr.getHostAddress();
		String thatDestAddr = that.destAddr.getHostAddress();

		if( !( thisSrcAddr.equalsIgnoreCase( thatSrcAddr )) ||
			!(thisDestAddr.equalsIgnoreCase( thatDestAddr )) )
		{
			return false;
		}

		return true;
	}//equals

	/**
	 * Override hashCode.
	 * @return -
	 */
	public int hashCode()
	{
		int hash = 7;
		hash = (31*hash) + srcPort;
		hash = (37*hash) + destPort;
		hash = (41*hash) + srcAddr.hashCode();
		hash = (43*hash) + destAddr.hashCode();

		return hash;
	}//hashCode

}
