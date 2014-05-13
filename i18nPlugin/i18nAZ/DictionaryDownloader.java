package i18nAZ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderURLImpl;
import org.gudy.azureus2.ui.swt.Utils;

import dk.dren.hunspell.Hunspell;
import dk.dren.hunspell.Hunspell.Dictionary;

public class DictionaryDownloader implements iTask
{
    final private static Task instance = new Task("DictionaryDownloader", 60, new DictionaryDownloader());

    final private static Vector<DictionaryDownloaderListener> listeners = new Vector<DictionaryDownloaderListener>();
    
    final private static Object resourceDownloaderAlternateMutex = new Object();
    private static ResourceDownloaderURLImpl resourceDownloader = null;
    private static AESemaphore semaphore = null;
    
    final private static List<Locale> queued = new ArrayList<Locale>();
    private static List<String[]> availables = null;
    final private static File dictionaryFolder = new File(i18nAZ.getPluginInterface().getPluginDirectoryName() + File.separator + "dictionaries" + File.separator);

    public static void add(Locale locale)
    {
        if (DictionaryDownloader.isStarted() == false)
        {
            DictionaryDownloader.start();
        }
        synchronized (DictionaryDownloader.queued)
        {
            if (DictionaryDownloader.queued.contains(locale) == false)
            {
                DictionaryDownloader.queued.add(locale);
            }
        }
        DictionaryDownloader.signal();
    }

    public static synchronized void addListener(DictionaryDownloaderListener listener)
    {
        if (!DictionaryDownloader.listeners.contains(listener))
        {
            DictionaryDownloader.listeners.addElement(listener);
        }
    }

    public static synchronized void deleteListener(DictionaryDownloaderListener listener)
    {
        DictionaryDownloader.listeners.removeElement(listener);
    }

    public static synchronized void deleteListeners()
    {
        if (DictionaryDownloader.listeners != null)
        {
            DictionaryDownloader.listeners.removeAllElements();
        }
    }

    public static void notifyListeners(DictionaryDownloaderEvent event)
    {
        Object[] localListeners = null;
        synchronized (DictionaryDownloader.listeners)
        {
            localListeners = DictionaryDownloader.listeners.toArray();
        }

        for (int i = localListeners.length - 1; i >= 0; i--)
        {
            ((DictionaryDownloaderListener) localListeners[i]).complete(event);
        }
    }

    
    public void check()
    {
        if (availables == null)
        {
            availables = new ArrayList<String[]>();
            Properties availablesProperties = Util.getProperties(i18nAZ.getPluginInterface().getPluginClassLoader().getResource("availableDictionaries.properties"));

            if (availablesProperties != null)
            {
                for (Enumeration<?> enumeration = availablesProperties.propertyNames(); enumeration.hasMoreElements();)
                {
                    String key = (String) enumeration.nextElement();
                    String[] available = availablesProperties.getProperty(key).split("\\t");
                    if (available.length == 5)
                    {
                        availables.add(available);
                    }
                }
            }
        }

        while (true)
        {
            Locale locale = null;
            synchronized (DictionaryDownloader.queued)
            {
                if (DictionaryDownloader.queued.size() == 0)
                {
                    return;
                }
                locale = DictionaryDownloader.queued.get(0);
            }

            List<String> languageTags = getLanguageTag(locale);
            String[] available = null;
            for (int i = 0; i < languageTags.size(); i++)
            {
                for (int j = 0; j < availables.size(); j++)
                {
                    String availablelanguageTag = availables.get(j)[0] + ((availables.get(j)[1].equals("") == true) ? "" : "_" + availables.get(j)[1]);
                    if (availablelanguageTag.equals(languageTags.get(i)) == true)
                    {
                        available = availables.get(j);
                        break;
                    }
                }
                if (available != null)
                {
                    break;
                }
            }

            boolean processed = false;
            while (true)
            {
                Dictionary dictionary = null;
                String availablelanguageTag = null;
                if (available != null)
                {
                    availablelanguageTag = available[0] + ((available[1].equals("") == true) ? "" : "_" + available[1]);;
                    dictionary = getDictionaries(Util.getLanguageTag(locale).replace('-', '_'));
                }
                if (dictionary != null || processed == true || availablelanguageTag == null)
                {
                    synchronized (DictionaryDownloader.queued)
                    {
                        DictionaryDownloader.queued.remove(locale);
                    }
                    if (dictionary != null)
                    {
                        DictionaryDownloader.notifyListeners(new DictionaryDownloaderEvent(dictionary, locale));
                    }
                    break;
                }
                final File destFile = new File(dictionaryFolder + File.separator + Util.getLanguageTag(locale).replace('-', '_') + ".zip");
                synchronized (DictionaryDownloader.resourceDownloaderAlternateMutex)
                {
                    DictionaryDownloader.semaphore = new AESemaphore("DictionaryDownloader");

                    URL downloadURL = null;
                    try
                    {
                        downloadURL = new URL("http://ftp.nluug.nl/office/openoffice/contrib/dictionaries/" + available[4]);
                    }
                    catch (MalformedURLException e1)
                    {
                    }
                    DictionaryDownloader.resourceDownloader = (ResourceDownloaderURLImpl) i18nAZ.getPluginInterface().getUtilities().getResourceDownloaderFactory().create(downloadURL);

                    DictionaryDownloader.resourceDownloader.addListener(new ResourceDownloaderListener()
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
                            DictionaryDownloader.semaphore.release();
                            return false;
                        }

                        
                        public void failed(ResourceDownloader downloader, ResourceDownloaderException e)
                        {
                            DictionaryDownloader.semaphore.release();
                        }
                    });
                    DictionaryDownloader.resourceDownloader.asyncDownload();
                }
                DictionaryDownloader.semaphore.reserve();
                synchronized (DictionaryDownloader.resourceDownloaderAlternateMutex)
                {
                    ArchiveFile archiveFile = null;
                    try
                    {
                        archiveFile = new ArchiveFile(destFile);
                    }
                    catch (IOException e)
                    {
                    }
                    if (archiveFile != null)
                    {
                        for (Enumeration<JarEntry> entries = archiveFile.entries(); entries.hasMoreElements();)
                        {
                            JarEntry entry = entries.nextElement();
                            if (entry.isDirectory() == true)
                            {
                                continue;
                            }
                            String fileName = Path.getFilenameWithoutExtension(entry.getName());
                            if (fileName.startsWith(availablelanguageTag) == false)
                            {
                                continue;
                            }
                            String extension = Path.getExtension(entry.getName());
                            if (extension.equalsIgnoreCase(".dic") == false && extension.equalsIgnoreCase(".aff") == false)
                            {
                                continue;
                            }
                            InputStream data = null;
                            try
                            {
                                data = archiveFile.getInputStream(entry);
                            }
                            catch (IOException e)
                            {
                            }
                            if (data == null)
                            {
                                continue;
                            }
                            OutputStream output = null;
                            File file = new File(dictionaryFolder + File.separator + Util.getLanguageTag(locale).replace('-', '_') + extension);
                            file.getParentFile().mkdirs();
                            try
                            {
                                output = new FileOutputStream(file);

                                byte[] buf = new byte[1024];

                                int bytesRead;

                                while ((bytesRead = data.read(buf)) > 0)
                                {
                                    output.write(buf, 0, bytesRead);
                                }
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
                        }
                        try
                        {
                            archiveFile.close();
                        }
                        catch (IOException e)
                        {
                        }
                    }
                    else
                    {
                        destFile.delete();
                    }
                    DictionaryDownloader.semaphore = null;
                    boolean isCancelled = DictionaryDownloader.resourceDownloader.isCancelled();
                    DictionaryDownloader.resourceDownloader = null;
                    if (isCancelled == true)
                    {
                        break;
                    }
                }
                processed = true;
            }
        }
    }

    private static Dictionary getDictionaries(String languageTag)
    {
        Dictionary dictionary = null;
        try
        {
            dictionary = Hunspell.getInstance().getDictionary(dictionaryFolder + File.separator + languageTag);
        }
        catch (FileNotFoundException e)
        {
        }
        catch (UnsupportedEncodingException e)
        {
        }
        catch (UnsatisfiedLinkError e)
        {
        }
        catch (UnsupportedOperationException e)
        {
        }
        return dictionary;
    }

    private static List<String> getLanguageTag(Locale locale)
    {
        List<String> languageTags = new ArrayList<String>();
        if (locale != null && (locale.equals(Util.EMPTY_LOCALE) == false))
        {
            String language = "";
            if (locale.getLanguage().equals("") == false)
            {
                language = locale.getLanguage();
                if (locale.getCountry().equals("") == false)
                {
                    languageTags.add(language + "_" + locale.getCountry());
                }
                else
                {
                    Locale[] availableLocales = Locale.getAvailableLocales();
                    for (int j = 0; j < availableLocales.length; j++)
                    {
                        if (availableLocales[j].getCountry().equals("") == false && availableLocales[j].getLanguage().equals(locale.getLanguage()) == true)
                        {

                            languageTags.add(language + "_" + availableLocales[j].getCountry());
                            break;
                        }
                    }
                }
                if (languageTags.size() == 0)
                {
                    languageTags.add("ia");
                }
            }
        }
        else
        {
            languageTags.add("ia");
        }
        return languageTags;
    }

    
    public void onStart()
    {
        DictionaryDownloader.addListener(new DictionaryDownloaderListener()
        {
            
            public void complete(DictionaryDownloaderEvent e)
            {
                SpellChecker.resetDictionary(e.getLocale());
                Utils.execSWTThread(new AERunnable()
                {
                    
                    public void runSupport()
                    {
                        i18nAZ.viewInstance.updateSpellCheckerButton();
                        i18nAZ.viewInstance.updateStyledTexts();
                    }
                });
            }
        });
    }

    
    public void onStop(StopEvent e)
    {
        synchronized (DictionaryDownloader.resourceDownloaderAlternateMutex)
        {
            if (DictionaryDownloader.resourceDownloader != null)
            {
                DictionaryDownloader.resourceDownloader.cancel();
                DictionaryDownloader.semaphore.reserve();
            }
        }
        DictionaryDownloader.deleteListeners();
    }

    public static void signal()
    {
        DictionaryDownloader.instance.signal();
    }

    public static void start()
    {
        DictionaryDownloader.instance.start();
    }

    public static void stop()
    {
        DictionaryDownloader.instance.stop();
    }

    public static boolean isStarted()
    {
        return DictionaryDownloader.instance.isStarted();
    }
}

class DictionaryDownloaderEvent
{
    private Dictionary dictionary = null;
    private Locale locale = null;

    public DictionaryDownloaderEvent(Dictionary dictionary, Locale locale)
    {
        this.dictionary = dictionary;
        this.locale = locale;
    }

    public Dictionary getDictionary()
    {
        return this.dictionary;
    }

    public Locale getLocale()
    {
        return this.locale;
    }
}

interface DictionaryDownloaderListener
{
    void complete(DictionaryDownloaderEvent e);
}
