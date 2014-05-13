/*
 * LocalizablePluginManager.java
 *
 * Created on March 21, 2014, 4:27 PM
 */

package i18nAZ;

import i18nAZ.FilterManager.Filter;
import i18nAZ.LocalizablePluginManager.LocalizablePlugin;
import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;
import i18nAZ.TargetLocaleManager.TargetLocale;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

/**
 * LocalizablePluginManager class
 * 
 * @author Repris d'injustice
 */
public class LocalizablePluginManager implements iTask
{
    final private static Task instance = new Task("LocalizablePluginManager", 60, (iTask) new LocalizablePluginManager());
    
    public static class LocalizablePlugin implements Cloneable
    {
        public static class LangFileObject implements Cloneable
        {
            private LocalizablePlugin parent = null;
            private String langFile = "";
            private String jarFolder = "";
            private String name = "";
            final private Map<Double, LocaleProperties> localeProperties = new HashMap<Double, LocaleProperties>();
            File[] existingFiles = new File[0];
            private CountObject counts = null;            
            
            
            protected Object clone()
            {
                synchronized(this.parent.langFileObjects)
                {
                    LocalizablePlugin localizablePlugin = (LocalizablePlugin) this.parent.clone();
                    for (int i = 0; i < localizablePlugin.langFileObjects.size(); i++)
                    {
                        if (localizablePlugin.langFileObjects.get(i).langFile.equals(this.langFile) == true)
                        {
                            return localizablePlugin.langFileObjects.get(i);                            
                        }                        
                    }                    
                }               
                
                return null;

            }

            protected Object clone(LocalizablePlugin parent)
            {
                LangFileObject langFileObject = new LangFileObject();
                langFileObject.parent = parent;
                langFileObject.langFile = this.langFile;
                langFileObject.jarFolder = this.langFile;
                langFileObject.name = this.langFile;
                synchronized(this.localeProperties)
                {
                    for (Iterator<Entry<Double, LocaleProperties>> iterator = this.localeProperties.entrySet().iterator(); iterator.hasNext();)
                    {
                        Entry<Double, LocaleProperties> entry = iterator.next();
                        langFileObject.localeProperties.put(entry.getKey(), entry.getValue());
                    }
                }
                langFileObject.existingFiles = this.existingFiles.clone();
                return langFileObject;

            }

            public CountObject getCounts()
            {
                return this.counts;
            }

            public CountObject getCounts(TargetLocale targetLocale)
            {
                LocaleProperties properties = null;
                synchronized(this.localeProperties)
                {
                    if (this.localeProperties.containsKey(targetLocale.getId()) == true)
                    {
                        properties = this.localeProperties.get(targetLocale.getId());                        
                    } 
                }
                
                if (properties == null)
                {
                    properties = new LocaleProperties(targetLocale.getLocale());
                }
                return properties.counts;
            }

            public String getId()
            {
                return this.parent.getId() + "!" + this.langFile;
            }

            public String getFileName(TargetLocale targetLocale)
            {
                return Util.getBundleFileName(this.name, targetLocale.getLocale());
            }

            public URL getInternalPath(TargetLocale targetLocale)
            {
                return Path.getUrl(LocalizablePluginManager.LOCALIZABLE_PLUGIN_DIR + this.getParent().getName() + "\\" + this.jarFolder.replace('/', '.') + "\\" + this.getFileName(targetLocale));
            }

            public String getJarFolder()
            {
                return this.jarFolder;
            }
            public String getLangFile()
            {
                return this.langFile;
            }

            public LocalizablePlugin getParent()
            {
                return this.parent;
            }

            public LocaleProperties getProperties(TargetLocale targetLocale)
            {
                LocaleProperties properties = null;
                if (this.isLoaded(targetLocale) == false)
                {
                    this.load(targetLocale);
                }
                synchronized(this.localeProperties)
                {
                    if (this.localeProperties.containsKey(targetLocale.getId()) == true)
                    {
                        properties = this.localeProperties.get(targetLocale.getId());
                    }
                }                
                if (properties == null)
                {
                    properties = new LocaleProperties(targetLocale.getLocale());
                }
                return properties;
            }
            public boolean isLoaded(TargetLocale targetLocale)
            {
                synchronized(this.localeProperties)
                {
                    return this.localeProperties.containsKey(targetLocale.getId());
                }                
            }
            public void load(TargetLocale targetLocale)
            {
                synchronized(this.localeProperties)
                {
                    if (this.localeProperties.containsKey(targetLocale.getId()) == true)
                    {
                        return;
                    }
                    LocaleProperties properties = null;
                    if (targetLocale.isReadOnly() == true && targetLocale.isVisible(this) == false)
                    {
                        properties = new LocaleProperties(targetLocale.getLocale());
                    }
                    else if (targetLocale.isReadOnly() == true && targetLocale.isVisible(this) == true)
                    {
                        properties = Util.getLocaleProperties(targetLocale.getLocale(), targetLocale.getExternalPath(this));
                    }
                    else
                    {
                        properties = Util.loadLocaleProperties(targetLocale.getLocale(), this.getInternalPath(targetLocale));
                        if (properties == null)
                        {
                            URL OriginalBundleURL = Path.getUrl(LocalizablePluginManager.LOCALIZABLE_PLUGIN_DIR + this.getParent().getName() + "\\" + this.jarFolder.replace('/', '.') + "\\" + this.getFileName(targetLocale) + ".original");
                            properties = Util.getLocaleProperties((targetLocale == null) ? Util.EMPTY_LOCALE : targetLocale.getLocale(), OriginalBundleURL);
                            if (properties != null && properties.IsLoaded() == true && targetLocale.isReference() == true)
                            {
                                //no save
                            }
                        };
                    }
                    if (properties == null)
                    {
                        properties = new LocaleProperties(targetLocale.getLocale());
                    }
                    this.localeProperties.put(targetLocale.getId(), properties);
                }                
                this.refreshCount(targetLocale);
            }

            public void setDefaultProperties(TargetLocale targetLocale, LocaleProperties defaultProperties)
            {
                synchronized(this.localeProperties)
                {
                    if (this.localeProperties.containsKey(targetLocale.getId()) == true)
                    {
                        if (defaultProperties == null)
                        {
                            defaultProperties = new LocaleProperties(targetLocale.getLocale());
                        }
                        this.localeProperties.put(targetLocale.getId(), defaultProperties);
                        this.refreshCount(targetLocale);
                    }
                }
            }

            public void refreshCount(TargetLocale targetLocale)
            {
                this.counts = FilterManager.getCounts(this, new Filter(), null, TargetLocaleManager.toArray());
                LocaleProperties _localeProperties = null;
                synchronized(this.localeProperties)
                {
                    if (this.localeProperties.containsKey(targetLocale.getId()) == true && targetLocale.isReference() == false)
                    {
                        _localeProperties = this.localeProperties.get(targetLocale.getId());
                    }
                }
                if (_localeProperties != null)
                {
                    _localeProperties.counts = FilterManager.getCounts(this, new Filter(), null, targetLocale);
                    CountEvent countEvent = new CountEvent(this, targetLocale);
                    countEvent.counts.add( _localeProperties.counts);
                    TargetLocaleManager.notifyCountListeners(countEvent);
                }                
            }

            private void removeProperties(TargetLocale targetLocale)
            {
                synchronized(this.localeProperties)
                {
                    if (this.localeProperties.containsKey(targetLocale.getId()) == true)
                    {
                        this.counts = FilterManager.getCounts(this, new Filter(), null, TargetLocaleManager.toArray());
                        this.localeProperties.remove(targetLocale.getId());
                    }
                }                
            }
        }

        private String id = null;
        private String name = null;
        private String displayName = null;
        private boolean notInstalled = false;
        final private List<LangFileObject> langFileObjects = new ArrayList<LangFileObject>();

        private LocalizablePlugin()
        {
        }

        private LocalizablePlugin(String pluginId, String pluginName, boolean notInstalled)
        {
            this.notInstalled = notInstalled;
            this.id = pluginId;
            this.displayName = LocalizablePluginManager.PLUGIN_CORE_DISPLAY_NAME;
            this.name = LocalizablePluginManager.PLUGIN_CORE_ID;
            if (this.id.equalsIgnoreCase(LocalizablePluginManager.PLUGIN_CORE_ID) == false)
            {
                String localizedKey = "Views.plugins." + pluginId + ".title";
                if (i18nAZ.getPluginInterface().getUtilities().getLocaleUtilities().hasLocalisedMessageText(localizedKey) == true)
                {
                    this.displayName = i18nAZ.getLocalisedMessageText(localizedKey);
                }
                else
                {
                    this.displayName = pluginName;
                }
                if (this.displayName == null)
                {
                    this.displayName = "Unknown plugin";
                }
                this.name = pluginId;
            }
        }

        
        protected Object clone()
        {
            LocalizablePlugin localizablePlugin = new LocalizablePlugin();
            localizablePlugin.id = this.id;
            localizablePlugin.name = this.name;
            localizablePlugin.displayName = this.displayName;
            synchronized(this.langFileObjects)
            {
                 for (int i = 0; i < localizablePlugin.langFileObjects.size(); i++)
                 {
                     localizablePlugin.langFileObjects.add((LangFileObject) this.langFileObjects.get(i).clone(localizablePlugin));                     
                 }
            }
           
            return localizablePlugin;
        }

        CountObject getCounts()
        {
            CountObject counts = new CountObject();
            LangFileObject[] langFileObjects = this.toArray();
            for (int i = 0; i < langFileObjects.length; i++)
            {
                counts.add(langFileObjects[i].getCounts());
            }
            return counts;
        }

        CountObject getCounts(TargetLocale targetLocale)
        {
            CountObject counts = new CountObject();
            LangFileObject[] langFileObjects = this.toArray();
            for (int i = 0; i < langFileObjects.length; i++)
            {
                counts.add(langFileObjects[i].getCounts(targetLocale));
            }
            return counts;
        }

        String getDisplayName()
        {
            return this.displayName;
        }

        String getId()
        {
            return this.id;
        }

        LangFileObject[] toArray()
        {
            synchronized(this.langFileObjects)
            {
                return this.langFileObjects.toArray(new LangFileObject[this.langFileObjects.size()]);
            }
        }

        String getName()
        {
            return this.name;
        }

        boolean isNotInstalled()
        {
            return this.notInstalled;
        }

        void addLangFiles(File langFileDir)
        {
            LangFileObject langFileObject = new LangFileObject();
            langFileObject.parent = this;
            langFileObject.jarFolder = langFileDir.getName();
            File[] files = langFileDir.listFiles();
            List<File> existingFiles = new ArrayList<File>();
            for (int i = 0; files != null && i < files.length; i++)
            {
                if (files[i].isDirectory() == true)
                {
                    continue;
                }
                String localeFileName = Path.getFilenameWithoutExtension(files[i]);
                String extension = Path.getExtension(localeFileName);
                if (extension.equalsIgnoreCase(LocalizablePluginManager.EXTENSION) == false)
                {
                    continue;
                }
                localeFileName = Path.getFilenameWithoutExtension(localeFileName);

                Locale locale = Util.getLocaleFromFilename(localeFileName);
                if (locale == null && langFileObject.name.equals("") == true)
                {
                    langFileObject.name = localeFileName;
                    continue;
                }
                existingFiles.add(files[i]);
            }

            langFileObject.existingFiles = existingFiles.toArray(new File[existingFiles.size()]);

            langFileObject.langFile = langFileObject.jarFolder + "." + langFileObject.name;
            synchronized(this.langFileObjects)
            {
                this.langFileObjects.add(langFileObject);
            }
        }
    }

    final static public int ACTION_ADDED = 1;
    final static public int ACTION_REMOVED = 2;
    final static String DEFAULT_NAME = "MessagesBundle";
    final static String EXTENSION = ".properties";
    final static String PLUGIN_CORE_ID = "Azureus2";
    static String LOCALIZABLE_PLUGIN_DIR = "";
    static String PLUGIN_CORE_DISPLAY_NAME = "(core)";
    static String fileSelectedId = COConfigurationManager.getStringParameter("i18nAZ.FileSelectedId", null);

    final private static Vector<LocalizablePluginListener> listeners = new Vector<LocalizablePluginListener>();

    final private static Map<String, LocalizablePlugin> localizablePlugins = new HashMap<String, LocalizablePlugin>();
    final private static Map<String, LocalizablePlugin> notLocalizablePlugins = new HashMap<String, LocalizablePlugin>();

    final private static Object currentLangFileObjectMutex = new Object();
    private static LangFileObject currentLangFileObject = null;
    private static LangFileObject oldLangFileObject = null;
   
    public static boolean contains(LocalizablePlugin localizablePlugin)
    {
        return LocalizablePluginManager.contains(localizablePlugin.getId());
    }

    public static boolean contains(String pluginID)
    {
        synchronized (LocalizablePluginManager.localizablePlugins)
        {
            return LocalizablePluginManager.localizablePlugins.containsKey(pluginID);
        }
    }

    private static LocalizablePlugin add(LocalizablePlugin localizablePlugin)
    {
        boolean notify = false;
        LocalizablePlugin _localizablePlugin = null;
        synchronized (LocalizablePluginManager.localizablePlugins)
        {
            if (LocalizablePluginManager.localizablePlugins.containsKey(localizablePlugin.getId()) == false)
            {
                _localizablePlugin = LocalizablePluginManager.localizablePlugins.put(localizablePlugin.getId(), localizablePlugin);
                notify = LocalizablePluginManager.localizablePlugins.containsKey(localizablePlugin.getId()) == true;
            }
            if (notify == true)
            {
                synchronized (LocalizablePluginManager.currentLangFileObjectMutex)
                {
                    LocalizablePlugin[] _localizablePlugins = LocalizablePluginManager.localizablePlugins.values().toArray(new LocalizablePlugin[LocalizablePluginManager.localizablePlugins.size()]);
                    Arrays.sort(_localizablePlugins, new Comparator<LocalizablePlugin>()
                    {
                        
                        public int compare(LocalizablePlugin localizablePlugin1, LocalizablePlugin localizablePlugin2)
                        {
                            String s1 = localizablePlugin1.getDisplayName();
                            String s2 = localizablePlugin2.getDisplayName();
                            return s1.toString().compareToIgnoreCase(s2);
                        }
                    });
                }
            }
        }
        if (notify == true)
        {
            LocalePropertiesLoader.signal();
            LocalizablePluginManager.notifyListeners(new LocalizablePluginEvent(localizablePlugin, LocalizablePluginManager.ACTION_ADDED));
        }
        return _localizablePlugin;
    }

    private static LocalizablePlugin remove(LocalizablePlugin localizablePlugin)
    {
        LocalizablePlugin _localizablePlugin = null;
        boolean notify = LocalizablePluginManager.localizablePlugins.containsKey(localizablePlugin.getId());
        _localizablePlugin = LocalizablePluginManager.localizablePlugins.remove(localizablePlugin.getId());
        notify = notify == true && LocalizablePluginManager.localizablePlugins.containsKey(localizablePlugin.getId()) == false;
        if (notify == true)
        {
            synchronized (LocalizablePluginManager.currentLangFileObjectMutex)
            {
                LocalizablePlugin[] _localizablePlugins = LocalizablePluginManager.localizablePlugins.values().toArray(new LocalizablePlugin[LocalizablePluginManager.localizablePlugins.size()]);
                Arrays.sort(_localizablePlugins, new Comparator<LocalizablePlugin>()
                {
                    
                    public int compare(LocalizablePlugin localizablePlugin1, LocalizablePlugin localizablePlugin2)
                    {
                        String s1 = localizablePlugin1.getDisplayName();
                        String s2 = localizablePlugin2.getDisplayName();
                        return s1.toString().compareToIgnoreCase(s2);
                    }
                });
            }
            LocalizablePluginManager.notifyListeners(new LocalizablePluginEvent(localizablePlugin, LocalizablePluginManager.ACTION_REMOVED));
        }
        return _localizablePlugin;
    }

    public static void removeProperties(TargetLocale TargetLocale)
    {
        synchronized (LocalizablePluginManager.localizablePlugins)
        {
            for (Iterator<LocalizablePlugin> iterator = localizablePlugins.values().iterator(); iterator.hasNext();)
            {
                LocalizablePlugin localizablePlugin = iterator.next();
                LangFileObject[] langFileObjects = localizablePlugin.toArray();
                for (int j = 0; j < langFileObjects.length; j++)
                {
                    langFileObjects[j].removeProperties(TargetLocale);
                }
            }
        }
    }

    public static LocalizablePlugin[] toArray()
    {
        LocalizablePlugin[] _localizablePlugins = null;
        synchronized (LocalizablePluginManager.localizablePlugins)
        {
            _localizablePlugins = LocalizablePluginManager.localizablePlugins.values().toArray(new LocalizablePlugin[LocalizablePluginManager.localizablePlugins.size()]);
        }
        Arrays.sort(_localizablePlugins, new Comparator<LocalizablePlugin>()
        {
            
            public int compare(LocalizablePlugin localizablePlugin1, LocalizablePlugin localizablePlugin2)
            {
                String s1 = localizablePlugin1.getDisplayName();
                String s2 = localizablePlugin2.getDisplayName();
                return s1.toString().compareToIgnoreCase(s2);
            }
        });
        return _localizablePlugins;
    }

    public static LocalizablePlugin[] getNotLocalizables()
    {
        LocalizablePlugin[] _notLocalizablePlugins = null;
        synchronized (notLocalizablePlugins)
        {
            _notLocalizablePlugins = notLocalizablePlugins.values().toArray(new LocalizablePlugin[notLocalizablePlugins.size()]);
            for (int i = 0; i < _notLocalizablePlugins.length; i++)
            {
                synchronized (LocalizablePluginManager.localizablePlugins)
                {
                    if (LocalizablePluginManager.localizablePlugins.containsKey(_notLocalizablePlugins[i].getId()) == true)
                    {
                        LocalizablePluginManager.notLocalizablePlugins.remove(_notLocalizablePlugins[i].getId());
                    }
                }
            }
            _notLocalizablePlugins = notLocalizablePlugins.values().toArray(new LocalizablePlugin[notLocalizablePlugins.size()]);
        }
        Arrays.sort(_notLocalizablePlugins, new Comparator<LocalizablePlugin>()
        {
            
            public int compare(LocalizablePlugin localizablePlugin1, LocalizablePlugin localizablePlugin2)
            {
                String s1 = localizablePlugin1.getDisplayName();
                String s2 = localizablePlugin2.getDisplayName();
                return s1.toString().compareToIgnoreCase(s2);
            }
        });
        return _notLocalizablePlugins;
    }

    public static boolean setCurrentLangFile(LangFileObject currentLangFileObject)
    {
        synchronized (LocalizablePluginManager.currentLangFileObjectMutex)
        {
            LocalizablePluginManager.currentLangFileObject = currentLangFileObject;
            if ((LocalizablePluginManager.currentLangFileObject == null && LocalizablePluginManager.oldLangFileObject != null) || (LocalizablePluginManager.currentLangFileObject != null && LocalizablePluginManager.oldLangFileObject == null) || (LocalizablePluginManager.currentLangFileObject != null && LocalizablePluginManager.oldLangFileObject != null && LocalizablePluginManager.currentLangFileObject.getId().equals(LocalizablePluginManager.oldLangFileObject.getId()) == false))
            {
                LocalizablePluginManager.oldLangFileObject = LocalizablePluginManager.currentLangFileObject;
                FilterManager.getCurrentFilter().emptyExcludedKey.clear();
                FilterManager.getCurrentFilter().unchangedExcludedKey.clear();
                FilterManager.getCurrentFilter().extraExcludedKey.clear();
                FilterManager.getCurrentFilter().hideRedirectKeysExcludedKey.clear();
                FilterManager.getCurrentFilter().urlsOverriddenStates.clear();

                if (LocalizablePluginManager.currentLangFileObject != null)
                {
                    COConfigurationManager.setParameter("i18nAZ.FileSelectedId", LocalizablePluginManager.currentLangFileObject.getId());
                }
                else
                {
                    COConfigurationManager.removeParameter("i18nAZ.FileSelectedId");
                }
                COConfigurationManager.save();

                return true;
            }
            return false;
        }
    }

    public static LangFileObject getCurrentLangFile()
    {
        synchronized (LocalizablePluginManager.currentLangFileObjectMutex)
        {
            return LocalizablePluginManager.currentLangFileObject;
        }
    }

    public static synchronized void addListener(LocalizablePluginListener listener)
    {
        if (!LocalizablePluginManager.listeners.contains(listener))
        {
            LocalizablePluginManager.listeners.addElement(listener);
        }
    }

    private static boolean install(File localizablePluginDir)
    {
        String pluginId = localizablePluginDir.getName();
        File internalPluginFile = new File(localizablePluginDir, "plugin.properties");
        if (internalPluginFile.exists() == false || internalPluginFile.isFile() == false)
        {
            return false;
        }
        Properties properties = Util.getProperties(internalPluginFile);
        if (properties.containsKey("plugin.id") == false || properties.containsKey("plugin.name") == false || properties.containsKey("plugin.version") == false || properties.containsKey("plugin.state") == false || properties.containsKey("plugin.notInstalled") == false)
        {
            return false;
        }
        if (((String) properties.get("plugin.id")).equalsIgnoreCase(pluginId) == false)
        {
            return false;
        }
        boolean notInstalled = Boolean.parseBoolean((String) properties.get("plugin.notInstalled"));
        String pluginName = (String) properties.get("plugin.name");
        if (((String) properties.get("plugin.state")).equalsIgnoreCase("localizable") == false)
        {
            LocalizablePlugin localizablePlugin = new LocalizablePlugin(pluginId, pluginName, notInstalled);
            synchronized (LocalizablePluginManager.notLocalizablePlugins)
            {
                if (LocalizablePluginManager.notLocalizablePlugins.containsKey(localizablePlugin.getId()) == false)
                {
                    LocalizablePluginManager.notLocalizablePlugins.put(localizablePlugin.getId(), localizablePlugin);
                }
            }
            return false;
        }
        LocalizablePlugin localizablePlugin = new LocalizablePlugin(pluginId, pluginName, notInstalled);

        File[] langFileDirs = localizablePluginDir.listFiles();
        for (int j = 0; langFileDirs != null && j < langFileDirs.length; j++)
        {
            if (langFileDirs[j].isDirectory() == false)
            {
                continue;
            }
            localizablePlugin.addLangFiles(langFileDirs[j]);
        }
        LocalizablePluginManager.add(localizablePlugin);
        return true;
    }

    public static synchronized void deleteListener(LocalizablePluginListener listener)
    {
        LocalizablePluginManager.listeners.removeElement(listener);
    }

    public static synchronized void deleteListeners()
    {
        if (LocalizablePluginManager.listeners != null)
        {
            LocalizablePluginManager.listeners.removeAllElements();
        }
    }

    public static void notifyListeners(LocalizablePluginEvent event)
    {
        Object[] localListeners = null;
        synchronized (LocalizablePluginManager.listeners)
        {
            localListeners = LocalizablePluginManager.listeners.toArray();
        }

        for (int i = localListeners.length - 1; i >= 0; i--)
        {
            ((LocalizablePluginListener) localListeners[i]).changed(event);
        }
    } 

    
    public void check()
    {
        while (PluginCoreUtils.isInitialisationComplete() == false)
        {
            Util.sleep(1000);      
        } 
        File[] localizablePluginDirs = new File(LocalizablePluginManager.LOCALIZABLE_PLUGIN_DIR).listFiles();
        HashSet<String> pluginIds = new HashSet<String>();
        for (int i = 0; localizablePluginDirs != null && i < localizablePluginDirs.length; i++)
        {
            if (localizablePluginDirs[i].isDirectory() == false)
            {
                continue;
            }
            String pluginId = localizablePluginDirs[i].getName();
            if (pluginId.equals(LocalizablePluginManager.PLUGIN_CORE_ID) == false)
            {
                continue;
            }
            if (LocalizablePluginManager.install(localizablePluginDirs[i]) == true)
            {
                pluginIds.add(pluginId);
            }
        }
        for (int i = 0; localizablePluginDirs != null && i < localizablePluginDirs.length; i++)
        {
            if (localizablePluginDirs[i].isDirectory() == false)
            {
                continue;
            }
            String pluginId = localizablePluginDirs[i].getName();
            if (pluginId.equals(LocalizablePluginManager.PLUGIN_CORE_ID) == true)
            {
                continue;
            }
            if (LocalizablePluginManager.install(localizablePluginDirs[i]) == true)
            {
                pluginIds.add(pluginId);
            }
        }
        synchronized (LocalizablePluginManager.localizablePlugins)
        {
            LocalizablePlugin[] _localizablePlugins = LocalizablePluginManager.toArray();
            for (int i = 0; i < _localizablePlugins.length; i++)
            {
                if (pluginIds.contains(_localizablePlugins[i].getId()) == false)
                {
                    LocalizablePluginManager.remove(_localizablePlugins[i]);
                }
            }
        }
        if(LocalizablePluginInterface.isStarted() == false)
        {
            LocalizablePluginInterface.start();
        }
        if(NotInstalledPluginDownloader.isStarted() == false && COConfigurationManager.getBooleanParameter("i18nAZ.AllowRemotePlugin") == true)
        {
            NotInstalledPluginDownloader.start();
        }        
    }

    
    public void onStart()
    {        
    }

    
    public void onStop(StopEvent e)
    {
        NotInstalledPluginDownloader.stop();       
        LocalizablePluginInterface.stop();          
    }
    public static void signal()
    {
        LocalizablePluginManager.instance.signal();
    }

    public static void start()
    {
        LocalizablePluginManager.instance.start();
    }

    public static void stop()
    {
        LocalizablePluginManager.instance.stop();
    }

    public static boolean isStarted()
    {
        return  LocalizablePluginManager.instance.isStarted();
    }
}

class LocalizablePluginEvent
{
    LocalizablePlugin localizablePlugin = null;
    int action = 0;

    LocalizablePluginEvent(LocalizablePlugin localizablePlugin, int action)
    {
        this.localizablePlugin = localizablePlugin;
        this.action = action;
    }
}

interface LocalizablePluginListener
{
    void changed(LocalizablePluginEvent e);
}
