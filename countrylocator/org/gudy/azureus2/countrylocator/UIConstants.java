package org.gudy.azureus2.countrylocator;

import org.eclipse.swt.graphics.RGB;

/**
 * Contains some UI constants for the MapView.
 * 
 * @author gooogelybear
 * @author free_lancer
 */
public class UIConstants {
	
	/* These constants represent indices into the combo box widget & selectedStats
	 * vector and also the column placement.
	 */
	public static final int NUMBER_OF_PEERS_POSITION = 0;
	public static final int DOWNLOADED_DATA_POSITION = 1;
	public static final int UPLOADED_DATA_POSITION = 2;
	public static final int DOWN_SPEED_POSITION = 3;
	public static final int UP_SPEED_POSITION = 4;
	
	/**
	 * Defines the size of the graphical objects which are used to display the
	 * values for each country, e.g. currently a filled circle
	 */
	public static final int COUNTRY_RADIUS = 4; // 3
	public static final int COUNTRY_SIZE = 2*COUNTRY_RADIUS + 1;
	
	/*
	 * Default colors for country object (dot) on worldmap
	 */
	public static final RGB COUNTRY_MIN_DEFAULT_COLOR = new RGB(0, 0, 0);
	public static final RGB COUNTRY_MAX_DEFAULT_COLOR = new RGB(255, 0, 0);
	public static final RGB COUNTRY_HIGHLIGHT_DEFAULT_COLOR = new RGB(255, 255, 0);

	public static float MAX_ZOOM = 5f;
	
	public static final String MAP_FILE_800 = "org/gudy/azureus2/countrylocator/maps/Earthmap_800.jpg";
	public static final String MAP_FILE_2048 = "org/gudy/azureus2/countrylocator/maps/Earthmap_2048.jpg";
	public static final String MAP_FILE_4096 = "org/gudy/azureus2/countrylocator/maps/Earthmap_4096.jpg";
	
	public static final String UNKNOWN_COUNTRY = "--";
}
