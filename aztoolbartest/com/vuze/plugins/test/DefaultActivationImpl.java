package com.vuze.plugins.test;

import java.util.Arrays;

import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;

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
				+ " activated!\n\n"
				+ (datasource == null ? "null" : datasource.getClass().isArray()
						? Arrays.toString((Object[]) datasource)
						: datasource.getClass().getSimpleName()), null, 0);

		if (activationType == ACTIVATIONTYPE_HELD) {
			UIToolBarManager tbm = uiInstance.getToolBarManager();
			tbm.removeToolBarItem(item.getID());
		}
		// We handled the activation.. return true
		return true;
	}
	

}
