/**
 * File: Profile.java
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
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

/**
 * 
 */
public class Profile {
	
	private LocaleUtilities lu;
	
	public Profile(SavePathCore core) {
		this.lu = core.plugin_interface.getUtilities().getLocaleUtilities();
	}
	
	public String name_key = null;
	public String name = null;
	public String value = null;
	public boolean customisable = false;
	public boolean match_on_null = false;
	
	public String getLabel() {
		if (this.name_key == null) {return this.name;}
		return this.lu.getLocalisedMessageText("azsavepath.profile." + this.name_key);
	}
	
	public String toString() {
		return "Profile [" + this.value + "]";
	}
	
}
