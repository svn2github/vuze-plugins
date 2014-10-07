package com.vuze.plugins.azmsgsync;
/*
 * Created on Aug 28, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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




import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.components.UITextArea;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseImpl;

import com.aelitis.azureus.plugins.dht.DHTPluginInterface;


public class 
MsgSyncPlugin
	implements Plugin
{
	private PluginInterface			plugin_interface;
	private PluginConfig			plugin_config;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	private LocaleUtilities			loc_utils;

	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException 
	{
		try{
			plugin_interface	= _plugin_interface;
			
			loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
			
			log	= plugin_interface.getLogger().getTimeStampedChannel( "Message Sync");
			
			final UIManager	ui_manager = plugin_interface.getUIManager();

			view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azmsgsync.name" ));

			view_model.getActivity().setVisible( false );
			view_model.getProgress().setVisible( false );
			
					
			plugin_config = plugin_interface.getPluginconfig();
						
			config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azmsgsync.name" );

			view_model.setConfigSectionID( "azmsgsync.name" );
			
			final StringParameter 	command_text_param = config_model.addStringParameter2( "azmsgsync.cmd.text", "azi2phelper.cmd.text", "" );
			final ActionParameter	command_exec_param = config_model.addActionParameter2( "azmsgsync.cmd.act1", "azi2phelper.cmd.act2" );
			
			final UITextArea text_area = config_model.addTextArea( "azmsgsync.statuslog");

			command_exec_param.addListener(
				new ParameterListener() 
				{
					public void 
					parameterChanged(
						Parameter param ) 
					{
						new AEThread2( "cmdrunner" )
						{
							public void
							run()
							{
								try{
									command_exec_param.setEnabled( false );

									executeCommand(	command_text_param.getValue());
																	
								}catch( Throwable e){
									
									log( "Command failed: " + Debug.getNestedExceptionMessage( e ));
									
								}finally{
									
									command_exec_param.setEnabled( true );
								}
							}
						}.start();
					}
				});

			log.addListener(
					new LoggerChannelListener()
					{
						public void
						messageLogged(
							int		type,
							String	content )
						{
							view_model.getLogArea().appendText( content + "\n" );
							
							text_area.appendText( content + "\n" );
						}
						
						public void
						messageLogged(
							String		str,
							Throwable	error )
						{
							view_model.getLogArea().appendText( str + "\n" );
							view_model.getLogArea().appendText( error.toString() + "\n" );
							
							String text = str + ": " + Debug.getNestedExceptionMessage( error );
							
							text_area.appendText( text + "\n" );

						}
					});

		}catch( Throwable e){
			
			throw( new PluginException( "Initialization failed", e ));
		}
	}

	private void
	executeCommand(
		String		cmd_str )
	{
		try{
			log( "Executing '" + cmd_str + "'" );
			
			cmd_str = cmd_str.trim();
			
			String[] bits = cmd_str.split( "\\s+" );

			if ( bits.length == 0 ){
				
				log( "No command" );

			}else{
				
				String c = bits[0].toLowerCase();
				
				if ( c.equals( "create" )){
				
					if ( bits.length != 3 ){
						
						throw( new Exception( "Usage: create <dht_id> <key>" ));
					}
					
					String dht_id 	= bits[1];
					String key		= bits[2];
					
					if ( dht_id.equals( "p" )){
						
						DHTPluginInterface dht = ((DDBaseImpl)plugin_interface.getDistributedDatabase()).getDHTPlugin();
						
						byte[]	key_bytes = key.getBytes( "UTF-8" );
						
						MsgSyncHandler handler = getSyncHandler( dht, key_bytes );
						
						log( "Got handler: " + handler.getString());
					}
				}else{
				
					log( "Unrecognized command" );
				}
			}
		}catch( Throwable e){
			
			log( "Command exec failed", e );
		}
	}
	
	public void
	log(
		String	str )
	{
		if ( log != null ){
			
			log.log( str );
			
		}else{
			
			System.out.println( str );
		}
	}
	
	public void
	log(
		String		str,
		Throwable 	e )
	{
		if ( log != null ){
			
			log.log( str, e );
			
		}else{
			
			System.out.println( str );
			
			e.printStackTrace();
		}
	}
	
	private List<MsgSyncHandler>	sync_handlers = new ArrayList<MsgSyncHandler>();
	
	private MsgSyncHandler
	getSyncHandler(
		DHTPluginInterface		dht,
		byte[]					key )
	{
		synchronized( sync_handlers ){
			
			for ( MsgSyncHandler h: sync_handlers ){
				
				if ( h.getDHT() == dht && Arrays.equals( h.getUserKey(), key )){
					
					return( h );
				}
			}
			
			MsgSyncHandler h = new MsgSyncHandler( dht, key );
			
			sync_handlers.add( h );
			
			return( h );
		}
	}
}
