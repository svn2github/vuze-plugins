/*
 * Created on Sep 21, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.plugins.minibrowser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * 
 */
public class BookmarkEditor {
  
  public BookmarkEditor(Display display,final BookmarkEditorListener listener,Bookmark bookmark) {
    final Shell shell = new Shell(display,SWT.BORDER | SWT.TITLE | SWT.CLOSE);
    shell.setText("Bookmark Editor");
    
    FormLayout layout = new FormLayout();
    layout.marginHeight = 5;
    layout.marginWidth = 5;
    layout.spacing = 3;
    shell.setLayout(layout);
    
    Label lblName = new Label(shell,SWT.NULL);
    lblName.setText("Name :");
    
    final Text txtName = new Text(shell,SWT.BORDER);
    txtName.setText(bookmark.getName());
    
    Label lblAddress = new Label(shell,SWT.NULL);
    lblAddress.setText("Address :");
    
    final Text txtAddress = new Text(shell,SWT.BORDER);
    txtAddress.setText(bookmark.getUrl());
    
    Button btnCancel = new Button(shell,SWT.PUSH);
    btnCancel.setText("Cancel");
        
    Button btnOk = new Button(shell,SWT.PUSH);
    btnOk.setText("Ok");
    shell.setDefaultButton(btnOk);
    
    
    FormData formData;
    
    formData = new FormData();     
    formData.width = 80;
    lblName.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(lblName);
    formData.right = new FormAttachment(100,-10);
    txtName.setLayoutData(formData);
    
    formData = new FormData();
    formData.top = new FormAttachment(txtName);
    formData.width = 80;
    lblAddress.setLayoutData(formData);
    
    formData = new FormData();
    formData.top = new FormAttachment(txtName);
    formData.left = new FormAttachment(lblAddress);
    formData.right = new FormAttachment(100,-10);
    txtAddress.setLayoutData(formData);
    
    formData = new FormData();
    formData.top =  new FormAttachment(txtAddress);
    formData.right = new FormAttachment(100,-10);
    formData.bottom = new FormAttachment(100,-10);
    formData.width = 80;
    btnOk.setLayoutData(formData);
    
    formData = new FormData();
    formData.top =  new FormAttachment(txtAddress);
    formData.right = new FormAttachment(btnOk);
    formData.width = 80;
    btnCancel.setLayoutData(formData);
    
    btnCancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
       listener.canceled();
       shell.dispose();
      }
    });
    
    btnOk.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
       listener.changed(txtName.getText(),txtAddress.getText());
       shell.dispose();
      }
    });
    
    Point p = shell.computeSize(500,SWT.DEFAULT);
    shell.setSize(p);
    
    shell.open();
  }   
}
