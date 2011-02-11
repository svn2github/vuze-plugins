/*
 * Created on Feb 25, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import omschaub.azcvsupdater.main.Tab1;
import omschaub.azcvsupdater.main.Tab1_Subtab_1;
import omschaub.azcvsupdater.main.View;

public class ButtonStatus {
//  Button Toggle for main 5 buttons

    /** ButtonStatus.set sets the five main buttons
     * 
     * @param boolean refresh button
     * @param boolean download button
     * @param boolean delete button
     * @param boolean checkCVS button
     * @param boolean backup_button
     * 
     */
    public static void set(final boolean refresh, final boolean download, final boolean delete, final boolean checkCVS, final boolean backup_button){
        if(View.getDisplay() == null && View.getDisplay().isDisposed())
            return;

        View.getDisplay().asyncExec( new Runnable() {
              public void run() {
                if(Tab1.refresh == null || Tab1.refresh.isDisposed())
                  return;
                Tab1.refresh.setEnabled(refresh);
              }
        });
        
        
        View.getDisplay().asyncExec( new Runnable() {
                public void run() {
                    if(Tab1_Subtab_1.download_tb == null || Tab1_Subtab_1.download_tb.isDisposed())
                    return;
                    Tab1_Subtab_1.download_tb.setEnabled(download);
                }
            });
            
        
        View.getDisplay().asyncExec( new Runnable() {
                public void run() {
                    if(Tab1.toolbar_delete == null || Tab1.toolbar_delete.isDisposed())
                        return;
                    Tab1.toolbar_delete.setEnabled(delete);
                }
            });
        
        
       
        View.getDisplay().asyncExec( new Runnable() {
                public void run() {
                    if(Tab1_Subtab_1.versionCheck == null || Tab1_Subtab_1.versionCheck.isDisposed())
                        return;
                    Tab1_Subtab_1.versionCheck.setEnabled(checkCVS);
                }
            });
    }   
}
