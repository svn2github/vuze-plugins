package lbms.plugins.azcron.main.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.ConfigAction;
import lbms.plugins.azcron.actions.Parameter;
import lbms.plugins.azcron.actions.Parameter.Type;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * @author OMSchaub
 *
 */
public class ConfigActionEditor extends Composite implements ActionEditor{

	protected Combo typeCombo;
	protected Composite valueComp;
	protected String configKey, configValue;


	public ConfigActionEditor(Composite parent) {
		super(parent, SWT.None);
		setLayout(new GridLayout(1,false));
		setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

/*		Label label = new Label(this,SWT.NONE);
		label.setText("Config Action");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		typeCombo = new Combo(this,SWT.READ_ONLY | SWT.DROP_DOWN);
		typeCombo.add("Boolean");
		typeCombo.add("String");
		typeCombo.add("Integer");
		typeCombo.add("Float");
		typeCombo.add("Long");
		typeCombo.select(0);

		Composite keyComp = new Composite(this,SWT.NULL);
		keyComp.setLayout(new GridLayout(2,false));
		keyComp.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		Label keyLabel = new Label(keyComp, SWT.NULL);
		keyLabel.setText("Config Key:");

		final Text textKey = new Text(keyComp, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData();
		gd.widthHint = 150;
		textKey.setLayoutData(gd);

		textKey.addModifyListener(new ModifyListener(){

			public void modifyText(ModifyEvent arg0) {
				configKey = textKey.getText();
			}

		});

		valueComp = new Composite(this,SWT.NULL);
		valueComp.setLayout(new GridLayout(2,false));
		valueComp.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		drawValueArea();


		typeCombo.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent arg0) {
				// Only called on a combo when ENTER is pressed
			}

			public void widgetSelected(SelectionEvent arg0) {
				drawValueArea();

			}

		});*/
		drawValueArea();
	}


	public void drawValueArea(){
		if(typeCombo == null || typeCombo.isDisposed() || valueComp == null || valueComp.isDisposed()) return;

		//first Label in valueComp
		Label label = new Label(valueComp,SWT.NULL);
		label.setText("Config Value:");

		switch (typeCombo.getSelectionIndex()){

		//boolean
		case 0:
			final Combo combo = new Combo(valueComp, SWT.READ_ONLY | SWT.DROP_DOWN);
			combo.add("true");
			combo.add("false");



			combo.addSelectionListener(new SelectionListener(){

				public void widgetDefaultSelected(SelectionEvent arg0) {
					// Just ENTER.. do not use
				}

				public void widgetSelected(SelectionEvent arg0) {
					try{
						configValue = combo.getText();
					}catch (Exception e){
						e.printStackTrace();
					}

				}

			});

			break;

		//String
		case 1:
			final Text text = new Text(valueComp, SWT.BORDER | SWT.SINGLE);
			GridData gd = new GridData();
			gd.widthHint = 150;
			text.setLayoutData(gd);

			if(configValue != null) text.setText(configValue);

			text.addModifyListener(new ModifyListener(){

				public void modifyText(ModifyEvent arg0) {
					configValue = text.getText();
				}

			});

			break;

		//Integer
		case 2:
			final Text intText = new Text(valueComp, SWT.BORDER | SWT.SINGLE);
			gd = new GridData();
			gd.widthHint = 150;
			intText.setLayoutData(gd);

			if(configValue != null) intText.setText(configValue);

			intText.addListener (SWT.Verify, new Listener () {
				public void handleEvent (Event e) {
					String string = e.text;
					char [] chars = new char [string.length ()];
					string.getChars (0, chars.length, chars, 0);
					for (int i=0; i<chars.length; i++) {
						if (!('0' <= chars [i] && chars [i] <= '9')) {
							e.doit = false;
							return;
						}
					}
				}
			});


			intText.addModifyListener(new ModifyListener(){

				public void modifyText(ModifyEvent arg0) {
					configValue = intText.getText();
				}

			});

			break;


		//Float
		case 3:
			final Text floatText = new Text(valueComp, SWT.BORDER | SWT.SINGLE);
			gd = new GridData();
			gd.widthHint = 150;
			floatText.setLayoutData(gd);
			if(configValue != null) floatText.setText(configValue);

			floatText.addListener (SWT.Verify, new Listener () {
				public void handleEvent (Event e) {
					String string = e.text;
					char [] chars = new char [string.length ()];
					string.getChars (0, chars.length, chars, 0);
					for (int i=0; i<chars.length; i++) {
						if(chars[i] == '.'){
							return;
						}else if (!('0' <= chars [i] && chars [i] <= '9')) {
							e.doit = false;
							return;
						}
					}
				}
			});


			floatText.addModifyListener(new ModifyListener(){

				public void modifyText(ModifyEvent arg0) {
					configValue = floatText.getText();
				}

			});

			break;
		//Long
		case 4:
			final Text longText = new Text(valueComp, SWT.BORDER | SWT.SINGLE);
			gd = new GridData();
			gd.widthHint = 150;
			longText.setLayoutData(gd);

			if(configValue != null) longText.setText(configValue);

			longText.addListener (SWT.Verify, new Listener () {
				public void handleEvent (Event e) {
					String string = e.text;
					char [] chars = new char [string.length ()];
					string.getChars (0, chars.length, chars, 0);
					for (int i=0; i<chars.length; i++) {
						if(chars[i] == '.'){
							return;
						}else if (!('0' <= chars [i] && chars [i] <= '9')) {
							e.doit = false;
							return;
						}
					}
				}
			});


			longText.addModifyListener(new ModifyListener(){

				public void modifyText(ModifyEvent arg0) {
					configValue = longText.getText();
				}

			});

			break;

		}
		valueComp.layout();

	}


	public Action getAction() throws InvalidActionException {
		switch(typeCombo.getSelectionIndex()){

		//Boolean
		case 0:
			return new ConfigAction(new Parameter(configKey, configValue, Type.Boolean));

		//String
		case 1:
			return new ConfigAction(new Parameter(configKey, configValue, Type.String));

		//Integer
		case 2:
			return new ConfigAction(new Parameter(configKey, configValue, Type.Integer));

		//Float
		case 3:
			return new ConfigAction(new Parameter(configKey, configValue, Type.Float));

		//Long
		case 4:
			return new ConfigAction(new Parameter(configKey, configValue, Type.Long));


		//COConfigurator does not have a Double for the type
		}
		throw new InvalidActionException();
	}

	public void setAction(Action a) throws InvalidActionException {
		if(typeCombo == null || typeCombo.isDisposed()) return;
		if(a instanceof ConfigAction){
			Parameter p = ((ConfigAction) a).getParameter();
			switch(p.getType()){
			case Boolean:
				typeCombo.select(0);
				break;
			case String:
				typeCombo.select(1);
				break;
			case Integer:
				typeCombo.select(2);
				break;
			case Float:
				typeCombo.select(3);
				break;
			case Long:
				typeCombo.select(4);
				break;
			}
			//TODO set the Inputs to the correct values p.getKey(); p.getValue();

		}else{
			throw new InvalidActionException();
		}

	}

}
