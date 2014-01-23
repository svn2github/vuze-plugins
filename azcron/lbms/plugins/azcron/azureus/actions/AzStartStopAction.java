package lbms.plugins.azcron.azureus.actions;

import org.jdom.Element;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.StartStopAction;
import lbms.plugins.azcron.azureus.AzCronPlugin;

/**
 * @author Damokles
 *
 */
public class AzStartStopAction extends StartStopAction {

	/**
	 * @param start
	 */
	public AzStartStopAction(boolean start) {
		super(start);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param e
	 */
	public AzStartStopAction(Element e) {
		super(e);
		// TODO Auto-generated constructor stub
	}

	public static AzStartStopAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("StartStopAction")) {
			return new AzStartStopAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.StartStopAction#run()
	 */
	@Override
	public void run() {
		if (start) {
			AzCronPlugin.getPluginInterface().getDownloadManager().startAllDownloads();
		} else {
			AzCronPlugin.getPluginInterface().getDownloadManager().stopAllDownloads();
		}
	}

}
