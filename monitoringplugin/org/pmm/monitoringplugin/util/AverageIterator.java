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

package org.pmm.monitoringplugin.util;

import java.util.Iterator;

public class AverageIterator implements Iterator<Integer> {
    
    private Integer[] data;
    
    private Iterator<Integer> iterator;
    
    public AverageIterator(Iterator<Integer> iterator, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        this.iterator = iterator;
        this.data = new Integer[n];
        for (int i = 1; i < n && this.iterator.hasNext(); ++i) {
            this.data[i] = this.iterator.next();
        }
    }

    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    public Integer next() {
        for (int i = 0; i < this.data.length - 1; ++i) {
            this.data[i] = this.data[i + 1];            
        }
        this.data[this.data.length - 1] = this.iterator.next();
        
        int sum = 0;
        for (Integer each : this.data) {
            sum += each;
        }
        
        return sum / this.data.length;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
