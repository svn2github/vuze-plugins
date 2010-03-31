package com.azureus.plugins.aznetmon.util;

import org.gudy.azureus2.plugins.logging.LoggerChannel;


/**
 * Created on Apr 4, 2008
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class AzNetMonUtil
{
	/**
	 * Test if running under Java 1.6 JVM or greater.
	 * @return - true - if a JVM 1.6 or greater.
	 */
	public static boolean canRunJava16()
	{

		LoggerChannel logger = AzNetMonLogger.getLogger();

		String jVersion = System.getProperty("java.version");
		logger.log("java version: "+jVersion);

		String[] items = jVersion.split("\\.");

		if( items!=null && items.length>2 ){

			int ver = Integer.parseInt(items[1]);

			if( ver>=6 ){
				logger.log("Java 1.6 detected, running advanced features");
				return true;
			}

		}
			logger.log("Java 1.6 was not detected. Will not run advanced features.");

		return false;
	}//canRunJava16

	public static boolean isNotVista()
	{

		//LoggerChannel logger = AzNetMonLogger.getLogger();

		String osName = System.getProperty("os.name");
		String osVers = System.getProperty("os.version");

		//logger.log("os-name: "+osName);
		//alogger.log("os-version: "+osVers);

		//ToDo: implement
		//Get the testing results.
		return true;
	}

}
