/*
 * Created on Mar 25, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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


package com.vuze.plugins.coreinstaller;

import java.io.File;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.update.UpdateInstaller;
import org.gudy.azureus2.plugins.update.UpdateManager;

public class 
CoreInstallerPlugin
	implements UnloadablePlugin
{
	public void
	initialize(
		final PluginInterface		pi )
	{
		pi.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					File jar = new File( pi.getPluginDirectoryName(), "Azureus2.jar" );
					
					if ( jar.exists()){
						
						try{
							UpdateManager um = pi.getUpdateManager();
							
							UpdateInstaller installer = um.createInstaller();
							
							installer.addMoveAction( 
								jar.getAbsolutePath(),
								new File( pi.getUtilities().getAzureusProgramDir(), "Azureus2.jar" ).getAbsolutePath());
							
							um.applyUpdates( true );
							
						}catch( Throwable e ){
							
							pi.getLogger().getNullChannel("").logAlert( "Failed to apply update", e );
						}
					}
				}
				
				public void
				closedownInitiated()
				{
					
				}
				
				public void
				closedownComplete()
				{
					
				}
			});
	}
	
	public void
	unload()
	{
		
	}
}
