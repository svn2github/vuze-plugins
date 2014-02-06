/*
 * Created on May 10, 2010
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


package com.aelitis.plugins.rcmplugin;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.content.ContentException;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.util.MapUtils;


public class 
RCMPlugin 
	implements UnloadablePlugin
{
	// following code to make plugin backwards compatible from 5101 to 5100
	
	private static final boolean IS_5101_PLUS = Constants.isCurrentVersionGE( "5.1.0.1" );

	protected static final int MIN_SEARCH_RANK_DEFAULT = 0;

	public static  final String PARAM_SOURCES_LIST = "Plugin.aercm.sources.setlist";

	public static final String PARAM_FTUX_SHOWN = "rcm.ftux.shown2";

	static{
		COConfigurationManager.setParameter( "rcm.persist", true );
		
		new RCMPatcher();
	}
	
	private PluginInterface			plugin_interface;
	
	private RelatedContentUI		ui;
	private SearchProvider 			search_provider;

	private boolean					destroyed;
	
	List<String>	source_map_defaults = new ArrayList<String>();
	{
		source_map_defaults.add( "vhdn.vuze.com" );
		source_map_defaults.add( "tracker.vodo.net" );
		source_map_defaults.add( "bt.archive.org" );
		source_map_defaults.add( "tracker.legaltorrents.com" );
		source_map_defaults.add( "tracker.mininova.org" );
	}
	
	private ByteArrayHashMap<Boolean>	source_map 	= new ByteArrayHashMap<Boolean>();
	private boolean						source_map_wildcard;
	private byte[]						source_vhdn = compressDomain( "vhdn.vuze.com" );
	
	private Object json_rpc_server;	// Object during migration
	
	private byte[]
	compressDomain(
		String	host )
	{
		String[] bits = host.split( "\\." );
		
		int	len = bits.length;
		
		if ( len < 2 ){
			
			bits = new String[]{ bits[0], "com" };
		}
					
		String	end = bits[len-1];
							
		String dom = bits[len-2] + "." + end;
				
		int hash = dom.hashCode();
				
		byte[]	bytes = { (byte)(hash>>24), (byte)(hash>>16),(byte)(hash>>8),(byte)hash };

		return( bytes );
	}

	
	public void
	initialize(
		final PluginInterface		_plugin_interface )
	
		throws PluginException
	{
		plugin_interface = _plugin_interface;
			
		COConfigurationManager.addAndFireParameterListener(
				PARAM_SOURCES_LIST,
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name) 
				{
					updateSourcesList();
				}
			});
		
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();

		loc_utils.integrateLocalisedMessageBundle( "com.aelitis.plugins.rcmplugin.internat.Messages" );

		hookSearch();
		
		updatePluginInfo();

		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void 
				UIAttached(
					UIInstance instance) 
				{
					if ( instance instanceof UISWTInstance ){
						
						String path = "com/aelitis/plugins/rcmplugin/skins/";

						String sFile = path + "skin3_rcm";

						ClassLoader loader = RCMPlugin.class.getClassLoader();

						SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();

						try {
							ResourceBundle subBundle = ResourceBundle.getBundle(sFile,
									Locale.getDefault(), loader);
							skinProperties.addResourceBundle(subBundle, path, loader);
						} catch (MissingResourceException mre) {
							Debug.out(mre);
						}	

						synchronized( RCMPlugin.this ){

							if ( destroyed ){

								return;
							}

							ui = RelatedContentUI.getSingleton( plugin_interface, (UISWTInstance)instance, RCMPlugin.this );
						}
					}
				}
				
				public void
				UIDetached(
					UIInstance		instance )
				{
					
				}
			});
		
		if ( IS_5101_PLUS ){

			json_rpc_server = 
				new Utilities.JSONServer() 
				{
					private List<String> methods = new ArrayList<String>();
					
					{
						methods.add( "rcm-is-enabled" );
						methods.add( "rcm-get-list" );
					}
					
					public String 
					getName() 
					{
						return( "SwarmDiscoveries" );
					}

					public List<String> 
					getSupportedMethods() 
					{
						return( methods );
					}				
					
					public Map 
					call(
						String 	method, 
						Map 	args )
								
						throws PluginException 
					{
						if ( destroyed ){
							
							throw( new PluginException( "Plugin unloaded" ));
						}
						
						Map result = new HashMap();
						
						if ( method.equals( "rcm-is-enabled" )){
							
							result.put( "enabled", isRCMEnabled());
							
						} else if ( method.equals( "rcm-get-list" )){
							rpcGetList(result, args);

						}else{
							
							throw( new PluginException( "Unsupported method" ));
						}
						
						return( result );
					}
				};
				
			plugin_interface.getUtilities().registerJSONRPCServer((Utilities.JSONServer)json_rpc_server);
		}
	}
	
	protected void rpcGetList(Map result, Map args) {
		long since = args == null ? 0 : MapUtils.getMapLong(args, "since", 0);
		long until = 0;

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		result.put("related", list);
		try {
			RelatedContentManager manager = RelatedContentManager.getSingleton();
			RelatedContent[] relatedContent = manager.getRelatedContent();
			
			for (RelatedContent item : relatedContent) {
				HashMap<String, Object> map = new HashMap<String, Object>();
				
				long changedLocallyOn = item.getChangedLocallyOn();
				if (changedLocallyOn < since) {
					continue;
				}
				if (changedLocallyOn > until) {
					until = changedLocallyOn;
				}

				map.put("changedOn", changedLocallyOn);
				map.put("contentNetwork", item.getContentNetwork());
				map.put("hash", ByteFormatter.encodeString(item.getHash()));
				map.put("lastSeenSecs", item.getLastSeenSecs());
				map.put("peers", item.getLeechers());
				map.put("level", item.getLevel());
				map.put("publishDate", item.getPublishDate());
				map.put("rank", item.getRank());
				map.put("relatedToHash", ByteFormatter.encodeString(item.getRelatedToHash()) );
				map.put("seeds", item.getSeeds());
				map.put("size", item.getSize());
				map.put("tags", item.getTags());
				map.put("title", item.getTitle());
				map.put("tracker", item.getTracker());
				map.put("unread", item.isUnread());
				list.add(map);
			}
		} catch (ContentException e) {
		}
		
		result.put("until", until);
	}


	protected void
	updatePluginInfo()
	{
		String plugin_info;
		
		if ( !hasFTUXBeenShown()){
			
			plugin_info = "f";
			
		}else if ( isRCMEnabled()){
		
			plugin_info = "e";
			
		}else{
			
			plugin_info = "d";
		}
		
		PluginConfig pc = plugin_interface.getPluginconfig();
		
		if ( !pc.getPluginStringParameter( "plugin.info", "" ).equals( plugin_info )){
			
			pc.setPluginParameter( "plugin.info", plugin_info );
		
			COConfigurationManager.save();
		}
	}
	
	protected boolean
	isRCMEnabled()
	{
		return( COConfigurationManager.getBooleanParameter( "rcm.overall.enabled", true ));
	}
	
	protected boolean
	setRCMEnabled(
		boolean	enabled )
	{
		if ( isRCMEnabled() != enabled ){
			
			COConfigurationManager.setParameter( "rcm.overall.enabled", enabled );
						
			hookSearch();
			
			updatePluginInfo();

			return true;
		}
		
		return false;
	}
	
	protected boolean
	hasFTUXBeenShown()
	{
		return( plugin_interface.getPluginconfig().getPluginBooleanParameter( PARAM_FTUX_SHOWN, false ));
	}

	protected void
	setFTUXBeenShown(
		boolean b )
	{
		plugin_interface.getPluginconfig().setPluginParameter( PARAM_FTUX_SHOWN, b );
				
		hookSearch();
		
		updatePluginInfo();
	}
	
	protected boolean
	isSearchEnabled()
	{
		return( plugin_interface.getPluginconfig().getPluginBooleanParameter( "rcm.search.enable", false ));
	}
	
	protected int
	getMinuumSearchRank()
	{
		return( plugin_interface.getPluginconfig().getPluginIntParameter( "rcm.search.min_rank", MIN_SEARCH_RANK_DEFAULT ));
	}
	
	protected void
	hookSearch()
	{		
		boolean enable = isRCMEnabled() && isSearchEnabled() && hasFTUXBeenShown();
		
		try{
				
			if ( enable ){
			
				if ( search_provider == null ){
					
					search_provider = new RCM_SearchProvider( this );
						
					plugin_interface.getUtilities().registerSearchProvider( search_provider );
				}
			}else{
				
				if ( search_provider != null ) {
					
					plugin_interface.getUtilities().unregisterSearchProvider( search_provider );
					
					search_provider = null;
				}
			}
		}catch( Throwable e ){
			
			Debug.out( "Failed to register/unregister search provider", e );
		}
	}
	
	private void
	updateSourcesList()
	{
		List<String>	list = getSourcesList();
		
		source_map.clear();
		source_map_wildcard	= false;
		
		for( String host: list ){
			
			if ( host.equals( "*" )){
				
				source_map_wildcard = true;
				
			}else{
				
				source_map.put( compressDomain( host ), Boolean.TRUE );
			}
		}
	}
	
	public List<String>
	getSourcesList()
	{
		List original_list = COConfigurationManager.getListParameter( PARAM_SOURCES_LIST, source_map_defaults );
		
		List<String>	list = BDecoder.decodeStrings( BEncoder.cloneList(original_list) );

		return( list );
	}
	
	public void setToDefaultSourcesList() {
		COConfigurationManager.setParameter(PARAM_SOURCES_LIST, source_map_defaults);
	}
	
	public void setToAllSources() {
		COConfigurationManager.setParameter(PARAM_SOURCES_LIST, Arrays.asList("*"));
	}
	
	public boolean isAllSources() {
		return source_map_wildcard;
	}
	
	public boolean
	isVisible(
		long	cnet )
	{
		if ( cnet == ContentNetwork.CONTENT_NETWORK_VHDNL ){
			
			return( isVisible( source_vhdn ));
		}
		
		return( false );
	}
	
	public boolean
	isVisible(
		byte[]	key_list )
	{
		if ( source_map_wildcard ){
			
			return( true );
		}
		
		if ( key_list != null ){
			
			for ( int i=0;i<key_list.length;i+=4 ){
				
				Boolean b = source_map.get( key_list, i, 4 );
				
				if ( b != null ){
					
					if ( b ){
						
						return( true );
					}
				}
			}
		}
		
		return( false );
	}
	
	public boolean
	isVisible(
		RelatedContent	related_content )
	{
		if ( source_map_wildcard ){
			
			return( true );
		}
		
		byte[] tracker_keys;
		
		long cnet = related_content.getContentNetwork();
		
		if ( cnet == ContentNetwork.CONTENT_NETWORK_VHDNL ){
			
			tracker_keys = source_vhdn;
			
		}else{
		
			tracker_keys = related_content.getTrackerKeys();
		}
		
		if ( isVisible( tracker_keys )){
			
			return( true );
		}
		
		byte[] ws_keys = related_content.getWebSeedKeys();
		
		return( isVisible( ws_keys ));
	}
	
	public void 
	unload() 
	
		throws PluginException 
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed = true;
		}
		
		if ( ui != null ){
			
			ui.destroy();
			
			ui = null;
		}
		
		if ( search_provider != null ){

			try{
				plugin_interface.getUtilities().unregisterSearchProvider( search_provider );

				search_provider = null;
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		if ( json_rpc_server != null ){
			
			plugin_interface.getUtilities().unregisterJSONRPCServer((Utilities.JSONServer)json_rpc_server);
			
			json_rpc_server = null;
		}
	}
	
		// IPC methods
	
	public void
	lookupByDownload(
		final Download	download )
	
		throws IPCException
	{
		RelatedContentUI current_ui = ui;
		
		if ( current_ui == null ){
			
			throw( new IPCException( "UI not bound" ));
		}
		
		if ( !hasFTUXBeenShown() || !isRCMEnabled()){
			
			RelatedContentUI.showFTUX( 
				null,
				new UserPrompterResultListener()
				{
					public void 
					prompterClosed(
						int result) 
					{
						if ( isRCMEnabled()){
							
							RelatedContentUI current_ui = ui;
							
							if ( current_ui != null ){
							
								current_ui.setUIEnabled( true );
								
								current_ui.addSearch( download );
							}
						}
					}
				});
		}else{
			
			current_ui.setUIEnabled( true );
			
			current_ui.addSearch( download );
		}
	}
	
	public void
	lookupBySize(
		final long	size )
	
		throws IPCException
	{
		RelatedContentUI current_ui = ui;
		
		if ( current_ui == null ){
			
			throw( new IPCException( "UI not bound" ));
		}
		
		if ( !hasFTUXBeenShown() || !isRCMEnabled()){
			
			RelatedContentUI.showFTUX( 
				null,
				new UserPrompterResultListener()
				{
					public void 
					prompterClosed(
						int result) 
					{
						if ( isRCMEnabled()){
							
							RelatedContentUI current_ui = ui;
							
							if ( current_ui != null ){
							
								current_ui.setUIEnabled( true );
								
								current_ui.addSearch( size );
							}
						}
					}
				});
		}else{
			
			current_ui.setUIEnabled( true );
			
			current_ui.addSearch( size );
		}
	}
	
	public void
	lookupByHash(
		final byte[]	hash,
		final String	name )
	
		throws IPCException
	{
		RelatedContentUI current_ui = ui;
		
		if ( current_ui == null ){
			
			throw( new IPCException( "UI not bound" ));
		}
		
		if ( !hasFTUXBeenShown() || !isRCMEnabled()){
			
			RelatedContentUI.showFTUX( 
				null,
				new UserPrompterResultListener()
				{
					public void 
					prompterClosed(
						int result) 
					{
						if ( isRCMEnabled()){
							
							RelatedContentUI current_ui = ui;
							
							if ( current_ui != null ){
							
								current_ui.setUIEnabled( true );
								
								current_ui.addSearch( hash, name );
							}
						}
					}
				});
		}else{
			
			current_ui.setUIEnabled( true );
			
			current_ui.addSearch( hash, name );
		}
	}
}
