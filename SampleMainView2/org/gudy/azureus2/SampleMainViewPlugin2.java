/**
 * Free
 */
package org.gudy.azureus2;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

/**
 * Sample Azureus Plugin demonstrating adding a menu item to the "My Torrents"
 * view, which when clicked, opens a new window displaying the torrents name,
 * and number of bytes uploaded.  The number of bytes uploaded is continuously
 * refreshed via the TYPE_REFRESH trigger.
 * 
 * @author TuxPaper
 * @created Oct 15, 2005
 *
 */
public class SampleMainViewPlugin2 implements UnloadablePlugin {
	private static final String VIEWID = "SampleMainViewPlugin2";

	private static final String MENU_RESOURCEID = "Views.plugins.SampleMainViewPlugin2.tableMenu.title";

	private UISWTInstance swtInstance = null;

	public void initialize(PluginInterface pluginInterface)
			throws PluginException {
		TableContextMenuItem menuItem;

		// When the menu item is triggered, open the view, passing the Download
		// object to the view
		MenuItemListener menuItemListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (swtInstance == null || !(target instanceof TableRow))
					return;

				Download dl = (Download) ((TableRow) target).getDataSource();

				if (dl == null)
					return;

				ViewListener viewListener = new ViewListener();
				swtInstance.openMainView(VIEWID, viewListener, dl);
			}
		};

		// Create two menu items, one for completed and one for incomplete torrents
		TableManager tableManger = pluginInterface.getUIManager().getTableManager();
		menuItem = tableManger.addContextMenuItem(
				TableManager.TABLE_MYTORRENTS_COMPLETE, MENU_RESOURCEID);
		menuItem.addListener(menuItemListener);
		menuItem = tableManger.addContextMenuItem(
				TableManager.TABLE_MYTORRENTS_INCOMPLETE, MENU_RESOURCEID);
		menuItem.addListener(menuItemListener);

		// Get notified when the UI is attached
		pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					swtInstance = ((UISWTInstance) instance);
				}
			}

			public void UIDetached(UIInstance instance) {
				swtInstance = null;
			}
		});
	}

	public void unload() throws PluginException {
		if (swtInstance != null)
			swtInstance.removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
	}

	private class ViewListener implements UISWTViewEventListener {
		private Label funLabel;

		public boolean eventOccurred(UISWTViewEvent event) {
			switch (event.getType()) {
				case UISWTViewEvent.TYPE_CREATE:
					System.out.println("TYPE_CREATE Called");
					break;

				case UISWTViewEvent.TYPE_INITIALIZE:
					System.out.println("TYPE_INITIALIZE Called");
					initialize((Composite) event.getData(), event.getView());
					break;

				case UISWTViewEvent.TYPE_REFRESH:
					refresh(event.getView());
					break;

				case UISWTViewEvent.TYPE_DESTROY:
					System.out.println("TYPE_DESTROY Called");
					break;

				case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
					System.out.println("TYPE_DATASOURCE_CHANGED Called");
					break;

				case UISWTViewEvent.TYPE_FOCUSGAINED:
					System.out.println("TYPE_FOCUSGAINED Called");
					break;

				case UISWTViewEvent.TYPE_FOCUSLOST:
					System.out.println("TYPE_FOCUSLOST Called");
					break;

				case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
					System.out.println("TYPE_LANGUAGEUPDATE Called "
							+ Locale.getDefault().toString());
					break;
			}
			return true;
		}

		private void initialize(Composite parent, final UISWTView view) {
			if (view.getDataSource() == null)
				return;

			Download dl = (Download) view.getDataSource();

			final Label lbl = new Label(parent, SWT.NONE);
			lbl.setText(dl.getName());
			lbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			funLabel = new Label(parent, SWT.LEFT);
			funLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Button buttonClose = new Button(parent, SWT.PUSH);
			buttonClose.setText("Close");
			buttonClose.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					view.closeView();
				}
			});
		}

		private void refresh(final UISWTView view) {
			if (view.getDataSource() == null)
				return;

			Download dl = (Download) view.getDataSource();

			funLabel.setText("Bytes Uploaded: " + dl.getStats().getUploaded());
		}
	}
}
