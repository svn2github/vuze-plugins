package com.azureus.plugins.aznetmon.pcap;

/**
 * Created on Apr 18, 2008
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

public class IpPort implements Comparable
{
	public static final String LOCAL_IP_CONSTANT = "1.1.1.1";
	public static final int LOCAL_PORT_CONSTANT = 1;

	String ip;

	int[] ipPart = new int[4];
	int port;

	/**
	 * Use to set value to local interface value.
	 */
	public IpPort(){
		this(LOCAL_IP_CONSTANT,LOCAL_PORT_CONSTANT);
	}

	public IpPort(String _ip, int _port){

		//clean the ip String of '/' characters at the beginning.
		_ip = _ip.replace('/',' ').trim();

		ip = _ip;
		port = _port;

		ipPart = parseIpString(_ip);
	}

	private int[] parseIpString(String ip)
		throws IllegalArgumentException
	{
		String[] part = ip.split("\\.");

		if( part.length!= 4){
			throw new IllegalArgumentException("Invalid IP - "+ip);
		}

		for(int i=0; i<part.length; i++){
			ipPart[i] = Integer.parseInt(part[i]);
		}

		return ipPart;
	}//parseIpString



	public int compareTo(Object o) {

		if( !(o instanceof IpPort) ){
			throw new ClassCastException("compareTo needs an IpPort class");
		}

		IpPort that = (IpPort) o;

		for(int i=0; i<4; i++){
			int diff = this.ipPart[i]-that.ipPart[i];
			if( (diff!=0) ){
				return diff;
			}
		}

		return this.port - that.port;
	}

	public String toString(){

		if( ip.equals(LOCAL_IP_CONSTANT) && port==LOCAL_PORT_CONSTANT ){
			return "LOCAL";
		}

		return ip+":"+port;
	}

}
