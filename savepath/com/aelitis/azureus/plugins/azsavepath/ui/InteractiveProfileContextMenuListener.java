/**
 * File: InteractiveProfileContextMenuListener.java
 * Library: Save Path plugin for Azureus
 * Date: 10 Nov 2006
 *
 * Author: Allan Crooks
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package com.aelitis.azureus.plugins.azsavepath.ui;

import com.aelitis.azureus.plugins.azsavepath.SavePathCore;
import com.aelitis.azureus.plugins.azsavepath.profiles.Profile;
import com.aelitis.azureus.plugins.azsavepath.profiles.ProfileManager;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInputReceiver;

/**
 * 
 */
public class InteractiveProfileContextMenuListener extends
		ProfileContextMenuListener implements UIManagerListener {
	
	private UIInstance ui = null;
	
	public InteractiveProfileContextMenuListener(SavePathCore core, Profile profile) {
		super(core, profile);
		core.plugin_interface.getUIManager().addUIListener(this);
	}
	
	public void UIAttached(UIInstance ui) {
		this.ui = ui;
	}
	
	public void UIDetached(UIInstance ui) {
		if (this.ui == ui) {this.ui = null;}
	}
		
	public void selected(MenuItem menu, Object target) {
		if (!((Boolean)menu.getData()).booleanValue()) {return;} // Not selected.
		//if (!menu.isSelected()) {return;}
		if (this.ui == null) {return;}
		
		// We'll grab the first download because we want to get the previous
		// value.
		Object[] targets = (Object[])target;
		if (targets.length == 0) {return;}
		
		UIInputReceiver ui_input = this.ui.getInputReceiver();		
		if (ui_input == null) {return;}
		
		UISWTInputReceiver ui_swt_input = null;
		if (ui_input instanceof UISWTInputReceiver) {
			ui_swt_input = (UISWTInputReceiver)ui_input;
		}
		
		Download first_download = (Download)((TableRow)targets[0]).getDataSource();
		
		ui_input.allowEmptyInput(false);
		ui_input.setMultiLine(false);
		ui_input.setTitle("azsavepath.menu.manual.title");
		ui_input.setPreenteredText(first_download.getAttribute(core.custom_path_attr), false);

		if (ui_swt_input != null) {
			ui_swt_input.selectPreenteredText(false);
		}
		
		ui_input.prompt();
		if (!ui_input.hasSubmittedInput()) {return;}
		
		String result = ui_input.getSubmittedInput();

		Download download;
		for (int i=0; i<targets.length; i++) {
			download = (Download)((TableRow)targets[i]).getDataSource();
			
			/**
			 * Change it in this order - we'll adjust the manual attribute first. If it's
			 * already set to manual, then the change will take place on the first line.
			 * 
			 * If it's not already using the manual attribute, nothing will happen. The
			 * setting will be changed on the second line.
			 */
			// 
			download.setAttribute(core.custom_path_attr, result);
			core.path_updater.updatePathTemplate(download, ProfileManager.MANUAL_PROFILE_TEMPLATE);
		}
	}

}
