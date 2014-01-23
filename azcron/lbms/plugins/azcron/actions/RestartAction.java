package lbms.plugins.azcron.actions;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class RestartAction implements Action {

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#toElement()
	 */
	public Element toElement() {
		Element e = new Element ("RestartAction");
		return e;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO Auto-generated method stub

	}

	public static RestartAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("RestartAction")) {
			return new RestartAction();
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getDetails()
	 */
	public String getDetails() {
		return "";
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getType()
	 */
	public String getType() {
		return "Restart Azureus";
	}
}
