package org.gudy.azureus2.countrylocator.statisticsProviders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.countrylocator.UIConstants;
import org.gudy.azureus2.countrylocator.GeoUtils;
import org.gudy.azureus2.plugins.peers.Peer;

/**
 * UploadSpeedProvider (see {@link #getStatistics(Peer[])})
 * 
 * @author gooogelybear
 * @author free_lancer
 */
public class UploadSpeedProvider implements IStatisticsProvider {

	public final static String NAME = "Upload speed";
	public final static String UNIT = "kB/s";

	public String getName() {
		return NAME;
	}

	/**
	 * @return The current upload speed (in kB/s) with which Azureus is currently
	 * uploading to each country (w.r.t. the currently connected peers from each
	 * country).
	 */
	public Map getStatistics(Peer[] peers) {
		Map statsMap = new HashMap();
		for (int i = 0; i < peers.length; i++) {
			Peer peer = peers[i];
			String CC = GeoUtils.mapIPToCountryCode(peer.getIp());
			/* Skip unknown country */
			if (! UIConstants.UNKNOWN_COUNTRY.equals(CC)) {
				/* aggregate up speeds */
				long upSpeed = (statsMap.get(CC) != null) ? ((Long)statsMap.get(CC)).longValue() : 0;
				upSpeed += peer.getStats().getUploadAverage();
				statsMap.put(CC, new Long(upSpeed));				
			}
		}
		
		/* conversion B/s -> kB/s (could be done directly above)*/
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
