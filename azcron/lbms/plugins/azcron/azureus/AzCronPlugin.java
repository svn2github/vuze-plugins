package lbms.plugins.azcron.azureus;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import lbms.plugins.azcron.azureus.gui.AzCronGUI;
import lbms.plugins.azcron.azureus.service.AzTaskService;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.jdom.Element;

/**
 * @author Damokles
 * 
 */
public class AzCronPlugin implements Plugin {

	private static PluginInterface	pluginInterface;

	//new API startup code
	private UISWTInstance			swtInstance	= null;
	private AzTaskService			taskService;

	private static Logger			logger;
	private static LoggerChannel	logChannel;
	private File					taskFile;
	private AzCronGUI				gui;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize (PluginInterface pluginInterface)
			throws PluginException {
		AzCronPlugin.pluginInterface = pluginInterface;

		taskFile = new File(pluginInterface.getPluginDirectoryName(),
				"tasks.xml");

		UIManager ui_manager = pluginInterface.getUIManager();

		///////////

		logger = pluginInterface.getLogger();
		logChannel = logger.getTimeStampedChannel("TaskManager");

		final BasicPluginViewModel view_model = ui_manager
				.createBasicPluginViewModel("TaskManager Log");
		view_model.getActivity().setVisible(false);
		view_model.getProgress().setVisible(false);
		view_model.getStatus().setVisible(false);

		logChannel.addListener(new LoggerChannelListener() {
			public void messageLogged (int type, String content) {
				view_model.getLogArea().appendText(content + "\n");
			}

			public void messageLogged (String str, Throwable error) {
				if (str.length() > 0) {
					view_model.getLogArea().appendText(str + "\n");
				}

				StringWriter sw = new StringWriter();

				PrintWriter pw = new PrintWriter(sw);

				error.printStackTrace(pw);

				pw.flush();

				view_model.getLogArea().appendText(sw.toString() + "\n");
			}
		});

		///////////

		//BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugin.taskmanager");

		//////////

		taskService = new AzTaskService();

		ui_manager.addUIListener(new UIManagerListener() {
			public void UIAttached (UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					if (gui == null) {
						gui = new AzCronGUI(taskService);
					}
					swtInstance = (UISWTInstance) instance;
					swtInstance.addView(UISWTInstance.VIEW_MAIN,
							AzCronGUI.VIEWID, gui);
				}
			}

			public void UIDetached (UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					swtInstance = null;
				}
			}
		});

		try {
			if (taskFile.exists()) {
				taskService.loadFromFile(taskFile);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		taskService.start();

		pluginInterface.addListener(new PluginListener() {
			public void closedownComplete () {
			}

			public void closedownInitiated () {
				try {
					taskService.saveToFile(taskFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			public void initializationComplete () {
				taskService.start();
			}
		});
	}

	/**
	 * @return the pluginInterface
	 */
	public static PluginInterface getPluginInterface () {
		return pluginInterface;
	}

	/**
	 * @return the logChannel
	 */
	public static LoggerChannel getLogChannel () {
		return logChannel;
	}

	/**
	 * @return the logger
	 */
	public static Logger getLogger () {
		return logger;
	}

	public Element ipcGetTaskService () {
		return taskService.toElement();
	}

	public void ipcSetTaskService (Element e) {
		taskService.readFromElement(e);
		taskService.reschedule();
	}
}
