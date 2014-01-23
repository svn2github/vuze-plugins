package lbms.plugins.azcron.azureus.gui;

import lbms.plugins.azcron.azureus.service.AzTask;
import lbms.plugins.azcron.azureus.service.AzTaskService;
import lbms.plugins.azcron.main.ImageRepository;
import lbms.plugins.azcron.main.CronGUI;
import lbms.plugins.azcron.service.Task;
import lbms.plugins.azcron.service.TaskService;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

public class AzCronGUI extends CronGUI implements
		UISWTViewEventListener {

	boolean						isCreated	= false;
	public static final String	VIEWID		= "azcron_View";
	private AzTaskService		azTaskService;
	protected AzCronGUI	azTMGUI;

	private void initialize (Composite parent) {
		ImageRepository.loadImages(parent.getDisplay());
		createContents(parent);

		MenuItem executeTask = new MenuItem(mainMenu, SWT.PUSH);
		executeTask.setText("Execute");
		executeTask.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (SelectionEvent e) {
				TableItem[] items = mainTable.getSelection();
				for (TableItem i : items) {
					if (i.getData() != null && i.getData() instanceof AzTask) {
						((AzTask) i.getData()).run();
					}
				}
			}
		});

		azTMGUI = this;
	}

	public AzCronGUI (AzTaskService _taskService) {
		super(_taskService);
		this.azTaskService = _taskService;
	}

	/**
	 * Delete function... runs at close of plugin from within Azureus
	 */

	public void delete () {
		isCreated = false;
	}

	public boolean eventOccurred (UISWTViewEvent event) {
		switch (event.getType()) {

		case UISWTViewEvent.TYPE_CREATE:
			if (isCreated) {
				return false;
			}
			isCreated = true;
			break;

		case UISWTViewEvent.TYPE_INITIALIZE:
			initialize((Composite) event.getData());
			// If (Plugin.isFirstRun()) {
			// Plugin.setFirstRun(false);
			// StartupWizard.open(((Composite)event.getData()).getDisplay());
			// }
			break;

		case UISWTViewEvent.TYPE_DESTROY:
			delete();
			break;
		}
		return true;
	}

	@Override
	protected void openInputEditorGUI (Display _display, CronGUI tmGUI,
			TaskService _taskService, Task _task) {
		new AzCronInputGUI(_display, tmGUI, azTaskService, _task);

	}

	// Pulls the taskService associated with the main GUI window
	@Override
	protected AzTaskService getTaskService () {
		return azTaskService;
	}

	// Pulls the TaskManagerGUI to send on so that the table can be cleared
	// later
	@Override
	protected AzCronGUI getTaskManagerGUI () {
		return azTMGUI;
	}

}
