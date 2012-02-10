/*
 * Created on Sep 16, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.xmwebui;

import java.io.*;
import java.net.URL;
import java.util.*;

import java.util.Properties;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentDownloader;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.webplugin.WebPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.util.MultiPartDecoder;
import com.aelitis.azureus.plugins.remsearch.RemSearchPluginPageGenerator;
import com.aelitis.azureus.plugins.remsearch.RemSearchPluginPageGeneratorAdaptor;
import com.aelitis.azureus.plugins.remsearch.RemSearchPluginSearch;
import com.aelitis.azureus.util.JSONUtils;

public class 
XMWebUIPlugin
	extends WebPlugin
	implements UnloadablePlugin, DownloadManagerListener
{
    public static final int DEFAULT_PORT    = 9091;

    private static Properties defaults = new Properties();

    static{

        defaults.put( WebPlugin.PR_DISABLABLE, new Boolean( true ));
        defaults.put( WebPlugin.PR_ENABLE, new Boolean( true ));
        defaults.put( WebPlugin.PR_PORT, new Integer( DEFAULT_PORT ));
        defaults.put( WebPlugin.PR_ROOT_DIR, "transmission/web" );
        defaults.put( WebPlugin.PR_ENABLE_KEEP_ALIVE, new Boolean(true));
        defaults.put( WebPlugin.PR_HIDE_RESOURCE_CONFIG, new Boolean(true));
        defaults.put( WebPlugin.PR_PAIRING_SID, "xmwebui" );
    }

    private static final String	SEARCH_PREFIX 	= "/psearch";
    private static final int	SEARCH_TIMEOUT	= 60*1000;
    
    private boolean	view_mode;
    
    private BooleanParameter trace_param;
    private BooleanParameter hide_ln_param;
    
    private TorrentAttribute	t_id;
    
    private List<Long>	recently_removed = new ArrayList<Long>();
    
    private RemSearchPluginPageGenerator	search_handler;
    private TimerEventPeriodic				search_timer;
    
    public
    XMWebUIPlugin()
    {
    	super( defaults );
    	
    	search_handler = 
			new RemSearchPluginPageGenerator(
					new RemSearchPluginPageGeneratorAdaptor()
					{
						public void 
						searchReceived(
							String originator )
								
							throws IOException 
						{
						}
							
						public void 
						searchCreated(
							RemSearchPluginSearch search )
						{
						}
						
						public void 
						log(
							String str )
						{
							XMWebUIPlugin.this.log( str );
						};
						
						public void 
						log(
							String 		str,
							Throwable 	e )
						{
							XMWebUIPlugin.this.log( str, e );
						};
					},
					SEARCH_PREFIX,
					null,
					16,
					100,
					false );
    			
    }
    
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		super.initialize( _plugin_interface );
		
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( 
				"com.aelitis.azureus.plugins.xmwebui.internat.Messages" );
		
		t_id = plugin_interface.getTorrentManager().getPluginAttribute( "xmui.dl.id" );
		
		BasicPluginConfigModel	config = getConfigModel();
			
        int port = plugin_interface.getPluginconfig().getPluginIntParameter( WebPlugin.CONFIG_PORT, CONFIG_PORT_DEFAULT );

		config.addLabelParameter2( "xmwebui.blank" );

		config.addHyperlinkParameter2( "xmwebui.openui", "http://127.0.0.1:" + port + "/" );
		
		config.addLabelParameter2( "xmwebui.blank" );

		hide_ln_param = config.addBooleanParameter2( "xmwebui.hidelownoise", "xmwebui.hidelownoise", true );

		trace_param = config.addBooleanParameter2( "xmwebui.trace", "xmwebui.trace", false );
		
		ConfigParameter mode_parameter = plugin_interface.getPluginconfig().getPluginParameter( WebPlugin.CONFIG_MODE );

		if ( mode_parameter == null ){

			view_mode = true;

		}else{

			mode_parameter.addConfigParameterListener(
				new ConfigParameterListener()
				{
					public void 
					configParameterChanged(
						ConfigParameter param )
					{
						setViewMode();
					}
				});

			setViewMode();
		}
		
		plugin_interface.getDownloadManager().addListener( this );
		
		search_timer = SimpleTimer.addPeriodicEvent(
			"XMSearchTimeout",
			30*1000,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent event ) 
				{
					Map<String,RemSearchPluginSearch> searches = search_handler.getSearches();
					
					Iterator<RemSearchPluginSearch> it = searches.values().iterator();
						
					while( it.hasNext()){
					
						RemSearchPluginSearch search = it.next();
						
						if ( search.getAge() > SEARCH_TIMEOUT ){
																			
							log( "Timeout: " + search.getString());
							
							search.destroy();
						}
					}
				}
			});
	}
	
	public void 
	unload() 
		
		throws PluginException 
	{
		if ( search_timer != null ){
			
			search_timer.cancel();
			
			search_timer = null;
		}
		
		plugin_interface.getDownloadManager().removeListener( this );

		super.unloadPlugin();
	}
	
	protected void
	setViewMode()
	{
		String mode_str = plugin_interface.getPluginconfig().getPluginStringParameter( WebPlugin.CONFIG_MODE, ((WebPlugin)plugin_interface.getPlugin()).CONFIG_MODE_DEFAULT );

		view_mode = !mode_str.equalsIgnoreCase( WebPlugin.CONFIG_MODE_FULL );
	}
	   
	public void
	downloadAdded(
		Download	download )
	{
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		synchronized( recently_removed ){
			
			recently_removed.add( getID( download ));
		}
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		boolean logit = trace_param.getValue();

		try{
			String	url = request.getURL();
		
			// System.out.println( request.getHeader() );
			
			if ( url.equals( "/transmission/rpc" )){
							
				LineNumberReader lnr = new LineNumberReader( new InputStreamReader( request.getInputStream(), "UTF-8" ));
				
				StringBuffer	request_json_str = new StringBuffer(2048);
				
				while( true ){
					
					String	line = lnr.readLine();
					
					if ( line == null ){
						
						break;
					}
					
					request_json_str.append( line );
				}
							
				if ( logit ){
				
					log( "-> " + request_json_str );
				}
				
				Map request_json = JSONUtils.decodeJSON( request_json_str.toString());
				
				Map response_json = processRequest( request_json );
					
				String response_json_str = JSONUtils.encodeToJSON( response_json );
				
				if ( logit ){
				
					log( "<- " + response_json_str );
				}
				
				PrintWriter pw =new PrintWriter( new OutputStreamWriter( response.getOutputStream(), "UTF-8" ));
			
				pw.println( response_json_str );
				
				pw.flush();
				
				response.setContentType( "application/json; charset=UTF-8" );
				
				response.setGZIP( true );
				
				return( true );
			
			}else if ( 	url.startsWith( "/transmission/rpc?json=" ) ||
						url.startsWith( "/vuze/rpc?json=" )){
											
				StringBuffer	request_json_str = new StringBuffer(2048);
				
				request_json_str.append( UrlUtils.decode( url.substring( url.indexOf('?') + 6 )));
							
				if ( logit ){
				
					log( "-> " + request_json_str );
				}
				
				Map request_json = JSONUtils.decodeJSON( request_json_str.toString());
				
				Map response_json = processRequest( request_json );
					
				String response_json_str = JSONUtils.encodeToJSON( response_json );
				
				if ( logit ){
				
					log( "<- " + response_json_str );
				}
				
				PrintWriter pw =new PrintWriter( new OutputStreamWriter( response.getOutputStream(), "UTF-8" ));
			
				pw.println( response_json_str );
				
				pw.flush();
				
				response.setContentType( "application/json; charset=UTF-8" );
				
				response.setGZIP( true );
				
				return( true );
				
			}else if ( url.startsWith( "/transmission/upload" )){
					
				if ( logit ){
					
					log( "upload request" );
				}
				
				checkUpdatePermissions();
				
				boolean	add_stopped = url.endsWith( "paused=true" );
				
				String	content_type = (String)request.getHeaders().get( "content-type" );
				
				if ( content_type == null ){
					
					throw( new IOException( "Content-Type missing" ));
				}
				
				int bp = content_type.toLowerCase().indexOf( "boundary=" );
				
				if ( bp == -1 ){
					
					throw( new IOException( "boundary missing" ));
				}
				
				String	boundary = content_type.substring(bp+9).trim();
				
				int	ep = boundary.indexOf( ';' );
				
				if ( ep != -1 ){
					
					boundary = boundary.substring(0,ep).trim();
				}
				
				MultiPartDecoder.FormField[] fields = new MultiPartDecoder().decode(boundary, request.getInputStream());
				
				try{
					
					int	num_found = 0;
					
					for ( MultiPartDecoder.FormField field: fields ){
						
						String field_name = field.getName();
						
						if ( field_name.equalsIgnoreCase( "torrent_file" ) || field_name.equalsIgnoreCase( "torrent_files[]" )){
					
							num_found++;
							
							String torrent_file_name = field.getAttribute( "filename" );
							
							if ( torrent_file_name == null ){
								
								throw( new IOException( "upload filename missing" ));
							}
							
							InputStream tis = field.getInputStream();
							
							Torrent torrent;
							
							try{
								torrent = plugin_interface.getTorrentManager().createFromBEncodedInputStream( tis );
							
								torrent.setDefaultEncoding();
								
							}catch( Throwable e ){
								
								throw( new IOException( "Failed to deserialise torrent file: " + Debug.getNestedExceptionMessage(e)));
							}
							
							try{
								Download download = addTorrent( torrent, add_stopped );
								
								response.setContentType( "text/xml; charset=UTF-8" );
								
								response.getOutputStream().write( "<h1>200: OK</h1>".getBytes());
								
								return( true );
								
							}catch( Throwable e ){
								
								throw( new IOException( "Failed to add torrent: " + Debug.getNestedExceptionMessage(e)));
	
							}
						}
					}
					
					if ( num_found == 0 ){
						
						log( "No torrents found in upload request" );
					}
					
					return( true );
					
				}finally{
					
					for ( MultiPartDecoder.FormField field: fields ){
						
						field.destroy();
					}
				}
			}else if ( url.startsWith( "/transmission/web")){
					
				response.setReplyStatus( 301 );
					
				response.setHeader( "Location", "/" );
					
				return( true );
				
			}else if ( url.startsWith( SEARCH_PREFIX )){
				
				return( search_handler.generate( request, response ));
			
			}else{
			
				return( false );
			}
		}catch( PermissionDeniedException e ){
			
			response.setReplyStatus( 401 );

			return( true );
			
		}catch( IOException e ){
			
			if ( logit ){
			
				log( "Processing failed", e );
			}
			
			throw( e );
			
		}catch( Throwable e ){
			
			if ( logit ){
				
				log( "Processing failed", e );
			}
			
			throw( new IOException( "Processing failed: " + Debug.getNestedExceptionMessage( e )));
		}
	}
	
	private static Object add_torrent_lock = new Object();
	
	protected Download
	addTorrent(
		Torrent		torrent,
		boolean		add_stopped )
	
		throws DownloadException
	{
		synchronized( add_torrent_lock ){
			
			org.gudy.azureus2.plugins.download.DownloadManager dm = plugin_interface.getDownloadManager();
			
			Download download = dm.getDownload( torrent );
			
			if ( download == null ){
				
				if ( add_stopped ){
					
					download = dm.addDownloadStopped( torrent, null, null );
					
				}else{
					
					download = dm.addDownload( torrent );
				}
			}
			
			return( download );
		}
	}
	
	protected void
	checkUpdatePermissions()
		
		throws IOException
	{
		if ( view_mode ){
			
			log( "permission denied" );
			
			throw( new PermissionDeniedException());
		}
	}
	
	protected Map
	processRequest(
		Map						request )
	
		throws IOException
	{
		Map	response = new JSONObject();
		
		String method = (String)request.get( "method" );
		
		if ( method == null ){
			
			throw( new IOException( "'method' missing" ));
		}

		Map	args = (Map)request.get( "arguments" );
		
		if ( args == null ){
			
			args = new HashMap();
		}
		
		try{
			Map	result = processRequest( method, args );
			
			if ( result == null ){
				
				result = new HashMap();
			}
				
			response.put( "arguments", result );
			
			response.put( "result", "success" );
			
		}catch( PermissionDeniedException e ){
			
			throw( e );
			
		}catch( Throwable e ){
		
			response.put( "result", "error: " + Debug.getNestedExceptionMessage( e ));
		}
		
		Object	tag = request.get( "tag" );

		if ( tag != null ){
			
			response.put( "tag", tag );
		}
		
		return( response );
	}
	
	protected Map
	processRequest(
		String					method,
		Map						args )
	
		throws Exception
	{
		final Long	ZERO 	= new Long(0);

		final Boolean	TRUE 	= new Boolean(true);
		final Boolean	FALSE 	= new Boolean(false);
				
		JSONObject	result = new JSONObject();
				
			// http://trac.transmissionbt.com/browser/trunk/doc/rpc-spec.txt
				
		if ( method.equals( "session-get" ) || method.equals( "session-set" )){
							
			PluginConfig pc = plugin_interface.getPluginconfig();
			
			String 	save_dir 	= pc.getCoreStringParameter( PluginConfig.CORE_PARAM_STRING_DEFAULT_SAVE_PATH );
			int		tcp_port 	= pc.getCoreIntParameter( PluginConfig.CORE_PARAM_INT_INCOMING_TCP_PORT );
			int		up_limit 	= pc.getCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC );
			int		down_limit 	= pc.getCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC );
			int		glob_con	= pc.getCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL );
			int		tor_con 	= pc.getCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT );
			
			boolean auto_speed_on = pc.getCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON ) ||
									pc.getCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_SEEDING_ON );
						
			
			boolean require_enc = COConfigurationManager.getBooleanParameter("network.transport.encrypted.require");
			
			if ( method.equals( "session-set" )){
				
				checkUpdatePermissions();
				
				for( Map.Entry<String,Object> arg: ((Map<String,Object>)args).entrySet()){
					
					String	key = arg.getKey();
					Object	val	= arg.getValue();
					
					if ( key.equals( "speed-limit-down-enabled" )){
								
						boolean enabled = getBoolean( val );
						
						if ( !enabled && down_limit != 0 ){
							
							down_limit = 0;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, 0 );
						}
					}else if ( key.equals( "speed-limit-down" )){
					
						int	limit = ((Number)val).intValue();
						
						if ( limit != down_limit ){
							
							down_limit = limit;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, limit );
						}
					}else if ( key.equals( "speed-limit-up-enabled" )){
						
						if ( auto_speed_on ){
							
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON, false );
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_SEEDING_ON, false );
						}
						
						boolean enabled = getBoolean( val );
						
						if ( !enabled && up_limit != 0 ){
							
							up_limit = 0;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, 0 );
						}
					}else if ( key.equals( "speed-limit-up" )){
					
						if ( auto_speed_on ){
							
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON, false );
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_SEEDING_ON, false );
						}

						int	limit = ((Number)val).intValue();
						
						if ( limit != up_limit ){
							
							up_limit = limit;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, limit );
						}
					}else if ( key.equals( "download-dir" )){

						String	dir = (String)val;
						
						if ( !save_dir.equals( dir )){
							
							save_dir = dir;
							
							pc.setCoreStringParameter( PluginConfig.CORE_PARAM_STRING_DEFAULT_SAVE_PATH, dir );
						}
					}else if ( key.equals( "peer-port" ) || key.equals( "port" )){
						
						int	port = ((Number)val).intValue();

						if ( port != tcp_port ){
							
							tcp_port = port;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_INCOMING_TCP_PORT, port );
						}
					}else if ( key.equals( "encryption" )){
						
						String	value = (String)val;
						
						boolean	required = value.equals( "required" );
						
						if ( required != require_enc ){
							
							require_enc = required;
							
							COConfigurationManager.setParameter("network.transport.encrypted.require", required );
						}
					}else if ( key.equals( "seedRatioLimit" )){
						
						float	ratio = ((Number)val).floatValue();

						COConfigurationManager.setParameter( "Stop Ratio", ratio );
						
					}else{
						
						System.out.println( "Unhandled session-set field: " + key );
					}
				}
			}		
			
			float stop_ratio = COConfigurationManager.getFloatParameter( "Stop Ratio" );
			
			result.put( "alt-speed-down", new Long( 0 ) );				// number     max global download speed (in K/s)
			result.put( "alt-speed-enabled", FALSE );       			// boolean    true means use the alt speeds
			result.put( "alt-speed-time-begin",  new Long( 0 ));     	// number     when to turn on alt speeds (units: minutes after midnight)
			result.put( "alt-speed-time-enabled", FALSE );   			// boolean    true means the scheduled on/off times are used
			result.put( "alt-speed-time-end", new Long( 0 ));       	// number     when to turn off alt speeds (units: same)
			result.put( "alt-speed-time-day", new Long( 0 ));       	// number     what day(s) to turn on alt speeds (look at tr_sched_day)
			result.put( "alt-speed-up", new Long( 0 ));             	// number     max global upload speed (in K/s)
			result.put( "blocklist-enabled", FALSE );       			// boolean    true means enabled
			result.put( "blocklist-size", new Long( 0 ));           	// number     number of rules in the blocklist
			result.put( "dht-enabled", TRUE );              			// boolean    true means allow dht in public torrents
			result.put( "encryption", require_enc?"required":"preferred" );               		// string     "required", "preferred", "tolerated"
			result.put( "download-dir", save_dir );             		// string     default path to download torrents
			result.put( "peer-limit-global", new Long( glob_con ));        	// number     maximum global number of peers
			result.put( "peer-limit-per-torrent", new Long( tor_con ));   	// number     maximum global number of peers
			result.put( "pex-enabled", TRUE );              			// boolean    true means allow pex in public torrents
			result.put( "peer-port", new Long( tcp_port ) );            // number     port number
			result.put( "port", new Long( tcp_port ) );                	// number     port number
			result.put( "peer-port-random-on-start", FALSE ); 			// boolean    true means pick a random peer port on launch
			result.put( "port-forwarding-enabled", FALSE );  			// boolean    true means enabled
			result.put( "rpc-version", new Long( 6 ));              	// number     the current RPC API version
			result.put( "rpc-version-minimum", new Long( 6 ));      	// number     the minimum RPC API version supported
			result.put( "seedRatioLimit", new Double(stop_ratio) );          	// double     the default seed ratio for torrents to use
			result.put( "seedRatioLimited", stop_ratio>0 );         			// boolean    true if seedRatioLimit is honored by default
			result.put( "speed-limit-down", new Long(  down_limit ));         		// number     max global download speed (in K/s)
			result.put( "speed-limit-down-enabled", down_limit==0?FALSE:TRUE ); 	// boolean    true means enabled
			result.put( "speed-limit-up", new Long( up_limit ));           			// number     max global upload speed (in K/s)
			result.put( "speed-limit-up-enabled", up_limit==0?FALSE:TRUE );   		// boolean    true means enabled
			result.put( "version", plugin_interface.getPluginVersion() );           // string     
			result.put( "az-version", Constants.AZUREUS_VERSION );                  // string     
			
		}else if ( method.equals( "torrent-add" )){
			
			checkUpdatePermissions();
			
			boolean add_stopped = getBoolean( args.get( "paused" ));
			
			String	url = (String)args.get( "filename" );
			
			if ( url == null ){
				
				throw( new IOException( "url missing" ));
			}
			
			url = url.trim().replaceAll( " ", "%20");

				// hack due to core bug - have to add a bogus arg onto magnet uris else they fail to parse
			
			if ( url.toLowerCase().startsWith( "magnet:" )){
				
				url += "&dummy_param=1";
			}
			
			URL	torrent_url = new URL( url );
				
			Torrent torrent;
			
			try{
				TorrentDownloader dl = 
					plugin_interface.getTorrentManager().getURLDownloader( torrent_url, null, null );
				
				torrent = dl.download( Constants.DEFAULT_ENCODING );
				
			}catch( Throwable e ){

				e.printStackTrace();
				
				throw( new IOException( "torrent download failed: " + Debug.getNestedExceptionMessage( e )));
			}
			
			Download download = addTorrent( torrent, add_stopped );

			JSONObject torrent_details = new JSONObject();
			
			torrent_details.put( "id", new Long( getID( download )));
			torrent_details.put( "name", escapeXML( download.getName()));
			torrent_details.put( "hashString", ByteFormatter.encodeString( torrent.getHash()));
			
			result.put( "torrent-added", torrent_details );
			
		}else if ( method.equals( "torrent-start-all" )){

			checkUpdatePermissions();
			
			plugin_interface.getDownloadManager().startAllDownloads();
		
		}else if ( method.equals( "torrent-stop-all" )){

			checkUpdatePermissions();
			
			plugin_interface.getDownloadManager().stopAllDownloads();
			 
		}else if ( method.equals( "torrent-start" )){

			checkUpdatePermissions();
			
			Object	ids = args.get( "ids" );

			List<Download>	downloads = getDownloads( ids );

			for ( Download download: downloads ){
				
				try{
					int	state = download.getState();
					
					if ( state != Download.ST_DOWNLOADING && state != Download.ST_SEEDING ){
					
						download.restart();
					}
				}catch( Throwable e ){
				}
			}
		}else if ( method.equals( "torrent-stop" )){

			checkUpdatePermissions();
			
			Object	ids = args.get( "ids" );

			List<Download>	downloads = getDownloads( ids );

			for ( Download download: downloads ){
				
				try{
					int	state = download.getState();
					
					if ( state != Download.ST_STOPPED ){
					
						download.stop();
					}
				}catch( Throwable e ){
				}
			}
		}else if ( method.equals( "torrent-verify" )){

			checkUpdatePermissions();
			
			Object	ids = args.get( "ids" );

			List<Download>	downloads = getDownloads( ids );

			for ( Download download: downloads ){
				
				try{
					int	state = download.getState();
					
					if ( state != Download.ST_STOPPED ){
					
						download.stop();
					}
					
					download.recheckData();
					
				}catch( Throwable e ){
				}
			}
		}else if ( method.equals( "torrent-remove" )){

			checkUpdatePermissions();
			
			Object	ids = args.get( "ids" );

			boolean	delete_data = getBoolean( args.get( "delete-local-data" ));
			
			List<Download>	downloads = getDownloads( ids );

			for ( Download download: downloads ){
				
				try{
					int	state = download.getState();
					
					if ( state != Download.ST_STOPPED ){
					
						download.stop();
					}
					
					if ( delete_data ){
						
						download.remove( true, true );
						
					}else{
						
						download.remove();	
					}
					
					synchronized( recently_removed ){
					
						recently_removed.add( getID( download ));
					}
				}catch( Throwable e ){
				}
			}
		}else if ( method.equals( "torrent-set" )){
			
			Object	ids = args.get( "ids" );
			
			if ( ids != null && ids instanceof String && ((String)ids).equals( "recently-active" )){
				
				synchronized( recently_removed ){
					
					if ( recently_removed.size() > 0 ){
						
						List<Long> removed = new ArrayList<Long>( recently_removed );
						
						recently_removed.clear();
						
						result.put( "removed", removed );
					}
				}
			}
			
			List<Download>	downloads = getDownloads( ids );
				
			JSONArray files_unwanted 	= (JSONArray)args.get( "files-unwanted" );
			JSONArray files_wanted 		= (JSONArray)args.get( "files-wanted" );
			JSONArray priority_high		= (JSONArray)args.get( "priority-high" );
			JSONArray priority_normal	= (JSONArray)args.get( "priority-normal" );
			JSONArray priority_low		= (JSONArray)args.get( "priority-low" );
			
			Long	speed_limit_down	= (Long)args.get( "speedLimitDownload" );
			Long	speed_limit_up		= (Long)args.get( "speedLimitUpload" );

			Long	l_uploaded_ever		= (Long)args.get( "uploadedEver" );
			Long	l_downloaded_ever 	= (Long)args.get( "downloadedEver" );

			long	uploaded_ever 	= l_uploaded_ever==null?-1:l_uploaded_ever.longValue();
			long	downloaded_ever = l_downloaded_ever==null?-1:l_downloaded_ever.longValue();
					
			for ( Download download: downloads ){
				
				Torrent t = download.getTorrent();
				
				if ( t == null ){
					
					continue;
				}
				
				if ( speed_limit_down != null ){
					
					download.setDownloadRateLimitBytesPerSecond( speed_limit_down.intValue());
				}
				
				if ( speed_limit_up != null ){
					
					download.setUploadRateLimitBytesPerSecond( speed_limit_up.intValue());
				}			
				
				DownloadManager	core_download = PluginCoreUtils.unwrap( download );
									
				DiskManagerFileInfo[] files = core_download.getDiskManagerFileInfo();
					
				if ( files_unwanted != null ){
					
					for ( int i=0;i<files_unwanted.size();i++){
						
						int	index = ((Long)files_unwanted.get( i )).intValue();
						
						if ( index >= 0 && index <= files.length ){
							
							files[index].setSkipped( true );
						}
					}
				}
				
				if ( files_wanted != null ){
					
					for ( int i=0;i<files_wanted.size();i++){
						
						int	index = ((Long)files_wanted.get( i )).intValue();
						
						if ( index >= 0 && index <= files.length ){
							
							files[index].setSkipped( false );
						}
					}
				}
				
				if ( priority_high != null ){
					
					for ( int i=0;i<priority_high.size();i++){
						
						int	index = ((Long)priority_high.get( i )).intValue();
						
						if ( index >= 0 && index <= files.length ){
							
							files[index].setPriority( 1 );
						}
					}
				}
				
				if ( priority_normal != null ){
					
					for ( int i=0;i<priority_normal.size();i++){
						
						int	index = ((Long)priority_normal.get( i )).intValue();
						
						if ( index >= 0 && index <= files.length ){
							
							files[index].setPriority( 0 );
						}
					}
				}
				
				if ( priority_low != null ){
					
					for ( int i=0;i<priority_low.size();i++){
						
						int	index = ((Long)priority_low.get( i )).intValue();
						
						if ( index >= 0 && index <= files.length ){
							
							files[index].setPriority( 0 );
						}
					}
				}
				
				if ( uploaded_ever != -1 || downloaded_ever != -1 ){
					
						// new method in 4511 B31
					
					try{
						download.getStats().resetUploadedDownloaded( uploaded_ever, downloaded_ever );
						
					}catch( Throwable e ){
					}
				}
			}
		}else if ( method.equals( "torrent-get" )){
					
			List<String>	fields = (List<String>)args.get( "fields" );
			
			if ( fields == null ){
				
				fields = new ArrayList();
			}
			
			Object	ids = args.get( "ids" );
			
			if ( ids != null && ids instanceof String && ((String)ids).equals( "recently-active" )){
				
				synchronized( recently_removed ){
					
					if ( recently_removed.size() > 0 ){
						
						List<Long> removed = new ArrayList<Long>( recently_removed );
						
						recently_removed.clear();
						
						result.put( "removed", removed );
					}
				}
			}
			
			List<Download>	downloads = getDownloads( ids );
			
			JSONArray	torrents = new JSONArray();
			
			result.put( "torrents", torrents );
			
			for ( Download download: downloads ){
				
				Torrent t = download.getTorrent();
				
				if ( t == null ){
					
					continue;
				}
				
				DownloadManager	core_download = PluginCoreUtils.unwrap( download );
				
				PEPeerManager pm = core_download.getPeerManager();
				
				DownloadStats	stats = download.getStats();
				
				JSONObject torrent = new JSONObject();
				
				torrents.add( torrent );
				
				int	peers_from_us 	= 0;
				int	peers_to_us		= 0;
				
				if ( pm != null ){
					
					List<PEPeer> peers = pm.getPeers();
					
					for ( PEPeer peer: peers ){
						
						PEPeerStats pstats = peer.getStats();
						
						if ( pstats.getDataReceiveRate() > 0 ){
							
							peers_to_us++;
						}
						
						if ( pstats.getDataSendRate() > 0 ){
							
							peers_from_us++;
						}
					}
				}
				
				for ( String field: fields ){
					
					Object	value = null;
					
					if ( field.equals( "addedDate" )){
						value = new Long(core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME)/1000);
					}else if ( field.equals( "announceURL" )){	
						value = t.getAnnounceURL().toExternalForm();
					}else if ( field.equals( "comment" )){	
						value = t.getComment();
					}else if ( field.equals( "creator" )){	
						value = t.getCreatedBy();
					}else if ( field.equals( "dateCreated" )){	
						value = new Long( t.getCreationDate());
					}else if ( field.equals( "downloadedEver" )){
						value = new Long( stats.getDownloaded() + stats.getDiscarded() + stats.getHashFails());
					}else if ( field.equals( "error" )){
						String str = download.getErrorStateDetails();
						
							// o = none, 1=tracker warn,2=tracker error,2=other
						if ( str != null && str.length() > 0 ){
							value = new Long(3);
						}else{
							value = ZERO;
							TRTrackerAnnouncer tracker_client = core_download.getTrackerClient();
							
							if ( tracker_client != null ){
								TRTrackerAnnouncerResponse x = tracker_client.getBestAnnouncer().getLastResponse();
								if ( x != null ){
									if ( x.getStatus() == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR ){
										value = new Long(2);
									}
								}
							}else{
								DownloadScrapeResult x = download.getLastScrapeResult();
								if ( x != null ){
									if ( x.getResponseType() == DownloadScrapeResult.RT_ERROR ){
										String status = x.getStatus();
										
										if ( status != null && status.length() > 0 ){
										
											value = new Long(2);
										}
									}
								}
							}
						}
					}else if ( field.equals( "errorString" )){	
						String str = download.getErrorStateDetails();
						
						if ( str != null && str.length() > 0 ){
							value = str;
						}else{
							value = "";
							TRTrackerAnnouncer tracker_client = core_download.getTrackerClient();
							
							if ( tracker_client != null ){
								TRTrackerAnnouncerResponse x = tracker_client.getBestAnnouncer().getLastResponse();
								if ( x != null ){
									if ( x.getStatus() == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR ){
										value = x.getStatusString();
									}
								}
							}else{
								DownloadScrapeResult x = download.getLastScrapeResult();
								if ( x != null ){
									if ( x.getResponseType() == DownloadScrapeResult.RT_ERROR ){
										value = x.getStatus();
									}
								}
							}
						}
					}else if ( field.equals( "eta" )){
							// infinite -> 215784000
						long eta_secs = stats.getETASecs();
						
						eta_secs = Math.min( eta_secs, 215784000 );
						
						value = new Long( eta_secs );
					}else if ( field.equals( "hashString" )){	
						value = ByteFormatter.encodeString( t.getHash());
					}else if ( field.equals( "haveUnchecked" )){	
						value = ZERO;
					}else if ( field.equals( "haveValid" )){
						value = new Long( stats.getDownloaded());
					}else if ( field.equals( "id" )){		
						value = new Long( getID( download ));
					}else if ( field.equals( "trackerSeeds" )){
						DownloadScrapeResult scrape = download.getLastScrapeResult();
						value = new Long( scrape==null?0:scrape.getSeedCount());
					}else if ( field.equals( "trackerLeechers" )){
						DownloadScrapeResult scrape = download.getLastScrapeResult();
						value = new Long( scrape==null?0:scrape.getNonSeedCount());
					}else if ( field.equals( "leechers" )){	
						if ( pm == null ){
							value = new Long(0);
						}else{
							value = new Long( pm.getNbPeers());
						}
					}else if ( field.equals( "leftUntilDone" )){	
						value = new Long( stats.getRemaining());
					}else if ( field.equals( "name" )){	
						value = download.getName();
					}else if ( field.equals( "peersConnected" )){	
						if ( pm == null ){
							value = new Long(0);
						}else{
							value = new Long( pm.getNbPeers() + pm.getNbSeeds());
						}
					}else if ( field.equals( "peersGettingFromUs" )){	
						value = new Long( peers_from_us );
					}else if ( field.equals( "peersSendingToUs" )){
						value = new Long( peers_to_us );
					}else if ( field.equals( "isPrivate" )){
						value = t.isPrivate()?TRUE:FALSE;
					}else if ( field.equals( "rateDownload" )){	
						value = new Long( stats.getDownloadAverage());
					}else if ( field.equals( "rateUpload" )){
						value = new Long( stats.getUploadAverage());
					}else if ( field.equals( "speedLimitDownload" )){	
						value = new Long( download.getDownloadRateLimitBytesPerSecond());
					}else if ( field.equals( "speedLimitUpload" )){
						value = new Long( download.getUploadRateLimitBytesPerSecond());
					}else if ( field.equals( "seeders" )){
						if ( pm == null ){
							value = new Long(-1);
						}else{
							value = new Long( pm.getNbSeeds());
						}
					}else if ( field.equals( "sizeWhenDone" )){	
						value = new Long( t.getSize());	// TODO: excluded DND
					}else if ( field.equals( "status" )){	
							// 1 - waiting to verify
							// 2 - verifying
							// 4 - downloading
							// 5 - queued (incomplete)
							// 8 - seeding
							// 9 - queued (complete)
							// 16 - paused
						
						int	status_int = 7;
						
						if ( download.isPaused()){
							
							status_int = 16;
														
						}else{
							int state = download.getState();
							
							if ( state == Download.ST_DOWNLOADING ){
								
								status_int = 4;
								
							}else if ( state == Download.ST_SEEDING ){
								
								status_int = 8;
								
							}else if ( state == Download.ST_QUEUED ){

								if ( download.isComplete()){
									
									status_int = 9;
									
								}else{
									
									status_int = 5;
								}
							}else if ( state == Download.ST_STOPPED || state == Download.ST_STOPPING ){
								
								status_int = 16;
								
							}else if ( state == Download.ST_ERROR ){
								
								status_int = 0;
								
							}else{
								
								if ( core_download.getState() == DownloadManager.STATE_CHECKING ){
								
									status_int = 2;
									
								}else{
									
									status_int = 1;
								}
							}
						}
						value = new Long(status_int);
					}else if ( field.equals( "swarmSpeed" )){	
						value = new Long( core_download.getStats().getTotalAveragePerPeer());
					}else if ( field.equals( "totalSize" )){
						value = new Long( t.getSize());
					}else if ( field.equals( "pieceCount" )){
						value = new Long( t.getPieceCount());
					}else if ( field.equals( "pieceSize" )){
						value = new Long( t.getPieceSize());
					}else if ( field.equals( "metadataPercentComplete" )){
						value = new Long( 100 );
					}else if ( field.equals( "uploadedEver" )){	
						value = new Long( stats.getUploaded());
					}else if ( field.equals( "recheckProgress" )){
						
						double x = 0;
						
						if ( core_download.getState() == DownloadManager.STATE_CHECKING ){
							
							DiskManager dm = core_download.getDiskManager();
							
							if ( dm != null ){
								
								x = ((double)stats.getCompleted())/1000;
							}
						}
						
						value = new Double( x );
					}else if ( field.equals( "uploadRatio" )){
						value = new Double( ((double)stats.getShareRatio())/1000);
					}else if ( field.equals( "seedRatioLimit" )){
						value = new Double( COConfigurationManager.getFloatParameter( "Stop Ratio" ));
					}else if ( field.equals( "seedRatioMode" )){
						// 0=global,1=local,2=unlimited
						value = new Long(1);
					}else if ( field.equals( "downloadDir" )){
						value = download.getSavePath();
					}else if ( field.equals( "files" )){
						
						List<JSONObject> file_list = new ArrayList<JSONObject>();
						
						DiskManagerFileInfo[] files = core_download.getDiskManagerFileInfo();
						
						for ( DiskManagerFileInfo file: files ){
							
							JSONObject obj = new JSONObject();
							
							file_list.add( obj );
							
							obj.put( "bytesCompleted", new Long( file.getDownloaded()));	// this must be a spec error...
							obj.put( "length", new Long( file.getLength()));
							obj.put( "name", file.getTorrentFile().getRelativePath());
						}
						
						value = file_list;
						
					}else if ( field.equals( "fileStats" )){
						
						List<JSONObject> stats_list = new ArrayList<JSONObject>();
						
						DiskManagerFileInfo[] files = core_download.getDiskManagerFileInfo();
						
						for ( DiskManagerFileInfo file: files ){
							
							JSONObject obj = new JSONObject();
							
							stats_list.add( obj );
							
							obj.put( "bytesCompleted", new Long( file.getDownloaded()));
							obj.put( "wanted", new Boolean( !file.isSkipped()));
							obj.put( "priority", new Long( file.getPriority()>0?1:0));
						}
						
						value = stats_list;
					}else{
						System.out.println( "Unhandled get-torrent field: " + field );
					}
					
					if ( value != null ){
						
						if ( value instanceof String ){
							
							value = escapeXML((String)value);
						}
						torrent.put( field, value );
					}
				}
			}
		}else{
			
			System.out.println( "unhandled methd: " + method + " - " + args );
		}

		return( result );
	}
	
	protected List<Download>
	getDownloads(
		Object		ids )
	{
		List<Download>	downloads = new ArrayList<Download>();
		
		Download[] all_downloads = plugin_interface.getDownloadManager().getDownloads();

		List<Long>		selected_ids 	= new ArrayList<Long>();
		List<String>	selected_hashes = new ArrayList<String>();
		
		if ( ids == null ){
			
		}else if ( ids instanceof String ){
			
			ids = null;
			
		}else if ( ids instanceof Number ){
			
			selected_ids.add(((Number)ids).longValue());
			
		}else if ( ids instanceof List ){
			
			List l = (List)ids;
			
			for (Object o: l ){
				
				if ( o instanceof Number ){
					
					selected_ids.add(((Number)o).longValue());
					
				}else if ( o instanceof String ){
					
					selected_hashes.add((String)o);
				}
			}
		}
		
		boolean hide_ln = hide_ln_param.getValue();
		
		for( Download download: all_downloads ){
			
			if ( hide_ln && download.getFlag( Download.FLAG_LOW_NOISE )){
				
				continue;
			}
			
			if ( ids == null ){
				
				downloads.add( download );
				
			}else{
				
				long	id = getID( download );
				
				if ( selected_ids.contains( id )){
					
					downloads.add( download );
					
				}else{
					
					Torrent t = download.getTorrent();
					
					if ( t != null ){
						
						if ( selected_hashes.contains( ByteFormatter.encodeString( t.getHash()))){
							
							downloads.add( download );
						}
					}
				}
			}
		}
		
		return( downloads );
	}
	
	protected boolean
	getBoolean(
		Object	o )
	{
		if ( o instanceof Boolean ){
			
			return((Boolean)o);
						
		}else if ( o instanceof String ){
			
			return( ((String)o).equalsIgnoreCase( "true" ));
			
		}else if ( o instanceof Number ){
			
			return(((Number)o).intValue()!=0);
			
		}else{
			
			return( false );
		}
	}
	
	protected long
	getID(
		Download		d )
	{
			
			// I was trying to be clever and allocate unique ids for downloads. however,
			// the webui assumes they are consecutive and give a queue index. ho hum
			
		// return( d.getIndex());
		
		long id = d.getLongAttribute( t_id );
			
		if ( id == 0 ){
		
			synchronized( this ){
				
				PluginConfig config = plugin_interface.getPluginconfig();
			
				id = config.getPluginLongParameter( "xmui.next.id", 1 );
				
				config.setPluginParameter( "xmui.next.id", id + 1 );
			}
			
			d.setLongAttribute( t_id, id );
		}
		
		return( id );
	}
	
	protected class
	PermissionDeniedException
		extends IOException
	{		
	}
	
	protected String
	escapeXML(
		String	str )
	{
		if ( str == null ){
			
			return( "" );
			
		}
		str = str.replaceAll( "&", "&amp;" );
		str = str.replaceAll( ">", "&gt;" );
		str = str.replaceAll( "<", "&lt;" );
		str = str.replaceAll( "\"", "&quot;" );
		str = str.replaceAll( "--", "&#45;&#45;" );
		
		return( str );
	}
	
	protected String
	escapeXSS(
		String	str )
	{
		if ( str == null ){
			
			return( "" );
			
		}
		str = str.replaceAll( "#", "&#35" );
		str = escapeXML(str);
		str = str.replaceAll( "\\(", "&#40;" );
		str = str.replaceAll( "\\)", "&#41;" );
		
		return( str );
	}
}
