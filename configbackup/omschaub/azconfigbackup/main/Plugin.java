package omschaub.azconfigbackup.main;


import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

public class Plugin implements org.gudy.azureus2.plugins.Plugin {
	
	public static LoggerChannel channel;
	
	public void initialize(final PluginInterface pluginInterface) {
		
		UIManager	ui_manager = pluginInterface.getUIManager();
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugin.azconfigbackup");
		
		config_model.addBooleanParameter2("AutoOpen","auto.open",true);
		
		channel = pluginInterface.getLogger().getChannel("azconfigbackup");
		//pluginInterface.getUIManager().createBasicPluginViewModel("azconfigbackup").attachLoggerChannel(channel);

		pluginInterface.getUtilities().createDelayedTask(new Runnable() {
			public void run() {
		        pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
		        	public void UIDetached(UIInstance instance) {}
		            public void UIAttached(UIInstance instance) {
		            	try {UIAttached0(instance);}
		            	catch (Throwable t) {channel.log(t);}
		            }
		            private void UIAttached0(UIInstance instance) {
		              if (instance instanceof UISWTInstance) {
		            	UISWTInstance swtInstance = (UISWTInstance)instance;
		                UISWTViewEventListener myView = new View(pluginInterface);
		                swtInstance.addView(UISWTInstance.VIEW_MAIN, View.VIEWID, myView);
		                if(pluginInterface.getPluginconfig().getPluginBooleanParameter("AutoOpen",true)){
		                    swtInstance.openMainView(View.VIEWID, myView, null, false);
		                }
		              }
		            }
		          });
			}
		}).queue();
	}//End of Initialize
        
//EOF
}