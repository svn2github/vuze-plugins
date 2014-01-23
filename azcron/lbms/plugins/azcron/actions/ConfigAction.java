package lbms.plugins.azcron.actions;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class ConfigAction implements Action {

	protected Parameter parameter;

	protected ConfigAction (Element e) {
		this.parameter = Parameter.createFromElement( e.getChild("Parameter"));
	}

	public ConfigAction(Parameter p) {
		this.parameter = p;
	}

	public static ConfigAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("ConfigAction")) {
			return new ConfigAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#toElement()
	 */
	public Element toElement() {
		Element e = new Element ("ConfigAction");
		e.addContent(parameter.toElement());
		return e;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the parameter
	 */
	public Parameter getParameter() {
		return parameter;
	}

	/**
	 * @param parameter the parameter to set
	 */
	public void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getDetails()
	 */
	public String getDetails() {
		return parameter.getName()+"("+parameter.getType()+") = "+parameter.getValue();
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getType()
	 */
	public String getType() {
		return "Set Config Value";
	}
}
