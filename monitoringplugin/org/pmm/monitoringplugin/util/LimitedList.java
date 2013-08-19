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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * 
 * @author kustos
 *
 * A List that contains only a maximum number of elements.
 */
public class LimitedList<E> implements List<E> {
    
    /** the actual List to which we delegate */
    private List<E> list;
    
    private int maxElements;
    
    public LimitedList(int maxElements) {
        this.maxElements = maxElements;
        this.list = new LinkedList<E>();
    }
    
    private void ensureSize() {
        while (this.size() > this.maxElements) {
            this.remove(this.size() - 1);
        }
    }

    /** 
     * @see java.util.List#add(E)
     */
    public boolean add(E o) {
        if (this.size() < this.maxElements) {
            return this.list.add(o);
        }
        return false;
    }

    /** 
     * @see java.util.List#add(int, E)
     */
    public void add(int index, E element) {
        this.list.add(index, element);
        this.ensureSize();
    }

    /** 
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends E> c) {
        for (E each : c) {
            if (this.size() >= this.maxElements) {
                break;
            }
            this.add(each);
        }
        return true;
    }

    /** 
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean changed = this.list.addAll(index, c);
        this.ensureSize();
        return changed;        
    }

    /** 
     * @see java.util.List#clear()
     */
    public void clear() {
        this.list.clear();
    }

    /** 
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return this.list.contains(o);
    }

    /** 
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> c) {
        return this.list.containsAll(c);
    }

    /** 
     * @see java.util.List#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return this.list.equals(o);
    }

    /** 
     * @see java.util.List#get(int)
     */
    public E get(int index) {
        return this.list.get(index);
    }

    /** 
     * @see java.util.List#hashCode()
     */
    public int hashCode() {
        return this.list.hashCode();
    }

    /** 
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        return this.list.indexOf(o);
    }

    /** 
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    /** 
     * @see java.util.List#iterator()
     */
    public Iterator<E> iterator() {
        return this.list.iterator();
    }

    /** 
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        return this.list.lastIndexOf(o);
    }

    /** 
     * @see java.util.List#listIterator()
     */
    public ListIterator<E> listIterator() {
        return this.list.listIterator();
    }

    /** 
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<E> listIterator(int index) {
        return this.list.listIterator(index);
    }

    /** 
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        return this.list.remove(o);
    }

    /** 
     * @see java.util.List#remove(int)
     */
    public E remove(int index) {
        return this.list.remove(index);
    }

    /** 
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> c) {
        return this.list.removeAll(c);
    }

    /** 
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        return this.list.retainAll(c);
    }

    /** 
     * @see java.util.List#set(int, E)
     */
    public E set(int index, E element) {
        return this.list.set(index, element);
    }

    /** 
     * @see java.util.List#size()
     */
    public int size() {
        return this.list.size();
    }

    /** 
     * @see java.util.List#subList(int, int)
     */
    public List<E> subList(int fromIndex, int toIndex) {
        return this.list.subList(fromIndex, toIndex);
    }

    /** 
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        return this.list.toArray();
    }

    /** 
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] a) {
        return this.list.toArray(a);
    }
}
