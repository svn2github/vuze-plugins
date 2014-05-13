/*
 * ImportDialog.java
 *
 * Created on April 10, 2014, 6:28 PM
 */

package i18nAZ;

import java.net.URL;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * ImportDialog class
 * 
 * @author Repris d'injustice
 */
class ImportLanguageDialog
{
    final private static String DATAKEY_IMPORT_OBJECT = "importObject";
    final private static String DATAKEY_OPTION = "option";

    private Shell shell = null;
    private Table table = null;
    private TableCursor tableCursor = null;
    private Button okButton = null;
    private Button cancelButton = null;

    public List<ImportObject> importObjects = null;

    static class Actions
    {
        final static int OPEN_READ_ONLY = 1;
        final static int REPLACE = 2;
        final static int IGNORE = 4;
        final static int ADD = 8;

        final static int[] LIST = { Actions.OPEN_READ_ONLY, Actions.REPLACE, Actions.IGNORE, Actions.ADD };
    }

    ImportLanguageDialog(Shell owner, List<ImportObject> importObjects)
    {
        this.importObjects = importObjects;

        // Shell
        this.shell = new Shell(owner, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        this.shell.setImages(owner.getImages());
        this.shell.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.ImportLanguageDialog"));
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        this.shell.setLayout(layout);

        // Add Widgets
        this.addWidgets();

        // Add Events
        this.addEvents();

        // Fill Table
        this.fillTable();

        // set shell size
        this.shell.setSize(700, 300);

        // open shell and center it to parent
        Util.openShell(owner, this.shell);
    }

    private void addWidgets()
    {
        // Label
        Label label = new Label(this.shell, SWT.NONE);
        label.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Labels.ImportLanguageDialog"));
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));

        // Table
        this.table = new Table(this.shell, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.NO_FOCUS | SWT.HIDE_SELECTION);
        this.table.setHeaderVisible(true);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1);
        this.table.setLayoutData(gridData);

        // Table Columns
        new TableColumn(this.table, SWT.LEFT);
        this.table.getColumn(0).setText(i18nAZ.getLocalisedMessageText("i18nAZ.Columns.Language"));
        this.table.getColumn(0).setWidth(120);
        Util.addSortManager(this.table.getColumn(0), Util.STRING_COMPARATOR);

        new TableColumn(this.table, SWT.FILL);
        this.table.getColumn(1).setText(i18nAZ.getLocalisedMessageText("i18nAZ.Columns.Path"));
        Util.addSortManager(this.table.getColumn(1), Util.STRING_COMPARATOR);

        new TableColumn(this.table, SWT.LEFT);
        this.table.getColumn(2).setText(i18nAZ.getLocalisedMessageText("i18nAZ.Columns.Action"));
        this.table.getColumn(2).setWidth(100);
        Util.addSortManager(this.table.getColumn(2), Util.STRING_COMPARATOR);

        // Table Cursor
        this.tableCursor = new TableCursor(this.table, SWT.NULL);
        this.tableCursor.setMenu(new Menu(this.tableCursor));
        this.tableCursor.setForeground(this.table.getForeground());

        // OK Button
        this.okButton = new Button(this.shell, SWT.PUSH);
        this.okButton.setText(i18nAZ.getLocalisedMessageText("Button.ok"));
        gridData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
        gridData.widthHint = 100;
        this.okButton.setLayoutData(gridData);
        this.okButton.setEnabled(false);
        this.shell.setDefaultButton(this.okButton);

        // Cancel Button
        this.cancelButton = new Button(this.shell, SWT.PUSH);
        this.cancelButton.setText(i18nAZ.getLocalisedMessageText("Button.cancel"));
        gridData = new GridData(SWT.RIGHT, SWT.TOP, false, false);
        gridData.widthHint = 100;
        this.cancelButton.setLayoutData(gridData);
    }

    private void addEvents()
    {
        final MouseMoveListener mouseMoveListener = new MouseMoveListener()
        {
            
            public void mouseMove(MouseEvent e)
            {
                if (ImportLanguageDialog.this.table.isDisposed() == true || ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                Point point = new Point(e.x, e.y);
                if (e.widget instanceof TableCursor)
                {
                    point = ImportLanguageDialog.this.table.toControl((ImportLanguageDialog.this.tableCursor.toDisplay(point)));
                }
                TableItem row = ImportLanguageDialog.this.table.getItem(point);
                if (row != null)
                {
                    ImportLanguageDialog.this.table.setSelection(row);
                    ImportLanguageDialog.this.tableCursor.setSelection(row, 2);
                    ImportLanguageDialog.this.tableCursor.setVisible(true);
                }
                else
                {
                    ImportLanguageDialog.this.tableCursor.setVisible(false);
                }
            }
        };
        final MouseTrackAdapter mouseTrackAdapter = new MouseTrackAdapter()
        {
            
            public void mouseExit(MouseEvent e)
            {
                if (ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                Point point = ImportLanguageDialog.this.table.toDisplay(e.x, e.y);
                point = ImportLanguageDialog.this.tableCursor.toControl(point);
                if (ImportLanguageDialog.this.tableCursor.getBounds().contains(e.x, e.y) == false)
                {
                    ImportLanguageDialog.this.tableCursor.setVisible(false);
                }
            }
        };
        final SelectionAdapter selectionAdapter = new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {
                if (e.widget.isDisposed() == true)
                {
                    return;
                }
                MenuItem menuItem = (MenuItem) e.widget;
                TableItem row = ImportLanguageDialog.this.tableCursor.getRow();
                row.setText(2, menuItem.getText());
                int selectedAction = (Integer) menuItem.getData(ImportLanguageDialog.DATAKEY_OPTION);

                ImportObject currentImportObject = (ImportObject) row.getData(ImportLanguageDialog.DATAKEY_IMPORT_OBJECT);
                switch (selectedAction)
                {
                    case ImportLanguageDialog.Actions.REPLACE:
                    case ImportLanguageDialog.Actions.ADD:
                        for (int i = 0; i < ImportLanguageDialog.this.table.getItemCount(); i++)
                        {
                            if (row.equals(ImportLanguageDialog.this.table.getItem(i)) == true)
                            {
                                continue;
                            }
                            ImportObject importObject = (ImportObject) ImportLanguageDialog.this.table.getItem(i).getData(DATAKEY_IMPORT_OBJECT);
                            if (importObject.locale.equals(currentImportObject.locale) == false)
                            {
                                continue;
                            }
                            importObject.allowedActions |= ImportLanguageDialog.Actions.OPEN_READ_ONLY;
                            if (importObject.selectedAction != selectedAction)
                            {
                                continue;
                            }
                            importObject.selectedAction = ImportLanguageDialog.Actions.OPEN_READ_ONLY;
                            ImportLanguageDialog.this.table.getItem(i).setText(2, ImportLanguageDialog.getTextAction(importObject.selectedAction));
                        }
                        break;
                    case ImportLanguageDialog.Actions.IGNORE:

                        boolean exist = false;
                        for (int i = 0; i < ImportLanguageDialog.this.table.getItemCount(); i++)
                        {
                            if (row.equals(ImportLanguageDialog.this.table.getItem(i)) == true)
                            {
                                continue;
                            }
                            ImportObject importObject = (ImportObject) ImportLanguageDialog.this.table.getItem(i).getData(DATAKEY_IMPORT_OBJECT);
                            if (importObject.locale.equals(currentImportObject.locale) == false)
                            {
                                continue;
                            }
                            if (importObject.selectedAction != ImportLanguageDialog.Actions.ADD && importObject.selectedAction != ImportLanguageDialog.Actions.REPLACE)
                            {
                                continue;
                            }
                            exist = true;
                        }
                        if (exist == false)
                        {
                            for (int i = 0; i < ImportLanguageDialog.this.table.getItemCount(); i++)
                            {
                                ImportObject importObject = (ImportObject) ImportLanguageDialog.this.table.getItem(i).getData(DATAKEY_IMPORT_OBJECT);
                                if (importObject.locale.equals(currentImportObject.locale) == false)
                                {
                                    continue;
                                }
                                importObject.allowedActions &= ~ImportLanguageDialog.Actions.OPEN_READ_ONLY;
                                if (row.equals(ImportLanguageDialog.this.table.getItem(i)) == true)
                                {
                                    continue;
                                }
                                if (importObject.selectedAction == ImportLanguageDialog.Actions.IGNORE)
                                {
                                    continue;
                                }
                                importObject.selectedAction = ImportLanguageDialog.Actions.IGNORE;
                                ImportLanguageDialog.this.table.getItem(i).setText(2, ImportLanguageDialog.getTextAction(importObject.selectedAction));
                            }
                        }

                        break;
                }
                currentImportObject.selectedAction = selectedAction;
                boolean enable = false;
                for (int i = 0; i < ImportLanguageDialog.this.table.getItemCount(); i++)
                {
                    ImportObject importObject = (ImportObject) ImportLanguageDialog.this.table.getItem(i).getData(DATAKEY_IMPORT_OBJECT);
                    if (importObject.selectedAction != ImportLanguageDialog.Actions.IGNORE)
                    {
                        enable = true;
                        break;
                    }
                }
                ImportLanguageDialog.this.okButton.setEnabled(enable);
            }
        };
        this.table.addControlListener(new ControlAdapter()
        {
            
            public void controlResized(ControlEvent e)
            {
                if (ImportLanguageDialog.this.table.isDisposed() == true || ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                Rectangle clientArea = ImportLanguageDialog.this.table.getClientArea();
                ScrollBar vBar = ImportLanguageDialog.this.table.getVerticalBar();
                int width = clientArea.width;
                if (ImportLanguageDialog.this.table.computeSize(SWT.DEFAULT, SWT.DEFAULT).y > clientArea.height + ImportLanguageDialog.this.table.getHeaderHeight())
                {
                    width -= vBar.getSize().x;
                }
                ImportLanguageDialog.this.table.getColumn(1).setWidth(width - ImportLanguageDialog.this.table.getColumn(0).getWidth() - ImportLanguageDialog.this.table.getColumn(2).getWidth());
            }
        });
        this.table.addListener(SWT.EraseItem, new Listener()
        {
            private Item itemHot = null;

            
            public void handleEvent(Event e)
            {
                if (ImportLanguageDialog.this.table.isDisposed() == true || ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                switch (e.type)
                {
                    case SWT.EraseItem:

                        // INIT
                        Rectangle originalClipping = e.gc.getClipping();

                        // NO HOT NO SELECTED
                        if (this.itemHot != null && this.itemHot.equals(e.item) == true && (e.detail & SWT.HOT) == 0 && e.index > 0)
                        {
                            // e.detail &= ~SWT.BACKGROUND;
                            // e.detail &= ~SWT.SELECTED;
                            e.detail |= SWT.HOT;

                        }
                        if ((e.detail & SWT.HOT) == 0 && e.index == 0)
                        {
                            this.itemHot = null;
                        }
                        if ((e.detail & SWT.HOT) == 0 && (e.detail & SWT.SELECTED) == 0)
                        {
                            e.gc.fillRectangle(e.x, e.y, e.width, e.height);
                        }

                        // HOT
                        if ((e.detail & SWT.HOT) != 0)
                        {
                            e.detail &= ~SWT.BACKGROUND;
                            if (e.index == 0)
                            {
                                this.itemHot = (Item) e.item;
                            }
                            e.gc.setBackground(TreeTableManager.COLOR_ROW_HOT_BACKGROUND);
                            Rectangle rowClipping = e.gc.getClipping();
                            rowClipping.width = ImportLanguageDialog.this.table.getClientArea().width;
                            e.gc.setClipping(rowClipping);
                            e.gc.fillRectangle(e.x, e.y, rowClipping.width, e.height);
                        }

                        // SELECTED
                        if ((e.detail & SWT.SELECTED) != 0)
                        {
                            e.detail &= ~SWT.SELECTED;
                            e.detail &= ~SWT.BACKGROUND;
                        }

                        e.gc.setClipping(originalClipping);
                        e.detail &= ~SWT.HOT;
                        break;
                }
            }
        });

        Util.setCustomHotColor(this.table);

        this.table.addMouseMoveListener(mouseMoveListener);
        this.table.addMouseTrackListener(mouseTrackAdapter);

        this.table.getColumn(0).addControlListener(new ControlAdapter()
        {
            
            public void controlResized(ControlEvent e)
            {
                if (ImportLanguageDialog.this.table.isDisposed() == true || ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                Rectangle clientArea = ImportLanguageDialog.this.table.getClientArea();
                ScrollBar vBar = ImportLanguageDialog.this.table.getVerticalBar();
                int width = clientArea.width;
                if (ImportLanguageDialog.this.table.computeSize(SWT.DEFAULT, SWT.DEFAULT).y > clientArea.height + ImportLanguageDialog.this.table.getHeaderHeight())
                {
                    width -= vBar.getSize().x;
                }
                ImportLanguageDialog.this.table.getColumn(1).setWidth(width - ImportLanguageDialog.this.table.getColumn(0).getWidth() - ImportLanguageDialog.this.table.getColumn(2).getWidth());
            }
        });
        this.table.getColumn(1).addControlListener(new ControlAdapter()
        {
            
            public void controlResized(ControlEvent e)
            {
                if (ImportLanguageDialog.this.table.isDisposed() == true || ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                Rectangle clientArea = ImportLanguageDialog.this.table.getClientArea();
                ScrollBar vBar = ImportLanguageDialog.this.table.getVerticalBar();
                int width = clientArea.width;
                if (ImportLanguageDialog.this.table.computeSize(SWT.DEFAULT, SWT.DEFAULT).y > clientArea.height + ImportLanguageDialog.this.table.getHeaderHeight())
                {
                    width -= vBar.getSize().x;
                }
                ImportLanguageDialog.this.table.getColumn(2).setWidth(width - ImportLanguageDialog.this.table.getColumn(0).getWidth() - ImportLanguageDialog.this.table.getColumn(1).getWidth());
            }
        });

        this.tableCursor.addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {
                if (ImportLanguageDialog.this.table.isDisposed() == true || ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                if (ImportLanguageDialog.this.tableCursor.getColumn() < 2)
                {
                    ImportLanguageDialog.this.tableCursor.setSelection(ImportLanguageDialog.this.tableCursor.getRow(), 2);
                }
            }
        });
        this.tableCursor.addPaintListener(new PaintListener()
        {
            private Image arrow = i18nAZ.viewInstance.getImageLoader().getImage("i18nAZ.image.arrow");

            private String getText(GC gc)
            {
                if (ImportLanguageDialog.this.tableCursor.getRow() == null)
                {
                    return "";
                }
                int width = ImportLanguageDialog.this.tableCursor.getSize().x - this.arrow.getBounds().width - 6;
                String text = ImportLanguageDialog.this.tableCursor.getRow().getText(ImportLanguageDialog.this.tableCursor.getColumn());
                text = (text == null) ? "" : text;
                if (gc.textExtent(text).x > width)
                {
                    int ellipseWidth = gc.textExtent("...").x;
                    int length = text.length();
                    while (length > 0)
                    {
                        length--;
                        text = text.substring(0, length);
                        if (gc.textExtent(text).x + ellipseWidth <= width)
                        {
                            text = text + "...";
                            break;
                        }
                    }
                }
                return text;
            }

            
            public void paintControl(PaintEvent e)
            {
                if (ImportLanguageDialog.this.table.isDisposed() == true || ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                String text = this.getText(e.gc);
                Point size = e.gc.textExtent(text);
                int offset2 = Math.max(0, (e.height - size.y) / 2);
                e.gc.setBackground(new Color(null, 255, 255, 255));
                e.gc.fillRectangle(e.x, e.y, e.width, e.height);
                e.gc.setForeground(new Color(null, 0, 0, 0));
                e.gc.drawText(text, e.x + 6, e.y + offset2, true);
                e.gc.drawImage(this.arrow, ImportLanguageDialog.this.tableCursor.getBounds().width - 15, (ImportLanguageDialog.this.tableCursor.getBounds().height - 15) / 2);

            }
        });
        this.tableCursor.addMouseListener(new MouseAdapter()
        {
            
            public void mouseUp(MouseEvent e)
            {
                if (ImportLanguageDialog.this.tableCursor.isDisposed() == true)
                {
                    return;
                }
                TableItem row = ImportLanguageDialog.this.tableCursor.getRow();
                if (row != null)
                {
                    while (ImportLanguageDialog.this.tableCursor.getMenu().getItemCount() > 0)
                    {
                        ImportLanguageDialog.this.tableCursor.getMenu().getItem(0).dispose();
                    }

                    MenuItem menuItem = null;

                    ImportObject importObject = (ImportObject) row.getData(ImportLanguageDialog.DATAKEY_IMPORT_OBJECT);
                    for (int i = 0; i < Actions.LIST.length; i++)
                    {
                        if (importObject.selectedAction != Actions.LIST[i] && (importObject.allowedActions & Actions.LIST[i]) != 0)
                        {
                            menuItem = new MenuItem(ImportLanguageDialog.this.tableCursor.getMenu(), SWT.PUSH);
                            menuItem.setText(ImportLanguageDialog.getTextAction(Actions.LIST[i]));
                            menuItem.setData(ImportLanguageDialog.DATAKEY_OPTION, Actions.LIST[i]);
                            menuItem.addSelectionListener(selectionAdapter);
                        }
                    }

                    Point point = ImportLanguageDialog.this.tableCursor.toDisplay(new Point(0, ((TableCursor) e.widget).getSize().y));
                    ImportLanguageDialog.this.tableCursor.getMenu().setLocation(point);
                    ImportLanguageDialog.this.tableCursor.getMenu().setVisible(true);
                }
            }
        });
        this.tableCursor.addMenuDetectListener(new MenuDetectListener()
        {
            
            public void menuDetected(MenuDetectEvent e)
            {
                e.doit = false;
            }
        });
        this.tableCursor.addMouseMoveListener(mouseMoveListener);

        this.okButton.addListener(SWT.Selection, new Listener()
        {
            
            public void handleEvent(Event event)
            {
                if (ImportLanguageDialog.this.okButton.isDisposed() == true)
                {
                    return;
                }
                for (int i = 0; i < ImportLanguageDialog.this.table.getItemCount(); i++)
                {
                    ImportObject importObject = (ImportObject) ImportLanguageDialog.this.table.getItem(i).getData(DATAKEY_IMPORT_OBJECT);
                    ImportLanguageDialog.this.importObjects.add(importObject);
                }
                ImportLanguageDialog.this.shell.dispose();
            }
        });
        this.cancelButton.addListener(SWT.Selection, new Listener()
        {
            
            public void handleEvent(Event event)
            {
                if (ImportLanguageDialog.this.cancelButton.isDisposed() == true)
                {
                    return;
                }
                ImportLanguageDialog.this.shell.dispose();
            }
        });
    }

    private static String getTextAction(int option)
    {
        switch (option)
        {
            case ImportLanguageDialog.Actions.OPEN_READ_ONLY:
                return i18nAZ.getLocalisedMessageText("i18nAZ.Menus.OpenReadOnly");
            case ImportLanguageDialog.Actions.IGNORE:
                return i18nAZ.getLocalisedMessageText("i18nAZ.Menus.Ignore");
            case ImportLanguageDialog.Actions.REPLACE:
                return i18nAZ.getLocalisedMessageText("i18nAZ.Menus.Replace");
            case ImportLanguageDialog.Actions.ADD:
                return i18nAZ.getLocalisedMessageText("i18nAZ.Menus.Add");
            default:
                break;
        }
        return null;
    }

    private void fillTable()
    {
        for (int i = 0; i < this.importObjects.size(); i++)
        {
            TableItem item = new TableItem(this.table, SWT.NULL);
            item.setText(0, Util.getLocaleDisplay(this.importObjects.get(i).locale, true));
            item.setImage(0, Util.getLocaleImage(this.importObjects.get(i).locale));
            item.setText(1, this.importObjects.get(i).url.toString());
            item.setText(2, ImportLanguageDialog.getTextAction(this.importObjects.get(i).selectedAction));
            item.setData(DATAKEY_IMPORT_OBJECT, this.importObjects.get(i).clone());
        }
        this.importObjects.clear();
    }

}

class ImportObject implements Cloneable
{
    int allowedActions = ImportLanguageDialog.Actions.IGNORE;
    Locale locale = null;
    URL url = null;
    LocaleProperties properties = null;
    int selectedAction = ImportLanguageDialog.Actions.IGNORE;

    ImportObject(URL url)
    {
        this.url = url;
    }

    
    public Object clone()
    {
        ImportObject importObject = new ImportObject(this.url);
        importObject.locale = (Locale) this.locale.clone();
        importObject.selectedAction = this.selectedAction;
        importObject.allowedActions = this.allowedActions;
        importObject.properties = this.properties;
        importObject.url = this.url;
        return importObject;
    }

    void dispose()
    {
        this.locale = null;
        this.properties = null;
        this.url = null;
        this.selectedAction = 0;
        this.allowedActions = 0;
    }
}
