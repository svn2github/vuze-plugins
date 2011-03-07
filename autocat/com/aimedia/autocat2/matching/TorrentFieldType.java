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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * , created 9-Apr-2005
 * 
 * @author Chris Rose
 */
public class TorrentFieldType {

    private static final int                            FILENAME_ID   = 1;

    private static SortedMap<Integer, TorrentFieldType> inverseHash;

    private static final int                            TRACKERURL_ID = 0;

    public static final TorrentFieldType                FILE_NAME     = new TorrentFieldType ("Filename", FILENAME_ID);

    public static final TorrentFieldType                TRACKER_URL   = new TorrentFieldType ("Tracker URL",
                                                                              TRACKERURL_ID);

    public static TorrentFieldType getById (final int id) {
        if (inverseHash == null) {
            inverseHash = new TreeMap<Integer, TorrentFieldType> ();
        }
        final TorrentFieldType ret = (TorrentFieldType) inverseHash.get (new Integer (id));
        if (ret == null) { throw new IllegalArgumentException (id + " is not a valid type ID"); }
        return ret;
    }

    public static final List<TorrentFieldType> getSupportedFields () {
        if (inverseHash == null) { return Collections.emptyList (); }
        final Set<Integer> keys = inverseHash.keySet ();
        final List<TorrentFieldType> retList = new ArrayList<TorrentFieldType> ();
        for (final Iterator<Integer> iter = keys.iterator (); iter.hasNext ();) {
            final Integer key = iter.next ();
            retList.add (inverseHash.get (key));
        }
        return retList;
    }

    /**
     * @param type
     */
    private static void addHash (final TorrentFieldType type) {
        if (inverseHash == null) {
            inverseHash = new TreeMap<Integer, TorrentFieldType> ();
        }
        inverseHash.put (new Integer (type.id), type);
    }

    private final int    id;

    private final String name;

    private TorrentFieldType(String name, int id) {
        this.name = name;
        this.id = id;
        addHash (this);
    }

    public int getID () {
        return id;
    }

    public String getName () {
        return name;
    }

    public String toString () {
        return name;
    }
}
