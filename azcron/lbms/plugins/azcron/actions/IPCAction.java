package lbms.plugins.azcron.actions;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class IPCAction implements Action {

	protected String pluginID;
	protected String methodName;
	protected List<Parameter> params = new ArrayList<Parameter>();

	public IPCAction(String pluginID, String methodName, List<Parameter> params) {
		super();
		this.pluginID = pluginID;
		this.methodName = methodName;
		this.params = params;
	}

	protected IPCAction (Element e) {
		this.pluginID = e.getAttributeValue("pluginID");
		this.methodName = e.getAttributeValue("methodName");
		List<Element> para = e.getChildren("Parameter");
		for (Element p : para) {
			params.add(Parameter.createFromElement(p));
		}
	}

	public static IPCAction createFromElement(Element e)throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("IPCAction")) {
			return new IPCAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#toElement()
	 */
	public Element toElement() {
		Element e = new Element ("IPCAction");
		e.setAttribute("pluginID", pluginID);
		e.setAttribute("methodName", methodName);
		for (Parameter p:params) {
			e.addContent(p.toElement());
		}
		return e;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the methodName
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @param methodName the methodName to set
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * @return the params
	 */
	public List<Parameter> getParams() {
		return params;
	}

	/**
	 * @param params the params to set
	 */
	public void setParams(List<Parameter> params) {
		this.params = params;
	}

	/**
	 * @return the pluginID
	 */
	public String getPluginID() {
		return pluginID;
	}

	/**
	 * @param pluginID the pluginID to set
	 */
	public void setPluginID(String pluginID) {
		this.pluginID = pluginID;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getDetails()
	 */
	public String getDetails() {
		String result = pluginID+"."+methodName+" (";
		for (Parameter p : params) {
			result += p.getValue()+ ", ";
		}
		if (result.endsWith(", ")) {
			result = result.substring(0, result.length()-2);
		}
		return result+")";
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getType()
	 */
	public String getType() {
		return "InterPluginCommunication";
	}
}
