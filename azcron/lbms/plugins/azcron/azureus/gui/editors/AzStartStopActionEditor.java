package lbms.plugins.azcron.azureus.gui.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.azureus.actions.AzStartStopAction;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.main.editors.StartStopActionEditor;

import org.eclipse.swt.widgets.Composite;

public class AzStartStopActionEditor extends StartStopActionEditor {

	public AzStartStopActionEditor(Composite parent) {
		super(parent);
	}

	public Action getAction() throws InvalidActionException {
		return new AzStartStopAction(combo.getSelectionIndex() == 1);
	}
}
