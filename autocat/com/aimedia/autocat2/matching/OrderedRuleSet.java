/* MODULE: OrderedRuleSet.java */

/* COPYRIGHT
 * $RCSfile: OrderedRuleSet.java,v $
 * $Revision: 1.0 $
 * $Date: 2006/02/13 20:51:58 $
 * $Author: rosec $
 *
 * Copyright (c) 2007 Chris Rose
 * All rights reserved.
 * END COPYRIGHT */

package com.aimedia.autocat2.matching;

import java.util.List;

import org.gudy.azureus2.plugins.torrent.Torrent;

/**
 * 
 */
public interface OrderedRuleSet {

    public abstract void addRule (final TorrentFieldType type, final String trigger, final String category);

    public abstract void addRule (final TorrentFieldType type, final String trigger, final String category, int index);

    public abstract void changeRule (final String oldRuleTrigger, final TorrentFieldType newType,
            final String newTrigger, final String newCategory);

    public abstract List<TorrentMatcher> getRules ();

    public abstract void removeRule (final int index);

    public abstract void swapRulePositions (final int rule1Position, final int rule2Position);

    public abstract void addRuleSetListener (final IRuleSetListener listener);

    public abstract void removeRuleSetListener (final IRuleSetListener listener);

    public abstract String match (final Torrent torrent);

}