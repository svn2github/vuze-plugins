package com.vuze.plugins.test;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;

public class PluginToolBarTest
	implements UnloadablePlugin
{

	private UIInstance instance;

	public void initialize(PluginInterface pi)
			throws PluginException {
		pi.getUIManager().addUIListener(new UIManagerListener() {

			public void UIDetached(UIInstance instance) {
			}

			public void UIAttached(UIInstance instance) {
				PluginToolBarTest.this.instance = instance;
				UIToolBarManager tbm = instance.getToolBarManager();
				if (tbm != null) {
					UIToolBarItem item1 = tbm.createToolBarItem("plugintest");
					item1.setTextID("PluginToolBarTest.toolbar.text");
					item1.setDefaultActivationListener(new DefaultActivationImpl(instance));
					item1.setState(UIToolBarItem.STATE_ENABLED);
					tbm.addToolBarItem(item1);

					UIToolBarItem item2 = tbm.createToolBarItem("plugintest2");
					item2.setTextID("PluginToolBarTest.toolbar.text2");
					item2.setDefaultActivationListener(new DefaultActivationImpl(instance));
					item2.setState(UIToolBarItem.STATE_ENABLED);
					tbm.addToolBarItem(item2);

					UIToolBarItem item3 = tbm.createToolBarItem("plugintest3");
					item3.setTextID("PluginToolBarTest.toolbar.text3");
					item3.setDefaultActivationListener(new DefaultActivationImpl(instance));
					item3.setState(UIToolBarItem.STATE_ENABLED);
					tbm.addToolBarItem(item3);
				}
			}
		});
	}

	public void unload()
			throws PluginException {
		// might not be needed
		instance.getToolBarManager().removeToolBarItem("plugintest");
		instance.getToolBarManager().removeToolBarItem("plugintest2");
		instance.getToolBarManager().removeToolBarItem("plugintest3");
	}

}
