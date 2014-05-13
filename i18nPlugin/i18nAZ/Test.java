/*
 * Test.java
 *
 * Created on March 21, 2014, 4:27 PM
 */
package i18nAZ;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Properties;

import org.gudy.azureus2.plugins.PluginManager;
/**
 * Test class.
 * 
 * @author Repris d'injustice
 */
public class Test
{
    public static void main(String[] args) throws MalformedURLException
    {
        System.out.println("Begin:");
        System.setProperty("user.dir",  "C:\\Program Files (x86)\\Vuze");
        Properties props = new Properties();
        File app_dir = new File("C:\\Program Files (x86)\\Vuze");
        File user_dir = new File("C:\\Users\\SI\\AppData\\Roaming\\Azureus");
        File doc_dir = new File("C:\\Program Files (x86)\\Vuze");
        props.put(PluginManager.PR_APP_DIRECTORY, app_dir.getAbsolutePath());
        props.put(PluginManager.PR_USER_DIRECTORY, user_dir.getAbsolutePath());
        props.put(PluginManager.PR_DOC_DIRECTORY, doc_dir.getAbsolutePath());
        try
        {
            PluginManager.startAzureus(PluginManager.UI_SWT, props);
        }
        catch (RuntimeException e)
        {
        }
    }
    public Test()
    {
    }
}
