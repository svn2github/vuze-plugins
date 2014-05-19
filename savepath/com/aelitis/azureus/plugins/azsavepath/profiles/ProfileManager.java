/**
 * File: ProfileManager.java
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
package com.aelitis.azureus.plugins.azsavepath.profiles;

import com.aelitis.azureus.plugins.azsavepath.SavePathCore;

/**
 * 
 */
public class ProfileManager {
	
	private Profile[] profiles;
	
	public static final String MANUAL_PROFILE_TEMPLATE = "${manual}";
	public static final String CATEGORY_PROFILE_TEMPLATE = "${category}";
	public static final String TRACKER_PROFILE_TEMPLATE = "${tracker}";
	
public ProfileManager(SavePathCore core) {
		this.profiles = new Profile[4];
		
		int i = 0;
		Profile p = null;
		
		// No profile.
		p = new Profile(core);
		p.name_key = "profile_none";
		p.value = "";
		p.match_on_null = true;
		this.profiles[i++] = p;
		
		// Manual profile.
		p = new Profile(core);
		p.name_key = "profile_manual";
		p.value = MANUAL_PROFILE_TEMPLATE;
		p.customisable = true;
		this.profiles[i++] = p;
		
		// Category profile.
		p = new Profile(core);
		p.name_key = "profile_category";
		p.value = CATEGORY_PROFILE_TEMPLATE;
		this.profiles[i++] = p;
		
		// Tracker profile.
		p = new Profile(core);
		p.name_key = "profile_tracker";
		p.value = TRACKER_PROFILE_TEMPLATE;
		this.profiles[i++] = p;
	}
	
	public Profile[] getProfiles() {
		return this.profiles;
	}
	
	public String[][] getProfileSelectionData() {
		String[][] result = new String[2][this.profiles.length];
		for (int i=0; i<this.profiles.length; i++) {
			result[0][i] = this.profiles[i].value;
			result[1][i] = this.profiles[i].getLabel();
		}
		return result;
	}
	
	// XXX: This can be better done.
	public String getProfileDisplayName(String template) {
		for (int i=0; i<this.profiles.length; i++) {
			if (this.profiles[i].value.equals(template)) {
				return this.profiles[i].getLabel();
			}
		}
		// What's better - returning the template or an empty string?
		return template;
	}

}
