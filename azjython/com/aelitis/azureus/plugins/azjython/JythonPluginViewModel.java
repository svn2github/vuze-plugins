/**
 * 
 */
package com.aelitis.azureus.plugins.azjython;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.components.UITextArea;

/**
 * @author Allan Crooks
 *
 */
public class JythonPluginViewModel {
	
	public static BasicPluginViewModel createLogViewModel(final JythonPluginCore core) {
		String log_view_name = core.locale_utils.getLocalisedMessageText("ConfigView.section.azjython");
		BasicPluginViewModel model = core.plugin_interface.getUIManager().createBasicPluginViewModel(log_view_name);
		final UITextArea logarea = model.getLogArea();
		model.getProgress().setVisible(false);
		model.getStatus().setVisible(false);
		model.getActivity().setVisible(false);
		core.logger.addListener(new LoggerChannelListener() {
			public void messageLogged(String message, Throwable t) {
				messageLogged(LoggerChannel.LT_ERROR, message, t);
			}
			public void messageLogged(int logtype, String message) {
				messageLogged(logtype, message, null);
			}
			public void messageLogged(int logtype, String message, Throwable t) {
				String log_type_s = null;
				switch(logtype) {
					case LoggerChannel.LT_WARNING:
						log_type_s = "warning";
						break;
					case LoggerChannel.LT_ERROR:
						log_type_s = "error";
						break;
				}
				if (log_type_s != null) {
					String prefix = core.locale_utils.getLocalisedMessageText("AlertMessageBox." + log_type_s);
					logarea.appendText("[" + prefix.toUpperCase() + "] ");
				}
				logarea.appendText(message + "\n");
				if (t != null) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					t.printStackTrace(pw);
					logarea.appendText(sw.toString() + "\n");
				}
			}
		});
		return model;
	}
	
	public static void prepareModel(BasicPluginViewModel model, boolean installed) {
		model.getActivity().setVisible(!installed);
		model.getProgress().setVisible(!installed);
		model.getStatus().setVisible(!installed);
	}

}
