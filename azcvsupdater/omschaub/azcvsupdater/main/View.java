package omschaub.azcvsupdater.main;

import omschaub.azcvsupdater.utilities.ButtonStatus;
import omschaub.azcvsupdater.utilities.ColorUtilities;
import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.DownloaderMain;
import omschaub.azcvsupdater.utilities.imagerepository.ImageRepository;

// This breaks encapsulation - we need to provide this in the plugin API!

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.utils.Formatters;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;


public class View implements UISWTViewEventListener {
    
    //New API settings
    private boolean isCreated = false;    
    static final String VIEWID = "AZCVSUpdater_View";
    
    
	//The current Display
	private static Display display;
	
	//This view main GUI object
	private Composite composite;
	
    //Setting variables up
    public static boolean DML_BOOLEAN = false;
    static boolean LOAD_DIR = false;
    static boolean AUTO_ONCE = false;
    static boolean PLUGIN_AUTO_GO = false;
    
    //SETTING UP PUBLIC WIDGETS
    static Composite composite_for_tab1;
    static Composite composite_for_tab6;
    static SashForm sash;
    static CTabFolder tab;
    static CTabItem tab6 ;
	
	//The Plugin interface
    private PluginInterface pluginInterface;
    private static PluginInterface pi_static;
    private static Formatters formatters;
    
  View(PluginInterface pluginInterface) {
    this.pluginInterface = pluginInterface;
   
  }
	
  
  private void initialize(Composite parent) {
		
      
      
		// We store the Display variable as we'll need it for async GUI Changes
		display = parent.getDisplay();
		
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth=2;
        layout.marginHeight=0;
        parent.setLayout(layout);
        
        GridData gridData = new GridData(GridData.FILL_BOTH);
        parent.setLayoutData(gridData);
        
        
        
		//get and set sash information
		if (pluginInterface.getPluginconfig().getPluginIntParameter("Azcvsupdater_sash_info", 60) == 0)
            pluginInterface.getPluginconfig().setPluginParameter("Azcvsupdater_sash_info", 87);
		
		//Load Images from the ImageRepository
		ImageRepository.loadImages(display);

		if (true) {
			if ((pluginInterface.getPluginconfig().getPluginIntParameter("WebUpdatePeriod",60) * 60 * 1000) < 900000){
                pluginInterface.getPluginconfig().setPluginParameter("WebUpdatePeriod",15);
				
			}
		}

        //Setting the static pi to the main pi for all calls after the View
        pi_static = pluginInterface;
        formatters = pi_static.getUtilities().getFormatters();
        
        //main composite layout
		this.composite = new Composite(parent,SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth=2;
		layout.marginHeight=0;
		composite.setLayout(layout);
		
        gridData = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(gridData);
        
        
		//SashForm
		sash = new SashForm(composite, SWT.VERTICAL );
		GridData sashGridData = new GridData(GridData.FILL_BOTH);
		sash.setLayoutData(sashGridData);
		
		
		
		//tab start
		tab = new CTabFolder(sash,SWT.TOP | SWT.BORDER);
		gridData = new GridData(GridData.FILL_BOTH);
		tab.setBackground(ColorUtilities.getDark_BackgroundColor());
		tab.setLayoutData(gridData);
	
        if(pluginInterface.getPluginconfig().getUnsafeBooleanParameter("GUI_SWT_bFancyTab")){
		    tab.setSimple(false);
			tab.setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
			
		      try {
		          tab.setSelectionBackground(
		                  new Color[] {display.getSystemColor(SWT.COLOR_LIST_BACKGROUND), 
		                               display.getSystemColor(SWT.COLOR_GRAY), 
		                               display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)},
                                       //tab.getBackground()},
		                  new int[] {15,25}, true);
		          
		        } catch (NoSuchMethodError e) { 
		          /** < SWT 3.0M8 **/ 
		          tab.setSelectionBackground(new Color[] {display.getSystemColor(SWT.COLOR_LIST_BACKGROUND) },
		                                                      new int[0]);
		        }    
		}
		
		
		CTabItem tab1 = new CTabItem(tab,SWT.NONE);
		tab6 = new CTabItem(tab,SWT.NONE);
		
		tab1.setText("CVS Update");
		tab6.setText("Insert / Backup Files");
		
        tab1.setImage(ImageRepository.getImage("bullet"));
        tab6.setImage(ImageRepository.getImage("bullet"));
                
		//Initialize StatusBox
		StatusBox.open(pluginInterface);
		
		//composite for tab1
		composite_for_tab1 = new Composite(tab,SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		composite_for_tab1.setLayout(layout);
		gridData = new GridData(GridData.FILL_BOTH);
		composite_for_tab1.setLayoutData(gridData);
        
		//Initiate graphics for tabs 5, 1, 2, 3 and 4 **in that order**
		Tab1.open();
		
    if (pluginInterface.getPluginconfig().getPluginIntParameter("Azcvsupdater_sash_info", 60) > 100)
        pluginInterface.getPluginconfig().setPluginParameter("Azcvsupdater_sash_info", 100);
     sash.setWeights(new int[] {pluginInterface.getPluginconfig().getPluginIntParameter("Azcvsupdater_sash_info", 60),100 - pluginInterface.getPluginconfig().getPluginIntParameter("Azcvsupdater_sash_info", 60)});
     
     tab.addListener(SWT.Resize, new Listener() {
        public void handleEvent(Event e) {
          int[] weights = sash.getWeights();
          int iSashValue = weights[0] * 100 / (weights[0] + weights[1]);
          pluginInterface.getPluginconfig().setPluginParameter("Azcvsupdater_sash_info", iSashValue);
        }
      });
 
    //Tab6 Scrolled and composite and initialization
	final ScrolledComposite scrollComposite = new ScrolledComposite(tab, SWT.V_SCROLL | SWT.H_SCROLL);
    scrollComposite.setExpandVertical(true);
    scrollComposite.setExpandHorizontal(true);
	
    composite_for_tab6 = new Composite(scrollComposite, SWT.NONE);
	gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan  = 3;
    composite_for_tab6.setLayoutData(gridData);
    GridLayout tab6_comp_layout = new GridLayout();
    tab6_comp_layout.numColumns = 3;
    composite_for_tab6.setLayout(tab6_comp_layout);
	
    
    Tab6.open();
    
    scrollComposite.setContent(composite_for_tab6);
    scrollComposite.addControlListener(new ControlAdapter() {
        public void controlResized(ControlEvent e) {
                Rectangle r = scrollComposite.getClientArea();
                
                scrollComposite.setMinSize(composite_for_tab6.computeSize(r.width,SWT.DEFAULT));
        }
    });
     
		
	//initial setting of buttons
    ButtonStatus.set(true,true,false,true,true);
    
    //tab controls
    tab1.setControl(composite_for_tab1);
    tab6.setControl(scrollComposite);
    tab.layout();
	composite_for_tab1.layout();
    
	Tab1Utils.loadDirectory(pluginInterface.getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
	
    //Tab4Utils.backup_loadDirectory();
    
    DownloaderMain.autoDownloader(pluginInterface);
	
    
    //Timer -- main timer startup
	Timers.checkForNewVersion(pluginInterface);
	
	
    if(composite != null && !composite.isDisposed()){
        //composite.pack();
    	composite.layout();
    }
    
    if(StatusBox.status_group != null && !StatusBox.status_group.isDisposed()){
        //StatusBox.status_group.pack();
        StatusBox.status_group.layout();
    }
    
    
    tab.setSelection(tab1);
    
	//END
	}

/**
 * Super delete function... runs at close of plugin from within Azureus
 */

void delete() {
	
   Timers.destroyTimers();
	
    ColorUtilities.unloadColors();
    
	if(composite != null && !composite.isDisposed())
	    composite.dispose();
    
	isCreated = false;
}

public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
    
    
    case UISWTViewEvent.TYPE_CREATE:
        if (isCreated)
          return false;
        
        isCreated = true;
        break;
    
        
    case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;
        
        
        
    case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;
        
    }

    return true;
  }

/**
 * Gets the pluginInterface from View
 * @return pluginInterface
 */
public static PluginInterface getPluginInterface(){
    return pi_static;
}

/**
 * Gets the display from View
 * @return display
 */
public static Display getDisplay(){
    return display;
}

public static String formatDate(long millis) {
	return formatters.formatDate(millis);
}

public static void asyncExec(Runnable r) {
	if (display == null) {return;}
	if (display.isDisposed()) {return;}
	display.asyncExec(r);
}

public static void syncExec(Runnable r) {
	if (display == null) {return;}
	if (display.isDisposed()) {return;}
	display.syncExec(r);
}

public static String getJARFileDestination() {
	if (pi_static.getUtilities().isOSX()) {
	    return DirectoryUtils.getInstallDirectory() + "/" + PluginManager.getDefaults().getApplicationName() + ".app/Contents/Resources/Java/Azureus2.jar";
	}
	else {
	    return new java.io.File(DirectoryUtils.getInstallDirectory(), "Azureus2.jar").getPath(); 
	}

}

//EOF
}
