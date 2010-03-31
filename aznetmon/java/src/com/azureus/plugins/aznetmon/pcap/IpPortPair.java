package com.azureus.plugins.aznetmon.pcap;

/**
 * Created on Apr 21, 2008
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

public class IpPortPair implements Comparable
{

	IpPort[] conn;


	public IpPortPair(IpPort ip1, IpPort ip2){

		conn = new IpPort[2];

		if( ip1.compareTo(ip2)>0 ){
			conn[0] = ip1;
			conn[1] = ip2;
		}else{
			conn[0] = ip2;
			conn[1] = ip1;
		}

	}

	public IpPortPair(String addr1, int port1, String addr2, int port2)
	{
		this(new IpPort(addr1,port1),new IpPort(addr2,port2));
	}


	public String lowAddr(){
		return conn[0].ip;
	}

	public String highAddr(){
		return conn[1].ip;
	}

	public int lowPort(){
		return conn[0].port;
	}

	public int highPort(){
		return conn[1].port;
	}

	public int compareTo(Object o){

		if( !(o instanceof IpPortPair) ){
			throw new ClassCastException("compareTo needs an IpPortPair class");
		}

		IpPortPair that = (IpPortPair) o;

		if( this.conn[0].compareTo(that.conn[0]) != 0 ){
			return this.conn[0].compareTo(that.conn[0]);
		}

		if( this.conn[1].compareTo(that.conn[1]) != 0 ){
			return this.conn[1].compareTo(that.conn[1]);
		}

		return 0;
	}

	public boolean equals(Object o){
		if( !(o instanceof IpPortPair) ){
			return false;
		}

		IpPortPair that = (IpPortPair) o;

		return ( this.conn[0].port == that.conn[0].port &&
				 this.conn[1].port == that.conn[1].port &&
				 this.conn[0].ip.equals(that.conn[0].ip) &&
				 this.conn[1].ip.equals(that.conn[1].ip) );
	}


	public int hashCode(){
		return 	  3*conn[0].ip.hashCode()
				+ 5*conn[1].ip.hashCode()
				+ 7*conn[0].port
				+ 11*conn[1].port;
	}

	public String toString(){
		return conn[0]+"<-->"+conn[1];
	}

	/**
	 * Print just the remote IP address.
	 * @return -
	 */
	public String remoteIp(){

		if( conn[0].port==IpPort.LOCAL_PORT_CONSTANT ){
			return conn[1].toString();
		}

		return conn[0].toString();
	}

}
