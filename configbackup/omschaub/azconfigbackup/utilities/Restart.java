/*
 * Created on Feb 6, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.utilities;

import java.io.File;
import java.io.FileInputStream;


import omschaub.azconfigbackup.main.StatusBoxUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.update.UpdateInstaller;
import org.gudy.azureus2.plugins.update.UpdateManager;

/**
 * Restarting Mechanism
  */
public class Restart {
    
    
    /** updatePlugin is the main restart function.  Handled by stacks 'From' and 'To' and restart boolean
     * 
     */
    public static void updateRestart(final PluginInterface pluginInterface, final StackX complete_file_from, final StackX complete_file_to, final boolean restart)
    {
			Thread restart_thread = new Thread() 
			{
				public void run() 
				{
				    
	        
	        try
	        {
	            UpdateManager um = pluginInterface.getUpdateManager();
	            UpdateInstaller updateInstaller = um.createInstaller();    
	            int i = 0;
	            while (!complete_file_from.isEmpty()){
	                
	                File from = new File (complete_file_from.pop());
	                if(!from.isFile())
	                {
	                    StatusBoxUtils.mainStatusAdd(" Could not update because " + from.getName() +  " is not a real file",2);
	                }
	                //System.out.println("From: " + from.getPath() + " To: " + complete_file_to.peek());
	                //System.out.println("addResource: azconfigbackup_" + i);
	                updateInstaller.addResource("azconfigbackup_" + i , new FileInputStream (from));
	                if(!complete_file_to.isEmpty())
	                {
	                    updateInstaller.addMoveAction("azconfigbackup_" + i , complete_file_to.pop());
	                }

                    i++;
	            }
	            //System.out.println("AZConfigBackup:  Sleeping for 5 seconds to ensure all has completed");
                Thread.sleep(5000);
                um.applyUpdates(restart);
	                    
	        
	        }
	        catch (Exception e)
	        {
	            StatusBoxUtils.mainStatusAdd(" Major Error Inserting/Restarting please report to omschaub@users.sourceforge.net",2);
	            System.out.println(e);
	        }
	        
	    }
		    
	};
	restart_thread.setDaemon(true);
    restart_thread.start();

}
    
    
    /** updateNORestart is an insert but not restart function.  Handled by stacks 'From' and 'To' and restart boolean
     * 
     */
    public static void updateNORestart(final PluginInterface pluginInterface, final StackX complete_file_from, final StackX complete_file_to)
    {
			Thread insert_thread = new Thread() 
			{
				public void run() 
				{
				    
	        
	        try
	        {
	            UpdateManager um = pluginInterface.getUpdateManager();
	            UpdateInstaller updateInstaller = um.createInstaller();    
	            int i = 0;
	            while (!complete_file_from.isEmpty()){
	                
                    
	                File from = new File (complete_file_from.pop());
	                if(!from.isFile())
	                {
	                    StatusBoxUtils.mainStatusAdd(" Could not update because " + from.getName() +  " is not a real file",2);
	                }
	                String file_to = complete_file_to.peek();
	               // System.out.println("From: " + from.getPath() + " To: " + file_to);
	                
	                updateInstaller.addResource("azconfigbackup_" + i , new FileInputStream (from));
	                if(!complete_file_to.isEmpty())
	                {
	                    updateInstaller.addMoveAction("azconfigbackup_" + i , complete_file_to.pop());
                        
	                }
	                StatusBoxUtils.mainStatusAdd(" Successfully added " + from.getName() + " for insertion as " + file_to + " on next restart ",1);
	      /*          if(from.getName().startsWith("azconfigbackup")){
                        StatusBoxUtils.mainStatusAdd(" Successfully deleted " + from.getName());
                        from.delete();
                    }*/
                    i++;
                    
	            }
	            
                
                
                
	            //um.applyUpdates(restart);
	                    
	        
	        }
	        catch (Exception e)
	        {
	            StatusBoxUtils.mainStatusAdd(" Major Error Inserting/Restarting please report to omschaub@users.sourceforge.net",2);
	            System.out.println(e);
	        }
	        
	    }
		    
	};
	insert_thread.setDaemon(true);
    insert_thread.start();

}

    //EOF
}