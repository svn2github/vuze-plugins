/**
 * File: SavePathPlugin.java
 * Library: Save Path plugin for Azureus
 * Date: 9 Nov 2006
 *
 * Author: Allan Crooks
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package com.aelitis.azureus.plugins.azsavepath;

import com.aelitis.azureus.plugins.azsavepath.ui.DownloadContextMenu;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;

/**
 * Here's how the plugin is put together.
 * 
 * You have two main attributes that are in use - the core attribute for setting
 * the relative path component, and the plugin attribute which defines a string
 * which you use to calculate the relative path (the path template).
 * 
 * The path template string will look something like "${category}" or "${manual}".
 * 
 * The parsers package will take that string, parse it, and return a path formatter.
 * 
 * The formatters package defines path formatters - essentially, something which,
 * when given a download, can generate a string indicating what it is.
 * 
 * The path formatter provides various methods - like what components it is made up
 * of (in future, path formatters could be made up of multiple path formatters, so
 * something like "${category}/${tracker}"). It also includes methods to define what
 * listeners it needs.
 * 
 * There are no concrete implementations in the package - there's one interface and
 * one abstract class which does most of the work. Implementations are in the datasource
 * package.
 * 
 * The datasource package contains implementations of path formatters - based on
 * individual components (so grabbing the category or tracker). This package is only
 * intended for atomic path formatter instances - when path formatters are added which
 * support multiple path formatter stuff put together, then that will be defined
 * elsewhere (probably in the formatters package).
 * 
 * The listeners package defines an interface (DLListener) which, if a path formatter
 * class implements, means that when it is used, it needs to register itself with a
 * download. This is used, for example, when a path formatter which uses the category,
 * attribute, finds out that the attribute has been modified.
 * 
 * When the path template, or some of the source data for the path template changes
 * for a download, the path updater gets called - this does the appropriate things of
 * registering / unregistering listeners and recalculating paths, as well as (and most
 * importantly) setting the attribute on the download object itself.
 * 
 * The profiles package is something which is quite simple at the moment, but will
 * grow as it becomes more customisable. A profile represents a path formatter, but
 * contains other attributes related to the path formatter's representation in the UI
 * (such as what name should be used when displaying it, "Manual", "Category" etc)
 * 
 * The profile manager creates the default profiles and provides some API methods to get
 * them.
 * 
 * The UI package is just for UI related features. TableColumns provides two columns,
 * the calculated save path and the name of the path formatter profile. DownloadContextMenu
 * generates the context menus.
 * 
 * The ProfileContextMenuListener class is used to handle each menu item which relates to a
 * particular profile. This is the class which does the fancy "Category - Anime" menu display.
 * It also tells the path to be updated upon selection.
 */


/**
 * 
 */
public class SavePathPlugin implements Plugin {
	
	private SavePathCore core;

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize(final PluginInterface plugin_interface)
			throws PluginException {
		
		this.core = new SavePathCore(this, plugin_interface);
		this.core.addExternalRefs();
		this.core.addInternalRefs();
		
		new DownloadContextMenu(this.core).addMenus();
		this.core.table_columns.createColumns();
		
		// Create config model.
		String[][] profiles = this.core.profile_manager.getProfileSelectionData();
		BasicPluginConfigModel model = plugin_interface.getUIManager().createBasicPluginConfigModel("azsavepath");
		model.addLabelParameter2("azsavepath.config.description");
		model.addHyperlinkParameter2("azsavepath.config.url.description", this.core.lu.getLocalisedMessageText("azsavepath.config.url.link"));
		model.addStringListParameter2("default_profile", "azsavepath.config.default_profile", profiles[0], profiles[1], profiles[0][0]);
		
		// Add listener to set default profile on download add.
		plugin_interface.getDownloadManager().addListener(new DownloadManagerListener() {
			public void downloadAdded(Download d) {
				String path_template = core.plugin_interface.getPluginconfig().getPluginStringParameter("default_profile");
				core.path_updater.updatePathTemplate(d, path_template);
			}
			public void downloadRemoved(Download d) {}
		}, false);
		
	}
}
