package Manager;

import Model.TorrentVO;
import Threads.DownloadThread;
import Utils.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 05/05/2010
 * Time: 11:27:06
 * To change this template use File | Settings | File Templates.
 */
public class TorrentListener implements DownloadManagerListener, DownloadCompletionListener, DownloadAttributeListener {
    private PluginInterface _pluginInterface;
    private ConfigManager _configManager;

    public TorrentListener(PluginInterface pluginInterface) {
        _pluginInterface = pluginInterface;
        _configManager = new ConfigManager(_pluginInterface);
    }

    @Override
    public void downloadAdded(Download download) {
        // Se não tem video no torrent nem põe o listener
        if (!TorrentUtils.hasMovieFile(download))
            return;
        // Põe os listeners
        download.addCompletionListener(this);
        download.addAttributeListener(this, TorrentUtils.getCategoryAttr(_pluginInterface), 0);
    }

    @Override
    public void downloadRemoved(Download download) {
        download.removeCompletionListener(this);
        download.removeAttributeListener(this, TorrentUtils.getCategoryAttr(_pluginInterface), 0);
    }

    @Override
    public void onCompletion(Download download) {
        // Terminou de baixar o torrent, agora pego as legendas
        TorrentVO torrentVO = TorrentUtils.torrentMovieToTorrentVO(download, _pluginInterface);
        DownloadThread downloadThread = new DownloadThread(_pluginInterface, torrentVO);
        new Thread(downloadThread).start();
    }

    @Override
    public void attributeEventOccurred(Download download, TorrentAttribute torrentAttribute, int i) {
        // Se mudou de categoria e estã completo, vejo se agora tenho que pegar legenda
        // Se não tem filtro por categoria ou não está completo cai fora, o evento onCompletion vai fazer o trabalho
        if ((_configManager.getCategoryAll()) || (!download.isComplete()))
            return;
        // Se estiver completo pego as legendas
        TorrentVO torrentVO = TorrentUtils.torrentMovieToTorrentVO(download, _pluginInterface);
        DownloadThread downloadThread = new DownloadThread(_pluginInterface, torrentVO);
        new Thread(downloadThread).start();
    }
}
