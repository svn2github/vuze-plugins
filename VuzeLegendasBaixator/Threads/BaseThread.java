package Threads;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 26/03/2010
 * Time: 16:08:46
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseThread {
    protected PluginInterface _pluginInterface;

    public BaseThread(PluginInterface pluginInterface) {
        _pluginInterface = pluginInterface;
    }
}
