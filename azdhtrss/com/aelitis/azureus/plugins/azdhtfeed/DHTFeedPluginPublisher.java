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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

public class 
DHTFeedPluginPublisher 
{
	private final static String		NL		= "\r\n";
	
	private RSAPublicKey		publish_public_key;
	private RSAPrivateKey		publish_private_key;
	
	private TorrentAttribute	ta_publish_feed_desc;
	private TorrentAttribute	ta_publish_feed_content;

	private Map					publish_records			= new HashMap();
	
	private Map					published_hashes		= new HashMap();
	private Map					publishes_in_progress	= new HashMap();
	
	private DHTFeedPlugin		plugin;
	private PluginInterface		plugin_interface;
	
	private File				publish_data_dir;
	private File				temp_data_dir;

	private boolean				initialised;
	
	private LoggerChannel		log;
	
	protected 
	DHTFeedPluginPublisher(
		DHTFeedPlugin		_plugin,
		PluginInterface		_plugin_interface )
	{
		plugin				= _plugin;
		plugin_interface	= _plugin_interface;
		
		log	= plugin.getLog();
		
		ta_publish_feed_desc		= plugin_interface.getTorrentManager().getPluginAttribute( "publish_feed_desc" );
		ta_publish_feed_content		= plugin_interface.getTorrentManager().getPluginAttribute( "publish_feed_content" );
		
		publish_data_dir = new File( plugin_interface.getPluginDirectoryName(), "publish" );		
		publish_data_dir.mkdirs();
		
		temp_data_dir = new File( plugin_interface.getPluginDirectoryName(), "tmp" );		
		temp_data_dir.mkdirs();
	}
	
	protected void
	initialise(
		int	repub_mins )		
	{
		if ( !initialised ){
			
			initialised	= true;
		
			loadPublishRecords();
			
			try{
				PluginConfig config = plugin_interface.getPluginconfig();			
	
				if ( config.getPluginByteParameter( "modulus", null ) == null){
					
					generateKeys();
					
				}else{
					
					recoverKeys();
				}
				
				removeDeadPublishes();
				
				if ( repub_mins > 0 ){
					
					UTTimer pub = plugin_interface.getUtilities().createTimer( "publisher" );
					
					pub.addPeriodicEvent(
						repub_mins*60*1000,
						new UTTimerEventPerformer()
						{
							public void
							perform(
								UTTimerEvent		event )
							{
								republish();
							}
						});
				}
				
					// initial publish operation on start
				
				plugin_interface.getUtilities().createThread(
					"Initial publish",
					new Runnable()
					{
						public void
						run()
						{
								// wait a while for initialisation (especially of shares if we're feeding off of
								// ourselves) to complete
							
							try{
								Thread.sleep( 60*1000 );
								
							}catch( Throwable e ){
							}
							
							republish();
						}
					});
			
			}catch( Throwable e ){
				
				log.logAlert( "DHTFeedPlugin: Subscriber initialisation failed", e );
			}
		}
	}
	
	protected void
	publish(
		String	feed_name,
		String	feed_location,
		String	feed_network )
	{
		if ( feed_location.length() == 0 ){
			
			 log.log( "Publish location undefined!" );
			 
			 return;
		}	
		
		if ( feed_name.length() == 0 ){
			
			 log.log( "Publish name undefined!" );
			 
			 return;
		}
		
		try{
			
			synchronized( this ){
				
				Boolean	in_progress = (Boolean)publishes_in_progress.get( feed_name );
				
				if ( in_progress != null && in_progress.booleanValue()){
					
					log.log( "Publish in progress for '" + feed_name + "', ignoring" );
					
					return;
				}
				
				publishes_in_progress.put( feed_name, new Boolean( true ));
			}
			
			addPublishRecord( feed_name, feed_location, feed_network );
			
				// first verify that the publish-point torrent is available
				// this contains the feed name and public key details necessary to locate feed content
		
			publishDescription( feed_name, feed_network  );
			
				// sign the content torrent hash
			
			Torrent	content_torrent	= publishContent( feed_name, feed_location, feed_network );
			
			if ( content_torrent != null ){
				
				publishLink( feed_name, feed_network, content_torrent );
			}
			
		}finally{
			
			synchronized( this ){
		
				publishes_in_progress.put( feed_name, new Boolean( false ));
			}
		}
	}
	
	protected void
	unpublish(
		String		feed_name )
	{
		if ( feed_name.length() == 0 ){
			
			 log.log( "Unpublish: feed name undefined!" );
			 
			 return;
		}
		
		if ( removePublishRecord( feed_name ) == null ){
			
			log.log( "Unpublish: feed '" + feed_name + "' not found" );
			
			// carry on and remove torrents just in case
		}else{
			
			log.log( "Unpublish: feed name '" + feed_name + "' unpublished" );

		}
		
		Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
				
		for (int i=0;i<downloads.length;i++){
			
			Download	dl = downloads[i];
			
			String	attr = dl.getAttribute( ta_publish_feed_desc );
			
			if ( attr != null && attr.equals( feed_name )){
				
				plugin.removeDownload( dl, publish_data_dir, "unpublish of descriptor for '" + feed_name + "'" );
			}
			
			attr = dl.getAttribute( ta_publish_feed_content );
			
			if ( attr != null && attr.equals( feed_name )){
				
				plugin.removeDownload( dl, publish_data_dir, "unpublish of content for '" + feed_name + "'" );
			}
		}
	}
	
	protected boolean
	isPublishDesc(
		Download		download )
	{
		String	attr = download.getAttribute( ta_publish_feed_desc );

		if ( attr != null ){
			
			publishRecord	record = getPublishRecord( attr );

			return( record != null );
		}
		
		return( false );
	}
	
	protected Download
	getPublishContent(
		String		feed_name )
	{
		Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
		
		long	latest_date	= 0;
		
		Download	result = null;
		
		for (int i=0;i<downloads.length;i++){
			
			Download	download = downloads[i];
		
			if ( download.getTorrent() == null ){
				
				continue;
			}
			
			String attr = download.getAttribute( ta_publish_feed_content );
		
			if ( attr != null && attr.equals( feed_name )){
				
				long date = download.getTorrent().getCreationDate();
				
				if ( date > latest_date ){
					
					result	= download;
				}
			}
		}
		
		return( result );
	}
	
	protected void
	removeDeadPublishes()
	{
		Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
		
		for (int i=0;i<downloads.length;i++){
			
			Download	dl = downloads[i];
			
			String	attr = dl.getAttribute( ta_publish_feed_desc );
		
			if ( attr != null ){
				
				publishRecord	record = getPublishRecord( attr );

				if ( dl.getTorrent() == null || dl.getState() == Download.ST_ERROR || record == null ){
				
					plugin.removeDownload( dl, publish_data_dir, "dead publish description torrent for '" + attr + "'" );
				}
			}
			
			attr = dl.getAttribute( ta_publish_feed_content );
			
			if ( attr != null ){
				
				publishRecord	record = getPublishRecord( attr );

				if ( dl.getTorrent() == null || dl.getState() == Download.ST_ERROR || record == null ){
					
					plugin.removeDownload( dl, publish_data_dir, "dead publish content torrent for '" + attr + "'" );
				}
			}
		}
	}
			
	protected void
	republish()
	{
		Iterator	it;
		
		synchronized( this ){
		
			it = new ArrayList(publish_records.values()).iterator();
		}
		
		while( it.hasNext()){
			
			publishRecord	record = (publishRecord)it.next();
			
			try{
				log.log( "Republish for '" + record.getString() + "'" );
				
				publish( record.getFeedName(), record.getFeedLocation(), record.getFeedNetwork());
				
			}catch( Throwable e ){
				
				log.log(e);
			}
		}
		
			// anything published is always force started
		
		Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
				
		for (int i=0;i<downloads.length;i++){
			
			Download	dl = downloads[i];

			if ( 	dl.getAttribute( ta_publish_feed_content ) != null ||
					dl.getAttribute( ta_publish_feed_desc ) != null ){
				
				dl.setForceStart( true );
			}
		}
	}
	
	protected Torrent
	publishDescription(
		String				feed_name,
		String				feed_network )
	{
		String	desc_data = 
			"<DDB_FEED>" + NL +
				"\t<NAME>" + feed_name + "</NAME>" + NL +
				"\t<VERSION>1.1</VERSION>" + NL +
				"\t<DESC>To access this feed you need to run the Azureus 'DDB Feed' plugin</DESC>"+ NL +
				"\t<NETWORKS>" + feed_network + "</NETWORKS>" + NL +
				"\t<SIGNATURE>" + NL + 
					"\t\t<ALGORITHM>RSAWithMD5</ALGORITHM>" + NL +
					"\t\t<EXPONENT>" + publish_public_key.getPublicExponent().toString(32) + "</EXPONENT>" + NL +
					"\t\t<MODULUS>" + publish_public_key.getModulus().toString(32) + "</MODULUS>" + NL +
				"\t</SIGNATURE>" + NL +
			"</DDB_FEED>";
		
		InputStream	is = new ByteArrayInputStream( desc_data.getBytes());
		
		try{
			return( publishStuff( "Publish", ta_publish_feed_desc, DHTFeedPlugin.TAG_FEED_DESC, feed_name, feed_network, is, "text/xml", "feed.publish.xml" ));
			
		}finally{
			
			try{
				is.close();
				
			}catch( Throwable e ){
				
				log.log(e);
			}
		}
	}
	
	protected Torrent
	publishContent(
		String				feed_name,
		String				feed_location,
		String				feed_network )
	{
		try{
			File	test_file = new File( feed_location );
			
			if ( test_file.exists() && test_file.isDirectory()){
				
				log.log( "Feed location '" + feed_location + "' must not be a directory" );
				
				return( null );
			}
		}catch( Throwable e){
			//ignore
		}
		
			// now obtain the actual feed content and create the content torrent
		
		FileOutputStream	fos = null;
		
		try{
			DHTFeedPlugin.downloadDetails	details = plugin.downloadResource( feed_location );
			
			InputStream	is	= details.getInputStream();
			
			try{
				Date date = new Date(SystemTime.getCurrentTime());
				
				int	offset_hours = TimeZone.getDefault().getOffset(date.getTime())/(60*60*1000);
				
				String	tz_offset;
				
				if ( offset_hours == 0 ){
					tz_offset = "UTC";
				}else if ( offset_hours > 0 ){
					tz_offset = "UTC+" + offset_hours;
				}else{
					tz_offset = "UTC" + offset_hours;
					
				}
				SimpleDateFormat temp = new SimpleDateFormat("ddMMyyyy_HHmmss");
				
				String	date_str = temp.format( date ) + "_" + tz_offset; 
					
				return( publishStuff( "Content", ta_publish_feed_content, DHTFeedPlugin.TAG_FEED_CONTENT, feed_name, feed_network, is, details.getContentType(), "feed." + date_str + ".content" ));
				
			}finally{
				
				try{
					is.close();
					
				}catch( Throwable e ){
					
					log.log(e);
				}
			}
			
		}catch( Throwable e ){
			
			log.log( "Failed to download feed from '" + feed_location + "'", e );
			
			return( null );
			
		}finally{
			
			if ( fos != null ){
				
				try{
					fos.close();
				}catch( Throwable e){
				}
			}
		}
	}
	
	protected void
	publishLink(
		String	feed_name,
		String	feed_network,
		Torrent	content_torrent )
	{
		try{	
			byte[]	content_hash = content_torrent.getHash();
	
				// if signed hash not yet published, publish it
			
			synchronized( this ){
				
				if ( Arrays.equals( (byte[])published_hashes.get( feed_name ), content_hash)){
					
					return;
				}
			}
			
			Signature rsa_md5_signature = Signature.getInstance("MD5withRSA"); 
			
			rsa_md5_signature.initSign( publish_private_key );
																
			rsa_md5_signature.update( content_hash );
																
			byte[]	signature = rsa_md5_signature.sign();

			log.log( "Torrent signed:" +  ByteFormatter.encodeString( content_hash) + 
							" -> " + ByteFormatter.encodeString( signature ));
				
			byte[]	content_dht_key = plugin.getContentKeyBytes( feed_name, publish_public_key );
			
			byte[]	content_dht_value = new byte[ content_hash.length + signature.length ];
			
			System.arraycopy( content_hash, 0, content_dht_value, 0, content_hash.length );
			System.arraycopy( signature, 0, content_dht_value, content_hash.length, signature.length );
			
			log.log( "Registering current content map in DDB (value length = " + content_dht_value.length + ")" );
			
			DistributedDatabase ddb = plugin.getDDB( feed_network );
			
			if ( ddb != null ){
				
				DistributedDatabaseKey 		k = ddb.createKey(content_dht_key, "DDB Feed registering current content for '" + feed_name + "'");
				DistributedDatabaseValue 	v = ddb.createValue(content_dht_value);
				
				ddb.write(
						new DistributedDatabaseListener()
						{
							public void
							event(
								DistributedDatabaseEvent		event )
							{
								if ( event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
									
									log.log( "Registration complete" );
								}
							}
						},
						k, v );
				
				synchronized( this ){
					
					published_hashes.put( feed_name, content_hash );
				}
			}
			
		}catch( Throwable e ){
			
			log.log( e );
			
			return;
		}
	}
	
	protected Torrent
	publishStuff(
		String				type,
		TorrentAttribute	torrent_attribute,
		String				tag_name,
		String				feed_name,
		String				feed_network,
		InputStream			is,
		String				content_type,
		String				suffix )
	{
		final String	log_str = "'" + feed_name + "/" + suffix + "/" + type + "'";
					
		File file_tmp = new File( temp_data_dir, feed_name + "." + suffix );
		File file_act = new File( publish_data_dir, feed_name + "." + suffix );
		
		FileOutputStream	fos = null;
		
		try{
			fos = new FileOutputStream( file_tmp );
				
			byte[]	buffer = new byte[65536];
			
			while( true ){
				
				int	len = is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				fos.write( buffer, 0, len );
			}
			
			fos.close();
			
			fos = null;
			
			Torrent t = plugin_interface.getTorrentManager().createFromDataFile( file_tmp, new URL("dht:"));
			
			t.setAnnounceURL( new URL( "dht://" + ByteFormatter.encodeString( t.getHash()) + ".dht/announce" ));
			
			if ( content_type != null ){
				
				t.setPluginStringProperty( DHTFeedPlugin.TORRENT_CONTENT_TYPE_PROPERTY, content_type );
			}
			
			DownloadManager	download_manager = plugin_interface.getDownloadManager();
			
				// first check based on torrent equivalent - actually this will fail because the
				// name of the published torrent varies over time and this is included in the 
				// hash -> even if content is same, hash changes
			
			Download	old_content = download_manager.getDownload( t );
			
			Torrent		old_torrent	= old_content==null?null:old_content.getTorrent();
			
			if ( old_torrent != null ){
				
				log.log( "Torrent already running for " + log_str + " (identical torrent)");
				
				file_tmp.delete();
				
				return( old_torrent );
			}
			
				// so, we now do a test based on the actual content - i.e using the piece hashes
			
			
			old_content = getPublishContent( feed_name );
			old_torrent	= old_content==null?null:old_content.getTorrent();
			
			if ( old_torrent != null ){
			
				byte[][]	old_pieces 	= old_content.getTorrent().getPieces();
				byte[][]	new_pieces	= t.getPieces();
				
				boolean	same = true;
				
				if ( old_pieces.length == new_pieces.length ){
					
					for (int i=0;i<old_pieces.length;i++){
						
						if ( !Arrays.equals( old_pieces[i], new_pieces[i] )){
							
							same	= false;
							
							break;
						}
					}
				}else{
					same	 = false;
				}
				
				if ( same ){
				
					log.log( "Torrent already running for " + log_str + " (identical pieces)");
					
					file_tmp.delete();
					
					return( old_torrent );
				}
			}
			
			Download[]	downloads = download_manager.getDownloads();
						
			for (int i=0;i<downloads.length;i++){
				
				Download	dl = downloads[i];
				
				String	attr = dl.getAttribute( torrent_attribute );
				
				if ( attr != null && attr.equals( feed_name )){
					
						// we want to delay removal here for a couple of minutes to allow any
						// existing subscriptions to be picked up
					
						// might be > 1 of these if we shutdown with deletes outstanding
					
					final Download	f_existing	= dl;
					
					plugin_interface.getUtilities().createThread(
						"Publisher:delayed removal",
						new Runnable()
						{
							public void
							run()
							{
								try{
									Thread.sleep( 5*60*1000 );
								
									plugin.removeDownload( f_existing, publish_data_dir, log_str + " (delayed removal of old publish content)" );
									
								}catch( Throwable e ){
									
									log.log(e);
								}
							}
						});
				}
			}
							
			TorrentUtils.setFlag( PluginCoreUtils.unwrap( t ), TorrentUtils.TORRENT_FLAG_LOW_NOISE, true );

			
				// gotta do this after setting the flag as it causes a copy of the torrent to be cached
				// and re-read on addition and the flag has to be in the cache....
			
			t.setComplete( publish_data_dir );

			File	torrent_file = new File( file_act.toString() + ".torrent" );
			
			t.writeToFile( torrent_file );
			
			if ( file_act.exists() && !file_act.delete()){
				
				log.log( "Failed to delete '" + file_act + "'" );
			}
			
			if ( !file_tmp.renameTo( file_act )){
				
				log.log( "Failed to rename '" + file_tmp + "' to '" + file_act + "'" );
			}
			
			Download d = plugin.addDownload( t, torrent_file, publish_data_dir, feed_network );
			
			d.setFlag( Download.FLAG_DISABLE_AUTO_FILE_MOVE, true );
			d.setFlag( Download.FLAG_LOW_NOISE, true );

			d.setForceStart( true );
			
			d.setAttribute( torrent_attribute, feed_name );
			
			//d.setAttribute( plugin.getCategoryAttribute(), category );
			
			plugin.assignTag( d, tag_name );
			
			log.log( "Torrent added for " + log_str );
			
			return( t );
			
		}catch( Throwable e ){
			
			log.log(e);
			
			return( null );
			
		}finally{
			
			if ( fos != null ){
				
				try{
					fos.close();
					
				}catch( Throwable e){
					
					log.log(e);
				}
			}
		}
	}
	
	protected void
	generateKeys()
	{
		try{
			PluginConfig config = plugin_interface.getPluginconfig();			
	
			Object[]	details = createKey( "peer" );
			
			Map map = (Map)details[0];
			
			config.setPluginParameter( "modulus", 		(byte[])map.get( "modulus" ));
			config.setPluginParameter( "public_exp", 	(byte[])map.get( "public_exp" ));
			config.setPluginParameter( "private_exp", 	(byte[])map.get( "private_exp" ));
			
			config.save();
			
			publish_public_key	= (RSAPublicKey)details[1];
			publish_private_key	= (RSAPrivateKey)details[2];
		
			log.logAlert( LoggerChannel.LT_INFORMATION, "Public and Private keys created, backup your Azureus config!" );
			
		}catch( Throwable e ){
			
			log.logAlert( "DHTFeedPlugin: failed to generate feed keys", e );
		}
	}
	
	protected void
	recoverKeys()
	{
		try{
			PluginConfig config = plugin_interface.getPluginconfig();			
	
			byte[]	modulus				=  config.getPluginByteParameter( "modulus", null );
			byte[]	public_exponent		=  config.getPluginByteParameter( "public_exp", null );
			byte[]	private_exponent	=  config.getPluginByteParameter( "private_exp", null );
			
			Object[]	details = 
				recoverKeyPair(	
					"peer",
					modulus,
					public_exponent,
					private_exponent );
			
			publish_public_key	= (RSAPublicKey)details[0];
			publish_private_key	= (RSAPrivateKey)details[1];
			
		}catch( Throwable e ){
			
			log.logAlert( "DHTFeedPlugin: failed to recover feed keys, please restore config from backup", e );
		}
	}
	
	protected Object[]
  	createKey(
  		String	name )
  	
  		throws Exception
  	{
  		Map map = new HashMap();
  		
  		RSAPublicKey	public_key		= null;
  		RSAPrivateKey	private_key 	= null;

  		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
  				
  		keyGen.initialize( 2048 );
  				
  		KeyPair pair = keyGen.generateKeyPair();
  					
  		public_key	= (RSAPublicKey)pair.getPublic();
  		private_key	= (RSAPrivateKey)pair.getPrivate();
  		
  		BigInteger	modulus 			= public_key.getModulus();
  		BigInteger	public_exponent		= public_key.getPublicExponent();
  		BigInteger	private_exponent 	= private_key.getPrivateExponent();
  		
  		map.put( "modulus", modulus.toByteArray());
  		
  		map.put( "public_exp", 	public_exponent.toByteArray());
  		
  		map.put( "private_exp", private_exponent.toByteArray());
  		
  		log.log( "Generated new key for '" + name + "'" );
 		log.log( "    mod(" + modulus.toByteArray().length + ") = " + modulus.toString(32));
  		log.log( "    pub(" + public_exponent.toByteArray().length + ") = " + public_exponent.toString(32));
  		log.log( "    pri(" + private_exponent.toByteArray().length + ") = ..." );
  		
  		return( new Object[]{ map, public_key, private_key });
  	}     
	
	protected Object[]
  	recoverKeyPair(
  		String		name,
  		byte[]		_modulus,
  		byte[]		_public_exponent,
  		byte[]		_private_exponent )
  	
  		throws Exception
  	{
  		BigInteger	modulus 			= new BigInteger( _modulus );
  		
  		BigInteger	public_exponent 	= new BigInteger( _public_exponent );
  		
  		BigInteger	private_exponent 	= new BigInteger( _private_exponent );

  		RSAPublicKey	public_key		= null;
  		RSAPrivateKey	private_key 	= null;

  		KeyFactory key_factory = KeyFactory.getInstance("RSA");
  		
  		RSAPublicKeySpec 	public_key_spec = new RSAPublicKeySpec( modulus, public_exponent );

  		RSAPrivateKeySpec	private_key_spec = new RSAPrivateKeySpec(modulus, private_exponent );
  		
  		public_key 	= (RSAPublicKey)key_factory.generatePublic( public_key_spec );
  		
  		private_key = (RSAPrivateKey)key_factory.generatePrivate( private_key_spec );
  		
  		log.log( "Using existing key for '" + name + "'" );
  		log.log( "    mod(" + _modulus.length + ") = " + modulus.toString(32));
  		log.log( "    pub(" + _public_exponent.length + ") = " + public_exponent.toString(32));
  		log.log( "    pri(" + _private_exponent.length + ") = ..." );
  		
  		return( new Object[]{ public_key, private_key });
  	}
	
	
	
	protected synchronized void
	loadPublishRecords()
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		Map publishes = config.getPluginMapParameter( "publishes", new HashMap());

		Iterator	it = publishes.keySet().iterator();
		
		boolean	failed = false;
		
		while( it.hasNext()){
			
			String	feed_name = (String)it.next();
			
			try{
				Map	map = (Map)publishes.get(feed_name);
				
				byte[] b_network = (byte[])map.get( "network" );
				
				String 	feed_network = b_network==null?AENetworkClassifier.AT_PUBLIC:AENetworkClassifier.internalise( new String( b_network, "UTF-8" ));
				
				publishRecord	record = 
					new publishRecord( 
							feed_name, 
							new String((byte[])map.get( "location" ), "UTF-8" ),
							feed_network );
				
				publish_records.put( feed_name, record );
				
				log.log( "Loaded publish: " + record.getString());
				
			}catch( Throwable e ){
				
				failed = true;
				
				it.remove();
				
				log.log(e);
			}
		}
		
		if ( failed ){
			
			config.setPluginMapParameter( "publishes", publishes );
			
			try{
				config.save();
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected synchronized void
	addPublishRecord(
		String			feed_name,
		String			feed_location,
		String			feed_network )
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		Map publishes = config.getPluginMapParameter( "publishes", new HashMap());
		
		Map	map = new HashMap();
			
		try{
			map.put( "name", feed_name.getBytes( "UTF-8" ));
			map.put( "location", feed_location.getBytes( "UTF-8" ));
			map.put( "network", feed_network.getBytes( "UTF-8" ));
			
		}catch( UnsupportedEncodingException e ){
			
			Debug.out(e);
		}
		
		publishes.put( feed_name, map );
		
		publishRecord	record = new publishRecord( feed_name, feed_location, feed_network );
		
		publish_records.put( feed_name, record );

		config.setPluginMapParameter( "publishes", publishes );
		
		try{
			config.save();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected synchronized publishRecord
	removePublishRecord(
		String		feed_name )
	{
		PluginConfig	config = plugin_interface.getPluginconfig();
		
		Map publishes = config.getPluginMapParameter( "publishes", new HashMap());
		
		publishes.remove( feed_name );

		publishRecord	record = (publishRecord)publish_records.remove( feed_name );
		
		config.setPluginMapParameter( "publishes", publishes );
		
		try{
			config.save();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		return( record );
	}
	
	protected synchronized publishRecord
	getPublishRecord(
		String		feed_name )
	{
		return((publishRecord)publish_records.get(feed_name));
	}
	
	protected class
	publishRecord
	{
		private String		feed_name;
		private String		feed_location;
		private String		feed_network;
		
		protected
		publishRecord(
			String	_feed_name,
			String	_feed_location,
			String	_feed_network )
		{
			feed_name		= _feed_name;
			feed_location	= _feed_location;
			feed_network	= _feed_network;
		}
		
		protected String
		getFeedName()
		{
			return( feed_name );
		}
		
		protected String
		getFeedLocation()
		{
			return( feed_location );
		}
		
		protected String
		getFeedNetwork()
		{
			return( feed_network );
		}
		
		protected String
		getString()
		{
			return( "name = " + feed_name + ", location = " + feed_location + ", network=" + feed_network );
		}
	}
}
