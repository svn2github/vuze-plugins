/**
 * Copyright (C) 2008 Vuze Inc., All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.plugins.rcmplugin.columns;


import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.ui.common.table.TableColumnCore;



public class 
ColumnRC_Networks
	implements TableCellRefreshListener
{	
	public static String COLUMN_ID = "rc_networks";
	
	public 
	ColumnRC_Networks(
		TableColumn column )
	{
		column.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_INVISIBLE, 60 );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);

		if ( column instanceof TableColumnCore ){
			((TableColumnCore)column).setUseCoreDataSource( true );
		}
	}

	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}

		String[] nets = rc.getNetworks();

		String nets_str = "";
		
		if ( nets != null && nets.length > 0 ){
			
			for ( int i=0;i< nets.length;i++){
				
				nets_str += (i==0?"":",") + nets[i];
			}
		}
	
		cell.setText( nets_str );
	}
}
