/*
 * Created on 8 nov. 2003
 *
 */
package example.torrentlister;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * A plugin example for Azureus
 * This Plugin lists all torrent files from a given directory.
 * It shows how to create a simple Plugin View, how to deal with SWT thread access, and how to load a torrent in azureus. 
 * 
 *  @author Olivier
 *
 */
public class Plugin implements org.gudy.azureus2.plugins.Plugin {

	private PluginInterface pluginInterface;
	
	/**
	 * This method is called when the plugin is loaded / initialized
	 * In our case, it'll simply store the pluginInterface reference
	 * and register our PluginView
	 */
	public void initialize(PluginInterface pluginInterface) {
		this.pluginInterface = pluginInterface;
		this.pluginInterface.addView(new View(pluginInterface));
	}
}
