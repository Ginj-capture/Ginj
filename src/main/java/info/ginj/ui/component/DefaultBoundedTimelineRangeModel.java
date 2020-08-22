/*
 * This code is based on DefaultBoundedRangeMondel
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package info.ginj.ui.component;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.io.Serializable;
import java.util.EventListener;

/**
 * A generic implementation of BoundedTimelineRangeModel.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans&trade;
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author David Kloba
 * @author Hans Muller
 * @see BoundedTimelineRangeModel
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultBoundedTimelineRangeModel implements BoundedTimelineRangeModel, Serializable
{
    /**
     * Only one <code>ChangeEvent</code> is needed per model instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this".
     */
    protected transient ChangeEvent changeEvent = null;

    /** The listeners waiting for model changes. */
    protected EventListenerList listenerList = new EventListenerList();

    private int value = 0;
    private int extent = 0;
    private int min = 0;
    private int lower = 0;
    private int higher = 100;
    private int max = 100;
    private int adjustingThumbIndex = THUMB_NONE;


    /**
     * Creates a DefaultBoundedTimelineRangeModel with default values.
     * Those values are:
     * <ul>
     * <li><code>value</code> = 0
     * <li><code>extent</code> = 0
     * <li><code>minimum</code> = 0
     * <li><code>maximum</code> = 100
     * <li><code>lower</code> = 0
     * <li><code>higher</code> = 100
     * <li><code>adjusting</code> = false
     * </ul>
     */
    public DefaultBoundedTimelineRangeModel() {
    }


    /**
     * Initializes value, extent, minimum and maximum. Adjusting is false.
     * Throws an <code>IllegalArgumentException</code> if the following
     * constraints aren't satisfied:
     * <pre>
     * min &lt;= value &lt;= value+extent &lt;= max
     * </pre>
     *
     * @param value  an int giving the current value
     * @param extent the length of the inner range that begins at the model's value
     * @param min    an int giving the minimum (and lower) value
     * @param max    an int giving the maximum (and higher) value
     */
    public DefaultBoundedTimelineRangeModel(int value, int extent, int min, int max)
    {
        if (    (min <= value) &&
                (value <= (value + extent)) &&
                ((value + extent) <= max)
        ) {
            this.value = value;
            this.extent = extent;
            this.lower = this.min = min;
            this.higher = this.max = max;
        }
        else {
            throw new IllegalArgumentException("invalid range properties");
        }
    }


    /**
     * Initializes value, extent, lower, higher, minimum and maximum. Adjusting is false.
     * Throws an <code>IllegalArgumentException</code> if the following
     * constraints aren't satisfied:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     *
     * @param value  an int giving the current value
     * @param extent the length of the inner range that begins at the model's value
     * @param min    an int giving the minimum value
     * @param max    an int giving the maximum value
     * @param lower  an int giving the lower bound value
     * @param higher an int giving the higher bound value
     */
    public DefaultBoundedTimelineRangeModel(int value, int extent, int min, int max, int lower, int higher)
    {
        if (    (min <= lower) &&
                (lower <= value) &&
                (value <= (value + extent)) &&
                ((value + extent) <= higher) &&
                (higher <= max)
        ) {
            this.value = value;
            this.extent = extent;
            this.min = min;
            this.lower = lower;
            this.higher = higher;
            this.max = max;
        }
        else {
            throw new IllegalArgumentException("invalid range properties");
        }
    }


    /**
     * Returns the model's current value.
     * @return the model's current value
     * @see #setValue
     * @see BoundedTimelineRangeModel#getValue
     */
    public int getValue() {
      return value;
    }


    /**
     * Returns the model's extent.
     * @return the model's extent
     * @see #setExtent
     * @see BoundedTimelineRangeModel#getExtent
     */
    public int getExtent() {
      return extent;
    }


    /**
     * Returns the model's minimum.
     * @return the model's minimum
     * @see #setMinimum
     * @see BoundedTimelineRangeModel#getMinimum
     */
    public int getMinimum() {
      return min;
    }


    /**
     * Returns the model's maximum.
     * @return  the model's maximum
     * @see #setMaximum
     * @see BoundedTimelineRangeModel#getMaximum
     */
    public int getMaximum() {
        return max;
    }

    /**
     * Returns the model's lower.
     * @return the model's lower
     * @see #setLower
     * @see BoundedTimelineRangeModel#getLower
     */
    public int getLower() {
        return lower;
    }

    /**
     * Returns the model's higher.
     * @return the model's higher
     * @see #setHigher
     * @see BoundedTimelineRangeModel#getHigher
     */
    public int getHigher() {
        return higher;
    }


    /**
     * Sets the current value of the model. For a slider, that
     * determines where the knob appears. Ensures that the new
     * value, <I>n</I> falls within the model's constraints:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     *
     * @see BoundedTimelineRangeModel#setValue
     */
    public void setValue(int n) {
        // Value can't go outside [lower, higher - extent]
        n = Math.min(n, Integer.MAX_VALUE - extent);

        int newValue = Math.max(n, lower);
        if (newValue + extent > higher) {
            newValue = higher - extent;
        }
        setRangeProperties(newValue, extent, min, max, lower, higher, adjustingThumbIndex);
    }


    /**
     * Sets the extent to <I>n</I> after ensuring that <I>n</I>
     * is greater than or equal to zero and falls within the model's
     * constraints:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     * @see BoundedTimelineRangeModel#setExtent
     */
    public void setExtent(int n) {
        // Extend can't go outside [0, higher - value]
        int newExtent = Math.max(0, n);
        if(value + newExtent > higher) {
            newExtent = higher - value;
        }
        setRangeProperties(value, newExtent, min, max, lower, higher, adjustingThumbIndex);
    }

    /**
     * Sets the minimum to <I>n</I> after ensuring that <I>n</I>
     * that the other properties obey the model's constraints:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     * @see #getMinimum
     * @see BoundedTimelineRangeModel#setMinimum
     */
    public void setMinimum(int n) {
        // Minimum has precedence, adapt all other fields
        int newMax = Math.max(n, max);
        int newHigher = Math.max(n, higher);
        int newValue = Math.max(n, value);
        int newExtent = Math.min(newHigher - newValue, extent);
        int newLower = Math.max(n, lower);
        setRangeProperties(newValue, newExtent, n, newMax, newLower, newHigher, adjustingThumbIndex);
    }


    /**
     * Sets the maximum to <I>n</I> after ensuring that <I>n</I>
     * that the other properties obey the model's constraints:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     * @see BoundedTimelineRangeModel#setMaximum
     */
    public void setMaximum(int n) {
        // Maximum has precedence, all adapt other fields
        int newMin = Math.min(n, min);
        int newLower = Math.min(n, lower);
        int newExtent = Math.min(n - newLower, extent);
        int newValue = Math.min(n - newExtent, value);
        int newHigher = Math.min(n, higher);
        setRangeProperties(newValue, newExtent, newMin, n, newLower, newHigher, adjustingThumbIndex);
    }

    /**
     * Sets the lower to <I>n</I> after ensuring that <I>n</I>
     * that the other properties obey the model's constraints:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     * @see BoundedTimelineRangeModel#setMaximum
     */
    @Override
    public void setLower(int n) {
        // Lower can't go outside [min, max]
        n = Math.max(min, n);
        n = Math.min(max, n);
        // Now adjust the other fields for the new lower
        int newHigher = Math.max(n, higher);
        int newValue = Math.max(n, value);
        int newExtent = Math.min(newHigher - n, extent);
        setRangeProperties(newValue, newExtent, min, max, n, newHigher, adjustingThumbIndex);
    }

    /**
     * Sets the maximum to <I>n</I> after ensuring that <I>n</I>
     * that the other properties obey the model's constraints:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     * @see BoundedTimelineRangeModel#setMaximum
     */
    @Override
    public void setHigher(int n) {
        // Higher can't go outside [min, max]
        n = Math.max(min, n);
        n = Math.min(max, n);
        // Now adjust the other fields for the new lower
        int newLower = Math.min(n, lower);
        int newExtent = Math.min(n - newLower, extent);
        int newValue = Math.min(n - newExtent, value);
        setRangeProperties(newValue, newExtent, min, max, newLower, n, adjustingThumbIndex);
    }


    /**
     * Sets the <code>adjustingThumbIndex</code> property.
     *
     * @see #getAdjustingThumbIndex
     * @see #setValue
     * @see BoundedTimelineRangeModel#setAdjustingThumbIndex
     */
    public void setAdjustingThumbIndex(int adjustingThumbIndex) {
        setRangeProperties(value, extent, min, max, lower, higher, adjustingThumbIndex);
    }

    /**
     * Returns the index of the thumb being adjusted if the current changes to the value property are part
     * of a series of changes, or THUMB_NONE if no thumb is adjusting
     *
     * @return the value of the <code>adjustingThumbIndex</code> property
     * @see #setValue
     * @see BoundedTimelineRangeModel#getAdjustingThumbIndex
     */
    @Override
    public int getAdjustingThumbIndex() {
        return adjustingThumbIndex;
    }


    /**
     * Sets all of the <code>BoundedTimelineRangeModel</code> properties after forcing
     * the arguments to obey the usual constraints:
     * <pre>
     * min &lt;= lower &lt;= value &lt;= value+extent &lt;= higher &lt;= max
     * </pre>
     * <p>
     * At most, one <code>ChangeEvent</code> is generated.
     *
     * @see BoundedTimelineRangeModel#setRangeProperties
     * @see #setValue
     * @see #setExtent
     * @see #setMinimum
     * @see #setMaximum
     * @see #setAdjustingThumbIndex
     */
    public void setRangeProperties(int newValue, int newExtent, int newMin, int newMax, int newLower, int newHigher, int newAdjustingThumbIndex)
    {
        // max has priority over min
        newMin = Math.min(newMin, newMax);
        // higher has priority over lower
        newLower = Math.min(newLower, newHigher);
        // higher and lower can't go outside [min, max]
        newLower = Math.min(Math.max(newLower, newMin), newMax);
        newHigher = Math.min(Math.max(newHigher, newMin), newMax);
        // value can't go outside [lower, higher]
        newValue = Math.min(Math.max(newValue, newLower), newHigher);
        // extent can't go outside [0, higher - value]
        newExtent = Math.min(Math.max(newExtent, 0), newHigher - newValue);

        boolean isChange =
            (newValue != value) ||
            (newExtent != extent) ||
            (newMin != min) ||
            (newMax != max) ||
            (newLower != lower) ||
            (newHigher != higher) ||
            (newAdjustingThumbIndex != adjustingThumbIndex);

        if (isChange) {
            value = newValue;
            extent = newExtent;
            min = newMin;
            max = newMax;
            lower = newLower;
            higher = newHigher;
            adjustingThumbIndex = newAdjustingThumbIndex;

            fireStateChanged();
        }
    }


    /**
     * Adds a <code>ChangeListener</code>.  The change listeners are run each
     * time any one of the Bounded Range model properties changes.
     *
     * @param l the ChangeListener to add
     * @see #removeChangeListener
     * @see BoundedTimelineRangeModel#addChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }


    /**
     * Removes a <code>ChangeListener</code>.
     *
     * @param l the <code>ChangeListener</code> to remove
     * @see #addChangeListener
     * @see BoundedTimelineRangeModel#removeChangeListener
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Returns an array of all the change listeners
     * registered on this <code>DefaultBoundedTimelineRangeModel</code>.
     *
     * @return all of this model's <code>ChangeListener</code>s
     *         or an empty
     *         array if no change listeners are currently registered
     *
     * @see #addChangeListener
     * @see #removeChangeListener
     *
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }


    /**
     * Runs each <code>ChangeListener</code>'s <code>stateChanged</code> method.
     *
     * @see #setRangeProperties
     * @see EventListenerList
     */
    protected void fireStateChanged()
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -=2 ) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }


    /**
     * Returns a string that displays all of the
     * <code>BoundedTimelineRangeModel</code> properties.
     */
    public String toString()  {
        String modelString =
            "value=" + getValue() + ", " +
            "extent=" + getExtent() + ", " +
            "min=" + getMinimum() + ", " +
            "max=" + getMaximum() + ", " +
            "lower=" + getLower() + ", " +
            "higher=" + getHigher() + ", " +
            "adj=" + getAdjustingThumbIndex();

        return getClass().getName() + "[" + modelString + "]";
    }

    /**
     * Returns an array of all the objects currently registered as
     * <code><em>Foo</em>Listener</code>s
     * upon this model.
     * <code><em>Foo</em>Listener</code>s
     * are registered using the <code>add<em>Foo</em>Listener</code> method.
     * <p>
     * You can specify the <code>listenerType</code> argument
     * with a class literal, such as <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a <code>DefaultBoundedTimelineRangeModel</code>
     * instance <code>m</code>
     * for its change listeners
     * with the following code:
     *
     * <pre>ChangeListener[] cls = (ChangeListener[])(m.getListeners(ChangeListener.class));</pre>
     *
     * If no such listeners exist,
     * this method returns an empty array.
     *
     * @param <T> the type of {@code EventListener} class being requested
     * @param listenerType  the type of listeners requested;
     *          this parameter should specify an interface
     *          that descends from <code>java.util.EventListener</code>
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s
     *          on this model,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code> doesn't
     *          specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getChangeListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
