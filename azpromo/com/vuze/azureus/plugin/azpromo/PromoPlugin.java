/**
 * Created on Sep 29, 2014
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.vuze.azureus.plugin.azpromo;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.util.FeatureUtils;

/**
 * @created Sep 29, 2014
 */
public class PromoPlugin
	implements UnloadablePlugin
{

	private static final String VIEWID = "SidebarPromo";

	private static PromoPlugin		pluginInstance;
	
	private UISWTInstance 			swtInstance;
	private List<PromoView>			views	= new ArrayList<PromoView>();
	
	private BasicPluginConfigModel 	configModel;

	private LoggerChannel 	logger;

	private PluginInterface 	pluginInterface;

	private volatile boolean	unloaded = false;
	
	public
	PromoPlugin()
	{
		pluginInstance	= this;
	}
	
	public String readStringFromUrl(String url) {
		StringBuffer sb = new StringBuffer();
		try {
			URL _url = new URL(url);
			HttpURLConnection con = (HttpURLConnection) _url.openConnection();

			InputStream is = con.getInputStream();

			byte[] buffer = new byte[256];

			int read = 0;

			while ((read = is.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, read));
			}
			con.disconnect();

		} catch (Throwable e) {

		}
		return sb.toString();
	}

	// @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	public void initialize(final PluginInterface pi) throws PluginException {
		pluginInterface = pi;

// We are usually initialized before FeatMan, so this check isn't very usefull
//		if (pi.getUtilities().getFeatureManager() != null) {
//  		boolean hasFullLicence = FeatureUtils.hasFullLicence();
//  		if (hasFullLicence) {
//  			return;
//  		}
//		}

		
		
		checkDumps();
		
		// COConfigurationManager.setParameter( "azpromo.dump.disable.plugin", false );
		
		if ( COConfigurationManager.getBooleanParameter( "azpromo.dump.disable.plugin", false )){
			
			PluginConfig pc = pluginInterface.getPluginconfig();
			
			if ( !pc.getPluginStringParameter( "plugin.info", "" ).equals( "c" )){
				
				pc.setPluginParameter( "plugin.info", "c" );
				
				logEvent( "crashed" );
			}
			
			return;
		}
		
		if ( COConfigurationManager.getBooleanParameter( "Beta Programme Enabled" )){
		
				// no ads for beta users unless testing with explicit pubid
			
			if ( pluginInterface.getPluginProperties().getProperty( "PubID", "" ) == "" ){
			
				return;
			}
		}
		
		String plugin_state = COConfigurationManager.getBooleanParameter( "Plugin.azpromo.enabled", true )?"e":"d";
			
		plugin_state += "-" + pi.getPluginVersion();
		
		PluginConfig pc = pluginInterface.getPluginconfig();
			
		if ( !pc.getPluginStringParameter( "plugin.info", "" ).equals( plugin_state )){
				
			pc.setPluginParameter( "plugin.info", plugin_state );
		}
		
		UIManager uiManager = pluginInterface.getUIManager();

		logger = pi.getLogger().getTimeStampedChannel(VIEWID);

		configModel = uiManager.createBasicPluginConfigModel("ConfigView.Section."
				+ VIEWID);
		BooleanParameter paramEnabled = configModel.addBooleanParameter2("enabled",
				VIEWID + ".enabled", true);
		paramEnabled.addConfigParameterListener(new ConfigParameterListener() {
			public void configParameterChanged(ConfigParameter param) {
				UIInstance[] uiInstances = pluginInterface.getUIManager().getUIInstances();
				for (UIInstance uiInstance : uiInstances) {
					if (uiInstance instanceof UISWTInstance) {
						swtInstance = (UISWTInstance) uiInstance;
						break;
					}
				}
				if (swtInstance != null) {
					boolean enabled = pluginInterface.getPluginconfig().getPluginBooleanParameter(
							"enabled");
					if (enabled) {
						swtInstance.addView(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID,
								PromoView.class, null);
					} else {
						swtInstance.removeViews(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID);

						PromoPlugin.logEvent("goaway");
					}
				}
			}
		});

		boolean enabled = pluginInterface.getPluginconfig().getPluginBooleanParameter(
				"enabled");
		if (enabled) {

			// Get notified when the UI is attached
			pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
				public void UIAttached(UIInstance instance) {
					if (instance instanceof UISWTInstance && !unloaded ) {
						swtInstance = ((UISWTInstance) instance);
						swtInstance.addView(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID,
								PromoView.class, null);
					}
				}

				public void UIDetached(UIInstance instance) {
					swtInstance = null;
				}
			});

			pluginInterface.addListener(new PluginListener() {
				public void initializationComplete() {
					if (pluginInterface == null) {
						return;
					}

					FeatureManager fm = pluginInterface.getUtilities().getFeatureManager();

					FeatureManager.FeatureManagerListener fml = new FeatureManager.FeatureManagerListener() {

						public void licenceRemoved(Licence licence) {
							checkLicence();
						}

						public void licenceChanged(Licence licence) {
							checkLicence();
						}

						public void licenceAdded(Licence licence) {
							checkLicence();
						}
					};

					fm.addListener(fml);

					checkLicence();
				}

				public void closedownInitiated() {
				}

				public void closedownComplete() {
				}
			});

		}

	}

	protected void checkLicence() {
		boolean hasFullLicence = FeatureUtils.hasFullLicence();

		if (hasFullLicence) {
			pluginInterface.getPluginconfig().setPluginParameter("enabled", false);
		}
	}

	// @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	public void unload() throws PluginException {
		
		unloaded = true;
		
		if (swtInstance != null){
			
			swtInstance.removeViews(UISWTInstance.VIEW_SIDEBAR_AREA, VIEWID);
			
			swtInstance = null;
		}
		
		// shouldn't need to track these views once bug in core that was preventing views from being
		// closed on 'removeViews' is rolled out... (5701+)
		
		List<PromoView>	to_close = new ArrayList<PromoView>();
		
		synchronized( views ){
			
			to_close.addAll( views );
			
			views.clear();
		}
		
		for ( PromoView view: to_close ){
			
			try{
				view.destroy();
				
			}catch( Throwable e ){
				
			}
		}
		if (configModel != null ){
			configModel.destroy();
			configModel = null;
		}
		
		logger = null;
		
		pluginInterface = null;
	}

	public static void logEvent(String event) {
		PlatformManager pm = PlatformManagerFactory.getPlatformManager();
		if (pm == null) {
			return;
		}
		Object[] params = new Object[] {
			"locale",
			MessageText.getCurrentLocale().toString(),
			"event-type",
			event
		};
		PlatformMessage message = new PlatformMessage("AZMSG", "PromoPlugin",
				"log", params, 1000);
		PlatformMessenger.pushMessageNow(message, null);
	}

	
	private static void
	checkDumps()
	{
		if ( !Constants.isWindows ){
			
			return;
		}
		
		try{
			List<File>	fdirs_to_check = new ArrayList<File>();
			
			fdirs_to_check.add( new File( SystemProperties.getApplicationPath()));
			
			try{
				File temp_file = File.createTempFile( "AZU", "tmp" );
				
				fdirs_to_check.add( temp_file.getParentFile());
				
				temp_file.delete();
				
			}catch( Throwable e ){
				
			}
			
			File	most_recent_dump 	= null;
			long	most_recent_time	= 0;

			for ( File dir: fdirs_to_check ){
			
				if ( dir.canRead()){
					
					File[]	files = dir.listFiles(
							new FilenameFilter() {
								
								public boolean 
								accept(
									File dir, 
									String name) 
								{
									return( name.startsWith( "hs_err_pid" ) && name.endsWith( ".log" ));
								}
							});
					
					if ( files != null ){
						
						long	now = SystemTime.getCurrentTime();
						
						long	one_week_ago = now - 7*24*60*60*1000;
						
						for (int i=0;i<files.length;i++){
							
							File	f = files[i];
																						
							long	last_mod = f.lastModified();
							
							if ( last_mod > most_recent_time && last_mod > one_week_ago){
								
								most_recent_dump 	= f;
								most_recent_time	= last_mod;
							}
						}
					}
				}
			}
		
			if ( most_recent_dump!= null ){
				
				long	last_done = COConfigurationManager.getLongParameter( "azpromo.dump.lasttime", 0 ); 
				
				if ( last_done < most_recent_time ){
					
					COConfigurationManager.setParameter( "azpromo.dump.lasttime", most_recent_time );
					
					analyseDump( most_recent_dump );
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected static void
	analyseDump(
		File	file )
	{
		try{
			LineNumberReader lnr = new LineNumberReader( new FileReader( file ));
			
			try{
				boolean	av_excep		= false;
				boolean	swt_excep		= false;
				boolean	browser_excep	= false;
							
				while( true ){
					
					String	line = lnr.readLine();
					
					if ( line == null ){
						
						break;
					}
					
					line = line.toUpperCase(Locale.US);
					
					if (line.indexOf( "EXCEPTION_ACCESS_VIOLATION") != -1 ){
						
						av_excep	= true;
						
					}else if ( line.startsWith( "# C  [SWT-WIN32")){
						
							// dll has same name for 32 + 64 bit VMs
						
						swt_excep 	= true;
						
					}else if ( line.contains( "WEBSITE.PROCESSURLACTION")){
						
						browser_excep = true;
					}
				}
				
				if ( av_excep && swt_excep && browser_excep ){
					
					Debug.out( "Hit SWT Browser bug" );
					
					COConfigurationManager.setParameter( "azpromo.dump.disable.plugin", true );
					
					COConfigurationManager.save();
				}
			}finally{
				
				lnr.close();
			}
		}catch( Throwable e){
			
			Debug.printStackTrace( e );
		}
	}
	
	protected PluginInterface
	getPluginInterface()
	{
		return( pluginInterface );
	}
	
	protected UISWTInstance
	getSWTInstance()
	{
		return( swtInstance );
	}
	
	protected void
	viewAdded(
		PromoView		view )
	{
		synchronized( views ){
			
			views.add( view );
		}
	}
	
	protected void
	viewRemoved(
		PromoView		view )
	{
		synchronized( views ){
			
			views.remove( view );
		}
	}
	protected void log(String s) {
		if (logger != null) {
			logger.log(s);
		}
	}
	
	protected static PromoPlugin
	getPlugin()
	{
		return( pluginInstance );
	}

}
