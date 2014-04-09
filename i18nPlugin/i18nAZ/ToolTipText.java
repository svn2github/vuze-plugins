/*
 * ToolTipText.java
 *
 * Created on April 7, 2014, 2:53 PM
 */
package i18nAZ;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;
/**
 * ToolTipText class.
 * 
 * @author Repris d'injustice
 */
public class ToolTipText
{
    static void config(final Control control)
    {
        Listener mouseListener = new Listener()
        {
            List<Object[]> hottedItems = new ArrayList<Object[]>();            
            
            public Event cloneEvent(Event event)
            {
                Event e = new Event();

                Field[] fields = Event.class.getDeclaredFields();
                for (int i = 0; i < fields.length; i++)
                {
                    fields[i].setAccessible(true);
                    try
                    {
                        fields[i].set(e, fields[i].get(event));
                    }
                    catch (IllegalArgumentException | IllegalAccessException e1)
                    {
                    }
                }
                return e;

            }

            public void handleItemEvent(Item item, int columnIndex, Event event, int eventType)
            {
                Event e = this.cloneEvent(event);
                e.widget = item;
                e.data = columnIndex;
                e.type = eventType;

                Listener[] listeners = item.getListeners(e.type);
                for (int k = 0; k < listeners.length; k++)
                {
                    TypedListener typedListener = (TypedListener) listeners[k];
                    typedListener.handleEvent(e);
                }                                        
            }

            @Override
            public void handleEvent(Event e)
            {
                if (e.type == SWT.MouseEnter || e.type == SWT.MouseExit)
                {
                    e.type = SWT.MouseMove;
                }
                
                Item item = Util.getItem(control, new Point(e.x, e.y));
                int columnIndex = -1;
                
                int columnCount = 0;
                if (item instanceof ToolItem)
                {
                    columnCount = 1;
                }
                else if (item instanceof TableItem)
                {
                    columnCount = ((Table) control).getColumnCount();
                }
                else if (item instanceof TreeItem)
                {
                    columnCount = ((Tree) control).getColumnCount();
                }
                
                Rectangle bounds = null;                
                for (int i = 0; i < columnCount && item != null; i++)
                {                    
                    if (item instanceof ToolItem)
                    {
                        bounds = ((ToolItem) item).getBounds();
                    }
                    else if (item instanceof TableItem)
                    {
                        bounds = ((TableItem) item).getBounds(i);
                    }
                    else if (item instanceof TreeItem)
                    {
                        bounds = ((TreeItem) item).getBounds(i);
                    }
                    
                    if (bounds.contains(e.x, e.y) == true)
                    {
                        columnIndex = i;
                        break;
                    }
                }
                
                if (e.type == SWT.MouseMove)
                {
                    boolean hotted = false;
                    for(int i = this.hottedItems.size() - 1; i >= 0 ; i--)
                    {
                        if(this.hottedItems.get(i)[0].equals(item) && (int)this.hottedItems.get(i)[1] == columnIndex)
                        {
                            hotted = true;
                            continue;
                        }
                        handleItemEvent((Item)this.hottedItems.get(i)[0], (int)this.hottedItems.get(i)[1], e, SWT.MouseExit);
                        this.hottedItems.remove(i);
                    }                    
                    if(hotted == false && item != null && item.isDisposed() == false && columnIndex > -1)
                    {
                        handleItemEvent(item, columnIndex, e, SWT.MouseEnter);
                        this.hottedItems.add(new Object[]{item, columnIndex});
                        return;
                    }
                }
                if(item != null && columnIndex > -1)
                {                 
                    handleItemEvent(item, columnIndex, e, e.type);                    
                }                    
            }
        };

        control.addListener(SWT.MouseDoubleClick, mouseListener);
        control.addListener(SWT.MouseDown, mouseListener);
        control.addListener(SWT.MouseEnter, mouseListener);
        control.addListener(SWT.MouseExit, mouseListener);
        control.addListener(SWT.MouseHover, mouseListener);
        control.addListener(SWT.MouseMove, mouseListener);
        control.addListener(SWT.MouseUp, mouseListener);
        control.addListener(SWT.MouseWheel, mouseListener);
    }
    static void set(Item item, String toolTipTextID)
    {
        ToolTipText.set(item, 0, toolTipTextID, null, null);
    }

    static void set(Item item, int index, String toolTipTextID)
    {
        ToolTipText.set(item, index, toolTipTextID, null, null);
    }

    static void set(Item item, int index, String toolTipTextID, String[] titleParams)
    {
        ToolTipText.set(item, index, toolTipTextID, titleParams, null);
    }

    static void set(Item item, int index, String toolTipTextID, String[] titleParams, String[] messageParams)
    {
        Control parent = (Control) Util.invoke(item, "getParent");
        ToolTipText.set(parent.getShell(), item, index, toolTipTextID, titleParams, messageParams);
    }

    static void set(Control control, String toolTipTextID)
    {
        ToolTipText.set(control, toolTipTextID, null);
    }

    static void set(Control control, String toolTipTextID, String[] titleParams)
    {
        ToolTipText.set(control.getShell(), control, 0, toolTipTextID, titleParams, null);
    }

    private static void set(Shell shell, Widget widget, int index, String toolTipTextID, String[] titleParams, String[] messageParams)
    {
        ToolTipTrackListener toolTipTrackListener = new ToolTipTrackListener(shell, index, toolTipTextID, titleParams, messageParams);

        ToolTipTrackListener.removeToolTipAndListeners(widget, SWT.MouseExit, index);
        ToolTipTrackListener.removeToolTipAndListeners(widget, SWT.MouseHover, index);       
      
        TypedListener typedListener = new TypedListener(toolTipTrackListener);
        Util.addTypedListenerAndChildren(widget, SWT.MouseExit, typedListener);
        Util.addTypedListenerAndChildren(widget, SWT.MouseHover, typedListener);
    } 
    static void unconfig(final Control control)
    {            
        Util.removeTypedListenerAndChildren(control, SWT.MouseDoubleClick);
        Util.removeTypedListenerAndChildren(control, SWT.MouseDown);
        Util.removeTypedListenerAndChildren(control, SWT.MouseEnter);
        Util.removeTypedListenerAndChildren(control, SWT.MouseExit);
        Util.removeTypedListenerAndChildren(control, SWT.MouseHover);
        Util.removeTypedListenerAndChildren(control, SWT.MouseMove);
        Util.removeTypedListenerAndChildren(control, SWT.MouseUp);
        Util.removeTypedListenerAndChildren(control, SWT.MouseWheel);
    }   
}
class ToolTipTrackListener implements MouseTrackListener
{
    private Shell shell = null;
    private int index = 0;
    private ToolTip toolTip = null;
    private String textID = null;
    private String title = null;
    private String message = null;

    private void dispose()
    {
        if (this.toolTip != null)
        {
            if (!this.toolTip.isDisposed())
            {
                this.toolTip.setVisible(false);
                this.toolTip.dispose();
            }
            this.toolTip = null;
        }
    }
    static void removeToolTipAndListeners(Widget widget, int eventType, int index)
    {
        List<Listener> listeners = new  ArrayList<Listener>();
        listeners.addAll(Arrays.asList(widget.getListeners(eventType)));
        
        for (int i = 0; i < listeners.size(); i++)
        {
            if (!(listeners.get(i) instanceof TypedListener))
            {
                continue;
            }
            TypedListener typedListener = (TypedListener) listeners.get(i);
            if (typedListener.getEventListener() instanceof ToolTipTrackListener)
            {
                if (((ToolTipTrackListener) typedListener.getEventListener()).getIndex() == index)
                {
                    Util.removeTypedListenerAndChildren(widget, eventType, new TypedListener[]{typedListener});
                    ((ToolTipTrackListener) typedListener.getEventListener()).dispose();
                }
            }
        }
    }
    
    ToolTipTrackListener(Shell shell, int index, String textID, String[] titleParams, String[] messageParams)
    {
        this.shell = shell;
        this.index = index;
        this.textID = textID;        
            
        if (textID == null)             
        {
            if (titleParams == null)
            {
                this.title = "!ERROR!";
            }
            else
            {
                this.title = titleParams[0];
            } 
            if (messageParams == null)
            {
                this.message = "!ERROR!";
            }
            else
            {
                this.message = messageParams[0];
            }           
        }
        else
        {
            if (titleParams == null)
            {
                this.title = i18nAZ.viewInstance.getLocalisedMessageText(this.textID + ".Title");
            }
            else
            {
                this.title = i18nAZ.viewInstance.getLocalisedMessageText(this.textID + ".Title", titleParams);
            }
            if (messageParams == null)
            {
                this.message = i18nAZ.viewInstance.getLocalisedMessageText(this.textID + ".Message");
            }
            else
            {
                this.message = i18nAZ.viewInstance.getLocalisedMessageText(this.textID + ".Message", messageParams);
            }
        }
    }
    
    int getIndex()
    {
        return this.index;
    }

    @Override
    public void mouseEnter(MouseEvent e)
    {
    }

    @Override
    public void mouseExit(MouseEvent e)
    {
        e.data = (e.data == null) ? 0 : e.data;
        if((int) e.data != this.getIndex())
        {
            return;
        }
        
        
        if (this.toolTip != null && this.toolTip.isDisposed() == false)
        {
            this.toolTip.setVisible(false);
        }
    }

    @Override
    public void mouseHover(MouseEvent e)
    {
        e.data = (e.data == null) ? 0 : e.data;
        Point point = null;
        if (e.widget instanceof Control)
        {
            Control control = ((Control) e.widget);
            point = control.toDisplay(0, 0);
            point = new Point(point.x, point.y + control.getSize().y - control.getBorderWidth());
        }
        else if (e.widget instanceof Item)
        {          
            Control parent = (Control) Util.invoke(e.widget, "getParent");
            Rectangle bounds = null;
            if (e.widget instanceof ToolItem)
            {
                bounds = (Rectangle) Util.invoke(e.widget, "getBounds");
            }
            else
            {
                bounds = (Rectangle) Util.invoke(e.widget, "getBounds", new Object[] { (int) e.data });
            }
            point = parent.getLocation();
            point = parent.getParent().toControl(parent.toDisplay(0,0));
            point = parent.getParent().toDisplay(point);            
            point.x += bounds.x;
            point.y += bounds.y + bounds.height;
        }
        if (this.toolTip == null || this.toolTip.isDisposed() == true)
        {
            this.toolTip = null;
            this.toolTip = new ToolTip(this.shell, SWT.NULL);
        }
        if((int) e.data != this.getIndex())
        {
            return;
        }   
        
        this.toolTip.setText(this.title);
        this.toolTip.setMessage(this.message);
        this.toolTip.setLocation(point);
        this.toolTip.setVisible(true);
    }
}
