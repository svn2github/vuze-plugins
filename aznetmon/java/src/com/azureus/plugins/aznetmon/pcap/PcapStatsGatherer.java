/**
 * Created on Mar 20, 2008
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

import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsMonitor;
import com.azureus.plugins.aznetmon.pcap.stats.ConnectStatsSummary;
import com.azureus.plugins.aznetmon.util.AzNetMonLogger;
import com.azureus.plugins.aznetmon.main.RSTPacketStats;


import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;

import java.util.*;

public class PcapStatsGatherer {
	private static PcapStatsGatherer ourInstance = new PcapStatsGatherer();

	private final LoggerChannel logger;

	public static PcapStatsGatherer getInstance() {
		return ourInstance;
	}

	private PcapStatsGatherer()
	{
		logger = AzNetMonLogger.getLogger();
	}


	/**
	 * Find the matches between RST connections and recently closed peers.
	 *
	 */
	public void gather()
	{

		logger.log("gather");

		PeersStateMonitor psm = PeersStateMonitor.getInstance();
		PeerStateSummary pss = psm.createArchiveSummary();

		ConnectStatsMonitor csm = ConnectStatsMonitor.getInstance();
		csm.moveToArchive();
		ConnectStatsSummary css = csm.createArchiveSummary();

		//find matches.
		Set<IpPortPair> allPeers = pss.getAllCommonPeers();
		Set<IpPortPair> resetPeers =  pss.getOnlyResetPeers(css);
		Set<IpPortPair> candidateResets = pss.getMultiResetPeers(css);

		int nAllPeers = allPeers.size();
		int nResetPeers = resetPeers.size();
		int nCanResets = candidateResets.size();

		float pReset = (float) nResetPeers/nAllPeers;
		float pCanReset = (float) nCanResets/nAllPeers;

		AzureusPeerStats aps = AzureusPeerStats.getInstance();
		AzureusPeerStats.RstData data = new AzureusPeerStats.RstData(nCanResets,nAllPeers,nResetPeers-nCanResets);

		for( IpPortPair ipp : candidateResets ){
			data.addIp( ipp.remoteIp() );
		}

		aps.addData(data);

		aps.update();

		//What we need at the end of this set is to take the IPs and link them with PeerStateMonitor.

		logger.log(" nAllPeers              : "+nAllPeers);
		logger.log(" nResetPeers            : "+nResetPeers);
		logger.log(" nCanResets             : "+nCanResets);
		logger.log(" failed opens and RSTs  : "+pReset);
		logger.log(" candidate injected RSTs: "+pCanReset);
		logger.log( css.getLog() );

	}//gather


	/**
	 * Return true if it is time to store data. This is the first results
	 * past the start of the hour.
	 * @return - boolean
	 */
	public boolean timeToStoreData(){

		//will have a hack implemention here, since we know that the gather period is 10 minutes.
		//maybe can fix it up in the future.
		long currTime = System.currentTimeMillis();
		long prevTime = currTime - 1000 * 60 * 10; //ToDo: replace with PERIOD_CONST.

		//If these are different hours then save the result.
		Date currDate = new Date(currTime);
		Date prevDate = new Date(prevTime);

		int cHour = currDate.getHours();
		int pHour = prevDate.getHours();


		return ( cHour!=pHour );
	}//timeToStoreData

	/**
	 * Store the results associated with this results. To store in the version server.
	 * This method should be called after the first result past the start of the hour.
	 *
	 *
	 *
	 */
	public void store(PluginInterface pi)
	{

		AzureusPeerStats aps = AzureusPeerStats.getInstance();

		AzureusPeerStats.RstData[] rawData = aps.getUnstoredResults();

		AzureusPeerStats.RstData summaryData = sum(rawData);

		//ToDo: need a sync block and object to prevent collision with
		//ToDo: RSTPacketCountPerformer.addToVersionServer

		try{

			PluginConfig pc = pi.getPluginconfig();

			Map vsData = pc.getPluginMapParameter( RSTPacketStats.PLUGIN_MAP_NAME, new HashMap() );
			List accumulatedResults = (List) vsData.get(RSTPacketStats.RESET_RECORDS);
			if( accumulatedResults == null ){
				accumulatedResults = new ArrayList();
				vsData.put(RSTPacketStats.RESET_RECORDS,accumulatedResults);
			}
			Map appendData = new AzureusPeerStore( summaryData );

			//Don't allow total records to go above 100.
			if( accumulatedResults.size()>100 ){
				accumulatedResults.remove(0);
			}

			accumulatedResults.add( appendData );
			pc.setPluginMapParameter(RSTPacketStats.PLUGIN_MAP_NAME,vsData);

		}catch(Exception e){
			e.printStackTrace();
		}

	}

	/**
	 * Hour the rawData will be accumulated.
	 * @param rawData -
	 * @return -
	 */
	AzureusPeerStats.RstData sum(AzureusPeerStats.RstData[] rawData){

		AzureusPeerStats.RstData retVal = new AzureusPeerStats.RstData();
		retVal.timestamp = System.currentTimeMillis();

		for( AzureusPeerStats.RstData curr : rawData ){

			retVal.numPeerRst += curr.numPeerRst;
			retVal.numPeers += curr.numPeers;
			retVal.numFailedConn += curr.numFailedConn;

			//ToDo: maybe accululate list of IPs in the future. This would be a StringBuffer.
		}

		return retVal;
	}//sum

	/**
	 * Just a Map with a special constructor.
	 */
	static class AzureusPeerStore extends HashMap{

		public AzureusPeerStore(AzureusPeerStats.RstData data){

			put( RSTPacketStats.TIME,  new Long( data.timestamp ) );
			put( AzureusPeerStats.N_PEER_RST, new Integer(data.numPeerRst) );
			put( AzureusPeerStats.N_CLOSED_PEER, new Integer(data.numPeers) );
			put( AzureusPeerStats.N_CLOSED_PEER_W_TXF, new Integer(data.numPeers) ); //ToDo: current same as numPeers, look for Peers with transfer in future.
			put( AzureusPeerStats.N_FAILED_CONN, new Integer(data.numFailedConn) );

		}//
	}

}
