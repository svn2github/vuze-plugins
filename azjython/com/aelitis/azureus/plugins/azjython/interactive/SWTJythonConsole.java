/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import com.aelitis.azureus.plugins.azjython.JythonPluginCore;
import com.aelitis.azureus.plugins.azjython.utils.DataSink;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.layout.GridData;
import org.gudy.azureus2.ui.swt.plugins.UISWTInputReceiver;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * @author Allan Crooks
 *
 */
public class SWTJythonConsole extends SWTJythonOutputWindow {
	
	private DataSink sink;
	private DataSink multi_sink;
	
	// Things we control.
	private Combo input;
	private Button send;
	private Button send_multi;
	
	private final int MAX_LINE_LIMIT = 30;
	
	public SWTJythonConsole(JythonPluginCore core, UISWTInstance swt_ui, String title) {
		super(core, swt_ui, title);
		this.enabled = true;
	}
	
	protected boolean shouldDisplayPrompt() {return true;}
	
	public void initialise(Composite composite) {
		super.initialise(composite);
		
		/**
	     * How to get a pop-up menu, but we'll do that another time...
	     * 
	    Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
	    MenuItem m1 = new MenuItem(menu, SWT.PUSH);
	    MenuItem m2 = new MenuItem(menu, SWT.PUSH);
	    Menu menu2 = new Menu(menu);
	    MenuItem m3 = new MenuItem(menu, SWT.CASCADE);
	    MenuItem m4 = new MenuItem(menu2, SWT.PUSH);
	    m3.setMenu(menu2);
	    m1.setText("ashes to ashes");
	    m2.setText("funk to funky");
	    m3.setText("yeen yeen is a");
	    m4.setText("cheeky monkey");
	    messages.setMenu(menu);
	     */
	    
	    input = new Combo(window_composite, SWT.BORDER);
	    input.setFont(main_font);
	    GridData data = new GridData(GridData.FILL_HORIZONTAL);
	    input.setLayoutData(data);
	    input.setItems(new String[]{""});
	    
	    send = new Button(window_composite, SWT.PUSH);
	    send.setText(getUIText("send"));
	    data = new GridData();
	    data.widthHint = 50;
	    send.setLayoutData(data);
	    
	    send_multi = new Button(window_composite, SWT.PUSH);
	    send_multi.setText(getUIText("send_multi"));
	    data = new GridData();
	    data.widthHint = 80;
	    send_multi.setLayoutData(data);
	    
	    InputListener input_listener = new InputListener();
	    send.addListener(SWT.Selection, input_listener);
	    input.addKeyListener(input_listener);
	    send_multi.addListener(SWT.Selection, new MultiLineTextEntry());
	}
	
	protected boolean hasBottomSection() {return true;}
	protected String getResourceKeyIdentifier() {return "interpreter";}
	
	public void setFocus() {
		if (!this.input.isDisposed()) {
			/**
			 * If the focus is currently on send_multi, and the user has submitted
			 * some input, we want the focus to be on "input" again, so the user can
			 * type something in.
			 * 
			 * But when the user enters the text and the presses enter, send_input seems
			 * to receive the event. So we set the focus on send first and then input,
			 * which seems to resolve this.
			 */
			this.send.setFocus();
			this.input.setFocus();
		}
	}
					
	public void setDataSink(DataSink sink) {
		this.sink = sink;
	}
	
	public void setMultiLineDataSink(DataSink sink) {
		this.multi_sink = sink;
	}
	
	public void disable() {
		if (!this.enabled) {return;}
		super.disable();
		this.input.setEnabled(false);
		this.send.setEnabled(false);
		this.send_multi.setEnabled(false);
	}
		
	private class MultiLineTextEntry implements Listener {
		public void handleEvent(Event event) {
			UISWTInputReceiver entry = (UISWTInputReceiver)SWTJythonConsole.this.swt_ui.getInputReceiver();
			entry.setMultiLine(true);
			entry.setWidthHint(400);
			entry.setLineHeight(10);
			entry.maintainWhitespace(true);
			entry.setLocalisedTitle(getUIText("send_multi.input.title"));
			entry.prompt();
			if (!entry.hasSubmittedInput()) {return;}
			String input = entry.getSubmittedInput();
			try {
				BufferedReader br = new BufferedReader(new StringReader(input));
				String line = br.readLine();
				ArrayList lines = new ArrayList();
				while (line != null) {
					lines.add(line);
					line = br.readLine();
				}
				multi_sink.put(lines.toArray(new String[lines.size()]));
			}
			catch (IOException ioe) {/* won't happen */}
			setFocus();
		}
	}
	
	private class InputListener extends KeyAdapter implements Listener, KeyListener {

		public void sendInput() {
			String text_to_send = SWTJythonConsole.this.input.getText();
			if (text_to_send != null && text_to_send.trim().length() > 0) {

				String[] prev_hist = input.getItems();
				int new_size = Math.min(MAX_LINE_LIMIT, prev_hist.length + 1);
				
				// Drop the placeholder blank line that we use to get the combo box to work.
				if (prev_hist[prev_hist.length-1].trim().length() == 0) {new_size--;}
				
				String[] new_hist = new String[new_size];
				System.arraycopy(prev_hist, 0, new_hist, 1, new_hist.length-1);
				new_hist[0] = text_to_send;
				input.setItems(new_hist);
			}
			if (text_to_send == null) {return;}
			input.setText(""); // Clear the text box.
			DataSink target = SWTJythonConsole.this.sink;
			if (target != null) {target.put(text_to_send);}
		}

		// Submit input on return.
		public void keyReleased(KeyEvent event) {
			if (event.keyCode == 13) {sendInput();}
		}

		// Send text on button press.
		public void handleEvent(Event event) { 
			sendInput();
		}		
		
	}

}
