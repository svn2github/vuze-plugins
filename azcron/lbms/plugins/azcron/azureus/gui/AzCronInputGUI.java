package lbms.plugins.azcron.azureus.gui;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.azureus.AzCronPlugin;
import lbms.plugins.azcron.azureus.gui.editors.AzConfigActionEditor;
import lbms.plugins.azcron.azureus.gui.editors.AzIPCActionEditor;
import lbms.plugins.azcron.azureus.gui.editors.AzJythonActionEditor;
import lbms.plugins.azcron.azureus.gui.editors.AzPauseResumeActionEditor;
import lbms.plugins.azcron.azureus.gui.editors.AzRestartActionEditor;
import lbms.plugins.azcron.azureus.gui.editors.AzSpeedActionEditor;
import lbms.plugins.azcron.azureus.gui.editors.AzStartStopActionEditor;
import lbms.plugins.azcron.main.CronGUI;
import lbms.plugins.azcron.main.CronInputGUI;
import lbms.plugins.azcron.main.editors.ActionEditor;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.service.Task;
import lbms.plugins.azcron.service.TaskService;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;


public class AzCronInputGUI extends CronInputGUI{

	public AzCronInputGUI(Display display, final CronGUI tmGUI, TaskService taskService, final Task task) {
		super(display, tmGUI, taskService, task);

	}

	protected Composite setActionEditor(int sel, Composite parent, final Action action, boolean bisEdit){

		Composite target = null;
		switch(sel){
		case 0:
			target = new AzStartStopActionEditor(parent);
			break;
		case 1:
			target = new AzPauseResumeActionEditor(parent);
			break;
		case 2:
			target = new AzSpeedActionEditor(parent);
			break;
		case 3:
			target = new AzRestartActionEditor(parent);
			break;
		case 4:
			target = new AzConfigActionEditor(parent);
			break;
		case 5:
			target = new AzIPCActionEditor(parent,AzCronPlugin.getPluginInterface());
			break;
		case 6:
			target = new AzJythonActionEditor(parent);
			break;
		}

		if(bisEdit){
			try {
				((ActionEditor) target).setAction(action);
			} catch (InvalidActionException e) {
				e.printStackTrace();
			}
		}

		//Finish by relaying out everything
		target.setParent(parent);
		target.layout();
		parent.layout();
		parent.getShell().pack();
		parent.getShell().layout();
		return target;
	}



}
