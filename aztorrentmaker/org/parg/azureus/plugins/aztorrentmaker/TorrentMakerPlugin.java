/*
 * Created on Nov 4, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.aztorrentmaker;

import java.util.*;
import java.io.File;
import java.net.URL;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentProgressListener;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.TrackersUtil;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.DirectoryParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.TextViewerWindow;
import org.gudy.azureus2.ui.swt.Utils;

public class 
TorrentMakerPlugin
	implements UnloadablePlugin
{
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		PluginInterface plugin_interface	= _plugin_interface;
				
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		LoggerChannel log	= plugin_interface.getLogger().getChannel( "TorrentMaker");
		
		final UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "aztorrentmaker.name" );
						
		final StringParameter tracker_group = config_model.addStringParameter2( "tracker_template", "!Tracker Template Name!", "" );
		
		final DirectoryParameter dir = config_model.addDirectoryParameter2( "torrent_dir", "!Parent Directory!", "" );
		
		ActionParameter create = config_model.addActionParameter2( "!Create Torrents!", "!Do It!!" );
		
		create.addListener(
			new ParameterListener() {
				
				public void 
				parameterChanged(
					Parameter param ) 
				{
					String template = tracker_group.getValue();
					
					String error_msg = null;

					final List<List<String>> trackers = TrackersUtil.getInstance().getMultiTrackers().get( template );
										
					if ( trackers == null ){
						
						error_msg =
							"Tracker template '" + template + "' not found - use the torrent creation wizard to make one:\n\n" +
								"File->New Torrent...\n" +
								"Select 'Add Multi-Tracker information to the torrent\n" +
								"Hit 'Next' and select the 'New' button to create a tracker template and name it appropriately";
						
					}else{
						
						final File parent = new File( dir.getValue());
						
						if ( !parent.exists()){
							
							error_msg = "Parent directory '" + parent + "' doesn't exist";
							
						}else{
							
							
							final TextViewerWindow viewer = new TextViewerWindow(
									"Torrent Creation Results",
									null,
									"Starting torrent creation process for '" + parent + "'\nTrackers = " + trackers+ "\n\n", false  );
							
							new AEThread2( "SOCKS test" )
							{
								public void
								run()
								{
									try{
										URL announce = new URL(trackers.get(0).get(0));
									
										for ( File file: parent.listFiles()){
											
											if ( !file.isDirectory()){
												
												continue;
											}
											
											log( "Processing '" + file.getAbsolutePath() + "'" );
											
											try{
												TOTorrentCreator creator = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength(	file, announce );
												
												creator.addListener(
													new TOTorrentProgressListener() {
														
														public void 
														reportProgress(
															int percent_complete) 
														{
															log( "\t\t" + percent_complete + "%" );
														}
														
														public void 
														reportCurrentTask(
															String task_description) 
														{
															log( "\t" + task_description );
														}
													});
												
												TOTorrent torrent = creator.create();
												
												if ( trackers.size() > 1 || trackers.get(0).size() > 1 ){
												
													TorrentUtils.listToAnnounceGroups( trackers, torrent );
												}
												
												File torrent_file = new File( file.getAbsolutePath() + ".torrent" );
												
												torrent.serialiseToBEncodedFile( torrent_file );
												
												log( "\tWrote " + torrent_file );
												
											}catch( Throwable e ){
										
												log( "\tTorrent creation failed: " + Debug.getNestedExceptionMessage( e ));
											}
										}
									}catch( Throwable e ){
										
										log( "Error: " + Debug.getNestedExceptionMessage( e ));
										
									}finally{
										
										log( "Done" );
									}
								}
								
								private void
								log(
									final String	str )
								{
									Utils.execSWTThread(
										new Runnable()
										{
											public void
											run()
											{
												viewer.append( str + "\r\n" );
											}
										});
								}
							}.start();
						}
					}
				
					if ( error_msg != null ){
						
						ui_manager.showTextMessage(
								"!Torrent Creation Results!", null, error_msg );
					}
				}
			});
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "aztorrentmaker.name" ));

		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		log.addListener(
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
						view_model.getLogArea().appendText( str + "\n" );
						view_model.getLogArea().appendText( error.toString() + "\n" );
					}
				});
	}
	
	public void
	unload()
	{
		
	}
}
