package lbms.plugins.azcron.main.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.JythonAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class JythonActionEditor extends Composite implements ActionEditor {

	protected String scriptFile;
	protected Text pathText;

	public JythonActionEditor(Composite parent) {
		super(parent, SWT.None);
		setLayout(new GridLayout(1,false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		this.setLayoutData(gd);


		Label label = new Label(this,SWT.NULL);
		label.setText("Enter path to Jython script file");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		label.setLayoutData(gd);

		drawText(this);

	}

	public Action getAction() throws InvalidActionException {
		return new JythonAction(scriptFile);
	}

	public void setAction(Action a) throws InvalidActionException {
		if(a instanceof JythonAction){
			JythonAction action = (JythonAction) a;
			pathText.setText(action.getScript());
		}else{
			throw new InvalidActionException();
		}

	}


	protected void drawText(final Composite parent){
		pathText = new Text(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		pathText.setLayoutData(gd);

		try{
			if(scriptFile != null)
				pathText.setText(scriptFile);
		}catch(Exception e){}


	}

}

