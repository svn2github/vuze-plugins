package lbms.plugins.azcron.main.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import lbms.plugins.azcron.actions.Action;

/**
 * @author Damokles
 *
 */
public class IPCActionEditor extends Composite implements ActionEditor {

	public IPCActionEditor(Composite parent) {
		super(parent, SWT.None);
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.main.ActionEditor#getAction()
	 */
	public Action getAction() throws InvalidActionException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.main.ActionEditor#setAction(lbms.plugins.azcron.actions.Action)
	 */
	public void setAction(Action a) throws InvalidActionException {
		// TODO Auto-generated method stub

	}

}
