package lbms.plugins.azcron.azureus.gui.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.azureus.actions.AzRestartAction;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.main.editors.RestartActionEditor;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Damokles
 *
 */
public class AzRestartActionEditor extends RestartActionEditor {

	/**
	 * @param parent
	 */
	public AzRestartActionEditor(Composite parent) {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.azureus.gui.RestartActionEditor#getAction()
	 */
	@Override
	public Action getAction() throws InvalidActionException {
		return new AzRestartAction();
	}
}
