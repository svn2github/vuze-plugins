/*
 * AddLanguageDialog.java
 *
 * Created on February 23, 2004, 4:09 PM
 */

package i18nAZ;

import i18nAZ.TargetLocaleManager.TargetLocale;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * Prompts the user for a Language to add to the editor. If successful, localSelected will not be null.
 * 
 * @author TuxPaper
 * @author Repris d'injustice
 */
class AddLanguageDialog
{
    private Table table = null;
    private Label label;

    private ArrayList<Object[]> localesFounded = null;
    private Set<Locale> localeAlsoSelected = null;
    Locale[] localesSelected = null;

    private boolean showOnlyExisting;

    AddLanguageDialog(Shell owner)
    {

        this.addExistingLocales();

        final Shell shell = new Shell(owner, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setImages(owner.getImages());
        shell.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.AddLanguage"));

        GridLayout layout = new GridLayout(2, false);
        shell.setLayout(layout);

        // Label
        this.label = new Label(shell, SWT.NONE);
        this.label.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Labels.AddLanguage"));
        this.label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));

        // Folders Label
        GridData gridData = null;

        // Show Only Existing CheckBox
        if (COConfigurationManager.hasParameter("i18nAZ.ShowOnlyExisting", true) == true)
        {
            this.showOnlyExisting = COConfigurationManager.getBooleanParameter("i18nAZ.ShowOnlyExisting");
        }
        else
        {
            this.showOnlyExisting = true;
        }

        if (this.localesFounded.size() > 0)
        {
            final Button ShowOnlyExistingCheckBox = new Button(shell, SWT.CHECK);
            gridData = new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1);
            gridData.heightHint = 20;
            ShowOnlyExistingCheckBox.setLayoutData(gridData);

            ShowOnlyExistingCheckBox.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Labels.ShowOnlyExisting"));
            ShowOnlyExistingCheckBox.setSelection(this.showOnlyExisting);
            ShowOnlyExistingCheckBox.addSelectionListener(new SelectionAdapter()
            {
                
                public void widgetSelected(SelectionEvent e)
                {
                    AddLanguageDialog.this.showOnlyExisting = ShowOnlyExistingCheckBox.getSelection();
                    COConfigurationManager.setParameter("i18nAZ.ShowOnlyExisting", AddLanguageDialog.this.showOnlyExisting);
                    COConfigurationManager.save();
                    AddLanguageDialog.this.table.removeAll();
                    AddLanguageDialog.this.refreshList();
                }
            });
        }
        else
        {
            this.showOnlyExisting = false;
        }

        // TABLE
        this.table = new Table(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.CHECK | SWT.FULL_SELECTION);
        this.table.setHeaderVisible(false);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        this.table.setLayoutData(gridData);

        Util.setCustomHotColor(this.table);

        TableColumn tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(220);
        tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(220);
        tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(55);
        tableColumn = new TableColumn(this.table, SWT.LEFT);
        tableColumn.setWidth(80);

        // Browse Button
        Button okButton = new Button(shell, SWT.PUSH);
        okButton.setText(i18nAZ.getLocalisedMessageText("Button.ok"));
        gridData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
        gridData.widthHint = 100;
        okButton.setLayoutData(gridData);
        shell.setDefaultButton(okButton);
        okButton.addListener(SWT.Selection, new Listener()
        {
            
            public void handleEvent(Event event)
            {
                ArrayList<Locale> localesSelectedList = new ArrayList<Locale>();
                TableItem[] tableItem = AddLanguageDialog.this.table.getItems();
                for (int i = 0; i < tableItem.length; i++)
                {
                    if (tableItem[i].getChecked() == true)
                    {
                        localesSelectedList.add((Locale) tableItem[i].getData());
                    }
                }
                AddLanguageDialog.this.localesSelected = localesSelectedList.toArray(new Locale[localesSelectedList.size()]);
                shell.dispose();
            }
        });

        // Cancel Button
        Button cancelButton = new Button(shell, SWT.PUSH);
        cancelButton.setText(i18nAZ.getLocalisedMessageText("Button.cancel"));
        gridData = new GridData(SWT.RIGHT, SWT.TOP, false, false);
        gridData.widthHint = 100;
        cancelButton.setLayoutData(gridData);
        cancelButton.addListener(SWT.Selection, new Listener()
        {
            
            public void handleEvent(Event event)
            {
                shell.dispose();
            }
        });

        // set shell size
        shell.setSize(613, 480);

        this.addOtherLocales();
        this.refreshList();

        // open shell and center it to parent
        Util.openShell(owner, shell);
    }

    private void addExistingLocales()
    {
        this.localeAlsoSelected = new HashSet<Locale>();
        TargetLocale[]  targetLocales = TargetLocaleManager.toArray();
        for (int i = 0; i < targetLocales.length; i++)
        {
            if (targetLocales[i].isReadOnly() == false)
            {
                this.localeAlsoSelected.add(targetLocales[i].getLocale());
            }

        }

        this.localesFounded = new ArrayList<Object[]>();        
        File[] existingFiles = LocalizablePluginManager.getCurrentLangFile().existingFiles.clone();
        for (int i = 0; i < existingFiles.length; i++)
        {
            String localeFileName = Path.getFilenameWithoutExtension(existingFiles[i]);
            localeFileName = Path.getFilenameWithoutExtension(localeFileName);
            Locale locale = Util.getLocaleFromFilename(localeFileName);
            if (this.localeAlsoSelected.contains(locale) == false)
            {
                this.localesFounded.add(new Object[] { locale, true });
            }

        }

    }

    private void addOtherLocales()
    {
        List<Locale> locales = new ArrayList<Locale>();
        Locale[] availableLocales = Locale.getAvailableLocales();
        String[] isoLanguages = Locale.getISOLanguages();
        for (int i = 0; i < isoLanguages.length; i++)
        {
            boolean found = false;
            for (int j = 0; j < availableLocales.length; j++)
            {
                if (availableLocales[j].getCountry().equals("") == false && availableLocales[j].getLanguage().equals(isoLanguages[i]) == true)
                {
                    locales.add(availableLocales[j]);
                    found = true;
                }
            }
            if (found == false)
            {
                locales.add(new Locale(isoLanguages[i]));
            }
        }
        for (int i = 0; i < locales.size(); i++)
        {
            {
                boolean existing = false;
                for (int j = 0; j < this.localesFounded.size(); j++)
                {
                    if (locales.get(i).equals(this.localesFounded.get(j)[0]) == true)
                    {
                        existing = true;
                        break;
                    }
                }
                if (existing == false && this.localeAlsoSelected.contains(locales.get(i)) == false)
                {
                    this.localesFounded.add(new Object[] { locales.get(i), false });
                }
            }
        }
    }

    private void refreshList()
    {
        Collections.sort(this.localesFounded, new Comparator<Object[]>()
        {
            public final int compare(Object[] a, Object[] b)
            {
                return Util.getLocaleDisplay((Locale) a[0], false).compareToIgnoreCase(Util.getLocaleDisplay((Locale) b[0], false));
            }
        });

        for (int i = 0; i < this.localesFounded.size(); i++)
        {
            Locale locale = ((Locale) this.localesFounded.get(i)[0]);
            if (this.showOnlyExisting == false || (Boolean) this.localesFounded.get(i)[1] == true)
            {
                TableItem item = new TableItem(this.table, SWT.NULL);
                item.setData(locale);
                if ((locale.getCountry().equals("") == true) && (locale.getLanguage().equals("") == true))
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
                    item.setText(0, Util.getLocaleDisplay(locale, false));
                    item.setImage(0, Util.getLocaleImage(locale));
                    item.setText(1, Util.getLocaleDisplay(locale, true));
                    item.setText(2, Util.getLanguageTag(locale).replace("-", ""));
                    item.setText(3, ((Boolean) this.localesFounded.get(i)[1] == true ? i18nAZ.getLocalisedMessageText("i18nAZ.Labels.Existing") : ""));
                    if ((Boolean) this.localesFounded.get(i)[1] == true & item.getFont().getFontData().length > 0)
                    {
                        item.setFont(new Font(this.table.getDisplay(), this.table.getFont().getFontData()[0].getName(), this.table.getFont().getFontData()[0].getHeight(), SWT.BOLD));
                    }
                }
            }
        }
    }
}
