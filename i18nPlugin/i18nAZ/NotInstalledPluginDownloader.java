package i18nAZ;

import i18nAZ.RemotePluginManager.RemotePlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Vector;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.pluginsimpl.PluginUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderAlternateImpl;
import org.gudy.azureus2.pluginsimpl.update.PluginUpdatePlugin;

public class NotInstalledPluginDownloader implements iTask
{
    final private static Task instance = new Task("NotInstalledPluginDownloader", 60, new NotInstalledPluginDownloader());

    final private static Vector<NotInstalledPluginDownloaderListener> listeners = new Vector<NotInstalledPluginDownloaderListener>();
    private static AESemaphore semaphore = null;
    private static ResourceDownloaderAlternateImpl resourceDownloaderAlternate = null;
    final private static Object resourceDownloaderAlternateMutex = new Object();

    public static synchronized void addListener(NotInstalledPluginDownloaderListener listener)
    {
        if (!NotInstalledPluginDownloader.listeners.contains(listener))
        {
            NotInstalledPluginDownloader.listeners.addElement(listener);
        }
    }

    public static synchronized void deleteListener(NotInstalledPluginDownloaderListener listener)
    {
        NotInstalledPluginDownloader.listeners.removeElement(listener);
    }

    public static synchronized void deleteListeners()
    {
        if (NotInstalledPluginDownloader.listeners != null)
        {
            NotInstalledPluginDownloader.listeners.removeAllElements();
        }
    }

    public static void notifyListeners(NotInstalledPluginDownloaderEvent event)
    {
        Object[] localListeners = null;
        synchronized (NotInstalledPluginDownloader.listeners)
        {
            localListeners = NotInstalledPluginDownloader.listeners.toArray();
        }

        for (int i = localListeners.length - 1; i >= 0; i--)
        {
            ((NotInstalledPluginDownloaderListener) localListeners[i]).complete(event);
        }
    }

    
    public void check()
    {
        while (PluginCoreUtils.isInitialisationComplete() == false)
        {
            Util.sleep(1000);
        }

        File notInstalledpluginFolderFile = new File(i18nAZ.getPluginInterface().getPluginDirectoryName() + File.separator + "notinstalledplugins" + File.separator);
        RemotePlugin[] remotePlugins = RemotePluginManager.toArray();
        for (int i = 0; i < remotePlugins.length; i++)
        {
            boolean download = false;
            final File destFile = new File(notInstalledpluginFolderFile + File.separator + new File(remotePlugins[i].getDownloadURL().toString()).getName());
            PluginInterface[] notInstalledPluginInterfaces = null;
            if (Path.exists(destFile) == true)
            {
                notInstalledPluginInterfaces = NotInstalledPluginManager.toArray(remotePlugins[i].getId());

                for (int j = 0; j < notInstalledPluginInterfaces.length; j++)
                {
                    if (notInstalledPluginInterfaces[j].getPluginID().toLowerCase(Locale.US).equals(remotePlugins[i].getId().toLowerCase(Locale.US)))
                    {
                        String pluginInterfaceVersion = notInstalledPluginInterfaces[j].getPluginVersion();
                        if (pluginInterfaceVersion != null)
                        {
                            int comp = PluginUtils.comparePluginVersions(pluginInterfaceVersion, remotePlugins[i].getVersion());
                            if (comp < 0)
                            {
                                download = true;
                                continue;
                            }
                        }
                    }
                }
            }
            else
            {
                download = true;
            }
            PluginInterface pluginInterface = i18nAZ.getPluginInterface().getPluginManager().getPluginInterfaceByID(remotePlugins[i].getId());
            if (pluginInterface != null)
            {
                String pluginInterfaceVersion = pluginInterface.getPluginVersion();
                int comp = pluginInterfaceVersion==null?-1:PluginUtils.comparePluginVersions(pluginInterfaceVersion, remotePlugins[i].getVersion());	// PARG
                if (comp >= 0)
                {
                    destFile.delete();
                    download = false;
                }
            }

            if (download == false)
            {
                continue;
            }
            destFile.delete();
            synchronized (NotInstalledPluginDownloader.resourceDownloaderAlternateMutex)
            {
                NotInstalledPluginDownloader.semaphore = new AESemaphore("NotInstalledPluginDownloader");
                ResourceDownloader resourceDownloaderURLImpl1 = i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().create(remotePlugins[i].getDownloadURL());
                ResourceDownloader resourceDownloaderURLImpl2 = i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().createWithAutoPluginProxy(remotePlugins[i].getDownloadURL());
                ResourceDownloader ResourceDownloaderTorrentImpl = i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().create(remotePlugins[i].getDownloadTorrentURL());
                ResourceDownloaderTorrentImpl = i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().getSuffixBasedDownloader(ResourceDownloaderTorrentImpl);

                NotInstalledPluginDownloader.resourceDownloaderAlternate = (ResourceDownloaderAlternateImpl) i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().getAlternateDownloader(new ResourceDownloader[] { ResourceDownloaderTorrentImpl, resourceDownloaderURLImpl1, resourceDownloaderURLImpl2 });
                try
                {
                    i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().getTimeoutDownloader(i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().getRetryDownloader(NotInstalledPluginDownloader.resourceDownloaderAlternate, PluginUpdatePlugin.RD_SIZE_RETRIES), PluginUpdatePlugin.RD_SIZE_TIMEOUT).getSize();
                }
                catch (ResourceDownloaderException e)
                {
                }

                NotInstalledPluginDownloader.resourceDownloaderAlternate.addListener(new ResourceDownloaderListener()
                {

                    
                    public void reportPercentComplete(ResourceDownloader downloader, int percentage)
                    {
                    }

                    
                    public void reportAmountComplete(ResourceDownloader downloader, long amount)
                    {
                    }

                    
                    public void reportActivity(ResourceDownloader downloader, String activity)
                    {
                    }

                    
                    public boolean completed(ResourceDownloader downloader, InputStream data)
                    {
                        boolean success = false;
                        OutputStream output = null;
                        destFile.getParentFile().mkdirs();
                        try
                        {
                            output = new FileOutputStream(destFile);

                            byte[] buf = new byte[1024];

                            int bytesRead;

                            while ((bytesRead = data.read(buf)) > 0)
                            {
                                output.write(buf, 0, bytesRead);
                            }
                            success = true;
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        finally
                        {
                            try
                            {
                                data.close();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                            try
                            {
                                output.close();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        if (success == true)
                        {
                            NotInstalledPluginDownloader.notifyListeners(new NotInstalledPluginDownloaderEvent(destFile));
                        }
                        NotInstalledPluginDownloader.semaphore.release();
                        return false;
                    }

                    
                    public void failed(ResourceDownloader downloader, ResourceDownloaderException e)
                    {
                        NotInstalledPluginDownloader.semaphore.release();
                    }
                });
                NotInstalledPluginDownloader.resourceDownloaderAlternate.asyncDownload();
            }
            NotInstalledPluginDownloader.semaphore.reserve();
            synchronized (NotInstalledPluginDownloader.resourceDownloaderAlternateMutex)
            {
                NotInstalledPluginDownloader.semaphore = null;
                boolean isCancelled = NotInstalledPluginDownloader.resourceDownloaderAlternate.isCancelled();
                NotInstalledPluginDownloader.resourceDownloaderAlternate = null;
                if (isCancelled == true)
                {
                    break;
                }
            }
        }

        if (NotInstalledPluginManager.isStarted() == false)
        {
            NotInstalledPluginManager.start();
        }
    }

    
    public void onStart()
    {
        NotInstalledPluginDownloader.addListener(new NotInstalledPluginDownloaderListener()
        {
            
            public void complete(NotInstalledPluginDownloaderEvent e)
            {
                NotInstalledPluginManager.signal();
            }
        });
    }

    
    public void onStop(StopEvent e)
    {
        synchronized (NotInstalledPluginDownloader.resourceDownloaderAlternateMutex)
        {
            if (NotInstalledPluginDownloader.resourceDownloaderAlternate != null)
            {
                NotInstalledPluginDownloader.resourceDownloaderAlternate.cancel();
                NotInstalledPluginDownloader.semaphore.reserve();
            }
        }

        NotInstalledPluginDownloader.deleteListeners();
        NotInstalledPluginManager.stop();
    }

    public static void signal()
    {
        NotInstalledPluginDownloader.instance.signal();
    }

    public static void start()
    {
        NotInstalledPluginDownloader.instance.start();
    }

    public static void stop()
    {
        NotInstalledPluginDownloader.instance.stop();
    }

    public static boolean isStarted()
    {
        return NotInstalledPluginDownloader.instance.isStarted();
    }
}

class NotInstalledPluginDownloaderEvent
{
    File notInstalledPluginFile = null;

    NotInstalledPluginDownloaderEvent(File notInstalledPluginFile)
    {
        this.notInstalledPluginFile = notInstalledPluginFile;
    }
}

interface NotInstalledPluginDownloaderListener
{
    void complete(NotInstalledPluginDownloaderEvent e);
}
