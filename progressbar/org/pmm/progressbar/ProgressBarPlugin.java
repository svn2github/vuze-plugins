/*
 * PBP - Progress Bar Plugin
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

package org.pmm.progressbar;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * @author kustos
 * Allows Azureus to create an instance of the Progress Bar Plugin
 */
public class ProgressBarPlugin implements Plugin, UIManagerListener {
    static final String COLUMN_ID_PROGRESS_BAR = "ProgressBarColumn";
    private PluginInterface pluginInterface;
    
    /**
     * Default constructor.
     * Doesn't do anything. Here to avoid problems.
     */
    public ProgressBarPlugin() {super();}

    /** 
     * This method is called when the Plugin is loaded by Azureus
     * @param pluginInterface the interface that the plugin must use to communicate with Azureus
     * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
     */
    public void initialize(PluginInterface pluginInterface) throws PluginException {
    	this.pluginInterface = pluginInterface;
    	pluginInterface.getUIManager().addUIListener(this);
    }
    
    public void UIAttached(UIInstance ui) {
    	if (!(ui instanceof UISWTInstance)) {return;}
        UIManager uiManager = pluginInterface.getUIManager();        
        
        TableManager tableManager = uiManager.getTableManager();
        TableColumn progressColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, COLUMN_ID_PROGRESS_BAR);
        
        ProgressBarConfig config = new ProgressBarConfig(pluginInterface, (UISWTInstance)ui, progressColumn);
        ProgressBar progressBar = new ProgressBar((UISWTInstance)ui, config);
        
        progressColumn.setAlignment(TableColumn.ALIGN_LEAD);
        progressColumn.setPosition(5);
        progressColumn.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
        progressColumn.setType(TableColumn.TYPE_GRAPHIC);
        
        progressColumn.addCellAddedListener(progressBar);
        progressColumn.addCellDisposeListener(progressBar);
        progressColumn.addCellRefreshListener(progressBar);
        
        tableManager.addColumn(progressColumn);
    }
    
    public void UIDetached(UIInstance ui) {}
}
