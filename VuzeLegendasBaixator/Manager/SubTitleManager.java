/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Manager;

import Exception.DownloadHandlerException;
import Implementation.LegendasTVHandler;
import Implementation.OpenSubtitlesOrgHandler;
import Implementation.SubDBHandler;
import Interface.IConfigManager;
import Interface.IDownloadHandler;
import Interface.ILogManager;
import Model.DownloadHandlerVO;
import Model.SubTitleLanguage;
import Model.SubTitleVO;
import Model.VideoFileVO;
import Utils.FileUtils;
import org.apache.log4j.lf5.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author Bruno
 */
public class SubTitleManager {

    public SubTitleManager(IConfigManager configManager, ILogManager logManager) {
        _configManager = configManager;
        _logManager = logManager;
    }

    private IConfigManager _configManager;
    public IConfigManager getConfigManager() {
        return _configManager;
    }

    private ILogManager _logManager;
    public ILogManager getLogManager() {
        return _logManager;
    }

    private List<VideoFileVO> _listaVideoSaved;
    public List<VideoFileVO> getListaVideoSaved() {
        return _listaVideoSaved;
    }

    private List<VideoFileVO> _listaVideoNoSubTitle;
    public List<VideoFileVO> getListaVideoNoSubTitle() {
        return _listaVideoNoSubTitle;
    }

    private Dictionary<VideoFileVO, Exception> _listaVideoError;
    public Dictionary<VideoFileVO, Exception> getListaVideoError() {
        return _listaVideoError;
    }

    private List<VideoFileVO> _listaVideo;
    public void setListaVideo(List<VideoFileVO> listaVideo) {
        _listaVideo = listaVideo;
    }
    public List<VideoFileVO> getListaVideo() {
        return _listaVideo;
    }

    private void initializeLists() {
        _listaVideoSaved = new ArrayList<VideoFileVO>();
        _listaVideoNoSubTitle = new ArrayList<VideoFileVO>();
        _listaVideoError = new  Hashtable<VideoFileVO, Exception>();
    }

    public void downloadSubTitles(IDownloadHandler downloadHandler, DownloadHandlerVO handlerVO) throws DownloadHandlerException {
        if ((_listaVideo == null) || (_listaVideo.isEmpty()))
            return;

        downloadHandler.setLogManager(_logManager);

        initializeLists();
        
        try {
            downloadHandler.doLogin(handlerVO);

            for (VideoFileVO movieFileVO : _listaVideo) {
                _logManager.debug(String.format("%s: Getting subtitle list from %s", movieFileVO.getFileName(), downloadHandler.getDescription()));
                List<SubTitleVO> listaSubTitle = downloadHandler.getSubTitleList(movieFileVO);

                if (listaSubTitle.isEmpty()) {
                    _logManager.debug(String.format("%s: SubTitle not found on %s", movieFileVO.getFileName(), downloadHandler.getDescription()));
                    _listaVideoNoSubTitle.add(movieFileVO);
                    continue;
                }
                _logManager.debug(String.format("%s: Found %s SubTitles on %s", movieFileVO.getFileName(), listaSubTitle.size(), downloadHandler.getDescription()));

                // Escolhe a melhor legenda nesse handler
                SubTitleVO chosenSubTitle = downloadHandler.chooseOneSubTitle(movieFileVO, listaSubTitle);

                _logManager.debug(String.format("%s: Downloading %s...", movieFileVO.getFileName(), chosenSubTitle.getFileName()));

                InputStream subTitleStream = null;
                try {
                    subTitleStream = downloadHandler.getSubTitleFile(chosenSubTitle);
                } catch (Exception e) {
                    _logManager.fatal(String.format("%s: Error downloading subtitle %s from %s: %s",
                            movieFileVO.getFileName(), chosenSubTitle.getFileName(), downloadHandler.getDescription(), e.getMessage()), e);
                    _listaVideoError.put(movieFileVO, e);
                }

                if (subTitleStream != null) {
                    String subtitleFileName = FileUtils.changeExtension(movieFileVO.getFileName(), FileUtils.getExtension(chosenSubTitle.getFileName()));
                    if (_configManager.getUseLanguageOnSubtitle()) {
                        String newExtension = SubTitleLanguage.getCodeISO639_2(_configManager.getLanguageOnSubtitle()) + "." +
                                FileUtils.getExtension(chosenSubTitle.getFileName());
                        subtitleFileName = FileUtils.changeExtension(subtitleFileName, newExtension);
                    }

                    File file = new File(movieFileVO.getPathDir(), subtitleFileName);
                    try {
                        FileOutputStream stream = new FileOutputStream(file);
                        StreamUtils.copyThenClose(subTitleStream, stream);
                    } catch (IOException ex) {
                        String message = "Error writing subtitle file: " + ex.getMessage();
                        _logManager.fatal(message, ex);
                        throw new RuntimeException(message, ex);
                    }
                    chosenSubTitle.setFileName(subtitleFileName);

                    _logManager.debug(String.format("%s: Subtitle file %s saved on %s", movieFileVO.getFileName(), subtitleFileName, movieFileVO.getPathDir()));
                    _listaVideoSaved.add(movieFileVO);

                    movieFileVO.setSubTitleVO(chosenSubTitle);
                    movieFileVO.setHasSubTitle(true);
                }
            }
        } finally {
            downloadHandler.doLogout();
        }
    }

     public static List<IDownloadHandler> getExistingHandlers() {
        List<IDownloadHandler> handlersList = new ArrayList<IDownloadHandler>();
        handlersList.add(new SubDBHandler());
        handlersList.add(new OpenSubtitlesOrgHandler());
        handlersList.add(new LegendasTVHandler());
        return handlersList;
    }
}
