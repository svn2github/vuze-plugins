/*
 * Created on May 8, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;

import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.Tab1_Subtab_1;
import omschaub.azcvsupdater.main.View;
import omschaub.azcvsupdater.utilities.download.SeedCVS;

public class TorrentUtils {

   
    
    static boolean sharingLatest(){
        DownloadManager dm = View.getPluginInterface().getDownloadManager();
        Download[] downloads = dm.getDownloads();
        for(int i = 0 ; i < downloads.length ; i++){
            //setForceSeed(Tab1.version);
            if(downloads[i].getName().equalsIgnoreCase(Tab1_Subtab_1.version)){
                return (true);
            }
        }
        return(false);
    }
    
    
    static void seedLatest(){
        try{
            if (Tab1_Subtab_1.version.equals("Checking...."))
                return;
            SeedCVS seedCVS = new SeedCVS();
            seedCVS.setURL(URLReader.get_cvsurl());
            seedCVS.setDir(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator"));
            seedCVS.initialize("torrent");
            seedCVS.start();
        }catch(Exception e){
            StatusBoxUtils.mainStatusAdd(" Error downloading torrent for auto seed, please use 'Manual Update' to try again later", 2);
            e.printStackTrace();
        }
    }
    
    public static void setForceSeed(final String name){
        DownloadManager dm;
        dm = View.getPluginInterface().getDownloadManager();
        Download download[] = dm.getDownloads();
        for(int i = 0; i < download.length ; i++){
            if(download[i].getName().equalsIgnoreCase(name)){
                try{
                    if(download[i].getState() == Download.ST_STOPPED){
                        download[i].setForceStart(true);
                        download[i].start();
                    }else{
                        
                        /*download[i].stop();
                        while(download[i].getState() != Download.ST_STOPPED){
                            Thread.sleep(1000);
                        }*/
                        
                        download[i].setForceStart(true);
                        //download[i].start();
                    }
                }catch(DownloadException e){
                    e.printStackTrace();
                } /*catch (InterruptedException e) {
                   
                    e.printStackTrace();
                }*/
                
                
                
            }else if(download[i].getName().startsWith("Azureus")){
                removeLastSeed(download[i].getName());
            }
        }
    }
    
    
    
    static void removeLastSeed(final String name){
        DownloadManager dm;
        dm = View.getPluginInterface().getDownloadManager();
        Download download[] = dm.getDownloads();
        for(int i = 0; i < download.length ; i++){
            if(download[i].getName().equalsIgnoreCase(name)){
                try{
                    if(download[i].isForceStart()){
                        download[i].setForceStart(false);
                        download[i].restart();
                    
                    }
                }catch(Exception e){
                    e.printStackTrace();
                } 
            }
        }
    }
    
}//EOF
