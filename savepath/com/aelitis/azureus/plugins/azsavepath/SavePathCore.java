/**
 * File: SavePathCore.java
 * Library: Save Path plugin for Azureus
 * Date: 17 Nov 2006
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

import com.aelitis.azureus.plugins.azsavepath.parsers.PathFormatterCache;
import com.aelitis.azureus.plugins.azsavepath.profiles.ProfileManager;
import com.aelitis.azureus.plugins.azsavepath.ui.TableColumns;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

/**
 * 
 */
public class SavePathCore {

	public SavePathPlugin plugin = null;
	public PluginInterface plugin_interface;
	
	public SavePathCore(SavePathPlugin plugin, PluginInterface plugin_interface) {
		this.plugin = plugin;
		this.plugin_interface = plugin_interface;
	}
	
	public TorrentAttribute path_attr = null;
	public TorrentAttribute template_attr = null;
	public TorrentAttribute custom_path_attr = null;
	public LoggerChannel logger_channel = null;
	public TableColumns table_columns = null;
	public LocaleUtilities lu = null;
	
	public void addExternalRefs() {
		TorrentManager tm = plugin_interface.getTorrentManager();
		this.path_attr = tm.getAttribute(TorrentAttribute.TA_RELATIVE_SAVE_PATH);
		this.template_attr = tm.getPluginAttribute("PathTemplate");
		this.custom_path_attr = tm.getPluginAttribute("UserDefinedPath");
		this.logger_channel = plugin_interface.getLogger().getChannel("SavePathPlugin");
		this.lu = plugin_interface.getUtilities().getLocaleUtilities();
		
		// We'll take this opportunity to set this value if it isn't set,
		// otherwise the plugin won't work properly.
		if (!plugin_interface.getPluginconfig().getUnsafeBooleanParameter("File.move.subdir_is_default")) {
			plugin_interface.getPluginconfig().setUnsafeBooleanParameter("File.move.subdir_is_default", true);
			
			// Inform the user that we've changed it.
			logger_channel.logAlert(
				LoggerChannel.LT_INFORMATION,
				lu.getLocalisedMessageText("azsavepath.useralert.default_subdir")
			);
		}
	}

	public PathUpdater path_updater = null;
	public ProfileManager profile_manager = null;
	public PathFormatterCache path_formatter_cache = null;

	public void addInternalRefs() {
		this.path_updater = new PathUpdater(this);
		this.profile_manager = new ProfileManager(this);
		this.path_formatter_cache = new PathFormatterCache(this);
		this.table_columns = new TableColumns(this);
	}
	
}
