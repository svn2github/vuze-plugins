package lbms.plugins.azcron.actions;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class StartStopAction implements Action {

	/**
	 * true  = start torrents
	 * false = pause torrents
	 */
	protected boolean start = false;

	public StartStopAction (boolean start) {
		this.start = start;
	}

	protected StartStopAction (Element e) {
		this.start = Boolean.parseBoolean(e.getAttributeValue("start"));
	}

	public static StartStopAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("StartStopAction")) {
			return new StartStopAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#toElement()
	 */
	public Element toElement() {
		Element e = new Element ("StartStopAction");
		e.setAttribute("start", Boolean.toString(start));
		return e;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO Auto-generated method stub
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(boolean start) {
		this.start = start;
	}

	/**
	 * Returns the start boolean
	 * true = start
	 * false = stop
	 * @return boolean start
	 */
	public boolean getStart(){
		return this.start;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getDetails()
	 */
	public String getDetails() {
		if (start)
			return "Start All Downloads";
		else return "Stop All Downloads";
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getType()
	 */
	public String getType() {
		return "Start or Stop Downloads";
	}
}
