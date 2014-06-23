package Interface;

import Exception.DownloadHandlerException;
import Model.*;
import Utils.FileUtils;

import java.io.InputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 25/02/2010
 * Time: 18:53:23
 * To change this template use File | Settings | File Templates.
 */
public interface IDownloadHandler {
    public static final String URL = "http://legendasbaixator.googlecode.com";

    public enum LogonType {
        None,
        Optional,
        Required
    }

    /**
     * Set System Information
     * @param systemInformation
     */
    void setSystemInformation(SystemInformation systemInformation);

    void setLogManager(ILogManager logManager);

    /**
     * Handler Description
     * @return
     */
    String getDescription();

    /**
     * Handler URL
     * @return
     */
    String getSiteUrl();

    /**
     * The VO with fields used by Handler
     * @return
     */
    Class getHandlerVOType();

    /**
     * Logon Type used by Handler
     * @return
     */
    LogonType getLogonType();

    /**
     * Language List supported by Handler
     * @return
     */
    SubTitleLanguage[] getSupportedLanguages();

    /**
     * SubTitle Extension List supported by Handler
     * @return
     */
    FileUtils.SubTitleExtensions[] getSupportedSubTitleExtensions();

    /**
     * Do login on Handler
     * @param handlerVO
     * @throws DownloadHandlerException
     */
    void doLogin(DownloadHandlerVO handlerVO) throws DownloadHandlerException;

    /**
     * Get a list of subtitles based on a movie
     * @param movieFile
     * @return
     */
    List<SubTitleVO> getSubTitleList(VideoFileVO movieFile);

    /**
     * Method do choose the best subtitle to a movie
     * @param movieFile
     * @param subList
     * @return selected subtitle
     */
    SubTitleVO chooseOneSubTitle(VideoFileVO movieFile, List<SubTitleVO> subList);

    /**
     * Download the subtitle content from a subtitle
     * @param subTitleVO
     * @return
     */
    InputStream getSubTitleFile(SubTitleVO subTitleVO);

    /**
     * Logout on Handler
     */
    void doLogout();
}
