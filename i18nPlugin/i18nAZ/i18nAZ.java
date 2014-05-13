/*
 * i18nAZ.java
 *
 * Created on February 22, 2004, 5:43 PM
 */

package i18nAZ;

import i18nAZ.FilterManager.PrebuildItem;
import i18nAZ.LocalizablePluginManager.LocalizablePlugin;
import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;
import i18nAZ.TargetLocaleManager.TargetLocale;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.widgets.Item;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * ResourceBundle Comparer/Editor: Plugin into Vuze
 * 
 * @author TuxPaper
 * @author Repris d'injustice
 */
public class i18nAZ implements UnloadablePlugin
{
    static UISWTInstance swtInstance = null;
    static View viewInstance = null;
    static File vuzeDirectory = null;
    static URL mergedInternalFile = null;
    private static PluginInterface currentPluginInterface = null;
    static LoggerChannel loggerChannel = null;

    static PluginInterface getPluginInterface()
    {
        return i18nAZ.currentPluginInterface;
    }

    static String getLocalisedMessageText(String key)
    {
        return i18nAZ.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(key);
    }

    static String getLocalisedMessageText(String key, String param)
    {
        return i18nAZ.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(key, new String[] { param });
    }

    static String getLocalisedMessageText(String key, String[] params)
    {
        return i18nAZ.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(key, params);
    }

    
    public void initialize(final PluginInterface pluginInterface)
    {
        i18nAZ.currentPluginInterface = pluginInterface;

        i18nAZ.vuzeDirectory = new File(i18nAZ.getPluginInterface().getUtilities().getAzureusProgramDir());
        i18nAZ.mergedInternalFile = Path.getUrl(i18nAZ.vuzeDirectory + File.separator + Util.getBundleFileName(Locale.getDefault()));

        final BasicPluginViewModel basicPluginViewModel = i18nAZ.getPluginInterface().getUIManager().createBasicPluginViewModel("i18nAZ.SideBar.Title");
        basicPluginViewModel.getActivity().setVisible(false);
        basicPluginViewModel.getProgress().setVisible(false);
        basicPluginViewModel.getStatus().setVisible(false);

        i18nAZ.loggerChannel = i18nAZ.getPluginInterface().getLogger().getChannel("i18nAZ");
        i18nAZ.loggerChannel.setDiagnostic();
        i18nAZ.loggerChannel.setForce(true);
        basicPluginViewModel.attachLoggerChannel(i18nAZ.loggerChannel);

        i18nAZ.log("i18nAZ Startup");
        LocaleProperties localeProperties = Util.loadLocaleProperties(Locale.getDefault(), i18nAZ.mergedInternalFile);
        if (localeProperties == null)
        {
            Path.getFile(i18nAZ.mergedInternalFile).delete();
        }
        else if (localeProperties != null && localeProperties.IsLoaded() == true)
        {
            localeProperties.clear();
            i18nAZ.getPluginInterface().getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle(LocalizablePluginManager.DEFAULT_NAME);
        }

        LocalizablePluginManager.PLUGIN_CORE_DISPLAY_NAME = i18nAZ.getLocalisedMessageText("i18nAZ.Labels.DefaultPlugin");
        LocalizablePluginManager.LOCALIZABLE_PLUGIN_DIR = i18nAZ.getPluginInterface().getPluginDirectoryName().toString() + "\\internat\\";

        i18nAZ.log("plugin dir: " + i18nAZ.getPluginInterface().getPluginDirectoryName().toString());
        Class<?> c = null;
        try
        {
            c = this.getClass().getClassLoader().loadClass("com.sun.jna.Library");
            if (c != null)
            {
                i18nAZ.log("load class: com.sun.jna.Library");
            }
        }
        catch (Throwable e)
        {
            i18nAZ.loggerChannel.log(e);
        }
        try
        {
            c = this.getClass().getClassLoader().loadClass("dk.dren.hunspell.Hunspell");
            if (c != null)
            {
                i18nAZ.log("load class: dk.dren.hunspell.Hunspell");
            }
        }
        catch (Throwable e)
        {
            i18nAZ.loggerChannel.log(e);
        }
        try
        {
            i18nAZ.currentPluginInterface.getUIManager().addUIListener(new UIManagerListener()
            {
                
                public void UIAttached(UIInstance instance)
                {
                    if (instance instanceof UISWTInstance)
                    {
                        i18nAZ.swtInstance = (UISWTInstance) instance;
                        i18nAZ.viewInstance = new View();
                        i18nAZ.swtInstance.addView(UISWTInstance.VIEW_MAIN, View.VIEWID, i18nAZ.viewInstance);
                    }
                }

                
                public void UIDetached(UIInstance instance)
                {
                    i18nAZ.swtInstance = null;
                    i18nAZ.viewInstance = null;
                }
            });
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        BasicPluginConfigModel basicPluginConfigModel = pluginInterface.getUIManager().createBasicPluginConfigModel("plugins", "plugins.i18nAZ");

        if (COConfigurationManager.hasParameter("i18nAZ.AllowRemotePlugin", true) == false)
        {
            COConfigurationManager.setParameter("i18nAZ.AllowRemotePlugin", true);
        }

        final BooleanParameter allowRemotePluginBooleanParameter = basicPluginConfigModel.addBooleanParameter2("AllowRemotePlugin", "i18nAZ.Labels.AllowRemotePlugin", COConfigurationManager.getBooleanParameter("i18nAZ.AllowRemotePlugin"));
        allowRemotePluginBooleanParameter.addConfigParameterListener(new ConfigParameterListener()
        {
            
            public void configParameterChanged(ConfigParameter param)
            {
                COConfigurationManager.setParameter("i18nAZ.AllowRemotePlugin", allowRemotePluginBooleanParameter.getValue());
                if (i18nAZ.viewInstance != null && i18nAZ.viewInstance.isCreated() == true)
                {
                    if (COConfigurationManager.getBooleanParameter("i18nAZ.AllowRemotePlugin") == true)
                    {
                        NotInstalledPluginDownloader.start();
                    }
                    else
                    {
                        NotInstalledPluginDownloader.stop();
                    }
                }
            }
        });
        basicPluginConfigModel.createGroup("i18nAZ.Groups.Plugins", new Parameter[] { allowRemotePluginBooleanParameter, });

        final ActionParameter resetDictionaryActionParameter = basicPluginConfigModel.addActionParameter2("i18nAZ.Labels.ResetDictionary", "i18nAZ.Buttons.ResetDictionary");
        resetDictionaryActionParameter.addConfigParameterListener(new ConfigParameterListener()
        {
            
            public void configParameterChanged(ConfigParameter param)
            {
                SpellChecker.reset();
                if (i18nAZ.viewInstance != null && i18nAZ.viewInstance.isCreated() == true)
                {
                    List<Item> items = Util.getAllItems(TreeTableManager.getCurrent(), null);
                    for (int i = 0; i < items.size(); i++)
                    {
                        Item item = items.get(i);
                        PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                        if (prebuildItem.isExist() == false)
                        {
                            continue;
                        }
                        prebuildItem.resetObjectManagers();
                    }
                }
            }
        });
        basicPluginConfigModel.createGroup("i18nAZ.Groups.Spellchecking", new Parameter[] { resetDictionaryActionParameter });
    }

    static void log(String data)
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss SSS");
        String formattedDate = sdf.format(date);
        i18nAZ.loggerChannel.log(LoggerChannel.LT_INFORMATION, formattedDate + ">" + data);
    }

    static void logWarning(String data)
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss SSS");
        String formattedDate = sdf.format(date);
        i18nAZ.loggerChannel.log(LoggerChannel.LT_WARNING, formattedDate + ">" + data);
    }

    static void logError(String data)
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");
        String formattedDate = sdf.format(date);
        i18nAZ.loggerChannel.log(LoggerChannel.LT_ERROR, formattedDate + ">" + data);
    }

    static String mergeBundleFile()
    {
        String result = "";
        LocalizablePlugin[] localizablePlugins = LocalizablePluginManager.toArray();
        TargetLocale[] targetLocales = TargetLocaleManager.toArray();
        for (int i = 1; i < targetLocales.length; i++)
        {
            if (targetLocales[i].getLocale().equals(Locale.getDefault()) == true)
            {
                // merge plugins properties
                LocaleProperties mergedlocaleProperties = new LocaleProperties(targetLocales[i].getLocale());
                for (int j = 0; j < localizablePlugins.length; j++)
                {
                    if (localizablePlugins[j].isNotInstalled() == true)
                    {
                        continue;
                    }
                    LangFileObject[] langFileObjects = localizablePlugins[j].toArray();
                    for (int k = 0; k < langFileObjects.length; k++)
                    {
                        URL internalPath = langFileObjects[k].getInternalPath(targetLocales[i]);
                        LocaleProperties localeProperties = Util.getLocaleProperties(targetLocales[i].getLocale(), internalPath);
                        if (localeProperties != null && localeProperties.IsLoaded() == true)
                        {
                            for (Enumeration<?> enumeration = localeProperties.propertyNames(); enumeration.hasMoreElements();)       
                            {
                                String key = (String) enumeration.nextElement();
                                mergedlocaleProperties.put(key, localeProperties.getProperty(key));
                            }
                        }

                    }
                }

                // save
                result = Util.saveLocaleProperties(mergedlocaleProperties, mergedInternalFile);
                break;
            }
        }
        return result;
    }

    
    public void unload() throws PluginException
    {
        if (i18nAZ.swtInstance == null || i18nAZ.viewInstance == null)
        {
            return;
        }
        i18nAZ.swtInstance.removeViews(UISWTInstance.VIEW_MAIN, View.VIEWID);
        i18nAZ.viewInstance = null;
    }
}