/*
 * RemotePluginManager.java
 *
 * Created on April 28, 2014, 4:25
 */

package i18nAZ;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsException;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsLoader;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsLoaderFactory;

/**
 * RemotePluginManager
 * 
 * @author Repris d'injustice
 */
public class RemotePluginManager implements iTask
{
    final private static Task instance = new Task("RemotePluginManager", 60 * 60 * 24, (iTask) new RemotePluginManager());
    
    static public class RemotePlugin
    {
        private String id;
        private String version = null;
        private URL downloadURL = null;
        private URL downloadTorrentURL = null;

        public String getId()
        {
            return this.id;
        }

        public String getVersion()
        {
            return this.version;
        }

        public URL getDownloadURL()
        {
            return this.downloadURL;
        }

        public URL getDownloadTorrentURL()
        {
            return this.downloadTorrentURL;
        }
    }
   
    final private static AtomicBoolean accessible = new AtomicBoolean(false);
    final private static List<RemotePlugin> remotePlugins = new ArrayList<RemotePlugin>();
    final private static boolean isCVSVersion = i18nAZ.getPluginInterface().getUtilities().isCVSVersion();
    final private static SFPluginDetailsLoader loader = SFPluginDetailsLoaderFactory.getSingleton();

    public static RemotePlugin[] toArray()
    {        
        if(RemotePluginManager.isStarted() == false)
        {
            RemotePluginManager.start();
        }        
        synchronized (RemotePluginManager.accessible)
        {
            if(RemotePluginManager.accessible.get() == false)
            {
                return new RemotePlugin[0];
            }
        }
        synchronized (RemotePluginManager.remotePlugins)
        {           
            return RemotePluginManager.remotePlugins.toArray(new RemotePlugin[RemotePluginManager.remotePlugins.size()]);
        }
    }

    
    public void check()
    {
        while (PluginCoreUtils.isInitialisationComplete() == false)
        {
            Util.sleep(1000);      
        } 
        synchronized(remotePlugins)
        {
            remotePlugins.clear();

            loader.reset();

            SFPluginDetails[] details = null;
            try
            {
                details = loader.getPluginDetails();
            }
            catch (SFPluginDetailsException e1)
            {
                return;
            }
            for (int i = 0; i < details.length; i++)
            {
                RemotePlugin remotePlugin = new RemotePlugin();
                remotePlugin.id = details[i].getId();
                String downloadURLString = "";
                try
                {
                    if (isCVSVersion && details[i].getCVSVersion() != null && details[i].getCVSVersion().equals("") == false)
                    {
                        remotePlugin.version = details[i].getCVSVersion();
                        if (remotePlugin.version.length() > 0)
                        {
                            remotePlugin.version = remotePlugin.version.substring(0, remotePlugin.version.length() - 4);
                            downloadURLString = details[i].getCVSDownloadURL();
                        }
                    }
                    else
                    {
                        remotePlugin.version = details[i].getVersion();
                        if (remotePlugin.version.length() > 0)
                        {
                            downloadURLString = details[i].getDownloadURL();
                        }
                    }
                }
                catch (SFPluginDetailsException e)
                {
                    continue;
                }
                try
                {
                    remotePlugin.downloadURL = new URL(downloadURLString);
                    remotePlugin.downloadTorrentURL = new URL(Constants.AELITIS_TORRENTS + downloadURLString.substring(downloadURLString.lastIndexOf("/") + 1) + ".torrent");
                }
                catch (MalformedURLException e)
                {
                    continue;
                }
                if (new File(remotePlugin.downloadURL.toString()).getName().toLowerCase(Locale.US).endsWith(".jar") == false && new File(remotePlugin.downloadURL.toString()).getName().toLowerCase(Locale.US).endsWith(".zip") == false)
                {
                    continue;
                }
                remotePlugins.add(remotePlugin);
            }
        }
        synchronized (RemotePluginManager.accessible)
        {
            RemotePluginManager.accessible.set(true);
        }        
    }

    
    public void onStart()
    {
    }

    
    public void onStop(StopEvent e)
    {
    }
    
    public static void signal()
    {
        RemotePluginManager.instance.signal();
    }

    public static void start()
    {
        RemotePluginManager.instance.start();
    }

    public static void stop()
    {
        RemotePluginManager.instance.stop();
    }

    public static boolean isStarted()
    {
        return  RemotePluginManager.instance.isStarted();
    }
}
