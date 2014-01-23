package lbms.plugins.azcron.actions;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class JythonAction implements Action {

	protected String script;
	protected boolean runMethod;
	protected String method;
	protected String params;

	protected JythonAction (Element e) {
		this.script = e.getAttributeValue("script");
		this.runMethod = Boolean.parseBoolean(e.getAttributeValue("runMethod"));
		if (runMethod) {
			this.method = e.getAttributeValue("method");
			this.params = e.getAttributeValue("parms");
		}
	}

	public JythonAction (String scriptFile) {
		this.script = scriptFile;
	}

	public JythonAction (String scriptFile, String method, String params) {
		this.script = scriptFile;
		this.runMethod = true;
		this.method = method;
		this.params = params;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getDetails()
	 */
	public String getDetails() {
		return script;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getType()
	 */
	public String getType() {
		return "Execute Jython Script";
	}

	public static JythonAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("JythonAction")) {
			return new JythonAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#toElement()
	 */
	public Element toElement() {
		Element e = new Element ("JythonAction");
		e.setAttribute("script", script);
		if (runMethod) {
			e.setAttribute("method", method);
			e.setAttribute("params", params);
		}
		return e;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

	}

	/**
	 * @return the script
	 */
	public String getScript() {
		return script;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * @return the params
	 */
	public String getParams() {
		return params;
	}

	/**
	 * @return the runMethod
	 */
	public boolean isRunMethod() {
		return runMethod;
	}
}
