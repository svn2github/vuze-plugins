/*
 * Created on 23-May-2005
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

package com.aelitis.azureus.plugins.azdhtfeed;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.web.TrackerAuthenticationListener;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

import com.aelitis.azureus.plugins.magnet.MagnetPlugin;

public class 
DHTFeedPluginSubscriber 
	implements TrackerWebPageGenerator
{
	private DHTFeedPlugin		plugin;
	private PluginInterface		plugin_interface;
	
	private TorrentAttribute	ta_subscribe_feed_desc;
	private TorrentAttribute	ta_subscribe_feed_content;

	private File	subscribe_data_dir;
	private File	temp_data_dir;

	private Map					subscription_records	= new HashMap();
	
	private Set					replicated_signed_data	= new HashSet();
	
	private BooleanParameter 	subscribe_port_local;
	
	private boolean				initialised;
	private LoggerChannel		log;


	protected 
	DHTFeedPluginSubscriber(
		DHTFeedPlugin		_plugin,
		PluginInterface		_plugin_interface )
	{
		plugin				= _plugin;
		plugin_interface	= _plugin_interface;
		
		log	= plugin.getLog();
		
		ta_subscribe_feed_desc		= plugin_interface.getTorrentManager().getPluginAttribute( "subscribe_feed_desc" );
		ta_subscribe_feed_content	= plugin_interface.getTorrentManager().getPluginAttribute( "subscribe_feed_content" );
		
		subscribe_data_dir = new File( plugin_interface.getPluginDirectoryName(), "subscribe" );		
		subscribe_data_dir.mkdirs();
		
		temp_data_dir = new File( plugin_interface.getPluginDirectoryName(), "tmp" );		
		temp_data_dir.mkdirs();
	}
	
	protected void
	initialise(
		int					port,
		int					refresh_mins,
		BooleanParameter 	_subscribe_port_local)
	{
		if ( !initialised ){
			
			initialised	= true;
		
			subscribe_port_local	= _subscribe_port_local;
			
			try{
				loadSubscriptionRecords();
				
				TrackerWebContext ctx = plugin_interface.getTracker().createWebContext( port, Tracker.PR_HTTP );
					
					// no authentication required for the moment
				
				ctx.addAuthenticationListener(
					new TrackerAuthenticationListener()
					{
						public boolean
						authenticate(
							URL			resource,
							String		user,
							String		password )
						{
							return( true );
						}
																			
						public byte[]
						authenticate(
							URL			resource,
							String		user )
						{
							return( null );
						}
					});
				
				ctx.addPageGenerator( DHTFeedPluginSubscriber.this );
				
				log.log( "Subscription running on port " + port );
	
				removeDeadSubscribes();
				
				if ( refresh_mins > 0 ){
					
					UTTimer pub = plugin_interface.getUtilities().createTimer( "subscribe refresher" );
					
					pub.addPeriodicEvent(
							refresh_mins*60*1000,
						new UTTimerEventPerformer()
						{
							public void
							perform(
								UTTimerEvent		event )
							{
								refresh();
							}
						});
				}
					
				// initial publish operation on start
				
				plugin_interface.getUtilities().createThread(
					"Initial subscribe",
					new Runnable()
					{
						public void
						run()
						{
							refresh();
						}
					});
								
					// TODO: manage lifetime of seeding of content torrents to ensure availability
					
			}catch( Throwable e ){
				
				log.logAlert( "DHTFeedPlugin: Subscriber initialisation failed", e );
			}
		}
	}
	
	protected boolean
	subscribe(
		final String	feed_location )
	{
		if ( feed_location.length() == 0 ){
			
			log.log( "Subscribe: feed location missing!" );
			
			return( false );
		}
		
		try{
			byte[] data = plugin.inputStreamToByteArray( plugin.downloadResource( feed_location ).getInputStream());
				
			if ( data.length == 0 ){
					
				throw( new Exception( "No data returned" ));
			}
			
			final Download[]	desc_download = { null };
			
			if ( data[0] != '<' ){
							
				final Torrent	t = plugin_interface.getTorrentManager().createFromBEncodedData( data );
				
				File	f = new File( subscribe_data_dir, t.getName() + ".torrent" );
				
				t.writeToFile( f );
				
				try{
					DownloadManager	download_manager = plugin_interface.getDownloadManager();
					
					DownloadManagerListener	l = 
						new DownloadManagerListener()
						{
							public void
							downloadAdded(
								Download	download )
							{
								Torrent x = download.getTorrent();
								
								if ( x != null && Arrays.equals( x.getHash(), t.getHash())){
									
									desc_download[0] = download;
								}
							}
							
							public void
							downloadRemoved(
								Download	download )
							{
							}
						};
						
					ResourceDownloaderFactory rdf = plugin_interface.getUtilities().getResourceDownloaderFactory();
					
					try{
						download_manager.addListener( l );
					
						data = plugin.inputStreamToByteArray( rdf.getTorrentDownloader( rdf.create( f ), true, subscribe_data_dir ).download());
						
					}finally{
						
						download_manager.removeListener( l );
					}
				}finally{
					
					f.delete();
				}
			}
			
			Download	download = desc_download[0];
			
			if ( download != null ){
				
				download.setForceStart( false );
			}
			
			SimpleXMLParserDocument	doc = plugin_interface.getUtilities().getSimpleXMLParserDocumentFactory().create( new ByteArrayInputStream( data ));

			log.log( "Subscription to '" + feed_location + "' ok" );
		
			final String	feed_name = doc.getChild( "NAME" ).getValue();
			
			SimpleXMLParserDocumentNode	sig = doc.getChild( "SIGNATURE" );
			
			BigInteger	mod = new BigInteger( sig.getChild( "MODULUS").getValue(), 32 );
			BigInteger	exp = new BigInteger( sig.getChild( "EXPONENT").getValue(), 32 );
			
			try{
				RSAPublicKey	feed_key = recoverPublicKey( mod, exp );
				
				subscriptionRecord	record = addSubscriptionRecord( feed_name, feed_key );
				
				if ( download != null ){
										
					download.setAttribute( ta_subscribe_feed_desc, record.getContentKey());
					
					download.setAttribute( plugin.getCategoryAttribute(), DHTFeedPlugin.CATEGORY_FEED_DESC );
				}
			
				refresh( record );
				
				return( true );
				
			}catch( Throwable e ){
				
				log.log( "Failed to decode subscription details", e );
			}
		}catch( Throwable e ){
			
			log.log( "Subscription to '" + feed_location + "' failed", e );
		}
		
		return( false );
	}
	
	protected void
	unsubscribe(
		String	feed_name )
	{
		if ( feed_name.length() == 0 ){
			
			log.log( "Unsubscribe: feed name missing!" );
			
			return;
		}
		
			// currently we don't do this properly as we don't have the public key, search for first matching feed
				
		subscriptionRecord	record = removeSubscriptionRecordUsingFeedName( feed_name );
		
		if ( record == null ){
			
			log.log( "Unsubscribe: feed name '" + feed_name + "' not found" );
			
		}else{
			
			log.log( "Unsubscribe: feed name '" + feed_name + "' unsubscribed" );

			String	content_key	= record.getContentKey();
			
			Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
					
			for (int i=0;i<downloads.length;i++){
				
				Download	dl = downloads[i];
				
				String	attr = dl.getAttribute( ta_subscribe_feed_desc );
				
				if ( attr != null && attr.equals( content_key )){
					
					plugin.removeDownload( dl, subscribe_data_dir, "unsubscribe of descriptor for '" + feed_name + "'" );
				}
				
				attr = dl.getAttribute( ta_subscribe_feed_content );
				
				if ( attr != null && attr.equals( content_key )){
					
					plugin.removeDownload( dl, subscribe_data_dir, "unsubscribe of content for '" + feed_name + "'" );
				}
			}
		}
	}
	
	protected boolean
	isSubscriptionDesc(
		Download		download )
	{
		String	attr = download.getAttribute( ta_subscribe_feed_desc );

		if ( attr != null ){
			
			subscriptionRecord	record = getSubscriptionRecord( attr );

			return( record != null );
		}
		
		return( false );
	}
	
	protected void
	refresh()
	{
		Iterator	it;
	
		synchronized( this ){
			
			it = new ArrayList(subscription_records.values()).iterator();
		}
		
		while( it.hasNext()){
			
			refresh((subscriptionRecord)it.next());
		}
	}
	
	protected void
	refresh(
		final	subscriptionRecord		record )
	{
		try{
			byte[]	content_dht_key = plugin.getContentKeyBytes( record.getFeedName(), record.getFeedKey());
			
			log.log( "Looking up current content map for '" + record.getFeedName() + "'" );
			
			DistributedDatabase ddb = plugin_interface.getDistributedDatabase();
		
			DistributedDatabaseKey 		k = ddb.createKey(content_dht_key, "DDB Feed looking up current content for '" + record.getFeedName() + "'" );
			
			ddb.read(
					new DistributedDatabaseListener()
					{
						private int		bad;
						private int		ok;
						
						private long	latest_time;
						private byte[]	latest_hash;
						private byte[]	latest_signed_value;
						
						public void
						event(
							DistributedDatabaseEvent		event )
						{
							int	type = event.getType();
							
							if ( type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
								
								log.log( "Lookup complete" );
								
								refresh( record, latest_time, latest_hash, latest_signed_value, ok );
								
							}else if ( type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
									
								log.log( "Lookup timeout" );
									
								refresh( record, latest_time, latest_hash, latest_signed_value, ok );

							}else if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
								
								try{
									DistributedDatabaseValue	value = event.getValue();
									
									byte[] val = (byte[])value.getValue(byte[].class);
									
									if ( val.length > 20 ){
									
										byte[]	hash 		= new byte[20];
										byte[]	sig_read	= new byte[val.length-20];
										
										System.arraycopy( val, 0, hash, 0, 20 );
										System.arraycopy( val, 20, sig_read, 0, val.length - 20 );
										
										Signature rsa_md5_signature = Signature.getInstance("MD5withRSA"); 
										
										rsa_md5_signature.initVerify( record.getFeedKey() );
																							
										rsa_md5_signature.update( hash );
											
										if ( rsa_md5_signature.verify( sig_read )){
										
											log.log( "Signature verify ok for read from " + value.getContact().getName());
											
											ok++;
											
											if ( value.getCreationTime() > latest_time ){
												
												latest_time 		= value.getCreationTime();
												
												latest_hash			= hash;
												
												latest_signed_value	= val;
											}
										}else{
											
											log.log( "Signature verify fails for read from " + value.getContact().getName());
											
											bad++;
										}

									}else{
										log.log( "Bad data read from " + value.getContact().getName());

										bad++;
									}
								}catch( Throwable e ){
									
									bad++;
									
									log.log(e);
								}
							}
						}
					},
					k, 60*1000 );
			
		}catch( Throwable e ){
			
			log.log( "Failed to lookup current key", e );
		}
	}
	
	protected void
	refresh(
		final subscriptionRecord	record,
		final long					time,
		final byte[]				hash,
		final byte[]				signed_value,
		final int					ok )
	{
			// get off callback thread from DHT
		
		plugin_interface.getUtilities().createThread(
			"processSubscription",
			new Runnable()
			{
				public void
				run()
				{
					refreshSupport( record, time, hash, signed_value, ok );
				}
			});
	}
	
	protected void
	refreshSupport(
		final subscriptionRecord	record,
		long						time,
		byte[]						hash,
		byte[]						signed_value,
		int							ok )
	{
			// store the latest value in case we need to replicate it later
		
		if ( signed_value != null && time > record.getTime()){
			
			record.setSignedValue( signed_value );
		}
		
			// we have a policy of trying to ensure that sufficient mappings are published so that if the 
			// original publisher goes away we still maintain the latest mapping
	
		if ( ok < 3 ){
	
			byte[]	latest_signed_value = record.getSignedValue();
			
			if ( latest_signed_value != null ){
				
				HashWrapper	hw = new HashWrapper( latest_signed_value );
				
				boolean	replicate = false;
				
				synchronized( this ){
							
					if ( !replicated_signed_data.contains( hw )){
					
						replicated_signed_data.add( hw );
					
						replicate	= true;
					}
				}
				
				if ( replicate ){
					
					log.log( "Replicated subscription link as insufficient copies available" );
					
					try{
						byte[]	content_dht_key = plugin.getContentKeyBytes( record.getFeedName(), record.getFeedKey());

						DistributedDatabase ddb = plugin_interface.getDistributedDatabase();
					
						DistributedDatabaseKey 		k = ddb.createKey(content_dht_key, "DDB Feed looking up current content for '" + record.getFeedName() + "'" );
						
						ddb.write(
								new DistributedDatabaseListener()
								{
									public void
									event(
										DistributedDatabaseEvent		event )
									{
										int	type = event.getType();
										
										if ( type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
											
											log.log( "Replication complete" );
										}
									}
								},
								k,
								ddb.createValue( latest_signed_value ));
						
					}catch( Throwable e ){
						
						log.log( e );
					}
				}
			}
		}
	
		final String	feed_name	= record.getFeedName();
		
		String	content_key	= record.getContentKey();
		
		if ( hash == null ){
			
			log.log( "Subscription processing complete for '" + feed_name + "', no content found" );	
			
			return;
		}
		
				// see if we have a current subscription
		
		Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
		
		final List	existing_content = new ArrayList();
		
		for (int i=0;i<downloads.length;i++){
			
			Download	download = downloads[i];
		
			String	attr = download.getAttribute( ta_subscribe_feed_content );
			
			if ( attr == null ){
				
				continue;
			}
			
			if ( attr.equals( content_key )){
				
					// found the content for this feed
				
				if ( Arrays.equals( hash, download.getTorrent().getHash())){
					
					log.log( "Subscription for '" + feed_name + "' is current (same content)" );
					
					return;
				}
				
				if ( time <= record.getTime()){
				
					log.log( "Subscription for '" + feed_name + "' is current (more recent content)" );
					
					return;
				}
				
				existing_content.add( download );
			}
		}
		
					// more recent content available
		
		URL	magnet_url = ((MagnetPlugin)plugin_interface.getPluginManager().getPluginInterfaceByClass( MagnetPlugin.class ).getPlugin()).getMagnetURL( hash );
		
		try{
			byte[]	data = plugin.inputStreamToByteArray( plugin.downloadResource( magnet_url.toString()).getInputStream());
	
			if ( data.length == 0 ){
			
				throw( new Exception( "No sources found" ));
			}
			
			log.log( "Subscription for '" + feed_name + "': downloaded new content torrent" );

			Torrent	t = plugin_interface.getTorrentManager().createFromBEncodedData( data );
			
			File	f = new File( subscribe_data_dir, t.getName() + ".torrent" );
			
			t.writeToFile( f );

			try{
				Download	d = plugin_interface.getDownloadManager().addDownload( t, f, subscribe_data_dir );
				
				d.setForceStart( true );
							
				record.setTime( time );
							
				d.setAttribute( ta_subscribe_feed_content, content_key );
				
				d.setAttribute( plugin.getCategoryAttribute(), DHTFeedPlugin.CATEGORY_FEED_CONTENT );
								
				d.addListener(
					new DownloadListener()
					{
						public void
						stateChanged(
							Download		download,
							int				old_state,
							int				new_state )
						{
							if ( download.getStats().getDownloadCompleted(false) == 1000 ){
								
								download.removeListener( this );
								
								download.setForceStart( false );
								
								log.log( "Subscription for '" + feed_name + "': new content downloaded" );
	
								for (int i=0;i<existing_content.size();i++){
									
									Download	existing = (Download)existing_content.get(i);
									
									plugin.removeDownload( existing, subscribe_data_dir, "old subscription content for '" + feed_name + "'" );
								}
							}
						}
		
						public void
						positionChanged(
							Download	download, 
							int oldPosition,
							int newPosition )
						{
						}
					});
			}finally{
				
				f.delete();
			}
		}catch( Exception e ){
			
			log.log( "Subscription for '" + feed_name + "' content download failed", e );
		}
	}

	protected void
	removeDeadSubscribes()
	{
		Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
		
		Map	most_recent	= new HashMap();
		
		for (int i=0;i<downloads.length;i++){
			
			Download	dl = downloads[i];
			
			String	content_key = dl.getAttribute( ta_subscribe_feed_desc );
		
			if ( content_key != null ){
						
				subscriptionRecord	record = getSubscriptionRecord( content_key );
			
				if ( dl.getTorrent() == null || dl.getState() == Download.ST_ERROR || record == null ){
					
					plugin.removeDownload( dl, subscribe_data_dir, "dead subscription description torrent for '" + content_key + "'" );
				}
			}
			
			content_key = dl.getAttribute( ta_subscribe_feed_content );
				
			if ( content_key != null ){
				
				subscriptionRecord	record = getSubscriptionRecord( content_key );
				
				if ( dl.getTorrent() == null || dl.getState() == Download.ST_ERROR  || record == null ){
						
					plugin.removeDownload( dl, subscribe_data_dir, "dead subscription content torrent for '" + content_key + "'" );
					
				}else{
										
					Download mr = (Download)most_recent.get( content_key );
					
					if ( mr == null ){
						
						most_recent.put( content_key, dl );
						
					}else{
						
						if ( dl.getTorrent().getCreationDate() > mr.getTorrent().getCreationDate()){
							
							plugin.removeDownload( mr, subscribe_data_dir, "old subscription content torrent for '" + record.getFeedName() + "'" );
							
							most_recent.put( content_key, dl );
							
						}else{
							
							plugin.removeDownload( dl, subscribe_data_dir, "old subscription content torrent for '" + record.getFeedName() + "'" );
						}
					}
				}
			}
		}
	}
	
	public InputStream
	getSubscriptionContent(
		String		feed_location )
	
		throws Exception
	{
			// see if magnet URI : magnet:?xt=urn:btih:BR4I7URIV7SAYTLP4ZXANUGWVHFU2Z63
		
		int	pos = feed_location.indexOf( "btih:" );
		
		if ( pos == -1 ){
			
			throw( new Exception( "Magnet URL format error: " + feed_location ));
		}
			
		byte[] hash = Base32.decode( feed_location.substring( pos+5));

		Download publish_download = plugin_interface.getDownloadManager().getDownload( hash );
		
		subscriptionRecord content_record = null;
		
		if ( publish_download != null ){

			String	desc_key = publish_download.getAttribute( ta_subscribe_feed_desc );
			
			if ( desc_key != null ){
			
				content_record = getSubscriptionRecord( desc_key );
			}
		}
		
		if ( content_record == null ){
	
			log.log( "getSubscriptionContent: attempting to subscribe to feed '" + feed_location + "'" );
			
			if ( subscribe( feed_location )){
			
				throw( new Exception( "Subscribed to feed '" + feed_location + "', waiting for content" ));
			
			}else{
			
				throw( new Exception( "Failed to subscribe to feed '" + feed_location + "'" ));
			}
		}else{
			
			Download		content_download	= plugin.getPublishContent( content_record.getFeedName());

			if ( content_download == null ){
								
				String	content_key = content_record.getContentKey();
					
				Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
							
				for (int i=0;i<downloads.length;i++){
					
					Download	download = downloads[i];
					
					if ( download.isComplete() && download.getTorrent() != null ){
						
						String	this_key = download.getAttribute( ta_subscribe_feed_content );
						
						if ( this_key != null ){
							
							if ( this_key.equals( content_key )){
								
									// get most uptodate content
								
								if ( content_download == null ){
									
									content_download	= download;
									
								}else if ( download.getTorrent().getCreationDate() > content_download.getTorrent().getCreationDate()){
									
									content_download	= download;
								}
							}
						}
					}
				}
			}
						
			if ( content_download == null ){
					
				throw( new Exception( "Subscribed to feed '" + feed_location + "', waiting for content" ));
				
			}else{
				
				File	content_file = content_download.getDiskManagerFileInfo()[0].getFile();
				
				return( new FileInputStream( content_file ));
			}
		}
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url	= request.getURL();
				
		log.log( request.getClientAddress() + " -> " + url );
	
		if (subscribe_port_local.getValue()){
			
			InetAddress addr = InetAddress.getByName( request.getClientAddress());
					
			if ( 	NetworkInterface.getByInetAddress( addr ) == null &&
					!addr.isLoopbackAddress()){
				
				log.log( "   access denied" );
				
				response.setReplyStatus( 404 );
				
				return( true );	
			}
		}
		
		int	pos = url.indexOf( "feed=" );
		
		if ( pos == -1 ){
			
			response.setReplyStatus( 404 );
			
			return( true );
		}
		
		String	feed_name = URLDecoder.decode( url.substring( pos+5), "UTF-8" );
		
			// test for locally published content first to avoid interference with idiots that
			// subscribe to a local feed explicitly :P
		
		Download		content_download	= plugin.getPublishContent( feed_name );

		if ( content_download == null ){
			
			subscriptionRecord	record = getSubscriptionRecordUsingFeedName( feed_name );
		
			if ( record != null ){
			
				String	content_key = record.getContentKey();
				
				Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
						
				for (int i=0;i<downloads.length;i++){
					
					Download	download = downloads[i];
					
					if ( download.isComplete() && download.getTorrent() != null ){
						
						String	this_key = download.getAttribute( ta_subscribe_feed_content );
						
						if ( this_key != null ){
							
							if ( this_key.equals( content_key )){
								
									// get most uptodate content
								
								if ( content_download == null ){
									
									content_download	= download;
									
								}else if ( download.getTorrent().getCreationDate() > content_download.getTorrent().getCreationDate()){
									
									content_download	= download;
								}
							}
						}
					}
				}
			}
		}
					
		if ( content_download != null ){
			
			Torrent torrent = content_download.getTorrent();
			
			String	ct = torrent.getPluginStringProperty( DHTFeedPlugin.TORRENT_CONTENT_TYPE_PROPERTY );
			
			if ( ct != null ){
				
				response.setContentType( ct );
			}
			
			File	content_file = content_download.getDiskManagerFileInfo()[0].getFile();
			
			OutputStream	os = response.getOutputStream();
			
			//os.write( 	(	"HTTP/1.1 200 OK" + NL + 
			//				"Content-Length: " + content.length() + NL + NL ).getBytes());
			
			InputStream	is = new FileInputStream( content_file );
			
			try{
				byte[]	buffer = new byte[65536];
				
				while( true ){
					
					int	len = is.read( buffer );
					
					if ( len <= 0 ){
						
						break;
					}
					
					os.write( buffer, 0, len );
				}
			}finally{
				
				is.close();
			}
			
			return( true );
		}
		
		response.setReplyStatus( 404 );
		
		return( true );
	}
	
	protected RSAPublicKey
 	recoverPublicKey(
 		byte[]		_modulus,
 		byte[]		_public_exponent )
 	
 		throws Exception
  	{
  		BigInteger	modulus 			= new BigInteger( _modulus );
  		
  		BigInteger	public_exponent 	= new BigInteger( _public_exponent );
  		
		return( recoverPublicKey( modulus, public_exponent ));
  	}
	
	protected RSAPublicKey
 	recoverPublicKey(
		BigInteger		modulus,
		BigInteger		public_exponent )
 	
 		throws Exception
  	{
  		RSAPublicKey	public_key		= null;

  		KeyFactory key_factory = KeyFactory.getInstance("RSA");
  		
  		RSAPublicKeySpec 	public_key_spec = new RSAPublicKeySpec( modulus, public_exponent );
 		
  		public_key 	= (RSAPublicKey)key_factory.generatePublic( public_key_spec );
 		
 		return( public_key );
  	}
	
	protected synchronized void
	loadSubscriptionRecords()
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		Map subscriptions = config.getPluginMapParameter( "subscriptions", new HashMap());

		Iterator	it = subscriptions.values().iterator();
		
		while( it.hasNext()){
						
			Map	map = (Map)it.next();
			
			try{
				subscriptionRecord	record = subscriptionRecord.fromMap( this, map );
				
				subscription_records.put( record.getContentKey(), record );
				
				log.log( "Loaded subscription: " + record.getString());
				
			}catch( Throwable e ){
				
				it.remove();
				
				log.log( e );
			}
		}
		
		config.setPluginMapParameter( "subscriptions", subscriptions );
	}
	
	protected void
	updateSubscriptionRecord(
		subscriptionRecord	record )
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		Map subscriptions = config.getPluginMapParameter( "subscriptions", new HashMap());
									
		subscriptions.put( record.getContentKey(), record.toMap());
	
		config.setPluginMapParameter( "subscriptions", subscriptions );
	}
	
	protected synchronized subscriptionRecord
	addSubscriptionRecord(
		String			feed_name,
		RSAPublicKey	feed_key )
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		Map subscriptions = config.getPluginMapParameter( "subscriptions", new HashMap());
						
		subscriptionRecord	record = new subscriptionRecord( this, feed_name, feed_key, 0, null );
			
		String	content_key = record.getContentKey();
		
		subscriptions.put( content_key, record.toMap());
	
		subscription_records.put( content_key, record );

		config.setPluginMapParameter( "subscriptions", subscriptions );
		
		return( record );
	}
	
	protected synchronized void
	removeSubscriptionRecord(
		subscriptionRecord	record )
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		Map subscriptions = config.getPluginMapParameter( "subscriptions", new HashMap());
		
		String	content_key = record.getContentKey();

		subscriptions.remove( content_key );
		
		subscription_records.remove( content_key );
		
		config.setPluginMapParameter( "subscriptions", subscriptions );
	}
	
	protected subscriptionRecord
	removeSubscriptionRecordUsingFeedName(
		String			feed_name )
	{
		subscriptionRecord	record = getSubscriptionRecordUsingFeedName( feed_name );
			
		if ( record != null ){
			
			removeSubscriptionRecord( record );
		}
		
		return( record );
	}
	
	protected synchronized subscriptionRecord
	getSubscriptionRecordUsingFeedName(
		String			feed_name )
	{
		Iterator	it = subscription_records.values().iterator();
		
		while( it.hasNext()){
						
			subscriptionRecord	record = (subscriptionRecord)it.next();
			
			if  ( record.getFeedName().equals( feed_name )){
								
				return( record );
			}
		}
		
		return( null );
	}
	
	protected synchronized subscriptionRecord
	getSubscriptionRecord(
		String	content_key )
	{
		return((subscriptionRecord)subscription_records.get(content_key));
	}
	
	protected static class
	subscriptionRecord
	{
		private DHTFeedPluginSubscriber		subscriber;
		private String						feed_name;
		private RSAPublicKey				feed_key;
		private long						time;
		private byte[]						signed_value;
		
		protected
		subscriptionRecord(
			DHTFeedPluginSubscriber		_subscriber,
			String						_feed_name,
			RSAPublicKey				_feed_key,
			long						_time,
			byte[]						_signed_value )
		{
			subscriber		= _subscriber;
			feed_name		= _feed_name;
			feed_key		= _feed_key;
			time			= _time;
			signed_value	= _signed_value;
		}
		
		protected Map
		toMap()
		{
			Map	map = new HashMap();
			
			map.put( "name", feed_name.getBytes());
			map.put( "mod", feed_key.getModulus().toByteArray());
			map.put( "exp", feed_key.getPublicExponent().toByteArray());
			map.put( "time", new Long(0));
			map.put( "sig", signed_value );
			
			return( map );
		}
		
		protected static subscriptionRecord
		fromMap(
			DHTFeedPluginSubscriber		subscriber,
			Map							map )
		
			throws Exception
		{	
			byte[]	mod 	= (byte[])map.get("mod");
			byte[]	exp 	= (byte[])map.get("exp");
			Long	time	= (Long)map.get( "time" );
			byte[]	sig		= (byte[])map.get( "sig" );
			
			RSAPublicKey feed_key = subscriber.recoverPublicKey( mod, exp );
			
			String	feed_name	= new String((byte[])map.get( "name" ));
			
			subscriptionRecord	record = 
				new subscriptionRecord( subscriber, feed_name, feed_key, time==null?0:time.longValue(), sig );

			return( record );
		}
		
		protected String
		getFeedName()
		{
			return( feed_name );
		}
		
		protected RSAPublicKey
		getFeedKey()
		{
			return( feed_key );
		}
		
		protected String
		getContentKey()
		{
			return( subscriber.plugin.getContentKey( feed_name, feed_key ));
		}
		
		protected long
		getTime()
		{
			return( time );
		}
		
		protected void
		setTime(
			long	_time )
		{
			time			= _time;
			
			subscriber.updateSubscriptionRecord( this );
		}
		
		protected byte[]
		getSignedValue()
		{
			return( signed_value );
		}
		
		protected void
		setSignedValue(
			byte[]	_signed_value )
		{
			signed_value	= _signed_value;
			
			subscriber.updateSubscriptionRecord( this );
		}
		
		protected String
		getString()
		{
			return( "name = " + feed_name + 
					", time=" + (time==0?"unknown":subscriber.plugin_interface.getUtilities().getFormatters().formatDate( time ))+ 
					", key = [" +
						feed_key.getPublicExponent().toString(32) + "," +
						feed_key.getModulus().toString(32) + "]" );
		}
	}
}
