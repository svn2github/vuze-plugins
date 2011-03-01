/*
 * Copyright (C) 2006  Chris Rose
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

package com.aimedia.stopseeding;

import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

/**
 * Convenience class for logging messages.
 */
public class Logger {

    private enum MessageType {
        ALERT,
        MESSAGE
    }

    private static LoggerChannel logger = null;
    private static LocaleUtilities locale = null;

    private static final void log (String messageId, Object[] params, Exception e, MessageType mtype, int messageLevel) {
        String message;
        if (params != null) {
            message = getLocale ().getLocalisedMessageText (messageId, getStringArrayForObjects (params));
        }
        else {
            message = getLocale ().getLocalisedMessageText (messageId);
        }

        if (e == null) {
            if (mtype == MessageType.ALERT) {
                getLogger().logAlert (messageLevel, message);
            }
            else if (mtype == MessageType.MESSAGE) {
                getLogger().log (messageLevel, message);
            }
        }
        else {
            if (mtype == MessageType.ALERT) {
                getLogger().logAlert (message, e);
            }
            else if (mtype == MessageType.MESSAGE) {
                getLogger().log (message, e);
            }
        }
    }

    public static void info (String messageId, Object... params) {
        log (messageId, params, null, MessageType.MESSAGE, LoggerChannel.LT_INFORMATION);
    }

    public static void warn (String messageId, Object... params) {
        log (messageId, params, null, MessageType.MESSAGE, LoggerChannel.LT_WARNING);
    }

    public static void error (String messageId, Exception e, Object... params) {
        log (messageId, params, e, MessageType.MESSAGE, LoggerChannel.LT_ERROR);
    }

    public static void alert (String messageId, Exception e, Object... params) {
        log (messageId, params, e, MessageType.ALERT, LoggerChannel.LT_ERROR);
    }

    private static String[] getStringArrayForObjects (Object[] params) {
        String [] sparams = new String[params.length];
        for (int i = 0; i < sparams.length; i++) {
            sparams[i] = params[i] == null ? "null" : params[i].toString ();
        }
        return sparams;
    }

    public static void infoAlert (String messageId) {
        log (messageId, null, null, MessageType.ALERT, LoggerChannel.LT_INFORMATION);
    }

    private static LocaleUtilities getLocale () {
        if (locale == null) {
            locale = AutoStopPlugin.plugin ().getPluginInterface ().getUtilities ().getLocaleUtilities ();
        }
        return locale;
    }

    /**
     * @return the logger
     */
    public static LoggerChannel getLogger () {
        if (logger == null) {
            logger = AutoStopPlugin.plugin ().getPluginInterface ().getLogger ().getChannel (ResourceConstants.PLUGIN_NAME);
        }
        return logger;
    }

}
