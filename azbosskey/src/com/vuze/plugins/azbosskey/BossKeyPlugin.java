/*
 * Created on Jan 21, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */



package com.vuze.plugins.azbosskey;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;

import org.gudy.azureus2.core3.internat.MessageText;
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

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;

public class 
BossKeyPlugin
	implements Plugin, HotkeyListener
{
    private static HashMap<String, Integer> key_map = new HashMap<String, Integer>();
    
    	// these copied into win_keys.txt for user info
    
    static{
	    key_map.put("first", KeyEvent.KEY_FIRST);
	    key_map.put("last", KeyEvent.KEY_LAST);
	    key_map.put("typed", KeyEvent.KEY_TYPED);
	    key_map.put("pressed", KeyEvent.KEY_PRESSED);
	    key_map.put("released", KeyEvent.KEY_RELEASED);
	    key_map.put("enter", 13);
	    key_map.put("back_space", KeyEvent.VK_BACK_SPACE);
	    key_map.put("tab", KeyEvent.VK_TAB);
	    key_map.put("cancel", KeyEvent.VK_CANCEL);
	    key_map.put("clear", KeyEvent.VK_CLEAR);
	    key_map.put("pause", KeyEvent.VK_PAUSE);
	    key_map.put("caps_lock", KeyEvent.VK_CAPS_LOCK);
	    key_map.put("escape", KeyEvent.VK_ESCAPE);
	    key_map.put("space", KeyEvent.VK_SPACE);
	    key_map.put("page_up", KeyEvent.VK_PAGE_UP);
	    key_map.put("page_down", KeyEvent.VK_PAGE_DOWN);
	    key_map.put("end", KeyEvent.VK_END);
	    key_map.put("home", KeyEvent.VK_HOME);
	    key_map.put("left", KeyEvent.VK_LEFT);
	    key_map.put("up", KeyEvent.VK_UP);
	    key_map.put("right", KeyEvent.VK_RIGHT);
	    key_map.put("down", KeyEvent.VK_DOWN);
	    key_map.put("comma", 188); 				key_map.put(",", 188);
		key_map.put("minus", 109); 	 			key_map.put("-", 109);
	    key_map.put("period", 110); 			key_map.put(".", 110);
	    key_map.put("slash", 191); 				key_map.put("/", 191);
	    key_map.put("accent `", 192);			key_map.put("`", 192);
	    key_map.put("0", KeyEvent.VK_0);
	    key_map.put("1", KeyEvent.VK_1);
	    key_map.put("2", KeyEvent.VK_2);
	    key_map.put("3", KeyEvent.VK_3);
	    key_map.put("4", KeyEvent.VK_4);
	    key_map.put("5", KeyEvent.VK_5);
	    key_map.put("6", KeyEvent.VK_6);
	    key_map.put("7", KeyEvent.VK_7);
	    key_map.put("8", KeyEvent.VK_8);
	    key_map.put("9", KeyEvent.VK_9);
	    key_map.put("semicolon", 186);			key_map.put(";", 186);
	    key_map.put("equals", 187);				key_map.put("=", 187);
	    key_map.put("a", KeyEvent.VK_A);
	    key_map.put("b", KeyEvent.VK_B);
	    key_map.put("c", KeyEvent.VK_C);
	    key_map.put("d", KeyEvent.VK_D);
	    key_map.put("e", KeyEvent.VK_E);
	    key_map.put("f", KeyEvent.VK_F);
	    key_map.put("g", KeyEvent.VK_G);
	    key_map.put("h", KeyEvent.VK_H);
	    key_map.put("i", KeyEvent.VK_I);
	    key_map.put("j", KeyEvent.VK_J);
	    key_map.put("k", KeyEvent.VK_K);
	    key_map.put("l", KeyEvent.VK_L);
	    key_map.put("m", KeyEvent.VK_M);
	    key_map.put("n", KeyEvent.VK_N);
	    key_map.put("o", KeyEvent.VK_O);
	    key_map.put("p", KeyEvent.VK_P);
	    key_map.put("q", KeyEvent.VK_Q);
	    key_map.put("r", KeyEvent.VK_R);
	    key_map.put("s", KeyEvent.VK_S);
	    key_map.put("t", KeyEvent.VK_T);
	    key_map.put("u", KeyEvent.VK_U);
	    key_map.put("v", KeyEvent.VK_V);
	    key_map.put("w", KeyEvent.VK_W);
	    key_map.put("x", KeyEvent.VK_X);
	    key_map.put("y", KeyEvent.VK_Y);
	    key_map.put("z", KeyEvent.VK_Z);
	    key_map.put("open_bracket", 219);				key_map.put("(", 219);
	    key_map.put("back_slash", 220);				 	key_map.put("\\", 220);
	    key_map.put("close_bracket", 221);				key_map.put(")", 221);
	    key_map.put("numpad0", KeyEvent.VK_NUMPAD0);
	    key_map.put("numpad1", KeyEvent.VK_NUMPAD1);
	    key_map.put("numpad2", KeyEvent.VK_NUMPAD2);
	    key_map.put("numpad3", KeyEvent.VK_NUMPAD3);
	    key_map.put("numpad4", KeyEvent.VK_NUMPAD4);
	    key_map.put("numpad5", KeyEvent.VK_NUMPAD5);
	    key_map.put("numpad6", KeyEvent.VK_NUMPAD6);
	    key_map.put("numpad7", KeyEvent.VK_NUMPAD7);
	    key_map.put("numpad8", KeyEvent.VK_NUMPAD8);
	    key_map.put("numpad9", KeyEvent.VK_NUMPAD9);
	    key_map.put("multiply", KeyEvent.VK_MULTIPLY);		key_map.put("*", KeyEvent.VK_MULTIPLY);
	    key_map.put("add", KeyEvent.VK_ADD);				key_map.put("+", KeyEvent.VK_ADD);
	    key_map.put("separator", KeyEvent.VK_SEPARATOR);
	    key_map.put("subtract", KeyEvent.VK_SUBTRACT);
	    key_map.put("decimal", KeyEvent.VK_DECIMAL);
	    key_map.put("divide", KeyEvent.VK_DIVIDE);
	    key_map.put("delete", 46);
	    key_map.put("num_lock", KeyEvent.VK_NUM_LOCK);
	    key_map.put("scroll_lock", KeyEvent.VK_SCROLL_LOCK);
	    key_map.put("f1", KeyEvent.VK_F1);
	    key_map.put("f2", KeyEvent.VK_F2);
	    key_map.put("f3", KeyEvent.VK_F3);
	    key_map.put("f4", KeyEvent.VK_F4);
	    key_map.put("f5", KeyEvent.VK_F5);
	    key_map.put("f6", KeyEvent.VK_F6);
	    key_map.put("f7", KeyEvent.VK_F7);
	    key_map.put("f8", KeyEvent.VK_F8);
	    key_map.put("f9", KeyEvent.VK_F9);
	    key_map.put("f10", KeyEvent.VK_F10);
	    key_map.put("f11", KeyEvent.VK_F11);
	    key_map.put("f12", KeyEvent.VK_F12);
	    key_map.put("f13", KeyEvent.VK_F13);
	    key_map.put("f14", KeyEvent.VK_F14);
	    key_map.put("f15", KeyEvent.VK_F15);
	    key_map.put("f16", KeyEvent.VK_F16);
	    key_map.put("f17", KeyEvent.VK_F17);
	    key_map.put("f18", KeyEvent.VK_F18);
	    key_map.put("f19", KeyEvent.VK_F19);
	    key_map.put("f20", KeyEvent.VK_F20);
	    key_map.put("f21", KeyEvent.VK_F21);
	    key_map.put("f22", KeyEvent.VK_F22);
	    key_map.put("f23", KeyEvent.VK_F23);
	    key_map.put("f24", KeyEvent.VK_F24);
	    key_map.put("printscreen", 44);
	    key_map.put("insert", 45);
	    key_map.put("help", 47);
	    key_map.put("meta", KeyEvent.VK_META);
	    key_map.put("back_quote", KeyEvent.VK_BACK_QUOTE);
	    key_map.put("quote", KeyEvent.VK_QUOTE);					key_map.put("'", KeyEvent.VK_QUOTE);
	    key_map.put("kp_up", KeyEvent.VK_KP_UP);
	    key_map.put("kp_down", KeyEvent.VK_KP_DOWN);
	    key_map.put("kp_left", KeyEvent.VK_KP_LEFT);
	    key_map.put("kp_right", KeyEvent.VK_KP_RIGHT);
	    key_map.put("dead_grave", KeyEvent.VK_DEAD_GRAVE);
	    key_map.put("dead_acute", KeyEvent.VK_DEAD_ACUTE);
	    key_map.put("dead_circumflex", KeyEvent.VK_DEAD_CIRCUMFLEX);
	    key_map.put("dead_tilde", KeyEvent.VK_DEAD_TILDE);
	    key_map.put("dead_macron", KeyEvent.VK_DEAD_MACRON);
	    key_map.put("dead_breve", KeyEvent.VK_DEAD_BREVE);
	    key_map.put("dead_abovedot", KeyEvent.VK_DEAD_ABOVEDOT);
	    key_map.put("dead_diaeresis", KeyEvent.VK_DEAD_DIAERESIS);
	    key_map.put("dead_abovering", KeyEvent.VK_DEAD_ABOVERING);
	    key_map.put("dead_doubleacute", KeyEvent.VK_DEAD_DOUBLEACUTE);
	    key_map.put("dead_caron", KeyEvent.VK_DEAD_CARON);
	    key_map.put("dead_cedilla", KeyEvent.VK_DEAD_CEDILLA);
	    key_map.put("dead_ogonek", KeyEvent.VK_DEAD_OGONEK);
	    key_map.put("dead_iota", KeyEvent.VK_DEAD_IOTA);
	    key_map.put("dead_voiced_sound", KeyEvent.VK_DEAD_VOICED_SOUND);
	    key_map.put("dead_semivoiced_sound", KeyEvent.VK_DEAD_SEMIVOICED_SOUND);
	    key_map.put("ampersand", KeyEvent.VK_AMPERSAND);				key_map.put("&", KeyEvent.VK_AMPERSAND);
	    key_map.put("asterisk", KeyEvent.VK_ASTERISK);
	    key_map.put("quotedbl", KeyEvent.VK_QUOTEDBL);					key_map.put("\"", KeyEvent.VK_QUOTEDBL);
	    key_map.put("less", KeyEvent.VK_LESS);							key_map.put("<", KeyEvent.VK_LESS);
	    key_map.put("greater", KeyEvent.VK_GREATER);					key_map.put(">", KeyEvent.VK_GREATER);
	    key_map.put("braceleft", KeyEvent.VK_BRACELEFT);				key_map.put("{", KeyEvent.VK_BRACELEFT);
	    key_map.put("braceright", KeyEvent.VK_BRACERIGHT);				key_map.put("}", KeyEvent.VK_BRACERIGHT);
	    key_map.put("at", KeyEvent.VK_AT);								key_map.put("@", KeyEvent.VK_AT);
	    key_map.put("colon", KeyEvent.VK_COLON);						key_map.put(":", KeyEvent.VK_COLON);
	    key_map.put("circumflex", KeyEvent.VK_CIRCUMFLEX);				key_map.put("^", KeyEvent.VK_CIRCUMFLEX);
	    key_map.put("dollar", KeyEvent.VK_DOLLAR);						key_map.put("$", KeyEvent.VK_DOLLAR);
	    key_map.put("euro_sign", KeyEvent.VK_EURO_SIGN);				key_map.put("€", KeyEvent.VK_EURO_SIGN);
	    key_map.put("exclamation_mark", KeyEvent.VK_EXCLAMATION_MARK);	key_map.put("!", KeyEvent.VK_EURO_SIGN);
	    key_map.put("inverted_exclamation_mark", KeyEvent.VK_INVERTED_EXCLAMATION_MARK);
	    key_map.put("left_parenthesis", KeyEvent.VK_LEFT_PARENTHESIS);
	    key_map.put("number_sign", KeyEvent.VK_NUMBER_SIGN);
	    key_map.put("plus", KeyEvent.VK_PLUS);
	    key_map.put("right_parenthesis", KeyEvent.VK_RIGHT_PARENTHESIS);
	    key_map.put("underscore", KeyEvent.VK_UNDERSCORE);				key_map.put("_", KeyEvent.VK_UNDERSCORE);
	    key_map.put("context_menu", KeyEvent.VK_CONTEXT_MENU);
	    key_map.put("final", KeyEvent.VK_FINAL);
	    key_map.put("convert", KeyEvent.VK_CONVERT);
	    key_map.put("nonconvert", KeyEvent.VK_NONCONVERT);
	    key_map.put("accept", KeyEvent.VK_ACCEPT);
	    key_map.put("modechange", KeyEvent.VK_MODECHANGE);
	    key_map.put("kana", KeyEvent.VK_KANA);
	    key_map.put("kanji", KeyEvent.VK_KANJI);
	    key_map.put("alphanumeric", KeyEvent.VK_ALPHANUMERIC);
	    key_map.put("katakana", KeyEvent.VK_KATAKANA);
	    key_map.put("hiragana", KeyEvent.VK_HIRAGANA);
	    key_map.put("full_width", KeyEvent.VK_FULL_WIDTH);
	    key_map.put("half_width", KeyEvent.VK_HALF_WIDTH);
	    key_map.put("roman_characters", KeyEvent.VK_ROMAN_CHARACTERS);
	    key_map.put("all_candidates", KeyEvent.VK_ALL_CANDIDATES);
	    key_map.put("previous_candidate", KeyEvent.VK_PREVIOUS_CANDIDATE);
	    key_map.put("code_input", KeyEvent.VK_CODE_INPUT);
	    key_map.put("japanese_katakana", KeyEvent.VK_JAPANESE_KATAKANA);
	    key_map.put("japanese_hiragana", KeyEvent.VK_JAPANESE_HIRAGANA);
	    key_map.put("japanese_roman", KeyEvent.VK_JAPANESE_ROMAN);
	    key_map.put("kana_lock", KeyEvent.VK_KANA_LOCK);
	    key_map.put("input_method_on_off", KeyEvent.VK_INPUT_METHOD_ON_OFF);
	    key_map.put("cut", KeyEvent.VK_CUT);
	    key_map.put("copy", KeyEvent.VK_COPY);
	    key_map.put("paste", KeyEvent.VK_PASTE);
	    key_map.put("undo", KeyEvent.VK_UNDO);
	    key_map.put("again", KeyEvent.VK_AGAIN);
	    key_map.put("find", KeyEvent.VK_FIND);
	    key_map.put("props", KeyEvent.VK_PROPS);
	    key_map.put("stop", KeyEvent.VK_STOP);
	    key_map.put("compose", KeyEvent.VK_COMPOSE);
	    key_map.put("alt_graph", KeyEvent.VK_ALT_GRAPH);
	    key_map.put("begin", KeyEvent.VK_BEGIN);
    }
	
	
	
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
		
		config_model.addHyperlinkParameter2( "azbosskey.info", MessageText.getString( "azbosskey.info.url" ));
		
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
			
			int		character	= 0;
			int		modifier	= 0;
			
			boolean	valid = false;
			
			if ( bits.length > 1 ){
				
				valid = true;
			
				for ( int i=0;i<bits.length;i++){
					
					String bit = bits[i].trim().toLowerCase();
					
					if ( i == bits.length - 1 ){
						
						Integer code = key_map.get( bit.toLowerCase());
						
						if ( code != null ){
							
							character = code;
							
						}else  if ( bit.length() == 1 ){
							
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
				
				j_inst.registerHotKey( 1, modifier, character );
			
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
