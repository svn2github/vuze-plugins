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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

import static org.eclipse.swt.SWT.DEFAULT;

/**
 * @author kustos
 * 
 * A simple Layout that lays out components in two rows. All are right aligned
 * all have their preffered size and not grabbing of space happens.
 * Each rightside component is strechted a few pixels {@link #hackbits} to
 * comensate for a too low width returned by
 * {@link org.eclipse.swt.widgets.Label#computeSize(int, int, boolean)}.
 * Wo wrapping if the width is not enough is supported
 */
public class KeyValueLayout extends Layout {
    
    /**
     * The horizontal as well as vertical spacing between components.
     * Also the border size.
     */
    private int spacing;
    
    /**
     * The amount of pixels that are added to the preffered width of 
     * each rightside component.
     */
    private int hackbits;
        
    /** chached info about the children and their sizes */
    private LayoutInfo cache;
    
    public KeyValueLayout() {
        this.spacing = 5;
        this.hackbits = 16;
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
    public Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {        
        LayoutInfo info = this.getLayoutInfo(composite, flushCache);
        int finalWidth = info.leftWidth + info.rightWidth + 3 * this.spacing + this.hackbits;
        Control[] children = composite.getChildren();
        int rows = children.length / 2;
        int finalHeigth;
        if (rows == 0) {
            finalHeigth = info.height + (2 * this.spacing);
        } else {
            finalHeigth = info.height + ((rows + 1) * this.spacing);
        }
        return new Point(finalWidth, finalHeigth);
    }
    
    
    
    /**
     * Instruct the layout to flush any cached values associated with the control specified in the argument control.
     * @param control control managed by this layout 
     * @see org.eclipse.swt.widgets.Layout#flushCache(org.eclipse.swt.widgets.Control)
     */
    protected boolean flushCache(Control control) {
        //boolean superOk = super.flushCache(composite);
        this.cache = null;
        return true;
    }
    private LayoutInfo getLayoutInfo(Composite composite, boolean flush) {
        if (this.cache == null || flush) {
            this.cache = this.computeInfo(composite);
        }
        return this.cache;        
    }
    
    private LayoutInfo computeInfo(Composite composite) {
        Control[] children = composite.getChildren();        
        int leftWidth = 0;
        int rightWidth = 0;
        int totalHeight = 0;
        Control nextChild;
        Point childSize;
        for (int i = 0; i < children.length; ++i) {
            nextChild = children[i];
            //childSize = nextChild.computeSize(DEFAULT, DEFAULT, true);
            childSize = nextChild.computeSize(DEFAULT, DEFAULT);
            if (i % 2 == 0) {
                totalHeight += childSize.y;
                leftWidth = Math.max(leftWidth, childSize.x);
            } else {
                rightWidth = Math.max(rightWidth, childSize.x);
            }
        }
        return new LayoutInfo(leftWidth, rightWidth, totalHeight);
    }

    /**
     * Lays out the children of the specified composite according to this layout.
     * @param composite - a composite widget using this layout
     * @param flushCache - true means flush cached layout values
     * @see org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite, boolean)
     */
    public void layout(Composite composite, boolean flushCache) {
        Control[] children = composite.getChildren();
        LayoutInfo info = this.getLayoutInfo(composite, flushCache);
        Control nextChild;
        Point childSize;
        int heightOffset = this.spacing;
        int widhtOffset = this.spacing + this.spacing + info.leftWidth;
        int nextHeight = 0;
        for (int i = 0; i < children.length; ++i) {            
            nextChild = children[i];
            childSize = nextChild.computeSize(DEFAULT, DEFAULT);
            if (i % 2 == 0) {
                int x = info.leftWidth + this.spacing - childSize.x;
                nextChild.setBounds(x, heightOffset, childSize.x, childSize.y);
                nextHeight = childSize.y;
            } else {
                int x = widhtOffset + (info.rightWidth - childSize.x);                
                nextChild.setBounds(x, heightOffset, childSize.x + this.hackbits, childSize.y);
                heightOffset += nextHeight + this.spacing;
            }
        }
    }

    private class LayoutInfo {
        public int leftWidth, rightWidth, height;
                
        public LayoutInfo(int leftWidth, int rightWidth, int height) {
            this.leftWidth = leftWidth;
            this.rightWidth = rightWidth;
            this.height = height;
        }
    }
}
