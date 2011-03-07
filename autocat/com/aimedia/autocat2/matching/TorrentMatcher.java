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

package com.aimedia.autocat2.matching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.gudy.azureus2.plugins.torrent.Torrent;

/**
 * , created 9-Apr-2005
 * 
 * @author Chris Rose
 */
public abstract class TorrentMatcher implements IMatcher, IMatcherObservable {

    public static TorrentMatcher createTorrentMatcher (final TorrentFieldType type, final String triggerText,
            final String category) {
        if (type == TorrentFieldType.FILE_NAME || type == null) {
            return new FileNameMatcher (triggerText, category);
        }
        else if (type == TorrentFieldType.TRACKER_URL) {
            return new TrackerURLMatcher (triggerText, category);
        }
        else {
            throw new IllegalArgumentException ("This is not a valid type of field");
        }
    }

    private String                 category;

    private List                   listenerList;

    private final TorrentFieldType matchField;

    private String                 triggerExpression;

    private Pattern                triggerPattern;

    private boolean                enabled;

    protected TorrentMatcher(TorrentFieldType type, String trigger, String category) {
        this.matchField = type;
        this.triggerExpression = trigger;
        this.category = category;
        if (trigger.startsWith ("/") && trigger.endsWith ("/")) {
            // Regex
            this.triggerPattern = Pattern.compile (trigger.substring (1, trigger.length () - 1),
                    Pattern.CASE_INSENSITIVE);
        }
        else {
            this.triggerPattern = Pattern.compile (trigger, Pattern.CASE_INSENSITIVE);
        }
    }

    /*
     * @see com.aimedia.autocat2.matching.IMatcherObservable#addMatcherListener(com.aimedia.autocat2.matching.IMatcherListener)
     */
    public void addMatcherListener (final IMatcherListener listener) {
        getListenerList ().add (listener);
    }

    /**
     * @return Returns the category.
     */
    public String getCategory () {
        return category;
    }

    /**
     * @return Returns the matchField.
     */
    public TorrentFieldType getMatchField () {
        return matchField;
    }

    /**
     * @return Returns the triggerExpression.
     */
    public String getTrigger () {
        return triggerExpression;
    }

    /*
     * @see com.aimedia.autocat2.matching.IMatcherObservable#removeMatcherListener(com.aimedia.autocat2.matching.IMatcherListener)
     */
    public void removeMatcherListener (final IMatcherListener listener) {
        getListenerList ().remove (listener);
    }

    /**
     * @return
     */
    private List getListenerList () {
        if (null == listenerList) {
            listenerList = new ArrayList ();
        }
        return listenerList;
    }

    /**
     * @return Returns the triggerPattern.
     */
    protected Pattern getTriggerPattern () {
        return triggerPattern;
    }

    protected void notifyListeners () {
        notifyListeners (null);
    }

    protected void notifyListeners (final Object message) {
        for (final Iterator iter = getListenerList ().iterator (); iter.hasNext ();) {
            final IMatcherListener listener = (IMatcherListener) iter.next ();
            listener.matcherUpdated (this, message);
        }
    }

    /**
     * @param triggerPattern
     *                The triggerPattern to set.
     */
    protected void setTriggerPattern (final Pattern triggerPattern) {
        this.triggerPattern = triggerPattern;
    }

    public boolean isEnabled () {
        return enabled;
    }

    public void setEnabled (final boolean enablement) {
        enabled = enablement;
    }

    /**
     * @param category
     *                The category to set.
     */
    public void setCategory (final String category) {
        this.category = category;
    }

    /**
     * @param triggerExpression
     *                The triggerExpression to set.
     */
    public void setTrigger (final String triggerExpression) {
        this.triggerExpression = triggerExpression;
    }

    public String toString () {
        return matchField + ": " + triggerExpression + " => " + category;
    }

    public abstract boolean match (Torrent t);
}
