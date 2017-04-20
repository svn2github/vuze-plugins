package com.vuze.plugins.test;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIDataSourceListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;

public class PluginToolBarTest
	implements UnloadablePlugin, UIDataSourceListener
{

	private UIInstance instance;
	private PluginInterface pi;

	public void initialize(final PluginInterface pi)
			throws PluginException {
		this.pi = pi;
		pi.getUIManager().addUIListener(new UIManagerListener() {

			public void UIDetached(UIInstance instance) {
			}

			public void UIAttached(UIInstance instance) {
				PluginToolBarTest.this.instance = instance;
				UIToolBarManager tbm = instance.getToolBarManager();
				if (tbm != null) {
					UIToolBarItem item1 = tbm.createToolBarItem("plugintest");
					item1.setTextID("PluginToolBarTest.toolbar.text");
					item1.setDefaultActivationListener(
							new DefaultActivationImpl(instance));
					item1.setState(UIToolBarItem.STATE_ENABLED);
					item1.setImageID("image.vuze-entry.frog");
					tbm.addToolBarItem(item1);

					UIToolBarItem item2 = tbm.createToolBarItem("plugintest2");
					item2.setTextID("PluginToolBarTest.toolbar.text2");
					item2.setDefaultActivationListener(
							new DefaultActivationImpl(instance));
					item2.setImageID("image.vuze-entry.frog");
					tbm.addToolBarItem(item2);

					UIToolBarItem item3 = tbm.createToolBarItem("plugintest3");
					item3.setTextID("PluginToolBarTest.toolbar.text3");
					item3.setDefaultActivationListener(
							new DefaultActivationImpl(instance));
					item3.setImageID("image.vuze-entry.frog");
					tbm.addToolBarItem(item3);

					pi.getUIManager().addDataSourceListener(PluginToolBarTest.this, true);
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
		
		pi.getUIManager().removeDataSourceListener(this);
	}

	// @see org.gudy.azureus2.plugins.ui.UIDataSourceListener#dataSourceChanged(java.lang.Object)
	public void dataSourceChanged(Object datasource) {
		UIToolBarManager tbm = PluginToolBarTest.this.instance.getToolBarManager();
		if (tbm == null) {
			return;
		}
		tbm.getToolBarItem("plugintest").setState(UIToolBarItem.STATE_ENABLED);
		tbm.getToolBarItem("plugintest2").setState(UIToolBarItem.STATE_ENABLED);
		tbm.getToolBarItem("plugintest3").setState(UIToolBarItem.STATE_ENABLED);
	}

}
