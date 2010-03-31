package com.azureus.plugins.aznetmon.pcap;

import com.azureus.plugins.aznetmon.main.DataListener;

import java.util.List;
import java.util.ArrayList;

/**
 * Created on Apr 16, 2008
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

public class AzureusPeerStats
{

	//This is java 1.6

	//reset_records data items for verion 2. Uses items from RSTPacketStatus.java and must be in synch with version server.
	//TIME, RESET_RECORDS, PLUGIN_MAP_NAME are also used here.
	public static final String N_PEER_RST = "nPeerRST";
	public static final String N_CLOSED_PEER = "nClosedPeer";
	public static final String N_CLOSED_PEER_W_TXF = "nClosedPeerTxfr";
	public static final String N_FAILED_CONN = "nFailedConn";


	private static AzureusPeerStats instance = new AzureusPeerStats();

	private List<RstData> rstDataList; //<RstData>
	private List<DataListener> dataListeners; //<DataListener>

	private String status;

	public static AzureusPeerStats getInstance(){
		return instance;
	}

	private AzureusPeerStats()
	{
		rstDataList = new ArrayList<RstData>();
		dataListeners = new ArrayList<DataListener>();
	}


	public String getStatus(){
		return status;
	}

	public void setStatus(String _status){
		status = _status;
	}

	
	public RstData getMostRecent(){

		if( rstDataList.size()>0 ){
			return rstDataList.get(0);
		}else{
			return null;
		}
	}//getMostRecent

	/**
	 * Get the "n" most recent items.
	 * @param n - the number of items to get.
	 * @return RstData -
	 */
	public RstData[] getMostRecent(int n){

		int size = rstDataList.size();
		if( n<size ) size = n;

		if(size==0){
			RstData[] def = new RstData[1];
			def[0] = new RstData();
			return def;
		}

		RstData[] retVal = new RstData[size];
		for(int i=0; i<size; i++ ){

			retVal[i] = rstDataList.get(i);

		}//for

		return retVal;
	}//getMostRecent

	/**
	 * Return all the unstored results and mark them as stored. Some
	 * process further down the line will actually use the data.
	 * @return - RstData[]
	 */
	public RstData[] getUnstoredResults()
	{

		List<RstData> unstoredList = new ArrayList<RstData>();
		for( RstData rd : rstDataList ){
			if( !rd.isStored ){
				unstoredList.add( rd );
				rd.isStored=true; //ToDo: verify.
			}
		}//for

		return unstoredList.toArray( new RstData[0] );
	}//getUnstoredResults


	/**
	 * Add data to the list.
	 * @param data -
	 */
	public void addData(RstData data){
		rstDataList.add(0,data);
	}


	public synchronized void addListener(DataListener dl){
		dataListeners.add(dl);
	}


	public synchronized void removeListener(DataListener dl){
		dataListeners.remove(dl);
	}
	

	public synchronized void update(){

		int size = dataListeners.size();
		for(int i=0; i<size; i++ ){
			DataListener dl = dataListeners.get(i);
			dl.update();
		}
	}//update
	

	public static class RstData{

		public boolean isStored=false;

		public long timestamp=0;  //col0
		public float percentRSTConn = 0.0f;
		public int numPeerRst = 0;
		public int numPeers = 0;
		public int numFailedConn = 0;
		public StringBuffer ips = new StringBuffer();
		

		public RstData(){
			timestamp = System.currentTimeMillis();
		}

		public RstData(int nPeerRst, int totPeerClose, int nFailedPeerConn){
			this();

			numPeerRst = nPeerRst;
			numPeers = totPeerClose;
			numFailedConn = nFailedPeerConn;

			if( numPeers!=0 ){
				percentRSTConn = (float) numPeerRst/numPeers;
			}else{
				percentRSTConn = 0.0f;
			}

		}

		public void addIp(String ip){

			if( ips.length()>0 ){
				ips.append(" : ");
			}
			ips.append(ip);
		}

	}//class

}
