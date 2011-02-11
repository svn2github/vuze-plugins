/*
 * Created on Feb 6, 2005
 * Created by omschaub
 *
 */
package omschaub.azcvsupdater.main;

import java.io.File;
import java.io.RandomAccessFile;

import omschaub.azcvsupdater.utilities.ButtonStatus;
import omschaub.azcvsupdater.utilities.ColorUtilities;
import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.commentmaker.CommentMaker;
import omschaub.azcvsupdater.utilities.download.MainCVSGet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


/**
 * @author omschaub
 * Tab1 Utilities
 */
public class Tab1Utils {



/**  Delete the Selected File
 *
 * @param filetodie
 * @param name
 */
  static void deleteselectedfile(final String filetodie, final String name) {
  	if (filetodie == null){
  	    if (Tab1.listTable != null && !Tab1.listTable.isDisposed()){
            Tab1.listTable.deselectAll();
  	    }
        ButtonStatus.set(true, true, false, true,true);
  	    return;
  	}
      Thread deleteFileThread = new Thread()
  	{
  		public void run()
  		{

  		    final File timetodie = new File(filetodie);
  		    if (timetodie.isDirectory() == true)
  		    {
  				StatusBoxUtils.mainStatusAdd(" You seem to have chosen a directory..   Action Cancelled",2);
  				return;
  		    }

  		    if (timetodie.isFile() == false)
  		    {
  				StatusBoxUtils.mainStatusAdd("Please select a file first",2);
  				return;
  		    }

  		    if(View.getDisplay()==null && View.getDisplay().isDisposed())
  		        return;

            View.DML_BOOLEAN = false;
  		    View.getDisplay().asyncExec(new Runnable (){
  		        public void run ()
  		        {
  		            Shell shell = new Shell(View.getDisplay());
  		            MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
  		            messageBox.setText("Delete Confirmation");
  		            messageBox.setMessage("Are you sure you want to delete " + timetodie + "?");
  		            int response = messageBox.open();
  		            switch (response){
  		            case SWT.YES:
  		                try
  		                {
  		                    MainCVSGet.removeDownload(name,View.getPluginInterface());
  		                    if(!View.DML_BOOLEAN);
  		                    {
  		                        timetodie.delete();
  								//System.out.println("AZCVSUPDATER:  dml_boolean is false.. straight delete");

  		                    }
  		                    StatusBoxUtils.mainStatusAdd(" Deleted file " + timetodie,0);
  							File checkdir = new File(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator") + "comments");
  							File inFile = new File(checkdir + System.getProperty("file.separator") + name+".txt");
  							//String commentFile = new String(checkdir + System.getProperty("file.separator") + name+".txt");
  							if (inFile.isFile())
  							{
  							    comment_delete(inFile);
  							}

  		                }
  		            catch (Exception e)
  		            {
  		                e.printStackTrace() ;
  		            }

  		            loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
  		            Tab6Utils.refreshLists();
  		            //StatusBoxUtils.mainStatusAdd(" File Deleted");
  					shell.dispose();
  					break;
  		            case SWT.NO:
  		                //loadDirectory(type);
  		                StatusBoxUtils.mainStatusAdd(" File Delete Cancelled",0);
  						if (Tab1.listTable !=null && !Tab1.listTable.isDisposed())
  						{
                            Tab1.listTable.deselectAll();
  						}
  						if (Tab1.toolbar_delete !=null && !Tab1.toolbar_delete.isDisposed())
  						{
                            Tab1.toolbar_delete.setEnabled(false);
  						}
  						shell.dispose();
  						break;
  		            }
  		        }
  		    });
  		}
  	};
  	deleteFileThread.setDaemon(true);
    deleteFileThread.start();
  }

  static void delete_multiple_files(TableItem[] list_item){
      String label_files="";
      String fileName;
      String backup_dir = DirectoryUtils.getBackupDirectory();
      for (int i = 0; i< list_item.length; i++){
          TableItem item = list_item[i];
          fileName = item.getText(0);
          label_files=label_files + fileName + "\n";

      }

      Shell shell = new Shell();
  	MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
  	messageBox.setText("Delete Confirmation");
  	messageBox.setMessage("Are you sure you want to delete these files: \n" + label_files);
  	int response = messageBox.open();
  	switch (response){
  		case SWT.YES:
  		    try {
  		    for (int i = 0; i< list_item.length; i++){
  		        TableItem item = list_item[i];
  		        fileName = item.getText(0);

  		        File timetodie = new File(backup_dir + System.getProperty("file.separator") + fileName);

  		    	if (timetodie.isDirectory() == true){

  		    				StatusBoxUtils.mainStatusAdd(" You seem to have chosen a directory..   Action Cancelled",2);

  		            return;
  		    	}

  		    	if (timetodie.isFile() == false){

  		    				StatusBoxUtils.mainStatusAdd(" Error.. File to delete does not seem to be an actual file",2);

  		    		return;
  		    	}
  		        MainCVSGet.removeDownload(fileName,View.getPluginInterface());
  		    	if(!View.DML_BOOLEAN);
  		    	{
  		    	    timetodie.delete();
  		    	}

  		    	StatusBoxUtils.mainStatusAdd(" Deleted file " + timetodie,0);

  		    	//check for comment file
  		    	File checkdir = new File(backup_dir + System.getProperty("file.separator") + "comments");
  				File inFile = new File(checkdir + System.getProperty("file.separator") + fileName+".txt");
  				//String commentFile = new String(checkdir + System.getProperty("file.separator") + fileName+".txt");
  				if (inFile.isFile()){
  					comment_delete(inFile);

  				}

  		    }

  		    } catch (Exception e){
  				e.printStackTrace() ;
  			}

  			    StatusBoxUtils.mainStatusAdd(" Multiple Files Deleted",0);

  		    shell.dispose();
  		    loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
  		    Tab6Utils.refreshLists();
  		    break;
  		case SWT.NO:

  			    StatusBoxUtils.mainStatusAdd(" Multiple File Deletion Cancelled",0);

  		    shell.dispose();
  		    break;
  	}
  }



  public static void loadDirectory(final int type) {
        if(View.LOAD_DIR){
            View.LOAD_DIR = false;
            return;
        }

        View.LOAD_DIR = true;
        //String installdirectory = DirectoryUtils.getInstallDirectory();

		final Thread load_directory_thread = new Thread() {
			public void run() {
				try {

					if(View.getDisplay()== null && View.getDisplay().isDisposed())
					    return;

				    View.getDisplay().syncExec(new Runnable (){
						public void run () {
						    if (Tab1.listTable !=null && !Tab1.listTable.isDisposed()){
                                Tab1.listTable.setEnabled(true);
                                Tab1.listTable.removeAll();
						    }
                            if(Tab1.pb_holder != null && !Tab1.pb_holder.isDisposed()){
                                if(Tab1.pb_holder.isVisible()){
                                    Tab1.downloadVisible(false,false);
                                }
                            }

						}
					});
					File [] files = fileDateSort(type);

					for(int i = 0 ; i < files.length ; i++) {
						String fileName = files[i].getName();
						if(!fileName.startsWith("Azureus"))
							continue;
						int fileSizeInKB = 0;
						try {
							//Create the raf as read only
							RandomAccessFile raf = new RandomAccessFile(files[i],"r");
							fileSizeInKB = (int) (raf.length() / 1024l);
							raf.close();
						} catch(Exception e) {
							e.printStackTrace();
						}
						long lastModified = files[i].lastModified();
						//String fileURL = CVSurlGetter(fileName);
						//urlMap.put(fileName,fileURL);
						addTableElement(fileName,fileSizeInKB,lastModified,Tab1.listTable);
					}

					//We also need to re-enable the browse button
                    ButtonStatus.set(true, true, false, true, true);

                    View.LOAD_DIR = false;

				} catch(Exception e) {
					//Stop process and trace the exception
					e.printStackTrace();
				}
			}
		};

    //Before starting our Thread, we remove all elements in the table
		if(View.getDisplay()==null && View.getDisplay().isDisposed())
		    return;
		View.getDisplay().asyncExec(new Runnable (){
			public void run () {
			    if (Tab1.listTable !=null && !Tab1.listTable.isDisposed()){

			        Tab1.listTable.removeAll();
                    Tab1.listTable.redraw();
                    Tab1.listTable.setEnabled(true);
			    }
                load_directory_thread.setDaemon(true);
			    load_directory_thread.start();
			}
		});


	}


	static void comment_delete(File commentFiletoDie){
		Shell shell = new Shell();
		MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
		messageBox.setText("Comment File Delete Confirmation");
		messageBox.setMessage("Are you sure you want to delete the Comment File: \n" + commentFiletoDie + "?");
		int response = messageBox.open();
		switch (response){
			case SWT.YES:
				commentFiletoDie.delete();
				//loadDirectory(type);
				//this.mainStatus.setText(" Idle...");
				StatusBoxUtils.mainStatusAdd(" Deleted Comment file " + commentFiletoDie,0);
				shell.dispose();
				break;
			case SWT.NO:

				//this.mainStatus.setText(" Idle...");
				shell.dispose();
				break;
		}

	}

	static void addTableElement(final String fileName,final int fileSizeInKb,final long lastModified, final Table table_to_add /*,final String fileURL*/) {

	    if(View.getDisplay() == null || View.getDisplay().isDisposed())
	      return;

	    View.getDisplay().asyncExec( new Runnable() {
	        public void run() {
		         if(table_to_add == null || table_to_add.isDisposed())
			            return;
		         TableItem item = new TableItem(table_to_add,SWT.NULL);
		         if (table_to_add.getItemCount()%2==0) {
		          	item.setBackground(ColorUtilities.getBackgroundColor());
		          }

		//final int table_to_addCount = table_to_add.getItemCount();
		item.setText(0,fileName);
		item.setText(1,fileSizeInKb + " KB");

	     	String date = View.formatDate(lastModified);

	        item.setText(2,date);

			String textTemp;
			textTemp = CommentMaker.commentOpen(fileName, true);
			item.setText(3,textTemp);


	        }
	    });


	  }

	static File[] fileDateSort(int type){
		/**
		*type = 0 is Date of CVS Build ascending
		*type = 1 is Date of CVS Build descending
		*type = 2 is File ascending
		*type = 3 is File descending
		*type = 4 is Size ascending
		*type = 5 is Size descending
		**/

		File f = new File(DirectoryUtils.getBackupDirectory());
		File[] files = f.listFiles();
		int filesLength = files.length;

	    long[] fileModified = new long[ filesLength ];
	    long[] fileSize = new long[ filesLength];

	    // Parsing
	    for ( int i = 0; i < filesLength; i++ ){
	        fileModified[i] = files[i].lastModified();
	        fileSize[i] = files[i].length();
	    }

	    // Bubblesort

	   //type 0 = Date of CVS Build ascending
	    	if (type==0){
	            for ( int i = 0; i < filesLength; i++ ){
	                for ( int j = 0; j < filesLength - 1; j++ ){
	                // This does the actual comparison
	                if ( fileModified[j] < fileModified[j+1] ||
	                    ( fileModified[j] == fileModified[j+1] && files[j].compareTo( files[j+1] ) > 0 ) ){
	                    // Swapping
	                	fileModified[j] ^= fileModified[j+1];
	                	fileModified[j+1] ^= fileModified[j];
	                	fileModified[j] ^= fileModified[j+1];
	                    File temp = files[j];
	                    files[j] = files[j+1];
	                    files[j+1] = temp;
	                }
	                }
	            }
	        }
	   //type 1 = Date of CVS Build descending
	   if (type==1){
	    for ( int i = 0; i < filesLength; i++ ){
	        for ( int j = 0; j < filesLength - 1; j++ ){
	        // This does the actual comparison
	        if ( fileModified[j] > fileModified[j+1] ||
	            ( fileModified[j] == fileModified[j+1] && files[j].compareTo( files[j+1] ) > 0 ) ){
	            // Swapping
	        	fileModified[j] ^= fileModified[j+1];
	        	fileModified[j+1] ^= fileModified[j];
	        	fileModified[j] ^= fileModified[j+1];
	            File temp = files[j];
	            files[j] = files[j+1];
	            files[j+1] = temp;
	        }
	        }
	    }
	    }


	    //type = 2 is File ascending

	  	if (type==2){
	        for ( int i = 0; i < filesLength; i++ ){
	            for ( int j = 0; j < filesLength - 1; j++ ){
	            // This does the actual comparison
	            if ( files[j].compareTo( files [j+1]) > 0 ||
	                ( files[j].compareTo( files[j+1]) == 0  && fileModified[j] > fileModified[j+1]) ){
	                // Swapping
	            	fileModified[j] ^= fileModified[j+1];
	            	fileModified[j+1] ^= fileModified[j];
	            	fileModified[j] ^= fileModified[j+1];
	                File temp = files[j];
	                files[j] = files[j+1];
	                files[j+1] = temp;
	            }
	            }
	        }
	    }

	  	//	type = 3 is File descending

	  	if (type==3){
	        for ( int i = 0; i < filesLength; i++ ){
	            for ( int j = 0; j < filesLength - 1; j++ ){
	            // This does the actual comparison
	            if ( files[j+1].compareTo( files [j]) > 0 ||
	                ( files[j+1].compareTo( files[j]) == 0  && fileModified[j] > fileModified[j+1]) ){
	                // Swapping
	            	fileModified[j] ^= fileModified[j+1];
	            	fileModified[j+1] ^= fileModified[j];
	            	fileModified[j] ^= fileModified[j+1];
	                File temp = files[j];
	                files[j] = files[j+1];
	                files[j+1] = temp;
	            }
	            }
	        }
	    }

	    //type = 4 is Size ascending
		if (type==4){
	        for ( int i = 0; i < filesLength; i++ ){
	            for ( int j = 0; j < filesLength - 1; j++ ){
	            // This does the actual comparison
	            if ( fileSize[j] < fileSize[j+1] ||
	                ( fileSize[j] == fileSize[j+1] && files[j].compareTo( files[j+1] ) > 0 ) ){
	                // Swapping
	            	fileSize[j] ^= fileSize[j+1];
	            	fileSize[j+1] ^= fileSize[j];
	            	fileSize[j] ^= fileSize[j+1];
	                File temp = files[j];
	                files[j] = files[j+1];
	                files[j+1] = temp;
	            }
	            }
	        }
	    }
	    //type = 5 is Size descending

		   if (type==5){
		    for ( int i = 0; i < filesLength; i++ ){
		        for ( int j = 0; j < filesLength - 1; j++ ){
		        // This does the actual comparison
		        if ( fileSize[j] > fileSize[j+1] ||
		            ( fileSize[j] == fileSize[j+1] && files[j].compareTo( files[j+1] ) > 0 ) ){
		            // Swapping
		        	fileSize[j] ^= fileSize[j+1];
		        	fileSize[j+1] ^= fileSize[j];
		        	fileSize[j] ^= fileSize[j+1];
		            File temp = files[j];
		            files[j] = files[j+1];
		            files[j+1] = temp;
		        }
		        }
		    }
		    }

	        return files;
	    }

	//EOF
}
