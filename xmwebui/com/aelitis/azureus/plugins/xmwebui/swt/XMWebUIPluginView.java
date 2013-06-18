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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
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
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIInstance;

import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.pairing.PairedService;
import com.aelitis.azureus.core.pairing.PairingConnectionData;
import com.aelitis.azureus.core.pairing.PairingManagerFactory;
import com.aelitis.azureus.plugins.xmwebui.XMWebUIPlugin;
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


	private XMWebUIPlugin		plugin;
	private UISWTInstance		ui_instance;
	
	private Map<String,AccountConfig>	account_config = new HashMap<String,AccountConfig>();
	
	private Map<String,RemoteConnection>	remote_connections = new HashMap<String, RemoteConnection>();
	
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
	log(
		String	str )
	{
		if ( current_instance != null ){
			
			current_instance.print( str );
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
		
		private Group 			account_group;
		private Button 			do_basic;
		private Button			do_basic_def;
		private Text			basic_username;
		private Text			basic_password;
		private Text			secure_password;
		
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

			Label add_label = new Label( main, SWT.NULL );
			
			add_label.setText( "Enter a new remote Vuze access code" );
			
			final Text	ac_text = new Text( main, SWT.BORDER );
						
			final Button 	add_button = new Button( main, SWT.PUSH );
			
			add_button.setText( "Add" );

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
			
			sel_label.setText( "Select an existing access code" );
			
			ac_list = new Combo( main, SWT.SINGLE | SWT.READ_ONLY );
						
			connect_button = new Button( main, SWT.PUSH );
			
			connect_button.setText( "Connect" );
			
			connect_button.addSelectionListener(
				new SelectionAdapter()
				{
					public void 
					widgetSelected(
						SelectionEvent e ) 
					{
						int index = ac_list.getSelectionIndex();
						
						String ac = ac_list.getItem( index );
						
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
				});
			
			connect_button.setEnabled( false );
			
			remove_button = new Button( main, SWT.PUSH );
			
			remove_button.setText( "Remove" );

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
			
			account_group = new Group( main, SWT.NULL );
			account_group.setText( "Options" );
			layout = new GridLayout();
			layout.numColumns = 6;

			account_group.setLayout(layout);
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalSpan = 4;
			account_group.setLayoutData(grid_data);

			do_basic = new Button( account_group, SWT.CHECK );
			do_basic.setText( "Enable basic connection" );
			
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
			
			do_basic_def = new Button( account_group, SWT.CHECK );
			do_basic_def.setText( "Use default authentication" );

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
			
			Label label = new Label( account_group, SWT.NULL );
			
			label.setText( "Username" );
			
			basic_username = new Text( account_group, SWT.BORDER );
			
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
			
			
			label = new Label( account_group, SWT.NULL );
			
			label.setText( "Password" );
			
			basic_password = new Text( account_group, SWT.BORDER );
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
				
			label = new Label( account_group, SWT.NULL );
			grid_data = new GridData();
			grid_data.horizontalSpan = 5;
			label.setLayoutData(grid_data);

			label.setText( "Secure password - this requires secure pairing to be enabled on the remote Vuze" );
			
			secure_password = new Text( account_group, SWT.BORDER );
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

							setSelectedAccount( account_config.get( ac ));
						}
					}
				});
			
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
				setEnabled( account_group, false );
				
				if ( ac_list.getSelectionIndex() != -1 ){
				
					ac_list.deselectAll();
				}
			}else{
				
				log( "Code '" + current_account.getAccessCode() + "' selected" );
				
				connect_button.setEnabled( true );
				remove_button.setEnabled( true );
				setEnabled( account_group, true );
	
				String[]	items = ac_list.getItems();
				
				boolean	found = false;
				
				for ( int i=0;i<items.length;i++ ){
					
					if ( items[i].equals( current_account.getAccessCode())){
						
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
			boolean	basic_enable 	= ac.isBasicEnabled();
			boolean	basic_defs 		= ac.isBasicDefaults();
			String	basic_user_str		= ac.getBasicUser();
			String	basic_pw_str		= ac.getBasicPassword();
			
			do_basic.setSelection( basic_enable );
			do_basic_def.setSelection( basic_defs );
			basic_username.setText( basic_user_str );
			basic_password.setText( basic_pw_str );
			
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
		
		private void
		updateAccountList()
		{
			ac_list.removeAll();
			
			List<String> acs = new ArrayList<String>( account_config.keySet());
			 
			Collections.sort( acs );
					
			for ( String ac: acs ){
				
				ac_list.add( ac );
			}
			
			int index = ac_list.getSelectionIndex();
			
			if ( index == -1 ){
				
				setSelectedAccount( null );
				
			}else{
				
				setSelectedAccount( account_config.get( ac_list.getItems()[ index ]));
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
		private boolean		basic_enabled;
		private boolean		basic_defs;
		private String		basic_user;
		private String		basic_password;
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
			basic_enabled 	= ImportExportUtils.importBoolean( map, "basic_enable", true );
			basic_defs 		= ImportExportUtils.importBoolean( map, "basic_defs", true );
			basic_user 		= ImportExportUtils.importString( map, "basic_user", "vuze" );
			basic_password 	= ImportExportUtils.importString( map, "basic_password", "" );
			secure_password = ImportExportUtils.importString( map, "secure_password", "" );
		}
		
		private Map
		export()
		
			throws IOException
		{
			Map	map = new HashMap();
			
			ImportExportUtils.exportString( map, "ac", access_code );
			ImportExportUtils.exportBoolean( map, "basic_enable", basic_enabled );
			ImportExportUtils.exportBoolean( map, "basic_defs", basic_defs );
			ImportExportUtils.exportString( map, "basic_user", basic_user );
			ImportExportUtils.exportString( map, "basic_password", basic_password );
			ImportExportUtils.exportString( map, "secure_password", secure_password );

			return( map );
		}
		
		public String
		getAccessCode()
		{
			return( access_code );
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
		 
		private
		RemoteConnection(
			AccountConfig	_ac )
		{
			account = _ac;
		}
		
		private void
		connect()
		{
			String	ac = account.getAccessCode();
			
			try{
				List<PairedService> services = PairingManagerFactory.getSingleton().lookupServices( account.getAccessCode());
				
				for ( PairedService service: services ){
					
					if ( service.getSID().equals( "xmwebui" )){
						
						connect( service );
						
						return;
					}
				}
				
				logError( "No binding found for Vuze Web Remote, '" + ac + "'" );
				
			}catch( Throwable e ){
				
				logError( "Pairing details unavailable for '" + ac + "': " + Debug.getNestedExceptionMessage( e ));
			}
		}
		
		private void
		connect(
			PairedService	service )
		{
			String	ac = account.getAccessCode();

			log( "Pairing details obtained for '" + ac + "'" );
			
			PairingConnectionData cd = service.getConnectionData();
			
			boolean http 	= cd.getAttribute( "protocol" ).equals( "http" );
			String	host	= cd.getAttribute( "ip" );
			int		port	= Integer.parseInt( cd.getAttribute( "port" ));
			
			if ( account.isBasicEnabled() ){
				
				log( "Attempting direct basic connection" );
			
				try{
					String	user 		= "vuze";
					String	password	= ac;
					
					if ( !account.isBasicDefaults()){
						
						user 		= account.getBasicUser();
						password	= account.getBasicPassword();
					}
					
					XMRPCClient rpc = XMRPCClientFactory.createDirect( http, host, port, user, password );
			
					getRPCStatus( rpc );
					
					log( "    success" );
					
					rpc.destroy();
					
				}catch( Throwable e ){
					
					logError( "    failed: " + Debug.getNestedExceptionMessage( e ));
				}
			}
			
			if ( true ){
				
				String	tunnel_server = (http?"http":"https") + "://" + host + ":" + port + "/";

				log( "Attempting direct secured connection: " + tunnel_server );
			
				try{
					
					String user 		= account.getSecureUser();
					String password		= account.getSecurePassword();
					
					XMRPCClient rpc = XMRPCClientFactory.createTunnel( tunnel_server, ac, user, password );
			
					getRPCStatus( rpc );
					
					log( "    success" );
					
					rpc.destroy();
					
				}catch( Throwable e ){
					
					logError( "    failed: " + Debug.getNestedExceptionMessage( e ));
				}
			}
			
			if ( true ){
				
				String	tunnel_server = "https://pair.vuze.com/";

				log( "Attempting indirect secured connection: " + tunnel_server );
			
				try{
					String user 		= account.getSecureUser();
					String password		= account.getSecurePassword();

					XMRPCClient rpc = XMRPCClientFactory.createTunnel( tunnel_server, ac, user, password );
			
					getRPCStatus( rpc );
					
					log( "    success" );
					
					rpc.destroy();
					
				}catch( Throwable e ){
					
					logError( "    failed: " + Debug.getNestedExceptionMessage( e ));
				}
			}
			
			remote_connections.remove( ac );
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
			
				String	rem_az_version 		= (String)args.get( "az-version" );
				String	rem_plug_version 	= (String)args.get( "version" );
				
				log( "RPC OK: " + rem_az_version + "/" + rem_plug_version );
				
				return( args );
				
			}else{
				
				throw( new XMRPCClientException( "RPC call failed: " + result ));
			}
		}
		
		private void
		destroy()
		{
			
		}
	}
}
