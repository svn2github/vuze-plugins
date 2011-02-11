/*
 * Created on Feb 6, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import omschaub.azcvsupdater.utilities.ColorUtilities;
import omschaub.azcvsupdater.utilities.CustomProgressBar;
import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.URLReader;
import omschaub.azcvsupdater.utilities.commentmaker.CommentEditor;
import omschaub.azcvsupdater.utilities.commentmaker.CommentMaker;
import omschaub.azcvsupdater.utilities.imagerepository.ImageRepository;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;



/**
 * Tab1 Graphic initialization
 */
public class Tab1 {
    

	public static ToolItem toolbar_delete;
    public static ToolItem refresh;
    static Label pb_holder;
    static ToolBar cancel_download_tb;
    public static ToolItem cancel_download;

    
    public static Table listTable;
    
    public static Group changer;
    static Composite mini_comp1;
    public static Composite mini_comp2;
    static Group cvsTableGroup;
    
    static CustomProgressBar customProgressBar;
    
    static MenuItem insertItem, commentItem, deleteItem, comment_deleteItem;
    
    private static int COLUMN_MODIFIED_INT;   
    private static int COLUMN_FILE_INT;
    private static int COLUMN_SIZE_INT;

    
    static void open(){
        
        COLUMN_MODIFIED_INT = 1;   
        COLUMN_FILE_INT = 2;
        COLUMN_SIZE_INT = 4;
        
        
        
        View.sash.setBackground(ColorUtilities.getBackgroundColor());
        //      cvsgroup Layout -- 
		
        changer = new Group(View.composite_for_tab1, SWT.NULL);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        changer.setLayoutData(gridData);
        
        GridLayout changer_layout = new GridLayout();
        changer_layout.numColumns = 2;
        changer_layout.makeColumnsEqualWidth = false;
        changer.setLayout(changer_layout);
        
        TabFolder tabsOnTabs = new TabFolder(changer,SWT.FLAT);
        //tabsOnTabs.setSimple(true);
        
        TabItem ctab_Item1 = new TabItem(tabsOnTabs,SWT.NULL);
        ctab_Item1.setText("Dev Beta Version Information");
        
        mini_comp1 = Tab1_Subtab_1.open(tabsOnTabs);
        ctab_Item1.setControl(mini_comp1);
        
        TabItem ctab_Item2 = new TabItem(tabsOnTabs,SWT.NULL);
        ctab_Item2.setText("Information Links");
        
        Tab1_Subtab_2 subTab2 = new Tab1_Subtab_2();
        Composite subTab2_comp = subTab2.open(tabsOnTabs);
        ctab_Item2.setControl(subTab2_comp);
        
        TabItem ctab_Item3 = new TabItem(tabsOnTabs,SWT.NULL);
        ctab_Item3.setText("Settings");
        
        mini_comp2 = Tab1_Subtab_3.open(tabsOnTabs);
        ctab_Item3.setControl(mini_comp2);
        
        
        //redrawing certain items
        mini_comp2.layout();
        mini_comp1.layout();
        mini_comp1.getParent().layout();
        mini_comp1.getParent().layout();
        mini_comp1.getParent().getParent().layout();
        
        
        //-----------------------------------------------------\\
        //---------------------Table Toolbar-------------------\\
        //-----------------------------------------------------\\
        
        
//		 New Config Table Section
	  	cvsTableGroup = new Group(View.composite_for_tab1, SWT.NULL);
	  	gridData = new GridData(GridData.FILL_BOTH);
	  	gridData.horizontalSpan  = 3;
	  	gridData.verticalSpan = 5;
	  	cvsTableGroup.setLayoutData(gridData);
	  	GridLayout cvsTableGroup_layout = new GridLayout();
	  	cvsTableGroup_layout.numColumns = 3;
	  	cvsTableGroup.setLayout(cvsTableGroup_layout);
	  	cvsTableGroup.setText("Current Beta Backups");
		
        //Toolbar for listTable
        ToolBar list_tb = new ToolBar(cvsTableGroup, SWT.FLAT | SWT.HORIZONTAL);
        
        //Refresh on list_tb
        refresh = new ToolItem(list_tb,SWT.PUSH);
        refresh.setToolTipText("Refresh the current Beta backup table");
        refresh.setImage(ImageRepository.getImage("refresh"));
        refresh.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
                StatusBoxUtils.mainStatusAdd(" Beta table refreshed",0);
                Tab6Utils.refreshLists();
                URLReader.newGetURL();
              //  listTable.setTopIndex(listTable.getItemCount()-1);
                
               // TrayAlert.open();
                //System.out.println(View.getPluginInterface().getAzureusVersion());
               // Timers.checkForNewVersion(View.getPluginInterface());
                
            }
        });
        
        //delete on list_tb
        toolbar_delete = new ToolItem(list_tb, SWT.PUSH); 
        toolbar_delete.setImage(ImageRepository.getImage("delete"));
        toolbar_delete.setToolTipText("Delete selected file(s)");
        toolbar_delete.setEnabled(false);
        toolbar_delete.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                try 
                {
                    
                  
                    TableItem[] list_item = listTable.getSelection();
                    //System.out.println(list_item.length);
                    if(list_item.length==1)
                    {
                        TableItem item = list_item[0];
                        Tab1Utils.deleteselectedfile((DirectoryUtils.getBackupDirectory() + 
                                System.getProperty("file.separator") +
                                item.getText(0)), item.getText(0));
                        //String fileName = item.getText(1);
                        if (listTable != null && !listTable.isDisposed()){
                            listTable.deselectAll();
                        }

                    }
                        if(list_item.length > 1){
                            Tab1Utils.delete_multiple_files(listTable.getSelection());
                        }
                    }
                    catch (Exception e1) 
                    {
                    e1.printStackTrace();
                    }
                    Tab6Utils.refreshLists();
                    
            }
        });
        
        
               
        
        ToolItem sep = new ToolItem(list_tb,SWT.SEPARATOR);
        sep.setWidth(10);
        
        ToolItem help = new ToolItem(list_tb,SWT.PUSH);
        help.setImage(ImageRepository.getImage("help"));
        help.setToolTipText("View helpful tips about this list");
        help.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Shell shell = new Shell();
                MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                mb.setText("Helpful Hints");
                mb.setMessage("1.  Double click on a jar to fill out Insert/Backup Files correctly and switch to that tab \n\n" + 
                        " 2.  Right click on jar file to view more options");
                mb.open();
                
            }
        });
        
        //Composite for Custom Progress Bar During Download
        Composite custom_download_comp = new Composite(cvsTableGroup,SWT.NULL);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        gridLayout.makeColumnsEqualWidth = false;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;        
        custom_download_comp.setLayout(gridLayout);
        
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.grabExcessHorizontalSpace = true;
       
        //gridData.widthHint = 200;
        custom_download_comp.setLayoutData(gridData);
        
        Label placeholder = new Label(custom_download_comp,SWT.NULL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.widthHint = 50;
        placeholder.setLayoutData(gridData);
        
        //Label for custom progress Bar
        pb_holder = new Label(custom_download_comp,SWT.NULL);
        customProgressBar = new CustomProgressBar();
        pb_holder.setImage(customProgressBar.paintProgressBar(pb_holder,100,20,new Integer(0),View.getDisplay(),true));
        
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        pb_holder.setLayoutData(gridData);
        
        pb_holder.setVisible(false);
        
        
        
        //Toolbar for Transparency for Cancel download button
        cancel_download_tb = new ToolBar(custom_download_comp, SWT.FLAT);
        
        cancel_download = new ToolItem(cancel_download_tb,SWT.PUSH);
        cancel_download.setImage(ImageRepository.getImage("cancel_download"));
        cancel_download.setToolTipText("Cancel Download");
        
        cancel_download_tb.setVisible(false);             
        
        
        //-----------------------------------------------------\\
        //-------------------Table initialization--------------\\
        //-----------------------------------------------------\\
        
	 	listTable = new Table(cvsTableGroup,SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		//Headers visible
		listTable.setHeaderVisible(true);
		

		
		TableColumn columnFile = new TableColumn(listTable,SWT.NULL);
		columnFile.setText("File");
		columnFile.setWidth(200);
		columnFile.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				if(listTable == null || listTable.isDisposed() )
					return;
			
				Tab1Utils.loadDirectory(COLUMN_FILE_INT);
				View.getPluginInterface().getPluginconfig().setPluginParameter("Azureus_TableSort",COLUMN_FILE_INT);
				if (COLUMN_FILE_INT == 2)
                    COLUMN_FILE_INT = 3;
				else{
                    COLUMN_FILE_INT = 2;
				}
				
			}
		});
	
		
		TableColumn columnSize = new TableColumn(listTable,SWT.NULL);
		columnSize.setText("Size");
		columnSize.setWidth(100);
		columnSize.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				if(listTable == null || listTable.isDisposed() )
					return;
			
				Tab1Utils.loadDirectory(COLUMN_SIZE_INT);
				View.getPluginInterface().getPluginconfig().setPluginParameter("Azureus_TableSort",COLUMN_SIZE_INT); 
				if (COLUMN_SIZE_INT == 4)
                    COLUMN_SIZE_INT=5;
				else{
                    COLUMN_SIZE_INT = 4;
				}
				
			}
		});		

		TableColumn columnModified = new TableColumn(listTable,SWT.NULL);
		columnModified.setText("Date");
		columnModified.setWidth(200);
		
		columnModified.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				if(listTable == null || listTable.isDisposed() )
					return;
				Tab1Utils.loadDirectory(COLUMN_MODIFIED_INT);
				View.getPluginInterface().getPluginconfig().setPluginParameter("Azureus_TableSort",COLUMN_MODIFIED_INT);
				if (COLUMN_MODIFIED_INT == 0)
                    COLUMN_MODIFIED_INT=1;
				else
                    COLUMN_MODIFIED_INT=0;
				
			}
		});
		
		//Comment File Table Column
		TableColumn commentFile = new TableColumn(listTable,SWT.NULL);
		commentFile.setText("Comments");
		commentFile.setWidth(400);
		
		
		
		
		
		//Add a GridData in order to make it grab all the view space
    //And use the 3 columns
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 3;
		listTable.setLayoutData(gridData);
        
    
		
		//And finally add a listener to the table
    listTable.addMouseListener(new MouseAdapter() {
    	public void mouseDown(MouseEvent event) {
         // Check is the table is not disposed
    		 if(listTable == null || listTable.isDisposed())
           return;
         //Get the selection

         TableItem[] items = listTable.getSelection();
         
         
         if(items.length == 0){
         	listTable.deselectAll();
         	View.asyncExec(new Runnable (){
				public void run () {
				    if(toolbar_delete !=null && !toolbar_delete.isDisposed()){
				        if (toolbar_delete.getEnabled())
                            toolbar_delete.setEnabled(false);
				    }
					
				}
			
			});
         }
         //If an item is selected (single click selects at least one)
         if(items.length == 1) {
           //re-enable menu items
             insertItem.setEnabled(true);
             commentItem.setEnabled(true);
             if(CommentMaker.commentCheck(items[0].getText(0)))
                 comment_deleteItem.setEnabled(true);
             else 
                 comment_deleteItem.setEnabled(false);
             

           if(toolbar_delete !=null && !toolbar_delete.isDisposed())
               toolbar_delete.setEnabled(true);
    
 
         }else if(items.length > 1){
             insertItem.setEnabled(false);
             commentItem.setEnabled(false);
             comment_deleteItem.setEnabled(false);
             
         }
      }
    	
    });
    
    listTable.addMouseListener(new MouseAdapter() {
    	public void mouseDoubleClick(MouseEvent event) {
         // Check is the table is not disposed
    		 if(listTable == null || listTable.isDisposed())
           return;
         //Get the selection
    		 
         TableItem[] items = listTable.getSelection();
         
         //Set the file in tab6 and change to that tab
         if(items.length == 1){
             TableItem item = items[0];
             String selectedJar =  item.getText(0);
             Tab6Utils.insertSelect(selectedJar);
             View.tab.setSelection(View.tab6);
         }
         
    	}
    });
    

	
//Pop-up Menu 
    Menu popupmenu_table = new Menu(View.composite_for_tab1);
    
    insertItem = new MenuItem(popupmenu_table, SWT.PUSH);
    insertItem.setText("Insert as Azureus2.jar");
    insertItem.addListener(SWT.Selection, new Listener() {
    	public void handleEvent (Event e){
         // Check is the table is not disposed
    		 if(listTable == null || listTable.isDisposed())
           return;
         //Get the selection
    		 
         TableItem[] items = listTable.getSelection();
         if(items.length == 1){
             View.tab.setSelection(View.tab6);
             Tab6Utils.clearLists();
             Tab6Utils.insertSelect(items[0].getText(0));
         }
         
    	}
    });
    
    deleteItem = new MenuItem(popupmenu_table, SWT.PUSH);
    deleteItem.setText("Delete File(s)");
    deleteItem.addListener(SWT.Selection, new Listener() {
    	public void handleEvent (Event e){
			try {
//   			 Check if the table is not disposed
	       		 if(listTable == null || listTable.isDisposed())
	              return;
	            //Get the selection
	            TableItem[] items = listTable.getSelection();
	            
	            //If an item is selected (single click selects at least one)
	            if(items.length == 1) {
		            //Grab selected item
		            TableItem item = items[0];
					Tab1Utils.deleteselectedfile(DirectoryUtils.getBackupDirectory()+
					        System.getProperty("file.separator")+
					        item.getText(0),item.getText(0));
					if (listTable != null && !listTable.isDisposed()){
					    listTable.deselectAll();
					}
			            }
				if(items.length > 1){
			        Tab1Utils.delete_multiple_files(listTable.getSelection());
			    }
			}catch (Exception e1) {
				e1.printStackTrace();
			}
			Tab6Utils.refreshLists();
    	}
    });
    
    commentItem = new MenuItem(popupmenu_table, SWT.PUSH);
    commentItem.setText("Read/Edit Comments");
    commentItem.addListener(SWT.Selection, new Listener() {
    	public void handleEvent (Event e){
			try {
//   			 Check is the table is not disposed
	       		 if(listTable == null || listTable.isDisposed())
	              return;
	            //Get the selection
	            TableItem[] items = listTable.getSelection();
	            TableItem item=null;
	            //If an item is selected (single click selects at least one)
	            if(items.length == 1) {
	              //Grab selected item
	              item = items[0];
					String textTemp = CommentMaker.commentOpen(item.getText(0), false);
					CommentEditor.open(textTemp,item.getText(0),View.getPluginInterface());
	            }
			}catch (Exception e1) {
				System.out.println("caught error");
				e1.printStackTrace();
			}
    	}
    });
    
//Menu Item delete Comment File
    
    comment_deleteItem = new MenuItem(popupmenu_table, SWT.PUSH);
    comment_deleteItem.setText("Delete Comment File");
    comment_deleteItem.addListener(SWT.Selection, new Listener() {
    	public void handleEvent (Event e){
			try {
			    //Check if the table is not disposed
	       		 if(listTable == null || listTable.isDisposed())
	              return;
	            
                 //Get the selection
	            TableItem[] items = listTable.getSelection();
	            
	            //If an item is selected (single click selects at least one)
	            TableItem item = null;
	            if(items.length == 1) {
	              //Grab selected item
	              item = items[0];

	            }
				
	            // AMC: Not sure of the best behaviour, but better to return rather than NPE'ing lower down.
	            if (item == null) return;
	            
				File checkdir = new File(DirectoryUtils.getBackupDirectory() + System.getProperty("file.separator") + "comments");
				File inFile = new File(checkdir + System.getProperty("file.separator") + item.getText(0)+".txt");
				//String commentFile = new String(checkdir + System.getProperty("file.separator") + item.getText(1)+".txt");
				if (!inFile.isFile()){
					//deleteselectedfile(commentFile);
					return;
				}
				final BufferedReader in = new BufferedReader(new FileReader(inFile));
				String testText = in.readLine() ;
				if (testText == null){
					if (inFile.isFile()){
					inFile.delete() ;
					}
					in.close();
				}
				else {
					//System.out.println(inFile);
					in.close();
					Tab1Utils.comment_delete(inFile);
					
				}
			}catch (Exception e1) {
				e1.printStackTrace();
			}
			Tab1Utils.loadDirectory(View.getPluginInterface().getPluginconfig().getPluginIntParameter("Azureus_TableSort",2));
			Tab6Utils.refreshLists();
    	}
    });    
    
        // Set the menu to the table
        listTable.setMenu( popupmenu_table);
        
        //new listener to gray out appropriate items unless certain criteria are met
        popupmenu_table.addMenuListener(new MenuListener(){
            public void menuHidden(MenuEvent arg0) {
                
                
            }

            public void menuShown(MenuEvent arg0) {
                insertItem.setEnabled(false);
                deleteItem.setEnabled(false);
                commentItem.setEnabled(false);
                comment_deleteItem.setEnabled(false);
                
                if(listTable.getItemCount() > 0){
                    
                }
                
                TableItem[] item = listTable.getSelection();                        
                if(item.length == 1){
                    deleteItem.setEnabled(true);
                    insertItem.setEnabled(true);
                    commentItem.setEnabled(true);
                    if(CommentMaker.commentCheck(item[0].getText(0)))
                        comment_deleteItem.setEnabled(true);
                    else 
                        comment_deleteItem.setEnabled(false);
                }else if(item.length > 1){
                    deleteItem.setEnabled(true);
                    insertItem.setEnabled(false);
                    commentItem.setEnabled(false);
                    comment_deleteItem.setEnabled(false);
                }
            }
        });
              
    }
    
    
    
    public static void downloadVisible(final boolean visible, final boolean isHTTP){
        View.getDisplay().syncExec(new Runnable (){
            public void run () 
            {
                
                if(pb_holder != null && !pb_holder.isDisposed()){
                    if(pb_holder.getParent() != null && !pb_holder.getParent().isDisposed()){
                        pb_holder.getParent().layout();
                    }    
                }
                
                if(pb_holder != null && !pb_holder.isDisposed()){
                    pb_holder.setVisible(visible);                    
                }
                
                if(!isHTTP){
                    if(cancel_download_tb != null && !cancel_download_tb.isDisposed()){
                        cancel_download_tb.setVisible(visible);
                    }    
                }
                
               
                
            }
        });
    }
    
    public static void setCustomPB(final Integer complete){
        View.getDisplay().syncExec(new Runnable (){
            public void run () 
            {
                if(pb_holder != null && !pb_holder.isDisposed()){
                    /*if(!pb_holder.isVisible()){
                        pb_holder.setVisible(true);
                    }*/
                    customProgressBar = new CustomProgressBar();
                    pb_holder.setImage(customProgressBar.paintProgressBar(pb_holder,100,20,complete,View.getDisplay(),true));
                }
            }
        });
    }


    public static void redrawSubTab(){
        View.getDisplay().syncExec(new Runnable (){
            public void run () {
                mini_comp1.pack();
                mini_comp1.layout();
                mini_comp2.pack();
                mini_comp2.layout();
                mini_comp1.getParent().pack();
                mini_comp1.getParent().layout();
                mini_comp1.getParent().getParent().pack();
                mini_comp1.getParent().getParent().layout();
                changer.pack();
                changer.layout();        
            }
        });
    }
    
//EOF
}
