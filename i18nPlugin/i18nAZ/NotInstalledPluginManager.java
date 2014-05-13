package i18nAZ;

import i18nAZ.NotInstalledPluginManager.NotInstalledPluginInterfaceImpl;
import i18nAZ.RemotePluginManager.RemotePlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.PluginState;
import org.gudy.azureus2.plugins.clientid.ClientIDManager;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.dht.mainline.MainlineDHTManager;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.platform.PlatformManager;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.update.UpdateManager;
import org.gudy.azureus2.plugins.utils.ShortCuts;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.pluginsimpl.PluginUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.launch.PluginLauncherImpl;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
public class NotInstalledPluginManager implements iTask
{
    final private static Task instance = new Task("NotInstalledPluginManager", 60, (iTask) new NotInstalledPluginManager());
    
    public static class NotInstalledPluginInterfaceImpl implements PluginInterface
    {          
        private ClassLoader pluginClassLoader = null;
        private String pluginDir = null;
        private Properties pluginProperties = null;
        private String givenPluginId = null;
        private String pluginVersion = null;
        private String pluginClassName = null;
        private String pluginIdToUse = null;
        
        public NotInstalledPluginInterfaceImpl(ClassLoader pluginClassLoader, String pluginDir, Properties pluginProperties, String pluginId, String pluginVersion, String pluginClassName)
        {
            this.pluginClassLoader = pluginClassLoader;
            this.pluginDir = pluginDir;
            this.pluginProperties = pluginProperties;
            this.givenPluginId = pluginId;
            this.pluginVersion = pluginVersion;
            this.pluginClassName = pluginClassName;
        }
    
        
        public String getAzureusName() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public String getApplicationName() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public String getAzureusVersion() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void addConfigUIParameters(Parameter[] parameters, String displayName) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void addConfigSection(ConfigSection section) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void removeConfigSection(ConfigSection section) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public ConfigSection[] getConfigSections() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public Tracker getTracker() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public Logger getLogger() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public IPFilter getIPFilter() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public DownloadManager getDownloadManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public ShareManager getShareManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public TorrentManager getTorrentManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public Utilities getUtilities() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public ShortCuts getShortCuts() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public UIManager getUIManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public UpdateManager getUpdateManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void openTorrentFile(String fileName) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void openTorrentURL(String url) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public Properties getPluginProperties()
        {
            return this.pluginProperties;
        }
    
        
        public String getPluginDirectoryName()
        {
            return this.pluginDir;
        }
    
        
        public String getPerUserPluginDirectoryName() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public String getPluginName()
        {
            String name = null;
            if (this.pluginProperties != null)
            {
                name = (String) this.pluginProperties.get("plugin.name");
            }
            if (name == null)
            {
                name = this.pluginDir;
            }
            if (name == null || name.length() == 0)
            {
                name = this.pluginClassName;
            }
            return (name);
        }
    
        
        public String getPluginVersion()
        {
            String version = (String) this.pluginProperties.get("plugin.version");
            if (version == null)
            {
                version = this.pluginVersion;
            }
            return (version);
        }
    
        
        public String getPluginID()
        {
            String id = (String) this.pluginProperties.get("plugin.id");
            if (id != null && id.equals("azupdater"))
            {
                this.pluginIdToUse = id;
            }
            if (this.pluginIdToUse != null)
            {
                return this.pluginIdToUse;
            }
            if (id == null)
            {
                id = this.givenPluginId;
            }
            if (id == null)
            {
                id = "<none>";
            }
            this.pluginIdToUse = id;
            return this.pluginIdToUse;
        }
    
        
        public boolean isMandatory() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public boolean isBuiltIn() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public PluginConfig getPluginconfig() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public PluginConfigUIFactory getPluginConfigUIFactory() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public ClassLoader getPluginClassLoader()
        {
            return this.pluginClassLoader;
        }
    
        public String getPluginClassName()
        {
            return this.pluginClassName;
        }
    
        
        public PluginInterface getLocalPluginInterface(@SuppressWarnings("rawtypes") Class plugin, String id) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public IPCInterface getIPC() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public Plugin getPlugin() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public boolean isOperational() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public boolean isDisabled() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void setDisabled(boolean disabled) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public boolean isUnloadable() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public boolean isShared() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void unload() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void reload() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void uninstall() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public boolean isInitialisationThread() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public PluginManager getPluginManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public ClientIDManager getClientIDManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public ConnectionManager getConnectionManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public MessageManager getMessageManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public DistributedDatabase getDistributedDatabase() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public PlatformManager getPlatformManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void addListener(PluginListener l) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void removeListener(PluginListener l) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void firePluginEvent(PluginEvent event) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void addEventListener(PluginEventListener l) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public void removeEventListener(PluginEventListener l) throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public MainlineDHTManager getMainlineDHTManager() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    
        
        public PluginState getPluginState() throws NotImplementedException
        {
            throw new NotImplementedException();
        }
    }    
    
    final static public int ACTION_ADDED = 1;
    final static public int ACTION_REMOVED = 2;

    final private static Vector<NotInstalledPluginInterfaceListener> listeners = new Vector<NotInstalledPluginInterfaceListener>();
    final private static Map<String, NotInstalledPluginInterfaceImpl> notInstalledPluginInterfaces = new HashMap<String, NotInstalledPluginInterfaceImpl>();
    private static NotInstalledPluginInterfaceImpl add(NotInstalledPluginInterfaceImpl notInstalledPluginInterface)
    {
        String fullId = notInstalledPluginInterface.getPluginID() + "." + notInstalledPluginInterface.getPluginClassName();
        boolean notify = false;
        NotInstalledPluginInterfaceImpl notInstalledPluginInterfaceImpl = null;
        synchronized (NotInstalledPluginManager.notInstalledPluginInterfaces)
        {
            if(NotInstalledPluginManager.notInstalledPluginInterfaces.containsKey(fullId) == false)
            {
                notInstalledPluginInterfaceImpl = NotInstalledPluginManager.notInstalledPluginInterfaces.put(fullId, notInstalledPluginInterface);
                notify = NotInstalledPluginManager.notInstalledPluginInterfaces.containsKey(fullId) == true;
            }            
        }
        if (notify == true)
        {
            NotInstalledPluginManager.notifyListeners(new NotInstalledPluginInterfaceEvent(notInstalledPluginInterface, NotInstalledPluginManager.ACTION_ADDED));
        }
        return notInstalledPluginInterfaceImpl;
    }
    public static synchronized void addListener(NotInstalledPluginInterfaceListener listener)
    {
        if (!NotInstalledPluginManager.listeners.contains(listener))
        {
            NotInstalledPluginManager.listeners.addElement(listener);
        }
    }
    public static boolean contains(NotInstalledPluginInterfaceImpl notInstalledPluginInterface)
    {
        return NotInstalledPluginManager.contains(notInstalledPluginInterface.getPluginID(), notInstalledPluginInterface.getPluginClassName());
    }
    public static boolean contains(String pluginID, String pluginClassName)
    {
        String fullId = pluginID + "." + pluginClassName;
        synchronized (NotInstalledPluginManager.notInstalledPluginInterfaces)
        {
            return NotInstalledPluginManager.notInstalledPluginInterfaces.containsKey(fullId);
        }
    }
    public static synchronized void deleteListener(NotInstalledPluginInterfaceListener listener)
    {
        NotInstalledPluginManager.listeners.removeElement(listener);
    }
    public static synchronized void deleteListeners()
    {
        if (NotInstalledPluginManager.listeners != null)
        {
            NotInstalledPluginManager.listeners.removeAllElements();
        }
    }
    private static NotInstalledPluginInterfaceImpl[] create(File archivePath, String pluginID)
    {
        List<NotInstalledPluginInterfaceImpl> notInstalledPluginInterfaces = new ArrayList<NotInstalledPluginInterfaceImpl>();
        List<File> pluginContentsList = new ArrayList<File>();
        if (archivePath.getName().toLowerCase(Locale.US).endsWith(".jar") == true)
        {
            pluginContentsList.add(archivePath);
        }
        else if (archivePath.getName().toLowerCase(Locale.US).endsWith(".zip") == true)
        {
            ArchiveFile archiveFile = null;
            try
            {
                archiveFile = new ArchiveFile(archivePath);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if (archiveFile != null)
            {
                Enumeration<JarEntry> entries = archiveFile.entries();
                while (entries.hasMoreElements())
                {
                    JarEntry jarEntry = entries.nextElement();
                    if (jarEntry.isDirectory() == false)
                    {
                        pluginContentsList.add(new File(archivePath.getPath() + "!/" + jarEntry.getName()));
                    }
                }
            }
        }
    
        File[] pluginContents = pluginContentsList.toArray(new File[pluginContentsList.size()]);
    
        if (pluginContents.length != 0)
        {
            boolean looksLikePlugin = false;
            for (int j = 0; j < pluginContents.length; j++)
            {
                String name = pluginContents[j].getName().toLowerCase();
                if (name.endsWith(".jar") || name.equals("plugin.properties") || name.equals("./plugin.properties"))
                {
                    looksLikePlugin = true;
                    break;
                }
            }
            if (looksLikePlugin == true)
            {
                String[] plugin_version = { null };
                String[] plugin_id = { null };
    
                ClassLoader root_class_loader = PluginInitializer.class.getClassLoader();
                ClassLoader plugin_class_loader = root_class_loader;
                pluginContents = PluginLauncherImpl.getHighestJarVersions(pluginContents, plugin_version, plugin_id, true);
                for (int j = 0; j < pluginContents.length; j++)
                {
                    if (pluginContents[j].getName().endsWith(".jar") == true)
                    {
                        URL jarUrl = Path.getUrl(pluginContents[j]);
                        if (jarUrl != null)
                        {
                            plugin_class_loader = PluginLauncherImpl.extendClassLoader(root_class_loader, plugin_class_loader, jarUrl);
                        }
    
                    }
                }
                Properties props = Util.getProperties(Path.getUrl(Path.getPath(archivePath) + "!/plugin.properties"));
                if (props == null)
                {
                    props = Util.getProperties(Path.getUrl(Path.getPath(archivePath) + "!/./plugin.properties"));
                }
                if (props == null)
                {
                    HashSet<URL> jarUrls = new HashSet<URL>();
                    URL[] urls = ((URLClassLoader) plugin_class_loader).getURLs();
                    for (int j = 0; j < urls.length; j++)
                    {
                        if (urls[j].getFile() == null || urls[j].getFile().endsWith("/") == false)
                        {
                            if ("file".equals(urls[j].getProtocol()) == false)
                            {
                                if (jarUrls.contains(urls[j]) == false)
                                {
                                    jarUrls.add(urls[j]);
                                }
                            }
                        }
                    }
                    for (Iterator<URL> iterator = jarUrls.iterator(); props == null && iterator.hasNext();)
                    {
                        URL url = iterator.next();
                        ArchiveFile archiveFile = null;
                        try
                        {
                            archiveFile = new ArchiveFile(url);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        if (archiveFile != null)
                        {
                            HashSet<URL> langFileURLs = new HashSet<URL>();
                            URL[] foundedLangFiles = archiveFile.findRessources("plugin", LocalizablePluginManager.EXTENSION);
                            langFileURLs.addAll(Arrays.asList(foundedLangFiles));
                            for (Iterator<URL> iterator2 = langFileURLs.iterator(); props == null && iterator2.hasNext();)
                            {
                                URL propertiesUrl = iterator2.next();
                                if (propertiesUrl.getProtocol().equals("file") == false)
                                {
                                    props = Util.getProperties(propertiesUrl);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (props != null)
                {
                    String pluginClassNames = (String) props.get("plugin.class");
                    if (pluginClassNames == null)
                    {
                        pluginClassNames = (String) props.get("plugin.classes");
                        if (pluginClassNames == null)
                        {
                            pluginClassNames = "";
                        }
                    }
                    String pluginNames = (String) props.get("plugin.name");
                    if (pluginNames == null)
                    {
                        pluginNames = (String) props.get("plugin.names");
                    }
    
                    int pos1 = 0;
                    int pos2 = 0;
                    while (true)
                    {
                        int p1 = pluginClassNames.indexOf(";", pos1);
                        String pluginClassName = null;
                        if (p1 == -1)
                        {
                            pluginClassName = pluginClassNames.substring(pos1).trim();
                        }
                        else
                        {
                            pluginClassName = pluginClassNames.substring(pos1, p1).trim();
                            pos1 = p1 + 1;
                        }
                        String pluginName = null;
                        if (pluginNames != null)
                        {
                            int p2 = pluginNames.indexOf(";", pos2);
                            if (p2 == -1)
                            {
                                pluginName = pluginNames.substring(pos2).trim();
                            }
                            else
                            {
                                pluginName = pluginNames.substring(pos2, p2).trim();
                                pos2 = p2 + 1;
                            }
                        }
                        Properties new_props = (Properties) props.clone();
                        new_props.put("plugin.class", pluginClassName);
                        if (pluginName != null)
                        {
                            new_props.put("plugin.name", pluginName);
                        }
    
                        String pid = plugin_id[0] == null ? pluginID : plugin_id[0];
                        String pluginDir = pluginID;
                        NotInstalledPluginInterfaceImpl notInstalledPluginInterface = new NotInstalledPluginInterfaceImpl(plugin_class_loader, pluginDir, new_props, pid, plugin_version[0], pluginClassName);
                        notInstalledPluginInterfaces.add(notInstalledPluginInterface);
                        if (p1 == -1)
                        {
                            break;
    
                        }
                    }
                }
            }
        }
        return notInstalledPluginInterfaces.toArray(new NotInstalledPluginInterfaceImpl[notInstalledPluginInterfaces.size()]);
    }
    public static void notifyListeners(NotInstalledPluginInterfaceEvent event)
    {
        Object[] localListeners = null;    
        synchronized (NotInstalledPluginManager.listeners)
        {
            localListeners = NotInstalledPluginManager.listeners.toArray();
        }
    
        for (int i = localListeners.length - 1; i >= 0; i--)
        {
            ((NotInstalledPluginInterfaceListener) localListeners[i]).changed(event);
        }
    }
    private static NotInstalledPluginInterfaceImpl remove(NotInstalledPluginInterfaceImpl notInstalledPluginInterface)
    {
        String fullId = notInstalledPluginInterface.getPluginID() + "." + notInstalledPluginInterface.getPluginClassName();
        NotInstalledPluginInterfaceImpl notInstalledPluginInterfaceImpl = null;
        boolean notify = false;
        synchronized (NotInstalledPluginManager.notInstalledPluginInterfaces)
        {
            notify = NotInstalledPluginManager.notInstalledPluginInterfaces.containsKey(fullId);
            notInstalledPluginInterfaceImpl = NotInstalledPluginManager.notInstalledPluginInterfaces.remove(fullId);
            notify = notify == true && NotInstalledPluginManager.notInstalledPluginInterfaces.containsKey(fullId) == false;
        }
        if (notify == true)
        {
            NotInstalledPluginManager.notifyListeners(new NotInstalledPluginInterfaceEvent(notInstalledPluginInterface, NotInstalledPluginManager.ACTION_REMOVED));
        }
        return notInstalledPluginInterfaceImpl;
    }

    public static PluginInterface[] toArray()
    {        
        PluginInterface[] _notInstalledPluginInterfaces = null;
        synchronized (NotInstalledPluginManager.notInstalledPluginInterfaces)
        {           
            _notInstalledPluginInterfaces = NotInstalledPluginManager.notInstalledPluginInterfaces.values().toArray(new PluginInterface[NotInstalledPluginManager.notInstalledPluginInterfaces.size()]);
        }         
        return _notInstalledPluginInterfaces;
    }
    public static PluginInterface[] toArray(String pluginId)
    {        
        PluginInterface[] _notInstalledPluginInterfaces1 = NotInstalledPluginManager.toArray();
        List<PluginInterface> _notInstalledPluginInterfaces2 = new ArrayList<PluginInterface>();
        for(int i = 0; i < _notInstalledPluginInterfaces1.length; i++)
        {
            if(_notInstalledPluginInterfaces1[i].getPluginID().equalsIgnoreCase(pluginId) == true)
            {
                _notInstalledPluginInterfaces2.add(_notInstalledPluginInterfaces1[i]);
            }
        }        
        return _notInstalledPluginInterfaces2.toArray(new PluginInterface[_notInstalledPluginInterfaces2.size()]);
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
            boolean update = false;
            File destFile = new File(notInstalledpluginFolderFile + File.separator + new File(remotePlugins[i].getDownloadURL().toString()).getName());
            NotInstalledPluginInterfaceImpl[] notInstalledPluginInterfaces = null;
            if (Path.exists(destFile) == true)
            {
                notInstalledPluginInterfaces = NotInstalledPluginManager.create(destFile, remotePlugins[i].getId());
    
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
                                NotInstalledPluginManager.remove(notInstalledPluginInterfaces[j]);
                                update = true;
                                continue;
                            }
                        }
                    }
                    NotInstalledPluginManager.add(notInstalledPluginInterfaces[j]);
                }              
            }
            else
            {
                update = true;
            }            
            PluginInterface pluginInterface = i18nAZ.getPluginInterface().getPluginManager().getPluginInterfaceByID(remotePlugins[i].getId());
            if (pluginInterface != null)
            {
                String pluginInterfaceVersion = pluginInterface.getPluginVersion();
                if(pluginInterfaceVersion != null)
                {
                    int comp = PluginUtils.comparePluginVersions(pluginInterfaceVersion, remotePlugins[i].getVersion());
                    if (comp >= 0)
                    {
                        for (int j = 0; notInstalledPluginInterfaces != null && j < notInstalledPluginInterfaces.length; j++)
                        {
                            NotInstalledPluginManager.remove(notInstalledPluginInterfaces[j]);
                        }
                        destFile.delete();
                        continue;
                    }
                }                
            }
            
            if (update == false)
            {
                continue;
            }
            destFile.delete();    
        }        
    }
    
    
    public void onStart()
    {
        NotInstalledPluginManager.addListener(new NotInstalledPluginInterfaceListener()
        {
            
            public void changed(NotInstalledPluginInterfaceEvent e)
            {
                LocalizablePluginInterface.signal();                    
            }                
        });        
    }
    
    public void onStop(StopEvent e)
    {
        NotInstalledPluginManager.deleteListeners();        
        
    }
    public static void signal()
    {
        NotInstalledPluginManager.instance.signal();
    }

    public static void start()
    {
        NotInstalledPluginManager.instance.start();
    }

    public static void stop()
    {
        NotInstalledPluginManager.instance.stop();
    }

    public static boolean isStarted()
    {
        return  NotInstalledPluginManager.instance.isStarted();
    }
}
class NotInstalledPluginInterfaceEvent
{
    NotInstalledPluginInterfaceImpl notInstalledPluginInterface = null;
    int action = 0;

    NotInstalledPluginInterfaceEvent(NotInstalledPluginInterfaceImpl notInstalledPluginInterface, int action)
    {
        this.notInstalledPluginInterface = notInstalledPluginInterface;
        this.action = action;
    }
}

interface NotInstalledPluginInterfaceListener
{
    void changed(NotInstalledPluginInterfaceEvent e);
}
