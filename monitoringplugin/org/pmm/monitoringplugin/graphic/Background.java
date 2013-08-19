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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/** 
 * @author kustos
 *
 * This class paints the background if a {@link ChartImage}.
 *
 * TODO find a way to use intermediate images
 */
public class Background {
    private Device device;
    
    private Color[] backgroundColors;
    
    private Color white;
    private Color lightGrey;
    private Color lightGrey2;
    
    private Image buffer;
        
    public Background(Device device) {
        this.device = device;
        this.initializeColors();
    }

    private void initializeColors() {
        this.lightGrey = new Color(this.device, 250,250,250);
        this.lightGrey2 = new Color(this.device,233,233,233);
        
        // the colors in Colors are null if Azureus is not running
        // is the case for prototpye apps
        
        if (Colors.white != null) {
            this.white = Colors.white;
        } else {
            this.white = new Color(this.device,255,255,255);
        }
        
        this.backgroundColors = new Color[4];
        this.backgroundColors[0] = this.lightGrey;
        this.backgroundColors[1] = this.white;
        this.backgroundColors[1] = this.white;
        this.backgroundColors[2] = this.lightGrey;
        this.backgroundColors[3] = this.lightGrey2;
    }
    
    public void dispose() {
        this.lightGrey.dispose();
        this.lightGrey2.dispose();
        
        if (this.white != Colors.white) {
            this.white.dispose();
        }
        
        this.disposeBuffer();
    }
    
    private void disposeBuffer() {
        if (this.buffer != null && !this.buffer.isDisposed()) {
            this.buffer.dispose();            
        }
        this.buffer = null;
    }
    
    /** Not used, so commented out.
    private Image getBuffer(int height, int startY) {
        if (this.buffer == null || this.buffer.getImageData().height != height) {
            this.disposeBuffer();
            this.buffer = new Image(this.device, 1, height);
            int end = startY + height;
            GC gc = new GC(this.buffer);
            int y;
            for(int i = startY ; i < end; ++i) { //XXX can be made more efficient
                gc.setForeground(this.backgroundColors[i%4]);
                y = i - startY;
                gc.drawLine(0, y, 1, y + 1);
            }
            gc.dispose();
        }
        return this.buffer;
    }
    */
    
    public void paintBackground(GC gc, ChartImage image) {
        int x1 = image.getBorderWidth();
        int y1 = x1;
        int x2 = image.getWidth() - image.getBorderWidth();
        int y2 = image.getHeight() - image.getBorderWidth();
        this.paintBackground(gc, image, x1, y1, x2, y2);
    }
    
    public void paintBackground(GC gc, ChartImage image, int x1, int y1, int x2, int y2) {
        for(int y = y1 ; y < y2; ++y) {
            gc.setForeground(this.backgroundColors[y%4]);
            gc.drawLine(x1, y, x2 - 1, y);
        }
//        Image intermediate = this.getBuffer(y2 - y1, y1);
//        for (int x = x1; x < x2; ++x) {
//            gc.copyArea(intermediate, x, y1);
//        }
    }
}
