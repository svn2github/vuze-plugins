/*
 * Created on Feb 5, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import java.io.File;
import java.text.Collator;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.StackX;

public class Tab6Utils {
	
	
    
    /**
     * Sets up the combo for Tab6 for the cvsfiles
     *
     */
    
    static void getCVSFiles()
    {
//      Getting Config Directory Listing
        if(Tab6.comboJar == null && Tab6.comboJar.isDisposed())
            return;
            
        Tab6.comboJar.removeAll();
        Tab6.comboJar.add("Please Choose a Azureus2-BXX file to Insert as Azureus2.jar");
        
        final String backupDirectory = DirectoryUtils.getBackupDirectory();
        try {
        	File f = new File(backupDirectory);
        	File[] files = f.listFiles();
            String[][] files_string = new String[files.length][2];
            for(int i = 0 ; i < files.length ; i++){
                files_string [i][0] = files[i].getName();
                files_string [i][1] = String.valueOf(files[i].lastModified());
            }
            //System.out.println("length: " + files_string.length);
            sortCVSArray(files_string);
            
            
            StackX sortedArray = new StackX(files_string.length);
            for(int i = 0 ; i < files_string.length ; i++){
                sortedArray.push(files_string[i][0]);
            }
            

        	
            while(!sortedArray.isEmpty()){
                String name = sortedArray.pop();
                if(name.startsWith("Azureus") )
                {
                
                if(Tab6.comboJar != null && !Tab6.comboJar.isDisposed())
                    Tab6.comboJar.add(name);
                }  
            }
       	
        	Tab6.comboJar.select(0);
        } catch(Exception e) {
        	//Stop process and trace the exception
        	e.printStackTrace();
        }

    }
    
    private static void sortCVSArray(String[][] strArray){
        Collator collator = Collator.getInstance(Locale.getDefault());
        
        String tmp0, tmp1;
        if (strArray.length == 1) return;
        for (int i = 0; i < strArray.length; i++) {
          for (int j = i + 1; j < strArray.length; j++) {
            if( collator.compare(strArray[i][1], strArray[j][1] ) > 0 ) {
              tmp0 = strArray[i][0];
              tmp1 = strArray[i][1];
              strArray[i][0] = strArray[j][0];
              strArray[i][1] = strArray[j][1];
              strArray[j][0] = tmp0;
              strArray[j][1] = tmp1;
              }
            }
          }
    }
    
    public static void refreshLists(){
        View.asyncExec(new Runnable (){
        	public void run () 
    		{
    		    getCVSFiles();  
    		}
        });
    }
    
    static void clearLists(){
       	Event e = new Event();
    	e.type = SWT.None;
       	Tab6.comboJar.notifyListeners(SWT.None, e);
		
		Tab6.submit_button.setVisible(true);
		Tab6.final_restart_button.setVisible(false);
		Tab6.final_stop_button.setVisible(false);
		Tab6.final_nostop_button.setVisible(false);
		Tab6.alert.setVisible(false);
		
    }

    static void insertSelect(String selectedJar){
        View.tab.setSelection(5);
        Tab6Utils.clearLists();
        
        Tab6.comboJar.setEnabled(true);
        String combo_items[] = Tab6.comboJar.getItems();
        for (int i = 0; i < combo_items.length ; i++){
            if(combo_items[i].startsWith(selectedJar)){
                Tab6.comboJar.select(i);
            }
        }
        
        Event evt = new Event();
        evt.type = SWT.Selection;
        evt.widget = Tab6.comboJar;
        Tab6.comboJar.notifyListeners(SWT.Selection, evt);

    }
    
    
    
    
//EOF
}
