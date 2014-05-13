/*
 * LocalizablePluginInterface.java
 *
 * Created on April 28, 2014, 4:25
 */
package i18nAZ;

import i18nAZ.LocalizablePluginManager.LocalizablePlugin;
import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;
import i18nAZ.TargetLocaleManager.TargetLocale;

/**
 * LocalizablePluginInterface
 * 
 * @author Repris d'injustice
 */
public class LocalePropertiesLoader implements iTask
{
    final private static Task instance = new Task("LocalePropertiesLoader", 5, (iTask) new LocalePropertiesLoader());    
   
    
    public void check()
    {
        TargetLocale[] targetLocales = TargetLocaleManager.toArray();
        for (int i = 0; i < targetLocales.length; i++)
        {
            LocalizablePlugin[] localizablePlugins = LocalizablePluginManager.toArray();            
            for (int j = 0; j <localizablePlugins.length; j++)
            {
                LangFileObject[] langFileObjects = localizablePlugins[j].toArray();
                for (int k = 0; k < langFileObjects.length; k++)
                {                    
                    langFileObjects[k].load(targetLocales[i]);                    
                }
            }
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
        LocalePropertiesLoader.instance.signal();
    }

    public static void start()
    {
        LocalePropertiesLoader.instance.start();
    }

    public static void stop()
    {
        LocalePropertiesLoader.instance.stop();
    }

    public static boolean isStarted()
    {
        return  LocalePropertiesLoader.instance.isStarted();
    }
}
