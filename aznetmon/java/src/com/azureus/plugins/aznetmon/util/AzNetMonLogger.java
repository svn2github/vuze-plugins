/**
 * Created on Mar 20, 2008
 * Created by Alan Snyder
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

package com.azureus.plugins.aznetmon.util;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.azureus.plugins.aznetmon.pcap.util.NetMon16Util;

public class AzNetMonLogger {

	private static AzNetMonLogger ourInstance = null;
	private static boolean isInit = false;

	private static final String ID = "aznetmon";
	private static final String MSG_PREFIX = "v3.netmon";

	private final LoggerChannel logger;
	

	public synchronized static AzNetMonLogger init(PluginInterface pi) {

		if( ourInstance==null ){
			ourInstance = new AzNetMonLogger(pi);

			ourInstance.registerListener();

			isInit=true;
		}

		return ourInstance;
	}

	public synchronized static AzNetMonLogger getInstance(){

		if(ourInstance==null){
			throw new IllegalStateException("must call init() first!");
		}
		return ourInstance;
	}


	private AzNetMonLogger( PluginInterface pi ) {
		logger = pi.getLogger().getTimeStampedChannel(ID);
	}


	public static LoggerChannel getLogger(){

		AzNetMonLogger inst = getInstance();
		return inst.logger;
	}//getLogger

	/**
	 *
	 * @param msg -
	 */
	public void log(String msg){

		if( isInit ){
			logger.log(msg);
		}

		if( NetMon16Util.useSysout ){
			NetMon16Util.debug(msg);
		}

	}//log

	/**
	 * log an error.
	 * @param msg -
	 * @param t -
	 */
	public void log(String msg, Throwable t){
		String message = msg+" for: "+t.getMessage();
		log( message );
	}


	public void registerListener(){

		logger.addListener(new LoggerChannelListener() {

			public void messageLogged(int type, String content) {
//				viewModel.getLogArea().appendText(content + "\n");
			}

			public void messageLogged(String str, Throwable error) {
				if (str.length() > 0) {
//					viewModel.getLogArea().appendText(str + "\n");
				}

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				error.printStackTrace(pw);
				pw.flush();
//				viewModel.getLogArea().appendText(sw.toString() + "\n");
			}
		});

	}//registerListener

	/**
	 * addListener
	 * @param listener -
	 */
	public void addListener(LoggerChannelListener listener){

		logger.addListener(listener);

	}//addListener

}//class
