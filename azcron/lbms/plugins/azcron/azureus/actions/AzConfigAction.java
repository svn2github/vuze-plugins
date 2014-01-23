package lbms.plugins.azcron.azureus.actions;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.ConfigAction;
import lbms.plugins.azcron.actions.Parameter;
import lbms.plugins.azcron.azureus.AzCronPlugin;

import org.gudy.azureus2.plugins.PluginConfig;
import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class AzConfigAction extends ConfigAction {

	/**
	 * @param e
	 */
	public AzConfigAction(Element e) {
		super(e);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param p
	 */
	public AzConfigAction(Parameter p) {
		super(p);
		// TODO Auto-generated constructor stub
	}

	public static AzConfigAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("ConfigAction")) {
			return new AzConfigAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.ConfigAction#run()
	 */
	@Override
	public void run() {
		PluginConfig pc = AzCronPlugin.getPluginInterface().getPluginconfig();
		if (parameter.getType().equals(Parameter.Type.Boolean)) {
			pc.setUnsafeBooleanParameter(parameter.getName(), Boolean.parseBoolean(parameter.getValue()));
		} else if (parameter.getType().equals(Parameter.Type.Integer)) {
			pc.setUnsafeIntParameter(parameter.getName(), Integer.parseInt(parameter.getValue()));
		} else if (parameter.getType().equals(Parameter.Type.Long)) {
			pc.setUnsafeLongParameter(parameter.getName(), Long.parseLong(parameter.getValue()));
		} else if (parameter.getType().equals(Parameter.Type.Float)) {
			pc.setUnsafeFloatParameter(parameter.getName(), Float.parseFloat(parameter.getValue()));
		} else if (parameter.getType().equals(Parameter.Type.String)) {
			pc.setUnsafeStringParameter(parameter.getName(), parameter.getValue());
		}
	}
}
