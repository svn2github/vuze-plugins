package lbms.plugins.azcron.main.editors;

import lbms.plugins.azcron.actions.Parameter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ParameterControl extends Composite {

	private Parameter p;
	private Label typeName;
	private Text tValue;
	private Button checkbox;

	public ParameterControl(Composite parent, Parameter param) {
		super(parent, SWT.NULL);
		this.p = new Parameter(param);
		if (p.getType().equals(Parameter.Type.String) || p.getType().equals(Parameter.Type.ParsedString)) {
			setLayout(new GridLayout(4,false));
		} else {
			setLayout(new GridLayout(2,false));
		}
		typeName = new Label(this,SWT.NONE);
		typeName.setText(p.getType().toString());
		if (p.getType() == Parameter.Type.Boolean) {
			checkbox = new Button(this,SWT.CHECK);
			checkbox.setSelection(Boolean.parseBoolean(p.getValue()));
		} else {
			tValue = new Text(this,SWT.BORDER);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			tValue.setLayoutData(gd);
			tValue.setText(p.getValue());
		}
		if (p.getType() == Parameter.Type.String || p.getType() == Parameter.Type.ParsedString) {
			String toolTip = "If enabled the String will be parsed for %dateformat%,\nwhere dateformat represents Java SimpleDateformat"
				+"\neg. yyyy-MM-dd or HH:mm:ss"
				+"\nhttp://java.sun.com/j2se/1.5.0/docs/api/java/text/SimpleDateFormat.html";

			Label parseStringLabel = new Label(this,SWT.NONE);
			parseStringLabel.setText("Enable date-format");
			parseStringLabel.setToolTipText(toolTip);

			final Button parseString = new Button(this,SWT.CHECK);
			parseString.setToolTipText(toolTip);

			parseString.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (parseString.getSelection()) {
						p.setType(Parameter.Type.ParsedString);
					} else {
						p.setType(Parameter.Type.String);
					}
				}
			});
			parseString.setSelection(p.getType() == Parameter.Type.ParsedString);
		}
	}

	public Parameter getParameter () {
		if (p.getType() == Parameter.Type.Boolean) {
			p.setValue(Boolean.toString(checkbox.getSelection()));
		} else {
			p.setValue(tValue.getText());
		}
		return new Parameter(p);
	}
}
