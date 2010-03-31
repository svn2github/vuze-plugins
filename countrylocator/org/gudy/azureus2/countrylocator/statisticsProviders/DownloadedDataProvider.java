package org.gudy.azureus2.countrylocator.statisticsProviders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.countrylocator.GeoUtils;
import org.gudy.azureus2.countrylocator.UIConstants;
import org.gudy.azureus2.plugins.peers.Peer;

/**
 * DownloadedDataProvider (see {@link #getStatistics(Peer[])})
 * 
 * @author gooogelybear
 * @author free_lancer
 */
public class DownloadedDataProvider implements IStatisticsProvider {

	public final static String NAME = "Downloaded data";
	public final static String UNIT = "kB";
	
	public String getName() {
		return NAME;
	}

	/**
	 * @return The amount of data (in kB) downloaded from each country (w.r.t. the
	 * currently connected peers from each country).
	 */
	public Map getStatistics(Peer[] peers) {
		// key = cc, value = downloaded data
		Map statsMap = new HashMap();
		for (int i = 0; i < peers.length; i++) {
			Peer peer = peers[i];
			String CC = GeoUtils.mapIPToCountryCode(peer.getIp());
			/* Skip unknown country */
			if (! UIConstants.UNKNOWN_COUNTRY.equals(CC)) {
				/* aggregate amount of downloaded data */
				long downloadedData = (statsMap.get(CC) != null) ? ((Long)statsMap.get(CC)).longValue() : 0;
				downloadedData += peer.getStats().getTotalReceived();
				statsMap.put(CC, new Long(downloadedData));
			}
		}
		/* conversion B -> kB (could be done directly above)*/
		for (Iterator it = statsMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry)it.next();
			entry.setValue(new Long(((Long)entry.getValue()).longValue() / 1024));
		}
		
		return statsMap;
	}

	public String getUnit() {
		return UNIT;
	}

}
