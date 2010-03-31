/**
 * File: PathFormatterCache.java
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
package com.aelitis.azureus.plugins.azsavepath.parsers;

import com.aelitis.azureus.plugins.azsavepath.SavePathCore;
import com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter;
import com.aelitis.azureus.plugins.azsavepath.datasource.*;
import java.util.HashMap;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

/**
 * 
 */
public class PathFormatterCache {
	
	private HashMap cache;
	private SavePathCore core;
	
	public PathFormatterCache(SavePathCore core) {
		this.core = core;
		
		// We create these objects now and get them out of the way.
		this.cache = new HashMap();
		this.cache.put("${tracker}", new TrackerHostSource(core));
		
		TorrentAttribute ta = this.core.plugin_interface.getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
		this.cache.put("${category}", new DownloadPropertySource(core, ta));
		this.cache.put("${manual}", new DownloadPropertySource(core, core.custom_path_attr));
	}
	
	public PathFormatter get(String s) {
		PathFormatter pf = (PathFormatter)cache.get(s);
		if (pf != null) {return pf;}
		
		// At this point, we process the string to determine what it should contain.
		// (Nothing here yet!)
		
		// At this point - we just give up and return a default value.
		pf = new SourceStub(s);
		this.cache.put(s, new SourceStub(s));
		return pf;
	}
	
}
