/*
 * SpellChecker.java
 *
 * Created on May 03, 2014, 18:01 PM
 */
package i18nAZ;

import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;
import i18nAZ.SpellChecker.SpellObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import dk.dren.hunspell.Hunspell;
import dk.dren.hunspell.Hunspell.Dictionary;

/**
 * SpellChecker class
 * 
 * @author Repris d'injustice
 */
public class SpellChecker
{
    public final static int TYPE_PARAM = 1;
    public final static int TYPE_REFERENCE = 2;
    public final static int TYPE_URL = 4;
    public final static int TYPE_TRANSLATED_WORDS = 8;
    public final static int TYPE_MISSPELLING_BRACKETS = 16;
    public final static int TYPE_MISSPELLING_EXTRA_SPACES = 32;
    public final static int TYPE_MISSPELLING_VUZE = 64;
    public final static int TYPE_MISSPELLING_DUPLICATE = 128;
    public final static int TYPE_MISSPELLING_ERROR = 256;
    public final static int TYPE_MISSPELLING = TYPE_MISSPELLING_EXTRA_SPACES | TYPE_MISSPELLING_BRACKETS | TYPE_MISSPELLING_VUZE | TYPE_MISSPELLING_DUPLICATE | TYPE_MISSPELLING_ERROR;
    private final static String[] separators = new String[] { "\\n", " ", "(", ")", "[", "]", "%", "{", "}", "\"", ",", "\u00A0", "\u2007", "\u202F", "\n", "\r", "\t", "\\r", "\\t", "=", "+", "*", ";", "?", "!", "<", ">", "#" };
    private final static String[] startSymbols = new String[] { "'", "«"};
    private final static String[] endSymbols = new String[] { "'", "»", "™", "®", "©"};
    private final static Character[] startBrackets = new Character[] { '(', '[', '{', '«'};
    private final static Character[] endBrackets = new Character[] { ')', ']', '}', '»'};    
    
    public static class Suggestion
    {
        private String name = null;
        private String value = null;  
        public Suggestion(String name)
        {
            this.name = name;
            this.value = name;
        }
        public Suggestion(String name, String value)
        {
            this.name = name;
            this.value = value;
        }
        public String getName()
        {
            return this.name;
        }
        public String getValue()
        {
            return this.value;
        }
    }

    public static class SpellObjectManager
    {
        final private Object mutex = new Object();
        private Locale locale = null;
        private String value = null;
        private SpellObject[] spellObjects;
        private SpellObject[] translatedObjects = null;
        SpellObjectManager()
        {
            this.locale = Util.EMPTY_LOCALE;
            this.value = "";            
        }
        
        public SpellObject[] getSpellObjects(LangFileObject langFileObject, String key, Locale locale, String value, boolean multiLine)
        {
            locale = (locale == null) ? Util.EMPTY_LOCALE : locale;              
            value = (value == null) ? "": value;
            String oldValue = value;
            if (multiLine == false)
            {
                value = Util.unescape(value);
            }
            synchronized(this.mutex )
            {
                if(this.spellObjects == null || this.locale.equals(locale) == false || this.value.equals(value) == false)
                {
                    this.locale = locale;
                    this.value = value;                      
                    this.spellObjects =  SpellChecker.get(langFileObject, key, 0, value, locale);                    
                }
                SpellObject[] finalSpellObjects = null;
                if(this.spellObjects != null)
                {
                    finalSpellObjects = new SpellObject[this.spellObjects.length + ((translatedObjects == null)? 0: translatedObjects.length)];
                    for(int i = 0; i  < this.spellObjects.length; i++)
                    {
                        finalSpellObjects[i] = (SpellObject) this.spellObjects[i].clone();
                    }
                    if(translatedObjects != null)
                    {
                        for(int i = this.spellObjects.length; i  < finalSpellObjects.length; i++)
                        {
                            finalSpellObjects[i] = (SpellObject) this.translatedObjects[i - this.spellObjects.length].clone();
                        }
                    }                    
                }         
                if (multiLine == false)
                {
                    int offset= 0;
                    String[] result = new String[2];
                    while((offset = Util.findEscape(oldValue, offset, result)) != -1)
                    {
                        for(int i = 0; i < finalSpellObjects.length; i++)
                        {
                            if(finalSpellObjects[i].getOffset()>=offset)
                            {
                                finalSpellObjects[i].offset += result[1].length() - 1;
                            }                                
                        }
                        offset += result[1].length();
                    }
                } 
                return finalSpellObjects;
            }            
        }

        public void setTranslatedObjects(SpellObject[] translatedObjects)
        {
            this.translatedObjects  = translatedObjects;            
            synchronized(this.mutex )
            {
                this.spellObjects = null;
            }  
        }

        public void reset()
        {
            synchronized(this.mutex )
            {
                this.spellObjects = null;
            }            
        }
    }
    
    public static class SpellObject implements Cloneable
    {
        private int type = 0;
        private int offset = 0;
        int length = 0;
        String value = null;
        private Suggestion[] suggestions;
        public Object clone()
        {
            SpellObject spellObject = new SpellObject(this.type, this.offset, this.length, this.value, this.suggestions == null ? null : this.suggestions.clone());
            return spellObject;       
        }
        public SpellObject(int type, int offset, int length, String value)
        {
            this.type = type;
            this.offset = offset;
            this.length = length;
            this.value = value;
        }

        public SpellObject(int type, int offset, int length, String value, Suggestion[] suggestions)
        {
            this.type = type;
            this.offset = offset;
            this.length = length;
            this.value = value;
            this.suggestions = suggestions;
        }

        public int getType()
        {
            return this.type;
        }

        public int getOffset()
        {
            return this.offset;
        }

        public int getLength()
        {
            return this.length;
        }

        public String getValue()
        {
            return this.value;
        }

        public Suggestion[] getSuggestions()
        {
            return this.suggestions;
        }
    }

    final private static Map<Locale, Dictionary> dictionaries = new HashMap<Locale, Dictionary>();
    private static Properties userDictionaries = null;
    final private static Object ignoreMutex = new Object();
    private static Properties ignoreDictionaries = null;
    private static String toUserWord(String word)
    {
        if(word.length() > 1 && Character.isUpperCase(word.charAt(0)) == true)
        {
             boolean isFirstUppercaseOnly= true;
             for (int i = 1; i < word.length(); i++)
             {
                 if(Character.isLowerCase(word.charAt(i)) == false)
                 {
                     isFirstUppercaseOnly = false;
                     break;                     
                 }                 
             }
             if(isFirstUppercaseOnly == true)
             {
                 word = word.toLowerCase(Locale.US);                 
             }        
        }
        return word;
    }
    public static void add(String word)
    {
        word = SpellChecker.toUserWord(word);         
        synchronized (SpellChecker.dictionaries)
        {            
            for(Iterator<Entry<Locale, Dictionary>> iterator = SpellChecker.dictionaries.entrySet().iterator(); iterator.hasNext();)
            {
                Entry<Locale, Dictionary> entry = iterator.next();
                entry.getValue().add(word);
            }
            
            if(SpellChecker.userDictionaries != null)
            {
                SpellChecker.userDictionaries.put(word, "");
                Util.saveProperties(SpellChecker.userDictionaries, Path.getUrl(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\user.properties"));
            }
        }
    }
    public static void reset()
    {        
        synchronized (SpellChecker.ignoreMutex)
        {
            if(SpellChecker.ignoreDictionaries != null)
            {                    
                SpellChecker.ignoreDictionaries.clear();                
                File ignoreDictionariesFile = new File(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\ignore.properties");                               
                ignoreDictionariesFile.delete();
                SpellChecker.ignoreDictionaries = null; 
            }
        }
        synchronized (SpellChecker.dictionaries)
        {            
            SpellChecker.dictionaries.clear();
            if(SpellChecker.userDictionaries != null)
            {
                SpellChecker.userDictionaries.clear();
                File userDictionariesFile = new File(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\user.properties");
                userDictionariesFile.delete();
                SpellChecker.userDictionaries = null;                
            }
        }
    }
    public static void resetIgnore(LangFileObject langFileObject, String key)
    {        
        synchronized (SpellChecker.ignoreMutex)
        {
            if(SpellChecker.ignoreDictionaries != null)
            {
                if(SpellChecker.ignoreDictionaries.containsKey(langFileObject.getId() + "/" + key) == false)
                {
                    return;
                }                    
                SpellChecker.ignoreDictionaries.remove(langFileObject.getId() + "/" + key);                
                Util.saveProperties(SpellChecker.ignoreDictionaries, Path.getUrl(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\ignore.properties"));                                 
            }
        }
    }
    public static void resetIgnore(LangFileObject langFileObject, String key, int type, String value)
    {        
        synchronized (SpellChecker.ignoreMutex)
        {
            if(SpellChecker.ignoreDictionaries != null)
            {
                if(SpellChecker.ignoreDictionaries.containsKey(langFileObject.getId() + "/" + key) == false)
                {
                    return;
                }
                List<String> ignores = listIgnores(langFileObject, key);
                if(ignores == null)
                {
                    return;
                }
                String ignore = String.valueOf(type) + ":";
                if(type == SpellChecker.TYPE_MISSPELLING_ERROR)
                {
                    ignore += SpellChecker.toUserWord(value);
                }
                else if(type == SpellChecker.TYPE_MISSPELLING_VUZE)
                {
                    ignore += SpellChecker.toUserWord(value);          
                }
                else if(type == SpellChecker.TYPE_MISSPELLING_EXTRA_SPACES)
                {
                    ignore += String.valueOf(value) + "_" + String.valueOf(value);                        
                }
                else if(type == SpellChecker.TYPE_MISSPELLING_DUPLICATE)
                {
                    ignore += value;                                               
                }
                if(ignores.contains(ignore) == true)
                {
                    ignores.remove(ignore);
                }              
                if(ignores.size() == 0)
                {
                    SpellChecker.ignoreDictionaries.remove(langFileObject.getId() + "/" + key);  
                }
                else
                {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ignores.get(0));
                    for(int i = 1; i < ignores.size(); i++)
                    {
                        stringBuilder.append("\n");
                        stringBuilder.append(ignores.get(i));                    
                    }
                    SpellChecker.ignoreDictionaries.put(langFileObject.getId()+ "/" + key , stringBuilder.toString());
                }
                Util.saveProperties(SpellChecker.ignoreDictionaries, Path.getUrl(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\ignore.properties"));                                 
            }
        }
    }
    public static void resetIgnore(LangFileObject langFileObject, String key, int types)
    {        
        synchronized (SpellChecker.ignoreMutex)
        {
            if(SpellChecker.ignoreDictionaries != null)
            {
                if(SpellChecker.ignoreDictionaries.containsKey(langFileObject.getId() + "/" + key) == false)
                {
                    return;
                }
                List<String> ignores = listIgnores(langFileObject, key);
                if(ignores == null)
                {
                    return;
                }
                for(int i = ignores.size() - 1; i >= 0; i--)
                {
                    String[] blocksIgnore = ignores.get(i).split(":", 2);
                    if(blocksIgnore.length == 2)
                    {
                        int type = Integer.parseInt(blocksIgnore[0]);
                        if((types & type) != 0 || types == -1)
                        {
                            ignores.remove(i);
                        }                        
                    }
                    else
                    {
                        ignores.remove(i);
                    }
                } 
                if(ignores.size() == 0)
                {
                    SpellChecker.ignoreDictionaries.remove(langFileObject.getId() + "/" + key);  
                }
                else
                {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ignores.get(0));
                    for(int i = 1; i < ignores.size(); i++)
                    {
                        stringBuilder.append("\n");
                        stringBuilder.append(ignores.get(i));                    
                    }
                    SpellChecker.ignoreDictionaries.put(langFileObject.getId()+ "/" + key , stringBuilder.toString());
                }
                Util.saveProperties(SpellChecker.ignoreDictionaries, Path.getUrl(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\ignore.properties"));                                 
            }
        }
    }
    private static List<String> listIgnores(LangFileObject langFileObject, String key)
    {
        if(SpellChecker.ignoreDictionaries != null)
        {
            if(SpellChecker.ignoreDictionaries.containsKey(langFileObject.getId() + "/" + key ) == false)
            {
                return null;
            }
            String[] blocks = ((String) SpellChecker.ignoreDictionaries.get(langFileObject.getId() + "/" + key)).split("\\n");
            List<String> ignores = new ArrayList<String>();
            ignores.addAll(Arrays.asList(blocks));
            return ignores;
        }
        return null;
    }
    private static List<String> listIgnores(LangFileObject langFileObject, String key, int type)
    {
        if(SpellChecker.ignoreDictionaries != null)
        {
            if(SpellChecker.ignoreDictionaries.containsKey(langFileObject.getId() + "/" + key ) == false)
            {
                return null;
            }
            String[] blocks = ((String) SpellChecker.ignoreDictionaries.get(langFileObject.getId() + "/" + key)).split("\\n");
            List<String> ignores = new ArrayList<String>();
            for(int i = 0; i < blocks.length; i++)
            {
                String[] blocksIgnore = blocks[i].split(":", 2);
                if(blocksIgnore.length == 2)
                {
                    int currentType = Integer.parseInt(blocksIgnore[0]);
                    if(currentType == type)
                    {
                        ignores.add(blocksIgnore[1]);
                    }
                }
            }
            return ignores;
        }
        return null;
    }
    public static boolean verifIgnore(LangFileObject langFileObject, String key, SpellObject spellObject)
    {
        if((spellObject.getType() & SpellChecker.TYPE_MISSPELLING_BRACKETS) != 0)
        {
            return true;
        }
        synchronized (SpellChecker.ignoreMutex)
        {
            if(SpellChecker.ignoreDictionaries != null)
            {                
                List<String> ignores = listIgnores(langFileObject, key);
                if(ignores == null)
                {
                    return true;
                }
                for(int i = ignores.size() - 1; i >= 0; i--)
                {
                    String[] blocksIgnore = ignores.get(i).split(":", 2);
                    if(blocksIgnore.length == 2)
                    {
                        int type = Integer.parseInt(blocksIgnore[0]);
                        if(type != spellObject.getType())
                        {
                            ignores.remove(i);
                        }                        
                    }
                    else
                    {
                        ignores.remove(i);
                    }
                } 
                if(ignores.size() == 0)
                {
                    return true;
                }
                String ignore = String.valueOf(spellObject.getType()) + ":";
                if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_ERROR)
                {
                    ignore += SpellChecker.toUserWord(spellObject.getValue());
                }
                else if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_VUZE)
                {
                    ignore += SpellChecker.toUserWord(spellObject.getValue());          
                }
                else if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_EXTRA_SPACES)
                {
                    ignore += String.valueOf(spellObject.getOffset()) + "_" + String.valueOf(spellObject.getLength());                        
                }
                else if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_DUPLICATE)
                {
                    ignore += spellObject.getValue();                                               
                }
                return ignores.contains(ignore) == false;
            }
            else
            {
                return true;
            }
        }        
    }
    
    public static void ignore(LangFileObject langFileObject, String key, SpellObject spellObject)
    {        
        if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_BRACKETS)
        {
            return;
        }
        synchronized (SpellChecker.ignoreMutex)
        {            
            if(SpellChecker.ignoreDictionaries != null)
            {
                List<String> ignores = listIgnores(langFileObject, key);
                if(ignores == null)
                {
                    ignores = new ArrayList<String>();
                }
                String ignore = String.valueOf(spellObject.getType()) + ":";
                if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_ERROR)
                {
                    ignore += SpellChecker.toUserWord(spellObject.getValue());
                }
                else if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_VUZE)
                {
                    ignore += SpellChecker.toUserWord(spellObject.getValue());          
                }
                else if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_EXTRA_SPACES)
                {
                    ignore += String.valueOf(spellObject.getOffset()) + "_" + String.valueOf(spellObject.getLength());                        
                }
                else if(spellObject.getType() == SpellChecker.TYPE_MISSPELLING_DUPLICATE)
                {
                    ignore += spellObject.getValue();                                               
                }
                if(ignores.contains(ignore) == false)
                {
                    ignores.add(ignore);
                }                
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(ignores.get(0));
                for(int i = 1; i < ignores.size(); i++)
                {
                    stringBuilder.append("\n");
                    stringBuilder.append(ignores.get(i));                    
                }               
                SpellChecker.ignoreDictionaries.put(langFileObject.getId()+ "/" + key , stringBuilder.toString());
                Util.saveProperties(SpellChecker.ignoreDictionaries, Path.getUrl(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\ignore.properties"));
            }
        }
    }
    public static void resetDictionary(Locale locale)
    {
        synchronized (SpellChecker.dictionaries)
        {
            SpellChecker.dictionaries.remove(locale);             
        }
    }
    public static Dictionary getDictionary(Locale locale)
    {
        Dictionary dictionary = null;
        synchronized (SpellChecker.ignoreMutex)
        {
            if(SpellChecker.ignoreDictionaries == null)
            {
                SpellChecker.ignoreDictionaries = Util.getProperties(Path.getUrl(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\ignore.properties"));
                if(SpellChecker.ignoreDictionaries == null)
                {
                    SpellChecker.ignoreDictionaries = new Properties();
                }
            }
        }
        synchronized (SpellChecker.dictionaries)
        {
            if(SpellChecker.userDictionaries == null)
            {
                SpellChecker.userDictionaries = Util.getProperties(Path.getUrl(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\user.properties"));
                if(SpellChecker.userDictionaries == null)
                {
                    SpellChecker.userDictionaries = Util.getProperties(i18nAZ.getPluginInterface().getPluginClassLoader().getResource("userDictionary.properties"));
                }
            } 
            if (SpellChecker.dictionaries.containsKey(locale) == false)
            {
                try
                {                    
                    dictionary = Hunspell.getInstance().getDictionary(i18nAZ.getPluginInterface().getPluginDirectoryName() + "\\dictionaries\\" + Util.getLanguageTag(locale).replace('-', '_'));
                    if(SpellChecker.userDictionaries != null)
                    {
                        for (Enumeration<?> enumeration = SpellChecker.userDictionaries.propertyNames(); enumeration.hasMoreElements();)       
                        {
                            String key = (String) enumeration.nextElement();                        
                            dictionary.add(key);
                        }                    
                    }                 
                }
                catch (Exception e)
                {   
                    DictionaryDownloader.add(locale);
                    
                }catch( Throwable e ){
                	e.printStackTrace();
                }
                SpellChecker.dictionaries.put(locale, dictionary); 
            }
            else
            {
                dictionary = SpellChecker.dictionaries.get(locale);
            }
        }
        return dictionary;
    }

    public static SpellObject[] get(LangFileObject langFileObject, String key, int offset, String value, Locale locale)
    {
        List<SpellObject> spellObjects = new ArrayList<SpellObject>();
        spellObjects.addAll(SpellChecker.getReferences(offset, value));
        spellObjects.addAll(SpellChecker.getUrls(offset, value));
        if (locale != null)
        {
            spellObjects.addAll(SpellChecker.getMisspelling(langFileObject, key, offset, value, locale, spellObjects));
        }
        spellObjects.addAll(SpellChecker.getParams(offset, value));
        return spellObjects.toArray(new SpellObject[spellObjects.size()]);
    }  
    
    private static List<SpellObject> getMisspelling(LangFileObject langFileObject, String key, int offset, String value, Locale locale, List<SpellObject> lastSpellObjects)
    {        
        value = (value == null) ? "" : value;
        List<SpellObject> misspellings = new ArrayList<SpellObject>();
        Dictionary dictionary = SpellChecker.getDictionary(locale);
        
        if ( dictionary == null ){
        	return( misspellings );
        }
        
        String[] spaceWords = value.split("\\s+");        

        //check duplicate word
        int index = 0;
        List<String> duplicates = new ArrayList<String>();
        int lastSpaceLength = 0;
        for (int i = 0; i < spaceWords.length; i++)
        {
            if (i > 0 && spaceWords[i].equals(spaceWords[i - 1]) == true)
            {
                String misspelling = value.substring(index - spaceWords[i - 1].length() - lastSpaceLength, index + spaceWords[i].length());
                List<Suggestion> suggestions = new ArrayList<Suggestion>();
                suggestions.add(new Suggestion(spaceWords[i]));                    
                SpellObject spellObject = new SpellObject(SpellChecker.TYPE_MISSPELLING_DUPLICATE, offset + index - spaceWords[i - 1].length() - lastSpaceLength, misspelling.length(), misspelling, suggestions.toArray(new Suggestion[suggestions.size()]));
                duplicates.add(misspelling);
                if(SpellChecker.verifIgnore(langFileObject, key, spellObject) == true)
                {
                    misspellings.add(spellObject);
                    if(lastSpellObjects != null)
                    {
                        lastSpellObjects.add(spellObject);
                    } 
                }               
            }
            
            index += spaceWords[i].length();
            if(i < spaceWords.length -1)
            {
                lastSpaceLength = value.indexOf(spaceWords[i + 1], index) - index;
                index += lastSpaceLength;
            }                    
        }        
        List<String> duplicateIgnores = SpellChecker.listIgnores(langFileObject, key, SpellChecker.TYPE_MISSPELLING_DUPLICATE);        
        for(int i = 0; duplicateIgnores != null && i < duplicateIgnores.size(); i++)
        {
            if(duplicates.contains(duplicateIgnores.get(i)) == false)
            {
                SpellChecker.resetIgnore(langFileObject, key, SpellChecker.TYPE_MISSPELLING_DUPLICATE, duplicateIgnores.get(i));
            }  
        }
        List<String> temp = new ArrayList<String>();
        List<Integer> temp2 = new ArrayList<Integer>();
        index = 0;
        for (int i = 0; lastSpellObjects!= null && i < lastSpellObjects.size(); i++)
        {
            temp.add(value.substring(index, lastSpellObjects.get(i).getOffset()));
            temp2.add(lastSpellObjects.get(i).getLength());
            index = lastSpellObjects.get(i).getOffset() + lastSpellObjects.get(i).getLength();
        }
        if (index < value.length() )
        {
            temp.add(value.substring(index));
            temp2.add(0);
        }
        for (int i = 0; i < separators.length; i++)
        {
            String[]  words = temp.toArray(new String[temp.size()]);
            Integer[] indexes = temp2.toArray(new Integer[temp2.size()]);
            temp.clear();
            temp2.clear();
            for (int j = 0; j < words.length; j++)
            {
                int pad = indexes[j];
                if (words[j].equals("") == true)
                {
                    temp.add("");
                    temp2.add(pad);
                    continue;
                }
                if (words[j].contains(separators[i]) == false)
                {
                    temp.add(words[j]);
                    temp2.add(pad);
                    continue;
                }
                int index1 = 0;
                while (true)
                {
                    int index2 = words[j].indexOf(separators[i], index1);
                    if (index2 == -1)
                    {
                        if (words[j].length() == index1)
                        {
                            temp.add("");
                            temp2.add(pad);
                        }
                        else
                        {
                            temp.add(words[j].substring(index1));
                            temp2.add(pad);
                        }
                        break;
                    }
                    temp.add(words[j].substring(index1, index2));
                    temp2.add(separators[i].length());
                    index1 = index2 + separators[i].length();
                }

            }
        }
        
        //check vuze error and spell error
        List<String> vuzes = new ArrayList<String>();
        List<String> errorWords = new ArrayList<String>();
        String[] words = temp.toArray(new String[temp.size()]);
        Integer[] indexes = temp2.toArray(new Integer[temp2.size()]);
        index = 0;
        for (int i = 0; i < words.length; i++)
        {
            if (words[i].equals("") == false)
            {           
                String word = words[i];
                String wordWithTrademarkSymbol = words[i];
                int index2 = index;
                while(true)
                {
                    boolean found = false;
                    for (int j = 0; j < startSymbols.length; j++)
                    {
                        while(word.startsWith(startSymbols[j]) == true && word.length() > 1)
                        {
                            found = true;
                            word = word.substring(1);
                            if(j < 3)
                            {
                                wordWithTrademarkSymbol = word;
                            }
                            index2++;
                        }
                    }
                    if (found == false)
                    {
                        break;
                    }
                }
                while(true)
                {
                    boolean found = false;
                    for (int j = 0; j < endSymbols.length; j++)
                    {
                        while(word.endsWith(endSymbols[j]) == true && word.length() > 1)
                        {
                            found = true;
                            word = word.substring(0, word.length() - 1);
                            if(j < 2)
                            {
                                wordWithTrademarkSymbol = word;
                            }
                        }
                    }
                    if (found == false)
                    {
                        break;
                    }
                }
                boolean isVuzeWord = false;
                if ((wordWithTrademarkSymbol.equalsIgnoreCase("vuze") == true || wordWithTrademarkSymbol.equalsIgnoreCase("vuze™") == true) && wordWithTrademarkSymbol.equals("Vuze™") == false)
                {
                    List<Suggestion> suggestions = new ArrayList<Suggestion>();
                    suggestions.add(new Suggestion("Vuze™"));                        
                    SpellObject spellObject = new SpellObject(SpellChecker.TYPE_MISSPELLING_VUZE, offset + index2, word.length(), wordWithTrademarkSymbol, suggestions.toArray(new Suggestion[suggestions.size()]));
                    vuzes.add(SpellChecker.toUserWord(wordWithTrademarkSymbol));
                    if(SpellChecker.verifIgnore(langFileObject, key, spellObject) == true)
                    {                        
                        misspellings.add(spellObject);
                        isVuzeWord = true;
                    }                 
                }
              
                boolean containsNumeric = word.matches(".*\\d.*");                
                if(isVuzeWord == false && containsNumeric == false && word.equals("™") == false &&
                        word.equals("-") == false && 
                        word.equals("'") == false &&
                        word.equals("&") == false &&
                        word.contains("/") == false &&
                        word.contains("\\") == false &&
                        word.contains("_") == false &&
                        word.contains(".") == false &&
                        word.contains("|") == false &&
                        word.contains(":") == false &&
                        word.contains("«") == false &&
                        word.contains("»") == false)
                {                        
                    if (dictionary != null && dictionary.misspelled(word.replaceAll("&", "")) == true)
                    {
                        List<String> suggestions1 = dictionary.suggest(word);
                        List<Suggestion> suggestions2 = new ArrayList<Suggestion>();
                        for (int j = 0; j < suggestions1.size(); j++)   
                        {
                            suggestions2.add(new Suggestion(suggestions1.get(j)));                             
                        }
                        SpellObject spellObject = new SpellObject(SpellChecker.TYPE_MISSPELLING_ERROR, offset + index2, word.length(), word, suggestions2.toArray(new Suggestion[suggestions2.size()]));
                        errorWords.add(SpellChecker.toUserWord(word));
                        if(SpellChecker.verifIgnore(langFileObject, key, spellObject) == true)
                        {                            
                            misspellings.add(spellObject);
                        }                                              
                    }
                }
                
            }
            index += words[i].length() + indexes[i];
        }
        List<String> vuseIgnores = SpellChecker.listIgnores(langFileObject, key, SpellChecker.TYPE_MISSPELLING_VUZE);        
        for(int i = 0;  vuseIgnores != null && i < vuseIgnores.size(); i++)
        {
            if(vuzes.contains(vuseIgnores.get(i)) == false)
            {
                SpellChecker.resetIgnore(langFileObject, key, SpellChecker.TYPE_MISSPELLING_VUZE, vuseIgnores.get(i));
            }  
        }
        List<String> errorIgnores = SpellChecker.listIgnores(langFileObject, key, SpellChecker.TYPE_MISSPELLING_ERROR);        
        for(int i = 0;  errorIgnores != null && i < errorIgnores.size(); i++)
        {
            if(errorWords.contains(errorIgnores.get(i)) == false)
            {
                SpellChecker.resetIgnore(langFileObject, key, SpellChecker.TYPE_MISSPELLING_ERROR, errorIgnores.get(i));
            }  
        }

        //check extra space
        int extraSpaceCount = 0;
        int indexOfSpace = -1;
        int currentOffset = 0;
        String[] lines = value.split("[\\n\\r]", -1);
        for(int i = 0; i < lines.length; i++)
        {
            while (true)
            {
                indexOfSpace = lines[i].indexOf(' ', indexOfSpace + 1);
                if (indexOfSpace == -1)
                {
                    break;
                }
                String misspelling = null;
                for (int j = indexOfSpace + 1; j <= lines[i].length(); j++)
                {
                    if (j == lines[i].length() || lines[i].charAt(j) != ' ')
                    {
                        if (j == lines[i].length()  || indexOfSpace == 0 || j - indexOfSpace > 1)
                        {
                            misspelling = lines[i].substring(indexOfSpace, j);
                        }
                        break;
                    }
                }
                if (misspelling != null)
                {
                    if ((i == 0 && misspelling.length() < 3) || indexOfSpace != 0)
                    {
                        List<Suggestion> suggestions = new ArrayList<Suggestion>();                
                        suggestions.add(new Suggestion(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.RemoveExtraSpaces"), misspelling.length() == 1 ? "" : " "));
                        SpellObject spellObject = new SpellObject(SpellChecker.TYPE_MISSPELLING_EXTRA_SPACES, offset + currentOffset + indexOfSpace, misspelling.length(), misspelling, suggestions.toArray(new Suggestion[suggestions.size()]));
                        extraSpaceCount++;
                        if(SpellChecker.verifIgnore(langFileObject, key, spellObject) == true)
                        {
                            misspellings.add(spellObject);          
                        }
                    }   
                    indexOfSpace += misspelling.length();                 
                }
            }
            currentOffset += 1 + lines[i].length();
        }
        if(extraSpaceCount == 0)
        {
            SpellChecker.resetIgnore(langFileObject, key, SpellChecker.TYPE_MISSPELLING_EXTRA_SPACES);
        }

        //check brackets
        int indexOfBracket = 0;
        List<Object[]> charsList = new ArrayList<Object[]>();
        while (indexOfBracket < value.length())
        {
            String misspelling = null;
            boolean isEndBarcket = false;
            for(int i = 0; i < SpellChecker.endBrackets.length; i++)
            {
                if(value.charAt(indexOfBracket) == SpellChecker.endBrackets[i])
                {
                    isEndBarcket = true;
                    break;
                }
            }
            if (isEndBarcket == true)
            {
                if (charsList.size() == 0)
                {
                    misspelling = value.substring(indexOfBracket, indexOfBracket + 1);
                    SpellObject spellObject = new SpellObject(SpellChecker.TYPE_MISSPELLING_BRACKETS, offset + indexOfBracket, misspelling.length(), misspelling);
                    if(SpellChecker.verifIgnore(langFileObject, key, spellObject) == true)
                    {
                        misspellings.add(spellObject);
                    }
                    break;
                }
                else if (value.charAt(indexOfBracket) != (Character) charsList.get(0)[0])
                {
                    misspelling = value.substring((Integer) charsList.get(0)[1], (Integer) charsList.get(0)[1] + 1);
                    SpellObject spellObject = new SpellObject(SpellChecker.TYPE_MISSPELLING_BRACKETS, offset + (Integer) charsList.get(0)[1], misspelling.length(), misspelling);
                    if(SpellChecker.verifIgnore(langFileObject, key, spellObject) == true)
                    {
                        misspellings.add(spellObject);
                    }
                    break;
                }
                else
                {
                    charsList.remove(0);
                }
                indexOfBracket++;
                continue;
            }
            boolean isNotStartBarcket = true;
            for(int i = 0; i < SpellChecker.startBrackets.length; i++)
            {
                if(value.charAt(indexOfBracket) == SpellChecker.startBrackets[i])
                {
                    isNotStartBarcket = false;
                    break;
                }
            }
            if (isNotStartBarcket == true)
            {
                indexOfBracket++;
                continue;
            }
            for(int i = 0; i < SpellChecker.startBrackets.length; i++)
            {
                if (value.charAt(indexOfBracket) == SpellChecker.startBrackets[i])
                {
                    charsList.add(0, new Object[] {SpellChecker.endBrackets[i], indexOfBracket });
                    break;
                }
            }     
            indexOfBracket++;
        }
        if (indexOfBracket == value.length())
        {
            for (int i = 0; i < charsList.size(); i++)
            {
                String misspelling = value.substring((Integer) charsList.get(i)[1], (Integer) charsList.get(i)[1] + 1);
                SpellObject spellObject = new SpellObject(SpellChecker.TYPE_MISSPELLING_BRACKETS, offset + (Integer) charsList.get(i)[1], misspelling.length(), misspelling);
                if(SpellChecker.verifIgnore(langFileObject, key, spellObject) == true)
                {
                    misspellings.add(spellObject);
                }
            }
        }
        return misspellings;
    }

    private static List<SpellObject> getParams(int offset, String value)
    {
        value = (value == null) ? "" : value;
        List<SpellObject> params = new ArrayList<SpellObject>();
        int indexOfPercent = -1;
        while (true)
        {
            indexOfPercent = value.indexOf('%', indexOfPercent + 1);
            if (indexOfPercent == -1 || indexOfPercent == value.length() - 1)
            {
                break;
            }
            int integer = -1;
            String param = null;
            int i = 0;
            for (i = indexOfPercent + 1; i < value.length(); i++)
            {
                try
                {
                    param = value.substring(indexOfPercent, i + 1);
                    integer = Integer.parseInt(param.substring(1));
                }
                catch (NumberFormatException nfe)
                {
                    break;
                }
            }
            if (integer != -1)
            {
                param = param.substring(0, param.length() - (i == value.length()? 0 : 1));                
                params.add(new SpellObject(SpellChecker.TYPE_PARAM, offset + indexOfPercent, param.length(), param));
            }
        }
        return params;
    }

    public static List<SpellObject> getReferences(int offset, String value)
    {
        value = (value == null) ? "" : value;
        List<SpellObject> references = new ArrayList<SpellObject>();
        int indexOfCurlyBracket = -1;
        while (true)
        {
            indexOfCurlyBracket = value.indexOf('{', indexOfCurlyBracket + 1);
            if (indexOfCurlyBracket == -1 || indexOfCurlyBracket == value.length() - 1)
            {
                break;
            }
            String reference = null;
            for (int i = indexOfCurlyBracket + 1; i < value.length(); i++)
            {
                if (value.charAt(i) == '}')
                {
                    boolean IsNotRef = false;
                    for (int j = indexOfCurlyBracket + 1; j < i; j++)
                    {
                        char c = value.charAt(j);
                        if (c == '#' || c == '.' || c == '_' || c == '%' || c == '-' || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || (c >= 48 && c <= 57))
                        {
                            continue;
                        }
                        IsNotRef = true;
                        break;
                    }
                    if (IsNotRef == false)
                    {
                        reference = value.substring(indexOfCurlyBracket, i + 1);
                    }
                    break;
                }
            }
            if (reference != null)
            {
                references.add(new SpellObject(SpellChecker.TYPE_REFERENCE, offset + indexOfCurlyBracket, reference.length(), reference));
            }
        }
        return references;
    }

    public static List<SpellObject> getUrls(int offset, String value)
    {
        value = (value == null) ? "" : value;
        List<SpellObject> urls = new ArrayList<SpellObject>();
        int indexOfScheme = -1;
        while (true)
        {
            indexOfScheme = value.indexOf("http://", indexOfScheme + 1);
            if (indexOfScheme == -1 || indexOfScheme == value.length() - 7)
            {
                break;
            }
            char finalChar = ' ';
            if (indexOfScheme - 1 > -1)
            {
                if (value.charAt(indexOfScheme - 1) == '\'' || value.charAt(indexOfScheme - 1) == '"')
                {
                    finalChar = value.charAt(indexOfScheme - 1);
                }
            }
            String url = null;
            for (int i = indexOfScheme + 6; i < value.length(); i++)
            {
                try
                {
                    new URL(value.substring(indexOfScheme, i));
                }
                catch (MalformedURLException e)
                {
                    break;
                }
                if (value.charAt(i) == finalChar)
                {
                    url = value.substring(indexOfScheme, i);
                    break;
                }
                if (finalChar == ' ')
                {
                    if (value.charAt(i) == '\n' || value.charAt(i) == '\r' || value.charAt(i) == ')')
                    {
                        url = value.substring(indexOfScheme, i);
                        break;
                    }
                    if (i == value.length() - 1)
                    {
                        url = value.substring(indexOfScheme, i + 1);
                        break;
                    }
                }
            }
            if (url != null)
            {
                urls.add(new SpellObject(SpellChecker.TYPE_URL, offset + indexOfScheme, url.length(), url));
            }
        }
        return urls;
    }

    public static SpellObject[] foundMissings(SpellObject[] source, SpellObject[] target, int type)
    {
        List<SpellObject> missings = new ArrayList<SpellObject>();
        for (int i = 0; source != null && i < source.length; i++)
        {
            if (source[i].getType() != type)
            {
                continue;
            }
            boolean found = false;
            for (int j = 0; target != null && j < target.length; j++)
            {
                if (target[j].getType() != type)
                {
                    continue;
                }
                if (source[i].getValue().equals(target[j].getValue()))
                {
                    found = true;
                    break;
                }
            }

            if (found == false)
            {
                missings.add(source[i]);
            }
        }
        return missings.toArray(new SpellObject[missings.size()]);
    }

    public static SpellObject foundMissing(SpellObject[] source, SpellObject[] target, int type)
    {
        for (int i = 0; source != null && i < source.length; i++)
        {
            if (source[i].getType() != type)
            {
                continue;
            }
            boolean found = false;
            for (int j = 0; target != null && j < target.length; j++)
            {
                if (target[j].getType() != type)
                {
                    continue;
                }
                if (source[i].getValue().equals(target[j].getValue()))
                {
                    found = true;
                    break;
                }
            }

            if (found == false)
            {
                return source[i];
            }
        }
        return null;
    }

}
class SpellCheckerEvent
{
    private SpellObject[] spellObjects = null;
    private String value = null;
    
    SpellCheckerEvent(SpellObject[] spellObjects, String value)
    {
        this.spellObjects = spellObjects;
        this.value = value;
    } 
    public SpellObject[] getSpellObjects()
    {
        return this.spellObjects;
    }
    public String getValue()
    {
        return this.value;
    }
}

interface SpellCheckerListener
{
    void complete(SpellCheckerEvent e);
}

