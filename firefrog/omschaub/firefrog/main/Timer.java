/*
 * Created on May 5, 2005
 * Created by omschaub
 * 
 */
package omschaub.firefrog.main;

import java.util.HashMap;
import java.util.Map;


import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

public class Timer {
    private static UTTimer TIMER1;
    public static Label downloadEmpty, seedEmpty, hiddenEmpty;
    private static int[][] oldDownloads;
    private static int[][] newDownloads;
    private static int oldDownloadGroupSize, newDownloadGroupSize;
    
    /**
     * starts the Timer Event to pull download stats on a regular basis
     *
     */
    public static synchronized void runMainTimer()
    {
        TIMER1 = Plugin.getPluginInterface().getUtilities().createTimer("timer1");
        
        TIMER1.addPeriodicEvent(1000,
                
        new UTTimerEventPerformer()

        {

        public void

        perform(

        UTTimerEvent ev1 )

        {

        try {
            
            //Update total speed dialog
            if(DownloadManagerShell.DOWNLOAD_MANAGER_SHELL != null && !DownloadManagerShell.DOWNLOAD_MANAGER_SHELL.isDisposed()){
                long down_speed = 0, up_speed = 0;
                Download[] downloads = Plugin.getPluginInterface().getDownloadManager().getDownloads();
                for (int i = 0 ; i < downloads.length; i++){
                    down_speed = down_speed + downloads[i].getStats().getDownloadAverage();
                    up_speed = up_speed + downloads[i].getStats().getUploadAverage();
                }
                DownloadManagerShell.setTitle(down_speed, up_speed);
                
            }
            
            
            //Timer code to 'periodically' catch the download stats
            String[] hidden = HideFiles.read();
            
            int countDownloads=1, countSeeds=1, countHides=1;
            
            final DownloadManager dm;
            dm = Plugin.getPluginInterface().getDownloadManager();
            Download[] downloads = dm.getDownloads();
            
            int[] counts = getCounts();
            
            if(counts[1] == 0)
                addEmptyDownloadMessage();
            else
                removeEmptyDownloadMessage();
            
            if(counts[2] == 0)
                addEmptySeedMessage();
            else
                removeEmptySeedMessage();
            
            
            if(counts[3] == 0 && !DownloadManagerShell.COLLAPSED)
                addEmptyHiddenMessage();
            else 
                removeEmptyHiddenMessage();
           
            Map hasChanged = getChanged();
            downloadGroupGetSize();
            for(int i = 0 ; i < downloads.length ; i++){
                //check to see if file should be hidden
                boolean isHidden = false, isLastDownload = false, isLastSeed = false, isLastHide = false;
                for(int j = 0; j < hidden.length ; j++){
                    if(hidden[j] != null && hidden[j].equalsIgnoreCase(Utilities.verifyName(downloads[i].getName())))
                        isHidden = true;
                }
                
                //see if it has changed from the last time
                boolean repaint = true;
                
                try{
                    
                    if(oldDownloadGroupSize == newDownloadGroupSize){
                        repaint = ((Boolean)hasChanged.get(Utilities.verifyName(downloads[i].getName()))).booleanValue();    
                    }else{
                        repaint = true;
                    }
                    
                    
                }catch(Exception e){
                    repaint = true;
                }
                //System.out.println(oldDownloadGroupSize + " : " + newDownloadGroupSize + " :" + repaint + " : " + downloads[i].getName());
                
                if(downloads[i].getState() == Download.ST_DOWNLOADING || downloads[i].getState() == Download.ST_PREPARING) {
                    if(countDownloads == counts[1]) isLastDownload = true;
                    DownloadManagerUtils.addDownload(downloads[i], isLastDownload, repaint);
                    countDownloads++;
                }else if(downloads[i].getState() == Download.ST_SEEDING){
                    if(isHidden){                    
                        if(countHides == counts[3]) isLastHide = true;
                        DownloadManagerUtils.addHidden(downloads[i],isLastHide);
                        countHides++;
                    }else{    
                        if(countSeeds == counts[2]) isLastSeed = true;
                        DownloadManagerUtils.addSeed(downloads[i], isLastSeed);
                        countSeeds++;
                    }
                }else if(downloads[i].getState() == Download.ST_QUEUED){
                    if(downloads[i].isComplete()){
                        if(isHidden){                    
                            if(countHides == counts[3]) isLastHide = true;
                            DownloadManagerUtils.addHidden(downloads[i],isLastHide);
                            countHides++;
                        }else{    
                            if(countSeeds == counts[2]) isLastSeed = true;
                            DownloadManagerUtils.addSeed(downloads[i], isLastSeed);
                            countSeeds++;
                        }
                    }else{
                        if(countDownloads == counts[1]) isLastDownload = true;
                        DownloadManagerUtils.addDownload(downloads[i], isLastDownload, repaint);
                        countDownloads++;
                    }
                }else if(downloads[i].getState() == Download.ST_STOPPED){
                    if(downloads[i].isComplete()){
                        if(isHidden){                    
                            if(countHides == counts[3]) isLastHide = true;
                            DownloadManagerUtils.addHidden(downloads[i],isLastHide);
                            countHides++;
                        }else{    
                            if(countSeeds == counts[2]) isLastSeed = true;
                            DownloadManagerUtils.addSeed(downloads[i], isLastSeed);
                            countSeeds++;
                        }
                    }else{
                        if(countDownloads == counts[1]) isLastDownload = true;
                        DownloadManagerUtils.addDownload(downloads[i], isLastDownload, repaint);
                        countDownloads++;
                    }
                }
            }
            oldDownloadGroupSize = newDownloadGroupSize;
            
        }catch(Exception f) {
           f.printStackTrace(); 
        }
    
    }

    });
    
}

    public static void destroyTimer(){
        TIMER1.destroy();
    }
    
    /**  
     * Counts the downloads, seeds, and hidden files
     * @return int[] counts[3] 1= downloads 2=seeds 3=hidden
     */
    public static int[] getCounts(){
        int[] counts = new int[4];
        counts[1] = 0;
        counts[2] = 0;
        counts[3] = 0;
        String[] hidden = HideFiles.read();
        final DownloadManager dm;
        dm = Plugin.getPluginInterface().getDownloadManager();
        Download[] downloads = dm.getDownloads();
       
        for(int i = 0 ; i < downloads.length; i++){
            //check to see if file should be hidden
            boolean isHidden = false;
            for(int j = 0; j < hidden.length ; j++){
                if(hidden[j] != null && hidden[j].equalsIgnoreCase(Utilities.verifyName(downloads[i].getName())))
                    isHidden = true;
            }
            if(downloads[i].getState() == Download.ST_DOWNLOADING) {
                counts[1]++;
            }else if(downloads[i].getState() == Download.ST_SEEDING){
                if(isHidden)                    
                    counts[3]++;
                else    
                    counts[2]++;
             
            }else if(downloads[i].getState() == Download.ST_QUEUED){
                if(downloads[i].isComplete()){
                    if(isHidden)
                        counts[3]++;
                    else    
                        counts[2]++;
                }else{
                    counts[1]++;
                }
            }else if(downloads[i].getState() == Download.ST_STOPPED){
                if(downloads[i].isComplete()){
                    if(isHidden) 
                        counts[3]++;
                    else    
                        counts[2]++;
                }else{
                    counts[1]++;
                }
            }else {
                if(downloads[i].isComplete())
                    counts[2]++;
                else
                    counts[1]++;
            }
        }
        //System.out.println("Downloads: " + counts[1] + " Seeds: " + counts[2] + " Hidden: " + counts[3]);
        return counts;
    }
    
    public static void addEmptyDownloadMessage(){
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        { 
                            if(DownloadManagerShell.DOWNLOADS != null && !DownloadManagerShell.DOWNLOADS.isDisposed()){
                                if(downloadEmpty == null || downloadEmpty.isDisposed()){
                                    downloadEmpty = new Label(DownloadManagerShell.DOWNLOADS, SWT.NULL);
                                    downloadEmpty.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                    downloadEmpty.setText("No downloads right now");    
                                    
                                    DownloadManagerShell.DOWNLOADS.layout();
                                    DownloadManagerShell.DOWNLOADS.getParent().layout();
                                }
                                    
                            }                          
                        }
                    });    
        }
        
    }
    
    public static void removeEmptyDownloadMessage(){
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        { 
                            if(downloadEmpty != null && !downloadEmpty.isDisposed()){
                                downloadEmpty.dispose();
                            }                
                        }
                    });    
        }        
    }
    
    public static void addEmptySeedMessage(){
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        { 
                            if(DownloadManagerShell.CURRENT_RESPONSES != null && !DownloadManagerShell.CURRENT_RESPONSES.isDisposed()){
                                if(seedEmpty == null || seedEmpty.isDisposed()){
                                    seedEmpty = new Label(DownloadManagerShell.CURRENT_RESPONSES, SWT.NULL);
                                    seedEmpty.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                    seedEmpty.setText("No uploads right now");
                                    
                                    DownloadManagerShell.CURRENT_RESPONSES.layout();
                                    DownloadManagerShell.CURRENT_RESPONSES.getParent().layout();
                                }
                                    
                            }
                        }
                    });            
        }
    }
    
    public static void removeEmptySeedMessage(){
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        { 
                            if(seedEmpty != null && !seedEmpty.isDisposed()){
                                seedEmpty.dispose();
                            }
                        }
                    });    
        }
    }
    
    public static void addEmptyHiddenMessage(){
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        { 
                            if(DownloadManagerShell.HIDDEN != null && !DownloadManagerShell.HIDDEN.isDisposed()){
                                if(hiddenEmpty == null || hiddenEmpty.isDisposed()){
                                    hiddenEmpty = new Label(DownloadManagerShell.HIDDEN, SWT.NULL);
                                    hiddenEmpty.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                    hiddenEmpty.setText("No hidden uploads right now");
                                    
                                    DownloadManagerShell.HIDDEN.layout();
                                    DownloadManagerShell.HIDDEN.getParent().layout();
                                }
                                    
                            }
                        }
                    });    
        }
        
    }
    
    public static void removeEmptyHiddenMessage(){
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        { 
                            if(hiddenEmpty != null && !hiddenEmpty.isDisposed()){
                                hiddenEmpty.dispose();
                            }
                        }
                    });    
        }
    }
    
    private static void downloadGroupGetSize(){
        
        if(Utilities.getDisplay() != null && !Utilities.getDisplay().isDisposed()){
            Utilities.getDisplay().syncExec(new Runnable ()
                    {
                        public void run () 
                        { 
                            if(DownloadManagerShell.DOWNLOADS != null && !DownloadManagerShell.DOWNLOADS.isDisposed()){
                                newDownloadGroupSize = DownloadManagerShell.DOWNLOADS.getSize().x;
                            }
                        }
                    });    
        }
        
      
   }
    
    
    private static Map getChanged(){
        Map hasChanged = new HashMap();
        Download[] downloads = placeNewDownloads();
        
        
        for(int i = 0 ; i < newDownloads.length ; i++){
            try{
                String key = Utilities.verifyName(downloads[newDownloads[i][0]].getName());
                
                if(oldDownloads[i][1] == newDownloads[i][1]){
                    hasChanged.put(key, new Boolean (false));
                }else{
                    hasChanged.put(key, new Boolean (true));
                }
                //System.out.println(key + " : " + hasChanged.get(key) + " : " + oldDownloads[i][1] +  " : " + newDownloads[i][1]);
            }catch(Exception e){
                //e.printStackTrace();
            }
        }
                
        oldDownloads = newDownloads;
        
        
        
        
        return hasChanged;
    }
    
    private static Download[] placeNewDownloads(){
        newDownloads = null;
        final DownloadManager dm;
        dm = Plugin.getPluginInterface().getDownloadManager();
        Download[] downloads = dm.getDownloads();
        Download[] new_downloads = new Download[downloads.length];
        newDownloads = new int[downloads.length][2];
        
        for(int i = 0 ; i < downloads.length ; i++){
            if(!downloads[i].isComplete()){
                newDownloads[i][0] = i;
                newDownloads[i][1] = downloads[i].getStats().getCompleted();
                new_downloads[i] = downloads[i];
            }
        }
        return new_downloads;
    }
    
}//EOF