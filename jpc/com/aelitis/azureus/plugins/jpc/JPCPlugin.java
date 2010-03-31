/*
 * Created on 07-Feb-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.jpc;

import java.util.Timer;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;

import com.aelitis.azureus.plugins.jpc.cache.impl.JPCCacheManagerImpl;
import com.aelitis.azureus.plugins.jpc.peer.PeerController;
import com.aelitis.azureus.plugins.jpc.peer.impl.PeerControllerImpl;



/**
 * Joltid Peer Cache (JPC) plugin for ISP BitTorrent data caching.
 */
public class 
JPCPlugin 
	implements Plugin
{
  
  public static final boolean USE_TEST_CACHE = false;
  
  public static final int LOG_PUBLIC = 1;
  public static final int LOG_DEBUG  = 2;
  
  public static final String	PLUGIN_VERSION			= "1.2";
  public static final String	PLUGIN_NAME				= "JPC";
  public static final String	PLUGIN_LANG_RESOURCE 	= "com.aelitis.azureus.plugins.jpc.internat.Messages";
  
  private PluginInterface	plugin_interface;
  private PeerController peer_controller;
  private LoggerChannel logger;
  private Timer plugin_timer;
  
  private BooleanParameter enable_uploader;
  private BooleanParameter enable_downloader;
  

	public void
	initialize(
		PluginInterface		_pi )
	{
		plugin_interface	= _pi;
    
		plugin_timer = new Timer( true );

		logger = plugin_interface.getLogger().getChannel( "JPC" );

		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	PLUGIN_VERSION );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );
		
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( PLUGIN_LANG_RESOURCE );
    
		BasicPluginConfigModel  config = plugin_interface.getUIManager().createBasicPluginConfigModel( "Plugins", "azjpc.name" );
    
		enable_uploader   = config.addBooleanParameter2( "azjpc.enableuploader", "azjpc.enableuploader", true );
		enable_downloader = config.addBooleanParameter2( "azjpc.enabledownloader", "azjpc.enabledownloader", true );
    
		final BasicPluginViewModel model = plugin_interface.getUIManager().createBasicPluginViewModel( "JPC" );
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
    
		model.getStatus().setText( "Loading..." );

		logger.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});
    
		if( USE_TEST_CACHE ) {
          log( "**** JPC TEST CACHE CONFIGURED ****", LOG_DEBUG );
        }
    
		enable_downloader.addListener( new ParameterListener() {
		  public void parameterChanged( Parameter param ) {
		    activate( model );
		  }
		});
    
		enable_uploader.addListener( new ParameterListener() {
		  public void parameterChanged( Parameter param ) {
		    activate( model );
		  }
		});
      
	    activate( model );
	}
  
  
  
    private void activate( BasicPluginViewModel model ) {
      if( !isCacheDownloadEnabled() ) {
        log( "Cache DOWNLOAD usage disabled in config.", JPCPlugin.LOG_PUBLIC );

        if( peer_controller != null ) {
          peer_controller.dropDownloadCacheSession();
        }
      }
      
      if( !isCacheUploadEnabled() ) {
        log( "Cache UPLOAD usage disabled in config.", JPCPlugin.LOG_PUBLIC );
      }
      
      if( isCacheDownloadEnabled() || isCacheUploadEnabled() ) {
        model.getStatus().setText( "Running" );
        
        if( peer_controller == null ) {
          peer_controller = new PeerControllerImpl( JPCPlugin.this, new JPCCacheManagerImpl( JPCPlugin.this ) );
          peer_controller.startPeerProcessing();
        }

        if( isCacheDownloadEnabled() ) {
          peer_controller.establishDownloadCacheSession();
        }
      }
      else {
        model.getStatus().setText( "Stopped" );
        
        if( peer_controller != null ) {
          peer_controller.stopPeerProcessing();
          peer_controller = null;
        }
      }
    }
  
  
  

	public PluginInterface 
	getPluginInterface() 
	{
	    return plugin_interface;
	}
    
  
    public Timer getPluginTimer() {   return plugin_timer;  }
  
  
	public void log( String txt, int level ) { 
      logger.log( txt );
	}
	
	public void 
	log(
		String		txt,
		Throwable 	e,
		int			level )
	{
		logger.log( txt, e );
	}
	
	
	
  public boolean isCacheUploadEnabled() {  return enable_uploader.getValue();  }
  public boolean isCacheDownloadEnabled() {  return enable_downloader.getValue();  }
  
  
  
  public void updateVersionCheckString( String info ) {
  	plugin_interface.getPluginconfig().setPluginParameter( "plugin.info", info );
  	
  	try{
  		plugin_interface.getPluginconfig().save();
  	}
  	catch( Exception e ) {  e.printStackTrace();  }  	
  }
  
  
  

  /*
	public static void
	main(
		String[]	args )
	{
		AzureusCore	core = AzureusCoreFactory.create();
		
		PluginInitializer.getSingleton(core,null);
		
		PluginInterface	pi = PluginInitializer.getDefaultInterface();
		
		JPCPlugin plugin = new JPCPlugin();
		plugin.initialize( pi );		
		
		try{
			Thread.sleep(100000000);
		}catch( Throwable e ){
			
		}
	}
	*/
}
