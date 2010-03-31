package org.gudy.azureus2.countrylocator.statisticsProviders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.countrylocator.UIConstants;
import org.gudy.azureus2.countrylocator.GeoUtils;
import org.gudy.azureus2.plugins.peers.Peer;

/**
 * UploadedDataProvider (see {@link #getStatistics(Peer[])})
 * 
 * @author gooogelybear
 * @author free_lancer
 */
public class UploadedDataProvider implements IStatisticsProvider {

	public final static String NAME = "Uploaded data";
	public final static String UNIT = "kB";
	
	public String getName() {
		return NAME;
	}

	/**
	 * @return The amount of data (in kB) uploaded to each country (w.r.t. the
	 * currently connected peers from each country).
	 */
	public Map getStatistics(Peer[] peers) {
		Map statsMap = new HashMap();
		for (int i = 0; i < peers.length; i++) {
			Peer peer = peers[i];
			String CC = GeoUtils.mapIPToCountryCode(peer.getIp());
			/* Skip unknown country */
			if (! UIConstants.UNKNOWN_COUNTRY.equals(CC)) {
				/* get the nr of peers and increase it */
				long uploadedData = (statsMap.get(CC) != null) ? ((Long)statsMap.get(CC)).longValue() : 0;
				uploadedData += peer.getStats().getTotalSent();
				statsMap.put(CC, new Long(uploadedData));				
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
