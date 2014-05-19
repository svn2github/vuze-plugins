/*
 * Created on May 20, 2005
 * Created by omschaub
 * 
 */
package omschaub.firefrog.main;


import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;


import org.gudy.azureus2.plugins.update.UpdateException;


public class TrayUtil {

    public static Tray tray;
    public static TrayItem TRAY_ITEM;
    public static MenuItem download_manager, exit;
    
    
    public static void open()
    {
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().asyncExec(new Runnable ()
                    {
                        public void run () 
                        {   
                                                        
                            tray = Utilities.getDisplay().getSystemTray();
                            if (tray == null) {
                                //System.out.println ("The system tray is not available");
                            } else {
                                 TrayItem TRAY_ITEM = new TrayItem(tray, SWT.NONE);
                                 TRAY_ITEM.setImage(ImageRepository.getImage("icon"));
                                 TRAY_ITEM.setVisible(true);
                                 TRAY_ITEM.setToolTipText("FireFrog - Right Click for menu");
                          
                                 
                                 
                                 final Menu popupmenu = new Menu(View.composite.getShell(), SWT.POP_UP);
                                 
                                 
                                 
                                 download_manager = new MenuItem(popupmenu, SWT.PUSH);
                                 download_manager.setText("Open FireFrog");
                                 download_manager.setImage(ImageRepository.getImage("icon"));
                                 download_manager.addListener(SWT.Selection, new Listener () {
                                    public void handleEvent (Event e){
                                        DownloadManagerShell.open();
                                    }
                                 });
                                 
                                
                                 
                                 //Pause/Resume All Cascade
                                 MenuItem transfers = new MenuItem(popupmenu, SWT.CASCADE);
                                 transfers.setText("Transfers");
                                 Menu transfers_menu = new Menu(View.composite.getShell(),SWT.DROP_DOWN);
                                 transfers.setMenu(transfers_menu);
                                 
                                 MenuItem pauseAll = new MenuItem(transfers_menu,SWT.PUSH);
                                 pauseAll.setText("Stop All Transfers");
                                 pauseAll.addListener(SWT.Selection, new Listener () {
                                     public void handleEvent (Event e){
                                         Plugin.getPluginInterface().getDownloadManager().stopAllDownloads();
                                     }
                                 });
                                 
                                 
                                 
                                 MenuItem startAll = new MenuItem(transfers_menu,SWT.PUSH);
                                 startAll.setText("Start All Transfers");
                                 startAll.addListener(SWT.Selection, new Listener () {
                                     public void handleEvent (Event e){
                                         Plugin.getPluginInterface().getDownloadManager().startAllDownloads();
                                     }
                                 });
                                 
                                 exit = new MenuItem(popupmenu, SWT.PUSH);
                                 exit.setText("Exit FireFrog and Azureus");
                                 exit.addListener(SWT.Selection, new Listener () {
                                     public void handleEvent (Event e){
                                         
                                         try {
                                             Shell shell = new Shell(Utilities.getDisplay());
                                             MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                                             messageBox.setText("Exit Confirmation");
                                             messageBox.setMessage("Are you sure you want to exit FireFrog and Azureus?");
                                             int response = messageBox.open();
                                             switch (response){
                                             case SWT.YES:
                                                 Plugin.getPluginInterface().getUpdateManager().applyUpdates(false);
                                                 shell.dispose();
                                                break;
                                             case SWT.NO:
                                                 break;
                                             }
                                             
                                        } catch (UpdateException e1) {
                                            e1.printStackTrace();
                                        }
                                     }
                                  });
                                 
                                 
                                 TRAY_ITEM.addListener (SWT.Selection, new Listener () {
                                     public void handleEvent (Event event) {
                                         //System.out.println("selection");
                                         //popupmenu.setVisible (true);
                                     }
                                 });
                                 
                                 TRAY_ITEM.addListener (SWT.MenuDetect, new Listener () {
                                    public void handleEvent (Event event) {
                                        popupmenu.setVisible (true);
                                    }
                                });
                                 
                                 TRAY_ITEM.addListener(SWT.DefaultSelection, new Listener() 
                                 {
                                     public void handleEvent(Event e) 
                                     {
                                         if(DownloadManagerShell.DOWNLOAD_MANAGER_SHELL == null || DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.isDisposed()){
                                             DownloadManagerShell.open();
                                             return;
                                         }
                                         
                                         if(!DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.getMinimized()){
                                             DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.setMinimized(true);
                                             DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.setVisible(false);
                                         }
                                         else{
                                             DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.setMinimized(false);
                                             DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.setVisible(true);
                                         }
                                     }
                                 }); 
                            }
                     
                        }
                    });
        
        }
    }
    
    
    public static void close(){
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().asyncExec(new Runnable ()
                    {
                        public void run () 
                        {
                            if(tray != null && !tray.isDisposed()){
                                TrayItem[] items = tray.getItems();
                                for (int i = 0 ; i < items.length ; i++){
                                    try{
                                        if(items[i].getToolTipText().startsWith("FireFrog"))
                                            items[i].dispose();  
                                        
                                        
                                    }catch(Exception e){
                                        
                                    }
                                }
                                
                                
                            }
                            
                            
                        }
                    });
        }
        
    }
        
        public static boolean isOpen(){

            if(tray != null && !tray.isDisposed()){
                TrayItem[] items = tray.getItems();
                for (int i = 0 ; i < items.length ; i++){
                    try{
                        if(items[i].getToolTipText().startsWith("FireFrog"))
                            return true;
                        
                        
                    }catch(Exception e){
                        
                    }
                }
                
               
            }
       
            return false;
    }
}//EOF
