/*
 * Created on Jan 15, 2006
 */
package lbms.azcatdest.gui;

import java.util.Properties;

import lbms.azcatdest.main.Plugin;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;



import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;


public class View implements UISWTViewEventListener {
	
    boolean isCreated = false;    
    public static final String VIEWID = "AZCatDest_View";
    
	//The current Display
	private static Display display;
	
	//This view main GUI object
	public static Composite composite;
	
    
    //The Plugin interface
    PluginInterface pluginInterface;
    
    //The toolbar for adding a category and a destination
    private static ToolBar toolBar1;
    private static ToolItem add, remove, refresh;
    
    //The main table
    private static Table table1; 
    
    
  public View(PluginInterface pluginInterface) {
    this.pluginInterface = pluginInterface;
      
  }
	
	
	
	/**
	 * Here is the GUI initialization
	 */
	public void initialize(Composite parent) {
               
        
		// We store the Display variable as we'll need it for async GUI Changes
		display = parent.getDisplay();

                
        //Before starting the GUI we will initiate all of the pics
        ImageRepository.loadImages(display);
      
        //Main composite layout
		composite = new Composite(parent,SWT.NULL);
		
        //Make the composite have a grid layout
        GridLayout layout = new GridLayout();

        //set num of columns and margins
        layout.numColumns = 1;


        //set the composite to the layout
        composite.setLayout(layout);
		
        //set the griddata for the composite
        GridData gridData = new GridData(GridData.FILL_BOTH);
        
        composite.setLayoutData(gridData);
        
        //toolbar initialization
        toolBar1 = new ToolBar(composite,SWT.BORDER | SWT.FLAT);
        
        //add new cat / directory destination button to toolbar
        add = new ToolItem(toolBar1, SWT.PUSH);
        add.setImage(ImageRepository.getImage("add"));
        add.setToolTipText("Add a new category and destination");
        
        
        //-- Listener for add
        
        add.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                final Shell shell = new Shell();
                shell.setLayout(new GridLayout(2,false));
                shell.setText("New Category");
                shell.setImage(ImageRepository.getImage("add"));
                
                Label label = new Label(shell,SWT.NULL);
                label.setText("Enter New Category Name:                    ");
                GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                gridData.horizontalSpan = 2;
                label.setLayoutData(gridData);
                
                final Text input = new Text(shell,SWT.SINGLE | SWT.BORDER);
                gridData = new GridData(GridData.FILL_HORIZONTAL);
                gridData.horizontalSpan = 2;
                input.setLayoutData(gridData);
                input.addKeyListener(new KeyListener() {
                    public void keyPressed(KeyEvent e) {
                        //Empty
                    }
                    
                    public void keyReleased (KeyEvent e) {
                        switch (e.character){
                        case SWT.CR:
                            String text = input.getText();
                            if(input.getText().length() > 0){
                                //Make the category in Azureus
                                TorrentAttribute ta = Plugin.getPluginInterface().getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);                                
                                ta.addDefinedValue(text);
                                //Add it to our Properties
                                Plugin.getProperties().setProperty(text, "");
                                
                                populateTable();
                                shell.dispose();
                            }
                        case SWT.ESC:
                            shell.dispose();
                        break;
                        }
                    }
                });

                Button cancel = new Button(shell, SWT.PUSH);
                cancel.setText("Cancel");
                cancel.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event e) {
                        shell.dispose();
                    }
                });
                
                Button accept = new Button(shell, SWT.PUSH);
                accept.setText("Accept");
                accept.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event e) {
                        String text = input.getText();
                        if(input.getText().length() > 0){
                            //Make the category in Azureus
                            TorrentAttribute ta = Plugin.getPluginInterface().getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);                                
                            ta.addDefinedValue(text);
                            //Add it to our Properties
                            Plugin.getProperties().setProperty(text, "");
                            
                            populateTable();
                            shell.dispose();
                        }
                    }
                });
                
                //open shell
                GraphicUtils.centerShellandOpen(shell);
                
                
            }
        });
        
        
        //remove button on toolbar
        remove = new ToolItem(toolBar1, SWT.PUSH);
        remove.setImage(ImageRepository.getImage("cancel"));
        remove.setEnabled(false);
        remove.setToolTipText("Delete selected category");
        //-- Listener for Remove
        remove.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                TableItem[] toDie = table1.getSelection();
                if(toDie.length == 1){
                    MessageBox messageBox = new MessageBox(toolBar1.getShell(), SWT.ICON_QUESTION | SWT.NO | SWT.YES);
                    messageBox.setText("Delete Confirmation");
                    messageBox.setMessage("Are you sure you want to remove the category " + toDie[0].getText(0) + "?");
                    int response = messageBox.open();
                    switch (response){
                    case SWT.YES:
                        String name = toDie[0].getText(0);
                        TorrentAttribute ta = Plugin.getPluginInterface().getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
                        ta.removeDefinedValue(name);
                        Plugin.getProperties().remove(name);
                        Plugin.saveConfig();
                        populateTable();
                        remove.setEnabled(false);
                        break;                        
                    }
                }                
            }
        });
        
        new ToolItem(toolBar1, SWT.SEPARATOR);
        
        refresh = new ToolItem(toolBar1, SWT.PUSH);
        refresh.setImage(ImageRepository.getImage("reload"));
        refresh.setToolTipText("Update the table by reloading all categories from Azureus");
        refresh.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                populateTable();
            }
        });
        
        
        //----------Table----------\\
        table1 = new Table(composite,SWT.SINGLE | SWT.BORDER |  SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
        gridData = new GridData(GridData.FILL_BOTH);        
        table1.setLayoutData(gridData);
        table1.setHeaderVisible(true);
        
        TableColumn name = new TableColumn(table1, SWT.NULL);
        name.setText("Category Name");
        name.setWidth(200);
        
        TableColumn directory = new TableColumn(table1, SWT.NULL);
        directory.setText("Directory for Category");        
        directory.setWidth(400);
        
        //listener to deselect if outside an item
        table1.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                if(e.button == 1) {
                    if(table1.getItem(new Point(e.x,e.y))==null){
                        table1.deselectAll();
                        remove.setEnabled(false);
                    }
                
                }
            }
        });
        
        
        //Listener to enable the remvoe button if table is selected
        table1.addListener (SWT.Selection, new Listener () {
            public void handleEvent (Event event) {
                remove.setEnabled(false);
                TableItem[] toDie = table1.getSelection();
                if(toDie.length == 1){
                    remove.setEnabled(true);                    
                }
            }
        });
        
        
        //Listener for table double clicks
        final TableEditor editor = new TableEditor (table1);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        
        
        table1.addListener (SWT.MouseDoubleClick, new Listener () {
            public void handleEvent (Event event) {
                Rectangle clientArea = table1.getClientArea ();
                Point pt = new Point (event.x, event.y);
                int index = table1.getTopIndex ();
                while (index < table1.getItemCount ()) {
                    boolean visible = false;
                    final TableItem item = table1.getItem (index);
                    for (int i=0; i<table1.getColumnCount (); i++) {
                        
                            Rectangle rect = item.getBounds (i);
                            if (rect.contains (pt)) {
                                //if(i == 0){
                                   /* final String oldName = item.getText(0);
                                    final String oldDir = item.getText(1);
                                    final int column = i;
                                    final Text text = new Text (table1, SWT.NONE);
                                    Listener textListener = new Listener () {
                                        public void handleEvent (final Event e) {
                                            switch (e.type) {
                                            case SWT.FocusOut:
                                                item.setText (column, text.getText ());
                                                System.out.println(oldName);                                                
                                                Plugin.getProperties().remove(oldName);
                                                if(oldDir.startsWith("Double Click to")){
                                                    Plugin.getProperties().setProperty(text.getText(), "");
                                                }else{
                                                    Plugin.getProperties().setProperty(text.getText(), oldDir);
                                                }
                                                Plugin.saveConfig();
                                                text.dispose ();
                                                break;
                                            case SWT.Traverse:
                                                switch (e.detail) {
                                                case SWT.TRAVERSE_RETURN:
                                                    item.setText (column, text.getText ());
                                                    Plugin.getProperties().remove(oldName);
                                                    if(oldDir.startsWith("Double Click to")){
                                                        Plugin.getProperties().setProperty(text.getText(), "");
                                                    }else{
                                                        Plugin.getProperties().setProperty(text.getText(), oldDir);
                                                    }
                                                    Plugin.saveConfig();
                                                    //FALL THROUGH
                                                case SWT.TRAVERSE_ESCAPE:
                                                    text.dispose ();
                                                    e.doit = false;
                                                }
                                                break;
                                            }
                                        }
                                    };
                                    text.addListener (SWT.FocusOut, textListener);
                                    text.addListener (SWT.Traverse, textListener);
                                    editor.setEditor (text, item, i);
                                    text.setText (item.getText (i));
                                    text.selectAll ();
                                    text.setFocus ();
                                    return;*/
                                //}else if(i == 1){
                                    //open directory dialog here
                                    DirectoryDialog dialog = new DirectoryDialog (table1.getShell());
                                    dialog.setText("Choose Save Directory for Category");
                                    String dialogResult = dialog.open();
                                    if(dialogResult != null){
                                        item.setText(1, dialogResult);
                                        Plugin.getProperties().setProperty(item.getText(0), dialogResult);
                                        Plugin.saveConfig();
                                    }
                                //}
                            }
                            if (!visible && rect.intersects (clientArea)) {
                                visible = true;
                            }
                        
                    }
                    if (!visible) return;
                    index++;
                }
            }
        });
        
        populateTable();
        
        
	composite.layout();
	//END
	}
	


    private void addCategoryToTable(String name){
        TableItem item = new TableItem(table1,SWT.NULL);
        item.setText(0, name);
        String directory = Plugin.getProperties().getProperty(name, "");
        if(directory.equalsIgnoreCase("")) directory = "Double Click to set destination directory";
        item.setText(1,directory);
        //gray if needed
        int index = table1.indexOf(item);
        if(index%2!=0){
            item.setBackground(ColorUtilities.getBackgroundColor());
        }
        
    }
    
    
    
    /**
     * Redraws table1.. since it is virtual, we need to repopulate it
     * each time the user array is modified
     * 
     */
    public static void redrawTable(){
        // Reset the data so that the SWT.Virtual picks up the array
        Plugin.getDisplay().syncExec(new Runnable() {
            public void run() {
                if (table1 == null || table1.isDisposed()) 
                    return;
                
                try{
                    //TODO need a length from the container
                    
                    //userTable.setItemCount(Plugin.getXMLConfig().getUserList().length);    
                }catch (Exception e){
                    table1.setItemCount(0);
                }
                
                table1.clearAll();
                
                
                
            }
        });
    }
    
    
    
    
    
    
    

/**
 * Delete function... runs at close of plugin from within Azureus
 */

public void delete() {
     

    isCreated = false;


}

public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
    
    
    case UISWTViewEvent.TYPE_CREATE:
        if (isCreated)
          return false;
        
        isCreated = true;
        break;
    
        
    case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;
        
        
        
    case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;
        
    }

    return true;
}


public Properties reReadCategories(){
    TorrentAttribute ta = Plugin.getPluginInterface().getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
    String[] categories = ta.getDefinedValues();
    Properties properties = Plugin.getProperties();
    for (String cat:categories){
        if(properties.getProperty(cat,null) == null) properties.setProperty(cat,"");
    }
    return properties;
}


public void populateTable(){
    try{
     if(table1 != null || !table1.isDisposed()){
         table1.removeAll();
         TorrentAttribute ta = Plugin.getPluginInterface().getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
         String[] categories = ta.getDefinedValues();
         for (String cat:categories){
             addCategoryToTable(cat);
         }     
     }
    }catch(Exception e){};
    
}

//EOF
}
