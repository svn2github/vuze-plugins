/*
 * AMP - Azureus Monitoring Plugin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.pmm.monitoringplugin;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.pmm.monitoringplugin.graphic.GraphicView;
import org.pmm.monitoringplugin.text.AMPView;

/**
 * @author kustos
 * Allows Azureus to create an instance of the Azureus Monitoring Plugin
 */
public class AMPPlugin implements Plugin, UIManagerListener {
    
    /**
     * Default constructor.
     * Doesn't do anything. Here to avoid problems.
     */
    public AMPPlugin() {super();}
    
    private PluginInterface pluginInterface;

    /**
     * This method is called when the Plugin is loaded by Azureus
     * @param pluginInterface the interface that the plugin must use to communicate with Azureus
     * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
     */
    public void initialize(PluginInterface pluginInterface) throws PluginException {
    	this.pluginInterface = pluginInterface;
    	pluginInterface.getUIManager().addUIListener(this);
    }
    
    public void UIAttached(UIInstance ui) {
    	if (!(ui instanceof UISWTInstance)) {return;}
    	UISWTInstance swt_ui = (UISWTInstance)ui;
    	swt_ui.addView(UISWTInstance.VIEW_MAIN, "MonitoringAMP", new AMPView(pluginInterface));
    	swt_ui.addView(UISWTInstance.VIEW_MAIN, "MonitoringGraphic", new GraphicView(pluginInterface, swt_ui));
    }
    public void UIDetached(UIInstance ui) {}
}
