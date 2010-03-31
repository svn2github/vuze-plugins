/*
 * Created on Apr 7, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities.download;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;

import omschaub.azcvsupdater.main.StatusBox;
import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.Tab1;
import omschaub.azcvsupdater.main.Tab1Utils;
import omschaub.azcvsupdater.main.Tab1_Subtab_1;
import omschaub.azcvsupdater.main.Tab6Utils;
import omschaub.azcvsupdater.main.View;
import omschaub.azcvsupdater.utilities.ButtonStatus;
import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.Restart;
import omschaub.azcvsupdater.utilities.StackX;
import omschaub.azcvsupdater.utilities.TorrentUtils;



public class MainCVSGet extends DownloadImp{
    public void preCommands(){
        View.getDisplay().asyncExec(new Runnable (){
            public void run () {                    
                Tab1.downloadVisible(true,false);
                Tab1.setCustomPB(new Integer(0));
                
                if(Tab1.cancel_download != null && !Tab1.cancel_download.isDisposed()){
                    
                    Tab1.cancel_download.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) {
                            if(!rd_t.isCancelled())
                                rd_t.removeListener(rdl);
                                rd_t.cancel();
                                removeDownload(Tab1_Subtab_1.version,View.getPluginInterface());
                                
                                if(StatusBox.status_group != null && !StatusBox.status_group.isDisposed()){
                                    //We should NEVER pack the status_group
                                    //StatusBox.status_group.pack();
                                    StatusBox.status_group.layout();
                                }
                                
                                
                                StatusBoxUtils.mainStatusAdd(" Torrent Download Cancelled",1);
                                Tab6Utils.refreshLists();
                                Tab1.downloadVisible(false,false);
                                Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
                                Tab1.cancel_download.removeListener(0,this);
                        }
                    });
                }
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
        
        
        //Cleanup calls
        
        //Get rid of progress bar
        Tab1.downloadVisible(false,false);
               
        StatusBoxUtils.mainStatusAdd(" Sucessfully Downloaded " + totalk/1024 + " KB",0);
        Tab6Utils.refreshLists();
        //ButtonStatus.set(true, true, false, true, true);
        Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
        
              
        if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false) &&
                !Tab1_Subtab_1.version.startsWith("Checking")){
            TorrentUtils.setForceSeed(Tab1_Subtab_1.version);
        }
        
        
        //AutoInsert / AutoRestart thread      
        if(!View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Azureus_downloadTracer",false)) {
            if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoRestart")||
                    View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoInsert"))
            {
                
                final String file = new String(dir + System.getProperty("file.separator")+ Tab1_Subtab_1.version);
                Thread download_main_thread = new Thread() 
                    {
                        public void run() 
                        {
                            try 
                            {
                                    
                                    //String output = ((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoRestart"))?"Going to restart":"File Inserted for later restart");
                                    //System.out.println(output);
                                    StackX from_string = new StackX(10);
                                    StackX to_string = new StackX(10);
                                    from_string.push(file);
                                    to_string.push(View.getJARFileDestination());
                                    
                                    if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoRestart")){
                                        Restart.updateRestart(View.getPluginInterface(),from_string, to_string, true);
                                    }else
                                        Restart.updateNORestart(View.getPluginInterface(),from_string,to_string);
                                     
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                
                    download_main_thread.start();                        
                }
                
                
            } 
            
        View.getPluginInterface().getPluginconfig().setPluginParameter("Azureus_downloadTracer",false);
        
    }
    
    public void percentageCommands(final int percentage){
        Tab1.setCustomPB(new Integer(percentage*10));
    }
    
    public void activityCommands(String activity){
       //System.out.println(activity);
    }
    public void failedCommands(){
        
        Tab1.downloadVisible(false,false);
        
        //StatusBoxUtils.mainStatusAdd(" Failed to the torrent for a CVS Update.. Aetelis server might be down, try again later.");
        StatusBoxUtils.mainStatusAdd(" Torrent download failed.. Falling back to alternative HTTP download",2);

        ButtonStatus.set(true, true, false, true, true);
        Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
        
        
        //Start the alternate HTTP method if the main method fails and if we have a real version
        if (Tab1_Subtab_1.version.equals("Checking...."))
            return;
        AltCVSGet altget = new AltCVSGet();
        altget.setURL("http://azureus.sourceforge.net/cvs/" + Tab1_Subtab_1.version);
        altget.setDir(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator"));
        altget.setFileName(Tab1_Subtab_1.version);
        altget.initialize();
        altget.start();
        
    }
    
    public static void removeDownload(final String name, final PluginInterface pm){
        final DownloadManager dm;
        dm = pm.getDownloadManager();
        View.DML_BOOLEAN = false;
            DownloadManagerListener dml = new DownloadManagerListener()
            {
                public void
                downloadAdded(
                final Download  download )
                {
                                    
                    if(download.getName().equals(name))
                    {
                        View.DML_BOOLEAN = true;
                        try 
                        {
                            if(download.getState() != Download.ST_STOPPED)
                                download.stop();
                            download.remove(true,true);
                            dm.removeListener(this);
                            
                        } catch (DownloadException e) {
                            e.printStackTrace();
                        } catch (DownloadRemovalVetoException e1) {
                            e1.printStackTrace();
                        }
                    }
                    dm.removeListener(this);
                }
                    public void
                    downloadRemoved(
                        Download    download )
                    {
                        
                }
            };
        

            dm.addListener(dml);
        
    }   
    


}//EOF
