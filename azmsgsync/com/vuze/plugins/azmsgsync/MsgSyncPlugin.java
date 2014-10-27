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




import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ipc.IPCException;
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

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface;


public class 
MsgSyncPlugin
	implements UnloadablePlugin
{
	protected static final int	TIMER_PERIOD = 2500;
	
	
	private PluginInterface			plugin_interface;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	private LocaleUtilities			loc_utils;

	private CopyOnWriteList<MsgSyncHandler>	sync_handlers = new CopyOnWriteList<MsgSyncHandler>();

	private TimerEventPeriodic		timer;
		
	private volatile boolean		init_called;
	private volatile boolean		destroyed;
	
	private AESemaphore				init_sem = new AESemaphore( "MsgSync:init" );
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException 
	{
		try{
			synchronized( this ){
				
				init_called = true;
				
				plugin_interface	= _plugin_interface;
				
				setUnloadable( true );
				
				loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
				
				log	= plugin_interface.getLogger().getTimeStampedChannel( "Message Sync");
				
				final UIManager	ui_manager = plugin_interface.getUIManager();
	
				view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azmsgsync.name" ));
	
				view_model.getActivity().setVisible( false );
				view_model.getProgress().setVisible( false );
											
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
	
				timer = SimpleTimer.addPeriodicEvent(
					"MsgSync:periodicSync",
					TIMER_PERIOD,
					new TimerEventPerformer() {
						
						private int	count = 0;
						
						@Override
						public void 
						perform(
							TimerEvent event ) 
						{
							count++;
							
							if ( sync_handlers.size() > 0 ){
								
								for ( MsgSyncHandler handler: sync_handlers ){
									
									handler.timerTick( count );
								}
							}
						}
					});
			}	
		}catch( Throwable e){
			
			throw( new PluginException( "Initialization failed", e ));
			
		}finally{
			
			init_sem.releaseForever();
			
			if ( destroyed ){
				
				unload();
			}
		}
	}

	public void
	unload()
	{
		destroyed = true;
		
		synchronized( this ){
			
			if ( timer != null ){
				
				timer.cancel();
				
				timer = null;
			}
			
			for ( MsgSyncHandler handler: sync_handlers ){
				
				handler.destroy();
			}
			
			sync_handlers.clear();
			
			if ( view_model != null ){
				
				view_model.destroy();
				
				view_model = null;
			}
			
			if ( config_model != null ){
				
				config_model.destroy();
				
				config_model = null;
			}
		}
	}
	
	private void
	setUnloadable(
		boolean	b )
	{
		PluginInterface pi = plugin_interface;
		
		if ( pi != null ){
			
			pi.getPluginProperties().put( "plugin.unload.disabled", String.valueOf( !b ));
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
				
				if ( c.equals( "send" )){
				
					if ( bits.length != 4 ){
						
						throw( new Exception( "Usage: create <dht_id> <key> <message>" ));
					}
					
					String dht_id 	= bits[1];
					String key		= bits[2];
					String message	= bits[3];
					
					DHTPluginInterface dht;
					
					if ( dht_id.equals( "p" )){
						
						dht = ((DDBaseImpl)plugin_interface.getDistributedDatabase()).getDHTPlugin();
						
					}else{
									
						List<DistributedDatabase> ddbs = plugin_interface.getUtilities().getDistributedDatabases( new String[]{ AENetworkClassifier.AT_I2P });
						
						if ( ddbs.size() == 0 ){
							
							throw( new Exception( "No I2P DDB Available" ));
						}
						
						dht = ((DDBaseImpl)ddbs.get(0)).getDHTPlugin();
					}
					
					byte[]	key_bytes = key.getBytes( "UTF-8" );
						
					MsgSyncHandler handler = getSyncHandler( dht, key_bytes );
						
					log( "Got handler: " + handler.getString());
					
					handler.sendMessage( message.getBytes( "UTF-8" ));
					
				}else{
				
					log( "Unrecognized command" );
				}
			}
		}catch( Throwable e){
			
			log( "Command exec failed", e );
		}
	}
	
		// IPC start
	
	public Map<String,Object>
	getMessageHandler(
		Map<String,Object>		options )
		
		throws IPCException
	{
		synchronized( this ){
			
			if ( !init_called ){
				
				throw( new IPCException( "Not initialised" ));
			}
		}
		
		init_sem.reserve();
		
		Map<String,Object>	reply = new HashMap<String, Object>();
		
		String		network = (String)options.get( "network" );
		byte[]		key		= (byte[])options.get( "key" );
		
		DHTPluginInterface dht;
		
		if ( network == null || network == AENetworkClassifier.AT_PUBLIC ){
			
			dht = ((DDBaseImpl)plugin_interface.getDistributedDatabase()).getDHTPlugin();

		}else if ( network == AENetworkClassifier.AT_I2P ){
				
			String server_id = (String)options.get( "server_id" );

			if ( server_id == null ){
				
				server_id = "dchat";
			}
			
			if ( server_id.equals( "dchat_shared" )){
			
				List<DistributedDatabase> ddbs = plugin_interface.getUtilities().getDistributedDatabases( new String[]{ AENetworkClassifier.AT_I2P });
				
				if ( ddbs.size() == 0 ){
					
					throw( new IPCException( "No I2P DDB Available" ));
				}
				
				dht = ((DDBaseImpl)ddbs.get(0)).getDHTPlugin();
				
			}else{
			
				PluginInterface pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "azneti2phelper" );
					
				if ( pi == null ){
					
					throw( new IPCException( "No I2P DDB Available" ));
				}
				
				Map<String,Object>	ipc_options = new HashMap<String, Object>();
					
				ipc_options.put( "server_id", server_id );
					
				dht = (DHTPluginInterface)pi.getIPC().invoke( "getProxyDHT", new Object[]{ "msg_sync_chat", ipc_options });
			}
		}else{
			
			throw( new IPCException( "Unsupported network: " + network ));
		}
				
		MsgSyncHandler	parent_handler = (MsgSyncHandler)options.get( "parent_handler" );
		
		MsgSyncHandler handler;
		
		if ( parent_handler != null ){
			
			byte[]				target_pk		= (byte[])options.get( "target_pk" );
			Map<String,Object>	target_contact	= (Map<String,Object>)options.get( "target_contact" );
			
			handler = getSyncHandler( dht, parent_handler, target_pk, target_contact, null, null );
			
		}else{
		
			handler = getSyncHandler( dht, key );
		}
		
		Object listener = options.get( "listener" );
		
		if ( listener != null ){
			
			addListener( handler, listener );
		}
		
		reply.put( "handler", handler );
		
		reply.put( "pk", handler.getPublicKey());

		synchronized( this ){
			
			if ( destroyed ){
				
				throw( new IPCException( "Plugin unloaded" ));
			}
		}
		
		return( reply );
	}
	
	public Map<String,Object>
	updateMessageHandler(
		Map<String,Object>		options )
		
		throws IPCException
	{
		synchronized( this ){
			
			if ( !init_called ){
				
				throw( new IPCException( "Not initialised" ));
			}
		}
		
		init_sem.reserve();
		
		MsgSyncHandler handler = (MsgSyncHandler)options.get( "handler" );
		
		if ( handler.getPlugin() != this ){
			
			throw( new IPCException( "Plugin has been unloaded" ));
		}
		
		Object listener = options.get( "addlistener" );
		
		if ( listener != null ){
			
			addListener( handler, listener );
		}
		
		Map<String,Object>	reply = new HashMap<String, Object>();

		reply.put( "pk", handler.getPublicKey());
		
		return( reply );
	}
	
	private void
	addListener(
		final MsgSyncHandler		handler,
		final Object				listener )
		
		throws IPCException
	{
		try{
			final Method mesasge_callback 	= listener.getClass().getMethod( "messageReceived", Map.class );
			final Method chat_callback 		= listener.getClass().getMethod( "chatRequested", Map.class );
			
			MsgSyncListener l = 
				new MsgSyncListener()
				{
					@Override
					public void 
					messageReceived(
						MsgSyncMessage message ) 
					{
						try{
							Map<String,Object> map = new HashMap<String, Object>();
							
							map.put( "content", message.getContent());
							map.put( "age", message.getAgeSecs());
							map.put( "pk", message.getNode().getPublicKey());
							map.put( "address", message.getNode().getContact().getAddress());
							map.put( "contact", message.getNode().getContact().exportToMap());
							
								// as a public ID we use the start of the signature 
							
							byte[]	sig = message.getSignature();
							
							byte[] 	msg_id = new byte[12];
							
							System.arraycopy( sig, 0, msg_id, 0, msg_id.length );
							
							map.put( "id", msg_id );
							
							if ( message.getStatus() != MsgSyncMessage.ST_OK ){
								
								map.put( "error", message.getError());
							}
							
							mesasge_callback.invoke( listener, map );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
					
					public String
					chatRequested(
						byte[]				remote_pk,
						MsgSyncHandler		handler )
						
						throws IPCException
					{
						try{
							Map<String,Object> map = new HashMap<String, Object>();
							
							map.put( "handler", handler );
							map.put( "pk", remote_pk );
							
							Map<String,Object> reply = (Map<String,Object>)chat_callback.invoke( listener, map );
							
							return((String)reply.get( "nickname" ));
							
						}catch( InvocationTargetException  e ){
							
							Throwable cause = ((InvocationTargetException)e).getCause();
							
							if ( cause instanceof IPCException ){
								
								throw((IPCException)cause);
							}
							
							throw( new IPCException( cause ));
							
						}catch( Throwable e ){
							
							throw( new IPCException( e ));
						}
					}
				};
				
			handler.addListener( l ); 
			
			List<MsgSyncMessage> messages = handler.getMessages();
			
			for ( MsgSyncMessage msg: messages ){
				
				try{
					l.messageReceived( msg );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}catch( Throwable e ){
			
			throw( new IPCException( "Failed to add listener", e ));
		}
	}
	
	public Map<String,Object>
	sendMessage(
		Map<String,Object>		options )
		
		throws IPCException
	{	
			// safest bet is to prevent auto-unloading once a message has been sent as it'll get 
			// lost if it doesn't get replicated
		
		setUnloadable( false );
		
		byte[]		content		= (byte[])options.get( "content" );

		MsgSyncHandler handler = (MsgSyncHandler)options.get( "handler" );
		
		if ( handler.getPlugin() != this ){
			
			throw( new IPCException( "Plugin has been unloaded" ));
		}
		
		handler.sendMessage( content );
		
		Map<String,Object>	reply = new HashMap<String, Object>();

		return( reply );
	}
	
	public Map<String,Object>
	getStatus(
		Map<String,Object>		options )
		
		throws IPCException
	{
		MsgSyncHandler handler = (MsgSyncHandler)options.get( "handler" );
		
		if ( handler.getPlugin() != this ){
			
			throw( new IPCException( "Plugin has been unloaded" ));
		}
				
		Map<String,Object>	reply = new HashMap<String, Object>();

		reply.put( "status", 		handler.getStatus());
		reply.put( "dht_nodes", 	handler.getDHTCount());
		
		int[] node_counts = handler.getNodeCounts();
		
		reply.put( "nodes_local", new Long(node_counts[0]));
		reply.put( "nodes_live", new Long(node_counts[1]));
		reply.put( "nodes_dying", new Long(node_counts[2]));
		
		double[] req_details = handler.getRequestCounts();

		reply.put( "req_in", new Double(req_details[0]));
		reply.put( "req_in_rate", new Double(req_details[1]));
		reply.put( "req_out_ok", new Double(req_details[2]));
		reply.put( "req_out_fail", new Double(req_details[3]));
		reply.put( "req_out_rate", new Double(req_details[4]));

		reply.put( "nid", handler.getNodeID());
		reply.put( "pk", handler.getPublicKey());
		//reply.put( "address", )	// could block here so ignore for the moment 
		
		return( reply );
	}
	
	public Map<String,Object>
	removeMessageHandler(
		Map<String,Object>		options )
		
		throws IPCException
	{
		MsgSyncHandler handler = (MsgSyncHandler)options.get( "handler" );
		
		if ( handler.getPlugin() != this ){
			
			throw( new IPCException( "Plugin has been unloaded" ));
		}
		
		removeSyncHandler( handler );
		
		Map<String,Object>	reply = new HashMap<String, Object>();

		return( reply );
	}
	
		// IPC end
	
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
		
	private MsgSyncHandler
	getSyncHandler(
		DHTPluginInterface		dht,
		byte[]					key )
		
		throws IPCException
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				throw( new IPCException( "Plugin unloaded" ));
			}
			
			for ( MsgSyncHandler h: sync_handlers ){
				
				if ( h.getDHT() == dht && Arrays.equals( h.getUserKey(), key )){
					
					return( h );
				}
			}
			
			try{
				MsgSyncHandler h = new MsgSyncHandler( this, dht, key );
				
				sync_handlers.add( h );
				
				return( h );
				
			}catch( Throwable e ){
				
				throw( new IPCException( "Failed to create message handler", e ));
			}
		}
	}
	
	protected MsgSyncHandler
	getSyncHandler(
		DHTPluginInterface		dht,
		MsgSyncHandler			parent_handler,
		byte[]					target_pk,
		Map<String,Object>		target_contact,
		byte[]					user_key,
		byte[]					secret )
		
		throws IPCException
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				throw( new IPCException( "Plugin unloaded" ));
			}
			
			try{
				MsgSyncHandler h = new MsgSyncHandler( this, dht, parent_handler, target_pk, target_contact, user_key, secret );
				
				sync_handlers.add( h );
				
				return( h );
				
			}catch( Throwable e ){
				
				throw( new IPCException( "Failed to create message handler", e ));
			}
		}
	}
	
	private void
	removeSyncHandler(
		MsgSyncHandler		handler )
	{
		synchronized( this ){

			sync_handlers.remove( handler );
		}
		
		handler.destroy();
	}
}