package lbms.plugins.azcron.main.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.StartStopAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class StartStopActionEditor extends Composite implements ActionEditor {

	protected Combo combo;


	public StartStopActionEditor(Composite parent) {
		super(parent, SWT.None);
		setLayout(new GridLayout(2,false));

		Label label = new Label(this,SWT.NONE);
		label.setText("Start / Stop Action");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		combo = new Combo(this,SWT.READ_ONLY | SWT.DROP_DOWN);
		combo.add("Stop all downloads");
		combo.add("Start all downloads");
		combo.select(0);

	}

	public Action getAction() throws InvalidActionException {
		return new StartStopAction(combo.getSelectionIndex() == 1);
	}

	public void setAction(Action a) throws InvalidActionException {
		if(a instanceof StartStopAction){
			StartStopAction action = (StartStopAction) a;
			if(action.getStart())
				combo.select(1);
			else
				combo.select(0);
		}else{
			throw new InvalidActionException();
		}

	}

}
