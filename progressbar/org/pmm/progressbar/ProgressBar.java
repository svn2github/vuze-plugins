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

import java.text.DecimalFormat;
import java.text.NumberFormat;


import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * @author kutos
 * The actual implementation of the Progress Bar
 */

public class ProgressBar implements TableCellRefreshListener, TableCellDisposeListener, TableCellAddedListener {
    
    private UISWTInstance swtManager;
        
    private ProgressBarConfig config;
    
    private NumberFormat longPercentFormat;
    
    public ProgressBar(UISWTInstance swtManager, ProgressBarConfig config) {
        this.swtManager = swtManager;
        this.config = config;
        this.longPercentFormat = new DecimalFormat("##0.0");        
    }
    
    private boolean isPaintRelief() {
        return this.config.isPaintRelief();
    }
    
    private boolean isPaintPercent() {
        return this.config.isPaintPercent();
    }
    
    private Color getBackgroundColor() {
        return this.config.getBackgroundColor();
    }
    
    private Color getProgressColor() {
        return this.config.getProgressColor();
    }
    
    private Color getBorderColor() {
        return this.config.getBorderColor();
    }
    
    private Color getTextColor() {
        return this.config.getTextColor();
    }
    
    /**
     * Triggered when a cell is being added.
     * @param cell the TableCell that is being added
     * @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
     */
    public void refresh(TableCell cell) {
        Object dataSource = cell.getDataSource();
        if (dataSource == null || ! (dataSource instanceof Download)) {
            return; //opps something went wrong
        }        
        Download download = (Download) dataSource;
        
                
        Integer completed = new Integer(download.getStats().getCompleted());
        if (!cell.setSortValue(completed) && cell.isValid()) {
            // if nothing has changed, we do nothing
            return;
        }
        
        int cWidth = cell.getWidth();
        int cHeight = cell.getHeight();
        
        // getWidth and getHeight can return 0 or negatives.  Safeguard against that
        // as well as width or heights less than 3 (or we can't even draw a border)
        if (cWidth <= 2 || cHeight <= 2) {
          return;
        }
        
        this.paintProgressBar(cell, cWidth, cHeight, completed);
    }
    
    private void paintProgressBar(TableCell cell, int width, int height, Integer completed) {
        Image image = this.getImage(cell, width, height);
        GC gc = new GC(image);
        
        this.paintBackground(width, height, gc);        
        this.paintProgress(width, height, completed, gc);        
        this.paintPercent(width, height, completed, gc);
        this.paintBorder(width, height, gc);
        
        gc.dispose();
        
        this.updateGraphic(cell, image);
    }
    
    private void paintPercent(int imageWidth, int imageHeight, Integer completed, GC gc) {
        gc.setForeground(this.getTextColor());
        String percent = this.longPercentFormat.format(completed.intValue() / 10.0f) + " %";
        Point extent = gc.stringExtent(percent);
        if (extent.x <= imageWidth) {
            this.paintString(imageWidth, imageHeight, percent, extent, gc);
        } else {
            percent = completed.toString() + " %";
            extent = gc.stringExtent(percent);
            if (extent.x <= imageWidth) {
                this.paintString(imageWidth, imageHeight, percent, extent, gc);
            } else {
                percent = completed.toString();
                extent = gc.stringExtent(percent);
                this.paintString(imageWidth, imageHeight, percent, extent, gc);
            }
        }
    }
    
    private void paintString(int imageWidth, int imageHeight, String percent, Point extent, GC gc) {        
        if (this.isPaintPercent()) { 
            int x = (imageWidth - extent.x + 1) / 2;
            int y = (imageHeight - extent.y + 1) / 2;
            gc.drawString(percent, x, y, true);
        }
    }
    
    private void paintProgress(int imageWidth, int imageHeight, Integer completed, GC gc) {
        if (!this.isPaintRelief()) {
            this.paintSolidProgress(imageWidth, imageHeight, completed, gc);
        } else {
            this.paintReliefProgress(imageWidth, imageHeight, completed, gc);
        }
    }


    private int getWidthToPaint(Integer completed, int imageWidth) {
        float precentComplete = completed.intValue() / 1000.0f;
        int widthToPaint = (int) ((imageWidth - 2) * precentComplete);
        return widthToPaint;
    }
    
    private void paintSolidProgress(int imageWidth, int imageHeight, Integer completed, GC gc) {
        int widthToPaint = getWidthToPaint(completed, imageWidth);
        gc.setBackground(this.getProgressColor());                
        gc.fillRectangle(1, 1, widthToPaint, imageHeight - 2);
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
    
    private void paintBackground(int imageWidth, int imageHeight, GC gc) {
        int heightToPaint = imageHeight - 2;
        int widthToPaint = imageWidth - 2;
        if (!this.isPaintRelief()) {
            gc.setBackground(this.getBackgroundColor());
            gc.fillRectangle(1, 1, widthToPaint, heightToPaint);
        } else {
            Image background = this.getColoredBackground();
            int srcHeight = background.getImageData().height;
            gc.drawImage(background, 0, 0, 1, srcHeight, 1, 1, widthToPaint, heightToPaint);
        }
    }
    
    private void paintBorder(int imageWidth, int imageHeight, GC gc) {
        gc.setForeground(this.getBorderColor());
        gc.drawRectangle(0, 0, imageWidth - 1, imageHeight - 1);
    }
    
    /**
     * Tell Azureus that we have a new graphic
     * @param cell the cell with a new graphic
     * @param image the new graphic
     */
    private void updateGraphic(TableCell cell, Image image) {
        // First, check to see if there is one
        Graphic g = cell.getGraphic();
        UISWTGraphic graphicSWT;
        if (g == null || !(g instanceof UISWTGraphic)) {
          // There isn't one, so make a new one!
          graphicSWT = this.swtManager.createGraphic(image);
        } else {
          // There's one, so we have to dispose of our last SWT Image
          // (Azureus DOES NOT dispose of it for us, in case we are using it
          //  somewhere else)
          graphicSWT = (UISWTGraphic)g;
          Image oldImage = graphicSWT.getImage();
          graphicSWT.setImage(image);
          if (oldImage != null && oldImage != image && !oldImage.isDisposed())
            oldImage.dispose();
        }
        // Set the graphic.  Even if graphicSWT is the same, we need to call 
        // getGraphic to tell Azureus to redraw it.
        cell.setGraphic(graphicSWT);
    }
    
    private Image getImage(TableCell cell, int width, int height) {
        Graphic graphic = cell.getGraphic();        
        if (graphic != null && graphic instanceof UISWTGraphic) {
        	UISWTGraphic graphicSWT = (UISWTGraphic)graphic;
            Image oldImage = graphicSWT.getImage();
            if (oldImage != null && !oldImage.isDisposed()) {
                Rectangle oldBounds =  oldImage.getBounds();
                if (oldBounds.width != width || oldBounds.height != height) {
                    oldImage.dispose();
                    return this.createImage(width, height);
                }
                return oldImage;
            }
        }
        return this.createImage(width, height);
    }
    
    private Image createImage(int width, int height) {
        return new Image(this.swtManager.getDisplay(), width, height);
    }
    
    /**
     * Triggered when a cell is being dispose of
     * @param cell the TableCell that is being disposed of 
     * @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
     */
    public void dispose(TableCell cell) {
        Graphic graphic = cell.getGraphic();        
        if (graphic != null && graphic instanceof UISWTGraphic) {
        	UISWTGraphic graphicSWT = (UISWTGraphic)graphic;
            Image oldImage = graphicSWT.getImage();
            if (oldImage != null && !oldImage.isDisposed())
                oldImage.dispose();
        }
    }
    
    /**
     * Triggered based on refresh interval specified in
     * {@link org.gudy.azureus2.plugins.ui.tables.TableColumn#getRefreshInterval()}
     * @param cell the TableCell that the refresh trigger is for 
     * @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
     */
    public void cellAdded(TableCell cell) {
        cell.setFillCell(true);
    }
    
    private Image getColoredBar() {
        return this.config.getColoredBar();
    }
    
    private Image getColoredBackground() {
        return this.config.getColoredBackground();
    }
}
