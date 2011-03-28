package com.vuze.plugin.btapp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;

public class ColumnAppListAction
	implements TableCellSWTPaintListener, TableCellRefreshListener,
	TableCellMouseMoveListener, TableCellAddedListener
{
	private Color colorLinkNormal;

	private Color colorLinkHover;

	private static Font font = null;

	public ColumnAppListAction() {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
				colorLinkNormal = skinProperties.getColor("color.links.normal");
				colorLinkHover = skinProperties.getColor("color.links.hover");
			}
		});
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		Object ds = cell.getDataSource();
		if (!(ds instanceof BtAppDataSource)) {
			return;
		}
		
		String text = (String) cell.getTableRow().getData("text");

		if (text != null && text.length() > 0) {
			if (font == null) {
				FontData[] fontData = gc.getFont().getFontData();
				fontData[0].setStyle(SWT.BOLD);
				font = new Font(gc.getDevice(), fontData);
			}
			gc.setFont(font);

			Rectangle bounds = getDrawBounds(cell);

			GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, true,
					SWT.WRAP | SWT.CENTER);

			sp.calculateMetrics();

			if (sp.hasHitUrl()) {
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

			sp.printString();
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		Object ds = cell.getDataSource();
		if (!(ds instanceof BtAppDataSource)) {
			return;
		}
		BtAppDataSource appInfo = (BtAppDataSource) ds;

		String ourAppId = appInfo.getOurAppId();
		boolean isInstalled = Plugin.isAppInstalled(ourAppId);
		boolean isInstalling = Plugin.isAppInstalling(ourAppId);
		
		int sortVal = (isInstalled ? 1 : 0) + (isInstalling ? 2 : 0);

		if (!cell.setSortValue(sortVal) && cell.isValid()) {
			return;
		}

		StringBuffer text = new StringBuffer();
		
		LocaleUtilities localeUtilities = Plugin.pi.getUtilities().getLocaleUtilities();
		if (isInstalling) {
			text.append(localeUtilities.getLocalisedMessageText("BtAppList.action.installing"));
		} else if (isInstalled) {
			text.append("<A HREF=\"open\">");
			text.append(localeUtilities.getLocalisedMessageText("BtAppList.action.open"));
			text.append("</A>");
			text.append(" | ");
			text.append("<A HREF=\"uninstall\">");
			text.append(localeUtilities.getLocalisedMessageText("BtAppList.action.uninstall"));
			text.append("</A>");
		} else {
			text.append("<A HREF=\"install\">");
			text.append(localeUtilities.getLocalisedMessageText("BtAppList.action.install"));
			text.append("</A>");
		}
		
		cell.getTableRow().setData("text", text.toString());
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		Object ds = event.cell.getDataSource();
		if (!(ds instanceof BtAppDataSource)) {
			return;
		}
		final BtAppDataSource appInfo = (BtAppDataSource) ds;

		boolean invalidateAndRefresh = false;

		Rectangle bounds = ((TableCellSWT) event.cell).getBounds();
		String text = (String) event.cell.getTableRow().getData("text");
		if (text == null) {
			return;
		}

		GCStringPrinter sp = null;
		GC gc = new GC(Display.getDefault());
		try {
			if (font != null) {
				gc.setFont(font);
			}
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
				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
					if (hitUrl.url.equals("install")) {
						Plugin.installApp(appInfo.getOurAppId(), appInfo.getProperty("btapp_url"));
					} else if (hitUrl.url.equals("uninstall")) {
						Plugin.uninstallApp(appInfo.getOurAppId());
					} else if (hitUrl.url.equals("open")) {
						Plugin.openApp(appInfo.getOurAppId());
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
			((TableCellSWT) event.cell).redraw();
		}
	}

	boolean bMouseDowned = false;

	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();
		bounds.height -= 12;
		bounds.y += 6;
		bounds.x += 4;
		bounds.width -= 4;

		return bounds;
	}
}
