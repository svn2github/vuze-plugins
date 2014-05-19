/**
 * File: DownloadPropertySource.java
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
package com.aelitis.azureus.plugins.azsavepath.datasource;

import com.aelitis.azureus.plugins.azsavepath.SavePathCore;
import com.aelitis.azureus.plugins.azsavepath.formatters.AbstractPathSourceElement;
import com.aelitis.azureus.plugins.azsavepath.listeners.DLListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;


/**
 * 
 */
public class DownloadPropertySource extends AbstractPathSourceElement implements DLListener, DownloadAttributeListener {
	
	private TorrentAttribute ta;

	public DownloadPropertySource(SavePathCore core, TorrentAttribute ta) {
		super(core);
		this.ta = ta;
	}

	public Object getData(Download download) {
		return download.getAttribute(ta);
	}
	
	public void register(Download download) {
		download.addAttributeListener(this, ta, DownloadAttributeListener.WRITTEN);
	}

	public void unregister(Download download) {
		download.removeAttributeListener(this, ta, DownloadAttributeListener.WRITTEN);
	}
	
	public void attributeEventOccurred(Download download, TorrentAttribute ta, int type) {
		this.core.path_updater.recalculatePath(download);
	}
	
	public String toString() {
		return this.getClass().getName() + "[for " + ta.getName() + "]@" + System.identityHashCode(this);
	}
	
}
