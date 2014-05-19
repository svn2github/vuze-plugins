/**
 * File: ProfileContextMenuListener.java
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
import com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter;
import com.aelitis.azureus.plugins.azsavepath.profiles.Profile;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableRow;

/**
 * 
 */
public class ProfileContextMenuListener implements MenuItemFillListener,
		MenuItemListener {
	
	protected SavePathCore core;
	protected Profile profile;
	
	public ProfileContextMenuListener(SavePathCore core, Profile profile) {
		this.core = core;
		this.profile = profile;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener#menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem, java.lang.Object)
	 */
	public void menuWillBeShown(MenuItem menu, Object data) {
		TableRow[] rows = (TableRow[])data;
		PathFormatter pf = core.path_formatter_cache.get(this.profile.value);
		
		Download dl = null;
		boolean has_set_value = false;
		String current_fmt = null;
		String this_fmt = null;

		for (int i=0; i<rows.length; i++) {
			dl = (Download)rows[i].getDataSource();
			if (dl == null) {continue;}
			this_fmt = pf.format(dl);
			
			// This item is null - means that we aren't going to
			// display any item in particular.
			if (this_fmt == null) {
				current_fmt = null;
				break;
			}
			
			// First item, so we just set it and carry on.
			if (!has_set_value) {
				has_set_value = true;
				current_fmt = this_fmt;
				continue;
			}
			
			// current_fmt should not be null.
			if (!current_fmt.equals(this_fmt)) {
				current_fmt = null;
				break;
			}
			
			// current_fmt === this_fmt, so carry on.
			
		}
		
		boolean is_common_template = true;
		String current_template = null; // temporary pointer.
		for (int i=0; i<rows.length; i++) {
			dl = (Download)rows[i].getDataSource();
			if (dl != null) {
				current_template = dl.getAttribute(core.template_attr);
				if (current_template != null) {
					if (profile.value.equals(current_template)) {
						continue; // This value is fine.
					}
				}
				// If the profile "matches" when there is no explicit template attribute
				// stored, then that's fine too.
				else if (profile.match_on_null) {
					continue;
				}
			}
			
			// If we didn't reach the continue statement, then it doesn't match.
			is_common_template = false;
			break;
		}
		
		setupMenuItem(menu, current_fmt, is_common_template);
	}
	
	protected void setupMenuItem(MenuItem menu, String common_data, boolean common_selected) {
		String label_to_show = this.profile.getLabel();
		if (common_data != null) {label_to_show += " - " + common_data;}
		menu.setText(label_to_show);
		menu.setData(Boolean.valueOf(common_selected));
	}

	public void selected(MenuItem menu, Object target) {
		if (!((Boolean)menu.getData()).booleanValue()) {return;} // Not selected.
		//if (!menu.isSelected()) {return;}
		Object[] objects = (Object[])target;
		for (int i=0; i<objects.length; i++) {
			Download download = (Download)((TableRow)objects[i]).getDataSource();
			core.path_updater.updatePathTemplate(download, this.profile.value);	
		}
	}
	

}
