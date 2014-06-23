/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Threads;

import Manager.ConfigManager;
import Manager.DownloadManager;
import org.gudy.azureus2.plugins.PluginInterface;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Bruno
 * Thread que busca legendas periodicamente para os torrents finalizados
 * que ainda estão sem legendas
 */
public class DownloadPeriodThread extends BaseThread implements Runnable {
    private ConfigManager _configManager;
    private boolean _threadOn = true;

    private void SleepLittle(int milisec) throws InterruptedException {
        int times = milisec / 1000;
        if (times <= 0)
            times = 1;
        for (int i=0;i<times;i++) {
            if (!_threadOn)
                break;
            Thread.sleep(1000);
        }
    }
    
    public DownloadPeriodThread(PluginInterface pluginInterface) {
        super(pluginInterface);
        _configManager = new ConfigManager(pluginInterface);
    }

    public void run() {
        try {
            while (_threadOn) {
                if (_configManager.getPluginActive()) {
                    DownloadManager manager = new DownloadManager(_pluginInterface);
                    manager.getSubTitleForAllCompletedMovies(false);
                }
                SleepLittle(_configManager.getIntervalSearch());
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DownloadPeriodThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stopSearching() {
        _threadOn = false;
    }

}
