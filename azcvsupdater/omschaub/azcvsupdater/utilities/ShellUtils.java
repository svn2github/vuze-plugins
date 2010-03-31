/*
 * Created on Oct 29, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;


import omschaub.azcvsupdater.main.View;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public class ShellUtils {
    public static void centerShellandOpen(Shell shell){
        //open shell
        shell.pack();
        
        //Center Shell
        Monitor primary = View.getDisplay().getPrimaryMonitor ();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = shell.getBounds ();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y +(bounds.height - rect.height) / 2;
        shell.setLocation (x, y);
        
        //open shell
        shell.open();
    }
}
