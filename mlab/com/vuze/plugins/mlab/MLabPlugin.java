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

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import com.vuze.plugins.mlab.tools.ndt.Tcpbw100;
import com.vuze.plugins.mlab.tools.shaperprobe.ShaperProbe;

public class 
MLabPlugin
	implements UnloadablePlugin
{
	private PluginInterface		plugin_interface;
	private LoggerChannel		logger;

	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		logger				= plugin_interface.getLogger().getTimeStampedChannel( "MLab" ); 


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
		
		final ActionParameter ndt = config_model.addActionParameter2( "mlab.tool.ndt", "mlab.run" );
		
		ndt.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					runTool(
						ndt,
						new Runnable()
						{
							public void
							run()
							{
								Tcpbw100.main( new String[]{ "ndt.iupui.donar.measurement-lab.org" });
							}
						});
				}
			});
		
		final ActionParameter sp = config_model.addActionParameter2( "mlab.tool.shaperprobe", "mlab.run" );
		
		sp.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					runTool(
							sp,
							new Runnable()
							{
								public void
								run()
								{
									ShaperProbe.run( plugin_interface );
								}
							});
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
}
