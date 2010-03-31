/*
 * LangEditor.java
 *
 * Created on March 1, 2004, 10:16 AM
 */

package i18nPlugin;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.TableTreeItem;

/** Opens a modal Editor Window.  When closed, sValue contains edited value,
 *  or null if user canceled.
 *
 * @author  TuxPaper
 */
public class LangEditor {
  /** Edited value, or null if user canceled. */
  public String sValue = null;
  
  /**
   * Creates a new instance of LangEditor
   * @param row The row being edited
   * @param iColumn The column # being edited
   */
  public LangEditor(TableTreeItem row, int iColumn, final boolean bMultiEdit) {
    String sOldValue = row.getText(iColumn);
    
    String sComment = (String)row.getData(View.DATAKEY_COMMENT);
    
    if (bMultiEdit) {
      sOldValue = sOldValue.replaceAll("\\\\n", "\n");
      sOldValue = sOldValue.replaceAll("\\\\t", "\t");
    }
    
    Display display = row.getDisplay();
    final Shell shell = new Shell(display, SWT.TITLE | SWT.RESIZE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.CLOSE);
    shell.setText("Edit Entry " + (String)row.getData(View.DATAKEY_PATH));
    Rectangle rBounds = row.getBounds(iColumn);
    Point pt = new Point(rBounds.x, rBounds.y);
    pt = row.getParent().toDisplay(pt);
    shell.setLocation(pt);

    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    shell.setLayout(layout);

    if (sComment != null) {
    	Label label = new Label(shell, SWT.WRAP);
    	label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	label.setText(sComment);
    }
    
    Text txtRef = new Text(shell, SWT.WRAP | SWT.MULTI | SWT.BORDER);
    txtRef.setEditable(false);
    GridData txtRefGridData = new GridData(GridData.FILL_HORIZONTAL);
    txtRef.setLayoutData(txtRefGridData);
    String sRefText = row.getText(1);
    if (bMultiEdit) {
      sRefText = sRefText.replaceAll("\\\\n", "\n");
      sRefText = sRefText.replaceAll("\\\\t", "\t");
    }
    txtRef.setText(sRefText);
    txtRef.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        // close the text editor when the user hits "ESC"
        if (e.character == SWT.ESC) {
          e.doit = false;
          shell.dispose();
        }
      }
    });
    
    final Text txtEdit = new Text(shell, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    GridData txtEditGridData = new GridData(GridData.FILL_BOTH);
    txtEdit.setLayoutData(txtEditGridData);
    txtEdit.setText(sOldValue == "" ? sRefText : sOldValue);
    txtEdit.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        // close the text editor when the user hits "ESC"
        if (e.character == SWT.ESC) {
          e.doit = false;
          shell.dispose();
        }
      }
      public void keyPressed(KeyEvent e) {
        if (e.character == SWT.CR && !bMultiEdit) {
          sValue = txtEdit.getText();
          shell.dispose();
        }
      }
    });

    if (bMultiEdit) {
      GridData gridData;
      
      Composite cButtons = new Composite(shell, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 2;
      cButtons.setLayout(layout);
      gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
      cButtons.setLayoutData(gridData);

      Button btnOk = new Button(cButtons, SWT.PUSH);
      btnOk.setText("&Ok");
      gridData = new GridData();
      gridData.widthHint = 100;
      btnOk.setLayoutData(gridData);
      btnOk.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          sValue = txtEdit.getText();
          shell.dispose();
        }
      });

      Button btnCancel = new Button(cButtons, SWT.PUSH);
      gridData = new GridData();
      gridData.widthHint = 100;
      btnCancel.setLayoutData(gridData);
      btnCancel.setText("&Cancel");
      btnCancel.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          shell.dispose();
        }
      });
    }

    txtEdit.selectAll();
    txtEdit.setFocus();
    
//    System.out.println("Before:"+shell.getBounds());
    shell.pack();
    Rectangle rDisplay = shell.getMonitor().getClientArea();
    Rectangle rShellBounds = shell.getBounds();
    
    Point point = shell.computeSize(500, SWT.DEFAULT);
    if (bMultiEdit) {
    	point.y += 20;
    }
    rShellBounds.width = 500;
    rShellBounds.height = point.y;
    
    if (rShellBounds.x + rShellBounds.width > rDisplay.x + rDisplay.width) {
      rShellBounds.x -= (rShellBounds.x + rShellBounds.width) - (rDisplay.x + rDisplay.width);
      if (rShellBounds.x < rDisplay.x)
        rShellBounds.x = rDisplay.x;
    }
    if (rShellBounds.y + rShellBounds.height > rDisplay.y + rDisplay.height) {
      rShellBounds.y -= (rShellBounds.y + rShellBounds.height) - (rDisplay.y + rDisplay.height);
      if (rShellBounds.y < rDisplay.y)
        rShellBounds.y = rDisplay.y;
    }
    //System.out.println("2Shell="+rShellBounds+"; Intersect="+rShellBounds.intersection(rDisplay));
    Rectangle rNewShellBounds = rShellBounds.intersection(rDisplay);
    shell.setBounds(rNewShellBounds);
    shell.layout(true);
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
    if (sValue != null && bMultiEdit) {
      sValue = sValue.replaceAll("\r\n", "\n");
      sValue = sValue.replaceAll("\n", "\\\\n");
      sValue = sValue.replaceAll("\t", "\\\\t");
    }
  }
}
