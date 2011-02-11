/*
 * Created on Apr 7, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities.download;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.View;

import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;




public class DownloadImp{
    private ResourceDownloaderFactory rdf;
    ResourceDownloaderListener rdl;
    ResourceDownloader rd_t;
    private URL url_get;
    String dir, fileName;
    private String download_type;
    long totalk;
    
    /**
     * Initialize the ResourceDownloader.. must be called before start()
     * @value type must be generic, meta, or torrent
     */
    public void initialize(String type) {
        try{
            download_type = type;
            rdf = View.getPluginInterface().getUtilities().getResourceDownloaderFactory();
            rd_t = rdf.create(url_get);
           /* 
            //as per Paul's suggestion.. I am reordering these
            if(type.equalsIgnoreCase("torrent")){
                //in this case, dir will be where to save the torrent data to
                rd_t = rdf.getTorrentDownloader(rd_t, true, new File(dir));
            }*/
            
            rd_t = rdf.getRetryDownloader(rd_t, 3);
            //rd_t = rdf.getTimeoutDownloader(rd_t,60000);
            
            if(type.equalsIgnoreCase("meta")){
                rd_t = rdf.getMetaRefreshDownloader(rd_t);
            } 
            
            
            
            if(type.equalsIgnoreCase("torrent")){
                //in this case, dir will be where to save the torrent data to
                rd_t = rdf.getTorrentDownloader(rd_t, true, new File(dir));
            }else{
                rd_t = rdf.getSuffixBasedDownloader(rd_t);
            }
            
            
            totalk = rd_t.getSize();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize the ResourceDownloader.. must be called before start()
     * This will initialize a 'generic' downloader.. ie NON meta
     */
    public void initialize() {
        try{
            download_type = "generic";
            rdf = View.getPluginInterface().getUtilities().getResourceDownloaderFactory();
            rd_t = rdf.create(url_get);
            rd_t = rdf.getRetryDownloader(rd_t, 3);
            //rd_t = rdf.getTimeoutDownloader(rd_t,60000);
            rd_t = rdf.getSuffixBasedDownloader(rd_t);
            totalk = rd_t.getSize();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
    
    public void setURL(String url) {
        try {
            url_get = new URL (url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        
    }
    
    
    public void preCommands() {
        
        
    }

    public void setDir(String dir_to_save_to){
        dir = dir_to_save_to;
    }
    
    public void setFileName(String filename){
        fileName = filename;
    }
    
    
    public void start() {
       rdl = new ResourceDownloaderListener()
                {
                    public boolean
                    completed(
                        final ResourceDownloader downloader,
                        InputStream data )
                    {
                    
                        try{
                            if(!download_type.equalsIgnoreCase("torrent")){
                                FileOutputStream file = new FileOutputStream(dir + fileName);
                                BufferedOutputStream out = new BufferedOutputStream(file);

                                byte[] buf = new byte[65536];
                                int buf_size = 0;
                                while (true) {
                                    buf_size = data.read(buf);
                                    if (buf_size == -1) {break;}
                                    out.write(buf, 0, buf_size);
                                }
                                
                                out.flush();
                                out.close();
                                if(totalk < 1024 && totalk != -1){
                                    StatusBoxUtils.mainStatusAdd(" Successfully Downloaded " + totalk + " B and updated " + fileName,0);    
                                }else if(totalk == -1 ){
                                    File temp_file = new File(dir + fileName);
                                    totalk = temp_file.length();
                                    if(totalk < 1024)
                                        StatusBoxUtils.mainStatusAdd(" Successfully Downloaded " + totalk + " B and updated " + fileName,0);
                                    else
                                        StatusBoxUtils.mainStatusAdd(" Successfully Downloaded " + totalk/1024 + " KB and updated " + fileName,0);
                                }else{
                                    StatusBoxUtils.mainStatusAdd(" Successfully Downloaded " + totalk/1024 + " KB and updated " + fileName,0);
                                }
                                
                            }
                                                     
                                                       
                        
                                postCommands();    
                            
                            
                            data.close();
                        
                        }  catch(Exception e){

                        }
                    return( true );
                    }

                    public void reportPercentComplete(ResourceDownloader downloader, final int percentage) {
                        percentageCommands(percentage);
                    }
                    public void reportActivity(ResourceDownloader downloader, String activity) {
                        activityCommands(activity);
                  
                    }
                    public void failed(ResourceDownloader downloader, ResourceDownloaderException e) {
                       e.printStackTrace();
                        failedCommands(); 
                    }

					public void reportAmountComplete(ResourceDownloader arg0,
							long arg1) {
						// TODO Auto-generated method stub
						
					}
                
                };
        rd_t.addListener(rdl);
        preCommands();
        rd_t.asyncDownload();
    }

    public void postCommands() {
        
        
    }
  
      
    public void percentageCommands(int percentage){
        System.out.println(percentage);
    }
    
    public void activityCommands(String activity){
        System.out.println(activity);
    }
    
    public void failedCommands(){
        
    }
}
