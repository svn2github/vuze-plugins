package lbms.plugins.azcron.azureus.gui.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.azureus.actions.AzSpeedAction;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.main.editors.SpeedActionEditor;


import org.eclipse.swt.widgets.Composite;

public class AzSpeedActionEditor extends SpeedActionEditor {

	public AzSpeedActionEditor(Composite parent) {
		super(parent);
	}

	public Action getAction() throws InvalidActionException {
		return new AzSpeedAction(uploadSpinner.getSelection(), downloadSpinner.getSelection());
	}
}
