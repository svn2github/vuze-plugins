package lbms.plugins.azcron.main.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.SpeedAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class SpeedActionEditor extends Composite implements ActionEditor {


	protected Spinner uploadSpinner, downloadSpinner;

	public SpeedActionEditor(Composite parent) {
		super(parent, SWT.None);
		setLayout(new GridLayout(3,false));

		Label label = new Label(this,SWT.NULL);
		label.setText("Set Upload and Download Speed");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);


		Label downloadSpeed = new Label(this, SWT.NULL);
		downloadSpeed.setText("Set Download Speed To:");

		downloadSpinner = new Spinner(this, SWT.BORDER);
		downloadSpinner.setMinimum(0);
		downloadSpinner.setMaximum(Integer.MAX_VALUE);
		downloadSpinner.setIncrement(1);
		downloadSpinner.setPageIncrement(30);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		downloadSpinner.setLayoutData(gd);

		Label downloadSpeedAfter = new Label(this, SWT.NULL);
		downloadSpeedAfter.setText("kB/s");


		Label uploadSpeed = new Label(this, SWT.NULL);
		uploadSpeed.setText("Set Upload Speed To:");

		uploadSpinner = new Spinner(this, SWT.BORDER);
		uploadSpinner.setMinimum(0);
		uploadSpinner.setMaximum(Integer.MAX_VALUE);
		uploadSpinner.setIncrement(1);
		uploadSpinner.setPageIncrement(30);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		uploadSpinner.setLayoutData(gd);

		Label uploadSpeedAfter = new Label(this, SWT.NULL);
		uploadSpeedAfter.setText("kB/s");


		Label bottomLabel = new Label(this, SWT.NULL);
		bottomLabel.setText("Use 0 for 'Unlimited Speed'");

	}

	public Action getAction() throws InvalidActionException {
		return new SpeedAction(uploadSpinner.getSelection(), downloadSpinner.getSelection());
	}

	public void setAction(Action a) throws InvalidActionException {
		if(a instanceof SpeedAction){
			SpeedAction action = (SpeedAction) a;
			uploadSpinner.setSelection(action.getUl());
			downloadSpinner.setSelection(action.getDl());
		}else{
			throw new InvalidActionException();
		}

	}

}

