package Threads;

import Manager.DownloadManager;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author Bruno
 * Thread que busca legenda para todos os torrent finalizados ainda sem
 * legendas, disparada pela página de Configuração
 */
public class DownloadAllThread extends BaseThread implements Runnable {

    public DownloadAllThread(PluginInterface pluginInterface) {
        super(pluginInterface);
    }

    @Override
    public void run() {
        DownloadManager manager = new DownloadManager(_pluginInterface);
        manager.getSubTitleForAllCompletedMovies(true);
    }
}
