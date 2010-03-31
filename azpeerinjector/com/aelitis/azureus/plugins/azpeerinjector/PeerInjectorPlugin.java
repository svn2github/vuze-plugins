/*
 * Created on 03-May-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.azpeerinjector;


import java.util.*;


import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;



public class 
PeerInjectorPlugin 
	implements Plugin, DownloadListener
{
	private PluginInterface		plugin_interface;
	private LoggerChannel		log;
	
	private BooleanParameter 	auto_inject;
	private StringParameter 	peers;
	
	private Set					auto_injected = new HashSet();
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel( "Peer Injector" );
				
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "com.aelitis.azureus.plugins.azpeerinjector.internat.Messages");
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azpeerinjector.name");

		peers 		= config_model.addStringParameter2( "azpeerinjector.peers", "azpeerinjector.peers", "" );
				
		final ActionParameter	inject		= config_model.addActionParameter2( "azpeerinjector.inject.info", "azpeerinjector.inject.button" );

		auto_inject		= config_model.addBooleanParameter2( "azpeerinjector.autoinject", "azpeerinjector.autoinject", false );

		inject.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						Download[] downloads = plugin_interface.getDownloadManager().getDownloads();

						inject( downloads, "manual injection" );
					}
				});
			
		final BasicPluginViewModel model = 
			plugin_interface.getUIManager().createBasicPluginViewModel( "Peer Injector" );
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );

		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});
		
		
		TableContextMenuItem menu1 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azpeerinjector.contextmenu.inject" );
		TableContextMenuItem menu2 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azpeerinjector.contextmenu.inject" );
		
		MenuItemListener	menu_listener =
			new MenuItemListener()
			{
				public void
				selected(
					MenuItem		_menu,
					Object			_target )
				{
					Download download = (Download)((TableRow)_target).getDataSource();
					
					inject( new Download[]{ download }, "manual injection");
				}
			};
			
		menu1.addListener( menu_listener );
		menu2.addListener( menu_listener );
		
		plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{
						download.addListener( PeerInjectorPlugin.this );
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						download.removeListener( PeerInjectorPlugin.this );
						
						auto_injected.remove( download );
					}
				});
	}
	
	public void
	stateChanged(
		Download		download,
		int				old_state,
		int				new_state )
	{
		if ( 	new_state == Download.ST_DOWNLOADING ||
				new_state == Download.ST_SEEDING ){
			
			if ( auto_inject.getValue()){
				
				if ( !auto_injected.contains( auto_injected )){
				
					auto_injected.add( download );
				
					inject( new Download[]{ download }, "state changed" );
				}
			}
		}else if ( 	new_state == Download.ST_QUEUED ||
					new_state == Download.ST_STOPPED ){
			
				// re-arm
		
			auto_injected.remove( download );

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
	inject(
		Download[]	downloads,
		String		cause )
	{
		String	x = peers.getValue().trim();
		
		StringTokenizer	tok = new StringTokenizer(x, "," );
		
		List	ips 	= new ArrayList();
		List	ports	= new ArrayList();
		
		while( tok.hasMoreTokens()){
			
			String	s = tok.nextToken();
			
			int	p = s.indexOf(":");
			
			if ( p == -1 ){
				
				log.log( "Invalid peer spec '" + s + "', must be 'IP:port'" );
				
				continue;
			}
			
			ips.add( s.substring( 0, p ).trim());
			
			ports.add( s.substring( p+1 ).trim());
			
		}
		
		try{
			
			for (int i=0;i<downloads.length;i++){
				
				Download	download = downloads[i];
				
				PeerManager pm = download.getPeerManager();
				
				if ( pm == null ){

					continue;
				}
				
				Torrent	torrent = download.getTorrent();
				
				if ( torrent == null ){
					
					continue;
				}
				
				if ( torrent.isPrivate()){
					
					log.log( "Not injecting " + ips.size() + " peers into '" + download.getName() + "': torrent is private" );

				}else{
				
					log.log( "Injecting " + ips.size() + " peers into '" + download.getName() + "': " + cause );
					
					for (int j=0;j<ips.size();j++){
						
						try{
					
							String	ip 		= (String)ips.get(j);
							int		port	= Integer.parseInt((String)ports.get(j));
							
							pm.addPeer( ip, port );
							
						}catch( Throwable e ){
							
							log.log(e);
						}
					}
				}
			}
		}catch( Throwable e ){
				
			log.log(e);
		}
	}
}
