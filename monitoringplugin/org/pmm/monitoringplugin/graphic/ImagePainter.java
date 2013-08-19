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

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;

/**
 * 
 * @author kustos
 *
 * This class paints a {@link ChartImage} on a
 * {@link org.eclipse.swt.widgets.Canvas}
 */
public class ImagePainter implements PaintListener {
    
    private ChartImage image;

    public ImagePainter(ChartImage image) {
        this.image = image;
    }

    public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        Canvas canvas = (Canvas) e.widget;
        this.image.paintInto(gc, canvas);
    }
}
