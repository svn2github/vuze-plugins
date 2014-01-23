package lbms.plugins.azcron.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

public class InputGUI {

	//GUI Declerations
	private Display display;
	private Composite mainComp;
	//topComp stuff
	private Composite inputComp;
	private Table taskListTable;


public void create(Composite parent){

	//Main Composite to put everything on
	mainComp = new Composite(parent,SWT.NONE);
	GridLayout gl = new GridLayout(1, false);
	mainComp.setLayout(gl);
	GridData gd = new GridData(GridData.FILL_BOTH);
	mainComp.setLayoutData(gd);


	//Sashform
	SashForm sash = new SashForm(mainComp, SWT.VERTICAL);
	sash.setLayout(new GridLayout());
	gd = new GridData(GridData.FILL_BOTH);
	sash.setLayoutData(gd);

	//TopComposite in sash
	final Composite topComp = new Composite(sash,SWT.NULL);
	topComp.setLayout(new GridLayout(3,false));
	gd = new GridData(GridData.FILL_BOTH);
	topComp.setLayoutData(gd);

	//Bottom Composite in sash
	final Composite bottomComp = new Composite(sash,SWT.NULL);
	bottomComp.setLayout(new GridLayout(1,false));
	gd = new GridData(GridData.FILL_BOTH);
	bottomComp.setLayoutData(gd);

	//----TopComposite Items----\\

	//Label
	Label taskSettingsLabel = new Label(topComp, SWT.NULL);
	taskSettingsLabel.setText("Task Settings");

	//Composite that changes based on combo selection
	inputComp = new Composite (topComp, SWT.BORDER);
	gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 3;
	gd.heightHint = 100;
	inputComp.setLayoutData(gd);

	//Table that shows Action List items
	taskListTable = new Table(topComp, SWT.NULL);

	//----BottomComposite Items----\\

	//Combo
	Combo intervalCombo = new Combo(bottomComp, SWT.READ_ONLY);
	intervalCombo.add("Simple Input Mode");
	intervalCombo.add("Expert Input Mode");
	intervalCombo.select(0);
}

}//EOF
