/*
 * Created on Apr 28, 2005
 * Created by omschaub
 * 
 */
package omschaub.firefrog.main;



import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;

public class TorrentListener {
    
        
    public static void isSeeding(){

            final DownloadManager dm;
            dm = Plugin.getPluginInterface().getDownloadManager();
            DownloadManagerListener dml = new DownloadManagerListener()
                {
                    public void
                    downloadAdded(
                    final Download  download )
                    {
                        //DownloadManagerUtils.cleanAll();
                        download.addListener(new DownloadListener() {

                            public void stateChanged(Download download, int old_state, int new_state) {
                                //System.out.println(download.getName() + " : From: " + old_state + " : To: " + new_state);
                                if(old_state == Download.ST_DOWNLOADING
                                        && new_state == Download.ST_SEEDING 
                                        && download.isComplete()){
                                    
                                    //Per Paul this should work to distinguish between the two states
                                    //Finished download and unpaused seeding
                                    
                                    if(download.getStats().getSecondsOnlySeeding() == 0){
                                        //System.out.println("Download to seed!");
                                        DownloadManagerUtils.cleanOne(Utilities.verifyName(download.getName()),1);
                                    }else{
                                        
                                        DownloadManagerUtils.cleanOne(Utilities.verifyName(download.getName()),1);
                                        //DownloadManagerUtils.cleanOne(download.getName(),2);
                                    }
                                        
                                                                        
                                   
                                    
                                }else if(old_state == Download.ST_DOWNLOADING && !download.isComplete()){
                                    //TODO check the states of a download finishing and see if we can distinguish between a 'stop'
                                    //for pausing and a stop for going to seeding
                                    //DownloadManagerUtils.cleanAll();
                                }else if(old_state == Download.ST_PREPARING){
                                    DownloadManagerUtils.cleanOne(Utilities.verifyName(download.getName()),1);
                                }else{
                                    //might not need the catchall here
                                    DownloadManagerUtils.cleanOne(Utilities.verifyName(download.getName()),2);
                                }
                                
                                
                            }

                            public void positionChanged(Download download, int oldPosition, int newPosition) {
                                //DownloadManagerUtils.cleanAll();
                            }
                            
                        });               
                        
                        
                        
                       
                    }
                    public void
                    downloadRemoved(
                        Download    download )
                    {
                        //DownloadManagerUtils.cleanAll();
                       
                        //First check to see if it is in the seeds section
                        DownloadManagerUtils.cleanOne(Utilities.verifyName(download.getName()),2);
                        
                        //Then run through hidden removal
                        String[] hidden = HideFiles.read();
                        for(int i = 0 ; i < hidden.length ; i++){
                            if(hidden[i] != null && hidden[i].equalsIgnoreCase(Utilities.verifyName(download.getName()))){
                                HideFiles.removeFile(Utilities.verifyName(download.getName()));
                            }
                        }
                    }
                };
            

                dm.addListener(dml);
        }
            
        

}
