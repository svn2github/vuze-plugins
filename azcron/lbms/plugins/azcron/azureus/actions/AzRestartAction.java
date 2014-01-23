package lbms.plugins.azcron.azureus.actions;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.RestartAction;
import lbms.plugins.azcron.azureus.AzCronPlugin;

import org.gudy.azureus2.plugins.update.UpdateException;
import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class AzRestartAction extends RestartAction {

	/**
	 * 
	 */
	public AzRestartAction() {
		// TODO Auto-generated constructor stub
	}

	public static AzRestartAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("RestartAction")) {
			return new AzRestartAction();
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.RestartAction#run()
	 */
	@Override
	public void run() {
		try {
			AzCronPlugin.getPluginInterface().getUpdateManager().applyUpdates(true);
		} catch (UpdateException e) {
			e.printStackTrace();
		}
	}
}
