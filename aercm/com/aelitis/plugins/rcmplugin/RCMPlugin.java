/*
 * Created on May 10, 2010
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


package com.aelitis.plugins.rcmplugin;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;


public class 
RCMPlugin 
	implements UnloadablePlugin
{
	static{
		COConfigurationManager.setParameter( "rcm.persist", true );
	}
	
	private RelatedContentUI		ui;
	
	private boolean					destroyed;
	
	public void
	initialize(
		final PluginInterface		plugin_interface )
	
		throws PluginException
	{
		if ( plugin_interface.getUtilities().compareVersions( plugin_interface.getAzureusVersion(), "4.4.0.5" ) < 0 ){
			
			throw( new PluginException( "Plugin requires Vuze version 4.4.0.5 or higher" ));
		}
		
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();

		loc_utils.integrateLocalisedMessageBundle( "com.aelitis.plugins.rcmplugin.internat.Messages" );
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void 
				UIAttached(
					UIInstance instance) 
				{
					String path = "com/aelitis/plugins/rcmplugin/skins/";
					
					String sFile = path + "skin3_rcm";
					
					ClassLoader loader = RCMPlugin.class.getClassLoader();
					
					SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
					
					try {
						ResourceBundle subBundle = ResourceBundle.getBundle(sFile,
								Locale.getDefault(), loader);
						skinProperties.addResourceBundle(subBundle, path, loader);
					} catch (MissingResourceException mre) {
						Debug.out(mre);
					}	
					
					synchronized( RCMPlugin.this ){
						
						if ( destroyed ){
							
							return;
						}
					
						ui = RelatedContentUI.getSingleton( plugin_interface );
					}
				}
				
				public void
				UIDetached(
					UIInstance		instance )
				{
					
				}
			});
	}
	
	public void 
	unload() 
	
		throws PluginException 
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed = true;
		}
		
		if ( ui != null ){
			
			ui.destroy();
			
			ui = null;
		}
	}
}
