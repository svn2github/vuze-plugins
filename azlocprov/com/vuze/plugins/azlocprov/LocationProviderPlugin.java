/*
 * Created on Mar 28, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.vuze.plugins.azlocprov;

import java.io.File;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;

public class 
LocationProviderPlugin 
	implements UnloadablePlugin
{
	private PluginInterface		plugin_interface;
	
	private LocationProviderImpl	provider;
	
	public void 
	initialize(
		PluginInterface _pi )
	
		throws PluginException 
	{
		plugin_interface = _pi;
		
		provider = new LocationProviderImpl( plugin_interface.getPluginVersion(), new File( plugin_interface.getPluginDirectoryName()));
		
		plugin_interface.getUtilities().addLocationProvider( provider );
	}
	
	
	public void 
	unload() 
	
		throws PluginException 
	{
		if ( plugin_interface != null ){
			
			provider.destroy();
			
			plugin_interface.getUtilities().removeLocationProvider( provider );
			
			provider			= null;
			plugin_interface 	= null;
		}
	}
}
