package lbms.plugins.azcron.main;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.ConfigAction;
import lbms.plugins.azcron.actions.IPCAction;
import lbms.plugins.azcron.actions.JythonAction;
import lbms.plugins.azcron.actions.PauseResumeAction;
import lbms.plugins.azcron.actions.RestartAction;
import lbms.plugins.azcron.actions.SpeedAction;
import lbms.plugins.azcron.actions.StartStopAction;
import lbms.plugins.azcron.main.editors.ActionEditor;
import lbms.plugins.azcron.main.editors.ConfigActionEditor;
import lbms.plugins.azcron.main.editors.InvalidActionException;
import lbms.plugins.azcron.main.editors.JythonActionEditor;
import lbms.plugins.azcron.main.editors.PauseResumeActionEditor;
import lbms.plugins.azcron.main.editors.RestartActionEditor;
import lbms.plugins.azcron.main.editors.SpeedActionEditor;
import lbms.plugins.azcron.main.editors.StartStopActionEditor;
import lbms.plugins.azcron.service.Task;
import lbms.plugins.azcron.service.TaskService;
import lbms.plugins.azcron.service.Task.EditMode;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class CronInputGUI {

	// GUI Declerations
	private Shell			shell;

	protected Display		display;
	private Composite		mainComp, inputComp, easyInputComp,
			expertInputComp;

	private ToolItem		addItem, editItem, deleteItem;

	private Table			actionListTable;

	private Text			name;

	private Combo			easyCombo;
	private Label			easyComboLabel;

	protected TaskService	taskService;
	protected Task			task;

	private boolean			bIsNew	= true;

	private StackLayout		stackLayout;

	// AE GUI stuff
	private Shell			aeshell;
	private Composite		actionComp, aeMainComp;
	private boolean			bisEdit	= false;
	private Button			brunOnce, bshowPopup;

	/*
	 * Allows us to come into the editor with a task already -- editing
	 */
	public CronInputGUI (Display _display, final CronGUI tmGUI,
			TaskService _taskService, final Task _task) {
		createContents(_display, tmGUI, _taskService, _task);
	}

	private void createContents (Display _display, final CronGUI tmGUI,
			TaskService _taskService, final Task _task) {
		display = _display;

		taskService = _taskService;
		if (_task != null) {
			bIsNew = false;
			task = new Task(_task.toElement(), true); // copy the task
		} else {
			task = new Task("noname", "0", "*", "*", "*", "*");
			task.setEditMode(EditMode.Basic_Hour);
		}

		// Main Shell
		shell = new Shell(display, SWT.DIALOG_TRIM);
		shell.setLayout(new GridLayout(1, false));
		shell.setText("Task Editor");
		// shell.setSize(500,600);

		shell.addDisposeListener(new DisposeListener() {

			public void widgetDisposed (DisposeEvent arg0) {
				tmGUI.clearMainTable();
				tmGUI.lockButtons(false);
			}
		});

		// Main Composite to put everything on
		mainComp = new Composite(shell, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		mainComp.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 500;
		// gd.heightHint = 600;
		mainComp.setLayoutData(gd);

		// Name
		Label nameLabel = new Label(mainComp, SWT.NULL);
		nameLabel.setText("Task Name:");

		name = new Text(mainComp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		name.setLayoutData(gd);

		if (!bIsNew) {
			name.setText(task.getTaskName());
		}

		// Label
		Label inputModeLabel = new Label(mainComp, SWT.NULL);
		inputModeLabel.setText("Input Mode");

		// Combo
		final Combo inputModeCombo = new Combo(mainComp, SWT.READ_ONLY);
		inputModeCombo.add("Easy Input Mode");
		inputModeCombo.add("Expert Input Mode");
		inputModeCombo.select(0);
		inputModeCombo.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (inputModeCombo.getSelectionIndex() == 0) {
					stackLayout.topControl = easyInputComp;
					inputComp.layout();
				} else if (inputModeCombo.getSelectionIndex() == 1) {
					stackLayout.topControl = expertInputComp;
					inputComp.layout();
					task.setEditMode(EditMode.Expert);
				}

			}
		});

		if (task.getEditMode() == EditMode.Expert) {
			inputModeCombo.select(1);
		} else {
			inputModeCombo.select(0);
		}

		// Input comp.. to be filled out later
		inputComp = new Composite(mainComp, SWT.BORDER);

		// New Stackedlayout
		stackLayout = new StackLayout();
		inputComp.setLayout(stackLayout);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		inputComp.setLayoutData(gd);

		// make the comps
		easyInputComp = makeEasyInput();
		expertInputComp = makeExpertInput();

		if (task.getEditMode() == EditMode.Expert) {
			stackLayout.topControl = expertInputComp;
		} else {
			stackLayout.topControl = easyInputComp;
		}

		Composite advComp = new Composite(mainComp, SWT.BORDER);
		advComp.setLayout(new GridLayout(1, false));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		advComp.setLayoutData(gd);

		brunOnce = new Button(advComp, SWT.CHECK);
		brunOnce.setText("Run task only once");

		if (!bIsNew) {
			brunOnce.setSelection(task.isRunOnce());
		} else {
			brunOnce.setSelection(false);
		}
		brunOnce.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				task.setRunOnce(brunOnce.getSelection());
			}
		});

		bshowPopup = new Button(advComp, SWT.CHECK);
		bshowPopup.setText("Show Popup Notification for when this task runs");

		if (!bIsNew) {
			bshowPopup.setSelection(task.isPopupNotify());
		} else {
			bshowPopup.setSelection(false);
		}
		bshowPopup.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				task.setPopupNotify(bshowPopup.getSelection());
			}
		});

		// Toolbar for actions done to table
		ToolBar tb = new ToolBar(mainComp, SWT.FLAT | SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		tb.setLayoutData(gd);

		// ToolBar Items: addItem, editItem, deleteItem
		addItem = new ToolItem(tb, SWT.PUSH);
		addItem.setImage(ImageRepository.getImage("add"));
		addItem.setToolTipText("Add New Action to this Task");
		addItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				ActionEditorGUI(display, task, null);
			}
		});

		editItem = new ToolItem(tb, SWT.PUSH);
		editItem.setImage(ImageRepository.getImage("edit"));
		editItem.setToolTipText("Edit Selected Action");
		editItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				try {
					TableItem[] items = actionListTable.getSelection();
					if (items.length == 1) {
						Action a = (Action) items[0].getData();
						ActionEditorGUI(display, task, a);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		deleteItem = new ToolItem(tb, SWT.PUSH);
		deleteItem.setImage(ImageRepository.getImage("delete"));
		deleteItem.setToolTipText("Delete Selected Action(s)");
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				TableItem[] items = actionListTable.getSelection();
				if (items.length == 0) {
					return;
				} else {
					for (TableItem item : items) {
						try {
							Action action = (Action) item.getData();
							if (action != null) {
								task.removeAction(action);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
					clearTable();
				}

			}
		});

		// Table that shows Action List items
		actionListTable = new Table(mainComp, SWT.BORDER | SWT.VIRTUAL
				| SWT.V_SCROLL | SWT.MULTI);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.heightHint = 100;
		actionListTable.setLayoutData(gd);
		actionListTable.setHeaderVisible(true);

		final TableColumn colType = new TableColumn(actionListTable, SWT.LEFT);
		colType.setText("Action Type");
		colType.setWidth(175);

		final TableColumn colDetails = new TableColumn(actionListTable,
				SWT.LEFT);
		colDetails.setText("Action Details");
		colDetails.setWidth(225);

		actionListTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent (Event e) {
				TableItem item = (TableItem) e.item;
				int index = actionListTable.indexOf(item);

				Action[] actions = task.getActions();

				item.setText(new String[] { actions[index].getType(),
						actions[index].getDetails() });

				item.setData(actions[index]);
				if (index % 2 == 0) {
					item.setBackground(ColorUtilities.getBackgroundColor(display));
				}

			}
		});

		actionListTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				TableItem[] items = actionListTable.getSelection();
				if (items.length == 0) {
					setToolBarIcons(false, false);
				} else if (items.length == 1) {
					setToolBarIcons(true, true);
				} else if (items.length > 1) {
					setToolBarIcons(false, true);
				}
			}
		});

		actionListTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown (MouseEvent e) {
				if (e.button == 1) {
					if (actionListTable.getItem(new Point(e.x, e.y)) == null) {
						actionListTable.deselectAll();
						setToolBarIcons(false, false);
					}

				}
			}

			// Edit on doubleclick
			@Override
			public void mouseDoubleClick (MouseEvent e) {
				try {
					TableItem[] items = actionListTable.getSelection();
					if (items.length == 1) {
						Action a = (Action) items[0].getData();
						ActionEditorGUI(display, task, a);
					}
				} catch (Exception f) {
					f.printStackTrace();
				}
			}
		});

		Composite buttonComp = new Composite(mainComp, SWT.NULL);
		buttonComp.setLayout(new GridLayout(2, false));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttonComp.setLayoutData(gd);

		Button save = new Button(buttonComp, SWT.PUSH);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.grabExcessHorizontalSpace = true;
		save.setLayoutData(gd);

		save.setText("Save");
		save.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				if (name.getText() == null || name.getText().equals("")) {
					MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR
							| SWT.OK);
					mb.setText("Error");
					mb.setMessage("The Task must be given a name, please check your settings and try again");
					mb.open();
					return;
				} else {
					if (!bIsNew) {
						taskService.removeTask(_task);
					}
					task.setTaskName(name.getText());
					taskService.addTask(task);
					shell.dispose();
				}

			}
		});

		Button cancel = new Button(buttonComp, SWT.PUSH);
		gd = new GridData();
		cancel.setLayoutData(gd);
		cancel.setText("Cancel");
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				shell.dispose();
			}
		});

		clearTable();

		// Center and open shell
		centerShellandOpen(shell);

		// turn off the icons until the user selects something
		setToolBarIcons(false, false);
	}

	/**
	 * Enables/disables the toolbar edit and delete icons
	 *
	 * @param bEdit
	 * @param bDelete
	 */
	private void setToolBarIcons (final boolean bEdit, final boolean bDelete) {
		if (display == null || !display.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {

			@Override
			public void runSafe () {
				if (editItem != null || !editItem.isDisposed()) {
					editItem.setEnabled(bEdit);
				}
				if (deleteItem != null || !deleteItem.isDisposed()) {
					deleteItem.setEnabled(bDelete);
				}
			}

		});
	}

	private Composite makeEasyInput () {
		// remake the composite
		Composite nestedComp = new Composite(inputComp, SWT.NULL);
		nestedComp.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		nestedComp.setLayoutData(gd);

		Composite settings = new Composite(nestedComp, SWT.NULL);
		settings.setLayout(new GridLayout(2, false));
		settings.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Settings
		Label mainLabel = new Label(settings, SWT.NULL);
		mainLabel.setText("I want my task to run");

		final Combo whenCombo = new Combo(settings, SWT.FLAT | SWT.READ_ONLY);
		whenCombo.add("Once an hour");
		whenCombo.add("Once a day");
		whenCombo.add("Once a week");
		whenCombo.add("Once a month");

		whenCombo.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				try {
					if (task != null) {
						switch (whenCombo.getSelectionIndex()) {
						case 0:
							task.setEditMode(EditMode.Basic_Hour);
							easyComboLabel.setText("Choose which minute during the hour to run task");
							easyCombo.removeAll();
							for (int i = 0; i <= 59; i++) {
								easyCombo.add(String.valueOf(i));
							}
							easyCombo.setVisibleItemCount(10);
							easyCombo.select(0);
							task.setMinutes("0");
							task.setHours("*");
							task.setDaysOfWeek("*");
							task.setMonths("*");
							task.setDaysOfMonth("*");
							break;

						case 1:
							task.setEditMode(EditMode.Basic_Day);
							easyComboLabel.setText("Choose which hour during the day to run task");
							easyCombo.removeAll();
							for (int i = 0; i <= 23; i++) {
								easyCombo.add(String.valueOf(i));
							}
							easyCombo.setVisibleItemCount(10);
							easyCombo.select(0);
							task.setMinutes("0");
							task.setHours("0");
							task.setDaysOfWeek("*");
							task.setMonths("*");
							task.setDaysOfMonth("*");
							break;

						case 2:
							task.setEditMode(EditMode.Basic_Week);
							easyComboLabel.setText("Choose which day during the week to run task");
							easyCombo.removeAll();
							easyCombo.add("Sunday");
							easyCombo.add("Monday");
							easyCombo.add("Tuesday");
							easyCombo.add("Wednesday");
							easyCombo.add("Thursday");
							easyCombo.add("Friday");
							easyCombo.add("Saturday");
							easyCombo.setVisibleItemCount(10);
							easyCombo.select(0);
							task.setMinutes("0");
							task.setHours("0");
							task.setDaysOfWeek("0");
							task.setMonths("*");
							task.setDaysOfMonth("*");
							break;

						case 3:
							task.setEditMode(EditMode.Basic_Month);
							easyComboLabel.setText("Choose which day during the month to run task");
							easyCombo.removeAll();
							for (int i = 0; i <= 30; i++) {
								easyCombo.add(String.valueOf(i + 1));
							}
							easyCombo.setVisibleItemCount(10);
							easyCombo.select(0);
							task.setMinutes("0");
							task.setHours("0");
							task.setDaysOfWeek("*");
							task.setMonths("*");
							task.setDaysOfMonth("0");
							break;

						}

					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		Composite subComp = new Composite(settings, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		subComp.setLayoutData(gd);
		subComp.setLayout(new GridLayout(2, false));

		easyComboLabel = new Label(subComp, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		easyComboLabel.setLayoutData(gd);

		easyCombo = new Combo(subComp, SWT.BORDER | SWT.READ_ONLY
				| SWT.DROP_DOWN);
		gd = new GridData(GridData.BEGINNING);
		gd.widthHint = 100;
		easyCombo.setLayoutData(gd);
		easyCombo.setVisibleItemCount(10);

		try {
			if (task != null) {
				EditMode em = task.getEditMode();
				if (em == EditMode.Basic_Hour) {
					whenCombo.select(0);
					easyComboLabel.setText("Choose which minute during the hour to run task");
					for (int i = 0; i <= 59; i++) {
						easyCombo.add(String.valueOf(i));
					}
					easyCombo.setVisibleItemCount(10);
					easyCombo.select(Integer.parseInt(task.getMinutes()));
				} else if (em == EditMode.Basic_Day) {
					whenCombo.select(1);
					easyComboLabel.setText("Choose which hour during the day to run task");
					for (int i = 0; i <= 23; i++) {
						easyCombo.add(String.valueOf(i));
					}
					easyCombo.setVisibleItemCount(10);
					easyCombo.select(Integer.parseInt(task.getHours()));
				} else if (em == EditMode.Basic_Week) {
					whenCombo.select(2);
					easyComboLabel.setText("Choose which day during the week to run task");
					easyCombo.add("Sunday");
					easyCombo.add("Monday");
					easyCombo.add("Tuesday");
					easyCombo.add("Wednesday");
					easyCombo.add("Thursday");
					easyCombo.add("Friday");
					easyCombo.add("Saturday");
					easyCombo.setVisibleItemCount(10);
					easyCombo.select(Integer.parseInt(task.getDaysOfWeek()));
				} else if (em == EditMode.Basic_Month) {
					whenCombo.select(3);
					easyComboLabel.setText("Choose which day during the month to run task");
					easyComboLabel.setText("Choose which hour during the day to run task");
					for (int i = 0; i <= 30; i++) {
						easyCombo.add(String.valueOf(i + 1));
					}
					easyCombo.setVisibleItemCount(10);
					easyCombo.select(Integer.parseInt(task.getDaysOfMonth()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		easyCombo.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				try {
					if (task != null) {
						EditMode em = task.getEditMode();
						if (em == EditMode.Basic_Hour) {
							task.setMinutes(String.valueOf(easyCombo.getSelectionIndex()));
							task.setHours("*");
							task.setDaysOfWeek("*");
							task.setMonths("*");
							task.setDaysOfMonth("*");
						} else if (em == EditMode.Basic_Day) {
							task.setMinutes("0");
							task.setHours(String.valueOf(easyCombo.getSelectionIndex()));
							task.setDaysOfWeek("*");
							task.setMonths("*");
							task.setDaysOfMonth("*");
						} else if (em == EditMode.Basic_Week) {
							task.setMinutes("0");
							task.setHours("0");
							task.setDaysOfWeek(String.valueOf(easyCombo.getSelectionIndex()));
							task.setMonths("*");
							task.setDaysOfMonth("*");
						} else if (em == EditMode.Basic_Month) {
							task.setMinutes("0");
							task.setHours("0");
							task.setDaysOfWeek("*");
							task.setMonths("*");
							task.setDaysOfMonth(String.valueOf(easyCombo.getSelectionIndex()));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		return nestedComp;
	}

	private Composite makeExpertInput () {
		Composite nestedComp = new Composite(inputComp, SWT.NULL);
		nestedComp.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		nestedComp.setLayoutData(gd);

		Composite comp = new Composite(nestedComp, SWT.NULL);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Listener verfiyListener = new Listener() {
			public void handleEvent (Event e) {
				String string = e.text;
				char[] chars = new char[string.length()];
				string.getChars(0, chars.length, chars, 0);
				for (int i = 0; i < chars.length; i++) {
					if (chars.length == 1
							&& (chars[i] == '*' || chars[i] == '/'
									|| chars[i] == ',' || chars[i] == '-')) {
						return;
					} else if (!('0' <= chars[i] && chars[i] <= '9')) {
						e.doit = false;
						return;
					}
				}
			}
		};
		// --minutes

		Label minuteL = new Label(comp, SWT.NULL);
		minuteL.setText("Minutes");

		final Text minuteT = new Text(comp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		minuteT.setLayoutData(gd);
		minuteT.setText(task.getMinutes());
		minuteT.addListener(SWT.Verify, verfiyListener);

		minuteT.addListener(SWT.Modify, new Listener() {
			public void handleEvent (Event arg0) {
				if (task != null) {
					try {
						task.setMinutes(minuteT.getText());
						task.setEditMode(EditMode.Expert);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		Label minuteL2 = new Label(comp, SWT.NULL);
		minuteL2.setText("* for all or 0-59");

		// --hour

		Label hourL = new Label(comp, SWT.NULL);
		hourL.setText("Hours");

		final Text hourT = new Text(comp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		hourT.setLayoutData(gd);
		hourT.setText(task.getHours());
		hourT.addListener(SWT.Verify, verfiyListener);

		hourT.addListener(SWT.Modify, new Listener() {
			public void handleEvent (Event arg0) {
				if (task != null) {
					try {
						task.setHours(hourT.getText());
						task.setEditMode(EditMode.Expert);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		Label hourL2 = new Label(comp, SWT.NULL);
		hourL2.setText("* for all or 0-23");

		// --dom

		Label domL = new Label(comp, SWT.NULL);
		domL.setText("Days of the Month");

		final Text domT = new Text(comp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		domT.setLayoutData(gd);
		domT.setText(task.getDaysOfMonth());
		domT.addListener(SWT.Verify, verfiyListener);

		domT.addListener(SWT.Modify, new Listener() {
			public void handleEvent (Event arg0) {
				if (task != null) {
					try {
						task.setDaysOfMonth(domT.getText());
						task.setEditMode(EditMode.Expert);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		Label domL2 = new Label(comp, SWT.NULL);
		domL2.setText("* for all or 0-30");

		// --month

		Label monthL = new Label(comp, SWT.NULL);
		monthL.setText("Months");

		final Text monthT = new Text(comp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		monthT.setLayoutData(gd);
		monthT.setText(task.getMonths());
		monthT.addListener(SWT.Verify, verfiyListener);

		monthT.addListener(SWT.Modify, new Listener() {
			public void handleEvent (Event arg0) {
				if (task != null) {
					try {
						task.setMonths(monthT.getText());
						task.setEditMode(EditMode.Expert);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		Label monthL2 = new Label(comp, SWT.NULL);
		monthL2.setText("* for all or 0-11");

		// --DoW

		Label dowL = new Label(comp, SWT.NULL);
		dowL.setText("Days of the Week");

		final Text dowT = new Text(comp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		dowT.setLayoutData(gd);
		dowT.setText(task.getDaysOfWeek());
		dowT.addListener(SWT.Verify, verfiyListener);

		dowT.addListener(SWT.Modify, new Listener() {
			public void handleEvent (Event arg0) {
				if (task != null) {
					try {
						task.setDaysOfWeek(dowT.getText());
						task.setEditMode(EditMode.Expert);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		Label dowL2 = new Label(comp, SWT.NULL);
		dowL2.setText("* for all or 0-6 (0 is Sunday)");

		Label description = new Label(comp, SWT.NULL);
		description.setText("You can use Cron syntax for all fields:"
				+ "\n* for every tick (0,1,2,3...)"
				+ "\n*/n for every nth tick "
				+ "\n    e.g. */2 every second tick (0,2,4,...)"
				+ "\nx-y a range whereas x<y "
				+ "\n    e.g. 4-12 every tick between 4 and 12 (4,5,...,12)"
				+ "\nCombinations are also possible"
				+ "\n    e.g. 4-12/2 every 2nd tick between 4 and 12 (4,6,...,12)"
				+ "\na,b,c is also possible"
				+ "\n    You can list any number of items but note a<b<c e.g. 1,12,30 (1,12,30)"
				+ "\nFinally 1,2-20/2,30-42/3,50 is also possible.");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		description.setLayoutData(gd);

		return nestedComp;
	}

	// Public method to clear and redraw the table
	public void clearTable () {
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new SWTSafeRunnable() {

			@Override
			public void runSafe () {
				if (task == null) {
					return;
				}
				if (actionListTable != null || !actionListTable.isDisposed()) {
					actionListTable.clearAll();
					actionListTable.setItemCount(task.getActions().length);
				}
			}
		});
	}

	/**
	 * Centers a Shell and opens it relative to the users Monitor
	 *
	 * @param shell
	 */

	public void centerShellandOpen (Shell shell) {
		// open shell
		shell.pack();

		// Center Shell
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);

		// open shell
		shell.open();
	}

	// --------------------Action Editor -----------------------\\

	/**
	 * Action Editor GUI
	 */
	private void ActionEditorGUI (Display display, final Task task,
			final Action _action) {

		// check for edit action or new action
		if (_action != null) {
			bisEdit = true;
		} else {
			bisEdit = false;
		}

		aeshell = new Shell(display);
		aeshell.setLayout(new GridLayout(1, false));
		aeshell.setText("Action Editor");
		// aeshell.setSize(500,600);
		aeshell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed (DisposeEvent arg0) {
				clearTable();
			}
		});

		Composite buttonComp = new Composite(aeshell, SWT.NULL);
		buttonComp.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttonComp.setLayoutData(gd);

		Button save = new Button(buttonComp, SWT.PUSH);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.grabExcessHorizontalSpace = true;
		save.setLayoutData(gd);
		save.setText("Save");
		save.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				try {
					// if edit, we need to remove _action first from the task,
					// then add in the new one
					if (bisEdit) {
						task.removeAction(_action);
					}
					task.addAction(((ActionEditor) actionComp).getAction());

				} catch (InvalidActionException e) {
					e.printStackTrace();
				}

				aeshell.close();
			}
		});

		Button cancel = new Button(buttonComp, SWT.PUSH);
		gd = new GridData();
		cancel.setLayoutData(gd);
		cancel.setText("Cancel");
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				aeshell.close();
			}
		});

		// Main Composite to put everything on
		aeMainComp = new Composite(aeshell, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		aeMainComp.setLayout(gl);
		gd = new GridData(GridData.FILL_BOTH);
		// gd.widthHint = 500;
		// gd.heightHint = 600;
		aeMainComp.setLayoutData(gd);

		// label
		Label label = new Label(aeMainComp, SWT.NULL);
		label.setText("Select type of Action:");

		// main combo for selecting action type
		final Combo combo = new Combo(aeMainComp, SWT.FLAT | SWT.READ_ONLY);
		combo.add("Start / Stop Torrents");
		combo.add("Pause / Resume Torrents");
		combo.add("Set Global Upload / Download Speeds");
		combo.add("Restart Azureus");
		combo.add("Set Azureus Core Setting");
		combo.add("Use InterPluginCommunication to Interact With Another Plugin");
		combo.add("Execute Jython Script");
		combo.setVisibleItemCount(7);
		combo.select(0);
		try {
			actionComp.dispose();
			aeMainComp.layout();
		} catch (Exception e) {
		}

		// surround with try catch just incase check for null results in error
		try {
			if (_action != null) {
				if (_action instanceof StartStopAction) {
					combo.select(0);
				} else if (_action instanceof PauseResumeAction) {
					combo.select(1);
				} else if (_action instanceof SpeedAction) {
					combo.select(2);
				} else if (_action instanceof RestartAction) {
					combo.select(3);
				} else if (_action instanceof ConfigAction) {
					combo.select(4);
				} else if (_action instanceof IPCAction) {
					combo.select(5);
				} else if (_action instanceof JythonAction) {
					combo.select(6);
				}

				try {
					actionComp.dispose();
					aeMainComp.layout();
				} catch (Exception e) {
				}

				try {
					actionComp = setActionEditor(combo.getSelectionIndex(),
							aeMainComp, _action, bisEdit);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				actionComp = setActionEditor(combo.getSelectionIndex(),
						aeMainComp, _action, bisEdit);
			}
		} catch (Exception e) {
		} // no action

		combo.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event arg0) {
				// Action editor loading code here
				try {
					actionComp.dispose();
					aeMainComp.layout();
				} catch (Exception e) {
				}

				try {
					actionComp = setActionEditor(combo.getSelectionIndex(),
							aeMainComp, _action, bisEdit);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// open shell
		centerShellandOpen(aeshell);

	}

	protected Composite setActionEditor (int sel, Composite parent,
			final Action action, boolean bisEdit) {
		Composite target = null;
		switch (sel) {
		case 0:
			target = new StartStopActionEditor(parent);
			break;
		case 1:
			target = new PauseResumeActionEditor(parent);
			break;
		case 2:
			target = new SpeedActionEditor(parent);
			break;
		case 3:
			target = new RestartActionEditor(parent);
			break;
		case 4:
			target = new ConfigActionEditor(parent);
			break;
		case 5:
			// TODO
			break;
		case 6:
			target = new JythonActionEditor(parent);
			break;
		}
		if (bisEdit) {
			try {
				((ActionEditor) target).setAction(action);
			} catch (InvalidActionException e) {
				e.printStackTrace();
			}
		}
		// Finish by relaying out everything
		target.setParent(parent);
		target.layout();
		parent.layout();
		parent.getShell().pack();
		parent.getShell().layout();
		return target;
	}

}// EOF
