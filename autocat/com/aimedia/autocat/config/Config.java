package com.aimedia.autocat.config;

import java.io.File;
import java.io.IOException;

import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import com.aimedia.autocat.AutoCatPlugin;
import com.aimedia.autocat2.matching.AdvancedRuleSet;
import com.aimedia.autocat2.matching.OrderedRuleSet;

public class Config implements ConfigSection {

    private static final String   MATCHER_CONFIG_FILE          = AutoCatPlugin.NAME + "_rules.properties";

    private static final String   OBSOLETE_MATCHER_CONFIG_FILE = AutoCatPlugin.NAME + ".config";

    private final AdvancedRuleSet rules;

    private boolean               enabled;

    private boolean               useTags;
    
    private boolean               modifyExistingCategories;

    private PluginInterface pi;

    private LoggerChannel log;

    private LocaleUtilities locale;

    public Config (PluginInterface pluginInterface) {
        this.pi = pluginInterface;
        this.log = AutoCatPlugin.getPlugin ().getLog ();
        locale = pi.getUtilities ().getLocaleUtilities ();
        /* Populate the section with the configuration values */
        log.log (LoggerChannel.LT_INFORMATION, locale.getLocalisedMessageText ("autocat.info.populating"));

        rules = new AdvancedRuleSet ();
        // This occurs before the listener setup because we don't
        // have a table yet.

        // : look for a new-style property config file, first:
        File propertyFile = FileUtil.getUserFile (MATCHER_CONFIG_FILE);
        if (!propertyFile.exists ()) {

            // : upgrade an obsolete bencoded file if it is present.
            File mapFile = FileUtil.getUserFile (OBSOLETE_MATCHER_CONFIG_FILE);

            if (mapFile.exists ()) {
                AdvancedRuleSet.transferSettings (mapFile, propertyFile);
            }
            else {
                try {
                    propertyFile.createNewFile ();
                }
                catch (IOException e) {
                    AutoCatPlugin.getPlugin ().getLog ().log ("Unable to create a new configuration file", e);
                }
            }

            rules.load (propertyFile);
        }
        else {
            rules.load (propertyFile);
        }

        enabled = pi.getPluginconfig ().getPluginBooleanParameter ("enabled", false);
        modifyExistingCategories = pi.getPluginconfig ().getPluginBooleanParameter ("modifyExistingCategories", true);
        useTags = pi.getPluginconfig ().getPluginBooleanParameter ("useTags", false);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionGetParentSection()
     */
    public String configSectionGetParentSection () {
        return ConfigSection.SECTION_PLUGINS;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionGetName()
     */
    public String configSectionGetName () {
        log.log ("configSectionGetName() start/end");
        return AutoCatPlugin.NAME;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionSave()
     */
    public void configSectionSave () {
        // Save the rules to their file.
        log.log ("configSectionSave() start");
        try {
            File saveFile = FileUtil.getUserFile (MATCHER_CONFIG_FILE);
            if (!saveFile.exists ()) {
                if (!saveFile.createNewFile ()) {
                    log.log (locale.getLocalisedMessageText ("autocat.err.failRuleCreate"));
                }
            }
            if (!rules.save (FileUtil.getUserFile (MATCHER_CONFIG_FILE))) {
                log.log (locale.getLocalisedMessageText ("autocat.err.failRuleWrite"));
            }
        }
        catch (IOException e) {
            log.log (e);
            log.log ("autocat.err.IO");
        }

        // Save all extra config information.
        final PluginConfig cfg = pi.getPluginconfig ();
        cfg.setPluginParameter ("enabled", enabled);
        cfg.setPluginParameter ("modifyExistingCategories", modifyExistingCategories);
        cfg.setPluginParameter ("useTags", useTags);
        try {
            cfg.save ();
            log.log ("configSectionSave() clean exit");
        }
        catch (PluginException e1) {
            log.log (e1);
            log.log ("configSectionSave() dirty exit");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionDelete()
     */
    public void configSectionDelete () {
        // TODO Anything that doesn't have a parent, I have to dispose() it
        // here.
        log.log (LoggerChannel.LT_INFORMATION, locale.getLocalisedMessageText ("autocat.info.configDelete"));
    }

    public boolean isEnabled () {
        return enabled;
    }

    public void setEnabled (boolean enablement) {
        if (this.enabled != enablement) {
            this.enabled = enablement;
        }
    }

    public OrderedRuleSet getRules () {
        return rules;
    }

    public boolean isUseTags () {
        return useTags;
    }

    public void setUseTags (boolean set) {
        if (this.useTags != set) {
            this.useTags = set;
        }
    }
    
    public boolean isModifyExistingCategories () {
        return modifyExistingCategories;
    }

    public void setModifyExistingCategories (boolean modifyExistingCategories) {
        if (this.modifyExistingCategories != modifyExistingCategories) {
            this.modifyExistingCategories = modifyExistingCategories;
        }
    }

}
