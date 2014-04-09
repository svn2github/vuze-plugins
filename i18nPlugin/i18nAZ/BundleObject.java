/*
 * BundleObject.java
 *
 * Created on March 21, 2014, 4:27 PM
 */

package i18nAZ;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * BundleObject class
 *   
 * @author Repris d'injustice
 */
class BundleObject
{
    static final String DEFAULT_NAME = "MessagesBundle";
    static final String EXTENSION = ".properties";
    
    private PluginInterface pluginInterface = null;
    private String pluginName = "";
    private String jarFolder = "";
    private String path = "";
    private String name = "";    
    private File file = null;
    private URL url = null;
    private boolean valid = false;    
    BundleObject( String path)
    {
        this.jarFolder = "";
        this.path = path;
        this.name = DEFAULT_NAME;
    }
    BundleObject(String jarFolder, String path)
    {
        this.jarFolder = jarFolder;
        this.path = path;
        this.name = DEFAULT_NAME;
    }
    BundleObject(String jarFolder, String path, String name)
    {
        this.jarFolder = jarFolder;
        this.path = path;
        this.name = path;
    }
    BundleObject(PluginInterface pluginInterface)
    {            
        String TempPluginName = "Azureus2";
        String TempLangFile = View.AZUREUS_LANG_FILE;
        if(pluginInterface != null)
        {
            Properties PluginProperties = pluginInterface.getPluginProperties();
            if (PluginProperties.containsKey("plugin.id") == true && PluginProperties.containsKey("plugin.version") == true && PluginProperties.containsKey("plugin.langfile") == true)
            {
                TempPluginName= PluginProperties.getProperty("plugin.id") + "_" + PluginProperties.getProperty("plugin.version");
                TempLangFile = PluginProperties.getProperty("plugin.langfile");
            }
            else
            {
                return;
            }
        }
      
        int LastDotIndex = TempLangFile.lastIndexOf('.');
        if (LastDotIndex == -1)
        {
            this.jarFolder = "";
            this.name = TempLangFile;
        }
        else
        {
            this.jarFolder = TempLangFile.substring(0, LastDotIndex).replace('.', '/');
            this.name = TempLangFile.substring(LastDotIndex + 1);
        }

        String ResourceName = this.jarFolder + "/" + this.name + BundleObject.EXTENSION;
        URL ResourceURL = null;           
        if(pluginInterface == null)            
        {            
            ResourceURL = i18nAZ.viewInstance.getPluginInterface().getClass().getClassLoader().getResource(ResourceName);            
            if (ResourceURL == null)
            {
                ResourceURL = i18nAZ.viewInstance.getPluginInterface().getPluginClassLoader().getResource(ResourceName);
            }
            if (ResourceURL == null)
            {
                ResourceURL = i18nAZ.viewInstance.getClass().getClassLoader().getResource(ResourceName);
            }
        }
        else
        {
            Enumeration<URL> entries = null;
            try
            {
                entries = pluginInterface.getPluginClassLoader().getResources(ResourceName);
            }
            catch (IOException e)
            {
             
            }             
            while (ResourceURL == null && entries != null && entries.hasMoreElements())
            {
                URL URLEntry = entries.nextElement();
                if (URLEntry.getProtocol().equals("file") == false)
                {
                    ResourceURL = URLEntry;
                    break;
                }
            }
            while (ResourceURL == null && entries != null && entries.hasMoreElements())
            {
                URL URLEntry = entries.nextElement();
                if (URLEntry.getProtocol().equals("file") == true)
                {
                    ResourceURL = URLEntry;
                    break;
                }
            }
        }
        if (ResourceURL != null)
        {
            this.path = ResourceURL.toString();
            this.path = this.path.substring(0, this.path.length() - this.name.length()- BundleObject.EXTENSION.length());
            try
            {
                this.url = new URL(this.path);
            }
            catch (MalformedURLException e)
            {
            }
            if (this.url != null)
            {
                URLConnection Connection = null;
                try
                {
                    Connection = this.url.openConnection();
                }
                catch (IOException e)
                {
                }
                if (Connection instanceof JarURLConnection)
                {
                    JarURLConnection Jarconnection = (JarURLConnection) Connection;
                    this.file = Util.getFileFromUrl(Jarconnection.getJarFileURL());
                }
                else if (this.url.getProtocol().equals("file") == true)
                {   
                    this.file = Util.getFileFromUrl(this.url);
                }
                this.pluginInterface = pluginInterface;
                this.pluginName = TempPluginName;   
            }
            this.valid = true;
        }
        else
        {
            this.jarFolder = "";
            this.name = "";
        }       
    }
    boolean isValid()
    {
        return this.valid;
    }
    PluginInterface getPluginInterface()
    {
        return this.pluginInterface;
    }
    URL getUrl()
    {
        return this.url;
    }
    File getFile()
    {
       return this.file;
    }
    String getPluginName()
    {
        return this.pluginName;
    }
    String getName()
    {
        return this.name;
    }
    String getPath()
    {
        return this.path;
    }
    String getJarFolder()
    {
        return this.jarFolder;
    }
}
