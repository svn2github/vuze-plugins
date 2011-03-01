package com.aimedia.stopseeding.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPropertyEvent;
import org.gudy.azureus2.plugins.download.DownloadPropertyListener;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aimedia.stopseeding.AutoStopPlugin;
import com.aimedia.stopseeding.Logger;
import com.aimedia.stopseeding.ResourceConstants;
import com.aimedia.stopseeding.core.AutoStopConfiguration.StopAction;

public class RatioWatcher implements DownloadListener, DownloadManagerListener, DownloadPropertyListener,
        UTTimerEventPerformer {

    private static RatioWatcher         instance;

    private Set<Download>               managedDownloads;

    private UTTimer                     timer;

    private TorrentAttribute            attribute;

    private final AutoStopConfiguration configuration;

    private RatioWatcher() {
        managedDownloads = new HashSet<Download> ();
        attribute = AutoStopPlugin.plugin ().getPluginInterface ().getTorrentManager ().getPluginAttribute (
                ResourceConstants.ATTRIBUTE_NAME);
        configuration = AutoStopPlugin.plugin ().getConfiguration ();

        timer = AutoStopPlugin.plugin ().getPluginInterface ().getUtilities ().createTimer ("autostop.ratioWatcher",
                true);

        timer.addPeriodicEvent (AutoStopPlugin.plugin ().getConfiguration ().getScanInterval (), this);

    }

    public static RatioWatcher getInstance () {
        if (null == instance) {
            instance = new RatioWatcher ();
        }
        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.download.DownloadListener#stateChanged(org.gudy.azureus2.plugins.download.Download,
     *      int, int)
     */
    public void stateChanged (Download download, int old_state, int new_state) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.download.DownloadListener#positionChanged(org.gudy.azureus2.plugins.download.Download,
     *      int, int)
     */
    public void positionChanged (Download download, int oldPosition, int newPosition) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.download.DownloadManagerListener#downloadAdded(org.gudy.azureus2.plugins.download.Download)
     */
    public void downloadAdded (Download download) {
        // Start managing this download

        download.addPropertyListener (this);
        download.addListener (this);

        TorrentManager tm = AutoStopPlugin.plugin ().getPluginInterface ().getTorrentManager ();
        TorrentAttribute ta = tm.getPluginAttribute (ResourceConstants.ATTRIBUTE_NAME);

        if (download.getAttribute (ta) == null) {
            download.setAttribute (ta, AutoStopPlugin.plugin ().getConfiguration ().getDefaultRatio (download));
        }

        synchronized (managedDownloads) {
            managedDownloads.add (download);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.download.DownloadManagerListener#downloadRemoved(org.gudy.azureus2.plugins.download.Download)
     */
    public void downloadRemoved (Download download) {

        download.removeListener (this);
        download.removePropertyListener (this);

        synchronized (managedDownloads) {
            managedDownloads.remove (download);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.download.DownloadPropertyListener#propertyChanged(org.gudy.azureus2.plugins.download.Download,
     *      org.gudy.azureus2.plugins.download.DownloadPropertyEvent)
     */
    public void propertyChanged (Download download, DownloadPropertyEvent event) {
        // TODO Auto-generated method stub

    }

    public void perform (UTTimerEvent event) {
        if (!configuration.isEnabled ()) { return; }
        synchronized (managedDownloads) {
            for (Iterator iter = managedDownloads.iterator (); iter.hasNext ();) {
                Download element = (Download) iter.next ();
                String thresholdString = element.getAttribute (attribute);

                // : Let "Unlimited" pass
                if ("Unlimited".equalsIgnoreCase (thresholdString)) {
                    continue;
                }

                if (element.isComplete ()) {
                    // : TODO perform ratio checks on the download.
                    int ratio = element.getStats ().getShareRatio ();

                    // : convert the ratio string into 1000ths
                    if (thresholdString == null) {
                        thresholdString = configuration.getDefaultRatio (element);
                    }
                    float ratioMultiple = Float.parseFloat (thresholdString);
                    int threshold = Math.round (ratioMultiple * 1000);

                    if (ratio > threshold) {

                        try {

                            // : only stop the download if it's not already
                            // stopped
                            if (element.getState () != Download.ST_STOPPED) {
                                element.stop ();
                            }

                            StopAction action = configuration.getAction ();

                            Logger.info (ResourceConstants.LOG_STOP_SEEDING, AutoStopPlugin.plugin ()
                                    .getPluginInterface ().getUtilities ().getLocaleUtilities ()
                                    .getLocalisedMessageText (action.getResource ()), element.getName ());

                            switch (action) {
                            case StopAndDeleteData:
                                element.remove (true, true);
                                break;
                            case StopAndDeleteTorrent:
                                element.remove (true, false);
                                break;
                            case StopAndRemove:
                                element.remove ();
                                break;
                            case StopSeeding:
                                // : nothing
                                break;
                            }
                        }
                        catch (DownloadException e) {
                            Logger.alert (ResourceConstants.CANNOT_STOP_DOWNLOAD, e);
                        }
                        catch (DownloadRemovalVetoException e) {
                            Logger.warn (ResourceConstants.CANNOT_REMOVE_DOWNLOAD, e.getMessage ());
                        }
                    }
                }
            }
        }
    }

}
