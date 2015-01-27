/*
 * Created on 11 mars 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.plugins.rating.updater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatMessage;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginUtils;
import com.aelitis.azureus.plugins.rating.RatingPlugin;

public class 
RatingsUpdater 	
	implements DownloadManagerListener
{
	private static final int READ_PUBLIC_TIMEOUT 		= 20*1000;
	private static final int READ_NON_PUBLIC_TIMEOUT 	= 80*1000;
	
    private static final int WRITE_DELAY			= 10*1000;
    private static final int READ_DELAY				= 20*1000;
    
    private static final int READ_BY_HASH_TIMEOUT	= 30*1000;
    
    private static final int COMPLETE_DOWNLOAD_LOOKUP_PERIOD 			= 8*60*60*1000;
    private static final int COMPLETE_DOWNLOAD_NO_SEEDS_LOOKUP_PERIOD 	= 2*60*60*1000;
    private static final int INCOMPLETE_OLD_DOWNLOAD_LOOKUP_PERIOD		= 4*60*60*1000;
    private static final int INCOMPLETE_DOWNLOAD_LOOKUP_PERIOD 			= 1*60*60*1000;
    
    private static final int TIMER_CHECK_PERIOD		= 2*60*1000;
    private static final int STALL_TRIGGER_PERIOD	= 15*60*1000;
    
	private RatingPlugin 			plugin;
	private DistributedDatabase 	database_public;  
  	
	private TorrentAttribute attributeRating;
	private TorrentAttribute attributeComment;
	private TorrentAttribute attributeGlobalRating;
	private TorrentAttribute attributeChatState;
	private TorrentAttribute attributeStallState;

  
	private Map<Download,RatingResults> 	torrentRatings	= new HashMap<Download, RatingResults>();

	private LinkedList<Download>	downloads_to_publish 	= new LinkedList<Download>();
	private Map<Download,Long>		downloads_to_lookup		= new HashMap<Download,Long>();
	
	private AsyncDispatcher		write_dispatcher 	= new AsyncDispatcher( "rating:write" );
	private AsyncDispatcher		read_dispatcher 	= new AsyncDispatcher( "read" );
	
	private boolean	write_in_progress;
	private boolean	read_in_progress;
	
	private TimerEventPeriodic		timer;
	private Object					reseed_bytes_key = new Object();
	private Object					reseed_asked_key = new Object();
	
	private volatile boolean	destroyed;
	
	public 
	RatingsUpdater(
		RatingPlugin _plugin ) 
	{    
		plugin = _plugin; 
	   
	    attributeRating  		= plugin.getPluginInterface().getTorrentManager().getPluginAttribute("rating");
	    attributeComment		= plugin.getPluginInterface().getTorrentManager().getPluginAttribute("comment");    
	    attributeGlobalRating  	= plugin.getPluginInterface().getTorrentManager().getPluginAttribute("globalRating");
	    attributeChatState  	= plugin.getPluginInterface().getTorrentManager().getPluginAttribute("chatState");
	    attributeStallState  	= plugin.getPluginInterface().getTorrentManager().getPluginAttribute("stallState");
	}
	  
	public void 
	initialize() 
	{
		plugin.getPluginInterface().getUtilities().createThread(
				"Initializer", 
				new Runnable() 
				{
					public void 
					run() 
					{
						PluginInterface pluginInterface = plugin.getPluginInterface();

						database_public = pluginInterface.getDistributedDatabase();

						if ( !destroyed ){
							
							DownloadManager download_manager = pluginInterface.getDownloadManager();
							
							download_manager.addListener( RatingsUpdater.this, false );
							
							Download[] existing = download_manager.getDownloads();
							
							for ( Download d: existing ){
								
								downloadAdded( d, false );
							}
							
							timer = SimpleTimer.addPeriodicEvent(
								"ratings:checker",
								TIMER_CHECK_PERIOD,
								new TimerEventPerformer() {
									
									public void 
									perform(
										TimerEvent event) 
									{
										if ( destroyed ){
											
											TimerEventPeriodic t = timer;
											
											timer = null;
											
											if ( t != null ){
											
												t.cancel();
											}
											
											return;
										}
										
										checkStalls();
									}
								});
						}
					}
				});  
	}

	public void
	destroy()
	{
		destroyed = true;
		
		DownloadManager download_manager = plugin.getPluginInterface().getDownloadManager();
		
		download_manager.removeListener( this );
		
		TimerEventPeriodic t = timer;
		
		timer = null;
		
		if ( t != null ){
		
			t.cancel();
		}
	}
	
	public void 
	downloadAdded(
		Download download )
	{   
		downloadAdded( download, true );
	}
	
	private void 
	downloadAdded(
		Download 	download,
		boolean		is_new )
	{    
		if ( destroyed ){
			
			return;
		}
		
		if ( plugin.isRatingEnabled(download)){
			
			if ( loadRatingsFromDownload( download ).needPublishing()){
				
				addForPublish( download, false );
				
			}else{
				
				addForLookup( download, is_new );
			}
		}
	}

	public void 
	downloadRemoved(
		Download download ) 
	{
		synchronized( torrentRatings ){
		
			torrentRatings.remove( download );
			
			downloads_to_publish.remove( download );
			
			downloads_to_lookup.remove( download );
		}
	}

	private void
	checkStalls()
	{
		DownloadManager download_manager = plugin.getPluginInterface().getDownloadManager();

		long	now = SystemTime.getMonotonousTime();

		for ( final Download download: download_manager.getDownloads()){
				
			boolean	actively_monitoring = false;
			
			org.gudy.azureus2.core3.download.DownloadManager core_dm = PluginCoreUtils.unwrap( download );

			try{
				if ( download.getState() != Download.ST_DOWNLOADING ){
					
					continue;
				}
								
				if ( core_dm.getUserData( reseed_asked_key ) != null ){
					
					continue;
				}
	
				PEPeerManager pm = core_dm.getPeerManager();
				
				if ( pm == null ){
					
					continue;
				}
				
				actively_monitoring = true;
				
				long downloaded = core_dm.getStats().getTotalDataBytesReceived();
				
				long[] old_entry = (long[])core_dm.getUserData( reseed_bytes_key );
				
				if ( old_entry == null || old_entry[1] < downloaded ){
					
					core_dm.setUserData( reseed_bytes_key, new long[]{ now, downloaded });
	
					continue;
				}
				
				long elapsed_since_dl = now - old_entry[0];
				
				if ( elapsed_since_dl < STALL_TRIGGER_PERIOD ){
					
					continue;
				}
				
					// download made no progress in the last interval
				
				DiskManagerFileInfoSet file_set = core_dm.getDiskManagerFileInfoSet();
				
				DiskManagerFileInfo[] files = file_set.getFiles();			
				
				DiskManagerFileInfo	worst_file 	= null;
				float				worst_avail	= Float.MAX_VALUE;
				
				for ( DiskManagerFileInfo file: files ){
					
					if ( ( !file.isSkipped()) && file.getDownloaded() < file.getLength()){
						
						float file_avail = pm.getMinAvailability( file.getIndex());
						
						if ( file_avail < 1.0 ){
							
							if ( file_avail < worst_avail ){
								
								worst_avail = file_avail;
								worst_file	= file;
							}
						}
					}
				}
				
				if ( worst_file != null ){
					
					core_dm.setUserData( reseed_asked_key, "" );
					
					final String msg_prefix = "Please seed! File '";
					final String message 	= msg_prefix + worst_file.getTorrentFile().getRelativePath() + "' is stuck with an availability of " + worst_avail;
					
					if ( BuddyPluginUtils.isBetaChatAvailable()){
						
						chat_write_dispatcher.dispatch(
								new AERunnable() {
									
									@Override
									public void 
									runSupport() 
									{
										final BuddyPluginBeta.ChatInstance chat = BuddyPluginUtils.getChat( download );
											
										if ( chat != null ){
												
											chat.setAutoNotify( true );
											
											final Runnable do_write = 
												new Runnable()
												{
													public void
													run()
													{		
														Map<String,Object>	flags 	= new HashMap<String, Object>();
														
														flags.put( BuddyPluginBeta.FLAGS_MSG_ORIGIN_KEY, BuddyPluginBeta.FLAGS_MSG_ORIGIN_RATINGS );
														
														Map<String,Object>	options = new HashMap<String, Object>();
														
														chat.sendMessage( message, flags, options );
													}
												};
											
											String str = download.getAttribute( attributeStallState );

											boolean	 written = str != null && str.equals( "w" );
												
											if ( written ){
												
												final TimerEventPeriodic[] event = { null };
												
												event[0] = 
													SimpleTimer.addPeriodicEvent(
														"Rating:chat:checker",
														30*1000,
														new TimerEventPerformer()
														{
															private int elapsed_time;
					
															public void 
															perform(
																TimerEvent e ) 
															{
																elapsed_time += 30*1000;
																
																	// get sync state before messages to things work reliably
																
																int	sync_state = chat.getIncomingSyncState();
																
																List<ChatMessage>	messages = chat.getMessages();
																
																if ( messages.size() > 50 || chat.isDestroyed()){
																	
																		// busy, let's not even bother checking 
																	
																	event[0].cancel();
																	
																}else{
																
																
																	for ( ChatMessage message: messages ){
																		
																		if ( message.getParticipant().isMe()){
																			
																			if ( message.getMessage().startsWith( msg_prefix )){
																				
																				event[0].cancel();
																				
																				return;
																			}
																		}
																	}
																	
																	if ( 	sync_state == 0 ||
																			elapsed_time >= 5*60*1000 ){
																		
																		do_write.run();
																		
																		event[0].cancel();
																	}
																}
															}
														});
											}else{
												
												do_write.run();
												
												download.setAttribute( attributeStallState, "w" );
											}
										}
									}
								});
					}
				}else{
					
					actively_monitoring = false;
				}
			}finally{
				
				if ( !actively_monitoring ){
					
					core_dm.setUserData( reseed_bytes_key, null );
				}
			}
		}
	}
	
	private void
	addForLookup(
		Download	download,
		boolean		is_priority )
	{
		if ( download.isRemoved()){
			
			downloadRemoved( download );
			
			return;
		}
		
		synchronized( torrentRatings ){
			
			Long data = downloads_to_lookup.get( download );
			
			if ( data == null || is_priority ){
				
				downloads_to_lookup.put( download, is_priority?-1L:0L );
			}
		}
		
		read_dispatcher.dispatch(
			new AERunnable()
			{
				private AERunnable dispatcher = this;

				public void
				runSupport()
				{
					if ( destroyed ){
						
						return;
					}
					
					if ( read_dispatcher.getQueueSize() > 0 ){
						
						return;
					}
					
					Download 	next_download 	= null;
					
					long	now = SystemTime.getCurrentTime();
					
					int	num_ready = 0;
					
					synchronized( torrentRatings ){
						
						if ( read_in_progress || downloads_to_lookup.size() == 0 ){
							
							return;
						}
						
						long		next_lookup		= 0;
						Download	priority_dl		= null;
						
						for ( Map.Entry<Download, Long> entry: downloads_to_lookup.entrySet()){
							
							Download 	dl 			= (Download)entry.getKey();
							long		last_lookup = (Long)entry.getValue();
							
							if ( last_lookup == -1 ){
								
									// priority
								
								num_ready++;
								
								next_download 	= dl;
								next_lookup		= now;
								
								priority_dl = dl;
																
							}else if ( last_lookup == 0 ){
								
									// first time
								
								num_ready++;
								
								if ( next_download == null || next_lookup > now ){
									
									next_download 	= dl;
																		
								}else{
									
									if ( !dl.isComplete( false )){
										
										next_download = dl;
									}
								}
								
								next_lookup		= now;

							}else{
								
								long	next = last_lookup;
								
								if ( dl.isComplete( false )){
									
									int delay = COMPLETE_DOWNLOAD_LOOKUP_PERIOD;
									
									if ( dl.getState() == Download.ST_STOPPED ){
									
										DownloadScrapeResult res = dl.getAggregatedScrapeResult();
										
										if ( res != null && res.getSeedCount() == 0 ){
											
												// check for possible stalled dl information more frequently
											
											delay = COMPLETE_DOWNLOAD_NO_SEEDS_LOOKUP_PERIOD;
										}
									}
									
									next += delay;
									
								}else{
									
									org.gudy.azureus2.core3.download.DownloadManager	core_download = PluginCoreUtils.unwrap( dl );
									
									long added = core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);

									if ( added > 0 && now - added > 24*60*60*1000 ){
										
										next += INCOMPLETE_OLD_DOWNLOAD_LOOKUP_PERIOD;
										
									}else{
									
										next += INCOMPLETE_DOWNLOAD_LOOKUP_PERIOD;
									}
								}
								
								if ( next <= now ){
									
									num_ready++;
								}
								
								if ( next_lookup == 0 || next < next_lookup ){
									
									next_lookup 	= next;
									next_download	= dl;
								}
							}
						}
						
						if ( priority_dl != null ){
							
							next_download 	= priority_dl;
							next_lookup		= now;
						}
						
						if ( next_lookup > now ){
							
							log( "Lookup sleeping for " + ( next_lookup - now ));
							
							SimpleTimer.addEvent(
									"Rating:read:next",
									next_lookup,
									new TimerEventPerformer()
									{
										public void 
										perform(
											TimerEvent event )
										{														
											read_dispatcher.dispatch( dispatcher );
										}
									});
							
							next_download = null;
							
						}else{
							
							if ( next_download != null ){
								
								downloads_to_lookup.put( next_download, now );
							
								read_in_progress = true;
							}
						}
					}
					
					log( "Lookup queue size: " + num_ready );

					if ( next_download != null ){
																
						readRating(
							next_download,
							new CompletionListener()
							{
								public void 
								operationComplete(
									RatingResults	results )
								{
									synchronized( torrentRatings ){
										
										read_in_progress = false;
																					
										SimpleTimer.addEvent(
											"Rating:read:delay",
											SystemTime.getCurrentTime() + READ_DELAY,
											new TimerEventPerformer()
											{
												public void 
												perform(
													TimerEvent event )
												{														
													read_dispatcher.dispatch( dispatcher );
												}
											});
										
									}
								}
							});
					}
				}
			});
	}
	
	private void
	addForPublish(
		Download	download,
		boolean		is_priority )
	{
		if ( download.isRemoved()){
			
			downloadRemoved( download );
			
			return;
		}
		
		synchronized( torrentRatings ){
			
			downloads_to_publish.remove( download );
			
			if ( is_priority || downloads_to_publish.size() == 0 ){
				
				downloads_to_publish.addFirst( download );
				
			}else{
				
				downloads_to_publish.add( RandomUtils.nextInt( downloads_to_publish.size()), download );
			}
		}
		
		write_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					if ( destroyed ){
						
						return;
					}

					if ( write_dispatcher.getQueueSize() > 0 ){
						
						return;
					}
					
					Download download;
					
					synchronized( torrentRatings ){
						
						if ( write_in_progress ){
							
							return;
						}
						
						if ( downloads_to_publish.size() > 0 ){
							
							download = downloads_to_publish.removeFirst();
							
							write_in_progress = true;
							
						}else{
							
							download = null;
						}
					}
					
					log( "Publish queue size: " + downloads_to_publish.size());
					
					if ( download != null ){
												
						final AERunnable dispatcher = this;
						
						writeRating(
							download,
							new CompletionListener()
							{
								public void 
								operationComplete(
									RatingResults	results )
								{
									synchronized( torrentRatings ){
										
										write_in_progress = false;
										
										if ( downloads_to_publish.size() > 0 ){
											
											SimpleTimer.addEvent(
												"Rating:write:delay",
												SystemTime.getCurrentTime() + WRITE_DELAY,
												new TimerEventPerformer()
												{
													public void 
													perform(
														TimerEvent event )
													{														
														write_dispatcher.dispatch( dispatcher );
													}
												});
										}
									}
								}
							});
					}
				}
			});
	}
	
	public RatingResults 
	getRatingsForDownload(
		Download d )
	{
		RatingResults result = null;

		synchronized( torrentRatings ){

			result = torrentRatings.get( d );       
		}

		if ( result != null ){

			return result;
		}

		result = new RatingResults();

		try{
			String str = d.getAttribute( attributeGlobalRating );

			if ( str != null ){

				float f = Float.parseFloat(str);

				result.setAverage(f);
				
			}else{
				
				RatingData rd = loadRatingsFromDownload( d );
				
				int score = rd.getScore();
				
				if ( score > 0 ){
					
					result.setAverage( score );
				}
			}
		}catch( Throwable e ){

			Debug.out( e );
		}

		synchronized( torrentRatings ){
			
			if ( !torrentRatings.containsKey( d )){
				
				torrentRatings.put( d, result );
			}
		}
		
		return( result );
	}


	public RatingData 
	loadRatingsFromDownload(
		Download download ) 
	{
		String strRating = download.getAttribute(attributeRating);	
		String comment   = download.getAttribute(attributeComment);		

		int rating = 0;
		
		if ( strRating != null ){
			
			try{
				rating = Integer.parseInt(strRating);

			}catch( Throwable e ){

				e.printStackTrace();
			}
		}
		
		if (comment == null ){
			
			comment = "";
		}
		
		String nick = plugin.getNick();
		
		return( new RatingData( rating,nick,comment ));   
	}

	
	public void 
	storeRatingsToDownload(
		Download 	download,
		RatingData 	data ) 
	{
		RatingData oldData = loadRatingsFromDownload(download);
		
		boolean updateNeeded = false;
		
		updateNeeded |= oldData.getScore() != data.getScore();
		updateNeeded |= !oldData.getComment().equals(data.getComment());

		if (updateNeeded ){
			
			download.setAttribute(attributeRating,"" + data.getScore());
			download.setAttribute(attributeComment, data.getComment());
		}

		updateNeeded |= !oldData.getNick().equals(data.getNick());
				
		if ( updateNeeded ){
			
			addForPublish( download, true );
		}
	}

	public void 
	readRating(
		final byte[]				hash,
		final CompletionListener 	listener )
	{	
		if ( database_public == null ){
			
			listener.operationComplete( null );
			
			return;
		}
		
		List<DistributedDatabase>	ddbs = new ArrayList<DistributedDatabase>();
		
		ddbs.add( database_public );
		
		readRating( ddbs, hash, listener );
	}
	
	public void 
	readRating(
		final List<DistributedDatabase>	ddbs,
		final byte[]					hash,
		final CompletionListener 		listener )
	{	
		DistributedDatabase ddb = null;
		
		for ( DistributedDatabase x: ddbs ){
			
			if ( x.getNetwork() == AENetworkClassifier.AT_PUBLIC ){
				
				ddb = x; 
				
				break;
				
			}else{
				
				ddb = x;
			}
		}
		
		if ( ddb == null || !ddb.isAvailable()){
			
			listener.operationComplete( null );
			
			return;
		}
				
		final String hash_str =  ByteFormatter.encodeString( hash );
		
		try{
			log( hash_str + " : getting rating" );
			
			DistributedDatabaseKey ddKey = ddb.createKey(KeyGenUtils.buildRatingKey( hash ),"Ratings read: " + hash_str);
			
			boolean	is_pub = ddb.getNetwork() == AENetworkClassifier.AT_PUBLIC;
			
			int timeout = READ_BY_HASH_TIMEOUT;
			
			if ( !is_pub ){
				
				timeout *= 2;
			}
			
			ddb.read(
				new DistributedDatabaseListener() 
				{
					List<DistributedDatabaseValue> results = new ArrayList<DistributedDatabaseValue>();
	
					public void 
					event(
						DistributedDatabaseEvent event) 
					{
						if (event.getType() == DistributedDatabaseEvent.ET_VALUE_READ ){
							
							results.add(event.getValue());
							
						}else if (	event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE || 
									event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
							
							RatingResults ratings = new RatingResults();

							try{
								log( hash_str + " : Rating read complete - results=" + results.size());
								
		
								for ( int i = 0 ; i < results.size() ; i++ ){
		
									DistributedDatabaseValue value = results.get(i);
		
									try{
										byte[] bValue = (byte[]) value.getValue(byte[].class);
		
										RatingData data = new RatingData(bValue);
		
										log("        " + data.getScore() + ", " + value.getContact().getName() + ", " +  data.getNick() + " : " + data.getComment());
		
										ratings.addRating( data, true );
		
									}catch( Throwable e ){
		
										Debug.out( e );
									}                            
								}
								
							}finally{
							
								listener.operationComplete( ratings );
							}
						}
				}
			}, ddKey, timeout, DistributedDatabase.OP_PRIORITY_HIGH );   

		}catch( Throwable e ){

			Debug.out( e );
			
			listener.operationComplete( null );
		}
	}

	private AsyncDispatcher		chat_read_dispatcher 	= new AsyncDispatcher( "Ratings:crd" );
	private AsyncDispatcher		chat_write_dispatcher 	= new AsyncDispatcher( "Ratings:cwd" );

	
	private void 
	readRating(
		final Download 				download,
		final CompletionListener 	listener )
	{	
		List<DistributedDatabase> ddbs = plugin.getDDBs( download );
		
		if ( ddbs.size() == 0 ){
			
			listener.operationComplete( null );
			
			return;
		}
		
			// if public network is there then we always just read from that
		
		DistributedDatabase	database = ddbs.get(0);
		
		for ( int i=1;i<ddbs.size();i++){
			
			DistributedDatabase d = ddbs.get(i);
			
			if ( d.getNetwork() == AENetworkClassifier.AT_PUBLIC ){
				
				database = d;
				
				break;
			}
		}
		
		if ( BuddyPluginUtils.isBetaChatAvailable()){

			chat_read_dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						Map<String,Object> peek_data = BuddyPluginUtils.peekChat( download );
												
						Number	message_count 	= (Number)peek_data.get( "m" );
						Number	node_count 		= (Number)peek_data.get( "n" );
						
						if ( message_count != null && node_count != null ){
							
							if ( message_count.intValue() > 0 ){
								
								BuddyPluginBeta.ChatInstance chat = BuddyPluginUtils.getChat( download );
			
								if ( chat != null ){
									
									chat.setAutoNotify( true );
								}
							}
						}		
					}
				});
		}
		
		final String torrentName = download.getTorrent().getName();
		
		try{
			log( torrentName + " : getting rating" );
			
			DistributedDatabaseKey ddKey = database.createKey(KeyGenUtils.buildRatingKey(download),"Ratings read: " + torrentName);
			
			database.read(
				new DistributedDatabaseListener() 
				{
					List<DistributedDatabaseValue> results = new ArrayList<DistributedDatabaseValue>();
	
					public void 
					event(
						DistributedDatabaseEvent event) 
					{
						if (event.getType() == DistributedDatabaseEvent.ET_VALUE_READ ){
							
							results.add(event.getValue());
							
						}else if (	event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE || 
									event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
							
							RatingResults ratings = new RatingResults();

							try{
								log( torrentName + " : Rating read complete - results=" + results.size());
									
								if ( results.size() == 0 ){
									
										// missed our own data it seems
									
									RatingData my_data = loadRatingsFromDownload( download );
									
									if ( my_data.needPublishing()){
									
										ratings.addRating( my_data, false );
									}
									
								}else{
									for ( int i = 0 ; i < results.size() ; i++ ){
			
										DistributedDatabaseValue value = results.get(i);
			
										try{
											byte[] bValue = (byte[]) value.getValue(byte[].class);
			
											RatingData data = new RatingData(bValue);
			
											log("        " + data.getScore() + ", " + value.getContact().getName() + ", " +  data.getNick() + " : " + data.getComment());
			
											ratings.addRating( data, false );
			
										}catch( Throwable e ){
			
											Debug.out( e );
										}                            
									}
								}
								
								synchronized( torrentRatings ){
		
									torrentRatings.put( download, ratings );
		
									download.setAttribute( attributeGlobalRating, "" + ratings.getRealAverageScore());
								}			
							}finally{
							
								listener.operationComplete( ratings );
							}
						}
				}
			}, ddKey, database.getNetwork()==AENetworkClassifier.AT_PUBLIC?READ_PUBLIC_TIMEOUT:READ_NON_PUBLIC_TIMEOUT );   

		}catch( Throwable e ){

			Debug.out( e );
			
			listener.operationComplete( null );
		}
	}


	private void 
	writeRating(      
		final Download 				download,
		final CompletionListener 	_listener ) 
	{
		final List<DistributedDatabase> ddbs = plugin.getDDBs( download );

		if ( ddbs.size() == 0 ){
			
			_listener.operationComplete( null );
			
			return;
		}
		
		final RatingData data = loadRatingsFromDownload( download );
		
		int score = data.getScore();
		
			//Non scored torrent aren't published,
			//Comments require score
		
		if ( score == 0 ){
			
			_listener.operationComplete( null );
			
			return;
		}
		
		final CompletionListener listener = 
			new CompletionListener()
			{
				int	num_done = 0;
				
				public void operationComplete(RatingResults ratings) {
				
					synchronized( this ){
						num_done++;
						
						if ( num_done < ddbs.size()){
							
							return;
						}
					}
					
					_listener.operationComplete( null );
					
					addForLookup( download, true );
				}
			};
			
		if ( BuddyPluginUtils.isBetaChatAvailable()){
			
			chat_write_dispatcher.dispatch(
					new AERunnable() {
						
						@Override
						public void 
						runSupport() 
						{
							final BuddyPluginBeta.ChatInstance chat = BuddyPluginUtils.getChat( download );
								
							if ( chat != null ){
									
								chat.setAutoNotify( true );
								
								String msg = "Rating: " + data.getScore();
								
								String comment = data.getComment().trim();
								
								if ( comment.length() > 0 ){
									
									msg += ", " + comment;
								}
								
								final String f_msg = msg;
								
								final Runnable do_write = 
									new Runnable()
									{
										public void
										run()
										{		
											Map<String,Object>	flags 	= new HashMap<String, Object>();
											
											flags.put( BuddyPluginBeta.FLAGS_MSG_ORIGIN_KEY, BuddyPluginBeta.FLAGS_MSG_ORIGIN_RATINGS );
											
											Map<String,Object>	options = new HashMap<String, Object>();
											
											chat.sendMessage( f_msg, flags, options );
										}
									};
								
								String str = download.getAttribute( attributeChatState );

								boolean	 written = str != null && str.equals( "w" );
									
								if ( written ){
									
									final TimerEventPeriodic[] event = { null };
									
									event[0] = 
										SimpleTimer.addPeriodicEvent(
											"Rating:chat:checker",
											30*1000,
											new TimerEventPerformer()
											{
												private int elapsed_time;
		
												public void 
												perform(
													TimerEvent e ) 
												{
													elapsed_time += 30*1000;
													
														// get sync state before messages to things work reliably
													
													int	sync_state = chat.getIncomingSyncState();
													
													List<ChatMessage>	messages = chat.getMessages();
													
													if ( messages.size() > 50 || chat.isDestroyed()){
														
															// busy, let's not even bother checking 
														
														event[0].cancel();
														
													}else{
													
													
														for ( ChatMessage message: messages ){
															
															if ( message.getParticipant().isMe()){
																
																if ( message.getMessage().equals( f_msg )){
																	
																	event[0].cancel();
																	
																	return;
																}
															}
														}
														
														if ( 	sync_state == 0 ||
																elapsed_time >= 5*60*1000 ){
															
															do_write.run();
															
															event[0].cancel();
														}
													}
												}
											});
								}else{
									
									do_write.run();
									
									download.setAttribute( attributeChatState, "w" );
								}
							}
						}
					});
		}
		
		for ( DistributedDatabase database: ddbs ){
			
			try{
				final String torrentName = download.getTorrent().getName();
	
				byte[] value = data.encodes();
	
				log( torrentName + " : publishing rating" );
				
				DistributedDatabaseKey ddKey = database.createKey(KeyGenUtils.buildRatingKey(download),"Ratings write: " + torrentName);
				
				DistributedDatabaseValue ddValue = database.createValue(value);
	
				ddKey.setFlags( DistributedDatabaseKey.FL_ANON );
	
				database.write(
					new DistributedDatabaseListener() 
					{
						public void 
						event(
							DistributedDatabaseEvent event) 
						{
							if( event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
							
								log(torrentName + " : rating write ok");
								
								listener.operationComplete( null );
																						
							}else if( event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
								
								log( torrentName + " : rating write failed" );
								
								listener.operationComplete( null );								
							}
						}
					}, ddKey, ddValue );
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				listener.operationComplete( null );
			}
		}
	} 


	private void
	log(
		String	str )
	{
		plugin.logInfo( str );
	}
}
