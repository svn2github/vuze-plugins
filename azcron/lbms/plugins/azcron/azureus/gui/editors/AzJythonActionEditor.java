package lbms.plugins.azcron.azureus.gui.editors;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.azureus.actions.AzJythonAction;
import lbms.plugins.azcron.main.ImageRepository;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.main.editors.JythonActionEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * @author Damokles
 *
 */
public class AzJythonActionEditor extends JythonActionEditor {

	/**
	 * @param parent
	 */
	public AzJythonActionEditor(Composite parent) {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.azureus.gui.RestartActionEditor#getAction()
	 */
	@Override
	public Action getAction() throws InvalidActionException {
		return new AzJythonAction(scriptFile);
	}



	@Override
	protected void drawText(final Composite parent){
		Composite comp = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(gd);
		comp.setLayout(new GridLayout(2,false));

		pathText = new Text(comp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		pathText.setLayoutData(gd);

		try{
			if(scriptFile != null)
				pathText.setText(scriptFile);
		}catch(Exception e){}

		Button iconButton = new Button(comp, SWT.PUSH);
		iconButton.setImage(ImageRepository.getImage("import"));
		iconButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		iconButton.setToolTipText("Choose Script File");
		iconButton.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event arg0) {
				FileDialog dialog = new FileDialog (parent.getShell(), SWT.OPEN);
				dialog.setFilterNames (new String [] {"All Files (*.*)"});
				dialog.setFilterExtensions (new String [] {"*.*"});
				dialog.setText("Choose Jython Script File");
				String fileString = dialog.open();
				if(fileString != null && !fileString.equals("")){
					scriptFile = fileString;
					pathText.setText(fileString);
				}
			}
		});

	}
}
