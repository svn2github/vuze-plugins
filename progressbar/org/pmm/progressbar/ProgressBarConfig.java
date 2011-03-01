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

import java.io.InputStream;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * @author kutos
 * The configuration of the Progress Bar
 */
public class ProgressBarConfig {
    private static final String LABEL_PROGRESSBAR = "Progressbar";
    
    private static final String CONFIG_BACKGROUND_COLOR = "BACKGROUND_COLOR";
    private static final String LABEL_BACKGROUNDCOLOR = LABEL_PROGRESSBAR + ".backgroundcolor";
    
    private static final String CONFIG_PROGRESS_COLOR = "PROGRESS_COLOR";
    private static final String LABEL_PROEGRESSCOLOR = LABEL_PROGRESSBAR + ".progresscolor";
    
    private static final String CONFIG_TEXT_COLOR = "TEXT_COLOR";
    private static final String LABEL_TEXTGROUNDCOLOR = LABEL_PROGRESSBAR + ".textcolor";
    
    private static final String CONFIG_BORDER_COLOR = "BORDER_COLOR";
    private static final String LABEL_BORDERCOLOR = LABEL_PROGRESSBAR + ".bordercolor";
    
    private static final String CONFIG_RELIEF = "RELIEF";
    private static final String LABEL_RELIEF = LABEL_PROGRESSBAR + ".relief";
    
    private static final String CONFIG_PERCENT = "PAINT_PERCENT";
    private static final String LABEL_PERCENT = LABEL_PROGRESSBAR + ".percent";
    
    private RGB defaultBackgroundRGB, defaultProgressRGB, defaultTextRGB, defaultBorderRGB;
    
    private RGB backgroundRGB, progressRGB, textRGB, borderRGB;
    
    private Color backgoundColor, progressColor, textColor, borderColor;
    
    private UISWTInstance swtManager;
    
    private PluginConfig pluginConfig;
    
    private TableColumn column;
    
    private static final String BAR = "org/pmm/progressbar/images/bar.png";
    
    private static final String BACKGRROUNG = "org/pmm/progressbar/images/background.png";
    
    private Image barImage, backgroundImage, coloredBarImage, coloredBackgroundImage;
    
    public ProgressBarConfig(PluginInterface pluginInterface, UISWTInstance swtManager, TableColumn column) {
        this.swtManager = swtManager;
        this.pluginConfig = pluginInterface.getPluginconfig();
        this.column = column;
        this.initDefaultRGBs();
        this.createConfigSection(pluginInterface);        
        try {this.loadImages();}
        catch (PluginException e) {
        	pluginInterface.getLogger().getNullChannel("progress bar").logAlert("Error loading images.", e);
        }
    }

    private void loadImages() throws PluginException {
        this.barImage = this.loadImage(BAR);
        this.backgroundImage = this.loadImage(BACKGRROUNG);
        if (this.barImage == null || this.backgroundImage == null) {
            throw new PluginException("could not load relief images");
        }
    }
    
    private void initDefaultRGBs() {
        this.defaultBackgroundRGB = new RGB(214, 235, 255); //210, 16, 100 (hsv)
        this.defaultProgressRGB = new RGB(128, 191, 255);//210, 50, 500 (hsv)
        this.defaultTextRGB = new RGB(0, 64, 128); //210, 100, 50 (hsv)
        this.defaultBorderRGB = this.defaultTextRGB;        
    }
    
    private RGB getBackgroundRGB() {
        if (this.backgroundRGB == null) {
            this.backgroundRGB = this.getConfigRGB(CONFIG_BACKGROUND_COLOR, this.defaultBackgroundRGB);
        }
        return this.backgroundRGB;
    }
    
    private RGB getProgressRGB() {
        if (this.progressRGB == null) {
            this.progressRGB = this.getConfigRGB(CONFIG_PROGRESS_COLOR, this.defaultProgressRGB);
        }
        return this.progressRGB;
    }
    
    private RGB getTextRGB() {
        if (this.textRGB == null) {
            this.textRGB = this.getConfigRGB(CONFIG_TEXT_COLOR, this.defaultTextRGB);
        }
        return this.textRGB;
    }
    
    private RGB getBorderRGB() {
        if (this.borderRGB == null) {
            this.borderRGB = this.getConfigRGB(CONFIG_BORDER_COLOR, this.defaultBorderRGB);
        }
        return this.borderRGB;
    }
    
    public Color getBackgroundColor() {
        if (this.backgoundColor == null) {
            this.backgoundColor = new Color(this.swtManager.getDisplay(), this.getBackgroundRGB());
        }
        return this.backgoundColor;
    }
    
    public Color getProgressColor() {
        if (this.progressColor == null) {
            this.progressColor = new Color(this.swtManager.getDisplay(), this.getProgressRGB());
        }
        return this.progressColor;
    }
    
    public Color getBorderColor() {
        if (this.borderColor == null) {
            this.borderColor = new Color(this.swtManager.getDisplay(), this.getBorderRGB());
        }
        return this.borderColor;
    }
    
    public Color getTextColor() {
        if (this.textColor == null) {
            this.textColor = new Color(this.swtManager.getDisplay(), this.getTextRGB());
        }
        return this.textColor;
    }
    
    private void createConfigSection(PluginInterface pluginInterface) {
        PluginConfigUIFactory factory = pluginInterface.getPluginConfigUIFactory();        
        
        Parameter backgroundColor = factory.createColorParameter(CONFIG_BACKGROUND_COLOR, LABEL_BACKGROUNDCOLOR, this.defaultBackgroundRGB.red, this.defaultBackgroundRGB.green, this.defaultBackgroundRGB.blue);                        
        Parameter progressColor = factory.createColorParameter(CONFIG_PROGRESS_COLOR, LABEL_PROEGRESSCOLOR, this.defaultProgressRGB.red, this.defaultProgressRGB.green, this.defaultProgressRGB.blue);        
        Parameter textColor = factory.createColorParameter(CONFIG_TEXT_COLOR, LABEL_TEXTGROUNDCOLOR, this.defaultTextRGB.red, this.defaultTextRGB.green, this.defaultTextRGB.blue);        
        Parameter borderColor = factory.createColorParameter(CONFIG_BORDER_COLOR, LABEL_BORDERCOLOR, this.defaultBorderRGB.red, this.defaultBorderRGB.green, this.defaultBorderRGB.blue);
        Parameter isPaintRelief = factory.createBooleanParameter(CONFIG_RELIEF, LABEL_RELIEF, true);
        Parameter isPaintPercent = factory.createBooleanParameter(CONFIG_PERCENT, LABEL_PERCENT, true);
        
        Parameter[] parameters = new Parameter[] {
                backgroundColor,
                progressColor,
                textColor,
                borderColor,
                isPaintRelief,
                isPaintPercent};
        ColorUpdater colorUpdater = new ColorUpdater();
        for (int i = 0; i < parameters.length - 2; ++i) {
            parameters[i].addConfigParameterListener(colorUpdater);
        }
        CellInvalidator cellInvalidator = new CellInvalidator();
        isPaintRelief.addConfigParameterListener(cellInvalidator);
        isPaintPercent.addConfigParameterListener(cellInvalidator);
        
        pluginInterface.addConfigUIParameters(parameters, LABEL_PROGRESSBAR);
    }
    
    private RGB getConfigRGB(String key, RGB defaultRGB) {
        int red = this.pluginConfig.getPluginIntParameter(key + ".red", defaultRGB.red);
        int green = this.pluginConfig.getPluginIntParameter(key + ".green", defaultRGB.blue);
        int blue = this.pluginConfig.getPluginIntParameter(key + ".blue", defaultRGB.green);
        return new RGB(red, green, blue);
    }
    
    protected void colorsChanged() {
        this.backgroundRGB = null;
        this.progressRGB = null;
        this.textRGB = null;
        this.borderRGB = null;
        
        this.disposeColors();
        this.disposeColoredImages();
        
        this.column.invalidateCells();
    }
    
    protected void invalidateCells() {
        this.column.invalidateCells();
    }

    private void disposeColors() {
        //XXX use reflection damn it!
        if (this.backgoundColor != null && !this.backgoundColor.isDisposed()) {
            this.backgoundColor.dispose();            
        }
        this.backgoundColor = null;
        
        if (this.progressColor != null && !this.progressColor.isDisposed()) {
            this.progressColor.dispose();            
        }
        this.progressColor = null;
        
        if (this.textColor != null && !this.textColor.isDisposed()) {
            this.textColor.dispose();            
        }
        this.textColor = null;
        
        if (this.borderColor != null && !this.borderColor.isDisposed()) {
            this.borderColor.dispose();            
        }
        this.borderColor = null;        
    }
    
    private void disposeColoredImages() {
//      XXX use reflection damn it!
        if (this.coloredBackgroundImage != null && !this.coloredBackgroundImage.isDisposed()) {
            this.coloredBackgroundImage.dispose();
        }
        this.coloredBackgroundImage = null;
        
        if (this.coloredBarImage != null && !this.coloredBarImage.isDisposed()) {
            this.coloredBarImage.dispose();
        }
        this.coloredBarImage = null;
    }
    
    private void disposeOriginalImages() {
//      XXX use reflection damn it!
        if (this.backgroundImage != null && !this.backgroundImage.isDisposed()) {
            this.backgroundImage.dispose();
        }
        this.backgroundImage = null;
        
        if (this.barImage != null && !this.barImage.isDisposed()) {
            this.barImage.dispose();
        }
        this.barImage = null;        
    }
    
    protected void finalize() throws Throwable {
        this.disposeColors();
        this.disposeColoredImages();
        this.disposeOriginalImages();
    }
    
    public boolean isPaintRelief() {
        //return this.pluginConfig.getBooleanParameter(CONFIG_RELIEF, true);
        // XXX have to prefix
        return this.pluginConfig.getUnsafeBooleanParameter("Plugin.progressbar." + CONFIG_RELIEF, true);
    }
    
    public boolean isPaintPercent() {
        //return this.pluginConfig.getBooleanParameter(CONFIG_PERCENT, true);
        // XXX have to prefix
        return this.pluginConfig.getUnsafeBooleanParameter("Plugin.progressbar." + CONFIG_PERCENT, true);        
    }
    
    public Image getColoredBar() {
        if (this.coloredBarImage == null) {                                               
            ImageData imageData = this.tintImageData(this.barImage.getImageData(), this.getProgressColor());
            this.coloredBarImage = new Image(this.swtManager.getDisplay(), imageData);
        }
        return this.coloredBarImage;
    }
    
    public Image getColoredBackground() {
        if (this.coloredBackgroundImage == null) {                                               
            ImageData imageData = this.tintImageData(this.backgroundImage.getImageData(), this.getBackgroundColor());
            this.coloredBackgroundImage = new Image(this.swtManager.getDisplay(), imageData);
        }
        return this.coloredBackgroundImage;
    }


    private ImageData tintImageData(ImageData imageData, Color color) {        
        PaletteData palette = imageData.palette;
        imageData = (ImageData) imageData.clone();
        
        if (palette.isDirect) {
            this.tintDirectImageData(imageData, color, palette);            
        } else {
            this.tintIndexedImageData(imageData, color, palette);
        }
        return imageData;
    }

    private void tintIndexedImageData(ImageData imageData, Color color, PaletteData palette) {
        float divisor = 1f / 255f;
        RGB[] rgbs = imageData.getRGBs();
        for (int x = 0; x < imageData.width; ++x) {
            for (int y = 0; y < imageData.height; ++y) {
                int pixel = imageData.getPixel(x, y);
                RGB rgb = rgbs[pixel];
                
                int red = this.multAndRound(rgb.red, color.getRed(), divisor);
                int green = this.multAndRound(rgb.green, color.getGreen(), divisor);
                int blue = this.multAndRound(rgb.blue, color.getBlue(), divisor);
                
                rgb.red = red;
                rgb.green = green;
                rgb.blue = blue;
                
                pixel = palette.getPixel(rgb);
                imageData.setPixel(x, y, pixel);
            }
        }
    }

    private void tintDirectImageData(ImageData imageData, Color color, PaletteData palette) {
        float divisor = 1f / 255f;
        int redMask = palette.redMask;
        int redShift = palette.redShift;
        int greenMask = palette.greenMask;
        int greenShift = palette.greenShift;
        int blueMask = palette.blueMask;
        int blueShift = palette.blueShift;
        for (int x = 0; x < imageData.width; ++x) {
            for (int y = 0; y < imageData.height; ++y) {
                int pixel = imageData.getPixel(x, y);
                int red = this.decodeColor(pixel, redMask, redShift);
                int green = this.decodeColor(pixel, greenMask, greenShift);
                int blue = this.decodeColor(pixel, blueMask, blueShift);
                
                red = this.multAndRound(red, color.getRed(), divisor);
                green = this.multAndRound(green, color.getGreen(), divisor);
                blue = this.multAndRound(blue, color.getBlue(), divisor);
                
                red = this.encodeColor(red, redMask, redShift);
                green = this.encodeColor(green, greenMask, greenShift);
                blue = this.encodeColor(blue, blueMask, blueShift);
                pixel = red | green | blue;
                imageData.setPixel(x, y, pixel);
            }
        }
    }
    
    private int decodeColor(int pixel, int mask, int shift) {
        int color = pixel & mask;
        color = (shift < 0) ? color >>> -shift : color << shift;
        return color;
    }
    
    private int encodeColor(int color, int mask, int shift) {
        int pixel = (shift < 0) ? color << -shift : color >> shift;
        pixel &= mask;
        return pixel;
    }
    
    private int multAndRound(int color1, int color2, float divisor) {
        int color = color1 * color2;
        return Math.round(color * divisor);
    }
    
    private Image loadImage(String res) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(res);
        if (is != null) {
            ImageData imageData = new ImageData(is);
            return new Image(this.swtManager.getDisplay(), imageData);
        }
        return null;
    }
    
    private class ColorUpdater implements  ConfigParameterListener {
        public void configParameterChanged(ConfigParameter parameter) {
            ProgressBarConfig.this.colorsChanged();
        }
    }
    
    private class CellInvalidator implements  ConfigParameterListener {
        public void configParameterChanged(ConfigParameter parameter) {
            ProgressBarConfig.this.invalidateCells();
        }
    }
}
