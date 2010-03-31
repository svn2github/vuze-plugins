/**
 * Created on Apr 23, 2008
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

import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;

public class LocalIpList
{

	private static LocalIpList ourInstance = new LocalIpList();

	private Set<InetAddress> ipSet = new HashSet<InetAddress>();

	public static LocalIpList getInstance() {
		return ourInstance;
	}

	private LocalIpList() {
	}

	public void add(InetAddress addr){
		ipSet.add(addr);
	}//add


	public boolean isLocalIp(InetAddress addr){

		return ipSet.contains(addr);

	}

}
