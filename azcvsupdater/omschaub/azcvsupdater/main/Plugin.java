package omschaub.azcvsupdater.main;


import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

public class Plugin implements org.gudy.azureus2.plugins.Plugin {
	
	  protected static String
	  normaliseJDK(
		String	jdk )
	  {
		  try{
			  String	str = "";

			  // take leading digit+. portion only

			  for (int i=0;i<jdk.length();i++){

				  char c = jdk.charAt( i );

				  if ( c == '.' || Character.isDigit( c )){

					  str += c;

				  }else{

					  break;
				  }
			  }

			  	// convert 5|6|... to 1.5|1.6 etc

			  if ( Integer.parseInt( "" + str.charAt(0)) > 1 ){
				  
				  str = "1." + str;
			  }
			  
			  return( str );
			  
		  }catch( Throwable e ){
			  
			  return( "" );
		  }
	  }

	public void initialize(final PluginInterface pluginInterface) {
		
		String java_version = normaliseJDK(System.getProperty("java.version"));
		if (java_version.startsWith("1.4")) {
			pluginInterface.getLogger().getChannel("azcvsupdater").logAlert(
					LoggerChannel.LT_ERROR, pluginInterface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
							"azcvsupdater.java14error"));
			return;
		}
		
		UIManager	ui_manager = pluginInterface.getUIManager();
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugin.azcvsupdater");
		
		config_model.addBooleanParameter2("AutoOpen","auto.open",true);

		if (!pluginInterface.getUtilities().isOSX()) {
		    config_model.addBooleanParameter2("TrayAlert","azcvsupdater.tray.alert",false);
		}
		
		config_model.addIntParameter2("WebUpdatePeriod", "web.update.period", 60, 15, 10080);
		
		pluginInterface.getUtilities().createDelayedTask(new Runnable() {
			public void run() {
		        pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
		        	public void UIDetached(UIInstance instance) {}
		            public void UIAttached(UIInstance instance) {
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