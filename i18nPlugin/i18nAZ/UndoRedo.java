/*
 * UndoRedo.java
 *
 * Created on April 5, 2014, 2:25
 */

package i18nAZ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * UndoRedo.java
 * 
 * @author Repris d'injustice
 */
public class UndoRedo
{
    
    class EventListener
    {
        int eventType;
        Listener listener;
        EventListener(int eventType, Listener listener)
        {
            this.eventType = eventType;
            this.listener = listener;
        }
    }
    class UndoRedoObject
    {
        String text;
        Point selection;
    }
    private static final int MAX_STACK_SIZE = 500;

    private boolean onDo;

    private Map<String, List<UndoRedoObject>> undoStacks = new HashMap<String, List<UndoRedoObject>>();
    private Map<String, List<UndoRedoObject>> redoStacks = new HashMap<String, List<UndoRedoObject>>();

    private String curentKey = null;
    private StyledText styledText;
       
    private List<EventListener> eventListeners = null;

    public UndoRedo(StyledText styledText)
    {
        this.styledText = styledText;
        this.styledText.addExtendedModifyListener(new ExtendedModifyListener()
        {
            @Override
            public void modifyText(ExtendedModifyEvent event)
            {
                if(UndoRedo.this.onDo == true)
                {
                    return;
                }
                UndoRedoObject undoRedoObject = new UndoRedoObject();
                undoRedoObject.text = UndoRedo.this.styledText.getText();
                undoRedoObject.selection = UndoRedo.this.styledText.getSelection();
                synchronized(UndoRedo.this.undoStacks)
                {
                    if(UndoRedo.this.curentKey == null || UndoRedo.this.undoStacks.containsKey(UndoRedo.this.curentKey) == false)
                    {
                        return;
                    }

                    List<UndoRedoObject> undoStack = UndoRedo.this.undoStacks.get(UndoRedo.this.curentKey);                   
                    
                    if (undoStack.size() > 0)
                    {
                        if(undoStack.get(0).text.equals(UndoRedo.this.styledText.getText()) == true)
                        {
                            return;
                        }
                    }
                    if (undoStack.size() == MAX_STACK_SIZE)                 
                    {
                        undoStack.remove(undoStack.size() - 1);                    
                    }
                    undoStack.add(0, undoRedoObject);
                }
              
                UndoRedo.this.notifyListeners(SWT.CHANGED, null);
            }
        });
    }
    synchronized public void addListener (int eventType, Listener listener) 
    {       
        if (listener == null) 
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        
        if (this.eventListeners == null) 
        {
            this.eventListeners = new ArrayList<EventListener>();            
        }
        this.eventListeners.add(new EventListener(eventType, listener));
    }
    synchronized public boolean canRedo()
    {
        if(this.curentKey == null || this.undoStacks.containsKey(this.curentKey) == false)
        {
            return false;
        }

        List<UndoRedoObject> redoStack = this.redoStacks.get(this.curentKey);
        
        return redoStack.size() > 0;
    }
    synchronized public boolean canUndo()
    {
        if(this.curentKey == null || this.undoStacks.containsKey(this.curentKey) == false)
        {
            return false;
        }

        List<UndoRedoObject> undoStack = this.undoStacks.get(this.curentKey);        
        
        return undoStack.size() > 1;
    }
    synchronized private void clear()
    {
        if(this.curentKey == null || this.undoStacks.containsKey(this.curentKey) == false)
        {
            return;
        }

        List<UndoRedoObject> undoStack = this.undoStacks.get(this.curentKey);
        List<UndoRedoObject> redoStack = this.redoStacks.get(this.curentKey);
        
        undoStack.clear();
        redoStack.clear();
            
        this.styledText.setSelectionRange(this.styledText.getText().length(), 0);

        UndoRedoObject undoRedoObject = new UndoRedoObject();
        undoRedoObject.text = this.styledText.getText();
        undoRedoObject.selection = this.styledText.getSelection();
        undoStack.add(0, undoRedoObject);
        
        this.notifyListeners(SWT.CHANGED, null);
    }
    synchronized public void notifyListeners(int eventType, Event event) 
    {       
        if (event == null) event = new Event ();
        event.type = eventType;
        event.display = styledText.getDisplay();
        event.widget = styledText;
        event.data = this;        
        
        if (this.eventListeners != null) 
        {            
            for(int i = 0; i < this.eventListeners.size(); i++)
            {
                if(this.eventListeners.get(i).eventType == eventType)
                {
                    this.eventListeners.get(i).listener.handleEvent(event); 
                }             
            }         
        }
    }
    synchronized public void redo()
    {
        if(this.curentKey == null || this.undoStacks.containsKey(this.curentKey) == false)
        {
            return;
        }

        List<UndoRedoObject> undoStack = this.undoStacks.get(this.curentKey);
        List<UndoRedoObject> redoStack = this.redoStacks.get(this.curentKey);
        
        if (redoStack.size() > 0)
        {
            this.onDo = true;
            UndoRedoObject undoRedoObject = redoStack.remove(0);
            this.styledText.setText(undoRedoObject.text);
            this.styledText.setSelection(undoRedoObject.selection);
            undoStack.add(0, undoRedoObject);
            this.notifyListeners(SWT.CHANGED, null);
            this.onDo = false;
        }
    }
    synchronized public void removeListener(int eventType, Listener listener) 
    {       
        if (listener == null) 
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        
        if (this.eventListeners != null) 
        {            
            for(int i =  this.eventListeners.size(); i >= 0 ; i--)
            {
                if(this.eventListeners.get(i).listener.equals(listener) && this.eventListeners.get(i).eventType == eventType)
                {
                    this.eventListeners.remove(i);
                }
            }         
        }
    }
    synchronized public void setKey(String key)
    {
        this.curentKey = key;
        if( this.curentKey == null || this.undoStacks.containsKey(this.curentKey) == true)
        {
            this.notifyListeners(SWT.CHANGED, null);
            return;
        }
        this.undoStacks.put(this.curentKey, new LinkedList<UndoRedoObject>());
        this.redoStacks.put(this.curentKey, new LinkedList<UndoRedoObject>());
        this.clear();
    }
    synchronized public void undo()
    {
        if(this.curentKey == null || this.undoStacks.containsKey(this.curentKey) == false)
        {
            return;
        }

        List<UndoRedoObject> undoStack = this.undoStacks.get(this.curentKey);
        List<UndoRedoObject> redoStack = this.redoStacks.get(this.curentKey);
        
        if (undoStack.size() > 1)
        {
            this.onDo = true;
            
            redoStack.add(0, undoStack.remove(0));
             UndoRedoObject undoRedoObject = undoStack.get(0);
            this.styledText.setText(undoRedoObject.text);
            this.styledText.setSelection(undoRedoObject.selection);
            this.notifyListeners(SWT.CHANGED, null);
            this.onDo = false;
        }
    }
    synchronized public void unsetKey()
    {
        this.setKey(null);
    }
}


