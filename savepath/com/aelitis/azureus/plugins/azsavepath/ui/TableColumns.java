/**
 * File: TableColumns.java
 * Library: Save Path plugin for Azureus
 * Date: 10 Nov 2006
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
package com.aelitis.azureus.plugins.azsavepath.ui;

import com.aelitis.azureus.plugins.azsavepath.SavePathCore;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * 
 */
public class TableColumns {
	
	private SavePathCore core;
	private TableColumn[] columns;
	
	public TableColumns(SavePathCore core) {
		this.core = core;
	}
	
	public void createColumns() {
		TableManager tm = core.plugin_interface.getUIManager().getTableManager();
		
		this.columns = new TableColumn[4];
		TableColumn tc = null;
		
		tc = tm.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, "save_path");
		populateSPColumn(tc);
		tm.addColumn(tc);
		this.columns[0] = tc;

		tc = tm.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "save_path");
		populateSPColumn(tc);
		tm.addColumn(tc);
		this.columns[1] = tc;
		
		tc = tm.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, "profile_name");
		populateProfileColumn(tc);
		tm.addColumn(tc);
		this.columns[2] = tc;
		
		tc = tm.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "profile_name");
		populateProfileColumn(tc);
		tm.addColumn(tc);
		this.columns[3] = tc;
	}
	
	public void invalidateColumns(Download d) {
		for (int i=0; i<this.columns.length; i++) {
			this.columns[i].invalidateCell(d);
		}
	}
	
	private void populateSPColumn(TableColumn tc) {
		tc.setWidth(150);
		tc.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		tc.setRefreshInterval(2);
		TableCellRefreshListener tcrl = new TableCellRefreshListener() {
			public void refresh(TableCell tc) {
				Download dl = ((Download)tc.getDataSource());
				String text = dl.getAttribute(core.path_attr);
				if (text == null) {text = "";}
				tc.setText(text);
			}
		};
		tc.addCellRefreshListener(tcrl);
	}
	
	private void populateProfileColumn(TableColumn tc) {
		tc.setWidth(50);
		tc.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		TableCellRefreshListener tcrl = new TableCellRefreshListener() {
			public void refresh(TableCell tc) {
				Download dl = ((Download)tc.getDataSource());
				String text = dl.getAttribute(core.template_attr);
				text = core.profile_manager.getProfileDisplayName(text);
				tc.setText(text);
			}
		};
		tc.addCellRefreshListener(tcrl);

	}

}
