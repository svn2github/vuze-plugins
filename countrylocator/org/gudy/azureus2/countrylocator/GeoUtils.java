package org.gudy.azureus2.countrylocator;


import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.eclipse.swt.graphics.Point;

/**
 * Some geographical conversion & mapping algorithms
 * 
 * @author free_lancer
 * @author gooogelybear
 */
public class GeoUtils {
	private final static int MAX_CACHE_SIZE = 500;
	
	/* Cache for CC to Longitude/lattitudeL mappings. key-type: String - value-type: Point2D.Float */
	private static HashMap CC2LLMappings = new HashMap();
		
	//caches
	/* key/value-type: String */
	private static HashMap IP2CCache = new HashMap();
	/* key-type: String - value-type: Point*/
	private static HashMap CC2XYCache = new HashMap();
	
	//map picture dimensions
	private static int mapWidth;
	private static int mapHeight;
	
	private static CountryLocator countryLocator;
	
	/** 
	 * Loads mappings from country code to location (longitude & lattitude) into the cache.
	 * Currently the Country2Coord.txt file is used for this.
	 * It would be nicer and more accurate to use the GeoIP Lite City database, but
	 * the database file is just too big to ship it with the plugin.
	 */
	public static void loadCC2LLMappings() {
		//load file into a BufferedReader
		try {
			InputStream is = GeoUtils.class.getClassLoader().getResourceAsStream("Country2Coord.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			
			//skip first 5 lines
			br.readLine(); br.readLine(); br.readLine(); br.readLine(); br.readLine();
			//parse lines
			while ((line = br.readLine()) != null) {
				String[] s = line.split(",");
				Point2D.Float p = new Point2D.Float(Float.valueOf(s[2]).floatValue(), 
						Float.valueOf(s[1]).floatValue());
				//save as countrycode, longitude, latitude
				CC2LLMappings.put(s[0], p);
			}
		} catch (IOException e) {
			System.err.println("Error while reading Country2Coord.txt");
		}
	}
	
	/** 
	 * Maps a country code to a point (x,y) in the <i>unscaled</i> map image.
	 * Results are cached.
	 * 
	 * @param cc the country code to map
	 * @return a point whose coordinates are relative to the unscaled map
	 */
	public static Point mapCountryCodeToXYCoord(String cc) {
		if (!CC2XYCache.containsKey(cc)) {
			// clear cache if it gets to big
			if (CC2XYCache.size() > MAX_CACHE_SIZE) {
				CC2XYCache.clear();
			}
			CC2XYCache.put(cc, geographicMapping(mapCountryCodeToLongLat(cc)));
		}
		return (Point)CC2XYCache.get(cc);
	}
	
	/** 
	 * Wrapper for <code>getIPISO3166(ip)</code> which uses a cache to store
	 * already received mappings.
	 * 
	 * @return the country code for the given IP
	 */
	public static String mapIPToCountryCode(String ip) {
		if (!IP2CCache.containsKey(ip)) {
			/* clear cache if it gets too big */
			if (IP2CCache.size() > MAX_CACHE_SIZE) {
				IP2CCache.clear();
			}
			IP2CCache.put(ip, countryLocator.getIPISO3166(ip) ); //Plugin.cl.getCountry(ip)
		}
		return (String)IP2CCache.get(ip);
	}
	
	private static Point2D.Float mapCountryCodeToLongLat(String cc) {
		/* Uses: Plugin.CC2LLMappings to get mappings */
		return (Point2D.Float)CC2LLMappings.get(cc);
	}
	
	/** 
	 * Maps longitude/latitude coordinates to xy coordinates on a geographic projection
	 */
	private static Point geographicMapping(Point2D.Float ll) {
		if (ll == null) return null;
		int x = (int)(mapWidth/360f*ll.x + mapWidth/2f);
		int y = (int)(-mapHeight/180f*ll.y + mapHeight/2f);
		return new Point(x, y);
	}
	
	/**
	 * Set the size of the image for which
	 * <code>mapCountryCodeToXYCoord(cc)</code> calculates (x,y) coordinates.
	 */
	public static void setMapSize(int width, int height) {
		mapWidth = width;
		mapHeight = height;
	}
	
	public static void setCountryLocator(CountryLocator cl) {
		countryLocator = cl;
	}
}
