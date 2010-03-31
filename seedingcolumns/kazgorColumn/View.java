/*
 * Created on Mar 12, 2005
 * Created by omschaub for kazgor
 * 
 */


package kazgorColumn;


import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.LocaleListener;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;


public class View implements UISWTViewEventListener {

	// Variable names 
	private static final String COLOR0_25_VARIABLE_NAME = "color0_25";
	private static final String COLOR25_50_VARIABLE_NAME = "color25_50";
	private static final String COLOR50_75_VARIABLE_NAME = "color50_75";
	private static final String COLOR75_100_VARIABLE_NAME = "color75_100";

	
	// Localized String IDs & strings & stuff
	private char decimalSeparator;
	private NumberFormat numberFormatter = null;
	private static final String PLUGIN_GUI = ExtraColumns.PLUGIN_ID + ".gui";
	private static final String COLOR_CHOOSER1_ID = PLUGIN_GUI + ".colorchooser1";
	private static final String COLOR_CHOOSER2_ID = PLUGIN_GUI + ".colorchooser2";
	private static final String COLOR_CHOOSER3_ID = PLUGIN_GUI + ".colorchooser3";
	private static final String COLOR_CHOOSER4_ID = PLUGIN_GUI + ".colorchooser4";
	private static final String RATIO_GROUP_TITLE = PLUGIN_GUI + ".ratiogrouptitle";
	private static final String RATIO_SELECT_RADIO1 = PLUGIN_GUI + ".ratioselectradio1";
	private static final String RATIO_SELECT_RADIO2 = PLUGIN_GUI + ".ratioselectradio2";
	private static final String RATIO_SELECT_RADIO3 = PLUGIN_GUI + ".ratioselectradio3";
	private static final String RATIO_SELECT_RADIO4 = PLUGIN_GUI + ".ratioselectradio4";
	private static final String RATIO_OF_LABEL = PLUGIN_GUI + ".ratiooflabel";
	private String colorChooser1Text,colorChooser2Text,colorChooser3Text,colorChooser4Text;
	private String ratioGroupTitle;
	private String ratioSelectRadio1Text,ratioSelectRadio2Text,ratioSelectRadio3Text,ratioSelectRadio4Text;
	private String ratioOfLabel;

	// field for ratio values
	private float firstPriorityRatio;
	private float ignoreRulesRatio;
	private float manualRatio = ExtraColumns.TARGET_RATIO_DEFAULT;
	private float manualRatioListTextValue = Float.NaN;
	
	// Inner class to handle the manual ratio list
	private ManualRatioList manualRatioList = null;
	
	private class ManualRatioList {

		// representations of the manual ratio list in different formats
		private float[] floats;
	    private TreeSet sortedSet;
	    private String[] localizedStrings;
	    

		ManualRatioList() {
	        floats = getManualRatioListStringParameter();			// get the plugin config parameter
	        sortedSet = convertFloatsToTreeSet(floats); 			// convert to TreeSet (sorting it and eliminating duplicates)
	        floats = convertTreeSetToFloats(sortedSet); 			// from now on use the sorted and duplicate-eliminated list
	        localizedStrings = convertFloatsToStrings(floats, true);// as well as the localized one
	    }

		
		/**
         * Gets the ManualRatioListParameter from the plugin config into a float array
         * 
         * @return the ManualRatioList array
         */
        synchronized private float[] getManualRatioListStringParameter()
        {
        	if(pluginConfig.hasPluginParameter(ExtraColumns.MANUAL_RATIO_LIST_STRING_PARAMETER_KEY)) {
            	String[] manualRatioListStrings = pluginConfig.getPluginStringParameter(ExtraColumns.MANUAL_RATIO_LIST_STRING_PARAMETER_KEY).split(ExtraColumns.MANUAL_RATIO_LIST_STRING_DELIMITER);
            	float[] rManualRatioList = convertStringsToFloats(manualRatioListStrings,false);
            	return rManualRatioList;
        	}
        	else {
        		pluginConfig.setPluginParameter(ExtraColumns.MANUAL_RATIO_LIST_STRING_PARAMETER_KEY, ExtraColumns.TARGET_RATIO_DEFAULT_STRING + ExtraColumns.MANUAL_RATIO_LIST_STRING_DELIMITER);
        		return ExtraColumns.TARGET_RATIO_LIST_DEFAULT;	
        	}
        }

		/**
         * Sets the ManualRatioListParameter of the plugin config
         */
        synchronized private void setManualRatioListStringParameter()
        {
        	String[] manualRatioListStrings = convertFloatsToStrings(floats,false);
        	String manualRatioListString = new String();
        	for (int i = 0; i < manualRatioListStrings.length; i++) {
        		manualRatioListString = manualRatioListString + manualRatioListStrings[i] + ExtraColumns.MANUAL_RATIO_LIST_STRING_DELIMITER;
        	}
        	pluginConfig.setPluginParameter(ExtraColumns.MANUAL_RATIO_LIST_STRING_PARAMETER_KEY, manualRatioListString);
        }

        
        /**
         * Localize the localized content
         */
        synchronized private void localize()
        {
        	localizedStrings = convertFloatsToStrings(floats,true);
        }

        synchronized String[] getLocalizedStrings() {
        	return localizedStrings;
        }


        /**
         * Convert manualRatioList from float[] to string[]
         * @param pFloats The float array to be converted
         * @param localize Whether or not the String array should be localized
         * @return the ManualRatioList array as string[]
         */
        synchronized private String[] convertFloatsToStrings(float[] pFloats, boolean localize)
        {
        	if(pFloats.length == 0) {
        		pFloats = new float[1];
        		pFloats[0] = ExtraColumns.ERROR_TARGET_RATIO;
        	}
        	String[] rStrings = new String[pFloats.length];
        	for(int i=0; i<pFloats.length; i++) {
        		if(localize) rStrings[i]=numberFormatter.format((double)pFloats[i]);
        		else rStrings[i]=String.valueOf(pFloats[i]);
        	}
        	return rStrings;
        }

		/**
         * Convert manualRatioList from string[] to float[]
         * @param pStrings The String array to be converted
         * @param localize Whether or not the String array to be converted is localized
         * @return the ManualRatioList array as float[]
         */
        synchronized private float[] convertStringsToFloats(String[] pStrings, boolean localize)
        {
        	if(pStrings.length == 0) {
        		pStrings = new String[1];
        		if(localize) pStrings[0] = numberFormatter.format((double)ExtraColumns.ERROR_TARGET_RATIO);
        		else pStrings[0] = String.valueOf(ExtraColumns.ERROR_TARGET_RATIO);
        	}
        	float[] rFloats = new float[pStrings.length];
           	try{
           	   	for(int i=0; i<pStrings.length; i++) {
           	   		if(localize) rFloats[i]=numberFormatter.parse(pStrings[i]).floatValue();
           	   		else rFloats[i]=Float.valueOf(pStrings[i]).floatValue();	
           	   	}
           	}catch(Exception e){
           		e.printStackTrace();
           		rFloats = new float[1];
           		rFloats[0] = ExtraColumns.ERROR_TARGET_RATIO;
           	}
        	return rFloats;
        }


		/**
         * Convert manualRatioList from float[] to a TreeSet collection (this also sorts it and eliminates double entries)
         * 
         * @return the ManualRatioList array as a TreeSet
         */
        synchronized private TreeSet convertFloatsToTreeSet(float[] pFloats)
        {
        	TreeSet rTreeSet = new TreeSet();
        	if(pFloats.length == 0) {
        		rTreeSet.add(new Float(ExtraColumns.ERROR_TARGET_RATIO));
        		return rTreeSet;
        	}
        	for(int i=0; i<pFloats.length; i++) rTreeSet.add(new Float(pFloats[i]));
        	return rTreeSet;
        }

		/**
         * Convert manualRatioList from TreeSet to float[]
         * 
         * @return the ManualRatioList array as float[]
         */
        synchronized private float[] convertTreeSetToFloats(TreeSet pTreeSet)
        {
        	float[] rManualRatioList;
        	if(pTreeSet.isEmpty()){
        		rManualRatioList = new float[1];
        		rManualRatioList[0] = ExtraColumns.ERROR_TARGET_RATIO;
        		return rManualRatioList;
        	}
        	rManualRatioList = new float[pTreeSet.size()];
        	Iterator iter = pTreeSet.iterator();
        	int i = 0;
        	while(iter.hasNext()) rManualRatioList[i++]=((Float)iter.next()).floatValue();
        	return rManualRatioList;
        }


	    /**
	     * Add a Float to the SortedSet
	     * @param toAdd the Float to add
	     * @return If the add was successfull (false means Float was already present in Set)
	     */
	    synchronized boolean addToSortedSet(Float toAdd) {
	    	boolean addSuccessfull = sortedSet.add(toAdd);
	    	if(addSuccessfull) {
           		// a number has successfully been added to the list, adjust the float[] and String[] representations
           		floats = convertTreeSetToFloats(sortedSet);
           		localizedStrings = convertFloatsToStrings(floats,true);
           		// and set the plugin config parameter
           		setManualRatioListStringParameter();

	    	}
	    	return addSuccessfull;
	    }
	    
	    /**
	     * Update all the fields to change the ManualRatioList
	     * @param changedLocalizedStrings Data to which the ManualRatioList should be changed to
	     */
	    synchronized void change(String[] changedLocalizedStrings) {
    		localizedStrings = changedLocalizedStrings;
    		floats = convertStringsToFloats(changedLocalizedStrings,true);
    		sortedSet = convertFloatsToTreeSet(floats);
    		// and set the plugin config parameter
    		setManualRatioListStringParameter();
	    }
    }


	//The current Display
    private static Display display;
    
    //This view main GUI object
    private Composite composite = null;
    
    //The Plugin interface
    private PluginInterface pluginInterface;
    private LocaleUtilities localizer = null;
    private LocaleListener localeListener = null;

    // GUI elements
    private Group colorGroup;
    private Button colorChooser1, colorChooser2, colorChooser3, colorChooser4; 
    private Group  settingsGroup;
    private Button firstPriorityRadio, ignoreRulesRadio, manualSetRadio;
    private Label manualLabel1, manualLabel2;
    private Text manualRatioTextField;
    private Label manualRatioInvisibleLabel;
    private Button manualListRadio;
    private Label manualRatioListTextFieldLabel1, manualRatioListTextFieldLabel2;
    private Text manualRatioListTextField;
    private Button manualRatioListAdd;
    private Label manualRatioListListInvisibleLabel1, manualRatioListListInvisibleLabel2; 
    private List manualRatioListList;
    private Button manualRatioListDel;

    
    //The Static Plugin interface, will describe in more details later
    private static PluginInterface pi_static;
    
    
    //We will be using a plugin config a LOT, so instead of calling one each time,
    //lets set up a global one
    private PluginConfig pluginConfig;
    
    private boolean created = false;

    
    public View(PluginInterface pluginInterface) {
    	this.pluginInterface = pluginInterface;
    }

    
  	// @see org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener#eventOccurred(org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent)
    public boolean eventOccurred(UISWTViewEvent event) {
    	switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				if (created) {
					return false;
				}
				created = true;
				return true;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData());
				break;
				
			case UISWTViewEvent.TYPE_DESTROY:
				created = false;
				if (composite != null && !composite.isDisposed()) {
					composite.dispose();
				}
				composite = null;
				if(localeListener != null) {
					localizer.removeListener(localeListener);
					localeListener = null;
				}
				break;
				
			default:
				break;
		}
  		
  		return true;
    }


    /**
     * Here is the GUI initialization
     */
    public void initialize(Composite parent) {

        // We store the Display variable as we'll need it for async GUI Changes
        display = parent.getDisplay();

        //For static calls -- we will use a static pluginInterface
        //make it private and make a call for it below the end of the initialize
        pi_static = pluginInterface;


        //we need to initialize the global pluginConfig
        pluginConfig = pluginInterface.getPluginconfig();


		// localize used strings and add a listener for Locale-changes
		localizer = pluginInterface.getUtilities().getLocaleUtilities();
		localize(localizer.getCurrentLocale());
		localeListener = new LocaleListener() {
			public void localeChanged(Locale l) {
				localize(l);
			}
		};
		localizer.addListener(localeListener);


        // get ratios
	    firstPriorityRatio = (float)pluginInterface.getPluginconfig().getUnsafeIntParameter("StartStopManager_iFirstPriority_ShareRatio",1000) / 1000;
        ignoreRulesRatio = pluginInterface.getPluginconfig().getUnsafeFloatParameter("Stop Ratio"); 
		manualRatio = getManualRatioParameter();
        manualRatioList = new ManualRatioList();


        //main composite layout -- we need to first set up a composite 
        //( a composite is like a chalk board to draw everything on)
        composite = new Composite(parent,SWT.NULL);

        //For this composite we will be using a grid layout -- much better than a form layout :)
        GridLayout layout = new GridLayout();

        //Specify the number of columns on the composite
        layout.numColumns = 2;

        //Tell the composite to use the layout
        composite.setLayout(layout);
        
        //And now some GridData to tell it what to do on the parent composite
        GridData gridData = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(gridData);
        

        //make a new Group to put the color composites in
        colorGroup = new Group(composite,SWT.NULL);
        colorGroup.setLayout(new GridLayout(2,false));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 2;
        colorGroup.setLayoutData(gridData);

        // create the color chooser buttons + listeners
        colorChooser4 = createColorChooserGroup(colorChooser4Text, COLOR0_25_VARIABLE_NAME, 
								        		ExtraColumns.COLOR0_25_RED_PARAMETER_KEY, ExtraColumns.COLOR0_25_GREEN_PARAMETER_KEY, ExtraColumns.COLOR0_25_BLUE_PARAMETER_KEY, 
								        		new RGB(ExtraColumns.COLOR_0_25_DEFAULT[0],ExtraColumns.COLOR_0_25_DEFAULT[1],ExtraColumns.COLOR_0_25_DEFAULT[2]));
        colorChooser3 = createColorChooserGroup(colorChooser3Text, COLOR25_50_VARIABLE_NAME, 
								        		ExtraColumns.COLOR25_50_RED_PARAMETER_KEY, ExtraColumns.COLOR25_50_GREEN_PARAMETER_KEY, ExtraColumns.COLOR25_50_BLUE_PARAMETER_KEY, 
								        		new RGB(ExtraColumns.COLOR_25_50_DEFAULT[0],ExtraColumns.COLOR_25_50_DEFAULT[1],ExtraColumns.COLOR_25_50_DEFAULT[2]));
        colorChooser2 = createColorChooserGroup(colorChooser2Text, COLOR50_75_VARIABLE_NAME, 
								        		ExtraColumns.COLOR50_75_RED_PARAMETER_KEY, ExtraColumns.COLOR50_75_GREEN_PARAMETER_KEY, ExtraColumns.COLOR50_75_BLUE_PARAMETER_KEY, 
								        		new RGB(ExtraColumns.COLOR_50_75_DEFAULT[0],ExtraColumns.COLOR_50_75_DEFAULT[1],ExtraColumns.COLOR_50_75_DEFAULT[2]));
        colorChooser1 = createColorChooserGroup(colorChooser1Text, COLOR75_100_VARIABLE_NAME, 
        										ExtraColumns.COLOR75_100_RED_PARAMETER_KEY, ExtraColumns.COLOR75_100_GREEN_PARAMETER_KEY, ExtraColumns.COLOR75_100_BLUE_PARAMETER_KEY, 
        										new RGB(ExtraColumns.COLOR_75_100_DEFAULT[0],ExtraColumns.COLOR_75_100_DEFAULT[1],ExtraColumns.COLOR_75_100_DEFAULT[2]));

        
        // This section adds the ratio widgets groups and its listeners
        settingsGroup = new Group (composite,SWT.NULL);
        settingsGroup.setText(ratioGroupTitle);
        settingsGroup.setLayout(new GridLayout(4,false));

        
        firstPriorityRadio = new Button(settingsGroup,SWT.RADIO);
        firstPriorityRadio.setText(ratioSelectRadio1Text + " (" + numberFormatter.format((double)firstPriorityRatio) + ":1)");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 4;
        firstPriorityRadio.setLayoutData(gridData);
        firstPriorityRadio.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                pluginConfig.setPluginParameter(ExtraColumns.RATIO_TYPE_PARAMETER_KEY,1);
            }
        });

        
        ignoreRulesRadio = new Button(settingsGroup,SWT.RADIO);
        ignoreRulesRadio.setText(ratioSelectRadio2Text + " (" + numberFormatter.format((double)ignoreRulesRatio) + ":1)");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 4;
        ignoreRulesRadio.setLayoutData(gridData);
        ignoreRulesRadio.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                pluginConfig.setPluginParameter(ExtraColumns.RATIO_TYPE_PARAMETER_KEY,2);
                
            }
        });
        
        
        manualSetRadio = new Button(settingsGroup,SWT.RADIO);
        manualSetRadio.setText(ratioSelectRadio3Text);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 4;
        manualSetRadio.setLayoutData(gridData);
        manualSetRadio.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                if(manualSetRadio.getSelection()){
                    enableManualRatio(true);
                    pluginConfig.setPluginParameter(ExtraColumns.RATIO_TYPE_PARAMETER_KEY,3);
                }else{
                    enableManualRatio(false);
                }
            }
        });

        
        // The manualRatio GUI elements
        manualLabel1 = new Label(settingsGroup,SWT.NULL);
        manualLabel1.setText(ratioOfLabel);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gridData.horizontalIndent = 40;
        manualLabel1.setLayoutData(gridData);
        
        manualRatioTextField = new Text(settingsGroup,SWT.BORDER | SWT.SINGLE | SWT.TRAIL);
        manualRatioTextField.setText(numberFormatter.format((double)manualRatio));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.widthHint = 30;
        manualRatioTextField.setLayoutData(gridData);
           
        //make sure people only put in numbers!
        manualRatioTextField.addListener (SWT.Verify, new Listener () {
            public void handleEvent (Event e) {
                String string = e.text;
                char [] chars = new char [string.length ()];
                string.getChars (0, chars.length, chars, 0);
                for (int i=0; i<chars.length; i++) {
                    if (!Character.isDigit(chars[i]) && chars[i] != decimalSeparator) {
                        e.doit = false;
                        return;
                    }
                }
            }
        });
        
        //listen for changes in the text
        manualRatioTextField.addListener(SWT.Modify,new Listener() {
            public void handleEvent(Event arg0) {
            	try {
            		String manualRatioText = manualRatioTextField.getText();
            		if( manualRatioText.length() == 0) {
            			manualRatio = Float.NaN;
            		}
            		else {
                		manualRatio = numberFormatter.parse(manualRatioText).floatValue();
                		setManualRatioParameter(manualRatio);
            		}
            	}
            	catch (Exception e) {
            		manualRatio = Float.NaN;
            	}
            }
        });

        manualLabel2 = new Label(settingsGroup,SWT.NULL);
        manualLabel2.setText(":1");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        manualLabel2.setLayoutData(gridData);
        
        // invisible Label to position next radio button one row below on its own
        manualRatioInvisibleLabel = new Label(settingsGroup, SWT.NULL);
        manualRatioInvisibleLabel.setVisible(false);

        
        manualListRadio = new Button(settingsGroup,SWT.RADIO);
        manualListRadio.setText(ratioSelectRadio4Text);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 4;
        manualListRadio.setLayoutData(gridData);
        manualListRadio.addListener(SWT.Selection, new Listener() {
        	public void handleEvent(Event e) {
        		if(manualListRadio.getSelection()) {
        			enableManualRatioList(true);
        			pluginConfig.setPluginParameter(ExtraColumns.RATIO_TYPE_PARAMETER_KEY,4);
        			manualRatioList.setManualRatioListStringParameter();
        		}else{
        			enableManualRatioList(false);
        		}
        	}
        });


        // The ManualRatioList GUI elements
        manualRatioListTextFieldLabel1 = new Label(settingsGroup, SWT.NULL);
        manualRatioListTextFieldLabel1.setText(ratioOfLabel);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gridData.horizontalIndent = 40;
        manualRatioListTextFieldLabel1.setLayoutData(gridData);
        
        manualRatioListTextField = new Text(settingsGroup,SWT.BORDER | SWT.SINGLE | SWT.TRAIL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.verticalAlignment = SWT.CENTER;
        gridData.widthHint = 30;
        manualRatioListTextField.setLayoutData(gridData);
           
        //make sure people only put in numbers!
        manualRatioListTextField.addListener (SWT.Verify, new Listener () {
            public void handleEvent (Event e) {
                String string = e.text;
                char [] chars = new char [string.length ()];
                string.getChars (0, chars.length, chars, 0);
                for (int i=0; i<chars.length; i++) {
                    if (!Character.isDigit(chars[i]) && chars[i] != decimalSeparator) {
                        e.doit = false;
                        if(chars[i] == '+') addRatioToList();
                        return;
                    }
                }
            }
        });

        //listen for changes in the text
        manualRatioListTextField.addListener(SWT.Modify,new Listener() {
            public void handleEvent(Event arg0) {
            	try {
            		String manualRatioListTextContent = manualRatioListTextField.getText();
            		if(manualRatioListTextContent.length() == 0) {
            			manualRatioListTextValue = Float.NaN;
            		}
            		else {
            			manualRatioListTextValue = numberFormatter.parse(manualRatioListTextContent).floatValue();
            		}
            	}
            	catch (Exception e) {
            		manualRatioListTextValue = Float.NaN;
            	}
            }
        });

        manualRatioListTextFieldLabel2 = new Label(settingsGroup, SWT.NULL);
        manualRatioListTextFieldLabel2.setText(":1");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        manualRatioListTextFieldLabel2.setLayoutData(gridData);

        manualRatioListAdd = new Button(settingsGroup,SWT.PUSH);
        manualRatioListAdd.setText("&+");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        manualRatioListAdd.setLayoutData(gridData);

        manualRatioListAdd.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
            	addRatioToList();
            }
        });

        // invisible Label to align List below TextField
        manualRatioListListInvisibleLabel1 = new Label(settingsGroup, SWT.NULL);
        manualRatioListListInvisibleLabel1.setVisible(false);

        manualRatioListList = new List(settingsGroup,SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        manualRatioListList.setItems(manualRatioList.getLocalizedStrings());
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        // recommended size 30 pixel wide, number of items + 1 rows high
        gridData.widthHint = 30;
        gridData.heightHint = (manualRatioListList.getItemCount()+1) * manualRatioListList.getItemHeight();
        gridData.verticalAlignment = SWT.FILL;
        manualRatioListList.setLayoutData(gridData);

        manualRatioListList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
            	String [] selected = manualRatioListList.getSelection();
            	manualRatioListTextField.setText(selected[0]);
            	try {
        			manualRatioListTextValue = numberFormatter.parse(selected[0]).floatValue();
        		}
            	catch (Exception exception) {
					manualRatioListTextValue = Float.NaN;
				}
            }
        });
        
        manualRatioListList.addKeyListener(new KeyAdapter() {
        	public void keyPressed(KeyEvent keyEvent) {
        		char keyPressed = keyEvent.character;
        		if(keyPressed == '+') addRatioToList();
        		else if(keyPressed == '-') deleteRatioFromList();
        	}
        	
        });
        
        // invisible Label to align del Button below add button
        manualRatioListListInvisibleLabel2 = new Label(settingsGroup, SWT.NULL);
        manualRatioListListInvisibleLabel2.setVisible(false);

        manualRatioListDel = new Button(settingsGroup,SWT.PUSH);
        manualRatioListDel.setText("&-");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.verticalAlignment = SWT.CENTER;
        manualRatioListDel.setLayoutData(gridData);
        manualRatioListDel.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	deleteRatioFromList();
            }
        });


        int ratioType = pluginConfig.getPluginIntParameter(ExtraColumns.RATIO_TYPE_PARAMETER_KEY, ExtraColumns.RATIO_TYPE_DEFAULT);
        switch(ratioType) {
	        case 1:
	            firstPriorityRadio.setSelection(true);
	            break;
	        case 2:
	            ignoreRulesRadio.setSelection(true);
	            break;
	        case 3:
	            manualSetRadio.setSelection(true);
	            break;
	        case 4:
	        	manualListRadio.setSelection(true);
	        	break;
	        default:
	        	ignoreRulesRadio.setSelection(true);
        }
        
        
        if(!manualSetRadio.getSelection()){
        	enableManualRatio(false);
        }

        if(!manualListRadio.getSelection()){
        	enableManualRatioList(false);
        }
    }


    /**
     * Call for returning the display -- used in the DrawImage to get the working display
     * 
     */
    public static Display getDisplay() {
        return display;
    }


	/**
     * Call for pluginInterface from view
     */
    public static PluginInterface getPluginInterface(){
        return pi_static;
    }


	/**
     * Get the manual ratio parameter from the plugin config.
     * If the parameter does not exist it gets created.
     * @return The manual ratio
     */
    private float getManualRatioParameter()
    {
    	if(pluginConfig.hasPluginParameter(ExtraColumns.MANUAL_RATIO_PARAMETER_KEY)) {
        	String manualRatioString = pluginConfig.getPluginStringParameter(ExtraColumns.MANUAL_RATIO_PARAMETER_KEY,ExtraColumns.TARGET_RATIO_DEFAULT_STRING);
            try{
            	float rManualRatio = Float.valueOf(manualRatioString).floatValue();
            	return rManualRatio;
        	}
            catch (Exception e) {
           		e.printStackTrace();
           		return ExtraColumns.ERROR_TARGET_RATIO;			}
    	}
        else {
    		pluginConfig.setPluginParameter(ExtraColumns.MANUAL_RATIO_PARAMETER_KEY, ExtraColumns.TARGET_RATIO_DEFAULT_STRING);
    		return ExtraColumns.TARGET_RATIO_DEFAULT;	
    	}
    }

	/**
     * Set the manual ratio parameter of the plugin config
     * 
     * @param pManualRatio The manual ratio to set
     */
    private void setManualRatioParameter(float pManualRatio)
    {
    	pluginConfig.setPluginParameter(ExtraColumns.MANUAL_RATIO_PARAMETER_KEY, String.valueOf(pManualRatio));
    }


	/**
     * Localizes the strings
     * TODO: Method should not be needed to be changed when a localized string is added/removed
     */
    private void localize(Locale l) {
    	// localize strings from resource bundle
    	colorChooser1Text = localizer.getLocalisedMessageText(COLOR_CHOOSER1_ID);
    	colorChooser2Text = localizer.getLocalisedMessageText(COLOR_CHOOSER2_ID);
    	colorChooser3Text = localizer.getLocalisedMessageText(COLOR_CHOOSER3_ID);
    	colorChooser4Text = localizer.getLocalisedMessageText(COLOR_CHOOSER4_ID);
    	ratioGroupTitle = localizer.getLocalisedMessageText(RATIO_GROUP_TITLE);
    	ratioSelectRadio1Text = localizer.getLocalisedMessageText(RATIO_SELECT_RADIO1);
    	ratioSelectRadio2Text = localizer.getLocalisedMessageText(RATIO_SELECT_RADIO2);
    	ratioSelectRadio3Text = localizer.getLocalisedMessageText(RATIO_SELECT_RADIO3);
    	ratioSelectRadio4Text = localizer.getLocalisedMessageText(RATIO_SELECT_RADIO4);
    	ratioOfLabel = localizer.getLocalisedMessageText(RATIO_OF_LABEL);
    
    	// number formatters
    	decimalSeparator = new DecimalFormatSymbols(l).getDecimalSeparator();
    	numberFormatter = NumberFormat.getNumberInstance(l);
    
    	// update localized strings in the manual ratio list
    	if( manualRatioList != null) manualRatioList.localize();
    
    	// update the GUI
    	if(composite != null){
        	if(display == null || display.isDisposed()) return;
        	
        	display.asyncExec( new Runnable() {
        		public void run() {
        			// set new texts on widgets
        			colorChooser1.setText(colorChooser1Text);
        			colorChooser2.setText(colorChooser2Text);
        			colorChooser3.setText(colorChooser3Text);
        			colorChooser4.setText(colorChooser4Text);
        			settingsGroup.setText(ratioGroupTitle);
        			firstPriorityRadio.setText(ratioSelectRadio1Text + " (" + numberFormatter.format((double)firstPriorityRatio) + ":1)");
        			ignoreRulesRadio.setText(ratioSelectRadio2Text + " (" + numberFormatter.format((double)ignoreRulesRatio) + ":1)");
        			manualSetRadio.setText(ratioSelectRadio3Text);
        			manualListRadio.setText(ratioSelectRadio4Text);
        			manualLabel1.setText(ratioOfLabel);
        			manualRatioListTextFieldLabel1.setText(ratioOfLabel);
    
        			// update text fields & list with localized entries
    				if(!Float.isNaN(manualRatio)) manualRatioTextField.setText(numberFormatter.format((double)manualRatio));
    				if(!Float.isNaN(manualRatioListTextValue)) manualRatioListTextField.setText(numberFormatter.format((double)manualRatioListTextValue));
        			manualRatioListList.setItems(manualRatioList.getLocalizedStrings());
    
        			// and resize/redraw
        			composite.pack(true);
        			composite.redraw();
        		}
        	});
    	}
    }


	/**
     * Create a color chooser Button + Dialog and listeners
     * @param label Button and dialogs label
     * @param variableName Color variable name
     * @param redParamKey Red plugin config parameter key
     * @param greenParamKey Green plugin config parameter key
     * @param blueParamKey Blue plugin config parameter key
     * @param defaultColor Default color to use
     * @return The created button
     */
    private Button createColorChooserGroup(final String label, final String variableName,
    										final String redParamKey, final String greenParamKey, final String blueParamKey,
    										final RGB defaultColor) {
        //Make a button to select the first color 
        Button colorChooser = new Button(colorGroup, SWT.PUSH);
        //Set the text of the button
        colorChooser.setText(label);
        // Fill cell horizontal
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        colorChooser.setLayoutData(gridData);
        
        //To be cute here, we will draw a label and set it as a image Label and draw a rectangle with the current color!  Cute, huh :)
        final Label colorLabel = new Label(colorGroup,SWT.BORDER);
        
        //Set its grid data
        gridData = new GridData(GridData.BEGINNING);
        colorLabel.setLayoutData(gridData);
        colorLabel.setImage(DrawImage.labelColor(variableName,defaultColor.red,defaultColor.green,defaultColor.blue));
        
        //Now we need to add a listener to the button to listen for it being pushed
        //The listeners ALWAYS use this form
        colorChooser.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                //define color dialog -- needs to be drawn on a shell
                ColorDialog colorDialog = new ColorDialog(colorGroup.getShell());
                
                //Set the title for the color dialog
                colorDialog.setText(((Button)e.widget).getText());
                
                //Set the initial color to the user specified color and if that does
                //not work, then a default color is chosen
                //This will be a little lengthy because the Azureus core settings 
                //does not have a int array, just an integer, so we will have to 
                //make 3 each time -- we will default the first color to 200,0,0
                colorDialog.setRGB((new RGB(
                        pluginConfig.getPluginIntParameter(redParamKey, defaultColor.red),
                        pluginConfig.getPluginIntParameter(greenParamKey,defaultColor.green),
                        pluginConfig.getPluginIntParameter(blueParamKey,defaultColor.blue))));
                
                //open the dialog and set the results to a new RGB color
                RGB selectedColor = colorDialog.open();
                
                //if cancel is hit then the selectedColor will be null and we need to break
                //out of the listener
                if(selectedColor==null)
                    return;
                
                
                //set the defaults to the new RGB color so that the columns can pick it up
                pluginConfig.setPluginParameter(redParamKey,selectedColor.red);
                pluginConfig.setPluginParameter(greenParamKey,selectedColor.green);
                pluginConfig.setPluginParameter(blueParamKey,selectedColor.blue);
                
                //now we need to redraw the label
                colorLabel.setImage(DrawImage.labelColor(variableName,defaultColor.red,defaultColor.green,defaultColor.blue));
            }
        });
        return colorChooser;
    }


	/**
     * Try to add a ratio from the TextField to the ManualRatioList and update the GUI List
     */
    private void addRatioToList() {
        // try to get a value from the TextField
        Float manualRatioListTextFieldValue;
        try{
           	manualRatioListTextFieldValue = new Float(numberFormatter.parse(manualRatioListTextField.getText()).floatValue());
        }catch(Exception except){
        	except.printStackTrace();
        	return;
        }
        // check if the value is a number, above zero, and not already present in the list
        if(!manualRatioListTextFieldValue.isNaN() &&
        	manualRatioListTextFieldValue.floatValue() > 0.0f &&
        	manualRatioList.addToSortedSet(manualRatioListTextFieldValue))
        {
        	// update the GUI list
        	manualRatioListList.setItems(manualRatioList.getLocalizedStrings());
        }
    }

	/**
     * Try to delete a ratio from the List & the ManualRatioList
     */
    private void deleteRatioFromList() {
        String[] selection = manualRatioListList.getSelection();
        // check if one entry is selected and the list contains more than 1 entries
        if(selection.length == 1 && manualRatioListList.getItemCount() > 1)
        {
        	// remove the entry from the GUI list
        	manualRatioListList.remove(selection[0]);
        	// update the manualRatioList
        	manualRatioList.change(manualRatioListList.getItems());
        }
    }


	/**
     * Little method to turn on and off the manually set ratio
     */
    public void enableManualRatio(final boolean hide){
        if(display == null || display.isDisposed())
            return;
        
        display.asyncExec( new Runnable() {
            public void run() {
                manualLabel1.setEnabled(hide);
                manualRatioTextField.setEnabled(hide);
                manualLabel2.setEnabled(hide);        
            }
        });
    }

    /**
     *  Turn on and off the manual ratio list elements
     */
    public void enableManualRatioList(final boolean hide){
    	if(display == null || display.isDisposed())
    		return;
    	
    	display.asyncExec( new Runnable() {
    		public void run() {
    			manualRatioListTextField.setEnabled(hide);
    			manualRatioListTextFieldLabel1.setEnabled(hide);
    			manualRatioListTextFieldLabel2.setEnabled(hide);
    			manualRatioListAdd.setEnabled(hide);
    			manualRatioListList.setEnabled(hide);
    			manualRatioListDel.setEnabled(hide);
    		}
    	});
    }

}