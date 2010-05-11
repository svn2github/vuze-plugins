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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInstance;
import org.gudy.azureus2.plugins.utils.search.SearchObserver;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;

import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.net.magneturi.MagnetURIHandler;

public class 
RCMPlugin 
	implements Plugin
{
	public void
	initialize(
		PluginInterface		plugin_interface )
	
		throws PluginException
	{
		if ( plugin_interface.getUtilities().compareVersions( plugin_interface.getAzureusVersion(), "4.4.0.5" ) < 0 ){
			
			throw( new PluginException( "Plugin requires Vuze version 4.4.0.5 or higher" ));
		}
		
		
		try{
			plugin_interface.getUtilities().registerSearchProvider(
				new SearchProvider()
				{
					private Map<Integer,Object>	properties = new HashMap<Integer, Object>();
					
					{
						properties.put( PR_NAME, MessageText.getString( "rcm.search.provider" ));
						
						try{
							URL url = 
								MagnetURIHandler.getSingleton().registerResource(
									new MagnetURIHandler.ResourceProvider()
									{
										public String
										getUID()
										{
											return( RelatedContentManager.class.getName() + ".1" );
										}
										
										public String
										getFileType()
										{
											return( "png" );
										}
												
										public byte[]
										getData()
										{
											InputStream is = getClass().getClassLoader().getResourceAsStream( "org/gudy/azureus2/ui/icons/rcm.png" );
											
											if ( is == null ){
												
												return( null );
											}
											
											try{
												ByteArrayOutputStream	baos = new ByteArrayOutputStream();
												
												try{
													byte[]	buffer = new byte[8192];
													
													while( true ){
							
														int	len = is.read( buffer );
										
														if ( len <= 0 ){
															
															break;
														}
								
														baos.write( buffer, 0, len );
													}
												}finally{
													
													is.close();
												}
												
												return( baos.toByteArray());
												
											}catch( Throwable e ){
												
												return( null );
											}
										}
									});
																	
							properties.put( PR_ICON_URL, url.toExternalForm());
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
					
					public SearchInstance
					search(
						Map<String,Object>	search_parameters,
						SearchObserver		observer )
					
						throws SearchException
					{		
						try{
							return( RelatedContentManager.getSingleton().searchRCM( search_parameters, observer ));
							
						}catch( Throwable e ){
							
							throw( new SearchException( "Search failed", e ));
						}
					}
					
					public Object
					getProperty(
						int			property )
					{
						return( properties.get( property ));
					}
					
					public void
					setProperty(
						int			property,
						Object		value )
					{
						properties.put( property, value );
					}
				});
		}catch( Throwable e ){
			
			Debug.out( "Failed to register search provider" );
		}
		
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
					
					new RelatedContentUI();
				}
				
				public void
				UIDetached(
					UIInstance		instance )
				{
					
				}
			});
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					
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
}
