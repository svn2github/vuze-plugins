package i18nAZ;

import i18nAZ.SpellChecker.SpellObject;
import i18nAZ.SpellChecker.Suggestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderURLImpl;
import org.json.simple.JSONArray;

import com.aelitis.azureus.util.JSONUtils;

public class TranslateManager
{
    final private static AtomicBoolean initialized = new AtomicBoolean();
    final private static List<String> availables = new ArrayList<String>();
    final private static Object resourceDownloaderAlternateMutex = new Object();
    private static ResourceDownloaderURLImpl resourceDownloader = null;
    private static AESemaphore semaphore = null;

    private synchronized static String download(String urlString)
    {
        final StringBuilder result = new StringBuilder();
        synchronized (TranslateManager.resourceDownloaderAlternateMutex)
        {
            TranslateManager.semaphore = new AESemaphore("TranslateManager");
            URL downloadURL = null;
            try
            {
                downloadURL = new URL(urlString);
            }
            catch (MalformedURLException e)
            {
            }
            TranslateManager.resourceDownloader = (ResourceDownloaderURLImpl) i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().create(downloadURL);

            TranslateManager.resourceDownloader.addListener(new ResourceDownloaderListener()
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
                    try
                    {
                        byte[] resultBytes = new byte[0];
                        byte[] buf = new byte[1024];

                        int bytesRead = 0;

                        while ((bytesRead = data.read(buf)) > 0)
                        {
                            byte[] temp = new byte[resultBytes.length + bytesRead];
                            System.arraycopy(resultBytes, 0, temp, 0, resultBytes.length);
                            System.arraycopy(buf, 0, temp, resultBytes.length, bytesRead);
                            resultBytes = temp;
                        }
                        result.append(new String(resultBytes, "UTF-8"));
                    }
                    catch (IOException e)
                    {
                    }
                    finally
                    {
                        try
                        {
                            data.close();
                        }
                        catch (IOException e)
                        {
                        }
                    }
                    if (TranslateManager.semaphore != null)
                    {
                        TranslateManager.semaphore.release();
                    }
                    return true;
                }

                
                public void failed(ResourceDownloader downloader, ResourceDownloaderException e)
                {
                    if (TranslateManager.semaphore != null)
                    {
                        TranslateManager.semaphore.release();
                    }
                }
            });
            TranslateManager.resourceDownloader.asyncDownload();
        }
        TranslateManager.semaphore.reserve();
        synchronized (TranslateManager.resourceDownloaderAlternateMutex)
        {
            TranslateManager.semaphore = null;
            boolean isCancelled = TranslateManager.resourceDownloader.isCancelled();
            TranslateManager.resourceDownloader = null;
            if (isCancelled == true)
            {
                return null;
            }
            return result.toString();
        }
    }

    public static void init()
    {
        synchronized (initialized)
        {
            if (initialized.get() == true)
            {
                return;
            }
            String urlString = "https://translate.google.com/translate_a/l?client=t&hl=en&alpha=1&ce=popo";
            String result = TranslateManager.download(urlString);
            while (result.startsWith("(") == true && result.length() > 1)
            {
                result = result.substring(1);
            }
            while (result.endsWith(")") == true && result.length() > 1)
            {
                result = result.substring(0, result.length() - 1);
            }
            Object object = JSONUtils.decodeJSON(result);
            if (object instanceof Map)
            {
                Map<?, ?> map = (Map<?, ?>) object;
                if (map.containsKey("tl") == true)
                {
                    object = map.get("tl");
                    if (object instanceof Map)
                    {
                        map = (Map<?, ?>) object;
                        for (Iterator<?> iterator = map.keySet().iterator(); iterator.hasNext();)
                        {
                            String key = (String) iterator.next();
                            if (TranslateManager.availables.contains(key) == false)
                            {
                                TranslateManager.availables.add(key);
                            }
                        }
                    }
                }
            }
            initialized.set(true);
        }
    }

    public static SpellObject[] translate(String value, Locale locale, String[] results)
    {
        if (results == null || results.length == 0)
        {
            return null;
        }
        TranslateManager.init();

        if (TranslateManager.availables.contains(locale.getLanguage()) == false)
        {
            return null;
        }
        String urlString = "https://translate.google.com/translate_a/t?";
        urlString += "client=t";
        urlString += "&ie=UTF-8";
        urlString += "&oe=UTF-8";
        urlString += "&multires=1";
        urlString += "&trs=1";
        urlString += "&otf=1";
        urlString += "&pc=0";
        urlString += "&oc=1";
        urlString += "&sc=2";
        urlString += "&ssel=0";
        urlString += "&tsel=0";
        urlString += "&hl=en";
        urlString += "&sl=en";
        urlString += "&v=2.0";
        urlString += "&tl=" + locale.getLanguage() + "";
        try
        {
            urlString += "&text=" + URLEncoder.encode(value.toLowerCase(Locale.US), "UTF8");
        }
        catch (UnsupportedEncodingException e1)
        {
        }
        String result = TranslateManager.download(urlString);
        String sentence = null;
        Object object = JSONUtils.decodeJSON(result);
        List<SpellObject> translatedObjects = new ArrayList<SpellObject>();
        if (object instanceof Map)
        {
            Map<?, ?> map = (Map<?, ?>) object;
            if (map.containsKey("value") == true)
            {
                object = map.get("value");
                if (object instanceof JSONArray)
                {
                    JSONArray jsonArray = (JSONArray) object;
                    JSONArray sentenceObjects = null;
                    // JSONArray dict = null;
                    // String lang = null;
                    JSONArray wordObjects = null;
                    JSONArray suggestionObjects = null;
                    int index = 0;
                    if (jsonArray.size() > index && jsonArray.get(index) instanceof JSONArray)
                    {
                        sentenceObjects = (JSONArray) jsonArray.get(index);
                        index++;
                    }
                    if (jsonArray.size() > index && jsonArray.get(index) instanceof JSONArray)
                    {
                        // dict = (JSONArray) array.get(index);
                        index++;
                    }
                    if (jsonArray.size() > index && jsonArray.get(index) instanceof String)
                    {
                        // lang = (String) array.get(index);
                        index++;
                    }
                    if (jsonArray.size() > index && jsonArray.get(index) instanceof JSONArray)
                    {
                        wordObjects = (JSONArray) jsonArray.get(index);
                        index++;
                    }
                    if (jsonArray.size() > index && jsonArray.get(index) instanceof JSONArray)
                    {
                        suggestionObjects = (JSONArray) jsonArray.get(index);
                        index++;
                    }
                    if (sentenceObjects != null && sentenceObjects.size() > 0 && sentenceObjects.get(0) instanceof JSONArray)
                    {
                        sentence = "";
                        for (int j = 0; j < sentenceObjects.size(); j++)
                        {
                            if (sentenceObjects.get(j) instanceof JSONArray)
                            {
                                JSONArray sentenceObject = (JSONArray) sentenceObjects.get(j);
                                if (sentenceObject.size() > 0 && sentenceObject.get(0) instanceof String)
                                {
                                    sentence += (String) sentenceObject.get(0);
                                }
                            }
                        }
                    }
                    int offset = 0;
                    String original = "";
                    for (int i = 0; wordObjects != null && i < wordObjects.size(); i++)
                    {
                        JSONArray wordObject = (JSONArray) wordObjects.get(i);
                        long indexTranslated = (Long) ((JSONArray) wordObject.get(1)).get(0);
                        String word = (String) wordObject.get(0);
                        int length = word.length();
                        int offsetBegin = 0;
                        int offsetEnd = 0;
                        List<String> suggestionStrings = new ArrayList<String>();
                        for (int j = 0; suggestionObjects != null && j < suggestionObjects.size(); j++)
                        {
                            JSONArray suggestionObject = (JSONArray) suggestionObjects.get(j);
                            long indexSuggestion = (Long) suggestionObject.get(1);
                            if (indexSuggestion == indexTranslated)
                            {
                                JSONArray offsets = (JSONArray) ((JSONArray) suggestionObject.get(suggestionObject.size() - 2)).get(0);
                                offsetBegin = (int) (long) (Long) offsets.get(0);
                                offsetEnd = (int) (long) (Long) offsets.get(1);

                                String currentBase = (String) suggestionObject.get(suggestionObject.size() - 1);
                                if (currentBase.equals("") == false)
                                {
                                    offsetBegin += original.length();
                                    offsetEnd += original.length();
                                    original += currentBase;
                                }
                                else
                                {
                                    offset += 1;
                                }

                                if (suggestionObject.size() > 4)
                                {
                                    JSONArray suggestionArray = (JSONArray) suggestionObject.get(2);
                                    for (int k = 0; suggestionArray != null && k < suggestionArray.size(); k++)
                                    {
                                        JSONArray subsuggestion = (JSONArray) suggestionArray.get(k);
                                        suggestionStrings.add((String) subsuggestion.get(0));
                                    }
                                }
                                break;
                            }
                        }
                        String oldword = value.substring(offsetBegin, offsetEnd);
                        if (value.length() > 1 && word.length() > 1 && sentence.length() > 1 && word.equals("\n") == false)
                        {
                            String[] originalLines = value.split("\\n");
                            String[] lines = sentence.split("\\n");
                            int indexLine = -1;
                            for (int j = 0; j < lines.length; j++)
                            {
                                if (lines[j].startsWith(word) == true)
                                {
                                    indexLine = j;
                                    break;
                                }
                            }
                            String valueToCheck = null;
                            if (indexLine != -1)
                            {
                                valueToCheck = originalLines[indexLine].split("[\\s+-]")[0];
                            }
                            else
                            {
                                valueToCheck = oldword;
                            }

                            boolean isUpperCase = Character.isUpperCase(valueToCheck.charAt(0));
                            for (int j = 1; j < valueToCheck.length(); j++)
                            {
                                if (Character.isUpperCase(valueToCheck.charAt(j)) == false)
                                {
                                    isUpperCase = false;
                                    break;
                                }
                            }
                            if (isUpperCase == true)
                            {
                                word = word.toUpperCase(locale);
                                sentence = sentence.substring(0, offset) + word + ((offset + length < sentence.length()) ? sentence.substring(offset + length) : "");
                                for (int j = 0; j < suggestionStrings.size(); j++)
                                {
                                    suggestionStrings.set(j, suggestionStrings.get(j).toUpperCase(locale));
                                }
                            }
                            boolean isFirstCase = Character.isUpperCase(valueToCheck.charAt(0)) && (indexLine != -1 || offset != 0);
                            for (int j = 1; isFirstCase == true && j < 2; j++)
                            {
                                if (Character.isLowerCase(valueToCheck.charAt(j)) == false)
                                {
                                    isFirstCase = false;
                                    break;
                                }
                            }
                            if (isFirstCase == true)
                            {
                                word = word.substring(0, 1).toUpperCase(locale) + word.substring(1).toLowerCase(locale);
                                sentence = sentence.substring(0, offset) + word + ((offset + length < sentence.length()) ? sentence.substring(offset + length) : "");
                                for (int j = 0; j < suggestionStrings.size(); j++)
                                {
                                    suggestionStrings.set(j, suggestionStrings.get(j).substring(0, 1).toUpperCase(locale) + suggestionStrings.get(j).substring(1).toLowerCase(locale));
                                }
                            }

                        }

                        List<Suggestion> suggestions = new ArrayList<Suggestion>();
                        for (int j = 0; j < suggestionStrings.size(); j++)
                        {
                            suggestions.add(new Suggestion(suggestionStrings.get(j)));
                        }
                        SpellObject translatedObject = new SpellObject(SpellChecker.TYPE_TRANSLATED_WORDS, offset, length, word, suggestions.toArray(new Suggestion[suggestions.size()]));
                        translatedObjects.add(translatedObject);

                        offset += length;
                    }
                }
            }
        }
        if (sentence != null)
        {
            results[0] = sentence;
            return translatedObjects.toArray(new SpellObject[translatedObjects.size()]);
        }
        return null;
    }

}
