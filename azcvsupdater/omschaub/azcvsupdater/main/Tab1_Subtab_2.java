/*
 * Created on Oct 17, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import java.net.URL;

import omschaub.azcvsupdater.main.View;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;


public class Tab1_Subtab_2 {
    
    
    /**
     * new sub tab for links
     * @param composite
     */
    Composite open(Composite parent){
        
        Composite composite = new Composite(parent,SWT.NULL);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        composite.setLayout(gridLayout);
        
        GridData gridData = new GridData(GridData.CENTER);
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
        composite.setLayoutData(gridData);
        
        
        Label changelog = new Label(composite,SWT.NULL);
        changelog.setText("Azureus Changelog");
        changelog.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        changelog.setCursor(View.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        changelog.setToolTipText("Open the current Azureus changelog in your default browser");
        changelog.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event e) {
            	try {View.getPluginInterface().getUIManager().openURL(new URL(Constants.AZUREUS_CHANGELOG_URL));}
            	catch (Exception uie) {}
                }
            });
        
        
        Label mailList = new Label(composite,SWT.NULL);
        mailList.setText("Azureus Commitlog");
        mailList.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        mailList.setCursor(View.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        mailList.setToolTipText("Open the Azureus commit log in your default browser");
        mailList.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event e) {
            	try {View.getPluginInterface().getUIManager().openURL(new URL(Constants.AZUREUS_COMMITLOG_URL));}
            	catch (Exception uie) {}
                }
            });
        
        
        return composite;
    }
    
}
