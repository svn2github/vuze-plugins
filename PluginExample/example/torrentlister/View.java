/*
 * Created on 8 nov. 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package example.torrentlister;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginView;

/**
 * @author Olivier
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class View extends PluginView {
	
	//The current Display
	private Display display;
	
	//This view main GUI object
	private Composite composite;
	
	//The String holding current directory value
	String directoryName;
	
	//The Label holding the current directory name
	Label directoryValue;
	
	//The Browse button;
	Button browse;
	  
	//The Table holding the current listing
	Table listTable;
	
	//The Plugin interface
  PluginInterface pluginInterface;
  
  public View(PluginInterface pluginInterface) {
    this.pluginInterface = pluginInterface;
  }
	
	/**
	 * The Plugin name, as it'll be seen within the Plugins Menu in azureus
	 */
	public String getPluginViewName() {
		return "Torrent Lister";
	}
	
	/**
	 * The plugin Title, used for its Tab name in the main window
	 */
	public String getFullTitle() {		
		return "Torrent Lister (Plugin Example)";
	}
	
	/**
	 * Here stands any GUI initialisation
	 */
	public void initialize(Composite parent) {
		// We store the Display variable as we'll need it for async GUI Changes
		this.display = parent.getDisplay();
		
		// We create our own composite, on which we'll add our components.
		this.composite = new Composite(parent,SWT.NULL);
		
		//We'll use a GridLayout, as this layout is simple and effective
		//Our view will consist of 4 elements :
		// the current directory title label
		// the current directory value label
		// the Browse button to change directory
		// a Table to list torrents
		
		GridLayout layout = new GridLayout();
		//3 columns, as first row will have 3 elements.
		layout.numColumns = 3;
		composite.setLayout(layout);
		
		//First element
		Label directoryTitle = new Label(composite,SWT.NULL);
		directoryTitle.setText("Directory :");
		
		//2nd element (class member in order to be changed later)
		directoryValue = new Label(composite,SWT.NULL);
		directoryValue.setText("Hit Browse to list a directoy");
		
		//We'll use a GridData in order to make this Label take the maximum
		//space horizontaly
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		directoryValue.setLayoutData(gridData);
				
		//3rd element : PUSH style is the default style for normal buttons
		browse = new Button(composite,SWT.PUSH);
		browse.setText("Browse...");
		browse.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				loadDirectory();
			}
		});
		
		//New Row (as it's 3 elements per row)
		
		//4th Element : the table
		listTable = new Table(composite,SWT.SINGLE | SWT.BORDER);
		//Headers visible
		listTable.setHeaderVisible(true);
		
		//This table columns :
		String[] columnNames = {"File" ,"Size" ,"Last Modified" };
		//and their width
		int[] columnWidths = {500,100,200};
		
		//Initialise this table
		for(int i = 0 ; i < columnNames.length ; i++) {
			TableColumn column = new TableColumn(listTable,SWT.NULL);
			column.setText(columnNames[i]);
			column.setWidth(columnWidths[i]);
		}
		
		//Add a GridData in order to make it grab all the view space
    //And use the 3 columns
		gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 3;
		listTable.setLayoutData(gridData);
        
    //And finally add a double click listener to the table
    listTable.addMouseListener(new MouseAdapter() {
    	public void mouseDoubleClick(MouseEvent event) {
         // Check is the table is not disposed
    		 if(listTable == null || listTable.isDisposed())
           return;
         //Get the selection
         TableItem[] items = listTable.getSelection();
         
         //If an item is selected (double click selects at least one)
         if(items.length == 1) {
           //Grab selected item
           TableItem item = items[0];
           String fileName = item.getText(0);
           String fullFileName = directoryName + System.getProperty("file.separator") + fileName;           
           pluginInterface.openTorrentFile(fullFileName);
         }
      }
    });
	}
	
	/**
	 * This method will be called after initialization, in order to grab this
	 * view composite.
	 */
	public Composite getComposite() {
		return this.composite;
	}
	
	
	private void loadDirectory() {
		//As we're called from an SWT Event, we're inside the GUI Thread
		//Therefore we can access the GUI Elements
		
		// Disable the 'browse' button
		this.browse.setEnabled(false);
		
		// Show up a directory window :
		// Requires a shell , let's get it from composite
		DirectoryDialog dd = new DirectoryDialog(composite.getShell());
		
		// If we already opened a Directory, let's say the current open path
		if(this.directoryName != null) {
			dd.setFilterPath(this.directoryName);
		}
		
		// This Method opens the Directory Browser,
		// and returns the selected Directory as a String
		String newDirectory = dd.open();
		
		// If it's null, then 'cancel' as been hit
		// Return
		if(newDirectory == null)
			return;
		
		// Else
		this.directoryName = newDirectory;
		
		//Also set the directory on the label
		this.directoryValue.setText(this.directoryName);
		
		//Ok, we could do stuff synchronously, but heh, no FUN with that
		//So, let's create a Thread that will scan the directory, asynchronously
		//And fill the Table
		Thread t = new Thread() {
			public void run() {
				//Working on files ... it's allways better to use a try / catch :)
				try {
					File f = new File(directoryName);
					//Checks for directory, shouldn't be a problem
					//as we got it from a Directory Dialog...
					if(!f.exists() || !f.isDirectory())
						return;
					//We filter files manualy :p
					File[] files = f.listFiles();
					//Ok we're demonstrating a async process, so let's show it
					//In a full way.
					//What must be understood here, is that NO Blocking process
					//should run within an SWT Event handle, or within any SWT Thread
					//access (like the View Creation). SWT only uses 1 Thread, blocking
					//this thread means freezing the GUI.
					//Of course listing files is a really fast operation,
					//And therefore this could have been done directly wihtin the
					//event handler code. Here's we're listing files, 1 per 1, and then
					//adding each to the Table, by calling a method that will queue
					//the 'add' operation to SWT's Thread. This is done as a DEMONSTRATION
					//and is not the right way of doing things. You should post as little
					//things to SWT's thread.
					for(int i = 0 ; i < files.length ; i++) {
						String fileName = files[i].getName();
            if(!fileName.endsWith(".torrent"))
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
            addTableElement(fileName,fileSizeInKB,lastModified);
					}
                    
					//We also need to re-enable the browse button
          enableBrowseButton();
				} catch(Exception e) {
					//Stop process and trace the exception
					e.printStackTrace();
				}
			}
		};
        
    //Before starting our Thread, we remove all elements in the table
    listTable.removeAll();
    t.start();
	}
    
  /**
   * Ok, the params are declared final cause we'll need them in an inner class.
   * @param fileName
   * @param fileSizeInKb
   * @param lastModified
   */
  private void addTableElement(final String fileName,final int fileSizeInKb,final long lastModified) {
    //As this method isn't called from the SWT Thread
    //(but, in our example from the created 't' Thread)
    //We need to 'post' GUI processing to the Display
    
    //We check if display isn't null, and not disposed
    if(display == null || display.isDisposed())
      return;
    
    //And the 'magic' method :D
    display.asyncExec( new Runnable() {
        public void run() {
         //Some time has passed, and so the table might be disposed
         //We must check for it
         if(listTable == null || listTable.isDisposed())
            return;
         TableItem item = new TableItem(listTable,SWT.NULL);
         item.setText(0,fileName);
         item.setText(1,fileSizeInKb + " Kb");
         
         //We use a Calendar to format the Date
         Calendar calendar = GregorianCalendar.getInstance();
         calendar.setTimeInMillis(lastModified);
         String date =   calendar.get(Calendar.DAY_OF_MONTH) + "/"
                       + calendar.get(Calendar.MONTH) + "/"
                       + calendar.get(Calendar.YEAR) + " "
                       + calendar.get(Calendar.HOUR) + ":"
                       + calendar.get(Calendar.MINUTE);
         item.setText(2,date);
        }        
    });
    
    
  }
	/**
   * cf. addTableElement for comments
	 *
	 */
	private void enableBrowseButton() {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec( new Runnable() {
        public void run() {
           if(browse == null || browse.isDisposed())
            return;
           browse.setEnabled(true);
        }
    });
  }

}
