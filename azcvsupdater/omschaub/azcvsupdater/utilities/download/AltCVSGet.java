/*
 * Created on Apr 7, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities.download;

import java.io.File;

import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.Tab1;
import omschaub.azcvsupdater.main.Tab1Utils;
import omschaub.azcvsupdater.main.Tab6Utils;
import omschaub.azcvsupdater.main.View;
import omschaub.azcvsupdater.utilities.ButtonStatus;
import omschaub.azcvsupdater.main.Constants;



public class AltCVSGet extends DownloadImp{
    
    public void preCommands(){
     
        View.getDisplay().syncExec(new Runnable (){
            public void run () {                    
                Tab1.downloadVisible(true,true);
                Tab1.setCustomPB(new Integer(0));
                
                //redraw
                if (Tab1.listTable != null && !Tab1.listTable.isDisposed())
                    if(Tab1.listTable.getEnabled()){
                        Tab1.listTable.setEnabled(false);
                    }
                ButtonStatus.set(false, false, false, false, false);
            }
        });
    }
    
    public void postCommands(){
        //get rid of Progress Bar
        Tab1.downloadVisible(false, true);
        StatusBoxUtils.mainStatusAdd(" Sucessfully Downloaded " + totalk/1024 + " KB",0);
        Tab6Utils.refreshLists();
        
        //put the file reading in a try/catch to be able to see any problems that might arise
        try{
            File temp_file = new File (dir + fileName);
            StatusBoxUtils.mainStatusAdd(" Size reported on web: "+ totalk + " bytes. Size reported locally: " + temp_file.length() + " bytes",1);    
        }catch (Exception e){
            e.printStackTrace();
        }       
        
        StatusBoxUtils.mainStatusAdd(" Since this alternate method to download the Dev Beta is unstable (be sure to check file size), no auto insert/restart allowed",2);
        ButtonStatus.set(true, true, false, true, true);
        Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
        
        /*View.getDisplay().asyncExec(new Runnable (){
            public void run () {
                
                if(View.status_composite != null && !View.status_composite.isDisposed()){
                    View.status_composite.layout(true);
                }
               
            }
        });*/
    }
    
    public void percentageCommands(final int percentage){
        Tab1.setCustomPB(new Integer(percentage*10));
    }
    
    public void activityCommands(String activity){
        //System.out.println(activity);
    }
    public void failedCommands(){
        Tab1.downloadVisible(false, true);
        StatusBoxUtils.mainStatusAdd(" Download by torrent AND http failed.. check " + Constants.AZUREUS_CVS_URL,2);
        ButtonStatus.set(true, true, false, true, true);
        Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
    }
}
