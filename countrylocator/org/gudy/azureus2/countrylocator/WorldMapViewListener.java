/* $Id: WorldMapViewListener.java,v 1.1 2006-07-07 14:20:14 gooogelybear Exp $ */
package org.gudy.azureus2.countrylocator;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

/**
 * This UISWTViewEventListener handles the lifecycle of the different instances of the 
 * view (one for each torrent).
 * 
 * @see MapView
 * 
 * @author gooogelybear
 * @author free_lancer
 */
public class WorldMapViewListener implements UISWTViewEventListener {

	/** our reference to the country locator */
	private CountryLocator countryLocator;
	
	/* UI stuff */
	private UISWTInstance swtInstance; // this is not used atm
	
	/**
	 * Maps the <code>UISWTView</code> object of a view to the
	 * <code>MapView</code> instance for this view
	 */
	private HashMap mapViews = new HashMap();
	
	public WorldMapViewListener(UISWTInstance swtInstance, CountryLocator cl) {
		this.swtInstance = swtInstance;
		countryLocator = cl;
	}
	
	public boolean eventOccurred(UISWTViewEvent event) {
		UISWTView currentView = event.getView();
		MapView mapView;
		
		switch (event.getType()) {
		case UISWTViewEvent.TYPE_CREATE:
			/* A new view has been opened: We create a new MapView instance for it */
			mapView = new MapView(countryLocator, currentView);
			mapViews.put(currentView, mapView);
			break;
			
		case UISWTViewEvent.TYPE_INITIALIZE:
			/* Initialise the view: Create the SWT widgets for it */
			mapView = (MapView)mapViews.get(currentView);
			mapView.initialize((Composite) event.getData());
			break;

		case UISWTViewEvent.TYPE_REFRESH:
			/* Periodical refresh: Update the stats values */
			mapView = (MapView)mapViews.get(event.getView());
			mapView.refresh();
			break;

		case UISWTViewEvent.TYPE_DESTROY:
			/* The view has been closed: Remove it from the map and let the GC collect it*/
			mapView = (MapView)mapViews.get(currentView);
			mapView = null;
			mapViews.remove(currentView);
			currentView = null;
			break;
		}
		return true;
	}

}