/*
 * Created on 05-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.parg.azureus.plugins.categorychanger;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.download.*;


/**
 * @author parg
 *
 */

public class 
CategoryChangerPlugin
	implements Plugin, DownloadManagerListener, DownloadListener
{
	public static final String 	CONFIG_ENABLE			= "enable";
	public static final boolean CONFIG_ENABLE_DEFAULT	= true;
		
	public static final String 	CONFIG_SEEDING_CATEGORY				= "seeding_category";
	public static final String 	CONFIG_DOWNLOADING_CATEGORY			= "downloading_category";
	
	protected PluginInterface		plugin_interface;
	
	protected LoggerChannel			log;
	
	protected BooleanParameter 		enable;
	protected StringParameter 		seeding_category;
	protected StringParameter 		downloading_category;
			
	protected TorrentAttribute		ta_category;
	
	
	protected List					downloads	= new ArrayList(); 
	protected Map					cat_map		= new HashMap();
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
				
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		log	= plugin_interface.getLogger().getChannel( "Category Changer");
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "categorychanger.name");
						
		enable 		= config_model.addBooleanParameter2( CONFIG_ENABLE, "categorychanger.enable", CONFIG_ENABLE_DEFAULT );

		seeding_category	 	= config_model.addStringParameter2( CONFIG_SEEDING_CATEGORY, "categorychanger.seeding_category", "" );
		downloading_category	= config_model.addStringParameter2( CONFIG_DOWNLOADING_CATEGORY, "categorychanger.downloading_category", "" );

		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "categorychanger.name" ));

		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	content )
					{
						view_model.getLogArea().appendText( content + "\n" );
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						view_model.getLogArea().appendText( str + "\n" );
						view_model.getLogArea().appendText( error.toString() + "\n" );
					}
				});
		
		
		log.log( "Seeding Category = '" + seeding_category.getValue() + "'" );
		log.log( "Downloading Category = '" + downloading_category.getValue() + "'" );
		
		view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
		
		enable.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter		p )
				{					
					view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
				}
			});	
		

		
		enable.addEnabledOnSelection( seeding_category );
		enable.addEnabledOnSelection( downloading_category );

		ta_category = plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_CATEGORY );
		
		plugin_interface.getDownloadManager().addListener( this );
		
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
					for (int i=0;i<downloads.size();i++){
						
						setNotSeeding((Download)downloads.get(i));
					}
					
					for (int i=0;i<downloads.size();i++){
						
						setNotDownloading((Download)downloads.get(i));
					}
				}
				
				public void
				closedownComplete()
				{
				}
			});
	}
	
	
	public void
	downloadAdded(
		Download	download )
	{
		log.log( "Download added:" + download.getName());
				
		downloads.add( download );
		
		download.addListener( this );
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		log.log( "Download removed:" + download.getName());
		
		downloads.remove( download );
		
		cat_map.remove( download );
		
		download.removeListener( this );
	}
	
	public void
	stateChanged(
		Download		download,
		int				old_state,
		int				new_state )
	{
		if ( enable.getValue()){
			
			log.log( 	"Download state changed:" + 
						download.getName() + ": " + Download.ST_NAMES[old_state] + " -> " + Download.ST_NAMES[new_state] );
	
			if ( new_state == Download.ST_SEEDING ){
	
				setNotDownloading( download );
				
				setSeeding( download );
				
			}else if ( new_state == Download.ST_DOWNLOADING ){
				
				setNotSeeding( download );
				
				setDownloading( download );
			}else{
				
				setNotDownloading( download );
				
				setNotSeeding( download );
			}
		}
	}
  
	public void
	positionChanged(
		Download	download, 
		int 		oldPosition,
		int 		newPosition )
	{	
	}
	
	
	protected void
	setDownloading(
		Download		d )
	{
		String	dc = downloading_category.getValue().trim();
		
		if ( dc.length() == 0 ){
			
			return;
		}
		
		String	old_name = d.getAttribute( ta_category );
		
		if ( old_name == null || !old_name.equals( dc )){
							
			cat_map.put( d, old_name );
			
			d.setAttribute( ta_category, dc );
		}
	}
	
	protected void
	setNotDownloading(
		Download		d )
	{
		String	dc = downloading_category.getValue().trim();
		
		if ( dc.length() == 0 ){
			
			return;
		}
		
		String	old_name = d.getAttribute( ta_category );

		if ( old_name != null && old_name.equals( dc )){

			String	old_cat = (String)cat_map.get(d);
						
			d.setAttribute( ta_category, old_cat );
		}
	}
	
	protected void
	setSeeding(
		Download		d )
	{
		String	sc = seeding_category.getValue().trim();
		
		if ( sc.length() == 0 ){
			
			return;
		}
		
		String	old_name = d.getAttribute( ta_category );
		
		if ( old_name == null || !old_name.equals( sc )){
							
			cat_map.put( d, old_name );
			
			d.setAttribute( ta_category, sc );
		}
	}
	
	protected void
	setNotSeeding(
		Download		d )
	{
		String	sc = seeding_category.getValue().trim();
		
		if ( sc.length() == 0 ){
			
			return;
		}
		
		String	old_name = d.getAttribute( ta_category );

		if ( old_name != null && old_name.equals( sc )){

			String	old_cat = (String)cat_map.get(d);
						
			d.setAttribute( ta_category, old_cat );
		}
	}
}
