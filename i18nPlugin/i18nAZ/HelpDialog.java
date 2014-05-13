/*
 * AddLanguageDialog.java
 *
 * Created on February 23, 2004, 4:09 PM
 */
package i18nAZ;

import i18nAZ.LocalizablePluginManager.LocalizablePlugin;
import i18nAZ.SpellChecker.SpellObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
/**
 * ImportDialog class
 * 
 * @author Repris d'injustice
 */
class HelpDialog
{
    private static String helpText = null;
    private static StyleRange[] styleRanges = null;
    static void show()
    {
     
        if(helpText == null)
        {
            String helpFullPath = "readme/" + Util.getBundleFileName("readme", Locale.getDefault(), ".txt");
            InputStream stream = i18nAZ.getPluginInterface().getPluginClassLoader().getResourceAsStream(helpFullPath);
            if (stream == null)
            {
                helpFullPath = "readme/readme.txt";
                stream = i18nAZ.getPluginInterface().getPluginClassLoader().getResourceAsStream(helpFullPath);
            }
            if (stream == null)
            {
                helpText = "Error loading resource: " + helpFullPath;
            }
            else
            {
                try
                {
                    helpText = Util.readInputStreamAsString(stream, 65535, "utf8");
                    stream.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        
        // create shell
        Shell shell = new Shell(SWTSkinFactory.getInstance().getShell().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setImages(SWTSkinFactory.getInstance().getShell().getShell().getImages());
        shell.setLayout(new FillLayout());
        shell.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.Help"));
    
        final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
       
        TabItem tabItem = null;
        Composite composite = null;
        
        tabItem = new TabItem(tabFolder, SWT.NULL);
        tabItem.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Tabs.About"));

        
        // add composite
        composite = Util.getNewComposite(tabFolder, SWT.NULL, 2, SWT.FILL, SWT.FILL, true, true);
        tabItem.setControl(composite);
        
        // add banner
        Label bannerLabel = new Label(composite, SWT.NULL);
        Util.setGridData(bannerLabel, SWT.CENTER, SWT.CENTER, false, false);
        bannerLabel.setImage(i18nAZ.viewInstance.getImageLoader().getImage("i18nAZ.image.banner"));
        
        // add help text
        StyledText helpStyledtext = new StyledText(composite, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
        Util.setGridData(helpStyledtext, SWT.FILL, SWT.FILL, true, true);
        helpStyledtext.setEditable(false);
        helpStyledtext.setBackground(shell.getBackground());
         Util.addLinkManager(helpStyledtext, true, true);        
        
        if(styleRanges != null)
        {
            helpStyledtext.setText(HelpDialog.helpText);
            helpStyledtext.setStyleRanges( HelpDialog.styleRanges);
        }
        else
        {        
            // find bold tags
            List<Object[]> bolds = Util.getTags(0, HelpDialog.helpText, "b");
            HelpDialog.helpText = (String) bolds.get(0)[0];
            
            // find bold tags
            List<Object[]> underlines = Util.getTags(0, HelpDialog.helpText, "u");            
            HelpDialog.helpText = (String) underlines.get(0)[0];
            
            for (int i = 1; i < underlines.size(); i++)
            {
                for (int j = 1; j < bolds.size(); j++)
                {
                    if((Integer) bolds.get(j)[0] > (Integer) underlines.get(i)[0])
                    {
                        bolds.get(j)[0] = (Integer) bolds.get(j)[0] - 7;
                    }                    
                }
            }            
           
            // find urls
            List<SpellObject> urls = SpellChecker.getUrls(0, helpText);
            
            // set Help Text             
            helpStyledtext.setText(HelpDialog.helpText);
             
            // set bold styles 
            for (int i = 1; i < bolds.size(); i++)
            {
                StyleRange styleRange = new StyleRange((Integer) bolds.get(i)[0], (Integer) bolds.get(i)[1], new Color(Display.getCurrent(), 0, 0, 0), null);
                styleRange.font = new Font(null, helpStyledtext.getFont().getFontData()[0].getName(), 10, SWT.BOLD);
                helpStyledtext.setStyleRange(styleRange);
            }            
       
            // set underline styles
            for (int i = 1; i < underlines.size(); i++)
            {
                StyleRange styleRange = new StyleRange((Integer) underlines.get(i)[0], (Integer) underlines.get(i)[1], new Color(Display.getCurrent(), 0, 0, 0), null);
                styleRange.font = new Font(null, helpStyledtext.getFont().getFontData()[0].getName(), helpStyledtext.getFont().getFontData()[0].getHeight(), SWT.NORMAL);
                styleRange.underline = true;
                helpStyledtext.setStyleRange(styleRange);
            }
            
            // set url styles 
            for (int i = 0; i < urls.size(); i++)
            {
                StyleRange styleRange = new StyleRange(urls.get(i).getOffset(), urls.get(i).getLength(), new Color(Display.getCurrent(), 0, 0, 255), null);
                styleRange.font = new Font(null, helpStyledtext.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
                styleRange.underline = true;
                
                styleRange.underlineStyle = SWT.UNDERLINE_LINK;
                
                styleRange.data = urls.get(i);
                helpStyledtext.setStyleRange(styleRange);
            }

            HelpDialog.styleRanges = helpStyledtext.getStyleRanges(true);            
        }        
        
        tabItem = new TabItem(tabFolder, SWT.NULL);
        tabItem.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Tabs.OtherPlugins"));
        
        // add composite
        composite = Util.getNewComposite(tabFolder, SWT.NULL, SWT.FILL, SWT.FILL, true, true);
        tabItem.setControl(composite);
       
        // add banner
        Label label = new Label(composite, SWT.NULL);
        label.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Labels.OtherPlugins"));
        Util.setGridData(label, SWT.FILL, SWT.FILL, false, false);
        
        // add list
        
        org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(composite, SWT.BORDER);
        Util.setGridData(list, SWT.FILL, SWT.FILL, true, true);
        
        LocalizablePlugin[] localizablePlugins = LocalizablePluginManager.getNotLocalizables();
        for(int i = 0; i < localizablePlugins.length; i++)
        {
            list.add(localizablePlugins[i].getDisplayName());
        }
        
        // set shell size
        shell.setSize(640, 480);
    
        // open shell and center it to parent
        Util.openShell(SWTSkinFactory.getInstance().getShell().getShell(), shell);
    }
}
