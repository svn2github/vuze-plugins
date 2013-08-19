/*
 * AMP - Azureus Monitoring Plugin
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

package org.pmm.monitoringplugin.graphic;

import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.Formatters;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.pmm.monitoringplugin.graphic.providers.MemoryUsageProvider;
import org.pmm.monitoringplugin.util.AverageIterator;

/**
 * @author kustos
 *
 * Paints a graphic on an image.
 */
public class ChartImage {
    private static final String INITIAL = "Initial";
    private static final String USED = "Used";
    private static final String COMMITED = "Commited";
    private static final String MAX = "Max";
    private String[] keys;    
    
    private Image image;
    private Device device;
    
    private int width, height;
    
    private Color black, blue, red, green, white, lightBlue, darkBlue, grey;
    
    private Map<String, Color> lineColors, barColors, averageColors;
    
    private GraphDataSource source;
    
    private int borderWidth;
    
    private int scaleWidth;
    
    private int labelWidth;
    
    private MemoryUsageProvider usageProvider;
    
    private Map<String, Integer> lastValues, lastAverages;
    
    private Background background;
    
    private PluginInterface pluginInterface;
    
    private int halfAverageDelay = 4;
    
    private int averageDelay = this.halfAverageDelay * 2 + 1;
        
    public ChartImage(Device device, MemoryUsageProvider usageProvider, PluginInterface pluginInterface) {        
        this.device = device;
        this.usageProvider = usageProvider;
        this.pluginInterface = pluginInterface;
        this.borderWidth = 1;
        this.scaleWidth = 68 + 1;
        this.labelWidth = 5;
        this.source = new GraphDataSource(this.borderWidth, 5, Display.getCurrent().getClientArea().width);
        //this.keys = new String[] {INITIAL, USED, COMMITED, MAX};
        this.keys = new String[] {USED, COMMITED, MAX};
        this.lastValues = new HashMap<String, Integer>();
        this.lastAverages = new HashMap<String, Integer>();
        this.initializeColors();
        this.background = new Background(device);
        
        this.refresh(false);
    }
    
    public Composite createLegendComposite(Composite parent) {
        // copy pasted from {@link rg.gudy.azureus2.ui.swt.views.TableView#createLegendComposite(Color[],String[]}        
        
        Composite legend = new Composite(parent,SWT.NULL);
        legend.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));        
        
        GridLayout layout = new GridLayout();
        int numColumns = this.keys.length * 2;
        if(numColumns > 10) numColumns = 10;
        layout.numColumns = numColumns;
        legend.setLayout(layout);
        GridData data;
        
        for (String nextKey : this.keys) {
            Label lblColor = new Label(legend,SWT.BORDER);
            lblColor.setBackground(this.lineColors.get(nextKey));
            data = new GridData();
            data.widthHint = 20;
            data.heightHint = 10;
            lblColor.setLayoutData(data);
            
            Label lblDesc = new Label(legend,SWT.NULL);
            lblDesc.setText(nextKey);
            data = new GridData();
            data.widthHint = 150;
            lblDesc.setLayoutData(data);
        }
        return legend;        
    }
    
    public void refresh() {
        this.refresh(true);
    }
    
    private void refresh(boolean updateImage) {
        MemoryUsage memoryUsage = this.usageProvider.getMemoryUsage();
        
        int max = this.source.addValue(MAX, memoryUsage.getMax());
        int commited = this.source.addValue(COMMITED, memoryUsage.getCommitted());
        int used = this.source.addValue(USED, memoryUsage.getUsed());
        int initial = this.source.addValue(INITIAL, memoryUsage.getInit());                
        
        if (updateImage) {
            if (!this.checkBuffer() || this.source.needsRescale()) {
                this.paintFromScratch();
            } else {
                this.paintUpdate(initial, used, commited, max);
            }
        }
    }
    
    public Image getImage() {
        if (!this.checkBuffer() || this.source.needsRescale()) {
            this.paintFromScratch();
        }
        return this.image;
    }
    
    private void paintFromScratch() {
        GC gc = new GC(this.image);
        this.paintBackground(gc);
        this.paintBackgroundScale(gc, this.borderWidth, true);
        this.paintBorder(gc);
        this.paintScale(gc);
        
        this.paintGraphLine(gc, MAX);
        this.paintGraphLine(gc, COMMITED);
        this.paintGraphGradient(gc, USED);
        this.paintGraphLine(gc, USED, true);
//        this.paintGraph(gc, INITIAL);
        gc.dispose();
    }
    
    private void paintScale(GC gc) {
        gc.setForeground(this.black);
        int x = this.width - this.borderWidth - this.scaleWidth;
        gc.drawLine(x, this.borderWidth, x, this.height - this.borderWidth);
    }
    
    private void paintUpdate(int initial, int used, int commited, int max) {
        GC gc = new GC(this.image);
        
        this.moveGraphic(gc);
        int x = this.getStartX();
        this.paintBrackgroundLine(gc, x);
//        this.paintBackgroundLegend(gc);
        this.paintBackgroundScale(gc, x, false);        
        
        this.paintUpdateLine(gc, MAX, max);
        
        this.paintUpdateLine(gc, COMMITED, commited);
        
        this.paintUpdateGradient(gc, USED, used);
        this.paintAverage(gc, USED);
        
//        this.paintUpdateLine(gc, INITIAL, initial);
        
        gc.dispose();
    }
    
    /* Not used, so commented out.
    private void paintUpdateBar(GC gc, String key, int value) {        
        int x = this.getStartX();
        
        gc.setForeground(this.barColors.get(key));
        gc.drawLine(x, this.transform(value), x, this.getMaxDrawableY());
        
//        this.paintLegend(gc, key, value);        
//        this.lastValues.put(key, new Integer(value));
    }*/
    
    private void paintUpdateGradient(GC gc, String key, int x, int value) {                
        gc.setForeground(this.darkBlue); 
        gc.setBackground(this.lightBlue);
//        int max = this.source.getUnscaledMax();
        
        int transformed = this.transform(value);
        gc.setClipping(
                x,
                transformed,
                1,
                this.getDrawableHeight() - transformed);
        gc.fillGradientRectangle(
                x,
                this.transform(this.source.getScaledMax()),
                1,
                this.getDrawableHeight(),
                true); // fade from froreground to background        
        gc.setClipping(0,0,this.width, this.height); // reset clipping
    }
    
    private void paintUpdateGradient(GC gc, String key, int value) {
        this.paintUpdateGradient(gc, key, this.getStartX(), value);
//      gc.setForeground(this.lineColors.get(key));
//      this.paintLegend(gc, key, value);        
//      this.lastValues.put(key, new Integer(value));
    }
    
    
    private int getMaxDrawableY() {
        return this.height - this.borderWidth;
    }
    
    private int getDrawableHeight() {
        return this.height - (this.borderWidth << 1);
    }
    
    private void paintAverage(GC gc, String key) {
        int value = this.source.getMovingAverage(key, this.averageDelay);
        gc.setForeground(this.averageColors.get(key));        
        Integer oldValue = this.lastAverages.get(key);
        int x = this.getStartX() - this.halfAverageDelay;
        
        gc.setForeground(this.averageColors.get(key));
        if (oldValue != null && this.source.getUnscaledSize(key) >= this.averageDelay) {
            this.drawLine(gc, x - 1, oldValue.intValue(), x, value);
        }
//        this.paintLegend(gc, key, value);        
        
        this.lastAverages.put(key, new Integer(value));
    }
    
    private void paintUpdateLine(GC gc, String key, int value) {
        Integer oldValue = this.lastValues.get(key);
        value = this.borderWidth + value;
        int x = this.getStartX();
        
        gc.setForeground(this.lineColors.get(key));        
        this.drawLine(gc, x - 1, oldValue.intValue(), x, value);
//        this.paintLegend(gc, key, value);
        
        this.lastValues.put(key, new Integer(value));
    }
    
    private void drawLine(GC gc, int x1, int y1, int x2, int y2) {
        gc.drawLine(x1, this.transform(y1), x2, this.transform(y2));
    }
    
    private int transform(int y) {
        return this.getDrawableHeight() -  y + this.borderWidth;
    }
    
    private void paintLegend(GC gc, String key, int y) {
        int x1 = this.getScaleStartX();        
        y = this.transform(y);
        gc.drawLine(x1, y, x1 + this.labelWidth, y);
        
        int stringHeight = gc.stringExtent(key).y;
        gc.drawString(key, x1 + this.labelWidth, y - stringHeight / 2, true);
    }
    
    private void initializeColors() {
        
        int value = 191;
        
        this.black = new Color(this.device,0,0,0);
        
        this.grey = new  Color(this.device,212,212,212);
        
        this.white = new Color(this.device,255,255,255);
        
        this.blue = new Color(this.device, 0, 0, value);
        
        this.red = new Color(this.device, value, 0, 0);
        
        this.green = new Color(this.device, 0, value, 0);
        
        if (Colors.blues[Colors.BLUES_LIGHTEST] != null) {
            this.lightBlue = Colors.blues[Colors.BLUES_LIGHTEST];
        } else {
            this.lightBlue = new Color(this.device, 215, 235, 235);
        }
        
        if (Colors.blues[Colors.BLUES_DARKEST] != null) {
            this.darkBlue = Colors.blues[Colors.BLUES_DARKEST];
        } else {
            this.darkBlue = new Color(this.device, 17, 68, 153);
        }
        
        this.lineColors = new HashMap<String, Color>(4);
        this.lineColors.put(COMMITED, this.blue);
        this.lineColors.put(USED, this.red);
        this.lineColors.put(INITIAL, this.black);
        this.lineColors.put(MAX, this.green);
        
        this.barColors = new HashMap<String, Color>(2);
        this.barColors.put(USED, this.red);
        this.barColors.put(COMMITED, this.grey);
        
        this.averageColors = new HashMap<String, Color>(1);
        this.averageColors.put(USED, this.red);
    }
    
    private void paintGraphGradient(GC gc, String key) {
        int x1, y1, y2;
        
        Iterator<Integer> scaled = this.source.getScaledData(key);
        x1 = this.getStartX();
        
        if (scaled.hasNext()) {
            y1 = scaled.next() + this.borderWidth;
        } else {
            return;
        }
                
        while (scaled.hasNext() && x1 >= this.borderWidth) {
            y2 = scaled.next() + this.borderWidth;
            this.paintUpdateGradient(gc, key, x1, y1);
            y1 = y2;
            x1 -= 1;
        }        
    }
    
    private void paintGraphLine(GC gc, String key, boolean average) {
        int x1, y1, y2;
        Color foreground = average ? this.averageColors.get(key) : this.lineColors.get(key);
        
        Iterator<Integer> scaled = this.source.getScaledData(key);
        if (average) {
            scaled = new AverageIterator(scaled, this.averageDelay);
        }
                
        if (scaled.hasNext()) {
            y1 = scaled.next() + this.borderWidth;
            if (average) {
                this.lastAverages.put(key, new Integer(y1));
            } else { 
                this.lastValues.put(key, new Integer(y1));
            }
        } else {
            return;
        }
        
        x1 = this.getStartX();
        if (average) {
            x1 -= this.halfAverageDelay;
        }
        
        gc.setForeground(foreground);
        
//        this.paintLegend(gc, key, y1);
        while (scaled.hasNext() && x1 >= this.borderWidth) {
            y2 = scaled.next() + this.borderWidth;
            this.drawLine(gc, x1, y1, x1 - 1, y2);
            y1 = y2;
            x1 -= 1;
        }        
    }
    
    private void paintGraphLine(GC gc, String key) {
        this.paintGraphLine(gc, key, false);
    }

    private int getStartX() {
        return this.width - this.scaleWidth - this.borderWidth - 1;
    }
    
    public void dispose() {
        // XXX use reflection damn it!
        if (this.black != Colors.black) {
            this.black.dispose();
        }
        
        if (this.white != Colors.white) {
            this.white.dispose();
        }
        
        if (this.blue != Colors.blue) {
            this.blue.dispose();
        }
        
        if (this.red != Colors.red) {
            this.red.dispose();
        }
        
        this.green.dispose();
        this.grey.dispose();
        
        if (this.lightBlue  != Colors.blues[Colors.BLUES_LIGHTEST]) {
            this.lightBlue.dispose();
        }
        
        if (this.darkBlue != Colors.blues[Colors.BLUES_DARKEST]) {
            this.darkBlue.dispose();
        }        
        
        this.background.dispose();
        this.disposeBuffer();
    }

    private void paintBackground(GC gc) {
        this.background.paintBackground(gc, this);
    }
    
    private void paintBrackgroundLine(GC gc, int x) {
        this.background.paintBackground(gc, this,
                x, this.borderWidth,
                x + 1, this.getMaxDrawableY());
    }
    
    private void paintBackgroundScale(GC gc, int x1, boolean paintLegend) {
        gc.setForeground(this.black);
        long max = this.source.getUnscaledMax();
        long base = this.source.getScalingBase();
        int value;
        if (base > 0) {
            int x2 = this.getStartX() + 1;
            int y;
            for (long current = base; current < max; current += base) {
                value = this.source.scale(current);
                y = this.transform(value);
                gc.drawLine(x1, y, x2, y);
                if (paintLegend) { 
                    this.paintLegend(gc, this.formatBytes(current), value);
                }
            }
        }
    }
    
    /** Not used, so commented out.
    private void paintBackgroundLegend(GC gc) {
        this.background.paintBackground(gc, this,
                this.getScaleStartX(),
                this.borderWidth,
                this.width - this.borderWidth,
                this.getMaxDrawableY());
    }
    */
    
    private int getScaleStartX() {
        int scaleBorderWidth = 1;
        return this.width - this.borderWidth - this.scaleWidth + scaleBorderWidth;
    }
    
    private void paintBorder(GC gc) {
        gc.setForeground(this.black);
        gc.drawRectangle(0, 0, this.width - 1, this.height - 1);
    }
    
    private void moveGraphic(GC gc) {
        int x = this.borderWidth + 1; 
        int y = this.borderWidth + 1;
        int widthToCopy = this.width - this.borderWidth - this.borderWidth - this.scaleWidth - 1;
        int heightToCopy = this.getDrawableHeight();
        gc.copyArea(x, y, widthToCopy, heightToCopy, x - 1, y);
    }
    
    private boolean checkBuffer() {
        if (this.image == null) {
            this.image = new Image(this.device, this.width, this.height);
            return false;
        }
        return true;
    }
    
    private void disposeBuffer() {
        if (this.image != null && this.image.isDisposed()) {
            this.image.dispose();
        }
        this.image = null;
    }
    
    private void setWidth(int width) {
        if (this.width != width) {
            this.disposeBuffer();
        }
        this.width = width;
    }
    
    private void setHeight(int height) {
        if (this.height != height) {
            this.disposeBuffer();
        }        
        this.height = height;
        this.source.setPixelHeight(height);        
    }
    
    public void paintInto(GC gc, Canvas canvas) {
        Rectangle bounds = canvas.getClientArea();
        this.setHeight(bounds.height);
        this.setWidth(bounds.width);
        
        gc.drawImage(this.getImage(), bounds.x, bounds.x);
    }

    public int getBorderWidth() {
        return this.borderWidth;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }
    
    private String formatBytes(long bytes) {
        Formatters formaters = this.pluginInterface.getUtilities().getFormatters();
        return formaters.formatByteCountToKiBEtc(bytes);
    }
}
