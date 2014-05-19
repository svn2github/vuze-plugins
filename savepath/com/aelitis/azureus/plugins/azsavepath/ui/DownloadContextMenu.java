/**
 * File: DownloadContextMenu.java
 * Library: Save Path plugin for Azureus
 * Date: 9 Nov 2006
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
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * 
 */
public class DownloadContextMenu {
	
	private SavePathCore core;
	
	public DownloadContextMenu(SavePathCore core) {
		this.core = core;
	}
	
	public void addMenus() {
		TableManager tm = core.plugin_interface.getUIManager().getTableManager();
		buildTables(tm, TableManager.TABLE_MYTORRENTS_COMPLETE);
		buildTables(tm, TableManager.TABLE_MYTORRENTS_INCOMPLETE);
	}
	
	private TableContextMenuItem buildTables(TableManager tm, String table_type) {
		final TableContextMenuItem root = tm.addContextMenuItem(table_type, "azsavepath.menu.context_menu_root");
		root.setStyle(TableContextMenuItem.STYLE_MENU);
		root.setVisible(true);
		
		Profile[] profiles = this.core.profile_manager.getProfiles();
		for (int i=0; i<profiles.length; i++) {
			addProfileOption(tm, root, profiles[i]);
		}
		
		return root;
	}
	
	private void addProfileOption(TableManager tm, TableContextMenuItem parent, Profile profile) {
		TableContextMenuItem tcmi;
		if (profile.name_key == null) {
			tcmi = tm.addContextMenuItem(parent, "blah");
			tcmi.setText(profile.name);
		}
		else {
			tcmi = tm.addContextMenuItem(parent, profile.name_key);
		}
		ProfileContextMenuListener pcml = null;
		tcmi.setStyle(TableContextMenuItem.STYLE_RADIO);
		
		/**
		 * The listeners should get invoked first, but I've seen on one occasion where
		 * the setData call on the listener doesn't get invoked (either logic bork or
		 *listener-call bork), so just set a default value here first.
		 */
		tcmi.setData(Boolean.valueOf(false));
		
		if (profile.customisable) {
			pcml = new InteractiveProfileContextMenuListener(core, profile);
		}
		else {
			pcml = new ProfileContextMenuListener(core, profile);
		}
		
		tcmi.addFillListener(pcml);
		tcmi.addMultiListener(pcml);
	}

}
