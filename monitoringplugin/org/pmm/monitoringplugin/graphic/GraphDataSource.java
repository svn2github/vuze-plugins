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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.pmm.monitoringplugin.util.LimitedList;

/**
 * @author kustos
 * 
 * Provides scaled data for severals graphs. Graphs are identifiedy by String
 * keys.
 */
public class GraphDataSource {

    private Map<String, List<Long>> unscaledData;

    private Map<String, List<Integer>> scaledData;

    private long min, max;

    private int pixelHeight;

    private int borderWidth;
    
    private int topPadding;

    private boolean needsRescale;
    
    private long scalingBase;

    private Scaler scaler;

    private boolean cutAtMininum;
    
    private int bufferWidth;
    
    public GraphDataSource(int borderWidth, int topPadding, int bufferWidth) {
        this.needsRescale = true;
        this.unscaledData = new TreeMap<String, List<Long>>();
        this.scaledData = new TreeMap<String, List<Integer>>();
        this.borderWidth = borderWidth;
        this.topPadding = topPadding;
        this.bufferWidth = bufferWidth;
    }
    
    public GraphDataSource(int bufferWidth) {
        this(0, 5, bufferWidth);
    }

    public Iterator<Integer> getScaledData(String key) {
        if (!this.unscaledData.containsKey(key)) {
            throw new IllegalArgumentException("no data for: " + key);
        }
        if (this.needsRescale()) {
            this.scaleData();
        }
        return this.scaledData.get(key).iterator();
    }
    
    public int getUnscaledSize(String key) {
        return this.unscaledData.get(key).size();
    }
    
    public int getMovingAverage(String key, int n) {
        int i = 0;
        int average = 0;
        Iterator<Integer> iterator = this.getScaledData(key);
        while (i < n && iterator.hasNext()) {
            average += iterator.next();
            i += 1;
        }
        if (i == 0) {
            return 0;
        }
        return average / i;
    }
    
    private void setScalingBase() {
        long tenBase = pow(10, (long) Math.floor(Math.log10(this.max)));
        if (this.max / tenBase > 3) {
            this.scalingBase = tenBase;
        } else {
            long eightBase = pow(8, (long) Math.floor(log(this.max, 8)));
            if (this.max / eightBase > 3) {
                this.scalingBase = eightBase;
            } else {
                this.scalingBase = tenBase / 10;
            }
        }
    }
    
    private static double log(double a, double base) {
        return Math.log(a) / Math.log(base);
    }

    private List<Long> getUnscaledList() {
        return new LimitedList<Long>(this.bufferWidth);
    }

    private List<Integer> getScaledList() {
        return new LimitedList<Integer>(this.bufferWidth);
    }

    /**
     * Adds a value a graph and returns it scaled.
     * 
     * @param key the key of the graph
     * @param value the value to add
     * @return the scaled value, <code>-1</code> if rescaling is needed
     */
    public int addValue(String key, Long value) {
        int scaled = this.addData(key, value);
        if (!this.setMinAndMax(value)) {
            return scaled;
        }
        return -1;
    }

    private int addData(String key, Long value) {
        if (!this.unscaledData.containsKey(key)) {
            this.unscaledData.put(key, this.getUnscaledList());
        }
        this.unscaledData.get(key).add(0, value);

        if (this.needsRescale) {
            return -1;
        }
        int scaled = this.scaler.scale(value);
        this.scaledData.get(key).add(0, new Integer(scaled));
        return scaled;
    }

    public boolean needsRescale() {
        return this.needsRescale;
    }
    
    public Integer getScaledMax() {
        return this.scaler.scale(this.max);
    }
    
    public long getUnscaledMax() {
        return this.max;
    }
    
public Integer getScaledMin() {
        return this.scaler.scale(this.min);
    }

    private boolean setMinAndMax(Long value) {
        if (value >= 0) {
            if (value > this.max) {
                this.max = value;
                this.scalingDataChanged();
                return true;
            } else if (value < this.min) {
                this.min = value;
                this.scalingDataChanged();
                return true;
            }
        }
        return false;
    }

    public void setPixelHeight(int height) {
        if (height != this.pixelHeight) {
            this.pixelHeight = height;
            this.scalingDataChanged();
        }
    }

    public void seBoderWidth(int width) {
        if (this.borderWidth != width) {
            this.borderWidth = width;
            this.scalingDataChanged();
        }
    }

    private void scalingDataChanged() {
        this.scaler = null;
        this.needsRescale = true;
        this.scaledData.clear();
    }

    private void createScaler() {
        if (this.cutAtMininum) {
            long range = this.max - this.min;
            double factor = ((double) this.pixelHeight - this.borderWidth - this.borderWidth - this.topPadding)
                    / (range);
            long base = (long) (this.min - this.borderWidth / factor);
            this.scaler = new CuttingScaler(base, factor);
        } else {
            double factor =
                ((double) this.pixelHeight - this.borderWidth - this.borderWidth - this.topPadding)
                    / this.max;
            this.scaler = new SimpleScaler(factor);
        }
    }

    private void scaleData() {
        this.createScaler();
        for (Entry<String, List<Long>> eachEntry : this.unscaledData.entrySet()) {
            List<Integer> scaled = this.getScaledList();
            for (Long eachValue : eachEntry.getValue()) {
                scaled.add(this.scaler.scale(eachValue));
            }
            this.scaledData.put(eachEntry.getKey(), scaled);
        }
        this.setScalingBase();
        this.needsRescale = false;
    }
    
    public Integer scale(Long value) {
        if (value == 0) {
            return 0;
        }
        return this.scaler.scale(value);
    }
    
    private interface Scaler {
        public Integer scale(Long value);
    }

    private class SimpleScaler implements Scaler {
        private double factor;

        public SimpleScaler(double factor) {
            this.factor = factor;
        }

        public Integer scale(Long value) {
            return new Integer((int) (value.longValue() * this.factor));
        }
    }

    private class CuttingScaler implements Scaler {
        private long base;

        private double factor;

        public CuttingScaler(long base, double factor) {
            this.base = base;
            this.factor = factor;
        }

        public Integer scale(Long value) {
            return new Integer(
                    (int) ((value.longValue() - this.base) * this.factor));
        }
    }

    public long getScalingBase() {
        if (this.needsRescale) {
            this.scaleData();
        }
        return this.scalingBase;
    }
    
    private static long pow(long base, long exp) {
        long result = 1;
        for (int i = 0; i < exp; ++i) {
            result *= base;
        }
        return result;
    }
}
