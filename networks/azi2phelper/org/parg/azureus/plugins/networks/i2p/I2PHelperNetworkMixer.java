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
	
	private PluginInterface		plugin_interface;
	private TorrentAttribute 	ta_networks;
	private TorrentAttribute 	ta_mixstate;

	private volatile boolean		enabled;
	private int						incomplete_limit;
	private int						complete_limit;
	
	private static final int MIN_CHECK_PERIOD 	= 60*1000;
	private static final int RECHECK_PERIOD 	= 5*60*1000;
	
	private TimerEventPeriodic	recheck_timer;
	
	private long		last_check		= -MIN_CHECK_PERIOD;
	private TimerEvent	check_pending 	= null;
	private boolean		check_active;
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( "i2pmixer", 5*1000 );
	
	private boolean	destroyed;
	
	protected
	I2PHelperNetworkMixer(
		I2PHelperPlugin			plugin,
		final BooleanParameter	enable_param,
		final IntParameter		incomp_param,
		final IntParameter		comp_param )
	{
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
			
			download.addAttributeListener( this, ta_networks, DownloadAttributeListener.WRITTEN );
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
		download.addAttributeListener( this, ta_networks, DownloadAttributeListener.WRITTEN );
		
		if ( enabled ){
			
			checkStuff();
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
	
	private void
	checkStuffSupport()
	{
		DownloadManager dm = plugin_interface.getDownloadManager();
		
		for ( Download download: dm.getDownloads()){
			
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
			
			int	existing_state = download.getIntAttribute( ta_mixstate );
			
			if ( existing_state == MS_CHANGING || existing_state == MS_MANUAL ){
				
				return;
			}
			
			int download_state = download.getState();
		
			if ( 	download_state == Download.ST_ERROR || 
					download_state == Download.ST_STOPPING || 
					download_state == Download.ST_STOPPED ){
				
				continue;
			}
			
			System.out.println( "Checking: " + download.getName() + ", comp=" + download.isComplete());
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
