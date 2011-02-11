/*
 * Created on Oct 21, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import java.net.URL;

import omschaub.azcvsupdater.main.View;
import omschaub.azcvsupdater.utilities.ButtonStatus;
import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.DownloaderMain;
import omschaub.azcvsupdater.utilities.Time;
import omschaub.azcvsupdater.utilities.TorrentUtils;
import omschaub.azcvsupdater.utilities.URLReader;
import omschaub.azcvsupdater.utilities.download.AltCVSGet;
import omschaub.azcvsupdater.utilities.download.MainCVSGetManual;
import omschaub.azcvsupdater.utilities.imagerepository.ImageRepository;
import omschaub.azcvsupdater.main.Constants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.gudy.azureus2.plugins.ui.UIException;

public class Tab1_Subtab_1 {

    public static Label displayVersion;
    public static Label version_color;
    public static Label serverTimeStamp;
    public static Label timestamp_color;
    public static Label lastCheck;
    public static Label nextAutoCheck;
    
    public static String version = "Checking....";
    
    public static ToolItem download_tb;
    public static ToolItem versionCheck;
    

    
    static Composite open(Composite parent){
        Composite cvsgroup = new Composite(parent, SWT.NULL);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        
        gridData.horizontalSpan =2;
        //gridData.verticalSpan = 5;
        
        gridData.horizontalAlignment = GridData.FILL;
        //gridData.grabExcessHorizontalSpace = true;
            
        cvsgroup.setLayoutData(gridData);
        
        GridLayout composite2_layout = new GridLayout();
        composite2_layout.numColumns = 6;
        composite2_layout.makeColumnsEqualWidth = false;
        composite2_layout.marginHeight = 3;
        composite2_layout.verticalSpacing = 0;
        cvsgroup.setLayout(composite2_layout);
        

        
        //new sub composite 1
        Composite mini_comp1 = new Composite(cvsgroup, SWT.NULL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =1;
        //gridData.widthHint=300;
        gridData.horizontalAlignment = GridData.FILL;
        mini_comp1.setLayoutData(gridData);
        GridLayout mini_comp1_layout = new GridLayout();
        mini_comp1_layout.marginHeight = 0;
        //mini_comp1_layout.marginWidth = 0;
        mini_comp1_layout.numColumns = 3;
        mini_comp1_layout.makeColumnsEqualWidth = false;
        mini_comp1.setLayout(mini_comp1_layout);
        


        
        //Label one in sub_on_mini_comp1
        displayVersion = new Label(mini_comp1,SWT.NULL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING  );
        gridData.horizontalSpan = 1;
        displayVersion.setLayoutData(gridData);
        displayVersion.setText("Latest version: ");
        
        
        //Colored Version -- in mini_comp1
        version_color = new Label(mini_comp1,SWT.NULL );
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        version_color.setLayoutData(gridData);
        version_color.setForeground( View.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        version_color.setText(version);
        
        
      
        version_color.setCursor( new Cursor(View.getDisplay(),SWT.CURSOR_HAND));
        version_color.addListener(SWT.MouseDoubleClick, new Listener() {
            public void handleEvent(Event e) {
            	try {View.getPluginInterface().getUIManager().openURL(new URL(Constants.AZUREUS_CVS_URL));}
                catch (Exception exc) {}
            }
        });
        version_color.setToolTipText("Double click to check the Vuze Dev page, right click to copy URL to clipboard");
        
        Menu popupmenu_link = new Menu(View.composite_for_tab1);
        
        MenuItem clipboard = new MenuItem(popupmenu_link, SWT.PUSH);
        clipboard.setText("Copy URL to clipboard");
        clipboard.addListener(SWT.Selection, new Listener() {
            public void handleEvent (Event e){
                try {
                    View.getPluginInterface().getUIManager().copyToClipBoard(Constants.AZUREUS_CVS_URL);
                } catch (UIException e1) {
                    
                    e1.printStackTrace();
                }
            }
        });
        version_color.setMenu(popupmenu_link);
        
        
        try{
            //View.URLReader(pluginInterface);
            ButtonStatus.set(true,true,false,true,true);
        }catch(Exception e) {
            version_color.setText("Could Not Update!");
            e.printStackTrace(); 
        }
        
//       serverTimeStamp Label -- inside cvsgroup
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 2;
        
        serverTimeStamp = new Label(mini_comp1,SWT.NULL);
        serverTimeStamp.setLayoutData(gridData);
        serverTimeStamp.setText("Latest version released: ");
        
            
        //Colored TimeStamp -- in mini_comp
        timestamp_color = new Label(mini_comp1, SWT.NULL );
        gridData = new GridData(GridData.BEGINNING );
        gridData.horizontalSpan = 1;
        timestamp_color.setLayoutData(gridData);
        timestamp_color.setForeground( View.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        if(URLReader.get_serverTimeStampVersion() == null)
            timestamp_color.setText("Checking...");
        else
            timestamp_color.setText(URLReader.get_serverTimeStampVersion());
        timestamp_color.setCursor(new Cursor(View.getDisplay(),SWT.CURSOR_HAND));
        
        timestamp_color.addListener(SWT.MouseDoubleClick, new Listener() {
            public void handleEvent(Event e) {
            	try {View.getPluginInterface().getUIManager().openURL(new URL(Constants.AZUREUS_CVS_URL));}
            	catch (Exception exc) {}
            }
        });
        
        timestamp_color.setToolTipText("Double click to check the Vuze Dev page, right click to copy URL to clipboard");
        timestamp_color.setMenu(popupmenu_link);
        
        try{
            URLReader.newGetURL();
            ButtonStatus.set(true,true,false,true,true);
        }catch(Exception e) {
            timestamp_color.setText("Could Not Update!");
            e.printStackTrace(); 
        }

        
        //Divider Line
        Label sep_one = new Label(cvsgroup,SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        sep_one.setLayoutData(gridData);
        
        
        
        
//      version check time mini sub composite 
        Composite mini_comp_version = new Composite(cvsgroup, SWT.NONE);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =2;
        //gridData.widthHint=300;
        gridData.horizontalAlignment = GridData.FILL;
        mini_comp_version.setLayoutData(gridData);
        GridLayout mini_comp_version_layout = new GridLayout();
        mini_comp_version_layout.marginHeight = 0;
        mini_comp_version_layout.marginWidth = 0;
        mini_comp_version_layout.numColumns = 3;
        mini_comp_version_layout.makeColumnsEqualWidth = false;
        
        mini_comp_version.setLayout(mini_comp_version_layout);
        
        //Latest Version Checked --- inside cvsgroup
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING );
        gridData.horizontalSpan = 1;
        lastCheck = new Label(mini_comp_version,SWT.NULL);
        lastCheck.setLayoutData(gridData);
        
        //seperator for times
        Label timeSeparator = new Label(mini_comp_version,SWT.SEPARATOR);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING  );
        gridData.heightHint =15;
        gridData.verticalSpan=1;
        timeSeparator.setLayoutData(gridData);
        
        
        String dateCurrentTime = Time.getCVSTime();
        View.getPluginInterface().getPluginconfig().setPluginParameter("dateNextTime",Time.getCVSTimeNext((View.getPluginInterface().getPluginconfig().getPluginIntParameter("WebUpdatePeriod") * 60 * 1000)));
        lastCheck.setText("Latest Check: " + dateCurrentTime);
        
        
        //Next Auto Check --inside cvsgroup
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING  );
        gridData.horizontalSpan = 1;
        nextAutoCheck = new Label(mini_comp_version,SWT.NULL);
        nextAutoCheck.setLayoutData(gridData);
        nextAutoCheck.setText("Next Auto Check: " +  View.getPluginInterface().getPluginconfig().getPluginStringParameter("dateNextTime","Checking..."));
        
        Label updates = new Label(mini_comp_version,SWT.NULL);
        updates.setText("Beta version information updates every " + (View.getPluginInterface().getPluginconfig().getPluginIntParameter("WebUpdatePeriod")) + " Minutes");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 3;
        updates.setLayoutData(gridData);
        
        
        
        
        //Divider Line
        Label sep_for_mini = new Label(cvsgroup,SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        sep_for_mini.setLayoutData(gridData);
        

        //Toolbar in cvs group
        final ToolBar toolBar = new ToolBar(cvsgroup,SWT.HORIZONTAL | SWT.FLAT);      
        
        versionCheck = new ToolItem(toolBar,SWT.PUSH);
        versionCheck.setImage(ImageRepository.getImage("manual_update"));
        versionCheck.setToolTipText("Manually update web information");
        versionCheck.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                
                    View.getDisplay().asyncExec(new Runnable (){
                        public void run () {
                            try {
                                //Timers.destroy_checkForNewVersion();
                                URLReader.newGetURL();
                                if(lastCheck == null && lastCheck.isDisposed())
                                    return;
                                lastCheck.setText("Latest Check: " + Time.getCVSTime());
                                nextAutoCheck.setText("Next Auto Check: " + View.getPluginInterface().getPluginconfig().getPluginStringParameter("dateNextTime","Checking..."));
                                }catch(Exception f) {
                                    version_color.setText("Could Not Update!");
                                    f.printStackTrace(); 
                                }
                                version_color.setText(version); 
                                DownloaderMain.autoDownloader(View.getPluginInterface());
                                
                                View.AUTO_ONCE = true;
                                if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("plugin_auto_restart",false) || View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("plugin_auto_insert",false))
                                    View.PLUGIN_AUTO_GO = true;
                                
                                if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false) &&
                                        !version.startsWith("Checking")){
                                    TorrentUtils.setForceSeed(version);
                                }
                           
                        }
                    });//end of async run
                }
            }
        );
        
        //Download button/menu stuff
        final Menu menu = new Menu (View.composite_for_tab1.getShell(), SWT.POP_UP);
        MenuItem item1 = new MenuItem (menu, SWT.PUSH);
        item1.setText ("Manually Download Latest Version Via Torrent");
        //item1.setImage(ImageRepository.getImage("manual_download"));
        item1.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                try {
                    View.getPluginInterface().getPluginconfig().setPluginParameter("Azureus_downloadTracer",true);
                    MainCVSGetManual mainCVSGet = new MainCVSGetManual();
                    mainCVSGet.setURL(URLReader.get_cvsurl());
                    mainCVSGet.setDir(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator"));
                    mainCVSGet.initialize("torrent");
                    mainCVSGet.start();
                    } 
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }); 
        
        MenuItem item2 = new MenuItem (menu, SWT.PUSH);
        item2.setText ("Manually Download Latest Version Via HTTP (Only Use if Torrent Download Fails)");
        //item2.setImage(ImageRepository.getImage("manual_download"));
        item2.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                
                if (version.equals("Checking...."))
                    return;
                AltCVSGet altget = new AltCVSGet();
                altget.setURL(URLReader.get_jarurl());
                altget.setDir(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator"));
                altget.setFileName(version);
                altget.initialize();
                altget.start();
                
            }
        }); 
     
        
        
        download_tb = new ToolItem (toolBar, SWT.DROP_DOWN);
        download_tb.setToolTipText("Manually Download Latest Version");
        download_tb.setImage(ImageRepository.getImage("manual_download"));
        download_tb.addListener (SWT.Selection, new Listener () {
            public void handleEvent (Event event) {
                if(event.detail == SWT.ARROW){
                    Rectangle rect = download_tb.getBounds ();
                    Point pt = new Point (rect.x, rect.y + rect.height);
                    pt = toolBar.toDisplay (pt);
                    menu.setLocation (pt.x, pt.y);
                    menu.setVisible (true);
                }else{
                    try {
                        View.getPluginInterface().getPluginconfig().setPluginParameter("Azureus_downloadTracer",true);
                        MainCVSGetManual mainCVSGet = new MainCVSGetManual();
                        mainCVSGet.setURL(URLReader.get_cvsurl());
                        mainCVSGet.setDir(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator"));
                        mainCVSGet.initialize("torrent");
                        mainCVSGet.start();
                        } 
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }                    
            }
        });
        
        
        
                
        toolBar.pack ();
        return cvsgroup;
    }
}
