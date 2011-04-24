/*
 * Created on 17-Jun-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.parg.azureus.plugins.azhtmlwebui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.peers.PeerManagerStats;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.Formatters;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.ui.webplugin.WebPlugin;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import HTML.Template;

import com.aelitis.json.JsonDecoder;
import com.aelitis.json.JsonMapEncoder;


public class 
HTMLWebUIPlugin
	extends 	WebPlugin
{
	public static final String	PLUGIN_NAME				= "Azureus HTML Web Interface";
	public static final String	PLUGIN_ID				= "azhtmlwebui";

	public static final String	PLUGIN_LANG_RESOURCE 	= "org.parg.azureus.plugins.azhtmlwebui.internat.Messages";

	public static final int		DEFAULT_PORT	= 6886;
	
	public static final String	CONFIG_HTMLWEBUI_TITLE					= "HTML WebUI Title";
	public static final String	CONFIG_HTMLWEBUI_TITLE_DEFAULT			= "Azureus HTML WebUI";
	
	public static final String	CONFIG_HTML_WEBUI_3TABS					= "Show completed torrents in a separate tab";
	public static final boolean	CONFIG_HTML_WEBUI_3TABS_DEFAULT			= false;
	
	public static final String	CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE	= "Number of Items per page";
	public static final int CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE_DEFAULT = 0;
	
	public static final String		CONFIG_CSS				= "HTML WebUI CSS";
	public static final String[]	CONFIG_CSS_BUILTIN		= { "theme.css", "hs_theme.css", "oldtheme.css" };
	public static final String		CONFIG_CSS_DEFAULT		= "theme.css";
	
	public static final String	CATEGORY_UNKNOWN	= "Uncategorized";
	
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	private PluginInterface		plugin_interface;
	private PluginInterface		tracker_plugin_interface;
	private PluginConfig		plugin_config;
	private PluginConfig		tracker_plugin_config;
	private DownloadManager		download_manager; 
	private Utilities			utilities;
	private LocaleUtilities 	locale_utils;
	private Formatters			formatters;
	private LoggerChannel		log;
	
	private TrackerWebPageRequest	pageRequest;
	
	protected boolean	view_mode;
	protected boolean	bCompletedTab;
	
	protected Tracker 			tracker;
	
	protected Comparator		comparator;
	
	protected TorrentAttribute	torrent_categories;
	
	protected String[][]		t_uploaded_names = new String[3][2];
	
	protected static final String[]		welcome_pages = {"index.html", "index.htm", "index.php", "index.tmpl" };
	protected static File[]				welcome_files;
	protected String				file_root;
	
	private static Properties	properties = new Properties();
	
	private int 				refresh_rate	=	30;
	private int					minSR;
	
	private Hashtable			torrentsHostedorPublished	= new Hashtable();
	
	private boolean 			debugOn;
	
	private boolean				tracker_plugin_loaded, tracker_enabled, tracker_web_enabled = false;
	
	
	public
	HTMLWebUIPlugin()
	{
		super( properties );
	}
	
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	
		throws PluginException
	{	
		
		
		plugin_interface	= _plugin_interface;
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{	
					try {
						tracker_plugin_interface = plugin_interface.getPluginManager().getPluginInterfaceByClass( "org.gudy.azureus2.ui.tracker.TrackerWebDefaultTrackerPlugin" );
						
						if( tracker_plugin_interface != null ) { //will be null if 'azplugins' plugin isn't installed
						
							tracker_plugin_config = tracker_plugin_interface.getPluginconfig();
						
							tracker_plugin_loaded  = true;
						}
						
					}catch(Exception e){
						e.printStackTrace();
					}
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
	
		plugin_config		= plugin_interface.getPluginconfig();
		
		utilities = plugin_interface.getUtilities();
		
		formatters = utilities.getFormatters();
		
		comparator	= formatters.getAlphanumericComparator( true );
		
		download_manager	= plugin_interface.getDownloadManager();
		
		tracker = plugin_interface.getTracker();
		
		locale_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		locale_utils.integrateLocalisedMessageBundle( PLUGIN_LANG_RESOURCE );

		String plugin_name = locale_utils.getLocalisedMessageText( "azhtmlwebui.name" );
		
		log	= plugin_interface.getLogger().getChannel( plugin_name );
		
		TorrentAttribute[]	tas = plugin_interface.getTorrentManager().getDefinedAttributes();
		
		for (int i=0;i<tas.length;i++){
			
			if( tas[i].getName() == TorrentAttribute.TA_CATEGORY ){
				
				torrent_categories = tas[i];
				
				break;
			}
		}
		///////////////////////////////
		file_root = utilities.getAzureusUserDir() + File.separator + "htmlwebui";

		welcome_files = new File[welcome_pages.length];
		
		for (int i=0;i<welcome_pages.length;i++){
			
			welcome_files[i] = new File( file_root + File.separator + welcome_pages[i] );
		}
		//////////////////////////////
		UIManager	ui_manager = plugin_interface.getUIManager();
		
			// config
	
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azhtmlwebui.name");
					
		config_model.addStringParameter2( CONFIG_HTMLWEBUI_TITLE, "azhtmlwebui.title", CONFIG_HTMLWEBUI_TITLE_DEFAULT );
		
		config_model.addBooleanParameter2(CONFIG_HTML_WEBUI_3TABS, "azhtmlwebui.completedtab", CONFIG_HTML_WEBUI_3TABS_DEFAULT);
		
		config_model.addIntParameter2(CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE, "azhtmlwebui.pagination.per_page", CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE_DEFAULT);
		
		String[]	css_list = getAvailableCSS();
		
		config_model.addStringListParameter2( CONFIG_CSS, "azhtmlwebui.css", css_list, CONFIG_CSS_DEFAULT );
		
			// log panel
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( plugin_name );

		properties.put( PR_PORT, new Integer( DEFAULT_PORT ));
		properties.put( PR_LOG, log );
		properties.put( PR_CONFIG_MODEL, config_model );
		properties.put( PR_VIEW_MODEL, view_model );
		properties.put( PR_HIDE_RESOURCE_CONFIG, new Boolean( true ));
		properties.put( PR_PAIRING_SID, "jswebui" );
		
		super.initialize( plugin_interface );
		
	}
	
	public String
	getName()
	{
		return( PLUGIN_NAME );
	}
	
	public String
	getLocalisedMessageText(
		String	resource )
	{
		return( locale_utils.getLocalisedMessageText( resource ));
	}
	
	public String
	getMessageText(
		String	resource )
	{
		return( locale_utils.getLocalisedMessageText( resource ).replaceAll("\n", "<br>").replaceAll("&", ""));
	}
	
	// Map the Homepage URL.
	protected String mapHomePage( String url_in )
	{
	  if ( url_in.equals("/")){
		for (int i=0;i<welcome_files.length;i++){
		  if ( welcome_files[i].exists()){
			url_in = "/" + welcome_pages[i];
			return (url_in);
		  }
		}
	  }
	  return (url_in);
	}
	
	protected Hashtable
	decodeParams(
		String	str )
	{
		Hashtable	params = new Hashtable();
		
		int	pos = 0;
		
		while(true){
		
			int	p1 = str.indexOf( '&', pos );
			String	bit;
			
			if ( p1 == -1 ){
				
				bit = str.substring(pos);
				
			}else{
				
				bit = str.substring(pos,p1);
				pos = p1+1;
			}
			
			int	p2 = bit.indexOf('=');
			
			if ( p2 == -1 ){
			
				params.put(bit,"true");
				
			}else{
				params.put(bit.substring(0,p2), bit.substring(p2+1));
				
			}
			
			if ( p1 == -1 ){
				
				break;
			}
		}
		
		return( params );
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		
		debugOn = false;
		
		OutputStream	os = response.getOutputStream();
		
		try {
			
		pageRequest = request;
		
		String url = request.getURL();
		
		if ( url.equals("/")){
			
			url = "/index.tmpl";

		}
		Hashtable	params = null;
		
		url = mapHomePage( url );
		
		int	p_pos = url.indexOf( '?' );
		
		if ( p_pos != -1 ){
			
			params = decodeParams( url.substring( p_pos+1 ));
			
			url = url.substring(0,p_pos);
		}	
		/////////////////////////////////////////////////////////////
		String	target = file_root + url.replace('/',File.separatorChar);
		File canonical_file = new File(target).getCanonicalFile();
		
		if ( useFile(canonical_file)){
			
			String str = canonical_file.toString().toLowerCase();
			
			int	pos = str.lastIndexOf( "." );
			
			if ( pos == -1 ){
				
				return( false );
			}
			
			String	file_type = str.substring(pos+1);
			
			if ( file_type.equals("php") || file_type.equals("tmpl")){
			
				Hashtable	args = new Hashtable();
			
				args.put( "filename", canonical_file.toString());
				
				return( handleTemplate( url, params, args, os ));
				
			} else if(file_type.equals("ajax")){
				return handleAjax( url, params, os , response);
			} else if(file_type.equals("upload")) {
				return handleUpload( url, params, os, response);
			}else{ 
			
				
				FileInputStream	fis = null;
				
				try{
					fis = new FileInputStream(canonical_file);
					
					response.useStream( file_type, fis );
					
					return( true );
					
				}finally{
					
					if ( fis != null ){
						
						fis.close();
					}
				}
			}
		}
		////////////////////////////////////////////////////////////////
		InputStream is = HTMLWebUIPlugin.class.getClassLoader().getResourceAsStream("org/parg/azureus/plugins/azhtmlwebui/templates" + url );
		
		if ( is == null ){
			
			return( false );
		}
		
		try{
			int	pos = url.lastIndexOf( "." );
			
			if ( pos == -1 ){
				
				return( false );
			}
			String	file_type = url.substring(pos+1);
			
			if ( file_type.equals("php") || file_type.equals("tmpl")){
				
				Hashtable	args = new Hashtable();
				
				args.put( "filehandle", new InputStreamReader( is, DEFAULT_ENCODING ));

				return( handleTemplate( url, params, args, os ));
					
			}else if(file_type.equals("ajax")){
				return handleAjax( url, params, os, response);
			} else if(file_type.equals("upload")) {
				return handleUpload( url, params, os, response);
			}else{ 
											
				response.useStream( file_type, is );
				
				return( true );
			}	
		}finally{
											
			is.close();
		}
		
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			os.write( e.toString().getBytes());
			
			return( true );
		}

	}


	private boolean useFile(File canonical_file) {
		if ( !canonical_file.toString().startsWith( file_root )){
			if ( !canonical_file.toString().toLowerCase().startsWith( file_root.toLowerCase() ) ) {
				return( false );
			}
		}
		
		if ( canonical_file.isDirectory()){
			return( false );
		}
		
		if (canonical_file.canRead()) {
			return ( true );
		}
		
		return( false );
	}
	
	protected boolean 
	handleUpload(
			String page_url,
			Hashtable params,
			OutputStream os,
			TrackerWebPageResponse response) throws IOException
	{
		
		System.out.println("handling Upload!");
		
		try {
			
			String up_dir = utilities.getAzureusUserDir() + File.separator + "torrents" + System.getProperty("file.separator");
			
		    File dir = new File( up_dir );
		    if (!dir.exists()) {
		      dir.mkdirs();
		    }

			uploadTorrent(pageRequest, up_dir);
			
			String torrent_msg = "";
			
			/*for(int i=0; i<t_uploaded_names.length;i++){
				if(t_uploaded_names[i][0] != null) System.out.println(i + ".0: " + t_uploaded_names[i][0] + " - " + i + ".1: " + t_uploaded_names[i][1]);
			}*/
			for(int i=0; i<t_uploaded_names.length;i++){
				if(t_uploaded_names[i][0] != null){
					if(t_uploaded_names[i][1].equals("1")) {
						torrent_msg += escapeXML(t_uploaded_names[i][0]) + " " + 
										getLocalisedMessageText("template.upload.local.msg.success") + " " + up_dir + "<br/>";
					} else {
						torrent_msg = getLocalisedMessageText("template.upload.local.msg.error") + " " + escapeXML(t_uploaded_names[i][0]) + "<br/>";
					}
				}
			}
			//t.setParam("url_load_msg", torrent_msg );
			
			t_uploaded_names = new String[3][2];
			
		} catch (Exception e) {
			
			if(debugOn) System.out.println("Loading of local torrent failed");
			
			//t.setParam("url_load_msg", getLocalisedMessageText("template.upload.local.msg.error"));
			
			e.printStackTrace();
			
		}
		
		return true;
	}
	
	protected boolean
	handleAjax(
			String page_url,
			Hashtable params,
			OutputStream os,
			TrackerWebPageResponse response) throws IOException
	{
		String responseText="";
		
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, DEFAULT_ENCODING), true);
		
		if(debugOn) System.out.println("Ajax request: " + page_url + params.toString());
		
		if ( params != null ){
			
			String refresh = (String)params.get("refresh");
			String priority = (String)params.get("priority");
			String autoUseDefDir = null; //(String)params.get("autodir");
			
			String cat = (String)params.get("cat");
			
			String remove		= (String)params.get("rem");
			String delete		= (String)params.get("del");
			
			String act = (String)params.get("act");
			
			String update = (String)params.get("update");
			
			String display = (String)params.get("d");
			
			if(refresh != null) {
				int rate;
				try {
					rate = Integer.parseInt(refresh);
				} catch(Exception ex) {
					ex.printStackTrace();
					rate = refresh_rate;
				}
				
				refresh_rate = (rate>4) ? rate : 5;
				responseText = "" + refresh_rate;
				
			} else if(autoUseDefDir != null) {
				
				try{
					plugin_config.setBooleanParameter("Use default data dir", true);
					responseText = getLocalisedMessageText("template.upload.auto.on");
					
				} catch(Exception e) {
					responseText = getLocalisedMessageText("template.upload.auto.off");
					e.printStackTrace();
				}
				
			} else if(priority != null) {
				/*
				 * Priority:
				 * 0 : delete
				 * 1 : do not download
				 * 2 : normal
				 * 3 : high
				 */
				int p = Integer.parseInt(priority);
				String hash = (String)params.get("hash");
				int index = Integer.parseInt((String)params.get("index"));
				try{
				Download[] Torrents = download_manager.getDownloads(true);
				Download torrent = null;
				DiskManagerFileInfo dmFileInfo = null;

				for (int i=0; i < Torrents.length ;i++) {
					
					if (formatters.encodeBytesToString(Torrents[i].getTorrent().getHash()).equals(hash)) {
							
							torrent = Torrents[i];
					}
				}
				
				dmFileInfo = torrent.getDiskManagerFileInfo()[index-1];
				
				switch (p) {
				case 0:
					dmFileInfo.setPriority(false);
					dmFileInfo.setDeleted(true);
					break;
				case 1:
					dmFileInfo.setDeleted(false);
					dmFileInfo.setPriority(false);
					dmFileInfo.setSkipped(true);
					break;
				case 2:
					dmFileInfo.setDeleted(false);
					dmFileInfo.setSkipped(false);
					dmFileInfo.setPriority(false);
					break;
				case 3:
					dmFileInfo.setDeleted(false);
					dmFileInfo.setSkipped(false);
					dmFileInfo.setPriority(true);
					break;
				default:
					break;
				}
				
				if (debugOn) System.out.println(dmFileInfo.getFile() + " : " + dmFileInfo.isPriority());
				}catch (Exception e) {
					e.printStackTrace();
					p = 5;
				}
				responseText = ""+p;
				
				
			} else if(cat != null) {
				
				/*
				 * cmd values:
				 * 	set: set category
				 * 	new: new category + set
				 * 	rem: delete category
				 * 	menu: display the cat menu
				 */
				String cmd = (String)params.get("cmd");	
				String hash = (String)params.get("hash");
				
				/*
				 * Change categories
				 */
				
				if(cmd.equalsIgnoreCase("set")  && hash != null) {
					String set_cat = cat;
					
					set_cat = URLDecoder.decode(set_cat, DEFAULT_ENCODING);
					
					if (debugOn) System.out.println("decoded cat: " + set_cat);
					
					String[]	x = torrent_categories.getDefinedValues();
					
					boolean cat_exists = false;
						
					for(int i=0;i<x.length;i++) {
						
						if (x[i].equals(set_cat)) {
							
							cat_exists = true;
							
							continue;
						}
					}
					
					if ( set_cat.equals(CATEGORY_UNKNOWN) ) {
						set_cat = null;
						cat_exists = true;
					}
					
					if (cat_exists) {
						
						Download[] TorrentsList = download_manager.getDownloads(true);

						for (int i=0; i < TorrentsList.length ;i++) {
							
							if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(hash)) {
								
								try {
									if(debugOn) System.out.println("Assigning category " + set_cat + " to torrent " + TorrentsList[i].getTorrent().getName());
								
									TorrentsList[i].setAttribute(torrent_categories, set_cat);
									
									responseText = cat;
									
								} catch (Exception e) {
									
									e.printStackTrace();
									
									continue;
									
								}
							}
						}
					}
				}
				
				if(cmd.equalsIgnoreCase("new")  && hash != null) {
				/*
				 * Create new category
				 */
				
				String new_cat	= cat;
					
					new_cat = URLDecoder.decode(new_cat, DEFAULT_ENCODING);
					
					if ( !new_cat.equals(CATEGORY_UNKNOWN) ) {

						torrent_categories.addDefinedValue(new_cat);
						
						Download[] TorrentsList = download_manager.getDownloads(true);
		
						for (int i=0; i < TorrentsList.length ;i++) {
							
							if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(hash)) {
								
								try {
									if(debugOn) System.out.println("Assigning new category " + new_cat + " to torrent " + TorrentsList[i].getTorrent().getName());
								
									TorrentsList[i].setAttribute(torrent_categories, new_cat);
									
									responseText = cat;
									
								} catch (Exception e) {
									
									e.printStackTrace();
									
									continue;
									
								}						
							}					
						}
					}
				}
				
				if(cmd.equalsIgnoreCase("rem")) {
				
				/*
				 * Remove a category
				 */
				
				String rem_cat	= cat;
					
					rem_cat = URLDecoder.decode(rem_cat, DEFAULT_ENCODING);
					
					torrent_categories.removeDefinedValue(rem_cat);
					
					Download[] TorrentsList = download_manager.getDownloads(true);

					for (int i=0; i < TorrentsList.length ;i++) {
						
						String c = TorrentsList[i].getAttribute(torrent_categories);
						
						if ( c == null || c.equals( "Categories.uncategorized" )){
							
							c = CATEGORY_UNKNOWN;
						}
						
						if (c.equals(rem_cat)) {
							
							try {
								if(debugOn) System.out.println("Removing category " + rem_cat + " and setting torrent " + TorrentsList[i].getTorrent().getName() + " to Uncategorized");
							
								TorrentsList[i].setAttribute(torrent_categories, null);
								
								responseText = CATEGORY_UNKNOWN;
								
							} catch (Exception e) {
								
								e.printStackTrace();
								
								continue;
								
							}
						}
					}
				}
				
				if(cmd.equalsIgnoreCase("menu")) {
					
					/*
					 * Display category menu
					 */
					
					response.setContentType("text/xml;charset=utf-8");
					response.setHeader("Cache-Control", "no-cache, must-revalidate");
					
					pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
					
					if ( torrent_categories != null ){
						
						responseText += ("<categories>");
					
						String[]	x = torrent_categories.getDefinedValues();
						
						for (int j=0;j<x.length;j++){
							
							responseText += ("	<cat>");
							responseText += ("		<name>");
							responseText += ("			" + escapeXML(x[j]));
							responseText += ("		</name>");
							responseText += ("	</cat>");

						}
						
						responseText += ("	<cat>");
						responseText += ("		<name>");
						responseText += ("			" + CATEGORY_UNKNOWN);
						responseText += ("		</name>");
						responseText += ("	</cat>");
						
						responseText += ("	<cat>");
						responseText += ("		<value>");
						responseText += ("			" + "new");
						responseText += ("		</value>");
						responseText += ("		<name>");
						responseText += ("			" + getLocalisedMessageText("template.cmd.cat.new"));
						responseText += ("		</name>");
						responseText += ("	</cat>");
						
						responseText += ("	<cat>");
						responseText += ("		<value>");
						responseText += ("			" + "rem");
						responseText += ("		</value>");
						responseText += ("		<name>");
						responseText += ("			" + getLocalisedMessageText("template.cmd.cat.rem"));
						responseText += ("		</name>");
						responseText += ("	</cat>");
						
						responseText += ("</categories>");
						
						if (debugOn) System.out.println("Categories updated: " + responseText);
						
					}
					
				
				}
				
				
			} else if(remove != null && delete != null) {
					
				Download[] TorrentsList = download_manager.getDownloads(false);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(remove)) {
						
						try {
								
							if(TorrentsList[i].getState() != Download.ST_STOPPED && TorrentsList[i].getState() != Download.ST_STOPPING) {
								
								TorrentsList[i].stop();
								
							}
							
							if (delete.equals("0")) {
								TorrentsList[i].remove(false, false);
								if(debugOn) System.out.println("Removing " + TorrentsList[i].getName());
							} else if ( delete.equals("1")) {
								TorrentsList[i].remove(true, false);
								if(debugOn) System.out.println("Removing AND DELETING torrent of " + TorrentsList[i].getName());
							} else if ( delete.equals("2")) {
								TorrentsList[i].remove(false, true);
								if(debugOn) System.out.println("Removing AND DELETING data of " + TorrentsList[i].getName());
							} else if ( delete.equals("3")) {
								TorrentsList[i].remove(true, true);
								if(debugOn) System.out.println("Removing AND DELETING torrent AND data of " + TorrentsList[i].getName());
							}
							
							responseText = "ok";
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							if(debugOn) System.out.println("Removing of " + TorrentsList[i].getName() + " failed");
							
							continue;
							
						}
					}
				}
			} else if(act != null) {
				
				String hash = (String)params.get("hash");
				
				/*
				 * act: 0 : start
				 * 		1 : stop
				 * 		2 : force start
				 * 		3 : unforce
				 * 
				 * 		update : update torrent info
				 * 
				 * 		stats : get global stats (state)
				 */
			
				/*
				 * Start a download
				 */
				
				//String start 		= (String)params.get("start");
				
				if(hash != null && act.equals("0")) {
					
					//t.setParam("refresh_rate", "3");
					Download[] Torrents = new Download[1];
						
					Download[] Torrents2 = download_manager.getDownloads(true);

					for (int i=0; i < Torrents2.length ;i++) {
						
						if (formatters.encodeBytesToString(Torrents2[i].getTorrent().getHash()).equals(hash)) {
								
								Torrents[0] = Torrents2[i];
						}
					}
					
					
					startTorrent(Torrents);
					
					responseText = Torrents[0].getStats().getStatus(true);
				}
				
				/*
				 * Force Start a download / seed
				 */
				
				//String fstart 		= (String)params.get("fstart");
				
				if(hash != null && act.equals("2")) {
					
					//t.setParam("refresh_rate", "3");
					
					Download[] TorrentsList = download_manager.getDownloads(true);
					
					for (int i=0; i < TorrentsList.length ;i++) {
						
						String _hash = formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash());
						
						if (hash.equals(_hash)) {
							
							try {
								
								if(debugOn) System.out.println("Force Starting " + TorrentsList[i].getName());
							
								TorrentsList[i].setForceStart(true);
								
								responseText = TorrentsList[i].getStats().getStatus(true);
								
							} catch (Exception e) {
								
								e.printStackTrace();
								
								continue;
								
							}
						}
					}
				}
				
				/*
				 * Set Force Start to false
				 */
				
				//String fstop 		= (String)params.get("fstop");
				
				if(hash != null && act.equals("3")) {
					
					//t.setParam("refresh_rate", "3");
					
					Download[] TorrentsList = download_manager.getDownloads(true);
					
					for (int i=0; i < TorrentsList.length ;i++) {
						
						String _hash = formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash());
						
						if (hash.equals(_hash)) {
							
							try {
								
								if(debugOn) System.out.println("Setting Force Start to false for " + TorrentsList[i].getName());
							
								TorrentsList[i].setForceStart(false);
								
								responseText = TorrentsList[i].getStats().getStatus(true);
								
							} catch (Exception e) {
								
								e.printStackTrace();
								
								continue;
								
							}
						}
					}
				}
				
				/*
				 * Stop a download / seed
				 */
				
				//String stop 		= (String)params.get("stop");
				Download[] Torrents = new Download[1];
				
				if(hash != null && act.equals("1")) {
					
					//t.setParam("refresh_rate", "3");
					
						
						Download[] Torrents2 = download_manager.getDownloads(false);
					
						for (int i=0; i < Torrents2.length ;i++) {
							
							String _hash = formatters.encodeBytesToString(Torrents2[i].getTorrent().getHash());
							
							if (hash.equals(_hash)) {
								
								Torrents[0] = Torrents2[i];
									
							}
						}
					
					
					stopTorrents(Torrents);
					
					responseText = Torrents[0].getStats().getStatus(true);
				}
				
				/*
				 * Update a torrent row / a list of torrents rows
				 */
				
				if(hash != null && act.equals("update")) {
					
					Hashtable t_ 			= new Hashtable();
					Hashtable torrents_		= new Hashtable();
					
					//torrent_info.add( t_row );
					
					Download[] Torrents2 = download_manager.getDownloads(true);
					
					/*Map hashes = JsonDecoder.decodeJSON(URLDecoder.decode(hash, "UTF8"));
					
					Set entries0 = hashes.entrySet();
					
					Iterator iterator0 = entries0.iterator();
					
					while(iterator0.hasNext()){
						
						Map.Entry entry = (Map.Entry)iterator0.next();
						
						System.out.println(entry.getKey() + ": " + entry.getValue());
						
					}
					
					JSONArray j = (JSONArray)hashes.get("value");*/
					
					String[] j = URLDecoder.decode(hash, "UTF8").split(",");
					
					Map hhh = new Hashtable();
					
					hhh.put("hashes", j);
					
					//t_.put("size", ""+j.size());
					
					//t_.put("hashes", hashes.get("value"));
					
					t_.put("size", ""+j.length);
					
					//t_.put("hashes", JsonMapEncoder.encodes(hhh));
					
					//for(int k=0; k < j.size(); k ++) {	
						
					for(int k=0; k < j.length; k ++) {	
						
						//String h = (String)(j.toArray())[k]; 
						String h = j[k]; 
						Hashtable t_row 			= new Hashtable();
					
					
					Download torrent = null;
					
					for (int i=0; i < Torrents2.length ;i++) {
						
						String _hash = formatters.encodeBytesToString(Torrents2[i].getTorrent().getHash());
						
						if (h.equals(_hash)) {
							
							torrent = Torrents2[i];
							
							t_row.put("index", "" + i);
								
						}
					}
					
					if(torrent != null) {
					
					String torrent_name = torrent.getName();
					
					if (torrent_name.length() > 35) {
						t_row.put("short_name", escapeXML(torrent_name.substring(0, 35)) + "...");
						t_row.put("name_too_long", "1");
					} else {
						t_row.put("short_name", escapeXML(torrent_name));
						t_row.put("name_too_long", "0");
					}
					
					t_row.put("name", escapeXML(torrent_name));
					
					t_row.put("url_name", formatters.encodeBytesToString( torrent.getTorrent().getHash()));
					
					t_row.put("hash_short", formatters.encodeBytesToString( torrent.getTorrent().getHash()));
					
					t_row.put("position", "" + torrent.getPosition());
					
					boolean bTorrentNoMagnet = torrent.getTorrent().isPrivate() || !torrent.getTorrent().isDecentralisedBackupEnabled();
					
					t_row.put("has_magnet", bTorrentNoMagnet?"0":"1");
					
					try{
						t_row.put("magnet", ""+torrent.getTorrent().getMagnetURI());
					}catch(TorrentException tex) {
						tex.printStackTrace();
					}
					
					t_row.put("ontracker", "0");
					
					Set entries = torrentsHostedorPublished.entrySet();
					
					Iterator iterator = entries.iterator();
					
					while(iterator.hasNext()){
						
						Map.Entry entry = (Map.Entry)iterator.next();
						
						if (entry.getKey().equals(torrent.getTorrent().getHash())) {

							t_row.put("ontracker", "1");
						}
						
					}

					String	c = torrent.getAttribute( torrent_categories );
					
					if ( c == null || c.equals( "Categories.uncategorized" )){
						
						c = CATEGORY_UNKNOWN;
					}

					//t_row.put("category", URLEncoder.encode(c, DEFAULT_ENCODING));
					t_row.put("category", escapeXML(c));
					
					long torrent_peers = 0;
					long torrent_seeds = 0;
					
					boolean isDownloading = false;
					boolean isSeeding	= false;
					boolean isStartable = false;		
					boolean isFStart	= false;
					boolean isCompleteAndStopped = false;
					
					int	status = torrent.getState();
					String	status_str = torrent.getStats().getStatus(true);
					
					if ( status == Download.ST_STOPPED ){
						
						isStartable = true;
						
						if(torrent.isComplete(false)) isCompleteAndStopped = true;
						
					}else if ( status == Download.ST_DOWNLOADING ){
						
						isDownloading = true;
						
						try {
							torrent_seeds = torrent.getPeerManager().getStats().getConnectedSeeds() ;
						} catch (Throwable e) {
							torrent_seeds = 0 ;
						}
						
						try {
							torrent_peers = torrent.getPeerManager().getStats().getConnectedLeechers() ;
						} catch (Throwable e) {
							torrent_peers = 0 ;
						}
						
					}else if ( status == Download.ST_SEEDING ){
						
						isSeeding = true;
						
						try {
							torrent_peers = torrent.getPeerManager().getStats().getConnectedLeechers() ;
						} catch (Throwable e) {
							torrent_peers = 0;
						}
					}
					
					if( torrent.isForceStart()) isFStart = true;
					
					t_row.put("isFStart", isFStart? "1":"0");
					
					t_row.put("status", status_str);
					
					t_row.put("isDLing", isDownloading? "1":"0");
					
					t_row.put("isCDing", isSeeding? "1":"0");
					
					t_row.put("isStartable", isStartable? "1":"0");
					
					long torrent_size = torrent.getTorrent().getSize();
//					total_size += torrent_size;
					
					t_row.put("size", torrent_size<=0 ? "N/A": formatters.formatByteCountToKiBEtc(torrent_size));
					
					t_row.put("percent_done", "" + (torrent.getStats().getCompleted()/10.0f));
					
					long torrent_downloaded = torrent.getStats().getDownloaded();
					long torrent_uploaded = torrent.getStats().getUploaded();
					
//					total_downloaded += torrent_downloaded;
//					total_uploaded += torrent_uploaded;
					
					t_row.put("downloaded", "" + formatters.formatByteCountToKiBEtc(torrent_downloaded));
					
					t_row.put("uploaded", "" + formatters.formatByteCountToKiBEtc(torrent_uploaded));
					
					long torrent_dl_speed = torrent.getStats().getDownloadAverage();
					long torrent_ul_speed = torrent.getStats().getUploadAverage();
					
					long total_download = download_manager.getStats().getDataReceiveRate();
					long total_upload = download_manager.getStats().getDataSendRate();
					
					t_row.put("total_download", formatters.formatByteCountToKiBEtcPerSec(total_download));
					t_row.put("total_upload", formatters.formatByteCountToKiBEtcPerSec(total_upload));
					
					t_row.put("dl_speed", formatters.formatByteCountToKiBEtcPerSec(torrent_dl_speed));
					
					t_row.put("ul_speed", formatters.formatByteCountToKiBEtcPerSec(torrent_ul_speed));
					
					String torrent_total_seeds = "" + torrent.getLastScrapeResult().getSeedCount();
					
					if ( torrent_total_seeds.equals("-1")) torrent_total_seeds = "-";
					
					t_row.put("seeds", "" + torrent_seeds);
					
					t_row.put("total_seeds", "" + torrent_total_seeds);
					
					String torrent_total_peers = "" + torrent.getLastScrapeResult().getNonSeedCount();
					
					if ( torrent_total_peers.equals("-1")) torrent_total_peers = "-";
					
					t_row.put("peers", "" + torrent_peers);
					
					t_row.put("total_peers", "" + torrent_total_peers);
					
					final NumberFormat sr_format;
					sr_format = NumberFormat.getInstance();
					sr_format.setMaximumFractionDigits(3);
					sr_format.setMinimumFractionDigits(1);
					
					float availf = torrent.getStats().getAvailability();
					t_row.put("avail", (availf == -1.0) ? "" : sr_format.format(availf) );
					
					int sr = torrent.getStats().getShareRatio();
					String share_ratio = "";
					
					if (sr ==  -1 ) {
						share_ratio = "&#8734;";
					} else {
						t_row.put("isFP", (sr < minSR) ? "1" : "0");
						
						share_ratio = sr_format.format( sr/1000.0f );
					}
					
					t_row.put("share_ratio", "" + share_ratio );
					
					t_row.put("eta", torrent.getStats().getETA() );
					
					boolean isInSeedList = !(isCompleteAndStopped && bCompletedTab);
					
					t_row.put("type", torrent.isComplete(false) ? ( isInSeedList ? "1" : "2" ) : "0");

					t_row.put( "count_0" , getDownloadingTorrentsCount() + "");
					t_row.put( "count_1" , bCompletedTab?getSeedingTorrentsCount(false) + "":getSeedingTorrentsCount(true) + "");
					
					if(bCompletedTab) t_row.put( "count_2" , getCompletedTorrentsCount() + "");
					
					}
						
						torrents_.put(h, t_row);
					}
					
					t_.put("torrents", torrents_);
					
					responseText = JsonMapEncoder.encodes(t_);
					
				}
				
				/*
				 * Update global stats
				 */
				
				if (act.equals("stats")){
					
					String st = (String)params.get("st");
					int state = Integer.parseInt(st);
					
					Download[] torrents = null;
					
					Hashtable statsMap	= new Hashtable();
					
					switch (state) {
					case 0:
						torrents = getDLDownloadingTorrentsByPos();
						break;
					case 1:
						torrents = getDLSeedingTorrentsByPos( bCompletedTab );
						break;
					case 2:
						torrents = getDLCompletedTorrentsByPos();
						break;

					default:
						break;
					}
					
					long total_transferred = 0;
					long total_download = 0;
					long total_upload = 0;
					long total_size = 0;
					
					for(int i = 0; i < torrents.length; i++) {
						
						DownloadStats torrent_stats = torrents[i].getStats();
						
						total_download += torrent_stats.getDownloadAverage();
						total_upload += torrent_stats.getUploadAverage();
						total_transferred += (state==0)?torrent_stats.getDownloaded():torrent_stats.getUploaded();
						total_size += torrents[i].getTorrent().getSize();
										
					}
					
					statsMap.put("total_download", formatters.formatByteCountToKiBEtcPerSec(total_download));
					
					statsMap.put("total_upload", formatters.formatByteCountToKiBEtcPerSec(total_upload));
					
					statsMap.put("total_transferred", "" + formatters.formatByteCountToKiBEtc(total_transferred));
					
					statsMap.put("total_size", "" + formatters.formatByteCountToKiBEtc(total_size));
					
					int max_ul_speed	= plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC );
					
					int max_ul_speed_seed = plugin_config.getIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC);
					
					boolean isSeedingOnly = download_manager.isSeedingOnly();
					
					boolean max_ul_speed_seed_enabled = plugin_config.getBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING);

					int max_dl_speed	= plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC );
					
					statsMap.put("max_upload_speed", (isSeedingOnly && max_ul_speed_seed_enabled)?max_ul_speed_seed==0? getLocalisedMessageText("template.speed.unlimited")+"*" : formatters.formatByteCountToKiBEtcPerSec( max_ul_speed_seed * 1024 )+"*":max_ul_speed==0? getLocalisedMessageText("template.speed.unlimited") : formatters.formatByteCountToKiBEtcPerSec( max_ul_speed * 1024 ));
					
					statsMap.put("max_dl_speed", max_dl_speed==0? getLocalisedMessageText("template.speed.unlimited") : formatters.formatByteCountToKiBEtcPerSec( max_dl_speed * 1024));
					
					statsMap.put("total_up", formatters.formatByteCountToKiBEtcPerSec(download_manager.getStats().getDataSendRate()));
					
					statsMap.put("total_dl", formatters.formatByteCountToKiBEtcPerSec(download_manager.getStats().getDataReceiveRate()));
					
					statsMap.put( "count_0" , getDownloadingTorrentsCount() + "");
					statsMap.put( "count_1" , bCompletedTab?getSeedingTorrentsCount(false) + "":getSeedingTorrentsCount(true) + "");
					
					if(bCompletedTab) statsMap.put( "count_2" , getCompletedTorrentsCount() + "");
					
					Long usableSpace = null;
					
					//if(torrents.length > 0){
//						File f = new File(torrents[0].getTorrentFileName());
						File f = plugin_config.getPluginUserFile("plugin.properties"); 
					
						try {
							Method usableSpaceMethod = File.class.getMethod("getUsableSpace", new Class[]{});
							usableSpace = (Long)usableSpaceMethod.invoke(f, new Object[]{});
							
						}catch(Exception e){//TODO better exception handling maybe ? or not ... whatever
							e.printStackTrace();
						}
					//}
					statsMap.put("usable_space", (usableSpace!= null)?""+formatters.formatByteCountToKiBEtc(usableSpace.longValue()):"N/A (requires Java 6)" );
					
					responseText = JsonMapEncoder.encodes(statsMap);
				}
			
			} else if( update != null ){
				
				// XXX
				
				String st = (String)params.get("st");
				int state = Integer.parseInt(st);
				//String search = URLDecoder.decode((String)params.get("search"),"UTF8");
				
				String _page = (String)params.get("page");
				int page = 1;
				if(_page != null) page = Integer.parseInt(_page);
				
				int nbPerPage = plugin_config.getPluginIntParameter(CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE);
				
				
				if(update.equals("hashes")){
				
					Download[] torrents = null;
					int l,count,startIndex,endIndex;
					Hashtable t_row 			= new Hashtable();
					
					switch (state) {
					case 0:
						//torrents = getDLDownloadingTorrentsByPos();
						count = getDownloadingTorrentsCount(null);
						startIndex = (page-1)*nbPerPage;
						startIndex = (startIndex<0 || startIndex >= count)?0:startIndex;
						endIndex = startIndex + nbPerPage;
						endIndex = (endIndex>count || endIndex == 0)?count:endIndex;
						l = endIndex - startIndex;
						torrents = new Download[l];
						if(debugOn) System.out.println("State " + state + " :: Copying " + l + " torrents / count: " + count +  " / nbPerPage: " + nbPerPage + " / startIndex: " + startIndex + " / endIndex: " + endIndex + " / l: " + l );
						System.arraycopy(getDLDownloadingTorrentsByPos(), startIndex, torrents, 0, l);
						break;
					case 1:
						//torrents = getDLSeedingTorrentsByPos( bCompletedTab );
						count = getSeedingTorrentsCount(!bCompletedTab, null);
						startIndex = (page-1)*nbPerPage;
						startIndex = (startIndex<0 || startIndex >= count)?0:startIndex;
						endIndex = startIndex + nbPerPage;
						endIndex = (endIndex>count || endIndex == 0)?count:endIndex;
						l = endIndex - startIndex;
						torrents = new Download[l];
						if(debugOn) System.out.println("State " + state + " :: Copying " + l + " torrents / count: " + count +  " / nbPerPage: " + nbPerPage + " / startIndex: " + startIndex + " / endIndex: " + endIndex + " / l: " + l );
						System.arraycopy(getDLSeedingTorrentsByPos( bCompletedTab ), startIndex, torrents, 0, l);
						break;
					case 2:
						//torrents = getDLCompletedTorrentsByPos();
						count = getCompletedTorrentsCount(null);
						startIndex = (page-1)*nbPerPage;
						startIndex = (startIndex<0 || startIndex >= count)?0:startIndex;
						endIndex = startIndex + nbPerPage;
						endIndex = (endIndex>count || endIndex == 0)?count:endIndex;
						l = endIndex - startIndex;
						torrents = new Download[l];
						if(debugOn) System.out.println("State " + state + " :: Copying " + l + " torrents / count: " + count +  " / nbPerPage: " + nbPerPage + " / startIndex: " + startIndex + " / endIndex: " + endIndex + " / l: " + l );
						System.arraycopy(getDLCompletedTorrentsByPos(), startIndex, torrents, 0, l);
						break;
	
					default:
						break;
					}
					
					for(int i = 0; i < torrents.length; i++) {
						
						Hashtable tor = new Hashtable();
	//					for(int j=0; j<3; j++){
							tor.put("hash", formatters.encodeBytesToString(torrents[i].getTorrent().getHash()));
							tor.put("index", "0");
							tor.put("pos", "" + torrents[i].getPosition());
	//					}
						t_row.put(""+i, tor);
					}
					
					t_row.put("size", "" + torrents.length);
					
					responseText = JsonMapEncoder.encodes(t_row);
				
				}
				
				if(update.equals("pagination")) {
					
					int l = 1;
					
					if(nbPerPage>0){
						
						switch (state) {
						case 0:
							//torrents = getDLDownloadingTorrentsByPos();
							l = (int)Math.ceil((double)getDownloadingTorrentsCount(null) / nbPerPage);
							break;
						case 1:
							//torrents = getDLSeedingTorrentsByPos( bCompletedTab );
							l = (int)Math.ceil((double)getSeedingTorrentsCount(!bCompletedTab, null) / nbPerPage);
							break;
						case 2:
							//torrents = getDLCompletedTorrentsByPos();
							l = (int)Math.ceil((double)getCompletedTorrentsCount(null) / nbPerPage);
							break;
		
						default:
							break;
						}
					}
					responseText = ""+l;
					
				}
				
			} else if(display != null && display.equals("u")) {
				
				final String up_url = (String)params.get("upurl");
				
				if(up_url != null) {
					
					String d_url = URLDecoder.decode(up_url, DEFAULT_ENCODING);
					
					String url_load_msg = "";
						
					  String[] urls = d_url.split(";"); 
					  
					  for(int i =0; i < urls.length; i++ ) {
						  
						  try {
						
					          URL url = new URL(urls[i]);
					          
					          ResourceDownloader rd = plugin_interface.getUtilities().getResourceDownloaderFactory().create(url);
					            
					          InputStream is = rd.download();
					          
					          final Torrent torrent = plugin_interface.getTorrentManager().createFromBEncodedInputStream(is);
					          
					          download_manager.addDownload(torrent);
								
					          url_load_msg += escapeXML(torrent.getName()) + " " + getLocalisedMessageText("template.upload.url.msg.success") + "<br>";
							
							} catch (Exception e) {
								
								if(debugOn) System.out.println("Loading of " + urls[i] + " failed");
								
								url_load_msg += getLocalisedMessageText("template.upload.url.msg.error") + " " + escapeXML(urls[i]) + "<br>";
								
								e.printStackTrace();
								
							}
					  }
					  
					  responseText = url_load_msg;
				}
				
			} else {
			
			/*
			 * Set config params
			 */
			
			boolean max_ul_speed_seed_enabled = plugin_config.getBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING);
			
			int max_ul_speed_seed = plugin_config.getIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC);
			
			int max_ul_speed	= plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC );

			int max_dl_speed	= plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC );
			
			int current_maxDLs = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOADS );
			
			int current_maxActive = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_ACTIVE );
			
			boolean max_active_seed_enabled = plugin_config.getBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_MAX_ACTIVE_SEEDING);
			
			int current_maxActiveSeed = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_ACTIVE_SEEDING );
			
			int current_maxConn_perTor = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT );
			
			int current_maxConn = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL );
			
			int current_maxUps = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOADS );
			
			int current_maxUps_seed = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOADS_SEEDING );
			
			boolean current_compTab = plugin_config.getPluginBooleanParameter(CONFIG_HTML_WEBUI_3TABS); 
			
			int current_pagination = plugin_config.getPluginIntParameter(CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE);
			
			String set_max_dl		= (String)params.get("max_dl");
			
			String set_max_active	= (String)params.get("max_active");
			
			String set_max_active_seed	= (String)params.get("max_active_seed");
			
			String set_max_active_seed_enabled	= (String)params.get("max_active_seed_enabled");
			
			String set_max_conn_per_torrent		= (String)params.get("max_conn_pertor");
			
			String set_max_conn		= (String)params.get("max_conn");
			
			String set_max_up_speed_auto = (String)params.get("max_auto_up");
			
			String set_max_up_speed	= (String)params.get("max_ul_speed");
			
			String set_max_up_speed_seed	= (String)params.get("max_ul_speed_seed");
			
			String set_max_up_speed_seed_enabled	= (String)params.get("max_ul_speed_seed_enabled");
			
			String set_max_do_speed	= (String)params.get("max_dl_speed");
			
			String set_max_ups	= (String)params.get("max_ups");
			
			String set_max_ups_seed	= (String)params.get("max_ups_seed");
			
			String set_comp_tab = (String)params.get("comp_tab");
			
			String set_pagination_per_page = (String)params.get("pagination_per_page");
			
			String op_set_msg ="";
			
			if(set_max_dl != null) {
				
				int maxDL = Integer.parseInt(set_max_dl);
				
				if (current_maxDLs != maxDL) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxdls") + " ";
				
						plugin_config.setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_DOWNLOADS, maxDL);
						
						op_set_msg += getLocalisedMessageText("template.options.set") + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset") + " ";
						
					}
				}
			}
			
			if(set_max_active != null) {
				
				int maxActive = Integer.parseInt(set_max_active);
				
				if (current_maxActive != maxActive) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxactive") + " ";
				
						plugin_config.setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_ACTIVE, maxActive);
						
						op_set_msg += getLocalisedMessageText("template.options.set") + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset") + " ";
						
					}
				}
			}
			
			if(set_max_active_seed != null) {
				
				int maxActiveSeed = Integer.parseInt(set_max_active_seed);
				
				if (current_maxActiveSeed != maxActiveSeed) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxactiveseed") + " ";
				
						plugin_config.setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_ACTIVE_SEEDING, maxActiveSeed);
						
						op_set_msg += getLocalisedMessageText("template.options.set") + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset") + " ";
						
					}
				}
			}
			
			if(set_max_active_seed_enabled != null) {
				
				boolean b_set_max_active_seed_enabled = false;
				if(set_max_active_seed_enabled.equals("1")) b_set_max_active_seed_enabled = true;
				
				if ( b_set_max_active_seed_enabled != max_active_seed_enabled) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxactiveseed") + " ";
						
						plugin_config.setBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_MAX_ACTIVE_SEEDING, b_set_max_active_seed_enabled);
						
						op_set_msg += (b_set_max_active_seed_enabled?
								getLocalisedMessageText("template.options.enabled"):
								getLocalisedMessageText("template.options.disabled")) + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += (b_set_max_active_seed_enabled?
								getLocalisedMessageText("template.options.enabled.not"):
								getLocalisedMessageText("template.options.disabled.not")) + " ";
						
					}
				}
			}
			
			if(set_max_conn_per_torrent != null) {
				
				int maxConnPerTor = Integer.parseInt(set_max_conn_per_torrent);
				
				if (current_maxConn_perTor != maxConnPerTor) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxconnpertorrent") + " ";
				
						plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT, maxConnPerTor);
						
						op_set_msg += getLocalisedMessageText("template.options.set") + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset") + " ";
						
					}
				}
			}
			
			if(set_max_conn != null) {
				
				int maxConn = Integer.parseInt(set_max_conn);
				
				if (current_maxConn != maxConn) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxconn") + " ";
				
						plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL, maxConn);
						
						op_set_msg += getLocalisedMessageText("template.options.set") + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset") + " ";
						
					}
				}
			}
			
			if(set_max_up_speed_auto != null && plugin_config.getBooleanParameter("AutoSpeed Available")) {
				
				boolean max_up_speed_auto = (Integer.parseInt(set_max_up_speed_auto) == 1)?true:false;
				
				boolean maxUpAutoEnabled = plugin_config.getBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON);
				
				if (max_up_speed_auto != maxUpAutoEnabled) {

					try {
						
						op_set_msg += getLocalisedMessageText("template.options.autospeed") + " ";
					
						plugin_config.setBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON, max_up_speed_auto);
						
						op_set_msg += (max_up_speed_auto?
								getLocalisedMessageText("template.options.enabled"):
								getLocalisedMessageText("template.options.disabled")) + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += (max_up_speed_auto?
								getLocalisedMessageText("template.options.enabled.not"):
								getLocalisedMessageText("template.options.disabled.not")) + " ";
					}
				}
				
			}
			
			if(set_max_up_speed != null) {
				
				int maxUp = Integer.parseInt(set_max_up_speed);
				
				if (max_ul_speed != maxUp) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxulspeed") + " ";
				
						plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, maxUp );
						
						op_set_msg += getLocalisedMessageText("template.options.set") + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset") + " ";
						
					}
				}
			}
			
			if(set_max_up_speed_seed != null) {
				
				int maxUpSeed = Integer.parseInt(set_max_up_speed_seed);
				
				if (max_ul_speed_seed != maxUpSeed) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxulspeedseeding") + " ";
				
						plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC, maxUpSeed );
						
						op_set_msg += getLocalisedMessageText("template.options.set") + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset") + " ";
						
					}
				}
			}
			
			if(set_max_up_speed_seed_enabled != null) {
				
				boolean b_set_max_up_speed_seed_enabled = false;
				if(set_max_up_speed_seed_enabled.equals("1")) b_set_max_up_speed_seed_enabled = true;
				
				if ( b_set_max_up_speed_seed_enabled != max_ul_speed_seed_enabled) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxulspeedseeding") + " ";
						
						plugin_config.setBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING, b_set_max_up_speed_seed_enabled);
						
						op_set_msg += (b_set_max_up_speed_seed_enabled?
								getLocalisedMessageText("template.options.enabled"):
								getLocalisedMessageText("template.options.disabled")) + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += (b_set_max_up_speed_seed_enabled?
								getLocalisedMessageText("template.options.enabled.not"):
								getLocalisedMessageText("template.options.disabled.not")) + " ";
						
					}
				}
			}
			
			if(set_max_do_speed != null) {
				
				int maxDown = Integer.parseInt(set_max_do_speed);
				
				if (max_dl_speed != maxDown) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxdlspeed") + " ";
								
						plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, maxDown );
						
						op_set_msg += getLocalisedMessageText("template.options.set")+ " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset")+ " ";
						
					}
				}
			}
			
			if(set_max_ups != null) {
				
				int maxUps = Integer.parseInt(set_max_ups);
				
				if (current_maxUps != maxUps) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxups") + " ";
								
						plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOADS, maxUps );
						
						op_set_msg += getLocalisedMessageText("template.options.set")+ " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset")+ " ";
						
					}
				}
			}
			
			if(set_max_ups_seed != null) {
				
				int maxUpsSeed = Integer.parseInt(set_max_ups_seed);
				
				if (current_maxUps_seed != maxUpsSeed) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.maxupsseed") + " ";
								
						plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOADS_SEEDING, maxUpsSeed );
						
						op_set_msg += getLocalisedMessageText("template.options.set")+ " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += getLocalisedMessageText("template.options.notset")+ " ";
						
					}
				}
			}
			
			if(set_comp_tab != null) {
				
				boolean compTab = (Integer.parseInt(set_comp_tab) == 1)?true:false;
				
				if (current_compTab != compTab) {
					
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.comptab") + " ";
						
						plugin_config.setPluginParameter( CONFIG_HTML_WEBUI_3TABS, compTab );
						
						bCompletedTab = compTab;
						
						op_set_msg += (compTab?
								getLocalisedMessageText("template.options.enabled"):
								getLocalisedMessageText("template.options.disabled")) + " ";
					
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += (compTab?
								getLocalisedMessageText("template.options.enabled.not"):
								getLocalisedMessageText("template.options.disabled.not")) + " ";
						
					}
				}
			}
			
			if(set_pagination_per_page != null) {
				
				boolean pagination = (Integer.parseInt(set_pagination_per_page) > 0)?true:false;
				boolean cur_pag = current_pagination > 0;
				
				if(debugOn) System.out.println("Current Pagination: " + current_pagination + " changing to " + set_pagination_per_page);
				
				if(current_pagination != Integer.parseInt(set_pagination_per_page)){
					try {
						
						op_set_msg += getLocalisedMessageText("template.options.pagination") + " ";
						
						if(debugOn) System.out.println(op_set_msg);
						
						plugin_config.setPluginParameter( CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE, Integer.parseInt(set_pagination_per_page) );
						
						op_set_msg += (pagination?
								getLocalisedMessageText("template.options.enabled") + "(" + set_pagination_per_page + " " + getLocalisedMessageText("template.options.pagination.per_page") + ")":
								getLocalisedMessageText("template.options.disabled")) + " ";
						if(debugOn) System.out.println(op_set_msg);
					} catch(Exception e) {
						
						e.printStackTrace();
						
						op_set_msg += (pagination?
								getLocalisedMessageText("template.options.enabled.not"):
								getLocalisedMessageText("template.options.disabled.not")) + " ";
						
					}
				}
			}
			
			responseText = (!op_set_msg.equals(""))?op_set_msg:getLocalisedMessageText("template.options.nothingchanged");
			
			////////
			}
		}
		
		if(debugOn) System.out.println("Ajax response: " + responseText);
		
		pw.print(responseText);
		pw.flush();
		
		return ( true );
	}
	
	protected boolean
	handleTemplate(
		String			page_url,
		Hashtable		params,
		Hashtable		args,
		OutputStream	os )
	
		throws IOException
	{	
		
		page_url = page_url.substring(1);
		
		args.put( "loop_context_vars", "true" );
		args.put( "global_vars", "true" );
		
		Template t = new Template( args );
		
		t.setParam("title", plugin_config.getPluginStringParameter( CONFIG_HTMLWEBUI_TITLE ));
		
		t.setParam( "css_name", plugin_config.getPluginStringParameter( CONFIG_CSS, CONFIG_CSS_DEFAULT ) );
		
		t.setParam("pause_resume_ok", 1);
		
		if (download_manager.canPauseDownloads()){
			
			t.setParam("pause_resume", "Pause");
			t.setParam("pause_resume_href", "pause");
			
		} else if(download_manager.canResumeDownloads()) {
			
			t.setParam("pause_resume", "Resume");
			t.setParam("pause_resume_href", "resume");
		} else {
			t.setParam("pause_resume_ok", 0);
		}
		
		t.setParam("cmd_startall", getLocalisedMessageText("template.cmd.startAll"));
		t.setParam("cmd_stopall", getLocalisedMessageText("template.cmd.stopAll"));
		t.setParam("cmd_set", getLocalisedMessageText("template.cmd.set"));
		
		t.setParam("txt_refresh", getLocalisedMessageText("template.txt.refresh"));
		
		t.setParam("links_tracker", getLocalisedMessageText("template.links.tracker"));
		
		t.setParam("tab_downloads", getLocalisedMessageText("template.tab.downloads"));
		t.setParam("tab_seeds", getLocalisedMessageText("template.tab.seeds"));
		t.setParam("tab_completed", getLocalisedMessageText("template.tab.completed"));
		t.setParam("tab_options", getLocalisedMessageText("template.tab.options"));
		t.setParam("tab_upload", getLocalisedMessageText("template.tab.upload"));
		t.setParam("th_torrent", getLocalisedMessageText("template.th.torrent"));
		t.setParam("th_category", getLocalisedMessageText("template.th.category"));
		t.setParam("th_status", getLocalisedMessageText("template.th.status"));
		t.setParam("th_size", getLocalisedMessageText("template.th.size"));
		t.setParam("th_commands", getLocalisedMessageText("template.th.commands"));
		t.setParam("th_downloaded", getLocalisedMessageText("template.th.downloaded"));
		t.setParam("th_uploaded", getLocalisedMessageText("template.th.uploaded"));
		t.setParam("th_done", getLocalisedMessageText("template.th.done"));
		t.setParam("th_DLspeed", getLocalisedMessageText("template.th.DLspeed"));
		t.setParam("th_ULspeed", getLocalisedMessageText("template.th.ULspeed"));
		t.setParam("th_peers", getLocalisedMessageText("template.th.peers"));
		t.setParam("th_seeds", getLocalisedMessageText("template.th.seeds"));
		t.setParam("th_shareRatio", getLocalisedMessageText("template.th.shareRatio"));
		t.setParam("th_ETA", getLocalisedMessageText("template.th.ETA"));
		t.setParam("th_availability", getLocalisedMessageText("template.th.availability"));
		
		t.setParam("cmd_start", getLocalisedMessageText("template.cmd.start"));
		t.setParam("cmd_stop", getLocalisedMessageText("template.cmd.stop"));
		t.setParam("cmd_force", getLocalisedMessageText("template.cmd.force"));
		t.setParam("cmd_unforce", getLocalisedMessageText("template.cmd.unforce"));
		
		t.setParam("cmd_remove", getMessageText("MyTorrentsView.menu.remove"));
		t.setParam("cmd_del1", getMessageText("MyTorrentsView.menu.removeand.deletetorrent"));
		t.setParam("cmd_del2", getMessageText("MyTorrentsView.menu.removeand.deletedata"));
		t.setParam("cmd_del3", getMessageText("MyTorrentsView.menu.removeand.deleteboth"));
		
		t.setParam( "display_state", 0);
		
		t.setParam("cmd_cat_new", getLocalisedMessageText("template.cmd.cat.new"));
		t.setParam("cmd_cat_rem", getLocalisedMessageText("template.cmd.cat.rem"));
		
		t.setParam("cmd_movetop", getLocalisedMessageText("template.cmd.tor.movetotop"));
		t.setParam("cmd_moveup", getLocalisedMessageText("template.cmd.tor.moveup"));
		t.setParam("cmd_movedown", getLocalisedMessageText("template.cmd.tor.movedown"));
		t.setParam("cmd_movebottom", getLocalisedMessageText("template.cmd.tor.movetobottom"));
		
		t.setParam("cmd_pub", getMessageText("MyTorrentsView.menu.publish"));
		t.setParam("cmd_host", getMessageText("MyTorrentsView.menu.host"));
		
		t.setParam("cmd_track_rem", getLocalisedMessageText("template.cmd.track.rem"));
		
		String mode_str = plugin_interface.getPluginconfig().getPluginStringParameter( WebPlugin.CONFIG_MODE, ((WebPlugin)plugin_interface.getPlugin()).CONFIG_MODE_DEFAULT );
		
		view_mode = !mode_str.equalsIgnoreCase( WebPlugin.CONFIG_MODE_FULL );
		
		t.setParam("view_only", view_mode);
		
		bCompletedTab = plugin_config.getPluginBooleanParameter(CONFIG_HTML_WEBUI_3TABS);
		
		t.setParam("completed_tab", bCompletedTab);
		
		t.setParam("tracker_enabled", tracker_enabled ? "1" : "0");
		
		if (tracker_enabled) {
		
			if (tracker_plugin_loaded) {
				
				tracker_web_enabled = tracker_plugin_config.getPluginBooleanParameter( "Tracker Publish Enable");
				
				t.setParam("tracker_web_enabled", tracker_web_enabled ? "1" : "0");
				
				if (tracker_web_enabled) {
					
					String tracker_url = tracker.getURLs()[Math.min(1,tracker.getURLs().length-1)].toExternalForm();
					tracker_url = tracker_url.substring(0, tracker_url.length()-9);
					
					t.setParam("tracker_url", tracker_url);

				}
			}
		}
		
		t.setParam( "page_url", page_url);
		t.setParam( "page", page_url);
		t.setParam("search_on", false);
		
		t.setParam("total_up", formatters.formatByteCountToKiBEtcPerSec(download_manager.getStats().getDataSendRate()));
		
		t.setParam("total_dl", formatters.formatByteCountToKiBEtcPerSec(download_manager.getStats().getDataReceiveRate()));
		
		boolean isSeedingOnly = plugin_interface.getDownloadManager().isSeedingOnly();
		
		boolean max_ul_speed_seed_enabled = plugin_config.getBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING);
		
		t.setParam("max_ul_speed_seed_enabled", max_ul_speed_seed_enabled);
		
		int max_ul_speed_seed = plugin_config.getIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC);
		
		t.setParam("max_ul_speed_seed", max_ul_speed_seed==0? "Unlimited" : formatters.formatByteCountToKiBEtcPerSec( max_ul_speed_seed * 1024 ));
		
		t.setParam("max_ul_speed_seed_o",  max_ul_speed_seed);
		
		int max_ul_speed	= plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC );

		int max_dl_speed	= plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC );
		
		//t.setParam("max_ul_speed", max_ul_speed==0? "Unlimited" : formatters.formatByteCountToKiBEtcPerSec( max_ul_speed * 1024 ));
		
		t.setParam("max_dl_speed", max_dl_speed==0? getLocalisedMessageText("template.speed.unlimited") : formatters.formatByteCountToKiBEtcPerSec( max_dl_speed * 1024));
		
		t.setParam("max_ul_speed_o",  max_ul_speed);
		
		t.setParam("max_dl_speed_o",  max_dl_speed);
		
		int current_maxDLs = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOADS );
		
		t.setParam("max_dl", current_maxDLs);
		
		int current_maxActive = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_ACTIVE );
		
		t.setParam("max_active", current_maxActive);
		
		boolean max_active_seed_enabled = plugin_config.getBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_MAX_ACTIVE_SEEDING);
		
		int current_maxActiveSeed = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_ACTIVE_SEEDING );
		
		t.setParam("max_active_seed", current_maxActiveSeed);
		
		t.setParam("max_active_seed_enabled", max_active_seed_enabled);
		
		int current_maxConn_perTor = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT );
		
		t.setParam("max_conn_pertorrent", current_maxConn_perTor);
		
		int current_maxConn = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL );
		
		t.setParam("max_conn", current_maxConn);
		
		int current_maxUps = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOADS );
		
		t.setParam("max_ups", current_maxUps);
		
		int current_maxUpsSeed = plugin_config.getIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOADS_SEEDING );
		
		t.setParam("max_ups_seed", current_maxUpsSeed);
		
		boolean auto_speed_avail = plugin_config.getBooleanParameter("AutoSpeed Available");
		
		t.setParam("auto_speed_not_avail", auto_speed_avail?"0":"1");
		
		boolean current_auto_speed = plugin_config.getBooleanParameter(PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON);
		
		t.setParam("max_ul_speed_auto_on", (current_auto_speed && auto_speed_avail)?"1":"0");
		
		boolean current_comp_tab = plugin_config.getPluginBooleanParameter(CONFIG_HTML_WEBUI_3TABS);
		
		t.setParam("comp_tab", current_comp_tab);
		
		int current_pagination = plugin_config.getPluginIntParameter(CONFIG_HTML_WEBUI_PAGINATION_PER_PAGE);
		
		t.setParam("pagination_per_page", current_pagination);
		
		minSR = plugin_config.getIntParameter("StartStopManager_iFirstPriority_ShareRatio");
		
		t.setParam("refresh_rate", refresh_rate);
		
		t.setParam("torrent_refresh_on", true);
		
		t.setParam("url_load_msg", " " + getLocalisedMessageText("template.upload.choose") + ":");
		
		TrackerTorrent[] trackerTorrent = tracker.getTorrents();
		
		torrentsHostedorPublished.clear();
		
		for(int i=0; i<trackerTorrent.length; i++) {
			
			torrentsHostedorPublished.put( trackerTorrent[i].getTorrent().getHash(), new Integer(i));
			
		}
		
		String[] search_items = null;
		
		String search = "";
		
		boolean bSearch = false;
		
		/* options menu */
		//t.setParam( "page_js", page_url + "?d=o");
		
		//t.setParam("op_set_msg",  op_set_msg.equals("") ? getLocalisedMessageText("template.options.currentSettings") : op_set_msg);
		
		t.setParam("max_active_txt", getMessageText("ConfigView.label.maxactivetorrents"));
		t.setParam("max_active_seed_txt", getMessageText("ConfigView.label.queue.maxactivetorrentswhenseeding"));
		t.setParam("max_dls_txt", getMessageText("ConfigView.label.maxdownloads"));
		t.setParam("max_conn_pertorrent_txt", getMessageText("ConfigView.label.max_peers_per_torrent"));
		t.setParam("max_conn_txt", getMessageText("ConfigView.label.max_peers_total"));
		t.setParam("max_down_txt", getMessageText("ConfigView.label.maxdownloadspeed"));
		t.setParam("max_up_txt", getMessageText("ConfigView.label.maxuploadspeed"));
		t.setParam("max_up_seed_txt", getMessageText("ConfigView.label.maxuploadspeedseeding"));
		t.setParam("max_up_speed_auto_txt", getMessageText("template.options.autospeed"));
		t.setParam("max_ups_txt", getMessageText("ConfigView.label.maxuploads"));
		t.setParam("max_ups_seed_txt", getMessageText("ConfigView.label.maxuploadsseeding"));
		t.setParam("comp_tab_txt", getLocalisedMessageText("azhtmlwebui.completedtab"));
		t.setParam("pagination_per_page_txt", getMessageText("template.options.pagination.per_page_txt"));

		t.setParam("options_set", getLocalisedMessageText("template.options.link.set"));
		
		//t.setParam("torrent_op_on", true);
		
		//t.setParam("torrent_refresh_on", false);
		
		//t.setParam("search", "");
		
		try {
			
		// parse get params.
		
		if ( params != null ){
			
			/*
			 * Refresh quicker when action performed
			 */
			
			String refresh		= (String)params.get("refresh");
			
			if(refresh != null) {
				
				int rate;
				try {
					rate = Integer.parseInt(refresh);
				} catch(Exception ex) {
					ex.printStackTrace();
					rate = refresh_rate;
				}
				
				refresh_rate = (rate>4)? rate : 5;
				
				t.setParam("refresh_rate", refresh_rate);
			}
			
			String op_set_msg = "";
			
			if(!view_mode) {

			/*
			 * Start a download
			 */
			
			String start 		= (String)params.get("start");
			
			if(start != null) {
				
				t.setParam("refresh_rate", "3");
				Download[] Torrents = new Download[1];
				
				if( start.equals("alld") ) {
					Torrents = getDLDownloadingTorrentsByPos();
					
				} else if( start.equals("alls") ) {
					Torrents = getDLSeedingTorrentsByPos( false );
					
				} else {
					
					Download[] Torrents2 = download_manager.getDownloads(true);

					for (int i=0; i < Torrents2.length ;i++) {
						
						if (formatters.encodeBytesToString(Torrents2[i].getTorrent().getHash()).equals(start)) {
								
								Torrents[0] = Torrents2[i];
						}
					}
				}
				
				startTorrent(Torrents);
			}
			
			/*
			 * Force Start a download / seed
			 */
			
			String fstart 		= (String)params.get("fstart");
			
			if(fstart != null) {
				
				t.setParam("refresh_rate", "3");
				
				Download[] TorrentsList = download_manager.getDownloads(true);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					String hash = formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash());
					
					if (hash.equals(fstart)) {
						
						try {
							
							if(debugOn) System.out.println("Force Starting " + TorrentsList[i].getName());
						
							TorrentsList[i].setForceStart(true);
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Set Force Start to false
			 */
			
			String fstop 		= (String)params.get("fstop");
			
			if(fstop != null) {
				
				t.setParam("refresh_rate", "3");
				
				Download[] TorrentsList = download_manager.getDownloads(true);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					String hash = formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash());
					
					if (hash.equals(fstop)) {
						
						try {
							
							if(debugOn) System.out.println("Setting Force Start to false for " + TorrentsList[i].getName());
						
							TorrentsList[i].setForceStart(false);
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Stop a download / seed
			 */
			
			String stop 		= (String)params.get("stop");
			Download[] Torrents = new Download[1];
			
			if(stop != null) {
				
				t.setParam("refresh_rate", "3");
				
				if(stop.equals("alld")) {
					
					Torrents = getDLDownloadingTorrentsByPos();
					
				} else if(stop.equals("alls")) {
					
					Torrents = getDLSeedingTorrentsByPos( true );
					
				} else {
					
					Download[] Torrents2 = download_manager.getDownloads(false);
				
					for (int i=0; i < Torrents2.length ;i++) {
						
						String hash = formatters.encodeBytesToString(Torrents2[i].getTorrent().getHash());
						
						if (hash.equals(stop)) {
							
							Torrents[0] = Torrents2[i];
								
						}
					}
				}
				
				stopTorrents(Torrents);
			}
			
			/*
			 * Pause/Resume downloads and seeds
			 */
			
			String act = (String)params.get("act");
			
			if(act != null) {
				
				if (act.equals("pause") && download_manager.canPauseDownloads()){
				
					download_manager.pauseDownloads();
					
					t.setParam("pause_resume", "Resume");
					t.setParam("pause_resume_href", "resume");
					
				} else if(act.equals("resume") && download_manager.canResumeDownloads()) {
					
					download_manager.resumeDownloads();
					
					t.setParam("pause_resume", "Pause");
					t.setParam("pause_resume_href", "pause");
				}
			}
			
			/*
			 * Publish a torrent
			 */
			
			String publish 		= (String)params.get("pub");
			
			if(publish != null) {
				
				Download[] TorrentsList = download_manager.getDownloads(true);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					String hash = formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash());
					
					if (hash.equals(publish)) {
						
						try {
							
							if(debugOn) System.out.println("Publishing " + TorrentsList[i].getName());
						
							tracker.publish( TorrentsList[i].getTorrent() );
							
							torrentsHostedorPublished.put( TorrentsList[i].getTorrent().getHash(), new Integer(torrentsHostedorPublished.size() ));
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							continue;
							
						}
					}
				}
			}
			
			
			/*
			 * Host a torrent
			 */
			
			String host 		= (String)params.get("host");
			
			if(host != null) {
				
				Download[] TorrentsList = download_manager.getDownloads(true);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					String hash = formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash());
					
					if (hash.equals(host)) {
						
						try {
							
							if(debugOn) System.out.println("Hosting " + TorrentsList[i].getName());
						
							tracker.host( TorrentsList[i].getTorrent(), true );
							
							torrentsHostedorPublished.put( TorrentsList[i].getTorrent().getHash(), new Integer(torrentsHostedorPublished.size() ));
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Remove a torrent from the tracker
			 */
			
			String remove_from_tracker 		= (String)params.get("trm");
			
			if(remove_from_tracker != null) {
				
				for (int i=0; i < trackerTorrent.length ;i++) {
					
					String hash = formatters.encodeBytesToString(trackerTorrent[i].getTorrent().getHash());
					
					if (hash.equals(remove_from_tracker)) {
						
						try {
							
							if(debugOn) System.out.println("Removing " + trackerTorrent[i].getTorrent().getName() + " from the tracker");
						
							trackerTorrent[i].remove();
							
							torrentsHostedorPublished.remove( trackerTorrent[i].getTorrent().getHash() );
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							continue;
							
						}
						

					}
				}
			}
			
			/*
			 * Move up
			 */
			
			String move_up	= (String)params.get("mup");
			
			if (move_up != null) {
				
				Download[] TorrentsList = download_manager.getDownloads(false);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(move_up)) {
						
						try {
							
							if(debugOn) System.out.println("Moving up torrent: " + TorrentsList[i].getName());
								

							TorrentsList[i].moveUp();

							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							if(debugOn) System.out.println("Moving up " + TorrentsList[i].getName() + " failed");
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Move to top
			 */
			
			String top	= (String)params.get("mtop");
			
			if (top != null) {
				
				Download[] TorrentsList = download_manager.getDownloads(false);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(top)) {
						
						try {
							
							int pos = TorrentsList[i].getPosition();
							
							if(debugOn) System.out.println("Moving torrent: " + TorrentsList[i].getName() + " to top from position: " + TorrentsList[i].getPosition());
										
							if((String)params.get( "d" ) != null) {
									
								for(int j=1; j < pos; j++) {
								
									TorrentsList[i].moveUp();
								
								}
							
							} else {
								
								for(int j=1; j < pos; j++) {
									
									TorrentsList[i].moveUp();
								
								}
							}
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							if(debugOn) System.out.println("Moving of " + TorrentsList[i].getName() + " to top failed");
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Move down
			 */
			
			String move	= (String)params.get("mdn");
			
			if (move != null) {
				
				Download[] TorrentsList = download_manager.getDownloads(false);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(move)) {
						
						try {
							
							if(debugOn) System.out.println("Moving down torrent: " + TorrentsList[i].getName());
								

							TorrentsList[i].moveDown();

							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							if(debugOn) System.out.println("Moving down " + TorrentsList[i].getName() + " failed");
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Move to bottom
			 */
			
			String bottom	= (String)params.get("mbot");
			
			if (bottom != null) {
				
				Download[] TorrentsList = download_manager.getDownloads(false);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(bottom)) {
						
						try {
							
							int pos = TorrentsList[i].getPosition();
							
							if(debugOn) System.out.println("Moving torrent: " + TorrentsList[i].getName() + " to bottom from position: " + pos);
								

							if((String)params.get( "d" ) != null) { 	// Seeding torrents
								
								int nbtorrents = getSeedingTorrentsCount(true, null);
								
								for(int j=0; j < nbtorrents-pos; j++) {
								
									TorrentsList[i].moveDown();
								
								}
							
							} else {											// Downloading torrents
								
								int nbtorrents = getDownloadingTorrentsCount(null);
								
								for(int j=0; j < nbtorrents-pos; j++) {
									
									TorrentsList[i].moveDown();
								
								}
							}

							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							if(debugOn) System.out.println("Moving of " + TorrentsList[i].getName() + " to bottom failed");
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Remove a torrent, delete...
			 */
			
			String remove		= (String)params.get("rem");
			String delete		= (String)params.get("del");
			
			if(remove != null && delete != null) {
				
				Download[] TorrentsList = download_manager.getDownloads(false);
				
				for (int i=0; i < TorrentsList.length ;i++) {
					
					if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(remove)) {
						
						try {
								
							if(TorrentsList[i].getState() != Download.ST_STOPPED && TorrentsList[i].getState() != Download.ST_STOPPING) {
								
								TorrentsList[i].stop();
								
							}
							
							if (delete.equals("0")) {
								TorrentsList[i].remove(false, false);
								if(debugOn) System.out.println("Removing " + TorrentsList[i].getName());
							} else if ( delete.equals("1")) {
								TorrentsList[i].remove(true, false);
								if(debugOn) System.out.println("Removing AND DELETING torrent of " + TorrentsList[i].getName());
							} else if ( delete.equals("2")) {
								TorrentsList[i].remove(false, true);
								if(debugOn) System.out.println("Removing AND DELETING data of " + TorrentsList[i].getName());
							} else if ( delete.equals("3")) {
								TorrentsList[i].remove(true, true);
								if(debugOn) System.out.println("Removing AND DELETING torrent AND data of " + TorrentsList[i].getName());
							}
							
							
							
						} catch (Exception e) {
							
							e.printStackTrace();
							
							if(debugOn) System.out.println("Removing of " + TorrentsList[i].getName() + " failed");
							
							continue;
							
						}
					}
				}
			}
			
			/*
			 * Add a download through an URL
			 */
			
			final String up_url = (String)params.get("upurl");
			
			if(up_url != null) {
				
				String d_url = URLDecoder.decode(up_url, DEFAULT_ENCODING);
				
				String url_load_msg = "";
					
				  String[] urls = d_url.split(";"); 
				  
				  for(int i =0; i < urls.length; i++ ) {
					  
					  try {
					
				          URL url = new URL(urls[i]);
				          
				          ResourceDownloader rd = plugin_interface.getUtilities().getResourceDownloaderFactory().create(url);
				            
				          InputStream is = rd.download();
				          
				          final Torrent torrent = plugin_interface.getTorrentManager().createFromBEncodedInputStream(is);
				          
				          download_manager.addDownload(torrent);
							
				          url_load_msg += escapeXML(torrent.getName()) + " " + getLocalisedMessageText("template.upload.url.msg.success") + "<br>";
						
						} catch (Exception e) {
							
							if(debugOn) System.out.println("Loading of " + urls[i] + " failed");
							
							url_load_msg += getLocalisedMessageText("template.upload.url.msg.error") + " " + escapeXML(urls[i]) + "<br>";
							
							e.printStackTrace();
							
						}
				  }
				  
				  t.setParam("url_load_msg", url_load_msg);
			}
			
			/*
			 * Add a local (distant) torrent
			 */
			
			final String up_file = (String)params.get("local");
			
			if(up_file != null) {
				
				try {
					
					String up_dir = utilities.getAzureusUserDir() + File.separator + "torrents" + System.getProperty("file.separator");
					
				    File dir = new File( up_dir );
				    if (!dir.exists()) {
				      dir.mkdirs();
				    }

					uploadTorrent(pageRequest, up_dir);
					
					String torrent_msg = "";
					
					/*for(int i=0; i<t_uploaded_names.length;i++){
						if(t_uploaded_names[i][0] != null) System.out.println(i + ".0: " + t_uploaded_names[i][0] + " - " + i + ".1: " + t_uploaded_names[i][1]);
					}*/
					for(int i=0; i<t_uploaded_names.length;i++){
						if(t_uploaded_names[i][0] != null){
							if(t_uploaded_names[i][1].equals("1")) {
								torrent_msg += escapeXML(t_uploaded_names[i][0]) + " " + 
												getLocalisedMessageText("template.upload.local.msg.success") + " " + up_dir + "<br/>";
							} else {
								torrent_msg = getLocalisedMessageText("template.upload.local.msg.error") + " " + escapeXML(t_uploaded_names[i][0]) + "<br/>";
							}
						}
					}
					t.setParam("url_load_msg", torrent_msg );
					
					t_uploaded_names = new String[3][2];
					
				} catch (Exception e) {
					
					if(debugOn) System.out.println("Loading of local torrent failed");
					
					t.setParam("url_load_msg", getLocalisedMessageText("template.upload.local.msg.error"));
					
					e.printStackTrace();
					
				}
			}
			
			
			
			/*
			 * Search downloads
			 */
			
			search = (String)params.get("search");
			
			if(search != null && !search.equals("")) {
				t.setParam("search", URLEncoder.encode(search, "UTF-8"));
				String search2 = URLDecoder.decode(search, DEFAULT_ENCODING);
				search_items = search2.split( " " );
				t.setParam("search_decoded", escapeXSS(search2));
				bSearch = true;
				t.setParam("search_on", true);
				
				t.setParam( "page_url", page_url + "?search=" + search);
			}
		} // end view mode full
			
			
			
			/*
			 * Change tab displayed (Downloads / Seeds / Completed / Options / Upload)
			 */
			
			String display 	= (String)params.get( "d" );
			
			t.setParam( "page_js", page_url + ((bSearch?("?search=" + search):"")) );
			
			if(display != null) {
				
				if (display.equals("s") || (display.equals("c") && !bCompletedTab))	{						// Seeding Torrents Page
					
					t.setParam( "page_url", page_url + "?d=s" + escapeXML((bSearch?("&search=" + search):"")) );
					t.setParam( "page_js", page_url + "?d=s" + ((bSearch?("&search=" + search):"")) );
					t.setParam( "page", page_url + "?d=s");
					
					t.setParam( "display_state", 1);
					
					t = populateTemplate(t, "up", search_items);
					
				} else if(display.equals("c") && bCompletedTab) {	// Completed Torrents Page
					
					t.setParam( "page_url", page_url + "?d=c" + escapeXML((bSearch?("&search=" + search):"")) );
					t.setParam( "page_js", page_url + "?d=c" + ((bSearch?("&search=" + search):"")) );
					t.setParam( "page", page_url + "?d=c");
					
					t.setParam( "display_state", 2);
					
					t = populateTemplate(t, "co", search_items);
					
				} else if (display.equals("o")) {					// Options Page
					
					t.setParam( "page_js", page_url + "?d=o");
					
					t.setParam("op_set_msg",  op_set_msg.equals("") ? getLocalisedMessageText("template.options.currentSettings") : op_set_msg);
					
					t.setParam("max_active_txt", getMessageText("ConfigView.label.maxactivetorrents"));
					t.setParam("max_active_seed_txt", getMessageText("ConfigView.label.queue.maxactivetorrentswhenseeding"));
					t.setParam("max_dls_txt", getMessageText("ConfigView.label.maxdownloads"));
					t.setParam("max_conn_pertorrent_txt", getMessageText("ConfigView.label.max_peers_per_torrent"));
					t.setParam("max_conn_txt", getMessageText("ConfigView.label.max_peers_total"));
					t.setParam("max_down_txt", getMessageText("ConfigView.label.maxdownloadspeed"));
					t.setParam("max_up_txt", getMessageText("ConfigView.label.maxuploadspeed"));
					t.setParam("max_up_seed_txt", getMessageText("ConfigView.label.maxuploadspeedseeding"));
					t.setParam("max_up_speed_auto_txt", getMessageText("template.options.autospeed"));
					t.setParam("max_ups_txt", getMessageText("ConfigView.label.maxuploads"));
					t.setParam("max_ups_seed_txt", getMessageText("ConfigView.label.maxuploadsseeding"));
					t.setParam("comp_tab_txt", getLocalisedMessageText("azhtmlwebui.completedtab"));

					t.setParam("options_set", getLocalisedMessageText("template.options.link.set"));
					
					t.setParam("torrent_op_on", true);
					
					t.setParam("torrent_refresh_on", false);
					
				} else if (display.equals("u") && !view_mode){		// Upload Page
					
					t.setParam( "page_url", page_url + "?d=u");
					
					t.setParam("upload_choose", getLocalisedMessageText("template.upload.choose"));
					t.setParam("upload_local", getLocalisedMessageText("template.upload.local"));
					t.setParam("upload_url", getLocalisedMessageText("template.upload.url"));
					t.setParam("upload_go", getLocalisedMessageText("template.upload.go"));
					
					t.setParam("torrent_up_on", true);
					
					t.setParam("torrent_refresh_on", false);
					
					String dex_str = "";
					
					String data_dir = plugin_config.getStringParameter("Default save path");
					
					boolean useDefDataDir = plugin_config.getBooleanParameter("Use default data dir");
					
					if(debugOn) System.out.println("data_dir: " + data_dir + " / useDefDataDir: " + useDefDataDir);
					
					if ( (data_dir == null || data_dir.length() == 0)){
						
						dex_str = " " + getLocalisedMessageText("template.upload.auto.stop");
						t.setParam("url_load_msg", dex_str);
						
					} else {
						
						if ( !useDefDataDir){
							
							dex_str = " " + getLocalisedMessageText("template.upload.auto.warn");
							t.setParam("url_load_msg", dex_str);
							
						}
					}
					
					
				} else if (display.equals("d")) {
					// Display torrent details
					String thash = (String)params.get( "t" );
					
					thash = URLEncoder.encode(thash, "UTF-8");
					
					t.setParam( "page_url", page_url + "?d=d&t="+thash);
					t.setParam( "page_js", page_url + "?d=d&t="+thash);
					t.setParam( "page", page_url + "?d=d&t="+thash);
					
					t.setParam("torrent_details", true);
					
					Download[] TorrentsList = download_manager.getDownloads(true);
					Download download = null;

					for (int i=0; i < TorrentsList.length ;i++) {						
						if (formatters.encodeBytesToString(TorrentsList[i].getTorrent().getHash()).equals(thash)) {
							download = TorrentsList[i]; 
						}
					}
					if (download == null) {
						t.setParam("error", true);
						t.setParam("error_header", getLocalisedMessageText("template.details.error_header"));
						t.setParam("errormsg", getLocalisedMessageText("template.details.notfound"));
					} else {
						DownloadStats stats = download.getStats();
						Torrent torrent = download.getTorrent();
						DecimalFormat df = new DecimalFormat(" ##0.0%");
						
						t.setParam("transfer_header", getLocalisedMessageText("GeneralView.section.transfer"));
						t.setParam("name_msg", getLocalisedMessageText("GeneralView.label.filename"));
						t.setParam("name", download.getName());
						t.setParam("status_msg", getLocalisedMessageText("MyTorrentsView.status")+" :");
						t.setParam("status", stats.getStatus(true));
						t.setParam("hash_msg", getLocalisedMessageText("GeneralView.label.hash"));
						t.setParam("hash", formatters.encodeBytesToString(torrent.getHash()));
						t.setParam("saving_to_msg", getLocalisedMessageText("GeneralView.label.savein"));
						t.setParam("saving_to", download.getSavePath());
						t.setParam("created_by_msg", getLocalisedMessageText("template.details.createdby"));
						t.setParam("created_by", torrent.getCreatedBy());
						t.setParam("created_on_msg", getLocalisedMessageText("GeneralView.label.creationdate"));
						t.setParam("created_on", formatters.formatDate(torrent.getCreationDate()*1000) );
						t.setParam("comment_msg", getLocalisedMessageText("GeneralView.label.comment"));
						t.setParam("comment", torrent.getComment());
						t.setParam("announce_url_msg", getLocalisedMessageText("GeneralView.label.trackerurl"));
						t.setParam("announce_url", torrent.getAnnounceURL().toString());
						t.setParam("downloaded_msg", getLocalisedMessageText("GeneralView.label.downloaded"));
						t.setParam("downloaded", formatters.formatByteCountToKiBEtc(stats.getDownloaded()));
						t.setParam("uploaded_msg", getLocalisedMessageText("GeneralView.label.uploaded"));
						t.setParam("uploaded", formatters.formatByteCountToKiBEtc(stats.getUploaded()));
						t.setParam("ratio_msg", getLocalisedMessageText("GeneralView.label.shareRatio"));
						t.setParam("ratio", df.format( ((float)stats.getUploaded()) / stats.getDownloaded() ));
						t.setParam("downloadspeed_msg", getLocalisedMessageText("GeneralView.label.downloadspeed"));
						t.setParam("downloadspeed", formatters.formatByteCountToKiBEtcPerSec(stats.getDownloadAverage()));
						t.setParam("uploadspeed_msg", getLocalisedMessageText("GeneralView.label.uploadspeed"));
						t.setParam("uploadspeed", formatters.formatByteCountToKiBEtcPerSec(stats.getUploadAverage()));

						t.setParam("info_header", getLocalisedMessageText("GeneralView.section.info"));
						t.setParam("size_msg", getLocalisedMessageText("GeneralView.label.totalsize"));
						t.setParam("size", formatters.formatByteCountToKiBEtc(torrent.getSize()) );
						t.setParam("elapsed_msg", getLocalisedMessageText("GeneralView.label.timeelapsed"));
						t.setParam("elapsed", stats.getElapsedTime());
						t.setParam("eta_msg", getLocalisedMessageText("GeneralView.label.remaining"));
						t.setParam("eta", stats.getETA());
						
						t.setParam("connected", getLocalisedMessageText("GeneralView.label.connected"));
						t.setParam("conleechers_msg", getLocalisedMessageText("GeneralView.label.peers"));
						t.setParam("conseeds_msg", getLocalisedMessageText("GeneralView.label.seeds"));
						try {
							PeerManagerStats peers = download.getPeerManager().getStats();
							t.setParam("conleechers", peers.getConnectedLeechers());
							t.setParam("conseeds", peers.getConnectedSeeds());
						} catch (Exception e) {
							t.setParam("conleechers", "0");
							t.setParam("conseeds", "0");
						}
						int scrape_nonseed = download.getLastScrapeResult().getNonSeedCount();
						int scrape_seed = download.getLastScrapeResult().getSeedCount();
						t.setParam("scrapeleechers", (scrape_nonseed!=-1)?scrape_nonseed:0);
						t.setParam("scrapeseeds", (scrape_seed!=-1)?scrape_seed:0);
						t.setParam("in_swarm", getLocalisedMessageText("GeneralView.label.in_swarm"));
						
						t.setParam("files_header", getLocalisedMessageText("FilesView.title.full"));
						t.setParam("filename_hd", getLocalisedMessageText("FilesView.name"));
						t.setParam("filesize_hd", getLocalisedMessageText("FilesView.size"));
						t.setParam("filepercent_hd", getLocalisedMessageText("FilesView.%"));
						t.setParam("filepriority_hd", getLocalisedMessageText("FilesView.priority"));
						Vector files = new Vector();
						DiskManagerFileInfo[] dmfi = download.getDiskManagerFileInfo();

						for (int i=0; i<dmfi.length; i++) {
							Hashtable fi = new Hashtable();
							File f = dmfi[i].getFile();
							files.add(fi);
							
							fi.put("filename", f.getName());
							fi.put("size", formatters.formatByteCountToKiBEtc(dmfi[i].getLength()));
							fi.put("percent", df.format(((float)dmfi[i].getDownloaded())/dmfi[i].getLength()));
							String prio;
							if (dmfi[i].isPriority()) {
								prio = getLocalisedMessageText("FileItem.high");
								fi.put("p_high", "1");
							} else if (dmfi[i].isSkipped()) {
								prio = getLocalisedMessageText("FileItem.donotdownload");
								fi.put("p_dnd", "1");
							} else if (dmfi[i].isDeleted()) {
								prio = getLocalisedMessageText("template.details.deleted");
								fi.put("p_del", "1");
							} else {
								prio = getLocalisedMessageText("FileItem.normal");
								fi.put("p_norm", "1");
							}
							fi.put("priority", prio);
						}
						t.setParam("files", files);
						
					}

				}
			} else {
				t = populateTemplate(t, "down", search_items);

			}
			
		}else {
			
			t = populateTemplate(t, "down", null);
			
		}
		
		if ( torrent_categories != null ){
			
			Vector	category_names = new Vector();
		
			String[]	x = torrent_categories.getDefinedValues();
			
			for (int j=0;j<x.length;j++){
				
				Hashtable ht = new Hashtable();
				
				ht.put( "name", escapeXML(x[j]) );
				
				category_names.add(ht);
			}
			
			Hashtable ht = new Hashtable();
			
			ht.put( "name", CATEGORY_UNKNOWN );
			
			category_names.add(ht);
			
			t.setParam( "categories_list", category_names );
		}
		
		t.setParam("nb_torrents_dl", getDownloadingTorrentsCount(search_items));
		t.setParam("nb_torrents_cd", getSeedingTorrentsCount(!bCompletedTab, search_items));
		if(bCompletedTab) t.setParam("nb_torrents_co", getCompletedTorrentsCount(search_items));
		
		t.setParam("max_upload_speed", (isSeedingOnly && max_ul_speed_seed_enabled)?max_ul_speed_seed==0? getLocalisedMessageText("template.speed.unlimited")+"*" : formatters.formatByteCountToKiBEtcPerSec( max_ul_speed_seed * 1024 )+"*":max_ul_speed==0? getLocalisedMessageText("template.speed.unlimited") : formatters.formatByteCountToKiBEtcPerSec( max_ul_speed * 1024 ));
		
		t.setParam( "azureus_version", plugin_interface.getAzureusVersion());
		
		t.setParam( "plugin_version", plugin_interface.getPluginVersion());
		
		String data = t.output();
		
		PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os, DEFAULT_ENCODING ), true);
		
		pw.write( data );
		
		pw.flush();
		
		return( true );
		
	}catch( Throwable e ){
		
		e.printStackTrace();
		
		os.write( e.toString().getBytes());
		
		return( true );
	}
	}


	private void stopTorrents(Download[] torrents) {
		for (int i=0; i < torrents.length ;i++) {
				
			try {
				
				if(torrents[i].getState() != 6 && torrents[i].getState() != 7) {
				
					if(debugOn) System.out.println("Stopping " + torrents[i].getName() + " in state " + torrents[i].getState());
				
					torrents[i].stop();
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
				
				if(debugOn) System.out.println("Error stopping " + torrents[i].getName() + " in state " + torrents[i].getState());
				
				continue;
			}
		}
	}


	private void startTorrent(Download[] torrents) {
			
		for (int i=0; i < torrents.length ;i++) {
				
			if(debugOn) System.out.println("Trying to start " + torrents[i].getName() + " in state " + torrents[i].getState());
				
			try {
				
				if(torrents[i].getState() != 4 && torrents[i].getState() != 5 && torrents[i].getState() != 9) {
				
					if(debugOn) System.out.println("Starting " + torrents[i].getName());
				
					torrents[i].restart();
				
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
				
				if(debugOn) System.out.println("Error starting " + torrents[i].getName() + " in state " + torrents[i].getState());
				
				continue;
			}
		}
	}
	
private Template populateTemplate(Template t, String type, String[] s_items) {
		
		try {
			
			List torrents = new ArrayList();
			
			if (type.equals("down")) {
				
				t.setParam("torrent_dl_on", true);
				
				torrents = getListDownloadingTorrentsByPos(s_items);
				
			} else if(type.equals("up")) {
			
				t.setParam("torrent_ul_on", true);
				
				t.setParam("torrent_uc_on", true);
				
				torrents = getListSeedingTorrentsByPos(s_items);
			} else {
			
				t.setParam("torrent_co_on", true);
				
				t.setParam("torrent_uc_on", true);
				
				torrents = getListCompletedTorrentsByPos(s_items);
			}
			
			Vector 	torrent_info 		= new Vector();
		
		long total_download = 0;
		long total_upload = 0;
		long total_size = 0;
		long total_downloaded = 0;
		long total_uploaded = 0;
		
		long total_torrents = torrents.size();
		
		if(debugOn) System.out.println("*** "+type+" ***");
		
		// loop downloads
		for (int i=0;i<total_torrents;i++){
			
			Hashtable t_row 			= new Hashtable();
			
			torrent_info.add( t_row );
			
			Download torrent = (Download) torrents.get(i);
			
			String torrent_name = torrent.getName();
			
			if (torrent_name.length() > 35) {
				t_row.put("torrent_short_name", escapeXML(torrent_name.substring(0, 35)) + "...");
				t_row.put("torrent_name_too_long", "1");
			} else {
				t_row.put("torrent_short_name", escapeXML(torrent_name));
				t_row.put("torrent_name_too_long", "0");
			}
			
			t_row.put("torrent_name", escapeXML(torrent_name));
			
			t_row.put("torrent_url_name", formatters.encodeBytesToString( torrent.getTorrent().getHash()));
			
			t_row.put("torrent_hash_short", formatters.encodeBytesToString( torrent.getTorrent().getHash()));
			
			t_row.put("torrent_position", "" + torrent.getPosition());
			
			boolean bTorrentNoMagnet = torrent.getTorrent().isPrivate() || !torrent.getTorrent().isDecentralisedBackupEnabled();
			
			t_row.put("torrent_has_magnet", bTorrentNoMagnet?"0":"1");
			
			try{
				t_row.put("torrent_magnet", ""+torrent.getTorrent().getMagnetURI());
			}catch(TorrentException tex) {
				tex.printStackTrace();
			}
			
			t_row.put("torrent_ontracker", "0");
			
			Set entries = torrentsHostedorPublished.entrySet();
			
			Iterator iterator = entries.iterator();
			
			while(iterator.hasNext()){
				
				Map.Entry entry = (Map.Entry)iterator.next();
				
				if (entry.getKey().equals(torrent.getTorrent().getHash())) {

					t_row.put("torrent_ontracker", "1");
				}
				
			}

			String	c = torrent.getAttribute( torrent_categories );
			
			if ( c == null || c.equals( "Categories.uncategorized" )){
				
				c = CATEGORY_UNKNOWN;
			}

			t_row.put("torrent_category", escapeXML(c));
			
			long torrent_peers = 0;
			long torrent_seeds = 0;
			
			boolean isDownloading = false;
			boolean isSeeding	= false;
			boolean isStartable = false;		
			boolean isFStart	= false;
			
			int	status = torrent.getState();
			String	status_str = torrent.getStats().getStatus(true);
			
			if ( status == Download.ST_STOPPED ){
				
				isStartable = true;
				
			}else if ( status == Download.ST_DOWNLOADING ){
				
				isDownloading = true;
				
				try {
					torrent_seeds = torrent.getPeerManager().getStats().getConnectedSeeds() ;
				} catch (Throwable e) {
					torrent_seeds = 0 ;
				}
				
				try {
					torrent_peers = torrent.getPeerManager().getStats().getConnectedLeechers() ;
				} catch (Throwable e) {
					torrent_peers = 0 ;
				}
				
			}else if ( status == Download.ST_SEEDING ){
				
				isSeeding = true;
				
				try {
					torrent_peers = torrent.getPeerManager().getStats().getConnectedLeechers() ;
				} catch (Throwable e) {
					torrent_peers = 0;
				}
			}
			
			if( torrent.isForceStart()) isFStart = true;
			
			t_row.put("torrent_isFStart", isFStart? "1":"0");
			
			t_row.put("torrent_status", status_str);
			
			t_row.put("torrent_isDLing", isDownloading? "1":"0");
			
			t_row.put("torrent_isCDing", isSeeding? "1":"0");
			
			t_row.put("torrent_isStartable", isStartable? "1":"0");
			
			long torrent_size = torrent.getTorrent().getSize();
			total_size += torrent_size;
			
			t_row.put("torrent_size", torrent_size<=0 ? "N/A": formatters.formatByteCountToKiBEtc(torrent_size));
			
			t_row.put("torrent_percent_done", "" + (torrent.getStats().getCompleted()/10.0f));
			
			long torrent_downloaded = torrent.getStats().getDownloaded();
			long torrent_uploaded = torrent.getStats().getUploaded();
			
			total_downloaded += torrent_downloaded;
			total_uploaded += torrent_uploaded;
			
			t_row.put("torrent_downloaded", "" + formatters.formatByteCountToKiBEtc(torrent_downloaded));
			
			t_row.put("torrent_uploaded", "" + formatters.formatByteCountToKiBEtc(torrent_uploaded));
			
			long torrent_dl_speed = torrent.getStats().getDownloadAverage();
			long torrent_ul_speed = torrent.getStats().getUploadAverage();
			
			total_download += torrent_dl_speed;
			total_upload += torrent_ul_speed;
			
			t_row.put("torrent_dl_speed", formatters.formatByteCountToKiBEtcPerSec(torrent_dl_speed));
			
			t_row.put("torrent_ul_speed", formatters.formatByteCountToKiBEtcPerSec(torrent_ul_speed));
			
			String torrent_total_seeds = "" + torrent.getLastScrapeResult().getSeedCount();
			
			if ( torrent_total_seeds.equals("-1")) torrent_total_seeds = "-";
			
			t_row.put("torrent_seeds", "" + torrent_seeds);
			
			t_row.put("torrent_total_seeds", "" + torrent_total_seeds);
			
			String torrent_total_peers = "" + torrent.getLastScrapeResult().getNonSeedCount();
			
			if ( torrent_total_peers.equals("-1")) torrent_total_peers = "-";
			
			t_row.put("torrent_peers", "" + torrent_peers);
			
			t_row.put("torrent_total_peers", "" + torrent_total_peers);
			
			final NumberFormat sr_format;
			sr_format = NumberFormat.getInstance();
			sr_format.setMaximumFractionDigits(3);
			sr_format.setMinimumFractionDigits(1);
			
			float availf = torrent.getStats().getAvailability();
			t_row.put("torrent_avail", (availf == -1.0) ? "" : sr_format.format(availf) );
			
			int sr = torrent.getStats().getShareRatio();
			String share_ratio = "";
			
			if (sr ==  -1 ) {
				share_ratio = "&#8734;";
			} else {
				t_row.put("isFP", (sr < minSR) ? "1" : "0");
				
				share_ratio = sr_format.format( sr/1000.0f );
			}
			
			t_row.put("torrent_share_ratio", "" + share_ratio );
			
			t_row.put("torrent_eta", "" + torrent.getStats().getETA());
			
			//if(debugOn) System.out.println("Name: " + torrent_name + " / Status: " + status_str);
			
		} // end loop downloads
		
		t.setParam("total_dling_torrents", getDownloadingTorrentsCount());
		
		t.setParam("total_uling_torrents", getSeedingTorrentsCount(bCompletedTab?false:true));
		
		if(bCompletedTab) t.setParam("total_cted_torrents", getCompletedTorrentsCount());
		
		t.setParam("total_downloaded", formatters.formatByteCountToKiBEtc(total_downloaded));
		
		t.setParam("total_uploaded", formatters.formatByteCountToKiBEtc(total_uploaded));
		
		t.setParam("total_download", formatters.formatByteCountToKiBEtcPerSec(total_download));
		
		t.setParam("total_upload", formatters.formatByteCountToKiBEtcPerSec(total_upload));
		
		t.setParam("total_size", formatters.formatByteCountToKiBEtc(total_size));
		
		t.setParam("total_torrents", "" + total_torrents);
		
		t.setParam("torrent_info", torrent_info);
		
		return t;
		
	}catch( Throwable e ){
		
		e.printStackTrace();
		
		System.out.println( e.toString().getBytes());
		
		return( t );
	}
	}

public PluginInterface getPluginInterface() {
 return plugin_interface; 
}

public DownloadManager getDownloadManager() {
  return download_manager;
}

public boolean isTorrentOK(Download d, String[] search_items) {
	
	if(search_items == null || search_items.equals("")) return true;
		
	String t_name = d.getTorrent().getName().toLowerCase();
	
	int search_count = 0;
	
	for (int j = 0 ; j < search_items.length ; j++) {
		
		if (t_name.indexOf(search_items[j].toLowerCase()) != -1) {
			search_count += 1;
			if(debugOn) System.out.println(t_name + " contains " + search_items[j] + " !! ");
		}
		
	}

	if (   search_count == search_items.length   ) {

		return true;
		
	}
		

	return false;
}

protected Download[] getDownloadsByPos() {
	
	Download[] TorrentsList = download_manager.getDownloads(true);
	
	Arrays.sort(
			TorrentsList,
			new Comparator()
			{
				public int
				compare(
					Object	o1,
					Object	o2 )
				{
					Download	d1 = (Download)o1;
					Download	d2 = (Download)o2;

					int	res = comparator.compare( "" + d1.getPosition(),  "" + d2.getPosition() );
					
					return(  res );
				}
			});
	
	Download[] down = TorrentsList;
	for(int i = 0; i < down.length; i++){
		if(debugOn) System.out.println("getDownloadsByPos:: " + down[i].getPosition());
	}
	
	return( TorrentsList );
}

protected List getListDownloadingTorrentsByPos(String[] s_items) {
	
//	Download[] downloads = getDownloadsByPos();
	Download[] downloads = download_manager.getDownloads(true);
	
	List torrents = new ArrayList();
	
	for (int i=0; i < downloads.length ;i++) {
		Download download = downloads[i];
		if (!download.isComplete(false)) {
			if( isTorrentOK(download, s_items)) torrents.add(downloads[i]);
		}
	}
	
	return torrents;
}

protected int getDownloadingTorrentsCount(String[] s_items) {
	
	if(s_items == null) return getDownloadingTorrentsCount();
	
	Download[] downloads = download_manager.getDownloads();
	
	int count = 0;
	
	for (int i=0; i < downloads.length ;i++) {
		if (!downloads[i].isComplete(false)) {
			if( isTorrentOK(downloads[i], s_items)) count++;
		}
	}
	return count;
}
protected int getDownloadingTorrentsCount() {
	
	Download[] downloads = download_manager.getDownloads();
	
	int count = 0;
	
	for (int i=0; i < downloads.length ;i++) {
		if (!downloads[i].isComplete(false)) {
			count++;
		}
	}
	return count;
}

protected int getSeedingTorrentsCount(boolean all, String[] s_items) {
	
	if(s_items == null) return getSeedingTorrentsCount( all );
	
	Download[] downloads = download_manager.getDownloads();
	
	int count = 0;
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && (all?true:downloads[i].getState() != Download.ST_STOPPED)) {
			if( isTorrentOK(downloads[i], s_items)) count++;
		}
	}
	return count;
}
/*
 * all: true: all seeding torrents
 * 		false: all non stopped seeding torrents
 */
protected int getSeedingTorrentsCount(boolean all) {
	
	Download[] downloads = download_manager.getDownloads();
	
	int count = 0;
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && (all?true:downloads[i].getState() != Download.ST_STOPPED)) {
			count++;
		}
	}
	return count;
}

protected int getCompletedTorrentsCount(String[] s_items) {
	
	if(s_items == null) return getCompletedTorrentsCount();
	
	Download[] downloads = download_manager.getDownloads();
	
	int count = 0;
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && downloads[i].getState() == Download.ST_STOPPED) {
			if( isTorrentOK(downloads[i], s_items)) count++;
		}
	}
	return count;
}

protected int getCompletedTorrentsCount() {
	
	Download[] downloads = download_manager.getDownloads();
	
	int count = 0;
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && downloads[i].getState() == Download.ST_STOPPED) {
			count++;
		}
	}
	return count;
}

protected List getListSeedingTorrentsByPos(String[] s_items) {
	
	Download[] downloads = getDownloadsByPos();
	
	List torrents = new ArrayList();
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && (bCompletedTab?downloads[i].getState() != Download.ST_STOPPED:true)) {
			if( isTorrentOK(downloads[i], s_items)) torrents.add(downloads[i]);
		}
	}
	
	return torrents;
}

protected List getListCompletedTorrentsByPos(String[] s_items) {
	
	Download[] downloads = getDownloadsByPos();
	
	List torrents = new ArrayList();
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && downloads[i].getState() == Download.ST_STOPPED) {
			if( isTorrentOK(downloads[i], s_items)) torrents.add(downloads[i]);
		}
	}
	
	return torrents;
}

protected List orderByPosition( List list) {
	Collections.sort(list, new Comparator(){
		public int compare(Object o1, Object o2){
			Download d1 = (Download)o1;
			Download d2 = (Download)o2;
			return comparator.compare(""+d1.getPosition(), ""+d2.getPosition());
		}
	});
	return list;
}

protected Download[] getDLDownloadingTorrentsByPos() {
	
//	Download[] downloads = getDownloadsByPos();
	Download[] downloads = download_manager.getDownloads(true);
	
	List dls = new ArrayList();
	for (int i=0; i < downloads.length ;i++) {
		if (!downloads[i].isComplete(false)) {
			dls.add(downloads[i]);
		}
	}
	dls = orderByPosition(dls);
	
	Download[] down = new Download[dls.size()];
	for(int i = 0; i < dls.size(); i++){
		down[i]=(Download)dls.get(i);
		if(debugOn) System.out.println("getDLDownloadingTorrentsByPos:: " + down[i].getPosition());
	}
	
	return down;
}

protected Download[] getDLSeedingTorrentsByPos( boolean completedTab) {
	
//	Download[] downloads = getDownloadsByPos();
	Download[] downloads = download_manager.getDownloads(true);
	
	List dls = new ArrayList();
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && (completedTab? (downloads[i].getState() != Download.ST_STOPPED):true)) {
			dls.add(downloads[i]);
		}
	}
	dls = orderByPosition(dls);
	
	Download[] down = new Download[dls.size()];
	for(int i = 0; i < dls.size(); i++){
		down[i]=(Download)dls.get(i);
		if(debugOn) System.out.println("getDLSeedingTorrentsByPos:: " + down[i].getPosition());
	}
	
	return down;
}

protected Download[] getDLCompletedTorrentsByPos() {
	
//	Download[] downloads = getDownloadsByPos();
	Download[] downloads = download_manager.getDownloads(true);
	
	List dls = new ArrayList();
	
	for (int i=0; i < downloads.length ;i++) {
		if (downloads[i].isComplete(false) && downloads[i].getState() == Download.ST_STOPPED) {
			dls.add(downloads[i]);
		}
	}
	dls = orderByPosition(dls);
	
	Download[] down = new Download[dls.size()];
	for(int i = 0; i < dls.size(); i++){
		down[i]=(Download)dls.get(i);
		if(debugOn) System.out.println("getDLCompletedTorrentsByPos:: " + down[i].getPosition());
	}
	
	return down;
}

protected void
uploadTorrent( TrackerWebPageRequest request, String upload_dir ) 		

	throws Exception
{

	try{
		if ( upload_dir == null){
			
			throw( new Exception( "Upload directory not found" ));
		}
		
		String	header = request.getHeader();
		
		int	pos = header.toLowerCase().indexOf( "content-type:");
		
		if( pos == -1 ){
			
			throw( new Exception( "content-type missing" ));
		}
		
		int	p2 = header.indexOf( "\r", pos );
		
		String	content_type = header.substring(pos,p2);
		
		
		int bp = content_type.toLowerCase().indexOf( "boundary=" );
		
		if ( bp == -1 ){
			
			throw( new Exception( "boundary missing" ));
		}
		
		String	boundary = content_type.substring(bp+9).trim();
		
		int	ep = boundary.indexOf( ';' );
		
		if ( ep != -1 ){
			
			boundary = boundary.substring(0,ep).trim();
		}
		
		HTMLWebUIFormDecoder.formField[]	fields = 
			new HTMLWebUIFormDecoder().decode( boundary, request.getInputStream());
		
		String	torrent_name		= null;
		
		InputStream	torrent_data	= null;
		
		try{
			int index = 0;
			
			for (int i=0;i<fields.length;i++){
				
				HTMLWebUIFormDecoder.formField	field = fields[i];
				
				String	name = field.getName();
				
				if ( name.indexOf( "upfile" ) != -1 ){
					
					torrent_name 	= field.getAttribute( "filename");
					
					torrent_data	= field.getInputStream();
					
					t_uploaded_names[index][0] = new String(torrent_name.getBytes(), "UTF-8");
					t_uploaded_names[index][1] = "1";
					
//					t_uploaded_name	= new String(torrent_name.getBytes(), "UTF-8");
					
					if ( torrent_name == null || torrent_name.equals("")){
						if(debugOn) System.out.println("torrent name empty : " + t_uploaded_names[index][0]);
						t_uploaded_names[index][1] = "0";
						continue;
						
						//throw( new UploadException(index, "'filename' attribute missing from upload" ));
					}
					if(debugOn) System.out.println("torrent name 1: " + t_uploaded_names[index][0]);
					int	ep1	= torrent_name.lastIndexOf("\\");
					int ep2	= torrent_name.lastIndexOf("/");
																	
					torrent_name = torrent_name.substring( Math.max( ep1, ep2 )+1 );
					if(debugOn) System.out.println("torrent name 2: " + t_uploaded_names[index][0]);
					if ( torrent_data == null ){
						
						t_uploaded_names[index][1] = "0";
						continue;
						
						//throw( new UploadException(index, "Failed to read upload data" ));
					}
					try{
						if(debugOn) {
							System.out.println("****************************");
							System.out.println("torrent_data: " + torrent_data);
							System.out.println("torrent_name: " + torrent_name);
							System.out.println("upload_dir: " + upload_dir);
							System.out.println("index: " + index);
						}
						upload( torrent_data, 
								torrent_name,
								upload_dir,
								index
								);
						
					} catch (Exception e) {
						t_uploaded_names[index][1] = "0";
					}
					index++;
				}
			}
			
		}finally{
									
			for (int i=0;i<fields.length;i++){
				
				HTMLWebUIFormDecoder.formField	field = fields[i];
				
				field.destroy();
			}
		}
		
	}catch( Throwable e ){
		
		e.printStackTrace();
		
	}
	
}
				
protected void
upload( InputStream is,
		String name,
		String up_dir,
		int nb
		) 		

	throws Throwable
{
	try {
	File		torrent_file;
	//File		data_directory	= new File(upload_dir.getValue());

	Torrent		torrent;
	
	//torrent_file 	= new File(upload_dir.getValue(), name );
	
	torrent_file 	= new File( up_dir , name );
	
	t_uploaded_names[nb][0] = name;
	t_uploaded_names[nb][1] = "0";

	byte[]	buffer = new byte[65536];
	
	FileOutputStream	fos = new FileOutputStream( torrent_file );
	
	while(true){
		
		int	len = is.read( buffer );
		
		if ( len <= 0 ){
			
			break;
		}
		
		fos.write( buffer, 0, len );
	}
	
	fos.close();		
		
	torrent = plugin_interface.getTorrentManager().createFromBEncodedFile( torrent_file );
	
	t_uploaded_names[nb][0] = torrent.getName();
	
	torrent.writeToFile( torrent_file );
		
	//download_manager.addDownload( torrent, torrent_file, data_directory );
	
	download_manager.addDownload( torrent );
	
	t_uploaded_names[nb][1] = "1";
		
	} catch( Throwable e ){
		
		e.printStackTrace();
		
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

protected File
getFileRoot()
{
	return( new File( file_root ));
}

protected String[]
getAvailableCSS()
{
	List	css = new ArrayList();
	
	File styles_dir = new File( getFileRoot(), "styles" );
	
	if ( styles_dir.exists()){
		
		File[]	files = styles_dir.listFiles(
							new FilenameFilter()
							{
								public boolean 
								accept(File dir, String name)
								{
									return( name.toLowerCase().endsWith( ".css" ));
								}
							});
		
		if ( files != null ){
			
			for ( int i=0;i<files.length;i++){
				
				css.add( files[i].getName());
			}
		}
	}
	
	for (int i=0;i<CONFIG_CSS_BUILTIN.length;i++){
		
		css.add( CONFIG_CSS_BUILTIN[i] );
	}
	
	String[]	res = new String[css.size()];
	
	css.toArray(res);
	
	Arrays.sort( res );

	return( res );
}


}
	
