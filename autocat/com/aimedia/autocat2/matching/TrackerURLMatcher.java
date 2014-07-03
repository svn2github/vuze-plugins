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

import java.util.List;
import java.util.regex.Matcher;

import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

/**
 * , created 9-Apr-2005
 * 
 * @author Chris Rose
 */
public class TrackerURLMatcher extends TorrentMatcher {

    protected TrackerURLMatcher(String triggerText, String category) {
        super (TorrentFieldType.TRACKER_URL, triggerText, category);
    }

    /*
     * @see com.aimedia.autocat2.matching.IMatcher#match(org.gudy.azureus2.plugins.torrent.Torrent)
     */
    public boolean match (Torrent torrent) {
     
    	List<List<String>> all_urls = TorrentUtils.announceGroupsToList(PluginCoreUtils.unwrap( torrent ));
    	
    	for ( List<String> l: all_urls ){
    		
    		for ( String url: l ){
    	
    			Matcher m = getTriggerPattern().matcher( url );

    			if ( m.find()){
    				
    				return( true );
    			}
    		}
    	}
    	
    	return( false );
    }
}
