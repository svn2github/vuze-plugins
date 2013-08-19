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

package org.pmm.monitoringplugin.layout;

import static org.eclipse.swt.SWT.DEFAULT;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * @author kustos
 *
 * Does basically the same thing as {@link org.eclipse.ui.forms.widgets.ColumnLayout}
 * but is not broken.
 */
public class FixedColumnLayout extends Layout {
    
    /** the width of the top, bottom, left and right border */
    private int borderWidth;
    
    /** the horizontal spacing between controls */
    private int horizontalSpacing;
    
    /** the vertical spacing between controls */
    private int verticalSpacing;
    
    /**
     * FixedColumnLayout with border with of 5 and horizontal and
     * vertical spacing of 5.
     */
    public FixedColumnLayout() {
        this(5, 5, 5);
    }
    
    /**
     * Constructs a new FixedColumnLayout.
     * @param borderWidth the width of the top, bottom, left and right border
     * @param horizontalSpacing the horizontal spacing between controls
     * @param verticalSpacing the vertical spacing between controls
     */
    public FixedColumnLayout(int borderWidth, int horizontalSpacing,
            int verticalSpacing) {
        this.borderWidth = borderWidth;
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }
    /**
     * Computes and returns the size of the specified composite's client area according to this layout.
     * 
     * @param composite - a composite widget using this layout
     * @param wHint - width (SWT.DEFAULT for preferred size)
     * @param hHint - height (SWT.DEFAULT for preferred size)
     * @param flushCache - true means flush cached layout values
     * @see org.eclipse.swt.widgets.Layout#computeSize(org.eclipse.swt.widgets.Composite, int, int, boolean)
     */
    protected Point computeSize(Composite composite, int wHint, int hHint,
            boolean flushCache) {
        Control[] children = composite.getChildren();
        Point[] sizes = this.getSizes(children);
        int maxWidth = this.getMaxWidth(sizes);        
        int numCols = getNumCols(wHint, children, maxWidth);        
        int maxHeight = this.getMaxHeight(numCols, sizes, wHint);        
        int height = maxHeight + this.verticalSpacing + this.verticalSpacing;
        int width = this.getFinalWidth(numCols, maxWidth);
        return new Point(width, height);
    }



    private int getNumCols(int compositeWidth, Control[] children, int childrenWidth) {
        int numCols;
        if (childrenWidth == DEFAULT || compositeWidth == DEFAULT) {
            numCols = children.length;
        } else {
            numCols = compositeWidth / childrenWidth;
        }
        return numCols;
    }

    /**
     * Lays out the children of the specified composite according to this layout.
     * @param composite - a composite widget using this layout
     * @param flushCache - true means flush cached layout values
     * @see org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite, boolean)
     */
    protected void layout(Composite composite, boolean flushCache) {
        Control[] children = composite.getChildren();
        if (children.length > 0) {
            Rectangle clientArea = composite.getClientArea();
            int clientWidth = clientArea.width;
            int clientHeight = clientArea.height;
            Point[] sizes = this.getSizes(children);            
            int maxWidth = this.getMaxWidth(sizes);
            this.recomputeHeights(children, sizes, maxWidth);
            int numCols = this.getNumCols(clientWidth, children, maxWidth);
            
            int x = this.borderWidth;
            int currentChild = 0;
            for (int i = 0; i < numCols; ++i) {
                int y = this.borderWidth;
                while (y <= clientHeight && currentChild < children.length) {
                    Point currentSize = sizes[currentChild];
                    int height = currentSize.y;
                    if (y + height < clientHeight || y == 0) {
                        children[currentChild].setBounds(x, y, maxWidth, height);
                        y += height + this.verticalSpacing;
                        currentChild += 1;
                    } else {
                        break;
                    }
                }
                x += maxWidth + this.horizontalSpacing; 
            }
        }
    }
    
    private int getMaxWidth(Point[] sizes) {
        int maxWidth = DEFAULT;
        for(Point eachSize : sizes) {
            maxWidth = Math.max(maxWidth, eachSize.x);
        }
        return maxWidth;
    }

    private Point[] getSizes(Control[] children) {
        Point[] sizes = new Point[children.length];
        for (int i = 0; i < children.length; ++i) {
            sizes[i] = children[i].computeSize(DEFAULT, DEFAULT);
        }
        return sizes;
    }
    
    private void recomputeHeights(Control[] children, Point[] sizes, int width) {
        for (int i = 0; i < children.length; ++i) {
            sizes[i] = children[i].computeSize(width, DEFAULT);
        }
    }
    
    private int getMaxHeight(int numCols, Point[] sizes, int compositeWidth) {
        int maxHeight = DEFAULT;
        int currentChild = 0;
        for (int i = 0; i < numCols; ++i) {
            int currentHeight = 0;
            do {
                Point currentSize = sizes[currentChild];
                currentHeight += currentSize.y;
                currentChild += 1;
            } while (currentHeight < compositeWidth);
            maxHeight = Math.max(maxHeight, currentHeight);
        }
        return maxHeight;
    }
    
    private int getFinalWidth(int numCols, int childrenWidth) {
        int width;        
        if (childrenWidth == DEFAULT) {
            width = DEFAULT;
        } else {
            width = numCols * childrenWidth;
            if (numCols > 0) {
                width += (numCols - 1) * this.horizontalSpacing; 
            }
        }
        return width;
    }
}
