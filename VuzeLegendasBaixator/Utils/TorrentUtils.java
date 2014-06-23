package Utils;

import Manager.ConfigManager;
import Model.SubTitleLanguage;
import Model.TorrentVO;
import Model.VideoFileVO;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.hamcrest.Matchers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.Lambda.*;


/**
 * Created by IntelliJ IDEA.
 * User: Bruno
 * Date: 24/03/2010
 * Time: 15:35:42
 * To change this template use File | Settings | File Templates.
 */
public class TorrentUtils {

    public static TorrentAttribute getCategoryAttr(PluginInterface pluginInterface) {
        return pluginInterface.getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
    }

    public static List<TorrentVO> getMovieTorrents(PluginInterface pluginInterface) {
        List<TorrentVO> listaTorrentVO = new ArrayList<TorrentVO>();
        // Filtra somente os que estão completos
        List<Download> listaTorrent = filter(having(on(Download.class).isComplete(), Matchers.equalTo(true)), pluginInterface.getDownloadManager().getDownloads());
        // Pega somente o que tem Video
        for (Download item : listaTorrent) {
            TorrentVO torrentVO = torrentMovieToTorrentVO(item, pluginInterface);
            if (torrentVO != null)
                listaTorrentVO.add(torrentMovieToTorrentVO(item, pluginInterface));
        }
        return listaTorrentVO;
    }

    public static TorrentVO torrentMovieToTorrentVO(Download download, PluginInterface pluginInterface) {
        List<VideoFileVO> movieList = new ArrayList<VideoFileVO>();
        if (!hasMovieFile(download))
            return null;

        // if we use the language name on subtitle file name
        ConfigManager config = new ConfigManager(pluginInterface);
        SubTitleLanguage subTitleLanguage = null;
        if (config.getUseLanguageOnSubtitle())
            subTitleLanguage = config.getLanguageOnSubtitle();

        for (DiskManagerFileInfo fileTorrent : download.getDiskManagerFileInfo()) {
            if ((!fileTorrent.isSkipped()) && (!fileTorrent.isDeleted()) && (FileUtils.isMovieFile(fileTorrent.getFile().getName()))) {
                VideoFileVO movieVO = VoUtils.fileToMovieVO(fileTorrent.getFile(), subTitleLanguage);
                movieList.add(movieVO);
            }
        }
        TorrentVO torrentVO = new TorrentVO();
        torrentVO.setCategory(download.getAttribute(getCategoryAttr(pluginInterface)));
        torrentVO.setTorrentName(download.getTorrent().getName());
        torrentVO.setVideoFileList(movieList);
        return torrentVO;
    }

    public static boolean hasMovieFile(Download download) {
        DiskManagerFileInfo[] filesTorrent = download.getDiskManagerFileInfo();
        for (DiskManagerFileInfo fileTorrent : filesTorrent) {
            if (fileTorrent == null)
                continue;
            File file = fileTorrent.getFile();
            if (file == null)
                continue;
            if (fileTorrent.isSkipped() || fileTorrent.isDeleted())
                continue;
            if (FileUtils.isMovieFile(file.getName()))
                return true;
        }
        return false;
    }
}
