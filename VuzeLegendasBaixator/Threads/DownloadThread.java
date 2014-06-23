package Threads;

import Model.TorrentVO;
import Manager.DownloadManager;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author Bruno
 * Thread que faz o download da legenda quando um Torrent termina
 */
public class DownloadThread extends BaseThread implements Runnable {
    private TorrentVO _torrentVO = null;

    public DownloadThread(PluginInterface pluginInterface, TorrentVO torrentVO) {
        super(pluginInterface);
        _torrentVO = torrentVO;
    }

    @Override
    public void run() {
        DownloadManager manager = new DownloadManager(_pluginInterface);
        manager.getSubTileForTorrent(_torrentVO);
    }
}
