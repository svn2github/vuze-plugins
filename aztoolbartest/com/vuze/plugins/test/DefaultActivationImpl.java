package com.vuze.plugins.test;

import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;

import com.aelitis.azureus.ui.common.ToolBarItem;

public class DefaultActivationImpl
	implements UIToolBarActivationListener
{

	private final UIInstance uiInstance;

	public DefaultActivationImpl(UIInstance instance) {
		this.uiInstance = instance;
	}

	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		uiInstance.promptUser("PluginToolBarTest", "Item " + item.getID()
				+ " activated!", null, 0);

		// We handled the activation.. return true
		return true;
	}

}
