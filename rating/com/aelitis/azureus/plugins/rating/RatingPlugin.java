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
package com.aelitis.azureus.plugins.rating;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;

import com.aelitis.azureus.plugins.rating.updater.CompletionListener;
import com.aelitis.azureus.plugins.rating.updater.RatingData;
import com.aelitis.azureus.plugins.rating.updater.RatingResults;
import com.aelitis.azureus.plugins.rating.updater.RatingsUpdater;

public class RatingPlugin implements UnloadablePlugin, PluginListener {
  
  private static final Object	DDBS_KEY	= new Object();

  static{
	  try{
		  
		  	// fix missing default for fancy menu causing settings not to be remembered
		  
		  COConfigurationManager.setBooleanDefault( "Library.showFancyMenu", true );
		  
		  	// switch default for chat sound notifications
		  
		  if ( !COConfigurationManager.doesParameterNonDefaultExist( "azbuddy.chat.notif.sound.enable" )){
			  
			  COConfigurationManager.setParameter( "azbuddy.chat.notif.sound.enable", false );
		  }
	  }catch( Throwable e ){  
	  }
  }
  
  
  private PluginInterface pluginInterface;
  
  private TorrentAttribute ta_networks;

  private LoggerChannel     log;
  
  private UIManagerListener			ui_listener;
  
  private BasicPluginConfigModel		config_model;
  
  private String 			nick;
  private RatingsUpdater 	updater;
  
  private RatingUI			ui;
   
  public void 
  initialize(
		PluginInterface _pluginInterface) 
  {
    this.pluginInterface = _pluginInterface;
    
	ta_networks 	= pluginInterface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );

    log = pluginInterface.getLogger().getChannel("Rating Plugin");
    
    addPluginConfig();

    ui_listener = 
    	new UIManagerListener()
		{
			public void
			UIAttached(
				UIInstance		instance )
			{
				if ( instance.getUIType() == UIInstance.UIT_SWT ){
										
					try{
						ui = (RatingUI)Class.forName( "com.aelitis.azureus.plugins.rating.ui.RatingSWTUI" ).
								getConstructor( RatingPlugin.class, UIInstance.class ).
									newInstance( RatingPlugin.this, instance );
						
					}catch( ClassNotFoundException e ){
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
			
			public void
			UIDetached(
				UIInstance		instance )
			{
				
			}
		};
	
	updater = new RatingsUpdater(this);
	    
	pluginInterface.addListener(this);
	
    pluginInterface.getUIManager().addUIListener( ui_listener );
  }
  
  	private static List<DistributedDatabase> EMPTY_DDBS = Collections.emptyList();
  	
  	public List<DistributedDatabase>
  	getDDBs(
  		Download		download )
	{	
  		if ( download.getTorrent() != null && !download.getFlag( Download.FLAG_METADATA_DOWNLOAD )){
  			
  			List<DistributedDatabase> result = (List<DistributedDatabase>)download.getUserData( DDBS_KEY);
  			
  			if ( result == null ){
  			
  				result = download.getDistributedDatabases();
  				
  				if ( result != null ){
  					
	  				download.setUserData( DDBS_KEY, result );
	  				
	  					// handle change from no network to some network
	  				
	  				if ( result.size() == 0 ){
	  					
		  				download.addAttributeListener(
		  					new DownloadAttributeListener(){
								
								public void 
								attributeEventOccurred(
									Download 			download,
									TorrentAttribute 	attribute, 
									int 				event_type) 
								{
									download.removeAttributeListener( this, ta_networks, DownloadAttributeListener.WRITTEN );
									
									download.setUserData( DDBS_KEY, null );
								}
							},
							ta_networks,
							DownloadAttributeListener.WRITTEN );
	  				}
  				}		
  			}
  			
  			return( result );
  		}
  		
  		return( EMPTY_DDBS );
	}
  
  public boolean
  isRatingEnabled(
	Download		download )
  {
	  if ( download.getTorrent() != null && !download.getFlag( Download.FLAG_METADATA_DOWNLOAD )){

		  String[]	networks = download.getListAttribute( ta_networks );

		  if ( networks != null ){

			  for ( int i=0; i<networks.length; i++ ){

				  if ( networks[i].equalsIgnoreCase( "Public" )){

					 return( true );
				  }
			  }
			  
			  return( getDDBs( download ).size() > 0 );
		  }
		  
		  return( true );
		 
	  }else{
		  
		  return( false );
	  }
  }
  
  public void closedownComplete() {
  }
  
  public void closedownInitiated() {
  }
  
  public void initializationComplete() {
    updater.initialize();        
  }
  
  
  private void addPluginConfig()  {
	  
	config_model = pluginInterface.getUIManager().createBasicPluginConfigModel( "rating.config.title" );
	
	final StringParameter nick_param = config_model.addStringParameter2( "nick","rating.config.nick","");
	
	nick = nick_param.getValue().trim();
	
	if ( nick.length() == 0 ){
		
		nick = "Anonymous";
	}
	
	nick_param.addListener(
		new ParameterListener()
		{
			public void 
			parameterChanged(
				Parameter param )
			{
				String val = nick_param.getValue().trim();
				
				if ( val.length() == 0 ){
					
					val = "Anonymous";
				}
				
				nick = val;
			}
		});
  }
  
  public void 
  unload() 
  
  	throws PluginException 
  {
		if ( updater != null ){
			
			updater.destroy();
		}
		
		if ( config_model != null ){
			
			config_model.destroy();
			
			config_model = null;
		}
		
		if ( ui != null ){
			
			ui.destroy();
			
			ui = null;
		}
		
		if ( pluginInterface != null ){
			
			pluginInterface.getUIManager().removeUIListener( ui_listener );
			
			pluginInterface.removeListener( this );
		}
  }
  
  
  public PluginInterface getPluginInterface() {
    return this.pluginInterface;
  }
  
  public String getNick() {
    return nick;
  }
  
  public void logInfo(String text) {
    log.log(LoggerChannel.LT_INFORMATION,text);
  }
  
  public void logError(String text) {
    log.log(LoggerChannel.LT_ERROR,text);
  }
  
  public void logError(String text, Throwable t) {
    log.log(text, t);
  }

  public RatingsUpdater getUpdater() {
    return updater;
  }
  
  	// IPC methods
  
  public Map
  lookupRatingByHash(
	 byte[]		hash )
  {
	  return( lookupRatingByHash( null, hash ));
  }
  
  public Map
  lookupRatingByHash(
	String[]		networks,
	byte[]			hash )
  {
	  final RatingResults[]	f_result = { null };
	  final AESemaphore	sem = new AESemaphore( "ratings_waiter" );
	  
	  if ( networks == null ){
		  updater.readRating(
			hash,
			new CompletionListener()
			{
				public void 
				operationComplete(
					RatingResults ratings ) 
				{
					try{
						f_result[0] = ratings;
						
					}finally{
						
						sem.release();
					}
				}
			});
	  }else{
		  List<DistributedDatabase> ddbs = pluginInterface.getUtilities().getDistributedDatabases( networks );
		  
		  updater.readRating(
			ddbs,
			hash,
			new CompletionListener()
			{
				public void 
				operationComplete(
					RatingResults ratings ) 
				{
					try{
						f_result[0] = ratings;
						
					}finally{
						
						sem.release();
					}
				}
			}); 
	  }
	  
	  sem.reserve();
	  
	  RatingResults result = f_result[0];
	  
	  if ( result == null || result.getNbRatings() == 0 ){
		  
		  return( null );
	  }
	  
	  Map map = new HashMap();
	  
	  List<RatingData> ratings = result.getRatings();
	  
	  List<Map> list = new ArrayList<Map>();
	  
	  map.put( "ratings", list );
	  
	  for ( RatingData rd: ratings ){
		  
		  Map r_m = new HashMap();
		  
		  list.add( r_m );
		  
		  r_m.put( "nick", rd.getNick());
		  r_m.put( "score", rd.getScore());
		  r_m.put( "comment", rd.getComment());
	  }
	  
	  return( map );
  }
}
