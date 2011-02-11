/*
 * Created on Feb 8, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities.commentmaker;


import omschaub.azcvsupdater.main.Tab1Utils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;

/**
 * @author omschaub
 */
public class CommentEditor {
    
    public static void open(final String textTemp, final String title, final PluginInterface pluginInterface){
    	UIInputReceiver input = pluginInterface.getUIManager().getInputReceiver();
    	input.setLocalisedTitle("Comment view for: " + title);
    	input.setPreenteredText(textTemp, false);
    	input.setMultiLine(true);
    	input.prompt();
    	if (input.hasSubmittedInput()) {
    		CommentMaker.commentWriter(title, input.getSubmittedInput());
    		Tab1Utils.loadDirectory(pluginInterface.getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
    	}
    }
}
