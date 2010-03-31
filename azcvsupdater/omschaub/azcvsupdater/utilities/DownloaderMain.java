/*
 * Created on Feb 24, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import java.io.File;

import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.Tab1_Subtab_1;
import omschaub.azcvsupdater.main.View;
import omschaub.azcvsupdater.utilities.download.MainCVSGet;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;

public class DownloaderMain {
    static ResourceDownloaderFactory rdf;
    static ResourceDownloaderListener rdl;
    static ResourceDownloader rd_t;
       
    public static void autoDownloader(final PluginInterface pm){

        Thread autoDownloaderThread = new Thread() {
            public void run() {
                try {

                    Thread.sleep(5000);
                    if(URLReader.get_whenLastModified() == 0){
                        Thread.sleep(5000);
                    }
                    
                    if (URLReader.get_whenLastModified() == 0){
                        return;
                    }
                                    
                    //oldestFile = 1;
                    File f = new File(DirectoryUtils.getBackupDirectory());
                    if(!f.exists() || !f.isDirectory())
                        return;

                    File[] files = f.listFiles();

                    for(int i = 0 ; i < files.length ; i++) 
                    {
                        
                        //String fileName = files[i].getName();
                        if(files[i].getName().equalsIgnoreCase(Tab1_Subtab_1.version))
                        {
                            //output message that download is not needed
                            StatusBoxUtils.mainStatusAdd(" Auto Download not needed",1);
                            // auto seed latest code
                            //System.out.println("Auto_seed: " + View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false) + " Sharing Latest: " + TorrentUtils.sharingLatest());
                            if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false) && !TorrentUtils.sharingLatest()){
                                TorrentUtils.seedLatest();
                               // TorrentUtils.setForceSeed(Tab1.version);
                            }

                            
                            return;
                        }
                        
                    }
                    
                    
                    
                        
                    
                    //resource_getter(URLReader.get_cvsurl(),pm);
                    
                    StatusBoxUtils.mainStatusAdd(" Auto Downloading " + Tab1_Subtab_1.version + " via torrent",1);
                    
                    call_MainCVSGet();
                    
                

                        
                } catch(Exception e) {
                    //Stop process and trace the exception
                    //e.printStackTrace();
                    System.out.println("AZCVSUpdater Error:  Error in autodupdate code");
                }
            }
        };  
        
        if (pm.getPluginconfig().getPluginBooleanParameter("AutoDownload"))
        {
            autoDownloaderThread.setDaemon(true);
            autoDownloaderThread.start();
        }else if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false) && !TorrentUtils.sharingLatest()){
                TorrentUtils.seedLatest();
                //TorrentUtils.setForceSeed(Tab1.version);
        }
    }


    /**
     * setup for main resource get
     */
    private static void call_MainCVSGet(){
        if (Tab1_Subtab_1.version.equals("Checking...."))
            return;
        MainCVSGet mainCVSGet = new MainCVSGet();
        mainCVSGet.setURL(URLReader.get_cvsurl());
        mainCVSGet.setDir(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator"));
        mainCVSGet.initialize("torrent");
        mainCVSGet.start();
    }

}//EOF
