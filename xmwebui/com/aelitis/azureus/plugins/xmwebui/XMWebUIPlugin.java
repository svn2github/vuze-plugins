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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraper;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentDownloader;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.webplugin.WebPlugin;

import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.core.util.MultiPartDecoder;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.remsearch.RemSearchPluginPageGenerator;
import com.aelitis.azureus.plugins.remsearch.RemSearchPluginPageGeneratorAdaptor;
import com.aelitis.azureus.plugins.remsearch.RemSearchPluginSearch;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.DefaultRankCalculator;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesDefaultPlugin;
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
    
    private Map<String, String> ip_to_session_id = new HashMap<String, String>();
    
    private RemSearchPluginPageGenerator	search_handler;
    private TimerEventPeriodic				search_timer;
    
    private boolean							check_ids_outstanding = true;
    
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

			checkViewMode();

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
	
	private void
	checkViewMode()
	{
		if ( view_mode ){
			
			return;
		}
		
		PluginConfig pc = plugin_interface.getPluginconfig();
		
		String 	data_dir 	= pc.getCoreStringParameter( PluginConfig.CORE_PARAM_STRING_DEFAULT_SAVE_PATH );

		if ( data_dir == null || data_dir.length() == 0 || !new File( data_dir ).canWrite()){
			
			Logger.log(
				new LogAlert(
					true,
					LogAlert.AT_ERROR,
					MessageText.getString( "xmwebui.error.data_path" )));	
		}
		
		if ( !pc.getUnsafeBooleanParameter( "Save Torrent Files" )){
			
			Logger.log(
					new LogAlert(
						true,
						LogAlert.AT_ERROR,
						MessageText.getString( "xmwebui.error.torrent_path" )));
		}else{
			
			String 	torrent_dir 	= pc.getUnsafeStringParameter( "General_sDefaultTorrent_Directory" );

			if ( torrent_dir == null || torrent_dir.length() == 0 || !new File( torrent_dir ).canWrite()){
				
				Logger.log(
					new LogAlert(
						true,
						LogAlert.AT_ERROR,
						MessageText.getString( "xmwebui.error.torrent_path" )));	
			}
		}
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
		
		checkViewMode();
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
			
			long id = getID( download, false );
			
			if ( id > 0 ){
			
				recently_removed.add( id );
			}
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
			// Set cookie just in case client is looking for one..
			response.setHeader( "Set-Cookie", "X-Transmission-Session-Id=" + getSessionID(request) + "; HttpOnly" );
			// This is the actual spec for massing session-id
			response.setHeader("X-Transmission-Session-Id", getSessionID(request));

			if (!isSessionValid(request)) {
				log("SessionID " + getSessionID(request) + " Not Valid -- returning 409");
				response.setReplyStatus( 409 );
				return true;
			}
			
			String	url = request.getURL();
		
			//System.out.println( request.getHeader() );
			
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
	
	private String
	getCookie(
		String		cookies,
		String		cookie_id)
	{
		if ( cookies == null ){
			
			return null;
		}
		
		String[] cookie_list = cookies.split( ";" );
		
		for ( String cookie: cookie_list ){
			
			String[] bits = cookie.split( "=" );
			
			if ( bits.length == 2 ){
				
				if ( bits[0].trim().equals( cookie_id )){
					
					return bits[1].trim();
				}
			}
		}
		
		return null;
	}

	private boolean 
	isSessionValid(
			TrackerWebPageRequest request) 
 {
		String session_id = getSessionID(request);
		String header_session_id = (String) request.getHeaders().get(
				"X-Transmission-Session-Id");
		if (header_session_id == null) {
			header_session_id = (String) request.getHeaders().get(
					"x-transmission-session-id");
		}
		if (header_session_id == null) {
			header_session_id = getCookie(
					(String) request.getHeaders().get("cookie"),
					"X-Transmission-Session-Id");
		}

		if (header_session_id == null) {
			log("no header " + session_id);
			return false;
		}

		return (header_session_id.equals(session_id));
	}

	private String 
	getSessionID(
			TrackerWebPageRequest request) 
	{
		String clientAddress = request.getClientAddress();
		
		synchronized (ip_to_session_id) {
			String session_id = ip_to_session_id.get(clientAddress);
			if (session_id == null) {
				session_id = Double.toHexString(Math.random());
				ip_to_session_id.put(clientAddress, session_id);
			}
			
			return session_id;
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
		Map	response = new HashMap();
		
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

	static Number getNumber(
			Object val)
	{
		return getNumber(val, 0);
	}

	static Number getNumber(
			Object val,
			Number defaultNumber)
	{
		if (val instanceof Number) {
			return (Number) val;
		}
		if (val instanceof String) {
			NumberFormat format = NumberFormat.getInstance();
			try {
				Number number = format.parse((String) val);
				return number;
			} catch (ParseException e) {
				return defaultNumber;
			}
		}
		return defaultNumber;
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
				
		Map	result = new HashMap();
				
			// https://trac.transmissionbt.com/browser/trunk/extras/rpc-spec.txt
		
			// to get 271 working with this backend change remote.js RPC _Root to be
			// _Root                   : './transmission/rpc',
		
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
					try {
					if ( key.equals( "speed-limit-down-enabled" ) || key.equals("downloadLimited")){
								
						boolean enabled = getBoolean( val );
						
						if ( !enabled && down_limit != 0 ){
							
							down_limit = 0;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, 0 );
						}
					}else if ( key.equals( "speed-limit-down" ) || key.equals("downloadLimit")){
					
						int	limit = getNumber(val).intValue();
						
						if ( limit != down_limit ){
							
							down_limit = limit;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, limit );
						}
					}else if ( key.equals( "speed-limit-up-enabled") || key.equals("uploadLimited")){
						
						if ( auto_speed_on ){
							
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON, false );
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_SEEDING_ON, false );
						}
						
						boolean enabled = getBoolean( val );
						
						if ( !enabled && up_limit != 0 ){
							
							up_limit = 0;
							
							pc.setCoreIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, 0 );
						}
					}else if ( key.equals( "speed-limit-up" ) || key.equals("uploadLimit")){
					
						if ( auto_speed_on ){
							
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON, false );
							pc.setCoreBooleanParameter( PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_SEEDING_ON, false );
						}

						int	limit = getNumber(val).intValue();
						
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
						
						int	port = getNumber(val).intValue();

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
						// RPC v5

						float	ratio = getNumber(val).floatValue();

						COConfigurationManager.setParameter( "Stop Ratio", ratio );
						
					}else{
						
						System.out.println( "Unhandled session-set field: " + key );
					}
					} catch (Throwable t) {
						Debug.out(key + ":" + val, t);
					}
				}
			}		
			
			float stop_ratio = COConfigurationManager.getFloatParameter( "Stop Ratio" );
			
			String az_mode = plugin_interface.getUtilities().getFeatureManager().isFeatureInstalled( "core" )?"plus":"trial";

			IpFilter ipFilter = IpFilterManagerFactory.getSingleton().getIPFilter();
			String filter_url = COConfigurationManager.getStringParameter("Ip Filter Autoload File", "");
			final PluginInterface dht_pi = 
					plugin_interface.getPluginManager().getPluginInterfaceByClass(
								DHTPlugin.class );
			DHTPlugin dht = (DHTPlugin)dht_pi.getPlugin();
			
			PluginInterface piUTP = plugin_interface.getPluginManager().getPluginInterfaceByID("azutp");
			boolean hasUTP = piUTP != null && piUTP.getPluginState().isOperational() && piUTP.getPluginconfig().getPluginBooleanParameter("utp.enabled", true);
			
			
	    result.put(TransmissionVars.TR_PREFS_KEY_BLOCKLIST_ENABLED, ipFilter.isEnabled() );
	    result.put(TransmissionVars.TR_PREFS_KEY_BLOCKLIST_URL, filter_url );
			// RPC v5, but no constant!
			result.put( "blocklist-size", ipFilter.getNbRanges());           	// number     number of rules in the blocklist
	    result.put(TransmissionVars.TR_PREFS_KEY_MAX_CACHE_SIZE_MB, 0 );  // TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_DHT_ENABLED, dht.isEnabled() );
	    result.put(TransmissionVars.TR_PREFS_KEY_UTP_ENABLED, hasUTP );
	    result.put(TransmissionVars.TR_PREFS_KEY_LPD_ENABLED, false );
	    result.put(TransmissionVars.TR_PREFS_KEY_DOWNLOAD_DIR, save_dir);
	    result.put("download-dir-free-space", -1);

	    result.put(TransmissionVars.TR_PREFS_KEY_DSPEED_KBps, down_limit );
	    result.put(TransmissionVars.TR_PREFS_KEY_DSPEED_ENABLED, down_limit != 0 );
	    result.put(TransmissionVars.TR_PREFS_KEY_ENCRYPTION, require_enc?"required":"preferred" );               		// string     "required", "preferred", "tolerated"
	    result.put(TransmissionVars.TR_PREFS_KEY_IDLE_LIMIT, 30 ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_IDLE_LIMIT_ENABLED, false );//TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_INCOMPLETE_DIR, save_dir );
	    result.put(TransmissionVars.TR_PREFS_KEY_INCOMPLETE_DIR_ENABLED, false );//TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_MSGLEVEL, TR_MSG_INF );//TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_DOWNLOAD_QUEUE_SIZE, 5 );//TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_DOWNLOAD_QUEUE_ENABLED, true ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_PEER_LIMIT_GLOBAL, glob_con );
	    result.put(TransmissionVars.TR_PREFS_KEY_PEER_LIMIT_TORRENT, tor_con );
	    result.put(TransmissionVars.TR_PREFS_KEY_PEER_PORT, tcp_port );
	    result.put(TransmissionVars.TR_PREFS_KEY_PEER_PORT_RANDOM_ON_START, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_PEER_PORT_RANDOM_LOW, 49152 ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_PEER_PORT_RANDOM_HIGH, 65535 ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_PEER_SOCKET_TOS, TR_DEFAULT_PEER_SOCKET_TOS_STR ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_PEX_ENABLED, true ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_PORT_FORWARDING, false ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_PREALLOCATION, TR_PREALLOCATE_SPARSE ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_PREFETCH_ENABLED, DEFAULT_PREFETCH_ENABLED ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_QUEUE_STALLED_ENABLED, true ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_QUEUE_STALLED_MINUTES, 30 ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RATIO, 2.0 ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RATIO_ENABLED, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RENAME_PARTIAL_FILES, true ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RPC_AUTH_REQUIRED, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RPC_BIND_ADDRESS, "0.0.0.0" ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RPC_ENABLED, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RPC_PASSWORD, "" ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RPC_USERNAME, "" ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_RPC_WHITELIST, TR_DEFAULT_RPC_WHITELIST ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_RPC_WHITELIST_ENABLED, true ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_RPC_PORT, atoi( TR_DEFAULT_RPC_PORT_STR ) ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_RPC_URL, TR_DEFAULT_RPC_URL_STR ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_SCRAPE_PAUSED_TORRENTS, true ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_SCRIPT_TORRENT_DONE_FILENAME, "" ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_SCRIPT_TORRENT_DONE_ENABLED, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_SEED_QUEUE_SIZE, 10 ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_SEED_QUEUE_ENABLED, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_ALT_SPEED_ENABLED, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_ALT_SPEED_UP_KBps, 50 );  //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_ALT_SPEED_DOWN_KBps, 50 );  //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_ALT_SPEED_TIME_BEGIN, 540 ); /* 9am */  //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_ALT_SPEED_TIME_ENABLED, false ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_ALT_SPEED_TIME_END, 1020 ); /* 5pm */ //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_ALT_SPEED_TIME_DAY, TransmissionVars.TR_SCHED_ALL ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_USPEED_KBps, up_limit );
	    result.put(TransmissionVars.TR_PREFS_KEY_USPEED_ENABLED, up_limit != 0);
	    result.put(TransmissionVars.TR_PREFS_KEY_UMASK, 022 ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_UPLOAD_SLOTS_PER_TORRENT, 14 ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_BIND_ADDRESS_IPV4, TR_DEFAULT_BIND_ADDRESS_IPV4 ); //TODO
	    //result.put(TransmissionVars.TR_PREFS_KEY_BIND_ADDRESS_IPV6, TR_DEFAULT_BIND_ADDRESS_IPV6 ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_START, true ); //TODO
	    result.put(TransmissionVars.TR_PREFS_KEY_TRASH_ORIGINAL, false ); //TODO


			result.put( "port", new Long( tcp_port ) );                	// number     port number
			result.put( "rpc-version", new Long( 14 ));              	// number     the current RPC API version
			result.put( "rpc-version-minimum", new Long( 6 ));      	// number     the minimum RPC API version supported
			result.put( "seedRatioLimit", new Double(stop_ratio) );          	// double     the default seed ratio for torrents to use
			result.put( "seedRatioLimited", stop_ratio>0 );         			// boolean    true if seedRatioLimit is honored by default

			String version = plugin_interface.getPluginVersion();
			result.put( "version",  version == null ? "Source" : version);           // string     
			result.put( "az-version", Constants.AZUREUS_VERSION );                  // string     
			result.put( "az-mode", az_mode );										// string
			
		}else if ( method.equals( "session-stats" )){

			method_Session_Stats(args, result);
			
		}else if ( method.equals( "torrent-add" )){
			
			method_Torrent_Add(args, result);
			
		}else if ( method.equals( "torrent-start-all" )){

			checkUpdatePermissions();
			
			plugin_interface.getDownloadManager().startAllDownloads();
		
		}else if ( method.equals( "torrent-stop-all" )){

			checkUpdatePermissions();
			
			plugin_interface.getDownloadManager().stopAllDownloads();
			 
		}else if ( method.equals( "torrent-start" )){

			method_Torrent_Start(args, result);

		}else if ( method.equals( "torrent-start-now" )){
			// RPC v14

			method_Torrent_Start_Now(args, result);

		}else if ( method.equals( "torrent-stop" )){

			method_Torrent_Stop(args, result);

		}else if ( method.equals( "torrent-verify" )){

			method_Torrent_Verify(args, result);

		}else if ( method.equals( "torrent-remove" )){
			// RPC v3

			method_Torrent_Remove(args, result);

		}else if ( method.equals( "torrent-set" )){
			
			method_Torrent_Set(args, result);
			
		}else if ( method.equals( "torrent-get" )){

			method_Torrent_Get(args, result);

		}else if ( method.equals( "torrent-reannounce" )){
			// RPC v5

			method_Torrent_Reannounce(args, result);

		}else{
			
			System.out.println( "unhandled methd: " + method + " - " + args );
		}

		return( result );
	}
	
	private void 
	method_Session_Stats(
			Map args, 
			Map result) 
	{
		
		// < RPC v4
		result.put("activeTorrentCount", 0); //TODO
		result.put("downloadSpeed", 0); //TODO
		result.put("pausedTorrentCount", 0); //TODO
		result.put("torrentCount", 0); //TODO
		result.put("uploadSpeed", 0); //TODO
		
		// RPC v4
  	Map	current_stats = new HashMap();
  	
  	result.put( "current-stats", current_stats );
  	
  	current_stats.put( "uploadedBytes", 0 );
  	current_stats.put( "downloadedBytes", 0 );
  	current_stats.put( "ratio", 0 );
  	current_stats.put( "secondsActive", 0 );
  	
		// RPC v4
  	Map	cumulative_stats = new HashMap();
  	
  	result.put( "cumulative-stats", cumulative_stats );
  
  	cumulative_stats.put( "uploadedBytes", 0 );
  	cumulative_stats.put( "downloadedBytes", 0 );
  	cumulative_stats.put( "ratio", 0 );
  	cumulative_stats.put( "secondsActive", 0 );
  	cumulative_stats.put( "sessionCount", 0 );
	}

	private void 
	method_Torrent_Set(
			Map args, 
			Map result) 
	{
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
		
		// RPC v5
		// Not used: Number bandwidthPriority = getNumber("bandwidthPriority", null);

		Number speed_limit_down = getNumber(
				args.get("downloadLimit"),
				getNumber(args.get("speed-limit-down"),
						getNumber(args.get("speedLimitDownload"))));
		Boolean downloadLimited = getBoolean("downloadLimited", null);

		List files_wanted 		= (List)args.get( "files-wanted" );
		List files_unwanted 	= (List)args.get( "files-unwanted" );

		// RPC v5
		/** true if session upload limits are honored */
		// Not Used: Boolean honorsSessionLimits = getBoolean("honorsSessionLimits", null);

		
		// "location"            | string     new location of the torrent's content
		String location = (String) args.get("location");

		// Not Implemented: By default, Vuze automatically adjusts mac connections per torrent based on bandwidth and seeding state
		// "peer-limit"          | number     maximum number of peers
		
		List priority_high		= (List)args.get( "priority-high" );
		List priority_low		= (List)args.get( "priority-low" );
		List priority_normal	= (List)args.get( "priority-normal" );

		// RPC v14
		// "queuePosition"       | number     position of this torrent in its queue [0...n)
		Number queuePosition = getNumber("queuePosition", null);

		// RPC v10
		// "seedIdleLimit"       | number     torrent-level number of minutes of seeding inactivity

		// RPC v10: Not used, always TransmissionVars.TR_IDLELIMIT_GLOBAL
		// "seedIdleMode"        | number     which seeding inactivity to use.  See tr_inactvelimit (OR tr_idlelimit and TR_IDLELIMIT_*)

		// RPC v5: Not Supported
		// "seedRatioLimit"      | double     torrent-level seeding ratio

		// RPC v5: Not Supported
		// "seedRatioMode"       | number     which ratio to use.  See tr_ratiolimit

		// RPC v10
		// "trackerAdd"          | array      strings of announce URLs to add
		List trackerAddList = (List) args.get("trackerAdd");

		// RPC v10: TODO
		// "trackerRemove"       | array      ids of trackers to remove
		// List trackerRemoveList = (List) args.get("trackerRemove");

		// RPC v10: TODO
		// "trackerReplace"      | array      pairs of <trackerId/new announce URLs>

		// "uploadLimit"         | number     maximum upload speed (KBps)
		Number speed_limit_up = getNumber(
				args.get("uploadLimit"),
				getNumber(args.get("speed-limit-up"),
						getNumber(args.get("speedLimitUpload"))));

		// "uploadLimited"       | boolean    true if "uploadLimit" is honored
		Boolean uploadLimited = getBoolean("uploadLimited", null);

		Long	l_uploaded_ever		= (Long)args.get( "uploadedEver" );
		Long	l_downloaded_ever 	= (Long)args.get( "downloadedEver" );
		
		long	uploaded_ever 	= l_uploaded_ever==null?-1:l_uploaded_ever.longValue();
		long	downloaded_ever = l_downloaded_ever==null?-1:l_downloaded_ever.longValue();
		

		for ( Download download: downloads ){
			
			Torrent t = download.getTorrent();
			
			if ( t == null ){
				
				continue;
			}

			DownloadManager	core_download = PluginCoreUtils.unwrap( download );
			
			if (location != null) {
				File file = new File(location);
				if (!file.isFile()) {
					try {
						download.moveDataFiles(file);
					} catch (DownloadException e) {
						Debug.out(e);
					}
				}
			}
			
			if (queuePosition != null) {
				download.moveTo(queuePosition.intValue());
			}
			
			if (trackerAddList != null) {
				for (Object oTracker : trackerAddList) {
					if (oTracker instanceof String) {
						String aTracker = (String) oTracker;
						TorrentUtils.announceGroupsInsertFirst(PluginCoreUtils.unwrap(t), aTracker);
					}
				}
			}
			
			
			if ( speed_limit_down != null && Boolean.TRUE.equals(downloadLimited) ){
				
				download.setDownloadRateLimitBytesPerSecond( speed_limit_down.intValue());
			} else if (Boolean.FALSE.equals(downloadLimited)) {

				download.setDownloadRateLimitBytesPerSecond(0);
			}
			
			if ( speed_limit_up != null && Boolean.TRUE.equals(uploadLimited) ){
				
				download.setUploadRateLimitBytesPerSecond( speed_limit_up.intValue());
			} else if (Boolean.FALSE.equals(uploadLimited)) {

				download.setUploadRateLimitBytesPerSecond(0);
			}			
			
								
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
	}

	private void
	method_Torrent_Reannounce(
			Map args, 
			Map result)
	throws IOException
	{
		checkUpdatePermissions();
		
		Object	ids = args.get( "ids" );

		List<Download>	downloads = getDownloads( ids );

		for ( Download download: downloads ){
			
			try{
				download.requestTrackerAnnounce();

			}catch( Throwable e ){
				
				Debug.out( "Failed to reannounce '" + download.getName() + "'", e );
			}
		}
	}


	private void 
	method_Torrent_Remove(
			Map args, 
			Map result) 
	throws IOException 
	{
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
				
					long id = getID( download, false );
					
					if ( id > 0 ){
					
						recently_removed.add( id );
					}
				}
			}catch( Throwable e ){
				
				Debug.out( "Failed to remove download '" + download.getName() + "'", e );
			}
		}
	}

	private void 
	method_Torrent_Verify(
			Map args, 
			Map result)
	throws IOException 
	{
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
	}

	private void 
	method_Torrent_Stop(
			Map args, 
			Map result)
	throws IOException 
	{
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
	}

	private void 
	method_Torrent_Start(
			Map args, 
			Map result) 
	throws IOException 
	{
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
	}

	private void 
	method_Torrent_Start_Now(
			Map args, 
			Map result) 
	throws IOException 
	{
		checkUpdatePermissions();
		
		Object	ids = args.get( "ids" );

		List<Download>	downloads = getDownloads( ids );

		for ( Download download: downloads ){
			
			try{
				download.startDownload(true);

			}catch( Throwable e ){
			}
		}
	}


	private void 
	method_Torrent_Add(
			Map args, 
			Map result) 
	throws IOException, DownloadException
	{
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
		} else if (!url.startsWith("http")) {
			url = UrlUtils.parseTextForURL(url, true, true);
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

		Map torrent_details = new HashMap();
		
		torrent_details.put( "id", new Long( getID( download, true )));
		torrent_details.put( "name", escapeXML( download.getName()));
		torrent_details.put( "hashString", ByteFormatter.encodeString( torrent.getHash()));
		
		result.put( "torrent-added", torrent_details );
	}

	private Map
	method_Torrent_Get(
			Map args,
			Map result)
	{
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
		
		List	torrents = new ArrayList();
		
		result.put( "torrents", torrents );
		
		for ( Download download: downloads ){
			
			Torrent t = download.getTorrent();
			
			if ( t == null ){
				
				continue;
			}
			
			DownloadManager	core_download = PluginCoreUtils.unwrap( download );
			
			PEPeerManager pm = core_download.getPeerManager();
			
			DownloadStats	stats = download.getStats();
			
			Map torrent = new HashMap();
			
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

// metadataPercentComplete     | double                      | tr_stat
// name                        | string                      | tr_info
// peer-limit                  | number                      | tr_torrent
// peers                       | array (see below)           | n/a
// peersConnected              | number                      | tr_stat
// peersFrom                   | object (see below)          | n/a
// peersGettingFromUs          | number                      | tr_stat
// peersSendingToUs            | number                      | tr_stat
// percentDone                 | double                      | tr_stat
// pieces                      | string (see below)          | tr_torrent
// pieceCount                  | number                      | tr_info
// pieceSize                   | number                      | tr_info
// priorities                  | array (see below)           | n/a
// queuePosition               | number                      | tr_stat
// rateDownload (B/s)          | number                      | tr_stat
// rateUpload (B/s)            | number                      | tr_stat
// recheckProgress             | double                      | tr_stat
// secondsDownloading          | number                      | tr_stat
// secondsSeeding              | number                      | tr_stat
// seedIdleLimit               | number                      | tr_torrent
// seedIdleMode                | number                      | tr_inactvelimit
// seedRatioLimit              | double                      | tr_torrent
// seedRatioMode               | number                      | tr_ratiolimit
// sizeWhenDone                | number                      | tr_stat
// startDate                   | number                      | tr_stat
// status                      | number                      | tr_stat
// trackers                    | array (see below)           | n/a
// trackerStats                | array (see below)           | n/a
// totalSize                   | number                      | tr_info
// torrentFile                 | string                      | tr_info
// uploadedEver                | number                      | tr_stat
// uploadLimit                 | number                      | tr_torrent
// uploadLimited               | boolean                     | tr_torrent
// uploadRatio                 | double                      | tr_stat
// wanted                      | array (see below)           | n/a
// webseeds                    | array (see below)           | n/a
// webseedsSendingToUs         | number                      | tr_stat

				Object	value = null;
				
				if ( field.equals( "activityDate" )){
					// RPC v0
					// activityDate                | number                      | tr_stat
					value = torrentGet_activityDate(core_download);

				}else if ( field.equals( "addedDate" )){
					// RPC v0
					// addedDate                   | number                      | tr_stat
					/** When the torrent was first added. */
					value = core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME)/1000;

				}else if ( field.equals( "announceURL" )){
					// Removed in RPC v7

					value = t.getAnnounceURL().toExternalForm();

				}else if ( field.equals( "bandwidthPriority" )){ 
					// RPC v5: Not Supported
					// bandwidthPriority           | number                      | tr_priority_t
					/** torrent's bandwidth priority. */
					value = TransmissionVars.TR_PRI_NORMAL;
					
				}else if ( field.equals( "comment" )){
					// RPC v0
					// comment                     | string                      | tr_info

					value = t.getComment();
					
				}else if ( field.equals( "corruptEver" )){
					// RPC v0 TODO: Do we want just hash fails?
					// corruptEver                 | number                      | tr_stat
					/** 
					 * Byte count of all the corrupt data you've ever downloaded for
					 * this torrent. If you're on a poisoned torrent, this number can
					 * grow very large. 
					 */
					value = stats.getDiscarded() + stats.getHashFails();

				}else if ( field.equals( "creator" )){	
					// RPC v0
					// creator                     | string                      | tr_info
					value = t.getCreatedBy();

				}else if ( field.equals( "dateCreated" )){	

					// RPC v0
					// dateCreated                 | number                      | tr_info
					value = t.getCreationDate();

				}else if ( field.equals( "desiredAvailable" )){
					// RPC v0 TODO: stats.getRemainingAvailable() ?
					// desiredAvailable            | number                      | tr_stat
					 /** 
					  * Byte count of all the piece data we want and don't have yet,
					  * but that a connected peer does have. [0...leftUntilDone] 
					  */
					value = stats.getRemaining();

				}else if ( field.equals( "doneDate" )){
					// RPC v0
					// doneDate                    | number                      | tr_stat
			    /** When the torrent finished downloading. */
					if (core_download.isDownloadComplete(false)) {
						value = core_download.getDownloadState().getLongParameter(
								DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
					} else {
						// TODO: Verify what value to send when not complete
						value = 0;
					}
					
				}else if ( field.equals( "downloadDir" )){
					// RPC v4
					// downloadDir                 | string                      | tr_torrent

					value = download.getSavePath();

				}else if ( field.equals( "downloadedEver" )){
					// RPC v0
					// downloadedEver              | number                      | tr_stat

					/** 
					 * Byte count of all the non-corrupt data you've ever downloaded
					 * for this torrent. If you deleted the files and downloaded a second
					 * time, this will be 2*totalSize.. 
					 */
					value = stats.getDownloaded();

				}else if ( field.equals( "downloadLimit" ) || field.equals("speed-limit-down")){
					// RPC v5 (alternate is from 'set' prior to v5 -- added for rogue clients)
					// downloadLimit               | number                      | tr_torrent
					
					/** maximum download speed (KBps) */
					value = download.getMaximumDownloadKBPerSecond();

				}else if ( field.equals( "downloadLimited" ) || field.equals("speed-limit-down-enabled")){
					// RPC v5 (alternate is from 'set' prior to v5 -- added for rogue clients)
					// downloadLimited             | boolean                     | tr_torrent

					/** true if "downloadLimit" is honored */
					value = download.getDownloadRateLimitBytesPerSecond() > 0;

				}else if ( field.equals( "error" )){
					// RPC v0
					// error                       | number                      | tr_stat
					/** Defines what kind of text is in errorString. TR_STAT_* */
					
					value = torrentGet_error(core_download, download);

				}else if ( field.equals( "errorString" )){
					// RPC v0
					// errorString                 | string                      | tr_stat

					value = torrentGet_errorString(core_download, download);

				}else if ( field.equals( "eta" )){
					// RPC v0
					// eta                         | number                      | tr_stat

					value = torrentGet_eta(download, stats);
					
				}else if ( field.equals( "files" )){
					// RPC v0

					value = torrentGet_files(core_download);
					
				}else if ( field.equals( "fileStats" )){
					// RPC v5
					
					value = torrentGet_fileStats(core_download);
					
				}else if ( field.equals( "hashString" )){
					// RPC v0
					// hashString                  | string                      | tr_info
					value = ByteFormatter.encodeString( t.getHash());

				}else if ( field.equals( "haveUnchecked" )){	
					// haveUnchecked               | number                      | tr_stat
			    /** Byte count of all the partial piece data we have for this torrent.
	        As pieces become complete, this value may decrease as portions of it
	        are moved to `corrupt' or `haveValid'. */
					// TODO: set when ST_CHECKING?
					value = 0;

				}else if ( field.equals( "haveValid" )){
					// haveValid                   | number                      | tr_stat
			    /** Byte count of all the checksum-verified data we have for this torrent.
			      */
					value = stats.getDownloaded();

				}else if ( field.equals( "honorsSessionLimits" )){
					// TODO RPC v5
					// honorsSessionLimits         | boolean                     | tr_torrent
					/** true if session upload limits are honored */
					value = false;

				}else if ( field.equals( "id" )){
					// id                          | number                      | tr_torrent
					value = getID( download, true );

				}else if ( field.equals( "isFinished" )){
					// RPC v9: TODO
					// isFinished                  | boolean                     | tr_stat
			    /** A torrent is considered finished if it has met its seed ratio.
	        As a result, only paused torrents can be finished. */

					value = false;
					
				}else if ( field.equals( "isPrivate" )){
					// RPC v0
					// isPrivate                   | boolean                     | tr_torrent
					value = t.isPrivate();

				}else if ( field.equals( "isStalled" )){
					// RPC v14
					// isStalled                   | boolean                     | tr_stat

					value = torrentGet_isStalled(download);
					
				}else if ( field.equals( "leftUntilDone" )){	
					// RPC v0
					// leftUntilDone               | number                      | tr_stat

					/** Byte count of how much data is left to be downloaded until we've got
	        all the pieces that we want. [0...tr_info.sizeWhenDone] */

					value = stats.getRemaining();

				}else if ( field.equals( "magnetLink" )){ 
					// TODO RPC v7
					// magnetLink                  | number                      | n/a
					// NOTE: I assume spec is wrong and it's a string..
					
					value = UrlUtils.getMagnetURI(download.getName(), t);
					
				}else if ( field.equals( "manualAnnounceTime" )){ 
					// manualAnnounceTime          | number                      | tr_stat
					// spec is time_t, although it should be relative time. :(
					
					value = torrentGet_manualAnnounceTime(core_download);
					
				}else if ( field.equals( "maxConnectedPeers" )){ 
					// maxConnectedPeers           | number                      | tr_torrent
					// TODO: Some sort of Peer Limit (tr_torrentSetPeerLimit )
					

				}else if ( field.equals( "trackerSeeds" )){
					DownloadScrapeResult scrape = download.getLastScrapeResult();
					value = new Long( scrape==null?0:scrape.getSeedCount());
				}else if ( field.equals( "trackerLeechers" )){
					DownloadScrapeResult scrape = download.getLastScrapeResult();
					value = new Long( scrape==null?0:scrape.getNonSeedCount());
				}else if ( field.equals( "leechers" )){
					// Removed in RPC v7
					if ( pm == null ){
						value = new Long(0);
					}else{
						value = new Long( pm.getNbPeers());
					}
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
				}else if ( field.equals( "rateDownload" )){	
					value = new Long( stats.getDownloadAverage());
				}else if ( field.equals( "rateUpload" )){
					value = new Long( stats.getUploadAverage());
				}else if ( field.equals( "speedLimitDownload" )){	
					value = new Long( download.getDownloadRateLimitBytesPerSecond());
				}else if ( field.equals( "speedLimitUpload" )){
					value = new Long( download.getUploadRateLimitBytesPerSecond());
				}else if ( field.equals( "seeders" )){
					// Removed in RPC v7
					if ( pm == null ){
						value = new Long(-1);
					}else{
						value = new Long( pm.getNbSeeds());
					}
				}else if ( field.equals( "sizeWhenDone" )){	
					value = new Long( t.getSize());	// TODO: excluded DND
				}else if ( field.equals( "status" )){
					
					value = torrentGet_status(download);

				}else if ( field.equals( "swarmSpeed" )){	
					// Removed in RPC v7
					value = new Long( core_download.getStats().getTotalAveragePerPeer());
				}else if ( field.equals( "totalSize" )){
					value = new Long( t.getSize());
				}else if ( field.equals( "pieceCount" )){
					value = new Long( t.getPieceCount());
				}else if ( field.equals( "pieceSize" )){
					value = new Long( t.getPieceSize());
				}else if ( field.equals( "metadataPercentComplete" )){
					// RPC v7
					value = 1.0f;
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
					// RPC v5

					value = new Double( COConfigurationManager.getFloatParameter( "Stop Ratio" ));

				}else if ( field.equals( "seedRatioMode" )){
					// 0=global,1=local,2=unlimited
					value = new Long(1);

				}else if ( field.equals( "percentDone" )){			// 2.71
          /** 
           * How much has been downloaded of the files the user wants. This differs
           * from percentComplete if the user wants only some of the torrent's files.
           * Range is [0..1]
           */
					// TODO
  				value = 0;

				}else if ( field.equals( "webseedsSendingToUs" )){
					// TODO: RPC v0
					/** Number of webseeds that are sending data to us. */
					value = 0;
				}else if ( field.equals( "startDate" )){
					/** When the torrent was last started. */
					value = 0;
				}else if ( field.equals( "trackerStats" )){
					// RPC v7
					
					value = torrentGet_trackerStats(core_download);
					
				}else if ( field.equals( "peers" )){				// 2.71
					// RPC v2

					List	peers = getPeerList(core_download);
					
					value = peers;
					
				}else if ( field.equals( "queuePosition" )){		// 2.71
					
					value = core_download.getPosition();
					
				}else if ( field.equals( "trackers" )){				// 2.71
					
					List	trackers = new ArrayList();
					
					value = trackers;

				}else if ( field.equals( "announceResponse" )){
					// Removed in RPC v7
					
					TRTrackerAnnouncer trackerClient = core_download.getTrackerClient();
					if (trackerClient != null) {
						value = trackerClient.getStatusString();
					} else {
						value = "";
					}

				}else if ( field.equals( "peersKnown" )){ // TODO, Removed in v13
					
					value = 0;

				}else if ( field.equals( "lastScrapeTime" )){
					// Unsure of wanted format
					// Removed in v7
					
					value = core_download.getTrackerTime();  

				}else if ( field.equals( "scrapeURL" )){
					// Removed in v7
					value = "";
					TRTrackerScraperResponse trackerScrapeResponse = core_download.getTrackerScrapeResponse();
					if (trackerScrapeResponse != null) {
						URL url = trackerScrapeResponse.getURL();
						if (url != null) {
							value = url.toString();
						}
					}

				}else if ( field.equals( "nextScrapeTime" )){
					// Removed in v7
					
					// Unsure of wanted format
					TRTrackerAnnouncer trackerClient = core_download.getTrackerClient();
					if (trackerClient != null) {
						value = trackerClient.getTimeUntilNextUpdate();
					} else {
						value = 0;
					}
					
				}else if ( field.equals( "nextAnnounceTime" )){
					// Removed in v7

					// Unsure of wanted format
					TRTrackerAnnouncer trackerClient = core_download.getTrackerClient();
					if (trackerClient != null) {
						value = trackerClient.getTimeUntilNextUpdate();
					} else {
						value = 0;
					}

				}else if ( field.equals( "webseeds" )){
					// TODO
					value = 0;

				}else if ( field.equals( "priorities" )){
					List list = new ArrayList();
					value = list;
					
					DiskManagerFileInfoSet files = core_download.getDiskManagerFileInfoSet();
					
					DiskManagerFileInfo[] fileInfos = files.getFiles();
					for (DiskManagerFileInfo fileInfo : fileInfos) {
						int priority = fileInfo.getPriority();
						long newPriority = TransmissionVars.convertVuzePriority(priority);
						list.add(newPriority);
					}

				}else if ( field.equals( "uploadLimited") || field.equals("speed-limit-up-enabled")){
					// RPC v5 (alternate is from 'set' prior to v5 -- added for rogue clients)
					// TODO
					value = false;

				}else if ( field.equals( "uploadLimit" ) || field.equals("speed-limit-up")){
					// RPC v5 (alternate is from 'set' prior to v5 -- added for rogue clients)
					// TODO
					value = -1;

				}else if ( field.equals( "peer-limit" )){
					// TODO
					value = -1;

				} else if (field.equals("downloadLimitMode") || field.equals("uploadLimitMode")) {
					// RPC < v5 -- Not supported -- ignore
/*
We Support? | RPC v | Trns Ver| BW Compat | method         | comment
					Y | 7     | 1.80    |        NO | torrent-get    | removed arg "announceResponse"
					Y         |         |        NO | torrent-get    | removed arg "announceURL"
					N         |         |        NO | torrent-get    | removed arg "downloaders"
					N         |         |        NO | torrent-get    | removed arg "lastAnnounceTime"
					N         |         |        NO | torrent-get    | removed arg "lastScrapeTime"
					Y         |         |        NO | torrent-get    | removed arg "leechers"
         	Y         |         |        NO | torrent-get    | removed arg "nextAnnounceTime"
         	Y         |         |        NO | torrent-get    | removed arg "nextScrapeTime"
         	N         |         |        NO | torrent-get    | removed arg "scrapeResponse"
          Y         |         |        NO | torrent-get    | removed arg "scrapeURL"
         	Y					|         |        NO | torrent-get    | removed arg "seeders"
         	N         |         |        NO | torrent-get    | removed arg "timesCompleted"
         	Y         |         |        NO | torrent-get    | removed arg "swarmSpeed"
 */
					
				}else{
					System.out.println( "Unhandled get-torrent field: " + field );
				}
				
				if ( value != null ){
					
					if ( value instanceof String ){
						
						value = escapeXML((String)value);
					}
					torrent.put( field, value );
				}
			} // for fields
		} // for downloads
		
		return result;
	}

  /** 
   * time when one or more of the torrent's trackers will
   * allow you to manually ask for more peers,
   * or 0 if you can't 
   */
	private Object torrentGet_manualAnnounceTime(DownloadManager manager) {
		// See ScrapeInfoView's updateButton logic
		Object value;
		TRTrackerAnnouncer trackerClient = manager.getTrackerClient();
		if (trackerClient != null) {

			value = Math.max(SystemTime.getCurrentTime() / 1000,
					trackerClient.getLastUpdateTime() + TRTrackerAnnouncer.REFRESH_MINIMUM_SECS);
					
		} else {
			// Technically the spec says "ask for more peers" which suggests
			// we don't need to handle scrape -- but let's do it anyway

			TRTrackerScraperResponse sr = manager.getTrackerScrapeResponse();
			
			if ( sr == null ){
				
				value = 0;
				
			}else{
				
				value = Math.max(SystemTime.getCurrentTime() / 1000,
						sr.getScrapeStartTime() / 1000 + TRTrackerScraper.REFRESH_MINIMUM_SECS);
			}
		}

		return value;
	}

	/** 
	 * If downloading, estimated number of seconds left until the torrent is done.
	 * If seeding, estimated number of seconds left until seed ratio is reached. 
	 */
	private Object torrentGet_eta(Download download, DownloadStats stats) {
		Object value;

		int state = download.getState();
		if (state == Download.ST_DOWNLOADING) {
			long eta_secs = stats.getETASecs();
			
			if (eta_secs == -1) {
				value = TransmissionVars.TR_ETA_NOT_AVAIL;
			} else if (eta_secs >= 315360000000L) {
				value = TransmissionVars.TR_ETA_UNKNOWN;
			} else {
				value = eta_secs;
			}
		} else if (state == Download.ST_SEEDING) {
			// TODO: secs left until SR met
			value = TransmissionVars.TR_ETA_NOT_AVAIL;
		} else {
			value = TransmissionVars.TR_ETA_NOT_AVAIL;
		}
		
		return value;
	}

	private Object torrentGet_trackerStats(DownloadManager core_download) {
		List	tracker_stats = new ArrayList();

		List<TrackerPeerSource> trackerPeerSources = core_download.getTrackerPeerSources();
		
		if (trackerPeerSources != null) {
			for (TrackerPeerSource tps : trackerPeerSources) {
		    String statusString = tps.getStatusString();
		    if (statusString == null) {
		    	statusString = "";
		    }

		    Map<String, Object> map = new HashMap<String, Object>();
				
		    /* how many downloads this tracker knows of (-1 means it does not know) */
		    map.put("downloadCount", -1); // TODO

		    /* whether or not we've ever sent this tracker an announcement */
		    map.put("hasAnnounced", tps.getPeers() >= 0); // TODO

		    /* whether or not we've ever scraped to this tracker */
		    map.put("hasScraped", false); // todo: bool);

		    /* human-readable string identifying the tracker */
		    map.put("host", tps.getName()); // TODO

		    /* the full announce URL */
		    map.put("announce", tps.getName()); // TODO

		    /* the full scrape URL */
		    map.put("scrape", tps.getName()); // TODO

		    /* Transmission uses one tracker per tier,
		     * and the others are kept as backups */
		    map.put("isBackup", false); // TODO

		    /* is the tracker announcing, waiting, queued, etc */
		    int status = tps.getStatus();
		    int state;
		    if (status == tps.ST_AVAILABLE || status == tps.ST_ONLINE) {
		    	state = TransmissionVars.TR_TRACKER_WAITING;
		    } else if (status == tps.ST_UPDATING) {
		    	state = TransmissionVars.TR_TRACKER_ACTIVE; 
		    } else if (status == tps.ST_QUEUED) {
		    	state = TransmissionVars.TR_TRACKER_QUEUED; 
		    } else {
		    	state = TransmissionVars.TR_TRACKER_INACTIVE;
		    }
		    map.put("announceState", state);

		    /* is the tracker scraping, waiting, queued, etc */
		    map.put("scrapeState", state);

		    /* number of peers the tracker told us about last time.
		     * if "lastAnnounceSucceeded" is false, this field is undefined */
		    map.put("lastAnnouncePeerCount", tps.getPeers());

		    /* human-readable string with the result of the last announce.
		       if "hasAnnounced" is false, this field is undefined */
		    if (statusString != null) {
		    	map.put("lastAnnounceResult", statusString);
		    }

		    /* when the last announce was sent to the tracker.
		     * if "hasAnnounced" is false, this field is undefined */
		    map.put("lastAnnounceStartTime", 0); // TODO: time_t);

		    /* whether or not the last announce was a success.
		       if "hasAnnounced" is false, this field is undefined */
		    map.put("lastAnnounceSucceeded", tps.getPeers() >= 0);

		    /* whether or not the last announce timed out. */
		    map.put("lastAnnounceTimedOut", false); // TODO

		    /* when the last announce was completed.
		       if "hasAnnounced" is false, this field is undefined */
		    map.put("lastAnnounceTime", 0); // TODO: time_t);

		    /* human-readable string with the result of the last scrape.
		     * if "hasScraped" is false, this field is undefined */
		    if (statusString != null) {
		    	map.put("lastScrapeResult", statusString);
		    }

		    /* when the last scrape was sent to the tracker.
		     * if "hasScraped" is false, this field is undefined */
		    map.put("lastScrapeStartTime", 0); // TODO: time_t);

		    /* whether or not the last scrape was a success.
		       if "hasAnnounced" is false, this field is undefined */
		    map.put("lastScrapeSucceeded", tps.getPeers() >= 0);

		    /* whether or not the last scrape timed out. */
		    map.put("lastScrapeTimedOut", false); // TODO: bool);

		    /* when the last scrape was completed.
		       if "hasScraped" is false, this field is undefined */
		    map.put("lastScrapeTime", 0); // TODO: time_t);

		    /* number of leechers this tracker knows of (-1 means it does not know) */
		    map.put("leecherCount", tps.getLeecherCount());

		    /* when the next periodic announce message will be sent out.
		       if announceState isn't TR_TRACKER_WAITING, this field is undefined */
		    map.put("nextAnnounceTime", 0); // TODO: time_t);

		    /* when the next periodic scrape message will be sent out.
		       if scrapeState isn't TR_TRACKER_WAITING, this field is undefined */
		    map.put("nextScrapeTime", 0); // TODO: time_t);

		    /* number of seeders this tracker knows of (-1 means it does not know) */
		    map.put("seederCount", tps.getSeedCount());

		    /* which tier this tracker is in */
		    map.put("tier", 0); // TODO: int);

		    /* used to match to a tr_tracker_info */
		    map.put("id", tps.hashCode());
		    
		    tracker_stats.add(map);
			}
		}

		return tracker_stats;
	}

	private Object torrentGet_status(Download download) {
		// 1 - waiting to verify
		// 2 - verifying
		// 4 - downloading
		// 5 - queued (incomplete)
		// 8 - seeding
		// 9 - queued (complete)
		// 16 - paused
	
		// 2.71 - these changed!

	    //TR_STATUS_STOPPED        = 0, /* Torrent is stopped */
	    //TR_STATUS_CHECK_WAIT     = 1, /* Queued to check files /*
	    //TR_STATUS_CHECK          = 2, /* Checking files */
	    //TR_STATUS_DOWNLOAD_WAIT  = 3, /* Queued to download */
	    //TR_STATUS_DOWNLOAD       = 4, /* Downloading */
	    //TR_STATUS_SEED_WAIT      = 5, /* Queued to seed */
	    //TR_STATUS_SEED           = 6  /* Seeding */
	   
  	final int CHECK_WAIT;
  	final int CHECKING;
  	final int DOWNLOADING;
  	final int QUEUED_INCOMPLETE;
  	final int QUEUED_COMPLETE;
  	final int STOPPED;
  	final int SEEDING;
  	final int ERROR;

  	boolean	RPC_14_OR_HIGHER = true; // fields.contains( "percentDone" );
  	
  	if ( RPC_14_OR_HIGHER ){
  		
  		CHECK_WAIT			= 1;
  		CHECKING			= 2;
  		DOWNLOADING			= 4;
  		QUEUED_INCOMPLETE	= 3;
  		QUEUED_COMPLETE		= 5;
  		STOPPED				= 0;
  		SEEDING				= 6;
  		ERROR				= STOPPED;
  	}else{
  		CHECK_WAIT			= 1;
  		CHECKING			= 2;
  		DOWNLOADING			= 4;
  		QUEUED_INCOMPLETE	= 5;
  		QUEUED_COMPLETE		= 9;
  		STOPPED				= 16;
  		SEEDING				= 8;
  		ERROR				= 0;
  	}
  	
  	int	status_int;
  	
  	if ( download.isPaused()){
  		
  		status_int = STOPPED;
  									
  	}else{
  		int state = download.getState();
  		
  		if ( state == Download.ST_DOWNLOADING ){
  			
  			status_int = DOWNLOADING;
  			
  		}else if ( state == Download.ST_SEEDING ){
  			
  			status_int = SEEDING;
  			
  		}else if ( state == Download.ST_QUEUED ){
  
  			if ( download.isComplete()){
  				
  				status_int = QUEUED_COMPLETE;
  				
  			}else{
  				
  				status_int = QUEUED_INCOMPLETE;
  			}
  		}else if ( state == Download.ST_STOPPED || state == Download.ST_STOPPING ){
  			
  			status_int = STOPPED;
  			
  		}else if ( state == Download.ST_ERROR ){
  			
  			status_int = ERROR;
  			
  		}else{
  			
  			DownloadManager	core_download = PluginCoreUtils.unwrap( download );
  			if ( core_download.getState() == DownloadManager.STATE_CHECKING ){
  			
  				status_int = CHECKING;
  				
  			}else{
  				
  				status_int = CHECK_WAIT;
  			}
  		}
  	}
  	
  	return status_int;
	}

	private Object torrentGet_fileStats(DownloadManager core_download) {
		// | a file's non-constant properties.    |
    // | array of tr_info.filecount objects,  |
    // | each containing:                     |
    // +-------------------------+------------+
    // | bytesCompleted          | number     | tr_torrent
    // | wanted                  | boolean    | tr_info
    // | priority                | number     | tr_info
		List<Map> stats_list = new ArrayList<Map>();
		
		DiskManagerFileInfo[] files = core_download.getDiskManagerFileInfoSet().getFiles();
		
		for ( DiskManagerFileInfo file: files ){
			
			Map obj = new HashMap();
			
			stats_list.add( obj );
			
			obj.put( "bytesCompleted", file.getDownloaded());
			obj.put( "wanted", !file.isSkipped());
			obj.put( "priority", TransmissionVars.convertVuzePriority(file.getPriority()));
		}
		
		return stats_list;
	}

	private Object torrentGet_files(DownloadManager core_download) {
		// | array of objects, each containing:   |
    // +-------------------------+------------+
    // | bytesCompleted          | number     | tr_torrent
    // | length                  | number     | tr_info
    // | name                    | string     | tr_info

		List<Map> file_list = new ArrayList<Map>();
		
		DiskManagerFileInfo[] files = core_download.getDiskManagerFileInfoSet().getFiles();
		
		for ( DiskManagerFileInfo file: files ){
			
			Map obj = new HashMap();
			
			file_list.add( obj );
			
			obj.put( "bytesCompleted", file.getDownloaded());	// this must be a spec error...
			obj.put( "length",  file.getLength());
			obj.put( "name", file.getTorrentFile().getRelativePath());
		}
		
		return file_list;
	}

	private Object torrentGet_errorString(DownloadManager core_download,
			Download download) {
		Object value;

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
		return value;
	}

	/** Defines what kind of text is in errorString. TR_STAT_* */
	private Object torrentGet_error(DownloadManager core_download, Download download) {
		Object value;
		/** Defines what kind of text is in errorString. */
		String str = download.getErrorStateDetails();
		
		if ( str != null && str.length() > 0 ){
			value = TransmissionVars.TR_STAT_LOCAL_ERROR;
		}else{
			value = 0;
			TRTrackerAnnouncer tracker_client = core_download.getTrackerClient();
			
			if ( tracker_client != null ){
				TRTrackerAnnouncerResponse x = tracker_client.getBestAnnouncer().getLastResponse();
				if ( x != null ){
					if ( x.getStatus() == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR ){
						value = TransmissionVars.TR_STAT_TRACKER_ERROR;
					}
				}
			}else{
				DownloadScrapeResult x = download.getLastScrapeResult();
				if ( x != null ){
					if ( x.getResponseType() == DownloadScrapeResult.RT_ERROR ){
						String status = x.getStatus();
						
						if ( status != null && status.length() > 0 ){
						
							value = TransmissionVars.TR_STAT_TRACKER_ERROR;
						}
					}
				}
			}
		}
		return value;
	}

	/** 
	 * The last time we uploaded or downloaded piece data on this torrent. 
	 */
	private Object torrentGet_activityDate(DownloadManager download) {
		int state = download.getState();
		if (state == Download.ST_SEEDING || state == Download.ST_DOWNLOADING) {
			return (SystemTime.getCurrentTime() / 1000)
					- Math.min(
							download.getStats().getTimeSinceLastDataReceivedInSeconds(),
							download.getStats().getTimeSinceLastDataSentInSeconds());
		}
		
		return 0;
	}

	/** 
	 * True if the torrent is running, but has been idle for long enough
	 * to be considered stalled.
	 */
	private Object torrentGet_isStalled(Download download) {
		Object value = false;
		int state = download.getState();
		if (state == Download.ST_SEEDING || state == Download.ST_DOWNLOADING) {
    	DefaultRankCalculator calc = StartStopRulesDefaultPlugin.getRankCalculator(download);
    	if (calc != null) {
				value = (state == Download.ST_SEEDING && !calc.getActivelySeeding())
						|| (state == Download.ST_DOWNLOADING && !calc.getActivelyDownloading());
    	}
		}
		return value;
	}

	private List getPeerList(DownloadManager core_download) {
		List peers = new ArrayList();
		
		if (core_download == null) {
			return peers;
		}
		
		PEPeerManager pm = core_download.getPeerManager();
		if (pm == null) {
			return peers;
		}
		List<PEPeer> peerList = pm.getPeers();
		for (PEPeer peer : peerList) {
			Map map = new HashMap();
			peers.add(map);
			
			
			map.put("address", peer.getIp());
			map.put("clientName", peer.getClient());
			map.put("clientIsChoked", peer.isChokedByMe());
			map.put("clientIsInterested", peer.isInterested());
			map.put("isDownloadingFrom", peer.isDownloadPossible() && peer.getStats().getDataReceiveRate() > 0);
			// peer.connection.getTransport().isEncrypted
			map.put("isEncrypted", !"None".equals(peer.getEncryption()));  // TODO FIX
			map.put("isIncoming", peer.isIncoming());
			map.put("isUploadingTo", peer.getStats().getDataSendRate() > 0);
			// RPC v13
			map.put("isUTP", peer.getProtocol().equals("uTP"));
			map.put("peerIsChoked", peer.isChokingMe());
			map.put("peerIsInterested", peer.isInteresting());
			// RPC v3
			map.put("port", peer.getPort());
			map.put("progress", peer.getPercentDoneInThousandNotation() / 1000.0);
			map.put("rateToClient", peer.getStats().getDataReceiveRate());
			map.put("rateToPeer", peer.getStats().getDataSendRate());
		}
		
		return peers;
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
				
				long	id = getID( download, true );
				
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
		
		Collections.sort(
			downloads,
			new Comparator<Download>()
			{
				public int 
				compare(
					Download arg0, 
					Download arg1 )
				{
					long res = getID( arg0, true ) - getID( arg1, true );
					
					if ( res < 0 ){
						return( -1 );
					}else if ( res > 0 ){
						return( 1 );
					}else{
						return( 0 );
					}
				}		
			});
		
		return( downloads );
	}

	protected boolean
	getBoolean(
		Object	o )
	{
		return getBoolean(o, false);
	}

	protected Boolean
	getBoolean(
		Object	o,
		Boolean defaultVal )
	{
		if ( o instanceof Boolean ){
			
			return((Boolean)o);
						
		}else if ( o instanceof String ){
			
			return( ((String)o).equalsIgnoreCase( "true" ));
			
		}else if ( o instanceof Number ){
			
			return(((Number)o).intValue()!=0);
			
		}else{
			
			return( defaultVal );
		}
	}
	
	protected long
	getID(
		Download		download,
		boolean			allocate_if_new )
	{
		synchronized( this ){
			
			if ( check_ids_outstanding ){
				
				check_ids_outstanding = false;
				
				Download[] all_downloads = plugin_interface.getDownloadManager().getDownloads();

				Set<Long>	all_ids = new HashSet<Long>();
				
				List<Download>	dups = new ArrayList<Download>();
				
				long	max_id = 0;
				
				for( Download d: all_downloads ){
					
					long	id = getID( d, false );
					
					if ( id <= 0 ){
						
						continue;
					}
					
					max_id = Math.max( max_id, id );
					
					if ( all_ids.contains( id )){
					
						dups.add( d );
						
					}else{
						
						all_ids.add( id );
					}
				}
				
				PluginConfig config = plugin_interface.getPluginconfig();
					
				long	next_id = max_id + 1;
				
				for ( Download d: dups ){
					
					//System.out.println( "Fixed duplicate id " + getID( d, false ) + " for " + d.getName());
					
					d.setLongAttribute( t_id, next_id++ );
				}
				
				config.setPluginParameter( "xmui.next.id", next_id );

			}
		}
			
			// I was trying to be clever and allocate unique ids for downloads. however,
			// the webui assumes they are consecutive and give a queue index. ho hum
			
		// return( d.getIndex());
		
		long id = download.getLongAttribute( t_id );
			
		if ( id == 0 && allocate_if_new ){
		
			synchronized( this ){
				
				PluginConfig config = plugin_interface.getPluginconfig();
			
				id = config.getPluginLongParameter( "xmui.next.id", 1 );
				
				config.setPluginParameter( "xmui.next.id", id + 1 );
			}
			
			download.setLongAttribute( t_id, id );
		}
		
		//System.out.println( download.getName() + " -> " + id );
		
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
	
	private Number getTrackerID(TrackerPeerSource source) {
		return Long.valueOf((source.getName().hashCode() << 4l) + source.getType());
	}

}
