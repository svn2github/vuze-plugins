/*
 * LocalizablePluginInterface.java
 *
 * Created on April 28, 2014, 4:25
 */
package i18nAZ;

import i18nAZ.LocalizablePluginManager.LocalizablePlugin;
import i18nAZ.NotInstalledPluginManager.NotInstalledPluginInterfaceImpl;

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
import java.util.Map.Entry;
import java.util.jar.JarEntry;

import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.pluginsimpl.PluginUtils;

/**
 * LocalizablePluginInterface
 * 
 * @author Repris d'injustice
 */
public class LocalizablePluginInterface implements iTask
{
    final private static Task instance = new Task("LocalizablePluginInterface", 5, (iTask) new LocalizablePluginInterface());
    
    final private static Vector<LocalizablePluginInterfaceListener> listeners = new Vector<LocalizablePluginInterfaceListener>();
  
    private static void uninstall(File internalPluginDir)
    {
        if (internalPluginDir.exists() == false || internalPluginDir.isDirectory() == false)
        {
            return;
        }
        File internalPluginFile = new File(internalPluginDir, "plugin.properties");
        if (internalPluginFile.exists() == true && internalPluginFile.isFile() == true)
        {
            Path.deleteFile(internalPluginFile);
        }
       

        File[] langFileDirs = internalPluginDir.listFiles();
        for (int j = 0; langFileDirs != null && j < langFileDirs.length; j++)
        {
            if (langFileDirs[j].exists() == false || langFileDirs[j].isDirectory() == false)
            {
                continue;
            }
            File[] messageBundlesFiles = langFileDirs[j].listFiles();
            for (int k = 0; messageBundlesFiles != null && k < messageBundlesFiles.length; k++)
            {
                if (messageBundlesFiles[k].isDirectory() == false && Path.getExtension(messageBundlesFiles[k]).equalsIgnoreCase(LocalizablePluginManager.EXTENSION) == true)
                {
                    continue;
                }
                Path.deleteFile(messageBundlesFiles[k]);
            }
            messageBundlesFiles = langFileDirs[j].listFiles();
            if( messageBundlesFiles != null && messageBundlesFiles.length == 0)
            {
                Path.deleteFile(langFileDirs[j]);                
            }            
        }  
        langFileDirs = internalPluginDir.listFiles();
        if( langFileDirs != null && langFileDirs.length == 0)
        {
            Path.deleteFile(internalPluginDir);                
        } 
    }

    private static URL[] searchLangFiles(URL url)
    {
        HashSet<URL> langFileURLs = new HashSet<URL>();
        ArchiveFile archiveFile = null;
        try
        {
            archiveFile = new ArchiveFile(url);
        }
        catch (IOException e)
        {
        }
        if (archiveFile != null)
        {
            URL[] foundedLangFiles = archiveFile.findContainsRessources("message", LocalizablePluginManager.EXTENSION);
            for (int i = 0; i < foundedLangFiles.length; i++)
            {
                if (Path.getFile(foundedLangFiles[i]).getName().contains("_") == false)
                {
                    langFileURLs.add(foundedLangFiles[i]);
                }
            }
            try
            {
                archiveFile.close();
            }
            catch (IOException e)
            {
            }
        }
        return langFileURLs.toArray(new URL[langFileURLs.size()]);
    }
    private static void installPlugin(String pluginId, HashSet<URL> foundedLangFiles,  PluginInterface associatedPluginInterface, boolean showLoadingState)
    {        
        File internalPluginDir = Path.getDirectory(LocalizablePluginManager.LOCALIZABLE_PLUGIN_DIR, pluginId);
        File internalPluginFile = new File(internalPluginDir, "plugin.properties");
        if (internalPluginFile.exists() == true && internalPluginFile.isFile() == true)
        {
            return;
        }
        int successCount = 0;
        for (Iterator<URL> iterator2 = foundedLangFiles.iterator(); iterator2.hasNext();)
        {
            if(LocalizablePluginInterface.installLangFile(iterator2.next(), internalPluginDir, showLoadingState) == true)
            {
                successCount++;
            }            
        }
        
        Properties properties = new Properties();
        String pluginName = LocalizablePluginManager.PLUGIN_CORE_DISPLAY_NAME;
        String pluginVersion = "<none>";
        if (associatedPluginInterface != null)
        {
            pluginName = associatedPluginInterface.getPluginName();
            pluginVersion = associatedPluginInterface.getPluginVersion();
        }

        if(pluginVersion == null)
        {
            pluginVersion = "";
        }
        
        properties.put("plugin.id", pluginId);
        properties.put("plugin.name", pluginName);
        properties.put("plugin.version", pluginVersion);   
        if (associatedPluginInterface instanceof NotInstalledPluginInterfaceImpl)
        {
            properties.put("plugin.notInstalled", "true");
        }
        else
        {
            properties.put("plugin.notInstalled", "false");
        }
        if (successCount > 0)
        {
            properties.put("plugin.state", "localizable");
        }
        else
        {
            properties.put("plugin.state", "notLocalizable");
        }
        Util.saveProperties(properties, Path.getUrl(internalPluginFile));
        LocalizablePluginInterface.notifyListeners(new LocalizablePluginInterfaceEvent(associatedPluginInterface));
    } 
    private static boolean installLangFile(URL langFileUrl, File internalPluginDir, boolean showLoadingState)
    {       
        if(showLoadingState == true)
        {
            i18nAZ.viewInstance.updateProgressBar(0);
        }
        ArrayList<URL> messageBundleURLs = new ArrayList<URL>();
        ArrayList<Long> messageBundleSize = new ArrayList<Long>();
        String langFile = langFileUrl.toString().substring(langFileUrl.toString().lastIndexOf("!/") + 2).replaceAll("/", ".");
        langFile = langFile.substring(0, langFile.lastIndexOf('.'));
        int LastDotIndex = langFile.lastIndexOf('.');
        String langDir = "default";
        if (LastDotIndex != -1)
        {
            langDir = langFile.substring(0, LastDotIndex).replace('.', '/');
        }
        File internalLangFileDir = Path.getDirectory(internalPluginDir, langDir.replace('/', '.'));
        if (internalLangFileDir.exists() == false || internalLangFileDir.isDirectory() == false)
        {
            String filename = langFile.substring(LastDotIndex + 1);
            String path = Path.getPath(langFileUrl);
            path = path.substring(0, path.length() - filename.length() - LocalizablePluginManager.EXTENSION.length());

            ArchiveFile archiveFile = null;
            try
            {
                archiveFile = new ArchiveFile(path);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if (archiveFile != null)
            {
                Enumeration<JarEntry> entries = archiveFile.entries();
                List<JarEntry> entriesList = new ArrayList<JarEntry>();               
                while (entries.hasMoreElements())
                {
                    entriesList.add(entries.nextElement());
                } 
                for(int i = 0; i < entriesList.size(); i++)
                {
                    JarEntry jarEntry = entriesList.get(i);
                    if (jarEntry.getName().startsWith(filename) == true && jarEntry.getName().endsWith(LocalizablePluginManager.EXTENSION) == true)
                    {
                        String localeFileName = Path.getFilenameWithoutExtension(jarEntry.getName());
                        Locale locale = Util.EMPTY_LOCALE;                            
                        URL messageBundleURL = Path.getUrl(path + jarEntry.getName());
                        if (filename.equals(localeFileName) == false)
                        {
                            locale = Util.getLocaleFromFilename(localeFileName);                            
                            if (locale == null)
                            {
                                continue;
                            }
                        }
                        else
                        {
                            LocaleProperties localeProperties = Util.getLocaleProperties(locale, messageBundleURL);
                            if (localeProperties == null || localeProperties.size() == 0)
                            {
                                continue;
                            }
                        }                        
                        messageBundleURLs.add(messageBundleURL);
                        messageBundleSize.add(jarEntry.getSize());                        
                    }
                }
                try
                {
                    archiveFile.close();
                }
                catch (IOException e)
                {
                }
                Float total = (float) 0;
                Float current = (float) 0;
                 for (int i = 0; i < messageBundleSize.size(); i++)
                {
                    total += messageBundleSize.get(i);
                }
                // create temp dir
                File temp = null;
                try
                {
                    temp = AETemporaryFileHandler.createTempDir();
                }
                catch (IOException e)
                {
                }
                if (temp != null)
                {
                    // copy
                    for (int i = 0; i < messageBundleURLs.size(); i++)
                    {
                        if(showLoadingState == true)
                        {
                            i18nAZ.viewInstance.updateProgressBar((float) current / total * (float) 50);                           
                        }
                        current += messageBundleSize.get(i);
                        String fileName = Path.getFilename(messageBundleURLs.get(i));
                        File destFile = new File(temp, fileName + ".original");
                        destFile.getParentFile().mkdirs();
                        if (Path.copyFile(messageBundleURLs.get(i), destFile, true) == false)
                        {
                            messageBundleURLs = null;
                            break;
                        }
                    }
                    
                    // move
                    if (messageBundleURLs != null && Path.moveDirectory(temp, internalLangFileDir))
                    {
                        if(showLoadingState == true)
                        {
                            i18nAZ.viewInstance.updateProgressBar((float) 100);
                        }
                    }
                    else
                    {
                        Path.deleteFile(temp);
                        messageBundleURLs = null;
                    }
                }              
            }
        }
        return messageBundleURLs != null && messageBundleURLs.size() > 0;
    }
    public static synchronized void addListener(LocalizablePluginInterfaceListener listener)
    {
        if (!LocalizablePluginInterface.listeners.contains(listener))
        {
            LocalizablePluginInterface.listeners.addElement(listener);
        }
    }

    public static synchronized void deleteListener(LocalizablePluginInterfaceListener listener)
    {
        LocalizablePluginInterface.listeners.removeElement(listener);
    }
    public static synchronized void deleteListeners()
    {
        if (LocalizablePluginInterface.listeners != null)
        {
            LocalizablePluginInterface.listeners.removeAllElements();
        }
    }
    public static void notifyListeners(LocalizablePluginInterfaceEvent event)
    {
        Object[] localListeners = null;
        synchronized (LocalizablePluginInterface.listeners)
        {
            localListeners = LocalizablePluginInterface.listeners.toArray();
        }

        for (int i = localListeners.length - 1; i >= 0; i--)
        {
            ((LocalizablePluginInterfaceListener) localListeners[i]).installed(event);
        }
    }

    
    public void check()
    {
        LocalizablePlugin[] localizablePlugins = LocalizablePluginManager.toArray();
        boolean showLoadingState = localizablePlugins.length == 0;
        if(showLoadingState == true)
        {
            i18nAZ.viewInstance.updateLoadingMessage("Recherche des fichiers de langues...");
            i18nAZ.viewInstance.updateProgressBar(0);
        }
  
        Map<URL, PluginInterface> jarUrls = new HashMap<URL, PluginInterface>();
        List<URL> excludeJarUrls = new ArrayList<URL>();
            
        List<PluginInterface> pluginInterfaces = new ArrayList<PluginInterface>();
        pluginInterfaces.addAll(Arrays.asList(i18nAZ.getPluginInterface().getPluginManager().getPluginInterfaces()));
        pluginInterfaces.addAll(Arrays.asList(NotInstalledPluginManager.toArray()));
        for (int i = -1; i < pluginInterfaces.size(); i++)
        {
            if (i > -1 && (pluginInterfaces.get(i) == null || (pluginInterfaces.get(i).getPluginProperties() == null) || (pluginInterfaces.get(i).getPluginID().equals("<none>"))))
            {
                continue;
            }
            
            URLClassLoader urlClassLoader = null;
            PluginInterface pluginInterface = null;
            if (i == -1)
            {
                pluginInterface = null;
                urlClassLoader = (URLClassLoader) i18nAZ.class.getClassLoader();
            }
            else
            {                
                if (pluginInterfaces.get(i).getPluginClassLoader() instanceof URLClassLoader)
                {
                    pluginInterface = pluginInterfaces.get(i);
                    urlClassLoader = (URLClassLoader) pluginInterface.getPluginClassLoader();
                }
            }

            if (urlClassLoader != null)
            {
                URL[] urls = urlClassLoader.getURLs();
                for (int j = 0; j < urls.length; j++)
                {
                    if (urls[j].getFile() == null || urls[j].getFile().endsWith("/") == false)
                    {
                        if ("file".equals(urls[j].getProtocol()) == true)
                        {
                            if (jarUrls.containsKey(urls[j]) == false && excludeJarUrls.contains(urls[j]) == false)
                            {
                                ArchiveFile archiveFile = null;
                                try
                                {
                                    archiveFile = new ArchiveFile(urls[j]);
                                }
                                catch (IOException e)
                                {
                                }
                                if(archiveFile != null)
                                {
                                    URL[] result = archiveFile.findPath("org/eclipse/swt/SWT.class");
                                    if(result.length == 0)
                                    {
                                        jarUrls.put(urls[j], pluginInterface);
                                    }
                                    else
                                    {
                                        excludeJarUrls.add(urls[j]);
                                    }                                    
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, HashSet<URL>> langFileURLs = new HashMap<String, HashSet<URL>>();
        Map<String, PluginInterface> associatedPluginInterfaces = new HashMap<String, PluginInterface>();
        int index = 0;
        for (Iterator<Entry<URL, PluginInterface>> iterator = jarUrls.entrySet().iterator(); iterator.hasNext();)
        {
            index++;
            Entry<URL, PluginInterface> entry = iterator.next();
            String pluginId = LocalizablePluginManager.PLUGIN_CORE_ID;
            String pluginInterfaceVersion = "<none>";
            if (entry.getValue() != null)
            {
                if(showLoadingState == true)
                {
                    i18nAZ.viewInstance.updateProgressBar((float)index /  (float)(jarUrls.size()-1) * (float)100);
                    i18nAZ.viewInstance.updateLoadingMessage("Recherche des fichiers de langues...\n" + entry.getValue().getPluginID());
                }
                pluginId = entry.getValue().getPluginID();
                pluginInterfaceVersion = entry.getValue().getPluginVersion();
            }

            File internalPluginDir = Path.getDirectory(LocalizablePluginManager.LOCALIZABLE_PLUGIN_DIR, pluginId);
            File internalPluginFile = new File(internalPluginDir, "plugin.properties");
            boolean search = true;
            
            if (internalPluginFile.exists() == true && internalPluginFile.isFile() == true)
            {
                Properties properties = Util.getProperties(internalPluginFile);
                if (properties != null && properties.containsKey("plugin.version") && properties.containsKey("plugin.notInstalled"))
                {
                    boolean notInstalled1 = Boolean.parseBoolean((String) properties.get("plugin.notInstalled"));
                    boolean notInstalled2 = entry.getValue() instanceof NotInstalledPluginInterfaceImpl;
                    if(notInstalled1 != notInstalled2)
                    {
                        LocalizablePluginInterface.uninstall(internalPluginDir);
                    }
                    else
                    {
                        if (pluginInterfaceVersion != "<none>")
                        {
                            int comp = 1;
                            if(pluginInterfaceVersion != null)
                            {
                                comp =  PluginUtils.comparePluginVersions(pluginInterfaceVersion, (String) properties.get("plugin.version"));
                            }
                            if (comp <= 0)
                            {
                                search = false;
                            }
                            else if (internalPluginDir.exists() == true && internalPluginDir.isDirectory() == true)
                            {
                                LocalizablePluginInterface.uninstall(internalPluginDir);
                            }
                        }
                        else
                        {
                            search = false;
                        }
                    }                    
                }
            }
            
            if (search == true)
            {
                URL[] foundedLangFiles = searchLangFiles(entry.getKey());
                if (langFileURLs.containsKey(pluginId) == false)
                {
                    langFileURLs.put(pluginId, new HashSet<URL>());
                    associatedPluginInterfaces.put(pluginId, entry.getValue());
                }
                if (foundedLangFiles.length > 0)
                {                  
                    langFileURLs.get(pluginId).addAll(Arrays.asList(foundedLangFiles));
                }
            }
        }
        if(showLoadingState == true)
        {
            i18nAZ.viewInstance.updateLoadingMessage("Chargement en cours...");        
        }
        
        for (Iterator<Entry<String, HashSet<URL>>> iterator = langFileURLs.entrySet().iterator(); iterator.hasNext();)
        {
            Entry<String, HashSet<URL>> entry = iterator.next();
            String pluginId = entry.getKey();   
            if (pluginId.equals(LocalizablePluginManager.PLUGIN_CORE_ID) == false)
            {
                continue;
            }
            if(showLoadingState == true)
            {
                i18nAZ.viewInstance.updateLoadingMessage("Chargement des fichiers de langues originaux de Vuze™");
            }
            HashSet<URL> foundedLangFiles = entry.getValue();
            PluginInterface associatedPluginInterface = associatedPluginInterfaces.get(pluginId);
            LocalizablePluginInterface.installPlugin(pluginId, foundedLangFiles, associatedPluginInterface, showLoadingState);
        }
        for (Iterator<Entry<String, HashSet<URL>>> iterator = langFileURLs.entrySet().iterator(); iterator.hasNext();)
        {
            Entry<String, HashSet<URL>> entry = iterator.next();
            String pluginId = entry.getKey();
            if (pluginId.equals(LocalizablePluginManager.PLUGIN_CORE_ID) == true)
            {
                continue;
            }
            HashSet<URL> foundedLangFiles = entry.getValue();
            PluginInterface associatedPluginInterface = associatedPluginInterfaces.get(pluginId);
            if(showLoadingState == true)
            {
                i18nAZ.viewInstance.updateLoadingMessage("Chargement des fichiers de langues originaux des plugin\n" + associatedPluginInterface.getPluginID());
            }
            LocalizablePluginInterface.installPlugin(pluginId, foundedLangFiles, associatedPluginInterface, showLoadingState);
        }        
    }


    
    public void onStart()
    {
        LocalizablePluginInterface.addListener(new LocalizablePluginInterfaceListener()
        {
            
            public void installed(LocalizablePluginInterfaceEvent e)
            {
                LocalizablePluginManager.signal();                    
            }                
        });        
    }

    
    public void onStop(StopEvent e)
    {
        LocalizablePluginInterface.deleteListeners();        
    }
    public static void signal()
    {
        LocalizablePluginInterface.instance.signal();
    }

    public static void start()
    {
        LocalizablePluginInterface.instance.start();
    }

    public static void stop()
    {
        LocalizablePluginInterface.instance.stop();
    }

    public static boolean isStarted()
    {
        return  LocalizablePluginInterface.instance.isStarted();
    }
}

class LocalizablePluginInterfaceEvent
{
    PluginInterface pluginInterface = null;

    LocalizablePluginInterfaceEvent(PluginInterface pluginInterface)
    {
        this.pluginInterface = pluginInterface;
    }
}

interface LocalizablePluginInterfaceListener
{
    void installed(LocalizablePluginInterfaceEvent e);
}
