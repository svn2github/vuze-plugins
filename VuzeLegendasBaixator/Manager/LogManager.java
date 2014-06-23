package Manager;

import Interface.ILogManager;
import Main.Core;
import Utils.FileUtils;
import ch.lambdaj.Lambda;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.hamcrest.Matchers;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 26/03/2010
 * Time: 15:24:07
 * To change this template use File | Settings | File Templates.
 */
public class LogManager implements ILogManager {
// ------------------------------ FIELDS ------------------------------

    private LoggerChannel _LoggerChannel;
    private LocaleUtilities _localeUtil;

// --------------------------- CONSTRUCTORS ---------------------------

    public LogManager(PluginInterface pluginInterface) {
        _LoggerChannel = Lambda.selectFirst(pluginInterface.getLogger().getChannels(), having(on(LoggerChannel.class).getName(), Matchers.equalToIgnoringCase(Core.SYSTEM_NAME)));
        if (_LoggerChannel == null)
            _LoggerChannel = pluginInterface.getLogger().getChannel(Core.SYSTEM_NAME);
        _localeUtil = pluginInterface.getUtilities().getLocaleUtilities();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ILogManager ---------------------

    public void error(String value) {
        _LoggerChannel.logAlertRepeatable(LoggerChannel.LT_ERROR, value);
    }

    public void debug(String value) {
        _LoggerChannel.log(value);
    }

    public void info(String value) {
        _LoggerChannel.logAlertRepeatable(LoggerChannel.LT_INFORMATION, value);
    }

    public void fatal(String value, Throwable e) {
        _LoggerChannel.logAlertRepeatable(value, e);
    }

    public void warning(String value) {
        _LoggerChannel.logAlertRepeatable(LoggerChannel.LT_WARNING, value);
    }

// -------------------------- OTHER METHODS --------------------------

    public void FinishAllDownloads() {
        info(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".FinishAllDownloads", new String[]{}));
    }

    public void InitiateAllDownloads() {
        info(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".InitiateAllDownloads", new String[]{}));
    }

    public void NoSubTitle(String fileName, String torrentName) {
        if (fileName.equalsIgnoreCase(torrentName))
            warning(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".NoSubtitle", new String[]{fileName}));
        else
            warning(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".NoSubtitleTorrent", new String[]{fileName, torrentName}));
    }

    public void NoTorrentToDownload() {
        warning(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".NoTorrentToDownload", new String[]{}));
    }

    public void NoVideoToDownload(String torrentName) {
        warning(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".NoVideoToDownload", new String[]{torrentName}));
    }

    public void SavedSubTitle(String subFileName, String movieFileName, String handlerDescription) {
        if (FileUtils.getFileNameWithoutExtension(subFileName).equalsIgnoreCase(FileUtils.getFileNameWithoutExtension(movieFileName)))
            info(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".SavedSubtitle", new String[]{subFileName, handlerDescription}));
        else
            info(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".SavedSubtitleFileName", new String[]{subFileName, movieFileName, handlerDescription}));
    }

    public void ServerError(String serverDescription, String errorMessage, Exception e) {
        fatal(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".ServerError", new String[]{serverDescription, errorMessage}), e);
    }

    public void FileNotExistsOnTorrent(String fileName, String torrentName) {
        warning(_localeUtil.getLocalisedMessageText(ConfigManager.BaseName + ".FileNotExistsOnTorrent", new String[]{fileName, torrentName}));
    }
}
