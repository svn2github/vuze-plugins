package lbms.plugins.azcron.main;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.service.Task;
import lbms.plugins.azcron.service.TaskService;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class CronGUI {

	private Display display;

	private TaskService taskService;
	protected Composite mainComp;

	protected ToolBar toolBar;
	protected ToolItem addItem, editItem, removeItem, exportItem, importItem;
	protected Menu mainMenu;

	protected Table mainTable;
	protected CronGUI tmGUI;

	protected Task[] tasks;

	public CronGUI (TaskService _taskService) {
		//Set the taskservice
		taskService = _taskService;
		tmGUI = this;
	}

	protected void createContents(Composite parent){
		//Set the display
		display = parent.getDisplay();
		final Shell shell = parent.getShell();

		//Main Composite to put everything on
		mainComp = new Composite(parent,SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		mainComp.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		mainComp.setLayoutData(gd);

		toolBar = new ToolBar(mainComp, SWT.HORIZONTAL | SWT.FLAT);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		toolBar.setLayoutData(gd);



		//Add toolbar button
		addItem = new ToolItem(toolBar, SWT.PUSH);
		addItem.setImage(ImageRepository.getImage("add"));
		addItem.setToolTipText("Add a new Task");
		addItem.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event arg0) {
				if(display == null || display.isDisposed()
						|| getTaskService() == null) return;
				lockButtons(true);
				openInputEditorGUI(display,getTaskManagerGUI(),getTaskService(),null);
			}
		});

		//Edit toolbar button
		editItem = new ToolItem(toolBar, SWT.PUSH);
		editItem.setImage(ImageRepository.getImage("edit"));
		editItem.setToolTipText("Edit Selected Task");
		editItem.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event arg0) {
				if(display == null || display.isDisposed()
						|| getTaskService() == null) return;
				if(mainTable != null || !mainTable.isDisposed()){
					TableItem[] items = mainTable.getSelection();
					if(items.length == 1){
						openInputEditorGUI(display,getTaskManagerGUI(),getTaskService(), (Task)items[0].getData());
						lockButtons(true);
					}
				}
			}
		});


		//remove toolbar button
		removeItem = new ToolItem(toolBar, SWT.PUSH);
		removeItem.setImage(ImageRepository.getImage("delete"));
		removeItem.setToolTipText("Remove Selected Task(s)");
		removeItem.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event arg0) {
				if(mainTable != null || !mainTable.isDisposed()){
					TableItem[] items = mainTable.getSelection();
					if(items.length != 0){
						try{
							for(TableItem item:items){
								Task task = (Task) item.getData();
								getTaskService().removeTask(task);
							}
							clearMainTable();
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}

			}
		});
		//Export toolbar button
		exportItem = new ToolItem(toolBar, SWT.PUSH);
		exportItem.setImage(ImageRepository.getImage("export"));
		exportItem.setToolTipText("Export Tasks to file");
		exportItem.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event arg0) {
				FileDialog dialog = new FileDialog (shell, SWT.SAVE);
				dialog.setText("Save Tasks File");
				dialog.setFilterNames (new String [] {"xml Files", "All Files (*.*)"});
				dialog.setFilterExtensions (new String [] {"*.xml", "*.*"});
				dialog.setFileName ("ExportedTasks.xml");
				String name = dialog.open();
				try{
					File file = new File(name);
					if(!file.isDirectory()){
						getTaskService().saveToFile(file);
					}
				}catch(Exception e){
					e.printStackTrace();
					MessageBox md = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
					md.setText("Save File Error");
					md.setMessage("Error saving file:\n\n"
							+ e.getMessage());
					md.open();
				}

			}
		});
		//import toolbar button
		importItem = new ToolItem(toolBar, SWT.PUSH);
		importItem.setImage(ImageRepository.getImage("import"));
		importItem.setToolTipText("Import Tasks from File");
		importItem.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event arg0) {
				FileDialog dialog = new FileDialog (shell, SWT.OPEN);
				dialog.setText("Open Tasks File");
				dialog.setFilterNames (new String [] {"xml Files", "All Files (*.*)"});
				dialog.setFilterExtensions (new String [] {"*.xml", "*.*"});
				String name = dialog.open();
				try{
					File file = new File(name);
					if(!file.isDirectory()){
						getTaskService().loadFromFile(file);
					}
				}catch(Exception e){
					e.printStackTrace();
					MessageBox md = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
					md.setText("Open File Error");
					md.setMessage("Error Opening file:\n\n"
							+ e.getMessage());
					md.open();
				}
				clearMainTable();
			}
		});



		//--------main table-----\\
		mainTable = new Table(mainComp, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		mainTable.setLayoutData(gd);
		mainTable.setHeaderVisible(true);

		tasks = getTaskService().getTasks();

		//Virtual data listener for table
		mainTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event e) {
				TableItem item = (TableItem) e.item;
				int index = mainTable.indexOf(item);

				//Pull the task for the given index we need to draw
				Task task = tasks[index];

				//Name is easy, but Timing needs to be constructed

				/*				minute hour dom month dow user cmd

				minute	This controls what minute of the hour the command will run on,
					 and is between '0' and '59'
				hour	This controls what hour the command will run on, and is specified in
						 the 24 hour clock, values must be between 0 and 23 (0 is midnight)
				dom	This is the Day of Month, that you want the command run on, e.g. to
					 run a command on the 19th of each month, the dom would be 19.
				month	This is the month a specified command will run on, it may be specified
					 numerically (0-12), or as the name of the month (e.g. May)
				dow	This is the Day of Week that you want a command to be run on, it can
					 also be numeric (0-7) or as the name of the day (e.g. sun).
				 */

				String timing = task.getMinutes() + " " +
				task.getHours() + " " +
				task.getDaysOfMonth() + " " +
				task.getMonths() + " " +
				task.getDaysOfWeek();


				//Actions also needs to be built
				String actionStr = "";
				Action[] actions = task.getActions();
				for(int i = 0; i < actions.length; i++){
					actionStr += actions[i].getDetails();
					if(actions.length > 0 && i != actions.length - 1)
						actionStr += "; ";

				}


				//Set strings to the table
				item.setText(new String[] {task.getTaskName(), timing, actionStr});

				//set the task to the tableItem
				item.setData(task);

				item.setChecked(!task.isDisabled());

				//color if even # line
				if (index%2==0)
					item.setBackground(ColorUtilities.getBackgroundColor(display));

			}
		});

		mainTable.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				TableItem[] items = mainTable.getSelection();
				if(items.length == 0)
					setToolBarIcons(false,false);
				else if(items.length == 1){
					setToolBarIcons(true,true);
				}else if(items.length > 1){
					setToolBarIcons(false, true);
				}
				if (event.detail == SWT.CHECK) {
					TableItem item = (TableItem) event.item;
					Task task = (Task) item.getData();
					task.setDisabled(!item.getChecked());
				}
			}
		});

		mainTable.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if(e.button == 1) {
					if(mainTable.getItem(new Point(e.x,e.y))==null){
						mainTable.deselectAll();
						setToolBarIcons(false,false);
					}

				}
			}
		});


		mainTable.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event arg0) {
				TableItem[] items = mainTable.getSelection();
				if(items.length == 1){
					openInputEditorGUI(display,getTaskManagerGUI(),getTaskService(), (Task)items[0].getData());
					lockButtons(true);
				}
			}
		});

		final TableColumn taskName = new TableColumn(mainTable, SWT.LEFT);
		taskName.setText("Task Name");
		taskName.setWidth(200);


		final TableColumn taskTimeCode = new TableColumn(mainTable, SWT.LEFT);
		taskTimeCode.setText("Task Timing");
		taskTimeCode.setWidth(150);

		final TableColumn shortList = new TableColumn(mainTable, SWT.LEFT);
		shortList.setText("Short List of Actions");
		shortList.setWidth(300);


		//Sort Listener
		Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				// determine new sort column and direction
				TableColumn sortColumn = mainTable.getSortColumn();
				TableColumn currentColumn = (TableColumn) e.widget;
				int dir = mainTable.getSortDirection();
				if (sortColumn == currentColumn) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				} else {
					mainTable.setSortColumn(currentColumn);
					dir = SWT.UP;
				}
				// sort the data based on column and direction
				//final int index = currentColumn == taskName ? 0 : 1;
				final int direction = dir == SWT.UP ? 1 : -1;


				Arrays.sort(tasks, new Comparator<Task>() {
					/* (non-Javadoc)
					 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
					 */
					public int compare(Task o1, Task o2) {
						return direction*o1.getTaskName().compareTo(o2.getTaskName());
					}
				});

				// update data displayed in table
				mainTable.setSortDirection(dir);
				mainTable.clearAll();
			}
		};

		taskName.addListener(SWT.Selection, sortListener);

		mainMenu = new Menu(mainTable);
		mainTable.setMenu(mainMenu);

		mainTable.setSortColumn(taskName);
		mainTable.setSortDirection(SWT.UP);

		clearMainTable();

		setToolBarIcons(false,false);
	}


	//Public method to clear and redraw the table
	public void clearMainTable(){
		if(display == null || display.isDisposed()) return;
		display.asyncExec(new SWTSafeRunnable(){

			@Override
			public void runSafe() {
				if(getTaskService() == null) return;
				if(mainTable != null || !mainTable.isDisposed()){
					//Repoll the tasks
					tasks = getTaskService().getTasks();
					sortMainTaskArray(mainTable.getSortDirection() == SWT.UP ? 1 : -1);
					mainTable.clearAll();
					mainTable.setItemCount(getTaskService().getTasks().length);
				}
			}
		});
	}


	private void sortMainTaskArray(final int direction){
		Arrays.sort(tasks, new Comparator<Task>() {
			/* (non-Javadoc)
			 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
			 */
			public int compare(Task o1, Task o2) {
				return direction*o1.getTaskName().compareTo(o2.getTaskName());
			}
		});
	}


	/**
	 * Public method to turn on or off the toolbar icons based on table selection
	 * @param boolean beditItem
	 * @param boolean bremoveItem
	 */
	public void setToolBarIcons(final boolean beditItem, final boolean bremoveItem){
		if(display == null || display.isDisposed()) return;
		display.asyncExec(new SWTSafeRunnable(){

			@Override
			public void runSafe() {
				//editItem, removeItem
				if(editItem != null || !editItem.isDisposed())
					editItem.setEnabled(beditItem);
				if(removeItem != null || !removeItem.isDisposed())
					removeItem.setEnabled(bremoveItem);
			}
		});
	}

	//Pulls the taskService associated with the main GUI window
	protected TaskService getTaskService(){
		return taskService;
	}

	//Pulls the TaskManagerGUI to send on so that the table can be cleared later
	protected CronGUI getTaskManagerGUI(){
		return tmGUI;
	}

	protected void openInputEditorGUI(Display _display, CronGUI tmGUI, TaskService _taskService, Task _task){
		new CronInputGUI(_display,tmGUI,	_taskService, _task);
	}

	public void lockButtons(final boolean bLock){
		if(display == null || display.isDisposed()) return;
		display.asyncExec(new SWTSafeRunnable(){

			@Override
			public void runSafe() {
				//editItem, removeItem
				if(addItem != null || !addItem.isDisposed())
					addItem.setEnabled(!bLock);
				if(editItem != null || !editItem.isDisposed())
					editItem.setEnabled(!bLock);
				if(removeItem != null || !removeItem.isDisposed())
					removeItem.setEnabled(!bLock);
			}
		});
	}

}//EOF
