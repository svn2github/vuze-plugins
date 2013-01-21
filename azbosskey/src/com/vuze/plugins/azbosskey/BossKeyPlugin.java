/*
 * Created on Jan 21, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.vuze.plugins.azbosskey;

import java.io.File;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;

public class 
BossKeyPlugin
	implements Plugin, HotkeyListener
{
	private PluginInterface	plugin_interface;
	
	private JIntellitype 	j_inst;
	
	private boolean	is_hidden		= false;
	private long	last_hide_event	= -1;
	
	private BooleanParameter 	bk_enable;
	private StringParameter		bk_key_spec;
	private LabelParameter		bk_key_status;
	
	boolean	key_registered;
	
	public void 
	initialize(
		PluginInterface _pi )
	
		throws PluginException 
	{
		plugin_interface = _pi;
		
		File dll = new File( plugin_interface.getPluginDirectoryName(), "JIntellitype" + (Constants.is64Bit?"64":"") + ".dll" );
		
			// File based constructor is borked if path is absolute!
		
		JIntellitype.setLibraryLocation( dll.getAbsolutePath());
	
		j_inst = JIntellitype.getInstance();	
	
		j_inst.addHotKeyListener( this );

		
		
		UIManager ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = 
			ui_manager.createBasicPluginConfigModel( "azbosskey.name" );

		
		bk_enable		= config_model.addBooleanParameter2( "azbosskey.enable", "azbosskey.enable", true );
		
		bk_key_spec		= config_model.addStringParameter2( "azbosskey.key_spec", "azbosskey.key_spec", "" );
		
		bk_key_status	= config_model.addLabelParameter2( "azbosskey.key_spec.status.ok" );
		
		LabelParameter label = config_model.addLabelParameter2( "azbosskey.key_spec.info" );
		
		bk_enable.addEnabledOnSelection( bk_key_spec );
		bk_enable.addEnabledOnSelection( label );
		
		ParameterListener listener = 
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param)
				{
					setupKey( false );
				};
			};
			
		bk_enable.addListener( listener );
		bk_key_spec.addListener( listener );
		bk_key_status.addListener( listener );
		
		setupKey( true );
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
				}
				
				public void
				closedownInitiated()
				{
					j_inst.cleanUp();
				}
				
				public void
				closedownComplete()
				{
				}
			});
		
	}
	
	private void
	setupKey(
		boolean	start )
	{
		if ( key_registered ){
			
			j_inst.unregisterHotKey( 1 );
		}
	
		if ( bk_enable.getValue()){
		
			String key_spec = bk_key_spec.getValue();
			
			String[] bits = key_spec.split( "\\+" );
			
			char	character	= 0;
			int		modifier	= 0;
			
			boolean	valid = false;
			
			if ( bits.length > 1 ){
				
				valid = true;
			
				for ( int i=0;i<bits.length;i++){
					
					String bit = bits[i].trim().toLowerCase();
					
					if ( i == bits.length - 1 ){
						
						if ( bit.length() == 1 ){
							
							character = Character.toUpperCase( bit.charAt( 0 ));
							
							valid = character >= 'A' && character <= 'Z';
							
						}else{
							
							valid = false;
						}
					}else{
						
						if ( bit.equals( "win" )){
					
							modifier |= JIntellitype.MOD_WIN;
							
						}else if ( bit.equals( "alt" )){
							
							modifier |= JIntellitype.MOD_ALT;
	
						}else if ( bit.equals( "ctrl" )){
							
							modifier |= JIntellitype.MOD_CONTROL;
	
						}else if ( bit.equals( "shift" )){
							
							modifier |= JIntellitype.MOD_SHIFT;
							
						}else{
							
							valid = false;
						}
					}
				}
			}
						
			if ( valid ){
				
				j_inst.registerHotKey( 1, modifier, (int)character );
			
				key_registered = true;
				
				bk_key_status.setLabelKey( "azbosskey.key_spec.status.ok" );
				
			}else{
				
				bk_key_status.setLabelKey( "azbosskey.key_spec.status.bad" );
				
				if ( start ){
					
					plugin_interface.getUIManager().addUIListener(
							new UIManagerListener()
							{
								public void
								UIAttached(
									UIInstance		instance )
								{
									plugin_interface.getUIManager().showMessageBox(
											"azbosskey.init.title",
											"azbosskey.init.details",
											UIManagerEvent.MT_OK );
								}
								
								public void
								UIDetached(
									UIInstance		instance )
								{
								}
							});
				}
			}
		}else{
			
			bk_key_status.setLabelText( "" );
		}
	}
	
	public void 
	onHotKey(
		int id ) 
	{
		if ( id == 1 ){
			
			long	now = SystemTime.getMonotonousTime();
			
			if ( last_hide_event < 0 || ( now - last_hide_event > 250 )){
				
				synchronized( this ){
					
					is_hidden = !is_hidden;
				}
				
				plugin_interface.getUIManager().setEverythingHidden( is_hidden );
			}
		}
	}
}
