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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.*;
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
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestWizard;

import com.vuze.plugins.mlab.tools.ndt.Tcpbw100;
import com.vuze.plugins.mlab.tools.ndt.swingemu.Tcpbw100UIWrapper;
import com.vuze.plugins.mlab.tools.ndt.swingemu.Tcpbw100UIWrapperListener;
import com.vuze.plugins.mlab.tools.shaperprobe.ShaperProbe;
import com.vuze.plugins.mlab.tools.shaperprobe.ShaperProbeListener;
import com.vuze.plugins.mlab.ui.MLabWizard;

public class 
MLabPlugin
	implements UnloadablePlugin
{
	private PluginInterface		plugin_interface;
	private LoggerChannel		logger;

	private ActionParameter ndt_button;
	private ActionParameter sp_button; 
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		logger				= plugin_interface.getLogger().getChannel( "MLab" ); 

		logger.setDiagnostic();
		
		logger.setForce( true );
		
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();

		loc_utils.integrateLocalisedMessageBundle( "com.vuze.plugins.mlab.internat.Messages" );

		UIManager	ui_manager	= plugin_interface.getUIManager();
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( "mlab.name" );

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
		
		BasicPluginConfigModel config_model = 
			ui_manager.createBasicPluginConfigModel( "mlab.name" );

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
		
		sp_button = config_model.addActionParameter2( "mlab.tool.shaperprobe", "mlab.run" );
		
		sp_button.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					runShaperProbe( null );
				}
			});
	}
	
	protected void
	runNDT(
		final toolListener	listener )
	{
		runTool(
			ndt_button,
			new Runnable()
			{
				public void
				run()
				{
					logger.log( "Starting NDT Test" );
					logger.log( "-----------------" );
					
					new Tcpbw100UIWrapper(
						new Tcpbw100UIWrapperListener()
						{
							public void
							reportSummary(
								String		str )
							{
								str = str.trim();
								
								logger.log( str.trim());
								
								if ( listener != null ){
									
									listener.reportSummary( str );
								}
							}
							
							public void
							reportDetail(
								String		str )
							{
								str = str.trim();
								
								if ( listener != null ){
									
									listener.reportDetail( str );
								}
							}
						});
					
					Tcpbw100 test = Tcpbw100.mainSupport( new String[]{ "ndt.iupui.donar.measurement-lab.org" });
					
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
					
					logger.log( 
							"Completed: up=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( up_bps ) +
							", down=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( down_bps ));
					
					if ( listener != null ){
						
						Map<String,Object>	results = new HashMap<String, Object>();
						
						results.put( "up", up_bps );
						results.put( "down", down_bps );
						
						listener.complete( results );
					}
				}
			});
	}
	
	protected void
	runShaperProbe(
		final toolListener	listener )
	{
		runTool(
			sp_button,
			new Runnable()
			{
				public void
				run()
				{
					logger.log( "Starting ShaperProbe Test" );
					logger.log( "-----------------" );

					ShaperProbe sp = 
						ShaperProbe.run(
							plugin_interface,
							new ShaperProbeListener()
							{
								public void 
								reportSummary(
									String str) 
								{
									logger.log( str.trim());
									
									if ( listener != null ){
									
										listener.reportSummary( str );
									}
								}
							});
					
					long up_bps 	= sp.getUpBitsPerSec()/8;
					long down_bps 	= sp.getDownBitsPerSec()/8;
					
					long shape_up_bps 		= sp.getShapeUpBitsPerSec()/8;
					long shape_down_bps 	= sp.getShapeDownBitsPerSec()/8;
					
					logger.log( "" );
					
					logger.log( 
							"Completed: up=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( up_bps ) +
							", down=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( down_bps ) +
							", shape_up=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( shape_up_bps ) +
							", shape_down=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( shape_down_bps ));

					if ( listener != null ){
						
						Map<String,Object>	results = new HashMap<String, Object>();
						
						results.put( "up", up_bps );
						results.put( "down", down_bps );
						results.put( "shape_up", shape_up_bps );
						results.put( "shape_down", shape_down_bps );
						
						listener.complete( results );
					}
				}
			});
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
	
	public void
	unload()
	{
	}
	
	public void
	runTest(
		Map<String,Object>		args,
		final IPCInterface		callback )
	{
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					new MLabWizard( MLabPlugin.this, callback );
				}
			});
		
		runNDT(
			new toolListener()
			{
				public void
				reportSummary(
					String		str )
				{
					
				}
				
				public void
				reportDetail(
					String		str )
				{
					
				}
				
				public void
				complete(
					Map<String,Object>	results )
				{
					try{
						callback.invoke( "complete", new Object[]{ results });
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			});
	}
	
	private interface
	toolListener
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
