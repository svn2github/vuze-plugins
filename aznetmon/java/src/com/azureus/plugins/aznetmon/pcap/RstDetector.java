package com.azureus.plugins.aznetmon.pcap;

import jpcap.NetworkInterface;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsMonitor;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStats;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsSummary;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsKey;

/**
 * Created on Mar 11, 2008
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

public class RstDetector
{

	public RstDetector(){}


	public static void main(String[] args){

		//determine if this is Java 1.6
		String javaVersion = System.getProperty("java.version");
		System.out.println(" java version = "+javaVersion);

		//ask which interface to listen to.



		//Obtain the list of network interfaces
		NetworkInterface[] devices = JpcapCaptor.getDeviceList();

		//for each network interface
		for (int i = 0; i < devices.length; i++) {
			//print out its name and description
			System.out.println(i+": "+devices[i].name + "(" + devices[i].description+")");

			//print out its datalink name and description
			System.out.println(" datalink: "+devices[i].datalink_name + "(" + devices[i].datalink_description+")");

			//print out its MAC address
			System.out.print(" MAC address:");
			for (byte b : devices[i].mac_address)
				System.out.print(Integer.toHexString(b&0xff) + ":");
			System.out.println();

			//print out its IP address, subnet mask and broadcast address
			for (NetworkInterfaceAddress a : devices[i].addresses)
				System.out.println(" address:"+a.address + " " + a.subnet + " "+ a.broadcast);
		}

		RstDetector detector = new RstDetector();

		int index = 2;//This is the wireless network device.
		NetworkInterface ni = devices[index];
		detector.go(ni);

	}//main

	private void go(NetworkInterface ni)
	{
		JpcapCaptor captor = null;
		ConnectStatsMonitor csm = ConnectStatsMonitor.getInstance();

		try{
			//Open an interface with openDevice(NetworkInterface intrface, int snaplen, boolean promics, int to_ms)
			try{
				captor=JpcapCaptor.openDevice(ni, 65535, false, 20);
			}catch(Exception e){
				System.out.println(" couldn't open device for: "+e);
				return;
			}

			//for this test we will capture packets for one minute then turn everything off.

			long endTime = System.currentTimeMillis() + 1000 * 60 * 5;

			PacketReceiver pktRecv = new PacketLogger();

			final long ARCHIVE_INTERVAL = 1000 * 15;
			long lastArchiveCheck = System.currentTimeMillis();

			while( System.currentTimeMillis()<endTime ){

				captor.processPacket(10, pktRecv );

				//check for archives.
				long currTime = System.currentTimeMillis();

				if( currTime > lastArchiveCheck+ARCHIVE_INTERVAL ){
					csm.moveToArchive();
				}

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
			System.out.println(" #unique src ports= "+summary.uniquePorts.size()+"\n\n" );
			
			int size = summary.resetIpAddr.size();
			for( int i=0;i<size;i++ ){

				ConnectStats cs = summary.resetIpAddr.get(i);

				System.out.println( cs.printConnectionResultWithFinPackets() );
			}


		}
	}


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

			System.out.println("new connection :: "+key);

		}else{

			if( !ConnectStats.isCloseConnectionPacket( tcpPacket ) ){
				csm.updateConnectStats(key,packet);
			}else{
				csm.setCloseStats(key,tcpPacket);
				System.out.println( "closing       ::  "+key );
			}
		}

	}


	/**
	 * Class to process a packet
	 */
	class PacketLogger implements PacketReceiver {

		public PacketLogger(){}

		public void receivePacket(Packet packet){

			logPacket(packet);

		}

	}//class PacketLogger

}
