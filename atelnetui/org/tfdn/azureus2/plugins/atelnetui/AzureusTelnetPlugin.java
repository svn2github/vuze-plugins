package org.tfdn.azureus2.plugins.atelnetui;

import java.io.IOException;

import org.gudy.azureus2.plugins.PluginConfigListener;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.PasswordParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Utilities;

public class AzureusTelnetPlugin implements UnloadablePlugin {

	private TelnetListener _telnet;
	private LoggerChannel _log;
	private PluginInterface _iface;

	public void unload() throws PluginException {
		_telnet.quit();
	}

	public void initialize(PluginInterface pluginInterface)
			throws PluginException {
		// Initializing localization
		_iface = pluginInterface;
		Utilities util = pluginInterface.getUtilities();
		LocaleUtilities local = util.getLocaleUtilities();
		local
				.integrateLocalisedMessageBundle("org.tfdn.azureus2.plugins.atelnetui.internat.Messages");

		// Initializing log
		_log = pluginInterface.getLogger().getChannel("atelnetui");

		// Creating config
		UIManager ui = pluginInterface.getUIManager();
		BasicPluginConfigModel model = ui.createBasicPluginConfigModel(
				"plugins", "atelnetui.name");
		model.addStringParameter2("welcome_message",
				"atelnetui.welcome_message",
				local.getLocalisedMessageText("atelnetui.welcome_messagetxt"));
		model.addStringParameter2("wrong_password", "atelnetui.wrong_password", local.getLocalisedMessageText("atelnetui.wrong_passwordtxt"));
		model.addStringParameter2("goodbye", "atelnetui.goodbye", local.getLocalisedMessageText("atelnetui.goodbyetxt"));
		model.addStringParameter2("prompt", "atelnetui.prompt", local.getLocalisedMessageText("atelnetui.prompttxt"));
		model.addIntParameter2("port", "atelnetui.port", 6870);
		model.addStringParameter2("username", "atelnetui.username", "admin");
		model.addPasswordParameter2("password", "atelnetui.password",
				PasswordParameter.ET_PLAIN, (new String("admin")).getBytes());

		// Launching server
		start();
		
		pluginInterface.getPluginconfig().addListener(new PluginConfigListener() {

			public void configSaved() {
				try {
					unload();
				} catch (PluginException e) {
				}
				start();
			}
			
		});
	}
	
	private void start() {
		try {
			_telnet = new TelnetListener(_iface);
			_telnet.start();
			_log.log(LoggerChannel.LT_INFORMATION, "atelnetui started");
		} catch (IOException e) {
			_log.log(LoggerChannel.LT_ERROR,
					"Unable to launch server. Stack trace is : "
							+ e.getMessage());
		}
	}

}
