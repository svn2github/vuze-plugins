/*
 * Created on Feb 24, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import omschaub.azcvsupdater.main.Tab1_Subtab_1;
import omschaub.azcvsupdater.main.View;
import omschaub.azcvsupdater.utilities.imagerepository.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;


public class TrayAlert {

    static TrayItem azcvs_trayitem;
    
    
    public static void open()
    {
        if(View.getDisplay() != null && !View.getDisplay().isDisposed()){
            View.getDisplay().asyncExec(new Runnable ()
                    {
                        public void run () 
                        {   
                            Tray tray = View.getDisplay().getSystemTray();
                            
                            TrayItem[] trayItems = tray.getItems();
                            for(int i = 0; i < trayItems.length; i++){
                                if(trayItems[i].getToolTipText().equalsIgnoreCase(Tab1_Subtab_1.version + " available (double click to remove alert)")){
                                    System.out.println("AZCVSUpdater trying to add a second tray item for the same thing! -- cancelling action");
                                    return;
                                }
                            }
                            
                            final TrayItem ti = new TrayItem(tray, SWT.NONE);
       
                            ti.setImage(ImageRepository.getImage("new_cvs"));
                            ti.setVisible(true);
                            ti.setToolTipText(Tab1_Subtab_1.version + " available (double click to remove alert)");
                            ti.addListener(SWT.DefaultSelection, new Listener() 
                            {
                                public void handleEvent(Event e) 
                                {
                                    ti.dispose();
                                }
                            });
                        }
                    });
        
        }
    }
        
}
