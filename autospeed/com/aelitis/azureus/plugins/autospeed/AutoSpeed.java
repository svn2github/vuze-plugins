/*
 * Created on 27 sept. 2004
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
package com.aelitis.azureus.plugins.autospeed;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.components.UITextArea;
import org.gudy.azureus2.plugins.ui.components.UITextField;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.plugins.autospeed.ping.Ping;
import com.aelitis.azureus.plugins.autospeed.ping.PingListener;
import com.aelitis.azureus.plugins.autospeed.tracert.TraceRoute;

/**
 * @author Olivier Chalouhi
 *
 */
public class AutoSpeed implements Plugin, PingListener, PluginListener, ConfigParameterListener,UTTimerEventPerformer {

	private static final int TIME_OUT = 4 * 1000;	
	
	private static final int NB_PINGS = 5;
	private static final int NB_WAIT_PINGS = 15;
	
	PluginInterface pluginInterface;
	UITextArea log;
	UITextField status;
	
	boolean enabled;
	
	UTTimer routeHostRecheck;
	
	String pingIP;
	Ping ping;
	
	//"" when not forced to anything, the IP otherwise
	String forceIP;
	//The minimum Up Speed the autospeed won't go lower than
	int minimumUp;
	//The maximum UP Speed the autospeed won't go higher than
	int maximumUp;
	//The minimum number of public IPs to byPass in the traceroute (0,1,2,3)
	int minByPass;
  
  //The UP / Down Ratio (*10) (eg: UP = 1/5 Down : 2).
  int upDownRatio;
	
	//The current number of IPs to byPass in the trace route
	//When a ping is successful, this value is reseted to minByPass
	//When a ping fails it's incremented. It helps finding the first
	//Hop that accepts pings.
	int nbByPass;
	
	int nbFailures;
	int lastPings[];
	int iPings;
	
	public void initialize(PluginInterface pluginInterface)
			throws PluginException {
		this.lastPings = new int[NB_PINGS];
		iPings = 0;
		
		this.pluginInterface = pluginInterface;
		
		enabled = pluginInterface.getPluginconfig().getPluginBooleanParameter("enable",true);
		forceIP = pluginInterface.getPluginconfig().getPluginStringParameter("forceIP","");
		minimumUp = pluginInterface.getPluginconfig().getPluginIntParameter("minimumUp",5);
		maximumUp = pluginInterface.getPluginconfig().getPluginIntParameter("maximumUp",1024);
		minByPass = pluginInterface.getPluginconfig().getPluginIntParameter("minByPass",0);
    upDownRatio = pluginInterface.getPluginconfig().getPluginIntParameter("upDownRatio",4);
		nbByPass = minByPass;
		
		pluginInterface.addListener(this);
		
		BasicPluginViewModel model = pluginInterface.getUIManager().createBasicPluginViewModel("Auto Speed");
		model.getActivity().setVisible(false);
		model.getProgress().setVisible(false);
		
		status = model.getStatus();							
		status.setVisible(true);
		
		log = model.getLogArea();
		log.setVisible(true);
		
		PluginConfigUIFactory factory = pluginInterface.getPluginConfigUIFactory();
		Parameter parameters[] = new Parameter[8];
		parameters[0] = factory.createBooleanParameter("enable","autospeed.config.enable",true);	
		parameters[0].addConfigParameterListener(this);
		parameters[1] = factory.createIntParameter("targetping","autospeed.config.targetping",150);
		parameters[2] = factory.createStringParameter("routehost","autospeed.config.routehost","www.google.com");
		parameters[3] = factory.createStringParameter("forceIP", "autospeed.config.forceIP","");
		parameters[3].addConfigParameterListener(this);
		parameters[4] = factory.createIntParameter("minimumUp","autospeed.config.minimumUp",5);
		parameters[4].addConfigParameterListener(this);
		parameters[5] = factory.createIntParameter("maximumUp","autospeed.config.maximumUp", 1024);
		parameters[5].addConfigParameterListener(this);
    int[] values = {0,1,2,3,4,5,6,7,8,9,10,15,20};
    String[] texts = new String[values.length];
    texts[0] = pluginInterface.getUtilities().getLocaleUtilities().getLocalisedMessageText("autospeed.config.dontSetDownload");
    String timesShareRatio = pluginInterface.getUtilities().getLocaleUtilities().getLocalisedMessageText("autospeed.config.timesUp");
    for(int i = 1 ; i < texts.length ; i++) {
      texts[i] = values[i] + " " + timesShareRatio;
    }
    parameters[6] = factory.createIntParameter("upDownRatio","autospeed.config.upDownRatio", 0, values,texts);
    parameters[6].addConfigParameterListener(this);
    
		parameters[7] = factory.createIntParameter("minByPass","autospeed.config.minByPass",0,new int[] {0,1,2,3} , new String[] {"1","2","3","4"});
		
    pluginInterface.addConfigUIParameters(parameters,"autospeed.config.title");
    
		if(enabled) {
		  getPingIP();
		}
	}
	
	private void log(String text) {
		log.appendText(text + "\n");
	}
	
	public void pingFailure(Ping ping) {
	  if(this.ping != ping) {
	    ping.stop();
	    return;
	  }
		nbFailures++;
		//In case we get 10 consecutive failures, re-get the ping IP
		if(nbFailures >= 10) {
		  	nbFailures = 0;
		  	nbByPass++;		  	
		  	if(nbByPass > 4) nbByPass = minByPass;
			log("Ping failed 10 consecutive times, re-getting the ping IP, lowering UP speed by 10");
			
			int maxUp = pluginInterface.getPluginconfig().getIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC);
			maxUp = maxUp - 10;
			if(maxUp < minimumUp) maxUp = minimumUp;			
			pluginInterface.getPluginconfig().setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC,maxUp);
			
			stopPing();			
			pingIP = null;
			getPingIP();
		} else {
			//Simulate a very slow ping
			pingSuccess(ping,5000);
		}
	}
	

	public synchronized void pingSuccess(Ping ping,int ms) {
	  if(this.ping != ping) {
	    ping.stop();
	    return;
	  }
		if(ms < 5000) {
		  nbFailures = 0;
		  nbByPass = minByPass;
		}
		
		//log("Pinged " + pingIP + " in " + ms + " ms. ");
		lastPings[(iPings++) % NB_PINGS] = ms;
		int average = 0;
		for(int i = 0 ; i < NB_PINGS ; i++) {
			average += lastPings[i];			
		}
		average = average / NB_PINGS;
		
		status.setText("Last Ping : " + ms + " ms, Average Ping Time (over " + NB_PINGS + " pings) : " + average + " ms.");
		
		if(iPings >= (NB_PINGS + NB_WAIT_PINGS) ) {
			
			iPings = 0;

			long totalUpSpeed = 0;
      long totalDownSpeed = 0;
			Download[] downloads = pluginInterface.getDownloadManager().getDownloads();
			for(int i = 0 ; i < downloads.length ; i++) {
				totalUpSpeed += downloads[i].getStats().getUploadAverage();
        totalDownSpeed += downloads[i].getStats().getDownloadAverage();
			}
			
			//B/s => KB/s
			int upSpeed = (int) (totalUpSpeed / 1024);			
			int downSpeed = (int) (totalDownSpeed / 1024);
      
			int maxUp = pluginInterface.getPluginconfig().getIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC);
			int maxDown = pluginInterface.getPluginconfig().getIntParameter(PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC);
      
			int targetPing = pluginInterface.getPluginconfig().getPluginIntParameter("targetping",150);
			if(targetPing <= 0) targetPing = 50;
			if(targetPing > 1000) targetPing = 1000;
			
			int minPing =  2 * targetPing / 3;
			int maxPing =  3 * targetPing / 2;

		
			//THE Rules ;)
			//Adjust : negative if ping is higher than desired, positive if lower
			if(average < minPing && (maxUp < (upSpeed + 10) || (upDownRatio != 0 && maxDown < (downSpeed + 10)))) {
			  int delta = 1 + (2 * minPing) / (3 * average) ;
			  maxUp += delta;
			  log("average ping (" + average + " ms) lower than " + minPing + ", increasing up speed by " + delta);
			}
			if(average > maxPing) {
			  int delta = 1 + (2 * average) / (3 * maxPing);
			  maxUp -= delta;
			  log("average ping (" + average + " ms) higher than " + maxPing + ", decreasing up speed by " + delta);
			}
			
			if(maxUp < minimumUp) maxUp = minimumUp;			
			if(maxUp > maximumUp) maxUp = maximumUp;
			            
			pluginInterface.getPluginconfig().setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC,maxUp);
      
      maxDown = maxUp * upDownRatio;
      if(maxDown > 0) {
        pluginInterface.getPluginconfig().setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC,maxDown);
      }
      
			try {
				pluginInterface.getPluginconfig().save();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	
	
	public void closedownInitiated() {
		stopPing();
	}
	

	public void closedownComplete() {

	}
	

	public void initializationComplete() {

	}
	
	public void configParameterChanged(ConfigParameter param) {
	  
	  minimumUp = pluginInterface.getPluginconfig().getPluginIntParameter("minimumUp",5);
		if(minimumUp < 5) minimumUp = 5;
		
		maximumUp = pluginInterface.getPluginconfig().getPluginIntParameter("maximumUp",1024);
		if(maximumUp < 5) maximumUp = 5;
		
		minByPass = pluginInterface.getPluginconfig().getPluginIntParameter("minByPass",0);
		forceIP = pluginInterface.getPluginconfig().getPluginStringParameter("forceIP","");
		
    upDownRatio = pluginInterface.getPluginconfig().getPluginIntParameter("upDownRatio",4);
	  
		boolean oldEnabled = enabled;		
		enabled = pluginInterface.getPluginconfig().getPluginBooleanParameter("enable",true);
		//if enabled has changed, start / stop the ping process
		if(oldEnabled != enabled) {
			if(enabled) {
				if(pingIP != null) {					
					startPing();
				} else {
					getPingIP();
				}
			}
			else {
				stopPing();
			}
		}		
		log("Configuration Changed, enabled : " + enabled + ", minimum Up Speed : " + minimumUp + " kB/s, maximum up Speed : " + maximumUp + " kB/s.");
	}
	
	private void startPing() {
	  if(ping != null)
	  	stopPing();
		log("Starting to ping " + pingIP);
		ping = new Ping(pingIP);
		ping.addListener(this);
		ping.start();
	}
	
	private void stopPing() {
		log("Stopping to ping " + pingIP);
		if(ping != null)
			ping.stop();
		
		ping = null;
		
		if(routeHostRecheck !=  null) {
			routeHostRecheck.destroy();
			routeHostRecheck = null;
		}
	}
	
	private void getPingIP() {
	  	
	  //should limit issues of this method being called when a ping already exists ...
	  if(ping != null || routeHostRecheck != null)
	  	  return;
	  	
	  forceIP = pluginInterface.getPluginconfig().getPluginStringParameter("forceIP","");
	  
	  if(forceIP != null && !forceIP.equals("")) {
	    pingIP = forceIP;
		log("PING IP Forced Set to : " + pingIP);
		startPing();
	  } else {			  
		String routeHost = pluginInterface.getPluginconfig().getStringParameter("routehost","www.google.com");
		log("Tracing route to : " + routeHost);
		String ips[] = TraceRoute.traceRoute(routeHost);
		for(int i = 0 ; i < ips.length ; i++) {
			log("Trace Route : " + ips[i]);			
		}
		log("Detecting IP to ping, bypassing " + nbByPass + " public IPs");
		String ip = TraceRoute.getGatewayIP(ips,nbByPass);
		log("Detected IP to ping : " + ip);
		if(TraceRoute.checkValidIp(ip)) {
			pingIP = ip;
			log("PING IP Set to : " + pingIP);
			startPing();
		} else {
			log("IP is not valid");
			if(pingIP != null) {
				log("PING IP remains " + pingIP);
			} else {
				log("No Ping IP found, will retry in 5 minutes.");
				routeHostRecheck = pluginInterface.getUtilities().createTimer("Route Host Recheck");
				routeHostRecheck.addPeriodicEvent(5 * 60 * 1000,this);
			}
		}	
	  }
	}
	
	public void perform(UTTimerEvent event) {
	  if(routeHostRecheck != null)
			routeHostRecheck.destroy();
	  
		routeHostRecheck = null;
		if(enabled) {
			getPingIP();
		}
	}
	
}
