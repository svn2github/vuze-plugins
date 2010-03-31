/**
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
package org.gudy.azureus2.countrylocator;

import java.util.Locale;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.Plugin;

/**
 * This class is called by Azureus.
 * 
 * All the country locator code has been moved to CountryLocator.java, so that
 * when AZPlugin gets initialized, there are no class dependencies (other than
 * Plugin and PluginException).  Even if Azureus plugin API ever removes 
 * classes, or if the user is using an old version where some classes are 
 * missing, this class will still initialize.
 * 
 * @author TuxPaper
 * @created Oct 18, 2005
 *
 * TODO: Make this a UnloadablePlugin.  Requires a TableManager.removeColumn()
 *       method first..
 */
public class AZPlugin implements Plugin
{
	public static final int IMAGESIZE_SMALL = 0;

	public static final int IMAGESIZE_BIG = 1;

	private CountryLocator plugin = null;

	public void initialize(PluginInterface pluginInterface)
		throws PluginException
	{
		try {
			plugin = new CountryLocator();
			plugin.initialize(pluginInterface);
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new PluginException("Bah", e);
		}

	}

	/**
	 * Returns the IP's Country name in the locale specified
	 *  
	 * @param sIP
	 * @param inLocale
	 * @return Country Name
	 */
	public String getIPCountry(String sIP, Locale inLocale) {
		if (plugin == null)
			return "";

		return plugin.getIPCountry(sIP, inLocale);
	}

	/**
	 * Returns 2 letter ISO 3166 Country code for provided IP address
	 * 
	 * @param sIP IP address to get Country Code for
	 * @return ISO 3166 code, or "" if not found
	 */
	public String getIPISO3166(String sIP) {
		if (plugin == null)
			return "";

		return plugin.getIPISO3166(sIP);
	}

	/**
	 * Return the location of the country images.  Country images are stored
	 * as 2 letter lowercase country codes + ".png".
	 * 
	 * @param imagesize IMAGESIZE_SMALL or IMAGESIZE_BIG
	 * @return location
	 */
	public String getImageLocation(int imagesize) {
		if (imagesize == IMAGESIZE_SMALL)
			return CountryLocator.FLAGS_LOC + CountryLocator.FLAGS_SMALL;
		return CountryLocator.FLAGS_LOC + CountryLocator.FLAGS_BIG;
	}
}
