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
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.gudy.azureus2.core3.util.AESemaphore;
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
	
	private UITextArea 				text_area;
	private StringParameter			script_param;
	
	
	private boolean					engine_load_attempted;
	private volatile ScriptEngine 	engine;

	private boolean					unloaded;
	
	private static AESemaphore		init_sem = new AESemaphore("jscripter:init");
	
	private static volatile JScripterPlugin		plugin_instance;
	
	private static ScriptProvider		provider;

	public static void 
	load(
		PluginInterface plugin_interface )
	{	
		try{
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
						JScripterPlugin inst = plugin_instance;
						
						if ( inst == null ){
							
							if ( !init_sem.reserve( 60*1000 )){
							
								throw( new Exception( "Plugin initialisation timeout" ));
							}
							
							inst = plugin_instance;
							
							if ( inst == null ){
								
								throw( new Exception( "Plugin initialisation failed" ));
							}
						}
						
						return( inst.evaluateScript( script, bindings ));
					}
				};
					
			plugin_interface.getUtilities().registerScriptProvider( provider );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	@Override
	public void 
	initialize(
		PluginInterface _plugin_interface )
		
		throws PluginException 
	{	
		try{
			plugin_instance = this;
			
			plugin_interface = _plugin_interface;
			
			log	= plugin_interface.getLogger().getTimeStampedChannel( "JScripter");
			
			final UIManager	ui_manager = plugin_interface.getUIManager();
			
			loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
	
			view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "azjscripter.name" ));
	
			view_model.getActivity().setVisible( false );
			view_model.getProgress().setVisible( false );
										
			config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azjscripter.name" );
	
			view_model.setConfigSectionID( "azjscripter.name" );
	
			script_param = config_model.addStringParameter2( "azjscripter.script", "azjscripter.script",  "print( pi.getAzureusVersion())");
			
			script_param.setMultiLine( 20 );
			
			ActionParameter	exec = config_model.addActionParameter2( "azjscripter.load", "azjscripter.exec" );
			
			text_area = config_model.addTextArea( "azjscripter.log");
	
			exec.addListener(
					new ParameterListener() 
					{
						public void 
						parameterChanged(
							Parameter param ) 
						{
							try{
								evaluateScript( script_param.getValue());
								
							}catch( Throwable e ){
								
								log( e );
							}
						}
					});
			
			ActionParameter	clear = config_model.addActionParameter2( "azjscripter.clear.log", "azjscripter.clear" );
			
			clear.addListener(
					new ParameterListener() 
					{
						public void 
						parameterChanged(
							Parameter param ) 
						{
							text_area.setText( "" );
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
							if ( !content.endsWith( "\n" )){
								content += "\n";
							}
							view_model.getLogArea().appendText( content );
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
		}finally{
			
			init_sem.releaseForever();
		}
	}
	
	private Object
	evaluateScript(
		String		script )
		
		throws Exception
	{
		Map<String,Object> bindings = new HashMap<String,Object>();
		
		bindings.put( "intent", "Load" );
		
		return( evaluateScript(script, bindings ));
	}
	
	private void
	log(
		String		str )
	{
		text_area.appendText( str );
		
		log.log( str );
	}
	
	private void
	log(
		Throwable	e )
	{
		text_area.appendText( e.getMessage());
		
		log.log( e );
	}
	
	private void
	log(
		String		str,
		Throwable	e )
	{
		text_area.appendText( str + ": " + e.getMessage());
		
		log.log( str, e );
	}
	
	private synchronized Object
	evaluateScript(
		String				script,
		Map<String,Object>	bindings_in )
		
		throws Exception
	{
		if ( unloaded ){
			
			throw( new Exception( "Plugin unloaded" ));
		}
		
		ScriptEngine	engine = getEngine();
		
		if ( engine == null ){
			
			throw( new Exception( "JavaScript engine unavailable" ));
		}
		
		String intent = (String)bindings_in.get( "intent" );
		
		Bindings bindings = engine.getBindings( ScriptContext.ENGINE_SCOPE );
		
		bindings.putAll( bindings_in );
		
		Object result = engine.eval(script);
		
		String str = (intent==null?"?":intent) + " -> " + result;
		
		log( str );
		
		return( result );
	}
	
	private synchronized ScriptEngine
	getEngine()
	{
		if ( engine != null || engine_load_attempted ){
			
			return( engine );
		}
		
		engine_load_attempted = true;
		
		ScriptEngineManager engineManager = new ScriptEngineManager();
		
		engine = engineManager.getEngineByName( "nashorn" );
		
		if ( engine != null ){

			try{
				final PipedReader reader = new PipedReader();
									
				new AEThread2("")
				{
					final ScriptEngine my_engine = engine;
					
					PipedReader current_reader = reader;
					
					public void
					run()
					{
						while( my_engine == engine ){
					
							try{
								char[] chars = new char[1024];
								
								int num = current_reader.read( chars );
								
								if ( num <= 0 ){
									
									break;
								}
								
								String str = new String( chars, 0, num );
								
								str = str.replaceAll( "\r", "" );
								
								if ( str.endsWith( "\n" )){
									
									str = str.substring( 0, str.length()-1 );
								}
							
								log( str );
							
							}catch( Throwable e ){
							
									// for some reason getting spurious pipe fails
								
								current_reader = new PipedReader();
								
								try{
									Writer out = new PipedWriter( current_reader );

									my_engine.getContext().setWriter( out );
									
									Thread.sleep( 500 );
									
								}catch( Throwable f ){
									
									log( f );
									
									break;
								}
							}
						}
					}
				}.start();
				
				Writer out = new PipedWriter( reader );

				engine.getContext().setWriter( out );
				
				Bindings bindings = engine.getBindings( ScriptContext.ENGINE_SCOPE );
				
				bindings.put( "pi", plugin_interface );
				bindings.put( "engine", engine );
				
				engine.eval( "function loadScript( abs_path ){ engine.eval( new java.io.FileReader( abs_path )); }" );
				engine.eval( "function loadPluginScript( rel_path ){ engine.eval( new java.io.FileReader( new java.io.File( pi.getPluginDirectoryName(), rel_path ))); }" );
				
				try{
					engine.eval( "loadPluginScript( 'init.js' )" );
					
				}catch( Throwable e ){
					
					log( "Failed to load 'init.js'", e );
				}
				
				String initial_script = script_param.getValue().trim();
				
				if ( initial_script != null ){
					
					engine.eval( initial_script );
				}
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( engine );
	}
	
	@Override
	public void 
	unload() 
		throws PluginException 
	{
		unloaded	= true;
		
		if ( plugin_interface != null && provider != null ){
			
			plugin_interface.getUtilities().unregisterScriptProvider( provider );
		}
				
		if ( config_model != null ){
			
			config_model.destroy();
			
			config_model = null;
		}
		
		if ( view_model != null ){
			
			view_model.destroy();
			
			view_model = null;
		}	
		
		ScriptEngine current_engine = engine;
		
		if ( current_engine != null ){
						
			engine = null;
			
			try{
				current_engine.getContext().getWriter().close();
				
			}catch( Throwable e ){
			}
		}
		
		plugin_instance 	= null;
		init_sem 			= new AESemaphore( "jscripter:reinit" );
	}
}
