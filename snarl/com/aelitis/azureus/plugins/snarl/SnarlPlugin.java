/**
 * File: SnarlPlugin.java
 * Library: snarl
 * Date: 1 Aug 2008
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
 */

package com.aelitis.azureus.plugins.snarl;

import java.io.File;

import at.dotti.snarl.Snarl4Java;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;

import org.gudy.azureus2.plugins.logging.*;

public class SnarlPlugin implements LogAlertListener, Plugin, PluginListener {

	private String appname = "";
	
	public void initialize(PluginInterface plugin_interface) throws PluginException {
		File dll_path = new File(plugin_interface.getPluginDirectoryName(), "snarl4java_0.1.1.dll");
		System.load(dll_path.getAbsolutePath());
			
		this.appname = plugin_interface.getApplicationName();
		Snarl4Java.snRegisterConfig(6880, this.appname, 0);
		plugin_interface.getLogger().addAlertListener(this);
		
		// Used to detect when we're shutting down and need to deregister.
		plugin_interface.addListener(this);
	}
	
	public void alertRaised(LogAlert alert) {
		Snarl4Java.snShowMessage(this.appname, alert.getPlainText(), alert.getTimeoutSecs(), "", 0, 0);
	}
	
	public void closedownInitiated() {
		Snarl4Java.snRevokeConfig(6880);
	}
	
	public void closedownComplete() {}	
	public void initializationComplete() {}

}
