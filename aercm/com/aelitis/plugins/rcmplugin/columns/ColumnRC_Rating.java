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


import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseMoveListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;


/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class 
ColumnRC_Rating
	implements TableCellRefreshListener, TableCellSWTPaintListener, TableCellMouseMoveListener
{	
	public static String COLUMN_ID = "rc_rating";
	
	private static IPCInterface			rating_ipc1;
	private static IPCInterface			rating_ipc2;
	private static DHTPlugin			dht_plugin;
	
	static{
		try{
			PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
					
			PluginInterface dht_pi = pm.getPluginInterfaceByClass( DHTPlugin.class );
			
			if ( dht_pi != null ){
				
				dht_plugin = (DHTPlugin)dht_pi.getPlugin();
			
				PluginInterface pi = pm.getPluginInterfaceByID( "azrating" );
				
				if ( pi != null ){
					
					IPCInterface ipc = pi.getIPC();
					
					if ( ipc.canInvoke( "lookupRatingByHash", new Object[]{ new byte[0] })){
						
						rating_ipc1 = ipc;
					}
					
					if ( ipc.canInvoke( "lookupRatingByHash", new Object[]{ new String[0], new byte[0] })){
						
						rating_ipc2 = ipc;
					}
				}
			}
		}catch( Throwable e ){
			
		}
	}
	
	private static ByteArrayHashMap<RatingsResult>	rating_lookups = new ByteArrayHashMap<RatingsResult>();
	
	private Color colorLinkNormal;
	private Color colorLinkHover;
	
	public 
	ColumnRC_Rating(
		TableColumn column )
	{
		column.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_LAST, 60 );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_GRAPHIC);

		if ( column instanceof TableColumnCore ){
			((TableColumnCore)column).setUseCoreDataSource( true );
			((TableColumnCore) column).addCellOtherListener("SWTPaint", this);
		}
		
		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = Colors.grey;
		colorLinkHover = skinProperties.getColor("color.links.hover");
	}

	public void 
	cellPaint(
		GC 				gc, 
		TableCellSWT 	cell ) 
	{
		String text = (String)cell.getSortValue();
		
		if ( text == null ){
			
			return;
		}

		if ( text != null && text.length() > 0 ){

			Rectangle bounds = getDrawBounds(cell);

			GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, true, SWT.WRAP | SWT.CENTER);

			sp.calculateMetrics();

			if (sp.hasHitUrl()){
				
				URLInfo[] hitUrlInfo = sp.getHitUrlInfo();
				for (int i = 0; i < hitUrlInfo.length; i++) {
					URLInfo info = hitUrlInfo[i];
					// handle fake row when showing in column editor

					info.urlUnderline = cell.getTableRow() == null
							|| cell.getTableRow().isSelected();
					if (info.urlUnderline) {
						info.urlColor = null;
					} else {
						info.urlColor = colorLinkNormal;
					}
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					Rectangle realBounds = cell.getBounds();
					URLInfo hitUrl = sp.getHitUrl(mouseOfs[0] + realBounds.x, mouseOfs[1]
							+ realBounds.y);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			sp.printString(GCStringPrinter.FLAG_FULLLINESONLY);
		}
	}
	
	public void cellMouseTrigger(TableCellMouseEvent event) {
		RelatedContent rc = (RelatedContent) event.cell.getDataSource();
		if (rc == null) {
			return;
		}

		boolean invalidateAndRefresh = event.eventType == event.EVENT_MOUSEEXIT;

		Rectangle bounds = ((TableCellSWT) event.cell).getBounds();
		String text = (String) event.cell.getSortValue();
		if (text == null) {
			return;
		}

		GCStringPrinter sp = null;
		GC gc = new GC(Display.getDefault());
		try {
			Rectangle drawBounds = getDrawBounds((TableCellSWT) event.cell);
			sp = new GCStringPrinter(gc, text, drawBounds, true, true, SWT.WRAP
					| SWT.CENTER);
			sp.calculateMetrics();
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			gc.dispose();
		}

		if (sp != null) {
			URLInfo hitUrl = sp.getHitUrl(event.x + bounds.x, event.y + bounds.y);
			int newCursor;
			if (hitUrl != null) {
				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP && event.button == 1) {
					if (hitUrl.url.equals("click")) {
						
						final byte[] 	hash 		= rc.getHash();
						final String[]	networks 	= rc.getNetworks();
						
						if ( !rating_lookups.containsKey( hash )){
							
							final RatingsResult res = new RatingsResult();
							
							rating_lookups.put( hash, res );
							
							new AEThread2( "rcm:rat:lookup" )
							{
								public void
								run()
								{
									try{
										Map result;
									
										if ( rating_ipc2 != null ){
											
											 result = (Map)rating_ipc2.invoke( "lookupRatingByHash", new Object[]{ networks, hash });

										}else{
											
											result = (Map)rating_ipc1.invoke( "lookupRatingByHash", new Object[]{ hash });
										}
										
										res.setResult( result );
										
									}catch( Throwable e ){
										
										res.setResult( null );
									}
									
									
								}
							}.start();
						}
					}
				}

				newCursor = SWT.CURSOR_HAND;
			} else {
				newCursor = SWT.CURSOR_ARROW;
			}

			int oldCursor = ((TableCellSWT) event.cell).getCursorID();
			if (oldCursor != newCursor) {
				invalidateAndRefresh = true;
				((TableCellSWT) event.cell).setCursorID(newCursor);
			}
		}

		if (invalidateAndRefresh) {
			event.cell.invalidate();
			((TableCellSWT)event.cell).redraw();
		}
	}
	
	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}
		
		String text;
		
		byte[] hash = rc.getHash();
		
		if ( hash == null ){
			
			text = "";
			
		}else if ( rating_ipc1 == null && rating_ipc2 == null ){
			
			text = MessageText.getString( "general.na.short" );
			
		}else{
			
			RatingsResult result = rating_lookups.get( hash );
			
			if ( result != null ){
				
				if ( result.isComplete()){
					
					Map data = result.getResult();
					
					List<Map> list = data==null?null:(List<Map>)data.get( "ratings" );

					if ( list == null || list.size() == 0 ){
						
						text = MessageText.getString( "PeersView.uniquepiece.none" );
						
					}else{
						
						float	total_score = 0;
						
						String tt = "";
						
						for ( Map m: list ){
							
							int		score	= ((Number)m.get( "score" )).intValue();
							
							if ( score < 1 ){
								score = 1;
							}else if ( score > 5 ){
								score = 5;
							}
							
							total_score += score;
							
							String	comment = ((String)m.get( "comment" )).trim();
							
							if ( comment.length() == 0 ){
								
								comment = MessageText.getString( "rcm.rating.nocomment" );
							}
							
							tt += "  " + score + ": " + comment + "\r\n"; 
						}
						
						double average = total_score / list.size();
						
						average = Math.floor( average * 10 )/10;
						
						text = String.valueOf( average );
						
						if ( text.endsWith( ".0")){
							
							text = text.substring( 0, text.length()-2 );
						}
						
						tt = MessageText.getString( "rcm.rating.summary", new String[]{ text }) + "\r\n" + tt;
						
						cell.setToolTip( tt );
					}
				}else{
					String old = (String)cell.getSortValue();
					
					if ( old.equals( "..." )){
						text = ".. ";
					}else if ( old.equals( ".. " )){
						text = ".  ";
					}else if ( old.equals( ".  " )){
						text = "  .";
					}else if ( old.equals( "  ." )){
						text = " ..";
					}else{
						text = "...";
					}
				}
				
			}else{
			
				if ( dht_plugin.isInitialising()){
									
					text = "<A HREF=\"init\">" + MessageText.getString( "rcm.rating.init" )  + "</A>";
					
				}else{
					
					text = "<A HREF=\"click\">" + MessageText.getString( "rcm.rating.click" )  + "</A>";
				}
			}
		}
		
		if (!cell.setSortValue(text) && cell.isValid()) {
			return;
		}
	}
	
	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();

		return bounds;
	}
	
	private class
	RatingsResult
	{
		private Map		result;
		private boolean	complete;
		
		private void
		setResult(
			Map		m )
		{
			result		= m;
			complete 	= true;
		}
		
		private boolean
		isComplete()
		{
			return( complete );
		}
		
		private Map
		getResult()
		{
			return( result );
		}
	}
}
