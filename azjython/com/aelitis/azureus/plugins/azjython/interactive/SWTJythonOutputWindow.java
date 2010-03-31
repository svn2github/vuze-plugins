/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import com.aelitis.azureus.plugins.azjython.JythonPluginCore;
import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;


/**
 * @author Allan Crooks
 *
 */
public class SWTJythonOutputWindow {
	
	protected JythonPluginCore core;
	protected UISWTInstance swt_ui;
	private String title;
	protected boolean enabled;
	private Display display;
	
	// Things we control.
	private StyledText messages;
	private Label header;
	
	// Things we dispose of.
	protected Font main_font;
	private Font header_font;
	private Composite composite; // The main thing.
	protected Composite window_composite; // For subclasses.
	
	// Maps console output descriptors to colours.
	private HashMap descriptor_color_map;
	
	// Maps console output descriptors to font styles.
	private HashMap descriptor_style_map;
	
	// Indicates which output descriptors are enabled.
	private HashMap descriptor_enabled_map;
	
	public SWTJythonOutputWindow(JythonPluginCore core, UISWTInstance swt_ui, String title) {
		this.core = core;
		this.title = title;
		this.swt_ui = swt_ui;
		this.enabled = true;
	}
	
	private void setupDescriptorMap(Display display) {
		descriptor_color_map.put(ConsoleOutputDescriptor.CONSOLE_INFO, new Color(display, 0, 0, 208));
		descriptor_color_map.put(ConsoleOutputDescriptor.PROMPT, new Color(display, 0, 0, 0));
		descriptor_color_map.put(ConsoleOutputDescriptor.STDIN, new Color(display, 0, 0, 0));
		descriptor_color_map.put(ConsoleOutputDescriptor.STDOUT, new Color(display, 0, 0, 0));
		descriptor_color_map.put(ConsoleOutputDescriptor.STDERR, new Color(display, 208, 0, 0));
		descriptor_color_map.put(ConsoleOutputDescriptor.STDOUT_UNKNOWN, new Color(display, 0, 0, 0));
		descriptor_color_map.put(ConsoleOutputDescriptor.STDERR_UNKNOWN, new Color(display, 208, 0, 0));
		
		Integer no_style = new Integer(0);
		Integer bold_style = new Integer(SWT.BOLD);
		Integer italic_style = new Integer(SWT.ITALIC);
		
		descriptor_style_map.put(ConsoleOutputDescriptor.CONSOLE_INFO, bold_style);
		descriptor_style_map.put(ConsoleOutputDescriptor.PROMPT, bold_style);
		descriptor_style_map.put(ConsoleOutputDescriptor.STDIN, no_style);
		descriptor_style_map.put(ConsoleOutputDescriptor.STDOUT, no_style);
		descriptor_style_map.put(ConsoleOutputDescriptor.STDERR, bold_style);
		descriptor_style_map.put(ConsoleOutputDescriptor.STDOUT_UNKNOWN, italic_style);
		descriptor_style_map.put(ConsoleOutputDescriptor.STDERR_UNKNOWN, italic_style);
		
		descriptor_enabled_map.put(ConsoleOutputDescriptor.CONSOLE_INFO, Boolean.TRUE);
		descriptor_enabled_map.put(ConsoleOutputDescriptor.PROMPT, Boolean.valueOf(shouldDisplayPrompt()));
		descriptor_enabled_map.put(ConsoleOutputDescriptor.STDERR, Boolean.TRUE);
		descriptor_enabled_map.put(ConsoleOutputDescriptor.STDIN, Boolean.TRUE);
		descriptor_enabled_map.put(ConsoleOutputDescriptor.STDOUT, Boolean.TRUE);
		descriptor_enabled_map.put(ConsoleOutputDescriptor.STDOUT_UNKNOWN, Boolean.TRUE);
		descriptor_enabled_map.put(ConsoleOutputDescriptor.STDERR_UNKNOWN, Boolean.TRUE);
	}
	
	protected boolean shouldDisplayPrompt() {return false;}
	
	public void initialise(Composite composite) {
		display = composite.getDisplay();
		
		descriptor_color_map = new HashMap();
		descriptor_style_map = new HashMap();
		descriptor_enabled_map = new HashMap();
		setupDescriptorMap(display);
	    
		// Main thing where everything lives.
	    composite = new Composite(composite,SWT.NONE);
	    GridLayout gridLayout = new GridLayout();
	    gridLayout.marginHeight = 0;
	    gridLayout.marginWidth = 0;   
	    composite.setLayout(gridLayout);
	    GridData data = new GridData(GridData.FILL_BOTH);
	    composite.setLayoutData(data);

	    this.composite = composite;
	    
	    // The header.
	    Composite cHeader = new Composite(composite, SWT.BORDER);
	    gridLayout = new GridLayout();
	    gridLayout.marginHeight = 3;
	    gridLayout.marginWidth = 0;
	    cHeader.setLayout(gridLayout);
	    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
	    cHeader.setLayoutData(data);

	    cHeader.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
	    cHeader.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

	    this.header = new Label(cHeader, SWT.NULL);
	    this.header.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
	    this.header.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
	    
	    FontData[] fontData = this.header.getFont().getFontData();
	    fontData[0].setStyle(SWT.BOLD);
	    int fontHeight = (int)(fontData[0].getHeight() * 1.2);
	    fontData[0].setHeight(fontHeight);
	    
	    header_font = new Font(display, fontData);
	    this.header.setFont(header_font);
	    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
	    this.header.setLayoutData(data);
	    this.header.setText(this.title);
	    
	    window_composite = new Composite(composite, SWT.NONE);
	    window_composite.setLayoutData(new GridData(GridData.FILL_BOTH));
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    window_composite.setLayout(layout);
	    
	    // What font should this be?
	    main_font = new Font(display, "Courier New", 10, 0);
	    
	    messages = new StyledText(window_composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
	    messages.setWordWrap(true);
	    messages.setFont(main_font);
	    data = new GridData(GridData.FILL_BOTH);
	    data.horizontalSpan = 3;
	    messages.setLayoutData(data);

	}
	
	protected boolean hasSectionAtBottom() {return false;}
	
	public void setFocus() {/* nothing to do */}
	
	protected String getResourceKeyIdentifier() {return "script_window";}
	
	protected final String getUIText(String key) {
		return core.localise("azjython." + getResourceKeyIdentifier() + ".ui." + key);
	}
	
	protected final String getDatedUIText(String key) {
		return core.getTimePrefix() + " " + getUIText(key);
	}
	
	protected final void doUITask(Runnable r) {
		if (display.isDisposed()) {return;}
		display.asyncExec(r);
	}
	
	private void putText(String text) {
		if (this.messages.isDisposed()) {return;}
		this.messages.append(text);
		this.messages.setSelection(this.messages.getText().length());
	}
	
	public void putText(OutputContextString ocs) {
		if (this.messages.isDisposed()) {return;}
		if (!((Boolean)this.descriptor_enabled_map.get(ocs.type)).booleanValue()) {return;}
		this.messages.append(ocs.text);
		
		final int end_pos = this.messages.getText().length();
		final int length = ocs.text.length();
		final int start_pos = end_pos - length; 
		
		Color text_color = (Color)this.descriptor_color_map.get(ocs.type); 
		int font_style = ((Integer)this.descriptor_style_map.get(ocs.type)).intValue();
		StyleRange sr = new StyleRange(start_pos, length, text_color, null, font_style);
		this.messages.setStyleRange(sr);
		this.messages.setSelection(end_pos);
	}
	
	public void putTextAsync(final OutputContextString ocs) {
		doUITask(new Runnable() {
			public void run() {SWTJythonOutputWindow.this.putText(ocs);}
		});
	}
	
	public void sendTextControlCommand(IOControlCommand command) {
		if (command == IOControlCommand.ENSURE_NEW_LINE && !this.messages.isDisposed()) {
			String text = this.messages.getText();
			if (text.length() > 0) {
				if (text.charAt(text.length()-1) != '\n') {
					putText("\n");
				}
			}
		}
	}
	
	public void sendTextControlCommandAsync(final IOControlCommand command) {
		doUITask(new Runnable() {
			public void run() {SWTJythonOutputWindow.this.sendTextControlCommand(command);}
		});
	}
		
	public void disable() {
		if (!this.enabled) {return;}
		this.header.setText(this.title + " [" + this.getUIText("header_terminated") + "]");
		this.putText(new OutputContextString(this.getDatedUIText("window_terminated"), ConsoleOutputDescriptor.CONSOLE_INFO));
		
		// We allow this still to be enabled, because I want to allow the text to be
		// selectable.
		// this.messages.setEnabled(false);
	}
	
	public void disableAsync() {
		doUITask(new Runnable() {
			public void run() {SWTJythonOutputWindow.this.disable();}
		});
	}
	
	public void dispose() {
		if (!this.main_font.isDisposed()) {this.main_font.dispose();}
		if (!this.header_font.isDisposed()) {this.header_font.dispose();}
		if (!this.composite.isDisposed()) {this.composite.dispose();}
		Color c = null;
		Iterator itr = descriptor_color_map.values().iterator();
		while (itr.hasNext()) {
			c = (Color)itr.next();
			if (!c.isDisposed()) {
				c.dispose();
			}
		}
	}

}
