/*
 * Created on Aug 4, 2005
 * Created by omschaub
 * 
 */
package omschaub.firefrog.main;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class CustomProgressBar {
    private NumberFormat longPercentFormat;
    private RGB defaultBackgroundRGB, defaultProgressRGB, defaultTextRGB, defaultBorderRGB; 
    private Display display;
    private Image barImage, backgroundImage, coloredBarImage, coloredBackgroundImage;
    private Color backgoundColor, progressColor, textColor, borderColor;
    
    public Image paintProgressBar(Label cell, int width, int height, Integer completed, Display sentDisplay, boolean isRelief) {
        display = sentDisplay;
        barImage = ImageRepository.getImage("barImage");
        backgroundImage = ImageRepository.getImage("backgroundImage");
        
        this.longPercentFormat = new DecimalFormat("##0.0");
        initDefaultRGBs();
        Image image = this.getImage(cell, width, height);
        GC gc = new GC(image);
        
        this.paintBackground(width, height, gc, isRelief);        
        this.paintProgress(width, height, completed, gc,isRelief);        
        this.paintPercent(width, height, completed, gc);
        this.paintBorder(width, height, gc);
        
        gc.dispose();
        return image;
        
    }
    
    private void initDefaultRGBs() {
        /*this.defaultBackgroundRGB = new RGB(139, 159, 160);
        this.defaultProgressRGB = new RGB(87, 118, 123);
        this.defaultTextRGB = new RGB(36, 111, 134); 
        this.defaultBorderRGB = this.defaultTextRGB; */       
        this.defaultBackgroundRGB = new RGB(214, 235, 255); //210, 16, 100 (hsv)
        this.defaultProgressRGB = new RGB(128, 191, 255);//210, 50, 500 (hsv)
        this.defaultTextRGB = new RGB(0, 64, 128); //210, 100, 50 (hsv)
        this.defaultBorderRGB = this.defaultTextRGB; 
    }
    
    private RGB getBackgroundRGB() {
       
        return this.defaultBackgroundRGB;
    }
    
    private RGB getProgressRGB() {
        
        return this.defaultProgressRGB;
    }
    
    private RGB getTextRGB() {
        
        return this.defaultTextRGB;
    }
    
    private RGB getBorderRGB() {
        
        return this.defaultBorderRGB;
    }
    public Color getBackgroundColor() {
        if (this.backgoundColor == null) {
            this.backgoundColor = new Color(display, this.getBackgroundRGB());
        }
        return this.backgoundColor;
    }
    
    public Color getProgressColor() {
        if (this.progressColor == null) {
            this.progressColor = new Color(display, this.getProgressRGB());
        }
        return this.progressColor;
    }
    
    public Color getBorderColor() {
        if (this.borderColor == null) {
            this.borderColor = new Color(display, this.getBorderRGB());
        }
        return this.borderColor;
    }
    
    public Color getTextColor() {
        if (this.textColor == null) {
            this.textColor = new Color(display, this.getTextRGB());
        }
        return this.textColor;
    }
    private void paintBackground(int imageWidth, int imageHeight, GC gc, boolean isRelief) {
        int heightToPaint = imageHeight - 2;
        int widthToPaint = imageWidth - 2;
        if (!isRelief) {
            gc.setBackground(new Color(display, defaultBackgroundRGB));
            gc.fillRectangle(1, 1, widthToPaint, heightToPaint);
        } else {
            Image background = this.getColoredBackground();
            int srcHeight = background.getImageData().height;
            gc.drawImage(background, 0, 0, 1, srcHeight, 1, 1, widthToPaint, heightToPaint);
        }
    }
    
    private void paintProgress(int imageWidth, int imageHeight, Integer completed, GC gc, boolean isRelief) {
        if (!isRelief) {
            this.paintSolidProgress(imageWidth, imageHeight, completed, gc);
        } else {
            this.paintReliefProgress(imageWidth, imageHeight, completed, gc);
        }
    }
    
    private void paintSolidProgress(int imageWidth, int imageHeight, Integer completed, GC gc) {
        int widthToPaint = getWidthToPaint(completed, imageWidth);
        gc.setBackground(new Color(display, defaultProgressRGB));                
        gc.fillRectangle(1, 1, widthToPaint, imageHeight - 2);
    }
    
    private void paintPercent(int imageWidth, int imageHeight, Integer completed, GC gc) {
        gc.setForeground(new Color(display, defaultTextRGB));
        String percent = this.longPercentFormat.format(completed.intValue() / 10.0f) + " %";
        Point extent = gc.stringExtent(percent);
        if (extent.x <= imageWidth) {
            this.paintString(imageWidth, imageHeight, percent, extent, gc, true);
        } else {
            percent = completed.toString() + " %";
            extent = gc.stringExtent(percent);
            if (extent.x <= imageWidth) {
                this.paintString(imageWidth, imageHeight, percent, extent, gc, true);
            } else {
                percent = completed.toString();
                extent = gc.stringExtent(percent);
                this.paintString(imageWidth, imageHeight, percent, extent, gc, false);
            }
        }
    }
    
    private void paintString(int imageWidth, int imageHeight, String percent, Point extent, GC gc, boolean isPaintPercent) {        
        if (isPaintPercent) { 
            int x = (imageWidth - extent.x + 1) / 2;
            int y = (imageHeight - extent.y + 1) / 2;
            gc.drawString(percent, x, y, true);
        }
    }
    
    


    private int getWidthToPaint(Integer completed, int imageWidth) {
        float precentComplete = completed.intValue() / 1000.0f;
        int widthToPaint = (int) ((imageWidth - 2) * precentComplete);
        return widthToPaint;
    }
    
    
    
    private void paintReliefProgress(int imageWidth, int imageHeight, Integer completed, GC gc) {
        int widthToPaint = getWidthToPaint(completed, imageWidth);
        Image bar = this.getColoredBar();
        if (widthToPaint > 0) {
            this.paintSlice(2, widthToPaint, 1, imageHeight, bar, gc); //end
            this.paintSlice(0, 1, 1, imageHeight, bar, gc); //beginning
            this.paintSlice(1, 2, widthToPaint - 2, imageHeight, bar, gc); //middle
        }
    }
    
    private void paintSlice(int srcX, int destX, int width, int imageHeight, Image pattern, GC gc) {
        if (width > 0) {
            int srcHeight = pattern.getImageData().height;
            gc.drawImage(pattern, srcX,             1, 1, srcHeight - 2, destX,               2, width, imageHeight - 4); //middle
            gc.drawImage(pattern, srcX,             0, 1,             1, destX,               1, width,               1); //top
            gc.drawImage(pattern, srcX, srcHeight - 1, 1,             1, destX, imageHeight - 2, width,               1); //botton
        }
    }
    

    
    private void paintBorder(int imageWidth, int imageHeight, GC gc) {
        gc.setForeground(new Color(display, defaultBorderRGB));
        gc.drawRectangle(0, 0, imageWidth - 1, imageHeight - 1);
    }

    private Image getImage(Label cell, int width, int height) {
            Image oldImage = cell.getImage();
            if (oldImage != null && !oldImage.isDisposed()) {
                Rectangle oldBounds =  oldImage.getBounds();
                if (oldBounds.width != width || oldBounds.height != height) {
                    oldImage.dispose();
                    return this.createImage(width, height);
                }
                return oldImage;
            }
        
        return this.createImage(width, height);
    }
    
    private Image createImage(int width, int height) {
        return new Image(display, width, height);
    }
    
    public Image getColoredBar() {
        if (this.coloredBarImage == null) {                                               
            ImageData imageData = this.tintImageData(this.barImage.getImageData(), getProgressColor());
            this.coloredBarImage = new Image(display, imageData);
        }
        return this.coloredBarImage;
    }
    
    public Image getColoredBackground() {
        if (this.coloredBackgroundImage == null) {                                               
            ImageData imageData = this.tintImageData(this.backgroundImage.getImageData(), this.getBackgroundColor());
            this.coloredBackgroundImage = new Image(display, imageData);
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
    
    

}//EOF
