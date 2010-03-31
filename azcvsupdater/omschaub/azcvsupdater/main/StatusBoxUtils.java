/*
 * Created on Feb 6, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TableItem;





/**
 * Utilities for the statusBox (mainStatus)
 */

public class StatusBoxUtils {

    /**
     * Saves the log txt file give the array info and the file
     * @param String[] info
     * @param File output_file
     * @param int append (0 for append, 1 for overwrite, 2 for newfile)
     */
    static void save_log(final String[] info, final File output_file, final int append)
    {
        try 
        {
            
            BufferedWriter bfw;
            if(append > 0){
                bfw = new BufferedWriter(new FileWriter(output_file, false));
            }else{
                bfw = new BufferedWriter(new FileWriter(output_file, true));
            }
                
            //write the status box info
            for(int i = 0; i < info.length; i++)
            {
                bfw.write(info[i]);
                bfw.newLine();
                
            }
            bfw.flush();
            bfw.close();
            
            if(append == 0){
                mainStatusAdd( " Log file at " + output_file.getAbsolutePath() + " successfully appended",0);
            }else if(append == 1){
                mainStatusAdd( " Log file at " + output_file.getAbsolutePath() + " successfully overwritten",0);    
            }else{
                mainStatusAdd( " Log file at " + output_file.getAbsolutePath() + " successfully created",0);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            MessageBox messageBox = new MessageBox(StatusBox.status_group.getShell(), SWT.ICON_ERROR | SWT.OK);
            messageBox.setText("Error writing to file");
            messageBox.setMessage("Your computer is reporting that the selected file cannot be written to, please retry this operation and select a different file");
            messageBox.open();
        }
    }
    
    /**
     * Main class to add a message to the status box
     * @param string
     * @param alert - 0 for normal, 1 for bold, 2 for red (alert)
     */
   
    public static void mainStatusAdd(final String string,final int alert){
        View.asyncExec(new Runnable ()
                {
    				public void run () 
    				{					
    				    try{
                            if(StatusBox.mainStatus == null && StatusBox.mainStatus.isDisposed()){
                                return;
                            }
                                String dateCurrentTime = View.formatDate(View.getPluginInterface().getUtilities().getCurrentSystemTime());
                                if(StatusBox.mainStatus == null && StatusBox.mainStatus.isDisposed())
                                    return;     
                                TableItem item = new TableItem(StatusBox.mainStatus,SWT.NULL,0);
                                
                                
                                if(alert == 1){
                                    item.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                                    
                                }else if(alert == 2){
                                    item.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
                                }
                                
                                item.setText("[" + dateCurrentTime + "] " + string);
                                    StatusBox.mainStatus.setSelection(0);
                                    StatusBox.mainStatus.deselect(0);
                        }catch (Exception e){
                            
                        }
                        
    				    
    				    
    				}
                });
    }
    
}
