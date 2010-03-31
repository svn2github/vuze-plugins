/**
 * File: StringSource.java
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
import com.aelitis.azureus.plugins.azsavepath.SavePathCore;
import com.aelitis.azureus.plugins.azsavepath.formatters.AbstractPathSourceElement;

/**
 * 
 */
public class StringSource extends AbstractPathSourceElement {
	
	private String data;
	
	public StringSource(SavePathCore core, String data) {
		super(core);
		this.data = data;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.plugins.azsavepath.formatters.AbstractPathSourceElement#getData(org.gudy.azureus2.plugins.download.Download)
	 */
	public Object getData(Download download) {
		return this.data;
	}
	
}
