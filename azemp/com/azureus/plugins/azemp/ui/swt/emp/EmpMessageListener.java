/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.azureus.plugins.azemp.ui.swt.emp;

import java.util.Map;

import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Nov 19, 2007
 *
 * @note placed in swt package since AbstractMessageListener holds a
 *       ClientMessageContext, which links to SWT objects.. :( 
 */
public class EmpMessageListener
	extends AbstractBrowserMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "emp";

	public static final String OP_SET_WAIT_TIME = "allow-play";

	public static final String OP_SET_MIN_SIZE = "set-min-size";

	public static final String OP_PAGE_READY = "page-ready";

	public static final String OP_SET_BROWSER_HEIGHT = "set-browser-height";

	public static final String OP_SET_BROWSER_WIDTH = "set-browser-width";

	private final BrowserHandlerSWT bh;

	public EmpMessageListener(BrowserHandlerSWT bh) {
		super(DEFAULT_LISTENER_ID);
		this.bh = bh;
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener#handleMessage(com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage)
	public void handleMessage(BrowserMessage message) {
		String id = message.getOperationId();
		if (id == null) {
			throw new IllegalArgumentException("Unknown NULL operation for "
					+ DEFAULT_LISTENER_ID);
		} else if (id.equals(OP_SET_WAIT_TIME)) {
			Map decodedMap = message.getDecodedMap();
			boolean allowPlay = MapUtils.getMapBoolean(decodedMap, "allow", true);
			bh.setPrePlaybackWait(!allowPlay);
		} else if (id.equals(OP_SET_MIN_SIZE)) {
			Map decodedMap = message.getDecodedMap();
			int width = MapUtils.getMapInt(decodedMap, "width", -1);
			int height = MapUtils.getMapInt(decodedMap, "height", -1);
			if (width > 0 && height > 0) {
				bh.setMinSize(width, height);
			}
		} else if (id.equals(OP_PAGE_READY)) {
			bh.pageReady();
		} else if (id.equals(OP_SET_BROWSER_HEIGHT)) {
			Map decodedMap = message.getDecodedMap();
			int height = MapUtils.getMapInt(decodedMap, "height", 100);
			boolean top = MapUtils.getMapString(decodedMap, "location", "bottom").equals("top");
			bh.setHeight(height, top);
		} else if (id.equals(OP_SET_BROWSER_WIDTH)) {
			Map decodedMap = message.getDecodedMap();
			int width = MapUtils.getMapInt(decodedMap, "width", 100);
			boolean left = MapUtils.getMapString(decodedMap, "location", "right").equals("left");
			bh.setWidth(width, left);
		} else {
			throw new IllegalArgumentException("Unknown operation: " + id + " for "
					+ DEFAULT_LISTENER_ID);
		}
	}

}
