/**
 * 
 */
package org.gudy.azureus2.countrylocator;

import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * Must be a seperate class file to trap NoClassDefFoundError
 * 
 * @author TuxPaper
 * @created Oct 18, 2005
 *
 */
class MyUIManagerListener implements UIManagerListener
{
	public void UIAttached(UIInstance instance) {
		if (instance instanceof UISWTInstance) {
			CountryLocator.swtInstance = ((UISWTInstance) instance);
			CountryLocator.display = CountryLocator.swtInstance.getDisplay();
		}
	}

	public void UIDetached(UIInstance instance) {
		if (instance instanceof UISWTInstance) {
			CountryLocator.swtInstance = null;
			CountryLocator.display = null;
		}
	}
}