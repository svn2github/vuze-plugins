package com.aimedia.stopseeding.core;

import java.net.URL;

import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringListParameter;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableRow;

import com.aimedia.stopseeding.AutoStopPlugin;
import com.aimedia.stopseeding.ResourceConstants;

public class AutoStopConfiguration {

    /**
     *
     */
    public class AddRatioListener implements MenuItemListener {

        /*
         * (non-Javadoc)
         *
         * @see org.gudy.azureus2.plugins.ui.menus.MenuItemListener#selected(org.gudy.azureus2.plugins.ui.menus.MenuItem,
         *      java.lang.Object)
         */
        public void selected (MenuItem menu, Object target) {
            System.out.println ("Menu item selected: " + menu.getText ());
            System.out.println ("Target: " + target.getClass () + ": " + target.toString ());
            if (target instanceof TableRow) {
                TableRow tr = (TableRow) target;

                System.out.println ("Download: " + ((Download) tr.getDataSource ()).getName ());
            }
        }

    }

    public static enum StopAction {
        StopSeeding(ResourceConstants.STOP_SEEDING),
        StopAndRemove(ResourceConstants.STOP_AND_REMOVE),
        StopAndDeleteTorrent(ResourceConstants.STOP_AND_DELETE_TORRENT),
        StopAndDeleteData(ResourceConstants.STOP_AND_DELETE_TORRENT_AND_DATA);

        private String resourceName;

        private StopAction(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getResource () {
            return resourceName;
        }

        public String toString () {
            return AutoStopPlugin.plugin ().getPluginInterface ().getUtilities ().getLocaleUtilities ()
                    .getLocalisedMessageText (getResource ());
        }

    }

    private static final String  CONFIG_ACTION          = "defaultAction";

    private static final String  CONFIG_DETAIL          = "detaillogging";

    private static final boolean CONFIG_DETAIL_DEFAULT  = false;

    private static final String  CONFIG_ENABLED         = "enabled";

    private static final boolean CONFIG_ENABLED_DEFAULT = false;

    private static final String  CONFIG_DEFAULT_RATIO   = "default_ratio";

    private BooleanParameter     enabled, detail;

    private StringListParameter  defaultRatio;

    private StringListParameter  removalAction;

    public AutoStopConfiguration(final AutoStopPlugin plugin) {

        BasicPluginConfigModel cfg = plugin.getPluginInterface ().getUIManager ().createBasicPluginConfigModel (
                "plugins", ResourceConstants.PLUGIN_NAME);

        enabled = cfg.addBooleanParameter2 (CONFIG_ENABLED, ResourceConstants.ENABLED, CONFIG_ENABLED_DEFAULT);

        // : support addition of new string ratios
        defaultRatio = cfg.addStringListParameter2 (CONFIG_DEFAULT_RATIO, ResourceConstants.DEFAULT_RATIO,
                getDefaultRatioOptions (), "1.0");

        // : FIXME reverse labels and values in the short term until sf.net
        // issue # 1701119 is resolved
        removalAction = cfg.addStringListParameter2 (CONFIG_ACTION, ResourceConstants.REMOVAL_ACTION,
                                                     
                makeActionValues (), makeActionLabels (), StopAction.StopAndRemove.name ());

        // : configure debug logging
        detail = cfg.addBooleanParameter2 (CONFIG_DETAIL, ResourceConstants.DETAIL_LOGGING, CONFIG_DETAIL_DEFAULT);
        detail.addConfigParameterListener (new ConfigParameterListener () {

            public void configParameterChanged (ConfigParameter param) {
                assert (param instanceof BooleanParameter);
                BooleanParameter b = (BooleanParameter) param;
                if (b.getValue ()) {
                    AutoStopPlugin.plugin ().startLogger ();
                }
                else {
                    AutoStopPlugin.plugin ().stopLogger ();
                }
            }
        });

        ActionParameter bugUrl = cfg.addActionParameter2 (ResourceConstants.BUG_TRACKER_LINK_LABEL,
                ResourceConstants.BUG_TRACKER_LINK);
        bugUrl.setStyle (ActionParameter.STYLE_LINK);
        bugUrl.addListener (new ParameterListener () {

            public void parameterChanged (Parameter param) {
                try {
                    plugin.getPluginInterface ().getUIManager ()
			.openURL (new URL (AutoStopPlugin.plugin ()
					   .getPluginInterface ()
					   .getUtilities ()
					   .getLocaleUtilities ()
					   .getLocalisedMessageText (ResourceConstants.BUG_TRACKER_LINK)));
                }
                catch (Throwable e) {
                    e.printStackTrace ();
                }
            }

        });
    }

    /**
     * Get the number of milliseconds between scans of the download table.
     *
     * @return the sweep interval in milliseconds.
     */
    public long getScanInterval () {
        return 1000;
    }

    private String[] makeActionValues () {
        String[] ret = new String[StopAction.values ().length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = StopAction.values ()[i].name ();
        }

        return ret;
    }

    private String[] makeActionLabels () {
        String[] ret = new String[StopAction.values ().length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = StopAction.values ()[i].toString ();
        }

        return ret;
    }

    public boolean isEnabled () {
        return enabled.getValue ();
    }

    public boolean isLoggingEnabled () {
        return detail.getValue ();
    }

    /**
     * @return
     */
    public MenuItemListener createAddRatioListener () {
        return new AddRatioListener ();
    }

    /**
     * @return
     */
    public String getDefaultRatio (Download download) {
        return defaultRatio.getValue ();
    }

    public String getGlobalDefaultRatio () {
        return defaultRatio.getValue ();
    }

    public StopAction getAction () {
        StopAction action;
        try {
            action = StopAction.valueOf (removalAction.getLabelKey ());
        }
        catch (IllegalArgumentException e) {
            action = StopAction.valueOf (removalAction.getValue ());
        }
        return action;
    }

    /**
     * @return
     */
    public String[] getDefaultRatioOptions () {
        return new String[] { "Unlimited", "1.0", "1.5", "2.0", "5.0" };
    }

}
