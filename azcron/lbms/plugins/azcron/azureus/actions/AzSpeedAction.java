package lbms.plugins.azcron.azureus.actions;

import org.gudy.azureus2.plugins.PluginConfig;
import org.jdom.Element;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.SpeedAction;
import lbms.plugins.azcron.azureus.AzCronPlugin;

/**
 * @author Damokles
 *
 */
public class AzSpeedAction extends SpeedAction {

	/**
	 * @param ul
	 * @param dl
	 */
	public AzSpeedAction(int ul, int dl) {
		super(ul, dl);
	}

	public AzSpeedAction(Element e) {
		super(e);
	}

	public static AzSpeedAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("SpeedAction")) {
			return new AzSpeedAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.SpeedAction#run()
	 */
	@Override
	public void run() {
		PluginConfig pc = AzCronPlugin.getPluginInterface().getPluginconfig();
		pc.setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, dl);
		pc.setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, ul);
	}

}
