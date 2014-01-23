package lbms.plugins.azcron.azureus.gui.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.azureus.actions.AzPauseResumeAction;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.main.editors.PauseResumeActionEditor;

import org.eclipse.swt.widgets.Composite;

public class AzPauseResumeActionEditor extends PauseResumeActionEditor {

	public AzPauseResumeActionEditor(Composite parent) {
		super(parent);
	}

	public Action getAction() throws InvalidActionException {
		return new AzPauseResumeAction(combo.getSelectionIndex() == 1);
	}
}
