package lbms.plugins.azcron.actions;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public class SpeedAction implements Action {

	protected int ul;
	protected int dl;

	public SpeedAction(int ul, int dl) {
		super();
		this.ul = ul;
		this.dl = dl;
	}

	protected SpeedAction (Element e) {
		this.dl = Integer.parseInt(e.getAttributeValue("dl"));
		this.ul = Integer.parseInt(e.getAttributeValue("ul"));
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getDetails()
	 */
	public String getDetails() {
		return "Upload: "+ul+" Download:"+dl;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#getType()
	 */
	public String getType() {
		return "Set Global Speed Limits";
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.actions.Action#toElement()
	 */
	public Element toElement() {
		Element e = new Element ("SpeedAction");
		e.setAttribute("dl", Integer.toString(dl));
		e.setAttribute("ul", Integer.toString(ul));
		return e;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
	}

	public static SpeedAction createFromElement(Element e) throws ActionSyntaxException {
		if (e.getName().equalsIgnoreCase("SpeedAction")) {
			return new SpeedAction(e);
		} else throw new ActionSyntaxException	("Class name does not match Element name.");
	}

	/**
	 * @return the dl
	 */
	public int getDl() {
		return dl;
	}

	/**
	 * @return the ul
	 */
	public int getUl() {
		return ul;
	}
}
