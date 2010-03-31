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

package com.azureus.plugins.azemp;

import java.util.ArrayList;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

/**
 * @author TuxPaper
 * @created Sep 4, 2007
 *
 */
public class EmbeddedPlayerWindowManager
{
	private static ArrayList list = new ArrayList();
	
	private static String pluginDir;

	private static LoggerChannel logger;
	
	private static PluginInterface pluginInterface;
	
	private static String pluginVersion;

	public static void add(EmbeddedPlayerWindowManager epw) {
		if (!list.contains(epw)) {
			list.add(epw);
		}
	}
	
	public static void remove(EmbeddedPlayerWindowManager epw) {
		list.remove(epw);
	}
	
	public static void killAll() {
		Object[] listArray = list.toArray();
		list.clear();

		for (int i = 0; i < listArray.length; i++) {
			EmbeddedPlayerWindow epw = (EmbeddedPlayerWindow) listArray[i];
			epw.close();
		}
	}

	public static String getPluginDir() {
		return pluginDir;
	}

	public static void setPluginDir(String pluginDir) {
		EmbeddedPlayerWindowManager.pluginDir = pluginDir;
	}

	public static LoggerChannel getLogger() {
		return logger;
	}

	public static void setLogger(LoggerChannel logger) {
		EmbeddedPlayerWindowManager.logger = logger;
	}

	public static PluginInterface getPluginInterface() {
		return pluginInterface;
	}

	public static void setPluginInterface(PluginInterface pluginInterface) {
		EmbeddedPlayerWindowManager.pluginInterface = pluginInterface;
	}
	
	public static String getPluginVersion() {
		if (pluginVersion == null) {
			if (pluginInterface != null) {
				pluginVersion = pluginInterface.getPluginVersion();
			}
		}
		return pluginVersion == null ? "" : pluginVersion;
	}
}
