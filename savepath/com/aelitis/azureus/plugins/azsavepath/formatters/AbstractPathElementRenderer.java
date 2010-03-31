/**
 * File: AbstractPathElementRenderer.java
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
package com.aelitis.azureus.plugins.azsavepath.formatters;

import org.gudy.azureus2.plugins.download.Download;

import com.aelitis.azureus.plugins.azsavepath.listeners.DLListener;

// Unused class - intended to take another object which formats an object, and
// modifies it so that it can format it in a slightly different way (think about
// defining a path formatter which uses the date of completion, and then other
// path formatters which just format that the default path formatter differently.)

/**
 * 
 */
public abstract class AbstractPathElementRenderer implements PathFormatter {
	
	protected PathFormatter pf;
	
	protected AbstractPathElementRenderer(PathFormatter pf) {
		this.pf = pf;
	}
	
	protected abstract String formatElement(Object element);

	public String format(Download download) {
		return this.formatElement(this.getData(download));
	}

	public Object getData(Download download) {
		return this.pf.getData(download);
	}

	public DLListener[] getListeners() {
		return this.pf.getListeners();
	}

}
