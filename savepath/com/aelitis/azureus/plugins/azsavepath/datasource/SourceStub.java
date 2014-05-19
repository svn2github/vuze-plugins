/**
 * File: SourceStub.java
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

import org.gudy.azureus2.plugins.download.Download;

import com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter;
import com.aelitis.azureus.plugins.azsavepath.listeners.DLListener;

/**
 * 
 */
public class SourceStub implements PathFormatter {
	
	private String name;
	
	public SourceStub(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter#format(org.gudy.azureus2.plugins.download.Download)
	 */
	public String format(Download download) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter#formatAsFilePath(org.gudy.azureus2.plugins.download.Download)
	 */
	public String formatAsFilePath(Download download) {
		return "";
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter#getData(org.gudy.azureus2.plugins.download.Download)
	 */
	public Object getData(Download download) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.plugins.azsavepath.formatters.PathFormatter#getListeners()
	 */
	public DLListener[] getListeners() {
		return new DLListener[0];
	}

}
