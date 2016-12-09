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
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.search.*;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.content.*;
import com.aelitis.azureus.ui.UserPrompterResultListener;


public class 
RCMPlugin 
	implements UnloadablePlugin
{
	protected static final int MIN_SEARCH_RANK_DEFAULT = 0;

	public static  final String PARAM_SOURCES_LIST = "Plugin.aercm.sources.setlist";

	public static  final String PARAM_SOURCES_ISDEFAULT = "Plugin.aercm.sources.isdefault";

	public static final String PARAM_FTUX_SHOWN = "rcm.ftux.shown2";

	public static final String POPULARITY_SEARCH_EXPR	= "(.)";

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
		// End of original list
		source_map_defaults.add( "www.legaltorrents.com" );
		source_map_defaults.add( "torrent.ubuntu.com" );
		source_map_defaults.add( "torrents.freebsd.org" );
		source_map_defaults.add( "torrent.fedoraproject.org" );
		source_map_defaults.add( "tracker.opensuse.org" );
		source_map_defaults.add( "torrents.linuxmint.com" );
		source_map_defaults.add( "tracker.johncave.co.nz" ); // OpenMandriva
		source_map_defaults.add( "academictorrents.com" );
	}
	
	private ByteArrayHashMap<Boolean>	source_map 	= new ByteArrayHashMap<Boolean>();
	private boolean						source_map_wildcard;
	private byte[]						source_vhdn = compressDomain( "vhdn.vuze.com" );
	
	private RCM_JSONServer json_rpc_server;
	
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
			

		// ensure source list is up to date if marked as default
		boolean isDefaultList = COConfigurationManager.getBooleanParameter(PARAM_SOURCES_ISDEFAULT, false);
		if (isDefaultList) {
			setToDefaultSourcesList();
		} else {
  		// Upgrade source list if it's a subset of default list (ie. older version)
  		// Should be done before anything hooks PARAM_SOURCES_LIST, since
  		// it may fire a config change
  		List<String> list = getSourcesList();
  		if (source_map_defaults.containsAll(list)) {
  			setToDefaultSourcesList();
  		}
		}
		
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
					if ( instance.getUIType() ==  UIInstance.UIT_SWT ){
						
						synchronized( RCMPlugin.this ){

							if ( destroyed ){

								return;
							}

							try{
								Class cla = RCMPlugin.class.forName( "com.aelitis.plugins.rcmplugin.RelatedContentUISWT" );
							
								ui = (RelatedContentUI)cla.getMethod( "getSingleton", PluginInterface.class, UIInstance.class, RCMPlugin.class ).invoke(
										null, plugin_interface, instance, RCMPlugin.this );
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}
				}
				
				public void
				UIDetached(
					UIInstance		instance )
				{
					
				}
			});
		
		json_rpc_server = new RCM_JSONServer(this);
			
		plugin_interface.getUtilities().registerJSONRPCServer(json_rpc_server);
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
	isUIEnabled() {
		return( plugin_interface.getPluginconfig().getPluginBooleanParameter( "rcm.ui.enable", false ));
	}

	protected void
	setUIEnabled(
		boolean b )
	{
		plugin_interface.getPluginconfig().setPluginParameter( "rcm.ui.enable", b );
	}

	protected boolean
	isSearchEnabled()
	{
		return( plugin_interface.getPluginconfig().getPluginBooleanParameter( "rcm.search.enable", false ));
	}


	protected void
	setSearchEnabled(
		boolean b )
	{
		plugin_interface.getPluginconfig().setPluginParameter( "rcm.search.enable", b );
	}

	protected int
	getMinuumSearchRank()
	{
		return( plugin_interface.getPluginconfig().getPluginIntParameter( "rcm.search.min_rank", MIN_SEARCH_RANK_DEFAULT ));
	}
	
	public SearchProvider
	getSearchProvider()
	{
		return( search_provider );
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected void
	hookSearch()
	{		
		boolean enable = isRCMEnabled() && isSearchEnabled() && hasFTUXBeenShown();
		
		try{
				
			Utilities utilities = plugin_interface.getUtilities();
			
			if ( enable ){
			
				if ( search_provider == null ){
					
					search_provider = new RCM_SearchProvider( this );
						
					utilities.registerSearchProvider( search_provider );
				}
			}else{
				
				if ( search_provider != null ) {
					
					utilities.unregisterSearchProvider( search_provider );
					
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
		
		boolean isDefaultList = list.size() == source_map_defaults.size()
				&& list.containsAll(source_map_defaults);
		COConfigurationManager.setParameter(PARAM_SOURCES_ISDEFAULT, isDefaultList);
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
	
	public boolean isDefaultSourcesList() {
		return COConfigurationManager.getBooleanParameter(PARAM_SOURCES_ISDEFAULT, false);
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
			
			plugin_interface.getUtilities().unregisterJSONRPCServer(json_rpc_server);
			
			json_rpc_server.unload();
			
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
			
			current_ui.showFTUX( 
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
		lookupBySize( size, new String[]{ AENetworkClassifier.AT_PUBLIC });
	}
	
	public void
	lookupBySize(
		final long		size,
		final String[]	networks )
	
		throws IPCException
	{
		RelatedContentUI current_ui = ui;
		
		if ( current_ui == null ){
			
			throw( new IPCException( "UI not bound" ));
		}
		
		if ( !hasFTUXBeenShown() || !isRCMEnabled()){
			
			current_ui.showFTUX(
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
								
								current_ui.addSearch( size, networks );
							}
						}
					}
				});
		}else{
			
			current_ui.setUIEnabled( true );
			
			current_ui.addSearch( size, networks );
		}
	}
	
	public void
	lookupByExpression(
		String expression )
	
		throws IPCException
	{
		lookupByExpression( expression, new String[]{ AENetworkClassifier.AT_PUBLIC });
	}
	
	public void
	lookupByExpression(
		final String 	expression,
		final String[]	networks )
	
		throws IPCException
	{
		lookupByExpression( expression, networks, new HashMap<String,Object>());
	}
	
	public void
	lookupByExpression(
		final String 				expression,
		final String[]				networks,
		final Map<String,Object>	options )
	
		throws IPCException
	{
		RelatedContentUI current_ui = ui;
		
		if ( current_ui == null ){
			
			throw( new IPCException( "UI not bound" ));
		}
				
		final Runnable do_it = 
			new Runnable()
			{
				public void
				run()
				{
					if ( isRCMEnabled()){

						Boolean b_is_subscription = (Boolean)options.get( "Subscription" );
						
						boolean is_subscription = b_is_subscription != null && b_is_subscription;
	
						if ( is_subscription ){
							
							Map<String,Object>	properties = new HashMap<String, Object>();
								
							String name = (String)options.get( "Name" );
							
							if ( name == null ){
								
								name = expression;
							}
							
							properties.put( SearchProvider.SP_SEARCH_NAME, name );
							properties.put( SearchProvider.SP_SEARCH_TERM, expression );
							properties.put( SearchProvider.SP_NETWORKS, networks );
							
							properties.put( "_frequency_", 10 );
							
							try{
								getPluginInterface().getUtilities().getSubscriptionManager().requestSubscription(
									getSearchProvider(),
									properties );
									
							}catch( Throwable e ){
									
								Debug.out( e );
							}
						}else{
							
							RelatedContentUI current_ui = ui;

							if ( current_ui != null ){
								
								current_ui.setUIEnabled( true );
							
								current_ui.addSearch( expression, networks );
							}
						}
					}
				}
			};
				
		if ( !hasFTUXBeenShown() || !isRCMEnabled()){
			
			current_ui.showFTUX(
				new UserPrompterResultListener()
				{
					public void 
					prompterClosed(
						int result) 
					{									
						do_it.run();
					}
				});
		}else{
			
			do_it.run();
		}
	}
	
	public void
	lookupByHash(
		final byte[]	hash,
		final String	name )
	
		throws IPCException
	{
		lookupByHash( hash, new String[]{ AENetworkClassifier.AT_PUBLIC }, name );
	}

	public void
	lookupByHash(
		final byte[]	hash,
		final String[]	networks,
		final String	name )
	
		throws IPCException
	{
		RelatedContentUI current_ui = ui;
		
		if ( current_ui == null ){
			
			throw( new IPCException( "UI not bound" ));
		}
		
		if ( !hasFTUXBeenShown() || !isRCMEnabled()){
			
			current_ui.showFTUX( 
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
								
								current_ui.addSearch( hash, networks, name );
							}
						}
					}
				});
		}else{
			
			current_ui.setUIEnabled( true );
			
			current_ui.addSearch( hash, networks, name );
		}
	}
	
	public static String
	getNetworkString(
		String[]		networks )
	{
		if ( networks == null || networks.length == 0 ){
			
			return( "" );
			
		}else if ( networks.length == 1 ){
			
			if ( networks[0] != AENetworkClassifier.AT_PUBLIC ){
				
				return( " [" + networks[0] + "]" );
				
			}else{
				
				return( "" );
			}
		}else{
	
			String str = "";
			
			for ( String net: networks ){
				
				str += (str.length()==0?"":",") + net;
			}
			
			return( " [" + str + "]" );
		}
	}


	public static String
	getMagnetURI(
		RelatedContent		rc )
	{
		String uri = UrlUtils.getMagnetURI( rc.getHash(), rc.getTitle(), rc.getNetworks());
		
		String[] tags = rc.getTags();
		
		if ( tags != null ){
			
			for ( String tag: tags ){
				
				uri += "&tag=" + UrlUtils.encode( tag );
			}
		}
		
		return( uri );
	}
	
	public boolean isDestroyed() {
		return destroyed;
	}
}
