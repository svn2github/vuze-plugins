package lbms.plugins.azcron.main.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.RestartAction;
import lbms.plugins.azcron.azureus.actions.AzRestartAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class RestartActionEditor extends Composite implements ActionEditor{

	public RestartActionEditor(Composite parent) {
		super(parent, SWT.None);
		setLayout(new GridLayout(1,false));
		Label label = new Label(this,SWT.NONE);
		label.setText("Restart Action - This Action causes Azureus to Restart");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

	}

	public Action getAction() throws InvalidActionException {
		return new RestartAction();
	}

	public void setAction(Action a) throws InvalidActionException {

	}

}
