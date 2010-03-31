package com.azureus.plugins.aznetmon.pcap;

import jpcap.NetworkInterface;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsMonitor;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsSummary;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStats;
import com.azureus.plugins.aznetmon.pcap.util.NetMon16Util;
import com.azureus.plugins.aznetmon.util.Signals;
import org.gudy.azureus2.plugins.PluginInterface;

import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;

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

public class RstDetectorRunnable implements Runnable
{

	private final PluginInterface pluginInterface;
	private final NetworkInterface device;


	Set<InetAddress> localIps;

	public RstDetectorRunnable(PluginInterface _pluginInterface, NetworkInterface _device){

		pluginInterface = _pluginInterface;
		device = _device;

		localIps = getLocalIps(device);

	}

	private static Set<InetAddress> getLocalIps(NetworkInterface device){

		LocalIpList local = LocalIpList.getInstance();
		Set<InetAddress> retVal = new HashSet<InetAddress>();
		for(NetworkInterfaceAddress a : device.addresses){
			retVal.add( a.address );
			local.add( a.address );
		}//for

		return retVal;
	}

	public void run()
	{
		
		//Obtain the list of network interfaces
		NetworkInterface[] devices = JpcapCaptor.getDeviceList();

		//for each network interface
		for (int i = 0; i < devices.length; i++) {
			//print out its name and description

			NetMon16Util.debug(i+": "+devices[i].name + "(" + devices[i].description+")");

			//print out its datalink name and description
			NetMon16Util.debug(" datalink: "+devices[i].datalink_name + "(" + devices[i].datalink_description+")");

			//print out its MAC address
			NetMon16Util.debug(" MAC address:");
			for (byte b : devices[i].mac_address)
				NetMon16Util.debug(Integer.toHexString(b&0xff) + ":");
			NetMon16Util.debug("\n");

			//print out its IP address, subnet mask and broadcast address
			for (NetworkInterfaceAddress a : devices[i].addresses){
				NetMon16Util.debug(" address:"+a.address + " " + a.subnet + " "+ a.broadcast);
			}
		}


		int index=0;
		int maxIndex = devices.length;
		for(int i=0; i<maxIndex; i++){

			//ToDo: any-way to find just the most active interface? This might induce a delay in startup.

			NetworkInterface ni = devices[index];
			go(ni);
		}

		NetMon16Util.debug("EXIT - RstDetectorRunnable.run() ");

	}//run

	private void go(NetworkInterface ni)
	{
		JpcapCaptor captor = null;
		ConnectStatsMonitor csm = ConnectStatsMonitor.getInstance();

		try{
			//Open an interface with openDevice(NetworkInterface intrface, int snaplen, boolean promics, int to_ms)
			try{
				captor=JpcapCaptor.openDevice(ni, 65535, false, 20);
			}catch(Exception e){
				NetMon16Util.debug(" couldn't open device for: "+e);
				return;
			}

			PacketReceiver pktRecv = new PacketLogger();

			Signals signals = Signals.getInstance();
			while( !signals.isShutdown() ){

				captor.processPacket(10, pktRecv );

			}//while

		}catch(Throwable t){
			t.printStackTrace();
		}finally{
		    if(captor!=null){
				captor.close();
			}

			//more everything to archive.


			//write summary.
			ConnectStatsSummary summary = csm.createArchiveSummary();

			float percReset = summary.percentResetCloses();

			System.out.println(" %RST closes:       "+percReset);
			System.out.println(" #RST               "+summary.numRstCloses);
			System.out.println(" #FIN               "+summary.numFinCloses);

			System.out.println(" #unique ips =      "+summary.uniqueIps.size() );
			System.out.println(" #unique src ports= "+summary.uniquePorts.size() );

			int size = summary.resetIpAddr.size();
			for( int i=0;i<size;i++ ){

				ConnectStats cs = summary.resetIpAddr.get(i);

				NetMon16Util.debug( cs.printConnectionResultWithFinPackets() );
			}

		}

	}//go


	public void logPacket(Packet packet){

		//only log TCP connections from BitTorrent.
		if(  !(packet instanceof TCPPacket) ){
			//only track TCP connections for now.
			return;
		}

		TCPPacket tcpPacket = (TCPPacket) packet;

		IpPortPair key = ConnectStats.connectionKey(tcpPacket);

		ConnectStatsMonitor csm = ConnectStatsMonitor.getInstance();
		if( !csm.hasConnection(key) ){

			ConnectStats cs = ConnectStats.createInitialStat(key,tcpPacket);

			csm.addConnection(key,cs);

			NetMon16Util.debug("new connection :: "+key);

		}else{

			if( !ConnectStats.isCloseConnectionPacket( tcpPacket ) ){
				csm.updateConnectStats(key,packet);
			}else{
				csm.setCloseStats(key,tcpPacket);
				NetMon16Util.debug( "closing       ::  "+key );
			}
		}

	}//logPacket


	/**
	 * Class to process a packet
	 */
	class PacketLogger implements PacketReceiver {

		public PacketLogger(){}

		public void receivePacket(Packet packet){
			logPacket(packet);
		}

	}//class

}
