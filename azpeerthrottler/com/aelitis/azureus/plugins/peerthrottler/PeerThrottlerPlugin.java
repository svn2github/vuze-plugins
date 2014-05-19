/*
 * Created on Jan 17, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.peerthrottler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerStats;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerStats;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerStatsImpl;


public class 
PeerThrottlerPlugin 
	implements Plugin
{
	private PluginInterface		plugin_interface;
	private LoggerChannel		logger;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException 
	{
		plugin_interface	= _plugin_interface;
		
		logger				= plugin_interface.getLogger().getTimeStampedChannel( "PeerThrottler" ); 
		
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		loc_utils.integrateLocalisedMessageBundle( "com.aelitis.azureus.plugins.peerthrottler.internat.Messages" );

		String plugin_name = loc_utils.getLocalisedMessageText( "azpeerthrottler.name" );

		UIManager	ui_manager	= plugin_interface.getUIManager();
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( plugin_name );

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
		
		BasicPluginConfigModel config_model = 
			ui_manager.createBasicPluginConfigModel( "azpeerthrottler.name" );

		view_model.setConfigSectionID( "azpeerthrottler.name" );
		
		final BooleanParameter 	pt_enable			= config_model.addBooleanParameter2( "azpeerthrottler.enable", "azpeerthrottler.enable", true );
		final IntParameter 		pt_check_secs		= config_model.addIntParameter2( "azpeerthrottler.check_secs", "azpeerthrottler.check_secs", 30, 1, 3600 );
		final StringParameter 	pt_min_dl_rate		= config_model.addStringParameter2( "azpeerthrottler.min_dl_rate", "azpeerthrottler.min_dl_rate", "0.5" );
		final StringParameter 	pt_limit_ratio		= config_model.addStringParameter2( "azpeerthrottler.limit_ratio", "azpeerthrottler.limit_ratio", "2.0" );
		
		ParameterListener float_checker = 
			new ParameterListener()
			{
				private Map<Object, String>	last_valid = new HashMap<Object,String>();
				
				public void 
				parameterChanged(
					Parameter param ) 
				{
					StringParameter sp = (StringParameter)param;
					
					String str = sp.getValue();
					
					try{
						Float.parseFloat( str );
						
						last_valid.put( param, str );
												
					}catch( Throwable e ){
						
						String last = last_valid.get( param );
						
						if ( last == null ){
							
							last = "0.0";
						}
						
						if ( !last.equals( str )){
						
							sp.setValue( last );	
						}
					}
					
					log( sp );
				};
			};
			
		pt_min_dl_rate.addListener( float_checker );
		pt_limit_ratio.addListener( float_checker );
			
		log( pt_min_dl_rate );
		log( pt_limit_ratio );
		
		pt_enable.addEnabledOnSelection( pt_check_secs );
		pt_enable.addEnabledOnSelection( pt_min_dl_rate );
		pt_enable.addEnabledOnSelection( pt_limit_ratio );
				
		plugin_interface.getUtilities().createTimer( "processor", true ).addPeriodicEvent(
			1000,
			new UTTimerEventPerformer()
			{
				private boolean	was_enabled;
				
				private int	tick_count		= 0;
				
				public void 
				perform(
					UTTimerEvent event )
				{
					boolean	is_enabled = pt_enable.getValue();
					
					if ( tick_count == 0 || is_enabled != was_enabled ){
						
						logger.log( "Enabled=" + is_enabled);

						was_enabled = is_enabled;
					}
					
					if ( !is_enabled ){
						
						return;
					}
					
					if ( tick_count % pt_check_secs.getValue() == 0 ){
						
						float min_dl_rate 	= 0.5f;
						float limit_ratio	= 2.0f;
						
						try{
							min_dl_rate = Float.parseFloat( pt_min_dl_rate.getValue());
							
						}catch( Throwable e ){
							
							logger.log( "Min download rate invalid: " + pt_min_dl_rate.getValue());
						}
						
						try{
							limit_ratio = Float.parseFloat( pt_limit_ratio.getValue());
							
						}catch( Throwable e ){
							
							logger.log( "Limit ratio invalid: " + pt_limit_ratio.getValue());
						}

						Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
						
						for ( Download download: downloads ){
						
							if ( download.getState() == Download.ST_DOWNLOADING ){
									
								PeerManager pm = download.getPeerManager();
								
								if ( pm == null ){
									
									continue;
								}
								
								Peer[] peers = pm.getPeers();
								
								for ( Peer peer: peers ){
									
									PeerStats stats = peer.getStats();
									
									if ( stats instanceof PeerStatsImpl ){
										
										PEPeerStats pe_stats = ((PeerStatsImpl)stats).getDelegate();
								
										int	upload_limit = 0;
										
										if ( peer.isInteresting()){
											
											long receive_rate = pe_stats.getDataReceiveRate();
											
											if ( receive_rate >= min_dl_rate ){
												
												upload_limit = (int)( receive_rate * limit_ratio );
											}
										}
										
										int existing = pe_stats.getUploadRateLimitBytesPerSecond();
										
										if ( existing != upload_limit ){
											
											logger.log( "Setting upload limit to " + getRateString( upload_limit ) + " for " + peer.getIp() + " (existing limit was " + getRateString( existing ) + ")" );
											
											pe_stats.setUploadRateLimitBytesPerSecond( upload_limit );
										}
									}
									
								}
							}
						}
					}
					
					tick_count++;

				}
			});
	}
	
	private void
	log(
		StringParameter	sp )
	{
		logger.log(  "'" + sp.getLabelText() + "' set to " + sp.getValue());

	}
	private String
	getRateString(
		int	rate )
	{
		if ( rate == -1 ){
			
			return( MessageText.getString("MyTorrents.items.UpSpeedLimit.disabled"));
			
		}else if ( rate == 0 ){
			
			return( Constants.INFINITY_STRING );
			
		}else{
			
			return( DisplayFormatters.formatByteCountToKiBEtcPerSec(rate));
		}
	}
}
