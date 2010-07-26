/*
 * Created on Feb 18, 2009
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


package com.aelitis.azureus.plugins.upnpmediaserver;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

import com.aelitis.azureus.core.content.AzureusContent;
import com.aelitis.azureus.core.content.AzureusContentDirectory;
import com.aelitis.azureus.core.content.AzureusContentDownload;
import com.aelitis.azureus.core.content.AzureusContentFile;
import com.aelitis.azureus.core.content.AzureusContentFilter;


public class 
UPnPMediaServerContentDirectory 
{
	public static final int		CT_UNKNOWN			= -1;
	public static final int		CT_DEFAULT			= 1;
	public static final int		CT_XBOX				= 2;
	public static final int		CT_MEDIA_PLAYER		= 3;
	

	private static final String	CONTENT_UNKNOWN	= "object.item";
	
	protected static final String	CONTENT_CONTAINER	= "object.container.storageFolder";
	
	//private static final String	CONTENT_VIDEO	= "object.item.videoItem";
	protected static final String	CONTENT_VIDEO	= "object.item.videoItem.movie";
	protected static final String	CONTENT_AUDIO	= "object.item.audioItem.musicTrack";
	protected static final String	CONTENT_IMAGE	= "object.item.imageItem.photo";

	private static final String	XBOX_CONTENT_VIDEO	= "object.item.videoItem";
	private static final String	XBOX_CONTENT_AUDIO	= "object.item.audioItem.musicTrack";
	private static final String	XBOX_CONTENT_IMAGE	= "object.item.imageItem.photo";

	
	
	private static final String[][]	mime_mappings = {
		
			// Microsoft
		
		{ "asf", "video/x-ms-asf",				CONTENT_VIDEO },
		{ "asx", "video/x-ms-asf",				CONTENT_VIDEO },
		{ "nsc", "video/x-ms-asf",				CONTENT_VIDEO },
		{ "wax", "audio/x-ms-wax",				CONTENT_AUDIO },
		{ "wm",  "video/x-ms-wm",				CONTENT_VIDEO },
		{ "wma", "audio/x-ms-wma",				CONTENT_AUDIO },
		{ "wmv", "video/x-ms-wmv",				CONTENT_VIDEO },
		{ "wmx", "video/x-ms-wmx",				CONTENT_VIDEO },
		{ "wvx", "video/x-ms-wvx",				CONTENT_VIDEO },
		
			// other video
		
		// { "avi",  "video/x-ms-video",			CONTENT_VIDEO },
		{ "avi",  "video/avi",					CONTENT_VIDEO },
		{ "mp2",  "video/mpeg", 				CONTENT_VIDEO },
		{ "mpa",  "video/mpeg", 				CONTENT_VIDEO },
		{ "mpe",  "video/mpeg", 				CONTENT_VIDEO },
		{ "mpeg", "video/mpeg", 				CONTENT_VIDEO },
		{ "mpg",  "video/mpeg", 				CONTENT_VIDEO },
		{ "mpv2", "video/mpeg", 				CONTENT_VIDEO },
		{ "vob",  "video/mpeg", 				CONTENT_VIDEO },
		{ "mov",  "video/quicktime", 			CONTENT_VIDEO },
		{ "qt",   "video/quicktime", 			CONTENT_VIDEO },
		{ "lsf",  "video/x-la-asf", 			CONTENT_VIDEO },
		{ "lsx",  "video/x-la-asf", 			CONTENT_VIDEO },
		{ "movie","video/x-sgi-movie", 			CONTENT_VIDEO },
		{ "mkv",  "video/x-matroska", 			CONTENT_VIDEO },
		{ "mp4",  "video/mp4", 					CONTENT_VIDEO },
		{ "mpg4", "video/mp4", 					CONTENT_VIDEO },
		{ "flv",  "video/x-flv", 				CONTENT_VIDEO },
		{ "ts",   "video/MP2T", 				CONTENT_VIDEO },
		{ "m4v",  "video/m4v", 					CONTENT_VIDEO },
		
			// audio
		
		{ "au",   "audio/basic",				CONTENT_AUDIO },
		{ "snd",  "audio/basic", 				CONTENT_AUDIO },
		{ "mid",  "audio/mid",  				CONTENT_AUDIO },
		{ "rmi",  "audio/mid", 					CONTENT_AUDIO },
		{ "mp3",  "audio/mpeg" ,				CONTENT_AUDIO },
		{ "aif",  "audio/x-aiff", 				CONTENT_AUDIO },
		{ "aifc", "audio/x-aiff", 				CONTENT_AUDIO },
		{ "aiff", "audio/x-aiff", 				CONTENT_AUDIO },
		{ "m3u",  "audio/x-mpegurl", 			CONTENT_AUDIO },
		{ "ra",   "audio/x-pn-realaudio",		CONTENT_AUDIO },
		{ "ram",  "audio/x-pn-realaudio",		CONTENT_AUDIO },
		{ "wav",  "audio/x-wav", 				CONTENT_AUDIO },
		{ "flac", "audio/flac",					CONTENT_AUDIO },
		{ "mka",  "audio/x-matroska",			CONTENT_AUDIO },
		{ "m4a",  "audio/mp4",                  CONTENT_AUDIO },
		
			// image
		
		{ "bmp",  "image/bmp", 					CONTENT_IMAGE },
		{ "cod",  "image/cis-cod",				CONTENT_IMAGE }, 
		{ "gif",  "image/gif", 					CONTENT_IMAGE },
		{ "ief",  "image/ief", 					CONTENT_IMAGE },
		{ "jpe",  "image/jpeg", 				CONTENT_IMAGE },
		{ "jpeg", "image/jpeg", 				CONTENT_IMAGE },
		{ "jpg",  "image/jpeg", 				CONTENT_IMAGE },
		{ "jfif", "image/pipeg",		 		CONTENT_IMAGE },
		{ "tif",  "image/tiff", 				CONTENT_IMAGE },
		{ "tiff", "image/tiff", 				CONTENT_IMAGE },
		{ "ras",  "image/x-cmu-raster", 		CONTENT_IMAGE },
		{ "cmx",  "image/x-cmx", 				CONTENT_IMAGE },
		{ "ico",  "image/x-icon", 				CONTENT_IMAGE },
		{ "pnm",  "image/x-portable-anymap", 	CONTENT_IMAGE }, 
		{ "pbm",  "image/x-portable-bitmap", 	CONTENT_IMAGE },
		{ "pgm",  "image/x-portable-graymap",	CONTENT_IMAGE },
		{ "ppm",  "image/x-portable-pixmap", 	CONTENT_IMAGE },
		{ "rgb",  "image/x-rgb", 				CONTENT_IMAGE },
		{ "xbm",  "image/x-xbitmap", 			CONTENT_IMAGE },
		{ "xpm",  "image/x-xpixmap", 			CONTENT_IMAGE },
		{ "xwd",  "image/x-xwindowdump", 		CONTENT_IMAGE },
		
			// other
		
		{ "ogg",   "application/ogg",			CONTENT_AUDIO },
		{ "ogm",   "application/ogg",			CONTENT_VIDEO },
				
	};
	
	private static Map<String,String[]>	ext_lookup_map = new HashMap<String,String[]>();
	
	static{
		for (int i=0;i<mime_mappings.length;i++){
			
			ext_lookup_map.put( mime_mappings[i][0], mime_mappings[i] );
		}
	}
	
	private static SimpleDateFormat upnp_date_format = new SimpleDateFormat( "yyyy-MM-dd" );
	
	private UPnPMediaServer	media_server;
	
	private TorrentAttribute	ta_unique_name;

	private int		next_oid;

	private Random	random = new Random();
	
	private int		system_update_id	= random.nextInt( Integer.MAX_VALUE );

	private Map<Integer,content>		content_map 	= new HashMap<Integer,content>();

	private Map<String,Long>			config;
	private boolean						config_dirty;
	
	private Object					lock = new Object();
	
	private contentContainer		root_container;
	private contentContainer		downloads_container;
	private contentContainer		movies_container;
	private contentContainer		music_container;
	private contentContainer		pictures_container;

	protected
	UPnPMediaServerContentDirectory(
		UPnPMediaServer		_media_server )
	{
		media_server = _media_server;

		ta_unique_name	= media_server.getPluginInterface().getTorrentManager().getPluginAttribute( "unique_name");
		
		root_container = new contentContainer( null, "Vuze" );
		
		music_container 	= new contentContainer( root_container, "Music" );
				
		pictures_container 	= new contentContainer( root_container, "Pictures" );
		
		movies_container 	= new contentContainer( root_container, "Movies" );
		
		downloads_container 	= new contentContainer( root_container, "Downloads" );
	}
	
	protected void
	addContent(
		final Download			download )
	{
		Torrent torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return;
		}
		
		HashWrapper hash = new HashWrapper(torrent.getHash());

		DiskManagerFileInfo[]	files = download.getDiskManagerFileInfo();

		synchronized( lock ){
				
			if ( files.length == 1 ){
						
				String title = getUniqueName( download, hash, files[0].getFile().getName());
				
				contentItem	item = new contentItem( downloads_container, getACF( files[0]), download.getTorrent().getHash(), title );
				
				addToFilters( item );
				
			}else{
				
				contentContainer container = 
					new contentContainer( 
							downloads_container,
							getACD( download ),
							getUniqueName( download, hash, download.getName()));
								
				Set<String>	name_set = new HashSet<String>();
				
				boolean	duplicate = false;
				
				for (int j=0;j<files.length;j++){

					DiskManagerFileInfo	file = files[j];
									
					String	title = file.getFile().getName();
					
					if ( name_set.contains( title )){
						
						duplicate = true;
						
						break;
					}
					
					name_set.add( title );
				}
				
				for (int j=0;j<files.length;j++){
					
					DiskManagerFileInfo	file = files[j];
							
						// keep these unique within the container
					
					String	title = file.getFile().getName();
					
					if ( duplicate ){
						
						title =  ( j + 1 ) + ". " + title;
					}
					
					new contentItem( container, getACF( file ), hash.getBytes(), title );
				}
				
				addToFilters( container );
			}
		}
	}
	
	protected void
	addContent(
		AzureusContentFile			content_file )
	{
		synchronized( lock ){
	
			DiskManagerFileInfo file = content_file.getFile();
			
			try{
				byte[]	hash = file.getDownloadHash();
				
				String title = getUniqueName( null, new HashWrapper( hash ), file.getFile().getName());
					
				contentItem	item = new contentItem( downloads_container, content_file, hash, title );
						
				addToFilters( item );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	removeContent(
		AzureusContentFile			file )
	{
		removeContent( file.getFile());
	}
	
	protected void
	removeContent(
		DiskManagerFileInfo			file )
	{
		synchronized( lock ){
			
			try{
				byte[]	hash = file.getDownloadHash();
				
				String unique_name = getUniqueName( null, new HashWrapper( hash ), file.getFile().getName());
											
				content container = downloads_container.removeChild( unique_name );
	
				removeUniqueName( new HashWrapper( hash ));
				
				if ( container != null ){
					
					removeFromFilters( container );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	removeContent(
		Download			download )
	{
		Torrent torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return;
		}
		
		HashWrapper hash = new HashWrapper(torrent.getHash());

		DiskManagerFileInfo[]	files = download.getDiskManagerFileInfo();

		synchronized( lock ){
			
			String	unique_name;
			
			if ( files.length == 1 ){

				unique_name = getUniqueName( download, hash, files[0].getFile().getName());
								
			}else{
				
				unique_name = getUniqueName( download, hash, download.getName());
			}
			
			content container = downloads_container.removeChild( unique_name );

			removeUniqueName( hash );
			
			if ( container != null ){
				
				removeFromFilters( container );
			}
		}
	}
	
	protected void
	invalidate()
	{
		root_container.invalidate();
	}
	
	private String
	findPrimaryContentType(
		content		con )
	{
		if ( con instanceof contentItem ){
			
			return(((contentItem)con).getContentClass());
			
		}else{
	
			String	type = CONTENT_UNKNOWN;
			
			contentContainer container = (contentContainer)con;
			
			List<content> kids = container.getChildren();
			
			for (int i=0;i<kids.size();i++){
				
				String	t = findPrimaryContentType(kids.get(i));
				
				if ( t == CONTENT_VIDEO ){
					
					return( t );
				}
				
				if ( t == CONTENT_AUDIO ){
					
					type = t;
					
				}else if ( t == CONTENT_IMAGE && type == CONTENT_UNKNOWN ){
					
					type = t;
					
				}
			}
			
			return( type );
		}
	}
	
	private void
	addToFilters(
		content		con )
	{
		String type = findPrimaryContentType( con );
		
		if ( media_server.useCategories()){
			
			String[]	categories = con.getCategories();
			
			if ( categories.length == 0 ){
				
				if ( type == CONTENT_VIDEO ){
					
					movies_container.addLink( con );
					
				}else if ( type == CONTENT_AUDIO ){
					
					music_container.addLink( con );
			
				}else if ( type == CONTENT_IMAGE ){
					
					pictures_container.addLink( con );
				}
			}else{
				
				for ( String cat: categories ){
					
					contentContainer parent = null;
					
					if ( type == CONTENT_VIDEO ){
						
						parent = movies_container;
						
					}else if ( type == CONTENT_AUDIO ){
						
						parent = music_container;
				
					}else if ( type == CONTENT_IMAGE ){
						
						parent = pictures_container;
					}
									
					if ( parent != null ){
						
						content node = null;
	
						int		num = 0;
						String	name;
						
						while( 	(! ( node instanceof contentContainer )) ||
								((contentContainer)node).getACD() != null ){
																		
							name = num++==0?cat:( num + ". " + cat );
								
							node = parent.getChild( name );
							
							if ( node == null ){
								
								node = new contentContainer( parent, name );
							}
						}
						
						((contentContainer)node).addLink( con );
					}
				}
			}
		}else{
			
			if ( type == CONTENT_VIDEO ){
				
				movies_container.addLink( con );
				
			}else if ( type == CONTENT_AUDIO ){
				
				music_container.addLink( con );
		
			}else if ( type == CONTENT_IMAGE ){
				
				pictures_container.addLink( con );
			}
		}
	}
	
	private void
	removeFromFilters(
		content		con )
	{
		contentContainer[] parents = { movies_container, pictures_container, music_container };
		
		for ( contentContainer parent: parents ){
			
			parent.removeLink( con.getName());
			
			if ( media_server.useCategories()){
				
				List<content> kids = parent.getChildren();
				
				for ( content k: kids ){
					
					if ( k instanceof contentContainer && ((contentContainer)k).getACD() == null ){
						
						((contentContainer)k).removeLink( con.getName());
					}
				}
			}
		}
	}
	
	protected void
	contentChanged(
		AzureusContentFile	acf )
	{
		if ( media_server.useCategories()){

			contentChanged( downloads_container, acf );
		}
	}
	
	protected boolean
	contentChanged(
		contentContainer	content,
		AzureusContentFile	acf )
	{			
		for ( content c: content.getChildren()){
			
			if ( c instanceof contentItem ){
				
				if (((contentItem)c).getACF() == acf ){
				
					removeFromFilters( c );
					
					addToFilters( c );
					
					return( true );
				}		
			}else{
				
				if ( contentChanged((contentContainer)c, acf )){
					
					return( true );
				}
			}
		}
		
		return( false );
	}
	
	private Map<HashWrapper,String> unique_name_map	= new HashMap<HashWrapper, String>();
	private Set<String> 			unique_names 	= new HashSet<String>();
	
	protected String
	getUniqueName(
		Download		dl,
		HashWrapper		hw,
		String			name )
	{
		synchronized( unique_names ){
			
			String result = (String)unique_name_map.get( hw );
	
			if ( result != null ){
				
				return( result );
			}
				// ensure that we have a unique name for the download
			
			result = dl==null?null:dl.getAttribute( ta_unique_name );
			
			if ( result == null || result.length() == 0 ){
				
				result = name;
			}
			
			int	num = 1;
			
			while( unique_names.contains( result )){
				
				result = (num++) + ". " + name; 
			}
		
				// if we had to make one up, record it persistently
			
			if ( num > 1 && dl != null ){
			
				dl.setAttribute( ta_unique_name, result );
			}
			
			unique_names.add( result );
			
			unique_name_map.put( hw, result );
			
			return( result );
		}
	}
	
	protected void
	removeUniqueName(
		HashWrapper			hash )
	{
		synchronized( unique_names ){
			
			String name = (String)unique_name_map.remove( hash );
			
			if ( name != null ){
				
				unique_names.remove( name );
			}
		}
	}
	
	
	protected void
	ensureStarted()
	{
		media_server.ensureStarted();
	}
	
	protected contentContainer
	getRootContainer()
	{
		ensureStarted();
		
		return( root_container );
	}
	
	protected contentContainer
	getMoviesContainer()
	{
		ensureStarted();
		
		return( movies_container );
	}
	
	protected contentContainer
	getMusicContainer()
	{
		ensureStarted();
		
		return( music_container );
	}
	
	protected contentContainer
	getPicturesContainer()
	{
		ensureStarted();
		
		return( pictures_container );
	}
	
	protected content
	getContentFromID(
		int		id )
	{
		ensureStarted();
		
		synchronized( content_map ){
			
			return( content_map.get( new Integer( id )));
		}
	}
	
	protected contentContainer
	getContainerFromID(
		int		id )
	{
		ensureStarted();
		
		synchronized( content_map ){
			
			content c = content_map.get( new Integer( id ));
			
			if ( c instanceof contentContainer ){
				
				return((contentContainer)c);
			}
		}
		
		return( null );
	}
		
	protected contentItem
	getContentFromResourceID(
		String		id )
	{
		ensureStarted();
		
		int	pos = id.indexOf( "-" );
		
		if ( pos == -1 ){
			
			log( "Failed to decode resource id '" + id + "'" );
			
			return( null );
		}
		
		byte[]	hash = ByteFormatter.decodeString( id.substring( 0, pos ));
		
		String	rem = id.substring( pos+1 );
		
		pos = rem.indexOf( "." );
		
		if ( pos != -1 ){
			
			rem = rem.substring( 0, pos );
		}
		
		try{
			int file_index = Integer.parseInt( rem );
			
			return( getExistingContentFromHashAndFileIndex( hash, file_index ));
			
		}catch( Throwable e ){
			
			log( "Failed to decode resource id '" + id + "'", e );
			
			return( null );
		}
	}
	
	protected contentItem
	getContentFromHash(
		byte[]		hash )
	{
		ensureStarted();
		
		contentItem	item = getExistingContentFromHash( hash );
		
		if ( item == null ){
			
			AzureusContentDirectory[]	directories = media_server.getAzureusContentDirectories();
			
			Map<String,byte[]>	lookup = new HashMap<String,byte[]>();
			
			lookup.put( AzureusContentDirectory.AT_BTIH, hash );
			
			for (int i=0;i<directories.length;i++){
				
				AzureusContentDirectory	directory = directories[i];
				
				AzureusContent	content = directory.lookupContent( lookup );
				
				if ( content != null ){
					
					Torrent	torrent = content.getTorrent();
					
					if ( torrent != null ){
						
						DownloadManager	dm = media_server.getPluginInterface().getDownloadManager();
						
							// hmm, when to resume...
						
						dm.pauseDownloads();
						
						try{
							Download download = dm.addDownload( torrent );
							
							addContent( download );
									
							item = getExistingContentFromHash( hash );

							int	sleep 	= 100;
							int	max		= 20000;
							
								// need to wait for things to get started else file might not
								// yet exist and we bork
							
							for (int j=0;j<max/sleep;j++){
								
								int	state = download.getState();
								
								if ( 	state == Download.ST_DOWNLOADING || 
										state == Download.ST_SEEDING ||
										state == Download.ST_ERROR ||
										state == Download.ST_STOPPED ){
									
									break;
								}
								
								Thread.sleep(sleep);
							}
							
							break;
							
						}catch( Throwable e ){
							
							log( "Failed to add download", e );
						}
					}
				}
			}
		}
		
		return( item );
	}
	
	protected AzureusContentFile
	getACF(
		final DiskManagerFileInfo		file )
	{
		try{
			byte[] hash = file.getDownloadHash();
			
			AzureusContentDirectory[]	directories = media_server.getAzureusContentDirectories();
				
			Map<String,Object>	lookup = new HashMap<String,Object>();
				
			lookup.put( AzureusContentDirectory.AT_BTIH, hash );
			lookup.put( AzureusContentDirectory.AT_FILE_INDEX, new Integer( file.getIndex()));
				
			for (int i=0;i<directories.length;i++){
					
				AzureusContentDirectory	directory = directories[i];
					
				AzureusContentFile	acf = directory.lookupContentFile( lookup );
				
				if ( acf != null ){
					
					return( acf );
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		return( 
				new AzureusContentFile()
				{
					public DiskManagerFileInfo
					getFile()
					{
						return( file );
					}
					
					public Object
					getProperty(
						String		name )
					{
						return( null );
					}
				});
	}
	
	protected AzureusContentDownload
	getACD(
		final Download		download )
	{
		try{
			byte[] hash = download.getTorrent().getHash();
		
			AzureusContentDirectory[]	directories = media_server.getAzureusContentDirectories();
				
			Map<String,Object>	lookup = new HashMap<String,Object>();
				
			lookup.put( AzureusContentDirectory.AT_BTIH, hash );
				
			for (int i=0;i<directories.length;i++){
					
				AzureusContentDirectory	directory = directories[i];
					
				AzureusContentDownload	acf = directory.lookupContentDownload( lookup );
				
				if ( acf != null ){
					
					return( acf );
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		return( 
				new AzureusContentDownload()
				{
					public Download
					getDownload()
					{
						return( download );
					}
					
					public Object
					getProperty(
						String		name )
					{
						return( null );
					}
				});
	}
	
	protected contentItem
	getExistingContentFromHash(
		byte[]		hash )
	{
		ensureStarted();
		
		return( getExistingContentFromHashAndFileIndex( hash, 0 ));
	}
	
	protected contentItem
	getExistingContentFromHashAndFileIndex(
		byte[]		hash,
		int			file_index )
	{
		ensureStarted();
		
		synchronized( content_map ){

			Iterator<content>	it = content_map.values().iterator();
			
			while( it.hasNext()){
				
				content	content = it.next();
				
				if ( content instanceof contentItem ){
					
					contentItem	item = (contentItem)content;
					
					DiskManagerFileInfo	file = item.getFile();
					
					if ( file.getIndex() == file_index ){
				
						if ( Arrays.equals( item.getHash(), hash )){
									
							return( item );
						}
					}
				}
			}
		}
		
		return( null );
	}
	
	protected String
	getDIDL(
		content						con,
		String						host,
		int							client_type,
		List<AzureusContentFilter>	filters,
		Map<String,Object>			filter_args )	
	{
		if ( con instanceof contentContainer ){
			
			contentContainer	child_container = (contentContainer)con;
			
			List<content> kids = child_container.getChildren();
			
			int		child_count;
			long	storage_used;
			
			if ( filters.size() == 0 ){
				
				child_count 	= kids.size();
				storage_used	= child_container.getStorageUsed();
			}else{
			
				child_count 	= 0;
				storage_used	= 0;
				
				for ( content kid: kids ){
					
					if ( media_server.isVisible( kid, filters, filter_args )){
						
						child_count++;
						storage_used	+= kid.getStorageUsed();
					}
				}
			}
			
			return(
				"<container id=\"" + child_container.getID() + "\" parentID=\"" + child_container.getParentID() + "\" childCount=\"" + child_count + "\" restricted=\"false\" searchable=\"true\">" +
				
					"<dc:title>" + escapeXML(child_container.getName()) + "</dc:title>" +
					"<upnp:class>" + CONTENT_CONTAINER + "</upnp:class>" +
					"<upnp:storageUsed>" + storage_used + "</upnp:storageUsed>" +
					"<upnp:writeStatus>WRITABLE</upnp:writeStatus>" +
	
				"</container>" );
			
		
		}else{
			contentItem	child_item = (contentItem)con;
			
			return( 
				"<item id=\"" + child_item.getID() + "\" parentID=\"" + child_item.getParentID() + "\" restricted=\"false\">" +
					child_item.getDIDL( host, client_type ) + 
				"</item>" );
		}
	}
	
	protected String
	createResourceID(
		byte[]					hash,
		DiskManagerFileInfo		file )
	{
		String	res =
			ByteFormatter.encodeString(hash) + "-" + file.getIndex();
		
		String	name = file.getFile().toString();
		
		int	pos = name.lastIndexOf('.');
		
		if ( pos != -1 && !name.endsWith(".")){
			
			res += name.substring( pos );
		}
		
		return( res );
	}

	protected int
	getSystemUpdateID()
	{
		return( system_update_id );
	}
	
	protected String
	escapeXML(
		String	str )
	{
		return( media_server.escapeXML(str));
	}

	protected int
	getPersistentContainerID(
		String		name )
	{
		synchronized( lock ){

			Long res = readConfig().get( name );
			
			if ( res != null ){
				
				return( res.intValue());
			}
		}
		
		return( -1 );
	}
	
	protected void
	persistentContainerIDAdded()
	{
		synchronized( lock ){
			
			if ( config_dirty  ){
				
				return;
			}
			
			config_dirty = true;
			
			new DelayedEvent( 
				"UPnPMS:CD:dirty",
				10*1000,
				new AERunnable()
				{
					public void
					runSupport()
					{
						writeConfig();
					}
				});
		}
	}
	
	protected Map<String,Long>
	readConfig()
	{
		synchronized( lock ){

			if ( config != null ){
				
				return( config );
			}
			
			File file = media_server.getPluginInterface().getPluginconfig().getPluginUserFile( "cd.dat" );
			
			// System.out.println( "Reading " + file );
			
			Map<String,Object> map = FileUtil.readResilientFile( file );
			
			config = (Map<String,Long>)map.get( "id_map" );
			
			if ( config == null ){
				
				config = new HashMap<String, Long>();
			}
			
			new DelayedEvent( 
				"UPnPMS:CD:dirty",
				30*1000,
				new AERunnable()
				{
					public void
					runSupport()
					{
						synchronized( lock ){
							
							config = null;
						}
					}
				});
			
			return( config );
		}
	}
	
	protected void
	writeConfig()
	{
		synchronized( lock ){
			
			if ( !config_dirty ){
				
				return;
			}
			
			File file = media_server.getPluginInterface().getPluginconfig().getPluginUserFile( "cd.dat" );

			// System.out.println( "Writing " + file );

			Map<String,Object> map = new HashMap<String, Object>();
			
			Map<String,Long> new_config = new HashMap<String, Long>();
			
			addPersistentContainerIDs( new_config, root_container );
			
			map.put( "id_map", new_config );
			
			FileUtil.writeResilientFile( file, map );

			config_dirty = false;
		}
	}
	
	protected void
	addPersistentContainerIDs(
		Map<String,Long>		map,
		contentContainer		container )
	{
		String	full_name  = container.getFullName( container.getName());
		
		map.put( full_name, new Long(container.getID()));
		
		List<content> kids = container.getChildren();
		
		for ( content kid: kids ){
			
			if ( kid instanceof contentContainer ){
				
				addPersistentContainerIDs( map, (contentContainer)kid );
			}
		}
	}
	
	protected void
	destroy()
	{
		writeConfig();
	}
	
	protected void
	print()
	{
		root_container.print( "" );
	}
	protected void
	log(
		String	str )
	{
		media_server.log( str );
	}
	
	protected void
	log(
		String		str,
		Throwable 	e )
	{
		media_server.log( str, e );
	}
	
	protected abstract class
	content
		implements Cloneable
	{
		private int						id;
		private contentContainer		parent;
		
		protected
		content(
			contentContainer		_parent )
		{
			this( _parent, null );
		}
			
		protected
		content(
			contentContainer		_parent,
			String					_name )
		{
			parent	= _parent;
			
			int	existing_oid = -1;
			
			if ( _name != null ){
				
				String	full_name = getFullName( _name );
				
				existing_oid = getPersistentContainerID( full_name );
				
				// System.out.println( "Container: " + full_name + " -> " + existing_oid );
			}
					
			synchronized( content_map ){
								
					// root is always 0 whatever, set up for subsequent
				
				if ( next_oid == 0 ){
					
					id			= 0;
					next_oid 	= (int)( SystemTime.getCurrentTime()/1000 );
					
				}else{
				
					if ( existing_oid != -1 ){
						
						id = existing_oid;
						
					}else{
						
						id = next_oid++;
					}
				}

				Integer	i_id = new Integer( id );
				
				while( content_map.containsKey( i_id )){
					
					i_id = id = next_oid++;
					
					existing_oid = -1;
				}
				
				content_map.put( i_id, this );
			}
			
			if ( _name != null && existing_oid == -1 ){
				
				persistentContainerIDAdded();
			}
		}
		
		protected int
		getID()
		{
			return( id );
		}
		
		protected String
		getFullName(
			String		name )
		{
			String	full_name = name;
				
			contentContainer current = parent;
				
			while( current != null ){
				
				full_name = current.getName() + "::" + full_name;
				
				current = current.getParent();
			}
	
			return( full_name );
		}
		
		protected int
		getParentID()
		{
			if ( parent == null ){
				
				return( -1 );
				
			}else{
				
				return( parent.getID());
			}
		}
		
		/*
		protected void
		setParent(
			contentContainer	_parent )
		{
			parent	= _parent;
		}
		*/
		
		protected contentContainer
		getParent()
		{
			return( parent );
		}

		protected abstract content
		getCopy(
			contentContainer	parent );

		protected abstract String
		getName();
		
		protected abstract String
		getContentClass();
		
		protected abstract long
		getDateMillis();
		
		protected abstract String[]
		getCategories();
		
		protected void
		deleted(
			boolean	is_link )
		{
			if ( !is_link ){
			
				synchronized( content_map ){
				
					if ( content_map.remove( new Integer( id )) == null ){
						
						Debug.out( "Content item with id " + id + " not found" );
					}
				}
			}
		}
		
		protected void
		invalidate()
		{
		}
		
		protected abstract long
		getStorageUsed();
		
		protected abstract void
		print(
			String	indent );
	}
	
	protected class
	contentContainer
		extends content
	{
		private AzureusContentDownload	download;
		private String					name;
		private List<content>			children 	= new ArrayList<content>();
		private int						update_id	= random.nextInt( Integer.MAX_VALUE );
		
		protected
		contentContainer(
			contentContainer	_parent,
			String				_name )
		{
			super( _parent, _name );
			
			name	= _name;
			
			if ( _parent != null ){
				
				_parent.addChildz( this );
			}
		}
		
		protected
		contentContainer(
			contentContainer			_parent,
			AzureusContentDownload		_download,
			String						_name )
		{		
			super( _parent, _name );
			
			download	= _download;
			name		= _name;
			
			if ( _parent != null ){
				
				_parent.addChildz( this );
			}
		}
		
		protected AzureusContentDownload
		getACD()
		{
			return( download );
		}
		
		protected content
		getCopy(
			contentContainer	parent )
		{
			contentContainer copy = new contentContainer( parent, download, name );
			
			for (int i=0;i<children.size();i++){
				
				children.get(i).getCopy( copy );
			}
			
			return( copy );
		}
		
		protected void
		addLink(
			content		child )
		{
			//logger.log( "Container: adding link '" + child.getName() + "' to '" + getName() + "'" );
				
			child.getCopy( this );
									
			updated();
		}
		
		protected content
		removeLink(
			String	child_name )
		{
			//logger.log( "Container: removing link '" + child_name + "' from '" + getName() + "'" );

			Iterator<content>	it = children.iterator();
				
			while( it.hasNext()){
				
				content	con = it.next();
				
				String	c_name = con.getName();
				
				if ( c_name.equals( child_name )){
						
					it.remove();
						
					updated();
						
					con.deleted( true );
					
					return( con );
				}
			}
			
			return( null );
		}
		
		protected void
		addChildz(
			content		child )
		{
			//logger.log( "Container: adding child '" + child.getName() + "' to '" + getName() + "'" );
						
			children.add( child );
			
			updated();
		}
		
		protected content
		removeChild(
			String	child_name )
		{
			//logger.log( "Container: removing child '" + child_name + "' from '" + getName() + "'" );

			Iterator<content>	it = children.iterator();
				
			while( it.hasNext()){
				
				content	con = (content)it.next();
				
				String	c_name = con.getName();
				
				if ( c_name.equals( child_name )){
						
					it.remove();
						
					updated();
						
					con.deleted( false );
					
					return( con );
				}
			}
			
			log( "    child not found" );
			
			return( null );
		}
		
		protected content
		getChild(
			String	child_name )
		{
			Iterator<content>	it = children.iterator();
			
			while( it.hasNext()){
				
				content	con = (content)it.next();
				
				String	c_name = con.getName();
				
				if ( c_name.equals( child_name )){

					return( con );
				}
			}
			
			return( null );
		}
		
		protected void
		updated()
		{
			update_id++;
		
			if ( update_id < 0 ){
				
				update_id = 0;
			}
			
			media_server.contentContainerUpdated( this );

			if ( getParent() != null ){
					
				getParent().updated();
				
			}else{
				
				system_update_id++;
				
				if ( system_update_id < 0 ){
					
					system_update_id = 0;
				}
			}
		}
		
		protected void
		invalidate()
		{
			update_id++;
			
			if ( update_id < 0 ){
				
				update_id = 0;
			}
			
			media_server.contentContainerUpdated( this );

			if ( getParent() == null ){
									
				system_update_id++;
				
				if ( system_update_id < 0 ){
					
					system_update_id = 0;
				}
			}
			
			Iterator<content>	it = children.iterator();
			
			while( it.hasNext()){

				content	con = it.next();
				
				con.invalidate();
			}
		}
		
		protected void
		deleted(
			boolean	is_link )
		{
			super.deleted( is_link );
			
			Iterator<content>	it = children.iterator();
			
			while( it.hasNext()){

				it.next().deleted( is_link );
			}
		}
		
		protected String
		getName()
		{
			return( name );
		}
		
		protected String
		getContentClass()
		{
			return( CONTENT_CONTAINER );
		}
		
		protected List<content>
		getChildren()
		{
			return( children );
		}
		
		protected int
		getUpdateID()
		{
			return( update_id );
		}
		
		protected long
		getStorageUsed()
		{
			long	res = 0;
			
			Iterator<content>	it = children.iterator();
			
			while( it.hasNext()){

				content	con = it.next();
				
				res += con.getStorageUsed();
			}
			
			return( res );
		}
		
		protected long
		getDateMillis()
		{
			if ( download == null || children.size() == 0 ){
				
				return( 0 );
			}
			
			return( children.get(0).getDateMillis());
		}
		
		protected String[]
		getCategories()
		{
			if ( download == null || children.size() == 0 ){
				
				return( new String[0] );
			}
			
			return( children.get(0).getCategories());
		}
		
		protected void
		print(
			String	indent )
		{
			log( indent + name + ", id=" + getID());
			
			indent += "    ";
			
			Iterator<content>	it = children.iterator();
			
			while( it.hasNext()){

				content	con = it.next();

				con.print( indent );
			}
		}
	}
	
	protected class
	contentItem
		extends content
		implements Cloneable
	{
		private AzureusContentFile		content_file;
		private byte[]					hash;
		private String					title;
		
		private boolean		valid;
				
		private String		content_type;
		private String		item_class;
		
		protected 
		contentItem(
			contentContainer		_parent,
			AzureusContentFile		_content_file,
			byte[]					_hash,
			String					_title )
		{
			super( _parent );
			
			try{
				content_file	= _content_file;
				hash			= _hash;
				title			= _title;
				
				DiskManagerFileInfo		file = content_file.getFile();
										
				String	file_name = file.getFile().getName();
										
				int	pos = file_name.lastIndexOf('.');
				
				if ( pos != -1 && !file_name.endsWith( "." )){
					
					String[]	entry = (String[])ext_lookup_map.get( file_name.substring( pos+1 ).toLowerCase( MessageText.LOCALE_ENGLISH ));
				
					if ( entry != null ){
						
						content_type	= entry[1];
						item_class		= entry[2];
						
						valid	= true;
					}
				}
				
				if ( !valid ){
					
					content_type	= "unknown/unknown";
					item_class		= CONTENT_UNKNOWN;
				}
			}finally{
				
				_parent.addChildz( this );
			}
		}
		
		protected content
		getCopy(
			contentContainer	parent )
		{
			try{
				content res = (content)clone();
				
				res.parent = parent;
				
				parent.addChildz( res );
				
				return( res );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				return( null );
			}
		}
		
		protected AzureusContentFile
		getACF()
		{
			return( content_file );
		}
		
		public DiskManagerFileInfo
		getFile()
		{
			return( content_file.getFile());
		}
				
		protected byte[]
		getHash()
		{
			return( hash );
		}
				
		protected String
		getTitle()
		{
			return( title );
		}
		
		protected String
		getDisplayTitle()
		{
			String t = getTitle();
			
			String str_percent = null;
			
			if ( media_server.showPercentDone()){
				
				long percent_done = getPercentDone();
				
				if ( percent_done >= 0 && percent_done < 1000 ){
					
					str_percent = DisplayFormatters.formatPercentFromThousands((int)percent_done);
				}
			}
			
			String	str_eta = null;
			
			if ( media_server.showETA()){
				
				long eta = getETA();
				
				if ( eta > 0 ){
				
					str_eta = TimeFormatter.format( eta );
				}
			}
			
			if ( str_percent != null || str_eta != null ){
				
				if ( str_percent == null ){
					
					t += " (" + str_eta + ")";
					
				}else if ( str_eta == null ){
					
					t += " (" + str_percent + ")";
					
				}else{
					
					t += " (" + str_eta + " - " + str_percent + ")";
				}
			}
			
			return( t );
		}
		
		protected String
		getCreator()
		{
			return( getStringProperty( AzureusContentFile.PT_CREATOR, "Unknown" ));
		}
		
		protected long
		getSize()
		{
			return( getFile().getLength());
		}
		
		protected long
		getAverageBitRate()
		{
			long	duration = getDurationMillis();
			
			if ( duration <= 0 ){
				
				return( 0 );
			}
			
			long	size = getSize();
			
			if ( size <= 0 ){
				
				return( 0 );
			}
			
			return( size*8*1000/duration );
		}
		
		protected int
		getPercentDone()
		{
			return( (int)getLongProperty( AzureusContentFile.PT_PERCENT_DONE, -1 ));
		}
		
		protected long
		getETA()
		{
			return( getLongProperty( AzureusContentFile.PT_ETA, -1 ));
		}
		
		protected long
		getDurationMillis()
		{
			return( getLongProperty( AzureusContentFile.PT_DURATION, 0 ));
		}
		
		protected String
		getDuration()
		{
				// hack for xbox "00:01:00.000";
			
			long 	millis = getLongProperty( AzureusContentFile.PT_DURATION, 1*60*1000 );
			
			long	secs = millis/1000;
			
			String	result = TimeFormatter.formatColon( secs );
			
			String ms = String.valueOf( millis%1000 );
			
			while( ms.length() < 3 ){
				
				ms = "0" + ms;
			}
			
			return( result + "." + ms );
		}

		protected String
		getResolution()
		{
			long 	width 	= getLongProperty( AzureusContentFile.PT_VIDEO_WIDTH, 0 );
			long 	height 	= getLongProperty( AzureusContentFile.PT_VIDEO_HEIGHT, 0 );

			if ( width > 0 && height > 0 ){
				
				return( width + "x" + height );
			}
			
				// default
			
			return( "640x480" );
		}
		
		protected long
		getDateMillis()
		{
			return( getLongProperty( AzureusContentFile.PT_DATE, 0 ));
		}
		
		protected String
		getDate()
		{
			long date_millis = getDateMillis();
			
			if ( date_millis == 0 ){
				
				return( null );
			}
			
			return( upnp_date_format.format(new Date( date_millis )));
		}
		
		protected String[]
		getCategories()
		{
			try{
				String[] cats = (String[])content_file.getProperty( AzureusContentFile.PT_CATEGORIES );
				
				if ( cats != null ){
					
					return( cats );
				}
			}catch( Throwable e ){
			}
			
			return( new String[0] );
		}
			
		protected String
		getProtocolInfo(
			String	attributes )
		{
			return( "http-get:*:" + content_type + ":" + attributes );
		}
		
		protected String
		getURI(
			String	host,
			int		stream_id )
		{
			return( "http://" + host + ":" + media_server.getContentServer().getPort() + "/Content/" + createResourceID( hash, getFile() ) + (stream_id==-1?"":("?sid=" + stream_id ))); 
		}
		
		protected String
		getResources(
			String		host,
			int			client_type )
		{
			String res = getResource( host, client_type, "*" );
			
			if ( client_type != CT_XBOX && item_class.equals( CONTENT_VIDEO )){
				
				try{
					DiskManagerFileInfo		file = content_file.getFile();
					
					String	file_name = file.getFile().getName();

						// hack to see if we can get something to work for Sony Bravia
					
					if ( file_name.toLowerCase().endsWith( ".vob" )){
				
						String attr = "DLNA.ORG_PN=MPEG_PS_NTSC;DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=01700000000000000000000000000000";
						
						res += getResource( host, client_type, attr );
					}
				}catch( Throwable e ){
					
				}
			}
			
			return( res );
		}
		
		protected String
		getResource(
			String		host,
			int			client_type,
			String		protocol_info_attributes )
		{
			LinkedHashMap<String,String>	attributes = new LinkedHashMap<String,String>();
			
			attributes.put( "protocolInfo", getProtocolInfo( protocol_info_attributes ));
			attributes.put( "size", String.valueOf( getFile().getLength()));
			
			if ( item_class.equals( CONTENT_VIDEO )){

				attributes.put( "duration", getDuration());
				attributes.put( "resolution", getResolution());

			}else if ( item_class.equals( CONTENT_AUDIO )){

				attributes.put( "duration", getDuration());
			}
				// experiment
			
			/*
			if ( item_class.equals( CONTENT_VIDEO )){
				attributes.put( "duration", "00:02:00.000" );
				attributes.put( "resolution", "480x270" );
				attributes.put( "bitsPerSample", "16" );
				attributes.put( "nrAudioChannels", "2" );
				attributes.put( "bitrate", "77355" );
				attributes.put( "sampleFrequency", "44100" );
			}
			*/
			
			if ( client_type == CT_XBOX ){
				
				if ( item_class.equals( CONTENT_VIDEO )){
				
					attributes.put( "bitrate", "500000" );
					
				}else if ( item_class.equals( CONTENT_AUDIO )){
					
					attributes.put( "nrAudioChannels", "2" );
					attributes.put( "bitrate", "4000" );
					attributes.put( "bitsPerSample", "16" );
					attributes.put( "sampleFrequency", "44100" );
					
				}else if ( item_class.equals( CONTENT_IMAGE )){
					
					attributes.put( "resolution", "100x100" );
				}
			}
			
			String	resource = "<res ";
			
			Iterator<String>	it = attributes.keySet().iterator();
			
			while( it.hasNext()){
				
				String	key = it.next();
				
				resource += key + "=\"" + attributes.get(key) + "\"" + (it.hasNext()?" ":"");
			}
			
			resource += ">" + getURI( host, -1 ) + "</res>";
			
			return( resource );
		}
		
		protected String
		getDIDL(
			String		host,
			int			client_type )
		{
				// for audio: dc:date 2005-11-07
				//			upnp:genre Rock/Pop
				//			upnp:artist
				// 			upnp:album
				//			upnp:originalTrackNumber
				
			String	hacked_class;
			
			if ( client_type == CT_XBOX ){
				
				if ( item_class.equals( CONTENT_VIDEO )){
					
					hacked_class = XBOX_CONTENT_VIDEO;
					
				}else if ( item_class.equals( CONTENT_AUDIO )){
					
					hacked_class = XBOX_CONTENT_AUDIO;
					
				}else if ( item_class.equals( CONTENT_IMAGE )){
					
					hacked_class = XBOX_CONTENT_IMAGE;
					
				}else{
					
					hacked_class = item_class;
				}
			}else{
				
				hacked_class = item_class;
			}
			
			String	didle = 
				"<dc:title>" + escapeXML( getDisplayTitle()) + "</dc:title>" +
				"<dc:creator>" +  escapeXML(getCreator()) + "</dc:creator>";
			
			String date = getDate();
			
			if ( date != null ){
				
				didle += "<dc:date>" + date + "</dc:date>";
			}
			
			didle +=
				
				"<upnp:class>" + hacked_class + "</upnp:class>" +
				//"<upnp:icon>" + escapeXML( "http://" + host + ":" + media_server.getContentServer().getPort() + "/blah" ) + "</upnp:icon>" +
				//"<albumArtURI>" + escapeXML( "http://" + host + ":" + media_server.getContentServer().getPort() + "/blah" ) + "</albumArtURI>" +
				getResources( host, client_type );
			
			if ( client_type == CT_XBOX ){
				
				if ( item_class.equals( CONTENT_AUDIO )){
					
					didle += 	"<upnp:genre>Unknown</upnp:genre>" +
								"<upnp:artist>Unknown</upnp:artist>" +
								"<upnp:album>Unknown</upnp:album>";
				}
			}

			return( didle );
		}
		
		protected String
		getName()
		{
			return( getTitle());
		}
		
		protected String
		getContentClass()
		{
			return( item_class );
		}
		
		protected String
		getContentType()
		{
			return( content_type );
		}
		
		protected void
		deleted(
			boolean	is_link )
		{
			super.deleted( is_link );
		}
		
		protected long
		getStorageUsed()
		{
			return( getFile().getLength());
		}
		
		protected String
		getStringProperty(
			String		name,
			String		def )
		{
			String	result = (String)content_file.getProperty( name );
			
			if ( result == null ){
				
				result	= def;
			}
			
			return( result );
		}
		
		protected long
		getLongProperty(
			String		name,
			long		def )
		{
			Long	result = (Long)content_file.getProperty( name );
			
			if ( result == null ){
				
				result	= def;
			}
			
			return( result );
		}
		
		protected void
		print(
			String	indent )
		{
			log( indent + getTitle() + ", id=" + getID() + ", class=" + item_class + ", type=" + content_type );
		}
	}

}