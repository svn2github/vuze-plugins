/*
 * Created on 10 Oct 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.upnpmediaserver.ui.swt;

import java.io.File;
import java.io.IOException;

import org.gudy.azureus2.platform.win32.access.AEWin32Access;
import org.gudy.azureus2.platform.win32.access.AEWin32Manager;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.plugins.upnpmediaserver.ui.UPnPMediaServerUI;

public class 
UPnPMediaServerUISWT 
	implements UPnPMediaServerUI
{
	
	public void
	play(
		File		file )
	{
		Utils.launch(file.getAbsolutePath());
	}

	public boolean
	runInMediaPlayer(
		String mediaFile, 
		boolean fallbackToPlay )
	{
		if (org.gudy.azureus2.core3.util.Constants.isWindows) {
			String wmpEXE = getWMP();
			if (new File(wmpEXE).exists()) {
				try {
					Runtime.getRuntime().exec(wmpEXE + " \"" + mediaFile + "\"");
					return true;
				} catch (IOException e) {
				}
			}
		}
		
		if (fallbackToPlay) {
			play(new File(mediaFile));
		}
		return false;
	}
	
	private static 
	String getWMP() 
	{
		AEWin32Access accessor = AEWin32Manager.getAccessor(true);
		if (accessor == null) {
			return null;
		}
		try {
			return accessor.readStringValue(AEWin32Access.HKEY_LOCAL_MACHINE,
					"SOFTWARE\\Microsoft\\Multimedia\\WMPlayer", "Player.Path");
		} catch (Exception e) {
		}
		return null;
	}
}
