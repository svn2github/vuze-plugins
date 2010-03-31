package org.gudy.azureus2.countrylocator;

import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * The UIManagerListener for the map view which is called when an UI is attached.
 * It is registered in the in the CountryLocator.
 *
 * @author free_lancer
 * @author gooogelybear
 */
public class MapViewUIManagerListener implements UIManagerListener {
	
	private final CountryLocator countryLocator;

	MapViewUIManagerListener(CountryLocator cl) {
		this.countryLocator = cl;
	}

	public void UIAttached(UIInstance instance) {
		if (instance instanceof UISWTInstance) {
			UISWTInstance swtInstance = (UISWTInstance) instance;
			
			/* Create the WorldMapViewListener and register the view */
			CountryLocator.viewListener = new WorldMapViewListener(swtInstance,countryLocator);
			swtInstance.addView(UISWTInstance.VIEW_MYTORRENTS, 
					CountryLocator.VIEWID, CountryLocator.viewListener);
			
			/* view utils setup */
			GeoUtils.loadCC2LLMappings();
			GeoUtils.setCountryLocator(countryLocator);
		}
	}

	public void UIDetached(UIInstance instance) {
		/* the swtInstance is the same as used by the country locator */
	}

}
