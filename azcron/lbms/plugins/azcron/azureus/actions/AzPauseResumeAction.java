package lbms.plugins.azcron.azureus.actions;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.PauseResumeAction;
import lbms.plugins.azcron.azureus.AzCronPlugin;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class AzPauseResumeAction extends PauseResumeAction {

	/**
	 * @param start
	 */
	public AzPauseResumeAction(boolean start) {
		super(start);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param e
	 */
	public AzPauseResumeAction(Element e) {
		super(e);
		// TODO Auto-generated constructor stub
	}

	public static AzPauseResumeAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("PauseResumeAction")) {
			return new AzPauseResumeAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.StartStopAction#run()
	 */
	@Override
	public void run() {
		if (start) {
			AzCronPlugin.getPluginInterface().getDownloadManager().resumeDownloads();
		} else {
			AzCronPlugin.getPluginInterface().getDownloadManager().pauseDownloads();
		}
	}

}
