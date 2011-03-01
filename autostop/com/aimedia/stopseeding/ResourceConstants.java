/* MODULE: ResourceConstants.java */

/* COPYRIGHT
 * $RCSfile: ResourceConstants.java,v $
 * $Revision: 1.0 $
 * $Date: 2006/02/13 20:51:58 $
 * $Author: rosec $
 *
 * Copyright (c) 2007 Chris Rose
 * All rights reserved.
 * END COPYRIGHT */

package com.aimedia.stopseeding;

/**
 *
 */
public interface ResourceConstants {

    // : general UI constants
    String SET_CUSTOM_RATIO = "autostop.ui.set_custom_ratio.menu_item";
    String SET_CUSTOM_RATIO_NO_SELECTION = "autostop.ui.set_custom_ratio.menu_item.no_selection";
    String SET_CUSTOM_RATIO_DISPLAY_RATIO = "autostop.ui.set_custom_ratio.menu_item.selection_with_ratio";
    String SET_CUSTOM_RATIO_NO_DISPLAY_RATIO = "autostop.ui.set_custom_ratio.menu_item.selection_with_no_ratio";
    String SET_CUSTOM_RATIO_TITLE = "autostop.ui.set_custom_ratio.title";
    String SET_CUSTOM_RATIO_CONTENTS = "autostop.ui.set_custom_ratio.contents";
    String PLUGIN_NAME = "autostop.name";

    // : Configuration names
    String ATTRIBUTE_NAME = "autostop.config.per_torrent.ratio";
    String DEFAULT_RATIO = "autostop.config.default_ratio";
    String REMOVAL_ACTION = "autostop.config.removal_action";
    String DETAIL_LOGGING = "autostop.config.detail_logging";
    String ENABLED = "autostop.config.enabled";
    String BUG_TRACKER_LINK = "autostop.config.bug_tracker_url";
    String BUG_TRACKER_LINK_LABEL = "autostop.config.bug_tracker_url.label";

    // : action names
    String STOP_SEEDING = "autostop.action.stop_seeding";
    String STOP_AND_REMOVE = "autostop.action.stop_and_remove";
    String STOP_AND_DELETE_TORRENT = "autostop.action.stop_and_delete_torrent";
    String STOP_AND_DELETE_TORRENT_AND_DATA = "autostop.action.stop_and_delete_data";

    // : messages
    String RATIO_TOO_SMALL = "autostop.messages.ratio_too_small";
    String CANNOT_REMOVE_DOWNLOAD = "autostop.messages.cannot_remove_download";
    String CANNOT_STOP_DOWNLOAD = "autostop.messages.cannot_stop_download";
    String LOGGER_STARTUP = "autostop.messages.logger_startup";
    String LOGGER_SHUTDOWN = "autostop.messages.logger_shutdown";
    String PLUGIN_STARTUP = "autostop.messages.plugin_started";
    String PLUGIN_STOPPED = "autostop.messages.plugin_stopped";
    String ERROR_NO_DATASOURCE = "autostop.messages.no_datasource_method";
    String ERROR_GENERAL = "autostop.messages.general_fault";
    String LOG_STOP_SEEDING = "autostop.messages.stop_seeding_for_torrent";
}
