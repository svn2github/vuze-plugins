/*
 * Copyright (C) 2005  Chris Rose
 *
 * AutoCatPlugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * AutoCatPlugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package com.aimedia.autocat;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import com.aimedia.autocat.config.Config;
import com.aimedia.autocat2.matching.IRuleSetListener;
import com.aimedia.autocat2.matching.OrderedRuleSet;

/**
 * , created 8-Jan-2005
 *
 * @author Chris Rose
 */
public class AutoCategorizer implements IRuleSetListener, DownloadManagerListener {

    final transient private DownloadManager dlm;

    final transient private LoggerChannel   log;

    final transient private LocaleUtilities locale;

    private Config config;

    public AutoCategorizer(Config config) {
        super ();
        this.config = config;
        if (config == null) { throw new IllegalArgumentException ("Null rulesets are not permitted"); }
        dlm = AutoCatPlugin.getPlugin ().getPluginInterface ().getDownloadManager ();
        log = AutoCatPlugin.getPlugin ().getLog ();
        locale = AutoCatPlugin.getPlugin ().getPluginInterface ().getUtilities ().getLocaleUtilities ();
        config.getRules ().addRuleSetListener (this);
        dlm.addListener (this);
    }

    /**
     * @see com.aimedia.autocat.matching.IRuleSetListener#ruleSetUpdated(com.aimedia.autocat.matching.RuleSet)
     */
    public void ruleSetUpdated (final OrderedRuleSet rules) {
        final Download[] dls = dlm.getDownloads ();
        for (int i = 0; i < dls.length; i++) {
            match (dls[i]);
        }
    }

    /**
     * @see org.gudy.azureus2.plugins.download.DownloadManagerListener#downloadAdded(org.gudy.azureus2.plugins.download.Download)
     */
    public void downloadAdded (final Download download) {
        // ** FIXED **
        download.addListener (new DownloadListener () {

            public void stateChanged (Download download, int old_state, int new_state) {
                if (old_state == Download.ST_READY
                        && (new_state == Download.ST_DOWNLOADING || new_state == Download.ST_SEEDING)) {
                    match (download);
                }
            }

            public void positionChanged (Download download, int oldPosition, int newPosition) {
                // Nothing
            }
        });
        // ** END FIXED **
        // ** BROKEN (Fails to allow the torrent to be added)
        // match(download);
        // ** END BROKEN
    }

    /**
     * @see org.gudy.azureus2.plugins.download.DownloadManagerListener#downloadRemoved(org.gudy.azureus2.plugins.download.Download)
     */
    public void downloadRemoved (final Download download) {
        // Nothing here.
    }

    private void match (final Download download) {
        final Torrent torrent = download.getTorrent ();
        String categoryName = download.getCategoryName ();

        // : if so configured, ignore already-categorized torrents.
        if (! "Categories.uncategorized".equals (categoryName)) {
            if (! config.isModifyExistingCategories ()) {
                return;
            }
        }

        final String cat = config.getRules ().match (torrent);
        if (cat == null) { // NOPMD
            // TODO Not too sure why I disabled this...
            // d.setCategory("Categories.uncategorized");
            // log.log(LoggerChannel.LT_INFORMATION, locale
            // .getLocalisedMessageText("autocat.matcher.nomatch",
            // new String[] { cat, t.getName() }));
        }
        else {
            download.setCategory (cat);
            log.log (LoggerChannel.LT_INFORMATION, locale.getLocalisedMessageText ("autocat.matcher.matched",
                    new String[] { cat, torrent.getName () }));
        }
    }

}