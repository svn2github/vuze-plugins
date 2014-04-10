/*
 * AddLanguageDialog.java
 *
 * Created on February 23, 2004, 4:09 PM
 */

package i18nAZ;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * Prompts the user for a Language to add to the editor. If successful,
 * localSelected will not be null.
 * 
 * @author TuxPaper
 * @author Repris d'injustice
 */
class AddLanguageDialog
{    
    private Table table;
    private Label label;
    
    private String defaultPath;
    
    private ArrayList<Object[]> localesFounded;    
    private Set<Locale> localeAlsoSelected;
    Locale[] localesSelected = null;
    private BundleObject localBundleObject = null;
   
    private boolean showOnlyExisting;

    AddLanguageDialog(Shell owner)
    {
        this.localeAlsoSelected = new HashSet<Locale>();        
        for (int i = 0; i < i18nAZ.viewInstance.localesProperties.size(); i++)                
        {
            this.localeAlsoSelected.add(i18nAZ.viewInstance.localesProperties.get(i).locale);
        }
        
        this.localBundleObject = i18nAZ.viewInstance.getCurrentBundleObject();
        this.defaultPath = i18nAZ.viewInstance.getDefaultPath();

        final Shell shell = new Shell(owner, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setImages(owner.getImages());
        shell.setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Titles.AddLanguage"));
        
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        shell.setLayout(layout);

        // Label
        this.label = new Label(shell, SWT.NONE);
        this.label.setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Labels.AddLanguage"));
        this.label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false,false,2, 1));

        // Folders Label
        final Text FoldersLabel = new Text(shell, SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.heightHint = 19;
        FoldersLabel.setLayoutData(gridData);
        String sText = this.localBundleObject.getPath() + "\n";

        FoldersLabel.setText(sText);
        FoldersLabel.setEditable(false);

        // Browse Button
        Button BrowseButton = new Button(shell, SWT.PUSH);
        gridData = new GridData(SWT.RIGHT, SWT.TOP, false, false);
        gridData.widthHint = 100;
        gridData.heightHint = 23;
        BrowseButton.setLayoutData(gridData);
        BrowseButton.setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Buttons.Browse"));     

        BrowseButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {

                DirectoryDialog directoryDialog = new DirectoryDialog(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
                directoryDialog.setFilterPath(AddLanguageDialog.this.defaultPath);
                directoryDialog.setMessage(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Titles.ChangeFolder"));
                directoryDialog.setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Labels.ChangeFolder", AddLanguageDialog.this.localBundleObject.getName()));
                String Path = directoryDialog.open();
                if (Path != null)
                {
                    FoldersLabel.setText(Path);
                    AddLanguageDialog.this.defaultPath = Path;
                    File Folder = new File(Path);
                    try
                    {
                        AddLanguageDialog.this.localBundleObject = new BundleObject(Folder.toURI().toURL().toString());
                        AddLanguageDialog.this.getLocales();
                        FoldersLabel.setText(Path);
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
        });

        // Show Only Existing CheckBox
        if (COConfigurationManager.hasParameter("i18nAZ.ShowOnlyExisting", true) == true)
        {
            this.showOnlyExisting = COConfigurationManager.getBooleanParameter("i18nAZ.ShowOnlyExisting");
        }
        else
        {
            this.showOnlyExisting = true;
        }
        final Button ShowOnlyExistingCheckBox = new Button(shell, SWT.CHECK);
        gridData =  new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1);
        gridData.heightHint = 20;
        ShowOnlyExistingCheckBox.setLayoutData(gridData);
        
        ShowOnlyExistingCheckBox.setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Labels.ShowOnlyExisting"));
        ShowOnlyExistingCheckBox.setSelection(this.showOnlyExisting);
        ShowOnlyExistingCheckBox.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                AddLanguageDialog.this.showOnlyExisting = ShowOnlyExistingCheckBox.getSelection();
                COConfigurationManager.setParameter("i18nAZ.ShowOnlyExisting", AddLanguageDialog.this.showOnlyExisting);
                COConfigurationManager.save();
                AddLanguageDialog.this.table.removeAll();
                AddLanguageDialog.this.refreshList();
            }
        });

        // TABLE
        this.table = new Table(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.CHECK | SWT.FULL_SELECTION);
        this.table.setHeaderVisible(false);
        gridData =  new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1);
        this.table.setLayoutData(gridData);

        TableColumn tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(220);
        tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(220);
        tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(60);
        tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(60);

        this.getLocales();

        // Browse Button
        Button OkButton = new Button(shell, SWT.PUSH);
        OkButton.setText(i18nAZ.viewInstance.getLocalisedMessageText("Button.ok"));
        gridData =  new GridData(SWT.RIGHT, SWT.TOP, true, false);
        gridData.widthHint = 100;
         OkButton.setLayoutData(gridData);
        shell.setDefaultButton(OkButton);
        OkButton.addListener(SWT.Selection, new Listener()
        {

            @Override
            public void handleEvent(Event event)
            {
                ArrayList<Locale> localesSelectedList = new ArrayList<Locale>();
                TableItem[] tableItem = AddLanguageDialog.this.table.getItems();
                for (int i = 0; i < tableItem.length; i++)
                {
                    if (tableItem[i].getChecked() == true)
                    {
                        localesSelectedList.add((Locale) AddLanguageDialog.this.table.getData(String.valueOf(i)));
                    }
                }
                AddLanguageDialog.this.localesSelected = localesSelectedList.toArray(new Locale[localesSelectedList.size()]);
                shell.dispose();
            }
        });

        // Cancel Button
        Button CancelButton = new Button(shell, SWT.PUSH);
        CancelButton.setText(i18nAZ.viewInstance.getLocalisedMessageText("Button.cancel"));
        gridData =  new GridData(SWT.RIGHT, SWT.TOP, false, false);
        gridData.widthHint = 100;
         CancelButton.setLayoutData(gridData);
        CancelButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                shell.dispose();
            }
        });

        // set shell size
        shell.setSize(600, 450); 

        // open shell and center it to parent
        Util.openShell(owner, shell);
    }
    BundleObject getLocalBundleObject()
    {
        return this.localBundleObject;
    }
    private void getLocales()
    {
        this.table.removeAll();

        this.localesFounded = new ArrayList<Object[]>();
        if (this.defaultPath != null)
        {       
            if (this.localBundleObject.getUrl() != null)
            {
                String[] BundleFiles = null;
                if (this.localBundleObject.getUrl().getProtocol().equals("jar") == true)
                {
                    
                    JarFile jarFile = null;
                    try                    
                    {   
                        jarFile = new JarFile(this.localBundleObject.getFile());
                    }
                    catch (Exception e)
                    {
                    }
                    if (jarFile != null)
                    {                        
                        Enumeration<JarEntry> entries = jarFile.entries();
                        ArrayList<String> list = new ArrayList<String>(250);
                        while (entries.hasMoreElements())
                        {
                            JarEntry jarEntry = entries.nextElement();
                            if (jarEntry.getName().startsWith(this.localBundleObject.getJarFolder() + "/" + this.localBundleObject.getName()) && jarEntry.getName().endsWith(BundleObject.EXTENSION) && jarEntry.getName().length() < this.localBundleObject.getJarFolder().length() + this.localBundleObject.getName().length() + 2 + BundleObject.EXTENSION.length() + 7)

                            {
                                list.add(jarEntry.getName().substring(this.localBundleObject.getJarFolder().length() + 1));
                            }
                        }
                        BundleFiles = list.toArray(new String[list.size()]);
                        try
                        {
                            jarFile.close();
                        }
                        catch (IOException e)
                        {
                        }
                    }
                }
                else if (this.localBundleObject.getUrl().getProtocol().equals("file") == true)
                {
                    File BundleDirectory = this.localBundleObject.getFile();
                    BundleFiles = BundleDirectory.list(new FilenameFilter()
                    {
                        @Override
                        public boolean accept(File dir, String name)
                        {
                            return name.startsWith(AddLanguageDialog.this.localBundleObject.getName()) && name.endsWith(BundleObject.EXTENSION);
                        }
                    });
                }

                for (int j = 0; j < BundleFiles.length; j++)
                {
                    String Language = BundleFiles[j].substring(this.localBundleObject.getName().length(), BundleFiles[j].length() - BundleObject.EXTENSION.length());
                    Locale locale = null;
                    if (Language.length() >= 6)
                    {
                        locale = new Locale(Language.substring(1, 3), Language.substring(4, 6));
                    }
                    else if (Language.length() >= 3)
                    {
                        locale = new Locale(Language.substring(1, 3));
                    }
                    else
                    {
                        locale = new Locale("");
                    }
                    if (this.localeAlsoSelected.contains(locale) == false)
                    {
                        this.localesFounded.add(new Object[] { locale, true });
                    }

                }
            }
        }
        Locale[] AvailableLocales = Locale.getAvailableLocales();

        for (int i = 0; i < AvailableLocales.length; i++)
        {
            if (AvailableLocales[i].getCountry() != "")
            {
                boolean Existing = false;
                for (int j = 0; j < this.localesFounded.size(); j++)
                {
                    if (Util.getLocaleDisplay(AvailableLocales[i], false) == Util.getLocaleDisplay((Locale) this.localesFounded.get(j)[0], false))
                    {
                        Existing = true;
                        break;
                    }
                }
                if (Existing == false && this.localeAlsoSelected.contains(AvailableLocales[i]) == false)
                {
                    this.localesFounded.add(new Object[] { AvailableLocales[i], false });
                }
            }
        }
        this.refreshList();
    }
    private void refreshList()
    {
        Collections.sort(this.localesFounded, new Comparator<Object[]>()
        {
            @Override
            public final int compare(Object[] a, Object[] b)
            {
                return Util.getLocaleDisplay((Locale) a[0], false).compareToIgnoreCase(Util.getLocaleDisplay((Locale) b[0], false));
            }
        });

        for (int i = 0; i < this.localesFounded.size(); i++)
        {
            Locale locale = ((Locale) this.localesFounded.get(i)[0]);
            if (this.showOnlyExisting == false || (boolean) this.localesFounded.get(i)[1] == true)
            {
                this.table.setData(String.valueOf(this.table.getItemCount()), locale);
                TableItem item = new TableItem(this.table, SWT.NULL);
                if ((locale.getCountry() == "") && (locale.getLanguage() == ""))
                {
                    item.setText(0, "* Default *");
                    item.setText(1, "");
                    item.setText(2, "");
                    item.setText(3, "Existing");
                    if (item.getFont().getFontData().length > 0)
                    {
                        item.setFont(new Font(this.table.getDisplay(), this.table.getFont().getFontData()[0].getLocale(), this.table.getFont().getFontData()[0].getHeight(), SWT.BOLD));
                    }
                }
                else
                {
                    String LocaleDisplayLanguage = "";
                    if (locale.getDisplayLanguage(locale) != "")
                    {
                        LocaleDisplayLanguage = locale.getDisplayLanguage(locale);
                        LocaleDisplayLanguage = Character.toTitleCase(LocaleDisplayLanguage.charAt(0)) + LocaleDisplayLanguage.substring(1).toLowerCase(locale);
                    }
                    String LocaleDisplayCountry = "";
                    if (locale.getDisplayCountry(locale) != "")
                    {
                        LocaleDisplayCountry = locale.getDisplayCountry(locale);
                        LocaleDisplayCountry = Character.toTitleCase(LocaleDisplayCountry.charAt(0)) + LocaleDisplayCountry.substring(1).toLowerCase(locale);
                    }
                    item.setText(0, Util.getLocaleDisplay(locale, false));
                    item.setText(1, Util.getLocaleDisplay(locale, true));
                    item.setText(2, locale.toLanguageTag());
                    item.setText(3, ((boolean) this.localesFounded.get(i)[1] == true ? i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Labels.Existing") : ""));
                    if ((boolean) this.localesFounded.get(i)[1] == true & item.getFont().getFontData().length > 0)
                    {
                        item.setFont(new Font(this.table.getDisplay(), this.table.getFont().getFontData()[0].getName(), this.table.getFont().getFontData()[0].getHeight(), SWT.BOLD));
                    }
                }
            }
        }
    }
}
