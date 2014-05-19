/*
 * Created on Apr 30, 2005
 * Created by omschaub
 * 
 */
package omschaub.firefrog.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;


public class DownloadManagerShell {
    public static ScrolledComposite sc;
    public static Shell DOWNLOAD_MANAGER_SHELL;
    public static Group DOWNLOADS;
    public static Group CURRENT_RESPONSES;
    public static Composite HIDDEN, dm_composite, miniHiddenComp;
    public static Label COMPLETED_LABEL;
    public static boolean COLLAPSED;
    public static String TITLE;
    
    public static void open(){
        if(DOWNLOAD_MANAGER_SHELL != null && !DOWNLOAD_MANAGER_SHELL.isDisposed()){
            DOWNLOAD_MANAGER_SHELL.setVisible(true);
            DOWNLOAD_MANAGER_SHELL.forceFocus();
            DOWNLOAD_MANAGER_SHELL.forceActive();
            return;
        }
        
        if(TITLE == null) TITLE = "FireFrog";
        
        DOWNLOAD_MANAGER_SHELL = new Shell(Utilities.getDisplay());
        
        DOWNLOAD_MANAGER_SHELL.setImage(ImageRepository.getImage("icon"));
        
        //Grid Layout 
        DOWNLOAD_MANAGER_SHELL.setLayout(new GridLayout(1,false));
        DOWNLOAD_MANAGER_SHELL.setMinimumSize(400,250);
        

        //Scrolled Composite to incorporate all fo the children
        sc = new ScrolledComposite(DOWNLOAD_MANAGER_SHELL, SWT.V_SCROLL);
        sc.setExpandVertical(true);
        sc.setExpandHorizontal(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 300;
        sc.setLayoutData(gridData);
        
        //composite for shell
        dm_composite = new Composite(sc,SWT.BORDER);
        
        //Grid Layout   
        dm_composite.setLayout(new GridLayout(1,false));
        gridData = new GridData(GridData.FILL_BOTH);
        dm_composite.setLayoutData(gridData);
        dm_composite.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        
        //shell title
        DOWNLOAD_MANAGER_SHELL.setText(TITLE + " -- Down 0 Kb/s | Up 0 Kb/s");
        
        
        //--------------------- Set Up Downloads 'group'---------------------
        DOWNLOADS = new Group(dm_composite,SWT.NULL);
        DOWNLOADS.setText("Downloads");
        DOWNLOADS.setLayout(new GridLayout(3,false));
        DOWNLOADS.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.widthHint = 500;
        DOWNLOADS.setLayout(new GridLayout(3,false));
        
        DOWNLOADS.setLayoutData(gridData);
        
        
        //----------------------Set Up Current Responses Group--------------
        CURRENT_RESPONSES = new Group(dm_composite, SWT.NULL);
        CURRENT_RESPONSES.setText("Uploads");
        CURRENT_RESPONSES.setLayout(new GridLayout(2,false));
        CURRENT_RESPONSES.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        
        CURRENT_RESPONSES.setLayoutData(gridData);
        CURRENT_RESPONSES.setLayout(new GridLayout(2,false));
        
//    -----------------------minihiddenComposite for label for completed Files--------------
        
        miniHiddenComp = new Composite(dm_composite, SWT.NULL);
        miniHiddenComp.setLayout(new GridLayout(3,false));
        miniHiddenComp.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        miniHiddenComp.setLayoutData(gridData);
        
        //Carrot Button
        COLLAPSED = true;
        final Label list_button = new Label(miniHiddenComp,SWT.NULL);
        list_button.setImage(ImageRepository.getImage("right_arrow"));
        list_button.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        list_button.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        list_button.addListener(SWT.MouseDown, new Listener(){
            public void handleEvent(Event e) {
                if(list_button.getImage().equals(ImageRepository.getImage("right_arrow"))){
                    list_button.setImage(ImageRepository.getImage("down_arrow"));
                    HIDDEN.layout();
                    dm_composite.layout();
                    COLLAPSED = false;
                    DownloadManagerUtils.cleanHidden();
                }else{
                    list_button.setImage(ImageRepository.getImage("right_arrow"));
                    HIDDEN.layout();
                    dm_composite.layout();
                    COLLAPSED = true;
                    DownloadManagerUtils.cleanHidden();
                }
              
            }
        });
        
        
        
        //Completed Label
        COMPLETED_LABEL = new Label(miniHiddenComp, SWT.NULL);
        COMPLETED_LABEL.setText("Also uploading " + HideFiles.getCount() + " hidden files");
        COMPLETED_LABEL.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        COMPLETED_LABEL.setLayoutData(gridData);
        
        
        //-----------------------Composite for completed Files--------------
        
        HIDDEN = new Composite(dm_composite, SWT.NULL);
        HIDDEN.setLayout(new GridLayout(2,false));
        HIDDEN.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        HIDDEN.setLayoutData(gridData);
        
        
   
        //Set minimum size of child composite (completed) to the scrolled composite
        sc.setContent(dm_composite);
        sc.addControlListener(new ControlAdapter() {
            public void controlResized(ControlEvent e) {
                DOWNLOADS.layout();
                dm_composite.layout();
                Rectangle r = sc.getClientArea();
                
                sc.setMinSize(dm_composite.computeSize(r.width,SWT.DEFAULT));
            }
        });
        
        
        //Bottom Button Composite
        
        Composite bottom = new Composite(DOWNLOAD_MANAGER_SHELL,SWT.NULL);
        bottom.setLayout(new GridLayout(5,false));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        bottom.setLayoutData(gridData);
        
        
        Button upload = new Button(bottom, SWT.PUSH);
        upload.setText("Not Used");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        upload.setLayoutData(gridData);
        upload.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event e) {
                
            }
        });
        upload.setVisible(false);
        
        
        
        Button pause_all = new Button(bottom, SWT.PUSH);
        pause_all.setText("Pause All");
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END);
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalSpan = 3;
        pause_all.setLayoutData(gridData);
        pause_all.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event e) {
                Plugin.getPluginInterface().getDownloadManager().pauseDownloads();
            }
        });
        
        
        Button resume_all = new Button(bottom, SWT.PUSH);
        resume_all.setText("Resume All");        
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        resume_all.setLayoutData(gridData);
        resume_all.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event e) {
                Plugin.getPluginInterface().getDownloadManager().startAllDownloads();
            }
        });
        
        
        
        
        
        
        Composite bottom1 = new Composite(DOWNLOAD_MANAGER_SHELL,SWT.NULL);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginHeight = 0;
        bottom1.setLayout(gridLayout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        bottom1.setLayoutData(gridData);
        
        Label default_directory = new Label(bottom1,SWT.NULL);
        gridData = new GridData(GridData.BEGINNING);
        gridData.horizontalSpan=1;
        default_directory.setLayoutData(gridData);
        default_directory.setText("All Downloads Saved To:  " + Plugin.getPluginInterface().getPluginconfig().getUnsafeStringParameter("Default save path") );
        //default_directory.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
        default_directory.setToolTipText("Click to open directory in default file browser");
        default_directory.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        default_directory.addListener(SWT.MouseDown, new Listener(){
            public void handleEvent(Event e) {
                String path = Plugin.getPluginInterface().getPluginconfig().getUnsafeStringParameter("Default save path");
                System.out.println(path);
                if(Plugin.getPluginInterface().getUtilities().isWindows()){
                    Program.launch(path);    
                }else if(Plugin.getPluginInterface().getUtilities().isLinux()){
                    //hopefully a linux person will have one of these or else clicking will do nothing 
                    
                    //if they use KDE they will have konqueror
                    if(!Program.launch("konqueror " + path)){
                        //if they use GNOME they will have nautilus
                        if(!Program.launch("nautilus " + path)){
                            //else, just display the directory in their browser
                            if(!Program.launch("firefox " + path)){
                                Program.launch("mozilla " + path);
                            }
                        }
                    }
                    
                    
                }else{
                    //MAC OSX stuff here.. dont have a clue what works, I will just try to do it the way that
                    //Windows does it and see if we get any complaints
                    Program.launch(path);
                }
                
                
                        
            }
        });
        
        
      
        
        //      -----------------------------Center/Open Shell-------------------
        //In case we need to do anything on disposal, set up a listener
        DOWNLOAD_MANAGER_SHELL.addListener(SWT.Dispose, new Listener(){
            public void handleEvent(Event e) {
                Timer.destroyTimer();
                DOWNLOAD_MANAGER_SHELL = null;
            }
        });
        
        
        //open shell and center it on the users monitor
        Utilities.centerShellandOpen(DOWNLOAD_MANAGER_SHELL);
        
        
        Timer.runMainTimer();
        
    }
    
    public static void setTitle(final long down_speed, final long up_speed){
       
        
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        {    
                            if(DownloadManagerShell.DOWNLOAD_MANAGER_SHELL != null && 
                                    !DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.isDisposed() &&
                                    DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.getVisible())
                                DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.setText(TITLE + " -- Down " + down_speed/1024 + " Kb/s | Up " + up_speed/1024 + " Kb/s" );
                        }
                    });
        
    }

    public static void setHiddenNumber(){
        
        
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        {    
                            if(DownloadManagerShell.COMPLETED_LABEL != null && 
                                    !DownloadManagerShell.COMPLETED_LABEL.isDisposed() &&
                                    DownloadManagerShell.COMPLETED_LABEL.getVisible())
                                COMPLETED_LABEL.setText("Also uploading " + HideFiles.getCount() + " hidden files");
                        }
                    });
        
    }
    
}//EOF
