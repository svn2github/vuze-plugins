package org.gudy.azureus2.countrylocator.statisticsProviders;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.countrylocator.UIConstants;
import org.gudy.azureus2.countrylocator.GeoUtils;
import org.gudy.azureus2.plugins.peers.Peer;

/**
 * NumberOfPeersProvider (see {@link #getStatistics(Peer[])})
 * 
 * @author gooogelybear
 * @author free_lancer
 */
public class NumberOfPeersProvider implements IStatisticsProvider {

	public final static String NAME = "Number of peers";
	
	/**
	 * Returns the number of peers in each country given through its CC.
	 */
	public Map getStatistics(Peer[] peers) {

		// key = cc, value = #peers
		Map statsMap = new HashMap();
		for (int i = 0; i < peers.length; i++) {
			Peer peer = peers[i];
			String CC = GeoUtils.mapIPToCountryCode(peer.getIp());
//			String CC = Plugin.getCountry(peer.getIp()).getCode();
			/* Skip unknown country */
			if (! UIConstants.UNKNOWN_COUNTRY.equals(CC)) {
				/* get the nr of peers and increase it */
				long numberOfPeers = (statsMap.get(CC) != null) ? ((Long)statsMap.get(CC)).longValue() : 0;
				numberOfPeers++;
				statsMap.put(CC, new Long(numberOfPeers));				
			}
		}

		return statsMap;
	}

	public String getName() {
		return NAME;
	}

	public String getUnit() {
		return "";
	}
}
