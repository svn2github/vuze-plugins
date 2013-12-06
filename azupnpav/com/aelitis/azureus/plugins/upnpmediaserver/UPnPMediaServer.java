
/*
 * Created on 23-Mar-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.upnpmediaserver;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.web.TrackerAuthenticationAdapter;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.components.UITextField;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.InfoParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.PasswordParameter;
import org.gudy.azureus2.plugins.ui.config.StringListParameter;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.PowerManagementListener;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

import com.aelitis.azureus.core.content.AzureusContentDirectory;
import com.aelitis.azureus.core.content.AzureusContentDirectoryListener;
import com.aelitis.azureus.core.content.AzureusContentDirectoryManager;
import com.aelitis.azureus.core.content.AzureusContentDownload;
import com.aelitis.azureus.core.content.AzureusContentFile;
import com.aelitis.azureus.core.content.AzureusContentFilter;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.UUIDGenerator;
import com.aelitis.azureus.plugins.upnp.UPnPMapping;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.azureus.plugins.upnpmediaserver.ui.UPnPMediaServerUI;
import com.aelitis.net.upnp.UPnP;
import com.aelitis.net.upnp.UPnPAdapter;
import com.aelitis.net.upnp.UPnPFactory;
import com.aelitis.net.upnp.UPnPListener;
import com.aelitis.net.upnp.UPnPRootDevice;
import com.aelitis.net.upnp.UPnPSSDP;
import com.aelitis.net.upnp.UPnPSSDPListener;
import com.aelitis.azureus.plugins.upnpmediaserver.UPnPMediaServerContentDirectory.*;

public class 
UPnPMediaServer 
	implements UnloadablePlugin, AzureusContentDirectoryListener, PowerManagementListener
{
	private static final int		HTTP_PORT_DEFAULT				= 6901;
	
	private static final boolean 	DISABLE_MENUS_FOR_INCOMPLETE	= false;
	
	private static final int EVENT_TIMEOUT_SECONDS	= 30*60;
	
	private static final String	ORIGINAL_SOURCE_PROTOCOLS = "http-get:*:*:*";
	
	private static final String	DNLA_SOURCE_PROTOCOLS = 
		"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_LRG;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:image/png:DLNA.ORG_PN=PNG_LRG;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:image/gif:DLNA.ORG_PN=GIF_LRG;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=32000;channels=2:DLNA.ORG_PN=LPCM_low;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=24000;channels=2:DLNA.ORG_PN=LPCM_low;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=16000;channels=1:DLNA.ORG_PN=LPCM_low;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=12000;channels=2:DLNA.ORG_PN=LPCM_low;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=11025;channels=1:DLNA.ORG_PN=LPCM_low;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=8000;channels=1:DLNA.ORG_PN=LPCM_low;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_PRO;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_FULL;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVHIGH_PRO;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVHIGH_FULL;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/3gpp:DLNA.ORG_PN=MPEG4_H263_MP4_P0_L10_AAC;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_NA;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_EU;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_HD_NA_ISO;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_NA;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_PAL;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_BASE;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:video/x-matroska:*,http-get:*:video/x-mkv:*,http-get:*:audio/x-matroska:*,"+
		"http-get:*:video/avi:*,"+
		"http-get:*:image/png:DLNA.ORG_PN=PNG_TN;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:image/png:DLNA.ORG_PN=PNG_LRG;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=00f00000000000000000000000000000,"+
		"http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMAPRO;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMAFULL;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/mpeg:DLNA.ORG_PN=MP3;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/mp4:DLNA.ORG_PN=HEAAC_L2_ISO;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=48000;channels=2:DLNA.ORG_PN=LPCM;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=48000;channels=1:DLNA.ORG_PN=LPCM;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=44100;channels=2:DLNA.ORG_PN=LPCM;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/L16;rate=44100;channels=1:DLNA.ORG_PN=LPCM;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMABASE;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=01700000000000000000000000000000,"+
		"http-get:*:*:*";

	private static final String	CM_SOURCE_PROTOCOLS = DNLA_SOURCE_PROTOCOLS;

	private static final String	CM_SINK_PROTOCOLS 	= "";


	private String 		UUID_rootdevice;
	private String		service_name;
	
	private String[]	upnp_entities;

	private PluginInterface		plugin_interface;
	private LoggerChannel		logger;
	
	private UPnPSSDP 				ssdp;
	private UPnPSSDPListener 		ssdp_listener;
	
	private UPnP					upnp;
	private UPnPListener			upnp_listener;
	
	private boolean	alive			= true;
			
	private Map		events					= new HashMap();
	private List	new_events				= new ArrayList();
	private Set		container_update_events;
	

	private UPnPMediaServerContentServer	content_server;
		
	private Set<UPnPMediaRenderer>		renderers = new HashSet<UPnPMediaRenderer>();
	
	private CopyOnWriteList<IPCInterface>	browse_listeners = new CopyOnWriteList<IPCInterface>();
	
	private int		stream_id_next;
	
	private boolean enable_upnp			= true;
	private boolean enable_lan_publish	= true;
	private int		stream_port			= 0;

	private UPnPMediaServerUI		media_server_ui;
	
	private List	logged_searches  = new ArrayList();

	private boolean quickTimeAvail = false;
		
	private BasicPluginConfigModel 	config_model;
	private BasicPluginViewModel	view_model;
	private List					menus = new ArrayList();
	private UTTimer					timer2;
	private UTTimerEvent 			timer2_event;
	private UTTimer					timer3;
	private UTTimerEvent 			timer3_event;
	private DownloadManagerListener	dm_listener;
	private TrackerWebContext		ssdp_web_context;
		
	private byte[]					BLANK_PASSWORD;

		
	private BooleanParameter 		stream_port_upnp_param;
	private BooleanParameter		apply_bind;
	
	private BooleanParameter 		main_auth;
	private BooleanParameter 		main_auth_ext_only;
	private StringParameter			main_username;
	private PasswordParameter		main_password;

	private BooleanParameter 		http_enable_param;
	private IntParameter 			http_port_param;
	private BooleanParameter 		http_port_upnp_param;
	private UPnPMediaServerHTTP		http_server;
	private BooleanParameter 		http_auth;
	private BooleanParameter 		http_auth_same;
	private StringParameter			http_username;
	private PasswordParameter		http_password;

	private IntParameter 			speed_bit_rate_mult;
	private IntParameter 			speed_max_kb_sec;
	private IntParameter 			speed_min_kb_sec;
	
	private StringListParameter		sort_order;
	private BooleanParameter		sort_order_ascending;
	
	private boolean					use_categories;
	private boolean					use_tags;
	private BooleanParameter		show_percent_done;
	private BooleanParameter		show_eta;
	
	private BooleanParameter 		prevent_sleep_param;
	
	private UPnPMediaServerContentDirectory	content_directory;
	
	private Set<AzureusContentDirectory>		registered_directories = new HashSet<AzureusContentDirectory>();
	
	private volatile boolean		unloaded;
	private volatile boolean		initialised;
	
	private Object	startup_lock	= new Object();
	private boolean	starting		= false;
	private AESemaphore			startup_sem = new AESemaphore( "UPnPMediaServer:Startup" );
	
	public void
	initialize(
		PluginInterface		_plugin_interface )
	{
		try{
		
			plugin_interface		= _plugin_interface;
			
			BLANK_PASSWORD = plugin_interface.getUtilities().getSecurityManager().calculateSHA1(new byte[0]);
			
			logger				= plugin_interface.getLogger().getTimeStampedChannel( "MediaServer" ); 
	
			LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
	
			loc_utils.integrateLocalisedMessageBundle( "com.aelitis.azureus.plugins.upnpmediaserver.internat.Messages" );
				
			Utilities utils = plugin_interface.getUtilities();
			
			timer2 	= utils.createTimer( "alive", true );
			timer3 	= utils.createTimer( "eventDispatch", false );
					
			PluginConfig	config = plugin_interface.getPluginconfig();
			
			UUID_rootdevice = config.getPluginStringParameter( "uuid", "" );
			
			if ( UUID_rootdevice.length() == 0 ){
				
				UUID_rootdevice = "uuid:" + UUIDGenerator.generateUUIDString();
			
				config.setPluginParameter( "uuid", UUID_rootdevice );
				
				try{
						// we want to request persistence now so we don't generate multiple device identities
						// if something prevents a later save
					
					COConfigurationManager.setDirty();
					
				}catch( Throwable e ){
				}
			}
				
			upnp_entities = new String[]{
					"upnp:rootdevice",
					"urn:schemas-upnp-org:device:MediaServer:1",
					"urn:schemas-upnp-org:service:ConnectionManager:1",
					"urn:schemas-upnp-org:service:ContentDirectory:1",
					UUID_rootdevice
				};
			
			final UIManager	ui_manager = plugin_interface.getUIManager();
			
			view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText("upnpmediaserver.name") );
			
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

			logger.setDiagnostic();
			logger.setForce( true );
		
			view_model.setConfigSectionID( "upnpmediaserver.name" );
			
			logger.log( "RootDevice: " + UUID_rootdevice );
			
			config_model = 
				ui_manager.createBasicPluginConfigModel( "upnpmediaserver.name" );
			
			config_model.addLabelParameter2( "upnpmediaserver.info" );
			
			config_model.addHyperlinkParameter2("upnpmediaserver.web_link", "http://www.azureuswiki.com/index.php/UG_Plugins#UPnP_Media_Server");
			
			final BooleanParameter enable_upnp_p = config_model.addBooleanParameter2( "upnpmediaserver.enable_upnp", "upnpmediaserver.enable_upnp", true );
	
			enable_upnp = enable_upnp_p.getValue();
				
			final IntParameter stream_port_p = config_model.addIntParameter2( "upnpmediaserver.stream.port", "upnpmediaserver.stream.port", 0 );
	
			final InfoParameter active_port = config_model.addInfoParameter2( "upnpmediaserver.stream.port.active", "" );
			
			stream_port = stream_port_p.getValue();
			
			stream_port_p.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						int	port = stream_port_p.getValue();
						
						if ( port != stream_port ){
							
							stream_port = port;
							
								// revert to random as needed
							
							if ( stream_port == 0 ){
								
								setContentPort( 0 );
							}
							
							createContentServer( active_port, false );
						}
					}
				});
			
			stream_port_upnp_param = config_model.addBooleanParameter2( "upnpmediaserver.stream_port_upnp", "upnpmediaserver.stream_port_upnp", false );
			
			apply_bind = config_model.addBooleanParameter2( "upnpmediaserver.bind.use.default", "upnpmediaserver.bind.use.default", true );
			
			String	default_name = null;
			
			try{
				String cn = plugin_interface.getPlatformManager().getComputerName();
				
				if ( cn != null ){
										
					default_name = "Vuze on " + cn;
				}
			}catch( Throwable e ){
			}
			
			if ( default_name == null ){
				
				PluginConfig plugin_config = plugin_interface.getPluginconfig();
				
				default_name = plugin_config.getPluginStringParameter( "upnpmediaserver.name.default", "" );
				
				if ( default_name.length() == 0 ){
					
					try{
							// seen high CPU on some systems calling this (due to a mass of weird 6to4 interfaces maybe
							// so just call the once
						
						Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
						
						InetAddress	ipv4 = null;
						InetAddress	ipv6 = null;
						
						while( interfaces.hasMoreElements()){
							
							Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
							
							while( addresses.hasMoreElements()){
								
								InetAddress address = addresses.nextElement();
								
								if ( address.isLoopbackAddress()){
									
									continue;
								}
								
								if ( address instanceof Inet4Address ){
									
									ipv4 = address;
									
								}else{
									
									ipv6 = address;
								}
							}
						}
						
						if ( ipv4 != null ){
							
							default_name = "Vuze on " + ipv4.getHostAddress();
							
						}else if ( ipv6 != null ){
							
							default_name = "Vuze on " + ipv6.getHostAddress();
	
						}else{
							
							default_name = "Vuze";
						}
					}catch( Throwable e ){
						
						default_name = "Vuze";
					}
					
					plugin_config.setPluginParameter( "upnpmediaserver.name.default", default_name );
				}
			}
			
			final StringParameter snp = config_model.addStringParameter2( "upnpmediaserver.service_name", "upnpmediaserver.service_name", default_name );
			
			service_name = snp.getValue();
			
			snp.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						service_name = snp.getValue();
					}
				});
			
			sort_order = config_model.addStringListParameter2( 
				"upnpmediaserver.sortorder", "upnpmediaserver.sortorder", 
				new String[]{ "0", "1", "2" }, 
				new String[]{
						loc_utils.getLocalisedMessageText( "upnpmediaserver.sortorder.default" ),
						loc_utils.getLocalisedMessageText( "upnpmediaserver.sortorder.alpha" ),
						loc_utils.getLocalisedMessageText( "upnpmediaserver.sortorder.date" ),
				},
				"0" );
			
			sort_order_ascending = config_model.addBooleanParameter2( "upnpmediaserver.sortorder.asc", "upnpmediaserver.sortorder.asc", true );
			
			ParameterListener sort_order_listener = 
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter param )
					{
						sort_order_ascending.setEnabled( !sort_order.getValue().equals( "0" ));
					};
				};
				
			sort_order_listener.parameterChanged( null );
				
			sort_order.addListener( sort_order_listener );
			
			show_percent_done 	= config_model.addBooleanParameter2(	"upnpmediaserver.show_percent_done", "upnpmediaserver.show_percent_done", false );
			show_eta			= config_model.addBooleanParameter2(	"upnpmediaserver.show_eta", "upnpmediaserver.show_eta", false );
			
			BooleanParameter separate_by_category = config_model.addBooleanParameter2(	"upnpmediaserver.sep_by_cat", "upnpmediaserver.sep_by_cat", false );
			
			use_categories = separate_by_category.getValue();
			
			BooleanParameter separate_by_tags = config_model.addBooleanParameter2(	"upnpmediaserver.sep_by_tags", "upnpmediaserver.sep_by_tags", false );
			
			use_tags = separate_by_tags.getValue();

			LabelParameter speed_lab = config_model.addLabelParameter2( "upnpmediaserver.stream.speed" );
			
			speed_bit_rate_mult = config_model.addIntParameter2( "upnpmediaserver.stream.speed_br_mult", "upnpmediaserver.stream.speed_br_mult", 10, 0, Integer.MAX_VALUE );

			speed_min_kb_sec = config_model.addIntParameter2( "upnpmediaserver.stream.speed_min_kbs", "upnpmediaserver.stream.speed_min_kbs", 4096, 0, Integer.MAX_VALUE );
			
			speed_max_kb_sec = config_model.addIntParameter2( "upnpmediaserver.stream.speed_max_kbs", "upnpmediaserver.stream.speed_max_kbs", 0, 0, Integer.MAX_VALUE );

			boolean supports_sleep = plugin_interface.getUtilities().supportsPowerStateControl( PowerManagementListener.ST_SLEEP );
			
			if ( supports_sleep ){
				
					// prevent sleep
				
				prevent_sleep_param = config_model.addBooleanParameter2("upnpmediaserver.prevent_sleep", "upnpmediaserver.prevent_sleep", true);
			}
			
				// publish all media
			
			final BooleanParameter enable_publish_p = config_model.addBooleanParameter2( "upnpmediaserver.enable_publish", "upnpmediaserver.enable_publish", false );
	
			enable_lan_publish = enable_publish_p.getValue();
			
			enable_publish_p.addListener(
					new ParameterListener()
					{
						public void
						parameterChanged(
							Parameter	param )
						{
							enable_lan_publish = enable_publish_p.getValue();
						}
					});
			
			enable_upnp_p.addListener(
					new ParameterListener()
					{
						public void
						parameterChanged(
							Parameter	param )
						{
							enable_upnp = enable_upnp_p.getValue();
							snp.setEnabled( enable_upnp );
							enable_publish_p.setEnabled( enable_upnp );
						}
					});
			
			if ( !enable_upnp ){
				
				snp.setEnabled( false );
				
				enable_publish_p.setEnabled( false );
			}
			
				// main password
			
			main_auth				= config_model.addBooleanParameter2( "upnpmediaserver.auth.enable", "upnpmediaserver.auth.enable", false );
			main_auth_ext_only		= config_model.addBooleanParameter2( "upnpmediaserver.auth.ext_only", "upnpmediaserver.auth.ext_only", false );

			main_username 	= config_model.addStringParameter2( "upnpmediaserver.auth.user", "upnpmediaserver.auth.user", "" );
			
			main_password 	= config_model.addPasswordParameter2( "upnpmediaserver.auth.password", "upnpmediaserver.auth.password", PasswordParameter.ET_SHA1, new byte[0] );

			main_auth.addEnabledOnSelection( main_auth_ext_only );
			main_auth.addEnabledOnSelection( main_username );
			main_auth.addEnabledOnSelection( main_password );
					
			config_model.createGroup(
					"upnpmediaserver.media_group",
					new Parameter[]{
							enable_upnp_p,
							stream_port_p,
							active_port,
							stream_port_upnp_param,
							apply_bind,
							snp,
							sort_order, sort_order_ascending, 
							show_percent_done,
							show_eta,
							separate_by_category, separate_by_tags,
							enable_publish_p,
							speed_lab, speed_bit_rate_mult, speed_min_kb_sec, speed_max_kb_sec,
							prevent_sleep_param,
							main_auth, main_auth_ext_only, main_username, main_password,
					});

			
			
				// http server
			
			http_enable_param = config_model.addBooleanParameter2("upnpmediaserver.http_enable", "upnpmediaserver.http_enable", false);

			http_port_param = config_model.addIntParameter2( "upnpmediaserver.http_port", "upnpmediaserver.http_port", HTTP_PORT_DEFAULT );
			
			http_port_upnp_param = config_model.addBooleanParameter2( "upnpmediaserver.http_port_upnp", "upnpmediaserver.http_port_upnp", false );
			
			http_auth		= config_model.addBooleanParameter2( "upnpmediaserver.httpauth.enable", "upnpmediaserver.httpauth.enable", false );
			
			http_auth_same	= config_model.addBooleanParameter2( "upnpmediaserver.httpauth.same", "upnpmediaserver.httpauth.same", true );

			http_username 	= config_model.addStringParameter2( "upnpmediaserver.httpauth.user", "upnpmediaserver.auth.user", "" );
			
			http_password 	= config_model.addPasswordParameter2( "upnpmediaserver.httpauth.password", "upnpmediaserver.auth.password", PasswordParameter.ET_SHA1, new byte[0] );

			http_auth.addEnabledOnSelection( http_auth_same );
			http_auth.addEnabledOnSelection( http_username );
			http_auth.addEnabledOnSelection( http_password );
			
			http_auth_same.addDisabledOnSelection( http_username );
			http_auth_same.addDisabledOnSelection( http_password );

			http_enable_param.addEnabledOnSelection( http_port_param );
			http_enable_param.addEnabledOnSelection( http_port_upnp_param );
			http_enable_param.addEnabledOnSelection( http_auth );	
			http_enable_param.addEnabledOnSelection( http_auth_same );
			http_enable_param.addEnabledOnSelection( http_username );
			http_enable_param.addEnabledOnSelection( http_password );
			
			
			config_model.createGroup(
				"upnpmediaserver.http_group",
				new Parameter[]{
						http_enable_param,
						http_port_param, http_port_upnp_param, 
						http_auth, http_auth_same, http_username, http_password,
				});
			
				// enable menus
			
			BooleanParameter menu_param = config_model.addBooleanParameter2("upnpmediaserver.enable_menus", "upnpmediaserver.enable_menus", true);
			
			if ( menu_param.getValue()){
				
				buildMenu();
			}
						
			ActionParameter print = config_model.addActionParameter2( "upnpmediaserver.printcd.label", "upnpmediaserver.printcd.button" );
			
			print.addListener(
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter param ) 
					{
						content_directory.print();
					}
				});
			
			ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance.getUIType() == UIInstance.UIT_SWT ){	
					
							ui_manager.removeUIListener( this );
							
							log( "SWT user interface bound" );
							
							try{
								media_server_ui = (UPnPMediaServerUI)UPnPMediaServer.class.forName( "com.aelitis.azureus.plugins.upnpmediaserver.ui.swt.UPnPMediaServerUISWT" ).newInstance();
								
							}catch( Throwable e ){
							
								Debug.out( e );
							}
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						
					}
				});
			
			createContentServer( active_port, true );
			
			content_directory = new UPnPMediaServerContentDirectory( this );
	
			plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						DelayedTask dt = 
							plugin_interface.getUtilities().createDelayedTask(new Runnable()
							{
								public void 
								run()
								{
									Thread t = 
										new Thread( "UPnPMediaServer::init" )
										{
											public void
											run()
											{
												UPnPMediaServer.this.start();
											}
										};
									
									t.setPriority( Thread.MIN_PRIORITY );
									
									t.setDaemon( true );
									
									t.start();
								}
							});
						
						dt.queue();
					}
					
					public void
					closedownInitiated()
					{	
						if ( ssdp != null ){
							
							stop();
						}
						
						if ( http_server != null ){
						
							http_server.destroy();
						}
						
						if ( content_directory != null ){
							
							content_directory.destroy();
						}
					}
					
					public void
					closedownComplete()
					{
					}
				});
			
			if ( supports_sleep ){
			
				plugin_interface.getUtilities().addPowerManagementListener( this );
			}
		}finally{
			
			initialised	= true;
			
			if ( unloaded ){
				
				unload();
			}
		}
	}
	
	private void
	handleUPnP(
		int			port,
		String		name,
		boolean		enabled )
	{
		PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
		
		if ( pi_upnp == null ){
			
			log( "No UPnP plugin available, not attempting port mapping");
			
		}else{
			
			PluginConfig config = plugin_interface.getPluginconfig();
			
			String	key = "upnp.port.for." + name;
			
			int	existing_port = config.getPluginIntParameter( key, 0 );
			
			if ( !enabled || existing_port != port ){
				
				if ( existing_port != 0 ){
					
					log( "Removing UPnP mapping: port=" + existing_port + ", name='" + name + "'"  );
					
					UPnPMapping m = ((UPnPPlugin)pi_upnp.getPlugin()).addMapping( name, true, existing_port, false );
					
					m.destroy();
				}
				
				config.setPluginParameter( key, 0 );
			}
			
			if ( enabled ){
				
				if ( port == 0 ){
					
					log( "Invalid port for UPnP mapping, ignoring" );
					
				}else{
					log( "Creating UPnP mapping: port=" + port + ", name='" + name + "'"  );
					
					((UPnPPlugin)pi_upnp.getPlugin()).addMapping( name, true, port, true );
					
					config.setPluginParameter( key, port );
				}
			}
		}
	}
	
	private void
	doContentUPnP()
	{
		handleUPnP( content_server==null?0:content_server.getPort(), "Media Server Content", stream_port_upnp_param.getValue());
	}
	
	protected int
	getAverageBitRateMultiplier()
	{
		return( speed_bit_rate_mult.getValue());
	}
	
	protected int
	getMinBytesPerSecond()
	{
		return( speed_min_kb_sec.getValue() * 1024 );
	}
	
	protected int
	getMaxBytesPerSecond()
	{
		return( speed_max_kb_sec.getValue() * 1024 );
	}
	
	private void
	createContentServer(
		InfoParameter	info,
		boolean			init )
	{
		if ( content_server != null ){
			
			log( "Destroying existing content server on port " + content_server.getPort());
			
			content_server.destroy();
			
			content_server = null;
		}
		
		try{
			content_server = new UPnPMediaServerContentServer( this );
	
			int	active_port = content_server.getPort();
			
			log("Content port = " + active_port);
		
			info.setValue( String.valueOf( active_port));
			
			if ( init ){
			
				plugin_interface.addListener(
					new PluginListener()
					{
						public void
						initializationComplete()
						{
							plugin_interface.removeListener( this );
							
							doContentUPnP();
						}
						
						public void
						closedownInitiated()
						{
						}
						
						public void
						closedownComplete()
						{	
						}
					});
			}else{
				
				doContentUPnP();
			}
		}catch( Throwable e ){
			
			log( "Failed to initialise content server", e );
		}
	}
	
	protected UPnPMediaServerContentServer
	getContentServer()
	{
		return( content_server );
	}
	
	protected boolean
	authContentPort(
		String		client_address )
	{
		if ( !main_auth.getValue()){
			
			return( false );
		}
		
		if ( main_auth_ext_only.getValue()){
			
			if ( client_address == null ){
				
				log( "Authentication failed, client address unavailable" );
				
				return( true );
			}
			
			try{
				InetAddress ia = InetAddress.getByName( client_address );
			
				if ( ia.isLoopbackAddress() || ia.isLinkLocalAddress() || ia.isSiteLocalAddress()){
					
					return( false );
				}
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				log( "Authentication failed", e );
				
				return( true );
			}
		}
		
		return( true );
	}
	
	protected boolean
	doContentAuth(
		String			client_address,
		String			user,
		String			password )
	{
		if ( !main_auth.getValue()){
			
			return( true );
		}
				
		byte[]	p_sha1 = main_password.getValue();
		
		if ( p_sha1.length == 0 || Arrays.equals( p_sha1, BLANK_PASSWORD )){
			
			return( true );
		}
		
		if ( !user.equals( main_username.getValue())){
			
			log( "Authentication failed: userame '" + user + "' invalid, client=" + client_address );
			
			return( false );
		}
		
		boolean ok = Arrays.equals( p_sha1, plugin_interface.getUtilities().getSecurityManager().calculateSHA1( password.getBytes()));
		
		if ( !ok ){
			
			log( "Authentication failed: incorrect password, client=" + client_address );
		}
		
		return( ok );
	}
	
	protected boolean
	doHTTPAuth(
		String		username,
		String		password )
	{
		if ( !http_auth.getValue()){
			
			return( true );
		}
		
		String	target_user;
		byte[]	target_password;;

		if ( http_auth_same.getValue()){
			
			target_user 	= main_username.getValue();
			target_password	= main_password.getValue();
			
		}else{
			
			target_user 	= http_username.getValue();
			target_password	= http_password.getValue();
		}
		
		if ( target_password.length == 0 || Arrays.equals( target_password, BLANK_PASSWORD )){
			
			return( true );
		}
		
		if ( !username.equals( target_user )){
			
			return( false );
		}
			
		return( Arrays.equals( target_password, plugin_interface.getUtilities().getSecurityManager().calculateSHA1(password.getBytes())));

	}
	
	private void 
	buildMenu() 
	{
		MenuItemFillListener	menu_fill_simple_listener = 
			new MenuItemFillListener()
			{
				public void
				menuWillBeShown(
					MenuItem	menu,
					Object		_target )
				{
					Object	obj = null;
					
					if ( _target instanceof TableRow ){
						
						obj = ((TableRow)_target).getDataSource();
	
					}else{
						
						TableRow[] rows = (TableRow[])_target;
					     
						if ( rows.length > 0 ){
						
							obj = rows[0].getDataSource();
						}
					}
					
					if ( obj == null ){
						
						menu.setEnabled( false );

						return;
					}
					
					Download				download;
					DiskManagerFileInfo		file;
					
					if ( obj instanceof Download ){
					
						download = (Download)obj;

						if ( DISABLE_MENUS_FOR_INCOMPLETE && !download.isComplete()){
							
							menu.setEnabled( false );

							return;
						}

					}else{
						
						file = (DiskManagerFileInfo)obj;
						
						if ( DISABLE_MENUS_FOR_INCOMPLETE && file.getDownloaded() != file.getLength()){
							
							menu.setEnabled( false );

							return;
						}
					}
					
					menu.setEnabled( true );
				}
			};
		
		{
			// play
		
			MenuItemListener	menu_listener = 
				new MenuItemListener()
				{
					public void
					selected(
						MenuItem		_menu,
						Object			_target )
					{
						Object	obj = ((TableRow)_target).getDataSource();
						
						if ( obj == null ){
							
							return;
						}
						
						Download				download;
						DiskManagerFileInfo		file;
						
						if ( obj instanceof Download ){
						
							download = (Download)obj;
		
							file	= download.getDiskManagerFileInfo()[0];
						
						}else{
							
							file = (DiskManagerFileInfo)obj;
							
							try{
								download	= file.getDownload();
								
							}catch( DownloadException e ){	
								
								Debug.printStackTrace(e);
								
								return;
							}
						}
						
						String	id = content_directory.createResourceID( download.getTorrent().getHash(), file );
						
						System.out.println( "play: " + id );
	
						UPnPMediaServerContentDirectory.contentItem item = content_directory.getContentFromResourceID( id );
						
						if ( item != null ){
							
							Iterator	it;
							
							synchronized( renderers ){
								
								it = new HashSet( renderers ).iterator();
							}
								
							while( it.hasNext()){
									
								UPnPMediaRenderer	renderer = (UPnPMediaRenderer)it.next();
								
								if ( !renderer.isBusy()){
									
									int	stream_id;
									
									synchronized( UPnPMediaServer.this ){
										
										stream_id	= stream_id_next++;
									}
									
									renderer.play( item, stream_id );
								}
							}
						}
					}
				};
				
				MenuItemListener	menu_listener_play_external = 
					new MenuItemListener()
					{
						public void
						selected(
							MenuItem		_menu,
							Object			_target )
						{
							Object	obj = ((TableRow)_target).getDataSource();
							
							if ( obj == null ){
								
								return;
							}
							
							try{
								play(obj);
								
							}catch( IPCException e ){
								
								log( "Failed to play '" + obj + "'", e );
							}
							
						}
					};
					
				// top level menus
					
			TableContextMenuItem menu_item_itorrents;
			if ( DISABLE_MENUS_FOR_INCOMPLETE ){
				menu_item_itorrents = null;
			}else{
				menu_item_itorrents = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "upnpmediaserver.contextmenu");
				menus.add( menu_item_itorrents );
			}
			TableContextMenuItem menu_item_ctorrents 	= plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "upnpmediaserver.contextmenu");
			menus.add( menu_item_ctorrents );
			TableContextMenuItem menu_item_files 		= plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_TORRENT_FILES, "upnpmediaserver.contextmenu");
			menus.add( menu_item_files );
			
				// set menu style
			
			if ( menu_item_itorrents != null ){
				menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_MENU);
			}
			menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_MENU);
			menu_item_files.setStyle(TableContextMenuItem.STYLE_MENU);
			
				// create play-external items
			
			TableContextMenuItem menup1;
			if ( DISABLE_MENUS_FOR_INCOMPLETE ){
				menup1 = null;
			}else{
				menup1 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_itorrents, 	"upnpmediaserver.contextmenu.playExternal" );
			}
			TableContextMenuItem menup2 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_ctorrents, "upnpmediaserver.contextmenu.playExternal" );
			TableContextMenuItem menup3 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_files, "upnpmediaserver.contextmenu.playExternal" );

				// create play items
			
			TableContextMenuItem menu1;
			if ( DISABLE_MENUS_FOR_INCOMPLETE ){
				menu1 = null;
			}else{
				menu1 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_itorrents, 	"upnpmediaserver.contextmenu.play" );
			}
			TableContextMenuItem menu2 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_ctorrents, "upnpmediaserver.contextmenu.play" );
			TableContextMenuItem menu3 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_files, "upnpmediaserver.contextmenu.play" );
		
			MenuItemFillListener	menu_fill_listener = 
				new MenuItemFillListener()
				{
					public void
					menuWillBeShown(
						MenuItem	menu,
						Object		_target )
					{
						Object	obj = null;
						
						if ( _target instanceof TableRow ){
							
							obj = ((TableRow)_target).getDataSource();
		
						}else{
							
							TableRow[] rows = (TableRow[])_target;
							     
							if ( rows.length > 0 ){
							
								obj = rows[0].getDataSource();
							}
						}
						
						if ( obj == null ){
							
							menu.setEnabled( false );

							return;
						}
						
						Download				download;
						DiskManagerFileInfo		file;
						
						if ( obj instanceof Download ){
						
							download = (Download)obj;

							if ( DISABLE_MENUS_FOR_INCOMPLETE && !download.isComplete()){
								
								menu.setEnabled( false );

								return;
							}

						}else{
							
							file = (DiskManagerFileInfo)obj;
							
							if ( DISABLE_MENUS_FOR_INCOMPLETE && file.getDownloaded() != file.getLength()){
								
								menu.setEnabled( false );

								return;
							}
						}
						
						synchronized( renderers ){
							
							boolean	enabled = false;
							
							Iterator	it = renderers.iterator();
							
							while( it.hasNext()){
								
								UPnPMediaRenderer	renderer = (UPnPMediaRenderer)it.next();
								
								if ( !renderer.isBusy()){
									
									enabled	= true;
									break;
								}
							}
							
							menu.setEnabled( enabled );
						}
					}
				};
			
			if ( menu1 != null ){
				menu1.addListener( menu_listener );
			}
			menu2.addListener( menu_listener );
			menu3.addListener( menu_listener );
			
			if ( menu1 != null ){
				menu1.addFillListener( menu_fill_listener );
			}
			menu2.addFillListener( menu_fill_listener );
			menu3.addFillListener( menu_fill_listener );
			
			if ( menup1 != null ){
				menup1.addListener( menu_listener_play_external );
			}
			menup2.addListener( menu_listener_play_external );
			menup3.addListener( menu_listener_play_external );
			
			menup2.addFillListener( menu_fill_simple_listener );
			menup3.addFillListener( menu_fill_simple_listener );
		
			// copy to clip
		
			menu_listener = 
				new MenuItemListener()
				{
					public void
					selected(
						MenuItem		_menu,
						Object			_target )
					{
						Object	obj = ((TableRow)_target).getDataSource();
						
						if ( obj == null ){
							
							return;
						}
						
						Download				download;
						DiskManagerFileInfo		file;
						
						if ( obj instanceof Download ){
						
							download = (Download)obj;
		
							file	= download.getDiskManagerFileInfo()[0];
						
						}else{
							
							file = (DiskManagerFileInfo)obj;
							
							try{
								download	= file.getDownload();
								
							}catch( DownloadException e ){	
								
								Debug.printStackTrace(e);
								
								return;
							}
						}
						
						String	id = content_directory.createResourceID( download.getTorrent().getHash(), file );
							
						UPnPMediaServerContentDirectory.contentItem item = content_directory.getContentFromResourceID( id );
						
						if ( item != null ){
						
							try{
								plugin_interface.getUIManager().copyToClipBoard( item.getURI( getLocalIP(), -1 ));
								
							}catch( Throwable e ){
								
								log( "Failed to copy to clipboard", e);
							}
						}
					}
				};
				
			if ( DISABLE_MENUS_FOR_INCOMPLETE ){
				menu1 = null;
			}else{
				menu1 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_itorrents, 	"upnpmediaserver.contextmenu.toclipboard" );
			}
			menu2 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_ctorrents, 	"upnpmediaserver.contextmenu.toclipboard" );
			menu3 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(menu_item_files, 			"upnpmediaserver.contextmenu.toclipboard" );
			
			if ( menu1 != null ){
				menu1.addFillListener( menu_fill_simple_listener );
			}
			menu2.addFillListener( menu_fill_simple_listener );
			menu3.addFillListener( menu_fill_simple_listener );

			if ( menu1 != null ){
				menu1.addListener( menu_listener );
			}
			menu2.addListener( menu_listener );
			menu3.addListener( menu_listener );
		}
	}

	public void
	unload()
	{
		unloaded = true;

		if ( !initialised ){
			
			return;
		}
					
		log( "Unload starts" );
		
		try{
				// get into fully init state before attempting to un-init otherwise the two things
				// can go on in parallel with non-deterministic results
			
			ensureStarted();
			
			if ( timer2_event != null ){
				
				timer2_event.cancel();
			}
			
			if ( timer3_event != null ){
				
				timer3_event.cancel();
			}
		
			timer2.destroy();
			timer3.destroy();
			
			config_model.destroy();
			view_model.destroy();
			
			for (int i=0;i<menus.size();i++){
				
				((TableContextMenuItem)menus.get(i)).remove();
			}
			
			if ( upnp != null && upnp_listener != null ){
				
				upnp.removeRootDeviceListener( upnp_listener );
			}
			
			if ( ssdp != null && ssdp_listener != null ){
				
				ssdp.removeListener( ssdp_listener );
			}
			
			if ( dm_listener != null ){
				
				plugin_interface.getDownloadManager().removeListener( dm_listener );
			}
			
			if ( ssdp_web_context != null ){
				
				ssdp_web_context.destroy();
			}
			
			if ( http_server != null ){
				
				try{
					http_server.destroy();
					
				}catch( Throwable e ){
				}
			}
			
			Set<UPnPMediaRenderer> rends;
			
			synchronized( renderers ){
				
				rends = new HashSet<UPnPMediaRenderer>( renderers );
				
				renderers.clear();
			}

			for ( UPnPMediaRenderer r: rends ){
				
				r.destroy();
			}
			
			content_server.destroy();
			
			if ( content_directory != null ){
				
				content_directory.destroy();
			}
			
			for ( AzureusContentDirectory azcd: registered_directories ){
				
				azcd.removeListener( this );
			}
			
			if ( plugin_interface != null ){
			
				plugin_interface.getUtilities().removePowerManagementListener( this );
			}

		}catch( Throwable e ){
			
			log( "Unload failed", e );
			
		}finally{
			
			log( "Unload complete" );
		}
	}
	
	protected AzureusContentDirectory[]
	getAzureusContentDirectories()
	{
		AzureusContentDirectory[]	directories = AzureusContentDirectoryManager.getDirectories();

		for ( AzureusContentDirectory directory: directories ){
	
			synchronized( registered_directories ){
				
				if ( !registered_directories.contains( directory )){
					
					directory.addListener( this );
					
					registered_directories.add( directory );
				}
			}
		}
		
		return( directories );
	}
	
	public void 
	contentChanged(
		AzureusContentFile 	file, 
		String 				property )
	{
		
		if ( 	( property.equals( AzureusContentFile.PT_CATEGORIES ) && use_categories ) ||
				( property.equals( AzureusContentFile.PT_TAGS ) && use_tags )){
			
			content_directory.contentChanged( file );
		}
	}
	
	protected boolean
	useCategories()
	{
		return( use_categories );
	}
	
	protected boolean
	useTags()
	{
		return( use_tags );
	}
	
	protected boolean
	showPercentDone()
	{
		return( show_percent_done.getValue());
	}
	
	protected boolean
	showETA()
	{
		return( show_eta.getValue());
	}
	
	protected void
	removeRenderer(
		UPnPMediaRenderer	renderer )
	{
		synchronized( renderers ){
			
			renderers.remove( renderer );
		}
	}
	
	protected List<AzureusContentFilter>
	receivedBrowse(
		TrackerWebPageRequest		request,
		boolean						is_http )
	{
		List<AzureusContentFilter> filters = new ArrayList<AzureusContentFilter>();
		
		Map<String,Object>	browse_args = new HashMap<String, Object>();
		
		browse_args.put( "source", is_http?"http":"upnp" );
		
		for ( IPCInterface ipc: browse_listeners ){
			
			try{
				Map<String,Object> result = (Map<String,Object>)ipc.invoke( "browseReceived", new Object[]{ request, browse_args });
				
				if ( result != null ){
					
					AzureusContentFilter filter = (AzureusContentFilter)result.get( "filter" );
					
					if ( filter != null ){
						
						filters.add( filter );
					}
				}
			}catch( Throwable e ){
				
				log( "browse callback failed", e );
			}
		}
		
		return( filters );
	}
	
	public String
	getPowerName()
	{
		return( "MediaServer" );
	}
	
	public boolean
	requestPowerStateChange(
		int		new_state,
		Object	data )
	{
		if ( prevent_sleep_param != null && prevent_sleep_param.getValue()){
			
			UPnPMediaServerContentServer cs = content_server;

			if ( cs != null ){
				
				if ( cs.getConnectionCount() > 0 ){
					
					return( false );
				}
			}
		}
		
		return( true );
	}

	public void
	informPowerStateChange(
		int		new_state,
		Object	data )
	{
	}
	
		// ***** plugin public interface starts
	
	public void
	addBrowseListener(
		IPCInterface	callback )
	{
		browse_listeners.add( callback );
	}
	
	public void
	removeBrowseListener(
		IPCInterface	callback )
	{
		browse_listeners.remove( callback );
	}
	
	public void
	addContent(
		AzureusContentFile		file )
	{
		content_directory.addContent( file);
	}
	
	public void
	removeContent(
		AzureusContentFile		file )
	{
		content_directory.removeContent( file );
	}
	
	public int
	addLocalRenderer(
		IPCInterface	callback )
	{
		UPnPMediaRendererLocal	renderer = new UPnPMediaRendererLocal( this, callback );
		
		synchronized( renderers ){
			
			renderers.add( renderer );
		}
		
		return( renderer.getID());
	}
	
	public long
	getStreamBufferBytes(
		int		content_id,
		int		stream_id )
	
		throws IPCException
	{
		UPnPMediaServerContentDirectory.content	c = content_directory.getContentFromID( content_id );
		
		if ( c == null || !( c instanceof UPnPMediaServerContentDirectory.contentItem )){
			
			throw( new IPCException( "Content id " + content_id + " not found" ));
		}
		
		UPnPMediaServerContentDirectory.contentItem	item = (UPnPMediaServerContentDirectory.contentItem)c;

		DiskManagerFileInfo	file = item.getFile();
		
			// if already complete then simple
		
		if ( file.getLength() == file.getDownloaded()){
			
			return( Long.MAX_VALUE );
		}
		
		UPnPMediaServerContentServer.streamInfo	stream_info = content_server.getStreamInfo( stream_id );
		
		if ( stream_info == null ){
			
			return( -1 );
		}
		
		long	remaining 	= stream_info.getRemaining();
		long	available	= stream_info.getAvailableBytes();
		
		if ( remaining == available ){
			
			if ( available == -1 ){
				
				return( -1 );
			}
			
				// indicate all available
			
			return( Long.MAX_VALUE );
		}
		
		return( available );
	}
	
	public int
	getStreamType(
		int		content_id )
	
		throws IPCException
	{
		UPnPMediaServerContentDirectory.content	c = content_directory.getContentFromID( content_id );
		
		if ( c == null || !( c instanceof UPnPMediaServerContentDirectory.contentItem )){
			
			throw( new IPCException( "Content id " + content_id + " not found" ));
		}
		
		UPnPMediaServerContentDirectory.contentItem	item = (UPnPMediaServerContentDirectory.contentItem)c;

		String	cla = item.getContentClass();
		
		if ( cla == UPnPMediaServerContentDirectory.CONTENT_AUDIO ){
			
			return( 0 );
			
		}else if ( cla == UPnPMediaServerContentDirectory.CONTENT_VIDEO ){
			
			return( 1 );
			
		}else{
			
			return( -1 );
		}
	}
	
	public void
	playDownload(
		Download			download )
	
		throws IPCException
	{
		play( download );
	}
	
	
	public void
	playFile(
		DiskManagerFileInfo	file )
	
		throws IPCException
	{
		play( file );
	}
	
	public String
	getContentURL(
		Download download)
	
		throws IPCException
	{
		return( getContentURL( download.getDiskManagerFileInfo()[0]));
	}
	
	public String
	getContentURL(
		DiskManagerFileInfo	file )
	
		throws IPCException
	{
		try{
			String	id = content_directory.createResourceID( file.getDownloadHash(), file );

			UPnPMediaServerContentDirectory.contentItem item = content_directory.getContentFromResourceID( id );

			if ( item != null ){

				try{
					return item.getURI( getLocalIP(), -1 );

				}catch( Throwable e ){

					log( "Failed to get URI", e);
				}
			}
			
			return "";
			
		} catch (Throwable t) {
			
			throw new IPCException(t);
		}
	}

	public String
	getMimeType(
		DiskManagerFileInfo	file )
	
		throws IPCException
	{
		try{
			String	id = content_directory.createResourceID( file.getDownloadHash(), file );

			UPnPMediaServerContentDirectory.contentItem item = content_directory.getContentFromResourceID( id );

			if ( item != null ){

				try{
					return( item.getContentType());

				}catch( Throwable e ){

					log( "Failed to get content type", e);
				}
			}
			
			return "";
			
		} catch (Throwable t) {
			
			throw new IPCException(t);
		}
	}
	
	public void
	invalidateDirectory()
	{
		content_directory.invalidate();
	}
	
	public void setQuickTimeAvailable(
		boolean avail )
	{
		quickTimeAvail = avail;
	}
	
	public boolean
	isRecognisedMedia(
		DiskManagerFileInfo	file )
	{
		try{
			String	id = content_directory.createResourceID( file.getDownload().getTorrent().getHash(), file );
		
			UPnPMediaServerContentDirectory.contentItem item = content_directory.getContentFromResourceID( id );
			
			if ( item == null ){
				
				return( false );
			}
			
			String cla = item.getContentClass();
		
			return( cla == UPnPMediaServerContentDirectory.CONTENT_AUDIO || 
					cla == UPnPMediaServerContentDirectory.CONTENT_IMAGE || 
					cla == UPnPMediaServerContentDirectory.CONTENT_VIDEO );
			
		}catch( Throwable e ){
			
			logger.log( e );
		}
		
		return( false );
	}

	public String
	getServiceName()
	{
		return( service_name );
	}
	
		// **** plugin public interface ends
	
	protected PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected String
	getServerName()
	{
		return( System.getProperty( "os.name" ) + "/" + System.getProperty("os.version") + " UPnP/1.0 " +
				Constants.AZUREUS_NAME + "/" + Constants.AZUREUS_VERSION );
	}
	
	protected void
	log(
		String		str )
	{
		logger.log( str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		logger.log( str, e );
	}
	
	protected void
	logAlert(
		String		str )
	{
		logger.logAlertRepeatable( LoggerChannel.LT_ERROR, "UPnP Media Server: " + str );
	}
	
	protected void
	start()
	{
		synchronized( startup_lock ){
			
			if ( starting ){
				
				return;
			}
			
			starting	= true;
		}
		
		logger.log( "Server starts: upnp enabled=" + enable_upnp );
		
		try{
			if ( unloaded ){
				
				return;
			}
			
			if ( enable_upnp ){
				
				doContentUPnP();
				
				UPnPAdapter adapter = 
					new UPnPAdapter()
					{
						public SimpleXMLParserDocument
						parseXML(
							String	data )
						
							throws SimpleXMLParserDocumentException
						{
							return( plugin_interface.getUtilities().getSimpleXMLParserDocumentFactory().create( data ));
						}
						
						public ResourceDownloaderFactory
						getResourceDownloaderFactory()
						{
							return( plugin_interface.getUtilities().getResourceDownloaderFactory());
						}
						
						public UTTimer
						createTimer(
							String	name )
						{
							return( plugin_interface.getUtilities().createTimer( name ));
						}
						
						public void
						createThread(
							String				name,
							final Runnable		runnable )
						{
							plugin_interface.getUtilities().createThread( name, runnable );
						}
						
						public Comparator
						getAlphanumericComparator()
						{
							return( plugin_interface.getUtilities().getFormatters().getAlphanumericComparator( true ));
						}
	
						public void
						log(
							Throwable	e )
						{
							Debug.printStackTrace(e);
						}
						
						public void
						trace(
							String	str )
						{
							// System.out.println( str );
						}
						
						public void
						log(
							String	str )
						{
							// System.out.println( str );
						}
						
						public String
						getTraceDir()
						{
							return( plugin_interface.getPluginDirectoryName());
						}
					};
					
	
				ssdp = UPnPFactory.getSSDP( 
									adapter, 
									UPnPSSDP.SSDP_GROUP_ADDRESS, 
									UPnPSSDP.SSDP_GROUP_PORT, 
									0, 
									null );
	
				int	ssdp_port = ssdp.getControlPort();
			
				logger.log( "SSDP port: " + ssdp_port );
				
				createContent();
				
					// bail out early in to reduce possibility of breaking a reload
					// in progress
				
				if ( unloaded ){
					
					return;
				}
				
				ssdp_web_context = 
					plugin_interface.getTracker().createWebContext(
							ssdp_port,
							Tracker.PR_HTTP );
				
				ssdp_web_context.addAuthenticationListener(
					new TrackerAuthenticationAdapter()
					{
						public boolean
						authenticate(
							String		headers,
							URL			resource,
							String		user,
							String		password )
						{
							String	client = getHeaderField( headers, "X-Real-IP" );
							
							if ( authContentPort( client )){
							
								return( doContentAuth( client, user, password ));
							}
							
							return( true );
						}
						
						private String
						getHeaderField(
							String	headers,
							String	field )
						{
							String lc_headers = headers.toLowerCase();
							
							int p1 = lc_headers.indexOf( field.toLowerCase() + ":" );
							
							if ( p1 != -1 ){
							
								int	p2 = lc_headers.indexOf( '\n', p1 );
								
								if ( p2 != -1 ){
									
									return( headers.substring( p1+field.length()+1, p2 ).trim());
								}
							}
							
							return( null );
						}
					});
				
				ssdp_web_context.addPageGenerator(
					new TrackerWebPageGenerator()
					{
						public boolean
						generate(
							TrackerWebPageRequest		request,
							TrackerWebPageResponse		response )
						
							throws IOException
						{
							List<AzureusContentFilter> filters = receivedBrowse( request, false );
							
							String	url = request.getURL();
							
							String	header	= request.getHeader();
														
							Map		headers = request.getHeaders();
														
							String	action = (String)headers.get( "soapaction" );					
	
							if ( 	url.equals( "/ConnectionManager/Event" )||
									url.equals( "/ContentDirectory/Event" )){					
									
								// System.out.println( request.getHeader());
	
								Map	headers_out = new HashMap();
								
								String result = 
									processEvent( 
										url.equals( "/ContentDirectory/Event" ),
										request.getClientAddress2(),
										headers_out,	
										header.startsWith( "SUBSCRIBE" ),
										(String)headers.get( "callback" ),
										(String)headers.get( "sid" ),
										(String)headers.get( "timeout" ));
										
								response.setHeader( "SERVER", getServerName());
								
								Iterator	it = headers_out.keySet().iterator();
								
								while( it.hasNext()){
									
									String	name 	= (String)it.next();
									String	value	= (String)headers_out.get(name);
									
									response.setHeader( name, value );
								}
								
								response.useStream( "xml", new ByteArrayInputStream( result.getBytes( "UTF-8" )));
	
								return( true );
															
							}else if ( action == null ){
								
									// unfortunately java lets getSourceAsStream escape from the
									// package hierarchy and onto the file system, at least when
									// loading the class from FS instead of .jar
								
									// first we don't support ..
								
								url = url.trim();
								
								if ( url.indexOf( ".." ) == -1 && !url.endsWith( "/" )){
								
										// second we don't support nested resources
									
									int	 pos = url.lastIndexOf("/");
									
									if ( pos != -1 ){
										
										url = url.substring( pos );
									}
									
									String UA = (String)headers.get( "user-agent" );
									
									if ( UA != null && UA.toLowerCase().startsWith( "xbox" )){
										
										url = url.replaceAll( "RootDevice", "RootDeviceXbox" );
									}
									
									String AV_Client_Info = (String)headers.get( "x-av-client-info" );
									
										// hack to drop in black-background icons for bravia and sony blu-rays
									
									if ( AV_Client_Info != null ){
										
										AV_Client_Info = AV_Client_Info.toLowerCase();
										
										if ( 	AV_Client_Info.contains( "bravia" ) ||
												( AV_Client_Info.contains( "sony" ) && AV_Client_Info.contains( "blu-ray" ))){
														
											if ( url.endsWith( ".jpg" )){
											
												url = url.substring( 0, url.length()-4 ) + "b.jpg";
											}
										}
									}
								
									String resource = "/com/aelitis/azureus/plugins/upnpmediaserver/resources" + url;
						
									InputStream stream = getClass().getResourceAsStream( resource );
									
									if ( stream != null ){
																				
										try{
											if ( url.startsWith( "/RootDevice" )){
												
												byte[]	buffer = new byte[1024];
												
												String	content = "";
												
												while( true ){
													
													int	len = stream.read( buffer );
													
													if ( len <= 0 ){
														
														break;
													}
													
													content += new String( buffer, 0, len, "UTF-8" );
												}
												
												content = content.replaceAll( "%UUID%", UUID_rootdevice );
												content = content.replaceAll( "%SERVICENAME%", service_name );
												
												stream.close();
												
												stream = new ByteArrayInputStream( content.getBytes( "UTF-8" ));
											}
											
											String ext = FileUtil.getExtension(resource);
											response.useStream( ext.length() <= 1 ? "xml" : ext.substring(1), stream );
										
											return( true );
											
										}finally{
												
											stream.close();
										}
									}
								}
								
								String 	temp = header.replaceAll( "\r", "\\\\r" ).replaceAll( "\n", "\\\\n" );

								logger.log( "HTTP: no match for " + url );
								logger.log( "    header: " + temp );
								logger.log( "    decoded: " + headers );
								
								return( false );
								
							}else{
													
								String	host = (String)headers.get( "host" );
								
								if ( host == null ){
									
									host = getLocalIP();
									
								}else{
									
									int	pos = host.indexOf(':');
									
									if ( pos != -1 ){
										
										host	= host.substring(0,pos);
									}
									
									host	= host.trim();
								}
								
								String	client = (String)headers.get( "user-agent" );
								
								if ( client == null ){
									
									client = request.getClientAddress();
									
								}else{
									
									client = client + ":" + request.getClientAddress();
								}
								
								try{
										// Philips steamium sends invalid XML (trailing 0 byte screws the parser) so
										// sanitise it here. Note it has to be in utf-8 as this is part of the UPnP 
										// spec.
									
									String	action_content = "";
									
									byte[]	buffer = new byte[1024];
									
									InputStream	is = request.getInputStream();
									
									while( true ){
										
										int	len = is.read( buffer );
										
										if ( len < 0 ){
											
											break;
										}
										
										action_content += new String( buffer, 0, len, "UTF-8" );
									}
									
									action_content	= action_content.trim();
									
									String	result;
									
									try{
										result = 
											processAction(
													host,
													client,
													url,
													action,
													plugin_interface.getUtilities().getSimpleXMLParserDocumentFactory().create( 
															action_content ),
													filters );
									}catch( Throwable e ){
										
										logger.log( "Failed to process action '" + action_content + "' - " + Debug.getNestedExceptionMessage(e));
										
										result = null;
									}
									
									if ( result == null ){
										
											// not implemented
										
										response.setReplyStatus( 501 );
										
									}else{
									
										response.setHeader( "SERVER", getServerName());
										
										response.useStream( "xml", new ByteArrayInputStream( result.getBytes( "UTF-8" )));
									}
									
									return( true );
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
									
									logger.log( e );
									
									return( false );
								}
							}
						}
					});
				
				ssdp_listener = 
						new UPnPSSDPListener()
						{
							public void
							receivedResult(
								NetworkInterface	network_interface,
								InetAddress			local_address,
								InetAddress			originator,
								String				USN,
								URL					location,
								String				ST,
								String				AL )
							{
								// System.out.println( "receivedResult: " + location + "/" + ST + "/" + AL );
							}
							
							public void
							receivedNotify(
								NetworkInterface	network_interface,
								InetAddress			local_address,
								InetAddress			originator,
								String				USN,
								URL					location,
								String				NT,
								String				NTS )
							{
								// System.out.println( "receivedNotify: " + USN + "/" + location + "/" + NT + "/" + NTS );					
							}
	
							public String[]
							receivedSearch(
								NetworkInterface	network_interface,
								InetAddress			local_address,
								InetAddress			originator,
								String				ST )
							{
								for (int i=0;i<upnp_entities.length;i++){
									
									if ( ST.equals( upnp_entities[i] )){
																					
										synchronized( logged_searches ){
											
											if ( !logged_searches.contains( originator )){
												
												logged_searches.add( originator );
												
												logger.log( "SSDP: search from " + originator );
											}
										}
										
										return( new String[]{ 
												UUID_rootdevice,
												"RootDevice.xml" });
									}
								}
								
								return( null );
							}
							
							public void
							interfaceChanged(
								NetworkInterface	network_interface )
							{	
							}
						};
				
				ssdp.addListener( ssdp_listener );

				sendAlive();
	
				upnp = UPnPFactory.getSingleton( adapter, null );
				
				upnp_listener = 
					new UPnPListener()
					{
						public boolean
						deviceDiscovered(
							String		USN,
							URL			location )
						{
							return( true );
						}
						
						public void
						rootDeviceFound(
							UPnPRootDevice		device )
						{
							if ( device.getDevice().getDeviceType().equals("urn:schemas-upnp-org:device:MediaRenderer:1")){
									
								UPnPMediaRendererRemote new_renderer = null;
								
								synchronized( renderers ){
									
									Iterator<UPnPMediaRenderer> it = renderers.iterator();
									
									while( it.hasNext()){
										
										UPnPMediaRenderer renderer = it.next();
										
										if ( renderer instanceof UPnPMediaRendererRemote ){
											
											if (((UPnPMediaRendererRemote)renderer).getDevice().getUSN().equals( device.getUSN())){
												
												it.remove();
												
												renderer.destroy();
											}
										}
									}
									
									new_renderer = new UPnPMediaRendererRemote( UPnPMediaServer.this, device );
									
									renderers.add( new_renderer );
								}
								
								if ( unloaded && new_renderer != null ){
									
									new_renderer.destroy();
								}
							}
						}
					};
				
				upnp.addRootDeviceListener( upnp_listener );

				if ( http_enable_param.getValue()){
					
					http_server = new UPnPMediaServerHTTP( this, http_port_param.getValue());
											
					handleUPnP(http_port_param.getValue(), "Media Server HTTP", http_port_upnp_param.getValue());
				}
				
				timer2_event = timer2.addPeriodicEvent(
						60*1000,
						new UTTimerEventPerformer()
						{
							public void
							perform(
								UTTimerEvent		event )
							{
								sendAlive();
							}
						});
				
				timer3_event = timer3.addPeriodicEvent(
						2*1000,
						new UTTimerEventPerformer()
						{
							int	ticks = 0;
							
							public void
							perform(
								UTTimerEvent		event )
							{
								ticks++;
								
								sendEvents( ticks % 30 == 0 );
								
								UITextField status = view_model.getStatus();
								
								UPnPMediaServerContentServer cs = content_server;
								
								status.setText( 
										"connections=" + (cs==null?0:content_server.getConnectionCount()) +
										", up=" + DisplayFormatters.formatByteCountToKiBEtc( UPnPMediaChannel.getTotalUp()) +
										" (" + DisplayFormatters.formatByteCountToKiBEtcPerSec( UPnPMediaChannel.getAverageUpSpeed()) + ")" );
							}
						});
				
			}else{
				
				createContent();
			}
		}catch( Throwable e ){
			
		}finally{
			
			startup_sem.releaseForever();
		}
	}
	
	protected void
	ensureStarted()
	{
			// we defer startup on init to help speed up AZ init. However, we can
			// get requests for content items before this delay has completed so
			// we need to force earlier init if needed
		
		if ( startup_sem.isReleasedForever()){
			
			return;
		}
		
		start();
		
		if ( !startup_sem.reserve( 30000 )){
			
			log( "Timeout waiting for startup" );
		}
	}
	
	protected boolean
	isUserSelectedContentPort()
	{
		return( stream_port != 0 );
	}
	
	protected UPnPMediaServerContentDirectory
	getContentDirectory()
	{
		return( content_directory );
	}
	
	protected boolean
	getApplyBindIPs()
	{
		return( apply_bind.getValue());
	}
	
	protected int
	getContentPort()
	{
		if ( stream_port != 0 ){
			
			return( stream_port );
		}
		
		PluginConfig	config = plugin_interface.getPluginconfig();
			
		return( config.getPluginIntParameter( "content_port", 0 ));
	}
	
	protected void
	setContentPort(
		int		port )
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		config.setPluginParameter( "content_port", port, true );
	}
	
	public String
	getLocalIP()
	{
		UPnPMediaServerContentServer cs = content_server;
		
		InetAddress bip = cs==null?null:cs.getBindIP();
		
		if ( bip == null ){
			
			return( "127.0.0.1" );
			
		}else{
			
			return( bip.getHostAddress());
		}
	}
	
	protected void
	createContent()
	{
		dm_listener =
			new DownloadManagerListener()
			{
				public void
				downloadAdded(
					Download	download )
				{		
					if ( download.getTorrent() == null ){
						
						return;
					}
					
					content_directory.addContent( download );
				}
				
				public void
				downloadRemoved(
					Download	download )
				{
					if ( download.getTorrent() == null ){
						
						return;
					}
					
					content_directory.removeContent( download );
				}
			};
		
		plugin_interface.getDownloadManager().addListener( dm_listener );
	}
	

	


	
	protected String
	processAction(
		String						host,
		String						client,
		String						url,
		String						action,
		SimpleXMLParserDocument		doc,
		List<AzureusContentFilter>	filters )
	{
		int	client_type = UPnPMediaServerContentDirectory.CT_UNKNOWN;
				
		String mp_prefix = "windows-media-player/";
		
		int	pos = client.toLowerCase().indexOf( mp_prefix );
		
		if ( pos != -1 ){
			
			try{
				String ver = client.substring( pos + mp_prefix.length());
				
				pos = ver.indexOf(' ');
				
				if ( pos != -1 ){
				
					ver = ver.substring( 0, pos ).trim();
				}
				
				pos = ver.indexOf( '.' );
				
				if ( pos != -1 ){
					
					ver = ver.substring( 0, pos );
				}
				
				int	mp_version = Integer.parseInt( ver );
				
				if ( mp_version >= 12 ){
					
					client_type = UPnPMediaServerContentDirectory.CT_MEDIA_PLAYER;
				}
			}catch( Throwable e ){
				
			}
		}
		
		if ( client_type == UPnPMediaServerContentDirectory.CT_UNKNOWN ){
			
			client_type = 
				client.toLowerCase().startsWith( "xbox" )?
						UPnPMediaServerContentDirectory.CT_XBOX:
						UPnPMediaServerContentDirectory.CT_DEFAULT;
		}
		
		String	xmlns;
		
		boolean	directory;
		
		if ( url.equals( "/ContentDirectory/Control" )){
			
			xmlns	= "urn:schemas-upnp-org:service:ContentDirectory:1";
			
			directory	= true;
			
		}else{
			
			xmlns	= "urn:schemas-upnp-org:service:ConnectionManager:1";
			
			directory = false;
		}
		
		// System.out.println( "Process action '" + action + "'" );
		// doc.print();
		
		SimpleXMLParserDocumentNode	body = doc.getChild( "Body" );

		SimpleXMLParserDocumentNode	call = body.getChildren()[0];
		
		String	command = call.getName();				
		
		SimpleXMLParserDocumentNode[]	args = call.getChildren();
		
		Map	arg_map = new HashMap();
		
		String	arg_str = "";
		
		for (int i=0;i<args.length;i++){
			
			SimpleXMLParserDocumentNode	arg = args[i];
			
			arg_map.put( arg.getName(), arg.getValue());
			
			arg_str += (i==0?"":",") + arg.getName() + " -> " + arg.getValue();
		}
	
		logger.log( "Action (client=" + client + "): " + url + ":" + command +", arg=" + arg_str );
		
		resultEntries	result = new resultEntries();
		
		if ( directory ){
			
			if ( command.equals( "GetSearchCapabilities")){
				
				getSearchCapabilities( arg_map, result );
				
			}else if ( command.equals( "GetSortCapabilities" )){
				
				getSortCapabilities( arg_map, result );
	
			}else if ( command.equals( "GetSystemUpdateID" )){
	
				getSystemUpdateID( arg_map, result);
	
			}else if ( command.equals( "Browse" )){
	
				int num = browseOrSearch( host, arg_map, false, client_type, filters, result );
				
				logger.log( "    -> " + num + " entries" );
				
			}else if ( command.equals( "Search" )){
				
				int num = browseOrSearch( host, arg_map, true, client_type, filters, result );
				
				logger.log( "    -> " + num + " entries" );
				
			}else{
				
				return( null );
			}
		}else{
			
			if ( command.equals( "GetProtocolInfo")){

				getProtocolInfo( arg_map, result );
				
			}else if ( command.equals( "GetCurrentConnectionIDs")){

				getCurrentConnectionIDs( arg_map, result );
				
			}else if ( command.equals( "GetCurrentConnectionInfo")){

				getCurrentConnectionInfo( arg_map, result );
				
			}else{
				
				return( null );
			}
		}
		
		String	response =
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
			"<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"+
				"<s:Body>";
				
			// todo we should support faults here for errors - see http://www.upnp.org/specs/arch/UPnP-DeviceArchitecture-v1.0.pdf page 50
		
		response += "<u:" + command + "Response xmlns:u=\"" + xmlns + "\">";
			
		List	result_args = result.getArgs();
		List	result_vals	= result.getValues();
		
		for (int i=0;i<result_args.size();i++){
			
			String	arg = (String)result_args.get(i);
			String	val	= (String)result_vals.get(i);
			
			response += "<" + arg + ">" + val + "</" + arg + ">";
		}
		
		response += "</u:" + command + "Response>";

		response += "</s:Body>"+
					"</s:Envelope>";
		
		//System.out.println(response.replaceAll(">", ">\n") + "\n\n\n");
		
		return( response );
	}
	
	protected void
	getProtocolInfo(
		Map				args,
		resultEntries	result )
	{
		result.add( "Source", CM_SOURCE_PROTOCOLS );
		result.add( "Sink", CM_SINK_PROTOCOLS );
	}
	
	protected void
	getCurrentConnectionIDs(
		Map				args,
		resultEntries	result )
	{
		result.add( "ConnectionIDs", "" );
	}
	
	protected void
	getCurrentConnectionInfo(
		Map				args,
		resultEntries	result )
	{
		result.add( "RcsID", "0" );
		result.add( "AVTransportID", "0" );
		result.add( "ProtocolInfo", "" );
		result.add( "PeerConnectionManager", "" );
		result.add( "PeerConnectionID", "-1" );
		result.add( "Direction", "Input" );
		result.add( "Status", "Unknown" );
	}
	
	protected void
	getSearchCapabilities(
		Map				args,
		resultEntries	result )
	{
		result.add( "SearchCaps", "" );
	}
	
	protected void
	getSortCapabilities(
		Map				args,
		resultEntries	result )
	{
		result.add( "SortCaps", "dc:title" );
	}
	
	
	protected void
	getSystemUpdateID(
		Map				args,
		resultEntries	result )
	{
		result.add( "Id",String.valueOf( content_directory.getSystemUpdateID()) );
	}
	
	protected int
	browseOrSearch(
		String						host,
		Map							args,
		boolean						is_search,
		int							client_type,
		List<AzureusContentFilter>	az_filters,
		resultEntries				result )
	{
		Map<String,Object> az_filter_args = new HashMap<String, Object>();
		
		String	oid				= (String)args.get( "ObjectID" );
		String	browseFlag 		= (String)args.get( "BrowseFlag" );
		String	searchCriteria 	= (String)args.get( "SearchCriteria" );
		String	sortCriteria	= (String)args.get( "SortCriteria" );
		String	start_str		= (String)args.get( "StartingIndex" );
		String	request_str		= (String)args.get( "RequestedCount" );

		if ( oid == null ){	
			
			if ( client_type == UPnPMediaServerContentDirectory.CT_XBOX ){
				
					// xbox uses ContainerID and fixed values: 15 = video
				
				String container_id = (String)args.get( "ContainerID" );
				
				if ( container_id != null ){
					
					if ( container_id.equals( "15" )){
					
						oid = String.valueOf( content_directory.getMoviesContainer().getID());
						
					}else if ( container_id.equals( "16" )){
						
						oid = String.valueOf( content_directory.getPicturesContainer().getID());
						
					}else if ( 	container_id.equals( "4" ) || 
								container_id.equals( "5" ) || 
								container_id.equals( "6" ) || 
								container_id.equals( "7" ) || 
								container_id.equals( "F" )){ 
						
						oid = String.valueOf( content_directory.getMusicContainer().getID());
						
					}else{
						
						oid = container_id;
					}
				}
			}else if ( client_type == UPnPMediaServerContentDirectory.CT_MEDIA_PLAYER ){
											
				if ( searchCriteria != null ){
					
					if ( searchCriteria.contains( "object.item.imageItem" )){
					
						oid = String.valueOf( content_directory.getPicturesContainer().getID());
					
					}else if ( searchCriteria.contains( "object.item.audioItem" )){
						
						oid = String.valueOf( content_directory.getMusicContainer().getID());

					}else{
						
						oid = String.valueOf( content_directory.getMoviesContainer().getID());
					}
				}
			}
		}
		
		if ( oid == null ){
			
			log( "'oid' missing from request, using root" );
			
			oid = String.valueOf( content_directory.getRootContainer().getID());
		}
		
		
		if ( is_search ){
			
			if ( browseFlag == null ){
				
				browseFlag = "BrowseDirectChildren";
			}
		}
		
		int	start_index	 	= start_str==null?0:Integer.parseInt( start_str );
		int	requested_count	= request_str==null?0:Integer.parseInt( request_str );
		
			// value of 0 -> all
		
		if ( requested_count == 0 ){
			
			requested_count = Integer.MAX_VALUE;
		}
		
		String	didle 			= "";
		int		num_returned	= 0;
		int		total_matches	= 0;
		int		update_id		= content_directory.getSystemUpdateID();
		
		UPnPMediaServerContentDirectory.content	cont = content_directory.getContentFromID( oid.length()==0?0:Integer.parseInt(oid ));
		
		
		if ( cont == null ){
			
				// todo error case
			
			log( "Object '" + oid + "' not found" );
			
		}else{
			
			if ( cont instanceof UPnPMediaServerContentDirectory.contentContainer ){
				
				update_id = ((UPnPMediaServerContentDirectory.contentContainer)cont).getUpdateID();
			}
			
			if ( browseFlag.equals( "BrowseMetadata")){
						
				didle = content_directory.getDIDL( cont, host, client_type, az_filters, az_filter_args );
				
				num_returned	= 1;
				total_matches	= 1;
				
			}else if ( browseFlag.equals( "BrowseDirectChildren")){
				
				boolean	ok = false;
				
				if ( enable_lan_publish || az_filters.size() > 0 ){
					
					ok = true;
					
				}else{
					
					try{
						ok = InetAddress.getByName( host ).isLoopbackAddress();
						
					}catch( Throwable e ){
						
						logger.log( e );
					}
				}
				
				if ( ok ){
					
					contentContainer	container = (contentContainer)cont;
	
					List<content>	children = container.getChildren();
					
						// todo: sort criteria
					
					sortContent( children );
					
					int	added 	= 0;
					int visible	= 0;
					
					for (int i=0;i<children.size();i++){
						
						content	child = children.get(i);
						
						boolean vis = isVisible( child, az_filters, az_filter_args );
						
						// System.out.println( child.getName() + " -> " + vis );
						
						if ( vis ){
							
							visible++;
					
							if ( start_index > 0 ){
								
								start_index--;
								
							}else if ( added < requested_count ){					
								
								didle += content_directory.getDIDL( child, host, client_type, az_filters, az_filter_args ); 
								
								added++;
							}
						}
					}
				
					num_returned	= added;
					total_matches	= visible;
					
				}else{
					
					num_returned	= 0;
					total_matches	= 0;
				}
			}
		}
		
			// always return the DIDL element as if we don't this borks PS3 for empty folders
		//if ( didle.length() > 0 ){
			
			didle = 
				"<DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " + 
				"xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" " +
				"xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\">" +
				
				didle + 
				"</DIDL-Lite>";
		//}
			
			//System.out.println(didle.replaceAll("><", ">\n<") + "\n\n\n");

		//	System.out.println( didle ); 
		result.add( "Result", didle );
		
		result.add( "NumberReturned", "" + num_returned );
		result.add( "TotalMatches", "" + total_matches );
		result.add( "UpdateID", "" + update_id  );
		
		return( num_returned );
	}
	
	protected void
	sortContent(
		List<content>		content )
	{
		final int		order 	= Integer.parseInt( sort_order.getValue());
		final boolean 	asc		= sort_order_ascending.getValue();
		
		Collections.sort(
			content,
			new Comparator<content>()
			{
				private Comparator<String> alpha_comp = plugin_interface.getUtilities().getFormatters().getAlphanumericComparator( true );
				
				public int 
				compare(
					content o1, 
					content o2 )
				{
					int	result = 0;
					
					boolean	reverse_order = order != 0 && !asc;
					
					if ( order == 2 ){
						
						long l1 = o1.getDateMillis();
						long l2 = o2.getDateMillis();
						
						if ( l2 > l1 ){
							
							result = -1;
							
						}else if ( l1 > l2 ){
							
							result = 1;
							
						}else{
						
								// date same - fall through to alpha but always ascending alpha
								// otherwise things look silly (especially for folders at top level)
							
							reverse_order = false;
						}
					}
					
					if ( result == 0 ){
					
						result = alpha_comp.compare( o1.getName(), o2.getName());
					}
										
						// only handle ascending for non-default order
					
					if ( reverse_order ){
						
						result = -result;
					}
					
					return( result );
				}
			});
	}
	
	protected boolean
	isVisible(
		content						c,
		List<AzureusContentFilter>	filters,
		Map<String,Object>			filter_args )
	{
		boolean	vis = false;
		
		if ( filters.size() == 0 ){
			
			vis = true;
			
		}else{

			if ( c instanceof contentItem ){
					
				AzureusContentFile acf = ((contentItem)c).getACF();
				
				for ( AzureusContentFilter filter: filters ){
					
					if ( filter.isVisible(acf, filter_args )){
						
						vis	= true;
					}
				}
			}else{
				
				AzureusContentDownload acd = ((contentContainer)c).getACD();
				
				if ( acd == null ){
					
					vis = true;
					
				}else{
					
					for ( AzureusContentFilter filter: filters ){
						
						if ( filter.isVisible(acd, filter_args )){
							
							vis	= true;
						}
					}
				}
			}
		}
		
		return( vis );
	}
	
	protected String
	processEvent(
		boolean				content_directory,
		InetSocketAddress	client,
		Map					headers,
		boolean				subscribe,
		String				callback,
		String				sid,
		String				timeout )
	{
		// System.out.println( "ProcessEvent:" + client + "," + subscribe + "," + callback + "," + sid + "," + timeout );
		
		if ( subscribe ){
			
			/*
			 * HTTP/1.1 200 OK
				DATE: when response was generated
				SERVER: OS/version UPnP/1.0 product/version
				SID: uuid:subscription-UUID
				TIMEOUT: Second-actual subscription duration
			 */
			
			if ( sid == null ){
				
				sid = "uuid:" + UUIDGenerator.generateUUIDString();
			
				event ev = new event( client, callback, sid, content_directory );
				
				synchronized( events ){
				
					events.put( sid, ev );
					
					new_events.add( ev );
				}
				
				logger.log( "Event: Subscribe - " + ev.getString());
				
			}else{
					// renew
				
				synchronized( events ){
					
					event	ev = (event)events.get(sid);
					
					if ( ev == null ){
						
						System.out.println( "can't renew event subscription for " + sid + ", not found" );
						
					}else{
						
						ev.renew();
						
						logger.log( "Event: Renew - " + ev.getString());
					}
				}
			}
		

			headers.put( "DATE", TimeFormatter.getHTTPDate( SystemTime.getCurrentTime()));
			headers.put( "SID", sid );
			headers.put( "TIMEOUT", "Second-" + EVENT_TIMEOUT_SECONDS );
			
		}else{
			
			if ( sid != null ){
				
				synchronized( events ){
					
					event	ev = (event)events.remove( sid );
					
					if ( ev == null ){
						
						logger.log( "Event: Unsubscribe - unknown event " + sid );

					}else{
						
						logger.log( "Event: Unsubscribe - " + ev.getString());
					}
				}
			}
		}
		
		return( "" );
	}
	
	protected void
	stop()
	{
		sendDead();
	}
	
	
	protected void
	play(Object obj) 
	
		throws IPCException
	{
		Download				download;
		DiskManagerFileInfo		file;
		
		if ( media_server_ui == null ){
			
			throw( new IPCException( "Media server UI not bound" ));
		}
		
		if ( obj instanceof Download ){
		
			download = (Download)obj;

			file	= download.getDiskManagerFileInfo()[0];
		
		}else{
			
			file = (DiskManagerFileInfo)obj;
			
			try{
				download	= file.getDownload();
				
			}catch( DownloadException e ){	
				
				throw( new IPCException( "Failed to get download from file", e ));
			}
		}
		
		String	id = content_directory.createResourceID( download.getTorrent().getHash(), file );
		
		System.out.println( "play: " + id );

		UPnPMediaServerContentDirectory.contentItem item = content_directory.getContentFromResourceID( id );
		
		if ( item == null ){
			
			throw( new IPCException( "Failed to find item for id '" + id + "'" ));
		}
		
		String url = item.getURI( getLocalIP(), -1 );
		
		/* no point in this - it'll match the "." in the host name...
		int lastDot = url.lastIndexOf(".");
		
		if ( lastDot == -1 ){
			
			throw( new IPCException( "For an item to be playable it has to have a file extension (" + url + ")" ));
		}
		*/
		
			//String extension = url.substring(lastDot);
			//Program program = Program.findProgram(".asx");
    
		try {
			String user_dir = plugin_interface.getUtilities().getAzureusUserDir();
       
			File playList;
			String playCode;
			
			String content_type = item.getContentType();
			boolean isQuickTime = content_type.equals("video/quicktime");
			boolean forceWMP = false;
			if ((plugin_interface.getUtilities().isOSX() && (content_type.equals("video/mpeg")
					|| isQuickTime || content_type.equals("video/x-ms-wmv")))
					|| (isQuickTime && quickTimeAvail)) {
				
				File playLists = new File(user_dir, "playlists");
				if(!playLists.exists()) playLists.mkdir();
				String fileName = "azplay" + (SystemTime.getCurrentTime() / 1000) + ".qtl";
				playList = new File( playLists, fileName);
				
				playCode = "<?xml version=\"1.0\"?>" +
						"<?quicktime type=\"application/x-quicktime-media-link\"?>" +
						"<embed autoplay=\"true\" fullscreen=\"full\" moviename=\"" + item.getTitle() + "\" " +
						"src=\"" + url + "\" />";
			} else {
				playList = new File( user_dir, "azplay.asx");
				playCode = "<ASX version = \"3.0\">" +
				"<Entry>" +
				"<Ref href = \"" + url + "\" />" +
				"</Entry>" +
				"</ASX>";
				forceWMP = Constants.isWindows;
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(playList));
			
			
			bw.write(playCode);
			
			bw.close();
			
			//program.execute(playList.getAbsolutePath());

			if (forceWMP) {
				media_server_ui.runInMediaPlayer( playList.getAbsolutePath(), true );
			} else {
				media_server_ui.play( playList );
			}
			
		} catch(Exception e) {
			
			throw( new IPCException( "Play operation failed", e ));
		}							
	}
	
	protected void
	sendAlive()
	{
		synchronized( this ){
			
			if ( alive ){
		
				sendNotify( "ssdp:alive" );
			}
		}
	}
	
	protected void
	sendDead()
	{
		synchronized( this ){

			alive	= false;
			
			sendNotify( "ssdp:byebye" );
		}
	}
	
	protected void
	sendNotify(
		String	status )
	{
		if ( ssdp != null ){
			
			for (int i=0;i<upnp_entities.length;i++){
				
				ssdp.notify( upnp_entities[i], status, upnp_entities[i]==UUID_rootdevice?null:UUID_rootdevice, "RootDevice.xml" );
			}
		}
	}
	
	protected void
	contentContainerUpdated(
		UPnPMediaServerContentDirectory.contentContainer		c )
	{
		synchronized( events ){
			
			if ( container_update_events == null ){
				
				container_update_events	= new HashSet();
			}
			
			container_update_events.add( c );
		}
	}
	
	protected void
	sendEvents(
		boolean	check_timeouts )
	{
		long	now = plugin_interface.getUtilities().getCurrentSystemTime();
		
		List	new_dir_targets = new ArrayList();
		List	new_con_targets = new ArrayList();

		synchronized( events ){
			
			Iterator	it = new_events.iterator();
			
			while( it.hasNext()){
				
				event	ev = (event)it.next();
				
				if ( ev.getStartTime() > now || now - ev.getStartTime() > 1000 ){
					
					it.remove();
										
					if ( ev.isContentDirectory()){
						
						new_dir_targets.add( ev );
						
					}else{
						
						new_con_targets.add( ev );
					}
				}
			}
			
			if ( check_timeouts ){
				
				it = events.values().iterator();
				
				while( it.hasNext()){
					
					event ev = (event)it.next();
					
					long	t = ev.getStartTime();
					
					if ( t > now ){
						
						ev.renew();
						
					}else if ( now - t > EVENT_TIMEOUT_SECONDS*1000 + 10*60*1000 ){
						
						logger.log( "Event: Timeout - " + ev.getString());
						
						it.remove();
					}
				}
			}
		}
		
		if ( new_dir_targets.size() > 0 ){

			sendDirEvents( new_dir_targets, ""+content_directory.getRootContainer().getUpdateID(), "", "" );
		}
		
		if ( new_con_targets.size() > 0 ){

			sendConEvents( new_con_targets );
		}
		
		Set		to_send;
		List	targets;
		
		synchronized( events ){
			
			to_send = container_update_events;
		
			container_update_events	= null;
			
			if ( to_send == null || to_send.size() == 0 || events.size() == 0 ){
				
				return;
			}
			
			targets = new ArrayList( events.values());
		}
	
			// currently only really support content directory events
		
		Iterator	it = targets.iterator();
		
		while( it.hasNext()){
			
			event	ev = (event)it.next();
			
			if ( !ev.isContentDirectory()){
				
				it.remove();
			}
		}
		
		if ( targets.size() == 0 ){
			
			return;
		}
		
		it = to_send.iterator();
		
		String	system_update 		= null;
		String	container_update	= null;
			
		while( it.hasNext()){
			
			UPnPMediaServerContentDirectory.contentContainer	container = (UPnPMediaServerContentDirectory.contentContainer)it.next();
			
			int	id 			= container.getID();
			int	update_id	= container.getUpdateID();
			
			if ( container.getParent() == null ){
				
				system_update = "" + update_id;
			}
			
			if ( container_update == null ){
				container_update = "";
			}
			
			container_update += (container_update.length()==0?"":",") + id + "," + update_id;
		}
		
		sendDirEvents( targets, system_update, container_update, null );
	}
	
	protected void
	sendDirEvents(
		List	targets,
		String	system_update,
		String	container_update,
		String	transfer_ids )
	{
		String	xml =
			"<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">";
		
		if ( system_update != null ){
			xml += 
				"<e:property>"+
					"<SystemUpdateID>" + system_update + "</SystemUpdateID>" +
				"</e:property>";
		}
		
		if ( container_update != null ){
			xml += 
				"<e:property>"+
					"<ContainerUpdateIDs>" + container_update + "</ContainerUpdateIDs>" +
				"</e:property>";
		}
		
		if ( transfer_ids != null ){
			xml += 
				"<e:property>"+
					"<TransferIDs>" + "" + "</TransferIDs>" +
				"</e:property>";
		}
		
		xml += "</e:propertyset>";
		  	
		try{
			byte[]	xml_bytes = xml.getBytes( "UTF-8" );
			
			Iterator it = targets.iterator();
			
			while( it.hasNext()){
				
				event	ev = (event)it.next();
				
				ev.sendEvent( xml_bytes );
			}	
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	sendConEvents(
		List	targets )
	{
		String	xml =
			"<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">";
		
		xml += 
			"<e:property>"+
				"<SourceProtocolInfo>" + CM_SOURCE_PROTOCOLS + "</SourceProtocolInfo>" +
			"</e:property>";
		
		xml += 
			"<e:property>"+
				"<SinkProtocolInfo>" + CM_SINK_PROTOCOLS + "</SinkProtocolInfo>" +
			"</e:property>";
	
		xml += 
			"<e:property>"+
				"<CurrentConnectionIDs>" + "" + "</CurrentConnectionIDs>" +
			"</e:property>";

		
		xml += "</e:propertyset>";
		  	
		try{
			byte[]	xml_bytes = xml.getBytes( "UTF-8" );
			
			Iterator it = targets.iterator();
			
			while( it.hasNext()){
				
				event	ev = (event)it.next();
				
				ev.sendEvent( xml_bytes );
			}	
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
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
		// str = str.replaceAll( "\"", "&quot;" );	breaks Philips streamium...
		
		return( str );
	}
	
	
	protected class
	resultEntries
	{
		private List		names	= new ArrayList();
		private List		values	= new ArrayList();
		
		public void
		add(
			String		name,
			String		value )
		{
			names.add( name );
			values.add( escapeXML( value ));
		}
		
		public List
		getArgs()
		{
			return( names );
		}
		
		public List
		getValues()
		{
			return( values );
		}
	}
	
	protected class
	event
	{
		private static final String	NL			= "\015\012";
		
		private boolean content_directory;
		private String	sid;
		private long	create_renew_time;
		private long	event_seq;
		
		private List	callbacks	= new ArrayList();
		
		protected
		event(
			InetSocketAddress	address,
			String				callback,
			String				_sid,
			boolean				_content_directory )
		{
			sid					= _sid;
			content_directory	= _content_directory;
			
			create_renew_time	= plugin_interface.getUtilities().getCurrentSystemTime();
			
			StringTokenizer	tok = new StringTokenizer( callback, ">" );
			
			while( tok.hasMoreTokens()){
				
				String	c = tok.nextToken().trim().substring(1);
				
				if ( !c.toLowerCase().startsWith( "http" )){
					
					if ( c.startsWith( "/" )){
						
						c = c.substring(1);
					}
					
					c = "http://" + address.getAddress().getHostAddress() + ":" + address.getPort() + "/" + c;				
				}
				
				try{
					URL	url = new URL( c );
					
					callbacks.add( url );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		protected boolean
		isContentDirectory()
		{
			return( content_directory );
		}
		
		protected void
		renew()
		{
			create_renew_time	= plugin_interface.getUtilities().getCurrentSystemTime();
		}
		
		protected long
		getStartTime()
		{
			return( create_renew_time );
		}
		
		protected String
		getString()
		{	
			String	cb = "";
			
			for (int i=0;i<callbacks.size();i++){
				
				cb += (i==0?"":",") + callbacks.get(i);
			}
			
			return( "sid=" + sid + ",callbacks=" + cb + ",seq=" + event_seq + ",cd=" + content_directory );
		}
		
		protected void
		sendEvent(
			byte[]	bytes )
		{
			/*
			NOTIFY delivery path HTTP/1.1
			HOST: delivery host:delivery port
			CONTENT-TYPE: text/xml
			CONTENT-LENGTH: Bytes in body
			NT: upnp:event
			NTS: upnp:propchange
			SID: uuid:subscription-UUID
			SEQ: event key
			*/
			
			for (int i=0;i<callbacks.size();i++){
				
				URL	url = (URL)callbacks.get(i);
				
				Socket	socket	= null;
				
				try{
					
					socket = new Socket();
					
					socket.setSoTimeout( 10000 );
					
					socket.connect( 
							new InetSocketAddress( 
									url.getHost(), 
									url.getPort()==-1?url.getDefaultPort():url.getPort()),
							10000 );
							
					OutputStream	os = socket.getOutputStream();
					
					write( os, "NOTIFY " + URLEncoder.encode(url.getPath(), "UTF-8") + " HTTP/1.1" + NL );
					write( os, "HOST: " + url.getHost() + ":" + socket.getPort() + NL );
					write( os, "CONTENT-TYPE: text/xml" + NL );
					write( os, "CONTENT-LENGTH: " + bytes.length + NL );
					write( os, "NT: upnp:event" + NL );
					write( os, "NTS: upnp:propchange" + NL );
					write( os, "SID: " + sid + NL );
					write( os, "SEQ: " + event_seq + NL + NL );
					
					os.write( bytes );
					
					os.flush();
					
					InputStream	is = socket.getInputStream();
					
					is.read();
					
					logger.log( "Event: sent to  " + url );
					
				}catch( Throwable e ){
					
					// e.printStackTrace();
					
				}finally{
					
					try{
						if ( socket != null ){
							
							socket.close();
						}
					}catch( Throwable e ){
					
					}
				}
			}
		
				// first event must be 0
			
			event_seq++;
		}
		
		protected void
		write(
			OutputStream	os,
			String			str )
		
			throws IOException
		{
			os.write( str.getBytes( "UTF-8" ));
		}
	}
}
