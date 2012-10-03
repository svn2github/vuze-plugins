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
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;


public class 
RCMPlugin 
	implements UnloadablePlugin
{
	static{
		COConfigurationManager.setParameter( "rcm.persist", true );
	}
	
	private PluginInterface			plugin_interface;
	
	private RelatedContentUI		ui;
	private SearchProvider 			search_provider;

	private boolean					destroyed;
	
	public void
	initialize(
		final PluginInterface		_plugin_interface )
	
		throws PluginException
	{
		plugin_interface = _plugin_interface;
				
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();

		loc_utils.integrateLocalisedMessageBundle( "com.aelitis.plugins.rcmplugin.internat.Messages" );

		hookSearch();
		
		updatePluginInfo();

		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void 
				UIAttached(
					UIInstance instance) 
				{
					if ( instance instanceof UISWTInstance ){
						
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

							ui = RelatedContentUI.getSingleton( plugin_interface, RCMPlugin.this );
						}
					}
				}
				
				public void
				UIDetached(
					UIInstance		instance )
				{
					
				}
			});
	}
	
	protected void
	updatePluginInfo()
	{
		String plugin_info;
		
		if ( !hasFTUXBeenShown()){
			
			plugin_info = "f";
			
		}else if ( isRCMEnabled()){
		
			plugin_info = "e";
			
		}else{
			
			plugin_info = "d";
		}
		
		PluginConfig pc = plugin_interface.getPluginconfig();
		
		if ( !pc.getPluginStringParameter( "plugin.info", "" ).equals( plugin_info )){
			
			pc.setPluginParameter( "plugin.info", plugin_info );
		
			COConfigurationManager.save();
		}
	}
	
	protected boolean
	isRCMEnabled()
	{
		return( COConfigurationManager.getBooleanParameter( "rcm.overall.enabled", true ));
	}
	
	protected boolean
	setRCMEnabled(
		boolean	enabled )
	{
		if ( isRCMEnabled() != enabled ){
			
			COConfigurationManager.setParameter( "rcm.overall.enabled", enabled );
			return true;
		}
		
		return false;
	}
	
	protected boolean
	hasFTUXBeenShown()
	{
		return( plugin_interface.getPluginconfig().getPluginBooleanParameter( "rcm.ftux.shown", false ));
	}

	protected void
	setFTUXBeenShown(
			boolean b )
	{
		plugin_interface.getPluginconfig().setPluginParameter( "rcm.ftux.shown", b );
	}
	
	protected boolean
	isSearchEnabled()
	{
		return( plugin_interface.getPluginconfig().getPluginBooleanParameter( "rcm.search.enable", false ));
	}
	
	protected void
	hookSearch()
	{
		boolean enable = isRCMEnabled() && isSearchEnabled() && hasFTUXBeenShown();
		
		try{
				
			if ( enable ){
			
				if ( search_provider == null ){
					
					search_provider = new RCM_SearchProvider();
						
					plugin_interface.getUtilities().registerSearchProvider( search_provider );
				}
			}else{
				
				if ( search_provider != null ) {
					
					plugin_interface.getUtilities().unregisterSearchProvider( search_provider );
					
					search_provider = null;
				}
			}
		}catch( Throwable e ){
			
			Debug.out( "Failed to register/unregister search provider", e );
		}
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
		
		if ( search_provider != null ){

			try{
				plugin_interface.getUtilities().unregisterSearchProvider( search_provider );

				search_provider = null;
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
}
