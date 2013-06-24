/*
 * Created on Mar 19, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.xmwebui.swt;


import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIInstance;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.pairing.PairedService;
import com.aelitis.azureus.core.pairing.PairingConnectionData;
import com.aelitis.azureus.core.pairing.PairingManagerFactory;
import com.aelitis.azureus.plugins.xmwebui.XMWebUIPlugin;
import com.aelitis.azureus.plugins.xmwebui.client.proxy.XMClientProxy;
import com.aelitis.azureus.plugins.xmwebui.client.rpc.XMRPCClient;
import com.aelitis.azureus.plugins.xmwebui.client.rpc.XMRPCClientException;
import com.aelitis.azureus.plugins.xmwebui.client.rpc.XMRPCClientFactory;
import com.aelitis.azureus.util.ImportExportUtils;



public class 
XMWebUIPluginView
	implements UISWTViewEventListener
{
	private final String CONFIG_PARAM = "xmwebui.remote.vuze.config";

	private static final int LOG_NORMAL 	= 1;
	private static final int LOG_SUCCESS 	= 2;
	private static final int LOG_ERROR 		= 3;

	private static final String	CS_DISCONNECTED		= "Disconnected";
	private static final String	CS_BASIC			= "Basic";
	private static final String	CS_SECURE_DIRECT	= "Secure (Direct)";
	private static final String	CS_SECURE_PROXIED	= "Secure (Proxied)";
	

	private XMWebUIPlugin		plugin;
	private UISWTInstance		ui_instance;
	
	private Map<String,AccountConfig>		account_config 		= new HashMap<String,AccountConfig>();
	
	private Map<String,RemoteConnection>	remote_connections 	= new HashMap<String, RemoteConnection>();
	
	private ViewInstance	current_instance;
			
	public
	XMWebUIPluginView(
		XMWebUIPlugin		_plugin,
		UIInstance			_ui_instance )
	{
		plugin			= _plugin;
		ui_instance		= (UISWTInstance)_ui_instance;

		loadConfig();
		
		ui_instance.addView( UISWTInstance.VIEW_MAIN, "xmwebui", this );
	}
	
	private void 
	loadConfig()
	{
		Map config = COConfigurationManager.getMapParameter( CONFIG_PARAM, new HashMap());
		
		List<Map>	acs = (List<Map>)config.get( "acs" );
		
		if ( acs != null ){
			
			for ( Map m: acs ){
				
				try{
					AccountConfig ac = new AccountConfig( m );
					
					account_config.put( ac.getAccessCode(), ac );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
	}
	
	private void
	saveConfig()
	{
		Map	config = new HashMap();
		
		List<Map>	list = new ArrayList<Map>();
		
		config.put( "acs", list );
		
		for ( AccountConfig ac: account_config.values()){
			
			try{
				list.add( ac.export());
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		COConfigurationManager.setParameter( CONFIG_PARAM,config );
		
		COConfigurationManager.save();
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
		switch( event.getType() ){

			case UISWTViewEvent.TYPE_CREATE:{
				
				if ( current_instance != null ){
					
					return( false );
				}
								
				break;
			}
			case UISWTViewEvent.TYPE_INITIALIZE:{
				

				current_instance = new ViewInstance((Composite)event.getData());
				
				break;
			}
			case UISWTViewEvent.TYPE_CLOSE:
			case UISWTViewEvent.TYPE_DESTROY:{
				
				try{
					if ( current_instance != null ){
						
						current_instance.destroy();
					}
				}finally{
					
					current_instance = null;
				}
				
				break;
			}
		}
		
		return true;
	}
	
	private void
	setConnected(
		RemoteConnection		connection,
		boolean					is_connected )
	{
		if ( !is_connected ){
			
			synchronized( remote_connections ){
				
				remote_connections.remove( connection.getAccessCode());
			}
		}
			
		if ( current_instance != null ){
				
			current_instance.updateRemoteConnection( connection, is_connected );
		}
	}
	
	private void
	log(
		String	str )
	{
		if ( current_instance != null ){
			
			current_instance.print( str );
		}
	}
	
	private void
	logError(
		Throwable e )
	{
		if ( current_instance != null ){
			
			current_instance.print( Debug.getNestedExceptionMessage( e ), LOG_ERROR, false );
		}
	}
	
	private void
	logError(
		String	str )
	{
		if ( current_instance != null ){
			
			current_instance.print( str, LOG_ERROR, false );
		}
	}
	
	private class
	ViewInstance
	{		
		private Composite 	composite;
		private Combo 		ac_list;
		private Button		connect_button;
		private Button		remove_button;
		
		private AccountConfig	current_account;
		
		private Group 			options_group;
		private Text			description;
		private Button 			do_basic;
		private Button			do_basic_def;
		private Text			basic_username;
		private Text			basic_password;
		
		private Button			force_proxy;
		private Text			secure_password;
		
		private Group 			operation_group;
		private Label			status_label;
		
		private StyledText 	log ;
		
		private boolean	initialised;
		
		private
		ViewInstance(
			Composite	_comp )
		{
			composite	= _comp;
			
			Composite main = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 4;

			main.setLayout(layout);
			GridData grid_data = new GridData(GridData.FILL_BOTH );
			main.setLayoutData(grid_data);

			Label info_label = new Label( main, SWT.NULL );
			grid_data = new GridData();
			grid_data.horizontalSpan = 3;
			info_label.setLayoutData( grid_data );
			Messages.setLanguageText( info_label, "xmwebui.rpc.info" );
			
			new LinkLabel( main, "xmwebui.link", MessageText.getString(  "xmwebui.control.wiki.link" ));
			
			Label add_label = new Label( main, SWT.NULL );
			Messages.setLanguageText( add_label, "xmwebui.rpc.add.info" );
			
			final Text	ac_text = new Text( main, SWT.BORDER );
						
			final Button 	add_button = new Button( main, SWT.PUSH );
			Messages.setLanguageText( add_button, "xmwebui.rpc.add" );

			add_button.setEnabled( false );

			add_button.addSelectionListener(
					new SelectionAdapter()
					{
						public void 
						widgetSelected(
							SelectionEvent e ) 
						{
							String ac = ac_text.getText().trim();
						
							if ( !account_config.containsKey( ac )){
						
								AccountConfig conf = new AccountConfig( ac );
								
								account_config.put( ac, conf );
								
								saveConfig();
								
								updateAccountList();
								
								setSelectedAccount( conf );
							}
						}
					});
			
			ac_text.addListener(SWT.Modify, new Listener() {
			      public void handleEvent(Event event) {
			    	  add_button.setEnabled( ac_text.getText().trim().length() > 0 );
			      }
			    });
			
			new Label( main, SWT.NULL );
			
			Label sel_label = new Label( main, SWT.NULL );
			Messages.setLanguageText( sel_label, "xmwebui.rpc.select" );
			
			ac_list = new Combo( main, SWT.SINGLE | SWT.READ_ONLY );
						
			connect_button = new Button( main, SWT.PUSH );
			Messages.setLanguageText( connect_button, "xmwebui.rpc.connect" );
		
			connect_button.addSelectionListener(
				new SelectionAdapter()
				{
					public void 
					widgetSelected(
						SelectionEvent e ) 
					{
						int index = ac_list.getSelectionIndex();
						
						String ac = ac_list.getItem( index );
						
						ac = trimAccount( ac );
						
						synchronized( remote_connections ){
							
							RemoteConnection rc = remote_connections.get( ac );
							
							if ( rc == null ){
								
								rc = new RemoteConnection( account_config.get( ac ));
								
								remote_connections.put( ac, rc );
								
								final RemoteConnection f_rc = rc;
								
								new AEThread2("")
								{
									public void
									run()
									{
										f_rc.connect();
									}
								}.start();
								
							}else{
								
								print( ac + " is already connecting/connected" );
							}
						}
					}
				});
			
			connect_button.setEnabled( false );
			
			remove_button = new Button( main, SWT.PUSH );
			Messages.setLanguageText( remove_button, "xmwebui.rpc.remove" );

			remove_button.addSelectionListener(
					new SelectionAdapter()
					{
						public void 
						widgetSelected(
							SelectionEvent e ) 
						{
							int index = ac_list.getSelectionIndex();
							
							if ( index != -1 ){
								
								String ac = ac_list.getItem( index );

								ac = trimAccount( ac );
								
								AccountConfig ac_config = account_config.remove( ac );
								
								if ( ac_config != null ){
									
									RemoteConnection rc = remote_connections.get( ac );

									if ( rc != null ){
										
										rc.destroy();
									}
									
									updateAccountList();
									
									saveConfig();
								}
							}
						}
					});
			
			options_group = new Group( main, SWT.NULL );
			Messages.setLanguageText( options_group, "xmwebui.rpc.options" );
			layout = new GridLayout();
			layout.numColumns = 6;

			options_group.setLayout(layout);
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalSpan = 4;
			options_group.setLayoutData(grid_data);

			Label label = new Label( options_group, SWT.NULL );
			Messages.setLanguageText( label, "xmwebui.rpc.options.desc" );

			description = new Text( options_group, SWT.BORDER );
			description.addListener(
				SWT.FocusOut, 
				new Listener() 
				{
			        public void handleEvent(Event event) {
			        	if ( current_account != null ){
							
							current_account.setDescription( description.getText());
							
							updateAccountList();
							
							saveConfig();
						}
			        }
			    });
			
			label = new Label( options_group, SWT.NULL );
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalSpan = 4;
			label.setLayoutData(grid_data);
			
			do_basic = new Button( options_group, SWT.CHECK );
			Messages.setLanguageText( do_basic, "xmwebui.rpc.options.enable.basic" );
			
			do_basic.addSelectionListener(
				new SelectionAdapter()
				{
					public void 
					widgetSelected(
						SelectionEvent e ) 
					{
						if ( current_account != null ){
							
							current_account.setBasicEnabled( do_basic.getSelection());
							
							updateSelectedAccount( current_account );
							
							saveConfig();
						}
					}
				});
			
			do_basic_def = new Button( options_group, SWT.CHECK );
			Messages.setLanguageText( do_basic_def, "xmwebui.rpc.options.basic.def.auth" );

			do_basic_def.addSelectionListener(
				new SelectionAdapter()
				{
					public void 
					widgetSelected(
						SelectionEvent e ) 
					{
						if ( current_account != null ){
							
							current_account.setBasicDefaults( do_basic_def.getSelection());
							
							updateSelectedAccount( current_account );
							
							saveConfig();
						}
					}
				});
			
			label = new Label( options_group, SWT.NULL );
			grid_data = new GridData();
			grid_data.horizontalIndent = 20;
			label.setLayoutData( grid_data );
			Messages.setLanguageText( label, "xmwebui.rpc.options.user" );

			
			basic_username = new Text( options_group, SWT.BORDER );
			
			basic_username.addListener(
				SWT.FocusOut, 
				new Listener() 
				{
			        public void handleEvent(Event event) {
			        	if ( current_account != null ){
							
							current_account.setBasicUser( basic_username.getText());
							
							updateSelectedAccount( current_account );
							
							saveConfig();
						}
			        }
			    });
			
			
			label = new Label( options_group, SWT.NULL );
			Messages.setLanguageText( label, "xmwebui.rpc.options.pw" );

			
			basic_password = new Text( options_group, SWT.BORDER );
			basic_password.setEchoChar( '*' );
			basic_password.addListener(
				SWT.FocusOut, 
				new Listener() 
				{
			        public void handleEvent(Event event) {
			        	if ( current_account != null ){
							
							current_account.setBasicPassword( basic_password.getText());
							
							updateSelectedAccount( current_account );
							
							saveConfig();
						}
			        }
			    });
				
			label = new Label( options_group, SWT.NULL );
			grid_data = new GridData();
			grid_data.horizontalSpan = 4;
			label.setLayoutData(grid_data);
			Messages.setLanguageText( label, "xmwebui.rpc.options.secure.pw" );
			
			force_proxy = new Button( options_group, SWT.CHECK );
			Messages.setLanguageText( force_proxy, "xmwebui.rpc.options.secure.force.proxy" );

			force_proxy.addSelectionListener(
				new SelectionAdapter()
				{
					public void 
					widgetSelected(
						SelectionEvent e ) 
					{
						if ( current_account != null ){
							
							current_account.setForceProxy( force_proxy.getSelection());
							
							updateSelectedAccount( current_account );
							
							saveConfig();
						}
					}
				});
			
			secure_password = new Text( options_group, SWT.BORDER );
			secure_password.setEchoChar( '*' );
			secure_password.addListener(
				SWT.FocusOut, 
				new Listener() 
				{
			        public void handleEvent(Event event) {
			        	if ( current_account != null ){
							
							current_account.setSecurePassword( secure_password.getText());
							
							updateSelectedAccount( current_account );
							
							saveConfig();
						}
			        }
			    });
			
			ac_list.addSelectionListener(
				new SelectionAdapter()
				{
					public void 
					widgetSelected(
						SelectionEvent e ) 
					{
						int index = ac_list.getSelectionIndex();
						
						
						if	( index == -1 ){
							
							setSelectedAccount( null );
							
						}else{
							
							String ac = ac_list.getItem( index );

							ac = trimAccount( ac );
							
							setSelectedAccount( account_config.get( ac ));
						}
					}
				});
			
				// operations
			
			operation_group = new Group( main, SWT.NULL );
			Messages.setLanguageText( operation_group, "xmwebui.rpc.operations" );
			layout = new GridLayout();
			layout.numColumns = 6;

			operation_group.setLayout(layout);
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalSpan = 4;
			operation_group.setLayoutData(grid_data);
			
				// status
			
			status_label = new Label( operation_group, SWT.NULL );
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalSpan = 6;
			status_label.setLayoutData(grid_data);
			Messages.setLanguageText( status_label, "xmwebui.rpc.constatus", new String[]{ CS_DISCONNECTED } );

			
				// launch ui
			
			Button launch_ui_button = new Button( operation_group, SWT.PUSH );
			Messages.setLanguageText( launch_ui_button, "xmwebui.rpc.launch.ui" );


			launch_ui_button.addSelectionListener(
					new SelectionAdapter()
					{
						public void 
						widgetSelected(
							SelectionEvent e ) 
						{
							RemoteConnection rc = getCurrentRemoteConnection();
							
							if ( rc != null ){
								
								URL url = rc.getProxyURL();
								
								Utils.launch( url.toExternalForm());
							}
						}
					});

				// status
			
			Button status_button = new Button( operation_group, SWT.PUSH );
			Messages.setLanguageText( status_button, "xmwebui.rpc.status" );


			status_button.addSelectionListener(
					new SelectionAdapter()
					{
						public void 
						widgetSelected(
							SelectionEvent event ) 
						{
							RemoteConnection rc = getCurrentRemoteConnection();
							
							if ( rc != null ){
								
								try{
									Map map = rc.getRPCStatus();
									
									log( map.toString());
									
								}catch( Throwable e ){
									
									logError( e );
								}
							}
						}
					});
			
				// disconnect
			
			Button disconnect_button = new Button( operation_group, SWT.PUSH );
			Messages.setLanguageText( disconnect_button, "xmwebui.rpc.disconnect" );

			disconnect_button.addSelectionListener(
					new SelectionAdapter()
					{
						public void 
						widgetSelected(
							SelectionEvent e ) 
						{
							RemoteConnection rc = getCurrentRemoteConnection();
							
							if ( rc != null ){
								
								rc.destroy();
							}
						}
					});

			label = new Label( operation_group, SWT.NULL );
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalSpan = 3;
			label.setLayoutData(grid_data);
			
				// command prompt
			
			label = new Label( operation_group, SWT.NULL );
			Messages.setLanguageText( label, "xmwebui.rpc.command" );

			final Text command = new Text( operation_group, SWT.BORDER );
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalSpan = 4;
			command.setLayoutData(grid_data);
			
			command.addKeyListener(
				new KeyListener()
				{
					private List<String> history = new ArrayList<String>();
					
					private int	history_index = 0;
					
					public void 
					keyPressed(
						KeyEvent e )
					{
					}
					
					public void 
					keyReleased(
						KeyEvent e )
					{
						int	code = e.keyCode;
						
						if ( code == SWT.ARROW_UP ){
						
							history_index--;
							
							if ( history_index < 0 ){
								
								history_index = 0;
							}
							
							if ( history_index < history.size()){
								
								command.setText( history.get(history_index));
								
								e.doit = false;
							}
						}else if ( code == SWT.ARROW_DOWN ){
						
							history_index++;
							
							if ( history_index > history.size()){
								
								history_index = history.size();
							}
							
							if ( history_index < history.size()){
								
								command.setText( history.get(history_index));
								
								e.doit = false;
							}
						}else if ( code == '\r' ){
						
							String str = command.getText().trim();
							
							command.setText( "" );
							
							if ( str.length() > 0 ){
								
								history.remove( str );
								
								history.add( str );
								
								if ( history.size() > 100 ){
									
									history.remove(0);
								}
								
								history_index = history.size();
								
								RemoteConnection rc = getCurrentRemoteConnection();
								
								if ( rc != null ){
								
									executeCommand( rc, str );
								}
							}
						}
					}
			    });
			
			new LinkLabel( operation_group, "xmwebui.link", MessageText.getString(  "xmwebui.commands.wiki.link" ));

			
				// log
			
			log = new StyledText( main,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
			grid_data = new GridData(GridData.FILL_BOTH);
			grid_data.horizontalSpan = 4;
			grid_data.verticalIndent = 10;
			log.setLayoutData(grid_data);
			log.setIndent( 4 );
			
			updateAccountList();
			
			initialised = true;
		}
		
		private void
		executeCommand(
			RemoteConnection		rc,
			String					str )
		{
			log( "> " + str );
			
			try{
				if ( str.equalsIgnoreCase( "status" )){
				
					log( "" + rc.getRPCStatus());
					
				}else if ( str.equalsIgnoreCase( "torrents" )){
					
					List<RemoteTorrent>	torrents = rc.getTorrents();
					
					int	pos = 1;
					
					for ( RemoteTorrent t: torrents ){
						
						log( (pos++) + ") " + t.getName());
					}
				}else{
					
					logError( "Unrecognized command '" + str + "'" );
				}
			}catch( Throwable e ){
				
				logError( e );
			}
		}
		
		private void
		setSelectedAccount(
			AccountConfig		_ac )
		{
			if ( _ac == current_account && initialised ){
				
				return;
			}
			
			current_account	= _ac;
			
			if ( current_account == null ){
				
				log( "No code selected" );
				
				connect_button.setEnabled( false );
				remove_button.setEnabled( false );
				setEnabled( options_group, false );
				setEnabled( operation_group, false );
				
				if ( ac_list.getSelectionIndex() != -1 ){
				
					ac_list.deselectAll();
				}
			}else{
				
				log( "Code '" + current_account.getAccessCode() + "' selected" );
				
				connect_button.setEnabled( true );
				remove_button.setEnabled( true );
				setEnabled( options_group, true );
	
				RemoteConnection rc = remote_connections.get( current_account.getAccessCode());
				
				setEnabled( operation_group, rc != null && rc.isConnected());
				
				String[]	items = ac_list.getItems();
				
				boolean	found = false;
				
				for ( int i=0;i<items.length;i++ ){
					
					String ac = trimAccount( items[i] );
					
					if ( ac.equals( current_account.getAccessCode())){
						
						if ( ac_list.getSelectionIndex() != i ){
						
							ac_list.select( i );
						}
						
						found = true;
					}
				}
				
				if ( !found ){
					
					ac_list.deselectAll();
				}
			
				updateSelectedAccount( current_account );
			}
		}
		
		private void
		updateSelectedAccount(
			AccountConfig	ac )
		{
			boolean	basic_enable 		= ac.isBasicEnabled();
			boolean	basic_defs 			= ac.isBasicDefaults();
			String	basic_user_str		= ac.getBasicUser();
			
			description.setText( ac.getDescription());
			do_basic.setSelection( basic_enable );
			do_basic_def.setSelection( basic_defs );
			basic_username.setText( basic_user_str );
			basic_password.setText( "xxx" );
			force_proxy.setSelection( ac.isForceProxy());
			secure_password.setText( "xxx" );
			
			if ( basic_enable ){
				
				do_basic_def.setEnabled( true );
				
				basic_username.setEnabled( !basic_defs );
				basic_password.setEnabled( !basic_defs );
			}else{
				
				do_basic_def.setEnabled( false );
				basic_username.setEnabled( false );
				basic_password.setEnabled( false );
			}
		}
		
		private String
		trimAccount(
			String	ac )
		{
			int	pos = ac.indexOf( '-' );
			
			if ( pos != -1 ){
				
				ac = ac.substring( 0, pos ).trim();
			}
			
			return( ac );
		}
		
		private void
		updateAccountList()
		{
			int index = ac_list.getSelectionIndex();

			String selected_account;
			
			if ( index == -1 ){
				
				selected_account = null;
				
			}else{
				
				selected_account = trimAccount( ac_list.getItems()[ index ]);
			}
			
			ac_list.removeAll();
			
			List<String> acs = new ArrayList<String>();
			
			for ( AccountConfig c: account_config.values()){
				
				String ac = c.getAccessCode();
				
				String desc = c.getDescription();
				
				if ( desc.length() > 0 ){
					
					ac += " - " + desc;
				}
				
				acs.add( ac );
			}
			
			Collections.sort( acs );
				
			index = -1;
			
			for ( int i=0; i<acs.size(); i++){
				
				String ac = acs.get(i);
			
				if ( selected_account != null && selected_account.equals( trimAccount( ac ))){
					
					index = i;
				}
				
				ac_list.add( ac );
			}
			
			ac_list.getParent().layout( true );
			
			if ( index == -1 ){
				
				setSelectedAccount( null );
				
			}else{
				
				ac_list.select( index );
				
				setSelectedAccount( account_config.get( selected_account ));
			}
		}
		
		private void
		updateRemoteConnection(
			final RemoteConnection	rc,
			final boolean			is_connected )
		{
			Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						if ( current_account != null ){
							
							if ( current_account.getAccessCode().equals( rc.getAccessCode())){
								
								setEnabled( operation_group, is_connected );
								
								String	text = "";
								
								if ( is_connected ){
									
									text = rc.getConnectionStatus();
									
								}else{
									
									text = CS_DISCONNECTED;
								}
								
								Messages.setLanguageText( status_label, "xmwebui.rpc.constatus", new String[]{ text } );
							}
						}
					}
				});
		}
		
		private RemoteConnection
		getCurrentRemoteConnection()
		{
			if ( current_account == null ){
				
				return( null );
			}
			
			synchronized( remote_connections ){
				
				return( remote_connections.get( current_account.getAccessCode()));
			}
		}
		
		private void
		setEnabled(
			Composite		comp,
			boolean			enable )
		{
			comp.setEnabled( enable );
			
			for ( Control c: comp.getChildren()){
				
				if ( c instanceof Composite ){
					
					setEnabled((Composite)c,enable );
					
				}else{
					
					c.setEnabled( enable );
				}
			}
		}
		
		private void
		destroy()
		{
		}

	
		protected void
		print(
			String		str,
			Throwable	e )
		{
			print( str + ": " + Debug.getNestedExceptionMessage( e ), LOG_ERROR, false );
		}
		
		protected void
		print(
			String		str )
		{
			print( str, LOG_NORMAL, false );
		}
	
		protected void
		print(
			final String		str,
			final int			log_type,
			final boolean		clear_first )
		{	
			if ( !log.isDisposed()){
	
				final int f_log_type = log_type;
	
				log.getDisplay().asyncExec(
						new Runnable()
						{
							public void
							run()
							{
								if ( log.isDisposed()){
	
									return;
								}
	
								int	start;
	
								if ( clear_first ){
	
									start	= 0;
	
									log.setText( str + "\n" );
	
								}else{
	
									String	text = log.getText();
									
									start = text.length();
	
									if ( start > 32000 ){
										
										log.replaceTextRange( 0, 1024, "" );
										
										start = log.getText().length();
									}
									
									log.append( str + "\n" );
								}
	
								Color 	color;
	
								if ( f_log_type == LOG_NORMAL ){
	
									color = Colors.black;
	
								}else if ( f_log_type == LOG_SUCCESS ){
	
									color = Colors.green;
	
								}else{
	
									color = Colors.red;
								}
	
								if ( color != Colors.black ){
									
									StyleRange styleRange = new StyleRange();
									styleRange.start = start;
									styleRange.length = str.length();
									styleRange.foreground = color;
									log.setStyleRange(styleRange);
								}
								
								log.setSelection( log.getText().length());
							}
						});
			}
		}
	}
	
	private class
	AccountConfig
	{
		private String		access_code;
		private String		description;
		private boolean		basic_enabled;
		private boolean		basic_defs;
		private String		basic_user;
		private String		basic_password;
		private boolean		force_proxy;
		private String		secure_password;
		
		private
		AccountConfig(
			String	_ac )
		{
			access_code		= _ac;
			basic_enabled	= true;
			basic_defs		= true;
			basic_user		= "vuze";
			basic_password	= "";
			secure_password	= "";
		}
		
		private
		AccountConfig(
			Map		map )
		
			throws IOException
		{
			access_code 	= ImportExportUtils.importString( map, "ac" );
			description 	= ImportExportUtils.importString( map, "desc", "" );
			basic_enabled 	= ImportExportUtils.importBoolean( map, "basic_enable", true );
			basic_defs 		= ImportExportUtils.importBoolean( map, "basic_defs", true );
			basic_user 		= ImportExportUtils.importString( map, "basic_user", "vuze" );
			basic_password 	= ImportExportUtils.importString( map, "basic_password", "" );
			force_proxy 	= ImportExportUtils.importBoolean( map, "force_proxy", false );
			secure_password = ImportExportUtils.importString( map, "secure_password", "" );
		}
		
		private Map
		export()
		
			throws IOException
		{
			Map	map = new HashMap();
			
			ImportExportUtils.exportString( map, "ac", access_code );
			ImportExportUtils.exportString( map, "desc", description );
			ImportExportUtils.exportBoolean( map, "basic_enable", basic_enabled );
			ImportExportUtils.exportBoolean( map, "basic_defs", basic_defs );
			ImportExportUtils.exportString( map, "basic_user", basic_user );
			ImportExportUtils.exportString( map, "basic_password", basic_password );
			ImportExportUtils.exportBoolean( map, "force_proxy", force_proxy );
			ImportExportUtils.exportString( map, "secure_password", secure_password );

			return( map );
		}
		
		public String
		getAccessCode()
		{
			return( access_code );
		}
		
		public void
		setDescription(
			String	str )
		{
			description = str;
		}
		
		public String
		getDescription()
		{
			return( description );
		}
		
		public void
		setBasicEnabled(
			boolean	e )
		{
			basic_enabled	= e;
		}
		
		public boolean
		isBasicEnabled()
		{
			return( basic_enabled );
		}
		
		public void
		setBasicDefaults(
			boolean	e )
		{
			basic_defs	= e;
		}
		
		public boolean
		isBasicDefaults()
		{
			return( basic_defs );
		}
		
		public void
		setBasicUser(
			String	str )
		{
			basic_user	= str;
		}
		
		public String
		getBasicUser()
		{
			return( basic_user );
		}
		
		public void
		setBasicPassword(
			String	str )
		{
			basic_password	= str;
		}
		
		public String
		getBasicPassword()
		{
			return( basic_password );
		}
		
		public void
		setForceProxy(
			boolean	b )
		{
			force_proxy = b;
		}
		
		public boolean
		isForceProxy()
		{
			return( force_proxy );
		}
		
		public String
		getSecureUser()
		{
			return( "vuze" );
		}
		
		public void
		setSecurePassword(
			String		str )
		{
			secure_password = str;
		}
		
		public String
		getSecurePassword()
		{
			return( secure_password );
		}
	}
	
	private class
	RemoteConnection
	{
		private AccountConfig		account;
		 
		private XMRPCClient			rpc;
		private XMClientProxy		proxy;
		
		private String				connection_state = CS_DISCONNECTED;
		
		private List<RemoteTorrent>	last_torrents;
		
		private
		RemoteConnection(
			AccountConfig	_ac )
		{
			account = _ac;
		}
		
		private String
		getAccessCode()
		{
			return( account.getAccessCode());
		}
		
		private void
		connect()
		{
			String	ac = account.getAccessCode();
			
			try{
				List<PairedService> services = PairingManagerFactory.getSingleton().lookupServices( account.getAccessCode());
				
				PairedService	target_service 	= null;
				boolean			has_tunnel		= false;
				
				for ( PairedService service: services ){
					
					String sid = service.getSID();
					
					if ( sid.equals( "tunnel" )){
						
						has_tunnel = true;
						
					}else if ( sid.equals(  "xmwebui" )){
						
						target_service = service;
					}
				}
				
				if ( target_service != null ){
						
					log( "Pairing details obtained for '" + ac + "', supports secure connections=" + has_tunnel );

					connect( target_service, has_tunnel );
					
				}else{
				
					logError( "No binding found for Vuze Web Remote, '" + ac + "'" );
				}
				
			}catch( Throwable e ){
				
				logError( "Pairing details unavailable for '" + ac + "': " + Debug.getNestedExceptionMessage( e ));
			}
		}
		
		private void
		connect(
			PairedService	service,
			boolean			has_tunnel )
		{
			String	ac = account.getAccessCode();
			
			try{
				PairingConnectionData cd = service.getConnectionData();
				
				boolean http 	= cd.getAttribute( "protocol" ).equals( "http" );
				String	host	= cd.getAttribute( "ip" );
				int		port	= Integer.parseInt( cd.getAttribute( "port" ));
				
				if ( account.isBasicEnabled() ){
					
					log( "Attempting direct basic connection" );
				
					XMRPCClient rpc_direct = null;
					
					try{
						String	user 		= "vuze";
						String	password	= ac;
						
						if ( !account.isBasicDefaults()){
							
							user 		= account.getBasicUser();
							password	= account.getBasicPassword();
						}
						
						rpc_direct = XMRPCClientFactory.createDirect( http, host, port, user, password );
				
						logConnect( CS_BASIC, getRPCStatus( rpc_direct ));
						
						rpc = rpc_direct;
											
					}catch( Throwable e ){
						
						if ( rpc_direct != null ){
							
							rpc_direct.destroy();
						}
						
						logError( "    Failed: " + Debug.getNestedExceptionMessage( e ));
					}
				}
				
				if ( rpc == null ){
					
					if ( has_tunnel ){
						
						if ( rpc == null && !account.isForceProxy()){

							String	tunnel_server = (http?"http":"https") + "://" + host + ":" + port + "/";

							log( "Attempting direct secured connection: " + tunnel_server );
						
							XMRPCClient rpc_tunnel = null;
							
							try{
								
								String user 		= account.getSecureUser();
								String password		= account.getSecurePassword();
								
								rpc_tunnel = XMRPCClientFactory.createTunnel( tunnel_server, ac, user, password );
						
								logConnect( CS_SECURE_DIRECT, getRPCStatus( rpc_tunnel ));
								
								rpc = rpc_tunnel;
																			
							}catch( Throwable e ){
								
								if ( rpc_tunnel != null ){
									
									rpc_tunnel.destroy();
								}
							
								logError( "    Failed: " + Debug.getNestedExceptionMessage( e ));
							}
						}
						
						if ( rpc == null ){
							
							String	tunnel_server = "https://pair.vuze.com/";
			
							log( "Attempting proxy secured connection: " + tunnel_server );
						
							XMRPCClient rpc_tunnel = null;
							
							try{
								String user 		= account.getSecureUser();
								String password		= account.getSecurePassword();
			
								rpc_tunnel = XMRPCClientFactory.createTunnel( tunnel_server, ac, user, password );
						
								logConnect( CS_SECURE_PROXIED, getRPCStatus( rpc_tunnel ));
								
								rpc = rpc_tunnel;
																			
							}catch( Throwable e ){
								
								if ( rpc_tunnel != null ){
									
									rpc_tunnel.destroy();
								}
							
								logError( "    Failed: " + Debug.getNestedExceptionMessage( e ));
							}
						}
					}else{
						
						logError( "    Failed - secure connections not enabled in remote Vuze or client is still initialising" );
					}
				}
			}finally{
			
				if ( rpc == null ){
			
					connection_state = CS_DISCONNECTED;

					setConnected( this, false );
				
				}else{
					
					setConnected( this, true );
				}
			}
		}
		
		private String
		getConnectionStatus()
		{
			return( connection_state );
		}
		
		private void
		logConnect(
			String	type,
			Map		args )
		{
			connection_state = type;
			
			String	rem_az_version 		= (String)args.get( "az-version" );
			String	rem_plug_version 	= (String)args.get( "version" );
			
			log( "    Connected: Remote Vuze version=" + rem_az_version + ", plugin=" + rem_plug_version );

		}
		private URL
		getProxyURL()
		{
			if ( rpc == null ){
				
				return( null );
			}
			
			if ( proxy == null ){
			
				try{
					proxy = new XMClientProxy( plugin.getResourceDir(), rpc );
					
					log( "Created client proxy on port " + proxy.getPort());
					
				}catch( Throwable e ){
					
					logError( "Failed to create rpc proxy: " + Debug.getNestedExceptionMessage( e ));
					
					return( null );
				}
			}
			
			try{
				return( new URL( "http://" + proxy.getHostName() + ":" + proxy.getPort() + "/" ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				return( null );
			}
		}
		
		private boolean
		isConnected()
		{
			return( rpc != null );
		}
		
		private Map
		getRPCStatus()
		
			throws XMRPCClientException
		{
			if ( rpc == null ){
				
				throw( new XMRPCClientException( "RPC not connected" ));
			}
			
			return( getRPCStatus( rpc ));
		}
		
		private Map
		getRPCStatus(
			XMRPCClient		rpc )
		
			throws XMRPCClientException
		{
			JSONObject	request = new JSONObject();
			
			request.put( "method", "session-get" );
			
			JSONObject reply = rpc.call( request );
			
			String result = (String)reply.get( "result" );
			
			if ( result.equals( "success" )){
				
				Map	args = (Map)reply.get( "arguments" );
							
				return( args );
				
			}else{
				
				throw( new XMRPCClientException( "RPC call failed: " + result ));
			}
		}
		
		private List<RemoteTorrent>
		getTorrents()
		
			throws XMRPCClientException
		{
			JSONObject	request = new JSONObject();
			
			request.put( "method", "torrent-get" );
			
			Map request_args = new HashMap();
			
			request.put( "arguments", request_args );
			
			List fields = new ArrayList();
			
			request_args.put( "fields", fields );
			
			fields.add( "name" );
			
			JSONObject reply = rpc.call( request );
			
			String result = (String)reply.get( "result" );
			
			if ( result.equals( "success" )){
				
				Map	args = (Map)reply.get( "arguments" );
					
				List<Map> torrent_maps = (List<Map>)args.get( "torrents" );
				
				List<RemoteTorrent> torrents = new ArrayList<RemoteTorrent>();
				
				for ( Map m: torrent_maps ){
					
					torrents.add( new RemoteTorrent( m ));
				}
				
				last_torrents = torrents;
				
				return( torrents );
				
			}else{
				
				throw( new XMRPCClientException( "RPC call failed: " + result ));
			}
		}
		
		private List<RemoteTorrent>
		getLastTorrents()
		{
			return( last_torrents );
		}
		
		private void
		destroy()
		{
			if ( rpc != null ){
		
				String	ac = account.getAccessCode();

				if ( proxy != null ){
					
					proxy.destroy();
					
					proxy = null;
				}
							
				rpc.destroy();
				
				rpc = null;
			
				log( "Disconnected '" + ac + "'" );

				connection_state = CS_DISCONNECTED;
				
				setConnected( this, false );
			}
		}
	}
	
	private class
	RemoteTorrent
	{
		private Map		map;
		private
		RemoteTorrent(
			Map		_m )
		{
			map = _m;
		}
		
		private String
		getName()
		{
			return( (String)map.get( "name" ));
		}
	}
}