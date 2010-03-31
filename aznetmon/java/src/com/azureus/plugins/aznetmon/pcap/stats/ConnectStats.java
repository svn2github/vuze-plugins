package com.azureus.plugins.aznetmon.pcap.stats;

import jpcap.packet.*;

import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;

import com.azureus.plugins.aznetmon.pcap.IpPortPair;
import com.azureus.plugins.aznetmon.pcap.LocalIpList;
import com.azureus.plugins.aznetmon.pcap.IpPort;

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

public class ConnectStats
{

	final IpPortPair key;

	long startTime=0;

	long endTime=0;
	long lastClosePacketTime=0;  //different then endTime when multiple packets recieved.

	long bytesTransferred=0;

	ConnectEndType endType = ConnectEndType.FIN;

	List<Packet> finPackets = new ArrayList<Packet>();

	private static final long ARCHIVE_WAIT_TIME = 1000 * 30;

	public ConnectStats(IpPortPair _key){

		key = _key;

		startTime = System.currentTimeMillis();

	}


	public IpPortPair getIpPortPair(){
		return key;
	}

	public synchronized void addByteTransferred(long pktLen){

		bytesTransferred += pktLen;

	}

	public synchronized void finPacket(ConnectEndType type, Packet pkt){

		if( endTime==0 ){
			endTime = System.currentTimeMillis();
		}

		//different then endTime only when multiple close packets are recieved.
		lastClosePacketTime = System.currentTimeMillis();
		
		finPackets.add(pkt);

		if( endType == ConnectEndType.FIN ){
			endType = type;
		}

	}

	/**
	 * True if multiple resets are detected.  Multiple RST packet might be
	 * an indicator of Forged RST packets from an ISP in the middle of a connection.
	 * @return - true if more then one packet sent.
	 */
	public boolean hasMultipleResets(){
		return ( finPackets.size()>1 );
	}

	public String printConnectionResult(){

		StringBuffer sb = new StringBuffer();

		sb.append( key ).append(" ");

		sb.append( getEndTypeString() );

		//If it had multiple RSTs detected.
		if( hasMultipleResets() ){
			sb.append(" Multi ");
		}else{
			sb.append("       ");
		}


		String connectTime = getConnectTime();
		sb.append(" - ").append( connectTime );

		String bytesTransferred = getBytesTransferred();
		sb.append(" - ").append( bytesTransferred );

		return sb.toString();
	}


	/**
	 * Print
	 * @return -
	 */
	public String printConnectionResultWithFinPackets(){

		String summary = printConnectionResult();

		StringBuffer sb = new StringBuffer();

		int size = finPackets.size();

		for(int i=0; i<size; i++){
			sb.append( printPacketType( finPackets.get(i) ) ).append("\n");
		}

		return summary+"\n"+sb.toString();
	}


	public String getBytesTransferred(){

		if( bytesTransferred < 1024 ){
			//use units bytes.
			return bytesTransferred+" bytes";


		}else if( bytesTransferred < (1024 * 1000) ){
			//use units kb
			return bytesTransferred/1024+" kbytes";

		}else{
			//user units MB
			return bytesTransferred/(1024*1024)+" MB";
		}
	}//getBytesTransferred


	public String getConnectTime(){

		//if
		if( startTime==0 ){
			return "not connected";
		}

		if( endTime==0 ){
			return "still connected";
		}

		long totTime = (endTime - startTime)/1000;

		return "connected - "+totTime+" seconds ";
	}//getConnectTime



	public String getEndTypeString(){

		if( endType == ConnectEndType.FIN ){
			return "FIN";
		}else if( endType == ConnectEndType.RST ){
			return "RST";
		}

		return "unknown";
	}


	/**
	 * 
	 * @return -
	 */
	public ConnectEndType getEndType(){
		return endType;
	}

	/**
	 *
	 * @return -
	 */
	public IpPortPair getKey(){
		return key;
	}

	
	public int getNumEndPackets(){
		return finPackets.size();
	}

	public static String printPacketType(Packet packet){

	if( packet instanceof IPPacket){

		if( packet instanceof TCPPacket){

			boolean rstDetected=false;
			boolean finDetected=false;

			TCPPacket tcpPkt = (TCPPacket) packet;

			if( tcpPkt.rst ){
				rstDetected=true;
			}

			if( tcpPkt.fin ){
				finDetected=true;
			}

			//Print-out some extra information when connection terminates.
			if( rstDetected || finDetected ){

				InetAddress destIp = tcpPkt.dst_ip;
				int destPort = tcpPkt.dst_port;

				InetAddress srcIp = tcpPkt.src_ip;
				int srcPort = tcpPkt.src_port;

				long sequence = tcpPkt.sequence;

				StringBuilder sb = new StringBuilder(" terminated connection: ");
				if( rstDetected ){
					sb.append("RST ");
				}
				if( finDetected ){
					sb.append("FIN ");
				}

				sb.append( "src: " ).append(srcIp.getHostAddress()).append(":").append(srcPort);
				sb.append( ",dest: ").append(destIp.getHostAddress()).append(":").append(destPort);
				sb.append(" -- seq# : ").append(sequence);

				return sb.toString();
			}

		}else if( packet instanceof UDPPacket){
			return "UDP Packet";
		}

		}else if( packet instanceof ARPPacket){
 			return "ARPPacket";
		}

		return "unknow packet type";
	}//printPacketType


	public enum ConnectEndType{
		FIN,
		RST
	}


	/**
	 *
	 * A key used to signify a connection. (bi-directional)
	 * @param packet -
	 * @return String like "192.168.6.85:8080 -> 71.34.56.23:54034", src -> dest
	 */
	public static IpPortPair connectionKey(TCPPacket packet)
	{
		//Need to determine which IpAddress is Local!!
		LocalIpList local = LocalIpList.getInstance();

		IpPort p1;
		IpPort p2;

		if( local.isLocalIp(packet.src_ip) ){
			p1 = new IpPort( IpPort.LOCAL_IP_CONSTANT, IpPort.LOCAL_PORT_CONSTANT );
			p2 = new IpPort( packet.dst_ip.getHostAddress(), packet.dst_port );
		}else if( local.isLocalIp(packet.dst_ip)){
			p1 = new IpPort( packet.src_ip.getHostAddress(), packet.src_port );
			p2 = new IpPort( IpPort.LOCAL_IP_CONSTANT, IpPort.LOCAL_PORT_CONSTANT );
		}else{
			p1 = new IpPort( packet.src_ip.getHostAddress(), packet.src_port );
			p2 = new IpPort( packet.dst_ip.getHostAddress(), packet.dst_port );
		}

		return new IpPortPair(p1,p2);
	}


	public static ConnectStats createInitialStat(IpPortPair key, TCPPacket packet)
	{
		//verify the key first.
		if( !key.equals( connectionKey(packet) ) ){
			throw new IllegalStateException("invalid keys: "+key+" != "+connectionKey(packet) );
		}

		ConnectStats retVal = new ConnectStats(key);
		retVal.addByteTransferred( packet.len );

		return retVal;
	}

	public static boolean isCloseConnectionPacket( TCPPacket packet )
	{
		return ( packet.fin || packet.rst );
	}

	/**
	 * Has this connect been inactive long enough to archive?
	 * @param currTime - current time.
	 * @return - boolean - true if time to archive.
	 */
	public boolean isTimeToArchive(long currTime){
		
		return ( lastClosePacketTime+ARCHIVE_WAIT_TIME > currTime );
	}

}//class

