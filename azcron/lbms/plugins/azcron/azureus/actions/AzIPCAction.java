package lbms.plugins.azcron.azureus.actions;

import java.io.InvalidObjectException;
import java.util.List;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.IPCAction;
import lbms.plugins.azcron.actions.Parameter;
import lbms.plugins.azcron.azureus.AzCronPlugin;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class AzIPCAction extends IPCAction {

	private Object[] objParams;

	/**
	 * @param pluginID
	 * @param methodName
	 * @param params
	 */
	public AzIPCAction(String pluginID, String methodName,
			List<Parameter> params) {
		super(pluginID, methodName, params);
	}

	/**
	 * @param e
	 */
	public AzIPCAction(Element e) {
		super(e);
	}

	public static AzIPCAction createFromElement(Element e)throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("IPCAction")) {
			return new AzIPCAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	private void init () {
		objParams = new Object[params.size()];
		try {
			for (int i=0;i<objParams.length;i++) {
				objParams[i] = params.get(i).getAsObject();
			}
		} catch (InvalidObjectException e) {
			objParams = new Object[0];
		}
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.IPCAction#run()
	 */
	@Override
	public void run() {
		init();
		PluginInterface tPI = AzCronPlugin.getPluginInterface().getPluginManager().getPluginInterfaceByID(pluginID);
		if (tPI != null) {
			try {
				tPI.getIPC().invoke(methodName, objParams);
			} catch (IPCException e) {
				e.printStackTrace();
			}
		}
	}
}
