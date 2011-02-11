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
        
        
        Label devList = new Label(composite,SWT.NULL);
        devList.setText("Vuze Dev site");
        devList.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        devList.setCursor(View.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        devList.setToolTipText("Open the Vuze Dev site in your default browser");
        devList.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event e) {
            	try {View.getPluginInterface().getUIManager().openURL(new URL(Constants.AZUREUS_CVS_URL));}
            	catch (Exception uie) {}
                }
            });
        
        
        Label mailList = new Label(composite,SWT.NULL);
        mailList.setText("Commitlog at Sourceforge");
        mailList.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        mailList.setCursor(View.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        mailList.setToolTipText("Open the Vuze commitlog in your default browser");
        mailList.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event e) {
            	try {View.getPluginInterface().getUIManager().openURL(new URL(Constants.AZUREUS_COMMITLOG_URL));}
            	catch (Exception uie) {}
                }
            });
        
        
        Label JIRAsite = new Label(composite,SWT.NULL);
        JIRAsite.setText("Vuze JIRA");
        JIRAsite.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        JIRAsite.setCursor(View.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        JIRAsite.setToolTipText("Open the Vuze JIRA bug tracker in your default browser");
        JIRAsite.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event e) {
            	try {View.getPluginInterface().getUIManager().openURL(new URL(Constants.VUZE_JIRA_URL));}
            	catch (Exception uie) {}
                }
            });
        
        
        return composite;
    }
    
}
