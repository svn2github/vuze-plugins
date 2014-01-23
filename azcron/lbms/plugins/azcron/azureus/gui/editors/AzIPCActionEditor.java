package lbms.plugins.azcron.azureus.gui.editors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.Parameter;
import lbms.plugins.azcron.azureus.actions.AzIPCAction;
import lbms.plugins.azcron.main.editors.ActionEditor;
import lbms.plugins.azcron.main.editors.IPCActionEditor;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.main.editors.ParameterControl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;

public class AzIPCActionEditor extends IPCActionEditor implements ActionEditor {
	private Combo pluginIDs, methodNames;
	private Composite inputComp;
	private List<Method> currentMethods = new ArrayList<Method>();
	private List<ParameterControl> parameterControls = new ArrayList<ParameterControl>();
	private PluginInterface pi;

	public AzIPCActionEditor(Composite parent, final PluginInterface _pi) {
		super(parent);
		pi = _pi;
		this.setLayout(new GridLayout(2,false));
		pluginIDs = new Combo(this,SWT.READ_ONLY);
		pluginIDs.setItems(getPluginIDs(pi));

		methodNames = new Combo (this,SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 500;
		methodNames.setLayoutData(gd);

		pluginIDs.addSelectionListener(new SelectionAdapter () {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fillMethods();
			}
		});

		methodNames.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				Control[] children = inputComp.getChildren();
				for (Control c:children)
					c.dispose();

				parameterControls.clear();

				Method m = currentMethods.get(methodNames.getSelectionIndex());
				if (m!=null) {
					Class[] params = m.getParameterTypes();
					if(params.length == 0) {
						Label label = new Label(inputComp,SWT.NULL);
						label.setText("This method has no assignable parameters");
					} else {
						for (int j=0;j<params.length;j++) {
							Parameter p = new Parameter("","",Parameter.Type.Boolean);
							if (params[j] == int.class) {
								p.setType(Parameter.Type.Integer);
							} else if (params[j] == long.class) {
								p.setType(Parameter.Type.Long);
							} else if (params[j] == float.class) {
								p.setType(Parameter.Type.Float);
							} else if (params[j] == double.class) {
								p.setType(Parameter.Type.Double);
							} else if (params[j] == String.class) {
								p.setType(Parameter.Type.String);
							} else if (params[j] == boolean.class) {
								p.setType(Parameter.Type.Boolean);
							}

							ParameterControl pc = new ParameterControl(inputComp,p);
							parameterControls.add(pc);
							pc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
							pc.layout();

						}
					}

					inputComp.layout();
					inputComp.getParent().layout();
					inputComp.getShell().layout();
				}
			}
		});

		inputComp = new Composite(this,SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.heightHint = 200;
		inputComp.setLayoutData(gd);
		inputComp.setLayout(new GridLayout(1,false));

		Label label = new Label(inputComp, SWT.NULL);
		label.setText("Please choose a plugin and a method");
	}

	public String[] getPluginIDs (PluginInterface pi) {
		PluginInterface[] pIDs = pi.getPluginManager().getPluginInterfaces();
		String[] result = new String [pIDs.length];
		for (int i=0;i<pIDs.length;i++)
			result[i] = pIDs[i].getPluginID();
		return result;
	}

	public String[] getMethodNames (Class cl) {
		Method[] methods = cl.getMethods();
		List<String> result = new ArrayList<String>();
		currentMethods.clear();
		outer:
		for (int i=0;i<methods.length;i++) {
			String tmp = methods[i].getReturnType().getName()+" "+methods[i].getName()+" (";
			Class[] params = methods[i].getParameterTypes();
			for (int j=0;j<params.length;j++) {
				if (!checkValidParam(params[j]))continue outer;
				tmp += params[j].getName();
				if ( j < params.length - 1 )
					tmp += ( ", " );
			}
			tmp+= ")";
			result.add(tmp);
			currentMethods.add(methods[i]);
		}
		return result.toArray(new String[0]);
	}


	private boolean checkValidParam (Class cl) {
		if (cl == int.class || cl == long.class || cl == float.class
			|| cl == double.class || cl == String.class || cl == boolean.class) return true;
		return false;
	}

	public Action getAction() throws InvalidActionException {
		//TODO: get the parameters in
		List<Parameter> params = new ArrayList<Parameter>();
		for (ParameterControl pc:parameterControls) {
			params.add(pc.getParameter());
		}
		AzIPCAction action = new AzIPCAction (pluginIDs.getText(),currentMethods.get(methodNames.getSelectionIndex()).getName(), params);
		return action;
	}

	private void fillMethods() {
		String id = pluginIDs.getText();
		if (!id.equals("")) {
			methodNames.removeAll();
			Plugin target = pi.getPluginManager().getPluginInterfaceByID(id).getPlugin();
			if (target != null)
				methodNames.setItems(getMethodNames(target.getClass()));
		}
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.azureus.gui.ActionEditor#setAction(lbms.plugins.azcron.actions.Action)
	 */
	public void setAction(Action a) throws InvalidActionException {
		if (a instanceof AzIPCAction) {
			AzIPCAction action = (AzIPCAction) a;
			String pID = action.getPluginID();
			String[] pIDs = pluginIDs.getItems();
			for (int i=0;i<pIDs.length;i++) {
				if (pID.equals(pIDs[i])) {
					pluginIDs.select(i);
					fillMethods();
					for (int k=0; k < currentMethods.size(); k++) {
						if (currentMethods.get(k).getName().equals(action.getMethodName())) {
							methodNames.select(k);
							Control[] children = inputComp.getChildren();
							for (Control c:children)
								c.dispose();

							parameterControls.clear();

							Method m = currentMethods.get(k);
							if (m!=null) {
								List<Parameter> params = action.getParams();
								if(params.size() == 0) {
									Label label = new Label(inputComp,SWT.NULL);
									label.setText("This method has no assignable parameters");
								} else {
									for (int j=0;j<params.size();j++) {
										ParameterControl pc = new ParameterControl(inputComp,params.get(j));
										parameterControls.add(pc);
										pc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
										pc.layout();
									}
								}

								inputComp.layout();
								inputComp.getParent().layout();
								inputComp.getShell().layout();
								break;
							}
						}
					}

					break;
				}
			}
		} else {
			throw new InvalidActionException();
		}
	}
}
