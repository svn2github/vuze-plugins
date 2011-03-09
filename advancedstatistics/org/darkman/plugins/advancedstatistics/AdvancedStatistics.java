/*
 * Azureus Advanced Statistics Plugin
 * 
 * Created on Monday, August 22nd 2005
 * Created by Darko Matesic
 * Copyright (C) 2005 Darko Matesic, All Rights Reserved.
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
 */
package org.darkman.plugins.advancedstatistics;

import org.darkman.plugins.advancedstatistics.dataprovider.*;
import org.darkman.plugins.advancedstatistics.util.Log;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * @author Darko Matesic
 *
 * 
 */
public class AdvancedStatistics implements Plugin, PluginListener, UIManagerListener {
	private static final String ADVANCED_STATISTICS_VIEW = "AdvancedStatisticsView";
    protected DataProvider dataProvider;
    protected AdvancedStatisticsView ASV = null;
    public AdvancedStatistics() { super(); }
    public void initialize(PluginInterface pluginInterface) throws PluginException {
    	System.err.println("AdvancedStats !+");
        Log.setLogDirectoryName(pluginInterface.getPluginDirectoryName());
        Log.clear();
        Log.out("AdvancedStatistics.initialize");
        try {
            dataProvider = new DataProvider(pluginInterface, this);
        } catch(Exception ex) {
            Log.out("Error initializing data provider: " + ex.getMessage());
            Log.outStackTrace(ex);
            throw new PluginException("Error initializing data provider", ex);
        }
		pluginInterface.addListener(this);
		pluginInterface.getUIManager().addUIListener(this);
        
    }
    public void initializationComplete() {
        Log.out("AdvancedStatistics.initializationComplete");
    }
	public void closedownInitiated() {
        Log.out("AdvancedStatistics.closedownInitiated");
	    dataProvider.closedown();
	}	
	public void closedownComplete() {
        Log.out("AdvancedStatistics.closedownComplete");
	}
	
	public UISWTInstance swt_ui;
	
	public void UIAttached(UIInstance ui) {
		if (!(ui instanceof UISWTInstance)) {return;}
		this.swt_ui = (UISWTInstance)ui;
		this.ASV = new AdvancedStatisticsView(dataProvider);
		System.out.println("adding view");
		swt_ui.addView(UISWTInstance.VIEW_MAIN, ADVANCED_STATISTICS_VIEW, ASV);
	}
	
	public void UIDetached(UIInstance ui) {
		if (ui instanceof UISWTInstance) {
			this.swt_ui.removeViews(UISWTInstance.VIEW_MAIN, ADVANCED_STATISTICS_VIEW);
			this.swt_ui = null;
			this.ASV = null;
		}		
	}
	
}