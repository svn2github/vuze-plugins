package Main;

import Threads.DownloadAllThread;
import Threads.DownloadPeriodThread;
import Manager.ConfigManager;
import Manager.TorrentListener;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 15/03/2010
 * Time: 14:05:19
 * To change this template use File | Settings | File Templates.
 */
public class Core implements UnloadablePlugin {
    public static final String VERSION_NUMBER = "0.3";
    public static final String SYSTEM_NAME = "VuzeLegendasBaixator";

    private DownloadPeriodThread downloadPeriod;

    public void unload() throws PluginException {
        downloadPeriod.stopSearching();
    }

    public void initialize(final PluginInterface pluginInterface) throws PluginException {
        // Add listener to execute when downloads finish
        pluginInterface.getDownloadManager().addListener(new TorrentListener(pluginInterface));

        // Initialize Configuration Page
        ConfigManager.initializeConfigPage(pluginInterface);

        // Create button to download all subtitles on Menu Bar
        pluginInterface.getUIManager().getMenuManager().addMenuItem(MenuManager.MENU_MENUBAR, "VuzeLegendasBaixator.config.btnDownloadAll").addListener(
                new MenuItemListener() {
                    public void selected(MenuItem menu, Object target) {
                        DownloadAllThread downAllThread = new DownloadAllThread(pluginInterface);
                        new Thread(downAllThread).start();
                    }
        });

        // Create thread for searching subtitles periodically
        downloadPeriod = new DownloadPeriodThread(pluginInterface);
        new Thread(downloadPeriod).start();
    }
}
