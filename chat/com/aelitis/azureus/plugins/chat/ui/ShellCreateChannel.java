/**
 * 
 */
package com.aelitis.azureus.plugins.chat.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

/**
 * @author TuxPaper
 * @created Mar 27, 2006
 *
 */
public class ShellCreateChannel {
	private String sText = null;
	public String open(Display display, String titleID, String detailsID, LocaleUtilities localeUtils) {
		GridData gridData;
		
		final Shell shell = new Shell(display.getActiveShell(), SWT.DIALOG_TRIM);
		GridLayout layout = new GridLayout();
		shell.setLayout(layout);
		shell.setText(localeUtils.getLocalisedMessageText(titleID));
		
		Label lbl = new Label(shell, SWT.WRAP);
		gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
		lbl.setLayoutData(gridData);
		lbl.setText(localeUtils.getLocalisedMessageText(detailsID));
		
		final Text txt = new Text(shell, SWT.BORDER);
		gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		txt.setLayoutData(gridData);
		
		Composite cButtons = new Composite(shell, SWT.NONE);
		gridData = new GridData(SWT.RIGHT, SWT.TOP, false, false);
		cButtons.setLayoutData(gridData);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cButtons.setLayout(layout);
		
		Button btnOk = new Button(cButtons, SWT.PUSH);
		btnOk.setText(localeUtils.getLocalisedMessageText("Button.ok"));
		btnOk.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sText = txt.getText();
				shell.dispose();
			}
		});
		shell.setDefaultButton(btnOk);

		Button btnCancel = new Button(cButtons, SWT.PUSH);
		btnCancel.setText(localeUtils.getLocalisedMessageText("Button.cancel"));
		btnCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sText = null;
				shell.dispose();
			}
		});
		
		shell.pack();
		shell.open();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		return sText;
	}
}
