/*
 * i18nAZ.java
 *
 * Created on February 22, 2004, 5:43 PM
 */

package i18nAZ;

import java.io.File;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
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
    
    @Override
    public void initialize(final PluginInterface pluginInterface)
    {
        File vuzeBundleFile = new File(pluginInterface.getUtilities().getAzureusProgramDir() + File.separator + BundleObject.DEFAULT_NAME + BundleObject.EXTENSION);
        File tempFile = new File(vuzeBundleFile.getAbsolutePath() + ".temp");
        
        CommentedProperties localeProperties = Util.getLocaleProperties(vuzeBundleFile);
        CommentedProperties tempProperties = Util.getLocaleProperties(tempFile);
        
        if (localeProperties == null && tempProperties != null)
        {            
            if(Util.saveLocaleProperties(tempProperties, vuzeBundleFile) != null)
            {
                localeProperties = tempProperties;                
            }
            else
            {
                tempProperties.clear();  
                tempProperties = null;   
            }
        }
        if (localeProperties != null && tempProperties != null)
        {            
            tempFile.delete();
            tempProperties = null;
        }
        
        if (localeProperties != null && localeProperties.IsLoaded() == true)
        {
            localeProperties.clear();
            pluginInterface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle(BundleObject.DEFAULT_NAME);            
        }
        
        
        try
        {
            pluginInterface.getUIManager().addUIListener(new UIManagerListener()
            {
                @Override
                public void UIAttached(UIInstance instance)
                {
                    if (instance instanceof UISWTInstance)
                    {
                        i18nAZ.swtInstance = (UISWTInstance) instance;
                        i18nAZ.viewInstance = new View(pluginInterface);                
                        i18nAZ.swtInstance.addView(UISWTInstance.VIEW_MAIN,  View.VIEWID, i18nAZ.viewInstance);
                    }
                }

                @Override
                public void UIDetached(UIInstance instance)
                {
                    i18nAZ.swtInstance = null;
                }
            });
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    @Override
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