/*
 * Copyright (C) 2005  Chris Rose
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package com.aimedia.autocat.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import com.aimedia.autocat.AutoCatPlugin;
import com.aimedia.autocat2.matching.AdvancedRuleSet;
import com.aimedia.autocat2.matching.IRuleSetListener;
import com.aimedia.autocat2.matching.OrderedRuleSet;
import com.aimedia.autocat2.matching.TorrentFieldType;
import com.aimedia.autocat2.matching.TorrentMatcher;

/**
 * @author Chris Rose
 */
public class ConfigUI implements ConfigSectionSWT, IRuleSetListener {

    private Label                 lblEnabled                   = null;

    private Label                 lblField                     = null;

    private Button                chkEnabled                   = null;

    private Table                 tblRules                     = null;

    private Label                 lblTrigger                   = null;

    private Text                  txtTrigger                   = null;

    private Button                btnAdd                       = null;

    private Button                btnDel                       = null;

    private Label                 lblCategory                  = null;

    private Combo                 cmbCategory                  = null;

    private Combo                 cmbField                     = null;

    private final PluginInterface pi;

    private final LoggerChannel   log;

    private final LocaleUtilities locale;

    private Map                   typeMap;

    private Config config;

    private Label lblModExisting;

    private Button chkModExisting;

    public ConfigUI (Config config) {
        this.pi = AutoCatPlugin.getPlugin ().getPluginInterface ();
        this.log = AutoCatPlugin.getPlugin ().getLog ();
        locale = pi.getUtilities ().getLocaleUtilities ();
        /* Populate the section with the configuration values */
        log.log (LoggerChannel.LT_INFORMATION, locale.getLocalisedMessageText ("autocat.info.populating"));
        this.config = config;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT#configSectionCreate(org.eclipse.swt.widgets.Composite)
     */

    public Composite configSectionCreate (final Composite parent) {
        config.getRules ().addRuleSetListener (this);
        final Composite panel = new Composite (parent, SWT.NONE);
        final GridData gridData5 = new org.eclipse.swt.layout.GridData ();
        final GridData gridData4 = new org.eclipse.swt.layout.GridData ();
        final GridData gridData1 = new org.eclipse.swt.layout.GridData ();
        final GridData gridData3 = new org.eclipse.swt.layout.GridData ();
        final GridData gridData2 = new org.eclipse.swt.layout.GridData ();
        final GridLayout gridLayout1 = new GridLayout ();
        lblEnabled = new Label (panel, SWT.NONE);
        chkEnabled = new Button (panel, SWT.CHECK);
        lblModExisting = new Label (panel, SWT.NONE);
        chkModExisting = new Button (panel, SWT.CHECK);
        createTable (panel);
        lblTrigger = new Label (panel, SWT.NONE);
        txtTrigger = new Text (panel, SWT.BORDER);
        lblCategory = new Label (panel, SWT.NONE);
        createCategoryCombo (panel);
        lblField = new Label (panel, SWT.NONE);
        createFieldCombo (panel);
        createControlPanel (panel);
        gridData3.horizontalSpan = 4;
        gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
        gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        gridData3.grabExcessHorizontalSpace = true;
        chkEnabled.setLayoutData (gridData3);
        gridData2.horizontalSpan = 4;
        gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
        gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        gridData2.grabExcessHorizontalSpace = true;
        chkModExisting.setLayoutData (gridData2);
        gridData4.grabExcessHorizontalSpace = false;
        gridData4.horizontalSpan = 2;
        gridData1.grabExcessHorizontalSpace = false;
        gridData1.horizontalSpan = 2;
        lblEnabled.setLayoutData (gridData4);
        lblModExisting.setLayoutData (gridData1);
        lblTrigger.setText (locale.getLocalisedMessageText ("autocat.trigger"));
        lblTrigger.setToolTipText (locale.getLocalisedMessageText ("autocat.trigger.tooltip"));
        gridData5.horizontalSpan = 5;
        gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData5.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        txtTrigger.setLayoutData (gridData5);
        txtTrigger.setToolTipText (locale.getLocalisedMessageText ("autocat.trigger.tooltip"));
        txtTrigger.addVerifyListener (new VerifyListener () {

            public void verifyText (VerifyEvent e) {
                // TODO Implement any verification of the trigger input.
                e.doit = true;
            }
        });
        lblCategory.setText ("Category");
        lblEnabled.setText (locale.getLocalisedMessageText ("autocat.enabled"));
        chkEnabled.setToolTipText (locale.getLocalisedMessageText ("autocat.enabled.tooltip"));
        lblModExisting.setText (locale.getLocalisedMessageText ("autocat.modify_existing"));
        chkModExisting.setToolTipText (locale.getLocalisedMessageText ("autocat.modify_existing.tooltip"));
        panel.setLayout (gridLayout1);
        gridLayout1.numColumns = 6;
        lblField.setText (locale.getLocalisedMessageText ("autocat.config.field"));
        panel.setSize (new org.eclipse.swt.graphics.Point (635, 456));
        chkEnabled.addSelectionListener (new SelectionAdapter () {
            /*
             * (non-Javadoc)
             *
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected (SelectionEvent e) {
                final Button b = (Button) e.widget;
                boolean enablement = b.getSelection ();
                config.setEnabled (enablement);

                chkModExisting.setEnabled (enablement);
                txtTrigger.setEnabled (enablement);
                cmbCategory.setEnabled (enablement);
                tblRules.setEnabled (enablement);
                btnAdd.setEnabled (enablement);
                btnDel.setEnabled (enablement);

                if (b.getSelection ()) {
                    log.log (locale.getLocalisedMessageText ("autocat.logenabled"));
                }
                else {
                    log.log (locale.getLocalisedMessageText ("autocat.logdisabled"));
                }
            }
        });

        chkModExisting.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                final Button b = (Button) e.widget;
                config.setModifyExistingCategories (b.getSelection ());
            }
        });

        boolean enablement = config.isEnabled ();
        chkEnabled.setSelection (enablement);
        chkModExisting.setEnabled (enablement);
        chkModExisting.setSelection (config.isModifyExistingCategories ());
        txtTrigger.setEnabled (enablement);
        cmbCategory.setEnabled (enablement);
        tblRules.setEnabled (enablement);
        btnAdd.setEnabled (enablement);
        btnDel.setEnabled (enablement);

        log.log ("configSectionGetParentSection() end");
        return panel;
    }

    void loadCategories (final Combo cmb) {
        final TorrentAttribute ta = pi.getTorrentManager ().getAttribute (TorrentAttribute.TA_CATEGORY);
        cmb.removeAll ();
        cmb.setItems (ta.getDefinedValues ());
    }

    void loadFields (final Combo cmb) {
        final List fields = TorrentFieldType.getSupportedFields ();
        final String[] flist = new String[fields.size ()];
        for (int i = 0; i < flist.length; i++) {
            flist[i] = ((TorrentFieldType) fields.get (i)).getName ();
        }
        cmb.removeAll ();
        cmb.setItems (flist);
    }

    void loadRules (final OrderedRuleSet set, final Table t) {
        final TableItem[] items = t.getItems ();
        for (int i = 0; i < items.length; i++) {
            items[i].dispose ();
        }
        final List<TorrentMatcher> rules = set.getRules ();
        for (int i = 0; i < rules.size (); i++) {
            final TableItem ti = new TableItem (t, SWT.NONE);
            ti.setText (0, i + "");
            TorrentMatcher rule = rules.get (i);
            ti.setText (1, rule.getTrigger ());
            ti.setText (2, rule.getCategory ());
            ti.setText (3, rule.getMatchField ().toString ());
            log.log ("Added rule " + rule.getTrigger () + ":" + rule.getMatchField ().toString () + "===>"
                    + rule.getCategory ());
        }
        t.setSelection (0);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aimedia.autocat.config.RuleSetListener#ruleSetUpdated()
     */
    public void ruleSetUpdated (final AdvancedRuleSet rules) {
        loadRules (rules, tblRules);
    }

    /**
     * This method initializes cmbField
     *
     * @param parent
     *                TODO
     *
     */
    private void createCategoryCombo (final Composite parent) {
        final GridData gridData13 = new org.eclipse.swt.layout.GridData ();
        cmbCategory = new Combo (parent, SWT.NONE);
        gridData13.horizontalSpan = 5;
        gridData13.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData13.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        cmbCategory.setLayoutData (gridData13);
        cmbCategory.setToolTipText (locale.getLocalisedMessageText ("autocat.category.tooltip"));
        loadCategories (cmbCategory);
    }

    /**
     * This method initializes composite1
     *
     * @param parent
     *                TODO
     *
     */
    private void createControlPanel (final Composite parent) {
        final GridData gridData12 = new org.eclipse.swt.layout.GridData ();
        final Composite cControls = new Composite (parent, SWT.NONE);
        btnAdd = new Button (cControls, SWT.NONE);
        btnDel = new Button (cControls, SWT.NONE);
        cControls.setLayout (new RowLayout ());
        cControls.setLayoutData (gridData12);
        gridData12.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData12.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        gridData12.horizontalSpan = 6;
        btnAdd.setText (locale.getLocalisedMessageText ("autocat.config.add"));
        btnAdd.addSelectionListener (new SelectionAdapter () {
            /*
             * (non-Javadoc)
             *
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected (SelectionEvent e) {
                // log.log(LoggerChannel.LT_WARNING, "Add needs to be
                // implemented");
                try {
                    Pattern.compile (txtTrigger.getText ());
                    if (cmbCategory.getText ().length () == 0 || cmbField.getSelectionIndex () < 0) {
                        // log.log("Cannot set 0-length categories");
                        log.log (locale.getLocalisedMessageText ("autocat.err.nocat"));
                    }
                    else {
                        // TODO Make this work
                        final TorrentFieldType type = (TorrentFieldType) getTypeMap ().get (
                                cmbField.getItem (cmbField.getSelectionIndex ()));
                        config.getRules ().addRule (type, txtTrigger.getText (), cmbCategory.getText ());
                    }
                }
                catch (PatternSyntaxException pse) {
                    // log.log("Could not compile regex", pse);
                    log.log (locale.getLocalisedMessageText ("autocat.err.badregex", new String[] { txtTrigger
                            .getText () }));
                }
                loadCategories (cmbCategory);
            }
        });
        btnDel.setText (locale.getLocalisedMessageText ("autocat.config.remove"));
        btnDel.addSelectionListener (new SelectionAdapter () {
            /*
             * (non-Javadoc)
             *
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected (SelectionEvent e) {
                // log.log(LoggerChannel.LT_WARNING, "Del needs to be
                // implemented");
                if (txtTrigger.getText ().length () > 0) {
                    for (int ruleIndex : tblRules.getSelectionIndices ()) {
                        config.getRules ().removeRule (ruleIndex);
                    }
                }
            }
        });
    }

    /**
     * @return
     */
    protected Map getTypeMap () {
        if (null == typeMap) {
            typeMap = new HashMap ();
            final List f = TorrentFieldType.getSupportedFields ();
            for (final Iterator iter = f.iterator (); iter.hasNext ();) {
                final TorrentFieldType field = (TorrentFieldType) iter.next ();
                typeMap.put (field.toString (), field);
            }
        }
        return typeMap;
    }

    /**
     * This method initializes cmbField
     *
     * @param parent
     *                TODO
     *
     */
    private void createFieldCombo (final Composite parent) {
        final GridData gridData1 = new org.eclipse.swt.layout.GridData ();
        cmbField = new Combo (parent, SWT.READ_ONLY);
        gridData1.horizontalSpan = 5;
        gridData1.grabExcessHorizontalSpace = false;
        gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        cmbField.setLayoutData (gridData1);
        cmbField.setToolTipText (locale.getLocalisedMessageText ("autocat.config.field.tooltip"));
        loadFields (cmbField);
    }

    /**
     * This method initializes table
     *
     * @param parent
     *                TODO
     *
     */
    private void createTable (final Composite parent) {
        final GridData gridData2 = new org.eclipse.swt.layout.GridData ();
        tblRules = new Table (parent, SWT.FULL_SELECTION | SWT.BORDER);
        final TableColumn colRuleIndex = new TableColumn (tblRules, SWT.NONE);
        final TableColumn colTrigger = new TableColumn (tblRules, SWT.NONE);
        final TableColumn colCategory = new TableColumn (tblRules, SWT.NONE);
        final TableColumn colField = new TableColumn (tblRules, SWT.NONE);
        colTrigger.setText (locale.getLocalisedMessageText ("autocat.trigger"));
        colTrigger.setWidth (200);
        colCategory.setText (locale.getLocalisedMessageText ("autocat.category"));
        colCategory.setWidth (200);
        colField.setText (locale.getLocalisedMessageText ("autocat.config.field"));
        colField.setWidth (200);
        colRuleIndex.setResizable (false);
        gridData2.verticalSpan = 1;
        tblRules.setHeaderVisible (true);
        gridData2.grabExcessHorizontalSpace = true;
        gridData2.grabExcessVerticalSpace = true;
        gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData2.horizontalSpan = 6;
        tblRules.setLayoutData (gridData2);
        tblRules.setLinesVisible (true);
        loadRules (config.getRules (), tblRules);
        tblRules.addSelectionListener (new SelectionAdapter () {
            /*
             * (non-Javadoc)
             *
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected (SelectionEvent e) {
                e.doit = true;
                final int i = tblRules.getSelectionIndex ();
                if (i >= 0) {
                    final TableItem t = tblRules.getItem (i);
                    txtTrigger.setText (t.getText (1));
                    cmbCategory.setText (t.getText (2));
                    cmbField.select (cmbField.indexOf (t.getText (3)));
                }
            }
        });
        tblRules.pack ();
    }

    public void ruleSetUpdated (OrderedRuleSet set) {
        loadRules (set, tblRules);
    }

    public void configSectionDelete () {
        config.getRules ().removeRuleSetListener (this);
        config.configSectionDelete ();
    }

    public String configSectionGetName () {
        return config.configSectionGetName ();
    }

    public String configSectionGetParentSection () {
        return config.configSectionGetParentSection ();
    }

    public void configSectionSave () {
        config.configSectionSave ();
    }

}