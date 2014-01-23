package lbms.plugins.azcron.actions;

import org.jdom.Element;

/**
 * @author Damokles
 *
 */
public interface Action extends Runnable{
	public static final Action[] EMPTY_ARRAY = new Action [0];

	public Element toElement();

	public String getDetails();

	public String getType();
}
