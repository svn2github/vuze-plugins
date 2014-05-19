package omschaub.firefrog.main;


import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;




public class Plugin implements org.gudy.azureus2.plugins.Plugin {

    PluginInterface pluginInterface;
    private static PluginInterface pi;
    private static Display display;
    
    private static String VIEW_ID = "Firefrog";
	
	
	public void initialize(final PluginInterface pluginInterface) {
		this.pluginInterface = pluginInterface;

        pi=pluginInterface;
        
        UIManager   ui_manager = pluginInterface.getUIManager();
        BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugin.firefrog");
        
        //settings on main options panel
        config_model.addBooleanParameter2("firefrog_military_time","firefrog.military.time",false);
        config_model.addBooleanParameter2("firefrog_auto_open","firefrog.auto.open",true);
        
        pluginInterface.getUIManager().addUIListener(
                   new UIManagerListener()
                   {
                     public void
                     UIAttached(
                             UIInstance instance )
                     {
                         if ( instance instanceof UISWTInstance ){
                             UISWTInstance swt = (UISWTInstance)instance;
                             display = swt.getDisplay();
                             View view = new View(pluginInterface);
                             swt.addView(UISWTInstance.VIEW_MAIN, VIEW_ID, view);
                             if (isPluginAutoOpen()) {
                            	 swt.openView(UISWTInstance.VIEW_MAIN, VIEW_ID, null);
                             }
                         }
                     }

                    public void UIDetached(UIInstance arg0) {
                        // TODO Auto-generated method stub
                        
                    }
                   });
        
         
		
		
	}
    
    
    
    /**
     * Gets the pluginInterface from  Plugin.java
     * @return pluginInterface
     */
    public static PluginInterface getPluginInterface(){
        return pi;
    }
    
    /**
     * Gets the Display from  Plugin.java from the UISWTInstance
     * @return display
     */
    public static Display getDisplay(){
        return display;
    }
    
    /**
     * Returns the user set status of whether or not the plugin should autoOpen
     * @return boolean autoOpen
     */
    public static boolean isPluginAutoOpen(){
        PluginConfig config_getter = getPluginInterface().getPluginconfig();
        return config_getter.getPluginBooleanParameter("firefrog_auto_open",true);
    }
    
    
	
//EOF
}
	
	
	
		

	
	









