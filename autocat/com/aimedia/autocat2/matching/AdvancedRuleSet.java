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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aimedia.autocat.AutoCatPlugin;

/**
 * , created 9-Apr-2005
 *
 * @author Chris Rose
 */
public class AdvancedRuleSet implements OrderedRuleSet {

    private final ArrayList<IRuleSetListener> listeners;

    private final List<TorrentMatcher>        rules;

    private int[]                             ver;

    public AdvancedRuleSet() {
        listeners = new ArrayList<IRuleSetListener> ();
        rules = new ArrayList<TorrentMatcher> ();
        setVersion (1, 1, 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#addRule(com.aimedia.autocat2.matching.TorrentFieldType,
     *      java.lang.String, java.lang.String)
     */
    public void addRule (final TorrentFieldType type, final String trigger, final String category) {

        addRule (type, trigger, category, -1);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#addRule(com.aimedia.autocat2.matching.TorrentFieldType,
     *      java.lang.String, java.lang.String, int)
     */
    public void addRule (final TorrentFieldType type, final String trigger, final String category, int index) {
        final TorrentMatcher matcher = TorrentMatcher.createTorrentMatcher (type, trigger, category);
        synchronized (rules) {
            if (index < 0) {
                rules.add (matcher);
            }
            else {
                rules.add (index, matcher);
            }
        }
        notifyListeners ();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#changeRule(java.lang.String,
     *      com.aimedia.autocat2.matching.TorrentFieldType, java.lang.String,
     *      java.lang.String)
     */
    public void changeRule (final String oldRuleTrigger, final TorrentFieldType newType, final String newTrigger,
            final String newCategory) {
        synchronized (rules) {
            final int idx = findRule (oldRuleTrigger);
            if (idx < 0) { throw new IllegalArgumentException ("The rule with trigger " + oldRuleTrigger
                    + " does not exist"); }
            final TorrentMatcher oldMatcher = rules.remove (idx);
            final TorrentMatcher newMatcher = TorrentMatcher.createTorrentMatcher (newType == null ? oldMatcher
                    .getMatchField () : newType, newTrigger == null ? oldMatcher.getTrigger () : newTrigger,
                    newCategory == null ? oldMatcher.getCategory () : newCategory);
            rules.add (idx, newMatcher);
        }
        notifyListeners ();
    }

    /**
     * @param oldRuleTrigger
     * @return
     */
    private int findRule (String trigger) {
        int found = -1;
        synchronized (rules) {
            for (int i = 0; i < rules.size (); i++) {
                if (rules.get (i).getTrigger ().equals (trigger)) {
                    found = i;
                    break;
                }
            }
        }

        return found;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#getRules()
     */
    public List<TorrentMatcher> getRules () {
        return rules;
    }

    @SuppressWarnings("unchecked")
    public boolean load (final File configFile) {
        boolean success = true;
        FileInputStream fin = null;
        // Open the file
        if (configFile.exists ()) {

            // : this assumes a property file configuration
            Properties ruleProperties = new Properties ();
            try {
                fin = new FileInputStream (configFile);
                ruleProperties.load (fin);
                fin.close ();
            }
            catch (Exception e) {
                success = false;
                AutoCatPlugin.getPlugin ().getLog ().log ("Unable to load configuration", e);
            }

            // : interpret the rule set as a set of properties
            this.rules.addAll (createRulesetFromProperties (ruleProperties));
        }
        else {
            success = false;
        }
        if (success) {
            notifyListeners ();
        }
        return success;
    }

    /**
     * @param versionMajor
     * @param versionMinor
     * @param versionSub
     */
    private void setVersion (final int versionMajor, final int versionMinor, final int versionSub) {
        if (ver == null) {
            ver = new int[3];
        }
        ver[0] = versionMajor;
        ver[1] = versionMinor;
        ver[2] = versionSub;
    }

    public int getMajorVersion () {
        return ver[0];
    }

    public int getMinorVersion () {
        return ver[1];
    }

    public int getSubVersion () {
        return ver[2];
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#addRuleSetListener(com.aimedia.autocat2.matching.IRuleSetListener)
     */
    public void addRuleSetListener (final IRuleSetListener listener) {
        synchronized (listeners) {
            listeners.add (listener);
        }
        AutoCatPlugin.getPlugin ().getLog ().log (
                AutoCatPlugin.getPlugin ().getLocale ().getLocalisedMessageText ("autocat.info.listeningToRules",
                        new String[] { listener.toString () }));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#removeRuleSetListener(com.aimedia.autocat2.matching.IRuleSetListener)
     */
    public void removeRuleSetListener (final IRuleSetListener listener) {
        synchronized (listeners) {
            listeners.remove (listener);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#match(org.gudy.azureus2.plugins.torrent.Torrent)
     */
    public String match (final Torrent torrent) {
        AutoCatPlugin.getPlugin ().getLog ().log (
                AutoCatPlugin.getPlugin ().getLocale ().getLocalisedMessageText ("autocat.info.startMatch",
                        new String[] { torrent.getName () }));
        for (final Iterator<TorrentMatcher> iter = rules.iterator (); iter.hasNext ();) {
            final TorrentMatcher r = iter.next ();
            if (r.match (torrent)) {
                AutoCatPlugin.getPlugin ().getLog ().log (
                        AutoCatPlugin.getPlugin ().getLocale ().getLocalisedMessageText ("autocat.info.ruleMatched",
                                new String[] { r.getTrigger (), torrent.getName (), r.getCategory () }));
                return r.getCategory ();
            }
        }
        return null;
    }

    private void notifyListeners () {
        synchronized (listeners) {
            for (final Iterator<IRuleSetListener> iter = listeners.iterator (); iter.hasNext ();) {
                final IRuleSetListener l = iter.next ();
                l.ruleSetUpdated (this);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean save (final File configFile) {
        boolean success = true;

        Properties ruleProperties = new Properties ();
        ruleProperties.setProperty ("autocat.version.major", getMajorVersion () + "");
        ruleProperties.setProperty ("autocat.version.minor", getMinorVersion () + "");
        ruleProperties.setProperty ("autocat.version.patch", getSubVersion () + "");

        int position = 0;
        for (TorrentMatcher rule : rules) {
            String base = String.format ("autocat.rule.%d.", position);

            ruleProperties.setProperty (base + "position", position + ""); // :
                                                                            // default
                                                                            // to
                                                                            // the
                                                                            // initial
                                                                            // order;
                                                                            // there
                                                                            // is
                                                                            // no
                                                                            // info
                                                                            // for
                                                                            // this
                                                                            // stored.
            ruleProperties.setProperty (base + ENABLED, Boolean.toString (rule.isEnabled ()));
            ruleProperties.setProperty (base + TRIGGER, rule.getTrigger ());
            ruleProperties.setProperty (base + CATEGORY, rule.getCategory ());
            ruleProperties.setProperty (base + FIELD, rule.getMatchField ().getID () + "");

            position++;
        }

        FileOutputStream fos = null;
        try {
            if (!configFile.canWrite ()) {
                success = false;
            }
            else {
                fos = new FileOutputStream (configFile);
                ruleProperties.store (fos, "Saved properties on " + new Date ().toString ());
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace ();
            success = false;
        }
        catch (IOException e) {
            e.printStackTrace ();
            success = false;
        }
        return success;
    }

    private static final String    BASEMAP       = "rules";

    private static final String    CATEGORY      = "category";

    private static final String    ENABLED       = "enabled";

    private static final String    TRIGGER       = "trigger";

    private static final String    FIELD         = "field";

    private static final String    VERSION_MAJOR = "version";

    private static final String    VERSION_MINOR = "minor_version";

    private static final String    VERSION_SUB   = "sub_version";

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#removeRule(int)
     */
    public void removeRule (int index) {
        rules.remove (index);
        notifyListeners ();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat2.matching.OrderedRuleSet#swapRulePositions(int,
     *      int)
     */
    public void swapRulePositions (int rule1Position, int rule2Position) {
        if (rule1Position == rule2Position)
            return;
        synchronized (rules) {
            TorrentMatcher rule1 = rules.remove (rule1Position);
            TorrentMatcher rule2 = rules.remove (rule2Position);
            if (rule1 == null || rule2 == null) { throw new IllegalArgumentException ("Invalid rule specification"); }
            rules.add (rule2Position, rule1);
            rules.add (rule1Position, rule2);
        }
    }

    /**
     * Transfer settings from the old bencoded map style to a property file.
     *
     * @param mapFile
     * @param propertyFile
     */
    @SuppressWarnings("unchecked")
    public static void transferSettings (File mapFile, File propertyFile) {
        List<TorrentMatcher> rules = new ArrayList<TorrentMatcher> ();

        try {
            FileInputStream fin = new FileInputStream (mapFile);
            BufferedInputStream bin = new BufferedInputStream (fin);
            final Map<String, ?> map = BDecoder.decode (bin);
            // This is version-specific code now.
            @SuppressWarnings("unused")
            Long vmaj, vmin, vsub; // NOPMD
            vmaj = (Long) map.get (VERSION_MAJOR);
            vmin = (Long) map.get (VERSION_MINOR);
            vsub = (Long) map.get (VERSION_SUB);
            final List<Map<String, ?>> ruleList = (List<Map<String, ?>>) map.get (BASEMAP);
            final Iterator<Map<String, ?>> iter = ruleList.iterator ();
            synchronized (rules) {
                while (iter.hasNext ()) {
                    // TODO Make this do cleaner versioned behaviour
                    final Map<String, ?> rule = iter.next ();
                    final String ltrig = new String ((byte[]) rule.get (TRIGGER));
                    final String lcat = new String ((byte[]) rule.get (CATEGORY));
                    Long ltype = (Long) rule.get (FIELD);
                    if (ltype == null) {
                        ltype = new Long (TorrentFieldType.FILE_NAME.getID ());
                    }
                    final boolean len = ((Long) rule.get (ENABLED)).longValue () != 0;

                    final TorrentMatcher nm = TorrentMatcher.createTorrentMatcher (TorrentFieldType.getById (ltype
                            .intValue ()), ltrig, lcat);
                    nm.setEnabled (len);
                    rules.add (nm);
                }
            }
            bin.close ();
            fin.close ();

            // : save the rules as properties
            Properties ruleProperties = new Properties ();
            ruleProperties.setProperty ("autocat.version.major", vmaj + "");
            ruleProperties.setProperty ("autocat.version.minor", vmin + "");
            ruleProperties.setProperty ("autocat.version.patch", vsub + "");
            int ruleId = 0;
            for (TorrentMatcher rule : rules) {
                String base = String.format ("autocat.rule.%d.", ruleId);

                ruleProperties.setProperty (base + "position", ruleId + ""); // :
                                                                                // default
                                                                                // to
                                                                                // the
                                                                                // initial
                                                                                // order;
                                                                                // there
                                                                                // is
                                                                                // no
                                                                                // info
                                                                                // for
                                                                                // this
                                                                                // stored.
                ruleProperties.setProperty (base + ENABLED, Boolean.toString (rule.isEnabled ()));
                ruleProperties.setProperty (base + TRIGGER, rule.getTrigger ());
                ruleProperties.setProperty (base + CATEGORY, rule.getCategory ());
                ruleProperties.setProperty (base + FIELD, rule.getMatchField ().getID () + "");

                ruleId++;
            }

            OutputStream out = new FileOutputStream (propertyFile);
            ruleProperties.store (out, "Written by 'transferSettings' on " + new Date ().toString ());
            out.close ();
        }
        catch (IOException e) {
            // : couldn't migrate; corrupt source file
            AutoCatPlugin.getPlugin ().getLog ().log ("Unable to convert old configuration", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<TorrentMatcher> createRulesetFromProperties (Properties properties) {
        SortedMap<Integer, TorrentMatcher> newRules = new TreeMap<Integer, TorrentMatcher> ();

        // : find all of our rule IDs
        Set<Integer> ruleIds = new HashSet<Integer> ();
        Pattern idPattern = Pattern.compile ("autocat\\.rule\\.([0-9]*)\\." + ENABLED);
        for (Enumeration<String> keyEnum = (Enumeration<String>) properties.propertyNames (); keyEnum
                .hasMoreElements ();) {
            String key = keyEnum.nextElement ();
            Matcher m = idPattern.matcher (key);
            if (m.matches ()) {
                ruleIds.add (Integer.valueOf (m.group (1)));
            }
        }

        // : read in the rules
        for (Integer ruleId : ruleIds) {
            String base = String.format ("autocat.rule.%d.", ruleId);

            try {
                String value = null;
                value = properties.getProperty (base + "position");
                if (value == null) {
                    AutoCatPlugin.getPlugin ().getLog ().log (
                            "Error parsing rule " + ruleId + ": missing required property " + base + "position");
                }
                Integer position = Integer.valueOf (value);

                value = properties.getProperty (base + ENABLED);
                if (value == null) {
                    AutoCatPlugin.getPlugin ().getLog ().log (
                            "Error parsing rule " + ruleId + ": missing required property " + base + ENABLED);
                }
                boolean enabled = Boolean.valueOf (value);

                value = properties.getProperty (base + TRIGGER);
                if (value == null) {
                    AutoCatPlugin.getPlugin ().getLog ().log (
                            "Error parsing rule " + ruleId + ": missing required property " + base + TRIGGER);
                }
                String trigger = value;

                value = properties.getProperty (base + CATEGORY);
                if (value == null) {
                    AutoCatPlugin.getPlugin ().getLog ().log (
                            "Error parsing rule " + ruleId + ": missing required property " + base + CATEGORY);
                }
                String category = value;

                value = properties.getProperty (base + FIELD);
                if (value == null) {
                    AutoCatPlugin.getPlugin ().getLog ().log (
                            "Error parsing rule " + ruleId + ": missing required property " + base + FIELD);
                }
                TorrentFieldType type = TorrentFieldType.getById (Integer.parseInt (value));

                TorrentMatcher rule = TorrentMatcher.createTorrentMatcher (type, trigger, category);
                rule.setEnabled (enabled);

                newRules.put (position, rule);
            }
            catch (Exception e) {
                AutoCatPlugin.getPlugin ().getLog ().log ("Error parsing rule " + ruleId, e);
            }
        }

        return Collections.unmodifiableCollection (newRules.values ());
    }
}
