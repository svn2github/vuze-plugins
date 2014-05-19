/**
 * File: TrackerHostSource.java
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
import java.net.URL;
import org.gudy.azureus2.plugins.download.Download;

/**
 * 
 */
public class TrackerHostSource extends AbstractPathSourceElement {
	
	public TrackerHostSource(SavePathCore core) {
		super(core);
	}
	
	public String format(Download download) {
		URL url = (URL)getData(download);
		if (url == null) {return null;}
		if (url.getProtocol().equals("dht")) {return "DHT";}
		return url.getHost();
	}

	public Object getData(Download download) {
		return download.getTorrent().getAnnounceURL();
	}
	
}
