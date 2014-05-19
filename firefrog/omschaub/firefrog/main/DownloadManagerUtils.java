/*
 * Created on Apr 30, 2005
 * Created by omschaub
 * 
 */
package omschaub.firefrog.main;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;

public class DownloadManagerUtils {
       
       private static CustomProgressBar customPB;
    
       public static Label connected, pause;
    
       public static void addDownload(final Download download, final boolean last, final boolean repaint){
           //System.out.println("Total: " + Timer.DOWNLOAD_COUNT + " : " + download.getName() + " : " + download.getIndex());
           
           if(Utilities.getDisplay() == null && Utilities.getDisplay().isDisposed())
               return;
           Utilities.getDisplay().asyncExec(new Runnable ()
                   {
                       public void run () 
                       {
                           boolean test = false;
                           if(DownloadManagerShell.DOWNLOADS != null && !DownloadManagerShell.DOWNLOADS.isDisposed()){
                               Control[] objects = DownloadManagerShell.DOWNLOADS.getChildren();
                               if(objects != null){
                                   for(int i = 0 ; i < objects.length ; i++){
                                       if(objects[i] != null){
                                           try{
                                               
                                                   if(objects[i].toString().startsWith("Progress")){
                                                       
                                                   }else{
                                                       Label temp = (Label)objects[i];
                                                       
                                                       if(temp.getText().equalsIgnoreCase(Utilities.verifyName(download.getName()))){
                                                           
                                                           test = true;                                                           
                                                                                                                     
                                                           //Label for Pause / Resume
                                                           if(objects[i+1] != null){
                                                               Label temp_pause = (Label)objects[i+1];
                                                               if(download.getState() == Download.ST_STOPPED){
                                                                   if(!temp_pause.getText().equalsIgnoreCase("Resume")){
                                                                       temp_pause.setText("Resume");
                                                                       temp_pause.getParent().layout(); 
                                                                       temp_pause.setToolTipText("Resume download of " + Utilities.verifyName(download.getName()));
                                                                   }                                                                   
                                                               }else{
                                                                   if(!temp_pause.getText().equalsIgnoreCase("Pause")){
                                                                       temp_pause.setText("Pause");
                                                                       temp_pause.getParent().layout();
                                                                       temp_pause.setToolTipText("Pause download of " + Utilities.verifyName(download.getName()));    
                                                                   }                                                                   
                                                               }
                                                               
                                                           }
                                                           if(repaint){
                                                               customPB = new CustomProgressBar();
                                                               Label customPB_label = (Label)objects[i+2];
                                                               customPB_label.setSize(customPB_label.getParent().getSize().x-70,20);
                                                               customPB_label.setImage(customPB.paintProgressBar(
                                                                       customPB_label,
                                                                       customPB_label.getSize().x,
                                                                       20,
                                                                       Integer.valueOf(String.valueOf(download.getStats().getCompleted()),10),
                                                                       Utilities.getDisplay(),
                                                                       true)); 
                                                           }
                                                           
                                                           //System.out.println("running Size: " + customPB_label.getSize().x);
                                                                                                                     
                                                           Label temp_stats = (Label)objects[i+4];
                                                           
                                                           int state = download.getState();
                                                           if(state == Download.ST_QUEUED){
                                                               temp_stats.setText("Queued");
                                                           }else if(state == Download.ST_STOPPED){
                                                               temp_stats.setText("Stopped");
                                                           }else if(state == Download.ST_PREPARING){
                                                               temp_stats.setText("Getting Files Ready (Allocating/Checking)");
                                                           }else if(state == Download.ST_WAITING){
                                                               temp_stats.setText("Waiting to Start");
                                                           }else if(state == Download.ST_READY){
                                                               temp_stats.setText("Ready To Be Started, Please Wait");
                                                           }else if(state == Download.ST_STOPPING){
                                                               temp_stats.setText("Stopping the Download, Please Wait");
                                                           }else{
                                                               //compile stats
                                                               long stats_downloaded = (download.getStats().getDownloaded() - download.getStats().getHashFails())/1024/1024;
                                                               String stats_text = stats_downloaded + "MB of " + 
                                                                   (download.getTorrent().getSize()/1024/1024) + "MB at " + (download.getStats().getDownloadAverage()/1024) +
                                                                   " KB/sec;  " + download.getStats().getETA();
                                                               temp_stats.setText(stats_text);    
                                                           }
                                                           
                                                           if(objects[i+5] != null){
                                                               Label temp_separator = (Label)objects[i+5];
                                                               try{
                                                                   if(objects[i+6] != null){
                                                                       //System.out.println("Not Last: " + download.getName());
                                                                       temp_separator.setVisible(true);
                                                                   }
                                                               }catch (Exception e){
                                                                   //System.out.println("Last: " + download.getName());
                                                                   temp_separator.setVisible(false);
                                                               }
                        
                                                           }
                                                       }    
                                                   }
                                              
                                               
                                               
                                               
                                                       
                                           }catch(Exception e){
                                               //e.printStackTrace();
                                           }
                                             
                                             
                                             
                                       }
                                       
                                   }    
                               }
                               if(!test){

                                                                      
                                   //name for download
                                   Label name = new Label(DownloadManagerShell.DOWNLOADS, SWT.NULL);
                                   name.setText(Utilities.verifyName(download.getName()));
                                   name.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                   GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                                   gridData.horizontalSpan = 2;
                                   name.setLayoutData(gridData);
                                   name.pack();
                                   
                                   /*Label percent = new Label(DownloadManagerShell.DOWNLOADS, SWT.NULL);
                                   percent.setText(download.getStats().getCompleted() / 10 + "%");
                                   percent.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                   gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                   gridData.grabExcessHorizontalSpace = true;
                                   gridData.horizontalSpan = 1;
                                   percent.setLayoutData(gridData);
                                   */
                                   
                                   final Label pause = new Label(DownloadManagerShell.DOWNLOADS,SWT.NULL);
                                   pause.setText("Pause");
                                   pause.setToolTipText("Pause " + Utilities.verifyName(download.getName()));
                                   gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                   if(download.getState() == Download.ST_STOPPED){
                                       pause.setText("Resume");
                                       pause.setToolTipText("Resume download of " + Utilities.verifyName(download.getName()));
                                   }else{
                                       pause.setText("Pause");
                                       pause.setToolTipText("Pause download of " + Utilities.verifyName(download.getName()));
                                   }
                                       
                                   
                                   pause.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                   pause.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                                   pause.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                                   pause.setLayoutData(gridData);
                                   pause.addListener(SWT.MouseDown, new Listener(){
                                       public void handleEvent(Event e) {
                                           
                                           try {
                                               if(download.getState() != Download.ST_STOPPED){
                                                   download.stop();
                                               }else{
                                                   download.restart();
                                               }
                                                                                                        
                                            } catch (DownloadException e1) {                                                   
                                                e1.printStackTrace();
                                            }
                                               
                                           }
                                           
                                         
                                     });
                                   
                                   pause.pack();
                                   
                                   
                                 /*  
                                   int size = (DownloadManagerShell.DOWNLOADS.getSize().x - 
                                           (DownloadManagerShell.DOWNLOADS.getBorderWidth() 
                                                   + DownloadManagerShell.DOWNLOADS.getClientArea().x  
                                                   + DownloadManagerShell.DOWNLOADS.getBounds().x 
                                                   + pause.getSize().x
                                                   + 5));
                                   */
                                   //System.out.println("Initial Size: " + size);
                                   
                                   customPB = new CustomProgressBar();
                                   Label customPB_label = new Label(DownloadManagerShell.DOWNLOADS,SWT.HORIZONTAL);
                                   customPB_label.setAlignment(SWT.LEFT);
                                   gridData = new GridData(GridData.FILL_HORIZONTAL);
                                   gridData.horizontalSpan = 2;
                                   customPB_label.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                   customPB_label.setLayoutData(gridData);
                                   customPB_label.setSize(customPB_label.getParent().getSize().x-70,20);
                                   customPB_label.pack();                                   
                                   DownloadManagerShell.DOWNLOADS.pack();
                                   DownloadManagerShell.DOWNLOADS.layout();                                   
                                   DownloadManagerShell.DOWNLOADS.getParent().layout();
                                   customPB_label.setSize(customPB_label.getParent().getSize().x-70,20);
                                   //System.out.println(download.getName() + " : " + customPB_label.getSize().x);
                                   customPB_label.setImage(customPB.paintProgressBar(
                                           customPB_label,
                                           customPB_label.getSize().x,
                                           20,
                                           Integer.valueOf(String.valueOf(download.getStats().getCompleted()),10),
                                           Utilities.getDisplay(),
                                           true));
                                   
                                   /*ProgressBar pb = new ProgressBar(DownloadManagerShell.DOWNLOADS, SWT.NULL);
                                   pb.setMinimum(0);
                                   pb.setMaximum(1000);
                                   pb.setSelection(download.getStats().getCompleted());
                                   pb.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                   gridData = new GridData(GridData.FILL_HORIZONTAL);
                                   gridData.horizontalSpan = 2;
                                   pb.setLayoutData(gridData);*/
                                   
                                   Label cancel = new Label(DownloadManagerShell.DOWNLOADS,SWT.NULL);
                                   cancel.setText("Cancel");
                                   cancel.setToolTipText("Cancel downloading  " + Utilities.verifyName(download.getName()));
                                   cancel.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                   cancel.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                                   cancel.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                                   gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                   gridData.horizontalSpan = 1;
                                   cancel.setLayoutData(gridData);
                                   cancel.addListener(SWT.MouseDown, new Listener(){
                                       public void handleEvent(Event e) {
                                           //pop up confirmation box for this event
                                           Shell messageshell = new Shell(DownloadManagerShell.DOWNLOAD_MANAGER_SHELL);
                                            MessageBox messageBox = new MessageBox(messageshell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
                                            messageBox.setText("Cancel Download Confirmation");
                                            messageBox.setMessage("Are you sure you want to stop downloading " + download.getName() + "?  " +
                                                    "Note:  This action will remove the file from your download list and delete the partially downloaded file.");
                                            int response = messageBox.open();
                                            switch (response){
                                            case SWT.YES:
                                                try {
                                                    if(download.getState() != Download.ST_STOPPED){
                                                        download.stop();
                                                        download.remove(true,true);
                                                        cleanOne(Utilities.verifyName(download.getName()),1);
                                                        //cleanAll();   
                                                    } else{
                                                        download.remove(true,true);
                                                        cleanOne(Utilities.verifyName(download.getName()),1);
                                                        //cleanAll();
                                                    }
                                                 }catch (DownloadRemovalVetoException e1) {
                                                     
                                                     e1.printStackTrace();
                                                 }catch (DownloadException e2) {
                                                   
                                                     e2.printStackTrace();
                                                 }
                                            }
                                            
                                          
                                               
                                           }
                                           
                                         
                                     });
                                   Label stats = new Label(DownloadManagerShell.DOWNLOADS,SWT.NULL);
                                   stats.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                   gridData = new GridData(GridData.FILL_HORIZONTAL);
                                   gridData.horizontalSpan = 3;
                                   stats.setLayoutData(gridData);
                                   
                                   
                                   int state = download.getState();
                                   if(state == Download.ST_QUEUED){
                                       stats.setText("Queued");
                                   }else if(state == Download.ST_STOPPED){
                                       stats.setText("Stopped");
                                   }else if(state == Download.ST_PREPARING){
                                       stats.setText("Getting Files Ready (Allocating/Checking)");
                                   }else if(state == Download.ST_WAITING){
                                       stats.setText("Waiting to Start");
                                   }else if(state == Download.ST_READY){
                                       stats.setText("Ready To Be Started, Please Wait");
                                   }else if(state == Download.ST_STOPPING){
                                       stats.setText("Stopping the Download, Please Wait");
                                   }else{
                                       //compile stats
                                       long stats_downloaded = (download.getStats().getDownloaded() - download.getStats().getHashFails())/1024/1024;
                                       String stats_text = stats_downloaded + "MB of " + 
                                           (download.getTorrent().getSize()/1024/1024) + "MB at " + (download.getStats().getDownloadAverage()/1024) +
                                           " KB/sec;  " + download.getStats().getETA();
                                       stats.setText(stats_text);    
                                   }
                                   
                                  
                                   Label separator = new Label(DownloadManagerShell.DOWNLOADS, SWT.HORIZONTAL | SWT.SEPARATOR);
                                   gridData = new GridData(GridData.FILL_HORIZONTAL);
                                   gridData.horizontalSpan = 3;
                                   separator.setLayoutData(gridData);
                                   
                                   
                                   
                                   
                                   //sideBar.layout();
                                   //download_comp.layout();
                                   DownloadManagerShell.DOWNLOADS.layout();
                                   DownloadManagerShell.dm_composite.layout();
                                   Rectangle r = DownloadManagerShell.sc.getClientArea();
                                   DownloadManagerShell.sc.setMinSize(DownloadManagerShell.dm_composite.computeSize(r.width,SWT.DEFAULT));    
                               }
                               
                               
                           }
                       
                       }
                   });
            
       }

       public static void addSeed(final Download download, final boolean last){
           if(Utilities.getDisplay() == null && Utilities.getDisplay().isDisposed())
               return;
           Utilities.getDisplay().asyncExec(new Runnable ()
                   {
                       public void run () 
                       {
                           boolean test = false;    
                           if(DownloadManagerShell.CURRENT_RESPONSES != null && !DownloadManagerShell.CURRENT_RESPONSES.isDisposed()){
                                   Control[] objects = DownloadManagerShell.CURRENT_RESPONSES.getChildren();
                                   
                                   for(int i = 0 ; i < objects.length ; i++){
                                       if(objects[i] != null){
                                                                                   
                                         Label temp = (Label)objects[i];
                                         if(temp.getText().equalsIgnoreCase(Utilities.verifyName(download.getName()))){
                                             if(objects[i+1] != null){
                                                 Label temp_pause = (Label)objects[i+1];
                                                 if(download.getState() == Download.ST_STOPPED){
                                                     temp_pause.setText("Resume");
                                                     temp_pause.getParent().layout();
                                                 }else{
                                                     temp_pause.setText("Pause");
                                                     temp_pause.getParent().layout();
                                                 }
                                             }
                                             if(objects[i+2] != null){
                                                 Label temp_stats = (Label)objects[i+2];
                                                 if(download.getState() == Download.ST_QUEUED){
                                                     temp_stats.setText("Queued");
                                                 }else if(download.getState() == Download.ST_STOPPED){
                                                     temp_stats.setText("Stopped");
                                                 }else{
                                                     int connected_peers = download.getPeerManager().getStats().getConnectedLeechers();
                                                     if(connected_peers == 1)
                                                         temp_stats.setText((connected_peers + " person connected, " + download.getStats().getUploadAverage()/1024 + "KB/sec"));
                                                     else
                                                         temp_stats.setText((connected_peers + " people connected, " + download.getStats().getUploadAverage()/1024 + "KB/sec"));
                                                 }
                                                 test = true;    
                                             }
                                             if(objects[i+5] != null){
                                                 Label temp_separator = (Label)objects[i+5];
                                                 try{
                                                     if(objects[i+6] != null){
                                                         //System.out.println("Not Last: " + download.getName());
                                                         temp_separator.setVisible(true);
                                                     }
                                                 }catch (Exception e){
                                                     //System.out.println("Last: " + download.getName());
                                                     temp_separator.setVisible(false);
                                                 }
          
                                             }
                                             
                                             
                                         }
                                       }
                                       
                                   }
                                   if(!test){
                                       Control[] objects_in_place = DownloadManagerShell.CURRENT_RESPONSES.getChildren();
                                       
                                       if(objects_in_place.length != 0){
                                           Label temp_sep_in_place = (Label)objects_in_place[objects_in_place.length-1];
                                           if(!temp_sep_in_place.getVisible()) temp_sep_in_place.setVisible(true);    
                                       }
                                       
                                       
                                       
                                       //name for download
                                       Label name = new Label(DownloadManagerShell.CURRENT_RESPONSES, SWT.NULL);
                                       name.setText(Utilities.verifyName(download.getName()));
                                       name.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                       
                                       pause = new Label(DownloadManagerShell.CURRENT_RESPONSES,SWT.NULL);
                                       GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                       if(download.getState() == Download.ST_STOPPED){
                                           pause.setText("Resume");
                                           pause.setToolTipText("Resume " + Utilities.verifyName(download.getName()));
                                       }else{
                                           pause.setText("Pause");
                                           pause.setToolTipText("Pause " + Utilities.verifyName(download.getName()));
                                       }
                                           
                                       
                                       pause.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                       pause.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                                       pause.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                                       pause.setLayoutData(gridData);
                                       pause.addListener(SWT.MouseDown, new Listener(){
                                           public void handleEvent(Event e) {
                                               
                                               try {
                                                   //System.out.println(pause.getText() + " : " + download.getName());
                                                   if(download.getState() != Download.ST_STOPPED){
                                                       download.stop();
                                                       //pause.setText("Resume");
                                                       //pause.getParent().layout();
                                                   }else{
                                                       download.restart();
                                                       //pause.setText("Pause");
                                                       //pause.getParent().layout();
                                                   }
                                                   
                                                   
                                                   
                                                   
                                        /*           if(pause.getText().equalsIgnoreCase("Pause")){
                                                       if(download.getState() != Download.ST_STOPPED){
                                                           download.stop();
                                                           //cleanAll();
                                                           
                                                        
                                                       }else{
                                                           download.restart();
                                                           //cleanAll();
                                                       }
                                                   }else{
                                                       download.restart();
                                                       //cleanAll();
                                                       pause.setText("Pause");
                                                       pause.getParent().layout();
                                                   }*/
                                                   
                                                                                                       
                                                } catch (DownloadException e1) {                                                   
                                                    e1.printStackTrace();
                                                }
                                                   
                                               }
                                               
                                             
                                         });
                                       
                                       
                                       //Get number of people connected
                                       
                                       connected = new Label(DownloadManagerShell.CURRENT_RESPONSES, SWT.NULL);
                                       connected.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                                       gridData = new GridData(GridData.FILL_HORIZONTAL);
                                       gridData.horizontalSpan = 1;
                                       connected.setLayoutData(gridData);
                                       connected.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                       int state = download.getState();
                                       if(state == Download.ST_QUEUED){
                                           connected.setText("Queued");
                                       }else if(state == Download.ST_STOPPED){
                                           connected.setText("Stopped");
                                       }else if(state == Download.ST_PREPARING){
                                           connected.setText("Getting Files Ready (Allocating/Checking)");
                                       }else if(state == Download.ST_WAITING){
                                           connected.setText("Waiting to Start");
                                       }else if(state == Download.ST_READY){
                                           connected.setText("Ready To Be Started, Please Wait");
                                       }else if(state == Download.ST_STOPPING){
                                           connected.setText("Stopping the Download, Please Wait");
                                       }else{
                                           int connected_peers = download.getPeerManager().getStats().getConnectedLeechers();
                                           connected.setText(connected_peers + " people connected, " + download.getStats().getUploadAverage()/1024 + "KB/sec");    
                                       }
                                       
                                       Label remove = new Label(DownloadManagerShell.CURRENT_RESPONSES,SWT.NULL);
                                       gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                       remove.setText("Remove");
                                       remove.setToolTipText("Remove " + Utilities.verifyName(download.getName()));
                                       remove.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                       remove.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                                       remove.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                                       remove.setLayoutData(gridData);
                                       remove.addListener(SWT.MouseDown, new Listener(){
                                           public void handleEvent(Event e) {
                                               //pop up confirmation box for this event
                                               Shell messageshell = new Shell(DownloadManagerShell.DOWNLOAD_MANAGER_SHELL);
                                                MessageBox messageBox = new MessageBox(messageshell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
                                                messageBox.setText("Remove File Confirmation");
                                                messageBox.setMessage("Are you sure you want to remove " + download.getName() + " from your share list?  " +
                                                        "Note:  This does not remove the file from your disk, it just stops sharing it.");
                                                int response = messageBox.open();
                                                switch (response){
                                                case SWT.YES:
                                                    try {
                                                        if(download.getState() != Download.ST_STOPPED){
                                                            download.stop();
                                                            download.remove(true,false);
                                                            //cleanAll();
                                                            cleanOne(Utilities.verifyName(download.getName()),2);
                                                        }else{
                                                            download.remove(true,false);
                                                            //cleanAll();
                                                            cleanOne(Utilities.verifyName(download.getName()),2);
                                                        }
                                                     }catch (DownloadRemovalVetoException e1) {
                                                   
                                                         e1.printStackTrace();
                                                     }catch (DownloadException e2) {
                                                       
                                                         e2.printStackTrace();
                                                     }
                                                }
                                                   
                                               }//end of handleevent
                                         });//end of listener
                                       
                                       Label hide = new Label(DownloadManagerShell.CURRENT_RESPONSES,SWT.NULL);
                                       gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                       gridData.horizontalSpan = 2;
                                       hide.setLayoutData(gridData);
                                       hide.setText("Hide");
                                       hide.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                                       hide.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                                       hide.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                                       hide.addListener(SWT.MouseDown, new Listener(){
                                           public void handleEvent(Event e) {
                                               HideFiles.addFile(Utilities.verifyName(download.getName()));
                                               cleanOne(Utilities.verifyName(download.getName()),2);
                                               DownloadManagerShell.CURRENT_RESPONSES.layout();
                                               DownloadManagerShell.setHiddenNumber();
                                           }
                                       });
                                       
                                       
                                       Label separator = new Label(DownloadManagerShell.CURRENT_RESPONSES, SWT.HORIZONTAL | SWT.SEPARATOR);
                                       gridData = new GridData(GridData.FILL_HORIZONTAL);
                                       gridData.horizontalSpan = 2;
                                       separator.setLayoutData(gridData);
                                       separator.setVisible(false);
                                       
                                       DownloadManagerShell.CURRENT_RESPONSES.layout();
                                       DownloadManagerShell.CURRENT_RESPONSES.getParent().layout();
                                       Rectangle r = DownloadManagerShell.sc.getClientArea();
                                       DownloadManagerShell.sc.setMinSize(DownloadManagerShell.dm_composite.computeSize(r.width,SWT.DEFAULT));
    
                                   }
                               }
    

                                                     
                       }
                   });

       }


       
       public static void addHidden(final Download download, final boolean last){
           if(DownloadManagerShell.COLLAPSED) return;
           if(Utilities.getDisplay() == null && Utilities.getDisplay().isDisposed())
               return;
           Utilities.getDisplay().asyncExec(new Runnable ()
                   {
                       public void run () 
                       {
                           boolean test = false;    
                           if(DownloadManagerShell.HIDDEN != null && !DownloadManagerShell.HIDDEN.isDisposed()){
                                   Control[] objects = DownloadManagerShell.HIDDEN.getChildren();
                                   
                                   for(int i = 0 ; i < objects.length ; i++){
                                       if(objects[i] != null){
                                                                                   
                                         Label temp = (Label)objects[i];
                                         if(temp.getText().equalsIgnoreCase(Utilities.verifyName(download.getName()))){
                                             test = true;    
                                         
                                             if(objects[i+3] != null){
                                                 Label temp_separator = (Label)objects[i+3];
                                                 try{
                                                     if(objects[i+4] != null){
                                                         temp_separator.setVisible(true);
                                                     }
                                                 }catch (Exception e){
                                                     temp_separator.setVisible(false);
                                                 }
          
                                             }
                                         }
                                       }
                                       
                                   }
                                   
                           }
                           
                           if(!test){
                               Control[] objects_in_place = DownloadManagerShell.HIDDEN.getChildren();
                               
                               if(objects_in_place.length != 0){
                                   Label temp_sep_in_place = (Label)objects_in_place[objects_in_place.length-1];
                                   if(!temp_sep_in_place.getVisible()) temp_sep_in_place.setVisible(true);    
                               }
                               
                               
                               
                               //name for download
                               Label name = new Label(DownloadManagerShell.HIDDEN, SWT.NULL);
                               name.setText(Utilities.verifyName(download.getName()));
                               name.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                               
                               Label remove = new Label(DownloadManagerShell.HIDDEN,SWT.NULL);
                               GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                               remove.setText("Remove");
                               remove.setToolTipText("Remove " + Utilities.verifyName(download.getName()));
                               remove.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                               remove.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                               remove.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                               remove.setLayoutData(gridData);
                               remove.addListener(SWT.MouseDown, new Listener(){
                                   public void handleEvent(Event e) {
                                       //pop up confirmation box for this event
                                       Shell messageshell = new Shell(DownloadManagerShell.DOWNLOAD_MANAGER_SHELL);
                                        MessageBox messageBox = new MessageBox(messageshell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
                                        messageBox.setText("Remove File Confirmation");
                                        messageBox.setMessage("Are you sure you want to remove " + download.getName() + " from your share list?  " +
                                                "Note:  This does not remove the file from your disk, it just stops sharing it.");
                                        int response = messageBox.open();
                                        switch (response){
                                        case SWT.YES:
                                            try {
                                                if(download.getState() != Download.ST_STOPPED){
                                                    download.stop();
                                                    download.remove(true,false);
                                                    cleanHidden();
                                                    DownloadManagerShell.setHiddenNumber();
                                                    //cleanAll();   
                                                }else{
                                                    download.remove(true,false);
                                                    cleanHidden();
                                                    DownloadManagerShell.setHiddenNumber();
                                                    //cleanAll();
                                                }
                                             }catch (DownloadRemovalVetoException e1) {
                                           
                                                 e1.printStackTrace();
                                             }catch (DownloadException e2) {
                                               
                                                 e2.printStackTrace();
                                             }
                                        }
                                           
                                       }//end of handleevent
                                 });//end of listener
                               
                               Label unHide = new Label(DownloadManagerShell.HIDDEN,SWT.NULL);
                               gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                               gridData.grabExcessHorizontalSpace = true;
                               gridData.horizontalSpan = 3;
                               unHide.setLayoutData(gridData);
                               unHide.setText("Unhide");
                               unHide.setBackground(Utilities.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                               unHide.setForeground(Utilities.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
                               unHide.setCursor(Utilities.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                               unHide.addListener(SWT.MouseDown, new Listener(){
                                   public void handleEvent(Event e) {
                                       HideFiles.removeFile(Utilities.verifyName(download.getName()));
                                       cleanOne(Utilities.verifyName(download.getName()),3);
                                       DownloadManagerShell.setHiddenNumber();
                                   }
                               });
                               
                               
                               Label separator = new Label(DownloadManagerShell.HIDDEN, SWT.HORIZONTAL | SWT.SEPARATOR);
                               gridData = new GridData(GridData.FILL_HORIZONTAL);
                               gridData.horizontalSpan = 3;
                               separator.setLayoutData(gridData);  
                               separator.setVisible(false);
                               
                               
                               DownloadManagerShell.HIDDEN.layout();
                               DownloadManagerShell.dm_composite.layout();
                               Rectangle r = DownloadManagerShell.sc.getClientArea();
                               DownloadManagerShell.sc.setMinSize(DownloadManagerShell.dm_composite.computeSize(r.width,SWT.DEFAULT));
                           }

                           
                       }
                   });
           
           
       }
       
       public static void cleanHidden(){
           if(Utilities.getDisplay() == null && Utilities.getDisplay().isDisposed())
               return;
           Utilities.getDisplay().asyncExec(new Runnable ()
                   {
                       public void run () 
                       {
                           if(DownloadManagerShell.HIDDEN != null && !DownloadManagerShell.HIDDEN.isDisposed()){
                               Control[] objects = DownloadManagerShell.HIDDEN.getChildren();
                               for(int i = 0 ; i < objects.length ; i++){
                                   objects[i].dispose();
                               }
                               
                           }
                            
                            
                            DownloadManagerShell.COMPLETED_LABEL.setText("Also uploading " + HideFiles.getCount() + " hidden files");
                            DownloadManagerShell.HIDDEN.layout();
                            DownloadManagerShell.DOWNLOADS.layout();
                            DownloadManagerShell.dm_composite.layout();
                            Rectangle r = DownloadManagerShell.sc.getClientArea();
                            DownloadManagerShell.sc.setMinSize(DownloadManagerShell.dm_composite.computeSize(r.width,SWT.DEFAULT));
                       }
                   });
       }
       
       
       
       public static void cleanAll(){
           if(Utilities.getDisplay() == null && Utilities.getDisplay().isDisposed())
               return;
           Utilities.getDisplay().asyncExec(new Runnable ()
                   {
                       public void run () 
                       {
                           if(DownloadManagerShell.CURRENT_RESPONSES != null && !DownloadManagerShell.CURRENT_RESPONSES.isDisposed()){
                               Control[] objects = DownloadManagerShell.CURRENT_RESPONSES.getChildren();
                               for(int i = 0 ; i < objects.length ; i++){
                                   objects[i].dispose();
                               }
                               
                           }
                                                    
                           if(DownloadManagerShell.DOWNLOADS != null && !DownloadManagerShell.DOWNLOADS.isDisposed()){
                               Control[] objects = DownloadManagerShell.DOWNLOADS.getChildren();
                               for(int i = 0 ; i < objects.length ; i++){
                                   objects[i].dispose();
                               }
                               DownloadManagerShell.COMPLETED_LABEL.setText("Also broadcasting " + HideFiles.getCount() + " completed files");
                               DownloadManagerShell.CURRENT_RESPONSES.layout();
                               DownloadManagerShell.DOWNLOADS.layout();
                               DownloadManagerShell.dm_composite.layout();
                               Rectangle r = DownloadManagerShell.sc.getClientArea();
                               DownloadManagerShell.sc.setMinSize(DownloadManagerShell.dm_composite.computeSize(r.width,SWT.DEFAULT));
                           }
                       }
                   });
           
       }
       
       
       /**
        * Removes one item from the downloadmanager instead of clearing them all
        * @param name -- String of name as found in the label
        * @param type -- use 1 for download, 2 for seed, 3 for hidden
        */
       
       public static void cleanOne(final String name, final int type){
           if(Utilities.getDisplay() == null && Utilities.getDisplay().isDisposed())
               return;
           Utilities.getDisplay().asyncExec(new Runnable ()
                   {
                       public void run () 
                       {
                           
                           int[] counts = Timer.getCounts();
                           
                           if(type == 1){
                               if(DownloadManagerShell.DOWNLOADS != null && !DownloadManagerShell.DOWNLOADS.isDisposed()){
                                   try{
                                       Control[] objects = DownloadManagerShell.DOWNLOADS.getChildren();
                                       for(int i = 0 ; i < objects.length ; i++){
                                           if(objects[i] != null){
                                               Label temp = (Label)objects[i];
                                               if(temp.getText().equalsIgnoreCase(name)){
                                                   temp.dispose();
                                                   for(int j = 1; j < 6 ; j++) objects[i+j].dispose();
                                                   DownloadManagerShell.DOWNLOADS.pack();
                                                   DownloadManagerShell.DOWNLOADS.layout();
                                                   DownloadManagerShell.DOWNLOADS.getParent().layout();
                                                   if(counts[1] == 0)
                                                       Timer.addEmptyDownloadMessage();
                                               }
                                           }
                                           
                                       }
                                   }catch (Exception e){
                                       
                                   }
                               }    
                           }else if(type == 2){
                               if(DownloadManagerShell.CURRENT_RESPONSES != null && !DownloadManagerShell.CURRENT_RESPONSES.isDisposed()){
                                   try{
                                       Control[] objects = DownloadManagerShell.CURRENT_RESPONSES.getChildren();
                                       for(int i = 0 ; i < objects.length ; i++){
                                           if(objects[i] != null){
                                               Label temp = (Label)objects[i];
                                               if(temp.getText().equalsIgnoreCase(name)){
                                                   temp.dispose();
                                                   for(int j = 1; j < 6 ; j++) objects[i+j].dispose();
                                                   DownloadManagerShell.CURRENT_RESPONSES.pack();
                                                   DownloadManagerShell.CURRENT_RESPONSES.layout();
                                                   DownloadManagerShell.CURRENT_RESPONSES.getParent().layout();
                                                   if(counts[2] == 0)
                                                       Timer.addEmptySeedMessage();
                                               }
                                           }
                                           
                                       }
                                   }catch (Exception e){
                                       
                                   }
                               }    
                           }else if(type == 3){
                               if(DownloadManagerShell.HIDDEN != null && !DownloadManagerShell.HIDDEN.isDisposed()){
                                   try{
                                       Control[] objects = DownloadManagerShell.HIDDEN.getChildren();
                                       for(int i = 0 ; i < objects.length ; i++){
                                           if(objects[i] != null){
                                               Label temp = (Label)objects[i];
                                               if(temp.getText().equalsIgnoreCase(name)){
                                                   temp.dispose();
                                                   for(int j = 1; j < 4 ; j++) objects[i+j].dispose();
                                                   if(counts[3] == 0 && !DownloadManagerShell.COLLAPSED){
                                                       Timer.addEmptyHiddenMessage();
                                                   }
                                               }
                                           }
                                           
                                       }
                                       //System.out.println(HideFiles.getCount());
                                       DownloadManagerShell.COMPLETED_LABEL.setText("Also broadcasting " + HideFiles.getCount() + " other files");
                                       //DownloadManagerShell.HIDDEN.layout();
                                                                   
                                       
                                   }catch (Exception e){
                                       
                                   }
                               }    
                           }
                           

                       }
                   });
           
       }
       
  
       
       
}//EOF
