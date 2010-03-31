/*
 * Created on 6-Sept-2003
 * Created by Olivier
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.views;


import java.util.*;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.irc.IrcClient;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;


public class 
IrcView 
	implements Plugin
{
	
	PluginInterface pluginInterface;
	
	private static final String VIEWID = "IRC";
		
	UISWTInstance swtInstance = null;
	
	public void 
	initialize(
		final PluginInterface plugin_interface)
	{
			pluginInterface = plugin_interface;		
			UIManager	ui_manager = plugin_interface.getUIManager();			
			final PluginConfig	plugin_config = plugin_interface.getPluginconfig();			

			// Fix language bug in 2504 when no default language is selected
			// Going to Config->Interface->Language and not selecting anything
			// will cause the lanuage to change to the first one on the list on
			// restart.
			pluginInterface.addListener(new PluginListener() {
			public void initializationComplete() {
		  	String sLocale = plugin_config.getUnsafeStringParameter("locale", "x");
		  	if (sLocale.equals("x")) {
		  		String s = Locale.getDefault().toString();
		  		if (s == null || s.length() == 0) {
		  			// No locale, or "use default".. the default is "en"
		  			// according to the language config page, and is forced to be
		  			// there via MessageText class
		  			s = new Locale("en").toString();
		  		}
		  		plugin_config.setUnsafeStringParameter("locale", s);
		  		plugin_config.setUnsafeBooleanParameter("locale.bug", true);
		  	}
			}

			public void closedownInitiated() {
			}

			public void closedownComplete() {
			}
		});
			
			// migrate any existing IRC config settings from core to plugin
			if ( plugin_config.getPluginStringParameter( SWTIrcView.CONFIG_IRC_SERVER, "").length() == 0 ){			
				plugin_config.setPluginParameter(
						SWTIrcView.CONFIG_IRC_SERVER,
						plugin_config.getStringParameter(
								SWTIrcView.CONFIG_IRC_SERVER,
								SWTIrcView.CONFIG_IRC_SERVER_DEFAULT ));
			
				plugin_config.setPluginParameter(
						SWTIrcView.CONFIG_IRC_CHANNEL,
						plugin_config.getStringParameter(
								SWTIrcView.CONFIG_IRC_CHANNEL,
								SWTIrcView.CONFIG_IRC_CHANNEL_DEFAULT ));

				plugin_config.setPluginParameter(
						SWTIrcView.CONFIG_IRC_USER,
						plugin_config.getStringParameter(
								SWTIrcView.CONFIG_IRC_USER,
								SWTIrcView.CONFIG_IRC_USER_DEFAULT ));
			}
			
			BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugins.irc");
			
			final LabelParameter label_wiki = config_model.addLabelParameter2("ConfigView.label.ircwiki"); 
			final StringParameter irc_server = config_model.addStringParameter2( SWTIrcView.CONFIG_IRC_SERVER, "ConfigView.label.ircserver", SWTIrcView.CONFIG_IRC_SERVER_DEFAULT );
			final StringParameter irc_channel = config_model.addStringParameter2( SWTIrcView.CONFIG_IRC_CHANNEL, "ConfigView.label.ircchannel", SWTIrcView.CONFIG_IRC_CHANNEL_DEFAULT );
			final StringParameter irc_user = config_model.addStringParameter2( SWTIrcView.CONFIG_IRC_USER, "ConfigView.label.irclogin", SWTIrcView.CONFIG_IRC_USER_DEFAULT );
			config_model.addBooleanParameter2(IrcClient.CONFIG_IRC_SEND_USER_INFO, "ConfigView.boolean.ircsendinfo",IrcClient.CONFIG_IRC_SEND_USER_INFO_DEFAULT);
			config_model.addBooleanParameter2(IrcClient.CONFIG_IRC_LOG, "ConfigView.boolean.irclog",IrcClient.CONFIG_IRC_LOG_DEFAULT);
			
			config_model.createGroup( "ConfigView.group.irctitle",
					new Parameter[]{ 
						label_wiki,
						irc_server,
						irc_channel,
						irc_user
					});

			pluginInterface.getUIManager().addUIListener(
		
				new UIManagerListener()
				{
					ViewListener	view_listener;
					
					private UTTimer			timer;
					private UTTimerEvent	event;
					
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){							
							swtInstance = (UISWTInstance)instance;														
							view_listener = new ViewListener();							
							swtInstance.addView( UISWTInstance.VIEW_MAIN, VIEWID, view_listener );							
							timer = plugin_interface.getUtilities().createTimer( "IRC refresher", true );							
							event = timer.addPeriodicEvent(
									2000,
									new UTTimerEventPerformer()
								{
									public void
									perform(
										UTTimerEvent		lEvent )
									{
										view_listener.refresh();
									}
								});
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						event.cancel();
						
						timer.destroy();
					}
				});
			

	}
	
	private class 
	ViewListener 
		implements UISWTViewEventListener 
	{
		private Map	irc_view_map = new HashMap();
		
		public boolean 
		eventOccurred(
			UISWTViewEvent event ) 
		{
			boolean	res = true;
			
			UISWTView view	= event.getView();

			switch (event.getType()) {

				case UISWTViewEvent.TYPE_CREATE:{

					res = irc_view_map.size() == 0;

					break;
				}
				case UISWTViewEvent.TYPE_INITIALIZE:{
											
					Composite	comp = (Composite)event.getData();
					
					SWTIrcView irc_view = new SWTIrcView(pluginInterface);
					
					irc_view_map.put( view, irc_view );


					Object data = event.getView().getDataSource();
					
					if (data instanceof String[] && ((String[])data).length == 3) {
						String[] sParams = (String[])data; 
						irc_view.initialize(comp, sParams[0], sParams[1], sParams[2]);
					} else {
						irc_view.initialize( comp );
					}
					
					refresh();
					
					break;
				}
				case UISWTViewEvent.TYPE_FOCUSGAINED:{
			
					SWTIrcView	swt_view = (SWTIrcView)irc_view_map.get( view );
					
					if ( swt_view != null ){
						
						swt_view.focusGained();
					}
					
					break;
				}	
				case UISWTViewEvent.TYPE_DESTROY:{
					
					SWTIrcView	swt_view = (SWTIrcView)irc_view_map.get( view );
					
					if ( swt_view != null ){

						swt_view.delete();
					
						irc_view_map.remove( view );
					}
					
					break;
				}	
			}
			
			return res;
		}

		protected void
		refresh()
		{
			List	views;
			
			try{
				views	= new ArrayList( irc_view_map.entrySet());
				
				Iterator	it = views.iterator();
				
				while( it.hasNext()){

					Map.Entry	entry = (Map.Entry)it.next();
					
					((SWTIrcView)entry.getValue()).refresh((UISWTView)entry.getKey());
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
}
