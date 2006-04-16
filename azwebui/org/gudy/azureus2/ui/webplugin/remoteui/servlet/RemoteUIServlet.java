/*
 * File    : RemoteUIServlet.java
 * Created : 27-Jan-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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

package org.gudy.azureus2.ui.webplugin.remoteui.servlet;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.ui.webplugin.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.*;
import java.net.URI;
import java.net.URL;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.jar.AEJarBuilder;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.*;

import org.gudy.azureus2.pluginsimpl.remote.*;

public class 
RemoteUIServlet
	extends WebPlugin
{
	public static final int	DEFAULT_PORT	= 6883;
	
	protected static Properties	defaults = new Properties();
	
	static{
		
		defaults.put( WebPlugin.CONFIG_PORT, new Integer( DEFAULT_PORT ));
	}
	
	static String[] resource_icon_names = {
			//"ui/icons/start2.png",
			"ui/icons/openFolder16x12.gif",
			"ui/icons/start.gif",
			"ui/icons/forcestart.gif",
			"ui/icons/stop.gif",
			"ui/icons/delete.gif",
			"ui/icons/recheck.gif",		
			"ui/icons/up.gif",		
			"ui/icons/down.gif",		
			"ui/icons/host.gif",		
			"ui/icons/pause.gif",		
			"ui/icons/resume.gif",		
	};
	
	static String resource_names_prefix = "org/gudy/azureus2";
		
	static String[] resource_names = {
			
		"ui/common/UIImageRepository.class",
		
		"ui/swing/UISwingImageRepository.class",	
		
		"ui/swt/views/AbstractIView.class",
		"ui/swt/views/IView.class",
		"ui/swt/IconBarEnabler.class",
		
		"ui/webplugin/remoteui/applet/RemoteUIApplet.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanelAdaptor.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadModel.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView.class",
		"ui/webplugin/remoteui/applet/view/TableSorter.class",
		"ui/webplugin/remoteui/applet/view/TableMap.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModelListener.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModel.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView.class",
		"ui/webplugin/remoteui/applet/view/VWGridBagConstraints.class",
		"ui/webplugin/remoteui/applet/view/VWStatusAreaView.class",
		"ui/webplugin/remoteui/applet/view/VWLabel.class",
		"ui/webplugin/remoteui/applet/model/MDStatusAreaModel.class",
		"ui/webplugin/remoteui/applet/view/VWStatusEntryBorder.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModelPropertyChangeEvent.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadViewListener.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadSplitModel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadFilter.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadFullModel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadFilterModel.class",
		"ui/webplugin/remoteui/applet/model/MDTrackerModel.class",
		"ui/webplugin/remoteui/applet/view/VWAuthorisationView.class",
		"ui/webplugin/remoteui/applet/view/VWEncodingView.class",
				
		"core3/config/COConfigurationManager.class",
		"core3/config/COConfigurationListener.class",
		"core3/config/StringList.class",
		"core3/config/impl/ConfigurationManager.class",
		"core3/config/impl/ConfigurationParameterNotFoundException.class",
		"core3/config/ParameterListener.class",
		"core3/util/Constants.class",
		"core3/util/DisplayFormatters.class",
		"core3/util/Debug.class",
		"core3/util/AESemaphore.class",
		"core3/util/AEThread.class",
		"core3/util/AERunnable.class",
		"core3/util/AEMonSem.class",
		"core3/util/AEMonitor.class",
		"core3/util/AEDiagnostics.class",
		"core3/util/AEDiagnosticsEvidenceGenerator.class",
		"core3/util/AEDiagnosticsLogger.class",
		"core3/util/Average.class",
		"core3/util/IndentWriter.class",
		"core3/util/Timer.class",
		"core3/util/TimerEvent.class",
		"core3/util/TimerEventPeriodic.class",
		"core3/util/TimerEventPerformer.class",
		"core3/util/SimpleTimer.class",
		"core3/util/ThreadPool.class",
		"core3/util/ThreadPoolTask.class",
		"core3/util/SystemTime.class",
		"core3/util/jar/AEJarReader.class",		
		
		"core3/torrentdownloader/TorrentDownloaderException.class",
		"core3/torrent/TOTorrentException.class",
		"core3/security/SEPasswordListener.class",
		
		
		"pluginsimpl/remote/RPRequestDispatcher.class",
		"pluginsimpl/remote/RPException.class",
		"pluginsimpl/remote/RPFactory.class",
		"pluginsimpl/remote/RPRequest.class",
		"pluginsimpl/remote/RPRequestHandler.class",		
		"pluginsimpl/remote/RPRequestAccessController.class",		
		"pluginsimpl/remote/RPObject.class",
		"pluginsimpl/remote/RPReply.class",
		"pluginsimpl/remote/RPPluginInterface.class",
		"pluginsimpl/remote/RPPluginConfig.class",
		"pluginsimpl/remote/disk/RPDiskManagerFileInfo.class",
		"pluginsimpl/remote/download/RPDownloadManager.class",
		"pluginsimpl/remote/download/RPDownload.class",
		"pluginsimpl/remote/download/RPDownloadStats.class",
		"pluginsimpl/remote/download/RPDownloadAnnounceResult.class",
		"pluginsimpl/remote/download/RPDownloadScrapeResult.class",
		"pluginsimpl/remote/torrent/RPTorrentManager.class",
		"pluginsimpl/remote/torrent/RPTorrentDownloader.class",
		"pluginsimpl/remote/torrent/RPTorrent.class",
		"pluginsimpl/remote/ipfilter/RPIPFilter.class",
		"pluginsimpl/remote/tracker/RPTracker.class",
		"pluginsimpl/remote/tracker/RPTrackerTorrent.class",
		
		"pluginsimpl/remote/rpexceptions/RPDeserialiseClassMismatchException.class",
		"pluginsimpl/remote/rpexceptions/RPDeserialiseParseException.class",
		"pluginsimpl/remote/rpexceptions/RPInternalProcessException.class",
		"pluginsimpl/remote/rpexceptions/RPMalformedXMLException.class",
		"pluginsimpl/remote/rpexceptions/RPMethodAccessDeniedException.class",
		"pluginsimpl/remote/rpexceptions/RPNoObjectIDException.class",
		"pluginsimpl/remote/rpexceptions/RPObjectNoLongerExistsException.class",
		"pluginsimpl/remote/rpexceptions/RPRemoteMethodInvocationException.class",
		"pluginsimpl/remote/rpexceptions/RPThrowableAsReplyException.class",
		"pluginsimpl/remote/rpexceptions/RPUnknownMethodException.class",
		"pluginsimpl/remote/rpexceptions/RPUnsupportedInputTypeException.class",		
		
		"plugins/PluginInterface.class",
		"plugins/PluginListener.class",
		"plugins/PluginView.class",
		"plugins/PluginException.class",
		"plugins/Plugin.class",
		"plugins/PluginEvent.class",
		"plugins/PluginManager.class",
		"plugins/PluginEventListener.class",
		"plugins/PluginConfig.class",
		"plugins/PluginConfigListener.class",
		"plugins/config/ConfigParameter.class",
		"plugins/logging/Logger.class",
		"plugins/logging/LoggerChannel.class",
		"plugins/peers/protocol/PeerProtocolManager.class",
		"plugins/sharing/ShareManager.class",
		"plugins/sharing/ShareException.class",
		"plugins/ui/config/PluginConfigUIFactory.class",
		"plugins/ui/config/Parameter.class",
		"plugins/ui/config/ConfigSection.class",
		"plugins/ui/tables/peers/PluginPeerItemFactory.class",
		"plugins/ui/tables/mytorrents/PluginMyTorrentsItemFactory.class",
		"plugins/ui/UIManager.class",
		"plugins/ddb/DistributedDatabase.class",
		"plugins/disk/DiskManager.class",
		"plugins/disk/DiskManagerFileInfo.class",
		"plugins/download/DownloadManager.class",
		"plugins/download/DownloadManagerStats.class",
		"plugins/download/DownloadException.class",
		"plugins/download/Download.class",
		"plugins/download/DownloadManagerListener.class",
		"plugins/download/DownloadWillBeAddedListener.class",
		"plugins/download/DownloadStats.class",
		"plugins/download/DownloadScrapeResult.class",
		"plugins/download/DownloadAnnounceResult.class",
		"plugins/download/DownloadAnnounceResultPeer.class",
		"plugins/download/DownloadRemovalVetoException.class",
		"plugins/download/DownloadListener.class",
		"plugins/download/DownloadPropertyListener.class",
		"plugins/download/DownloadTrackerListener.class",
		"plugins/download/DownloadWillBeRemovedListener.class",
		"plugins/download/DownloadPeerListener.class",
		"plugins/download/session/SessionAuthenticator.class",
		"plugins/network/ConnectionManager.class",
		"plugins/messaging/MessageManager.class",
		"plugins/peers/PeerManager.class",
		"plugins/platform/PlatformManager.class",
		"plugins/torrent/Torrent.class",
		"plugins/torrent/TorrentFile.class",
		"plugins/torrent/TorrentException.class",
		"plugins/torrent/TorrentEncodingException.class",
		"plugins/torrent/TorrentManager.class",
		"plugins/torrent/TorrentManagerListener.class",
		"plugins/torrent/TorrentAttribute.class",
		"plugins/torrent/TorrentDownloader.class",
		"plugins/torrent/TorrentAnnounceURLList.class",
		"plugins/tracker/Tracker.class",
		"plugins/tracker/TrackerTorrent.class",
		"plugins/tracker/TrackerException.class",
		"plugins/tracker/TrackerListener.class",
		"plugins/tracker/TrackerTorrentListener.class",
		"plugins/tracker/TrackerTorrentRemovalVetoException.class",
		"plugins/tracker/TrackerPeer.class",
		"plugins/tracker/TrackerTorrentWillBeRemovedListener.class",
		"plugins/tracker/web/TrackerWebContext.class",
		"plugins/tracker/web/TrackerWebPageGenerator.class",
		"plugins/tracker/web/TrackerAuthenticationListener.class",
		"plugins/utils/Utilities.class",
		"plugins/ipfilter/IPFilter.class",
		"plugins/ipfilter/IPFilterException.class",
		"plugins/ipfilter/IPRange.class",
		"plugins/ipfilter/IPBlocked.class",
		"plugins/utils/resourcedownloader/ResourceDownloaderException.class",
		"plugins/utils/ShortCuts.class",
		"plugins/update/UpdateManager.class",
		"plugins/clientid/ClientIDManager.class",


	};
	
	private PluginInterface		plugin_interface;
	
	private RPRequestHandler				request_handler;
	private WebPluginAccessController		access_controller;

	private BooleanParameter		sign_enable;
	private StringParameter			sign_alias;
	private DirectoryParameter		data_dir;
	
	private File					cache_dir;
	private boolean					use_cache;
	
	public
	RemoteUIServlet()
	{
		super( defaults );
	}
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		super.initialize( _plugin_interface );
		
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( 
				"org.gudy.azureus2.ui.webplugin.remoteui.servlet.internat.Messages" );
		
		request_handler = new RPRequestHandler( _plugin_interface );
		
		
		BasicPluginConfigModel	config = getConfigModel();
		
		config.addLabelParameter2( "webui.signjars.info" );
		
		sign_enable = config.addBooleanParameter2("Sign Jars", "webui.signjars", false );
		
		sign_alias 	= config.addStringParameter2("Sign Alias", "webui.signalias", "Azureus" );
		
			// this param also accessed in applet
		
		data_dir 	= config.addDirectoryParameter2("Data Directory", "webui.datadir", "" );
		
		sign_enable.addEnabledOnSelection( sign_alias );
		
		access_controller = new WebPluginAccessController( _plugin_interface );

		cache_dir 	= plugin_interface.getPluginconfig().getPluginUserFile( "cache" );
		
		if ( cache_dir.exists() && cache_dir.isDirectory()){
			
			use_cache = true;
			
			File[]	existing = cache_dir.listFiles();
			
			if ( existing != null ){
				
				for (int i=0;i<existing.length;i++){
					
					if ( !existing[i].delete()){
						
						use_cache = false;
						
						break;
					}
				}
			}
		}else{
			
			use_cache = cache_dir.mkdirs();
		}
		
		Map	package_map = new HashMap();
		
		buildPackageMap( package_map, getClass().getClassLoader(), "org/gudy/azureus2/plugins/PluginInterface.class" );
		buildPackageMap( package_map, getClass().getClassLoader(), "org/gudy/azureus2/ui/webplugin/remoteui/applet/RemoteUIApplet.class" );
		
		List	additional_resource_names = new ArrayList();
		
		for (int i=0;i<resource_names.length;i++){
			
			String	resource = resource_names_prefix + "/" + resource_names[i];
			
			int	pos = resource.lastIndexOf('/');
			
			String	pack = resource.substring(0,pos);
			String	clas = resource.substring(pos+1);
			
			pos	= clas.indexOf('.');
			
			clas = clas.substring(0,pos) + "$";
			
			List	entries = (List)package_map.get( pack );
			
			if ( entries == null ){
				
				System.out.println("no map for " + pack );
				
			}else{
								
				for (int j=0;j<entries.size();j++){
					
					String	entry = (String)entries.get(j);
					
					if ( entry.startsWith( clas )){
										
						additional_resource_names.add( pack.substring( resource_names_prefix.length() + 1 ) + "/" + entry );
					}
				}
			}
		}
		
		String[]	new_rn = new String[ resource_names.length + additional_resource_names.size()];
		
		System.arraycopy( resource_names, 0, new_rn, 0, resource_names.length );
		
		for (int i=0;i<additional_resource_names.size();i++){
			
			String	ar = (String)additional_resource_names.get(i);
						
			new_rn[resource_names.length+i] = ar;
		}
		
		resource_names = new_rn;
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		// System.out.println( "header:" + request.getHeader());
		
		String	url = request.getURL();
		
		if ( url.equals( "/remui.jar" ) || url.equals( "/remuiicons.jar" )){
			
			if ( use_cache ){
				
				File	cache_file	= new File( cache_dir, url.substring(1));

				boolean	cache_file_ok = false;
				
				if ( cache_file.exists()){
					
					cache_file_ok	= true;
					
				}else{
					
					OutputStream	os = null;

					try{
						
						os = new FileOutputStream( cache_file );
						
							// this code appears below as well...
						
						AEJarBuilder.buildFromResources( 
								os, 
								plugin_interface.getPluginClassLoader(), 
								resource_names_prefix, 
								url.equals( "/remui.jar")?resource_names:resource_icon_names,
								sign_enable.getValue()?sign_alias.getValue():null );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
						
					}finally{
						
						if ( os != null ){
							
							try{
								
								os.close();
								
								cache_file_ok	= true;
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					}
				}
				
				if ( cache_file_ok ){
					
					response.useStream( "jar", new FileInputStream( cache_file ));
					
					return( true );
					
				}else{
					
					use_cache = false;
				}
			}
			
			OutputStream	os = null;
			
			try{
				os = response.getOutputStream();
			
					// this code appears above as well...
				
				AEJarBuilder.buildFromResources( 
						os, 
						plugin_interface.getPluginClassLoader(), 
						resource_names_prefix, 
						url.equals( "/remui.jar")?resource_names:resource_icon_names,
						sign_enable.getValue()?sign_alias.getValue():null );
				
				
				response.setContentType("application/java-archive");
			
			
				return( true );
		
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				if ( e instanceof IOException ){
					
					throw((IOException)e);
				}
				
				throw( new IOException( e.toString()));
				
			}finally{
				
				if ( os != null ){

					os.close();
				}
			}
		}else if ( url.equals( "/process.cgi")){
	
			ObjectInputStream	dis = null;
			
			try{
				dis = new ObjectInputStream( new GZIPInputStream(request.getInputStream()));
								
				RPRequest	rp_request = (RPRequest)dis.readObject();
				
				rp_request.setClientIP( request.getClientAddress());
				
				// System.out.println( "RemoteUIServlet:got request: " + rp_request.getString());
				
				RPReply	reply = request_handler.processRequest( rp_request, access_controller );
				
				if ( reply == null ){
					
					reply = new RPReply( null );
				}
				
				response.setContentType( "application/octet-stream" );
				
				ObjectOutputStream	oos = new ObjectOutputStream(new GZIPOutputStream(response.getOutputStream()));
				
				try{
					oos.writeObject( reply );
				
				}finally{
					
					oos.close();
				}
				
				return( true );
				
			}catch( ClassNotFoundException e ){
				
				e.printStackTrace();
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
			}finally{
				
				if ( dis != null ){
					
					dis.close();
				}
			}
		}else if ( url.equals( "/upload.cgi")){

			// -----------------------------7d4f2a310bca
			//Content-Disposition: form-data; name="upfile"; filename="C:\Temp\(HH)-Demon Beast Resurrection 1-2.torrent"
			//Content-Type: application/octet-stream
			//
			// <data>
			// -----------------------------7d4f2a310bca
						
			InputStream	is = request.getInputStream();
			
			try{
				
				String	content = "";
				
				while( true ){
					
					byte[]	buffer = new byte[1024];
					
					int	len = is.read(buffer);
					
					if ( len <= 0 ){
						
						break;
					}
					
					content += new String(buffer, 0,len, "ISO-8859-1" );
				}
				
				PrintWriter pw = new PrintWriter( new OutputStreamWriter( response.getOutputStream()));
				
				try{

					access_controller.checkUploadAllowed();
					
					int	sep1 = content.indexOf( "\r\n" );
					
					String	tag = content.substring(0,sep1);
					
					int	sep2 = content.indexOf( "\r\n\r\n");
					
					int	data_start 	= sep2 + 4;
					int data_end	= content.indexOf(tag, data_start );
					
					String	str_data = content.substring(data_start, data_end);
					
					// System.out.println("ds = "+ data_start + ", de = " + data_end + ", content = " + content);
					
					Torrent	torrent	= null;
					
					if ( data_end - data_start <= 2 ){
						
						int	fn_start = content.indexOf( "filename=\"");
						
						if ( fn_start != -1 ){
							
							fn_start += 10;
							
							int	fn_end	= content.indexOf( "\"", fn_start );
							
							if ( fn_end != -1 ){
								
								String	file_name = content.substring( fn_start, fn_end );
																							
								if ( file_name.toLowerCase().startsWith("http")){
									
									file_name = file_name.replaceAll( " ", "%20");

									URL	torrent_url = new URL( file_name );
										
									// System.out.println( "downloading '" + torrent_url.toString() + "'" );
									try{
										TorrentDownloader dl = 
											plugin_interface.getTorrentManager().getURLDownloader( torrent_url, null, null );
										
										torrent = dl.download("system");
										
									}catch( TorrentException e ){
										
										if ( 	e.getCause() instanceof ResourceDownloaderException &&
												e.getCause().getMessage() != null &&
												e.getCause().getMessage().indexOf( "401" ) != -1 ){
											
											throw( new Exception( 	"Authorisation failed, encode the user name and password in the URL using " +
																	"http://&lt;user&gt;:&lt;password&gt;@" + torrent_url.getHost() + ":" + torrent_url.getPort()+
																	torrent_url.getPath()));
										}
										
										if ( e instanceof TorrentEncodingException ){
											
											throw( new Exception( "Torrent encoding can't be resolved - use the applet interface to upload the torrent" ));
										}
									}
								}
							}
						}
					}
					
					if ( torrent == null ){
						
						byte[]	data = str_data.getBytes("ISO-8859-1");
			
						torrent = plugin_interface.getTorrentManager().createFromBEncodedData( data );
					}
					
					String	data = data_dir.getValue();
					
					if ( data.length() > 0 ){
						
						File	data_dir_file = new File(data);
						
						if (!data_dir_file.exists()){
							
							throw( new Exception( "Data directory '" + data + "' doesn't exist" ));
						}
						
						plugin_interface.getDownloadManager().addDownload( torrent, null, data_dir_file );
						
					}else{
					
						plugin_interface.getDownloadManager().addDownload( torrent );
					}
				
					pw.println("<HTML><BODY><P><FONT COLOR=#00CC44>Upload OK</FONT></P></BODY></HTML>");
				
				}catch( Throwable e ){
					
					String	message_chain = "";
					
					Throwable	temp = e;
					
					while( temp != null ){
						
						String	this_message = temp.getMessage();
						
						if ( this_message != null ){
							
							message_chain += (message_chain.length()==0?"":"\n") + this_message;
						}
						
						temp = temp.getCause();
					}
								
					String	message = message_chain.length()==0?e.toString():message_chain;
						

					pw.println("<HTML><BODY><P><FONT COLOR=#FF0000>Upload Failed: " + message + "</FONT></P></BODY></HTML>");
					
				}finally{
					
					pw.close();
				}
				
				return( true );
			}finally{
				
				if ( is != null ){
					
					is.close();
				}
			}
		}else if ( url.endsWith(".class" )){ 
	
			response.setReplyStatus( 404 );
		
			return( true );		
		}
		
		return( false );
	}
	
	protected void
	buildPackageMap(
		Map			package_map,
		ClassLoader	cl,
		String		resource )
	{
		URL url = cl.getResource( resource );
		
		String	url_string = url.toString();
				
	    if ( url_string.startsWith("jar:file:" )){
	    	
	        File jar = FileUtil.getJarFileFromURL( url_string );
	        
	        if ( jar != null ){

	        	try{
			        JarFile jarFile = new JarFile(jar);
			        
			        Enumeration entries = jarFile.entries();
			        			        
			        while (entries.hasMoreElements()){
			       
			        	JarEntry entry = (JarEntry) entries.nextElement();
			          
			        	if ( !entry.isDirectory()){
			        		
			        		String	name = entry.getName();
			        		
			        		int	pos = name.lastIndexOf( "/" );
			        		
			        		if ( pos != -1 ){
			        			
				        		String	pack 	= name.substring(0,pos);
				        		String	term	= name.substring(pos+1);
				        		
				        		List	list = (List)package_map.get(pack);
				        		
				        		if ( list == null ){
				        			
				        			list = new ArrayList();
				        			
				        			// System.out.println( "new package: " + pack );
				        			
				        			package_map.put( pack, list );
				        		}
				        				        		
				        		if ( term.indexOf( '$' ) != -1 && term.endsWith( ".class" )){
				        			
				        			list.add( term );
				        		}
			        		}
			        	}
			        }
	        	}catch( Throwable e){
	        		
	        		e.printStackTrace();
	        	}
	        }
	    }else{
	    	
	    	int	depth 	= 0;
	    	int	pos		= 0;
	    	
	    	while(true){
	    		int	p1 = resource.indexOf( "/", pos );
	    		
	    		if ( p1 == -1 ){
	    			break;
	    		}
	    		depth++;
	    		pos	= p1+1;
	    	}
	    	
	        File root_dir = new File(URI.create(url_string));
	        
	        for (int i=0;i<=depth;i++){
	        	
	        	root_dir = root_dir.getParentFile();
	        }
	        	        
	        buildPackageMap( package_map, root_dir, new ArrayList(), null );
	    }
	}
	
	protected void
	buildPackageMap(
		Map			package_map,
		File		file,
		List		list,
		String		prefix )
	{
		String	name = file.getName();
		
		if ( file.isFile()){
			
			if ( name.indexOf( '$' ) != -1 && name.endsWith( ".class" )){
							
				list.add( name );
			}
		}else{
			
			File[]	files = file.listFiles();
			
			if ( prefix == null ){
				
				prefix = "";
				
			}else if ( prefix.length() == 0 ){
				
				prefix = name;
				
			}else{
				
				prefix = prefix+"/"+name;
			}
			
			list	= new ArrayList();
				
			// System.out.println( "new package:" + prefix );
			
			package_map.put( prefix, list );
			
			for (int i=0;i<files.length;i++){
			
				buildPackageMap( package_map, files[i], list, prefix );
			}
		}
	}
}

