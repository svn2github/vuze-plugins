/*
 * Created on Jan 29, 2010
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


package com.vuze.plugins.mlab;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.util.JSONUtils;
import com.vuze.plugins.mlab.tools.ndt.Tcpbw100;
import com.vuze.plugins.mlab.tools.ndt.swingemu.Tcpbw100UIWrapper;
import com.vuze.plugins.mlab.tools.ndt.swingemu.Tcpbw100UIWrapperListener;
import com.vuze.plugins.mlab.ui.MLabVzWizard;
import com.vuze.plugins.mlab.ui.MLabWizard;

public class 
MLabPlugin
	implements UnloadablePlugin
{
	private PluginInterface		plugin_interface;
	private LoggerChannel		logger;

	private LocaleUtilities 		loc_utils;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	
	private ActionParameter ndt_button;
	
	//private ActionParameter sp_button; 
	
	private boolean test_active;
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		logger				= plugin_interface.getLogger().getChannel( "MLab" ); 

		logger.setDiagnostic();
		
		logger.setForce( true );
		
		loc_utils = plugin_interface.getUtilities().getLocaleUtilities();

		loc_utils.integrateLocalisedMessageBundle( "com.vuze.plugins.mlab.internat.Messages" );

		UIManager	ui_manager	= plugin_interface.getUIManager();
		
		view_model = ui_manager.createBasicPluginViewModel( "mlab.name" );

		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		logger.addListener(
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
						if ( str.length() > 0 ){
							
							view_model.getLogArea().appendText( str + "\n" );
						}
						
						StringWriter sw = new StringWriter();
						
						PrintWriter	pw = new PrintWriter( sw );
						
						error.printStackTrace( pw );
						
						pw.flush();
						
						view_model.getLogArea().appendText( sw.toString() + "\n" );
					}
				});		
		
		config_model = ui_manager.createBasicPluginConfigModel( "mlab.name" );

		view_model.setConfigSectionID( "mlab.name" );
		
		config_model.addLabelParameter2( "mlab.info" );
		config_model.addHyperlinkParameter2( "mlab.link", loc_utils.getLocalisedMessageText( "mlab.link.url" ));
		
		ndt_button = config_model.addActionParameter2( "mlab.tool.ndt", "mlab.run" );
		
		ndt_button.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					runNDT( null );
				}
			});
	}
	
	public ToolRun
	runNDT(
		final ToolListener	listener )
	{
		final ToolRun run = new ToolRunImpl();
		
		final AESemaphore	sem = new AESemaphore( "waiter" );
		
		runTool(
			ndt_button,
			new Runnable()
			{
				public void
				run()
				{
					boolean	completed = false;
					
					try{
						logger.log( "Starting NDT Test" );
						logger.log( "-----------------" );
						
						new Tcpbw100UIWrapper(
							new Tcpbw100UIWrapperListener()
							{
								private LinkedList<String>	history = new LinkedList<String>();
								
								public void
								reportSummary(
									String		str )
								{
									str = str.trim();
									
									log( str );
									
									if ( listener != null ){
									
										if ( !str.startsWith( "Click" )){
										
											listener.reportSummary( str );
										}
									}
								}
								
								public void
								reportDetail(
									String		str )
								{
									str = str.trim();
									
									log( str );
									
									if ( listener != null ){
										
										listener.reportDetail( str );
									}
								}
								
								private void
								log(
									String	str )
								{
									synchronized( history ){
										
										if ( history.size() > 0 && history.getLast().equals( str )){
											
											return;
										}
										
										history.add( str );
									}
									
									logger.log( str );
								}
						
							});
						
						// String host = "ndt.iupui.donar.measurement-lab.org";
						// String host = "jlab4.jlab.org";

						// on 2014/01/14 (or maybe before) things stopped working with the above. Found server below
						// still running 3.6.4. Unfortunately when I tested the latest client code against servers
						// allegedly running compatible server code it didn't work... ;(
						
						// reply on mailing list to above issue:
						
						// The first is to switch to our new name server, ns.measurementlab.net. For example: http://ns.measurementlab.net/ndt will return a JSON string with the closest NDT server. Example integration can be found on www.measurementlab.net/p/ndt.html
						// The other option, discouraged, is to continue using donar which should still be resolving. It just uses ns.measurementlab.net on the backend now. However, this is currently down according to my tests, so we'll work on getting this back as soon as possible.
						
						String server_host = null;
						
						try{
						
							InputStream is = plugin_interface.getUtilities().getResourceDownloaderFactory().create( new URL( "http://ns.measurementlab.net/ndt?format=json" )).download();
							
							Map map = JSONUtils.decodeJSON( FileUtil.readInputStreamAsString( is, 32*1024, "UTF-8" ));
							
							URL url = new URL((String)map.get( "url" ));
							
							server_host = url.getHost();
							
							logger.log( "Selected server: " + server_host );
							
						}catch( Throwable e ){
							
							Debug.out( "Failed to get server", e );
						}
						
						
						if ( server_host == null ){
						
								// fallback to old, discouraged approach
							
							server_host = "ndt.iupui.donar.measurement-lab.org";
							
							logger.log( "Failed to select server, falling back to donar method" );
						}
						
						final Tcpbw100 test = Tcpbw100.mainSupport( new String[]{ server_host });
						
						run.addListener(
							new ToolRunListener()
							{
								public void
								cancelled()
								{
									test.killIt();
								}
							});
						
						sem.release();
						
						test.runIt();
						
						long	up_bps = 0;
						
						try{
							up_bps = (long)(Double.parseDouble( test.get_c2sspd())*1000000)/8;
							
						}catch( Throwable e ){
						}
						
						long	down_bps = 0;
						
						try{
							down_bps = (long)(Double.parseDouble( test.get_s2cspd())*1000000)/8;
							
						}catch( Throwable e ){
						}
						
						logger.log( "" );
						
						String	result_str;
						
						if ( up_bps == 0 || down_bps == 0 ){
							
							result_str  = "No results were received. Either the test server is unavailable or network problems are preventing the test from running correctly. Please try again.";
							
						}else{
							
							result_str = 	
								"Completed: up=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( up_bps ) +
								", down=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( down_bps );
						}
						
						logger.log( result_str );
						
						completed = true;
						
						if ( listener != null ){
							
							listener.reportSummary( result_str );
							listener.reportDetail( result_str );
							
							Map<String,Object>	results = new HashMap<String, Object>();
							
							results.put( "up", up_bps );
							results.put( "down", down_bps );
							
							listener.complete( results );
						}
					}finally{
						
						sem.release();
						
						if ( !completed && listener != null ){
							
							listener.complete( new HashMap<String, Object>());
						}
					}
				}
			});
		
		sem.reserve();
		
		return( run );
	}
	
	protected void
	runTool(
		final ActionParameter		ap,
		final Runnable				target )
	{
		ap.setEnabled( false );
		
		new AEThread2( "toolRunner" )
		{
			public void
			run()
			{
				try{
					target.run();
					
				}finally{
					
					ap.setEnabled( true );
				}
			}
		}.start();
	}
	
	public String
	getLocalisedText(
		String		key )
	{
		return( loc_utils.getLocalisedMessageText( key ));
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	public void
	unload()
	{
		if ( config_model != null ){
			
			config_model.destroy();
		}
		
		if ( view_model != null ){
			
			view_model.destroy();
		}
	}
	
	public void
	runTest(
		final Map<String,Object>		args,
		final IPCInterface		callback,
		final boolean 			autoApply )
	
		throws IPCException
	{
		synchronized( this ){
			
			if ( test_active ){
				
				throw( new IPCException( "Test already active" ));
			}
			
			plugin_interface.getPluginProperties().put( "plugin.unload.disabled", "true" );
			
			test_active = true;
		}
		
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					IPCInterface wrapper = 
						new IPCInterface()
						{
							public Object 
							invoke(
								String methodName, 
								Object[] params ) 
							
								throws IPCException
							{
								synchronized( MLabPlugin.this ){
									
									test_active = false;
									
									plugin_interface.getPluginProperties().put( "plugin.unload.disabled", "false" );
								}
								
								return( callback == null ? null : callback.invoke( methodName, params ));
							}
						
							public boolean 
							canInvoke( 
								String methodName, 
								Object[] params )
							{
								return( callback == null ? true : callback.canInvoke( methodName, params ));
							}
						};
					
					try{
						if ( autoApply ){
							
							MLabVzWizard wizard = new MLabVzWizard( MLabPlugin.this, wrapper, args);
							
							wizard.open();
							
						}else{
							
							new MLabWizard( MLabPlugin.this, wrapper );
						}
						
					}catch( Throwable e ){
						
						try{
							wrapper.invoke( "error", new Object[]{ e } );
							
						}catch( Throwable f ){
						}
						
						Debug.out( e );
					}
				}
			});
	}
	
	public interface
	ToolRun
	{
		public void
		cancel();
		
		public void
		addListener(
			ToolRunListener		l );
	}
	
	private class
	ToolRunImpl
		implements ToolRun
	{
		private List<ToolRunListener>	listeners = new ArrayList<ToolRunListener>();
		private boolean	cancelled;
		
		public void 
		cancel() 
		{
			List<ToolRunListener> copy;
			
			synchronized( this ){
			
				cancelled = true;
				
				copy = new ArrayList<ToolRunListener>( listeners );
			}
			
			for ( ToolRunListener l: copy ){
				
				try{
					l.cancelled();
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		public void
		addListener(
			ToolRunListener		l )
		{
			boolean	inform = false;
			
			synchronized( this ){
				
				inform = cancelled;
				
				listeners.add( l );
			}
			
			if ( inform ){
				
				try{
					l.cancelled();
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
	}
	
	public interface
	ToolRunListener
	{
		public void
		cancelled();
	}
	
	public interface
	ToolListener
	{
		public void
		reportSummary(
			String		str );
		
		public void
		reportDetail(
			String		str );
		
		public void
		complete(
			Map<String,Object>	results );
	}
}
