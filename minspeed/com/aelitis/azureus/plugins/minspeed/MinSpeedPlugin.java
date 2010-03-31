/**
 * 
 */
package com.aelitis.azureus.plugins.minspeed;

import java.util.ArrayList;
import java.util.Iterator;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.components.UITextField;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

/**
 * @author Allan Crooks
 *
 */
public class MinSpeedPlugin implements Plugin, UTTimerEventPerformer, DownloadListener, DownloadManagerListener {
	
	private long min_upload_speed_last_met;
	private long min_upload_speed_last_failed;
	private Utilities utils;
	private DownloadManager download_manager;
	
	private static long ACTION_PERIOD_IN_MS = 30000; 
	
	private boolean enabled;
	private int min_upspeed = 0;
	private boolean obey_ordering;
	private ArrayList started_uploads = new ArrayList();
	private TorrentAttribute forced_by_us;
	private LoggerChannel log;
	private UITextField status;
	private UITextField activity;
	
	public void initialize(PluginInterface plugin_interface) {
		utils = plugin_interface.getUtilities();
		download_manager = plugin_interface.getDownloadManager();
		forced_by_us = plugin_interface.getTorrentManager().getPluginAttribute("forced_up_start");
		
		// Config settings.
		final BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel("minspeed");
		BooleanParameter enabled_param = config.addBooleanParameter2("up.enabled", "minspeed.enabled", true);
		IntParameter up_speed_param = config.addIntParameter2("up.speed", "minspeed.min_up_speed", 0);
		BooleanParameter obey_ordering_param = config.addBooleanParameter2("up.obey_ordering", "minspeed.obey_ordering", true);
		
		enabled = enabled_param.getValue();
		min_upspeed = up_speed_param.getValue();
		obey_ordering = obey_ordering_param.getValue();
		
		// Create an area for logging.
		final BasicPluginViewModel model = plugin_interface.getUIManager().createBasicPluginViewModel("minspeed");
		log = plugin_interface.getLogger().getTimeStampedChannel("minspeed");
		model.getProgress().setVisible(false);

		// Set these fields to their default values.
		status = model.getStatus();
		activity = model.getActivity();
		this.updateStatus();
		this.updateActivity(null);
		
		this.resetTimeCounters("plugin initialised");

		// Link everything up to the view model.
		enabled_param.addListener(new ParameterListener() {
			public void parameterChanged(Parameter p) {
				enabled = ((BooleanParameter)p).getValue();
				if (enabled) {resetTimeCounters("plugin has been enabled");}
				else {resetStartedTorrents("plugin has been disabled");}
			}
		});
		
		up_speed_param.addListener(new ParameterListener() {
			public void parameterChanged(Parameter p) {
				min_upspeed = ((IntParameter)p).getValue();
				
				/**
				 * I think things could sort themselves out if this happened, so let's not
				 * force it. We will update the status though (text change).
				 */
				// resetStartedTorrents("minimum upload speed has been changed");
				updateStatus();
			}
		});
		
		obey_ordering_param.addListener(new ParameterListener() {
			public void parameterChanged(Parameter p) {
				obey_ordering = ((BooleanParameter)p).getValue();
			}
		});
		
		log.addListener(new LoggerChannelListener() {
			public void messageLogged(int type, String message) {model.getLogArea().appendText(message+"\n");}
			public void messageLogged(String message, Throwable t) {model.getLogArea().appendText(t.toString() + "\n");}
		});
		
		log.log("Plugin initialised.");

		// Cleans up invalid force start statuses and allows us to make sure we clear our
		// data from it.
		download_manager.addListener(this, true);
		
		// Do this last!
		utils.createTimer("MinSpeedPlugin", false).addPeriodicEvent(3000, this);
	}
	
	public void perform(UTTimerEvent event) {
		try {perform0(event);}
		catch (RuntimeException re) {log.log("ERROR", re); throw re;}
	}
	
	private void perform0(UTTimerEvent event) {
		if (this.min_upspeed == 0 || !enabled) {
			return;
		}
		
		long now = utils.getCurrentSystemTime();
		long rate = this.download_manager.getStats().getDataSendRate();
		boolean speed_met = (this.min_upspeed * 1024) <= rate;
		if (speed_met) {
			this.min_upload_speed_last_met = now;
			long elapsed = now - this.min_upload_speed_last_failed;
			
			// We only want to report "over-uploading" if we are contributing to it.
			int reported_secs = (int)((elapsed / 5000) * 5);
			if (reported_secs == 0 || this.started_uploads.isEmpty()) {this.updateActivity(null); return;}

			this.updateActivity("Upload speed above minimum for " + reported_secs + " seconds.");
			if (elapsed >= ACTION_PERIOD_IN_MS && !this.started_uploads.isEmpty()) {
				this.updateActivity("Upload speed above minimum for " + reported_secs + " seconds - seeing if we can stop some torrents.");
				this.tryToReduceSlotsUsed();
			}
		}
		else {
			this.min_upload_speed_last_failed = now;
			long elapsed = now - this.min_upload_speed_last_met;
			int reported_secs = (int)((elapsed / 5000) * 5);
			if (reported_secs == 0) {this.updateActivity(null);}
			else {this.updateActivity("Upload speed less than minimum for " + reported_secs + " seconds.");}
			if (elapsed >= ACTION_PERIOD_IN_MS) {
				Download started = useNewUploadSlot(rate);
				if (started == null) {
					this.updateActivity("Upload speed less than minimum for " + reported_secs + " seconds - waiting to find a torrent to start.");
				}
			}
		}
	}
	
	private void tryToReduceSlotsUsed() {
		// What's the current upload rate?
		long current_rate = download_manager.getStats().getDataSendRate();
		
		boolean did_stuff = false;
		
		// We'll keep going through in order, to find the lowest positioned torrent.
		while (!this.started_uploads.isEmpty()) {
			Download last_d = null;
			for (int i=0; i<started_uploads.size(); i++) {
				Download this_d = (Download)started_uploads.get(i);
				if (last_d == null) {last_d = this_d;}
				else if (last_d.getPosition() < this_d.getPosition()) {last_d = this_d;}
			}
			
			// Can we stop this torrent without falling beneath the minimum size limit?
			long last_d_rate = last_d.getStats().getUploadAverage();
			if (current_rate - last_d_rate >= (this.min_upspeed * 1024)) {
				current_rate -= last_d_rate;
				this.stopUpload(last_d);
				log.log("Estimating upload speed will drop from " + fmtSize(current_rate + last_d_rate) + " to " + fmtSize(current_rate) + ".");
				did_stuff = true;
			}
			else {break;}
		}
		
		if (did_stuff) {
			this.resetTimeCounters("freed up forced upload slot(s) for torrents in last position");
		}
		
		did_stuff = false;
		
		// If we still have torrents left, check to see what the smallest one is and see if
		// we can stop it.
		if (!obey_ordering) {
			while (!this.started_uploads.isEmpty()) {
				Download slow_d = null;
				long slow_rate = Long.MAX_VALUE;
				for (int i=0; i<started_uploads.size(); i++) {
					Download this_d = (Download)started_uploads.get(i);
					long this_rate = this_d.getStats().getUploadAverage();
					
					if (this_rate < slow_rate) {
						slow_d = this_d;
						slow_rate = this_rate;
					}
				}
				
				if (current_rate - slow_rate >= (this.min_upspeed * 1024)) {
					current_rate -= slow_rate;
					this.stopUpload(slow_d);
					log.log("Estimating upload speed will drop from " + fmtSize(current_rate + slow_rate) + " to " + fmtSize(current_rate) + ".");
					did_stuff = true;
				}
				else {break;}
			}
		}
		
		if (did_stuff) {
			this.resetTimeCounters("freed up forced upload slot(s) for slowest torrents");
		}
		
	}
	
	private Download useNewUploadSlot(long rate) {
		// Force a new upload to start - go through uploads, and force start a queued one.
		// Add an attribute.
		Download[] downloads = download_manager.getDownloads();
		for (int i=0; i<downloads.length; i++) {
			if (!downloads[i].isComplete()) {continue;}
			if (downloads[i].getState() != Download.ST_QUEUED) {continue;}
			
			// We only start queued torrents for seeding.
			this.startUpload(downloads[i]);
			this.resetTimeCounters("started torrent to try to reach minimum upload speed (currently at " + fmtSize(rate) + ")");
			return downloads[i];
		}
		return null;
	}
	
	private void resetTimeCounters(String reason) {
		this.min_upload_speed_last_failed = this.min_upload_speed_last_met = utils.getCurrentSystemTime();
		log.log("Resetting time counters - " + reason + ".");
		updateActivity(null);
	}
	
	private void resetStartedTorrents(String reason) {
		if (started_uploads.isEmpty()) {return;}
		log.log("Stopping " + started_uploads.size() + " torrents - " + reason + ".");
		ArrayList to_remove = new ArrayList(started_uploads);
		for (Iterator itr=to_remove.iterator(); itr.hasNext();) {
			stopUpload((Download)itr.next());
		}
		resetTimeCounters(reason);
	}
	
	private void updateActivity(String text) {
		if (!enabled) {activity.setText(""); return;}
		if (text == null) {text = "Monitoring upload bandwidth.";}
		activity.setText(text);
	}
	
	private void updateStatus() {
		if (!enabled) {status.setText("Disabled."); return;}
		status.setText("Running, " + started_uploads.size() + " torrent(s) force started. Rate is " + fmtSize(min_upspeed * 1024) + ".");
	}
	
	private void startUpload(Download d) {
		d.setBooleanAttribute(forced_by_us, true);
		this.started_uploads.add(d);
		d.setForceStart(true);
		d.addListener(this);
		updateStatus();
		log.log(describe(d) + " started.");
	}
	
	private void stopUpload(Download d) {
		d.setBooleanAttribute(forced_by_us, false);
		this.started_uploads.remove(d);
		d.removeListener(this);
		d.setForceStart(false);
		updateStatus();
		log.log(describe(d) + " stopped.");
	}
	
	// We don't need to react to position changes.
	public void positionChanged(Download d, int old_state, int new_state) {}
	public void stateChanged(Download d, int old_state, int new_state) {
		/**
		 * This is something we want to react to. If a torrent changes its state, then we will just
		 * deregister our involvement with it. Why? Because the torrent should just stay as "force start".
		 * 
		 * If the user stops it, or sets it back to downloading (for example), then it's not suitable for us
		 * to still consider it being managed by us.
		 * 
		 * Of course, we have to check the state of the download, just to make sure it's something that can
		 * cope with...
		 */
		if (d.isForceStart()) {
			switch (new_state) {
				case Download.ST_PREPARING:
				case Download.ST_READY:
				case Download.ST_SEEDING:
				case Download.ST_WAITING:
					return;
			}
			
			// I believe the torrent moves from "downloading" to "seeding" instantly. Which is
			// confusing.
			if (new_state == Download.ST_DOWNLOADING && d.isComplete()) {return;}
		}
		stopUpload(d);
		this.resetTimeCounters(describe(d) + " went to state \"" + d.getStats().getStatus(true) + "\" outside of our control.");
	}
	
	private String fmtSize(long size) {
		return this.utils.getFormatters().formatByteCountToKiBEtc(size);
	}
	
	private static String describe(Download d) {
		return "Torrent \"" + d.getName() + "\"";
	}
	
	public void downloadAdded(Download d) {
		// If we still have our attribute on here (in the case of a unclean stop), remove it
		// and reset the force start status.
		if (d.getBooleanAttribute(this.forced_by_us)) {
			log.log(describe(d) + " had flag indicating controlled by us - resetting.");
			d.setBooleanAttribute(this.forced_by_us, false);
		}
	}
	
	public void downloadRemoved(Download d) {
		if (d.getBooleanAttribute(this.forced_by_us)) {stopUpload(d);}
	}

}
