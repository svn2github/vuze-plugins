package com.azureus.plugins.aznetmon.pcap.util;

import org.gudy.azureus2.plugins.logging.LoggerChannel;
import com.azureus.plugins.aznetmon.util.AzNetMonLogger;
import jpcap.NetworkInterface;
import jpcap.JpcapCaptor;

/**
 * Created on Mar 13, 2008
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

public class NetMon16Util {

	public static boolean useSysout=true;

	private NetMon16Util(){}


	/**
	 *
	 */
	public static boolean hasJPcap()
	{
		try{
			debug( "hasJPcap() - before getDeviceList()" );
			NetworkInterface[] devices = JpcapCaptor.getDeviceList();
			debug(" after ");
		}catch(Throwable t){
			
			return false;
		}
		//We seem to have the libraries.
		return true;
	}//hasJPcap

	/**
	 * for now print out results.
	 * @param msg -
	 */
	public static void debug(String msg){
		if( useSysout ){

			LoggerChannel logger = AzNetMonLogger.getLogger();
			System.out.println(msg);
			logger.log(msg);

		}
	}

}
