/**
 * 
 */
package com.aelitis.azureus.plugins.azjython;

import com.aelitis.azureus.plugins.azjython.interactive.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * @author Allan Crooks
 *
 */
public class JythonSWTUIManager implements UIManagerListener {
	
	public UISWTInstance ui_instance = null;
	private InterpreterFactory factory = null;
	private JythonPluginCore core;
	private boolean init_ok;
	
	public JythonSWTUIManager(JythonPluginCore core, boolean init_ok) {
		this.core = core; 
		this.init_ok = init_ok;
		core.plugin_interface.getUIManager().addUIListener(this);
		if (init_ok && core.getFlag("mark_as_selected")) {this.addSelectedMenuItem();}
		
		// Not strictly a "SWT UI" thing, but it's related...
		core.plugin_interface.getUIManager().getMenuManager().addMenuItem(
				MenuManager.MENU_MENUBAR, "azjython.menu.execute_script").addListener(new RunScriptPopup());
	}
	
	public void addSelectedMenuItem() {
		TableManager tm = core.plugin_interface.getUIManager().getTableManager();
	    TableContextMenuItem main = tm.addContextMenuItem((String)null, "azjython.menu.main");
	    main.setStyle(TableContextMenuItem.STYLE_MENU);
	    TableContextMenuItem mark_as_selected = tm.addContextMenuItem(main, "azjython.menu.mark_as_selected");
    	mark_as_selected.addMultiListener(new MenuItemListener() {
    		public void selected(MenuItem item, Object o) {
    			Object[] source_array = (Object[])o;
    			Object[] target_array = new Object[source_array.length];
    			for (int i=0; i<source_array.length; i++) {
    				target_array[i] = ((TableRow)source_array[i]).getDataSource();
    			}
    			core.ui_namespace.setSelectedItems(target_array);
    			
    			/**
    			 * After doing this - if the user has never used this function before,
    			 * we will let them know what has just happened.
    			 * 
    			 * (Note: informUser doesn't use logAlertRepeatable, so duplicates will
    			 * get filtered out. Maybe we should not use this flag...)
    			 */
    			if (!core.hasFlag("has_done_mark_as_selected_before")) {
    				core.informUser("ui.mark_as_selected_help");
    				core.setFlag("has_done_mark_as_selected_before", true);
    			}
    		}
    	});
    }

	public void populateConsoleVariables(JythonCoreConsole con) {
		if (this.ui_instance != null) {con.setVariable("swt_ui", this.ui_instance);}
	}
	
	public void UIAttached(UIInstance ui_instance) {
		if (!(ui_instance instanceof UISWTInstance)) {return;}
		this.ui_instance = (UISWTInstance)ui_instance;
		
		if (!this.init_ok) {return;}
		this.factory = new InterpreterFactory(core, this.ui_instance);
		
		String new_console = core.locale_utils.getLocalisedMessageText("azjython.interpreter.menu.new");
		this.ui_instance.addView(UISWTInstance.VIEW_MAIN, new_console, this.factory);
		if (core.getFlag("auto_open_console")) {
			this.ui_instance.openView(UISWTInstance.VIEW_MAIN, new_console, null, false);
		}
		
		// Now we initialise any output script windows if we need to.
		String script_name = core.getScriptValue("startup_script.azureus");
		if (script_name != null && core.startup_console == null) {
			core.runStartupScript(script_name, false);
		}

		// We will run the console at this point - either immediately (if we have no window)
		// or indirectly with event listeners.
		if (core.startup_console != null) {
			boolean create_window = core.getFlag("startup_script.azureus.output_in_window");
			if (!create_window) {
				// We'll have to attach the swt_ui variable manually then.
				populateConsoleVariables(core.startup_console);
				core.startup_console.startConsoleRunning("");
			}
			else {
				createOutputWindow(core.startup_console, false);
			}
		}
		
	}
	
	public boolean createOutputWindow(final JythonCoreConsole console, final boolean focus_on_window) {
		if (this.ui_instance == null) {return false;}
		UISWTViewEventListener event_listener = new UISWTViewEventListener() {
			public boolean eventOccurred(UISWTViewEvent event) {
				if (event.getType() != UISWTViewEvent.TYPE_INITIALIZE) {return true;}
				Composite c = (Composite)event.getData();
				event.getView().setTitle(console.getName());
				JythonOutputWindow window = new JythonOutputWindow(console, JythonSWTUIManager.this.ui_instance, c);
				window.startConsoleRunning();
				return true;
			}
		};
		this.ui_instance.openMainView(UISWTInstance.VIEW_MAIN, event_listener, null, focus_on_window);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIManagerListener#UIDetached(org.gudy.azureus2.plugins.ui.UIInstance)
	 */
	public void UIDetached(UIInstance instance) {
		// TODO Auto-generated method stub

	}
	
	public class RunScriptPopup implements MenuItemListener {
		public void selected(MenuItem item, Object o) {
			if (ui_instance == null) {return;}
	        FileDialog dialog = new FileDialog(ui_instance.getDisplay().getActiveShell(), SWT.APPLICATION_MODAL);
	        dialog.setFilterExtensions(new String[] {"*.py"});
	        dialog.setFilterNames(new String[] {core.locale_utils.getLocalisedMessageText("azjython.popup.execute_script.filter_name")});        
	        
	        String path = dialog.open();
	        if (path == null) {return;}
	        core.plugin.ipcRunScriptFromFileSafe(path, true);
		}
	}

}
