/*
 * Created on Feb 26, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import omschaub.azcvsupdater.utilities.DownloaderMain;
import omschaub.azcvsupdater.utilities.Time;
import omschaub.azcvsupdater.utilities.TorrentUtils;
import omschaub.azcvsupdater.utilities.URLReader;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

class Timers {
    static UTTimer tim1;
    
    
    /**
     *Timer Event to check the CVS page every "WebUpdatePeriod"
     *@param pluginInterface
     */
    static synchronized void checkForNewVersion(final PluginInterface pluginInterface)
    {
       
        tim1 = pluginInterface.getUtilities().createTimer("poo");
        
        tim1.addPeriodicEvent((pluginInterface.getPluginconfig().getPluginIntParameter("WebUpdatePeriod",60)*60*1000),
        
        new UTTimerEventPerformer()

        {

        public void

        perform(

        UTTimerEvent ev1 )

        {
            try {
                
                
                //System.out.println("Timer Running");
                
                
                View.AUTO_ONCE = true;
                
                
                    URLReader.newGetURL();
                    DownloaderMain.autoDownloader(pluginInterface);
                    if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false) &&
                            !Tab1_Subtab_1.version.startsWith("Checking")){
                        TorrentUtils.setForceSeed(Tab1_Subtab_1.version);
                    }
                    
                    View.asyncExec(new Runnable (){
                        public void run () {
                            if(Tab1_Subtab_1.lastCheck !=null && !Tab1_Subtab_1.lastCheck.isDisposed()){
                                Tab1_Subtab_1.lastCheck.setText("Latest Check: " + Time.getCVSTime());
                                pluginInterface.getPluginconfig().setPluginParameter("dateNextTime",Time.getCVSTimeNext((pluginInterface.getPluginconfig().getPluginIntParameter("WebUpdatePeriod") * 60 * 1000)));
                                Tab1_Subtab_1.nextAutoCheck.setText("Next Auto Check: " + pluginInterface.getPluginconfig().getPluginStringParameter("dateNextTime","Checking..."));
                            }
                            if(Tab1_Subtab_1.version_color !=null && !Tab1_Subtab_1.version_color.isDisposed())
                                Tab1_Subtab_1.version_color.setText(Tab1_Subtab_1.version);
                            }
                        });
                    
                    
            }catch(Exception f) {
                    View.asyncExec(new Runnable (){
                    	public void run () {
                            if(Tab1_Subtab_1.lastCheck !=null && !Tab1_Subtab_1.lastCheck.isDisposed())
                                Tab1_Subtab_1.lastCheck.setText("Latest Check: Could Not Update!");
                            if(Tab1_Subtab_1.displayVersion !=null && !Tab1_Subtab_1.displayVersion.isDisposed())
                                Tab1_Subtab_1.version_color.setText("Could Not Update!");
                            }
                        });
                    f.printStackTrace(); 
                }
            
            
        
        
        }

        });
        
    }


    
    
    
  static void destroyTimers(){
      if(tim1 != null)
          tim1.destroy();
  }
            
}
