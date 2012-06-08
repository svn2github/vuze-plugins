/*
 * Created on Jun 8, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.azureus.plugins.highchartsstats;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Properties;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.download.DownloadManagerStats;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.webplugin.WebPlugin;

import com.aelitis.azureus.core.pairing.PairingManager;
import com.aelitis.azureus.core.pairing.PairingManagerFactory;
import com.azureus.plugins.highchartsstats.swt.HSPSWT;

public class 
HighchartsStatsPlugin 
	extends WebPlugin
	implements UnloadablePlugin
{
    public static final int DEFAULT_PORT    = 9092;

    private static Properties defaults = new Properties();

    static{

        defaults.put( WebPlugin.PR_DISABLABLE, new Boolean( true ));
        defaults.put( WebPlugin.PR_ENABLE, new Boolean( true ));
        defaults.put( WebPlugin.PR_PORT, new Integer( DEFAULT_PORT ));
        defaults.put( WebPlugin.PR_ROOT_DIR, "web" );
        defaults.put( WebPlugin.PR_ENABLE_KEEP_ALIVE, new Boolean(true));
        defaults.put( WebPlugin.PR_HIDE_RESOURCE_CONFIG, new Boolean(true));
        defaults.put( WebPlugin.PR_PAIRING_SID, "highchartsstats" );
    }

    private HSPSWT	swt_ui;
    
    public
    HighchartsStatsPlugin()
    {
    	super( defaults );
    }
    
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		super.initialize( _plugin_interface );
		
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( 
				"com.azureus.plugins.highchartsstats.internat.Messages" );
				
		BasicPluginConfigModel	config = getConfigModel();
			
		config.addLabelParameter2( "highchartsstats.blank" );

		config.addHyperlinkParameter2( "highchartsstats.openui", getBaseURL());
		
		config.addLabelParameter2( "highchartsstats.blank" );
		
		plugin_interface.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							UISWTInstance	swt = (UISWTInstance)instance;
							
							swt_ui = new HSPSWT( HighchartsStatsPlugin.this, swt );
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						
					}
				});
	}
	
	public void 
	unload() 
		
		throws PluginException 
	{	
		if ( swt_ui != null ){
			
			swt_ui.destroy();
			
			swt_ui = null;
		}
	}
	
	protected String
	getBaseURL()
	{
		int port = plugin_interface.getPluginconfig().getPluginIntParameter( WebPlugin.CONFIG_PORT, CONFIG_PORT_DEFAULT );

		return( "http://127.0.0.1:" + port + "/" );
	}
	
	public String
	getLocalURL()
	{
		PairingManager pm = PairingManagerFactory.getSingleton();
		
		String	res = getBaseURL();
		
		if ( pm.isEnabled()){
			
			String ac = pm.peekAccessCode();
			
			if ( ac != null ){
				
				res += "?vuze_pairing_ac=" + ac;
			}
		}
		
		return( res );
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		try{
			URL url = request.getAbsoluteURL();
				
			String	url_path = url.getPath();
			
			if ( url_path.equals( "/" )){
				
				PrintWriter pw =new PrintWriter( new OutputStreamWriter( response.getOutputStream(), "UTF-8" ));
				
				pw.println( "<HTML>" );
				pw.println( "<HEAD><TITLE>Azureus Highcharts Stats</TITLE></HEAD>" );
				pw.println( "<BODY>" );
				pw.println( "<UL>" );
				pw.println( "<li><a href=\"charts/updown.html\">Up Down</a> ") ;
				pw.println( "</UL>" );
				pw.println( "</BODY></HTML>" );
				pw.flush();
				
				response.setContentType( "text/html; charset=UTF-8" );
				
				response.setGZIP( true );
				
				return( true );
				
			}else if ( url_path.equals( "/charts/updown_latest.dat" )){
				
				PrintWriter pw =new PrintWriter( new OutputStreamWriter( response.getOutputStream(), "UTF-8" ));
				
				DownloadManagerStats stats = plugin_interface.getDownloadManager().getStats();
				
				int send	= stats.getDataSendRate() + stats.getProtocolSendRate();
				int rec 	= stats.getDataReceiveRate() + stats.getProtocolReceiveRate();
				
				pw.println( send + "," + rec );
				
				pw.flush();
				
				response.setContentType( "text/html; charset=UTF-8" );
				
				response.setGZIP( true );
				
				return( true );
				
			}else{
				
				return( super.generateSupport(request, response));
			}
		}catch( Throwable e ){
							
			log( "Processing failed", e );
			
			throw( new IOException( "Processing failed: " + Debug.getNestedExceptionMessage( e )));
		}
	}
}
