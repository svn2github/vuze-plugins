/* MODULE: InputDialog.java */

/* COPYRIGHT
 * $RCSfile: InputDialog.java,v $
 * $Revision: 1.0 $
 * $Date: 2006/02/13 20:51:58 $
 * $Author: rosec $
 *
 * Copyright (c) 2007 Chris Rose
 * All rights reserved.
 * END COPYRIGHT */

package com.aimedia.stopseeding.ui.menu;

import java.text.NumberFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.aimedia.stopseeding.AutoStopPlugin;
import com.aimedia.stopseeding.Logger;
import com.aimedia.stopseeding.ResourceConstants;

/**
 * 
 */
public class InputDialog {

    private Shell     inputDialogShell = null;

    private Composite comboPanel       = null;

    private Composite buttonPanel      = null;

    private Button    cancelButton     = null;

    private Button    okButton         = null;

    private Combo     combo            = null;

    private Composite contentPanel     = null;

    private Label    contentLabel     = null;

    private String    message;

    private String    title;

    private Listener  listener;

    protected boolean result;

    protected String  value            = null;

    private String currentRatio;

    @SuppressWarnings("unused")
    private Display display;

    private NumberFormat nf;

    public InputDialog (Display display, String currentRatio, String title, String message) {
        this.title = title;
        this.message = message;
        this.currentRatio = currentRatio;
        this.display = display;
        
        this.nf = NumberFormat.getInstance ();
        this.nf.setMaximumFractionDigits (3);
        
        createSShell (display);
        inputDialogShell.addDisposeListener (new DisposeListener () {

            public void widgetDisposed (DisposeEvent arg0) {
                // : store the value
                value = combo.getText ();
            }
        });

    }

    public Shell getShell () {
        return inputDialogShell;
    }

    public String getValue () {
        if (!combo.isDisposed ()) {
            return combo.getText ();
        }
        else {
            return value;
        }
    }

    /**
     * This method initializes sShell
     */
    private void createSShell (Display display) {
        GridLayout gridLayout = new GridLayout ();
        gridLayout.numColumns = 1;
        inputDialogShell = new Shell (display.getActiveShell ());
        inputDialogShell.setText (title);
        createMessagePanel ();
        createComboPanel ();
        inputDialogShell.setLayout (gridLayout);
        createButtonPanel ();
        // sShell.setSize (new Point (300, 200));
    }

    /**
     * This method initializes composite
     * 
     */
    private void createComboPanel () {
        GridData gridData = new GridData ();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = false;
        comboPanel = new Composite (inputDialogShell, SWT.NONE);
        comboPanel.setLayout (new FillLayout ());
        createCombo ();
        comboPanel.setLayoutData (gridData);
    }

    private Listener getListener () {
        if (listener == null) {
            listener = new Listener () {
                public void handleEvent (Event event) {
                    if (event.widget == cancelButton) {
                        result =  false;
                        inputDialogShell.close ();
                        return;
                    }
                    
                    String text = combo.getText ();
                    if (text == null) {
                        result = false;
                    }
                    else if (text.equalsIgnoreCase ("unlimited")) {
                        result = event.widget == okButton;
                    }
                    else {
                        try {
                            Float ratio = Float.parseFloat (text);
                            if (ratio >= 1.0) {
                                result = true;
                            }
                            else {
                                Logger.alert (ResourceConstants.RATIO_TOO_SMALL, null, nf.format (ratio));
                            }
                        }
                        catch (NumberFormatException e) {
                            result = false;
                            Logger.alert ("The value you entered is not a valid number", null);
                        }
                    }
                    inputDialogShell.close ();
                }
            };
        }
        return listener;
    }

    /**
     * This method initializes composite1
     * 
     */
    private void createButtonPanel () {
        GridData gridData5 = new GridData ();
        gridData5.horizontalAlignment = GridData.FILL;
        gridData5.verticalAlignment = GridData.CENTER;
        GridData gridData4 = new GridData ();
        gridData4.horizontalAlignment = GridData.FILL;
        gridData4.verticalAlignment = GridData.CENTER;
        GridData gridData1 = new GridData ();
        gridData1.horizontalAlignment = GridData.END;
        gridData1.verticalAlignment = GridData.CENTER;
        GridLayout gridLayout1 = new GridLayout ();
        gridLayout1.numColumns = 2;
        gridLayout1.makeColumnsEqualWidth = true;
        buttonPanel = new Composite (inputDialogShell, SWT.NONE);
        buttonPanel.setLayout (gridLayout1);
        buttonPanel.setLayoutData (gridData1);
        cancelButton = new Button (buttonPanel, SWT.NONE);
        cancelButton.setText ("Cancel");
        cancelButton.setLayoutData (gridData5);
        cancelButton.addListener (SWT.Selection, getListener ());
        okButton = new Button (buttonPanel, SWT.NONE);
        okButton.setText ("Ok");
        okButton.setLayoutData (gridData4);
        okButton.addListener (SWT.Selection, getListener ());
    }

    /**
     * This method initializes combo
     * 
     */
    private void createCombo () {
        combo = new Combo (comboPanel, SWT.NONE);
        combo.setItems (AutoStopPlugin.plugin ().getConfiguration ().getDefaultRatioOptions ());
        combo.setText (currentRatio);
    }

    /**
     * This method initializes composite2
     * 
     */
    private void createMessagePanel () {
        GridData gridData3 = new GridData ();
        gridData3.grabExcessVerticalSpace = true;
        gridData3.horizontalAlignment = GridData.FILL;
        gridData3.verticalAlignment = GridData.FILL;
        gridData3.grabExcessHorizontalSpace = true;
        gridData3.widthHint = 300;
        GridData gridData2 = new GridData ();
        gridData2.horizontalAlignment = GridData.FILL;
        gridData2.grabExcessVerticalSpace = true;
        gridData2.grabExcessHorizontalSpace = true;
        gridData2.verticalAlignment = GridData.FILL;
        contentPanel = new Composite (inputDialogShell, SWT.NONE);
        contentPanel.setLayout (new GridLayout ());
        contentPanel.setLayoutData (gridData2);
        contentLabel = new Label (contentPanel, SWT.WRAP | SWT.BORDER);
        contentLabel.setText (message);
        contentLabel.setLayoutData (gridData3);
    }

    /**
     * @return
     */
    public boolean getResult () {
        return result;
    }

}
