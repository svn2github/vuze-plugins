/*
 * TargetLocaleManager.java
 *
 * Created on April 15, 2014, 12:11 PM
 */

package i18nAZ;

import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;

/**
 * TargetLocaleManager class
 * 
 * @author Repris d'injustice
 */
class TargetLocaleManager
{
    final private static List<TargetLocale> targetLocales = new ArrayList<TargetLocale>();
    final private static Vector<CountListener> countListeners = new Vector<CountListener>();
    private static boolean initialized = false;
    public static synchronized boolean isInitialized()
    {
        return TargetLocaleManager.initialized;
    }
    public static synchronized void init()
    {           
        TargetLocaleManager.add(Util.EMPTY_LOCALE);
        Object listParameter = COConfigurationManager.getParameter("i18nAZ.LocalesSelected");
        if (listParameter instanceof List<?> == false)
        {
            COConfigurationManager.removeParameter("i18nAZ.LocalesSelected");
            COConfigurationManager.setParameter("i18nAZ.LocalesSelected", new ArrayList<String>());
        }
        List<?> original_list = COConfigurationManager.getListParameter("i18nAZ.LocalesSelected", new ArrayList<String>());
        List<?> list = BDecoder.decodeStrings(BEncoder.cloneList(original_list));

        Locale[] AvailableLocales = Locale.getAvailableLocales();
        for (int i = 0; i < list.size(); i++)
        {
            String[] block = list.get(i).toString().split("\\|", 2);
            String languageTag = block[0];
            ExternalPathCollection externalPaths = null;
            if (block.length == 2)
            {
                String[] subBlock = block[1].split("\\|");
                for (int j = 0; j < subBlock.length; j++)
                {
                    String[] subsubBlock = subBlock[j].split(":", 2);
                    URL url = Path.getUrl(subsubBlock[1]);
                    if (url != null)
                    {
                        String langFileId = subsubBlock[0];
                        if (externalPaths == null)
                        {
                            externalPaths = new ExternalPathCollection();                            
                        }
                        externalPaths.put(langFileId, new ExternalPath(langFileId, url));                        
                    }
                }
            }
            for (int j = 0; j < AvailableLocales.length; j++)
            {
                if (languageTag.equals(Util.getLanguageTag(AvailableLocales[j])))
                {
                    TargetLocaleManager.add(AvailableLocales[j], externalPaths);
                    break;
                }
            }
        } 
        TargetLocaleManager.initialized = true;
    }
    
    
    static TargetLocale add(Locale locale)
    {
        return TargetLocaleManager.add(-1, locale, null, null);
    }

    static TargetLocale add(Locale locale, LocaleProperties defaultProperties)
    {
        return TargetLocaleManager.add(-1, locale, null, defaultProperties);
    }

    static TargetLocale add(Locale locale, ExternalPathCollection externalPaths)
    {
        return TargetLocaleManager.add(-1, locale, externalPaths, null);
    }

    static TargetLocale add(Locale locale, ExternalPathCollection externalPaths, LocaleProperties defaultProperties)
    {
        return TargetLocaleManager.add(-1, locale, externalPaths, defaultProperties);
    }

    static TargetLocale add(int index, Locale locale)
    {
        return TargetLocaleManager.add(index, locale, null, null);
    }

    static TargetLocale add(int index, Locale locale, LocaleProperties defaultProperties)
    {
        return TargetLocaleManager.add(index, locale, null, defaultProperties);
    }

    static TargetLocale add(int index, Locale locale, ExternalPathCollection externalPaths)
    {
        return TargetLocaleManager.add(index, locale, externalPaths, null);
    }

    static TargetLocale add(int index, Locale locale, ExternalPathCollection externalPaths, LocaleProperties defaultProperties)
    {
        TargetLocale targetLocale = null;
        synchronized(TargetLocaleManager.targetLocales)
        {
            if(index == TargetLocaleManager.targetLocales.size())
            {
                index = -1;
            }
            targetLocale = new TargetLocale(TargetLocaleManager.targetLocales, locale);
            targetLocale.externalPaths = externalPaths;
            
            if(defaultProperties != null)
            {
                targetLocale.setDefaultProperties(defaultProperties);
            }
            
            
            if (index == -1)
            {
                TargetLocaleManager.targetLocales.add(targetLocale);
            }
            else
            {
                TargetLocaleManager.targetLocales.add(index, targetLocale);
            }
        }
        
        LocalePropertiesLoader.signal();
        
        return targetLocale;
    }
    static void remove(TargetLocale targetLocale)
    {
        synchronized(TargetLocaleManager.targetLocales)
        {
            for (int i = 0; i < TargetLocaleManager.targetLocales.size(); i++)
            {
                if (TargetLocaleManager.targetLocales.get(i).getId() == targetLocale.id)
                {
                    TargetLocaleManager.targetLocales.remove(i);
                    LocalizablePluginManager.removeProperties(targetLocale);
                    break;
                }
            }
        }
    }
    static TargetLocale[] toArray()
    {
        return TargetLocaleManager.toArray(false);
    }
    static TargetLocale[] toArray(boolean clone)
    {
        TargetLocale[] _targetLocales = null;
        synchronized(TargetLocaleManager.targetLocales)
        {
            if (clone == true)        
            {
                List<TargetLocale> clonedTargetLocales = new ArrayList<TargetLocale>();
                for (int i = 0; i < TargetLocaleManager.targetLocales.size(); i++)
                {
                    clonedTargetLocales.add((TargetLocale) TargetLocaleManager.targetLocales.get(i).clone(clonedTargetLocales));
                }
                _targetLocales = clonedTargetLocales.toArray(new TargetLocale[clonedTargetLocales.size()]);
            }
            else
            {
                _targetLocales = TargetLocaleManager.targetLocales.toArray(new TargetLocale[TargetLocaleManager.targetLocales.size()]);
            }            
        }
        return _targetLocales;
    }

    public static class ExternalPathCollection extends HashMap<String, ExternalPath>
    {
        private static final long serialVersionUID = 1L;

        ExternalPathCollection()
        {
        }

        @Override
        public ExternalPath put(String langFileId, ExternalPath externalPath)
        {
            return super.put(langFileId, externalPath);
        }
    }

    public static class ExternalPath
    {
        private String langFileId = null;
        private URL url = null;

        ExternalPath(String langFileId, URL url)
        {
            this.langFileId = langFileId;
            this.url = url;
        }

        public String getLangFileId()
        {
            return this.langFileId;
        }

        public URL getUrl()
        {
            return this.url;
        }
    }

    static public class TargetLocale
    {
        private ExternalPathCollection externalPaths = null;
        private double id = 0;
        private List<TargetLocale> parent = null;
        private Locale locale = null;
        private boolean reference = false;

        TargetLocale(List<TargetLocale> parent, Locale locale)
        {
            this.id = Math.random();
            this.parent = parent;
            this.locale = locale;
            this.reference = locale.equals(Util.EMPTY_LOCALE) == true;
        }

        @Override
        public Object clone()
        {
            List<TargetLocale> clonedTargetLocales = new ArrayList<TargetLocale>();
            int index = 0;
            for (int i = 0; i < this.parent.size(); i++)
            {
                clonedTargetLocales.add((TargetLocale) this.parent.get(i).clone(clonedTargetLocales));
                if (this.parent.get(i).getId() == this.id)
                {
                    index = i;
                }
            }
            return clonedTargetLocales.get(index);
        }

        public Object clone(List<TargetLocale> parent)
        {
            TargetLocale targetLocale = new TargetLocale(parent, (Locale) this.locale.clone());
            targetLocale.id = this.id;
            if (this.getExternalPaths() != null)
            {
                targetLocale.externalPaths = (ExternalPathCollection) this.getExternalPaths().clone();
            }
            return targetLocale;
        }

        Locale getLocale()
        {
            return this.locale;
        }

        boolean isReference()
        {
            return this.reference;
        }

        boolean isReadOnly()
        {
            return this.externalPaths != null;
        }

        boolean isVisible()
        {
            return this.isVisible(LocalizablePluginManager.getCurrentLangFile());
        }

        boolean isVisible(LangFileObject langFileObject)
        {
            if (this.isReadOnly() == false)
            {
                return true;
            }
            if (langFileObject == null)
            {
                return false;
            }            
            return this.externalPaths.containsKey(langFileObject.getId());
        }
        URL getExternalPath()
        {
            return this.getExternalPath(LocalizablePluginManager.getCurrentLangFile());
        }

        URL getExternalPath(LangFileObject langFileObject)
        {
            if (this.isVisible(langFileObject) == false)
            {
                return null;
            }
            return this.externalPaths.get(langFileObject.getId()).getUrl();
        }

        TargetLocale[] getParent()
        {
            synchronized(this.parent)
            {
                return this.parent.toArray(new TargetLocale[this.parent.size()]);                
            }
        }

        LocaleProperties getProperties()
        {
            return this.getProperties(LocalizablePluginManager.getCurrentLangFile());
        }

        LocaleProperties getProperties(LangFileObject langFileObject)
        {
            return langFileObject.getProperties(this);
        }

        URL getInternalPath()
        {
            return this.getInternalPath(LocalizablePluginManager.getCurrentLangFile());
        }

        URL getInternalPath(LangFileObject langFileObject)
        {
            return langFileObject.getInternalPath(this);
        }

        double getId()
        {
            return this.id;
        }

        void setDefaultProperties(LocaleProperties properties)
        {
            this.setDefaultProperties(LocalizablePluginManager.getCurrentLangFile(), properties);
        }
        void setDefaultProperties(LangFileObject langFileObject, LocaleProperties properties)
        {
            langFileObject.setDefaultProperties(this, properties);
        }
        public ExternalPathCollection getExternalPaths()
        {
            return this.externalPaths;
        }
    }



    static synchronized void addCountListener(CountListener countListener)
    {
        if (!TargetLocaleManager.countListeners.contains(countListener))
        {
            TargetLocaleManager.countListeners.addElement(countListener);
        }
    }

    static synchronized void deleteCountListener(CountListener countListener)
    {
        TargetLocaleManager.countListeners.removeElement(countListener);
    }

    static void notifyCountListeners(CountEvent countEvent)
    {
        Object[] localCountListeners = null;

        synchronized (TargetLocaleManager.countListeners)
        {
            localCountListeners = TargetLocaleManager.countListeners.toArray();
        }

        for (int i = localCountListeners.length - 1; i >= 0; i--)
        {
            ((CountListener) localCountListeners[i]).countChanged(countEvent);
        }
    }

    static synchronized void deleteCountListeners()
    {
        if (TargetLocaleManager.countListeners != null)
        {
            TargetLocaleManager.countListeners.removeAllElements();
        }
    }
}
