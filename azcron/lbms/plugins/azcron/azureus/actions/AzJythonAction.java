package lbms.plugins.azcron.azureus.actions;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.JythonAction;
import lbms.plugins.azcron.azureus.AzCronPlugin;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class AzJythonAction extends JythonAction {

	/**
	 * @param e
	 */
	protected AzJythonAction(Element e) {
		super(e);
	}

	/**
	 * @param scriptFile
	 */
	public AzJythonAction(String scriptFile) {
		super(scriptFile);
	}
	public static AzJythonAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("JythonAction")) {
			return new AzJythonAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.JythonAction#run()
	 */
	@Override
	public void run() {
		PluginInterface pi = AzCronPlugin.getPluginInterface().getPluginManager().getPluginInterfaceByID("azjython");
		if (pi != null) {
			try {
				if (runMethod) {
					pi.getIPC().invoke("ipcRunMethodFromFile", new Object[] {script, method, params});
				} else {
					pi.getIPC().invoke("ipcRunScriptFromFile", new Object[] {script, Boolean.FALSE});
				}
			} catch (IPCException e) {
				e.printStackTrace();
			}
		}
	}

}
