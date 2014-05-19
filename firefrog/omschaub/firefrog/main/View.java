package omschaub.firefrog.main;



import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;


import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;


public class View implements UISWTViewEventListener {
	
	//The current Display
	private static Display display;
	
	//This view main GUI object
	public static Composite composite;
	
    
    //The Plugin interface
    PluginInterface pluginInterface;
    
    
  public View(PluginInterface pluginInterface) {
    this.pluginInterface = pluginInterface;
      
  }
	
	/**
	 * Here is the GUI initialization
	 */
	private void initialize(Composite parent) {
               
        
		// We store the Display variable as we'll need it for async GUI Changes
		display = parent.getDisplay();

                
        //Before starting the GUI we will initiate all of the pics
        ImageRepository.loadImages(display);
      
        //Main composite layout
		composite = new Composite(parent,SWT.NULL);
		
        //Make the composite have a grid layout
        GridLayout layout = new GridLayout();

        //set num of columns and margins
        layout.numColumns = 1;
		//layout.marginWidth=2;
		//layout.marginHeight=0;

        //set the composite to the layout
        composite.setLayout(layout);
		

        
        //Button for login screen
        Button dm = new Button(composite,SWT.PUSH);
        GridData gridData = new GridData(GridData.BEGINNING);
        gridData.horizontalSpan = 1;
        dm.setText("Open FireFrog");
        dm.setLayoutData(gridData);
        dm.setToolTipText("Open FireFrog");
        
        //Listener for the button
        dm.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
             
             DownloadManagerShell.open();
             if(Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("FireFrog_tray_use",true)){
                 if(!TrayUtil.isOpen()){
                     TrayUtil.open();
                 }
             }
             
            }
        });
        
        final boolean tray_use_boolean = Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("FireFrog_tray_use",true);
        
        //Check for using tray icon
        final Button tray_use = new Button(composite,SWT.CHECK);
        gridData = new GridData(GridData.BEGINNING);
        gridData.horizontalSpan = 1;
        tray_use.setText("Use TrayIcon");
        tray_use.setLayoutData(gridData);
        tray_use.setSelection(tray_use_boolean);
        
        //Listener for tray_use
        tray_use.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
               
               Plugin.getPluginInterface().getPluginconfig().setPluginParameter("FireFrog_tray_use",tray_use.getSelection());
               if(tray_use.getSelection()){
                   if(!TrayUtil.isOpen()){
                       TrayUtil.open();
                   }
               }else
                   if(TrayUtil.isOpen())
                       TrayUtil.close();
                
               }
           });
        
        TorrentListener.isSeeding();
        
        if(Plugin.isPluginAutoOpen()){ 
            DownloadManagerShell.open();
            if(tray_use_boolean){
                if(!TrayUtil.isOpen()){
                    TrayUtil.open();
                }
            }
        }
        
        
	composite.layout();
	//END
	}
	

/**
 * Super delete function... runs at close of plugin from within Azureus
 */

private void delete() {
    TrayUtil.close();
    if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
        Utilities.getDisplay().asyncExec(new Runnable ()
                {
            public void run () 
            {
                if(DownloadManagerShell.DOWNLOAD_MANAGER_SHELL != null && !DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.isDisposed()){
                    DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.dispose();
                }  
            }
                }
        );
    } 
 
}

private boolean isCreated = false;
public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
	    
    	case UISWTViewEvent.TYPE_CREATE:
	        if (isCreated)
	          return false;
	
	        isCreated = true;
	        break;

    	case UISWTViewEvent.TYPE_DESTROY:
	          delete();
	          isCreated = false;
	          break;
	          
        case UISWTViewEvent.TYPE_INITIALIZE:
            initialize((Composite)event.getData());
            break;
    }

    return true;
  }


//EOF
}
