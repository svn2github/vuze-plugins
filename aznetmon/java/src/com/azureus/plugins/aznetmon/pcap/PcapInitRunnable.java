package com.azureus.plugins.aznetmon.pcap;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import org.gudy.azureus2.plugins.PluginInterface;

import java.util.Set;
import java.util.HashSet;
import java.net.InetAddress;


/**
 * Created on Mar 13, 2008
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

public class PcapInitRunnable implements Runnable
{

	final PluginInterface pluginInterface;

	public PcapInitRunnable(PluginInterface pi){
		pluginInterface = pi;
	}

	/**
	 * Start the RstDetector threads here. Will need one per
	 * interface until we determine which one it is.
	 */
	public void run()
	{

		//Obtain the list of network interfaces
		NetworkInterface[] devices; //= JpcapCaptor.getDeviceList();
		try{
			devices = JpcapCaptor.getDeviceList();
		}catch(NoClassDefFoundError ncdfe){

			return;
		}catch(UnsatisfiedLinkError ule){
			
			return;
		}


		//for each network interface
		Set localIps = new HashSet(); //Set<InetAddress>

		for (int i = 0; i < devices.length; i++) {
			String deviceName = createDeviceName(i, devices);

			if( ! devices[i].loopback  ){

				pluginInterface.getUtilities().createThread("RstDetector",new RstDetectorRunnable(pluginInterface,devices[i]));

				Set interfaceAddrSet = getInterfaceIps(devices[i]);
				localIps.addAll( interfaceAddrSet );

			}//if

		}//for

		//Start the PeerManager logs.
		pluginInterface.getUtilities().createThread("AllPeersMonitor",new AllPeersMonitorRunnable(pluginInterface));

	}//run

	private static Set<InetAddress> getInterfaceIps(NetworkInterface ni){

		Set retVal = new HashSet();
		NetworkInterfaceAddress[] nia = ni.addresses;
		for(int i=0; i<nia.length; i++){
			retVal.add( nia[i].address );
		}

		return retVal;
	}

	private static String createDeviceName(int i, NetworkInterface[] devices)
	{
		StringBuffer sb = new StringBuffer();

		//print out its name and description
		sb.append(i).append(": ").append(devices[i].name).append("(").append(devices[i].description).append(")\n");

		//print out its datalink name and description
		sb.append(" datalink: ").append(devices[i].datalink_name);
		sb.append("(").append(devices[i].datalink_description).append(")\n");

		//print out its MAC address
		sb.append(" MAC address:\n");
		for (byte b : devices[i].mac_address){
			sb.append(Integer.toHexString(b & 0xff)).append(":");
		}
		sb.append("\n");

		//print out its IP address, subnet mask and broadcast address
		for (NetworkInterfaceAddress a : devices[i].addresses){
			sb.append(" address:").append(a.address).append(" ").append(a.subnet);
			sb.append(" ").append(a.broadcast).append(" \n");
		}

		return sb.toString();

	}//createDeviceName


}//class
