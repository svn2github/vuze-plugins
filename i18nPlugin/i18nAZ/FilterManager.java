/*
 * FilterManager.java
 *
 * Created on April 15, 2014, 4:09 AM
 */
package i18nAZ;

import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;
import i18nAZ.SpellChecker.SpellObjectManager;
import i18nAZ.TargetLocaleManager.TargetLocale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * FilterManager class
 * 
 * @author Repris d'injustice
 */
public class FilterManager
{
    private static Filter currentFilter = null;

    public static class PrebuildItem
    {
        private String key = null;
        private String[] commentsLines = null;
        private String[] values = null;
        private int[] states = null;
        private boolean exist = false;
        private PrebuildItemCollection childs = null;
        private PrebuildItemCollection parent = null;
        private LangFileObject langFileObject = null;
        private SpellObjectManager[] spellObjectManagers = null;
        final private Object mutex = new Object();
            
        public String getKey()
        {
            return this.key;
        }
        public String[] getCommentsLines()
        {
            return this.commentsLines;
        }
        public String[] getValues()
        {
            synchronized(this.mutex)
            {
                return this.values;                
            }
        }
        public int[] getStates()
        {
            synchronized(this.mutex)
            {
                return this.states;
            }
        }
        public boolean isExist()
        {
            return this.exist;
        }
        public PrebuildItemCollection getChilds()
        {
            return this.childs;
        }
        public PrebuildItemCollection getParent()
        {
            return this.parent;
        }
        public LangFileObject getLangFileObject()
        {
            return this.langFileObject;
        }        
        public int getColumnCount()
        {
            return this.parent.columnCount;
        }
        public void setValue(int column, String value, int state)
        {
            synchronized(this.mutex)
            {
                this.values[column] = value;
                this.states[column] = state;
            }            
        }
        public void resetObjectManagers()
        {
            synchronized(this.mutex)
            {            
                for(int i = 0; i < this.spellObjectManagers.length; i++)
                {
                    this.spellObjectManagers[i].reset();
                }
            }            
        }
        public SpellObjectManager getSpellObjectManager(int column)
        {
            synchronized(this.mutex)
            {            
                return this.spellObjectManagers[column];
            }            
        }
        private PrebuildItem()
        {

        }
        public static boolean isShowable(PrebuildItem prebuildItem)
        {
            for(int i = 0; prebuildItem.childs != null && i < prebuildItem.childs.size(); i++)
            {
                if(PrebuildItem.isShowable(prebuildItem.childs.get(i)) == true)
                {
                    return true;
                }
            }
            return FilterManager.getPrebuildItem(prebuildItem.key, prebuildItem.langFileObject) != null;
        }
    }

    public static class PrebuildItemCollection extends ArrayList<PrebuildItem>
    {
        private static final long serialVersionUID = 452271052052937619L;
        private int columnCount = 0;

        PrebuildItemCollection(int columnCount)
        {
            this.columnCount = columnCount;
        }

        
        public boolean add(PrebuildItem prebuildItem)
        {
            prebuildItem.parent = this;
            return super.add(prebuildItem);
        }
        public PrebuildItem get(String key)
        {
            for (int k = 0; k < this.size(); k++)
            {
                if(this.get(k).key.equals(key) == true)
                {
                    return this.get(k);
                }
            }
            return null;
        }
        public void set(PrebuildItem prebuildItem)
        {           
            for (int j = 0; j < this.size(); j++)
            {
                if(this.get(j).key.equals(prebuildItem.key) == true)
                {
                    this.set(j, prebuildItem);
                    return;
                }
            }
            this.add(prebuildItem);             
        }
        int getColumnCount()
        {
            return this.columnCount;
        }
    }    
    static public class State
    {
        static final int NONE = 0;
        static final int EMPTY = 1;
        static final int UNCHANGED = 2;
        static final int EXTRA = 4;
        static final int URL = 8;
        static final int REDIRECT_KEY = 16;
    }
    static class Filter
    {
        private Map<Pattern, Object> searchPatterns = new HashMap<Pattern, Object>();
        private HashSet<String> searchPrefixes = new HashSet<String>();
        private boolean regexSearch = false;
    
        boolean empty = false;
        boolean unchanged = false;
        boolean extra = false;
    
        boolean redirectKeys = false;
        int urls = 0;
    
        HashSet<String> emptyExcludedKey = new HashSet<String>();
        HashSet<String> unchangedExcludedKey = new HashSet<String>();
        HashSet<String> extraExcludedKey = new HashSet<String>();
    
        HashSet<String> hideRedirectKeysExcludedKey = new HashSet<String>();
        Map<String, Integer> urlsOverriddenStates = new HashMap<String, Integer>();
        private String text = "";
    
        Filter()
        {
    
        }
        boolean clearText()
        {
            if(this.text.equals("") == true)
            {
                return false;
            }
            this.text = "";            
            this.searchPatterns.clear();
            this.searchPrefixes.clear();
            return true;
        }
        boolean find(int columnIndex, String value)
        {
            return find(columnIndex, null, value);
        }        
        boolean find(int columnIndex, Locale locale, String value)
        {
            if (this.searchPrefixes.size() > 0)
            {
                String prefix = "";
                switch (columnIndex)
                {
                    case -1:
                        prefix = "c";
                        break;
    
                    case 0:
                        prefix = "k";
                        break;
    
                    case 1:
                        prefix = "r";
                        break;
    
                    default:
                        prefix = Util.getLanguageTag(locale).replaceAll("-", "");
                        break;
    
                }
                if (this.searchPrefixes.contains(prefix) == false)
                {
                    return false;
                }
    
            }
            for (Iterator<Entry<Pattern, Object>> iterator = this.searchPatterns.entrySet().iterator(); iterator.hasNext();)
            {
                Entry<Pattern, Object> entry = iterator.next();
                Pattern searchPattern = entry.getKey();
                boolean searchResult = (Boolean) entry.getValue();
                if (searchPattern.matcher(value).find() == searchResult)
                {
                    return true;
                }
            }
            return false;
        }
        Iterator<Entry<Pattern, Object>> getPatterns()
        {
            return this.searchPatterns.entrySet().iterator();
        }
        boolean isRegexEnabled()
        {
            return regexSearch;
        }
        boolean isTextEnabled()
        {
            return this.searchPatterns.size() > 0;
        }
        boolean isTextEnabled(String languageTag)
        {
            return this.isTextEnabled() == true && (this.searchPrefixes.size() == 0 || this.searchPrefixes.contains(languageTag) == true);       
        }
        boolean setText(String text)
        {
            text = (text == null) ? "" : text;
            if(this.text.equals(text) == true)
            {
                return false;
            }
            this.text = text;
            this.searchPatterns.clear();
            this.searchPrefixes.clear();
    
            String prefix = "";
            while (text.indexOf(':') != -1)
            {
                prefix = text.split(":", 2)[0];
                boolean valid = true;
                for (int i = 0; i < prefix.length(); i++)
                {
                    char c = prefix.charAt(i);
                    if (c != 'k' && c != 'r' && c != 'c')
                    {
                        valid = false;
                        break;
                    }
                }
                if (valid == true)
                {
                    for (int i = 0; i < prefix.length(); i++)
                    {
                        this.searchPrefixes.add(Character.toString(prefix.charAt(i)));
                    }
                    text = text.split(":", 2)[1];
                    continue;
                }
                boolean found = true;
                Locale[] AvailableLocales = Locale.getAvailableLocales();
                for (int i = 0; i < AvailableLocales.length; i++)
                {
                    if (Util.getLanguageTag(AvailableLocales[i]).replace("-", "").equals(prefix) == true)
                    {
                        found = true;
                        break;
                    }
                }
                if (found == true)
                {
                    this.searchPrefixes.add(prefix);
                    text = text.split(":", 2)[1];
                    continue;
                }
                break;
            }
    
            text = text.replace("\\|", Character.toString((char) 0));
            text = text.replace("\\!", Character.toString((char) 1));
          
            String[] phrases = text.split("\\|");
            for (int i = 0; i < phrases.length; i++)
            {
                if (phrases[i].equals("") == false)
                {
                    boolean searchResult = true;
                    if (phrases[i].startsWith("!"))
                    {
                        searchResult = false;
                        phrases[i] = phrases[i].substring(1);
                    }
                    phrases[i] = phrases[i].replace(Character.toString((char) 0), "\\|");
                    phrases[i] = phrases[i].replace(Character.toString((char) 1), "!");
    
                    String search = (this.isRegexEnabled() == true) ? phrases[i] : ("\\Q" + phrases[i].replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E");
    
                    this.searchPatterns.put(Pattern.compile(search, 2), searchResult);
                }
            }
            if (this.searchPatterns.size() == 0)
            {
                this.searchPrefixes.clear();
            }
            return true;
        }
        void setRegexEnabled(boolean regexSearch)
        {
            this.regexSearch = regexSearch;
        }
    }
    static void init()
    {
        FilterManager.currentFilter = new Filter();
        FilterManager.currentFilter.empty = COConfigurationManager.getBooleanParameter("i18nAZ.emptyFilter");
        FilterManager.currentFilter.unchanged = COConfigurationManager.getBooleanParameter("i18nAZ.unchangedFilter");
        FilterManager.currentFilter.extra = COConfigurationManager.getBooleanParameter("i18nAZ.extraFilter");
        FilterManager.currentFilter.redirectKeys = COConfigurationManager.getBooleanParameter("i18nAZ.redirectKeysFilter");
        FilterManager.currentFilter.urls = COConfigurationManager.getIntParameter("i18nAZ.urlsFilter", 0);
        FilterManager.currentFilter.setRegexEnabled(COConfigurationManager.getBooleanParameter("i18nAZ.RegexSearch"));
    }
    static Filter getCurrentFilter()
    {
        return currentFilter;
    }
    static CountObject getCounts(String topKey)
    {
        return getCounts(LocalizablePluginManager.getCurrentLangFile(), FilterManager.currentFilter, topKey, TargetLocaleManager.toArray(), null);
    }
    static CountObject getCounts(String topKey, TargetLocale[] targetLocales)
    {
        return getCounts(LocalizablePluginManager.getCurrentLangFile(), FilterManager.currentFilter, topKey, targetLocales, null);
    }
    static CountObject getCounts(String topKey, TargetLocale selectedTargetLocale)
    {
        return getCounts(LocalizablePluginManager.getCurrentLangFile(), FilterManager.currentFilter, topKey, selectedTargetLocale.getParent(), selectedTargetLocale);
    }
    static CountObject getCounts(LangFileObject langFileObject, Filter filter, String topKey, TargetLocale[] targetLocales)
    {
        if (targetLocales == null)
        {
            return new CountObject();
        }
        return getCounts(langFileObject, filter, topKey, targetLocales, null);
    }
    static CountObject getCounts(LangFileObject langFileObject, Filter filter, String topKey, TargetLocale selectedTargetLocale)
    {
        if (selectedTargetLocale == null)
        {
            return new CountObject();
        }
        return getCounts(langFileObject, filter, topKey, selectedTargetLocale.getParent(), selectedTargetLocale);
    }
    private static CountObject getCounts(LangFileObject langFileObject, Filter filter, String topKey, TargetLocale[] targetLocales, TargetLocale selectedTargetLocale)
    {
        int columnIndex = -1;
        for (int i = 0; i < targetLocales.length; i++)
        {
            if (targetLocales[i].equals(selectedTargetLocale) == true)
            {
                columnIndex = i + 1;
            }
        }
        return getCounts(langFileObject, filter, topKey, targetLocales, columnIndex, null);
    }
    private static CountObject getCounts(LangFileObject langFileObject, Filter filter, String topKey, TargetLocale[] targetLocales, int columnIndex, CountObject parentCounts)
    {
        if (topKey != null && topKey.equals("") == true)
        {
            topKey = null;
        }
        PrebuildItemCollection prebuildItems = FilterManager.getPrebuildItems(langFileObject, false, filter, targetLocales, topKey);
        return FilterManager.getCounts(prebuildItems, parentCounts, columnIndex);
    }

    private static CountObject getCounts(PrebuildItemCollection prebuildItems, CountObject parentCounts, int columnIndex)
    {
        CountObject counts = new CountObject();
        parentCounts = (parentCounts == null) ? new CountObject() : parentCounts;
        for (int i = 0; i < prebuildItems.size(); i++)
        {
            PrebuildItem prebuildItem = prebuildItems.get(i);

            if (prebuildItem.childs != null)
            {
                PrebuildItemCollection childPrebuildItems = prebuildItem.childs;
                counts = FilterManager.getCounts(childPrebuildItems, counts, columnIndex);
            }

            if (prebuildItem.exist == false)
            {
                continue;
            }

            counts.entryCount++;

            if ((prebuildItem.states[1] & State.REDIRECT_KEY) != 0)
            {
                counts.redirectKeyCount++;
                continue;
            }
            if ((prebuildItem.states[1] & State.URL) != 0)
            {
                counts.urlsCount++;
                continue;
            }

            boolean rowContainEmpty = false;
            boolean rowContainUnchanged = false;
            boolean rowContainExtra = false;

            for (int j = 2; j < prebuildItems.getColumnCount(); j++)
            {
                if (columnIndex == -1 || columnIndex == j)
                {
                    switch (prebuildItem.states[j])
                    {
                        case State.EMPTY:
                            rowContainEmpty = true;
                            break;

                        case State.UNCHANGED:
                            rowContainUnchanged = true;
                            break;

                        case State.EXTRA:
                            rowContainExtra = true;
                            break;
                    }
                }
            }
            if (rowContainEmpty == true)
            {
                counts.emptyCount += 1;
            }
            if (rowContainUnchanged == true)
            {
                counts.unchangedCount += 1;
            }
            if (rowContainExtra == true)
            {
                counts.extraCount += 1;
            }
        }
        counts.add(parentCounts);
        return counts;
    }

    static PrebuildItem getPrebuildItem(String key, LangFileObject langFileObject)
    {
        TargetLocale[] targetLocales = TargetLocaleManager.toArray();
        LocaleProperties[] localeProperties = new LocaleProperties[targetLocales.length];
        for (int i = 0; i < localeProperties.length; i++)
        {
            if (langFileObject == null)
            {
                localeProperties[i] = new LocaleProperties(targetLocales[i].getLocale());
            }
            else
            {
                localeProperties[i] = targetLocales[i].getProperties(langFileObject);
            }
        }
        PrebuildItem prebuildItem = getPrebuildItem(key, false, FilterManager.currentFilter, localeProperties, null);
        if(prebuildItem != null)
        {
            prebuildItem.langFileObject = langFileObject;
        }
        return prebuildItem;        
    }

    static PrebuildItem getPrebuildItem(String key, boolean treeMode, Filter filter, LocaleProperties[] localeProperties, String topKey)
    {
        PrebuildItem prebuildItem = null;

        boolean show = true;

        boolean matchesSearch = filter.isTextEnabled() == false;

        if (topKey != null && key.startsWith(topKey) == false)
        {
            return prebuildItem;
        }

        String[] values = new String[localeProperties.length + 1];
        SpellObjectManager[] spellObjectManagers = new SpellObjectManager[localeProperties.length + 1];
        for(int i = 0; i < spellObjectManagers.length; i++)
        {
            spellObjectManagers[i] = new SpellObjectManager();
        }       
          
        int[] states = new int[localeProperties.length + 1];
        values[0] = (treeMode ? key.substring(key.lastIndexOf('.') + 1) : key);
        if (matchesSearch == false && key != null)
        {
            matchesSearch = filter.find(0, null, key);
        }

        // define show row values
        boolean showRowEmpty = filter.empty;
        boolean showRowUnchanged = filter.unchanged;
        boolean showRowExtra = filter.extra;
        boolean hideRedirectKeysFilter = filter.redirectKeys;
        int urlsFilterState = filter.urls;

        for (Iterator<String> iterator = filter.emptyExcludedKey.iterator(); iterator.hasNext();)
        {
            if (key.startsWith(iterator.next() + "."))
            {
                showRowEmpty = !showRowEmpty;
                break;
            }
        }
        for (Iterator<String> iterator = filter.unchangedExcludedKey.iterator(); iterator.hasNext();)
        {
            if (key.startsWith(iterator.next() + "."))
            {
                showRowUnchanged = !showRowUnchanged;
                break;
            }
        }
        for (Iterator<String> iterator = filter.extraExcludedKey.iterator(); iterator.hasNext();)
        {
            if (key.startsWith(iterator.next() + "."))
            {
                showRowExtra = !showRowExtra;
                break;
            }
        }
        for (Iterator<String> iterator = filter.hideRedirectKeysExcludedKey.iterator(); iterator.hasNext();)
        {
            if (key.startsWith(iterator.next() + "."))
            {
                hideRedirectKeysFilter = !hideRedirectKeysFilter;
                break;
            }
        }
        for (Iterator<Entry<String, Integer>> iterator = filter.urlsOverriddenStates.entrySet().iterator(); iterator.hasNext();)
        {
            Entry<String, Integer> entry = iterator.next();
            if (key.startsWith(entry.getKey() + "."))
            {
                urlsFilterState = entry.getValue();
                break;
            }
        }

        // reference
        values[1] = Util.escape(localeProperties[0].getProperty(key), false);
        states[1] = Util.getStateOfReference(values[1]);
        if (matchesSearch == false && values[1] != null)
        {
            matchesSearch = filter.find(1, null, values[1]);
        }

        // comments
        String[] commentsLines = new String[] {};

        commentsLines = localeProperties[0].getComment(key);
        if (matchesSearch == false && commentsLines != null)
        {
            String comments = "";
            for (int k = 0; k < commentsLines.length; k++)
            {
                comments += commentsLines[k].replaceAll("\\n", "\\\\n") + "\n";
            }
            matchesSearch = filter.find(-1, null, comments);
        }

        // show not redirect key
        if (hideRedirectKeysFilter == true && (states[1] & State.REDIRECT_KEY) != 0)
        {
            show = false;
        }

        // show/hide url
        switch (urlsFilterState)
        {
            case 1:
                if ((states[1] & State.URL) != 0)
                {
                    show = false;
                }
                break;

            case 2:
                if ((states[1] & State.URL) == 0)
                {
                    show = false;
                }
                break;
        }

        // values
        String rowText = values[1];
        boolean rowContainEmpty = false;
        boolean rowContainUnchanged = false;
        boolean rowContainExtra = false;

        for (int j = 1; j < localeProperties.length; j++)
        {
            values[j + 1] = Util.escape(localeProperties[j].getProperty(key), false);
            states[j + 1] = Util.getStateOfValue(values[1], values[j + 1]);
            if (matchesSearch == false)
            {
                matchesSearch = filter.find(j + 1, localeProperties[j].locale, values[j + 1]);
            }
            switch (states[j + 1])
            {
                case State.EMPTY:
                    rowContainEmpty = true;
                    break;

                case State.UNCHANGED:
                    rowContainUnchanged = true;
                    break;

                case State.EXTRA:
                    rowContainExtra = true;
                    break;
            }
            rowText += values[j + 1];
        }
        if (matchesSearch == false)
        {
            show = false;
        }

        if (show == true && (showRowEmpty || showRowUnchanged || showRowExtra))
        {
            show = (rowContainEmpty && showRowEmpty) || (rowContainUnchanged && showRowUnchanged) || (rowContainExtra && showRowExtra);
        }
        if (topKey != null)
        {
            show = true;
        }
        if (rowText.equals("") == true)
        {
            show = false;
        }
        if (show == true)
        {
            prebuildItem = new PrebuildItem();

            prebuildItem.key = key;
            prebuildItem.commentsLines = commentsLines;
            prebuildItem.values = values;
            prebuildItem.states = states;
            prebuildItem.spellObjectManagers = spellObjectManagers;   
            prebuildItem.exist = true;
        }
        return prebuildItem;
    }

    static PrebuildItemCollection getPrebuildItems(LangFileObject langFileObject)
    {     
        return FilterManager.getPrebuildItems(langFileObject, TreeTableManager.isTreeMode(), FilterManager.currentFilter, TargetLocaleManager.toArray(), null);
    }

    private static PrebuildItemCollection getPrebuildItems(LangFileObject langFileObject, boolean treeMode, Filter filter, TargetLocale[] targetLocales, String topKey)
    {        
        LocaleProperties[] localeProperties = new LocaleProperties[targetLocales.length];
        List<String> keys = new ArrayList<String>();
        for (int i = 0; i < localeProperties.length; i++)
        {
            if (langFileObject == null)
            {
                localeProperties[i] = new LocaleProperties(targetLocales[i].getLocale());
            }
            else
            {
                localeProperties[i] = targetLocales[i].getProperties(langFileObject);
            }
            if (localeProperties[i] != null && localeProperties[i].IsLoaded() == true)
            {
                for (Enumeration<?> enumeration = localeProperties[i].propertyNames(); enumeration.hasMoreElements();)       
                {
                    String key = (String) enumeration.nextElement();
                    if (keys.contains(key) == false)
                    {
                        keys.add(key);
                    }
                }
            }
        }
        Collections.sort(keys, Util.KEY_COMPARATOR);
        PrebuildItemCollection prebuildItems = new PrebuildItemCollection(localeProperties.length + 1);
        if (prebuildItems.getColumnCount() > 0)
        {           
            for (int i = 0; i < keys.size(); i++)
            {
                String currentKey = keys.get(i);
                PrebuildItem prebuildItem = FilterManager.getPrebuildItem(currentKey, treeMode, filter, localeProperties, topKey);
                if(prebuildItem == null)
                {
                    continue;
                }
                prebuildItem.langFileObject = langFileObject;
                if(treeMode == true)
                {
                    String[] parentKeys = keys.get(i).split("\\.");
                    PrebuildItemCollection tempPrebuildItems = prebuildItems;
                    for (int j = 0; j < parentKeys.length - 1; j++)
                    {
                        String parentKey = parentKeys[0];
                        for (int k = 0; k < j; k++)
                        {
                            parentKey += "." + parentKeys[k + 1];
                        } 
                        PrebuildItem parentPrebuildItem = tempPrebuildItems.get(parentKey);
                        if(parentPrebuildItem == null)
                        {
                            parentPrebuildItem = new PrebuildItem();
                            String[] values = new String[prebuildItems.getColumnCount()];
                            values[0] = parentKey.substring(parentKey.lastIndexOf('.') + 1);
                            parentPrebuildItem.key = parentKey;
                            parentPrebuildItem.commentsLines = new String[0];
                            parentPrebuildItem.values = values;
                            parentPrebuildItem.states = new int[prebuildItems.getColumnCount()];
                            SpellObjectManager[] spellObjectManagers = new SpellObjectManager[localeProperties.length + 1];
                            for(int k = 0; k < spellObjectManagers.length; k++)
                            {
                                spellObjectManagers[k] = new SpellObjectManager();
                            }  
                            parentPrebuildItem.spellObjectManagers = spellObjectManagers;
                            parentPrebuildItem.exist = false;
                            tempPrebuildItems.add(parentPrebuildItem);                            
                        } 
                        if(parentPrebuildItem.childs == null)
                        {
                            parentPrebuildItem.childs = new PrebuildItemCollection(prebuildItems.getColumnCount());
                        }
                        tempPrebuildItems = parentPrebuildItem.childs;                        
                    }
                    tempPrebuildItems.set(prebuildItem); 
                }
                else
                {
                    prebuildItems.add(prebuildItem);                    
                }
            }           
        }
        return prebuildItems;
    }
}

