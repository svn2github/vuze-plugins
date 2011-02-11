/*
 * Created on Apr 7, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities.download;



import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.Tab1_Subtab_1;
import omschaub.azcvsupdater.utilities.TorrentUtils;




public class SeedCVS extends DownloadImp{
    public void preCommands(){
        
    }
    
    public void postCommands(){
                StatusBoxUtils.mainStatusAdd(" Sucessfully auto seeded " + Tab1_Subtab_1.version,0);
                TorrentUtils.setForceSeed(Tab1_Subtab_1.version);
    
    }
    
    public void percentageCommands(final int percentage){
 
    }
    
    public void activityCommands(String activity){
       
    }
    public void failedCommands(){
        
        StatusBoxUtils.mainStatusAdd(" Failed to Auto Seed latest torrent",2);
   
    }
    
  
    


}//EOF
