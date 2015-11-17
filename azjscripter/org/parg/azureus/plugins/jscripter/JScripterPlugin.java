/*
 * Created on Nov 16, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.jscripter;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
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
import org.gudy.azureus2.plugins.utils.ScriptProvider;

public class 
JScripterPlugin 
	implements UnloadablePlugin
{
	private PluginInterface			plugin_interface;
	private LoggerChannel 			log;
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	private LocaleUtilities			loc_utils;
	
	private ScriptProvider			provider;
	
	private ScriptEngine engine;
	
	@Override
	public void 
	initialize(
		PluginInterface _plugin_interface )
		
		throws PluginException 
	{	
		plugin_interface = _plugin_interface;
		
		log	= plugin_interface.getLogger().getTimeStampedChannel( "JScripter");
		
		final UIManager	ui_manager = plugin_interface.getUIManager();
		
		loc_utils = plugin_interface.getUtilities().getLocaleUtilities();

		view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azjscripter.name" ));

		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
									
		config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azjscripter.name" );

		view_model.setConfigSectionID( "azjscripter.name" );

		final StringParameter script_param = config_model.addStringParameter2( "azjscripter.script", "azjscripter.script",  "print( pi.getAzureusVersion())");
		
		script_param.setMultiLine( 20 );
		
		ActionParameter	exec = config_model.addActionParameter2( "azjscripter.exec", "azjscripter.exec" );

		
		final UITextArea text_area = config_model.addTextArea( "azjscripter.log");
		
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
		
		try{
			ScriptEngineManager engineManager = new ScriptEngineManager();
			
			engine = engineManager.getEngineByName( "nashorn" );
			
			exec.addListener(
					new ParameterListener() 
					{
						public void 
						parameterChanged(
							Parameter param ) 
						{
							try{
								engine.eval( script_param.getValue());
								
							}catch( Throwable e ){
								
								text_area.appendText( e.getMessage());
								
								log.log( e );
							}
						}
					});

			final PipedReader reader = new PipedReader();
			
			Writer out = new PipedWriter( reader );
				
			new AEThread2("")
			{
				public void
				run()
				{
					try{
						while( true ){
						
							char[] chars = new char[1024];
							
							int num = reader.read( chars );
							
							if ( num <= 0 ){
								
								break;
							}
							String str = new String( chars, 0, num );
							
							log.log( str );
							
							text_area.appendText( str );
						}
					}catch( Throwable e ){
						
						log.log( e );
						
						Debug.out( e );
					}
				}
			}.start();
			
			engine.getContext().setWriter( out );
			
			Bindings bindings = engine.getBindings( ScriptContext.ENGINE_SCOPE );
			
			bindings.put( "pi", plugin_interface );
					
			//engine.eval( "function sum(a, b) { return a + b; }" );
			
			//System.out.println(engine.eval( "print( pi.getAzureusVersion())" ));
			
			provider = 
				new ScriptProvider()
				{
					public String
					getProviderName()
					{
						return( "nashorn" );
					}
				
					public String
					getScriptType()
					{
						return( ST_JAVASCRIPT );
					}
				
					public Object
					eval(
						String					script,
						Map<String,Object>		bindings )
						
						throws Exception
					{
						return( null );
					}
				};
					
			plugin_interface.getUtilities().registerScriptProvider( provider );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	@Override
	public void 
	unload() 
		throws PluginException 
	{
		if ( config_model != null ){
			
			config_model.destroy();
			
			config_model = null;
		}
		
		if ( view_model != null ){
			
			view_model.destroy();
			
			view_model = null;
		}	
		
		if ( plugin_interface != null && provider != null ){
			
			plugin_interface.getUtilities().unregisterScriptProvider( provider );
		}
	}
}
