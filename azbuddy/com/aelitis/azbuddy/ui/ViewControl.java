package com.aelitis.azbuddy.ui;

import org.gudy.azureus2.ui.swt.plugins.*;


import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


public class ViewControl implements UISWTViewEventListener
{
	UISWTView	view	= null;
	
	TabFolder folder;
	Tab[] items;

	
    public boolean eventOccurred(UISWTViewEvent event) {
        switch (event.getType()) {
            case UISWTViewEvent.TYPE_CREATE:
                if (view != null)
                    return false;
                view = event.getView();
                break;

            case UISWTViewEvent.TYPE_INITIALIZE:
                initialize((Composite) event.getData());
                break;

            case UISWTViewEvent.TYPE_REFRESH:
                refresh();
                break;

            case UISWTViewEvent.TYPE_DESTROY:
            	destroy();
                break;

            case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
                break;

            case UISWTViewEvent.TYPE_FOCUSGAINED:
                break;

            case UISWTViewEvent.TYPE_FOCUSLOST:
                break;

            case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
                break;
        }
        return true;
    }

    private void initialize(Composite parent)
    {
    	parent.setLayout(new FillLayout(SWT.VERTICAL));
    	folder = new TabFolder(parent,SWT.NULL);
    	
    	UILogger.log("creating tabs");
    	
    	items = new Tab[] {
    			new ControlView(folder,SWT.NULL),
    			};
   	
    	for(Tab i : items)
    	{
    		i.initControl();
    	}

    }

    private void refresh()
    {
    	items[folder.getSelectionIndex()].refreshControl();
    }
    
    private void destroy()
    {
    	for(Tab i : items)
    	{
    		i.destroyControl();
    	}
    	view = null;
    }
}