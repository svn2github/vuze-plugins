/**
 * File: PathUpdater.java
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
package com.aelitis.azureus.plugins.azsavepath;

import com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter;
import com.aelitis.azureus.plugins.azsavepath.listeners.DLListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationChange;

/**
 * Class which will update attributes appropriately when either the path template
 * or some of the source values for the path are changed.
 */
public class PathUpdater {
	
	private SavePathCore core;
	
	public PathUpdater(SavePathCore core) {
		this.core = core;
	}
	
	public void recalculatePath(Download download) {
		String template = download.getAttribute(core.template_attr);
		PathFormatter pf = this.core.path_formatter_cache.get(template);
		String new_save_path = pf.formatAsFilePath(download);
		
		// Only bother resetting if the value is different.
		String old_save_path = download.getAttribute(core.path_attr);
		if (new_save_path.equals(old_save_path)) {return;}
		download.setAttribute(core.path_attr, new_save_path);
		this.core.table_columns.invalidateColumns(download);
		
		// Most users want the download path to be updated immediately.
		// So move it if we can.
		if (download.canMoveDataFiles()) {
			SaveLocationChange slc = download.calculateDefaultDownloadLocation();
			try {
				if (slc != null) {download.changeLocation(slc);}
			}
			catch (DownloadException de) {
				core.logger_channel.log(de);
			}
		}
	}
	
	public void updatePathTemplate(Download download, String template) {
		String old_fmt = download.getAttribute(core.template_attr);
		if (template.equals(old_fmt)) {return;}
		PathFormatter old_pf = this.core.path_formatter_cache.get(old_fmt);
		PathFormatter new_pf = this.core.path_formatter_cache.get(template);

        // Value -> True (add), False (remove)
		HashMap changes = new HashMap();
		DLListener[] listeners = old_pf.getListeners();
		for (int i=0; i<listeners.length; i++) {
		    changes.put(listeners[i], Boolean.FALSE);
		}

		Object old_value = null;
		listeners = new_pf.getListeners();
		for (int i=0; i<listeners.length; i++) {
		    old_value = changes.remove(listeners[i]);
		    if (old_value == null) {
		        changes.put(listeners[i], Boolean.TRUE);
		    }
		}
		
		Iterator itr = changes.entrySet().iterator();
		Map.Entry me = null;
		while (itr.hasNext()) {
			me = (Map.Entry)itr.next();
			if (me.getValue() == Boolean.TRUE) {
				((DLListener)me.getKey()).register(download);
			}
			else if (me.getValue() == Boolean.FALSE) {
				((DLListener)me.getKey()).unregister(download);
			}
		}
		
		download.setAttribute(core.template_attr, template);
		this.core.table_columns.invalidateColumns(download);
		this.recalculatePath(download);
	}

}
