/*
 * i18nPlugin.java
 *
 * Created on February 22, 2004, 5:43 PM
 */

package i18nPlugin;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * ResourceBundle Comparer/Editor: Plugin into Azureus
 * @author TuxPaper
 */
public class i18nPlugin implements UnloadablePlugin {
	UISWTInstance swtInstance = null;

	View myView = null;
	
	public void initialize(final PluginInterface pluginInterface) {
		try {
			pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
				public void UIAttached(UIInstance instance) {
					if (instance instanceof UISWTInstance) {
						swtInstance = (UISWTInstance)instance;
						myView =  new View(pluginInterface, swtInstance);
						
						swtInstance.addView(UISWTInstance.VIEW_MAIN, View.VIEWID, myView);
					}
				}
	
				public void UIDetached(UIInstance instance) {
					swtInstance = null;
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void unload() throws PluginException {
		if (swtInstance == null || myView == null)
			return;

		swtInstance.removeViews(UISWTInstance.VIEW_MAIN, View.VIEWID);

		myView = null;
	}
}

