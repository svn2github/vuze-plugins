/*
 * Created on May 15, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.networks.i2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

public class 
I2PHelperNetworkMixer 
	implements DownloadAttributeListener, DownloadManagerListener
{
	private static final int MS_NONE				= 0;
	private static final int MS_CHANGING			= 1;
	private static final int MS_ACTIVE				= 2;
	private static final int MS_MANUAL				= 3;
	
	private I2PHelperPlugin		plugin;
	private PluginInterface		plugin_interface;
	private TorrentAttribute 	ta_networks;
	private TorrentAttribute 	ta_mixstate;

	private volatile boolean		enabled;
	private int						incomplete_limit;
	private int						complete_limit;
	
	private static final int MIN_CHECK_PERIOD 	= 60*1000;
	private static final int RECHECK_PERIOD 	= 5*60*1000;
	
	private static final int MIN_ACTIVE_PERIOD 	= 20*60*1000;
	
	private TimerEventPeriodic	recheck_timer;
	
	private long		last_check		= -MIN_CHECK_PERIOD;
	private TimerEvent	check_pending 	= null;
	private boolean		check_active;
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( "i2pmixer", 5*1000 );
	
	private boolean	destroyed;
	
	protected
	I2PHelperNetworkMixer(
		I2PHelperPlugin			_plugin,
		final BooleanParameter	enable_param,
		final IntParameter		incomp_param,
		final IntParameter		comp_param )
	{
		plugin				= _plugin;
		plugin_interface	= plugin.getPluginInterface();
		
		ta_networks 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );

		TorrentManager tm = plugin_interface.getTorrentManager();
		
		ta_mixstate = tm.getPluginAttribute( "mixstate" );
		
		ParameterListener	listener = 
			new ParameterListener() 
			{	
				public void 
				parameterChanged(
					Parameter param )
				{
					synchronized( I2PHelperNetworkMixer.this ){
						
						enabled				= enable_param.getValue();
						incomplete_limit	= incomp_param.getValue();
						complete_limit		= comp_param.getValue();
					}
					
					parametersChanged();
				}
			};
			
		listener.parameterChanged( null );
		
		enable_param.addListener( listener );
		incomp_param.addListener( listener );
		comp_param.addListener( listener );
		
		plugin_interface.getDownloadManager().addListener( this, false );
		
		Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
		
		for ( Download download: downloads ){
			
			int	existing_state = download.getIntAttribute( ta_mixstate );
			
			if ( existing_state == MS_NONE ){
				
				if ( PluginCoreUtils.unwrap( download ).getDownloadState().isNetworkEnabled( AENetworkClassifier.AT_I2P )){
					
					download.setIntAttribute( ta_mixstate, MS_MANUAL );
					
					existing_state = MS_MANUAL;
				}
			}
			
			if ( existing_state != MS_MANUAL ){
			
				download.addAttributeListener( this, ta_networks, DownloadAttributeListener.WRITTEN );
			}
		}
		
		recheck_timer = 
			SimpleTimer.addPeriodicEvent(
				"i2pmixrecheck",
				RECHECK_PERIOD,
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event ) 
					{
						checkStuff();
					}
				});
	}
		
	private void
	parametersChanged()
	{
		if ( enabled ){
			
			checkStuff();
			
		}else{
			
			Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
			
			for ( Download download: downloads ){
				
				if ( download.getIntAttribute( ta_mixstate ) == MS_ACTIVE ){
					
					setNetworkState( download, false );
				}
			}
		}
	}
	
	public void
	downloadAdded(
		Download	download )
	{
		int	existing_state = download.getIntAttribute( ta_mixstate );

		if ( existing_state == MS_NONE ){
			
			if ( PluginCoreUtils.unwrap( download ).getDownloadState().isNetworkEnabled( AENetworkClassifier.AT_I2P )){
				
				download.setIntAttribute( ta_mixstate, MS_MANUAL );
				
				existing_state = MS_MANUAL;
			}
		}
		
		if ( existing_state != MS_MANUAL ){
			
			download.addAttributeListener( this, ta_networks, DownloadAttributeListener.WRITTEN );
			
			if ( enabled ){
				
				checkStuff();
			}
		}
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		if ( enabled ){
			
			checkStuff();
		}
	}
	
	private void
	checkStuff()
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			if ( check_pending != null ){
				
				return;
			}
			
			long now = SystemTime.getMonotonousTime();
			
			long time_since_last_check = now - last_check;
			
			if ( time_since_last_check < MIN_CHECK_PERIOD ){
				
				check_pending = 
					SimpleTimer.addEvent(
						"i2pmixer",
						SystemTime.getCurrentTime() + ( MIN_CHECK_PERIOD - time_since_last_check ),
						new TimerEventPerformer() 
						{
							public void 
							perform(
								TimerEvent event ) 
							{
								synchronized( I2PHelperNetworkMixer.this ){
									
									check_pending = null;
								}
								
								checkStuff();
							}
						});
				
				return;
			}
			
			last_check = now;
		}
		
		dispatcher.dispatch(
			new AERunnable()
			{
				public void 
				runSupport() 
				{
					synchronized( I2PHelperNetworkMixer.this ){
						
						if ( check_active || destroyed ){
							
							return;
						}
						
						check_active = true;
					}
					
					try{
						checkStuffSupport();
						
					}finally{
						
						synchronized( I2PHelperNetworkMixer.this ){
							
							check_active = false;
						}
					}
				}
			});
	}
	
	private Comparator<Download> download_comparator =
			new Comparator<Download>() 
			{
				public int 
				compare(
					Download 	d1, 
					Download 	d2 ) 
				{
					DownloadScrapeResult s1 = d1.getAggregatedScrapeResult();
					DownloadScrapeResult s2 = d2.getAggregatedScrapeResult();
					
					if ( s1 == s2 ){
						
						return( 0 );
						
					}else if ( s1 == null ){
						
						return( 1 );
	
					}else if ( s2 == null ){
						
						return( -1 );
						
					}else{
						
						return( s2.getSeedCount() - s1.getSeedCount());
					}
				}
			};
			
	private void
	checkStuffSupport()
	{
		DownloadManager dm = plugin_interface.getDownloadManager();
		
		Download[]	downloads = dm.getDownloads();
		
		List<Download>	complete_downloads		= new ArrayList<Download>( downloads.length );
		List<Download>	incomplete_downloads 	= new ArrayList<Download>( downloads.length );
		
		for ( Download download: downloads ){
			
			if ( 	download.getFlag( Download.FLAG_LOW_NOISE ) || 
					download.getFlag( Download.FLAG_METADATA_DOWNLOAD )){
				
				continue;
			}
			
			Torrent torrent = download.getTorrent();
			
			if ( torrent == null ){
				
				continue;
			}
			
			if ( TorrentUtils.isReallyPrivate( PluginCoreUtils.unwrap( torrent ))){
				
				continue;
			}
			
			if ( !PluginCoreUtils.unwrap( download ).getDownloadState().isNetworkEnabled( AENetworkClassifier.AT_PUBLIC )){
				
				continue;
			}
			
			int	existing_state = download.getIntAttribute( ta_mixstate );
			
			if ( existing_state == MS_CHANGING || existing_state == MS_MANUAL ){
				
				continue;
			}
			
			int download_state = download.getState();
		
			if ( 	download_state == Download.ST_ERROR || 
					download_state == Download.ST_STOPPING || 
					download_state == Download.ST_STOPPED ){
				
				continue;
			}
			
			boolean	complete = download.isComplete();
			
			if ( complete ){
				
				complete_downloads.add( download );
				
			}else{
				
				incomplete_downloads.add( download );
			}
		}
		
		applyRules( complete_downloads, complete_limit );
		applyRules( incomplete_downloads, incomplete_limit );
	}
	
	private void
	applyRules(
		List<Download>		downloads,
		int					active_limit )
	{	
		if ( active_limit <= 0 ){
			
			active_limit = Integer.MAX_VALUE;
		}
		
		Collections.sort( downloads, download_comparator );
				
			// downloads are sorted highest priority first
		
		ArrayList<Download>		to_activate 	= new ArrayList<Download>( downloads.size());

		int	failed_to_deactivate = 0;
		
		for ( int i=0;i<downloads.size();i++){
			
			Download download = downloads.get( i );
			
			int	existing_state = download.getIntAttribute( ta_mixstate );

			if ( existing_state == MS_ACTIVE ){
				
				if ( i < active_limit ){
					
					// already good
					
				}else{
					
					Long	activate_time = (Long)download.getUserData( I2PHelperNetworkMixer.class );
					
					if ( 	activate_time == null ||	// shouldn't happen but just in case
							SystemTime.getMonotonousTime() - activate_time >= MIN_ACTIVE_PERIOD ){
						
						plugin.log( "Netmix: deactivating " + download.getName());
						
						setNetworkState( download, false );
						
					}else{
						
						failed_to_deactivate++;
					}
				}
			}else{
				
				if ( i < active_limit ){
					
					to_activate.add( download );
				}
			}
		}
		
			// if we failed to deactivate N downloads then that means we can't yet activate N
			// new ones
		
		int	num_to_activate = to_activate.size() - failed_to_deactivate;
		
		for ( int i=0;i<num_to_activate;i++){
			
			Download download = to_activate.get(i);
			
			plugin.log( "Netmix: activating " + download.getName());
			
			setNetworkState( download, true );
		}
	}
	
	protected void
	checkMixState(
		Download		download )
	{
		if ( !enabled ){
			
			return;
		}
		
		int download_state = download.getState();
		
		if ( 	download_state == Download.ST_ERROR || 
				download_state == Download.ST_STOPPING || 
				download_state == Download.ST_STOPPED ){
			
			return;
		}
		
		int	existing_state = download.getIntAttribute( ta_mixstate );

		if ( existing_state == MS_NONE ){
			
			if ( !PluginCoreUtils.unwrap( download ).getDownloadState().isNetworkEnabled( AENetworkClassifier.AT_I2P )){

				plugin.log( "Netmix: activating " + download.getName() + " on demand" );

				setNetworkState( download, true );
			}
		}
	}
	
	public void 
	attributeEventOccurred(
		Download 			download, 
		TorrentAttribute 	attribute, 
		int 				event_type)
	{
		if ( attribute != ta_networks ){
			
			return;
		}
		
		int	existing_state = download.getIntAttribute( ta_mixstate );
		
		if ( existing_state == MS_CHANGING || existing_state == MS_MANUAL ){
			
			return;
		}
		
			// user is manually configuring networks, don't touch from now on
		
		download.setIntAttribute( ta_mixstate, MS_MANUAL );
	}
	
	private void
	setNetworkState(
		Download		download,
		boolean			enabled )
	{
		int	existing_state = download.getIntAttribute( ta_mixstate );
		
		if ( existing_state == MS_MANUAL ){
			
			return;
		}
		
		try{
			download.setIntAttribute( ta_mixstate, MS_CHANGING );
			
			PluginCoreUtils.unwrap( download ).getDownloadState().setNetworkEnabled( AENetworkClassifier.AT_I2P, enabled );
			
			if ( enabled ){
				
				download.setUserData( I2PHelperNetworkMixer.class, SystemTime.getMonotonousTime());
				
			}else{
				
				download.setUserData( I2PHelperNetworkMixer.class, null );
			}
		}finally{
			
			download.setIntAttribute( ta_mixstate, enabled?MS_ACTIVE:MS_NONE );
		}
	}
	
	protected void
	destroy()
	{
		synchronized( this ){
			
			destroyed = true;
			
			if ( check_pending != null ){
				
				check_pending.cancel();
				
				check_pending = null;
			}
		}
		
		if ( recheck_timer != null ){
			
			recheck_timer.cancel();
			
			recheck_timer = null;
		}
		
		plugin_interface.getDownloadManager().removeListener( this );
		
		DownloadManager dm = plugin_interface.getDownloadManager();
				
		for ( Download download: dm.getDownloads()){
			
			download.removeAttributeListener( this, ta_networks, DownloadAttributeListener.WRITTEN );
		}
	}
}
