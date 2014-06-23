package Manager;

import Interface.IDownloadHandler;
import Model.DownloadHandlerVO;
import Model.TorrentVO;
import Model.VideoFileVO;
import Utils.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.hamcrest.Matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static ch.lambdaj.Lambda.*;

/**
 * Created by IntelliJ IDEA.
 * User: Bruno
 * Date: 24/03/2010
 * Time: 15:32:31
 * To change this template use File | Settings | File Templates.
 */
public class DownloadManager {
    private PluginInterface _pluginInterface;
    private ConfigManager _config;
    private LogManager _log;
    private LocaleUtilities _localeUtil;

    public DownloadManager(PluginInterface pluginInterface) {
        _pluginInterface = pluginInterface;
        _config = new ConfigManager(_pluginInterface);
        _log = new LogManager(_pluginInterface);
        _localeUtil = _pluginInterface.getUtilities().getLocaleUtilities();
    }

    private List<TorrentVO> filterHasSubTitle(List<TorrentVO> listaTorrents) {
        List<TorrentVO> listaFiltrada = new ArrayList<TorrentVO>();
        // Tira os que já tem legenda
        for (TorrentVO torrentVO : listaTorrents) {
            List<VideoFileVO> listaVideo = filter(having(on(VideoFileVO.class).getHasSubTitle(), Matchers.equalTo(false)), torrentVO.getVideoFileList());
            if ((listaVideo != null) && (!listaVideo.isEmpty())) {
                torrentVO.setVideoFileList(listaVideo);
                listaFiltrada.add(torrentVO);
            }
        }
        return listaFiltrada;
    }

    private List<TorrentVO> filterCategory(List<TorrentVO> listaTorrents) {
        // Tira os que não devem ser baixados de acordo com a Categoria, se precisar
        if (!_config.getCategoryAll()) {
            String[] categoryList = _config.getCategoryList();
            // Filtra por Categoria, se houver alguma
            if (categoryList != null)
                for (int i = listaTorrents.size() - 1; i >= 0; i--) {
                    Boolean naoTem = true;
                    for (String aCategoryList : categoryList) {
                        if (aCategoryList.equalsIgnoreCase(listaTorrents.get(i).getCategory())) {
                            naoTem = false;
                            break;
                        }
                    }
                    if (naoTem)
                        listaTorrents.remove(i);
                }
        }
        return listaTorrents;
    }

    private List<TorrentVO> filterRegexExclude(List<TorrentVO> listaTorrents) {
        String excludeRegex = _config.getExcludeFilesRegex();
        if ((excludeRegex == null) || (excludeRegex.trim().isEmpty()))
            return listaTorrents;

        Pattern patternExcludeFiles = Pattern.compile(excludeRegex, Pattern.CASE_INSENSITIVE);
        
        List<TorrentVO> listaFiltrada = new ArrayList<TorrentVO>();
        // Tira o que estão de acordo com a Regex de eliminação configurado
        for (TorrentVO torrentVO : listaTorrents) {
            List<VideoFileVO> listaVideo = new ArrayList<VideoFileVO>();
            for (VideoFileVO videoVO : torrentVO.getVideoFileList()) {
                if (!patternExcludeFiles.matcher(videoVO.getFileName()).find())
                    listaVideo.add(videoVO);
            }
            if (!listaVideo.isEmpty()) {
                torrentVO.setVideoFileList(listaVideo);
                listaFiltrada.add(torrentVO);
            }
        }
        return listaFiltrada;
    }

    private List<TorrentVO> filterBasic(List<TorrentVO> listaTorrents) {
        listaTorrents = filterCategory(listaTorrents);
        listaTorrents = filterHasSubTitle(listaTorrents);
        listaTorrents = filterRegexExclude(listaTorrents);
        return listaTorrents;
    }

    /**
     * Verifica se os arquivos do Torrent realmente existem
     * @param torrentList
     * @return
     */
    private List<TorrentVO> filterFileExists(List<TorrentVO> torrentList) {
        List<TorrentVO> newTorrentList = new ArrayList<TorrentVO>();
        for (TorrentVO torrentVO : torrentList) {
            List<VideoFileVO> newList = new ArrayList<VideoFileVO>();
            for (VideoFileVO videoFile : torrentVO.getVideoFileList()) {
                if (!videoFile.getFile().exists()) {
                    _log.FileNotExistsOnTorrent(videoFile.getFile().getName(), torrentVO.getTorrentName());
                } else {
                    newList.add(videoFile);
                }
            }
            if (!newList.isEmpty()) {
                torrentVO.setVideoFileList(newList);
                newTorrentList.add(torrentVO);
            }
        }
        return newTorrentList;
    }

    public void getSubTileForTorrent(TorrentVO torrentVO) {
        if (!_config.getPluginActive())
            return;

        List<TorrentVO> listaTorrent = new ArrayList<TorrentVO>();
        listaTorrent.add(torrentVO);
        // Filtros básicos
        listaTorrent = filterBasic(listaTorrent);
        if (listaTorrent.isEmpty()) {
            _log.NoVideoToDownload(torrentVO.getTorrentName());
            return;
        }
        // Vamos ver se os arquivos existem mesmo
        listaTorrent = filterFileExists(listaTorrent);
        if (listaTorrent.isEmpty()) {
            _log.NoVideoToDownload(torrentVO.getTorrentName());
            return;
        }

        List<VideoFileVO> listaMovies = torrentVO.getVideoFileList();
        // Pega os Handlers
        Map<IDownloadHandler, DownloadHandlerVO> downloadHandlers = _config.getDownloadHandlers();
        boolean anyWasOk = false;
        // Vê se acha a bagaça nos Handlers
        for (Map.Entry<IDownloadHandler, DownloadHandlerVO> handler : downloadHandlers.entrySet()) {
            // Pego as legendas para os filmes
            try {
                downloadSubTitles(listaMovies, handler.getKey(), handler.getValue());
                anyWasOk = true;
            } catch (Exception e) {
                _log.ServerError(handler.getKey().getDescription(), e.getMessage(), e);
            }
            // Tiro da lista os que achei legenda
            listaMovies = filter(having(on(VideoFileVO.class).getHasSubTitle(), Matchers.equalTo(false)), listaMovies);
            // Se a lista está vazia, já saio, não tenho porque ficar aqui
            if (listaMovies.isEmpty())
                break;
        }
        // Agora mostra a mensagem pra quem sobrou, que foram os sem legenda
        if (anyWasOk)
            for (VideoFileVO movie : listaMovies)
                _log.NoSubTitle(movie.getFileName(), torrentVO.getTorrentName());
    }

    public void getSubTitleForAllCompletedMovies(Boolean showMessageIfNoTorrent) {
        // Pega todos os torrents que são video
        List<TorrentVO> listaTorrents = TorrentUtils.getMovieTorrents(_pluginInterface);
        // Filtros necessários
        listaTorrents = filterBasic(listaTorrents);
        if (listaTorrents.isEmpty()) {
            if (showMessageIfNoTorrent)
                _log.NoTorrentToDownload();
            return;
        }

        _log.InitiateAllDownloads();

        listaTorrents = filterFileExists(listaTorrents);
        if (listaTorrents.isEmpty()) {
            if (showMessageIfNoTorrent)
                _log.NoTorrentToDownload();
            return;
        }

        try {
            List<VideoFileVO> listaMovies = new ArrayList<VideoFileVO>();
            for (TorrentVO torrentVO : listaTorrents)
                listaMovies.addAll(torrentVO.getVideoFileList());
            boolean anyWasOk = false;
            // Pega os Handlers
            Map<IDownloadHandler, DownloadHandlerVO> downloadHandlers = _config.getDownloadHandlers();
            // Vê se acha a bagaça nos Handlers
            for (Map.Entry<IDownloadHandler, DownloadHandlerVO> handler : downloadHandlers.entrySet()) {
                if (listaMovies.isEmpty()) {
                    break;
                }
                // Pego as legendas para os filmes
                try {
                    downloadSubTitles(listaMovies, handler.getKey(), handler.getValue());
                    anyWasOk = true;
                } catch (Exception e) {
                    _log.ServerError(handler.getKey().getDescription(), e.getMessage(), e);
                }
                // Tira os que consegui pegar a legenda, os que sobraram vão para o próximo loop no próximo handler
                listaMovies = filter(having(on(VideoFileVO.class).getHasSubTitle(), Matchers.equalTo(false)), listaMovies);
            }
            // Log dos que não consegui legenda
            if (anyWasOk) {
                for (VideoFileVO movie : listaMovies) {
                    _log.NoSubTitle(movie.getFileName(), movie.getFileName());
                }
            }
        } finally {
            _log.FinishAllDownloads();
        }
    }

    private void downloadSubTitles(List<VideoFileVO> listaMovies, IDownloadHandler downloadHandler, DownloadHandlerVO handlerVO) throws Exception {
        if ((listaMovies == null) || (listaMovies.isEmpty()))
            return;

        SubTitleManager subManager = new SubTitleManager(_config, _log);
        subManager.setListaVideo(listaMovies);

        subManager.downloadSubTitles(downloadHandler, handlerVO);

        for (VideoFileVO videoVO : subManager.getListaVideoSaved())
            _log.SavedSubTitle(videoVO.getSubTitleVO().getFileName(), videoVO.getFileName(), downloadHandler.getDescription());
    }
}
