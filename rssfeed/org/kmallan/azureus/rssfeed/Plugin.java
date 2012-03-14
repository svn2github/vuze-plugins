/*
 * RSSFeed - Azureus2 Plugin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package org.kmallan.azureus.rssfeed;

import java.util.Date;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;

public class Plugin implements org.gudy.azureus2.plugins.Plugin {

  public static String PLUGIN_VERSION = "x.x.x";
  public static final int MIN_REFRESH = 300;
  public static final boolean DEBUG = false;

  private static PluginInterface pluginInterface;
  private final String VIEWID = "RSSFeed Scanner";
  private ViewListener viewListener = null;
  private UISWTInstance swtInstance = null;
  private static Config rssfeedConfig;
  private static View view = null;

  public void initialize(PluginInterface pluginIf) {
    pluginInterface = pluginIf;
    rssfeedConfig = new Config();
    
			pluginInterface.addConfigUIParameters(getParameters(), "RSSFeed.Config");
		PLUGIN_VERSION = pluginInterface.getPluginVersion();

		pluginInterface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						view = new View(pluginInterface, rssfeedConfig);
						
						swtInstance = ((UISWTInstance) instance);

						viewListener = new ViewListener();
						if (viewListener != null) {
							// Add it to the menu
							swtInstance.addView(UISWTInstance.VIEW_MAIN, VIEWID, viewListener);
							// Open it immediately
							if( pluginInterface.getPluginconfig().getPluginBooleanParameter("AutoLoad", false) )
								swtInstance.openMainView(VIEWID, viewListener, null);
						}
						
					}
				}
				
				public void
				UIDetached(
					UIInstance		instance )
				{
					if (instance instanceof UISWTInstance)
						instance = null;
				}
			});
	
  }
  
	private class ViewListener implements UISWTViewEventListener {

		boolean bInitialized = false;

		public boolean eventOccurred(UISWTViewEvent event) {
			switch (event.getType()) {
				case UISWTViewEvent.TYPE_CREATE:
					/* We only want one view
					 * 
					 * If we wanted multiple views, we would need a class to handle
					 * one view.  Then, we could set up a Map, with the key
					 * being the UISWTView, and the value being a new instance of the
					 * class.  When the other types of events are called, we would
					 * lookup our class using getView(), and then pass the work to
					 * the our class.
					 */
					if (bInitialized)
						return false;
					break;

				case UISWTViewEvent.TYPE_INITIALIZE:
					if (bInitialized)
						return false;

					view.initialize((Composite) event.getData());
					bInitialized = true;
					break;

				case UISWTViewEvent.TYPE_REFRESH:
					//view.refresh();
					break;

				case UISWTViewEvent.TYPE_DESTROY:
					view.delete();
					bInitialized = false;
					break;

				case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
					break;

				case UISWTViewEvent.TYPE_FOCUSGAINED:
					break;

				case UISWTViewEvent.TYPE_FOCUSLOST:
					break;

				case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
					break;
			}
			return true;
		}
	}

  private Parameter[] getParameters() {
    PluginConfigUIFactory configUIFactory = pluginInterface.getPluginConfigUIFactory();
    Parameter[] para = new Parameter[6];
    para[0] = configUIFactory.createBooleanParameter("Enabled", "RSSFeed.Config.Enable", true);
    para[1] = configUIFactory.createIntParameter("Delay", "RSSFeed.Config.Delay", 900);
    para[2] = configUIFactory.createBooleanParameter("AutoLoad", "RSSFeed.Config.AutoLoad", false);
    para[3] = configUIFactory.createBooleanParameter("AutoStartManual", "RSSFeed.Config.AutoStartManual", true);
    para[4] = configUIFactory.createIntParameter("KeepOld", "RSSFeed.Config.KeepOld", 2);
    para[5] = configUIFactory.createIntParameter("KeepMax", "RSSFeed.Config.KeepMax", 1000);
    ((EnablerParameter) para[0]).addEnabledOnSelection(para[1]);

    return para;
  }

  public static String getPluginDirectoryName() {
    return pluginInterface.getPluginDirectoryName();
  }

  public static int getIntParameter(String name) {
    return pluginInterface.getPluginconfig().getPluginIntParameter(name);
  }

  public static boolean getBooleanParameter(String name) {
    return pluginInterface.getPluginconfig().getPluginBooleanParameter(name);
  }

  public static void setParameter(String name, int value) {
    pluginInterface.getPluginconfig().setPluginParameter(name, value);
  }

  public static PluginInterface
  getPluginInterface()
  {
	  return( pluginInterface );
  }
  
  public static void updateView(final ListBean listBean) {
    if (view != null && view.isOpen() && view.display != null
				&& !view.display.isDisposed()) {
			view.display.asyncExec(new Runnable() {
				public void run() {
					if (view.listTable == null || view.listTable.isDisposed())
						return;
					ListTreeItem listItem = view.treeViewManager.getItem(listBean);
					listItem.update();
				}
			});
    }
  }

  public static void debugOut(String s) {
    if (DEBUG) {
			System.out.println(new Date() + "] RSSFeed: " + s + " ["
					+ Thread.currentThread().getName() + "]");
    }
  }

  public static void launchUrl(final String url) {
    new Thread("URLLauncherThread") {
      public void run() {
        Program.launch(url);
      }
    }.start();
  }
}