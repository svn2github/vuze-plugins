package i18nAZ;

/****************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Marty Jones <martybjones@gmail.com> - initial API and implementation
 *  Enrico Schnepel <enrico.schnepel@randomice.net> - clear selectedImage bug 297209
 *  Enrico Schnepel <enrico.schnepel@randomice.net> - disable selectedImage bug 297327
 *  Wolfgang Schramm <wschramm@ch.ibm.com> - added vertical alignment of text for selected table item.
 *****************************************************************************/

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleTextAdapter;
import org.eclipse.swt.accessibility.AccessibleTextEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;

/**
 * The TableCombo class represents a selectable user interface object that
 * combines a label, textfield, and a table and issues notification when an item
 * is selected from the table.
 * 
 * Note: This widget is basically a extension of the CCombo widget. The list
 * control was replaced by a Table control and a Label control was added so that
 * images can be displayed when a value from the drop down items has a image
 * associated to it.
 * 
 * <p>
 * TableCombo was written to allow the user to be able to display multiple
 * columns of data in the "Drop Down" portion of the combo.
 * </p>
 * <p>
 * Special Note: Although this class is a subclass of <code>Composite</code>, it
 * does not make sense to add children to it, or set a layout on it.
 * </p>
 * <dl>
 * <dt><b>Styles:</b>
 * <dd>BORDER, READ_ONLY, FLAT</dd>
 * <dt><b>Events:</b>
 * <dd>DefaultSelection, Modify, Selection, Verify</dd>
 * </dl>
 * 
 */

class TableCombo extends Composite
{
    private Shell popup;
    private Button arrow;
    private Label selectedImage;
    private Text text;
    private Table table;
    private Font font;
    private boolean hasFocus;
    private int visibleItemCount = 7;
    private Listener listener;
    private Listener focusFilter;
    private int displayColumnIndex = 0;
    private Color foreground;
    private Color background;
    private int[] columnWidths;
    private int tableWidthPercentage = 100;
    private boolean showImageWithinSelection = true;
    private boolean showColorWithinSelection = true;
    private boolean showFontWithinSelection = true;

    /**
     * Constructs a new instance of this class given its parent and a style
     * value describing its behavior and appearance.
     * <p>
     * The style value is either one of the style constants defined in class
     * <code>SWT</code> which is applicable to instances of this class, or must
     * be built by <em>bitwise OR</em>'ing together (that is, using the
     * <code>int</code> "|" operator) two or more of those <code>SWT</code>
     * style constants. The class description lists the style constants that are
     * applicable to the class. Style bits are also inherited from superclasses.
     * </p>
     * 
     * @param parent
     *            a widget which will be the parent of the new instance (cannot
     *            be null)
     * @param style
     *            the style of widget to construct
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the parent</li>
     *                </ul>
     * 
     * @see SWT#BORDER
     * @see SWT#READ_ONLY
     * @see SWT#FLAT
     * @see Widget#getStyle()
     */
    public TableCombo(Composite parent, int style)
    {
        super(parent, style = checkStyle(style));

        // set the label style
        int textStyle = SWT.SINGLE;
        if ((style & SWT.READ_ONLY) != 0)
        {
            textStyle |= SWT.READ_ONLY;
        }
        if ((style & SWT.FLAT) != 0)
        {
            textStyle |= SWT.FLAT;
        }

        // set control background to white
        this.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

        // create label to hold image if necessary.
        this.selectedImage = new Label(this, SWT.NONE);
        this.selectedImage.setAlignment(SWT.RIGHT);
        this.selectedImage.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

        this.getLayout();

        // create the control to hold the display text of what the user
        // selected.
        this.text = new Text(this, textStyle);
        this.text.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

        // set the arrow style.
        int arrowStyle = SWT.ARROW | SWT.DOWN;
        if ((style & SWT.FLAT) != 0)
        {
            arrowStyle |= SWT.FLAT;
        }

        // create the down arrow button
        this.arrow = new Button(this, arrowStyle);

        // now add a listener to listen to the events we are interested in.
        this.listener = new Listener()
        {
            
            public void handleEvent(Event event)
            {
                if (TableCombo.this.isDisposed())
                {
                    return;
                }

                // check for a popup event
                if (TableCombo.this.popup == event.widget)
                {
                    TableCombo.this.popupEvent(event);
                    return;
                }

                if (TableCombo.this.text == event.widget)
                {
                    TableCombo.this.textEvent(event);
                    return;
                }

                // check for a table event
                if (TableCombo.this.table == event.widget)
                {
                    TableCombo.this.tableEvent(event);
                    return;
                }

                // check for arrow event
                if (TableCombo.this.arrow == event.widget)
                {
                    TableCombo.this.arrowEvent(event);
                    return;
                }

                // check for this widget's event
                if (TableCombo.this == event.widget)
                {
                    TableCombo.this.comboEvent(event);
                    return;
                }

                // check for shell event
                if (TableCombo.this.getShell() == event.widget)
                {
                    TableCombo.this.getDisplay().asyncExec(new Runnable()
                    {
                        
                        public void run()
                        {
                            if (TableCombo.this.isDisposed())
                            {
                                return;
                            }
                            TableCombo.this.handleFocus(SWT.FocusOut);
                        }
                    });
                }
            }
        };

        // create new focus listener
        this.focusFilter = new Listener()
        {
            
            public void handleEvent(Event event)
            {
                if (TableCombo.this.isDisposed())
                {
                    return;
                }
                Shell shell = ((Control) event.widget).getShell();

                if (shell == TableCombo.this.getShell())
                {
                    TableCombo.this.handleFocus(SWT.FocusOut);
                }
            }
        };

        // set the listeners for this control
        int[] comboEvents = { SWT.Dispose, SWT.FocusIn, SWT.Move, SWT.Resize };
        for (int i = 0; i < comboEvents.length; i++)
        {
            this.addListener(comboEvents[i], this.listener);
        }

        int[] textEvents = { SWT.DefaultSelection, SWT.KeyDown, SWT.KeyUp, SWT.MenuDetect, SWT.Modify, SWT.MouseDown, SWT.MouseUp, SWT.MouseDoubleClick, SWT.MouseWheel, SWT.Traverse, SWT.FocusIn, SWT.Verify };
        for (int i = 0; i < textEvents.length; i++)
        {
            this.text.addListener(textEvents[i], this.listener);
        }

        // set the listeners for the arrow image
        int[] arrowEvents = { SWT.Selection, SWT.FocusIn };
        for (int i = 0; i < arrowEvents.length; i++)
        {
            this.arrow.addListener(arrowEvents[i], this.listener);
        }

        // initialize the drop down
        this.createPopup(-1);

        this.initAccessible();
    }

    /**
     * @param style
     * @return
     */
    private static int checkStyle(int style)
    {
        int mask = SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
        return SWT.NO_FOCUS | (style & mask);
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the receiver's text is modified, by sending it one of the messages
     * defined in the <code>ModifyListener</code> interface.
     * 
     * @param listener
     *            the listener which should be notified
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @see ModifyListener
     * @see #removeModifyListener
     */
    public void addModifyListener(ModifyListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        TypedListener typedListener = new TypedListener(listener);
        this.addListener(SWT.Modify, typedListener);
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the user changes the receiver's selection, by sending it one of the
     * messages defined in the <code>SelectionListener</code> interface.
     * <p>
     * <code>widgetSelected</code> is called when the combo's list selection
     * changes. <code>widgetDefaultSelected</code> is typically called when
     * ENTER is pressed the combo's text area.
     * </p>
     * 
     * @param listener
     *            the listener which should be notified when the user changes
     *            the receiver's selection
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @see SelectionListener
     * @see #removeSelectionListener
     * @see SelectionEvent
     */
    public void addSelectionListener(SelectionListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        TypedListener typedListener = new TypedListener(listener);
        this.addListener(SWT.Selection, typedListener);
        this.addListener(SWT.DefaultSelection, typedListener);
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the user presses keys in the text field. interface.
     * 
     * @param listener
     *            the listener which should be notified when the user presses
     *            keys in the text control.
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public void addTextControlKeyListener(KeyListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        this.text.addKeyListener(listener);
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the user presses keys in the text control.
     * 
     * @param listener
     *            the listener which should no longer be notified
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     */
    public void removeTextControlKeyListener(KeyListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        this.text.removeKeyListener(listener);
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the receiver's text is verified, by sending it one of the messages
     * defined in the <code>VerifyListener</code> interface.
     * 
     * @param listener
     *            the listener which should be notified
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @see VerifyListener
     * @see #removeVerifyListener
     * 
     * @since 3.3
     */
    public void addVerifyListener(VerifyListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        TypedListener typedListener = new TypedListener(listener);
        this.addListener(SWT.Verify, typedListener);
    }

    /**
     * Handle Arrow Event
     * 
     * @param event
     */
    private void arrowEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.FocusIn:
                {
                    this.handleFocus(SWT.FocusIn);
                    break;
                }
            case SWT.MouseDown:
                {
                    Event mouseEvent = new Event();
                    mouseEvent.button = event.button;
                    mouseEvent.count = event.count;
                    mouseEvent.stateMask = event.stateMask;
                    mouseEvent.time = event.time;
                    mouseEvent.x = event.x;
                    mouseEvent.y = event.y;
                    this.notifyListeners(SWT.MouseDown, mouseEvent);
                    event.doit = mouseEvent.doit;
                    break;
                }
            case SWT.MouseUp:
                {
                    Event mouseEvent = new Event();
                    mouseEvent.button = event.button;
                    mouseEvent.count = event.count;
                    mouseEvent.stateMask = event.stateMask;
                    mouseEvent.time = event.time;
                    mouseEvent.x = event.x;
                    mouseEvent.y = event.y;
                    this.notifyListeners(SWT.MouseUp, mouseEvent);
                    event.doit = mouseEvent.doit;
                    break;
                }
            case SWT.Selection:
                {
                    this.text.setFocus();
                    this.dropDown(!this.isDropped());
                    break;
                }
        }
    }

    /**
     * Sets the selection in the receiver's text field to an empty selection
     * starting just before the first character. If the text field is editable,
     * this has the effect of placing the i-beam at the start of the text.
     * <p>
     * Note: To clear the selected items in the receiver's list, use
     * <code>deselectAll()</code>.
     * </p>
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @see #deselectAll
     */
    public void clearSelection()
    {
        this.checkWidget();
        this.text.clearSelection();
        this.table.deselectAll();
    }

    /**
     * Handle Combo events
     * 
     * @param event
     */
    private void comboEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.Dispose:
                this.removeListener(SWT.Dispose, this.listener);
                this.notifyListeners(SWT.Dispose, event);
                event.type = SWT.None;

                if (this.popup != null && !this.popup.isDisposed())
                {
                    this.table.removeListener(SWT.Dispose, this.listener);
                    this.popup.dispose();
                }
                Shell shell = this.getShell();
                shell.removeListener(SWT.Deactivate, this.listener);
                Display display = this.getDisplay();
                display.removeFilter(SWT.FocusIn, this.focusFilter);
                this.popup = null;
                this.text = null;
                this.table = null;
                this.arrow = null;
                this.selectedImage = null;
                break;
            case SWT.FocusIn:
                Control focusControl = this.getDisplay().getFocusControl();
                if (focusControl == this.arrow || focusControl == this.table)
                {
                    return;
                }
                if (this.isDropped())
                {
                    this.table.setFocus();
                }
                else
                {
                    this.text.setFocus();
                }
                break;
            case SWT.Move:
                this.dropDown(false);
                break;
            case SWT.Resize:
                this.internalLayout(false);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        this.checkWidget();

        int overallWidth = 0;
        int overallHeight = 0;
        int borderWidth = this.getBorderWidth();

        // use user defined values if they are specified.
        if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
        {
            overallWidth = wHint;
            overallHeight = hHint;
        }
        else
        {
            TableItem[] tableItems = this.table.getItems();

            GC gc = new GC(this.text);
            int spacer = gc.stringExtent(" ").x; //$NON-NLS-1$
            int maxTextWidth = gc.stringExtent(this.text.getText()).x;
            int colIndex = this.getDisplayColumnIndex();
            int maxImageHeight = 0;
            int currTextWidth = 0;

            // calculate the maximum text width and image height.
            for (int i = 0; i < tableItems.length; i++)
            {
                currTextWidth = gc.stringExtent(tableItems[i].getText(colIndex)).x;

                // take image into account if there is one for the tableitem.
                if (tableItems[i].getImage() != null)
                {
                    currTextWidth += tableItems[i].getImage().getBounds().width;
                    maxImageHeight = Math.max(tableItems[i].getImage().getBounds().height, maxImageHeight);
                }

                maxTextWidth = Math.max(currTextWidth, maxTextWidth);
            }

            gc.dispose();
            Point textSize = this.text.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
            Point arrowSize = this.arrow.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
            Point tableSize = this.table.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);

            overallHeight = Math.max(textSize.y, arrowSize.y);
            overallHeight = Math.max(maxImageHeight, overallHeight);
            overallWidth = Math.max(maxTextWidth + 2 * spacer + arrowSize.x + 2 * borderWidth, tableSize.x);

            // use user specified if they were entered.
            if (wHint != SWT.DEFAULT)
            {
                overallWidth = wHint;
            }
            if (hHint != SWT.DEFAULT)
            {
                overallHeight = hHint;
            }
        }

        return new Point(overallWidth + 2 * borderWidth, overallHeight + 2 * borderWidth);
    }

    /**
     * Copies the selected text.
     * <p>
     * The current selection is copied to the clipboard.
     * </p>
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.3
     */
    public void copy()
    {
        this.checkWidget();
        this.text.copy();
    }

    /**
     * creates the popup shell.
     * 
     * @param selectionIndex
     */
    void createPopup(int selectionIndex)
    {
        // create shell and table
        this.popup = new Shell(this.getShell(), SWT.NO_TRIM | SWT.ON_TOP);

        // set style
        int style = this.getStyle();
        @SuppressWarnings("unused")
        int tableStyle = SWT.SINGLE | SWT.V_SCROLL;
        if ((style & SWT.FLAT) != 0)
        {
            tableStyle |= SWT.FLAT;
        }
        if ((style & SWT.RIGHT_TO_LEFT) != 0)
        {
            tableStyle |= SWT.RIGHT_TO_LEFT;
        }
        if ((style & SWT.LEFT_TO_RIGHT) != 0)
        {
            tableStyle |= SWT.LEFT_TO_RIGHT;
        }

        // create table
        this.table = new Table(this.popup, SWT.SINGLE | SWT.FULL_SELECTION);

        if (this.font != null)
        {
            this.table.setFont(this.font);
        }
        if (this.foreground != null)
        {
            this.table.setForeground(this.foreground);
        }
        if (this.background != null)
        {
            this.table.setBackground(this.background);
        }

        // Add popup listeners
        int[] popupEvents = { SWT.Close, SWT.Paint, SWT.Deactivate };
        for (int i = 0; i < popupEvents.length; i++)
        {
            this.popup.addListener(popupEvents[i], this.listener);
        }

        // add table listeners
        int[] tableEvents = { SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.Dispose };
        for (int i = 0; i < tableEvents.length; i++)
        {
            this.table.addListener(tableEvents[i], this.listener);
        }

        // set the selection
        if (selectionIndex != -1)
        {
            this.table.setSelection(selectionIndex);
        }
    }

    /**
     * Cuts the selected text.
     * <p>
     * The current selection is first copied to the clipboard and then deleted
     * from the widget.
     * </p>
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.3
     */
    public void cut()
    {
        this.checkWidget();
        this.text.cut();
    }

    /**
     * handle DropDown request
     * 
     * @param drop
     */
    void dropDown(boolean drop)
    {

        // if already dropped then return
        if (drop == this.isDropped())
        {
            return;
        }

        // closing the dropDown
        if (!drop)
        {
            this.popup.setVisible(false);
            if (!this.isDisposed() && this.isFocusControl())
            {
                this.text.setFocus();
            }
            return;
        }

        // if not visible then return
        if (!this.isVisible())
        {
            return;
        }

        // create a new popup if needed.
        if (this.getShell() != this.popup.getParent())
        {
            int selectionIndex = this.table.getSelectionIndex();
            this.table.removeListener(SWT.Dispose, this.listener);
            this.popup.dispose();
            this.popup = null;
            this.table = null;
            this.createPopup(selectionIndex);
        }

        // get the size of the TableCombo.
        Point tableComboSize = this.getSize();

        // calculate the table height.
        int itemCount = this.table.getItemCount();
        itemCount = (itemCount == 0) ? this.visibleItemCount : Math.min(this.visibleItemCount, itemCount) - 1;
        int itemHeight = (this.table.getItemHeight() * itemCount);

        // add 1 to the table height if the table item count is less than the
        // visible item count.
        if (this.table.getItemCount() <= this.visibleItemCount)
        {
            itemHeight += 5;
        }

        // add height of header if the header is being displayed.
        if (this.table.getHeaderVisible())
        {
            itemHeight += this.table.getHeaderHeight();
        }

        // get table column references
        TableColumn[] tableColumns = this.table.getColumns();
        int totalColumns = (tableColumns == null ? 0 : tableColumns.length);

        // check to make sure at least one column has been specified. if it
        // hasn't
        // then just create a blank one.
        if (this.table.getColumnCount() == 0)
        {
            new TableColumn(this.table, SWT.NONE);
            totalColumns = 1;
            tableColumns = this.table.getColumns();
        }
        
        int totalColumnBounds = this.columnWidths == null ? 0 : columnWidths.length;
        
        int totalColumnWidth = 0;
        // now pack any columns that do not have a explicit value set for them.
        for (int colIndex = 0; colIndex < totalColumns; colIndex++)
        {
            if (!this.wasColumnWidthSpecified(colIndex))
            {
                tableColumns[colIndex].pack();
            }
            else if (colIndex < totalColumnBounds)
            {
                tableColumns[colIndex].setWidth(columnWidths[colIndex]);
            }
            totalColumnWidth += tableColumns[colIndex].getWidth();
        }

        // calculate the table size after making adjustments.
        Point tableSize = this.table.computeSize(SWT.DEFAULT, itemHeight, false);

        // calculate the table width and table height.
        double pct = this.tableWidthPercentage / 100d;
        int tableWidth = (int) (Math.max(tableComboSize.x - 2, tableSize.x) * pct);
        int tableHeight = tableSize.y;

        // add the width of a horizontal scrollbar to the table height if we are
        // not viewing the full table.
        if (this.tableWidthPercentage < 100)
        {
            tableHeight += this.table.getHorizontalBar().getSize().y;
        }

        // set the bounds on the table.
        this.table.setBounds(1, 1, tableWidth, tableHeight);

        // adjust any of the columns to make sure that there is no empty space
        // after
        // the last column.
        this.autoAdjustColumnWidthsIfNeeded(tableColumns, tableWidth, totalColumnWidth);

        // set the table top index if there is a valid selection.
        int index = this.table.getSelectionIndex();
        if (index != -1)
        {
            this.table.setTopIndex(index);
        }

        // calculate popup dimensions.
        Display display = this.getDisplay();
        Rectangle tableRect = this.table.getBounds();
        Rectangle parentRect = display.map(this.getParent(), null, this.getBounds());
        Point comboSize = this.getSize();
        Rectangle displayRect = this.getMonitor().getClientArea();

        int overallWidth = 0;

        // now set what the overall width should be.
        if (this.tableWidthPercentage < 100)
        {
            overallWidth = tableRect.width + 2;
        }
        else
        {
            overallWidth = Math.max(comboSize.x, tableRect.width + 2);
        }

        int overallHeight = tableRect.height + 2;
        int x = parentRect.x;
        int y = parentRect.y + comboSize.y;
        if (y + overallHeight > displayRect.y + displayRect.height)
        {
            y = parentRect.y - overallHeight;
        }
        if (x + overallWidth > displayRect.x + displayRect.width)
        {
            x = displayRect.x + displayRect.width - tableRect.width;
        }

        // set the bounds of the popup
        this.popup.setBounds(x, y, overallWidth, overallHeight);

        // set the popup visible
        this.popup.setVisible(true);

        // set focus on the table.
        this.table.setFocus();
    }

    /*
     * Return the Label immediately preceding the receiver in the z-order, or
     * null if none.
     */
    private Label getAssociatedLabel()
    {
        Control[] siblings = this.getParent().getChildren();
        for (int i = 0; i < siblings.length; i++)
        {
            if (siblings[i] == TableCombo.this)
            {
                if (i > 0 && siblings[i - 1] instanceof Label)
                {
                    return (Label) siblings[i - 1];
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    
    public Control[] getChildren()
    {
        this.checkWidget();
        return new Control[0];
    }

    /**
     * Gets the editable state.
     * 
     * @return whether or not the receiver is editable
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.0
     */
    public boolean getEditable()
    {
        this.checkWidget();
        return this.text.getEditable();
    }

    /**
     * Returns the item at the given, zero-relative index in the receiver's
     * list. Throws an exception if the index is out of range.
     * 
     * @param index
     *            the index of the item to return
     * @return the item at the given index
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_INVALID_RANGE - if the index is not between 0
     *                and the number of elements in the list minus 1 (inclusive)
     *                </li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public String getItem(int index)
    {
        this.checkWidget();
        return this.table.getItem(index).getText(this.getDisplayColumnIndex());
    }

    /**
     * Returns the number of items contained in the receiver's list.
     * 
     * @return the number of items
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public int getItemCount()
    {
        this.checkWidget();
        return this.table.getItemCount();
    }

    /**
     * Returns the height of the area which would be used to display
     * <em>one</em> of the items in the receiver's list.
     * 
     * @return the height of one item
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public int getItemHeight()
    {
        this.checkWidget();
        return this.table.getItemHeight();
    }

    /**
     * Returns an array of <code>String</code>s which are the items in the
     * receiver's list.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its list of items, so modifying the array will not affect the receiver.
     * </p>
     * 
     * @return the items in the receiver's list
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public String[] getItems()
    {
        this.checkWidget();

        // get a list of the table items.
        TableItem[] tableItems = this.table.getItems();

        int totalItems = (tableItems == null ? 0 : tableItems.length);

        // create string array to hold the total number of items.
        String[] stringItems = new String[totalItems];

        int colIndex = this.getDisplayColumnIndex();

        // now copy the display string from the tableitems.
        for (int index = 0; index < totalItems; index++)
        {
            stringItems[index] = tableItems[index].getText(colIndex);
        }

        return stringItems;
    }

    /**
     * Returns a <code>Point</code> whose x coordinate is the start of the
     * selection in the receiver's text field, and whose y coordinate is the end
     * of the selection. The returned values are zero-relative. An "empty"
     * selection as indicated by the the x and y coordinates having the same
     * value.
     * 
     * @return a point representing the selection start and end
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public Point getSelection()
    {
        this.checkWidget();
        return this.text.getSelection();
    }

    /**
     * Returns the zero-relative index of the item which is currently selected
     * in the receiver's list, or -1 if no item is selected.
     * 
     * @return the index of the selected item
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public int getSelectionIndex()
    {
        this.checkWidget();
        return this.table.getSelectionIndex();
    }

    /**
     * {@inheritDoc}
     */
    
    public int getStyle()
    {
        this.checkWidget();

        int style = super.getStyle();
        style &= ~SWT.READ_ONLY;
        if (!this.text.getEditable())
        {
            style |= SWT.READ_ONLY;
        }
        return style;
    }

    /**
     * Returns a string containing a copy of the contents of the receiver's text
     * field.
     * 
     * @return the receiver's text
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public String getText()
    {
        this.checkWidget();
        return this.text.getText();
    }

    /**
     * Returns the height of the receivers's text field.
     * 
     * @return the text height
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public int getTextHeight()
    {
        this.checkWidget();
        return this.text.getLineHeight();
    }

    /**
     * Gets the number of items that are visible in the drop down portion of the
     * receiver's list.
     * 
     * @return the number of items that are visible
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.0
     */
    public int getVisibleItemCount()
    {
        this.checkWidget();
        return this.visibleItemCount;
    }

    /**
     * Handle Focus event
     * 
     * @param type
     */
    private void handleFocus(int type)
    {
        switch (type)
        {
            case SWT.FocusIn:
                {
                    if (this.hasFocus)
                    {
                        return;
                    }
                    if (this.text.getEditable())
                    {
                        this.text.selectAll();
                    }
                    else
                    {
                        this.text.setSelection(0);
                        this.text.clearSelection();
                        this.text.getParent().forceFocus();
                    }
                    this.hasFocus = true;
                    Shell shell = this.getShell();
                    shell.removeListener(SWT.Deactivate, this.listener);
                    shell.addListener(SWT.Deactivate, this.listener);
                    Display display = this.getDisplay();
                    display.removeFilter(SWT.FocusIn, this.focusFilter);
                    display.addFilter(SWT.FocusIn, this.focusFilter);
                    Event e = new Event();
                    this.notifyListeners(SWT.FocusIn, e);
                    break;
                }
            case SWT.FocusOut:
                {
                    if (!this.hasFocus)
                    {
                        return;
                    }
                    Control focusControl = this.getDisplay().getFocusControl();
                    if (focusControl == this.arrow || focusControl == this.table || focusControl == this.text)
                    {
                        return;
                    }
                    this.hasFocus = false;
                    Shell shell = this.getShell();
                    shell.removeListener(SWT.Deactivate, this.listener);
                    Display display = this.getDisplay();
                    display.removeFilter(SWT.FocusIn, this.focusFilter);
                    Event e = new Event();
                    this.notifyListeners(SWT.FocusOut, e);
                    break;
                }
        }
    }

    /**
     * Searches the receiver's list starting at the first item (index 0) until
     * an item is found that is equal to the argument, and returns the index of
     * that item. If no item is found, returns -1.
     * 
     * @param string
     *            the search item
     * @return the index of the item
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public int indexOf(String string)
    {
        this.checkWidget();
        if (string == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        // get a list of the table items.
        TableItem[] tableItems = this.table.getItems();

        int totalItems = (tableItems == null ? 0 : tableItems.length);
        int colIndex = this.getDisplayColumnIndex();

        // now copy the display string from the tableitems.
        for (int index = 0; index < totalItems; index++)
        {
            if (string.equals(tableItems[index].getText(colIndex)))
            {
                return index;
            }
        }

        return -1;
    }

    /**
     * Searches the receiver's list starting at the given, zero-relative index
     * until an item is found that is equal to the argument, and returns the
     * index of that item. If no item is found or the starting index is out of
     * range, returns -1.
     * 
     * @param string
     *            the search item
     * @param start
     *            the zero-relative index at which to begin the search
     * @return the index of the item
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public int indexOf(String string, int start)
    {
        this.checkWidget();
        if (string == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        // get a list of the table items.
        TableItem[] tableItems = this.table.getItems();

        int totalItems = (tableItems == null ? 0 : tableItems.length);

        if (start < totalItems)
        {

            int colIndex = this.getDisplayColumnIndex();

            // now copy the display string from the tableitems.
            for (int index = start; index < totalItems; index++)
            {
                if (string.equals(tableItems[index].getText(colIndex)))
                {
                    return index;
                }
            }
        }

        return -1;
    }

    /**
     * sets whether or not to show table lines
     * 
     * @param showTableLines
     */
    public void setShowTableLines(boolean showTableLines)
    {
        this.checkWidget();
        this.table.setLinesVisible(showTableLines);
    }

    /**
     * sets whether or not to show table header.
     * 
     * @param showTableHeader
     */
    public void setShowTableHeader(boolean showTableHeader)
    {
        this.checkWidget();
        this.table.setHeaderVisible(showTableHeader);
    }

    /**
     * Add Accessbile listeners to label and table.
     */
    void initAccessible()
    {
        AccessibleAdapter accessibleAdapter = new AccessibleAdapter()
        {
            
            public void getName(AccessibleEvent e)
            {
                String name = null;
                Label label = TableCombo.this.getAssociatedLabel();
                if (label != null)
                {
                    name = TableCombo.this.stripMnemonic(TableCombo.this.text.getText());
                }
                e.result = name;
            }

            
            public void getKeyboardShortcut(AccessibleEvent e)
            {
                String shortcut = null;
                Label label = TableCombo.this.getAssociatedLabel();
                if (label != null)
                {
                    String text = label.getText();
                    if (text != null)
                    {
                        char mnemonic = TableCombo.this._findMnemonic(text);
                        if (mnemonic != '\0')
                        {
                            shortcut = "Alt+" + mnemonic; //$NON-NLS-1$
                        }
                    }
                }
                e.result = shortcut;
            }

            
            public void getHelp(AccessibleEvent e)
            {
                e.result = TableCombo.this.getToolTipText();
            }
        };

        this.getAccessible().addAccessibleListener(accessibleAdapter);
        this.text.getAccessible().addAccessibleListener(accessibleAdapter);
        this.table.getAccessible().addAccessibleListener(accessibleAdapter);

        this.arrow.getAccessible().addAccessibleListener(new AccessibleAdapter()
        {
            
            public void getName(AccessibleEvent e)
            {
                e.result = TableCombo.this.isDropped() ? SWT.getMessage("SWT_Close") : SWT.getMessage("SWT_Open"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            
            public void getKeyboardShortcut(AccessibleEvent e)
            {
                e.result = "Alt+Down Arrow"; //$NON-NLS-1$
            }

            
            public void getHelp(AccessibleEvent e)
            {
                e.result = TableCombo.this.getToolTipText();
            }
        });

        this.getAccessible().addAccessibleTextListener(new AccessibleTextAdapter()
        {
            
            public void getCaretOffset(AccessibleTextEvent e)
            {
                e.offset = TableCombo.this.text.getCaretPosition();
            }

            
            public void getSelectionRange(AccessibleTextEvent e)
            {
                Point sel = TableCombo.this.text.getSelection();
                e.offset = sel.x;
                e.length = sel.y - sel.x;
            }
        });

        this.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter()
        {
            
            public void getChildAtPoint(AccessibleControlEvent e)
            {
                Point testPoint = TableCombo.this.toControl(e.x, e.y);
                if (TableCombo.this.getBounds().contains(testPoint))
                {
                    e.childID = ACC.CHILDID_SELF;
                }
            }

            
            public void getLocation(AccessibleControlEvent e)
            {
                Rectangle location = TableCombo.this.getBounds();
                Point pt = TableCombo.this.getParent().toDisplay(location.x, location.y);
                e.x = pt.x;
                e.y = pt.y;
                e.width = location.width;
                e.height = location.height;
            }

            
            public void getChildCount(AccessibleControlEvent e)
            {
                e.detail = 0;
            }

            
            public void getRole(AccessibleControlEvent e)
            {
                e.detail = ACC.ROLE_COMBOBOX;
            }

            
            public void getState(AccessibleControlEvent e)
            {
                e.detail = ACC.STATE_NORMAL;
            }

            
            public void getValue(AccessibleControlEvent e)
            {
                e.result = TableCombo.this.text.getText();
            }
        });

        this.text.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter()
        {
            
            public void getRole(AccessibleControlEvent e)
            {
                e.detail = TableCombo.this.text.getEditable() ? ACC.ROLE_TEXT : ACC.ROLE_LABEL;
            }
        });

        this.arrow.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter()
        {
            
            public void getDefaultAction(AccessibleControlEvent e)
            {
                e.result = TableCombo.this.isDropped() ? SWT.getMessage("SWT_Close") : SWT.getMessage("SWT_Open"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        });
    }

    /**
     * returns if the drop down is currently open
     * 
     * @return
     */
    boolean isDropped()
    {
        return this.popup.getVisible();
    }

    /**
     * {@inheritDoc}
     */
    
    public boolean isFocusControl()
    {
        this.checkWidget();
        // if (label.isFocusControl () || arrow.isFocusControl () ||
        // table.isFocusControl () || popup.isFocusControl ()) {
        if (this.arrow.isFocusControl() || this.table.isFocusControl() || this.popup.isFocusControl())
        {
            return true;
        }
        return super.isFocusControl();
    }

    /**
     * This method is invoked when a resize event occurs.
     * 
     * @param changed
     */
    private void internalLayout(boolean changed)
    {
        if (this.isDropped())
        {
            this.dropDown(false);
        }
        Rectangle rect = this.getClientArea();
        int width = rect.width;
        int height = rect.height;
        Point arrowSize = this.arrow.computeSize(SWT.DEFAULT, height, changed);

        // calculate text vertical alignment.
        int textYPos = 0;
        Point textSize = this.text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        if (textSize.y < height)
        {
            textYPos = (height - textSize.y) / 2;
        }

        // does the selected entry have a image associated with it?
        if (this.selectedImage.getImage() == null)
        {
            // set image, text, and arrow boundaries
            this.selectedImage.setBounds(0, 0, 0, 0);
            this.text.setBounds(0, textYPos, width - arrowSize.x, height);
            this.arrow.setBounds(width - arrowSize.x, 0, arrowSize.x, arrowSize.y);
        }
        else
        {
            // calculate the amount of width left in the control after taking
            // into account the arrow selector
            int remainingWidth = width - arrowSize.x;
            int imageWidth = this.selectedImage.computeSize(SWT.DEFAULT, height, changed).x + 2;

            // handle the case where the image is larger than the available
            // space in the control.
            if (imageWidth > remainingWidth)
            {
                imageWidth = remainingWidth;
                remainingWidth = 0;
            }
            else
            {
                remainingWidth = remainingWidth - imageWidth;
            }

            // set the width of the text.
            int textWidth = remainingWidth;

            // set image, text, and arrow boundaries
            this.selectedImage.setBounds(0, 0, imageWidth, height);
            this.text.setBounds(imageWidth, textYPos, textWidth, height);
            this.arrow.setBounds(imageWidth + textWidth, 0, arrowSize.x, arrowSize.y);
        }
    }

    /**
     * Handles Table Events.
     * 
     * @param event
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private void tableEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.Dispose:
                if (this.getShell() != this.popup.getParent())
                {
                    int selectionIndex = this.table.getSelectionIndex();
                    this.popup = null;
                    this.table = null;
                    this.createPopup(selectionIndex);
                }
                break;
            case SWT.FocusIn:
                {
                    this.handleFocus(SWT.FocusIn);
                    break;
                }
            case SWT.MouseUp:
                {
                    if (event.button != 1)
                    {
                        return;
                    }
                    this.dropDown(false);
                    break;
                }
            case SWT.Selection:
                {
                    int index = this.table.getSelectionIndex();
                    if (index == -1)
                    {
                        return;
                    }

                    // refresh the text.
                    this.refreshText(index);

                    // set the selection in the table.
                    this.table.setSelection(index);

                    Event e = new Event();
                    e.time = event.time;
                    e.stateMask = event.stateMask;
                    e.doit = event.doit;
                    this.notifyListeners(SWT.Selection, e);
                    event.doit = e.doit;
                    break;
                }
            case SWT.Traverse:
                {
                    switch (event.detail)
                    {
                        case SWT.TRAVERSE_RETURN:
                        case SWT.TRAVERSE_ESCAPE:
                        case SWT.TRAVERSE_ARROW_PREVIOUS:
                        case SWT.TRAVERSE_ARROW_NEXT:
                            event.doit = false;
                            break;
                        case SWT.TRAVERSE_TAB_NEXT:
                        case SWT.TRAVERSE_TAB_PREVIOUS:
                            event.doit = this.text.traverse(event.detail);
                            event.detail = SWT.TRAVERSE_NONE;
                            if (event.doit)
                            {
                                this.dropDown(false);
                            }
                            return;
                    }
                    Event e = new Event();
                    e.time = event.time;
                    e.detail = event.detail;
                    e.doit = event.doit;
                    e.character = event.character;
                    e.keyCode = event.keyCode;
                    this.notifyListeners(SWT.Traverse, e);
                    event.doit = e.doit;
                    event.detail = e.detail;
                    break;
                }
            case SWT.KeyUp:
                {
                    Event e = new Event();
                    e.time = event.time;
                    e.character = event.character;
                    e.keyCode = event.keyCode;
                    e.stateMask = event.stateMask;
                    this.notifyListeners(SWT.KeyUp, e);
                    break;
                }
            case SWT.KeyDown:
                {
                    if (event.character == SWT.ESC)
                    {
                        // Escape key cancels popup list
                        this.dropDown(false);
                    }
                    if ((event.stateMask & SWT.ALT) != 0 && (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN))
                    {
                        this.dropDown(false);
                    }
                    if (event.character == SWT.CR)
                    {
                        // Enter causes default selection
                        this.dropDown(false);
                        Event e = new Event();
                        e.time = event.time;
                        e.stateMask = event.stateMask;
                        this.notifyListeners(SWT.DefaultSelection, e);
                    }
                    // At this point the widget may have been disposed.
                    // If so, do not continue.
                    if (this.isDisposed())
                    {
                        break;
                    }
                    Event e = new Event();
                    e.time = event.time;
                    e.character = event.character;
                    e.keyCode = event.keyCode;
                    e.stateMask = event.stateMask;
                    this.notifyListeners(SWT.KeyDown, e);
                    break;

                }
        }
    }

    /**
     * Pastes text from clipboard.
     * <p>
     * The selected text is deleted from the widget and new text inserted from
     * the clipboard.
     * </p>
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.3
     */
    public void paste()
    {
        this.checkWidget();
        this.text.paste();
    }

    /**
     * Handles Popup Events
     * 
     * @param event
     */
    private void popupEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.Paint:
                // draw rectangle around table
                Rectangle tableRect = this.table.getBounds();
                event.gc.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
                event.gc.drawRectangle(0, 0, tableRect.width + 1, tableRect.height + 1);
                break;
            case SWT.Close:
                event.doit = false;
                this.dropDown(false);
                break;
            case SWT.Deactivate:
                /*
                 * Bug in GTK. When the arrow button is pressed the popup
                 * control receives a deactivate event and then the arrow button
                 * receives a selection event. If we hide the popup in the
                 * deactivate event, the selection event will show it again. To
                 * prevent the popup from showing again, we will let the
                 * selection event of the arrow button hide the popup. In
                 * Windows, hiding the popup during the deactivate causes the
                 * deactivate to be called twice and the selection event to be
                 * disappear.
                 */
                if (!"carbon".equals(SWT.getPlatform()))
                {
                    Point point = this.arrow.toControl(this.getDisplay().getCursorLocation());
                    Point size = this.arrow.getSize();
                    Rectangle rect = new Rectangle(0, 0, size.x, size.y);
                    if (!rect.contains(point))
                    {
                        this.dropDown(false);
                    }
                }
                else
                {
                    this.dropDown(false);
                }
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    
    public void redraw()
    {
        super.redraw();
        this.text.redraw();
        this.arrow.redraw();
        if (this.popup.isVisible())
        {
            this.table.redraw();
        }
    }

    /**
     * {@inheritDoc}
     */
    
    public void redraw(int x, int y, int width, int height, boolean all)
    {
        super.redraw(x, y, width, height, true);
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the receiver's text is modified.
     * 
     * @param listener
     *            the listener which should no longer be notified
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @see ModifyListener
     * @see #addModifyListener
     */
    public void removeModifyListener(ModifyListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        this.removeListener(SWT.Modify, listener);
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the user changes the receiver's selection.
     * 
     * @param listener
     *            the listener which should no longer be notified
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @see SelectionListener
     * @see #addSelectionListener
     */
    public void removeSelectionListener(SelectionListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        this.removeListener(SWT.Selection, listener);
        this.removeListener(SWT.DefaultSelection, listener);
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the control is verified.
     * 
     * @param listener
     *            the listener which should no longer be notified
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @see VerifyListener
     * @see #addVerifyListener
     * 
     * @since 3.3
     */
    public void removeVerifyListener(VerifyListener listener)
    {
        this.checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        this.removeListener(SWT.Verify, listener);
    }

    /**
     * Selects the item at the given zero-relative index in the receiver's list.
     * If the item at the index was already selected, it remains selected.
     * Indices that are out of range are ignored.
     * 
     * @param index
     *            the index of the item to select
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public void select(int index)
    {
        this.checkWidget();

        // deselect if a value of -1 is passed in.
        if (index == -1)
        {
            this.table.deselectAll();
            this.text.setText("");
            this.selectedImage.setImage(null);
            return;
        }

        if (0 <= index && index < this.table.getItemCount())
        {
            if (index != this.getSelectionIndex())
            {

                // refresh the text field and image label
                this.refreshText(index);

                // select the row in the table.
                this.table.setSelection(index);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    
    public void setBackground(Color color)
    {
        super.setBackground(color);
        this.background = color;
        if (this.text != null)
        {
            this.text.setBackground(color);
        }
        if (this.selectedImage != null)
        {
            this.selectedImage.setBackground(color);
        }
        if (this.table != null)
        {
            this.table.setBackground(color);
        }
        if (this.arrow != null)
        {
            this.arrow.setBackground(color);
        }
    }

    /**
     * Sets the editable state.
     * 
     * @param editable
     *            the new editable state
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.0
     */
    public void setEditable(boolean editable)
    {
        this.checkWidget();
        this.text.setEditable(editable);
    }

    /**
     * {@inheritDoc}
     */
    
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        if (this.popup != null)
        {
            this.popup.setVisible(false);
        }
        if (this.selectedImage != null)
        {
            this.selectedImage.setEnabled(enabled);
        }
        if (this.text != null)
        {
            this.text.setEnabled(enabled);
        }
        if (this.arrow != null)
        {
            this.arrow.setEnabled(enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    
    public boolean setFocus()
    {
        this.checkWidget();
        if (!this.isEnabled() || !this.isVisible())
        {
            return false;
        }
        if (this.isFocusControl())
        {
            return true;
        }

        return this.text.setFocus();
    }

    /**
     * {@inheritDoc}
     */
    
    public void setFont(Font font)
    {
        super.setFont(font);
        this.font = font;
        this.text.setFont(font);
        this.table.setFont(font);
        this.internalLayout(true);
    }

    /**
     * {@inheritDoc}
     */
    
    public void setForeground(Color color)
    {
        super.setForeground(color);
        this.foreground = color;
        if (this.text != null)
        {
            this.text.setForeground(color);
        }
        if (this.table != null)
        {
            this.table.setForeground(color);
        }
        if (this.arrow != null)
        {
            this.arrow.setForeground(color);
        }
    }

    /**
     * Sets the layout which is associated with the receiver to be the argument
     * which may be null.
     * <p>
     * Note : No Layout can be set on this Control because it already manages
     * the size and position of its children.
     * </p>
     * 
     * @param layout
     *            the receiver's new layout or null
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    
    public void setLayout(Layout layout)
    {
        this.checkWidget();
        return;
    }

    /**
     * Marks the receiver's list as visible if the argument is <code>true</code>
     * , and marks it invisible otherwise.
     * <p>
     * If one of the receiver's ancestors is not visible or some other condition
     * makes the receiver not visible, marking it visible may not actually cause
     * it to be displayed.
     * </p>
     * 
     * @param visible
     *            the new visibility state
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.4
     */
    public void setTableVisible(boolean visible)
    {
        this.checkWidget();
        this.dropDown(visible);
    }

    /**
     * Sets the selection in the receiver's text field to the range specified by
     * the argument whose x coordinate is the start of the selection and whose y
     * coordinate is the end of the selection.
     * 
     * @param selection
     *            a point representing the new selection start and end
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the point is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public void setSelection(Point selection)
    {
        this.checkWidget();
        if (selection == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        this.text.setSelection(selection.x, selection.y);
    }

    /**
     * Sets the contents of the receiver's text field to the given string.
     * <p>
     * Note: The text field in a <code>Combo</code> is typically only capable of
     * displaying a single line of text. Thus, setting the text to a string
     * containing line breaks or other special characters will probably cause it
     * to display incorrectly.
     * </p>
     * 
     * @param string
     *            the new text
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public void setText(String string)
    {
        this.checkWidget();
        if (string == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        // find the index of the given string.
        int index = this.indexOf(string);

        if (index == -1)
        {
            this.table.deselectAll();
            this.text.setText(string);
            return;
        }

        // select the text and table row.
        this.select(index);
    }

    /**
     * Sets the maximum number of characters that the receiver's text field is
     * capable of holding to be the argument.
     * 
     * @param limit
     *            new text limit
     * 
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_CANNOT_BE_ZERO - if the limit is zero</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    public void setTextLimit(int limit)
    {
        this.checkWidget();
        this.text.setTextLimit(limit);
    }

    /**
     * {@inheritDoc}
     */
    
    public void setToolTipText(String tipText)
    {
        this.checkWidget();
        super.setToolTipText(tipText);
        if (this.selectedImage != null)
        {
            this.selectedImage.setToolTipText(tipText);
        }
        if (this.text != null)
        {
            this.text.setToolTipText(tipText);
        }
        if (this.arrow != null)
        {
            this.arrow.setToolTipText(tipText);
        }
    }

    /**
     * {@inheritDoc}
     */
    
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        /*
         * At this point the widget may have been disposed in a FocusOut event.
         * If so then do not continue.
         */
        if (this.isDisposed())
        {
            return;
        }
        // TEMPORARY CODE
        if (this.popup == null || this.popup.isDisposed())
        {
            return;
        }
        if (!visible)
        {
            this.popup.setVisible(false);
        }
    }

    /**
     * Sets the number of items that are visible in the drop down portion of the
     * receiver's list.
     * 
     * @param count
     *            the new number of items to be visible
     * 
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * 
     * @since 3.0
     */
    public void setVisibleItemCount(int count)
    {
        this.checkWidget();
        if (count > 0)
        {
            this.visibleItemCount = count;
        }
    }

    /**
     * @param string
     * @return
     */
    private String stripMnemonic(String string)
    {
        int index = 0;
        int length = string.length();
        do
        {
            while ((index < length) && (string.charAt(index) != '&'))
            {
                index++;
            }
            if (++index >= length)
            {
                return string;
            }
            if (string.charAt(index) != '&')
            {
                return string.substring(0, index - 1) + string.substring(index, length);
            }
            index++;
        }
        while (index < length);
        return string;
    }

    /**
     * Defines what columns the drop down table will have.
     * 
     * Use this method when you don't care about the width of the columns but
     * want to set the column header text.
     * 
     * @param columnHeaders
     * @param columnWidths
     */
    public void defineColumns(String[] columnHeaders)
    {
        if (columnHeaders != null && columnHeaders.length > 0)
        {
            this.defineColumnsInternal(columnHeaders, null, null, columnHeaders.length);
        }
    }

    /**
     * Defines what columns the drop down table will have.
     * 
     * Use this method when you don't care about the column header text but you
     * want the fields to be a specific width.
     * 
     * @param columnHeaders
     * @param columnBounds
     */
    public void defineColumns(int[] columnBounds)
    {
        this.columnWidths = columnBounds;

        if (columnBounds != null && columnBounds.length > 0)
        {
            this.defineColumnsInternal(null, columnBounds, null, columnBounds.length);
        }
    }

    /**
     * Defines what columns the drop down table will have.
     * 
     * Use this method when you don't care about the column header text but you
     * want the fields to be a specific width and specific style.
     * 
     * @param columnHeaders
     * @param columnBounds
     * @param columnStyles
     */
    public void defineColumns(int[] columnBounds, int[] columnStyles)
    {
        this.columnWidths = columnBounds;

        if (columnBounds != null && columnBounds.length > 0 && columnStyles != null && columnStyles.length > 0)
        {
            this.defineColumnsInternal(null, columnBounds, columnStyles, columnBounds.length);
        }
    }

    /**
     * Defines what columns the drop down table will have.
     * 
     * Use this method when you don't care about the column headers and you want
     * the columns to be automatically sized based upon their content.
     * 
     * @param columnHeaders
     * @param columnWidths
     */

    public void defineColumns(int numberOfColumnsToCreate)
    {
        if (numberOfColumnsToCreate > 0)
        {
            this.defineColumnsInternal(null, null, null, numberOfColumnsToCreate);
        }

    }

    /**
     * Defines what columns the drop down table will have.
     * 
     * Use this method when you want to specify the column header text and the
     * column widths.
     * 
     * @param columnHeaders
     * @param columnBounds
     */
    public void defineColumns(String[] columnHeaders, int[] columnBounds)
    {
        if (columnHeaders != null || columnBounds != null)
        {
            int total = columnHeaders == null ? 0 : columnHeaders.length;
            if (columnBounds != null && columnBounds.length > total)
            {
                total = columnBounds.length;
            }

            this.columnWidths = columnBounds;
            // define the columns
            this.defineColumnsInternal(columnHeaders, columnBounds, null, total);
        }
    }

    /**
     * Defines what columns the drop down table will have.
     * 
     * @param columnHeaders
     * @param columnBounds
     */
    private void defineColumnsInternal(String[] columnHeaders, int[] columnBounds, int[] columnStyles, int totalColumnsToBeCreated)
    {

        this.checkWidget();

        int totalColumnHeaders = columnHeaders == null ? 0 : columnHeaders.length;
        int totalColumnBounds = columnBounds == null ? 0 : columnBounds.length;
        int totalColumnStyles = columnStyles == null ? 0 : columnStyles.length;

        if (totalColumnsToBeCreated > 0)
        {

            for (int index = 0; index < totalColumnsToBeCreated; index++)
            {

                int columnStyle = SWT.NULL;
                if (index < totalColumnStyles)
                {
                    columnStyle = columnStyles[index];
                }

                TableColumn column = new TableColumn(this.table, columnStyle);

                if (index < totalColumnHeaders)
                {
                    column.setText(columnHeaders[index]);
                }

                if (index < totalColumnBounds)
                {
                    column.setWidth(columnBounds[index]);
                }

                column.setResizable(true);
                column.setMoveable(true);
            }
        }
    }

    /**
     * Sets the table width percentage in relation to the width of the label
     * control.
     * 
     * The default value if 100% which means that it will be the same size as
     * the label control. If you want the table to be wider than the label then
     * just display a value higher than 100%.
     * 
     * @param ddWidthPct
     */
    public void setTableWidthPercentage(int ddWidthPct)
    {
        this.checkWidget();

        // don't accept invalid input.
        if (ddWidthPct > 0 && ddWidthPct <= 100)
        {
            this.tableWidthPercentage = ddWidthPct;
        }
    }

    /**
     * Sets the zero-relative column index that will be used to display the
     * currently selected item in the label control.
     * 
     * @param displayColumnIndex
     */
    public void setDisplayColumnIndex(int displayColumnIndex)
    {
        this.checkWidget();

        if (displayColumnIndex >= 0)
        {
            this.displayColumnIndex = displayColumnIndex;
        }
    }

    /**
     * returns the column index of the TableColumn to be displayed when
     * selected.
     * 
     * @return
     */
    private int getDisplayColumnIndex()
    {
        // make sure the requested column index is valid.
        return (this.displayColumnIndex <= (this.table.getColumnCount() - 1) ? this.displayColumnIndex : 0);
    }

    /*
     * Return the lowercase of the first non-'&' character following an '&'
     * character in the given string. If there are no '&' characters in the
     * given string, return '\0'.
     */
    private char _findMnemonic(String string)
    {
        if (string == null)
        {
            return '\0';
        }
        int index = 0;
        int length = string.length();
        do
        {
            while (index < length && string.charAt(index) != '&')
            {
                index++;
            }
            if (++index >= length)
            {
                return '\0';
            }
            if (string.charAt(index) != '&')
            {
                return Character.toLowerCase(string.charAt(index));
            }
            index++;
        }
        while (index < length);
        return '\0';
    }

    /**
     * Refreshes the label control with the selected object's details.
     */
    private void refreshText(int index)
    {

        // get a reference to the selected TableItem
        TableItem tableItem = this.table.getItem(index);

        // get the TableItem index to use for displaying the text.
        int colIndexToUse = this.getDisplayColumnIndex();

        // set image if requested
        if (this.showImageWithinSelection)
        {
            // set the selected image
            this.selectedImage.setImage(tableItem.getImage(colIndexToUse));

            // refresh the layout of the widget
            this.internalLayout(false);
        }

        // set color if requested
        if (this.showColorWithinSelection)
        {
            this.text.setForeground(tableItem.getForeground(colIndexToUse));
        }

        // set font if requested
        if (this.showFontWithinSelection)
        {
            // set the selected font
            this.text.setFont(tableItem.getFont(colIndexToUse));
        }

        // set the label text.
        this.text.setText(tableItem.getText(colIndexToUse));
        if (this.text.getEditable())
        {
            this.text.selectAll();
        }
        else
        {
            this.text.setSelection(0);
            this.text.clearSelection();
            this.text.getParent().forceFocus();
        }
    }

    /**
     * @param showImageWithinSelection
     */
    public void setShowImageWithinSelection(boolean showImageWithinSelection)
    {
        this.checkWidget();
        this.showImageWithinSelection = showImageWithinSelection;
    }

    /**
     * @param showColorWithinSelection
     */
    public void setShowColorWithinSelection(boolean showColorWithinSelection)
    {
        this.checkWidget();
        this.showColorWithinSelection = showColorWithinSelection;
    }

    /**
     * @param showFontWithinSelection
     */
    public void setShowFontWithinSelection(boolean showFontWithinSelection)
    {
        this.checkWidget();
        this.showFontWithinSelection = showFontWithinSelection;
    }

    /**
     * returns the Table reference.
     * 
     * NOTE: the access is public for now but will most likely be changed in a
     * future release.
     * 
     * @return
     */
    public Table getTable()
    {
        this.checkWidget();
        return this.table;
    }

    /**
     * determines if the user explicitly set a column width for a given column
     * index.
     * 
     * @param columnIndex
     * @return
     */
    private boolean wasColumnWidthSpecified(int columnIndex)
    {
        return (this.columnWidths != null && this.columnWidths.length > columnIndex && this.columnWidths[columnIndex] != SWT.DEFAULT);
    }

    void textEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.FocusIn:
                {
                    this.handleFocus(SWT.FocusIn);
                    break;
                }
            case SWT.DefaultSelection:
                {
                    this.dropDown(false);
                    Event e = new Event();
                    e.time = event.time;
                    e.stateMask = event.stateMask;
                    this.notifyListeners(SWT.DefaultSelection, e);
                    break;
                }
            case SWT.KeyDown:
                {
                    Event keyEvent = new Event();
                    keyEvent.time = event.time;
                    keyEvent.character = event.character;
                    keyEvent.keyCode = event.keyCode;
                    keyEvent.stateMask = event.stateMask;
                    this.notifyListeners(SWT.KeyDown, keyEvent);
                    if (this.isDisposed())
                    {
                        break;
                    }
                    event.doit = keyEvent.doit;
                    if (!event.doit)
                    {
                        break;
                    }
                    if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN)
                    {
                        event.doit = false;
                        if ((event.stateMask & SWT.ALT) != 0)
                        {
                            boolean dropped = this.isDropped();
                            if (this.text.getEditable())
                            {
                                this.text.selectAll();
                            }
                            else
                            {
                                this.text.setSelection(0);
                                this.text.clearSelection();
                                this.text.getParent().forceFocus();
                            }
                            if (!dropped)
                            {
                                this.setFocus();
                            }
                            this.dropDown(!dropped);
                            break;
                        }

                        int oldIndex = this.table.getSelectionIndex();
                        if (event.keyCode == SWT.ARROW_UP)
                        {
                            this.select(Math.max(oldIndex - 1, 0));
                        }
                        else
                        {
                            this.select(Math.min(oldIndex + 1, this.getItemCount() - 1));
                        }
                        if (oldIndex != this.table.getSelectionIndex())
                        {
                            Event e = new Event();
                            e.time = event.time;
                            e.stateMask = event.stateMask;
                            this.notifyListeners(SWT.Selection, e);
                        }
                        if (this.isDisposed())
                        {
                            break;
                        }
                    }

                    // Further work : Need to add support for incremental search
                    // in
                    // pop up list as characters typed in text widget
                    break;
                }
            case SWT.KeyUp:
                {
                    Event e = new Event();
                    e.time = event.time;
                    e.character = event.character;
                    e.keyCode = event.keyCode;
                    e.stateMask = event.stateMask;
                    this.notifyListeners(SWT.KeyUp, e);
                    event.doit = e.doit;
                    break;
                }
            case SWT.MenuDetect:
                {
                    Event e = new Event();
                    e.time = event.time;
                    this.notifyListeners(SWT.MenuDetect, e);
                    break;
                }
            case SWT.Modify:
                {
                    this.table.deselectAll();
                    Event e = new Event();
                    e.time = event.time;
                    this.notifyListeners(SWT.Modify, e);
                    break;
                }
            case SWT.MouseDown:
                {
                    Event mouseEvent = new Event();
                    mouseEvent.button = event.button;
                    mouseEvent.count = event.count;
                    mouseEvent.stateMask = event.stateMask;
                    mouseEvent.time = event.time;
                    mouseEvent.x = event.x;
                    mouseEvent.y = event.y;
                    this.notifyListeners(SWT.MouseDown, mouseEvent);
                    if (this.isDisposed())
                    {
                        break;
                    }
                    event.doit = mouseEvent.doit;
                    if (!event.doit)
                    {
                        break;
                    }
                    if (event.button != 1)
                    {
                        return;
                    }
                    if (this.text.getEditable())
                    {
                        return;
                    }
                    boolean dropped = this.isDropped();
                    if (this.text.getEditable())
                    {
                        this.text.selectAll();
                    }
                    else
                    {
                        this.text.setSelection(0);
                        this.text.clearSelection();
                        this.text.getParent().forceFocus();
                    }
                    if (!dropped)
                    {
                        this.setFocus();
                    }
                    this.dropDown(!dropped);
                    break;
                }
            case SWT.MouseUp:
                {
                    Event mouseEvent = new Event();
                    mouseEvent.button = event.button;
                    mouseEvent.count = event.count;
                    mouseEvent.stateMask = event.stateMask;
                    mouseEvent.time = event.time;
                    mouseEvent.x = event.x;
                    mouseEvent.y = event.y;
                    this.notifyListeners(SWT.MouseUp, mouseEvent);
                    if (this.isDisposed())
                    {
                        break;
                    }
                    event.doit = mouseEvent.doit;
                    if (!event.doit)
                    {
                        break;
                    }
                    if (event.button != 1)
                    {
                        return;
                    }
                    if (this.text.getEditable())
                    {
                        this.text.selectAll();
                    }
                    else
                    {
                        this.text.setSelection(0);
                        this.text.clearSelection();
                        this.text.getParent().forceFocus();
                    }
                    break;
                }
            case SWT.MouseDoubleClick:
                {
                    Event mouseEvent = new Event();
                    mouseEvent.button = event.button;
                    mouseEvent.count = event.count;
                    mouseEvent.stateMask = event.stateMask;
                    mouseEvent.time = event.time;
                    mouseEvent.x = event.x;
                    mouseEvent.y = event.y;
                    this.notifyListeners(SWT.MouseDoubleClick, mouseEvent);
                    break;
                }
            case SWT.MouseWheel:
                {
                    Event keyEvent = new Event();
                    keyEvent.time = event.time;
                    keyEvent.keyCode = event.count > 0 ? SWT.ARROW_UP : SWT.ARROW_DOWN;
                    keyEvent.stateMask = event.stateMask;
                    this.notifyListeners(SWT.KeyDown, keyEvent);
                    if (this.isDisposed())
                    {
                        break;
                    }
                    event.doit = keyEvent.doit;
                    if (!event.doit)
                    {
                        break;
                    }
                    if (event.count != 0)
                    {
                        event.doit = false;
                        int oldIndex = this.table.getSelectionIndex();
                        if (event.count > 0)
                        {
                            this.select(Math.max(oldIndex - 1, 0));
                        }
                        else
                        {
                            this.select(Math.min(oldIndex + 1, this.getItemCount() - 1));
                        }
                        if (oldIndex != this.table.getSelectionIndex())
                        {
                            Event e = new Event();
                            e.time = event.time;
                            e.stateMask = event.stateMask;
                            this.notifyListeners(SWT.Selection, e);
                        }
                        if (this.isDisposed())
                        {
                            break;
                        }
                    }
                    break;
                }
            case SWT.Traverse:
                {
                    switch (event.detail)
                    {
                        case SWT.TRAVERSE_ARROW_PREVIOUS:
                        case SWT.TRAVERSE_ARROW_NEXT:
                            // The enter causes default selection and
                            // the arrow keys are used to manipulate the list
                            // contents so
                            // do not use them for traversal.
                            event.doit = false;
                            break;
                        case SWT.TRAVERSE_TAB_PREVIOUS:
                            event.doit = this.traverse(SWT.TRAVERSE_TAB_PREVIOUS);
                            event.detail = SWT.TRAVERSE_NONE;
                            return;
                    }
                    Event e = new Event();
                    e.time = event.time;
                    e.detail = event.detail;
                    e.doit = event.doit;
                    e.character = event.character;
                    e.keyCode = event.keyCode;
                    this.notifyListeners(SWT.Traverse, e);
                    event.doit = e.doit;
                    event.detail = e.detail;
                    break;
                }
            case SWT.Verify:
                {
                    Event e = new Event();
                    e.text = event.text;
                    e.start = event.start;
                    e.end = event.end;
                    e.character = event.character;
                    e.keyCode = event.keyCode;
                    e.stateMask = event.stateMask;
                    this.notifyListeners(SWT.Verify, e);
                    event.doit = e.doit;
                    break;
                }
        }
    }

    /**
     * adjusts the table column widths to fit inside of the table if the table
     * column data does not fill out the table area.
     */
    private void autoAdjustColumnWidthsIfNeeded(TableColumn[] tableColumns, int totalAvailWidth, int totalColumnWidthUsage)
    {

        int totalColumns = (tableColumns == null ? 0 : tableColumns.length);

        int scrollBarSize = 0;

        // determine if the vertical scroll bar needs to be taken into account
        if (this.table.getItemCount() > this.visibleItemCount)
        {
            scrollBarSize = (this.table.getVerticalBar() == null ? 0 : this.table.getVerticalBar().getSize().x);
        }

        // is there any extra space that the table is not using?
        if (totalAvailWidth > totalColumnWidthUsage + scrollBarSize)
        {

            int totalAmtToBeAllocated = (totalAvailWidth - totalColumnWidthUsage - scrollBarSize);

            int totalBufferAmount = (int) Math.floor(totalAmtToBeAllocated / totalColumns);

            // add the buffer amount to each column
            for (int colIndex = 0; colIndex < totalColumns; colIndex++)
            {
                tableColumns[colIndex].setWidth(tableColumns[colIndex].getWidth() + totalBufferAmount);
                totalAmtToBeAllocated -= totalBufferAmount;
            }

            // allocate any remainder to the last column.
            if (totalAmtToBeAllocated > 0)
            {
                tableColumns[totalColumns - 1].setWidth(tableColumns[totalColumns - 1].getWidth() + totalAmtToBeAllocated);
            }
        }
    }

    /**
     * Returns the Text control reference.
     * 
     * @return
     */
    public Text getTextControl()
    {
        this.checkWidget();
        return this.text;
    }
}