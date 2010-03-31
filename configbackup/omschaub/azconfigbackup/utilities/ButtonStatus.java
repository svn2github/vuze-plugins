/*
 * Created on Feb 25, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.utilities;

import omschaub.azconfigbackup.main.Tab4;
import omschaub.azconfigbackup.main.View;

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
                if(Tab4.backup == null || Tab4.backup.isDisposed())
                    return;
                Tab4.backup.setEnabled(backup_button);
            }
        });
    }   
}
