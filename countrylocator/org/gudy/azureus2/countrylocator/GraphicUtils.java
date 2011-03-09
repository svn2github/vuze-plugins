/**
 * 
 */
package org.gudy.azureus2.countrylocator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;

/**
 * @author TuxPaper
 * @created Nov 14, 2005
 *
 */
public class GraphicUtils
{
	public static Image getImage(Graphic graphic) {
		Image im = null;
		if (graphic instanceof UISWTGraphic)
			im = ((UISWTGraphic) graphic).getImage();
		return im;
	}

	public static Graphic createGraphic(Image im) {
		return CountryLocator.swtInstance.createGraphic(im);
	}
	
	/* methods for the map view */
	/** encode RGB color into an int */
	public static int rgb2int(RGB rgb) {
		return (rgb.red<<16) + (rgb.green<<8) + rgb.blue;
	}
	
	/** decode a RGB color from an int */
	public static RGB int2rgb(int val) {
		return new RGB((val<<8)>>>24, (val<<16)>>>24, (val<<24)>>>24);
	}

}
