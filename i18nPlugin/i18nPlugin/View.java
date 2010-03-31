/*
 * View.java
 *
 * Created on February 22, 2004, 5:50 PM
 */

package i18nPlugin;


import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

/** Main view of i18nPlugin
 *
 * @author  TuxPaper
 */
public class View implements UISWTViewEventListener {
	public final static String VIEWID = "i18nAZ";
	
  /** Full path (resource bundle entry key) key name for TableTreeItem.get/setData() */
  public static final String DATAKEY_PATH = "Path";
  /** Locale object key name for TableTreeItem.get/setData() */
  public static final String DATAKEY_LOCALE = "locale";
  public static final String DATAKEY_COMMENT = "Comment";

  public static final String AZUREUS_BUNDLE = "org.gudy.azureus2.internat.MessagesBundle";
  public static final String AZUREUS_PLUGIN_NAME = "(core)";

  /** Location in Azureus2.jar where the resource bundle is stored */
  public static String BUNDLE_LOCATION;
  /** BUNDLE_LOCATION as a path (for use in GetResource) */
  public static String BUNDLE_FOLDER;
  /** Resource Bundle name we look for */
  public static String BUNDLE_NAME;
  /** Extension of the resource bundles we look for */
  public static final String BUNDLE_EXT = ".properties";
  
  static {setBundlePath(AZUREUS_BUNDLE);}
  
  public static void setBundlePath(String langfile) {
	  int pkg_part = langfile.lastIndexOf('.');
	  if (pkg_part == -1) {
		  BUNDLE_LOCATION = BUNDLE_FOLDER = "";
		  BUNDLE_NAME = langfile;
	  }
	  else {
		  BUNDLE_LOCATION = langfile.substring(0, pkg_part); 
		  BUNDLE_FOLDER = BUNDLE_LOCATION.replace('.', '/');
		  BUNDLE_NAME = langfile.substring(pkg_part + 1);
	  }
  }
  

  private Composite cView;
  private TableTree tt;
  private TableCursor cursor;
  private Label lInfo;
  private Image imgMissing;
  private Image imgExtra;
  private URL[] urls;

  private boolean bShowOnlyMissing = false;
  private boolean bFlat = false;
  private boolean bMultiLineEdit = false;

  private PluginInterface pluginInterface;
  private LoggerChannel log;

  private String sDefaultPath;
  private int iNumMissing = 0;
  private int iNumExtra = 0;
  
  // List of all the keys present
  ArrayList keyList;
  String[] keysSorted;
  // Position in keylist that the original ends
  // Anything after this value will be extra entries added by other bundles
  int posOriginalEnds = 0;
  
  Map keyComments;

  // Key = Locale
  // Data = Map of values
  //          Key = bundle entry key
  //          Data = bundle entry value
  // Future: Make localList store a class in for its Data
  //         The class will hold the key/value entries as well as other info
  //         (like Save Directory)
  Map localeList;
  
  // Each column contains a reference to its locale
  
  boolean isCreated = false;

	protected String sSearch;

	private Pattern searchPattern;

	private Label lblComment;

	private final UISWTInstance swtInstance;
	
  /**
   * Creates a new instance of View
   * @param pluginInterface Access to Azureus plugin interface
   * @param swtInstance 
   */
  public View(PluginInterface pluginInterface, UISWTInstance swtInstance) {
    this.pluginInterface = pluginInterface;
		this.swtInstance = swtInstance;
		
    log = pluginInterface.getLogger().getChannel("i18nEditor");
    log.log(LoggerChannel.LT_INFORMATION, "i18nEditor View Startup");

    try {
      sDefaultPath = new File(pluginInterface.getPluginDirectoryName()).getParentFile().getParent();
    } catch (Exception e) { 
      sDefaultPath = "";
    }
}
  
  /**
   * This method is called after initialize so that the Tab is set its control
   * Caller is the GUI Thread.
   * @return the Composite that should be set as the control for the Tab item
   */
  public Composite getComposite() {
    return cView;
  }
  
  /**
   * Called when the language needs updating
   */
  private void updateLanguage() {
  }
  
  /**
   * This method is called when the view is instantiated, it should initialize all GUI
   * components. Must NOT be blocking, or it'll freeze the whole GUI.
   * Caller is the GUI Thread.
   * @param composite the parent composite. Each view should create a child composite, and then use this child composite to add all elements to.
   */
  public void initialize(Composite composite) {
	setBundlePath(AZUREUS_BUNDLE);
    log.log(LoggerChannel.LT_INFORMATION, "Initialize");
    
    GridData gd;
    
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("images/missing.gif");
    imgMissing = new Image(composite.getDisplay(), is);
    is = this.getClass().getClassLoader().getResourceAsStream("images/extra.gif");
    imgExtra = new Image(composite.getDisplay(), is);
    
    
    cView = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    cView.setLayout(layout);
    
    Composite cOptions = new Composite(cView, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    cOptions.setLayout(layout);
    gd = new GridData(SWT.FILL, SWT.FILL, false, false);
    gd.verticalSpan = 2;
    cOptions.setLayoutData(gd);
    
    // option
    final Button btnOnlyMissing = new Button(cOptions, SWT.CHECK);
    btnOnlyMissing.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        bShowOnlyMissing = btnOnlyMissing.getSelection();
        buildTable();
      }
    });
    btnOnlyMissing.setText("Show only missing/extra");
    
    // option
    final Button btnFlat = new Button(cOptions, SWT.CHECK);
    btnFlat.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        bFlat = btnFlat.getSelection();
        buildTable();
      }
    });
    btnFlat.setText("Flat (No Tree)");

    // option
    final Button btnFancyEditor = new Button(cOptions, SWT.CHECK);
    btnFancyEditor.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        bMultiLineEdit = btnFancyEditor.getSelection();
      }
    });
    btnFancyEditor.setText("Fancier Editor (Allows Enter,Tab, instead of \\n, \\t)");

    
    Composite cButtons = new Composite(cView, SWT.NULL);
    layout = new GridLayout(5, false);
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cButtons.setLayout(layout);
    gd = new GridData(SWT.FILL, SWT.FILL, true, false);
    cButtons.setLayoutData(gd);
    
    // button
    Button btnAddLang = new Button(cButtons, SWT.PUSH);
    btnAddLang.setText("Add Language");
    btnAddLang.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        addLang();
      }
    });
    
    // button
    Button btnNewLang = new Button(cButtons, SWT.PUSH);
    btnNewLang.setText("New Language");
    btnNewLang.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        newLang();
      }
    });
    
    // button
    Button btnSaveAll = new Button(cButtons, SWT.PUSH);
    btnSaveAll.setText("Save..");
    btnSaveAll.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        saveAll();
      }
    });
    
    // button
    Button btnHelp = new Button(cButtons, SWT.PUSH);
    btnHelp.setText("Help");
    btnHelp.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
      	String sText = "";
  			String helpFullPath = "/readme.txt";
  			InputStream stream = getClass().getResourceAsStream(helpFullPath);
  			if (stream == null) {
  				sText = "Error loading resource: " + helpFullPath;
  			} else {
  				try {
  					sText = readInputStreamAsString(stream, 65535, "utf8");
  					stream.close();
  				} catch (IOException err) {
  				}
  			}
  			Shell shell = new Shell(cView.getShell(), SWT.DIALOG_TRIM);
  			shell.setText("i18AZ Help");
  			shell.setLayout(new FillLayout());
  			Text txt = new Text(shell, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
  			txt.setText(sText);
  			txt.setEditable(false);
  			shell.setSize(640, 480);
  			shell.open();
      }
    });

    final Combo pluginChooser = new Combo(cButtons, SWT.READ_ONLY);
    final Map plugin_mapping = populatePluginMapping(); 
    Iterator itr = plugin_mapping.keySet().iterator();
    while (itr.hasNext()) {
    	String key = (String)itr.next();
    	if (key.equals("")) {key = AZUREUS_PLUGIN_NAME;}
    	pluginChooser.add(key);
    }
    pluginChooser.addSelectionListener(new SelectionListener() {
    	public void widgetSelected(SelectionEvent e) {
    		String key = pluginChooser.getText();
    		if (key.equals(AZUREUS_PLUGIN_NAME)) {key = "";}
    		System.err.println("key is " + key);
    		String bundle_path = (String)plugin_mapping.get(key);
    		System.err.println("plugin chooser:: " + bundle_path);
    		setBundlePath(bundle_path);
    	    try {initListFromBundle();}
    	    catch (Exception ee) { log.log(ee); }
    		buildTable();
    	}
    	public void widgetDefaultSelected(SelectionEvent e) {
    		widgetSelected(e);
    	}
    });
    
    lInfo = new Label(cButtons, SWT.NULL);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 4;
    lInfo.setLayoutData(gd);
    
    
    // search

    Composite cSearch = new Composite(cView, SWT.NULL);
    layout = new GridLayout(4, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cSearch.setLayout(layout);
    gd = new GridData(SWT.FILL, SWT.FILL, true, false);
    cSearch.setLayoutData(gd);
    
    
    Label lblSearch = new Label(cSearch, SWT.NULL);
    lblSearch.setText("Filter:");
    gd = new GridData();
    lblSearch.setLayoutData(gd);
    
    
    final Text txtSearch = new Text(cSearch, SWT.BORDER);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    txtSearch.setLayoutData(gd);
    txtSearch.addSelectionListener(new SelectionAdapter() {
    	public void widgetDefaultSelected(SelectionEvent e) {
    		setSearch(txtSearch.getText());
    	}
    });
    
    Button btnSearch = new Button(cSearch, SWT.PUSH);
    btnSearch.setText("Go");
    btnSearch.addSelectionListener(new SelectionAdapter() {
    	public void widgetSelected(SelectionEvent e) {
    		setSearch(txtSearch.getText());
    	}
    });
    
    Button btnClearSearch = new Button(cSearch, SWT.PUSH);
    btnClearSearch.setText("Clear");
    btnClearSearch.addSelectionListener(new SelectionAdapter() {
    	public void widgetSelected(SelectionEvent e) {
    		setSearch(null);
    	}
    });
    
    
    
    // table
    tt = new TableTree(cView,  SWT.FULL_SELECTION | SWT.SINGLE);
    gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 4;
    tt.setLayoutData(gd);
    final Table table = tt.getTable();
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    
    tt.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
      	if (cursor != null && !cursor.isDisposed()) {
      		cursor.setSelection(tt.getTable().getSelection()[0], cursor.getColumn());
      	}
      	updateComment((String)((TableTreeItem)e.item).getData(DATAKEY_PATH));
      }
    });


    TableColumn tcFirst = new TableColumn(table, SWT.LEFT);
    tcFirst.setText("Key");
    tcFirst.setWidth(200);
    
    lblComment = new Label(cView, SWT.WRAP);
    lblComment.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    try {initListFromBundle();}
    catch (Exception e) { log.log(e); }

    buildTable();
}
  
  private void initListFromBundle() throws Exception {
	    // Setup default ResourceBundle
	    keyList = new ArrayList();
	    keyComments = new HashMap();
	    localeList = new HashMap();
	    
	      // Get the jarURL
	      // XXX Is there a better way to get the JAR name?
	      String resource_loc = BUNDLE_FOLDER + "/" + BUNDLE_NAME + BUNDLE_EXT;

	      String sJar = getResource(resource_loc).toString(); 
	      sJar = sJar.substring(0, sJar.length() - BUNDLE_NAME.length() - BUNDLE_EXT.length());
	      URL jarURL = new URL(sJar);

	      // Add Original's keys to the list in the order they are in the file
	      // (getBundle(..) doesn't return them in original order)
	      URL urlBundle = new URL(sJar + BUNDLE_NAME + BUNDLE_EXT);
	      InputStream ins = urlBundle.openStream();
	      BufferedInputStream bis = new BufferedInputStream(ins);
	      InputStreamReader isr = new InputStreamReader(bis);
	      BufferedReader reader= new BufferedReader(isr);
	      
	      boolean skipNext = false;
	      String sComment = null;
	      while (reader.ready()) {
	        String sLine = reader.readLine();
	        boolean wasSkipNext = skipNext;
	        
	        String sTrimmedLine = sLine.trim();
	        skipNext = sTrimmedLine.endsWith("\\");

	        if (wasSkipNext) {
	        	continue;
	        }
	        
	        if (sTrimmedLine.startsWith("#")) {
	        	if (sComment != null) {
	        		sComment = sComment + "\n" + sTrimmedLine;
	        	} else {
	        		sComment = sTrimmedLine;
	        	}
	        } else {
	          String[] sSplit = sLine.split("=", 2);
	          
	          if (sSplit.length > 1) {
	            String sKey = sSplit[0].trim();
	            if (!keyList.contains(sKey))
	              keyList.add(sKey);
	            
	            if (sComment != null) {
	            	keyComments.put(sKey, sComment);
	            	sComment = null;
	            }
	          }
	        }
	      }
	      reader.close();
	      isr.close();
	      bis.close();
	      ins.close();
	      
	      posOriginalEnds = keyList.size() - 1;

	      
	      // Add Reference Language, which is the default locale inside the jar
	      urls = new URL[] {jarURL};
	      addLocale(new Locale("", ""), urls);
	    
  }

  
  /**
	 * @param data
	 *
	 * @since 3.0.1.3
	 */
	protected void updateComment(String key) {
  	String sComment = (String) keyComments.get(key);
  	if (sComment == null) {
  		lblComment.setText("");
  	} else {
  		lblComment.setText(sComment.replaceAll("\\n", "\\\\n"));
  	}
	}

	/**
	 * @param text
	 */
	protected void setSearch(String text) {
		boolean bRegexSearch = false;
		
		if (text != null && text.length() > 0) {
			sSearch = (bRegexSearch) ? text : "\\Q"
				+ text.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
	  	searchPattern = Pattern.compile(sSearch, Pattern.CASE_INSENSITIVE);
		} else {
			searchPattern = null;
			sSearch = null;
		}
		buildTable();
	}

	/** Resets the table and rebuilds all the language columns.
   */
  private void buildTable() {
    if (cursor != null && !cursor.isDisposed()) {
      cursor.setVisible(false);
    }
    tt.removeAll();

    TableTreeItem ttiLast = null;
    iNumMissing = 0;
    iNumExtra = 0;
    for (int iKeyNo = 0; iKeyNo < keysSorted.length; iKeyNo++) {
      boolean bHasOneMissing = false;
      boolean bHasExtra = false;
      boolean bMatchesSearch = (sSearch == null);
      
      String sKey = keysSorted[iKeyNo];
      int iKeySuffixPos = sKey.lastIndexOf('.') + 1;
      //String sKeyParent = sKey.substring(0, iKeySuffixPos);
      String sSuffix = sKey.substring(iKeySuffixPos);
      
      String[] sValues = new String[tt.getTable().getColumnCount()];
      Image[] images = new Image[tt.getTable().getColumnCount()];
      sValues[0] = (bFlat) ? sKey :  sSuffix;
      
      if (!bMatchesSearch) {
      	bMatchesSearch = searchPattern.matcher(sKey).find();
      }

      // Build sValues[]
      for (int iCol = 1; iCol < tt.getTable().getColumnCount(); iCol++) {
        Locale locale = (Locale)tt.getTable().getColumn(iCol).getData(DATAKEY_LOCALE);
        Map mapEntries = (HashMap)localeList.get(locale);
        if (mapEntries == null) {mapEntries = Collections.EMPTY_MAP;}
        
        String sValue = (String)mapEntries.get(sKey);
        if (sValue == null) {
          sValue = "";
        } else {
          sValue = sValue.replaceAll("\\n",  "\\\\n");
          sValue = sValue.replaceAll("\\t",  "\\\\t");
        }

        if (!bMatchesSearch) {
        	bMatchesSearch = searchPattern.matcher(sValue).find();
        }

        sValues[iCol] = sValue;
        if (iCol > 1 && sValue != "") {
          if (sValues[1] == "") {
            images[iCol] = imgExtra;
            bHasExtra = true;
            iNumExtra++;
          } else if (sValue.equals(sValues[1])) {
          	mapEntries.remove(sKey);
          	sValue = "";
          	
            images[iCol] = imgMissing;
            bHasOneMissing = true;
            iNumMissing++;
          }
        }
      }

      // Finish building images[]
      for (int iCol = 2; iCol < tt.getTable().getColumnCount(); iCol++) {
        if (images[iCol] == null && sValues[iCol] == "" && sValues[1] != "") {
          images[iCol] = imgMissing;
          bHasOneMissing = true;
          iNumMissing++;
        }
      }
      
      if (bMatchesSearch && (!bShowOnlyMissing || bHasOneMissing || bHasExtra)) {
        TableTreeItem ttiParent = (bFlat) ? null : buildTreePath(sKey, ttiLast);
        if (ttiParent == null)
          ttiLast = new TableTreeItem(tt, SWT.NULL);
        else
          ttiLast = new TableTreeItem(ttiParent, SWT.NULL);
        ttiLast.setData(DATAKEY_PATH, sKey);
        Object commentsObject = keyComments.get(sKey);
        if (commentsObject instanceof String) {
        	ttiLast.setData(DATAKEY_COMMENT, commentsObject);
        }
        for (int x = 0; x < sValues.length; x++) {
          ttiLast.setText(x, sValues[x]);
          if (images[x] != null) ttiLast.setImage(x, images[x]);
        }
      }
      
    } // KeysSorted

    if (cursor != null && !cursor.isDisposed()) {
      if (tt.getTable().getColumnCount() > 1 && tt.getItemCount() > 0) {
        cursor.setSelection(0,2);
        cursor.setVisible(true);
      }
    }
    updateInfoLabel();
  }
  
  /** The time to comment such a small and self-expanatory function could much
   * better be spent on writing a comment that tells you that this function is
   * too small and self-explanatory to be commented.
   **/
  private void updateInfoLabel() {
    String s = keysSorted.length + " keys. " + iNumMissing + " missing/unchanged";
    if (iNumExtra > 0) {
      s += " and " + iNumExtra + " extra (mislabeled or no longer used)";
    }
    lInfo.setText(s);
  }

  /** Makes sure the branches are in the tree, building the path if necessary.
   * @param sPath Path to verify/build
   * @param ttiLeaf Leaf that the path belongs on.
   * @return the leaf (TableTreeItem) with they key of sPath
   */
  private TableTreeItem buildTreePath(String sPath, TableTreeItem ttiLeaf) {
    TableTreeItem ttiParent = ttiLeaf;
    //if (ttiParent != null) log.log(LoggerChannel.LT_INFORMATION, "  Last= "+ttiParent.getText(0));
    while (ttiParent != null && !sPath.equals((String)ttiParent.getData(DATAKEY_PATH))) {
      ttiParent = ttiParent.getParentItem();
      //if (ttiParent != null) log.log(LoggerChannel.LT_INFORMATION, "  Parent= "+ttiParent.getText(0));
    }

    if (ttiParent == null) {
      String sPaths[] = sPath.split("\\.");
      String sCurPath = "";
      for (int j = 0; j < sPaths.length - 1; j++) {
        if (j>0) sCurPath += ".";
        sCurPath += sPaths[j];


        // first, check if it's up the path
        TableTreeItem ttiFind = ttiLeaf;
        while (ttiFind != null && !sCurPath.equals((String)ttiFind.getData(DATAKEY_PATH))) {
          ttiFind = ttiFind.getParentItem();
        }
        if (ttiFind != null)
          ttiParent = ttiFind;

        //log.log(LoggerChannel.LT_INFORMATION, "  "+sCurPath+"ttiFind==null"+(ttiFind==null)+" "+(ttiParent==null?"null":ttiParent.getData(DATAKEY_PATH)));

        if (ttiFind == null) { // && Collections.binarySearch(alEnglish, sCurPath) < 0) {
          if (j == 0 || ttiParent == null)
            ttiParent = new TableTreeItem(tt, SWT.NULL);
          else
            ttiParent = new TableTreeItem(ttiParent, SWT.NULL);
          ttiParent.setText(0, sPaths[j]);
          ttiParent.setData(DATAKEY_PATH, sCurPath);
          Object commentsObject = keyComments.get(sCurPath);
          if (commentsObject instanceof String) {
          	ttiParent.setData(DATAKEY_COMMENT, commentsObject);
          }
          ttiParent.setData("EmptyParent", new Boolean(true));
          ttiParent.setBackground(tt.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        }
      }
    }
    return ttiParent;
 }
  
  private void buildCursor(Table table) {
    cursor = new TableCursor(table,SWT.NULL);

    cursor.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent e) {
      	setSelection(cursor.getRow());
        editCell();
      }
    });

    cursor.addSelectionListener(new SelectionAdapter() {
      // when the cursor is over a cell, select the corresponding row in 
      // the table
      public void widgetSelected(SelectionEvent e) {
        int column = cursor.getColumn();
        if (column < 2) {
          cursor.setSelection(cursor.getRow(), 2);
        }
        setSelection(cursor.getRow());
      }
      // when the user hits "ENTER" in the TableCursor, pop up a text editor so that 
      // they can change the text of the cell
      public void widgetDefaultSelected(SelectionEvent e){
      	setSelection(cursor.getRow());
        editCell();
      }
    });

    cursor.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        if (e.character == '+') {
          tt.getSelection()[0].setExpanded(true);
        } else if (e.character == '-') {
          tt.getSelection()[0].setExpanded(false);
        }
      }
    });
  }
  
  private void setSelection(TableItem item) {
    tt.getTable().setSelection(new TableItem[] {item});
  	updateComment((String) tt.getSelection()[0].getData(DATAKEY_PATH));  	
  }

  /** Pops up an LangEditor box for the current cursor entry.
   */
  private void editCell() {
    if (cursor == null)
      return;
    
    TableTreeItem row = tt.getSelection()[0];
    int column = cursor.getColumn();
    String sOldValue = row.getText(column);
    if (sOldValue == null) {
      sOldValue = "";
    }
    LangEditor langEditor = new LangEditor(row, column, bMultiLineEdit);
    if (langEditor.sValue != null && !sOldValue.equals(langEditor.sValue)) {
      Locale locale = (Locale)tt.getTable().getColumn(column).getData(DATAKEY_LOCALE);
      if (locale != null) {
        Map mapEntries = (Map)localeList.get(locale);
        if (mapEntries != null) {
          String sRealValue = langEditor.sValue;
          row.setText(column, sRealValue);
          sRealValue = sRealValue.replaceAll("\\\\n", "\n");
          sRealValue = sRealValue.replaceAll("\\\\t", "\t");
          mapEntries.put(row.getData(DATAKEY_PATH), sRealValue);
        }
      }

      // Check if it was the same as the reference, and decrease num missing
      String sRefValue = row.getText(1);
      if (sRefValue == null) {
        sRefValue = "";
      }
      if (sRefValue.equals(sOldValue)) {
        iNumMissing--;
        updateInfoLabel();
      }
      if (sRefValue == "" && langEditor.sValue == "" && sOldValue != "") {
        iNumExtra--;
        updateInfoLabel();
      }
      
    }
  }

  /** Builds a map of keys/values, updates keyList, and adds the locale to
   *  the table.
   **/
  private void addLocale(Locale locale, URL[] urls) {
    ResourceBundle rb;
    Table table = tt.getTable();
    try {
      rb = ResourceBundle.getBundle(BUNDLE_NAME, locale, 
                                    new URLClassLoader(urls));
    } catch (MissingResourceException e) {
      // TODO: notify user here
      e.printStackTrace();
      return;
    }

    TableColumn tc = new TableColumn(table, SWT.LEFT);
    if (table.getColumnCount() == 2)
      tc.setText("Reference");
    else
      tc.setText(locale.getDisplayName());
    tc.setWidth(200);
    tc.setData(DATAKEY_LOCALE, locale);
    
    if (tt.getTable().getColumnCount() > 2 && (cursor == null || cursor.isDisposed())) {
      buildCursor(table);
    }
    
    tc.addControlListener(new ControlAdapter () {
      public void controlResized(ControlEvent e) {
        if (cursor != null && !cursor.isDisposed())
          cursor.setSelection(cursor.getRow(), cursor.getColumn());
      }
    });

    Map mapEntries = new HashMap();
    for (Enumeration enumeration = rb.getKeys(); enumeration.hasMoreElements();) {
      Object key = enumeration.nextElement();
      if (!keyList.contains(key))
        keyList.add(key);
      mapEntries.put(key, rb.getObject((String)key));
    }    
    localeList.put(locale, mapEntries);

    keysSorted = (String[])keyList.toArray(new String[keyList.size()]);
    Arrays.sort(keysSorted, String.CASE_INSENSITIVE_ORDER);
  }

  /** Saves all languages except the reference language */
  private void saveAll() {
    DirectoryDialog dialog = new DirectoryDialog(cView.getShell(), SWT.APPLICATION_MODAL);
    dialog.setFilterPath(sDefaultPath);
    dialog.setText("Save " + View.BUNDLE_NAME + "_*");
    dialog.setMessage("Select a folder to save your MessagesBundle_* files to:");
    String path = dialog.open();
    if (path != null) {
      sDefaultPath = path;
      File fDir = new File(path);
      TableColumn[] columns = tt.getTable().getColumns();
      for (int i = 2; i < columns.length; i++) {
        Locale locale = (Locale)columns[i].getData(DATAKEY_LOCALE);;
        String sFileName = View.BUNDLE_NAME;
        if (locale.getLanguage() != "")
          sFileName += "_" + locale.getLanguage();
        if (locale.getCountry() != "")
          sFileName += "_" + locale.getCountry();
        if (locale.getVariant() != "")
          sFileName += "_" + locale.getVariant();
        sFileName += View.BUNDLE_EXT;
        
        HashMap mapEntries = (HashMap)localeList.get(locale);

        try {
        	Writer fw;
        	if (Util.doUTF8) {
						fw = new BufferedWriter(new OutputStreamWriter(
								new FileOutputStream(fDir + fDir.separator + sFileName),
								"UTF-8"));
        	} else {
        		fw = new FileWriter(fDir + fDir.separator + sFileName);
        	}

          saveItems(fw, mapEntries);
          fw.close();
        } catch (Exception e) { };
        
      }
    }
  }

  /** Saves one language */
  private void saveItems(Writer fw, Map mapEntries) {

    try {
      for (int i = 0; i < keyList.size(); i++) {
        if (i == posOriginalEnds + 1) {
          fw.write("# The remaining keys were not in " + BUNDLE_NAME + BUNDLE_EXT + "\n");
        }
        String sKey = (String)keyList.get(i);
        String sValue = (String)mapEntries.get(sKey);
        String sComment = (String)keyComments.get(sKey);
        if (sComment != null) {
        	fw.write(sComment + "\n");
        }
        if (sValue != null && sValue != "") {
          fw.write(sKey + "=" + Util.toEscape(sValue) + "\n");
        }
      }
    } catch (IOException e) { 
      e.printStackTrace();
    } catch (Exception e) { 
      e.printStackTrace(); 
    }
  }
  
  /** Calls the AddLanguageDialog, then adds the locale and rebuilds the table */
  private void addLang() {
    AddLanguageDialog dialog = new AddLanguageDialog(cView.getDisplay(), urls, sDefaultPath);
    if (dialog.localesSelected != null) {
    	for (int i = 0; i < dialog.localesSelected.length; i++) {
        addLocale(dialog.localesSelected[i], dialog.urls);
			}
      buildTable();
      tt.setFocus();
    }
  }

  private void newLang() {
    AddLanguageDialog dialog = new AddLanguageDialog(cView.getDisplay(), urls, null);
    if (dialog.localesSelected != null) {
    	for (int i = 0; i < dialog.localesSelected.length; i++) {
        addLocale(dialog.localesSelected[i], dialog.urls);
			}
      buildTable();
      tt.setFocus();
    }
  }

  /**
   * This method is caled when the view is destroyed.
   * Each color instanciated, images and such things should be disposed.
   * The caller is the GUI thread.
   */
  public void delete() {
    Widget[] widgetsToDispose = {cursor, tt, lInfo, cView};
    for (int i = 0; i < widgetsToDispose.length; i++) {
      if (widgetsToDispose[i] != null) {
        if (!widgetsToDispose[i].isDisposed()) {
        	try {
            widgetsToDispose[i].dispose();
					} catch (Exception e) {
						// TODO: handle exception
					}
        }
        widgetsToDispose[i] = null;      
      }
    }
    if (!imgExtra.isDisposed()) {
      imgExtra.dispose();
    }
    imgExtra = null;
    if (!imgMissing.isDisposed()) {
      imgMissing.dispose();
    }
    imgMissing = null;
  }

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				if (isCreated)
					return false;

				isCreated = true;
				break;
				
			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				isCreated = false;
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite)event.getData());
				event.getView().setTitle("Internationalize Azureus");
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				updateLanguage();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				break;
		}
		return true;
	}
  
	public static String
	readFileAsString(
		File	file,
		int		size_limit,
		String charset)
	
		throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return readInputStreamAsString(fis, size_limit, charset);
		} finally {

			fis.close();
		}
	}

	public static String
	readInputStreamAsString(
		InputStream is,
		int		size_limit,
		String charSet)
	
		throws IOException
	{
		StringBuffer result = new StringBuffer(1024);

		byte[] buffer = new byte[1024];

		while (true) {

			int len = is.read(buffer);

			if (len <= 0) {

				break;
			}

			result.append(new String(buffer, 0, len, charSet));

			if (size_limit >= 0 && result.length() > size_limit) {

				result.setLength(size_limit);

				break;
			}
		}

		return (result.toString());
	}
	
	private Map populatePluginMapping() {
		Map plugin_map = new TreeMap();
		plugin_map.put("", AZUREUS_BUNDLE);
		
		PluginInterface[] plugins = pluginInterface.getPluginManager().getPluginInterfaces();
		for (int i=0; i<plugins.length; i++) {
			Properties p = plugins[i].getPluginProperties();
			if (p.containsKey("plugin.langfile")) {
				plugin_map.put(plugins[i].getPluginID(), p.get("plugin.langfile"));
			}
		}
		return plugin_map;
	}
	
	private URL getResource(String resource_loc) {
	  ClassLoader cl = pluginInterface.getClass().getClassLoader();
	  URL resource_url = cl.getResource(resource_loc);
	  if (resource_url == null) {
	  	  cl = pluginInterface.getPluginClassLoader();
	   	  resource_url = cl.getResource(resource_loc);
	  }
	  if (resource_url == null) {
	  	  cl = getClass().getClassLoader();
	   	  resource_url = cl.getResource(resource_loc);
	  }
	  if (resource_url == null) {
		  PluginInterface[] plugins = pluginInterface.getPluginManager().getPluginInterfaces();
		  for (int i=0; i<plugins.length; i++) {
			  cl = plugins[i].getPluginClassLoader();
			  resource_url = cl.getResource(resource_loc);
			  if (resource_url != null) {break;}
		  }
	  }
	  if (resource_url == null) {
		  throw new RuntimeException("unable to get resource - " + resource_loc);
	  }
	  return resource_url;
	}

} 