/*
 * Created on 03-May-2005
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

package org.parg.azureus.plugins.azdhtscraper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteEncodedKeyHashMap;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;

import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;

public class 
DHTScraperPlugin 
	implements Plugin, TrackerWebPageGenerator
{
	private final static boolean	TESTING	= false;
	
	private final static int		DEFAULT_PORT = 6885;
	
	
	private PluginInterface		plugin_interface;
	private LoggerChannel		log;
	
	private DHTPlugin				dht;
	private DHTTrackerPlugin		dht_tracker;
		
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel( "DHT Scraper" );
				
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "org.parg.azureus.plugins.azdhtscraper.internat.Messages");
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azdhtscraper.name");

		BooleanParameter enable 		= config_model.addBooleanParameter2( "azdhtscraper.enable", "azdhtscraper.enable", true );
		
		final IntParameter	port = config_model.addIntParameter2( "azdhtscraper.port", "azdhtscraper.port", DEFAULT_PORT );
		
		final BasicPluginViewModel model = 
			plugin_interface.getUIManager().createBasicPluginViewModel( "DHT Scraper" );
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );

		log.addListener(
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
		if ( !enable.getValue()){
			
			log.log( "Plugin disabled" );
			
			return;
		}
		
		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						final PluginInterface dht_pi = 
							plugin_interface.getPluginManager().getPluginInterfaceByClass(
										DHTPlugin.class );
						
						final PluginInterface dht_tracker_pi = 
							plugin_interface.getPluginManager().getPluginInterfaceByClass(
										DHTTrackerPlugin.class );
						

						if ( dht_pi != null && dht_tracker_pi != null ){
							
							dht = (DHTPlugin)dht_pi.getPlugin();
							
							Thread	t = 
								new AEThread( "DHTScraper:init" )
								{
									public void
									runSupport()
									{
										try{
								
											if ( dht.isEnabled()){

												dht_tracker = (DHTTrackerPlugin)dht_tracker_pi.getPlugin();
												
												TrackerWebContext ctx = plugin_interface.getTracker().createWebContext( port.getValue(), Tracker.PR_HTTP );
													
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
												
												ctx.addPageGenerator( DHTScraperPlugin.this );
												
												if ( TESTING ){
													
													new AEThread( "DHTScraper:test", true )
													{
														
														public void
														runSupport()
														{
															test();
														}
													}.start();
												}
											}else{
												
												log.log( "DHT unavailable" );
											}
										}catch( Throwable e ){
													
											log.log( e );
										}
									}
								};
								
							t.setDaemon( true );
							
							t.start();
						}
					}
					
					public void
					closedownInitiated()
					{	
					}
					
					public void
					closedownComplete()
					{	
					}
				});
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url	= request.getURL();
		
		System.out.println( "DHTScraper plugin: " + request.getClientAddress() + " -> " + url );
		
		log.log( request.getClientAddress() + " -> " + url );
		
		int	pos = url.indexOf('?');
		
		if ( pos == -1 ){
		
			return( false );
		}
		
		url = url.substring( pos+1 );
		
		pos = 0;
		
		byte[]	target_hash = null;
		
		while( pos < url.length()){
			
			int	p1 = url.indexOf( '&', pos );
				
			String	token;
				
			if ( p1 == -1 ){
					
				token = url.substring( pos );
					
			}else{
					
				token = url.substring( pos, p1 );
					
				pos = p1+1;
			}
			
			int	p2 = token.indexOf('=');
				
			if ( p2 != -1 ){
					
				String	lhs = token.substring( 0, p2 ).toLowerCase();
				String	rhs = URLDecoder.decode(token.substring( p2+1 ), Constants.BYTE_ENCODING );
						
				if ( lhs.equals( "info_hash" )){
					
					if ( rhs.length() == 20 ){
					
						target_hash = rhs.getBytes(Constants.BYTE_ENCODING);
						
					}else{

						target_hash = plugin_interface.getUtilities().getFormatters().base32Decode( rhs );
					}
					
					break;
				}
			}
			
			if ( p1 == -1 ){
				
				break;
			}
		}				

		if ( target_hash != null ){
		
			// System.out.println( "target = " + plugin_interface.getUtilities().getFormatters().encodeBytesToString( target_hash ));
						
			DownloadScrapeResult	sr = dht_tracker.scrape( target_hash );
			
			Map	files = new ByteEncodedKeyHashMap();

			String	str_hash = new String( target_hash,Constants.BYTE_ENCODING );
				
			Map	scrape_details 			= new TreeMap();
			
			scrape_details.put( "complete", new Long( sr.getSeedCount()));
			
			scrape_details.put( "incomplete", new Long( sr.getNonSeedCount()));

			files.put( str_hash, scrape_details );
			
			Map	root = new HashMap();
			
				// add scrape response
			
			
			root.put( "files", files );

			
				// add retry interval flags
			
			Map	flags = new HashMap();
			
			flags.put("min_request_interval", new Long(3600));
			
			root.put( "flags", flags );	

			response.getOutputStream().write( BEncoder.encode( root ));
			
			return( true );
			
		}else{
		
			return( false );
		}
	}
	
	protected void
	test()
	{
		String	base_32 = "ANSGLU3S6S4LOESS2TKIWU4MNOC3AK3P";
		String	binary	= "%A9%85%DD%E6%C2%A5%E9j%9A%FF%5C%1F%5C%A3%05%E2%FD%1B%CA%9E";

		int	count = 0;
		
		while( true ){

			count++;
			
			try{
				URL	url = 
					new URL( "http://localhost:" + DEFAULT_PORT + 
						"/dht/scrape?info_hash=" +
						(count%2==0?base_32:binary));
				
				System.out.println( "Testing URL '" + url + "'" );
				
				HttpURLConnection	con = (HttpURLConnection)url.openConnection();
				
				InputStream	is = con.getInputStream();
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				while( true ){
					
					byte[]	buffer = new byte[1024];
					
					int	len = is.read( buffer );
					
					if( len <= 0 ){
						
						break;
					}
					
					baos.write( buffer, 0, len );
				}
				
				System.out.println( "got response:" + new String( baos.toByteArray()));
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
			}finally{
				
				try{
					Thread.sleep(10*1000);
				
				}catch( Throwable e ){
				
					e.printStackTrace();
				}
			}
		}
	}
}
