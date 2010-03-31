/**
 * File: AbstractPathSourceElement.java
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

import com.aelitis.azureus.plugins.azsavepath.SavePathCore;
import com.aelitis.azureus.plugins.azsavepath.listeners.DLListener;
import org.gudy.azureus2.plugins.download.Download;

/**
 * 
 */
public abstract class AbstractPathSourceElement implements PathFormatter {
	
	private DLListener[] dl_listeners;
	protected SavePathCore core;
	
	protected AbstractPathSourceElement(SavePathCore core) {
		this.core = core;
		if (this instanceof DLListener) {
			this.dl_listeners = new DLListener[] {(DLListener)this};
		}
		else {
			this.dl_listeners = new DLListener[0];
		}
	}
	
	public String formatAsFilePath(Download download) {
		String result = this.format(download);
		return (result == null) ? "" : core.plugin_interface.getUtilities().normaliseFileName(result);
	}

	public String format(Download download) {
		Object result = this.getData(download);
		return (result == null) ? null : String.valueOf(result);
	}

	public abstract Object getData(Download download);

	public DLListener[] getListeners() {
		return this.dl_listeners;
	}

}
