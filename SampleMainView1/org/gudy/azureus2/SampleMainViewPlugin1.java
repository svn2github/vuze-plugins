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
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

/**
 * Opens a view on the main azureus window on startup.
 * Adds a menu item to open the window.
 * 
 * @author TuxPaper
 * @created Oct 14, 2005
 *
 */
public class SampleMainViewPlugin1 implements UnloadablePlugin {
	private static final String VIEWID = "org.gudy.azureus2.SampleMainViewPlugin";

	private ViewListener viewListener = null;

	private UISWTInstance swtInstance = null;

	public void initialize(PluginInterface pluginInterface)
			throws PluginException {
		
		// Get notified when the UI is attached
		pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					swtInstance = ((UISWTInstance) instance);

					viewListener = new ViewListener();
					if (viewListener != null) {
						// Add it to the menu
						swtInstance.addView(UISWTInstance.VIEW_MAIN, VIEWID, viewListener);
						// Open it immediately
						swtInstance.openMainView(VIEWID, viewListener, null);
					}
				}
			}

			public void UIDetached(UIInstance instance) {
				if (instance instanceof UISWTInstance)
					instance = null;
			}
		});
	}

	public void unload() throws PluginException {
		if (swtInstance != null)
			swtInstance.removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
	}

	private class ViewListener implements UISWTViewEventListener {

		UISWTView view = null;

		Label funLabel;

		int iIndex = 0;

		public boolean eventOccurred(UISWTViewEvent event) {
			switch (event.getType()) {
				case UISWTViewEvent.TYPE_CREATE:
					System.out.println("TYPE_CREATE Called");
					/* We only want one view
					 * 
					 * If we wanted multiple views, we would need a class to handle
					 * one view.  Then, we could set up a Map, with the key
					 * being the UISWTView, and the value being a new instance of the
					 * class.  When the other types of events are called, we would
					 * lookup our class using getView(), and then pass the work to
					 * the our class.
					 */
					if (view != null)
						return false;
					view = event.getView();
					break;

				case UISWTViewEvent.TYPE_INITIALIZE:
					System.out.println("TYPE_INITIALIZE Called");
					initialize((Composite) event.getData());
					break;

				case UISWTViewEvent.TYPE_REFRESH:
					refresh();
					break;

				case UISWTViewEvent.TYPE_DESTROY:
					System.out.println("TYPE_DESTROY Called");
					view = null;
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

		private void initialize(Composite parent) {
			final Label lbl = new Label(parent, SWT.NONE);
			lbl.setText("Welcome to my View!");
			lbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			funLabel = new Label(parent, SWT.LEFT);
			funLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Button button = new Button(parent, SWT.PUSH);
			button.setText("Push me");
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					lbl.setText("My plugin ID is " + view.getViewID());
				}
			});

			Button buttonClose = new Button(parent, SWT.PUSH);
			buttonClose.setText("Close");
			buttonClose.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					view.closeView();
				}
			});
		}

		private void refresh() {
			funLabel.setText(String.valueOf(iIndex++));
		}
	}
}
